# 데이터 스토어 아키텍처 재설계

## 개요
기존의 `HybridDataStore`에서 읽기(Read)와 쓰기(Write) 기능을 분리하여 CQRS 패턴을 적용한 아키텍처로 재설계했습니다.

## 핵심 설계 원칙

### 1. 관심사 분리 (Separation of Concerns)
- **DataWriter**: 데이터 저장 전담
- **DataReader**: 데이터 읽기 전담
- 각 역할이 명확하게 분리되어 코드 복잡성 감소

### 2. 단일 책임 원칙 (Single Responsibility Principle)
- 각 클래스가 하나의 책임만 수행
- 변경의 영향 범위 최소화

### 3. 개방-폐쇄 원칙 (Open-Closed Principle)
- 새로운 읽기/쓰기 전략 추가 시 기존 코드 수정 불필요
- 인터페이스 확장으로 새로운 구현체 추가 가능

## 아키텍처 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                      SeparatedHybridDataStore                   │
│          (Facade: Reader/Writer 조합 관리)                        │
└─────────────────────────────────────────────────────────────────┘
                    ↙ (위임)          ↖ (위임)
        ┌──────────────────────┐   ┌──────────────────────┐
        │    DataWriter        │   │    DataReader        │
        │   (Interface)        │   │   (Interface)        │
        └──────────────────────┘   └──────────────────────┘
                  ↓                         ↓
        ┌──────────────────────┐   ┌──────────────────────┐
        │ HybridDataWriter     │   │ HybridDataReader     │
        │ (구현체)              │   │ (구현체)              │
        └──────────────────────┘   └──────────────────────┘
                  ↓                         ↓
        ┌──────────────────────┐   ┌──────────────────────┐
        │  node.bin            │   │  node.bin            │
        │  edge.bin            │   │  edge.bin            │
        │  (직렬 쓰기)           │   │  (순차/메모리 매핑)   │
        └──────────────────────┘   └──────────────────────┘
```

## 컴포넌트 설명

### 1. DataWriter (Interface)
**책임**: 데이터 저장

**메서드**:
- `saveNode(Node)`: 노드 저장 (자동 오프셋)
- `saveNode(Node, long offset)`: 노드 저장 (지정 오프셋)
- `saveEdge(Edge)`: 엣지 저장 (자동 오프셋)
- `saveEdge(Edge, long offset)`: 엣지 저장 (지정 오프셋)
- `overwriteNode/overwriteEdge()`: 기존 데이터 덮어쓰기
- `saveNodeIndex()`: 인덱스 정보 저장
- `allocateNodeFileSpace/allocateEdgeFileSpace()`: 파일 공간 할당

### 2. HybridDataWriter (Implementation)
**특징**:
- Node/Edge를 이진 형식으로 파일에 저장
- 좌표 → 오프셋 인덱싱 (HashMap 사용)
- 추출 단계에서 사용

**저장 형식**:
```
Node: [id(4) | startEdgeOffset(4) | lon(8) | lat(8)] = 24바이트
Edge: [id(4) | from(4) | to(4) | distance(8) | nextOffset(4)] = 24바이트
```

### 3. DataReader (Interface)
**책임**: 데이터 읽기

**메서드**:
- `readNode(long offset)`: 노드 읽기
- `readEdge(long offset)`: 엣지 읽기
- `getNodeOffset(Coordinate)`: 좌표로 노드 오프셋 조회
- `hasExtractedData()`: 데이터 존재 여부 확인
- `switchToMappingMode()`: 메모리 매핑 모드 전환

### 4. HybridDataReader (Implementation)
**특징**:
- 초기: 순차 읽기 (FileChannel 사용)
- 최적화: 메모리 매핑 읽기 (MappedByteBuffer 사용)
- 경로탐색 단계에서 사용

**모드 전환**:
```
추출 완료 후 경로탐색 시작
    ↓
switchToMappingMode() 호출
    ↓
