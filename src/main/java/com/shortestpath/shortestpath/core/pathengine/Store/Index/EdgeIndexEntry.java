package com.shortestpath.shortestpath.core.pathengine.Store.Index;

import com.shortestpath.shortestpath.core.pathengine.RoadLevel;

public class EdgeIndexEntry {
    private int nodeId;
    private LevelEdgeIndex level0EdgeIndex;
    private LevelEdgeIndex level1EdgeIndex;
    private LevelEdgeIndex level2EdgeIndex;

    public EdgeIndexEntry(int nodeId) {
        this.nodeId = nodeId;
        this.level0EdgeIndex = new LevelEdgeIndex(RoadLevel.L0, -1, 0);
        this.level1EdgeIndex = new LevelEdgeIndex(RoadLevel.L1, -1, 0);
        this.level2EdgeIndex = new LevelEdgeIndex(RoadLevel.L2, -1, 0);
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public LevelEdgeIndex getLevel0EdgeIndex() {
        return level0EdgeIndex;
    }

    public void setLevel0EdgeIndex(LevelEdgeIndex level0EdgeIndex) {
        this.level0EdgeIndex = level0EdgeIndex;
    }

    public LevelEdgeIndex getLevel1EdgeIndex() {
        return level1EdgeIndex;
    }

    public void setLevel1EdgeIndex(LevelEdgeIndex level1EdgeIndex) {
        this.level1EdgeIndex = level1EdgeIndex;
    }

    public LevelEdgeIndex getLevel2EdgeIndex() {
        return level2EdgeIndex;
    }

    public void setLevel2EdgeIndex(LevelEdgeIndex level2EdgeIndex) {
        this.level2EdgeIndex = level2EdgeIndex;
    }

    public boolean hasLevelEdgeIndex(RoadLevel roadLevel) {
        switch (roadLevel) {
            case L0:
                return level0EdgeIndex.getStartOffset() != -1;
            case L1:
                return level1EdgeIndex.getStartOffset() != -1;
            case L2:
                return level2EdgeIndex.getStartOffset() != -1;
            default:
                return false;
        }
    }
    public LevelEdgeIndex getLevelEdgeIndex(RoadLevel roadLevel) {
        switch (roadLevel) {
            case L0:
                return level0EdgeIndex;
            case L1:
                return level1EdgeIndex;
            case L2:
                return level2EdgeIndex;
            default:
                return null;
        }
    }

}
