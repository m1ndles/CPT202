import { cancelRevisionDraft, createRevisionDraft, getSessionUser, logout } from './heritage-data.js';

const form = document.getElementById('resourceForm');
const saveText = document.getElementById('saveText');
const submitMessage = document.getElementById('submitMessage');
const saveError = document.getElementById('saveError');
const saveErrorText = document.getElementById('saveErrorText');
const retrySaveBtn = document.getElementById('retrySaveBtn');
const resourceIdInput = document.getElementById('resourceId');
const saveDraftBtn = document.getElementById('saveDraftBtn');
const submitBtn = document.getElementById('submitBtn');
const cancelRevisionBtn = document.getElementById('cancelRevisionBtn');
const pageTitle = document.getElementById('pageTitle');
const pageSubtitle = document.getElementById('pageSubtitle');
const footerNote = document.getElementById('footerNote');
const topBackLink = document.getElementById('topBackLink');
const topBackLabel = document.getElementById('topBackLabel');
const myResourcesBackLink = document.getElementById('myResourcesBackLink');
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
const attachments = [];
const tags = [];
const AUTOSAVE_DELAY_MS = 3000;
const UNSAVED_RESOURCE_STORAGE_KEY = 'resource-submission-unsaved';

let autosaveTimer = null;
let autosaveEnabled = true;
let readOnlyMode = false;
let saveInFlight = false;
const pageUrl = new URL(window.location.href);
const revisionSourceStatuses = new Set(['APPROVED', 'PENDING', 'REJECTED']);
let sourceStatus = String(pageUrl.searchParams.get('sourceStatus') || '').toUpperCase();
const openedFromReedit = pageUrl.searchParams.get('edit') === '1' || revisionSourceStatuses.has(sourceStatus);
let revisionMode = false;
let originalRevisionSnapshot = null;

function refreshPageCopy() {
  if (!pageTitle || !pageSubtitle || !footerNote) {
    return;
  }

  if (revisionMode) {
    pageTitle.textContent = 'Re-edit Heritage Resource';
    pageSubtitle.textContent = 'Update the existing resource and submit the revised version back into the review queue.';
    footerNote.innerHTML = '<span class="font-medium text-primary-600">Note:</span> when you submit these changes, the updated resource returns to the pending review queue.';
    saveDraftBtn.textContent = 'Save Revision Draft';
    submitBtn.textContent = 'Resubmit for Review';
    confirmSubmitBtn.textContent = 'Confirm Resubmission';
    cancelRevisionBtn?.classList.remove('hidden');
    if (topBackLink) topBackLink.href = '/my-resources.html';
    if (topBackLabel) topBackLabel.textContent = 'Back to My Resources';
    myResourcesBackLink?.classList.remove('hidden');
    return;
  }

  pageTitle.textContent = 'Submit Heritage Resource';
  pageSubtitle.textContent = 'Start a new draft or submit a heritage resource for review.';
  footerNote.innerHTML = '<span class="font-medium text-primary-600">Note:</span> submitted resources enter the review workflow and cannot be edited during review.';
  saveDraftBtn.textContent = 'Save Draft';
  submitBtn.textContent = 'Submit for Review';
  confirmSubmitBtn.textContent = 'Confirm Submit';
  cancelRevisionBtn?.classList.add('hidden');
  if (topBackLink) topBackLink.href = '/index.html';
  if (topBackLabel) topBackLabel.textContent = 'Back to Jiangsu Heritage Discovery';
  myResourcesBackLink?.classList.add('hidden');
}

function setSaveState(text) {
  saveText.textContent = text;
}

function isNewDraftFlow() {
  return !resourceIdInput.value;
}

function buildUnsavedSnapshot() {
  return {
    title: document.getElementById('title').value.trim(),
    description: document.getElementById('description').value.trim(),
    category: document.getElementById('category').value,
    period: document.getElementById('period').value.trim(),
    place: document.getElementById('place').value.trim(),
    thumbnail: document.getElementById('thumbnail').value.trim(),
    copyright: document.getElementById('copyright').value.trim(),
    tags: [...tags]
  };
}

function buildRevisionSnapshot(data) {
  return {
    restoreStatus: sourceStatus,
    title: data.title || '',
    titleEn: data.titleEn || '',
    category: data.category || '',
    period: data.period || '',
    place: data.place || '',
    tags: Array.isArray(data.tags) ? data.tags.join(',') : (data.tags || ''),
    description: data.description || '',
    thumbnail: data.thumbnail || '',
    copyright: data.copyright || ''
  };
}

