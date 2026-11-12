package com.shortestpath.shortestpath.dto;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.model.NodeIndex;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NodeIndexDto implements NodeIndex {

    private int id;
    private Coordinate coordinate;
    private int offset;
}
