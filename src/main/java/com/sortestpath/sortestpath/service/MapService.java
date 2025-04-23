package com.sortestpath.sortestpath.service;

import com.sortestpath.sortestpath.dto.request.RequestFindPathDto;
import com.sortestpath.sortestpath.dto.response.ResponseFindPathDto;

public interface MapService {
	public ResponseFindPathDto findPath(RequestFindPathDto findPathDto);
}