function viewForStatus(status) {
  const normalized = String(status || '').toUpperCase();
  if (normalized === 'APPROVED') return 'APPROVED';
  if (normalized === 'PENDING') return 'PENDING';
  if (normalized === 'REJECTED') return 'REJECTED';
  if (normalized === 'DRAFT') return 'DRAFT';
  return 'ALL';
}

function goToMyResources(status = sourceStatus) {
  window.location.href = `/my-resources.html?view=${encodeURIComponent(viewForStatus(status))}`;
}

function hasUnsavedSnapshotContent(snapshot) {
  if (!snapshot) return false;
  return Boolean(
    snapshot.title ||
    snapshot.description ||
    snapshot.category ||
    snapshot.period ||
    snapshot.place ||
    snapshot.thumbnail ||
    snapshot.copyright ||
    (Array.isArray(snapshot.tags) && snapshot.tags.length)
  );
}

function persistUnsavedSnapshot() {
  if (!isNewDraftFlow() || readOnlyMode) {
    return;
  }
  const snapshot = buildUnsavedSnapshot();
  if (!hasUnsavedSnapshotContent(snapshot)) {
    localStorage.removeItem(UNSAVED_RESOURCE_STORAGE_KEY);
    return;
  }
  localStorage.setItem(UNSAVED_RESOURCE_STORAGE_KEY, JSON.stringify(snapshot));
}

function clearUnsavedSnapshot() {
  localStorage.removeItem(UNSAVED_RESOURCE_STORAGE_KEY);
}

function syncDraftUrl(id = '') {
  const url = new URL(window.location.href);
  if (id) {
    url.searchParams.set('draftId', String(id));
  } else {
    url.searchParams.delete('draftId');
  }
  window.history.replaceState({}, '', url);
}

