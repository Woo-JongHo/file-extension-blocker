# Backend - File Extension Blocker

파일 확장자 차단 시스템 백엔드 서버

---

## 기술 스택

### Core Framework
- **Spring Boot** 3.x
- **Java** 17+
- **Gradle** 8.x

### Database
- **PostgreSQL** 15
- **Spring Data JPA**
- **Hibernate**

### Security & Validation
- **Apache Tika** - 라이브러리를 이용하여 MIME 타입 감지 및 매직바이트 검증 
- **ZipValidator** - 압축 파일 내부 검증 (Zip Bomb, 중첩 깊이)

### DevOps
- **Docker Compose** - PostgreSQL 컨테이너 관리
- **Lombok** - 보일러플레이트 코드 제거
- **ModelMapper** - Entity ↔ DTO 변환

---

## 아키텍처 구조

### 배포 환경

```
┌─────────────────────────────────────────────────────┐
│              Vercel (Frontend)                      │
│   https://file-extension-blocker-three.vercel.app  │
└─────────────────┬───────────────────────────────────┘
                  │ HTTPS Request
                  ↓
┌─────────────────────────────────────────────────────┐
│              ngrok (HTTPS Tunnel)                   │
│   https://hilton-roseolar-pauselessly.ngrok-free.dev│
│   - Mixed Content 해결 (HTTPS → HTTPS)              │
│   - 인증 불필요 (ngrok-skip-browser-warning 헤더)   │
└─────────────────┬───────────────────────────────────┘
                  │ HTTP Request (내부)
                  ↓
┌─────────────────────────────────────────────────────┐
│          개인 PC (Backend Server)                    │
│                                                      │
│  ┌────────────────────────────────────────────┐    │
│  │  Spring Boot Application                   │    │
│  │  - Host: 0.0.0.0                          │    │
│  │  - Port: 8800                             │    │
│  │  - External IP: 121.131.197.71            │    │
│  │  - Tunnel: ngrok (항상 실행 필요)          │    │
│  └────────────┬───────────────────────────────┘    │
│               │                                      │
│               ↓                                      │
│  ┌────────────────────────────────────────────┐    │
│  │  PostgreSQL (Docker Container)             │    │
│  │  - Host: localhost                        │    │
│  │  - Port: 5445 (외부) → 5432 (내부)        │    │
│  │  - Database: file_extension_blocker       │    │
│  └────────────────────────────────────────────┘    │
│                                                      │
│  ┌────────────────────────────────────────────┐    │
│  │  File Storage                              │    │
│  │  /Volumes/USB_WOO_2TB/flow-file-storage   │    │
│  └────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

### 포트 매핑

| 서비스 | 내부 포트 | 외부 접속 | 설명 |
|--------|----------|----------|------|
| Spring Boot | 8800 | https://hilton-roseolar-pauselessly.ngrok-free.dev | ngrok HTTPS 터널 (권장) |
| Spring Boot | 8800 | http://121.131.197.71:8800 | 직접 접속 (포트 포워딩 필요) |
| PostgreSQL | 5432 | localhost:5445 | 로컬 전용 |


---

## 주요 구조

### CORE 패키지 (com.woo.core)

공통 기능을 제공하는 재사용 가능한 코어 모듈입니다.

#### **1. config/**
- `CorsConfig.java` - CORS 설정
- `ModelMapperConfig.java` - Entity ↔ DTO 변환 설정

#### **2. controller/**
- `BaseController.java` - CRUD API 기본 구현 제공

#### **3. domain/**
- `BaseEntity.java` - 공통 엔티티 필드 (createdAt, updatedAt, createdBy, updatedBy, isDeleted)

#### **4. repository/**
- `BaseRepository.java` - JPA Repository 기본 인터페이스

#### **5. service/**
- `BaseService.java` - 비즈니스 로직 기본 인터페이스
- `BaseServiceImpl.java` - 공통 CRUD 로직 구현

#### **6. response/**
- `BaseResponse.java` - 통일된 API 응답 포맷
- `ErrorCode.java` - 에러 코드 관리

#### **7. logging/**
- `LayerLoggingAspect.java` - AOP 기반 계층별 로깅
- `loggingInterceptor.java` - HTTP 요청/응답 로깅

#### **8. util/**
- `audit/AuditorAwareImpl.java` - 생성자/수정자 자동 추적
- `common/Identifiable.java` - ID 관리 인터페이스
- `search/SearchCondition.java` - 검색 조건 관리

---

### API 패키지 (com.flow.api)

실제 비즈니스 로직을 담당하는 애플리케이션 레이어입니다.

#### **1. controller/**
REST API 엔드포인트 제공
- `SpaceController.java` - Space 관리
- `MemberController.java` - 멤버 관리
- `BlockedExtensionController.java` - 차단 확장자 관리
- `UploadedFileController.java` - 파일 업로드/다운로드
- `TestFileController.java` - 테스트 파일 제공
- `LogStreamController.java` - 로그 조회

#### **2. domain/**
도메인 엔티티 및 DTO
- **Entity**: `Space`, `Member`, `BlockedExtension`, `UploadedFile`
- **DTO**: `SpaceDto`, `MemberDto`, `BlockedExtensionDto`, `UploadedFileDto`
- **Request/Response**: `SpaceCreationRequest`, `SpaceCreationResponse`

#### **3. repository/**
JPA Repository (Spring Data JPA 활용)
- `SpaceRepository.java`
- `MemberRepository.java`
- `BlockedExtensionRepository.java`
- `UploadedFileRepository.java`

#### **4. service/**
비즈니스 로직 구현
- **Interface**: `SpaceService`, `MemberService`, `BlockedExtensionService`, `UploadedFileService`
- **Implementation**: `SpaceServiceImpl`, `MemberServiceImpl`, `BlockedExtensionServiceImpl`, `UploadedFileServiceImpl`

#### **5. util/**
파일 방어 전략 유틸리티
- `fileDefence/FileValidator.java` - 1단계(확장자) + 2단계(MIME) 검증
- `fileDefence/ZipValidator.java` - 3단계(압축 파일 내부) 검증

---

## 디렉토리 구조

```
backend/
├── build.gradle                    # Gradle 빌드 설정
├── compose.yaml                    # Docker Compose 설정
├── docker/
│   └── init.sql                   # PostgreSQL 초기 데이터 스크립트
├── logs/
│   └── app.log                    # 애플리케이션 로그
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       ├── flow/
│   │   │       │   ├── FileBlockerApplication.java
│   │   │       │   ├── api/
│   │   │       │   │   ├── controller/
│   │   │       │   │   │   ├── BlockedExtensionController.java
│   │   │       │   │   │   ├── LogStreamController.java
│   │   │       │   │   │   ├── MemberController.java
│   │   │       │   │   │   ├── SpaceController.java
│   │   │       │   │   │   ├── TestFileController.java
│   │   │       │   │   │   └── UploadedFileController.java
│   │   │       │   │   ├── domain/
│   │   │       │   │   │   ├── BlockedExtension.java
│   │   │       │   │   │   ├── Member.java
│   │   │       │   │   │   ├── Space.java
│   │   │       │   │   │   ├── UploadedFile.java
│   │   │       │   │   │   └── data/
│   │   │       │   │   │       ├── BlockedExtensionDto.java
│   │   │       │   │   │       ├── MemberDto.java
│   │   │       │   │   │       ├── SpaceCreationRequest.java
│   │   │       │   │   │       ├── SpaceCreationResponse.java
│   │   │       │   │   │       ├── SpaceDto.java
│   │   │       │   │   │       └── UploadedFileDto.java
│   │   │       │   │   ├── repository/
│   │   │       │   │   │   ├── BlockedExtensionRepository.java
│   │   │       │   │   │   ├── MemberRepository.java
│   │   │       │   │   │   ├── SpaceRepository.java
│   │   │       │   │   │   └── UploadedFileRepository.java
│   │   │       │   │   ├── service/
│   │   │       │   │   │   ├── BlockedExtensionService.java
│   │   │       │   │   │   ├── MemberService.java
│   │   │       │   │   │   ├── SpaceService.java
│   │   │       │   │   │   ├── UploadedFileService.java
│   │   │       │   │   │   └── impl/
│   │   │       │   │   │       ├── BlockedExtensionServiceImpl.java
│   │   │       │   │   │       ├── MemberServiceImpl.java
│   │   │       │   │   │       ├── SpaceServiceImpl.java
│   │   │       │   │   │       └── UploadedFileServiceImpl.java
│   │   │       │   │   └── util/
│   │   │       │   └── util/
│   │   │       │       └── fileDefence/
│   │   │       │           ├── FileValidator.java
│   │   │       │           └── ZipValidator.java
│   │   │       └── woo/
│   │   │           └── core/
│   │   │               ├── config/
│   │   │               │   ├── CorsConfig.java
│   │   │               │   └── ModelMapperConfig.java
│   │   │               ├── controller/
│   │   │               │   └── BaseController.java
│   │   │               ├── domain/
│   │   │               │   └── BaseEntity.java
│   │   │               ├── logging/
│   │   │               │   ├── LayerLoggingAspect.java
│   │   │               │   └── loggingInterceptor.java
│   │   │               ├── repository/
│   │   │               │   └── BaseRepository.java
│   │   │               ├── response/
│   │   │               │   ├── BaseResponse.java
│   │   │               │   └── ErrorCode.java
│   │   │               ├── service/
│   │   │               │   ├── BaseService.java
│   │   │               │   └── BaseServiceImpl.java
│   │   │               └── util/
│   │   │                   ├── audit/
│   │   │                   │   └── AuditorAwareImpl.java
│   │   │                   ├── common/
│   │   │                   │   └── Identifiable.java
│   │   │                   └── search/
│   │   │                       └── SearchCondition.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/
│   │       ├── templates/
│   │       └── test-files/
│   │           ├── 1-normal/
│   │           │   ├── document.txt
│   │           │   └── data.json
│   │           ├── 2-blocked-ext/
│   │           │   ├── virus.bat
│   │           │   ├── script.sh
│   │           │   └── hack.php
│   │           ├── 3-disguised/
│   │           │   ├── fake-image.jpg
│   │           │   └── fake-image-bat.jpg
│   │           └── 4-archive/
│   │               ├── normal.zip
│   │               ├── malicious.zip
│   │               ├── zipbomb.zip
│   │               └── nested.zip
│   └── test/
│       └── java/
└── build/

