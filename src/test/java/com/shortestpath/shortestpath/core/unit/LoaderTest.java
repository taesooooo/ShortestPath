package com.shortestpath.shortestpath.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.shortestpath.shortestpath.core.pathengine.Loader;
import com.shortestpath.shortestpath.core.pathengine.Extractor.Extractor;
import com.shortestpath.shortestpath.core.pathengine.Extractor.ProgressStatus;
import com.shortestpath.shortestpath.core.pathengine.Extractor.TaskType;

class LoaderTest {

	@Test
	void throttlesConsoleProgressOutputAndPrintsCompletion() throws Exception {
		Extractor extractor = mock(Extractor.class);
		doAnswer(invocation -> {
			ProgressStatus progressStatus = invocation.getArgument(0);
			for (int current = 1; current <= 100_000; current++) {
				progressStatus.progress(TaskType.NODE_EXTRACT, 100_000, current);
			}
			return null;
		}).when(extractor).extract(any(ProgressStatus.class));

		PrintStream originalOut = System.out;
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (PrintStream testOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
			System.setOut(testOut);
			new Loader(extractor).extractData(true);
		} finally {
			System.setOut(originalOut);
		}

		String progressOutput = output.toString(StandardCharsets.UTF_8);
		assertThat(countOccurrences(progressOutput, "[노드 추출]")).isLessThan(100);
		assertThat(progressOutput).contains("100000개");
	}

	private int countOccurrences(String text, String target) {
		int count = 0;
		int index = 0;
		while ((index = text.indexOf(target, index)) >= 0) {
			count++;
			index += target.length();
		}
		return count;
	}
}
