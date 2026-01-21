package com.shortestpath.shortestpath.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import com.shortestpath.shortestpath.core.pathengine.RouteSearchResult;
import com.shortestpath.shortestpath.core.pathengine.TraceRoute;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

class EngineTest {

	@Mock
	private DataStore store;

	@Mock
	private NodeProvider dataProvider;

	@BeforeEach
	void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);
		setupMockData();
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

		// Edge 설정
		Edge edge1 = new Edge(1, 1, 2, 50, 3);
		Edge edge2 = new Edge(2, 2, 1, 2, -1);
		Edge edge3 = new Edge(3, 1, 3, 1, -1);
		Edge edge4 = new Edge(4, 3, 1, 1, -1);
		Edge edge5 = new Edge(5, 2, 4, 2, -1);
		Edge edge6 = new Edge(6, 4, 2, 2, 8);
		Edge edge7 = new Edge(7, 3, 4, 1, -1);
		Edge edge8 = new Edge(8, 4, 3, 1, -1);

		// readEdge mock 설정
		when(store.readEdge(1)).thenReturn(edge1);
		when(store.readEdge(3)).thenReturn(edge3);
		when(store.readEdge(5)).thenReturn(edge5);
		when(store.readEdge(2)).thenReturn(edge2);
		when(store.readEdge(7)).thenReturn(edge7);
		when(store.readEdge(6)).thenReturn(edge6);
		when(store.readEdge(4)).thenReturn(edge4);
		when(store.readEdge(8)).thenReturn(edge8);
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
