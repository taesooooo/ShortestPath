package com.shortestpath.shortestpath.core.unit.Extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.MockedStatic;

import com.shortestpath.shortestpath.core.pathengine.Extractor.IndexInfo;
import com.shortestpath.shortestpath.core.pathengine.Extractor.NodeCSVWriter;
import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

public class NodeCSVWriterTest {

    private ArrayList<IndexInfo> indexList;

    @BeforeEach
    public void setUp() {
        indexList = new ArrayList<>();
    }

    @Test
    @DisplayName("NodeCSVWriter 생성 테스트 - 기본 생성자")
    public void nodeCSVWriterConstructorTest(@TempDir Path tempDir) {
        String filePath = tempDir.resolve("test.csv").toString();
        
        NodeCSVWriter writer = new NodeCSVWriter(filePath, indexList);
        
        assertThat(writer).isNotNull();
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - CSV 파일 생성")
    public void nodeCSVWriterCreateFileTest(@TempDir Path tempDir) throws IOException {
        // Arrange
        String filePath = tempDir.resolve("nodes.csv").toString();
        
        indexList.add(new IndexInfo(0, 1000L, 100));
        
        NodeCSVWriter writer = new NodeCSVWriter(filePath, indexList);

        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(1000L))
                .thenReturn(new Coordinate(37.5, 126.5));

            // Act
            writer.write();
        }

        // Assert
        File file = new File(filePath);
        assertThat(file).exists();
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - CSV 헤더 작성")
    public void nodeCSVWriterHeaderTest(@TempDir Path tempDir) throws IOException {
        // Arrange
        String filePath = tempDir.resolve("nodes.csv").toString();
        
        indexList.add(new IndexInfo(0, 1000L, 100));
        
        NodeCSVWriter writer = new NodeCSVWriter(filePath, indexList);

        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(any(Long.class)))
                .thenReturn(new Coordinate(37.5, 126.5));

            // Act
            writer.write();
        }

        // Assert
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String header = reader.readLine();
            assertThat(header).isEqualTo("nodeId,x,y,offset");
        }
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - 다중 항목 작성")
    public void nodeCSVWriterMultipleItemsTest(@TempDir Path tempDir) throws IOException {
        // Arrange
        String filePath = tempDir.resolve("nodes.csv").toString();
        
        indexList.add(new IndexInfo(0, 1000L, 100));
        indexList.add(new IndexInfo(1, 2000L, 200));
        indexList.add(new IndexInfo(2, 3000L, 300));
        
        NodeCSVWriter writer = new NodeCSVWriter(filePath, indexList);

        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(1000L))
                .thenReturn(new Coordinate(37.5, 126.5));
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(2000L))
                .thenReturn(new Coordinate(37.6, 126.6));
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(3000L))
                .thenReturn(new Coordinate(37.7, 126.7));

            // Act
            writer.write();
        }

        // Assert
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String header = reader.readLine();
            assertThat(header).isEqualTo("nodeId,x,y,offset");
            
            String line1 = reader.readLine();
            assertThat(line1).contains("0").contains("37.5");
            
            String line2 = reader.readLine();
            assertThat(line2).contains("1").contains("37.6");
            
            String line3 = reader.readLine();
            assertThat(line3).contains("2").contains("37.7");
            
            String line4 = reader.readLine();
            assertThat(line4).isNull();
        }
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - 좌표 포맷팅 (7자리)")
    public void nodeCSVWriterCoordinateFormatTest(@TempDir Path tempDir) throws IOException {
        // Arrange
        String filePath = tempDir.resolve("nodes.csv").toString();
        
        indexList.add(new IndexInfo(0, 1000L, 100));
        
        NodeCSVWriter writer = new NodeCSVWriter(filePath, indexList);

        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(1000L))
                .thenReturn(new Coordinate(37.123456789, 126.987654321));

            // Act
            writer.write();
        }

        // Assert
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            reader.readLine(); // skip header
            String dataLine = reader.readLine();
            // 좌표가 7자리로 포맷팅되는지 확인
            assertThat(dataLine).matches("0,37\\.\\d{7},126\\.\\d{7},100");
        }
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - 빈 리스트")
    public void nodeCSVWriterEmptyListTest(@TempDir Path tempDir) throws IOException {
        // Arrange
        String filePath = tempDir.resolve("nodes.csv").toString();
        
        // indexList는 비어있음
        
        NodeCSVWriter writer = new NodeCSVWriter(filePath, indexList);

        // Act
        writer.write();

        // Assert
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String header = reader.readLine();
            assertThat(header).isEqualTo("nodeId,x,y,offset");
            
            String line2 = reader.readLine();
            assertThat(line2).isNull();
        }
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - 폴더 자동 생성")
    public void nodeCSVWriterCreateParentDirectoryTest(@TempDir Path tempDir) throws IOException {
        // Arrange
        String filePath = tempDir.resolve("subdir/nodes.csv").toString();
        
        indexList.add(new IndexInfo(0, 1000L, 100));
        
        NodeCSVWriter writer = new NodeCSVWriter(filePath, indexList);

        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(any(Long.class)))
                .thenReturn(new Coordinate(37.5, 126.5));

            // Act
            writer.write();
        }

        // Assert
        File file = new File(filePath);
        assertThat(file).exists();
        assertThat(file.getParentFile()).exists().isDirectory();
    }

    @Test
    @DisplayName("NodeCSVWriter 실행 테스트 - IOException 처리 (잘못된 경로)")
    public void nodeCSVWriterInvalidPathTest() {
        // Arrange
        String filePath = "null";
        
        indexList.add(new IndexInfo(0, 1000L, 100));
        
        NodeCSVWriter writer = new NodeCSVWriter(filePath, indexList);

        try (MockedStatic<GeometryUtil> mockedUtil = mockStatic(GeometryUtil.class)) {
            mockedUtil.when(() -> GeometryUtil.longToCoordinate(any(Long.class)))
                .thenReturn(new Coordinate(37.5, 126.5));

            // Act & Assert - IOException이 발생해야 함
            assertThatThrownBy(() -> writer.write()).isInstanceOf(IOException.class);
        }
    }
}
