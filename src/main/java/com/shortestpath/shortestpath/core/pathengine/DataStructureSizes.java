package com.shortestpath.shortestpath.core.pathengine;

public class DataStructureSizes {
    // Node의 id(int, 4바이트), startEdgeOffset(int, 4바이트), x(double, 8바이트), y(double, 8바이트)
    public static final int NODE_SIZE = 24;
        // Edge id(int, 4바이트), from(int, 4바이트), to(int, 4바이트), distance(double, 8바이트), nextEdgeOffset(int, 4바이트)
    public static final int EDGE_SIZE = 24;
}
