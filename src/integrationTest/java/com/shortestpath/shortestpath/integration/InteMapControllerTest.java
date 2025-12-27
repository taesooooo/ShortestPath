package com.shortestpath.shortestpath.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;

import jakarta.transaction.Transactional;

@ActiveProfiles("inte")
@SpringBootTest
@Transactional
class InteMapControllerTest {
	private static final Logger log = LoggerFactory.getLogger(InteMapControllerTest.class);

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private NodeProvider nodeIndexProvider;
	
	private MockMvc mockMvc;
	private ObjectMapper om = new ObjectMapper();
	private HybridDataStore store;

	@BeforeEach
	void setUp() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = getClass().getClassLoader().getResource("sample/sample_jeju.shp").getPath();
        this.store = new HybridDataStore(new File(path).getParent(), nodeIndexProvider);
        Extractor extractor = new NodeEdgeExtractor(path, this.store);
		extractor.extract();
	}
	
	@Test
	@DisplayName("경로 탐색 요청(리스트) - 정상")
	public void findMapListTest() throws Exception {
		// 33.2403307/126.5624673|33.2417782/126.5647375
		// 33.2417782/126.5647375|33.2573009/126.574876

		this.mockMvc.perform(get("/api/map/find-path")
				.queryParam("coordinates", "33.2403307/126.5624673|33.2417782/126.5647375,33.2417782/126.5647375|33.2573009/126.574876")
				.accept(MediaType.APPLICATION_JSON_VALUE)
				.characterEncoding("UTF-8"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(jsonPath("$").isArray())
		.andExpect(jsonPath("$.[0].routeList").isNotEmpty())
		.andExpect(jsonPath("$.[0].routeList").isArray())
		.andExpect(jsonPath("$.[0].routeList[0].latitude").isNumber())
		.andExpect(jsonPath("$.[0].routeList[0].longitude").isNumber());
	}
	
	@Test
	@DisplayName("경로 탐색 요청(단일) - 정상")
	public void findMapTest() throws Exception {
		// 33.2403307/126.5624673|33.2417782/126.5647375
		
		this.mockMvc.perform(get("/api/map/find-path")
				.queryParam("coordinates", "33.2403307/126.5624673|33.2417782/126.5647375")
				.accept(MediaType.APPLICATION_JSON_VALUE)
				.characterEncoding("UTF-8"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(jsonPath("$").isArray())
		.andExpect(jsonPath("$.[0].routeList").isNotEmpty())
		.andExpect(jsonPath("$.[0].routeList").isArray())
		.andExpect(jsonPath("$.[0].routeList[0].latitude").isNumber())
		.andExpect(jsonPath("$.[0].routeList[0].longitude").isNumber());
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
		return Stream.of( "33.4824388-126.4898217|33.4845859-126.4963428",
				"33.2417782/126.5647375",
				"126.4824388/33.4898217|33.4845859/126.4963428");
	}

	@Test
	@DisplayName("경로 탐색 요청 - 경로 없음")
	public void notFoundPathTest() throws Exception {
		this.mockMvc.perform(get("/api/map/find-path")
				.queryParam("coordinates", "33.0000000/126.0000000|33.1000000/126.1000000")
				.accept(MediaType.APPLICATION_JSON_VALUE)
				.characterEncoding("UTF-8"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.[0].routeList").isEmpty());
	}

}
