package com.shortestpath.shortestpath.core.pathengine.Extractor.Task;

public class EndItem implements TaskItem {
    private int taskId;
    
    public EndItem(int taskId) {
        this.taskId = taskId;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    
}
