package com.shortestpath.shortestpath.core.pathengine.Store;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HybridDataStore implements MappableDataStore {
    private String fileDirectory;
    private FileChannel nodeFileChannel = null;
    private FileChannel edgeFileChannel = null;
    private MappedByteBuffer nodeMappedBuffer = null;
    private MappedByteBuffer edgeMappedBuffer = null;
    private boolean graphRead = false;

    private NodeProvider nodeIndexProvider;

    @Getter
    private Path nodeFilePath;
    @Getter
    private Path edgeFilePath;

    public HybridDataStore(String fileDirectory, NodeProvider nodeIndexProvider) throws IOException {
        this.fileDirectory = fileDirectory;
        this.nodeFilePath = new File(fileDirectory).toPath().resolve("node.bin");
        this.edgeFilePath = new File(fileDirectory).toPath().resolve("edge.bin");

        this.nodeFileChannel = FileChannel.open(nodeFilePath, StandardOpenOption.WRITE, StandardOpenOption.READ,
                StandardOpenOption.CREATE);
        this.edgeFileChannel = FileChannel.open(edgeFilePath, StandardOpenOption.WRITE, StandardOpenOption.READ,
                StandardOpenOption.CREATE);

        if (this.hasExtractedData()) {
            log.info("경로탐색에 필요한 파일이 존재합니다. 맵핑 모드로 전환했습니다.");
            switchMapMode();
        } else {
            log.info("경로탐색에 필요한 파일이 존재하지 않아 파일을 생성했습니다.");
        }

        if (nodeIndexProvider == null) {
            throw new IllegalArgumentException("NodeIndexProvider 객체는 null 일 수 없습니다.");
        }
        this.nodeIndexProvider = nodeIndexProvider;

        log.info("FileDirectory = {}", this.fileDirectory);
    }

    public String getFileDirectory() {
        return fileDirectory;
    }

    @Override
    public int saveNode(Node node) throws IOException {
        long writeOffset = nodeFileChannel.position();
        saveNode(node, writeOffset);
        nodeFileChannel.position(writeOffset + DataStructureSizes.NODE_SIZE);

        return (int) writeOffset;
    }

    @Override
    public int saveNode(Node node, long offset) throws IllegalArgumentException, IOException {
        if (node == null) {
            throw new IllegalArgumentException("Node 객체가 Null 입니다.");
        }

        // 버퍼에 미리 크기 할당
        // Node의 id(int, 4바이트), startEdgeOffset(int, 4바이트), x(double, 8바이트), y(double,
        // 8바이트) 저장
        // 총 24바이트
        ByteBuffer buffer = ByteBuffer.allocate(24);
        buffer.putInt(node.getId());
        buffer.putInt(node.getStartEdgeOffset());
        buffer.putDouble(node.getCoordinate().getLongitude());
        buffer.putDouble(node.getCoordinate().getLatitude());
        buffer.flip();

        nodeFileChannel.write(buffer, offset);

        return (int) offset;
    }

    @Override
    public int saveEdge(Edge edge) throws IOException {
        long writeOffset = edgeFileChannel.position();
        saveEdge(edge, writeOffset);
        edgeFileChannel.position(writeOffset + DataStructureSizes.EDGE_SIZE);

        return (int) writeOffset;
    }

    @Override
    public int saveEdge(Edge edge, long offset) throws IllegalArgumentException, IOException {
        if (edge == null) {
            throw new IllegalArgumentException("Node 객체가 Null 입니다.");
        }

        // Edge id(int, 4바이트), from(int, 4바이트), to(int, 4바이트), distance(double, 8바이트),
        // nextEdgeOffset(int, 4바이트) 저장
        // 총 24바이트
        ByteBuffer buffer = ByteBuffer.allocate(24);
        buffer.putInt(edge.getId());
        buffer.putInt(edge.getFrom());
        buffer.putInt(edge.getTo());
        buffer.putDouble(edge.getDistance());
        buffer.putInt(edge.getNextEdgeOffset());
        buffer.flip();

        edgeFileChannel.write(buffer, offset);

        return (int) offset;
    }

    @Override
    public int overwriteEdge(Edge edge, long offset) throws IOException {
        int writeOffset = saveEdge(edge, offset);

        return (int) writeOffset;
    }

    @Override
    public int overwriteNode(Node node, long offset) throws IOException {
        int writeOffset = saveNode(node, offset);

        return (int) writeOffset;
    }

    @Override
    public Node readNode(long offset) throws IOException {
        if (graphRead) {
            return readMappedNode(offset);
        }

        ByteBuffer buffer = ByteBuffer.allocate(24);

        nodeFileChannel.position(offset);
        nodeFileChannel.read(buffer, offset);

        buffer.flip();

        int id = buffer.getInt();
        int startEdgeOffset = buffer.getInt();
        double longitude = buffer.getDouble();
        double latitude = buffer.getDouble();

        return new Node(id, new Coordinate(latitude, longitude), startEdgeOffset, Double.MAX_VALUE, 0, 0);
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

        return new Edge(id, from, to, distance, nextEdgeOffset);
    }

    private Node readMappedNode(long offset) throws IOException {
        nodeMappedBuffer.position((int) offset);

        int id = nodeMappedBuffer.getInt();
        int startEdgeOffset = nodeMappedBuffer.getInt();
        double longitude = nodeMappedBuffer.getDouble();
        double latitude = nodeMappedBuffer.getDouble();

        return new Node(id, new Coordinate(latitude, longitude), startEdgeOffset, Double.MAX_VALUE, 0, 0);
    }

    private Edge readMappedEdge(long offset) throws IOException {
        edgeMappedBuffer.position((int) offset);

        int id = edgeMappedBuffer.getInt();
        int from = edgeMappedBuffer.getInt();
        int to = edgeMappedBuffer.getInt();
        double distance = edgeMappedBuffer.getDouble();
        int nextEdgeOffset = edgeMappedBuffer.getInt();

        return new Edge(id, from, to, distance, nextEdgeOffset);
    }

    @Override
    public void close() throws IOException {
        if (nodeFileChannel != null && nodeFileChannel.isOpen()) {
            nodeFileChannel.close();
        }
        if (edgeFileChannel != null && edgeFileChannel.isOpen()) {
            edgeFileChannel.close();
        }
    }

    public boolean hasExtractedData() {
        File nodeFile = new File(this.nodeFilePath.toString());
        File edgeFile = new File(this.edgeFilePath.toString());
        File indexFile = new File(this.nodeFilePath.toString());

        return nodeFile.exists() && edgeFile.exists() && indexFile.exists() &&
                nodeFile.length() > 0 && edgeFile.length() > 0 && indexFile.length() > 0;
    }

    private void switchMapMode() throws IOException {
        this.nodeMappedBuffer = nodeFileChannel.map(MapMode.READ_WRITE, 0, nodeFilePath.toFile().length());
        this.edgeMappedBuffer = edgeFileChannel.map(MapMode.READ_WRITE, 0, edgeFilePath.toFile().length());
        this.graphRead = true;

        log.info("Node Map Buffer Size = {}", nodeMappedBuffer.capacity());
        log.info("Edge Map Buffer Size = {}", edgeMappedBuffer.capacity());

    }

    @Override
    public void saveNodeIndex(List<IndexInfo> indexList) throws IOException {
        this.nodeIndexProvider.insertNodeIndex(indexList);
    }

    @Override
    public int getNodeOffset(Coordinate coordinate) {
        return nodeIndexProvider.getNodeIndex(coordinate);
    }

    @Override
    public void allocateNodeFileSpace(long size) throws IOException {
        this.nodeFileChannel.truncate(size);
    }

    @Override
    public void allocateEdgeFileSpace(long size) throws IOException {
        this.edgeFileChannel.truncate(size);
    }

    @Override
    public void switchToMappingMode() throws IOException {
        switchMapMode();
        log.info("파일을 맵핑 모드로 전환했습니다.");
    }
}
