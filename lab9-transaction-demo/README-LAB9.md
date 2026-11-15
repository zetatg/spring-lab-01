# Lab 9 - Transaction Management and Schema Versioning

## Requirements
- JDK 17 or newer
- No external database is required; the project uses in-memory H2.
- Run all commands from the repository root (the folder containing `mvnw.cmd`).
- In PowerShell, a command in the current folder must be started with `.\`, so use `.\mvnw.cmd`.

## 1. Start the application
```powershell
.\mvnw.cmd -f lab9-transaction-demo/pom.xml spring-boot:run
```
Wait for the line "Started Lab9Application" and for the Flyway messages
("Successfully applied 3 migrations"). The server listens on http://localhost:8081.

## 2. H2 Console
Open http://localhost:8081/h2-console and enter exactly:
- JDBC URL: `jdbc:h2:mem:txlab`
- User Name: `sa`
- Password: (leave empty)

Click **Connect**. The database is in memory, so it exists only while the application is running. If you stop the application, the data is lost; start it again to recreate it.

## 3. REST demonstrations
```powershell
# Initial balances: Alice 1000.00, Bob 500.00
Invoke-RestMethod http://localhost:8081/api/accounts

# Task A: successful transfer -> Alice 900.00, Bob 600.00
Invoke-RestMethod -Method Post "http://localhost:8081/api/transfer?fromId=1&toId=2&amount=100.00"

# Task A: rollback demo (HTTP 500 is expected); balances must not change
try { Invoke-RestMethod -Method Post http://localhost:8081/api/demo/rollback } catch { $_.Exception.Message }
Invoke-RestMethod http://localhost:8081/api/accounts

# Task B: propagation demo (HTTP 500 is expected each time)
Invoke-RestMethod http://localhost:8081/api/audit
try { Invoke-RestMethod -Method Post http://localhost:8081/api/demo/propagation/required } catch { $_.Exception.Message }
Invoke-RestMethod http://localhost:8081/api/audit          # still empty
try { Invoke-RestMethod -Method Post http://localhost:8081/api/demo/propagation/requires-new } catch { $_.Exception.Message }
Invoke-RestMethod http://localhost:8081/api/audit          # one REQUIRES_NEW record
```

## 4. Database verification (run in H2 Console)
```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history ORDER BY installed_rank;

SELECT * FROM accounts;

SELECT * FROM INFORMATION_SCHEMA.INDEXES
WHERE INDEX_NAME = 'IDX_ACCOUNTS_OWNER_NAME';
```
There are three migrations: V1, V2, and V3.

## 5. Integration tests (Tasks A-E)
Open a second terminal in the repository root and run:
```powershell
.\mvnw.cmd -f lab9-transaction-demo/pom.xml clean test
```
Test methods (in `Lab9IntegrationTests`):
- `transferMovesMoney`
- `insufficientFundsChangesNothing`
- `uncheckedExceptionAfterDebitRollsBack`
- `checkedExceptionCommitsByDefault`
- `rollbackForMakesCheckedExceptionRollBack`
- `requiredAuditRecordIsRolledBackWithOuter`
- `requiresNewAuditRecordSurvivesOuterRollback`
- `isolationLevelsShowExpectedSecondReadOnH2`
- `staleEntityUpdateRaisesOptimisticLockConflict`
- `allMigrationsApplied`

The isolation test prints the observed H2 values to the console:
`H2 READ_COMMITTED second read = ...` and `H2 REPEATABLE_READ second read = ...`.
Record the values the test actually prints; do not assume an engine-specific result.
