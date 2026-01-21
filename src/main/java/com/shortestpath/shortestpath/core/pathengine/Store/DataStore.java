package com.shortestpath.shortestpath.core.pathengine.Store;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;

public interface DataStore {
    int saveNode(Node node) throws IOException;
    int saveNode(Node node, long offset) throws IOException;
    int saveEdge(Edge edge) throws IOException;
    int saveEdge(Edge edge, long offset) throws IOException;
    int overwriteNode(Node node, long offset) throws IOException;
    int overwriteEdge(Edge edge, long offset) throws IOException;
    Node readNode(long offset) throws IOException;
    Edge readEdge(long offset) throws IOException;
    void saveNodeIndex(List<IndexInfo> indexList) throws IOException;
    // HashMap<Coordinate, Integer> loadNodeOffsetIndex() throws Exception;
    int getNodeOffset(Coordinate coordinate);
    boolean hasExtractedData();

    void allocateNodeFileSpace(long size) throws IOException;
    void allocateEdgeFileSpace(long size) throws IOException;
}
