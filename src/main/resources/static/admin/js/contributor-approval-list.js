import { getContributorApprovalList } from "/admin/js/api.js";

const pendingCount = document.getElementById("pendingCount");
const pendingCountText = document.getElementById("pendingCountText");
const searchInput = document.getElementById("searchInput");
const expertiseFilter = document.getElementById("expertiseFilter");
const applicationTableBody = document.getElementById("applicationTableBody");
const emptyState = document.getElementById("emptyState");
const refreshButton = document.getElementById("refreshButton");
const clearFiltersButton = document.getElementById("clearFiltersButton");

const state = {
    search: "",
    expertise: "All"
};

function formatDate(value) {
    if (!value) return "-";
    return new Date(value).toLocaleDateString("en-CA", {
        year: "numeric",
        month: "short",
        day: "numeric"
    });
}

function renderFilters(fields) {
    expertiseFilter.innerHTML = fields.map((field) => `
        <option value="${field}" ${field === state.expertise ? "selected" : ""}>${field === "All" ? "All expertise fields" : field}</option>
    `).join("");
}

function renderTable(items) {
    if (!items.length) {
        applicationTableBody.innerHTML = "";
        emptyState.hidden = false;
        return;
    }

    applicationTableBody.innerHTML = items.map((item) => `
        <tr>
            <td>
                <div class="applicant-cell">
                    <div class="applicant-name">${item.fullName}</div>
                    <div class="applicant-subtitle">@${item.username}</div>
                </div>
            </td>
            <td>${item.expertiseField}</td>
            <td>${formatDate(item.submittedAt)}</td>
            <td><span class="status-badge pending">PENDING</span></td>
            <td><a class="primary-button button-link" href="/admin/contributor-approval-detail.html?id=${item.id}">Review Application</a></td>
        </tr>
    `).join("");

    emptyState.hidden = true;
}

async function loadApplications() {
    refreshButton.disabled = true;
    refreshButton.textContent = "Refreshing...";

    try {
        const data = await getContributorApprovalList(state);
        pendingCount.textContent = data.pendingCount;
        pendingCountText.textContent = `${data.pendingCount} contributor applications awaiting review`;
        renderFilters(data.expertiseFields);
        renderTable(data.items);
    } finally {
        refreshButton.disabled = false;
        refreshButton.textContent = "Refresh List";
    }
}

function resetFilters() {
    state.search = "";
    state.expertise = "All";
    searchInput.value = "";
    loadApplications();
}

let searchTimer;
searchInput.addEventListener("input", (event) => {
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(() => {
        state.search = event.target.value.trim();
        loadApplications();
    }, 220);
});

expertiseFilter.addEventListener("change", (event) => {
    state.expertise = event.target.value;
    loadApplications();
});

refreshButton.addEventListener("click", loadApplications);
clearFiltersButton.addEventListener("click", resetFilters);

loadApplications();
