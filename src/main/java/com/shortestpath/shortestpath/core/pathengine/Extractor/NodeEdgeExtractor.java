package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.Query;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.locationtech.jts.geom.Geometry;


import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Sort.EdgeSort;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.EdgeHeader;
import com.shortestpath.shortestpath.core.pathengine.Store.NodeHeader;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeEdgeExtractor implements Extractor {
	private File file;
	private DataStore store;
	private FileDataStore shpStore = null;
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

		this.shpStore = FileDataStoreFinder.getDataStore(file);

		this.saveToDb = saveToDb;
	}

	@Override
	public void extract() throws IOException, CQLException {
		doExtract(null);
	}

	@Override
	public void extract(ProgressStatus progressStatus) throws IOException, CQLException {
		doExtract(progressStatus);
	}

	private void doExtract(ProgressStatus progressStatus) throws IOException, CQLException {
		log.info("노드 및 엣지 추출 및 저장 시작");
		FeatureSource<SimpleFeatureType, SimpleFeature> source = shpStore.getFeatureSource(shpStore.getTypeNames()[0]);
		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();

		int featureCount = source.getCount(Query.ALL);
		
		// featureCount가 0이면 작업을 수행하지 않고 종료
		if (featureCount == 0) {
			log.warn("피쳐가 존재하지 않습니다. 작업을 수행하지 않습니다.");
			shpStore.dispose();
			return;
		}

		boolean nodeTaskCompleted = isNodeTaskCompleted();
		boolean edgeTaskCompleted = isEdgeTaskCompleted();
		boolean edgeIndexTaskCompleted = store.isEdgeIndexTaskCompleted();
		boolean reverseEdgeIndexTaskCompleted = store.isReverseEdgeIndexTaskCompleted();
		Path csvFilePath = file.getParentFile().toPath().resolve("node_index.csv");
		boolean nodeIndexOutputCompleted = saveToDb || Files.exists(csvFilePath);

		if (nodeTaskCompleted
				&& edgeTaskCompleted
				&& edgeIndexTaskCompleted
				&& reverseEdgeIndexTaskCompleted
				&& nodeIndexOutputCompleted) {
			log.info("노드/엣지/인덱스 추출 작업이 모두 완료되어 건너뜁니다.");
			shpStore.dispose();
			return;
		}

		long[] idArray = createIdArray(collection);
		boolean[] nodeCreatedArray = new boolean[idArray.length];
		int[] lastEdgeOffsetArray = new int[idArray.length];
		Arrays.fill(lastEdgeOffsetArray, -1);

		BlockingQueue<List<TaskItem>> nodeQueue = new LinkedBlockingQueue<List<TaskItem>>(2000);
		BlockingQueue<List<TaskItem>> edgeQueue = new LinkedBlockingQueue<List<TaskItem>>(2000);
		AtomicBoolean taskContinue = new AtomicBoolean(true); // 모든 작업 스레드가 공유하는 플래그
		AtomicBoolean taskError = new AtomicBoolean(false); // 작업 중 예외 발생 여부 플래그

		int threadCount = 4;

		boolean nodeRebuilt = false;
		if (!nodeTaskCompleted || !isNodeTaskCompleted(idArray.length)) {
			log.info("노드 작업을 시작합니다. 기존 파일이 있어도 taskCompleted=false이면 이 단계부터 다시 생성합니다.");
			store.truncateNodeFile(0);
			store.writeNodeHeader(new NodeHeader(idArray.length, false, false));
			store.allocateNodeFileSpace((long) idArray.length * DataStructureSizes.NODE_SIZE);

			ExecutorService nodeExecutorService = Executors.newFixedThreadPool(threadCount + 1);
			AtomicInteger totalSavedNodeCount = new AtomicInteger(0);  // 멀티스레드 환경에서 안전하게 노드 개수 집계
			
			Thread shutdownHook = new Thread(() -> {
				log.warn("종료 신호 감지됨. ExecutorService를 종료합니다...");
				nodeExecutorService.shutdownNow();
			});
			Runtime.getRuntime().addShutdownHook(shutdownHook);
			
			nodeExecutorService.submit(new NodeExtract(idArray, nodeQueue, collection, taskContinue, taskError, progressStatus, threadCount));
			for(int i=0; i<threadCount; i++) {
				nodeExecutorService.submit(new NodeSaver(nodeQueue, nodeCreatedArray, store, progressStatus, taskContinue, taskError, totalSavedNodeCount));
			}

			nodeExecutorService.shutdown();
			try {
				while(!nodeExecutorService.awaitTermination(1, TimeUnit.MINUTES)) {}
				Runtime.getRuntime().removeShutdownHook(shutdownHook);

				taskContinue.set(true);
				
				if (taskError.get()) {
					log.error("노드 추출 중 예외 발생으로 작업 중단");
					return;
				}

				store.truncateNodeFile(DataStructureSizes.HEADER_SIZE + ((long) idArray.length * DataStructureSizes.NODE_SIZE));
				store.writeNodeHeader(new NodeHeader(idArray.length, false, true));
				nodeRebuilt = true;
			} catch (InterruptedException e) {
				log.error("작업 대기 중 인터럽트 발생", e);
				nodeExecutorService.shutdownNow();
				Thread.currentThread().interrupt();
				return;
			}
		} else {
			log.info("노드 작업이 이미 완료되어 건너뜁니다.");
		}

		boolean edgeRebuilt = false;
		if (nodeRebuilt || !edgeTaskCompleted) {
			log.info("엣지 작업을 시작합니다. 기존 파일이 있어도 taskCompleted=false이면 이 단계부터 다시 생성합니다.");
			store.truncateEdgeFile(0);
			store.writeEdgeHeader(new EdgeHeader(0, false, false));
			taskContinue.set(true);
			taskError.set(false);

			ExecutorService edgeExecutorService = Executors.newFixedThreadPool(threadCount + 1);
			AtomicInteger totalSavedEdgeCount = new AtomicInteger(0);  // 멀티스레드 환경에서 안전하게 엣지 개수 집계
			
			Thread edgeShutdownHook = new Thread(() -> {
				log.warn("종료 신호 감지됨. ExecutorService를 종료합니다...");
				edgeExecutorService.shutdownNow();
			});
			Runtime.getRuntime().addShutdownHook(edgeShutdownHook);

			edgeExecutorService.submit(new EdgeExtract(idArray, edgeQueue, store, collection, taskContinue, taskError, progressStatus, threadCount));
			for(int i=0; i<threadCount; i++) {
				edgeExecutorService.submit(new EdgeSaver(edgeQueue, store, progressStatus, taskContinue, taskError, totalSavedEdgeCount));
			}

			edgeExecutorService.shutdown();
			try {
				while(!edgeExecutorService.awaitTermination(1, TimeUnit.MINUTES)) {}
				Runtime.getRuntime().removeShutdownHook(edgeShutdownHook);

				store.truncateEdgeFile(DataStructureSizes.HEADER_SIZE + ((long) store.getTotalEdges() * DataStructureSizes.EDGE_SIZE));

				taskContinue.set(true);
				
				if (taskError.get()) {
					log.error("엣지 추출 중 예외 발생으로 작업 중단");
					return;
				}
			} catch (InterruptedException e) {
				log.error("작업 대기 중 인터럽트 발생", e);
				edgeExecutorService.shutdownNow();
				Thread.currentThread().interrupt();
				return;
			}

			log.info("엣지 정렬 시작");
			EdgeSort edgeSort = new EdgeSort(store);
			edgeSort.sort();
			edgeRebuilt = true;
		} else {
			log.info("엣지 작업이 이미 완료되어 건너뜁니다.");
		}

		EdgeIndexCreator edgeIndexCreator = new EdgeIndexCreator(store);
		if (edgeRebuilt || !edgeIndexTaskCompleted) {
			log.info("엣지 인덱스 생성 시작");
			edgeIndexCreator.createEdgeIndex();
		} else {
			log.info("엣지 인덱스 작업이 이미 완료되어 건너뜁니다.");
		}

		if (edgeRebuilt || !reverseEdgeIndexTaskCompleted) {
			log.info("리버스 엣지 인덱스 생성 시작");
			edgeIndexCreator.createReverseEdgeIndex();
		} else {
			log.info("리버스 엣지 인덱스 작업이 이미 완료되어 건너뜁니다.");
		}
		
		ArrayList<IndexInfo> indexList = createIndexList(idArray);

		if (saveToDb) {
			saveIndex(indexList);
			log.info("노드 인덱스 DB 저장 완료");
		} else {
			if(!Files.exists(csvFilePath)) {
				NodeCSVWriter csvWriter = new NodeCSVWriter(csvFilePath.toString(), indexList);
				csvWriter.write();
				log.info("노드 CSV 저장 완료: {}", csvFilePath);
			}
			else {
				log.info("노드 CSV 파일이 있어 건너 뜁니다.");
			}
		}

		log.info("노드 및 엣지 추출 작업 완료");
		shpStore.dispose();
	}

	@Override
	public void createIndex() throws IOException {
		FeatureSource<SimpleFeatureType, SimpleFeature> source = shpStore.getFeatureSource(shpStore.getTypeNames()[0]);
		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
		long[] idArray = createIdArray(collection);
		ArrayList<IndexInfo> indexList = createIndexList(idArray);

		saveIndex(indexList);
	}

	private boolean isNodeTaskCompleted(int expectedNodeCount) {
		try {
			NodeHeader header = store.readNodeHeader();
			boolean completed = header.isTaskCompleted() && header.getNodeCount() == expectedNodeCount;
			if (!completed) {
				log.info("노드 헤더 확인 - nodeCount: {}, expected: {}, taskCompleted: {}",
						header.getNodeCount(), expectedNodeCount, header.isTaskCompleted());
			}
			return completed;
		} catch (Exception e) {
			log.info("노드 헤더를 읽을 수 없어 노드 작업부터 다시 시작합니다. reason: {}", e.getMessage());
			return false;
		}
	}

	private boolean isNodeTaskCompleted() {
		try {
			NodeHeader header = store.readNodeHeader();
			if (!header.isTaskCompleted()) {
				log.info("노드 헤더 확인 - nodeCount: {}, taskCompleted: {}",
						header.getNodeCount(), header.isTaskCompleted());
			}
			return header.isTaskCompleted();
		} catch (Exception e) {
			log.info("노드 헤더를 읽을 수 없어 노드 작업이 필요합니다. reason: {}", e.getMessage());
			return false;
		}
	}

	private boolean isEdgeTaskCompleted() {
		try {
			EdgeHeader edgeHeader = store.readEdgeHeader();
			EdgeHeader reverseEdgeHeader = store.readReverseEdgeHeader();
			boolean completed = edgeHeader.isTaskCompleted()
					&& reverseEdgeHeader.isTaskCompleted()
					&& edgeHeader.isSorted()
					&& reverseEdgeHeader.isSorted()
					&& edgeHeader.getEdgeCount() == reverseEdgeHeader.getEdgeCount();
			if (!completed) {
				log.info("엣지 헤더 확인 - edgeCount: {}, reverseEdgeCount: {}, edgeTask: {}, reverseTask: {}, edgeSorted: {}, reverseSorted: {}",
						edgeHeader.getEdgeCount(), reverseEdgeHeader.getEdgeCount(), edgeHeader.isTaskCompleted(),
						reverseEdgeHeader.isTaskCompleted(), edgeHeader.isSorted(), reverseEdgeHeader.isSorted());
			}
			return completed;
		} catch (Exception e) {
			log.info("엣지 헤더를 읽을 수 없어 엣지 작업부터 다시 시작합니다. reason: {}", e.getMessage());
			return false;
		}
	}

	private ArrayList<IndexInfo> createIndexList(long[] idArray) {
		ArrayList<IndexInfo> indexList = new ArrayList<>();
		for (int nodeId = 0; nodeId < idArray.length; nodeId++) {
			long coordinate = idArray[nodeId];
			int offset = (int) DataStructureSizes.calculateNodeOffset(nodeId);
			indexList.add(new IndexInfo(nodeId, coordinate, offset));
		}
		return indexList;
	}

	private long[] createIdArray(FeatureCollection<SimpleFeatureType, SimpleFeature> collection) throws IOException {
		log.info("내부 인덱스 배열 생성중 (레이어 포함)");
		long[] nodeIdArray = new long[100000];
		int count = 0;

		FeatureIterator<SimpleFeature> iterator = collection.features();

		while (iterator.hasNext()) {
			SimpleFeature feature = iterator.next();
			Geometry geo = (Geometry) feature.getDefaultGeometry();

			for (org.locationtech.jts.geom.Coordinate coordinate : geo.getCoordinates()) {
				// 좌표와 레이어 정보를 조합하여 유니크 ID 생성
				long uniqueId = GeometryUtil.coordinateToLong(coordinate);
				
				if (count == nodeIdArray.length) {
					nodeIdArray = Arrays.copyOf(nodeIdArray, nodeIdArray.length * 2);
				}

				nodeIdArray[count++] = uniqueId;
			}
		}

		Arrays.sort(nodeIdArray);

		// 중복 제거
		int tempIndex = 0;
		for (int i = 0; i < nodeIdArray.length; i++) {
			if (i == 0 || nodeIdArray[i] != nodeIdArray[i - 1]) {
				nodeIdArray[tempIndex] = nodeIdArray[i];
				tempIndex++;
			}
		}

		log.info("내부 노드 인덱스 배열 총 개수 {}, 중복 제외 총 개수 {}", nodeIdArray.length, tempIndex);
		return Arrays.copyOf(nodeIdArray, tempIndex);
	}

	public void saveIndex(ArrayList<IndexInfo> indexList) throws IOException {
		store.saveNodeIndex(indexList);
	}

	@Override
	public DataStore getStore() {
		return this.store;
	}
}
