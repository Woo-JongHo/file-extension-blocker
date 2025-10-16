package com.flow.api.controller;

import com.flow.api.domain.Space;
import com.flow.api.domain.data.SpaceDto;
import com.flow.api.service.SpaceService;
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

  // ========== 비즈니스 로직 ==========
  
  @GetMapping
  public ResponseEntity<BaseResponse<List<SpaceDto>>> getAllSpaces() {
    List<Space> spaces = spaceService.getAllSpaces();
    List<SpaceDto> dtos = spaces.stream().map(this::toDto).collect(Collectors.toList());
    return successResponse(dtos);
  }
}

