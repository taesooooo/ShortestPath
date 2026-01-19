package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.GeometryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.mockito.MockedStatic;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeCreator;
import com.shortestpath.shortestpath.core.pathengine.Extractor.ProgressStatus;
import com.shortestpath.shortestpath.core.pathengine.Extractor.TaskType;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeEdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

public class NodeCreatorTest {

    private DefaultFeatureCollection collection;
    private BlockingQueue<TaskItem> nodeEdgeQueue;
    private ProgressStatus progressStatus;
    private DataStore dataStore;
    private long[] idArray;
    private boolean[] nodeCreated;
    private AtomicBoolean shouldContinue;

    @BeforeEach
    public void setUp() {
        collection = new DefaultFeatureCollection();
        nodeEdgeQueue = new LinkedBlockingQueue<>();
        progressStatus = mock(ProgressStatus.class);
        dataStore = mock(DataStore.class);
        idArray = new long[100];
        nodeCreated = new boolean[100];
        shouldContinue = new AtomicBoolean(true);
    }

    @Test
    @DisplayName("NodeCreator 생성 테스트 - 기본 생성자")
    public void nodeCreatorConstructorTest() {
        NodeCreator creator = new NodeCreator(collection, idArray, nodeCreated, nodeEdgeQueue, dataStore, progressStatus, shouldContinue);
        
        assertThat(creator).isNotNull();
    }

    @Test
    @DisplayName("NodeCreator 실행 테스트 - 빈 컬렉션")
    public void nodeCreatorRunEmptyCollectionTest() throws InterruptedException {
        NodeCreator creator = new NodeCreator(collection, idArray, nodeCreated, nodeEdgeQueue, dataStore, null, shouldContinue);
        
        creator.run();
        
        TaskItem item = nodeEdgeQueue.poll();
        assertThat(item).isInstanceOf(EndItem.class);
    }

    @Test
    @DisplayName("NodeCreator 실행 테스트 - LineString 처리")
    public void nodeCreatorRunWithLineStringTest() throws InterruptedException, IOException {
        // Arrange
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("TestLineString");
        typeBuilder.add("geometry", LineString.class);
        SimpleFeatureType featureType = typeBuilder.buildFeatureType();

        GeometryBuilder geometryBuilder = new GeometryBuilder();
        LineString lineString = geometryBuilder.lineString(37.0, 127.0, 37.1, 127.1, 37.2, 127.2);

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", lineString);
        SimpleFeature feature = featureBuilder.buildFeature(null);

        collection.add(feature);

        // idArray 설정
        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.coordinateToLong(any()))
                .thenReturn(1L, 2L, 2L, 3L);

            // 정렬된 idArray 설정
            Arrays.fill(idArray, 10);
            idArray[0] = 1L;
            idArray[1] = 2L;
            idArray[2] = 3L;

            when(dataStore.readNode(1)).thenReturn(new Node(1, new Coordinate(37.1, 127.1), -1, 0, 0, 0));

            NodeCreator creator = new NodeCreator(collection, idArray, nodeCreated, nodeEdgeQueue, dataStore, null, shouldContinue);

            // Act
            creator.run();

            // Assert
            TaskItem item1 = nodeEdgeQueue.poll();
            assertThat(item1).isInstanceOf(NodeEdgeItem.class);
            
            NodeEdgeItem nodeEdgeItem = (NodeEdgeItem) item1;
            assertThat(nodeEdgeItem.getNodeA()).isNotNull();
            assertThat(nodeEdgeItem.getNodeB()).isNotNull();

            TaskItem item2 = nodeEdgeQueue.poll();
            NodeEdgeItem nodeEdgeItem2 = (NodeEdgeItem) item2;
            assertThat(nodeEdgeItem2.getNodeA()).isNotNull(); // 이미 생성된 노드
            assertThat(nodeEdgeItem2.getNodeB()).isNotNull();

