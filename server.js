const express = require('express');
const { Pool } = require('pg');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const app = express();

// --- KLASÖR KONTROLÜ ---
// Upload klasörü yoksa otomatik oluşturur, hata almanı engeller
const uploadDir = './public/uploads/';
if (!fs.existsSync(uploadDir)){
    fs.mkdirSync(uploadDir, { recursive: true });
}

// --- MIDDLEWARE ---
app.use(cors());
app.use(express.json());
app.use('/public', express.static('public'));

// --- POSTGRESQL BAĞLANTISI ---
const pool = new Pool({
    user: 'postgres',
    host: 'localhost',
    database: 'update_db', // Burayı kontrol et kanka, database adın bu olmalı
    password: 'admin123',      // Burayı kontrol et, şifren root olmalı
    port: 5432,
});

// BAĞLANTI TESTİ VE HATALARI YAKALAMA
pool.connect((err, client, release) => {
    if (err) {
        return console.error('CRITICAL: Veritabanına bağlanılamadı!', err.stack);
    }
    console.log('PostgreSQL bağlantısı başarıyla kuruldu. Sistem aktif! 🚀');
    release();
});

// --- RESİM YÜKLEME AYARI (MULTER) ---
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, 'profile-' + uniqueSuffix + path.extname(file.originalname));
    }
});
const upload = multer({ 
    storage: storage,
    limits: { fileSize: 5 * 1024 * 1024 }, // Max 5MB
    fileFilter: (req, file, cb) => {
        const filetypes = /jpeg|jpg|png|webp/;
        const mimetype = filetypes.test(file.mimetype);
        const extname = filetypes.test(path.extname(file.originalname).toLowerCase());
        if (mimetype && extname) return cb(null, true);
        cb(new Error("Sadece resim dosyaları yüklenebilir!"));
    }
});

// --- 1. KAYIT ROTASI (Registration) ---
app.post('/api/register', async (req, res) => {
    const { full_name, email, password, gender, birth_date, zodiac, interests } = req.body;
    
    if(!full_name || !email || !password) {
        return res.status(400).json({ success: false, message: "Eksik bilgi kanka!" });
    }

    try {
        const checkUser = await pool.query("SELECT * FROM users WHERE email = $1", [email]);
        if(checkUser.rows.length > 0) {
            return res.status(400).json({ success: false, message: "Bu e-posta zaten kullanımda." });
        }

        const query = `
            INSERT INTO users (full_name, email, password, gender, birth_date, zodiac, interests) 
            VALUES ($1, $2, $3, $4, $5, $6, $7) 
            RETURNING id, full_name, email
        `;
        const values = [full_name, email, password, gender, birth_date, zodiac, interests];
        const result = await pool.query(query, values);
        
        res.status(201).json({ 
            success: true, 
            message: "Hoş geldin kanka!",
            userId: result.rows[0].id 
        });
    } catch (err) {
        console.error("Register Hatası:", err.message);
        res.status(500).json({ success: false, message: "Sunucu hatası oluştu." });
    }
});

// --- 2. GİRİŞ ROTASI (Login) ---
app.post('/api/login', async (req, res) => {
    const { email, password } = req.body;
    try {
        const query = "SELECT * FROM users WHERE email = $1 AND password = $2";
        const result = await pool.query(query, [email, password]);
        
        if (result.rows.length > 0) {
            const user = result.rows[0];
            delete user.password; // Güvenlik için şifreyi geri gönderme
            res.json({ success: true, user });
        } else {
            res.status(401).json({ success: false, message: "E-posta veya şifre hatalı!" });
        }
    } catch (err) {
        console.error("Login Hatası:", err.message);
        res.status(500).json({ success: false });
    }
});

