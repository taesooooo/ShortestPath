package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Extractor.HilbertIndex;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtract;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EdgeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

@DisplayName("NodeEdgeExtract 통합 추출 단위 테스트")
class NodeEdgeExtractTest {
    private GeometryFactory geometryFactory;
    private SimpleFeatureType lineFeatureType;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory();
        lineFeatureType = createFeatureType(LineString.class);
    }

    @Test
    @DisplayName("한 번의 피처 순회에서 중복 없는 노드와 연속 좌표 엣지를 함께 생성한다")
    void extractsNodesAndEdgesTogether() {
        Coordinate a = new Coordinate(127.0, 37.0);
        Coordinate b = new Coordinate(127.1, 37.1);
        Coordinate c = new Coordinate(127.2, 37.2);

        DefaultFeatureCollection collection = new DefaultFeatureCollection();
        collection.add(createFeature(lineFeatureType, geometryFactory.createLineString(new Coordinate[] {a, b}), "primary", 60, "B"));
        collection.add(createFeature(lineFeatureType, geometryFactory.createLineString(new Coordinate[] {b, c}), "primary", 60, "B"));

        long[] idArray = sortedIds(a, b, c);
        ExtractionResult result = extract(collection, idArray);

        assertThat(result.nodeItems).hasSize(3);
        assertThat(result.nodeItems)
                .extracting(item -> ((NodeItem) item).getNode().getId())
                .containsExactlyInAnyOrder(0, 1, 2);
        assertThat(result.edgeItems).hasSize(4);
        assertThat(result.edgeCount).isEqualTo(4);
        assertThat(result.nodeEndCount).isEqualTo(1);
        assertThat(result.edgeEndCount).isEqualTo(1);

        int bId = Arrays.binarySearch(idArray, GeometryUtil.coordinateToLong(b));
        assertThat(result.edgeItems)
                .extracting(item -> ((EdgeItem) item).getEdge())
                .noneMatch(edge -> edge.getFrom() == bId && edge.getTo() == bId);
    }

    @Test
    @DisplayName("통합 추출에서도 Hilbert 순서의 새 노드 ID를 사용한다")
    void usesHilbertNodeIds() {
        Coordinate bottomLeft = new Coordinate(0, 0);
        Coordinate topLeft = new Coordinate(0, 1);
        Coordinate topRight = new Coordinate(1, 1);
        Coordinate bottomRight = new Coordinate(1, 0);

        DefaultFeatureCollection collection = new DefaultFeatureCollection();
        collection.add(createFeature(lineFeatureType,
                geometryFactory.createLineString(new Coordinate[] {
                        bottomLeft, topLeft, topRight, bottomRight
                }), "primary", 60, "F"));

        long[] coordinateKeys = sortedIds(bottomLeft, topLeft, topRight, bottomRight);
        HilbertIndex hilbertIndex = HilbertIndex.build(coordinateKeys, 1);
        ExtractionResult result = extract(collection, hilbertIndex);

        int topRightId = hilbertIndex.findNodeId(GeometryUtil.coordinateToLong(topRight));
        int bottomRightId = hilbertIndex.findNodeId(GeometryUtil.coordinateToLong(bottomRight));

        assertThat(topRightId).isEqualTo(2);
        assertThat(bottomRightId).isEqualTo(3);
        assertThat(result.edgeItems)
                .extracting(item -> ((EdgeItem) item).getEdge())
                .anyMatch(edge -> edge.getFrom() == topRightId && edge.getTo() == bottomRightId);
    }

    @Test
    @DisplayName("MultiLineString의 서로 다른 LineString 경계를 엣지로 연결하지 않는다")
    void doesNotConnectSeparateLineStrings() {
        Coordinate a = new Coordinate(127.0, 37.0);
        Coordinate b = new Coordinate(127.1, 37.1);
        Coordinate c = new Coordinate(127.2, 37.2);
        Coordinate d = new Coordinate(127.3, 37.3);

        LineString first = geometryFactory.createLineString(new Coordinate[] {a, b});
        LineString second = geometryFactory.createLineString(new Coordinate[] {c, d});
        MultiLineString multiLineString = geometryFactory.createMultiLineString(new LineString[] {first, second});
        SimpleFeatureType multiLineFeatureType = createFeatureType(MultiLineString.class);

        DefaultFeatureCollection collection = new DefaultFeatureCollection();
        collection.add(createFeature(multiLineFeatureType, multiLineString, "residential", 30, "B"));

        long[] idArray = sortedIds(a, b, c, d);
        ExtractionResult result = extract(collection, idArray);
        int bId = Arrays.binarySearch(idArray, GeometryUtil.coordinateToLong(b));
        int cId = Arrays.binarySearch(idArray, GeometryUtil.coordinateToLong(c));

        assertThat(result.edgeItems).hasSize(4);
        assertThat(result.edgeItems)
                .extracting(item -> ((EdgeItem) item).getEdge())
                .noneMatch(edge -> connects(edge, bId, cId));
    }

    private ExtractionResult extract(DefaultFeatureCollection collection, long[] idArray) {
        return extract(collection, HilbertIndex.naturalOrder(idArray));
    }

    private ExtractionResult extract(DefaultFeatureCollection collection, HilbertIndex nodeIndex) {
        BlockingQueue<List<TaskItem>> nodeQueue = new LinkedBlockingQueue<>();
        BlockingQueue<List<TaskItem>> edgeQueue = new LinkedBlockingQueue<>();
        AtomicInteger edgeCount = new AtomicInteger();

        NodeEdgeExtract extractor = new NodeEdgeExtract(
                nodeIndex,
                nodeQueue,
                edgeQueue,
                collection,
                new AtomicBoolean(true),
                new AtomicBoolean(false),
                null,
                true,
                true,
                1,
                1,
                edgeCount);
        extractor.run();

        return new ExtractionResult(
                itemsOfType(nodeQueue, NodeItem.class),
                itemsOfType(edgeQueue, EdgeItem.class),
                countItems(nodeQueue, EndItem.class),
                countItems(edgeQueue, EndItem.class),
                edgeCount.get());
    }

    private List<TaskItem> itemsOfType(BlockingQueue<List<TaskItem>> queue, Class<?> type) {
        List<TaskItem> result = new ArrayList<>();
        for (List<TaskItem> batch : queue) {
            for (TaskItem item : batch) {
                if (type.isInstance(item)) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    private int countItems(BlockingQueue<List<TaskItem>> queue, Class<?> type) {
        return itemsOfType(queue, type).size();
    }

    private SimpleFeatureType createFeatureType(Class<?> geometryType) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Road");
        builder.add("geometry", geometryType);
        builder.add("fclass", String.class);
        builder.add("maxspeed", Integer.class);
        builder.add("oneway", String.class);
        return builder.buildFeatureType();
    }

    private SimpleFeature createFeature(
            SimpleFeatureType featureType,
            Object geometry,
            String roadType,
            int speed,
            String oneWay) {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        builder.set("geometry", geometry);
        builder.set("fclass", roadType);
        builder.set("maxspeed", speed);
        builder.set("oneway", oneWay);
        return builder.buildFeature(null);
    }

    private long[] sortedIds(Coordinate... coordinates) {
        long[] ids = Arrays.stream(coordinates)
                .mapToLong(GeometryUtil::coordinateToLong)
                .toArray();
        Arrays.sort(ids);
        return ids;
    }

    private boolean connects(Edge edge, int firstNodeId, int secondNodeId) {
        return (edge.getFrom() == firstNodeId && edge.getTo() == secondNodeId)
                || (edge.getFrom() == secondNodeId && edge.getTo() == firstNodeId);
    }

    private static class ExtractionResult {
        private final List<TaskItem> nodeItems;
        private final List<TaskItem> edgeItems;
        private final int nodeEndCount;
        private final int edgeEndCount;
        private final int edgeCount;

        private ExtractionResult(
                List<TaskItem> nodeItems,
                List<TaskItem> edgeItems,
                int nodeEndCount,
                int edgeEndCount,
                int edgeCount) {
            this.nodeItems = nodeItems;
            this.edgeItems = edgeItems;
            this.nodeEndCount = nodeEndCount;
            this.edgeEndCount = edgeEndCount;
            this.edgeCount = edgeCount;
        }
    }
}
