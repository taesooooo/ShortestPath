package com.sortestpath.sortestpath.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sortestpath.sortestpath.core.pathengine.Coordinate;
import com.sortestpath.sortestpath.core.pathengine.Engine;
import com.sortestpath.sortestpath.core.pathengine.Node;
import com.sortestpath.sortestpath.dto.request.RequestFindPathDto;
import com.sortestpath.sortestpath.dto.response.ResponseFindPathDto;
import com.sortestpath.sortestpath.dto.response.RouteDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {
	private static final Logger log = LoggerFactory.getLogger(MapServiceImpl.class);

	private final Engine engine;

	@Override
	public ResponseFindPathDto findPath(List<RouteDto> coordinateList) {
		
		ArrayList<RouteDto> resultList = new ArrayList<RouteDto>();
		
		for(int i=0; i<coordinateList.size(); i++) {
			RouteDto route = coordinateList.get(i);
			Coordinate startCoordinate = route.getStart();
			Coordinate endCoordinate = route.getEnd();
			
			List<Node> pathList = engine.shortestPathFind(startCoordinate, endCoordinate);
			
			resultList.add(RouteDto.builder()
					.start(startCoordinate)
					.end(endCoordinate)
					.routeList(pathList.stream().map((node) -> node.getCoordinate()).collect(Collectors.toCollection(ArrayList::new)))
					.build());
		}
		
		return ResponseFindPathDto.builder().routeDto(resultList).build();
	}

}
