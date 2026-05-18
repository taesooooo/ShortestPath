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
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.locationtech.jts.geom.Envelope;

import com.shortestpath.shortestpath.core.pathengine.Provider.NodeProvider;
import com.shortestpath.shortestpath.core.pathengine.Store.DataStore;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.EdgeIndexEntry;
import com.shortestpath.shortestpath.core.pathengine.Store.Index.FileBasedEdgeIndex;
import com.shortestpath.shortestpath.core.pathengine.Util.PathUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Engine {
	private DataStore store;
	private NodeProvider dataProvider;
	private HotRoadCache hotRoadCache;
	private final Object checkListLock = new Object();  // 🔥 양방향 탐색의 Race Condition 방지
	private static final int SEARCH_BUFFER_INITIAL_CAPACITY = 1024;
	private final ThreadLocal<SearchBuffers> searchBuffers = ThreadLocal.withInitial(
			() -> new SearchBuffers(SEARCH_BUFFER_INITIAL_CAPACITY));
	private final ThreadLocal<SearchBuffers> reverseSearchBuffers = ThreadLocal.withInitial(
			() -> new SearchBuffers(SEARCH_BUFFER_INITIAL_CAPACITY));
		
	public Engine(DataStore store, NodeProvider dataProvider) throws IOException {
		if(store == null) {
			throw new IllegalArgumentException("경로 탐색 엔진 초기화를 실패했습니다. DataStore가 null입니다..");
		}
		
		if(dataProvider == null) {
			throw new IllegalArgumentException("경로 탐색 엔진 초기화를 실패했습니다. DataProvider가 null입니다.");
		}

		this.store = store;
		this.dataProvider = dataProvider;
		this.hotRoadCache = HotRoadCache.load(store, canUseMappedViews());

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
	private double getWeightedDistance(long offset) {
		double baseDistance = store.viewEdgeDistance(offset);
		double speed;  // km/h
		double weight; // 추가 가중치
		
		switch(store.viewEdgeRoadLevel(offset)) {
			case L0:
				speed = 100;  // 고속도로
				weight = 0.5; // 선호 (할인)
				break;
			case L1:
				speed = 60;   // 일반도로
				weight = 1.0; // 표준
				break;
			case L2:
				speed = 30;   // 지방도로
				weight = 1.5; // 회피 (페널티)
				break;
			default:
				speed = 20;
				weight = 2.5;
		}
		
		// 최종 비용 = (거리 / 속도) × 가중치
		double timeCost = (baseDistance / speed) * weight;
		
		return timeCost;
	}

	private double getWeightedDistance(Edge edge) {
		double baseDistance = edge.getDistance();
		double speed;  // km/h
		double weight; // 추가 가중치
		
		switch(edge.getRoadLevel()) {
			case L0:
				speed = 100;  // 고속도로
				weight = 0.5; // 선호 (할인)
				break;
			case L1:
				speed = 60;   // 일반도로
				weight = 1.0; // 표준
				break;
			case L2:
				speed = 30;   // 지방도로
				weight = 1.5; // 회피 (페널티)
				break;
			default:
				speed = 20;
				weight = 2.5;
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
		

		// resultPath = findhierarchyPath(startNode, endNode, routeTracker);

		resultPath = routeTracker == null
				? findBidirectionalPath(startNode, endNode)
				: findtest(startNode, endNode, routeTracker);

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

		int a = 0,b = 0,c = 0;

		// 시작 노드의 휴리스틱(목적지까지의 하버사인 거리) 계산
		double heuristic = PathUtil.haversineDistance(startNode.getCoordinate(), endNode.getCoordinate()) / 100;
		a++;

		// 첫 노드 설정
		startNode.setgCost(0);
		startNode.sethCost(heuristic);
		startNode.setfCost(heuristic);
		nodeList.put(startNode.getId(), startNode);

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

			double distToTarget = PathUtil.haversineDistance(minRoute.getNode().getCoordinate(), endNode.getCoordinate()) / 100;
			b++;
			// 현재 노드에 도달한 엣지의 계층으로 매번 업데이트
			// (L2 → L1 → L0 → L1 → L2 등 자유롭게 이동 가능)
			if(minRoute.getEdge() != null) {
				currentLevel = minRoute.getEdge().getRoadLevel();
				// log.debug("현재 계층 업데이트: " + currentLevel);
			}
			
			// EdgeIndexEntry entry = edgeIndex.get(minRoute.getNode().getId());
			ArrayList<Edge> connectedEdges = new ArrayList<>();

			// 현재 계층에 따른 엣지 제공
			if(currentLevel != null) {
				boolean isGate = false;
				if(currentLevel == RoadLevel.L0) {
					// L0(고속도로)에서는 게이트 노드에서만 다른 계층으로 전환 가능
					if(isGate) {
						// 게이트 노드면 모든 엣지 제공 (다른 계층 진출 가능)
						connectedEdges = getConnectedEdges(edgeList, minRoute.getNode());
					} else {
						// 게이트가 아니면 L0 엣지만 제공 (같은 계층 유지)
						connectedEdges = getConnectedLevelEdges(edgeList,minRoute.getNode(), RoadLevel.L0);
						// L0 엣지가 없으면 모든 엣지 (고속도로 끝나는 지점)
						if(connectedEdges.isEmpty()) {
							connectedEdges =  getConnectedEdges(edgeList, minRoute.getNode());
						}
					}
				}
				else if(currentLevel == RoadLevel.L1) {
					// 만약 거리가 10키로 안쪽이라면 L1에서 바로 L2로 나갈 수 있도록 모든 엣지 제공
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
			// log.info("엣지 개수 - {}/ nodeId - {}", connectedEdges.size(), minRoute.getNode().getId());
			// connectedEdges.forEach((i) -> log.info("엣지 - {}", i.getTo()));

 			for(Edge edge : connectedEdges) {
				Node toNode = nodeList.get(edge.getTo());
				if(toNode == null) {
					toNode = store.readNode(DataStructureSizes.calculateNodeOffset(edge.getTo()));
					nodeList.put(toNode.getId(), toNode);
				}

				//
				int toNodeId = edge.getTo();


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
					double hCost = (PathUtil.haversineDistance(toNode.getCoordinate(), endNode.getCoordinate()) / 100);
					double fCost = newDist + (hCost * 1.5);
					toNode.sethCost(hCost);
					toNode.setgCost(newDist);
					toNode.setfCost(fCost);
					c++;
					// if(!isNewNode) {
					// 	// 이미 openList에 있으면 제거 (우선순위 재계산을 위해)
					// 	openList.remove(toNode);
					// }
					// log.info("test - {}/{}/{}/{}/{}", toNodeId, hCost, fCost, newDist, edge.getId());
					openList.add(new SearchRoute(toNode, edge));
					// 경로 역추적을 위해 이전 노드 저장
					location.put(toNode, minRoute.getNode());

				}
			}
		}

		// 탐색 결과를 역추적하여 경로 리스트 생성
	    ArrayList<Node> path = new ArrayList<Node>();
	    // Node node = location.get(endNode);
	    // while(node != null) {
	    //     path.add(node);
	    //     node = location.get(node);
	    // }

	    // // 경로를 올바른 순서로 뒤집음
	    // Collections.reverse(path);

		// path.add(endNode);

		// if(path.isEmpty() || path.get(0) != startNode) {
		// 	// 연결된 노드가 없는 경우
		// 	return null;
		// }
		log.info("횟수 {}/{}/{}", a,b,c);
	    return path;
	}

	private ArrayList<Node> findBidirectionalPath(Node startNode, Node endNode) throws IOException {
		long searchStartTimeNanos = System.nanoTime();
		SearchBuffers forwardBuffer = searchBuffers.get();
		SearchBuffers reverseBuffer = reverseSearchBuffers.get();
		int maxEndpointId = Math.max(startNode.getId(), endNode.getId());
		forwardBuffer.prepare(maxEndpointId);
		reverseBuffer.prepare(maxEndpointId);

		PriorityQueue<SearchState> forwardQueue = new PriorityQueue<SearchState>(Comparator.comparingDouble(SearchState::getfCost));
		PriorityQueue<SearchState> reverseQueue = new PriorityQueue<SearchState>(Comparator.comparingDouble(SearchState::getfCost));
		HashMap<Long, Edge> edgeList = new HashMap<>();
		HashMap<Long, Edge> reverseEdgeList = new HashMap<>();
		boolean useMappedViews = canUseMappedViews();

		int startNodeId = startNode.getId();
		int endNodeId = endNode.getId();
		double forwardTargetLon = endNode.getCoordinate().getLongitude();
		double forwardTargetLat = endNode.getCoordinate().getLatitude();
		double reverseTargetLon = startNode.getCoordinate().getLongitude();
		double reverseTargetLat = startNode.getCoordinate().getLatitude();

		if(startNodeId == endNodeId) {
			ArrayList<Node> path = new ArrayList<Node>();
			path.add(startNode);
			return path;
		}

		double forwardStartHeuristic = getMinimumWeightedHeuristicCost(
				startNode.getCoordinate().getLongitude(),
				startNode.getCoordinate().getLatitude(),
				forwardTargetLon,
				forwardTargetLat);
		double reverseStartHeuristic = getMinimumWeightedHeuristicCost(
				endNode.getCoordinate().getLongitude(),
				endNode.getCoordinate().getLatitude(),
				reverseTargetLon,
				reverseTargetLat);
		forwardBuffer.initializeStartNode(startNodeId, forwardStartHeuristic);
		reverseBuffer.initializeStartNode(endNodeId, reverseStartHeuristic);
		forwardQueue.add(new SearchState(startNodeId, -1L, 0, forwardStartHeuristic));
		reverseQueue.add(new SearchState(endNodeId, -1L, 0, reverseStartHeuristic));

		SearchProfile profile = new SearchProfile();
		MeetingState meeting = new MeetingState();

		while(!forwardQueue.isEmpty() && !reverseQueue.isEmpty()) {
			if(meeting.hasMeeting()
					&& forwardQueue.peek().getfCost() >= meeting.bestCost
					&& reverseQueue.peek().getfCost() >= meeting.bestCost) {
				break;
			}

			boolean expandForward = forwardQueue.peek().getfCost() <= reverseQueue.peek().getfCost();
			if(expandForward) {
				expandBidirectionalSide(
						forwardQueue,
						forwardBuffer,
						reverseBuffer,
						edgeList,
						false,
						forwardTargetLon,
						forwardTargetLat,
						useMappedViews,
						profile,
						meeting);
			}
			else {
				expandBidirectionalSide(
						reverseQueue,
						reverseBuffer,
						forwardBuffer,
						reverseEdgeList,
						true,
						reverseTargetLon,
						reverseTargetLat,
						useMappedViews,
						profile,
						meeting);
			}
		}

		log.info("bidirectional edge cache hot/cold/total = {}/{}/{}",
				profile.hotEdgeHitCount,
				profile.coldEdgeReadCount,
				profile.connectedEdgeCount);
		log.info(
				"bidirectional profile ms - poll: {}, connected: {}, edgeTo: {}, edgeCost: {}, heuristic: {} (calls/miss={}/{}), offer: {}",
				nanosToMillis(profile.queuePollTimeNanos),
				nanosToMillis(profile.connectedEdgeTimeNanos),
				nanosToMillis(profile.edgeToTimeNanos),
				nanosToMillis(profile.edgeCostTimeNanos),
				nanosToMillis(profile.heuristicTimeNanos),
				profile.heuristicCallCount,
				profile.heuristicMissCount,
				nanosToMillis(profile.queueOfferTimeNanos));

		if(!meeting.hasMeeting()) {
			return null;
		}

		long pathBuildStartTimeNanos = System.nanoTime();
		ArrayList<Node> path = buildBidirectionalPath(
				startNode,
				endNode,
				meeting.nodeId,
				forwardBuffer,
				reverseBuffer);
		long pathBuildTimeNanos = System.nanoTime() - pathBuildStartTimeNanos;

		long measuredTimeNanos = profile.queuePollTimeNanos
				+ profile.currentLevelTimeNanos
				+ profile.connectedEdgeTimeNanos
				+ profile.hotEdgeCheckTimeNanos
				+ profile.edgeToTimeNanos
				+ profile.edgeCostTimeNanos
				+ profile.heuristicTimeNanos
				+ profile.queueOfferTimeNanos
				+ pathBuildTimeNanos;
		long totalTimeNanos = System.nanoTime() - searchStartTimeNanos;
		log.info(
				"bidirectional profile detail ms - total: {}, measured: {}, other: {}, currentLevel: {}, hotCheck: {}, pathBuild: {}, meet: {}, cost: {}, expanded F/R: {}/{}, meetCandidates: {}",
				nanosToMillis(totalTimeNanos),
				nanosToMillis(measuredTimeNanos),
				nanosToMillis(totalTimeNanos - measuredTimeNanos),
				nanosToMillis(profile.currentLevelTimeNanos),
				nanosToMillis(profile.hotEdgeCheckTimeNanos),
				nanosToMillis(pathBuildTimeNanos),
				meeting.nodeId,
				meeting.bestCost,
				profile.forwardExpandedCount,
				profile.reverseExpandedCount,
				profile.meetingCandidateCount);

		return path;
	}

	private void expandBidirectionalSide(
			PriorityQueue<SearchState> queue,
			SearchBuffers ownBuffer,
			SearchBuffers otherBuffer,
			Map<Long, Edge> edgeList,
			boolean reverseSide,
			double targetLon,
			double targetLat,
			boolean useMappedViews,
			SearchProfile profile,
			MeetingState meeting) throws IOException {
		long profileStart = System.nanoTime();
		SearchState min = queue.poll();
		profile.queuePollTimeNanos += System.nanoTime() - profileStart;
		int minNodeId = min.getNodeId();
		long minEdge = min.getEdgeOffset();
		ownBuffer.ensureCapacity(minNodeId);
		if(!ownBuffer.hasCost(minNodeId)) {
			return;
		}

		double minGCost = ownBuffer.getCurrentGCost(minNodeId);
		if(min.getgCost() > minGCost || ownBuffer.isVisited(minNodeId)) {
			return;
		}

		ownBuffer.markVisited(minNodeId);
		if(reverseSide) {
			profile.reverseExpandedCount++;
		}
		else {
			profile.forwardExpandedCount++;
		}
		if(otherBuffer.hasCost(minNodeId)) {
			profile.meetingCandidateCount++;
			meeting.accept(minNodeId, minGCost + otherBuffer.getCurrentGCost(minNodeId));
		}

		RoadLevel currentLevel = null;
		if(minEdge != -1) {
			profileStart = System.nanoTime();
			currentLevel = getSearchEdgeRoadLevel(minEdge, edgeList, useMappedViews, reverseSide);
			profile.currentLevelTimeNanos += System.nanoTime() - profileStart;
		}

		profileStart = System.nanoTime();
		int[] connectedEdges = getSearchConnectedEdges(
				edgeList,
				minNodeId,
				currentLevel,
				ownBuffer,
				reverseSide,
				targetLon,
				targetLat,
				useMappedViews,
				profile);
		profile.connectedEdgeTimeNanos += System.nanoTime() - profileStart;

		for(int edge : connectedEdges) {
			profile.connectedEdgeCount++;
			profileStart = System.nanoTime();
			boolean hotEdge = !reverseSide && hotRoadCache.containsEdge(edge);
			profile.hotEdgeCheckTimeNanos += System.nanoTime() - profileStart;
			if(hotEdge) {
				profile.hotEdgeHitCount++;
			}
			else {
				profile.coldEdgeReadCount++;
			}

			profileStart = System.nanoTime();
			int toNodeId = getSearchEdgeNextNode(edge, edgeList, useMappedViews, reverseSide, hotEdge);
			profile.edgeToTimeNanos += System.nanoTime() - profileStart;
			ownBuffer.ensureCapacity(toNodeId);

			if(ownBuffer.isVisited(toNodeId)) {
				continue;
			}

			profileStart = System.nanoTime();
			double edgeCost = getSearchWeightedDistance(edge, edgeList, useMappedViews, reverseSide, hotEdge);
			profile.edgeCostTimeNanos += System.nanoTime() - profileStart;
			double newDist = minGCost + edgeCost;

			if(newDist < ownBuffer.getCurrentGCost(toNodeId)) {
				ownBuffer.updateCost(toNodeId, minNodeId, newDist);

				profile.heuristicCallCount++;
				if(!ownBuffer.hasHeuristic(toNodeId)) {
					profile.heuristicMissCount++;
				}
				profileStart = System.nanoTime();
				double heuristic = getMinimumWeightedHeuristicCost(ownBuffer, toNodeId, targetLon, targetLat, useMappedViews);
				profile.heuristicTimeNanos += System.nanoTime() - profileStart;

				profileStart = System.nanoTime();
				queue.add(new SearchState(toNodeId, edge, newDist, newDist + heuristic));
				profile.queueOfferTimeNanos += System.nanoTime() - profileStart;

				if(otherBuffer.hasCost(toNodeId)) {
					profile.meetingCandidateCount++;
					meeting.accept(toNodeId, newDist + otherBuffer.getCurrentGCost(toNodeId));
				}
			}
		}
	}

	private int[] getSearchConnectedEdges(
			Map<Long, Edge> edgeList,
			int nodeId,
			RoadLevel currentLevel,
			SearchBuffers searchBuffer,
			boolean reverseSide,
			double targetLon,
			double targetLat,
			boolean useMappedViews,
			SearchProfile profile) throws IOException {
		if(currentLevel == null) {
			return getConnectedEdgesByNodeId(edgeList, nodeId, reverseSide);
		}

		if(currentLevel == RoadLevel.L0) {
			int[] levelEdges = getConnectedLevelEdgesByNodeId(edgeList, nodeId, RoadLevel.L0, reverseSide);
			if(levelEdges.length == 0) {
				return getConnectedEdgesByNodeId(edgeList, nodeId, reverseSide);
			}

			return levelEdges;
		}

		if(currentLevel == RoadLevel.L1) {
			double distToTarget = getDistanceToTargetCost(nodeId, targetLon, targetLat, useMappedViews);
			if(distToTarget <= 0.1) {
				return getConnectedEdgesByNodeId(edgeList, nodeId, reverseSide);
			}

			return getConnectedLevelEdgesWithExitByNodeId(edgeList, nodeId, currentLevel, reverseSide);
		}

		return getConnectedEdgesByNodeId(edgeList, nodeId, reverseSide);
	}

	private ArrayList<Node> buildBidirectionalPath(
			Node startNode,
			Node endNode,
			int meetingNodeId,
			SearchBuffers forwardBuffer,
			SearchBuffers reverseBuffer) throws IOException {
		ArrayList<Node> forwardPath = new ArrayList<Node>();
		int nodeId = meetingNodeId;
		int hopCount = 0;
		while(nodeId != startNode.getId()) {
			if(!forwardBuffer.hasCost(nodeId) || hopCount++ > forwardBuffer.capacity()) {
				return null;
			}
			forwardPath.add(store.readNode(DataStructureSizes.calculateNodeOffset(nodeId)));
			nodeId = forwardBuffer.getPreviousNode(nodeId);
		}
		forwardPath.add(startNode);
		Collections.reverse(forwardPath);

		nodeId = reverseBuffer.getPreviousNode(meetingNodeId);
		hopCount = 0;
		while(nodeId != -1 && nodeId != endNode.getId()) {
			if(!reverseBuffer.hasCost(nodeId) || hopCount++ > reverseBuffer.capacity()) {
				return null;
			}
			forwardPath.add(store.readNode(DataStructureSizes.calculateNodeOffset(nodeId)));
			nodeId = reverseBuffer.getPreviousNode(nodeId);
		}
		if(forwardPath.isEmpty() || forwardPath.get(forwardPath.size() - 1).getId() != endNode.getId()) {
			forwardPath.add(endNode);
		}

		return forwardPath;
	}

	private static final class MeetingState {
		private int nodeId = -1;
		private double bestCost = Double.MAX_VALUE;

		private boolean hasMeeting() {
			return nodeId != -1;
		}

		private void accept(int candidateNodeId, double candidateCost) {
			if(candidateCost < bestCost) {
				nodeId = candidateNodeId;
				bestCost = candidateCost;
			}
		}
	}

	private static final class SearchProfile {
		private long connectedEdgeCount;
		private long hotEdgeHitCount;
		private long coldEdgeReadCount;
		private long queuePollTimeNanos;
		private long connectedEdgeTimeNanos;
		private long edgeToTimeNanos;
		private long edgeCostTimeNanos;
		private long heuristicTimeNanos;
		private long queueOfferTimeNanos;
		private long currentLevelTimeNanos;
		private long hotEdgeCheckTimeNanos;
		private long heuristicCallCount;
		private long heuristicMissCount;
		private long forwardExpandedCount;
		private long reverseExpandedCount;
		private long meetingCandidateCount;
	}

	private ArrayList<Node> findtest(Node startNode, Node endNode, RouteTracker routeTracker) throws IOException {
		long findtestStartTimeNanos = System.nanoTime();
		SearchBuffers searchBuffer = searchBuffers.get();
		searchBuffer.prepare(Math.max(startNode.getId(), endNode.getId()));

		PriorityQueue<SearchState> nodeQueue = new PriorityQueue<SearchState>(Comparator.comparingDouble(SearchState::getfCost));

		// 노드와 엣지 캐싱 맵
		// HashMap<Integer, Node> nodeList = new HashMap<>();
		HashMap<Long, Edge> edgeList = new HashMap<>();
		boolean useMappedViews = canUseMappedViews();

		int a = 0;
		int b = 0;
		int c = 0;
		long connectedEdgeCount = 0;
		long hotEdgeHitCount = 0;
		long coldEdgeReadCount = 0;
		long queuePollTimeNanos = 0;
		long connectedEdgeTimeNanos = 0;
		long edgeToTimeNanos = 0;
		long edgeCostTimeNanos = 0;
		long heuristicTimeNanos = 0;
		long queueOfferTimeNanos = 0;
		long currentLevelTimeNanos = 0;
		long hotEdgeCheckTimeNanos = 0;
		long pathBuildTimeNanos = 0;
		long heuristicCallCount = 0;
		long heuristicMissCount = 0;

		int startNodeId = startNode.getId();
		int endNodeId = endNode.getId();
		double endLon = endNode.getCoordinate().getLongitude();
		double endLat = endNode.getCoordinate().getLatitude();

		if(startNodeId == endNodeId) {
			ArrayList<Node> path = new ArrayList<Node>();
			path.add(startNode);
			return path;
		}

		// 시작 노드의 휴리스틱(목적지까지의 하버사인 거리) 계산
		double heuristic = PathUtil.haversineDistance(startNode.getCoordinate().getLongitude(), startNode.getCoordinate().getLatitude(), endLon, endLat) / 100;
		a++;
		// 첫 노드 설정
		searchBuffer.initializeStartNode(startNodeId, heuristic);
		nodeQueue.add(new SearchState(startNodeId, -1L, 0, heuristic));

		boolean found = false;

		while(!nodeQueue.isEmpty()) {
			// //
			long profileStart = System.nanoTime();
		 	SearchState min = nodeQueue.poll();
			queuePollTimeNanos += System.nanoTime() - profileStart;
			int minNodeId = min.getNodeId();
			long minEdge = min.getEdgeOffset();
			searchBuffer.ensureCapacity(minNodeId);
			if(!searchBuffer.hasCost(minNodeId)) {
				continue;
			}

			double minGCost = searchBuffer.getCurrentGCost(minNodeId);
			if(min.getgCost() > minGCost) {
				continue;
			}

			TraceRoute traceRoute = null;
			if(routeTracker != null) {
				traceRoute = new TraceRoute(getNodeCoordinate(minNodeId, useMappedViews));
				routeTracker.addTraceRoute(traceRoute);
			}

			// // 이미 방문한 노드는 건너뜀
			if(searchBuffer.isVisited(minNodeId)) {
				continue;
			}

			if(minNodeId == endNodeId) {
				found = true;
				break;
			}

			searchBuffer.markVisited(minNodeId);

			//
			b++;
			
			// 현재 노드에 도달한 엣지의 계층으로 매번 업데이트
			// (L2 → L1 → L0 → L1 → L2 등 자유롭게 이동 가능
			RoadLevel currentLevel = null; // 현재 진행 중인 도로 계층 (null이면 모든 계층)
			if(minEdge != -1) {
				profileStart = System.nanoTime();
				currentLevel = getEdgeRoadLevel(minEdge, edgeList, useMappedViews);
				currentLevelTimeNanos += System.nanoTime() - profileStart;
			}
			
			// EdgeIndexEntry entry = edgeIndex.get(minRoute.getNode().getId());
			int[] connectedEdges;

			profileStart = System.nanoTime();
			// 현재 계층에 따른 엣지 제공
			if(currentLevel != null) {
				boolean isGate = false;
				if(currentLevel == RoadLevel.L0) {
					// L0(고속도로)에서는 게이트 노드에서만 다른 계층으로 전환 가능
					if(isGate) {
						// 게이트 노드면 모든 엣지 제공 (다른 계층 진출 가능)
						connectedEdges = getConnectedEdgesByNodeId(edgeList,minNodeId);
					} else {
						// 게이트가 아니면 L0 엣지만 제공 (같은 계층 유지)
						connectedEdges = getConnectedLevelEdgesByNodeId(edgeList, minNodeId, RoadLevel.L0);
						// L0 엣지가 없으면 모든 엣지 (고속도로 끝나는 지점)
						if(connectedEdges.length == 0) {
							connectedEdges =  getConnectedEdgesByNodeId(edgeList, minNodeId);
						}
					}
				}
				else if(currentLevel == RoadLevel.L1) {
					// 만약 거리가 10키로 안쪽이라면 L1에서 바로 L2로 나갈 수 있도록 모든 엣지 제공
					double distToTarget = getHeuristicCost(searchBuffer, minNodeId, endLon, endLat, useMappedViews);
					if(distToTarget <= 0.1) {
						connectedEdges = getConnectedEdgesByNodeId(edgeList, minNodeId);
					}
					else {
						connectedEdges = getConnectedLevelEdgesWithExitByNodeId(edgeList, minNodeId, currentLevel);
					}
				}
				else {
					// 초기 상태면 모든 엣지 사용 (모든 계층 탐색)
					connectedEdges = getConnectedEdgesByNodeId(edgeList, minNodeId);
				}		 
			}
			else {
				// 초기 상태면 모든 엣지 사용 (모든 계층 탐색)
				connectedEdges = getConnectedEdgesByNodeId(edgeList, minNodeId);
			}
			connectedEdgeTimeNanos += System.nanoTime() - profileStart;


			for(int edge : connectedEdges) {
				connectedEdgeCount++;
				profileStart = System.nanoTime();
				boolean hotEdge = hotRoadCache.containsEdge(edge);
				hotEdgeCheckTimeNanos += System.nanoTime() - profileStart;
				if(hotEdge) {
					hotEdgeHitCount++;
				} else {
					coldEdgeReadCount++;
				}

				profileStart = System.nanoTime();
				int toNodeId = hotEdge ? hotRoadCache.getEdgeTo(edge) : getEdgeTo(edge, edgeList, useMappedViews);
				edgeToTimeNanos += System.nanoTime() - profileStart;
				searchBuffer.ensureCapacity(toNodeId);

				// 이미 방문한 노드는 건너뜀
				if(searchBuffer.isVisited(toNodeId)) {
					continue;
				}

				if(routeTracker != null && traceRoute != null) {
					traceRoute.addChild(getNodeCoordinate(toNodeId, useMappedViews));
				}

				// 새로운 gCost(시작점부터 이웃 노드까지의 누적 거리) 계산 - roadLevel 가중치 적용
				profileStart = System.nanoTime();
				double edgeCost = hotEdge ? hotRoadCache.getWeightedDistance(edge) : getWeightedDistance(edge, edgeList, useMappedViews);
				edgeCostTimeNanos += System.nanoTime() - profileStart;
				double newDist = minGCost + edgeCost;
				double currentGCost = searchBuffer.getCurrentGCost(toNodeId);

				// 더 짧은 경로를 발견한 경우
				if(newDist < currentGCost) {
					heuristicCallCount++;
					if(!searchBuffer.hasHeuristic(toNodeId)) {
						heuristicMissCount++;
					}
					profileStart = System.nanoTime();
					double hCost = getHeuristicCost(searchBuffer, toNodeId, endLon, endLat, useMappedViews);
					heuristicTimeNanos += System.nanoTime() - profileStart;
					double calFCost = newDist + (hCost * 1.5);
					searchBuffer.updateCost(toNodeId, minNodeId, newDist);
					c++;
					
					profileStart = System.nanoTime();
					nodeQueue.add(new SearchState(toNodeId, edge, newDist, calFCost));
					queueOfferTimeNanos += System.nanoTime() - profileStart;
				}
			}
		}

		// 탐색 결과를 역추적하여 경로 리스트 생성
	    ArrayList<Node> path = new ArrayList<Node>();

		log.info("횟수 {}/{}/{}, edge cache hot/cold/total = {}/{}/{}", a,b,c, hotEdgeHitCount, coldEdgeReadCount, connectedEdgeCount);
		log.info(
				"findtest profile ms - poll: {}, connected: {}, edgeTo: {}, edgeCost: {}, heuristic: {} (calls/miss={}/{}), offer: {}",
				nanosToMillis(queuePollTimeNanos),
				nanosToMillis(connectedEdgeTimeNanos),
				nanosToMillis(edgeToTimeNanos),
				nanosToMillis(edgeCostTimeNanos),
				nanosToMillis(heuristicTimeNanos),
				heuristicCallCount,
				heuristicMissCount,
				nanosToMillis(queueOfferTimeNanos));

		if(!found) {
			return null;
		}

		long pathBuildStartTimeNanos = System.nanoTime();
		int nodeId = endNodeId;
		int hopCount = 0;
		while(nodeId != startNodeId) {
			if(!searchBuffer.hasCost(nodeId) || hopCount++ > searchBuffer.capacity()) {
				return null;
			}
			path.add(store.readNode(DataStructureSizes.calculateNodeOffset(nodeId)));
			nodeId = searchBuffer.getPreviousNode(nodeId);
		}

		path.add(startNode);
		Collections.reverse(path);
		pathBuildTimeNanos = System.nanoTime() - pathBuildStartTimeNanos;

		long measuredTimeNanos = queuePollTimeNanos
				+ currentLevelTimeNanos
				+ connectedEdgeTimeNanos
				+ hotEdgeCheckTimeNanos
				+ edgeToTimeNanos
				+ edgeCostTimeNanos
				+ heuristicTimeNanos
				+ queueOfferTimeNanos
				+ pathBuildTimeNanos;
		long totalTimeNanos = System.nanoTime() - findtestStartTimeNanos;
		log.info(
				"findtest profile detail ms - total: {}, measured: {}, other: {}, currentLevel: {}, hotCheck: {}, pathBuild: {}",
				nanosToMillis(totalTimeNanos),
				nanosToMillis(measuredTimeNanos),
				nanosToMillis(totalTimeNanos - measuredTimeNanos),
				nanosToMillis(currentLevelTimeNanos),
				nanosToMillis(hotEdgeCheckTimeNanos),
				nanosToMillis(pathBuildTimeNanos));

	    return path;
	}

	private double nanosToMillis(long nanos) {
		return nanos / 1_000_000.0;
	}

	private double getHeuristicCost(SearchBuffers searchBuffer, int nodeId, double endLon, double endLat, boolean useMappedViews) throws IOException {
		searchBuffer.ensureCapacity(nodeId);
		if(!searchBuffer.hasHeuristic(nodeId)) {
			Coordinate coordinate = getNodeCoordinate(nodeId, useMappedViews);
			double x = coordinate.getLongitude();
			double y = coordinate.getLatitude();
			searchBuffer.setHeuristic(nodeId, PathUtil.haversineDistance(x, y, endLon, endLat) / 100);
		}

		return searchBuffer.getHeuristic(nodeId);
	}

	private double getDistanceToTargetCost(int nodeId, double endLon, double endLat, boolean useMappedViews) throws IOException {
		Coordinate coordinate = getNodeCoordinate(nodeId, useMappedViews);
		return PathUtil.haversineDistance(
				coordinate.getLongitude(),
				coordinate.getLatitude(),
				endLon,
				endLat) / 100;
	}

	private double getMinimumWeightedHeuristicCost(SearchBuffers searchBuffer, int nodeId, double endLon, double endLat, boolean useMappedViews) throws IOException {
		searchBuffer.ensureCapacity(nodeId);
		if(!searchBuffer.hasHeuristic(nodeId)) {
			Coordinate coordinate = getNodeCoordinate(nodeId, useMappedViews);
			searchBuffer.setHeuristic(nodeId, getMinimumWeightedHeuristicCost(
					coordinate.getLongitude(),
					coordinate.getLatitude(),
					endLon,
					endLat));
		}

		return searchBuffer.getHeuristic(nodeId);
	}

	private double getMinimumWeightedHeuristicCost(double startLon, double startLat, double endLon, double endLat) {
		return (PathUtil.haversineDistance(startLon, startLat, endLon, endLat) / 100) * 1.5;
	}

	private boolean canUseMappedViews() {
		EdgeIndex index = store.getEdgeIndex();
		if(index instanceof FileBasedEdgeIndex) {
			if(!((FileBasedEdgeIndex)index).isMappingMode()) {
				return false;
			}
		}

		EdgeIndex reverseIndex = store.getReverseEdgeIndex();
		if(reverseIndex instanceof FileBasedEdgeIndex) {
			return ((FileBasedEdgeIndex)reverseIndex).isMappingMode();
		}

		return true;
	}

	private Coordinate getNodeCoordinate(int nodeId, boolean useMappedViews) throws IOException {
		Coordinate cachedCoordinate = hotRoadCache.getNodeCoordinate(nodeId);
		if(cachedCoordinate != null) {
			return cachedCoordinate;
		}

		if(useMappedViews) {
			return new Coordinate(store.viewNodeYCoordinate(nodeId), store.viewNodeXCoordinate(nodeId));
		}

		return store.readNode(DataStructureSizes.calculateNodeOffset(nodeId)).getCoordinate();
	}

	private int getEdgeTo(long edgeOffset, Map<Long, Edge> edgeList, boolean useMappedViews) throws IOException {
		if(hotRoadCache.containsEdge(edgeOffset)) {
			return hotRoadCache.getEdgeTo(edgeOffset);
		}

		if(useMappedViews) {
			return store.viewEdgeTo(edgeOffset);
		}

		return getCachedEdge(edgeList, edgeOffset).getTo();
	}

	private int getSearchEdgeNextNode(long edgeOffset, Map<Long, Edge> edgeList, boolean useMappedViews, boolean reverseSide, boolean hotEdge) throws IOException {
		if(hotEdge) {
			return hotRoadCache.getEdgeTo(edgeOffset);
		}

		if(reverseSide) {
			if(useMappedViews) {
				return store.viewReverseEdgeFrom(edgeOffset);
			}

			return getCachedReverseEdge(edgeList, edgeOffset).getFrom();
		}

		return getEdgeTo(edgeOffset, edgeList, useMappedViews);
	}

	private RoadLevel getEdgeRoadLevel(long edgeOffset, Map<Long, Edge> edgeList, boolean useMappedViews) throws IOException {
		if(hotRoadCache.containsEdge(edgeOffset)) {
			return hotRoadCache.getEdgeRoadLevel(edgeOffset);
		}

		if(useMappedViews) {
			return store.viewEdgeRoadLevel(edgeOffset);
		}

		return getCachedEdge(edgeList, edgeOffset).getRoadLevel();
	}

	private RoadLevel getSearchEdgeRoadLevel(long edgeOffset, Map<Long, Edge> edgeList, boolean useMappedViews, boolean reverseSide) throws IOException {
		if(!reverseSide) {
			return getEdgeRoadLevel(edgeOffset, edgeList, useMappedViews);
		}

		if(useMappedViews) {
			return store.viewReverseEdgeRoadLevel(edgeOffset);
		}

		return getCachedReverseEdge(edgeList, edgeOffset).getRoadLevel();
	}

	private double getWeightedDistance(long offset, Map<Long, Edge> edgeList, boolean useMappedViews) throws IOException {
		if(hotRoadCache.containsEdge(offset)) {
			return hotRoadCache.getWeightedDistance(offset);
		}

		if(useMappedViews) {
			return getWeightedDistance(offset);
		}

		return getWeightedDistance(getCachedEdge(edgeList, offset));
	}

	private double getSearchWeightedDistance(long offset, Map<Long, Edge> edgeList, boolean useMappedViews, boolean reverseSide, boolean hotEdge) throws IOException {
		if(hotEdge) {
			return hotRoadCache.getWeightedDistance(offset);
		}

		if(reverseSide) {
			if(useMappedViews) {
				return getWeightedDistance(store.viewReverseEdgeDistance(offset), store.viewReverseEdgeRoadLevel(offset));
			}

			return getWeightedDistance(getCachedReverseEdge(edgeList, offset));
		}

		return getWeightedDistance(offset, edgeList, useMappedViews);
	}

	private double getWeightedDistance(double baseDistance, RoadLevel roadLevel) {
		double speed;
		double weight;

		switch(roadLevel) {
			case L0:
				speed = 100;
				weight = 0.5;
				break;
			case L1:
				speed = 60;
				weight = 1.0;
				break;
			case L2:
				speed = 30;
				weight = 1.5;
				break;
			default:
				speed = 20;
				weight = 2.5;
		}

		return (baseDistance / speed) * weight;
	}

	private Edge getCachedEdge(Map<Long, Edge> edgeList, long edgeOffset) throws IOException {
		Edge edge = edgeList.get(edgeOffset);
		if(edge == null) {
			edge = store.readEdge(edgeOffset);
			edgeList.put(edgeOffset, edge);
		}

		return edge;
	}

	private Edge getCachedReverseEdge(Map<Long, Edge> edgeList, long edgeOffset) throws IOException {
		Edge edge = edgeList.get(edgeOffset);
		if(edge == null) {
			edge = store.readReverseEdge(edgeOffset);
			edgeList.put(edgeOffset, edge);
		}

		return edge;
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

	private int[] getConnectedLevelEdgesByNodeId(Map<Long,Edge> edgeList, int nodeId, RoadLevel level) throws IOException {
		return getConnectedLevelEdgesByNodeId(edgeList, nodeId, level, false);
	}

	private int[] getConnectedLevelEdgesByNodeId(Map<Long,Edge> edgeList, int nodeId, RoadLevel level, boolean reverseSide) throws IOException {
		if(!reverseSide && hotRoadCache.supportsLevel(level)) {
			return hotRoadCache.getConnectedLevelEdges(nodeId, level);
		}

		FileBasedEdgeIndex index = getSearchEdgeIndex(reverseSide);

		int count = getIndexEdgeCount(index, nodeId, level);
		int[] edgeArray = new int[count];
		long startOffset = getIndexStartOffset(index, nodeId, level);

		for(int i = 0; i<count; i++) {
			edgeArray[i] = (int)startOffset + (i * DataStructureSizes.EDGE_SIZE);
		}

		// 지정된 계층의 엣지만 필터링해서 반환
		// ArrayList<Integer> allEdges = getConnectedEdgesByNodeId(edgeList, nodeId);
		// for(Integer edge : allEdges) {
		// 	if(store.viewEdgeRoadLevel(edge) == level) {
		// 		levelEdges.add(edge);
		// 	}
		// }
		
		return edgeArray;
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

	private int[] getConnectedLevelEdgesWithExitByNodeId(Map<Long,Edge> edgeList, int nodeId, RoadLevel currentLevel) throws IOException {
		return getConnectedLevelEdgesWithExitByNodeId(edgeList, nodeId, currentLevel, false);
	}

	private int[] getConnectedLevelEdgesWithExitByNodeId(Map<Long,Edge> edgeList, int nodeId, RoadLevel currentLevel, boolean reverseSide) throws IOException {
		// ArrayList<Integer> levelEdges = new ArrayList<Integer>();
		FileBasedEdgeIndex index = getSearchEdgeIndex(reverseSide);
		
		// int nextRoadLevelOrdinal = currentLevel.ordinal() + 1 > 2 ? currentLevel.ordinal() : currentLevel.ordinal() + 1;
		RoadLevel nextRoadLevel = getNextLevel(currentLevel);
	
		int count = !reverseSide && hotRoadCache.supportsLevel(currentLevel)
				? hotRoadCache.getLevelEdgeCount(nodeId, currentLevel)
				: getIndexEdgeCount(index, nodeId, currentLevel);
		int nextEdgeCount = nextRoadLevel != null
				? getCachedOrIndexEdgeCount(index, nodeId, nextRoadLevel, reverseSide)
				: 0;

		int[] levelEdgeArray = new int[count + nextEdgeCount];
		long startOffset = !reverseSide && hotRoadCache.supportsLevel(currentLevel)
				? hotRoadCache.getLevelStartOffset(nodeId, currentLevel)
				: getIndexStartOffset(index, nodeId, currentLevel);

		// 지정된 현재 계층의 엣지 추가
		// int[] allEdges = getConnectedEdgesByNodeId(edgeList, nodeId);
		for(int i=0; i < count; i++) {
			levelEdgeArray[i] = (int)startOffset + (i * DataStructureSizes.EDGE_SIZE);
		}
		
		if(nextRoadLevel != null) {
			// 다음 레벨 엣지도 추가 (나갈 출구)
			startOffset = getCachedOrIndexStartOffset(index, nodeId, nextRoadLevel, reverseSide);
			for(int i=0; i< nextEdgeCount; i++) {
				levelEdgeArray[i + count] = (int)startOffset + (i * DataStructureSizes.EDGE_SIZE);
			}
		}
		
		return levelEdgeArray;
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

	private int[] getConnectedEdgesByNodeId(Map<Long,Edge> edgeList, int nodeId) throws IOException {
		return getConnectedEdgesByNodeId(edgeList, nodeId, false);
	}

	private int[] getConnectedEdgesByNodeId(Map<Long,Edge> edgeList, int nodeId, boolean reverseSide) throws IOException {
		// ArrayList<Integer> edges = new ArrayList<Integer>();

		FileBasedEdgeIndex index = getSearchEdgeIndex(reverseSide);

		int level0Count = !reverseSide && hotRoadCache.supportsLevel(RoadLevel.L0)
				? hotRoadCache.getLevelEdgeCount(nodeId, RoadLevel.L0)
				: getIndexEdgeCount(index, nodeId, RoadLevel.L0);
		int level1Count = !reverseSide && hotRoadCache.supportsLevel(RoadLevel.L1)
				? hotRoadCache.getLevelEdgeCount(nodeId, RoadLevel.L1)
				: getIndexEdgeCount(index, nodeId, RoadLevel.L1);
		int level2Count = !reverseSide && hotRoadCache.supportsLevel(RoadLevel.L2)
				? hotRoadCache.getLevelEdgeCount(nodeId, RoadLevel.L2)
				: getIndexEdgeCount(index, nodeId, RoadLevel.L2);
		int edgeCount = level0Count + level1Count + level2Count;
		int[] edgeArray = new int[edgeCount];
		
		if(edgeCount == 0) {
			return edgeArray;
		}

        long startOffset = 0;

		if (level0Count > 0) {
			startOffset = !reverseSide && hotRoadCache.supportsLevel(RoadLevel.L0)
					? hotRoadCache.getLevelStartOffset(nodeId, RoadLevel.L0)
					: getIndexStartOffset(index, nodeId, RoadLevel.L0);
		}
		 else if (level1Count > 0) {
			startOffset = !reverseSide && hotRoadCache.supportsLevel(RoadLevel.L1)
					? hotRoadCache.getLevelStartOffset(nodeId, RoadLevel.L1)
					: getIndexStartOffset(index, nodeId, RoadLevel.L1);
		} 
		else {
			startOffset = !reverseSide && hotRoadCache.supportsLevel(RoadLevel.L2)
					? hotRoadCache.getLevelStartOffset(nodeId, RoadLevel.L2)
					: getIndexStartOffset(index, nodeId, RoadLevel.L2);
		}

        for(int i = 0; i<edgeCount; i++) {
            edgeArray[i] = (int)startOffset + (i * DataStructureSizes.EDGE_SIZE);
        }
		
		return edgeArray;
	}

	private FileBasedEdgeIndex getSearchEdgeIndex(boolean reverseSide) {
		return (FileBasedEdgeIndex)(reverseSide ? store.getReverseEdgeIndex() : store.getEdgeIndex());
	}

	private int getCachedOrIndexEdgeCount(FileBasedEdgeIndex index, int nodeId, RoadLevel level) throws IOException {
		return getCachedOrIndexEdgeCount(index, nodeId, level, false);
	}

	private int getCachedOrIndexEdgeCount(FileBasedEdgeIndex index, int nodeId, RoadLevel level, boolean reverseSide) throws IOException {
		return !reverseSide && hotRoadCache.supportsLevel(level)
				? hotRoadCache.getLevelEdgeCount(nodeId, level)
				: getIndexEdgeCount(index, nodeId, level);
	}

	private long getCachedOrIndexStartOffset(FileBasedEdgeIndex index, int nodeId, RoadLevel level) throws IOException {
		return getCachedOrIndexStartOffset(index, nodeId, level, false);
	}

	private long getCachedOrIndexStartOffset(FileBasedEdgeIndex index, int nodeId, RoadLevel level, boolean reverseSide) throws IOException {
		return !reverseSide && hotRoadCache.supportsLevel(level)
				? hotRoadCache.getLevelStartOffset(nodeId, level)
				: getIndexStartOffset(index, nodeId, level);
	}

	private int getIndexEdgeCount(FileBasedEdgeIndex index, int nodeId, RoadLevel level) throws IOException {
		if(index.isMappingMode()) {
			return index.viewEdgeCount(nodeId, level);
		}

		EdgeIndexEntry entry = index.get(nodeId);
		if(entry == null) {
			return 0;
		}

		switch(level) {
			case L0:
				return entry.getLevel0EdgeIndex().getEdgeCount();
			case L1:
				return entry.getLevel1EdgeIndex().getEdgeCount();
			case L2:
				return entry.getLevel2EdgeIndex().getEdgeCount();
			default:
				return 0;
		}
	}

	private long getIndexStartOffset(FileBasedEdgeIndex index, int nodeId, RoadLevel level) throws IOException {
		if(index.isMappingMode()) {
			return index.viewStartOffset(nodeId, level);
		}

		EdgeIndexEntry entry = index.get(nodeId);
		if(entry == null) {
			return 0;
		}

		switch(level) {
			case L0:
				return entry.getLevel0EdgeIndex().getStartOffset();
			case L1:
				return entry.getLevel1EdgeIndex().getStartOffset();
			case L2:
				return entry.getLevel2EdgeIndex().getStartOffset();
			default:
				return 0;
		}
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
