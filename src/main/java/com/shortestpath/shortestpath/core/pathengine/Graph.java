package com.shortestpath.shortestpath.core.pathengine;

import java.util.Collection;
import java.util.HashMap;

import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shortestpath.shortestpath.util.PathUtil;

import lombok.extern.log4j.Log4j;


public class Graph {
	private Logger logger = LoggerFactory.getLogger(getClass());

	private HashMap<Coordinate, Node> graph;
	
	public Graph() {
		graph = new HashMap<Coordinate, Node>();
	}

	public Graph(HashMap<Coordinate, Node> graph) {
		this.graph = graph;
	}

	public void addNode(Node node) {
		graph.putIfAbsent(node.getCoordinate(), node);
	}
	
	/**
	 * 하버사인 공식으로 시작 노드와 끝 노드의 거리를 계산값을 엣지에 같이 추가함
	 * 
	 * @param startNode
	 * @param endNode
	 */
	public void addEdge(Coordinate startCoordinate, Coordinate endCoordinate, Geometry geometry) {
		double distance = PathUtil.haversine(startCoordinate, endCoordinate);
		
		Node startNode = graph.get(startCoordinate);
		Node endNode = graph.get(endCoordinate);
		
		if(startNode == null) {
			throw new NullPointerException("해당 하는 좌표의 노드가 없습니다. - " + startCoordinate.toWKT());
		}
		
		if(endNode == null) {
			throw new NullPointerException("해당 하는 좌표의 노드가 없습니다. - " + endCoordinate.toWKT());
		}
		
		// startNode.getEdge().put(endNode.getId(), new Edge(endNode, distance, geometry));
		// endNode.getEdge().put(startNode.getId(), new Edge(startNode, distance, geometry));
	}
	
	public Node getNode(Coordinate coordinate) {
		return graph.get(coordinate);
	}
	
	public Node getNodeById(int id) {
		return graph.get(id);
	}
	
	public boolean containsKey(Coordinate coordinate) {
		return graph.containsKey(coordinate);
	}
	
	public Collection<Node> getAllNodes() {
		return graph.values();
	}

	public int size() {
		return graph.size();
	}
	
	// public void printAll() {
	// 	for(Coordinate c : graph.keySet()) {
	// 		System.out.print("[" +  c.getLatitude() + " " + c.getLongitude() + "]");
	// 		for(Edge e : graph.get(c).getEdge().values()) {
	// 			System.out.print(e.getTo().getCoordinate().getLatitude() + " " + e.getTo().getCoordinate().getLongitude() + " ");
	// 		}
	// 		System.out.println("");
	// 	}
	// }
	
	// public void printMoveTo(Node checkNode) {
	// 	System.out.println("EndNode로 향하는 연결 목록: ");
	// 	for (Node node : graph.values()) {
	// 	    for (Edge edge : node.getEdge().values()) {
	// 	        if (edge.getTo().equals(checkNode)) {
	// 	            System.out.println("연결: " + node.getCoordinate() + " -> " + checkNode.getCoordinate() + " / 거리: " + edge.getDistance());
	// 	        }
	// 	    }
	// 	}
	// }
}
