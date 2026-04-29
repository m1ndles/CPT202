import { getResourceReviewList } from "/admin/js/api.js";

const pendingCount = document.getElementById("pendingCount");
const pendingCountText = document.getElementById("pendingCountText");
const searchInput = document.getElementById("searchInput");
const categoryFilter = document.getElementById("categoryFilter");
const statusFilter = document.getElementById("statusFilter");
const sortSelect = document.getElementById("sortSelect");
const resourceTableBody = document.getElementById("resourceTableBody");
const emptyState = document.getElementById("emptyState");
const refreshButton = document.getElementById("refreshButton");
const clearFiltersButton = document.getElementById("clearFiltersButton");

const state = {
    search: "",
    category: "All",
    status: "All",
    sort: "desc"
};

function formatDate(value) {
    return new Date(value).toLocaleDateString("en-CA", {
        year: "numeric",
        month: "short",
        day: "numeric"
    });
}

function renderCategories(categories) {
    categoryFilter.innerHTML = categories.map((category) => `
        <option value="${category}" ${category === state.category ? "selected" : ""}>${category === "All" ? "All categories" : category}</option>
    `).join("");
}

function countLabel(status, count) {
    switch (status) {
        case "PENDING_REVIEW":
            return `${count} pending resources`;
        case "REJECTED":
            return `${count} rejected resources`;
        default:
            return `${count} resources in the review queue`;
    }
}

function escapeHtml(value) {
    return String(value || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function renderConversationState(item) {
    if (item.status !== "REJECTED") {
        return `<span class="conversation-muted">Available after rejection</span>`;
    }

    const count = Number(item.appealMessageCount || 0);
    if (!count) {
        return `
            <div class="conversation-state no-thread">
                <strong>No appeal yet</strong>
                <span>Contributor has not sent a message.</span>
            </div>
        `;
    }

    const latestRole = String(item.latestAppealSenderRole || "").toUpperCase();
    const needsReply = latestRole !== "ADMIN";
    return `
        <div class="conversation-state ${needsReply ? "needs-reply" : "admin-replied"}">
            <strong>${needsReply ? "Needs reply" : "Admin replied"}</strong>
            <span>${escapeHtml(item.latestAppealPreview || "Conversation message received.")}</span>
            <small>${escapeHtml(item.latestAppealAt || "")} · ${count} message${count === 1 ? "" : "s"}</small>
        </div>
    `;
}

function renderTable(items) {
    if (!items.length) {
        resourceTableBody.innerHTML = "";
        emptyState.hidden = false;
        return;
    }

    resourceTableBody.innerHTML = items.map((item) => `
        <tr>
            <td>
                <div class="resource-cell">
                    <img class="resource-thumb" src="${escapeHtml(item.thumbnailUrl)}" alt="${escapeHtml(item.title)}">
                    <div>
                        <div class="resource-title">${escapeHtml(item.title)}</div>
                        <div class="resource-subtitle">${escapeHtml(item.category)}</div>
                    </div>
                </div>
            </td>
            <td>${escapeHtml(item.contributor)}</td>
            <td>${escapeHtml(item.category)}</td>
            <td>${escapeHtml(item.place)}</td>
            <td>${formatDate(item.submissionDate)}</td>
            <td><span class="status-badge ${item.status.toLowerCase()}">${item.status.replaceAll("_", " ")}</span></td>
            <td>${renderConversationState(item)}</td>
            <td><a class="primary-button button-link" href="/admin/resource-review-detail.html?id=${encodeURIComponent(item.id)}">${item.status === "REJECTED" ? "Open Record" : "Open Review"}</a></td>
        </tr>
    `).join("");

    emptyState.hidden = true;
}

async function loadResources() {
    refreshButton.disabled = true;
    refreshButton.textContent = "Refreshing...";

    try {
        const data = await getResourceReviewList(state);
        state.category = data.activeCategory;
        pendingCount.textContent = data.displayCount;
        pendingCountText.textContent = countLabel(state.status, data.displayCount);
        renderCategories(data.categories);
        renderTable(data.items);
    } finally {
        refreshButton.disabled = false;
        refreshButton.textContent = "Refresh Queue";
    }
}

function resetFilters() {
    state.search = "";
    state.category = "All";
    state.status = "All";
    state.sort = "desc";
    searchInput.value = "";
    statusFilter.value = "All";
    sortSelect.value = "desc";
    loadResources();
}

let searchTimer;
searchInput.addEventListener("input", (event) => {
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(() => {
        state.search = event.target.value.trim();
        loadResources();
    }, 220);
});

categoryFilter.addEventListener("change", (event) => {
    state.category = event.target.value;
    loadResources();
});

statusFilter.addEventListener("change", (event) => {
    state.status = event.target.value;
    loadResources();
});

sortSelect.addEventListener("change", (event) => {
    state.sort = event.target.value;
    loadResources();
});

refreshButton.addEventListener("click", loadResources);
clearFiltersButton.addEventListener("click", resetFilters);

loadResources();
