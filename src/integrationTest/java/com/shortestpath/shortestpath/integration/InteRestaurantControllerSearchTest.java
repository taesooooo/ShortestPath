package com.shortestpath.shortestpath.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortestpath.TestApplication;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.dto.response.ResponseRestaurantDto;
import com.shortestpath.shortestpath.dto.response.ResponseRestaurantSearchDto;

/**
 * RestaurantController 통합 테스트
 * 
 * 테스트 대상 엔드포인트:
 * - GET /api/restaurants (모든 음식점 조회)
 * - GET /api/restaurants/{id} (ID로 특정 음식점 조회)
 * - GET /api/restaurants/category/{category} (카테고리별 조회)
 * - GET /api/restaurants/search?keyword={keyword} (키워드 검색)
 */
@ActiveProfiles("inte")
@SpringBootTest(classes=TestApplication.class)
@Transactional
class InteRestaurantControllerSearchTest {

	private static final Logger log = LoggerFactory.getLogger(InteRestaurantControllerSearchTest.class);
	
	@Autowired
	private WebApplicationContext context;
	
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper om;
	
	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	@DisplayName("모든 음식점 조회 - 정상 조회 성공")
	void getAllRestaurants_Success() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/restaurants")
				.param("page", "1")
				.param("size", "10"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.content.length()").value(greaterThan(0)))
			.andExpect(jsonPath("$.totalElements").exists())
			.andExpect(jsonPath("$.totalPages").exists())
			.andReturn();
		
		// DTO로 변환하여 리스트 전체 검증
		String content = result.getResponse().getContentAsString();
		JsonNode node = om.readTree(content).get("content");
		List<ResponseRestaurantSearchDto> Restaurants = om.readValue(node.toString(), om.getTypeFactory().constructCollectionType(List.class, ResponseRestaurantSearchDto.class)
		);
		
		assertThat(Restaurants).isNotEmpty();
		
		assertThat(Restaurants).allMatch(store -> store.getId() != null, "ID가 null이 아니어야 함");
		assertThat(Restaurants).allMatch(store -> store.getBplcNm() != null, "음식점명이 null이 아니어야 함");
	}

	@Test
	@DisplayName("ID로 음식점 조회 - 존재하는 음식점 조회 성공")
	void getRestaurantById_Success() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/restaurants/{id}", 1))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andReturn();

		String content = result.getResponse().getContentAsString();
		ResponseRestaurantDto dto = om.readValue(content, ResponseRestaurantDto.class);

