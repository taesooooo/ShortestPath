package com.shortestpath.shortestpath;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;
@TestConfiguration
public class TestRootContext {
	
	@Value("${findpath.shp-path}")
	private String shpFilePath;

	@Bean
	public Engine pathEngine(NodeProvider dataProvider, NodeProvider nodeIndexProvider) throws Exception {
		HybridDataStore dataStore = new HybridDataStore(new File(shpFilePath).getParent(), nodeIndexProvider);
		Extractor extractor = new NodeEdgeExtractor(shpFilePath, dataStore, false);
		Loader loader = new Loader(extractor);

		if(!loader.isDataExtracted()) {

			loader.extractData();
			dataStore = new HybridDataStore(new File(shpFilePath).getParent(), nodeIndexProvider);
		}
		
		return new Engine(dataStore, dataProvider);
	}
}
