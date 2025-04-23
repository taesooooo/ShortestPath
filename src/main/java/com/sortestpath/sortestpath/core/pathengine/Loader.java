package com.sortestpath.sortestpath.core.pathengine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.index.strtree.STRtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Loader {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private DataStore store;
	private String typeName;
	private FeatureSource<SimpleFeatureType, SimpleFeature> source;
	
	public Loader(String filePath) throws IOException {
		File file = new File(filePath);
		if(!file.exists()) {
			throw new FileNotFoundException(filePath + " 위치에 shp파일이 존재 하지 않습니다.");
		}
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("url", file.toURI().toURL());

		this.store = DataStoreFinder.getDataStore(map);
		this.typeName = store.getTypeNames()[0];
		
		this.source = store.getFeatureSource(typeName);
	}

	public Graph loadData() throws IOException {
		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
		FeatureIterator<SimpleFeature> iterator = collection.features();

		Graph graph = new Graph();
		long st = System.currentTimeMillis();
		int num = 0;
		
		while (iterator.hasNext()) {
			SimpleFeature feature = iterator.next();

//			int id = Integer.parseInt(feature.getAttribute("id").toString());
//			String fclass = feature.getAttribute("fclass").toString();
			MultiLineString multiLine = (MultiLineString) feature.getAttribute("the_geom");
			
			Node previousNode = null;
			
			for (int i = 0; i < multiLine.getNumPoints(); i++) {
				double x = multiLine.getCoordinates()[i].getX();
				double y = multiLine.getCoordinates()[i].getY();

				Node node = new Node();
				node.setId(num++);
//				node.setCategory(fclass);
				node.setCoordinate(new Coordinate(y, x));

				if (!graph.containsKey(node.getCoordinate())) {
					graph.addNode(node);
				}
				else {
					node = graph.getNode(node.getCoordinate());
					if(previousNode != null) {
						graph.addEdge(previousNode, node);
					}
				}

				if (previousNode != null) {
					graph.addEdge(previousNode, node);
				}
				
				previousNode = node;
			}
		}
		
		long et = System.currentTimeMillis();
		double rt = (et - st) / 1000.0;

		logger.info("FileLoad Excution Time - " + rt + "s");
		
		return graph;
	}
	
	public STRtree loadRtree() throws IOException {
		STRtree rtree = new STRtree();
		
		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
		FeatureIterator<SimpleFeature> iterator = collection.features();
		
		while(iterator.hasNext()) {
			SimpleFeature feature = iterator.next();
			MultiLineString geo = (MultiLineString) feature.getDefaultGeometry();
			
			for(int i=0;i< geo.getNumGeometries();i++) {
				LineString line = (LineString)geo.getGeometryN(i);
				rtree.insert(line.getEnvelopeInternal(), line);
			}
		}
		
		return rtree;
	}
}
