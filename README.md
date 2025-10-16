# 📋 File Extension Blocker

파일 확장자 차단 시스템

---

## 👨‍💻 과제 정보

**작성자**: 우종호

**과제 시작일**: 2025.10.14 (화)  
**과제 마감일**: 2025.10.18 (토) 23:00

---

## 📢 과제 공지

### 제출 과제
1. **"파일 확장자 차단" 과제 제출**
2. **파일 확장자 차단 : 화면 호출까지**

### 제출 파일
1. 면접 당일까지 유지되는 사이트 제출
2. GitHub 주소 제출

---

## 📖 과제 설명

어떤 파일들은 첨부 시 보안에 문제가 될 수 있습니다. 특히 `exe`, `sh` 등의 실행파일이 존재할 경우 서버에 올려서 실행될 수 있는 위험이 있어 파일 확장자를 차단하게 되었습니다.

### 요구사항

#### 1. 고정 확장자
- **1-1** 고정 확장자는 차단을 자주하는 확장자 리스트이며, default는 unCheck 되어 있습니다.
- **1-2** 고정 확장자를 check 또는 unCheck 할 경우 DB에 저장됩니다.
  - 새로고침 시 유지
  - 아래쪽 커스텀 확장자에는 표현되지 않아야 함

#### 2. 확장자 입력
- **2-1** 확장자 최대 입력 길이는 20자리
- **2-2** 추가 버튼 클릭 시 DB에 저장되며, 아래쪽 커스텀 확장자 영역에 표시

#### 3. 커스텀 확장자
- **3-1** 커스텀 확장자는 최대 200개까지 추가 가능
- **3-2** 확장자 옆 X를 클릭 시 DB에서 삭제

### 추가 고려사항
> 위 요건 이외에 어떤 점을 고려했는지 적어주세요.

---

## 📚 문서

- **과제에 대한 전략**: [strategy.md](docs/strategy.md)에서 확인할 수 있습니다.
- **데이터베이스 설계 전략 및 스키마**: [sql.md](docs/sql.md)에서 확인할 수 있습니다.

---

## 🚀 시작하기

### 필수 요구사항
- Java 17+
- Docker Desktop
- Gradle

### 설치 및 실행

#### 1. 저장소 클론
```bash
git clone https://github.com/yourusername/file-extension-blocker.git
cd file-extension-blocker
```

#### 2. 데이터베이스 실행
```bash
cd backend
docker compose up -d
```

#### 3. 백엔드 실행
```bash
./gradlew bootRun
```

애플리케이션이 `http://localhost:8800`에서 실행됩니다.

## 📡 서비스 포트

- **Backend API**: `http://localhost:8800`
- **PostgreSQL**: `localhost:5445`

## 🗄️ 데이터베이스 접속 정보

- Host: `localhost`
- Port: `5445`
- Database: `flow-assignment-fileblocker`
- Username: `flow-user`
- Password: `flow12!@`

## 🔧 유용한 명령어

### Docker
```bash
# 컨테이너 시작
docker compose up -d

# 컨테이너 중지
docker compose down

# 로그 확인
docker compose logs -f
```

### Gradle
```bash
# 애플리케이션 실행
./gradlew bootRun

# 빌드
./gradlew build

# 테스트
./gradlew test
```

