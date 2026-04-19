import {
  getCurrentContributorApplication,
  getProfileEngagement,
  getProfile,
  getMyContributorApplications,
  logout,
  submitContributorApplicationAppeal,
  updateEmail,
  updatePassword,
  updateProfile,
  uploadAvatar
} from "./heritage-data.js";

const pageMessage = document.getElementById("pageMessage");
const profileForm = document.getElementById("profileForm");
const saveProfileBtn = document.getElementById("saveProfileBtn");

const usernameInput = document.getElementById("username");
const bioInput = document.getElementById("bio");
const bioCount = document.getElementById("bioCount");
const avatarFileInput = document.getElementById("avatarFile");
const uploadAvatarBtn = document.getElementById("uploadAvatarBtn");
const avatarUploadTrigger = document.getElementById("avatarUploadTrigger");

const avatarPreview = document.getElementById("avatarPreview");
const avatarFallback = document.getElementById("avatarFallback");
const sidebarName = document.getElementById("sidebarName");
const sidebarRole = document.getElementById("sidebarRole");
const sidebarEmail = document.getElementById("sidebarEmail");
const sidebarBio = document.getElementById("sidebarBio");
const applicationRecordsBtn = document.getElementById("applicationRecordsBtn");
const myResourcesBtn = document.getElementById("myResourcesBtn");
const contributorApplicationPanel = document.getElementById("contributorApplicationPanel");
const applicationStatusBadge = document.getElementById("applicationStatusBadge");
const applicationSummaryCard = document.getElementById("applicationSummaryCard");
const emailValue = document.getElementById("emailValue");
const roleBadge = document.getElementById("roleBadge");
const roleDescription = document.getElementById("roleDescription");
const uploadResourceBtn = document.getElementById("uploadResourceBtn");
const uploadResourceDesc = document.getElementById("uploadResourceDesc");
const engagementPanel = document.getElementById("engagementPanel");
const engagementChart = document.getElementById("engagementChart");
const engagementTotalLikes = document.getElementById("engagementTotalLikes");
const emailForm = document.getElementById("emailForm");
const newEmailInput = document.getElementById("newEmail");
const emailCurrentPasswordInput = document.getElementById("emailCurrentPassword");
const submitEmailBtn = document.getElementById("submitEmailBtn");
const emailMessage = document.getElementById("emailMessage");
const passwordForm = document.getElementById("passwordForm");
const currentPasswordInput = document.getElementById("currentPassword");
const newPasswordInput = document.getElementById("newPassword");
const confirmPasswordInput = document.getElementById("confirmPassword");
const submitPasswordBtn = document.getElementById("submitPasswordBtn");
const passwordMessage = document.getElementById("passwordMessage");
let currentAvatarUrl = "";
let canUploadResource = false;
let currentContributorApplication = null;

