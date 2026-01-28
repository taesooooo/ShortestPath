package com.shortestpath.shortestpath.provider;

import java.io.IOException;
import java.util.List;

import org.locationtech.jts.geom.Envelope;
import org.springframework.stereotype.Component;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Store.DataPersistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DataPersistence 구현 - JpaNodeProvider를 활용한 DB 기반 조회 및 저장
 * 
 * HybridDataStore에 설정되면:
 * 1. 조회: getNodeIndex(coordinate) - DB에서 좌표로 노드 오프셋 조회
 * 2. 저장: saveNodeIndex() - 추출 단계 후 노드 인덱스를 DB에 저장
 * 
 * HybridDataStore에 설정되지 않으면 인메모리 모드 (Reader 사용)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaDataPersistence implements DataPersistence {

    private final JpaNodeProvider nodeProvider;

    @Override
    public void saveNodeIndex(List<IndexInfo> indexList) throws IOException {
        nodeProvider.insertNodeIndex(indexList);
    }

    @Override
    public int getNodeIndex(Coordinate coordinate) {
        log.debug("DB에서 노드 오프셋 조회 - coordinate: {}", coordinate);
        return nodeProvider.getNodeIndex(coordinate);
    }

    @Override
    public Coordinate getNearestNode(Envelope envelope, Coordinate coordinate) {
        return nodeProvider.getNearestNode(envelope, coordinate);
    }

    @Override
    public List<Integer> findNearestNodeId(Envelope envelope, Coordinate coordinate) {
        return nodeProvider.findNearestNodeId(envelope, coordinate);
    }
}
