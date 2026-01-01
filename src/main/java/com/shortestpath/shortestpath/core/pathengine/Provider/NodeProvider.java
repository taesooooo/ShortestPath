package com.shortestpath.shortestpath.core.pathengine.Provider;

import java.util.HashMap;
import java.util.List;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;

public interface NodeProvider {
    public void insertNodeIndex(HashMap<Coordinate, IndexInfo> indexMap);
    public int getNodeIndex(Coordinate coordinate);
    public Coordinate getNearestNode(Coordinate coordinate);
    public List<Integer> findNearestNodeId(Coordinate coordinate);
}