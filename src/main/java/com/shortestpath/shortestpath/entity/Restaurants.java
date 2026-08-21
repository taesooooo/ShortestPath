package com.shortestpath.shortestpath.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Column;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
public class Restaurants {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 개방자치단체코드
    @Column(name = "opn_atmy_grp_cd")
    private Integer oppnAtmyGrpCd;
    
    // 관리번호
    @Column(name = "mng_no", length = 64, unique = true)
    private String mngNo;
    
    // 인허가일자
    @Column(name = "lcpmt_ymd")
    private LocalDate lcpmtYmd;
    
    // 영업상태명
    @Column(name = "sals_stts_nm", length = 50)
    private String salsSttsNm;
    
    // 폐업일자
    @Column(name = "clsbiz_ymd")
    private LocalDate clsbizYmd;
    
    // 소재지면적
    @Column(name = "lctn_area", precision = 8, scale = 2)
    private BigDecimal lctnArea;
    
    // 소재지우편번호
    @Column(name = "lctn_zip", length = 12)
    private String lctnZip;
    
    // 도로명우편번호
    @Column(name = "road_nm_zip", length = 12)
    private String roadNmZip;
    
    // 사업장명
    @Column(name = "bplc_nm", length = 200)
    private String bplcNm;
    
    // 업태구분명
    @Column(name = "bzstat_se_nm", length = 100)
    private String bzstatSeNm;
    
    // 데이터갱신구분
    @Column(name = "dat_updt_se", length = 1)
    private String datUpdtSe;
    
    // 건물소유구분명
    @Column(name = "bldg_psn_se_nm", length = 50)
    private String bldgPsnSeNm;
    
    // 공장사무직직원수
    @Column(name = "fctry_ofjb_emp_cnt")
    private Integer fctryOfjbEmpCnt;
    
    // 공장생산직직원수
    @Column(name = "fctry_prodwk_emp_cnt")
    private Integer fctryProdwkEmpCnt;
    
    // 공장판매직직원수
    @Column(name = "fctry_slspos_emp_cnt")
    private Integer fctrySlsposEmpCnt;
    
    // 급수시설구분명
    @Column(name = "wtrsppl_fclt_se_nm", length = 50)
    private String wtrspplFcltSeNm;
    
    // 남성종사자수
    @Column(name = "ml_prctr_cnt")
    private Integer mlPrctrCnt;
    
    // 다중이용업소여부
    @Column(name = "mlt_utztn_bsnssp_yn")
    private Boolean mltUtztmBsnssPyn;
    
    // 데이터갱신시점
    @Column(name = "dat_updt_pnt")
    private LocalDateTime datUpdtPnt;
    
    // 도로명주소
    @Column(name = "road_nm_addr", length = 500)
    private String roadNmAddr;
    
    // 등급구분명
    @Column(name = "grd_se_nm", length = 50)
    private String grdSeNm;
    
    // 보증액
    @Column(name = "grnamt", precision = 12, scale = 2)
    private BigDecimal grnamt;
    
    // 본사직원수
    @Column(name = "hdofc_emp_cnt")
    private Integer hdofcEmpCnt;
    
    // 상세영업상태명
    @Column(name = "dtl_sals_stts_nm", length = 100)
    private String dtlSalsSttsNm;
    
    // 상세영업상태코드
    @Column(name = "dtl_sals_stts_cd", length = 10)
    private String dtlSalsSttsCd;
    
    // 시설총규모
    @Column(name = "fclt_total_scl", length = 100)
    private String fcltTotalScl;
    
    // 여성종사자수
    @Column(name = "fml_prctr_cnt")
    private Integer fmlPrctrCnt;
    
    // 영업상태코드
    @Column(name = "sals_stts_cd", length = 10)
    private String salsSttsCd;
    
    // 영업장주변구분명
    @Column(name = "bizplc_surrnd_se_nm", length = 100)
    private String bizplcSurrndSeNm;
    
    // 월세액
    @Column(name = "mrnt_amount", precision = 12, scale = 2)
    private BigDecimal mrntAmount;
    
    // 위생업태명
    @Column(name = "snttn_bzstat_nm", length = 100)
    private String snttmBzstatNm;
    
    // 전통업소주된음식
    @Column(name = "trdtn_bsnssp_princ_fd", length = 100)
    private String trdtnBsnssPrincFd;
    
    // 전통업소지정번호
    @Column(name = "trdtn_bsnssp_dsgn_no", length = 50)
    private String trdtnBsnssDsgnNo;
    
    // 전화번호
    @Column(name = "telno", length = 30)
    private String telno;
    
    // 좌표정보(X)
    @Column(name = "crd_info_x", precision = 14, scale = 6)
    private BigDecimal crdInfoX;
    
    // 좌표정보(Y)
    @Column(name = "crd_info_y", precision = 14, scale = 6)
    private BigDecimal crdInfoY;
    
    // 지번주소
    @Column(name = "lotno_addr", length = 500)
    private String lotnAddress;
    
    // 홈페이지
    @Column(name = "hpg", length = 500)
    private String hpg;
    
    // 최종수정시점
    @Column(name = "last_mdfcn_pnt")
    private LocalDateTime lastMdfcnPnt;
}
