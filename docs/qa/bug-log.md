# QA Bug Log

No application defect was identified by the final 12-case run. The two initial failures below were defects in the test harness; each was fixed in its own local commit and passed on rerun. The workshop's target of at least three discovered defects, including one Critical/Major, is not met by actual evidence and is intentionally not fabricated.

### QA-HARNESS-01 — Low

1. **Mã/Mức độ:** QA-HARNESS-01 / Low — test-vector construction.
2. **Phát hiện bởi:** TC-06, initial run.
3. **Triệu chứng:** Test expected tampered JWT to return 401 but received 403; the final status screenshot is [TC-06-initial-harness-failure.png](../screenshots/TC-06-initial-harness-failure.png).
4. **Cách tái lập:** Change only the last Base64URL character of the JWT signature and call the Marketing-only template endpoint.
5. **Nguyên nhân gốc rễ:** Depending on the last character, the mutation can change only unused Base64 padding bits and leave the decoded signature unchanged.
6. **Cách sửa:** Mutate the first encoded signature character so the JWT signature bytes are changed.
7. **Commit sửa + chạy lại:** [2a973f4](https://github.com/nhan231105/cdtn1-05/commit/2a973f4c1e2fbc35dad9db78c2e723554fa18b0e) (local, not pushed); TC-06 rerun returned 401 for the tampered JWT and **PASS**.

### QA-HARNESS-02 — Low

1. **Mã/Mức độ:** QA-HARNESS-02 / Low — evidence-log decoding.
2. **Phát hiện bởi:** TC-09, initial run.
3. **Triệu chứng:** Test could not find “No migration necessary” and displayed garbled text; the initial evidence is [TC-09-initial-harness-failure.png](../screenshots/TC-09-initial-harness-failure.png).
4. **Cách tái lập:** Read a PowerShell `*>`-redirected startup log as UTF-8 and assert on its Flyway message.
5. **Nguyên nhân gốc rễ:** The redirected log is UTF-16LE, but the test reader decoded it as UTF-8.
6. **Cách sửa:** Detect NUL bytes and decode UTF-16LE; otherwise decode UTF-8.
7. **Commit sửa + chạy lại:** [b84aaca](https://github.com/nhan231105/cdtn1-05/commit/b84aaca7cdf16107cbe03aab2a0036031405d49e) (local, not pushed); TC-09 found the no-op log and six successful migrations and **PASS**.
