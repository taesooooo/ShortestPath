package com.shortestpath.shortestpath.core.pathengine.Store.Reader;

import java.nio.MappedByteBuffer;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;

public class EdgeViewer {
    private final MappedByteBuffer mappedByteBuffer;
    private static final int ROAD_LEVEL_OFFSET = DataStructureSizes.EDGE_ID_SIZE
            + DataStructureSizes.EDGE_FROM_SIZE
            + DataStructureSizes.EDGE_TO_SIZE
            + DataStructureSizes.EDGE_DISTANCE_SIZE
            + DataStructureSizes.EDGE_NEXT_EDGE_OFFSET_SIZE
            + DataStructureSizes.EDGE_SPEED_SIZE;

    public EdgeViewer(MappedByteBuffer mappedByteBuffer) {
        if (mappedByteBuffer == null) {
            throw new NullPointerException("Edge 메모리 매핑 버퍼는 null일 수 없습니다.");
        }
        this.mappedByteBuffer = mappedByteBuffer;
    }

    public int readEdgeId(long offset) throws IndexOutOfBoundsException {
        return mappedByteBuffer.getInt(validateOffset(offset));
    }

    public int readEdgeFrom(long offset) throws IndexOutOfBoundsException {
        return mappedByteBuffer.getInt(validateOffset(offset) + DataStructureSizes.EDGE_ID_SIZE);
    }

    public int readEdgeTo(long offset) throws IndexOutOfBoundsException {
        return mappedByteBuffer.getInt(validateOffset(offset) + DataStructureSizes.EDGE_ID_SIZE + DataStructureSizes.EDGE_FROM_SIZE);
    }

    public double readEdgeDistance(long offset) throws IndexOutOfBoundsException {
        return mappedByteBuffer.getDouble(validateOffset(offset) + DataStructureSizes.EDGE_ID_SIZE + DataStructureSizes.EDGE_FROM_SIZE + DataStructureSizes.EDGE_TO_SIZE);
    }

    public int readEdgeNextEdgeOffset(long offset) throws IndexOutOfBoundsException {
        return mappedByteBuffer.getInt(validateOffset(offset) + DataStructureSizes.EDGE_ID_SIZE + DataStructureSizes.EDGE_FROM_SIZE + DataStructureSizes.EDGE_TO_SIZE + DataStructureSizes.EDGE_DISTANCE_SIZE);
    }

    public int readEdgeSpeed(long offset) throws IndexOutOfBoundsException {
        return mappedByteBuffer.getInt(validateOffset(offset) + DataStructureSizes.EDGE_ID_SIZE + DataStructureSizes.EDGE_FROM_SIZE + DataStructureSizes.EDGE_TO_SIZE + DataStructureSizes.EDGE_DISTANCE_SIZE + DataStructureSizes.EDGE_NEXT_EDGE_OFFSET_SIZE);
    }
    
    public RoadLevel readEdgeRoadLevel(long offset) throws IndexOutOfBoundsException {
        byte b2 = mappedByteBuffer.get(validateOffset(offset) + ROAD_LEVEL_OFFSET + 1);

        return RoadLevel.valueOf(b2);
    }

    private int validateOffset(long offset) throws IndexOutOfBoundsException {
        if (offset < 0 || offset > Integer.MAX_VALUE || offset + DataStructureSizes.EDGE_SIZE > mappedByteBuffer.capacity()) {
            throw new IndexOutOfBoundsException(
                    "유효하지 않은 엣지 오프셋입니다: offset=" + offset + ", capacity=" + mappedByteBuffer.capacity());
        }
        return (int) offset;
    }
}
