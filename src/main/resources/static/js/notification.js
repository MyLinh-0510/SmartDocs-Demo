let currentFilter = "all";
let allNotifications = [];

window.addEventListener("load", () => {

    const notifyList = document.getElementById("notifyList");
    const notifyBadge = document.getElementById("notifyBadge");

    if (notifyList) {
        loadNotifications();

        notifyList.addEventListener("click", function (e) {
            const item = e.target.closest(".notify-item");
            if (!item) return;

            e.preventDefault();
            e.stopPropagation();

            openNotify(item);
        });
    }

    if (notifyBadge) {
        loadUnreadCount();
    }

    const tabs = document.querySelectorAll(".notify-tab");

    tabs.forEach(tab => {
        tab.addEventListener("click", (e) => {

            e.preventDefault();
            e.stopPropagation();

            // đổi active UI
            tabs.forEach(t => t.classList.remove("active"));
            tab.classList.add("active");

            // đổi filter
            currentFilter = tab.dataset.filter;

            // render lại
            renderNotifications();
        });
    });

});


/* LOAD NOTIFICATIONS */
function loadNotifications() {
    fetch("/api/notifications/my", {
        credentials: "include"
    })
        .then(res => {
            if (!res.ok) throw new Error("API lỗi: " + res.status);
            return res.json();
        })
        .then(data => {

            allNotifications = data;   // LƯU DATA

            renderNotifications();     // RENDER QUA FILTER

        })
        .catch(err => {
            console.error("Lỗi load notifications:", err);
        });
}

function renderNotifications() {

    const list = document.getElementById("notifyList");

    let filtered = allNotifications;

    if (currentFilter === "unread") {
        filtered = allNotifications.filter(n => !n.isRead);
    }

    if (!filtered || filtered.length === 0) {
        list.innerHTML = `
            <div class="p-3 text-center text-muted">
                Không có thông báo
            </div>`;
        return;
    }

    // CHIA HÔM NAY / TRƯỚC ĐÓ

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const todayList = [];
    const oldList = [];

    filtered.forEach(n => {
        const d = new Date(n.createdAt);

        if (d >= today) {
            todayList.push(n);
        } else {
            oldList.push(n);
        }
    });

    // HTML

    let html = "";

    if (todayList.length > 0) {
        html += `<div class="notify-group-title">Hôm nay</div>`;
        html += todayList.map(renderItem).join("");
    }

    if (oldList.length > 0) {
        html += `<div class="notify-group-title">Trước đó</div>`;
        html += oldList.map(renderItem).join("");
    }

    list.innerHTML = html;
}

function renderItem(n) {
    return `
        <div class="notify-item ${!n.isRead ? 'unread' : ''}"
             data-id="${n.id}"
             data-content="${n.message}"
             data-url="${n.url || ''}"
             data-time="${n.createdAt}"
             data-read="${n.isRead}">              

            <div class="notify-content">
                <div class="notify-title">
                    ${n.message}
                </div>
                <div class="notify-time">
                    ${formatTime(n.createdAt)}
                </div>
            </div>

            ${!n.isRead ? '<div class="notify-dot"></div>' : ''}
        </div>
    `;
}

/* COUNT */
function loadUnreadCount() {
    fetch("/api/notifications/count", {
        credentials: "include"
    })
        .then(res => {
            if (!res.ok) throw new Error("API lỗi: " + res.status);
            return res.text();
        })
        .then(count => {

            const badge = document.getElementById("notifyBadge");

            count = parseInt(count);

            if (count > 0) {
                badge.innerText = count;
                badge.style.display = "inline-block";
            } else {
                badge.style.display = "none";
            }
        })
        .catch(err => console.error("Lỗi count:", err));
}


/* MARK READ */
function markAsRead(id, element) {
    fetch(`/api/notifications/${id}/read`, {
        method: "POST",
        credentials: "include"
    })
        .then(res => {
            if (!res.ok) throw new Error("Mark read lỗi");

            if (element) {
                element.classList.remove("unread");

                const dot = element.querySelector(".notify-dot");
                if (dot) dot.remove();
            }

            // update data local
            const n = allNotifications.find(x => x.id == id);
            if (n) n.isRead = true;

            loadUnreadCount();

            // render lại nếu đang ở tab unread
            renderNotifications();
        })
        .catch(err => console.error(err));
}


/* FORMAT TIME */
function formatTime(time) {
    const date = new Date(time);
    const now = new Date();

    const diff = Math.floor((now - date) / 1000);

    if (diff < 60) return "Vừa xong";
    if (diff < 3600) return Math.floor(diff / 60) + " phút trước";
    if (diff < 86400) return Math.floor(diff / 3600) + " giờ trước";

    return date.toLocaleDateString("vi-VN");
}

function openNotify(element) {

    const id = element.dataset.id;
    const content = element.dataset.content;
    const url = element.dataset.url;
    const rawTime = element.dataset.time;
    const isRead = String(element.dataset.read) === "true";

    // format time
    let formattedTime = "Không rõ thời gian";
    if (rawTime) {
        const d = new Date(rawTime);
        if (!isNaN(d)) {
            formattedTime = d.toLocaleString("vi-VN");
        }
    }

    // xử lý nội dung (có link)
    let messageHtml = content;

    if (url && content.includes(":")) {
        const parts = content.split(":");
        messageHtml = `
            ${parts[0]}:
            <a href="${url}" target="_blank" class="fw-bold">
                ${parts[1].trim()}
            </a>
        `;
    }

    // render HTML
    const html = `
        <div class="notify-detail-wrapper">
            <div class="notify-message mb-2">
                ${messageHtml}
            </div>

            <div class="notify-meta d-flex justify-content-between">
                <div class="notify-time">
                    🕒 ${formattedTime}
                </div>

            </div>
        </div>
    `;

    document.getElementById('notifyDetail').innerHTML = html;

    // 1. ĐÓNG DROPDOWN ĐANG MỞ
    const openedDropdown = document.querySelector('.dropdown-menu.show');

    if (openedDropdown) {
        const toggle = openedDropdown.parentElement.querySelector('[data-bs-toggle="dropdown"]');

        if (toggle) {
            const dropdown = bootstrap.Dropdown.getOrCreateInstance(toggle);
            dropdown.hide();
        }
    }

    // backup (tránh bootstrap giữ state)
    document.querySelectorAll('.dropdown-menu.show').forEach(el => {
        el.classList.remove('show');
    });

    // 3. MỞ MODAL (KHÔNG TẠO TRÙNG)
    const modalEl = document.getElementById('notifyModal');

    let modal = bootstrap.Modal.getInstance(modalEl);
    if (!modal) {
        modal = new bootstrap.Modal(modalEl);
    }

    // tránh mở lại khi đang mở
    if (!modalEl.classList.contains('show')) {
        setTimeout(() => {
            modal.show();
        }, 120);
    }

    // 4. MARK AS READ
    markAsRead(id, element);
}

let notificationStompClient = null;

function connectSocket() {

    const socket = new SockJS('/ws');
    notificationStompClient = Stomp.over(socket);

    notificationStompClient.connect({}, function () {

        console.log("✅ Connected WebSocket");

        notificationStompClient.subscribe("/user/queue/notifications", function (msg) {

            const noti = JSON.parse(msg.body);

            console.log("🔔 New notification:", noti);

            // thêm vào đầu list
            allNotifications.unshift(noti);

            // render lại
            renderNotifications();

            // update badge
            loadUnreadCount();
        });
    });
}

window.addEventListener("load", () => {
    connectSocket(); // 👈 thêm dòng này
});