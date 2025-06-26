package com.shortestpath.shortestpath.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
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
	public List<ResponseFindPathDto> findPath(List<RequestFindPathDto> coordinateList) {
		
		ArrayList<ResponseFindPathDto> resultList = new ArrayList<ResponseFindPathDto>();
		
		for(int i=0; i<coordinateList.size(); i++) {
			RequestFindPathDto route = coordinateList.get(i);
			Coordinate startCoordinate = route.getStart();
			Coordinate endCoordinate = route.getEnd();
			
			List<Node> pathList = engine.shortestPathFind(startCoordinate, endCoordinate);
			
			resultList.add(ResponseFindPathDto.builder()
					.start(startCoordinate)
					.end(endCoordinate)
					.routeList(pathList.stream().map((node) -> node.getCoordinate()).collect(Collectors.toCollection(ArrayList::new)))
					.build());
		}
		
		return resultList;
	}

}
