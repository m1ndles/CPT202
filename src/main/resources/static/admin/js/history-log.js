import { getHistoryLog } from "/admin/js/api.js";

const entryCount = document.getElementById("entryCount");
const searchInput = document.getElementById("searchInput");
const actionTypeFilter = document.getElementById("actionTypeFilter");
const targetTypeFilter = document.getElementById("targetTypeFilter");
const operatorFilter = document.getElementById("operatorFilter");
const clearFiltersButton = document.getElementById("clearFiltersButton");
const refreshButton = document.getElementById("refreshButton");
const historyTableBody = document.getElementById("historyTableBody");
const emptyState = document.getElementById("emptyState");

const state = {
    search: "",
    actionType: "All",
    targetType: "All",
    operator: "All"
};

function formatDate(value) {
    return new Date(value).toLocaleString("en-CA", {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

function renderOptions(element, items, currentValue) {
    element.innerHTML = items.map((item) => `
        <option value="${item}" ${item === currentValue ? "selected" : ""}>${formatOptionLabel(element.id, item)}</option>
    `).join("");
}

function toTitleCase(value) {
    return String(value || "")
        .split(" ")
        .filter(Boolean)
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join(" ");
}

function formatActionType(value) {
    if (value === "All") return "All actions";
    return toTitleCase(String(value || "").replaceAll("_", " ").replaceAll("-", " "));
}

function formatTargetType(value) {
    if (value === "All") return "All record types";
    return toTitleCase(String(value || "").replaceAll("_", " ").replaceAll("-", " "));
}

function formatOperator(value) {
    if (value === "All") return "All operators";
    if (String(value || "").toUpperCase() === "SYSTEM") {
        return "System";
    }
    return value;
}

function formatOptionLabel(selectId, value) {
    if (selectId === "actionTypeFilter") return formatActionType(value);
    if (selectId === "targetTypeFilter") return formatTargetType(value);
    if (selectId === "operatorFilter") return formatOperator(value);
    return value;
}

function renderTable(items) {
    if (!items.length) {
        historyTableBody.innerHTML = "";
        emptyState.hidden = false;
        return;
    }

    historyTableBody.innerHTML = items.map((item) => `
        <tr>
            <td class="history-action">${item.actionType}</td>
            <td><strong>${item.targetName}</strong></td>
            <td>${item.targetType}</td>
            <td>${item.operator}</td>
            <td>${formatDate(item.createdAt)}</td>
            <td>${item.details}</td>
        </tr>
    `).join("");

    emptyState.hidden = true;
}

async function loadHistory() {
    refreshButton.disabled = true;
    refreshButton.textContent = "Refreshing...";

    try {
        const data = await getHistoryLog(state);
        entryCount.textContent = data.items.length;
        renderOptions(actionTypeFilter, data.actionTypes, state.actionType);
        renderOptions(targetTypeFilter, data.targetTypes, state.targetType);
        renderOptions(operatorFilter, data.operators, state.operator);
        renderTable(data.items);
    } finally {
        refreshButton.disabled = false;
        refreshButton.textContent = "Refresh History";
    }
}

searchInput.addEventListener("input", (event) => {
    state.search = event.target.value.trim();
    loadHistory();
});

actionTypeFilter.addEventListener("change", (event) => {
    state.actionType = event.target.value;
    loadHistory();
});

targetTypeFilter.addEventListener("change", (event) => {
    state.targetType = event.target.value;
    loadHistory();
});

operatorFilter.addEventListener("change", (event) => {
    state.operator = event.target.value;
    loadHistory();
});

clearFiltersButton.addEventListener("click", () => {
    state.search = "";
    state.actionType = "All";
    state.targetType = "All";
    state.operator = "All";
    searchInput.value = "";
    loadHistory();
});

refreshButton.addEventListener("click", loadHistory);

loadHistory();
