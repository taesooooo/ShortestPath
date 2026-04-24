package com.shortestpath.shortestpath.service;

import com.shortestpath.shortestpath.common.Page;
import com.shortestpath.shortestpath.common.PageInfo;
import com.shortestpath.shortestpath.dto.request.RequestFoodStoreSearchDto;
import com.shortestpath.shortestpath.dto.response.ResponseFoodStoreDto;
import com.shortestpath.shortestpath.dto.response.ResponseFoodStoreSearchDto;
import java.util.List;

public interface FoodStoreService {
    
    /**
     * 모든 음식점 조회
     */
    Page<ResponseFoodStoreSearchDto> getAllFoodStores(PageInfo pageInfo);
    
    /**
     * ID로 특정 음식점 조회
     */
    ResponseFoodStoreDto getFoodStoreById(Long id);
    
    /**
     * 키워드/카테고리/Bbox로 음식점 검색 (통합 검색)
     */
    Page<ResponseFoodStoreSearchDto> searchFoodStores(PageInfo pageInfo, RequestFoodStoreSearchDto searchDto);
}
