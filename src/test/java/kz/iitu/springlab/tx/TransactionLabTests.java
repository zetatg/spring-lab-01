package kz.iitu.springlab.tx;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lab 9. No class-level @Transactional on purpose: the tests must commit and
 * roll back real transactions (REQUIRES_NEW, optimistic locking, isolation).
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionLabTests {

    @Autowired AccountRepository accounts;
    @Autowired AuditLogRepository auditLogs;
    @Autowired TransferService transfers;
    @Autowired OuterService outer;
    @Autowired PlatformTransactionManager txManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;

    @BeforeEach
    void resetData() {
        auditLogs.deleteAll();
        accounts.deleteAll();
        accounts.save(new Account("Alice", new BigDecimal("1000.00")));
        accounts.save(new Account("Bob", new BigDecimal("500.00")));
    }

    private Long id(String owner) {
        return accounts.findByOwnerName(owner).orElseThrow().getId();
    }

    private BigDecimal balance(String owner) {
        return accounts.findByOwnerName(owner).orElseThrow().getBalance();
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    // ---------- Task A ----------

    @Test
    void transferMovesMoney() {
        transfers.transfer(id("Alice"), id("Bob"), new BigDecimal("100.00"));
        assertMoney("900.00", balance("Alice"));
        assertMoney("600.00", balance("Bob"));
    }

    @Test
    void insufficientFundsChangesNothing() {
        assertThrows(IllegalStateException.class,
                () -> transfers.transfer(id("Alice"), id("Bob"), new BigDecimal("5000.00")));
        assertMoney("1000.00", balance("Alice"));
        assertMoney("500.00", balance("Bob"));
    }

    @Test
    void uncheckedExceptionAfterDebitRollsBack() {
        assertThrows(IllegalStateException.class,
                () -> transfers.transferFailingAfterDebit(id("Alice"), id("Bob"), new BigDecimal("100.00")));
        assertMoney("1000.00", balance("Alice"));
    }

    @Test
    void checkedExceptionCommitsByDefault() {
        assertThrows(Exception.class,
                () -> transfers.transferCheckedNoRollback(id("Alice"), id("Bob"), new BigDecimal("100.00")));
        assertMoney("900.00", balance("Alice")); // debit was committed: money lost
    }

    @Test
    void rollbackForMakesCheckedExceptionRollBack() {
        assertThrows(Exception.class,
                () -> transfers.transferCheckedWithRollback(id("Alice"), id("Bob"), new BigDecimal("100.00")));
        assertMoney("1000.00", balance("Alice"));
    }

    // ---------- Task B ----------

    @Test
    void requiredAuditRecordIsRolledBackWithOuter() {
        assertThrows(IllegalStateException.class, () -> outer.outerRequired("required"));
        assertEquals(0, auditLogs.count());
    }

    @Test
    void requiresNewAuditRecordSurvivesOuterRollback() {
        assertThrows(IllegalStateException.class, () -> outer.outerRequiresNew("requires_new"));
        assertEquals(1, auditLogs.count());
    }

    // ---------- Task C ----------

    private BigDecimal readAliceTwiceAroundConcurrentUpdate(int isolation) {
        TransactionTemplate reader = new TransactionTemplate(txManager);
        reader.setIsolationLevel(isolation);
        TransactionTemplate writer = new TransactionTemplate(txManager);
        writer.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        String sql = "select balance from accounts where owner_name = 'Alice'";
        return reader.execute(status -> {
            BigDecimal first = jdbc.queryForObject(sql, BigDecimal.class);
            writer.executeWithoutResult(s ->
                    jdbc.update("update accounts set balance = balance - 100 where owner_name = 'Alice'"));
            BigDecimal second = jdbc.queryForObject(sql, BigDecimal.class);
            assertMoney("1000.00", first);
            return second;
        });
    }

    @Test
    void readCommittedSeesCommittedChange() {
        assertMoney("900.00", readAliceTwiceAroundConcurrentUpdate(TransactionDefinition.ISOLATION_READ_COMMITTED));
    }

    @Test
    void repeatableReadObservation() {
        // Engine-specific (H2): we only record what we observe, as the lab asks.
        BigDecimal second = readAliceTwiceAroundConcurrentUpdate(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        System.out.println("H2 REPEATABLE_READ second read = " + second);
        assertNotNull(second);
    }

    // ---------- Task D ----------

    @Test
    void staleUpdateIsRejectedByVersion() {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        Account first = tt.execute(s -> accounts.findByOwnerName("Alice").orElseThrow());
        Account stale = tt.execute(s -> accounts.findByOwnerName("Alice").orElseThrow());

        first.debit(new BigDecimal("100.00"));
        tt.executeWithoutResult(s -> accounts.save(first));

        stale.debit(new BigDecimal("50.00"));
        assertThrows(OptimisticLockingFailureException.class,
                () -> tt.executeWithoutResult(s -> accounts.save(stale)));

        assertMoney("900.00", balance("Alice")); // the first update was not overwritten
    }

    // ---------- Task E ----------

    @Test
    void allMigrationsApplied() {
        assertEquals(6, flyway.info().applied().length); // V1-V4 (lab 9) + V5 security + V6 remember-me (lab 10)
        assertEquals(0, flyway.info().pending().length);
    }
}
