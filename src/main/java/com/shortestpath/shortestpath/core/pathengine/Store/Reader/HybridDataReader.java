package com.shortestpath.shortestpath.core.pathengine.Store.Reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.Store.EdgeHeader;
import com.shortestpath.shortestpath.core.pathengine.Store.NodeHeader;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 하이브리드 데이터 읽기 구현
 * MappableDataReader, IndexableDataReader 인터페이스를 선택적으로 구현
 * 초기에는 직렬 읽기, 데이터 준비 후 메모리 매핑 읽기로 전환
 */
@Slf4j
public class HybridDataReader implements MappableDataReader {
    private FileChannel nodeFileChannel = null;
    private FileChannel edgeFileChannel = null;
    private FileChannel reverseEdgeFileChannel = null;
    private MappedByteBuffer nodeMappedBuffer = null;
    private MappedByteBuffer edgeMappedBuffer = null;
    private MappedByteBuffer reverseEdgeMappedBuffer = null;
    private boolean graphRead = false;

    @Getter
    private Path nodeFilePath;
    @Getter
    private Path edgeFilePath;
    @Getter
    private Path reverseEdgeFilePath;

    private NodeViewer nodeViewer;
    private EdgeViewer edgeViewer;
    private EdgeViewer reverseEdgeViewer;


    public HybridDataReader(Path nodeFilePath, Path edgeFilePath) throws IOException {
        this.nodeFilePath = nodeFilePath;
        this.edgeFilePath = edgeFilePath;
        this.reverseEdgeFilePath = edgeFilePath.resolveSibling("reverse_edge.bin");

        this.nodeFileChannel = FileChannel.open(nodeFilePath, StandardOpenOption.READ);
        this.edgeFileChannel = FileChannel.open(edgeFilePath, StandardOpenOption.READ);

        // this.nodeViewer = new NodeViewer(this.nodeMappedBuffer);

        log.info("HybridDataReader 초기화 완료 - nodeFile: {}, edgeFile: {}", nodeFilePath, edgeFilePath);
    }

    @Override
    public EdgeHeader readEdgeHeader() throws IOException {
        if(graphRead) {
            edgeMappedBuffer.position(0);
            int edgeCount = edgeMappedBuffer.getInt();
            boolean sorted = edgeMappedBuffer.get() != 0;
            boolean taskCompleted = readBoolean(edgeMappedBuffer);

            return new EdgeHeader(edgeCount, sorted, taskCompleted);
        } 
        else {
            ByteBuffer buffer = ByteBuffer.allocate(DataStructureSizes.HEADER_SIZE);
            edgeFileChannel.read(buffer, 0);
            buffer.flip();

            int edgeCount = buffer.getInt();
            boolean sorted = buffer.get() != 0;
            boolean taskCompleted = readBoolean(buffer);

            return new EdgeHeader(edgeCount, sorted, taskCompleted);
        }
    }

    @Override
    public EdgeHeader readReverseEdgeHeader() throws IOException {
        if(graphRead && reverseEdgeMappedBuffer != null) {
            reverseEdgeMappedBuffer.position(0);
            int edgeCount = reverseEdgeMappedBuffer.getInt();
            boolean sorted = reverseEdgeMappedBuffer.get() != 0;
            boolean taskCompleted = readBoolean(reverseEdgeMappedBuffer);

            return new EdgeHeader(edgeCount, sorted, taskCompleted);
        }

        FileChannel channel = getReverseEdgeFileChannel();
        ByteBuffer buffer = ByteBuffer.allocate(DataStructureSizes.HEADER_SIZE);
        channel.read(buffer, 0);
        buffer.flip();

        int edgeCount = buffer.getInt();
        boolean sorted = buffer.get() != 0;
        boolean taskCompleted = readBoolean(buffer);

        return new EdgeHeader(edgeCount, sorted, taskCompleted);
    }

    @Override
    public NodeHeader readNodeHeader() throws IOException {
        if (graphRead) {
            nodeMappedBuffer.position(0);
            int nodeCount = nodeMappedBuffer.getInt();
            boolean indexed = nodeMappedBuffer.get() != 0;
            boolean taskCompleted = readBoolean(nodeMappedBuffer);

            return new NodeHeader(nodeCount, indexed, taskCompleted);
        }

        ByteBuffer buffer = ByteBuffer.allocate(DataStructureSizes.HEADER_SIZE);
        nodeFileChannel.read(buffer, 0);
        buffer.flip();

        int nodeCount = buffer.getInt();
        boolean indexed = buffer.get() != 0;
        boolean taskCompleted = readBoolean(buffer);

        return new NodeHeader(nodeCount, indexed, taskCompleted);
    }

