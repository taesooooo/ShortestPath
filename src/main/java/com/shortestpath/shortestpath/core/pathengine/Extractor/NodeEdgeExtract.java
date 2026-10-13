package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;
import com.shortestpath.shortestpath.core.pathengine.Util.PathUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * SHP 피처를 한 번 순회하면서 노드와 엣지를 함께 생성한다.
 * 생성된 데이터는 서로 다른 큐로 전달되어 노드/엣지 저장 스레드가 병렬로 기록한다.
 */
@Slf4j
public class NodeEdgeExtract implements Runnable {
    private static final int BATCH_SIZE = 1000;
    private static final long QUEUE_OFFER_TIMEOUT_MILLIS = 100;

    private static final Set<String> L0_TYPES = Set.of("motorway", "motorway_link", "trunk", "trunk_link");
    private static final Set<String> L1_TYPES = Set.of(
            "primary", "primary_link", "secondary", "secondary_link", "tertiary", "tertiary_link");

    private final long[] idArray;
    private final BlockingQueue<List<TaskItem>> nodeQueue;
    private final BlockingQueue<List<TaskItem>> edgeQueue;
    private final FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
    private final AtomicBoolean taskContinue;
    private final AtomicBoolean taskError;
    private final ProgressStatus progressStatus;
    private final boolean extractNodes;
    private final boolean extractEdges;
    private final int nodeSaverCount;
    private final int edgeSaverCount;
    private final AtomicInteger extractedEdgeCount;

    public NodeEdgeExtract(
            long[] idArray,
            BlockingQueue<List<TaskItem>> nodeQueue,
            BlockingQueue<List<TaskItem>> edgeQueue,
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection,
            AtomicBoolean taskContinue,
            AtomicBoolean taskError,
            ProgressStatus progressStatus,
            boolean extractNodes,
            boolean extractEdges,
            int nodeSaverCount,
            int edgeSaverCount,
            AtomicInteger extractedEdgeCount) {
        this.idArray = idArray;
        this.nodeQueue = nodeQueue;
        this.edgeQueue = edgeQueue;
        this.collection = collection;
        this.taskContinue = taskContinue;
        this.taskError = taskError;
        this.progressStatus = progressStatus;
        this.extractNodes = extractNodes;
        this.extractEdges = extractEdges;
        this.nodeSaverCount = nodeSaverCount;
        this.edgeSaverCount = edgeSaverCount;
        this.extractedEdgeCount = extractedEdgeCount;
    }

    @Override
    public void run() {
        extractNodeAndEdges();
    }

