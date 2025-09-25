package com.shortestpath.shortestpath.core.pathengine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

public class Loader {
	private Logger logger = LoggerFactory.getLogger(getClass());

	private String nodeFilePath;
	private String linkFilePath;
	private File file;
	private DataStore store;
	@Getter
	private Graph mapGraph = new Graph();

	public Loader(String nodeFilePath, String linkFilePath) throws IOException {
		// file = new File(filePath);
		// if (!file.exists()) {
		// 	throw new FileNotFoundException(filePath + " 위치에 shp파일이 존재 하지 않습니다.");
		// }

		// Map<String, Object> map = new HashMap<String, Object>();
		// map.put("url", file.toURI().toURL());

		// store = DataStoreFinder.getDataStore(map);
		this.nodeFilePath = nodeFilePath;
		this.linkFilePath = linkFilePath;
	}

	// public Graph loadData() throws IOException {
	// 	FeatureSource<SimpleFeatureType, SimpleFeature> source = getSource();

	// 	FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
	// 	FeatureIterator<SimpleFeature> iterator = collection.features();

	// 	Graph graph = new Graph();
	// 	long st = System.currentTimeMillis();
	// 	int num = 0;

	// 	while (iterator.hasNext()) {
	// 		SimpleFeature feature = iterator.next();

	// 		// int id = Integer.parseInt(feature.getAttribute("id").toString());
	// 		// String fclass = feature.getAttribute("fclass").toString();
	// 		MultiLineString multiLine = (MultiLineString) feature.getAttribute("the_geom");

	// 		Node previousNode = null;

	// 		for (int i = 0; i < multiLine.getNumPoints(); i++) {
	// 			double x = multiLine.getCoordinates()[i].getX();
	// 			double y = multiLine.getCoordinates()[i].getY();

	// 			Node node = new Node();
	// 			node.setId(num++);
	// 			// node.setCategory(fclass);
	// 			node.setCoordinate(new Coordinate(y, x));

	// 			if (!graph.containsKey(node.getCoordinate())) {
	// 				graph.addNode(node);
	// 			} else {
	// 				node = graph.getNode(node.getCoordinate());
	// 				if (previousNode != null) {
	// 					graph.addEdge(previousNode, node);
	// 				}
	// 			}

	// 			if (previousNode != null) {
	// 				graph.addEdge(previousNode, node);
	// 			}

	// 			previousNode = node;
	// 		}
	// 	}

	// 	long et = System.currentTimeMillis();
	// 	double rt = (et - st) / 1000.0;

	// 	logger.info("FileLoad Excution Time - " + rt + "s");

	// 	iterator.close();

	// 	return graph;
	// }

	public void loadNode() {
		File file = new File(nodeFilePath);
		if (!file.exists()) {
			logger.error(nodeFilePath + " 위치에 노드 파일이 존재 하지 않습니다.");
			return;
		}

		FileDataStore nodeStore = null;

		try {
			nodeStore = FileDataStoreFinder.getDataStore(file);
			FeatureSource<SimpleFeatureType, SimpleFeature> source = nodeStore.getFeatureSource(nodeStore.getTypeNames()[0]);
			FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();

			try (FeatureIterator<SimpleFeature> iterator = collection.features()) {
		
				while (iterator.hasNext()) {
					SimpleFeature feature = iterator.next();
					Point geo = (Point)feature.getDefaultGeometry();

					int id = Integer.parseInt(feature.getAttribute("id").toString());
					double x = geo.getCoordinate().x;
					double y = geo.getCoordinate().y;

					Node node = new Node();
					node.setId(id);
					node.setCoordinate(new Coordinate(y, x));

					mapGraph.addNode(node);
				}
			}
			
			logger.info("노드 데이터 로드 완료");
			
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("노드 파일 로드 중 오류가 발생 했습니다.", e);
		} finally {
			if (nodeStore != null) {
				nodeStore.dispose();
			}
		}
	}

	public void loadEdge() {
		File file = new File(linkFilePath);
		if (!file.exists()) {
			logger.error(linkFilePath + " 위치에 링크 파일이 존재 하지 않습니다.");
			return;
		}

		FileDataStore edgeStore = null;

		try {
			edgeStore = FileDataStoreFinder.getDataStore(file);
			FeatureSource<SimpleFeatureType, SimpleFeature> source = edgeStore.getFeatureSource(edgeStore.getTypeNames()[0]);
			FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();

			try (FeatureIterator<SimpleFeature> iterator = collection.features()) {
		
				while (iterator.hasNext()) {
					SimpleFeature feature = iterator.next();
					Geometry geo = (Geometry)feature.getDefaultGeometry();
//					double s_x = Integer.parseInt(feature.getAttribute("start_x").toString());
//					double s_y = Integer.parseInt(feature.getAttribute("start_y").toString());
//					double e_x = Integer.parseInt(feature.getAttribute("end_x").toString());
//					double e_y = Integer.parseInt(feature.getAttribute("end_y").toString());
					
					double s_x = (double)feature.getAttribute("start_x");
					double s_y = (double)feature.getAttribute("start_y");
					double e_x = (double)feature.getAttribute("end_x");
					double e_y = (double)feature.getAttribute("end_y");

					mapGraph.addEdge(new Coordinate(s_y, s_x), new Coordinate(e_y, e_x), geo);
				}
			}
			
			logger.info("링크 데이터 로드 완료");
			
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("링크 파일 로드 중 오류가 발생 했습니다.", e);
		} finally {
			if (edgeStore != null) {
				edgeStore.dispose();
			}
		}
	}

	// public STRtree loadRtree() throws IOException {
	// 	STRtree rtree = new STRtree();
		
	// 	FeatureSource<SimpleFeatureType, SimpleFeature> source = getSource();
		
	// 	FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
	// 	FeatureIterator<SimpleFeature> iterator = collection.features();
		
	// 	while(iterator.hasNext()) {
	// 		SimpleFeature feature = iterator.next();
	// 		MultiLineString geo = (MultiLineString) feature.getDefaultGeometry();
			
	// 		for(int i=0;i< geo.getNumGeometries();i++) {
	// 			LineString line = (LineString)geo.getGeometryN(i);
	// 			rtree.insert(line.getEnvelopeInternal(), line);
	// 		}
	// 	}
		
	// 	rtree.build();
		
	// 	return rtree;
	// }

	private FeatureSource<SimpleFeatureType, SimpleFeature> getSource() throws IOException {
		String typeName = store.getTypeNames()[0];
		FeatureSource<SimpleFeatureType, SimpleFeature> source = store.getFeatureSource(typeName);

		return source;
	}

	public void dispose() {
		if (store != null) {
			store.dispose();
		}
	}
}