    @Override
    public Node readNode(long offset) throws IOException {
        if (graphRead) {
            return readMappedNode(offset);
        }

        ByteBuffer buffer = ByteBuffer.allocate(DataStructureSizes.NODE_SIZE);
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

        return readEdge(edgeFileChannel, offset);
    }

    @Override
    public Edge readReverseEdge(long offset) throws IOException {
        if (graphRead && reverseEdgeMappedBuffer != null) {
            return readMappedEdge(reverseEdgeMappedBuffer, offset);
        }

        return readEdge(getReverseEdgeFileChannel(), offset);
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
            
            if (!Files.exists(reverseEdgeFilePath)) {
                log.debug("데이터 추출 여부 확인 - reverse edge 파일 없음: {}", reverseEdgeFilePath);
                return false;
            }

            long reverseEdgeSize = Files.size(reverseEdgeFilePath);
            if (nodeSize < DataStructureSizes.HEADER_SIZE
                    || edgeSize < DataStructureSizes.HEADER_SIZE
                    || reverseEdgeSize < DataStructureSizes.HEADER_SIZE) {
                log.debug("데이터 추출 여부 확인 - 헤더 크기 미만: nodeSize: {}, edgeSize: {}, reverseEdgeSize: {}",
                        nodeSize, edgeSize, reverseEdgeSize);
                return false;
            }

            NodeHeader nodeHeader = readNodeHeader();
            EdgeHeader edgeHeader = readEdgeHeader();
            EdgeHeader reverseEdgeHeader = readReverseEdgeHeader();
            boolean hasData = nodeHeader.isTaskCompleted()
                    && edgeHeader.isTaskCompleted()
                    && reverseEdgeHeader.isTaskCompleted()
                    && nodeSize >= DataStructureSizes.HEADER_SIZE + ((long) nodeHeader.getNodeCount() * DataStructureSizes.NODE_SIZE)
                    && edgeSize >= DataStructureSizes.HEADER_SIZE + ((long) edgeHeader.getEdgeCount() * DataStructureSizes.EDGE_SIZE)
                    && reverseEdgeSize >= DataStructureSizes.HEADER_SIZE + ((long) reverseEdgeHeader.getEdgeCount() * DataStructureSizes.EDGE_SIZE);
            log.debug("데이터 추출 여부 확인 - nodeSize: {}, edgeSize: {}, reverseEdgeSize: {}, nodeTask: {}, edgeTask: {}, reverseEdgeTask: {}, hasData: {}",
                      nodeSize, edgeSize, reverseEdgeSize, nodeHeader.isTaskCompleted(), edgeHeader.isTaskCompleted(),
                      reverseEdgeHeader.isTaskCompleted(), hasData);
            
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

        if (Files.exists(reverseEdgeFilePath)) {
            FileChannel channel = getReverseEdgeFileChannel();
            long reverseEdgeFileSize = channel.size();
            if (reverseEdgeFileSize > 0) {
                this.reverseEdgeMappedBuffer = channel.map(MapMode.READ_ONLY, 0, reverseEdgeFileSize);
            }
        }

        this.graphRead = true;
        log.info("메모리 매핑 모드로 전환 완료 - nodeSize: {}, edgeSize: {}", nodeFileSize, edgeFileSize);
    }

    private synchronized Node readMappedNode(long offset) throws IOException {
        if (nodeMappedBuffer == null) {
            throw new IOException("Node 메모리 매핑 버퍼가 초기화되지 않았습니다.");
        }

        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IOException("유효하지 않은 노드 오프셋: " + offset + " (int 범위: 0 ~ " + Integer.MAX_VALUE + ")");
        }
        if (offset >= nodeMappedBuffer.capacity()) {
            throw new IOException("노드 오프셋이 버퍼 크기를 초과함: offset=" + offset + ", capacity=" + nodeMappedBuffer.capacity());
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
        return readMappedEdge(edgeMappedBuffer, offset);
    }

