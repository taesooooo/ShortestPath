package com.shortestpath.shortestpath.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.repository.NodeIndexInsertRepository;

@ActiveProfiles("inte")
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({NodeIndexInsertRepository.class})
public class InterNodeIndexBatchRepositoryTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NodeIndexInsertRepository nodeIndexInsertRepository;

    @Test
    @DisplayName("노드 인덱스 배치 저장 롤백 테스트")
    public void batchInsertNodeIndexRollbackTest() {
        // 좌표와 인덱스 정보를 저장할 해시맵 생성
        HashMap<Coordinate, IndexInfo> indexMap = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            Coordinate coordinate = new Coordinate(37.5 + (i * 0.001), 127.0 + (i * 0.001));
            IndexInfo indexInfo = new IndexInfo(i, i, i);
            indexMap.put(coordinate, indexInfo);
        }

        // 강제로 예외를 발생시키기 위한 데이터 
        indexMap.put(null, null);
        

        // 테스트 시작
        try {
            nodeIndexInsertRepository.insertNodeIndex(indexMap);
        } catch (Exception e) {
            // 예외가 발생하는 것은 예상된 동작이므로 무시
        }

        Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM node_index",
        Integer.class);

        assertThat(count).as("데이터가 롤백되지 않았습니다.").isEqualTo(0);
    }
}
