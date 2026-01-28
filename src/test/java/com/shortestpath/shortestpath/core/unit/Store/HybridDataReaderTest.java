package com.shortestpath.shortestpath.core.unit.Store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataReader;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataWriter;

/**
 * HybridDataReader를 테스트하는 JUnit 테스트 클래스
 * 노드와 엣지 읽기, 좌표 인덱싱, 메모리 매핑 전환 등을 테스트합니다
 */
public class HybridDataReaderTest {

    @Test
    @DisplayName("HybridDataReader가 저장된 노드를 올바르게 읽음")
    public void readNodeReadsDataCorrectly() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            // 데이터 저장
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            int testId = 50;
            int testStartEdgeOffset = 100;
            double testLon = 127.5;
            double testLat = 37.5;

            Node writeNode = new Node(testId, new Coordinate(testLat, testLon), testStartEdgeOffset, 0, 0, 0);
            writer.saveNode(writeNode, 0L);
            writer.close();

            // 데이터 읽기
            Path nodeFile = tempDir.resolve("node.bin");
            Path edgeFile = tempDir.resolve("edge.bin");
            HybridDataReader reader = new HybridDataReader(nodeFile, edgeFile);
            
            Node readNode = reader.readNode(0L);

            assertThat(readNode.getId()).isEqualTo(testId);
            assertThat(readNode.getStartEdgeOffset()).isEqualTo(testStartEdgeOffset);
            assertThat(readNode.getCoordinate().getLongitude()).isEqualTo(testLon);
            assertThat(readNode.getCoordinate().getLatitude()).isEqualTo(testLat);
            
            reader.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("HybridDataReader가 저장된 엣지를 올바르게 읽음")
    public void readEdgeReadsDataCorrectly() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            // 데이터 저장
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            int testId = 10;
            int testFrom = 5;
            int testTo = 15;
            double testDistance = 250.75;
            int testNextEdgeOffset = 100;

            Edge writeEdge = new Edge(testId, testFrom, testTo, testDistance, testNextEdgeOffset);
            writer.saveEdge(writeEdge, 0L);
            writer.close();

            // 데이터 읽기
            Path nodeFile = tempDir.resolve("node.bin");
            Path edgeFile = tempDir.resolve("edge.bin");
            HybridDataReader reader = new HybridDataReader(nodeFile, edgeFile);
            
            Edge readEdge = reader.readEdge(0L);

            assertThat(readEdge.getId()).isEqualTo(testId);
            assertThat(readEdge.getFrom()).isEqualTo(testFrom);
            assertThat(readEdge.getTo()).isEqualTo(testTo);
            assertThat(readEdge.getDistance()).isEqualTo(testDistance);
            assertThat(readEdge.getNextEdgeOffset()).isEqualTo(testNextEdgeOffset);
            
            reader.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("HybridDataReader가 여러 노드를 다양한 오프셋에서 읽음")
    public void readMultipleNodesAtDifferentOffsets() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            // 여러 데이터 저장
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            for (int i = 0; i < 5; i++) {
                Node node = new Node(i, new Coordinate(37.5 + i, 127.5 + i), i * 10, 0, 0, 0);
                writer.saveNode(node, i * 24L);
            }
            writer.close();

            // 여러 위치에서 읽기
            Path nodeFile = tempDir.resolve("node.bin");
            Path edgeFile = tempDir.resolve("edge.bin");
            HybridDataReader reader = new HybridDataReader(nodeFile, edgeFile);
            
            for (int i = 0; i < 5; i++) {
                Node readNode = reader.readNode(i * 24L);
                assertThat(readNode.getId()).isEqualTo(i);
                assertThat(readNode.getCoordinate().getLatitude()).isEqualTo(37.5 + i);
            }
            
