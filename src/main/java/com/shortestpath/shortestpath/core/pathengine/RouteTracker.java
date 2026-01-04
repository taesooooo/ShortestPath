package com.shortestpath.shortestpath.core.pathengine;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class RouteTracker {
    private LinkedHashSet<Coordinate> routeCoordinates;

    public RouteTracker() {
        this.routeCoordinates = new LinkedHashSet<>();
    }

    public RouteTracker(LinkedHashSet<Coordinate> routeCoordinates) {
        this.routeCoordinates = routeCoordinates;
    }

    public void addCoordinate(Coordinate coordinate) {
        if (routeCoordinates == null) {
            routeCoordinates = new LinkedHashSet<>();
        }
        routeCoordinates.add(coordinate);
    }

    public LinkedHashSet<Coordinate> getRouteCoordinates() {
        return routeCoordinates;
    }
}
