import {
    getArchiveDetail,
    restoreArchivedResource
} from "/admin/js/api.js";

const params = new URLSearchParams(window.location.search);
const archiveId = params.get("id");

const archiveTitle = document.getElementById("archiveTitle");
const archiveCategory = document.getElementById("archiveCategory");
const archiveContributor = document.getElementById("archiveContributor");
const archiveStatus = document.getElementById("archiveStatus");
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

function renderDetail(item) {
    archiveTitle.textContent = item.title;
    archiveCategory.textContent = item.category;
    archiveContributor.textContent = item.contributor;
    archiveStatus.textContent = item.status;
    archiveStatus.className = `status-badge ${item.status.toLowerCase()}`;
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
    try {
        const response = await restoreArchivedResource(archiveId);
        restoreButton.disabled = true;
        setFeedback(response.message, "success");
    } catch (error) {
        setFeedback(error.message || "Failed to restore resource.", "error");
    }
});

loadDetail();
