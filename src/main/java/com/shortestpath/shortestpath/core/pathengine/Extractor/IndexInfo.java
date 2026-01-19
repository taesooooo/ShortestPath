package com.shortestpath.shortestpath.core.pathengine.Extractor;

public class IndexInfo {
    public int nodeId;
    public long coordinate;
    public int offset;

    public IndexInfo(int nodeId, long coordinate, int offset) {
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

    public int getOffset() {
        return offset;
    }

}
