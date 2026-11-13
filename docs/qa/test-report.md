# Smart CRM L8 System Test Report

**Run date:** 2026-11-13 (Asia/Ho_Chi_Minh)

**Environment:** MySQL 8.0.46, `smartcrm_l8_test` on 3307, clean temporary datadir, backend 8080, Vite/Playwright 5173.

**Final status:** 12/12 system cases PASS. Each linked screenshot is the final successful run.

| TC | Preconditions | Steps | Test data | Expected result | Priority | Actual result | Status |
|---|---|---|---|---|---|---|---|
| TC-01 | Clean migrated DB; Marketing account; active seed template | Create inactive template; list/detail; update; delete; read after delete | Unique `TC01-*` template | 201, 200, 200, 204, then 404; seed template remains | P1 | POST 201; GET detail/list 200/200; PUT 200; DELETE 204; GET after delete 404. [Screenshot](../screenshots/TC-01.png) | PASS |
| TC-02 | Marketing account and active seed template | Submit exact maximum lengths, then one character over name limit | name 80/81; title 120; intro 1000 | Max accepted; 81-character name gets 400 naming `name` | P2 | 80/120/1000 accepted (201); name=81 rejected 400 with `name`; cleanup 204. [Screenshot](../screenshots/TC-02.png) | PASS |
| TC-03 | Ticket 401 closed; no invitation; valid service key | Create invitation; read list/detail; change channel before send | `ticket_id=401`, channel `ZALO` | 201 `CHO_GUI`; reads 200; pre-send update 200 | P1 | POST 201 `CHO_GUI`; GET detail/list 200/200; PATCH 200, channel `ZALO`. [Screenshot](../screenshots/TC-03.png) | PASS |
| TC-04 | Marketing authenticated; record template count | POST template without `name`; compare count | Missing `name` | 400 `VALIDATION_FAILED`, field named, no row added | P1 | 400; `VALIDATION_FAILED`; `name` present; template count 1→1. [Screenshot](../screenshots/TC-04.png) | PASS |
| TC-05 | Ticket 404 remains open; valid service key | POST without ticket id; POST open ticket; GET unknown template | Missing `ticket_id`; ticket 404; template 999999999 | 400 with field, 422 `TICKET_NOT_CLOSED`, 404; no sensitive error detail | P1 | 400 with `ticket_id`; 422 `TICKET_NOT_CLOSED`; 404 `TEMPLATE_NOT_FOUND`; no stack/SQL/JDBC detail. [Screenshot](../screenshots/TC-05.png) | PASS |
| TC-06 | Marketing and `manager101` accounts; seed template | Manager GET/POST/PUT template; omit JWT; tamper JWT signature; compare full DB snapshot | Manager token and signature mutation | Role mismatch 403; missing/invalid JWT 401; no template mutation | P1 | Manager GET/POST/PUT 403/403/403; no/tampered JWT 401/401; full template snapshot unchanged. [Screenshot](../screenshots/TC-06.png) | PASS |
| TC-07 | Invitation API on isolated DB | GET without/wrong/correct key; POST without key; compare ticket 403 count | Missing, invalid, valid `X-Service-Key`; ticket 403 | 401/401/200 and 401; denied POST causes no row | P1 | GET 401/401/200; POST without key 401; ticket 403 invitation count 0→0. [Screenshot](../screenshots/TC-07.png) | PASS |
| TC-08 | Tickets 402 and 403 closed and initially uninvited | Create/delete unsent 402 and retry; create/send 403 and PATCH/DELETE | Tickets 402, 403 | Cancellation tombstone blocks reissue; sent invitation is immutable | P1 | 402 create/delete/reissue 201/204/409 `INVITATION_CANCELLED`; 403 create/send/PATCH/DELETE 201/200/409/409. [Screenshot](../screenshots/TC-08.png) | PASS |
| TC-09 | Same clean datadir; backend started twice | Inspect second-start log and Flyway history | V1–V6 | Second start is a no-op; six successful versions | P1 | Log: “No migration necessary”; history 6/6 successful, versions 1–6. [Screenshot](../screenshots/TC-09.png) | PASS |
| TC-10 | Seed counts saved before second startup | Compare seed counts; inspect Marketing password hash format | Centers/customers/technicians/tickets/templates/users | Counts unchanged at 2/2/2/4/1/4; BCrypt hash | P1 | Before/after `2|2|2|4|1|4`; Marketing password storage detected as BCrypt. [Screenshot](../screenshots/TC-10.png) | PASS |
| TC-11 | One invitation exists for ticket 401; Marketing and valid key | Reissue 401; save/read SQL payload as template name; delete test template | Duplicate ticket 401; literal `' OR 1=1 -- <script>…` | Duplicate 409, count stays 1; payload remains literal data | P1 | Duplicate 409; ticket 401 count 1; template POST/GET/DELETE 201/200/204; payload preserved literally. [Screenshot](../screenshots/TC-11.png) | PASS |
| TC-12 | Fresh synthetic closed ticket and sent invitation; frontend and backend running | Submit score 5 and XSS-shaped comment at mobile width; inspect response; verify duplicate prevention, scoped views, masked phone, Excel export | Fresh `E2E-*` ticket; score 5; `<script>alert(1)</script>` comment payload | One immutable response; sanitized text; no script/dialog; authorized pages and export work | P1 | Browser E2E 1/1; comment rendered as `E2E XSS alert(1) intact`; no dialog; scoped statistics/list and Excel export passed. [Screenshot](../screenshots/TC-12.png) | PASS |

## Supporting suite results

- Backend: 54 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS` (`docs/qa/backend-tests.txt`).
- Frontend Vitest: 18/18 tests passed (`docs/qa/frontend-unit-tests.txt`).
- Frontend production build: succeeded (`docs/qa/frontend-build.txt`).
- Playwright system API cases: 11/11 passed on one clean final DB run.
- Playwright browser E2E: 1/1 passed on that same DB after fixture creation.

TC-01–11 screenshots are Playwright evidence pages populated with the actual API/database/log observations from each passing test. TC-12 is a screenshot of the running application UI. Two initial failures were test-harness issues, documented and fixed in the Bug Log; both cases passed on rerun.
