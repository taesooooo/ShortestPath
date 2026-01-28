# Store 폴더 클래스 설계도

## 패키지 구조

```
com.shortestpath.shortestpath.core.pathengine.Store/
├── ARCHITECTURE.md                    # 상세 설계 문서
├── 
├── [Interface Layer]
├── ├── DataStore.java                 # 기존 인터페이스 (호환성 유지)
├── ├── MappableDataStore.java         # DataStore 확장 인터페이스
├── ├── DataReader.java                # 읽기 전담 인터페이스 (NEW)
├── └── DataWriter.java                # 쓰기 전담 인터페이스 (NEW)
├──
├── [Implementation Layer - Reader]
├── └── HybridDataReader.java          # 읽기 구현체 (NEW)
│       ├─ 순차 읽기 (FileChannel)
│       └─ 메모리 매핑 읽기 (MappedByteBuffer)
├──
├── [Implementation Layer - Writer]
├── └── HybridDataWriter.java          # 쓰기 구현체 (NEW)
│       ├─ 바이너리 형식 저장
│       └─ 좌표 인덱싱
├──
├── [Facade/Adapter Layer]
├── ├── HybridDataStore.java           # 기존 구현체 (유지)
├── └── SeparatedHybridDataStore.java  # 새로운 통합 구현체 (NEW)
│       ├─ Reader/Writer 위임
│       ├─ 단계별 초기화
│       └─ 하위 호환성 제공
```

## 클래스 다이어그램 (상세)

```
┌─────────────────────────────────────┐
│       <<interface>>                 │
│          DataStore                  │
├─────────────────────────────────────┤
│ + saveNode(Node): int               │
│ + saveEdge(Edge): int               │
│ + readNode(long): Node              │
│ + readEdge(long): Edge              │
│ + getNodeOffset(Coordinate): int    │
│ + hasExtractedData(): boolean       │
│ + close(): void                     │
└─────────────────────────────────────┘
           △                  △
           │                  │
    ┌──────┴─────────┐    ┌───┴──────────┐
    │                │    │              │
    │ extends        │    │ implements   │
    │                │    │              │
┌───┴───────────────────────────────────┴───┐
│    <<interface>>                          │
│      MappableDataStore                    │
├───────────────────────────────────────────┤
│ + switchToMappingMode(): void             │
└───┬───────────────────────────────────────┘
    │
    │ implements
    │
    ├─────────────────────────────────────┐
    │                                     │
┌───┴──────────────────────┐   ┌──────────┴────────────────┐
│   HybridDataStore        │   │ SeparatedHybridDataStore  │
│   (기존 구현)             │   │ (NEW - Facade)            │
├──────────────────────────┤   ├───────────────────────────┤
│ - nodeFileChannel        │   │ - dataWriter: DataWriter  │
│ - edgeFileChannel        │   │ - dataReader: DataReader  │
│ - nodeMappedBuffer       │   │ - fileDirectory: String   │
│ - edgeMappedBuffer       │   ├───────────────────────────┤
│ - graphRead: boolean     │   │ + saveNode(): int         │
│ - nodeIndexProvider      │   │ + saveEdge(): int         │
├──────────────────────────┤   │ + readNode(): Node        │
│ + saveNode(): int        │   │ + readEdge(): Edge        │
│ + saveEdge(): int        │   │ + getNodeOffset(): int    │
│ + readNode(): Node       │   │ + close(): void           │
│ + readEdge(): Edge       │   │ + switchToMappingMode()   │
│ + overwriteNode(): int   │   └───────────────────────────┘
│ + overwriteEdge(): int   │            △
│ + getNodeOffset(): int   │            │ uses (위임)
│ + hasExtractedData()     │            │
│ + close(): void          │            │
│ + switchMapMode()        │    ┌───────┴──────────┬─────────────┐
│ + allocateFileSpace()    │    │                  │             │
└──────────────────────────┘    │                  │             │
                            ┌───┴──────────┐  ┌────┴────────┐
                            │ DataWriter   │  │ DataReader  │
                            │ <<interface>>│  │ <<interface>>│
                            └───┬──────────┘  └────┬────────┘
                                │                  │
                                │ implements       │ implements
                                │                  │
                        ┌───────┴──────────┐   ┌───┴──────────────┐
                        │HybridDataWriter  │   │HybridDataReader  │
                        │(NEW)             │   │(NEW)             │
                        ├──────────────────┤   ├──────────────────┤
                        │- nodeFileChannel │   │- nodeFileChannel │
                        │- edgeFileChannel │   │- edgeFileChannel │
                        │- coordinateMap   │   │- mappedBuffer    │
                        ├──────────────────┤   │- graphRead       │
                        │+ saveNode(): int │   ├──────────────────┤
                        │+ saveEdge(): int │   │+ readNode():Node │
                        │+ overwriteNode() │   │+ readEdge():Edge │
                        │+ overwriteEdge() │   │+ getNodeOffset() │
                        │+ allocateSpace() │   │+ switchToMapping│
                        │+ saveNodeIndex() │   │+ hasExtractedData│
                        └──────────────────┘   └──────────────────┘
```

