const express = require('express');
const { HttpError, requireAdmin } = require('../auth');
const { STATUSES, FINAL_STATUSES, CATEGORIES } = require('../catalog');
const { toRequest, requestStore, summarize } = require('../requests');

const ADMIN_SETTABLE = ['pending', 'scheduled', 'picked_up', 'completed', 'rejected'];

module.exports = function adminRoutes({ db, auth }) {
  const router = express.Router();
  const store = requestStore(db);
  const countUsers = db.prepare("SELECT COUNT(*) AS n FROM users WHERE role = 'user'");

  router.use(auth, requireAdmin);

  router.get('/stats', (req, res) => {
    const rows = store.listAll();
    const byStatus = Object.fromEntries(Object.keys(STATUSES).map((s) => [s, 0]));
    const byCategory = {};
    for (const r of rows) {
      byStatus[r.status] = (byStatus[r.status] ?? 0) + 1;
      const label = CATEGORIES[r.category]?.label ?? r.category;
      byCategory[label] = (byCategory[label] ?? 0) + r.quantity;
    }
    res.json({ ...summarize(rows), users: countUsers.get().n, byStatus, byCategory });
  });

  router.get('/requests', (req, res) => {
    const status = req.query.status ? String(req.query.status) : null;
    if (status && !STATUSES[status]) throw new HttpError(400, 'Unknown status');
    res.json(store.listAll(status).map((row) => toRequest(row, { includeUser: true })));
  });

  router.patch('/requests/:id/status', (req, res) => {
    const row = store.get(Number(req.params.id));
    if (!row) throw new HttpError(404, 'Request not found');
    const status = String(req.body?.status ?? '');
    const note = String(req.body?.note ?? '').trim().slice(0, 300);
    if (!ADMIN_SETTABLE.includes(status)) throw new HttpError(400, 'Invalid status');
    if (FINAL_STATUSES.includes(row.status)) throw new HttpError(409, `Request is already ${STATUSES[row.status].toLowerCase()}`);
    if (row.status === status) throw new HttpError(409, `Request is already ${STATUSES[status].toLowerCase()}`);

    store.changeStatus(row.id, status, note);
    res.json(toRequest(store.get(row.id), { events: store.eventsFor(row.id), includeUser: true }));
  });

  return router;
};
