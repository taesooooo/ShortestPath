package com.sortestpath.sortestpath.core.pathengine;

import java.util.Collection;
import java.util.HashMap;

import com.sortestpath.sortestpath.util.PathUtil;


public class Graph {
	private HashMap<Coordinate, Node> graph;
	
	public Graph() {
		graph = new HashMap<Coordinate, Node>();
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
	public void addEdge(Node startNode, Node endNode) {
		double distance = PathUtil.haversine(startNode.getCoordinate(), endNode.getCoordinate());
		graph.get(startNode.getCoordinate()).getEdge().put(endNode.getId(), new Edge(endNode, distance));
		graph.get(endNode.getCoordinate()).getEdge().put(startNode.getId(), new Edge(startNode, distance));
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
	
	public void printAll() {
		for(Coordinate c : graph.keySet()) {
			System.out.print("[" +  c.getLatitude() + " " + c.getLongitude() + "]");
			for(Edge e : graph.get(c).getEdge().values()) {
				System.out.print(e.getTo().getCoordinate().getLatitude() + " " + e.getTo().getCoordinate().getLongitude() + " ");
			}
			System.out.println("");
		}
	}
	
	public void printMoveTo(Node checkNode) {
		System.out.println("EndNode로 향하는 연결 목록: ");
		for (Node node : graph.values()) {
		    for (Edge edge : node.getEdge().values()) {
		        if (edge.getTo().equals(checkNode)) {
		            System.out.println("연결: " + node.getCoordinate() + " -> " + checkNode.getCoordinate() + " / 거리: " + edge.getDistance());
		        }
		    }
		}
	}
}
