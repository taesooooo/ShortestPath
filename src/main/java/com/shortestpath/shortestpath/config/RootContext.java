package com.shortestpath.shortestpath.config;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.DataProvider;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Store.FileDataStore;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RootContext {
	
	@Value("${findpath.shp-path}")
	private String shpFilePath;
	private final DataProvider dataProvider;
	
	
	public RootContext(DataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}

	@Bean
	public Engine pathEngine() throws Exception {
		FileDataStore dataStore = new FileDataStore(new File(shpFilePath).getParent());
		Extractor extractor = new NodeEdgeExtractor(shpFilePath, dataStore);
		Loader loader = new Loader(extractor);

		if(!loader.isDataExtracted()) {
			log.info("추출된 노드 및 엣지 데이터가 없으므로 추출을 시작합니다.");

			loader.extractData();
			dataStore = new FileDataStore(new File(shpFilePath).getParent());
		}
		
		return new Engine(dataStore, dataProvider);
	}
}
