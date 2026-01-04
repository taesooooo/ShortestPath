package com.shortestpath.shortestpath.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.RouteSearchResult;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

class EngineTest {

	@Mock
	private NodeProvider dataProvider;
	
	@BeforeEach
	void setUp() throws Exception {
		// loader = new Loader(nodeFilePath, linkFilePath);
		// engine = new Engine(loader, dataProvider);
		MockitoAnnotations.openMocks(this);
	}

	@Test
	@DisplayName("경로탐색 - 정상")
	public void findPathByNodeTest() throws IOException {	
		DataStore store = testDataStore();
		Engine engine = new Engine(store, dataProvider);

		Node startNode = store.readNode(1);
		Node endNode = store.readNode(4);

		ArrayList<Node> path = (ArrayList<Node>)engine.shortestPathFind(startNode, endNode);
		
		assertThat(path).extracting(Node::getId)
				.containsExactly(1,3,4);
	}

	@Test
	@DisplayName("경로탐색 - 연결이 끊어져 있어 탐색이 불가한 경우")
	public void findPathDisconnectNode() throws IOException {
		DataStore store = testDataStore();
		Engine engine = new Engine(store, dataProvider);

		Node startNode = store.readNode(1);
		Node endNode = store.readNode(5);

		ArrayList<Node> path = (ArrayList<Node>)engine.shortestPathFind(startNode, endNode);
		
		assertThat(path).isNull();
	}
	
	@Test
	@DisplayName("경로탐색추척 - 정상")
	public void findPathWithTrackingTest() throws IOException {	
		DataStore store = testDataStore();
		Engine engine = new Engine(store, dataProvider);

		when(dataProvider.findNearestNodeId(any(Envelope.class), any(Coordinate.class)))
				.thenReturn(List.of(1))
				.thenReturn(List.of(4));
		Node startNode = store.readNode(1);
		Node endNode = store.readNode(4);

		RouteSearchResult result = engine.shortestPathFind(startNode.getCoordinate(), endNode.getCoordinate(), true);
		ArrayList<Node> path = result.getRouteNode();
		LinkedHashSet<Coordinate> trackCoordinates = result.getRouteTracker().getRouteCoordinates();
		
		assertThat(path).extracting(Node::getId)
				.containsExactly(1,3,4);

		assertThat(trackCoordinates).extracting(Coordinate::getLatitude, Coordinate::getLongitude)
				.containsExactly(tuple(1.0, 1.0), tuple(2.0, 2.0), tuple(1.0, 4.0));
	}
	
	// @Test
	// @DisplayName("경로탐색 - 좌표")
	// void findPathByCoordinateTest() {	
	// 	Coordinate startCoordinate = new Coordinate(33.2403307, 126.5624673);
	// 	Coordinate endCoordinate = new Coordinate(33.2417782, 126.5647375);
		
	// 	ArrayList<Node> path = (ArrayList<Node>)engine.shortestPathFind(startCoordinate, endCoordinate);
		
	// 	assertThat(path).isNotEmpty();
	// }
	
	// @Test
	// @DisplayName("경로탐색 - 그래프에 없는 좌표")
	// void findPathByCooridnateTest() {
	// 	// 시작 지점 없는 좌표 33.4822905, 126.4904020
	// 	// 목표 지점 없는 좌표 33.4844175, 126.4962931
	// 	// start=33.3209235283,126.2460707194
	// 	// end=33.4893700755,126.5038611983
	// 	Coordinate startCoordinate = new Coordinate(33.3209235283,126.2460707194);
	// 	Coordinate endCoordinate = new Coordinate(33.4893700755, 126.5038611983);
		
	// 	ArrayList<Node> path = (ArrayList<Node>)engine.shortestPathFind(startCoordinate, endCoordinate);
		
	// 	assertThat(path).isNotEmpty();
	// 	assertTrue(PathUtil.haversine(path.get(0).getCoordinate(), startCoordinate) < 5);
	// 	assertTrue(PathUtil.haversine(path.get(path.size() - 1).getCoordinate(), endCoordinate) < 5);
		
	// 	path.forEach(item -> System.out.println(item.getCoordinate().toWKT()));
	// }

	private DataStore testDataStore() throws IOException {
		TestFileDataStore store = new TestFileDataStore("");
		store.saveNode(new Node(1, new Coordinate(1, 1), 1, Double.MAX_VALUE, 0, 0), 1);
		store.saveNode(new Node(2, new Coordinate(1, 2), 5, Double.MAX_VALUE, 0, 0), 2);
		store.saveNode(new Node(3, new Coordinate(2, 2), 7, Double.MAX_VALUE, 0, 0), 3);
		store.saveNode(new Node(4, new Coordinate(1, 4), 6, Double.MAX_VALUE, 0, 0), 4);

		store.saveNode(new Node(5, new Coordinate(2, 3), -1, Double.MAX_VALUE, 0, 0), 5);

		store.saveEdge(new Edge(1, 1, 2, 50, 3), 1);
		store.saveEdge(new Edge(2, 2, 1, 2, -1), 2);
		store.saveEdge(new Edge(3, 1, 3, 1, -1), 3);
		store.saveEdge(new Edge(4, 3, 1, 1, -1), 4);
		store.saveEdge(new Edge(5, 2, 4, 2, -1), 5);
		store.saveEdge(new Edge(6, 4, 2, 2, 8), 6);
		store.saveEdge(new Edge(7, 3, 4, 1, -1), 7);
		store.saveEdge(new Edge(8, 4, 3, 1, -1), 8);

		return store;
	}

	 public static class TestFileDataStore extends com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore {
         private HashMap<Integer, Node> nodeMap = new HashMap<Integer, Node>();
         private HashMap<Integer, Edge> edgeMap = new HashMap<Integer, Edge>();

        public TestFileDataStore(String filePath) throws IOException {
            super(filePath, mock(NodeProvider.class));
        }

        public int getEdgeByteSize() {
            return 1;
        }
        public int getNodeByteSize() {
            return 1;
        }

        @Override
		public Edge readEdge(long offset) throws IOException {
			Edge edge = edgeMap.get((int)offset);
			return new Edge(edge.getId(), edge.getFrom(), edge.getTo(), edge.getDistance(), edge.getNextEdgeOffset());
		}

		@Override
		public Node readNode(long offset) throws IOException {
			Node node = nodeMap.get((int)offset);
			return new Node(node.getId(), node.getCoordinate(), node.getStartEdgeOffset(), node.getGCost(), node.getHCost(), node.getFCost());
		}

		@Override
		public int saveEdge(Edge edge) throws IOException {
			return saveEdge(edge, edge.getId());
		}

		@Override
		public int saveEdge(Edge edge, long offset) throws IOException {
			edgeMap.put((int)offset, edge);

            return (int)offset;
		}

		@Override
		public int saveNode(Node node) throws IOException {
			return saveNode(node, node.getId());
		}

		@Override
		public int saveNode(Node node, long offset) throws IOException {
			nodeMap.put((int)offset, node);

            return (int)offset;
		}
     }

}
