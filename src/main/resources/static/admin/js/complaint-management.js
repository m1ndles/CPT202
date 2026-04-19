import {
    deleteReportedCommentComplaint,
    getComplaintDetail,
    getComplaintInbox,
    reopenReportedResourceComplaint,
    replyToComplaint
} from "/admin/js/api.js";

const complaintCount = document.getElementById("complaintCount");
const searchInput = document.getElementById("searchInput");
const typeFilter = document.getElementById("typeFilter");
const statusFilter = document.getElementById("statusFilter");
const refreshButton = document.getElementById("refreshButton");
const complaintList = document.getElementById("complaintList");
const emptyState = document.getElementById("emptyState");
const detailTitle = document.getElementById("detailTitle");
const detailMeta = document.getElementById("detailMeta");
const detailHelper = document.getElementById("detailHelper");
const detailActionLink = document.getElementById("detailActionLink");
const reopenButton = document.getElementById("reopenButton");
const deleteCommentButton = document.getElementById("deleteCommentButton");
const messageThread = document.getElementById("messageThread");
const replyCard = document.getElementById("replyCard");
const replyInput = document.getElementById("replyInput");
const replyButton = document.getElementById("replyButton");
const replyStatus = document.getElementById("replyStatus");

const state = {
    search: "",
    type: "All",
    status: "All",
    selectedType: "",
    selectedId: null,
    items: []
};

