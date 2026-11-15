package kz.iitu.springlab.service;

import kz.iitu.springlab.aspect.RateLimit;
import kz.iitu.springlab.audit.Audited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
public class CatalogService {

    /**
     * Task 4.5, the fix: a reference to this bean's PROXY. Calls through it pass the advice chain.
     * @Lazy, because the bean is still being built when Spring injects it into itself
     * (Spring Boot forbids circular references by default).
     */
    @Autowired
    @Lazy
    private CatalogService self;

    @RateLimit(perMinute = 5)          // variant 7: the sixth call within a minute is refused (429)
    public String findById(long id) {
        sleep(50);
        return "Item no. " + id;
    }

    @Audited(action = "CATALOG_LIST", logArguments = true)
    public List<String> findAll(int limit) {
        sleep(300);

        return IntStream.rangeClosed(1, limit)
                .mapToObj(i -> "Item no. " + i)
                .toList();
    }

    @Audited(action = "CATALOG_REMOVE")
    public String remove(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Invalid identifier: " + id);
        }

        return "Removed item no. " + id;
    }

    /** Task 4.3, BEFORE the fix: self-invocation through "this" goes past the proxy - no audit records. */
    public String removeTwiceViaThis(long id) {
        String first = remove(id);          // this.remove(...)
        String second = remove(id + 1);
        return first + "; " + second;
    }

    /** Task 4.5, AFTER the fix: the inner calls go through the proxy - two audit records. */
    public String removeTwice(long id) {
        String first = self.remove(id);
        String second = self.remove(id + 1);
        return first + "; " + second;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}