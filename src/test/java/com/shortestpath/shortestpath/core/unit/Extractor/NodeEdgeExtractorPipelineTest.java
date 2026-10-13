package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Store.EdgeHeader;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.NodeHeader;

@DisplayName("NodeEdgeExtractor 통합 파이프라인 테스트")
class NodeEdgeExtractorPipelineTest {

    @Test
    @DisplayName("실제 SHP에서 노드와 엣지를 함께 추출하고 정렬·인덱스 생성을 완료한다")
    void extractsAndStoresNodesAndEdgesFromSingleProducer(@TempDir Path tempDir) throws Exception {
        Path shapeFile = createShapeFile(tempDir.resolve("roads.shp"));
        Path outputDirectory = Files.createDirectory(tempDir.resolve("output"));

        HybridDataStore dataStore = new HybridDataStore(outputDirectory.toString());
        try {
            NodeEdgeExtractor extractor = new NodeEdgeExtractor(shapeFile.toString(), dataStore, false);
            extractor.extract();

            NodeHeader nodeHeader = dataStore.readNodeHeader();
            EdgeHeader edgeHeader = dataStore.readEdgeHeader();
            EdgeHeader reverseEdgeHeader = dataStore.readReverseEdgeHeader();

            assertThat(nodeHeader.getNodeCount()).isEqualTo(3);
            assertThat(nodeHeader.isTaskCompleted()).isTrue();
            assertThat(edgeHeader.getEdgeCount()).isEqualTo(4);
            assertThat(edgeHeader.isSorted()).isTrue();
            assertThat(edgeHeader.isTaskCompleted()).isTrue();
            assertThat(reverseEdgeHeader.getEdgeCount()).isEqualTo(4);
            assertThat(reverseEdgeHeader.isSorted()).isTrue();
            assertThat(reverseEdgeHeader.isTaskCompleted()).isTrue();

            for (int edgeIndex = 0; edgeIndex < edgeHeader.getEdgeCount(); edgeIndex++) {
                Edge edge = dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(edgeIndex));
                assertThat(edge.getFrom()).isBetween(0, nodeHeader.getNodeCount() - 1);
                assertThat(edge.getTo()).isBetween(0, nodeHeader.getNodeCount() - 1);
            }
        } finally {
            dataStore.close();
        }
    }

    private Path createShapeFile(Path shapeFilePath) throws Exception {
        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        Map<String, Object> parameters = new HashMap<>();
        URL shapeFileUrl = shapeFilePath.toUri().toURL();
        parameters.put("url", shapeFileUrl);
        parameters.put("create spatial index", Boolean.FALSE);

        ShapefileDataStore shapeFileStore = (ShapefileDataStore) factory.createNewDataStore(parameters);
        try {
            SimpleFeatureType featureType = createFeatureType();
            shapeFileStore.createSchema(featureType);

            try (FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
                    shapeFileStore.getFeatureWriterAppend(Transaction.AUTO_COMMIT)) {
                SimpleFeature feature = writer.next();
                feature.setAttribute("the_geom", createRoadGeometry());
                feature.setAttribute("fclass", "primary");
                feature.setAttribute("maxspeed", 60);
                feature.setAttribute("oneway", "B");
                writer.write();
            }
        } finally {
            shapeFileStore.dispose();
        }

        return shapeFilePath;
    }

    private SimpleFeatureType createFeatureType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("roads");
        builder.add("the_geom", LineString.class);
        builder.add("fclass", String.class);
        builder.add("maxspeed", Integer.class);
        builder.add("oneway", String.class);
        return builder.buildFeatureType();
    }

    private LineString createRoadGeometry() {
        return new GeometryFactory().createLineString(new Coordinate[] {
                new Coordinate(127.0, 37.0),
                new Coordinate(127.1, 37.1),
                new Coordinate(127.2, 37.2)
        });
    }
}
