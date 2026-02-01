package com.shortestpath.shortestpath.pathengine.intergration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;

import com.shortestpath.shortestpath.DBHelper;
import com.shortestpath.shortestpath.IntegrationTestHelper;
import com.shortestpath.shortestpath.TestConfig;
import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndexEntry;
import com.shortestpath.shortestpath.provider.JpaDataPersistence;
import com.shortestpath.shortestpath.provider.JpaNodeProvider;
import com.shortestpath.shortestpath.repository.NodeIndexInsertRepository;

import jakarta.transaction.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("inte")
@Import({
        JpaDataPersistence.class,
        JpaNodeProvider.class,
        NodeIndexInsertRepository.class,
        TestConfig.class,
        DBHelper.class
})
@TestInstance(Lifecycle.PER_CLASS)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Transactional
public class InteLoaderTest {
    @Autowired
    DataStore dataStore;
    @Autowired
    Extractor extractor;
    @Autowired
    Loader loader;

    @Autowired
    DBHelper dbHelper;

    @BeforeAll
    public void init() throws IOException {
        loader.extractData();
        ((HybridDataStore) dataStore).switchToMappingMode();
    }

    @AfterAll
    public void destroy() throws IOException {
        dataStore.close();
        IntegrationTestHelper.deleteBinaryFiles((HybridDataStore) dataStore);
        dbHelper.turncate();
    }

    @Test
    @DisplayName("Loader 데이터 추출 통합 테스트 - 특정 노드에 이웃 노드를 제대로 연결이 되어있는지 확인")
    public void LoaderLoadTest() throws Exception {
        int testNodeOffset = dataStore.getNodeOffset(new Coordinate(33.2408904, 126.5637502));
        Node readNode = dataStore.readNode(testNodeOffset);

        assertThat(readNode).isNotNull();

        ArrayList<Node> neighborNodeList = getNeighborNode(dataStore, readNode);

        assertThat(neighborNodeList.size()).isEqualTo(4);
        for (int i = 0; i < neighborNodeList.size() - 1; i++) {
            Node testNode = neighborNodeList.get(i);
            Coordinate coordinate = testNode.getCoordinate();
            Node actualNode = dataStore.readNode(dataStore.getNodeOffset(coordinate));

            assertThat(actualNode).usingRecursiveComparison().isEqualTo(testNode);
        }
    }

    private ArrayList<Node> getNeighborNode(DataStore dataStore, Node node) throws IOException {
        ArrayList<Node> list = new ArrayList<Node>();
        ArrayList<Edge> edgeList = new ArrayList<Edge>();
        EdgeIndex index = dataStore.getEdgeIndex();

        EdgeIndexEntry entry = index.get(node.getId());
        int edgeCount = entry.getLevel0EdgeIndex().getEdgeCount() + entry.getLevel1EdgeIndex().getEdgeCount()
                + entry.getLevel2EdgeIndex().getEdgeCount();

        long startOffset = getStartOffset(entry);
        for(int i = 0; i<edgeCount; i++) {
            Edge edge = dataStore.readEdge(startOffset + i * DataStructureSizes.EDGE_SIZE);
            edgeList.add(edge);
        }
        Node preivouseNode = null;
        for (Edge edge : edgeList) {
            Node neighborNode = dataStore.readNode(DataStructureSizes.calculateNodeOffset(edge.getTo()));
            if(preivouseNode == null || neighborNode.getId() != preivouseNode.getId()) {
                preivouseNode = neighborNode;
                list.add(neighborNode);
            }
        }

        return list;
    }

    private long getStartOffset(EdgeIndexEntry entry) {
        if (entry.getLevel0EdgeIndex().getEdgeCount() > 0) {
            return entry.getLevel0EdgeIndex().getStartOffset();
        } 
        else if (entry.getLevel1EdgeIndex().getEdgeCount() > 0) {
            return entry.getLevel1EdgeIndex().getStartOffset();
        } 
        else {
            return entry.getLevel2EdgeIndex().getStartOffset();
        }
    }
}
