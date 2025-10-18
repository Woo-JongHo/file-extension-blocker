package com.flow.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 실시간 로그 스트리밍 컨트롤러 (SSE)
 *
 * <p>application.log 파일을 실시간으로 스트리밍한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
public class LogStreamController {

  private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

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


  @GetMapping("/file")
  public String getLogFile(@RequestParam(defaultValue = "100") int lines) {
    try {
      Path logPath = Paths.get("./logs/app.log");
      if (!Files.exists(logPath)) {
        return "로그 파일이 아직 생성되지 않았습니다.";
      }

      return Files.lines(logPath)
          .skip(Math.max(0, Files.lines(logPath).count() - lines))
          .reduce("", (a, b) -> a + b + "\n");

    } catch (IOException e) {
      log.error("로그 파일 읽기 실패", e);
      return "로그 파일 읽기 실패: " + e.getMessage();
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

