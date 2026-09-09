package com.shortestpath.shortestpath.persistance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Envelope;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.model.NodeIndex;
import com.shortestpath.shortestpath.exception.NodeIndexNotFoundException;
import com.shortestpath.shortestpath.mapper.NodeIndexMapper;

@ExtendWith(MockitoExtension.class)
class MyBatisNodeProviderTest {

    @Mock
    private NodeIndexMapper nodeIndexMapper;

    @Mock
    private NodeIndex nodeIndex;

    private MyBatisNodePersistance persistance;

    @BeforeEach
    void setUp() {
        persistance = new MyBatisNodePersistance(nodeIndexMapper);
    }
    
    @Test
    @DisplayName("노드 인덱스 저장 - 배치 개수 확인")
    void insertNodeIndexBatchSizeCheck() {
        List<IndexInfo> indexList = new ArrayList<>();
        for (int i = 0; i < 10001; i++) {
            IndexInfo indexInfo = new IndexInfo(i, i, i);
            indexList.add(indexInfo);
        }

        persistance.saveNodeIndex(indexList);

        verify(nodeIndexMapper, times(2)).insertNodeIndexBatch(anyList());
    }

    @Test
    @DisplayName("좌표로 조회한 노드의 offset을 반환한다")
    void getNodeIndexReturnsOffset() {
        Coordinate coordinate = new Coordinate(33.24, 126.56);
        when(nodeIndexMapper.findByCoordinate(coordinate)).thenReturn(Optional.of(nodeIndex));
        when(nodeIndex.getOffset()).thenReturn(42);

        int offset = persistance.getNodeIndex(coordinate);

        assertThat(offset).isEqualTo(42);
        verify(nodeIndexMapper).findByCoordinate(coordinate);
    }

    @Test
    @DisplayName("좌표에 해당하는 노드가 없으면 예외를 던진다")
    void getNodeIndexThrowsWhenNodeDoesNotExist() {
        Coordinate coordinate = new Coordinate(33.24, 126.56);
        when(nodeIndexMapper.findByCoordinate(coordinate)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> persistance.getNodeIndex(coordinate))
                .isInstanceOf(NodeIndexNotFoundException.class)
                .hasMessage("노드 인덱스가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("검색 영역에서 가장 가까운 노드의 좌표를 반환한다")
    void getNearestNodeReturnsFirstCoordinate() {
        Envelope envelope = new Envelope(126.55, 126.57, 33.23, 33.25);
        Coordinate target = new Coordinate(33.24, 126.56);
        Coordinate nearestCoordinate = new Coordinate(33.2401, 126.5601);
        when(nodeIndexMapper.findNearestNode(any(String.class), eq(target)))
                .thenReturn(List.of(nodeIndex));
        when(nodeIndex.getCoordinate()).thenReturn(nearestCoordinate);

        Coordinate result = persistance.getNearestNode(envelope, target);

        assertThat(result).isEqualTo(nearestCoordinate);
        verify(nodeIndexMapper).findNearestNode(any(String.class), eq(target));
    }

    @Test
    @DisplayName("가장 가까운 노드가 없으면 예외를 던진다")
    void getNearestNodeThrowsWhenNoNodeExists() {
        Envelope envelope = new Envelope(126.55, 126.57, 33.23, 33.25);
        Coordinate target = new Coordinate(33.24, 126.56);
        when(nodeIndexMapper.findNearestNode(any(String.class), eq(target)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> persistance.getNearestNode(envelope, target))
                .isInstanceOf(NodeIndexNotFoundException.class)
                .hasMessage("가장 가까운 노드 인덱스를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("최근접 노드 목록에서 ID 목록을 반환한다")
    void findNearestNodeIdReturnsNodeIds() {
        Envelope envelope = new Envelope(126.55, 126.57, 33.23, 33.25);
        Coordinate target = new Coordinate(33.24, 126.56);
        NodeIndex firstNode = mock(NodeIndex.class);
        NodeIndex secondNode = mock(NodeIndex.class);
        when(nodeIndexMapper.findNearestNode(any(String.class), eq(target)))
                .thenReturn(List.of(firstNode, secondNode));
        when(firstNode.getId()).thenReturn(10);
        when(secondNode.getId()).thenReturn(20);

        List<Integer> result = persistance.findNearestNodeId(envelope, target);

        assertThat(result).containsExactly(10, 20);
        verify(nodeIndexMapper).findNearestNode(any(String.class), eq(target));
    }
}
