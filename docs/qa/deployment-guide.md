# Smart CRM L8 Test Deployment Guide

## 1. Requirements

- Windows PowerShell, Java 21, Node.js/npm, and MySQL 8.0.
- A local MySQL installation with `mysqld.exe`, `mysql.exe`, and `mysqladmin.exe` available. The commands below use the repository's default MySQL 8 path; pass `-MySqlBin` when yours differs.
- Use only a disposable test machine/schema. This guide binds MySQL to `127.0.0.1:3307`; do not point the test backend at port 3306 or production data.

## 2. Create the isolated database

From the repository root, start a new temporary MySQL datadir and schema:

```powershell
$testDb = .\scripts\start-test-db.ps1 -Port 3307
$testDb
```

Record the printed `DataDirectory`. The script creates `smartcrm_l8_test`; it does not reuse a server already listening on 3307. Configure a password for the isolated test account and use the same value in `DB_PASSWORD` and `TEST_DB_PASSWORD`. Do not save that value in a tracked file.

The new test server starts with an empty local root password. Set a temporary one only on this newly created server:

```powershell
$mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
$env:MYSQL_PWD = ''
& $mysql --no-defaults --protocol=TCP --host=127.0.0.1 --port=3307 --user=root --execute="ALTER USER 'root'@'localhost' IDENTIFIED BY '<temporary-test-password>';"
```

Start the backend twice against the same test datadir when verifying Flyway repeatability. Flyway applies V1–V6 on first startup; the second startup should report that no migration is necessary. The test bootstrap creates demo role accounts with the password supplied through the environment.

## 3. Configure and start the backend

Set the following environment values in the shell that starts the backend:

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3307/smartcrm_l8_test?connectionTimeZone=Asia/Ho_Chi_Minh'
$env:DB_USER = '<isolated-test-user>'
$env:DB_PASSWORD = '<isolated-test-password>'
$env:SERVICE_API_KEY = '<random test-only value, at least 32 characters>'
$env:JWT_SECRET = '<random test-only secret>'
$env:BOOTSTRAP_ENABLED = 'true'
$env:BOOTSTRAP_PASSWORD = '<temporary test password, at least 12 characters>'
$env:SURVEY_SCHEDULER_ENABLED = 'false'
$env:SURVEY_PUBLIC_BASE_URL = 'http://127.0.0.1:5173/khao-sat'
```

Run `cd backend; .\mvnw.cmd spring-boot:run`. Confirm the log names `smartcrm_l8_test` on port 3307 and Flyway reaches version 6. Never use a real SMS/Zalo key for this test; the gateway is simulated.

## 4. Start the frontend and run checks

In a second shell, run `cd frontend; npm ci; npm run dev`. For the verification suite:

```powershell
npm test
npm run build
npm run test:e2e -- system-qa.spec.js
```

Set `QA_API_BASE`, `QA_PASSWORD`, `QA_SERVICE_KEY`, `QA_MYSQL_BIN`, `TEST_DB_PASSWORD`, `QA_SECOND_STARTUP_LOG`, and `QA_SEED_COUNTS_BEFORE` for `system-qa.spec.js`. To run browser E2E, set `E2E_SERVICE_KEY`, run `frontend/e2e/prepare-fixture.ps1 -TestPort 3307 -ApiBase http://127.0.0.1:8080`, then pass its returned token and ticket code as `E2E_SURVEY_TOKEN` and `E2E_TICKET_CODE`; set `E2E_PASSWORD` to the test bootstrap password. Run `npm run test:e2e -- l8.spec.js`.

The fixture script requires a schema ending in `_test`, refuses port 3306, inserts a unique synthetic ticket, and creates/sends its invitation through the API. The browser test writes one immutable response to this disposable schema.

## 5. Verify, preserve evidence, and stop

Review `docs/qa/test-report.md`, `docs/qa/bug-log.md`, `docs/qa/technical-test-summary.md`, and `docs/screenshots/TC-01.png` through `TC-12.png`. Keep initial-failure screenshots when documenting a test-harness correction. Stop the backend/frontend with Ctrl+C. Stop only the isolated MySQL process on port 3307 with `mysqladmin shutdown`; retain its datadir unless its owner separately authorizes cleanup.

### Security checklist

- [ ] Use a dedicated, least-privilege database account and an isolated `_test` schema.
- [ ] Keep database passwords, JWT secrets, and service keys in environment/secret storage; never commit them.
- [ ] Use a random service key of at least 32 characters and rotate it outside the test run.
- [ ] Disable demo-account bootstrap in deployed environments; allow it only for isolated tests.
- [ ] Require HTTPS/TLS for deployed browser/API traffic and encrypted database connections where supported.
- [ ] Restrict database/API network access and configure CORS for the intended frontend origins.
- [ ] Verify role checks, BCrypt password hashes, and safe error messages before deployment.
