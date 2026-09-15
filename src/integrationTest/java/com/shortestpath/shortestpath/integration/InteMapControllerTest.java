package com.shortestpath.shortestpath.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortestpath.TestApplication;

@ActiveProfiles("inte")
@SpringBootTest(classes=TestApplication.class)
@Transactional
class InteMapControllerTest {
	private static final Logger log = LoggerFactory.getLogger(InteMapControllerTest.class);

	@Autowired
	private WebApplicationContext context;
	// @Autowired
	// private Engine engine;
	// @Autowired
	// private JdbcTemplate jdbcTemplate;

	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper om;

	@BeforeEach
	void setUp() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
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

	}

	@ParameterizedTest
	@MethodSource("testArguments")
	@DisplayName("경로 탐색 요청 - 잘못된 좌표")
	public void findMapInValidCoordinateTest(String parameter) throws Exception {
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

	// private void assertRouteGuide(JsonNode routeResult) {
	// 	JsonNode routeSteps = routeResult.get("routeSteps");

	// 	assertThat(routeSteps).as("경로 안내 정보가 없습니다.").isNotNull();

	// 	if (routeSteps.isEmpty()) {
	// 		return;
	// 	}

	// 	assertRouteStepHasCoordinate(routeSteps.get(0));
	// 	assertThat(routeSteps.get(0).get("turnDirection").asText()).isEqualTo("START");

	// 	if (routeSteps.size() > 1) {
	// 		JsonNode lastStep = routeSteps.get(routeSteps.size() - 1);
	// 		assertRouteStepHasCoordinate(lastStep);
	// 		assertThat(lastStep.get("turnDirection").asText()).isEqualTo("END");
	// 	}

	// 	for (JsonNode routeStep : routeSteps) {
	// 		assertRouteStepHasCoordinate(routeStep);
	// 		assertThat(routeStep.get("turnDirection").asText())
	// 				.isIn("START", "STRAIGHT", "LEFT", "RIGHT", "U_TURN", "END");
	// 	}
	// }

	// private void assertRouteStepHasCoordinate(JsonNode routeStep) {
	// 	JsonNode stepCoordinate = routeStep.get("coordinate");

	// 	assertThat(stepCoordinate).isNotNull();
	// 	assertThat(stepCoordinate.get("latitude").isNumber()).isTrue();
	// 	assertThat(stepCoordinate.get("longitude").isNumber()).isTrue();
	// }

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
