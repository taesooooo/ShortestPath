package com.shortestpath.shortestpath.pathengine.intergration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.shortestpath.TestApplication;
import com.shortestpath.shortestpath.TestRootContext;
import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.RouteSearchResult;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.FileBasedEdgeIndex;


@ActiveProfiles("inte")
@SpringBootTest(classes=TestApplication.class)
@Transactional
public class InteEngineTest {
    // @Autowired
    // DataStore dataStore;
    // @Autowired
    // Extractor extractor;
    // @Autowired
    // Loader loader;
    @Autowired
    Engine engine; 
    
    @BeforeEach
    public void setUp() throws IOException {
        
    }


    // @Test
    // @DisplayName("경로 탐색 - 정상 탐색")
    // public void findPathTestByNode() throws IOException {
    //     ArrayList<Coordinate> coordinateList = new ArrayList<Coordinate>();
    //     coordinateList.add(new Coordinate(33.2403307, 126.5624673));
    //     coordinateList.add(new Coordinate(33.2403234, 126.5627931));
    //     coordinateList.add(new Coordinate(33.2402282, 126.5630821));
    //     coordinateList.add(new Coordinate(33.2401702, 126.5632367));
    //     coordinateList.add(new Coordinate(33.2399523, 126.5638167));
    //     coordinateList.add(new Coordinate(33.2398888, 126.5640292));
    //     coordinateList.add(new Coordinate(33.2398754, 126.5640982));
    //     coordinateList.add(new Coordinate(33.2400544, 126.5642293));
    //     coordinateList.add(new Coordinate(33.2402428, 126.5643355));
    //     coordinateList.add(new Coordinate(33.2408074, 126.5644749));
    //     coordinateList.add(new Coordinate(33.2417782, 126.5647375));

    //     Coordinate startCoordinate = new Coordinate(33.2403307, 126.5624673);
    //     Coordinate endCoordinate = new Coordinate(33.2417782, 126.5647375);

    //     RouteSearchResult searchResult = engine.shortestPathFind(startCoordinate, endCoordinate, false);
    //     ArrayList<Node> findPath = searchResult.getRouteNode();

    //     findPath.forEach(item -> System.out.println(item.getCoordinate().toWKT()));

    //     assertThat(findPath).extracting(Node::getCoordinate)
    //             .usingRecursiveComparison()
    //             .isEqualTo(coordinateList);
    // }

    @Test
    @DisplayName("양방향 경로 탐색 - 제주 장거리 정방향/역방향 정상 탐색")
    public void bidirectionalPathFindForwardAndReverseTest() throws IOException {
        Coordinate startCoordinate = new Coordinate(33.22155, 126.25198);
        Coordinate endCoordinate = new Coordinate(33.52386, 126.85794);

        RouteSearchResult forwardResult = engine.shortestPathFind(startCoordinate, endCoordinate, false);
        RouteSearchResult reverseResult = engine.shortestPathFind(endCoordinate, startCoordinate, false);

        ArrayList<Node> forwardPath = forwardResult.getRouteNode();
        ArrayList<Node> reversePath = reverseResult.getRouteNode();

        System.out.println("forward bidirectional search time = " + forwardResult.getSearchTime());
        System.out.println("reverse bidirectional search time = " + reverseResult.getSearchTime());
        System.out.println("forward path size = " + (forwardPath != null ? forwardPath.size() : 0));
        System.out.println("reverse path size = " + (reversePath != null ? reversePath.size() : 0));

        assertThat(forwardPath).isNotNull();
        assertThat(reversePath).isNotNull();
        assertThat(forwardPath).hasSizeGreaterThan(1);
        assertThat(reversePath).hasSizeGreaterThan(1);
        assertThat(forwardPath.get(0).getCoordinate()).isEqualTo(reversePath.get(reversePath.size() - 1).getCoordinate());
        assertThat(forwardPath.get(forwardPath.size() - 1).getCoordinate()).isEqualTo(reversePath.get(0).getCoordinate());
        assertPathEdgesAreConnected(forwardPath);
        assertPathEdgesAreConnected(reversePath);

        assertThat(forwardResult.getSearchTime()).isGreaterThanOrEqualTo(0);
        assertThat(reverseResult.getSearchTime()).isGreaterThanOrEqualTo(0);
    }

    private void assertPathEdgesAreConnected(ArrayList<Node> path) throws IOException {
        for(int i = 0; i < path.size() - 1; i++) {
            int fromNodeId = path.get(i).getId();
            int toNodeId = path.get(i + 1).getId();

            assertThat(hasForwardEdge(fromNodeId, toNodeId))
                    .as("%s 노드에서 %s 노드로 이어지는 엣지가 존재해야 합니다.", fromNodeId, toNodeId)
                    .isTrue();
        }
    }

    private boolean hasForwardEdge(int fromNodeId, int toNodeId) throws IOException {
        FileBasedEdgeIndex edgeIndex = (FileBasedEdgeIndex) engine.getStore().getEdgeIndex();

        for(RoadLevel roadLevel : RoadLevel.values()) {
            int edgeCount = edgeIndex.viewEdgeCount(fromNodeId, roadLevel);
            if(edgeCount == 0) {
                continue;
            }

            long startOffset = edgeIndex.viewStartOffset(fromNodeId, roadLevel);
            for(int i = 0; i < edgeCount; i++) {
                Edge edge = engine.getStore().readEdge(startOffset + (i * DataStructureSizes.EDGE_SIZE));
                if(edge.getTo() == toNodeId) {
                    return true;
                }
            }
        }

        return false;
    }

    // @Test
    // @DisplayName("경로 탐색 - 연결이 끊어져 있어 탐색이 불가한 경우")
    // public void findPathTestDisconnectNode() throws IOException {
    //     // 126.56571449999998,33.2601044
    //     // 126.5662567,33.257629
    //     Coordinate startCoordinate = new Coordinate(33.2601044, 126.56571449999998);
    //     Coordinate endCoordinate = new Coordinate(33.257629, 126.5662567);

    //     RouteSearchResult searchResult = engine.shortestPathFind(startCoordinate, endCoordinate, false);

    //     assertThat(searchResult.getRouteNode()).isNull();
    // }
}
