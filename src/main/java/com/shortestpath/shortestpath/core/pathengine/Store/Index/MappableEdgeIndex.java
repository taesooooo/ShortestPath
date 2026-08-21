package com.shortestpath.shortestpath.core.pathengine.Store.Index;

import java.io.IOException;

/**
 * 메모리 매핑을 지원하는 EdgeIndex 확장 인터페이스
 * EdgeIndex를 메모리 매핑 모드로 전환할 수 있는 기능 제공
 */
public interface MappableEdgeIndex extends EdgeIndex {
    /**
     * EdgeIndex를 메모리 매핑 모드로 전환
     * 대량의 읽기가 필요할 때 성능 향상
     * 메모리 매핑 모드에서는 쓰기 작업이 불가능합니다.
     * @throws IOException IO 오류 발생 시
     */
    void switchToMappingMode() throws IOException;
    
    /**
     * 현재 메모리 매핑 모드 상태 확인
     * @return 메모리 매핑 모드이면 true
     */
    boolean isMappingMode();
}
