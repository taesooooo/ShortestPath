package com.shortestpath.shortestpath.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import com.shortestpath.shortestpath.common.PageInfo;
import com.shortestpath.shortestpath.dto.request.RequestRestaurantSearchDto;
import com.shortestpath.shortestpath.dto.response.ResponseRestaurantSearchDto;
import com.shortestpath.shortestpath.mapper.RestaurantMapper;

@MybatisTest
@ActiveProfiles("inte")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan(basePackages = "com.shortestpath.shortestpath.mapper")
public class InterRestaurantMapper {

    @Autowired
    private RestaurantMapper restaurantMapper;
    
    @BeforeEach
    public void setup() {

    }

    @Test
    @DisplayName("아이디로 음식점 가져오기")
    public void selectByIdTest() {
        int id = 1;
        var restaurant = restaurantMapper.selectById(id);

        assertThat(restaurant).isNotNull();
        assertThat(restaurant.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("모든 음식점 가져오기") 
    public void selectByAllTest() {
        PageInfo testPageInfo = new PageInfo(1, 5);
        List<ResponseRestaurantSearchDto> restaurants = restaurantMapper.selectAll(testPageInfo);

        assertThat(restaurants)
            .extracting(ResponseRestaurantSearchDto::getId)
            .containsExactly(1, 2, 3, 4, 5);
    }         

    @Test
    @DisplayName("키워드로 음식점 조회")
    public void searchByKeywordTest() {
        PageInfo testPageInfo = new PageInfo(1, 10);
        RequestRestaurantSearchDto testSearchDto = RequestRestaurantSearchDto.builder()
            .keyword("피자")
            .build();
        List<ResponseRestaurantSearchDto> restaurants = restaurantMapper.search(testPageInfo, testSearchDto);

        assertThat(restaurants)
            .extracting(ResponseRestaurantSearchDto::getBplcNm)
            .allMatch(n -> n.contains("피자"));
    }

    @Test
    @DisplayName("카테고리로 음식점 조회")
    public void searchByCategoryTest() {
        PageInfo testPageInfo = new PageInfo(1, 10);
        RequestRestaurantSearchDto testSearchDto = RequestRestaurantSearchDto
            .builder()
            .category("한식")
            .build();
        List<ResponseRestaurantSearchDto> restaurants = restaurantMapper.search(testPageInfo, testSearchDto);

        assertThat(restaurants)
            .extracting(ResponseRestaurantSearchDto::getBzstatSeNm)
            .allMatch(n -> n.equals("한식"));
    }

    @Test
    @DisplayName("BBox로 음식점 조회")
    public void searchByBBoxTest() {
        PageInfo testPageInfo = new PageInfo(1, 10);
        RequestRestaurantSearchDto testSearchDto = RequestRestaurantSearchDto.builder()
            .minLat(36.816)
            .minLon(127.149)
            .maxLat(36.822)
            .maxLon(127.155)
            .build();
        
        Envelope testEnvelope = new Envelope(testSearchDto.getMinLon(), testSearchDto.getMaxLon(), testSearchDto.getMinLat(), testSearchDto.getMaxLat());
        List<ResponseRestaurantSearchDto> restaurants = restaurantMapper.search(testPageInfo, testSearchDto);

        assertThat(restaurants)
            .allMatch(r -> testEnvelope.contains(new Coordinate(r.getGeometry().getLongitude(), r.getGeometry().getLatitude())));
    }
}
