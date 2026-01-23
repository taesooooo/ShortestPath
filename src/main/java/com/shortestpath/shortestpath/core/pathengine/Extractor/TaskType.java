package com.shortestpath.shortestpath.core.pathengine.Extractor;

public enum TaskType {
    NODE_EXTRACT("노드 추출"),
    EDGE_EXTRACT("엣지 추출"),
    NODE_CSV_WRITER("노드 CSV 저장");
    
    private final String description;
    
    TaskType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
