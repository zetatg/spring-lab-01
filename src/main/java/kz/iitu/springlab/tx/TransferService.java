package kz.iitu.springlab.tx;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransferService {

    private final AccountRepository repository;

    public TransferService(AccountRepository repository) {
        this.repository = repository;
    }

    /** Task A: atomic transfer. Unchecked exceptions roll everything back. */
    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        Account from = repository.findById(fromId).orElseThrow();
        Account to = repository.findById(toId).orElseThrow();
        from.debit(amount);
        to.credit(amount);
        repository.save(from);
        repository.save(to);
    }

    /** Unchecked exception after debit, before credit -> rollback. */
    @Transactional
    public void transferFailingAfterDebit(Long fromId, Long toId, BigDecimal amount) {
        Account from = repository.findById(fromId).orElseThrow();
        from.debit(amount);
        repository.save(from);
        throw new IllegalStateException("Simulated failure after debit");
    }

    /** Checked exception: by default Spring COMMITS (no rollback). */
    @Transactional
    public void transferCheckedNoRollback(Long fromId, Long toId, BigDecimal amount) throws Exception {
        Account from = repository.findById(fromId).orElseThrow();
        from.debit(amount);
        repository.save(from);
        throw new Exception("Checked failure after debit");
    }

    /** Same, but rollbackFor makes the checked exception roll back. */
    @Transactional(rollbackFor = Exception.class)
    public void transferCheckedWithRollback(Long fromId, Long toId, BigDecimal amount) throws Exception {
        Account from = repository.findById(fromId).orElseThrow();
        from.debit(amount);
        repository.save(from);
        throw new Exception("Checked failure after debit");
    }
}
