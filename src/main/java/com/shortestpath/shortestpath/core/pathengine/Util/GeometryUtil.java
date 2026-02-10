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

	public static org.locationtech.jts.geom.Coordinate longToCoordinate(long packed) {
		long lon = packed >> 32;
		long lat = packed & 0xFFFFFFFFL;
		
		// 부호 확장 (음수 처리)
		if ((lat & 0x80000000L) != 0) {
			lat = lat | 0xFFFFFFFF00000000L;
		}
		
		double x = lon / 10000000.0;
		double y = lat / 10000000.0;
		
		return new org.locationtech.jts.geom.Coordinate(x, y);
	}

	/**
	 * 좌표와 레이어 ID를 조합하여 유니크한 Long 값 생성
	 * 좌표 + 레이어 정보를 비트 패킹으로 조합
	 * 
	 * @param coordinate 지리 좌표
	 * @param layerId 레이어 ID (long 64비트, 음수 지원)
	 * @return 조합된 유니크 ID
	 */
	public static long coordinateAndLayerToLong(org.locationtech.jts.geom.Coordinate coordinate, Object layerId) {
		long coordinateId = coordinateToLong(coordinate);
		
		// layerId 처리 (null인 경우 0으로 설정, 음수값 지원)
		long layer = 0;
		if (layerId != null) {
			try {
				// 이미 Long이면 직접 사용, 아니면 문자열 파싱
				if (layerId instanceof Long) {
					layer = (Long) layerId;
				} else {
					layer = Long.parseLong(layerId.toString());
				}
			} catch (NumberFormatException e) {
				layer = 0;
			}
		}
		
		// 좌표와 레이어를 조합
		return combineCoordinateAndLayer(coordinateId, layer);
	}

	/**
	 * 좌표 ID와 레이어 ID를 비트 패킹으로 조합
	 * 상위 16비트: 레이어 ID의 해시 (음수 지원)
	 * 하위 48비트: 좌표 정보
	 * 
	 * @param coordinateId coordinateToLong에서 생성한 64비트 좌표 ID
	 * @param layerId 레이어 ID (64비트 long)
	 * @return 조합된 유니크 ID
	 */
	private static long combineCoordinateAndLayer(long coordinateId, long layerId) {
		// 레이어 ID의 해시값을 상위 16비트에 배치
		long layerHash = (layerId & 0xFFFFL); // 하위 16비트만 사용
		
		// 좌표의 상위 48비트와 레이어의 하위 16비트 조합
		return ((layerHash << 48) | (coordinateId & 0x0000FFFFFFFFFFFFL));
	}

	/**
	 * 레이어를 포함한 조합 ID를 다시 분해하여 좌표 획득
	 * 
	 * @param combined 조합된 ID
	 * @return 좌표
	 */
	public static org.locationtech.jts.geom.Coordinate longToCoordinateFromCombined(long combined) {
		// 하위 48비트 추출 후, 상위 32비트는 원래 대로 복구
		long coordinateId = (combined & 0x0000FFFFFFFFFFFFL) | ((combined & 0x0000FFFF00000000L) << 16);
		return longToCoordinate(coordinateId);
	}

	/**
	 * 조합 ID에서 레이어 ID 추출 (음수 값 지원)
	 * 
	 * @param combined 조합된 ID
	 * @return 레이어 ID (음수 포함)
	 */
	public static long getLayerFromCombined(long combined) {
		long layerValue = (combined >> 48) & 0xFFFFL;
		
		// 음수 값 부호 확장 처리
		if ((layerValue & 0x8000L) != 0) {
			layerValue = layerValue | 0xFFFFFFFFFFFF0000L;
		}
		
		return layerValue;
	}
}