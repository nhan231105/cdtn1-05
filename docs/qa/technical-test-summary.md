# Smart CRM L8 Technical Test Summary

## Execution result

Final verification ran on 2026-11-13 against a newly initialized `smartcrm_l8_test` database on MySQL port 3307. Flyway applied V1–V6 from the repository on first application startup. A second startup on the same datadir logged “No migration necessary”; the migration history still contained exactly six successful versions. Before the E2E fixture was added, seed counts stayed `2|2|2|4|1|4`, and the Marketing password was stored as BCrypt.

| Suite | Observed result | Evidence |
|---|---|---|
| Backend Maven/JUnit | 54 run; 54 passed; 0 failures/errors/skips; `BUILD SUCCESS` | [backend-tests.txt](backend-tests.txt) |
| Frontend Vitest | 18/18 passed | [frontend-unit-tests.txt](frontend-unit-tests.txt) |
| Frontend production build | Vite build succeeded | [frontend-build.txt](frontend-build.txt) |
| Playwright system API cases | TC-01–TC-11: 11/11 passed on one clean final database run | [test-report.md](test-report.md); screenshots TC-01–TC-11 |
| Playwright browser E2E | TC-12: 1/1 passed on the same database after creating a fresh fixture | [test-report.md](test-report.md); [TC-12.png](../screenshots/TC-12.png) |

The final database snapshot records six successful migrations, one synthetic ticket added for TC-12, one survey response, and the seed counts before fixture insertion: [test-database-final-state.txt](test-database-final-state.txt).

TC-01–11 screenshots are Playwright evidence pages populated from actual API responses, database queries, and startup logs. TC-12 is a screenshot of the live frontend response list; it shows the sanitized comment and masked phone number. Initial failing evidence for the two harness defects is retained as `TC-06-initial-harness-failure.png` and `TC-09-initial-harness-failure.png`; the final `TC-06.png` and `TC-09.png` show PASS.

## Coverage map

- **Happy path:** TC-01 and TC-03; TC-12 exercises the customer-to-reporting browser flow.
- **Boundary values:** TC-02 tests maximum and maximum-plus-one text lengths.
- **Negative/exception input:** TC-04 and TC-05.
- **OWASP A01 Broken Access Control:** TC-06 checks Manager denial for Marketing endpoints and verifies the full template snapshot did not change.
- **OWASP A02 Cryptographic Failures:** TC-10 checks that the seeded Marketing password is BCrypt-hashed. This is a storage-format check, not a full cryptographic audit.
- **OWASP A04 Insecure Design:** TC-08 checks cancellation tombstones and disallows edits/deletion after delivery.
- **OWASP A05 Security Misconfiguration:** TC-05 verifies error responses do not expose stack, SQL, or JDBC details; this is a targeted check, not a configuration audit.
- **OWASP A07 Identification and Authentication Failures:** TC-06 checks missing/tampered JWTs; TC-07 checks missing, wrong, and valid internal service keys.
- **SQL/XSS input handling:** TC-11 stores a SQL-shaped string literally; TC-12 confirms script markup is removed before the response is displayed.
- **Repeatability/idempotency:** TC-09–11 cover migration rerun, stable bootstrap seeds, duplicate invitation rejection, and fixture-backed E2E behavior.

## Initial harness failures and fixes

The first TC-06 run changed only the final Base64URL character of a JWT signature. Some such mutations do not alter decoded signature bytes, so the API correctly treated that token as valid and returned the Manager role's 403. TC-06 was corrected to mutate the first signature character; commit `2a973f4c1e2fbc35dad9db78c2e723554fa18b0e`; rerun PASS.

The first TC-09 run decoded a PowerShell UTF-16LE startup log as UTF-8. Flyway had in fact logged that the schema was up to date. The reader now detects UTF-16LE; commit `b84aaca7cdf16107cbe03aab2a0036031405d49e`; rerun PASS.

These are test-harness defects, not application defects. The final 12-case run found **zero application bugs**. The workshop's requested minimum of three defects including one Critical/Major therefore remains unfulfilled; inventing defects would contradict the observed results.

## Limits and remaining evidence gaps

- The gateway was simulated; no real SMS/Zalo provider was contacted.
- This run is not a penetration test, a production multi-instance test, or a full OWASP audit. Only the explicitly listed checks were run.
- The backend suite evidence is a separate fresh-database Maven run; the 12 final system cases ran together on another newly initialized test datadir. The backend source was unchanged between those runs.
- Test secrets were temporary environment values and were not committed. MySQL 3306 was not used; the pre-existing local datadir was retained.
