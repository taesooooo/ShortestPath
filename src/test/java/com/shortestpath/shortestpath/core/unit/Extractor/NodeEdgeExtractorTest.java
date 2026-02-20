package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.filter.text.cql2.CQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Extractor.ProgressStatus;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;

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
        }, "DataStore가 null이면 IllegalArgumentException이 발생해야 합니다.");
    }

    @Test
    @DisplayName("NodeExtractor 객체 생성 테스트 - 모두 정상")
    public void NodeExtractorConstructorNormalTest(@TempDir Path tempDir) throws IOException {
        Path tempFile = Files.createFile(tempDir.resolve("test.shp"));

        DataStore dataStore = mock(HybridDataStore.class);
        NodeEdgeExtractor extractor = new NodeEdgeExtractor(tempFile.toAbsolutePath().toString(), dataStore);

        assertThat(extractor).isNotNull();
    }

     @Test
     @DisplayName("NodeExtractor 객체의 getStore() 메서드 테스트")
     public void getStoreTest(@TempDir Path tempDir) throws IOException {
         Path tempFile = Files.createFile(tempDir.resolve("test.shp"));
         DataStore dataStore = mock(HybridDataStore.class);

         NodeEdgeExtractor extractor = new NodeEdgeExtractor(tempFile.toAbsolutePath().toString(), dataStore);
         DataStore returnedStore = extractor.getStore();

         assertThat(returnedStore).isEqualTo(dataStore);
     }

    @Test
    @DisplayName("extract() 메서드 - FileDataStoreFinder.getDataStore() 호출 확인")
    public void extractCallsFileDataStoreFinder(@TempDir Path tempDir) throws IOException, InterruptedException, CQLException {
        Path tempFile = Files.createFile(tempDir.resolve("test.shp"));
        FileDataStore fileDataStore = mock(FileDataStore.class);
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = mock(SimpleFeatureSource.class);
        DefaultFeatureCollection collection = new DefaultFeatureCollection();
        DataStore dataStore = mock(HybridDataStore.class);

        try (MockedStatic<FileDataStoreFinder> mockedStatic = mockStatic(FileDataStoreFinder.class)) {
            mockedStatic.when(() -> FileDataStoreFinder.getDataStore(any(File.class))).thenReturn(fileDataStore);
            when(fileDataStore.getTypeNames()).thenReturn(new String[] { "TestFeatureType" });
            when(fileDataStore.getFeatureSource(anyString())).thenReturn((SimpleFeatureSource) featureSource);
            when(featureSource.getFeatures()).thenReturn(collection);
            when(featureSource.getCount(any())).thenReturn(0);

            NodeEdgeExtractor extractor = new NodeEdgeExtractor(tempFile.toString(), dataStore);
            extractor.extract();

            Thread.sleep(1000);

            // FileDataStoreFinder.getDataStore() 호출 확인
            mockedStatic.verify(() -> FileDataStoreFinder.getDataStore(any(File.class)), times(1));
        }
    }

    @Test
    @DisplayName("saveToDb=true일 때 saveNodeIndex() 호출 확인")
    public void extractWithSaveToDbTrue(@TempDir Path tempDir) throws IOException, InterruptedException, CQLException {
        Path tempFile = Files.createFile(tempDir.resolve("test.shp"));
        FileDataStore fileDataStore = mock(FileDataStore.class);
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = mock(SimpleFeatureSource.class);
        DefaultFeatureCollection collection = new DefaultFeatureCollection();

        DataStore dataStore = mock(HybridDataStore.class);
        ExecutorService mockExecutorService = mock(ExecutorService.class);

        try (MockedStatic<FileDataStoreFinder> mockedStatic = mockStatic(FileDataStoreFinder.class);
             MockedStatic<Executors> executorsMockedStatic = mockStatic(Executors.class)) {
            
            mockedStatic.when(() -> FileDataStoreFinder.getDataStore(any(File.class))).thenReturn(fileDataStore);
            when(fileDataStore.getTypeNames()).thenReturn(new String[] { "test" });
            when(fileDataStore.getFeatureSource(anyString())).thenReturn((SimpleFeatureSource) featureSource);
            when(featureSource.getFeatures()).thenReturn(collection);
            when(featureSource.getCount(any())).thenReturn(1);
            
            executorsMockedStatic.when(() -> Executors.newFixedThreadPool(any(int.class))).thenReturn(mockExecutorService);
            when(mockExecutorService.submit(any(Runnable.class))).thenReturn(null);
            when(mockExecutorService.awaitTermination(any(long.class), any())).thenReturn(true);

            NodeEdgeExtractor extractor = new NodeEdgeExtractor(tempFile.toString(), dataStore, true);
            extractor.extract();

            // saveNodeIndex() 호출 확인
            verify(dataStore, times(1)).saveNodeIndex(any());
        }
    }

    @Test
    @DisplayName("saveToDb=false일 때 CSV 파일 생성 확인")
    public void extractWithSaveToDbFalse(@TempDir Path tempDir) throws IOException, InterruptedException, CQLException {
        Path tempFile = Files.createFile(tempDir.resolve("test.shp"));
        FileDataStore fileDataStore = mock(FileDataStore.class);
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = mock(SimpleFeatureSource.class);
        DefaultFeatureCollection collection = new DefaultFeatureCollection();
        DataStore dataStore = mock(HybridDataStore.class);
        ExecutorService mockExecutorService = mock(ExecutorService.class);

        try (MockedStatic<FileDataStoreFinder> mockedStatic = mockStatic(FileDataStoreFinder.class);
                MockedStatic<Executors> executorsMockedStatic = mockStatic(Executors.class)) {
            mockedStatic.when(() -> FileDataStoreFinder.getDataStore(any(File.class))).thenReturn(fileDataStore);
            when(fileDataStore.getTypeNames()).thenReturn(new String[] { "test" });
            when(fileDataStore.getFeatureSource(anyString())).thenReturn((SimpleFeatureSource) featureSource);
            when(featureSource.getFeatures()).thenReturn(collection);
            when(featureSource.getCount(any())).thenReturn(1);

            executorsMockedStatic.when(() -> Executors.newFixedThreadPool(any(int.class))).thenReturn(mockExecutorService);
            when(mockExecutorService.submit(any(Runnable.class))).thenReturn(null);
            when(mockExecutorService.awaitTermination(any(long.class), any())).thenReturn(true);

            NodeEdgeExtractor extractor = new NodeEdgeExtractor(tempFile.toString(), dataStore, false);
            extractor.extract();

            Thread.sleep(1500);

            // CSV 파일 생성 확인 (saveNodeIndex 호출 안됨)
            verify(dataStore, times(0)).saveNodeIndex(any());

            // CSV 파일이 생성되었는지 확인
            Path csvFilePath = tempDir.resolve("node_index.csv");
            assertThat(csvFilePath).exists();
        }
    }

    @Test
    @DisplayName("extract() 메서드 - ProgressStatus 파라미터 전달 확인")
    public void extractWithProgressStatusParameter(@TempDir Path tempDir) throws IOException, InterruptedException, CQLException {
        Path tempFile = Files.createFile(tempDir.resolve("test.shp"));
        FileDataStore fileDataStore = mock(FileDataStore.class);
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = mock(SimpleFeatureSource.class);
        DefaultFeatureCollection collection = new DefaultFeatureCollection();
        DataStore dataStore = mock(HybridDataStore.class);
        ProgressStatus progressStatus = mock(ProgressStatus.class);

        try (MockedStatic<FileDataStoreFinder> mockedStatic = mockStatic(FileDataStoreFinder.class)) {
            mockedStatic.when(() -> FileDataStoreFinder.getDataStore(any(File.class))).thenReturn(fileDataStore);
            when(fileDataStore.getTypeNames()).thenReturn(new String[] { "test" });
            when(fileDataStore.getFeatureSource(anyString())).thenReturn((SimpleFeatureSource) featureSource);
            when(featureSource.getFeatures()).thenReturn(collection);
            when(featureSource.getCount(any())).thenReturn(0);

            NodeEdgeExtractor extractor = new NodeEdgeExtractor(tempFile.toString(), dataStore);
            extractor.extract(progressStatus);

            Thread.sleep(1000);

            // extract(ProgressStatus) 메서드가 정상 호출되었는지 확인
            verify(featureSource, times(1)).getFeatures();
        }
    }
}

