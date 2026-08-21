package com.shortestpath.shortestpath.core.pathengine.Store;

import java.io.IOException;

/**
 * 메모리 매핑을 지원하는 DataStore 확장 인터페이스
 * 기존 DataStore를 확장하여 메모리 매핑 모드 전환 기능 제공 
 */
public interface MappableDataStore extends DataStore {
    /**
     * 읽기 모드를 메모리 매핑 모드로 전환
     * 대량의 읽기가 필요할 때 성능 향상
     * @throws IOException 전환 실패 시
     */
    void switchToMappingMode() throws IOException;
    
    /**
     * DataReader를 메모리 매핑 모드로 전환
     * @throws IOException IO 오류 발생 시
     * @throws UnsupportedOperationException Reader가 메모리 매핑을 지원하지 않는 경우
     */
    void switchDataReaderToMappingMode() throws IOException;
    
    /**
     * EdgeIndex를 메모리 매핑 모드로 전환 (선택적)
     * EdgeIndex가 MappableEdgeIndex를 구현하는 경우만 전환
     * @throws IOException IO 오류 발생 시
     */
    void switchEdgeIndexToMappingMode() throws IOException;
}
