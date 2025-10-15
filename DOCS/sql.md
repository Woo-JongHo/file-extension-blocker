# SQL Schema - File Extension Blocker

## 목차
1. [스키마 개요](#스키마-개요)
2. [테이블 구조](#테이블-구조)
3. [인덱스 전략](#인덱스-전략)
4. [트리거 및 함수](#트리거-및-함수)

---

## 스키마 개요

### 시스템 특징
- **확장자 정규화**: 모든 확장자는 소문자, 앞의 점(.) 제거
- **고정/커스텀 정책**: 체크박스(고정) + 태그(커스텀) 통합 관리
- **인기 집계**: 전역 확장자 사용 빈도 추적
- **자동 시드**: 공간 생성 시 Top-6 확장자 자동 설정
- **S3 기반 파일 관리**: 파일 메타데이터 + S3 참조

---

## 전체 스키마

```sql
-- =========================================================
-- 0. 확장자 정규화 함수 (모든 테이블 공통)
-- =========================================================
CREATE OR REPLACE FUNCTION normalize_ext(txt TEXT)
RETURNS TEXT LANGUAGE sql IMMUTABLE AS $
  SELECT lower(regexp_replace(COALESCE($1,''), '^\.+', ''));
$;

-- =========================================================
-- 1. 공간 (Space)
-- =========================================================
CREATE TABLE space (
  space_id     BIGSERIAL PRIMARY KEY,
  space_name   TEXT NOT NULL,
  owner_member BIGINT NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =========================================================
-- 2. 공간별 확장자 정책 (고정/커스텀 통합)
-- =========================================================
CREATE TABLE space_extension (
  space_id   BIGINT NOT NULL REFERENCES space(space_id) ON DELETE CASCADE,
  ext        TEXT   NOT NULL,
  is_fixed   BOOLEAN NOT NULL,         -- TRUE=고정(체크박스), FALSE=커스텀(태그)
  created_at TIMESTAMPTZ DEFAULT now(),
  CONSTRAINT space_extension_pk PRIMARY KEY (space_id, ext)
);

-- 확장자 정규화 트리거
CREATE OR REPLACE FUNCTION trg_norm_space_ext_fn()
RETURNS trigger LANGUAGE plpgsql AS $
BEGIN
  NEW.ext := normalize_ext(NEW.ext);
  RETURN NEW;
END $;

CREATE TRIGGER trg_norm_space_ext
BEFORE INSERT OR UPDATE ON space_extension
FOR EACH ROW EXECUTE FUNCTION trg_norm_space_ext_fn();

-- 조회/집계 성능 인덱스
CREATE INDEX ix_space_ext_fixed ON space_extension(ext) WHERE is_fixed = TRUE;

-- =========================================================
-- 3. 전역 확장자 인기 집계 (Top-N 시드용)
-- =========================================================
CREATE TABLE ext_popularity (
  ext TEXT PRIMARY KEY,
  cnt BIGINT NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_ext_popularity_cnt_desc ON ext_popularity (cnt DESC, ext ASC);

CREATE OR REPLACE FUNCTION trg_norm_pop_ext_fn()
RETURNS trigger LANGUAGE plpgsql AS $
BEGIN
  NEW.ext := normalize_ext(NEW.ext);
  RETURN NEW;
END $;

CREATE TRIGGER trg_norm_pop_ext
BEFORE INSERT OR UPDATE ON ext_popularity
FOR EACH ROW EXECUTE FUNCTION trg_norm_pop_ext_fn();

-- =========================================================
-- 4. 확장자 인기 집계 갱신 함수 (Upsert)
-- =========================================================
CREATE OR REPLACE FUNCTION ext_popularity_add(ext_in TEXT, delta BIGINT)
RETURNS VOID LANGUAGE plpgsql AS $
BEGIN
  IF delta = 0 THEN RETURN; END IF;
  INSERT INTO ext_popularity(ext, cnt, updated_at)
  VALUES (normalize_ext(ext_in), GREATEST(delta,0), now())
  ON CONFLICT (ext) DO UPDATE
    SET cnt = GREATEST(ext_popularity.cnt + EXCLUDED.cnt, 0),
        updated_at = now();
END $;

-- =========================================================
-- 5. 확장자 정책 <-> 인기집계 동기화 트리거
-- =========================================================
CREATE OR REPLACE FUNCTION trg_space_ext_pop_sync_fn()
RETURNS trigger LANGUAGE plpgsql AS $
BEGIN
  IF (TG_OP = 'INSERT') THEN
    IF NEW.is_fixed THEN PERFORM ext_popularity_add(NEW.ext, 1); END IF;
    RETURN NEW;
  ELSIF (TG_OP = 'UPDATE') THEN
    IF (NOT OLD.is_fixed) AND NEW.is_fixed THEN
      PERFORM ext_popularity_add(NEW.ext, 1);
    ELSIF OLD.is_fixed AND (NOT NEW.is_fixed) THEN
      PERFORM ext_popularity_add(NEW.ext, -1);
    END IF;
    RETURN NEW;
  ELSIF (TG_OP = 'DELETE') THEN
    IF OLD.is_fixed THEN PERFORM ext_popularity_add(OLD.ext, -1); END IF;
    RETURN OLD;
  END IF;
  RETURN NULL;
END $;

CREATE TRIGGER trg_space_ext_pop_sync_ins
AFTER INSERT ON space_extension
FOR EACH ROW EXECUTE FUNCTION trg_space_ext_pop_sync_fn();

CREATE TRIGGER trg_space_ext_pop_sync_upd
AFTER UPDATE ON space_extension
FOR EACH ROW EXECUTE FUNCTION trg_space_ext_pop_sync_fn();

CREATE TRIGGER trg_space_ext_pop_sync_del
AFTER DELETE ON space_extension
FOR EACH ROW EXECUTE FUNCTION trg_space_ext_pop_sync_fn();

-- =========================================================
-- 6. 공간 생성 시 Top-6 자동 시드 (Fallback 포함)
-- =========================================================
CREATE OR REPLACE FUNCTION seed_top6_for_space()
RETURNS trigger LANGUAGE plpgsql AS $
DECLARE
  v_sid BIGINT := NEW.space_id;
  fallback TEXT[] := ARRAY['js','bat','css','exe','sh','ps1'];
BEGIN
  -- Top6 집계 기반 삽입
  WITH top6 AS (
    SELECT ext FROM ext_popularity ORDER BY cnt DESC, ext ASC LIMIT 6
  )
  INSERT INTO space_extension(space_id, ext, is_fixed)
  SELECT v_sid, ext, TRUE FROM top6
  ON CONFLICT (space_id, ext) DO UPDATE SET is_fixed = TRUE;

  -- Fallback 6개로 보충 (부족할 때만)
  WITH cur AS (
    SELECT ext FROM space_extension WHERE space_id = v_sid AND is_fixed = TRUE
  ), need AS (
    SELECT unnest(fallback) AS ext
  )
  INSERT INTO space_extension(space_id, ext, is_fixed)
  SELECT v_sid, normalize_ext(n.ext), TRUE
  FROM need n
  WHERE NOT EXISTS (SELECT 1 FROM cur c WHERE c.ext = normalize_ext(n.ext))
  LIMIT GREATEST(0, 6 - (SELECT COUNT(*) FROM cur))
  ON CONFLICT (space_id, ext) DO UPDATE SET is_fixed = TRUE;

  RETURN NEW;
END $;

CREATE TRIGGER trg_space_seed_top6
AFTER INSERT ON space
FOR EACH ROW EXECUTE FUNCTION seed_top6_for_space();

-- =========================================================
-- 7. 파일 업로드 (S3 기반)
-- =========================================================
CREATE TABLE file_upload (
  upload_id        BIGSERIAL PRIMARY KEY,
  space_id         BIGINT NOT NULL REFERENCES space(space_id) ON DELETE CASCADE,
  uploader_member  BIGINT,
  original_name    TEXT NOT NULL,
  ext              TEXT NOT NULL,
  content_type     TEXT,
  magic_hex_prefix TEXT,
  size_bytes       BIGINT NOT NULL CHECK (size_bytes >= 0 AND size_bytes <= 1024*1024*1024*5),
  sha256_hex       CHAR(64),
  s3_bucket        TEXT NOT NULL,
  s3_key           TEXT NOT NULL UNIQUE,
  s3_region        TEXT,
  status           TEXT NOT NULL CHECK (status IN ('UPLOADING','PENDING_SCAN','AVAILABLE','BLOCKED','DELETED','FAILED')),
  blocked_reason   TEXT,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  uploaded_at      TIMESTAMPTZ,
  scanned_at       TIMESTAMPTZ,
  deleted_at       TIMESTAMPTZ
);

CREATE INDEX ix_file_upload_space_created ON file_upload(space_id, created_at DESC);
CREATE INDEX ix_file_upload_sha256 ON file_upload(sha256_hex);
CREATE INDEX ix_file_upload_status ON file_upload(status);

-- 확장자 정규화 트리거
CREATE OR REPLACE FUNCTION trg_norm_file_ext_fn()
RETURNS trigger LANGUAGE plpgsql AS $
BEGIN
  NEW.ext := normalize_ext(NEW.ext);
  RETURN NEW;
END $;

CREATE TRIGGER trg_norm_file_ext
BEFORE INSERT OR UPDATE ON file_upload
FOR EACH ROW EXECUTE FUNCTION trg_norm_file_ext_fn();

-- 업로드 차단 정책 반영 트리거
CREATE OR REPLACE FUNCTION trg_file_block_on_insert_fn()
RETURNS trigger LANGUAGE plpgsql AS $
DECLARE
  v_blocked BOOLEAN;
BEGIN
  SELECT TRUE INTO v_blocked
  FROM space_extension
  WHERE space_id = NEW.space_id
    AND ext = normalize_ext(NEW.ext)
  LIMIT 1;

  IF v_blocked THEN
    NEW.status := 'BLOCKED';
    NEW.blocked_reason := COALESCE(NEW.blocked_reason, 'Blocked by extension policy');
  ELSE
    IF NEW.status = 'UPLOADING' OR NEW.status IS NULL THEN
      NEW.status := 'PENDING_SCAN';
    END IF;
  END IF;
  RETURN NEW;
END $;

CREATE TRIGGER trg_file_block_on_insert
BEFORE INSERT ON file_upload
FOR EACH ROW EXECUTE FUNCTION trg_file_block_on_insert_fn();
```

---

## 테이블 구조 상세

### 1. space (공간)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| space_id | BIGSERIAL | 공간 ID (PK) |
| space_name | TEXT | 공간 이름 |
| owner_member | BIGINT | 소유자 회원 ID |
| created_at | TIMESTAMPTZ | 생성 시각 |

**특징:**
- 트리거로 생성 시 자동으로 Top-6 확장자 시드

---

### 2. space_extension (확장자 정책)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| space_id | BIGINT | 공간 ID (FK, PK) |
| ext | TEXT | 확장자 (정규화됨, PK) |
| is_fixed | BOOLEAN | 고정(TRUE) / 커스텀(FALSE) |
| created_at | TIMESTAMPTZ | 등록 시각 |

**특징:**
- 복합 PK: `(space_id, ext)` - 공간당 확장자 중복 방지
- 정규화 트리거: 자동으로 소문자 변환, 점(.) 제거
- 인기 집계 동기화: `is_fixed=TRUE` 변경 시 자동 반영

**쿼리 예시:**
```sql
-- 특정 공간의 차단 확장자 조회
SELECT ext FROM space_extension
WHERE space_id = ?
ORDER BY is_fixed DESC, ext ASC;

-- 고정 확장자만 조회
SELECT ext FROM space_extension
WHERE space_id = ? AND is_fixed = TRUE;
```

---

### 3. ext_popularity (확장자 인기도)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| ext | TEXT | 확장자 (정규화됨, PK) |
| cnt | BIGINT | 사용 횟수 |
| updated_at | TIMESTAMPTZ | 마지막 갱신 시각 |

**특징:**
- 전역 통계: 모든 공간의 확장자 사용 빈도
- Top-6 시드 소스: 새 공간 생성 시 사용
- 자동 동기화: `space_extension`의 `is_fixed` 변경 시 자동 증감

**쿼리 예시:**
```sql
-- Top 10 인기 확장자 조회
SELECT ext, cnt FROM ext_popularity
ORDER BY cnt DESC, ext ASC
LIMIT 10;
```

---

### 4. file_upload (파일 업로드)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| upload_id | BIGSERIAL | 업로드 ID (PK) |
| space_id | BIGINT | 공간 ID (FK) |
| uploader_member | BIGINT | 업로더 회원 ID |
| original_name | TEXT | 원본 파일명 |
| ext | TEXT | 확장자 (정규화됨) |
| content_type | TEXT | MIME 타입 |
| magic_hex_prefix | TEXT | 매직 넘버 (파일 시그니처) |
| size_bytes | BIGINT | 파일 크기 (최대 5GB) |
| sha256_hex | CHAR(64) | SHA-256 해시 |
| s3_bucket | TEXT | S3 버킷명 |
| s3_key | TEXT | S3 키 (UNIQUE) |
| s3_region | TEXT | S3 리전 |
| status | TEXT | 파일 상태 |
| blocked_reason | TEXT | 차단 사유 |
| created_at | TIMESTAMPTZ | 생성 시각 |
| uploaded_at | TIMESTAMPTZ | 업로드 완료 시각 |
| scanned_at | TIMESTAMPTZ | 스캔 완료 시각 |
| deleted_at | TIMESTAMPTZ | 삭제 시각 |

**status 값:**
- `UPLOADING`: 업로드 중
- `PENDING_SCAN`: 바이러스 스캔 대기
- `AVAILABLE`: 사용 가능
- `BLOCKED`: 차단됨
- `DELETED`: 삭제됨
- `FAILED`: 실패

**특징:**
- S3 메타데이터만 저장 (실제 파일은 S3)
- 자동 차단: 정책에 맞으면 INSERT 시 자동으로 `BLOCKED`
- 중복 방지: `sha256_hex` 인덱스로 동일 파일 감지 가능

**쿼리 예시:**
```sql
-- 공간의 최근 업로드 파일 조회
SELECT * FROM file_upload
WHERE space_id = ?
ORDER BY created_at DESC
LIMIT 20;

-- 차단된 파일 조회
SELECT * FROM file_upload
WHERE space_id = ? AND status = 'BLOCKED';

-- 중복 파일 찾기
SELECT sha256_hex, COUNT(*)
FROM file_upload
GROUP BY sha256_hex
HAVING COUNT(*) > 1;
```

---

## 인덱스 전략

### space_extension 인덱스

```sql
-- PK: (space_id, ext)
CONSTRAINT space_extension_pk PRIMARY KEY (space_id, ext)

-- 고정 확장자 조회 최적화
CREATE INDEX ix_space_ext_fixed ON space_extension(ext) WHERE is_fixed = TRUE;
```

**활용:**
- PK로 특정 공간의 확장자 빠른 조회
- Partial Index로 고정 확장자만 필터링

---

### ext_popularity 인덱스

```sql
-- PK: ext
ext TEXT PRIMARY KEY

-- Top-N 조회 최적화
CREATE INDEX ix_ext_popularity_cnt_desc ON ext_popularity (cnt DESC, ext ASC);
```

**활용:**
- `ORDER BY cnt DESC` 쿼리 최적화
- Top-6 시드 쿼리 효율적 처리

---

### file_upload 인덱스

```sql
-- 공간별 최근 파일 조회
CREATE INDEX ix_file_upload_space_created ON file_upload(space_id, created_at DESC);

-- 중복 파일 감지
CREATE INDEX ix_file_upload_sha256 ON file_upload(sha256_hex);

-- 상태별 조회
CREATE INDEX ix_file_upload_status ON file_upload(status);
```

**복합 인덱스 분석:**

**`(space_id, created_at DESC)`:**
- 가장 빈번한 쿼리: "특정 공간의 최근 파일"
- `space_id`로 필터 → `created_at` 정렬
- Covering Index 효과

**선택 이유:**
1. `space_id`: 선택도 높음 (공간 수만큼 분산)
2. `created_at DESC`: 정렬 조건 → 두 번째
3. 동등 조건(`=`) + 정렬 → 최적 순서

---

## 트리거 및 함수 상세

### 1. normalize_ext() - 확장자 정규화

```sql
CREATE OR REPLACE FUNCTION normalize_ext(txt TEXT)
RETURNS TEXT LANGUAGE sql IMMUTABLE AS $
  SELECT lower(regexp_replace(COALESCE($1,''), '^\.+', ''));
$;
```

**동작:**
- NULL → 빈 문자열
- 앞의 점(.) 제거: `.js` → `js`
- 소문자 변환: `JS` → `js`

**사용 위치:**
- `space_extension.ext` INSERT/UPDATE
- `ext_popularity.ext` INSERT/UPDATE
- `file_upload.ext` INSERT/UPDATE

---

### 2. ext_popularity_add() - 인기도 증감

```sql
CREATE OR REPLACE FUNCTION ext_popularity_add(ext_in TEXT, delta BIGINT)
RETURNS VOID LANGUAGE plpgsql AS $
BEGIN
  IF delta = 0 THEN RETURN; END IF;
  INSERT INTO ext_popularity(ext, cnt, updated_at)
  VALUES (normalize_ext(ext_in), GREATEST(delta,0), now())
  ON CONFLICT (ext) DO UPDATE
    SET cnt = GREATEST(ext_popularity.cnt + EXCLUDED.cnt, 0),
        updated_at = now();
END $;
```

**동작:**
- `delta > 0`: 증가
- `delta < 0`: 감소 (최소 0)
- Upsert 패턴: 없으면 INSERT, 있으면 UPDATE

**호출 시점:**
- `space_extension` INSERT (고정일 때 +1)
- `space_extension` UPDATE (`is_fixed` 변경 시 ±1)
- `space_extension` DELETE (고정이었으면 -1)

---

### 3. 확장자 정책 동기화 트리거

```sql
CREATE TRIGGER trg_space_ext_pop_sync_ins
AFTER INSERT ON space_extension
FOR EACH ROW EXECUTE FUNCTION trg_space_ext_pop_sync_fn();
```

**동작 시나리오:**

| 작업 | 조건 | 동작 |
|------|------|------|
| INSERT | `is_fixed=TRUE` | `ext_popularity_add(ext, +1)` |
| UPDATE | FALSE→TRUE | `ext_popularity_add(ext, +1)` |
| UPDATE | TRUE→FALSE | `ext_popularity_add(ext, -1)` |
| DELETE | `is_fixed=TRUE` | `ext_popularity_add(ext, -1)` |

**예시:**
```sql
-- js 확장자를 고정으로 추가
INSERT INTO space_extension(space_id, ext, is_fixed)
VALUES (1, 'js', TRUE);
-- → ext_popularity에 js +1

-- 커스텀으로 변경
UPDATE space_extension
SET is_fixed = FALSE
WHERE space_id = 1 AND ext = 'js';
-- → ext_popularity에 js -1

-- 삭제
DELETE FROM space_extension
WHERE space_id = 1 AND ext = 'js';
-- → is_fixed=FALSE라 인기도 변화 없음
```

---

### 4. Top-6 자동 시드 트리거

```sql
CREATE TRIGGER trg_space_seed_top6
AFTER INSERT ON space
FOR EACH ROW EXECUTE FUNCTION seed_top6_for_space();
```

**동작 과정:**

1. **Top-6 집계 기반 삽입:**
```sql
WITH top6 AS (
  SELECT ext FROM ext_popularity ORDER BY cnt DESC, ext ASC LIMIT 6
)
INSERT INTO space_extension(space_id, ext, is_fixed)
SELECT v_sid, ext, TRUE FROM top6
ON CONFLICT (space_id, ext) DO UPDATE SET is_fixed = TRUE;
```

2. **Fallback 보충:**
```sql
-- 현재 고정 확장자가 6개 미만이면 fallback 배열에서 보충
fallback := ARRAY['js','bat','css','exe','sh','ps1']
```

**예시:**
```sql
-- 새 공간 생성
INSERT INTO space(space_name, owner_member)
VALUES ('My Workspace', 1001);

-- 자동으로 space_extension에 삽입됨:
-- (space_id, ext, is_fixed)
-- (새공간ID, 'js', TRUE)    -- Top 1
-- (새공간ID, 'exe', TRUE)   -- Top 2
-- ...
-- (새공간ID, 'ps1', TRUE)   -- Top 6 or Fallback
```

---

### 5. 파일 업로드 자동 차단 트리거

```sql
CREATE TRIGGER trg_file_block_on_insert
BEFORE INSERT ON file_upload
FOR EACH ROW EXECUTE FUNCTION trg_file_block_on_insert_fn();
```

**동작:**

```sql
-- space_extension에 해당 확장자가 있으면 차단
SELECT TRUE INTO v_blocked
FROM space_extension
WHERE space_id = NEW.space_id
  AND ext = normalize_ext(NEW.ext)
LIMIT 1;

IF v_blocked THEN
  NEW.status := 'BLOCKED';
  NEW.blocked_reason := 'Blocked by extension policy';
```

**예시:**
```sql
-- space_id=1에 exe 차단 정책이 있다고 가정
INSERT INTO file_upload(space_id, original_name, ext, ...)
VALUES (1, 'virus.exe', 'exe', ...);

-- 자동으로 차단됨:
-- status = 'BLOCKED'
-- blocked_reason = 'Blocked by extension policy'
```

---

## 쿼리 최적화 가이드

### 시나리오 1: 공간의 차단 확장자 확인

```sql
-- 파일 업로드 전 클라이언트에서 확인
SELECT ext FROM space_extension
WHERE space_id = ?
ORDER BY is_fixed DESC, ext ASC;
```

**인덱스 활용:**
- PK `(space_id, ext)` 사용
- `space_id`로 빠른 필터링
- 결과 정렬은 작은 데이터셋 (보통 10~100건)

---

### 시나리오 2: Top-10 인기 확장자 조회

```sql
SELECT ext, cnt FROM ext_popularity
ORDER BY cnt DESC, ext ASC
LIMIT 10;
```

**인덱스 활용:**
- `ix_ext_popularity_cnt_desc (cnt DESC, ext ASC)` 직접 활용
- Index-Only Scan 가능
- 매우 빠름 (O(log N))

---

### 시나리오 3: 공간의 최근 업로드 파일

```sql
SELECT * FROM file_upload
WHERE space_id = ?
ORDER BY created_at DESC
LIMIT 20;
```

**인덱스 활용:**
- `ix_file_upload_space_created (space_id, created_at DESC)` 완벽 매칭
- Covering Index는 아니지만 (SELECT *)
- 정렬 없이 순서대로 읽음 → 매우 효율적

---

### 시나리오 4: 차단된 파일 통계

```sql
SELECT space_id, COUNT(*) as blocked_count
FROM file_upload
WHERE status = 'BLOCKED'
GROUP BY space_id
ORDER BY blocked_count DESC;
```

**인덱스 활용:**
- `ix_file_upload_status (status)` 사용
- `status='BLOCKED'` 빠른 필터링
- GROUP BY는 결과셋이 작으면 빠름

**개선 가능:**
```sql
-- 복합 인덱스 추가 고려
CREATE INDEX ix_file_upload_status_space ON file_upload(status, space_id);
```

---

## 성능 고려사항

### 1. 정규화 함수 IMMUTABLE

```sql
CREATE OR REPLACE FUNCTION normalize_ext(txt TEXT)
RETURNS TEXT LANGUAGE sql IMMUTABLE
```

**이유:**
- `IMMUTABLE`: 같은 입력 → 항상 같은 출력
- 함수 기반 인덱스 가능
- 쿼리 최적화 시 재계산 불필요

---

### 2. Partial Index 활용

```sql
CREATE INDEX ix_space_ext_fixed ON space_extension(ext) WHERE is_fixed = TRUE;
```

**장점:**
- 인덱스 크기 감소 (고정 확장자만)
- 조회 속도 향상
- 디스크 I/O 감소

**쿼리 예시:**
```sql
-- Partial Index 활용
SELECT space_id FROM space_extension
WHERE ext = 'exe' AND is_fixed = TRUE;
```

---

### 3. 복합 인덱스 컬럼 순서

**`file_upload (space_id, created_at DESC)`:**
- 가장 빈번한 쿼리 패턴 반영
- `WHERE space_id = ?` + `ORDER BY created_at DESC`
- 선택도 높은 컬럼(`space_id`) 우선

---

### 4. 트리거 오버헤드

**주의사항:**
- `space_extension` INSERT/UPDATE/DELETE 시 트리거 5개 실행
- 대량 작업 시 성능 영향 가능

**최적화:**
```sql
-- 대량 작업 시 트리거 임시 비활성화
ALTER TABLE space_extension DISABLE TRIGGER trg_space_ext_pop_sync_ins;
-- 작업 수행
ALTER TABLE space_extension ENABLE TRIGGER trg_space_ext_pop_sync_ins;
-- 인기도 수동 재계산
```

---

## ERD

```
┌──────────────┐
│    space     │
├──────────────┤
│ space_id (PK)│
│ space_name   │
│ owner_member │
│ created_at   │
└──────┬───────┘
       │
       │ 1:N
       │
┌──────┴────────────────┐
│  space_extension      │
├───────────────────────┤
│ space_id (PK, FK)     │
│ ext (PK)              │
│ is_fixed              │
│ created_at            │
└───────────────────────┘
       │
       │ 트리거 동기화
       │
┌──────┴────────────────┐
│  ext_popularity       │
├───────────────────────┤
│ ext (PK)              │
│ cnt                   │
│ updated_at            │
└───────────────────────┘

┌──────────────┐
│    space     │
└──────┬───────┘
       │
       │ 1:N
       │
┌──────┴────────────────┐
│   file_upload         │
├───────────────────────┤
│ upload_id (PK)        │
│ space_id (FK)         │
│ ext                   │
│ s3_key (UQ)           │
│ sha256_hex            │
│ status                │
│ created_at            │
│ ...                   │
└───────────────────────┘
```

---

## 마이그레이션 순서

### 1. 함수 생성
```sql
-- normalize_ext() 생성
```

### 2. 테이블 생성
```sql
CREATE TABLE space;
CREATE TABLE ext_popularity;
CREATE TABLE space_extension;
CREATE TABLE file_upload;
```

### 3. 인덱스 생성
```sql
-- 각 테이블의 인덱스 생성
```

### 4. 트리거 생성
```sql
-- 정규화 트리거
-- 동기화 트리거
-- 시드 트리거
-- 차단 트리거
```

### 5. 초기 데이터
```sql
-- Fallback 확장자를 ext_popularity에 시드
INSERT INTO ext_popularity(ext, cnt) VALUES
('js', 100), ('exe', 90), ('bat', 80),
('css', 70), ('sh', 60), ('ps1', 50);
```

---

**작성일**: 2025-10-16
**버전**: 1.0
**DBMS**: PostgreSQL 14+
