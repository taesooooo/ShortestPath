package com.shortestpath.shortestpath.unit.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.repository.NodeIndexInsertRepository;

class NodeIndexInsertRepositoryTest {

    // @Mock
    // private JdbcTemplate jdbcTemplate;

    // private NodeIndexInsertRepository repository;

    // @BeforeEach
    // void setUp() {
    //     MockitoAnnotations.openMocks(this);
    //     repository = new NodeIndexInsertRepository(jdbcTemplate);
    // }

    // @Test
    // @DisplayName("빈 맵으로 노드 인덱스 삽입하기")
    // void testInsertNodeIndexWithEmptyMap() {
    //     HashMap<Coordinate, IndexInfo> emptyMap = new HashMap<>();
    //     assertDoesNotThrow(() -> repository.insertNodeIndex(emptyMap));
    //     verify(jdbcTemplate, never()).batchUpdate(anyString(), any());
    // }

    // @Test
    // @DisplayName("단일 배치로 노드 인덱스 삽입하기")
    // void testInsertNodeIndexWithSingleBatch() {
    //     HashMap<Coordinate, IndexInfo> indexMap = new HashMap<>();
    //     indexMap.put(new Coordinate(10.0, 20.0), new IndexInfo(1, 0, 0));

    //     when(jdbcTemplate.batchUpdate(anyString(), any())).thenReturn(new int[]{1});
    //     assertDoesNotThrow(() -> repository.insertNodeIndex(indexMap));
    //     verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any());
    // }

    // @Test
    // @DisplayName("다중 배치로 노드 인덱스 삽입하기")
    // void testInsertNodeIndexWithMultipleBatches() {
    //     HashMap<Coordinate, IndexInfo> indexMap = new HashMap<>();
    //     for (int i = 0; i < 1500; i++) {
    //         indexMap.put(new Coordinate(10.0 + i, 20.0 + i), new IndexInfo(i, i));
    //     }

    //     when(jdbcTemplate.batchUpdate(anyString(), any())).thenReturn(new int[]{1000, 500});
    //     assertDoesNotThrow(() -> repository.insertNodeIndex(indexMap));
    //     verify(jdbcTemplate, times(2)).batchUpdate(anyString(), any());
    // }

    // @Test
    // @DisplayName("런타임 예외 발생 시 처리하기")
    // void testInsertNodeIndexThrowsRuntimeException() {
    //     HashMap<Coordinate, IndexInfo> indexMap = new HashMap<>();
    //     indexMap.put(new Coordinate(10.0, 20.0), new IndexInfo(1, 0));

    //     when(jdbcTemplate.batchUpdate(anyString(), any())).thenThrow(new RuntimeException("DB Error"));
    //     assertThrows(RuntimeException.class, () -> repository.insertNodeIndex(indexMap));
    // }

    // @Test
    // @DisplayName("유효한 데이터로 노드 인덱스 성공적으로 삽입하기")
    // void testInsertNodeIndexSuccessfully() {
    //     HashMap<Coordinate, IndexInfo> indexMap = new HashMap<>();
    //     indexMap.put(new Coordinate(5.0, 15.0), new IndexInfo(2, 1));

    //     when(jdbcTemplate.batchUpdate(anyString(), any())).thenReturn(new int[]{1});
    //     assertDoesNotThrow(() -> repository.insertNodeIndex(indexMap));
    //     verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any());
    // }
}