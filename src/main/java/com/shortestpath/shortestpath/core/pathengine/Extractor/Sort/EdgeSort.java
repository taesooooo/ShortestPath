package com.shortestpath.shortestpath.core.pathengine.Extractor.Sort;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shortestpath.shortestpath.core.pathengine.DataStructureSizes;
import com.shortestpath.shortestpath.core.pathengine.Edge;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.MappableDataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.HybridDataStore;

/**
 * 외부 정렬(External Sort)을 사용한 대용량 엣지 정렬
 * 
 * 1단계: 파일을 청크로 분할하여 메모리에서 정렬 (병렬)
 * 2단계: 정렬된 청크들을 K-way 병합
 * 
 * from 노드 순서대로 정렬, from이 같으면 RoadLevel 순서(L0, L1, L2)
 */
public class EdgeSort {
    private static final Logger logger = LoggerFactory.getLogger(EdgeSort.class);

    private DataStore dataStore;
    private int chunkSize; // 청크당 엣지 개수
    private int threadCount; // 병렬 처리 스레드 수

    // 기본값: 50만 개씩, 4스레드
    private static final int DEFAULT_CHUNK_SIZE = 500000;
    private static final int DEFAULT_THREAD_COUNT = 4;

    public EdgeSort(DataStore dataStore) {
        this(dataStore, DEFAULT_CHUNK_SIZE, DEFAULT_THREAD_COUNT);
    }

    public EdgeSort(DataStore dataStore, int chunkSize, int threadCount) {
        this.dataStore = dataStore;
        this.chunkSize = chunkSize;
        this.threadCount = threadCount;
    }

    public void sort() throws IOException {
        sortForward();
        sortReverse();
    }

    public void sortForward() throws IOException {
        sortToFile(new EdgeComparator(), "edge_chunk_", "edge.bin");
    }

    public void sortReverse() throws IOException {
        sortToFile(new EdgeToComparator(), "reverse_edge_chunk_", "reverse_edge.bin");
    }

    private void sortToFile(Comparator<Edge> comparator, String chunkPrefix, String resultFileName) throws IOException {
        if (dataStore == null) {
            throw new IllegalStateException("DataStore가 초기화되지 않았습니다.");
        }

        int totalEdges = dataStore.getTotalEdges();
        if (totalEdges < 2) {
            logger.info("정렬할 엣지가 없습니다.");
            return;
        }

        logger.info("외부 정렬 시작 - 총 엣지: {}, 청크 크기: {}, 스레드: {}",
                totalEdges, chunkSize, threadCount);

        // 메모리 매핑 모드로 전환 (읽기 성능 향상)
        if (dataStore instanceof MappableDataStore) {
            ((MappableDataStore) dataStore).switchDataReaderToMappingMode();
            logger.info("메모리 매핑 모드 활성화");
        }

        // 파일 디렉토리 가져오기
        String fileDirectory = getFileDirectory();

        // 1단계: 청크 정렬
        List<File> sortedChunkFiles = sortChunksInParallel(totalEdges, fileDirectory, comparator, chunkPrefix);

        try (FileChannel resultChannel = FileChannel.open(Path.of(fileDirectory, resultFileName), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            writeEdgeHeader(resultChannel, totalEdges, true, true);

            // 2단계: K-way 병합
            mergeChunks(sortedChunkFiles, totalEdges, comparator, resultChannel, resultFileName);
        } finally {
            // 임시 파일 정리
            cleanupTempFiles(sortedChunkFiles);
        }

        logger.info("외부 정렬 완료! (정렬된 결과가 {}에 저장됨)", resultFileName);
    }



    /**
     * DataStore의 파일 디렉토리 가져오기
     */
    private String getFileDirectory() throws IOException {
        if (dataStore instanceof HybridDataStore) {
            return ((HybridDataStore) dataStore).getFileDirectory();
        }
        throw new UnsupportedOperationException(
                "파일 디렉토리를 지원하지 않는 DataStore입니다.");
    }

    /**
     * 1단계: 청크 단위로 분할하여 병렬 정렬
     */
    private List<File> sortChunksInParallel(int totalEdges, String fileDirectory, Comparator<Edge> comparator, String chunkPrefix) throws IOException {
        int numChunks = (totalEdges + chunkSize - 1) / chunkSize;
        List<File> chunkFiles = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<File>> futures = new ArrayList<>();

        AtomicInteger completedChunks = new AtomicInteger(0);

        try {
            for (int chunkIdx = 0; chunkIdx < numChunks; chunkIdx++) {
                final int currentChunk = chunkIdx;
                final int startIdx = chunkIdx * chunkSize;
                final int endIdx = Math.min(startIdx + chunkSize, totalEdges);

                Future<File> future = executor.submit(new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        File chunkFile = sortChunk(currentChunk, startIdx, endIdx, fileDirectory, comparator, chunkPrefix);
                        int completed = completedChunks.incrementAndGet();
                        logger.info("청크 정렬 완료: {}/{} ({}%)",
                                completed, numChunks, completed * 100 / numChunks);
                        return chunkFile;
                    }
                });

                futures.add(future);
            }

            // 모든 청크 정렬 완료 대기
            for (Future<File> future : futures) {
                chunkFiles.add(future.get());
            }

        } catch (Exception e) {
            throw new IOException("청크 정렬 중 오류 발생", e);
        } finally {
            executor.shutdown();
        }

