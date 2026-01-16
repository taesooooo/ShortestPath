package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeEdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeEdgeTaskItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

public class NodeEdgeSave implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(NodeEdgeSave.class);

    private DataStore dataStore;
    long[] idArray;
    boolean[] nodeCreated;
    int[] lastEdgeOffsetArray;
    private BlockingQueue<NodeEdgeTaskItem> nodeEdgeQueue;
    private ProgressStatus progressStatus;

    public NodeEdgeSave(DataStore dataStore, long[] idArray, boolean[] nodeCreated, int[] lastEdgeOffsetArray, BlockingQueue<NodeEdgeTaskItem> nodeEdgeQueue, ProgressStatus progressStatus) {
        this.dataStore = dataStore;
        this.idArray = idArray;
        this.nodeCreated = nodeCreated;
        this.lastEdgeOffsetArray = lastEdgeOffsetArray;
        this.nodeEdgeQueue = nodeEdgeQueue;
        this.progressStatus = progressStatus;
    }

    @Override
    public void run() {
        logger.info("노드/엣지 저장 시작");
        int saveCount = 0;
        while(true) {
            try {
                if(Thread.currentThread().isInterrupted()) {
                    break;
                }

                NodeEdgeTaskItem item = nodeEdgeQueue.take();
                if(item instanceof EndItem) {
                    logger.info("노드 엣지 저장 완료");
                    break;
                }
                else {
                    NodeEdgeItem saveTaskItem = (NodeEdgeItem) item;
                    Node nodeA = saveTaskItem.getNodeA();
                    Node nodeB = saveTaskItem.getNodeB();
                    Edge edgeA = saveTaskItem.getEdgeA();
                    Edge edgeB = saveTaskItem.getEdgeB();
    
                    long edgeOffsetA = dataStore.saveEdge(edgeA);
                    long edgeOffsetB = dataStore.saveEdge(edgeB);
                    
                    long nodeOffsetA = saveNode(nodeA, edgeOffsetA);
                    long nodeOffsetB = saveNode(nodeB, edgeOffsetB);
                    
                    updateNextEdgeOffset(nodeA, edgeA, edgeOffsetA);
                    updateNextEdgeOffset(nodeB, edgeB, edgeOffsetB);
                    
                    // 진행률 업데이트
                    saveCount++;
                    if (progressStatus != null) {
                        progressStatus.progress(TaskType.NODE_EDGE_SAVE, idArray.length, saveCount);
                    }
                }
            }
            catch(InterruptedException e) {
                logger.info("노드/엣지 저장 - 인터럽트 되어 종료되었습니다.");
                Thread.currentThread().interrupt();
                break;
            }
            catch(IOException e) {
                logger.info("노드/엣지 저장 - 저장 중 문제가 발생하여 종료되었습니다.", e);
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private int saveNode(Node node, long edgeOffset) throws IOException {
        if(node != null) {
            node.setStartEdgeOffset((int)edgeOffset);
            return dataStore.saveNode(node);
        }

        return 0;
    }

    private void updateNextEdgeOffset(Node node, Edge edge, long edgeOffset) throws IOException {
        int nodeId = node != null ? node.getId() : edge.getFrom();
        int lastEdgeOffset = lastEdgeOffsetArray[nodeId];

        if(lastEdgeOffset != -1) {
            Edge readEdge = dataStore.readEdge(lastEdgeOffset);
            readEdge.setNextEdgeOffset((int)edgeOffset);
            dataStore.overwriteEdge(readEdge, lastEdgeOffset);
        }
        lastEdgeOffsetArray[nodeId] = (int)edgeOffset;
    }
}
