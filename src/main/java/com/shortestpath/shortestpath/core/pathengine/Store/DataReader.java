package com.shortestpath.shortestpath.core.pathengine.Store;

import java.io.IOException;

import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;

/**
 * 데이터 읽기 전담 인터페이스 (기본)
 * 파일 기반 데이터 스토어에서 Node와 Edge 데이터를 읽는 핵심 책임만 담당
 */
public interface DataReader {
    /**
     * 지정된 오프셋에서 Node 데이터를 읽음
     * @param offset 읽을 위치
     * @return 읽어온 Node 객체
     * @throws IOException 파일 읽기 실패 시
     */
    Node readNode(long offset) throws IOException;

    /**
     * 지정된 오프셋에서 Edge 데이터를 읽음
     * @param offset 읽을 위치
     * @return 읽어온 Edge 객체
     * @throws IOException 파일 읽기 실패 시
     */
    Edge readEdge(long offset) throws IOException;

    /**
     * 추출된 데이터 존재 여부 확인
     * @return 데이터 존재 여부
     */
    boolean hasExtractedData();

    /**
     * 리소스 해제
     * @throws IOException 해제 실패 시
     */
    void close() throws IOException;
}
