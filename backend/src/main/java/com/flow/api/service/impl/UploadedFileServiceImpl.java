package com.flow.api.service.impl;

import com.flow.api.domain.BlockedExtension;
import com.flow.api.domain.UploadedFile;
import com.flow.api.repository.UploadedFileRepository;
import com.flow.api.service.BlockedExtensionService;
import com.flow.api.service.UploadedFileService;
import com.woo.core.service.BaseServiceImpl;
import com.flow.util.fileDefence.FileValidator;
import com.flow.util.fileDefence.ZipValidator;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
   *   <li>1단계: 확장자 Blacklist 검증 ({@link FileValidator})</li>
   *   <li>2단계: Apache Tika 매직 넘버 검증 ({@link FileValidator})</li>
   *   <li>3단계: 압축 파일 내부 검증 ({@link ZipValidator})</li>
   *   <li>4단계: Polyglot 공격 방어 (서버 설정 - 인프라 설정 필요)</li>
   * </ul>
   *
   * <p>현재 적용 단계:
   * <ul>
   *   <li>1단계: 확장자 Blacklist로 텍스트 스크립트(.bat, .cmd, .sh 등) 차단</li>
   *   <li>2단계: 매직 넘버로 바이너리 실행 파일(.exe, .elf) 차단</li>
   *   <li>3단계: 압축 파일 내부 재귀 검증 + Zip Bomb 차단</li>
   * </ul>
   */
  @Override
  public UploadedFile uploadFile(Long spaceId, MultipartFile file) {
    // 차단 확장자 목록 조회
    List<BlockedExtension> blockedList = blockedExtensionService.getBlockedExtensions(spaceId);
    Set<String> blockedExtensions = convertToExtensionSet(blockedList);

    // 1,2단계 방어: FileValidator
    FileValidator fileValidator = new FileValidator(blockedExtensions, tika, maxFileSize);
    String extension = fileValidator.validateFile(file);

    // 3단계 방어: ZipValidator (압축 파일만)
    ZipValidator zipValidator = new ZipValidator(blockedExtensions, tika);
    zipValidator.validateZipFile(file, 0);

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
  // Private 헬퍼 메서드들
  // ═══════════════════════════════════════════════════════════

  /**
   * BlockedExtension List를 확장자 Set으로 변환
   *
   * @param blockedList 차단 확장자 목록
   * @return 확장자 Set (소문자)
   */
  private Set<String> convertToExtensionSet(List<BlockedExtension> blockedList) {
    Set<String> extensionSet = new HashSet<>();
    for (BlockedExtension blocked : blockedList) {
      extensionSet.add(blocked.getExtension().toLowerCase());
    }
    return extensionSet;
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

