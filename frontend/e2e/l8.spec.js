import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

// This suite sends a real response. Run only against a disposable MySQL test schema.
const token = process.env.E2E_SURVEY_TOKEN;
const password = process.env.E2E_PASSWORD;
const ticketCode = process.env.E2E_TICKET_CODE || 'DEMO-L8-0001';
if (!token || !password) throw new Error('E2E_SURVEY_TOKEN (fresh invitation in disposable database) and E2E_PASSWORD are required. See frontend/README.md.');

async function login(page, username) {
  await page.goto('/');
  await page.getByLabel('Tên đăng nhập').fill(username);
  await page.getByLabel('Mật khẩu').fill(password);
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Đăng xuất' })).toBeVisible();
}

test('TC-12 real customer response, XSS sanitization, scoped statistics, lookup and Excel', async ({ page }, testInfo) => {
  const dialogs = [];
  page.on('dialog', async dialog => { dialogs.push(dialog.message()); await dialog.dismiss(); });
  const xssComment = 'E2E XSS <script>alert(1)</script> intact';
  const sanitizedComment = 'E2E XSS alert(1) intact';
  await page.setViewportSize({ width: 360, height: 800 });
  await page.goto(`/khao-sat/${token}`);
  await expect(page.getByText(ticketCode, { exact: true })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.screenshot({ path: testInfo.outputPath('01-survey-mobile.png'), fullPage: true });
  await page.getByRole('radio', { name: '5 điểm' }).check();
  await page.getByLabel(/Nhận xét/).fill(xssComment);
  await page.getByRole('button', { name: 'GỬI PHẢN HỒI' }).click();
  await expect(page.getByRole('heading', { name: 'Đánh giá đã được ghi nhận' })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('alert')).toContainText('Bạn đã gửi phản hồi');
  await expect(page.getByRole('button', { name: 'GỬI PHẢN HỒI' })).toHaveCount(0);

  await page.setViewportSize({ width: 1440, height: 1000 });
  await login(page, process.env.E2E_MANAGER_USERNAME || 'manager101');
  await expect(page.getByRole('heading', { name: 'Chất lượng từ đội ngũ của bạn' })).toBeVisible();
  await expect(page.getByRole('table')).toBeVisible();
  await expect(page.getByRole('alert')).toHaveCount(0);
  await page.screenshot({ path: testInfo.outputPath('03-technician-stats.png'), fullPage: true });
  await page.getByRole('link', { name: 'Danh sách phản hồi' }).click();
  const row = page.getByRole('row').filter({ hasText: ticketCode });
  await expect(row).toContainText(sanitizedComment);
  expect(dialogs).toEqual([]);
  await expect(row).toContainText(/\d{3}\*+\d{3}/);
  await expect(page.getByLabel('Trung tâm')).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Xuất Excel' })).toHaveCount(0);
  await page.screenshot({ path: testInfo.outputPath('02-response-list.png'), fullPage: true });
  const evidencePath = resolve(process.cwd(), '..', 'docs', 'screenshots', 'TC-12.png');
  mkdirSync(resolve(evidencePath, '..'), { recursive: true });
  await page.screenshot({ path: evidencePath, fullPage: true });
  await page.getByRole('link', { name: 'Tỉ lệ phản hồi', exact: true }).click();
  await expect(page.getByRole('table')).toBeVisible();
  await expect(page.getByRole('alert')).toHaveCount(0);
  await page.getByRole('button', { name: 'Đăng xuất' }).click();

  await login(page, process.env.E2E_MARKETING_USERNAME || 'marketing');
  await expect(page.getByRole('row').filter({ hasText: ticketCode })).toBeVisible();
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Xuất Excel' }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
  expect(await download.failure()).toBeNull();
  await expect(page.getByRole('status')).toContainText('Đã tải file Excel');
  await page.getByRole('button', { name: 'Đăng xuất' }).click();

  await login(page, process.env.E2E_DIRECTOR_USERNAME || 'director');
  await expect(page.getByRole('heading', { name: 'Bức tranh hài lòng toàn công ty' })).toBeVisible();
  await expect(page.getByRole('columnheader', { name: 'Phân bố điểm 1 → 5' })).toBeVisible();
  await expect(page.getByRole('table')).toBeVisible();
  await expect(page.getByRole('alert')).toHaveCount(0);
  await page.screenshot({ path: testInfo.outputPath('04-center-stats.png'), fullPage: true });

  const session = await page.evaluate(() => JSON.parse(sessionStorage.getItem('smartcrm-session')));
  const forbidden = await page.request.get('/api/survey-responses', { headers: { Authorization: `Bearer ${session.access_token}` } });
  expect(forbidden.status()).toBe(403);
  expect((await forbidden.json()).error.code).toBe('FORBIDDEN_SCOPE');
});
