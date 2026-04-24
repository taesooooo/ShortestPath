package com.shortestpath.shortestpath.service;

import com.shortestpath.shortestpath.common.Page;
import com.shortestpath.shortestpath.common.PageInfo;
import com.shortestpath.shortestpath.dto.request.RequestFoodStoreSearchDto;
import com.shortestpath.shortestpath.dto.response.ResponseFoodStoreDto;
import com.shortestpath.shortestpath.dto.response.ResponseFoodStoreSearchDto;
import com.shortestpath.shortestpath.exception.ItemEmptyException;
import com.shortestpath.shortestpath.mapper.FoodStoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodStoreServiceImpl implements FoodStoreService {
    
    private final FoodStoreMapper foodStoreMapper;
    
    /**
     * 모든 음식점 조회
     */
    @Override
    public Page<ResponseFoodStoreSearchDto> getAllFoodStores(PageInfo pageInfo) {
        List<ResponseFoodStoreSearchDto> foodStores = foodStoreMapper.selectAll(pageInfo);
        long totalElements = foodStoreMapper.countAll();

        Page<ResponseFoodStoreSearchDto> page = new Page<ResponseFoodStoreSearchDto>(totalElements, pageInfo, foodStores);

        return page;
    }
    
    /**
     * ID로 특정 음식점 조회
     */
    @Override
    public ResponseFoodStoreDto getFoodStoreById(Long id) {
        ResponseFoodStoreDto foodStore = foodStoreMapper.selectById(id);
        if(foodStore == null) {
            throw new ItemEmptyException("음식점이 존재하지 않습니다. ID: " + id);
        }
        return foodStore;
    }
    
    /**
     * 키워드/카테고리/Bbox로 음식점 검색 (통합 검색)
     */
    @Override
    public Page<ResponseFoodStoreSearchDto> searchFoodStores(PageInfo pageInfo, RequestFoodStoreSearchDto searchDto) {
        List<ResponseFoodStoreSearchDto> foodStores = foodStoreMapper.search(pageInfo, searchDto);
        if(foodStores.isEmpty()) {
            throw new ItemEmptyException("음식점들이 존재하지 않습니다.");
        }

        long totalElements = foodStoreMapper.countBySearch(searchDto);
        
        Page<ResponseFoodStoreSearchDto> page = new Page<ResponseFoodStoreSearchDto>(totalElements, pageInfo, foodStores);
        return page;
    }
}
