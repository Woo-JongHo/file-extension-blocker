package com.flow.api.domain;

import com.woo.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;


@Entity
@Table(name = "member")
@Getter
@Setter
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Member extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long memberId;

  @Column(length = 100, nullable = false, unique = true)
  private String username;

  @Column(length = 255, nullable = false)
  private String password;

  private Long spaceId;

  @Enumerated(EnumType.STRING)
  @Column(length = 50, nullable = false)
  @Builder.Default
  private MemberRole role = MemberRole.MEMBER;

  /**
   * 멤버 권한
   */
  public enum MemberRole {
    ADMIN,   // 확장자 관리 권한 + 파일 업로드
    MEMBER   // 파일 업로드만 가능
  }
}

