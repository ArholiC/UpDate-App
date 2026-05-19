require('dotenv').config();
const express = require('express');
const { Pool } = require('pg');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const http = require('http');
const { Server } = require('socket.io');

const app = express();
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: '*' } });

const JWT_SECRET = process.env.JWT_SECRET || 'update_secret_key';

// --- MIDDLEWARE ---
const uploadDir = path.join(__dirname, 'public/uploads/');
if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir, { recursive: true });

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use('/public', express.static(path.join(__dirname, 'public')));
app.use(express.static(path.join(__dirname)));

// --- VERİTABANI ---
const pool = new Pool({
    user: process.env.DB_USER || 'postgres',
    host: process.env.DB_HOST || 'localhost',
    database: process.env.DB_NAME || 'update_db',
    password: process.env.DB_PASSWORD || 'root',
    port: 5432,
});

pool.query('SELECT NOW()', (err) => {
    if (err) console.error("❌ PostgreSQL bağlantısı başarısız!");
    else console.log("✅ PostgreSQL bağlantısı kuruldu.");
});

// --- JWT MIDDLEWARE ---
const authMiddleware = (req, res, next) => {
    const token = req.headers.authorization?.split(' ')[1];
    if (!token) return res.status(401).json({ message: 'Token gerekli' });
    try {
        req.user = jwt.verify(token, JWT_SECRET);
        next();
    } catch {
        res.status(401).json({ message: 'Geçersiz token' });
    }
};

// --- MULTER ---
const storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, uploadDir),
    filename: (req, file, cb) => cb(null, 'up-' + Date.now() + path.extname(file.originalname))
});
const upload = multer({ storage, limits: { fileSize: 5 * 1024 * 1024 } });

// ==========================================
// AUTH
// ==========================================

app.post('/api/register', async (req, res) => {
    const { full_name, email, password, gender, birth_date, zodiac, interests, bio } = req.body;
    try {
        const check = await pool.query("SELECT id FROM users WHERE email = $1", [email]);
        if (check.rows.length > 0)
            return res.status(400).json({ success: false, message: "Bu e-posta zaten kullanımda!" });

        const hashed = await bcrypt.hash(password, 10);
       const result = await pool.query(
            `INSERT INTO users (full_name, email, password, gender, birth_date, zodiac, interests, bio) 
             VALUES ($1,$2,$3,$4,$5,$6,$7,$8) RETURNING *`,
            [full_name, email, hashed, gender, birth_date, zodiac, interests, bio]
        );
        const user = result.rows[0];
        delete user.password;
        const token = jwt.sign({ id: user.id, email }, JWT_SECRET, { expiresIn: '30d' });
        res.status(201).json({ success: true, token, user });
    } catch (err) {
        console.error("Register Error:", err.message);
        res.status(500).json({ success: false, error: err.message });
    }
});

app.post('/api/login', async (req, res) => {
    const { email, password } = req.body;
    try {
        const result = await pool.query("SELECT * FROM users WHERE email = $1", [email]);
        if (result.rows.length === 0)
            return res.status(401).json({ success: false, message: "E-posta veya şifre hatalı!" });

        const user = result.rows[0];
        
        // Hem hash'li hem düz metin şifreleri destekle (geçiş dönemi için)
        let valid = false;
        if (user.password.startsWith('$2b$')) {
            valid = await bcrypt.compare(password, user.password);
        } else {
            valid = password === user.password;
            if (valid) {
                // Düz metin şifreyi hash'le
                const hashed = await bcrypt.hash(password, 10);
                await pool.query("UPDATE users SET password = $1 WHERE id = $2", [hashed, user.id]);
            }
        }

        if (!valid)
            return res.status(401).json({ success: false, message: "E-posta veya şifre hatalı!" });

        const token = jwt.sign({ id: user.id, email }, JWT_SECRET, { expiresIn: '30d' });
        res.json({ success: true, token, user: { ...user, password: undefined } });
    } catch (err) {
        res.status(500).json({ success: false, error: err.message });
    }
});

