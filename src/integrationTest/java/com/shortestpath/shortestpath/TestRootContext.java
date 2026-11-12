package com.shortestpath.shortestpath;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.NodeDataPersistence;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.FileBasedEdgeIndex;

@TestConfiguration
public class TestRootContext {
	private static final Logger log = LoggerFactory.getLogger(TestRootContext.class);
	
	@Value("${findpath.shp-path}")
	private String shpFilePath;

	@Value("${findpath.node-db-save}")
	private boolean isNodeDbSave;

	@Value("${findpath.search-buffer-pool-size:1}")
	private int searchBufferPoolSize;


	// 공유 컨텍스트 사용 후 DB 삭제시 사용
	// @Autowired
	// private JdbcTemplate jdbcTemplate;
	// @Autowired
	// private Engine engine;

	// @Override
    // public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    //     resolvers.add(new PageInfoArgumentResolver());
    // }
	@Bean
	public HybridDataStore testDataStore(NodeDataPersistence dataPersistence) throws IOException {
		String shpFileParent = new File(shpFilePath).getParent();
		HybridDataStore dataStore = new HybridDataStore(shpFileParent);
		dataStore.setPersistence(dataPersistence);
		dataStore.setEdgeIndex(new FileBasedEdgeIndex(shpFileParent));
		dataStore.setReverseEdgeIndex(new FileBasedEdgeIndex(new File(shpFileParent, "reverse_edge_index.bin").toPath()));

		return dataStore;
	}

	@Bean
	public Loader testLoader(DataStore dataStore) throws IOException {
		Extractor extractor = new NodeEdgeExtractor(shpFilePath, dataStore, isNodeDbSave);
		Loader loader = new Loader(extractor);
		return loader;
	}

	@Bean
	@Primary
	public Engine testPathEngine(HybridDataStore dataStore, Loader loader, NodeDataPersistence dataPersistence) throws Exception {

		log.info("노드/엣지/인덱스 추출 상태를 확인합니다.");
		loader.extractData(false);

		dataStore.switchToMappingMode();
		dataStore.switchEdgeIndexToMappingMode();
		
		return new Engine(dataStore, dataPersistence, searchBufferPoolSize);
	}

	@Bean
	@Primary
	public ObjectMapper testObjectMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}
}