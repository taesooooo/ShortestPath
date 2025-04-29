package com.sortestpath.sortestpath.service;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sortestpath.sortestpath.core.pathengine.Coordinate;
import com.sortestpath.sortestpath.core.pathengine.Engine;
import com.sortestpath.sortestpath.core.pathengine.Node;
import com.sortestpath.sortestpath.dto.request.RequestFindPathDto;
import com.sortestpath.sortestpath.dto.response.ResponseFindPathDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {
	private static final Logger log = LoggerFactory.getLogger(MapServiceImpl.class);

	private final Engine engine;

	@Override
	public ResponseFindPathDto findPath(RequestFindPathDto findPathDto) {
		Coordinate startCoordinate = new Coordinate(findPathDto.getStart());
		Coordinate endCoordinate = new Coordinate(findPathDto.getEnd());
		
		ArrayList<Node> resultPath = engine.shortestPathFind(startCoordinate, endCoordinate);

		return ResponseFindPathDto.builder()
				.start(startCoordinate)
				.end(endCoordinate)
				.routeList(resultPath.stream().map((node) -> node.getCoordinate()).collect(Collectors.toCollection(ArrayList::new)))
				.build();
	}

}
