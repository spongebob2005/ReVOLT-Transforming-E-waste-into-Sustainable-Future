const os = require('node:os');
const config = require('./config');
const { openDatabase } = require('./db');
const { createApp } = require('./app');

const db = openDatabase(config.dbPath, config);
const app = createApp({ db, uploadDir: config.uploadDir, jwtSecret: config.jwtSecret });

app.listen(config.port, '0.0.0.0', () => {
  const lan = Object.values(os.networkInterfaces())
    .flat()
    .filter((i) => i && i.family === 'IPv4' && !i.internal)
    .map((i) => i.address);

  console.log(`ReVOLT API listening on port ${config.port}`);
  console.log(`  Admin dashboard : http://localhost:${config.port}/admin/`);
  console.log(`  Android emulator: http://10.0.2.2:${config.port}/`);
  for (const ip of lan) console.log(`  Phone on Wi-Fi  : http://${ip}:${config.port}/`);
  console.log(`  Admin login     : ${config.adminEmail}` +
    (process.env.ADMIN_PASSWORD ? '' : ` / ${config.adminPassword} (set ADMIN_PASSWORD to change)`));
});
