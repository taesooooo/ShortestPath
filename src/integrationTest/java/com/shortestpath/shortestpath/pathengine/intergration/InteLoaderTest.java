package com.shortestpath.shortestpath.pathengine.intergration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.shortestpath.TestApplication;
import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.EdgeHeader;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.NodeHeader;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndexEntry;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.LevelEdgeIndex;

@ActiveProfiles("inte")
@SpringBootTest(classes=TestApplication.class)
@Transactional
public class InteLoaderTest {
    private static final List<String> REQUIRED_OUTPUT_FILES = List.of(
            "node.bin",
            "edge.bin",
            "node_index.csv",
            "edge_index.bin",
            "reverse_edge.bin",
            "reverse_edge_index.bin");

    @Autowired
    private Engine engine;
    @Autowired
    private DataStore dataStore;
    @Autowired
    private Loader loader;

    @Value("${findpath.shp-path}")
    private String shpFilePath;
    
    @BeforeEach
    public void setup() {

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
