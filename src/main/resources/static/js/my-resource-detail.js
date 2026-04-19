import { getOwnedDraft, getResourceById, getSessionUser, logout, submitResourceAppeal } from "./heritage-data.js";

const pageNotice = document.getElementById("pageNotice");
const sessionPill = document.getElementById("sessionPill");
const backLink = document.getElementById("backLink");
const logoutBtn = document.getElementById("logoutBtn");
const statusBadge = document.getElementById("statusBadge");
const resourceTitle = document.getElementById("resourceTitle");
const resourceSubtitle = document.getElementById("resourceSubtitle");
const metaCategory = document.getElementById("metaCategory");
const metaPlace = document.getElementById("metaPlace");
const metaPeriod = document.getElementById("metaPeriod");
const metaTracking = document.getElementById("metaTracking");
const metaVisibility = document.getElementById("metaVisibility");
const metaRecordTime = document.getElementById("metaRecordTime");
const thumbnailPanel = document.getElementById("thumbnailPanel");
const resourceDescription = document.getElementById("resourceDescription");
const tagList = document.getElementById("tagList");
const attachmentList = document.getElementById("attachmentList");
const linkList = document.getElementById("linkList");
const reviewFeedbackSection = document.getElementById("reviewFeedbackSection");
const reviewFeedbackBox = document.getElementById("reviewFeedbackBox");
const appealSection = document.getElementById("appealSection");
const appealList = document.getElementById("appealList");
const appealReadonlyNote = document.getElementById("appealReadonlyNote");
const appealForm = document.getElementById("appealForm");
const appealInput = document.getElementById("appealInput");
const sendAppealBtn = document.getElementById("sendAppealBtn");
const appealMessage = document.getElementById("appealMessage");
const statusNote = document.getElementById("statusNote");
const actionSummary = document.getElementById("actionSummary");
const actionList = document.getElementById("actionList");

const params = new URL(window.location.href).searchParams;
const resourceId = params.get("id");
const requestedStatus = normalizeStatus(params.get("status"));
let currentResource = null;

const STATUS_CONFIG = {
  DRAFT: {
    label: "Draft",
    badgeClass: "status-draft",
    view: "draft",
    note: "This is still your editable working draft. You can continue refining the content before sending it for review.",
    summary: "Continue editing when you are ready, or return to the resource list.",
    primaryAction(id) {
      return {
        label: "Continue Editing",
        href: `/submit-resource.html?draftId=${encodeURIComponent(id)}`
      };
    }
  },
  PENDING: {
    label: "Pending Review",
    badgeClass: "status-pending",
    view: "pending",
    note: "This submission is already in the moderation queue. You can reopen it, revise the content, and send the updated version back into review.",
    summary: "Re-edit the submission if you need to adjust the content before the admin team finishes review.",
    primaryAction(id) {
      return {
        label: "Re-edit Submission",
        href: `/submit-resource.html?draftId=${encodeURIComponent(id)}&edit=1&sourceStatus=PENDING`
      };
    }
  },
  APPROVED: {
    label: "Published",
    badgeClass: "status-published",
    view: "approved",
    note: "This resource is currently public. If you reopen it for revision, the updated version will go back through review before it is published again.",
    summary: "You can inspect the public page, or reopen the editor to prepare an updated version for review.",
    primaryAction(id) {
      return {
        label: "Re-edit Resource",
        href: `/submit-resource.html?draftId=${encodeURIComponent(id)}&edit=1&sourceStatus=APPROVED`
      };
    }
  },
  REJECTED: {
    label: "Rejected",
    badgeClass: "status-rejected",
    view: "rejected",
    note: "This submission needs revision before it can return to review. Reopen the editor to adjust the content and resubmit.",
    summary: "Check the current content, then reopen the editor for your next revision.",
    primaryAction(id) {
      return {
        label: "Revise and Resubmit",
        href: `/submit-resource.html?draftId=${encodeURIComponent(id)}&edit=1&sourceStatus=REJECTED`
      };
    }
  }
};

function normalizeStatus(value) {
  return String(value || "").trim().toUpperCase();
}

