package com.shortestpath.shortestpath.core.pathengine.Store.Index;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.shortestpath.shortestpath.core.pathengine.RoadLevel;

/**
 * FileBasedEdgeIndex 유닛 테스트
 */
@DisplayName("FileBasedEdgeIndex 테스트")
class FileBasedEdgeIndexTest {
    
    @TempDir
    Path tempDir;
    
    private FileBasedEdgeIndex index;
    private Path indexFilePath;
    
    @BeforeEach
    void setUp() throws IOException {
        indexFilePath = tempDir.resolve("test_edge_index.bin");
        index = new FileBasedEdgeIndex(indexFilePath);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        if (index != null) {
            index.close();
        }
    }
    
    @Test
    @DisplayName("인덱스 파일 생성 확인")
    void testIndexFileCreation() {
        assertThat(Files.exists(indexFilePath)).isTrue();
        assertThat(index.getIndexFilePath()).isEqualTo(indexFilePath);
    }
    
    @Test
    @DisplayName("엔트리 저장 및 조회")
    void testPutAndGet() throws IOException {
        // Given
        EdgeIndexEntry entry = createTestEntry(1, 100L, 200L, 300L);
        
        // When
        index.put(entry);
        EdgeIndexEntry retrieved = index.get(1);
        
        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getNodeId()).isEqualTo(1);
        assertThat(retrieved.getLevel0EdgeIndex().getStartOffset()).isEqualTo(100L);
        assertThat(retrieved.getLevel1EdgeIndex().getStartOffset()).isEqualTo(200L);
        assertThat(retrieved.getLevel2EdgeIndex().getStartOffset()).isEqualTo(300L);
    }

    @Test
    @DisplayName("여러 엔트리 저장 및 조회")
    void testPutMultipleEntries() throws IOException {
        // Given & When
        for (int i = 0; i < 5; i++) {
            EdgeIndexEntry entry = createTestEntry(i, i * 100L, i * 200L, i * 300L);
            index.put(entry);
        }
        
        // Then
        for (int i = 0; i < 5; i++) {
            EdgeIndexEntry retrieved = index.get(i);
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getNodeId()).isEqualTo(i);
            assertThat(retrieved.getLevel0EdgeIndex().getStartOffset()).isEqualTo(i * 100L);
        }
    }
    
    @Test
    @DisplayName("flush() 메서드 테스트")
    void testFlush() throws IOException {
        // Given
        EdgeIndexEntry entry = createTestEntry(1, 100L, 200L, 300L);
        index.put(entry);
        
        // When & Then
        assertThatCode(() -> index.flush()).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("매핑 모드 지원 확인")
    void testSupportsMappingMode() {
        assertThat(index.supportsMappingMode()).isTrue();
    }
    
    @Test
    @DisplayName("매핑 모드로 전환 및 조회")
    void testSwitchToMappingMode() throws IOException {
        // Given
        EdgeIndexEntry entry1 = createTestEntry(0, 100L, 200L, 300L);
        EdgeIndexEntry entry2 = createTestEntry(1, 400L, 500L, 600L);
        index.put(entry1);
        index.put(entry2);
        
        // When
        index.switchToMappingMode();
        assertThat(index.isMappingMode()).isTrue();
        
        // Then - 매핑 모드에서도 조회 가능
        EdgeIndexEntry retrieved1 = index.get(0);
        EdgeIndexEntry retrieved2 = index.get(1);
        
        assertThat(retrieved1).isNotNull();
        assertThat(retrieved2).isNotNull();
        assertThat(retrieved1.getLevel0EdgeIndex().getStartOffset()).isEqualTo(100L);
        assertThat(retrieved2.getLevel0EdgeIndex().getStartOffset()).isEqualTo(400L);
    }
    
    @Test
    @DisplayName("매핑 모드에서 쓰기 시도 시 예외 발생")
    void testWriteInMappingModeThrowsException() throws IOException {
        // Given
        index.switchToMappingMode();
        EdgeIndexEntry entry = createTestEntry(1, 100L, 200L, 300L);
        
        // When & Then
        assertThatThrownBy(() -> index.put(entry))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("매핑 모드에서는 쓰기가 불가능");
    }
    
    @Test
    @DisplayName("load() 메서드로 헤더 읽기")
    void testLoad() throws IOException {
        // Given
        index.put(createTestEntry(0, 100L, 200L, 300L));
        index.put(createTestEntry(1, 200L, 300L, 400L));
        index.flush();
        
        // When
        index.load();
        
        assertThat(index.size()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("containsKey() 메서드 테스트")
    void testContainsKey() throws IOException {
        // Given
        EdgeIndexEntry entry = createTestEntry(0, 100L, 200L, 300L);
        index.put(entry);
        
        // When & Then
        assertThat(index.containsKey(0)).isTrue();
        assertThat(index.containsKey(999)).isFalse();
    }
    
    @Test
    @DisplayName("clear() 메서드 테스트")
    void testClear() throws IOException {
        // Given
        EdgeIndexEntry entry = createTestEntry(0, 100L, 200L, 300L);
        index.put(entry);
        
        // When
        index.clear();
        
        // Then
        assertThat(index.isMappingMode()).isFalse();
    }
    
    @Test
    @DisplayName("close() 메서드로 리소스 해제")
    void testClose() throws IOException {
        // Given
        EdgeIndexEntry entry = createTestEntry(0, 100L, 200L, 300L);
        index.put(entry);
        
        // When
        index.close();
        
        // Then - 파일은 여전히 존재해야 함
        assertThat(Files.exists(indexFilePath)).isTrue();
    }
    
    @Test
    @DisplayName("close() 후 재사용 불가")
    void testCloseAndReuse() throws IOException {
        // Given
        index.close();
        EdgeIndexEntry entry = createTestEntry(0, 100L, 200L, 300L);
        
        // When & Then - 닫힌 채널에 쓰기 시도하면 예외 발생
        assertThatThrownBy(() -> index.put(entry))
            .isInstanceOf(Exception.class);
    }
    
    @Test
    @DisplayName("Level별 EdgeCount 확인")
    void testLevelEdgeCount() throws IOException {
        // Given
        EdgeIndexEntry entry = new EdgeIndexEntry(0);
        entry.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 100L, 5));
        entry.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 200L, 10));
        entry.setLevel2EdgeIndex(new LevelEdgeIndex(RoadLevel.L2, 300L, 15));
        
        // When
        index.put(entry);
        EdgeIndexEntry retrieved = index.get(0);
        
        // Then
        assertThat(retrieved.getLevel0EdgeIndex().getEdgeCount()).isEqualTo(5);
        assertThat(retrieved.getLevel1EdgeIndex().getEdgeCount()).isEqualTo(10);
        assertThat(retrieved.getLevel2EdgeIndex().getEdgeCount()).isEqualTo(15);
    } 
    
    @Test
    @DisplayName("매핑 모드에서 대량 데이터 조회")
    void testLargeDataSetInMappingMode() throws IOException {
        // Given
        int dataCount = 500;
        for (int i = 0; i < dataCount; i++) {
            index.put(createTestEntry(i, i * 100L, i * 200L, i * 300L));
        }
        
        // When
        index.switchToMappingMode();
        
        // Then
        for (int i = 0; i < dataCount; i++) {
            EdgeIndexEntry retrieved = index.get(i);
            assertThat(retrieved.getLevel0EdgeIndex().getStartOffset()).isEqualTo(i * 100L);
        }
    }
    
    // 헬퍼 메서드
    private EdgeIndexEntry createTestEntry(int nodeId, long level0Offset, long level1Offset, long level2Offset) {
        EdgeIndexEntry entry = new EdgeIndexEntry(nodeId);
        entry.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, level0Offset, 1));
        entry.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, level1Offset, 1));
        entry.setLevel2EdgeIndex(new LevelEdgeIndex(RoadLevel.L2, level2Offset, 1));
        return entry;
    }
}
