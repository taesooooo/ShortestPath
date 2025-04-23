package com.sortestpath.sortestpath.dto.request;

import com.sortestpath.sortestpath.common.validation.ValidCoordinate;
import com.sortestpath.sortestpath.core.pathengine.Coordinate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class RequestFindPathDto {
	@ValidCoordinate
	private Coordinate startCoordinate;
	@ValidCoordinate
	private Coordinate endCoordinate;
}
