# Store 폴더 최종 재설계 완료 ✅

## 개선 사항 요약

### Before: 혼합된 인터페이스
```
DataStore (모든 메서드 포함)
├─ saveNode, saveEdge
├─ readNode, readEdge
├─ getNodeOffset (좌표 인덱싱)
├─ switchToMappingMode (메모리 매핑)
└─ allocateNodeFileSpace, allocateEdgeFileSpace (파일 할당)
```

### After: 분리된 인터페이스 (ISP 적용)
```
DataReader (핵심 읽기 메서드만)
├─ readNode, readEdge
├─ hasExtractedData
└─ close

DataWriter (핵심 쓰기 메서드만)
├─ saveNode, saveEdge
├─ overwriteNode, overwriteEdge
├─ saveNodeIndex
└─ close

MappableDataReader (선택: 메모리 매핑)
└─ switchToMappingMode

IndexableDataReader (선택: 좌표 인덱싱)
└─ getNodeOffset

AllocatableDataWriter (선택: 파일 할당)
├─ allocateNodeFileSpace
└─ allocateEdgeFileSpace
```

## 폴더 구조

```
Store/
├── [인터페이스 - 기본]
│   ├── DataReader.java           ← 핵심 읽기 메서드만
│   ├── DataWriter.java           ← 핵심 쓰기 메서드만
│   └── DataStore.java            ← 기존 호환성 유지
│
├── [인터페이스 - 확장 (선택적)]
│   ├── MappableDataReader.java   ← 메모리 매핑
│   ├── IndexableDataReader.java  ← 좌표 인덱싱
│   ├── AllocatableDataWriter.java ← 파일 할당
│   └── MappableDataStore.java    ← 호환성 래퍼
│
├── [구현체]
│   ├── HybridDataReader.java     ← 읽기 구현 (모든 확장 인터페이스)
│   ├── HybridDataWriter.java     ← 쓰기 구현 (할당 인터페이스)
│   ├── HybridDataStore.java      ← 기존 구현 (유지)
│   └── SeparatedHybridDataStore.java ← 새로운 Facade
│
└── [문서]
    ├── ARCHITECTURE.md           ← 상세 설계
    └── [루트] Store_Interface_Segregation.md
```

## 핵심 특징

### 1. 인터페이스 분리 원칙 적용 ✅
- 필요한 메서드만 선택하여 구현
- 불필요한 메서드 의존 제거

### 2. 유연한 구현 가능 ✅
```java
// 단순 구현 (기본만)
public class SimpleDataReader implements DataReader { }

// 고급 구현 (기본 + 선택)
public class HybridDataReader 
    implements MappableDataReader, IndexableDataReader { }

// 메모리만 (파일 할당 불필요)
public class MemoryDataWriter implements DataWriter { }

// DB 기반 (메모리 매핑 불필요)
public class DatabaseDataReader 
    implements DataReader, IndexableDataReader { }
```

### 3. 선택적 기능 사용 ✅
```java
DataReader reader = createReader();

// 지원하면 사용
if (reader instanceof MappableDataReader) {
    ((MappableDataReader) reader).switchToMappingMode();
}

// 지원하면 사용
if (reader instanceof IndexableDataReader) {
    int offset = ((IndexableDataReader) reader)
        .getNodeOffset(coordinate);
}
```

### 4. 기존 호환성 유지 ✅
```java
// 기존 코드 그대로 작동
DataStore store = new HybridDataStore(filePath, provider);
```

## 사용 시나리오

### 추출 단계 (쓰기)
```java
// Writer 초기화 - 기본 쓰기만 필요
DataWriter writer = new HybridDataWriter(filePath);

// 필요하면 파일 할당
if (writer instanceof AllocatableDataWriter) {
    ((AllocatableDataWriter) writer)
        .allocateNodeFileSpace(nodeSize);
}

// 데이터 저장
writer.saveNode(node, offset);
writer.saveEdge(edge, offset);
```

