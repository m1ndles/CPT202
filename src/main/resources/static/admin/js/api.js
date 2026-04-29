async function request(url, options = {}) {
    const hasFormData = options.body instanceof FormData;
    const response = await fetch(url, {
        credentials: "same-origin",
        ...options,
        headers: {
            ...(options.body && !hasFormData ? { "Content-Type": "application/json" } : {}),
            ...(options.headers || {})
        }
    });

    const contentType = response.headers.get("content-type") || "";
    const payload = contentType.includes("application/json") ? await response.json() : null;

    if (!response.ok) {
        const error = new Error(payload?.message || "Request failed.");
        error.status = response.status;
        throw error;
    }

    return payload;
}

function matchesSearch(item, search, fields) {
    if (!search) {
        return true;
    }
    const normalized = search.toLowerCase();
    return fields.some((field) => String(item[field] || "").toLowerCase().includes(normalized));
}

function sortByDate(items, key, direction) {
    const copy = [...items];
    copy.sort((left, right) => {
        const leftDate = new Date(String(left[key] || "").replace(" ", "T")).getTime();
        const rightDate = new Date(String(right[key] || "").replace(" ", "T")).getTime();
        return direction === "asc" ? leftDate - rightDate : rightDate - leftDate;
    });
    return copy;
}

export async function getDashboardSummary() {
    return await request("/api/admin/dashboard");
}

export async function getContributorApprovalList({ search = "", expertise = "All" } = {}) {
    const applications = await request("/api/contributor-applications/admin/pending");
    const pending = applications
        .filter((item) => item.status === "PENDING")
        .filter((item) => expertise === "All" || item.expertiseField === expertise)
        .filter((item) => matchesSearch(item, search, ["username", "fullName", "expertiseField"]));

    return {
        pendingCount: applications.filter((item) => item.status === "PENDING").length,
        expertiseFields: ["All", ...new Set(applications
            .filter((item) => item.status === "PENDING")
            .map((item) => item.expertiseField))],
        items: sortByDate(pending, "submittedAt", "desc")
    };
}

export async function getContributorApprovalDetail(id) {
    return await request(`/api/contributor-applications/admin/${id}`);
}

export async function approveContributorApplication(id) {
    return await request(`/api/contributor-applications/admin/${id}/approve`, {
        method: "POST"
    });
}

export async function rejectContributorApplication(id, rejectionComments) {
    const params = new URLSearchParams({ comments: rejectionComments.trim() });
    return await request(`/api/contributor-applications/admin/${id}/reject?${params.toString()}`, {
        method: "POST"
    });
}

export async function sendContributorAppealReply(id, content) {
    return await request(`/api/contributor-applications/admin/${id}/appeals`, {
        method: "POST",
        body: JSON.stringify({ content })
    });
}

export async function getResourceReviewList({ search = "", category = "All", status = "All", sort = "desc" } = {}) {
    const resources = await request("/api/admin/resources/reviews");
    const statusFiltered = resources
        .filter((item) => status === "All" || item.status === status);
    const categories = ["All", ...new Set(statusFiltered.map((item) => item.category).filter(Boolean))];
    const activeCategory = categories.includes(category) ? category : "All";
    const items = statusFiltered
        .filter((item) => activeCategory === "All" || item.category === activeCategory)
        .filter((item) => matchesSearch(item, search, ["title", "contributor", "category", "place"]));

    return {
        totalCount: statusFiltered.length,
        displayCount: items.length,
        categories,
        activeCategory,
        items: sortByDate(items, "submissionDate", sort)
    };
}

export async function getResourceReviewDetail(id) {
    return await request(`/api/admin/resources/reviews/${id}`);
}

export async function approveResourceReview(id) {
    const response = await request(`/api/admin/resources/reviews/${id}/approve`, {
        method: "POST"
    });
    const resource = await getResourceReviewDetail(id);
    return { ...response, resource };
}

export async function rejectResourceReview(id, rejectionComments) {
    const response = await request(`/api/admin/resources/reviews/${id}/reject`, {
        method: "POST",
        body: JSON.stringify({ rejectionComments })
    });
    const resource = await getResourceReviewDetail(id);
    return { ...response, resource };
}

export async function archiveResourceReview(id, archiveReason) {
    const response = await request(`/api/admin/resources/reviews/${id}/archive`, {
        method: "POST",
        body: JSON.stringify({ rejectionComments: archiveReason })
    });
    const resource = await getResourceReviewDetail(id);
    return { ...response, resource };
}

export async function sendResourceReviewReply(id, content) {
    return await request(`/api/admin/resources/reviews/${id}/appeals`, {
        method: "POST",
        body: JSON.stringify({ content })
    });
}

