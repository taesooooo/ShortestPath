package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.Extractor.EdgeExtract;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

@DisplayName("EdgeExtract 단위 테스트")
public class EdgeExtractTest {
    private EdgeExtract edgeExtract;
    private long[] idArrays;
    private BlockingQueue<List<TaskItem>> edgeQueue;
    private DataStore dataStore;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
    private SimpleFeatureType featureType;
    private GeometryFactory geometryFactory;
    private AtomicBoolean taskContinue = new AtomicBoolean(true);
    private AtomicBoolean taskError = new AtomicBoolean(false);
    
    @BeforeEach
    public void setUp() {
        idArrays = new long[]{
            GeometryUtil.coordinateToLong(new Coordinate(37.0, 127.0)),
            GeometryUtil.coordinateToLong(new Coordinate(37.1, 127.1)),
            GeometryUtil.coordinateToLong(new Coordinate(37.2, 127.2))
        };
        java.util.Arrays.sort(idArrays);
        
        edgeQueue = new LinkedBlockingQueue<>();
        dataStore = mock(DataStore.class);
        geometryFactory = new GeometryFactory();
        
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Road");
        builder.add("geometry", LineString.class);
        builder.add("fclass", String.class);
        featureType = builder.buildFeatureType();
        
        collection = new DefaultFeatureCollection();
    }
    
    @Test
    @DisplayName("L0 도로 유형 정렬 테스트")
    public void testMotorwayRoadLevel() throws InterruptedException {
        Coordinate[] coords = {
            new Coordinate(127.0, 37.0),
            new Coordinate(127.1, 37.1)
        };
        LineString geometry = geometryFactory.createLineString(coords);
        
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", geometry);
        featureBuilder.set("fclass", "motorway");
        SimpleFeature feature = featureBuilder.buildFeature(null);
        
        ((DefaultFeatureCollection) collection).add(feature);
        
        edgeExtract = new EdgeExtract(idArrays, edgeQueue, dataStore, collection, taskContinue, taskError, null, 1);
        
        edgeExtract.run();
        
        List<TaskItem> items = edgeQueue.take();
        
        assertThat(items).hasSize(2);
    }
    
    @Test
    @DisplayName("L1 도로 유형 정렬 테스트")
    public void testPrimaryRoadLevel() throws InterruptedException {
        Coordinate[] coords = {
            new Coordinate(127.0, 37.0),
            new Coordinate(127.1, 37.1)
        };
        LineString geometry = geometryFactory.createLineString(coords);
        
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", geometry);
        featureBuilder.set("fclass", "primary");
        SimpleFeature feature = featureBuilder.buildFeature(null);
        
        ((DefaultFeatureCollection) collection).add(feature);
        
        edgeExtract = new EdgeExtract(idArrays, edgeQueue, dataStore, collection, taskContinue, taskError, null, 1);
        
        edgeExtract.run();
        
        List<TaskItem> items = edgeQueue.take();
        
        assertThat(items).hasSize(2);
    }
    
    @Test
    @DisplayName("L2 도로 유형 정렬 테스트")
    public void testResidentialRoadLevel() throws InterruptedException {
        Coordinate[] coords = {
            new Coordinate(127.0, 37.0),
            new Coordinate(127.1, 37.1)
        };
        LineString geometry = geometryFactory.createLineString(coords);
        
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", geometry);
        featureBuilder.set("fclass", "residential");
        SimpleFeature feature = featureBuilder.buildFeature(null);
        
        ((DefaultFeatureCollection) collection).add(feature);
        
        edgeExtract = new EdgeExtract(idArrays, edgeQueue, dataStore, collection, taskContinue, taskError, null, 1);
        
        edgeExtract.run();
        
        List<TaskItem> items = edgeQueue.take();
        
        assertThat(items).hasSize(2);
    }
    
