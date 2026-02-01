package com.shortestpath.shortestpath.core.pathengine.Store.Index;

import java.io.IOException;

/**
 * Edge 인덱스 관리 인터페이스
 * Edge ID를 기반으로 파일 오프셋을 관리
 * 
 * 용도:
 * - 추출 단계: Edge 저장 시 인덱스 기록
 * - 경로탐색 단계: Edge ID로 빠른 오프셋 조회
 */
public interface EdgeIndex {
    /**
     * Edge ID에 해당하는 오프셋 저장
     * @throws IOException IO 오류 발생 시
     */
    void put(EdgeIndexEntry entry) throws IOException;
    
    /**
     * Edge ID로 오프셋 조회
     * @param nodeId Edge ID
     * @return 파일 오프셋, 없으면 -1
     * @throws IOException IO 오류 발생 시
     */
    EdgeIndexEntry get(int nodeId) throws IOException;
    
    /**
     * Edge ID가 인덱스에 존재하는지 확인
     * @param nodeId Edge ID
     * @return 존재하면 true
     */
    boolean containsKey(int nodeId);
    
    /**
     * 저장된 인덱스 개수
     * @return 인덱스 개수
     */
    int size();
    
    /**
     * 인덱스 데이터를 영구 저장소에 플러시 (파일 기반 구현에서 사용)
     * @throws IOException IO 오류 발생 시
     */
    void flush() throws IOException;
    
    /**
     * 인덱스를 메모리로 로드 (파일 기반 구현에서 사용)
     * @throws IOException IO 오류 발생 시
     */
    void load() throws IOException;
    
    /**
     * 리소스 해제
     * @throws IOException IO 오류 발생 시
     */
    void close() throws IOException;
    
    /**
     * 인덱스 초기화
     */
    void clear();
    
    /**
     * 메모리 매핑 모드 지원 여부 확인
     * @return 지원하면 true
     */
    default boolean supportsMappingMode() {
        return false;
    }
    
    /**
     * 메모리 매핑 모드로 전환 (선택적 구현)
     * @throws IOException IO 오류 발생 시
     * @throws UnsupportedOperationException 지원하지 않는 경우
     */
    default void switchToMappingMode() throws IOException {
        throw new UnsupportedOperationException("이 구현은 메모리 매핑 모드를 지원하지 않습니다.");
    }
}
