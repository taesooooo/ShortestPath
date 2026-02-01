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
		setupMockData();
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

	private void setupMockData() throws IOException {
		// Node 1 → Node 2 (50), Node 3 (1)
		// Node 2 → Node 1 (2), Node 4 (2)
		// Node 3 → Node 1 (1), Node 4 (1)
		// Node 4 → Node 2 (2), Node 3 (1)
		// Node 5 → 연결된 엣지 없음

		// Node 설정
		Node node1 = new Node(1, new Coordinate(1, 1), 1, Double.MAX_VALUE, 0, 0);
		Node node2 = new Node(2, new Coordinate(1, 2), 5, Double.MAX_VALUE, 0, 0);
		Node node3 = new Node(3, new Coordinate(2, 2), 7, Double.MAX_VALUE, 0, 0);
		Node node4 = new Node(4, new Coordinate(1, 4), 6, Double.MAX_VALUE, 0, 0);
		Node node5 = new Node(5, new Coordinate(2, 3), -1, Double.MAX_VALUE, 0, 0);

		// readNode mock 설정
		when(store.readNode(1 * DataStructureSizes.NODE_SIZE)).thenReturn(node1);
		when(store.readNode(2 * DataStructureSizes.NODE_SIZE)).thenReturn(node2);
		when(store.readNode(3 * DataStructureSizes.NODE_SIZE)).thenReturn(node3);
		when(store.readNode(4 * DataStructureSizes.NODE_SIZE)).thenReturn(node4);
		when(store.readNode(5 * DataStructureSizes.NODE_SIZE)).thenReturn(node5);

		// Edge 설정 (from 노드 ID 기준으로 정렬됨)
		Edge edge1 = new Edge(1, 1, 2, 50, 3, RoadLevel.L0);
		Edge edge3 = new Edge(3, 1, 3, 1, -1, RoadLevel.L0);
		Edge edge2 = new Edge(2, 2, 1, 2, -1, RoadLevel.L0);
		Edge edge5 = new Edge(5, 2, 4, 2, -1, RoadLevel.L0);
		Edge edge4 = new Edge(4, 3, 1, 1, -1, RoadLevel.L0);
		Edge edge7 = new Edge(7, 3, 4, 1, -1, RoadLevel.L0);
		Edge edge6 = new Edge(6, 4, 2, 2, 8, RoadLevel.L0);
		Edge edge8 = new Edge(8, 4, 3, 1, -1, RoadLevel.L0);

		// readEdge mock 설정 (offset 기준)
		// Node 1: startOffset=1, edgeCount=2 → readEdge(1), readEdge(1+26)
		when(store.readEdge(1)).thenReturn(edge1);
		when(store.readEdge(1 + DataStructureSizes.EDGE_SIZE)).thenReturn(edge3);
		
		// Node 2: startOffset=2, edgeCount=2 → readEdge(2), readEdge(2+26)
		when(store.readEdge(2)).thenReturn(edge2);
		when(store.readEdge(2 + DataStructureSizes.EDGE_SIZE)).thenReturn(edge5);
		
		// Node 3: startOffset=4, edgeCount=2 → readEdge(4), readEdge(4+26)
		when(store.readEdge(4)).thenReturn(edge4);
		when(store.readEdge(4 + DataStructureSizes.EDGE_SIZE)).thenReturn(edge7);
		
		// Node 4: startOffset=6, edgeCount=2 → readEdge(6), readEdge(6+26)
		when(store.readEdge(6)).thenReturn(edge6);
		when(store.readEdge(6 + DataStructureSizes.EDGE_SIZE)).thenReturn(edge8);
		
		// EdgeIndex 설정 (from 노드 ID 기준으로 정렬됨) - FileBasedEdgeIndex 사용
		tempIndexFile = Files.createTempFile("test_edge_index", ".bin");
		edgeIndex = new FileBasedEdgeIndex(tempIndexFile);
		
		// Node 1의 엣지 인덱스 (edge1: offset 1, edge3: offset 3)
		EdgeIndexEntry entry1 = new EdgeIndexEntry(1);
		entry1.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 1, 2));
		edgeIndex.put(entry1);
		
		// Node 2의 엣지 인덱스 (edge2: offset 2, edge5: offset 5)
		EdgeIndexEntry entry2 = new EdgeIndexEntry(2);
		entry2.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 2, 2));
		edgeIndex.put(entry2);
		
		// Node 3의 엣지 인덱스 (edge4: offset 4, edge7: offset 7)
		EdgeIndexEntry entry3 = new EdgeIndexEntry(3);
		entry3.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 4, 2));
		edgeIndex.put(entry3);
		
		// Node 4의 엣지 인덱스 (edge6: offset 6, edge8: offset 8)
		EdgeIndexEntry entry4 = new EdgeIndexEntry(4);
		entry4.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 6, 2));
		edgeIndex.put(entry4);
		
		when(store.getEdgeIndex()).thenReturn(edgeIndex);
	}

	@Test
	@DisplayName("경로탐색 - 정상")
	public void findPathByNodeTest() throws IOException {
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
		Engine engine = new Engine(store, dataProvider);

		Node startNode = store.readNode(1 * DataStructureSizes.NODE_SIZE);
		Node endNode = store.readNode(5 * DataStructureSizes.NODE_SIZE);

		ArrayList<Node> path = (ArrayList<Node>) engine.shortestPathFind(startNode, endNode);

		assertThat(path).isNull();
	}

	@Test
	@DisplayName("경로탐색추척 - 정상")
	public void findPathWithTrackingTest() throws IOException {
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
				.containsExactly(tuple(1.0, 1.0), tuple(2.0, 2.0), tuple(1.0, 4.0));

				// 한번에 모든 탐색한 좌표 검증
				assertThat(traceRoutes).flatExtracting(TraceRoute::getVisitedCoordinates)
				.extracting(Coordinate::getLatitude, Coordinate::getLongitude)
				.containsExactly(tuple(1.0,2.0),tuple(2.0, 2.0),tuple(1.0, 4.0));
	}


}
