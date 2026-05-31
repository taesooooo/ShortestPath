package com.shortestpath.shortestpath.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.EmptyGeometryListException;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.RouteSearchResult;
import com.shortestpath.shortestpath.core.pathengine.RouteStep;
import com.shortestpath.shortestpath.core.pathengine.TraceRoute;
import com.shortestpath.shortestpath.dto.request.RequestBBox;
import com.shortestpath.shortestpath.dto.request.RequestFindPathDto;
import com.shortestpath.shortestpath.dto.response.ResponeseRouteSearchTraceDto;
import com.shortestpath.shortestpath.dto.response.ResponseFindPathDto;
import com.shortestpath.shortestpath.dto.response.ResponseRestaurantsDto;
import com.shortestpath.shortestpath.dto.response.ResponseRouteStepDto;
import com.shortestpath.shortestpath.entity.Restaurants;
import com.shortestpath.shortestpath.repository.RestaurantsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {
	private final Engine engine;
	private final RestaurantsRepository restaurantsRepository;

	@Override
	public List<ResponseFindPathDto> findPath(List<RequestFindPathDto> coordinateList) throws IOException, EmptyGeometryListException {
		
		ArrayList<ResponseFindPathDto> resultList = new ArrayList<ResponseFindPathDto>();
		
		for(int i=0; i<coordinateList.size(); i++) {
			RequestFindPathDto route = coordinateList.get(i);
			Coordinate startCoordinate = route.getStart();
			Coordinate endCoordinate = route.getEnd();
			
			RouteSearchResult searchResult = engine.shortestPathFind(startCoordinate, endCoordinate, false);
			resultList.add(ResponseFindPathDto.builder()
				.start(startCoordinate)
				.end(endCoordinate)
				.routeSteps(getRouteStepDto(searchResult.getRouteSteps()))
				.searchTime(searchResult.getSearchTime())
				.build());
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
				.routeSteps(getRouteStepDto(searchResult.getRouteSteps()))
				.traceRoutes(getTraceRoutes(searchResult))
				.searchTime(searchResult.getSearchTime())
				.build();
	}

	private ArrayList<ResponseRouteStepDto> getRouteStepDto(List<RouteStep> routeSteps) {
		if(routeSteps == null || routeSteps.isEmpty()) {
			return new ArrayList<ResponseRouteStepDto>();
		}

		return routeSteps.stream()
				.map(routeStep -> ResponseRouteStepDto.builder()
						.coordinate(routeStep.getCoordinate())
						.turnDirection(routeStep.getTurnDirection())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private List<TraceRoute> getTraceRoutes(RouteSearchResult searchResult) {
		if(searchResult.getRouteTracker() == null) {
			return new ArrayList<TraceRoute>();
		}

		return searchResult.getRouteTracker().getTrackRoutes();
	}


	@Override
	public ResponseRestaurantsDto findRestaurantsByBBox(RequestBBox bbox) {
		// List<Restaurants> restaurantsList = restaurantsRepository.findRestaurantsByBBox(bbox);
		return null;
	}

	

}
