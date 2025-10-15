# 백엔드 인덱스 완벽 정리

## 목차
1. [인덱스 동작 원리](#인덱스-동작-원리)
2. [복합 인덱스 스터디](#복합-인덱스-스터디)
3. [실전 쿼리 최적화](#실전-쿼리-최적화)

---

## 인덱스 동작 원리

### 시간 복잡도
- **인덱스 접근**: O(log N) - B-Tree 구조
- **배열 인덱스 접근**: O(1) - 메모리 주소 계산
- **Full Table Scan**: O(N)

### 인덱스 종류별 특성

| 자료구조 | 조회 시간 | 특징 |
|---------|----------|------|
| Array | O(1) | 인덱스 직접 접근 |
| ArrayList | O(1) | get(index) |
| LinkedList | O(N) | 순차 탐색 필요 |
| HashMap | O(1) 평균 | key 기반 |
| TreeMap | O(log N) | 정렬된 구조 |

---

## 복합 인덱스 스터디

### 테스트 환경
- **Book 테이블**: 500만 건
- **category 컬럼**: 20개 카테고리
- **author 컬럼**: 작가 (동명이인 가능)

```sql
SELECT * FROM book WHERE category = ? AND author = ?
```

### 인덱스 옵션
- A: `(category)` - 단일 인덱스
- B: `(author)` - 단일 인덱스
- C: `(category, author)` - 복합 인덱스
- D: `(author, category)` - 복합 인덱스

---

## 실전 쿼리 최적화

### Q1. C(category, author) vs D(author, category) 차이

**정답: 동등 조건(exact match)이므로 속도 차이 거의 없음**

#### 이유
```sql
-- 두 조건 모두 = 연산자 사용
WHERE category = ? AND author = ?
```

- **C 인덱스**: category(25만건) → author 필터링
- **D 인덱스**: author(수천건) → category 필터링
- 최종적으로 **같은 행** 찾음
- **B-Tree 특성상 동등 조건은 효율적**

#### 핵심 원리
> 복합 인덱스에서 **모든 컬럼이 동등 조건(=)** 이면 순서와 관계없이 비슷한 성능

---

### Q2. 가장 느린 인덱스는?

**정답: B (author)**

#### 동작 과정 비교

**A (category):**
```
1. category 인덱스로 25만건 필터링
2. 25만건에 대해 author Full Scan
3. 순차 접근 (Sequential I/O)
```

**B (author):**
```
1. author 인덱스로 수천건 필터링
2. 각 행마다 테이블 접근하여 category 확인
3. 랜덤 I/O 수천 번 발생 ⚠️
```

**C, D (복합):**
```
1. 두 조건 모두 인덱스에서 처리
2. 테이블 접근 최소화
```

#### 핵심 원리
> **랜덤 I/O > Sequential I/O** (비용 측면)
>
> 단일 인덱스는 한 조건만 처리 → 나머지는 테이블 스캔

---

### Q3. B(author) vs D(author, category) 차이

**정답:**
- **B**: 동명이인 모두 찾음 → **카테고리 관계없이 항상 랜덤 I/O**
- **D**: 인덱스 단에서 category 필터링 → **일치하는 것만 랜덤 I/O**

#### 예시: 김철수 검색

```sql
-- 김철수 데이터: 소설 3건, 에세이 7건 (총 10건)
SELECT * FROM book WHERE author = '김철수' AND category = '소설';
```

**B (author) 인덱스:**
```
1. author='김철수' 10건 찾음
2. 10건 모두 테이블 접근 (10번 랜덤 I/O)
3. 테이블에서 category='소설' 확인
4. 최종 3건 반환
```

**D (author, category) 인덱스:**
```
1. author='김철수' 찾음
2. 인덱스 내에서 category='소설' 필터링
3. 3건만 테이블 접근 (3번 랜덤 I/O)
4. 최종 3건 반환
```

#### 핵심 원리
> **Covering Index** 효과
>
> 복합 인덱스는 인덱스 내에서 추가 필터링 가능 → 랜덤 I/O 최소화

---

### Q4. LIKE '%단어%' 쿼리 최적화

```sql
SELECT * FROM book WHERE category = ? AND author LIKE '%단어%'
```

**정답: C (category, author)**

#### 이유

**LIKE '%단어%' 특성:**
- 앞에 `%`가 있으면 **인덱스 정렬 순서 활용 불가**
- B-Tree는 왼쪽부터 매칭하는데 중간/끝 매칭은 불가능

**C 인덱스 동작:**
```
1. category로 25만건까지 인덱스 스캔 ✅
2. 인덱스 내 author 값으로 LIKE 검색 (Covering Index)
3. 테이블 접근 없이 인덱스만으로 처리 가능
```

**A vs C 비교:**
- A: category만 → 테이블 접근 필요
- **C**: category + author 모두 인덱스에 포함 → **Covering Index**

#### 핵심 원리
> LIKE '%...%'는 인덱스 정렬 못 씀
>
> But, **Covering Index**로 테이블 접근 줄일 수 있음

---

### Q5. author LIKE '%단어%' 단독 쿼리

```sql
SELECT * FROM book WHERE author LIKE '%단어%'
```

**정답: B (author)**

#### 이유

**조건:**
- 인덱스 정렬 활용 불가
- Full Scan 필요

**B 인덱스 선택 이유:**
```
1. author 인덱스 Full Scan
2. 인덱스는 테이블보다 훨씬 작음
3. author 값만 확인하면 됨 (Covering Index)
4. 테이블 접근 없이 LIKE 검색
```

**크기 비교:**
- **B**: author만 → 작음
- C, D: category + author → 큼
- 테이블: 모든 컬럼 → 가장 큼

#### 핵심 원리
> Index Full Scan도 Table Full Scan보다 빠름
>
> 인덱스가 작고, 필요한 컬럼만 포함되어 있으면 효율적

---

### Q6. 복합 인덱스의 비효율적 사용

```sql
SELECT * FROM book WHERE author = ?
```

**C (category, author) 인덱스 동작:**

**정답: Index Full Scan (리프 노드 전체 읽음) → author 일치 시만 랜덤 I/O**

#### 인덱스 구조

```
C 인덱스: (category, author) 순 정렬

(소설, 김철수)
(소설, 박영희)
(소설, 이순신)
(에세이, 김철수)  ← 김철수가 흩어져 있음
(에세이, 박영희)
(시, 김철수)      ← 여기도 김철수
```

#### 동작 과정

```
1. category 조건 없음 → 첫 번째 컬럼 활용 불가
2. 인덱스 리프 노드 처음부터 끝까지 읽음 (Index Full Scan)
3. 각 노드에서 author='김철수' 확인
4. 일치하면 테이블로 랜덤 I/O
```

#### 비효율적인 이유

- **정렬 순서**: category 우선 → author는 흩어져 있음
- **탐색 불가**: B-Tree 탐색 불가 → Full Scan 필요
- **B 인덱스 대비**: author 단일 인덱스면 바로 찾음

#### 올바른 선택

**B (author) 인덱스:**
```
1. author='김철수' B-Tree 탐색 O(log N)
2. 연속된 위치에 모여 있음
3. 효율적인 Range Scan
```

#### 핵심 원리
> 복합 인덱스는 **왼쪽 컬럼부터** 사용해야 효율적
>
> 중간 컬럼만 사용 = Index Full Scan → 비효율

---

## 인덱스 설계 핵심 원칙

### 1. 선택도(Selectivity) 고려

**잘못된 생각:**
> "카테고리 칼럼이 적으니까 먼저" ❌

**올바른 생각:**
> "선택도가 높은(데이터를 많이 걸러내는) 컬럼을 먼저" ✅

- **category**: 20개 → 선택도 낮음 (500만 ÷ 20 = 25만건)
- **author**: 수천~수만 명 → 선택도 높음 (수천건)

### 2. 동등 조건 vs 범위 조건

| 조건 타입 | 인덱스 순서 영향 | 이유 |
|----------|---------------|------|
| 모두 `=` | 거의 없음 | B-Tree 효율적 탐색 |
| `=` + 범위 | 큼 | 범위는 뒤로 |
| LIKE '%...' | 정렬 활용 불가 | Covering Index만 고려 |

### 3. Covering Index 활용

**정의**: 쿼리에 필요한 모든 컬럼이 인덱스에 포함

**장점:**
- 테이블 접근 없이 인덱스만으로 처리
- 랜덤 I/O 최소화
- Full Scan도 효율적

**예시:**
```sql
-- author 컬럼만 필요
SELECT author FROM book WHERE author LIKE '%김%'

-- B(author) 인덱스면 Covering Index
-- 테이블 접근 없이 인덱스만 스캔
```

### 4. 랜덤 I/O vs Sequential I/O

**비용 순서:**
```
랜덤 I/O >> Sequential I/O > 인덱스 스캔
```

**최적화 우선순위:**
1. 랜덤 I/O 횟수 줄이기
2. Covering Index로 테이블 접근 제거
3. 인덱스 크기 줄이기

### 5. 복합 인덱스 컬럼 순서

**원칙:**
1. **동등 조건(=)** 우선
2. **선택도 높은** 컬럼 우선
3. **범위 조건**은 마지막

**예시:**
```sql
-- 쿼리
WHERE status = ? AND created_at > ? AND category = ?

-- 최적 인덱스
(status, category, created_at)

-- 이유
-- status, category: 동등 조건
-- created_at: 범위 조건 → 마지막
```

---

## 실전 체크리스트

### 인덱스 생성 전 확인사항

- [ ] 쿼리의 WHERE 조건 분석
- [ ] 동등 조건 / 범위 조건 구분
- [ ] 각 컬럼의 선택도 확인
- [ ] Covering Index 가능성 검토
- [ ] 기존 인덱스와 중복 여부

### 쿼리 최적화 순서

1. **EXPLAIN ANALYZE** 실행
2. Index Scan / Full Scan 확인
3. 랜덤 I/O 횟수 확인
4. Covering Index 적용 가능성
5. 인덱스 추가/수정
6. 재측정 및 비교

---

## 마무리

### 핵심 요약

1. **동등 조건**은 복합 인덱스 순서 영향 적음
2. **랜덤 I/O** 횟수가 성능의 핵심
3. **Covering Index**는 테이블 접근 제거
4. **LIKE '%...'**는 정렬 못 쓰지만 Covering은 가능
5. **복합 인덱스**는 왼쪽 컬럼부터 순서대로 사용

### 자주 하는 실수

❌ "컬럼 개수가 적으면 앞에"
✅ "선택도가 높으면 앞에"

❌ "LIKE '%...'는 인덱스 무용지물"
✅ "Covering Index로 활용 가능"

❌ "복합 인덱스는 무조건 빠르다"
✅ "왼쪽 컬럼부터 사용해야 효율적"

---

**작성일**: 2025-10-16
**버전**: 1.0
