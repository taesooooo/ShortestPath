package com.shortestpath.shortestpath.service;

import com.shortestpath.shortestpath.common.Page;
import com.shortestpath.shortestpath.common.PageInfo;
import com.shortestpath.shortestpath.dto.request.RequestRestaurantSearchDto;
import com.shortestpath.shortestpath.dto.response.ResponseRestaurantSearchDto;
import com.shortestpath.shortestpath.dto.response.ResponseRestaurantDto;

public interface RestaurantService {
    
    /**
     * 모든 음식점 조회
     */
    Page<ResponseRestaurantSearchDto> getAllRestaurants(PageInfo pageInfo);
    
    /**
     * ID로 특정 음식점 조회
     */
    ResponseRestaurantDto getRestaurantById(int id);
    
    /**
     * 키워드/카테고리/Bbox로 음식점 검색 (통합 검색)
     */
    Page<ResponseRestaurantSearchDto> searchRestaurants(PageInfo pageInfo, RequestRestaurantSearchDto searchDto);
}
