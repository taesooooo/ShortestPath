package com.shortestpath.shortestpath.controller;

import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shortestpath.shortestpath.Exception.InvalidCoordinate;
import com.shortestpath.shortestpath.core.pathengine.Coordinate;
import com.shortestpath.shortestpath.dto.request.RequestFindPathDto;
import com.shortestpath.shortestpath.service.MapService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {
	private final MapService mapService;

	@GetMapping("/find-path")
	public ResponseEntity<Object> findPath(@RequestParam("coordinates") List<String> list) {
		List<RequestFindPathDto> coordinateList = list.stream().map(item -> {
			boolean check = validCoordinate(item);
			if(!check) {
				throw new InvalidCoordinate("잘못된 좌표 리스트 형식입니다.");
			}
			
			String[] str = item.split("\\|");
			if(str.length != 2) {
				throw new InvalidCoordinate("잘못된 좌표 리스트 형식입니다.");
			}
			
			String[] startString = str[0].split("/");
			String[] endString = str[1].split("/");
			
			Coordinate start = new Coordinate(Double.parseDouble(startString[0]), Double.parseDouble(startString[1]));
			Coordinate end = new Coordinate(Double.parseDouble(endString[0]), Double.parseDouble(endString[1]));
			
			RequestFindPathDto routeDto = RequestFindPathDto.builder().start(start).end(end).build();
			return routeDto;
		}).toList();

		return ResponseEntity.ok().body(mapService.findPath(coordinateList));
	}
	
	private boolean validCoordinate(String value) {
		String[] str = value.split("\\|");
		
		for(String token : str) {
			try {
				String[] splitStr = token.split("/");
				
				double y = Double.parseDouble(splitStr[0]);
				double x = Double.parseDouble(splitStr[1]);
				
				return (y >= 33 && y <= 43 && x >= 124 && x <= 132);
			}
			catch (PatternSyntaxException e) {
				return false;
			}
			catch (NumberFormatException e) {
				return false;
			}
		}
		
		return false;
	}
}
