# zipbomb.zip 생성 방법

## Zip Bomb (압축 폭탄) - 3단계 차단

### 개념:
```
압축 파일: 1MB
압축 해제: 100MB+  (100배 이상)
→ Zip Bomb 의심!
```

### PowerShell에서 생성:

```powershell
# 1. 큰 파일 생성 (15MB)
$size = 15MB
$path = "huge.txt"
$stream = [System.IO.File]::Create($path)
$stream.SetLength($size)
$stream.Close()

# 2. 압축 (압축률이 매우 높음)
Compress-Archive -Path huge.txt -DestinationPath zipbomb.zip -CompressionLevel Optimal

# 3. 확인
$original = (Get-Item huge.txt).Length
$compressed = (Get-Item zipbomb.zip).Length
$ratio = [math]::Round($original / $compressed, 2)
Write-Host "압축률: ${ratio}배"

# 4. 정리
Remove-Item huge.txt
```

### 예상 결과:
- ✅ 1단계: .zip 확장자 허용
- ✅ 2단계: application/zip MIME 타입 정상
- ❌ 3단계: 압축 해제 크기 10MB 초과 → **차단!**
  - 에러: "압축 해제 크기가 10MB를 초과했습니다. (Zip Bomb 의심)"

### 임계값:
- 최대 압축 해제 크기: **10MB**
- 최대 압축률: **100배**

현재 설정:
```java
private static final long MAX_UNCOMPRESSED_SIZE = 10 * 1024 * 1024; // 10MB
private static final int MAX_COMPRESSION_RATIO = 100; // 100배
```

