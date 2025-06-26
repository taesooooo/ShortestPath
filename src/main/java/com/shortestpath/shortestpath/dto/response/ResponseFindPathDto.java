package com.shortestpath.shortestpath.dto.response;

import java.util.ArrayList;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ResponseFindPathDto {
	private Coordinate start;
	private Coordinate end;
	private ArrayList<Coordinate> routeList;
}
