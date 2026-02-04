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
import com.shortestpath.shortestpath.core.pathengine.Store.DataPersistence;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;
@TestConfiguration
public class TestRootContext {
	
	@Value("${findpath.shp-path}")
	private String shpFilePath;

	@Bean
	public Engine pathEngine(NodeProvider dataProvider, DataPersistence dataPersistence) throws Exception {
		HybridDataStore dataStore = new HybridDataStore(new File(shpFilePath).getParent());
		dataStore.setPersistence(dataPersistence);
		Extractor extractor = new NodeEdgeExtractor(shpFilePath, dataStore, false);
		Loader loader = new Loader(extractor);

		if(!loader.isDataExtracted()) {

			loader.extractData(true);
			dataStore = new HybridDataStore(new File(shpFilePath).getParent(), true); // 읽기 전용 모드로 재생성
			dataStore.setPersistence(dataPersistence);
		}
		
		return new Engine(dataStore, dataProvider);
	}
}
