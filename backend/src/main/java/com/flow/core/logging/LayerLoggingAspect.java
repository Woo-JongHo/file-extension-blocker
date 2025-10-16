package com.flow.core.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Profile("dev") // 개발환경에서만 활성화
public class LayerLoggingAspect {

  @Around("execution(* com.dev.millionhands.api..controller..*(..))")
  public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
    return logExecution(joinPoint, "Controller");
  }

  @Around("execution(* com.dev.millionhands.api..service..*(..))")
  public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
    return logExecution(joinPoint, "Service");
  }

  @Around("execution(* com.dev.millionhands.api..repository..*(..))")
  public Object logRepository(ProceedingJoinPoint joinPoint) throws Throwable {
    return logExecution(joinPoint, "Repository");
  }

  private Object logExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
    long start = System.currentTimeMillis();

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String className = signature.getDeclaringType().getSimpleName();
    String methodName = signature.getName();

    System.out.println("➡️ [" + layer + "] " + className + "#" + methodName + " 시작");

    Object result;
    try {
      result = joinPoint.proceed(); // 실제 메서드 실행
    } catch (Throwable e) {
      System.err.println(
          "❌ [" + layer + "] " + className + "#" + methodName + " 예외 발생: " + e.getMessage());
      throw e;
    }

    long end = System.currentTimeMillis();
    System.out.println(
        "⬅️ [" + layer + "] " + className + "#" + methodName + " 완료 (" + (end - start) + "ms)");
    return result;
  }
}
