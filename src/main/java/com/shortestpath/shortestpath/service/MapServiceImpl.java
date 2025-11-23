package com.shortestpath.shortestpath.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.dto.request.RequestFindPathDto;
import com.shortestpath.shortestpath.dto.response.ResponseFindPathDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {
	private static final Logger log = LoggerFactory.getLogger(MapServiceImpl.class);

	private final Engine engine;

	@Override
	public List<ResponseFindPathDto> findPath(List<RequestFindPathDto> coordinateList) throws IOException {
		
		ArrayList<ResponseFindPathDto> resultList = new ArrayList<ResponseFindPathDto>();
		
		for(int i=0; i<coordinateList.size(); i++) {
			RequestFindPathDto route = coordinateList.get(i);
			Coordinate startCoordinate = route.getStart();
			Coordinate endCoordinate = route.getEnd();
			
			List<Node> pathList = engine.shortestPathFind(startCoordinate, endCoordinate);
			
			resultList.add(ResponseFindPathDto.builder()
					.start(startCoordinate)
					.end(endCoordinate)
					.routeList(getNodeCoordinate(pathList))
					.build());
		}
		
		return resultList;
	}
	
	private ArrayList<Coordinate> getNodeCoordinate(List<Node> pathList) {
		// LinkedHashSet<Coordinate> list = new LinkedHashSet<Coordinate>();
		
		// int nodeId = 0;
		// org.locationtech.jts.geom.Coordinate previousCoordinate = null;
		
		// for(int i=0; i<pathList.size(); i++) {
		// 	Node node = pathList.get(i);
		// 	Map<Integer, Edge> edges = node.getEdge();
			
		// 	if(i > 0 && edges.containsKey(nodeId) || i == pathList.size()-1 && edges.containsKey(nodeId)) {
		// 		Edge edge = edges.get(nodeId);
		// 		org.locationtech.jts.geom.Coordinate[] coordinates = edge.getGeometry().getCoordinates();
				
		// 		// 배열의 시작과 끝을 확인하여 정확한 순서로 엣지 좌표들을 넣기 위함
		// 		if(coordinates[0].equals(previousCoordinate)) {
		// 			for(org.locationtech.jts.geom.Coordinate coordinate : coordinates) {
		// 				list.add(new Coordinate(coordinate.getY(), coordinate.getX()));
		// 			}					
		// 		}
		// 		else {
		// 			// 이전 좌표가 배열의 끝에 있는 경우 마지막 순서부터 리스트에 넣음
		// 			for(int j=coordinates.length-1; j>=0; j--) {
		// 				list.add(new Coordinate(coordinates[j].getY(), coordinates[j].getX()));
		// 			}
					
		// 		}
				
		// 	}
		// 	else {
		// 		list.add(node.getCoordinate());
		// 	}
			
		// 	nodeId = node.getId();
		// 	previousCoordinate = new org.locationtech.jts.geom.Coordinate(node.getCoordinate().getLongitude(), node.getCoordinate().getLatitude());
		// }
		
		// return new ArrayList<Coordinate>(list);

		return null;
	}

}
