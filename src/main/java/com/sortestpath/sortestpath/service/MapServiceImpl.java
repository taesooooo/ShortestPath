package com.sortestpath.sortestpath.service;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
		ArrayList<Node> resultPath = engine.shortestPathFind(findPathDto.getStartCoordinate(), findPathDto.getEndCoordinate());
		log.info(resultPath == null ? "탐색 실패" : "탐색 성공");
		
		return ResponseFindPathDto.builder()
				.startCoordinate(findPathDto.getStartCoordinate())
				.endCoordinate(findPathDto.getEndCoordinate())
				.paths(resultPath.stream().map((node) -> node.getCoordinate()).collect(Collectors.toCollection(ArrayList::new)))
				.build();
	}

}
