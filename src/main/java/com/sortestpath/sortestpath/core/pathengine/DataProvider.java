package com.sortestpath.sortestpath.core.pathengine;

import java.util.List;

import org.locationtech.jts.geom.Geometry;

public interface DataProvider {
	public List<Geometry> findNearestLine(double longitude, double latitude, double range);
}
