package com.shortestpath.shortestpath.core.pathengine.Store;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndex;

public interface DataStore {
    int getTotalNodes() throws IOException;
    int getTotalEdges() throws IOException;
    int saveNode(Node node) throws IOException;
    int saveNode(Node node, long offset) throws IOException;
    int saveEdge(Edge edge) throws IOException;
    int saveEdge(Edge edge, long offset) throws IOException;
    int overwriteNode(Node node, long offset) throws IOException;
    int overwriteEdge(Edge edge, long offset) throws IOException;
    Node readNode(long offset) throws IOException;
    Edge readEdge(long offset) throws IOException;
    void saveNodeIndex(List<IndexInfo> indexList) throws IOException;
    int getNodeOffset(Coordinate coordinate);
    boolean hasExtractedData();
    void close() throws IOException;
    void allocateNodeFileSpace(long size) throws IOException;
    void allocateEdgeFileSpace(long size) throws IOException;
    void truncateNodeFile(long actualSize) throws IOException;
    void truncateEdgeFile(long actualSize) throws IOException;
    void writeNodeHeader(NodeHeader header) throws IOException;
    void writeEdgeHeader(EdgeHeader header) throws IOException;
    
    // EdgeIndex 관리 메서드
    /**
     * EdgeIndex 설정
     * @param edgeIndex Edge 인덱스 구현체
     */
    void setEdgeIndex(EdgeIndex edgeIndex);
    
    /**
     * EdgeIndex 조회
     * @return Edge 인덱스 구현체
     */
    EdgeIndex getEdgeIndex();
}
