package com.shortestpath.shortestpath.core.pathengine.Extractor.Task;

import com.shortestpath.shortestpath.core.pathengine.Node;

public class NodeEdgeItem implements TaskItem {
    private Node nodeA;
    private Node nodeB;


    public NodeEdgeItem(Node nodeA, Node nodeB) {
        this.nodeA = nodeA;
        this.nodeB = nodeB;
    }

    public Node getNodeA() {
        return nodeA;
    }

    public Node getNodeB() {
        return nodeB;
    }
}
