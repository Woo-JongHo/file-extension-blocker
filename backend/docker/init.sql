-- ============================================
-- 데이터베이스 초기화 스크립트
-- File Extension Blocker - 파일 확장자 차단 시스템
-- ============================================

-- =========================================================
-- 1. Space (공간)
-- =========================================================
CREATE TABLE space (
  space_id     BIGSERIAL PRIMARY KEY,
  space_name   VARCHAR(255) NOT NULL,
  description  TEXT,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by   BIGINT,
  updated_by   BIGINT,
  is_deleted   BOOLEAN NOT NULL DEFAULT false
);

COMMENT ON TABLE space IS '파일 업로드 그룹 공간';
COMMENT ON COLUMN space.space_name IS '공간 이름 (팀명, 프로젝트명 등)';
COMMENT ON COLUMN space.description IS '공간 설명';

-- =========================================================
-- 2. Member (사용자)
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
COMMENT ON COLUMN member.username IS '사용자 이름 (로그인 ID)';
COMMENT ON COLUMN member.password IS '비밀번호 (평문 저장, 실제 환경에서는 BCrypt 암호화 권장)';
COMMENT ON COLUMN member.space_id IS '소속 공간 ID';
COMMENT ON COLUMN member.role IS 'ADMIN: 확장자 관리 + 파일 업로드 가능 / MEMBER: 파일 업로드만 가능';

-- 인덱스
CREATE INDEX idx_member_space ON member(space_id) WHERE is_deleted = false;
CREATE INDEX idx_member_username ON member(username) WHERE is_deleted = false;

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

COMMENT ON TABLE blocked_extension IS '공간별 차단 확장자 정책 (고정 + 커스텀)';
COMMENT ON COLUMN blocked_extension.is_fixed IS 'TRUE: 고정 확장자 (체크박스로 활성화/비활성화), FALSE: 커스텀 확장자 (추가/삭제 가능)';
COMMENT ON COLUMN blocked_extension.extension IS '확장자 (점 제외, 소문자, 최대 20자)';
COMMENT ON COLUMN blocked_extension.is_deleted IS 'TRUE: 비활성화/삭제됨, FALSE: 활성화/사용 중';

-- 인덱스
CREATE INDEX idx_blocked_extension_space ON blocked_extension(space_id) WHERE is_deleted = false;
CREATE INDEX idx_blocked_extension_fixed ON blocked_extension(space_id, is_fixed) WHERE is_deleted = false;
CREATE INDEX idx_blocked_extension_extension ON blocked_extension(extension) WHERE is_deleted = false;

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
COMMENT ON COLUMN uploaded_file.extension IS '파일 확장자 (점 제외, 소문자)';
COMMENT ON COLUMN uploaded_file.mime_type IS 'Apache Tika가 감지한 MIME Type';
COMMENT ON COLUMN uploaded_file.file_path IS '로컬 파일 시스템 저장 경로';

-- 인덱스
CREATE INDEX idx_uploaded_file_space ON uploaded_file(space_id) WHERE is_deleted = false;
CREATE INDEX idx_uploaded_file_uploader ON uploaded_file(created_by) WHERE is_deleted = false;
CREATE INDEX idx_uploaded_file_extension ON uploaded_file(extension) WHERE is_deleted = false;

-- =========================================================
-- 5. 확장자 정규화 함수
-- =========================================================
CREATE OR REPLACE FUNCTION normalize_extension(ext TEXT)
RETURNS TEXT LANGUAGE SQL IMMUTABLE AS $$
  SELECT lower(regexp_replace(COALESCE(ext, ''), '^\.+', ''))
$$;

COMMENT ON FUNCTION normalize_extension IS '확장자 정규화: 소문자 변환 + 앞의 점(.) 제거';

-- =========================================================
-- 6. 확장자 정규화 트리거
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

-- Uploaded_File 트리거
CREATE TRIGGER trg_uploaded_file_normalize
BEFORE INSERT OR UPDATE ON uploaded_file
FOR EACH ROW EXECUTE FUNCTION trg_normalize_extension();

-- =========================================================
-- 7. Updated_At 자동 갱신 함수
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

