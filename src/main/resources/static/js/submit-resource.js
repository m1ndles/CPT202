import { getSessionUser, logout, submitResourceAppeal } from './heritage-data.js';

const form = document.getElementById('resourceForm');
const saveText = document.getElementById('saveText');
const submitMessage = document.getElementById('submitMessage');
const saveError = document.getElementById('saveError');
const saveErrorText = document.getElementById('saveErrorText');
const retrySaveBtn = document.getElementById('retrySaveBtn');
const resourceIdInput = document.getElementById('resourceId');
const saveDraftBtn = document.getElementById('saveDraftBtn');
const submitBtn = document.getElementById('submitBtn');
const confirmModal = document.getElementById('confirmModal');
const cancelModalBtn = document.getElementById('cancelModalBtn');
const confirmSubmitBtn = document.getElementById('confirmSubmitBtn');
const successToast = document.getElementById('successToast');
const toastMessage = document.getElementById('toastMessage');
const logoutBtn = document.getElementById('logoutBtn');
const fileInput = document.getElementById('fileInput');
const dropzone = document.getElementById('dropzone');
const uploadedFilesList = document.getElementById('uploadedFilesList');
const uploadedFilesTitle = document.getElementById('uploadedFilesTitle');
const uploadError = document.getElementById('uploadError');
const hiddenTags = document.getElementById('hiddenTags');
const attachmentNames = document.getElementById('attachmentNames');
const revisionContextSection = document.getElementById('revisionContextSection');
const revisionFeedbackText = document.getElementById('revisionFeedbackText');
const revisionAppealList = document.getElementById('revisionAppealList');
const appealFormCard = document.getElementById('appealFormCard');
const appealReadonlyCard = document.getElementById('appealReadonlyCard');
const appealReadonlyText = document.getElementById('appealReadonlyText');
const appealInput = document.getElementById('appealInput');
const sendAppealBtn = document.getElementById('sendAppealBtn');
const appealStatus = document.getElementById('appealStatus');

const attachments = [];
const tags = [];
const AUTOSAVE_DELAY_MS = 3000;
const ACTIVE_DRAFT_STORAGE_KEY = 'resource-submission-active-draft';

let autosaveTimer = null;
let autosaveEnabled = true;
let saveInFlight = false;
let pendingAutosave = false;
let readOnlyMode = false;
let currentRevisionContext = {
  rejectionComments: '',
  appealMessages: [],
  canSendAppeal: false,
  status: ''
};

function setSaveState(text) {
  saveText.textContent = text;
}

function setAppealStatus(message, isError = false) {
  if (!message) {
    appealStatus.textContent = '';
    appealStatus.className = 'hidden mt-3 rounded-xl border px-4 py-3 text-sm';
    return;
  }

  appealStatus.textContent = message;
  appealStatus.className = isError
    ? 'mt-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600'
    : 'mt-3 rounded-xl border border-primary-200 bg-primary-50 px-4 py-3 text-sm text-primary-700';
}

function setReadOnlyMode(enabled, options = {}) {
  const {
    trackingId = '',
    label = 'Pending Review',
    message = '',
    isError = false
  } = options;

  readOnlyMode = enabled;
  form.querySelectorAll('input, textarea, select, button').forEach(element => {
    element.disabled = enabled;
  });

  fileInput.disabled = enabled;
  saveDraftBtn.disabled = enabled;
  submitBtn.disabled = enabled;
  confirmSubmitBtn.disabled = enabled;
  retrySaveBtn.disabled = enabled;
  appealInput.disabled = enabled || !currentRevisionContext.canSendAppeal;
  sendAppealBtn.disabled = enabled || !currentRevisionContext.canSendAppeal;

  if (enabled) {
    closeModal();
    dropzone.classList.add('pointer-events-none', 'opacity-60');
    setSaveState(label);
    setSubmitMessage(
      message || (
        trackingId
          ? `This resource is now pending review. Tracking ID: ${trackingId}`
          : 'This resource is now pending review and can no longer be edited.'
      ),
      isError
    );
  } else {
    dropzone.classList.remove('pointer-events-none', 'opacity-60');
  }
}

