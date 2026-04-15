import {
    approveResourceReview,
    getResourceReviewDetail,
    rejectResourceReview
} from "/admin/js/api.js";

const params = new URLSearchParams(window.location.search);
const resourceId = params.get("id");

const titleElement = document.getElementById("title");
const subtitleElement = document.getElementById("subtitle");
const placeElement = document.getElementById("place");
const submissionDateElement = document.getElementById("submissionDate");
const contributorElement = document.getElementById("contributor");
const statusBadgeElement = document.getElementById("statusBadge");
const descriptionElement = document.getElementById("description");
const descriptionToggle = document.getElementById("descriptionToggle");
const categoryElement = document.getElementById("category");
const tagsElement = document.getElementById("tags");
const resourceImage = document.getElementById("resourceImage");
const fileLink = document.getElementById("fileLink");
const fileDetails = document.getElementById("fileDetails");
const externalLink = document.getElementById("externalLink");
const externalDetails = document.getElementById("externalDetails");
const copyrightDeclaration = document.getElementById("copyrightDeclaration");
const submissionMetadata = document.getElementById("submissionMetadata");
const visibilityPill = document.getElementById("visibilityPill");
const rejectionCommentsInput = document.getElementById("rejectionComments");
const approveButton = document.getElementById("approveButton");
const rejectButton = document.getElementById("rejectButton");
const feedbackMessage = document.getElementById("feedbackMessage");
const existingRejectionBlock = document.getElementById("existingRejectionBlock");
const existingRejectionComments = document.getElementById("existingRejectionComments");

function formatDate(value) {
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

function renderTags(tags) {
    if (!tags.length) {
        tagsElement.innerHTML = `<span class="tag muted">No tags supplied</span>`;
        return;
    }

    tagsElement.innerHTML = tags.map((tag) => `<span class="tag">${tag}</span>`).join("");
}

function renderLink(element, href, activeText, inactiveText) {
    if (href) {
        element.href = href;
        element.textContent = activeText;
        element.classList.remove("disabled");
        return;
    }

    element.href = "#";
    element.textContent = inactiveText;
    element.classList.add("disabled");
}

function updateDescriptionClamp() {
    descriptionElement.classList.add("collapsed");
    descriptionToggle.hidden = true;

    window.requestAnimationFrame(() => {
        const hasOverflow = descriptionElement.scrollHeight - descriptionElement.clientHeight > 6;
        descriptionToggle.hidden = !hasOverflow;
        descriptionToggle.textContent = "Read more";
    });
}

function renderDetail(detail) {
    titleElement.textContent = detail.title;
    subtitleElement.textContent = detail.subtitle || "Heritage Resource Overview";
    placeElement.textContent = detail.place || "Place not provided";
    submissionDateElement.textContent = detail.submissionDate ? formatDate(detail.submissionDate) : "Date not provided";
    contributorElement.textContent = detail.contributor || "Unknown contributor";
    statusBadgeElement.textContent = detail.status.replaceAll("_", " ");
    statusBadgeElement.className = `status-badge ${detail.status.toLowerCase()}`;
    descriptionElement.textContent = detail.description || "No introduction was provided with this submission.";
    categoryElement.textContent = detail.category || "Category not provided";
    renderTags(detail.tags || []);
    updateDescriptionClamp();
    resourceImage.src = detail.imageUrl || "/review/images/resource-placeholder.svg";
    resourceImage.alt = detail.title;
    renderLink(fileLink, detail.fileLink, "Open attached file", "No attached file provided");
    renderLink(externalLink, detail.externalLink, "Visit external reference", "No external reference provided");
    fileDetails.textContent = detail.fileLink || "No file details are available for this submission.";
    externalDetails.textContent = detail.externalLink || "No external link details are available for this submission.";
    copyrightDeclaration.textContent = detail.copyrightDeclaration || "No rights declaration was supplied with this submission.";
    submissionMetadata.textContent = detail.submissionMetadata || "No submission metadata available.";
    rejectionCommentsInput.value = detail.rejectionComments || "";
    visibilityPill.textContent = detail.visible ? "Visibility: public" : "Visibility: hidden";

    const reviewable = detail.status === "PENDING_REVIEW";
    approveButton.disabled = !reviewable;
    rejectButton.disabled = !reviewable;
    rejectionCommentsInput.disabled = !reviewable;

    if (detail.rejectionComments) {
        existingRejectionBlock.hidden = false;
        existingRejectionComments.textContent = detail.rejectionComments;
    } else {
        existingRejectionBlock.hidden = true;
    }
}

async function loadDetail() {
    try {
        const detail = await getResourceReviewDetail(resourceId);
        renderDetail(detail);
        setFeedback("");
    } catch (error) {
        setFeedback(error.message || "Resource not found.", "error");
        approveButton.disabled = true;
        rejectButton.disabled = true;
    }
}

approveButton.addEventListener("click", async () => {
    try {
        const response = await approveResourceReview(resourceId);
        renderDetail(response.resource);
        setFeedback(response.message, "success");
    } catch (error) {
        setFeedback(error.message || "Failed to approve resource.", "error");
    }
});

rejectButton.addEventListener("click", async () => {
    if (!rejectionCommentsInput.value.trim()) {
        setFeedback("Rejection comments are required before rejecting a resource.", "error");
        return;
    }

    try {
        const response = await rejectResourceReview(resourceId, rejectionCommentsInput.value);
        renderDetail(response.resource);
        setFeedback(response.message, "success");
    } catch (error) {
        setFeedback(error.message || "Failed to reject resource.", "error");
    }
});

descriptionToggle.addEventListener("click", () => {
    const collapsed = descriptionElement.classList.contains("collapsed");
    descriptionElement.classList.toggle("collapsed", !collapsed);
    descriptionToggle.textContent = collapsed ? "Show less" : "Read more";
});

window.addEventListener("resize", updateDescriptionClamp);

loadDetail();