```

---

## API 엔드포인트

### Space 관리
- `GET /api/spaces/space-list` - 모든 Space 조회
- `POST /api/spaces/create-with-admin` - Space + Admin + 고정 확장자 생성

### Member 관리
- `GET /api/members/member-list?spaceId={id}` - Space별 멤버 조회

### 차단 확장자 관리
- `GET /api/blocked-extensions/block-list?spaceId={id}` - 모든 차단 확장자 조회
- `GET /api/blocked-extensions/fixed-block-list?spaceId={id}` - 고정 확장자 조회
- `GET /api/blocked-extensions/custom-block-list?spaceId={id}` - 커스텀 확장자 조회
- `PATCH /api/blocked-extensions/fixed-change-status` - 고정 확장자 활성화/비활성화
- `POST /api/blocked-extensions` - 커스텀 확장자 추가
- `DELETE /api/blocked-extensions/{id}` - 커스텀 확장자 삭제
- `GET /api/blocked-extensions/count-active?spaceId={id}` - 활성화된 전체 확장자 개수

### 파일 업로드/다운로드
- `POST /api/uploaded-files/upload` - 파일 업로드 (4단계 방어 전략 적용)
- `GET /api/uploaded-files/list?spaceId={id}` - 파일 목록 조회
- `GET /api/uploaded-files/download/{fileId}` - 파일 다운로드
- `DELETE /api/uploaded-files/{id}` - 파일 삭제
- `GET /api/uploaded-files/count?spaceId={id}` - 파일 개수 조회

### 로그 조회
- `GET /api/logs/file?lines={n}` - 최근 로그 조회

### 테스트 파일
- `GET /api/test-files/list` - 테스트 파일 목록
- `GET /api/test-files/download/{category}/{filename}` - 테스트 파일 다운로드

---

## 파일 방어 전략 (4단계)

### 1단계: 확장자 Blacklist 검증
- DB에 등록된 차단 확장자 목록과 비교
- Space별로 독립적인 차단 목록 관리

### 2단계: 매직바이트 & MIME 타입 검증
- **2-1**: 실행 파일 탐지 (PE, ELF, Mach-O)
- **2-2**: 확장자 위장 탐지 (Apache Tika 활용)
  - 선언된 확장자와 실제 MIME 타입 비교
  - 차단 확장자의 MIME 타입과 일치 시 차단

### 3단계: 압축 파일 내부 검증
- 재귀적 압축 파일 검증 (ZIP, TAR, GZIP)
- Zip Bomb 탐지 (압축 해제 시 10MB 초과)
- 중첩 깊이 제한 (최대 3단계)

### 4단계: 파일 저장 & 권한 제거
- UUID 기반 고유 파일명 생성
- 파일 권한 제한 (644: rw-r--r--)
- 실행 권한 완전 제거

---

## 실행 방법

### 1. PostgreSQL 시작
```bash
cd backend
docker compose up -d
```

### 2. Spring Boot 실행
```bash
./gradlew bootRun
```

### 3. 로그 확인
```bash
tail -f logs/app.log
```

### 4. Swagger 접속

로컬 개발:
```
http://localhost:8800/swagger-ui/index.html
```

외부 접속:
```
http://121.131.197.71:8800/swagger-ui/index.html
```

---







