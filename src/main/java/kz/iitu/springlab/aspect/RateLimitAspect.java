package kz.iitu.springlab.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Laboratory work no. 4, individual variant 7: rate limiting.
 * @Around advice on @RateLimit(perMinute = N); a sliding window of the last 60 seconds per method.
 * @Order(0) - outermost, so a refused call is not logged, timed or audited as if it had run.
 */
@Aspect
@Component
@Order(0)
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);
    private static final long WINDOW_NANOS = 60_000_000_000L;

    private final Map<String, Deque<Long>> calls = new ConcurrentHashMap<>();

    @Around("@annotation(limit)")
    public Object limit(ProceedingJoinPoint pjp, RateLimit limit) throws Throwable {
        String method = pjp.getSignature().toShortString();
        Deque<Long> window = calls.computeIfAbsent(method, k -> new ArrayDeque<>());
        long now = System.nanoTime();

        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= WINDOW_NANOS) {
                window.pollFirst();                                   // forget calls older than a minute
            }
            if (window.size() >= limit.perMinute()) {
                long retryAfter = Math.max(1, (WINDOW_NANOS - (now - window.peekFirst())) / 1_000_000_000L);
                log.warn("[RATE] {} refused: {} calls in the last minute (limit {})",
                        method, window.size(), limit.perMinute());
                throw new RateLimitExceededException(method, limit.perMinute(), retryAfter);
            }
            window.addLast(now);
            log.info("[RATE] {} call {}/{} in the current minute", method, window.size(), limit.perMinute());
        }
        return pjp.proceed();
    }

    /** For tests and the demonstration: start a fresh minute. */
    public void reset() {
        calls.clear();
    }
}
