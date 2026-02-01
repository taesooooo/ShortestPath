package com.shortestpath.shortestpath.core.unit.Store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Store.DataPersistence;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;
import com.shortestpath.shortestpath.provider.JpaDataPersistence;

/**
 * HybridDataStore를 테스트하는 JUnit 테스트 클래스
 * 추출 모드(Reader + Writer 동시)와 경로탐색 모드(Reader만)를 테스트합니다
 */
public class HybridDataStoreTest {

    @Test
    @DisplayName("추출 모드 생성자에서 Reader + Writer가 모두 초기화되는지 확인")
    public void constructorInitializesReaderAndWriter() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString());

            Path nodeFile = tempDir.resolve("node.bin");
            Path edgeFile = tempDir.resolve("edge.bin");

            assertThat(nodeFile).as("Node.bin 파일이 생성되어야 합니다.").exists();
            assertThat(edgeFile).as("Edge.bin 파일이 생성되어야 합니다.").exists();
            
            store.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("추출 모드에서 노드 저장 후 즉시 읽기 가능")
    public void extractionModeCanReadAfterWrite() throws Exception {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString());
            
            int testId = 1;
            int testStartEdgeOffset = 100;
            double testLon = 127.5;
            double testLat = 37.5;
            Node node = new Node(testId, new Coordinate(testLat, testLon), testStartEdgeOffset, 0, 0, 0);

            // 쓰기
            store.saveNode(node, 0L);

            // 즉시 읽기 - 추출 단계에서 Reader+Writer 동시 사용
            Node readNode = store.readNode(0L);

            assertThat(readNode.getId()).isEqualTo(testId);
            assertThat(readNode.getCoordinate().getLatitude()).isEqualTo(testLat);
            assertThat(readNode.getCoordinate().getLongitude()).isEqualTo(testLon);

            store.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("읽기 전용 모드에서 저장을 시도하면 예외 발생")
    public void readOnlyModeThrowsExceptionOnWrite() throws Exception {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            // 먼저 쓰기 모드로 데이터 저장
            HybridDataStore writeStore = new HybridDataStore(tempDir.toAbsolutePath().toString());
            Node node = new Node(1, new Coordinate(37.5, 127.5), 0, 0, 0, 0);
            writeStore.saveNode(node, 0L);
            writeStore.close();

            // 읽기 전용 모드에서 저장 시도
            HybridDataStore readStore = new HybridDataStore(tempDir.toAbsolutePath().toString(), true);
            
            assertThrows(IllegalStateException.class, () -> {
                readStore.saveNode(node);
            });

            readStore.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("readEdge가 임의 위치에 저장된 Edge 데이터를 올바르게 반환하는지 확인")
    public void readEdgeReturnDataConfirm() throws Exception {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString());
            
            int testId = 1;
            int testFromOffset = 1;
            int testToOffset = 2;
            double testDistance = 30.5;
            int testNextEdgeOffset = 40;
            RoadLevel testRoadLevel = RoadLevel.L0;

            Edge edge = new Edge(testId, testFromOffset, testToOffset, testDistance, testNextEdgeOffset, testRoadLevel);

            store.saveEdge(edge, 26L);

            Edge readEdge = store.readEdge(26L);

            assertThat(readEdge.getId()).as("Id값이 테스트 값과 일치하지 않습니다.").isEqualTo(testId);
            assertThat(readEdge.getFrom()).as("읽어온 Edge의 From값이 일치하지 않습니다.").isEqualTo(testFromOffset);
            assertThat(readEdge.getTo()).as("읽어온 Edge의 To값이 일치하지 않습니다.").isEqualTo(testToOffset);
            assertThat(readEdge.getDistance()).as("읽어온 Edge의 Distance값이 일치하지 않습니다.").isEqualTo(testDistance);
            assertThat(readEdge.getNextEdgeOffset()).as("읽어온 Edge의 NextEdgeOffset값이 일치하지 않습니다.").isEqualTo(testNextEdgeOffset);

            store.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("노드 인덱스를 저장 할 때 Persistence로 호출하는지 확인")
    public void saveNodeIndexInPersistence() throws Exception {
        Path tempDir = Files.createTempDirectory("test");

        try {
            HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString());
            
            // DataPersistence 설정
            DataPersistence persistence = mock(JpaDataPersistence.class);
            store.setPersistence(persistence);

            store.saveNodeIndex(new ArrayList<IndexInfo>());

            verify(persistence).saveNodeIndex(any());

            store.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("노드 인덱스를 가져올 때 DB로 호출하는지 확인")
    public void getNodeIndexInPersistence() throws Exception {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString());
            
            // DataPersistence 설정
            DataPersistence persistence = mock(JpaDataPersistence.class);
            store.setPersistence(persistence);

            store.getNodeOffset(new Coordinate(33.1, 126.1));

            verify(persistence).getNodeIndex(any(Coordinate.class));
            
            store.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    // @Test
    // @DisplayName("노드 인덱스를 가져올 때 Reader에서 호출하는지 확인")
    // public void getNodeIndexInReader() throws Exception {
    //     Path tempDir = Files.createTempDirectory("test");
        
    //     try {
    //         HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString());
            
    //         // DataPersistence 설정
    //         // DataPersistence persistence = mock(JpaDataPersistence.class);

    //         store.getNodeOffset(new Coordinate(33.1, 126.1));
            
    //         ((HybridDataStore) verify(store.getDataReader())).getNodeOffset(any(Coordinate.class));

    //         store.close();
    //     } finally {
    //         deleteTestDirectory(tempDir);
    //     }
    // }

    private void deleteTestDirectory(Path tempDir) throws IOException {
        Files.walk(tempDir)
            .sorted((a, b) -> b.compareTo(a)) // 역순 정렬로 파일부터 삭제
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    // 무시
                }
            });
    }

}
