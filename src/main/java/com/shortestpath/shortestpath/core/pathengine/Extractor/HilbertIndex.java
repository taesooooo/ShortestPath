package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.util.Arrays;

import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

/**
 * 좌표 키를 Hilbert 곡선 순서의 내부 nodeId로 변환하는 인덱스입니다.
 *
 * <p>좌표 키 배열은 오름차순으로 정렬되고 중복이 제거된 상태여야 합니다.
 * 동일한 Hilbert 셀에 속한 노드는 기존 nodeId(좌표 키 배열의 위치) 순서로
 * 정렬하여 결과가 항상 결정적이도록 합니다.</p>
 */
public final class HilbertIndex {
    public static final int DEFAULT_BITS_PER_AXIS = 10;

    private final long[] coordinateKeys;
    private final int[] nodeIdByCoordinatePosition;
    private final long[] orderedKeys;
    private final int bitsPerAxis;
    private final double minLongitude;
    private final double maxLongitude;
    private final double minLatitude;
    private final double maxLatitude;

    private HilbertIndex(long[] coordinateKeys,
            int[] nodeIdByCoordinatePosition,
            long[] orderedKeys,
            int bitsPerAxis,
            double minLongitude,
            double maxLongitude,
            double minLatitude,
            double maxLatitude) {
        this.coordinateKeys = coordinateKeys;
        this.nodeIdByCoordinatePosition = nodeIdByCoordinatePosition;
        this.orderedKeys = orderedKeys;
        this.bitsPerAxis = bitsPerAxis;
        this.minLongitude = minLongitude;
        this.maxLongitude = maxLongitude;
        this.minLatitude = minLatitude;
        this.maxLatitude = maxLatitude;
    }

    public static HilbertIndex build(long[] sortedCoordinateKeys) {
        return build(sortedCoordinateKeys, DEFAULT_BITS_PER_AXIS);
    }

    public static HilbertIndex build(long[] sortedCoordinateKeys, int bitsPerAxis) {
        validateInput(sortedCoordinateKeys, bitsPerAxis);

        if (sortedCoordinateKeys.length == 0) {
            return new HilbertIndex(sortedCoordinateKeys, new int[0], new long[0], bitsPerAxis,
                    0, 0, 0, 0);
        }

        double minLongitude = Double.POSITIVE_INFINITY;
        double maxLongitude = Double.NEGATIVE_INFINITY;
        double minLatitude = Double.POSITIVE_INFINITY;
        double maxLatitude = Double.NEGATIVE_INFINITY;

        for (long coordinateKey : sortedCoordinateKeys) {
            double longitude = GeometryUtil.longitudeFromLong(coordinateKey);
            double latitude = GeometryUtil.latitudeFromLong(coordinateKey);
            minLongitude = Math.min(minLongitude, longitude);
            maxLongitude = Math.max(maxLongitude, longitude);
            minLatitude = Math.min(minLatitude, latitude);
            maxLatitude = Math.max(maxLatitude, latitude);
        }

        long[] orderedKeys = new long[sortedCoordinateKeys.length];
        for (int oldNodeId = 0; oldNodeId < sortedCoordinateKeys.length; oldNodeId++) {
            long coordinateKey = sortedCoordinateKeys[oldNodeId];
            int x = normalize(GeometryUtil.longitudeFromLong(coordinateKey), minLongitude, maxLongitude, bitsPerAxis);
            int y = normalize(GeometryUtil.latitudeFromLong(coordinateKey), minLatitude, maxLatitude, bitsPerAxis);
            long hilbertValue = calculateHilbertValue(x, y, bitsPerAxis);

            // 상위 32비트: Hilbert 값, 하위 32비트: 기존 nodeId(tie-breaker)
            orderedKeys[oldNodeId] = (hilbertValue << 32) | Integer.toUnsignedLong(oldNodeId);
        }

        Arrays.sort(orderedKeys);

        int[] nodeIdByCoordinatePosition = new int[sortedCoordinateKeys.length];
        for (int newNodeId = 0; newNodeId < orderedKeys.length; newNodeId++) {
            int oldNodeId = (int) orderedKeys[newNodeId];
            nodeIdByCoordinatePosition[oldNodeId] = newNodeId;
        }

        return new HilbertIndex(sortedCoordinateKeys, nodeIdByCoordinatePosition, orderedKeys, bitsPerAxis,
                minLongitude, maxLongitude, minLatitude, maxLatitude);
    }

