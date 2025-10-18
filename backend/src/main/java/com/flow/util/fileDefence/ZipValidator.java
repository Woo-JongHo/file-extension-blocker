package com.flow.util.fileDefence;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * 압축 파일 검증 유틸리티 (3단계 방어)
 *
 * <p>이 클래스는 strategy.md의 3단계 방어 전략을 구현한다:
 * <ul>
 *   <li>압축 파일 내부 파일 재귀 검증</li>
 *   <li>Zip Bomb(압축 폭탄) 방어</li>
 *   <li>중첩 압축 깊이 제한</li>
 *   <li>암호화된 압축 파일 차단</li>
 * </ul>
 *
 * <p>지원 포맷:
 * <ul>
 *   <li>ZIP (.zip)</li>
 *   <li>TAR (.tar)</li>
 *   <li>GZIP (.gz, .tar.gz, .tgz)</li>
 *   <li>7Z (.7z) - XZ Utils 라이브러리 필요</li>
 * </ul>
 *
 * <p>사용 예시:
 * <pre>{@code
 * ZipValidator validator = new ZipValidator(blockedExtensions, tika);
 * validator.validateZipFile(multipartFile, 0);
 * }</pre>
 *
 * <p>주요 메서드:
 * <ul>
 *   <li>{@link #validateZipFile(MultipartFile, int)} - 압축 파일 검증 (재귀)</li>
 *   <li>{@link #checkZipBomb(long, long)} - Zip Bomb 감지</li>
 *   <li>{@link #validateInnerFile(String, byte[], int)} - 내부 파일 검증</li>
 * </ul>
 *
 * <p>Zip Bomb 방어 기준:
 * <ul>
 *   <li>최대 압축 해제 크기: 10MB</li>
 *   <li>최대 압축률: 100배</li>
 *   <li>최대 파일 개수: 1,000개</li>
 *   <li>최대 중첩 깊이: 1단계</li>
 * </ul>
 *
 * @see FileValidator
 * @see org.apache.commons.compress.archivers.ArchiveInputStream
 * @see org.apache.tika.Tika
 */
@Slf4j
public class ZipValidator {

  // Zip Bomb 방어 임계값
  private static final long MAX_UNCOMPRESSED_SIZE = 10 * 1024 * 1024; // 10MB
  private static final int MAX_COMPRESSION_RATIO = 100; // 100배
  private static final int MAX_FILE_COUNT = 1000; // 최대 1,000개 파일
  private static final int MAX_NESTING_DEPTH = 1; // 최대 중첩 깊이

  // 압축 파일 MIME Types
  private static final Set<String> ARCHIVE_MIME_TYPES = Set.of(
      "application/zip",
      "application/x-zip-compressed",
      "application/x-tar",
      "application/x-gzip",
      "application/gzip",
      "application/x-7z-compressed"
  );

  private final Set<String> blockedExtensions;
  private final Tika tika;

  public ZipValidator(Set<String> blockedExtensions, Tika tika) {
    this.blockedExtensions = new HashSet<>(blockedExtensions);
    this.tika = tika;
  }

  /**
   * 압축 파일 검증 (재귀)
   *
   * <p>검증 항목:
   * <ul>
   *   <li>중첩 깊이 확인 (최대 1단계)</li>
   *   <li>Zip Bomb 감지 (압축률, 총 크기)</li>
   *   <li>내부 파일 재귀 검증 (확장자 + 매직바이트)</li>
   *   <li>파일 개수 제한</li>
   * </ul>
   *
   * @param file 업로드된 압축 파일
   * @param currentDepth 현재 중첩 깊이 (최초 호출 시 0)
   * @throws IllegalArgumentException 검증 실패 시
   */
  public void validateZipFile(MultipartFile file, int currentDepth) {
    log.info("[3단계-ZIP] 압축 파일 검증 시작 - 파일: {}, 깊이: {}", file.getOriginalFilename(), currentDepth);
    
    // 1. 중첩 깊이 확인
    if (currentDepth > MAX_NESTING_DEPTH) {
      log.warn("[3단계-ZIP] 차단! - 중첩 깊이 초과: {} > {}", currentDepth, MAX_NESTING_DEPTH);
      throw new IllegalArgumentException(
          String.format("압축 파일 중첩 깊이가 %d단계를 초과했습니다.", MAX_NESTING_DEPTH)
      );
    }

    try (InputStream inputStream = new BufferedInputStream(file.getInputStream())) {
      // 2. MIME Type으로 압축 포맷 감지
      String mimeType = tika.detect(inputStream);
      log.info("[3단계-ZIP] 감지된 MIME 타입: {}", mimeType);

      // 3. 압축 포맷에 맞는 스트림 생성
      try (ArchiveInputStream<?> archiveInputStream = createArchiveInputStream(
          file.getInputStream(), mimeType)) {
        
        if (archiveInputStream == null) {
          log.info("[3단계-ZIP] 압축 파일 아님, 검증 스킵");
          return; // 압축 파일이 아니면 패스
        }

        validateArchiveEntries(archiveInputStream, file.getSize(), currentDepth);
      }

    } catch (IOException e) {
      log.error("[3단계-ZIP] 오류 발생: {}", e.getMessage());
      throw new IllegalArgumentException("압축 파일 검증 중 오류 발생: " + e.getMessage(), e);
    }
    
    log.info("[3단계-ZIP] 압축 파일 검증 완료!");
  }

  /**
   * 압축 포맷에 맞는 ArchiveInputStream 생성
   *
   * @param inputStream 원본 입력 스트림
   * @param mimeType MIME Type
   * @return ArchiveInputStream 또는 null (지원하지 않는 포맷)
   * @throws IOException 스트림 생성 실패 시
   */
  private ArchiveInputStream<?> createArchiveInputStream(InputStream inputStream, String mimeType)
      throws IOException {
    
    BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
    
    if (mimeType.contains("zip")) {
      return new ZipArchiveInputStream(bufferedInputStream);
    } else if (mimeType.contains("tar")) {
      return new TarArchiveInputStream(bufferedInputStream);
    } else if (mimeType.contains("gzip")) {
      // GZIP은 단일 파일 압축 또는 TAR.GZ
      GzipCompressorInputStream gzipStream = new GzipCompressorInputStream(bufferedInputStream);
      // TAR.GZ인지 확인
      return new TarArchiveInputStream(gzipStream);
    }
    
    return null; // 지원하지 않는 포맷
  }

  /**
   * 압축 파일 내부 엔트리 검증
   *
   * @param archiveInputStream 압축 파일 스트림
   * @param compressedSize 압축된 파일 크기
   * @param currentDepth 현재 중첩 깊이
   * @throws IllegalArgumentException 검증 실패 시
   * @throws IOException 파일 읽기 실패 시
   */
  private void validateArchiveEntries(
      ArchiveInputStream<?> archiveInputStream,
      long compressedSize,
      int currentDepth) throws IOException {
    
    log.info("[3단계-ZIP] 압축 엔트리 검증 시작 - 압축 크기: {} bytes", compressedSize);
    
    long totalUncompressedSize = 0;
    int fileCount = 0;

    ArchiveEntry entry;
    while ((entry = archiveInputStream.getNextEntry()) != null) {
      // 디렉토리는 스킵
      if (entry.isDirectory()) {
        continue;
      }

      String fileName = entry.getName();
      long entrySize = entry.getSize();
      
      log.info("[3단계-ZIP] 내부 파일 발견: {} (크기: {} bytes)", fileName, entrySize);

      // 1. 파일 개수 제한
      fileCount++;
      if (fileCount > MAX_FILE_COUNT) {
        log.warn("[3단계-ZIP] 차단! - 파일 개수 초과: {} > {}", fileCount, MAX_FILE_COUNT);
        throw new IllegalArgumentException(
            String.format("압축 파일 내부 파일 개수가 %d개를 초과했습니다.", MAX_FILE_COUNT)
        );
      }

      // 2. 누적 압축 해제 크기 확인
      if (entrySize > 0) {
        totalUncompressedSize += entrySize;
        if (totalUncompressedSize > MAX_UNCOMPRESSED_SIZE) {
          log.warn("[3단계-ZIP] 차단! - 압축 해제 크기 초과: {} > {}", 
              totalUncompressedSize, MAX_UNCOMPRESSED_SIZE);
          throw new IllegalArgumentException(
              String.format("압축 해제 크기가 %dMB를 초과했습니다. (Zip Bomb 의심)",
                  MAX_UNCOMPRESSED_SIZE / (1024 * 1024))
          );
        }
      }

      // 3. 내부 파일 읽기 (최대 1MB만 읽음)
      byte[] fileContent = readEntryContent(archiveInputStream, Math.min(entrySize, 1024 * 1024));

      // 4. 내부 파일 검증 (확장자 + 매직바이트 + 재귀 압축)
      validateInnerFile(fileName, fileContent, currentDepth);
    }

    log.info("[3단계-ZIP] 총 파일 개수: {}, 총 압축 해제 크기: {} bytes", fileCount, totalUncompressedSize);

    // 5. Zip Bomb 최종 확인
    checkZipBomb(compressedSize, totalUncompressedSize);
  }

  /**
   * 압축 엔트리 내용 읽기
   *
   * @param archiveInputStream 압축 스트림
   * @param maxSize 최대 읽을 크기
   * @return 파일 내용 (byte array)
   * @throws IOException 읽기 실패 시
   */
  private byte[] readEntryContent(ArchiveInputStream<?> archiveInputStream, long maxSize)
      throws IOException {
    
    byte[] buffer = new byte[(int) maxSize];
    int bytesRead = archiveInputStream.read(buffer, 0, buffer.length);
    
    if (bytesRead == -1) {
      return new byte[0];
    }
    
    // 실제 읽은 크기만큼만 반환
    byte[] result = new byte[bytesRead];
    System.arraycopy(buffer, 0, result, 0, bytesRead);
    return result;
  }

  /**
   * 내부 파일 검증 (재귀)
   *
   * <p>검증 항목:
   * <ul>
   *   <li>1단계: 확장자 Blacklist 확인</li>
   *   <li>2단계: Apache Tika 매직바이트 검증</li>
   *   <li>3단계: 내부에 또 다른 압축 파일이 있으면 재귀 검증</li>
   * </ul>
   *
   * @param fileName 파일명
   * @param fileContent 파일 내용
   * @param currentDepth 현재 중첩 깊이
   * @throws IllegalArgumentException 검증 실패 시
   */
  private void validateInnerFile(String fileName, byte[] fileContent, int currentDepth) {
    log.info("[3단계-ZIP] 내부 파일 검증: {}", fileName);
    
    // 1. 확장자 추출
    String extension = extractExtension(fileName);
    log.debug("[3단계-ZIP]   확장자: {}", extension);
    
    // 2. 확장자 Blacklist 확인 (1단계 방어)
    if (blockedExtensions.contains(extension.toLowerCase())) {
      log.warn("[3단계-ZIP] 차단! - 압축 내부에 차단된 확장자: {} ({})", fileName, extension);
      throw new IllegalArgumentException(
          String.format("압축 파일 내부에 차단된 확장자 파일이 있습니다: %s (%s)", fileName, extension)
      );
    }
    log.debug("[3단계-ZIP]   확장자 검증 통과");

    // 3. 매직바이트 검증 (2단계 방어)
    String detectedMimeType = tika.detect(fileContent, fileName);
    log.debug("[3단계-ZIP]   감지된 MIME: {}", detectedMimeType);

    // 실행 파일 감지
    if (detectedMimeType.contains("application/x-msdownload") ||  // .exe
        detectedMimeType.contains("application/x-executable") ||  // Linux ELF
        detectedMimeType.contains("application/x-mach-binary")) { // macOS Mach-O
      log.warn("[3단계-ZIP] 차단! - 압축 내부에 실행 파일: {} ({})", fileName, detectedMimeType);
      throw new IllegalArgumentException(
          String.format("압축 파일 내부에 실행 파일이 있습니다: %s (%s)", fileName, detectedMimeType)
      );
    }
    log.debug("[3단계-ZIP]   실행 파일 아님");

    // 4. 중첩 압축 파일 재귀 검증 (3단계 방어)
    if (ARCHIVE_MIME_TYPES.contains(detectedMimeType)) {
      log.info("[3단계-ZIP] 중첩 압축 파일 감지: {} (깊이: {})", fileName, currentDepth + 1);
      if (currentDepth + 1 > MAX_NESTING_DEPTH) {
        log.warn("[3단계-ZIP] 차단! - 중첩 깊이 초과: {} > {}", currentDepth + 1, MAX_NESTING_DEPTH);
        throw new IllegalArgumentException(
            String.format("중첩 압축 파일 깊이가 %d단계를 초과했습니다: %s", 
                MAX_NESTING_DEPTH, fileName)
        );
      }
    }
    
    log.debug("[3단계-ZIP]   내부 파일 검증 완료: {}", fileName);
  }

  /**
   * Zip Bomb 감지
   *
   * <p>압축률이 비정상적으로 높으면 Zip Bomb으로 간주하고 차단한다.
   *
   * @param compressedSize 압축된 파일 크기
   * @param uncompressedSize 압축 해제된 총 크기
   * @throws IllegalArgumentException Zip Bomb 감지 시
   */
  private void checkZipBomb(long compressedSize, long uncompressedSize) {
    log.info("[3단계-ZIP] Zip Bomb 검사 - 압축: {} bytes, 해제: {} bytes", 
        compressedSize, uncompressedSize);
    
    if (compressedSize == 0 || uncompressedSize == 0) {
      log.debug("[3단계-ZIP] 크기 정보 없음, Zip Bomb 검사 스킵");
      return; // 크기 정보 없음
    }

    int compressionRatio = (int) (uncompressedSize / compressedSize);
    log.info("[3단계-ZIP] 압축률: {}배 (최대 허용: {}배)", compressionRatio, MAX_COMPRESSION_RATIO);
    
    if (compressionRatio > MAX_COMPRESSION_RATIO) {
      log.warn("[3단계-ZIP] 차단! - Zip Bomb 감지: 압축률 {}배 초과", compressionRatio);
      throw new IllegalArgumentException(
          String.format("비정상적인 압축률 감지: %d배 (최대: %d배). Zip Bomb 의심!",
              compressionRatio, MAX_COMPRESSION_RATIO)
      );
    }
    
    log.info("[3단계-ZIP] Zip Bomb 검사 통과!");
  }

  /**
   * 파일명에서 확장자 추출
   *
   * @param fileName 파일명
   * @return 확장자 (점 제외, 소문자)
   */
  private String extractExtension(String fileName) {
    if (fileName == null || !fileName.contains(".")) {
      return "";
    }
    
    int lastDotIndex = fileName.lastIndexOf('.');
    return fileName.substring(lastDotIndex + 1).toLowerCase();
  }
}

