package com.shortestpath.shortestpath.core.pathengine.Store;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 하이브리드 데이터 읽기 구현
 * MappableDataReader, IndexableDataReader 인터페이스를 선택적으로 구현
 * 초기에는 직렬 읽기, 데이터 준비 후 메모리 매핑 읽기로 전환
 */
@Slf4j
public class HybridDataReader implements MappableDataReader, IndexableDataReader {
    private FileChannel nodeFileChannel = null;
    private FileChannel edgeFileChannel = null;
    private MappedByteBuffer nodeMappedBuffer = null;
    private MappedByteBuffer edgeMappedBuffer = null;
    private boolean graphRead = false;

    @Getter
    private Path nodeFilePath;
    @Getter
    private Path edgeFilePath;

    // 좌표 -> Node 오프셋 맵핑
    private HashMap<String, Integer> coordinateNodeIndexMap;

    public HybridDataReader(Path nodeFilePath, Path edgeFilePath) throws IOException {
        this.nodeFilePath = nodeFilePath;
        this.edgeFilePath = edgeFilePath;
        this.coordinateNodeIndexMap = new HashMap<>();

        this.nodeFileChannel = FileChannel.open(nodeFilePath, StandardOpenOption.READ);
        this.edgeFileChannel = FileChannel.open(edgeFilePath, StandardOpenOption.READ);

        log.info("HybridDataReader 초기화 완료 - nodeFile: {}, edgeFile: {}", nodeFilePath, edgeFilePath);
    }

    @Override
    public Node readNode(long offset) throws IOException {
        if (graphRead) {
            return readMappedNode(offset);
        }

        ByteBuffer buffer = ByteBuffer.allocate(24);
        nodeFileChannel.read(buffer, offset);
        buffer.flip();

        int id = buffer.getInt();
        int startEdgeOffset = buffer.getInt();
        double longitude = buffer.getDouble();
        double latitude = buffer.getDouble();

        Node node = new Node(id, new Coordinate(latitude, longitude), startEdgeOffset, Double.MAX_VALUE, 0, 0);
        return node;
    }

    @Override
    public Edge readEdge(long offset) throws IOException {
        if (graphRead) {
            return readMappedEdge(offset);
        }

        ByteBuffer buffer = ByteBuffer.allocate(24);
        edgeFileChannel.read(buffer, offset);
        buffer.flip();

        int id = buffer.getInt();
        int from = buffer.getInt();
        int to = buffer.getInt();
        double distance = buffer.getDouble();
        int nextEdgeOffset = buffer.getInt();

        Edge edge = new Edge(id, from, to, distance, nextEdgeOffset);
        return edge;
    }

    @Override
    public int getNodeOffset(Coordinate coordinate) {
        String coordinateKey = coordinate.getLatitude() + "," + coordinate.getLongitude();
        return coordinateNodeIndexMap.getOrDefault(coordinateKey, -1);
    }

    @Override
    public boolean hasExtractedData() {
        try {
            // 파일이 존재하지 않으면 데이터 없음
            if (!Files.exists(nodeFilePath) || !Files.exists(edgeFilePath)) {
                return false;
            }
            
            long nodeSize = Files.size(nodeFilePath);
            long edgeSize = Files.size(edgeFilePath);
            
            // 최소 1개 노드/엣지(24바이트)보다 커야 실제 데이터 있음
            boolean hasData = nodeSize >= 24 && edgeSize >= 24;
            log.debug("데이터 추출 여부 확인 - nodeSize: {}, edgeSize: {}, hasData: {}", 
                      nodeSize, edgeSize, hasData);
            
            return hasData;
        } catch (IOException e) {
            log.error("파일 크기 확인 실패", e);
            return false;
        }
    }

    @Override
    public void switchToMappingMode() throws IOException {
        long nodeFileSize = nodeFileChannel.size();
        long edgeFileSize = edgeFileChannel.size();

        if (nodeFileSize > 0) {
            this.nodeMappedBuffer = nodeFileChannel.map(MapMode.READ_ONLY, 0, nodeFileSize);
        }

        if (edgeFileSize > 0) {
            this.edgeMappedBuffer = edgeFileChannel.map(MapMode.READ_ONLY, 0, edgeFileSize);
        }

        this.graphRead = true;
        log.info("메모리 매핑 모드로 전환 완료 - nodeSize: {}, edgeSize: {}", nodeFileSize, edgeFileSize);
    }

    private Node readMappedNode(long offset) throws IOException {
        if (nodeMappedBuffer == null) {
            throw new IOException("Node 메모리 매핑 버퍼가 초기화되지 않았습니다.");
        }

        nodeMappedBuffer.position((int) offset);

        int id = nodeMappedBuffer.getInt();
        int startEdgeOffset = nodeMappedBuffer.getInt();
        double longitude = nodeMappedBuffer.getDouble();
        double latitude = nodeMappedBuffer.getDouble();

        Node node = new Node(id, new Coordinate(latitude, longitude), startEdgeOffset, Double.MAX_VALUE, 0, 0);
        return node;
    }

    private Edge readMappedEdge(long offset) throws IOException {
        if (edgeMappedBuffer == null) {
            throw new IOException("Edge 메모리 매핑 버퍼가 초기화되지 않았습니다.");
        }

        edgeMappedBuffer.position((int) offset);

        int id = edgeMappedBuffer.getInt();
        int from = edgeMappedBuffer.getInt();
        int to = edgeMappedBuffer.getInt();
        double distance = edgeMappedBuffer.getDouble();
        int nextEdgeOffset = edgeMappedBuffer.getInt();

        Edge edge = new Edge(id, from, to, distance, nextEdgeOffset);
        return edge;
    }

    @Override
    public void close() throws IOException {
        if (nodeFileChannel != null && nodeFileChannel.isOpen()) {
            nodeFileChannel.close();
        }
        if (edgeFileChannel != null && edgeFileChannel.isOpen()) {
            edgeFileChannel.close();
        }
        log.info("HybridDataReader 리소스 해제 완료");
    }

    /**
     * 좌표 인덱스 추가 (Writer에서 호출)
     */
    public void addCoordinateIndex(Coordinate coordinate, int offset) {
        String coordinateKey = coordinate.getLatitude() + "," + coordinate.getLongitude();
        coordinateNodeIndexMap.put(coordinateKey, offset);
    }
}
