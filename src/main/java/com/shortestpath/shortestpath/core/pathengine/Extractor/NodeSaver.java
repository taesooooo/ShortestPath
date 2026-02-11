package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

/**
 * BlockingQueue에서 노드를 하나씩 꺼내서 저장하는 클래스
 * NodeExtract에서 생성된 NodeItem을 받아서 데이터베이스에 저장
 */
public class NodeSaver implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(NodeSaver.class);

    private BlockingQueue<List<TaskItem>> nodeQueue;
    private boolean[] nodeCreated;
    private DataStore dataStore;
    private ProgressStatus progressStatus;
    private boolean[] taskArray;
    private AtomicBoolean taskContinue;
    private AtomicBoolean taskError;
    private AtomicInteger totalSavedNodeCount;  // 멀티스레드 안전한 총 저장 개수

    public NodeSaver(BlockingQueue<List<TaskItem>> nodeQueue, boolean[] nodeCreated, DataStore dataStore, ProgressStatus progressStatus, AtomicBoolean taskContinue, AtomicBoolean taskError) {
        this(nodeQueue, nodeCreated, dataStore, progressStatus, taskContinue, taskError, null);
    }

    public NodeSaver(BlockingQueue<List<TaskItem>> nodeQueue, boolean[] nodeCreated, DataStore dataStore, ProgressStatus progressStatus, AtomicBoolean taskContinue, AtomicBoolean taskError, AtomicInteger totalSavedNodeCount) {
        this.nodeQueue = nodeQueue;
        this.nodeCreated = nodeCreated;
        this.dataStore = dataStore;
        this.progressStatus = progressStatus;
        this.taskContinue = taskContinue;
        this.taskError = taskError;
        this.totalSavedNodeCount = totalSavedNodeCount;
    }

    @Override
    public void run() {
        int localNodeCount = 0;  // 스레드 로컬 카운트
        try {
            while (!Thread.currentThread().isInterrupted() && !taskError.get()) {
                List<TaskItem> taskList = nodeQueue.take();
                
                // EndItem 확인 - 종료 신호 수신
                if (taskList != null && taskList.size() > 0 && taskList.get(0) instanceof EndItem) {
                    logger.debug("스레드 {} - 종료 신호 수신", Thread.currentThread().getName());
                    break;
                }

                for (TaskItem task : taskList) {
                    if (task instanceof NodeItem) {
                        NodeItem nodeItem = (NodeItem) task;
                        // 노드 저장
                        saveNodeIfNotExists(nodeItem.getNode());

                        localNodeCount += 1;
                    }
                }

                // 진행률 업데이트 (멀티스레드 안전하게 집계된 개수 표시)
                if(progressStatus != null && totalSavedNodeCount != null) {
                    int totalCount = totalSavedNodeCount.addAndGet(localNodeCount);
                    progressStatus.progress(TaskType.NODE_SAVE, nodeCreated.length, totalCount);
                    localNodeCount = 0;  // 로컬 카운트 초기화
                }
            }

            // logger.info("노드 저장 완료. 저장된 노드 개수: {}", localNodeCount);

        } 
        catch (InterruptedException e) {
            logger.error("노드 저장 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
        } 
        catch (Exception e) {
            logger.error("노드 저장 중 예외 발생", e);
            taskError.set(true); // 예외 발생 시 모든 작업 중단 플래그 설정
        }
    }

    /**
     * nodeCreated 배열을 확인하여 이미 저장된 노드가 아닌 경우에만 저장
     */
    private void saveNodeIfNotExists(Node node) throws IOException {
        int nodeId = node.getId();

        // 이미 생성되어 저장된 노드인지 확인
        if (nodeCreated[nodeId]) {
            return;
        }

        // 아직 저장되지 않은 노드이면 저장
        dataStore.saveNode(node, nodeId * DataStructureSizes.NODE_SIZE);
        nodeCreated[nodeId] = true;

        // 진행률 업데이트
        if (progressStatus != null) {
            progressStatus.progress(TaskType.NODE_EXTRACT, nodeCreated.length, 0);
        }
    }
}
