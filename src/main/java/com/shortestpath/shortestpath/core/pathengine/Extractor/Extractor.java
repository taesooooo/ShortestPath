package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.IOException;

import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

public interface Extractor {
	public DataStore getStore();
	public void extract() throws IOException;
	default public void extract(ProgressStatus progressStatus) throws IOException {
		extract();
	}
}
