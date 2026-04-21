const express = require('express');
const { Pool } = require('pg');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const app = express();

// --- SİSTEM AYARLARI & KLASÖR KONTROLÜ ---
const uploadDir = path.join(__dirname, 'public/uploads/');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
    console.log("📁 Uploads klasörü oluşturuldu.");
}

// --- MIDDLEWARE ---
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use('/public', express.static(path.join(__dirname, 'public')));

// --- POSTGRESQL BAĞLANTISI ---
const pool = new Pool({
    user: 'postgres',
    host: 'localhost',
    database: 'update_db',
    password: 'root', // pgAdmin şifren 'root' değilse burayı hemen düzelt kanka!
    port: 5432,
});

// Veritabanı Bağlantı Testi (Kritik)
pool.query('SELECT NOW()', (err, res) => {
    if (err) {
        console.error("❌ HATA: PostgreSQL bağlantısı kurulamadı!");
        console.error("Detay:", err.message);
    } else {
        console.log("✅ PostgreSQL bağlantısı başarılı! Zaman:", res.rows[0].now);
    }
});

// --- MULTER (RESİM YÜKLEME) AYARLARI ---
const storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, uploadDir),
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, 'up-' + uniqueSuffix + path.extname(file.originalname));
    }
});
const upload = multer({ 
    storage,
    limits: { fileSize: 5 * 1024 * 1024 }, // 5MB Limit
    fileFilter: (req, file, cb) => {
        const filetypes = /jpeg|jpg|png|webp/;
        const mimetype = filetypes.test(file.mimetype);
        if (mimetype) return cb(null, true);
        cb(new Error("Sadece resim yüklenebilir kanka!"));
    }
});

// ==========================================
// 1. KULLANICI İŞLEMLERİ (Giriş, Kayıt, Profil)
// ==========================================

app.post('/api/register', async (req, res) => {
    const { full_name, email, password, gender, birth_date, zodiac, interests } = req.body;
    try {
        const check = await pool.query("SELECT * FROM users WHERE email = $1", [email]);
        if (check.rows.length > 0) return res.status(400).json({ success: false, message: "Bu mail zaten kayıtlı!" });

        const query = `INSERT INTO users (full_name, email, password, gender, birth_date, zodiac, interests) 
                       VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING id`;
        const result = await pool.query(query, [full_name, email, password, gender, birth_date, zodiac, interests]);
        res.status(201).json({ success: true, userId: result.rows[0].id });
    } catch (err) {
        res.status(500).json({ success: false, error: err.message });
    }
});

app.post('/api/login', async (req, res) => {
    const { email, password } = req.body;
    try {
        const result = await pool.query("SELECT * FROM users WHERE email = $1 AND password = $2", [email, password]);
        if (result.rows.length > 0) {
            res.json({ success: true, user: result.rows[0] });
        } else {
            res.status(401).json({ success: false, message: "Hatalı giriş!" });
        }
    } catch (err) { res.status(500).json({ success: false }); }
});

app.get('/api/users/:id', async (req, res) => {
    try {
        const result = await pool.query("SELECT * FROM users WHERE id = $1", [req.params.id]);
        if (result.rows.length > 0) res.json(result.rows[0]);
        else res.status(404).json({ message: "Bulunamadı" });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.put('/api/users/:id', async (req, res) => {
    const { bio, interests, profile_pic } = req.body;
    try {
        await pool.query("UPDATE users SET bio = $1, interests = $2, profile_pic = $3 WHERE id = $4", [bio, interests, profile_pic, req.params.id]);
        res.json({ success: true });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

// ==========================================
// 2. KEŞFET & SWIPE (MATCHMAKING)
// ==========================================

app.get('/api/discover/:userId', async (req, res) => {
    const myId = parseInt(req.params.userId);
    try {
        // 1. Hobilerimi al
        const meResult = await pool.query("SELECT interests FROM users WHERE id = $1", [myId]);
        const myInterests = meResult.rows[0]?.interests ? meResult.rows[0].interests.split(', ') : [];

        // 2. Görülenleri al
        const seenResult = await pool.query("SELECT liked_id FROM likes WHERE liker_id = $1", [myId]);
        const seenIds = seenResult.rows.map(r => r.liked_id);
        seenIds.push(myId);

        // 3. Filtrele & Getir
        const others = await pool.query("SELECT * FROM users WHERE id <> ALL($1)", [seenIds]);
        
        // 4. Akıllı Sıralama (Hobi benzerliği)
        const sorted = others.rows.sort((a, b) => {
            const countA = a.interests ? a.interests.split(', ').filter(i => myInterests.includes(i)).length : 0;
            const countB = b.interests ? b.interests.split(', ').filter(i => myInterests.includes(i)).length : 0;
            return countB - countA;
        });

        res.json(sorted);
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.post('/api/like', async (req, res) => {
    const { liker_id, liked_id, is_like } = req.body;
    try {
        await pool.query("INSERT INTO likes (liker_id, liked_id, is_like) VALUES ($1, $2, $3)", [liker_id, liked_id, is_like ? 1 : 0]);
        let match = false;
        if (is_like) {
            const check = await pool.query("SELECT * FROM likes WHERE liker_id = $1 AND liked_id = $2 AND is_like = 1", [liked_id, liker_id]);
            if (check.rows.length > 0) match = true;
        }
        res.json({ success: true, match });
    } catch (err) { res.status(500).json({ success: false }); }
});

// ==========================================
// 3. MESAJLAŞMA SİSTEMİ (404-FIX)
// ==========================================

app.post('/api/messages', async (req, res) => {
    const { sender_id, receiver_id, message } = req.body;
    try {
        await pool.query("INSERT INTO messages (sender_id, receiver_id, message) VALUES ($1, $2, $3)", [sender_id, receiver_id, message]);
        res.json({ success: true });
    } catch (err) { res.status(500).json({ success: false }); }
});

app.get('/api/messages/:myId/:otherId', async (req, res) => {
    const { myId, otherId } = req.params;
    try {
        const query = `SELECT * FROM messages 
                       WHERE (sender_id = $1 AND receiver_id = $2) 
                       OR (sender_id = $2 AND receiver_id = $1)
                       ORDER BY created_at ASC`;
        const result = await pool.query(query, [myId, otherId]);
        res.json(result.rows || []);
    } catch (err) { res.status(500).json([]); }
});

app.get('/api/matches/:userId', async (req, res) => {
    const myId = req.params.userId;
    try {
        const query = `SELECT u.id, u.full_name, u.profile_pic, u.bio FROM users u
                       JOIN likes l1 ON u.id = l1.liked_id
                       JOIN likes l2 ON u.id = l2.liker_id
                       WHERE l1.liker_id = $1 AND l1.is_like = 1
                       AND l2.liked_id = $1 AND l2.is_like = 1`;
        const result = await pool.query(query, [myId]);
        res.json(result.rows);
    } catch (err) { res.status(500).json([]); }
});

// ==========================================
// 4. RESİM YÜKLEME
// ==========================================

app.post('/api/upload', upload.single('profil_resmi'), (req, res) => {
    if (!req.file) return res.status(400).json({ success: false });
    res.json({ success: true, url: `/public/uploads/${req.file.filename}` });
});

const PORT = 5000;
app.listen(PORT, () => console.log(`🚀 UpDate Server Arşa Çıktı: ${PORT}`));