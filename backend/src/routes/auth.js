const express = require('express');
const bcrypt = require('bcryptjs');
const { HttpError, signToken, publicUser } = require('../auth');
const { now } = require('../db');

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

module.exports = function authRoutes({ db, jwtSecret, auth }) {
  const router = express.Router();
  const findByEmail = db.prepare('SELECT * FROM users WHERE email = ?');
  const findById = db.prepare('SELECT * FROM users WHERE id = ?');
  const insertUser = db.prepare(
    `INSERT INTO users (name, email, phone, password_hash, created_at) VALUES (?, ?, ?, ?, ?)`
  );

  router.post('/register', (req, res) => {
    const body = req.body ?? {};
    const name = String(body.name ?? '').trim();
    const email = String(body.email ?? '').trim().toLowerCase();
    const phone = String(body.phone ?? '').replace(/[\s-]/g, '');
    const password = String(body.password ?? '');

    if (name.length < 2) throw new HttpError(400, 'Please enter your full name');
    if (!EMAIL_RE.test(email)) throw new HttpError(400, 'Please enter a valid email address');
    if (!/^\+?\d{10,13}$/.test(phone)) throw new HttpError(400, 'Please enter a valid 10-digit phone number');
    if (password.length < 6) throw new HttpError(400, 'Password must be at least 6 characters');
    if (findByEmail.get(email)) throw new HttpError(409, 'An account with this email already exists');

    const { lastInsertRowid } = insertUser.run(name, email, phone, bcrypt.hashSync(password, 10), now());
    const user = findById.get(lastInsertRowid);
    res.status(201).json({ token: signToken(user, jwtSecret), user: publicUser(user) });
  });

  router.post('/login', (req, res) => {
    const body = req.body ?? {};
    const email = String(body.email ?? '').trim().toLowerCase();
    const password = String(body.password ?? '');
    const user = findByEmail.get(email);
    if (!user || !bcrypt.compareSync(password, user.password_hash)) {
      throw new HttpError(401, 'Incorrect email or password');
    }
    res.json({ token: signToken(user, jwtSecret), user: publicUser(user) });
  });

  router.get('/me', auth, (req, res) => {
    res.json(publicUser(req.user));
  });

  return router;
};
