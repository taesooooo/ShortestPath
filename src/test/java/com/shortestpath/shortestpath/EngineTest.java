package com.shortestpath.shortestpath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.DataProvider;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Graph;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Provider.MapDataProvider;
import com.shortestpath.shortestpath.util.PathUtil;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MapDataProvider.class)
class EngineTest {
	private static final Logger log = LoggerFactory.getLogger(EngineTest.class);
	@Value("${findpath.node-shp-path}")
	private String nodeFilePath;
	@Value("${findpath.link-shp-path}")
	private String linkFilePath;
	
	private Loader loader;
	private Engine engine;
	
	@Autowired
	private DataProvider dataProvider;

	@BeforeEach
	void setUp() throws Exception {
		loader = new Loader(nodeFilePath, linkFilePath);
		engine = new Engine(loader, dataProvider);
	}

	@Test
	@DisplayName("경로탐색 - 노드")
	void findPathByNodeTest() {	
		Graph g = engine.getGraph();

		Node startNode = g.getNode(new Coordinate(33.2403307, 126.5624673));
		Node endNode = g.getNode(new Coordinate(33.2417782, 126.5647375));
		
		ArrayList<Node> path = (ArrayList<Node>)engine.shortestPathFind(startNode, endNode);
		
		assertThat(path).isNotEmpty();
		
		path.forEach(item -> System.out.println(item.getCoordinate().toWKT()));
	}

	@Test
	@DisplayName("경로탐색 - 시작 노드와 종료노드가 정확한 위치인지")
	void findPathByNodeExactTest() {	
		Graph g = engine.getGraph();

		Node startNode = g.getNode(new Coordinate(33.2403307, 126.5624673));
		Node endNode = g.getNode(new Coordinate(33.2417782, 126.5647375));
		
		ArrayList<Node> path = (ArrayList<Node>)engine.shortestPathFind(startNode, endNode);
		
		assertThat(path).isNotEmpty();
		assertTrue(path.get(0).getCoordinate().equals(startNode.getCoordinate()));
		assertTrue(path.get(path.size() - 1).getCoordinate().equals(endNode.getCoordinate()));
		
		path.forEach(item -> System.out.println(item.getCoordinate().toWKT()));
	}

	@Test
	@DisplayName("경로탐색 - 탐색 경로의 노드수가 기대값과 같은지")
	void findPathByNodeCountTest() {	
		// 탐색 경로 기대값은 qgis에서 직접 확인
		Graph g = engine.getGraph();

		Node startNode = g.getNode(new Coordinate(33.2403307, 126.5624673));
		Node endNode = g.getNode(new Coordinate(33.2417782, 126.5647375));
		
		ArrayList<Node> path = (ArrayList<Node>)engine.shortestPathFind(startNode, endNode);
		
		assertThat(path).isNotEmpty();
		assertThat(path.size()).isEqualTo(7);
		
		path.forEach(item -> System.out.println(item.getCoordinate().toWKT()));
	}

	@Test
	@DisplayName("경로탐색 - 탐색된 경로가 모두 이어져 있는지")
	void findPathByNodeConnectTest() {	
		Graph g = engine.getGraph();

		Node startNode = g.getNode(new Coordinate(33.2403307, 126.5624673));
		Node endNode = g.getNode(new Coordinate(33.2417782, 126.5647375));
		
		ArrayList<Node> path = (ArrayList<Node>)engine.shortestPathFind(startNode, endNode);
		
		assertThat(path).isNotEmpty();
		
		boolean isConnected = true;
		for(int i=0; i < path.size() - 1; i++) {
			Node n1 = path.get(i);
			Node n2 = path.get(i + 1);
			if(n1.getAdjacentNodes().contains(n2) == false) {
				isConnected = false;
				break;
			}
		}

		assertTrue(isConnected);
		
		path.forEach(item -> System.out.println(item.getCoordinate().toWKT()));
	}
	
	@Test
	@DisplayName("경로탐색 - 좌표")
	void findPathByCoordinateTest() {	
		Coordinate startCoordinate = new Coordinate(33.2403307, 126.5624673);
		Coordinate endCoordinate = new Coordinate(33.2417782, 126.5647375);
		
		ArrayList<Node> path = (ArrayList<Node>)engine.shortestPathFind(startCoordinate, endCoordinate);
		
		assertThat(path).isNotEmpty();
	}
	
	@Test
	@DisplayName("경로탐색 - 그래프에 없는 좌표")
	void findPathByCooridnateTest() {
		// 시작 지점 없는 좌표 33.4822905, 126.4904020
		// 목표 지점 없는 좌표 33.4844175, 126.4962931
		// start=33.3209235283,126.2460707194
		// end=33.4893700755,126.5038611983
		Coordinate startCoordinate = new Coordinate(33.3209235283,126.2460707194);
		Coordinate endCoordinate = new Coordinate(33.4893700755, 126.5038611983);
		
		ArrayList<Node> path = (ArrayList<Node>)engine.shortestPathFind(startCoordinate, endCoordinate);
		
		assertThat(path).isNotEmpty();
		assertTrue(PathUtil.haversine(path.get(0).getCoordinate(), startCoordinate) < 5);
		assertTrue(PathUtil.haversine(path.get(path.size() - 1).getCoordinate(), endCoordinate) < 5);
		
		path.forEach(item -> System.out.println(item.getCoordinate().toWKT()));
	}

}
