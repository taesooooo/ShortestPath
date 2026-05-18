package com.shortestpath.shortestpath.core.pathengine.Store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Node;
import com.shortestpath.shortestpath.core.pathengine.RoadLevel;
import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.FileBasedEdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.MappableEdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.InMemoryEdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Reader.DataReader;
import com.shortestpath.shortestpath.core.pathengine.Store.Reader.EdgeViewer;
import com.shortestpath.shortestpath.core.pathengine.Store.Reader.HybridDataReader;
import com.shortestpath.shortestpath.core.pathengine.Store.Reader.MappableDataReader;
import com.shortestpath.shortestpath.core.pathengine.Store.Reader.NodeViewer;
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
    private EdgeIndex reverseEdgeIndex;  // Reverse Edge 인덱스 관리

    /**
     * 추출 단계 생성자 - Reader + Writer 모두 초기화
     * 노드/엣지 추출 중 읽기와 쓰기가 모두 필요
     */
    public HybridDataStore(String fileDirectory) throws IOException {
        this.fileDirectory = fileDirectory;
        this.readOnlyMode = false;
        this.dataPersistence = null;
        this.edgeIndex = new InMemoryEdgeIndex();  // 기본 인메모리 인덱스
        this.reverseEdgeIndex = new InMemoryEdgeIndex();

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
        this.reverseEdgeIndex = new InMemoryEdgeIndex();

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
    public int getTotalReverseEdges() throws IOException {
        return dataReader.readReverseEdgeHeader().getEdgeCount();
    }

    @Override
    public int getTotalNodes() throws IOException {
        return dataReader.readNodeHeader().getNodeCount();
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

    /**
     * 노드 파일을 지정된 크기로 축소
     * 모든 데이터 저장 후 실제 필요한 크기만큼만 유지하려면 호출
     */
    public void truncateNodeFile(long actualSize) throws IOException {
        if (readOnlyMode) {
            throw new IllegalStateException("읽기 전용 모드에서는 파일을 수정할 수 없습니다.");
        }
        if (dataWriter == null) {
            throw new IllegalStateException("Writer가 초기화되지 않았습니다.");
        }

        if (dataWriter instanceof AllocatableDataWriter) {
            ((AllocatableDataWriter) dataWriter).truncateNodeFile(actualSize);
            log.info("노드 파일 축소 완료 - 축소된 크기: {} bytes", actualSize);
        } else {
            throw new UnsupportedOperationException(
                    "현재 Writer는 파일 축소를 지원하지 않습니다: " + dataWriter.getClass().getSimpleName());
        }
    }

    /**
     * 엣지 파일을 지정된 크기로 축소
     * 모든 데이터 저장 후 실제 필요한 크기만큼만 유지하려면 호출
     */
    public void truncateEdgeFile(long actualSize) throws IOException {
        if (readOnlyMode) {
            throw new IllegalStateException("읽기 전용 모드에서는 파일을 수정할 수 없습니다.");
        }
        if (dataWriter == null) {
            throw new IllegalStateException("Writer가 초기화되지 않았습니다.");
        }

        if (dataWriter instanceof AllocatableDataWriter) {
            ((AllocatableDataWriter) dataWriter).truncateEdgeFile(actualSize);
            log.info("엣지 파일 축소 완료 - 축소된 크기: {} bytes", actualSize);
        } else {
            throw new UnsupportedOperationException(
                    "현재 Writer는 파일 축소를 지원하지 않습니다: " + dataWriter.getClass().getSimpleName());
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
    public Edge readReverseEdge(long offset) throws IOException {
        if (dataReader == null) {
            throw new IllegalStateException("Reader가 초기화되지 않았습니다.");
        }
        return dataReader.readReverseEdge(offset);
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
        boolean dataReady = dataReader.hasExtractedData();
        boolean indexReady = isIndexTaskCompleted(edgeIndex);
        boolean reverseIndexReady = isIndexTaskCompleted(reverseEdgeIndex);

        return dataReady && indexReady && reverseIndexReady;
    }

    @Override
    public NodeHeader readNodeHeader() throws IOException {
        if (dataReader == null) {
            throw new IllegalStateException("Reader가 초기화되지 않았습니다.");
        }
        return dataReader.readNodeHeader();
    }

    @Override
    public EdgeHeader readEdgeHeader() throws IOException {
        if (dataReader == null) {
            throw new IllegalStateException("Reader가 초기화되지 않았습니다.");
        }
        return dataReader.readEdgeHeader();
    }

    @Override
    public EdgeHeader readReverseEdgeHeader() throws IOException {
        if (dataReader == null) {
            throw new IllegalStateException("Reader가 초기화되지 않았습니다.");
        }
        return dataReader.readReverseEdgeHeader();
    }

    /**
     * 메모리 매핑 모드로 전환
     * 경로탐색 단계에서 성능 최적화를 위해 호출
     * Reader가 MappableDataReader를 구현하는 경우만 사용 가능
     * EdgeIndex도 매핑 모드를 지원하면 함께 전환
     */
    public void switchToMappingMode() throws IOException {
        switchDataReaderToMappingMode();
        switchEdgeIndexToMappingMode();
    }
    
    /**
     * DataReader를 메모리 매핑 모드로 전환
     * @throws IOException IO 오류 발생 시
     * @throws UnsupportedOperationException Reader가 메모리 매핑을 지원하지 않는 경우
     */
    public void switchDataReaderToMappingMode() throws IOException {
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
    }
    
    /**
     * EdgeIndex를 메모리 매핑 모드로 전환 (선택적)
     * EdgeIndex가 MappableEdgeIndex를 구현하는 경우만 전환
     * @throws IOException IO 오류 발생 시
     */
    public void switchEdgeIndexToMappingMode() throws IOException {
        if (edgeIndex != null && edgeIndex instanceof MappableEdgeIndex) {
            ((MappableEdgeIndex) edgeIndex).switchToMappingMode();
            log.info("EdgeIndex 메모리 매핑 모드로 전환 완료");
        }
        if (reverseEdgeIndex != null && reverseEdgeIndex instanceof MappableEdgeIndex) {
            ((MappableEdgeIndex) reverseEdgeIndex).switchToMappingMode();
            log.info("Reverse EdgeIndex 메모리 매핑 모드로 전환 완료");
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
        if (reverseEdgeIndex != null) {
            reverseEdgeIndex.close();
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

    @Override
    public boolean isEdgeIndexTaskCompleted() {
        return isIndexTaskCompleted(edgeIndex);
    }

    @Override
    public void setReverseEdgeIndex(EdgeIndex reverseEdgeIndex) {
        if (reverseEdgeIndex == null) {
            throw new IllegalArgumentException("ReverseEdgeIndex는 null일 수 없습니다.");
        }
        this.reverseEdgeIndex = reverseEdgeIndex;
        log.info("ReverseEdgeIndex 설정 완료 - type: {}", reverseEdgeIndex.getClass().getSimpleName());
    }

    @Override
    public EdgeIndex getReverseEdgeIndex() {
        return reverseEdgeIndex;
    }

    @Override
    public boolean isReverseEdgeIndexTaskCompleted() {
        return isIndexTaskCompleted(reverseEdgeIndex);
    }

    private boolean isIndexTaskCompleted(EdgeIndex index) {
        if (index == null) {
            return true;
        }
        if (index instanceof FileBasedEdgeIndex) {
            return ((FileBasedEdgeIndex) index).isTaskCompleted();
        }
        return index.size() > 0;
    }

    @Override
    public int viewNodeId(int nodeId) {
        return getNodeViewer().readNodeId(nodeId);
    }

    @Override
    public int viewNodeStartEdgeOffset(int nodeId) {
        return getNodeViewer().readStartEdgeOffset(nodeId);
    }

    @Override
    public double viewNodeXCoordinate(int nodeId) {
        return getNodeViewer().readXCoordinate(nodeId);
    }

    @Override
    public double viewNodeYCoordinate(int nodeId) {
        return getNodeViewer().readYCoordinate(nodeId);
    }

    @Override
    public int viewEdgeId(long offset) {
        return getEdgeViewer().readEdgeId(offset);
    }

    @Override
    public int viewEdgeFrom(long offset) {
        return getEdgeViewer().readEdgeFrom(offset);
    }

    @Override
    public int viewEdgeTo(long offset) {
        return getEdgeViewer().readEdgeTo(offset);
    }

    @Override
    public double viewEdgeDistance(long offset) {
        return getEdgeViewer().readEdgeDistance(offset);
    }

    @Override
    public int viewEdgeNextEdgeOffset(long offset) {
        return getEdgeViewer().readEdgeNextEdgeOffset(offset);
    }

    @Override
    public int viewEdgeSpeed(long offset) {
        return getEdgeViewer().readEdgeSpeed(offset);
    }

    @Override
    public RoadLevel viewEdgeRoadLevel(long offset) {
        return getEdgeViewer().readEdgeRoadLevel(offset);
    }

    @Override
    public int viewReverseEdgeId(long offset) {
        return getReverseEdgeViewer().readEdgeId(offset);
    }

    @Override
    public int viewReverseEdgeFrom(long offset) {
        return getReverseEdgeViewer().readEdgeFrom(offset);
    }

    @Override
    public int viewReverseEdgeTo(long offset) {
        return getReverseEdgeViewer().readEdgeTo(offset);
    }

    @Override
    public double viewReverseEdgeDistance(long offset) {
        return getReverseEdgeViewer().readEdgeDistance(offset);
    }

    @Override
    public int viewReverseEdgeNextEdgeOffset(long offset) {
        return getReverseEdgeViewer().readEdgeNextEdgeOffset(offset);
    }

    @Override
    public int viewReverseEdgeSpeed(long offset) {
        return getReverseEdgeViewer().readEdgeSpeed(offset);
    }

    @Override
    public RoadLevel viewReverseEdgeRoadLevel(long offset) {
        return getReverseEdgeViewer().readEdgeRoadLevel(offset);
    }

    private NodeViewer getNodeViewer() {
        if (dataReader == null) {
            throw new IllegalStateException("Reader가 초기화되지 않았습니다.");
        }
        if (dataReader instanceof HybridDataReader) {
            return ((HybridDataReader) dataReader).getNodeViewer();
        } else {
            throw new UnsupportedOperationException(
                    "현재 Reader는 NodeViewer를 지원하지 않습니다: " + dataReader.getClass().getSimpleName());
        }
    }

    private EdgeViewer getEdgeViewer() {
         if (dataReader == null) {
            throw new IllegalStateException("Reader가 초기화되지 않았습니다.");
        }
        if (dataReader instanceof HybridDataReader) {
            return ((HybridDataReader) dataReader).getEdgeViewer();
        } else {
            throw new UnsupportedOperationException(
                    "현재 Reader는 EdgeViewer를 지원하지 않습니다: " + dataReader.getClass().getSimpleName());
        }
    }

    private EdgeViewer getReverseEdgeViewer() {
         if (dataReader == null) {
            throw new IllegalStateException("Reader가 초기화되지 않았습니다.");
        }
        if (dataReader instanceof HybridDataReader) {
            return ((HybridDataReader) dataReader).getReverseEdgeViewer();
        } else {
            throw new UnsupportedOperationException(
                    "현재 Reader는 Reverse EdgeViewer를 지원하지 않습니다: " + dataReader.getClass().getSimpleName());
        }
    }

    
}
