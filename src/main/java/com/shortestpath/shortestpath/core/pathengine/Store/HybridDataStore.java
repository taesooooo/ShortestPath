package com.shortestpath.shortestpath.core.pathengine.Store;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.InMemoryEdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Reader.DataReader;
import com.shortestpath.shortestpath.core.pathengine.Store.Reader.HybridDataReader;
import com.shortestpath.shortestpath.core.pathengine.Store.Reader.MappableDataReader;
import com.shortestpath.shortestpath.core.pathengine.Store.Writer.AllocatableDataWriter;
import com.shortestpath.shortestpath.core.pathengine.Store.Writer.DataWriter;
import com.shortestpath.shortestpath.core.pathengine.Store.Writer.HeaderWriter;
import com.shortestpath.shortestpath.core.pathengine.Store.Writer.HybridDataWriter;

import lombok.extern.slf4j.Slf4j;

/**
 * Reader/Writer를 통합한 데이터 스토어 구현
 * 추출 단계: Reader + Writer 모두 활성화 (읽기/쓰기 자유로운 사용)
 * 경로탐색 단계: Reader만 활성화 (메모리 매핑으로 최적화)
 * 
 * 조회 모드:
 * - DataPersistence 설정: DB에서 먼저 조회
 * - DataPersistence 미설정: Reader(인메모리/파일)에서만 조회
 * 
 * - MappableDataReader, IndexableDataReader, AllocatableDataWriter는 선택적 사용
 */
@Slf4j
public class HybridDataStore implements MappableDataStore {
    private DataWriter dataWriter;
    private DataReader dataReader;
    private String fileDirectory;
    private boolean readOnlyMode;
    private DataPersistence dataPersistence;  // DB 모드 설정
    private EdgeIndex edgeIndex;  // Edge 인덱스 관리

    /**
     * 추출 단계 생성자 - Reader + Writer 모두 초기화
     * 노드/엣지 추출 중 읽기와 쓰기가 모두 필요
     */
    public HybridDataStore(String fileDirectory) throws IOException {
        this.fileDirectory = fileDirectory;
        this.readOnlyMode = false;
        this.dataPersistence = null;
        this.edgeIndex = new InMemoryEdgeIndex();  // 기본 인메모리 인덱스

        // 추출 단계: Writer 먼저 생성 (파일 생성)
        this.dataWriter = new HybridDataWriter(fileDirectory);
        
        // 추출 단계: Reader도 함께 활성화 (노드/엣지 검증 시 읽기 필요)
        Path nodeFilePath = new java.io.File(fileDirectory).toPath().resolve("node.bin");
        Path edgeFilePath = new java.io.File(fileDirectory).toPath().resolve("edge.bin");
        this.dataReader = new HybridDataReader(nodeFilePath, edgeFilePath);

        log.info("HybridDataStore 추출 모드 초기화 완료 - fileDirectory: {}", fileDirectory);
    }

    /**
     * 경로탐색 단계 생성자 - Reader만 초기화
     * 읽기 전용 모드로 메모리 매핑 최적화
     */
    public HybridDataStore(String fileDirectory, boolean readMode) throws IOException {
        this.fileDirectory = fileDirectory;
        this.readOnlyMode = readMode;
        this.edgeIndex = new InMemoryEdgeIndex();  // 기본 인메모리 인덱스

        if (readMode) {
            // 경로탐색 단계: Reader만 초기화
            Path nodeFilePath = new java.io.File(fileDirectory).toPath().resolve("node.bin");
            Path edgeFilePath = new java.io.File(fileDirectory).toPath().resolve("edge.bin");

            this.dataReader = new HybridDataReader(nodeFilePath, edgeFilePath);
            this.dataWriter = null;
            
            log.info("HybridDataStore 경로탐색 모드 초기화 완료 - fileDirectory: {}", fileDirectory);
        } else {
            throw new IllegalArgumentException("readMode가 false인 경우 기본 생성자를 사용하세요");
        }
    }

    @Override
    public int getTotalEdges() throws IOException {
        return dataReader.readEdgeHeader().getEdgeCount();
    }

    @Override
    public int getTotalNodes() throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int saveNode(Node node) throws IOException {
        if (readOnlyMode) {
            throw new IllegalStateException("읽기 전용 모드에서는 저장할 수 없습니다.");
        }
        if (dataWriter == null) {
            throw new IllegalStateException("Writer가 초기화되지 않았습니다.");
        }
        return dataWriter.saveNode(node);
    }

    @Override
    public int saveNode(Node node, long offset) throws IOException {
        if (readOnlyMode) {
            throw new IllegalStateException("읽기 전용 모드에서는 저장할 수 없습니다.");
        }
        if (dataWriter == null) {
            throw new IllegalStateException("Writer가 초기화되지 않았습니다.");
        }
        return dataWriter.saveNode(node, offset);
    }

