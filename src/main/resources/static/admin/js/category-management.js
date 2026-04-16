import {
    createCategory,
    createTag,
    getCategoryList,
    getTagList,
    toggleCategoryStatus,
    toggleTagStatus,
    updateCategory,
    updateTag
} from "/admin/js/api.js";

const tabButtons = {
    categories: document.getElementById("categoriesTab"),
    tags: document.getElementById("tagsTab")
};

const panels = {
    categories: document.getElementById("categoriesPanel"),
    tags: document.getElementById("tagsPanel")
};

const activeCategoryCount = document.getElementById("activeCategoryCount");
const activeTagCount = document.getElementById("activeTagCount");

const categoryUi = {
    searchInput: document.getElementById("categorySearchInput"),
    statusFilter: document.getElementById("categoryStatusFilter"),
    clearFiltersButton: document.getElementById("clearCategoryFiltersButton"),
    refreshButton: document.getElementById("refreshCategoriesButton"),
    tableBody: document.getElementById("categoryTableBody"),
    emptyState: document.getElementById("categoryEmptyState"),
    form: document.getElementById("categoryForm"),
    editorTitle: document.getElementById("categoryEditorTitle"),
    nameInput: document.getElementById("categoryNameInput"),
    descriptionInput: document.getElementById("categoryDescriptionInput"),
    resetButton: document.getElementById("resetCategoryButton"),
    feedbackMessage: document.getElementById("categoryFeedbackMessage")
};

const tagUi = {
    searchInput: document.getElementById("tagSearchInput"),
    statusFilter: document.getElementById("tagStatusFilter"),
    clearFiltersButton: document.getElementById("clearTagFiltersButton"),
    refreshButton: document.getElementById("refreshTagsButton"),
    tableBody: document.getElementById("tagTableBody"),
    emptyState: document.getElementById("tagEmptyState"),
    form: document.getElementById("tagForm"),
    editorTitle: document.getElementById("tagEditorTitle"),
    nameInput: document.getElementById("tagNameInput"),
    descriptionInput: document.getElementById("tagDescriptionInput"),
    resetButton: document.getElementById("resetTagButton"),
    feedbackMessage: document.getElementById("tagFeedbackMessage")
};

const state = {
    activeTab: new URLSearchParams(window.location.search).get("tab") === "tags" ? "tags" : "categories",
    categories: {
        search: "",
        status: "All",
        editingId: null
    },
    tags: {
        search: "",
        status: "All",
        editingId: null
    }
};

function formatDate(value) {
    return new Date(value).toLocaleDateString("en-CA", {
        year: "numeric",
        month: "short",
        day: "numeric"
    });
}

function setFeedback(element, message = "", type = "") {
    element.textContent = message;
    element.className = `feedback-message ${type}`.trim();
}

function setActiveTab(tab) {
    state.activeTab = tab;
    Object.entries(tabButtons).forEach(([key, button]) => {
        const active = key === tab;
        button.classList.toggle("active", active);
        button.setAttribute("aria-selected", String(active));
    });
    Object.entries(panels).forEach(([key, panel]) => {
        panel.hidden = key !== tab;
    });
    const nextUrl = new URL(window.location.href);
    nextUrl.searchParams.set("tab", tab);
    window.history.replaceState({}, "", nextUrl);
}

function resetCategoryForm() {
    state.categories.editingId = null;
    categoryUi.editorTitle.textContent = "Add Category";
    categoryUi.form.reset();
    setFeedback(categoryUi.feedbackMessage);
}

function resetTagForm() {
    state.tags.editingId = null;
    tagUi.editorTitle.textContent = "Add Tag";
    tagUi.form.reset();
    setFeedback(tagUi.feedbackMessage);
}

