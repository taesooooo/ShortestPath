package com.shortestpath.shortestpath.core.pathengine.Store.Writer;

import java.io.IOException;

/**
 * 파일 공간 사전 할당을 지원하는 DataWriter 확장 인터페이스
 * 성능 최적화를 위해 파일 공간을 사전 할당하는 구현체가 선택적으로 구현
 */
public interface AllocatableDataWriter extends DataWriter {
    /**
     * Node 파일 공간 할당
     * @param size 할당할 크기
     * @throws IOException 할당 실패 시
     */
    void allocateNodeFileSpace(long size) throws IOException;

    /**
     * Edge 파일 공간 할당
     * @param size 할당할 크기
     * @throws IOException 할당 실패 시
     */
    void allocateEdgeFileSpace(long size) throws IOException;

    /**
     * Node 파일을 지정된 크기로 축소
     * @param actualSize 실제 필요한 크기
     * @throws IOException 축소 실패 시
     */
    void truncateNodeFile(long actualSize) throws IOException;

    /**
     * Edge 파일을 지정된 크기로 축소
     * @param actualSize 실제 필요한 크기
     * @throws IOException 축소 실패 시
     */
    void truncateEdgeFile(long actualSize) throws IOException;
}