    private void extractNodeAndEdges() {
        boolean[] nodeQueued = extractNodes ? new boolean[idArray.length] : null;
        ArrayList<TaskItem> nodeBatch = new ArrayList<>(BATCH_SIZE);
        ArrayList<TaskItem> edgeBatch = new ArrayList<>(BATCH_SIZE);
        int nodeCount = 0;
        int edgeCount = 0;

        try (FeatureIterator<SimpleFeature> iterator = collection.features()) {
            while (iterator.hasNext() && !Thread.currentThread().isInterrupted() && !taskError.get()) {
                SimpleFeature feature = iterator.next();
                String roadType = getRoadType(feature);
                if (!RoadTypeFilter.isAllowedRoadType(roadType)) {
                    continue;
                }

                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                RoadLevel roadLevel = determineRoadLevel(roadType);
                int speed = getSpeed(feature);
                String oneWay = getOneWay(feature);
                boolean isGate = isGateType(roadType);

                // MultiLineString의 서로 다른 선분 사이에는 엣지를 만들지 않는다.
                for (int geometryIndex = 0; geometryIndex < geometry.getNumGeometries(); geometryIndex++) {
                    org.locationtech.jts.geom.Coordinate[] coordinates = geometry
                            .getGeometryN(geometryIndex)
                            .getCoordinates();
                    Integer previousNodeId = null;
                    Coordinate previousCoordinate = null;

                    for (org.locationtech.jts.geom.Coordinate sourceCoordinate : coordinates) {
                        int currentNodeId = findNodeId(sourceCoordinate);
                        Coordinate currentCoordinate = new Coordinate(sourceCoordinate.y, sourceCoordinate.x);

                        if (extractNodes && !nodeQueued[currentNodeId]) {
                            nodeBatch.add(new NodeItem(new Node(currentNodeId, currentCoordinate, isGate)));
                            nodeQueued[currentNodeId] = true;
                            nodeCount++;
                            nodeBatch = offerIfFull(nodeQueue, nodeBatch);

                            if (progressStatus != null) {
                                progressStatus.progress(TaskType.NODE_EXTRACT, idArray.length, nodeCount);
                            }
                        }

                        if (extractEdges && previousNodeId != null) {
                            double distance = PathUtil.haversineDistance(previousCoordinate, currentCoordinate);
                            edgeCount = appendEdges(edgeBatch, edgeCount, previousNodeId, currentNodeId,
                                    distance, speed, roadLevel, oneWay);
                            edgeBatch = offerIfFull(edgeQueue, edgeBatch);

                            if (progressStatus != null) {
                                progressStatus.progress(TaskType.EDGE_EXTRACT, -1, edgeCount);
                            }
                        }

                        previousNodeId = currentNodeId;
                        previousCoordinate = currentCoordinate;
                    }
                }
            }

            if (extractNodes && !nodeBatch.isEmpty()) {
                offerBatch(nodeQueue, nodeBatch);
            }
            if (extractEdges && !edgeBatch.isEmpty()) {
                offerBatch(edgeQueue, edgeBatch);
            }

            extractedEdgeCount.set(edgeCount);
            if (progressStatus != null && extractEdges) {
                progressStatus.progress(TaskType.EDGE_EXTRACT, edgeCount, edgeCount);
            }

            signalCompletion();
            log.info("노드/엣지 통합 추출 완료 - 노드: {}, 엣지: {}", nodeCount, edgeCount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            taskError.set(true);
            log.error("노드/엣지 통합 추출 중 인터럽트 발생", e);
        } catch (Exception e) {
            taskError.set(true);
            log.error("노드/엣지 통합 추출 중 예외 발생", e);
        } finally {
            taskContinue.set(false);
        }
    }

    private int appendEdges(
            List<TaskItem> edgeBatch,
            int edgeCount,
            int previousNodeId,
            int currentNodeId,
            double distance,
            int speed,
            RoadLevel roadLevel,
            String oneWay) {
        if ("F".equalsIgnoreCase(oneWay)) {
            edgeBatch.add(new EdgeItem(createEdge(edgeCount++, previousNodeId, currentNodeId,
                    distance, speed, roadLevel)));
        } else if ("T".equalsIgnoreCase(oneWay)) {
            edgeBatch.add(new EdgeItem(createEdge(edgeCount++, currentNodeId, previousNodeId,
                    distance, speed, roadLevel)));
        } else {
            edgeBatch.add(new EdgeItem(createEdge(edgeCount++, previousNodeId, currentNodeId,
                    distance, speed, roadLevel)));
            edgeBatch.add(new EdgeItem(createEdge(edgeCount++, currentNodeId, previousNodeId,
                    distance, speed, roadLevel)));
        }
        return edgeCount;
    }

    private ArrayList<TaskItem> offerIfFull(
            BlockingQueue<List<TaskItem>> queue,
            ArrayList<TaskItem> batch) throws InterruptedException {
        if (batch.size() < BATCH_SIZE) {
            return batch;
        }
        offerBatch(queue, batch);
        return new ArrayList<>(BATCH_SIZE);
    }

    private void offerBatch(BlockingQueue<List<TaskItem>> queue, List<TaskItem> batch)
            throws InterruptedException {
        while (true) {
            if (taskError.get()) {
                throw new IllegalStateException("저장 작업 오류로 큐 전달을 중단합니다.");
            }
            if (queue.offer(batch, QUEUE_OFFER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                return;
            }
        }
    }

    private void signalCompletion() throws InterruptedException {
        if (extractNodes) {
            putEndItems(nodeQueue, nodeSaverCount);
        }
        if (extractEdges) {
            putEndItems(edgeQueue, edgeSaverCount);
        }
    }

    private void putEndItems(BlockingQueue<List<TaskItem>> queue, int count) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            offerBatch(queue, List.of(new EndItem(i)));
        }
    }

    private int findNodeId(org.locationtech.jts.geom.Coordinate coordinate) {
        long coordinateId = GeometryUtil.coordinateToLong(coordinate);
        int nodeId = Arrays.binarySearch(idArray, coordinateId);
        if (nodeId < 0) {
            throw new IllegalStateException("사전 생성된 노드 ID를 찾을 수 없습니다: " + coordinate);
        }
        return nodeId;
    }

    private Edge createEdge(int id, int from, int to, double distance, int speed, RoadLevel roadLevel) {
        return new Edge(id, from, to, distance, -1, speed, roadLevel);
    }

    private String getRoadType(SimpleFeature feature) {
        Object roadType = feature.getAttribute("fclass");
        return roadType == null ? "unclassified" : roadType.toString().toLowerCase().trim();
    }

    private int getSpeed(SimpleFeature feature) {
        Object speed = feature.getAttribute("maxspeed");
        if (speed instanceof Number) {
            return ((Number) speed).intValue();
        }
        if (speed != null) {
            try {
                return Integer.parseInt(speed.toString());
            } catch (NumberFormatException ignored) {
                log.debug("maxspeed 값을 정수로 변환할 수 없습니다: {}", speed);
            }
        }
        return 0;
    }

    private String getOneWay(SimpleFeature feature) {
        Object oneWay = feature.getAttribute("oneway");
        return oneWay == null ? "B" : oneWay.toString();
    }

    private boolean isGateType(String roadType) {
        return "motorway_link".equals(roadType)
                || "primary_link".equals(roadType)
                || "trunk_link".equals(roadType);
    }

    private RoadLevel determineRoadLevel(String roadType) {
        if (L0_TYPES.contains(roadType)) {
            return RoadLevel.L0;
        }
        if (L1_TYPES.contains(roadType)) {
            return RoadLevel.L1;
        }
        return RoadLevel.L2;
    }
}
