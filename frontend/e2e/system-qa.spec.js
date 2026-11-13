import { expect, test } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { mkdirSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const apiBase = process.env.QA_API_BASE || 'http://127.0.0.1:8080/api';
const password = process.env.QA_PASSWORD;
const serviceKey = process.env.QA_SERVICE_KEY;
const screenshotDir = resolve(process.cwd(), '..', 'docs', 'screenshots');

test.describe.configure({ mode: 'serial' });

function database(query) {
  const mysql = process.env.QA_MYSQL_BIN || 'C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe';
  return execFileSync(mysql, [
    '--no-defaults', '--protocol=TCP', '--host=127.0.0.1', '--port=3307', '--user=root',
    '--database=smartcrm_l8_test', '--batch', '--skip-column-names', `--execute=${query}`,
  ], {
    encoding: 'utf8',
    env: { ...process.env, MYSQL_PWD: process.env.TEST_DB_PASSWORD || '' },
  }).trim();
}

async function body(response) {
  try { return await response.json(); } catch { return {}; }
}

async function login(request, username) {
  const response = await request.post(`${apiBase}/auth/login`, { data: { username, password } });
  const data = await body(response);
  if (response.status() !== 200 || !data.access_token) {
    throw new Error(`Login ${username}: HTTP ${response.status()}`);
  }
  return data.access_token;
}

const bearer = token => ({ Authorization: `Bearer ${token}` });
const keyHeader = value => ({ 'X-Service-Key': value });

async function captureCase(page, testInfo, details, action) {
  let actual = 'No result captured';
  let failure;
  try {
    actual = await action();
  } catch (error) {
    failure = error;
    actual = `Execution error: ${error.message}`;
  }

  const status = failure ? 'FAIL' : 'PASS';
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.setContent(`<!doctype html><html><head><meta charset="utf-8"><style>
    *{box-sizing:border-box}body{margin:0;background:#edf2f5;color:#1b3040;font:16px/1.5 Arial,sans-serif}
    main{max-width:1100px;margin:48px auto;background:white;border:1px solid #bdcbd5;border-radius:8px;overflow:hidden}
    header{background:#183040;color:white;padding:22px 30px;font-size:22px;font-weight:700}
    section{padding:28px 32px}.badge{display:inline-block;padding:6px 14px;border-radius:4px;font-weight:700;background:#d8f3e3;color:#126239}
    .fail{background:#ffe0df;color:#9b211d}.row{margin-top:18px;padding:14px;background:#f4f7f9;border-left:4px solid #34627d}
    h2{font-size:15px;margin:0 0 6px;color:#436174}pre{white-space:pre-wrap;overflow-wrap:anywhere;font:15px/1.5 Consolas,monospace;margin:0}
    footer{padding:0 32px 26px;color:#607987;font-size:13px}
  </style></head><body><main><header id="title"></header><section>
    <span id="status" class="badge"></span>
    <div class="row"><h2>Tiền điều kiện</h2><pre id="precondition"></pre></div>
    <div class="row"><h2>Bước và dữ liệu thử</h2><pre id="input"></pre></div>
    <div class="row"><h2>Kết quả mong đợi</h2><pre id="expected"></pre></div>
    <div class="row"><h2>Kết quả thực tế</h2><pre id="actual"></pre></div>
  </section><footer>Minh chứng được tạo từ phản hồi API, truy vấn DB hoặc log chạy thực tế của test case.</footer></main></body></html>`);
  await page.evaluate(({ details, status, actual }) => {
    document.querySelector('#title').textContent = `${details.id} · ${details.title}`;
    const badge = document.querySelector('#status');
    badge.textContent = status;
    if (status === 'FAIL') badge.classList.add('fail');
    document.querySelector('#precondition').textContent = details.precondition;
    document.querySelector('#input').textContent = details.input;
    document.querySelector('#expected').textContent = details.expected;
    document.querySelector('#actual').textContent = actual;
  }, { details, status, actual });
  mkdirSync(screenshotDir, { recursive: true });
  await page.screenshot({ path: resolve(screenshotDir, `${details.id}.png`), fullPage: true });
  if (failure) throw failure;
}

test('TC-01 template CRUD happy path through Marketing API', async ({ page, request }, testInfo) => {
  await captureCase(page, testInfo, {
    id: 'TC-01', title: 'survey_template: tạo, đọc, cập nhật, xóa mẫu không hoạt động',
    precondition: 'Database sạch; tài khoản Marketing đã được bootstrap; mẫu seed đang hoạt động.',
    input: 'POST mẫu inactive → GET chi tiết/danh sách → PUT cập nhật → DELETE → GET lại.',
    expected: '201, 200, 200, 204, 404; dữ liệu sau mỗi thao tác đúng; mẫu hoạt động seed vẫn còn.',
  }, async () => {
    const token = await login(request, 'marketing');
    const name = `TC01-${Date.now()}`;
    const created = await request.post(`${apiBase}/survey-templates`, {
      headers: bearer(token), data: { name, title: 'Tiêu đề TC01', intro_text: 'Mô tả TC01', is_active: false },
    });
    const createdBody = await body(created);
    const id = createdBody.template_id;
    const detail = await request.get(`${apiBase}/survey-templates/${id}`, { headers: bearer(token) });
    const list = await request.get(`${apiBase}/survey-templates`, { headers: bearer(token) });
    const updated = await request.put(`${apiBase}/survey-templates/${id}`, {
      headers: bearer(token), data: { name, title: 'Tiêu đề TC01 đã sửa', intro_text: 'Mô tả đã sửa', is_active: false },
    });
    const deleted = await request.delete(`${apiBase}/survey-templates/${id}`, { headers: bearer(token) });
    const afterDelete = await request.get(`${apiBase}/survey-templates/${id}`, { headers: bearer(token) });
    const listBody = await body(list);
    const detailBody = await body(detail);
    const updatedBody = await body(updated);
    expect(created.status()).toBe(201);
    expect(detail.status()).toBe(200);
    expect(detailBody.name).toBe(name);
    expect(listBody.items.some(item => item.template_id === id)).toBe(true);
    expect(updated.status()).toBe(200);
    expect(updatedBody.title).toBe('Tiêu đề TC01 đã sửa');
    expect(deleted.status()).toBe(204);
    expect(afterDelete.status()).toBe(404);
    return `POST=${created.status()}, GET chi tiết/danh sách=${detail.status()}/${list.status()}, PUT=${updated.status()}, DELETE=${deleted.status()}, GET sau xóa=${afterDelete.status()}; mẫu=${name}.`;
  });
});

test('TC-02 template boundary lengths at max and max+1', async ({ page, request }, testInfo) => {
  await captureCase(page, testInfo, {
    id: 'TC-02', title: 'survey_template: biên độ dài 80/120/1000',
    precondition: 'Database sạch; role Marketing hợp lệ.',
    input: 'name=80 ký tự, title=120, intro_text=1000; sau đó name=81.',
    expected: 'Giá trị đúng biên được chấp nhận; vượt biên một ký tự trả 400 và nêu trường name.',
  }, async () => {
    const token = await login(request, 'marketing');
    const name = `B${'n'.repeat(79)}`;
    const accepted = await request.post(`${apiBase}/survey-templates`, {
      headers: bearer(token), data: { name, title: 't'.repeat(120), intro_text: 'i'.repeat(1000), is_active: false },
    });
    const acceptedBody = await body(accepted);
    const id = acceptedBody.template_id;
    const rejected = await request.post(`${apiBase}/survey-templates`, {
      headers: bearer(token), data: { name: 'n'.repeat(81), title: 'title', intro_text: 'intro', is_active: false },
    });
    const rejectedBody = await body(rejected);
    const deleted = await request.delete(`${apiBase}/survey-templates/${id}`, { headers: bearer(token) });
    expect(accepted.status()).toBe(201);
    expect(rejected.status()).toBe(400);
    expect(rejectedBody.error.fields.name).toBeTruthy();
    expect(deleted.status()).toBe(204);
    return `Max lengths POST=${accepted.status()} (80/120/1000); name=81 POST=${rejected.status()}, field=name; cleanup DELETE=${deleted.status()}.`;
  });
});

test('TC-03 invitation create/read/update before send', async ({ page, request }, testInfo) => {
  await captureCase(page, testInfo, {
    id: 'TC-03', title: 'survey_invitation: tạo, đọc, đổi kênh trước khi gửi',
    precondition: 'Phiếu 401 ở trạng thái DA_DONG, chưa có lời mời; Service Key đúng.',
    input: 'POST ticket_id=401 → GET danh sách/chi tiết → PATCH channel=ZALO.',
    expected: '201 CHO_GUI; GET 200; PATCH 200 đổi kênh; chưa gửi nên thao tác hợp lệ.',
  }, async () => {
    const headers = keyHeader(serviceKey);
    const created = await request.post(`${apiBase}/survey-invitations`, { headers, data: { ticket_id: 401 } });
    const createdBody = await body(created);
    const id = createdBody.invitation_id;
    const detail = await request.get(`${apiBase}/survey-invitations/${id}`, { headers });
    const detailBody = await body(detail);
    const list = await request.get(`${apiBase}/survey-invitations`, { headers });
    const updated = await request.patch(`${apiBase}/survey-invitations/${id}`, { headers, data: { channel: 'ZALO' } });
    const updatedBody = await body(updated);
    expect(created.status()).toBe(201);
    expect(createdBody.status).toBe('CHO_GUI');
    expect(detail.status()).toBe(200);
    expect(detailBody.ticket_id).toBe(401);
    expect(list.status()).toBe(200);
    expect(updated.status()).toBe(200);
    expect(updatedBody.channel).toBe('ZALO');
    return `POST=${created.status()} (${createdBody.status}), GET chi tiết/danh sách=${detail.status()}/${list.status()}, PATCH=${updated.status()} channel=${updatedBody.channel}.`;
  });
});

test('TC-04 missing required template field returns 400 with field name', async ({ page, request }, testInfo) => {
  await captureCase(page, testInfo, {
    id: 'TC-04', title: 'thiếu trường bắt buộc của survey_template',
    precondition: 'Marketing đã đăng nhập; ghi nhận số mẫu trước request.',
    input: 'POST /survey-templates bỏ trường name.',
    expected: '400 VALIDATION_FAILED; body.error.fields có name; số mẫu không tăng.',
  }, async () => {
    const token = await login(request, 'marketing');
    const headers = bearer(token);
    const beforeBody = await body(await request.get(`${apiBase}/survey-templates`, { headers }));
    const response = await request.post(`${apiBase}/survey-templates`, {
      headers, data: { title: 'Thiếu tên', intro_text: 'Mô tả', is_active: false },
    });
    const error = await body(response);
    const afterBody = await body(await request.get(`${apiBase}/survey-templates`, { headers }));
    expect(response.status()).toBe(400);
    expect(error.error.code).toBe('VALIDATION_FAILED');
    expect(error.error.fields.name).toBeTruthy();
    expect(afterBody.items.length).toBe(beforeBody.items.length);
    return `POST=${response.status()}, code=${error.error.code}, field=name (${Boolean(error.error.fields.name)}), template count=${beforeBody.items.length}→${afterBody.items.length}.`;
  });
});

test('TC-05 invalid invitation data and safe error body', async ({ page, request }, testInfo) => {
  await captureCase(page, testInfo, {
    id: 'TC-05', title: 'dữ liệu lời mời sai và phản hồi lỗi an toàn',
    precondition: 'Ticket 404 chưa đóng; Service Key đúng.',
    input: 'POST không có ticket_id; POST ticket_id=404; GET template id không tồn tại.',
    expected: '400 kèm ticket_id; 422 TICKET_NOT_CLOSED; 404; không lộ stack trace/SQL/JDBC.',
  }, async () => {
    const headers = keyHeader(serviceKey);
    const missing = await request.post(`${apiBase}/survey-invitations`, { headers, data: {} });
    const missingBody = await body(missing);
    const openTicket = await request.post(`${apiBase}/survey-invitations`, { headers, data: { ticket_id: 404 } });
    const openBody = await body(openTicket);
    const token = await login(request, 'marketing');
    const notFound = await request.get(`${apiBase}/survey-templates/999999999`, { headers: bearer(token) });
    const notFoundBody = await body(notFound);
    const combined = JSON.stringify([missingBody, openBody, notFoundBody]);
    expect(missing.status()).toBe(400);
    expect(missingBody.error.fields.ticket_id).toBeTruthy();
    expect(openTicket.status()).toBe(422);
    expect(openBody.error.code).toBe('TICKET_NOT_CLOSED');
    expect(notFound.status()).toBe(404);
    expect(combined).not.toMatch(/org\.springframework|SQLException|jdbc:mysql|stack trace/i);
    return `missing ticket_id=${missing.status()} (field present), open ticket=${openTicket.status()} ${openBody.error.code}, unknown template=${notFound.status()} ${notFoundBody.error.code}; response has no stack/SQL/JDBC details.`;
  });
});

test('TC-06 A01 role boundary denies manager and leaves templates unchanged', async ({ page, request }, testInfo) => {
  await captureCase(page, testInfo, {
    id: 'TC-06', title: 'A01 Broken Access Control và A07 token',
    precondition: 'Có tài khoản Marketing và manager101; mẫu seed tồn tại.',
    input: 'Manager GET/POST/PUT survey_templates; request thiếu JWT và JWT bị sửa chữ ký.',
    expected: 'Manager nhận 403; thiếu/sai JWT nhận 401; template count không đổi.',
  }, async () => {
    const marketing = await login(request, 'marketing');
    const manager = await login(request, 'manager101');
    const snapshot = () => database("SELECT GROUP_CONCAT(CONCAT_WS(':',template_id,name,title,intro_text,is_active) ORDER BY template_id SEPARATOR '|') FROM survey_template");
    const before = snapshot();
    const getDenied = await request.get(`${apiBase}/survey-templates`, { headers: bearer(manager) });
    const postDenied = await request.post(`${apiBase}/survey-templates`, {
      headers: bearer(manager), data: { name: `TC06-${Date.now()}`, title: 'Denied', intro_text: 'Denied', is_active: false },
    });
    const putDenied = await request.put(`${apiBase}/survey-templates/1`, {
      headers: bearer(manager), data: { name: 'CSAT sau bảo hành', title: 'Changed', intro_text: 'Denied', is_active: true },
    });
    const missingJwt = await request.get(`${apiBase}/survey-templates`);
    const jwtParts = manager.split('.');
    jwtParts[2] = `${jwtParts[2][0] === 'A' ? 'B' : 'A'}${jwtParts[2].slice(1)}`;
    const changedJwt = jwtParts.join('.');
    const tamperedJwt = await request.get(`${apiBase}/survey-templates`, { headers: bearer(changedJwt) });
    const after = snapshot();
    expect(getDenied.status()).toBe(403);
    expect(postDenied.status()).toBe(403);
    expect(putDenied.status()).toBe(403);
    expect(missingJwt.status()).toBe(401);
    expect(tamperedJwt.status()).toBe(401);
    expect(after).toBe(before);
    return `Manager GET/POST/PUT=${getDenied.status()}/${postDenied.status()}/${putDenied.status()}, no JWT/tampered JWT=${missingJwt.status()}/${tamperedJwt.status()}, full template snapshot unchanged=${before === after}.`;
  });
});

test('TC-07 X-Service-Key protects internal invitation API', async ({ page, request }, testInfo) => {
  await captureCase(page, testInfo, {
    id: 'TC-07', title: 'X-Service-Key: thiếu/sai bị từ chối, đúng được phép đọc',
    precondition: 'API invitation đang chạy trên test database.',
    input: 'GET danh sách khi thiếu key, key sai, key hợp lệ; POST ticket 403 khi thiếu key.',
    expected: '401, 401, 200, 401; request bị từ chối không tạo lời mời.',
  }, async () => {
    const before = await request.get(`${apiBase}/survey-invitations`, { headers: keyHeader(serviceKey) });
    const beforeList = await body(before);
    const missing = await request.get(`${apiBase}/survey-invitations`);
    const wrong = await request.get(`${apiBase}/survey-invitations`, { headers: keyHeader('invalid-key') });
    const valid = await request.get(`${apiBase}/survey-invitations`, { headers: keyHeader(serviceKey) });
    const unauthorizedCreate = await request.post(`${apiBase}/survey-invitations`, { data: { ticket_id: 403 } });
    const afterList = await body(await request.get(`${apiBase}/survey-invitations`, { headers: keyHeader(serviceKey) }));
    const before403 = beforeList.filter(item => item.ticket_id === 403).length;
    const after403 = afterList.filter(item => item.ticket_id === 403).length;
    expect(missing.status()).toBe(401);
    expect(wrong.status()).toBe(401);
    expect(valid.status()).toBe(200);
    expect(unauthorizedCreate.status()).toBe(401);
    expect(after403).toBe(before403);
    return `GET missing/wrong/valid=${missing.status()}/${wrong.status()}/${valid.status()}, POST missing key=${unauthorizedCreate.status()}, ticket 403 invitation count=${before403}→${after403}.`;
  });
});

test('TC-08 cancellation tombstone blocks reissue and sent invitation changes', async ({ page, request }, testInfo) => {
  await captureCase(page, testInfo, {
    id: 'TC-08', title: 'dấu hủy và khóa sửa/xóa lời mời đã gửi',
    precondition: 'Ticket 402 và 403 đã đóng, chưa có lời mời.',
    input: 'Tạo/xóa lời mời unsent của ticket 402 rồi tạo lại; gửi lời mời ticket 403 rồi PATCH/DELETE.',
    expected: 'Xóa unsent=204; phát hành lại=409 INVITATION_CANCELLED; PATCH/DELETE đã gửi=409.',
  }, async () => {
    const headers = keyHeader(serviceKey);
    const cancelled = await request.post(`${apiBase}/survey-invitations`, { headers, data: { ticket_id: 402 } });
    const cancelledId = (await body(cancelled)).invitation_id;
    const deleted = await request.delete(`${apiBase}/survey-invitations/${cancelledId}`, { headers });
    const reissue = await request.post(`${apiBase}/survey-invitations`, { headers, data: { ticket_id: 402 } });
    const reissueBody = await body(reissue);
    const sent = await request.post(`${apiBase}/survey-invitations`, { headers, data: { ticket_id: 403 } });
    const sentId = (await body(sent)).invitation_id;
    const send = await request.post(`${apiBase}/survey-invitations/${sentId}/send`, { headers, data: { channel: 'SMS' } });
    const patch = await request.patch(`${apiBase}/survey-invitations/${sentId}`, { headers, data: { channel: 'ZALO' } });
    const deleteSent = await request.delete(`${apiBase}/survey-invitations/${sentId}`, { headers });
    expect(cancelled.status()).toBe(201);
    expect(deleted.status()).toBe(204);
    expect(reissue.status()).toBe(409);
    expect(reissueBody.error.code).toBe('INVITATION_CANCELLED');
    expect(sent.status()).toBe(201);
    expect(send.status()).toBe(200);
    expect(patch.status()).toBe(409);
    expect(deleteSent.status()).toBe(409);
    return `ticket402 POST/DELETE/reissue=${cancelled.status()}/${deleted.status()}/${reissue.status()} ${reissueBody.error.code}; ticket403 POST/send/PATCH/DELETE=${sent.status()}/${send.status()}/${patch.status()}/${deleteSent.status()}.`;
  });
});

test('TC-09 Flyway rerun is a no-op at schema version 6', async ({ page }, testInfo) => {
  await captureCase(page, testInfo, {
    id: 'TC-09', title: 'Flyway lần hai không chạy lại migration',
    precondition: 'Backend đã khởi động hai lần trên cùng datadir sạch; log lần hai được lưu.',
    input: 'Đọc log startup lần hai và flyway_schema_history của smartcrm_l8_test.',
    expected: 'Log báo No migration necessary; đúng 6 migration thành công, từ V1 đến V6.',
  }, async () => {
    const logPath = process.env.QA_SECOND_STARTUP_LOG;
    if (!logPath) throw new Error('QA_SECOND_STARTUP_LOG is required');
    const startupBytes = readFileSync(logPath);
    const startup = startupBytes.includes(0)
      ? startupBytes.toString('utf16le').replace(/^\uFEFF/, '')
      : startupBytes.toString('utf8');
    const applied = database('SELECT COUNT(*) FROM flyway_schema_history WHERE success=1');
    const versions = database('SELECT GROUP_CONCAT(version ORDER BY installed_rank SEPARATOR ",") FROM flyway_schema_history WHERE success=1');
    expect(startup).toContain('No migration necessary');
    expect(applied).toBe('6');
    expect(versions).toBe('1,2,3,4,5,6');
    return `Startup lần hai có “No migration necessary”; history success=${applied}, versions=${versions}.`;
  });
});

test('TC-10 seed remains stable after second application startup and passwords are hashed', async ({ page }, testInfo) => {
  await captureCase(page, testInfo, {
    id: 'TC-10', title: 'seed không nhân đôi; mật khẩu được băm',
    precondition: 'Có số đếm seed trước lần khởi động thứ hai và DB vẫn là datadir test mới.',
    input: 'So sánh số đếm trước/sau; đọc số lượng seed và định dạng hash trong app_user.',
    expected: 'Seed counts không đổi (2 center, 2 customer, 2 technician, 4 ticket, 1 template, 4 user); password dùng BCrypt.',
  }, async () => {
    const beforePath = process.env.QA_SEED_COUNTS_BEFORE;
    if (!beforePath) throw new Error('QA_SEED_COUNTS_BEFORE is required');
    const before = readFileSync(beforePath, 'utf8').trim();
    const query = `SELECT CONCAT_WS('|',(SELECT COUNT(*) FROM service_center),(SELECT COUNT(*) FROM customer),(SELECT COUNT(*) FROM technician),(SELECT COUNT(*) FROM ticket),(SELECT COUNT(*) FROM survey_template),(SELECT COUNT(*) FROM app_user))`;
    const after = database(query);
    const passwordHash = database("SELECT IF(LEFT(password_hash,4) IN (CHAR(36,50,97,36),CHAR(36,50,98,36)),'BCRYPT','NOT_BCRYPT') FROM app_user WHERE username='marketing'");
    expect(before).toBe(after);
    expect(after).toBe('2|2|2|4|1|4');
    expect(passwordHash).toBe('BCRYPT');
    return `Seed counts before/after=${before}/${after}; marketing password storage=${passwordHash}.`;
  });
});

test('TC-11 duplicate invitation is rejected and SQL injection stays data', async ({ page, request }, testInfo) => {
  await captureCase(page, testInfo, {
    id: 'TC-11', title: 'idempotency và SQL injection',
    precondition: 'Ticket 401 có đúng một lời mời từ TC-03; role Marketing và Service Key hợp lệ.',
    input: 'Tạo lời mời ticket 401 lần hai; tạo template có chuỗi SQL injection trong name rồi GET lại.',
    expected: 'Tạo trùng trả 409, số lời mời vẫn 1; payload được lưu nguyên văn, không làm thay đổi tập dữ liệu khác.',
  }, async () => {
    const headers = keyHeader(serviceKey);
    const duplicate = await request.post(`${apiBase}/survey-invitations`, { headers, data: { ticket_id: 401 } });
    const invitationList = await body(await request.get(`${apiBase}/survey-invitations`, { headers }));
    const forTicket = invitationList.filter(item => item.ticket_id === 401).length;
    const token = await login(request, 'marketing');
    const injection = `TC11-' OR 1=1 -- <script>alert(1)</script>`;
    const created = await request.post(`${apiBase}/survey-templates`, {
      headers: bearer(token), data: { name: injection, title: 'Injection sample', intro_text: 'Stored as literal data', is_active: false },
    });
    const createdBody = await body(created);
    const detail = await request.get(`${apiBase}/survey-templates/${createdBody.template_id}`, { headers: bearer(token) });
    const detailBody = await body(detail);
    const deleted = await request.delete(`${apiBase}/survey-templates/${createdBody.template_id}`, { headers: bearer(token) });
    expect(duplicate.status()).toBe(409);
    expect(forTicket).toBe(1);
    expect(created.status()).toBe(201);
    expect(detail.status()).toBe(200);
    expect(detailBody.name).toBe(injection);
    expect(deleted.status()).toBe(204);
    return `Duplicate invitation=${duplicate.status()}, ticket401 rows=${forTicket}; injection template POST/GET/DELETE=${created.status()}/${detail.status()}/${deleted.status()}, name preserved literally.`;
  });
});
