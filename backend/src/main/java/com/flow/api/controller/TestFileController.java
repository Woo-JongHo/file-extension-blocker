package com.flow.api.controller;

import com.woo.core.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 테스트 파일 다운로드 컨트롤러
 *
 * <p>방어 전략 테스트를 위한 샘플 파일들을 제공한다.
 *
 * <p>제공 카테고리:
 * <ul>
 *   <li>1-normal: 정상 파일 (모든 단계 통과)</li>
 *   <li>2-blocked-ext: 차단된 확장자 (1단계 차단)</li>
 *   <li>3-disguised: 확장자 위장 (2단계 차단)</li>
 *   <li>4-archive: 압축 파일 (3단계 테스트)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/test-files")
public class TestFileController {

  private static final String TEST_FILES_BASE = "test-files/";

  @GetMapping("/list")
  public ResponseEntity<BaseResponse<Map<String, List<String>>>> getTestFileList() {
    Map<String, List<String>> fileMap = new HashMap<>();

    fileMap.put("1-normal", List.of(
        "document.txt",
        "data.json"
    ));

    fileMap.put("2-blocked-ext", List.of(
        "virus.bat",
        "script.sh",
        "hack.php"
    ));

    fileMap.put("3-disguised", List.of(
        "fake-image.jpg"
    ));

    fileMap.put("4-archive", List.of(
        "normal.zip",
        "malicious.zip",
        "zipbomb.zip",
        "nested.zip"
    ));

    return BaseResponse.successResponse(fileMap, "테스트 파일 목록 조회 완료");
  }

  /**
   * 테스트 파일 다운로드
   *
   * @param category 카테고리 (1-normal, 2-blocked-ext, 3-disguised, 4-archive)
   * @param filename 파일명
   * @return 파일 다운로드 ResponseEntity
   */
  @GetMapping("/download/{category}/{filename}")
  public ResponseEntity<Resource> downloadTestFile(
      @PathVariable String category,
      @PathVariable String filename) {
    
    try {
      String filePath = TEST_FILES_BASE + category + "/" + filename;
      Resource resource = new ClassPathResource(filePath);
      
      if (!resource.exists()) {
        return ResponseEntity.notFound().build();
      }
      
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .header(HttpHeaders.CONTENT_DISPOSITION, 
              "attachment; filename=\"" + filename + "\"")
          .body(resource);
          
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 카테고리 정보 조회
   *
   * @return 카테고리 설명
   */
  @GetMapping("/categories")
  public ResponseEntity<BaseResponse<Map<String, String>>> getCategories() {
    Map<String, String> categories = new HashMap<>();
    categories.put("1-normal", "정상 파일 (모든 단계 통과)");
    categories.put("2-blocked-ext", "차단된 확장자 (1단계 차단)");
    categories.put("3-disguised", "확장자 위장 (2단계 차단)");
    categories.put("4-archive", "압축 파일 (3단계 테스트)");

    return BaseResponse.successResponse(categories, "카테고리 목록 조회 완료");
  }
}