function showSaveError(message) {
  saveErrorText.textContent = message || 'Save failed, please retry.';
  saveError.classList.remove('hidden');
}

function hideSaveError() {
  saveError.classList.add('hidden');
}

function setUploadError(message) {
  if (!message) {
    uploadError.textContent = '';
    uploadError.classList.add('hidden');
    return;
  }
  uploadError.textContent = message;
  uploadError.classList.remove('hidden');
}

function setSubmitMessage(message, isError = false) {
  if (!message) {
    submitMessage.textContent = '';
    submitMessage.className = 'mt-4 hidden rounded-2xl border px-4 py-3 text-sm';
    return;
  }

  submitMessage.textContent = message;
  submitMessage.className = isError
    ? 'mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600'
    : 'mt-4 rounded-2xl border border-primary-200 bg-primary-50 px-4 py-3 text-sm text-primary-700';
}

function showToast(message) {
  toastMessage.textContent = message;
  successToast.classList.remove('translate-x-full');
  window.setTimeout(() => {
    successToast.classList.add('translate-x-full');
  }, 2200);
}

function formatSavedTime(value) {
  const date = value ? new Date(value) : new Date();
  return date.toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

function openModal() {
  confirmModal.classList.remove('hidden');
  requestAnimationFrame(() => {
    confirmModal.classList.remove('modal-hidden');
  });
}

function closeModal() {
  confirmModal.classList.add('modal-hidden');
  window.setTimeout(() => {
    confirmModal.classList.add('hidden');
  }, 180);
}

function updateHiddenValues() {
  hiddenTags.value = tags.join(',');
  attachmentNames.value = attachments.map(item => item.name).join(',');
}

function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function labelForRole(role) {
  switch (String(role || '').toUpperCase()) {
    case 'ADMIN':
      return 'Admin';
    case 'SYSTEM':
      return 'System';
    default:
      return 'Contributor';
  }
}

function buildTagChip(value, list, render) {
  const chip = document.createElement('span');
  chip.className = 'inline-flex items-center gap-1 rounded-full bg-primary-100 px-3 py-1 text-xs font-medium text-primary-700';
  chip.textContent = value;

  const removeBtn = document.createElement('button');
  removeBtn.type = 'button';
  removeBtn.className = 'text-primary-700 hover:text-primary-900';
  removeBtn.textContent = 'x';
  removeBtn.setAttribute('aria-label', `Remove tag ${value}`);
  removeBtn.addEventListener('click', () => {
    const index = list.indexOf(value);
    if (index >= 0) {
      list.splice(index, 1);
      updateHiddenValues();
      render();
      setSaveState('Unsaved changes');
    }
  });

  chip.appendChild(removeBtn);
  return chip;
}

function renderTagList(containerId, inputId, values) {
  const container = document.getElementById(containerId);
  const input = document.getElementById(inputId);
  container.querySelectorAll('[data-chip="true"]').forEach(node => node.remove());

  values.forEach(value => {
    const chip = buildTagChip(value, values, () => renderAllTags());
    chip.dataset.chip = 'true';
    container.insertBefore(chip, input);
  });
}

function renderAllTags() {
  renderTagList('tagContainer', 'tagInput', tags);
}

function bindTagInput(inputId, values) {
  const input = document.getElementById(inputId);
  input.addEventListener('keydown', event => {
    if (event.key !== 'Enter') {
      return;
    }

    event.preventDefault();
    const value = input.value.trim();
    if (!value || values.includes(value)) {
      input.value = '';
      return;
    }

    values.push(value);
    input.value = '';
    updateHiddenValues();
    renderAllTags();
    setSaveState('Unsaved changes');
  });
}

function renderAttachments() {
  uploadedFilesTitle.textContent = `Uploaded Files (${attachments.length})`;
  uploadedFilesList.innerHTML = '';

  attachments.forEach((item, index) => {
    const row = document.createElement('div');
    row.className = 'flex items-center justify-between gap-4 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm';

    const left = document.createElement('div');
    left.className = 'flex min-w-0 items-center gap-3';

    if (item.progress != null && item.progress < 100) {
      const progressWrap = document.createElement('div');
      progressWrap.className = 'w-44';
      const label = document.createElement('div');
      label.className = 'mb-1 truncate text-xs text-slate-500';
      label.textContent = item.name;
      const track = document.createElement('div');
      track.className = 'h-2 overflow-hidden rounded-full bg-slate-200';
      const bar = document.createElement('div');
      bar.className = 'h-full rounded-full bg-primary-500 transition-all';
      bar.style.width = `${item.progress}%`;
      track.appendChild(bar);
      progressWrap.append(label, track);
      left.appendChild(progressWrap);
    } else {
      const preview = document.createElement(item.type === 'image' ? 'img' : 'div');
      if (item.type === 'image') {
        preview.src = item.url;
        preview.alt = item.name;
        preview.className = 'h-14 w-14 rounded-lg object-cover border border-slate-200';
      } else {
        preview.className = 'flex h-14 w-14 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600';
        preview.innerHTML = '<span class="material-symbols-outlined">description</span>';
      }
      left.appendChild(preview);

      const meta = document.createElement('div');
      meta.className = 'min-w-0';
      const name = document.createElement('div');
      name.className = 'truncate font-medium text-slate-700';
      name.textContent = item.name;
      const type = document.createElement('div');
      type.className = 'text-xs text-slate-500';
      type.textContent = item.type === 'image' ? 'Image file' : (item.type === 'audio' ? 'Audio file' : 'Document file');
      meta.append(name, type);
      left.appendChild(meta);
    }

    row.appendChild(left);

    const removeBtn = document.createElement('button');
    removeBtn.type = 'button';
    removeBtn.className = 'ml-4 text-sm font-medium text-red-600 hover:text-red-700';
    removeBtn.textContent = 'Remove';
    removeBtn.disabled = !item.id && item.progress != null && item.progress < 100;
    removeBtn.addEventListener('click', async () => {
      if (readOnlyMode) {
        return;
      }
      if (!item.id) {
        attachments.splice(index, 1);
        updateHiddenValues();
        renderAttachments();
        return;
      }
      try {
        await fetch(`/api/resources/draft/${resourceIdInput.value}/attachments/${item.id}`, {
          method: 'DELETE',
          credentials: 'same-origin'
        });
        attachments.splice(index, 1);
        updateHiddenValues();
        renderAttachments();
        setUploadError('');
      } catch (error) {
        setUploadError('Failed to remove attachment.');
      }
    });
    row.appendChild(removeBtn);

    uploadedFilesList.appendChild(row);
  });
}

function syncAttachments(list) {
  attachments.length = 0;
  list.forEach(item => attachments.push(item));
  updateHiddenValues();
  renderAttachments();
}

function renderAppealThread(messages) {
  if (!Array.isArray(messages) || !messages.length) {
    revisionAppealList.innerHTML = `
      <div class="appeal-bubble system">
        <div class="text-xs font-bold uppercase tracking-[0.18em] text-slate-500">Conversation Timeline</div>
        <p class="mt-3 text-sm leading-7 text-slate-600">No conversation messages have been sent for this resource yet.</p>
      </div>
    `;
    return;
  }

  revisionAppealList.innerHTML = messages.map(item => {
    const role = String(item.senderRole || '').toLowerCase() || 'contributor';
    return `
      <article class="appeal-bubble ${escapeHtml(role)}">
        <div class="flex flex-wrap items-center gap-2 text-xs uppercase tracking-[0.14em] text-slate-500">
          <strong class="text-slate-700">${escapeHtml(item.senderName || labelForRole(item.senderRole))}</strong>
          <span>${escapeHtml(labelForRole(item.senderRole))}</span>
          <span>${escapeHtml(item.createdAt || '')}</span>
        </div>
        <p class="mt-3 text-sm leading-7 text-slate-700">${escapeHtml(item.content || '')}</p>
      </article>
    `;
  }).join('');
}

function hideRevisionContext() {
  currentRevisionContext = {
    rejectionComments: '',
    appealMessages: [],
    canSendAppeal: false,
    status: currentRevisionContext.status || ''
  };
  revisionContextSection.classList.add('hidden');
  revisionFeedbackText.textContent = 'No reviewer feedback is currently stored for this resource.';
  revisionAppealList.innerHTML = '';
  appealFormCard.classList.add('hidden');
  appealReadonlyCard.classList.add('hidden');
  appealInput.value = '';
  setAppealStatus('');
}

function renderRevisionContext(data = {}) {
  const feedback = String(data.rejectionComments || '').trim();
  const appealMessages = Array.isArray(data.appealMessages) ? data.appealMessages : [];
  const canSendAppeal = Boolean(data.canSendAppeal);
  const status = String(data.status || '').toUpperCase();
  const hasContext = Boolean(feedback) || appealMessages.length || canSendAppeal;

  currentRevisionContext = {
    rejectionComments: feedback,
    appealMessages,
    canSendAppeal,
    status
  };

  if (!hasContext) {
    hideRevisionContext();
    return;
  }

  revisionContextSection.classList.remove('hidden');
  revisionFeedbackText.textContent = feedback || 'No reviewer feedback is currently stored for this resource.';
  renderAppealThread(appealMessages);
  setAppealStatus('');

  if (canSendAppeal) {
    appealFormCard.classList.remove('hidden');
    appealReadonlyCard.classList.add('hidden');
  } else {
    appealFormCard.classList.add('hidden');
    appealReadonlyCard.classList.remove('hidden');
    appealReadonlyText.textContent = status === 'PENDING'
      ? 'This conversation becomes read-only while the resource is under review.'
      : 'This resource is not currently open for new conversation messages.';
  }

  appealInput.disabled = readOnlyMode || !canSendAppeal;
  sendAppealBtn.disabled = readOnlyMode || !canSendAppeal;
}

function validateUploadFile(file) {
  const maxSize = 10 * 1024 * 1024;
  const allowedExtensions = ['jpg', 'jpeg', 'png', 'pdf', 'mp3', 'wav'];
  const name = file.name.toLowerCase();
  const extension = name.includes('.') ? name.split('.').pop() : '';

  if (file.size > maxSize) {
    return 'Files larger than 10MB are not allowed.';
  }
  if (!allowedExtensions.includes(extension)) {
    return 'Unsupported file type. Only JPG, PNG, PDF, MP3, and WAV are allowed.';
  }
  return '';
}

async function ensureDraftForUpload() {
  if (resourceIdInput.value) {
    return Number(resourceIdInput.value);
  }
  const result = await saveDraft({ showToastMessage: false });
  if (!result?.id) {
    throw new Error('Please save a valid draft before uploading attachments.');
  }
  return Number(result.id);
}

function uploadSingleFile(draftId, file) {
  return new Promise((resolve, reject) => {
    const formData = new FormData();
    formData.append('file', file);

    const xhr = new XMLHttpRequest();
    xhr.open('POST', `/api/resources/draft/${draftId}/attachments`, true);
    xhr.withCredentials = true;

    const item = {
      tempId: `${Date.now()}-${Math.random()}`,
      name: file.name,
      type: 'document',
      url: '',
      progress: 0
    };
    attachments.push(item);
    renderAttachments();

    xhr.upload.addEventListener('progress', event => {
      if (!event.lengthComputable) {
        return;
      }
      item.progress = Math.round((event.loaded / event.total) * 100);
      renderAttachments();
    });

    xhr.addEventListener('load', () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        const data = JSON.parse(xhr.responseText);
        const index = attachments.findIndex(entry => entry.tempId === item.tempId);
        if (index >= 0) {
          attachments[index] = { ...data, progress: 100 };
        }
        updateHiddenValues();
        renderAttachments();
        resolve(data);
        return;
      }

      let message = 'Upload failed.';
      try {
        message = JSON.parse(xhr.responseText)?.message || message;
      } catch (error) {
        // ignore parse issues
      }
      const index = attachments.findIndex(entry => entry.tempId === item.tempId);
      if (index >= 0) {
        attachments.splice(index, 1);
      }
      renderAttachments();
      reject(new Error(message));
    });

    xhr.addEventListener('error', () => {
      const index = attachments.findIndex(entry => entry.tempId === item.tempId);
      if (index >= 0) {
        attachments.splice(index, 1);
      }
      renderAttachments();
      reject(new Error('Upload failed.'));
    });

    xhr.send(formData);
  });
}

