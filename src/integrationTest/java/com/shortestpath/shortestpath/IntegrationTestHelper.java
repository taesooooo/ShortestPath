package com.shortestpath.shortestpath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;

public class IntegrationTestHelper {
    
    public static void deleteBinaryFiles(HybridDataStore dataStore) throws IOException {
        if (dataStore == null) return;
        
        String dir = dataStore.getFileDirectory();
        Files.deleteIfExists(Paths.get(dir, "node.bin"));
        Files.deleteIfExists(Paths.get(dir, "edge.bin"));
        Files.deleteIfExists(Paths.get(dir, "edge_index.bin"));
    }
}