-- ============================================
-- 더미 데이터 삽입
-- ============================================

-- =========================================================
-- 8. Space 데이터 (3개)
-- =========================================================
INSERT INTO space (space_name, description, created_at, updated_at, is_deleted) VALUES
('프론트엔드팀', 'React, Vue.js 프론트엔드 개발팀', now(), now(), false),
('백엔드팀', 'Spring Boot, Node.js 백엔드 개발팀', now(), now(), false),
('DevOps팀', 'CI/CD 및 인프라 관리팀', now(), now(), false);

-- =========================================================
-- 9. Member 데이터 (각 공간마다 관리자 1명 + 일반 멤버 2명)
-- =========================================================
-- 관리자 (ADMIN) - 확장자 관리 + 파일 업로드 권한
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('frontend_admin', '1234', 1, 'ADMIN', now(), now(), false),
('backend_admin', '1234', 2, 'ADMIN', now(), now(), false),
('devops_admin', '1234', 3, 'ADMIN', now(), now(), false);

-- 일반 멤버 (MEMBER) - 파일 업로드만 가능 (확장자 관리 불가)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('frontend_user1', '1234', 1, 'MEMBER', now(), now(), false),
('frontend_user2', '1234', 1, 'MEMBER', now(), now(), false),
('backend_user1', '1234', 2, 'MEMBER', now(), now(), false),
('backend_user2', '1234', 2, 'MEMBER', now(), now(), false),
('devops_user1', '1234', 3, 'MEMBER', now(), now(), false),
('devops_user2', '1234', 3, 'MEMBER', now(), now(), false);

-- =========================================================
-- 10. 고정 확장자 7개 자동 삽입
-- =========================================================
-- 3개 Space × 7개 확장자 = 21개
-- 기본 비활성화 상태 (isDeleted = true)
-- 관리자가 필요한 확장자만 체크박스로 활성화

INSERT INTO blocked_extension (space_id, extension, is_fixed, created_by, updated_by, created_at, updated_at, is_deleted)
SELECT
    s.space_id,
    ext.extension,
    true,
    m.member_id,  -- 해당 Space의 관리자 member_id
    m.member_id,  -- 해당 Space의 관리자 member_id
    NOW(),
    NOW(),
    true  -- 기본 비활성화 (체크 해제 상태)
FROM space s
INNER JOIN member m ON s.space_id = m.space_id AND m.role = 'ADMIN' AND m.is_deleted = false
CROSS JOIN (VALUES ('bat'), ('cmd'), ('com'), ('cpl'), ('exe'), ('js'), ('scr')) AS ext(extension)
WHERE s.is_deleted = false
ORDER BY s.space_id, ext.extension;

-- =========================================================
-- 11. 시퀀스 재설정 (다음 ID 설정)
-- =========================================================
SELECT setval('space_space_id_seq', (SELECT MAX(space_id) FROM space));
SELECT setval('member_member_id_seq', (SELECT MAX(member_id) FROM member));
SELECT setval('blocked_extension_blocked_id_seq', (SELECT MAX(blocked_id) FROM blocked_extension));

-- =========================================================
-- 12. 초기 데이터 확인용 조회
-- =========================================================
SELECT
    s.space_id,
    s.space_name,
    COUNT(CASE WHEN be.is_fixed = true THEN 1 END) as fixed_extensions_count,
    COUNT(CASE WHEN be.is_fixed = true AND be.is_deleted = false THEN 1 END) as active_fixed_count,
    COUNT(CASE WHEN be.is_fixed = false THEN 1 END) as custom_extensions_count
FROM space s
LEFT JOIN blocked_extension be ON s.space_id = be.space_id
WHERE s.is_deleted = false
GROUP BY s.space_id, s.space_name
ORDER BY s.space_id;

-- ============================================
-- 테이블 정보 요약
-- ============================================

-- Space 테이블: 3개 공간
-- Member 테이블: 3명 (각 공간마다 Admin 1명)
-- Blocked_Extension 테이블: 21개 (3개 Space × 7개 고정 확장자, 모두 비활성화)
-- Uploaded_File 테이블: 0개 (비어있음)

