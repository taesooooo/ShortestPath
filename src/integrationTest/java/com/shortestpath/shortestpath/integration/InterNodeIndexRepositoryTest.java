package com.shortestpath.shortestpath.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;
import com.shortestpath.shortestpath.entity.NodeIndex;
import com.shortestpath.shortestpath.repository.NodeIndexRepository;

import jakarta.transaction.Transactional;

@ActiveProfiles("inte")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class InterNodeIndexRepositoryTest {
    
    @Autowired
    private NodeIndexRepository nodeIndexRepository;

    @Test
    @DisplayName("노드 인덱스 조회 테스트")
    public void findByCoordinateTest() {
        nodeIndexRepository.save(new NodeIndex(1_000_053, new Coordinate(33.24019619, 126.56186369), 0));
        nodeIndexRepository.save(new NodeIndex(1_000_054, new Coordinate(33.24008459, 126.56200559), 0));

        Optional<NodeIndex> nodeIndex = nodeIndexRepository.findByCoordinate(new Coordinate(33.24019619, 126.56186369));
        
        assertThat(nodeIndex.isPresent()).as("노드 인덱스가 존재하지 않습니다.").isTrue();

        assertThat(nodeIndex.get()).as("노드 인덱스가 예상 노드와 다릅니다.").usingRecursiveComparison().isEqualTo(new NodeIndex(1_000_053, new Coordinate(33.24019619, 126.56186369), 0));
    }

    @Test
    @DisplayName("가장 가까운 노드 인덱스 조회 테스트")
    public void nearestNodeTest() {
        nodeIndexRepository.save(new NodeIndex(1_000_053, new Coordinate(33.24019619, 126.56186369), 0));
        nodeIndexRepository.save(new NodeIndex(1_000_054, new Coordinate(33.24008459, 126.56200559), 0));
        nodeIndexRepository.save(new NodeIndex(1_001_006, new Coordinate(33.24033079, 126.56246739), 0));
        nodeIndexRepository.save(new NodeIndex(1_002_943, new Coordinate(33.24030459, 126.56188999), 0));
        nodeIndexRepository.save(new NodeIndex(1_002_944, new Coordinate(33.24064889, 126.56173109), 0));

        // 100미터 이내 가까운 노드 인덱스 조회
        Coordinate coordinate = new Coordinate(33.24033079, 126.56246739);
        String bbox = GeometryUtil.toWkt(GeometryUtil.createSearchEnvelope(coordinate, 100));
        List<NodeIndex> nodeIndex = nodeIndexRepository.findNearestNode(bbox, coordinate);
        
        assertThat(nodeIndex.isEmpty()).as("가장 가까운 노드가 존재하지 않습니다.").isFalse();

        assertThat(nodeIndex.get(0)).as("가장 가까운 노드가 예상 노드와 다릅니다.").usingRecursiveComparison().isEqualTo(new NodeIndex(1_001_006, new Coordinate(33.24033079, 126.56246739), 0));
    }
}
