package com.shortestpath.shortestpath.config;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shortestpath.shortestpath.common.resolver.PageInfoArgumentResolver;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.DataPersistence;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.FileBasedEdgeIndex;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RootContext implements WebMvcConfigurer {
	
	@Value("${findpath.shp-path}")
	private String shpFilePath;

	@Value("${findpath.node-db-save}")
	private boolean isNodeDbSave;

	@Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PageInfoArgumentResolver());
    }

	@Bean
	public Engine pathEngine(NodeProvider dataProvider, DataPersistence dataPersistence, NodeProvider nodeIndexProvider) throws Exception {
		String shpFileParent = new File(shpFilePath).getParent();
		HybridDataStore dataStore = new HybridDataStore(shpFileParent);
		dataStore.setPersistence(dataPersistence);
		dataStore.setEdgeIndex(new FileBasedEdgeIndex(shpFileParent));
		Extractor extractor = new NodeEdgeExtractor(shpFilePath, dataStore, isNodeDbSave);
		Loader loader = new Loader(extractor);

		if(!loader.isDataExtracted()) {
			log.info("추출된 노드 및 엣지 데이터가 없으므로 추출을 시작합니다.");

			loader.extractData(true);
			dataStore.switchToMappingMode();
			// dataStore = new HybridDataStore(new File(shpFilePath).getParent(), true); // 읽기 전용 모드로 재생성
		}
		
		return new Engine(dataStore, dataProvider);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}
}
