'use strict';

const STATUS_LABELS = {
  pending: 'Pending review',
  scheduled: 'Pickup scheduled',
  picked_up: 'Picked up',
  completed: 'Completed',
  cancelled: 'Cancelled',
  rejected: 'Rejected',
};
const SETTABLE = ['pending', 'scheduled', 'picked_up', 'completed', 'rejected'];
const FINAL = ['completed', 'cancelled', 'rejected'];
const NEXT_STEP = { pending: 'scheduled', scheduled: 'picked_up', picked_up: 'completed' };

const $ = (sel) => document.querySelector(sel);
const state = { token: null, filter: 'all', requests: [], editing: null };

function storage(fn) {
  try { return fn(window.localStorage); } catch { return null; }
}

async function api(path, options = {}) {
  const headers = { 'content-type': 'application/json' };
  if (state.token) headers.authorization = `Bearer ${state.token}`;
  const res = await fetch(path, { ...options, headers });
  const body = await res.json().catch(() => ({}));
  if (res.status === 401) {
    signOut();
    throw new Error(body.error || 'Session expired');
  }
  if (!res.ok) throw new Error(body.error || `Request failed (${res.status})`);
  return body;
}

function el(tag, attrs = {}, ...children) {
  const node = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (k === 'class') node.className = v;
    else if (k.startsWith('on')) node.addEventListener(k.slice(2), v);
    else if (v !== undefined && v !== null) node.setAttribute(k, v);
  }
  for (const child of children.flat()) {
    if (child !== null && child !== undefined && child !== false) node.append(child);
  }
  return node;
}

function statusPill(status) {
  return el('span', {
    class: 'pill',
    style: `color: var(--${status}); background: var(--${status}-bg)`,
  }, STATUS_LABELS[status] || status);
}

function formatDate(iso) {
  if (!iso) return '';
  const d = new Date(iso.length === 10 ? `${iso}T00:00:00` : iso);
  return d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

function showView(loggedIn) {
  $('#login-view').hidden = loggedIn;
  $('#app-view').hidden = !loggedIn;
}

function signOut() {
  state.token = null;
  storage((s) => s.removeItem('revolt-admin-token'));
  showView(false);
}

async function loadStats() {
  const s = await api('/api/admin/stats');
  const cards = [
    ['Total requests', s.totalRequests],
    ['Awaiting action', s.activeRequests],
    ['Completed', s.completedRequests],
    ['Est. kg diverted', s.kgDiverted],
    ['Registered users', s.users],
  ];
  $('#stats').replaceChildren(...cards.map(([label, value]) =>
    el('div', { class: 'stat' }, el('div', { class: 'value' }, String(value)), el('div', { class: 'label' }, label))));
}

function renderTabs() {
  const counts = { all: state.requests.length };
  for (const r of state.requests) counts[r.status] = (counts[r.status] || 0) + 1;
  const tabs = ['all', ...Object.keys(STATUS_LABELS)].map((key) =>
    el('button', {
      class: 'tab',
      type: 'button',
      'aria-pressed': String(state.filter === key),
      onclick: () => { state.filter = key; renderTabs(); renderRequests(); },
    }, key === 'all' ? 'All' : STATUS_LABELS[key], el('span', { class: 'count' }, String(counts[key] || 0))));
  $('#tabs').replaceChildren(...tabs);
}

function renderRequests() {
  const list = state.requests.filter((r) => state.filter === 'all' || r.status === state.filter);
  if (!list.length) {
    $('#requests').replaceChildren(el('div', { class: 'empty' }, 'No requests here yet.'));
    return;
  }
  $('#requests').replaceChildren(...list.map((r) => {
    const thumb = r.imageUrl
      ? el('img', { class: 'thumb', src: r.imageUrl, alt: r.deviceName, loading: 'lazy' })
      : el('div', { class: 'thumb' }, 'No photo');
    const next = NEXT_STEP[r.status];
    return el('article', { class: 'request' },
      thumb,
      el('div', {},
        el('h3', {}, `#${r.id} · ${r.deviceName}`, r.quantity > 1 ? ` ×${r.quantity}` : ''),
        el('div', { class: 'meta' },
          el('span', {}, `${r.categoryLabel} · ${r.conditionLabel}`),
          el('span', {}, `→ ${r.organization.name}`),
          el('span', {}, `Submitted ${formatDate(r.createdAt)}`),
          r.preferredDate ? el('span', {}, `Preferred ${formatDate(r.preferredDate)}`) : null),
        el('div', { class: 'meta' },
          el('span', {}, `${r.user.name} · ${r.user.phone} · ${r.user.email}`),
          el('span', {}, r.pickupAddress)),
        r.description ? el('p', { class: 'desc' }, r.description) : null),
      el('div', { class: 'side' },
        el('span', { class: 'pill type' }, r.type),
        statusPill(r.status),
        FINAL.includes(r.status) ? null :
          el('button', { class: 'btn small primary', type: 'button', onclick: () => openDialog(r, next) },
            next ? `Mark ${STATUS_LABELS[next].toLowerCase()}` : 'Update')));
  }));
}

async function loadRequests() {
  state.requests = await api('/api/admin/requests');
  renderTabs();
  renderRequests();
}

async function refresh() {
  try {
    await Promise.all([loadStats(), loadRequests()]);
  } catch (err) {
    if (state.token) alert(err.message);
  }
}

function openDialog(request, suggested) {
  state.editing = request;
  $('#status-title').textContent = `Update #${request.id} · ${request.deviceName}`;
  $('#status-select').replaceChildren(...SETTABLE.filter((s) => s !== request.status)
    .map((s) => el('option', { value: s }, STATUS_LABELS[s])));
  $('#status-select').value = suggested || 'scheduled';
  $('#status-form').note.value = '';
  $('#status-error').textContent = '';
  $('#status-dialog').showModal();
}

$('#status-form').addEventListener('submit', async (e) => {
  if (e.submitter?.value !== 'save') return;
  e.preventDefault();
  const form = e.target;
  $('#status-save').disabled = true;
  try {
    await api(`/api/admin/requests/${state.editing.id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status: form.status.value, note: form.note.value }),
    });
    $('#status-dialog').close();
    await refresh();
  } catch (err) {
    $('#status-error').textContent = err.message;
  } finally {
    $('#status-save').disabled = false;
  }
});

$('#login-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.target;
  $('#login-error').textContent = '';
  try {
    const { token, user } = await api('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: form.email.value, password: form.password.value }),
    });
    if (user.role !== 'admin') throw new Error('This account does not have admin access');
    state.token = token;
    storage((s) => s.setItem('revolt-admin-token', token));
    showView(true);
    refresh();
  } catch (err) {
    $('#login-error').textContent = err.message;
  }
});

$('#logout').addEventListener('click', signOut);
$('#refresh').addEventListener('click', refresh);

state.token = storage((s) => s.getItem('revolt-admin-token'));
showView(Boolean(state.token));
if (state.token) refresh();
setInterval(() => { if (state.token && !$('#status-dialog').open) refresh(); }, 30000);
