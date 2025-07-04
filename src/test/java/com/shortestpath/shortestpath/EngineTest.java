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

//	private String filePath = getClass().getClassLoader().getResource("shp/test.shp").getFile();
	private String filePath = "C:\\Users\\Bear\\Documents\\gis\\제주링크.shp";
	
	private Loader loader;
	private Engine engine;
	
	@Autowired
	private DataProvider dataProvider;

	@BeforeEach
	void setUp() throws Exception {
		loader = new Loader(filePath);
		engine = new Engine(loader, dataProvider);
	}

	@Test
	@DisplayName("경로탐색 - 노드")
	void findPathByNodeTest() {	
		Graph g = engine.getGraph();

		Node startNode = g.getNode(new Coordinate(33.4824388, 126.4898217));
		Node endNode = g.getNode(new Coordinate(33.4845859, 126.4963428));
		
		ArrayList<Node> path = (ArrayList<Node>)engine.shortestPathFind(startNode, endNode);
		
		assertThat(path).isNotEmpty();
		
		path.forEach(item -> System.out.println(item.getCoordinate().toWKT()));
	}
	
	@Test
	@DisplayName("경로탐색 - 좌표")
	void findPathByCoordinateTest() {	
		Coordinate startCoordinate = new Coordinate(33.4824388, 126.4898217);
		Coordinate endCoordinate = new Coordinate(33.4845859, 126.4963428);
		
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
