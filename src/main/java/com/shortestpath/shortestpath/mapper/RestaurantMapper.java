package com.shortestpath.shortestpath.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.shortestpath.shortestpath.common.PageInfo;
import com.shortestpath.shortestpath.dto.request.RequestRestaurantSearchDto;
import com.shortestpath.shortestpath.dto.response.ResponseRestaurantSearchDto;
import com.shortestpath.shortestpath.dto.response.ResponseRestaurantDto;

@Mapper
public interface RestaurantMapper {
    // SELECT 쿼리는 RestaurantMapper.xml 파일에서 정의
    
    int countAll();
    
    ResponseRestaurantDto selectById(int id);
    
    List<ResponseRestaurantSearchDto> selectAll(@Param("pageInfo") PageInfo pageInfo);

    /**
     * 키워드/카테고리/BBox 통합 검색
     */
    List<ResponseRestaurantSearchDto> search(@Param("pageInfo") PageInfo pageInfo, @Param("searchDto") RequestRestaurantSearchDto searchDto);
    
    /**
     * 검색 결과 총 개수
     */
    int countBySearch(@Param("searchDto") RequestRestaurantSearchDto searchDto);
}