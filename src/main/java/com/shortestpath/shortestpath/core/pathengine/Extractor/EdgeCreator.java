package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeEdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeCSVItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

public class EdgeCreator implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(EdgeCreator.class);

    private DataStore dataStore;
    long[] idArray;
    boolean[] nodeCreated;
    int[] lastEdgeOffsetArray;
    private BlockingQueue<TaskItem> nodeEdgeQueue;
    private ProgressStatus progressStatus;
    private AtomicBoolean shouldContinue;
    private int edgeIndex = 0;

    public EdgeCreator(DataStore dataStore, long[] idArray, boolean[] nodeCreated, int[] lastEdgeOffsetArray, BlockingQueue<TaskItem> nodeEdgeQueue, ProgressStatus progressStatus, AtomicBoolean shouldContinue) {
        this.dataStore = dataStore;
        this.idArray = idArray;
        this.nodeCreated = nodeCreated;
        this.lastEdgeOffsetArray = lastEdgeOffsetArray;
        this.nodeEdgeQueue = nodeEdgeQueue;
        this.progressStatus = progressStatus;
        this.shouldContinue = shouldContinue;
    }

    @Override
    public void run() {
        logger.info("노드/엣지 저장 시작");
        while(shouldContinue.get()) {
            try {
                TaskItem item = nodeEdgeQueue.take();
                if(item instanceof EndItem) {
                    logger.info("노드 엣지 저장 완료");
                    break;
                }
                else {
                    NodeEdgeItem saveTaskItem = (NodeEdgeItem) item;
                    Node nodeA = saveTaskItem.getNodeA();
                    Node nodeB = saveTaskItem.getNodeB();

                    long edgeOffsetA = createAndSaveEdge(nodeA, nodeB);
                    long edgeOffsetB = createAndSaveEdge(nodeB, nodeA);
                    
                    // 처음 생성된 노드라면 엣지 시작 오프셋 설정하고 다시 저장
                    updateNodeStartEdgeIfNeeded(nodeA, edgeOffsetA);
                    updateNodeStartEdgeIfNeeded(nodeB, edgeOffsetB);
                    
                    updateNextEdgeOffset(nodeA.getId(), edgeOffsetA);
                    updateNextEdgeOffset(nodeB.getId(), edgeOffsetB);

                    // 진행률 업데이트

                }
            }
            catch(InterruptedException e) {
                logger.info("노드/엣지 저장 - 인터럽트 발생하여 종료합니다.");
                Thread.currentThread().interrupt();
                shouldContinue.set(false);
            }
            catch(IOException e) {
                logger.error("노드/엣지 저장 - 저장 중 문제가 발생하여 종료되었습니다.", e);
                shouldContinue.set(false);
            }
            catch(Exception e) {
                logger.error("노드/엣지 저장 중 예외 발생", e);
                shouldContinue.set(false);
            }
        }
    }

    private void updateNodeStartEdgeIfNeeded(Node node, long edgeOffset) throws IOException {
        if (node != null && node.getStartEdgeOffset() == -1) {
            node.setStartEdgeOffset((int)edgeOffset);
            dataStore.overwriteNode(node, node.getId());
        }
    }

    private void updateNextEdgeOffset(int nodeId, long edgeOffset) throws IOException {
        int lastEdgeOffset = lastEdgeOffsetArray[nodeId];

        if(lastEdgeOffset != -1) {
            Edge readEdge = dataStore.readEdge(lastEdgeOffset);
            readEdge.setNextEdgeOffset((int)edgeOffset);
            dataStore.overwriteEdge(readEdge, lastEdgeOffset);
        }
        lastEdgeOffsetArray[nodeId] = (int)edgeOffset;
    }

    private long createAndSaveEdge(Node nodeA, Node nodeB) throws IOException {
        Edge edge = new Edge(edgeIndex++, nodeA.getId(), nodeB.getId(), nodeA.getCoordinate().calculateDistanceToTarget(nodeB.getCoordinate()), -1);
        return dataStore.saveEdge(edge);
    }
}
