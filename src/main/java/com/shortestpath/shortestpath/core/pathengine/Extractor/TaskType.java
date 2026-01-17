package com.shortestpath.shortestpath.core.pathengine.Extractor;

public enum TaskType {
    NODE_EDGE_CREATOR("노드/엣지 생성"),
    NODE_EDGE_SAVE("노드/엣지 저장"),
    NODE_CSV_WRITER("노드 CSV 저장");
    
    private final String description;
    
    TaskType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
