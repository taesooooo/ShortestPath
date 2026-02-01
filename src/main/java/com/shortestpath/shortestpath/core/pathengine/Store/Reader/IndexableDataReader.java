package com.shortestpath.shortestpath.core.pathengine.Store.Reader;

import java.io.IOException;

/**
 * 좌표 인덱싱과 오프셋 조회를 지원하는 DataReader 확장 인터페이스
 */
public interface IndexableDataReader extends DataReader {
    /**
     * 좌표로부터 노드 오프셋 조회
     * @param coordinate 조회할 좌표
     * @return 노드 오프셋, 없으면 -1 반환
     */
    int getNodeOffset(com.shortestpath.shortestpath.core.pathengine.Coordinate coordinate);
}
