import React, { useEffect, useState } from 'react';
import { currentMonth, decimal, formatDate, percent, queryString, request } from './api.js';

const roles = { QL_TRUNG_TAM: 'Quản lý trung tâm', MARKETING: 'Nhân viên Marketing', BAN_GIAM_DOC: 'Ban giám đốc' };
const routes = {
  QL_TRUNG_TAM: [['/thong-ke/ky-thuat-vien', 'CSAT theo kỹ thuật viên'], ['/phan-hoi', 'Danh sách phản hồi'], ['/thong-ke/ty-le', 'Tỉ lệ phản hồi']],
  MARKETING: [['/phan-hoi', 'Danh sách phản hồi']],
  BAN_GIAM_DOC: [['/thong-ke/trung-tam', 'CSAT theo trung tâm']],
};

function useResource(url, token) {
  const [state, setState] = useState({ loading: true });
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    if (!url) { setState({ loading: false }); return; }
    const controller = new AbortController();
    setState({ loading: true });
    request(url, { token, signal: controller.signal })
      .then(data => setState({ loading: false, data }))
      .catch(error => { if (error.name !== 'AbortError') setState({ loading: false, error }); });
    return () => controller.abort();
  }, [url, token, revision]);
  return { ...state, retry: () => setRevision(value => value + 1) };
}

function ErrorBox({ error, retry }) {
  if (!error) return null;
  let message = error.message;
  if (error.code === 'ALREADY_RESPONDED') message = `Bạn đã gửi phản hồi${error.fields.responded_at ? ` ngày ${formatDate(error.fields.responded_at)}` : ''}. Cảm ơn bạn!`;
  if (error.code === 'SURVEY_EXPIRED') message = 'Liên kết khảo sát đã hết hạn.';
  if (error.code === 'SURVEY_NOT_FOUND') message = 'Liên kết khảo sát không hợp lệ.';
  if (error.code === 'RATE_LIMITED') message = 'Thao tác quá nhanh, vui lòng thử lại sau 1 phút.';
  if (error.status === 401) message = 'Thông tin xác thực không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.';
  const terminal = ['ALREADY_RESPONDED', 'SURVEY_EXPIRED', 'SURVEY_NOT_FOUND'].includes(error.code);
  return <div className="notice error" role="alert"><p>{message}</p>
    {Object.entries(error.fields || {}).filter(([key]) => key !== 'responded_at').map(([key, value]) => <p key={key}>{String(value)}</p>)}
    {retry && !terminal && <button className="secondary" onClick={retry}>Thử lại</button>}
  </div>;
}

function Loading() { return <p className="notice" role="status">Đang tải dữ liệu…</p>; }
function Empty({ children = 'Chưa có dữ liệu trong khoảng thời gian đã chọn.' }) { return <p className="empty" role="status">{children}</p>; }

