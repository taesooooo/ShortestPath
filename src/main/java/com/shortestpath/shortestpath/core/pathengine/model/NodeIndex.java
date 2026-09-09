package com.shortestpath.shortestpath.core.pathengine.model;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;

public interface NodeIndex {

    int getId();

    Coordinate getCoordinate();

    int getOffset();
}
