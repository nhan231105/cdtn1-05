# Smart CRM L8 System Test Plan

## 1. Scope and exclusions

Verify the post-warranty CSAT survey flow for `survey_template`, `survey_invitation`, immutable customer responses, authentication, authorization, reporting, and the browser journey. The plan contains 12 cases across the required happy path, boundary, invalid-input, security/data-quality, and repeatability categories.

Coverage includes API behavior, database invariants, Flyway/seed idempotency, and one browser E2E flow. Real SMS/Zalo delivery, production-scale load, penetration testing, and multi-instance deployment are outside this run; the message gateway is simulated.

## 2. Test design and case count

- **TC-01–03:** template CRUD and field-length boundaries; invitation create/read/update before delivery.
- **TC-04–05:** missing or invalid input and safe error responses.
- **TC-06–08:** OWASP access control/authentication checks, internal service-key protection, cancellation tombstones, and post-send immutability.
- **TC-09–11:** repeated Flyway startup, stable seed/password hashing, duplicate prevention, and SQL-injection payload handling.
- **TC-12:** browser E2E for a fresh synthetic ticket, response submission, duplicate prevention, role-scoped views, export, and XSS sanitization.

The 12 system cases use Playwright API requests and Chromium. The backend suite additionally runs Spring Boot/JUnit unit and integration tests, including MockMvc coverage. Frontend component tests run with Vitest.

## 3. Measurable exit criteria

- All 12 system cases finish **PASS** and each has `docs/screenshots/TC-XX.png` evidence.
- Backend JUnit suite: zero failures/errors; frontend component tests: zero failures; production build succeeds.
- Flyway history contains exactly successful V1–V6 migrations; the second backend startup reports no migration is needed.
- Seed counts remain `2|2|2|4|1|4` across the second startup; password storage is BCrypt.
- Security probes show role/key/token denials without unauthorized data changes; invalid input does not expose SQL/JDBC/stack details.
- All setup and API/E2E activity uses a newly created `smartcrm_l8_test` schema on port 3307. Port 3306 is not used.

## 4. Environment and fixtures

- Windows PowerShell; Java 21; Maven Wrapper; Node/npm; Chromium via Playwright; MySQL 8.0.46.
- A disposable MySQL datadir is created by `scripts/start-test-db.ps1 -Port 3307`. Flyway applies the repository's V1–V6 migrations. The prior local test datadir is retained; it is not reset or deleted.
- V2 provides synthetic centers, customers, technicians, and tickets 401–404. Test-only bootstrap creates the role accounts with a temporary password from the environment. No real SMS/Zalo provider is called.
- `frontend/e2e/prepare-fixture.ps1` inserts one uniquely coded synthetic closed ticket and issues/sends its invitation through the real API. The E2E submits an immutable response only to this disposable schema.
- Test passwords, JWT secret, database password, and service key are ephemeral environment values and are not stored in this repository.
