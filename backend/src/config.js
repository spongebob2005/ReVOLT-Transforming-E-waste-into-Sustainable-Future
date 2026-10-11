const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');

const dataDir = path.resolve(process.env.DATA_DIR || path.join(__dirname, '..', 'data'));

// A JWT secret that survives restarts, so logged-in phones stay logged in.
function loadJwtSecret() {
  if (process.env.JWT_SECRET) return process.env.JWT_SECRET;
  const file = path.join(dataDir, '.jwt-secret');
  if (fs.existsSync(file)) return fs.readFileSync(file, 'utf8').trim();
  fs.mkdirSync(dataDir, { recursive: true });
  const secret = crypto.randomBytes(48).toString('hex');
  fs.writeFileSync(file, secret, { mode: 0o600 });
  return secret;
}

module.exports = {
  port: Number(process.env.PORT) || 3000,
  dbPath: process.env.DB_PATH || path.join(dataDir, 'revolt.db'),
  uploadDir: path.resolve(process.env.UPLOAD_DIR || path.join(dataDir, 'uploads')),
  jwtSecret: loadJwtSecret(),
  adminEmail: process.env.ADMIN_EMAIL || 'admin@revolt.app',
  adminPassword: process.env.ADMIN_PASSWORD || 'admin123',
};
