package com.shortestpath.shortestpath.core.pathengine.Store.Index;

public class EdgeIndexHedaer {
    private int entryCount;
    private boolean taskCompleted;

    public EdgeIndexHedaer(int entryCount) {
        this(entryCount, false);
    }

    public EdgeIndexHedaer(int entryCount, boolean taskCompleted) {
        this.entryCount = entryCount;
        this.taskCompleted = taskCompleted;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(int entryCount) {
        this.entryCount = entryCount;
    }

    public boolean isTaskCompleted() {
        return taskCompleted;
    }

    public void setTaskCompleted(boolean taskCompleted) {
        this.taskCompleted = taskCompleted;
    }
}
