package com.shortestpath.shortestpath.pathengine.intergration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeIndexProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.FileDataStore;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("inte")
@Transactional
public class InteLoaderTest {
    @Autowired
    private NodeIndexProvider nodeIndexProvider;

    @Test
    @DisplayName("Loader 데이터 추출 통합 테스트 - 특정 노드에 이웃 노드를 제대로 연결이 되어있는지 확인")
    public void LoaderLoadTest() throws Exception {
        String filePath = getClass().getClassLoader().getResource("sample/sample_jeju.shp").getPath();
        FileDataStore dataStore = new FileDataStore(new File(filePath).getParent(), nodeIndexProvider);
        Extractor extractor = new NodeEdgeExtractor(filePath, dataStore, nodeIndexProvider);
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
        while(readEdge != null) {
            readEdge = dataStore.readEdge(readEdge.getNextEdgeOffset());
            edgeList.add(readEdge);

            if(readEdge.getNextEdgeOffset() == -1) {
                break;
            }
        }
        
        for(Edge edge : edgeList) {
            Node readNode = dataStore.readNode(edge.getTo());
            list.add(readNode);
        }

        return list;
    }
}
