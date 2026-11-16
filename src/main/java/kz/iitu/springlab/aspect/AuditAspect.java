package kz.iitu.springlab.aspect;

import kz.iitu.springlab.audit.Audited;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
@Component
@Order(1)
public class AuditAspect {

    private static final Logger log =
            LoggerFactory.getLogger(AuditAspect.class);

    @Around("@annotation(audited)")
    public Object audit(
            ProceedingJoinPoint pjp,
            Audited audited) throws Throwable {

        log.info(
                "[AUDIT] start {} action={} time={} args={}",
                pjp.getSignature().toShortString(),
                audited.action(),
                Instant.now(),
                audited.logArguments()
                        ? Arrays.toString(pjp.getArgs())
                        : "[hidden]"
        );

        try {
            Object result = pjp.proceed();

            log.info(
                    "[AUDIT] {} success",
                    audited.action()
            );

            return result;

        } catch (Throwable ex) {

            log.info(
                    "[AUDIT] {} failure: {}",
                    audited.action(),
                    ex.getMessage()
            );

            throw ex;
        }
    }
}