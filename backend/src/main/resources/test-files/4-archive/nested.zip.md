# nested.zip 생성 방법

## 중첩 압축 파일 (3단계 차단)

### 파일 구조:
```
nested.zip (깊이 0)
└── level1.zip (깊이 1)
    └── level2.zip (깊이 2) ← 차단!
```

### PowerShell에서 생성:

```powershell
# 1. 가장 내부 파일 생성
"Level 2 content" | Out-File level2.txt
Compress-Archive -Path level2.txt -DestinationPath level2.zip
Remove-Item level2.txt

# 2. 중간 레벨 생성
Compress-Archive -Path level2.zip -DestinationPath level1.zip
Remove-Item level2.zip

# 3. 최상위 레벨 생성
Compress-Archive -Path level1.zip -DestinationPath nested.zip
Remove-Item level1.zip
```

### 예상 결과:
- ✅ 1단계: .zip 확장자 허용
- ✅ 2단계: application/zip MIME 타입 정상
- ✅ 3단계: nested.zip 열기 시작
  - level1.zip 발견 (중첩 깊이 1) → ✅ 허용 (MAX_NESTING_DEPTH = 1)
  - level2.zip 발견 (중첩 깊이 2) → ❌ **차단!**
  - 에러: "중첩 압축 파일 깊이가 1단계를 초과했습니다: level2.zip"

### 임계값:
```java
private static final int MAX_NESTING_DEPTH = 1; // 최대 중첩 깊이
```

**현재 설정**: 1단계까지만 허용 (nested.zip → level1.zip까지만)

