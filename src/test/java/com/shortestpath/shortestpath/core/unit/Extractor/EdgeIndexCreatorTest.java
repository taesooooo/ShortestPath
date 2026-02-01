package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.Extractor.EdgeIndexCreator;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndexEntry;

public class EdgeIndexCreatorTest {

    @Mock
    private DataStore mockDataStore;
    
    private EdgeIndexCreator edgeIndexCreator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        edgeIndexCreator = new EdgeIndexCreator(mockDataStore);
    }

    @Test
    @DisplayName("엣지 인덱스 저장 및 확인")
    public void testCreateEdgeIndexSave() throws IOException {
        EdgeIndex testEdgeIndex = mock(EdgeIndex.class);
        Edge testEdge = new Edge(0, 11, 22, 0, -1, RoadLevel.L0);

        when(mockDataStore.getTotalEdges()).thenReturn(1);
        when(mockDataStore.getEdgeIndex()).thenReturn(testEdgeIndex);
        when(mockDataStore.readEdge(anyLong())).thenReturn(testEdge);

        edgeIndexCreator.createEdgeIndex();
        
        ArgumentCaptor<EdgeIndexEntry> captor = ArgumentCaptor.forClass(EdgeIndexEntry.class);
        verify(testEdgeIndex).put(captor.capture());

        assertThat(captor.getValue().getNodeId()).isEqualTo(11);
        assertThat(captor.getValue().getLevel0EdgeIndex().getStartOffset()).isEqualTo(DataStructureSizes.calculateEdgeOffset(0));
        assertThat(captor.getValue().getLevel0EdgeIndex().getEdgeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 엣지 인덱스 저장 및 확인")
    public void testCreateEdgeIndexMultipleSave() throws IOException {
        EdgeIndex testEdgeIndex = mock(EdgeIndex.class);
        Edge testEdge1 = new Edge(0, 11, 22, 0, -1, RoadLevel.L0);
        Edge testEdge2 = new Edge(1, 11, 33, 0, -1, RoadLevel.L1);
        Edge testEdge3 = new Edge(2, 11, 55, 0, -1, RoadLevel.L1);
        Edge testEdge4 = new Edge(3, 44, 55, 0, -1, RoadLevel.L2);

        when(mockDataStore.getTotalEdges()).thenReturn(4);
        when(mockDataStore.getEdgeIndex()).thenReturn(testEdgeIndex);
        when(mockDataStore.readEdge(anyLong())).thenReturn(testEdge1, testEdge2, testEdge3, testEdge4);

        edgeIndexCreator.createEdgeIndex();
        
        ArgumentCaptor<EdgeIndexEntry> captor = ArgumentCaptor.forClass(EdgeIndexEntry.class);
        verify(testEdgeIndex, times(2)).put(captor.capture());

        assertThat(captor.getAllValues().size()).isEqualTo(2);

        assertThat(captor.getAllValues()).extracting(EdgeIndexEntry::getNodeId).containsExactlyInAnyOrder(11, 44);
        assertThat(captor.getAllValues().get(0)).extracting(e -> e.getLevel0EdgeIndex().getStartOffset()).isEqualTo(DataStructureSizes.calculateEdgeOffset(0));
        assertThat(captor.getAllValues().get(0)).extracting(e -> e.getLevel0EdgeIndex().getEdgeCount()).isEqualTo(1);
        assertThat(captor.getAllValues().get(0)).extracting(e -> e.getLevel1EdgeIndex().getStartOffset()).isEqualTo(DataStructureSizes.calculateEdgeOffset(1));
        assertThat(captor.getAllValues().get(0)).extracting(e -> e.getLevel1EdgeIndex().getEdgeCount()).isEqualTo(2);
        assertThat(captor.getAllValues().get(0)).extracting(e -> e.getLevel2EdgeIndex().getStartOffset()).isEqualTo(-1L);

        assertThat(captor.getAllValues().get(1)).extracting(e -> e.getLevel0EdgeIndex().getStartOffset()).isEqualTo(-1L);
        assertThat(captor.getAllValues().get(1)).extracting(e -> e.getLevel1EdgeIndex().getStartOffset()).isEqualTo(-1L);
        assertThat(captor.getAllValues().get(1)).extracting(e -> e.getLevel2EdgeIndex().getStartOffset()).isEqualTo(DataStructureSizes.calculateEdgeOffset(3));
        assertThat(captor.getAllValues().get(1)).extracting(e -> e.getLevel2EdgeIndex().getEdgeCount()).isEqualTo(1);
    }
}
