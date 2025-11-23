package com.shortestpath.shortestpath.core.pathengine.Extractor;

@FunctionalInterface
public interface ProgressStatus {
    void progress(int total, int current);
}
