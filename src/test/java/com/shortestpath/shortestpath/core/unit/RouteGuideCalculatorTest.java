package com.shortestpath.shortestpath.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.RouteGuideCalculator;
import com.shortestpath.shortestpath.core.pathengine.RouteStep;
import com.shortestpath.shortestpath.core.pathengine.TurnDirection;

class RouteGuideCalculatorTest {

    @Test
    @DisplayName("방위각 차이로 직진을 계산한다")
    void calculateStraightDirection() {
        TurnDirection direction = RouteGuideCalculator.calculateTurnDirection(
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(2, 0));

        assertThat(direction).isEqualTo(TurnDirection.STRAIGHT);
    }

    @Test
    @DisplayName("방위각 차이로 우회전을 계산한다")
    void calculateRightDirection() {
        TurnDirection direction = RouteGuideCalculator.calculateTurnDirection(
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(1, 1));

        assertThat(direction).isEqualTo(TurnDirection.RIGHT);
    }

    @Test
    @DisplayName("방위각 차이로 좌회전을 계산한다")
    void calculateLeftDirection() {
        TurnDirection direction = RouteGuideCalculator.calculateTurnDirection(
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(1, -1));

        assertThat(direction).isEqualTo(TurnDirection.LEFT);
    }

    @Test
    @DisplayName("방위각 차이로 유턴을 계산한다")
    void calculateUTurnDirection() {
        TurnDirection direction = RouteGuideCalculator.calculateTurnDirection(
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(0, 0));

        assertThat(direction).isEqualTo(TurnDirection.U_TURN);
    }

    @Test
    @DisplayName("경로 노드에 시작과 종료 안내를 포함한다")
    void createRouteStepsWithStartAndEnd() {
        ArrayList<Node> path = new ArrayList<Node>();
        path.add(new Node(1, new Coordinate(0, 0)));
        path.add(new Node(2, new Coordinate(1, 0)));
        path.add(new Node(3, new Coordinate(1, 1)));

        ArrayList<RouteStep> routeSteps = RouteGuideCalculator.createRouteSteps(path);

        assertThat(routeSteps)
                .extracting(RouteStep::getTurnDirection)
                .containsExactly(TurnDirection.START, TurnDirection.RIGHT, TurnDirection.END);
    }
}