// --- 3. AKILLI KEŞFET ALGORİTMASI (Discovery) ---
app.get('/api/discover/:userId', async (req, res) => {
    const myId = parseInt(req.params.userId);
    try {
        // Senin bilgilerini ve tercihlerini al
        const meQuery = await pool.query("SELECT interests FROM users WHERE id = $1", [myId]);
        if (meQuery.rows.length === 0) return res.status(404).send("User not found");
        
        const myInterests = meQuery.rows[0].interests ? meQuery.rows[0].interests.split(', ') : [];

        // Daha önce kaydırdığın (beğendiğin/sildiğin) ID'leri topla
        const seenResult = await pool.query("SELECT liked_id FROM likes WHERE liker_id = $1", [myId]);
        const seenIds = seenResult.rows.map(r => r.liked_id);
        seenIds.push(myId); // Kendini de listeye ekle

        // SQL: Kara listede olmayan herkesi çek
        const others = await pool.query("SELECT * FROM users WHERE id <> ALL($1)", [seenIds]);
        
        // Matchmaking: Ortak hobi sayısına göre akıllı sıralama
        const sortedUsers = others.rows.sort((a, b) => {
            const interestsA = a.interests ? a.interests.split(', ') : [];
            const interestsB = b.interests ? b.interests.split(', ') : [];
            
            const commonA = interestsA.filter(i => myInterests.includes(i)).length;
            const commonB = interestsB.filter(i => myInterests.includes(i)).length;
            
            return commonB - commonA; // Çok ortak noktası olan en üste
        });

        res.json(sortedUsers);
    } catch (err) {
        console.error("Discover Hatası:", err.message);
        res.status(500).send("Sunucu Hatası");
    }
});

// --- 4. BEĞENİ / KAYDIRMA (Swipe & Match) ---
app.post('/api/like', async (req, res) => {
    const { liker_id, liked_id, is_like } = req.body;
    try {
        // Etkileşimi kaydet
        await pool.query(
            "INSERT INTO likes (liker_id, liked_id, is_like) VALUES ($1, $2, $3)", 
            [liker_id, liked_id, is_like ? 1 : 0]
        );
        
        let isMatch = false;
        if (is_like) {
            // Karşı taraf seni daha önce beğenmiş mi?
            const checkMatch = await pool.query(
                "SELECT * FROM likes WHERE liker_id = $1 AND liked_id = $2 AND is_like = 1", 
                [liked_id, liker_id]
            );
            if (checkMatch.rows.length > 0) isMatch = true;
        }

        res.json({ success: true, match: isMatch });
    } catch (err) {
        console.error("Like Hatası:", err.message);
        res.status(500).json({ success: false });
    }
});

// --- 5. PROFİL YÖNETİMİ ---
app.get('/api/users/:id', async (req, res) => {
    try {
        const result = await pool.query("SELECT id, full_name, email, bio, gender, zodiac, interests, profile_pic FROM users WHERE id = $1", [req.params.id]);
        if (result.rows.length > 0) res.json(result.rows[0]);
        else res.status(404).send("Bulunamadı");
    } catch (err) {
        res.status(500).send("Hata");
    }
});

app.put('/api/users/:id', async (req, res) => {
    const { bio, interests, profile_pic } = req.body;
    try {
        await pool.query(
            "UPDATE users SET bio = $1, interests = $2, profile_pic = $3 WHERE id = $4", 
            [bio, interests, profile_pic, req.params.id]
        );
        res.json({ success: true, message: "Profil güncellendi kanka!" });
    } catch (err) {
        console.error(err);
        res.status(500).send("Hata oluştu");
    }
});

// --- 6. RESİM YÜKLEME ---
app.post('/api/upload', upload.single('profil_resmi'), (req, res) => {
    if (!req.file) return res.status(400).json({ success: false, message: "Dosya yüklenemedi!" });
    
    // Web üzerinden erişilebilir URL'yi döndürür
    const fileUrl = `/public/uploads/${req.file.filename}`;
    res.json({ success: true, url: fileUrl });
});

// --- 7. EŞLEŞMELERİ LİSTELE (Matches List) ---
app.get('/api/matches/:userId', async (req, res) => {
    const myId = req.params.userId;
    try {
        const query = `
            SELECT u.id, u.full_name, u.profile_pic, u.bio 
            FROM users u
            INNER JOIN likes l1 ON u.id = l1.liked_id
            INNER JOIN likes l2 ON u.id = l2.liker_id
            WHERE l1.liker_id = $1 AND l1.is_like = 1
            AND l2.liked_id = $1 AND l2.is_like = 1
        `;
        const matches = await pool.query(query, [myId]);
        res.json(matches.rows);
    } catch (err) {
        console.error(err);
        res.status(500).send("Hata");
    }
});

// --- ERROR HANDLING (404) ---
app.use((req, res) => {
    res.status(404).json({ message: "Burası ıssız kanka, aradığın rotayı bulamadım." });
});

const PORT = 5000;
app.listen(PORT, () => {
    console.log(`=========================================`);
    console.log(`🚀 UpDate Server Arşa Çıktı: ${PORT}`);
    console.log(`🔥 Veritabanı: PostgreSQL (update_db)`);
    console.log(`📸 Uploads: /public/uploads/`);
    console.log(`=========================================`);
});