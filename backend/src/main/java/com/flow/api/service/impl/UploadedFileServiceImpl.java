package com.flow.api.service.impl;

import com.flow.api.domain.BlockedExtension;
import com.flow.api.domain.UploadedFile;
import com.flow.api.repository.UploadedFileRepository;
import com.flow.api.service.BlockedExtensionService;
import com.flow.api.service.UploadedFileService;
import com.woo.core.service.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class UploadedFileServiceImpl extends BaseServiceImpl<UploadedFile> implements UploadedFileService {

  private final UploadedFileRepository uploadedFileRepository;
  private final BlockedExtensionService blockedExtensionService;

  @Value("${file.upload.max-size:10485760}")
  private long maxFileSize;

  public UploadedFileServiceImpl(
      UploadedFileRepository uploadedFileRepository,
      BlockedExtensionService blockedExtensionService) {
    super(uploadedFileRepository);
    this.uploadedFileRepository = uploadedFileRepository;
    this.blockedExtensionService = blockedExtensionService;
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
   *   <li>1단계: 확장자 Blacklist 검증 {@link #validate1stDefense} ✅ 구현 완료</li>
   *   <li>2단계: Apache Tika 매직 넘버 검증 ⏳ 미구현</li>
   *   <li>3단계: Polyglot 공격 방어 (서버 설정) ⏳ 미구현</li>
   *   <li>4단계: 압축 파일 내부 검증 ⏳ 미구현</li>
   * </ul>
   */
  @Override
  public UploadedFile uploadFile(Long spaceId, MultipartFile file) {
    String extension = validate1stDefense(spaceId, file);
    // String extension = validate2ndDefense(file);
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
   * 1단계: 기본 검증 + 확장자 Blacklist 검증 (상세: docs/strategy.md)
   * 
   * <p>파일 업로드 시 1차 방어 전략을 수행한다.
   * 
   * <p>검증 순서:
   * <ol>
   *   <li>파일 null/empty 검증</li>
   *   <li>파일명 존재 여부 검증</li>
   *   <li>확장자 추출 (마지막 점 이후, 소문자 변환)</li>
   *   <li>확장자 Blacklist 검증 (DB 조회)</li>
   *   <li>파일 크기 검증</li>
   * </ol>
   * 
   * <p>차단 대상:
   * <ul>
   *   <li>고정 확장자 6개: Top-6 악성 확장자</li>
   *   <li>커스텀 확장자: 최대 200개</li>
   *   <li>확장자 없는 파일</li>
   * </ul>
   *
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

  // 파일 메타데이터 저장
  private UploadedFile saveFileMetadata(Long spaceId, MultipartFile file, String extension) {
    String storedName = UUID.randomUUID().toString() + "." + extension;
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