export async function getCategoryList({ search = "", status = "All" } = {}) {
    const categories = await request("/api/admin/categories");
    const items = categories
        .filter((item) => status === "All" || item.status === status)
        .filter((item) => matchesSearch(item, search, ["name", "description"]));

    return {
        totalActive: categories.filter((item) => item.status === "ACTIVE").length,
        items: sortByDate(items, "updatedAt", "desc")
    };
}

export async function createCategory({ name, description }) {
    return await request("/api/admin/categories", {
        method: "POST",
        body: JSON.stringify({ name, description })
    });
}

export async function updateCategory(id, updates) {
    return await request(`/api/admin/categories/${id}`, {
        method: "PUT",
        body: JSON.stringify(updates)
    });
}

export async function toggleCategoryStatus(id) {
    return await request(`/api/admin/categories/${id}/toggle-status`, {
        method: "POST"
    });
}

export async function getTagList({ search = "", status = "All" } = {}) {
    const tags = await request("/api/admin/tags");
    const items = tags
        .filter((item) => status === "All" || item.status === status)
        .filter((item) => matchesSearch(item, search, ["name", "description"]));

    return {
        totalActive: tags.filter((item) => item.status === "ACTIVE").length,
        items: sortByDate(items, "updatedAt", "desc")
    };
}

export async function createTag({ name, description }) {
    return await request("/api/admin/tags", {
        method: "POST",
        body: JSON.stringify({ name, description })
    });
}

export async function updateTag(id, updates) {
    return await request(`/api/admin/tags/${id}`, {
        method: "PUT",
        body: JSON.stringify(updates)
    });
}

export async function toggleTagStatus(id) {
    return await request(`/api/admin/tags/${id}/toggle-status`, {
        method: "POST"
    });
}

export async function getArchiveList({ search = "", category = "All", sort = "desc" } = {}) {
    const archives = await request("/api/admin/archives");
    const items = archives
        .filter((item) => category === "All" || item.category === category)
        .filter((item) => matchesSearch(item, search, ["title", "contributor", "category", "archiveReason"]));

    return {
        archivedCount: archives.length,
        categories: ["All", ...new Set(archives.map((item) => item.category).filter(Boolean))],
        items: sortByDate(items, "archivedAt", sort)
    };
}

export async function getArchiveDetail(id) {
    return await request(`/api/admin/archives/${id}`);
}

export async function restoreArchivedResource(id) {
    return await request(`/api/admin/archives/${id}/restore`, {
        method: "POST"
    });
}

export async function getHistoryLog({ search = "", actionType = "All", targetType = "All", operator = "All" } = {}) {
    const history = await request("/api/admin/history");
    const items = history
        .filter((item) => actionType === "All" || item.actionType === actionType)
        .filter((item) => targetType === "All" || item.targetType === targetType)
        .filter((item) => operator === "All" || item.operator === operator)
        .filter((item) => matchesSearch(item, search, ["actionType", "targetName", "details", "operator"]));

    return {
        actionTypes: ["All", ...new Set(history.map((item) => item.actionType))],
        targetTypes: ["All", ...new Set(history.map((item) => item.targetType))],
        operators: ["All", ...new Set(history.map((item) => item.operator))],
        items: sortByDate(items, "createdAt", "desc")
    };
}

export async function getComplaintInbox({ search = "", type = "All", status = "All" } = {}) {
    const complaints = await request("/api/admin/complaints");
    const items = complaints
        .filter((item) => type === "All" || item.complaintType === type)
        .filter((item) => status === "All" || item.status === status)
        .filter((item) => matchesSearch(item, search, ["title", "targetName", "createdBy", "latestMessagePreview"]));

    return {
        types: ["All", ...new Set(complaints.map((item) => item.complaintType))],
        statuses: ["All", ...new Set(complaints.map((item) => item.status))],
        items: sortByDate(items, "updatedAt", "desc")
    };
}

export async function getComplaintDetail(type, id) {
    return await request(`/api/admin/complaints/${type}/${id}`);
}

export async function replyToComplaint(type, id, content) {
    return await request(`/api/admin/complaints/${type}/${id}/reply`, {
        method: "POST",
        body: JSON.stringify({ content })
    });
}

export async function reopenReportedResourceComplaint(id) {
    return await request(`/api/admin/complaints/resource-report/${id}/reopen`, {
        method: "POST"
    });
}

export async function deleteReportedCommentComplaint(id) {
    return await request(`/api/admin/complaints/comment-report/${id}/delete-comment`, {
        method: "DELETE"
    });
}
