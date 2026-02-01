package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
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

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeSaver;
import com.shortestpath.shortestpath.core.pathengine.Extractor.ProgressStatus;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

@DisplayName("NodeSaver 단위 테스트")
public class NodeSaverTest {
    
    private NodeSaver nodeSaver;
    private DataStore dataStore;
    private BlockingQueue<TaskItem> nodeQueue;
    private boolean[] nodeCreated;
    private ProgressStatus progressStatus;
    private AtomicBoolean taskContinue;
    @BeforeEach
    public void setUp() {
        dataStore = mock(DataStore.class);
        nodeQueue = new LinkedBlockingQueue<>();
        nodeCreated = new boolean[10];
        progressStatus = mock(ProgressStatus.class);
        taskContinue = new AtomicBoolean(true);
        nodeSaver = new NodeSaver(nodeQueue, nodeCreated, dataStore, 1, progressStatus, taskContinue);
    }
    
    @Test
    @DisplayName("노드를 정상적으로 저장하고 읽는 테스트")
    public void testSaveAndReadNodes() throws InterruptedException, IOException {
        Node nodeA = new Node(0, new Coordinate(37.0, 127.0));
        Node nodeB = new Node(1, new Coordinate(37.1, 127.1));
        Node nodeC = new Node(2, new Coordinate(37.2, 127.2));
        
        nodeQueue.put(new NodeItem(nodeA, nodeB));
        nodeQueue.put(new NodeItem(nodeB, nodeC));
        nodeQueue.put(new EndItem(0));
        
        nodeSaver.run();
        
        verify(dataStore).saveNode(nodeA, 0 * DataStructureSizes.NODE_SIZE);
        verify(dataStore).saveNode(nodeB, 1 * DataStructureSizes.NODE_SIZE);
        verify(dataStore).saveNode(nodeC, 2 * DataStructureSizes.NODE_SIZE);
        
        assertThat(nodeSaver.getSavedNodeCount()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("중복되지 않은 노드만 저장되는지 확인")
    public void testDuplicateNodeNotSaved() throws InterruptedException, IOException {
        Node nodeA = new Node(0, new Coordinate(37.0, 127.0));
        Node nodeB = new Node(1, new Coordinate(37.1, 127.1));
        
        nodeCreated[0] = true;
        nodeQueue.put(new NodeItem(nodeA, nodeB));
        nodeQueue.put(new NodeItem(nodeA, nodeB));
        nodeQueue.put(new EndItem(0));
        
        nodeSaver.run();
        
        verify(dataStore, times(1)).saveNode(any(Node.class), anyLong());
    }
    
    @Test
    @DisplayName("EndItem 수신 후 정상 종료")
    public void testNormalTermination() throws InterruptedException {
        Node nodeA = new Node(0, new Coordinate(37.0, 127.0));
        Node nodeB = new Node(1, new Coordinate(37.1, 127.1));
        
        nodeQueue.put(new NodeItem(nodeA, nodeB));
        nodeQueue.put(new EndItem(0));
        
        Thread thread = new Thread(nodeSaver);
        thread.start();
        thread.join(1000);
        
        assertThat(thread.isAlive()).isFalse();
    }
}
