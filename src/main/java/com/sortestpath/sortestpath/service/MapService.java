package com.sortestpath.sortestpath.service;

import java.util.List;

import com.sortestpath.sortestpath.core.pathengine.Coordinate;
import com.sortestpath.sortestpath.dto.response.ResponseFindPathDto;
import com.sortestpath.sortestpath.dto.response.RouteDto;

public interface MapService {
	public ResponseFindPathDto findPath(List<RouteDto> findPathDto);
}
