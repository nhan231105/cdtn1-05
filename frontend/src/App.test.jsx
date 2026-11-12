import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App.jsx';

const form = { ticket_code: 'TEST-L8', center_name: 'Trung tâm thử nghiệm', device_name: 'Thiết bị thử nghiệm', closed_at: '2026-11-01T09:00:00+07:00', expires_at: '2026-11-08T09:00:00+07:00' };
const json = (data, status = 200) => ({ ok: status < 400, status, json: async () => data });
afterEach(() => vi.unstubAllGlobals());
const surveyPath = () => window.history.replaceState({}, '', `/khao-sat/${'a'.repeat(43)}`);

describe('UC03 survey', () => {
  it('renders the title and introduction captured from the invitation API', async () => {
    surveyPath(); vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json({ ...form, survey_title: 'Khảo sát mới', survey_intro: 'Xin hãy cho biết ý kiến.' })));
    render(<App />);
    expect(await screen.findByRole('heading', { name: 'Khảo sát mới' })).toBeTruthy();
    expect(screen.getByText('Xin hãy cho biết ý kiến.')).toBeTruthy();
  });

  it('loads ticket details, requires score and submits exactly one response', async () => {
    surveyPath();
    let complete;
    const fetch = vi.fn().mockResolvedValueOnce(json(form)).mockImplementationOnce(() => new Promise(resolve => { complete = resolve; })); vi.stubGlobal('fetch', fetch);
    const user = userEvent.setup(); render(<App />);
    await screen.findByText('TEST-L8');
    await user.click(screen.getByRole('button', { name: 'GỬI PHẢN HỒI' }));
    expect(screen.getByRole('alert').textContent).toContain('Vui lòng chọn điểm'); expect(fetch).toHaveBeenCalledTimes(1);
    await user.click(screen.getByRole('radio', { name: '5 điểm' }));
    await user.type(screen.getByLabelText(/Nhận xét/), 'Dịch vụ tốt');
    await user.click(screen.getByRole('button', { name: 'GỬI PHẢN HỒI' }));
    expect(screen.getByRole('button', { name: 'Đang gửi…' }).disabled).toBe(true);
    expect(JSON.parse(fetch.mock.calls[1][1].body)).toEqual({ score: 5, comment: 'Dịch vụ tốt' });
    complete(json({ ticket_code: 'TEST-L8', score: 5, comment: 'Dịch vụ tốt', responded_at: '2026-11-03T09:00:00+07:00' }, 201));
    await screen.findByText('Đánh giá đã được ghi nhận'); expect(screen.queryByRole('button', { name: 'GỬI PHẢN HỒI' })).toBeNull();
  });
  it('retains score and comment after database failure and allows resubmission', async () => {
    surveyPath();
    const fetch = vi.fn().mockResolvedValueOnce(json(form)).mockResolvedValueOnce(json({ error: { code: 'SERVICE_UNAVAILABLE', message: 'Hệ thống tạm thời gặp sự cố, vui lòng thử lại' } }, 503)).mockResolvedValueOnce(json({ ...form, score: 4, responded_at: '2026-11-03T09:00:00+07:00' }, 201));
    vi.stubGlobal('fetch', fetch); const user = userEvent.setup(); render(<App />); await screen.findByText('TEST-L8');
    await user.click(screen.getByRole('radio', { name: '4 điểm' })); await user.type(screen.getByLabelText(/Nhận xét/), 'Giữ nhận xét'); await user.click(screen.getByRole('button', { name: 'GỬI PHẢN HỒI' }));
    await screen.findByRole('alert'); expect(screen.getByLabelText(/Nhận xét/).value).toBe('Giữ nhận xét'); expect(screen.getByRole('radio', { name: '4 điểm' }).checked).toBe(true);
    await user.click(screen.getByRole('button', { name: 'GỬI PHẢN HỒI' })); await screen.findByText('Đánh giá đã được ghi nhận');
  });
  it.each([['SURVEY_EXPIRED', 410, 'Liên kết khảo sát đã hết hạn.'], ['ALREADY_RESPONDED', 409, 'Bạn đã gửi phản hồi. Cảm ơn bạn!'], ['SURVEY_NOT_FOUND', 404, 'Liên kết khảo sát không hợp lệ.']])('handles terminal %s without a submit action', async (code, status, message) => {
    surveyPath(); vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json({ error: { code } }, status))); render(<App />);
    await screen.findByText(message); expect(screen.queryByRole('button', { name: 'GỬI PHẢN HỒI' })).toBeNull(); expect(screen.queryByRole('button', { name: 'Thử lại' })).toBeNull();
  });
});

