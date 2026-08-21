package com.shortestpath.shortestpath.core.pathengine;

public class RouteStep {
    private Node node;
    private Coordinate coordinate;
    private TurnDirection turnDirection;

    public RouteStep() {
    }

    public RouteStep(Node node, TurnDirection turnDirection) {
        this.node = node;
        this.coordinate = node != null ? node.getCoordinate() : null;
        this.turnDirection = turnDirection;
    }

    public RouteStep(Node node, Coordinate coordinate, TurnDirection turnDirection) {
        this.node = node;
        this.coordinate = coordinate;
        this.turnDirection = turnDirection;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public TurnDirection getTurnDirection() {
        return turnDirection;
    }

    public void setTurnDirection(TurnDirection turnDirection) {
        this.turnDirection = turnDirection;
    }
}
