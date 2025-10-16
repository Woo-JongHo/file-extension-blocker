# 📊 데이터베이스 설계

**작성자**: 우종호  
**작성일**: 2025.10.16

---

## 📑 목차

1. [요건 고려](#1-요건-고려)
2. [설계 개요](#2-설계-개요)
3. [테이블 구조](#3-테이블-구조)
4. [ERD](#4-erd)
5. [DDL](#5-ddl)
6. [초기 데이터](#6-초기-데이터)
7. [주요 쿼리](#7-주요-쿼리)
8. [비즈니스 로직](#8-비즈니스-로직)

---

## 1. 요건 고려

### 📋 필수 요구사항

#### ✅ 확장자 길이 제한
- **확장자 최대 길이**: 20자
- DB 컬럼: `VARCHAR(20)`

#### ✅ 고정 확장자 (자주 쓰는 확장자)
- **최대 개수**: 6개
- 공간 생성 시 전역 Top-6 자동 삽입
- 기본값: unCheck (is_deleted = true)

#### ✅ 커스텀 확장자
- **최대 개수**: 200개
- 사용자가 직접 추가
- 추가 버튼 클릭 시 DB 저장
- X 버튼 클릭 시 삭제 (is_deleted = true)
- 커스텀 확장자 영역에 표시

#### ✅ 총 차단 가능 확장자 개수

```
고정 확장자:    6개 (Top-6)
커스텀 확장자: 200개 (사용자 추가)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
총 합계:      206개
```

**제약 조건**:
- 공간당 커스텀 확장자는 200개까지만 허용
- 고정 확장자는 개수 제한에 포함되지 않음
- 애플리케이션 레벨에서 검증:
  ```sql
  SELECT COUNT(*)
  FROM blocked_extension
  WHERE space_id = ? 
    AND is_fixed = false 
    AND is_deleted = false;
  -- 결과가 200 이상이면 추가 불가
  ```

---

## 2. 설계 개요

### 시스템 특징

- **공간 기반**: 각 공간(Space)마다 독립적인 확장자 차단 정책
- **권한 분리**: ADMIN (확장자 관리 가능) / MEMBER (파일 업로드만)
- **고정/커스텀**: 고정 확장자 (자주 차단) + 커스텀 확장자 (사용자 추가, 최대 200개)
- **사용 통계**: Extension_Usage로 Top-6 관리
- **Soft Delete**: is_deleted 플래그로 논리 삭제

### 공통 컬럼

모든 테이블에 다음 컬럼 포함:
- `created_at` TIMESTAMPTZ - 생성 일시
- `updated_at` TIMESTAMPTZ - 수정 일시
- `created_by` BIGINT - 생성자 (member_id)
- `updated_by` BIGINT - 수정자 (member_id)
- `is_deleted` BOOLEAN - 삭제 여부

---

## 2. 테이블 구조

### 2.1 Space (공간)

**설명**: 파일을 업로드하는 그룹 공간

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| space_id | BIGSERIAL | PK | 공간 ID |
| space_name | VARCHAR(255) | NOT NULL | 공간 이름 |
| description | TEXT | | 공간 설명 |
| created_at | TIMESTAMPTZ | NOT NULL | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL | 수정 일시 |
| created_by | BIGINT | NOT NULL | 생성자 |
| updated_by | BIGINT | NOT NULL | 수정자 |
| is_deleted | BOOLEAN | DEFAULT false | 삭제 여부 |

---

### 2.2 Member (사용자)

**설명**: 시스템 사용자 (한 사용자는 하나의 공간에만 속함)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| member_id | BIGSERIAL | PK | 사용자 ID |
| username | VARCHAR(100) | UNIQUE, NOT NULL | 사용자명 |
| password | VARCHAR(255) | NOT NULL | 비밀번호 (암호화) |
| space_id | BIGINT | FK | 소속 공간 ID |
| role | VARCHAR(50) | NOT NULL | 권한 ('ADMIN', 'MEMBER') |
| created_at | TIMESTAMPTZ | NOT NULL | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL | 수정 일시 |
| created_by | BIGINT | | 생성자 (회원가입 시 NULL) |
| updated_by | BIGINT | | 수정자 |
| is_deleted | BOOLEAN | DEFAULT false | 삭제 여부 |

**권한**:
- `ADMIN`: 확장자 관리 권한 + 파일 업로드
- `MEMBER`: 파일 업로드만 가능

---

### 2.3 Blocked_Extension (차단 확장자)

**설명**: 공간별 차단 확장자 목록 (고정 + 커스텀)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| blocked_id | BIGSERIAL | PK | 차단 ID |
| space_id | BIGINT | FK, NOT NULL | 공간 ID |
| extension | VARCHAR(20) | NOT NULL | 확장자 (최대 20자) |
| is_fixed | BOOLEAN | NOT NULL | 고정 확장자 여부 |
| created_at | TIMESTAMPTZ | NOT NULL | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL | 수정 일시 |
| created_by | BIGINT | NOT NULL | 생성자 |
| updated_by | BIGINT | NOT NULL | 수정자 |
| is_deleted | BOOLEAN | DEFAULT false | 삭제 여부 |

**제약**:
- UNIQUE (space_id, extension) - 공간별로 확장자 중복 불가
- extension은 소문자로 정규화 (트리거로 처리)

**고정 확장자** (is_fixed = true):
- 공간 생성 시 자동 삽입
- 체크/언체크 시 is_deleted 토글 (삭제는 안 함)
- 기본값: unCheck (is_deleted = true)

**커스텀 확장자** (is_fixed = false):
- 사용자가 직접 추가
- 최대 200개 제한 (애플리케이션 레벨)
- X 버튼으로 삭제 시 is_deleted = true

---

### 2.4 Uploaded_File (업로드된 파일)

**설명**: 업로드된 파일 메타데이터

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| file_id | BIGSERIAL | PK | 파일 ID |
| space_id | BIGINT | FK, NOT NULL | 공간 ID |
| original_name | VARCHAR(255) | NOT NULL | 원본 파일명 |
| stored_name | VARCHAR(255) | NOT NULL | 저장된 파일명 (UUID) |
| extension | VARCHAR(20) | NOT NULL | 확장자 |
| file_size | BIGINT | NOT NULL | 파일 크기 (bytes) |
| mime_type | VARCHAR(100) | | Apache Tika 감지 MIME Type |
| file_path | TEXT | NOT NULL | S3 경로 또는 로컬 경로 |
| created_at | TIMESTAMPTZ | NOT NULL | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL | 수정 일시 |
| created_by | BIGINT | NOT NULL | 업로더 (member_id) |
| updated_by | BIGINT | NOT NULL | 수정자 |
| is_deleted | BOOLEAN | DEFAULT false | 삭제 여부 |

---

## 3. ERD

### 테이블 관계도

```
┌─────────────────┐
│     Member      │
│  (사용자)        │
│  space_id (FK)  │  ← 한 사용자는 하나의 공간에만 속함
│  role: ADMIN/   │
│       MEMBER    │
└────────┬────────┘
         │ 
         │ space_id (FK)
         ▼
    ┌─────────┐
    │  Space  │
    │ (공간)   │
    └────┬────┘
         │
         │ space_id (FK)
         │
         ├────────────────────┐
         │                    │
         ▼                    ▼
  ┌──────────────┐  ┌──────────────┐
  │  Blocked_    │  │  Uploaded_   │
  │  Extension   │  │  File        │
  │ (차단 확장자) │  │ (업로드 파일) │
  └──────────────┘  └──────────────┘
```

### 상세 관계

```
Member (N) ──────── (1) Space
                            │
                            │ space_id (FK)
                            │
                ┌───────────┴───────────┐
                │                       │
                ▼                       ▼
        Blocked_Extension       Uploaded_File
        - space_id (FK)         - space_id (FK)
        - is_fixed (고정/커스텀) - mime_type
        - extension             - file_path
```

**Top-6 확장자**:
- 별도 테이블 없이 `Blocked_Extension`에서 직접 집계
- 공간 생성 시 쿼리로 실시간 조회

---

## 4. DDL

### 4.1 테이블 생성

```sql
-- =========================================================
-- 1. Member (사용자)
-- =========================================================
CREATE TABLE member (
  member_id    BIGSERIAL PRIMARY KEY,
  username     VARCHAR(100) NOT NULL UNIQUE,
  password     VARCHAR(255) NOT NULL,
  space_id     BIGINT REFERENCES space(space_id) ON DELETE SET NULL,
  role         VARCHAR(50) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('ADMIN', 'MEMBER')),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by   BIGINT,
  updated_by   BIGINT,
  is_deleted   BOOLEAN NOT NULL DEFAULT false
);

COMMENT ON TABLE member IS '시스템 사용자 (한 사용자는 하나의 공간에만 속함)';
COMMENT ON COLUMN member.password IS '암호화된 비밀번호 (BCrypt)';
COMMENT ON COLUMN member.space_id IS '소속 공간 ID';
COMMENT ON COLUMN member.role IS 'ADMIN: 확장자 관리 가능, MEMBER: 업로드만 가능';

-- 인덱스
CREATE INDEX idx_member_space ON member(space_id) WHERE is_deleted = false;

-- =========================================================
-- 2. Space (공간)
-- =========================================================
CREATE TABLE space (
  space_id     BIGSERIAL PRIMARY KEY,
  space_name   VARCHAR(255) NOT NULL,
  description  TEXT,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by   BIGINT NOT NULL REFERENCES member(member_id),
  updated_by   BIGINT NOT NULL REFERENCES member(member_id),
  is_deleted   BOOLEAN NOT NULL DEFAULT false
);

COMMENT ON TABLE space IS '파일 업로드 그룹 공간';

-- =========================================================
-- 3. Blocked_Extension (차단 확장자)
-- =========================================================
CREATE TABLE blocked_extension (
  blocked_id     BIGSERIAL PRIMARY KEY,
  space_id       BIGINT NOT NULL REFERENCES space(space_id) ON DELETE CASCADE,
  extension      VARCHAR(20) NOT NULL,
  is_fixed       BOOLEAN NOT NULL,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by     BIGINT NOT NULL REFERENCES member(member_id),
  updated_by     BIGINT NOT NULL REFERENCES member(member_id),
  is_deleted     BOOLEAN NOT NULL DEFAULT false,
  CONSTRAINT uq_space_extension UNIQUE (space_id, extension)
);

COMMENT ON TABLE blocked_extension IS '공간별 차단 확장자 정책';
COMMENT ON COLUMN blocked_extension.is_fixed IS 'TRUE: 고정 확장자, FALSE: 커스텀 확장자';
COMMENT ON COLUMN blocked_extension.extension IS '확장자 (점 제외, 소문자, 최대 20자)';

-- 인덱스
CREATE INDEX idx_blocked_extension_space ON blocked_extension(space_id) WHERE is_deleted = false;
CREATE INDEX idx_blocked_extension_fixed ON blocked_extension(space_id, is_fixed) WHERE is_deleted = false;

-- =========================================================
-- 4. Uploaded_File (업로드된 파일)
-- =========================================================
CREATE TABLE uploaded_file (
  file_id        BIGSERIAL PRIMARY KEY,
  space_id       BIGINT NOT NULL REFERENCES space(space_id) ON DELETE CASCADE,
  original_name  VARCHAR(255) NOT NULL,
  stored_name    VARCHAR(255) NOT NULL UNIQUE,
  extension      VARCHAR(20) NOT NULL,
  file_size      BIGINT NOT NULL CHECK (file_size >= 0),
  mime_type      VARCHAR(100),
  file_path      TEXT NOT NULL,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by     BIGINT NOT NULL REFERENCES member(member_id),
  updated_by     BIGINT NOT NULL REFERENCES member(member_id),
  is_deleted     BOOLEAN NOT NULL DEFAULT false
);

COMMENT ON TABLE uploaded_file IS '업로드된 파일 메타데이터';
COMMENT ON COLUMN uploaded_file.original_name IS '사용자가 업로드한 원본 파일명';
COMMENT ON COLUMN uploaded_file.stored_name IS '서버에 저장된 파일명 (UUID)';
COMMENT ON COLUMN uploaded_file.mime_type IS 'Apache Tika가 감지한 MIME Type';
COMMENT ON COLUMN uploaded_file.file_path IS 'S3 경로 또는 로컬 파일 경로';

-- 인덱스
CREATE INDEX idx_uploaded_file_space ON uploaded_file(space_id) WHERE is_deleted = false;
CREATE INDEX idx_uploaded_file_uploader ON uploaded_file(created_by) WHERE is_deleted = false;
CREATE INDEX idx_uploaded_file_extension ON uploaded_file(extension) WHERE is_deleted = false;

-- =========================================================
-- 9. 확장자 정규화 함수
-- =========================================================
CREATE OR REPLACE FUNCTION normalize_extension(ext TEXT)
RETURNS TEXT LANGUAGE SQL IMMUTABLE AS $$
  SELECT lower(regexp_replace(COALESCE(ext, ''), '^\.+', ''))
$$;

COMMENT ON FUNCTION normalize_extension IS '확장자 정규화: 소문자 변환 + 앞의 점(.) 제거';

-- =========================================================
-- 10. 확장자 정규화 트리거
-- =========================================================
CREATE OR REPLACE FUNCTION trg_normalize_extension()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.extension := normalize_extension(NEW.extension);
  RETURN NEW;
END $$;

-- Blocked_Extension 트리거
CREATE TRIGGER trg_blocked_extension_normalize
BEFORE INSERT OR UPDATE ON blocked_extension
FOR EACH ROW EXECUTE FUNCTION trg_normalize_extension();

-- Extension_Usage 트리거
CREATE TRIGGER trg_extension_usage_normalize
BEFORE INSERT OR UPDATE ON extension_usage
FOR EACH ROW EXECUTE FUNCTION trg_normalize_extension();

-- =========================================================
-- 11. Updated_At 자동 갱신 함수
-- =========================================================
CREATE OR REPLACE FUNCTION trg_update_timestamp()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END $$;

-- 모든 테이블에 적용
CREATE TRIGGER trg_member_update BEFORE UPDATE ON member
FOR EACH ROW EXECUTE FUNCTION trg_update_timestamp();

CREATE TRIGGER trg_space_update BEFORE UPDATE ON space
FOR EACH ROW EXECUTE FUNCTION trg_update_timestamp();

CREATE TRIGGER trg_blocked_extension_update BEFORE UPDATE ON blocked_extension
FOR EACH ROW EXECUTE FUNCTION trg_update_timestamp();

CREATE TRIGGER trg_uploaded_file_update BEFORE UPDATE ON uploaded_file
FOR EACH ROW EXECUTE FUNCTION trg_update_timestamp();
```

---

## 5. 초기 데이터

### 5.1 공간 생성 시 Top-6 자동 삽입

**공간 생성 프로시저**:

```sql
CREATE OR REPLACE FUNCTION create_space_with_defaults(
  p_space_name VARCHAR(255),
  p_creator_id BIGINT
) RETURNS BIGINT LANGUAGE plpgsql AS $$
DECLARE
  v_space_id BIGINT;
BEGIN
  -- 1. 공간 생성
  INSERT INTO space (space_name, created_by, updated_by)
  VALUES (p_space_name, p_creator_id, p_creator_id)
  RETURNING space_id INTO v_space_id;
  
  -- 2. 생성자를 ADMIN으로 등록 (Member 테이블의 space_id 업데이트)
  UPDATE member 
  SET space_id = v_space_id, 
      role = 'ADMIN',
      updated_by = p_creator_id,
      updated_at = now()
  WHERE member_id = p_creator_id;
  
  -- 3. 고정 확장자 자동 삽입 (전역 Top-6, 기본 unCheck)
  -- Blocked_Extension에서 실시간 집계하여 Top-6 조회
  INSERT INTO blocked_extension (space_id, extension, is_fixed, created_by, updated_by, is_deleted)
  SELECT 
    v_space_id,
    extension,
    true,  -- is_fixed
    p_creator_id,
    p_creator_id,
    true   -- 기본 unCheck (is_deleted = true)
  FROM (
    SELECT extension, COUNT(*) as usage_count
    FROM blocked_extension
    WHERE is_fixed = false 
      AND is_deleted = false
    GROUP BY extension
    ORDER BY usage_count DESC
    LIMIT 6
  ) AS top_extensions;
  
  RETURN v_space_id;
END $$;

COMMENT ON FUNCTION create_space_with_defaults IS '공간 생성 시 Top-6 고정 확장자 자동 삽입 (실시간 집계)';
```

**설명**:
- `Blocked_Extension`에서 커스텀 확장자를 실시간 집계
- 최대 200개 × N개 공간 = 충분히 빠름
- 별도 테이블 없이 항상 최신 Top-6 보장

### 5.2 샘플 데이터

```sql
-- 사용자 생성
INSERT INTO member (username, password) VALUES
('admin', '$2a$10$...'),  -- BCrypt 암호화
('user1', '$2a$10$...'),
('user2', '$2a$10$...');

-- 공간 생성 (프로시저 사용)
SELECT create_space_with_defaults('프로젝트 A', 1);  -- admin이 생성
SELECT create_space_with_defaults('프로젝트 B', 2);  -- user1이 생성

-- 커스텀 확장자 추가 (사용자가 직접 추가)
INSERT INTO blocked_extension (space_id, extension, is_fixed, created_by, updated_by, is_deleted)
VALUES 
(1, 'php', false, 1, 1, false),  -- 프로젝트 A에 php 차단 추가
(1, 'jsp', false, 1, 1, false);  -- 프로젝트 A에 jsp 차단 추가
```

---

## 6. 주요 쿼리

### 6.1 공간의 차단 확장자 목록 조회

```sql
-- 활성화된 차단 확장자만 조회
SELECT extension, is_fixed
FROM blocked_extension
WHERE space_id = ? 
  AND is_deleted = false
ORDER BY is_fixed DESC, extension ASC;
```

### 6.2 Top-6 고정 확장자 조회

```sql
-- 전역 Top-6 (실시간 집계)
SELECT extension, COUNT(*) as usage_count
FROM blocked_extension
WHERE is_fixed = false 
  AND is_deleted = false
GROUP BY extension
ORDER BY usage_count DESC
LIMIT 6;
```

### 6.3 커스텀 확장자 개수 확인 (200개 제한)

```sql
-- 공간별 커스텀 확장자 개수 (최대 200개 제한)
SELECT COUNT(*)
FROM blocked_extension
WHERE space_id = ? 
  AND is_fixed = false 
  AND is_deleted = false;
```

### 6.4 파일 업로드 가능 여부 확인

```sql
-- 특정 확장자가 차단되어 있는지 확인
SELECT EXISTS (
  SELECT 1
  FROM blocked_extension
WHERE space_id = ?
    AND extension = normalize_extension(?)
    AND is_deleted = false
) AS is_blocked;
```

### 6.5 고정 확장자 체크/언체크

```sql
-- 체크: is_deleted = false
-- 언체크: is_deleted = true

UPDATE blocked_extension
SET is_deleted = ?,
    updated_at = now(),
    updated_by = ?
WHERE space_id = ?
  AND extension = normalize_extension(?)
  AND is_fixed = true;
```

### 6.6 커스텀 확장자 추가

```sql
-- 사전 검증: 개수 제한 확인 (200개)
SELECT COUNT(*) FROM blocked_extension
WHERE space_id = ? AND is_fixed = false AND is_deleted = false;
-- → 200개 이상이면 추가 거부

-- 사전 검증: 확장자 길이 확인 (20자)
-- 애플리케이션 레벨: if (extension.length() > 20) throw Exception

-- 커스텀 확장자 추가
INSERT INTO blocked_extension (space_id, extension, is_fixed, created_by, updated_by, is_deleted)
VALUES (?, normalize_extension(?), false, ?, ?, false);
```

---

## 7. 비즈니스 로직

### 7.1 고정 확장자 (is_fixed = true)

- **개수**: 최대 6개 (Top-6)
- 공간 생성 시 자동 삽입 (실시간 집계)
- 기본값: unCheck (is_deleted = true)
- 체크/언체크 시 is_deleted 토글
- **삭제 불가** (항상 테이블에 존재)
- 커스텀 확장자 영역에 표시 안 됨
- **개수 제한에 포함되지 않음**

### 7.2 커스텀 확장자 (is_fixed = false)

- **최대 개수**: 200개 (요건 명시)
- **최대 길이**: 20자 (요건 명시)
- 사용자가 직접 추가
- X 버튼 클릭 시 is_deleted = true
- 커스텀 확장자 영역에 표시
- **전역 Top-6 집계에 자동 반영** (조회 시 실시간 집계)

**추가 전 검증** (애플리케이션 레벨):
```java
// 1. 확장자 길이 검증 (20자)
if (extension.length() > 20) {
    throw new ValidationException("확장자는 최대 20자까지 입력 가능합니다.");
}

// 2. 커스텀 확장자 개수 검증 (200개)
long customCount = countCustomExtensions(spaceId);
if (customCount >= 200) {
    throw new ValidationException("커스텀 확장자는 최대 200개까지만 추가 가능합니다.");
}
```

### 7.3 총 차단 확장자 개수 (요건 기반)

```
┌──────────────────────────────────────┐
│ 고정 확장자:    6개 (Top-6)          │
│ 커스텀 확장자: 200개 (요건 명시)     │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│ 총 합계:      206개                  │
│                                      │
│ ⚠️ 확장자 최대 길이: 20자 (요건)    │
└──────────────────────────────────────┘
```

**제약 조건**:
- 고정 6개는 개수 제한에서 제외 (자동 삽입)
- 커스텀 200개는 애플리케이션 레벨에서 검증
- 확장자 길이 20자 초과 시 입력 불가

### 7.4 Top-6 갱신 방식

- **실시간 집계**: 별도 테이블 없이 `Blocked_Extension`에서 직접 조회
- **조회 쿼리**:
  ```sql
  SELECT extension, COUNT(*) as usage_count
  FROM blocked_extension
  WHERE is_fixed = false AND is_deleted = false
  GROUP BY extension
  ORDER BY usage_count DESC
  LIMIT 6;
  ```
- **장점**:
  - 항상 최신 데이터 반영
  - 트리거 불필요
  - 단순한 구조
- **성능**: 최대 200개 × N개 공간 → GROUP BY 충분히 빠름

### 7.5 확장자 중복 방지

- UNIQUE 제약: (space_id, extension)
- 같은 공간에서 동일 확장자 중복 불가
- 고정 확장자를 커스텀으로 다시 추가 불가

---