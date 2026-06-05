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

app.use(cors({ origin: '*', methods: ['GET','POST','PUT','DELETE','OPTIONS'] }));
app.use(express.json({ limit: '20mb' }));
app.use(express.urlencoded({ extended: true, limit: '20mb' }));

// UTF-8 charset header - Sadece API rotaları için
app.use('/api', (req, res, next) => {
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    next();
});

// Ses/video dosyaları için CORS ve Range (streaming) desteği
app.use('/public', (req, res, next) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Cross-Origin-Resource-Policy', 'cross-origin');
    next();
}, express.static(path.join(__dirname, 'public')));

app.use(express.static(path.join(__dirname)));

// --- VERİTABANI ---
const pool = new Pool({
    user: process.env.DB_USER || 'postgres',
    host: process.env.DB_HOST || 'localhost',
    database: process.env.DB_NAME || 'update_db',
    password: process.env.DB_PASSWORD || 'root',
    port: 5432,
});

pool.query('SELECT NOW()', async (err) => {
    if (err) console.error("❌ PostgreSQL bağlantısı başarısız!");
    else {
        console.log("✅ PostgreSQL bağlantısı kuruldu.");
        // Sütunları otomatik ekle (varsa hata vermez)
        const migrations = [
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS answers TEXT;",
            "ALTER TABLE messages ADD COLUMN IF NOT EXISTS image_url TEXT;",
            "ALTER TABLE messages ADD COLUMN IF NOT EXISTS audio_url TEXT;",
            "ALTER TABLE messages ADD COLUMN IF NOT EXISTS is_read BOOLEAN DEFAULT FALSE;",
        ];
        for (const sql of migrations) {
            try { await pool.query(sql); } catch(e) { /* zaten var */ }
        }
        console.log("✅ Veritabanı sütunları hazır.");
    }
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
const upload = multer({
    storage,
    limits: { fileSize: 20 * 1024 * 1024 }, // 20MB (ses/video için)
    fileFilter: (req, file, cb) => {
        // Resim, ses ve video kabul et
        const allowed = /image|audio|video/;
        if (allowed.test(file.mimetype)) cb(null, true);
        else cb(new Error('Desteklenmeyen dosya türü'));
    }
});

// ==========================================
// YARDIMCI: BURÇ HESAPLAMA
// ==========================================

function calcZodiac(birthDate) {
    if (!birthDate) return null;
    const d = new Date(birthDate);
    const month = d.getMonth() + 1;
    const day = d.getDate();
    if ((month === 3 && day >= 21) || (month === 4 && day <= 19)) return 'Koç';
    if ((month === 4 && day >= 20) || (month === 5 && day <= 20)) return 'Boğa';
    if ((month === 5 && day >= 21) || (month === 6 && day <= 20)) return 'İkizler';
    if ((month === 6 && day >= 21) || (month === 7 && day <= 22)) return 'Yengeç';
    if ((month === 7 && day >= 23) || (month === 8 && day <= 22)) return 'Aslan';
    if ((month === 8 && day >= 23) || (month === 9 && day <= 22)) return 'Başak';
    if ((month === 9 && day >= 23) || (month === 10 && day <= 22)) return 'Terazi';
    if ((month === 10 && day >= 23) || (month === 11 && day <= 21)) return 'Akrep';
    if ((month === 11 && day >= 22) || (month === 12 && day <= 21)) return 'Yay';
    if ((month === 12 && day >= 22) || (month === 1 && day <= 19)) return 'Oğlak';
    if ((month === 1 && day >= 20) || (month === 2 && day <= 18)) return 'Kova';
    return 'Balık';
}

// ==========================================
// AUTH
// ==========================================

app.post('/api/register', async (req, res) => {
    const { full_name, email, password, gender, birth_date, interests, bio, answers } = req.body;
    try {
        const check = await pool.query("SELECT id FROM users WHERE email = $1", [email]);
        if (check.rows.length > 0)
            return res.status(400).json({ success: false, message: "Bu e-posta zaten kullanımda!" });

        const hashed = await bcrypt.hash(password, 10);
        const answersStr = answers ? (typeof answers === 'object' ? JSON.stringify(answers) : answers) : null;
        // Doğum tarihinden otomatik burç hesapla
        const zodiac = calcZodiac(birth_date);
        const result = await pool.query(
            `INSERT INTO users (full_name, email, password, gender, birth_date, zodiac, interests, bio, answers) 
             VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9) RETURNING *`,
            [full_name, email, hashed, gender, birth_date, zodiac, interests, bio, answersStr]
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
    let { email, password } = req.body;
    
    // Debug log to see exact input from client
    console.log(`[LOGIN ATTEMPT] Raw email: "${email}", Raw password: "${password}"`);
    
    email = email ? email.trim() : '';
    password = password ? password.trim() : '';
    
    console.log(`[LOGIN ATTEMPT] Trimmed email: "${email}", Trimmed password: "${password}"`);

    try {
        const result = await pool.query("SELECT * FROM users WHERE email ILIKE $1", [email]);
        console.log(`[LOGIN ATTEMPT] DB search for "${email}" returned ${result.rows.length} rows.`);
        
        if (result.rows.length === 0) {
            console.log(`[LOGIN FAILED] No user found for ${email}`);
            return res.status(401).json({ success: false, message: "E-posta veya şifre hatalı!" });
        }

        const user = result.rows[0];
        console.log(`[LOGIN ATTEMPT] User found in DB. DB password starts with: "${user.password.substring(0, 10)}..."`);
        
        // Hem hash'li hem düz metin şifreleri destekle (geçiş dönemi için)
        let valid = false;
        if (user.password.startsWith('$2b$')) {
            valid = await bcrypt.compare(password, user.password);
        } else {
            valid = password === user.password;
            if (valid) {
                const hashed = await bcrypt.hash(password, 10);
                await pool.query("UPDATE users SET password = $1 WHERE id = $2", [hashed, user.id]);
            }
        }

        if (!valid) {
            console.log(`[LOGIN FAILED] Invalid password for ${email}`);
            return res.status(401).json({ success: false, message: "E-posta veya şifre hatalı!" });
        }

        console.log(`[LOGIN SUCCESS] ${email} logged in successfully.`);
        const token = jwt.sign({ id: user.id, email }, JWT_SECRET, { expiresIn: '30d' });
        res.json({ success: true, token, user: { ...user, password: undefined } });
    } catch (err) {
        console.error("[LOGIN ERROR]:", err.message);
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
        if (profile_pic !== undefined && profile_pic !== null && profile_pic !== '') {
            await pool.query(
                "UPDATE users SET bio=$1, interests=$2, profile_pic=$3 WHERE id=$4",
                [bio, interests, profile_pic, req.params.id]
            );
        } else {
            // Fotoğrafı KORUYARAK güncelle
            await pool.query(
                "UPDATE users SET bio=$1, interests=$2 WHERE id=$3",
                [bio, interests, req.params.id]
            );
        }
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
// UYUM DETAYI
// ==========================================

app.get('/api/compatibility/:myId/:otherId', async (req, res) => {
    const myId = parseInt(req.params.myId);
    const otherId = parseInt(req.params.otherId);
    try {
        const [meRes, otherRes] = await Promise.all([
            pool.query("SELECT interests, answers FROM users WHERE id=$1", [myId]),
            pool.query("SELECT interests, answers FROM users WHERE id=$1", [otherId])
        ]);
        const me = meRes.rows[0];
        const other = otherRes.rows[0];

        const myInterests = me?.interests?.split(',').map(i => i.trim()).filter(Boolean) || [];
        const otherInterests = other?.interests?.split(',').map(i => i.trim()).filter(Boolean) || [];
        const commonInterests = myInterests.filter(i => otherInterests.includes(i));

        let myAnswers = {}, otherAnswers = {};
        try { myAnswers = JSON.parse(me?.answers || '{}'); } catch(e) {}
        try { otherAnswers = JSON.parse(other?.answers || '{}'); } catch(e) {}

        const commonAnswers = [];
        for (const key of Object.keys(myAnswers)) {
            if (otherAnswers[key] && otherAnswers[key] === myAnswers[key]) {
                commonAnswers.push({ question: key, answer: myAnswers[key] });
            }
        }

        // Ağırlıklı uyum: %60 ilgi, %40 cevap
        const combined = new Set([...myInterests, ...otherInterests]);
        const interestRate = combined.size > 0 ? (commonInterests.length / combined.size) : 0;
        const answerTotal = Object.keys(myAnswers).filter(k => otherAnswers[k]).length;
        const answerRate = answerTotal > 0 ? (commonAnswers.length / answerTotal) : 0;
        const matchRate = Math.round((interestRate * 0.6 + answerRate * 0.4) * 100);

        res.json({ matchRate, commonInterests, commonAnswers, totalAnswers: answerTotal });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// KEŞFET
// ==========================================

app.get('/api/discover/:userId', async (req, res) => {
    const myId = parseInt(req.params.userId);
    const genderFilter = req.query.gender;
    const minAge = req.query.minAge ? parseInt(req.query.minAge) : null;
    const maxAge = req.query.maxAge ? parseInt(req.query.maxAge) : null;
    const zodiacFilter = req.query.zodiac;

    try {
        const meResult = await pool.query("SELECT interests, answers FROM users WHERE id=$1", [myId]);
        const myInterests = meResult.rows[0]?.interests?.split(',').map(i => i.trim()) || [];
        let myAnswers = {};
        try { myAnswers = JSON.parse(meResult.rows[0]?.answers || '{}'); } catch(e) {}

        const seenResult = await pool.query("SELECT liked_id FROM likes WHERE liker_id=$1", [myId]);
        const seenIds = [...seenResult.rows.map(r => r.liked_id), myId];

        // Süper beğeni — beni superlike yapanlar
        const superlikersRes = await pool.query(
            "SELECT liker_id FROM likes WHERE liked_id=$1 AND is_like=2", [myId]
        );
        const superlikerIds = new Set(superlikersRes.rows.map(r => r.liker_id));

        let query = "SELECT * FROM users WHERE id <> ALL($1)";
        let params = [seenIds];
        let paramIndex = 2;

        if (genderFilter && genderFilter !== 'all') {
            query += ` AND LOWER(gender) = LOWER($${paramIndex++})`;
            params.push(genderFilter);
        }
        if (minAge !== null) {
            query += ` AND EXTRACT(YEAR FROM AGE(birth_date)) >= $${paramIndex++}`;
            params.push(minAge);
        }
        if (maxAge !== null) {
            query += ` AND EXTRACT(YEAR FROM AGE(birth_date)) <= $${paramIndex++}`;
            params.push(maxAge);
        }
        if (zodiacFilter && zodiacFilter !== 'all') {
            query += ` AND LOWER(zodiac) = LOWER($${paramIndex++})`;
            params.push(zodiacFilter);
        }

        const others = await pool.query(query, params);

        const scoredUsers = others.rows.map(user => {
            const userInterests = user.interests?.split(',').map(i => i.trim()) || [];
            const interestMatches = userInterests.filter(h => myInterests.includes(h));
            const combined = new Set([...myInterests, ...userInterests]);
            const interestRate = combined.size > 0 ? (interestMatches.length / combined.size) : 0;

            let answerMatches = 0, answerTotal = 0;
            try {
                const theirAnswers = JSON.parse(user.answers || '{}');
                for (const key of Object.keys(myAnswers)) {
                    if (theirAnswers[key]) {
                        answerTotal++;
                        if (theirAnswers[key] === myAnswers[key]) answerMatches++;
                    }
                }
            } catch(e) {}
            const answerRate = answerTotal > 0 ? (answerMatches / answerTotal) : 0;
            const matchRate = Math.round((interestRate * 0.6 + answerRate * 0.4) * 100);

            return {
                ...user,
                password: undefined,
                match_rate: matchRate,
                is_superliked_by_me: superlikerIds.has(user.id) // biz onu superlike yaptık mı? (keşfette gösterme ama)
            };
        });

        // Superlike yapanları öne çıkar (opsiyonel)
        scoredUsers.sort((a, b) => b.match_rate - a.match_rate);
        res.json(scoredUsers);
    } catch (err) { console.error(err); res.status(500).json([]); }
});

app.post('/api/like', async (req, res) => {
    const { liker_id, liked_id, is_like } = req.body;
    try {
        // is_like: 0=dislike, 1=like, 2=superlike
        await pool.query("INSERT INTO likes (liker_id, liked_id, is_like) VALUES ($1,$2,$3) ON CONFLICT DO NOTHING",
            [liker_id, liked_id, is_like]);
        let isMatch = false;
        if (is_like >= 1) {
            const check = await pool.query(
                "SELECT id FROM likes WHERE liker_id=$1 AND liked_id=$2 AND is_like >= 1",
                [liked_id, liker_id]
            );
            isMatch = check.rows.length > 0;
        }
        res.json({ success: true, isMatch, isSuperLike: is_like === 2 });
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
            JOIN likes l1 ON u.id = l1.liked_id AND l1.liker_id = $1 AND l1.is_like >= 1
            JOIN likes l2 ON u.id = l2.liker_id AND l2.liked_id = $1 AND l2.is_like >= 1`, [myId]);
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
    const { sender_id, receiver_id, message, image_url, audio_url } = req.body;
    try {
        const result = await pool.query(
            "INSERT INTO messages (sender_id, receiver_id, message, image_url, audio_url) VALUES ($1,$2,$3,$4,$5) RETURNING *",
            [sender_id, receiver_id, message || '', image_url || null, audio_url || null]);
        const msg = result.rows[0];
        // Socket.io ile karşı tarafa anlık ilet
        io.to(`user_${receiver_id}`).emit('new_message', msg);
        res.json({ success: true, message: msg });
    } catch (err) { res.status(500).json({ success: false }); }
});

// Mesajları okundu olarak işaretle
app.put('/api/messages/read/:myId/:otherId', async (req, res) => {
    const { myId, otherId } = req.params;
    try {
        await pool.query(
            "UPDATE messages SET is_read=TRUE WHERE receiver_id=$1 AND sender_id=$2 AND is_read=FALSE",
            [myId, otherId]
        );
        // Karşı tarafa "okundu" eventi gönder
        io.to(`user_${otherId}`).emit('messages_read', { by: parseInt(myId) });
        res.json({ success: true });
    } catch (err) { res.status(500).json({ success: false }); }
});

// ==========================================
// UPLOAD
// ==========================================

app.post('/api/upload', authMiddleware, upload.single('profil_resmi'), async (req, res) => {
    if (!req.file) return res.status(400).json({ success: false, message: "Dosya seçilmedi!" });
    const filename = req.file.filename;
    const picPath = `/public/uploads/${filename}`;
    await pool.query("UPDATE users SET profile_pic=$1 WHERE id=$2", [picPath, req.user.id]);
    res.json({ success: true, filename, url: picPath });
});

// Sohbet için dosya yükleme (fotoğraf veya ses)
app.post('/api/upload-message-image', authMiddleware, upload.single('image'), async (req, res) => {
    if (!req.file) return res.status(400).json({ success: false, message: "Dosya seçilmedi!" });
    const filename = req.file.filename;
    const filePath = `/public/uploads/${filename}`;
    res.json({ success: true, filename, url: filePath });
});

// Ses mesajı yükleme
app.post('/api/upload-audio', authMiddleware, upload.single('audio'), async (req, res) => {
    if (!req.file) return res.status(400).json({ success: false, message: "Ses dosyası seçilmedi!" });
    const filename = req.file.filename;
    const audioPath = `/public/uploads/${filename}`;
    res.json({ success: true, filename, url: audioPath });
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

    // Typing indicator
    socket.on('typing', ({ fromId, toId }) => {
        io.to(`user_${toId}`).emit('user_typing', { fromId });
    });

    socket.on('stop_typing', ({ fromId, toId }) => {
        io.to(`user_${toId}`).emit('user_stop_typing', { fromId });
    });

    // ─── WebRTC Arama Sinyalleri ──────────────────────────────
    // Arama teklifi
    socket.on('call_offer', ({ fromId, toId, offer, callType, callerName }) => {
        console.log(`📞 Arama: ${fromId} → ${toId} (${callType})`);
        io.to(`user_${toId}`).emit('call_offer', { fromId, offer, callType, callerName });
    });

    // Arama yanıtı
    socket.on('call_answer', ({ fromId, toId, answer }) => {
        io.to(`user_${toId}`).emit('call_answer', { fromId, answer });
    });

    // ICE candidate exchange
    socket.on('ice_candidate', ({ fromId, toId, candidate }) => {
        io.to(`user_${toId}`).emit('ice_candidate', { fromId, candidate });
    });

    // Aramayı bitir
    socket.on('call_end', ({ fromId, toId }) => {
        io.to(`user_${toId}`).emit('call_end', { fromId });
        console.log(`📵 Arama bitti: ${fromId} → ${toId}`);
    });

    // Aramayı reddet
    socket.on('call_reject', ({ fromId, toId }) => {
        io.to(`user_${toId}`).emit('call_reject', { fromId });
        console.log(`❌ Arama reddedildi: ${fromId} → ${toId}`);
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