package com.shortestpath.shortestpath.core.pathengine.Store;

import java.io.IOException;
import java.util.List;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndex;

public interface DataStore {
    int getTotalNodes() throws IOException;
    int getTotalEdges() throws IOException;
    int getTotalReverseEdges() throws IOException;
    int saveNode(Node node) throws IOException;
    int saveNode(Node node, long offset) throws IOException;
    int saveEdge(Edge edge) throws IOException;
    int saveEdge(Edge edge, long offset) throws IOException;
    int overwriteNode(Node node, long offset) throws IOException;
    int overwriteEdge(Edge edge, long offset) throws IOException;
    Node readNode(long offset) throws IOException;
    Edge readEdge(long offset) throws IOException;
    Edge readReverseEdge(long offset) throws IOException;
    void saveNodeIndex(List<IndexInfo> indexList) throws IOException;
    int getNodeOffset(Coordinate coordinate);
    boolean hasExtractedData();
    NodeHeader readNodeHeader() throws IOException;
    EdgeHeader readEdgeHeader() throws IOException;
    EdgeHeader readReverseEdgeHeader() throws IOException;
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

    boolean isEdgeIndexTaskCompleted();

    /**
     * Reverse EdgeIndex 설정
     * @param reverseEdgeIndex Reverse Edge 인덱스 구현체
     */
    void setReverseEdgeIndex(EdgeIndex reverseEdgeIndex);

    /**
     * Reverse EdgeIndex 조회
     * @return Reverse Edge 인덱스 구현체
     */
    EdgeIndex getReverseEdgeIndex();

    boolean isReverseEdgeIndexTaskCompleted();

    int viewNodeId(int nodeId);
    int viewNodeStartEdgeOffset(int nodeId);
    double viewNodeXCoordinate(int nodeId);
    double viewNodeYCoordinate(int nodeId);

    int viewEdgeId(long offset);
    int viewEdgeFrom(long offset);
    int viewEdgeTo(long offset);
    double viewEdgeDistance(long offset);
    int viewEdgeNextEdgeOffset(long offset);
    int viewEdgeSpeed(long offset);
    RoadLevel viewEdgeRoadLevel(long offset);

    int viewReverseEdgeId(long offset);
    int viewReverseEdgeFrom(long offset);
    int viewReverseEdgeTo(long offset);
    double viewReverseEdgeDistance(long offset);
    int viewReverseEdgeNextEdgeOffset(long offset);
    int viewReverseEdgeSpeed(long offset);
    RoadLevel viewReverseEdgeRoadLevel(long offset);
}
