package com.shortestpath.shortestpath.core.pathengine;

public class DataStructureSizes {
    // EdgeHeader edgeCount(int, 4바이트), sorted(boolean, 1바이트)
    public static final int HEADER_SIZE = 5;
    // EdgeIndexHeader entryCount(int, 4바이트)
    public static final int EDGE_INDEX_HEADER_SIZE = 4;
    // Node id(int, 4바이트), startEdgeOffset(int, 4바이트), x(double, 8바이트), y(double, 8바이트)
    public static final int NODE_SIZE = 24;
    // Edge id(int, 4바이트), from(int, 4바이트), to(int, 4바이트), distance(double, 8바이트), nextEdgeOffset(int, 4바이트), speed(int, 4바이트), loadLevel(String, 2바이트)
    public static final int EDGE_SIZE = 30;
    // LevelEdgeIndex level(String, 2바이트), startOffset(long, 8바이트), edgeCount(int, 4바이트)
    public static final int LEVEL_EDGE_INDEX_SIZE = 14;
    // EdgeIndexEntry nodeId(int, 4바이트), Level0EdgeIndex(14바이트), Level1EdgeIndex(14바이트), Level2EdgeIndex(14바이트)
    public static final int EDGE_INDEX_SIZE = 4 + (3 * LEVEL_EDGE_INDEX_SIZE);

    public static final int NODE_ENTRY_SIZE = HEADER_SIZE + NODE_SIZE;
    public static final int EDGE_ENTRY_SIZE = HEADER_SIZE + EDGE_SIZE;
    public static final int EDGE_INDEX_ENTRY_SIZE = EDGE_INDEX_HEADER_SIZE + EDGE_INDEX_SIZE;

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
     * Node는 헤더가 없으므로 헤더 크기 미포함
     * @param nodeId 노드 ID
     * @return 파일 오프셋
     */
    public static long calculateNodeOffset(int nodeId) {
        return (long) nodeId * NODE_SIZE;
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
