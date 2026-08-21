package com.shortestpath.shortestpath.core.unit.Extractor.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Sort.EdgeSort;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.EdgeHeader;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;

public class EdgeSortTest {

    private DataStore dataStore;   
    @TempDir 
    Path fileDirectory;

    @BeforeEach
    public void setUp() throws IOException {
        dataStore = new HybridDataStore(fileDirectory.toString());
    }

    @Test
    @DisplayName("EdgeSort 생성 테스트 - 정상 생성")
    public void edgeSortConstructorTest() {
        EdgeSort edgeSort = new EdgeSort(dataStore);

        assertThat(edgeSort).isNotNull();
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - DataStore가 null인 경우 예외 발생")
    public void sortWithNullDataStoreTest() {
        EdgeSort edgeSort = new EdgeSort(null);

        assertThatThrownBy(() -> edgeSort.sort())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DataStore가 초기화되지 않았습니다");
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - 단일 엣지")
    public void sortSingleEdgeTest() throws IOException {
        dataStore.writeEdgeHeader(new EdgeHeader(1, false));
        dataStore.allocateEdgeFileSpace(DataStructureSizes.HEADER_SIZE + DataStructureSizes.EDGE_SIZE);
        dataStore.saveEdge(new Edge(0, 10, 20, 100.0, -1, 100, RoadLevel.L0));
        
        EdgeSort edgeSort = new EdgeSort(dataStore);
        edgeSort.sort();

        Edge resultEdge = dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(0));
        assertThat(resultEdge.getFrom()).isEqualTo(10);
        assertThat(resultEdge.getTo()).isEqualTo(20);
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - from 기준으로 정렬")
    public void sortByFromNodeTest() throws IOException {
        dataStore.writeEdgeHeader(new EdgeHeader(5, false));
        dataStore.allocateEdgeFileSpace(DataStructureSizes.HEADER_SIZE+ (DataStructureSizes.EDGE_SIZE * 5));
        dataStore.saveEdge(new Edge(0, 50, 60, 100.0, -1, 100, RoadLevel.L0));
        dataStore.saveEdge(new Edge(1, 30, 40, 200.0, -1, 60, RoadLevel.L1));
        dataStore.saveEdge(new Edge(2, 10, 20, 150.0, -1, 40, RoadLevel.L2));
        dataStore.saveEdge(new Edge(3, 40, 50, 180.0, -1, 100, RoadLevel.L0));
        dataStore.saveEdge(new Edge(4, 20, 30, 120.0, -1, 60, RoadLevel.L1));

        EdgeSort edgeSort = new EdgeSort(dataStore);

        edgeSort.sort();

        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(0)).getFrom()).isEqualTo(10);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(1)).getFrom()).isEqualTo(20);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(2)).getFrom()).isEqualTo(30);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(3)).getFrom()).isEqualTo(40);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(4)).getFrom()).isEqualTo(50);
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - from이 같을 때 RoadLevel 기준으로 정렬")
    public void sortByRoadLevelWhenFromIsSameTest() throws IOException {
        dataStore.writeEdgeHeader(new EdgeHeader(5, false));
        dataStore.allocateEdgeFileSpace(DataStructureSizes.HEADER_SIZE + (DataStructureSizes.EDGE_SIZE * 5));
        dataStore.saveEdge(new Edge(0, 10, 50, 100.0, -1, 40, RoadLevel.L2));
        dataStore.saveEdge(new Edge(1, 10, 30, 200.0, -1, 100, RoadLevel.L0));
        dataStore.saveEdge(new Edge(2, 10, 10, 150.0, -1, 60, RoadLevel.L1));
        dataStore.saveEdge(new Edge(3, 10, 40, 180.0, -1, 40, RoadLevel.L2));
        dataStore.saveEdge(new Edge(4, 10, 20, 120.0, -1, 100, RoadLevel.L0));

        EdgeSort edgeSort = new EdgeSort(dataStore);
        edgeSort.sort();

        // from이 모두 10이므로 RoadLevel 순서대로 정렬되어야 함 (L0, L0, L1, L2, L2)
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(0)).getRoadLevel()).isEqualTo(RoadLevel.L0);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(1)).getRoadLevel()).isEqualTo(RoadLevel.L0);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(2)).getRoadLevel()).isEqualTo(RoadLevel.L1);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(3)).getRoadLevel()).isEqualTo(RoadLevel.L2);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(4)).getRoadLevel()).isEqualTo(RoadLevel.L2);
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - from과 RoadLevel 혼합 정렬")
    public void sortByFromAndRoadLevelTest() throws IOException {
        dataStore.writeEdgeHeader(new EdgeHeader(7, false));
        dataStore.allocateEdgeFileSpace(DataStructureSizes.HEADER_SIZE + (DataStructureSizes.EDGE_SIZE * 7));
        dataStore.saveEdge(new Edge(0, 20, 30, 100.0, -1, 40, RoadLevel.L2));
        dataStore.saveEdge(new Edge(1, 10, 50, 200.0, -1, 60, RoadLevel.L1));
        dataStore.saveEdge(new Edge(2, 20, 10, 150.0, -1, 100, RoadLevel.L0));
        dataStore.saveEdge(new Edge(3, 10, 20, 180.0, -1, 100, RoadLevel.L0));
        dataStore.saveEdge(new Edge(4, 30, 40, 120.0, -1, 60, RoadLevel.L1));
        dataStore.saveEdge(new Edge(5, 10, 30, 220.0, -1, 40, RoadLevel.L2));
        dataStore.saveEdge(new Edge(6, 20, 20, 160.0, -1, 60, RoadLevel.L1));

        EdgeSort edgeSort = new EdgeSort(dataStore);
        edgeSort.sort();

        // from = 10인 엣지들 (L0, L1, L2 순서)
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(0)).getFrom()).isEqualTo(10);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(0)).getRoadLevel()).isEqualTo(RoadLevel.L0);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(1)).getFrom()).isEqualTo(10);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(1)).getRoadLevel()).isEqualTo(RoadLevel.L1);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(2)).getFrom()).isEqualTo(10);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(2)).getRoadLevel()).isEqualTo(RoadLevel.L2);

        // from = 20인 엣지들 (L0, L1, L2 순서)
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(3)).getFrom()).isEqualTo(20);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(3)).getRoadLevel()).isEqualTo(RoadLevel.L0);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(4)).getFrom()).isEqualTo(20);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(4)).getRoadLevel()).isEqualTo(RoadLevel.L1);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(5)).getFrom()).isEqualTo(20);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(5)).getRoadLevel()).isEqualTo(RoadLevel.L2);

        // from = 30인 엣지
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(6)).getFrom()).isEqualTo(30);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(6)).getRoadLevel()).isEqualTo(RoadLevel.L1);
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - 이미 정렬된 엣지")
    public void shouldPreserveSortOrderForAlreadySortedEdges() throws IOException {
        dataStore.writeEdgeHeader(new EdgeHeader(5, false));
        dataStore.allocateEdgeFileSpace(DataStructureSizes.HEADER_SIZE + (DataStructureSizes.EDGE_SIZE * 5));
        dataStore.saveEdge(new Edge(0, 10, 20, 100.0, -1, 100, RoadLevel.L0));
        dataStore.saveEdge(new Edge(1, 10, 30, 200.0, -1, 60, RoadLevel.L1));
        dataStore.saveEdge(new Edge(2, 20, 20, 180.0, -1, 100, RoadLevel.L0));
        dataStore.saveEdge(new Edge(3, 20, 10, 150.0, -1, 40, RoadLevel.L2));
        dataStore.saveEdge(new Edge(4, 30, 40, 120.0, -1, 60, RoadLevel.L1));

        EdgeSort edgeSort = new EdgeSort(dataStore);
        edgeSort.sort();

        // from 기준으로 정렬되어야 함
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(0)).getFrom()).isEqualTo(10);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(1)).getFrom()).isEqualTo(10);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(2)).getFrom()).isEqualTo(20);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(3)).getFrom()).isEqualTo(20);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(4)).getFrom()).isEqualTo(30);
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - 역순으로 정렬된 엣지")
    public void shouldSortReverseSortedEdges() throws IOException {
        dataStore.writeEdgeHeader(new EdgeHeader(5, false));
        dataStore.allocateEdgeFileSpace(DataStructureSizes.HEADER_SIZE + (DataStructureSizes.EDGE_SIZE * 5));
        dataStore.saveEdge(new Edge(0, 50, 60, 100.0, -1, 100, RoadLevel.L0));
        dataStore.saveEdge(new Edge(1, 40, 50, 200.0, -1, 60, RoadLevel.L1));
        dataStore.saveEdge(new Edge(2, 30, 40, 150.0, -1, 40, RoadLevel.L2));
        dataStore.saveEdge(new Edge(3, 20, 30, 180.0, -1, 100, RoadLevel.L0));
        dataStore.saveEdge(new Edge(4, 10, 20, 120.0, -1, 60, RoadLevel.L1));

        EdgeSort edgeSort = new EdgeSort(dataStore);
        edgeSort.sort();

        // 역순 데이터가 오름차순으로 정렬되어야 함
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(0)).getFrom()).isEqualTo(10);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(1)).getFrom()).isEqualTo(20);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(2)).getFrom()).isEqualTo(30);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(3)).getFrom()).isEqualTo(40);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(4)).getFrom()).isEqualTo(50);
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - 동일한 from을 가지지만 다른 RoadLevel을 가진 엣지")
    public void sortDuplicateFromWithDifferentRoadLevelTest() throws IOException {
        dataStore.writeEdgeHeader(new EdgeHeader(3, false));
        dataStore.allocateEdgeFileSpace(DataStructureSizes.HEADER_SIZE + (DataStructureSizes.EDGE_SIZE * 3));
        dataStore.saveEdge(new Edge(0, 10, 20, 100.0, -1, 40, RoadLevel.L2));
        dataStore.saveEdge(new Edge(1, 10, 20, 200.0, -1, 100, RoadLevel.L0));
        dataStore.saveEdge(new Edge(2, 10, 20, 150.0, -1, 60, RoadLevel.L1));

        EdgeSort edgeSort = new EdgeSort(dataStore);
        edgeSort.sort();

        // 모두 from=10이므로 RoadLevel 순서대로 정렬 (L0, L1, L2)
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(0)).getRoadLevel()).isEqualTo(RoadLevel.L0);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(1)).getRoadLevel()).isEqualTo(RoadLevel.L1);
        assertThat(dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(2)).getRoadLevel()).isEqualTo(RoadLevel.L2);
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - 큰 데이터셋")
    public void sortLargeDatasetTest() throws IOException {
        int[] fromNodes = { 45, 23, 67, 12, 89, 34, 56, 78, 23, 45, 12, 67, 89, 34, 56, 78, 23, 12, 45, 67 };
        int[] toNodes = { 50, 30, 70, 20, 90, 40, 60, 80, 35, 55, 25, 75, 95, 45, 65, 85, 28, 15, 48, 72 };
        RoadLevel[] roadLevels = { RoadLevel.L0, RoadLevel.L2, RoadLevel.L1, RoadLevel.L2, RoadLevel.L0, 
                                   RoadLevel.L1, RoadLevel.L2, RoadLevel.L0, RoadLevel.L0, RoadLevel.L1,
                                   RoadLevel.L0, RoadLevel.L2, RoadLevel.L1, RoadLevel.L2, RoadLevel.L0,
                                   RoadLevel.L1, RoadLevel.L1, RoadLevel.L1, RoadLevel.L2, RoadLevel.L0 };

        dataStore.writeEdgeHeader(new EdgeHeader(20, false));
        dataStore.allocateEdgeFileSpace(DataStructureSizes.HEADER_SIZE + (DataStructureSizes.EDGE_SIZE * 20));
        
        for (int i = 0; i < 20; i++) {
            dataStore.saveEdge(new Edge(i, fromNodes[i], toNodes[i], 100.0 * i, -1, 0, roadLevels[i]));
        }
        
        EdgeSort edgeSort = new EdgeSort(dataStore);
        edgeSort.sort();

        // 정렬된 순서 검증
        for (int i = 0; i < 19; i++) {
            Edge current = dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(i));
            Edge next = dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(i + 1));

            // from이 같거나 증가해야 함
            int cmp = Integer.compare(current.getFrom(), next.getFrom());
            if (cmp == 0) {
                // from이 같으면 RoadLevel이 같거나 증가해야 함 (L0 < L1 < L2)
                assertThat(current.getRoadLevel().ordinal()).isLessThanOrEqualTo(next.getRoadLevel().ordinal());
            } else {
                assertThat(cmp).isLessThan(0);
            }
        }
    }
}
