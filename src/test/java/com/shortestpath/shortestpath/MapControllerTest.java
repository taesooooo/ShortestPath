package com.shortestpath.shortestpath;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@ActiveProfiles("test")
@SpringBootTest
class MapControllerTest {
	private static final Logger log = LoggerFactory.getLogger(MapControllerTest.class);

	@Autowired
	private WebApplicationContext context;
	
	private MockMvc mockMvc;
	private ObjectMapper om = new ObjectMapper();

	@BeforeEach
	void setUp() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}
	
	@Test
	@DisplayName("경로 탐색 요청(리스트) - 정상")
	public void findMapListTest() throws Exception {
		// 33.3057279944/126.2466098987|33.3209235283/126.2460707194,33.3209235283/126.2460707194|33.4018299117/126.6876111209
		// 33.4824388/126.4898217|33.4845859/126.4963428
		this.mockMvc.perform(get("/api/map/find-path")
				.queryParam("coordinates", "33.3057279944/126.2466098987|33.3209235283/126.2460707194,33.3209235283/126.2460707194|33.4018299117/126.6876111209")
				.accept(MediaType.APPLICATION_JSON_VALUE)
				.characterEncoding("UTF-8"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.routeDto").isArray())
		.andExpect(jsonPath("$.routeDto[0].routeList").isNotEmpty());
	}
	
	@ParameterizedTest()
	@MethodSource("testArguments")
	@DisplayName("경로 탐색 요청 - 잘못된 좌표")
	void findMapInValidCoordinateTest(String parameter) throws Exception {
		mockMvc.perform(get("/api/map/find-path")
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

}
