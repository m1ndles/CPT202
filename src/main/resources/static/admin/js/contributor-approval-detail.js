import {
    approveContributorApplication,
    getContributorApprovalDetail,
    rejectContributorApplication,
    sendContributorAppealReply
} from "/admin/js/api.js";

const params = new URLSearchParams(window.location.search);
const applicationId = params.get("id");

const applicantName = document.getElementById("applicantName");
const username = document.getElementById("username");
const submittedAt = document.getElementById("submittedAt");
const expertiseField = document.getElementById("expertiseField");
const statusBadge = document.getElementById("statusBadge");
const motivationStatement = document.getElementById("motivationStatement");
const portfolioLink = document.getElementById("portfolioLink");
const supportingEvidence = document.getElementById("supportingEvidence");
const rejectionComments = document.getElementById("rejectionComments");
const approveButton = document.getElementById("approveButton");
const rejectButton = document.getElementById("rejectButton");
const feedbackMessage = document.getElementById("feedbackMessage");
const existingRejectionBlock = document.getElementById("existingRejectionBlock");
const existingRejectionComments = document.getElementById("existingRejectionComments");
const appealThreadList = document.getElementById("appealThreadList");
const appealReplyCard = document.getElementById("appealReplyCard");
const appealReplyInput = document.getElementById("appealReplyInput");
const sendAppealReplyButton = document.getElementById("sendAppealReplyButton");
const appealReplyStatus = document.getElementById("appealReplyStatus");
let currentApplication = null;

function formatDate(value) {
    if (!value) return "-";
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
    if (!appealReplyStatus) return;
    appealReplyStatus.textContent = message;
    appealReplyStatus.className = `feedback-message ${type}`.trim();
}

function escapeHtml(value) {
    return String(value || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function renderAppealThread(messages) {
    if (!appealThreadList) return;

    if (!Array.isArray(messages) || !messages.length) {
        appealThreadList.innerHTML = `
            <div class="appeal-thread-empty">
                No appeal messages have been sent for this application yet.
            </div>
        `;
        return;
    }

    appealThreadList.innerHTML = messages.map((item) => {
        const role = String(item.senderRole || "").toLowerCase() === "admin" ? "admin" : "applicant";
        return `
            <article class="appeal-thread-item ${role}">
                <div class="appeal-thread-meta">
                    <span>${escapeHtml(item.createdAt || "")}</span>
                    <strong>${escapeHtml(item.senderName || role)}</strong>
                </div>
                <p class="appeal-thread-body">${escapeHtml(item.content || "")}</p>
            </article>
        `;
    }).join("");
}

function renderPortfolio(item) {
    const link = item.portfolioLink || item.attachmentUrl;
    if (link) {
        portfolioLink.href = link;
        portfolioLink.textContent = item.attachmentName ? `Open ${item.attachmentName}` : "Open supporting portfolio";
        portfolioLink.classList.remove("disabled");
        return;
    }

    portfolioLink.href = "#";
    portfolioLink.textContent = "No supporting portfolio provided";
    portfolioLink.classList.add("disabled");
}

function renderEvidence(item) {
    if (item.attachmentName && item.attachmentUrl) {
        supportingEvidence.textContent = `Uploaded file: ${item.attachmentName}`;
        return;
    }
    supportingEvidence.textContent = "No additional supporting evidence was provided.";
}

function renderDetail(item) {
    currentApplication = item;
    applicantName.textContent = item.fullName;
    username.textContent = `@${item.username}`;
    submittedAt.textContent = formatDate(item.submittedAt);
    expertiseField.textContent = item.expertiseField;
    statusBadge.textContent = item.status;
    statusBadge.className = `status-badge ${String(item.status || "").toLowerCase()}`;
    motivationStatement.textContent = item.motivationStatement;
    renderPortfolio(item);
    renderEvidence(item);
    rejectionComments.value = item.rejectionComments || "";

    const reviewable = item.status === "PENDING";
    approveButton.disabled = !reviewable;
    rejectButton.disabled = !reviewable;
    rejectionComments.disabled = !reviewable;

    if (item.rejectionComments) {
        existingRejectionBlock.hidden = false;
        existingRejectionComments.textContent = item.rejectionComments;
    } else {
        existingRejectionBlock.hidden = true;
    }

    renderAppealThread(item.appealMessages || []);
    const canReply = item.status === "REJECTED" || (Array.isArray(item.appealMessages) && item.appealMessages.length > 0);
    appealReplyCard.hidden = !canReply;
    if (!canReply) {
        setAppealReplyStatus("");
        if (appealReplyInput) {
            appealReplyInput.value = "";
        }
    }
}

async function loadDetail() {
    if (!applicationId) {
        setFeedback("Missing application id in the page URL.", "error");
        return;
    }

    try {
        const item = await getContributorApprovalDetail(applicationId);
        renderDetail(item);
        setFeedback("");
    } catch (error) {
        setFeedback(error.message || "Contributor application not found.", "error");
    }
}

approveButton.addEventListener("click", async () => {
    try {
        const response = await approveContributorApplication(applicationId);
        renderDetail(response.application);
        setFeedback(response.message, "success");
    } catch (error) {
        setFeedback(error.message || "Failed to approve contributor application.", "error");
    }
});

rejectButton.addEventListener("click", async () => {
    if (!rejectionComments.value.trim()) {
        setFeedback("Rejection comments are required before rejecting an application.", "error");
        return;
    }

    try {
        const response = await rejectContributorApplication(applicationId, rejectionComments.value.trim());
        renderDetail(response.application);
        setFeedback(response.message, "success");
    } catch (error) {
        setFeedback(error.message || "Failed to reject contributor application.", "error");
    }
});

sendAppealReplyButton?.addEventListener("click", async () => {
    const content = appealReplyInput.value.trim();
    if (!applicationId || !currentApplication) {
        setAppealReplyStatus("Application detail is not ready yet.", "error");
        return;
    }
    if (!content) {
        setAppealReplyStatus("Reply message content is required.", "error");
        return;
    }

    sendAppealReplyButton.disabled = true;
    setAppealReplyStatus("");
    try {
        const response = await sendContributorAppealReply(applicationId, content);
        currentApplication = {
            ...currentApplication,
            appealMessages: response.messages || currentApplication.appealMessages
        };
        renderDetail(currentApplication);
        appealReplyInput.value = "";
        setAppealReplyStatus(response.message || "Reply sent.", "success");
    } catch (error) {
        setAppealReplyStatus(error.message || "Failed to send reply.", "error");
    } finally {
        sendAppealReplyButton.disabled = false;
    }
});

loadDetail();
