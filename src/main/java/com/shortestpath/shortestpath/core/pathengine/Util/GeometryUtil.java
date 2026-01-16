package com.shortestpath.shortestpath.core.pathengine.Util;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;

import com.shortestpath.shortestpath.core.pathengine.Coordinate;

public class GeometryUtil {

    public static Envelope createSearchEnvelope(Coordinate coordinate, double distance) {
		double latDiff = distance / 111111.0; // 위도 1도는 약 111km
		double lonDiff = distance / (111111.0 * Math.cos(Math.toRadians(coordinate.getLatitude()))); // 경도 1도는 위도에 따라 다름

		double minLat = coordinate.getLatitude() - latDiff;
		double maxLat = coordinate.getLatitude() + latDiff;
		double minLon = coordinate.getLongitude() - lonDiff;
		double maxLon = coordinate.getLongitude() + lonDiff;

		return new Envelope(minLon, maxLon, minLat, maxLat);
	}

    public static String toWkt(Envelope envelope) {
        GeometryFactory geometryFactory = new GeometryFactory();
        return new WKTWriter().write(geometryFactory.toGeometry(envelope));
    }

	public static long coordinateToLong(org.locationtech.jts.geom.Coordinate coordinate) {
		long lon = (long)(coordinate.getX() * 10000000);
		long lat = (long)(coordinate.getY() * 10000000);

		// 비트 패킹
		return (lon << 32) | (lat & 0xFFFFFFFFL);
	}
}