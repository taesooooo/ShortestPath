package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.EdgeCreator;
import com.shortestpath.shortestpath.core.pathengine.Extractor.ProgressStatus;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeEdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

public class EdgeCreatorTest {

    private DataStore dataStore;
    private long[] idArray;
    private boolean[] nodeCreated;
    private int[] lastEdgeOffsetArray;
    private BlockingQueue<TaskItem> nodeEdgeQueue;
    private ProgressStatus progressStatus;
    private AtomicBoolean shouldContinue;

    @BeforeEach
    public void setUp() {
        dataStore = mock(DataStore.class);
        idArray = new long[100];
        nodeCreated = new boolean[100];
        lastEdgeOffsetArray = new int[100];
        nodeEdgeQueue = new LinkedBlockingQueue<>();
        progressStatus = mock(ProgressStatus.class);
        shouldContinue = new AtomicBoolean(true);

        // lastEdgeOffsetArray 초기화
        Arrays.fill(lastEdgeOffsetArray, -1);
    }

    @Test
    @DisplayName("EdgeCreator 생성 테스트 - 기본 생성자")
    public void edgeCreatorConstructorTest() {
        EdgeCreator creator = new EdgeCreator(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, progressStatus, shouldContinue);
        
        assertThat(creator).isNotNull();
    }

    @Test
    @DisplayName("EdgeCreator 실행 테스트 - EndItem 수신")
    public void edgeCreatorRunWithEndItemTest() throws InterruptedException, IOException {
        EdgeCreator creator = new EdgeCreator(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, progressStatus, shouldContinue);
        
        // EndItem을 큐에 추가
        nodeEdgeQueue.put(new EndItem());
        
        // Act
        creator.run();
        
        // Assert
        verify(dataStore, times(0)).saveEdge(any(Edge.class));
    }

    @Test
    @DisplayName("EdgeCreator 실행 테스트 - NodeEdgeItem 처리")
    public void edgeCreatorRunWithNodeEdgeItemTest() throws InterruptedException, IOException {
        // Arrange
        Coordinate coordA = new Coordinate(37.0, 127.0);
        Coordinate coordB = new Coordinate(37.1, 127.1);
        
        Node nodeA = new Node(0, coordA, -1, 0, 0, 0);
        Node nodeB = new Node(1, coordB, -1, 0, 0, 0);
        
        NodeEdgeItem nodeEdgeItem = new NodeEdgeItem(nodeA, nodeB);
        
        nodeEdgeQueue.put(nodeEdgeItem);
        nodeEdgeQueue.put(new EndItem());

        when(dataStore.saveEdge(any(Edge.class))).thenReturn(0);

        EdgeCreator creator = new EdgeCreator(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, progressStatus, shouldContinue);

        // Act
        creator.run();

        // Assert
        ArgumentCaptor<Edge> edgeCaptor = ArgumentCaptor.forClass(Edge.class);
        verify(dataStore, times(2)).saveEdge(edgeCaptor.capture());
        
        assertThat(edgeCaptor.getAllValues()).isNotNull();
        assertThat(edgeCaptor.getAllValues()).extracting(Edge::getFrom).containsExactly(0, 1);
    }

    @Test
    @DisplayName("EdgeCreator 실행 테스트 - 양방향 엣지 생성")
    public void edgeCreatorBidirectionalEdgeTest() throws InterruptedException, IOException {
        // Arrange
        Coordinate coordA = new Coordinate(37.0, 127.0);
        Coordinate coordB = new Coordinate(37.1, 127.1);
        
        Node nodeA = new Node(0, coordA, -1, 0, 0, 0);
        Node nodeB = new Node(1, coordB, -1, 0, 0, 0);
        
        NodeEdgeItem nodeEdgeItem = new NodeEdgeItem(nodeA, nodeB);
        
        nodeEdgeQueue.put(nodeEdgeItem);
        nodeEdgeQueue.put(new EndItem());

        when(dataStore.saveEdge(any(Edge.class))).thenReturn(0).thenReturn(1);

        EdgeCreator creator = new EdgeCreator(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, progressStatus, shouldContinue);

        // Act
        creator.run();

        // Assert - saveEdge가 2번 호출되어야 함 (양방향)
        verify(dataStore, times(2)).saveEdge(any(Edge.class));
    }

