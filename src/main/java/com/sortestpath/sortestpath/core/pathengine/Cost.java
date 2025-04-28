package com.sortestpath.sortestpath.core.pathengine;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Cost {
	private Node node;
	private double gCost = Double.MAX_VALUE;
	private double hCost;
	private double fCost;
}
