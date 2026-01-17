package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeSave;
import com.shortestpath.shortestpath.core.pathengine.Extractor.ProgressStatus;
import com.shortestpath.shortestpath.core.pathengine.Extractor.TaskType;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeCSVItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeEdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

public class NodeEdgeSaveTest {

    private DataStore dataStore;
    private BlockingQueue<TaskItem> nodeEdgeQueue;
    private BlockingQueue<TaskItem> csvQueue;
    private ProgressStatus progressStatus;
    private long[] idArray;
    private boolean[] nodeCreated;
    private int[] lastEdgeOffsetArray;
    private AtomicBoolean shouldContinue;

    @BeforeEach
    public void setUp() {
        dataStore = mock(DataStore.class);
        nodeEdgeQueue = new LinkedBlockingQueue<>();
        csvQueue = new LinkedBlockingQueue<>();
        progressStatus = mock(ProgressStatus.class);
        idArray = new long[10];
        nodeCreated = new boolean[10];
        lastEdgeOffsetArray = new int[10];
        Arrays.fill(lastEdgeOffsetArray, -1);
        shouldContinue = new AtomicBoolean(true);
    }

    @Test
    @DisplayName("NodeEdgeSave 생성 테스트 - 기본 생성자")
    public void nodeEdgeSaveConstructorTest() {
        NodeEdgeSave save = new NodeEdgeSave(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, csvQueue, progressStatus, shouldContinue);
        
        assertThat(save).isNotNull();
    }

    @Test
    @DisplayName("NodeEdgeSave 실행 테스트 - EndItem 수신")
    public void nodeEdgeSaveRunWithEndItemTest() throws InterruptedException, IOException {
        // Arrange
        nodeEdgeQueue.put(new EndItem());
        
        NodeEdgeSave save = new NodeEdgeSave(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, csvQueue, null, shouldContinue);

        // Act
        save.run();

        // Assert
        TaskItem item = csvQueue.poll();
        assertThat(item).isInstanceOf(EndItem.class);
    }

    @Test
    @DisplayName("NodeEdgeSave 실행 테스트 - Node와 Edge 저장")
    public void nodeEdgeSaveRunWithNodeEdgeTest() throws InterruptedException, IOException {
        // Arrange
        Node nodeA = new Node(0, new Coordinate(0.0, 0.0), -1, 0, 0, 0);
        Node nodeB = new Node(1, new Coordinate(1.0, 1.0), -1, 0, 0, 0);
        Edge edgeA = new Edge(0, 0, 1, 1.414, -1);
        Edge edgeB = new Edge(1, 1, 0, 1.414, -1);

        idArray[0] = 1L;
        idArray[1] = 2L;

        NodeEdgeItem nodeEdgeItem = new NodeEdgeItem(nodeA, nodeB, edgeA, edgeB);
        nodeEdgeQueue.put(nodeEdgeItem);
        nodeEdgeQueue.put(new EndItem());

        when(dataStore.saveEdge(any(Edge.class))).thenReturn(100);
        when(dataStore.saveNode(any(Node.class))).thenReturn(1);

        NodeEdgeSave save = new NodeEdgeSave(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, csvQueue, null, shouldContinue);

        // Act
        save.run();

        // Assert
        verify(dataStore).saveEdge(edgeA);
        verify(dataStore).saveEdge(edgeB);
        verify(dataStore).saveNode(nodeA);
        verify(dataStore).saveNode(nodeB);

        // CSV 큐에 데이터가 추가되었는지 확인
        TaskItem csvItem1 = csvQueue.poll();
        assertThat(csvItem1).isInstanceOf(NodeCSVItem.class);
        
        TaskItem csvItem2 = csvQueue.poll();
        assertThat(csvItem2).isInstanceOf(NodeCSVItem.class);
        
        TaskItem endItem = csvQueue.poll();
        assertThat(endItem).isInstanceOf(EndItem.class);
    }

    @Test
    @DisplayName("NodeEdgeSave 실행 테스트 - null Node 처리")
    public void nodeEdgeSaveRunWithNullNodeTest() throws InterruptedException, IOException {
        // Arrange
        Node nodeB = new Node(1, new Coordinate(1.0, 1.0), -1, 0, 0, 0);
        Edge edgeA = new Edge(0, 0, 1, 1.414, -1);
        Edge edgeB = new Edge(1, 1, 0, 1.414, -1);

        idArray[0] = 1L;
        idArray[1] = 2L;

        NodeEdgeItem nodeEdgeItem = new NodeEdgeItem(null, nodeB, edgeA, edgeB);
        nodeEdgeQueue.put(nodeEdgeItem);
        nodeEdgeQueue.put(new EndItem());

        when(dataStore.saveEdge(any(Edge.class))).thenReturn(100);
        when(dataStore.saveNode(any(Node.class))).thenReturn(1);

        NodeEdgeSave save = new NodeEdgeSave(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, csvQueue, null, shouldContinue);

        // Act
        save.run();

        // Assert
        verify(dataStore).saveEdge(edgeA);
        verify(dataStore).saveEdge(edgeB);
        
        // nodeA가 null이면 nodeB만 저장
        verify(dataStore).saveNode(nodeB);
        
        TaskItem csvItem = csvQueue.poll();
        assertThat(csvItem).isInstanceOf(NodeCSVItem.class);
        
        TaskItem endItem = csvQueue.poll();
        assertThat(endItem).isInstanceOf(EndItem.class);
    }

