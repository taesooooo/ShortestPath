package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.EdgeHeader;

/**
 * 엣지를 파일에 저장하는 클래스
 * EdgeExtract에서 설정된 등급 정보와 함께 엣지를 저장
 */
public class EdgeSaver implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(EdgeSaver.class);

    private BlockingQueue<List<TaskItem>> edgeQueue;
    private DataStore dataStore;
    private ProgressStatus progressStatus;
    private AtomicBoolean taskContinue;
    private AtomicBoolean taskError;
    private AtomicInteger totalSavedEdgeCount;  // 멀티스레드 안전한 총 저장 개수

    public EdgeSaver(BlockingQueue<List<TaskItem>> edgeQueue, DataStore dataStore,
            ProgressStatus progressStatus, AtomicBoolean taskContinue, AtomicBoolean taskError, AtomicInteger totalSavedEdgeCount) {
        this.edgeQueue = edgeQueue;
        this.dataStore = dataStore;
        this.progressStatus = progressStatus;
        this.taskContinue = taskContinue;
        this.taskError = taskError;
        this.totalSavedEdgeCount = totalSavedEdgeCount;
    }

    @Override
    public void run() {
        logger.info("엣지 저장 시작");
        int localEdgeCount = 0;  // 스레드 로컬 카운트

        try {
            while (!Thread.currentThread().isInterrupted() && !taskError.get()) {
                List<TaskItem> taskList = edgeQueue.take();
                
                // EndItem 확인 - 종료 신호 수신
                if (taskList != null && taskList.size() > 0 && taskList.get(0) instanceof EndItem) {
                    logger.debug("스레드 {} - 종료 신호 수신", Thread.currentThread().getName());
                    break;
                }

                for (TaskItem task : taskList) {
                    if (task instanceof EdgeItem) {
                        EdgeItem edgeItem = (EdgeItem) task;
                        Edge edge = edgeItem.getEdge();
                        
                        // DataStore에 저장
                        dataStore.saveEdge(edge);
                        localEdgeCount++;
                    }
                }
                
                // 진행률 업데이트 (멀티스레드 안전하게 집계된 개수 표시)
                if (progressStatus != null && totalSavedEdgeCount != null) {
                    int totalCount = totalSavedEdgeCount.addAndGet(localEdgeCount);
                    progressStatus.progress(TaskType.EDGE_SAVE, -1, totalCount);
                    localEdgeCount = 0;  // 로컬 카운트 초기화
                }
            }
            
            logger.info("엣지 저장 완료. 저장된 엣지 개수: {}", localEdgeCount);
        } 
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("엣지 저장 중 인터럽트 발생", e);
        } 
        catch (Exception e) {
            logger.error("엣지 저장 중 예외 발생", e);
            taskError.set(true); // 예외 발생 시 모든 작업 중단 플래그 설정
        }
    }
}
