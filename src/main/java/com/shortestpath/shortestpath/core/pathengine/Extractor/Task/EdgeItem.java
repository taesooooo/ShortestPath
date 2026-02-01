package com.shortestpath.shortestpath.core.pathengine.Extractor.Task;

import com.shortestpath.shortestpath.core.pathengine.Edge;

/**
 * 도로 등급 정보를 포함한 엣지 아이템
 */
public class EdgeItem implements TaskItem {
    private Edge edge;

    public EdgeItem(Edge edge) {
        this.edge = edge;
    }
    
    public Edge getEdge() {
        return edge;
    }
}
