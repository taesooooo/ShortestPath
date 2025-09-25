package com.shortestpath.shortestpath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.dto.request.RequestFindPathDto;
import com.shortestpath.shortestpath.dto.response.ResponseFindPathDto;
import com.shortestpath.shortestpath.service.MapServiceImpl;

class MapServiceTest {

	@Mock
	private Engine engine;

	@InjectMocks
	private MapServiceImpl mapService;

	@BeforeEach
	void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	@DisplayName("정상적인 값으로 반환하는지 테스트")
	void findPathTest() throws ParseException {
		// mock
		ArrayList<Node> testNodes = createTestNode();
		List<RequestFindPathDto> testRequestList = new ArrayList<RequestFindPathDto>();
		testRequestList.add(RequestFindPathDto.builder().start(new Coordinate(1,1)).end(new Coordinate(6, 6)).build());

		when(engine.shortestPathFind(any(Coordinate.class), any(Coordinate.class)))
				.thenReturn(testNodes);
		// test
		List<ResponseFindPathDto> responseFindPathDto = mapService.findPath(testRequestList);

		// verify
		assertThat(responseFindPathDto).isNotNull()
				.isNotEmpty();
		
		assertThat(responseFindPathDto.get(0).getRouteList()).
				as("정상적인 경로 반환을 하지 못했습니다.")
				.containsExactly(
						new Coordinate(1,1),
						new Coordinate(1,2),
						new Coordinate(2,2),
						new Coordinate(3,3),
						new Coordinate(4,4),
						new Coordinate(5,5),
						new Coordinate(5,6),
						new Coordinate(6,6)
						);
	}

	private ArrayList<Node> createTestNode() throws ParseException {
		ArrayList<Node> testNodes = new ArrayList<Node>();
		// MULTIPOINT ((1 1),(2 1),(2 2),(3 3),(4 4),(5 5),(6 5),(6 6))
		// MULTILINESTRING ((1 1, 2 1),(2 1, 2 2),(2 2, 3 3),(3 3, 4 4),(4 4, 5 5),(5 5, 6 5),(6 5, 6 6))

		// 반복문으로 노드와 엣지 생성
		int[][] points = {
			{1,1}, {2,1}, {2,2}, {3,3}, {4,4}, {5,5}, {6,5}, {6,6}
		};
		String[] lines = {"MULTILINESTRING ((1 1, 2 1))", "MULTILINESTRING ((2 1, 2 2))", "MULTILINESTRING ((2 2, 3 3))",
				"MULTILINESTRING ((3 3, 4 4))", "MULTILINESTRING ((4 4, 5 5))", "MULTILINESTRING ((5 5, 6 5))", "MULTILINESTRING ((6 5, 6 6))"};

		ArrayList<Node> nodes = new ArrayList<>();
		for (int i = 0; i < points.length; i++) {
			Node node = new Node(i+1, null, new Coordinate(points[i][0], points[i][1]), new HashMap<Integer, Edge>(), 0, 0, 0);
			nodes.add(node);
		}

		// MULTILINESTRING의 각 선분을 따라 양방향 엣지 연결
		for (int i = 0; i < lines.length; i++) {
			Node from = nodes.get(i);
			Node to = nodes.get(i+1);
			double distance = from.getCoordinate().calculateDistanceToTarget(to.getCoordinate());
			from.getEdge().put(to.getId(), new Edge(to, distance, new WKTReader().read(lines[i])));
			to.getEdge().put(from.getId(), new Edge(from, distance, new WKTReader().read(lines[i])));
		}

		// 테스트 노드 리스트에 추가
		testNodes.addAll(nodes);

		return testNodes;
	}

}
