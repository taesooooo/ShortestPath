package com.shortestpath.shortestpath.core.pathengine;

import java.util.Arrays;

public final class SearchBuffers {
    private double[] gCost;
    private double[] hCost;
    private int[] previousNode;
    private int[] costStamp;
    private int[] heuristicStamp;
    private int[] visitedStamp;
    private int generation;

    public SearchBuffers(int capacity) {
        this.gCost = new double[capacity];
        this.hCost = new double[capacity];
        this.previousNode = new int[capacity];
        this.costStamp = new int[capacity];
        this.heuristicStamp = new int[capacity];
        this.visitedStamp = new int[capacity];
        this.generation = 0;
    }

    public int nextGeneration() {
        generation++;
        if(generation == Integer.MAX_VALUE) {
            Arrays.fill(costStamp, 0);
            Arrays.fill(heuristicStamp, 0);
            Arrays.fill(visitedStamp, 0);
            generation = 1;
        }
        return generation;
    }

    public int prepare(int maxNodeId) {
        ensureCapacity(maxNodeId);
        return nextGeneration();
    }

    public void ensureCapacity(int nodeId) {
        if(nodeId < gCost.length) {
            return;
        }

        int newCapacity = gCost.length;
        while(newCapacity <= nodeId) {
            newCapacity = newCapacity + (newCapacity >> 1);
            if(newCapacity <= 0) {
                newCapacity = nodeId + 1;
                break;
            }
        }

        gCost = Arrays.copyOf(gCost, newCapacity);
        hCost = Arrays.copyOf(hCost, newCapacity);
        previousNode = Arrays.copyOf(previousNode, newCapacity);
        costStamp = Arrays.copyOf(costStamp, newCapacity);
        heuristicStamp = Arrays.copyOf(heuristicStamp, newCapacity);
        visitedStamp = Arrays.copyOf(visitedStamp, newCapacity);
    }

    public void initializeStartNode(int nodeId, double heuristic) {
        ensureCapacity(nodeId);
        gCost[nodeId] = 0;
        hCost[nodeId] = heuristic;
        previousNode[nodeId] = -1;
        costStamp[nodeId] = generation;
        heuristicStamp[nodeId] = generation;
    }

    public boolean isVisited(int nodeId) {
        ensureCapacity(nodeId);
        return visitedStamp[nodeId] == generation;
    }

    public void markVisited(int nodeId) {
        ensureCapacity(nodeId);
        visitedStamp[nodeId] = generation;
    }

    public double getGCost(int nodeId) {
        ensureCapacity(nodeId);
        return gCost[nodeId];
    }

    public double getCurrentGCost(int nodeId) {
        ensureCapacity(nodeId);
        return costStamp[nodeId] == generation ? gCost[nodeId] : Double.MAX_VALUE;
    }

    public void updateCost(int nodeId, int previousNodeId, double newGCost) {
        ensureCapacity(nodeId);
        gCost[nodeId] = newGCost;
        previousNode[nodeId] = previousNodeId;
        costStamp[nodeId] = generation;
    }

    public boolean hasCost(int nodeId) {
        ensureCapacity(nodeId);
        return costStamp[nodeId] == generation;
    }

    public int getPreviousNode(int nodeId) {
        ensureCapacity(nodeId);
        return previousNode[nodeId];
    }

    public boolean hasHeuristic(int nodeId) {
        ensureCapacity(nodeId);
        return heuristicStamp[nodeId] == generation;
    }

    public double getHeuristic(int nodeId) {
        ensureCapacity(nodeId);
        return hCost[nodeId];
    }

    public void setHeuristic(int nodeId, double heuristic) {
        ensureCapacity(nodeId);
        hCost[nodeId] = heuristic;
        heuristicStamp[nodeId] = generation;
    }

    public int capacity() {
        return gCost.length;
    }
}
