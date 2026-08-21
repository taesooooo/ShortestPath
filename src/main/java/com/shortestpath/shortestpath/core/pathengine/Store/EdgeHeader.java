package com.shortestpath.shortestpath.core.pathengine.Store;

public class EdgeHeader {
    private int edgeCount;
    private boolean sorted;
    private boolean taskCompleted;

    public EdgeHeader(int edgeCount, boolean sorted) {
        this(edgeCount, sorted, false);
    }

    public EdgeHeader(int edgeCount, boolean sorted, boolean taskCompleted) {
        this.edgeCount = edgeCount;
        this.sorted = sorted;
        this.taskCompleted = taskCompleted;
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

    public boolean isTaskCompleted() {
        return taskCompleted;
    }

    public void setTaskCompleted(boolean taskCompleted) {
        this.taskCompleted = taskCompleted;
    }
}
