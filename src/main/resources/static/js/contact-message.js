// ================= GLOBAL =================
// email user hiện tại (lấy từ server)
const CURRENT_USER_EMAIL = (window.CURRENT_USER_EMAIL || "").trim().toLowerCase();

// websocket client
let stompClient = null;

// email người đang chat
let currentRecipientEmail = null;

// lưu thời gian tin nhắn cuối (để hiển thị mốc thời gian)
let lastMsgTime = null;

// tránh bị add nhiều event enter
let enterBound = false;

// ================= CSRF =================
// token csrf để gọi api
const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

// ================= CONNECT WEBSOCKET =================
function connectWS() {
    const socket = new SockJS("/ws");
    stompClient = Stomp.over(socket);

    stompClient.connect({}, () => {
        console.log("✅ WebSocket đã kết nối thành công");

        // Nhận tin nhắn realtime từ server
        stompClient.subscribe("/user/queue/messages", (msg) => {
            const m = JSON.parse(msg.body);

            // nếu đã tồn tại → update thay vì append
            const existing = document.querySelector(`[data-id='${m.id}']`);

            if (existing) {
                const bubble = existing.querySelector(".msg-bubble");

                if (m.deleted) {
                    bubble.innerText = "tin nhắn đã bị xóa";
                    bubble.classList.add("deleted");
                } else {
                    bubble.innerText = m.content;
                }

                // update trạng thái edited
                let meta = existing.querySelector(".msg-meta");
                if (m.edited) {
                    if (!meta) {
                        meta = document.createElement("div");
                        meta.className = "msg-meta";
                        existing.querySelector(".msg-wrapper").prepend(meta);
                    }
                    meta.innerText = "(đã sửa)";
                }

                return; // ❗ không append nữa
            }

            const isMine = m.senderEmail &&
                m.senderEmail.trim().toLowerCase() === CURRENT_USER_EMAIL;

            // Chỉ hiển thị nếu đang chat với người gửi hoặc nhận
            if (m.senderEmail === currentRecipientEmail ||
                m.recipientEmail === currentRecipientEmail) {

                const msgTime = new Date(m.timestamp);

                // Hiển thị mốc thời gian nếu cách > 15 phút
                if (!lastMsgTime || (msgTime - lastMsgTime) > 15 * 60 * 1000) {
                    appendTimeDivider(msgTime);
                }
                lastMsgTime = msgTime;

                appendMessage(
                    m.content || "",
                    isMine,
                    m.id,
                    m.edited,
                    m.deleted || false,
                    m.type || "TEXT",
                    m.fileUrl,
                    m.fileName,
                    m.fileUrls,
                    m.fileNames,
                    m.fileUrls,
                    m.downloadUrls || m.fileUrls
                );
            }
        });
    });
}

// ================= TOGGLE SIDEBAR =================
function toggleChatSidebar() {
    const sidebar = document.getElementById("chatSidebarMain");
    const overlay = document.getElementById("chatOverlay");

    sidebar.classList.toggle("open");

    if (sidebar.classList.contains("open")) {
        overlay.classList.add("show");
        loadRecentContacts();
    } else {
        overlay.classList.remove("show");
    }
}

function closeChatSidebar() {
    const sidebar = document.getElementById("chatSidebarMain");
    const overlay = document.getElementById("chatOverlay");

    sidebar.classList.remove("open");
    overlay.classList.remove("show");
}

// ================= LOAD & RENDER CONTACT =================
function loadRecentContacts() {
    fetch("/chat/recent-contacts")
        .then(res => res.json())
        .then(data => renderContacts(data))
        .catch(() => renderContacts([]));
}

function renderContacts(list) {
    const container = document.getElementById("recentContactsListMain");
    container.innerHTML = "";

    if (!list || list.length === 0) {
        container.innerHTML = "chưa có chat";
        return;
    }

    list.forEach(c => {
        const div = document.createElement("div");
        div.className = "chat-contact-item";
        div.onclick = () => openChat(c.otherEmail, c.otherName, c.avatar);

        div.innerHTML = `
            <img src="/uploads/avatars/${c.avatar || 'default.jpg'}" 
                 onerror="this.src='/uploads/avatars/default.jpg'" 
                 style="width:40px;height:40px;border-radius:50%">
            <div>
                <b>${c.otherName}</b>
                <div>${c.lastMessage || ''}</div>
            </div>`;
        container.appendChild(div);
    });
}

