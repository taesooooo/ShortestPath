package com.shortestpath.shortestpath.core.pathengine;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HotRoadCache {
    private static final int INDEX_LEVEL_COUNT = 3;

    private final int totalNodes;
    private final int totalEdges;
    private final int hotEdgeCount;
    private final int hotNodeCount;

    private final int[] levelStartOffset;
    private final int[] levelEdgeCount;
    private final int[] hotEdgeIndexByEdgeId;
    private final int[] edgeTo;
    private final float[] edgeCost;
    private final byte[] edgeLevel;

    private final int[] hotNodeIndexByNodeId;
    private final double[] nodeLongitude;
    private final double[] nodeLatitude;

    private HotRoadCache(
            int totalNodes,
            int totalEdges,
            int hotEdgeCount,
            int hotNodeCount,
            int[] levelStartOffset,
            int[] levelEdgeCount,
            int[] hotEdgeIndexByEdgeId,
            int[] edgeTo,
            float[] edgeCost,
            byte[] edgeLevel,
            int[] hotNodeIndexByNodeId,
            double[] nodeLongitude,
            double[] nodeLatitude) {
        this.totalNodes = totalNodes;
        this.totalEdges = totalEdges;
        this.hotEdgeCount = hotEdgeCount;
        this.hotNodeCount = hotNodeCount;
        this.levelStartOffset = levelStartOffset;
        this.levelEdgeCount = levelEdgeCount;
        this.hotEdgeIndexByEdgeId = hotEdgeIndexByEdgeId;
        this.edgeTo = edgeTo;
        this.edgeCost = edgeCost;
        this.edgeLevel = edgeLevel;
        this.hotNodeIndexByNodeId = hotNodeIndexByNodeId;
        this.nodeLongitude = nodeLongitude;
        this.nodeLatitude = nodeLatitude;
    }

    public static HotRoadCache load(DataStore store, boolean useMappedViews) throws IOException {
        long startTime = System.currentTimeMillis();
        int totalNodes = store.getTotalNodes();
        int totalEdges = store.getTotalEdges();

        if(totalNodes <= 0 || totalEdges <= 0) {
            log.warn("HotRoadCache 로드를 건너뜁니다. totalNodes={}, totalEdges={}", totalNodes, totalEdges);
            return empty(totalNodes, totalEdges);
        }

        BitSet hotNodeSet = new BitSet(totalNodes);
        int hotEdgeCount = 0;

        for(int edgeId = 0; edgeId < totalEdges; edgeId++) {
            long edgeOffset = DataStructureSizes.calculateEdgeOffset(edgeId);
            Edge edge = null;
            RoadLevel roadLevel;
            int from;
            int to;
            if(useMappedViews) {
                roadLevel = store.viewEdgeRoadLevel(edgeOffset);
                from = store.viewEdgeFrom(edgeOffset);
                to = store.viewEdgeTo(edgeOffset);
            } else {
                edge = store.readEdge(edgeOffset);
                roadLevel = edge.getRoadLevel();
                from = edge.getFrom();
                to = edge.getTo();
            }

            if(!isHotEdgeLevel(roadLevel)) {
                continue;
            }

            hotEdgeCount++;
            if(from >= 0 && from < totalNodes) {
                hotNodeSet.set(from);
            }
            if(to >= 0 && to < totalNodes) {
                hotNodeSet.set(to);
            }
        }

        int hotNodeCount = hotNodeSet.cardinality();
        int[] levelStartOffset = new int[totalNodes * INDEX_LEVEL_COUNT];
        int[] levelEdgeCount = new int[totalNodes * INDEX_LEVEL_COUNT];
        int[] hotEdgeIndexByEdgeId = new int[totalEdges];
        int[] hotNodeIndexByNodeId = new int[totalNodes];
        int[] edgeTo = new int[hotEdgeCount];
        float[] edgeCost = new float[hotEdgeCount];
        byte[] edgeLevel = new byte[hotEdgeCount];
        double[] nodeLongitude = new double[hotNodeCount];
        double[] nodeLatitude = new double[hotNodeCount];

        Arrays.fill(levelStartOffset, -1);
        Arrays.fill(hotEdgeIndexByEdgeId, -1);
        Arrays.fill(hotNodeIndexByNodeId, -1);

        int hotNodeIndex = 0;
        for(int nodeId = hotNodeSet.nextSetBit(0); nodeId >= 0; nodeId = hotNodeSet.nextSetBit(nodeId + 1)) {
            hotNodeIndexByNodeId[nodeId] = hotNodeIndex;
            if(useMappedViews) {
                nodeLongitude[hotNodeIndex] = store.viewNodeXCoordinate(nodeId);
                nodeLatitude[hotNodeIndex] = store.viewNodeYCoordinate(nodeId);
            } else {
                Coordinate coordinate = store.readNode(DataStructureSizes.calculateNodeOffset(nodeId)).getCoordinate();
                nodeLongitude[hotNodeIndex] = coordinate.getLongitude();
                nodeLatitude[hotNodeIndex] = coordinate.getLatitude();
            }
            hotNodeIndex++;
        }

        int hotEdgeIndex = 0;
        for(int edgeId = 0; edgeId < totalEdges; edgeId++) {
            long edgeOffset = DataStructureSizes.calculateEdgeOffset(edgeId);
            Edge edge = null;
            RoadLevel roadLevel;
            int from;
            int to;
            double distance;
            if(useMappedViews) {
                roadLevel = store.viewEdgeRoadLevel(edgeOffset);
                from = store.viewEdgeFrom(edgeOffset);
                to = store.viewEdgeTo(edgeOffset);
                distance = store.viewEdgeDistance(edgeOffset);
            } else {
                edge = store.readEdge(edgeOffset);
                roadLevel = edge.getRoadLevel();
                from = edge.getFrom();
                to = edge.getTo();
                distance = edge.getDistance();
            }

            int slot = levelSlot(from, roadLevel);

            if(slot >= 0) {
                if(levelStartOffset[slot] == -1) {
                    levelStartOffset[slot] = (int)edgeOffset;
                }
                levelEdgeCount[slot]++;
            }

            if(!isHotEdgeLevel(roadLevel)) {
                continue;
            }

            hotEdgeIndexByEdgeId[edgeId] = hotEdgeIndex;
            edgeTo[hotEdgeIndex] = to;
            edgeCost[hotEdgeIndex] = (float)calculateWeightedDistance(distance, roadLevel);
            edgeLevel[hotEdgeIndex] = (byte)roadLevel.ordinal();
            hotEdgeIndex++;
        }

        HotRoadCache cache = new HotRoadCache(
                totalNodes,
                totalEdges,
                hotEdgeCount,
                hotNodeCount,
                levelStartOffset,
                levelEdgeCount,
                hotEdgeIndexByEdgeId,
                edgeTo,
                edgeCost,
                edgeLevel,
                hotNodeIndexByNodeId,
                nodeLongitude,
                nodeLatitude);

        log.info(
                "HotRoadCache 로드 완료 - L0/L1 edge: {}, L0/L1/L2 index, node: {}, 예상 메모리: {} MiB, 시간: {}초",
                hotEdgeCount,
                hotNodeCount,
                cache.estimatedMemoryBytes() / (1024 * 1024),
                (System.currentTimeMillis() - startTime) / 1000.0);

        return cache;
    }

    private static HotRoadCache empty(int totalNodes, int totalEdges) {
        int safeTotalNodes = Math.max(totalNodes, 0);
        int safeTotalEdges = Math.max(totalEdges, 0);
        int[] levelStartOffset = new int[safeTotalNodes * INDEX_LEVEL_COUNT];
        Arrays.fill(levelStartOffset, -1);
        int[] hotEdgeIndexByEdgeId = new int[safeTotalEdges];
        int[] hotNodeIndexByNodeId = new int[safeTotalNodes];
        Arrays.fill(hotEdgeIndexByEdgeId, -1);
        Arrays.fill(hotNodeIndexByNodeId, -1);

        return new HotRoadCache(
                safeTotalNodes,
                safeTotalEdges,
                0,
                0,
                levelStartOffset,
                new int[safeTotalNodes * INDEX_LEVEL_COUNT],
                hotEdgeIndexByEdgeId,
                new int[0],
                new float[0],
                new byte[0],
                hotNodeIndexByNodeId,
                new double[0],
                new double[0]);
    }

    public boolean supportsLevel(RoadLevel roadLevel) {
        return isIndexLevel(roadLevel) && levelEdgeCount.length > 0;
    }

    public int getLevelEdgeCount(int nodeId, RoadLevel roadLevel) {
        int slot = levelSlot(nodeId, roadLevel);
        if(slot < 0 || slot >= levelEdgeCount.length) {
            return 0;
        }

        return levelEdgeCount[slot];
    }

    public int getLevelStartOffset(int nodeId, RoadLevel roadLevel) {
        int slot = levelSlot(nodeId, roadLevel);
        if(slot < 0 || slot >= levelStartOffset.length) {
            return -1;
        }

        return levelStartOffset[slot];
    }

    public int[] getConnectedLevelEdges(int nodeId, RoadLevel roadLevel) {
        int count = getLevelEdgeCount(nodeId, roadLevel);
        int[] edgeOffsets = new int[count];
        int startOffset = getLevelStartOffset(nodeId, roadLevel);

        for(int i = 0; i < count; i++) {
            edgeOffsets[i] = startOffset + (i * DataStructureSizes.EDGE_SIZE);
        }

        return edgeOffsets;
    }

    public Coordinate getNodeCoordinate(int nodeId) {
        if(nodeId < 0 || nodeId >= hotNodeIndexByNodeId.length) {
            return null;
        }

        int hotNodeIndex = hotNodeIndexByNodeId[nodeId];
        if(hotNodeIndex < 0) {
            return null;
        }

        return new Coordinate(nodeLatitude[hotNodeIndex], nodeLongitude[hotNodeIndex]);
    }

    public boolean containsEdge(long edgeOffset) {
        return hotEdgeIndex(edgeOffset) >= 0;
    }

    public int getEdgeTo(long edgeOffset) {
        int hotEdgeIndex = hotEdgeIndex(edgeOffset);
        if(hotEdgeIndex < 0) {
            throw new IllegalArgumentException("캐시되지 않은 엣지입니다. offset=" + edgeOffset);
        }

        return edgeTo[hotEdgeIndex];
    }

    public RoadLevel getEdgeRoadLevel(long edgeOffset) {
        int hotEdgeIndex = hotEdgeIndex(edgeOffset);
        if(hotEdgeIndex < 0) {
            throw new IllegalArgumentException("캐시되지 않은 엣지입니다. offset=" + edgeOffset);
        }

        return RoadLevel.valueOf((int)edgeLevel[hotEdgeIndex]);
    }

    public double getWeightedDistance(long edgeOffset) {
        int hotEdgeIndex = hotEdgeIndex(edgeOffset);
        if(hotEdgeIndex < 0) {
            throw new IllegalArgumentException("캐시되지 않은 엣지입니다. offset=" + edgeOffset);
        }

        return edgeCost[hotEdgeIndex];
    }

    public long estimatedMemoryBytes() {
        return ((long)levelStartOffset.length * Integer.BYTES)
                + ((long)levelEdgeCount.length * Integer.BYTES)
                + ((long)hotEdgeIndexByEdgeId.length * Integer.BYTES)
                + ((long)edgeTo.length * Integer.BYTES)
                + ((long)edgeCost.length * Float.BYTES)
                + edgeLevel.length
                + ((long)hotNodeIndexByNodeId.length * Integer.BYTES)
                + ((long)nodeLongitude.length * Double.BYTES)
                + ((long)nodeLatitude.length * Double.BYTES);
    }

    public int getHotEdgeCount() {
        return hotEdgeCount;
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

    private int hotEdgeIndex(long edgeOffset) {
        long relativeOffset = edgeOffset - DataStructureSizes.HEADER_SIZE;
        if(relativeOffset < 0 || relativeOffset % DataStructureSizes.EDGE_SIZE != 0) {
            return -1;
        }

        long edgeId = relativeOffset / DataStructureSizes.EDGE_SIZE;
        if(edgeId < 0 || edgeId >= hotEdgeIndexByEdgeId.length) {
            return -1;
        }

        return hotEdgeIndexByEdgeId[(int)edgeId];
    }

    private static boolean isHotEdgeLevel(RoadLevel roadLevel) {
        return roadLevel == RoadLevel.L0 || roadLevel == RoadLevel.L1;
    }

    private static boolean isIndexLevel(RoadLevel roadLevel) {
        return roadLevel == RoadLevel.L0 || roadLevel == RoadLevel.L1 || roadLevel == RoadLevel.L2;
    }

    private static int levelSlot(int nodeId, RoadLevel roadLevel) {
        if(nodeId < 0 || !isIndexLevel(roadLevel)) {
            return -1;
        }

        return (nodeId * INDEX_LEVEL_COUNT) + roadLevel.ordinal();
    }

    private static double calculateWeightedDistance(double baseDistance, RoadLevel roadLevel) {
        double speed;
        double weight;

        switch(roadLevel) {
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
}
