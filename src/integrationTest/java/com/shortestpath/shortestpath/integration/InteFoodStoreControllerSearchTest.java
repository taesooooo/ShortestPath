package com.shortestpath.shortestpath.integration;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortestpath.shortestpath.dto.response.ResponseFoodStoreDto;
import com.shortestpath.shortestpath.dto.response.ResponseFoodStoreSearchDto;

import jakarta.transaction.Transactional;

/**
 * FoodStoreController 통합 테스트
 * 
 * 테스트 대상 엔드포인트:
 * - GET /api/foodstores (모든 음식점 조회)
 * - GET /api/foodstores/{id} (ID로 특정 음식점 조회)
 * - GET /api/foodstores/category/{category} (카테고리별 조회)
 * - GET /api/foodstores/search?keyword={keyword} (키워드 검색)
 */
@ActiveProfiles("inte")
@SpringBootTest
@Transactional
class InteFoodStoreControllerSearchTest {
	private static final Logger log = LoggerFactory.getLogger(InteFoodStoreControllerSearchTest.class);
	
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
	void getAllFoodStores_Success() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/foodstores")
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
		List<ResponseFoodStoreSearchDto> foodStores = om.readValue(node.toString(), om.getTypeFactory().constructCollectionType(List.class, ResponseFoodStoreSearchDto.class)
		);
		
		assertThat(foodStores).isNotEmpty();
		
		assertThat(foodStores).allMatch(store -> store.getId() != null, "ID가 null이 아니어야 함");
		assertThat(foodStores).allMatch(store -> store.getBplcNm() != null, "음식점명이 null이 아니어야 함");
		assertThat(foodStores).allMatch(store -> store.getTrdStateNm() != null, "상태명이 null이 아니어야 함");
		
		assertThat(foodStores).allMatch(store -> store.getTrdStateNm().contains("영업"), "영업 상태여야 함");
	}

	@Test
	@DisplayName("ID로 음식점 조회 - 존재하는 음식점 조회 성공")
	void getFoodStoreById_Success() throws Exception {
		ResponseFoodStoreDto expected = ResponseFoodStoreDto.builder()
				.id(2094496L)
				.opnStdNmCd(5580000)
				.mgtNo("5580000-101-2006-00017")
				.trdStateNm("영업/정상")
				.dcbDt(null)
				.locPostNo("321-900")
				.rdnPostNo("32826")
				.bplcNm("다향")
				.uptaeGbnNm("중국식")
				.dtUpdGbn("I")
				.multiUseYn(false)
				.dtUpdTm(LocalDate.of(2026, 1, 14))
				.rdnWhlAddr("충청남도 계룡시 서금암3길 5 (금암동)")
				.dtlTrdStateNm("영업")
				.dtlTrdStateCd("01")
				.trdStateCd("01")
				.telNo("0428410093")
				.x(222855.790714)
				.y(307932.893189)
				.siteWhlAddr("충청남도 계룡시 금암동 146-9")
				.homepage(null)
				.lastModetm(LocalDate.of(2021, 8, 9))
				.sidoNm("충청남도")
				.sigunguNm("계룡시")
				.lotNmMain(146)
				.lotNmSub(9)
				.emdNm("금암동")
				.buildingId(670365)
				.build();

		MvcResult result = mockMvc.perform(get("/api/foodstores/{id}", 2094496))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andReturn();

		String content = result.getResponse().getContentAsString();
		ResponseFoodStoreDto dto = om.readValue(content, ResponseFoodStoreDto.class);

		assertThat(dto).usingRecursiveComparison().isEqualTo(expected);
	}

	@Test
	@DisplayName("ID로 음식점 조회 - 존재하지 않는 ID 조회시 404")
	void getFoodStoreById_NotFound() throws Exception {
		mockMvc.perform(get("/api/foodstores/00000"))
				.andDo(print())
				.andExpect(status().is(404))
				.andReturn();
	}

	@Test
	@DisplayName("ID로 음식점 조회 - 유효하지 않은 ID 형식")
	void getFoodStoreById_InvalidIdFormat() throws Exception {
		mockMvc.perform(get("/api/foodstores/invalid"))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andReturn();
	}

	@Test
	@DisplayName("카테고리별 조회 - 한식 카테고리 조회")
	void getFoodStoresByCategory_Korean() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/foodstores/category/한식")
				.param("page", "1")
				.param("size", "10"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/json"))
			.andReturn();

		String content = result.getResponse().getContentAsString();
		JsonNode node = om.readTree(content).get("content");
		List<ResponseFoodStoreSearchDto> searchResults = om.readValue(node.toString(), om.getTypeFactory().constructCollectionType(List.class, ResponseFoodStoreSearchDto.class));

		assertThat(searchResults).isNotEmpty();
		assertThat(searchResults).extracting("trdStateNm").allMatch(state -> ((String) state).contains("영업"));
		assertThat(searchResults).extracting("uptaeGbnNm").contains("한식");
	}

	@Test
	@DisplayName("카테고리별 조회 - 존재하지 않는 카테고리 조회시 빈 배열")
	void getFoodStoresByCategory_NonExistent() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/foodstores/category/존재하지않는카테고리"))
				.andDo(print())
				.andExpect(status().isNotFound())
				.andReturn();

	}

	@Test
	@DisplayName("음식점 검색 - 키워드 '피자' 검색 성공")
	void searchFoodStores_KeywordKorean() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/foodstores/search")
				.param("page", "1")
				.param("size", "10")
				.param("keyword", "피자"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/json"))
			.andReturn();

		String content = result.getResponse().getContentAsString();
		JsonNode node = om.readTree(content).get("content");
		List<ResponseFoodStoreSearchDto> searchResults = om.readValue(node.toString(), om.getTypeFactory().constructCollectionType(List.class, ResponseFoodStoreSearchDto.class));

		assertThat(searchResults).isNotEmpty();
		assertThat(searchResults).extracting("trdStateNm").allMatch(state -> ((String) state).contains("영업"));
		assertThat(searchResults).extracting("bplcNm").allMatch(state -> ((String) state).contains("피자"));
	}

	@Test
	@DisplayName("음식점 검색 - 존재하지 않는 키워드 검색시 빈 배열")
	void searchFoodStores_NonExistentKeyword() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/foodstores/search")
				.param("keyword", "존재하지않는음식점"))
				.andDo(print())
				.andExpect(status().isNotFound())
				.andReturn();
		
	}
}
