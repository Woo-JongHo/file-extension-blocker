package com.flow.api.service.impl;

import com.flow.api.domain.BlockedExtension;
import com.flow.api.domain.Member;
import com.flow.api.domain.Space;
import com.flow.api.domain.data.MemberDto;
import com.flow.api.domain.data.SpaceCreationRequest;
import com.flow.api.domain.data.SpaceCreationResponse;
import com.flow.api.domain.data.SpaceDto;
import com.flow.api.repository.BlockedExtensionRepository;
import com.flow.api.repository.MemberRepository;
import com.flow.api.repository.SpaceRepository;
import com.flow.api.service.SpaceService;
import com.woo.core.service.BaseServiceImpl;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SpaceServiceImpl extends BaseServiceImpl<Space> implements SpaceService {

  private final SpaceRepository spaceRepository;
  private final BlockedExtensionRepository blockedExtensionRepository;
  private final MemberRepository memberRepository;
  private final ModelMapper modelMapper;

  // 고정 확장자 7개 (알파벳 순)
  private static final List<String> FIXED_EXTENSIONS = Arrays.asList(
      "bat", "cmd", "com", "cpl", "exe", "js", "scr"
  );

  public SpaceServiceImpl(
      SpaceRepository spaceRepository, 
      BlockedExtensionRepository blockedExtensionRepository,
      MemberRepository memberRepository,
      ModelMapper modelMapper) {
    super(spaceRepository);
    this.spaceRepository = spaceRepository;
    this.blockedExtensionRepository = blockedExtensionRepository;
    this.memberRepository = memberRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Space> getAllSpaces() {
    return spaceRepository.findByIsDeletedFalse();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsBySpaceName(String spaceName) {
    return spaceRepository.existsBySpaceNameAndIsDeletedFalse(spaceName);
  }

  @Override
  public void insertTop6Extensions(Long spaceId, Long memberId) {
    // 전역 커스텀 확장자에서 Top-6 조회
    List<Object[]> top6 = blockedExtensionRepository.findTop6CustomExtensions();
    
    // Top-6 고정 확장자 삽입 (기본 unCheck = is_deleted true)
    List<BlockedExtension> extensions = top6.stream()
        .map(row -> {
          String extension = (String) row[0];
          BlockedExtension be = new BlockedExtension();
          be.setSpaceId(spaceId);
          be.setExtension(extension);
          be.setIsFixed(true);
          be.setCreatedBy(memberId);
          be.setUpdatedBy(memberId);
          be.setIsDeleted(true); // 기본 unCheck
          return be;
        })
        .collect(Collectors.toList());
    
    blockedExtensionRepository.saveAll(extensions);
  }

  @Override
  public SpaceCreationResponse createSpaceWithAdmin(SpaceCreationRequest request) {
    // 1. 유효성 검증
    if (existsBySpaceName(request.getSpaceName())) {
      throw new IllegalArgumentException("이미 존재하는 Space 이름입니다: " + request.getSpaceName());
    }
    
    if (memberRepository.existsByUsernameAndIsDeletedFalse(request.getAdminUsername())) {
      throw new IllegalArgumentException("이미 존재하는 사용자 이름입니다: " + request.getAdminUsername());
    }
    
    // 2. Space 생성 (createdBy, updatedBy는 나중에 업데이트)
    Space createdSpace = Space.builder()
        .spaceName(request.getSpaceName())
        .description(request.getDescription())
        .isDeleted(false)
        .build();
    createdSpace = spaceRepository.save(createdSpace);
    
    // 3. Admin Member 생성
    Member createdAdmin = Member.builder()
        .username(request.getAdminUsername())
        .password(request.getAdminPassword())
        .spaceId(createdSpace.getSpaceId())
        .role(Member.MemberRole.ADMIN)
        .isDeleted(false)
        .build();
    createdAdmin = memberRepository.save(createdAdmin);
    
    // 4. Space의 createdBy, updatedBy 업데이트
    createdSpace.setCreatedBy(createdAdmin.getMemberId());
    createdSpace.setUpdatedBy(createdAdmin.getMemberId());
    final Space finalSpace = spaceRepository.save(createdSpace);
    
    // 5. 고정 확장자 7개 자동 삽입 (기본 비활성화 상태)
    final Member finalAdmin = createdAdmin;
    List<BlockedExtension> fixedExtensions = FIXED_EXTENSIONS.stream()
        .map(ext -> BlockedExtension.builder()
            .spaceId(finalSpace.getSpaceId())
            .extension(ext)
            .isFixed(true)
            .createdBy(finalAdmin.getMemberId())
            .updatedBy(finalAdmin.getMemberId())
            .isDeleted(true)  // 기본 비활성화 (체크 해제 상태)
            .build())
        .collect(Collectors.toList());
    blockedExtensionRepository.saveAll(fixedExtensions);
    
    // 6. 응답 생성
    return SpaceCreationResponse.builder()
        .space(modelMapper.map(finalSpace, SpaceDto.class))
        .adminMember(modelMapper.map(finalAdmin, MemberDto.class))
        .fixedExtensionsCount(fixedExtensions.size())
        .build();
  }
}

