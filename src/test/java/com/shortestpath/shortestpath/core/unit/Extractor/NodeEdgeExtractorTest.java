package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.GeometryBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.LineString;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;

public class NodeEdgeExtractorTest  {

    @Test
    @DisplayName("NodeExtractor 객체 생성 테스트 - 파일 없음")
    public void NodeExtractorConstructorFileExistenceTest(@TempDir Path tempDir) {
        File tempFile = tempDir.resolve("test.shp").toFile();

        assertThrows(IOException.class, () -> {
            NodeEdgeExtractor extractor = new NodeEdgeExtractor(tempFile.getAbsolutePath(), null);
        }, "파일이 존재하지 않으면 IOException이 발생해야 합니다.");
    }

    @Test
    @DisplayName("NodeExtractor 객체 생성 테스트 - DataStore null")
    public void NodeExtractorConstructorDataStoreNullTest(@TempDir Path tempDir) throws IOException {
        Path tempFile = Files.createFile(tempDir.resolve("test.shp"));

        assertThrows(IllegalArgumentException.class, () -> {
            NodeEdgeExtractor extractor = new NodeEdgeExtractor(tempFile.toAbsolutePath().toString(), null);
        }, "DataStroe가 null이면 IllegalArgumentException이 발생해야 합니다.");
    }

    @Test
    @DisplayName("NodeExtractor 객체 생성 테스트 - 모두 정상")
    public void NodeExtractorConstructorNormalTest(@TempDir Path tempDir) throws IOException {
        Path tempFile = Files.createFile(tempDir.resolve("test.shp"));

        NodeEdgeExtractor extractor = new NodeEdgeExtractor(tempFile.toAbsolutePath().toString(), mock(TestFileDataStore.class));

        assertThat(extractor).isNotNull();
    }

     @Test
     @DisplayName("노드 추출 테스트 - 정상")
     public void nodeExtractionTest() throws IOException {
         File tmp = File.createTempFile("test", ".shp");
         tmp.deleteOnExit();

         SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
         typeBuilder.setName("TestFeatureType");
         typeBuilder.add("geometry", LineString.class);
         SimpleFeatureType featureType = typeBuilder.buildFeatureType();

         GeometryBuilder geometryBuilder = new GeometryBuilder();
         LineString lineString = geometryBuilder.lineString(0.0, 0.0, 1.0, 1.0, 2.0, 2.0, 3.0, 3.0, 4.0, 4.0, 5.0, 5.0);

         SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
         featureBuilder.add(lineString);
         SimpleFeature feature = featureBuilder.buildFeature(null);

         DefaultFeatureCollection collection = new DefaultFeatureCollection();
         collection.add(feature);

         FileDataStore store = mock(FileDataStore.class);
         FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = mock(SimpleFeatureSource.class);

         MockedStatic<FileDataStoreFinder> mockedStatic = mockStatic(FileDataStoreFinder.class);
         mockedStatic.when(() -> FileDataStoreFinder.getDataStore(any(File.class))).thenReturn(store);
         // when(FileDataStoreFinder.getDataStore(any(File.class))).thenReturn(store);
         when(store.getTypeNames()).thenReturn(new String[] { "TestFeatureType" });
         when(store.getFeatureSource(anyString())).thenReturn((SimpleFeatureSource) featureSource);
         when(featureSource.getFeatures()).thenReturn(collection);

         DataStore testStore = spy(new TestFileDataStore(tmp.getParent()));

         NodeEdgeExtractor extractor = new NodeEdgeExtractor(tmp.toString(), testStore);
         extractor.extract();

         ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
         ArgumentCaptor<Node> nodeCaptor2 = ArgumentCaptor.forClass(Node.class);
         verify(testStore, times(6)).saveNode(nodeCaptor.capture());
         verify(testStore, times(12)).saveNode(nodeCaptor2.capture(), anyLong());

         assertThat(nodeCaptor.getAllValues()).isNotEmpty()
                 .extracting(Node::getId, node -> node.getCoordinate().getLatitude(),
                         node -> node.getCoordinate().getLatitude())
                 .containsExactly(
                         tuple(0, 0.0, 0.0),
                         tuple(1, 1.0, 1.0),
                         tuple(2, 2.0, 2.0),
                         tuple(3, 3.0, 3.0),
                         tuple(4, 4.0, 4.0),
                         tuple(5, 5.0, 5.0));

        mockedStatic.close();
     }

