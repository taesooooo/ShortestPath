package com.shortestpath.shortestpath.core.pathengine.Store;

import java.io.IOException;
import java.util.List;

import org.locationtech.jts.geom.Envelope;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;

/**
 * 데이터베이스 영속성 및 노드 조회 통합 인터페이스
 * 
 * 역할:
 * 1. 저장: 추출 단계에서 노드 인덱스를 DB에 저장
 * 2. 조회: 경로탐색 단계에서 DB에서 노드 정보를 조회
 */
public interface DataPersistence {
    /**
     * 노드 인덱스 정보를 데이터베이스에 저장
     * @param indexList 노드 인덱스 정보 목록
     * @throws IOException IO 오류 발생 시
     */
    void saveNodeIndex(List<IndexInfo> indexList) throws IOException;
    
    /**
     * 좌표로 노드 오프셋 조회
     * @param coordinate 좌표
     * @return 노드 오프셋
     */
    int getNodeIndex(Coordinate coordinate);
    
    /**
     * 범위 내에서 가장 가까운 노드 조회
     * @param envelope 검색 범위
     * @param coordinate 기준 좌표
     * @return 가장 가까운 노드의 좌표
     */
    Coordinate getNearestNode(Envelope envelope, Coordinate coordinate);
    
    /**
     * 범위 내에서 가장 가까운 노드 ID 목록 조회
     * @param envelope 검색 범위
     * @param coordinate 기준 좌표
     * @return 가장 가까운 노드 ID 목록
     */
    List<Integer> findNearestNodeId(Envelope envelope, Coordinate coordinate);
}
