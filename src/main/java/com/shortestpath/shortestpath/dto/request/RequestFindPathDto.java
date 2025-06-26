package com.shortestpath.shortestpath.dto.request;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class RequestFindPathDto {
	private Coordinate start;
	private Coordinate end;
}
