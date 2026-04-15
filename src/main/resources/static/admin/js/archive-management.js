import {
    getArchiveList,
    restoreArchivedResource
} from "/admin/js/api.js";

const archivedCount = document.getElementById("archivedCount");
const archivedCountText = document.getElementById("archivedCountText");
const searchInput = document.getElementById("searchInput");
const categoryFilter = document.getElementById("categoryFilter");
const sortSelect = document.getElementById("sortSelect");
const clearFiltersButton = document.getElementById("clearFiltersButton");
const refreshButton = document.getElementById("refreshButton");
const archiveTableBody = document.getElementById("archiveTableBody");
const emptyState = document.getElementById("emptyState");

const state = {
    search: "",
    category: "All",
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

function renderTable(items) {
    if (!items.length) {
        archiveTableBody.innerHTML = "";
        emptyState.hidden = false;
        return;
    }

    archiveTableBody.innerHTML = items.map((item) => `
        <tr>
            <td><strong>${item.title}</strong></td>
            <td>${item.contributor}</td>
            <td>${item.category}</td>
            <td>${formatDate(item.archivedAt)}</td>
            <td>${item.archivedBy}</td>
            <td>${item.archiveReason}</td>
            <td>
                <div class="table-actions">
                    <a class="secondary-button button-link compact-action" href="/admin/archive-detail.html?id=${item.id}">View Archive</a>
                    <button class="primary-button compact-action" type="button" data-restore="${item.id}">Restore</button>
                </div>
            </td>
        </tr>
    `).join("");

    emptyState.hidden = true;
}

async function loadArchive() {
    refreshButton.disabled = true;
    refreshButton.textContent = "Refreshing...";

    try {
        const data = await getArchiveList(state);
        archivedCount.textContent = data.archivedCount;
        archivedCountText.textContent = `${data.archivedCount} archived resources`;
        renderCategories(data.categories);
        renderTable(data.items);
    } finally {
        refreshButton.disabled = false;
        refreshButton.textContent = "Refresh Archive";
    }
}

searchInput.addEventListener("input", (event) => {
    state.search = event.target.value.trim();
    loadArchive();
});

categoryFilter.addEventListener("change", (event) => {
    state.category = event.target.value;
    loadArchive();
});

sortSelect.addEventListener("change", (event) => {
    state.sort = event.target.value;
    loadArchive();
});

clearFiltersButton.addEventListener("click", () => {
    state.search = "";
    state.category = "All";
    state.sort = "desc";
    searchInput.value = "";
    sortSelect.value = "desc";
    loadArchive();
});

refreshButton.addEventListener("click", loadArchive);

archiveTableBody.addEventListener("click", async (event) => {
    const restoreButton = event.target.closest("[data-restore]");
    if (!restoreButton) {
        return;
    }

    await restoreArchivedResource(Number(restoreButton.dataset.restore));
    loadArchive();
});

loadArchive();