// ================= OPEN & BACK CHAT =================
function openChat(email, name, avatar) {
    currentRecipientEmail = email;
    lastMsgTime = null;

    document.getElementById("chatListView").style.display = "none";
    document.getElementById("chatBoxView").style.display = "flex";
    document.getElementById("chatTitle").innerText = name;
    document.getElementById("chatAvatar").src = "/uploads/avatars/" + (avatar || "default.jpg");

    const box = document.getElementById("contactMesages");
    box.innerHTML = "đang tải...";

    fetch("/chat/history?email=" + email)
        .then(res => res.ok ? res.json() : Promise.reject())
        .then(data => renderMessages(data))
        .catch(() => box.innerHTML = "không tải được tin nhắn");

    // Bind phím Enter chỉ 1 lần
    if (!enterBound) {
        document.getElementById("chatTextInputMain").addEventListener("keydown", function(e) {
            if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                sendMessageFromModal();
            }
        });
        enterBound = true;
    }
}

function backToList() {
    document.getElementById("chatBoxView").style.display = "none";
    document.getElementById("chatListView").style.display = "flex";
}

// ================= SEARCH USER =================
const searchInput = document.getElementById("chatSearchInputMain");

let searchTimeout;

searchInput.addEventListener("input", function () {
    clearTimeout(searchTimeout);

    const keyword = this.value.trim();

    searchTimeout = setTimeout(() => {

        if (!keyword) {
            loadRecentContacts();
            return;
        }

        fetch("/chat/search-user?keyword=" + encodeURIComponent(keyword))
            .then(res => res.json())
            .then(data => renderContacts(data));

    }, 300);
});

// ================= RENDER LỊCH SỬ TIN NHẮN =================
function renderMessages(messages) {
    const box = document.getElementById("contactMesages");
    box.innerHTML = "";

    if (!messages || messages.length === 0) {
        box.innerHTML = "bắt đầu cuộc trò chuyện";
        return;
    }

    let lastTime = null;

    messages.forEach(m => {
        const msgTime = new Date(m.timestamp);
        if (!lastTime || (msgTime - lastTime) > 15 * 60 * 1000) {
            appendTimeDivider(msgTime);
            lastTime = msgTime;
        }

        const isMine = m.senderEmail && m.senderEmail.trim().toLowerCase() === CURRENT_USER_EMAIL;

        appendMessage(
            m.content || "",
            isMine,
            m.id,
            m.edited,
            m.deleted || false,
            m.type || "TEXT",
            m.fileUrl,
            m.fileName,
            m.fileUrls,
            m.fileNames,
            m.fileUrls,                    // viewUrls cho PDF/ảnh
            m.downloadUrls || m.fileUrls   // downloadUrls cho file gốc
        );
    });

    box.scrollTop = box.scrollHeight;
}