## 상호작용 시퀀스

### 추출 단계 (데이터 쓰기)

```
Extractor                  SeparatedStore          DataWriter          File
    │                             │                    │                │
    ├─ new(..., null) ──────────>│                    │                │
    │                             ├─ new Writer() ──→ │                │
    │                             │                    ├─ open() ──────→│
    │                             │                    │<─ FileChannel─┤
    │                             │<─────────────────┤                │
    │<──────────────────────────┤                    │                │
    │                             │                    │                │
    ├─ saveNode(node) ──────────>│                    │                │
    │                             ├─ saveNode() ────→ │                │
    │                             │                    ├─ write() ─────→│
    │                             │                    │<─ offset ──────┤
    │                             │<────────────────┤                │
    │<──────────────────────────┤                    │                │
    │                             │                    │                │
    ├─ close() ─────────────────>│                    │                │
    │                             ├─ close() ────────→ │                │
    │                             │                    ├─ close() ─────→│
    │                             │                    │<─ closed ──────┤
    │                             │<────────────────┤                │
    │<──────────────────────────┤                    │                │
```

### 경로탐색 단계 (데이터 읽기)

```
Engine                     SeparatedStore          DataReader          File
    │                             │                    │                │
    ├─ new(..., provider, true)─>│                    │                │
    │                             ├─ new Reader() ──→ │                │
    │                             │                    ├─ open() ──────→│
    │                             │                    │<─ FileChannel─┤
    │                             │                    │ (READ MODE)    │
    │                             │<────────────────┤                │
    │<──────────────────────────┤                    │                │
    │                             │                    │                │
    ├─ switchToMappingMode()───>│                    │                │
    │                             ├─ switchMapping()──→ │                │
    │                             │                    ├─ map() ───────→│
    │                             │                    │<─ MappedBuffer─┤
    │                             │<────────────────┤                │
    │<──────────────────────────┤                    │                │
    │                             │                    │                │
    ├─ readNode(offset) ────────>│                    │                │
    │                             ├─ readNode() ────→ │                │
    │                             │                    ├─ position() ──→│
    │                             │                    ├─ getInt()... →│
    │                             │                    │<─ Node data────┤
    │                             │<────────────────┤                │
    │<──────────────────────────┤                    │                │
    │  (고속! 메모리 매핑 사용)    │                    │  (캐시됨)      │
```

## 데이터 흐름

### 저장 형식 (이진)

```
┌─────────────────────────────────────────────┐
│  Node Data (24 bytes)                       │
├─────────────────────────────────────────────┤
│ ID (4 bytes)        │ [00 00 00 42]          │
│ StartEdgeOffset (4) │ [00 00 00 C8]          │
│ Longitude (8 bytes) │ [40 5E D0 00 00 00 00] │
│ Latitude (8 bytes)  │ [40 40 80 00 00 00 00] │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│  Edge Data (24 bytes)                       │
├─────────────────────────────────────────────┤
│ ID (4 bytes)        │ [00 00 00 01]          │
│ From (4 bytes)      │ [00 00 00 02]          │
│ To (4 bytes)        │ [00 00 00 03]          │
│ Distance (8 bytes)  │ [40 59 00 00 00 00 00] │
│ NextEdgeOffset (4)  │ [FF FF FF FF]          │
└─────────────────────────────────────────────┘
```