async function handleFiles(fileList) {
  if (readOnlyMode) {
    return;
  }
  const files = Array.from(fileList);
  if (!files.length) {
    return;
  }

  setUploadError('');

  for (const file of files) {
    const validationError = validateUploadFile(file);
    if (validationError) {
      setUploadError(validationError);
      continue;
    }

    try {
      const draftId = await ensureDraftForUpload();
      await uploadSingleFile(draftId, file);
      setSaveState(`Last saved at ${formatSavedTime()}`);
    } catch (error) {
      setUploadError(error.message || 'Upload failed.');
    }
  }
}

function collectFormValues() {
  updateHiddenValues();
  return {
    id: resourceIdInput.value ? Number(resourceIdInput.value) : null,
    title: document.getElementById('title').value.trim(),
    titleEn: null,
    category: document.getElementById('category').value,
    period: document.getElementById('period').value.trim(),
    place: document.getElementById('place').value.trim(),
    tags: hiddenTags.value,
    description: document.getElementById('description').value.trim(),
    thumbnail: document.getElementById('thumbnail').value.trim(),
    copyright: document.getElementById('copyright').value.trim()
  };
}

function validateDraftPayload(payload) {
  if (!payload.title) {
    return 'Title is required.';
  }
  if (!payload.description) {
    return 'Description is required.';
  }
  return '';
}

