package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.IOException;
import java.util.HashMap;

import org.locationtech.jts.index.strtree.STRtree;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

public interface Extractor {
	public DataStore getStore();
	public void extract() throws IOException;
	default public void extract(ProgressStatus progressStatus) throws IOException {
		extract();
	}
	public STRtree getRtree();
}
