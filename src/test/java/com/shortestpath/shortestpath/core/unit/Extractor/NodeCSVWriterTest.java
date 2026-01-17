package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.MockedStatic;

import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeCSVWriter;
import com.shortestpath.shortestpath.core.pathengine.Extractor.ProgressStatus;
import com.shortestpath.shortestpath.core.pathengine.Extractor.TaskType;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.EndItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.NodeCSVItem;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Task.TaskItem;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

public class NodeCSVWriterTest {

    private BlockingQueue<TaskItem> csvQueue;
    private ProgressStatus progressStatus;
    private int totalItems;
    private AtomicBoolean shouldContinue;

    @BeforeEach
    public void setUp() {
        csvQueue = new LinkedBlockingQueue<>();
        progressStatus = mock(ProgressStatus.class);
        totalItems = 100;
        shouldContinue = new AtomicBoolean(true);
    }

    @Test
    @DisplayName("NodeCSVWriter 생성 테스트 - 기본 생성자")
    public void nodeCSVWriterConstructorTest(@TempDir Path tempDir) {
        String filePath = tempDir.resolve("test.csv").toString();
        
        NodeCSVWriter writer = new NodeCSVWriter(csvQueue, filePath, progressStatus, totalItems, shouldContinue);
        
        assertThat(writer).isNotNull();
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - CSV 파일 생성")
    public void nodeCSVWriterCreateFileTest(@TempDir Path tempDir) throws InterruptedException, IOException {
        // Arrange
        String filePath = tempDir.resolve("nodes.csv").toString();
        
        csvQueue.put(new EndItem());
        
        NodeCSVWriter writer = new NodeCSVWriter(csvQueue, filePath, null, totalItems, shouldContinue);

        // Act
        writer.run();

        // Assert
        File file = new File(filePath);
        assertThat(file).exists();
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - CSV 헤더 작성")
    public void nodeCSVWriterHeaderTest(@TempDir Path tempDir) throws InterruptedException, IOException {
        // Arrange
        String filePath = tempDir.resolve("nodes.csv").toString();
        
        csvQueue.put(new EndItem());
        
        NodeCSVWriter writer = new NodeCSVWriter(csvQueue, filePath, null, totalItems, shouldContinue);

        // Act
        writer.run();

        // Assert
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String header = reader.readLine();
            assertThat(header).isEqualTo("nodeId,x,y,offset");
        }
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - NodeCSVItem 작성")
    public void nodeCSVWriterWriteItemTest(@TempDir Path tempDir) throws InterruptedException, IOException {
        // Arrange
        String filePath = tempDir.resolve("nodes.csv").toString();
        
        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(1000L))
                .thenReturn(new Coordinate(37.5, 126.5));

            NodeCSVItem csvItem = new NodeCSVItem(0, 1000L, 100);
            csvQueue.put(csvItem);
            csvQueue.put(new EndItem());

            NodeCSVWriter writer = new NodeCSVWriter(csvQueue, filePath, null, totalItems, shouldContinue);

            // Act
            writer.run();

            // Assert
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String header = reader.readLine();
                assertThat(header).isEqualTo("nodeId,x,y,offset");
                
                String dataLine = reader.readLine();
                assertThat(dataLine).contains("0");
                assertThat(dataLine).contains("37.5");
                assertThat(dataLine).contains("126.5");
                assertThat(dataLine).contains("100");
            }
        }
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - 다중 항목 작성")
    public void nodeCSVWriterMultipleItemsTest(@TempDir Path tempDir) throws InterruptedException, IOException {
        // Arrange
        String filePath = tempDir.resolve("nodes.csv").toString();
        
        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(1000L))
                .thenReturn(new Coordinate(37.5, 126.5));
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(2000L))
                .thenReturn(new Coordinate(37.6, 126.6));
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(3000L))
                .thenReturn(new Coordinate(37.7, 126.7));

            csvQueue.put(new NodeCSVItem(0, 1000L, 100));
            csvQueue.put(new NodeCSVItem(1, 2000L, 200));
            csvQueue.put(new NodeCSVItem(2, 3000L, 300));
            csvQueue.put(new EndItem());

            NodeCSVWriter writer = new NodeCSVWriter(csvQueue, filePath, null, totalItems, shouldContinue);

            // Act
            writer.run();

            // Assert
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String header = reader.readLine();
                assertThat(header).isEqualTo("nodeId,x,y,offset");
                
                String line1 = reader.readLine();
                assertThat(line1).contains("0");
                
                String line2 = reader.readLine();
                assertThat(line2).contains("1");
                
                String line3 = reader.readLine();
                assertThat(line3).contains("2");
                
                String line4 = reader.readLine();
                assertThat(line4).isNull();
            }
        }
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - 진행률 업데이트")
    public void nodeCSVWriterProgressUpdateTest(@TempDir Path tempDir) throws InterruptedException, IOException {
        // Arrange
        String filePath = tempDir.resolve("nodes.csv").toString();
        
        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(any(Long.class)))
                .thenReturn(new Coordinate(37.5, 126.5));

            csvQueue.put(new NodeCSVItem(0, 1000L, 100));
            csvQueue.put(new EndItem());

            NodeCSVWriter writer = new NodeCSVWriter(csvQueue, filePath, progressStatus, totalItems, shouldContinue);

            // Act
            writer.run();

            // Assert
            verify(progressStatus).progress(TaskType.NODE_CSV_WRITER, totalItems, 1);
        }
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - 스레드 인터럽트")
    public void nodeCSVWriterThreadInterruptTest(@TempDir Path tempDir) throws InterruptedException, IOException {
        // Arrange
        String filePath = tempDir.resolve("nodes.csv").toString();
        
        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(1000L))
                .thenReturn(new Coordinate(37.5, 126.5));

            NodeCSVWriter writer = new NodeCSVWriter(csvQueue, filePath, null, totalItems, shouldContinue);

            // Act - 현재 스레드에 인터럽트 신호 설정
            Thread currentThread = Thread.currentThread();
            currentThread.interrupt();
            
            writer.run();

            // Assert - 정상 종료되어야 함
            Thread.interrupted(); // 인터럽트 상태 정리
        }
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - 폴더 생성")
    public void nodeCSVWriterCreateDirectoryTest(@TempDir Path tempDir) throws InterruptedException, IOException {
        // Arrange
        String filePath = tempDir.resolve("subdir/nodes.csv").toString();
        
        csvQueue.put(new EndItem());
        
        NodeCSVWriter writer = new NodeCSVWriter(csvQueue, filePath, null, totalItems, shouldContinue);

        // Act
        writer.run();

        // Assert
        File file = new File(filePath);
        assertThat(file).exists();
        assertThat(file.getParentFile()).exists().isDirectory();
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - 좌표 포맷팅")
    public void nodeCSVWriterCoordinateFormatTest(@TempDir Path tempDir) throws InterruptedException, IOException {
        // Arrange
        String filePath = tempDir.resolve("nodes.csv").toString();
        
        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            // 7자리까지 출력되는지 확인
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(1000L))
                .thenReturn(new Coordinate(37.123456789, 126.987654321));

            csvQueue.put(new NodeCSVItem(0, 1000L, 100));
            csvQueue.put(new EndItem());

            NodeCSVWriter writer = new NodeCSVWriter(csvQueue, filePath, null, totalItems, shouldContinue);

            // Act
            writer.run();

            // Assert
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                reader.readLine(); // skip header
                String dataLine = reader.readLine();
                // 좌표가 7자리로 포맷팅되는지 확인
                assertThat(dataLine).matches("0,37\\.\\d{7},126\\.\\d{7},100");
            }
        }
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - EndItem 수신으로 정상 종료")
    public void nodeCSVWriterEndItemTest(@TempDir Path tempDir) throws InterruptedException, IOException {
        // Arrange
        String filePath = tempDir.resolve("nodes.csv").toString();
        
        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(1000L))
                .thenReturn(new Coordinate(37.5, 126.5));

            csvQueue.put(new NodeCSVItem(0, 1000L, 100));
            csvQueue.put(new EndItem());

            NodeCSVWriter writer = new NodeCSVWriter(csvQueue, filePath, null, totalItems, shouldContinue);

            // Act
            writer.run();

            // Assert - 파일이 정상 생성되고 4줄(헤더 + 데이터 + null)
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    lineCount++;
                }
                assertThat(lineCount).isEqualTo(2); // header + 1 data line
            }
        }
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - IOException 처리")
    public void nodeCSVWriterIOExceptionTest() throws InterruptedException, IOException {
        // Arrange
        String filePath = "/invalid/path/that/does/not/exist/nodes.csv";
        
        csvQueue.put(new EndItem());
        
        NodeCSVWriter writer = new NodeCSVWriter(csvQueue, filePath, null, totalItems, shouldContinue);

        // Act & Assert
        writer.run();
    }
}