    @Test
    @DisplayName("EdgeCreator 실행 테스트 - 노드 시작 엣지 오프셋 설정")
    public void edgeCreatorSetStartEdgeOffsetTest() throws InterruptedException, IOException {
        // Arrange
        Coordinate coordA = new Coordinate(37.0, 127.0);
        Coordinate coordB = new Coordinate(37.1, 127.1);
        
        Node nodeA = new Node(0, coordA, -1, 0, 0, 0);
        Node nodeB = new Node(1, coordB, -1, 0, 0, 0);
        
        NodeEdgeItem nodeEdgeItem = new NodeEdgeItem(nodeA, nodeB);
        
        nodeEdgeQueue.put(nodeEdgeItem);
        nodeEdgeQueue.put(new EndItem());

        when(dataStore.saveEdge(any(Edge.class))).thenReturn(0).thenReturn(1);

        EdgeCreator creator = new EdgeCreator(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, progressStatus, shouldContinue);

        // Act
        creator.run();

        // Assert - overwriteNode가 호출되어 시작 엣지 오프셋이 설정되어야 함
        verify(dataStore, times(2)).overwriteNode(any(Node.class), anyLong());
    }

    @Test
    @DisplayName("EdgeCreator 실행 테스트 - 다음 엣지 오프셋 설정")
    public void edgeCreatorSetNextEdgeOffsetTest() throws InterruptedException, IOException {
        // Arrange
        Coordinate coordA = new Coordinate(37.0, 127.0);
        Coordinate coordB = new Coordinate(37.1, 127.1);
        Coordinate coordC = new Coordinate(37.2, 127.2);
        
        Node nodeA = new Node(0, coordA, 0, 0, 0, 0);
        Node nodeB = new Node(1, coordB, 1, 0, 0, 0);
        Node nodeC = new Node(2, coordC, 2, 0, 0, 0);
        
        Edge edge = new Edge(1, 1, 0, 0, -1);
        
        nodeEdgeQueue.put(new NodeEdgeItem(nodeA, nodeB));
        nodeEdgeQueue.put(new NodeEdgeItem(nodeB, nodeC));
        nodeEdgeQueue.put(new EndItem());

        when(dataStore.saveEdge(any(Edge.class))).thenReturn(0).thenReturn(1).thenReturn(2).thenReturn(3);
        when(dataStore.readEdge(anyLong())).thenReturn(edge);

        EdgeCreator creator = new EdgeCreator(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, progressStatus, shouldContinue);

        // Act
        creator.run();

        // Assert
        assertThat(lastEdgeOffsetArray[1]).isEqualTo(2);
    }

    @Test
    @DisplayName("EdgeCreator 실행 테스트 - shouldContinue 플래그")
    public void edgeCreatorShouldContinueFlagTest() throws InterruptedException, IOException {
        // Arrange
        AtomicBoolean shouldContinueFlag = new AtomicBoolean(true);
        
        nodeEdgeQueue.put(new EndItem());

        EdgeCreator creator = new EdgeCreator(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, progressStatus, shouldContinueFlag);

        // Act
        creator.run();

        // Assert
        assertThat(shouldContinueFlag.get()).isTrue();
    }

    @Test
    @DisplayName("EdgeCreator 실행 테스트 - InterruptedException 처리")
    public void edgeCreatorInterruptedExceptionTest() throws InterruptedException, IOException {
        // Arrange
        AtomicBoolean shouldContinueFlag = new AtomicBoolean(true);
        
        BlockingQueue<TaskItem> mockQueue = mock(BlockingQueue.class);
        when(mockQueue.take()).thenThrow(new InterruptedException("Test interrupt"));

        EdgeCreator creator = new EdgeCreator(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, mockQueue, progressStatus, shouldContinueFlag);

        // Act
        creator.run();

        // Assert
        assertThat(shouldContinueFlag.get()).isFalse();
    }

    @Test
    @DisplayName("EdgeCreator 실행 테스트 - IOException 처리")
    public void edgeCreatorIOExceptionTest() throws InterruptedException, IOException {
        // Arrange
        Coordinate coordA = new Coordinate(37.0, 127.0);
        Coordinate coordB = new Coordinate(37.1, 127.1);
        
        Node nodeA = new Node(0, coordA, -1, 0, 0, 0);
        Node nodeB = new Node(1, coordB, -1, 0, 0, 0);
        
        NodeEdgeItem nodeEdgeItem = new NodeEdgeItem(nodeA, nodeB);
        
        nodeEdgeQueue.put(nodeEdgeItem);

        when(dataStore.saveEdge(any(Edge.class))).thenThrow(new IOException("Test IOException"));

        AtomicBoolean shouldContinueFlag = new AtomicBoolean(true);
        EdgeCreator creator = new EdgeCreator(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, progressStatus, shouldContinueFlag);

        // Act
        creator.run();

        // Assert
        assertThat(shouldContinueFlag.get()).isFalse();
    }

