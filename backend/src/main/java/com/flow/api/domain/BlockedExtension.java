package com.flow.api.domain;

import com.woo.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;


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

