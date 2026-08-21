package com.shortestpath.shortestpath.core.pathengine;

/**
 * 도로 등급 열거형
 * L0: 고속도로 (motorway, trunk)
 * L1: 주요 도로 (primary, secondary, tertiary)
 * L2: 일반 도로 (residential, unclassified, service)
 */
public enum RoadLevel {
    L0("L0"),
    L1("L1"),
    L2("L2");
    
    private final String value;
    
    RoadLevel(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * 문자열을 RoadLevel로 변환
     */
    public static RoadLevel fromString(String value) {
        if (value == null) {
            return L2;  // 기본값
        }
        
        for (RoadLevel level : RoadLevel.values()) {
            if (level.value.equalsIgnoreCase(value)) {
                return level;
            }
        }
        return L2;  // 매칭되는 값이 없으면 기본값
    }

    public static RoadLevel valueOf(byte value) {
        // byte b1 = value[0];
        byte b2 = value;
        if (b2 == 49) return RoadLevel.L1;
        if (b2 == 50) return RoadLevel.L2;
        if (b2 == 48) return RoadLevel.L0;

        return RoadLevel.L2;
    }

    public static RoadLevel valueOf(int ordinal) {
        if(RoadLevel.L0.ordinal() == ordinal) return RoadLevel.L0;
        if(RoadLevel.L1.ordinal() == ordinal) return RoadLevel.L1;
        if(RoadLevel.L2.ordinal() == ordinal) return RoadLevel.L2;

        return RoadLevel.L2;
    }
}
