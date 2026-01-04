package com.shortestpath.shortestpath.dto.response;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResponeseRouteSearchTrackDto {
    private Coordinate start;
    private Coordinate end;
    private ArrayList<Coordinate> routeCoordinates;
    private LinkedHashSet<Coordinate> visitedCoordinates;
    private double searchTime;
}
