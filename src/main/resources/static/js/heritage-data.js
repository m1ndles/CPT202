const JSON_HEADERS = {
  'Content-Type': 'application/json'
};

async function request(url, options = {}) {
  const config = {
    credentials: 'same-origin',
    ...options,
    headers: {
      ...(options.body ? JSON_HEADERS : {}),
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

export function getResourceById(resourceId) {
  return request(`/api/resources/${resourceId}`);
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
