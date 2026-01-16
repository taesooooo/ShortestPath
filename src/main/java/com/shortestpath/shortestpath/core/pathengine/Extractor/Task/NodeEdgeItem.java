package com.shortestpath.shortestpath.core.pathengine.Extractor.Task;

import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;

public class NodeEdgeItem implements NodeEdgeTaskItem {
    private Node nodeA;
    private Node nodeB;
    private Edge edgeA;
    private Edge EdgeB;

    public NodeEdgeItem(Node nodeA, Node nodeB, Edge edgeA, Edge edgeB) {
        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.edgeA = edgeA;
        EdgeB = edgeB;
    }

    public Node getNodeA() {
        return nodeA;
    }

    public Node getNodeB() {
        return nodeB;
    }

    public Edge getEdgeA() {
        return edgeA;
    }

    public Edge getEdgeB() {
        return EdgeB;
    }
}
