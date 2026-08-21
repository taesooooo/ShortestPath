package com.shortestpath.shortestpath.core.pathengine.Store.Writer;

import java.io.IOException;
import java.util.List;

import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;

/**
 * 데이터 쓰기 전담 인터페이스 (기본)
 * 파일 기반 데이터 스토어에 Node와 Edge 데이터를 쓰는 핵심 책임만 담당
 */
public interface DataWriter {
    /**
     * Node 데이터를 파일에 저장
     * @param node 저장할 Node 객체
     * @return 저장된 오프셋
     * @throws IOException 파일 쓰기 실패 시
     */
    int saveNode(Node node) throws IOException;

    /**
     * Node 데이터를 지정된 오프셋에 저장
     * @param node 저장할 Node 객체
     * @param offset 저장 위치
     * @return 저장된 오프셋
     * @throws IOException 파일 쓰기 실패 시
     */
    int saveNode(Node node, long offset) throws IOException;

    /**
     * Edge 데이터를 파일에 저장
     * @param edge 저장할 Edge 객체
     * @return 저장된 오프셋
     * @throws IOException 파일 쓰기 실패 시
     */
    int saveEdge(Edge edge) throws IOException;

    /**
     * Edge 데이터를 지정된 오프셋에 저장
     * @param edge 저장할 Edge 객체
     * @param offset 저장 위치
     * @return 저장된 오프셋
     * @throws IOException 파일 쓰기 실패 시
     */
    int saveEdge(Edge edge, long offset) throws IOException;

    /**
     * 기존 Node 데이터를 덮어쓰기
     * @param node 덮어쓸 Node 객체
     * @param offset 덮어쓸 위치
     * @return 덮어쓴 오프셋
     * @throws IOException 파일 쓰기 실패 시
     */
    int overwriteNode(Node node, long offset) throws IOException;

    /**
     * 기존 Edge 데이터를 덮어쓰기
     * @param edge 덮어쓸 Edge 객체
     * @param offset 덮어쓸 위치
     * @return 덮어쓴 오프셋
     * @throws IOException 파일 쓰기 실패 시
     */
    int overwriteEdge(Edge edge, long offset) throws IOException;

    /**
     * 노드 인덱스 정보 저장
     * @param indexList 저장할 인덱스 정보 리스트
     * @throws IOException 파일 쓰기 실패 시
     */
    void saveNodeIndex(List<IndexInfo> indexList) throws IOException;

    /**
     * 리소스 해제
     * @throws IOException 해제 실패 시
     */
    void close() throws IOException;
}
