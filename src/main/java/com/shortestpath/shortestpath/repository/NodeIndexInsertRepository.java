package com.shortestpath.shortestpath.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class NodeIndexInsertRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Transactional
    public void insertNodeIndex(List<IndexInfo> indexList) {
        int batchSize = 10000;
        String sql = """
                INSERT INTO node_index (id, coordinate, offset)
                VALUES (?, ST_SRID(POINT(?, ?), 4326), ?)
                ON DUPLICATE KEY UPDATE coordinate = VALUES(coordinate), offset = VALUES(offset)
                """;
        
        for (int i = 0; i < indexList.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, indexList.size());
            List<IndexInfo> batch = indexList.subList(i, endIndex);

            try {
                long startTime = System.currentTimeMillis();
                log.info("노드 인덱스 배치 저장 시작: {} ~ {} (총 {}개)", i, endIndex - 1, indexList.size());
                batchInsert(sql, batch);
                log.info("노드 인덱스 배치 저장 완료: {} ~ {} (총 {}개, {}ms)",
                        i, endIndex - 1, indexList.size(), System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("노드 인덱스 배치 저장 실패: {} ~ {}", i, endIndex - 1, e);
                throw new RuntimeException("노드 인덱스 저장 중 오류 발생", e);
            }
        }
    };


    private void batchInsert(String sql, List<IndexInfo> batch) {
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                IndexInfo indexInfo = batch.get(i);
                org.locationtech.jts.geom.Coordinate coordinate = GeometryUtil.longToCoordinate(indexInfo.coordinate);

                ps.setInt(1, indexInfo.getNodeId());
                ps.setDouble(2, coordinate.getX());
                ps.setDouble(3, coordinate.getY());
                ps.setInt(4, indexInfo.getOffset());
            }

            @Override
            public int getBatchSize() {
                return batch.size();
            }
        });
    }
}
