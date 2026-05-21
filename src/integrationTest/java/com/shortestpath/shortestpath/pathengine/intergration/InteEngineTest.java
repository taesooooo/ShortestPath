package com.shortestpath.shortestpath.pathengine.intergration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.locationtech.jts.geom.Envelope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.shortestpath.shortestpath.IntegrationTestHelper;
import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.RouteSearchResult;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.FileBasedEdgeIndex;

@SpringJUnitConfig(InteEngineTest.EngineIntegrationConfig.class)
@TestPropertySource("classpath:application-inte.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class InteEngineTest {
    @Autowired
    DataStore dataStore;
    @Autowired
    Extractor extractor;
    @Autowired
    Loader loader;
    @Autowired
    Engine engine; 

    @TestConfiguration
    static class EngineIntegrationConfig {
        @Bean
        public DataStore dataStore(@Value("${findpath.shp-path}") String shpFilePath) throws Exception {
            String parentDir = new File(shpFilePath).getParent();

            HybridDataStore dataStore = new HybridDataStore(parentDir);
            dataStore.setEdgeIndex(new FileBasedEdgeIndex(parentDir));
            dataStore.setReverseEdgeIndex(new FileBasedEdgeIndex(new File(parentDir, "reverse_edge_index.bin").toPath()));

            return dataStore;
        }

        @Bean
        public Extractor extractor(@Value("${findpath.shp-path}") String shpFilePath, DataStore dataStore) throws IOException {
            return new NodeEdgeExtractor(shpFilePath, dataStore, false);
        }

        @Bean
        public Loader loader(Extractor extractor) throws IOException {
            return new Loader(extractor);
        }

        @Bean
        public NodeProvider fileNodeProvider(DataStore dataStore) {
            return new NodeProvider() {
                @Override
                public void insertNodeIndex(List<IndexInfo> indexList) {
                }

                @Override
                public int getNodeIndex(Coordinate coordinate) {
                    throw new UnsupportedOperationException("Engine integration test uses nearest-node lookup only.");
                }

                @Override
                public Coordinate getNearestNode(Envelope envelope, Coordinate coordinate) {
                    return findNearestNodeId(envelope, coordinate).stream()
                            .findFirst()
                            .map(nodeId -> readNodeCoordinate(dataStore, nodeId))
                            .orElseThrow(() -> new IllegalStateException("가장 가까운 노드를 찾을 수 없습니다."));
                }

                @Override
                public List<Integer> findNearestNodeId(Envelope envelope, Coordinate coordinate) {
                    try {
                        ArrayList<Node> candidates = new ArrayList<Node>();
                        ArrayList<Node> fallback = new ArrayList<Node>();
                        int totalNodes = dataStore.getTotalNodes();

                        for(int nodeId = 0; nodeId < totalNodes; nodeId++) {
                            Node node = dataStore.readNode(DataStructureSizes.calculateNodeOffset(nodeId));
                            fallback.add(node);
                            if(envelope.contains(node.getCoordinate().getLongitude(), node.getCoordinate().getLatitude())) {
                                candidates.add(node);
                            }
                        }

                        ArrayList<Node> searchNodes = candidates.isEmpty() ? fallback : candidates;
                        searchNodes.sort(Comparator.comparingDouble(node -> node.getCoordinate().calculateDistanceToTarget(coordinate)));

                        return searchNodes.stream()
                                .limit(5)
                                .map(Node::getId)
                                .toList();
                    }
                    catch(IOException e) {
                        throw new IllegalStateException("노드 바이너리에서 가까운 노드를 찾는 중 오류가 발생했습니다.", e);
                    }
                }

                private Coordinate readNodeCoordinate(DataStore dataStore, int nodeId) {
                    try {
                        return dataStore.readNode(DataStructureSizes.calculateNodeOffset(nodeId)).getCoordinate();
                    }
                    catch(IOException e) {
                        throw new IllegalStateException("노드 좌표를 읽는 중 오류가 발생했습니다.", e);
                    }
                }
            };
        }

        @Bean
        public Engine engine(DataStore dataStore, NodeProvider nodeProvider, Loader loader) throws IOException {
            loader.extractData(false);
            ((HybridDataStore) dataStore).switchToMappingMode();
            return new Engine(dataStore, nodeProvider);
        }
    }
    
    @BeforeAll
    public void setUp() throws IOException {
        assertThat(engine).isNotNull();
    }

    @AfterAll
    public void destroy() throws IOException {
        dataStore.close();
        IntegrationTestHelper.deleteBinaryFiles((HybridDataStore) dataStore);
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
        FileBasedEdgeIndex edgeIndex = (FileBasedEdgeIndex) dataStore.getEdgeIndex();

        for(RoadLevel roadLevel : RoadLevel.values()) {
            int edgeCount = edgeIndex.viewEdgeCount(fromNodeId, roadLevel);
            if(edgeCount == 0) {
                continue;
            }

            long startOffset = edgeIndex.viewStartOffset(fromNodeId, roadLevel);
            for(int i = 0; i < edgeCount; i++) {
                Edge edge = dataStore.readEdge(startOffset + (i * DataStructureSizes.EDGE_SIZE));
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
