package com.shortestpath.shortestpath.core.pathengine;

import java.util.ArrayList;

public class RouteSearchResult {
    private ArrayList<Node> routeNode;
    private RouteTracker routeTracker;
    private double searchTime;
    
    public RouteSearchResult(ArrayList<Node> routeNode, double searchTime) {
        this.routeNode = routeNode;
        this.searchTime = searchTime;
    }

    public RouteSearchResult(ArrayList<Node> routeNode, RouteTracker routeTracker, double searchTime) {
        this.routeNode = routeNode;
        this.routeTracker = routeTracker;
        this.searchTime = searchTime;
    }

    public ArrayList<Node> getRouteNode() {
        return routeNode;
    }

    public void setRouteNode(ArrayList<Node> routeNode) {
        this.routeNode = routeNode;
    }

    public RouteTracker getRouteTracker() {
        return routeTracker;
    }

    public void setRouteTracker(RouteTracker routeTracker) {
        this.routeTracker = routeTracker;
    }

    public double getSearchTime() {
        return searchTime;
    }

    public void setSearchTime(double searchTime) {
        this.searchTime = searchTime;
    }    
}
