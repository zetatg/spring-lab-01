package kz.iitu.lab9;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task B. The inner call goes to ANOTHER bean (AuditService), so it passes
 * through the Spring proxy. A call to a method of the same class would not.
 */
@Service
public class OuterService {

    private final AuditService audit;

    public OuterService(AuditService audit) {
        this.audit = audit;
    }

    @Transactional
    public void outerRequired(String message) {
        audit.recordRequired(message);
        throw new IllegalStateException("Outer failure (REQUIRED)");
    }

    @Transactional
    public void outerRequiresNew(String message) {
        audit.recordRequiresNew(message);
        throw new IllegalStateException("Outer failure (REQUIRES_NEW)");
    }
}
