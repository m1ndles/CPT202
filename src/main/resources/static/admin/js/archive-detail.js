import {
    getArchiveDetail,
    restoreArchivedResource
} from "/admin/js/api.js";

const params = new URLSearchParams(window.location.search);
const archiveId = params.get("id");

const archiveTitle = document.getElementById("archiveTitle");
const archiveCategory = document.getElementById("archiveCategory");
const archiveContributor = document.getElementById("archiveContributor");
const archivePlace = document.getElementById("archivePlace");
const archiveTrackingId = document.getElementById("archiveTrackingId");
const archiveStatus = document.getElementById("archiveStatus");
const archiveThumbnail = document.getElementById("archiveThumbnail");
const archiveDescription = document.getElementById("archiveDescription");
const archiveTags = document.getElementById("archiveTags");
const archiveFileMeta = document.getElementById("archiveFileMeta");
const archiveLinkMeta = document.getElementById("archiveLinkMeta");
const originalMetadata = document.getElementById("originalMetadata");
const publicationHistory = document.getElementById("publicationHistory");
const archivedBy = document.getElementById("archivedBy");
const archivedAt = document.getElementById("archivedAt");
const archiveReason = document.getElementById("archiveReason");
const restoreButton = document.getElementById("restoreButton");
const feedbackMessage = document.getElementById("feedbackMessage");

function formatDate(value) {
    return new Date(value).toLocaleDateString("en-CA", {
        year: "numeric",
        month: "short",
        day: "numeric"
    });
}

function setFeedback(message = "", type = "") {
    feedbackMessage.textContent = message;
    feedbackMessage.className = `feedback-message ${type}`.trim();
}

function escapeHtml(value) {
    return String(value || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

function formatContributorName(value) {
    const raw = String(value || "").trim();
    if (!raw) return "Contributor";
    return raw
        .replace(/[_-]+/g, " ")
        .replace(/\s+/g, " ")
        .split(" ")
        .map((part) => part ? part.charAt(0).toUpperCase() + part.slice(1) : "")
        .join(" ");
}

function renderThumbnail(url, title) {
    if (!url) {
        archiveThumbnail.innerHTML = "<div class=\"archive-thumbnail-placeholder\">No archived thumbnail available.</div>";
        return;
    }

    archiveThumbnail.innerHTML = `<img src="${escapeHtml(url)}" alt="${escapeHtml(title || "Archived resource thumbnail")}" onerror="this.parentElement.innerHTML='<div class=&quot;archive-thumbnail-placeholder&quot;>No archived thumbnail available.</div>'">`;
}

function renderTags(tags) {
    const safeTags = Array.isArray(tags) ? tags.filter(Boolean) : [];
    if (!safeTags.length) {
        archiveTags.innerHTML = "<span class=\"archive-tag muted\">No tags recorded</span>";
        return;
    }

    archiveTags.innerHTML = safeTags
        .map((tag) => `<span class="archive-tag">${escapeHtml(tag)}</span>`)
        .join("");
}

function renderLinkMeta(element, label, url, emptyText) {
    if (!url) {
        element.textContent = emptyText;
        return;
    }

    element.innerHTML = `<a class="archive-inline-link" href="${escapeHtml(url)}" target="_blank" rel="noreferrer">${escapeHtml(label || url)}</a>`;
}

function renderDetail(item) {
    archiveTitle.textContent = item.title;
    archiveCategory.textContent = item.category;
    archiveContributor.textContent = formatContributorName(item.contributor);
    archivePlace.textContent = item.place || "Place not recorded";
    archiveTrackingId.textContent = item.trackingId || "No tracking ID";
    archiveStatus.textContent = item.status;
    archiveStatus.className = `status-badge ${item.status.toLowerCase()}`;
    archiveDescription.textContent = item.description || "No archived description available.";
    renderThumbnail(item.thumbnailUrl, item.title);
    renderTags(item.tags || []);
    renderLinkMeta(archiveFileMeta, item.fileName || "Open attached file", item.fileLink, "No file attachment recorded.");
    renderLinkMeta(archiveLinkMeta, item.externalLabel || "Open external reference", item.externalLink, "No external reference recorded.");
    originalMetadata.textContent = item.originalMetadata;
    publicationHistory.textContent = item.publicationHistory;
    archivedBy.textContent = item.archivedBy;
    archivedAt.textContent = formatDate(item.archivedAt);
    archiveReason.textContent = item.archiveReason;
}

async function loadDetail() {
    try {
        const item = await getArchiveDetail(archiveId);
        renderDetail(item);
    } catch (error) {
        setFeedback(error.message || "Archive record not found.", "error");
        restoreButton.disabled = true;
    }
}

restoreButton.addEventListener("click", async () => {
    const confirmed = window.confirm(
        "Restore this archived resource?\n\nIt will return to the active published lifecycle and may become visible again in the public resource collection."
    );
    if (!confirmed) {
        return;
    }

    try {
        const response = await restoreArchivedResource(archiveId);
        restoreButton.disabled = true;
        setFeedback(response.message, "success");
    } catch (error) {
        setFeedback(error.message || "Failed to restore resource.", "error");
    }
});

loadDetail();
