package com.shortestpath.shortestpath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.shortestpath.shortestpath.service.MapServiceImpl;

class MapServiceTest {
	@InjectMocks
	private MapServiceImpl mapService;

	@BeforeEach
	void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void findPathTest() {
//		List<RouteDto> testCoordiateList = List.of(RouteDto.builder()
//				.start(new Coordinate("33.2417782, 126.5647375"))
//				.end(new Coordinate("33.2387792, 126.6015835"))
//				.build());
//	
//		ResponseFindPathDto result = mapService.findPath(testCoordiateList);
//		
//		assertThat(result.getRouteDto()).isNotEmpty();
	}

}
