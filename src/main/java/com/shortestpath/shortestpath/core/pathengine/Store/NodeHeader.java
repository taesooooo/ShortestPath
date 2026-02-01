package com.shortestpath.shortestpath.core.pathengine.Store;

public class NodeHeader {
    private int nodeCount;
    private boolean indexed;

    public NodeHeader(int nodeCount, boolean indexed) {
        this.nodeCount = nodeCount;
        this.indexed = indexed;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }
}
