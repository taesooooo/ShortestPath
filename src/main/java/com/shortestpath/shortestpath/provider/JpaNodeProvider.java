package com.shortestpath.shortestpath.provider;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Component;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;
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
    public void insertNodeIndex(List<IndexInfo> indexList) {
        NodeIndexInsertRepository.insertNodeIndex(indexList);
    }

    @Override
    public int getNodeIndex(Coordinate coordinate) {
        NodeIndex nodeIndex = nodeIndexRepository.findByCoordinate(coordinate).orElseThrow(() -> new NodeIndexNotFoundException("노드 인덱스가 존재하지 않습니다."));
        
        return nodeIndex.getOffset();
    }

    @Override
    public Coordinate getNearestNode(Envelope envelope, Coordinate coordinate) {
        String bbox = GeometryUtil.toWkt(envelope);

        List<NodeIndex> nodeIndexList = nodeIndexRepository.findNearestNode(bbox, coordinate);
        if(nodeIndexList.isEmpty()) {
            throw new NodeIndexNotFoundException("가장 가까운 노드 인덱스를 찾을 수 없습니다.");
        }
        
        return nodeIndexList.get(0).getCoordinate();
    }

   @Override
	public List<Integer> findNearestNodeId(Envelope envelope, Coordinate coordinate) {
        String bbox = GeometryUtil.toWkt(envelope);
		return nodeIndexRepository.findNearestNode(bbox, coordinate).stream().map(data -> data.getOffset()).toList();
	}
}