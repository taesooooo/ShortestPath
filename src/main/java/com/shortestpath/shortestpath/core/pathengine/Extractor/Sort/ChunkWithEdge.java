package com.shortestpath.shortestpath.core.pathengine.Extractor.Sort;

import com.shortestpath.shortestpath.core.pathengine.Edge;

/**
 * 우선순위 큐에 저장될 청크-엣지 쌍
 * 
 * K-way 병합에서 여러 청크 파일로부터 읽은 엣지를
 * 우선순위 큐에 저장하기 위해 사용
 */
public class ChunkWithEdge {
    int chunkIndex;  // 어느 청크 파일에서 읽었는지
    Edge edge;       // 실제 엣지 데이터
    
    /**
     * 청크-엣지 쌍 생성
     * @param chunkIndex 청크 파일의 인덱스 (0, 1, 2, ...)
     * @param edge 엣지 데이터
     */
    public ChunkWithEdge(int chunkIndex, Edge edge) {
        this.chunkIndex = chunkIndex;
        this.edge = edge;
    }
    
    public int getChunkIndex() {
        return chunkIndex;
    }
    
    public Edge getEdge() {
        return edge;
    }
}