function Survey({ token }) {
  const resource = useResource(`/api/surveys/${encodeURIComponent(token)}`);
  const [score, setScore] = useState('');
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);
  const [validation, setValidation] = useState('');
  async function submit(event) {
    event.preventDefault();
    if (submitting) return;
    if (!score) { setValidation('Vui lòng chọn điểm từ 1 đến 5.'); return; }
    setValidation(''); setError(null); setSubmitting(true);
    try { setResult(await request(`/api/surveys/${encodeURIComponent(token)}/responses`, { method: 'POST', body: { score: Number(score), comment } })); }
    catch (failure) { setError(failure); }
    finally { setSubmitting(false); }
  }
  if (result) return <section className="panel survey-panel"><div className="panel-title">CẢM ƠN BẠN ĐÃ PHẢN HỒI</div><div className="panel-body">
    <div className="success" role="status"><h1>Đánh giá đã được ghi nhận</h1><p>Phiếu {result.ticket_code} · {result.score}/5 điểm</p><p>{result.comment}</p><p>{formatDate(result.responded_at)}</p></div>
    <p className="muted">Mỗi phiếu bảo hành được gửi một phản hồi. Phản hồi đã gửi không thể chỉnh sửa hoặc xóa.</p>
  </div></section>;
  const closed = error && ['ALREADY_RESPONDED', 'SURVEY_EXPIRED', 'SURVEY_NOT_FOUND'].includes(error.code);
  return <section className="panel survey-panel"><div className="panel-title">01 &nbsp; BIỂU MẪU CSAT</div><div className="panel-body">
    <p className="eyebrow">Khách hàng · Đánh giá sau bảo hành</p><h1>{resource.data?.survey_title || 'Trải nghiệm của bạn thế nào?'}</h1>
    {resource.data?.survey_intro && <p>{resource.data.survey_intro}</p>}
    {resource.loading && <Loading />}
    <ErrorBox error={resource.error} retry={resource.retry} />
    {resource.data && <><dl className="ticket-details"><div><dt>Mã phiếu</dt><dd>{resource.data.ticket_code}</dd></div><div><dt>Trung tâm</dt><dd>{resource.data.center_name}</dd></div><div><dt>Thiết bị</dt><dd>{resource.data.device_name || 'Chưa có thông tin thiết bị'}</dd></div><div><dt>Ngày đóng</dt><dd>{formatDate(resource.data.closed_at)}</dd></div></dl>
      <ErrorBox error={error} />
      {!closed && <form onSubmit={submit}>
        <fieldset className="scores" disabled={submitting}><legend>Điểm hài lòng <span aria-hidden="true">*</span></legend>
          <div className="score-options">{[1, 2, 3, 4, 5].map(value => <label key={value} className={Number(score) === value ? 'chosen' : ''}><input type="radio" name="score" value={value} checked={Number(score) === value} onChange={event => setScore(event.target.value)} aria-label={`${value} điểm`} /><span>{value}</span></label>)}</div>
          <div className="score-hint"><span>Rất không hài lòng</span><span>Rất hài lòng</span></div>
          {validation && <p className="field-error" role="alert">{validation}</p>}
        </fieldset>
        <label className="comment-label" htmlFor="comment">Nhận xét <span className="muted">· Không bắt buộc</span></label>
        <textarea id="comment" value={comment} onChange={event => setComment(event.target.value)} maxLength={1000} rows={5} placeholder="Chia sẻ trải nghiệm của bạn…" disabled={submitting} />
        <p className="counter">{comment.length}/1.000 ký tự</p>
        <button className="primary full" type="submit" disabled={submitting}>{submitting ? 'Đang gửi…' : 'GỬI PHẢN HỒI'}</button>
      </form>}
      <p className="muted small">Liên kết hết hạn: {formatDate(resource.data.expires_at)}</p><p className="small">1 phiếu → tối đa 1 phản hồi. Phản hồi đã gửi không thể chỉnh sửa hoặc xóa.</p>
    </>}
  </div></section>;
}

function Login({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError(null);
    try { onLogin(await request('/api/auth/login', { method: 'POST', body: { username, password } })); }
    catch (failure) { setError(failure); }
    finally { setBusy(false); }
  }
  return <section className="panel login-panel"><div className="panel-title">SMART CRM · MEKONG MOBILE</div><div className="panel-body"><p className="eyebrow">Không gian làm việc nội bộ</p><h1>Đăng nhập</h1><p className="muted">Theo dõi sự hài lòng sau bảo hành trong phạm vi được phân quyền.</p>
    <ErrorBox error={error} /><form onSubmit={submit}><label htmlFor="username">Tên đăng nhập</label><input id="username" autoComplete="username" value={username} onChange={event => setUsername(event.target.value)} required disabled={busy} />
      <label htmlFor="password">Mật khẩu</label><input id="password" type="password" autoComplete="current-password" value={password} onChange={event => setPassword(event.target.value)} required disabled={busy} />
      <button className="primary full" disabled={busy}>{busy ? 'Đang đăng nhập…' : 'Đăng nhập'}</button></form>
  </div></section>;
}

