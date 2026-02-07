package com.shortestpath.shortestpath.core.pathengine.Extractor.Sort;

import java.util.Comparator;

import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;

/**
 * Edge 정렬용 Comparator: from -> RoadLevel 순서
 * 
 * from 노드 ID로 먼저 정렬하고,
 * from이 같으면 RoadLevel 순서(L0 < L1 < L2)로 정렬
 */
public class EdgeComparator implements Comparator<Edge> {
    
    @Override
    public int compare(Edge e1, Edge e2) {
        // from 비교
        int cmp = Integer.compare(e1.getFrom(), e2.getFrom());
        if (cmp != 0) return cmp;
        
        // from이 같으면 RoadLevel 비교
        return compareRoadLevel(e1.getRoadLevel(), e2.getRoadLevel());
    }
    
    private int compareRoadLevel(RoadLevel levelA, RoadLevel levelB) {
        if (levelA == levelB) return 0;
        if (levelA == null) return 1;
        if (levelB == null) return -1;
        
        return Integer.compare(levelA.ordinal(), levelB.ordinal());
    }
}