    @Test
    @DisplayName("NodeEdgeSave 실행 테스트 - 진행률 업데이트")
    public void nodeEdgeSaveProgressUpdateTest() throws InterruptedException, IOException {
        // Arrange
        Node nodeA = new Node(0, new Coordinate(0.0, 0.0), -1, 0, 0, 0);
        Node nodeB = new Node(1, new Coordinate(1.0, 1.0), -1, 0, 0, 0);
        Edge edgeA = new Edge(0, 0, 1, 1.414, -1);
        Edge edgeB = new Edge(1, 1, 0, 1.414, -1);

        idArray[0] = 1L;
        idArray[1] = 2L;

        NodeEdgeItem nodeEdgeItem = new NodeEdgeItem(nodeA, nodeB, edgeA, edgeB);
        nodeEdgeQueue.put(nodeEdgeItem);
        nodeEdgeQueue.put(new EndItem());

        when(dataStore.saveEdge(any(Edge.class))).thenReturn(100);
        when(dataStore.saveNode(any(Node.class))).thenReturn(1);
        when(dataStore.readEdge(anyLong())).thenReturn(mock(Edge.class));

        NodeEdgeSave save = new NodeEdgeSave(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, csvQueue, progressStatus, shouldContinue);

        // Act
        save.run();

        // Assert
        verify(progressStatus).progress(TaskType.NODE_EDGE_SAVE, idArray.length, 1);
    }

    @Test
    @DisplayName("NodeEdgeSave 실행 테스트 - 엣지 연결 (nextEdgeOffset)")
    public void nodeEdgeSaveEdgeLinkingTest() throws InterruptedException, IOException {
        // Arrange
        Node nodeA = new Node(0, new Coordinate(0.0, 0.0), -1, 0, 0, 0);
        Node nodeB = new Node(1, new Coordinate(1.0, 1.0), -1, 0, 0, 0);
        Edge edgeA = new Edge(0, 0, 1, 1.414, -1);
        Edge edgeB = new Edge(1, 1, 0, 1.414, -1);

        idArray[0] = 1L;
        idArray[1] = 2L;

        // lastEdgeOffsetArray 초기값 설정
        lastEdgeOffsetArray[0] = -1;
        lastEdgeOffsetArray[1] = -1;

        NodeEdgeItem nodeEdgeItem = new NodeEdgeItem(nodeA, nodeB, edgeA, edgeB);
        nodeEdgeQueue.put(nodeEdgeItem);
        nodeEdgeQueue.put(new EndItem());

        when(dataStore.saveEdge(any(Edge.class))).thenReturn(100).thenReturn(200);
        when(dataStore.saveNode(any(Node.class))).thenReturn(1);

        NodeEdgeSave save = new NodeEdgeSave(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, csvQueue, null, shouldContinue);

        // Act
        save.run();

        // Assert
        verify(dataStore).saveEdge(edgeA);
        verify(dataStore).saveEdge(edgeB);
        
        // lastEdgeOffsetArray가 업데이트되었는지 확인
        assertThat(lastEdgeOffsetArray[0]).isEqualTo(100);
        assertThat(lastEdgeOffsetArray[1]).isEqualTo(200);
    }

    @Test
    @DisplayName("NodeEdgeSave 실행 테스트 - 스레드 인터럽트")
    public void nodeEdgeSaveThreadInterruptTest() throws InterruptedException, IOException {
        // Arrange
        Node nodeA = new Node(0, new Coordinate(0.0, 0.0), -1, 0, 0, 0);
        Node nodeB = new Node(1, new Coordinate(1.0, 1.0), -1, 0, 0, 0);
        Edge edgeA = new Edge(0, 0, 1, 1.414, -1);
        Edge edgeB = new Edge(1, 1, 0, 1.414, -1);

        idArray[0] = 1L;
        idArray[1] = 2L;

        NodeEdgeItem nodeEdgeItem = new NodeEdgeItem(nodeA, nodeB, edgeA, edgeB);
        nodeEdgeQueue.put(nodeEdgeItem);

        when(dataStore.saveEdge(any(Edge.class))).thenThrow(new IOException("Test IO exception"));

        NodeEdgeSave save = new NodeEdgeSave(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, csvQueue, null, shouldContinue);

        // Act
        Thread t = new Thread(save);
        t.start();
        Thread.sleep(500);
        t.interrupt();
        t.join();

        
        // Assert
        // EndItem이 전송되고 정상 종료되는지 확인
        TaskItem item = csvQueue.poll();
        
        assertThat(item).isInstanceOf(EndItem.class);
        assertThat(t.isAlive()).isFalse();
        
    }

    @Test
    @DisplayName("NodeEdgeSave 실행 테스트 - IOException 처리")
    public void nodeEdgeSaveIOExceptionTest() throws InterruptedException, IOException {
        // Arrange
        Node nodeA = new Node(0, new Coordinate(0.0, 0.0), -1, 0, 0, 0);
        Node nodeB = new Node(1, new Coordinate(1.0, 1.0), -1, 0, 0, 0);
        Edge edgeA = new Edge(0, 0, 1, 1.414, -1);
        Edge edgeB = new Edge(1, 1, 0, 1.414, -1);

        idArray[0] = 1L;
        idArray[1] = 2L;

        NodeEdgeItem nodeEdgeItem = new NodeEdgeItem(nodeA, nodeB, edgeA, edgeB);
        nodeEdgeQueue.put(nodeEdgeItem);

        when(dataStore.saveEdge(any(Edge.class))).thenThrow(new IOException("Save edge failed"));

        NodeEdgeSave save = new NodeEdgeSave(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, csvQueue, null, shouldContinue);

        // Act
        save.run();

        // Assert
        // EndItem이 전송되고 정상 종료되는지 확인
        TaskItem item = csvQueue.poll();
        assertThat(item).isInstanceOf(EndItem.class);
    }
}
