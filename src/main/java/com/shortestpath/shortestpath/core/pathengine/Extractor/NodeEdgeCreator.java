package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeEdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeEdgeTaskItem;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

public class NodeEdgeCreator implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(NodeEdgeCreator.class);
    
    private long[] idArray;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
    private boolean[] nodeCreated;
    private BlockingQueue<NodeEdgeTaskItem> nodeEdgeQueue;
    private int nodeIndex = 0;
    private int edgeIndex = 0;
    private ProgressStatus progressStatus;

    public NodeEdgeCreator(FeatureCollection<SimpleFeatureType, SimpleFeature> collection, long[] idArray,
            boolean[] nodeCreated, BlockingQueue<NodeEdgeTaskItem> nodeEdgeQueue, ProgressStatus progressStatus) {
        this.collection = collection;
        this.idArray = idArray;
        this.nodeCreated = nodeCreated;
        this.nodeEdgeQueue = nodeEdgeQueue;
        this.progressStatus = progressStatus;
    }

    @Override
    public void run() {
        int creatorCount = 0;
        try (FeatureIterator<SimpleFeature> iterator = collection.features()) {
            while (iterator.hasNext()) {
                
                if(Thread.currentThread().isInterrupted()) {
                    break;
                }

                SimpleFeature feature = iterator.next();
                Geometry geo = (Geometry) feature.getDefaultGeometry();

                for (int i = 0; i < geo.getNumPoints() - 1; i++) {
                    // int id = Integer.parseInt(feature.getAttribute("id").toString());
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

                    Node nodeA = createNode(coordinateA, indexA);
                    Node nodeB = createNode(coordinateB, indexB);

                    Edge edge = createEdge(indexA, indexB, coordinateA, coordinateB);
                    Edge reverseEdge = createEdge(indexB, indexA, coordinateB, coordinateA);

                    nodeEdgeQueue.put(new NodeEdgeItem(nodeA, nodeB, edge, reverseEdge));
                    
                    // 진행률 업데이트
                    creatorCount++;
                    if (progressStatus != null) {
                        progressStatus.progress(TaskType.NODE_EDGE_CREATOR, idArray.length, creatorCount);
                    }
                }
                
            }

            nodeEdgeQueue.put(new EndItem());
            logger.info("노드/엣지 생성 완료");
        } 
        catch (InterruptedException e) {
            logger.info("노드/엣지 생성 - 생성 중 인터럽트가 발생하여 종료합니다.", e);
            Thread.currentThread().interrupt();
        }

    }

    private Node createNode(Coordinate coordinate, int nodeId) {
        if(nodeCreated[(int)nodeId]) {
            return null;
        }

        Node node = new Node(nodeId, coordinate, -1, 0, 0, 0);
        nodeCreated[nodeId] = true;

        return node;
    }

    private Edge createEdge(int nodeIdA, int nodeIdB, Coordinate coordinateA, Coordinate coordinateB) {
        return new Edge(edgeIndex++, nodeIdA, nodeIdB,
                coordinateA.calculateDistanceToTarget(coordinateB), -1);
    }
}
