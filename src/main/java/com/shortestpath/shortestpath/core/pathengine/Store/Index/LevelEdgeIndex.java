package com.shortestpath.shortestpath.core.pathengine.Store.Index;

import com.shortestpath.shortestpath.core.pathengine.RoadLevel;

public class LevelEdgeIndex {
    private RoadLevel level;
    private long startOffset;
    private int edgeCount;

    public LevelEdgeIndex(RoadLevel level, long startOffset, int edgeCount) {
        this.level = level;
        this.startOffset = startOffset;
        this.edgeCount = edgeCount;
    }

    public RoadLevel getLevel() {
        return level;
    }

    public void setLevel(RoadLevel level) {
        this.level = level;
    }

    public long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(long startOffset) {
        this.startOffset = startOffset;
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    public void setEdgeCount(int edgeCount) {
        this.edgeCount = edgeCount;
    }

}
