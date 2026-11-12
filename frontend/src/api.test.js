import { afterEach, describe, expect, it, vi } from 'vitest';
import { currentMonth, decimal, percent, queryString, request } from './api.js';

afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers(); });

describe('API contract client', () => {
  it('omits empty filters and safely encodes values', () => {
    expect(queryString({ center_id: '', score_max: 2, from: '2026-11-01', query: 'a&b', absent: null })).toBe('score_max=2&from=2026-11-01&query=a%26b');
  });
  it('uses Vietnam calendar month, including leap-year boundary', () => {
    vi.useFakeTimers(); vi.setSystemTime(new Date('2024-02-01T00:00:00+07:00'));
    expect(currentMonth()).toEqual({ from: '2024-02-01', to: '2024-02-29' });
  });
  it('preserves null statistics as a dash', () => {
    expect(decimal(null)).toBe('—'); expect(percent(null)).toBe('—');
    expect(decimal(4.5)).toBe('4,50'); expect(percent(.92)).toBe('92%');
  });
  it('sends only bearer auth for internal user APIs', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ items: [] }) }); vi.stubGlobal('fetch', fetch);
    await request('/api/survey-responses', { token: 'jwt' });
    expect(fetch.mock.calls[0][1].headers).toEqual({ Authorization: 'Bearer jwt' });
  });
  it('preserves structured validation error fields', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 400, json: async () => ({ error: { code: 'VALIDATION_FAILED', message: 'Sai điểm', fields: { score: '1–5' } } }) }));
    await expect(request('/api/surveys/token/responses')).rejects.toMatchObject({ status: 400, code: 'VALIDATION_FAILED', fields: { score: '1–5' } });
  });
  it('returns an actionable network error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    await expect(request('/api/surveys/token')).rejects.toThrow('Không kết nối được máy chủ');
  });
  it('keeps cancellation distinguishable from a connection failure', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new DOMException('Aborted', 'AbortError')));
    await expect(request('/api/surveys/token')).rejects.toMatchObject({ name: 'AbortError' });
  });
  it('reads export responses as a binary blob', async () => {
    const blob = new Blob(['xlsx']); vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, blob: async () => blob }));
    expect(await request('/api/survey-responses/export', { download: true })).toBe(blob);
  });
});
