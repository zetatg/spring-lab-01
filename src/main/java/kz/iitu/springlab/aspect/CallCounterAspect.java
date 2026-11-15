package kz.iitu.springlab.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Laboratory work no. 4, individual variant 1: a call counter.
 * @Before advice on the service-layer pointcut; counts calls per method name.
 */
@Aspect
@Component
@Order(4)   // innermost; does not change the relative order of Audit(1), Logging(2), Timing(3)
public class CallCounterAspect {

    private final Map<String, Integer> methodCounts = new ConcurrentHashMap<>();

    @Before("kz.iitu.springlab.aspect.Pointcuts.serviceLayer()")
    public void countCall(JoinPoint jp) {
        String methodName = jp.getSignature().toShortString();
        methodCounts.merge(methodName, 1, Integer::sum);   // atomic, so thread-safe
    }

    public Map<String, Integer> getStatistics() {
        return Map.copyOf(methodCounts);   // immutable snapshot; the internal map is not exposed
    }
}
