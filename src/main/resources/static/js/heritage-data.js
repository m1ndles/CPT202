const JSON_HEADERS = {
  'Content-Type': 'application/json'
};

async function request(url, options = {}) {
  const hasFormData = options.body instanceof FormData;
  const config = {
    credentials: 'same-origin',
    ...options,
    headers: {
      ...(options.body && !hasFormData ? JSON_HEADERS : {}),
      ...(options.headers || {})
    }
  };

  const response = await fetch(url, config);
  const contentType = response.headers.get('content-type') || '';
  const payload = contentType.includes('application/json') ? await response.json() : null;

  if (!response.ok) {
    const error = new Error(payload?.message || 'Request failed.');
    error.status = response.status;
    throw error;
  }

  return payload;
}

export function getSessionUser() {
  return request('/api/auth/me');
}

export function getProfile() {
  return request('/api/profile');
}

export function updateProfile(payload) {
  return request('/api/profile', {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export function uploadAvatar(file) {
  const formData = new FormData();
  formData.append('file', file);
  return request('/api/profile/avatar', {
    method: 'POST',
    body: formData
  });
}

export function updatePassword(payload) {
  return request('/api/profile/password', {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export function updateEmail(payload) {
  return request('/api/profile/email', {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export function getCurrentContributorApplication() {
  return request('/api/contributor-applications/current');
}

export function getMyContributorApplications() {
  return request('/api/contributor-applications/mine');
}

export function submitContributorApplication(formData) {
  return request('/api/contributor-applications', {
    method: 'POST',
    body: formData
  });
}

export function logout() {
  return request('/api/auth/logout', { method: 'POST' });
}

export function getResources(filters = {}) {
  const params = new URLSearchParams();
  if (filters.keyword) params.set('keyword', filters.keyword);
  if (filters.category) params.set('category', filters.category);
  if (filters.place) params.set('place', filters.place);
  if (filters.sort) params.set('sort', filters.sort);
  if (filters.page) params.set('page', String(filters.page));
  if (filters.size) params.set('size', String(filters.size));
  return request(`/api/resources?${params.toString()}`);
}

export function getMyResources(status = "") {
  const params = new URLSearchParams();
  if (status) params.set("status", status);
  const query = params.toString();
  return request(query ? `/api/resources/mine?${query}` : "/api/resources/mine");
}

export function getMyFavoriteResources() {
  return request("/api/resources/favorites");
}

export function deleteMyResource(resourceId) {
  return request(`/api/resources/${resourceId}`, {
    method: 'DELETE'
  });
}

export function getOwnedDraft(resourceId) {
  return request(`/api/resources/draft/${resourceId}`);
}

export function getResourceById(resourceId) {
  return request(`/api/resources/${resourceId}`);
}

export function toggleResourceFavorite(resourceId) {
  return request(`/api/resources/${resourceId}/favorite`, {
    method: 'POST'
  });
}

export function submitResourceAppeal(resourceId, content) {
  return request(`/api/resources/${resourceId}/appeals`, {
    method: 'POST',
    body: JSON.stringify({ content })
  });
}

export function getCategories() {
  return request('/api/resources/categories');
}

export function getPlaces() {
  return request('/api/resources/places');
}

export function incrementView(resourceId) {
  return request(`/api/resources/${resourceId}/view`, { method: 'POST' });
}

export function getComments(resourceId, page = 1, size = 10) {
  return request(`/api/resources/${resourceId}/comments?page=${page}&size=${size}`);
}

export function addComment(resourceId, content) {
  return request(`/api/resources/${resourceId}/comments`, {
    method: 'POST',
    body: JSON.stringify({ content })
  });
}

export function toggleLike(commentId) {
  return request(`/api/comments/${commentId}/like`, { method: 'POST' });
}
