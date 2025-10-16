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
import java.util.stream.Collectors;

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

  // ========== 비즈니스 로직 ==========
  
  @GetMapping
  public ResponseEntity<BaseResponse<List<BlockedExtensionDto>>> getBlockedExtensions(@PathVariable Long spaceId) {
    List<BlockedExtension> extensions = blockedExtensionService.getBlockedExtensions(spaceId);
    List<BlockedExtensionDto> dtos = extensions.stream().map(this::toDto).collect(Collectors.toList());
    return successResponse(dtos);
  }

  @GetMapping("/fixed")
  public ResponseEntity<BaseResponse<List<BlockedExtensionDto>>> getFixedExtensions(@PathVariable Long spaceId) {
    List<BlockedExtension> extensions = blockedExtensionService.getFixedExtensions(spaceId);
    List<BlockedExtensionDto> dtos = extensions.stream().map(this::toDto).collect(Collectors.toList());
    return successResponse(dtos);
  }

  @GetMapping("/custom")
  public ResponseEntity<BaseResponse<List<BlockedExtensionDto>>> getCustomExtensions(@PathVariable Long spaceId) {
    List<BlockedExtension> extensions = blockedExtensionService.getCustomExtensions(spaceId);
    List<BlockedExtensionDto> dtos = extensions.stream().map(this::toDto).collect(Collectors.toList());
    return successResponse(dtos);
  }

  @PatchMapping("/toggle")
  public ResponseEntity<BaseResponse<String>> toggleFixedExtension(
      @PathVariable Long spaceId,
      @RequestParam String extension,
      @RequestParam Boolean isBlocked) {
    blockedExtensionService.toggleFixedExtension(spaceId, extension, isBlocked);
    return successResponse("고정 확장자 토글 완료");
  }

  @GetMapping("/custom/count")
  public ResponseEntity<BaseResponse<Long>> countCustomExtensions(@PathVariable Long spaceId) {
    Long count = blockedExtensionService.countCustomExtensions(spaceId);
    return successResponse(count);
  }
}

