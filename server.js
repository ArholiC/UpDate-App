const express = require('express');
const app = express();

app.get('/', (req, res) => {
    res.send('UpDate Projesi Başlatıldı!');
});

const PORT = 5000;
app.listen(PORT, () => {
    console.log(`Sunucu http://localhost:${PORT} adresinde yanıyor!`);
});