-- ============================================
-- 데이터베이스 초기화 스크립트
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
COMMENT ON COLUMN member.password IS '암호화된 비밀번호 (BCrypt)';
COMMENT ON COLUMN member.space_id IS '소속 공간 ID';
COMMENT ON COLUMN member.role IS 'ADMIN: 확장자 관리 가능, MEMBER: 업로드만 가능';

-- 인덱스
CREATE INDEX idx_member_space ON member(space_id) WHERE is_deleted = false;

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

-- =========================================================
-- 8. 공간 생성 시 Top-6 자동 삽입 함수
-- =========================================================
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

-- ============================================
-- 더미 데이터 삽입
-- ============================================

-- Space 데이터 (9개)
INSERT INTO space (space_name, description, created_at, updated_at, is_deleted) VALUES
('프론트엔드팀', 'React, Vue.js 프론트엔드 개발팀', now(), now(), false),
('백엔드팀', 'Spring Boot, Node.js 백엔드 개발팀', now(), now(), false),
('DevOps팀', 'CI/CD 및 인프라 관리팀', now(), now(), false),
('디자인팀', 'UI/UX 디자인팀', now(), now(), false),
('기획팀', '서비스 기획 및 전략팀', now(), now(), false),
('QA팀', '품질 보증 및 테스트팀', now(), now(), false),
('마케팅팀', '마케팅 및 홍보팀', now(), now(), false),
('데이터팀', '데이터 분석 및 AI팀', now(), now(), false),
('경영지원팀', '인사, 총무, 재무팀', now(), now(), false);

-- Member 데이터 (각 공간마다 1~6명, 관리자 반드시 포함)
-- password: '1234' (실제로는 BCrypt 암호화 필요, 여기서는 평문)

-- 1. 프론트엔드팀 (6명: ADMIN 1명 + MEMBER 5명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('frontend_admin', '1234', 1, 'ADMIN', now(), now(), false),
('frontend_user1', '1234', 1, 'MEMBER', now(), now(), false),
('frontend_user2', '1234', 1, 'MEMBER', now(), now(), false),
('frontend_user3', '1234', 1, 'MEMBER', now(), now(), false),
('frontend_user4', '1234', 1, 'MEMBER', now(), now(), false),
('frontend_user5', '1234', 1, 'MEMBER', now(), now(), false);

-- 2. 백엔드팀 (5명: ADMIN 1명 + MEMBER 4명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('backend_admin', '1234', 2, 'ADMIN', now(), now(), false),
('backend_user1', '1234', 2, 'MEMBER', now(), now(), false),
('backend_user2', '1234', 2, 'MEMBER', now(), now(), false),
('backend_user3', '1234', 2, 'MEMBER', now(), now(), false),
('backend_user4', '1234', 2, 'MEMBER', now(), now(), false);

-- 3. DevOps팀 (3명: ADMIN 1명 + MEMBER 2명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('devops_admin', '1234', 3, 'ADMIN', now(), now(), false),
('devops_user1', '1234', 3, 'MEMBER', now(), now(), false),
('devops_user2', '1234', 3, 'MEMBER', now(), now(), false);

-- 4. 디자인팀 (4명: ADMIN 1명 + MEMBER 3명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('design_admin', '1234', 4, 'ADMIN', now(), now(), false),
('design_user1', '1234', 4, 'MEMBER', now(), now(), false),
('design_user2', '1234', 4, 'MEMBER', now(), now(), false),
('design_user3', '1234', 4, 'MEMBER', now(), now(), false);

-- 5. 기획팀 (2명: ADMIN 1명 + MEMBER 1명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('planning_admin', '1234', 5, 'ADMIN', now(), now(), false),
('planning_user1', '1234', 5, 'MEMBER', now(), now(), false);

-- 6. QA팀 (4명: ADMIN 1명 + MEMBER 3명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('qa_admin', '1234', 6, 'ADMIN', now(), now(), false),
('qa_user1', '1234', 6, 'MEMBER', now(), now(), false),
('qa_user2', '1234', 6, 'MEMBER', now(), now(), false),
('qa_user3', '1234', 6, 'MEMBER', now(), now(), false);

-- 7. 마케팅팀 (3명: ADMIN 1명 + MEMBER 2명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('marketing_admin', '1234', 7, 'ADMIN', now(), now(), false),
('marketing_user1', '1234', 7, 'MEMBER', now(), now(), false),
('marketing_user2', '1234', 7, 'MEMBER', now(), now(), false);

-- 8. 데이터팀 (1명: ADMIN만)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('data_admin', '1234', 8, 'ADMIN', now(), now(), false);

-- 9. 경영지원팀 (5명: ADMIN 2명 + MEMBER 3명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('management_admin1', '1234', 9, 'ADMIN', now(), now(), false),
('management_admin2', '1234', 9, 'ADMIN', now(), now(), false),
('management_user1', '1234', 9, 'MEMBER', now(), now(), false),
('management_user2', '1234', 9, 'MEMBER', now(), now(), false),
('management_user3', '1234', 9, 'MEMBER', now(), now(), false);

-- 시퀀스 재설정 (다음 ID가 올바르게 생성되도록)
SELECT setval('space_space_id_seq', (SELECT MAX(space_id) FROM space));
SELECT setval('member_member_id_seq', (SELECT MAX(member_id) FROM member));

-- 확인용 조회
SELECT 
    s.space_name,
    COUNT(m.member_id) as member_count,
    SUM(CASE WHEN m.role = 'ADMIN' THEN 1 ELSE 0 END) as admin_count,
    SUM(CASE WHEN m.role = 'MEMBER' THEN 1 ELSE 0 END) as member_only_count
FROM space s
LEFT JOIN member m ON s.space_id = m.space_id AND m.is_deleted = false
WHERE s.is_deleted = false
GROUP BY s.space_id, s.space_name
ORDER BY s.space_id;

