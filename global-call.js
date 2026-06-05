/**
 * global-call.js
 * Tüm sayfalarda çalışan global gelen arama dinleyici.
 * Her HTML sayfasına <script src="global-call.js"> ile dahil edilir.
 */
(function () {
    const myId = parseInt(localStorage.getItem('loggedInUserId') || '0');
    if (!myId) return; // Giriş yapılmamışsa çalışma

    // chat.html kendi call_offer listener'ına sahip — çakışma önle
    const currentPage = window.location.pathname.split('/').pop();
    if (currentPage === 'chat.html' || currentPage === 'call.html' || currentPage === 'login.html') return;

    // Socket.IO daha önce yüklenmemişse CDN'den yükle
    function initSocket() {
        if (typeof io === 'undefined') return; // socket.io script'i henüz yüklenmedi

        const socket = io('http://localhost:5000', { reconnection: true });
        socket.emit('join', myId);

        socket.on('connect', () => {
            socket.emit('join', myId);
        });

        let incomingCallerId = null, incomingCallType = 'audio', incomingCallerName = '';

        socket.on('call_offer', ({ fromId, callType, callerName, offer }) => {
            // Eğer zaten arama ekranındaysak tekrar gösterme
            if (window.location.pathname.includes('call.html')) return;

            incomingCallerId = fromId;
            incomingCallType = callType || 'audio';
            incomingCallerName = callerName || 'Kullanıcı';

            // WebRTC SDP offer'ı sessionStorage'a kaydet (call.html callee için)
            if (offer && offer.type && offer.sdp) {
                sessionStorage.setItem('webrtc_offer', JSON.stringify(offer));
                sessionStorage.setItem('webrtc_offer_from', String(fromId));
            } else {
                sessionStorage.removeItem('webrtc_offer');
            }

            showIncomingCall(fromId, callerName, callType);
        });

        socket.on('call_end', ({ fromId }) => {
            if (fromId === incomingCallerId) {
                hideOverlay();
            }
        });

        socket.on('call_reject', ({ fromId }) => {
            hideOverlay();
        });

        // ── Overlay UI ────────────────────────────────────────────────────
        function showIncomingCall(fromId, callerName, callType) {
            if (document.getElementById('__gcall_overlay')) return; // zaten var

            const overlay = document.createElement('div');
            overlay.id = '__gcall_overlay';
            overlay.style.cssText = `
                position: fixed; inset: 0; z-index: 99999;
                display: flex; align-items: flex-end; justify-content: center;
                padding-bottom: 40px;
                background: rgba(5,3,9,0.55);
                backdrop-filter: blur(8px);
                animation: gcFadeIn 0.3s ease;
            `;

            // Animasyon keyframe
            if (!document.getElementById('__gcall_style')) {
                const style = document.createElement('style');
                style.id = '__gcall_style';
                style.textContent = `
                    @import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;600;700;800&display=swap');
                    @keyframes gcFadeIn { from{opacity:0;transform:translateY(20px)} to{opacity:1;transform:translateY(0)} }
                    @keyframes gcPulse { 0%,100%{box-shadow:0 0 0 0 rgba(255,65,108,0.4)} 50%{box-shadow:0 0 0 18px rgba(255,65,108,0)} }
                    #__gcall_card * { font-family: 'Poppins', sans-serif; box-sizing: border-box; }
                `;
                document.head.appendChild(style);
            }

            overlay.innerHTML = `
                <div id="__gcall_card" style="
                    background: rgba(20,12,30,0.95);
                    border: 1px solid rgba(255,65,108,0.3);
                    border-radius: 28px;
                    padding: 28px 32px;
                    width: 340px;
                    box-shadow: 0 24px 80px rgba(0,0,0,0.6), 0 0 40px rgba(255,65,108,0.12);
                    display: flex; flex-direction: column; align-items: center; gap: 12px;
                ">
                    <div style="font-size:12px;color:rgba(255,255,255,0.45);font-weight:600;letter-spacing:0.5px;">
                        ${callType === 'video' ? '📹 Görüntülü Arama' : '📞 Gelen Sesli Arama'}
                    </div>

                    <div id="__gcall_avatar" style="
                        width: 80px; height: 80px; border-radius: 50%;
                        background: rgba(255,65,108,0.15);
                        border: 3px solid rgba(255,65,108,0.5);
                        display: flex; align-items: center; justify-content: center;
                        font-size: 34px; font-weight: 900; color: #ff416c;
                        animation: gcPulse 1.4s infinite;
                    ">${(callerName || '?')[0].toUpperCase()}</div>

                    <div style="font-size:20px;font-weight:800;color:white;">${callerName}</div>
                    <div style="font-size:12px;color:rgba(255,255,255,0.4);">seni arıyor...</div>

                    <div style="display:flex;gap:32px;margin-top:8px;">
                        <div style="display:flex;flex-direction:column;align-items:center;gap:8px;">
                            <button onclick="__gcallAccept()" style="
                                width:62px;height:62px;border-radius:50%;border:none;
                                background:#22c55e;cursor:pointer;font-size:26px;
                                box-shadow:0 8px 24px rgba(34,197,94,0.45);
                                transition:transform 0.15s;
                            " onmouseover="this.style.transform='scale(1.08)'" onmouseout="this.style.transform='scale(1)'">
                                ${callType === 'video' ? '📹' : '📞'}
                            </button>
                            <span style="font-size:11px;color:rgba(255,255,255,0.5);font-weight:600;">Kabul Et</span>
                        </div>
                        <div style="display:flex;flex-direction:column;align-items:center;gap:8px;">
                            <button onclick="__gcallReject()" style="
                                width:62px;height:62px;border-radius:50%;border:none;
                                background:#ef4444;cursor:pointer;font-size:26px;
                                box-shadow:0 8px 24px rgba(239,68,68,0.45);
                                transition:transform 0.15s;
                            " onmouseover="this.style.transform='scale(1.08)'" onmouseout="this.style.transform='scale(1)'">📵</button>
                            <span style="font-size:11px;color:rgba(255,255,255,0.5);font-weight:600;">Reddet</span>
                        </div>
                    </div>
                </div>
            `;

            document.body.appendChild(overlay);

            // Avatar yükleme
            const token = localStorage.getItem('token') || '';
            fetch(`http://localhost:5000/api/users/${fromId}`, {
                headers: { Authorization: `Bearer ${token}` }
            }).then(r => r.json()).then(user => {
                const el = document.getElementById('__gcall_avatar');
                if (el && user.profile_pic) {
                    el.innerHTML = `<img src="http://localhost:5000${user.profile_pic}"
                        style="width:100%;height:100%;border-radius:50%;object-fit:cover;">`;
                    el.style.padding = '0';
                }
            }).catch(() => {});

            // Zil sesi — 30sn sonra otomatik kapat
            const autoClose = setTimeout(() => {
                hideOverlay();
                socket.emit('call_reject', { fromId: myId, toId: incomingCallerId });
            }, 30000);
            overlay._autoClose = autoClose;
        }

        function hideOverlay() {
            const ov = document.getElementById('__gcall_overlay');
            if (ov) {
                if (ov._autoClose) clearTimeout(ov._autoClose);
                ov.remove();
            }
        }

        // Global fonksiyonlar (onclick içinden erişilebilsin)
        window.__gcallAccept = function () {
            // call_answer burada gönderilmiyor!
            // call.html (callee mode) gerçek WebRTC answer üretip gönderecek
            hideOverlay();
            const name = encodeURIComponent(incomingCallerName);
            window.location.href = `call.html?id=${incomingCallerId}&type=${incomingCallType}&mode=callee&name=${name}&auto=1`;
        };

        window.__gcallReject = function () {
            socket.emit('call_reject', { fromId: myId, toId: incomingCallerId });
            hideOverlay();
        };

        // Global socket erişimi (bu sayfanın kendi socket'i yoksa)
        window.__globalCallSocket = socket;
    }

    // Socket.IO yüklendikten sonra başlat
    if (typeof io !== 'undefined') {
        initSocket();
    } else {
        // socket.io CDN'den yükle
        const script = document.createElement('script');
        script.src = 'https://cdn.socket.io/4.7.2/socket.io.min.js';
        script.onload = initSocket;
        document.head.appendChild(script);
    }
})();
