package com.shortestpath.shortestpath.provider;

import java.util.List;

import org.springframework.stereotype.Component;

import com.shortestpath.shortestpath.core.pathengine.Provider.DataProvider;
import com.shortestpath.shortestpath.entity.GeoLink;
import com.shortestpath.shortestpath.repository.MapRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JapMapDataProvider implements DataProvider {
	
	public final MapRepository mapRepository;

	@Override
	public List<GeoLink> findNearestLine(double longitude, double latitude, double range) {
		return mapRepository.findNearestLine(longitude, latitude, range);
	}

}
