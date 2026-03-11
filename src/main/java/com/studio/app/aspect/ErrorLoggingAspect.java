package com.studio.app.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Cross-cutting error logger that writes every unhandled exception to a
 * dedicated error log file on the user's Desktop.
 *
 * <p><b>Activation:</b> loaded in every Spring profile <em>except</em>
 * {@code test}.  The companion {@code logback-spring.xml} routes this
 * logger's output to {@code ~/Desktop/studio-error-logs/error.log}.</p>
 *
 * <p><b>Pointcut:</b> intercepts exceptions thrown from the controller
 * layer only (the outermost boundary), so each error is logged exactly
 * once per request — even if it originated deeper in the service or
 * repository layer.</p>
 *
 * <p>Known business exceptions (4xx) are logged at {@code WARN} level
 * with their message only.  Unexpected exceptions (5xx) are logged at
 * {@code ERROR} level with the full stack trace.</p>
 */
@Aspect
@Component
@Profile("!test")
public class ErrorLoggingAspect {

    private static final Logger log =
            LoggerFactory.getLogger(ErrorLoggingAspect.class);

    /** Matches every public method inside controller implementations. */
    @Pointcut("within(com.studio.app.controller.impl..*)")
    public void controllerLayer() { /* pointcut definition */ }


    /**
     * Fires after any exception escapes a controller method.
     * Business exceptions are treated as warnings; everything else
     * is a genuine error worth a full stack trace.
     *
     * @param joinPoint the method that threw
     * @param ex        the thrown exception
     */
    @AfterThrowing(pointcut = "controllerLayer()", throwing = "ex")
    public void logError(JoinPoint joinPoint, Throwable ex) {
        String method = joinPoint.getSignature().toShortString();

        if (isBusinessException(ex)) {
            log.warn("BUSINESS | {} | {}", method, ex.getMessage());
        } else {
            log.error("UNEXPECTED | {} | {}", method, ex.getMessage(), ex);
        }
    }

    /**
     * Returns {@code true} for exceptions that map to 4xx responses
     * and should not produce full stack traces.
     */
    private boolean isBusinessException(Throwable ex) {
        String name = ex.getClass().getSimpleName();
        return name.equals("ResourceNotFoundException")
                || name.equals("BadRequestException")
                || name.equals("ConflictException")
                || name.equals("MethodArgumentNotValidException");
    }
}

