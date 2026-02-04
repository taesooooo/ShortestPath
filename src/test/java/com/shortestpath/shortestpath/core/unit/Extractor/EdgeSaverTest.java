package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private BlockingQueue<List<TaskItem>> edgeQueue;
    private AtomicBoolean taskContinue;
    private AtomicBoolean taskError;
    private AtomicInteger totalSavedEdgeCount;
    
    @BeforeEach
    public void setUp() {
        dataStore = mock(DataStore.class);
        edgeQueue = new LinkedBlockingQueue<>();
        taskContinue = new AtomicBoolean(true);
        taskError = new AtomicBoolean(false);
        totalSavedEdgeCount = new AtomicInteger(0);
        edgeSaver = new EdgeSaver(edgeQueue, dataStore, null, taskContinue, taskError, totalSavedEdgeCount);
    }
    
    @Test
    @DisplayName("엣지 저장이 정상적으로 동작하는지 확인")
    public void testEdgeSaveAndRead() throws InterruptedException, IOException {
        Edge edge1 = new Edge(1, 0, 1, 100, 24, RoadLevel.L0);
        Edge edge2 = new Edge(2, 1, 2, 150, 48, RoadLevel.L1);
        Edge edge3 = new Edge(3, 2, 3, 200, 72, RoadLevel.L2);
        
        ArrayList<TaskItem> edgeItems = new ArrayList<>();
        edgeItems.add(new EdgeItem(edge1));
        edgeItems.add(new EdgeItem(edge2));
        edgeItems.add(new EdgeItem(edge3));

        edgeQueue.put(edgeItems);

        taskContinue.set(false);

        edgeQueue.put(Arrays.asList(new EndItem(0)));
        
        edgeSaver.run();

        verify(dataStore, times(3)).saveEdge(any(Edge.class));
    }
    
    @Test
    @DisplayName("다중 엣지 저장 테스트")
    public void testMultipleEdgeSave() throws InterruptedException, IOException {
        ArrayList<TaskItem> edgeItems = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Edge edge = new Edge(i, i, i + 1, 100 * i, 24 * i, RoadLevel.L0);
            edgeItems.add(new EdgeItem(edge));
        }
        edgeQueue.put(edgeItems);

        taskContinue.set(false);
        
        edgeQueue.put(Arrays.asList(new EndItem(0)));
        
        edgeSaver.run();

        verify(dataStore, times(5)).saveEdge(any(Edge.class));
    }
    
    @Test
    @DisplayName("인터럽트 발생 테스트")
    public void testInterruptionHandling() throws InterruptedException {
        Edge edge1 = new Edge(1, 0, 1, 100, 24, RoadLevel.L0);
        Edge edge2 = new Edge(2, 1, 2, 150, 48, RoadLevel.L1);
        
        BlockingQueue<List<TaskItem>> slowQueue = new LinkedBlockingQueue<>();
        slowQueue.put(new ArrayList<TaskItem>(List.of(new EdgeItem(edge1), new EdgeItem(edge2))));
        
        EdgeSaver saver = new EdgeSaver(slowQueue, dataStore, null, taskContinue, taskError, totalSavedEdgeCount);
        
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
        
        BlockingQueue<List<TaskItem>> queue = new LinkedBlockingQueue<>();
        queue.put(new ArrayList<TaskItem>(List.of(new EdgeItem(edge))));
        
        EdgeSaver saver = new EdgeSaver(queue, failingStore, null, taskContinue, taskError, totalSavedEdgeCount);
        saver.run();
        
        verify(failingStore).saveEdge(edge);
    }
}
