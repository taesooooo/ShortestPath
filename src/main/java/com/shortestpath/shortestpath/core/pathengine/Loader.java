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
	private AtomicInteger edgeExtractCount = new AtomicInteger(0);
	private boolean nodeCompleted = false;
	private boolean edgeCompleted = false;
	long startTime = 0;

	public Loader(Extractor extractor) throws IOException {
		this.extractor = extractor;
	}

	public void extractData() throws IOException {
		try {
			// 노드 및 엣지 추출
			startTime = System.currentTimeMillis();
			extractor.extract((taskType, total, current) -> onPrgress(taskType, total, current));
			long endTime = System.currentTimeMillis();

			log.info("종료 시간 - " + formatDuration(endTime - startTime));
		} catch (IOException e) {
			log.error("노드 및 엣지 추출 작업중 오류가 발생 했습니다.", e);
			throw e;
		}
	}

	public boolean isDataExtracted() {
		return extractor.getStore().hasExtractedData();
	}

	private void onPrgress(TaskType type, int total, int current) {
		if(type == TaskType.NODE_EXTRACT) {
			nodeExtractCount.set(current);
			if(current >= total) {
				nodeCompleted = true;
			}
		}
		else {
			edgeExtractCount.set(current);
			if(current >= total) {
				edgeCompleted = true;
			}
		}

		// 둘 다 완료되지 않았으면 진행 상황 출력
		if(!nodeCompleted || !edgeCompleted) {
			printProgress(total, type, current);
		}
	}

	private void printProgress(int total, TaskType type, int current) {
		int nodeCurrent = nodeExtractCount.get();
		int edgeCurrent = edgeExtractCount.get();

		double nodeProgress = calcProgress(total, nodeCurrent);

		String nodeRemainingTime = calcETA(total, nodeCurrent);

		System.out.printf("노드 추출: %.2f%% (%s) / 엣지 추출: %d개 \r", nodeProgress, nodeRemainingTime, edgeCurrent);
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
