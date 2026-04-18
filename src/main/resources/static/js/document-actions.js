/* Tài lưu đã lưu */
let currentSavedType = "FAVORITE";

const SAVED_TITLE_MAP = {
    FAVORITE: {
        icon: "⭐",
        text: "Tài liệu đã yêu thích"
    },
    SAVED: {
        icon: "🔖",
        text: "Tài liệu đã lưu"
    },
    PINNED: {
        icon: "📌",
        text: "Tài liệu đã ghim"
    }
};

function updateSavedTitle(type) {
    const titleEl = document.getElementById("savedTitle");
    if (!titleEl || !SAVED_TITLE_MAP[type]) return;
    const { icon, text } = SAVED_TITLE_MAP[type];
    titleEl.innerHTML = `${icon} ${text}`;
}

/* lấy param */
function getUrlParam(name) {
    const params = new URLSearchParams(window.location.search);
    return params.get(name);
}

function truncateText(text, maxLength = 20) {
    if (!text) return "";
    return text.length > maxLength
        ? text.substring(0, maxLength) + "…"
        : text;
}

/* lấy giá trị ô nhập trang documents-user */
const keywordInput = document.getElementById("keywordInput");
const categorySelect = document.getElementById("categorySelect");
const searchBtn = document.getElementById("searchBtn");

if (searchBtn) {
    searchBtn.addEventListener("click", searchDocuments);
}

function searchDocuments() {
    if (!keywordInput || !categorySelect) return;

    const keyword = keywordInput.value.trim();
    const categoryId = categorySelect.value;

    // === PHẦN MỚI: Cập nhật URL để thanh địa chỉ có keyword ===
    const url = new URL(window.location.href);

    if (keyword) {
        url.searchParams.set("keyword", keyword);   // hoặc .append nếu muốn nhiều giá trị
    } else {
        url.searchParams.delete("keyword");
    }

    if (categoryId) {
        url.searchParams.set("categoryId", categoryId);
    } else {
        url.searchParams.delete("categoryId");
    }

    // Cập nhật URL mà KHÔNG reload trang (pushState)
    window.history.pushState({}, "", url.toString());
    // ========================================================

    const wrapper = document.getElementById("docResultWrapper");
    wrapper.innerHTML = "<div class='text-muted'>Đang tìm kiếm...</div>";

    const params = new URLSearchParams();
    if (keyword) params.append("keyword", keyword);
    if (categoryId) params.append("categoryId", categoryId);

    fetch(`/user/search-api?${params.toString()}`, {
        credentials: "same-origin"
    })
        .then(res => res.ok ? res.json() : [])
        .then(data => {
            wrapper.innerHTML = "";
            if (!data || data.length === 0) {
                wrapper.innerHTML = "<div class='text-muted'>Tài liệu bạn tìm kiếm không tồn tại</div>";
                return;
            }
            data.forEach(doc => {
                wrapper.innerHTML += renderDocCard(doc);
            });
        })
        .catch(err => {
            console.error("Lỗi tìm kiếm:", err);
            wrapper.innerHTML = "<div class='text-danger'>Lỗi tìm kiếm</div>";
        });
}

/* search documents-user */
document.addEventListener("DOMContentLoaded", () => {
    const categoryIdFromUrl = getUrlParam("categoryId");
    const keywordFromUrl = getUrlParam("keyword");

    if (categoryIdFromUrl && categorySelect) {
        categorySelect.value = categoryIdFromUrl;
    }

    if (keywordFromUrl && keywordInput) {
        keywordInput.value = keywordFromUrl;
    }

    // search
    if ((categoryIdFromUrl || keywordFromUrl) && searchBtn) {
        searchDocuments();
    }
});


/* search từ trang home */
document.querySelectorAll(".category-card").forEach(card => {
    card.addEventListener("click", () => {
        const categoryId = card.dataset.categoryId;
        // Sửa thành đường dẫn đúng với Controller
        window.location.href = `/user/documentsu/document-page?categoryId=${categoryId}`;
    });
});


