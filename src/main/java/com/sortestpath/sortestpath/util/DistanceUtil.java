package com.sortestpath.sortestpath.util;

import com.sortestpath.sortestpath.core.pathengine.Coordinate;

public class DistanceUtil {
	public static Double haversine(Coordinate a, Coordinate b) {
		// 하버사인 거리 공식
        Coordinate startPoint = a;
        Coordinate endPoint = b;

        double r = 6371; // 지구 반지름
        double deltaLon = Math.toRadians(endPoint.getLongitude()) - Math.toRadians(startPoint.getLongitude());
        double deltaLat = Math.toRadians(endPoint.getLatitude()) - Math.toRadians(startPoint.getLatitude());
        double sinLon = Math.sin(deltaLon/2);
        double sinLat = Math.sin(deltaLat/2);
        double middleResult = Math.sqrt((sinLat * sinLat) + Math.cos(startPoint.getLatitude()) * Math.cos(endPoint.getLatitude()) * (sinLon * sinLon));
        double newDistance = 2 * r * Math.asin(middleResult);
        
        return newDistance;
	}
	
	public static Double cal2(Coordinate a, Coordinate b) {
		// 맨하튼 거리 공식
		Coordinate currentNode = a;
		
		double dx = Math.abs(currentNode.getLongitude() - b.getLongitude());
        double dy = Math.abs(currentNode.getLatitude() - b.getLatitude());
        double newDistance = dx + dy;
        
        return newDistance;
	}
}
