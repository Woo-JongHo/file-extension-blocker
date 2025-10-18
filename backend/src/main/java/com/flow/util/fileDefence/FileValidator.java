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
   * 공격 1: malware.exe -> malware.jpg (확장자만 변경)
   * 1단계: 확장자 .jpg -> 통과 (Blacklist에 없음)
   * 2단계: Tika 매직 넘버 분석 -> "4D 5A" (PE 실행 파일) 감지 -> 차단됨
   * 
   * 공격 2: script.sh -> fake-image.jpg (텍스트 스크립트)
   * 1단계: 확장자 .jpg -> 통과 (Blacklist에 없음)
   * 2단계: Tika MIME 분석 -> text/plain 감지, .jpg는 image/* 이어야 함 -> MIME 불일치 차단
   * </pre>
   *
   * @param file 업로드 파일
   * @param extension 1단계에서 추출한 확장자
   * @throws IllegalArgumentException 실행 파일 감지 또는 MIME 타입 불일치 시
   * @throws RuntimeException Tika 분석 실패 시
   */
  private void validate2ndDefense(MultipartFile file, String extension) {
    try (InputStream inputStream = file.getInputStream()) {
      String detectedMimeType = tika.detect(inputStream, file.getOriginalFilename());
      
      log.debug("파일: {}, 확장자: {}, 감지된 MIME: {}", 
          file.getOriginalFilename(), extension, detectedMimeType);

      // 2-1. 실행 파일 감지 (바이너리 실행 파일 차단)
      if (isExecutableFile(detectedMimeType)) {
        throw new IllegalArgumentException(
            String.format("실행 파일은 업로드할 수 없습니다. 감지된 타입: %s", detectedMimeType));
      }

      // 2-2. MIME 타입 불일치 검증 (확장자 위장 차단)
      validateMimeTypeConsistency(extension, detectedMimeType, file.getOriginalFilename());

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
   * MIME 타입 일관성 검증 (확장자 위장 차단)
   *
   * <p>Apache Tika의 MimeTypes 데이터베이스를 사용하여 확장자와 실제 파일의 MIME 타입이 일치하는지 검증합니다.
   * <p>예: .jpg 확장자인데 text/plain이면 차단
   *
   * @param extension 파일 확장자
   * @param detectedMimeType Tika가 감지한 MIME 타입
   * @param fileName 파일명 (로깅용)
   * @throws IllegalArgumentException MIME 타입 불일치 시
   */
  private void validateMimeTypeConsistency(String extension, String detectedMimeType, String fileName) {
    if (detectedMimeType == null || extension == null || extension.isEmpty()) {
      return;
    }
    
    try {
      // Tika의 내장 데이터베이스에서 확장자에 대한 예상 MIME 타입 조회
      String dummyFileName = "file." + extension.toLowerCase();
      String expectedMimeString = tika.detect(dummyFileName);
      
      // 일반적인 카테고리 매칭 (image/*, video/*, audio/* 등)
      String detectedCategory = getMimeCategory(detectedMimeType);
      String expectedCategory = getMimeCategory(expectedMimeString);
      
      // application/octet-stream은 Tika가 알 수 없는 파일 타입이므로 검증 스킵
      if ("application/octet-stream".equals(expectedMimeString)) {
        log.debug("알 수 없는 확장자, MIME 검증 스킵: {}", extension);
        return;
      }
      
      // MIME 카테고리가 일치하는지 확인
      if (!detectedCategory.equals(expectedCategory)) {
        log.warn("MIME 타입 불일치 감지 - 파일: {}, 확장자: {}, 예상: {} ({}), 실제: {} ({})", 
            fileName, extension, expectedMimeString, expectedCategory, detectedMimeType, detectedCategory);
        throw new IllegalArgumentException(
            String.format("파일 형식이 올바르지 않습니다. (확장자: .%s, 예상: %s, 실제: %s)", 
                extension, expectedCategory, detectedCategory));
      }
      
    } catch (IllegalArgumentException e) {
      // 검증 실패는 그대로 전파
      throw e;
    } catch (Exception e) {
      // 기타 예외는 로깅만 하고 통과 (너무 엄격하면 정상 파일도 차단될 수 있음)
      log.debug("MIME 검증 중 예외 발생, 검증 스킵: {}", extension, e);
    }
  }
  
  /**
   * MIME 타입에서 주요 카테고리 추출
   *
   * <p>예: "image/jpeg" → "image", "application/pdf" → "application/pdf"
   *
   * @param mimeType MIME 타입
   * @return MIME 카테고리
   */
  private String getMimeCategory(String mimeType) {
    if (mimeType == null) {
      return "unknown";
    }
    
    // 특정 MIME 타입은 전체 매칭 (너무 세분화된 타입)
    if (mimeType.equals("application/pdf") 
        || mimeType.equals("application/json") 
        || mimeType.equals("application/xml")) {
      return mimeType;
    }
    
    // 일반적인 경우 주요 카테고리만 추출 (image/*, video/*, audio/*, text/* 등)
    int slashIndex = mimeType.indexOf('/');
    return slashIndex > 0 ? mimeType.substring(0, slashIndex + 1) + "*" : mimeType;
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