function Filters({ auth, draft, setDraft, apply, responseFilters = false }) {
  const centers = useResource('/api/reference/centers', auth.access_token);
  const technicians = useResource(responseFilters ? `/api/reference/technicians?${queryString({ center_id: draft.center_id })}` : null, auth.access_token);
  return <form className="filters" onSubmit={event => { event.preventDefault(); apply(); }}>
    <label>Trung tâm<select aria-label="Trung tâm" value={draft.center_id} onChange={event => setDraft({ ...draft, center_id: event.target.value, technician_id: '' })} disabled={auth.role === 'QL_TRUNG_TAM' || centers.loading}>
      {auth.role !== 'QL_TRUNG_TAM' && <option value="">Tất cả trung tâm</option>}
      {(centers.data?.items || []).map(center => <option key={center.center_id} value={center.center_id}>{center.center_name}</option>)}
    </select></label>
    <label>Từ ngày<input type="date" aria-label="Từ ngày" value={draft.from} onChange={event => setDraft({ ...draft, from: event.target.value })} required /></label>
    <label>Đến ngày<input type="date" aria-label="Đến ngày" value={draft.to} onChange={event => setDraft({ ...draft, to: event.target.value })} required /></label>
    {responseFilters && <><label>Kỹ thuật viên<select aria-label="Kỹ thuật viên" value={draft.technician_id} onChange={event => setDraft({ ...draft, technician_id: event.target.value })} disabled={technicians.loading}><option value="">Tất cả kỹ thuật viên</option>{(technicians.data?.items || []).map(item => <option key={item.technician_id} value={item.technician_id}>{item.technician_name}</option>)}</select></label>
      <label>Điểm từ<select aria-label="Điểm từ" value={draft.score_min} onChange={event => setDraft({ ...draft, score_min: event.target.value })}><option value="">1</option>{[2, 3, 4, 5].map(value => <option key={value}>{value}</option>)}</select></label>
      <label>Điểm đến<select aria-label="Điểm đến" value={draft.score_max} onChange={event => setDraft({ ...draft, score_max: event.target.value })}><option value="">5</option>{[1, 2, 3, 4].map(value => <option key={value}>{value}</option>)}</select></label></>}
    <button className="primary filter-button">Áp dụng</button>
    <div className="filter-messages"><ErrorBox error={centers.error} retry={centers.retry} /><ErrorBox error={technicians.error} retry={technicians.retry} /></div>
  </form>;
}

function useFilters(auth, extra = {}) {
  const [draft, setDraft] = useState({ ...currentMonth(), center_id: auth.role === 'QL_TRUNG_TAM' ? String(auth.center_id) : '', ...extra });
  const [active, setActive] = useState(draft);
  const [error, setError] = useState(null);
  function apply(after) {
    const duration = (new Date(draft.to) - new Date(draft.from)) / 86400000;
    if (duration < 0 || duration > 365 || !Number.isFinite(duration)) { setError(new Error('Khoảng ngày không hợp lệ: từ ngày không được sau đến ngày, tối đa 366 ngày.')); return; }
    if (Number(draft.score_min || 1) > Number(draft.score_max || 5)) { setError(new Error('Điểm từ không được lớn hơn điểm đến.')); return; }
    setError(null); setActive({ ...draft }); after?.();
  }
  return { draft, setDraft, active, error, apply };
}

function Responses({ auth }) {
  const filters = useFilters(auth, { technician_id: '', score_min: '', score_max: '' });
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState(null);
  const [exported, setExported] = useState(false);
  const resource = useResource(`/api/survey-responses?${queryString({ ...filters.active, page, size })}`, auth.access_token);
  async function exportFile() {
    setExporting(true); setExportError(null); setExported(false);
    try {
      const blob = await request(`/api/survey-responses/export?${queryString(filters.active)}`, { token: auth.access_token, download: true });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a'); link.href = url; link.download = `phan-hoi-khao-sat_${filters.active.from}_${filters.active.to}.xlsx`;
      document.body.appendChild(link); link.click(); link.remove(); URL.revokeObjectURL(url); setExported(true);
    } catch (failure) { setExportError(failure); }
    finally { setExporting(false); }
  }
  return <section className="panel"><div className="panel-title">02 &nbsp; DANH SÁCH PHẢN HỒI</div><div className="panel-body"><p className="eyebrow">{roles[auth.role]} · Tra cứu phản hồi sau bảo hành</p><h1>Lắng nghe từng trải nghiệm</h1>
    <Filters auth={auth} {...filters} apply={() => filters.apply(() => { setPage(1); setExported(false); })} responseFilters />
    <ErrorBox error={filters.error} />{resource.loading && <Loading />}<ErrorBox error={resource.error} retry={resource.retry} />
    {resource.data && <>{resource.data.items.length === 0 ? <Empty /> : <div className="table-scroll"><table><caption className="sr-only">Danh sách phản hồi khảo sát</caption><thead><tr><th>Mã phiếu</th><th>Trung tâm</th><th>Tên KTV</th><th>Điểm</th><th>Nhận xét</th><th>SĐT che</th><th>Ngày trả lời</th></tr></thead><tbody>{resource.data.items.map(row => <tr key={row.response_id}><td className="ticket-code">{row.ticket_code}</td><td>{row.center_name}</td><td>{row.technician_name || '—'}</td><td><span className={`score-badge ${row.score <= 2 ? 'low' : ''}`}>{row.score}</span></td><td className="comment-cell">{row.comment || '—'}</td><td className="nowrap">{row.customer_phone}</td><td className="nowrap">{formatDate(row.responded_at)}</td></tr>)}</tbody></table></div>}
      <div className="pagination"><span>{resource.data.total} phản hồi · Trang {page}/{Math.max(1, Math.ceil(resource.data.total / size))}</span><label>Dòng/trang<select value={size} onChange={event => { setSize(Number(event.target.value)); setPage(1); }}>{[20, 50, 100].map(value => <option key={value}>{value}</option>)}</select></label><button className="secondary" disabled={page === 1} onClick={() => setPage(page - 1)}>Trước</button><button className="secondary" disabled={page * size >= resource.data.total} onClick={() => setPage(page + 1)}>Sau</button></div>
    </>}
    {auth.role === 'MARKETING' && <div className="export-row"><span className="muted small">Xuất tối đa 10.000 phản hồi theo bộ lọc đã áp dụng.</span><button className="primary" onClick={exportFile} disabled={exporting || resource.loading || !!resource.error}>{exporting ? 'Đang xuất…' : 'Xuất Excel'}</button></div>}
    <ErrorBox error={exportError} retry={exportFile} />{exported && <p className="success" role="status">Đã tải file Excel theo bộ lọc đã áp dụng.</p>}
    <p className="muted small">Số điện thoại được che. Phản hồi đã gửi không thể chỉnh sửa hoặc xóa.</p>
  </div></section>;
}

