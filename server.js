require('dotenv').config();
const express = require('express');
const { Pool } = require('pg');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json()); // Dışarıdan gelen verileri JSON olarak okumak için

// Veritabanı Bağlantı Ayarları
const pool = new Pool({
    user: process.env.DB_USER,
    host: process.env.DB_HOST,
    database: process.env.DB_NAME,
    password: process.env.DB_PASSWORD,
    port: process.env.DB_PORT,
});

// Veritabanı bağlantısını test edelim
pool.connect()
    .then(() => console.log("Veritabanına efsane bir giriş yapıldı! 🚀"))
    .catch(err => console.error("Bağlantı hatası:", err));

// İLK API'MİZ: Tüm kullanıcıları getir
app.get('/api/users', async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM users');
        res.json(result.rows);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Sunucuyu Başlat
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Sunucu http://localhost:${PORT} adresinde ayaklandı!`);
});