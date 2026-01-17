package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeCSVItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

public class NodeCSVWriter implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(NodeCSVWriter.class);
    
    private BlockingQueue<TaskItem> csvQueue;
    private String filePath;
    private ProgressStatus progressStatus;
    private int totalItems;
    private AtomicBoolean shouldContinue;
    
    public NodeCSVWriter(BlockingQueue<TaskItem> csvQueue, String filePath, ProgressStatus progressStatus, int totalItems, AtomicBoolean shouldContinue) {
        this.csvQueue = csvQueue;
        this.filePath = filePath;
        this.progressStatus = progressStatus;
        this.totalItems = totalItems;
        this.shouldContinue = shouldContinue;
    }
    
    @Override
    public void run() {
        logger.info("노드 CSV 저장 시작");
        int csvCount = 0;
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
        // 파일의 디렉토리가 없으면 생성
        File file = new File(filePath);
        // file.getParentFile().mkdirs();
        
            // CSV 헤더 작성
            writer.write("nodeId,x,y,offset");
            writer.newLine();
            
            while (shouldContinue.get()) {
                TaskItem item = csvQueue.take();
                
                if (item instanceof EndItem) {
                    logger.info("노드 CSV 저장 완료");
                    break;
                } 
                else if (item instanceof NodeCSVItem) {
                    NodeCSVItem csvItem = (NodeCSVItem) item;
                    
                    // 좌표 분리
                    Coordinate coord = GeometryUtil.longToCoordinate(csvItem.getCoordinate());
                    
                    // CSV 라인 작성: nodeId, x, y, offset
                    String line = String.format("%d,%.7f,%.7f,%d",
                        csvItem.getNodeId(),
                        coord.getX(),
                        coord.getY(),
                        csvItem.getOffset());
                    
                    writer.write(line);
                    writer.newLine();
                    
                    csvCount++;
                    if (progressStatus != null) {
                        progressStatus.progress(TaskType.NODE_CSV_WRITER, totalItems, csvCount);
                    }
                }
            }
        } 
        catch (InterruptedException e) {
            logger.info("노드 CSV 저장 - 인터럽트 발생하여 종료합니다.");
            Thread.currentThread().interrupt();
            shouldContinue.set(false);
        }
        catch (IOException e) {
            logger.error("노드 CSV 저장 중 오류가 발생했습니다.", e);
            shouldContinue.set(false);
        }
        catch (Exception e) {
            logger.error("노드 CSV 저장 중 예외 발생", e);
            shouldContinue.set(false);
        }
    }
}
