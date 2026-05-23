package com.shortestpath.shortestpath.core.pathengine;

import java.util.ArrayList;
import java.util.List;

public final class RouteGuideCalculator {
    private static final double STRAIGHT_THRESHOLD_DEGREES = 30.0;
    private static final double U_TURN_THRESHOLD_DEGREES = 150.0;

    private RouteGuideCalculator() {
    }

    public static ArrayList<RouteStep> createRouteSteps(List<Node> path) {
        ArrayList<RouteStep> routeSteps = new ArrayList<RouteStep>();
        if (path == null || path.isEmpty()) {
            return routeSteps;
        }

        if (path.size() == 1) {
            routeSteps.add(new RouteStep(path.get(0), TurnDirection.START));
            return routeSteps;
        }

        for (int i = 0; i < path.size(); i++) {
            Node node = path.get(i);
            TurnDirection turnDirection;

            if (i == 0) {
                turnDirection = TurnDirection.START;
            } else if (i == path.size() - 1) {
                turnDirection = TurnDirection.END;
            } else {
                turnDirection = calculateTurnDirection(
                        path.get(i - 1).getCoordinate(),
                        node.getCoordinate(),
                        path.get(i + 1).getCoordinate());
            }

            routeSteps.add(new RouteStep(node, turnDirection));
        }

        return routeSteps;
    }

    public static TurnDirection calculateTurnDirection(Coordinate previous, Coordinate current, Coordinate next) {
        double previousBearing = calculateBearing(previous, current);
        double nextBearing = calculateBearing(current, next);
        double diff = normalizeBearingDiff(nextBearing - previousBearing);
        double absDiff = Math.abs(diff);

        if (absDiff <= STRAIGHT_THRESHOLD_DEGREES) {
            return TurnDirection.STRAIGHT;
        }

        if (absDiff >= U_TURN_THRESHOLD_DEGREES) {
            return TurnDirection.U_TURN;
        }

        return diff > 0 ? TurnDirection.RIGHT : TurnDirection.LEFT;
    }

    public static double calculateBearing(Coordinate from, Coordinate to) {
        double fromLat = Math.toRadians(from.getLatitude());
        double toLat = Math.toRadians(to.getLatitude());
        double deltaLon = Math.toRadians(to.getLongitude() - from.getLongitude());

        double y = Math.sin(deltaLon) * Math.cos(toLat);
        double x = Math.cos(fromLat) * Math.sin(toLat)
                - Math.sin(fromLat) * Math.cos(toLat) * Math.cos(deltaLon);

        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0;
    }

    public static double normalizeBearingDiff(double diff) {
        while (diff <= -180.0) {
            diff += 360.0;
        }
        while (diff > 180.0) {
            diff -= 360.0;
        }

        return diff;
    }
}
