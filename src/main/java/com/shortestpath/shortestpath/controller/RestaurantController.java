package com.shortestpath.shortestpath.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shortestpath.shortestpath.common.Page;
import com.shortestpath.shortestpath.common.PageInfo;
import com.shortestpath.shortestpath.dto.request.RequestRestaurantSearchDto;
import com.shortestpath.shortestpath.dto.response.ResponseRestaurantSearchDto;
import com.shortestpath.shortestpath.dto.response.ResponseRestaurantDto;
import com.shortestpath.shortestpath.service.RestaurantService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {
    
    private final RestaurantService RestaurantService;
    
    /**
     * 모든 음식점 조회
     * GET /api/restaurants
     */
    @GetMapping
    public ResponseEntity<Page<ResponseRestaurantSearchDto>> getAllRestaurants(PageInfo pageInfo) {
        Page<ResponseRestaurantSearchDto> Restaurants = RestaurantService.getAllRestaurants(pageInfo);
        return ResponseEntity.ok(Restaurants);
    }
    
    /**
     * ID로 특정 음식점 조회
     * GET /api/restaurants/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseRestaurantDto> getRestaurantById(@PathVariable("id") int id) {
        ResponseRestaurantDto Restauant = RestaurantService.getRestaurantById(id);

        return ResponseEntity.ok(Restauant);
    }
    
    /**
     * 카테고리별 음식점 조회
     * GET /api/restaurants/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<ResponseRestaurantSearchDto>> getRestaurantsByCategory(PageInfo pageInfo, @RequestParam(value = "keyword", required = false) String keyword, @PathVariable("category") String category) {
        RequestRestaurantSearchDto searchDto = RequestRestaurantSearchDto.builder()
                .keyword(keyword)
                .category(category)
                .build();
        Page<ResponseRestaurantSearchDto> Restaurants = RestaurantService.searchRestaurants(pageInfo, searchDto);

        return ResponseEntity.ok(Restaurants);
    }
    
    /**
     * 키워드/카테고리/Bbox로 음식점 검색 (통합 검색)
     * GET /api/restaurants/search?keyword=한식&category=한식
     * GET /api/restaurants/search?minLat=37.0&minLon=127.0&maxLat=38.0&maxLon=128.0
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ResponseRestaurantSearchDto>> searchRestaurants(PageInfo pageInfo, RequestRestaurantSearchDto searchDto) {
        Page<ResponseRestaurantSearchDto> Restaurants = RestaurantService.searchRestaurants(pageInfo, searchDto);
        return ResponseEntity.ok(Restaurants);
    }
}
