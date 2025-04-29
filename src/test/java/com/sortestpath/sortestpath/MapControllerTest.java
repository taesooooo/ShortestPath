package com.sortestpath.sortestpath;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
import com.sortestpath.sortestpath.dto.request.RequestFindPathDto;

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
	@DisplayName("경로 탐색 요청 - 정상")
	void findMapTest() throws Exception {	
		mockMvc.perform(get("/api/map/find-path")
				.queryParam("start", "33.4824388,126.4898217")
				.queryParam("end", "33.4845859,126.4963428")
				.accept(MediaType.APPLICATION_JSON)
				.characterEncoding("UTF-8"))
		.andDo(print())
		.andExpect(status().isOk());
	}
	
	@ParameterizedTest()
	@MethodSource("testArguments")
	@DisplayName("경로 탐색 요청 - 잘못된 좌표")
	void findMapInValidCoordinateTest(String startCo, String endCo) throws Exception {
		mockMvc.perform(get("/api/map/find-path")
				.accept(MediaType.APPLICATION_JSON)
				.characterEncoding("UTF-8"))
		.andDo(print())
		.andExpect(status().isBadRequest());
	}

	private static Stream<Arguments> testArguments() {
		return Stream.of(
				Arguments.of("33.2417782,126.5647375", "321, 0123"),				// 잘못된 End 좌표
				Arguments.of("123123, 125.32)", "33.2387792,126.6015835"),			// 잘못된 Start 좌표
				Arguments.of("126.5647375,33.2417782", "33.2387792,126.6015835"), // 잘못된 Start 좌표 위도,경도 뒤바뀜
				Arguments.of("33.2417782,126.5647375", "126.6015835,33.2387792"), // 잘못된 End 좌표 위도,경도 뒤바뀜
				Arguments.of("0,0", "0,0")										// 잘못된 좌표
				);
	}

}
