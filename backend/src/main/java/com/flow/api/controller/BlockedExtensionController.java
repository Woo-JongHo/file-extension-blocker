package com.flow.api.controller;

import com.flow.api.domain.BlockedExtension;
import com.flow.api.domain.data.BlockedExtensionDto;
import com.flow.api.service.BlockedExtensionService;
import com.woo.core.controller.BaseController;
import com.woo.core.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/spaces/{spaceId}/blocked-extensions")
public class BlockedExtensionController extends BaseController<BlockedExtension, BlockedExtensionDto> {

  private final BlockedExtensionService blockedExtensionService;

  public BlockedExtensionController(BlockedExtensionService blockedExtensionService, ModelMapper modelMapper) {
    super(blockedExtensionService, modelMapper);
    this.blockedExtensionService = blockedExtensionService;
  }
  
  @Override
  protected Class<BlockedExtensionDto> getDtoClass() { return BlockedExtensionDto.class; }

  @Override
  protected Class<BlockedExtension> getEntityClass() { return BlockedExtension.class; }
  
  // ══════════════════════════════════════
  // ========== 비즈니스 로직 ==========
  // 1. GET /block-list - 모든 차단 확장자 조회
  // 2. GET /fixed-block-list - 고정 확장자 조회
  // 3. GET /custom-block-list - 커스텀 확장자 조회
  // 4. PATCH /fixed-change-status - 고정 확장자 상태 변경 (체크박스)
  // 5. GET /count-custom-block-list - 커스텀 확장자 개수
  // ══════════════════════════════════════
  
  @GetMapping("/block-list")
  public ResponseEntity<BaseResponse<List<BlockedExtensionDto>>> getBlockExtensionsBySpace(@PathVariable Long spaceId) {
    List<BlockedExtension> extensions = blockedExtensionService.getBlockedExtensions(spaceId);
    return successResponse(toDtoList(extensions));
  }

  @GetMapping("/fixed-block-list")
  public ResponseEntity<BaseResponse<List<BlockedExtensionDto>>> getFixedBlockExtensions(@PathVariable Long spaceId) {
    List<BlockedExtension> extensions = blockedExtensionService.getFixedExtensions(spaceId);
    return successResponse(toDtoList(extensions));
  }

  @GetMapping("/custom-block-list")
  public ResponseEntity<BaseResponse<List<BlockedExtensionDto>>> getCustomBlockExtensions(@PathVariable Long spaceId) {
    List<BlockedExtension> extensions = blockedExtensionService.getCustomExtensions(spaceId);
    return successResponse(toDtoList(extensions));
  }

  @PatchMapping("/fixed-change-status")
  public ResponseEntity<BaseResponse<String>> changeFixedExtensionStatus(
      @PathVariable Long spaceId,
      @RequestParam String extension,
      @RequestParam Boolean isBlocked) {
    blockedExtensionService.toggleFixedExtension(spaceId, extension, isBlocked);
    return successResponse("고정 확장자 상태 변경 완료");
  }

  @GetMapping("/count-custom-block-list")
  public ResponseEntity<BaseResponse<Long>> countCustomBlockExtensions(@PathVariable Long spaceId) {
    Long count = blockedExtensionService.countCustomExtensions(spaceId);
    return successResponse(count);
  }
}

