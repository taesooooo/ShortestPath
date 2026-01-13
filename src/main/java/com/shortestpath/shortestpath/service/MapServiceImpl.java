package com.shortestpath.shortestpath.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.EmptyGeometryListException;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.RouteSearchResult;
import com.shortestpath.shortestpath.dto.request.RequestFindPathDto;
import com.shortestpath.shortestpath.dto.response.ResponeseRouteSearchTraceDto;
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
			
			try {
				RouteSearchResult searchResult = engine.shortestPathFind(startCoordinate, endCoordinate, false);
			
				List<Node> pathList = searchResult.getRouteNode();
				
				resultList.add(ResponseFindPathDto.builder()
					.start(startCoordinate)
					.end(endCoordinate)
					.routeList(getNodeCoordinate(pathList))
					.build());
			}
			catch(EmptyGeometryListException e) {
				log.info(e.getMessage());

				resultList.add(ResponseFindPathDto.builder()
					.start(startCoordinate)
					.end(endCoordinate)
					.routeList(getNodeCoordinate(null))
					.build());
			}
		}
		
		return resultList;
	}
	
	
	@Override
	public ResponeseRouteSearchTraceDto searchRouteTrack(RequestFindPathDto searchRouteDto) throws EmptyGeometryListException, IOException {
		Coordinate startCoordinate = searchRouteDto.getStart();
		Coordinate endCoordinate = searchRouteDto.getEnd();

		RouteSearchResult searchResult = engine.shortestPathFind(startCoordinate, endCoordinate, true);

		return ResponeseRouteSearchTraceDto.builder()
				.start(startCoordinate)
				.end(endCoordinate)
				.routeCoordinates(getNodeCoordinate(searchResult.getRouteNode()))
				.traceRoutes(searchResult.getRouteTracker().getTrackRoutes())
				.build();
	}


	private ArrayList<Coordinate> getNodeCoordinate(List<Node> pathList) {
		if(pathList == null || pathList.isEmpty()) {
			return new ArrayList<Coordinate>();
		}

		return pathList.stream().map(node -> node.getCoordinate())
			.collect(Collectors.toCollection(ArrayList::new));
	}

}
