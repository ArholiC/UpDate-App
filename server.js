const express = require('express');
const { Pool } = require('pg');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const app = express();

// --- 1. SİSTEM AYARLARI & DOSYA YÖNETİMİ ---
const uploadDir = path.join(__dirname, 'public/uploads/');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
    console.log("📁 Sistem Notu: Uploads klasörü otomatik oluşturuldu.");
}

// --- 2. MIDDLEWARE YAPILANDIRMASI ---
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use('/public', express.static(path.join(__dirname, 'public')));

// --- 3. DOCKER & LOCAL UYUMLU VERİTABANI BAĞLANTISI ---
const pool = new Pool({
    user: process.env.DB_USER || 'postgres',
    host: process.env.DB_HOST || 'localhost',
    database: process.env.DB_NAME || 'update_db',
    password: process.env.DB_PASSWORD || 'root',
    port: 5432,
});

// Veritabanı Bağlantı Testi (Sunumda Terminalde Gözükmesi Şart)
pool.query('SELECT NOW()', (err, res) => {
    if (err) {
        console.error("❌ KRİTİK HATA: PostgreSQL bağlantısı başarısız!");
    } else {
        console.log("✅ BAŞARILI: PostgreSQL bağlantısı kuruldu. Sistem sunuma hazır.");
    }
});

// --- 4. MULTER (PROFİL RESMİ YÜKLEME) KONFİGÜRASYONU ---
const storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, uploadDir),
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, 'up-' + uniqueSuffix + path.extname(file.originalname));
    }
});

const upload = multer({ 
    storage,
    limits: { fileSize: 5 * 1024 * 1024 }, // Max 5MB
    fileFilter: (req, file, cb) => {
        const filetypes = /jpeg|jpg|png|webp/;
        const mimetype = filetypes.test(file.mimetype);
        const extname = filetypes.test(path.extname(file.originalname).toLowerCase());
        if (mimetype && extname) return cb(null, true);
        cb(new Error("HATA: Sadece resim dosyaları yüklenebilir!"));
    }
});

// ==========================================
// 5. KULLANICI YÖNETİMİ API'LARI
// ==========================================

// Kullanıcı Kaydı (Register)
app.post('/api/register', async (req, res) => {
    const { full_name, email, password, gender, birth_date, zodiac, interests } = req.body;
    try {
        const check = await pool.query("SELECT * FROM users WHERE email = $1", [email]);
        if (check.rows.length > 0) {
            return res.status(400).json({ success: false, message: "Bu e-posta adresi zaten kullanımda!" });
        }

        const query = `INSERT INTO users (full_name, email, password, gender, birth_date, zodiac, interests) 
                       VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING id`;
        const result = await pool.query(query, [full_name, email, password, gender, birth_date, zodiac, interests]);
        res.status(201).json({ success: true, userId: result.rows[0].id });
    } catch (err) {
        console.error("Register Error:", err.message);
        res.status(500).json({ success: false, error: "Sunucu tarafında bir hata oluştu." });
    }
});

// Kullanıcı Girişi (Login)
app.post('/api/login', async (req, res) => {
    const { email, password } = req.body;
    try {
        const result = await pool.query("SELECT * FROM users WHERE email = $1 AND password = $2", [email, password]);
        if (result.rows.length > 0) {
            res.json({ success: true, user: result.rows[0] });
        } else {
            res.status(401).json({ success: false, message: "E-posta veya şifre hatalı!" });
        }
    } catch (err) { 
        res.status(500).json({ success: false, error: "Giriş işlemi başarısız." }); 
    }
});

// Tüm Kullanıcıları Listele (Yönetim/Genel)
app.get('/api/users', async (req, res) => {
    try {
        const result = await pool.query("SELECT id, full_name, profile_pic, zodiac, interests FROM users ORDER BY id DESC");
        res.json(result.rows);
    } catch (err) {
        res.status(500).json({ error: "Veri çekilemedi." });
    }
});

// Tekil Kullanıcı Bilgisi (Profil Sayfası İçin)
app.get('/api/users/:id', async (req, res) => {
    try {
        const result = await pool.query("SELECT * FROM users WHERE id = $1", [req.params.id]);
        if (result.rows.length > 0) res.json(result.rows[0]);
        else res.status(404).json({ message: "Kullanıcı bulunamadı." });
    } catch (err) { 
        res.status(500).json({ error: err.message }); 
    }
});

// ==========================================
// 6. KEŞFET VE AKILLI EŞLEŞTİRME ALGORİTMASI
// ==========================================

