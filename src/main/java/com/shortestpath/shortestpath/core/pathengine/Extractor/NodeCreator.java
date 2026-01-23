package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeEdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

import java.io.IOException;

public class NodeCreator implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(NodeCreator.class);
    
    private long[] idArray;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
    private boolean[] nodeCreated;
    private BlockingQueue<TaskItem> nodeEdgeQueue;
    private DataStore dataStore;
    private ProgressStatus progressStatus;
    private AtomicBoolean shouldContinue;

    public NodeCreator(FeatureCollection<SimpleFeatureType, SimpleFeature> collection, long[] idArray,
            boolean[] nodeCreated, BlockingQueue<TaskItem> nodeEdgeQueue, DataStore dataStore, ProgressStatus progressStatus, AtomicBoolean shouldContinue) {
        this.collection = collection;
        this.idArray = idArray;
        this.nodeCreated = nodeCreated;
        this.nodeEdgeQueue = nodeEdgeQueue;
        this.dataStore = dataStore;
        this.progressStatus = progressStatus;
        this.shouldContinue = shouldContinue;
    }

    @Override
    public void run() {
        int creatorCount = 0;
        try (FeatureIterator<SimpleFeature> iterator = collection.features()) {
            while (iterator.hasNext() && shouldContinue.get()) {
                
                SimpleFeature feature = iterator.next();
                Geometry geo = (Geometry) feature.getDefaultGeometry();

                // 연속된 두 좌표를 쌍으로 읽기 (0-1, 1-2, 2-3, ...)
                for (int i = 0; i < geo.getNumPoints() - 1; i++) {
                    double x = geo.getCoordinates()[i].x;
                    double y = geo.getCoordinates()[i].y;
                    double nextX = geo.getCoordinates()[i + 1].x;
                    double nextY = geo.getCoordinates()[i + 1].y;

                    Coordinate coordinateA = new Coordinate(y, x);
                    Coordinate coordinateB = new Coordinate(nextY, nextX);

                    long coordIdA = GeometryUtil.coordinateToLong(geo.getCoordinates()[i]);
                    long coordIdB = GeometryUtil.coordinateToLong(geo.getCoordinates()[i + 1]);

                    // idArray에서 이진 탐색 후 나오는 인덱스가 노드 ID
                    int indexA = Arrays.binarySearch(idArray, coordIdA);
                    int indexB = Arrays.binarySearch(idArray, coordIdB);

                    // 노드 생성 및 저장
                    Node nodeA = saveNode(coordinateA, indexA);
                    Node nodeB = saveNode(coordinateB, indexB);

                    nodeEdgeQueue.put(new NodeEdgeItem(nodeA, nodeB));
                    
                    // 진행률 업데이트
                    creatorCount++;
                    if (progressStatus != null) {
                        progressStatus.progress(TaskType.NODE_EXTRACT, idArray.length, creatorCount);
                    }
                }
                
            }

            nodeEdgeQueue.put(new EndItem());
            logger.info("노드/엣지 생성 완료");
        } 
        catch (InterruptedException e) {
            logger.info("노드/엣지 생성 - 인터럽트 발생하여 종료합니다.");
            Thread.currentThread().interrupt();
            shouldContinue.set(false);
        }
        catch (Exception e) {
            logger.error("노드/엣지 생성 중 예외 발생", e);
            shouldContinue.set(false);
        }

    }

    private Node saveNode(Coordinate coordinate, int nodeId) throws IOException {
        // 노드 생성 및 저장
        Node node = new Node(nodeId, coordinate, -1, 0, 0, 0);
        
        // 이미 생성된 노드가 있다면 그냥 노드 반환
        if(nodeCreated[nodeId]) {
            return node;
        }
        
        // DataStore에 노드 저장
        dataStore.saveNode(node, nodeId * DataStructureSizes.NODE_SIZE);
        nodeCreated[nodeId] = true;

        return node;
    }
}
