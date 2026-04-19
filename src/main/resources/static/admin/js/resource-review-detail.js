import {
    approveResourceReview,
    archiveResourceReview,
    getResourceReviewDetail,
    rejectResourceReview,
    sendResourceReviewReply
} from "/admin/js/api.js";

const params = new URLSearchParams(window.location.search);
const resourceId = params.get("id");

const titleElement = document.getElementById("title");
const subtitleElement = document.getElementById("subtitle");
const placeElement = document.getElementById("place");
const submissionDateElement = document.getElementById("submissionDate");
const contributorElement = document.getElementById("contributor");
const statusBadgeElement = document.getElementById("statusBadge");
const descriptionElement = document.getElementById("description");
const descriptionToggle = document.getElementById("descriptionToggle");
const categoryElement = document.getElementById("category");
const tagsElement = document.getElementById("tags");
const resourceImage = document.getElementById("resourceImage");
const fileLink = document.getElementById("fileLink");
const fileDetails = document.getElementById("fileDetails");
const externalLink = document.getElementById("externalLink");
const externalDetails = document.getElementById("externalDetails");
const copyrightDeclaration = document.getElementById("copyrightDeclaration");
const submissionMetadata = document.getElementById("submissionMetadata");
const visibilityPill = document.getElementById("visibilityPill");
const rejectionCommentsInput = document.getElementById("rejectionComments");
const approveButton = document.getElementById("approveButton");
const rejectButton = document.getElementById("rejectButton");
const archiveButton = document.getElementById("archiveButton");
const feedbackMessage = document.getElementById("feedbackMessage");
const existingRejectionBlock = document.getElementById("existingRejectionBlock");
const existingRejectionComments = document.getElementById("existingRejectionComments");
const appealThreadList = document.getElementById("appealThreadList");
const appealReplyCard = document.getElementById("appealReplyCard");
const appealReplyInput = document.getElementById("appealReplyInput");
const sendAppealReplyButton = document.getElementById("sendAppealReplyButton");
const appealReplyStatus = document.getElementById("appealReplyStatus");
const workflowNotice = document.getElementById("workflowNotice");
const archiveModal = document.getElementById("archiveModal");
const archiveReasonInput = document.getElementById("archiveReasonInput");
const archiveModalFeedback = document.getElementById("archiveModalFeedback");
const confirmArchiveButton = document.getElementById("confirmArchiveButton");
let currentDetail = null;

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

function setAppealReplyStatus(message = "", type = "") {
    appealReplyStatus.textContent = message;
    appealReplyStatus.className = `feedback-message ${type}`.trim();
}

function setArchiveModalFeedback(message = "", type = "") {
    archiveModalFeedback.textContent = message;
    archiveModalFeedback.className = `feedback-message ${type}`.trim();
}

