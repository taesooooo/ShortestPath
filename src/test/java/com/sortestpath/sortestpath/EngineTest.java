package com.sortestpath.sortestpath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sortestpath.sortestpath.core.pathengine.Coordinate;
import com.sortestpath.sortestpath.core.pathengine.Engine;
import com.sortestpath.sortestpath.core.pathengine.Graph;
import com.sortestpath.sortestpath.core.pathengine.Loader;
import com.sortestpath.sortestpath.core.pathengine.Node;
import com.sortestpath.sortestpath.util.PathUtil;

class EngineTest {
	private static final Logger log = LoggerFactory.getLogger(EngineTest.class);

	String filePath = getClass().getClassLoader().getResource("shp/test.shp").getFile();
	
	Loader loader;
	Engine engine;

	@BeforeEach
	void setUp() throws Exception {
		loader = new Loader(filePath);
		engine = new Engine(loader);
	}

	@Test
	@DisplayName("경로탐색 - 노드")
	void findPathByNodeTest() {	
		Graph g = engine.getGraph();

		Node startNode = g.getNode(new Coordinate(33.4824388, 126.4898217));
		Node endNode = g.getNode(new Coordinate(33.4845859, 126.4963428));
		
		ArrayList<Node> path = (ArrayList<Node>)engine.shortestPathFind(startNode, endNode);
		
		assertThat(path).isNotEmpty();
	}
	
	@Test
	@DisplayName("경로탐색 - 좌표")
	void findPathByCoordinateTest() {	
		Coordinate startCoordinate = new Coordinate(33.2417782, 126.5647375);
		Coordinate endCoordinate = new Coordinate(33.2387792, 126.6015835);
		
		ArrayList<Node> path = (ArrayList<Node>)engine.shortestPathFind(startCoordinate, endCoordinate);
		
		assertThat(path).isNotEmpty();
	}
	
	@Test
	@DisplayName("경로탐색 - 그래프에 없는 좌표")
	void findPathByCooridnateTest() {
		// 시작 지점 없는 좌표 33.4822905, 126.4904020
		// 목표 지점 없는 좌표 33.4844175, 126.4962931
		Coordinate startCoordinate = new Coordinate(33.4822905, 126.4904020);
		Coordinate endCoordinate = new Coordinate(33.4844175, 126.4962931);
		
		ArrayList<Node> path = (ArrayList<Node>)engine.shortestPathFind(startCoordinate, endCoordinate);
		
		assertThat(path).isNotEmpty();
		assertTrue(PathUtil.haversine(path.get(0).getCoordinate(), startCoordinate) < 5);
		assertTrue(PathUtil.haversine(path.get(path.size() - 1).getCoordinate(), endCoordinate) < 5);
	}

}
