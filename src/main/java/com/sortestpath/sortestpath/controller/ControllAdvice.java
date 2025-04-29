package com.sortestpath.sortestpath.controller;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControllAdvice {

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		// get 메서드에서 dto 매핑할 때 Setter를 사용하지 않기 위함
		binder.initDirectFieldAccess();
	}
}