            reader.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }
    @Test
    @DisplayName("switchToMappingMode가 메모리 매핑으로 전환 후 데이터 읽음")
    public void switchToMappingModeAndRead() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            // 데이터 저장
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            Node node = new Node(99, new Coordinate(37.5, 127.5), 100, 0, 0, 0);
            writer.saveNode(node, 0L);
            writer.close();

            // 메모리 매핑 모드로 읽기
            Path nodeFile = tempDir.resolve("node.bin");
            Path edgeFile = tempDir.resolve("edge.bin");
            HybridDataReader reader = new HybridDataReader(nodeFile, edgeFile);
            
            reader.switchToMappingMode();
            
            Node readNode = reader.readNode(0L);
            assertThat(readNode.getId()).isEqualTo(99);
            
            reader.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("addCoordinateIndex와 getNodeOffset이 좌표 인덱싱 동작")
    public void coordinateIndexing() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            Path nodeFile = tempDir.resolve("node.bin");
            Path edgeFile = tempDir.resolve("edge.bin");
            Files.createFile(nodeFile);
            Files.createFile(edgeFile);
            
            HybridDataReader reader = new HybridDataReader(nodeFile, edgeFile);
            
            Coordinate coord1 = new Coordinate(37.5, 127.5);
            Coordinate coord2 = new Coordinate(37.6, 127.6);
            
            reader.addCoordinateIndex(coord1, 0);
            reader.addCoordinateIndex(coord2, 24);
            
            assertThat(reader.getNodeOffset(coord1)).isEqualTo(0);
            assertThat(reader.getNodeOffset(coord2)).isEqualTo(24);
            assertThat(reader.getNodeOffset(new Coordinate(99.9, 99.9))).isEqualTo(-1); // 존재하지 않는 좌표
            
            reader.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("readMappedNode가 메모리 매핑 버퍼에서 노드를 읽음")
    public void readMappedNodeFromBuffer() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            // 3개 노드 저장
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            for (int i = 0; i < 3; i++) {
                Node node = new Node(i, new Coordinate(37.5 + i, 127.5 + i), i * 10, 0, 0, 0);
                writer.saveNode(node, i * 24L);
            }
            writer.close();

            // 메모리 매핑 모드로 전환 후 여러 노드 읽기
            Path nodeFile = tempDir.resolve("node.bin");
            Path edgeFile = tempDir.resolve("edge.bin");
            HybridDataReader reader = new HybridDataReader(nodeFile, edgeFile);
            
            reader.switchToMappingMode();
            
            for (int i = 0; i < 3; i++) {
                Node readNode = reader.readNode(i * 24L);
                assertThat(readNode.getId()).isEqualTo(i);
                assertThat(readNode.getCoordinate().getLatitude()).isEqualTo(37.5 + i);
            }
            
            reader.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("hasExtractedData - 파일이 없을 때 Reader 생성 시 IOException 발생")
    public void readerThrowsIOExceptionWhenFilesNotExist() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            Path nodeFile = tempDir.resolve("node.bin");
            Path edgeFile = tempDir.resolve("edge.bin");
            // 파일을 생성하지 않음
            
            // Reader 생성 시 IOException 발생
            assertThrows(IOException.class, () -> new HybridDataReader(nodeFile, edgeFile));
            
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("hasExtractedData - 빈 파일일 때 false 반환")
    public void hasExtractedDataReturnsFalseWhenFilesAreEmpty() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            // 빈 파일 생성
            Path nodeFile = tempDir.resolve("node.bin");
            Path edgeFile = tempDir.resolve("edge.bin");
            Files.createFile(nodeFile);
            Files.createFile(edgeFile);
            
            HybridDataReader reader = new HybridDataReader(nodeFile, edgeFile);
            
            // 파일 크기가 0이므로 false
            assertThat(reader.hasExtractedData()).isFalse();
            
            reader.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("hasExtractedData - 파일 크기가 24바이트 이상일 때 true 반환")
    public void hasExtractedDataReturnsTrueWhenDataExists() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            // 데이터 저장
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            
            Node node = new Node(1, new Coordinate(37.5, 127.5), 0, 0, 0, 0);
            Edge edge = new Edge(1, 0, 1, 100.0, -1);
            
            writer.saveNode(node);
            writer.saveEdge(edge);
            writer.close();
            
            // 데이터 존재 확인
            Path nodeFile = tempDir.resolve("node.bin");
            Path edgeFile = tempDir.resolve("edge.bin");
            HybridDataReader reader = new HybridDataReader(nodeFile, edgeFile);
            
            // 노드와 엣지 모두 24바이트 이상이므로 true
            assertThat(reader.hasExtractedData()).isTrue();
            
            reader.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("hasExtractedData - 파일 크기가 24바이트 미만일 때 false 반환")
    public void hasExtractedDataReturnsFalseWhenFileSizeIsLessThan24Bytes() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            // 작은 파일 생성
            Path nodeFile = tempDir.resolve("node.bin");
            Path edgeFile = tempDir.resolve("edge.bin");
            
            Files.write(nodeFile, new byte[10]); // 10바이트
            Files.write(edgeFile, new byte[10]); // 10바이트
            
            HybridDataReader reader = new HybridDataReader(nodeFile, edgeFile);
            
            // 24바이트 미만이므로 false
            assertThat(reader.hasExtractedData()).isFalse();
            
            reader.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("hasExtractedData - 정확히 24바이트일 때 true 반환")
    public void hasExtractedDataReturnsTrueWhenFileSizeEquals24Bytes() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            // 정확히 24바이트 파일 생성
            Path nodeFile = tempDir.resolve("node.bin");
            Path edgeFile = tempDir.resolve("edge.bin");
            
            Files.write(nodeFile, new byte[24]); // 24바이트
            Files.write(edgeFile, new byte[24]); // 24바이트
            
            HybridDataReader reader = new HybridDataReader(nodeFile, edgeFile);
            
            // 정확히 24바이트이므로 true
            assertThat(reader.hasExtractedData()).isTrue();
            
            reader.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

     private void deleteTestDirectory(Path tempDir) throws IOException {
        Files.walk(tempDir)
            .sorted((a, b) -> b.compareTo(a))
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    // 무시
                }
            });
    }

}