function renderContributorEngagement(data) {
  if (!engagementPanel || !engagementChart || !engagementTotalLikes) {
    return;
  }

  const points = Array.isArray(data?.dailyReceivedLikes) ? data.dailyReceivedLikes : [];
  const totalLikes = Number(data?.totalReceivedLikes || 0);
  engagementTotalLikes.textContent = String(totalLikes);

  if (!points.length) {
    engagementPanel.classList.remove("hidden");
    engagementChart.innerHTML = `
      <div class="rounded-xl border border-dashed border-surface-line bg-white px-4 py-8 text-center text-sm text-text-soft">
        No public likes have been recorded for your resources yet.
      </div>
    `;
    return;
  }

  const normalizedPoints = points.map((item) => ({
    date: item.date || "",
    label: item.label || "",
    count: Number(item.count || 0)
  }));
  const maxValue = Math.max(...normalizedPoints.map((item) => item.count), 1);
  const bestDay = normalizedPoints.reduce(
    (best, item) => item.count > best.count ? item : best,
    normalizedPoints[0]
  );
  const width = 920;
  const height = 260;
  const paddingLeft = 58;
  const paddingRight = 24;
  const paddingTop = 24;
  const paddingBottom = 52;
  const chartWidth = width - paddingLeft - paddingRight;
  const chartHeight = height - paddingTop - paddingBottom;
  const xStep = normalizedPoints.length > 1 ? chartWidth / (normalizedPoints.length - 1) : 0;
  const linePath = normalizedPoints.map((item, index) => {
    const x = paddingLeft + xStep * index;
    const y = paddingTop + chartHeight - (item.count / maxValue) * chartHeight;
    return `${index === 0 ? "M" : "L"} ${x} ${y}`;
  }).join(" ");
  const areaPath = `${linePath} L ${paddingLeft + xStep * (normalizedPoints.length - 1)} ${paddingTop + chartHeight} L ${paddingLeft} ${paddingTop + chartHeight} Z`;
  const yTicks = Array.from({ length: 4 }, (_, index) => {
    const value = Math.round((maxValue / 3) * (3 - index));
    const y = paddingTop + (chartHeight / 3) * index;
    return { value, y };
  });

  engagementPanel.classList.remove("hidden");
  engagementChart.innerHTML = `
    <div class="grid gap-4 lg:grid-cols-[minmax(0,1fr)_240px] lg:items-start">
      <div class="rounded-xl bg-white px-3 py-4">
        <div class="mb-3 flex flex-wrap items-center justify-between gap-3 px-2">
          <div>
            <div class="text-xs uppercase tracking-[0.18em] text-outline">Trend</div>
            <div class="text-sm text-text-soft">X-axis: month/day, Y-axis: likes received</div>
          </div>
          <div class="rounded-full bg-primary/5 px-3 py-1 text-xs font-semibold text-primary">
            Total likes: ${totalLikes}
          </div>
        </div>
        <div class="overflow-x-auto">
          <svg viewBox="0 0 ${width} ${height}" class="min-w-[760px] w-full h-[280px]" aria-label="Contributor likes trend chart">
            ${yTicks.map((tick) => `
              <g>
                <line x1="${paddingLeft}" y1="${tick.y}" x2="${width - paddingRight}" y2="${tick.y}" stroke="rgba(112, 121, 116, 0.15)" stroke-width="1" />
                <text x="${paddingLeft - 12}" y="${tick.y + 4}" text-anchor="end" font-size="11" fill="#707974">${tick.value}</text>
              </g>
            `).join("")}
            <line x1="${paddingLeft}" y1="${paddingTop + chartHeight}" x2="${width - paddingRight}" y2="${paddingTop + chartHeight}" stroke="rgba(55, 103, 87, 0.22)" stroke-width="1.5" />
            <path d="${areaPath}" fill="rgba(55, 103, 87, 0.10)"></path>
            <path d="${linePath}" fill="none" stroke="#376757" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"></path>
            ${normalizedPoints.map((item, index) => {
              const x = paddingLeft + xStep * index;
              const y = paddingTop + chartHeight - (item.count / maxValue) * chartHeight;
              return `
                <g>
                  <circle cx="${x}" cy="${y}" r="4.5" fill="#376757"></circle>
                  <circle cx="${x}" cy="${y}" r="9" fill="transparent">
                    <title>${escapeHtml(item.date)}: ${item.count} likes</title>
                  </circle>
                  <text x="${x}" y="${paddingTop + chartHeight + 22}" text-anchor="middle" font-size="11" fill="#707974">${escapeHtml(item.label)}</text>
                </g>
              `;
            }).join("")}
          </svg>
        </div>
      </div>
      <div class="rounded-xl border border-primary/15 bg-primary/5 px-4 py-4 text-sm text-text-soft">
        <div class="text-[11px] uppercase tracking-[0.18em] text-outline">Highlights</div>
        <div class="mt-3 space-y-3">
          <div>
            <div class="font-semibold text-text-main">Best Day</div>
            <div>${escapeHtml(bestDay.date || "-")} with ${bestDay.count} likes</div>
          </div>
          <div>
            <div class="font-semibold text-text-main">Range</div>
            <div>Last ${normalizedPoints.length} days of public saves on your resources</div>
          </div>
          <div>
            <div class="font-semibold text-text-main">Total</div>
            <div>${totalLikes} likes collected across all your resources</div>
          </div>
        </div>
      </div>
    </div>
  `;
}

async function loadContributorEngagementSafely() {
  try {
    const engagement = await getProfileEngagement();
    renderContributorEngagement(engagement);
  } catch (error) {
    engagementPanel?.classList.remove("hidden");
    if (engagementChart) {
      engagementChart.innerHTML = `
        <div class="rounded-xl border border-dashed border-surface-line bg-white px-4 py-8 text-center text-sm text-text-soft">
          We could not load your likes trend right now.
        </div>
      `;
    }
    if (engagementTotalLikes) {
      engagementTotalLikes.textContent = "0";
    }
  }
}

