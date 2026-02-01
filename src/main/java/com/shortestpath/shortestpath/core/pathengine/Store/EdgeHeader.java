package com.shortestpath.shortestpath.core.pathengine.Store;

public class EdgeHeader {
    private int edgeCount;
    private boolean sorted;

    public EdgeHeader(int edgeCount, boolean sorted) {
        this.edgeCount = edgeCount;
        this.sorted = sorted;
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    public void setEdgeCount(int edgeCount) {
        this.edgeCount = edgeCount;
    }

    public boolean isSorted() {
        return sorted;
    }

    public void setSorted(boolean sorted) {
        this.sorted = sorted;
    }
}
