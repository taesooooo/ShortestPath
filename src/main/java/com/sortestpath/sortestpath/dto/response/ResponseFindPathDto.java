package com.sortestpath.sortestpath.dto.response;

import java.util.ArrayList;

import com.sortestpath.sortestpath.core.pathengine.Coordinate;

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
