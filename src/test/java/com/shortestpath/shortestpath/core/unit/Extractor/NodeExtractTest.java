package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
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

import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeExtract;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

@DisplayName("NodeExtract 단위 테스트")
public class NodeExtractTest {
    
    private NodeExtract nodeExtract;
    private long[] idArrays;
    private BlockingQueue<TaskItem> nodeQueue;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
    private SimpleFeatureType featureType;
    private GeometryFactory geometryFactory;
    private AtomicBoolean taskContinue = new AtomicBoolean(true);
    
    @BeforeEach
    public void setUp() {
        idArrays = new long[]{
            GeometryUtil.coordinateToLong(new Coordinate(127.0, 37.0)),
            GeometryUtil.coordinateToLong(new Coordinate(127.1, 37.1)),
            GeometryUtil.coordinateToLong(new Coordinate(127.2, 37.2)),
            GeometryUtil.coordinateToLong(new Coordinate(127.3, 37.3))
        };
        Arrays.sort(idArrays);
        
        nodeQueue = new LinkedBlockingQueue<>();
        geometryFactory = new GeometryFactory();
        
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Road");
        builder.add("geometry", LineString.class);
        builder.add("fclass", String.class);
        featureType = builder.buildFeatureType();
        
        collection = new DefaultFeatureCollection();
    }
    
    @Test
    @DisplayName("라인스트링 노드 추출 테스트")
    public void testMultipleSegmentNodeExtraction() throws InterruptedException {
        Coordinate[] coords = {
            new Coordinate(127.0, 37.0),
            new Coordinate(127.1, 37.1),
            new Coordinate(127.2, 37.2)
        };
        LineString geometry = geometryFactory.createLineString(coords);
        
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("geometry", geometry);
        featureBuilder.set("fclass", "primary");
        SimpleFeature feature = featureBuilder.buildFeature(null);
        
        ((DefaultFeatureCollection) collection).add(feature);
        
        nodeExtract = new NodeExtract(0, idArrays, nodeQueue, collection, taskContinue);
        
        nodeExtract.run();
        
        List<TaskItem> items = new ArrayList<>();
        nodeQueue.drainTo(items);
        
        List<NodeItem> nodeItems = new ArrayList<>();
        for (TaskItem item : items) {
            if (item instanceof NodeItem) {
                nodeItems.add((NodeItem) item);
            }
        }
        
        assertThat(nodeItems).hasSize(2);
    }
    
    @Test
    @DisplayName("노드 ID가 올바르게 설정되는지 확인")
    public void testNodeIdAssignment() throws InterruptedException {
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
        
        nodeExtract = new NodeExtract(0, idArrays, nodeQueue, collection, taskContinue);
        
        nodeExtract.run();
        
        List<TaskItem> items = new ArrayList<>();
        nodeQueue.drainTo(items);
        
        List<NodeItem> nodeItems = new ArrayList<>();
        for (TaskItem item : items) {
            if (item instanceof NodeItem) {
                nodeItems.add((NodeItem) item);
            }
        }
        
        assertThat(nodeItems).hasSize(1);
        NodeItem nodeItem = nodeItems.get(0);
        
        Node nodeA = nodeItem.getNodeA();
        Node nodeB = nodeItem.getNodeB();
        
        assertThat(nodeA.getId()).isEqualTo(0);
        assertThat(nodeB.getId()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("노드 좌표가 올바르게 설정되는지 확인")
    public void testNodeCoordinates() throws InterruptedException {
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
        
        nodeExtract = new NodeExtract(0, idArrays, nodeQueue, collection, taskContinue);
        
        nodeExtract.run();
        
        List<TaskItem> items = new ArrayList<>();
        nodeQueue.drainTo(items);
        
        List<NodeItem> nodeItems = new ArrayList<>();
        for (TaskItem item : items) {
            if (item instanceof NodeItem) {
                nodeItems.add((NodeItem) item);
            }
        }

        NodeItem nodeItem = nodeItems.get(0);
        
        Node nodeA = nodeItem.getNodeA();
        Node nodeB = nodeItem.getNodeB();
        
        assertThat(nodeA.getCoordinate()).extracting(com.shortestpath.shortestpath.core.pathengine.Coordinate::getLatitude, com.shortestpath.shortestpath.core.pathengine.Coordinate::getLongitude)
                 .containsExactly(coords[0].getY(), coords[0].getX());
        assertThat(nodeB.getCoordinate()).extracting(com.shortestpath.shortestpath.core.pathengine.Coordinate::getLatitude, com.shortestpath.shortestpath.core.pathengine.Coordinate::getLongitude)
                 .containsExactly(coords[1].getY(), coords[1].getX());
    }
    
    @Test
    @DisplayName("다중 피처에서 노드 추출 테스트")
    public void testMultipleFeaturesNodeExtraction() throws InterruptedException {
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
        
        nodeExtract = new NodeExtract(0, idArrays, nodeQueue, collection, taskContinue);
        
        nodeExtract.run();
        
        List<TaskItem> items = new ArrayList<>();
        nodeQueue.drainTo(items);
        
        List<NodeItem> nodeItems = new ArrayList<>();
        for (TaskItem item : items) {
            if (item instanceof NodeItem) {
                nodeItems.add((NodeItem) item);
            }
        }
        
        assertThat(nodeItems).hasSize(2);
    }
}
