package com.shortestpath.shortestpath.pathengine.intergration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.shortestpath.shortestpath.IntegrationTestHelper;
import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.EdgeHeader;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.NodeHeader;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndexEntry;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.FileBasedEdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.LevelEdgeIndex;

@SpringJUnitConfig(InteLoaderTest.LoaderIntegrationConfig.class)
@TestPropertySource("classpath:application-inte.properties")
@TestInstance(Lifecycle.PER_CLASS)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class InteLoaderTest {
    private static final List<String> REQUIRED_OUTPUT_FILES = List.of(
            "node.bin",
            "edge.bin",
            "node_index.csv",
            "edge_index.bin",
            "reverse_edge.bin",
            "reverse_edge_index.bin");

    @Autowired
    DataStore dataStore;

    @Autowired
    Loader loader;

    @Value("${findpath.shp-path}")
    String shpFilePath;

    @TestConfiguration
    static class LoaderIntegrationConfig {
        @Bean
        public DataStore dataStore(@Value("${findpath.shp-path}") String shpFilePath) throws Exception {
            String parentDir = new File(shpFilePath).getParent();

            HybridDataStore dataStore = new HybridDataStore(parentDir);
            dataStore.setEdgeIndex(new FileBasedEdgeIndex(parentDir));
            dataStore.setReverseEdgeIndex(new FileBasedEdgeIndex(new File(parentDir, "reverse_edge_index.bin").toPath()));

            return dataStore;
        }

        @Bean
        public Extractor extractor(@Value("${findpath.shp-path}") String shpFilePath, DataStore dataStore) throws IOException {
            return new NodeEdgeExtractor(shpFilePath, dataStore, false);
        }

        @Bean
        public Loader loader(Extractor extractor) throws IOException {
            return new Loader(extractor);
        }
    }

    @BeforeAll
    public void init() throws IOException {
        loader.extractData(false);
    }

    @AfterAll
    public void destroy() throws IOException {
        dataStore.close();
        IntegrationTestHelper.deleteBinaryFiles((HybridDataStore) dataStore);
    }

    @Test
    @DisplayName("Loader는 SHP 데이터를 추출해 경로탐색용 파일을 생성한다")
    public void extractDataCreatesRequiredFiles() {
        Path outputDirectory = outputDirectory();

        for (String fileName : REQUIRED_OUTPUT_FILES) {
            assertThat(outputDirectory.resolve(fileName))
                    .as("%s 파일이 생성되어야 합니다.", fileName)
                    .exists()
                    .isRegularFile();
        }
    }

    @Test
    @DisplayName("Loader가 생성한 파일은 비어 있지 않다")
    public void extractDataCreatesNonEmptyFiles() throws IOException {
        Path outputDirectory = outputDirectory();

        for (String fileName : REQUIRED_OUTPUT_FILES) {
            Path outputFile = outputDirectory.resolve(fileName);

            assertThat(Files.size(outputFile))
                    .as("%s 파일은 비어 있으면 안 됩니다.", fileName)
                    .isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("Loader가 생성한 헤더는 추출 완료 상태와 데이터 개수를 가진다")
    public void extractedHeadersAreCompleted() throws IOException {
        NodeHeader nodeHeader = dataStore.readNodeHeader();
        EdgeHeader edgeHeader = dataStore.readEdgeHeader();
        EdgeHeader reverseEdgeHeader = dataStore.readReverseEdgeHeader();

        assertThat(nodeHeader.isTaskCompleted()).isTrue();
        assertThat(nodeHeader.getNodeCount()).isGreaterThan(0);

        assertThat(edgeHeader.isTaskCompleted()).isTrue();
        assertThat(edgeHeader.isSorted()).isTrue();
        assertThat(edgeHeader.getEdgeCount()).isGreaterThan(0);

        assertThat(reverseEdgeHeader.isTaskCompleted()).isTrue();
        assertThat(reverseEdgeHeader.isSorted()).isTrue();
        assertThat(reverseEdgeHeader.getEdgeCount()).isEqualTo(edgeHeader.getEdgeCount());
    }

    @Test
    @DisplayName("Loader가 생성한 바이너리 데이터는 DataStore로 다시 읽을 수 있다")
    public void extractedDataCanBeReadByDataStore() throws IOException {
        assertThat(loader.isDataExtracted()).isTrue();
        assertThat(dataStore.getTotalNodes()).isGreaterThan(0);
        assertThat(dataStore.getTotalEdges()).isGreaterThan(0);
        assertThat(dataStore.getTotalReverseEdges()).isEqualTo(dataStore.getTotalEdges());

        Node firstNode = dataStore.readNode(DataStructureSizes.calculateNodeOffset(0));
        Edge firstEdge = dataStore.readEdge(DataStructureSizes.calculateEdgeOffset(0));
        Edge firstReverseEdge = dataStore.readReverseEdge(DataStructureSizes.calculateEdgeOffset(0));

        assertThat(firstNode).isNotNull();
        assertThat(firstEdge).isNotNull();
        assertThat(firstReverseEdge).isNotNull();
    }

    @Test
    @DisplayName("Loader가 생성한 정방향/역방향 인덱스는 실제 엣지 오프셋을 가리킨다")
    public void extractedIndexesPointToReadableEdges() throws IOException {
        EdgeIndexEntry forwardEntry = findEntryWithEdges(dataStore.getEdgeIndex());
        LevelEdgeIndex forwardLevel = firstLevelWithEdges(forwardEntry);
        Edge forwardEdge = dataStore.readEdge(forwardLevel.getStartOffset());

        assertThat(forwardEdge.getFrom()).isEqualTo(forwardEntry.getNodeId());
        assertThat(forwardEdge.getTo()).isBetween(0, dataStore.getTotalNodes() - 1);

        EdgeIndexEntry reverseEntry = findEntryWithEdges(dataStore.getReverseEdgeIndex());
        LevelEdgeIndex reverseLevel = firstLevelWithEdges(reverseEntry);
        Edge reverseEdge = dataStore.readReverseEdge(reverseLevel.getStartOffset());

        assertThat(reverseEdge.getTo()).isEqualTo(reverseEntry.getNodeId());
        assertThat(reverseEdge.getFrom()).isBetween(0, dataStore.getTotalNodes() - 1);
    }

    @Test
    @DisplayName("Loader는 이미 생성된 데이터가 있어도 다시 실행할 수 있다")
    public void extractDataCanBeRunTwiceSafely() throws IOException {
        Loader rerunLoader = new Loader(new NodeEdgeExtractor(shpFilePath, dataStore, false));

        rerunLoader.extractData(false);

        assertThat(rerunLoader.isDataExtracted()).isTrue();
        assertThat(dataStore.getTotalNodes()).isGreaterThan(0);
        assertThat(dataStore.getTotalEdges()).isGreaterThan(0);
    }

    private Path outputDirectory() {
        return Path.of(((HybridDataStore) dataStore).getFileDirectory());
    }

    private EdgeIndexEntry findEntryWithEdges(EdgeIndex edgeIndex) throws IOException {
        int totalNodes = dataStore.getTotalNodes();

        for (int nodeId = 0; nodeId < totalNodes; nodeId++) {
            EdgeIndexEntry entry = edgeIndex.get(nodeId);
            if (entry != null && totalEdgeCount(entry) > 0) {
                return entry;
            }
        }

        throw new AssertionError("엣지를 가진 인덱스 엔트리를 찾을 수 없습니다.");
    }

    private int totalEdgeCount(EdgeIndexEntry entry) {
        return entry.getLevel0EdgeIndex().getEdgeCount()
                + entry.getLevel1EdgeIndex().getEdgeCount()
                + entry.getLevel2EdgeIndex().getEdgeCount();
    }

    private LevelEdgeIndex firstLevelWithEdges(EdgeIndexEntry entry) {
        if (entry.getLevel0EdgeIndex().getEdgeCount() > 0) {
            return entry.getLevel0EdgeIndex();
        }
        if (entry.getLevel1EdgeIndex().getEdgeCount() > 0) {
            return entry.getLevel1EdgeIndex();
        }
        if (entry.getLevel2EdgeIndex().getEdgeCount() > 0) {
            return entry.getLevel2EdgeIndex();
        }

        throw new AssertionError("인덱스 엔트리에 엣지가 없습니다. nodeId=" + entry.getNodeId());
    }
}
