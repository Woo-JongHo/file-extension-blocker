package com.flow.api.service.impl;

import com.flow.api.domain.BlockedExtension;
import com.flow.api.domain.UploadedFile;
import com.flow.api.repository.UploadedFileRepository;
import com.flow.api.service.BlockedExtensionService;
import com.flow.api.service.UploadedFileService;
import com.woo.core.service.BaseServiceImpl;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UploadedFileServiceImpl extends BaseServiceImpl<UploadedFile> implements UploadedFileService {

  private final UploadedFileRepository uploadedFileRepository;
  private final BlockedExtensionService blockedExtensionService;
  private final Tika tika;

  @Value("${file.upload.max-size:10485760}")
  private long maxFileSize;

  public UploadedFileServiceImpl(
      UploadedFileRepository uploadedFileRepository,
      BlockedExtensionService blockedExtensionService) {
    super(uploadedFileRepository);
    this.uploadedFileRepository = uploadedFileRepository;
    this.blockedExtensionService = blockedExtensionService;
    this.tika = new Tika();
  }

  @Override
  @Transactional(readOnly = true)
  public List<UploadedFile> getFilesBySpace(Long spaceId) {
    return uploadedFileRepository.findBySpaceIdAndIsDeletedFalse(spaceId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UploadedFile> getFilesByUploader(Long memberId) {
    return uploadedFileRepository.findByCreatedByAndIsDeletedFalse(memberId);
  }

  @Override
  @Transactional(readOnly = true)
  public Long countFilesBySpace(Long spaceId) {
    return uploadedFileRepository.countBySpaceIdAndIsDeletedFalse(spaceId);
  }


  /**
   * 파일 업로드 (방어 전략 4단계 적용)
   *
   * <p>strategy.md의 방어 전략을 단계별로 적용하여 파일을 검증 후 업로드
   *
   * <p>방어 전략:
   * <ul>
   *   <li>1단계: 확장자 Blacklist 검증 {@link #validate1stDefense} (구현 완료)</li>
   *   <li>2단계: Apache Tika 매직 넘버 검증 {@link #validate2ndDefense} (구현 완료)</li>
   *   <li>3단계: Polyglot 공격 방어 (서버 설정 - 인프라 설정 필요)</li>
   *   <li>4단계: 압축 파일 내부 검증 (미구현)</li>
   * </ul>
   *
   * <p>현재 적용 단계:
   * <ul>
   *   <li>1단계: 확장자 Blacklist로 텍스트 스크립트(.bat, .cmd, .sh 등) 차단</li>
   *   <li>2단계: 매직 넘버로 바이너리 실행 파일(.exe, .elf) 차단</li>
   * </ul>
   */
  @Override
  public UploadedFile uploadFile(Long spaceId, MultipartFile file) {
    // 2단계 방어 (1단계 포함)
    String extension = validate2ndDefense(spaceId, file);

    // TODO: 4단계 압축 파일 내부 검증
    // validate4thDefense(spaceId, file, extension);

    return saveFileMetadata(spaceId, file, extension);
  }

  /**
   * 확장자 차단 여부 확인
   * 
   * <p>검증 흐름: DB 조회 → List를 Set으로 변환 → 차단 여부 확인
   * 
   * <p>시간 복잡도: O(n)
   * <ul>
   *   <li>매 호출마다 Set 변환: O(n)</li>
   *   <li>Set 검증: O(1)</li>
   *   <li>전체: O(n)</li>
   * </ul>
   * 
   * <p>최적화:
   * <ul>
   *   <li>캐싱으로 O(1) 가능하지만, 차단 확장자 최대 206개(고정 6 + 커스텀 200)로 성능 영향 미미하여 제외</li>
   * </ul>
   */
  @Override
  @Transactional(readOnly = true)
  public boolean isExtensionBlocked(Long spaceId, String extension) {
    if (extension == null || extension.isEmpty()) {
      return true;
    }
    
    List<BlockedExtension> blockedExtensions = blockedExtensionService.getBlockedExtensions(spaceId);
    
    Set<String> blockedSet = new HashSet<>();
    for (BlockedExtension be : blockedExtensions) {
      blockedSet.add(be.getExtension().toLowerCase());
    }
    
    String normalizedExtension = extension.toLowerCase().trim();
    return blockedSet.contains(normalizedExtension);
  }

  // ═══════════════════════════════════════════════════════════
  // Private 검증 메서드들
  // ═══════════════════════════════════════════════════════════


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
   * @param spaceId 공간 ID
   * @param file 업로드 파일
   * @return 검증된 확장자
   * @throws IllegalArgumentException 실행 파일 감지 시
   * @throws RuntimeException Tika 분석 실패 시
   */
  private String validate2ndDefense(Long spaceId, MultipartFile file) {
    // 1단계: 기본 검증 + 확장자 Blacklist (재사용)
    String extension = validate1stDefense(spaceId, file);

    // 2단계: Apache Tika 매직 넘버 검증
    try (InputStream inputStream = file.getInputStream()) {
      String detectedMimeType = tika.detect(inputStream, file.getOriginalFilename());

      // 실행 파일 감지 (바이너리 실행 파일만 차단)
      if (isExecutableFile(detectedMimeType)) {
        throw new IllegalArgumentException(
            String.format("실행 파일은 업로드할 수 없습니다. 감지된 타입: %s", detectedMimeType));
      }

      return extension;

    } catch (IOException e) {
      throw new RuntimeException("파일 형식 검증 중 오류가 발생했습니다.", e);
    }
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
   * @param spaceId 공간 ID
   * @param file 업로드 파일
   * @return 추출된 확장자
   * @throws IllegalArgumentException 차단된 확장자인 경우
   */
  private String validate1stDefense(Long spaceId, MultipartFile file) {
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
    if (isExtensionBlocked(spaceId, extension)) {
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

    // 실행 파일 MIME 타입 목록
    Set<String> executableMimeTypes = Set.of(
        "application/x-msdownload",      // Windows .exe, .dll
        "application/x-executable",      // Linux ELF
        "application/x-dosexec",         // DOS executable
        "application/x-mach-binary",     // macOS Mach-O
        "application/x-sharedlib",       // Shared libraries (.so, .dylib)
        "application/vnd.microsoft.portable-executable"  // PE format
    );

    return executableMimeTypes.contains(mimeType.toLowerCase());
  }

  // 파일 메타데이터 저장
  private UploadedFile saveFileMetadata(Long spaceId, MultipartFile file, String extension) {
    String storedName = UUID.randomUUID() + "." + extension;
    String filePath = String.format("/uploads/%d/%s", spaceId, storedName);

    UploadedFile uploadedFile = UploadedFile.builder()
        .spaceId(spaceId)
        .originalName(file.getOriginalFilename())
        .storedName(storedName)
        .extension(extension)
        .fileSize(file.getSize())
        .mimeType(file.getContentType())
        .filePath(filePath)
        .build();

    return uploadedFileRepository.save(uploadedFile);
  }
}

