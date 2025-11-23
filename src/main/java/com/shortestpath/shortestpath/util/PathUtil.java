package com.shortestpath.shortestpath.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;

public class PathUtil {
	public static Coordinate roundCoordinate(Coordinate coordinate) {
		double lat = new BigDecimal(coordinate.getLatitude()).setScale(6, RoundingMode.HALF_UP).doubleValue();
		double lon = new BigDecimal(coordinate.getLongitude()).setScale(6, RoundingMode.HALF_UP).doubleValue();
		
		return new Coordinate(lat, lon);
	}
	
	public static Double haversine(Coordinate a, Coordinate b) {
		// 하버사인 거리 공식
        Coordinate startPoint = a;
        Coordinate endPoint = b;

        double r = 6371; // 지구 반지름

        double lat1 = Math.toRadians(startPoint.getLatitude());
        double lat2 = Math.toRadians(endPoint.getLatitude());
        double lon1 = Math.toRadians(startPoint.getLongitude());
        double lon2 = Math.toRadians(endPoint.getLongitude());
        double deltaLon = lon2 - lon1;
        double deltaLat = lat2 - lat1;
        double sinLon = Math.sin(deltaLon/2);
        double sinLat = Math.sin(deltaLat/2);
        double middleResult = Math.sqrt((sinLat * sinLat) + Math.cos(lat1) * Math.cos(lat2) * (sinLon * sinLon));
        double newDistance = 2 * r * Math.asin(middleResult);
        
        return newDistance;
	}
	
	public static Double Euclidean(Coordinate a, Coordinate b) {
		// 맨하튼 거리 공식
		Coordinate currentNode = a;
		
		double dx = Math.abs(currentNode.getLongitude() - b.getLongitude());
        double dy = Math.abs(currentNode.getLatitude() - b.getLatitude());
        double newDistance = dx + dy;
        
        return newDistance;
	}
}
