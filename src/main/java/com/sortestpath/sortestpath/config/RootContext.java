package com.sortestpath.sortestpath.config;

import java.io.IOException;

import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sortestpath.sortestpath.core.pathengine.DataProvider;
import com.sortestpath.sortestpath.core.pathengine.Engine;
import com.sortestpath.sortestpath.core.pathengine.Graph;
import com.sortestpath.sortestpath.core.pathengine.Loader;
import com.sortestpath.sortestpath.repository.MapRepository;

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
