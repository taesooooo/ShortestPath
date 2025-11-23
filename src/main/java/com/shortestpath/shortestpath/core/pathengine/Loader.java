package com.shortestpath.shortestpath.core.pathengine;

import java.io.IOException;

import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Loader {
	private Extractor extractor;
	// @Getter
	// private Graph mapGraph = new Graph();

	public Loader(Extractor extractor) throws IOException {
		this.extractor = extractor;
	}

	public void extractData() throws Exception {
		try {
			// 노드 및 엣지 추출
			long startTime = System.currentTimeMillis();
			extractor.extract((t, c) -> porgress(t, c, startTime));
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

	private void porgress(int total, int current, long startTime) {
		String eta = null;
		double progress = current/(double)total;
		if (current > 0 && progress > 0.0) {
			long elapsed = System.currentTimeMillis() - startTime;
			long estimatedTotal = (long)(elapsed / progress);
			long remain = estimatedTotal - elapsed;

			eta = formatDuration(remain);
		}
		System.out.printf("진행률: %.2f%% - 예상시간 : %s\r", (current)/(double)total * 100, eta);
	}

	private String formatDuration(long millis) {
		long seconds = millis / 1000;
		long minutes = (seconds / 60) % 60;
		long hours = (seconds / 3600);
		long sec = seconds % 60;

		return String.format("%02d:%02d:%02d", hours, minutes, sec);
	}
}
