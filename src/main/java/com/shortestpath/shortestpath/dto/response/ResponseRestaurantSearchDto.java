package com.shortestpath.shortestpath.dto.response;

import org.locationtech.jts.geom.Point;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;

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
public class ResponseRestaurantSearchDto {
    private Integer id;
    private String salsSttsNm;
    private String bplcNm;
    private String bzstatSeNm;
    private String roadNmAddr;
    private String telno;
    private Double x;
    private Double y;
    private String hpg;
    private Coordinate geometry;
}
