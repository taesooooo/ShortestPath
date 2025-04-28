package com.sortestpath.sortestpath.core.pathengine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.comparator.Comparators;

import com.sortestpath.sortestpath.repository.MapRepository;
import com.sortestpath.sortestpath.util.PathUtil;

import lombok.Getter;

public class Engine {
	private static final Logger log = LoggerFactory.getLogger(Engine.class);
	
	@Getter
	private Graph graph;
	private Loader loader;
	private STRtree stRtree;
	private final DataProvider dataProvider;
//	private PriorityQueue<Node> openList;
//	private HashSet<Node> closeList;
//	private HashMap<Node, Node> location;
		
	public Engine(Loader loader, DataProvider dataProvider) throws IOException {
		if(loader == null) {
			throw new IllegalArgumentException("경로 탐색 엔진 초기화를 실패했습니다. 로더가 null입니다..");
		}
		
		if(dataProvider == null) {
			throw new IllegalArgumentException("경로 탐색 엔진 초기화를 실패했습니다. DataProvider가 null입니다.");
		}
		
		this.loader = loader;
		this.dataProvider = dataProvider;
		try {
			log.info("지도 링크 데이터 로드 시작");
	        this.graph = this.loader.loadData();
	        this.loader.dispose();
	    } catch (IOException e) {
	        log.error("로드 중 오류 발생: {}", e.getMessage(), e);
	        throw e; // 예외를 다시 던져서 상위에서 처리하도록 함
	    }
		log.info("엔진 초기화 완료");
	}
	
	/**
	 * 그래프에 있는 노드를 이용하여 경로를 탐색하여 리스트로 반환합니다.
	 * @param startNode
	 * @param endNode
	 * @return 탐색된 최단 경로 리스트
	 * @throws NullPointerException
	 */
	public ArrayList<Node> shortestPathFind(Node startNode, Node endNode) throws NullPointerException {
		if(startNode == null || endNode == null) {
			throw new NullPointerException("탐색에 필요한 노드가 없습니다.");
		}
		
		return findPath(startNode, endNode);
	}
	
	/**
	 * 좌표를 이용하여 경로를 탐색하고 탐색 경로를 리스트로 반환합니다.
	 * 주어진 좌표가 그래프에 없는 좌표 또는 이어진 좌표가 아니라면 주어진 좌표에서
	 * 가까운 라인의 좌표가 생성되어 경로를 탐색합니다.
	 * @param startCoordinate
	 * @param endCoordinate
	 * @return 탐색된 최단 경로 리스트
	 */
	public ArrayList<Node> shortestPathFind(Coordinate startCoordinate, Coordinate endCoordinate) {
		Node startNode = graph.getNode(startCoordinate);
		Node endNode = graph.getNode(endCoordinate);
		Coordinate startNearestPoint = null;
		Coordinate endNearestPoint = null;
		
		if(startNode == null) {
			// 가까운 라인의 시작 과 끝 좌표를 가져온다.
			Coordinate[] linePoints = findNearestLineCoordinate(startCoordinate);
			startNearestPoint = calculateNearestPointOnLine(linePoints[0], linePoints[1], startCoordinate); 
			startNode = findNearestNode(linePoints, startNearestPoint);
		}
		
		if(endNode == null) {
			// 가까운 라인의 시작 과 끝 좌표를 가져온다.
			Coordinate[] linePoints = findNearestLineCoordinate(endCoordinate);
			endNearestPoint = calculateNearestPointOnLine(linePoints[0], linePoints[1], endCoordinate); 
			endNode = findNearestNode(linePoints, endNearestPoint);
		}
		
		ArrayList<Node> resultPath = findPath(startNode, endNode);
		
		if(startNearestPoint != null) {
			Node node = new Node();
			node.setCoordinate(startNearestPoint);
			resultPath.add(0, node);
		}
		
		if(endNearestPoint != null) {
			Node node = new Node();
			node.setCoordinate(endNearestPoint);
			resultPath.add(node);
		}
		
		return resultPath;
	}
	
	/**
	 * 좌표 배열에서 목표 좌표에 가장 가까운 좌표를 찾고 찾은 좌표를 이용해 그래프에서 좌표에 해당하는 노드를 찾아 반환합니다.
	 * 가장 가까운 좌표인지 비교하는 방법은 유클리드 거리 공식을 이용하여 배열에 있는 좌표들을 모두 비교합니다.
	 * @param coordinateArray
	 * @param targetCoordinate
	 * @return 그래프에서 노드를 찾아 반환
	 */
	private Node findNearestNode(Coordinate[] coordinateArray, Coordinate targetCoordinate) {
		double minDistance = Double.MAX_VALUE;
		Coordinate minCoordinate = null;
		
		for(Coordinate coordinate : coordinateArray) {
			double distance = coordinate.calculateDistanceToTarget(targetCoordinate);
			if(distance < minDistance) {
				minCoordinate = coordinate;
				minDistance = distance; 
			}
		}
		
		return graph.getNode(minCoordinate);
	}
	
