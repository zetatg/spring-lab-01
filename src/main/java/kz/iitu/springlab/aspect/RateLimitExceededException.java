package kz.iitu.springlab.aspect;

/** Thrown by RateLimitAspect; ApiExceptionHandler turns it into 429 Too Many Requests. */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String method, int perMinute, long retryAfterSeconds) {
        super("Rate limit exceeded for " + method + ": at most " + perMinute + " calls per minute");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