/* Load section */
function loadSection(url, wrapperId, showDownloadCount = false) {
    const wrapper = document.getElementById(wrapperId);
    if (!wrapper) return;
    wrapper.innerHTML = "<div class='text-muted'>Đang tải...</div>";

    // Gọi API
    fetch(url, { credentials: "same-origin" })
        .then(r => r.ok ? r.json() : [])
        .then(data => {
            wrapper.innerHTML = "";
            if (!data || data.length === 0) {
                wrapper.innerHTML = `<div class="text-muted">Chưa có dữ liệu</div>`;
                return;
            }
            data.forEach(d => {
                wrapper.innerHTML += renderDocCard(d, showDownloadCount);
            });
        })
        .catch(err => {
            console.error("Lỗi tải section:", err);
            wrapper.innerHTML = `<div class="text-danger">Lỗi tải</div>`;
        });
}

/* Top 10 tài liệu mới nhất*/
function loadLatestDocuments() {
    loadSection("/user/latest-api", "latestDocWrapper");
}

/* Tài liệu tải nhiều nhất */
function loadPopularDocuments() {
    loadSection("/user/popular-api", "popularDocWrapper", true); // show download count
}

/* Tài liệu đã lưu, yêu thích, ghim */
function loadSavedDocuments() {
    const wrapper = document.getElementById("savedDocWrapper");

    if (!wrapper) return;
    wrapper.innerHTML = "<div class='text-muted'>Đang tải...</div>";

    const params = new URLSearchParams({ type: currentSavedType });
    fetch(`/user/saved-api?${params}`, { credentials: "same-origin" })
        .then(r => r.ok ? r.json() : [])
        .then(data => {
            wrapper.innerHTML = "";
            if (!data || data.length === 0) {
                wrapper.innerHTML = `<div class="text-muted">Chưa có tài liệu</div>`;
                return;
            }
            data.forEach(d => {
                wrapper.innerHTML += renderDocCard(d);
            });
        })
        .catch(err => {
            console.error("Lỗi tải saved:", err);
            wrapper.innerHTML = `<div class="text-danger">Lỗi tải</div>`;
        });
}

/* Click vào tab lưu, yêu thích, ghim */
document.querySelectorAll(".saved-tabs .tab-btn").forEach(tab => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".saved-tabs .tab-btn")
            .forEach(t => t.classList.remove("active"));
        const type = tab.dataset.type;
        tab.classList.add("active");
        currentSavedType = type;
        updateSavedTitle(type);
        loadSavedDocuments();
    });
});

/* Action trên 1 tài liệu */
function toggleAction(docId, type, icon) {
    fetch(`/user/document-action/${docId}/${type}`, {
        method: "POST",
        credentials: "same-origin"
    }).then(() => {
        const card = icon.closest(".doc-card");
        const wasActive = icon.classList.contains("active");
        icon.classList.toggle("active");
        if (wasActive && currentSavedType === type && card) {
            card.remove();
        }
    });
}

/* Tài liệu liên quan & lịch sử từng xem */
function loadRelatedDocs() {

    const wrapper = document.getElementById("historyDocWrapper");

    if (!wrapper) return;
    wrapper.innerHTML = "<div class='text-muted'>Đang tải...</div>";

    fetch("/user/history-api", { credentials: "same-origin" })
        .then(res => res.ok ? res.json() : [])
        .then(docs => {
            wrapper.innerHTML = "";
            if (!docs || docs.length === 0) {
                wrapper.innerHTML = `<div class="text-muted">Chưa có tài liệu liên quan</div>`;
                return;
            }
            docs.forEach(doc => {
                wrapper.innerHTML += renderDocCard(doc);
            });
        })
        .catch(err => {
            console.error("Lỗi tải history:", err);
            wrapper.innerHTML = `<div class="text-danger">Lỗi tải</div>`;
        });
}

/* Đánh dấu đã xem tài liệu */
function viewDocument(docId) {
    fetch(`/user/document-viewed/${docId}`, {
        method: "POST",
        credentials: "same-origin"
    }).then(() => {
        setTimeout(loadRelatedDocs, 300);
    });
}

