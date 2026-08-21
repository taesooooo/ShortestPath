package com.shortestpath.shortestpath.core.pathengine.Extractor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shortestpath.shortestpath.core.pathengine.Util.GeometryUtil;

public class NodeCSVWriter {
    private static Logger logger = LoggerFactory.getLogger(NodeCSVWriter.class);

    private String filePath;
    private ArrayList<IndexInfo> indexList;

    // IndexInfo 리스트를 사용하는 생성자
    public NodeCSVWriter(String filePath, ArrayList<IndexInfo> indexList) {
        this.filePath = filePath;
        this.indexList = indexList;
    }

    public void write() throws IOException {
        logger.info("노드 CSV 저장 시작");
        BufferedWriter writer = null;
        try {
            File file = new File(filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            writer = new BufferedWriter(new FileWriter(filePath));

            // CSV 헤더 작성
            writer.write("nodeId,x,y,offset");
            writer.newLine();

            // 각 노드 정보를 CSV 형식으로 작성
            for (IndexInfo indexInfo : indexList) {
                Coordinate coordinate = GeometryUtil
                        .longToCoordinate(indexInfo.getCoordinate());
                String line = String.format("%d,%.7f,%.7f,%d",
                        indexInfo.getNodeId(),
                        coordinate.getX(),
                        coordinate.getY(),
                        indexInfo.getOffset());
                writer.write(line);
                writer.newLine();
            }
            logger.info("노드 CSV 저장 완료");
        } catch (IOException e) {
            logger.error("노드 CSV 저장 중 오류가 발생했습니다.", e);
            throw e;
        } catch (Exception e) {
            logger.error("노드 CSV 저장 중 예외 발생", e);
            throw new IOException(e);
        }
        finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
