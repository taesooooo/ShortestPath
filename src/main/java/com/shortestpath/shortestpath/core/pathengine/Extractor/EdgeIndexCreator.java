package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.IOException;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndexEntry;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.LevelEdgeIndex;

import lombok.extern.slf4j.Slf4j;

/**
 */
@Slf4j
public class EdgeIndexCreator {
    private DataStore store;
    
    public EdgeIndexCreator(DataStore dataStore) {
        this.store = dataStore;
    }

    public void createEdgeIndex() throws IOException {
        createEdgeIndex(store.getEdgeIndex());
    }

    public void createEdgeIndex(EdgeIndex edgeIndex) throws IOException {
        validateEdgeIndex(edgeIndex, "엣지 인덱스");

        createEdgeIndex(store.getTotalEdges(), edgeIndex, new EdgeReader() {
            @Override
            public Edge read(long offset) throws IOException {
                return store.readEdge(offset);
            }
        }, new EdgeNodeSelector() {
            @Override
            public int select(Edge edge) {
                return edge.getFrom();
            }
        }, "엣지 인덱스");
    }

    public void createReverseEdgeIndex(EdgeIndex reverseEdgeIndex) throws IOException {
        validateEdgeIndex(reverseEdgeIndex, "리버스 엣지 인덱스");

        createEdgeIndex(store.getTotalReverseEdges(), reverseEdgeIndex, new EdgeReader() {
            @Override
            public Edge read(long offset) throws IOException {
                return store.readReverseEdge(offset);
            }
        }, new EdgeNodeSelector() {
            @Override
            public int select(Edge edge) {
                return edge.getTo();
            }
        }, "리버스 엣지 인덱스");
    }

    public void createReverseEdgeIndex() throws IOException {
        createReverseEdgeIndex(store.getReverseEdgeIndex());
    }

    private void createEdgeIndex(int totalEdges, EdgeIndex edgeIndex, EdgeReader edgeReader, EdgeNodeSelector nodeSelector, String indexName) throws IOException {
        log.info("{} 생성 시작 - 총 엣지 개수: {}", indexName, totalEdges);
        edgeIndex.clear();

        EdgeIndexEntry currentEntry = null;
        int previousNodeId = -1;

        for (int i = 0; i < totalEdges; i++) {
            long edgeOffset = DataStructureSizes.calculateEdgeOffset(i);
            Edge edge = edgeReader.read(edgeOffset);
            
            int currentNodeId = nodeSelector.select(edge);
            
            // 노드가 바뀌면 이전 노드의 엣지 인덱스를 저장
            if (previousNodeId != -1 && previousNodeId != currentNodeId) {
                edgeIndex.put(currentEntry);
                log.trace("{} - 노드 {} 저장 완료", indexName, previousNodeId);
                currentEntry = null;
            }
            
            // 새로운 노드의 첫 엣지
            if (currentEntry == null) {
                currentEntry = new EdgeIndexEntry(currentNodeId);
            }
            
            // 현재 엣지의 RoadLevel별로 인덱스 추가 또는 업데이트
            updateLevelEdgeIndex(edge.getRoadLevel(), edgeOffset, currentEntry);
            
            previousNodeId = currentNodeId;
        }
        
        // 마지막 노드의 엣지 인덱스 저장
        if (currentEntry != null) {
            edgeIndex.put(currentEntry);
            log.debug("{} - 노드 {} 저장 완료 (마지막)", indexName, previousNodeId);
        }

        edgeIndex.flush();
        log.info("{} 생성 및 저장 완료", indexName);
    }

    private void validateEdgeIndex(EdgeIndex edgeIndex, String indexName) {
        if(edgeIndex == null) {
            throw new IllegalArgumentException(indexName + "는 null일 수 없습니다.");
        }
    }

    private void updateLevelEdgeIndex(RoadLevel roadLevel, long edgeOffset, EdgeIndexEntry edgeIndexEntry) {
        if (roadLevel == RoadLevel.L0) {
            if (edgeIndexEntry.getLevel0EdgeIndex().getStartOffset() == -1) {
                // 첫 L0 엣지: 시작 오프셋 저장
                LevelEdgeIndex level0EdgeIndex = new LevelEdgeIndex(RoadLevel.L0, edgeOffset, 1);
                edgeIndexEntry.setLevel0EdgeIndex(level0EdgeIndex);
            } else {
                // 이미 있음: 카운트만 증가
                edgeIndexEntry.getLevel0EdgeIndex().setEdgeCount(
                    edgeIndexEntry.getLevel0EdgeIndex().getEdgeCount() + 1
                );
            }
        } 
        else if (roadLevel == RoadLevel.L1) {
            if (edgeIndexEntry.getLevel1EdgeIndex().getStartOffset() == -1) {
                LevelEdgeIndex level1EdgeIndex = new LevelEdgeIndex(RoadLevel.L1, edgeOffset, 1);
                edgeIndexEntry.setLevel1EdgeIndex(level1EdgeIndex);
            } else {
                edgeIndexEntry.getLevel1EdgeIndex().setEdgeCount(
                    edgeIndexEntry.getLevel1EdgeIndex().getEdgeCount() + 1
                );
            }
        } 
        else if (roadLevel == RoadLevel.L2) {
            if (edgeIndexEntry.getLevel2EdgeIndex().getStartOffset() == -1) {
                LevelEdgeIndex level2EdgeIndex = new LevelEdgeIndex(RoadLevel.L2, edgeOffset, 1);
                edgeIndexEntry.setLevel2EdgeIndex(level2EdgeIndex);
            } else {
                edgeIndexEntry.getLevel2EdgeIndex().setEdgeCount(
                    edgeIndexEntry.getLevel2EdgeIndex().getEdgeCount() + 1
                );
            }
        }
    }

    private interface EdgeNodeSelector {
        int select(Edge edge);
    }

    private interface EdgeReader {
        Edge read(long offset) throws IOException;
    }
}
