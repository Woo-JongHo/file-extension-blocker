# File Extension Blocker - Frontend

Vite + React로 구축된 파일 확장자 차단 시스템 프론트엔드

## 🛠️ 기술 스택

- **React 18** - UI 라이브러리
- **Vite** - 빌드 도구
- **React Router v6** - 라우팅
- **Axios** - HTTP 클라이언트
- **Tailwind CSS** - 스타일링

## 🚀 시작하기

### 개발 서버 실행

```bash
npm install
npm run dev
```

서버가 `http://localhost:3000`에서 실행됩니다.

### 빌드

```bash
npm run build
```

### 프리뷰

```bash
npm run preview
```

## 📁 폴더 구조

```
frontend/
├── src/
│   ├── components/     # 재사용 가능한 컴포넌트
│   ├── pages/          # 페이지 컴포넌트
│   ├── services/       # API 서비스
│   └── App.jsx         # 루트 컴포넌트
├── public/             # 정적 파일
└── package.json        # 의존성 관리
```

## 🎯 네이밍 규칙

모든 파일명은 **kebab-case** 사용:
- `space-list-page.jsx`
- `space-service.js`
- `custom-extension-section.jsx`

## 🔌 API 연동

백엔드 API: `http://localhost:8800`

Vite Proxy 설정으로 CORS 해결:
- `/api/*` → `http://localhost:8800/api/*`

BaseResponse 구조:
```json
{
  "timestamp": "2025-10-17 12:00:00",
  "status": 200,
  "message": "Space 목록 조회 완료",
  "data": [...],
  "errorCode": null,
  "errorDetail": null
}
```

