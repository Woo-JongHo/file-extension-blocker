# normal.zip 생성 방법

## 정상 압축 파일 (모든 검증 통과)

### 파일 구조:
```
normal.zip
├── readme.txt
├── data.json
└── report.pdf
```

### PowerShell에서 생성:

```powershell
# 1. 임시 폴더 생성
New-Item -ItemType Directory -Path temp_normal
cd temp_normal

# 2. 정상 파일들 생성
"정상 텍스트 파일" | Out-File readme.txt
'{"test": "data"}' | Out-File data.json
# report.pdf는 실제 PDF 파일 복사

# 3. ZIP 압축
Compress-Archive -Path * -DestinationPath ../normal.zip

# 4. 정리
cd ..
Remove-Item temp_normal -Recurse -Force
```

### 예상 결과:
- ✅ 1단계: .zip 확장자 허용
- ✅ 2단계: application/zip MIME 타입 정상
- ✅ 3단계: 내부 파일 모두 정상 → 업로드 성공

