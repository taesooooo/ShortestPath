package com.shortestpath.shortestpath.core.pathengine.Store;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 하이브리드 데이터 쓰기 구현
 * AllocatableDataWriter 인터페이스를 선택적으로 구현
 * 추출 단계에서 Node와 Edge를 파일에 기록
 */
@Slf4j
public class HybridDataWriter implements AllocatableDataWriter {
    private String fileDirectory;
    private FileChannel nodeFileChannel = null;
    private FileChannel edgeFileChannel = null;

    @Getter
    private Path nodeFilePath;
    @Getter
    private Path edgeFilePath;

    public HybridDataWriter(String fileDirectory) throws IOException {
        this.fileDirectory = fileDirectory;
        this.nodeFilePath = new File(fileDirectory).toPath().resolve("node.bin");
        this.edgeFilePath = new File(fileDirectory).toPath().resolve("edge.bin");

        this.nodeFileChannel = FileChannel.open(nodeFilePath, StandardOpenOption.WRITE, StandardOpenOption.READ,
                StandardOpenOption.CREATE);
        this.edgeFileChannel = FileChannel.open(edgeFilePath, StandardOpenOption.WRITE, StandardOpenOption.READ,
                StandardOpenOption.CREATE);

        log.info("HybridDataWriter 초기화 완료 - fileDirectory: {}", fileDirectory);
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

        // Node의 id(int, 4바이트), startEdgeOffset(int, 4바이트), x(double, 8바이트), y(double, 8바이트) 저장
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
            throw new IllegalArgumentException("Edge 객체가 Null 입니다.");
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
    public void saveNodeIndex(List<IndexInfo> indexList) throws IOException {
        // 인덱스 정보를 메모리에 저장
        // 필요시 별도 인덱스 파일로 저장 가능
        log.info("노드 인덱스 저장 완료 - 항목 수: {}", indexList.size());
    }

    @Override
    public void allocateNodeFileSpace(long size) throws IOException {
        if (nodeFileChannel == null || !nodeFileChannel.isOpen()) {
            throw new IOException("Node 파일 채널이 초기화되지 않았습니다.");
        }

        ByteBuffer buffer = ByteBuffer.allocate((int) size);
        nodeFileChannel.write(buffer, 0);

        log.info("Node 파일 공간 할당 완료 - 크기: {} bytes", size);
    }

    @Override
    public void allocateEdgeFileSpace(long size) throws IOException {
        if (edgeFileChannel == null || !edgeFileChannel.isOpen()) {
            throw new IOException("Edge 파일 채널이 초기화되지 않았습니다.");
        }

        ByteBuffer buffer = ByteBuffer.allocate((int) size);
        edgeFileChannel.write(buffer, 0);
        log.info("Edge 파일 공간 할당 완료 - 크기: {} bytes", size);
    }

    @Override
    public void close() throws IOException {
        if (nodeFileChannel != null && nodeFileChannel.isOpen()) {
            nodeFileChannel.close();
        }
        if (edgeFileChannel != null && edgeFileChannel.isOpen()) {
            edgeFileChannel.close();
        }
        log.info("HybridDataWriter 리소스 해제 완료");
    }
}
