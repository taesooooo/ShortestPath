package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeCreator;
import com.shortestpath.shortestpath.core.pathengine.Extractor.ProgressStatus;
import com.shortestpath.shortestpath.core.pathengine.Extractor.TaskType;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeEdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

public class NodeEdgeCreatorTest {

    private DefaultFeatureCollection collection;
    private BlockingQueue<TaskItem> nodeEdgeQueue;
    private ProgressStatus progressStatus;
    private long[] idArray;
    private boolean[] nodeCreated;
    private AtomicBoolean shouldContinue;

    @BeforeEach
    public void setUp() {
        collection = new DefaultFeatureCollection();
        nodeEdgeQueue = new LinkedBlockingQueue<>();
        progressStatus = mock(ProgressStatus.class);
        idArray = new long[100];
        nodeCreated = new boolean[100];
        shouldContinue = new AtomicBoolean(true);
    }

    @Test
    @DisplayName("NodeEdgeCreator 생성 테스트 - 기본 생성자")
    public void nodeEdgeCreatorConstructorTest() {
        NodeEdgeCreator creator = new NodeEdgeCreator(collection, idArray, nodeCreated, nodeEdgeQueue, progressStatus, shouldContinue);
        
        assertThat(creator).isNotNull();
    }

    @Test
    @DisplayName("NodeEdgeCreator 실행 테스트 - 빈 컬렉션")
    public void nodeEdgeCreatorRunEmptyCollectionTest() throws InterruptedException {
        NodeEdgeCreator creator = new NodeEdgeCreator(collection, idArray, nodeCreated, nodeEdgeQueue, null, shouldContinue);
        
        creator.run();
        
        TaskItem item = nodeEdgeQueue.poll();
        assertThat(item).isInstanceOf(EndItem.class);
    }

    @Test
    @DisplayName("NodeEdgeCreator 실행 테스트 - LineString 처리")
    public void nodeEdgeCreatorRunWithLineStringTest() throws InterruptedException {
        // Arrange
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("TestLineString");
        typeBuilder.add("geometry", LineString.class);
        var featureType = typeBuilder.buildFeatureType();

        GeometryBuilder geometryBuilder = new GeometryBuilder();
        LineString lineString = geometryBuilder.lineString(0.0, 0.0, 1.0, 1.0, 2.0, 2.0);

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", lineString);
        var feature = featureBuilder.buildFeature(null);

        collection.add(feature);

        // idArray 설정 - 좌표를 long으로 변환하여 저장
        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.coordinateToLong(any()))
                .thenReturn(1L, 2L, 2L, 3L);

            // idArray 정렬된 상태로 설정
            Arrays.fill(idArray, 10);
            idArray[0] = 1L;
            idArray[1] = 2L;
            idArray[2] = 3L;

            NodeEdgeCreator creator = new NodeEdgeCreator(collection, idArray, nodeCreated, nodeEdgeQueue, null, shouldContinue);

            // Act
            creator.run();

            // Assert
            TaskItem item1 = nodeEdgeQueue.poll();
            assertThat(item1).isInstanceOf(NodeEdgeItem.class);
            
            NodeEdgeItem nodeEdgeItem = (NodeEdgeItem) item1;
            assertThat(Arrays.asList(
                    nodeEdgeItem.getNodeA(),
                    nodeEdgeItem.getNodeB(),
                    nodeEdgeItem.getEdgeA(),
                    nodeEdgeItem.getEdgeB())).doesNotContainNull();

            TaskItem item2 = nodeEdgeQueue.poll();
            NodeEdgeItem nodeEdgeItem2 = (NodeEdgeItem) item2;
            assertThat(nodeEdgeItem2.getNodeA()).isNull();
            assertThat(Arrays.asList(
                    nodeEdgeItem2.getNodeB(),
                    nodeEdgeItem2.getEdgeA(),
                    nodeEdgeItem2.getEdgeB())).doesNotContainNull();

