package com.shortestpath.shortestpath.core.unit.Store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.File;
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
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeIndexProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;

/**
 * 파일 기반 데이터 저장소(FileDataStore)를 테스트하는 JUnit 테스트 클래스
 */
public class HybridDataStoreTest {

    @Test
    @DisplayName("FileChannel이 파일을 생성하는지 확인")
    public void constructorCreatesFile() throws IOException {
        // 임시 파일 생성 (FileDataStore에는 경로만 전달)
        Path tempDir = Files.createTempDirectory("test");
        File tempNodeFile = File.createTempFile("filestore-node-test", ".bin", tempDir.toFile());
        File tempEdgeFile = File.createTempFile("filestore-edge-test", ".bin", tempDir.toFile());
        String nodePath = tempNodeFile.getAbsolutePath();
        String edgePath = tempEdgeFile.getAbsolutePath();

        // FileDataStore 생성 시 데이터 파일이 존재해야 함
        HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString(), mock(NodeIndexProvider.class));

        File testNodeFile = new File(nodePath);
        File testEdgeFile = new File(edgePath);

        assertThat(testNodeFile.exists()).as("생성자 호출시 Node.bin 파일이 생성되어야 합니다.").isTrue();
        assertThat(testEdgeFile.exists()).as("생성자 호출시 Edge.bin 파일이 생성되어야 합니다.").isTrue();

