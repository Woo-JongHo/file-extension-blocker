package com.flow.api.controller;

import com.flow.api.domain.Space;
import com.flow.api.domain.data.SpaceCreationRequest;
import com.flow.api.domain.data.SpaceCreationResponse;
import com.flow.api.domain.data.SpaceDto;
import com.flow.api.service.SpaceService;
import com.woo.core.controller.BaseController;
import com.woo.core.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/spaces")
public class SpaceController extends BaseController<Space, SpaceDto> {

  private final SpaceService spaceService;

  public SpaceController(SpaceService spaceService, ModelMapper modelMapper) {
    super(spaceService, modelMapper);
    this.spaceService = spaceService;
  }
  
  @Override
  protected Class<SpaceDto> getDtoClass() { return SpaceDto.class; }

  @Override
  protected Class<Space> getEntityClass() { return Space.class; }
  
  // ══════════비즈니스 로직═══════════════════
  // 1. GET /space-list - 모든 공간 조회
  // 2. POST /create-with-admin - Space + Admin Member + 고정 확장자 동시 생성
  // 3. POST /{spaceId}/top6 - Top-6 고정 확장자 자동 삽입 (레거시)
  // ══════════════════════════════════════
  
  @GetMapping("/space-list")
  public ResponseEntity<BaseResponse<List<SpaceDto>>> getAllSpaces() {
    List<Space> spaces = spaceService.getAllSpaces();
    return successResponse(toDtoList(spaces), "Space 목록 조회 완료");
  }

  /**
   * Space와 Admin Member를 동시에 생성하고 고정 확장자 7개 자동 삽입
   * 
   * <p>트랜잭션으로 안전하게 처리:
   * <ol>
   *   <li>Space 생성</li>
   *   <li>Admin Member 생성</li>
   *   <li>고정 확장자 7개 자동 삽입 (비활성화 상태)</li>
   * </ol>
   */
  @PostMapping("/create-with-admin")
  public ResponseEntity<BaseResponse<SpaceCreationResponse>> createSpaceWithAdmin(
      @RequestBody SpaceCreationRequest request) {
    SpaceCreationResponse response = spaceService.createSpaceWithAdmin(request);
    return successResponse(response, "Space 및 Admin Member 생성 완료");
  }

  @PostMapping("/{spaceId}/top6")
  public ResponseEntity<BaseResponse<Void>> insertTop6Extensions(@PathVariable Long spaceId, @RequestParam Long memberId) {
    spaceService.insertTop6Extensions(spaceId, memberId);
    return successResponse(null, "Top-6 고정 확장자 삽입 완료");
  }
}

