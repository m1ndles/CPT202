import { deleteMyResource, getMyResources, getSessionUser, logout } from "./heritage-data.js";

const pageMessage = document.getElementById("pageMessage");
const userSummary = document.getElementById("userSummary");
const resourceList = document.getElementById("resourceList");
const draftLimitNotice = document.getElementById("draftLimitNotice");
const logoutBtn = document.getElementById("logoutBtn");
const tabButtons = Array.from(document.querySelectorAll(".status-tab"));

const countElements = {
  ALL: document.getElementById("allTabCount"),
  DRAFT: document.getElementById("draftTabCount"),
  PENDING: document.getElementById("pendingTabCount"),
  APPROVED: document.getElementById("approvedTabCount"),
  REJECTED: document.getElementById("rejectedTabCount")
};

const statCards = {
  DRAFT: document.getElementById("draftCount"),
  PENDING: document.getElementById("pendingCount"),
  APPROVED: document.getElementById("approvedCount"),
  REJECTED: document.getElementById("rejectedCount")
};

const STATUS_LABELS = {
  DRAFT: "Draft",
  PENDING: "Pending Review",
  APPROVED: "Published",
  REJECTED: "Rejected"
};

const STATUS_BADGES = {
  DRAFT: "border-primary/20 bg-primary/10 text-primary",
  PENDING: "border-amber-200 bg-amber-50 text-amber-700",
  APPROVED: "border-emerald-200 bg-emerald-50 text-emerald-700",
  REJECTED: "border-red-200 bg-red-50 text-red-700"
};

let currentView = "ALL";
let allResources = [];
let deletingResourceId = null;

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

function buildPrimaryAction(resource) {
  switch (resource.status) {
    case "DRAFT":
      return {
        label: "Continue Editing",
        href: `/submit-resource.html?draftId=${encodeURIComponent(resource.id)}`
      };
    case "PENDING":
      return {
        label: "Re-edit",
        href: `/submit-resource.html?draftId=${encodeURIComponent(resource.id)}&edit=1&sourceStatus=${encodeURIComponent(resource.status)}`
      };
    case "APPROVED":
      return {
        label: "Re-edit",
        href: `/submit-resource.html?draftId=${encodeURIComponent(resource.id)}&edit=1&sourceStatus=${encodeURIComponent(resource.status)}`
      };
    case "REJECTED":
      return {
        label: "Re-edit",
        href: `/submit-resource.html?draftId=${encodeURIComponent(resource.id)}&edit=1&sourceStatus=${encodeURIComponent(resource.status)}`
      };
    default:
      return {
        label: "Open",
        href: "#"
      };
  }
}

function buildSecondaryAction(resource) {
  if (resource.status === "DRAFT") {
    return null;
  }
  return {
    label: resource.status === "REJECTED" ? "Details & Messages" : "View Details",
    href: `/my-resource-detail.html?id=${encodeURIComponent(resource.id)}&status=${encodeURIComponent(resource.status)}`
  };
}

function buildMessageAction(resource) {
  if (resource.status !== "REJECTED") {
    return null;
  }
  return {
    label: "Message Admin",
    href: `/my-resource-detail.html?id=${encodeURIComponent(resource.id)}&status=REJECTED#appealSection`
  };
}

function canDelete(resource) {
  return resource.status === "DRAFT" || resource.status === "APPROVED";
}

function deleteLabel(resource) {
  return resource.status === "APPROVED" ? "Delete Published" : "Delete";
}

function deleteConfirmMessage(resource) {
  if (resource.status === "APPROVED") {
    return `Delete "${resource.title}" from your published resources? This will also remove it from the public site.`;
  }
  return `Delete draft "${resource.title}" permanently?`;
}

function renderTabs() {
  tabButtons.forEach((button) => {
    const active = button.dataset.view === currentView;
    button.className = active
      ? "status-tab rounded-full border border-primary/20 bg-primary px-4 py-2 text-sm font-semibold text-white transition-all"
      : "status-tab rounded-full border border-surface-line bg-white px-4 py-2 text-sm font-semibold text-text-soft transition-all hover:border-primary/30 hover:text-primary";
  });

  draftLimitNotice.classList.toggle("hidden", !(currentView === "ALL" || currentView === "DRAFT"));
}