		assertThat(dto).isNotNull();
	}

	@Test
	@DisplayName("ID로 음식점 조회 - 존재하지 않는 ID 조회시 404")
	void getRestaurantById_NotFound() throws Exception {
		mockMvc.perform(get("/api/restaurants/00000"))
				.andDo(print())
				.andExpect(status().is(404))
				.andReturn();
	}

	@Test
	@DisplayName("ID로 음식점 조회 - 유효하지 않은 ID 형식")
	void getRestaurantById_InvalidIdFormat() throws Exception {
		mockMvc.perform(get("/api/restaurants/invalid"))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andReturn();
	}

	@Test
	@DisplayName("카테고리별 조회 - 한식 카테고리 조회")
	void getRestaurantsByCategory_Korean() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/restaurants/category/한식")
				.param("page", "1")
				.param("size", "10"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/json"))
			.andReturn();

		String content = result.getResponse().getContentAsString();
		JsonNode node = om.readTree(content).get("content");
		List<ResponseRestaurantSearchDto> searchResults = om.readValue(node.toString(), om.getTypeFactory().constructCollectionType(List.class, ResponseRestaurantSearchDto.class));

		assertThat(searchResults).isNotEmpty();
		assertThat(searchResults).extracting(ResponseRestaurantSearchDto::getBzstatSeNm).contains("한식");
	}

	@Test
	@DisplayName("카테고리별 조회 - 존재하지 않는 카테고리 조회시 빈 배열")
	void getRestaurantsByCategory_NonExistent() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/restaurants/category/존재하지않는카테고리"))
				.andDo(print())
				.andExpect(status().isNotFound())
				.andReturn();

	}

	@Test
	@DisplayName("음식점 검색 - 키워드 '피자' 검색 성공")
	void searchRestaurants_KeywordKorean() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/restaurants/search")
				.param("page", "1")
				.param("size", "10")
				.param("keyword", "피자"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/json"))
			.andReturn();

		String content = result.getResponse().getContentAsString();
		JsonNode node = om.readTree(content).get("content");
		List<ResponseRestaurantSearchDto> searchResults = om.readValue(node.toString(), om.getTypeFactory().constructCollectionType(List.class, ResponseRestaurantSearchDto.class));

		assertThat(searchResults).isNotEmpty();

		assertThat(searchResults).extracting(ResponseRestaurantSearchDto::getBplcNm).allMatch(state -> ((String) state).contains("피자"));
	}

	@Test
	@DisplayName("음식점 검색 - 존재하지 않는 키워드 검색시 빈 배열")
	void searchRestaurants_NonExistentKeyword() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/restaurants/search")
				.param("keyword", "존재하지않는음식점"))
				.andDo(print())
				.andExpect(status().isNotFound())
				.andReturn();
		
	}

	@Test
	@DisplayName("음식점 검색 - bbox 검색")
	void searchRestaurants_Bbox() throws Exception {
		Envelope envelope = new Envelope(127.02503272295894, 127.20324474749289, 36.74404275122443, 36.9037120534218);
		CoordinateReferenceSystem sCRS = CRS.decode("EPSG:4326", true);
		CoordinateReferenceSystem tCRS = CRS.decode("EPSG:5174", true	);
		MathTransform transform = CRS.findMathTransform(sCRS, tCRS, true);

		Envelope transformedEnvelope = JTS.transform(envelope, transform);

		MvcResult result = mockMvc.perform(get("/api/restaurants/search")
				.param("page", "1")
				.param("size", "10")
				.param("minLat", "36.74404275122443")
				.param("minLon", "127.02503272295894")
				.param("maxLat", "36.9037120534218")
				.param("maxLon", "127.20324474749289"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/json"))
			.andReturn();

		String content = result.getResponse().getContentAsString();
		JsonNode node = om.readTree(content).get("content");
		List<ResponseRestaurantSearchDto> searchResults = om.readValue(node.toString(), om.getTypeFactory().constructCollectionType(List.class, ResponseRestaurantSearchDto.class));

		assertThat(searchResults).isNotEmpty();
		
		assertThat(searchResults).as("BBox 검색 결과가 지정된 영역 내에 있어야 합니다.").allMatch((item) -> (item.getX() == null && item.getY() == null) ? true : transformedEnvelope.contains(new Coordinate(item.getX(), item.getY())));
	}

	@Test
	@DisplayName("음식점 검색 - 키워드, 카테고리, bbox 검색")
	void searchRestaurants_keyword_category_bbox() throws Exception {
		// EPSG 4326
		Envelope envelope = new Envelope(127.145, 127.159, 36.814, 36.824); 

		MvcResult result = mockMvc.perform(get("/api/restaurants/search")
				.param("page", "1")
				.param("size", "10")
				.param("keyword", "삼겹살")
				.param("category", "한식")

				.param("minLat", "36.814")
				.param("minLon", "127.145")
				.param("maxLat", "36.824")
				.param("maxLon", "127.159"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/json"))
			.andReturn();

		String content = result.getResponse().getContentAsString();
		JsonNode node = om.readTree(content).get("content");
		List<ResponseRestaurantSearchDto> searchResults = om.readValue(node.toString(), om.getTypeFactory().constructCollectionType(List.class, ResponseRestaurantSearchDto.class));

		assertThat(searchResults).isNotEmpty();
		assertThat(searchResults).extracting(ResponseRestaurantSearchDto::getBzstatSeNm).contains("한식");
		
		assertThat(searchResults).as("BBox 검색 결과가 지정된 영역 내에 있어야 합니다.").allMatch((item) -> envelope.intersects(new Coordinate(item.getGeometry().getLongitude(), item.getGeometry().getLatitude())));
	}
}