    /**
     * 기존 테스트와 단순 조회 코드에서 좌표 키 배열 위치를 그대로 nodeId로 사용합니다.
     */
    public static HilbertIndex naturalOrder(long[] sortedCoordinateKeys) {
        validateSortedUnique(sortedCoordinateKeys);
        int[] nodeIdByCoordinatePosition = new int[sortedCoordinateKeys.length];
        long[] orderedKeys = new long[sortedCoordinateKeys.length];
        for (int nodeId = 0; nodeId < sortedCoordinateKeys.length; nodeId++) {
            nodeIdByCoordinatePosition[nodeId] = nodeId;
            orderedKeys[nodeId] = Integer.toUnsignedLong(nodeId);
        }
        return new HilbertIndex(sortedCoordinateKeys, nodeIdByCoordinatePosition, orderedKeys, 0,
                0, 0, 0, 0);
    }

    public int findNodeId(long coordinateKey) {
        int coordinatePosition = Arrays.binarySearch(coordinateKeys, coordinateKey);
        return coordinatePosition < 0 ? -1 : nodeIdByCoordinatePosition[coordinatePosition];
    }

    public long getCoordinateKey(int nodeId) {
        checkNodeId(nodeId);
        int oldNodeId = (int) orderedKeys[nodeId];
        return coordinateKeys[oldNodeId];
    }

    public long getHilbertValue(int nodeId) {
        checkNodeId(nodeId);
        return orderedKeys[nodeId] >>> 32;
    }

    public int size() {
        return coordinateKeys.length;
    }

    public int getBitsPerAxis() {
        return bitsPerAxis;
    }

    public double getMinLongitude() {
        return minLongitude;
    }

    public double getMaxLongitude() {
        return maxLongitude;
    }

    public double getMinLatitude() {
        return minLatitude;
    }

    public double getMaxLatitude() {
        return maxLatitude;
    }

    public static int normalize(double value, double min, double max, int bitsPerAxis) {
        int gridSize = 1 << bitsPerAxis;
        if (Double.compare(min, max) == 0) {
            return 0;
        }

        double ratio = (value - min) / (max - min);
        int normalized = (int) Math.floor(ratio * gridSize);
        return Math.max(0, Math.min(gridSize - 1, normalized));
    }

    public static long calculateHilbertValue(int x, int y, int bitsPerAxis) {
        int gridSize = 1 << bitsPerAxis;
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
            throw new IllegalArgumentException("정규화 좌표가 Hilbert 격자 범위를 벗어났습니다.");
        }

        long hilbertValue = 0;
        int rotatedX = x;
        int rotatedY = y;

        for (int scale = gridSize >>> 1; scale > 0; scale >>>= 1) {
            int rx = (rotatedX & scale) == 0 ? 0 : 1;
            int ry = (rotatedY & scale) == 0 ? 0 : 1;
            hilbertValue += (long) scale * scale * ((3 * rx) ^ ry);

            if (ry == 0) {
                if (rx == 1) {
                    rotatedX = scale - 1 - rotatedX;
                    rotatedY = scale - 1 - rotatedY;
                }
                int temp = rotatedX;
                rotatedX = rotatedY;
                rotatedY = temp;
            }
        }

        return hilbertValue;
    }

    private static void validateInput(long[] sortedCoordinateKeys, int bitsPerAxis) {
        if (bitsPerAxis < 1 || bitsPerAxis > 15) {
            throw new IllegalArgumentException("bitsPerAxis는 1 이상 15 이하여야 합니다.");
        }
        validateSortedUnique(sortedCoordinateKeys);
    }

    private static void validateSortedUnique(long[] sortedCoordinateKeys) {
        if (sortedCoordinateKeys == null) {
            throw new IllegalArgumentException("좌표 키 배열은 null일 수 없습니다.");
        }
        for (int i = 1; i < sortedCoordinateKeys.length; i++) {
            if (sortedCoordinateKeys[i - 1] >= sortedCoordinateKeys[i]) {
                throw new IllegalArgumentException("좌표 키 배열은 오름차순 정렬되고 중복이 없어야 합니다.");
            }
        }
    }

    private void checkNodeId(int nodeId) {
        if (nodeId < 0 || nodeId >= size()) {
            throw new IndexOutOfBoundsException("nodeId가 범위를 벗어났습니다: " + nodeId);
        }
    }

}
