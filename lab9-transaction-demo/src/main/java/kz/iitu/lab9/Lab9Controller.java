package kz.iitu.lab9;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST endpoints used by the Lab 9 practical demonstration.
 * Unhandled IllegalStateException is intentionally returned as HTTP 500
 * for the rollback and propagation demonstrations.
 */
@RestController
@RequestMapping("/api")
public class Lab9Controller {

    private final AccountRepository accounts;
    private final AuditRecordRepository auditRecords;
    private final TransferService transfers;
    private final OuterService outer;

    public Lab9Controller(AccountRepository accounts,
                          AuditRecordRepository auditRecords,
                          TransferService transfers,
                          OuterService outer) {
        this.accounts = accounts;
        this.auditRecords = auditRecords;
        this.transfers = transfers;
        this.outer = outer;
    }

    @GetMapping("/accounts")
    public List<Account> accounts() {
        return accounts.findAll();
    }

    @PostMapping("/transfer")
    public List<Account> transfer(@RequestParam Long fromId,
                                  @RequestParam Long toId,
                                  @RequestParam BigDecimal amount) {
        transfers.transfer(fromId, toId, amount);
        return accounts.findAll();
    }

    @PostMapping("/demo/rollback")
    public void rollbackDemo() {
        Account alice = accounts.findByOwnerName("Alice")
                .orElseThrow(() -> new IllegalStateException("Alice account not found"));
        transfers.transferFailingAfterDebit(
                alice.getId(), alice.getId(), new BigDecimal("100.00"));
    }

    @PostMapping("/demo/propagation/required")
    public void requiredDemo() {
        outer.outerRequired("REQUIRED audit record");
    }

    @PostMapping("/demo/propagation/requires-new")
    public void requiresNewDemo() {
        outer.outerRequiresNew("REQUIRES_NEW audit record");
    }

    @GetMapping("/audit")
    public List<AuditRecord> audit() {
        return auditRecords.findAll();
    }
}
