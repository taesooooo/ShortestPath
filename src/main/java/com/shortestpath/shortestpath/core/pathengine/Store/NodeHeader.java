package com.shortestpath.shortestpath.core.pathengine.Store;

public class NodeHeader {
    private int nodeCount;
    private boolean indexed;
    private boolean taskCompleted;

    public NodeHeader(int nodeCount, boolean indexed) {
        this(nodeCount, indexed, false);
    }

    public NodeHeader(int nodeCount, boolean indexed, boolean taskCompleted) {
        this.nodeCount = nodeCount;
        this.indexed = indexed;
        this.taskCompleted = taskCompleted;
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

    public boolean isTaskCompleted() {
        return taskCompleted;
    }

    public void setTaskCompleted(boolean taskCompleted) {
        this.taskCompleted = taskCompleted;
    }
}
