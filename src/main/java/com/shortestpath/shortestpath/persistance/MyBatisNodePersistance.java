package com.shortestpath.shortestpath.persistance;

import java.util.List;

import org.locationtech.jts.geom.Envelope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Store.NodeDataPersistence;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;
import com.shortestpath.shortestpath.core.pathengine.model.NodeIndex;
import com.shortestpath.shortestpath.exception.NodeIndexNotFoundException;
import com.shortestpath.shortestpath.mapper.NodeIndexMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyBatisNodePersistance implements NodeDataPersistence{

    private final NodeIndexMapper nodeIndexMapper;
    
    @Override
    @Transactional
    public void saveNodeIndex(List<IndexInfo> indexList) {
        int batchSize = 10000;
        
        for (int i = 0; i < indexList.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, indexList.size());
            List<IndexInfo> batch = indexList.subList(i, endIndex);
            
            try {
                long startTime = System.currentTimeMillis();
                log.info("노드 인덱스 배치 저장 시작: {} ~ {} (총 {}개)", i, endIndex - 1, indexList.size());
                
                // MyBatis 매퍼에 직접 IndexInfo 리스트 전달
                nodeIndexMapper.insertNodeIndexBatch(batch);
                
                log.info("노드 인덱스 배치 저장 완료: {} ~ {} (총 {}개, {}ms)",
                        i, endIndex - 1, indexList.size(), System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("노드 인덱스 배치 저장 실패: {} ~ {}", i, endIndex - 1, e);
                throw new RuntimeException("노드 인덱스 저장 중 오류 발생", e);
            }
        }
    }

    @Override
    public int getNodeIndex(Coordinate coordinate) {
        NodeIndex nodeIndex = nodeIndexMapper.findByCoordinate(coordinate).orElseThrow(() -> new NodeIndexNotFoundException("노드 인덱스가 존재하지 않습니다."));
        
        return nodeIndex.getOffset();
    }

    @Override
    public Coordinate getNearestNode(Envelope envelope, Coordinate coordinate) {
        String bbox = GeometryUtil.toWkt(envelope);

        List<NodeIndex> nodeIndexList = nodeIndexMapper.findNearestNode(bbox, coordinate);
        if(nodeIndexList.isEmpty()) {
            throw new NodeIndexNotFoundException("가장 가까운 노드 인덱스를 찾을 수 없습니다.");
        }
        
        return nodeIndexList.get(0).getCoordinate();
    }

   @Override
	public List<Integer> findNearestNodeId(Envelope envelope, Coordinate coordinate) {
        String bbox = GeometryUtil.toWkt(envelope);
        return nodeIndexMapper.findNearestNode(bbox, coordinate).stream().map(data -> data.getId()).toList();
	}
}