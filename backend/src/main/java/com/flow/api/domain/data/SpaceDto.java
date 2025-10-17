package com.flow.api.domain.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.woo.core.util.common.Identifiable;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Space DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceDto implements Identifiable {
  
  private Long spaceId;
  private String spaceName;
  private String description;
  private String adminUsername;
  private String adminPassword;
  private Long createdBy;
  private Long updatedBy;
  private LocalDateTime createdAt;

  @Override
  @JsonIgnore
  public Long getId() {
    return spaceId;
  }

  @Override
  @JsonIgnore
  public void setId(Long id) {
    this.spaceId = id;
  }
}

