package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;

import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeEdgeExtractor implements Extractor {
	private File file;
	private DataStore store;

	public NodeEdgeExtractor(String filePath, DataStore dataStore) throws IOException {
		this.file = new File(filePath);
		if (!file.exists()) {
			// logger.error(filePath + " 위치에 파일이 존재 하지 않습니다.");
			throw new IOException(filePath + " 위치에 shp파일이 존재 하지 않습니다.");
		}

		this.store = dataStore;
		if (this.store == null) {
			throw new IllegalArgumentException("DataStore 객체는 null 일 수 없습니다.");
		}

	}

	@Override
	public void extract() throws IOException {
		doExtract(null);
	}

	@Override
	public void extract(ProgressStatus progressStatus) throws IOException {
		doExtract(progressStatus);
	}

	private void doExtract(ProgressStatus progressStatus) throws IOException {
		log.info("노드 및 엣지 추출 및 저장 시작");
		FileDataStore shpStore = null;
		shpStore = FileDataStoreFinder.getDataStore(file);
		FeatureSource<SimpleFeatureType, SimpleFeature> source = shpStore.getFeatureSource(shpStore.getTypeNames()[0]);
		// Filter filter = CQL
		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();

		long[] idArray = createIdArray(collection);
		boolean[] nodeCreatedArray = new boolean[idArray.length];
		int[] lastEdgeOffsetArray = new int[idArray.length];
		Arrays.fill(lastEdgeOffsetArray, -1);

		BlockingQueue<TaskItem> nodeEdgeQueue = new LinkedBlockingQueue<TaskItem>(2000);
		BlockingQueue<TaskItem> csvQueue = new LinkedBlockingQueue<TaskItem>(2000);
		AtomicBoolean shouldContinue = new AtomicBoolean(true);

		List<Runnable> tasks = Arrays.asList(
			new NodeEdgeCreator(collection, idArray, nodeCreatedArray, nodeEdgeQueue, progressStatus, shouldContinue),
			new NodeEdgeSave(store, idArray, nodeCreatedArray, lastEdgeOffsetArray, nodeEdgeQueue, csvQueue, progressStatus, shouldContinue),
			new NodeCSVWriter(csvQueue, file.toPath().getParent().toString(), progressStatus, idArray.length, shouldContinue)
		);
		List<Thread> workers = tasks.stream().map((task) -> {
			return new Thread(task, task.getClass().getSimpleName());
		}).toList();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			workers.forEach(t -> t.interrupt());
		}));

		workers.forEach(Thread::start);
		workers.forEach(t -> {
			try {
				t.join();
			} catch (InterruptedException e) {
				t.interrupt();
				e.printStackTrace();
			}
		});

		log.info("노드 인덱스 CSV파일로 저장되었습니다.");
	}

	private long[] createIdArray(FeatureCollection<SimpleFeatureType, SimpleFeature> collection) throws IOException {
		log.info("내부 인덱스 배열 생성중.");
		long[] nodeIdArray = new long[100000];
		int count = 0;

		FeatureIterator<SimpleFeature> iterator = collection.features();

		while (iterator.hasNext()) {
			SimpleFeature feature = iterator.next();
			Geometry geo = (Geometry) feature.getDefaultGeometry();

			for (org.locationtech.jts.geom.Coordinate coordinate : geo.getCoordinates()) {
				long coordinateId = GeometryUtil.coordinateToLong(coordinate);
				if(count == nodeIdArray.length) {
					nodeIdArray = Arrays.copyOf(nodeIdArray, nodeIdArray.length * 2);
				}
				
				nodeIdArray[count++] = coordinateId;
				
			}
		}

		Arrays.sort(nodeIdArray);

		// 중복 제거
		int tempIndex = 0;
		for(int i=0; i<nodeIdArray.length; i++) {
			if(i == 0 || nodeIdArray[i] != nodeIdArray[i - 1]) {
				nodeIdArray[tempIndex] = nodeIdArray[i];
				tempIndex++;
			}
		}

		return Arrays.copyOf(nodeIdArray, tempIndex);
	}

	@Override
	public DataStore getStore() {
		return this.store;
	}
}
