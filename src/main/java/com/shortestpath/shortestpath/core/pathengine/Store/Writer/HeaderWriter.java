package com.shortestpath.shortestpath.core.pathengine.Store.Writer;

import java.io.IOException;

import com.shortestpath.shortestpath.core.pathengine.Store.EdgeHeader;
import com.shortestpath.shortestpath.core.pathengine.Store.NodeHeader;

/**
 * 파일 헤더 쓰기 인터페이스
 * Node 및 Edge 파일에 헤더 정보를 기록할 수 있는 Writer용 인터페이스
 */
public interface HeaderWriter {
    /**
     * Node 파일 헤더를 작성
     * @param header 작성할 Node 헤더 정보
     * @throws IOException 파일 쓰기 실패 시
     */
    void writeNodeHeader(NodeHeader header) throws IOException;

    /**
     * Edge 파일 헤더를 작성
     * @param header 작성할 Edge 헤더 정보
     * @throws IOException 파일 쓰기 실패 시
     */
    void writeEdgeHeader(EdgeHeader header) throws IOException;
}