// ================= APPEND MESSAGE - ĐÃ SỬA HOÀN CHỈNH =================
function appendMessage(content, isMine, messageId = null, edited = false, deleted = false,
                       type = "TEXT", fileUrl = null, fileName = null,
                       fileUrls = null, fileNames = null,
                       viewUrls = null, downloadUrls = null) {   // thêm 2 tham số mới

    const box = document.getElementById("contactMesages");
    const isAtBottom = box.scrollHeight - box.scrollTop - box.clientHeight < 60;

    const div = document.createElement("div");
    div.className = isMine ? "msg-user" : "msg-admin";
    if (messageId) div.dataset.id = messageId;

    let bubbleHTML = '';

    if (deleted) {
        bubbleHTML = `<div class="msg-bubble deleted">Tin nhắn đã bị xóa</div>`;
    }
    // ==================== ẢNH ====================
    else if (type === "IMAGE") {
        let urls = fileUrls && fileUrls.length > 0 ? fileUrls : (fileUrl ? [fileUrl] : []);
        let names = fileNames && fileNames.length > 0 ? fileNames : (fileName ? [fileName] : []);

        if (urls.length > 1) {
            // nhiều ảnh
            const total = urls.length;
            const showCount = Math.min(4, total);
            const remaining = total > 4 ? total - 4 : 0;

            let imagesHTML = '';
            for (let i = 0; i < showCount; i++) {
                const encoded = encodeURIComponent(JSON.stringify(urls));
                imagesHTML += `
                    <img src="${urls[i]}" 
                         class="chat-image multiple-image" 
                         data-all-urls="${encoded}"
                         onclick="showImagePopupFromElement(this)" 
                         alt="${names[i] || ''}">`;
            }

            let extra = remaining > 0 ?
                `<div class="extra-images-overlay" data-all-urls="${encodeURIComponent(JSON.stringify(urls))}" onclick="showImagePopupFromElement(this)"><span>+${remaining}</span></div>` : '';

            bubbleHTML = `
                <div class="msg-bubble multiple-images-bubble">
                    <div class="images-grid" data-count="${showCount}">
                        ${imagesHTML}${extra}
                    </div>
                </div>`;
        } else if (urls.length === 1) {
            const encoded = encodeURIComponent(JSON.stringify(urls));
            bubbleHTML = `<div class="msg-bubble image-bubble">
                <img src="${urls[0]}" class="chat-image" data-all-urls="${encoded}" 
                     onclick="showImagePopupFromElement(this)" alt="${names[0] || 'Ảnh'}">
            </div>`;
        }
    }
    // ==================== FILE (PDF xem + tải gốc) ====================
    else if (type === "FILE") {
        let viewUrl = null;
        let downloadUrl = null;
        let name = fileName || 'Tài liệu';

        if (viewUrls && viewUrls.length > 0) {
            viewUrl = viewUrls[0];
        } else if (fileUrls && fileUrls.length > 0) {
            viewUrl = fileUrls[0];
        }

        if (downloadUrls && downloadUrls.length > 0) {
            downloadUrl = downloadUrls[0];
        } else if (viewUrl) {
            downloadUrl = viewUrl;
        }

        if (fileNames && fileNames.length > 0) {
            name = fileNames[0];
        }

        if (viewUrl) {
            bubbleHTML = `
            <div class="msg-bubble file-bubble">
                <a href="${viewUrl}" target="_blank" class="file-link"> 
                    📄 ${name} 
                </a>
            </div>`;
        } else {
            bubbleHTML = `<div class="msg-bubble">${content || 'File'}</div>`;
        }
    }
    // ==================== TEXT ====================
    else {
        bubbleHTML = `<div class="msg-bubble">${content || ''}</div>`;
    }

    const metaHTML = (!deleted && edited) ? `<div class="msg-meta">(đã sửa)</div>` : "";

    const actionsHTML = (isMine && !deleted) ? `
        <div class="msg-actions">
            <i class="fa-solid fa-ellipsis" onclick="toggleMenu(this)"></i>
            <div class="msg-menu">
                <div onclick="editMessage(this)">✏️ sửa</div>
                <div onclick="deleteMessage(this)">🗑 xóa</div>
                ${type === "FILE" ? `<div onclick="downloadOriginalFile(this)" data-download-url="${downloadUrls && downloadUrls.length > 0 ? downloadUrls[0] : ''}" data-filename="${fileNames && fileNames.length > 0 ? fileNames[0] : ''}">⬇️ tải gốc</div>` : ''}
            </div>
        </div>` : "";

    div.innerHTML = `
        <div class="msg-wrapper">
            ${metaHTML}
            ${bubbleHTML}
            ${actionsHTML}
        </div>
    `;

    box.appendChild(div);

    if (isMine || isAtBottom) {
        box.scrollTop = box.scrollHeight;
    }
}

// ================= GỬI NHIỀU ẢNH/FILE (CHỈ 1 TIN NHẮN) =================
async function handleMultipleFileSelect(e) {
    const files = Array.from(e.target.files);
    if (files.length === 0 || !currentRecipientEmail) return;

    const box = document.getElementById("contactMesages");

    const loadingDiv = document.createElement("div");
    loadingDiv.id = "uploadProgress";
    loadingDiv.innerHTML = `
        <div>Đang upload ${files.length} file...</div>
        <div style="background:#eee;height:6px;border-radius:4px;margin-top:5px;">
            <div id="progressBar" style="height:100%;width:0%;background:#4caf50;border-radius:4px;"></div>
        </div>
        <div id="progressText">0%</div>
    `;
    box.appendChild(loadingDiv);

    try {
        const formData = new FormData();
        files.forEach(f => formData.append("files", f));

        const xhr = new XMLHttpRequest();
        xhr.open("POST", "/chat/upload-multiple", true);
        xhr.setRequestHeader(csrfHeader, csrfToken);

        xhr.upload.onprogress = function (e) {
            if (e.lengthComputable) {
                const percent = Math.round((e.loaded / e.total) * 100);
                document.getElementById("progressBar").style.width = percent + "%";
                document.getElementById("progressText").innerText = percent + "%";
            }
        };

        xhr.onload = function () {
            if (xhr.status === 200) {
                const data = JSON.parse(xhr.responseText);

                sendMultipleMessage(
                    data.viewUrls,
                    data.downloadUrls,
                    data.fileNames,
                    data.type
                );
            } else {
                alert("Upload lỗi");
            }
            loadingDiv.remove();
        };

        xhr.send(formData);

    } catch (err) {
        console.error(err);
        alert("Upload thất bại");
    }

    e.target.value = "";
}