function escapeHtml(text) {
  if (!text) return "";
  return String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function showNotice(message, isError = false) {
  if (!message) {
    pageNotice.className = "notice";
    pageNotice.textContent = "";
    return;
  }
  pageNotice.textContent = message;
  pageNotice.className = isError ? "notice error" : "notice";
}

function setAppealMessage(message, isError = false) {
  if (!message) {
    appealMessage.textContent = "";
    appealMessage.className = "inline-note hidden-section";
    return;
  }
  appealMessage.textContent = message;
  appealMessage.className = isError ? "inline-note error" : "inline-note";
}

function formatDate(value) {
  if (!value) {
    return "Not recorded";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString("en-GB", { hour12: false });
}

function resolveStatusConfig(status) {
  return STATUS_CONFIG[status] || STATUS_CONFIG.PENDING;
}

function renderThumbnail(url, title) {
  if (!url) {
    thumbnailPanel.innerHTML = "<div class=\"thumbnail-placeholder\">No thumbnail preview is available for this resource yet.</div>";
    return;
  }
  thumbnailPanel.innerHTML = `<img src="${escapeHtml(url)}" alt="${escapeHtml(title || "Resource thumbnail")}" />`;
}

function renderTags(tags) {
  const normalized = Array.isArray(tags)
    ? tags.filter(Boolean)
    : String(tags || "")
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean);

  if (!normalized.length) {
    tagList.innerHTML = "<p class=\"empty\">No tags were added for this resource.</p>";
    return;
  }

  tagList.innerHTML = normalized
    .map((tag) => `<span class="tag">${escapeHtml(tag)}</span>`)
    .join("");
}

function renderAttachments(data) {
  const ownedAttachments = Array.isArray(data.attachments) ? data.attachments : [];
  const publicFiles = Array.isArray(data.files) ? data.files : [];
  const items = ownedAttachments.length
    ? ownedAttachments.map((item) => ({
        name: item.name,
        url: item.url,
        meta: item.type ? `${item.type} attachment` : "Attachment"
      }))
    : publicFiles.map((item) => ({
        name: item.name,
        url: item.url,
        meta: item.type ? `${item.type} file` : "Published file"
      }));

  if (!items.length) {
    attachmentList.innerHTML = "<p class=\"empty\">No files were attached to this resource.</p>";
    return;
  }

  attachmentList.innerHTML = items.map((item) => `
    <div class="attachment-item">
      <div>
        <div class="attachment-name">${escapeHtml(item.name || "Stored file")}</div>
        <div class="attachment-meta">${escapeHtml(item.meta)}</div>
      </div>
      <a class="attachment-link" href="${escapeHtml(item.url || "#")}" target="_blank" rel="noreferrer">Open</a>
    </div>
  `).join("");
}

function renderLinks(links) {
  if (!Array.isArray(links) || !links.length) {
    linkList.innerHTML = "<p class=\"empty\">No external links were attached to this resource.</p>";
    return;
  }

  linkList.innerHTML = links.map((item) => `
    <div class="link-item">
      <div>
        <div class="link-name">${escapeHtml(item.label || item.url || "External link")}</div>
        <div class="link-meta">${escapeHtml(item.url || "")}</div>
      </div>
      <a class="attachment-link" href="${escapeHtml(item.url || "#")}" target="_blank" rel="noreferrer">Visit</a>
    </div>
  `).join("");
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

function renderAppealMessages(messages) {
  if (!Array.isArray(messages) || !messages.length) {
    appealList.innerHTML = "<p class=\"empty\">No conversation messages have been sent for this resource yet.</p>";
    return;
  }

  appealList.innerHTML = messages.map((item) => {
    const role = String(item.senderRole || "").toLowerCase();
    return `
      <article class="appeal-item ${escapeHtml(role || "contributor")}">
        <div class="appeal-meta">
          <strong>${escapeHtml(item.senderName || labelForRole(item.senderRole))}</strong>
          <span>${escapeHtml(labelForRole(item.senderRole))}</span>
          <span>${escapeHtml(item.createdAt || "")}</span>
        </div>
        <p class="body-text">${escapeHtml(item.content || "")}</p>
      </article>
    `;
  }).join("");
}

function renderRevisionContext(data, status) {
  const feedback = (data.rejectionComments || "").trim();
  const messages = Array.isArray(data.appealMessages) ? data.appealMessages : [];
  const hasContext = Boolean(feedback) || messages.length || Boolean(data.canSendAppeal);

  reviewFeedbackSection.classList.toggle("hidden-section", !feedback);
  if (feedback) {
    reviewFeedbackBox.textContent = feedback;
  }

  appealSection.classList.toggle("hidden-section", !hasContext);
  if (!hasContext) {
    setAppealMessage("");
    return;
  }

  renderAppealMessages(messages);
  if (data.canSendAppeal) {
    appealForm.classList.remove("hidden-section");
    appealReadonlyNote.classList.add("hidden-section");
  } else {
    appealForm.classList.add("hidden-section");
    appealReadonlyNote.classList.remove("hidden-section");
    appealReadonlyNote.textContent = status === "PENDING"
      ? "This conversation becomes read-only while the resource is under review."
      : "This resource is not currently open for new conversation messages.";
  }
}

function renderActions(id, status) {
  const config = resolveStatusConfig(status);
  const buttons = [];

  if (typeof config.primaryAction === "function") {
    const primary = config.primaryAction(id);
    buttons.push(`
      <a class="action-button action-primary" href="${primary.href}">
        ${escapeHtml(primary.label)}
      </a>
    `);
  }

  if (status === "APPROVED") {
    buttons.push(`
      <a class="action-button action-secondary" href="/detail.html?id=${encodeURIComponent(id)}">
        Open Public Page
      </a>
    `);
  }

  if (status === "REJECTED") {
    buttons.push(`
      <a class="action-button action-secondary" href="#appealSection">
        Message Admin
      </a>
    `);
  }

  buttons.push(`
    <a class="action-button action-secondary" href="/my-resources.html?view=${encodeURIComponent(config.view)}">
      Back to My Resources
    </a>
  `);

  actionSummary.textContent = config.summary;
  actionList.innerHTML = buttons.join("");
}

function renderResource(data, sessionUser) {
  currentResource = data;
  const status = normalizeStatus(data.status) || requestedStatus || "PENDING";
  const config = resolveStatusConfig(status);
  const subtitleBits = [
    data.category || "Uncategorised",
    data.place || "Unknown location",
    sessionUser?.username ? `Contributor: ${sessionUser.username}` : ""
  ].filter(Boolean);

  document.title = `${data.title || "My Resource"} - Jiangsu Heritage Discovery`;
  backLink.href = `/my-resources.html?view=${encodeURIComponent(config.view)}`;
  statusBadge.textContent = config.label;
  statusBadge.className = `status-badge ${config.badgeClass}`;
  resourceTitle.textContent = data.title || "Untitled Resource";
  resourceSubtitle.textContent = subtitleBits.join(" | ");
  metaCategory.textContent = data.category || "Not provided";
  metaPlace.textContent = data.place || "Not provided";
  metaPeriod.textContent = data.period || "Not provided";
  metaTracking.textContent = data.trackingId || (status === "APPROVED" ? "Published record" : "Not assigned");
  metaVisibility.textContent = status === "APPROVED"
    ? `${Number(data.viewCount || 0)} public views`
    : "Contributor and moderator access";
  metaRecordTime.textContent = formatDate(data.createdAt || data.savedAt);
  resourceDescription.textContent = data.description || "No description was provided.";
  statusNote.textContent = config.note;

  renderThumbnail(data.thumbnail, data.title);
  renderTags(data.tags);
  renderAttachments(data);
  renderLinks(data.links);
  renderRevisionContext(data, status);
  renderActions(data.id || resourceId, status);
}

async function loadResourceDetail() {
  if (!resourceId) {
    throw new Error("No resource id was provided.");
  }

  if (requestedStatus === "APPROVED") {
    return getResourceById(resourceId);
  }

  if (requestedStatus) {
    return getOwnedDraft(resourceId);
  }

  try {
    return await getOwnedDraft(resourceId);
  } catch (error) {
    if (error.status === 404) {
      return getResourceById(resourceId);
    }
    throw error;
  }
}

async function init() {
  try {
    const sessionUser = await getSessionUser();
    if (!sessionUser.canUpload) {
      showNotice("Your current account cannot access contributor resource management.", true);
      sessionPill.textContent = `${sessionUser.username} | ${sessionUser.roleLabel}`;
      return;
    }

    sessionPill.textContent = `${sessionUser.username} | ${sessionUser.roleLabel}`;

    const view = resolveStatusConfig(requestedStatus || "APPROVED").view;
    backLink.href = `/my-resources.html?view=${encodeURIComponent(view)}`;

    const data = await loadResourceDetail();
    renderResource(data, sessionUser);
    if (window.location.hash === "#appealSection") {
      appealSection?.scrollIntoView({ behavior: "smooth", block: "start" });
    }
    showNotice("");
  } catch (error) {
    if (error.status === 401) {
      window.location.href = "/login.html";
      return;
    }

    showNotice(error.message || "Failed to load this resource record.", true);
    resourceTitle.textContent = "Resource not found";
    resourceSubtitle.textContent = "We could not load this contributor record.";
    actionList.innerHTML = `
      <a class="action-button action-secondary" href="/my-resources.html">
        Back to My Resources
      </a>
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

sendAppealBtn?.addEventListener("click", async () => {
  const content = appealInput?.value.trim();
  if (!currentResource?.id) {
    return;
  }
  if (!content) {
    setAppealMessage("Message content is required.", true);
    return;
  }

  sendAppealBtn.disabled = true;
  try {
    const response = await submitResourceAppeal(currentResource.id, content);
    currentResource = {
      ...currentResource,
      appealMessages: response.appealMessages || currentResource.appealMessages
    };
    renderAppealMessages(currentResource.appealMessages);
    appealInput.value = "";
    setAppealMessage(response.message || "Message sent.");
  } catch (error) {
    setAppealMessage(error.message || "Failed to send message.", true);
  } finally {
    sendAppealBtn.disabled = false;
  }
});

init();
