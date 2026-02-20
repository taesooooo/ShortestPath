package com.shortestpath.shortestpath.core.pathengine.Extractor;

/**
 * 도로 타입 필터링을 위한 유틸리티 클래스
 * NodeExtract와 EdgeExtract에서 공통으로 사용
 */
public class RoadTypeFilter {
    
    // 허용되는 도로 타입들
    private static final String[] ALLOWED_ROAD_TYPES = {
        "motorway", "motorway_link", "primary", "primary_link", "residential",
        "secondary", "secondary_link", "service", "tertiary_link", "tertiary", "trunk", "trunk_link"
    };
    
    /**
     * 허용되는 도로 타입인지 확인
     * @param roadType 확인할 도로 타입
     * @return 허용되는 도로 타입이면 true, 아니면 false
     */
    public static boolean isAllowedRoadType(String roadType) {
        if (roadType == null) {
            return false;
        }
        
        for (String allowedType : ALLOWED_ROAD_TYPES) {
            if (allowedType.equals(roadType)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 허용되는 도로 타입 목록 반환 (읽기 전용)
     * @return 허용되는 도로 타입 배열의 복사본
     */
    public static String[] getAllowedRoadTypes() {
        return ALLOWED_ROAD_TYPES.clone();
    }
}