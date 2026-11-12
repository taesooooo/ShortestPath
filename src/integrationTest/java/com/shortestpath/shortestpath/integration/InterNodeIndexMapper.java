package com.shortestpath.shortestpath.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;
import com.shortestpath.shortestpath.core.pathengine.model.NodeIndex;
import com.shortestpath.shortestpath.mapper.NodeIndexMapper;

@MybatisTest
@ActiveProfiles("inte")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan(basePackages = "com.shortestpath.shortestpath.mapper")
public class InterNodeIndexMapper {
    
    @Autowired
    private NodeIndexMapper nodeIndexMapper;
    
    @BeforeEach
    public void setUp() {
        // 임시 테스트 데이터
        List<IndexInfo> tempIndexList = List.of(
            new IndexInfo(10000001, GeometryUtil.coordinateToLong(new org.locationtech.jts.geom.Coordinate(0, 1)), 1001),
            new IndexInfo(10000002, GeometryUtil.coordinateToLong(new org.locationtech.jts.geom.Coordinate(0, 2)), 1002),
            new IndexInfo(10000003, GeometryUtil.coordinateToLong(new org.locationtech.jts.geom.Coordinate(0, 3)), 1003),
            new IndexInfo(10000004, GeometryUtil.coordinateToLong(new org.locationtech.jts.geom.Coordinate(0, 4)), 1004),
            new IndexInfo(10000005, GeometryUtil.coordinateToLong(new org.locationtech.jts.geom.Coordinate(0, 5)), 1005)
        );

        nodeIndexMapper.insertNodeIndexBatch(tempIndexList);
    }
    
    @Test
    @DisplayName("노드 인덱스 배치 삽입 테스트")
    public void nodeIndexInsertBatchTest() {
         // 좌표와 인덱스 정보를 저장할 해시맵 생성
        List<IndexInfo> indexList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            IndexInfo indexInfo = new IndexInfo(10000006 + i, i, i);
            indexList.add(indexInfo);
        }

        // 테스트 시작
        nodeIndexMapper.insertNodeIndexBatch(indexList);

        int id = nodeIndexMapper.findOffsetById(10000006);
        assertThat(id).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Coordinate로 노드 인덱스 가져오기")
    public void findbyCoordinateTest() {
        Coordinate coordinate = new Coordinate(2, 0);

        NodeIndex nodeIndex = nodeIndexMapper.findByCoordinate(coordinate).get();

        assertThat(nodeIndex.getId()).isEqualTo(10000002);
    }

    @Test
    @DisplayName("Id로 노드 인덱스 오프셋 가져오기")
    public void findOffsetByIdTest() {
        int offset = nodeIndexMapper.findOffsetById(10000001);

        assertThat(offset).isEqualTo(1001);
    }
    
    @Test
    @DisplayName("가까운 노드 인덱스 가져오기")
    public void findNearestNodeTest() {
        Envelope envelope = new Envelope(0, 0, 0, 2);
        Coordinate coordinate = new Coordinate(0, 2);

        List<NodeIndex> nodeIndex = nodeIndexMapper.findNearestNode(GeometryUtil.toWkt(envelope), coordinate);

        assertThat(nodeIndex).extracting(NodeIndex::getId).containsExactly(10000001, 10000002);
    }

    
}
