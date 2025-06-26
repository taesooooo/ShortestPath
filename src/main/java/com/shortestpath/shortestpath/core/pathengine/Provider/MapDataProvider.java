package com.shortestpath.shortestpath.core.pathengine.Provider;

import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;

import com.shortestpath.shortestpath.core.pathengine.DataProvider;
import com.shortestpath.shortestpath.repository.MapRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MapDataProvider implements DataProvider {
	
	public final MapRepository mapRepository;

	@Override
	public List<Geometry> findNearestLine(double longitude, double latitude, double range) {
		return mapRepository.findNearestLine(longitude, latitude, range);
	}

}
