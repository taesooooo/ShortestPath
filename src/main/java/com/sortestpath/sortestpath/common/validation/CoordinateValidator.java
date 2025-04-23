package com.sortestpath.sortestpath.common.validation;

import com.sortestpath.sortestpath.core.pathengine.Coordinate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CoordinateValidator implements ConstraintValidator<ValidCoordinate, Coordinate> {
	
	@Override
	public boolean isValid(Coordinate value, ConstraintValidatorContext context) {
		if(value == null) {
			return false;
		}
		
		return value.getLatitude() >= 33 && value.getLatitude() <= 43 &&
				value.getLongitude() >= 124 && value.getLongitude() <= 132;
	}
	

}
