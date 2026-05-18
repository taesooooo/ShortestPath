package com.shortestpath.shortestpath.core.pathengine.Store.Reader;

import java.nio.MappedByteBuffer;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;

public class NodeViewer {
    private final MappedByteBuffer mappedByteBuffer;

    public NodeViewer(MappedByteBuffer mappedByteBuffer) {
        if (mappedByteBuffer == null) {
            throw new NullPointerException("Node 메모리 매핑 버퍼는 null일 수 없습니다.");
        }
        this.mappedByteBuffer = mappedByteBuffer;
    }

    public int readNodeId(int nodeId) throws IndexOutOfBoundsException {
        int offset = calculateOffset(nodeId);
        return mappedByteBuffer.getInt(offset);
    }
    
    public int readStartEdgeOffset(int nodeId) throws IndexOutOfBoundsException {
        int offset = calculateOffset(nodeId) + DataStructureSizes.NODE_ID_SIZE;
        return mappedByteBuffer.getInt(offset);
    }
    
    public double readXCoordinate(int nodeId) throws IndexOutOfBoundsException {
        int offset = calculateOffset(nodeId) + DataStructureSizes.NODE_ID_SIZE + DataStructureSizes.NODE_START_EDGE_OFFSET_SIZE;
        return mappedByteBuffer.getDouble(offset);
    }
    
    public double readYCoordinate(int nodeId) throws IndexOutOfBoundsException {
        int offset = calculateOffset(nodeId) + DataStructureSizes.NODE_ID_SIZE + DataStructureSizes.NODE_START_EDGE_OFFSET_SIZE + (DataStructureSizes.NODE_COORDINATE_SIZE / 2);
        return mappedByteBuffer.getDouble(offset);
    }

    private int calculateOffset(int nodeId) throws IndexOutOfBoundsException {
        long offset = DataStructureSizes.calculateNodeOffset(nodeId);
        if (nodeId < 0 || offset > Integer.MAX_VALUE || offset + DataStructureSizes.NODE_SIZE > mappedByteBuffer.capacity()) {
            throw new IndexOutOfBoundsException(
                    "유효하지 않은 노드 ID입니다: nodeId=" + nodeId + ", capacity=" + mappedByteBuffer.capacity());
        }
        return (int) offset;
    }
}
 
