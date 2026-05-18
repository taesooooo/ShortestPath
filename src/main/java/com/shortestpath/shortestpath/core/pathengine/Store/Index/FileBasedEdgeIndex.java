package com.shortestpath.shortestpath.core.pathengine.Store.Index;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import lombok.extern.slf4j.Slf4j;

/**
 * 파일 기반 Edge 인덱스 구현
 * 바이너리 형식으로 Node ID와 연결된 엣지 인덱스 정보를 저장
 * HashMap을 사용하지 않고 파일에서 직접 읽기/쓰기
 
 * 쓰기 모드: FileChannel 사용 (append)
 * 읽기 모드: MappedByteBuffer 사용 (빠른 조회)
 */
@Slf4j
public class FileBasedEdgeIndex implements MappableEdgeIndex, Viewable {
    private final Path indexFilePath;
    private FileChannel channel;  // 읽기/쓰기용 채널
    private MappedByteBuffer mappedBuffer;
    private boolean mappingMode;
    private EdgeIndexHedaer header;
    private int entryCount;  // 현재 저장된 엔트리 수
    
    public FileBasedEdgeIndex(String fileDirectory) throws IOException {
        this(Path.of(fileDirectory, "edge_index.bin"));
    }
    
    public FileBasedEdgeIndex(Path indexFilePath) throws IOException {
        this.indexFilePath = indexFilePath;
        this.mappingMode = false;
        this.channel = FileChannel.open(indexFilePath, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        this.mappedBuffer = null;
        this.entryCount = 0;

        if (channel.size() >= DataStructureSizes.EDGE_INDEX_HEADER_SIZE) {
            load();
            this.entryCount = header.getEntryCount();
        } else {
            writeHeader(0, false);
            header = new EdgeIndexHedaer(0, false);
        }

        log.info("FileBasedEdgeIndex 초기화 - 파일: {}", indexFilePath);
    }
    
    @Override
    public void put(EdgeIndexEntry entry) throws IOException {
        if(mappingMode) {
            throw new IOException("매핑 모드에서는 쓰기가 불가능합니다.");
        }
        
        // 엔트리 추가
        ByteBuffer entryBuffer = ByteBuffer.allocate(DataStructureSizes.EDGE_INDEX_SIZE);
        entryBuffer.putInt(entry.getNodeId());
        entryBuffer.put(entry.getLevel0EdgeIndex().getLevel().toString().getBytes());
        entryBuffer.putLong(entry.getLevel0EdgeIndex().getStartOffset());
        entryBuffer.putInt(entry.getLevel0EdgeIndex().getEdgeCount());
        entryBuffer.put(entry.getLevel1EdgeIndex().getLevel().toString().getBytes());
        entryBuffer.putLong(entry.getLevel1EdgeIndex().getStartOffset());
        entryBuffer.putInt(entry.getLevel1EdgeIndex().getEdgeCount());
        entryBuffer.put(entry.getLevel2EdgeIndex().getLevel().toString().getBytes());
        entryBuffer.putLong(entry.getLevel2EdgeIndex().getStartOffset());
        entryBuffer.putInt(entry.getLevel2EdgeIndex().getEdgeCount());
        entryBuffer.flip();
        
        channel.write(entryBuffer, DataStructureSizes.calculateEdgeIndexOffset(entry.getNodeId()));
        entryCount++;
        
        writeHeader(entryCount, false);
        header.setEntryCount(entryCount);
        header.setTaskCompleted(false);
    }
    
    @Override
    public EdgeIndexEntry get(int nodeId) throws IOException {
        try {
            // 매핑 모드인 경우 MappedByteBuffer에서 직접 읽기
            if (mappingMode && mappedBuffer != null) {
                return readMapped(nodeId);
            }
            
            // 일반 모드: FileChannel에서 직접 읽기
            return read(nodeId);
        }
        catch (BufferUnderflowException e) {
            return null;
        }
    }

    private EdgeIndexEntry read(int nodeId) throws IOException {
        ByteBuffer entryBuffer = ByteBuffer.allocate(DataStructureSizes.EDGE_INDEX_SIZE);
        byte[] levelBytes = new byte[2];

        channel.read(entryBuffer, DataStructureSizes.calculateEdgeIndexOffset(nodeId));

        entryBuffer.flip();

        EdgeIndexEntry entry = new EdgeIndexEntry(entryBuffer.getInt());
        entry.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.fromString(new String(entryBuffer.get(levelBytes).array(), StandardCharsets.US_ASCII)), entryBuffer.getLong(), entryBuffer.getInt()));
        entry.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.fromString(new String(entryBuffer.get(levelBytes).array(), StandardCharsets.US_ASCII)), entryBuffer.getLong(), entryBuffer.getInt()));
        entry.setLevel2EdgeIndex(new LevelEdgeIndex(RoadLevel.fromString(new String(entryBuffer.get(levelBytes).array(), StandardCharsets.US_ASCII)), entryBuffer.getLong(), entryBuffer.getInt()));

        return entry;
    }
    
    private synchronized EdgeIndexEntry readMapped(int nodeId) {
        mappedBuffer.position((int) DataStructureSizes.calculateEdgeIndexOffset(nodeId));
        byte[] levelBytes = new byte[2];
        EdgeIndexEntry entry = new EdgeIndexEntry(mappedBuffer.getInt());
        int currentPos = mappedBuffer.position();
        byte b2 = mappedBuffer.get(currentPos + 1);
        mappedBuffer.position(currentPos + 2);
        entry.setLevel0EdgeIndex(new LevelEdgeIndex(RoadLevel.valueOf(b2), mappedBuffer.getLong(), mappedBuffer.getInt()));
        currentPos = mappedBuffer.position();
        b2 = mappedBuffer.get(currentPos + 1);
        mappedBuffer.position(currentPos + 2);
        // mappedBuffer.get(levelBytes);
        entry.setLevel1EdgeIndex(new LevelEdgeIndex(RoadLevel.valueOf(b2), mappedBuffer.getLong(), mappedBuffer.getInt()));
        currentPos = mappedBuffer.position();
        b2 = mappedBuffer.get(currentPos + 1);
        mappedBuffer.position(currentPos + 2);
        // mappedBuffer.get(levelBytes);
        entry.setLevel2EdgeIndex(new LevelEdgeIndex(RoadLevel.valueOf(b2), mappedBuffer.getLong(), mappedBuffer.getInt()));
        
        return entry;
    }

    @Override
    public RoadLevel viewRoadLevel(int nodeId, RoadLevel roadLevel) {
        int offset = (int) DataStructureSizes.calculateEdgeIndexOffset(nodeId) + DataStructureSizes.NODE_ID_SIZE + ((roadLevel.ordinal()) * DataStructureSizes.LEVEL_EDGE_INDEX_SIZE);
        byte[] levelBytes = new byte[2];

        mappedBuffer.get(offset,levelBytes);

        return RoadLevel.fromString((new String(levelBytes, StandardCharsets.US_ASCII)));
    }
    
    @Override
    public long viewStartOffset(int nodeId, RoadLevel roadLevel) {
        int offset = (int) DataStructureSizes.calculateEdgeIndexOffset(nodeId) + DataStructureSizes.NODE_ID_SIZE + ((roadLevel.ordinal()) * DataStructureSizes.LEVEL_EDGE_INDEX_SIZE) + DataStructureSizes.LEVEL_EDGE_INDEX_LEVEL_SIZE;
        return mappedBuffer.getLong(offset);
    }

    @Override
    public int viewEdgeCount(int nodeId, RoadLevel roadLevel) {
        int offset = (int) DataStructureSizes.calculateEdgeIndexOffset(nodeId) + DataStructureSizes.NODE_ID_SIZE + ((roadLevel.ordinal()) * DataStructureSizes.LEVEL_EDGE_INDEX_SIZE) + DataStructureSizes.LEVEL_EDGE_INDEX_LEVEL_SIZE + DataStructureSizes.LEVEL_EDGE_INDEX_START_OFFSET_SIZE;
        return mappedBuffer.getInt(offset);
    }

    @Override
    public boolean containsKey(int nodeId) {
        try {
            return get(nodeId) != null;
        } catch (IOException e) {
            log.error("containsKey 확인 실패", e);
            return false;
        }
    }
    
    @Override
    public int size() {
        if (header == null) {
            return 0;
        }
        
        return header.getEntryCount();
    }
    
    @Override
    public void flush() throws IOException {
        // FileChannel을 사용하는 경우 put() 메서드에서 이미 파일에 쓰기 때문에
        // flush는 단순히 채널을 flush하기만 하면 됨
        if (channel != null && channel.isOpen()) {
            writeHeader(entryCount, true);
            header.setEntryCount(entryCount);
            header.setTaskCompleted(true);
            channel.force(true);
            log.info("Edge 인덱스 파일 flush 완료 - {} 개 항목, 파일: {}", header.getEntryCount(), indexFilePath);
        }
    }
    
    @Override
    public void load() throws IOException {
        long fileSize = channel.size();
        
        if (fileSize == 0) {
            log.warn("인덱스 파일이 비어있습니다.");
            return;
        }
        
        // 헤더 읽기
        ByteBuffer headerBuffer = ByteBuffer.allocate(DataStructureSizes.EDGE_INDEX_HEADER_SIZE);
        channel.read(headerBuffer, 0);
        headerBuffer.flip();
        int entryCount = headerBuffer.getInt();
        boolean taskCompleted = headerBuffer.hasRemaining() && headerBuffer.get() != 0;

        header = new EdgeIndexHedaer(entryCount, taskCompleted);
        this.entryCount = entryCount;
    }
    
    /**
     * 메모리 매핑 모드로 전환 (읽기 전용)
     * 읽기 모드에서 성능 최적화를 위해 사용
     * @throws IOException IO 오류 발생 시
     */
    @Override
    public void switchToMappingMode() throws IOException {
        if (mappingMode) {
            log.debug("이미 매핑 모드입니다.");
            return;
        }
        
        if (!Files.exists(indexFilePath)) {
            log.warn("인덱스 파일이 존재하지 않습니다: {}", indexFilePath);
            return;
        }
        
        // 기존 채널 flush 및 닫기
        if (channel != null && channel.isOpen()) {
            channel.force(true);
            channel.close();
        }
        
        // 읽기 전용 FileChannel 열기
        channel = FileChannel.open(indexFilePath, StandardOpenOption.READ);
        long fileSize = channel.size();
        
        if (fileSize == 0) {
            log.warn("인덱스 파일이 비어있습니다.");
            return;
        }
        
        // MappedByteBuffer 생성
        mappedBuffer = channel.map(MapMode.READ_ONLY, 0, fileSize);
        mappingMode = true;
        
        log.info("Edge 인덱스 메모리 매핑 모드로 전환 완료 - 파일 크기: {} bytes", fileSize);
    }

    @Override
    public void close() throws IOException {
        // 채널 닫기
        if (channel != null && channel.isOpen()) {
            channel.close();
            channel = null;
        }
        
        // MappedByteBuffer는 GC가 처리 (명시적 해제 불가)
        if (mappedBuffer != null) {
            mappedBuffer = null;
        }
        
        log.info("FileBasedEdgeIndex 리소스 해제 완료");
    }
    
    @Override
    public void clear() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.truncate(0);
                writeHeader(0, false);
            }
            entryCount = 0;
            header = new EdgeIndexHedaer(0, false);
        } catch (IOException e) {
            throw new IllegalStateException("인덱스 파일 초기화 실패", e);
        }
        mappingMode = false;
    }

    public boolean isTaskCompleted() {
        return header != null && header.isTaskCompleted();
    }

    private void writeHeader(int entryCount, boolean taskCompleted) throws IOException {
        ByteBuffer headerBuffer = ByteBuffer.allocate(DataStructureSizes.EDGE_INDEX_HEADER_SIZE);
        headerBuffer.putInt(entryCount);
        headerBuffer.put((byte) (taskCompleted ? 1 : 0));
        headerBuffer.flip();
        channel.write(headerBuffer, 0);
    }
    
    /**
     * 인덱스 파일 경로 반환
     * @return 파일 경로
     */
    public Path getIndexFilePath() {
        return indexFilePath;
    }
    
    /**
     * 메모리 매핑 모드인지 확인
     * @return 매핑 모드 여부
     */
    public boolean isMappingMode() {
        return mappingMode;
    }
}