     @Test
     @DisplayName("노드에 연결된 엣지가 정상적으로 연결되어있는지 확인")
     public void edgeConnectionTest() throws IOException {
         File tmp = File.createTempFile("test", ".shp");
         tmp.deleteOnExit();

         SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
         typeBuilder.setName("TestFeatureType");
         typeBuilder.add("geometry", LineString.class);
         SimpleFeatureType featureType = typeBuilder.buildFeatureType();

         GeometryBuilder geometryBuilder = new GeometryBuilder();
         LineString lineString = geometryBuilder.lineString(0.0, 0.0, 1.0, 1.0, 2.0, 2.0, 3.0, 3.0, 4.0, 4.0, 5.0, 5.0);

         SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
         featureBuilder.add(lineString);
         SimpleFeature feature = featureBuilder.buildFeature(null);

         DefaultFeatureCollection collection = new DefaultFeatureCollection();
         collection.add(feature);

         FileDataStore store = mock(FileDataStore.class);
         FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = mock(SimpleFeatureSource.class);

         MockedStatic<FileDataStoreFinder> mockedStatic = mockStatic(FileDataStoreFinder.class);
         mockedStatic.when(() -> FileDataStoreFinder.getDataStore(any(File.class))).thenReturn(store);
         // when(FileDataStoreFinder.getDataStore(any(File.class))).thenReturn(store);
         when(store.getTypeNames()).thenReturn(new String[] { "TestFeatureType" });
         when(store.getFeatureSource(anyString())).thenReturn((SimpleFeatureSource) featureSource);
         when(featureSource.getFeatures()).thenReturn(collection);

         DataStore testStore = spy(new TestFileDataStore(tmp.getParent()));
         NodeEdgeExtractor extractor = new NodeEdgeExtractor(tmp.toString(), testStore);
         extractor.extract();

         ArgumentCaptor<Edge> edgeCaptor = ArgumentCaptor.forClass(Edge.class);

         verify(testStore, times(14)).saveEdge(edgeCaptor.capture(), anyLong());

         assertThat(edgeCaptor.getAllValues()).isNotEmpty()
                 .extracting(Edge::getId, Edge::getFrom, Edge::getTo, Edge::getNextEdgeOffset)
                 .contains(
                         tuple(0, 0, 1, -1),
                         tuple(1, 1, 0, 2),
                         tuple(2, 1, 2, -1),
                         tuple(3, 2, 1, 4),
                         tuple(4, 2, 3, -1),
                         tuple(5, 3, 2, 6),
                         tuple(6, 3, 4, -1),
                         tuple(7, 4, 3, 8),
                         tuple(8, 4, 5, -1),
                         tuple(9, 5, 4, -1));

        mockedStatic.close();

     }

    //  private TestFileDataStore createFileDataStore() {
    //     TestFileDataStore store = new TestFileDataStore();

    //     return null;
    //  }

     public static class TestFileDataStore extends com.shortestpath.shortestpath.core.pathengine.Store.FileDataStore {
         private HashMap<Integer, Node> nodeMap = new HashMap<Integer, Node>();
         private HashMap<Integer, Edge> edgeMap = new HashMap<Integer, Edge>();

        public TestFileDataStore(String filePath) throws IOException {
            super(filePath);
        }

        public int getEdgeByteSize() {
            return 1;
        }
        public int getNodeByteSize() {
            return 1;
        }

        @Override
		public Edge readEdge(long offset) throws IOException {
			Edge edge = edgeMap.get((int)offset);
			return new Edge(edge.getId(), edge.getFrom(), edge.getTo(), edge.getDistance(), edge.getNextEdgeOffset());
		}

		@Override
		public Node readNode(long offset) throws IOException {
			Node node = nodeMap.get((int)offset);
			return new Node(node.getId(), node.getCoordinate(), node.getStartEdgeOffset(), node.getGCost(), node.getHCost(), node.getFCost());
		}

		@Override
		public int saveEdge(Edge edge) throws IOException {
			return saveEdge(edge, edge.getId());
		}

		@Override
		public int saveEdge(Edge edge, long offset) throws IOException {
			edgeMap.put((int)offset, edge);

            return (int)offset;
		}

		@Override
		public int saveNode(Node node) throws IOException {
			return saveNode(node, node.getId());
		}

		@Override
		public int saveNode(Node node, long offset) throws IOException {
			nodeMap.put((int)offset, node);

            return (int)offset;
		}	
    
     }
}