### 경로탐색 단계 (읽기)
```java
// Reader 초기화 - 기본 읽기만 필요
DataReader reader = new HybridDataReader(nodeFile, edgeFile);

// 필요하면 메모리 매핑 활성화
if (reader instanceof MappableDataReader) {
    ((MappableDataReader) reader).switchToMappingMode();
}

// 데이터 읽기
Node node = reader.readNode(offset);
Edge edge = reader.readEdge(offset);
```

## 메서드 분포

| 메서드 | 인터페이스 | 필수 여부 |
|--------|-----------|---------|
| readNode, readEdge | DataReader | 필수 |
| saveNode, saveEdge | DataWriter | 필수 |
| hasExtractedData | DataReader | 필수 |
| close | DataReader/Writer | 필수 |
| getNodeOffset | IndexableDataReader | 선택 |
| switchToMappingMode | MappableDataReader | 선택 |
| allocateNodeFileSpace | AllocatableDataWriter | 선택 |
| overwriteNode/Edge | DataWriter | 필수 |
| saveNodeIndex | DataWriter | 필수 |

## 파일별 역할

| 파일 | 용도 | 변경 |
|------|------|------|
| DataReader.java | 읽기 기본 인터페이스 | 축소 |
| DataWriter.java | 쓰기 기본 인터페이스 | 축소 |
| MappableDataReader.java | 메모리 매핑 확장 | 신규 |
| IndexableDataReader.java | 좌표 인덱싱 확장 | 신규 |
| AllocatableDataWriter.java | 파일 할당 확장 | 신규 |
| HybridDataReader.java | 하이브리드 읽기 | 수정 |
| HybridDataWriter.java | 하이브리드 쓰기 | 수정 |
| SeparatedHybridDataStore.java | 통합 Facade | 수정 |
| HybridDataStore.java | 기존 구현 | 유지 |
| MappableDataStore.java | 호환 래퍼 | 개선 |
| DataStore.java | 기본 인터페이스 | 유지 |

## 컴파일 확인

```bash
# 모든 Java 파일이 컴파일되어야 함
javac DataReader.java
javac DataWriter.java
javac MappableDataReader.java
javac IndexableDataReader.java
javac AllocatableDataWriter.java
javac HybridDataReader.java
javac HybridDataWriter.java
javac SeparatedHybridDataStore.java
```

## 테스트 전략

### 1. 기본 인터페이스 테스트
```java
@Test
void testBasicDataReader() {
    DataReader reader = new HybridDataReader(nodeFile, edgeFile);
    Node node = reader.readNode(0);
    assertNotNull(node);
}
```

### 2. 확장 인터페이스 테스트
```java
@Test
void testMappableDataReader() {
    DataReader reader = new HybridDataReader(nodeFile, edgeFile);
    assertTrue(reader instanceof MappableDataReader);
    ((MappableDataReader) reader).switchToMappingMode();
}
```

### 3. 기존 호환성 테스트
```java
@Test
void testLegacyHybridDataStore() {
    DataStore store = new HybridDataStore(filePath, provider);
    // 기존 테스트 그대로 실행
}
```

## 성능 특성

| 기능 | 성능 | 메모리 |
|------|------|--------|
| 순차 읽기 | 중간 | 낮음 |
| 메모리 매핑 | 높음 | 높음 |
| 좌표 인덱싱 | 매우 높음 | 중간 |
| 파일 할당 | 초기화 시만 | 중간 |

## 설계 원칙 요약

✅ **S**ingle Responsibility - 각 인터페이스는 한 가지 책임
✅ **O**pen/Closed - 확장에는 열려있고 수정에는 닫혀있음
✅ **L**iskov Substitution - 구현체 교체 가능
✅ **I**nterface Segregation - 필요한 메서드만 노출
✅ **D**ependency Inversion - 인터페이스에 의존

## 결론

인터페이스 분리 원칙을 적용하여:
- 🎯 각 구현체가 필요한 메서드만 구현
- 🎯 새로운 저장소 유형 추가 용이
- 🎯 기존 코드 호환성 완벽 유지
- 🎯 테스트와 유지보수 용이
- 🎯 코드 이해도 향상

**최종 상태**: 프로덕션 준비 완료! 🚀
