package com.flow.core.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class loggingInterceptor implements HandlerInterceptor {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final String START_TIME_ATTR = "startTime";

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    long startTime = System.currentTimeMillis();
    request.setAttribute(START_TIME_ATTR, startTime);
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    long endTime = System.currentTimeMillis();
    long startTime = (Long) request.getAttribute(START_TIME_ATTR);
    long duration = endTime - startTime;

    String time = LocalDateTime.now().format(FORMATTER);
    String uri = request.getRequestURI();
    String method = request.getMethod();
    String query = request.getQueryString() != null ? "?" + request.getQueryString() : "";
    int status = response.getStatus();
    String userAgent = request.getHeader("User-Agent");

    // 핸들러 정보 추출 (컨트롤러, 메서드)
    String controllerInfo = "";
    if (handler instanceof HandlerMethod handlerMethod) {
      String controllerName = handlerMethod.getBeanType().getSimpleName(); // 예: AuthController
      String methodName = handlerMethod.getMethod().getName(); // 예: login
      controllerInfo = controllerName + "#" + methodName;
    } else {
      controllerInfo = handler.toString(); // fallback
    }

    System.out.println(
        "["
            + time
            + "] [API 호출] "
            + method
            + " "
            + uri
            + query
            + " (처리시간: "
            + duration
            + "ms"
            + ", 상태코드: "
            + status
            + ", 컨트롤러: "
            + controllerInfo
            + ", User-Agent: "
            + userAgent
            + ")");

    if (ex != null) {
      System.err.println(
          "[" + time + "] [예외 발생] " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
      ex.printStackTrace();
    }
  }
}