function validateSubmitPayload(payload) {
  const draftError = validateDraftPayload(payload);
  if (draftError) {
    return draftError;
  }
  if (!payload.category) {
    return 'Category is required.';
  }
  if (!payload.place) {
    return 'Place is required.';
  }
  return '';
}

async function requestJson(url, payload, options = {}) {
  const method = options.method || 'POST';
  const headers = { ...(options.headers || {}) };
  const fetchOptions = {
    method,
    credentials: 'same-origin',
    headers
  };

  if (payload !== undefined && payload !== null) {
    if (!headers['Content-Type'] && !(payload instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
    }
    fetchOptions.body = payload instanceof FormData ? payload : JSON.stringify(payload);
  }

  const response = await fetch(url, {
    ...fetchOptions
  });

  const contentType = response.headers.get('content-type') || '';
  const data = contentType.includes('application/json') ? await response.json() : null;

  if (!response.ok) {
    throw new Error(data?.message || 'Request failed.');
  }

  return data;
}

function applySavedDraft(data) {
  resourceIdInput.value = data.id ?? '';
  document.getElementById('title').value = data.title || '';
  document.getElementById('description').value = data.description || '';
  document.getElementById('category').value = data.category || '';
  document.getElementById('period').value = data.period || '';
  document.getElementById('place').value = data.place || '';
  document.getElementById('thumbnail').value = data.thumbnail || '';
  document.getElementById('copyright').value = data.copyright || '';
  tags.length = 0;
  (data.tags || []).forEach(tag => tags.push(tag));
  renderAllTags();
  syncAttachments(data.attachments || []);
  renderRevisionContext(data);
  setReadOnlyMode(data.status === 'PENDING', { trackingId: data.trackingId || '' });
}

function persistDraftIdentity(id) {
  if (!id) {
    return;
  }
  localStorage.setItem(ACTIVE_DRAFT_STORAGE_KEY, String(id));
  const url = new URL(window.location.href);
  url.searchParams.set('draftId', String(id));
  window.history.replaceState({}, '', url);
}

async function loadDraft(id) {
  const data = await requestJson(`/api/resources/draft/${id}`, null, { method: 'GET' });
  applySavedDraft(data);
  persistDraftIdentity(data.id);
  setSaveState(data.status === 'PENDING' ? 'Pending Review' : `Last saved at ${formatSavedTime(data.savedAt)}`);
  hideSaveError();
}

async function saveDraft({ showToastMessage = true } = {}) {
  if (readOnlyMode) {
    return null;
  }
  const payload = collectFormValues();
  const validationError = validateDraftPayload(payload);

  if (validationError) {
    if (showToastMessage) {
      setSubmitMessage(validationError, true);
    }
    setSaveState('Unsaved changes');
    return null;
  }

  hideSaveError();
  saveDraftBtn.disabled = true;
  saveInFlight = true;
  setSubmitMessage('');
  setSaveState('Saving draft...');

  try {
    const result = await requestJson('/api/resources/draft', payload);
    resourceIdInput.value = result?.id ?? '';
    persistDraftIdentity(result?.id);
    if (showToastMessage) {
      showToast(result?.message || 'Draft created successfully.');
      setSubmitMessage(
        result?.draftId
          ? `${result.message || 'Draft created successfully.'} Draft ID: ${result.draftId}`
          : (result?.message || 'Draft created successfully.')
      );
    }
    setSaveState(`Last saved at ${formatSavedTime(result?.savedAt)}`);
    return result;
  } catch (error) {
    if (showToastMessage) {
      setSubmitMessage(error.message || 'Failed to save draft.', true);
    }
    showSaveError(error.message || 'Save failed, please retry.');
    setSaveState('Save failed');
    return null;
  } finally {
    saveDraftBtn.disabled = readOnlyMode;
    saveInFlight = false;
    if (pendingAutosave) {
      pendingAutosave = false;
      void saveDraft({ showToastMessage: false });
    }
  }
}

async function submitForReview() {
  if (readOnlyMode) {
    closeModal();
    return;
  }
  const payload = collectFormValues();
  const validationError = validateSubmitPayload(payload);
  if (validationError) {
    setSubmitMessage(validationError, true);
    setSaveState('Validation required');
    closeModal();
    return;
  }

  submitBtn.disabled = true;
  confirmSubmitBtn.disabled = true;
  setSubmitMessage('');
  setSaveState('Submitting...');

  try {
    const result = await requestJson('/api/resources/submit', payload);
    showToast(result?.message || 'Resource submitted for review.');
    closeModal();
    resourceIdInput.value = result?.id ?? resourceIdInput.value;
    persistDraftIdentity(result?.id);
    setReadOnlyMode(true, { trackingId: result?.trackingId || '' });
    hideSaveError();
    window.location.href = `/submit-confirmation.html?id=${encodeURIComponent(result?.id ?? '')}&trackingId=${encodeURIComponent(result?.trackingId ?? '')}`;
  } catch (error) {
    setSubmitMessage(error.message || 'Failed to submit resource.', true);
    setSaveState('Submission failed');
  } finally {
    submitBtn.disabled = readOnlyMode;
    confirmSubmitBtn.disabled = readOnlyMode;
  }
}

async function ensureUploadAccess() {
  try {
    const sessionUser = await getSessionUser();
    if (!sessionUser.canUpload) {
      setReadOnlyMode(true, {
        label: 'Upload unavailable',
        message: 'Your current role cannot submit new resources.',
        isError: true
      });
      return false;
    }
    return true;
  } catch (error) {
    if (error.status === 401) {
      window.location.href = '/login.html';
      return false;
    }
    setSubmitMessage(error.message || 'Failed to verify your session.', true);
    return false;
  }
}

form.addEventListener('input', () => {
  if (!autosaveEnabled || readOnlyMode) {
    return;
  }
  setSaveState('Unsaved changes');
  hideSaveError();

  window.clearTimeout(autosaveTimer);
  autosaveTimer = window.setTimeout(async () => {
    if (saveInFlight) {
      pendingAutosave = true;
      return;
    }
    await saveDraft({ showToastMessage: false });
  }, AUTOSAVE_DELAY_MS);
});

saveDraftBtn.addEventListener('click', saveDraft);
submitBtn.addEventListener('click', () => {
  if (readOnlyMode) {
    return;
  }
  openModal();
});
cancelModalBtn.addEventListener('click', closeModal);
confirmSubmitBtn.addEventListener('click', submitForReview);
retrySaveBtn.addEventListener('click', async () => {
  if (saveInFlight || readOnlyMode) {
    return;
  }
  await saveDraft({ showToastMessage: false });
});

sendAppealBtn.addEventListener('click', async () => {
  if (readOnlyMode) {
    return;
  }

  const resourceId = resourceIdInput.value ? Number(resourceIdInput.value) : null;
  const content = appealInput.value.trim();

  if (!resourceId) {
    setAppealStatus('Please save this draft before sending a message.', true);
    return;
  }

  if (!content) {
    setAppealStatus('Message content is required.', true);
    return;
  }

  sendAppealBtn.disabled = true;
  setAppealStatus('');

  try {
    const response = await submitResourceAppeal(resourceId, content);
    currentRevisionContext = {
      ...currentRevisionContext,
      appealMessages: response.appealMessages || currentRevisionContext.appealMessages
    };
    renderRevisionContext(currentRevisionContext);
    appealInput.value = '';
    setAppealStatus(response.message || 'Message sent to the admin review team.');
  } catch (error) {
    setAppealStatus(error.message || 'Failed to send message.', true);
  } finally {
    sendAppealBtn.disabled = readOnlyMode || !currentRevisionContext.canSendAppeal;
  }
});

logoutBtn.addEventListener('click', async () => {
  await logout();
  window.location.href = '/login.html';
});

dropzone.addEventListener('click', () => {
  if (readOnlyMode) {
    return;
  }
  fileInput.click();
});
dropzone.addEventListener('dragover', event => {
  if (readOnlyMode) {
    return;
  }
  event.preventDefault();
  dropzone.classList.add('dropzone-active');
});
dropzone.addEventListener('dragleave', () => {
  dropzone.classList.remove('dropzone-active');
});
dropzone.addEventListener('drop', event => {
  if (readOnlyMode) {
    return;
  }
  event.preventDefault();
  dropzone.classList.remove('dropzone-active');
  if (event.dataTransfer?.files?.length) {
    handleFiles(event.dataTransfer.files);
  }
});
fileInput.addEventListener('change', event => {
  if (readOnlyMode) {
    return;
  }
  if (event.target.files?.length) {
    handleFiles(event.target.files);
    fileInput.value = '';
  }
});

bindTagInput('tagInput', tags);
renderAllTags();
renderAttachments();
hideRevisionContext();

window.addEventListener('beforeunload', () => {
  window.clearTimeout(autosaveTimer);
});

(async function initDraftPage() {
  const allowed = await ensureUploadAccess();
  if (!allowed) {
    return;
  }

  const url = new URL(window.location.href);
  const draftId = url.searchParams.get('draftId') || localStorage.getItem(ACTIVE_DRAFT_STORAGE_KEY);
  if (!draftId) {
    return;
  }

  autosaveEnabled = false;
  try {
    await loadDraft(draftId);
    if (!readOnlyMode) {
      setSubmitMessage('Loaded the latest saved draft.');
    }
  } catch (error) {
    localStorage.removeItem(ACTIVE_DRAFT_STORAGE_KEY);
    setSubmitMessage(error.message || 'Failed to load draft.', true);
  } finally {
    autosaveEnabled = true;
  }
})();
