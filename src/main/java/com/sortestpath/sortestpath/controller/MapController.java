package com.sortestpath.sortestpath.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sortestpath.sortestpath.dto.request.RequestFindPathDto;
import com.sortestpath.sortestpath.service.MapService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/map")
@RequiredArgsConstructor
public class MapController {
	private final MapService mapService;

	@GetMapping("/find-path")
	public ResponseEntity<Object> findPath(@Valid @RequestBody RequestFindPathDto requestDto) {
		return ResponseEntity.ok().body(mapService.findPath(requestDto));
	}
}
