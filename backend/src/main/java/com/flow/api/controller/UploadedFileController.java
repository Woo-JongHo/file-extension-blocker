package com.flow.api.controller;

import com.flow.api.domain.UploadedFile;
import com.flow.api.domain.data.UploadedFileDto;
import com.flow.api.service.UploadedFileService;
import com.woo.core.controller.BaseController;
import com.woo.core.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/uploaded-files")
public class UploadedFileController extends BaseController<UploadedFile, UploadedFileDto> {

  private final UploadedFileService uploadedFileService;

  public UploadedFileController(UploadedFileService uploadedFileService, ModelMapper modelMapper) {
    super(uploadedFileService, modelMapper);
    this.uploadedFileService = uploadedFileService;
  }
  
  @Override
  protected Class<UploadedFileDto> getDtoClass() { return UploadedFileDto.class; }

  @Override
  protected Class<UploadedFile> getEntityClass() { return UploadedFile.class; }
  
  // ══════════비즈니스 로직═══════════════════
  // 1. POST /upload - 파일 업로드 (차단 확장자 검증 포함)
  // 2. GET /list - 공간의 모든 파일 조회
  // 3. GET /count - 공간의 파일 개수
  // 4. GET /check-extension - 확장자 차단 여부 확인
  // 5. GET /download/{fileId} - 파일 다운로드
  // ══════════════════════════════════════
  
  @PostMapping("/upload")
  public ResponseEntity<BaseResponse<UploadedFileDto>> uploadFile(
      @RequestParam Long spaceId,
      @RequestParam("file") MultipartFile file) {
    try {
      UploadedFile uploadedFile = uploadedFileService.uploadFile(spaceId, file);
      return successResponse(toDto(uploadedFile), "파일 업로드 완료");

    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(BaseResponse.error("FILE_UPLOAD_FAILED", e.getMessage()));
    }
  }

  @GetMapping("/check-extension")
  public ResponseEntity<BaseResponse<Boolean>> checkExtension(
      @RequestParam Long spaceId,
      @RequestParam String extension) {
    boolean isBlocked = uploadedFileService.isExtensionBlocked(spaceId, extension);
    return successResponse(isBlocked, "확장자 차단 여부 확인 완료");
  }

  @GetMapping("/list")
  public ResponseEntity<BaseResponse<List<UploadedFileDto>>> getFiles(@RequestParam Long spaceId) {
    List<UploadedFileDto> files = uploadedFileService.getFilesBySpaceWithUploader(spaceId);
    return successResponse(files, "파일 목록 조회 완료");
  }

  @GetMapping("/count")
  public ResponseEntity<BaseResponse<Long>> countFiles(@RequestParam Long spaceId) {
    Long count = uploadedFileService.countFilesBySpace(spaceId);
    return successResponse(count, "파일 개수 조회 완료");
  }

  /**
   * 업로드된 파일 다운로드
   * 
   * @param fileId 파일 ID
   * @return 파일 다운로드 ResponseEntity
   */
  @GetMapping("/download/{fileId}")
  public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
    log.info("========== 업로드 파일 다운로드 시작 ==========");
    log.info("요청 정보 - FileId: {}", fileId);
    
    try {
      log.info("[1단계] DB에서 파일 정보 조회 시작");
      UploadedFile file = uploadedFileService.getFileById(fileId);
      log.info("[1단계] 파일 정보 조회 완료 - 파일명: {}, 경로: {}", file.getOriginalName(), file.getFilePath());
      
      if (file.getIsDeleted()) {
        log.warn("[실패] 삭제된 파일입니다 - FileId: {}", fileId);
        return ResponseEntity.notFound().build();
      }
      log.info("[2단계] 삭제 여부 확인 완료 - 활성 파일");
      
      Path filePath = Paths.get(file.getFilePath()).normalize();
      log.info("[3단계] 파일 경로 정규화 완료: {}", filePath.toAbsolutePath());
      
      Resource resource = new UrlResource(filePath.toUri());
      log.info("[4단계] UrlResource 생성 완료");
      
      if (!resource.exists()) {
        log.error("[실패] 실제 파일이 존재하지 않음: {}", filePath.toAbsolutePath());
        return ResponseEntity.notFound().build();
      }
      log.info("[5단계] 파일 존재 확인 완료");
      
      if (!resource.isReadable()) {
        log.error("[실패] 파일 읽기 권한 없음: {}", filePath.toAbsolutePath());
        return ResponseEntity.notFound().build();
      }
      log.info("[6단계] 파일 읽기 권한 확인 완료");
      
      String contentType = file.getMimeType();
      if (contentType == null) {
        contentType = "application/octet-stream";
        log.info("[7단계] MIME 타입 기본값 사용: {}", contentType);
      } else {
        log.info("[7단계] MIME 타입 확인: {}", contentType);
      }
      
      log.info("[8단계] 다운로드 응답 생성 - 파일명: {}", file.getOriginalName());
      log.info("========== 업로드 파일 다운로드 성공 ==========");
      
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(contentType))
          .header(HttpHeaders.CONTENT_DISPOSITION, 
              "attachment; filename=\"" + file.getOriginalName() + "\"")
          .body(resource);
          
    } catch (MalformedURLException e) {
      log.error("========== 업로드 파일 다운로드 실패 (잘못된 URL) ==========");
      log.error("에러 메시지: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().build();
    } catch (Exception e) {
      log.error("========== 업로드 파일 다운로드 실패 ==========");
      log.error("에러 메시지: {}", e.getMessage(), e);
      return ResponseEntity.notFound().build();
    }
  }
}