        return chunkFiles;
    }

    /**
     * 개별 청크를 메모리에서 정렬하고 임시 파일로 저장
     */
    private File sortChunk(int chunkIdx, int startIdx, int endIdx, String fileDirectory, Comparator<Edge> comparator, String chunkPrefix) throws IOException {
        int chunkLength = endIdx - startIdx;
        Edge[] edges = new Edge[chunkLength];

        // 청크 데이터 읽기
        for (int i = 0; i < chunkLength; i++) {
            long offset = DataStructureSizes.calculateEdgeOffset(startIdx + i);
            edges[i] = dataStore.readEdge(offset);
        }

        // 메모리 내 정렬 (병렬 정렬)
        Arrays.parallelSort(edges, comparator);

        // 정렬된 청크를 임시 파일로 저장
        File chunkFile = createTempChunkFile(chunkIdx, fileDirectory, chunkPrefix);
        try (FileChannel chunkChannel = FileChannel.open(chunkFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writeChunkToFile(edges, chunkChannel, 0);
        }

        return chunkFile;
    }

    /**
     * 임시 파일 생성 (DataStore 디렉토리에 생성)
    */
    private File createTempChunkFile(int chunkIdx, String fileDirectory, String chunkPrefix) throws IOException {
        File file = new File(fileDirectory, chunkPrefix + chunkIdx + ".tmp");
        file.deleteOnExit();
        return file;
    }

    /**
     * 엣지 배열을 파일에 쓰기
     */
    private void writeChunkToFile(Edge[] edges, FileChannel channel, long offset) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(edges.length * DataStructureSizes.EDGE_SIZE);

        for (Edge edge : edges) {
            writeEdgeToBuffer(buffer, edge);
        }

        buffer.flip();
        while (buffer.hasRemaining()) {
            offset += channel.write(buffer, offset);
        }
    }

    /**
     * 2단계: K-way 병합 - 우선순위 큐 사용 (인플레이 방식)
     * 정렬된 결과를 DataStore에 직접 써서 edge.bin을 현장 수정
     */
    private void mergeChunks(List<File> chunkFiles, int totalEdges, Comparator<Edge> comparator, FileChannel resultChannel, String resultFileName) throws IOException {
        logger.info("K-way 병합 시작 - 청크 수: {}", chunkFiles.size());

        // 각 청크 파일의 리더 준비
        List<ChunkReader> readers = new ArrayList<>();
        for (File chunkFile : chunkFiles) {
            readers.add(new ChunkReader(chunkFile));
        }

        try {
            // 우선순위 큐로 K-way 병합
            PriorityQueue<ChunkWithEdge> pq = new PriorityQueue<>(
                    new Comparator<ChunkWithEdge>() {
                        @Override
                        public int compare(ChunkWithEdge c1, ChunkWithEdge c2) {
                            return comparator.compare(c1.getEdge(), c2.getEdge());
                        }
                    });

            // 각 청크에서 첫 번째 엣지를 큐에 추가
            for (int i = 0; i < readers.size(); i++) {
                ChunkReader reader = readers.get(i);
                Edge edge = reader.readNext();
                if (edge != null) {
                    pq.offer(new ChunkWithEdge(i, edge));
                }
            }

            // 병합 처리: 정렬된 결과를 DataStore에 직접 쓰기
            int writeIndex = 0;
            int logInterval = Math.max(1, totalEdges / 10); // 10% 단위로 로그

            while (!pq.isEmpty()) {
                ChunkWithEdge current = pq.poll();

                // DataStore에 직접 쓰기 (인플레이 방식)
                long writeOffset = DataStructureSizes.calculateEdgeOffset(writeIndex);
                writeChunkToFile(new Edge[] { current.getEdge() }, resultChannel, writeOffset);

                writeIndex++;

                // 진행률 로그
                if (writeIndex % logInterval == 0) {
                    logger.info("병합 진행: {}/{} ({}%)",
                            writeIndex, totalEdges, writeIndex * 100 / totalEdges);
                }

                // 해당 청크에서 다음 엣지 읽기
                Edge nextEdge = readers.get(current.getChunkIndex()).readNext();
                if (nextEdge != null) {
                    pq.offer(new ChunkWithEdge(current.getChunkIndex(), nextEdge));
                }
            }

            logger.info("병합 완료: {} 개 엣지를 {}에 저장", writeIndex, resultFileName);
        } finally {
            // 모든 리더 닫기
            for (ChunkReader reader : readers) {
                reader.close();
            }
        }
    }

    private void writeEdgeHeader(FileChannel channel, int edgeCount, boolean sorted, boolean taskCompleted) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(DataStructureSizes.HEADER_SIZE);
        buffer.putInt(edgeCount);
        buffer.put((byte) (sorted ? 1 : 0));
        buffer.put((byte) (taskCompleted ? 1 : 0));
        buffer.flip();
        channel.write(buffer, 0);
    }

    private void writeEdgeToBuffer(ByteBuffer buffer, Edge edge) {
        buffer.putInt(edge.getId());
        buffer.putInt(edge.getFrom());
        buffer.putInt(edge.getTo());
        buffer.putDouble(edge.getDistance());
        buffer.putInt(edge.getNextEdgeOffset());
        buffer.putInt(edge.getSpeed());
        buffer.put(edge.getRoadLevel().toString().getBytes());
    }

    /**
     * 임시 청크 파일 정리
     */
    private void cleanupTempFiles(List<File> chunkFiles) {
        for (File file : chunkFiles) {
            if (file.exists()) {
                file.delete();
            }
        }
        logger.info("임시 파일 정리 완료: {} 개", chunkFiles.size());
    }
}