    @Test
    @DisplayName("복수의 도로 세그먼트에서 엣지 추출 테스트")
    public void testMultipleSegmentExtraction() throws InterruptedException {
        Coordinate[] coords = {
            new Coordinate(127.0, 37.0),
            new Coordinate(127.1, 37.1),
            new Coordinate(127.2, 37.2)
        };
        LineString geometry = geometryFactory.createLineString(coords);
        
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", geometry);
        featureBuilder.set("fclass", "motorway");
        SimpleFeature feature = featureBuilder.buildFeature(null);
        
        ((DefaultFeatureCollection) collection).add(feature);
        
        edgeExtract = new EdgeExtract(idArrays, edgeQueue, dataStore, collection, taskContinue, taskError, null, 1);
        
        edgeExtract.run();
        
        List<TaskItem> items = edgeQueue.take();
        
        assertThat(items).hasSize(4);
    }
    
    @Test
    @DisplayName("정의되지 않은 도로 유형은 L2로 분류")
    public void testUndefinedRoadTypeDefaultsToL2() throws InterruptedException {
        Coordinate[] coords = {
            new Coordinate(127.0, 37.0),
            new Coordinate(127.1, 37.1)
        };
        LineString geometry = geometryFactory.createLineString(coords);
        
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", geometry);
        featureBuilder.set("fclass", "unknown_type");
        SimpleFeature feature = featureBuilder.buildFeature(null);
        
        ((DefaultFeatureCollection) collection).add(feature);
        
        edgeExtract = new EdgeExtract(idArrays, edgeQueue, dataStore, collection, taskContinue, taskError, null,1);
        
        edgeExtract.run();
        
        List<TaskItem> items = edgeQueue.take();
        
        assertThat(items).hasSize(2);
        assertThat(items).allMatch(edgeItem -> ((EdgeItem)edgeItem).getEdge().getRoadLevel() == RoadLevel.L2);
    }
    
    
    @Test
    @DisplayName("여러 도로 피처에서 엣지 추출 테스트")
    public void testMultipleFeaturesExtraction() throws InterruptedException {
        Coordinate[] coords1 = {
            new Coordinate(127.0, 37.0),
            new Coordinate(127.1, 37.1)
        };
        Coordinate[] coords2 = {
            new Coordinate(127.2, 37.2),
            new Coordinate(127.3, 37.3)
        };
        
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        
        featureBuilder.set("geometry", geometryFactory.createLineString(coords1));
        featureBuilder.set("fclass", "motorway");
        SimpleFeature feature1 = featureBuilder.buildFeature(null);
        ((DefaultFeatureCollection) collection).add(feature1);
        
        featureBuilder.set("geometry", geometryFactory.createLineString(coords2));
        featureBuilder.set("fclass", "residential");
        SimpleFeature feature2 = featureBuilder.buildFeature(null);
        ((DefaultFeatureCollection) collection).add(feature2);
        
        edgeExtract = new EdgeExtract(idArrays, edgeQueue, dataStore, collection, taskContinue, taskError, null, 1);
        
        edgeExtract.run();
        
        List<TaskItem> items = edgeQueue.take();
        
        assertThat(items).hasSize(4);
    }
    
    @Test
    @DisplayName("도로 유형이 null인 경우 기본값(unclassified) 사용")
    public void testNullRoadTypeDefaultsToUnclassified() throws InterruptedException {
        Coordinate[] coords = {
            new Coordinate(127.0, 37.0),
            new Coordinate(127.1, 37.1)
        };
        LineString geometry = geometryFactory.createLineString(coords);
        
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", geometry);
        featureBuilder.set("fclass", null);
        SimpleFeature feature = featureBuilder.buildFeature(null);
        
        ((DefaultFeatureCollection) collection).add(feature);
        
        edgeExtract = new EdgeExtract(idArrays, edgeQueue, dataStore, collection, taskContinue, taskError, null,1);
        
        edgeExtract.run();
        
        List<TaskItem> items = edgeQueue.take();
        
        assertThat(items).hasSize(2);
        assertThat(items).allMatch(edgeItem -> ((EdgeItem)edgeItem).getEdge().getRoadLevel() == RoadLevel.L2);
    }
}
