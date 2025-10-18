package com.flow.api.domain;

import com.woo.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;


@Entity
@Table(name = "space")
@Getter
@Setter
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Space extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long spaceId;

  @Column(length = 255, nullable = false)
  private String spaceName;

  @Column(columnDefinition = "TEXT")
  private String description;
}

