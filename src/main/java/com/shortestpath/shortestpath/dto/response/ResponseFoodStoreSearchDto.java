package com.shortestpath.shortestpath.dto.response;

import org.locationtech.jts.geom.Point;

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
public class ResponseFoodStoreSearchDto {
    private Long id;
    private String trdStateNm;
    private String bplcNm;
    private String uptaeGbnNm;
    private String rdnWhlAddr;
    private String telNo;
    private Double x;
    private Double y;
    private String homepage;
    private Integer buildingId;
    private Point centerCord;
}