MappedByteBuffer로 전환 (고속 읽기)
```

### 5. SeparatedHybridDataStore (Facade)
**책임**: 
- DataReader/Writer 통합 관리
- 단계별 초기화 (추출/경로탐색)
- 하위 호환성 유지 (기존 DataStore 인터페이스 구현)

**사용 패턴**:
```
// 추출 단계
DataStore store = new SeparatedHybridDataStore(filePath, provider);
store.saveNode(node);  // Writer 사용

// 경로탐색 단계 (새로운 인스턴스)
DataStore store = new SeparatedHybridDataStore(filePath, provider, true);
store.readNode(offset);  // Reader 사용
store.switchToMappingMode();  // 메모리 매핑 전환
```

## 주요 개선 사항

### 1. 코드 복잡성 감소
- 기존: 약 264줄의 혼합 로직
- 변경 후: 각각 120-150줄의 명확한 책임

### 2. 유지보수성 향상
- 읽기 로직 개선 시 HybridDataReader만 수정
- 쓰기 로직 개선 시 HybridDataWriter만 수정

### 3. 확장성 개선
- 새로운 저장소 전략 추가 가능
  - 예: `DatabaseDataWriter`, `RedisDataReader`
- 인터페이스 구현만으로 추가 가능

### 4. 성능 최적화
- Reader: 단계별 최적화 (순차 → 메모리 매핑)
- Writer: 좌표 인덱싱으로 빠른 조회

### 5. 테스트 용이성
- 각 컴포넌트 독립적 테스트 가능
- Mock 객체 사용 용이

## 사용 예제

### 추출 단계
```java
// 데이터 추출
DataStore writer = new SeparatedHybridDataStore(filePath, provider);

for (Node node : nodes) {
    writer.saveNode(node, nodeId * NODE_SIZE);
}

for (Edge edge : edges) {
    writer.saveEdge(edge, edgeId * EDGE_SIZE);
}

writer.saveNodeIndex(indexInfo);
writer.close();
```

### 경로탐색 단계
```java
// 경로 탐색 (읽기 모드)
DataStore reader = new SeparatedHybridDataStore(filePath, provider, true);

// 초기: 순차 읽기
Node startNode = reader.readNode(startOffset);

// 최적화: 메모리 매핑 모드 전환
reader.switchToMappingMode();

// 이후 모든 읽기는 고속 메모리 매핑
Node node = reader.readNode(offset);
Edge edge = reader.readEdge(offset);

reader.close();
```

## 호환성

### 기존 코드와의 호환성
- `SeparatedHybridDataStore`는 `MappableDataStore` 인터페이스 구현
- 기존 `HybridDataStore` 대체 가능
- 기존 `Engine`, `Extractor` 클래스 수정 불필요

### 마이그레이션
```java
// Before
DataStore store = new HybridDataStore(filePath, provider);

// After (동일하게 작동)
DataStore store = new SeparatedHybridDataStore(filePath, provider);
```

## 향후 확장 가능성

### 1. 비동기 I/O 지원
```java
public interface AsyncDataReader extends DataReader {
    CompletableFuture<Node> readNodeAsync(long offset);
    CompletableFuture<Edge> readEdgeAsync(long offset);
}
```

### 2. 캐싱 전략
```java
public class CachedDataReader implements DataReader {
    private DataReader delegate;
    private Map<Long, Node> cache;
    
    @Override
    public Node readNode(long offset) {
        return cache.computeIfAbsent(offset, 
            k -> delegate.readNode(k));
    }
}
```

### 3. 다양한 저장소 지원
```java
DataWriter dbWriter = new DatabaseDataWriter(connection);
DataReader cacheReader = new RedisDataReader(redisClient);
```

## 결론

이번 아키텍처 재설계는 다음을 실현합니다:
- ✅ 관심사의 명확한 분리
- ✅ 높은 코드 응집력
- ✅ 낮은 결합도
- ✅ 우수한 테스트 가능성
- ✅ 용이한 확장성
- ✅ 기존 호환성 유지