// Mevcut kullanıcı bilgisi (token ile)
app.get('/api/me', authMiddleware, async (req, res) => {
    try {
        const result = await pool.query("SELECT id,full_name,email,gender,birth_date,zodiac,interests,profile_pic,bio FROM users WHERE id=$1", [req.user.id]);
        res.json(result.rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// KULLANICILAR
// ==========================================

app.get('/api/users', async (req, res) => {
    try {
        const result = await pool.query("SELECT id,full_name,profile_pic,zodiac,interests FROM users ORDER BY id DESC");
        res.json(result.rows);
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.get('/api/users/:id', async (req, res) => {
    try {
        const result = await pool.query("SELECT id,full_name,email,gender,birth_date,zodiac,interests,profile_pic,bio FROM users WHERE id=$1", [req.params.id]);
        if (result.rows.length > 0) res.json(result.rows[0]);
        else res.status(404).json({ message: "Kullanıcı bulunamadı." });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.put('/api/users/:id', authMiddleware, async (req, res) => {
    const { bio, interests, profile_pic } = req.body;
    try {
        await pool.query("UPDATE users SET bio=$1, interests=$2, profile_pic=$3 WHERE id=$4",
            [bio, interests, profile_pic, req.params.id]);
        res.json({ success: true });
    } catch (err) { res.status(500).json({ success: false, error: err.message }); }
});

app.delete('/api/users/:id', authMiddleware, async (req, res) => {
    try {
        await pool.query("DELETE FROM users WHERE id=$1", [req.params.id]);
        res.json({ success: true });
    } catch (err) { res.status(500).json({ success: false }); }
});

// ==========================================
// KEŞFET
// ==========================================

app.get('/api/discover/:userId', async (req, res) => {
    const myId = parseInt(req.params.userId);
    try {
        const meResult = await pool.query("SELECT interests FROM users WHERE id=$1", [myId]);
        const myInterests = meResult.rows[0]?.interests?.split(',').map(i => i.trim()) || [];

        const seenResult = await pool.query("SELECT liked_id FROM likes WHERE liker_id=$1", [myId]);
        const seenIds = [...seenResult.rows.map(r => r.liked_id), myId];

        const others = await pool.query("SELECT * FROM users WHERE id <> ALL($1)", [seenIds]);

        const scoredUsers = others.rows.map(user => {
            const userInterests = user.interests?.split(',').map(i => i.trim()) || [];
            const matches = userInterests.filter(h => myInterests.includes(h));
            const combined = new Set([...myInterests, ...userInterests]);
            const matchRate = combined.size > 0 ? Math.round((matches.length / combined.size) * 100) : 0;
            return { ...user, password: undefined, match_rate: matchRate };
        });

        scoredUsers.sort((a, b) => b.match_rate - a.match_rate);
        res.json(scoredUsers);
    } catch (err) { res.status(500).json([]); }
});

app.post('/api/like', async (req, res) => {
    const { liker_id, liked_id, is_like } = req.body;
    try {
        await pool.query("INSERT INTO likes (liker_id, liked_id, is_like) VALUES ($1,$2,$3) ON CONFLICT DO NOTHING",
            [liker_id, liked_id, is_like ? 1 : 0]);
        let isMatch = false;
        if (is_like) {
            const check = await pool.query("SELECT id FROM likes WHERE liker_id=$1 AND liked_id=$2 AND is_like=1", [liked_id, liker_id]);
            isMatch = check.rows.length > 0;
        }
        res.json({ success: true, isMatch });
    } catch (err) { res.status(500).json({ success: false }); }
});

// ==========================================
// MESAJLAŞMA
// ==========================================

app.get('/api/matches/:userId', async (req, res) => {
    const myId = parseInt(req.params.userId);
    try {
        const result = await pool.query(`
            SELECT u.id, u.full_name, u.profile_pic, u.bio 
            FROM users u
            JOIN likes l1 ON u.id = l1.liked_id AND l1.liker_id = $1 AND l1.is_like = 1
            JOIN likes l2 ON u.id = l2.liker_id AND l2.liked_id = $1 AND l2.is_like = 1`, [myId]);
        res.json(result.rows);
    } catch (err) { res.status(500).json([]); }
});

app.get('/api/messages/:myId/:otherId', async (req, res) => {
    const { myId, otherId } = req.params;
    try {
        const result = await pool.query(`
            SELECT * FROM messages 
            WHERE (sender_id=$1 AND receiver_id=$2) OR (sender_id=$2 AND receiver_id=$1)
            ORDER BY created_at ASC`, [myId, otherId]);
        res.json(result.rows);
    } catch (err) { res.status(500).json([]); }
});

app.post('/api/messages', async (req, res) => {
    const { sender_id, receiver_id, message } = req.body;
    try {
        const result = await pool.query(
            "INSERT INTO messages (sender_id, receiver_id, message) VALUES ($1,$2,$3) RETURNING *",
            [sender_id, receiver_id, message]);
        const msg = result.rows[0];
        // Socket.io ile karşı tarafa anlık ilet
        io.to(`user_${receiver_id}`).emit('new_message', msg);
        res.json({ success: true, message: msg });
    } catch (err) { res.status(500).json({ success: false }); }
});

// ==========================================
// UPLOAD
// ==========================================

app.post('/api/upload', authMiddleware, upload.single('profil_resmi'), async (req, res) => {
    if (!req.file) return res.status(400).json({ success: false, message: "Dosya seçilmedi!" });
    const filename = req.file.filename;
    await pool.query("UPDATE users SET profile_pic=$1 WHERE id=$2", [filename, req.user.id]);
    res.json({ success: true, filename, url: `/public/uploads/${filename}` });
});

// ==========================================
// SOCKET.IO - GERÇEK ZAMANLI MESAJLAŞMA
// ==========================================

io.on('connection', (socket) => {
    console.log('🔌 Kullanıcı bağlandı:', socket.id);

    socket.on('join', (userId) => {
        socket.join(`user_${userId}`);
        console.log(`👤 User ${userId} odasına katıldı`);
    });

    socket.on('disconnect', () => {
        console.log('🔌 Kullanıcı ayrıldı:', socket.id);
    });
});

// ==========================================
// SUNUCU BAŞLAT
// ==========================================

const PORT = process.env.PORT || 5000;
server.listen(PORT, () => {
    console.log(`🚀 UpDate Sunucusu: http://localhost:${PORT}`);
});