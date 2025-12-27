package com.shortestpath.shortestpath.provider;

import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Component;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.entity.NodeIndex;
import com.shortestpath.shortestpath.exception.NodeIndexNotFoundException;
import com.shortestpath.shortestpath.repository.NodeIndexInsertRepository;
import com.shortestpath.shortestpath.repository.NodeIndexRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaNodeProvider implements NodeProvider{

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

    @Override
    public Coordinate getNearestNode(Coordinate coordinate, double distance) {
        List<NodeIndex> nodeIndexList = nodeIndexRepository.findNearestNode(coordinate, distance);
        if(nodeIndexList.isEmpty()) {
            throw new NodeIndexNotFoundException("가장 가까운 노드 인덱스를 찾을 수 없습니다.");
        }
        
        return nodeIndexList.get(0).getCoordinate();
    }

   @Override
	public List<Integer> findNearestNodeId(Coordinate coordinate, double distance) {
		return nodeIndexRepository.findNearestNode(coordinate, distance).stream().map(data -> data.getOffset()).toList();
	}
}