## 메모리 구조 (읽기 최적화)

```
메모리 매핑 전 (순차 읽기)
─────────────────────────────
Disk File
    │
    ├─ Node 0
    ├─ Node 1
    ├─ Node 2  ← FileChannel.read() 호출마다 디스크 접근
    ├─ Node 3
    └─ ...

메모리 매핑 후 (고속 읽기)
─────────────────────────────
Disk File
    │
    └─→ Memory Mapped Buffer (OS 캐시)
            │
            ├─ Node 0
            ├─ Node 1
            ├─ Node 2  ← position() + getInt() 만으로 접근 (메모리)
            ├─ Node 3
            └─ ...

        캐시 히트율 ↑↑↑ (훨씬 빠름)
```

## 인덱싱 전략

```
좌표 인덱싱 (HybridDataWriter)
──────────────────────────────

Node 저장 시:
    Coordinate(33.456, 123.456)
            ↓
    Key: "33.456,123.456"
            ↓
    coordinateNodeIndexMap.put(key, offset)
            ↓
┌──────────────────────────────┐
│ HashMap<String, Integer>      │
├──────────────────────────────┤
│"33.456,123.456" → 0          │
│"33.457,123.457" → 24         │
│"33.458,123.458" → 48         │
│...                           │
└──────────────────────────────┘

좌표로 오프셋 조회:
    getNodeOffset(Coordinate) → O(1) 시간복잡도
```

## 코드 라인 수 비교

```
┌────────────────────────────────────────┐
│ 클래스별 라인 수                        │
├────────────────────────────────────────┤
│ HybridDataStore (기존)     │ ~264 lines │
│ ├─ 읽기 로직                │ ~100 lines │
│ ├─ 쓰기 로직                │ ~100 lines │
│ └─ 혼합 로직                │  ~64 lines │
├────────────────────────────────────────┤
│ 새로운 구조                 │ ~420 lines │
│ ├─ DataReader (interface)  │  ~40 lines │
│ ├─ DataWriter (interface)  │  ~80 lines │
│ ├─ HybridDataReader        │ ~140 lines │
│ ├─ HybridDataWriter        │ ~140 lines │
│ └─ SeparatedHybridDataStore│  ~140 lines │
├────────────────────────────────────────┤
│ 개선사항                                │
│ ✓ 명확한 책임 분리         │            │
│ ✓ 각 200줄 이하 (관리용이)  │            │
│ ✓ 독립적 테스트 가능        │            │
│ ✓ 확장성 향상              │            │
└────────────────────────────────────────┘
```

## 사용 시나리오

### 시나리오 1: 단순 구현 유지 (기존 코드)

```java
// 호환성 유지 - 기존 코드와 동일
DataStore store = new HybridDataStore(filePath, provider);
store.saveNode(node);
Node n = store.readNode(offset);
store.close();
```

### 시나리오 2: 새로운 구조 활용 (분리된 구현)

```java
// 추출 단계
DataStore writer = new SeparatedHybridDataStore(filePath, provider);
// writer.saveNode(), writer.saveEdge() ...
writer.close();

// 경로탐색 단계 (새로운 인스턴스)
DataStore reader = new SeparatedHybridDataStore(filePath, provider, true);
reader.switchToMappingMode();  // 최적화
// reader.readNode(), reader.readEdge() ...
reader.close();
```

### 시나리오 3: 다양한 저장소 구현

```java
// 향후 추가 가능한 구현들
DataWriter dbWriter = new DatabaseDataWriter(connection);
DataReader cacheReader = new RedisCachedDataReader(redis);
DataReader s3Reader = new S3DataReader(bucket);
```

## 성능 예상

| 작업 | 기존 방식 | 최적화 후 | 개선율 |
|-----|---------|---------|-------|
| 순차 읽기 (메모리 매핑 전) | 100% | 100% | - |
| 메모리 매핑 읽기 | - | 80-90% ↓ | ~15% 개선 |
| 좌표 조회 | O(n) | O(1) | 대폭 개선 |
| 코드 관리성 | 낮음 | 높음 | 매우 개선 |

---

**설계 완료** ✓ Store 폴더가 CQRS 패턴으로 재설계되었습니다!