    @Test
    @DisplayName("EdgeCreator 실행 테스트 - 여러 NodeEdgeItem 처리")
    public void edgeCreatorMultipleNodeEdgeItemsTest() throws InterruptedException, IOException {
        // Arrange
        Coordinate coordA = new Coordinate(37.0, 127.0);
        Coordinate coordB = new Coordinate(37.1, 127.1);
        Coordinate coordC = new Coordinate(37.2, 127.2);
        
        Node nodeA = new Node(0, coordA, -1, 0, 0, 0);
        Node nodeB = new Node(1, coordB, -1, 0, 0, 0);
        Node nodeC = new Node(2, coordC, -1, 0, 0, 0);
        
        NodeEdgeItem item1 = new NodeEdgeItem(nodeA, nodeB);
        NodeEdgeItem item2 = new NodeEdgeItem(nodeB, nodeC);
        
        nodeEdgeQueue.put(item1);
        nodeEdgeQueue.put(item2);
        nodeEdgeQueue.put(new EndItem());

        when(dataStore.saveEdge(any(Edge.class))).thenReturn(0).thenReturn(1).thenReturn(2).thenReturn(3);

        EdgeCreator creator = new EdgeCreator(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, progressStatus, shouldContinue);

        // Act
        creator.run();

        // Assert - 4개의 엣지가 저장되어야 함 (2개 아이템 x 2 양방향)
        verify(dataStore, org.mockito.Mockito.times(4)).saveEdge(any(Edge.class));
    }

    @Test
    @DisplayName("EdgeCreator 실행 테스트 - 엣지 거리 계산")
    public void edgeCreatorDistanceCalculationTest() throws InterruptedException, IOException {
        // Arrange
        Coordinate coordA = new Coordinate(37.0, 127.0);
        Coordinate coordB = new Coordinate(37.1, 127.1);
        
        Node nodeA = new Node(0, coordA, -1, 0, 0, 0);
        Node nodeB = new Node(1, coordB, -1, 0, 0, 0);
        
        NodeEdgeItem nodeEdgeItem = new NodeEdgeItem(nodeA, nodeB);
        
        nodeEdgeQueue.put(nodeEdgeItem);
        nodeEdgeQueue.put(new EndItem());

        when(dataStore.saveEdge(any(Edge.class))).thenReturn(0).thenReturn(1);

        EdgeCreator creator = new EdgeCreator(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, progressStatus, shouldContinue);

        // Act
        creator.run();

        // Assert
        ArgumentCaptor<Edge> edgeCaptor = ArgumentCaptor.forClass(Edge.class);
        verify(dataStore, org.mockito.Mockito.times(2)).saveEdge(edgeCaptor.capture());
        
        assertThat(edgeCaptor.getAllValues()).allMatch(edge -> edge.getDistance() > 0);
    }

    @Test
    @DisplayName("EdgeCreator 실행 테스트 - 엣지 ID 증가")
    public void edgeCreatorEdgeIdIncrementTest() throws InterruptedException, IOException {
        // Arrange
        Coordinate coordA = new Coordinate(37.0, 127.0);
        Coordinate coordB = new Coordinate(37.1, 127.1);
        Coordinate coordC = new Coordinate(37.2, 127.2);
        
        Node nodeA = new Node(0, coordA, -1, 0, 0, 0);
        Node nodeB = new Node(1, coordB, -1, 0, 0, 0);
        Node nodeC = new Node(2, coordC, -1, 0, 0, 0);
        
        nodeEdgeQueue.put(new NodeEdgeItem(nodeA, nodeB));
        nodeEdgeQueue.put(new NodeEdgeItem(nodeB, nodeC));
        nodeEdgeQueue.put(new EndItem());

        when(dataStore.saveEdge(any(Edge.class))).thenReturn(0).thenReturn(1).thenReturn(2).thenReturn(3);

        EdgeCreator creator = new EdgeCreator(dataStore, idArray, nodeCreated, lastEdgeOffsetArray, nodeEdgeQueue, progressStatus, shouldContinue);

        // Act
        creator.run();

        // Assert
        ArgumentCaptor<Edge> edgeCaptor = ArgumentCaptor.forClass(Edge.class);
        verify(dataStore, org.mockito.Mockito.times(4)).saveEdge(edgeCaptor.capture());
        
        java.util.List<Edge> edges = edgeCaptor.getAllValues();
        assertThat(edges.get(0).getId()).isEqualTo(0);
        assertThat(edges.get(1).getId()).isEqualTo(1);
        assertThat(edges.get(2).getId()).isEqualTo(2);
        assertThat(edges.get(3).getId()).isEqualTo(3);
    }
}
