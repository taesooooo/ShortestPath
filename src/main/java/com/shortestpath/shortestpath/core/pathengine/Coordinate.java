package com.shortestpath.shortestpath.core.pathengine;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Coordinate {
	private double latitude;
	private double longitude;
	
	public Coordinate(String coordinate) {
		String[] splitStr = coordinate.split(",");
		
		double y = Double.parseDouble(splitStr[0]);
		double x = Double.parseDouble(splitStr[1]);

		this.latitude = y;
		this.longitude = x;
	}
	
	public double calculateDistanceToTarget(Coordinate coordinate) {
		double dx = Math.pow(this.latitude - coordinate.getLatitude(), 2);
		double dy = Math.pow(this.longitude - coordinate.getLongitude(), 2);
		
		return Math.sqrt(dx + dy);
	}
	
	public String toWKT() {
		return "POINT(" + longitude + " " + latitude + ")";
	}

	@Override
	public int hashCode() {
		int latHash = (int)(latitude * 1e7);  // 소수점 7자리까지만 해시 처리
		int lonHash = (int)(longitude * 1e7);
		return Objects.hash(latHash, lonHash);
	}
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Coordinate other = (Coordinate) obj;
		double epsilon = 1e-7;  // 소수점 7자리까지 동일하면 같은 좌표 취급
		return Math.abs(this.latitude - other.latitude) < epsilon
			&& Math.abs(this.longitude - other.longitude) < epsilon;
	}
}
