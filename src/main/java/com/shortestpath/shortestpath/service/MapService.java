package com.shortestpath.shortestpath.service;

import java.io.IOException;
import java.util.List;

import com.shortestpath.shortestpath.dto.request.RequestFindPathDto;
import com.shortestpath.shortestpath.dto.response.ResponeseRouteSearchTraceDto;
import com.shortestpath.shortestpath.dto.response.ResponseFindPathDto;

public interface MapService {
	public List<ResponseFindPathDto> findPath(List<RequestFindPathDto> findPathDto) throws IOException;
	public ResponeseRouteSearchTraceDto searchRouteTrack(RequestFindPathDto searchRouteDto) throws Exception;
}
