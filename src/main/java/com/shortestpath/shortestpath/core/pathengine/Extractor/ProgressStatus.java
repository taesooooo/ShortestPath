package com.shortestpath.shortestpath.core.pathengine.Extractor;

@FunctionalInterface
public interface ProgressStatus {
    void progress(TaskType taskType, int total, int current);
}
