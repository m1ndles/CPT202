const trackingId = document.getElementById('trackingId');
const resourceId = document.getElementById('resourceId');
const viewSubmissionLink = document.getElementById('viewSubmissionLink');

const url = new URL(window.location.href);
const trackingValue = url.searchParams.get('trackingId') || '--';
const resourceValue = url.searchParams.get('id') || '--';

trackingId.textContent = trackingValue;
resourceId.textContent = resourceValue;

if (resourceValue !== '--') {
  viewSubmissionLink.href = `/submit-resource.html?draftId=${encodeURIComponent(resourceValue)}`;
}
