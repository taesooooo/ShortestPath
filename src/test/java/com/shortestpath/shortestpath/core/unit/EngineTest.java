package com.shortestpath.shortestpath.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.shortestpath.shortestpath.core.pathengine.SearchSide;
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
	private EdgeIndex reverseEdgeIndex;
	private Path tempIndexFile;
	private Path tempReverseIndexFile;

	@BeforeEach
	void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);
	}
	
	@AfterEach
	void tearDown() throws Exception {
		if (edgeIndex != null) {
			edgeIndex.close();
		}
		if (reverseEdgeIndex != null) {
			reverseEdgeIndex.close();
		}
		if (tempIndexFile != null && Files.exists(tempIndexFile)) {
			Files.delete(tempIndexFile);
		}
		if (tempReverseIndexFile != null && Files.exists(tempReverseIndexFile)) {
			Files.delete(tempReverseIndexFile);
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

	private void setupMockReverseDataL0Only() throws IOException {
		// Reverse Edge는 원본 edge를 도착 노드 기준으로 묶어 역방향 탐색에서 이전 노드를 찾도록 구성한다.
		Edge edge1 = new Edge(1, 1, 2, 40, -1, 60, RoadLevel.L0);
		Edge edge2 = new Edge(2, 2, 1, 2, -1, 60, RoadLevel.L0);
		Edge edge3 = new Edge(3, 1, 3, 1, -1, 60, RoadLevel.L0);
		Edge edge4 = new Edge(4, 3, 1, 1, -1, 60, RoadLevel.L0);
		Edge edge5 = new Edge(5, 2, 4, 2, -1, 60, RoadLevel.L0);
		Edge edge6 = new Edge(6, 4, 2, 2, -1, 60, RoadLevel.L0);
		Edge edge7 = new Edge(7, 3, 4, 1, -1, 60, RoadLevel.L0);
		Edge edge8 = new Edge(8, 4, 3, 1, -1, 60, RoadLevel.L0);

		when(store.readReverseEdge(0 + 0 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge2);
		when(store.readReverseEdge(0 + 1 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge4);
		when(store.readReverseEdge(2 + 0 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge1);
		when(store.readReverseEdge(2 + 1 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge6);
		when(store.readReverseEdge(4 + 0 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge3);
		when(store.readReverseEdge(4 + 1 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge8);
		when(store.readReverseEdge(6 + 0 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge5);
		when(store.readReverseEdge(6 + 1 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge7);

		tempReverseIndexFile = Files.createTempFile("test_reverse_edge_index_l0", ".bin");
		reverseEdgeIndex = new FileBasedEdgeIndex(tempReverseIndexFile);

		EdgeIndexEntry entry1 = new EdgeIndexEntry(1);
		entry1.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 0, 2));
		entry1.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		reverseEdgeIndex.put(entry1);

		EdgeIndexEntry entry2 = new EdgeIndexEntry(2);
		entry2.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 2, 2));
		entry2.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		reverseEdgeIndex.put(entry2);

		EdgeIndexEntry entry3 = new EdgeIndexEntry(3);
		entry3.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 4, 2));
		entry3.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		reverseEdgeIndex.put(entry3);

		EdgeIndexEntry entry4 = new EdgeIndexEntry(4);
		entry4.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 6, 2));
		entry4.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		reverseEdgeIndex.put(entry4);

		EdgeIndexEntry entry5 = new EdgeIndexEntry(5);
		entry5.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 0, 0));
		entry5.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		reverseEdgeIndex.put(entry5);

		when(store.getReverseEdgeIndex()).thenReturn(reverseEdgeIndex);
	}

	private void setupMockDataWithUpperLevel() throws IOException {
		Node node1 = new Node(1, new Coordinate(127.1, 33.1), 1, Double.MAX_VALUE, 0, 0);
		Node node2 = new Node(2, new Coordinate(127.1, 33.2), 2, Double.MAX_VALUE, 0, 0);
		Node node3 = new Node(3, new Coordinate(127.15, 33.25), -1, Double.MAX_VALUE, 0, 0);
		Node node4 = new Node(4, new Coordinate(127.2, 33.4), 4, Double.MAX_VALUE, 0, 0);

		when(store.readNode(DataStructureSizes.calculateNodeOffset(1))).thenReturn(node1);
		when(store.readNode(DataStructureSizes.calculateNodeOffset(2))).thenReturn(node2);
		when(store.readNode(DataStructureSizes.calculateNodeOffset(3))).thenReturn(node3);
		when(store.readNode(DataStructureSizes.calculateNodeOffset(4))).thenReturn(node4);

		Edge l0Entry = new Edge(1, 1, 2, 100, -1, 60, RoadLevel.L0);
		Edge l1Direct = new Edge(2, 1, 4, 1, -1, 60, RoadLevel.L1);
		Edge l0Exit = new Edge(3, 2, 4, 100, -1, 60, RoadLevel.L0);
		Edge l1Return = new Edge(4, 4, 1, 1, -1, 60, RoadLevel.L1);

		when(store.readEdge(0 * DataStructureSizes.EDGE_SIZE)).thenReturn(l0Entry);
		when(store.readEdge(1 * DataStructureSizes.EDGE_SIZE)).thenReturn(l1Direct);
		when(store.readEdge(2 * DataStructureSizes.EDGE_SIZE)).thenReturn(l0Exit);
		when(store.readEdge(3 * DataStructureSizes.EDGE_SIZE)).thenReturn(l1Return);

		tempIndexFile = Files.createTempFile("test_edge_index_upper_level", ".bin");
		edgeIndex = new FileBasedEdgeIndex(tempIndexFile);

		EdgeIndexEntry entry1 = new EdgeIndexEntry(1);
		entry1.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 0 * DataStructureSizes.EDGE_SIZE, 1));
		entry1.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 1 * DataStructureSizes.EDGE_SIZE, 1));
		edgeIndex.put(entry1);

		EdgeIndexEntry entry2 = new EdgeIndexEntry(2);
		entry2.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 2 * DataStructureSizes.EDGE_SIZE, 1));
		entry2.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		edgeIndex.put(entry2);

		EdgeIndexEntry entry3 = new EdgeIndexEntry(3);
		entry3.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 0, 0));
		entry3.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		edgeIndex.put(entry3);

		EdgeIndexEntry entry4 = new EdgeIndexEntry(4);
		entry4.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 0, 0));
		entry4.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 3 * DataStructureSizes.EDGE_SIZE, 1));
		edgeIndex.put(entry4);

		when(store.getEdgeIndex()).thenReturn(edgeIndex);
	}

	private void setupMockReverseDataWithUpperLevel() throws IOException {
		Edge l1Return = new Edge(4, 4, 1, 1, -1, 60, RoadLevel.L1);
		Edge l0Entry = new Edge(1, 1, 2, 100, -1, 60, RoadLevel.L0);
		Edge l0Exit = new Edge(3, 2, 4, 100, -1, 60, RoadLevel.L0);
		Edge l1Direct = new Edge(2, 1, 4, 1, -1, 60, RoadLevel.L1);

		when(store.readReverseEdge(0 * DataStructureSizes.EDGE_SIZE)).thenReturn(l1Return);
		when(store.readReverseEdge(1 * DataStructureSizes.EDGE_SIZE)).thenReturn(l0Entry);
		when(store.readReverseEdge(2 * DataStructureSizes.EDGE_SIZE)).thenReturn(l0Exit);
		when(store.readReverseEdge(3 * DataStructureSizes.EDGE_SIZE)).thenReturn(l1Direct);

		tempReverseIndexFile = Files.createTempFile("test_reverse_edge_index_upper_level", ".bin");
		reverseEdgeIndex = new FileBasedEdgeIndex(tempReverseIndexFile);

		EdgeIndexEntry entry1 = new EdgeIndexEntry(1);
		entry1.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 0, 0));
		entry1.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0 * DataStructureSizes.EDGE_SIZE, 1));
		reverseEdgeIndex.put(entry1);

		EdgeIndexEntry entry2 = new EdgeIndexEntry(2);
		entry2.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 1 * DataStructureSizes.EDGE_SIZE, 1));
		entry2.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		reverseEdgeIndex.put(entry2);

		EdgeIndexEntry entry3 = new EdgeIndexEntry(3);
		entry3.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 0, 0));
		entry3.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		reverseEdgeIndex.put(entry3);

		EdgeIndexEntry entry4 = new EdgeIndexEntry(4);
		entry4.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 2 * DataStructureSizes.EDGE_SIZE, 1));
		entry4.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 3 * DataStructureSizes.EDGE_SIZE, 1));
		reverseEdgeIndex.put(entry4);

		when(store.getReverseEdgeIndex()).thenReturn(reverseEdgeIndex);
	}

	private void setupMockDisconnectedEdgeData() throws IOException {
		Node node1 = new Node(1, new Coordinate(127.1, 33.1), 1, Double.MAX_VALUE, 0, 0);
		Node node2 = new Node(2, new Coordinate(127.1, 33.2), 2, Double.MAX_VALUE, 0, 0);
		Node node3 = new Node(3, new Coordinate(127.2, 33.3), 3, Double.MAX_VALUE, 0, 0);
		Node node4 = new Node(4, new Coordinate(127.2, 33.4), 4, Double.MAX_VALUE, 0, 0);

		when(store.readNode(DataStructureSizes.calculateNodeOffset(1))).thenReturn(node1);
		when(store.readNode(DataStructureSizes.calculateNodeOffset(2))).thenReturn(node2);
		when(store.readNode(DataStructureSizes.calculateNodeOffset(3))).thenReturn(node3);
		when(store.readNode(DataStructureSizes.calculateNodeOffset(4))).thenReturn(node4);

		Edge edge1 = new Edge(1, 1, 2, 1, -1, 60, RoadLevel.L0);
		Edge edge2 = new Edge(2, 2, 1, 1, -1, 60, RoadLevel.L0);
		Edge edge3 = new Edge(3, 3, 4, 1, -1, 60, RoadLevel.L0);
		Edge edge4 = new Edge(4, 4, 3, 1, -1, 60, RoadLevel.L0);

		when(store.readEdge(0 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge1);
		when(store.readEdge(1 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge2);
		when(store.readEdge(2 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge3);
		when(store.readEdge(3 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge4);

		tempIndexFile = Files.createTempFile("test_edge_index_disconnected", ".bin");
		edgeIndex = new FileBasedEdgeIndex(tempIndexFile);

		EdgeIndexEntry entry1 = new EdgeIndexEntry(1);
		entry1.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 0 * DataStructureSizes.EDGE_SIZE, 1));
		entry1.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		edgeIndex.put(entry1);

		EdgeIndexEntry entry2 = new EdgeIndexEntry(2);
		entry2.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 1 * DataStructureSizes.EDGE_SIZE, 1));
		entry2.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		edgeIndex.put(entry2);

		EdgeIndexEntry entry3 = new EdgeIndexEntry(3);
		entry3.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 2 * DataStructureSizes.EDGE_SIZE, 1));
		entry3.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		edgeIndex.put(entry3);

		EdgeIndexEntry entry4 = new EdgeIndexEntry(4);
		entry4.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 3 * DataStructureSizes.EDGE_SIZE, 1));
		entry4.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		edgeIndex.put(entry4);

		when(store.getEdgeIndex()).thenReturn(edgeIndex);
	}

	private void setupMockReverseDisconnectedEdgeData() throws IOException {
		Edge edge2 = new Edge(2, 2, 1, 1, -1, 60, RoadLevel.L0);
		Edge edge1 = new Edge(1, 1, 2, 1, -1, 60, RoadLevel.L0);
		Edge edge4 = new Edge(4, 4, 3, 1, -1, 60, RoadLevel.L0);
		Edge edge3 = new Edge(3, 3, 4, 1, -1, 60, RoadLevel.L0);

		when(store.readReverseEdge(0 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge2);
		when(store.readReverseEdge(1 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge1);
		when(store.readReverseEdge(2 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge4);
		when(store.readReverseEdge(3 * DataStructureSizes.EDGE_SIZE)).thenReturn(edge3);

		tempReverseIndexFile = Files.createTempFile("test_reverse_edge_index_disconnected", ".bin");
		reverseEdgeIndex = new FileBasedEdgeIndex(tempReverseIndexFile);

		EdgeIndexEntry entry1 = new EdgeIndexEntry(1);
		entry1.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 0 * DataStructureSizes.EDGE_SIZE, 1));
		entry1.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		reverseEdgeIndex.put(entry1);

		EdgeIndexEntry entry2 = new EdgeIndexEntry(2);
		entry2.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 1 * DataStructureSizes.EDGE_SIZE, 1));
		entry2.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		reverseEdgeIndex.put(entry2);

		EdgeIndexEntry entry3 = new EdgeIndexEntry(3);
		entry3.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 2 * DataStructureSizes.EDGE_SIZE, 1));
		entry3.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		reverseEdgeIndex.put(entry3);

		EdgeIndexEntry entry4 = new EdgeIndexEntry(4);
		entry4.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 3 * DataStructureSizes.EDGE_SIZE, 1));
		entry4.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 0, 0));
		reverseEdgeIndex.put(entry4);

		when(store.getReverseEdgeIndex()).thenReturn(reverseEdgeIndex);
	}

	@Test
	@DisplayName("경로탐색 - 정상")
	public void findPathByNodeTest() throws IOException {
		setupMockDataL0Only();
		setupMockReverseDataL0Only();
		Engine engine = new Engine(store, dataProvider);

		when(dataProvider.findNearestNodeId(any(Envelope.class), any(Coordinate.class)))
				.thenReturn(List.of(1))
				.thenReturn(List.of(4));
		Node startNode = store.readNode(DataStructureSizes.calculateNodeOffset(1));
		Node endNode = store.readNode(DataStructureSizes.calculateNodeOffset(4));

		RouteSearchResult result = engine.shortestPathFind(startNode, endNode, false);

		assertThat(result.getRouteNode()).extracting(Node::getId)
				.containsExactly(1, 3, 4);
	}

	@Test
	@DisplayName("경로탐색 - 엣지가 있지만 연결이 끊어진 경우")
	public void findPathDisconnectNode() throws IOException {
		setupMockDisconnectedEdgeData();
		setupMockReverseDisconnectedEdgeData();
		Engine engine = new Engine(store, dataProvider);

		when(dataProvider.findNearestNodeId(any(Envelope.class), any(Coordinate.class)))
				.thenReturn(List.of(1))
				.thenReturn(List.of(4));
		Node startNode = store.readNode(DataStructureSizes.calculateNodeOffset(1));
		Node endNode = store.readNode(DataStructureSizes.calculateNodeOffset(4));

		RouteSearchResult result = engine.shortestPathFind(startNode, endNode, false);

		assertThat(result.getRouteNode()).isNull();
	}

	@Test
	@DisplayName("양방향 경로탐색 - 정상")
	public void findBidirectionalPathByCoordinateTest() throws IOException {
		setupMockDataL0Only();
		setupMockReverseDataL0Only();
		Engine engine = new Engine(store, dataProvider);

		when(dataProvider.findNearestNodeId(any(Envelope.class), any(Coordinate.class)))
				.thenReturn(List.of(1))
				.thenReturn(List.of(4));
		Node startNode = store.readNode(DataStructureSizes.calculateNodeOffset(1));
		Node endNode = store.readNode(DataStructureSizes.calculateNodeOffset(4));

		RouteSearchResult result = engine.shortestPathFind(startNode.getCoordinate(), endNode.getCoordinate(), false);

		assertThat(result.getRouteNode()).extracting(Node::getId)
				.containsExactly(1, 3, 4);
		assertThat(result.getRouteTracker()).isNull();
		assertThat(result.getSearchTime()).isGreaterThanOrEqualTo(0.0);
	}

	@Test
	@DisplayName("양방향 경로탐색 - 상위계층이 있는 경우")
	public void findBidirectionalPathWithUpperLevelTest() throws IOException {
		setupMockDataWithUpperLevel();
		setupMockReverseDataWithUpperLevel();
		Engine engine = new Engine(store, dataProvider);

		when(dataProvider.findNearestNodeId(any(Envelope.class), any(Coordinate.class)))
				.thenReturn(List.of(1))
				.thenReturn(List.of(4));
		Node startNode = store.readNode(DataStructureSizes.calculateNodeOffset(1));
		Node endNode = store.readNode(DataStructureSizes.calculateNodeOffset(4));

		RouteSearchResult result = engine.shortestPathFind(startNode.getCoordinate(), endNode.getCoordinate(), false);

		assertThat(result.getRouteNode()).extracting(Node::getId)
				.containsExactly(1, 4);
	}

	@Test
	@DisplayName("양방향 경로탐색 - 엣지가 있지만 연결이 끊어진 경우")
	public void findBidirectionalPathDisconnectedEdgeTest() throws IOException {
		setupMockDisconnectedEdgeData();
		setupMockReverseDisconnectedEdgeData();
		Engine engine = new Engine(store, dataProvider);

		when(dataProvider.findNearestNodeId(any(Envelope.class), any(Coordinate.class)))
				.thenReturn(List.of(1))
				.thenReturn(List.of(4));
		Node startNode = store.readNode(DataStructureSizes.calculateNodeOffset(1));
		Node endNode = store.readNode(DataStructureSizes.calculateNodeOffset(4));

		RouteSearchResult result = engine.shortestPathFind(startNode.getCoordinate(), endNode.getCoordinate(), false);

		assertThat(result.getRouteNode()).isNull();
		assertThat(result.getRouteTracker()).isNull();
	}

	@Test
	@DisplayName("경로탐색추적 - 양방향 탐색")
	public void findPathWithTrackingTest() throws IOException {
		setupMockDataL0Only();
		setupMockReverseDataL0Only();
		Engine engine = new Engine(store, dataProvider);

		when(dataProvider.findNearestNodeId(any(Envelope.class), any(Coordinate.class)))
				.thenReturn(List.of(1))
				.thenReturn(List.of(4));
		Node startNode = store.readNode(DataStructureSizes.calculateNodeOffset(1));
		Node endNode = store.readNode(DataStructureSizes.calculateNodeOffset(4));

		RouteSearchResult result = engine.shortestPathFind(startNode.getCoordinate(), endNode.getCoordinate(), true);
		ArrayList<Node> path = result.getRouteNode();
		List<TraceRoute> traceRoutes = result.getRouteTracker().getTrackRoutes();

		assertThat(path).extracting(Node::getId)
				.containsExactly(1, 3, 4);

		assertThat(traceRoutes).isNotEmpty();
		assertThat(traceRoutes)
				.extracting(TraceRoute::getSearchSide)
				.doesNotContainNull()
				.contains(SearchSide.FORWARD);
		assertThat(traceRoutes)
				.extracting(TraceRoute::getParentCoordinate)
				.doesNotContainNull();
		assertThat(traceRoutes)
				.flatExtracting(TraceRoute::getVisitedCoordinates)
				.isNotEmpty();
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
	
