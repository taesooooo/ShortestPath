package com.shortestpath.shortestpath.core.pathengine;

public class SearchState {
    private final int nodeId;
    private final long edgeOffset;
    private final double gCost;
    private final double fCost;

    public SearchState(int nodeId, long edgeOffset, double gCost, double fCost) {
        this.nodeId = nodeId;
        this.edgeOffset = edgeOffset;
        this.gCost = gCost;
        this.fCost = fCost;
    }

    public int getNodeId() {
        return nodeId;
    }

    public long getEdgeOffset() {
        return edgeOffset;
    }

    public double getgCost() {
        return gCost;
    }

    public double getfCost() {
        return fCost;
    }
}
