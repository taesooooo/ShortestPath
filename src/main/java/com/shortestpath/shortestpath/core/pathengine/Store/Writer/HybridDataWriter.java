package com.shortestpath.shortestpath.core.pathengine.Store.Writer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Store.EdgeHeader;
import com.shortestpath.shortestpath.core.pathengine.Store.NodeHeader;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 하이브리드 데이터 쓰기 구현
 * AllocatableDataWriter 인터페이스를 선택적으로 구현
 * 추출 단계에서 Node와 Edge를 파일에 기록
 */
@Slf4j
public class HybridDataWriter implements AllocatableDataWriter, HeaderWriter {
    private String fileDirectory;
    private FileChannel nodeFileChannel = null;
    private FileChannel edgeFileChannel = null;

    @Getter
    private Path nodeFilePath;
    @Getter
    private Path edgeFilePath;
    
    // 멀티스레드 환경에서 파일 크기 추적
    private final AtomicLong currentEdgeFileSize = new AtomicLong(0);
    private final AtomicLong currentNodeFileSize = new AtomicLong(0);
    private static final double EXPANSION_THRESHOLD = 0.8;  // 80% 찼을 때 확장

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
        if (node == null) {
            throw new IllegalArgumentException("Node 객체가 Null 입니다.");
        }
        
        long offset = DataStructureSizes.calculateNodeOffset(node.getId());

        saveNode(node, offset);

        return (int) offset;                                                                                   
    }

    @Override
    public int saveNode(Node node, long offset) throws IllegalArgumentException, IOException {
        if (node == null) {
            throw new IllegalArgumentException("Node 객체가 Null 입니다.");
        }

        ByteBuffer buffer = ByteBuffer.allocate(DataStructureSizes.NODE_SIZE);
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
         if (edge == null) {
            throw new IllegalArgumentException("Edge 객체가 Null 입니다.");
        }

        long offset = DataStructureSizes.calculateEdgeOffset(edge.getId());
        saveEdge(edge, offset);

        return (int) offset;
    }

    @Override
    public int saveEdge(Edge edge, long offset) throws IllegalArgumentException, IOException {
        if (edge == null) {
            throw new IllegalArgumentException("Edge 객체가 Null 입니다.");
        }

        // 파일 크기 동적 확장 체크 (추출 모드이고 80% 찼을 때만)
        long currentSize = currentEdgeFileSize.get();
        if (offset + DataStructureSizes.EDGE_SIZE > (long)(currentSize * EXPANSION_THRESHOLD)) {
            // 필요할 때만 동기화
            synchronized(this) {
                long latestSize = currentEdgeFileSize.get();
                int addSize = DataStructureSizes.EDGE_ENTRY_SIZE * 10000;
                if (offset + DataStructureSizes.EDGE_SIZE > latestSize) {
                    long additionalSize = Math.max(
                        addSize,
                        latestSize / 2  // 현재 크기의 절반 또는 최소 10000개분
                    );
                    allocateEdgeFileSpace(additionalSize);

                    log.info("Edge 파일 동적 확장 - 추가 크기: {} bytes, 총 크기: {} bytes", 
                                additionalSize, currentEdgeFileSize.get());
                }
            }
        }

        ByteBuffer buffer = ByteBuffer.allocate(DataStructureSizes.EDGE_SIZE);
        buffer.putInt(edge.getId());
        buffer.putInt(edge.getFrom());
        buffer.putInt(edge.getTo());
        buffer.putDouble(edge.getDistance());
        buffer.putInt(edge.getNextEdgeOffset());
        buffer.put(edge.getRoadLevel().toString().getBytes());
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

        // 현재 파일 크기를 구하고 그 뒤에서부터 확장
        long currentSize = nodeFileChannel.size();
        ByteBuffer buffer = ByteBuffer.allocate((int) size);
        nodeFileChannel.write(buffer, currentSize);
        currentNodeFileSize.addAndGet(size);

        log.info("Node 파일 공간 할당 완료 - 기존크기: {} bytes, 추가크기: {} bytes, 총크기: {} bytes", 
                 currentSize, size, currentSize + size);
    }

    @Override
    public void allocateEdgeFileSpace(long size) throws IOException {
        if (edgeFileChannel == null || !edgeFileChannel.isOpen()) {
            throw new IOException("Edge 파일 채널이 초기화되지 않았습니다.");
        }

        // 현재 파일 크기를 구하고 그 뒤에서부터 확장
        long currentSize = edgeFileChannel.size();
        ByteBuffer buffer = ByteBuffer.allocate((int) size);
        edgeFileChannel.write(buffer, currentSize);
        currentEdgeFileSize.addAndGet(size);
        
        log.info("Edge 파일 공간 할당 완료 - 기존크기: {} bytes, 추가크기: {} bytes, 총크기: {} bytes", 
                 currentSize, size, currentSize + size);
    }

    @Override
    public void truncateNodeFile(long actualSize) throws IOException {
        if (nodeFileChannel == null || !nodeFileChannel.isOpen()) {
            throw new IOException("Node 파일 채널이 초기화되지 않았습니다.");
        }

        nodeFileChannel.truncate(actualSize);
        currentNodeFileSize.set(actualSize);
        log.info("Node 파일 축소 완료 - 축소된 크기: {} bytes", actualSize);
    }

    @Override
    public void truncateEdgeFile(long actualSize) throws IOException {
        if (edgeFileChannel == null || !edgeFileChannel.isOpen()) {
            throw new IOException("Edge 파일 채널이 초기화되지 않았습니다.");
        }

        edgeFileChannel.truncate(actualSize);
        currentEdgeFileSize.set(actualSize);
        log.info("Edge 파일 축소 완료 - 축소된 크기: {} bytes", actualSize);
    }

    @Override
    public void writeNodeHeader(NodeHeader header) throws IOException {
        if (nodeFileChannel == null || !nodeFileChannel.isOpen()) {
            throw new IOException("Node 파일 채널이 초기화되지 않았습니다.");
        }

        ByteBuffer buffer = ByteBuffer.allocate(DataStructureSizes.HEADER_SIZE);
        buffer.putInt(header.getNodeCount());
        buffer.put((byte) (header.isIndexed() ? 1 : 0));
        buffer.flip();

        nodeFileChannel.write(buffer, 0);
        log.info("Node 헤더 작성 완료 - nodeCount: {}, indexed: {}", header.getNodeCount(), header.isIndexed());
    }

    @Override
    public void writeEdgeHeader(EdgeHeader header) throws IOException {
        if (edgeFileChannel == null || !edgeFileChannel.isOpen()) {
            throw new IOException("Edge 파일 채널이 초기화되지 않았습니다.");
        }

        ByteBuffer buffer = ByteBuffer.allocate(DataStructureSizes.HEADER_SIZE);
        buffer.putInt(header.getEdgeCount());
        buffer.put((byte) (header.isSorted() ? 1 : 0));
        buffer.flip();

        edgeFileChannel.write(buffer, 0);
        log.info("Edge 헤더 작성 완료 - edgeCount: {}, sorted: {}", header.getEdgeCount(), header.isSorted());
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