    @Override
    public int saveEdge(Edge edge) throws IOException {
        if (readOnlyMode) {
            throw new IllegalStateException("읽기 전용 모드에서는 저장할 수 없습니다.");
        }
        if (dataWriter == null) {
            throw new IllegalStateException("Writer가 초기화되지 않았습니다.");
        }
        
        int offset = dataWriter.saveEdge(edge);
        
        return offset;
    }

    @Override
    public int saveEdge(Edge edge, long offset) throws IOException {
        if (readOnlyMode) {
            throw new IllegalStateException("읽기 전용 모드에서는 저장할 수 없습니다.");
        }
        if (dataWriter == null) {
            throw new IllegalStateException("Writer가 초기화되지 않았습니다.");
        }
        
        int savedOffset = dataWriter.saveEdge(edge, offset);
        
        return savedOffset;
    }

    @Override
    public int overwriteNode(Node node, long offset) throws IOException {
        if (readOnlyMode) {
            throw new IllegalStateException("읽기 전용 모드에서는 수정할 수 없습니다.");
        }
        if (dataWriter == null) {
            throw new IllegalStateException("Writer가 초기화되지 않았습니다.");
        }
        return dataWriter.overwriteNode(node, offset);
    }

    @Override
    public int overwriteEdge(Edge edge, long offset) throws IOException {
        if (readOnlyMode) {
            throw new IllegalStateException("읽기 전용 모드에서는 수정할 수 없습니다.");
        }
        if (dataWriter == null) {
            throw new IllegalStateException("Writer가 초기화되지 않았습니다.");
        }
        return dataWriter.overwriteEdge(edge, offset);
    }

    @Override
    public void saveNodeIndex(List<IndexInfo> indexList) throws IOException {
        if (readOnlyMode) {
            throw new IllegalStateException("읽기 전용 모드에서는 인덱스를 저장할 수 없습니다.");
        }
        
        if (dataPersistence == null) {
            log.warn("DataPersistence가 설정되지 않았습니다. 인덱스 저장을 건너뜁니다.");
            return;
        }

        if (dataWriter == null) {
            throw new IllegalStateException("Writer가 초기화되지 않았습니다.");
        }

        dataPersistence.saveNodeIndex(indexList);
        log.info("노드 인덱스를 데이터베이스에 저장 완료 - count: {}", indexList.size());
    }

    /**
     * AllocatableDataWriter 메서드
     * Writer가 AllocatableDataWriter를 구현하는 경우만 사용 가능
     */
    public void allocateNodeFileSpace(long size) throws IOException {
        if (readOnlyMode) {
            throw new IllegalStateException("읽기 전용 모드에서는 파일 공간을 할당할 수 없습니다.");
        }
        if (dataWriter == null) {
            throw new IllegalStateException("Writer가 초기화되지 않았습니다.");
        }

        if (dataWriter instanceof AllocatableDataWriter) {
            ((AllocatableDataWriter) dataWriter).allocateNodeFileSpace(size);
        } else {
            throw new UnsupportedOperationException(
                    "현재 Writer는 파일 공간 할당을 지원하지 않습니다: " + dataWriter.getClass().getSimpleName());
        }
    }

    public void allocateEdgeFileSpace(long size) throws IOException {
        if (readOnlyMode) {
            throw new IllegalStateException("읽기 전용 모드에서는 파일 공간을 할당할 수 없습니다.");
        }
        if (dataWriter == null) {
            throw new IllegalStateException("Writer가 초기화되지 않았습니다.");
        }

        if (dataWriter instanceof AllocatableDataWriter) {
            ((AllocatableDataWriter) dataWriter).allocateEdgeFileSpace(size);
        } else {
            throw new UnsupportedOperationException(
                    "현재 Writer는 파일 공간 할당을 지원하지 않습니다: " + dataWriter.getClass().getSimpleName());
        }
    }

    // ===== Read 메서드 =====

    @Override
    public Node readNode(long offset) throws IOException {
        if (dataReader == null) {
            throw new IllegalStateException("Reader가 초기화되지 않았습니다.");
        }
        
        // 인메모리/파일에서만 조회
        return dataReader.readNode(offset);
    }

    @Override
    public Edge readEdge(long offset) throws IOException {
        if (dataReader == null) {
            throw new IllegalStateException("Reader가 초기화되지 않았습니다.");
        }
        return dataReader.readEdge(offset);
    }

