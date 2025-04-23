package com.sortestpath.sortestpath;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sortestpath.sortestpath.core.pathengine.Coordinate;
import com.sortestpath.sortestpath.core.pathengine.Node;
import com.sortestpath.sortestpath.dto.request.RequestFindPathDto;
import com.sortestpath.sortestpath.dto.response.ResponseFindPathDto;
import com.sortestpath.sortestpath.service.MapService;
import com.sortestpath.sortestpath.service.MapServiceImpl;

class MapServiceTest {
	@InjectMocks
	private MapServiceImpl mapService;

	@BeforeEach
	void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void findPathTest() {
		RequestFindPathDto testDto = RequestFindPathDto.builder()
				.startCoordinate(new Coordinate(33.2417782, 126.5647375))
				.endCoordinate(new Coordinate(33.2387792, 126.6015835))
				.build();
		
		ResponseFindPathDto result = mapService.findPath(testDto);
		
		assertThat(result.getPaths()).isNotEmpty();
	}

}
