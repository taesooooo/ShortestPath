package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.IOException;
import java.util.ArrayList;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.text.cql2.CQLException;

import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

public interface Extractor {
	public DataStore getStore();
	public void extract() throws IOException, CQLException;
	default public void extract(ProgressStatus progressStatus) throws IOException, CQLException {
		extract();
	}
	public void createIndex() throws IOException;
}
 