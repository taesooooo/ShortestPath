package com.shortestpath.shortestpath.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.RouteSearchResult;
import com.shortestpath.shortestpath.core.pathengine.TraceRoute;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndexEntry;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.FileBasedEdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.LevelEdgeIndex;

class EngineTest {

	@Mock
	private DataStore store;

	@Mock
	private NodeProvider dataProvider;
	
	private EdgeIndex edgeIndex;
	private Path tempIndexFile;

	@BeforeEach
	void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);
	}
	
	@AfterEach
	void tearDown() throws Exception {
		if (edgeIndex != null) {
			edgeIndex.close();
		}
		if (tempIndexFile != null && Files.exists(tempIndexFile)) {
			Files.delete(tempIndexFile);
		}
	}

	private void setupMockDataL0Only() throws IOException {
		// L0 계층만 있고 L1 계층 없는 경우 테스트
		// Node 1 → Node 2 (40), Node 3 (1)
		// Node 2 → Node 1 (2), Node 4 (2)
		// Node 3 → Node 1 (1), Node 4 (1)
		// Node 4 → Node 2 (2), Node 3 (1)
		// Node 5 → 연결된 엣지 없음

		// Node 설정
		Node node1 = new Node(1, new Coordinate(127.1, 33.1), 1, Double.MAX_VALUE, 0, 0);
		Node node2 = new Node(2, new Coordinate(127.1, 33.2), 5, Double.MAX_VALUE, 0, 0);
		Node node3 = new Node(3, new Coordinate(127.2, 33.2), 7, Double.MAX_VALUE, 0, 0);
		Node node4 = new Node(4, new Coordinate(127.2, 33.4), 6, Double.MAX_VALUE, 0, 0);
		Node node5 = new Node(5, new Coordinate(127.2, 33.3), -1, Double.MAX_VALUE, 0, 0);

		when(store.readNode(DataStructureSizes.calculateNodeOffset(1))).thenReturn(node1);
		when(store.readNode(DataStructureSizes.calculateNodeOffset(2))).thenReturn(node2);
		when(store.readNode(DataStructureSizes.calculateNodeOffset(3))).thenReturn(node3);
		when(store.readNode(DataStructureSizes.calculateNodeOffset(4))).thenReturn(node4);
		when(store.readNode(DataStructureSizes.calculateNodeOffset(5))).thenReturn(node5);

		// L0만 있는 Edge 설정
		// Node 1의 엣지
		Edge edge1 = new Edge(1, 1, 2, 40, -1, 60, RoadLevel.L0);
		Edge edge3 = new Edge(3, 1, 3, 1, -1, 60, RoadLevel.L0);
		
		// Node 2의 엣지
		Edge edge2 = new Edge(2, 2, 1, 2, -1, 60, RoadLevel.L0);
		Edge edge5 = new Edge(5, 2, 4, 2, -1, 60, RoadLevel.L0);
		
		// Node 3의 엣지
		Edge edge4 = new Edge(4, 3, 1, 1, -1, 60, RoadLevel.L0);
		Edge edge7 = new Edge(7, 3, 4, 1, -1, 60, RoadLevel.L0);
		
		// Node 4의 엣지
		Edge edge6 = new Edge(6, 4, 2, 2, -1, 60, RoadLevel.L0);
		Edge edge8 = new Edge(8, 4, 3, 1, -1, 60, RoadLevel.L0);

		// readEdge mock 설정 (offset 기준)
		// Node 1: L0 startOffset=0, count=2 → 0+(0*EDGE_SIZE), 0+(1*EDGE_SIZE)
		when(store.readEdge(0 + 0 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge1);
		when(store.readEdge(0 + 1 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge3);

		// Node 2: L0 startOffset=2, count=2 → 2+(0*EDGE_SIZE), 2+(1*EDGE_SIZE)
		when(store.readEdge(2 + 0 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge2);
		when(store.readEdge(2 + 1 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge5);

		// Node 3: L0 startOffset=4, count=2 → 4+(0*EDGE_SIZE), 4+(1*EDGE_SIZE)
		when(store.readEdge(4 + 0 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge4);
		when(store.readEdge(4 + 1 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge7);

		// Node 4: L0 startOffset=6, count=2 → 6+(0*EDGE_SIZE), 6+(1*EDGE_SIZE)
		when(store.readEdge(6 + 0 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge6);
		when(store.readEdge(6 + 1 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge8);

		// EdgeIndex 설정 (L0만 존재, L1과 L2는 count=0)
		tempIndexFile = Files.createTempFile("test_edge_index_l0", ".bin");
		edgeIndex = new FileBasedEdgeIndex(tempIndexFile);

		EdgeIndexEntry entry1 = new EdgeIndexEntry(1);
		entry1.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 0, 2));
		entry1.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		edgeIndex.put(entry1);

		EdgeIndexEntry entry2 = new EdgeIndexEntry(2);
		entry2.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 2, 2));
		entry2.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		edgeIndex.put(entry2);

		EdgeIndexEntry entry3 = new EdgeIndexEntry(3);
		entry3.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 4, 2));
		entry3.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		edgeIndex.put(entry3);

		EdgeIndexEntry entry4 = new EdgeIndexEntry(4);
		entry4.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 6, 2));
		entry4.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		edgeIndex.put(entry4);

		EdgeIndexEntry entry5 = new EdgeIndexEntry(5);
		entry5.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 0, 0));
		entry5.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		edgeIndex.put(entry5);

		when(store.getEdgeIndex()).thenReturn(edgeIndex);
	}

	private void setupMockDataL0L1() throws IOException {
		// L0 계층 (일반도로)
		// Node 1 → Node 2 (50), Node 3 (1)
		// Node 2 → Node 1 (2), Node 4 (2)
		// Node 3 → Node 1 (1), Node 4 (1)
		// Node 4 → Node 2 (2), Node 3 (1)
		// Node 5 → 연결된 엣지 없음
		
		// L1 계층 (고속도로 - 상위 계층)
		// Node 1 → Node 4 (직통로, 비용: 10)

		// Node 설정
		Node node1 = new Node(1, new Coordinate(127.1, 33.1), 1, Double.MAX_VALUE, 0, 0);
		Node node2 = new Node(2, new Coordinate(127.1, 33.2), 5, Double.MAX_VALUE, 0, 0);
		Node node3 = new Node(3, new Coordinate(127.2, 33.2), 7, Double.MAX_VALUE, 0, 0);
		Node node4 = new Node(4, new Coordinate(127.1, 33.4), 6, Double.MAX_VALUE, 0, 0);
		Node node5 = new Node(5, new Coordinate(127.2, 33.3), -1, Double.MAX_VALUE, 0, 0);
		// readNode mock 설정
		when(store.readNode(DataStructureSizes.calculateNodeOffset(1))).thenReturn(node1);
		when(store.readNode(DataStructureSizes.calculateNodeOffset(2))).thenReturn(node2);
		when(store.readNode(DataStructureSizes.calculateNodeOffset(3))).thenReturn(node3);
		when(store.readNode(DataStructureSizes.calculateNodeOffset(4))).thenReturn(node4);
		when(store.readNode(DataStructureSizes.calculateNodeOffset(5))).thenReturn(node5);
		// L0 계층 Edge 설정 (from 다음 RoadLevel 순으로 정렬)
		// Node 1의 엣지
		Edge edge1 = new Edge(1, 1, 2, 40, 3, 100, RoadLevel.L0);  // 상위 계층 엣지 참조: 3
		Edge edge3 = new Edge(3, 1, 3, 1, -1, 100, RoadLevel.L0);
		Edge edge101 = new Edge(9, 1, 4, 10, -1, 60, RoadLevel.L1);   // 1 → 4 직통로, 비용: 10
		Edge edge105 = new Edge(13, 1, 3, 5, -1, 60, RoadLevel.L1);   // 1 → 3 고속도로, 비용: 5
		
		// Node 2의 엣지
		Edge edge2 = new Edge(2, 2, 1, 2, 5, 100, RoadLevel.L0);
		Edge edge5 = new Edge(5, 2, 4, 2, -1, 100, RoadLevel.L0);
		Edge edge103 = new Edge(11, 2, 4, 8, -1, 60, RoadLevel.L1);    // 2 → 4 고속도로 진입로, 비용: 8
		Edge edge107 = new Edge(15, 2, 3, 6, -1, 60, RoadLevel.L1);    // 2 → 3 고속도로, 비용: 6
		
		// Node 3의 엣지
		Edge edge4 = new Edge(4, 3, 1, 1, 7, 100, RoadLevel.L0);
		Edge edge7 = new Edge(7, 3, 4, 1, -1, 100, RoadLevel.L0);
		Edge edge106 = new Edge(14, 3, 1, 5, -1, 60, RoadLevel.L1);    // 3 → 1 양방향, 비용: 5
		Edge edge108 = new Edge(16, 3, 2, 6, -1, 60, RoadLevel.L1);    // 3 → 2 양방향, 비용: 6
		
		// Node 4의 엣지
		Edge edge6 = new Edge(6, 4, 2, 2, 8, 100, RoadLevel.L0);  // 상위 계층 엣지 참조: 8
		Edge edge8 = new Edge(8, 4, 3, 1, -1, 100, RoadLevel.L0);
		Edge edge102 = new Edge(10, 4, 1, 10, -1, 60, RoadLevel.L1);   // 4 → 1 양방향, 비용: 10
		Edge edge104 = new Edge(12, 4, 2, 8, -1, 60, RoadLevel.L1);    // 4 → 2 양방향, 비용: 8

		// readEdge mock 설정 (offset 기준)
		// Node 1: L0 offset 1-2, L1 offset 3-4
		when(store.readEdge(1 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge1);
		when(store.readEdge(2 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge3);
		when(store.readEdge(3 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge101);
		when(store.readEdge(4 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge105);

		// Node 2: L0 offset 5-6, L1 offset 7-8
		when(store.readEdge(5 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge2);
		when(store.readEdge(6 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge5);
		when(store.readEdge(7 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge103);
		when(store.readEdge(8 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge107);

		// Node 3: L0 offset 9-10, L1 offset 11-12
		when(store.readEdge(9 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge4);
		when(store.readEdge(10 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge7);
		when(store.readEdge(11 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge106);
		when(store.readEdge(12 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge108);

		// Node 4: L0 offset 13-14, L1 offset 15-16
		when(store.readEdge(13 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge6);
		when(store.readEdge(14 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge8);
		when(store.readEdge(15 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge102);
		when(store.readEdge(16 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge104);
		
		// EdgeIndex 설정 (from 노드 ID 기준으로 정렬됨) - FileBasedEdgeIndex 사용
		tempIndexFile = Files.createTempFile("test_edge_index", ".bin");
		edgeIndex = new FileBasedEdgeIndex(tempIndexFile);
		
		// Node 1의 엣지 인덱스 (L0: startOffset=1, count=2 / L1: startOffset=3, count=2)
		EdgeIndexEntry entry1 = new EdgeIndexEntry(1);
		entry1.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 1, 2));
		entry1.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 3, 2));
		edgeIndex.put(entry1);
		
		// Node 2의 엣지 인덱스 (L0: startOffset=5, count=2 / L1: startOffset=7, count=2)
		EdgeIndexEntry entry2 = new EdgeIndexEntry(2);
		entry2.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 5, 2));
		entry2.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 7, 2));
		edgeIndex.put(entry2);
		
		// Node 3의 엣지 인덱스 (L0: startOffset=9, count=2 / L1: startOffset=11, count=2)
		EdgeIndexEntry entry3 = new EdgeIndexEntry(3);
		entry3.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 9, 2));
		entry3.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 11, 2));
		edgeIndex.put(entry3);
		
		// Node 4의 엣지 인덱스 (L0: startOffset=13, count=2 / L1: startOffset=15, count=2)
		EdgeIndexEntry entry4 = new EdgeIndexEntry(4);
		entry4.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 13, 2));
		entry4.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 15, 2));
		edgeIndex.put(entry4);
		
		when(store.getEdgeIndex()).thenReturn(edgeIndex);
	}

	@Test
	@DisplayName("경로탐색 - 정상")
	public void findPathByNodeTest() throws IOException {
		setupMockDataL0Only();
		Engine engine = new Engine(store, dataProvider);

		Node startNode = store.readNode(1 * DataStructureSizes.NODE_SIZE);
		Node endNode = store.readNode(4 * DataStructureSizes.NODE_SIZE);

		ArrayList<Node> path = (ArrayList<Node>) engine.shortestPathFind(startNode, endNode);

		assertThat(path).extracting(Node::getId)
				.containsExactly(1, 3, 4);
	}

	@Test
	@DisplayName("경로탐색 - 연결이 끊어져 있어 탐색이 불가한 경우")
	public void findPathDisconnectNode() throws IOException {
		setupMockDataL0Only();
		Engine engine = new Engine(store, dataProvider);

		Node startNode = store.readNode(1 * DataStructureSizes.NODE_SIZE);
		Node endNode = store.readNode(5 * DataStructureSizes.NODE_SIZE);

		ArrayList<Node> path = (ArrayList<Node>) engine.shortestPathFind(startNode, endNode);

		assertThat(path).isNull();
	}

	@Test
	@DisplayName("경로탐색추척 - 정상")
	public void findPathWithTrackingTest() throws IOException {
		setupMockDataL0Only();
		Engine engine = new Engine(store, dataProvider);

		when(dataProvider.findNearestNodeId(any(Envelope.class), any(Coordinate.class)))
				.thenReturn(List.of(1))
				.thenReturn(List.of(4));
		Node startNode = store.readNode(1 * DataStructureSizes.NODE_SIZE);
		Node endNode = store.readNode(4 * DataStructureSizes.NODE_SIZE);

		RouteSearchResult result = engine.shortestPathFind(startNode.getCoordinate(), endNode.getCoordinate(), true);
		ArrayList<Node> path = result.getRouteNode();
		List<TraceRoute> traceRoutes = result.getRouteTracker().getTrackRoutes();

		assertThat(path).extracting(Node::getId)
				.containsExactly(1, 3, 4);

		assertThat(traceRoutes)
				.hasSize(3)
				.extracting(TraceRoute::getParentCoordinate)
				.extracting(Coordinate::getLatitude, Coordinate::getLongitude)
				.containsExactly(tuple(127.1, 33.1), tuple(127.2, 33.2), tuple(127.2, 33.4));

				// 한번에 모든 탐색한 좌표 검증
				assertThat(traceRoutes).flatExtracting(TraceRoute::getVisitedCoordinates)
				.extracting(Coordinate::getLatitude, Coordinate::getLongitude)
				.containsExactly(tuple(127.1, 33.2),tuple(127.2, 33.2),tuple(127.2, 33.4));
	}

	// @Test
	// @DisplayName("계층 경로 탐색 - 현재 엣지에 상위 계층이 있는 경우")
	// public void findHierarchyPathWithUpperLevelTest() throws IOException {
	// 	setupMockDataL0L1();
	// 	Engine engine = new Engine(store, dataProvider);

	// 	// Node 1 → Node 2 (50), Node 3 (1)
	// 	// Node 2 → Node 1 (2), Node 4 (2)
	// 	// Node 3 → Node 1 (1), Node 4 (1)
	// 	// Node 4 → Node 2 (2), Node 3 (1)

	// 	Node startNode = store.readNode(1 * DataStructureSizes.NODE_SIZE);
	// 	Node endNode = store.readNode(4 * DataStructureSizes.NODE_SIZE);

	// 	ArrayList<Node> path = (ArrayList<Node>) engine.shortestPathFind(startNode, endNode);

	// 	assertThat(path).extracting(Node::getId)
	// 			.containsExactly(1, 3, 4);
	// }
}
	