function updateCounts() {
  const counts = {
    ALL: allResources.length,
    DRAFT: allResources.filter((item) => item.status === "DRAFT").length,
    PENDING: allResources.filter((item) => item.status === "PENDING").length,
    APPROVED: allResources.filter((item) => item.status === "APPROVED").length,
    REJECTED: allResources.filter((item) => item.status === "REJECTED").length
  };

  Object.entries(countElements).forEach(([key, element]) => {
    element.textContent = String(counts[key] ?? 0);
  });

  Object.entries(statCards).forEach(([key, element]) => {
    element.textContent = String(counts[key] ?? 0);
  });
}

function renderEmptyState(view) {
  const label = view === "ALL" ? "resources" : STATUS_LABELS[view]?.toLowerCase() || "resources";
  resourceList.innerHTML = `
    <div class="rounded-2xl border border-dashed border-surface-line bg-surface-soft px-6 py-12 text-center">
      <div class="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
        <span class="material-symbols-outlined text-3xl">inventory_2</span>
      </div>
      <h3 class="text-lg font-semibold text-text-main">No ${escapeHtml(label)} yet</h3>
      <p class="mt-2 text-sm text-text-soft">Create a new submission or switch to another status tab.</p>
    </div>
  `;
}

function renderList() {
  const filtered = currentView === "ALL"
    ? allResources
    : allResources.filter((item) => item.status === currentView);

  if (!filtered.length) {
    renderEmptyState(currentView);
    return;
  }

  resourceList.innerHTML = filtered.map((resource) => {
    const action = buildPrimaryAction(resource);
    const secondaryAction = buildSecondaryAction(resource);
    const messageAction = buildMessageAction(resource);
    const tags = (resource.tags || [])
      .map((tag) => `<span class="rounded-full bg-primary/5 px-2.5 py-1 text-xs font-medium text-primary">${escapeHtml(tag)}</span>`)
      .join("");
    const thumbnailBlock = resource.thumbnail
      ? `<img src="${escapeHtml(resource.thumbnail)}" alt="${escapeHtml(resource.title)}" class="h-32 w-full rounded-2xl border border-surface-line object-cover lg:h-full" onerror="this.parentElement.innerHTML='<div class=&quot;flex h-32 w-full items-center justify-center rounded-2xl border border-dashed border-surface-line bg-surface-soft text-outline lg:h-full&quot;><span class=&quot;material-symbols-outlined text-4xl&quot;>photo</span></div>'">`
      : `<div class="flex h-32 w-full items-center justify-center rounded-2xl border border-dashed border-surface-line bg-surface-soft text-outline lg:h-full">
          <span class="material-symbols-outlined text-4xl">photo</span>
        </div>`;
    const trackingLine = resource.trackingId
      ? `<div class="inline-flex items-center gap-2 rounded-full bg-surface-soft px-3 py-1 text-xs font-semibold text-text-soft">
          <span class="material-symbols-outlined text-sm">confirmation_number</span>
          ${escapeHtml(resource.trackingId)}
        </div>`
      : "";
    const rejectionNote = resource.status === "REJECTED"
      ? `<div class="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          This submission was rejected during review. Open it again to revise the content and resubmit.
        </div>`
      : "";

    return `
      <article class="rounded-2xl border border-surface-line bg-white p-5 shadow-card">
        <div class="grid gap-5 lg:grid-cols-[220px_1fr]">
          <div>${thumbnailBlock}</div>
          <div class="min-w-0">
            <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
              <div class="min-w-0">
                <div class="mb-3 flex flex-wrap items-center gap-2">
                  <span class="inline-flex items-center rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-[0.14em] ${STATUS_BADGES[resource.status] || "border-surface-line bg-surface-soft text-text-soft"}">
                    ${escapeHtml(STATUS_LABELS[resource.status] || resource.status)}
                  </span>
                  ${trackingLine}
                </div>
                <h3 class="font-headline text-2xl leading-tight text-primary">${escapeHtml(resource.title || "Untitled Resource")}</h3>
                <p class="mt-3 text-sm leading-7 text-text-soft">${escapeHtml(resource.description || "No description provided yet.")}</p>
              </div>
              <div class="flex shrink-0 flex-wrap items-center gap-2">
                <a href="${action.href}" class="inline-flex items-center justify-center rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-white transition-all hover:bg-[#2d5648]">
                  ${escapeHtml(action.label)}
                </a>
                ${secondaryAction ? `
                  <a href="${secondaryAction.href}" class="inline-flex items-center justify-center rounded-xl border border-surface-line bg-white px-4 py-2.5 text-sm font-semibold text-text-soft transition-all hover:border-primary/20 hover:bg-primary/5 hover:text-primary">
                    ${escapeHtml(secondaryAction.label)}
                  </a>
                ` : ""}
                ${messageAction ? `
                  <a href="${messageAction.href}" class="inline-flex items-center justify-center rounded-xl border border-primary/20 bg-primary/5 px-4 py-2.5 text-sm font-semibold text-primary transition-all hover:bg-primary/10">
                    ${escapeHtml(messageAction.label)}
                  </a>
                ` : ""}
                ${canDelete(resource) ? `
                  <button
                    type="button"
                    class="delete-resource-btn inline-flex items-center justify-center rounded-xl border border-surface-line bg-white px-4 py-2.5 text-sm font-semibold text-text-soft transition-all hover:border-red-200 hover:bg-red-50 hover:text-red-700 disabled:cursor-not-allowed disabled:opacity-60"
                    data-resource-id="${escapeHtml(resource.id)}"
                    data-resource-title="${escapeHtml(resource.title || "Untitled Resource")}"
                    data-resource-status="${escapeHtml(resource.status)}"
                    ${deletingResourceId === resource.id ? "disabled" : ""}
                  >
                    ${deletingResourceId === resource.id ? "Deleting..." : escapeHtml(deleteLabel(resource))}
                  </button>
                ` : ""}
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
                <span class="material-symbols-outlined text-base">schedule</span>
                ${escapeHtml(resource.createdAt || "No recorded date")}
              </span>
            </div>

            <div class="mt-4 flex flex-wrap gap-2">
              ${tags || '<span class="text-xs text-outline">No tags attached.</span>'}
            </div>
            ${rejectionNote}
          </div>
        </div>
      </article>
    `;
  }).join("");
}

