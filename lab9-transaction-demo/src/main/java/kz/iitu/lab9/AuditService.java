package kz.iitu.lab9;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditRecordRepository repository;

    public AuditService(AuditRecordRepository repository) {
        this.repository = repository;
    }

    /** Joins the caller's transaction: rolled back together with it. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordRequired(String message) {
        repository.save(new AuditRecord(message));
    }

    /** Own, independent transaction: commits even if the caller later rolls back. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRequiresNew(String message) {
        repository.save(new AuditRecord(message));
    }
}
