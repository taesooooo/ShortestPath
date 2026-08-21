package com.shortestpath.shortestpath.core.pathengine.Extractor.Sort;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;

/**
 * 청크 리더: 임시 파일에서 엣지를 순차적으로 읽음
 * 
 * K-way 병합 단계에서 각 정렬된 청크 파일을 읽기 위해 사용
 */
public class ChunkReader {
    private RandomAccessFile raf;
    private FileChannel channel;
    private long position = 0;
    private long fileSize;
    
    public ChunkReader(File file) throws IOException {
        this.raf = new RandomAccessFile(file, "r");
        this.channel = raf.getChannel();
        this.fileSize = channel.size();
    }
    
    /**
     * 다음 엣지를 읽음
     * @return 읽은 엣지, 파일 끝에 도달하면 null
     */
    public Edge readNext() throws IOException {
        if (position >= fileSize) {
            return null;
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(DataStructureSizes.EDGE_SIZE);
        channel.read(buffer, position);
        buffer.flip();
        
        position += DataStructureSizes.EDGE_SIZE;
        
        return readEdgeFromBuffer(buffer);
    }
    
    private Edge readEdgeFromBuffer(ByteBuffer buffer) {
        byte[] roadLevelBytes = new byte[2];
        
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
    
    /**
     * 리소스 해제
     */
    public void close() throws IOException {
        if (channel != null) channel.close();
        if (raf != null) raf.close();
    }
}
