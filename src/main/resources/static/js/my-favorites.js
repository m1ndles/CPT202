import {
  getMyFavoriteResources,
  getSessionUser,
  logout,
  toggleResourceFavorite
} from "./heritage-data.js";

const pageMessage = document.getElementById("pageMessage");
const userSummary = document.getElementById("userSummary");
const favoriteCount = document.getElementById("favoriteCount");
const favoriteList = document.getElementById("favoriteList");
const logoutBtn = document.getElementById("logoutBtn");

let favoriteResources = [];
let removingResourceId = null;

function escapeHtml(text) {
  if (!text) return "";
  return String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function showMessage(message, type = "info") {
  if (!message) {
    pageMessage.classList.add("hidden");
    pageMessage.textContent = "";
    return;
  }

  pageMessage.classList.remove("hidden");
  pageMessage.textContent = message;

  if (type === "error") {
    pageMessage.className = "mb-6 rounded-xl border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-700";
    return;
  }

  if (type === "success") {
    pageMessage.className = "mb-6 rounded-xl border border-primary/20 bg-primary/5 px-5 py-4 text-sm text-primary";
    return;
  }

  pageMessage.className = "mb-6 rounded-xl border border-surface-line bg-surface-soft px-5 py-4 text-sm text-text-soft";
}

function renderStats() {
  favoriteCount.textContent = String(favoriteResources.length);
}

function renderEmptyState() {
  favoriteList.innerHTML = `
    <div class="rounded-2xl border border-dashed border-surface-line bg-surface-soft px-6 py-12 text-center">
      <div class="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
        <span class="material-symbols-outlined text-3xl">favorite</span>
      </div>
      <h3 class="text-lg font-semibold text-text-main">No favorites yet</h3>
      <p class="mt-2 text-sm text-text-soft">Open any resource detail page and tap Save to collect it here.</p>
      <a href="/index.html" class="mt-5 inline-flex items-center justify-center rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-white transition-all hover:bg-[#2d5648]">
        Discover Resources
      </a>
    </div>
  `;
}

function renderList() {
  if (!favoriteResources.length) {
    renderEmptyState();
    return;
  }

  favoriteList.innerHTML = favoriteResources.map((resource) => {
    const tags = (resource.tags || [])
      .map((tag) => `<span class="rounded-full bg-primary/5 px-2.5 py-1 text-xs font-medium text-primary">${escapeHtml(tag)}</span>`)
      .join("");
    const thumbnailBlock = resource.thumbnail
      ? `<img src="${escapeHtml(resource.thumbnail)}" alt="${escapeHtml(resource.title)}" class="h-32 w-full rounded-2xl border border-surface-line object-cover lg:h-full" onerror="this.parentElement.innerHTML='<div class=&quot;flex h-32 w-full items-center justify-center rounded-2xl border border-dashed border-surface-line bg-surface-soft text-outline lg:h-full&quot;><span class=&quot;material-symbols-outlined text-4xl&quot;>photo</span></div>'">`
      : `<div class="flex h-32 w-full items-center justify-center rounded-2xl border border-dashed border-surface-line bg-surface-soft text-outline lg:h-full">
          <span class="material-symbols-outlined text-4xl">photo</span>
        </div>`;

    return `
      <article class="rounded-2xl border border-surface-line bg-white p-5 shadow-card">
        <div class="grid gap-5 lg:grid-cols-[220px_1fr]">
          <div>${thumbnailBlock}</div>
          <div class="min-w-0">
            <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
              <div class="min-w-0">
                <div class="mb-3 flex flex-wrap items-center gap-2">
                  <span class="inline-flex items-center gap-1 rounded-full border border-red-200 bg-red-50 px-3 py-1 text-xs font-bold uppercase tracking-[0.14em] text-red-600">
                    <span class="material-symbols-outlined text-sm" style="font-variation-settings: 'FILL' 1;">favorite</span>
                    Saved
                  </span>
                </div>
                <h3 class="font-headline text-2xl leading-tight text-primary">${escapeHtml(resource.title || "Untitled Resource")}</h3>
              </div>
              <div class="flex shrink-0 flex-wrap items-center gap-2">
                <a href="/detail.html?id=${encodeURIComponent(resource.id)}" class="inline-flex items-center justify-center rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-white transition-all hover:bg-[#2d5648]">
                  Details
                </a>
                <button
                  type="button"
                  class="remove-favorite-btn inline-flex items-center justify-center rounded-xl border border-surface-line bg-white px-4 py-2.5 text-sm font-semibold text-text-soft transition-all hover:border-red-200 hover:bg-red-50 hover:text-red-700 disabled:cursor-not-allowed disabled:opacity-60"
                  data-resource-id="${escapeHtml(resource.id)}"
                  ${removingResourceId === resource.id ? "disabled" : ""}
                >
                  ${removingResourceId === resource.id ? "Removing..." : "Remove"}
                </button>
              </div>
            </div>

            <div class="mt-4 flex flex-wrap gap-3 text-sm text-text-soft">
              <span class="inline-flex items-center gap-1 rounded-full bg-surface-soft px-3 py-1">
                <span class="material-symbols-outlined text-base">category</span>
                ${escapeHtml(resource.category || "Uncategorised")}
              </span>
              <span class="inline-flex items-center gap-1 rounded-full bg-surface-soft px-3 py-1">
                <span class="material-symbols-outlined text-base">location_on</span>
                ${escapeHtml(resource.place || "Unknown place")}
              </span>
              <span class="inline-flex items-center gap-1 rounded-full bg-surface-soft px-3 py-1">
                <span class="material-symbols-outlined text-base">visibility</span>
                ${escapeHtml(resource.viewCount || 0)} views
              </span>
              <span class="inline-flex items-center gap-1 rounded-full bg-surface-soft px-3 py-1">
                <span class="material-symbols-outlined text-base">schedule</span>
                ${escapeHtml(resource.createdAt || "No recorded date")}
              </span>
            </div>

            <div class="mt-4 flex flex-wrap gap-2">
              ${tags || '<span class="text-xs text-outline">No tags attached.</span>'}
            </div>
          </div>
        </div>
      </article>
    `;
  }).join("");
}

async function handleRemove(resourceId) {
  const resource = favoriteResources.find((item) => String(item.id) === String(resourceId));
  if (!resource) {
    return;
  }

  removingResourceId = resource.id;
  renderList();
  showMessage("");

  try {
    const response = await toggleResourceFavorite(resource.id);
    favoriteResources = favoriteResources.filter((item) => item.id !== resource.id);
    removingResourceId = null;
    renderStats();
    renderList();
    showMessage(response?.message || "Resource removed from favorites.", "success");
  } catch (error) {
    removingResourceId = null;
    renderList();
    showMessage(error.message || "Failed to remove favorite.", "error");
  }
}

favoriteList?.addEventListener("click", async (event) => {
  const button = event.target.closest(".remove-favorite-btn");
  if (!button || button.disabled) {
    return;
  }
  event.preventDefault();
  await handleRemove(button.dataset.resourceId);
});

async function initPage() {
  try {
    const sessionUser = await getSessionUser();
    if (sessionUser.guest) {
      showMessage("Guest mode does not support personal favorites. Please sign in with a registered account.", "error");
      userSummary.textContent = "Guest mode";
      favoriteResources = [];
      renderStats();
      renderList();
      return;
    }

    userSummary.textContent = `${sessionUser.username} · ${sessionUser.roleLabel}`;
    favoriteResources = await getMyFavoriteResources();
    renderStats();
    renderList();
    showMessage("");
  } catch (error) {
    if (error.status === 401) {
      window.location.href = "/login.html";
      return;
    }
    showMessage(error.message || "Failed to load your favorites.", "error");
    favoriteList.innerHTML = `
      <div class="rounded-2xl border border-red-200 bg-red-50 px-6 py-12 text-center text-sm text-red-700">
        Failed to load your favorites.
      </div>
    `;
  }
}

logoutBtn?.addEventListener("click", async () => {
  try {
    await logout();
  } finally {
    window.location.href = "/login.html";
  }
});

initPage();