// ================= GỬI NHIỀU FILE (TÁCH RIÊNG TỪNG FILE) =================
function sendMultipleMessage(viewUrls, downloadUrls, fileNames, type) {
    if (!stompClient || !currentRecipientEmail) return;

    if (type === "IMAGE") {
        const payload = {
            recipientEmail: currentRecipientEmail,
            content: `📷 ${viewUrls.length} ảnh`,
            type: "IMAGE",
            fileUrls: viewUrls,
            fileNames: fileNames
        };

        stompClient.send("/app/chat.send", {}, JSON.stringify(payload));
        return;
    }

    // FILE - gửi từng tin nhắn riêng (hoặc ghép nếu muốn)
    viewUrls.forEach((viewUrl, index) => {
        const downloadUrl = downloadUrls[index] || viewUrl;
        const name = fileNames[index] || "Tài liệu";

        const payload = {
            recipientEmail: currentRecipientEmail,
            content: "",
            type: "FILE",
            fileUrl: viewUrl,
            fileName: name,
            downloadUrls: [downloadUrl],
            fileUrls: [viewUrl],
            fileNames: [name]
        };

        stompClient.send("/app/chat.send", {}, JSON.stringify(payload));
    });
}

// ================= PASTE ẢNH TỪ CLIPBOARD (Ctrl + V) =================
document.addEventListener('paste', async (e) => {
    if (!currentRecipientEmail) return;

    const items = e.clipboardData.items;
    for (let item of items) {
        if (item.type.indexOf('image') !== -1) {
            const blob = item.getAsFile();
            if (!blob) continue;

            const formData = new FormData();
            formData.append("files", blob, "screenshot.png");

            try {
                const res = await fetch("/chat/upload-multiple", {
                    method: "POST",
                    headers: { [csrfHeader]: csrfToken },
                    body: formData
                });

                // Trong phần paste ảnh (Ctrl + V)
                if (res.ok) {
                    const data = await res.json();
                    sendMultipleMessage(
                        data.viewUrls,
                        data.downloadUrls,
                        data.fileNames,
                        data.type
                    );
                }
            } catch (err) {
                console.error("Paste ảnh lỗi:", err);
            }
        }
    }
});

// ================= POPUP XEM ẢNH =================
let currentImageIndex = 0;
let currentImageList = [];

function showImagePopup(url, allUrls = null) {
    currentImageList = allUrls && allUrls.length > 0 ? allUrls : [url];
    currentImageIndex = currentImageList.indexOf(url);
    if (currentImageIndex === -1) currentImageIndex = 0;

    const popup = document.getElementById("imagePopup");
    if (!popup) return console.error("Không tìm thấy imagePopup");

    document.getElementById("popupImage").src = currentImageList[currentImageIndex];
    popup.style.display = "flex";

    updateNavButtons();
}

function showImagePopupFromElement(el) {
    const encoded = el.getAttribute("data-all-urls");
    const src = el.src || el.getAttribute("src");

    if (encoded) {
        try {
            const urls = JSON.parse(decodeURIComponent(encoded));
            showImagePopup(src || urls[0], urls);
        } catch(e) {
            if (src) showImagePopup(src);
        }
    } else if (src) {
        showImagePopup(src);
    }
}

function updateNavButtons() {
    const prev = document.getElementById("prevBtn");
    const next = document.getElementById("nextBtn");
    if (!prev || !next) return;

    if (currentImageList.length > 1) {
        prev.style.display = "flex";
        next.style.display = "flex";
    } else {
        prev.style.display = "none";
        next.style.display = "none";
    }
}

function prevImage() {
    currentImageIndex = (currentImageIndex - 1 + currentImageList.length) % currentImageList.length;
    document.getElementById("popupImage").src = currentImageList[currentImageIndex];
}

function nextImage() {
    currentImageIndex = (currentImageIndex + 1) % currentImageList.length;
    document.getElementById("popupImage").src = currentImageList[currentImageIndex];
}

function hideImagePopup() {
    document.getElementById("imagePopup").style.display = "none";
}


// ================= CÁC HÀM CŨ (giữ nguyên) =================
function sendMessageFromModal() {
    const input = document.getElementById("chatTextInputMain");
    const content = input.value.trim();
    if (!currentRecipientEmail || !content) return;

    sendWS(content);
    input.value = "";
}