        // 정리: 임시 파일 삭제
        testNodeFile.delete();
        testEdgeFile.delete();
    }

    @Test
    @DisplayName("saveNode 메서드에 null을 전달하면 IllegalArgumentException이 발생하는지 확인")
    public void saveNodeNullPointerException() throws Exception {
        // 임시 파일 생성 및 경로 전달
        Path tempDir = Files.createTempDirectory("test");
        HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString(), mock(NodeIndexProvider.class));
        
        // null을 전달하면 saveNode에서 NullPointerException이 발생해야 함
        assertThrows(IllegalArgumentException.class, () -> {
            store.saveNode(null);
        });
    }

    @Test
    @DisplayName("saveNode가 파일 쓰기 데이터 확인")
    public void saveNodeWriteBytes() throws Exception {
        Path tempDir = Files.createTempDirectory("test");
        HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString(), mock(NodeIndexProvider.class));
        
        int testId = 66;
        int testStartEdgeOffset = 100;
        double testLon = 123.456;
        double testLat = 33.456;

        Node node = new Node(testId, new Coordinate(testLat, testLon), testStartEdgeOffset, 0, 0, 0);

        store.saveNode(node);

        // 파일에서 읽어서 확인
        try {
            FileChannel fc = FileChannel.open(store.getNodeFilePath().toAbsolutePath(), StandardOpenOption.READ);
            ByteBuffer buf = ByteBuffer.allocate(24);
            int read = fc.read(buf);

            buf.flip();
            int id = buf.getInt();
            int startEdgeOffset = buf.getInt();
            double lon = buf.getDouble();
            double lat = buf.getDouble();


            assertThat(id).as("Id값이 테스트 값과 일치하지 않습니다.").isEqualTo(testId);
            assertThat(startEdgeOffset).as("startEdgeOffset값이 테스트 값과 일치하지 않습니다.").isEqualTo(testStartEdgeOffset);
            assertThat(lon).as("Lon값이 테스트 값과 일치하지 않습니다.").isEqualTo(testLon);
            assertThat(lat).as("Lat값이 테스트 값과 일치하지 않습니다.").isEqualTo(testLat);
        } finally {
           testFileDelete(store);
        }
    }

    @Test
    @DisplayName("saveNode가 지정된 오프셋에 데이터를 기록하는지 확인")
    public void saveNodeWriteBytesAtOffset() throws Exception {
        Path tempDir = Files.createTempDirectory("test");
        HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString(), mock(NodeIndexProvider.class));
        
        int testId = 66;
        int testStartEdgeOffset = 100;
        double testLon = 123.456;
        double testLat = 33.456;

        Node node = new Node(testId, new Coordinate(testLat, testLon), testStartEdgeOffset, 0, 0, 0);

        store.saveNode(node, 24L);

        // 파일에서 읽어서 확인
        try {
            FileChannel fc = FileChannel.open(store.getNodeFilePath().toAbsolutePath(), StandardOpenOption.READ);
            ByteBuffer buf = ByteBuffer.allocate(24);
            
            fc.position(24L);
            
            int read = fc.read(buf);

            buf.flip();
            int id = buf.getInt();
            int startEdgeOffset = buf.getInt();
            double lon = buf.getDouble();
            double lat = buf.getDouble();

            assertThat(id).as("Id값이 테스트 값과 일치하지 않습니다.").isEqualTo(testId);
            assertThat(startEdgeOffset).as("startEdgeOffset값이 테스트 값과 일치하지 않습니다.").isEqualTo(testStartEdgeOffset);
            assertThat(lon).as("Lon값이 테스트 값과 일치하지 않습니다.").isEqualTo(testLon);
            assertThat(lat).as("Lat값이 테스트 값과 일치하지 않습니다.").isEqualTo(testLat);
        } finally {
            testFileDelete(store);
        }
    }

    @Test
    @DisplayName("saveEdge 메서드에 null을 전달하면 IllegalArgumentException이 발생하는지 확인")
    public void saveEdgeNullPointerException() throws Exception {
        // 임시 파일 생성 및 경로 전달
        Path tempDir = Files.createTempDirectory("test");
        HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString(), mock(NodeIndexProvider.class));
        
        // null을 전달하면 saveEdge에서 NullPointerException이 발생해야 함
        assertThrows(IllegalArgumentException.class, () -> {
            store.saveEdge(null);
        });
    }

    @Test
    @DisplayName("saveEdge가 파일 쓰기 데이터 확인")
    public void saveEdgeWriteBytes() throws Exception {
        Path tempDir = Files.createTempDirectory("test");
        HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString(), mock(NodeIndexProvider.class));
        
        int testId = 0;
        int testFromOffset = 66;
        int testToOffset = 777;
        double testDistance = 200;
        int testNextEdgeOffset = 280;

        Edge Edge = new Edge(testId, testFromOffset, testToOffset, testDistance, testNextEdgeOffset);

        store.saveEdge(Edge);

        // 파일에서 읽어서 확인
        try {
            FileChannel fc = FileChannel.open(store.getEdgeFilePath().toAbsolutePath(), StandardOpenOption.READ);
            ByteBuffer buf = ByteBuffer.allocate(24);
            int read = fc.read(buf);

            buf.flip();
            int id = buf.getInt();
            int fromOffset = buf.getInt();
            int toOffset = buf.getInt();
            double distance = buf.getDouble();
            int nextEdgeOffset = buf.getInt();

            assertThat(id).as("Id값이 테스트 값과 일치하지 않습니다.").isEqualTo(testId);
            assertThat(fromOffset).as("fromOffset값이 테스트 값과 일치하지 않습니다.").isEqualTo(testFromOffset);
            assertThat(toOffset).as("toOffset값이 테스트 값과 일치하지 않습니다.").isEqualTo(testToOffset);
            assertThat(distance).as("distance값이 테스트 값과 일치하지 않습니다.").isEqualTo(testDistance);
            assertThat(nextEdgeOffset).as("nextEdgeOffset값이 테스트 값과 일치하지 않습니다.").isEqualTo(testNextEdgeOffset);
        } finally {
           testFileDelete(store);
        }
    }

    @Test
    @DisplayName("saveEdge가 지정된 오프셋에 데이터를 기록하는지 확인")
    public void saveEdgeWriteBytesAtOffset() throws Exception {
        Path tempDir = Files.createTempDirectory("test");
        HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString(), mock(NodeIndexProvider.class));
        
        int testId = 0;
        int testFromOffset = 0;
        int testToOffset = 240;
        double testDistance = 100;
        int testNextEdgeOffset = 480;

        Edge Edge = new Edge(testId, testFromOffset, testToOffset, testDistance, testNextEdgeOffset);

        store.saveEdge(Edge, 20L);

        // 파일에서 읽어서 확인
        try {
            FileChannel fc = FileChannel.open(store.getEdgeFilePath().toAbsolutePath(), StandardOpenOption.READ);
            ByteBuffer buf = ByteBuffer.allocate(24);
            
            fc.position(20L);
            
            int read = fc.read(buf);

            buf.flip();
            int id = buf.getInt();
            int fromOffset = buf.getInt();
            int toOffset = buf.getInt();
            double distance = buf.getDouble();
            int nextEdgeOffset = buf.getInt();

            assertThat(id).as("Id값이 테스트 값과 일치하지 않습니다.").isEqualTo(testId);
            assertThat(fromOffset).as("fromOffset값이 테스트 값과 일치하지 않습니다.").isEqualTo(testFromOffset);
            assertThat(toOffset).as("toOffset값이 테스트 값과 일치하지 않습니다.").isEqualTo(testToOffset);
            assertThat(distance).as("distance값이 테스트 값과 일치하지 않습니다.").isEqualTo(testDistance);
            assertThat(nextEdgeOffset).as("nextEdgeOffset값이 테스트 값과 일치하지 않습니다.").isEqualTo(testNextEdgeOffset);
        } finally {
           testFileDelete(store);
        }
    }

    @Test
    @DisplayName("readNode가 임의 위치에 저장된 Node 데이터를 올바르게 반환하는지 확인")
    public void readNodeReturnDataConfirm() throws Exception {
        Path tempDir = Files.createTempDirectory("test");
        HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString(), mock(NodeIndexProvider.class));
        
        int testId = 123;
        int testStartEdgeOffset = 456;
        double testLon = 78.9;
        double testLat = 12.34;

        Node node = new Node(testId, new Coordinate(testLat, testLon), testStartEdgeOffset, 0, 0, 0);

        store.saveNode(node, 0L);

        Node readNode = store.readNode(0L);

        assertThat(readNode.getId()).as("읽어온 Node의 Id값이 일치하지 않습니다.").isEqualTo(testId);
        assertThat(readNode.getStartEdgeOffset()).as("읽어온 Node의 startEdgeOffset값이 일치하지 않습니다.").isEqualTo(testStartEdgeOffset);
        assertThat(readNode.getCoordinate().getLongitude()).as("읽어온 Node의 Longitude값이 일치하지 않습니다.").isEqualTo(testLon);
        assertThat(readNode.getCoordinate().getLatitude()).as("읽어온 Node의 Latitude값이 일치하지 않습니다.").isEqualTo(testLat);

        testFileDelete(store);
    }

    @Test
    @DisplayName("readEdge가 임의 위치에 저장된 Edge 데이터를 올바르게 반환하는지 확인")
    public void readEdgeReturnDataConfirm() throws Exception {
        Path tempDir = Files.createTempDirectory("test");
        HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString(), mock(NodeIndexProvider.class));
        
        int testId = 1;
        int testFromOffset = 10;
        int testToOffset = 20;
        double testDistance = 30.5;
        int testNextEdgeOffset = 40;

        Edge edge = new Edge(testId, testFromOffset, testToOffset, testDistance, testNextEdgeOffset);

        store.saveEdge(edge, 20L);

        Edge readEdge = store.readEdge(20L);

        assertThat(readEdge.getId()).as("Id값이 테스트 값과 일치하지 않습니다.").isEqualTo(testId);
        assertThat(readEdge.getFrom()).as("읽어온 Edge의 From값이 일치하지 않습니다.").isEqualTo(testFromOffset);
        assertThat(readEdge.getTo()).as("읽어온 Edge의 To값이 일치하지 않습니다.").isEqualTo(testToOffset);
        assertThat(readEdge.getDistance()).as("읽어온 Edge의 Distance값이 일치하지 않습니다.").isEqualTo(testDistance);
        assertThat(readEdge.getNextEdgeOffset()).as("읽어온 Edge의 NextEdgeOffset값이 일치하지 않습니다.").isEqualTo(testNextEdgeOffset);

        testFileDelete(store);
    }

    @Test
    @DisplayName("readNode가 graphRead=true일 때 임의 위치에 저장된 Node 데이터를 올바르게 반환하는지 확인")
    public void readNodeReturnDataConfirmGraphReadTrue() throws Exception {
        Object[] testInfo = testFileCreate(Files.createTempDirectory("test"));
        Path tempDir = (Path)testInfo[0];
        Node testNode = (Node)testInfo[1];
        
        HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString(), mock(NodeIndexProvider.class));
        
        Node readNode = store.readNode(20L);

        assertThat(readNode.getId()).as("읽어온 Node의 Id값이 일치하지 않습니다.").isEqualTo(testNode.getId());
        assertThat(readNode.getStartEdgeOffset()).as("읽어온 Node의 startEdgeOffset값이 일치하지 않습니다.").isEqualTo(testNode.getStartEdgeOffset());
        assertThat(readNode.getCoordinate().getLongitude()).as("읽어온 Node의 Longitude값이 일치하지 않습니다.").isEqualTo(testNode.getCoordinate().getLongitude());
        assertThat(readNode.getCoordinate().getLatitude()).as("읽어온 Node의 Latitude값이 일치하지 않습니다.").isEqualTo(testNode.getCoordinate().getLatitude());

        testFileDelete(tempDir);
    }

    @Test
    @DisplayName("readEdge가 graphRead=true일 때 임의 위치에 저장된 Edge 데이터를 올바르게 반환하는지 확인")
    public void readEdgeReturnDataConfirmGraphReadTrue() throws Exception {
        Object[] testInfo = testFileCreate(Files.createTempDirectory("test"));
        Path tempDir = (Path)testInfo[0];
        Edge testEdge = (Edge)testInfo[2];
        
        HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString(), mock(NodeIndexProvider.class));
        

        Edge readEdge = store.readEdge(20L);

        assertThat(readEdge.getFrom()).as("읽어온 Edge의 From값이 일치하지 않습니다.").isEqualTo(testEdge.getFrom());
        assertThat(readEdge.getTo()).as("읽어온 Edge의 To값이 일치하지 않습니다.").isEqualTo(testEdge.getTo());
        assertThat(readEdge.getDistance()).as("읽어온 Edge의 Distance값이 일치하지 않습니다.").isEqualTo(testEdge.getDistance());
        assertThat(readEdge.getNextEdgeOffset()).as("읽어온 Edge의 NextEdgeOffset값이 일치하지 않습니다.").isEqualTo(testEdge.getNextEdgeOffset());

        testFileDelete(tempDir);
    }

    private Object[] testFileCreate(Path tempDir) throws IOException {
        HybridDataStore store = new HybridDataStore(tempDir.toAbsolutePath().toString(), mock(NodeIndexProvider.class));
 
        int testId = 123;
        int testStartEdgeOffset = 456;
        double testLon = 78.9;
        double testLat = 12.34;

        Node node = new Node(testId, new Coordinate(testLat, testLon), testStartEdgeOffset, 0, 0, 0);
        store.saveNode(node, 20L);
        
        int testEdgeId = 0;
        int testFromOffset = 0;
        int testToOffset = 240;
        double testDistance = 100;
        int testNextEdgeOffset = 480;

        Edge edge = new Edge(testEdgeId, testFromOffset, testToOffset, testDistance, testNextEdgeOffset);
        store.saveEdge(edge, 20L);
        
        return new Object[] { tempDir, node, edge };
    }
    

    private void testFileDelete(HybridDataStore store) {
        store.getEdgeFilePath().toFile().delete();
        store.getNodeFilePath().toFile().delete();
    }

    private void testFileDelete(Path tempDir) {
        tempDir.toFile().delete();
    }


}
