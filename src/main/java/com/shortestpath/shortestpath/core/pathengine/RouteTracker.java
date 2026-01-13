package com.shortestpath.shortestpath.core.pathengine;

import java.util.ArrayList;
import java.util.List;

public class RouteTracker {
    private List<TraceRoute> trackRoutes = new ArrayList<TraceRoute>();

    public RouteTracker() {

    }

    public RouteTracker(List<TraceRoute> trackRoutes) {
        this.trackRoutes = trackRoutes;
    }

    public List<TraceRoute> getTrackRoutes() {
        return trackRoutes;
    }

    public void addTraceRoute(TraceRoute traceRoute) {
        this.trackRoutes.add(traceRoute);
    }
}
