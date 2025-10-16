-- ============================================
-- 더미 데이터 삽입 스크립트
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
-- password: 'password123' (실제로는 BCrypt 암호화 필요, 여기서는 평문)

-- 1. 프론트엔드팀 (6명: ADMIN 1명 + MEMBER 5명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('frontend_admin', 'password123', 1, 'ADMIN', now(), now(), false),
('frontend_user1', 'password123', 1, 'MEMBER', now(), now(), false),
('frontend_user2', 'password123', 1, 'MEMBER', now(), now(), false),
('frontend_user3', 'password123', 1, 'MEMBER', now(), now(), false),
('frontend_user4', 'password123', 1, 'MEMBER', now(), now(), false),
('frontend_user5', 'password123', 1, 'MEMBER', now(), now(), false);

-- 2. 백엔드팀 (5명: ADMIN 1명 + MEMBER 4명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('backend_admin', 'password123', 2, 'ADMIN', now(), now(), false),
('backend_user1', 'password123', 2, 'MEMBER', now(), now(), false),
('backend_user2', 'password123', 2, 'MEMBER', now(), now(), false),
('backend_user3', 'password123', 2, 'MEMBER', now(), now(), false),
('backend_user4', 'password123', 2, 'MEMBER', now(), now(), false);

-- 3. DevOps팀 (3명: ADMIN 1명 + MEMBER 2명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('devops_admin', 'password123', 3, 'ADMIN', now(), now(), false),
('devops_user1', 'password123', 3, 'MEMBER', now(), now(), false),
('devops_user2', 'password123', 3, 'MEMBER', now(), now(), false);

-- 4. 디자인팀 (4명: ADMIN 1명 + MEMBER 3명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('design_admin', 'password123', 4, 'ADMIN', now(), now(), false),
('design_user1', 'password123', 4, 'MEMBER', now(), now(), false),
('design_user2', 'password123', 4, 'MEMBER', now(), now(), false),
('design_user3', 'password123', 4, 'MEMBER', now(), now(), false);

-- 5. 기획팀 (2명: ADMIN 1명 + MEMBER 1명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('planning_admin', 'password123', 5, 'ADMIN', now(), now(), false),
('planning_user1', 'password123', 5, 'MEMBER', now(), now(), false);

-- 6. QA팀 (4명: ADMIN 1명 + MEMBER 3명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('qa_admin', 'password123', 6, 'ADMIN', now(), now(), false),
('qa_user1', 'password123', 6, 'MEMBER', now(), now(), false),
('qa_user2', 'password123', 6, 'MEMBER', now(), now(), false),
('qa_user3', 'password123', 6, 'MEMBER', now(), now(), false);

-- 7. 마케팅팀 (3명: ADMIN 1명 + MEMBER 2명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('marketing_admin', 'password123', 7, 'ADMIN', now(), now(), false),
('marketing_user1', 'password123', 7, 'MEMBER', now(), now(), false),
('marketing_user2', 'password123', 7, 'MEMBER', now(), now(), false);

-- 8. 데이터팀 (1명: ADMIN만)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('data_admin', 'password123', 8, 'ADMIN', now(), now(), false);

-- 9. 경영지원팀 (5명: ADMIN 2명 + MEMBER 3명)
INSERT INTO member (username, password, space_id, role, created_at, updated_at, is_deleted) VALUES
('management_admin1', 'password123', 9, 'ADMIN', now(), now(), false),
('management_admin2', 'password123', 9, 'ADMIN', now(), now(), false),
('management_user1', 'password123', 9, 'MEMBER', now(), now(), false),
('management_user2', 'password123', 9, 'MEMBER', now(), now(), false),
('management_user3', 'password123', 9, 'MEMBER', now(), now(), false);

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

