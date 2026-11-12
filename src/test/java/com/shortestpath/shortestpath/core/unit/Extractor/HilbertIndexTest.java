package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import com.shortestpath.shortestpath.core.pathengine.Extractor.HilbertIndex;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

class HilbertIndexTest {

    @Test
    @DisplayName("1비트 Hilbert 곡선의 네 좌표를 순서대로 계산한다")
    void calculatesHilbertOrder() {
        assertThat(HilbertIndex.calculateHilbertValue(0, 0, 1)).isEqualTo(0);
        assertThat(HilbertIndex.calculateHilbertValue(0, 1, 1)).isEqualTo(1);
        assertThat(HilbertIndex.calculateHilbertValue(1, 1, 1)).isEqualTo(2);
        assertThat(HilbertIndex.calculateHilbertValue(1, 0, 1)).isEqualTo(3);
    }

    @Test
    @DisplayName("좌표 키를 Hilbert 순서의 nodeId로 변환한다")
    void mapsCoordinateKeyToHilbertNodeId() {
        long bottomLeft = key(0, 0);
        long topLeft = key(0, 1);
        long bottomRight = key(1, 0);
        long topRight = key(1, 1);
        long[] coordinateKeys = {bottomLeft, topLeft, bottomRight, topRight};
        Arrays.sort(coordinateKeys);

        HilbertIndex index = HilbertIndex.build(coordinateKeys, 1);

        assertThat(index.findNodeId(bottomLeft)).isEqualTo(0);
        assertThat(index.findNodeId(topLeft)).isEqualTo(1);
        assertThat(index.findNodeId(topRight)).isEqualTo(2);
        assertThat(index.findNodeId(bottomRight)).isEqualTo(3);
        assertThat(index.getCoordinateKey(2)).isEqualTo(topRight);
    }

    @Test
    @DisplayName("같은 Hilbert 셀에서는 기존 nodeId 순서를 유지한다")
    void usesOldNodeIdAsTieBreaker() {
        long first = key(0, 0);
        long second = key(0.1, 0.1);
        long last = key(10, 10);
        long[] coordinateKeys = {first, second, last};
        Arrays.sort(coordinateKeys);

        HilbertIndex index = HilbertIndex.build(coordinateKeys, 1);

        assertThat(index.getHilbertValue(index.findNodeId(first)))
                .isEqualTo(index.getHilbertValue(index.findNodeId(second)));
        assertThat(index.findNodeId(first)).isLessThan(index.findNodeId(second));
    }

    @Test
    @DisplayName("정규화는 최댓값을 마지막 격자에 고정하고 범위를 벗어난 값을 클램프한다")
    void normalizesAndClampsCoordinate() {
        assertThat(HilbertIndex.normalize(0, 0, 10, 10)).isZero();
        assertThat(HilbertIndex.normalize(10, 0, 10, 10)).isEqualTo(1023);
        assertThat(HilbertIndex.normalize(-1, 0, 10, 10)).isZero();
        assertThat(HilbertIndex.normalize(11, 0, 10, 10)).isEqualTo(1023);
    }

    @Test
    @DisplayName("좌표 키는 정렬되고 중복이 없어야 한다")
    void rejectsUnsortedOrDuplicatedCoordinateKeys() {
        assertThrows(IllegalArgumentException.class, () -> HilbertIndex.build(new long[] {2, 1}));
        assertThrows(IllegalArgumentException.class, () -> HilbertIndex.build(new long[] {1, 1}));
    }

    @Test
    @DisplayName("음수 경도와 위도를 포함한 좌표 범위를 복원한다")
    void decodesNegativeCoordinates() {
        long[] coordinateKeys = {key(-128.5, -10.25), key(127.5, 37.5)};
        Arrays.sort(coordinateKeys);

        HilbertIndex index = HilbertIndex.build(coordinateKeys);

        assertThat(index.getMinLongitude()).isEqualTo(-128.5);
        assertThat(index.getMaxLongitude()).isEqualTo(127.5);
        assertThat(index.getMinLatitude()).isEqualTo(-10.25);
        assertThat(index.getMaxLatitude()).isEqualTo(37.5);
    }

    private long key(double longitude, double latitude) {
        return GeometryUtil.coordinateToLong(new Coordinate(longitude, latitude));
    }
}
