package com.shortestpath.shortestpath.core.pathengine.Store;

import java.io.IOException;

/**
 * 메모리 매핑 읽기를 지원하는 DataReader 확장 인터페이스
 */
public interface MappableDataReader extends DataReader {
    /**
     * 읽기 모드를 메모리 매핑 모드로 전환
     * 대량의 읽기가 필요할 때 성능 향상
     * @throws IOException 전환 실패 시
     */
    void switchToMappingMode() throws IOException;
}
