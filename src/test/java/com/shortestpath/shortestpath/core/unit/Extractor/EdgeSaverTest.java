package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.Extractor.EdgeSaver;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

@DisplayName("EdgeSaver 단위 테스트")
public class EdgeSaverTest {
    
    private EdgeSaver edgeSaver;
    private DataStore dataStore;
    private BlockingQueue<TaskItem> edgeQueue;
    private AtomicBoolean taskContinue = new AtomicBoolean(true);
    
    @BeforeEach
    public void setUp() {
        dataStore = mock(DataStore.class);
        edgeQueue = new LinkedBlockingQueue<>();
        edgeSaver = new EdgeSaver(edgeQueue, dataStore, 1, null, taskContinue);
    }
    
    @Test
    @DisplayName("엣지 저장이 정상적으로 동작하는지 확인")
    public void testEdgeSaveAndRead() throws InterruptedException, IOException {
        Edge edge1 = new Edge(1, 0, 1, 100, 24, RoadLevel.L0);
        Edge edge2 = new Edge(2, 1, 2, 150, 48, RoadLevel.L1);
        Edge edge3 = new Edge(3, 2, 3, 200, 72, RoadLevel.L2);
        
        edgeQueue.put(new EdgeItem(edge1));
        edgeQueue.put(new EdgeItem(edge2));
        edgeQueue.put(new EdgeItem(edge3));
        edgeQueue.put(new EndItem(0));
        
        edgeSaver.run();
        
        verify(dataStore, times(3)).saveEdge(any(Edge.class));
    }
    
    @Test
    @DisplayName("빈 큐(엣지 없음)에서 EndItem만 있는 테스트")
    public void testEmptyQueueWithEndItem() throws InterruptedException, IOException {
        edgeQueue.put(new EndItem(0));
        
        edgeSaver.run();
        
        verify(dataStore, times(0)).saveEdge(any(Edge.class));
    }
    
    @Test
    @DisplayName("다중 엣지 저장 테스트")
    public void testMultipleEdgeSave() throws InterruptedException, IOException {
        for (int i = 0; i < 5; i++) {
            Edge edge = new Edge(i, i, i + 1, 100 * i, 24 * i, RoadLevel.L0);
            edgeQueue.put(new EdgeItem(edge));
        }
        edgeQueue.put(new EndItem(0));
        
        edgeSaver.run();
        
        verify(dataStore, times(5)).saveEdge(any(Edge.class));
    }
    
    @Test
    @DisplayName("인터럽트 발생 테스트")
    public void testInterruptionHandling() throws InterruptedException {
        Edge edge1 = new Edge(1, 0, 1, 100, 24, RoadLevel.L0);
        Edge edge2 = new Edge(2, 1, 2, 150, 48, RoadLevel.L1);
        
        BlockingQueue<TaskItem> slowQueue = new LinkedBlockingQueue<>();
        slowQueue.put(new EdgeItem(edge1));
        
        EdgeSaver saver = new EdgeSaver(slowQueue, dataStore, 1, null, taskContinue);
        
        Thread thread = new Thread(saver);
        thread.start();
        
        Thread.sleep(100);
        thread.interrupt();
        thread.join(1000);
        
        assertThat(thread.isAlive()).isFalse();
    }
    
    @Test
    @DisplayName("IOException 발생 시 처리 테스트")
    public void testIOExceptionHandling() throws InterruptedException, IOException {
        Edge edge = new Edge(1, 0, 1, 100, 24, RoadLevel.L0);
        
        DataStore failingStore = mock(DataStore.class);
        doThrow(new IOException("Save failed")).when(failingStore).saveEdge(any(Edge.class));
        
        BlockingQueue<TaskItem> queue = new LinkedBlockingQueue<>();
        queue.put(new EdgeItem(edge));
        queue.put(new EndItem(0));
        
        EdgeSaver saver = new EdgeSaver(queue, failingStore, 1, null, taskContinue);
        saver.run();
        
        verify(failingStore).saveEdge(edge);
    }

    
    @Test
    @DisplayName("EndItem 수신 후 정상 종료 테스트")
    public void testNormalTermination() throws InterruptedException {
        Edge edge = new Edge(1, 0, 1, 100, 24, RoadLevel.L0);
        
        edgeQueue.put(new EdgeItem(edge));
        edgeQueue.put(new EndItem(0));
        
        Thread thread = new Thread(edgeSaver);
        thread.start();
        thread.join(1000);
        
        assertThat(thread.isAlive()).isFalse();
    }
}
