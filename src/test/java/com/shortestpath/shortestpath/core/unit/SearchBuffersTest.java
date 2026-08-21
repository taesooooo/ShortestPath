package com.shortestpath.shortestpath.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shortestpath.shortestpath.core.pathengine.SearchBuffers;

class SearchBuffersTest {

    @Test
    @DisplayName("nodeId 0도 local index sentinel과 충돌하지 않고 저장된다")
    void nodeIdZeroUsesLocalIndex() {
        SearchBuffers buffers = new SearchBuffers(10);

        buffers.prepare(0);
        buffers.initializeStartNode(0);
        buffers.updateCost(3, 0, 12.5);
        buffers.markVisited(0);

        assertThat(buffers.hasCost(0)).isTrue();
        assertThat(buffers.getCurrentGCost(0)).isZero();
        assertThat(buffers.isVisited(0)).isTrue();
        assertThat(buffers.hasCost(3)).isTrue();
        assertThat(buffers.getCurrentGCost(3)).isEqualTo(12.5);
        assertThat(buffers.getPreviousNode(3)).isEqualTo(0);
    }

    @Test
    @DisplayName("prepare는 이전 탐색에서 사용한 노드만 초기화한다")
    void prepareClearsTouchedNodes() {
        SearchBuffers buffers = new SearchBuffers(10);

        buffers.prepare(0);
        buffers.initializeStartNode(0);
        buffers.updateCost(5, 0, 7.0);
        buffers.markVisited(5);

        buffers.prepare(9);

        assertThat(buffers.hasCost(0)).isFalse();
        assertThat(buffers.hasCost(5)).isFalse();
        assertThat(buffers.isVisited(5)).isFalse();
        assertThat(buffers.getCurrentGCost(5)).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    @DisplayName("nodeId가 전체 노드 범위를 벗어나면 실패한다")
    void invalidNodeIdFails() {
        SearchBuffers buffers = new SearchBuffers(10);

        assertThatThrownBy(() -> buffers.ensureCapacity(10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nodeId=10");
    }
}
