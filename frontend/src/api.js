export class ApiError extends Error {
  constructor(status, error = {}) {
    super(error.message || `Yêu cầu không thành công (${status})`);
    this.status = status;
    this.code = error.code;
    this.fields = error.fields || {};
  }
}

export function queryString(values) {
  return new URLSearchParams(Object.entries(values).filter(([, value]) => value !== '' && value != null)).toString();
}

export async function request(path, { token, body, method = 'GET', signal, download = false } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  let response;
  try {
    response = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body), signal });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new Error('Không kết nối được máy chủ. Vui lòng kiểm tra kết nối và thử lại.');
  }
  if (!response.ok) {
    const payload = await response.json().catch(() => ({}));
    throw new ApiError(response.status, payload.error);
  }
  if (download) return response.blob();
  return response.json();
}

export function currentMonth() {
  const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
  const [year, month] = today.split('-').map(Number);
  const last = new Date(year, month, 0).getDate();
  return { from: `${year}-${String(month).padStart(2, '0')}-01`, to: `${year}-${String(month).padStart(2, '0')}-${last}` };
}

export const formatDate = value => value ? new Intl.DateTimeFormat('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh', dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '—';
export const decimal = value => value == null ? '—' : Number(value).toLocaleString('vi-VN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
export const percent = value => value == null ? '—' : `${Math.round(Number(value) * 100)}%`;
