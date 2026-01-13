package com.shortestpath.shortestpath.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.EmptyGeometryListException;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.RouteSearchResult;
import com.shortestpath.shortestpath.core.pathengine.RouteTracker;
import com.shortestpath.shortestpath.core.pathengine.TraceRoute;
import com.shortestpath.shortestpath.dto.request.RequestFindPathDto;
import com.shortestpath.shortestpath.dto.response.ResponeseRouteSearchTraceDto;
import com.shortestpath.shortestpath.dto.response.ResponseFindPathDto;
import com.shortestpath.shortestpath.service.MapServiceImpl;

class MapServiceTest {

    @Mock
    private Engine engine;

    @InjectMocks
    private MapServiceImpl mapService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("정상적인 값으로 반환하는지 테스트")
    void findPathTest() throws Exception {
        ArrayList<Node> testNodes = createSimpleTestNodes();
        List<RequestFindPathDto> testRequestList = new ArrayList<>();
        testRequestList.add(
                RequestFindPathDto.builder()
                        .start(new Coordinate(0, 0))
                        .end(new Coordinate(3, 3))
                        .build());

        when(engine.shortestPathFind(any(Coordinate.class), any(Coordinate.class), eq(false)))
                .thenReturn(new RouteSearchResult(testNodes, 0.0));

        List<ResponseFindPathDto> responseFindPathDto = mapService.findPath(testRequestList);

        assertThat(responseFindPathDto)
                .isNotNull()
                .isNotEmpty()
                .hasSize(1);

        // 경로의 좌표 리스트 검증
        assertThat(responseFindPathDto.get(0).getRouteList())
                .as("정상적인 경로 반환을 하지 못했습니다.")
                .containsExactly(
                        new Coordinate(0, 0),
                        new Coordinate(1, 1),
                        new Coordinate(2, 2),
                        new Coordinate(3, 3));

        // 시작점과 도착점 검증
        assertThat(responseFindPathDto.get(0).getStart())
                .isEqualTo(new Coordinate(0, 0));
        assertThat(responseFindPathDto.get(0).getEnd())
                .isEqualTo(new Coordinate(3, 3));
    }

    @Test
    @DisplayName("빈 경로 반환 테스트")
    void findPathEmptyTest() throws Exception {
        ArrayList<Node> emptyNodes = new ArrayList<>();
        List<RequestFindPathDto> testRequestList = new ArrayList<>();
        testRequestList.add(
                RequestFindPathDto.builder()
                        .start(new Coordinate(0, 0))
                        .end(new Coordinate(1, 1))
                        .build());

        when(engine.shortestPathFind(any(Coordinate.class), any(Coordinate.class), eq(false)))
                .thenReturn(new RouteSearchResult(emptyNodes, 0.0));

        List<ResponseFindPathDto> responseFindPathDto = mapService.findPath(testRequestList);

        assertThat(responseFindPathDto.get(0).getRouteList())
                .isEmpty();
    }

    @Test
    @DisplayName("경로를 찾을 수 없는 경우 테스트")
    void findPathNotFoundTest() throws Exception {
        List<RequestFindPathDto> testRequestList = new ArrayList<>();
        testRequestList.add(
                RequestFindPathDto.builder()
                        .start(new Coordinate(0, 0))
                        .end(new Coordinate(5, 5))
                        .build());

        when(engine.shortestPathFind(any(Coordinate.class), any(Coordinate.class), eq(false)))
                .thenThrow(new EmptyGeometryListException("지오메트리를 가져올 수 없습니다."));

        List<ResponseFindPathDto> responseFindPathDto = mapService.findPath(testRequestList);

        assertThat(responseFindPathDto.get(0).getRouteList())
                .isEmpty();
    }

    @Test
    @DisplayName("여러 경로 요청 테스트")
    void findPathMultipleTest() throws Exception {
        ArrayList<Node> testNodes1 = createSimpleTestNodes();
        ArrayList<Node> testNodes2 = createSimpleTestNodes2();

        List<RequestFindPathDto> testRequestList = new ArrayList<>();
        testRequestList.add(
                RequestFindPathDto.builder()
                        .start(new Coordinate(0, 0))
                        .end(new Coordinate(3, 3))
                        .build());
        testRequestList.add(
                RequestFindPathDto.builder()
                        .start(new Coordinate(1, 1))
                        .end(new Coordinate(4, 4))
                        .build());

        when(engine.shortestPathFind(any(Coordinate.class), any(Coordinate.class), eq(false)))
                .thenReturn(new RouteSearchResult(testNodes1, 0.0))
                .thenReturn(new RouteSearchResult(testNodes2, 0.0));

        List<ResponseFindPathDto> responseFindPathDto = mapService.findPath(testRequestList);

        assertThat(responseFindPathDto)
                .hasSize(2);
        assertThat(responseFindPathDto.get(0).getRouteList())
                .hasSize(4);
        assertThat(responseFindPathDto.get(1).getRouteList())
                .hasSize(4);
    }

