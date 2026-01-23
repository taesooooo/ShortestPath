package com.shortestpath.shortestpath.core.pathengine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import org.locationtech.jts.geom.Envelope;

import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Util.PathUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Engine {
	private DataStore store;
	private NodeProvider dataProvider;
		
	public Engine(DataStore store, NodeProvider dataProvider) throws IOException {
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
	
	public DataStore getStore() {
		return store;
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
		
		return findPath(startNode, endNode, null);
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
	public RouteSearchResult shortestPathFind(Coordinate startCoordinate, Coordinate endCoordinate, boolean trackRoute) throws IOException, EmptyGeometryListException {
		Node startNode = null;
		Node endNode = null;
		Coordinate startNearestPoint = null;
		Coordinate endNearestPoint = null;
		
		if(startNode == null) {
			// 가까운 라인의 시작 과 끝 좌표를 가져온다.
			Node nearestNode = findNearestNode(startCoordinate);
			Edge nearestEdge = findNearestEdge(nearestNode, startCoordinate);
			Node fromNode = store.readNode(nearestEdge.getFrom() * DataStructureSizes.NODE_SIZE);
			Node toNode = store.readNode(nearestEdge.getTo() * DataStructureSizes.NODE_SIZE);
			startNearestPoint = calculateNearestPointOnLine(fromNode.getCoordinate(), toNode.getCoordinate(), startCoordinate);

			startNode = nearestNode;
		}
		
		if(endNode == null) {
			// 가까운 라인의 시작 과 끝 좌표를 가져온다.
			Node nearestNode = findNearestNode(endCoordinate);
			Edge nearestEdge = findNearestEdge(nearestNode, endCoordinate);
			Node fromNode = store.readNode(nearestEdge.getFrom() * DataStructureSizes.NODE_SIZE);
			Node toNode = store.readNode(nearestEdge.getTo() * DataStructureSizes.NODE_SIZE);
			endNearestPoint = calculateNearestPointOnLine(fromNode.getCoordinate(), toNode.getCoordinate(), endCoordinate); 
			// endNode = findNearestNode(fromNode, toNode, endNearestPoint);
			endNode = nearestNode;
		}
		log.info("노드 탐색 완료 / 경로 탐색 시작");
		long st = System.currentTimeMillis();
		
		RouteTracker routeTracker = null;
		if(trackRoute) {
			routeTracker = new RouteTracker();
		}

		ArrayList<Node> resultPath = findPath(startNode, endNode, routeTracker);
		
		long et = System.currentTimeMillis();
		double searchTime = (et - st) / 1000.0;
		
		log.info("탐색 완료 시간 - " + searchTime);
		
		RouteSearchResult result = new RouteSearchResult(resultPath, routeTracker, searchTime);
		
		return result;
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
	private ArrayList<Node> findPath(Node startNode, Node endNode, RouteTracker routeTracker) throws IOException {
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
	    double heuristic = PathUtil.haversineDistance(startNode.getCoordinate(), endNode.getCoordinate());

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
			
			TraceRoute traceRoute = null;
			if(routeTracker != null) {
				traceRoute = new TraceRoute(minNode.getCoordinate());
				routeTracker.addTraceRoute(traceRoute);
			}

	        // 도착 노드에 도달하면 탐색 종료
	        if(minNode.equals(endNode)) {
				break;
	        }
			
	        // 현재 노드를 closeList에 추가
	        closeList.add(minNode);
			
	        // 현재 노드에 연결된 모든 이웃 노드(엣지) 탐색
	        for(Edge edge : getConnectedEdges(edgeList, minNode)) {
				Node storeNode = store.readNode(edge.getTo() * DataStructureSizes.NODE_SIZE);
				Node listNode = nodeList.get(storeNode.getId());
				if(listNode == null) {
					nodeList.put(storeNode.getId(), storeNode);
				}
				
				Node toNode = listNode != null ? listNode : storeNode;
				
	            // 이미 방문한 노드는 건너뜀
	            if(closeList.contains(toNode)) {
					continue;
	            }
				
				if(routeTracker != null && traceRoute != null) {
					traceRoute.addChild(toNode.getCoordinate());
				}
	            // 새로운 gCost(시작점부터 이웃 노드까지의 누적 거리) 계산
	            double newDist = minNode.getGCost() + edge.getDistance();
	            // openList에 없고, 더 짧은 경로라면 갱신
	            if(!openList.contains(toNode) && newDist < toNode.getGCost()) {
	                // hCost(이웃 노드에서 목적지까지의 하버사인 거리) 계산
	                double hCost = PathUtil.haversineDistance(toNode.getCoordinate(), endNode.getCoordinate());
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
	 * 노드와 연결된 엣지를 모두 반환합니다.
	 * @param edgeList
	 * @return 연결된 모든 엣지
	 * @throws IOException 
	 */
	private ArrayList<Edge> getConnectedEdges(Node node) throws IOException {
		ArrayList<Edge> edges = new ArrayList<Edge>();
		
		Edge edge = store.readEdge(node.getStartEdgeOffset());
		edges.add(edge);

		while(edges.get(edges.size() - 1).getNextEdgeOffset() != -1) {
			edge = store.readEdge(edges.get(edges.size() - 1).getNextEdgeOffset());
			edges.add(edge);
		}
		
		return edges;
	}

	/**
	 * 주어진 좌표와 거리가 가까운 노드들을 찾고 그 중 가장 가까운 노드를 반환합니다.
	 * @param coordinate
	 * @return 가까운 노드 좌표
	 * @throws IOException 
	 */
	private Node findNearestNode(Coordinate coordinate) throws IOException {
		// 주어진 좌표에서 가까운 노드 오프셋을 가져온다. 30미터 이내
		Envelope envelope = createSearchEnvelope(coordinate, 100);
		List<Integer> nodeIdList = dataProvider.findNearestNodeId(envelope, coordinate);
		ArrayList<Node> nodeList = new ArrayList<Node>();
		Node n = null;

		if(nodeIdList.isEmpty()) {
			throw new EmptyGeometryListException("해당 좌표에 가까운 노드 데이터를 찾을 수 없습니다. 좌표 : " + coordinate.toString());
		}

		for(Integer nodeId : nodeIdList) {
			Node node = store.readNode(nodeId * DataStructureSizes.NODE_SIZE);
			nodeList.add(node);
		}

		// 후보 노드 중 거리가 제일 가까운 라인 검색
		if(nodeList.size() >= 2) {
			double minDistance = Double.MAX_VALUE;
			Node minNode = null;
			for(Node node : nodeList) {
				double distance = node.getCoordinate().calculateDistanceToTarget(coordinate);
				if(distance < minDistance) {
					minDistance = distance;
					minNode = node;
				}
			}

			n = minNode;
		}
		else {
			n = nodeList.get(0);
		}

		log.debug("가장 가까운 노드 ID : " + (n != null ? n.getId() : "null") + " / 좌표 : " + (n != null ? n.getCoordinate().toString() : "null"));

		return n;
	}

	private Envelope createSearchEnvelope(Coordinate coordinate, double distance) {
		double latDiff = distance / 111111.0; // 위도 1도는 약 111km
		double lonDiff = distance / (111111.0 * Math.cos(Math.toRadians(coordinate.getLatitude()))); // 경도 1도는 위도에 따라 다름

		double minLat = coordinate.getLatitude() - latDiff;
		double maxLat = coordinate.getLatitude() + latDiff;
		double minLon = coordinate.getLongitude() - lonDiff;
		double maxLon = coordinate.getLongitude() + lonDiff;

		return new Envelope(minLon, maxLon, minLat, maxLat);
	}
	
	/**
	 * 주어진 노드의 연결된 엣지중 주어진 좌표에 거리가 가까운 엣지를 찾아 반환합니다.
	 * @param edge
	 * @return 주어진 좌표에 거리와 가장 가까운 엣지
	 * @throws IOException 
	 */
	private Edge findNearestEdge(Node node, Coordinate coordinate) throws IOException {
		// 주어진 노드에 연결된 엣지들을 가져온다.
		ArrayList<Edge> edgeList = getConnectedEdges(node);
		if (edgeList.isEmpty()) {
			throw new EmptyGeometryListException("노드에 연결된 엣지 데이터가 없습니다. 노드 ID : " + node.getId());
		}

		// 후보 라인 중 거리가 제일 가까운 라인 검색
		Edge nearestEdge = null;
		double minDistance = Double.MAX_VALUE;
		
		// 노드에서 각 엣지의 도착 노드까지의 거리를 확인하여 가까운 엣지를 선택
		for(Edge edge : edgeList) {
			log.debug("엣지 - {}, {}, {}, {}", edge.getId(), edge.getFrom(), edge.getTo(), edge.getDistance());

			Node toNode = store.readNode(edge.getTo() * DataStructureSizes.NODE_SIZE);
			double distance = coordinate.calculateDistanceToTarget(toNode.getCoordinate());
			if(distance < minDistance) {
				minDistance = distance;
				nearestEdge = edge;
			}
		}

		log.debug("이웃 엣지 - {}, {}, {}, {}", nearestEdge.getId(), nearestEdge.getFrom(), nearestEdge.getTo(), nearestEdge.getDistance());
		
		return nearestEdge;
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
