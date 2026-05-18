package com.shortestpath.shortestpath.core.pathengine;

public class DataStructureSizes {
    // Node/Edge header: count(int, 4바이트), status(boolean, 1바이트), taskCompleted(boolean, 1바이트)
    public static final int HEADER_SIZE = 6;
    // EdgeIndexHeader entryCount(int, 4바이트), taskCompleted(boolean, 1바이트)
    public static final int EDGE_INDEX_HEADER_SIZE = 5;
    // Node id(int, 4바이트), startEdgeOffset(int, 4바이트), x(double, 8바이트), y(double, 8바이트)
    public static final int NODE_SIZE = 24;
    public static final int NODE_ID_SIZE = 4;
    public static final int NODE_START_EDGE_OFFSET_SIZE = 4;
    public static final int NODE_COORDINATE_SIZE = 16; // x와 y 좌표를 합친 크기
    // Edge id(int, 4바이트), from(int, 4바이트), to(int, 4바이트), distance(double, 8바이트), nextEdgeOffset(int, 4바이트), speed(int, 4바이트), roadLevel(String, 2바이트)
    public static final int EDGE_SIZE = 30;
    public static final int EDGE_ID_SIZE = 4;
    public static final int EDGE_FROM_SIZE = 4;
    public static final int EDGE_TO_SIZE = 4;
    public static final int EDGE_DISTANCE_SIZE = 8;
    public static final int EDGE_NEXT_EDGE_OFFSET_SIZE = 4;
    public static final int EDGE_SPEED_SIZE = 4;
    public static final int EDGE_ROAD_LEVEL_SIZE = 2;
    // LevelEdgeIndex level(String, 2바이트), startOffset(long, 8바이트), edgeCount(int, 4바이트)
    public static final int LEVEL_EDGE_INDEX_SIZE = 14;
    public static final int LEVEL_EDGE_INDEX_LEVEL_SIZE = 2;
    public static final int LEVEL_EDGE_INDEX_START_OFFSET_SIZE = 8;
    public static final int LEVEL_EDGGE_INDEX_EDGE_COUNT_SIZE = 4;
    // EdgeIndexEntry nodeId(int, 4바이트), Level0EdgeIndex(14바이트), Level1EdgeIndex(14바이트), Level2EdgeIndex(14바이트)
    public static final int EDGE_INDEX_SIZE = NODE_ID_SIZE + (3 * LEVEL_EDGE_INDEX_SIZE);

    public static final int NODE_ENTRY_SIZE = HEADER_SIZE + NODE_SIZE;
    public static final int EDGE_ENTRY_SIZE = HEADER_SIZE + EDGE_SIZE;
    public static final int EDGE_INDEX_ENTRY_SIZE = EDGE_INDEX_SIZE;

    // 오프셋 계산 메서드들
    
    /**
     * Edge 인덱스 엔트리의 파일 오프셋 계산
     * 헤더 크기를 포함하여 정확한 위치 반환
     * @param nodeId 노드 ID
     * @return 파일 오프셋
     */
    public static long calculateEdgeIndexOffset(int nodeId) {
        return EDGE_INDEX_HEADER_SIZE + ((long) nodeId * EDGE_INDEX_ENTRY_SIZE);
    }
    
    /**
     * Node 엔트리의 파일 오프셋 계산
     * 헤더 크기를 포함하여 정확한 위치 반환
     * @param nodeId 노드 ID
     * @return 파일 오프셋
     */
    public static long calculateNodeOffset(int nodeId) {
        return HEADER_SIZE + ((long) nodeId * NODE_SIZE);
    }
    
    /**
     * Edge 엔트리의 파일 오프셋 계산
     * 헤더 크기를 포함하여 정확한 위치 반환
     * @param edgeId 엣지 ID
     * @return 파일 오프셋
     */
    public static long calculateEdgeOffset(int edgeId) {
        return HEADER_SIZE + ((long) edgeId * EDGE_SIZE);
    }

}
