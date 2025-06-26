package com.shortestpath.shortestpath.core.pathengine;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Edge{
	private Node to;
	private double distance;
}