	private ArrayList<Node> findPath(Node startNode, Node endNode) {
		PriorityQueue<Cost> openList = new PriorityQueue<Cost>(Comparator.comparingDouble(c -> c.getFCost()));
		HashSet<Cost> closeList = new HashSet<Cost>();
		HashMap<Node, Node> location = new HashMap<Node, Node>();
		HashMap<Node, Cost> costList = new HashMap<Node, Cost>();
		
		//		for(Node node : graph.getAllNodes()) {  
		//		node.calculateHeuristic(endNode);  // 목적지로 휴리스틱 선계산
		//	}

		// AStar
		double heuristic = PathUtil.haversine(startNode.getCoordinate(), endNode.getCoordinate());
		Cost startCost = new Cost(startNode, 0, heuristic, 0 + heuristic);
		openList.add(startCost);
		costList.put(startNode, startCost);

		while(!openList.isEmpty()) {
			Cost minNode = openList.poll();
			Cost minNodeCost = costList.get(minNode.getNode());

			if(minNode.getNode().equals(endNode)) {
				log.info("경로 탐색 종료");
				break;
			}

			closeList.add(minNode);

			for(Edge edge : minNode.getNode().getEdge().values()) {
				Cost toCost = costList.get(edge.getTo());
				if(toCost == null) {
					toCost = new Cost(edge.getTo(), Double.MAX_VALUE, 0, 0);
					costList.put(edge.getTo(), toCost);
				}
				
				if(closeList.contains(toCost)) {
					continue;
				}
				
				
				double newDist = minNodeCost.getGCost() + edge.getDistance();
				if(!openList.contains(toCost) && newDist < toCost.getGCost()) {
					double hCost = PathUtil.haversine(edge.getTo().getCoordinate(), endNode.getCoordinate());
					double fCost = newDist + hCost;
					Cost c = new Cost(edge.getTo(), newDist, hCost, fCost);
					costList.put(edge.getTo(), c);
					
					openList.add(c);
					location.put(edge.getTo(), minNode.getNode());
				}
			}
		}
		
		// 탐색 결과 역추적
		ArrayList<Node> path = new ArrayList<Node>();
		Node node = location.get(endNode);
		while(node != null) {
			path.add(node);
			node = location.get(node);
		}
		
		Collections.reverse(path);
		
		// 마지막 좌표 추가
		path.add(endNode);
		
		return path;
	}
	
	/**
	 * 주어진 좌표와 거리가 가까운 라인의 시작과 끝 좌표를 찾아 반환합니다.
	 * @param coordinate
	 * @return 배열 첫 번째는 시작 좌표 마지막 배열 값은 마지막 좌표
	 */
	private Coordinate[] findNearestLineCoordinate(Coordinate coordinate) {
		org.locationtech.jts.geom.Coordinate convertCoordinate = new org.locationtech.jts.geom.Coordinate(coordinate.getLongitude(), coordinate.getLatitude());
		Point point = new GeometryFactory().createPoint(convertCoordinate);

		List<Geometry> geoList = dataProvider.findNearestLine(point.getX(), point.getY(), 0.001);
		
		if(geoList.isEmpty()) {
			throw new EmptyGeometryListException("지오메트리 리스트가 비어있습니다. 데이터베이스 또는 DataProvider를 확인해주세요.");
		}
		
		// 후보 라인 중 거리가 제일 가까운 라인 검색
		Geometry nearestGeoLine = null;
		double minDistance = Double.MAX_VALUE;
		
		for(Geometry geo : geoList) {
			double distance = geo.distance(point);
			if(distance < minDistance) {
				minDistance = distance;
				nearestGeoLine = geo;
			}
		}

		org.locationtech.jts.geom.Coordinate[] lines = nearestGeoLine.getCoordinates();
		
		if(nearestGeoLine.getNumPoints() > 2) {
			for(int i=0;i< nearestGeoLine.getNumPoints();i++) {
				for(int j=0;j< nearestGeoLine.getNumPoints() - 1 - i; j++) {
					if(lines[j].distance(convertCoordinate) > lines[j + 1].distance(convertCoordinate)) {
						org.locationtech.jts.geom.Coordinate temp = lines[j];
						lines[j] = lines[j + 1];
						lines[j + 1] = temp;
					}
				}
			}
		}

		org.locationtech.jts.geom.Coordinate start = lines[0];
		org.locationtech.jts.geom.Coordinate end = lines[1];
		
		return new Coordinate[] {new Coordinate(start.y, start.x), new Coordinate(end.y, end.x)};
	}
	
	/**
	 * 주어진 좌표의 라인과 주어진 점의 직선이 교차하는 점을 계산하여 반환합니다.
	 * @param a 라인의 첫 번째 좌표
	 * @param b 라인의 두 번째 좌표
	 * @param c 주어진 점
	 * @return 라인 과 주어진 점의 직선이 교차하는 점
	 */
	private Coordinate calculateNearestPointOnLine(Coordinate a, Coordinate b, Coordinate c) {
		double cax = (c.getLongitude() - a.getLongitude());
		double bax = (b.getLongitude() - a.getLongitude());
		double cay = (c.getLatitude() - a.getLatitude());
		double bay = (b.getLatitude() - a.getLatitude());
		
		double t = ((cax * bax) + (cay * bay)) / (Math.pow(b.getLongitude() - a.getLongitude(), 2) + Math.pow(b.getLatitude() - a.getLatitude(), 2));
		
		double x = a.getLongitude() + t * (b.getLongitude() - a.getLongitude());
		double y = a.getLatitude() + t * (b.getLatitude() - a.getLatitude());
		
		
		return new Coordinate(y, x);
	}
}