function escapeHtml(value) {
    return String(value || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function fillSelect(select, values, activeValue) {
    select.innerHTML = values.map((value) => `
        <option value="${escapeHtml(value)}" ${value === activeValue ? "selected" : ""}>${escapeHtml(formatFilterValue(select.id, value))}</option>
    `).join("");
}

function formatType(type) {
    if (type === "CONTRIBUTOR_APPEAL") return "Contributor Appeal";
    if (type === "COMMENT_REPORT") return "Comment Report";
    return "Resource Report";
}

function formatProgress(status) {
    if (status === "OPEN") return "Needs Review";
    if (status === "REPLIED") return "Replied";
    if (status === "IN_REVIEW") return "Sent Back to Review";
    if (status === "RESOLVED") return "Resolved";
    return status || "Unknown";
}

function getReplyState(item = {}) {
    const messages = Array.isArray(item.messages) ? item.messages : [];
    const latestMessage = messages.length ? messages[messages.length - 1] : null;
    const latestRole = String(latestMessage?.senderRole || "").toUpperCase();
    const status = String(item.status || "").toUpperCase();

    if (latestRole === "ADMIN" || status === "REPLIED" || status === "RESOLVED") {
        return {
            className: "is-replied",
            label: "Admin replied",
            helper: "The latest response has been sent by an administrator."
        };
    }

    if (status === "IN_REVIEW") {
        return {
            className: "is-reviewing",
            label: "In moderation",
            helper: "The reported content has been sent back into moderation."
        };
    }

    return {
        className: "needs-reply",
        label: "Needs admin reply",
        helper: "This thread is waiting for an administrator response."
    };
}

function formatFilterValue(filterId, value) {
    if (value === "All") return "All";
    if (filterId === "typeFilter") return formatType(value);
    if (filterId === "statusFilter") return formatProgress(value);
    return value;
}

function renderList(items) {
    complaintCount.textContent = String(items.length);

    if (!items.length) {
        complaintList.innerHTML = "";
        emptyState.hidden = false;
        resetDetail();
        return;
    }

    emptyState.hidden = true;
    complaintList.innerHTML = items.map((item) => {
        const replyState = getReplyState(item);
        return `
        <article class="complaint-item ${item.complaintType === state.selectedType && item.id === state.selectedId ? "is-active" : ""}" data-type="${item.complaintType}" data-id="${item.id}">
            <div class="complaint-item-head">
                <div class="complaint-type-row">
                    <span class="complaint-pill">${formatType(item.complaintType)}</span>
                    <span class="status-badge ${String(item.status || "").toLowerCase()}">${escapeHtml(formatProgress(item.status || ""))}</span>
                    <span class="reply-state-pill ${replyState.className}">${escapeHtml(replyState.label)}</span>
                </div>
                <span class="text-xs text-outline">${escapeHtml(item.updatedAt || "")}</span>
            </div>
            <div>
                <h3>${escapeHtml(item.title || "")}</h3>
                <p class="complaint-preview">${escapeHtml(item.latestMessagePreview || "")}</p>
            </div>
            <div class="text-xs text-outline">From ${escapeHtml(item.createdBy || "-")} · ${escapeHtml(item.targetName || "-")}</div>
        </article>
    `;
    }).join("");

    complaintList.querySelectorAll(".complaint-item").forEach((element) => {
        element.addEventListener("click", () => {
            state.selectedType = element.dataset.type;
            state.selectedId = Number(element.dataset.id);
            renderList(state.items);
            loadDetail();
        });
    });
}

function resetDetail() {
    detailTitle.textContent = "Select a complaint thread";
    detailMeta.innerHTML = "";
    detailHelper.textContent = "Choose a thread from the list to inspect the conversation.";
    detailActionLink.hidden = true;
    reopenButton.hidden = true;
    deleteCommentButton.hidden = true;
    messageThread.innerHTML = "<div class=\"thread-empty\">No complaint thread selected.</div>";
    replyCard.hidden = true;
    replyInput.value = "";
    setReplyStatus("");
}

function renderMessages(messages) {
    if (!Array.isArray(messages) || !messages.length) {
        messageThread.innerHTML = "<div class=\"thread-empty\">No conversation messages are available for this thread.</div>";
        return;
    }

    messageThread.innerHTML = messages.map((item) => {
        const role = String(item.senderRole || "").toLowerCase();
        const cssRole = role === "admin" ? "admin" : role === "applicant" ? "applicant" : "user";
        return `
            <article class="thread-message ${cssRole}">
                <div class="thread-meta">
                    <span>${escapeHtml(item.createdAt || "")}</span>
                    <strong>${escapeHtml(item.senderName || "")}</strong>
                </div>
                <p class="thread-body">${escapeHtml(item.content || "")}</p>
            </article>
        `;
    }).join("");
}

function renderDetail(detail) {
    const replyState = getReplyState(detail);
    detailTitle.textContent = detail.title || "Complaint detail";
    detailMeta.innerHTML = `
        <div class="complaint-meta-row"><span class="meta-label">Reported Item</span><strong>${escapeHtml(formatType(detail.complaintType))}</strong></div>
        <div class="complaint-meta-row"><span class="meta-label">Related Record</span><strong>${escapeHtml(detail.targetName || "-")}</strong></div>
        <div class="complaint-meta-row"><span class="meta-label">Moderation Stage</span><strong>${escapeHtml(detail.targetStatus || "-")}</strong></div>
        <div class="complaint-meta-row"><span class="meta-label">Progress</span><strong>${escapeHtml(formatProgress(detail.status || "-"))}</strong></div>
        <div class="complaint-meta-row"><span class="meta-label">Reply Status</span><strong class="reply-state-text ${replyState.className}">${escapeHtml(replyState.label)}</strong></div>
        <div class="complaint-meta-row"><span class="meta-label">Created By</span><strong>${escapeHtml(detail.createdBy || "-")}</strong></div>
        <div class="complaint-meta-row"><span class="meta-label">Updated At</span><strong>${escapeHtml(detail.updatedAt || "-")}</strong></div>
    `;
    detailHelper.textContent = `${replyState.helper} ${detail.helperText || ""}`.trim();
    detailActionLink.hidden = !detail.actionUrl;
    detailActionLink.href = detail.actionUrl || "#";
    detailActionLink.textContent = detail.actionLabel || "Open";
    reopenButton.hidden = !detail.canReopenForReview;
    deleteCommentButton.hidden = !detail.canDeleteComment;
    replyCard.hidden = !detail.canReply;
    replyInput.value = "";
    setReplyStatus("");
    renderMessages(detail.messages || []);
}

function setReplyStatus(message = "", type = "") {
    replyStatus.textContent = message;
    replyStatus.className = `feedback-message ${type}`.trim();
}

async function loadList() {
    refreshButton.disabled = true;
    refreshButton.textContent = "Refreshing...";
    try {
        const data = await getComplaintInbox(state);
        state.items = data.items;
        fillSelect(typeFilter, data.types, state.type);
        fillSelect(statusFilter, data.statuses, state.status);
        if (!state.selectedId && data.items.length) {
            state.selectedType = data.items[0].complaintType;
            state.selectedId = data.items[0].id;
        } else if (!data.items.some((item) => item.complaintType === state.selectedType && item.id === state.selectedId)) {
            state.selectedType = data.items[0]?.complaintType || "";
            state.selectedId = data.items[0]?.id || null;
        }
        renderList(data.items);
        if (state.selectedId) {
            await loadDetail();
        } else {
            resetDetail();
        }
    } finally {
        refreshButton.disabled = false;
        refreshButton.textContent = "Refresh Inbox";
    }
}

async function loadDetail() {
    if (!state.selectedType || !state.selectedId) {
        resetDetail();
        return;
    }

    const detail = await getComplaintDetail(state.selectedType, state.selectedId);
    renderDetail(detail);
}

let searchTimer;
searchInput.addEventListener("input", (event) => {
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(() => {
        state.search = event.target.value.trim();
        loadList();
    }, 220);
});

typeFilter.addEventListener("change", (event) => {
    state.type = event.target.value;
    loadList();
});

statusFilter.addEventListener("change", (event) => {
    state.status = event.target.value;
    loadList();
});

refreshButton.addEventListener("click", loadList);

replyButton?.addEventListener("click", async () => {
    if (!state.selectedType || !state.selectedId) {
        setReplyStatus("Please select a complaint thread first.", "error");
        return;
    }
    const content = replyInput.value.trim();
    if (!content) {
        setReplyStatus("Reply content is required.", "error");
        return;
    }

    replyButton.disabled = true;
    setReplyStatus("");
    try {
        const response = await replyToComplaint(state.selectedType, state.selectedId, content);
        replyInput.value = "";
        setReplyStatus(response.message || "Reply sent.", "success");
        await loadList();
    } catch (error) {
        setReplyStatus(error.message || "Failed to send reply.", "error");
    } finally {
        replyButton.disabled = false;
    }
});

reopenButton?.addEventListener("click", async () => {
    if (!state.selectedId) {
        return;
    }
    reopenButton.disabled = true;
    try {
        await reopenReportedResourceComplaint(state.selectedId);
        setReplyStatus("Resource moved back into the review queue.", "success");
        await loadList();
    } catch (error) {
        setReplyStatus(error.message || "Failed to reopen resource for review.", "error");
    } finally {
        reopenButton.disabled = false;
    }
});

deleteCommentButton?.addEventListener("click", async () => {
    if (!state.selectedId) {
        return;
    }
    if (!window.confirm("Delete the reported comment? This cannot be undone.")) {
        return;
    }
    deleteCommentButton.disabled = true;
    try {
        await deleteReportedCommentComplaint(state.selectedId);
        setReplyStatus("Reported comment deleted.", "success");
        await loadList();
    } catch (error) {
        setReplyStatus(error.message || "Failed to delete reported comment.", "error");
    } finally {
        deleteCommentButton.disabled = false;
    }
});

resetDetail();
loadList();
