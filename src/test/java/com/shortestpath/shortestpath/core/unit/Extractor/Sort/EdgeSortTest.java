package com.shortestpath.shortestpath.core.unit.Extractor.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Sort.EdgeSort;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

public class EdgeSortTest {

    private DataStore dataStore;
    private EdgeSort edgeSort;
    private List<Edge> edgeStorage; // 메모리에서 엣지를 저장하는 리스트
    private MockedStatic<DataStructureSizes> dataStructureSizesMock;
    private static final int EDGE_SIZE = 33; // EdgeSort에서 사용하는 EDGE_SIZE

    @BeforeEach
    public void setUp() {
        dataStore = mock(DataStore.class);
        edgeStorage = new ArrayList<>();
        
        dataStructureSizesMock = mockStatic(DataStructureSizes.class);
    }
    
    @AfterEach
    public void tearDown() {
        if (dataStructureSizesMock != null) {
            dataStructureSizesMock.close();
        }
    }

    /**
     * DataStore의 동작을 시뮬레이션하는 헬퍼 메서드
     */
    private void setupDataStoreMock(int totalEdges) throws IOException {
        when(dataStore.getTotalEdges()).thenReturn(totalEdges);

        // DataStructureSizes.calculateEdgeOffset을 mock하여 간단한 계산으로 변경
        // 실제: HEADER_SIZE(5) + index * EDGE_SIZE(26)
        // 테스트: index만 반환 (간소화)
        dataStructureSizesMock.when(() -> DataStructureSizes.calculateEdgeOffset(anyInt()))
            .thenAnswer((Answer<Long>) invocation -> {
                int index = invocation.getArgument(0);
                return (long) index;
            });
                    
        // readEdge mock: offset을 기반으로 리스트에서 엣지 반환
        when(dataStore.readEdge(anyLong())).thenAnswer(new Answer<Edge>() {
            @Override
            public Edge answer(InvocationOnMock invocation) throws Throwable {
                long offset = invocation.getArgument(0);
                // offset = index로 간소화했으므로 직접 사용
                int index = (int) offset;
                if (index >= 0 && index < edgeStorage.size()) {
                    Edge edge = edgeStorage.get(index);
                    return new Edge(edge.getId(), edge.getFrom(), edge.getTo(), edge.getDistance(), edge.getNextEdgeOffset(), edge.getSpeed(), edge.getRoadLevel());
                }
                return null;
            }
        });

        // overwriteEdge mock: offset 위치에 엣지 쓰기
        when(dataStore.overwriteEdge(any(Edge.class), anyLong())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                Edge edge = invocation.getArgument(0);
                long offset = invocation.getArgument(1);
                // offset = index로 간소화했으므로 직접 사용
                int index = (int) offset;
                if (index >= 0 && index < edgeStorage.size()) {
                    edgeStorage.set(index, new Edge(edge.getId(), edge.getFrom(), edge.getTo(), edge.getDistance(), edge.getNextEdgeOffset(), edge.getSpeed(), edge.getRoadLevel()));
                }
                return EDGE_SIZE;
            }
        });
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
    @DisplayName("EdgeSort 정렬 테스트 - 빈 엣지 리스트")
    public void sortEmptyEdgesTest() throws IOException {
        setupDataStoreMock(0);
        EdgeSort edgeSort = new EdgeSort(dataStore);

        edgeSort.sort();

        verify(dataStore, times(1)).getTotalEdges();
        verify(dataStore, times(0)).readEdge(anyLong());
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - 단일 엣지")
    public void sortSingleEdgeTest() throws IOException {
        edgeStorage.add(new Edge(1, 10, 20, 100.0, -1, 100, RoadLevel.L0));
        setupDataStoreMock(1);
        EdgeSort edgeSort = new EdgeSort(dataStore);

        edgeSort.sort();

        assertThat(edgeStorage).hasSize(1);
        assertThat(edgeStorage.get(0).getFrom()).isEqualTo(10);
        assertThat(edgeStorage.get(0).getTo()).isEqualTo(20);
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - from 기준으로 정렬")
    public void sortByFromNodeTest() throws IOException {
        edgeStorage.add(new Edge(1, 50, 60, 100.0, -1, 100, RoadLevel.L0));
        edgeStorage.add(new Edge(2, 30, 40, 200.0, -1, 60, RoadLevel.L1));
        edgeStorage.add(new Edge(3, 10, 20, 150.0, -1, 40, RoadLevel.L2));
        edgeStorage.add(new Edge(4, 40, 50, 180.0, -1, 100, RoadLevel.L0));
        edgeStorage.add(new Edge(5, 20, 30, 120.0, -1, 60, RoadLevel.L1));

        setupDataStoreMock(5);
        EdgeSort edgeSort = new EdgeSort(dataStore);

        edgeSort.sort();

        assertThat(edgeStorage).hasSize(5);
        assertThat(edgeStorage.get(0).getFrom()).isEqualTo(10);
        assertThat(edgeStorage.get(1).getFrom()).isEqualTo(20);
        assertThat(edgeStorage.get(2).getFrom()).isEqualTo(30);
        assertThat(edgeStorage.get(3).getFrom()).isEqualTo(40);
        assertThat(edgeStorage.get(4).getFrom()).isEqualTo(50);
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - from이 같을 때 RoadLevel 기준으로 정렬")
    public void sortByRoadLevelWhenFromIsSameTest() throws IOException {
        edgeStorage.add(new Edge(1, 10, 50, 100.0, -1, 40, RoadLevel.L2));
        edgeStorage.add(new Edge(2, 10, 30, 200.0, -1, 100, RoadLevel.L0));
        edgeStorage.add(new Edge(3, 10, 10, 150.0, -1, 60, RoadLevel.L1));
        edgeStorage.add(new Edge(4, 10, 40, 180.0, -1, 40, RoadLevel.L2));
        edgeStorage.add(new Edge(5, 10, 20, 120.0, -1, 100, RoadLevel.L0));

        setupDataStoreMock(5);
        EdgeSort edgeSort = new EdgeSort(dataStore);

        edgeSort.sort();

        assertThat(edgeStorage).hasSize(5);
        assertThat(edgeStorage.get(0).getFrom()).isEqualTo(10);
        assertThat(edgeStorage.get(0).getRoadLevel()).isEqualTo(RoadLevel.L0);
        assertThat(edgeStorage.get(1).getFrom()).isEqualTo(10);
        assertThat(edgeStorage.get(1).getRoadLevel()).isEqualTo(RoadLevel.L0);
        assertThat(edgeStorage.get(2).getFrom()).isEqualTo(10);
        assertThat(edgeStorage.get(2).getRoadLevel()).isEqualTo(RoadLevel.L1);
        assertThat(edgeStorage.get(3).getFrom()).isEqualTo(10);
        assertThat(edgeStorage.get(3).getRoadLevel()).isEqualTo(RoadLevel.L2);
        assertThat(edgeStorage.get(4).getFrom()).isEqualTo(10);
        assertThat(edgeStorage.get(4).getRoadLevel()).isEqualTo(RoadLevel.L2);
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - from과 RoadLevel 혼합 정렬")
    public void sortByFromAndRoadLevelTest() throws IOException {
        edgeStorage.add(new Edge(1, 20, 30, 100.0, -1, 40, RoadLevel.L2));
        edgeStorage.add(new Edge(2, 10, 50, 200.0, -1, 60, RoadLevel.L1));
        edgeStorage.add(new Edge(3, 20, 10, 150.0, -1, 100, RoadLevel.L0));
        edgeStorage.add(new Edge(4, 10, 20, 180.0, -1, 100, RoadLevel.L0));
        edgeStorage.add(new Edge(5, 30, 40, 120.0, -1, 60, RoadLevel.L1));
        edgeStorage.add(new Edge(6, 10, 30, 220.0, -1, 40, RoadLevel.L2));
        edgeStorage.add(new Edge(7, 20, 20, 160.0, -1, 60, RoadLevel.L1));

        setupDataStoreMock(7);
        EdgeSort edgeSort = new EdgeSort(dataStore);

        edgeSort.sort();

        assertThat(edgeStorage).hasSize(7);

        // from = 10 (L0, L1, L2 순서)
        assertThat(edgeStorage.get(0).getFrom()).isEqualTo(10);
        assertThat(edgeStorage.get(0).getRoadLevel()).isEqualTo(RoadLevel.L0);
        assertThat(edgeStorage.get(1).getFrom()).isEqualTo(10);
        assertThat(edgeStorage.get(1).getRoadLevel()).isEqualTo(RoadLevel.L1);
        assertThat(edgeStorage.get(2).getFrom()).isEqualTo(10);
        assertThat(edgeStorage.get(2).getRoadLevel()).isEqualTo(RoadLevel.L2);

        // from = 20 (L0, L1, L2 순서)
        assertThat(edgeStorage.get(3).getFrom()).isEqualTo(20);
        assertThat(edgeStorage.get(3).getRoadLevel()).isEqualTo(RoadLevel.L0);
        assertThat(edgeStorage.get(4).getFrom()).isEqualTo(20);
        assertThat(edgeStorage.get(4).getRoadLevel()).isEqualTo(RoadLevel.L1);
        assertThat(edgeStorage.get(5).getFrom()).isEqualTo(20);
        assertThat(edgeStorage.get(5).getRoadLevel()).isEqualTo(RoadLevel.L2);

        // from = 30
        assertThat(edgeStorage.get(6).getFrom()).isEqualTo(30);
        assertThat(edgeStorage.get(6).getRoadLevel()).isEqualTo(RoadLevel.L1);
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - 이미 정렬된 엣지")
    public void sortAlreadySortedEdgesTest() throws IOException {
        edgeStorage.add(new Edge(1, 10, 20, 100.0, -1, 100, RoadLevel.L0));
        edgeStorage.add(new Edge(2, 10, 30, 200.0, -1, 60, RoadLevel.L1));
        edgeStorage.add(new Edge(4, 20, 20, 180.0, -1, 100, RoadLevel.L0));
        edgeStorage.add(new Edge(3, 20, 10, 150.0, -1, 40, RoadLevel.L2));
        edgeStorage.add(new Edge(5, 30, 40, 120.0, -1, 60, RoadLevel.L1));

        setupDataStoreMock(5);
        EdgeSort edgeSort = new EdgeSort(dataStore);

        edgeSort.sort();

        assertThat(edgeStorage).hasSize(5);
        assertThat(edgeStorage.get(0).getId()).isEqualTo(1);
        assertThat(edgeStorage.get(1).getId()).isEqualTo(2);
        assertThat(edgeStorage.get(2).getId()).isEqualTo(4);
        assertThat(edgeStorage.get(3).getId()).isEqualTo(3);
        assertThat(edgeStorage.get(4).getId()).isEqualTo(5);
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - 역순으로 정렬된 엣지")
    public void sortReverseSortedEdgesTest() throws IOException {
        edgeStorage.add(new Edge(1, 50, 60, 100.0, -1, 100, RoadLevel.L0));
        edgeStorage.add(new Edge(2, 40, 50, 200.0, -1, 60, RoadLevel.L1));
        edgeStorage.add(new Edge(3, 30, 40, 150.0, -1, 40, RoadLevel.L2));
        edgeStorage.add(new Edge(4, 20, 30, 180.0, -1, 100, RoadLevel.L0));
        edgeStorage.add(new Edge(5, 10, 20, 120.0, -1, 60, RoadLevel.L1));

        setupDataStoreMock(5);
        EdgeSort edgeSort = new EdgeSort(dataStore);

        edgeSort.sort();

        assertThat(edgeStorage).hasSize(5);
        assertThat(edgeStorage.get(0).getFrom()).isEqualTo(10);
        assertThat(edgeStorage.get(0).getId()).isEqualTo(5);
        assertThat(edgeStorage.get(1).getFrom()).isEqualTo(20);
        assertThat(edgeStorage.get(1).getId()).isEqualTo(4);
        assertThat(edgeStorage.get(2).getFrom()).isEqualTo(30);
        assertThat(edgeStorage.get(2).getId()).isEqualTo(3);
        assertThat(edgeStorage.get(3).getFrom()).isEqualTo(40);
        assertThat(edgeStorage.get(3).getId()).isEqualTo(2);
        assertThat(edgeStorage.get(4).getFrom()).isEqualTo(50);
        assertThat(edgeStorage.get(4).getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("EdgeSort 정렬 테스트 - 동일한 from을 가지지만 다른 RoadLevel을 가진 엣지")
    public void sortDuplicateFromWithDifferentRoadLevelTest() throws IOException {
        edgeStorage.add(new Edge(1, 10, 20, 100.0, -1, 40, RoadLevel.L2));
        edgeStorage.add(new Edge(2, 10, 20, 200.0, -1, 100, RoadLevel.L0));
        edgeStorage.add(new Edge(3, 10, 20, 150.0, -1, 60, RoadLevel.L1));

        setupDataStoreMock(3);
        EdgeSort edgeSort = new EdgeSort(dataStore);
        edgeSort.sort();

        assertThat(edgeStorage).hasSize(3);
        assertThat(edgeStorage.get(0).getFrom()).isEqualTo(10);
        assertThat(edgeStorage.get(0).getRoadLevel()).isEqualTo(RoadLevel.L0);
        assertThat(edgeStorage.get(1).getFrom()).isEqualTo(10);
        assertThat(edgeStorage.get(1).getRoadLevel()).isEqualTo(RoadLevel.L1);
        assertThat(edgeStorage.get(2).getFrom()).isEqualTo(10);
        assertThat(edgeStorage.get(2).getRoadLevel()).isEqualTo(RoadLevel.L2);
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

        for (int i = 0; i < 20; i++) {
            edgeStorage.add(new Edge(i + 1, fromNodes[i], toNodes[i], 100.0 * i, -1, 0, roadLevels[i]));
        }

        setupDataStoreMock(20);
        EdgeSort edgeSort = new EdgeSort(dataStore);

        edgeSort.sort();

        assertThat(edgeStorage).hasSize(20);
        for (int i = 0; i < edgeStorage.size() - 1; i++) {
            Edge current = edgeStorage.get(i);
            Edge next = edgeStorage.get(i + 1);

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