app.get('/api/discover/:userId', async (req, res) => {
    const myId = parseInt(req.params.userId);
    try {
        // 1. Kendi hobilerimi diziye çevir
        const meResult = await pool.query("SELECT interests FROM users WHERE id = $1", [myId]);
        const myInterests = meResult.rows[0]?.interests 
            ? meResult.rows[0].interests.split(',').map(i => i.trim()) 
            : [];

        // 2. Beğendiğim veya reddettiğim kişileri filtrele
        const seenResult = await pool.query("SELECT liked_id FROM likes WHERE liker_id = $1", [myId]);
        const seenIds = seenResult.rows.map(r => r.liked_id);
        seenIds.push(myId); // Kendini görme

        // 3. Potansiyel adayları çek
        const others = await pool.query("SELECT * FROM users WHERE id <> ALL($1)", [seenIds]);
        
        // 4. Jaccard Benzerliği ile Yüzde Skoru Hesapla
        const scoredUsers = others.rows.map(user => {
            const userInterests = user.interests 
                ? user.interests.split(',').map(i => i.trim()) 
                : [];
            
            const matches = userInterests.filter(h => myInterests.includes(h));
            
            let matchRate = 0;
            if (myInterests.length > 0 || userInterests.length > 0) {
                const combined = new Set([...myInterests, ...userInterests]);
                matchRate = Math.round((matches.length / combined.size) * 100);
            }
            
            return { ...user, match_rate: matchRate };
        });

        // 5. En uyumludan en düşüğe sırala
        scoredUsers.sort((a, b) => b.match_rate - a.match_rate);
        res.json(scoredUsers);

    } catch (err) { 
        console.error("Algoritma Hatası:", err.message);
        res.status(500).json([]); 
    }
});

// Beğenme/Reddetme İşlemi
app.post('/api/like', async (req, res) => {
    const { liker_id, liked_id, is_like } = req.body;
    try {
        await pool.query("INSERT INTO likes (liker_id, liked_id, is_like) VALUES ($1, $2, $3)", 
                         [liker_id, liked_id, is_like ? 1 : 0]);
        res.json({ success: true });
    } catch (err) { 
        res.status(500).json({ success: false }); 
    }
});

// ==========================================
// 7. MESAJLAŞMA VE EŞLEŞMELER (MATCH)
// ==========================================

// Karşılıklı Eşleşenleri Listele
app.get('/api/matches/:userId', async (req, res) => {
    const myId = parseInt(req.params.userId);
    try {
        const query = `
            SELECT u.id, u.full_name, u.profile_pic, u.bio 
            FROM users u
            JOIN likes l1 ON u.id = l1.liked_id
            JOIN likes l2 ON u.id = l2.liker_id
            WHERE l1.liker_id = $1 AND l1.is_like = 1
            AND l2.liked_id = $1 AND l2.is_like = 1`;

        const result = await pool.query(query, [myId]);
        res.json(result.rows);
    } catch (err) {
        res.status(500).json([]);
    }
});

// Mesajları Getir (Chat Geçmişi)
app.get('/api/messages/:myId/:otherId', async (req, res) => {
    const { myId, otherId } = req.params;
    try {
        const query = `SELECT * FROM messages 
                       WHERE (sender_id = $1 AND receiver_id = $2) 
                       OR (sender_id = $2 AND receiver_id = $1)
                       ORDER BY created_at ASC`;
        const result = await pool.query(query, [myId, otherId]);
        res.json(result.rows);
    } catch (err) { 
        res.status(500).json([]); 
    }
});

// Mesaj Gönder
app.post('/api/messages', async (req, res) => {
    const { sender_id, receiver_id, message } = req.body;
    try {
        await pool.query("INSERT INTO messages (sender_id, receiver_id, message) VALUES ($1, $2, $3)", 
                         [sender_id, receiver_id, message]);
        res.json({ success: true });
    } catch (err) { 
        res.status(500).json({ success: false }); 
    }
});

// ==========================================
// 8. MEDYA VE SUNUCU BAŞLATMA
// ==========================================

// Profil Resmi Yükleme
app.post('/api/upload', upload.single('profil_resmi'), (req, res) => {
    if (!req.file) return res.status(400).json({ success: false, message: "Dosya seçilmedi!" });
    res.json({ success: true, url: `/public/uploads/${req.file.filename}` });
});
// ==========================================
// 9. PROFİL GÜNCELLEME (profile.html için)
// ==========================================
app.put('/api/users/:id', async (req, res) => {
    const { id } = req.params;
    const { bio, interests, profile_pic } = req.body;
    try {
        const query = `
            UPDATE users 
            SET bio = $1, interests = $2, profile_pic = $3 
            WHERE id = $4`;
        await pool.query(query, [bio, interests, profile_pic, id]);
        res.json({ success: true, message: "Profil güncellendi" });
    } catch (err) {
        console.error("Güncelleme Hatası:", err.message);
        res.status(500).json({ success: false, error: err.message });
    }
});

// HESAP SİLME (Opsiyonel ama dökümanda varsa kalsın)
app.delete('/api/users/:id', async (req, res) => {
    try {
        await pool.query("DELETE FROM users WHERE id = $1", [req.params.id]);
        res.json({ success: true });
    } catch (err) { res.status(500).json({ success: false }); }
});

// PORT Yapılandırması
const PORT = process.env.PORT || 5000;
app.listen(PORT, () => {
    console.log(`
    ===========================================
    🚀 UpDate Sunucusu Aktif!
    🔗 Adres: http://localhost:${PORT}
    📡 Veritabanı: PostgreSQL
    📸 Medya Dizini: /public/uploads
    ===========================================
    `);
});