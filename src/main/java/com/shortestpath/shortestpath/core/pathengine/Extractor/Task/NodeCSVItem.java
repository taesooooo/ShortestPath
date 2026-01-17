package com.shortestpath.shortestpath.core.pathengine.Extractor.Task;

public class NodeCSVItem implements TaskItem {
    private int nodeId;
    private long coordinate;
    private long offset;
    
    public NodeCSVItem(int nodeId, long coordinate, long offset) {
        this.nodeId = nodeId;
        this.coordinate = coordinate;
        this.offset = offset;
    }
    
    public int getNodeId() {
        return nodeId;
    }
    
    public long getCoordinate() {
        return coordinate;
    }
    
    public long getOffset() {
        return offset;
    }
}
