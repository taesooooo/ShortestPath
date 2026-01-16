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
	private AtomicInteger creatorCount = new AtomicInteger(0);
	private AtomicInteger saveCount = new AtomicInteger(0);
	long startTime = 0;
	public Loader(Extractor extractor) throws IOException {
		this.extractor = extractor;
	}

	public void extractData() throws Exception {
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
		if(type == TaskType.NODE_EDGE_CREATOR) {
			creatorCount.set(current);
		}
		else {
			saveCount.set(current);
		}

		printProgress(total, type, current);
	}

	private void printProgress(int total, TaskType type, int current) {
		int creatorCurrent = creatorCount.get();
		int saveCurrent = saveCount.get();
		double creatorPrgoress = calcProgress(total, creatorCurrent);
		double saveProgress = calcProgress(total, saveCurrent);
		String creatorRemainingTime = calcETA(total, creatorCurrent);
		String saveRemainingTime = calcETA(total, saveCurrent);

		System.out.printf("노드 엣지 생성: %.2f%% (%s) / 노드 엣지 저장: %.2f%% (%s) \r", creatorPrgoress, creatorRemainingTime, saveProgress, saveRemainingTime);
	}

	private double calcProgress(int total, int current) {
		return (current / (double)total) * 100;
	}

	private String calcETA(int total, int current) {
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
