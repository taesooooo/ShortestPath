package com.shortestpath.shortestpath.core.pathengine.Store.Index;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shortestpath.shortestpath.core.pathengine.RoadLevel;

/**
 * InMemoryEdgeIndex 유닛 테스트
 */
@DisplayName("InMemoryEdgeIndex 테스트")
class InMemoryEdgeIndexTest {
    
    private InMemoryEdgeIndex index;
    
    @BeforeEach
    void setUp() {
        index = new InMemoryEdgeIndex();
    }
    
    @Test
    @DisplayName("기본 생성자로 인덱스 초기화")
    void testDefaultConstructor() {
        assertThat(index).isNotNull();
        assertThat(index.size()).isZero();
    }
    
    @Test
    @DisplayName("초기 용량 지정하여 인덱스 초기화")
    void testConstructorWithInitialCapacity() {
        InMemoryEdgeIndex indexWithCapacity = new InMemoryEdgeIndex(100);
        assertThat(indexWithCapacity).isNotNull();
        assertThat(indexWithCapacity.size()).isZero();
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
        for (int i = 1; i <= 5; i++) {
            EdgeIndexEntry entry = createTestEntry(i, i * 100L, i * 200L, i * 300L);
            index.put(entry);
        }
        
        // Then
        assertThat(index.size()).isEqualTo(5);
        
        for (int i = 1; i <= 5; i++) {
            EdgeIndexEntry retrieved = index.get(i);
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getNodeId()).isEqualTo(i);
            assertThat(retrieved.getLevel0EdgeIndex().getStartOffset()).isEqualTo(i * 100L);
        }
    }
    
    @Test
    @DisplayName("존재하지 않는 노드 ID 조회")
    void testGetNonExistentNode() {
        // When
        EdgeIndexEntry retrieved = index.get(999);
        
        // Then
        assertThat(retrieved).isNull();
    }
    
    @Test
    @DisplayName("엔트리 덮어쓰기")
    void testOverwriteEntry() throws IOException {
        // Given
        EdgeIndexEntry entry1 = createTestEntry(1, 100L, 200L, 300L);
        EdgeIndexEntry entry2 = createTestEntry(1, 400L, 500L, 600L);
        
        // When
        index.put(entry1);
        index.put(entry2);
        EdgeIndexEntry retrieved = index.get(1);
        
        // Then
        assertThat(index.size()).isEqualTo(1); // 크기는 1개 유지
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getLevel0EdgeIndex().getStartOffset()).isEqualTo(400L);
        assertThat(retrieved.getLevel1EdgeIndex().getStartOffset()).isEqualTo(500L);
        assertThat(retrieved.getLevel2EdgeIndex().getStartOffset()).isEqualTo(600L);
    }
    
    @Test
    @DisplayName("노드 ID 존재 확인 - 존재하는 경우")
    void testContainsKeyExists() throws IOException {
        // Given
        EdgeIndexEntry entry = createTestEntry(1, 100L, 200L, 300L);
        index.put(entry);
        
        // When & Then
        assertThat(index.containsKey(1)).isTrue();
    }
    
    @Test
    @DisplayName("노드 ID 존재 확인 - 존재하지 않는 경우")
    void testContainsKeyNotExists() {
        // When & Then
        assertThat(index.containsKey(999)).isFalse();
    }
    
    @Test
    @DisplayName("clear() 메서드 테스트")
    void testClear() throws IOException {
        // Given
        index.put(createTestEntry(1, 100L, 200L, 300L));
        index.put(createTestEntry(2, 100L, 200L, 300L));
        assertThat(index.size()).isEqualTo(2);
        
        // When
        index.clear();
        
        // Then
        assertThat(index.size()).isZero();
        assertThat(index.containsKey(1)).isFalse();
        assertThat(index.containsKey(2)).isFalse();
    }
    
    @Test
    @DisplayName("flush() 메서드 호출 - no-op")
    void testFlush() {
        // When & Then
        assertThatCode(() -> index.flush()).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("load() 메서드 호출 - no-op")
    void testLoad() {
        // When & Then
        assertThatCode(() -> index.load()).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("close() 메서드 테스트")
    void testClose() throws IOException {
        // Given
        index.put(createTestEntry(1, 100L, 200L, 300L));
        index.put(createTestEntry(2, 100L, 200L, 300L));
        
        // When
        index.close();
        
        // Then
        assertThat(index.size()).isZero(); // close 후 clear됨
    }
    
    @Test
    @DisplayName("getIndexMap() 메서드로 내부 맵 접근")
    void testGetIndexMap() throws IOException {
        // Given
        index.put(createTestEntry(1, 100L, 200L, 300L));
        index.put(createTestEntry(2, 100L, 200L, 300L));
        
        // When
        var indexMap = index.getIndexMap();
        
        // Then
        assertThat(indexMap).isNotNull()
            .hasSize(2)
            .containsKeys(1, 2);
    }
    
    @Test
    @DisplayName("Level별 EdgeCount 확인")
    void testLevelEdgeCount() throws IOException {
        // Given
        EdgeIndexEntry entry = new EdgeIndexEntry(1);
        entry.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 100L, 5));
        entry.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 200L, 10));
        entry.setLevel2EdgeIndex(new LevelEdgeIndex(RoadLevel.L2, 300L, 15));
        
        // When
        index.put(entry);
        EdgeIndexEntry retrieved = index.get(1);
        
        // Then
        assertThat(retrieved.getLevel0EdgeIndex().getEdgeCount()).isEqualTo(5);
        assertThat(retrieved.getLevel1EdgeIndex().getEdgeCount()).isEqualTo(10);
        assertThat(retrieved.getLevel2EdgeIndex().getEdgeCount()).isEqualTo(15);
    }
    
    @Test
    @DisplayName("RoadLevel 확인")
    void testRoadLevels() throws IOException {
        // Given
        EdgeIndexEntry entry = new EdgeIndexEntry(1);
        entry.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.L0, 100L, 1));
        entry.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.L1, 200L, 1));
        entry.setLevel2EdgeIndex(new LevelEdgeIndex(RoadLevel.L2, 300L, 1));
        
        // When
        index.put(entry);
        EdgeIndexEntry retrieved = index.get(1);
        
        // Then
        assertThat(retrieved.getLevel0EdgeIndex().getLevel()).isEqualTo(RoadLevel.L0);
        assertThat(retrieved.getLevel1EdgeIndex().getLevel()).isEqualTo(RoadLevel.L1);
        assertThat(retrieved.getLevel2EdgeIndex().getLevel()).isEqualTo(RoadLevel.L2);
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
