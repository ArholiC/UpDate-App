const heartBtn = document.getElementById('likeBtn');
const crossBtn = document.getElementById('dislikeBtn');

if (heartBtn && crossBtn) {
    heartBtn.addEventListener('click', () => {
        alert("Beğenildi! Veritabanına kaydedilecek...");
        console.log("Like gönderildi!");
    });

    crossBtn.addEventListener('click', () => {
        alert("Pas geçildi.");
        console.log("Pas geçildi.");
    });
} else {
    console.error("Butonlar bulunamadı! ID'leri kontrol et.");
}