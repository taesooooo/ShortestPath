package com.shortestpath.shortestpath.core.pathengine.Provider;

import java.util.List;

import com.shortestpath.shortestpath.entity.GeoLink;

public interface DataProvider {
	public List<GeoLink> findNearestLine(double longitude, double latitude, double range);
}
