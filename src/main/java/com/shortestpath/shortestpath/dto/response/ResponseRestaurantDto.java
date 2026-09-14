package com.shortestpath.shortestpath.dto.response;

import java.time.LocalDateTime;

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
public class ResponseRestaurantDto {
    private Integer id;
    private String opnAtmyGrpCd;
    private String mngNo;
    private String salsSttsNm;
    private String lctnZip;
    private String roadNmZip;
    private String bplcNm;
    private String bzstatSeNm;
    private String datUpdtSe;
    private String mltUtztnBsnsspYn;
    private LocalDateTime datUpdtPnt;
    private String roadNmAddr;
    private String dtlSalsSttsNm;
    private String dtlSalsSttsCd;
    private String salsSttsCd;
    private String bizplcSurrndSeNm;
    private String snttnBzstatNm;
    private String telno;
    private Double x;
    private Double y;
    private String lotnoAddr;
    private String hpg;
    private LocalDateTime lastMdfcnPnt;
    private Coordinate geometry;
}
