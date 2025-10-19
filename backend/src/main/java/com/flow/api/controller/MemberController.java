package com.flow.api.controller;

import com.flow.api.domain.Member;
import com.flow.api.domain.data.MemberDto;
import com.flow.api.service.MemberService;
import com.woo.core.controller.BaseController;
import com.woo.core.response.BaseResponse;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
  
  // ══════════비즈니스 로직═══════════════════
  // 1. GET /member-list?spaceId={spaceId} - 공간의 회원 목록 조회 (spaceId 필수)
  // ══════════════════════════════════════
  
  @GetMapping("/member-list")
  public ResponseEntity<BaseResponse<List<MemberDto>>> getMembersBySpace(
      @RequestParam Long spaceId) {
    List<Member> members = memberService.getMembersBySpace(spaceId);
    return successResponse(toDtoList(members), "멤버 목록 조회 완료");
  }
}

