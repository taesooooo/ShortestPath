package com.shortestpath.shortestpath.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortestpath.shortestpath.DBHelper;
import com.shortestpath.shortestpath.IntegrationTestHelper;
import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.TraceRoute;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;
import com.shortestpath.shortestpath.dto.response.ResponeseRouteSearchTraceDto;
import com.shortestpath.shortestpath.dto.response.ResponseRouteStepDto;

import jakarta.transaction.Transactional;

@ActiveProfiles("inte")
@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InteMapControllerTest {
	private static final Logger log = LoggerFactory.getLogger(InteMapControllerTest.class);

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private Engine engine;

	@Autowired
	private DBHelper dbHelper;

	private MockMvc mockMvc;
	private ObjectMapper om = new ObjectMapper();

	@BeforeEach
	void setUp() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@AfterAll
    public void destroy() throws IOException {
		engine.getStore().close();
        IntegrationTestHelper.deleteBinaryFiles(((HybridDataStore) engine.getStore()));
		dbHelper.turncate();
    }

	@Test
	@DisplayName("경로 탐색 요청(리스트) - 정상")
	public void findMapListTest() throws Exception {
		// 33.2403307/126.5624673|33.2417782/126.5647375
		// 33.2417782/126.5647375|33.2573009/126.574876

		MvcResult mvcResult = this.mockMvc.perform(get("/api/map/find-path")
				.queryParam("coordinates",
						"33.2403307/126.5624673|33.2417782/126.5647375,33.2417782/126.5647375|33.2573009/126.574876")
				.accept(MediaType.APPLICATION_JSON_VALUE)
				.characterEncoding("UTF-8"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.[0].routeSteps").isArray())
				.andExpect(jsonPath("$.[0].routeSteps[0].coordinate.latitude").isNumber())
				.andExpect(jsonPath("$.[0].routeSteps[0].coordinate.longitude").isNumber())
				.andExpect(jsonPath("$.[0].routeSteps[0].turnDirection").value("START"))
				.andReturn();

		JsonNode response = om.readTree(mvcResult.getResponse().getContentAsString());
		assertRouteGuide(response.get(0));
		assertRouteGuide(response.get(1));
	}

	@Test
	@DisplayName("경로 탐색 요청(단일) - 정상")
	public void findMapTest() throws Exception {
		// 33.2403307/126.5624673|33.2417782/126.5647375

		MvcResult mvcResult = this.mockMvc.perform(get("/api/map/find-path")
				.queryParam("coordinates", "33.2403307/126.5624673|33.2417782/126.5647375")
				.accept(MediaType.APPLICATION_JSON_VALUE)
				.characterEncoding("UTF-8"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.[0].routeSteps").isArray())
				.andExpect(jsonPath("$.[0].routeSteps[0].coordinate.latitude").isNumber())
				.andExpect(jsonPath("$.[0].routeSteps[0].coordinate.longitude").isNumber())
				.andExpect(jsonPath("$.[0].routeSteps[0].turnDirection").value("START"))
				.andReturn();

		JsonNode response = om.readTree(mvcResult.getResponse().getContentAsString());
		assertRouteGuide(response.get(0));
	}

	@ParameterizedTest()
	@MethodSource("testArguments")
	@DisplayName("경로 탐색 요청 - 잘못된 좌표")
	private void findMapInValidCoordinateTest(String parameter) throws Exception {
		this.mockMvc.perform(get("/api/map/find-path")
				.param("coordinates", parameter)
				.accept(MediaType.APPLICATION_JSON)
				.characterEncoding("UTF-8"))
				.andDo(print())
				.andExpect(status().isBadRequest());
	}

	// 순서대로
	// 잘못된 형식, 잘못된 자표
	private static Stream<String> testArguments() {
		return Stream.of("33.4824388-126.4898217|33.4845859-126.4963428",
				"33.2417782/126.5647375",
				"126.4824388/33.4898217|33.4845859/126.4963428");
	}

	@Test
	@DisplayName("경로 추적 요청 - 정상")
	public void searchRouteTrackTest() throws Exception {
		ArrayList<Coordinate> routeCoordinates = new ArrayList<Coordinate>();
		routeCoordinates.add(new Coordinate(33.2403234, 126.5627931));
		routeCoordinates.add(new Coordinate(33.2402282, 126.5630821));
		routeCoordinates.add(new Coordinate(33.2404177, 126.5631293));
		routeCoordinates.add(new Coordinate(33.2409855, 126.5631549));
		routeCoordinates.add(new Coordinate(33.2408904, 126.5637502));
		routeCoordinates.add(new Coordinate(33.2407988, 126.5643231));
		routeCoordinates.add(new Coordinate(33.2408074, 126.5644749));

		ArrayList<Coordinate> parentCoordinates = new ArrayList<Coordinate>();
		parentCoordinates.add(new Coordinate(33.2403234, 126.5627931));
		parentCoordinates.add(new Coordinate(33.2402282, 126.56308210000002));
		parentCoordinates.add(new Coordinate(33.2404177, 126.5631293));
		parentCoordinates.add(new Coordinate(33.2409855, 126.5631549));
		parentCoordinates.add(new Coordinate(33.2408904, 126.5637502));
		parentCoordinates.add(new Coordinate(33.2407988, 126.5643231));
		parentCoordinates.add(new Coordinate(33.2408074, 126.5644749));


		ArrayList<Coordinate> visitedCoordinates = new ArrayList<Coordinate>();
		// visited coordinates from provided POINT list
		visitedCoordinates.add(new Coordinate(33.2403307, 126.5624673));
		visitedCoordinates.add(new Coordinate(33.2402282, 126.5630821));
		visitedCoordinates.add(new Coordinate(33.2404177, 126.5631293));
		visitedCoordinates.add(new Coordinate(33.2401702, 126.5632367));
		visitedCoordinates.add(new Coordinate(33.2409855, 126.5631549));
		visitedCoordinates.add(new Coordinate(33.2418930, 126.5631720));
		visitedCoordinates.add(new Coordinate(33.2408904, 126.5637502));
		visitedCoordinates.add(new Coordinate(33.2409625, 126.5624993));
		visitedCoordinates.add(new Coordinate(33.2412932, 126.5638586));
		visitedCoordinates.add(new Coordinate(33.2404554, 126.5635482));
		visitedCoordinates.add(new Coordinate(33.2407988, 126.5643231));
		visitedCoordinates.add(new Coordinate(33.2408074, 126.5644749));

		MvcResult mvcResult = this.mockMvc.perform(get("/api/map/search-route-track")
				.queryParam("coordinates", "33.2403234/126.5627931|33.2408074/126.5644749")
				.accept(MediaType.APPLICATION_JSON_VALUE)
				.characterEncoding("UTF-8"))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		String contentAsString = mvcResult.getResponse().getContentAsString();
		ResponeseRouteSearchTraceDto responseDto = om.readValue(contentAsString, ResponeseRouteSearchTraceDto.class);

		responseDto.getRouteSteps().stream()
				.map(ResponseRouteStepDto::getCoordinate)
				.forEach(item -> System.out.println(item.toWKT()));

		assertThat(responseDto.getRouteSteps()).as("예상한 탐색 경로 좌표가 일치하지 않습니다.")
				.extracting(ResponseRouteStepDto::getCoordinate)
				.containsExactlyElementsOf(routeCoordinates);

		assertThat(responseDto.getTraceRoutes()).as("예상한 탐색 부모 좌표가 없습니다.")
				.flatExtracting(TraceRoute::getParentCoordinate)
				.containsExactlyInAnyOrderElementsOf(parentCoordinates);

		assertThat(responseDto.getTraceRoutes()).as("예상한 방문 좌표가 없습니다.")
		.flatExtracting(TraceRoute::getVisitedCoordinates)
				.containsExactlyInAnyOrderElementsOf(visitedCoordinates);
	}

	private void assertRouteGuide(JsonNode routeResult) {
		JsonNode routeSteps = routeResult.get("routeSteps");

		assertThat(routeSteps).as("경로 안내 정보가 없습니다.").isNotNull();

		if (routeSteps.isEmpty()) {
			return;
		}

		assertRouteStepHasCoordinate(routeSteps.get(0));
		assertThat(routeSteps.get(0).get("turnDirection").asText()).isEqualTo("START");

		if (routeSteps.size() > 1) {
			JsonNode lastStep = routeSteps.get(routeSteps.size() - 1);
			assertRouteStepHasCoordinate(lastStep);
			assertThat(lastStep.get("turnDirection").asText()).isEqualTo("END");
		}

		for (JsonNode routeStep : routeSteps) {
			assertRouteStepHasCoordinate(routeStep);
			assertThat(routeStep.get("turnDirection").asText())
					.isIn("START", "STRAIGHT", "LEFT", "RIGHT", "U_TURN", "END");
		}
	}

	private void assertRouteStepHasCoordinate(JsonNode routeStep) {
		JsonNode stepCoordinate = routeStep.get("coordinate");

		assertThat(stepCoordinate).isNotNull();
		assertThat(stepCoordinate.get("latitude").isNumber()).isTrue();
		assertThat(stepCoordinate.get("longitude").isNumber()).isTrue();
	}

	// @Test
	// @DisplayName("경로 탐색 요청 - 경로 없음")
	// public void notFoundPathTest() throws Exception {
	// this.mockMvc.perform(get("/api/map/find-path")
	// .queryParam("coordinates", "33.0000000/126.0000000|33.1000000/126.1000000")
	// .accept(MediaType.APPLICATION_JSON_VALUE)
	// .characterEncoding("UTF-8"))
	// .andDo(print())
	// .andExpect(status().isOk())
	// .andExpect(jsonPath("$.[0].routeList").isEmpty());
	// }

}
