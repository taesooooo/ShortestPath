# Store 폴더 인터페이스 분리 설계 (최종)

## 핵심 개념: 인터페이스 분리 원칙 (Interface Segregation Principle)

클라이언트는 자신이 사용하지 않는 메서드에 의존하지 않아야 합니다.

## 인터페이스 계층 구조

```
┌─────────────────────────────────────┐
│       <<interface>>                 │
│          DataStore (기본)            │
├─────────────────────────────────────┤
│ + saveNode(Node): int               │
│ + saveNode(Node, offset): int       │
│ + saveEdge(Edge): int               │
│ + saveEdge(Edge, offset): int       │
│ + overwriteNode(Node, offset): int  │
│ + overwriteEdge(Edge, offset): int  │
│ + readNode(long): Node              │
│ + readEdge(long): Edge              │
│ + saveNodeIndex(List): void         │
│ + getNodeOffset(Coordinate): int    │
│ + hasExtractedData(): boolean       │
│ + close(): void                     │
│ + allocateNodeFileSpace(long): void │
│ + allocateEdgeFileSpace(long): void │
└─────────────────────────────────────┘
         △                  △
         │                  │
         └──────┬───────────┘
                │
        extends (구현)
                │
   ┌────────────┴────────────┐
   │                         │
┌──┴─────────────────┐   ┌───┴──────────────────┐
│  MappableDataStore │   │   (분리된 읽기)       │
│  (기본 호환)        │   │   (분리된 쓰기)       │
└──────┬──────────────┘   └────────┬─────────────┘
       │                           │
       │ (이전 방식)         (새로운 방식)
       │
   HybridDataStore        SeparatedHybridDataStore
```

## 분리된 인터페이스

### 1. 기본 인터페이스 (필수)

#### DataReader (읽기 기본)
```java
public interface DataReader {
    Node readNode(long offset) throws IOException;
    Edge readEdge(long offset) throws IOException;
    boolean hasExtractedData();
    void close() throws IOException;
}
```

#### DataWriter (쓰기 기본)
```java
public interface DataWriter {
    int saveNode(Node) throws IOException;
    int saveNode(Node, long offset) throws IOException;
    int saveEdge(Edge) throws IOException;
    int saveEdge(Edge, long offset) throws IOException;
    int overwriteNode(Node, long offset) throws IOException;
    int overwriteEdge(Edge, long offset) throws IOException;
    void saveNodeIndex(List<IndexInfo>) throws IOException;
    void close() throws IOException;
}
```

### 2. 확장 인터페이스 (선택적)

#### MappableDataReader (메모리 매핑 지원)
```java
public interface MappableDataReader extends DataReader {
    void switchToMappingMode() throws IOException;
}
```

#### IndexableDataReader (좌표 인덱싱 지원)
```java
public interface IndexableDataReader extends DataReader {
    int getNodeOffset(Coordinate coordinate);
}
```

#### AllocatableDataWriter (파일 공간 할당 지원)
```java
public interface AllocatableDataWriter extends DataWriter {
    void allocateNodeFileSpace(long size) throws IOException;
    void allocateEdgeFileSpace(long size) throws IOException;
}
```

## 구현체 설계

### HybridDataReader
- ✅ `DataReader` 구현 (필수)
- ✅ `MappableDataReader` 구현 (선택)
- ✅ `IndexableDataReader` 구현 (선택)

```java
public class HybridDataReader 
    implements MappableDataReader, IndexableDataReader {
    // 모든 인터페이스 메서드 구현
}
```

### HybridDataWriter
- ✅ `DataWriter` 구현 (필수)
- ✅ `AllocatableDataWriter` 구현 (선택)

```java
public class HybridDataWriter 
    implements AllocatableDataWriter {
    // 모든 인터페이스 메서드 구현
}
```

## 사용 패턴

### 패턴 1: 기본 읽기/쓰기만 필요한 경우

```java
// 읽기만 필요
DataReader reader = new SimpleDataReader(filePath);
Node node = reader.readNode(offset);
reader.close();

// 쓰기만 필요
DataWriter writer = new SimpleDataWriter(filePath);
writer.saveNode(node, offset);
writer.close();
```

### 패턴 2: 메모리 매핑이 필요한 경우

```java
// Reader가 MappableDataReader를 구현하는 경우
DataReader reader = new HybridDataReader(nodeFile, edgeFile);

if (reader instanceof MappableDataReader) {
    ((MappableDataReader) reader).switchToMappingMode();
}

Node node = reader.readNode(offset);
reader.close();
```

### 패턴 3: 좌표 인덱싱이 필요한 경우

```java
// Reader가 IndexableDataReader를 구현하는 경우
DataReader reader = new HybridDataReader(nodeFile, edgeFile);

if (reader instanceof IndexableDataReader) {
    int offset = ((IndexableDataReader) reader)
        .getNodeOffset(coordinate);
}

Node node = reader.readNode(offset);
reader.close();
```

### 패턴 4: 파일 공간 할당이 필요한 경우