-- ============================================
-- 사용 가이드
-- ============================================

-- 1. Space 목록 조회
-- SELECT * FROM space WHERE is_deleted = false;

-- 2. Space별 멤버 조회
-- SELECT * FROM member WHERE space_id = 1 AND is_deleted = false;

-- 3. Space별 활성화된 차단 확장자 조회
-- SELECT * FROM blocked_extension WHERE space_id = 1 AND is_deleted = false ORDER BY extension;

-- 4. Space별 고정 확장자 조회 (활성화/비활성화 모두)
-- SELECT * FROM blocked_extension WHERE space_id = 1 AND is_fixed = true ORDER BY extension;

-- 5. Space별 커스텀 확장자 조회
-- SELECT * FROM blocked_extension WHERE space_id = 1 AND is_fixed = false AND is_deleted = false ORDER BY extension;

-- 6. Space별 업로드된 파일 조회
-- SELECT * FROM uploaded_file WHERE space_id = 1 AND is_deleted = false ORDER BY created_at DESC;

-- 7. 고정 확장자 활성화/비활성화 토글
-- UPDATE blocked_extension SET is_deleted = NOT is_deleted WHERE space_id = 1 AND extension = 'bat';

-- 8. 커스텀 확장자 추가 (새로 생성)
-- INSERT INTO blocked_extension (space_id, extension, is_fixed, created_by, updated_by, is_deleted)
-- VALUES (1, 'php', false, 1, 1, false);

-- 9. 커스텀 확장자 재활성화 (삭제된 것 복구)
-- UPDATE blocked_extension SET is_deleted = false, updated_at = NOW() WHERE space_id = 1 AND extension = 'php';

-- 10. 커스텀 확장자 삭제 (논리 삭제)
-- UPDATE blocked_extension SET is_deleted = true WHERE space_id = 1 AND extension = 'php';

-- ============================================
-- 고정 확장자 정책
-- ============================================

-- 고정 확장자 7개 (알파벳 순):
-- bat, cmd, com, cpl, exe, js, scr

-- 특징:
-- - Space 생성 시 자동으로 추가됨
-- - 기본 비활성화 상태 (isDeleted = true)
-- - 관리자가 체크박스로 필요한 것만 활성화
-- - 삭제 불가 (체크박스로만 제어)
-- - 삭제 여부와 관계없이 항상 화면에 표시

-- ============================================
-- 커스텀 확장자 정책
-- ============================================

-- 특징:
-- - 관리자가 직접 추가/삭제 가능
-- - 최대 200개까지 추가 가능
-- - 삭제 시 is_deleted = true로 변경 (논리 삭제)
-- - 삭제된 확장자를 다시 추가하면 is_deleted = false로 UPDATE (재활성화)
-- - 고정 확장자와 중복 불가
-- - 활성화된 커스텀 확장자끼리 중복 불가

-- ============================================
-- 검증 전략
-- ============================================

-- [1단계] 확장자 Blacklist 검증
-- - 파일명에서 확장자 추출
-- - Space의 활성화된 차단 확장자 목록(isDeleted=false)과 비교
-- - 일치하면 차단

-- [2단계] 매직바이트 & MIME 타입 검증
-- - Apache Tika로 파일의 실제 MIME 타입 감지
-- - 바이너리 실행 파일 차단 (exe, elf, mach-o)
-- - 확장자 위장 검증: 감지된 MIME이 차단 확장자 목록의 MIME과 일치하면 차단
--   예: .jpg인데 실제 application/x-sh → sh가 차단 목록에 있으면 차단

-- [3단계] 압축 파일 내부 검증
-- - ZipValidator로 압축 파일 내부 재귀 검증
-- - 내부 파일의 확장자 검증
-- - Zip Bomb 감지 (압축률 100배 초과 차단)
-- - 중첩 압축 깊이 제한 (최대 1단계)

-- [4단계] 파일 저장 & 권한 제거
-- - 파일을 로컬에 저장
-- - chmod 644 적용 (실행 권한 제거)
-- - DB에 메타데이터 저장

-- ============================================
-- 초기화 완료
-- ============================================
