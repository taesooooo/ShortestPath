package com.shortestpath.shortestpath.core.pathengine;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HotRoadCache {
    private static final int INDEX_LEVEL_COUNT = 3;

    private final HotRoadCacheMode cacheMode;
    private final int totalNodes;
    private final int totalEdges;
    private final int totalReverseEdges;
    private final int hotNodeCount;
    private final int[] hotNodeIndexByNodeId;
    private final double[] nodeLongitude;
    private final double[] nodeLatitude;
    private final SideCache forwardCache;
    private final SideCache reverseCache;

    private HotRoadCache(
            HotRoadCacheMode cacheMode,
            int totalNodes,
            int totalEdges,
            int totalReverseEdges,
            int hotNodeCount,
            int[] hotNodeIndexByNodeId,
            double[] nodeLongitude,
            double[] nodeLatitude,
            SideCache forwardCache,
            SideCache reverseCache) {
        this.cacheMode = cacheMode;
        this.totalNodes = totalNodes;
        this.totalEdges = totalEdges;
        this.totalReverseEdges = totalReverseEdges;
        this.hotNodeCount = hotNodeCount;
        this.hotNodeIndexByNodeId = hotNodeIndexByNodeId;
        this.nodeLongitude = nodeLongitude;
        this.nodeLatitude = nodeLatitude;
        this.forwardCache = forwardCache;
        this.reverseCache = reverseCache;
    }

    public static HotRoadCache load(DataStore store, boolean useMappedViews) throws IOException {
        return load(store, useMappedViews, HotRoadCacheMode.INDEX_ONLY);
    }

    public static HotRoadCache load(DataStore store, boolean useMappedViews, HotRoadCacheMode cacheMode)
            throws IOException {
        HotRoadCacheMode resolvedMode = cacheMode == null ? HotRoadCacheMode.INDEX_ONLY : cacheMode;
        long startTime = System.currentTimeMillis();
        int totalNodes = store.getTotalNodes();
        int totalEdges = store.getTotalEdges();
        int totalReverseEdges = store.getTotalReverseEdges();

        if (totalNodes <= 0 || totalEdges <= 0 || totalReverseEdges <= 0) {
            log.warn("HotRoadCache 로드를 건너뜁니다. totalNodes={}, totalEdges={}, totalReverseEdges={}, mode={}",
                    totalNodes, totalEdges, totalReverseEdges, resolvedMode);
            return empty(totalNodes, totalEdges, totalReverseEdges, resolvedMode);
        }

        BitSet hotNodeSet = resolvedMode.isEdgeDataCacheEnabled() ? new BitSet(totalNodes) : null;
        int forwardHotEdgeCount = resolvedMode.isEdgeDataCacheEnabled()
                ? countHotEdges(store, false, totalEdges, totalNodes, hotNodeSet, useMappedViews)
                : 0;
        int reverseHotEdgeCount = resolvedMode.isEdgeDataCacheEnabled()
                ? countHotEdges(store, true, totalReverseEdges, totalNodes, hotNodeSet, useMappedViews)
                : 0;

        int hotNodeCount = hotNodeSet == null ? 0 : hotNodeSet.cardinality();
        int[] hotNodeIndexByNodeId = resolvedMode.isEdgeDataCacheEnabled() ? new int[totalNodes] : new int[0];
        double[] nodeLongitude = new double[hotNodeCount];
        double[] nodeLatitude = new double[hotNodeCount];

        if (resolvedMode.isEdgeDataCacheEnabled()) {
            fillHotNodeCoordinates(store, hotNodeSet, hotNodeIndexByNodeId, nodeLongitude, nodeLatitude,
                    useMappedViews);
        }

        SideCache forwardCache = loadSide(store, false, totalNodes, totalEdges, forwardHotEdgeCount,
                resolvedMode.isEdgeDataCacheEnabled(), useMappedViews);
        SideCache reverseCache = loadSide(store, true, totalNodes, totalReverseEdges, reverseHotEdgeCount,
                resolvedMode.isEdgeDataCacheEnabled(), useMappedViews);

        HotRoadCache cache = new HotRoadCache(
                resolvedMode,
                totalNodes,
                totalEdges,
                totalReverseEdges,
                hotNodeCount,
                hotNodeIndexByNodeId,
                nodeLongitude,
                nodeLatitude,
                forwardCache,
                reverseCache);

        log.info(
                "HotRoadCache 로드 완료 - mode: {}, forward L0 edge: {}, reverse L0 edge: {}, L0/L1/L2 index 양방향, node: {}, 예상 메모리: {} MiB, 시간: {}초",
                resolvedMode,
                forwardHotEdgeCount,
                reverseHotEdgeCount,
                hotNodeCount,
                cache.estimatedMemoryBytes() / (1024 * 1024),
                (System.currentTimeMillis() - startTime) / 1000.0);

        return cache;
    }

    private static HotRoadCache empty(int totalNodes, int totalEdges, int totalReverseEdges,
            HotRoadCacheMode cacheMode) {
        int safeTotalNodes = Math.max(totalNodes, 0);
        int safeTotalEdges = Math.max(totalEdges, 0);
        int safeTotalReverseEdges = Math.max(totalReverseEdges, 0);

        return new HotRoadCache(
                cacheMode,
                safeTotalNodes,
                safeTotalEdges,
                safeTotalReverseEdges,
                0,
                new int[0],
                new double[0],
                new double[0],
                SideCache.empty(safeTotalNodes, safeTotalEdges),
                SideCache.empty(safeTotalNodes, safeTotalReverseEdges));
    }

    private static int countHotEdges(
            DataStore store,
            boolean reverseSide,
            int totalEdges,
            int totalNodes,
            BitSet hotNodeSet,
            boolean useMappedViews) throws IOException {
        int hotEdgeCount = 0;

        for (int edgeId = 0; edgeId < totalEdges; edgeId++) {
            long edgeOffset = DataStructureSizes.calculateEdgeOffset(edgeId);
            CachedEdgeData edgeData = readEdgeData(store, reverseSide, edgeOffset, useMappedViews);

            if (!isHotEdgeLevel(edgeData.roadLevel)) {
                continue;
            }

            hotEdgeCount++;
            if (edgeData.from >= 0 && edgeData.from < totalNodes) {
                hotNodeSet.set(edgeData.from);
            }
            if (edgeData.to >= 0 && edgeData.to < totalNodes) {
                hotNodeSet.set(edgeData.to);
            }
        }

        return hotEdgeCount;
    }

    private static void fillHotNodeCoordinates(
            DataStore store,
            BitSet hotNodeSet,
            int[] hotNodeIndexByNodeId,
            double[] nodeLongitude,
            double[] nodeLatitude,
            boolean useMappedViews) throws IOException {
        Arrays.fill(hotNodeIndexByNodeId, -1);

        int hotNodeIndex = 0;
        for (int nodeId = hotNodeSet.nextSetBit(0); nodeId >= 0; nodeId = hotNodeSet.nextSetBit(nodeId + 1)) {
            hotNodeIndexByNodeId[nodeId] = hotNodeIndex;
            if (useMappedViews) {
                nodeLongitude[hotNodeIndex] = store.viewNodeXCoordinate(nodeId);
                nodeLatitude[hotNodeIndex] = store.viewNodeYCoordinate(nodeId);
            } else {
                Coordinate coordinate = store.readNode(DataStructureSizes.calculateNodeOffset(nodeId)).getCoordinate();
                nodeLongitude[hotNodeIndex] = coordinate.getLongitude();
                nodeLatitude[hotNodeIndex] = coordinate.getLatitude();
            }
            hotNodeIndex++;
        }
    }

    private static SideCache loadSide(
            DataStore store,
            boolean reverseSide,
            int totalNodes,
            int totalEdges,
            int hotEdgeCount,
            boolean cacheEdgeData,
            boolean useMappedViews) throws IOException {
        int[] levelStartOffset = new int[totalNodes * INDEX_LEVEL_COUNT];
        int[] levelEdgeCount = new int[totalNodes * INDEX_LEVEL_COUNT];
        int[] hotEdgeIndexByEdgeId = cacheEdgeData ? new int[totalEdges] : new int[0];
        int[] edgeTo = cacheEdgeData ? new int[hotEdgeCount] : new int[0];
        float[] edgeCost = cacheEdgeData ? new float[hotEdgeCount] : new float[0];
        byte[] edgeLevel = cacheEdgeData ? new byte[hotEdgeCount] : new byte[0];

        Arrays.fill(levelStartOffset, -1);
        if (cacheEdgeData) {
            Arrays.fill(hotEdgeIndexByEdgeId, -1);
        }

        int hotEdgeIndex = 0;
        for (int edgeId = 0; edgeId < totalEdges; edgeId++) {
            long edgeOffset = DataStructureSizes.calculateEdgeOffset(edgeId);
            CachedEdgeData edgeData = readEdgeData(store, reverseSide, edgeOffset, useMappedViews);

            int indexNodeId = reverseSide ? edgeData.to : edgeData.from;
            int slot = levelSlot(indexNodeId, edgeData.roadLevel);

            if (slot >= 0 && slot < levelStartOffset.length) {
                if (levelStartOffset[slot] == -1) {
                    levelStartOffset[slot] = (int) edgeOffset;
                }
                levelEdgeCount[slot]++;
            }

            if (!cacheEdgeData || !isHotEdgeLevel(edgeData.roadLevel)) {
                continue;
            }

            hotEdgeIndexByEdgeId[edgeId] = hotEdgeIndex;
            edgeTo[hotEdgeIndex] = reverseSide ? edgeData.from : edgeData.to;
            edgeCost[hotEdgeIndex] = (float) calculateWeightedDistance(edgeData.distance, edgeData.roadLevel);
            edgeLevel[hotEdgeIndex] = (byte) edgeData.roadLevel.ordinal();
            hotEdgeIndex++;
        }

        return new SideCache(totalEdges, hotEdgeCount, levelStartOffset, levelEdgeCount, hotEdgeIndexByEdgeId,
                edgeTo, edgeCost, edgeLevel);
    }

    private static CachedEdgeData readEdgeData(
            DataStore store,
            boolean reverseSide,
            long edgeOffset,
            boolean useMappedViews) throws IOException {
        if (useMappedViews) {
            if (reverseSide) {
                return new CachedEdgeData(
                        store.viewReverseEdgeFrom(edgeOffset),
                        store.viewReverseEdgeTo(edgeOffset),
                        store.viewReverseEdgeDistance(edgeOffset),
                        store.viewReverseEdgeRoadLevel(edgeOffset));
            }

            return new CachedEdgeData(
                    store.viewEdgeFrom(edgeOffset),
                    store.viewEdgeTo(edgeOffset),
                    store.viewEdgeDistance(edgeOffset),
                    store.viewEdgeRoadLevel(edgeOffset));
        }

        Edge edge = reverseSide ? store.readReverseEdge(edgeOffset) : store.readEdge(edgeOffset);
        return new CachedEdgeData(edge.getFrom(), edge.getTo(), edge.getDistance(), edge.getRoadLevel());
    }

    public boolean supportsLevel(RoadLevel roadLevel) {
        return supportsLevel(roadLevel, false);
    }

    public boolean supportsLevel(RoadLevel roadLevel, boolean reverseSide) {
        return sideCache(reverseSide).supportsLevel(roadLevel);
    }

    public int getLevelEdgeCount(int nodeId, RoadLevel roadLevel) {
        return getLevelEdgeCount(nodeId, roadLevel, false);
    }

    public int getLevelEdgeCount(int nodeId, RoadLevel roadLevel, boolean reverseSide) {
        return sideCache(reverseSide).getLevelEdgeCount(nodeId, roadLevel);
    }

    public int getLevelStartOffset(int nodeId, RoadLevel roadLevel) {
        return getLevelStartOffset(nodeId, roadLevel, false);
    }

    public int getLevelStartOffset(int nodeId, RoadLevel roadLevel, boolean reverseSide) {
        return sideCache(reverseSide).getLevelStartOffset(nodeId, roadLevel);
    }

    public int[] getConnectedLevelEdges(int nodeId, RoadLevel roadLevel) {
        return getConnectedLevelEdges(nodeId, roadLevel, false);
    }

    public int[] getConnectedLevelEdges(int nodeId, RoadLevel roadLevel, boolean reverseSide) {
        return sideCache(reverseSide).getConnectedLevelEdges(nodeId, roadLevel);
    }

    public Coordinate getNodeCoordinate(int nodeId) {
        if (!cacheMode.isEdgeDataCacheEnabled() || nodeId < 0 || nodeId >= hotNodeIndexByNodeId.length) {
            return null;
        }

        int hotNodeIndex = hotNodeIndexByNodeId[nodeId];
        if (hotNodeIndex < 0) {
            return null;
        }

        return new Coordinate(nodeLatitude[hotNodeIndex], nodeLongitude[hotNodeIndex]);
    }

    public boolean containsEdge(long edgeOffset) {
        return containsEdge(edgeOffset, false);
    }

    public boolean containsEdge(long edgeOffset, boolean reverseSide) {
        return sideCache(reverseSide).containsEdge(edgeOffset);
    }

    public int getEdgeTo(long edgeOffset) {
        return getEdgeTo(edgeOffset, false);
    }

    public int getEdgeTo(long edgeOffset, boolean reverseSide) {
        return sideCache(reverseSide).getEdgeTo(edgeOffset);
    }

    public RoadLevel getEdgeRoadLevel(long edgeOffset) {
        return getEdgeRoadLevel(edgeOffset, false);
    }

    public RoadLevel getEdgeRoadLevel(long edgeOffset, boolean reverseSide) {
        return sideCache(reverseSide).getEdgeRoadLevel(edgeOffset);
    }

    public double getWeightedDistance(long edgeOffset) {
        return getWeightedDistance(edgeOffset, false);
    }

    public double getWeightedDistance(long edgeOffset, boolean reverseSide) {
        return sideCache(reverseSide).getWeightedDistance(edgeOffset);
    }

    public long estimatedMemoryBytes() {
        return ((long) hotNodeIndexByNodeId.length * Integer.BYTES)
                + ((long) nodeLongitude.length * Double.BYTES)
                + ((long) nodeLatitude.length * Double.BYTES)
                + forwardCache.estimatedMemoryBytes()
                + reverseCache.estimatedMemoryBytes();
    }

    public int getHotEdgeCount() {
        return forwardCache.hotEdgeCount;
    }

    public int getReverseHotEdgeCount() {
        return reverseCache.hotEdgeCount;
    }

    public int getHotNodeCount() {
        return hotNodeCount;
    }

    public int getTotalNodes() {
        return totalNodes;
    }

    public int getTotalEdges() {
        return totalEdges;
    }

    public int getTotalReverseEdges() {
        return totalReverseEdges;
    }

    private SideCache sideCache(boolean reverseSide) {
        return reverseSide ? reverseCache : forwardCache;
    }

    private static boolean isHotEdgeLevel(RoadLevel roadLevel) {
        return roadLevel == RoadLevel.L0;
    }

    private static boolean isIndexLevel(RoadLevel roadLevel) {
        return roadLevel == RoadLevel.L0 || roadLevel == RoadLevel.L1 || roadLevel == RoadLevel.L2;
    }

    private static int levelSlot(int nodeId, RoadLevel roadLevel) {
        if (nodeId < 0 || !isIndexLevel(roadLevel)) {
            return -1;
        }

        return (nodeId * INDEX_LEVEL_COUNT) + roadLevel.ordinal();
    }

    private static double calculateWeightedDistance(double baseDistance, RoadLevel roadLevel) {
        double speed;
        double weight;

        switch (roadLevel) {
            case L0:
                speed = 100;
                weight = 0.5;
                break;
            case L1:
                speed = 60;
                weight = 1.0;
                break;
            case L2:
                speed = 30;
                weight = 1.5;
                break;
            default:
                speed = 20;
                weight = 2.5;
        }

        return (baseDistance / speed) * weight;
    }

    private static final class CachedEdgeData {
        private final int from;
        private final int to;
        private final double distance;
        private final RoadLevel roadLevel;

        private CachedEdgeData(int from, int to, double distance, RoadLevel roadLevel) {
            this.from = from;
            this.to = to;
            this.distance = distance;
            this.roadLevel = roadLevel;
        }
    }

    private static final class SideCache {
        private final int totalEdges;
        private final int hotEdgeCount;
        private final int[] levelStartOffset;
        private final int[] levelEdgeCount;
        private final int[] hotEdgeIndexByEdgeId;
        private final int[] edgeTo;
        private final float[] edgeCost;
        private final byte[] edgeLevel;

        private SideCache(
                int totalEdges,
                int hotEdgeCount,
                int[] levelStartOffset,
                int[] levelEdgeCount,
                int[] hotEdgeIndexByEdgeId,
                int[] edgeTo,
                float[] edgeCost,
                byte[] edgeLevel) {
            this.totalEdges = totalEdges;
            this.hotEdgeCount = hotEdgeCount;
            this.levelStartOffset = levelStartOffset;
            this.levelEdgeCount = levelEdgeCount;
            this.hotEdgeIndexByEdgeId = hotEdgeIndexByEdgeId;
            this.edgeTo = edgeTo;
            this.edgeCost = edgeCost;
            this.edgeLevel = edgeLevel;
        }

        private static SideCache empty(int totalNodes, int totalEdges) {
            int[] levelStartOffset = new int[totalNodes * INDEX_LEVEL_COUNT];
            Arrays.fill(levelStartOffset, -1);

            return new SideCache(
                    totalEdges,
                    0,
                    levelStartOffset,
                    new int[totalNodes * INDEX_LEVEL_COUNT],
                    new int[0],
                    new int[0],
                    new float[0],
                    new byte[0]);
        }

        private boolean supportsLevel(RoadLevel roadLevel) {
            return isIndexLevel(roadLevel) && levelEdgeCount.length > 0;
        }

        private int getLevelEdgeCount(int nodeId, RoadLevel roadLevel) {
            int slot = levelSlot(nodeId, roadLevel);
            if (slot < 0 || slot >= levelEdgeCount.length) {
                return 0;
            }

            return levelEdgeCount[slot];
        }

        private int getLevelStartOffset(int nodeId, RoadLevel roadLevel) {
            int slot = levelSlot(nodeId, roadLevel);
            if (slot < 0 || slot >= levelStartOffset.length) {
                return -1;
            }

            return levelStartOffset[slot];
        }

        private int[] getConnectedLevelEdges(int nodeId, RoadLevel roadLevel) {
            int count = getLevelEdgeCount(nodeId, roadLevel);
            int[] edgeOffsets = new int[count];
            int startOffset = getLevelStartOffset(nodeId, roadLevel);

            for (int i = 0; i < count; i++) {
                edgeOffsets[i] = startOffset + (i * DataStructureSizes.EDGE_SIZE);
            }

            return edgeOffsets;
        }

        private boolean containsEdge(long edgeOffset) {
            return hotEdgeIndex(edgeOffset) >= 0;
        }

        private int getEdgeTo(long edgeOffset) {
            int hotEdgeIndex = hotEdgeIndex(edgeOffset);
            if (hotEdgeIndex < 0) {
                throw new IllegalArgumentException("캐시되지 않은 엣지입니다. offset=" + edgeOffset);
            }

            return edgeTo[hotEdgeIndex];
        }

        private RoadLevel getEdgeRoadLevel(long edgeOffset) {
            int hotEdgeIndex = hotEdgeIndex(edgeOffset);
            if (hotEdgeIndex < 0) {
                throw new IllegalArgumentException("캐시되지 않은 엣지입니다. offset=" + edgeOffset);
            }

            return RoadLevel.valueOf((int) edgeLevel[hotEdgeIndex]);
        }

        private double getWeightedDistance(long edgeOffset) {
            int hotEdgeIndex = hotEdgeIndex(edgeOffset);
            if (hotEdgeIndex < 0) {
                throw new IllegalArgumentException("캐시되지 않은 엣지입니다. offset=" + edgeOffset);
            }

            return edgeCost[hotEdgeIndex];
        }

        private int hotEdgeIndex(long edgeOffset) {
            if (hotEdgeIndexByEdgeId.length == 0) {
                return -1;
            }

            long relativeOffset = edgeOffset - DataStructureSizes.HEADER_SIZE;
            if (relativeOffset < 0 || relativeOffset % DataStructureSizes.EDGE_SIZE != 0) {
                return -1;
            }

            long edgeId = relativeOffset / DataStructureSizes.EDGE_SIZE;
            if (edgeId < 0 || edgeId >= hotEdgeIndexByEdgeId.length) {
                return -1;
            }

            return hotEdgeIndexByEdgeId[(int) edgeId];
        }

        private long estimatedMemoryBytes() {
            return ((long) levelStartOffset.length * Integer.BYTES)
                    + ((long) levelEdgeCount.length * Integer.BYTES)
                    + ((long) hotEdgeIndexByEdgeId.length * Integer.BYTES)
                    + ((long) edgeTo.length * Integer.BYTES)
                    + ((long) edgeCost.length * Float.BYTES)
                    + edgeLevel.length;
        }
    }
}
