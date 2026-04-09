package com.shortestpath.shortestpath.controller;

import com.shortestpath.shortestpath.common.Page;
import com.shortestpath.shortestpath.common.PageInfo;
import com.shortestpath.shortestpath.dto.request.RequestFoodStoreSearchDto;
import com.shortestpath.shortestpath.dto.response.ResponseFoodStoreDto;
import com.shortestpath.shortestpath.dto.response.ResponseFoodStoreSearchDto;
import com.shortestpath.shortestpath.entity.FoodStore;
import com.shortestpath.shortestpath.service.FoodStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/foodstores")
@RequiredArgsConstructor
public class FoodStoreController {
    
    private final FoodStoreService foodStoreService;
    
    /**
     * 모든 음식점 조회
     * GET /api/foodstores
     */
    @GetMapping
    public ResponseEntity<Page<ResponseFoodStoreSearchDto>> getAllFoodStores(PageInfo pageInfo) {
        Page<ResponseFoodStoreSearchDto> foodStores = foodStoreService.getAllFoodStores(pageInfo);
        return ResponseEntity.ok(foodStores);
    }
    
    /**
     * ID로 특정 음식점 조회
     * GET /api/foodstores/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseFoodStoreDto> getFoodStoreById(@PathVariable("id") Long id) {
        ResponseFoodStoreDto foodStore = foodStoreService.getFoodStoreById(id);

        return ResponseEntity.ok(foodStore);
    }
    
    /**
     * 카테고리별 음식점 조회
     * GET /api/foodstores/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<ResponseFoodStoreSearchDto>> getFoodStoresByCategory(PageInfo pageInfo, @RequestParam(value = "keyword", required = false) String keyword, @PathVariable("category") String category) {
        RequestFoodStoreSearchDto searchDto = RequestFoodStoreSearchDto.builder()
                .keyword(keyword)
                .category(category)
                .build();
        Page<ResponseFoodStoreSearchDto> foodStores = foodStoreService.searchFoodStores(pageInfo, searchDto);

        return ResponseEntity.ok(foodStores);
    }
    
    /**
     * 키워드/카테고리로 음식점 검색 (통합 검색)
     * GET /api/foodstores/search?keyword=한식&category=한식
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ResponseFoodStoreSearchDto>> searchFoodStores(PageInfo pageInfo, @RequestParam(value = "keyword", required = false) String keyword, @RequestParam(value = "category", required = false) String category) {
        RequestFoodStoreSearchDto searchDto = RequestFoodStoreSearchDto.builder()
                .keyword(keyword)
                .category(category)
                .build();
        Page<ResponseFoodStoreSearchDto> foodStores = foodStoreService.searchFoodStores(pageInfo, searchDto);
        return ResponseEntity.ok(foodStores);
    }
}
