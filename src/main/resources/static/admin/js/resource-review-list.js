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
        <option value="${category}" ${category === state.category ? "selected" : ""}>${category}</option>
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
                    <img class="resource-thumb" src="${item.thumbnailUrl}" alt="${item.title}">
                    <div>
                        <div class="resource-title">${item.title}</div>
                        <div class="resource-subtitle">${item.category}</div>
                    </div>
                </div>
            </td>
            <td>${item.contributor}</td>
            <td>${item.category}</td>
            <td>${item.place}</td>
            <td>${formatDate(item.submissionDate)}</td>
            <td><span class="status-badge ${item.status.toLowerCase()}">${item.status.replaceAll("_", " ")}</span></td>
            <td><a class="primary-button button-link" href="/admin/resource-review-detail.html?id=${item.id}">${item.status === "REJECTED" ? "Open Record" : "Open Review"}</a></td>
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
