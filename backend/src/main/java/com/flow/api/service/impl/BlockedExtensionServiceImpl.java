package com.flow.api.service.impl;

import com.flow.api.domain.BlockedExtension;
import com.flow.api.repository.BlockedExtensionRepository;
import com.flow.api.service.BlockedExtensionService;
import com.woo.core.service.BaseServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional
public class BlockedExtensionServiceImpl extends BaseServiceImpl<BlockedExtension> implements BlockedExtensionService {

  private final BlockedExtensionRepository blockedExtensionRepository;

  // 영문자, 숫자, 하이픈(-), 마침표(.), 플러스(+) 허용
  private static final Pattern VALID_EXTENSION_PATTERN = Pattern.compile("^[a-zA-Z0-9.+\\-]+$");
  private static final int MAX_EXTENSION_LENGTH = 20;

  public BlockedExtensionServiceImpl(BlockedExtensionRepository blockedExtensionRepository) {
    super(blockedExtensionRepository);
    this.blockedExtensionRepository = blockedExtensionRepository;
  }

  @Override
  public BlockedExtension create(BlockedExtension entity) {
    validateExtension(entity.getExtension());
    
    // 중복 확인 (삭제 여부 무관하게 확인)
    String normalizedExtension = entity.getExtension().toLowerCase().trim();
    return blockedExtensionRepository.findBySpaceIdAndExtension(entity.getSpaceId(), normalizedExtension)
        .map(existing -> {
          if (existing.getIsFixed()) {
            // 고정 확장자와 중복 → 에러
            throw new IllegalArgumentException(
                String.format("'%s' 는 이미 고정 확장자로 등록되어 있습니다. 고정 확장자는 체크박스로 활성화/비활성화할 수 있습니다.", 
                    normalizedExtension));
          } else {
            // 커스텀 확장자와 중복
            if (existing.getIsDeleted()) {
              // 삭제된 커스텀 확장자 → isDeleted를 false로 UPDATE (재활성화)
              existing.setIsDeleted(false);
              existing.setUpdatedBy(entity.getCreatedBy()); // 수정자 업데이트
              return blockedExtensionRepository.save(existing);
            } else {
              // 이미 활성화된 커스텀 확장자 → 에러
              throw new IllegalArgumentException(
                  String.format("'%s' 는 이미 추가된 확장자입니다.", normalizedExtension));
            }
          }
        })
        .orElseGet(() -> {
          // 존재하지 않음 → 새로 CREATE
          return super.create(entity);
        });
  }

  @Override
  @Transactional(readOnly = true)
  public List<BlockedExtension> getBlockedExtensions(Long spaceId) {
    return blockedExtensionRepository.findBySpaceIdAndIsDeletedFalse(spaceId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BlockedExtension> getFixedExtensions(Long spaceId) {
    // 고정 확장자는 삭제 여부와 관계없이 모두 표시 (확장자명 알파벳 순으로 정렬)
    return blockedExtensionRepository.findBySpaceIdAndIsFixedOrderByExtensionAsc(spaceId, true);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BlockedExtension> getCustomExtensions(Long spaceId) {
    // 커스텀 확장자도 확장자명 알파벳 순으로 정렬
    return blockedExtensionRepository.findBySpaceIdAndIsFixedAndIsDeletedFalseOrderByExtensionAsc(spaceId, false);
  }

  @Override
  public void toggleFixedExtension(Long spaceId, String extension) {
    BlockedExtension blockedExtension = blockedExtensionRepository
        .findBySpaceIdAndExtension(spaceId, extension.toLowerCase())
        .orElseThrow(() -> new IllegalArgumentException("고정 확장자를 찾을 수 없습니다: " + extension));
    
    // 현재 상태 반전 (isDeleted 토글)
    blockedExtension.setIsDeleted(!blockedExtension.getIsDeleted());
    blockedExtensionRepository.save(blockedExtension);
  }

  @Override
  @Transactional(readOnly = true)
  public Long countCustomExtensions(Long spaceId) {
    return blockedExtensionRepository.countBySpaceIdAndIsFixedAndIsDeletedFalse(spaceId, false);
  }

  @Override
  @Transactional(readOnly = true)
  public Long countActiveExtensions(Long spaceId) {
    // 고정 + 커스텀 확장자 중 활성화된 것 (is_deleted = false)
    List<BlockedExtension> allExtensions = blockedExtensionRepository.findBySpaceIdAndIsDeletedFalse(spaceId);
    return (long) allExtensions.size();
  }

  /**
   * 확장자 유효성 검증
   *
   * <p>검증 규칙:
   * <ul>
   *   <li>영문자, 숫자, 일부 특수문자 허용 (a-z, A-Z, 0-9, -, ., +)</li>
   *   <li>한글 및 기타 특수문자 차단</li>
   *   <li>최대 20자</li>
   *   <li>빈 문자열 불가</li>
   * </ul>
   *
   * @param extension 검증할 확장자
   * @throws IllegalArgumentException 유효하지 않은 확장자인 경우
   */
  private void validateExtension(String extension) {
    if (extension == null || extension.trim().isEmpty()) {
      throw new IllegalArgumentException("확장자는 필수입니다.");
    }

    String trimmed = extension.trim();

    if (trimmed.length() > MAX_EXTENSION_LENGTH) {
      throw new IllegalArgumentException(
          String.format("확장자는 최대 %d자까지 가능합니다.", MAX_EXTENSION_LENGTH));
    }

    if (!VALID_EXTENSION_PATTERN.matcher(trimmed).matches()) {
      throw new IllegalArgumentException(
          "확장자는 영문자, 숫자, 일부 특수문자(-, ., +)만 사용 가능합니다.");
    }
  }
}

