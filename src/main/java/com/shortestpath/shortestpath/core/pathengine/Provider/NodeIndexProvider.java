package com.shortestpath.shortestpath.core.pathengine.Provider;

import java.util.HashMap;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;

public interface NodeIndexProvider {
    public void insertNodeIndex(HashMap<Coordinate, IndexInfo> indexMap);
    public int getNodeIndex(Coordinate coordinate);
}