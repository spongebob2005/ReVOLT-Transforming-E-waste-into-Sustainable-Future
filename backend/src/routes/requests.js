const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const express = require('express');
const multer = require('multer');
const { HttpError } = require('../auth');
const { CATEGORIES, CONDITIONS, TYPES } = require('../catalog');
const { toRequest, requestStore, summarize } = require('../requests');
const { now } = require('../db');

const IMAGE_TYPES = { 'image/jpeg': '.jpg', 'image/png': '.png', 'image/webp': '.webp' };

function makeUpload(uploadDir) {
  fs.mkdirSync(uploadDir, { recursive: true });
  return multer({
    storage: multer.diskStorage({
      destination: uploadDir,
      filename: (req, file, cb) => cb(null, crypto.randomUUID() + IMAGE_TYPES[file.mimetype]),
    }),
    limits: { fileSize: 8 * 1024 * 1024 },
    fileFilter: (req, file, cb) => {
      if (IMAGE_TYPES[file.mimetype]) cb(null, true);
      else cb(new HttpError(400, 'Photo must be a JPEG, PNG or WebP image'));
    },
  });
}

function parseNewRequest(body, orgExists) {
  const type = String(body.type ?? '');
  const deviceName = String(body.deviceName ?? '').trim();
  const category = String(body.category ?? '');
  const condition = String(body.condition ?? '');
  const quantity = Number(body.quantity ?? 1);
  const description = String(body.description ?? '').trim();
  const organizationId = Number(body.organizationId);
  const pickupAddress = String(body.pickupAddress ?? '').trim();
  const preferredDate = body.preferredDate ? String(body.preferredDate) : null;

  if (!TYPES.includes(type)) throw new HttpError(400, 'Choose whether to reuse or recycle the device');
  if (deviceName.length < 2 || deviceName.length > 80) throw new HttpError(400, 'Please enter the device name');
  if (!CATEGORIES[category]) throw new HttpError(400, 'Please choose a device category');
  if (!CONDITIONS[condition]) throw new HttpError(400, 'Please choose the device condition');
  if (!Number.isInteger(quantity) || quantity < 1 || quantity > 50) throw new HttpError(400, 'Quantity must be between 1 and 50');
  if (description.length > 500) throw new HttpError(400, 'Description must be 500 characters or fewer');
  if (!orgExists(organizationId)) throw new HttpError(400, 'Please choose a partner organization');
  if (pickupAddress.length < 5) throw new HttpError(400, 'Please enter a pickup address');
  if (preferredDate !== null) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(preferredDate) || Number.isNaN(Date.parse(preferredDate))) {
      throw new HttpError(400, 'Preferred date must be in YYYY-MM-DD format');
    }
    if (preferredDate < new Date().toISOString().slice(0, 10)) {
      throw new HttpError(400, 'Preferred pickup date cannot be in the past');
    }
  }
  return { type, deviceName, category, condition, quantity, description, organizationId, pickupAddress, preferredDate };
}

module.exports = function requestRoutes({ db, uploadDir, auth }) {
  const router = express.Router();
  const store = requestStore(db);
  const upload = makeUpload(uploadDir);
  const findOrg = db.prepare('SELECT id FROM organizations WHERE id = ?');
  const insert = db.prepare(
    `INSERT INTO pickup_requests
       (user_id, organization_id, type, device_name, category, condition, quantity, description,
        pickup_address, preferred_date, image_path, status, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', ?, ?)`
  );

  router.use(auth);

  router.post('/', upload.single('image'), (req, res) => {
    let input;
    try {
      input = parseNewRequest(req.body ?? {}, (id) => Boolean(findOrg.get(id)));
    } catch (err) {
      if (req.file) fs.rm(req.file.path, { force: true }, () => {});
      throw err;
    }
    const ts = now();
    const { lastInsertRowid: id } = insert.run(
      req.user.id, input.organizationId, input.type, input.deviceName, input.category, input.condition,
      input.quantity, input.description, input.pickupAddress, input.preferredDate,
      req.file ? path.basename(req.file.path) : null, ts, ts
    );
    store.addEvent(id, 'pending', 'Request submitted');
    res.status(201).json(toRequest(store.get(id), { events: store.eventsFor(id) }));
  });

  router.get('/', (req, res) => {
    res.json(store.listForUser(req.user.id).map((row) => toRequest(row)));
  });

  router.get('/stats', (req, res) => {
    res.json(summarize(store.listForUser(req.user.id)));
  });

  function ownedRequest(req) {
    const row = store.get(Number(req.params.id));
    if (!row || (row.user_id !== req.user.id && req.user.role !== 'admin')) {
      throw new HttpError(404, 'Request not found');
    }
    return row;
  }

  router.get('/:id', (req, res) => {
    const row = ownedRequest(req);
    res.json(toRequest(row, { events: store.eventsFor(row.id) }));
  });

  router.post('/:id/cancel', (req, res) => {
    const row = ownedRequest(req);
    if (!['pending', 'scheduled'].includes(row.status)) {
      throw new HttpError(409, 'This request can no longer be cancelled');
    }
    store.changeStatus(row.id, 'cancelled', 'Cancelled by you');
    res.json(toRequest(store.get(row.id), { events: store.eventsFor(row.id) }));
  });

  return router;
};
