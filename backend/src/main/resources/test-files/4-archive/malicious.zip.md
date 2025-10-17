# malicious.zip 생성 방법

## 악성 파일 포함 압축 (3단계 차단)

### 파일 구조:
```
malicious.zip
├── readme.txt      (정상)
├── data.json       (정상)
└── virus.bat       (악성!) ← 차단 대상
```

### PowerShell에서 생성:

```powershell
# 1. 임시 폴더 생성
New-Item -ItemType Directory -Path temp_malicious
cd temp_malicious

# 2. 정상 파일 + 악성 파일
"정상 텍스트" | Out-File readme.txt
'{"test": "data"}' | Out-File data.json
"@echo off`necho virus" | Out-File virus.bat

# 3. ZIP 압축
Compress-Archive -Path * -DestinationPath ../malicious.zip

# 4. 정리
cd ..
Remove-Item temp_malicious -Recurse -Force
```

### 예상 결과:
- ✅ 1단계: .zip 확장자 허용
- ✅ 2단계: application/zip MIME 타입 정상
- ❌ 3단계: 내부에 virus.bat 발견 → **차단!**
  - 에러: "압축 파일 내부에 차단된 확장자 파일이 있습니다: virus.bat (bat)"

