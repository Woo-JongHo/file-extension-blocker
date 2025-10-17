# 테스트 파일 자동 생성 스크립트

Write-Host "🚀 테스트 파일 생성 시작..." -ForegroundColor Green

# 현재 위치
$baseDir = $PSScriptRoot

# ============================================
# 1. normal.zip (정상 압축 파일)
# ============================================
Write-Host "`n📦 1. normal.zip 생성 중..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path "$baseDir\temp_normal" -Force | Out-Null
Set-Location "$baseDir\temp_normal"

"정상 텍스트 파일입니다." | Out-File readme.txt -Encoding UTF8
@"
{
  "test": "normal data"
}
"@ | Out-File data.json -Encoding UTF8
"보고서 내용입니다." | Out-File report.txt -Encoding UTF8

Compress-Archive -Path * -DestinationPath "$baseDir\4-archive\normal.zip" -Force
Set-Location $baseDir
Remove-Item temp_normal -Recurse -Force
Write-Host "✅ normal.zip 생성 완료" -ForegroundColor Green

# ============================================
# 2. malicious.zip (악성 파일 포함)
# ============================================
Write-Host "`n📦 2. malicious.zip 생성 중..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path "$baseDir\temp_malicious" -Force | Out-Null
Set-Location "$baseDir\temp_malicious"

"정상 텍스트" | Out-File readme.txt -Encoding UTF8
@"
{
  "test": "data"
}
"@ | Out-File data.json -Encoding UTF8
@"
@echo off
echo This is a test virus file
"@ | Out-File virus.bat -Encoding UTF8

Compress-Archive -Path * -DestinationPath "$baseDir\4-archive\malicious.zip" -Force
Set-Location $baseDir
Remove-Item temp_malicious -Recurse -Force
Write-Host "✅ malicious.zip 생성 완료 (virus.bat 포함)" -ForegroundColor Green

# ============================================
# 3. zipbomb.zip (압축 폭탄)
# ============================================
Write-Host "`n💣 3. zipbomb.zip 생성 중..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path "$baseDir\temp_zipbomb" -Force | Out-Null
Set-Location "$baseDir\temp_zipbomb"

# 15MB 파일 생성 (10MB 초과하여 차단되도록)
$size = 15MB
$path = "huge.txt"
$stream = [System.IO.File]::Create($path)
$stream.SetLength($size)
$stream.Close()

Compress-Archive -Path huge.txt -DestinationPath "$baseDir\4-archive\zipbomb.zip" -CompressionLevel Optimal -Force

# 압축률 확인
$original = (Get-Item huge.txt).Length
$compressed = (Get-Item "$baseDir\4-archive\zipbomb.zip").Length
$ratio = [math]::Round($original / $compressed, 2)
Write-Host "   압축률: ${ratio}배 (원본: $([math]::Round($original/1MB, 2))MB → 압축: $([math]::Round($compressed/1KB, 2))KB)" -ForegroundColor Cyan

Set-Location $baseDir
Remove-Item temp_zipbomb -Recurse -Force
Write-Host "✅ zipbomb.zip 생성 완료" -ForegroundColor Green

# ============================================
# 4. nested.zip (중첩 압축)
# ============================================
Write-Host "`n📦 4. nested.zip 생성 중..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path "$baseDir\temp_nested" -Force | Out-Null
Set-Location "$baseDir\temp_nested"

# Level 2
"Level 2 content" | Out-File level2.txt -Encoding UTF8
Compress-Archive -Path level2.txt -DestinationPath level2.zip -Force
Remove-Item level2.txt

# Level 1
Compress-Archive -Path level2.zip -DestinationPath level1.zip -Force
Remove-Item level2.zip

# Level 0 (최상위)
Compress-Archive -Path level1.zip -DestinationPath "$baseDir\4-archive\nested.zip" -Force

Set-Location $baseDir
Remove-Item temp_nested -Recurse -Force
Write-Host "✅ nested.zip 생성 완료 (2단계 중첩)" -ForegroundColor Green

# ============================================
# 5. 확장자 위장 파일 (fake-image.jpg)
# ============================================
Write-Host "`n🎭 5. 확장자 위장 파일 생성 중..." -ForegroundColor Yellow

# Windows 실행 파일을 복사하여 확장자만 변경
if (Test-Path "C:\Windows\System32\notepad.exe") {
  Copy-Item "C:\Windows\System32\notepad.exe" "$baseDir\3-disguised\fake-image.jpg" -Force
  Write-Host "✅ fake-image.jpg 생성 완료 (.exe → .jpg 위장)" -ForegroundColor Green
} else {
  Write-Host "⚠️  notepad.exe를 찾을 수 없습니다. fake-image.jpg 생성 스킵" -ForegroundColor Yellow
}

# ============================================
# 완료
# ============================================
Write-Host "`n🎉 테스트 파일 생성 완료!" -ForegroundColor Green
Write-Host "`n📁 생성된 파일 목록:" -ForegroundColor Cyan
Write-Host "  ✅ 4-archive/normal.zip" -ForegroundColor White
Write-Host "  ✅ 4-archive/malicious.zip" -ForegroundColor White
Write-Host "  ✅ 4-archive/zipbomb.zip" -ForegroundColor White
Write-Host "  ✅ 4-archive/nested.zip" -ForegroundColor White
Write-Host "  ✅ 3-disguised/fake-image.jpg" -ForegroundColor White

Write-Host "`n🧪 테스트 방법:" -ForegroundColor Cyan
Write-Host "  GET /api/test-files/download/4-archive/normal.zip" -ForegroundColor White
Write-Host "  GET /api/test-files/download/4-archive/malicious.zip" -ForegroundColor White

