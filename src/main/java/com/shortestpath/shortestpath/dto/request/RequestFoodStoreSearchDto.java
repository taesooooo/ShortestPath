package com.shortestpath.shortestpath.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestFoodStoreSearchDto {
    
    /**
     * 키워드 검색 (음식점명, 주소 등)
     */
    private String keyword;
    
    /**
     * 카테고리 검색 (음식 종류)
     */
    private String category;
}