async function handleDelete(resourceId) {
  const resource = allResources.find((item) => String(item.id) === String(resourceId));
  if (!resource || !canDelete(resource)) {
    return;
  }

  const confirmed = window.confirm(deleteConfirmMessage(resource));
  if (!confirmed) {
    return;
  }

  deletingResourceId = resource.id;
  renderList();
  showMessage("");

  try {
    const response = await deleteMyResource(resource.id);
    allResources = allResources.filter((item) => item.id !== resource.id);
    deletingResourceId = null;
    updateCounts();
    renderList();
    showMessage(response?.message || "Resource deleted successfully.", "success");
  } catch (error) {
    deletingResourceId = null;
    renderList();
    showMessage(error.message || "Failed to delete resource.", "error");
  }
}

resourceList.addEventListener("click", async (event) => {
  const button = event.target.closest(".delete-resource-btn");
  if (!button) {
    return;
  }
  event.preventDefault();
  if (button.disabled) {
    return;
  }
  await handleDelete(button.dataset.resourceId);
});

function bindTabs() {
  tabButtons.forEach((button) => {
    button.addEventListener("click", () => {
      currentView = button.dataset.view || "ALL";
      renderTabs();
      renderList();
      const url = new URL(window.location.href);
      if (currentView === "ALL") {
        url.searchParams.delete("view");
      } else {
        url.searchParams.set("view", currentView.toLowerCase());
      }
      window.history.replaceState({}, "", url);
    });
  });
}

async function initPage() {
  try {
    const sessionUser = await getSessionUser();
    if (!sessionUser.canUpload) {
      showMessage("Your current account cannot access contributor resource management.", "error");
      userSummary.textContent = `${sessionUser.username} · ${sessionUser.roleLabel}`;
      resourceList.innerHTML = `
        <div class="rounded-2xl border border-dashed border-surface-line bg-surface-soft px-6 py-12 text-center text-sm text-text-soft">
          This page becomes available after contributor access is enabled.
        </div>
      `;
      return;
    }

    userSummary.textContent = `${sessionUser.username} · ${sessionUser.roleLabel}`;
    allResources = await getMyResources();

    const url = new URL(window.location.href);
    const requestedView = (url.searchParams.get("view") || "").toUpperCase();
    if (requestedView && ["DRAFT", "PENDING", "APPROVED", "REJECTED"].includes(requestedView)) {
      currentView = requestedView;
    }

    updateCounts();
    renderTabs();
    renderList();
    showMessage("");
  } catch (error) {
    if (error.status === 401) {
      window.location.href = "/login.html";
      return;
    }
    showMessage(error.message || "Failed to load your resources.", "error");
    resourceList.innerHTML = `
      <div class="rounded-2xl border border-red-200 bg-red-50 px-6 py-12 text-center text-sm text-red-700">
        Failed to load your resources.
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

bindTabs();
initPage();
