package com.shortestpath.shortestpath.core.pathengine.Extractor.Task;

import com.shortestpath.shortestpath.core.pathengine.Node;

public class NodeItem implements TaskItem{
    private Node node;

    public NodeItem(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }
}
