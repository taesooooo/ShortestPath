package com.shortestpath.shortestpath.core.pathengine;

import org.locationtech.jts.geom.Geometry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Edge{
	private int id;
	private int from;
	private int to;
	private double distance;
	private int nextEdgeOffset;
	private int speed;
	private RoadLevel roadLevel;  // L0, L1, L2
	// private Geometry geometry;
	
}
