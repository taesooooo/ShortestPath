package com.shortestpath.shortestpath.core.pathengine.Store.Index;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * 인메모리 Edge 인덱스 구현
 * HashMap을 사용하여 Edge ID -> 오프셋 매핑
 * 빠른 읽기/쓰기에 최적화
 */
@Slf4j
public class InMemoryEdgeIndex implements EdgeIndex {
    private final Map<Integer, EdgeIndexEntry> indexMap;
    
    public InMemoryEdgeIndex() {
        this.indexMap = new HashMap<>();
        log.info("InMemoryEdgeIndex 초기화 완료");
    }
    
    public InMemoryEdgeIndex(int initialCapacity) {
        this.indexMap = new HashMap<>(initialCapacity);
        log.info("InMemoryEdgeIndex 초기화 완료 - initialCapacity: {}", initialCapacity);
    }
    
    @Override
    public void put(EdgeIndexEntry entry) throws IOException {
        indexMap.put(entry.getNodeId(), entry);
    }
    
    @Override
    public EdgeIndexEntry get(int nodeId) {
        return indexMap.get(nodeId);
    }
    
    @Override
    public boolean containsKey(int nodeId) {
        return indexMap.containsKey(nodeId);
    }
    
    @Override
    public int size() {
        return indexMap.size();
    }
    
    @Override
    public void flush() throws IOException {
        // 인메모리 구현이므로 flush 불필요
        log.debug("InMemoryEdgeIndex flush 호출 (no-op)");
    }
    
    @Override
    public void load() throws IOException {
        // 인메모리 구현이므로 load 불필요
        log.debug("InMemoryEdgeIndex load 호출 (no-op)");
    }
    
    @Override
    public void close() throws IOException {
        log.info("InMemoryEdgeIndex 리소스 해제 - 총 {} 개의 인덱스", indexMap.size());
        indexMap.clear();
    }
    
    @Override
    public void clear() {
        indexMap.clear();
        log.info("InMemoryEdgeIndex 초기화됨");
    }
    
    /**
     * 현재 인덱스 맵 반환 (테스트/디버깅용)
     * @return 인덱스 맵
     */
    public Map<Integer, EdgeIndexEntry> getIndexMap() {
        return indexMap; 
    }
}
