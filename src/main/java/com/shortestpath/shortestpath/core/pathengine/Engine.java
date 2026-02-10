package com.shortestpath.shortestpath.core.pathengine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.locationtech.jts.geom.Envelope;

import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndexEntry;
import com.shortestpath.shortestpath.core.pathengine.Util.PathUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Engine {
	private DataStore store;
	private NodeProvider dataProvider;
	private final Object checkListLock = new Object();  // 🔥 양방향 탐색의 Race Condition 방지
		
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
	 * 
	 * L0 (고속도로): 100 km/h, 가중치 0.8 → 선호 (시간 0.8배 + 가중치 할인)
	 * L1 (일반도로): 60 km/h, 가중치 1.0 → 표준
	 * L2 (지방도로): 40 km/h, 가중치 1.2 → 회피 (시간 느림 + 가중치 페널티)
	 * 
	 * @param edge 엣지
	 * @return 속도와 가중치를 고려한 비용 (거리/속도) × 가중치
	 */
	private double getWeightedDistance(Edge edge) {
		double baseDistance = edge.getDistance();
		double speed;  // km/h
		double weight; // 추가 가중치
		
		switch(edge.getRoadLevel()) {
			case L0:
				speed = 100;  // 고속도로
				weight = 0.8; // 선호 (할인)
				break;
			case L1:
				speed = 60;   // 일반도로
				weight = 1.0; // 표준
				break;
			case L2:
				speed = 40;   // 지방도로
				weight = 1.2; // 회피 (페널티)
				break;
			default:
				speed = 20;
				weight = 1.5;
		}
		
		// 최종 비용 = (거리 / 속도) × 가중치
		double timeCost = (baseDistance / speed) * weight;
		
		return timeCost;
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
			Node fromNode = store.readNode(DataStructureSizes.calculateNodeOffset(nearestEdge.getFrom()));
			Node toNode = store.readNode(DataStructureSizes.calculateNodeOffset(nearestEdge.getTo()));
			startNearestPoint = calculateNearestPointOnLine(fromNode.getCoordinate(), toNode.getCoordinate(), startCoordinate);

			startNode = nearestNode;
		}
		
		if(endNode == null) {
			// 가까운 라인의 시작 과 끝 좌표를 가져온다.
			Node nearestNode = findNearestNode(endCoordinate);
			Edge nearestEdge = findNearestEdge(nearestNode, endCoordinate);
			Node fromNode = store.readNode(DataStructureSizes.calculateNodeOffset(nearestEdge.getFrom()));
			Node toNode = store.readNode(DataStructureSizes.calculateNodeOffset(nearestEdge.getTo()));
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

		// double searchDistance = PathUtil.haversineDistance(startCoordinate, endCoordinate);
		ArrayList<Node> resultPath = null;
		

		resultPath = findhierarchyPath(startNode, endNode, routeTracker);

		// if(searchDistance >= 50) {
		// 	log.info("계층 경로 탐색 모드로 전환 - 탐색 거리 : " + searchDistance);
		// 	resultPath = findhierarchyPath(startNode, endNode, routeTracker);
		// 	// resultPath = test(startNode, endNode, routeTracker);
		// }
		// else {
		// 	resultPath = findhierarchyPath(startNode, endNode, routeTracker);
		// }
		
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
	    PriorityQueue<Node> openList = new PriorityQueue<Node>(Comparator.comparingDouble(c -> c.getfCost()));
	    // 이미 방문한 노드 집합
	    HashSet<Node> closeList = new HashSet<Node>();
	    // 각 노드의 이전 노드를 저장(경로 역추적용)
	    HashMap<Node, Node> location = new HashMap<Node, Node>();

		// 노드와 엣지 캐싱 맵
		HashMap<Integer, Node> nodeList = new HashMap<>();
		HashMap<Long, Edge> edgeList = new HashMap<>();

	    // 시작 노드의 휴리스틱(목적지까지의 하버사인 거리) 계산
	    double heuristic = PathUtil.haversineDistance(startNode.getCoordinate(), endNode.getCoordinate());

		// 첫 시작 노드 gCost = 0 설정
		startNode.setgCost(0);
		startNode.sethCost(heuristic);
		startNode.setfCost(heuristic);
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
				Node storeNode = store.readNode(DataStructureSizes.calculateNodeOffset(edge.getTo()));
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
	            double newDist = minNode.getgCost() + edge.getDistance();
	            
	            // 더 짧은 경로를 발견한 경우
	            if(newDist < toNode.getgCost()) {
	                // 처음 발견한 노드인지 확인
	                boolean isNewNode = !openList.contains(toNode);
	                
	                // hCost(이웃 노드에서 목적지까지의 하버사인 거리) 계산
	                double hCost = PathUtil.haversineDistance(toNode.getCoordinate(), endNode.getCoordinate());
	                double fCost = newDist + hCost;
					toNode.sethCost(hCost);
					toNode.setgCost(newDist);
					toNode.setfCost(fCost);

	                if(!isNewNode) {
	                    // 이미 openList에 있으면 제거 (우선순위 재계산을 위해)
	                    openList.remove(toNode);
	                }
	                
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

	private ArrayList<Node> findhierarchyPath(Node startNode, Node endNode, RouteTracker routeTracker) throws IOException {
		// return null;
		// fCost(=gCost+hCost)가 가장 낮은 노드를 우선적으로 꺼내는 우선순위 큐
		PriorityQueue<SearchRoute> openList = new PriorityQueue<SearchRoute>(Comparator.comparingDouble(c -> c.getNode().getfCost()));
		// 이미 방문한 노드 집합
		HashSet<Node> closeList = new HashSet<Node>();
		// 각 노드의 이전 노드를 저장(경로 역추적용)
		HashMap<Node, Node> location = new HashMap<Node, Node>();

		// 노드와 엣지 캐싱 맵
		HashMap<Integer, Node> nodeList = new HashMap<>();
		HashMap<Long, Edge> edgeList = new HashMap<>();

		EdgeIndex edgeIndex = store.getEdgeIndex();

		// 시작 노드의 휴리스틱(목적지까지의 하버사인 거리) 계산
		double heuristic = PathUtil.haversineDistance(startNode.getCoordinate(), endNode.getCoordinate());

		// 첫 노드 설정
		startNode.setgCost(0);
		startNode.sethCost(heuristic);
		startNode.setfCost(heuristic);
		nodeList.put(startNode.getId(), startNode);;

		openList.add(new SearchRoute(startNode, null));

		RoadLevel currentLevel = null; // 현재 진행 중인 도로 계층 (null이면 모든 계층)

		while(!openList.isEmpty()) {
			SearchRoute minRoute = openList.poll();

			TraceRoute traceRoute = null;
			if(routeTracker != null) {
				traceRoute = new TraceRoute(minRoute.getNode().getCoordinate());
				routeTracker.addTraceRoute(traceRoute);
			}

			// 이미 방문한 노드는 건너뜀
			if(closeList.contains(minRoute.getNode())) {
				continue;
			}

			if(minRoute.getNode().equals(endNode)) {
				break;
			}

			closeList.add(minRoute.getNode());
			double distToTarget = PathUtil.haversineDistance(minRoute.getNode().getCoordinate(), endNode.getCoordinate());
			
			// 현재 노드에 도달한 엣지의 계층으로 매번 업데이트
			// (L2 → L1 → L0 → L1 → L2 등 자유롭게 이동 가능)
			if(minRoute.getEdge() != null) {
				currentLevel = minRoute.getEdge().getRoadLevel();
				// log.debug("현재 계층 업데이트: " + currentLevel);
			}
			
			EdgeIndexEntry entry = edgeIndex.get(minRoute.getNode().getId());
			ArrayList<Edge> connectedEdges = new ArrayList<>();

			// 현재 계층에 따른 엣지 제공
			if(currentLevel != null) {
				if(currentLevel == RoadLevel.L0) {
					// L0(고속도로)에서는 게이트 노드에서만 다른 계층으로 전환 가능
					if(minRoute.getNode().isGate()) {
						// 게이트 노드면 모든 엣지 제공 (다른 계층 진출 가능)
						connectedEdges = getConnectedEdges(edgeList, minRoute.getNode());
					} else {
						// 게이트가 아니면 L0 엣지만 제공 (같은 계층 유지)
						connectedEdges = getConnectedLevelEdges(edgeList, minRoute.getNode(), RoadLevel.L0);
						// L0 엣지가 없으면 모든 엣지 (고속도로 끝나는 지점)
						if(connectedEdges.isEmpty()) {
							connectedEdges = getConnectedEdges(edgeList, minRoute.getNode());
						}
					}
				}
				else if(currentLevel == RoadLevel.L1) {
					if(distToTarget <= 10) {
						connectedEdges = getConnectedEdges(edgeList, minRoute.getNode());
					}
					else {
						connectedEdges = getConnectedLevelEdgesWithExit(edgeList, minRoute.getNode(), currentLevel);
					}
				}
				else {
					// 초기 상태면 모든 엣지 사용 (모든 계층 탐색)
					connectedEdges = getConnectedEdges(edgeList, minRoute.getNode());
				}		 
			}
			else {
				// 초기 상태면 모든 엣지 사용 (모든 계층 탐색)
				connectedEdges = getConnectedEdges(edgeList, minRoute.getNode());
			}

			for(Edge edge : connectedEdges) {
				Node toNode = nodeList.get(edge.getTo());
				if(toNode == null) {
					toNode = store.readNode(DataStructureSizes.calculateNodeOffset(edge.getTo()));
					nodeList.put(toNode.getId(), toNode);
				}

				// 이미 방문한 노드는 건너뜀
				if(closeList.contains(toNode)) {
					continue;
				}

				if(routeTracker != null && traceRoute != null) {
					traceRoute.addChild(toNode.getCoordinate());
				}

				// 새로운 gCost(시작점부터 이웃 노드까지의 누적 거리) 계산 - roadLevel 가중치 적용
				double newDist = minRoute.getNode().getgCost() + getWeightedDistance(edge);

				// 더 짧은 경로를 발견한 경우
				if(newDist < toNode.getgCost()) {
					// 처음 발견한 노드인지 확인
					boolean isNewNode = !openList.contains(toNode);

					// hCost(이웃 노드에서 목적지까지의 하버사인 거리) 계산
					double hCost = PathUtil.haversineDistance(toNode.getCoordinate(), endNode.getCoordinate());
					double fCost = newDist + hCost;
					toNode.sethCost(hCost);
					toNode.setgCost(newDist);
					toNode.setfCost(fCost);

					if(!isNewNode) {
						// 이미 openList에 있으면 제거 (우선순위 재계산을 위해)
						openList.remove(toNode);
					}

					openList.add(new SearchRoute(toNode, edge));
					// 경로 역추적을 위해 이전 노드 저장
					location.put(toNode, minRoute.getNode());
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

	private ArrayList<Edge> getConnectedLevelEdges(Map<Long,Edge> edgeList, Node node, RoadLevel level) throws IOException {
		ArrayList<Edge> levelEdges = new ArrayList<Edge>();
		
		// 지정된 계층의 엣지만 필터링해서 반환
		ArrayList<Edge> allEdges = getConnectedEdges(edgeList, node);
		for(Edge edge : allEdges) {
			if(edge.getRoadLevel() == level) {
				levelEdges.add(edge);
			}
		}
		
		return levelEdges;
	}

	/**
	 * 현재 계층의 엣지 + 다음 레벨 엣지를 함께 반환
	 * L0이면 L0 + L1, L1이면 L1 + L2 등의 방식으로 나갈 출구 제공
	 */
	private ArrayList<Edge> getConnectedLevelEdgesWithExit(Map<Long,Edge> edgeList, Node node, RoadLevel currentLevel) throws IOException {
		ArrayList<Edge> levelEdges = new ArrayList<Edge>();
		
		// 지정된 현재 계층의 엣지 추가
		ArrayList<Edge> allEdges = getConnectedEdges(edgeList, node);
		for(Edge edge : allEdges) {
			if(edge.getRoadLevel() == currentLevel) {
				levelEdges.add(edge);
			}
		}
		
		// 다음 레벨 엣지도 추가 (나갈 출구)
		RoadLevel nextLevel = getNextLevel(currentLevel);
		if(nextLevel != null) {
			for(Edge edge : allEdges) {
				if(edge.getRoadLevel() == nextLevel) {
					levelEdges.add(edge);
				}
			}
		}
		
		return levelEdges;
	}

	/**
	 * 다음 레벨 계층을 반환 (L0 → L1, L1 → L2, L2 → null)
	 */
	private RoadLevel getNextLevel(RoadLevel currentLevel) {
		if(currentLevel == null) {
			return null;
		}
		
		switch(currentLevel) {
			case L0:
				return RoadLevel.L1;
			case L1:
				return RoadLevel.L2;
			case L2:
				return null; // 최하단 계층
			default:
				return null;
		}
	}

	/**
	 * 노드와 연결된 엣지를 모두 반환합니다. edgeList에 없는 엣지는 store에서 읽어와 추가합니다.
	 * @param Node
	 * @return List<Edge>
	 * @throws IOException 
	 */
	private ArrayList<Edge> getConnectedEdges(Map<Long,Edge> edgeList, Node node) throws IOException {
		ArrayList<Edge> edges = new ArrayList<Edge>();

		EdgeIndex index = store.getEdgeIndex();

        EdgeIndexEntry entry = index.get(node.getId());
        int edgeCount = entry.getLevel0EdgeIndex().getEdgeCount() + entry.getLevel1EdgeIndex().getEdgeCount()
                + entry.getLevel2EdgeIndex().getEdgeCount();

        long startOffset = getStartOffset(entry);
        for(int i = 0; i<edgeCount; i++) {
            long edgeOffset = startOffset + (i * DataStructureSizes.EDGE_SIZE);
            
            // 캐시에서 먼저 확인
            Edge cachedEdge = edgeList.get(edgeOffset);
            if(cachedEdge != null) {
                edges.add(cachedEdge);
            } else {
                Edge edge = store.readEdge(edgeOffset);
                edgeList.put(edgeOffset, edge);
                edges.add(edge);
            }
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
		
		EdgeIndex index = store.getEdgeIndex();

        EdgeIndexEntry entry = index.get(node.getId());
        int edgeCount = entry.getLevel0EdgeIndex().getEdgeCount() + entry.getLevel1EdgeIndex().getEdgeCount()
                + entry.getLevel2EdgeIndex().getEdgeCount();

        long startOffset = getStartOffset(entry);
        for(int i = 0; i<edgeCount; i++) {
            Edge edge = store.readEdge(startOffset + (i * DataStructureSizes.EDGE_SIZE));
            edges.add(edge);
        }
		
		return edges;
	}

	private long getStartOffset(EdgeIndexEntry entry) {
		if (entry.getLevel0EdgeIndex().getEdgeCount() > 0) {
			return entry.getLevel0EdgeIndex().getStartOffset();
		}
		 else if (entry.getLevel1EdgeIndex().getEdgeCount() > 0) {
			return entry.getLevel1EdgeIndex().getStartOffset();
		} 
		else {
			return entry.getLevel2EdgeIndex().getStartOffset();
		}
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
			Node node = store.readNode(DataStructureSizes.calculateNodeOffset(nodeId));
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

			Node toNode = store.readNode(DataStructureSizes.calculateNodeOffset(edge.getTo()));
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
