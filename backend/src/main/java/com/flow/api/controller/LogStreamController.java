package com.flow.api.controller;

import com.woo.core.response.BaseResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 로그 조회 컨트롤러
 *
 * <p>application.log 파일 조회 및 실시간 스트리밍(SSE) 기능 제공
 */
@RestController
@RequestMapping("/api/logs")
public class LogStreamController {

  private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamLogs() {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    emitters.add(emitter);

    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(e -> emitters.remove(emitter));

    // 초기 연결 메시지
    try {
      emitter.send(SseEmitter.event()
          .name("connected")
          .data("Connected to log stream at " + LocalDateTime.now()));
    } catch (IOException e) {
      emitter.complete();
    }

    return emitter;
  }


  /**
   * 로그 파일 조회 (최근 N줄)
   * 
   * @param lines 조회할 라인 수 (기본 100줄)
   * @return 로그 파일 내용
   */
  @GetMapping("/file")
  public ResponseEntity<BaseResponse<String>> getLogFile(@RequestParam(defaultValue = "100") int lines) {
    try {
      Path logPath = Paths.get("./logs/app.log");
      if (!Files.exists(logPath)) {
        return BaseResponse.successResponse("로그 파일이 아직 생성되지 않았습니다.", "로그 조회 완료");
      }

      // 전체 라인 수 계산
      long totalLines = Files.lines(logPath).count();
      long skipLines = Math.max(0, totalLines - lines);
      
      // 최근 N줄 읽기
      String logContent = Files.lines(logPath)
          .skip(skipLines)
          .collect(Collectors.joining("\n"));

      return BaseResponse.successResponse(logContent, 
          String.format("로그 조회 완료 (총 %d줄 중 최근 %d줄)", totalLines, Math.min(lines, totalLines)));

    } catch (IOException e) {
      return BaseResponse.errorResponse("로그 파일 읽기 실패: " + e.getMessage(), "LOG_READ_FAILED");
    }
  }

  /**
   * 외부에서 로그 이벤트를 브로드캐스트
   */
  public void broadcastLog(String message) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    String logMessage = String.format("[%s] %s", timestamp, message);
    
    emitters.forEach(emitter -> {
      try {
        emitter.send(SseEmitter.event()
            .name("log")
            .data(logMessage));
      } catch (IOException e) {
        emitter.complete();
        emitters.remove(emitter);
      }
    });
  }
}

