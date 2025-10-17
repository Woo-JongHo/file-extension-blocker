# 🧪 테스트 파일 모음

방어 전략 테스트를 위한 파일 모음

## 📂 폴더 구조

```
test-files/
├── 1-normal/          # 정상 파일 (모든 단계 통과)
├── 2-blocked-ext/     # 차단된 확장자 (1단계 차단)
├── 3-disguised/       # 확장자 위장 (2단계 차단)
└── 4-archive/         # 압축 파일 (3단계 테스트)
```

## 🎯 테스트 시나리오

### 1️⃣ 정상 파일 (1-normal/)
- `document.txt` - 일반 텍스트 파일
- `image.jpg` - 정상 이미지 (JPEG)
- `data.json` - JSON 데이터

**예상 결과**: ✅ 모든 단계 통과 → 업로드 성공

---

### 2️⃣ 차단된 확장자 (2-blocked-ext/)
- `virus.bat` - Windows Batch 스크립트
- `malware.exe` - 실행 파일
- `script.sh` - Unix Shell 스크립트
- `hack.php` - PHP 스크립트

**예상 결과**: ❌ 1단계에서 차단 → "'bat' 확장자는 차단되어 업로드할 수 없습니다."

---

### 3️⃣ 확장자 위장 (3-disguised/)
- `fake-image.jpg` - .exe의 매직바이트를 가진 가짜 이미지
- `fake-doc.txt` - ELF 실행 파일인데 확장자만 .txt

**예상 결과**: ❌ 2단계에서 차단 → "실행 파일은 업로드할 수 없습니다. 감지된 타입: application/x-msdownload"

---

### 4️⃣ 압축 파일 (4-archive/)
- `normal.zip` - 정상 파일들만 포함
- `malicious.zip` - 내부에 virus.bat 포함
- `zipbomb.zip` - 압축 해제 시 10MB 초과
- `nested.zip` - 2단계 중첩 압축

**예상 결과**:
- `normal.zip`: ✅ 통과
- `malicious.zip`: ❌ "압축 파일 내부에 차단된 확장자 파일이 있습니다: virus.bat"
- `zipbomb.zip`: ❌ "압축 해제 크기가 10MB를 초과했습니다. (Zip Bomb 의심)"
- `nested.zip`: ❌ "중첩 압축 파일 깊이가 1단계를 초과했습니다"

---

## 📥 다운로드 API

```
GET /api/test-files/{category}/{filename}
```

**예시:**
```
GET /api/test-files/1-normal/document.txt
GET /api/test-files/2-blocked-ext/virus.bat
GET /api/test-files/3-disguised/fake-image.jpg
GET /api/test-files/4-archive/malicious.zip
```

