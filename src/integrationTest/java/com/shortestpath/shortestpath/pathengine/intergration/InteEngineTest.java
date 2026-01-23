package com.shortestpath.shortestpath.pathengine.intergration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
import com.shortestpath.shortestpath.core.pathengine.Engine;
import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.RouteSearchResult;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;
import com.shortestpath.shortestpath.provider.JpaNodeProvider;
import com.shortestpath.shortestpath.repository.NodeIndexInsertRepository;

import jakarta.transaction.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("inte")
@Import({
        JpaNodeProvider.class,
        NodeIndexInsertRepository.class,
        TestConfig.class,
        DBHelper.class
})
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class InteEngineTest {
    @Autowired
    DataStore dataStore;
    @Autowired
    Extractor extractor;
    @Autowired
    Loader loader;
    @Autowired
    Engine engine; 
    @Autowired
    DBHelper dbHelper;
    
    @BeforeAll
    public void setUp() throws IOException {
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

        RouteSearchResult searchResult = engine.shortestPathFind(startCoordinate, endCoordinate, false);
        ArrayList<Node> findPath = searchResult.getRouteNode();

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

        RouteSearchResult searchResult = engine.shortestPathFind(startCoordinate, endCoordinate, false);

        assertThat(searchResult.getRouteNode()).isNull();
    }

}
