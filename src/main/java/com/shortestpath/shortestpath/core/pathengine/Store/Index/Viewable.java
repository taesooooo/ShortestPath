package com.shortestpath.shortestpath.core.pathengine.Store.Index;

import com.shortestpath.shortestpath.core.pathengine.RoadLevel;

public interface Viewable {
    // public int viewNodeId();
    public RoadLevel viewRoadLevel(int nodeId, RoadLevel roadLevel);
    public long viewStartOffset(int nodeId, RoadLevel roadLevel);
    public int viewEdgeCount(int nodeId, RoadLevel roadLevel);
}
