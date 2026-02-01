package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

/**
 * BlockingQueue에서 노드를 하나씩 꺼내서 저장하는 클래스
 * NodeExtract에서 생성된 NodeItem을 받아서 데이터베이스에 저장
 */
public class NodeSaver implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(NodeSaver.class);

    private BlockingQueue<TaskItem> nodeQueue;
    private boolean[] nodeCreated;
    private DataStore dataStore;
    private ProgressStatus progressStatus;
    private int savedNodeCount = 0;
    private boolean[] taskArray;
    private AtomicBoolean taskContinue;

    public NodeSaver(BlockingQueue<TaskItem> nodeQueue, boolean[] nodeCreated, DataStore dataStore,
            int extractTaskCount, ProgressStatus progressStatus, AtomicBoolean taskContinue) {
        this.nodeQueue = nodeQueue;
        this.nodeCreated = nodeCreated;
        this.dataStore = dataStore;
        this.taskArray = new boolean[extractTaskCount];
        this.progressStatus = progressStatus;
        this.taskContinue = taskContinue;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted() && taskContinue.get()) {
                TaskItem taskItem = nodeQueue.take();

                // EndItem을 받으면 종료
                if (taskItem instanceof EndItem) {
                    int taskId = ((EndItem) taskItem).getTaskId();
                    taskArray[taskId] = true;
                }

                // NodeItem을 처리
                if (taskItem instanceof NodeItem) {
                    NodeItem nodeItem = (NodeItem) taskItem;

                    // nodeA 저장
                    saveNodeIfNotExists(nodeItem.getNodeA());

                    // nodeB 저장
                    saveNodeIfNotExists(nodeItem.getNodeB());
                }

                if(allTasksCompleted() && nodeQueue.isEmpty()) {
                    break;
                }
            }

            logger.info("노드 저장 완료. 저장된 노드 개수: {}", savedNodeCount);

        } catch (InterruptedException e) {
            logger.error("노드 저장 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("노드 저장 중 예외 발생", e);
            taskContinue.set(false); // 예외 발생 시 모든 작업 중단 플래그 설정
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
        savedNodeCount++;

        // 진행률 업데이트
        if (progressStatus != null) {
            progressStatus.progress(TaskType.NODE_EXTRACT, nodeCreated.length, savedNodeCount);
        }
    }

    public int getSavedNodeCount() {
        return savedNodeCount;
    }

    private boolean allTasksCompleted() {
        for (boolean completed : taskArray) {
            if (!completed) {
                return false;
            }
        }
        return true;
    }
}