/* Doc card lên UI và hiển thị số lần tải */
function renderDocCard(doc, showDownloadCount = false) {
    const viewUrl = `/user/documents/view/${doc.meta}`;
    const downloadUrl = `/user/documents/download/${doc.id}`;
    const previewUrl = `/user/pdf-preview/${doc.id}`;

    return `
<div class="doc-card">
    <a href="${viewUrl}" 
       target="_blank" 
       class="doc-preview-wrapper" 
       onclick="viewDocument(${doc.id})">
        <div class="pdf-page-frame">
            <img src="${previewUrl}" 
                 class="doc-preview-img" 
                 loading="lazy"
                 onerror="this.src='/images/default-pdf.png'"> </div>
    </a>
    
    <div class="doc-actions">
        <button class="btn btn-warning btn-sm me-2 btn-summarize"
            data-id="${doc.id}" type="button">
            Tóm tắt
        </button>
        <div class="action-icons-group">
            <i class="bi bi-star-fill ${doc.favorite ? 'active' : ''}"
               onclick="toggleAction(${doc.id}, 'FAVORITE', this)"></i>
            <i class="bi bi-bookmark-fill ${doc.saved ? 'active' : ''}"
               onclick="toggleAction(${doc.id}, 'SAVED', this)"></i>
            <i class="bi bi-pin-angle-fill ${doc.pinned ? 'active' : ''}"
               onclick="toggleAction(${doc.id}, 'PINNED', this)"></i>  
        </div>
    </div>
    <h6 title="${doc.title}">
        ${truncateText(doc.title, 20)}
    </h6>
    <div class="doc-card-footer">
        <a class="btn btn-outline-success"
           href="#"
           onclick="downloadDoc(${doc.id})">
           ⬇ Tải
        </a>
        
        <a class="btn btn-outline-primary"
            onclick="openShareModal(${doc.id})">
            🔗 Chia sẻ
        </a>
    </div>
    ${
        showDownloadCount && doc.downloadCount !== undefined
            ? `<div class="download-count">⬇ <b>${doc.downloadCount}</b> lượt tải</div>`
            : ""
    }
</div>`;
}

/* Lấy tổng lượt tải của hệ thống */
function loadTotalDownloads() {
    fetch('/api/downloads/total', { credentials: "same-origin" })
        .then(res => res.ok ? res.json() : Promise.reject("Lỗi tải tổng lượt tải"))
        .then(total => {
            const el = document.getElementById('download-count-24');
            if (el) el.innerText = total.toLocaleString();
        })
        .catch(err => {
            console.error("Lỗi tải tổng lượt tải:", err);
            const el = document.getElementById('download-count-24');
            if (el) el.innerText = "—";
        });
}

/* Xử lý tải file và cập nhật dữ liệu */
function downloadDoc(docId) {
    window.open(`/user/documents/download/${docId}`, "_blank");
    setTimeout(() => {
        loadTotalDownloads();
        loadPopularDocuments();
    }, 800);
}

// Xử lý sự kiện click cho toàn trang
document.addEventListener("click", function (e) {
    // Nếu click vào nút có class btn-summarize
    if (e.target.classList.contains("btn-summarize") || e.target.closest(".btn-summarize")) {
        const btn = e.target.classList.contains("btn-summarize") ? e.target : e.target.closest(".btn-summarize");
        const docId = btn.getAttribute("data-id");
        if(docId) {
            window.summarizeDoc(docId); // Gọi hàm tóm tắt
        }
    }
});

/* INIT (HOME) */
document.addEventListener("DOMContentLoaded", () => {
    updateSavedTitle("FAVORITE");
    loadLatestDocuments();
    loadPopularDocuments();
    loadSavedDocuments();
    loadRelatedDocs();
    loadTotalDownloads();
});

/* share tài liệu qua mail và link có thời hạn */
let currentMinutes = 60;

function formatMinutes(minutes) {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;

    if (h > 0 && m > 0) return `${h} giờ ${m.toString().padStart(2, '0')} phút`;
    if (h > 0) return `${h} giờ 00 phút`;
    return `${m} phút`;
}