function sendWS(content) {
    stompClient.send("/app/chat.send", {}, JSON.stringify({
        recipientEmail: currentRecipientEmail,
        content: content
    }));
}

function toggleMenu(icon) {
    document.querySelectorAll(".msg-menu").forEach(m => m.classList.remove("show"));
    icon.parentElement.querySelector(".msg-menu").classList.toggle("show");
}

// sửa tin nhắn
function editMessage(el) {

    const msgDiv = el.closest(".msg-user");
    if (!msgDiv) return;

    const wrapper = el.closest(".msg-wrapper");
    const bubble = wrapper.querySelector(".msg-bubble");

    let meta = wrapper.querySelector(".msg-meta");
    if (!meta) {
        meta = document.createElement("div");
        meta.className = "msg-meta";
        wrapper.prepend(meta);
    }

    const oldText = bubble.innerText;

    // tránh mở nhiều lần
    if (bubble.querySelector("input")) return;

    bubble.innerHTML = `<input class="edit-input" value="${oldText}">`;

    const input = bubble.querySelector("input");
    input.focus();
    input.select();

    // ===== SAVE =====
    function save() {
        const newText = input.value.trim();

        // nếu không đổi → revert
        if (!newText || newText === oldText) {
            bubble.innerText = oldText;
            return;
        }

        // 🔥 OPTIMISTIC UI (update ngay)
        bubble.innerText = newText;
        meta.innerText = "(đã sửa)";

        const messageId = msgDiv.dataset.id;

        fetch("/chat/edit", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({
                id: messageId,
                content: newText
            })
        }).catch(() => {
            // ❌ nếu lỗi → rollback
            bubble.innerText = oldText;
            meta.innerText = "";
            alert("Lưu thất bại!");
        });
    }

    // ===== CANCEL =====
    function cancel() {
        bubble.innerText = oldText;
    }

    // ===== EVENTS =====
    input.addEventListener("keydown", function (e) {
        if (e.key === "Enter") {
            e.preventDefault();
            save();
        }
        if (e.key === "Escape") {
            cancel();
        }
    });

    // click ra ngoài → cancel
    input.addEventListener("blur", cancel);

    // đóng menu
    document.querySelectorAll(".msg-menu").forEach(m => m.classList.remove("show"));
}

// xóa tin nhắn
function deleteMessage(el) {

    const msgDiv = el.closest(".msg-user");
    if (!msgDiv) return;

    const wrapper = el.closest(".msg-wrapper");
    const bubble = wrapper.querySelector(".msg-bubble");

    const messageId = msgDiv?.dataset?.id;

    // animation
    bubble.style.opacity = "0.5";

    setTimeout(() => {

        bubble.innerText = "tin nhắn đã bị xóa";
        bubble.classList.add("deleted");
        bubble.style.opacity = "1";

        const actions = wrapper.querySelector(".msg-actions");
        if (actions) actions.remove();

    }, 150);

    if (messageId) {
        fetch("/chat/delete", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({ id: messageId })
        });
    }
}

// Tải file gốc
window.downloadOriginalFile = function(el) {
    const url = el.getAttribute("data-download-url");
    const filename = el.getAttribute("data-filename");

    if (!url) return alert("Không tìm thấy link tải!");

    const a = document.createElement("a");
    a.href = url;
    if (filename) a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
};

// hiển thị mốc thời gian
function appendTimeDivider(date) {

    const box = document.getElementById("contactMesages");

    const div = document.createElement("div");
    div.className = "msg-time-divider";

    const timeString = date.toLocaleString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
        day: "2-digit",
        month: "2-digit",
        year: "numeric"
    });

    div.innerText = timeString;

    box.appendChild(div);
}

// ================= INIT =================
window.addEventListener("load", () => {
    connectWS();
});

document.addEventListener("click", function(e) {

    // nếu KHÔNG click vào vùng action (dấu 3 chấm + menu)
    if (!e.target.closest(".msg-actions")) {
        document.querySelectorAll(".msg-menu")
            .forEach(m => m.classList.remove("show"));
    }

});

// Expose các hàm ra global để HTML gọi được
window.toggleChatSidebar = toggleChatSidebar;
window.openChat = openChat;
window.backToList = backToList;
window.showImagePopup = showImagePopup;
window.hideImagePopup = hideImagePopup;
window.prevImage = prevImage;
window.nextImage = nextImage;
window.showImagePopupFromElement = showImagePopupFromElement;
window.updateNavButtons = updateNavButtons;