package com.sortestpath.sortestpath;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.index.strtree.STRtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sortestpath.sortestpath.core.pathengine.Graph;
import com.sortestpath.sortestpath.core.pathengine.Loader;

class LoaderTest {
	private Logger logger = LoggerFactory.getLogger(getClass());
	static Properties properties;	
	
	@BeforeAll
	static void load() {
		properties = new Properties();
		Reader reader;
		try {
			reader = new InputStreamReader(LoaderTest.class.getClassLoader().getResourceAsStream("application-test.properties"), "UTF-8");
			properties.load(reader);
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
	void fileLoadTest() throws IOException {
		String filePath = properties.getProperty("project.find-path.test-file-path");
		Loader loader = new Loader(filePath);
		Graph g = loader.loadData();
		
		assertThat(g).isNotNull();
	}
	
	@Test
	void SRTreeLoadTest() throws IOException {
		String filePath = properties.getProperty("project.find-path.test-file-path");
		Loader loader = new Loader(filePath);
		STRtree stRtree = loader.loadRtree();
		
		assertThat(stRtree.isEmpty()).isFalse();
	}

}
