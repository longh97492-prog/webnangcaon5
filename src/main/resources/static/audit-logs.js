const element = id => document.getElementById(id);
    let currentPage = 0;
    let activeFilters = new URLSearchParams({size: "20"});
    let busy = false;

    function showMessage(text, error = false) {
        element("message").textContent = text;
        element("message").className = error ? "error" : "";
    }

    function clearResults() {
        element("rows").replaceChildren();
        element("pageInfo").textContent = "Chưa có kết quả";
        element("previousButton").disabled = true;
        element("nextButton").disabled = true;
    }

    async function request(url, options = {}) {
        const config = {...options, credentials: "same-origin"};
        const method = (config.method || "GET").toUpperCase();
        if (["POST", "PUT", "PATCH", "DELETE"].includes(method)) {
            const tokenResponse = await fetch("/api/auth/csrf", {
                credentials: "same-origin",
                cache: "no-store"
            });
            if (!tokenResponse.ok) {
                throw new Error("Không lấy được CSRF token.");
            }
            const csrf = await tokenResponse.json();
            config.headers = {...(config.headers || {}), [csrf.headerName]: csrf.token};
        }
        const response = await fetch(url, config);

        if (response.status === 401) {
            throw new Error("HTTP 401 — Vui lòng đăng nhập.");
        }

        if (response.status === 403) {
            throw new Error(
                "HTTP 403 — Không đủ quyền xem nhật ký. Yêu cầu ADMIN.");
        }

        if (!response.ok) {
            let message = `HTTP ${response.status}`;
            try {
                const error = await response.json();
                if (error.message) message += " — " + error.message;
            } catch (_) {
                // Giữ thông báo HTTP nếu phản hồi không phải JSON.
            }
            throw new Error(message);
        }

        return response;
    }

    async function loadPage(page) {
        if (busy) return;
        busy = true;
        clearResults();
        showMessage("Đang tải nhật ký...");

        try {
            const params = new URLSearchParams(activeFilters);
            params.set("page", page);

            const response = await request(
                "/api/admin/audit-logs?" + params.toString());
            const data = await response.json();

            for (const log of data.content) {
                const row = document.createElement("tr");

                for (const value of [
                    log.id,
                    log.username,
                    log.action,
                    log.entityName,
                    log.createdAt
                ]) {
                    const cell = document.createElement("td");
                    cell.textContent = value ?? "";
                    row.appendChild(cell);
                }

                element("rows").appendChild(row);
            }

            currentPage = data.page;

            element("pageInfo").textContent = data.totalPages === 0
                ? "0 bản ghi — 0 trang"
                : `Trang ${data.page + 1}/${data.totalPages}`
                  + ` — Tổng ${data.totalElements} bản ghi`;

            element("previousButton").disabled =
                data.first || data.totalPages === 0;
            element("nextButton").disabled = data.last;

            showMessage(data.content.length
                ? "HTTP 200 — Tải nhật ký thành công."
                : "HTTP 200 — Không có nhật ký phù hợp.");
        } catch (error) {
            clearResults();
            showMessage(error.message, true);
        } finally {
            busy = false;
        }
    }

    element("loginForm").addEventListener("submit", async event => {
        event.preventDefault();
        if (busy) return;
        clearResults();

        const username = element("loginUsername").value.trim();
        const password = element("loginPassword").value;
        element("loginPassword").value = "";

        try {
            // Kết thúc phiên cũ trước khi thử tài khoản khác.
            await request("/api/auth/logout", {method: "POST"});
            element("accountStatus").textContent = "Chưa đăng nhập.";

            const response = await request("/api/auth/login", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({username, password})
            });

            const account = await response.json();

            element("accountStatus").textContent =
                `Đã đăng nhập: ${account.username} — ${account.role}`;

            await loadPage(0);
        } catch (error) {
            showMessage(error.message, true);
        }
    });

    element("logoutButton").addEventListener("click", async () => {
        if (busy) return;

        try {
            await request("/api/auth/logout", {method: "POST"});
            clearResults();
            element("loginPassword").value = "";
            element("accountStatus").textContent = "Đã đăng xuất.";
            showMessage("Đã kết thúc phiên đăng nhập.");
        } catch (error) {
            showMessage(error.message, true);
        }
    });

    element("filterForm").addEventListener("submit", event => {
        event.preventDefault();
        if (busy) return;

        const fromTime = element("fromTime").value;
        const toTime = element("toTime").value;

        if (fromTime && toTime && fromTime > toTime) {
            clearResults();
            showMessage(
                "Thời điểm bắt đầu phải nhỏ hơn hoặc bằng thời điểm kết thúc.",
                true);
            return;
        }

        activeFilters = new URLSearchParams();

        for (const key of [
            "username", "action", "fromTime", "toTime", "size"
        ]) {
            const value = element(key).value.trim();
            if (value) activeFilters.set(key, value);
        }

        loadPage(0);
    });

    element("resetButton").addEventListener("click", () => {
        if (busy) return;
        element("filterForm").reset();
        activeFilters = new URLSearchParams({size: "20"});
        loadPage(0);
    });

    element("previousButton").addEventListener("click", () => {
        loadPage(currentPage - 1);
    });

    element("nextButton").addEventListener("click", () => {
        loadPage(currentPage + 1);
    });
