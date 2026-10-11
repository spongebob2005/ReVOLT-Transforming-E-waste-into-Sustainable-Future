const express = require('express');
const { HttpError } = require('../auth');

function toOrganization(row) {
  return {
    id: row.id,
    slug: row.slug,
    name: row.name,
    tagline: row.tagline,
    city: row.city,
    website: row.website,
    logoKey: row.logo_key,
    about: row.about,
  };
}

module.exports = function organizationRoutes({ db }) {
  const router = express.Router();
  const listAll = db.prepare('SELECT * FROM organizations ORDER BY id');
  const findOne = db.prepare('SELECT * FROM organizations WHERE id = ?');

  router.get('/', (req, res) => {
    res.json(listAll.all().map(toOrganization));
  });

  router.get('/:id', (req, res) => {
    const row = findOne.get(Number(req.params.id));
    if (!row) throw new HttpError(404, 'Organization not found');
    res.json(toOrganization(row));
  });

  return router;
};
