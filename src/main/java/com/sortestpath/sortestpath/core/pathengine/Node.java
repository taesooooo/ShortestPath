package com.sortestpath.sortestpath.core.pathengine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.sortestpath.sortestpath.util.DistanceUtil;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class Node implements Comparable<Node> {
	private int id;
	private String category;
	private Coordinate coordinate;
	private Map<Integer, Edge> edge = new HashMap<Integer, Edge>();
	private double gCost = Double.MAX_VALUE;
	private double hCost;
	private double fCost;
	
	/**
	 * 휴리스틱을 계산하면 fCost까지 모두 갱신됨
	 **/
	public void calculateHeuristic(Node endNode) {
		// 맨하튼 거리 공식
//		Coordinate currentNode = this.getCoordinate();
//		
//		double dx = Math.abs(currentNode.getLongitude() - endNode.getCoordinate().getLongitude());
//        double dy = Math.abs(currentNode.getLatitude() - endNode.getCoordinate().getLatitude());
//        double newDistance = dx + dy;
        
        // 하버사인 거리 공식
        Coordinate startPoint = this.getCoordinate();
        Coordinate endPoint = endNode.getCoordinate();

        double newDistance = DistanceUtil.haversine(startPoint, endPoint);
        
		this.hCost = newDistance;
		
		this.fCost = gCost + hCost;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(coordinate);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Node other = (Node) obj;
		return Objects.equals(coordinate, other.coordinate);
	}

	@Override
	public int compareTo(Node o) {
		return Double.compare(fCost, o.getFCost());
	}
	
	
}
