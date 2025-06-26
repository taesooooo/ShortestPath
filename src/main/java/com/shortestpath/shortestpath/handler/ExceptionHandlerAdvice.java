package com.shortestpath.shortestpath.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.shortestpath.shortestpath.Exception.InvalidCoordinate;

@RestControllerAdvice
public class ExceptionHandlerAdvice {	
	private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);
	
	@ExceptionHandler(exception = Exception.class)
	public ResponseEntity<Object> exception(Exception e) {
		log.error("서버 에러: " + e.getMessage(), e);
		e.getStackTrace();
		return ResponseEntity.status(HttpStatus.CONFLICT).body("문제가 발생했습니다. 문제가 계속 된다면 개발자에게 문의 해주세요");
	}
	
	@ExceptionHandler(exception = MethodArgumentNotValidException.class)
	public ResponseEntity<Object> inValidException(Exception e, BindingResult result) {
		log.info("유효성 검사 실패: " + e.getMessage(), e);
		e.getStackTrace();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getFieldError().getDefaultMessage());
	}
	
	@ExceptionHandler(exception = InvalidCoordinate.class)
	public ResponseEntity<Object> inValidCoordinate(Exception e) {
		log.info("유효성 검사 실패: " + e.getMessage(), e);
		e.getStackTrace();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
	}

}
