package com.shortestpath.shortestpath.pathengine.intergration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeEdgeExtractor;
import com.shortestpath.shortestpath.core.pathengine.Provider.DataProvider;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeIndexProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.FileDataStore;
import com.shortestpath.shortestpath.provider.JapMapDataProvider;
import com.shortestpath.shortestpath.provider.JpaNodeIndexProvider;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("inte")
@Transactional
public class InteEngineTest {
    private DataStore store;
    private Engine engine;

    @Autowired
	private DataProvider dataProvider;
    
    @Autowired
    private NodeIndexProvider nodeIndexProvider;
    
    @BeforeEach
    public void setUp() throws IOException {
        String path = getClass().getClassLoader().getResource("sample/sample_jeju.shp").getPath();
        this.store = new FileDataStore(new File(path).getParent(), nodeIndexProvider);
        this.engine = new Engine(store, dataProvider);
        Extractor extractor = new NodeEdgeExtractor(path, this.store, nodeIndexProvider);
        extractor.extract();
    }
    
    @Test
    @DisplayName("경로 탐색 - 정상 탐색")
    public void findPathTestByNode() throws IOException {
        ArrayList<Coordinate> coordinateList = new ArrayList<Coordinate>();
        coordinateList.add(new Coordinate(33.2403307, 126.5624673));
        coordinateList.add(new Coordinate(33.2403234, 126.5627931));
        coordinateList.add(new Coordinate(33.2402282, 126.5630821));
        coordinateList.add(new Coordinate(33.2401702, 126.5632367));
        coordinateList.add(new Coordinate(33.2403103, 126.5634159));
        coordinateList.add(new Coordinate(33.2404554, 126.5635482));
        coordinateList.add(new Coordinate(33.2408904, 126.5637502));
        coordinateList.add(new Coordinate(33.2412932, 126.5638586));
        coordinateList.add(new Coordinate(33.2415727, 126.5639338));
        coordinateList.add(new Coordinate(33.2418125, 126.5640198));
        coordinateList.add(new Coordinate(33.2417782, 126.5647375));

        Coordinate startCoordinate = new Coordinate(33.2403307, 126.5624673);
	    Coordinate endCoordinate = new Coordinate(33.2417782, 126.5647375);
        
        ArrayList<Node> findPath = engine.shortestPathFind(startCoordinate, endCoordinate);
        
        findPath.forEach(item -> System.out.println(item.getCoordinate().toWKT()));

        assertThat(findPath).extracting(Node::getCoordinate)
            .usingRecursiveComparison()
            .isEqualTo(coordinateList);
    }

    @Test
    @DisplayName("경로 탐색 - 연결이 끊어져 있어 탐색이 불가한 경우")
    public void findPathTestDisconnectNode() throws IOException {
        // 126.56571449999998,33.2601044
        // 126.5662567,33.257629
        Coordinate startCoordinate = new Coordinate(33.2601044, 126.56571449999998);
        Coordinate endCoordinate = new Coordinate(33.257629, 126.5662567);
        
        ArrayList<Node> findPath = engine.shortestPathFind(startCoordinate, endCoordinate);
        
        assertThat(findPath).isNull();
    }

}
