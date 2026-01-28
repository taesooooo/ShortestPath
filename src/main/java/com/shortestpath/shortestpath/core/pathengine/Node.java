package com.shortestpath.shortestpath.core.pathengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.shortestpath.shortestpath.core.pathengine.Util.PathUtil;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


public class Node implements Comparable<Node> {
	private int id;
	// private String category;
	private Coordinate coordinate;
	private int startEdgeOffset;
	// private Map<Integer, Edge> edge = new HashMap<Integer, Edge>();
	private double gCost = Double.MAX_VALUE;
	private double hCost;
	private double fCost;

	public Node(int id, Coordinate coordinate) {
		this.id = id;
		this.coordinate = coordinate;
	}

	public Node(int id, Coordinate coordinate, int startEdgeOffset, double gCost, double hCost, double fCost) {
		this.id = id;
		this.coordinate = coordinate;
		this.startEdgeOffset = startEdgeOffset;
		this.gCost = gCost;
		this.hCost = hCost;
		this.fCost = fCost;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	public void setCoordinate(Coordinate coordinate) {
		this.coordinate = coordinate;
	}

	public int getStartEdgeOffset() {
		return startEdgeOffset;
	}

	public void setStartEdgeOffset(int startEdgeOffset) {
		this.startEdgeOffset = startEdgeOffset;
	}

	public double getgCost() {
		return gCost;
	}

	public void setgCost(double gCost) {
		this.gCost = gCost;
	}

	public double gethCost() {
		return hCost;
	}

	public void sethCost(double hCost) {
		this.hCost = hCost;
	}

	public double getfCost() {
		return fCost;
	}

	public void setfCost(double fCost) {
		this.fCost = fCost;
	}

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
        Coordinate startPoint = this.coordinate;
        Coordinate endPoint = endNode.coordinate;

        double newDistance = PathUtil.haversineDistance(startPoint, endPoint);
        
		this.hCost = newDistance;
		
		this.fCost = gCost + hCost;
	}

	public List<Node> getAdjacentNodes() {
		// List<Node> adjacentNodes = new ArrayList<Node>();
		
		// for(Edge e : edge.values()) {
		// 	adjacentNodes.add(e.getTo());
		// }
		
		// return adjacentNodes;

		return null;
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
		return Double.compare(fCost, o.getfCost());
	}
	
}