function updateDisplay() {
    const displayEl = document.getElementById("shareMinutesDisplay");
    const hiddenEl = document.getElementById("shareMinutes");

    if (displayEl) displayEl.value = formatMinutes(currentMinutes);
    if (hiddenEl) hiddenEl.value = currentMinutes;
}

/* Khởi tạo icon tam giác bên trong textbox */
function initShareTimeControls() {
    const btnPlus = document.getElementById("btnPlus");
    const btnMinus = document.getElementById("btnMinus");

    if (!btnPlus || !btnMinus) return;

    btnPlus.addEventListener("click", () => {
        currentMinutes += 5;
        updateDisplay();
    });

    btnMinus.addEventListener("click", () => {
        currentMinutes = Math.max(15, currentMinutes - 5);
        updateDisplay();
    });

    // Giữ chức năng cuộn chuột trên input
    const timeDisplay = document.getElementById("shareMinutesDisplay");
    if (timeDisplay) {
        timeDisplay.addEventListener("wheel", (e) => {
            e.preventDefault();
            if (e.deltaY < 0) {
                currentMinutes += 15;
            } else {
                currentMinutes = Math.max(15, currentMinutes - 15);
            }
            updateDisplay();
        });
    }
}

/* Mở modal chia sẻ */
function openShareModal(docId) {
    shareDocId = docId;
    currentMinutes = 60;
    updateDisplay();

    document.getElementById("shareEmailBox").style.display = "none";
    document.getElementById("shareLinkBox").style.display = "none";
    document.getElementById("btnShareEmail").classList.remove("active");
    document.getElementById("btnShareLink").classList.remove("active");

    const modal = new bootstrap.Modal(document.getElementById("shareModal"));
    modal.show();

    setTimeout(initShareTimeControls, 400);
}

/* Đổi trạng thái nút */
function setActiveShareType(btn) {
    document.querySelectorAll(".share-type-btn").forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
}

/* Mở chia sẻ qua email */
function showShareEmail() {
    document.getElementById("shareEmailBox").style.display = "block";
    document.getElementById("shareLinkBox").style.display = "none";
}

/* Mở chia sẻ qua link */
function showShareLink() {
    document.getElementById("shareEmailBox").style.display = "none";
    document.getElementById("shareLinkBox").style.display = "block";
}

/* ============= GỬI TÀI LIỆU QUA EMAIL ============= */
let isSendingEmail = false;

function shareToEmail() {
    if (isSendingEmail) return; // 🔥 chặn spam

    const btn = event.target;
    btn.disabled = true;
    btn.innerText = "Đang gửi...";

    isSendingEmail = true;

    const email = document.getElementById("shareEmailInput").value.trim();
    const box = document.getElementById("shareEmailResult");

    if (!email) {
        box.innerHTML = `<div class="text-danger">Vui lòng nhập email</div>`;
        btn.disabled = false;
        btn.innerText = "📧 Gửi link";
        isSendingEmail = false;
        return;
    }

    showLoading(); // 🔥 hiệu ứng đang gửi

    const formData = new FormData();
    formData.append("email", email);

    fetch(`/user/share/${shareDocId}/email`, {
        method: "POST",
        body: formData,
        credentials: "same-origin"
    })
        .then(async res => {
            hideLoading();

            if (!res.ok) {
                showMessage("Lỗi máy chủ!", false);
                throw new Error("Server error");
            }

            return res.json();
        })
        .then(data => {
            const fullUrl = data.link;

            box.innerHTML = `
                <div class="alert alert-success">Gửi email thành công!</div>
                <div class="mt-2">
                    <a href="${fullUrl}" target="_blank">${fullUrl}</a>
                </div>
            `;
            btn.innerText = "Đã gửi";

        })
        .catch(err => {
            hideLoading();
            console.error(err);
            showMessage("Gửi mail thất bại!", false); // 🔥 popup thất bại

            // ❗ chỉ mở lại khi lỗi
            btn.disabled = false;
            btn.innerText = "📧 Gửi link";
            isSendingEmail = false;
        });
}

