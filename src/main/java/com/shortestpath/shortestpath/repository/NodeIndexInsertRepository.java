package com.shortestpath.shortestpath.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class NodeIndexInsertRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Transactional
    public void insertNodeIndex(HashMap<Coordinate, IndexInfo> indexMap) {
        int batchSize = 10000;
        String sql = "INSERT INTO node_index (id, coordinate, offset) VALUES (?, ST_SRID(POINT(?, ?), 4326), ?)";
        List<Map.Entry<Coordinate, IndexInfo>> entries = new ArrayList<>(indexMap.entrySet());
        
        for (int i = 0; i < entries.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, entries.size());
            List<Map.Entry<Coordinate, IndexInfo>> batch = entries.subList(i, endIndex);

            try {
                batchInsert(sql, batch);
                log.info("노드 인덱스 배치 저장 완료: {} ~ {} (총 {}개)", i, endIndex - 1, indexMap.size());
            } catch (Exception e) {
                log.error("노드 인덱스 배치 저장 실패: {} ~ {}", i, endIndex - 1, e);
                throw new RuntimeException("노드 인덱스 저장 중 오류 발생", e);
            }
        }
    };


    private void batchInsert(String sql, List<Map.Entry<Coordinate, IndexInfo>> batch) {
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Map.Entry<Coordinate, IndexInfo> entry = batch.get(i);
                Coordinate coordinate = entry.getKey();
                IndexInfo indexInfo = entry.getValue();

                ps.setInt(1, indexInfo.getId());
                ps.setDouble(2, coordinate.getLongitude());
                ps.setDouble(3, coordinate.getLatitude());
                ps.setInt(4, indexInfo.getNodeIndex());
            }

            @Override
            public int getBatchSize() {
                return batch.size();
            }
        });
    }
}
