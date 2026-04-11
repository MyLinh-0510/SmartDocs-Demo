document.addEventListener("DOMContentLoaded", function () {

    /* ================= FILE TITLE MODAL ================= */

    const fileInput = document.querySelector("input[name='files']");
    const container = document.getElementById("fileTitleContainer");

    function updateDeleteButtons() {
        const removeButtons = document.querySelectorAll(".remove-file");

        if (removeButtons.length === 1) {
            removeButtons[0].disabled = true;
        } else {
            removeButtons.forEach(btn => btn.disabled = false);
        }
    }

    if (fileInput) {
        fileInput.addEventListener("change", function () {

            const files = this.files;
            if (!files || files.length === 0) return;

            container.innerHTML = "";

            for (let i = 0; i < files.length; i++) {

                let fileName = files[i].name;
                let defaultTitle = fileName.replace(/\.[^/.]+$/, "");

                const div = document.createElement("div");
                div.className = "mb-3";

                div.innerHTML = `
                    <div class="row align-items-end">
                        <div class="col-11">
                            <label class="form-label">${fileName}</label>
                            <input type="text"
                                   name="titles"
                                   class="form-control"
                                   value="${defaultTitle}">
                        </div>

                        <div class="col-1 text-end">
                            <button type="button"
                                    class="btn btn-outline-danger remove-file">
                                <i class="bi bi-x-lg"></i>
                            </button>
                        </div>
                    </div>
                `;

                container.appendChild(div);

                div.querySelector(".remove-file").addEventListener("click", function () {
                    div.remove();
                    updateDeleteButtons();
                });
            }

            updateDeleteButtons();

            new bootstrap.Modal(
                document.getElementById('fileTitleModal')
            ).show();
        });
    }

    /* ================= CONFIRM UPLOAD ================= */

    const confirmUploadBtn = document.getElementById("confirmUpload");

    if (confirmUploadBtn) {
        confirmUploadBtn.addEventListener("click", function () {

            if (this.disabled) return;

            this.disabled = true;
            this.innerHTML = "⏳ Đang tải lên...";

            document.getElementById("uploadForm").submit();
        });
    }

    /* ================= UPLOAD BUTTON (ANTI DOUBLE CLICK) ================= */

    const uploadForm = document.getElementById("uploadForm");
    const uploadBtn = document.getElementById("uploadBtn");

    if (uploadForm && uploadBtn) {
        uploadForm.addEventListener("submit", function () {

            if (uploadBtn.disabled) return false;

            uploadBtn.disabled = true;
            uploadBtn.classList.remove("btn-success");
            uploadBtn.classList.add("btn-secondary");
            uploadBtn.innerHTML = "⏳ Đang upload...";
        });
    }

    /* ================= APPROVE / REJECT MODAL ================= */

    const actionModal = document.getElementById('actionModal');

    if (actionModal) {
        actionModal.addEventListener('show.bs.modal', function (event) {

            const button = event.relatedTarget;
            const documentId = button.getAttribute('data-doc-id');
            const action = button.getAttribute('data-action');

            const form = document.getElementById('actionForm');
            const title = document.getElementById('modalTitle');
            const submitBtn = document.getElementById('modalSubmitBtn');

            document.getElementById('actionDocumentId').value = documentId;

            if (action === 'approve') {
                form.action = '/user/documentsu/approve';
                title.innerText = '✍️ Lý do duyệt';
                submitBtn.innerText = 'Xác nhận duyệt';
                submitBtn.className = 'btn btn-success';
            } else {
                form.action = '/user/documentsu/reject';
                title.innerText = '❌ Lý do từ chối';
                submitBtn.innerText = 'Xác nhận từ chối';
                submitBtn.className = 'btn btn-danger';
            }
        });
    }

    /* ================= SUBMIT ACTION (ANTI DOUBLE CLICK) ================= */

    const actionForm = document.getElementById("actionForm");
    const modalSubmitBtn = document.getElementById("modalSubmitBtn");

    if (actionForm && modalSubmitBtn) {
        actionForm.addEventListener("submit", function () {

            if (modalSubmitBtn.disabled) return false;

            modalSubmitBtn.disabled = true;
            modalSubmitBtn.innerHTML = "⏳ Đang xử lý...";
        });
    }

});