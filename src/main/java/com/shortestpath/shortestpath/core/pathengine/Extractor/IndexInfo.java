package com.shortestpath.shortestpath.core.pathengine.Extractor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class IndexInfo {
    public int id;
    public int nodeIndex;
    public int lastEdgeIndex;
}
