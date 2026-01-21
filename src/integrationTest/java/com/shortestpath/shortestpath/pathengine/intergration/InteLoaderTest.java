package com.shortestpath.shortestpath.pathengine.intergration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.test.context.ActiveProfiles;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;
import com.shortestpath.shortestpath.repository.NodeIndexRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("inte")
@Transactional
public class InteLoaderTest {
    @Autowired
    private NodeProvider nodeIndexProvider;

    @Autowired
    private NodeIndexRepository repository;

    @BeforeEach
    public void init() {
        // repository.deleteAll();
    }

    @AfterEach
    public void clear() {
        // repository.deleteAll();
    }

    @Test
    @DisplayName("Loader 데이터 추출 통합 테스트 - 특정 노드에 이웃 노드를 제대로 연결이 되어있는지 확인")
    public void LoaderLoadTest() throws Exception {
        String filePath = getClass().getClassLoader().getResource("sample/sample_jeju.shp").getPath();
        HybridDataStore dataStore = new HybridDataStore(new File(filePath).getParent(), nodeIndexProvider);
        Extractor extractor = new NodeEdgeExtractor(filePath, dataStore, true);
        Loader loader = new Loader(extractor);
        loader.extractData();
        
        assertThat(loader.isDataExtracted()).isTrue();
        
        int testNodeOffset = dataStore.getNodeOffset(new Coordinate(33.2408904, 126.5637502));
        Node readNode = dataStore.readNode(testNodeOffset);
        
        assertThat(readNode).isNotNull();

        ArrayList<Node> neighborNodeList = getNeighborNode(dataStore, readNode);
        
        assertThat(neighborNodeList.size()).isEqualTo(4);
        for(int i=0; i<neighborNodeList.size() - 1; i++ ) {
            Node testNode = neighborNodeList.get(i);
            Coordinate coordinate = testNode.getCoordinate();
            Node actualNode = dataStore.readNode(dataStore.getNodeOffset(coordinate));
            
            assertThat(actualNode).usingRecursiveComparison().isEqualTo(testNode);
        }
    }
    
    private ArrayList<Node> getNeighborNode(DataStore dataStore, Node node) throws IOException {
        ArrayList<Node> list = new ArrayList<Node>();
        ArrayList<Edge> edgeList = new ArrayList<Edge>();

        Edge readEdge = dataStore.readEdge(node.getStartEdgeOffset());
        edgeList.add(readEdge);
        while(readEdge != null && readEdge.getNextEdgeOffset() != -1) {
            readEdge = dataStore.readEdge(readEdge.getNextEdgeOffset());
            edgeList.add(readEdge);
        }
        
        for(Edge edge : edgeList) {
            Node readNode = dataStore.readNode(edge.getTo() * DataStructureSizes.NODE_SIZE);
            list.add(readNode);
        }

        return list;
    }
}
