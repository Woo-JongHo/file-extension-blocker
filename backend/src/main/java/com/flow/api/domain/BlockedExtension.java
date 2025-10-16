package com.flow.api.domain;

import com.woo.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 공간별 차단 확장자 정책 (고정 + 커스텀)
 * - 고정 확장자 (is_fixed = true): Top-6, 체크/언체크만 가능, 삭제 불가
 * - 커스텀 확장자 (is_fixed = false): 사용자 추가, 최대 200개
 */
@Entity
@Table(
  name = "blocked_extension",
  uniqueConstraints = @UniqueConstraint(name = "uq_space_extension", columnNames = {"space_id", "extension"})
)
@Getter
@Setter
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BlockedExtension extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long blockedId;

  @Column(nullable = false)
  private Long spaceId;

  @Column(length = 20, nullable = false)
  private String extension;

  @Column(nullable = false)
  private Boolean isFixed;
}