function escapeHtml(value) {
    return String(value || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function labelForRole(role) {
    switch (String(role || "").toUpperCase()) {
        case "ADMIN":
            return "Admin";
        case "SYSTEM":
            return "System";
        default:
            return "Contributor";
    }
}

function renderTags(tags) {
    if (!tags.length) {
        tagsElement.innerHTML = `<span class="tag muted">No tags supplied</span>`;
        return;
    }

    tagsElement.innerHTML = tags.map((tag) => `<span class="tag">${tag}</span>`).join("");
}

function renderLink(element, href, activeText, inactiveText) {
    if (href) {
        element.href = href;
        element.textContent = activeText;
        element.classList.remove("disabled");
        return;
    }

    element.href = "#";
    element.textContent = inactiveText;
    element.classList.add("disabled");
}

function renderAppealThread(messages) {
    if (!Array.isArray(messages) || !messages.length) {
        appealThreadList.innerHTML = `
            <div class="appeal-thread-empty">
                No contributor appeal messages have been recorded for this resource yet.
            </div>
        `;
        return;
    }

    appealThreadList.innerHTML = messages.map((item) => {
        const role = String(item.senderRole || "").toLowerCase() || "contributor";
        return `
            <article class="appeal-thread-item ${escapeHtml(role)}">
                <div class="appeal-thread-meta">
                    <strong>${escapeHtml(item.senderName || labelForRole(item.senderRole))}</strong>
                    <span>${escapeHtml(labelForRole(item.senderRole))}</span>
                    <span>${escapeHtml(item.createdAt || "")}</span>
                </div>
                <p class="appeal-thread-body">${escapeHtml(item.content || "")}</p>
            </article>
        `;
    }).join("");
}

function renderWorkflowNotice(status) {
    if (status === "REJECTED") {
        workflowNotice.hidden = false;
        workflowNotice.className = "workflow-notice waiting";
        workflowNotice.textContent = "This record is currently read-only because it has already been rejected. You can continue the clarification thread below, but the approve and reject controls will unlock only after the contributor resubmits it and the status returns to Pending Review.";
        return;
    }

    if (status === "APPROVED") {
        workflowNotice.hidden = false;
        workflowNotice.className = "workflow-notice locked";
        workflowNotice.textContent = "This resource has already been approved. You can archive it from the controls below if it should be removed from the public lifecycle.";
        return;
    }

    if (status === "ARCHIVED") {
        workflowNotice.hidden = false;
        workflowNotice.className = "workflow-notice locked";
        workflowNotice.textContent = "This resource has been archived and is hidden from the public lifecycle. Restore it from Archive Management if it should become active again.";
        return;
    }

    workflowNotice.hidden = true;
    workflowNotice.className = "workflow-notice";
    workflowNotice.textContent = "";
}

function renderReplyComposer(detail) {
    const canReply = Boolean(detail.canReplyInAppealThread);
    appealReplyCard.hidden = !canReply;
    if (!canReply) {
        appealReplyInput.value = "";
        setAppealReplyStatus("");
        return;
    }
    sendAppealReplyButton.disabled = false;
}

function updateDescriptionClamp() {
    descriptionElement.classList.add("collapsed");
    descriptionToggle.hidden = true;

    window.requestAnimationFrame(() => {
        const hasOverflow = descriptionElement.scrollHeight - descriptionElement.clientHeight > 6;
        descriptionToggle.hidden = !hasOverflow;
        descriptionToggle.textContent = "Read more";
    });
}

function renderDetail(detail) {
    currentDetail = detail;
    titleElement.textContent = detail.title;
    subtitleElement.textContent = detail.subtitle || "Heritage Resource Overview";
    placeElement.textContent = detail.place || "Place not provided";
    submissionDateElement.textContent = detail.submissionDate ? formatDate(detail.submissionDate) : "Date not provided";
    contributorElement.textContent = detail.contributor || "Unknown contributor";
    statusBadgeElement.textContent = detail.status.replaceAll("_", " ");
    statusBadgeElement.className = `status-badge ${detail.status.toLowerCase()}`;
    descriptionElement.textContent = detail.description || "No introduction was provided with this submission.";
    categoryElement.textContent = detail.category || "Category not provided";
    renderTags(detail.tags || []);
    updateDescriptionClamp();
    resourceImage.src = detail.imageUrl || "/review/images/resource-placeholder.svg";
    resourceImage.alt = detail.title;
    renderLink(fileLink, detail.fileLink, "Open attached file", "No attached file provided");
    renderLink(externalLink, detail.externalLink, "Visit external reference", "No external reference provided");
    fileDetails.textContent = detail.fileLink || "No file details are available for this submission.";
    externalDetails.textContent = detail.externalLink || "No external link details are available for this submission.";
    copyrightDeclaration.textContent = detail.copyrightDeclaration || "No rights declaration was supplied with this submission.";
    submissionMetadata.textContent = detail.submissionMetadata || "No submission metadata available.";
    rejectionCommentsInput.value = detail.rejectionComments || "";
    visibilityPill.textContent = detail.visible ? "Visibility: public" : "Visibility: hidden";
    renderAppealThread(detail.appealMessages || []);
    renderReplyComposer(detail);
    renderWorkflowNotice(detail.status);

    const reviewable = detail.status === "PENDING_REVIEW";
    const archivable = detail.status === "APPROVED";
    approveButton.disabled = !reviewable;
    rejectButton.disabled = !reviewable;
    rejectionCommentsInput.disabled = !reviewable;
    archiveButton.hidden = !archivable;
    archiveButton.disabled = !archivable;

    if (detail.rejectionComments) {
        existingRejectionBlock.hidden = false;
        existingRejectionComments.textContent = detail.rejectionComments;
    } else {
        existingRejectionBlock.hidden = true;
    }
}

function openArchiveModal() {
    archiveReasonInput.value = "";
    setArchiveModalFeedback("");
    archiveModal.hidden = false;
    archiveReasonInput.focus();
}

function closeArchiveModal() {
    archiveModal.hidden = true;
    archiveReasonInput.value = "";
    setArchiveModalFeedback("");
}

async function loadDetail() {
    try {
        const detail = await getResourceReviewDetail(resourceId);
        renderDetail(detail);
        setFeedback("");
        setAppealReplyStatus("");
    } catch (error) {
        setFeedback(error.message || "Resource not found.", "error");
        approveButton.disabled = true;
        rejectButton.disabled = true;
    }
}

approveButton.addEventListener("click", async () => {
    try {
        const response = await approveResourceReview(resourceId);
        renderDetail(response.resource);
        setFeedback(response.message, "success");
    } catch (error) {
        setFeedback(error.message || "Failed to approve resource.", "error");
    }
});

rejectButton.addEventListener("click", async () => {
    if (!rejectionCommentsInput.value.trim()) {
        setFeedback("Rejection comments are required before rejecting a resource.", "error");
        return;
    }

    try {
        const response = await rejectResourceReview(resourceId, rejectionCommentsInput.value);
        renderDetail(response.resource);
        setFeedback(response.message, "success");
    } catch (error) {
        setFeedback(error.message || "Failed to reject resource.", "error");
    }
});

archiveButton?.addEventListener("click", () => {
    if (currentDetail?.status !== "APPROVED") {
        return;
    }
    openArchiveModal();
});

confirmArchiveButton?.addEventListener("click", async () => {
    const archiveReason = archiveReasonInput.value.trim();
    if (!archiveReason) {
        setArchiveModalFeedback("Archive reason is required.", "error");
        return;
    }

    confirmArchiveButton.disabled = true;
    archiveButton.disabled = true;
    setArchiveModalFeedback("");

    try {
        const response = await archiveResourceReview(resourceId, archiveReason);
        closeArchiveModal();
        renderDetail(response.resource);
        setFeedback(response.message || "Resource archived successfully.", "success");
    } catch (error) {
        setArchiveModalFeedback(error.message || "Failed to archive resource.", "error");
        archiveButton.disabled = false;
    } finally {
        confirmArchiveButton.disabled = false;
    }
});

archiveModal?.addEventListener("click", (event) => {
    if (event.target.closest("[data-archive-cancel]")) {
        closeArchiveModal();
    }
});

sendAppealReplyButton?.addEventListener("click", async () => {
    const content = appealReplyInput.value.trim();
    if (!resourceId || !currentDetail?.canReplyInAppealThread) {
        return;
    }
    if (!content) {
        setAppealReplyStatus("Reply message content is required.", "error");
        return;
    }

    sendAppealReplyButton.disabled = true;
    setAppealReplyStatus("");

    try {
        const response = await sendResourceReviewReply(resourceId, content);
        currentDetail = {
            ...currentDetail,
            appealMessages: response.appealMessages || currentDetail.appealMessages
        };
        renderAppealThread(currentDetail.appealMessages);
        appealReplyInput.value = "";
        setAppealReplyStatus(response.message || "Reply sent.", "success");
    } catch (error) {
        setAppealReplyStatus(error.message || "Failed to send reply.", "error");
    } finally {
        sendAppealReplyButton.disabled = false;
    }
});

descriptionToggle.addEventListener("click", () => {
    const collapsed = descriptionElement.classList.contains("collapsed");
    descriptionElement.classList.toggle("collapsed", !collapsed);
    descriptionToggle.textContent = collapsed ? "Show less" : "Read more";
});

window.addEventListener("resize", updateDescriptionClamp);

loadDetail();
