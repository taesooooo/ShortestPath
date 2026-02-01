package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.EdgeHeader;

/**
 * 엣지를 파일에 저장하는 클래스
 * EdgeExtract에서 설정된 등급 정보와 함께 엣지를 저장
 */
public class EdgeSaver implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(EdgeSaver.class);

    private BlockingQueue<TaskItem> edgeQueue;
    private DataStore dataStore;
    private boolean[] taskArray;
    private ProgressStatus progressStatus;
    private AtomicBoolean taskContinue;

    public EdgeSaver(BlockingQueue<TaskItem> edgeQueue, DataStore dataStore, int extractTaskCount,
            ProgressStatus progressStatus, AtomicBoolean taskContinue) {
        this.edgeQueue = edgeQueue;
        this.dataStore = dataStore;
        this.taskArray = new boolean[extractTaskCount];
        this.progressStatus = progressStatus;
        this.taskContinue = taskContinue;
    }

    @Override
    public void run() {
        logger.info("엣지 저장 시작");
        int edgeCount = 0;

        try {
            dataStore.writeEdgeHeader(new EdgeHeader(0, false));
            
            while (!Thread.currentThread().isInterrupted() && taskContinue.get()) {
                TaskItem item = edgeQueue.take();

                if (item instanceof EndItem) {
                    int taskId = ((EndItem) item).getTaskId();
                    taskArray[taskId] = true;
                }
                
                if (item instanceof EdgeItem) {
                    Edge edge = ((EdgeItem) item).getEdge();
                    
                    // 엣지 ID를 저장 순서대로 설정
                    edge.setId(edgeCount);
                    
                    // DataStore에 저장
                    dataStore.saveEdge(edge);
                    edgeCount++;
                }
                
                // 진행률 업데이트
                if (progressStatus != null) {
                    progressStatus.progress(TaskType.EDGE_EXTRACT, -1, edgeCount);
                }
                
                if (allTasksCompleted() && edgeQueue.isEmpty()) {
                    dataStore.writeEdgeHeader(new EdgeHeader(edgeCount, false));
                    logger.info("엣지 저장 완료. 총 {} 개의 엣지 저장", edgeCount);
                    break;
                }
            }
        } 
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("엣지 저장 중 인터럽트 발생", e);
        } 
        catch (Exception e) {
            logger.error("엣지 저장 중 예외 발생", e);
            taskContinue.set(false); // 예외 발생 시 모든 작업 중단 플래그 설정
        }
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
