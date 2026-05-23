package com.shortestpath.shortestpath.dto.response;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.TurnDirection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ResponseRouteStepDto {
    private Coordinate coordinate;
    private TurnDirection turnDirection;
}
