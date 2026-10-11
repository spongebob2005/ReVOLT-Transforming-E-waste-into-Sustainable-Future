const fs = require('node:fs');
const path = require('node:path');
const { DatabaseSync } = require('node:sqlite');
const bcrypt = require('bcryptjs');
const organizations = require('./organizations');

const SCHEMA = `
CREATE TABLE IF NOT EXISTS users (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  name          TEXT NOT NULL,
  email         TEXT NOT NULL UNIQUE COLLATE NOCASE,
  phone         TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  role          TEXT NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'admin')),
  created_at    TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS organizations (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  slug     TEXT NOT NULL UNIQUE,
  name     TEXT NOT NULL,
  tagline  TEXT NOT NULL,
  city     TEXT NOT NULL,
  website  TEXT,
  logo_key TEXT,
  about    TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS pickup_requests (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  organization_id INTEGER NOT NULL REFERENCES organizations(id),
  type            TEXT NOT NULL CHECK (type IN ('reuse', 'recycle')),
  device_name     TEXT NOT NULL,
  category        TEXT NOT NULL,
  condition       TEXT NOT NULL,
  quantity        INTEGER NOT NULL DEFAULT 1,
  description     TEXT NOT NULL DEFAULT '',
  pickup_address  TEXT NOT NULL,
  preferred_date  TEXT,
  image_path      TEXT,
  status          TEXT NOT NULL DEFAULT 'pending',
  created_at      TEXT NOT NULL,
  updated_at      TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_requests_user ON pickup_requests(user_id);

CREATE TABLE IF NOT EXISTS request_events (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  request_id INTEGER NOT NULL REFERENCES pickup_requests(id) ON DELETE CASCADE,
  status     TEXT NOT NULL,
  note       TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_events_request ON request_events(request_id);
`;

function now() {
  return new Date().toISOString();
}

function seed(db, { adminEmail, adminPassword }) {
  const insertOrg = db.prepare(
    `INSERT OR IGNORE INTO organizations (slug, name, tagline, city, website, logo_key, about)
     VALUES (?, ?, ?, ?, ?, ?, ?)`
  );
  for (const o of organizations) {
    insertOrg.run(o.slug, o.name, o.tagline, o.city, o.website, o.logoKey, o.about);
  }

  const admin = db.prepare('SELECT id FROM users WHERE email = ?').get(adminEmail);
  if (!admin) {
    db.prepare(
      `INSERT INTO users (name, email, phone, password_hash, role, created_at)
       VALUES (?, ?, ?, ?, 'admin', ?)`
    ).run('ReVOLT Admin', adminEmail, '0000000000', bcrypt.hashSync(adminPassword, 10), now());
  }
}

function openDatabase(file, options) {
  if (file !== ':memory:') fs.mkdirSync(path.dirname(file), { recursive: true });
  const db = new DatabaseSync(file);
  db.exec('PRAGMA foreign_keys = ON;');
  db.exec(SCHEMA);
  seed(db, options);
  return db;
}

module.exports = { openDatabase, now };
