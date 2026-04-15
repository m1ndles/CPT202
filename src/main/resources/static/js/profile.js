import {
  getCurrentContributorApplication,
  getProfile,
  getMyContributorApplications,
  logout,
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

function renderContributorApplicationSummary(application) {
  if (!contributorApplicationPanel || !applicationSummaryCard || !applicationStatusBadge) {
    return;
  }

  contributorApplicationPanel.classList.remove("hidden");

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
  `;
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
    } else {
      renderContributorApplicationSummary(null);
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
