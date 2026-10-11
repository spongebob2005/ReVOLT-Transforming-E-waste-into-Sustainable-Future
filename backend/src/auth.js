const jwt = require('jsonwebtoken');

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function signToken(user, secret) {
  return jwt.sign({ sub: String(user.id), role: user.role }, secret, { expiresIn: '30d' });
}

function publicUser(row) {
  return {
    id: row.id,
    name: row.name,
    email: row.email,
    phone: row.phone,
    role: row.role,
    createdAt: row.created_at,
  };
}

// Attaches req.user (the full DB row) or rejects with 401.
function requireAuth(db, secret) {
  const findUser = db.prepare('SELECT * FROM users WHERE id = ?');
  return (req, res, next) => {
    const header = req.get('authorization') || '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : null;
    if (!token) return next(new HttpError(401, 'Please log in to continue'));
    try {
      const payload = jwt.verify(token, secret);
      const user = findUser.get(Number(payload.sub));
      if (!user) return next(new HttpError(401, 'Your session has expired, please log in again'));
      req.user = user;
      next();
    } catch {
      next(new HttpError(401, 'Your session has expired, please log in again'));
    }
  };
}

function requireAdmin(req, res, next) {
  if (req.user?.role !== 'admin') return next(new HttpError(403, 'Admin access required'));
  next();
}

module.exports = { HttpError, signToken, publicUser, requireAuth, requireAdmin };
