package com.flow.api.domain.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.woo.core.util.common.Identifiable;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedFileDto implements Identifiable {
  
  private Long fileId;
  private Long spaceId;
  private String originalName;
  private String storedName;
  private String extension;
  private Long fileSize;
  private String mimeType;
  private String filePath;
  private LocalDateTime createdAt;
  private String uploaderName;

  @Override
  @JsonIgnore
  public Long getId() { return fileId; }

  @Override
  @JsonIgnore
  public void setId(Long id) { this.fileId = id; }
}

