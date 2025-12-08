package com.shortestpath.shortestpath.core.pathengine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.entity.GeoLink;
import com.shortestpath.shortestpath.util.PathUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Engine {
	private DataStore store;
	private final DataProvider dataProvider;
		
	public Engine(DataStore store, DataProvider dataProvider) throws IOException {
		if(store == null) {
			throw new IllegalArgumentException("경로 탐색 엔진 초기화를 실패했습니다. DataStore가 null입니다..");
		}
		
		if(dataProvider == null) {
			throw new IllegalArgumentException("경로 탐색 엔진 초기화를 실패했습니다. DataProvider가 null입니다.");
		}
		
		this.store = store;
		this.dataProvider = dataProvider;

		log.info("엔진 초기화 완료");
	}
	
	/**
	 * 저장소에 있는 노드를 이용하여 경로를 탐색하여 리스트로 반환합니다.
	 * @param startNode
	 * @param endNode
	 * @return 탐색된 최단 경로 리스트, null은 연결된 노드가 없어 탐색이 불가능한 경우
	 * @throws NullPointerException
	 * @throws IOException 
	 */
	public ArrayList<Node> shortestPathFind(Node startNode, Node endNode) throws NullPointerException, IOException {
		if(startNode == null || endNode == null) {
			throw new NullPointerException("탐색에 필요한 노드가 없습니다.");
		}
		
		return findPath(startNode, endNode);
	}
	
	/**
	 * 좌표를 이용하여 경로를 탐색하고 탐색 경로를 리스트로 반환합니다.
	 * 주어진 좌표가 저장소에 없는 좌표 또는 이어진 좌표가 아니라면 주어진 좌표에서
	 * 가까운 라인의 좌표가 생성되어 경로를 탐색합니다.
	 * @param startCoordinate
	 * @param endCoordinate
	 * @return 탐색된 최단 경로 리스트, null은 연결된 노드가 없어 탐색이 불가능한 경우
	 * @throws IOException 
	 */
	public ArrayList<Node> shortestPathFind(Coordinate startCoordinate, Coordinate endCoordinate) throws IOException, EmptyGeometryListException {
		Node startNode = null;
		Node endNode = null;
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
		long st = System.currentTimeMillis();
		
		ArrayList<Node> resultPath = findPath(startNode, endNode);
		
		long et = System.currentTimeMillis();
		
		log.info("탐색 완료 시간 - " + (et-st) / 1000.0);
		
		return resultPath;
	}
	
	/**
	 * 좌표 배열에서 목표 좌표에 가장 가까운 좌표를 찾고 찾은 좌표를 이용해 그래프에서 좌표에 해당하는 노드를 찾아 반환합니다.
	 * 가장 가까운 좌표인지 비교하는 방법은 유클리드 거리 공식을 이용하여 배열에 있는 좌표들을 모두 비교합니다.
	 * @param coordinateArray
	 * @param targetCoordinate
	 * @return 저장소에서 노드를 찾아 반환
	 * @throws IOException 
	 */
	private Node findNearestNode(Coordinate[] coordinateArray, Coordinate targetCoordinate) throws IOException {
		double minDistance = Double.MAX_VALUE;
		Coordinate minCoordinate = null;
		
		for(Coordinate coordinate : coordinateArray) {
			double distance = coordinate.calculateDistanceToTarget(targetCoordinate);
			if(distance < minDistance) {
				minCoordinate = coordinate;
				minDistance = distance; 
			}
		}
		
		int offset = store.getNodeOffset(minCoordinate);

		return store.readNode(offset);
	}
	
	/**
	 * A* 알고리즘을 이용해 시작 노드에서 종료 노드까지의 최단 경로를 탐색합니다.
	 * 각 노드의 gCost(시작점부터 현재 노드까지의 거리), hCost(목적지까지의 휴리스틱, 하버사인 거리), fCost(gCost + hCost)를 계산하여
	 * 우선순위 큐를 사용해 가장 fCost가 낮은 노드를 우선적으로 탐색합니다.
	 * 
	 * @param startNode 출발 노드
	 * @param endNode 도착 노드
	 * @return 최단 경로에 포함된 노드 리스트(순서대로)
	 * @throws IOException 
	 */
	private ArrayList<Node> findPath(Node startNode, Node endNode) throws IOException {
	    // fCost(=gCost+hCost)가 가장 낮은 노드를 우선적으로 꺼내는 우선순위 큐
	    PriorityQueue<Node> openList = new PriorityQueue<Node>(Comparator.comparingDouble(c -> c.getFCost()));
	    // 이미 방문한 노드 집합
	    HashSet<Node> closeList = new HashSet<Node>();
	    // 각 노드의 이전 노드를 저장(경로 역추적용)
	    HashMap<Node, Node> location = new HashMap<Node, Node>();

		// 노드와 엣지 캐싱 맵
		HashMap<Integer, Node> nodeList = new HashMap<>();
		HashMap<Integer, Edge> edgeList = new HashMap<>();

	    // 시작 노드의 휴리스틱(목적지까지의 하버사인 거리) 계산
	    double heuristic = PathUtil.haversine(startNode.getCoordinate(), endNode.getCoordinate());

		// 첫 시작 노드 gCost = 0 설정
		startNode.setGCost(0);
		startNode.setHCost(heuristic);
		startNode.setFCost(heuristic);
		nodeList.put(startNode.getId(), startNode);
		// 첫 시작 노드를 추가
	    openList.add(startNode);
	    // costList.put(startNode, startCost);

	    // A* 탐색 루프
	    while(!openList.isEmpty()) {
	        // fCost가 가장 낮은 노드를 꺼냄
	        Node minNode = openList.poll();
	        // Cost minNodeCost = costList.get(minNode.getNode());

	        // 도착 노드에 도달하면 탐색 종료
	        if(minNode.equals(endNode)) {
				break;
	        }

	        // 현재 노드를 closeList에 추가
	        closeList.add(minNode);

	        // 현재 노드에 연결된 모든 이웃 노드(엣지) 탐색
	        for(Edge edge : getConnectedEdges(edgeList, minNode)) {
				Node storeNode = store.readNode(edge.getTo());
				Node listNode = nodeList.get(storeNode.getId());
				if(listNode == null) {
					nodeList.put(storeNode.getId(), storeNode);
				}
				Node toNode = listNode != null ? listNode : storeNode;
	            // 이미 방문한 노드는 건너뜀
	            if(closeList.contains(toNode)) {
	                continue;
	            }

	            // 새로운 gCost(시작점부터 이웃 노드까지의 누적 거리) 계산
	            double newDist = minNode.getGCost() + edge.getDistance();
	            // openList에 없고, 더 짧은 경로라면 갱신
	            if(!openList.contains(toNode) && newDist < toNode.getGCost()) {
	                // hCost(이웃 노드에서 목적지까지의 하버사인 거리) 계산
	                double hCost = PathUtil.haversine(toNode.getCoordinate(), endNode.getCoordinate());
	                double fCost = newDist + hCost;
					toNode.setHCost(hCost);
					toNode.setGCost(newDist);
					toNode.setFCost(fCost);

	                openList.add(toNode);
	                // 경로 역추적을 위해 이전 노드 저장
	                location.put(toNode, minNode);
	            }
	        }
	    }

	    // 탐색 결과를 역추적하여 경로 리스트 생성
	    ArrayList<Node> path = new ArrayList<Node>();
	    Node node = location.get(endNode);
	    while(node != null) {
	        path.add(node);
	        node = location.get(node);
	    }

	    // 경로를 올바른 순서로 뒤집음
	    Collections.reverse(path);

		path.add(endNode);

		if(path.isEmpty() || path.get(0) != startNode) {
			// 연결된 노드가 없는 경우
			return null;
		}

	    return path;
	}


	/**
	 * 노드와 연결된 엣지를 모두 반환합니다. edgeList에 없는 엣지는 store에서 읽어와 추가합니다.
	 * @param Node
	 * @return List<Edge>
	 * @throws IOException 
	 */
	private List<Edge> getConnectedEdges(HashMap<Integer,Edge> edgeList, Node node) throws IOException {
		ArrayList<Edge> edges = new ArrayList<Edge>();
		
		Edge storeEdge = store.readEdge(node.getStartEdgeOffset());
		Edge listEdge = edgeList.get(storeEdge.getId());
		if(listEdge == null) {
			edgeList.put(storeEdge.getId(), storeEdge);
		}
		edges.add(storeEdge);

		while(edges.get(edges.size() - 1).getNextEdgeOffset() != -1) {
			storeEdge = store.readEdge(edges.get(edges.size() - 1).getNextEdgeOffset());
			listEdge = edgeList.get(storeEdge.getId());
			if(listEdge == null) {
				edgeList.put(storeEdge.getId(), storeEdge);
			}
			edges.add(storeEdge);
		}
		
		return edges;
	}
	
	/**
	 * 주어진 좌표와 거리가 가까운 라인의 시작과 끝 좌표를 찾아 반환합니다.
	 * @param coordinate
	 * @return 배열 첫 번째는 시작 좌표 마지막 배열 값은 마지막 좌표
	 */
	private Coordinate[] findNearestLineCoordinate(Coordinate coordinate) {
		org.locationtech.jts.geom.Coordinate convertCoordinate = new org.locationtech.jts.geom.Coordinate(coordinate.getLongitude(), coordinate.getLatitude());
		Point point = new GeometryFactory().createPoint(convertCoordinate);

// 		// 주어진 좌표에서 가까운 라인들을 가져온다.
		List<GeoLink> geoDataList = dataProvider.findNearestLine(point.getX(), point.getY(), 0.001);
		
		if(geoDataList.isEmpty()) {
			throw new EmptyGeometryListException("지오메트리 리스트가 비어있습니다. 데이터베이스 또는 DataProvider를 확인해주세요.");
		}

		List<Geometry> geoList = geoDataList.stream().map(item -> item.getShape()).toList();

		// 후보 라인 중 거리가 제일 가까운 라인 검색
		Geometry nearestLine = null;
		double minDistance = Double.MAX_VALUE;
		
		for(Geometry geo : geoList) {
			double distance = geo.distance(point);
			if(distance < minDistance) {
				minDistance = distance;
				nearestLine = geo;
			}
		}

		org.locationtech.jts.geom.Coordinate[] lines = nearestLine.getCoordinates();
		org.locationtech.jts.geom.Coordinate[] sortLines = null;
		org.locationtech.jts.geom.Coordinate start = null;
		org.locationtech.jts.geom.Coordinate end = null;
		
		// 만약 가까운 라인에 포인트가 두개 이상이라면 시작점과 끝점을 가져오고
		// 가까운 점 기준으로 정렬한 뒤 시작 노드와 끝 노드를 결정한다.
		// 요청 좌표와 가까운 점을 선택하기 위함
		if(nearestLine.getNumPoints() > 2) {
			sortLines = new org.locationtech.jts.geom.Coordinate[2];
			if(lines[0].distance(convertCoordinate) > lines[lines.length - 1].distance(convertCoordinate)) {
				sortLines[0] = lines[0];
				sortLines[1] = lines[lines.length - 1];
			}
			else {
				sortLines[0] = lines[lines.length - 1];
				sortLines[1] = lines[0];
			}
			
			start = sortLines[0];
			end = sortLines[1];
		}
		else {
			start = lines[0];
			end = lines[lines.length - 1];			
		}
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
