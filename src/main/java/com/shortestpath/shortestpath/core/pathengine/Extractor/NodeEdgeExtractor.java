package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.MappableDataStore;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeEdgeExtractor implements Extractor {
	private File file;
	private DataStore store;
	private boolean saveToDb;

	public NodeEdgeExtractor(String filePath, DataStore dataStore) throws IOException {
		this(filePath, dataStore, false);
	}

	public NodeEdgeExtractor(String filePath, DataStore dataStore, boolean saveToDb) throws IOException {
		this.file = new File(filePath);
		if (!file.exists()) {
			// logger.error(filePath + " 위치에 파일이 존재 하지 않습니다.");
			throw new IOException(filePath + " 위치에 shp파일이 존재 하지 않습니다.");
		}

		this.store = dataStore;
		if (this.store == null) {
			throw new IllegalArgumentException("DataStore 객체는 null 일 수 없습니다.");
		}

		this.saveToDb = saveToDb;
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
		AtomicBoolean shouldContinue = new AtomicBoolean(true);

		List<Runnable> tasks = new ArrayList<>();
		tasks.add(new NodeCreator(collection, idArray, nodeCreatedArray, nodeEdgeQueue, store, progressStatus, shouldContinue));
		tasks.add(new EdgeCreator(store, idArray, nodeCreatedArray, lastEdgeOffsetArray, nodeEdgeQueue, progressStatus, shouldContinue));

		store.allocateNodeFileSpace((long) idArray.length * DataStructureSizes.NODE_SIZE);

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

		// 모든 워커 스레드가 종료되면 idArray를 이용해 indexList 생성
		ArrayList<IndexInfo> indexList = createIndexList(idArray);

		// DB 저장 여부 판단
		if (saveToDb) {
			// DB에 저장하는 경우
			saveIndex(indexList);
			log.info("노드 인덱스 DB 저장 완료");
		} else {
			// CSV 파일로 저장하는 경우
			String csvFilePath = file.getParentFile().toPath().resolve("node_index.csv").toString();
			NodeCSVWriter csvWriter = new NodeCSVWriter(csvFilePath, indexList);
			csvWriter.write();
			log.info("노드 CSV 저장 완료: {}", csvFilePath);
		}

		log.info("노드 및 엣지 추출 작업 완료");

		if(store instanceof MappableDataStore) {
			((MappableDataStore)store).switchToMappingMode();
		}

		shpStore.dispose();
	}

	private ArrayList<IndexInfo> createIndexList(long[] idArray) {
		ArrayList<IndexInfo> indexList = new ArrayList<>();
		for (int nodeId = 0; nodeId < idArray.length; nodeId++) {
			long coordinate = idArray[nodeId];
			int offset = nodeId * DataStructureSizes.NODE_SIZE;
			indexList.add(new IndexInfo(nodeId, coordinate, offset));
		}
		return indexList;
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

	private void saveIndex(ArrayList<IndexInfo> indexList) throws IOException {
		store.saveNodeIndex(indexList);
	}

	@Override
	public DataStore getStore() {
		return this.store;
	}
}
