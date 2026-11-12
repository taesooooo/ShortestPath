package com.shortestpath.shortestpath.core.unit.Util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

class GeometryUtilTest {

    @Test
    @DisplayName("양수 위경도를 하나의 long으로 패킹하고 복원한다")
    void packsAndUnpacksPositiveCoordinate() {
        Coordinate source = new Coordinate(37.5665123, 126.9780456);

        Coordinate decoded = GeometryUtil.longToPathCoordinate(GeometryUtil.coordinateToLong(source));

        assertThat(decoded.getLongitude()).isCloseTo(source.getLongitude(), within(0.0000001));
        assertThat(decoded.getLatitude()).isCloseTo(source.getLatitude(), within(0.0000001));
    }

    @Test
    @DisplayName("음수 위경도의 부호를 유지하여 패킹하고 복원한다")
    void packsAndUnpacksNegativeCoordinate() {
        Coordinate source = new Coordinate(-33.8651432, -151.2099001);

        long packed = GeometryUtil.coordinateToLong(source);

        assertThat(GeometryUtil.longitudeFromLong(packed)).isCloseTo(source.getLongitude(), within(0.0000001));
        assertThat(GeometryUtil.latitudeFromLong(packed)).isCloseTo(source.getLatitude(), within(0.0000001));
    }
}
