/**
 * global-call.js — Tüm sayfalarda çalışan global gelen arama dinleyici.
 */
(function () {
    // chat.html ve call.html kendi listener'larına sahip, çakışma önle
    const currentPage = window.location.pathname.split('/').pop();
    if (currentPage === 'chat.html' || currentPage === 'call.html' || currentPage === 'login.html') return;

    const myId = parseInt(localStorage.getItem('loggedInUserId') || '0');
    if (!myId) return;

    const BASE = 'http://localhost:5005';
    let socket = null;
    let incomingCallerId = null;
    let incomingCallType = 'audio';
    let incomingCallerName = '';

    function initGlobalCall() {
        if (typeof io === 'undefined') {
            // io henüz yüklenmediyse tekrar dene
            setTimeout(initGlobalCall, 300);
            return;
        }

        // Zaten bağlıysa tekrar bağlanma
        if (socket && socket.connected) return;

        socket = io(BASE, {
            reconnection: true,
            reconnectionAttempts: Infinity,
            reconnectionDelay: 1000,
        });

        socket.on('connect', () => {
            socket.emit('join', myId);
            console.log('[GlobalCall] Socket bağlandı, oda:', myId);
        });

        socket.on('reconnect', () => {
            socket.emit('join', myId);
        });

        socket.on('call_offer', ({ fromId, callType, callerName, offer }) => {
            console.log('[GlobalCall] Gelen arama:', fromId, callType);
            incomingCallerId = fromId;
            incomingCallType = callType || 'audio';
            incomingCallerName = callerName || 'Kullanıcı';

            // WebRTC SDP offer'ı sakla
            if (offer && offer.type && offer.sdp) {
                sessionStorage.setItem('webrtc_offer', JSON.stringify(offer));
                sessionStorage.setItem('webrtc_offer_from', String(fromId));
            } else {
                sessionStorage.removeItem('webrtc_offer');
            }

            showIncomingCall(fromId, callerName, callType);
        });

        socket.on('call_end', ({ fromId }) => {
            if (fromId === incomingCallerId) hideOverlay();
        });

        socket.on('call_reject', () => {
            hideOverlay();
        });
    }

    // ── Overlay UI ─────────────────────────────────────────────────────────
    function showIncomingCall(fromId, callerName, callType) {
        if (document.getElementById('__gcall_overlay')) return;

        // Stil ekle (bir kez)
        if (!document.getElementById('__gcall_style')) {
            const style = document.createElement('style');
            style.id = '__gcall_style';
            style.textContent = `
                @import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;600;700;800&display=swap');
                @keyframes gcFadeIn { from{opacity:0;transform:translateY(30px)} to{opacity:1;transform:translateY(0)} }
                @keyframes gcPulse  { 0%,100%{box-shadow:0 0 0 0 rgba(255,65,108,0.5)} 60%{box-shadow:0 0 0 20px rgba(255,65,108,0)} }
                @keyframes gcRing   { 0%,100%{transform:rotate(-8deg)} 50%{transform:rotate(8deg)} }
                #__gcall_card * { font-family:'Poppins',sans-serif; box-sizing:border-box; }
            `;
            document.head.appendChild(style);
        }

        const overlay = document.createElement('div');
        overlay.id = '__gcall_overlay';
        overlay.style.cssText = `
            position:fixed; inset:0; z-index:999999;
            display:flex; align-items:flex-end; justify-content:center;
            padding-bottom:48px;
            background:rgba(5,3,12,0.65);
            backdrop-filter:blur(10px);
            animation:gcFadeIn 0.35s ease;
        `;

        const isVideo = callType === 'video';
        const initial = (callerName || '?')[0].toUpperCase();

        overlay.innerHTML = `
            <div id="__gcall_card" style="
                background:linear-gradient(145deg,rgba(18,10,32,0.98),rgba(25,14,45,0.98));
                border:1px solid rgba(255,65,108,0.35);
                border-radius:32px; padding:32px 36px;
                width:350px; text-align:center;
                box-shadow:0 32px 100px rgba(0,0,0,0.7), 0 0 60px rgba(255,65,108,0.1);
            ">
                <div style="font-size:11px;color:rgba(255,255,255,0.4);font-weight:700;letter-spacing:1px;text-transform:uppercase;margin-bottom:16px;">
                    ${isVideo ? '📹 Görüntülü Arama' : '📞 Sesli Arama'}
                </div>

                <div id="__gcall_avatar" style="
                    width:88px;height:88px;border-radius:50%;margin:0 auto 14px;
                    background:rgba(255,65,108,0.15);
                    border:3px solid rgba(255,65,108,0.6);
                    display:flex;align-items:center;justify-content:center;
                    font-size:36px;font-weight:900;color:#ff416c;
                    animation:gcPulse 1.6s infinite;
                ">${initial}</div>

                <div style="font-size:22px;font-weight:800;color:white;margin-bottom:4px;">${callerName || 'Kullanıcı'}</div>
                <div style="font-size:13px;color:rgba(255,255,255,0.4);margin-bottom:28px;">seni arıyor...</div>

                <div style="display:flex;justify-content:center;gap:48px;">
                    <div style="display:flex;flex-direction:column;align-items:center;gap:10px;">
                        <button id="__gcall_accept_btn" onclick="__gcallAccept()" style="
                            width:68px;height:68px;border-radius:50%;border:none;
                            background:linear-gradient(135deg,#22c55e,#16a34a);
                            cursor:pointer;font-size:28px;
                            box-shadow:0 8px 28px rgba(34,197,94,0.5);
                            transition:transform 0.15s;
                            animation:gcRing 0.5s ease-in-out infinite;
                        ">${isVideo ? '📹' : '📞'}</button>
                        <span style="font-size:12px;color:rgba(255,255,255,0.5);font-weight:600;">Kabul Et</span>
                    </div>
                    <div style="display:flex;flex-direction:column;align-items:center;gap:10px;">
                        <button onclick="__gcallReject()" style="
                            width:68px;height:68px;border-radius:50%;border:none;
                            background:linear-gradient(135deg,#ef4444,#dc2626);
                            cursor:pointer;font-size:28px;
                            box-shadow:0 8px 28px rgba(239,68,68,0.5);
                            transition:transform 0.15s;
                        ">📵</button>
                        <span style="font-size:12px;color:rgba(255,255,255,0.5);font-weight:600;">Reddet</span>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        // Hover efektleri
        const acceptBtn = document.getElementById('__gcall_accept_btn');
        if (acceptBtn) {
            acceptBtn.addEventListener('mouseover', () => acceptBtn.style.transform = 'scale(1.1)');
            acceptBtn.addEventListener('mouseout', () => acceptBtn.style.transform = 'scale(1)');
        }

        // Avatar fotoğrafını yükle
        const token = localStorage.getItem('token') || '';
        fetch(`${BASE}/api/users/${fromId}`, {
            headers: token ? { Authorization: `Bearer ${token}` } : {}
        }).then(r => r.json()).then(user => {
            const el = document.getElementById('__gcall_avatar');
            if (el && user.profile_pic) {
                el.innerHTML = `<img src="${BASE}${user.profile_pic}"
                    style="width:100%;height:100%;border-radius:50%;object-fit:cover;">`;
                el.style.padding = '0';
            }
        }).catch(() => {});

        // 35 saniye sonra otomatik kapat (cevap verilmezse)
        const autoClose = setTimeout(() => {
            hideOverlay();
            if (socket) socket.emit('call_reject', { fromId: myId, toId: incomingCallerId });
        }, 35000);
        overlay._autoClose = autoClose;
    }

    function hideOverlay() {
        const ov = document.getElementById('__gcall_overlay');
        if (ov) {
            if (ov._autoClose) clearTimeout(ov._autoClose);
            ov.style.opacity = '0';
            ov.style.transition = 'opacity 0.25s';
            setTimeout(() => ov.remove(), 250);
        }
    }

    // Global fonksiyonlar (onclick erişimi için window'a ekle)
    window.__gcallAccept = function () {
        hideOverlay();
        const name = encodeURIComponent(incomingCallerName);
        window.location.href = `call.html?id=${incomingCallerId}&type=${incomingCallType}&mode=callee&name=${name}&auto=1`;
    };

    window.__gcallReject = function () {
        if (socket) socket.emit('call_reject', { fromId: myId, toId: incomingCallerId });
        hideOverlay();
    };

    window.__globalCallSocket = socket;

    // Başlat — DOMContentLoaded sonrası güvenli başlatma
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initGlobalCall);
    } else {
        initGlobalCall();
    }
})();