    private Edge readMappedEdge(MappedByteBuffer mappedBuffer, long offset) throws IOException {
        if (mappedBuffer == null) {
            throw new IOException("Edge 메모리 매핑 버퍼가 초기화되지 않았습니다.");
        }

        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IOException("유효하지 않은 엣지 오프셋: " + offset + " (int 범위: 0 ~ " + Integer.MAX_VALUE + ")");
        }
        if (offset >= mappedBuffer.capacity()) {
            throw new IOException("엣지 오프셋이 버퍼 크기를 초과함: offset=" + offset + ", capacity=" + mappedBuffer.capacity());
        }

        synchronized (mappedBuffer) {
            mappedBuffer.position((int) offset);
            byte[] roadLevelBytes = new byte[2];
    
            int id = mappedBuffer.getInt();
            int from = mappedBuffer.getInt();
            int to = mappedBuffer.getInt();
            double distance = mappedBuffer.getDouble();
            int nextEdgeOffset = mappedBuffer.getInt();
            int speed = mappedBuffer.getInt();
            mappedBuffer.get(roadLevelBytes);
            String roadLevel = new String(roadLevelBytes, StandardCharsets.US_ASCII);
    
            Edge edge = new Edge(id, from, to, distance, nextEdgeOffset, speed, RoadLevel.fromString(roadLevel));
            return edge;
        }
    }

    private Edge readEdge(FileChannel channel, long offset) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(DataStructureSizes.EDGE_SIZE);
        byte[] roadLevelBytes = new byte[2];
        channel.read(buffer, offset);
        buffer.flip();

        int id = buffer.getInt();
        int from = buffer.getInt();
        int to = buffer.getInt();
        double distance = buffer.getDouble();
        int nextEdgeOffset = buffer.getInt();
        int speed = buffer.getInt();
        buffer.get(roadLevelBytes);
        String roadLevel = new String(roadLevelBytes, StandardCharsets.US_ASCII);

        return new Edge(id, from, to, distance, nextEdgeOffset, speed, RoadLevel.fromString(roadLevel));
    }

    public NodeViewer getNodeViewer() throws IllegalStateException {
        if(nodeViewer == null) {
            if (nodeMappedBuffer == null) {
                throw new IllegalStateException("Node 메모리 매핑 버퍼가 초기화되지 않았습니다.");
            }
            nodeViewer = new NodeViewer(nodeMappedBuffer);
        }
        return nodeViewer;
    }

    public EdgeViewer getEdgeViewer() throws IllegalStateException {
        if(edgeViewer == null) {
            if (edgeMappedBuffer == null) {
                throw new IllegalStateException("Edge 메모리 매핑 버퍼가 초기화되지 않았습니다.");
            }
            edgeViewer = new EdgeViewer(edgeMappedBuffer);
        }
        return edgeViewer;
    }

    public EdgeViewer getReverseEdgeViewer() throws IllegalStateException {
        if(reverseEdgeViewer == null) {
            if (reverseEdgeMappedBuffer == null) {
                throw new IllegalStateException("Reverse Edge 메모리 매핑 버퍼가 초기화되지 않았습니다.");
            }
            reverseEdgeViewer = new EdgeViewer(reverseEdgeMappedBuffer);
        }
        return reverseEdgeViewer;
    }

    private FileChannel getReverseEdgeFileChannel() throws IOException {
        if (reverseEdgeFileChannel == null || !reverseEdgeFileChannel.isOpen()) {
            if (!Files.exists(reverseEdgeFilePath)) {
                throw new IOException("Reverse Edge 파일이 존재하지 않습니다: " + reverseEdgeFilePath);
            }
            reverseEdgeFileChannel = FileChannel.open(reverseEdgeFilePath, StandardOpenOption.READ);
        }

        return reverseEdgeFileChannel;
    }

    private boolean readBoolean(ByteBuffer buffer) {
        return buffer.hasRemaining() && buffer.get() != 0;
    }

    @Override
    public void close() throws IOException {
        if (nodeFileChannel != null && nodeFileChannel.isOpen()) {
            nodeFileChannel.close();
        }
        if (edgeFileChannel != null && edgeFileChannel.isOpen()) {
            edgeFileChannel.close();
        }
        if (reverseEdgeFileChannel != null && reverseEdgeFileChannel.isOpen()) {
            reverseEdgeFileChannel.close();
        }
        log.info("HybridDataReader 리소스 해제 완료");
    }
}
