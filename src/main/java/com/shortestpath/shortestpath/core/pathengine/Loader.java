package com.shortestpath.shortestpath.core.pathengine;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Extractor.TaskType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Loader {
	private Extractor extractor;
	// @Getter
	// private Graph mapGraph = new Graph();
	private AtomicInteger nodeExtractCount = new AtomicInteger(0);
	private AtomicInteger nodeSaveCount = new AtomicInteger(0);
	private AtomicInteger edgeExtractCount = new AtomicInteger(0);
	private AtomicInteger edgeSaveCount = new AtomicInteger(0);
	private boolean nodeExtractCompleted = false;
	private boolean nodeSaveCompleted = false;
	private boolean edgeExtractCompleted = false;
	private boolean edgeSaveCompleted = false;
	long startTime = 0;
	int totalNodes = 0;
	int totalEdges = 0;

	public Loader(Extractor extractor) throws IOException {
		this.extractor = extractor;
	}

	public void extractData(boolean progress) throws IOException {
		try {
			// 노드 및 엣지 추출
			startTime = System.currentTimeMillis();
			if(progress) {
				extractor.extract((taskType, total, current) -> onPrgress(taskType, total, current));
			}
			else {
				extractor.extract();
			}
			long endTime = System.currentTimeMillis();

			log.info("종료 시간 - " + formatDuration(endTime - startTime));
		} catch (IOException e) {
			log.error("노드 및 엣지 추출 작업중 오류가 발생 했습니다.", e);
			throw e;
		}
	}

	public void createIndex() throws IOException {
		extractor.createIndex();
	}

	public boolean isDataExtracted() {
		return extractor.getStore().hasExtractedData();
	}

	private void onPrgress(TaskType type, int total, int current) {
		switch(type) {
			case NODE_EXTRACT:
				nodeExtractCount.set(current);
				totalNodes = total;
				if(current >= total) {
					nodeExtractCompleted = true;
				}
				break;
			case NODE_SAVE:
				nodeSaveCount.set(current);
				if(current >= total) {
					nodeSaveCompleted = true;
				}
				break;
			case EDGE_EXTRACT:
				edgeExtractCount.set(current);
				totalEdges = total;
				if(total > 0 && current >= total) {
					edgeExtractCompleted = true;
				}
				break;
			case EDGE_SAVE:
				edgeSaveCount.set(current);
				if(current >= totalEdges && totalEdges > 0) {
					edgeSaveCompleted = true;
				}
				break;
			default:
				break;
		}

		// 진행 상황 표시
		printProgress();
	}

	private void printProgress() {
		// 노드 추출 개수
		int nodeExtracted = nodeExtractCount.get();
		
		// 노드 저장 개수
		int nodeSaved = nodeSaveCount.get();

		// 엣지 추출 개수
		int edgeExtracted = edgeExtractCount.get();

		// 엣지 저장 개수
		int edgeSaved = edgeSaveCount.get();

		// 모든 진행 상황을 한 줄에 표시 (개수 기준)
		System.out.printf("[노드 추출] %10d개 | [노드 저장] %10d개 | [엣지 추출] %10d개 | [엣지 저장] %10d개        \r",
				nodeExtracted,
				nodeSaved,
				edgeExtracted,
				edgeSaved);

	}

	private double calcProgress(int total, int current) {
		if(current >= total) {
			return 100.0;
		}

		return (current / (double)total) * 100;
	}

	private String calcETA(int total, int current) {
		if(current >= total) {
			return "00:00:00";
		}

		long elapsedTime = System.currentTimeMillis() - startTime;
		double speed = current / (double)elapsedTime;
		long remainingItems = total - current;
		long remainingTime = (long)(remainingItems / speed);

		return formatDuration(remainingTime);
	}

	private String formatDuration(long millis) {
		long seconds = millis / 1000;
		long minutes = (seconds / 60) % 60;
		long hours = (seconds / 3600);
		long sec = seconds % 60;

		return String.format("%02d:%02d:%02d", hours, minutes, sec);
	}
}