describe('authenticated screens', () => {
  it('logs in with entered credentials and navigates only to allowed role links', async () => {
    vi.stubGlobal('fetch', vi.fn(async url => url === '/api/auth/login' ? json({ username: 'marketing', access_token: 'jwt', role: 'MARKETING', center_id: null }) : json({ items: [], total: 0 })));
    const user = userEvent.setup(); render(<App />); await user.type(screen.getByLabelText('Tên đăng nhập'), 'marketing'); await user.type(screen.getByLabelText('Mật khẩu'), 'test-password-only'); await user.click(screen.getByRole('button', { name: 'Đăng nhập' }));
    await screen.findByRole('heading', { name: 'Lắng nghe từng trải nghiệm' }); expect(window.location.pathname).toBe('/phan-hoi'); expect(screen.queryByRole('link', { name: 'CSAT theo kỹ thuật viên' })).toBeNull();
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({ username: 'marketing', password: 'test-password-only' });
    await user.click(screen.getByRole('button', { name: 'Đăng xuất' })); expect(sessionStorage.getItem('smartcrm-session')).toBeNull(); await screen.findByRole('heading', { name: 'Đăng nhập' });
  });
  it('uses applied filters and pagination while showing masked backend phone values', async () => {
    sessionStorage.setItem('smartcrm-session', JSON.stringify({ username: 'manager', access_token: 'jwt', role: 'QL_TRUNG_TAM', center_id: 101 })); window.history.replaceState({}, '', '/phan-hoi');
    const fetch = vi.fn(async url => url.startsWith('/api/reference/centers') ? json({ items: [{ center_id: 101, center_name: 'Trung tâm A' }] }) : url.startsWith('/api/reference/technicians') ? json({ items: [] }) : json({ items: [{ response_id: 1, ticket_code: 'TEST-L8', score: 2, customer_phone: '090****567', comment: '<script>alert(1)</script>', responded_at: '2026-11-03T09:00:00+07:00' }], total: 22 })); vi.stubGlobal('fetch', fetch);
    const user = userEvent.setup(); render(<App />); await screen.findByText('090****567'); expect(screen.getByLabelText('Trung tâm').disabled).toBe(true); expect(screen.queryByRole('button', { name: 'Xuất Excel' })).toBeNull();
    expect(document.querySelector('script')).toBeNull(); await user.selectOptions(screen.getByLabelText('Điểm đến'), '2'); await user.click(screen.getByRole('button', { name: 'Áp dụng' }));
    await waitFor(() => expect(fetch.mock.calls.some(([url]) => url.includes('score_max=2') && url.includes('page=1'))).toBe(true)); await screen.findByText('090****567');
    await user.click(screen.getByRole('button', { name: 'Sau' })); await waitFor(() => expect(fetch.mock.calls.some(([url]) => url.includes('page=2') && url.includes('score_max=2'))).toBe(true));
  });
  it('rejects inconsistent filters locally without issuing a data request', async () => {
    sessionStorage.setItem('smartcrm-session', JSON.stringify({ username: 'manager', access_token: 'jwt', role: 'QL_TRUNG_TAM', center_id: 101 })); window.history.replaceState({}, '', '/phan-hoi');
    const fetch = vi.fn(async () => json({ items: [], total: 0 })); vi.stubGlobal('fetch', fetch); const user = userEvent.setup(); render(<App />);
    await screen.findByText('Chưa có dữ liệu trong khoảng thời gian đã chọn.'); const before = fetch.mock.calls.length;
    await user.selectOptions(screen.getByLabelText('Điểm từ'), '5'); await user.selectOptions(screen.getByLabelText('Điểm đến'), '2'); await user.click(screen.getByRole('button', { name: 'Áp dụng' })); await screen.findByText('Điểm từ không được lớn hơn điểm đến.'); expect(fetch.mock.calls.length).toBe(before);
  });
  it('does not request unauthorized pages for a logged-in user', async () => {
    sessionStorage.setItem('smartcrm-session', JSON.stringify({ username: 'marketing', access_token: 'jwt', role: 'MARKETING' })); window.history.replaceState({}, '', '/thong-ke/trung-tam'); const fetch = vi.fn(); vi.stubGlobal('fetch', fetch); render(<App />);
    expect(screen.getByRole('heading', { name: 'Không có quyền truy cập' })).toBeTruthy(); expect(fetch).not.toHaveBeenCalled();
  });
});
