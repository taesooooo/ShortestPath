package com.shortestpath.shortestpath;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.geolatte.geom.Geometry;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Node;

class LoaderTest {
	private Logger logger = LoggerFactory.getLogger(getClass());
	static Properties properties;	
	
	static String nodeFilePath;
	static String linkFilePath;
	
	@BeforeAll
	static void load() {
		properties = new Properties();
		Reader reader;
		try {
			reader = new InputStreamReader(LoaderTest.class.getClassLoader().getResourceAsStream("application-test.properties"), "UTF-8");
			properties.load(reader);
			nodeFilePath = properties.getProperty("findpath.node-shp-path");
			linkFilePath = properties.getProperty("findpath.link-shp-path");
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	@DisplayName("노드 파일 로드 테스트")
	void fileLoadTest() throws IOException {
		Loader loader = new Loader(nodeFilePath, linkFilePath);
		loader.loadNode();
		loader.loadEdge();

		int nodeCount = loader.getMapGraph().size();
		int expectedNodeCount = getNodeCount(nodeFilePath);

		assertThat(loader.getMapGraph()).isNotNull();
		assertThat(nodeCount).isEqualTo(expectedNodeCount);
	}

	@Test
	@DisplayName("노드가 서로 연결이 되어있는지")
	void nodeEdgeConnectionTest() throws IOException {
		Loader loader = new Loader(nodeFilePath, linkFilePath);
		loader.loadNode();
		loader.loadEdge();

		Node n1 = loader.getMapGraph().getNode(new Coordinate(33.2403307, 126.5624673));
		Node n2 = loader.getMapGraph().getNode(new Coordinate(33.2402282, 126.5630821));

		assertThat(n1).isNotNull();
		assertThat(n2).isNotNull();

		assertThat(n1.getEdge().containsKey(n2.getId())).isTrue();
		assertThat(n2.getEdge().containsKey(n1.getId())).isTrue();
	}

	private int getNodeCount(String filePath) throws IOException {
		Set<Node> nodeSet = new HashSet<Node>();
		
		File file = new File(nodeFilePath);
		if (!file.exists()) {
			logger.error(nodeFilePath + " 위치에 노드 파일이 존재 하지 않습니다.");
			return 0;
		}
		
		FileDataStore store = FileDataStoreFinder.getDataStore(file);
		SimpleFeatureCollection collection = store.getFeatureSource().getFeatures();
		SimpleFeatureIterator iterator = collection.features();

		while(iterator.hasNext()) {
			SimpleFeature feature = iterator.next();
			Point geo = (Point)feature.getDefaultGeometry();

			int id = Integer.parseInt(feature.getAttribute("id").toString());
			double x = geo.getCoordinate().x;
			double y = geo.getCoordinate().y;

			Node node = new Node();
			node.setId(id);
			node.setCoordinate(new Coordinate(y, x));

			nodeSet.add(node);
		}

		return nodeSet.size();

		// return 0;
	}
}
