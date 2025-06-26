package com.shortestpath.shortestpath.config;

import java.io.IOException;

import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.shortestpath.shortestpath.core.pathengine.DataProvider;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Graph;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.repository.MapRepository;

@Configuration
public class RootContext {
	
	@Value("${findpath.shp-path}")
	private String filePath;
	private final DataProvider dataProvider;
	
	
	public RootContext(DataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}

	@Bean
	public Engine pathEngine() throws IOException {
		Loader loader = new Loader(filePath);
		return new Engine(loader, dataProvider);
	}
}