```java
// Writer가 AllocatableDataWriter를 구현하는 경우
DataWriter writer = new HybridDataWriter(filePath);

if (writer instanceof AllocatableDataWriter) {
    ((AllocatableDataWriter) writer)
        .allocateNodeFileSpace(1024 * 1024);
}

writer.saveNode(node, offset);
writer.close();
```

## 확장 시나리오

### 시나리오 1: 메모리 매핑을 지원하지 않는 Reader 추가

```java
// 메모리 부족 환경에서만 순차 읽기 사용
public class SimpleDataReader implements DataReader {
    // DataReader 메서드만 구현
    // MappableDataReader 미구현
}

// 사용
DataReader reader = new SimpleDataReader(filePath);
// switchToMappingMode() 호출 불가 (구현하지 않음)
```

### 시나리오 2: 데이터베이스 기반 Reader

```java
public class DatabaseDataReader 
    implements DataReader, IndexableDataReader {
    // 데이터베이스에서 데이터 읽기
    // 메모리 매핑 불필요 (DB가 최적화 담당)
}
```

### 시나리오 3: Redis 캐시 기반 Reader

```java
public class RedisCachedDataReader 
    implements DataReader, IndexableDataReader {
    // Redis에서 캐시된 데이터 읽기
    // 메모리 매핑 불필요 (Redis가 캐싱 담당)
}
```

### 시나리오 4: 메모리만 할당하는 Writer

```java
public class MemoryDataWriter 
    implements DataWriter {
    // 메모리만 사용하여 저장
    // 파일 공간 할당 불필요 (메모리는 자동 할당)
}
```

## 클래스 다이어그램

```
┌──────────────────────┐
│  <<interface>>       │
│   DataReader         │
└──────────┬───────────┘
           △
           │ extends
     ┌─────┴──────────┬─────────────────┐
     │                │                 │
┌────┴──────────┐ ┌──┴────────────────┐│
│Mappable       │ │ Indexable         ││
│DataReader     │ │ DataReader        ││
└────┬──────────┘ └──┬────────────────┘│
     │                │                 │
     └────────┬───────┴─────────────────┘
              │
      implements (선택)
              │
  ┌───────────┴────────────┐
  │                        │
┌─┴──────────────────┐  ┌──┴──────────────────┐
│ HybridDataReader   │  │ SimpleDataReader    │
│ - 메모리 매핑 ○    │  │ - 메모리 매핑 ✗     │
│ - 좌표 인덱싱 ○    │  │ - 좌표 인덱싱 ✗     │
└────────────────────┘  └─────────────────────┘


┌──────────────────────┐
│  <<interface>>       │
│   DataWriter         │
└──────────┬───────────┘
           △
           │ extends
           │
    ┌──────┴──────────┐
    │                 │
┌───┴────────────────┐
│ Allocatable        │
│ DataWriter         │
└───┬────────────────┘
    │
    │ implements (선택)
    │
    ┌─────────────────────────────┬──────────────────┐
    │                             │                  │
┌───┴──────────────┐    ┌────────┴──────────┐  ┌────┴───────────────┐
│ HybridDataWriter │    │MemoryDataWriter   │  │DatabaseDataWriter  │
│ - 파일 할당 ○    │    │ - 파일 할당 ✗     │  │ - 파일 할당 ✗      │
└──────────────────┘    └───────────────────┘  └────────────────────┘
```

## 마이그레이션 전략

### 기존 코드 (호환성 유지)

```java
// 기존 방식 - 그대로 작동
DataStore store = new HybridDataStore(filePath, provider);
store.saveNode(node);
store.readNode(offset);
store.switchToMappingMode();
store.allocateNodeFileSpace(size);
store.close();
```

### 새로운 코드 (선택적 최적화)

```java
// 필요한 메서드만 사용
DataWriter writer = new HybridDataWriter(filePath);
writer.saveNode(node);  // 기본 메서드만 사용
writer.close();

// 고급 기능이 필요하면 캐스팅
if (writer instanceof AllocatableDataWriter) {
    ((AllocatableDataWriter) writer).allocateNodeFileSpace(size);
}
```

## 인터페이스 분리의 이점

| 항목 | 기존 | 개선 후 |
|------|------|--------|
| **필수 메서드** | 모두 구현 | 필요한 것만 구현 |
| **이해도** | 복잡 | 단순 명확 |
| **테스트** | 어려움 | 쉬움 |
| **확장성** | 제한적 | 매우 높음 |
| **유지보수** | 어려움 | 수월 |

## 체크리스트: 새 구현체 추가 시

새로운 Reader/Writer를 추가할 때:

```
[ ] DataReader / DataWriter 구현
[ ] MappableDataReader 필요?
    [ ] Yes → 구현
    [ ] No → 생략
[ ] IndexableDataReader 필요?
    [ ] Yes → 구현
    [ ] No → 생략
[ ] AllocatableDataWriter 필요?
    [ ] Yes → 구현
    [ ] No → 생략
[ ] SeparatedHybridDataStore에서 instanceof 체크로 사용
```

---

**최종 설계**: 인터페이스 분리 원칙으로 확장 가능한 구조! 🎯
