import { getDashboardSummary } from "/admin/js/api.js";

const attentionCount = document.getElementById("attentionCount");
const summaryGrid = document.getElementById("summaryGrid");
const pendingApplications = document.getElementById("pendingApplications");
const latestApplicant = document.getElementById("latestApplicant");
const pendingResources = document.getElementById("pendingResources");
const latestResource = document.getElementById("latestResource");
const activeCategories = document.getElementById("activeCategories");
const activeTags = document.getElementById("activeTags");
const latestTaxonomy = document.getElementById("latestTaxonomy");
const archivedResources = document.getElementById("archivedResources");
const latestArchive = document.getElementById("latestArchive");
const historyPreview = document.getElementById("historyPreview");

const sectionBindings = {
    contribution: {
        title: document.getElementById("contributionTitle"),
        subtitle: document.getElementById("contributionSubtitle"),
        headlineLabel: document.getElementById("contributionHeadlineLabel"),
        headlineValue: document.getElementById("contributionHeadlineValue"),
        accentValue: document.getElementById("contributionAccentValue"),
        trendChart: document.getElementById("contributionTrendChart"),
        breakdownChart: document.getElementById("contributionBreakdownChart"),
        breakdownLegend: document.getElementById("contributionBreakdownLegend"),
        todoList: document.getElementById("contributionTodoList")
    },
    resource: {
        title: document.getElementById("resourceTitle"),
        subtitle: document.getElementById("resourceSubtitle"),
        headlineLabel: document.getElementById("resourceHeadlineLabel"),
        headlineValue: document.getElementById("resourceHeadlineValue"),
        accentValue: document.getElementById("resourceAccentValue"),
        trendChart: document.getElementById("resourceTrendChart"),
        breakdownChart: document.getElementById("resourceBreakdownChart"),
        breakdownLegend: document.getElementById("resourceBreakdownLegend"),
        todoList: document.getElementById("resourceTodoList")
    }
};

const CONTRIBUTION_BREAKDOWN_COLORS = ["#0f766e", "#38bdf8", "#f59e0b", "#f97316"];
const RESOURCE_BREAKDOWN_COLORS = ["#2563eb", "#06b6d4", "#14b8a6", "#f97316"];

