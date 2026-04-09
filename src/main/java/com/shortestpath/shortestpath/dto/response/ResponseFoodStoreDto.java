package com.shortestpath.shortestpath.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.locationtech.jts.geom.Geometry;

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
public class ResponseFoodStoreDto {
    /** 음식점 ID */
    private long id;
    /** 개방자치단체코드 */
    private int opnStdNmCd;
    /** 관리번호 */
    private String mgtNo;
    /** 영업상태명 */
    private String trdStateNm;
    /** 폐업일자 */
    private LocalDate dcbDt;
    /** 소재지우편번호 */
    private String locPostNo;
    /** 도로명우편번호 */
    private String rdnPostNo;
    /** 사업장명 */
    private String bplcNm;
    /** 업태구분명 */
    private String uptaeGbnNm;
    /** 데이터갱신구분 */
    private String dtUpdGbn;
    /** 다중이용업소여부 */
    private Boolean multiUseYn;
    /** 데이터갱신시점 */
    private LocalDate dtUpdTm;
    /** 도로명주소 */
    private String rdnWhlAddr;
    /** 상세영업상태명 */
    private String dtlTrdStateNm;
    /** 상세영업상태코드 */
    private String dtlTrdStateCd;
    /** 영업상태코드 */
    private String trdStateCd;
    /** 전화번호 */
    private String telNo;
    /** 좌표정보(X) */
    private Double x;
    /** 좌표정보(Y) */
    private Double y;
    /** 지번주소 */
    private String siteWhlAddr;
    /** 홈페이지 */
    private String homepage;
    /** 최종수정시점 */
    private LocalDate lastModetm;
    /** 공간정보(지리정보) */
    private Geometry geometry;
    /** 시도명 */
    private String sidoNm;
    /** 시군구명 */
    private String sigunguNm;
    /** 지번 메인 */
    private int lotNmMain;
    /** 지번 서브 */
    private int lotNmSub;
    /** 읍면동명 */
    private String emdNm;
    /** 건물ID */
    private int buildingId;
}
