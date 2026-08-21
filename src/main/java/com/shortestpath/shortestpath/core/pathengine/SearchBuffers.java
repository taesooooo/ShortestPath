package com.shortestpath.shortestpath.core.pathengine;

import java.util.Arrays;

public final class SearchBuffers {
    private static final int LOCAL_INITIAL_CAPACITY = 65_536;

    private final int[] nodeToLocalIndex;
    private int[] touchedNodeIds;
    private double[] gCostByLocalIndex;
    private int[] previousNodeByLocalIndex;
    private boolean[] visitedByLocalIndex;
    private int size;
    private int touchedCount;

    public SearchBuffers(int capacity) {
        if(capacity < 1) {
            throw new IllegalArgumentException("SearchBuffers capacity는 1 이상이어야 합니다.");
        }

        int localCapacity = Math.min(LOCAL_INITIAL_CAPACITY, capacity);
        this.nodeToLocalIndex = new int[capacity];
        this.touchedNodeIds = new int[localCapacity];
        this.gCostByLocalIndex = new double[localCapacity];
        this.previousNodeByLocalIndex = new int[localCapacity];
        this.visitedByLocalIndex = new boolean[localCapacity];
        this.size = 0;
        this.touchedCount = 0;
    }

    public int prepare(int maxNodeId) {
        ensureCapacity(maxNodeId);
        clearTouchedNodes();
        return 0;
    }

    public void ensureCapacity(int nodeId) {
        if(nodeId < 0 || nodeId >= nodeToLocalIndex.length) {
            throw new IllegalArgumentException(
                    "유효하지 않은 nodeId입니다. nodeId=" + nodeId + ", capacity=" + nodeToLocalIndex.length);
        }
    }

    public void initializeStartNode(int nodeId) {
        int localIndex = getOrCreateLocalIndex(nodeId);
        gCostByLocalIndex[localIndex] = 0;
        previousNodeByLocalIndex[localIndex] = -1;
        visitedByLocalIndex[localIndex] = false;
    }

    public boolean isVisited(int nodeId) {
        int localIndex = getLocalIndex(nodeId);
        return localIndex >= 0 && visitedByLocalIndex[localIndex];
    }

    public void markVisited(int nodeId) {
        int localIndex = getOrCreateLocalIndex(nodeId);
        visitedByLocalIndex[localIndex] = true;
    }

    public double getGCost(int nodeId) {
        return getCurrentGCost(nodeId);
    }

    public double getCurrentGCost(int nodeId) {
        int localIndex = getLocalIndex(nodeId);
        return localIndex >= 0 ? gCostByLocalIndex[localIndex] : Double.MAX_VALUE;
    }

    public void updateCost(int nodeId, int previousNodeId, double newGCost) {
        int localIndex = getOrCreateLocalIndex(nodeId);
        gCostByLocalIndex[localIndex] = newGCost;
        previousNodeByLocalIndex[localIndex] = previousNodeId;
    }

    public boolean hasCost(int nodeId) {
        return getLocalIndex(nodeId) >= 0;
    }

    public int getPreviousNode(int nodeId) {
        int localIndex = getLocalIndex(nodeId);
        return localIndex >= 0 ? previousNodeByLocalIndex[localIndex] : -1;
    }

    public int capacity() {
        return nodeToLocalIndex.length;
    }

    private int getLocalIndex(int nodeId) {
        ensureCapacity(nodeId);
        return nodeToLocalIndex[nodeId] - 1;
    }

    private int getOrCreateLocalIndex(int nodeId) {
        ensureCapacity(nodeId);

        int localPlusOne = nodeToLocalIndex[nodeId];
        if(localPlusOne != 0) {
            return localPlusOne - 1;
        }

        int localIndex = size++;
        ensureLocalCapacity(size);
        nodeToLocalIndex[nodeId] = localIndex + 1;
        touchedNodeIds[touchedCount++] = nodeId;
        previousNodeByLocalIndex[localIndex] = -1;
        visitedByLocalIndex[localIndex] = false;

        return localIndex;
    }

    private void clearTouchedNodes() {
        for(int i = 0; i < touchedCount; i++) {
            nodeToLocalIndex[touchedNodeIds[i]] = 0;
        }

        size = 0;
        touchedCount = 0;
    }

    private void ensureLocalCapacity(int requiredCapacity) {
        if(requiredCapacity <= touchedNodeIds.length) {
            return;
        }

        int newCapacity = touchedNodeIds.length;
        while(newCapacity < requiredCapacity) {
            newCapacity = newCapacity + (newCapacity >> 1);
            if(newCapacity <= 0) {
                newCapacity = requiredCapacity;
                break;
            }
        }

        touchedNodeIds = Arrays.copyOf(touchedNodeIds, newCapacity);
        gCostByLocalIndex = Arrays.copyOf(gCostByLocalIndex, newCapacity);
        previousNodeByLocalIndex = Arrays.copyOf(previousNodeByLocalIndex, newCapacity);
        visitedByLocalIndex = Arrays.copyOf(visitedByLocalIndex, newCapacity);
    }
}
