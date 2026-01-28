package com.shortestpath.shortestpath.core.unit.Store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataWriter;

/**
 * HybridDataWriter를 테스트하는 JUnit 테스트 클래스
 * 노드와 엣지 저장, 오버라이트, 파일 공간 할당 등을 테스트합니다
 */
public class HybridDataWriterTest {

    @Test
    @DisplayName("HybridDataWriter 생성자에서 파일이 생성되는지 확인")
    public void constructorCreatesFiles() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());

            Path nodeFile = tempDir.resolve("node.bin");
            Path edgeFile = tempDir.resolve("edge.bin");

            assertThat(nodeFile).exists();
            assertThat(edgeFile).exists();
            
            writer.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("saveNode가 파일에 데이터를 올바르게 기록")
    public void saveNodeWritesDataToFile() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            
            int testId = 100;
            int testStartEdgeOffset = 200;
            double testLon = 127.5;
            double testLat = 37.5;

            Node node = new Node(testId, new Coordinate(testLat, testLon), testStartEdgeOffset, 0, 0, 0);
            int offset = writer.saveNode(node, 0L);

            // 파일에서 읽어서 검증
            Path nodeFile = tempDir.resolve("node.bin");
            FileChannel fc = FileChannel.open(nodeFile, StandardOpenOption.READ);
            ByteBuffer buf = ByteBuffer.allocate(24);
            fc.read(buf);

            buf.flip();
            int id = buf.getInt();
            int startEdgeOffset = buf.getInt();
            double lon = buf.getDouble();
            double lat = buf.getDouble();

            assertThat(id).isEqualTo(testId);
            assertThat(startEdgeOffset).isEqualTo(testStartEdgeOffset);
            assertThat(lon).isEqualTo(testLon);
            assertThat(lat).isEqualTo(testLat);
            assertThat(offset).isEqualTo(0);
            
            fc.close();
            writer.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("saveNode에 null을 전달하면 IllegalArgumentException 발생")
    public void saveNodeThrowsExceptionOnNull() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            
            assertThrows(IllegalArgumentException.class, () -> {
                writer.saveNode(null);
            });
            
            writer.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("saveEdge가 파일에 데이터를 올바르게 기록")
    public void saveEdgeWritesDataToFile() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            
            int testId = 5;
            int testFrom = 1;
            int testTo = 2;
            double testDistance = 150.5;
            int testNextEdgeOffset = 50;

            Edge edge = new Edge(testId, testFrom, testTo, testDistance, testNextEdgeOffset);
            int offset = writer.saveEdge(edge, 0L);

            // 파일에서 읽어서 검증
            Path edgeFile = tempDir.resolve("edge.bin");
            FileChannel fc = FileChannel.open(edgeFile, StandardOpenOption.READ);
            ByteBuffer buf = ByteBuffer.allocate(24);
            fc.read(buf);

            buf.flip();
            int id = buf.getInt();
            int from = buf.getInt();
            int to = buf.getInt();
            double distance = buf.getDouble();
            int nextEdgeOffset = buf.getInt();

            assertThat(id).isEqualTo(testId);
            assertThat(from).isEqualTo(testFrom);
            assertThat(to).isEqualTo(testTo);
            assertThat(distance).isEqualTo(testDistance);
            assertThat(nextEdgeOffset).isEqualTo(testNextEdgeOffset);
            assertThat(offset).isEqualTo(0);
            
            fc.close();
            writer.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("saveEdge에 null을 전달하면 IllegalArgumentException 발생")
    public void saveEdgeThrowsExceptionOnNull() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            
            assertThrows(IllegalArgumentException.class, () -> {
                writer.saveEdge(null);
            });
            
            writer.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("overwriteNode가 지정된 오프셋의 데이터를 덮어쓰기")
    public void overwriteNodeOverwritesData() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            
            // 첫 번째 노드 저장
            Node node1 = new Node(1, new Coordinate(37.5, 127.5), 0, 0, 0, 0);
            writer.saveNode(node1, 0L);
            
            // 두 번째 노드로 덮어쓰기
            Node node2 = new Node(2, new Coordinate(37.6, 127.6), 100, 0, 0, 0);
            writer.overwriteNode(node2, 0L);

            // 파일에서 읽어서 검증
            Path nodeFile = tempDir.resolve("node.bin");
            FileChannel fc = FileChannel.open(nodeFile, StandardOpenOption.READ);
            ByteBuffer buf = ByteBuffer.allocate(24);
            fc.read(buf);

            buf.flip();
            int id = buf.getInt();

            assertThat(id).isEqualTo(2);
            
            fc.close();
            writer.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("overwriteEdge가 지정된 오프셋의 데이터를 덮어쓰기")
    public void overwriteEdgeOverwritesData() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            
            // 첫 번째 엣지 저장
            Edge edge1 = new Edge(1, 10, 20, 100.0, 50);
            writer.saveEdge(edge1, 0L);
            
            // 두 번째 엣지로 덮어쓰기
            Edge edge2 = new Edge(2, 15, 25, 200.0, 75);
            writer.overwriteEdge(edge2, 0L);

            // 파일에서 읽어서 검증
            Path edgeFile = tempDir.resolve("edge.bin");
            FileChannel fc = FileChannel.open(edgeFile, StandardOpenOption.READ);
            ByteBuffer buf = ByteBuffer.allocate(24);
            fc.read(buf);

            buf.flip();
            int id = buf.getInt();

            assertThat(id).isEqualTo(2);
            
            fc.close();
            writer.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("allocateNodeFileSpace가 노드 파일 공간을 할당")
    public void allocateNodeFileSpaceAllocatesSpace() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            
            long allocateSize = 24 * 100; // 100개 노드 공간
            writer.allocateNodeFileSpace(allocateSize);

            Path nodeFile = tempDir.resolve("node.bin");
            long fileSize = Files.size(nodeFile);

            assertThat(fileSize).isGreaterThanOrEqualTo(allocateSize);
            
            writer.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("allocateEdgeFileSpace가 엣지 파일 공간을 할당")
    public void allocateEdgeFileSpaceAllocatesSpace() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            
            long allocateSize = 24 * 100; // 100개 엣지 공간
            writer.allocateEdgeFileSpace(allocateSize);

            Path edgeFile = tempDir.resolve("edge.bin");
            long fileSize = Files.size(edgeFile);

            assertThat(fileSize).isEqualTo(allocateSize);
            
            writer.close();
        } finally {
            deleteTestDirectory(tempDir);
        }
    }

    @Test
    @DisplayName("여러 노드를 순차적으로 저장")
    public void saveMultipleNodesSequentially() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        
        try {
            HybridDataWriter writer = new HybridDataWriter(tempDir.toAbsolutePath().toString());
            
            // 3개 노드 저장
            for (int i = 0; i < 3; i++) {
                Node node = new Node(i, new Coordinate(37.5 + i, 127.5 + i), i * 10, 0, 0, 0);
                writer.saveNode(node);
            }

            Path nodeFile = tempDir.resolve("node.bin");
            long fileSize = Files.size(nodeFile);

            // 3개 노드 = 24 * 3 = 72 바이트
            assertThat(fileSize).isEqualTo(24 * 3);
            
            writer.close();
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
