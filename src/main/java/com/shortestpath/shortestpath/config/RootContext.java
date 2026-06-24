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

	@Value("${findpath.search-buffer-pool-size:1}")
	private int searchBufferPoolSize;

	@Value("${findpath.hot-road-cache-mode:index-only}")
	private String hotRoadCacheMode;

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
		dataStore.setReverseEdgeIndex(new FileBasedEdgeIndex(new File(shpFileParent, "reverse_edge_index.bin").toPath()));
		Extractor extractor = new NodeEdgeExtractor(shpFilePath, dataStore, isNodeDbSave);
		Loader loader = new Loader(extractor);

		log.info("노드/엣지/인덱스 추출 상태를 확인합니다.");
		loader.extractData(true);

		dataStore.switchToMappingMode();
		dataStore.switchEdgeIndexToMappingMode();
		
		return new Engine(dataStore, dataProvider, searchBufferPoolSize, hotRoadCacheMode);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}
}
