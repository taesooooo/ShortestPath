package com.sortestpath.sortestpath.service;

import java.util.List;

import com.sortestpath.sortestpath.dto.request.RequestFindPathDto;
import com.sortestpath.sortestpath.dto.response.ResponseFindPathDto;

public interface MapService {
	public List<ResponseFindPathDto> findPath(List<RequestFindPathDto> findPathDto);
}
