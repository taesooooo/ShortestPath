package com.sortestpath.sortestpath.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sortestpath.sortestpath.core.pathengine.Engine;
import com.sortestpath.sortestpath.core.pathengine.Graph;
import com.sortestpath.sortestpath.core.pathengine.Loader;

@Configuration
public class RootContext {
	
	@Value("${findpath.shp-path}")
	String filePath;

	@Bean
	public Engine pathEngine() throws IOException {
		Loader loader = new Loader(filePath);
		return new Engine(loader);
	}
}