            TaskItem endItem = nodeEdgeQueue.poll();
            assertThat(endItem).isInstanceOf(EndItem.class);
        }
    }

    @Test
    @DisplayName("NodeCreator 실행 테스트 - 진행률 업데이트")
    public void nodeCreatorProgressUpdateTest() throws InterruptedException, IOException {
        // Arrange
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("TestLineString");
        typeBuilder.add("geometry", LineString.class);
        SimpleFeatureType featureType = typeBuilder.buildFeatureType();

        GeometryBuilder geometryBuilder = new GeometryBuilder();
        LineString lineString = geometryBuilder.lineString(37.0, 127.0, 37.1, 127.1, 37.2, 127.2);

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", lineString);
        SimpleFeature feature = featureBuilder.buildFeature(null);

        collection.add(feature);

        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.coordinateToLong(any()))
                .thenReturn(1L, 2L, 2L, 3L);

            Arrays.fill(idArray, 10);
            idArray[0] = 1L;
            idArray[1] = 2L;
            idArray[2] = 3L;

            ProgressStatus mockProgress = mock(ProgressStatus.class);
            NodeCreator creator = new NodeCreator(collection, idArray, nodeCreated, nodeEdgeQueue, dataStore, mockProgress, shouldContinue);

            // Act
            creator.run();

            // Assert - 진행률이 업데이트되었는지 확인
            verify(mockProgress, org.mockito.Mockito.atLeastOnce()).progress(any(TaskType.class), anyInt(), anyInt());
        }
    }

    @Test
    @DisplayName("NodeCreator 실행 테스트 - shouldContinue 플래그 확인")
    public void nodeCreatorShouldContinueFlagTest() throws InterruptedException, IOException {
        // Arrange
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("TestLineString");
        typeBuilder.add("geometry", LineString.class);
        SimpleFeatureType featureType = typeBuilder.buildFeatureType();

        GeometryBuilder geometryBuilder = new GeometryBuilder();
        LineString lineString = geometryBuilder.lineString(37.0, 127.0, 37.1, 127.1);

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", lineString);
        SimpleFeature feature = featureBuilder.buildFeature(null);

        collection.add(feature);

        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.coordinateToLong(any()))
                .thenReturn(1L, 2L);

            Arrays.fill(idArray, 10);
            idArray[0] = 1L;
            idArray[1] = 2L;

            AtomicBoolean shouldContinueFlag = new AtomicBoolean(true);
            NodeCreator creator = new NodeCreator(collection, idArray, nodeCreated, nodeEdgeQueue, dataStore, null, shouldContinueFlag);

            // Act
            creator.run();

            // Assert
            assertThat(shouldContinueFlag.get()).isTrue();
        }
    }

    @Test
    @DisplayName("NodeCreator 실행 테스트 - 중단 플래그")
    public void nodeCreatorInterruptedTest() throws InterruptedException, IOException {
        // Arrange
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("TestLineString");
        typeBuilder.add("geometry", LineString.class);
        SimpleFeatureType featureType = typeBuilder.buildFeatureType();

        GeometryBuilder geometryBuilder = new GeometryBuilder();
        LineString lineString = geometryBuilder.lineString(37.0, 127.0, 37.1, 127.1);

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", lineString);
        SimpleFeature feature = featureBuilder.buildFeature(null);

        collection.add(feature);

        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.coordinateToLong(any()))
                .thenReturn(1L, 2L);

            Arrays.fill(idArray, 10);
            idArray[0] = 1L;
            idArray[1] = 2L;

            AtomicBoolean shouldContinueFlag = new AtomicBoolean(false);
            NodeCreator creator = new NodeCreator(collection, idArray, nodeCreated, nodeEdgeQueue, dataStore, null, shouldContinueFlag);

            // Act
            creator.run();

            // Assert
            TaskItem item = nodeEdgeQueue.poll();
            assertThat(item).isInstanceOf(EndItem.class);
        }
    }

    @Test
    @DisplayName("NodeCreator 실행 테스트 - 노드 생성 상태 확인")
    public void nodeCreatorNodeCreationStatusTest() throws InterruptedException, IOException {
        // Arrange
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("TestLineString");
        typeBuilder.add("geometry", LineString.class);
        SimpleFeatureType featureType = typeBuilder.buildFeatureType();

        GeometryBuilder geometryBuilder = new GeometryBuilder();
        LineString lineString = geometryBuilder.lineString(37.0, 127.0, 37.1, 127.1, 37.2, 127.2);

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", lineString);
        SimpleFeature feature = featureBuilder.buildFeature(null);

        collection.add(feature);

        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.coordinateToLong(any()))
                .thenReturn(1L, 2L, 2L, 3L);

            Arrays.fill(idArray, 10);
            idArray[0] = 1L;
            idArray[1] = 2L;
            idArray[2] = 3L;

            boolean[] nodeCreatedBefore = new boolean[100];
            NodeCreator creator = new NodeCreator(collection, idArray, nodeCreatedBefore, nodeEdgeQueue, dataStore, null, shouldContinue);

            // Act
            creator.run();

            // Assert
            assertThat(nodeCreatedBefore[0]).isTrue();
            assertThat(nodeCreatedBefore[1]).isTrue();
            assertThat(nodeCreatedBefore[2]).isTrue();
        }
    }

    @Test
    @DisplayName("NodeCreator 실행 테스트 - IOException 처리")
    public void nodeCreatorIOExceptionTest() throws InterruptedException, IOException {
        // Arrange
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("TestLineString");
        typeBuilder.add("geometry", LineString.class);
        SimpleFeatureType featureType = typeBuilder.buildFeatureType();

        GeometryBuilder geometryBuilder = new GeometryBuilder();
        LineString lineString = geometryBuilder.lineString(37.0, 127.0, 37.1, 127.1);

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", lineString);
        SimpleFeature feature = featureBuilder.buildFeature(null);

        collection.add(feature);

        DataStore mockDataStore = mock(DataStore.class);
        when(mockDataStore.saveNode(any(Node.class))).thenThrow(new IOException("Test IOException"));

        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.coordinateToLong(any()))
                .thenReturn(1L, 2L);

            Arrays.fill(idArray, 10);
            idArray[0] = 1L;
            idArray[1] = 2L;

            AtomicBoolean shouldContinueFlag = new AtomicBoolean(true);
            NodeCreator creator = new NodeCreator(collection, idArray, nodeCreated, nodeEdgeQueue, mockDataStore, null, shouldContinueFlag);

            // Act
            creator.run();

            // Assert - shouldContinue가 false로 설정되어야 함
            assertThat(shouldContinueFlag.get()).isFalse();
        }
    }
}