function applyUnsavedSnapshot(snapshot) {
  document.getElementById('title').value = snapshot.title || '';
  document.getElementById('description').value = snapshot.description || '';
  document.getElementById('category').value = snapshot.category || '';
  document.getElementById('period').value = snapshot.period || '';
  document.getElementById('place').value = snapshot.place || '';
  document.getElementById('thumbnail').value = snapshot.thumbnail || '';
  document.getElementById('copyright').value = snapshot.copyright || '';
  tags.length = 0;
  (snapshot.tags || []).forEach(tag => tags.push(tag));
  renderAllTags();
  updateHiddenValues();
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

  const normalizedMessage = String(message);
  if (normalizedMessage.includes('50 saved drafts')) {
    message = 'Draft box is full. You can keep up to 50 saved drafts. Please delete an older draft in My Resources before creating a new one.';
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
      persistUnsavedSnapshot();
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
    persistUnsavedSnapshot();
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
        persistUnsavedSnapshot();
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
        persistUnsavedSnapshot();
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
  if (!sourceStatus) {
    sourceStatus = String(data.status || '').toUpperCase();
  }
  revisionMode = openedFromReedit || revisionSourceStatuses.has(sourceStatus);
  if (revisionMode && !originalRevisionSnapshot && revisionSourceStatuses.has(sourceStatus)) {
    originalRevisionSnapshot = buildRevisionSnapshot(data);
  }
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
  clearUnsavedSnapshot();
  syncDraftUrl(data.id);
  refreshPageCopy();
  if (revisionMode) {
    setReadOnlyMode(false);
    return;
  }
  setReadOnlyMode(data.status === 'PENDING', { trackingId: data.trackingId || '' });
}

async function loadDraft(id) {
  const data = await requestJson(`/api/resources/draft/${id}`, null, { method: 'GET' });
  applySavedDraft(data);
  setSaveState(data.status === 'PENDING' ? 'Pending Review' : `Last saved at ${formatSavedTime(data.savedAt)}`);
  hideSaveError();
}

async function ensureRevisionDraft(id) {
  if (!openedFromReedit || !id) {
    return id;
  }
  if (sourceStatus !== 'APPROVED' && sourceStatus !== 'PENDING') {
    return id;
  }
  const response = await createRevisionDraft(id);
  if (!response?.id) {
    throw new Error('Failed to prepare revision draft.');
  }
  return response.id;
}

async function abandonRevision() {
  if (!revisionMode) {
    goToMyResources();
    return;
  }

  const confirmed = window.confirm('Abandon this re-edit and return to My Resources? The resource will keep its previous status.');
  if (!confirmed) {
    return;
  }

  const currentId = resourceIdInput.value;
  if (!currentId || !originalRevisionSnapshot || !['APPROVED', 'PENDING'].includes(sourceStatus)) {
    goToMyResources(sourceStatus);
    return;
  }

  cancelRevisionBtn.disabled = true;
  saveDraftBtn.disabled = true;
  submitBtn.disabled = true;
  setSaveState('Restoring original resource...');
  setSubmitMessage('');

  try {
    await cancelRevisionDraft(currentId, originalRevisionSnapshot);
    showToast(sourceStatus === 'APPROVED' ? 'Re-edit abandoned. Resource remains published.' : 'Re-edit abandoned. Resource remains pending.');
    goToMyResources(sourceStatus);
  } catch (error) {
    setSubmitMessage(error.message || 'Failed to abandon re-edit. Please try again.', true);
    setSaveState('Restore failed');
    cancelRevisionBtn.disabled = false;
    saveDraftBtn.disabled = false;
    submitBtn.disabled = false;
  }
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
  saveInFlight = true;
  saveDraftBtn.disabled = true;
  setSubmitMessage('');
  setSaveState('Saving draft...');

  try {
    const result = await requestJson('/api/resources/draft', payload);
    resourceIdInput.value = result?.id ?? '';
    clearUnsavedSnapshot();
    syncDraftUrl(result?.id);
    if (showToastMessage) {
      const successMessage = revisionMode
        ? (sourceStatus === 'PENDING' || sourceStatus === 'APPROVED'
          ? 'Revision draft saved. Submit it when you are ready to send the updated version back for review.'
          : (result?.message || 'Draft saved to My Resources.'))
        : (result?.message || 'Draft saved to My Resources.');
      showToast(result?.message || 'Draft saved to My Resources.');
      setSubmitMessage(
        result?.draftId
          ? `${successMessage} Draft ID: ${result.draftId}`
          : successMessage
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
    saveInFlight = false;
    saveDraftBtn.disabled = readOnlyMode;
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
    showToast(revisionMode ? 'Updated resource sent back to review.' : (result?.message || 'Resource submitted for review.'));
    closeModal();
    resourceIdInput.value = result?.id ?? resourceIdInput.value;
    clearUnsavedSnapshot();
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
  autosaveTimer = window.setTimeout(() => {
    persistUnsavedSnapshot();
    if (isNewDraftFlow()) {
      setSaveState('Unsaved edits backed up locally');
    }
  }, AUTOSAVE_DELAY_MS);
});

saveDraftBtn.addEventListener('click', saveDraft);
cancelRevisionBtn?.addEventListener('click', abandonRevision);
submitBtn.addEventListener('click', () => {
  if (readOnlyMode) {
    return;
  }
  openModal();
});
topBackLink?.addEventListener('click', event => {
  if (!revisionMode || !['APPROVED', 'PENDING'].includes(sourceStatus)) {
    return;
  }
  event.preventDefault();
  abandonRevision();
});
myResourcesBackLink?.addEventListener('click', event => {
  if (!revisionMode || !['APPROVED', 'PENDING'].includes(sourceStatus)) {
    return;
  }
  event.preventDefault();
  abandonRevision();
});
cancelModalBtn.addEventListener('click', closeModal);
confirmSubmitBtn.addEventListener('click', submitForReview);
retrySaveBtn.addEventListener('click', async () => {
  if (saveInFlight || readOnlyMode) {
    return;
  }
  await saveDraft({ showToastMessage: false });
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

window.addEventListener('beforeunload', () => {
  window.clearTimeout(autosaveTimer);
});

(async function initDraftPage() {
  const allowed = await ensureUploadAccess();
  if (!allowed) {
    return;
  }

  const url = new URL(window.location.href);
  const draftId = url.searchParams.get('draftId');
  refreshPageCopy();
  if (draftId) {
    autosaveEnabled = false;
    try {
      const resolvedDraftId = await ensureRevisionDraft(draftId);
      await loadDraft(resolvedDraftId);
      if (!readOnlyMode) {
        setSubmitMessage(
          revisionMode
            ? 'Loaded the selected resource for revision. Submit the updated version to place it back in the review queue.'
            : 'Loaded the selected draft.'
        );
      }
    } catch (error) {
      setSubmitMessage(error.message || 'Failed to load draft.', true);
    } finally {
      autosaveEnabled = true;
    }
    return;
  }

  try {
    const rawSnapshot = localStorage.getItem(UNSAVED_RESOURCE_STORAGE_KEY);
    const snapshot = rawSnapshot ? JSON.parse(rawSnapshot) : null;
    if (hasUnsavedSnapshotContent(snapshot)) {
      applyUnsavedSnapshot(snapshot);
      setSubmitMessage('Recovered unsaved local edits from this browser. Use Save Draft if you want to add this resource to My Resources.');
      setSaveState('Recovered unsaved local edits');
    }
  } catch (error) {
    clearUnsavedSnapshot();
  }

  autosaveEnabled = true;
})();