function escapeHtml(value) {
    return String(value || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

function hasText(value) {
    return value !== null && value !== undefined && String(value).trim() !== "";
}

function asCount(value) {
    const numericValue = Number(value);
    if (Number.isFinite(numericValue)) {
        return numericValue;
    }
    const match = String(value ?? "").match(/-?\d+/);
    return match ? Number(match[0]) : 0;
}

function asText(value, fallback = "-") {
    return hasText(value) ? String(value).trim() : fallback;
}

function findMetricValue(metricCards, label) {
    const card = Array.isArray(metricCards)
        ? metricCards.find((item) => item && item.label === label)
        : null;
    return card ? asCount(card.value) : 0;
}

function normalizeMetricCards(metricCards, summary) {
    if (Array.isArray(metricCards) && metricCards.length) {
        return metricCards.map((card) => ({
            label: asText(card?.label, "Untitled metric"),
            value: asText(card?.value, "0"),
            tone: asText(card?.tone, "")
        }));
    }

    return [
        {
            label: "Pending Contributor Applications",
            value: String(asCount(summary.pendingContributorApplications)),
            tone: "priority"
        },
        {
            label: "Pending Resource Reviews",
            value: String(asCount(summary.pendingResourceReviews)),
            tone: "priority"
        },
        {
            label: "Archived Resources",
            value: String(asCount(summary.archivedResources)),
            tone: "neutral"
        },
        {
            label: "Active Categories / Tags",
            value: `${asCount(summary.activeCategories)} / ${asCount(summary.activeTags)}`,
            tone: "neutral"
        }
    ];
}

function normalizeTrend(points) {
    if (!Array.isArray(points)) {
        return [];
    }
    return points.map((point) => ({
        label: asText(point?.label, "Unknown"),
        value: asCount(point?.value)
    }));
}

function normalizeBreakdown(items, fallbackColors) {
    if (!Array.isArray(items) || !items.length) {
        return [];
    }
    return items.map((item, index) => ({
        label: asText(item?.label, "Unknown"),
        value: asCount(item?.value),
        color: hasText(item?.color) ? item.color : fallbackColors[index % fallbackColors.length]
    }));
}

function normalizeTodoItems(items, fallbackText) {
    if (!Array.isArray(items) || !items.length) {
        return [fallbackText];
    }
    return items.filter(hasText).map((item) => String(item).trim());
}

function buildLegacyContributionSection(summary, metricCards) {
    const pending = asCount(summary.pendingContributorApplications);
    const approved = findMetricValue(metricCards, "Approved Contributors");
    const rejected = findMetricValue(metricCards, "Rejected Applications");
    const total = approved + pending + rejected;
    const todoItems = [];

    if (pending > 0) {
        todoItems.push(`${pending} contributor applications are still waiting for review.`);
    }
    if (rejected > 0) {
        todoItems.push(`${rejected} applications were rejected and may need clearer feedback or follow-up.`);
    }
    if (!todoItems.length) {
        todoItems.push("Contributor approval data will appear here as new applications arrive.");
    }

    return {
        title: "Contribution",
        subtitle: "Application growth, field distribution, and review backlog",
        headlineValue: String(total),
        headlineLabel: "Total contributor applications",
        accentLabel: "Approved / Pending",
        accentValue: `${approved} / ${pending}`,
        trend: [],
        breakdown: [],
        todoItems
    };
}

function buildLegacyResourceSection(summary, metricCards) {
    const pending = asCount(summary.pendingResourceReviews);
    const approved = findMetricValue(metricCards, "Approved Resources");
    const rejected = findMetricValue(metricCards, "Rejected Resources");
    const total = approved + pending + rejected;
    const archived = asCount(summary.archivedResources);
    const todoItems = [];

    if (pending > 0) {
        todoItems.push(`${pending} resources are waiting for moderation review.`);
    }
    if (archived > 0) {
        todoItems.push(`${archived} archived resources may need restoration checks.`);
    }
    if (rejected > 0) {
        todoItems.push(`${rejected} resources were rejected and may need contributor revisions.`);
    }
    if (!todoItems.length) {
        todoItems.push("Resource review data will appear here as new submissions arrive.");
    }

    return {
        title: "Resource",
        subtitle: "Submission growth, category mix, and moderation workload",
        headlineValue: String(total),
        headlineLabel: "Total resource submissions",
        accentLabel: "Approved / Pending",
        accentValue: `${approved} / ${pending}`,
        trend: [],
        breakdown: [],
        todoItems
    };
}

function normalizeInsightSection(section, fallbackSection, breakdownColors) {
    const source = section && typeof section === "object" ? section : fallbackSection;
    return {
        title: asText(source?.title, fallbackSection.title),
        subtitle: asText(source?.subtitle, fallbackSection.subtitle),
        headlineValue: asText(source?.headlineValue, fallbackSection.headlineValue),
        headlineLabel: asText(source?.headlineLabel, fallbackSection.headlineLabel),
        accentLabel: asText(source?.accentLabel, fallbackSection.accentLabel),
        accentValue: asText(source?.accentValue, fallbackSection.accentValue),
        trend: normalizeTrend(source?.trend),
        breakdown: normalizeBreakdown(source?.breakdown, breakdownColors),
        todoItems: normalizeTodoItems(source?.todoItems, fallbackSection.todoItems[0])
    };
}

function normalizeHistoryItems(items) {
    if (!Array.isArray(items)) {
        return [];
    }
    return items.map((entry) => ({
        actionType: asText(entry?.actionType, "No activity"),
        targetName: asText(entry?.targetName, "Waiting for admin activity"),
        createdAt: entry?.createdAt || null
    }));
}

function normalizeSummary(summary) {
    const safeSummary = summary && typeof summary === "object" ? summary : {};
    const metricCards = normalizeMetricCards(safeSummary.metricCards, safeSummary);
    const contributionFallback = buildLegacyContributionSection(safeSummary, metricCards);
    const resourceFallback = buildLegacyResourceSection(safeSummary, metricCards);
    const archivedResourcesCount = asCount(safeSummary.archivedResources);
    const pendingContributorApplications = asCount(safeSummary.pendingContributorApplications);
    const pendingResourceReviews = asCount(safeSummary.pendingResourceReviews);

    return {
        attentionCount: asCount(
            safeSummary.attentionCount
                ?? pendingContributorApplications + pendingResourceReviews + archivedResourcesCount
        ),
        metricCards,
        contribution: normalizeInsightSection(
            safeSummary.contribution,
            contributionFallback,
            CONTRIBUTION_BREAKDOWN_COLORS
        ),
        resource: normalizeInsightSection(
            safeSummary.resource,
            resourceFallback,
            RESOURCE_BREAKDOWN_COLORS
        ),
        pendingContributorApplications,
        pendingResourceReviews,
        activeCategories: asCount(safeSummary.activeCategories),
        activeTags: asCount(safeSummary.activeTags),
        latestApplicant: asText(safeSummary.latestApplicant, "No applications yet"),
        latestResource: asText(safeSummary.latestResource, "No resources awaiting review"),
        latestTaxonomy: asText(safeSummary.latestTaxonomy, "No taxonomy updates yet"),
        archivedResources: archivedResourcesCount,
        latestArchive: asText(safeSummary.latestArchive, "No archived resources"),
        recentHistory: normalizeHistoryItems(safeSummary.recentHistory)
    };
}

function renderMetricCards(metricCards) {
    if (!summaryGrid) {
        return;
    }

    summaryGrid.innerHTML = metricCards.map((card) => `
        <article class="panel summary-card ${card.tone || ""}">
            <h2>${escapeHtml(card.label)}</h2>
            <strong>${escapeHtml(card.value)}</strong>
        </article>
    `).join("");
}

function renderContributionTrendChart(container, points) {
    const safePoints = Array.isArray(points) && points.length ? points : [{ label: "No data", value: 0 }];
    const maxValue = Math.max(...safePoints.map((point) => point.value), 1);
    container.innerHTML = `
        <div class="trend-column-grid" role="img" aria-label="Application trend chart">
            ${safePoints.map((point, index) => {
                const fillHeight = point.value > 0 ? Math.max((point.value / maxValue) * 100, 18) : 0;
                const isPeak = point.value > 0 && point.value === maxValue;
                const classes = [
                    "trend-column",
                    isPeak ? "is-peak" : "",
                    index === safePoints.length - 1 ? "is-latest" : ""
                ].filter(Boolean).join(" ");

                return `
                    <div class="${classes}">
                        <div class="trend-pill">${point.value}</div>
                        <div class="trend-guide">
                            <div class="trend-fill" style="height:${fillHeight}%;"></div>
                        </div>
                        <div class="trend-caption">${escapeHtml(point.label)}</div>
                    </div>
                `;
            }).join("")}
        </div>
    `;
}

function renderBarChart(container, points, fillColor) {
    const safePoints = Array.isArray(points) && points.length ? points : [{ label: "No data", value: 0 }];
    const maxValue = Math.max(...safePoints.map((point) => point.value), 1);
    container.innerHTML = safePoints.map((point) => {
        const height = point.value > 0 ? Math.max((point.value / maxValue) * 100, 14) : 0;
        return `
            <div class="bar-column">
                <div class="bar-value">${point.value}</div>
                <div class="bar-track">
                    <div class="bar-fill" style="height:${height}%; background:${fillColor};"></div>
                </div>
                <div class="bar-label">${escapeHtml(point.label)}</div>
            </div>
        `;
    }).join("");
}

function renderDonutChart(container, items) {
    const safeItems = Array.isArray(items) && items.length ? items : [{ label: "No data", value: 1, color: "#cbd5e1" }];
    const total = safeItems.reduce((sum, item) => sum + item.value, 0) || 1;
    let currentAngle = -90;
    const segments = safeItems.map((item) => {
        const sweep = (item.value / total) * 360;
        const startAngle = currentAngle;
        const endAngle = currentAngle + sweep;
        currentAngle = endAngle;
        return describeArcSegment(76, 76, 56, 32, startAngle, endAngle, item.color);
    }).join("");

    container.innerHTML = `
        <svg viewBox="0 0 152 152" class="donut-svg" role="img" aria-label="Breakdown chart">
            ${segments}
            <circle cx="76" cy="76" r="24" fill="#fffdf8"></circle>
            <text x="76" y="72" text-anchor="middle" class="donut-total-label">Total</text>
            <text x="76" y="92" text-anchor="middle" class="donut-total-value">${total}</text>
        </svg>
    `;
}

function describeArcSegment(cx, cy, radius, innerRadius, startAngle, endAngle, color) {
    const start = polarToCartesian(cx, cy, radius, endAngle);
    const end = polarToCartesian(cx, cy, radius, startAngle);
    const innerStart = polarToCartesian(cx, cy, innerRadius, endAngle);
    const innerEnd = polarToCartesian(cx, cy, innerRadius, startAngle);
    const largeArcFlag = endAngle - startAngle <= 180 ? 0 : 1;
    return `
        <path d="
            M ${start.x} ${start.y}
            A ${radius} ${radius} 0 ${largeArcFlag} 0 ${end.x} ${end.y}
            L ${innerEnd.x} ${innerEnd.y}
            A ${innerRadius} ${innerRadius} 0 ${largeArcFlag} 1 ${innerStart.x} ${innerStart.y}
            Z
        " fill="${color}"></path>
    `;
}

function polarToCartesian(cx, cy, radius, angleInDegrees) {
    const angleInRadians = ((angleInDegrees - 90) * Math.PI) / 180.0;
    return {
        x: cx + (radius * Math.cos(angleInRadians)),
        y: cy + (radius * Math.sin(angleInRadians))
    };
}

function renderBreakdownLegend(container, items) {
    const safeItems = Array.isArray(items) && items.length ? items : [{ label: "No data", value: 0, color: "#cbd5e1" }];
    container.innerHTML = safeItems.map((item) => `
        <div class="legend-item">
            <span class="legend-dot" style="background:${item.color};"></span>
            <div>
                <strong>${escapeHtml(item.label)}</strong>
                <span>${item.value}</span>
            </div>
        </div>
    `).join("");
}

function renderHorizontalBreakdown(container, items) {
    const safeItems = Array.isArray(items) && items.length ? items : [{ label: "No data", value: 0, color: "#cbd5e1" }];
    const maxValue = Math.max(...safeItems.map((item) => item.value), 1);
    container.innerHTML = safeItems.map((item) => `
        <div class="stack-row">
            <div class="stack-meta">
                <strong>${escapeHtml(item.label)}</strong>
                <span>${item.value}</span>
            </div>
            <div class="stack-track">
                <div class="stack-fill" style="width:${Math.max((item.value / maxValue) * 100, item.value ? 12 : 0)}%; background:${item.color};"></div>
            </div>
        </div>
    `).join("");
}

function renderTodoList(container, items) {
    const safeItems = Array.isArray(items) && items.length ? items : ["No action items right now."];
    container.innerHTML = safeItems.map((item) => `
        <li>
            <span class="todo-dot"></span>
            <span>${escapeHtml(item)}</span>
        </li>
    `).join("");
}

function renderInsightSection(binding, section, mode) {
    binding.title.textContent = asText(section?.title, "Dashboard section");
    binding.subtitle.textContent = asText(section?.subtitle, "Live analytics will appear here.");
    binding.headlineLabel.textContent = asText(section?.headlineLabel, "Total");
    binding.headlineValue.textContent = asText(section?.headlineValue, "0");
    binding.accentValue.textContent = `${asText(section?.accentLabel, "Status")}: ${asText(section?.accentValue, "0 / 0")}`;
    renderTodoList(binding.todoList, section?.todoItems || []);
    renderBreakdownLegend(binding.breakdownLegend, section?.breakdown || []);

    if (mode === "contribution") {
        renderContributionTrendChart(binding.trendChart, section?.trend || []);
        renderDonutChart(binding.breakdownChart, section?.breakdown || []);
    } else {
        renderBarChart(binding.trendChart, section?.trend || [], "#2563eb");
        renderHorizontalBreakdown(binding.breakdownChart, section?.breakdown || []);
    }
}

function renderHistoryPreview(items) {
    const safeItems = Array.isArray(items) && items.length ? items : [{
        actionType: "No activity yet",
        targetName: "Recent admin actions will appear here.",
        createdAt: null
    }];

    historyPreview.innerHTML = safeItems.map((entry) => {
        const date = entry.createdAt ? new Date(entry.createdAt) : null;
        const displayDate = date && !Number.isNaN(date.getTime())
            ? date.toLocaleDateString("en-CA", {
                year: "numeric",
                month: "short",
                day: "numeric"
            })
            : "Waiting";

        return `
        <article class="history-preview-item">
            <div>
                <strong>${escapeHtml(entry.actionType)}</strong>
                <p>${escapeHtml(entry.targetName)}</p>
            </div>
            <span>${displayDate}</span>
        </article>
    `;
    }).join("");
}

function renderSummary(summary) {
    const safeSummary = normalizeSummary(summary);
    if (attentionCount) {
        attentionCount.textContent = safeSummary.attentionCount;
    }
    renderMetricCards(safeSummary.metricCards);
    renderInsightSection(sectionBindings.contribution, safeSummary.contribution, "contribution");
    renderInsightSection(sectionBindings.resource, safeSummary.resource, "resource");
    pendingApplications.textContent = safeSummary.pendingContributorApplications;
    latestApplicant.textContent = safeSummary.latestApplicant;
    pendingResources.textContent = safeSummary.pendingResourceReviews;
    latestResource.textContent = safeSummary.latestResource;
    activeCategories.textContent = safeSummary.activeCategories;
    activeTags.textContent = safeSummary.activeTags;
    latestTaxonomy.textContent = safeSummary.latestTaxonomy;
    archivedResources.textContent = safeSummary.archivedResources;
    latestArchive.textContent = safeSummary.latestArchive;
    renderHistoryPreview(safeSummary.recentHistory);
}

async function loadDashboard() {
    try {
        const summary = await getDashboardSummary();
        renderSummary(summary);
    } catch (error) {
        console.error("Failed to load admin dashboard summary.", error);
        renderSummary({});
    }
}

loadDashboard();