            TaskItem endItem = nodeEdgeQueue.poll();
            assertThat(endItem).isInstanceOf(EndItem.class);
        }
    }

    @Test
    @DisplayName("NodeEdgeCreator 실행 테스트 - 진행률 업데이트")
    public void nodeEdgeCreatorProgressUpdateTest() throws InterruptedException {
        // Arrange
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("TestLineString");
        typeBuilder.add("geometry", LineString.class);
        var featureType = typeBuilder.buildFeatureType();

        GeometryBuilder geometryBuilder = new GeometryBuilder();
        LineString lineString = geometryBuilder.lineString(0.0, 0.0, 1.0, 1.0, 2.0, 2.0);

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", lineString);
        var feature = featureBuilder.buildFeature(null);

        collection.add(feature);

        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.coordinateToLong(any()))
                .thenReturn(1L, 2L, 3L);

            Arrays.fill(idArray, 10);
            idArray[0] = 1L;
            idArray[1] = 2L;
            idArray[2] = 3L;

            NodeEdgeCreator creator = new NodeEdgeCreator(collection, idArray, nodeCreated, nodeEdgeQueue, progressStatus, shouldContinue);

            // Act
            creator.run();

            // Assert
            verify(progressStatus).progress(TaskType.NODE_EDGE_CREATOR, idArray.length, 1);
        }
    }

    @Test
    @DisplayName("NodeEdgeCreator 실행 테스트 - 노드 중복 생성 방지")
    public void nodeEdgeCreatorDuplicateNodePreventionTest() throws InterruptedException {
        // Arrange
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("TestLineString");
        typeBuilder.add("geometry", LineString.class);
        var featureType = typeBuilder.buildFeatureType();

        GeometryBuilder geometryBuilder = new GeometryBuilder();
        LineString lineString = geometryBuilder.lineString(0.0, 0.0, 1.0, 1.0);

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", lineString);
        var feature = featureBuilder.buildFeature(null);

        collection.add(feature);

        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.coordinateToLong(any()))
                .thenReturn(1L, 2L);

            Arrays.fill(idArray, 10);
            idArray[0] = 1L;
            idArray[1] = 2L;

            NodeEdgeCreator creator = new NodeEdgeCreator(collection, idArray, nodeCreated, nodeEdgeQueue, null, shouldContinue);

            // Act
            creator.run();

            // Assert
            TaskItem item = nodeEdgeQueue.poll();
            NodeEdgeItem nodeEdgeItem = (NodeEdgeItem) item;
            
            // 첫 번째 생성 후 nodeCreated[0]은 true
            assertThat(nodeCreated[0]).isTrue();
            assertThat(nodeCreated[1]).isTrue();
            
            // nodeA는 첫 번째 생성 가능하지만, 두 번째 호출 시 null 반환해야 함
            // (이는 두 번째 엣지에서 같은 노드를 사용할 때의 동작)
        }
    }

    @Test
    @DisplayName("NodeEdgeCreator 실행 테스트 - 스레드 인터럽트")
    public void nodeEdgeCreatorThreadInterruptTest() throws InterruptedException {
        // Arrange
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("TestLineString");
        typeBuilder.add("geometry", LineString.class);
        var featureType = typeBuilder.buildFeatureType();

        GeometryBuilder geometryBuilder = new GeometryBuilder();
        LineString lineString = geometryBuilder.lineString(0.0, 0.0, 1.0, 1.0, 2.0, 2.0);

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", lineString);
        var feature = featureBuilder.buildFeature(null);

        collection.add(feature);

        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.coordinateToLong(any()))
                .thenReturn(1L, 2L, 3L);

            Arrays.fill(idArray, -1);
            idArray[0] = 1L;
            idArray[1] = 2L;
            idArray[2] = 3L;

            NodeEdgeCreator creator = new NodeEdgeCreator(collection, idArray, nodeCreated, nodeEdgeQueue, null, shouldContinue);

            // Act
            Thread t = new Thread(creator);
            t.start();
            Thread.sleep(500);
            t.interrupt();
            t.join();

            // Assert
            assertThat(t.isAlive()).isFalse();
        }
    }
}
