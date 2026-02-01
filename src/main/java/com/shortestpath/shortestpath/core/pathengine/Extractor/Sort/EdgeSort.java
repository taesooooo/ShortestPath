package com.shortestpath.shortestpath.core.pathengine.Extractor.Sort;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

/**
 * DataStore를 사용하여 엣지를 from 노드 순서대로 정렬
 * from이 같으면 RoadLevel 순서(L0, L1, L2)로 정렬
 * DataStore의 리더/라이터를 사용하여 모든 읽기/쓰기 수행
 * 퀵정렬로 엣지 순서 재정렬
 */
public class EdgeSort {
    private static final Logger logger = LoggerFactory.getLogger(EdgeSort.class);
    
    private DataStore dataStore;

    public EdgeSort(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void sort() throws IOException { 
        if (dataStore == null) {
            throw new IllegalStateException("DataStore가 초기화되지 않았습니다.");
        }
        logger.info("엣지 정렬 시작...");

        int totalEdges = dataStore.getTotalEdges();
        quickSortInPlace(0, totalEdges - 1);
        
        logger.info("엣지 정렬 완료!");
    }

    private void quickSortInPlace(int low, int high) throws IOException {
        if (low < high) {
            int pi = partition(low, high);
            quickSortInPlace(low, pi - 1);
            quickSortInPlace(pi + 1, high);
        }
    }

    private int partition(int low, int high) throws IOException {
        long pivotOffset = DataStructureSizes.calculateEdgeOffset(high);
        Edge pivotEdge = dataStore.readEdge(pivotOffset);
        int pivotFrom = pivotEdge.getFrom();
        
        int i = low - 1;
        
        for (int j = low; j < high; j++) {
            long jOffset = DataStructureSizes.calculateEdgeOffset(j);
            Edge jEdge = dataStore.readEdge(jOffset);
            int jFrom = jEdge.getFrom();
            
            // from 기준 비교, 같으면 RoadLevel로 비교 (L0 < L1 < L2)
            int cmp = Integer.compare(jFrom, pivotFrom);
            if (cmp == 0) {
                cmp = compareRoadLevel(jEdge.getRoadLevel(), pivotEdge.getRoadLevel());
            }
            
            if (cmp < 0) {
                i++;
                // DataStore를 사용하여 엣지 교환
                swapEdgesWithDataStore(i, j);
            }
        }
        
        // 피벗을 최종 위치로 이동
        swapEdgesWithDataStore(i + 1, high);
        
        if ((high + 1) % 10000 == 0) {
            logger.debug("정렬 진행: {}", high + 1);
        }
        
        return i + 1;
    }
    
    /**
     * RoadLevel 비교 (L0 < L1 < L2 순서)
     */
    private int compareRoadLevel(RoadLevel levelA, 
                                 RoadLevel levelB) {
        if (levelA == levelB) {
            return 0;
        }
        if (levelA == null) {
            return 1; // null은 가장 뒤로
        }
        if (levelB == null) {
            return -1; // null은 가장 뒤로
        }
        // ordinal() 사용: L0(0) < L1(1) < L2(2)
        return Integer.compare(levelA.ordinal(), levelB.ordinal());
    }
    
    private void swapEdgesWithDataStore(int index1, int index2) throws IOException {
        if (index1 == index2) {
            return;
        }
        
        long offset1 = DataStructureSizes.calculateEdgeOffset(index1);
        long offset2 = DataStructureSizes.calculateEdgeOffset(index2);
        
        Edge edge1 = dataStore.readEdge(offset1);
        Edge edge2 = dataStore.readEdge(offset2);
        
        dataStore.overwriteEdge(edge2, offset1);
        dataStore.overwriteEdge(edge1, offset2);
    }
    
}
