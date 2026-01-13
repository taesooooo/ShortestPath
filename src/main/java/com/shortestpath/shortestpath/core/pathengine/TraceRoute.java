package com.shortestpath.shortestpath.core.pathengine;

import java.util.ArrayList;
import java.util.List;

public class TraceRoute {
    private Coordinate parentCoordinate;
    private List<Coordinate> visitedCoordinates = new ArrayList<Coordinate>();

    public TraceRoute() {

    }

    public TraceRoute(Coordinate parentCoordinate) {
        this.parentCoordinate = parentCoordinate;
    }

    public TraceRoute(Coordinate parentCoordinate, List<Coordinate> childCoordinates) {
        this.parentCoordinate = parentCoordinate;
        this.visitedCoordinates = childCoordinates;
    }

    public void setParentCoordinate(Coordinate parentCoordinate) {
        this.parentCoordinate = parentCoordinate;
    }

    public Coordinate getParentCoordinate() {
        return parentCoordinate;
    }

    public List<Coordinate> getVisitedCoordinates() {
        return visitedCoordinates;
    }

    public void addChild(Coordinate coordinate) {
        visitedCoordinates.add(coordinate);
    }

}
