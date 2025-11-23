package com.shortestpath.shortestpath.core.pathengine.Store;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileDataStore implements DataStore {
    // Node의 id(int, 4바이트), startEdgeOffset(int, 4바이트), x(double, 8바이트), y(double, 8바이트)
    private final int nodeByteSize = 4 + 4 + 8 + 8;
    // Edge id(int, 4바이트), from(int, 4바이트), to(int, 4바이트), distance(double, 8바이트), nextEdgeOffset(int, 4바이트)
    private final int edgeByteSize = 4 + 4 + 4 + 8 + 4;

    private String fileDirectory;
    private int totalNodeCount;
    private int totalEdgeCount;
    private FileChannel nodeIndexFileChannel = null;
    private FileChannel nodeFileChannel = null;
    private FileChannel edgeFileChannel = null;
    private MappedByteBuffer nodeMappedBuffer = null;
    private MappedByteBuffer edgeMappedBuffer = null;
    private boolean graphRead = false;
    private STRtree rtree;

    // @Getter
    private HashMap<Coordinate, Integer> nodeOffsetIndex;

    @Getter
    private Path nodeFilePath;
    @Getter
    private Path edgeFilePath;
    @Getter
    private Path indexFilePath;

    public FileDataStore(String fileDirectory) throws IOException {
        this.fileDirectory = fileDirectory;
        this.nodeFilePath = new File(fileDirectory).toPath().resolve("node.bin");
        this.edgeFilePath = new File(fileDirectory).toPath().resolve("edge.bin");
        this.indexFilePath = new File(fileDirectory).toPath().resolve("nodeindex.bin");
        
        this.nodeIndexFileChannel = FileChannel.open(indexFilePath, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE);
        this.nodeFileChannel = FileChannel.open(nodeFilePath, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE);
        this.edgeFileChannel = FileChannel.open(edgeFilePath, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE);
        
        if (this.hasExtractedData()) {
            this.nodeMappedBuffer = nodeFileChannel.map(MapMode.READ_WRITE, 0, nodeFilePath.toFile().length());
            this.edgeMappedBuffer = edgeFileChannel.map(MapMode.READ_WRITE, 0, edgeFilePath.toFile().length());
            this.nodeOffsetIndex = loadNodeOffsetIndex();
            this.graphRead = true;
            
            log.info("경로탐색에 필요한 파일이 존재합니다.");

            log.info("Node Map Buffer Size = {}", nodeMappedBuffer.capacity());
            log.info("Edge Map Buffer Size = {}", edgeMappedBuffer.capacity());
        }
        else {
            // if (!nodeFilePath.toFile().exists() || !edgeFilePath.toFile().exists() || !indexFilePath.toFile().exists()) {
            //     throw new IOException("필요한 파일이 없습니다");
            // }
            log.info("경로탐색에 필요한 파일이 존재하지 않아 파일을 생성했습니다.");
        }

        
        log.info("FileDirectory = {}", this.fileDirectory);
    }
    
    // public STRtree getRtree() {
    //     return rtree;
    // }

    // public void setRtree(STRtree rtree) {
    //     this.rtree = rtree;
    // }

    public int getNodeByteSize() {
        return nodeByteSize;
    }

    public int getEdgeByteSize() {
        return edgeByteSize;
    }

    @Override
    public int saveNode(Node node) throws IOException {
        return saveNode(node, nodeFileChannel.position());
    }

    @Override
    public int saveNode(Node node, long offset) throws IllegalArgumentException, IOException {
        if(node == null) {
            throw new IllegalArgumentException("Node 객체가 Null 입니다.");
        }

        // 버퍼에 미리 크기 할당
        // Node의 id(int, 4바이트), startEdgeOffset(int, 4바이트), x(double, 8바이트), y(double, 8바이트) 저장
        // 총 24바이트
        ByteBuffer buffer = ByteBuffer.allocate(24);
        buffer.putInt(node.getId());
        buffer.putInt(node.getStartEdgeOffset());
        buffer.putDouble(node.getCoordinate().getLongitude());
        buffer.putDouble(node.getCoordinate().getLatitude());
        buffer.flip();

        long writeOffset = nodeFileChannel.position();
        nodeFileChannel.position(offset);
        nodeFileChannel.write(buffer);
    
        // nodeFileChannel.force(true);
        // log.info("Extract Node = Node ID - {}, Offset - {}", node.getId(), fileChannel.position() - written);

        return (int)writeOffset;
    }

    @Override
    public int saveEdge(Edge edge) throws IOException {
        return saveEdge(edge, edgeFileChannel.position());
    }

    @Override
    public int saveEdge(Edge edge, long offset) throws IllegalArgumentException, IOException {
        if(edge == null) {
            throw new IllegalArgumentException("Node 객체가 Null 입니다.");
        }

        // Edge id(int, 4바이트), from(int, 4바이트), to(int, 4바이트), distance(double, 8바이트), nextEdgeOffset(int, 4바이트) 저장
        // 총 24바이트
        ByteBuffer buffer = ByteBuffer.allocate(24);
        buffer.putInt(edge.getId());
        buffer.putInt(edge.getFrom());
        buffer.putInt(edge.getTo());
        buffer.putDouble(edge.getDistance());
        buffer.putInt(edge.getNextEdgeOffset());
        buffer.flip();
        
        long writeOffset = edgeFileChannel.position();
        edgeFileChannel.position(offset);
        edgeFileChannel.write(buffer);

        // edgeFileChannel.force(true);

        // log.info("Extract Edge = Offset - {}", edgeFileChannel.position() - written);

        return (int)writeOffset;
    }
    
    @Override
    public int overwriteEdge(Edge edge, long offset) throws IOException {
        int previousPosition = (int)edgeFileChannel.position();
        // edgeFileChannel.position(offset);
        int writeOffset = saveEdge(edge, offset);
        edgeFileChannel.position(previousPosition);

        return (int)writeOffset;
    }

    @Override
    public int overwriteNode(Node node, long offset) throws IOException {
        int previousPosition = (int)nodeFileChannel.position();
        // nodeFileChannel.position(offset);
        int writeOffset = saveNode(node, offset);
        nodeFileChannel.position(previousPosition);

        return writeOffset;
    }

    @Override
    public Node readNode(long offset) throws IOException {
        log.debug("오프셋 - " + offset);

        if(graphRead) {
            return readMappedNode(offset);
        }

        ByteBuffer buffer = ByteBuffer.allocate(24);
        
        long previousPosition = nodeFileChannel.position();
        nodeFileChannel.position(offset);
        nodeFileChannel.read(buffer);
        
        buffer.flip();
        
        int id = buffer.getInt();
        int startEdgeOffset = buffer.getInt();
        double longitude = buffer.getDouble();
        double latitude = buffer.getDouble();

        nodeFileChannel.position(previousPosition);

        return new Node(id, new Coordinate(latitude, longitude), startEdgeOffset, Double.MAX_VALUE, 0,0);
    }

    @Override
	public Edge readEdge(long offset) throws IOException {
        log.debug("오프셋 - " + offset);

        if(graphRead) {
            return readMappedEdge(offset);
        }
        
		ByteBuffer buffer = ByteBuffer.allocate(24);
        
        long previousPosition = edgeFileChannel.position();
        edgeFileChannel.position(offset);
        edgeFileChannel.read(buffer);
        
        buffer.flip();
        
        int id = buffer.getInt();
        int from = buffer.getInt();
        int to = buffer.getInt();
        double distance = buffer.getDouble();
        int nextEdgeOffset = buffer.getInt();

        edgeFileChannel.position(previousPosition);

        return new Edge(id, from, to, distance, nextEdgeOffset);
	}


    private Node readMappedNode(long offset) throws IOException {
        // long previousPosition = nodeMappedBuffer.position();
        nodeMappedBuffer.position((int)offset);

        int id = nodeMappedBuffer.getInt();
        int startEdgeOffset = nodeMappedBuffer.getInt();
        double longitude = nodeMappedBuffer.getDouble();
        double latitude = nodeMappedBuffer.getDouble();

        // nodeMappedBuffer.position((int)previousPosition);

        return new Node(id, new Coordinate(latitude, longitude), startEdgeOffset, Double.MAX_VALUE, 0,0);
    }

    private Edge readMappedEdge(long offset) throws IOException {
        // long previousPosition = edgeMappedBuffer.position();
        edgeMappedBuffer.position((int)offset);

        int id = edgeMappedBuffer.getInt();
        int from = edgeMappedBuffer.getInt();
        int to = edgeMappedBuffer.getInt();
        double distance = edgeMappedBuffer.getDouble();
        int nextEdgeOffset = edgeMappedBuffer.getInt();

        // edgeMappedBuffer.position((int)previousPosition);

        return new Edge(id, from, to, distance, nextEdgeOffset);
    }

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

    @Override
    public void saveNodeIndex(HashMap<Coordinate, IndexInfo> indexMap) throws IOException {
        indexFilePath.toFile().createNewFile();
        for(Coordinate coord : indexMap.keySet()) {
            ByteBuffer buffer = ByteBuffer.allocate(8 + 8 + 4);
            double x = coord.getLongitude();
            double y = coord.getLatitude();
            int nodeOffset = indexMap.get(coord).getNodeIndex();

            buffer.putDouble(x);
            buffer.putDouble(y);
            buffer.putInt(nodeOffset);
            buffer.flip();

            nodeIndexFileChannel.write(buffer);
        }
    }

    @Override
    public HashMap<Coordinate, Integer> loadNodeOffsetIndex() throws IOException {
        HashMap<Coordinate, Integer> nodeIndex = new HashMap<>();
        ByteBuffer buffer = ByteBuffer.allocate(20);
        int read = 0;
        while(read != -1) {
            buffer.clear();
            read = nodeIndexFileChannel.read(buffer);

            if(read == buffer.capacity()) {
                buffer.flip();
                double x = buffer.getDouble();
                double y = buffer.getDouble();
                int nodeOffset = buffer.getInt();
                Coordinate coord = new Coordinate(y, x);
                nodeIndex.put(coord, nodeOffset);
            }

            read = nodeIndexFileChannel.read(buffer);
        }

        return nodeIndex;
    }

    @Override
    public int getNodeOffset(Coordinate coordinate) {
        return nodeOffsetIndex.get(coordinate);
    }

    @Override
    public Object getGeometryIndex() {
        return rtree;
    }

    public boolean saveIndex(STRtree rtree) {
        
        return true;
    }
    
    public STRtree loadIndex() {
        
        return new STRtree();
    }
}
