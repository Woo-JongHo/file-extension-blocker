package com.flow.api.controller;

import com.flow.api.domain.Member;
import com.flow.api.domain.data.MemberDto;
import com.flow.api.service.MemberService;
import com.woo.core.controller.BaseController;
import com.woo.core.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/members")
public class MemberController extends BaseController<Member, MemberDto> {

  private final MemberService memberService;

  public MemberController(MemberService memberService, ModelMapper modelMapper) {
    super(memberService, modelMapper);
    this.memberService = memberService;
  }

  @Override
  protected Class<MemberDto> getDtoClass() { return MemberDto.class; }

  @Override
  protected Class<Member> getEntityClass() { return Member.class; }

  // ========== 비즈니스 로직 ==========
  
  @GetMapping("/check-username/{username}")
  public ResponseEntity<BaseResponse<Boolean>> checkUsername(@PathVariable String username) {
    boolean exists = memberService.existsByUsername(username);
    return successResponse(exists);
  }
}

