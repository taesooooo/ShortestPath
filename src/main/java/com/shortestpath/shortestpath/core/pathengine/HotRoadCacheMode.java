package com.shortestpath.shortestpath.core.pathengine;

public enum HotRoadCacheMode {
    INDEX_ONLY(false),
    EDGE(true);

    private final boolean edgeDataCacheEnabled;

    HotRoadCacheMode(boolean edgeDataCacheEnabled) {
        this.edgeDataCacheEnabled = edgeDataCacheEnabled;
    }

    public boolean isEdgeDataCacheEnabled() {
        return edgeDataCacheEnabled;
    }

    public static HotRoadCacheMode fromProperty(String value) {
        if(value == null || value.isBlank()) {
            return INDEX_ONLY;
        }

        String normalized = value.trim()
                .replace("-", "_")
                .replace(" ", "_")
                .toUpperCase();

        if("EDGE".equals(normalized)
                || "EDGE_DATA".equals(normalized)
                || "INDEX_AND_EDGE".equals(normalized)
                || "INDEX_EDGE".equals(normalized)) {
            return EDGE;
        }

        if("INDEX_ONLY".equals(normalized)
                || "INDEX".equals(normalized)) {
            return INDEX_ONLY;
        }

        throw new IllegalArgumentException(
                "지원하지 않는 HotRoadCache mode입니다: " + value + " (사용 가능: index-only, edge)");
    }
}
