package com.woo.core.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * API 응답의 표준 형식을 제공하는 제네릭 래퍼 클래스
 *
 * <p>모든 REST API 응답을 통일된 형식으로 반환하여 클라이언트의 응답 처리를 단순화한다.
 * 성공/실패 여부와 관계없이 일관된 JSON 구조를 제공하며, 제네릭을 활용하여 다양한 데이터 타입을 지원한다.
 *
 * <p>응답 필드 구성:
 * <ul>
 *   <li>{@code timestamp} - 응답 생성 시각 (yyyy-MM-dd HH:mm:ss 형식)</li>
 *   <li>{@code status} - HTTP 상태 코드 (200, 400, 404, 500 등)</li>
 *   <li>{@code data} - 성공 시 반환되는 실제 데이터 (제네릭 타입 T)</li>
 *   <li>{@code errorCode} - 에러 발생 시 에러 코드 (예: "VALIDATION_ERROR")</li>
 *   <li>{@code errorDetail} - 에러 발생 시 상세 메시지</li>
 * </ul>
 *
 * <p>주요 정적 팩토리 메서드:
 * <ul>
 *   <li>{@link #successResponse(Object)} - 성공 응답 생성 (HTTP 200)</li>
 *   <li>{@link #errorResponse(String, String)} - 에러 응답 생성 (HTTP 400)</li>
 *   <li>{@link #errorResponse(int, String, String)} - 커스텀 상태 코드로 에러 응답 생성</li>
 *   <li>{@link #pdf(byte[], String)} - PDF 파일 다운로드 응답 생성</li>
 * </ul>
 *
 
 * @param <T> 응답 데이터의 타입 (DTO, List, String 등)
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "data")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {
  private String timestamp;
  private int status;
  private String message;
  private T data;
  private String errorCode;
  private String errorDetail;

  public static String now() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  // BaseResponse 직접 반환
  public static <T> BaseResponse<T> success(T data) {
    return BaseResponse.<T>builder()
        .timestamp(now())
        .status(200)
        .data(data)
        .build();
  }

  public static <T> BaseResponse<T> success(T data, String message) {
    return BaseResponse.<T>builder()
        .timestamp(now())
        .status(200)
        .message(message)
        .data(data)
        .build();
  }

  public static <T> BaseResponse<T> error(String errorCode, String errorDetail) {
    return BaseResponse.<T>builder()
        .timestamp(now())
        .status(400)
        .message("요청 처리 중 오류가 발생했습니다.")
        .errorCode(errorCode)
        .errorDetail(errorDetail)
        .build();
  }

  public static <T> BaseResponse<T> error(String errorCode, String errorDetail, String message) {
    return BaseResponse.<T>builder()
        .timestamp(now())
        .status(400)
        .message(message)
        .errorCode(errorCode)
        .errorDetail(errorDetail)
        .build();
  }

  public static <T> BaseResponse<T> error(int status, String errorCode, String errorDetail) {
    return BaseResponse.<T>builder()
        .timestamp(now())
        .status(status)
        .message("요청 처리 중 오류가 발생했습니다.")
        .errorCode(errorCode)
        .errorDetail(errorDetail)
        .build();
  }

  public static <T> BaseResponse<T> error(int status, String errorCode, String errorDetail, String message) {
    return BaseResponse.<T>builder()
        .timestamp(now())
        .status(status)
        .message(message)
        .errorCode(errorCode)
        .errorDetail(errorDetail)
        .build();
  }

  // ResponseEntity 반환 (하위 호환성)
  public static <T> ResponseEntity<BaseResponse<T>> successResponse(T data) {
    return ResponseEntity.ok(success(data));
  }

  public static <T> ResponseEntity<BaseResponse<T>> successResponse(T data, String message) {
    return ResponseEntity.ok(success(data, message));
  }

  public static <T> ResponseEntity<BaseResponse<T>> errorResponse(
      String errorCode, String errorDetail) {
    return ResponseEntity.badRequest().body(error(errorCode, errorDetail));
  }

  public static <T> ResponseEntity<BaseResponse<T>> errorResponse(
      int status, String errorCode, String errorDetail) {
    return ResponseEntity.status(status).body(error(status, errorCode, errorDetail));
  }

  public static ResponseEntity<byte[]> pdf(byte[] pdfBytes, String fileName) {
    // Remove duplicate .pdf extension if it already exists
    String cleanFileName = fileName.toLowerCase().endsWith(".pdf") ? fileName : fileName + ".pdf";

    // Create Content-Disposition header with proper UTF-8 encoding
    String contentDisposition = createContentDispositionHeader(cleanFileName);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
        .contentLength(pdfBytes.length)
        .body(pdfBytes);
  }

  private static String createContentDispositionHeader(String fileName) {
    try {
      String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());

      encodedFileName = encodedFileName.replace("+", "%20");

      return String.format(
          "attachment; filename=\"%s\"; filename*=UTF-8''%s",
          fileName.replaceAll("[^\\x00-\\x7F]", "?"), // ASCII fallback
          encodedFileName);
    } catch (UnsupportedEncodingException e) {
      return "attachment; filename=\"" + fileName.replaceAll("[^\\x00-\\x7F]", "?") + "\"";
    }
  }
}

