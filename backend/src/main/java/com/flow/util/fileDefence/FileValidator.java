package com.flow.util.fileDefence;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * 파일 검증 유틸리티 (1,2단계 방어)
 *
 * <p>이 클래스는 strategy.md의 1,2단계 방어 전략을 구현한다:
 * <ul>
 *   <li>1단계: 확장자 Blacklist 검증</li>
 *   <li>2단계: Apache Tika 매직 넘버 검증</li>
 * </ul>
 *
 * <p>사용 예시:
 * <pre>{@code
 * FileValidator validator = new FileValidator(blockedExtensions, tika, maxFileSize);
 * String extension = validator.validateFile(multipartFile);
 * }</pre>
 *
 * <p>주요 메서드:
 * <ul>
 *   <li>{@link #validateFile(MultipartFile)} - 파일 검증 (1,2단계 통합)</li>
 *   <li>{@link #validate1stDefense(MultipartFile)} - 확장자 Blacklist 검증</li>
 *   <li>{@link #validate2ndDefense(MultipartFile, String)} - 매직 넘버 검증</li>
 * </ul>
 *
 * @see ZipValidator
 * @see org.apache.tika.Tika
 */
@Slf4j
public class FileValidator {

  // 실행 파일 MIME 타입 목록
  private static final Set<String> EXECUTABLE_MIME_TYPES = Set.of(
      "application/x-msdownload",      // Windows .exe, .dll
      "application/x-executable",      // Linux ELF
      "application/x-dosexec",         // DOS executable
      "application/x-mach-binary",     // macOS Mach-O
      "application/x-sharedlib",       // Shared libraries (.so, .dylib)
      "application/vnd.microsoft.portable-executable"  // PE format
  );

  private final Set<String> blockedExtensions;
  private final Tika tika;
  private final long maxFileSize;

  public FileValidator(Set<String> blockedExtensions, Tika tika, long maxFileSize) {
    this.blockedExtensions = new HashSet<>(blockedExtensions);
    this.tika = tika;
    this.maxFileSize = maxFileSize;
  }

  /**
   * 파일 검증 (1,2단계 통합)
   *
   * <p>1단계: 확장자 Blacklist 검증
   * <p>2단계: Apache Tika 매직 넘버 검증
   *
   * @param file 업로드 파일
   * @return 검증된 확장자
   * @throws IllegalArgumentException 검증 실패 시
   */
  public String validateFile(MultipartFile file) {
    // 1단계: 확장자 Blacklist 검증
    String extension = validate1stDefense(file);

    // 2단계: Apache Tika 매직 넘버 검증
    validate2ndDefense(file, extension);

    return extension;
  }

  /**
   * 1단계: 확장자 Blacklist 검증 (상세: docs/strategy.md)
   *
   * <p>파일명의 확장자를 추출하여 Blacklist와 비교하여 차단한다.
   *
   * <p>방어 시나리오:
   * <pre>
   * 공격: virus.bat 업로드
   * 검증: 확장자 .bat 추출 -> Blacklist 조회 -> 차단됨
   * </pre>
   *
   * <p>한계:
   * <pre>
   * 공격: malware.exe -> malware.jpg (확장자만 변경)
   * 검증: 확장자 .jpg -> Blacklist에 없음 -> 통과
   * 한계: 파일명만 보고 판단하므로 실제 내용은 검증 불가
   * </pre>
   *
   * @param file 업로드 파일
   * @return 추출된 확장자
   * @throws IllegalArgumentException 차단된 확장자인 경우
   */
  private String validate1stDefense(MultipartFile file) {
    // 파일 기본 검증
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("파일이 비어있습니다.");
    }
    
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.isEmpty()) {
      throw new IllegalArgumentException("파일명이 없습니다.");
    }
    
    // 확장자 추출
    int lastDotIndex = originalFilename.lastIndexOf('.');
    String extension = (lastDotIndex == -1) ? "" : originalFilename.substring(lastDotIndex + 1).toLowerCase();
    
    // 확장자 Blacklist 검증
    if (blockedExtensions.contains(extension.toLowerCase())) {
      throw new IllegalArgumentException(
          String.format("'%s' 확장자는 차단되어 업로드할 수 없습니다.", extension));
    }
    
    // 파일 크기 검증
    if (file.getSize() > maxFileSize) {
      throw new IllegalArgumentException(
          String.format("파일 크기가 너무 큽니다. 최대 크기: %dMB", maxFileSize / (1024 * 1024)));
    }
    
    return extension;
  }

  /**
   * 2단계: Apache Tika 매직 넘버 검증 (상세: docs/strategy.md)
   *
   * <p>1단계를 통과한 파일의 실제 내용을 매직 넘버(파일 시그니처)로 검증한다.
   * <p>1단계의 한계(확장자만 변경한 공격)를 극복한다.
   *
   * <p>방어 시나리오:
   * <pre>
   * 공격: malware.exe -> malware.jpg (확장자만 변경)
   * 1단계: 확장자 .jpg -> 통과 (Blacklist에 없음)
   * 2단계: Tika 매직 넘버 분석 -> "4D 5A" (PE 실행 파일) 감지 -> 차단됨
   * </pre>
   *
   * <p>한계:
   * <pre>
   * 공격: script.bat -> script.jpg (텍스트 스크립트)
   * 2단계: Tika 매직 넘버 분석 -> 매직 넘버 없음 (일반 텍스트) -> 통과
   * 한계: 텍스트 기반 스크립트(.bat, .cmd, .sh)는 매직 넘버가 없어 감지 불가
   * 보완: 1단계 확장자 Blacklist가 주 방어선 역할
   * </pre>
   *
   * @param file 업로드 파일
   * @param extension 1단계에서 추출한 확장자
   * @throws IllegalArgumentException 실행 파일 감지 시
   * @throws RuntimeException Tika 분석 실패 시
   */
  private void validate2ndDefense(MultipartFile file, String extension) {
    try (InputStream inputStream = file.getInputStream()) {
      String detectedMimeType = tika.detect(inputStream, file.getOriginalFilename());

      // 실행 파일 감지 (바이너리 실행 파일만 차단)
      if (isExecutableFile(detectedMimeType)) {
        throw new IllegalArgumentException(
            String.format("실행 파일은 업로드할 수 없습니다. 감지된 타입: %s", detectedMimeType));
      }

    } catch (IOException e) {
      throw new RuntimeException("파일 형식 검증 중 오류가 발생했습니다.", e);
    }
  }

  /**
   * 실행 파일 여부 확인
   *
   * <p>Tika가 감지한 MIME 타입이 실행 파일인지 검증한다.
   *
   * <p>차단 대상:
   * <ul>
   *   <li>application/x-msdownload: Windows PE 실행 파일 (.exe, .dll)</li>
   *   <li>application/x-executable: Unix/Linux ELF 실행 파일</li>
   *   <li>application/x-dosexec: DOS 실행 파일</li>
   *   <li>application/x-mach-binary: macOS Mach-O 실행 파일</li>
   *   <li>application/x-sharedlib: 공유 라이브러리 (.so, .dylib)</li>
   * </ul>
   *
   * @param mimeType Tika가 감지한 MIME 타입
   * @return 실행 파일이면 true
   */
  private boolean isExecutableFile(String mimeType) {
    if (mimeType == null) {
      return false;
    }

    return EXECUTABLE_MIME_TYPES.contains(mimeType.toLowerCase());
  }

  /**
   * 파일명에서 확장자 추출
   *
   * @param fileName 파일명
   * @return 확장자 (점 제외, 소문자)
   */
  public static String extractExtension(String fileName) {
    if (fileName == null || !fileName.contains(".")) {
      return "";
    }
    
    int lastDotIndex = fileName.lastIndexOf('.');
    return fileName.substring(lastDotIndex + 1).toLowerCase();
  }
}