function renderCategoryTable(items) {
    if (!items.length) {
        categoryUi.tableBody.innerHTML = "";
        categoryUi.emptyState.hidden = false;
        return;
    }

    categoryUi.tableBody.innerHTML = items.map((item) => `
        <tr>
            <td><strong>${item.name}</strong></td>
            <td>${item.description}</td>
            <td><span class="status-badge ${item.status.toLowerCase()}">${item.status}</span></td>
            <td>${formatDate(item.updatedAt)}</td>
            <td>${item.resourceCount}</td>
            <td>
                <div class="table-actions">
                    <button class="secondary-button compact-action" type="button" data-category-edit="${item.id}">Edit</button>
                    <button class="button-link secondary-button compact-action" type="button" data-category-toggle="${item.id}">
                        ${item.status === "ACTIVE" ? "Deactivate" : "Activate"}
                    </button>
                </div>
            </td>
        </tr>
    `).join("");

    categoryUi.emptyState.hidden = true;
}

function renderTagTable(items) {
    if (!items.length) {
        tagUi.tableBody.innerHTML = "";
        tagUi.emptyState.hidden = false;
        return;
    }

    tagUi.tableBody.innerHTML = items.map((item) => `
        <tr>
            <td><strong>${item.name}</strong></td>
            <td>${item.description}</td>
            <td><span class="status-badge ${item.status.toLowerCase()}">${item.status}</span></td>
            <td>${formatDate(item.updatedAt)}</td>
            <td>${item.usageCount}</td>
            <td>
                <div class="table-actions">
                    <button class="secondary-button compact-action" type="button" data-tag-edit="${item.id}">Edit</button>
                    <button class="button-link secondary-button compact-action" type="button" data-tag-toggle="${item.id}">
                        ${item.status === "ACTIVE" ? "Deactivate" : "Activate"}
                    </button>
                </div>
            </td>
        </tr>
    `).join("");

    tagUi.emptyState.hidden = true;
}

async function loadCategories() {
    categoryUi.refreshButton.disabled = true;
    categoryUi.refreshButton.textContent = "Refreshing...";
    try {
        const data = await getCategoryList(state.categories);
        activeCategoryCount.textContent = data.totalActive;
        renderCategoryTable(data.items);
    } finally {
        categoryUi.refreshButton.disabled = false;
        categoryUi.refreshButton.textContent = "Refresh Categories";
    }
}

async function loadTags() {
    tagUi.refreshButton.disabled = true;
    tagUi.refreshButton.textContent = "Refreshing...";
    try {
        const data = await getTagList(state.tags);
        activeTagCount.textContent = data.totalActive;
        renderTagTable(data.items);
    } finally {
        tagUi.refreshButton.disabled = false;
        tagUi.refreshButton.textContent = "Refresh Tags";
    }
}

tabButtons.categories.addEventListener("click", () => setActiveTab("categories"));
tabButtons.tags.addEventListener("click", () => setActiveTab("tags"));

categoryUi.searchInput.addEventListener("input", (event) => {
    state.categories.search = event.target.value.trim();
    loadCategories();
});

categoryUi.statusFilter.addEventListener("change", (event) => {
    state.categories.status = event.target.value;
    loadCategories();
});

categoryUi.clearFiltersButton.addEventListener("click", () => {
    state.categories.search = "";
    state.categories.status = "All";
    categoryUi.searchInput.value = "";
    categoryUi.statusFilter.value = "All";
    loadCategories();
});

categoryUi.refreshButton.addEventListener("click", loadCategories);
categoryUi.resetButton.addEventListener("click", resetCategoryForm);

categoryUi.form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const payload = {
        name: categoryUi.nameInput.value.trim(),
        description: categoryUi.descriptionInput.value.trim()
    };

    if (!payload.name || !payload.description) {
        setFeedback(categoryUi.feedbackMessage, "Category name and description are required.", "error");
        return;
    }

    try {
        if (state.categories.editingId) {
            await updateCategory(state.categories.editingId, payload);
            setFeedback(categoryUi.feedbackMessage, "Category updated.", "success");
        } else {
            await createCategory(payload);
            setFeedback(categoryUi.feedbackMessage, "Category created.", "success");
        }

        resetCategoryForm();
        await loadCategories();
    } catch (error) {
        setFeedback(categoryUi.feedbackMessage, error.message || "Failed to save category.", "error");
    }
});

