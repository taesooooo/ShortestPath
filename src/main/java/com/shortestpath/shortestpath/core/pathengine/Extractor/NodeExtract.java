package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

public class NodeExtract implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(NodeExtract.class);
    
    private long[] idArrays;
    private BlockingQueue<List<TaskItem>> nodeQueue;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
    private AtomicBoolean taskContinue;
    private AtomicBoolean taskError;
    private ProgressStatus progressStatus;
    private int threadCount;

    public NodeExtract(long[] idArrays, BlockingQueue<List<TaskItem>> nodeQueue,
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection, AtomicBoolean taskContinue, AtomicBoolean taskError, ProgressStatus progressStatus, int threadCount) {
        this.idArrays = idArrays;
        this.nodeQueue = nodeQueue;
        this.collection = collection;
        this.taskContinue = taskContinue;
        this.taskError = taskError;
        this.progressStatus = progressStatus;
        this.threadCount = threadCount;
    }

    @Override
    public void run() {
        extractNodes();
    }

    private void extractNodes() {
        FeatureIterator<SimpleFeature> iterator = collection.features();
        ArrayList<TaskItem> nodeList = new ArrayList<>(1000);
        int extractedNodeCount = 0;
        int totalFeatures = collection.size();
        try {
            while (iterator.hasNext() && !Thread.currentThread().isInterrupted() && !taskError.get()) {
                if(nodeList.size() >= 1000) {
                    nodeQueue.put(nodeList);
                    nodeList = new ArrayList<>(1000);
                }
                SimpleFeature feature = iterator.next();
                Node nodeA = null;
                Node nodeB = null;

                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                
                // layer 속성 추출
                Object layerId = null;
                try {
                    layerId = feature.getProperty("layer").getValue();
                } catch (Exception e) {
                    logger.debug("layer 속성을 찾을 수 없습니다. 기본값 0으로 처리");
                    layerId = 0;
                }
                
                // 게이트 여부 판단 (motorway_link, primary_link, trunk_link)
                String roadType = getRoadType(feature);
                boolean isGate = isGateType(roadType);
                
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

                    nodeA = createNode(coordinateA, indexA, isGate);
                    nodeB = createNode(coordinateB, indexB, isGate);
                    nodeList.add(new NodeItem(nodeA, nodeB));
                    extractedNodeCount += 2;
                }
                
                // 진행률 업데이트
                if(progressStatus != null) {
                    progressStatus.progress(TaskType.NODE_EXTRACT, idArrays.length, extractedNodeCount);
                }
            }

            // 나머지 데이터 큐에 추가
            if(nodeList.size() > 0) {
                nodeQueue.put(nodeList);
            }

            // 종료 신호: 각 워커 스레드마다 EndItem 넣기
            for(int i = 0; i < threadCount; i++) {
                List<TaskItem> endList = new ArrayList<>();
                endList.add(new EndItem(0));
                nodeQueue.put(endList);
            }
            
            taskContinue.set(false);
            logger.info("NodeExtract 완료, 총 추출된 노드 개수: {}", extractedNodeCount);
        } 
        catch (InterruptedException e) {
            logger.error("NodeExtract 인터럽트 발생", e);
            Thread.currentThread().interrupt();
        }
        catch (Exception e) {
            logger.error("NodeExtract 예외 발생", e);
            taskError.set(true); // 예외 발생 시 모든 작업 중단 플래그 설정
        }
        finally {
            iterator.close();
        }
    }

    private String getRoadType(SimpleFeature feature) {
        try {
            Object roadTypeValue = feature.getAttribute("fclass");
            if (roadTypeValue != null) {
                return roadTypeValue.toString().toLowerCase().trim();
            }
        } 
        catch (Exception e) {
            logger.debug("도로 유형 속성 'fclass' 읽기 실패");
        }
        return "unclassified"; // 기본값
    }

    private boolean isGateType(String roadType) {
        return ("motorway_link".equals(roadType) || 
                "primary_link".equals(roadType) || 
                "trunk_link".equals(roadType));
    }

    private Node createNode(Coordinate coordinate, int nodeId, boolean isGate) {
        return new Node(nodeId, coordinate, isGate);
    }

}
