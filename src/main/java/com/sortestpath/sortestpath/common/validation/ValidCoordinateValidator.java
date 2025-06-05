package com.sortestpath.sortestpath.common.validation;

import java.util.List;
import java.util.regex.PatternSyntaxException;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidCoordinateValidator implements ConstraintValidator<ValidCoordinate, String> {
	
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if(value == null|| value.isEmpty() || value.isBlank()) {
			return false;
		}
		
		return validCoordinate(value);
	}
	
	private boolean validCoordinate(String value) {
		boolean result = false;
		
		try {
			String[] splitStr = value.split("/");
			
			double y = Double.parseDouble(splitStr[0]);
			double x = Double.parseDouble(splitStr[1]);
			
			result = (y >= 33 && y <= 43 && x >= 124 && x <= 132);
		}
		catch (PatternSyntaxException e) {
			return false;
		}
		catch (NumberFormatException e) {
			return false;
		}
		
		return result;
	}
}
