const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { openDatabase } = require('../src/db');
const { createApp } = require('../src/app');

const ADMIN = { email: 'admin@test.local', password: 'admin-pass' };
let server;
let base;
let uploadDir;

before(async () => {
  uploadDir = fs.mkdtempSync(path.join(os.tmpdir(), 'revolt-test-'));
  const db = openDatabase(':memory:', { adminEmail: ADMIN.email, adminPassword: ADMIN.password });
  const app = createApp({ db, uploadDir, jwtSecret: 'test-secret' });
  await new Promise((resolve) => {
    server = app.listen(0, '127.0.0.1', resolve);
  });
  base = `http://127.0.0.1:${server.address().port}`;
});

after(() => {
  server.close();
  fs.rmSync(uploadDir, { recursive: true, force: true });
});

async function api(method, url, { token, json, form } = {}) {
  const headers = {};
  if (token) headers.authorization = `Bearer ${token}`;
  let body;
  if (json) {
    headers['content-type'] = 'application/json';
    body = JSON.stringify(json);
  } else if (form) {
    body = form;
  }
  const res = await fetch(base + url, { method, headers, body });
  const text = await res.text();
  return { status: res.status, body: text ? JSON.parse(text) : null };
}

const tomorrow = new Date(Date.now() + 86_400_000).toISOString().slice(0, 10);

function requestForm(overrides = {}) {
  const fields = {
    type: 'recycle',
    deviceName: 'Old Nokia phone',
    category: 'phone',
    condition: 'not_working',
    quantity: '2',
    description: 'Screen cracked',
    organizationId: '1',
    pickupAddress: '12 MG Road, Pune',
    preferredDate: tomorrow,
    ...overrides,
  };
  const form = new FormData();
  for (const [k, v] of Object.entries(fields)) if (v !== undefined) form.append(k, v);
  return form;
}

const user = { name: 'Asha Patil', email: 'asha@example.com', phone: '9876543210', password: 'secret1' };
let userToken;
let adminToken;
let requestId;

test('health check', async () => {
  const res = await api('GET', '/api/health');
  assert.equal(res.status, 200);
  assert.equal(res.body.ok, true);
});

test('registration validates input and rejects duplicates', async () => {
  const bad = await api('POST', '/api/auth/register', { json: { ...user, email: 'not-an-email' } });
  assert.equal(bad.status, 400);

  const ok = await api('POST', '/api/auth/register', { json: user });
  assert.equal(ok.status, 201);
  assert.ok(ok.body.token);
  assert.equal(ok.body.user.email, user.email);
  assert.equal(ok.body.user.password_hash, undefined);

  const dup = await api('POST', '/api/auth/register', { json: { ...user, email: 'ASHA@example.com' } });
  assert.equal(dup.status, 409);
});

test('login succeeds with the right password only', async () => {
  const wrong = await api('POST', '/api/auth/login', { json: { email: user.email, password: 'nope' } });
  assert.equal(wrong.status, 401);

  const ok = await api('POST', '/api/auth/login', { json: { email: user.email, password: user.password } });
  assert.equal(ok.status, 200);
  userToken = ok.body.token;

  const me = await api('GET', '/api/auth/me', { token: userToken });
  assert.equal(me.body.name, user.name);

  const admin = await api('POST', '/api/auth/login', { json: ADMIN });
  assert.equal(admin.body.user.role, 'admin');
  adminToken = admin.body.token;
});

test('protected routes require a valid token', async () => {
  assert.equal((await api('GET', '/api/requests')).status, 401);
  assert.equal((await api('GET', '/api/requests', { token: 'garbage' })).status, 401);
});

test('organizations are seeded', async () => {
  const res = await api('GET', '/api/organizations');
  assert.equal(res.status, 200);
  assert.equal(res.body.length, 4);
  assert.equal(res.body[0].logoKey, 'green_peace');
});

test('creating a pickup request with a photo', async () => {
  const invalid = await api('POST', '/api/requests', { token: userToken, form: requestForm({ category: 'spaceship' }) });
  assert.equal(invalid.status, 400);
  assert.equal(fs.readdirSync(uploadDir).length, 0, 'rejected uploads are cleaned up');

  const form = requestForm();
  form.append('image', new Blob([Buffer.from([0xff, 0xd8, 0xff, 0xd9])], { type: 'image/jpeg' }), 'phone.jpg');
  const res = await api('POST', '/api/requests', { token: userToken, form });
  assert.equal(res.status, 201);
  assert.equal(res.body.status, 'pending');
  assert.equal(res.body.quantity, 2);
  assert.equal(res.body.estimatedWeightKg, 0.4);
  assert.equal(res.body.organization.name, 'Greenpeace India');
  assert.match(res.body.imageUrl, /^\/uploads\/.+\.jpg$/);
  assert.equal(res.body.events.length, 1);
  requestId = res.body.id;

  const image = await fetch(base + res.body.imageUrl);
  assert.equal(image.status, 200);
});

test('rejects a preferred date in the past', async () => {
  const res = await api('POST', '/api/requests', { token: userToken, form: requestForm({ preferredDate: '2000-01-01' }) });
  assert.equal(res.status, 400);
});

test('users only see their own requests', async () => {
  const other = await api('POST', '/api/auth/register', {
    json: { name: 'Ravi', email: 'ravi@example.com', phone: '9123456780', password: 'secret2' },
  });
  const list = await api('GET', '/api/requests', { token: other.body.token });
  assert.deepEqual(list.body, []);
  const detail = await api('GET', `/api/requests/${requestId}`, { token: other.body.token });
  assert.equal(detail.status, 404);

  const mine = await api('GET', '/api/requests', { token: userToken });
  assert.equal(mine.body.length, 1);
});

test('admin endpoints are forbidden to normal users', async () => {
  const res = await api('GET', '/api/admin/requests', { token: userToken });
  assert.equal(res.status, 403);
});

test('admin moves a request through its lifecycle and stats update', async () => {
  for (const status of ['scheduled', 'picked_up', 'completed']) {
    const res = await api('PATCH', `/api/admin/requests/${requestId}/status`, {
      token: adminToken,
      json: { status, note: `Marked ${status}` },
    });
    assert.equal(res.status, 200, JSON.stringify(res.body));
    assert.equal(res.body.status, status);
  }

  const again = await api('PATCH', `/api/admin/requests/${requestId}/status`, {
    token: adminToken,
    json: { status: 'scheduled' },
  });
  assert.equal(again.status, 409, 'completed requests are final');

  const detail = await api('GET', `/api/requests/${requestId}`, { token: userToken });
  assert.deepEqual(detail.body.events.map((e) => e.status), ['pending', 'scheduled', 'picked_up', 'completed']);

  const stats = await api('GET', '/api/requests/stats', { token: userToken });
  assert.deepEqual(stats.body, {
    totalRequests: 1, activeRequests: 0, completedRequests: 1, devicesHandled: 2, kgDiverted: 0.4,
  });

  const adminStats = await api('GET', '/api/admin/stats', { token: adminToken });
  assert.equal(adminStats.body.users, 2);
  assert.equal(adminStats.body.byStatus.completed, 1);
});

test('users can cancel pending requests but not completed ones', async () => {
  const created = await api('POST', '/api/requests', { token: userToken, form: requestForm({ type: 'reuse', condition: 'working' }) });
  const cancel = await api('POST', `/api/requests/${created.body.id}/cancel`, { token: userToken });
  assert.equal(cancel.status, 200);
  assert.equal(cancel.body.status, 'cancelled');

  const late = await api('POST', `/api/requests/${requestId}/cancel`, { token: userToken });
  assert.equal(late.status, 409);
});
