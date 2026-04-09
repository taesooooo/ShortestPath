package com.shortestpath.shortestpath.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodStore {
    private Long id;
    private Integer opnStdNmCd;           // 개방표준번호코드
    private String mgtNo;                  // 관리번호
    private String trdStateNm;             // 영업상태명
    private LocalDate dcbDt;               // 폐업일자
    private String locPostNo;              // 지번우편번호
    private String rdnPostNo;              // 도로명우편번호
    private String bplcNm;                 // 사업장명
    private String uptaeGbnNm;             // 업태구분명
    private String dtUpdGbn;               // 데이터업데이트구분
    private Boolean multiUseYn;            // 다중이용여부
    private LocalDate dtUpdTm;             // 데이터업데이트시간
    private String rdnWhlAddr;             // 도로명전체주소
    private String dtlTrdStateNm;          // 상세영업상태명
    private String dtlTrdStateCd;          // 상세영업상태코드
    private String trdStateCd;             // 영업상태코드
    private String telNo;                  // 전화번호
    private Double x;                      // X좌표
    private Double y;                      // Y좌표
    private String siteWhlAddr;            // 지번전체주소
    private String homepage;               // 홈페이지
    private LocalDate lastModTm;           // 최종수정시간
    private String geometry;               // 기하정보
    private String sidoNm;                 // 시도명
    private String sigunguNm;              // 시군구명
    private Double lotNmMain;              // 지번본번
    private Double lotNmSub;               // 지번부번
    private String emdNm;                  // 읍면동명
    private Integer buildingId;            // 건물ID (FK: building_info.ogc_fid)
}
