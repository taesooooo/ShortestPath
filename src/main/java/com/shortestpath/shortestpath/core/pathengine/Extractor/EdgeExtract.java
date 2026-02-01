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
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;
import com.shortestpath.shortestpath.core.pathengine.Util.PathUtil;

/**
 * SHP 파일에서 엣지 정보를 추출하는 클래스
 * 도로 유형을 파악하여 도로 등급(L0, L1, L2)을 결정하고 엣지를 생성
 */
public class EdgeExtract implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(EdgeExtract.class);
    
    private int taskId;
    private long[] idArrays;
    private BlockingQueue<TaskItem> edgeQueue;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
    private AtomicBoolean taskContinue;
    
    // 도로 유형 속성 이름 (SHP 파일의 표준 속성)
    private static final String ROAD_TYPE_ATTRIBUTE = "fclass";
    
    // 도로 유형별 등급 정의
    private static final String[] L0_TYPES = {"motorway", "motorway_link", "trunk", "trunk_link"};
    private static final String[] L1_TYPES = {"primary", "primary_link", "secondary", "secondary_link", "tertiary", "tertiary_link"};
    private static final String[] L2_TYPES = {"residential", "unclassified", "service", "track", "living_street"};
    
    public EdgeExtract(int taskId, long[] idArrays, BlockingQueue<TaskItem> edgeQueue, 
                      FeatureCollection<SimpleFeatureType, SimpleFeature> collection, AtomicBoolean taskContinue) {
        this.taskId = taskId;
        this.idArrays = idArrays;
        this.edgeQueue = edgeQueue;
        this.collection = collection;
        this.taskContinue = taskContinue;
    }
    
    @Override
    public void run() {
        extractEdges();
    }
    
    /**
     * SHP 파일에서 엣지 정보를 추출하여 큐에 추가
     */
    private void extractEdges() {
        FeatureIterator<SimpleFeature> iterator = collection.features();
        int edgeId = 0;
        
        try {
            while (iterator.hasNext() && !Thread.currentThread().isInterrupted() && taskContinue.get()) {
                SimpleFeature feature = iterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                
                // 도로 유형을 속성에서 가져오기
                String roadType = getRoadType(feature);
                RoadLevel roadLevel = determinateRoadLevel(roadType);
                
                // 연속된 두 좌표를 엣지로 변환
                for (int i = 0; i < geometry.getNumPoints() - 1; i++) {
                    double x = geometry.getCoordinates()[i].x;
                    double y = geometry.getCoordinates()[i].y;
                    double nextX = geometry.getCoordinates()[i + 1].x;
                    double nextY = geometry.getCoordinates()[i + 1].y;
                    
                    Coordinate coordinateFrom = new Coordinate(y, x);
                    Coordinate coordinateTo = new Coordinate(nextY, nextX);
                    
                    long coordIdFrom = GeometryUtil.coordinateToLong(geometry.getCoordinates()[i]);
                    long coordIdTo = GeometryUtil.coordinateToLong(geometry.getCoordinates()[i + 1]);
                    
                    // idArray에서 이진 탐색으로 노드 ID 획득
                    int fromNodeId = Arrays.binarySearch(idArrays, coordIdFrom);
                    int toNodeId = Arrays.binarySearch(idArrays, coordIdTo);
                    
                    // 엣지 거리 계산
                    double distance = PathUtil.haversineDistance(coordinateFrom, coordinateTo);
                    
                    // 양방향 엣지 생성 (ID는 0으로 고정)
                    Edge forwardEdge = createEdge(0, fromNodeId, toNodeId, distance, roadLevel);
                    Edge backwardEdge = createEdge(0, toNodeId, fromNodeId, distance, roadLevel);
                    
                    // 엣지를 큐에 추가
                    edgeQueue.put(new EdgeItem(forwardEdge));
                    edgeQueue.put(new EdgeItem(backwardEdge));
                }
            }
            
            // 작업 완료 신호 전송
            edgeQueue.put(new EndItem(taskId));
            logger.info("엣지 추출 완료. 총 {} 개의 엣지 생성 (양방향 포함)", edgeId);
        } 
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("엣지 추출 중 인터럽트 발생", e);
        }
        catch (Exception e) {
            logger.error("엣지 추출 중 예외 발생", e);
            taskContinue.set(false); // 예외 발생 시 모든 작업 중단 플래그 설정
        }
        finally {
            iterator.close();
        }
    }
    
    /**
     * SHP 파일의 속성에서 도로 유형을 추출
     */
    private String getRoadType(SimpleFeature feature) {
        try {
            Object roadTypeValue = feature.getAttribute(ROAD_TYPE_ATTRIBUTE);
            if (roadTypeValue != null) {
                return roadTypeValue.toString().toLowerCase().trim();
            }
        } 
        catch (Exception e) {
            logger.warn("도로 유형 속성 '{}' 읽기 실패", ROAD_TYPE_ATTRIBUTE);
        }
        return "unclassified"; // 기본값
    }
    
    /**
     * 도로 유형에 따라 도로 등급(L0, L1, L2)을 결정
     */
    private RoadLevel determinateRoadLevel(String roadType) {
        if (isInArray(roadType, L0_TYPES)) {
            return RoadLevel.L0;
        } 
        else if (isInArray(roadType, L1_TYPES)) {
            return RoadLevel.L1;
        } 
        else if (isInArray(roadType, L2_TYPES)) {
            return RoadLevel.L2;
        }
        
        // 정의되지 않은 유형은 L2로 분류
        return RoadLevel.L2;
    }
    
    /**
     * 문자열이 배열에 포함되어 있는지 확인
     */
    private boolean isInArray(String value, String[] array) {
        for (String item : array) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 엣지 객체 생성
     */
    private Edge createEdge(int id, int fromNodeId, int toNodeId, double distance, RoadLevel roadLevel) {
        return new Edge(id, fromNodeId, toNodeId, distance, -1, roadLevel);
    }
}
