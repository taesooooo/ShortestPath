package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeExtract implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(NodeExtract.class);
    
    private int taskId;
    private long[] idArrays;
    private BlockingQueue<TaskItem> nodeQueue;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
    private AtomicBoolean taskContinue;

    public NodeExtract(int taskId, long[] idArrays, BlockingQueue<TaskItem> nodeQueue,
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection, AtomicBoolean taskContinue) {
        this.taskId = taskId;
        this.idArrays = idArrays;
        this.nodeQueue = nodeQueue;
        this.collection = collection;
        this.taskContinue = taskContinue;
    }

    @Override
    public void run() {
        extractNodes();
    }

    private void extractNodes() {
        FeatureIterator<SimpleFeature> iterator = collection.features();
        try {
            while (iterator.hasNext() && !Thread.currentThread().isInterrupted() && taskContinue.get()) {
                SimpleFeature feature = iterator.next();
                Node nodeA = null;
                Node nodeB = null;

                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                for (int i = 0; i < geometry.getNumPoints() - 1; i++) {
                    double x = geometry.getCoordinates()[i].x;
                    double y = geometry.getCoordinates()[i].y;
                    double nextX = geometry.getCoordinates()[i + 1].x;
                    double nextY = geometry.getCoordinates()[i + 1].y;

                    Coordinate coordinateA = new Coordinate(y, x);
                    Coordinate coordinateB = new Coordinate(nextY, nextX);

                    long coordIdA = GeometryUtil.coordinateToLong(geometry.getCoordinates()[i]);
                    long coordIdB = GeometryUtil.coordinateToLong(geometry.getCoordinates()[i + 1]);

                    // idArray에서 이진 탐색 후 나오는 인덱스가 노드 ID
                    int indexA = Arrays.binarySearch(idArrays, coordIdA);
                    int indexB = Arrays.binarySearch(idArrays, coordIdB);

                    nodeA = createNode(coordinateA, indexA);
                    nodeB = createNode(coordinateB, indexB);

                    nodeQueue.put(new NodeItem(nodeA, nodeB));
                }

                nodeQueue.put(new EndItem(taskId));
            }
        } 
        catch (InterruptedException e) {
            logger.error("NodeExtract 인터럽트 발생", e);
            Thread.currentThread().interrupt();
        }
        catch (Exception e) {
            logger.error("NodeExtract 예외 발생", e);
            taskContinue.set(false); // 예외 발생 시 모든 작업 중단 플래그 설정
        }
        finally {
            iterator.close();
        }
    }

    private Node createNode(Coordinate coordinate, int nodeId) {
        return new Node(nodeId, coordinate);
    }

}
