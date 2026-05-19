const { Client } = require('pg');

const client = new Client({
  user: 'postgres',
  password: 'admin123',
  host: '127.0.0.1',
  port: 5432,
  database: 'update_db'
});

client.connect()
  .then(() => console.log('BAŞARILI: Veritabanı sapasağlam! Sorun projenin kodlarında.'))
  .catch(err => console.error('GERÇEK HATA ŞU:', err.message))
  .finally(() => client.end());