function escapeHtml(value) {
  return String(value || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function renderApplicationAppealMessages(messages) {
  if (!Array.isArray(messages) || !messages.length) {
    return `
      <div class="rounded-xl border border-surface-line bg-white/70 px-4 py-4 text-sm text-text-soft">
        No appeal messages have been sent yet.
      </div>
    `;
  }

  return `
    <div class="space-y-3">
      ${messages.map((item) => {
        const isAdmin = String(item.senderRole || "").toUpperCase() === "ADMIN";
        return `
          <article class="rounded-xl border px-4 py-4 ${isAdmin ? "border-primary/15 bg-primary/5" : "border-red-200 bg-red-50/70"}">
            <div class="flex flex-wrap items-center gap-2 text-xs uppercase tracking-[0.16em] text-outline">
              <span>${escapeHtml(item.createdAt || "")}</span>
              <strong class="text-text-main tracking-normal normal-case">${escapeHtml(item.senderName || "")}</strong>
            </div>
            <p class="mt-2 text-sm leading-6 text-text-main">${escapeHtml(item.content || "")}</p>
          </article>
        `;
      }).join("")}
    </div>
  `;
}

function bindApplicationAppealComposer(application) {
  const button = document.getElementById("applicationAppealSendBtn");
  const input = document.getElementById("applicationAppealInput");
  const status = document.getElementById("applicationAppealStatus");
  if (!button || !input || !status || !application?.canSendAppeal) {
    return;
  }

  button.addEventListener("click", async () => {
    const content = input.value.trim();
    if (!content) {
      status.textContent = "Appeal message content is required.";
      status.className = "mt-3 text-xs leading-5 text-red-700";
      return;
    }

    button.disabled = true;
    status.textContent = "";
    try {
      const response = await submitContributorApplicationAppeal(content);
      currentContributorApplication = {
        ...application,
        appealMessages: response.messages || application.appealMessages
      };
      renderContributorApplicationSummary(currentContributorApplication);
      showMessage(response.message || "Appeal message sent.", "success");
    } catch (error) {
      status.textContent = error.message || "Failed to send appeal message.";
      status.className = "mt-3 text-xs leading-5 text-red-700";
    } finally {
      button.disabled = false;
    }
  });
}

function renderContributorApplicationSummary(application) {
  if (!contributorApplicationPanel || !applicationSummaryCard || !applicationStatusBadge) {
    return;
  }

  contributorApplicationPanel.classList.remove("hidden");
  currentContributorApplication = application;

  if (!application) {
    applicationStatusBadge.textContent = "No Record";
    applicationStatusBadge.className = "inline-flex items-center rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-[0.16em] border-surface-line bg-surface-soft text-text-soft";
    applicationSummaryCard.dataset.status = "NONE";
    applicationSummaryCard.className = "application-summary-card border-surface-line bg-surface-soft text-text-soft";
    applicationSummaryCard.innerHTML = `
      <div class="text-xs uppercase tracking-[0.18em] text-outline">Current Status</div>
      <p class="mt-2 text-sm leading-6">You have not submitted a contributor application yet.</p>
    `;
    return;
  }

  const status = application.status || "PENDING";
  const statusLabel = status === "APPROVED"
    ? "Approved"
    : status === "REJECTED"
      ? "Rejected"
      : "Pending";
  const badgeClass = status === "APPROVED"
    ? "inline-flex items-center rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-[0.16em] border-emerald-200 bg-emerald-50 text-emerald-700"
    : status === "REJECTED"
      ? "inline-flex items-center rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-[0.16em] border-red-200 bg-red-50 text-red-700"
      : "inline-flex items-center rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-[0.16em] border-amber-200 bg-amber-50 text-amber-700";
  const summaryText = status === "APPROVED"
    ? "Your contributor access is active. You can now upload heritage resources."
    : status === "REJECTED"
      ? "Your application was rejected. Please read the admin feedback before applying again."
      : "Your application is still being reviewed by the admin team.";
  const reviewRow = application.reviewedAt
    ? `<div><strong class="text-text-main">Reviewed:</strong> ${application.reviewedAt}</div>`
    : "";
  const feedbackRow = application.rejectionComments
    ? `<div class="rounded-xl border border-current/15 bg-white/60 px-3 py-3"><strong class="block text-text-main mb-1">Admin Feedback</strong><span>${application.rejectionComments}</span></div>`
    : "";
  const link = application.portfolioLink || application.attachmentUrl || "";
  const linkRow = link
    ? `<a class="font-semibold underline" href="${link}" target="_blank" rel="noreferrer">Open supporting link</a>`
    : "<div>No supporting link attached.</div>";
  const appealThread = status === "REJECTED"
    ? `
      <div class="mt-4 border-t border-current/15 pt-4">
        <div class="text-xs uppercase tracking-[0.18em] opacity-80 mb-3">Appeal Thread</div>
        ${renderApplicationAppealMessages(application.appealMessages || [])}
        ${application.canSendAppeal ? `
          <div class="mt-4 space-y-3">
            <textarea id="applicationAppealInput" rows="4" maxlength="1000"
              placeholder="Explain your qualifications, supplementary evidence, or why you would like the admin team to reconsider."
              class="w-full rounded-xl border border-surface-line bg-white px-3 py-3 text-sm leading-6 text-text-main focus:ring-2 focus:ring-primary/20 focus:border-primary resize-y"></textarea>
            <button id="applicationAppealSendBtn" type="button"
              class="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-white hover:bg-[#2d5648] transition-all">
              Send Appeal Message
            </button>
            <p id="applicationAppealStatus" class="mt-3 text-xs leading-5 text-text-soft"></p>
          </div>
        ` : `
          <p class="mt-4 text-xs leading-5">This application is no longer accepting appeal messages.</p>
        `}
      </div>
    `
    : "";

  applicationStatusBadge.textContent = statusLabel;
  applicationStatusBadge.className = badgeClass;
  applicationSummaryCard.dataset.status = status;
  applicationSummaryCard.className = "application-summary-card";
  applicationSummaryCard.innerHTML = `
    <div class="text-xs uppercase tracking-[0.18em] opacity-80">Current Status</div>
    <p class="mt-2 text-sm font-semibold leading-6">${summaryText}</p>
    <div class="mt-4 grid gap-3 text-sm leading-6">
      <div><strong class="text-text-main">Expertise Field:</strong> ${application.expertiseField || "-"}</div>
      <div><strong class="text-text-main">Submitted:</strong> ${application.submittedAt || "-"}</div>
      ${reviewRow}
      ${feedbackRow}
      ${linkRow}
    </div>
    ${appealThread}
  `;

  if (status === "REJECTED") {
    bindApplicationAppealComposer(application);
  }
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
    pageMessage.className = "mb-6 px-5 py-4 rounded-xl border text-sm bg-red-50 border-red-200 text-red-700";
    return;
  }

  if (type === "success") {
    pageMessage.className = "mb-6 px-5 py-4 rounded-xl border text-sm bg-primary/5 border-primary/20 text-primary";
    return;
  }

  pageMessage.className = "mb-6 px-5 py-4 rounded-xl border text-sm bg-surface-soft border-surface-line text-text-soft";
}

function showPasswordMessage(message, type = "info") {
  if (!passwordMessage) return;
  if (!message) {
    passwordMessage.classList.add("hidden");
    passwordMessage.textContent = "";
    return;
  }

  passwordMessage.classList.remove("hidden");
  passwordMessage.textContent = message;
  if (type === "error") {
    passwordMessage.className = "text-xs leading-5 text-red-700";
    return;
  }
  if (type === "success") {
    passwordMessage.className = "text-xs leading-5 text-primary";
    return;
  }
  passwordMessage.className = "text-xs leading-5 text-text-soft";
}

function showEmailMessage(message, type = "info") {
  if (!emailMessage) return;
  if (!message) {
    emailMessage.classList.add("hidden");
    emailMessage.textContent = "";
    return;
  }

  emailMessage.classList.remove("hidden");
  emailMessage.textContent = message;
  if (type === "error") {
    emailMessage.className = "text-xs leading-5 text-red-700";
    return;
  }
  if (type === "success") {
    emailMessage.className = "text-xs leading-5 text-primary";
    return;
  }
  emailMessage.className = "text-xs leading-5 text-text-soft";
}

function setAvatarPreview(url) {
  const normalized = (url || "").trim();
  if (!normalized) {
    avatarPreview.classList.add("hidden");
    avatarPreview.removeAttribute("src");
    avatarFallback.classList.remove("hidden");
    return;
  }

  avatarPreview.src = normalized;
  avatarPreview.classList.remove("hidden");
  avatarFallback.classList.add("hidden");
}

function updateBioCount() {
  bioCount.textContent = String((bioInput.value || "").length);
}

function renderRole(user) {
  const currentRole = user.roleLabel || user.role || "User";
  roleBadge.textContent = currentRole;
  roleDescription.textContent = `Your current role: ${currentRole}`;

  if (user.role === "ADMIN") {
    roleBadge.className = "inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border bg-primary/10 border-primary/20 text-primary";
    return;
  }

  if (user.role === "CONTRIBUTOR") {
    roleBadge.className = "inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border bg-tertiary/10 border-tertiary/25 text-tertiary";
    return;
  }

  roleBadge.className = "inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border bg-surface-soft border-surface-line text-text-soft";
}

function applyProfileToUI(user) {
  usernameInput.value = user.username || "";
  bioInput.value = user.bio || "";
  currentAvatarUrl = (user.avatarUrl || "").trim();
  updateBioCount();
  setAvatarPreview(currentAvatarUrl);

  sidebarName.textContent = user.username || "Unknown user";
  sidebarRole.textContent = user.roleLabel || user.role || "User";
  sidebarEmail.textContent = user.email || "-";
  sidebarBio.textContent = (user.bio || "").trim() || "No biography yet.";
  emailValue.textContent = user.email || "-";
  renderRole(user);

  const applyContributorBtn = document.getElementById("applyContributorBtn");
  const isRegisteredUser = user.role === "USER";
  const isContributor = user.role === "CONTRIBUTOR";
  const isContributorOrAdmin = user.role === "CONTRIBUTOR" || user.role === "ADMIN";

  if (isRegisteredUser) {
    applicationRecordsBtn.classList.remove("hidden");
  } else {
    applicationRecordsBtn.classList.add("hidden");
  }

  if (isContributor) {
    myResourcesBtn.classList.remove("hidden");
  } else {
    myResourcesBtn.classList.add("hidden");
  }

  if (isContributor) {
    applyContributorBtn.classList.add("hidden");
  } else {
    applyContributorBtn.classList.remove("hidden");
  }

  if (isContributorOrAdmin) {
    applyContributorBtn.disabled = true;
    applyContributorBtn.classList.add("opacity-60", "cursor-not-allowed");
    applyContributorBtn.querySelector("div").textContent = "Contributor Role Active";
  } else {
    applyContributorBtn.disabled = false;
    applyContributorBtn.classList.remove("opacity-60", "cursor-not-allowed");
    applyContributorBtn.querySelector("div").textContent = "Apply for Contributor";
  }

  canUploadResource = isContributorOrAdmin;
  if (canUploadResource) {
    uploadResourceBtn.disabled = false;
    uploadResourceBtn.classList.remove("opacity-60", "cursor-not-allowed");
    if (isContributor) {
      uploadResourceDesc.textContent = "As a contributor, you can upload new heritage resources.";
    } else {
      uploadResourceDesc.textContent = "Upload and manage your heritage resources.";
    }
  } else {
    uploadResourceBtn.disabled = true;
    uploadResourceBtn.classList.add("opacity-60", "cursor-not-allowed");
    if (isRegisteredUser) {
      uploadResourceDesc.textContent = "Available after your contributor application is approved.";
    } else {
      uploadResourceDesc.textContent = "Upload and manage your heritage resources.";
    }
  }
}

async function loadProfile() {
  try {
    const user = await getProfile();
    if (user.guest) {
      showMessage("Guest mode cannot edit profile. Please sign in with a registered account.", "error");
      saveProfileBtn.disabled = true;
      profileForm.querySelectorAll("input, textarea, button").forEach(element => {
        if (element.id !== "changeEmailBtn" && element.id !== "changePasswordBtn" && element.id !== "applyContributorBtn" && element.id !== "uploadResourceBtn") {
          element.disabled = true;
        }
      });
    }
    applyProfileToUI(user);
    if (user.role === "USER") {
      engagementPanel?.classList.add("hidden");
      const currentApplication = await getCurrentContributorApplication();
      renderContributorApplicationSummary(currentApplication || null);
    } else if (user.role === "CONTRIBUTOR") {
      renderContributorApplicationSummary({
        status: "APPROVED",
        expertiseField: "Contributor account active",
        submittedAt: "Approved account",
        reviewedAt: "",
        rejectionComments: "",
        portfolioLink: "",
        attachmentUrl: ""
      });
      await loadContributorEngagementSafely();
    } else {
      renderContributorApplicationSummary(null);
      if (user.role === "ADMIN") {
        await loadContributorEngagementSafely();
      } else {
        engagementPanel?.classList.add("hidden");
      }
    }
  } catch (error) {
    if (error.status === 401) {
      window.location.href = "/login.html";
      return;
    }
    showMessage(error.message || "Failed to load profile data.", "error");
  }
}

async function handleProfileSubmit(event) {
  event.preventDefault();
  showMessage("");

  const payload = {
    username: usernameInput.value.trim(),
    bio: bioInput.value.trim(),
    avatarUrl: currentAvatarUrl
  };

  saveProfileBtn.disabled = true;
  const originalLabel = saveProfileBtn.innerHTML;
  saveProfileBtn.innerHTML = '<span class="material-symbols-outlined text-base">progress_activity</span> Saving...';

  try {
    const user = await updateProfile(payload);
    applyProfileToUI(user);
    showMessage("Profile updated successfully.", "success");
  } catch (error) {
    showMessage(error.message || "Failed to save profile.", "error");
  } finally {
    saveProfileBtn.disabled = false;
    saveProfileBtn.innerHTML = originalLabel;
  }
}

async function handleAvatarUpload() {
  const file = avatarFileInput.files?.[0];
  if (!file) {
    return;
  }

  if (file.size > 2 * 1024 * 1024) {
    showMessage("Avatar image must be 2MB or smaller.", "error");
    return;
  }

  const allowedTypes = ["image/jpeg", "image/png", "image/webp"];
  if (!allowedTypes.includes(file.type)) {
    showMessage("Only JPG, PNG, and WEBP images are supported.", "error");
    return;
  }

  const originalLabel = uploadAvatarBtn.innerHTML;
  uploadAvatarBtn.disabled = true;
  uploadAvatarBtn.innerHTML = '<span class="material-symbols-outlined text-base">progress_activity</span> Uploading...';

  try {
    const updatedUser = await uploadAvatar(file);
    applyProfileToUI(updatedUser);
    avatarFileInput.value = "";
    showMessage("Avatar uploaded successfully.", "success");
  } catch (error) {
    showMessage(error.message || "Failed to upload avatar.", "error");
  } finally {
    uploadAvatarBtn.disabled = false;
    uploadAvatarBtn.innerHTML = originalLabel;
  }
}

function resetPasswordForm() {
  if (!passwordForm) return;
  passwordForm.classList.add("hidden");
  currentPasswordInput.value = "";
  newPasswordInput.value = "";
  confirmPasswordInput.value = "";
  showPasswordMessage("");
}

function resetEmailForm() {
  if (!emailForm) return;
  emailForm.classList.add("hidden");
  newEmailInput.value = "";
  emailCurrentPasswordInput.value = "";
  showEmailMessage("");
}

function openEmailForm() {
  if (!emailForm) return;
  emailForm.classList.remove("hidden");
  newEmailInput.focus();
  showEmailMessage("");
}

function openPasswordForm() {
  if (!passwordForm) return;
  passwordForm.classList.remove("hidden");
  currentPasswordInput.focus();
  showPasswordMessage("");
}

async function handlePasswordSubmit(event) {
  event.preventDefault();

  const currentPassword = currentPasswordInput.value;
  const newPassword = newPasswordInput.value;
  const confirmPassword = confirmPasswordInput.value;
  showPasswordMessage("");

  if (!currentPassword || !newPassword || !confirmPassword) {
    showPasswordMessage("Please complete all password fields.", "error");
    return;
  }

  if (newPassword.length < 8) {
    showPasswordMessage("New password must be at least 8 characters.", "error");
    return;
  }

  if (newPassword !== confirmPassword) {
    showPasswordMessage("New password and confirmation do not match.", "error");
    return;
  }

  const originalLabel = submitPasswordBtn.innerHTML;
  submitPasswordBtn.disabled = true;
  submitPasswordBtn.innerHTML = '<span class="material-symbols-outlined text-sm">progress_activity</span> Updating...';

  try {
    await updatePassword({ currentPassword, newPassword });
    showPasswordMessage("Password updated successfully.", "success");
    setTimeout(() => {
      resetPasswordForm();
    }, 700);
  } catch (error) {
    showPasswordMessage(error.message || "Failed to update password.", "error");
  } finally {
    submitPasswordBtn.disabled = false;
    submitPasswordBtn.innerHTML = originalLabel;
  }
}

async function handleEmailSubmit(event) {
  event.preventDefault();
  const newEmail = newEmailInput.value.trim();
  const currentPassword = emailCurrentPasswordInput.value;
  showEmailMessage("");

  if (!newEmail || !currentPassword) {
    showEmailMessage("Please provide new email and current password.", "error");
    return;
  }

  const originalLabel = submitEmailBtn.innerHTML;
  submitEmailBtn.disabled = true;
  submitEmailBtn.innerHTML = '<span class="material-symbols-outlined text-sm">progress_activity</span> Updating...';

  try {
    const updatedUser = await updateEmail({ newEmail, currentPassword });
    applyProfileToUI(updatedUser);
    showEmailMessage("Email updated successfully.", "success");
    setTimeout(() => {
      resetEmailForm();
    }, 700);
  } catch (error) {
    showEmailMessage(error.message || "Failed to update email.", "error");
  } finally {
    submitEmailBtn.disabled = false;
    submitEmailBtn.innerHTML = originalLabel;
  }
}

function bindEvents() {
  profileForm?.addEventListener("submit", handleProfileSubmit);

  bioInput?.addEventListener("input", updateBioCount);

  avatarUploadTrigger?.addEventListener("click", () => {
    avatarFileInput?.click();
  });

  uploadAvatarBtn?.addEventListener("click", () => {
    avatarFileInput?.click();
  });

  avatarFileInput?.addEventListener("change", () => {
    handleAvatarUpload();
  });

  avatarPreview?.addEventListener("error", () => {
    avatarPreview.classList.add("hidden");
    avatarFallback.classList.remove("hidden");
  });

  document.getElementById("logoutBtn")?.addEventListener("click", async () => {
    try {
      await logout();
    } finally {
      window.location.href = "/login.html";
    }
  });

  document.getElementById("changeEmailBtn")?.addEventListener("click", () => {
    if (emailForm?.classList.contains("hidden")) {
      openEmailForm();
    } else {
      resetEmailForm();
    }
  });

  emailForm?.addEventListener("submit", handleEmailSubmit);

  document.getElementById("cancelEmailBtn")?.addEventListener("click", () => {
    resetEmailForm();
  });

  document.getElementById("changePasswordBtn")?.addEventListener("click", () => {
    if (passwordForm?.classList.contains("hidden")) {
      openPasswordForm();
    } else {
      resetPasswordForm();
    }
  });

  passwordForm?.addEventListener("submit", handlePasswordSubmit);

  document.getElementById("cancelPasswordBtn")?.addEventListener("click", () => {
    resetPasswordForm();
  });

  document.getElementById("applyContributorBtn")?.addEventListener("click", () => {
    window.location.href = "/applicant.html";
  });

  document.getElementById("applicationRecordsBtn")?.addEventListener("click", async () => {
    try {
      const [currentApplication, items] = await Promise.all([
        getCurrentContributorApplication(),
        getMyContributorApplications()
      ]);
      if (!currentApplication && !items.length) {
        renderContributorApplicationSummary(null);
        showMessage("You have not submitted a contributor application yet.", "info");
        return;
      }

      const latest = currentApplication || items[0];
      renderContributorApplicationSummary(latest);
      const detail = [
        `Latest status: ${latest.status}`,
        `Submitted at: ${latest.submittedAt}`,
        latest.portfolioLink ? `Portfolio link ready in your latest application.` : "No portfolio link in latest application."
      ].join(" ");
      showMessage(detail, "success");
    } catch (error) {
      showMessage(error.message || "Failed to load application records.", "error");
    }
  });

  document.getElementById("myResourcesBtn")?.addEventListener("click", () => {
    window.location.href = "/my-resources.html";
  });

  document.getElementById("favoritesBtn")?.addEventListener("click", () => {
    window.location.href = "/my-favorites.html";
  });

  document.getElementById("uploadResourceBtn")?.addEventListener("click", () => {
    if (!canUploadResource) {
      return;
    }
    window.location.href = "/submit-resource.html";
  });
}

function init() {
  loadProfile();
  bindEvents();
}

init();
