package com.example.screen;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Логирует каждый вызов метода любого @RestController: кто, откуда, с какими аргументами,
 * чем закончилось и сколько это заняло. Пишет в логгер "controller-calls" (см. logback-spring.xml).
 */
@Aspect
@Component
public class ControllerLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger("controller-calls");

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logCall(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String method = sig.getDeclaringType().getSimpleName() + "." + sig.getName();
        String args = Arrays.stream(pjp.getArgs()).map(this::format).collect(Collectors.joining(", "));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = auth != null ? auth.getName() : "anonymous";

        String http = "-";
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest r = attrs.getRequest();
            http = r.getMethod() + " " + r.getRequestURI() + " from " + r.getRemoteAddr();
        }

        long start = System.nanoTime();
        try {
            Object result = pjp.proceed();
            log.info("{} user={} [{}] args=[{}] -> {} in {} ms",
                    method, user, http, args, describe(result), (System.nanoTime() - start) / 1_000_000);
            return result;
        } catch (Throwable t) {
            log.error("{} user={} [{}] args=[{}] -> EXCEPTION {} in {} ms",
                    method, user, http, args, t, (System.nanoTime() - start) / 1_000_000);
            throw t;
        }
    }

    private String describe(Object result) {
        if (result instanceof ResponseEntity<?> re) {
            Object body = re.getBody();
            return "HTTP " + re.getStatusCode().value() + (body != null ? " body=" + format(body) : "");
        }
        return format(result);
    }

    /** Бинарные данные не пишем в лог, только размер. Пароли в аргументы контроллеров не попадают. */
    private String format(Object o) {
        if (o instanceof byte[] b) return "byte[" + b.length + "]";
        return String.valueOf(o);
    }
}
