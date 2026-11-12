package com.shortestpath.shortestpath.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.shortestpath.shortestpath.common.Page;
import com.shortestpath.shortestpath.common.PageInfo;
import com.shortestpath.shortestpath.dto.request.RequestRestaurantSearchDto;
import com.shortestpath.shortestpath.dto.response.ResponseRestaurantSearchDto;
import com.shortestpath.shortestpath.dto.response.ResponseRestaurantDto;
import com.shortestpath.shortestpath.exception.ItemEmptyException;
import com.shortestpath.shortestpath.mapper.RestaurantMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {
    
    private final RestaurantMapper RestaurantMapper;
    
    /**
     * 모든 음식점 조회
     */
    @Override
    public Page<ResponseRestaurantSearchDto> getAllRestaurants(PageInfo pageInfo) {
        List<ResponseRestaurantSearchDto> Restaurants = RestaurantMapper.selectAll(pageInfo);
        long totalElements = RestaurantMapper.countAll();

        Page<ResponseRestaurantSearchDto> page = new Page<ResponseRestaurantSearchDto>(totalElements, pageInfo, Restaurants);

        return page;
    }
    
    /**
     * ID로 특정 음식점 조회
     */
    @Override
    public ResponseRestaurantDto getRestaurantById(int id) {
        ResponseRestaurantDto Restaurant = RestaurantMapper.selectById(id);
        if(Restaurant == null) {
            throw new ItemEmptyException("음식점이 존재하지 않습니다. ID: " + id);
        }
        return Restaurant;
    }
    
    /**
     * 키워드/카테고리/Bbox로 음식점 검색 (통합 검색)
     */
    @Override
    public Page<ResponseRestaurantSearchDto> searchRestaurants(PageInfo pageInfo, RequestRestaurantSearchDto searchDto) {
        List<ResponseRestaurantSearchDto> Restaurants = RestaurantMapper.search(pageInfo, searchDto);
        if(Restaurants.isEmpty()) {
            throw new ItemEmptyException("음식점들이 존재하지 않습니다.");
        }

        long totalElements = RestaurantMapper.countBySearch(searchDto);
        
        Page<ResponseRestaurantSearchDto> page = new Page<ResponseRestaurantSearchDto>(totalElements, pageInfo, Restaurants);
        return page;
    }
}