/* ============= TẠO LINK CÓ THỜI HẠN ============= */
let isCreatingLink = false;

function createShareLink() {
    if (isCreatingLink) return;

    const btn = event.target;
    btn.disabled = true;
    btn.innerText = "Đang tạo...";

    isCreatingLink = true;

    const minutes = document.getElementById("shareMinutes").value;
    const box = document.getElementById("shareResult");

    if (!minutes || minutes <= 0) {
        box.innerHTML = `<div class="text-danger">Thời hạn không hợp lệ</div>`;
        btn.disabled = false;
        btn.innerText = "🔗 Tạo link";
        isCreatingLink = false;
        return;
    }

    const formData = new FormData();
    formData.append("minutes", minutes);

    fetch(`/user/share/${shareDocId}`, {
        method: "POST",
        body: formData
    })
        .then(res => {
            if (!res.ok) throw new Error("Server trả về lỗi HTTP");
            return res.json();
        })
        .then(data => {
            const fullUrl = data.link;
            const minutes = document.getElementById("shareMinutes").value;

            box.innerHTML = `
                <div class="alert alert-success">
                    <b>Link chia sẻ:</b><br>
                    <a href="${fullUrl}" target="_blank">${fullUrl}</a>
                    <div class="mt-2 text-muted">
                        ⏱ Thời hạn: ${formatMinutes(minutes)}
                    </div>
                </div>
            `;
            btn.innerText = "Đã tạo link";
        })
        .catch(err => {
            box.innerHTML = `<div class="text-danger">❌ Lỗi tạo link: ${err.message}</div>`;

            // ❗ chỉ cho click lại nếu lỗi
            btn.disabled = false;
            btn.innerText = "🔗 Tạo link";
            isCreatingLink = false;

        });
}

function showLoading() {
    document.getElementById("loadingPopup").style.display = "flex";
}

function hideLoading() {
    document.getElementById("loadingPopup").style.display = "none";
}

function showMessage(msg, isSuccess = true) {
    const box = document.getElementById("messagePopup");
    box.style.display = "block";
    box.style.borderLeft = isSuccess ? "6px solid #28a745" : "6px solid #dc3545";
    box.innerHTML = `<b>${msg}</b>`;

    setTimeout(() => {
        box.style.display = "none";
    }, 2500);
}

// Đảm bảo hàm summarizeDoc có thể được gọi từ bất cứ đâu
window.summarizeDoc = function(docId) {
    const modalEl = document.getElementById('summaryModal');
    if (!modalEl) return;

    // 1. Reset Modal Title và thay icon bằng màu trắng
    const titleEl = modalEl.querySelector('.modal-title');
    if (titleEl) {
        titleEl.innerHTML = `<i class="bi bi-file-earmark-text text-white me-2"></i>Tóm tắt nội dung`;
    }

    const modal = new bootstrap.Modal(modalEl);
    modal.show();

    const content = document.getElementById("summaryContent");

    // 2. Cập nhật HTML Loading - Thêm bố cục và các ngôi sao Bootstrap Icon
    content.innerHTML = `
        <div class="summary-loading-container">
            <div class="summary-spinner-wrapper">
                <div class="summary-spinner"></div>
            </div>
            
            <i class="bi bi-star-fill sparkle-icon s1"></i>
            <i class="bi bi-star-fill sparkle-icon s2"></i>
            <i class="bi bi-star-fill sparkle-icon s3"></i>
            <i class="bi bi-sparkles sparkle-icon s4"></i>
            <i class="bi bi-star-fill sparkle-icon s5"></i>
            
            <p class="loading-text mt-3">AI đang tóm tắt tài liệu...</p>
        </div>`;

    fetch(`/user/summarize/${docId}`, {
        method: "GET",
        credentials: "same-origin"
    })
        .then(res => res.json())
        .then(data => {
            let summary = data.summary || "Không có nội dung";

            content.innerHTML = `
        <div style="white-space: pre-line; color: #1f2937;">
            ${summary}
        </div>`;
        })

};

