package com.shortestpath.shortestpath.core.pathengine.Store;

import java.io.IOException;

public interface MappableDataStore extends DataStore{
    void switchToMappingMode() throws IOException;
}