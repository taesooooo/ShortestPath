package com.sortestpath.sortestpath.common.validation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = ValidCoordinateValidator.class)
@Retention(RUNTIME)
@Target(ElementType.FIELD)
public @interface ValidCoordinate {
	String message() default "좌표값이 유효하지 않습니다.";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}