    @Override
    public int getNodeOffset(Coordinate coordinate) {
        if (dataReader == null) {
            throw new IllegalStateException("Reader가 초기화되지 않았습니다.");
        }

        // DataPersistence가 설정된 경우 DB에서 조회 (우선)
        if (dataPersistence == null) {
            throw new IllegalStateException("DataPersistence가 설정되지 않았습니다.");
        }

        int offset = dataPersistence.getNodeIndex(coordinate);
        log.debug("DB에서 노드 오프셋 조회 성공 - coordinate: {}, offset: {}", coordinate, offset);
        
        return offset;
        
        // // Reader에서 조회 (인메모리 모드)
        // if (dataReader instanceof IndexableDataReader) {
        //     int offset = ((IndexableDataReader) dataReader).getNodeOffset(coordinate);
        //     log.debug("Reader에서 노드 오프셋 조회 성공 - coordinate: {}, offset: {}", coordinate, offset);
        //     return offset;
        // }
        
        // throw new UnsupportedOperationException(
        //         "현재 Reader는 좌표 인덱싱을 지원하지 않습니다: " + dataReader.getClass().getSimpleName());
        
    }

    @Override
    public boolean hasExtractedData() {
        if (dataReader == null) {
            throw new IllegalStateException("Reader가 초기화되지 않았습니다.");
        }
        return dataReader.hasExtractedData();
    }

    /**
     * 메모리 매핑 모드로 전환
     * 경로탐색 단계에서 성능 최적화를 위해 호출
     * Reader가 MappableDataReader를 구현하는 경우만 사용 가능
     * EdgeIndex도 매핑 모드를 지원하면 함께 전환
     */
    public void switchToMappingMode() throws IOException {
        if (!readOnlyMode) {
            log.warn("경로탐색 단계에서만 메모리 매핑 모드를 사용하는 것을 권장합니다.");
        }
        
        if (dataReader == null) {
            throw new IllegalStateException("Reader가 초기화되지 않았습니다.");
        }

        // DataReader 매핑 모드 전환
        if (dataReader instanceof MappableDataReader) {
            ((MappableDataReader) dataReader).switchToMappingMode();
            log.info("DataReader 메모리 매핑 모드로 전환 완료");
        } else {
            throw new UnsupportedOperationException(
                    "현재 Reader는 메모리 매핑을 지원하지 않습니다: " + dataReader.getClass().getSimpleName());
        }
        
        // EdgeIndex 매핑 모드 전환 (지원하는 경우)
        if (edgeIndex != null && edgeIndex.supportsMappingMode()) {
            edgeIndex.switchToMappingMode();
            log.info("EdgeIndex 메모리 매핑 모드로 전환 완료");
        }
    }

    @Override
    public void close() throws IOException {
        if (dataWriter != null) {
            dataWriter.close();
        }
        if (dataReader != null) {
            dataReader.close();
        }
        if (edgeIndex != null) {
            edgeIndex.close();
        }
        log.info("HybridDataStore 리소스 해제 완료");
    }

    @Override
    public void writeNodeHeader(NodeHeader header) throws IOException {
        if (readOnlyMode) {
            throw new IllegalStateException("읽기 전용 모드에서는 헤더를 작성할 수 없습니다.");
        }
        if (dataWriter == null) {
            throw new IllegalStateException("Writer가 초기화되지 않았습니다.");
        }
        if (!(dataWriter instanceof HeaderWriter)) {
            throw new UnsupportedOperationException("현재 Writer는 헤더 작성을 지원하지 않습니다.");
        }
        ((HeaderWriter) dataWriter).writeNodeHeader(header);
    }

    @Override
    public void writeEdgeHeader(EdgeHeader header) throws IOException {
        if (readOnlyMode) {
            throw new IllegalStateException("읽기 전용 모드에서는 헤더를 작성할 수 없습니다.");
        }
        if (dataWriter == null) {
            throw new IllegalStateException("Writer가 초기화되지 않았습니다.");
        }
        if (!(dataWriter instanceof HeaderWriter)) {
            throw new UnsupportedOperationException("현재 Writer는 헤더 작성을 지원하지 않습니다.");
        }
        ((HeaderWriter) dataWriter).writeEdgeHeader(header);
    }

    public String getFileDirectory() {
        return fileDirectory;
    }

    /**
     * DataPersistence 설정 (DB 조회 및 저장 모드 활성화)
     * @param dataPersistence DataPersistence 구현체 (null이면 인메모리 모드)
     */
    public void setPersistence(DataPersistence dataPersistence) {
        this.dataPersistence = dataPersistence;
    }
    
    /**
     * @deprecated setPersistence() 사용 권장
     */
    @Deprecated
    public void setNodeProvider(DataPersistence dataPersistence) {
        this.dataPersistence = dataPersistence;
    }

    public DataWriter getDataWriter() {
        return dataWriter;
    }

    public DataReader getDataReader() {
        return dataReader;
    }
    
    @Override
    public void setEdgeIndex(EdgeIndex edgeIndex) {
        if (edgeIndex == null) {
            throw new IllegalArgumentException("EdgeIndex는 null일 수 없습니다.");
        }
        this.edgeIndex = edgeIndex;
        log.info("EdgeIndex 설정 완료 - type: {}", edgeIndex.getClass().getSimpleName());
    }
    
    @Override
    public EdgeIndex getEdgeIndex() {
        return edgeIndex;
    }
}
