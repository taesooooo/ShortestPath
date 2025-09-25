package com.shortestpath.shortestpath.core.pathengine;

import java.util.List;

import org.locationtech.jts.geom.Geometry;

import com.shortestpath.shortestpath.entity.GeoLink;

public interface DataProvider {
	public List<GeoLink> findNearestLine(double longitude, double latitude, double range);
}
