package com.shortestpath.shortestpath.dto.response;

import java.util.ArrayList;
import java.util.List;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.TraceRoute;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResponeseRouteSearchTraceDto {
    private Coordinate start;
    private Coordinate end;
    private ArrayList<ResponseRouteStepDto> routeSteps;
    private List<TraceRoute> traceRoutes; 
    private double searchTime;
}
