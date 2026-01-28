package com.shortestpath.shortestpath;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.DataPersistence;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;

@TestConfiguration
@Profile("inte")
public class TestConfig {
    @Value("${findpath.shp-path}")
	private String shpFilePath;
    
    @Bean
    public DataStore dataStore(DataPersistence dataPersistence) throws Exception {
        File nodeFile = new File(new File(shpFilePath).getParent(), "node.bin");
        File edgeFile = new File(new File(shpFilePath).getParent(), "edge.bin");
        if (nodeFile.exists()) {
            nodeFile.delete();
        }
        if (edgeFile.exists()) {
            edgeFile.delete();
        }

        HybridDataStore dataStore = new HybridDataStore(new File(shpFilePath).getParent());
        dataStore.setPersistence(dataPersistence);
        
        return dataStore;
    }

    @Bean
    public Extractor extractor(DataStore dataStore) throws IOException {
        return new NodeEdgeExtractor(shpFilePath, dataStore, true);
    }

    @Bean
    public Loader loader(Extractor extractor, DataStore dataStore) throws IOException {
        Loader loader = new Loader(extractor);
        return loader;
    }

    @Bean
    public Engine engine(DataStore dataStore, NodeProvider nodeIndexProvider) throws IOException {
        return new Engine(dataStore, nodeIndexProvider);
    }
}
