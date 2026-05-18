package com.shortestpath.shortestpath.core.pathengine.Extractor.Sort;

import java.util.Comparator;

import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;

/**
 * Edge 정렬용 Comparator: to -> RoadLevel 순서
 */
public class EdgeToComparator implements Comparator<Edge> {

    @Override
    public int compare(Edge e1, Edge e2) {
        int cmp = Integer.compare(e1.getTo(), e2.getTo());
        if (cmp != 0) return cmp;

        return compareRoadLevel(e1.getRoadLevel(), e2.getRoadLevel());
    }

    private int compareRoadLevel(RoadLevel levelA, RoadLevel levelB) {
        if (levelA == levelB) return 0;
        if (levelA == null) return 1;
        if (levelB == null) return -1;

        return Integer.compare(levelA.ordinal(), levelB.ordinal());
    }
}
