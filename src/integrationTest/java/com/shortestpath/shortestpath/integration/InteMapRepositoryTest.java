package com.shortestpath.shortestpath.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.shortestpath.shortestpath.entity.GeoLink;
import com.shortestpath.shortestpath.repository.MapRepository;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InteMapRepositoryTest {
	private static final Logger log = LoggerFactory.getLogger(InteMapRepositoryTest.class);

	@Autowired
	private MapRepository mapRepository;

	@BeforeEach
	void setUp() throws Exception {
	}
	
	@Test
	@DisplayName("주어진 좌표에서 가까운 라인 가져오기")
	void findNearestLineTest() {
		// 126.4898217 33.4824388
		List<GeoLink> list = mapRepository.findNearestLine(126.5624673, 33.2403307, 0.001);

		assertThat(list)
				.as("가까운 라인을 찾지 못했습니다.")
				.isNotEmpty()
				.extracting(GeoLink::getOsmId)
				.contains("75606417", "232438708", "232439144", "232439144", "232439145", "280266099", "292864982",
						"292864994", "292864996", "292864999", "292865000", "292865003", "369770453", "375861208", "375861209",
						"375861209");
	}
}
