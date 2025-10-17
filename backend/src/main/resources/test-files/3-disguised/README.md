# 확장자 위장 테스트 파일

## 파일 생성 방법

이 파일들은 **실제 실행 파일의 매직바이트**를 가지고 있지만, **확장자는 정상 파일**로 위장합니다.

### Windows에서 생성:

```powershell
# 1. 정상 실행 파일 준비
copy C:\Windows\System32\notepad.exe temp.exe

# 2. 확장자만 변경
copy temp.exe fake-image.jpg
copy temp.exe fake-doc.txt

# 3. 매직바이트 확인
Format-Hex fake-image.jpg | Select-Object -First 1
# → 4D 5A (PE 실행 파일)
```

### 시나리오:

**fake-image.jpg**
- 확장자: .jpg (정상)
- 매직바이트: `4D 5A` (PE 실행 파일)
- 1단계: ✅ 통과 (.jpg는 차단 목록에 없음)
- 2단계: ❌ 차단 → "실행 파일은 업로드할 수 없습니다. 감지된 타입: application/x-msdownload"

**fake-doc.txt**
- 확장자: .txt (정상)
- 매직바이트: `7F 45 4C 46` (ELF 실행 파일)
- 1단계: ✅ 통과 (.txt는 차단 목록에 없음)
- 2단계: ❌ 차단 → "실행 파일은 업로드할 수 없습니다. 감지된 타입: application/x-executable"

---

**주의**: 실제 악성 파일이 아닌, Windows 기본 실행 파일(notepad.exe 등)을 복사하여 사용하세요.

