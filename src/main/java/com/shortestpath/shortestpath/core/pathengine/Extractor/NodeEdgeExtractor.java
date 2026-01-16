package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.FeatureTaskItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeEdgeTaskItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeEdgeExtractor implements Extractor {
	private HashMap<Coordinate, IndexInfo> indexMap = new HashMap<Coordinate, IndexInfo>();
	private ConcurrentHashMap<Integer, Integer> index = new ConcurrentHashMap<>();
	private File file;
	private DataStore store;
	private int nodeIndex = 0;
	private int edgeIndex = 0;

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

		BlockingQueue<NodeEdgeTaskItem> nodeEdgeQueue = new LinkedBlockingQueue<NodeEdgeTaskItem>(2000);

		List<Runnable> tasks = Arrays.asList(
			new NodeEdgeCreator(collection, idArray, nodeCreatedArray, nodeEdgeQueue, progressStatus),
			new NodeEdgeSave(store, idArray, nodeCreatedArray, lastEdgeOffsetArray, nodeEdgeQueue, progressStatus)
		);
		List<Thread> workers = tasks.stream().map(Thread::new).toList();

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

		// ExecutorService executorService = Executors.newFixedThreadPool(3);
		// executorService.execute(new FeatureReader(collection, featureQueue));
		// executorService.execute(new NodeEdgeCreator(index, featureQueue, nodeEdgeQueue));
		// executorService.execute(new NodeEdgeSave(store, index, nodeEdgeQueue));
		// executorService.shutdown();

		// try {
		// shpStore = FileDataStoreFinder.getDataStore(file);
		// FeatureSource<SimpleFeatureType, SimpleFeature> source =
		// shpStore.getFeatureSource(shpStore.getTypeNames()[0]);
		// FeatureCollection<SimpleFeatureType, SimpleFeature> collection =
		// source.getFeatures();

		// int totalNodecount = getTotalNodeCount(collection);

		// try (FeatureIterator<SimpleFeature> iterator = collection.features()) {

		// while (iterator.hasNext()) {
		// SimpleFeature feature = iterator.next();
		// Geometry geo = (Geometry)feature.getDefaultGeometry();

		// for(int i=0; i<geo.getNumPoints() - 1; i++) {
		// // int id = Integer.parseInt(feature.getAttribute("id").toString());
		// double x = geo.getCoordinates()[i].x;
		// double y = geo.getCoordinates()[i].y;
		// double nextX = geo.getCoordinates()[i + 1].x;
		// double nextY = geo.getCoordinates()[i + 1].y;

		// Coordinate startCoordinate = new Coordinate(y, x);
		// Coordinate endCoordinate = new Coordinate(nextY, nextX);

		// Node startNode = getOrCreateNode(startCoordinate);
		// Node endNode = getOrCreateNode(endCoordinate);

		// IndexInfo startNodeInfo = indexMap.get(startCoordinate);
		// IndexInfo endNodeInfo = indexMap.get(endCoordinate);

		// Edge edge = createEdge(edgeIndex++, startNode, endNode);
		// Edge reverseEdge = createEdge(edgeIndex++, endNode, startNode);
		// int edgeOffset = getEdgeOffset(edge);
		// int reverseEdgeOffset = getEdgeOffset(reverseEdge);

		// updateStartEdgeOffset(startNode, edgeOffset);
		// updateStartEdgeOffset(endNode, reverseEdgeOffset);

		// updateNextEdgeOffset(startNode, edgeOffset);
		// updateNextEdgeOffset(endNode, reverseEdgeOffset);

		// // 인덱스 정보에 노드에 연결되어있는 마지막 엣지 오프셋 업데이트
		// startNodeInfo.setLastEdgeIndex(edgeOffset);
		// endNodeInfo.setLastEdgeIndex(reverseEdgeOffset);

		// // 진행률 표시 메서드 호출
		// if(progressStatus != null) {
		// progressStatus.progress(totalNodecount, nodeIndex);
		// }
		// }
		// }

		// }

		// saveNodeIndex();

		// log.info("노드 인덱스 저장 완료");
		// log.info("노드 및 엣지 추출 및 저장 완료 - 노드 {}개, 엣지 {}개 총 {}개 저장", nodeIndex,
		// edgeIndex, nodeIndex + edgeIndex);

		// } catch (IOException e) {
		// e.printStackTrace();
		// log.error("노드 및 엣지 추출 중 오류가 발생 했습니다. 파일을 확인해주세요.", e);
		// } finally {
		// if (shpStore != null) {
		// shpStore.dispose();
		// }
		// }
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

	/**
	 * 인덱스에서 좌표에 해당하는 노드 정보를 가져옵니다.
	 * 만약 없다면 노드를 새로 만들고 파일에 저장 및 인덱스에 추가 하고 노드를 반환합니다.
	 * 
	 * @param coordinate
	 * @return
	 * @throws IOException
	 */
	private Node getOrCreateNode(Coordinate coordinate) throws IOException {
		Node node = null;

		IndexInfo startInfo = indexMap.get(coordinate);
		if (startInfo == null) {
			node = createNode(nodeIndex++, coordinate);
			int offset = store.saveNode(node);
			IndexInfo newIndexInfo = new IndexInfo(nodeIndex, offset, -1);
			indexMap.put(coordinate, newIndexInfo);
			startInfo = newIndexInfo;
		} else {
			node = store.readNode(startInfo.nodeIndex);
		}

		return node;
	}

	/**
	 * 노드를 생성합니다. 노드의 시작 엣지 오프셋은 -1로 설정됩니다.
	 * 
	 * @param index
	 * @param coordinate
	 * @return 새로운 노드
	 */
	private Node createNode(int index, Coordinate coordinate) {
		Node node = new Node();
		node.setId(index);
		node.setStartEdgeOffset(-1);
		node.setCoordinate(coordinate);

		return node;
	}

	/**
	 * 엣지를 생성하고 파일에 저장합니다. 엣지의 다음 엣지 오프셋은 -1로 설정됩니다.
	 * 
	 * @param index
	 * @param startNode
	 * @param endNode
	 * @return 새로운 엣지
	 * @throws IOException
	 */
	private Edge createEdge(int index, Node startNode, Node endNode) throws IOException {
		int startNodeOffset = getNodeOffset(startNode);
		int endNodeOffset = getNodeOffset(endNode);

		Edge edge = new Edge(index, startNodeOffset, endNodeOffset,
				startNode.getCoordinate().calculateDistanceToTarget(endNode.getCoordinate()), -1);

		store.saveEdge(edge);

		return edge;
	}

	/**
	 * 노드의 오프셋을 인덱스 정보에서 가져오거나 인덱스에 없다면 노드 아이디와 노드 바이트 크기로 계산하여 반환합니다.
	 * 
	 * @param node
	 * @return
	 */
	private int getNodeOffset(Node node) {
		int offset = -1;

		IndexInfo info = indexMap.get(node.getCoordinate());
		if (info == null) {
			offset = node.getId()
					* ((com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore) store).getNodeByteSize();
		} else {
			offset = info.getNodeIndex();
		}

		return offset;
	}

	/**
	 * 엣지의 오프셋을 엣지 아이디와 엣지 바이트 크기로 계산하여 반환합니다.
	 * 
	 * @param edge
	 * @return
	 */
	private int getEdgeOffset(Edge edge) {
		return edge.getId()
				* ((com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore) store).getEdgeByteSize();
	}

	/**
	 * 노드의 시작 엣지 오프셋을 설정하고 노드를 다시 저장합니다.
	 * 
	 * @param node
	 * @param edgeOffset
	 * @throws IOException
	 */
	private void updateStartEdgeOffset(Node node, int edgeOffset) throws IOException {
		// 노드의 시작 엣지 오프셋을 설정
		// 기존에 있던 노드 위치에 startEdgeOffet이 포함된 노드를 다시 저장
		if (node.getStartEdgeOffset() <= -1) {
			node.setStartEdgeOffset(edgeOffset);
			store.overwriteNode(node, getNodeOffset(node));
		}
	}

	/**
	 * 인덱스에서 노드와 연결되어 있는 마지막 엣지를 가져와 다음 엣지 오프셋을 설정하고 엣지를 다시 저장합니다.
	 * 
	 * @param node
	 * @param nextEdgeOffset
	 * @throws IOException
	 */
	private void updateNextEdgeOffset(Node node, int nextEdgeOffset) throws IOException {
		// 인덱스 정보에 마지막 엣지 오프셋이 있다면
		// 마지막 엣지를 불러와서 다음 엣지(이웃노드와 연결된 엣지) 오프셋을 설정 후 다시 저장
		IndexInfo info = indexMap.get(node.getCoordinate());
		if (info.getLastEdgeIndex() >= 0) {
			Edge tempEdge = store.readEdge(info.getLastEdgeIndex());
			tempEdge.setNextEdgeOffset(nextEdgeOffset);
			store.overwriteEdge(tempEdge, getEdgeOffset(tempEdge));
		}
	}

	private int getTotalNodeCount(FeatureCollection<SimpleFeatureType, SimpleFeature> collection) throws IOException {
		log.info("총 노드 수 계산 중...");

		Set<org.locationtech.jts.geom.Coordinate> coordinateSet = new HashSet<org.locationtech.jts.geom.Coordinate>();

		try (FeatureIterator<SimpleFeature> iterator = collection.features()) {

			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				Geometry geo = (Geometry) feature.getDefaultGeometry();
				for (org.locationtech.jts.geom.Coordinate coord : geo.getCoordinates()) {
					coordinateSet.add(coord);
				}
			}
		}

		log.info("총 노드 수 계산 완료 - 총 {}개", coordinateSet.size());

		return coordinateSet.size();
	}

	private void saveNodeIndex() throws IOException {
		log.info("노드 인덱스 저장 시작");

		this.store.saveNodeIndex(indexMap);
	}

	@Override
	public DataStore getStore() {
		return this.store;
	}
}
