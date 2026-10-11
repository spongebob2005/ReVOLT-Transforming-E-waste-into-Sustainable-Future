// Shared queries and serialisation for pickup requests (used by user and admin routes).
const { CATEGORIES, CONDITIONS, STATUSES, FINAL_STATUSES } = require('./catalog');
const { now } = require('./db');

const SELECT_REQUEST = `
  SELECT r.*, o.name AS org_name, o.logo_key AS org_logo_key, o.city AS org_city,
         u.name AS user_name, u.email AS user_email, u.phone AS user_phone
  FROM pickup_requests r
  JOIN organizations o ON o.id = r.organization_id
  JOIN users u ON u.id = r.user_id`;

function estimatedWeightKg(row) {
  const perItem = CATEGORIES[row.category]?.weightKg ?? 1;
  return Math.round(perItem * row.quantity * 100) / 100;
}

function toRequest(row, { events, includeUser } = {}) {
  const dto = {
    id: row.id,
    type: row.type,
    deviceName: row.device_name,
    category: row.category,
    categoryLabel: CATEGORIES[row.category]?.label ?? row.category,
    condition: row.condition,
    conditionLabel: CONDITIONS[row.condition] ?? row.condition,
    quantity: row.quantity,
    description: row.description,
    pickupAddress: row.pickup_address,
    preferredDate: row.preferred_date,
    imageUrl: row.image_path ? `/uploads/${row.image_path}` : null,
    status: row.status,
    statusLabel: STATUSES[row.status] ?? row.status,
    estimatedWeightKg: estimatedWeightKg(row),
    organization: { id: row.organization_id, name: row.org_name, logoKey: row.org_logo_key, city: row.org_city },
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
  if (includeUser) dto.user = { id: row.user_id, name: row.user_name, email: row.user_email, phone: row.user_phone };
  if (events) {
    dto.events = events.map((e) => ({
      status: e.status,
      statusLabel: STATUSES[e.status] ?? e.status,
      note: e.note,
      createdAt: e.created_at,
    }));
  }
  return dto;
}

function requestStore(db) {
  const byId = db.prepare(`${SELECT_REQUEST} WHERE r.id = ?`);
  const byUser = db.prepare(`${SELECT_REQUEST} WHERE r.user_id = ? ORDER BY r.id DESC`);
  const all = db.prepare(`${SELECT_REQUEST} ORDER BY r.id DESC`);
  const byStatus = db.prepare(`${SELECT_REQUEST} WHERE r.status = ? ORDER BY r.id DESC`);
  const eventsFor = db.prepare('SELECT * FROM request_events WHERE request_id = ? ORDER BY id');
  const setStatus = db.prepare('UPDATE pickup_requests SET status = ?, updated_at = ? WHERE id = ?');
  const addEvent = db.prepare('INSERT INTO request_events (request_id, status, note, created_at) VALUES (?, ?, ?, ?)');

  return {
    get: (id) => byId.get(id),
    eventsFor: (id) => eventsFor.all(id),
    listForUser: (userId) => byUser.all(userId),
    listAll: (status) => (status ? byStatus.all(status) : all.all()),
    addEvent: (id, status, note) => addEvent.run(id, status, note ?? '', now()),
    changeStatus(id, status, note) {
      db.exec('BEGIN');
      try {
        setStatus.run(status, now(), id);
        addEvent.run(id, status, note ?? '', now());
        db.exec('COMMIT');
      } catch (err) {
        db.exec('ROLLBACK');
        throw err;
      }
    },
  };
}

// Aggregate numbers for a set of request rows.
function summarize(rows) {
  const completed = rows.filter((r) => r.status === 'completed');
  return {
    totalRequests: rows.length,
    activeRequests: rows.filter((r) => !FINAL_STATUSES.includes(r.status)).length,
    completedRequests: completed.length,
    devicesHandled: completed.reduce((sum, r) => sum + r.quantity, 0),
    kgDiverted: Math.round(completed.reduce((sum, r) => sum + estimatedWeightKg(r), 0) * 10) / 10,
  };
}

module.exports = { toRequest, requestStore, summarize };