function Statistics({ auth, kind }) {
  const filters = useFilters(auth);
  const endpoint = { technician: 'by-technician', center: 'by-center', rate: 'response-rate' }[kind];
  const resource = useResource(`/api/survey-stats/${endpoint}?${queryString({ ...filters.active, ...(kind === 'center' ? { group_by: 'month' } : {}) })}`, auth.access_token);
  const titles = { technician: '03 · CSAT THEO KỸ THUẬT VIÊN', center: 'CSAT THEO TRUNG TÂM', rate: 'TỈ LỆ PHẢN HỒI' };
  const items = resource.data?.items || [];
  return <section className="panel"><div className="panel-title">{titles[kind]}</div><div className="panel-body"><p className="eyebrow">{roles[auth.role]} · Thống kê chất lượng dịch vụ</p><h1>{kind === 'technician' ? 'Chất lượng từ đội ngũ của bạn' : kind === 'center' ? 'Bức tranh hài lòng toàn công ty' : 'Mức độ tham gia khảo sát'}</h1>
    <Filters auth={auth} {...filters} apply={() => filters.apply()} /><ErrorBox error={filters.error} />
    {resource.loading && <Loading />}<ErrorBox error={resource.error} retry={resource.retry} />
    {resource.data && (items.length === 0 ? <Empty /> : <>
      {kind === 'center' && <CenterTrend items={items} />}
      <div className="table-scroll"><table><caption className="sr-only">{titles[kind]}</caption><thead><tr>
        <th>{kind === 'technician' ? 'Tên KTV' : 'Trung tâm'}</th>{kind !== 'technician' && <th>Tháng</th>}{kind !== 'center' && <th>Đã gửi</th>}<th>Phản hồi</th>{kind !== 'rate' ? <><th>CSAT TB</th><th>Hài lòng</th>{kind === 'center' && <th>Phân bố điểm 1 → 5</th>}</> : <th>Tỉ lệ phản hồi</th>}
      </tr></thead><tbody>{items.map(row => <tr key={`${row.technician_id || row.center_id}-${row.month || ''}`}><td>{kind === 'technician' ? row.technician_name : row.center_name || `Trung tâm ${row.center_id}`}</td>{kind !== 'technician' && <td>{row.month}</td>}{kind !== 'center' && <td>{row.invitations_sent}</td>}<td>{row.responses}</td>{kind !== 'rate' ? <><td className="metric">{decimal(row.csat_avg)}</td><td>{percent(row.satisfied_rate)}</td>{kind === 'center' && <td><div className="distribution">{[1, 2, 3, 4, 5].map(score => <span key={score} title={`${score} điểm: ${row.distribution?.[score] || 0} phản hồi`}>{score}: <b>{row.distribution?.[score] || 0}</b></span>)}</div></td>}</> : <td className="metric">{percent(row.response_rate)}</td>}</tr>)}</tbody></table></div>
    </>)}
    <p className="muted formula">{kind === 'rate' ? 'Tỉ lệ phản hồi = số phản hồi ÷ số lời mời gửi thành công. Không tính lời mời gửi lỗi.' : 'CSAT = điểm trung bình · Tỉ lệ hài lòng = số phản hồi ≥ 4 điểm ÷ tổng phản hồi · “—” khi chưa có dữ liệu.'}</p>
    <button className="secondary" onClick={resource.retry} disabled={resource.loading}>Làm mới số liệu</button>
  </div></section>;
}

