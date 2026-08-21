package com.shortestpath.shortestpath.core.pathengine;

public class SearchRoute {
    private Node node;
    private Edge edge;

    private int nodeId;
    private int edgeOffset;
    private double gCost;
    private double fCost;
    
    public SearchRoute(Node node, Edge edge) {
        this.node = node;
        this.edge = edge;
    }
    public SearchRoute(int nodeId, int edgeOffset, double gCost, double fCost) {
        this.nodeId = nodeId;
        this.edgeOffset = edgeOffset;
        this.gCost = gCost;
        this.fCost = fCost;
    }
    public Node getNode() {
        return node;
    }
    public void setNode(Node node) {
        this.node = node;
    }
    public Edge getEdge() {
        return edge;
    }
    public void setEdge(Edge edge) {
        this.edge = edge;
    }
    public int getNodeId() {
        return nodeId;
    }
    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }
    public int getEdgeOffset() {
        return edgeOffset;
    }
    public void setEdgeOffset(int edgeOffset) {
        this.edgeOffset = edgeOffset;
    }
    public double getgCost() {
        return gCost;
    }
    public void setgCost(double gCost) {
        this.gCost = gCost;
    }
    public double getfCost() {
        return fCost;
    }
    public void setfCost(double fCost) {
        this.fCost = fCost;
    }

    
}