    @Test
    @DisplayName("여러 경로 요청 - 성공과 예외 둘다 테스트")
    void findPathMultipleMixTest() throws Exception {
        ArrayList<Node> testNodes1 = createSimpleTestNodes();
        // ArrayList<Node> testNodes2 = createSimpleTestNodes2();

        List<RequestFindPathDto> testRequestList = new ArrayList<>();
        testRequestList.add(
                RequestFindPathDto.builder()
                        .start(new Coordinate(0, 0))
                        .end(new Coordinate(3, 3))
                        .build());
        testRequestList.add(
                RequestFindPathDto.builder()
                        .start(new Coordinate(1, 1))
                        .end(new Coordinate(4, 4))
                        .build());

        when(engine.shortestPathFind(any(Coordinate.class), any(Coordinate.class), eq(false)))
                .thenReturn(new RouteSearchResult(testNodes1, 0.0))
                .thenThrow(new EmptyGeometryListException("지오메트리를 가져올 수 없습니다."));

        List<ResponseFindPathDto> responseFindPathDto = mapService.findPath(testRequestList);

        assertThat(responseFindPathDto).hasSize(2);
        assertThat(responseFindPathDto.get(0).getRouteList()).hasSize(4);
        assertThat(responseFindPathDto.get(1).getRouteList()).isEmpty();
    }

    @Test
    @DisplayName("시작점과 도착점이 같은 경우")
    void findPathSameStartEndTest() throws Exception {
        // Given: 시작과 도착이 같은 경우
        ArrayList<Node> singleNode = new ArrayList<>();
        singleNode.add(new Node(0, new Coordinate(2, 2), -1, 0.0, 0.0, 0.0));

        List<RequestFindPathDto> testRequestList = new ArrayList<>();
        testRequestList.add(
                RequestFindPathDto.builder()
                        .start(new Coordinate(2, 2))
                        .end(new Coordinate(2, 2))
                        .build());

        when(engine.shortestPathFind(any(Coordinate.class), any(Coordinate.class), eq(false)))
                .thenReturn(new RouteSearchResult(singleNode, 0.0));

        // When
        List<ResponseFindPathDto> responseFindPathDto = mapService.findPath(testRequestList);

        // Then
        assertThat(responseFindPathDto.get(0).getRouteList())
                .hasSize(1)
                .containsExactly(new Coordinate(2, 2));
    }

    @Test
    @DisplayName("경로탐색추적 - 정상")
    public void searchRouteTrackTest() throws Exception {
        ArrayList<Node> testNodes = createSimpleTestNodes();

        // RouteTracker와 TraceRoute 객체 생성
        RouteTracker routeTracker = new RouteTracker();
        routeTracker.addTraceRoute(new TraceRoute(new Coordinate(0, 0), Arrays.asList(new Coordinate(1, 1))));
        routeTracker.addTraceRoute(new TraceRoute(new Coordinate(1, 1), Arrays.asList(new Coordinate(2, 2))));
        routeTracker.addTraceRoute(new TraceRoute(new Coordinate(2, 2), Arrays.asList(new Coordinate(3, 3))));
        routeTracker.addTraceRoute(new TraceRoute(new Coordinate(3, 3), new ArrayList<>()));

        when(engine.shortestPathFind(any(Coordinate.class), any(Coordinate.class), eq(true)))
                .thenReturn(new RouteSearchResult(testNodes, routeTracker, 0.0));

        RequestFindPathDto requestDto = RequestFindPathDto.builder()
                .start(new Coordinate(0, 0))
                .end(new Coordinate(3, 3))
                .build();

        ResponeseRouteSearchTraceDto responseDto = mapService.searchRouteTrack(requestDto);

        assertThat(responseDto.getRouteCoordinates())
                .as("정상적인 경로 반환을 하지 못했습니다.")
                .containsExactly(
                        new Coordinate(0, 0),
                        new Coordinate(1, 1),
                        new Coordinate(2, 2),
                        new Coordinate(3, 3));

        assertThat(responseDto.getTraceRoutes())
                .as("정상적인 탐색 좌표 반환을 하지 못했습니다.")
                .extracting(TraceRoute::getParentCoordinate)
                .extracting(Coordinate::getLatitude, Coordinate::getLongitude)
                .containsExactly(tuple(0.0, 0.0), tuple(1.0, 1.0), tuple(2.0, 2.0), tuple(3.0, 3.0));

        assertThat((responseDto.getTraceRoutes()))
                .as("정상적인 방문 좌표를 반환하지 못했습니다.")
                .flatExtracting(TraceRoute::getVisitedCoordinates)
                .extracting(Coordinate::getLatitude, Coordinate::getLongitude)
                .containsExactly(tuple(1.0, 1.0), tuple(2.0, 2.0), tuple(3.0, 3.0));
    }

    /**
     * 간단한 테스트용 노드 생성 (0,0) → (1,1) → (2,2) → (3,3)
     */
    private ArrayList<Node> createSimpleTestNodes() {
        ArrayList<Node> nodes = new ArrayList<>();
        int[][] points = { { 0, 0 }, { 1, 1 }, { 2, 2 }, { 3, 3 } };

        for (int i = 0; i < points.length; i++) {
            Node node = new Node(
                    i,
                    new Coordinate(points[i][0], points[i][1]),
                    -1,
                    0.0, 0.0, 0.0);
            nodes.add(node);
        }

        return nodes;
    }

    /**
     * 간단한 테스트용 노드 생성 (1,1) → (2,2) → (3,3) → (4,4)
     */
    private ArrayList<Node> createSimpleTestNodes2() {
        ArrayList<Node> nodes = new ArrayList<>();
        int[][] points = { { 1, 1 }, { 2, 2 }, { 3, 3 }, { 4, 4 } };

        for (int i = 0; i < points.length; i++) {
            Node node = new Node(
                    i + 1,
                    new Coordinate(points[i][0], points[i][1]),
                    -1,
                    0.0, 0.0, 0.0);
            nodes.add(node);
        }

        return nodes;
    }
}