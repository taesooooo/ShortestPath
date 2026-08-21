package com.shortestpath.shortestpath.service;

import java.io.IOException;
import java.util.List;

import com.shortestpath.shortestpath.core.pathengine.EmptyGeometryListException;
import com.shortestpath.shortestpath.dto.request.RequestBBox;
import com.shortestpath.shortestpath.dto.request.RequestFindPathDto;
import com.shortestpath.shortestpath.dto.response.ResponeseRouteSearchTraceDto;
import com.shortestpath.shortestpath.dto.response.ResponseFindPathDto;
import com.shortestpath.shortestpath.dto.response.ResponseRestaurantsDto;

public interface MapService {
	public List<ResponseFindPathDto> findPath(List<RequestFindPathDto> findPathDto) throws IOException, EmptyGeometryListException;
	public ResponeseRouteSearchTraceDto searchRouteTrack(RequestFindPathDto searchRouteDto) throws Exception;
	public ResponseRestaurantsDto findRestaurantsByBBox(RequestBBox requestBBox); 
}
