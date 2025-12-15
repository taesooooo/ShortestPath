package com.shortestpath.shortestpath.provider;

import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeIndexProvider;
import com.shortestpath.shortestpath.entity.NodeIndex;
import com.shortestpath.shortestpath.exception.NodeIndexNotFoundException;
import com.shortestpath.shortestpath.repository.NodeIndexInsertRepository;
import com.shortestpath.shortestpath.repository.NodeIndexRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaNodeIndexProvider implements NodeIndexProvider{

    private final NodeIndexInsertRepository NodeIndexInsertRepository;
    private final NodeIndexRepository nodeIndexRepository;
    
    @Override
    public void insertNodeIndex(HashMap<Coordinate, IndexInfo> indexMap) {
        NodeIndexInsertRepository.insertNodeIndex(indexMap);
    }

    @Override
    public int getNodeIndex(Coordinate coordinate) {
        NodeIndex nodeIndex = nodeIndexRepository.findByCoordinate(coordinate).orElseThrow(() -> new NodeIndexNotFoundException("노드 인덱스가 존재하지 않습니다."));
        
        return nodeIndex.getOffset();
    }
}