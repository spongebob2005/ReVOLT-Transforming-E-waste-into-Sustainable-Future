const path = require('node:path');
const express = require('express');
const multer = require('multer');
const { HttpError, requireAuth } = require('./auth');
const authRoutes = require('./routes/auth');
const organizationRoutes = require('./routes/organizations');
const requestRoutes = require('./routes/requests');
const adminRoutes = require('./routes/admin');

function createApp({ db, uploadDir, jwtSecret }) {
  const app = express();
  const auth = requireAuth(db, jwtSecret);
  const deps = { db, uploadDir, jwtSecret, auth };

  app.disable('x-powered-by');
  app.use(express.json({ limit: '100kb' }));

  app.get('/api/health', (req, res) => res.json({ ok: true, service: 'revolt' }));
  app.use('/api/auth', authRoutes(deps));
  app.use('/api/organizations', organizationRoutes(deps));
  app.use('/api/requests', requestRoutes(deps));
  app.use('/api/admin', adminRoutes(deps));

  app.use('/uploads', express.static(uploadDir, { fallthrough: false, maxAge: '7d' }));
  app.use('/admin', express.static(path.join(__dirname, '..', 'public', 'admin')));
  app.get('/', (req, res) => res.redirect('/admin/'));

  app.use('/api', (req, res) => res.status(404).json({ error: 'Not found' }));

  // eslint-disable-next-line no-unused-vars
  app.use((err, req, res, next) => {
    if (err instanceof HttpError) return res.status(err.status).json({ error: err.message });
    if (err instanceof multer.MulterError) {
      const message = err.code === 'LIMIT_FILE_SIZE' ? 'Photo is too large (max 8 MB)' : err.message;
      return res.status(400).json({ error: message });
    }
    if (err.type === 'entity.parse.failed') return res.status(400).json({ error: 'Malformed JSON body' });
    if (err.status === 404 || err.statusCode === 404) return res.status(404).json({ error: 'Not found' });
    console.error(err);
    res.status(500).json({ error: 'Something went wrong on the server' });
  });

  return app;
}

module.exports = { createApp };