categoryUi.tableBody.addEventListener("click", async (event) => {
    const editButton = event.target.closest("[data-category-edit]");
    const toggleButton = event.target.closest("[data-category-toggle]");

    if (editButton) {
        const id = Number(editButton.dataset.categoryEdit);
        const data = await getCategoryList({ search: "", status: "All" });
        const category = data.items.find((item) => item.id === id);
        if (!category) {
            return;
        }
        state.categories.editingId = id;
        categoryUi.editorTitle.textContent = "Edit Category";
        categoryUi.nameInput.value = category.name;
        categoryUi.descriptionInput.value = category.description;
        setFeedback(categoryUi.feedbackMessage);
    }

    if (toggleButton) {
        try {
            await toggleCategoryStatus(Number(toggleButton.dataset.categoryToggle));
            await loadCategories();
            setFeedback(categoryUi.feedbackMessage, "Category status updated.", "success");
        } catch (error) {
            setFeedback(categoryUi.feedbackMessage, error.message || "Failed to update category status.", "error");
        }
    }
});

tagUi.searchInput.addEventListener("input", (event) => {
    state.tags.search = event.target.value.trim();
    loadTags();
});

tagUi.statusFilter.addEventListener("change", (event) => {
    state.tags.status = event.target.value;
    loadTags();
});

tagUi.clearFiltersButton.addEventListener("click", () => {
    state.tags.search = "";
    state.tags.status = "All";
    tagUi.searchInput.value = "";
    tagUi.statusFilter.value = "All";
    loadTags();
});

tagUi.refreshButton.addEventListener("click", loadTags);
tagUi.resetButton.addEventListener("click", resetTagForm);

tagUi.form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const payload = {
        name: tagUi.nameInput.value.trim(),
        description: tagUi.descriptionInput.value.trim()
    };

    if (!payload.name || !payload.description) {
        setFeedback(tagUi.feedbackMessage, "Tag name and description are required.", "error");
        return;
    }

    try {
        if (state.tags.editingId) {
            await updateTag(state.tags.editingId, payload);
            setFeedback(tagUi.feedbackMessage, "Tag updated.", "success");
        } else {
            await createTag(payload);
            setFeedback(tagUi.feedbackMessage, "Tag created.", "success");
        }

        resetTagForm();
        await loadTags();
    } catch (error) {
        setFeedback(tagUi.feedbackMessage, error.message || "Failed to save tag.", "error");
    }
});

tagUi.tableBody.addEventListener("click", async (event) => {
    const editButton = event.target.closest("[data-tag-edit]");
    const toggleButton = event.target.closest("[data-tag-toggle]");

    if (editButton) {
        const id = Number(editButton.dataset.tagEdit);
        const data = await getTagList({ search: "", status: "All" });
        const tag = data.items.find((item) => item.id === id);
        if (!tag) {
            return;
        }
        state.tags.editingId = id;
        tagUi.editorTitle.textContent = "Edit Tag";
        tagUi.nameInput.value = tag.name;
        tagUi.descriptionInput.value = tag.description;
        setFeedback(tagUi.feedbackMessage);
    }

    if (toggleButton) {
        try {
            await toggleTagStatus(Number(toggleButton.dataset.tagToggle));
            await loadTags();
            setFeedback(tagUi.feedbackMessage, "Tag status updated.", "success");
        } catch (error) {
            setFeedback(tagUi.feedbackMessage, error.message || "Failed to update tag status.", "error");
        }
    }
});

setActiveTab(state.activeTab);
loadCategories();
loadTags();
