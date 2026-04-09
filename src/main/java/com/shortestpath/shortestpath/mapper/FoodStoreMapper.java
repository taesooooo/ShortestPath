package com.shortestpath.shortestpath.mapper;

import com.shortestpath.shortestpath.common.PageInfo;
import com.shortestpath.shortestpath.dto.request.RequestFoodStoreSearchDto;
import com.shortestpath.shortestpath.dto.response.ResponseFoodStoreDto;
import com.shortestpath.shortestpath.dto.response.ResponseFoodStoreSearchDto;
import com.shortestpath.shortestpath.entity.FoodStore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface FoodStoreMapper {
    // SELECT 쿼리는 FoodStoreMapper.xml 파일에서 정의
    
    long countAll();
    
    ResponseFoodStoreDto selectById(Long id);
    
    List<ResponseFoodStoreSearchDto> selectAll(@Param("pageInfo") PageInfo pageInfo);
    
    long countByCategory(@Param("category") String category);
    
    List<ResponseFoodStoreSearchDto> searchByKeyword(String keyword);
    
    /**
     * 키워드/카테고리 통합 검색
     */
    List<ResponseFoodStoreSearchDto> searchByKeywordAndCategory(@Param("pageInfo") PageInfo pageInfo, @Param("searchDto") RequestFoodStoreSearchDto searchDto);
    
    /**
     * 검색 결과 총 개수
     */
    long countBySearch(RequestFoodStoreSearchDto searchDto);
}