function CenterTrend({ items }) {
  const groups = Object.groupBy(items, item => item.center_id);
  return <div className="trends">{Object.entries(groups).map(([id, rows]) => {
    const sorted = [...rows].sort((a, b) => a.month.localeCompare(b.month));
    return <section key={id} className="trend-card"><h2>{sorted[0].center_name}</h2><p className="muted small">Xu hướng CSAT theo tháng</p><div className="month-trend">{sorted.map((row, index) => {
      const previous = sorted[index - 1]?.csat_avg;
      const difference = previous == null || row.csat_avg == null ? null : Number(row.csat_avg) - Number(previous);
      return <div key={row.month}><span>{row.month}</span><strong>{decimal(row.csat_avg)}</strong><small>{difference == null ? '—' : `${difference >= 0 ? '+' : ''}${decimal(difference)} so với mốc trước`}</small></div>;
    })}</div></section>;
  })}</div>;
}

function readSession() {
  try { return JSON.parse(sessionStorage.getItem('smartcrm-session') || 'null'); }
  catch { return null; }
}

export default function App() {
  const [path, setPath] = useState(window.location.pathname);
  const [auth, setAuth] = useState(readSession);
  useEffect(() => { const update = () => setPath(window.location.pathname); window.addEventListener('popstate', update); return () => window.removeEventListener('popstate', update); }, []);
  function navigate(next) { window.history.pushState({}, '', next); setPath(next); }
  function login(session) { sessionStorage.setItem('smartcrm-session', JSON.stringify(session)); setAuth(session); navigate(routes[session.role]?.[0][0] || '/'); }
  function logout() { sessionStorage.removeItem('smartcrm-session'); setAuth(null); navigate('/'); }
  const survey = path.match(/^\/khao-sat\/([^/]+)\/?$/);
  const allowed = auth && routes[auth.role]?.some(([url]) => url === path);
  const activePath = path === '/' && auth ? routes[auth.role]?.[0][0] : path;
  return <><header className="topbar"><a className="brand" href="/" onClick={event => { event.preventDefault(); navigate('/'); }}><span className="brand-mark">M</span><span>MEKONG <b>MOBILE</b><small>SMART CRM · KHẢO SÁT HÀI LÒNG</small></span></a>{auth && !survey && <div className="account"><span>{auth.username}<small>{roles[auth.role]}</small></span><button className="logout" onClick={logout}>Đăng xuất</button></div>}</header>
    {auth && !survey && <nav aria-label="Điều hướng chính">{(routes[auth.role] || []).map(([url, label]) => <a key={url} href={url} aria-current={activePath === url ? 'page' : undefined} onClick={event => { if (!event.ctrlKey && !event.metaKey) { event.preventDefault(); navigate(url); } }}>{label}</a>)}</nav>}
    <main className={survey || !auth ? 'compact-main' : ''}>
      {survey ? <Survey token={survey[1]} /> : !auth ? <Login onLogin={login} /> : !allowed && path !== '/' ? <section className="panel"><div className="panel-body"><h1>Không có quyền truy cập</h1><p>Chọn chức năng được phân quyền trên thanh điều hướng.</p></div></section> : activePath === '/phan-hoi' ? <Responses auth={auth} /> : <Statistics key={activePath} auth={auth} kind={activePath === '/thong-ke/trung-tam' ? 'center' : activePath === '/thong-ke/ty-le' ? 'rate' : 'technician'} />}
    </main><footer>Mekong Mobile · Chất lượng dịch vụ bắt đầu từ việc lắng nghe.</footer></>;
}
