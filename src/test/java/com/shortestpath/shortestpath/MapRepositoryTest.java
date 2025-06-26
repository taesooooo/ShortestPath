package com.shortestpath.shortestpath;

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

import com.shortestpath.shortestpath.repository.MapRepository;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MapRepositoryTest {
	private static final Logger log = LoggerFactory.getLogger(MapRepositoryTest.class);

	@Autowired
	private MapRepository mapRepository;

	@BeforeEach
	void setUp() throws Exception {
	}
	
	@Test
	@DisplayName("주어진 좌표에서 가까운 라인 가져오기")
	void findNearestLineTest() {
		//126.4898217 33.4824388
		List<Geometry> list = mapRepository.findNearestLine(126.4898217, 33.4824388, 0.001);
		
		assertThat(list).isNotEmpty();
//		assertThat(list.stream().map(item -> item.getOgrFID()).toList())
//		.containsExactly(489,533,521,23,146,218,202,56,90,102,195,101,62,73,225,199);
		
//		list.forEach(item -> log.info(item.getShape().toText()));
	}
}
