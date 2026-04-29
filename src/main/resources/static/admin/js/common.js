const pageName = document.body.dataset.page;

function syncActiveNavState() {
    document.querySelectorAll("[data-nav]").forEach((link) => {
        link.classList.toggle("active", link.dataset.nav === pageName);
    });
}

function ensureComplaintLink() {
    const adminNav = document.querySelector(".admin-nav");
    if (!adminNav || adminNav.querySelector('[data-nav="complaints"]')) {
        return;
    }

    const complaintLink = document.createElement("a");
    complaintLink.href = "/admin/complaint-management.html";
    complaintLink.dataset.nav = "complaints";
    complaintLink.className = "nav-link";
    complaintLink.textContent = "Complaints";
    adminNav.appendChild(complaintLink);
}

function ensureLogoutButton() {
    const adminNav = document.querySelector(".admin-nav");
    if (!adminNav || document.getElementById("adminLogoutButton")) {
        return null;
    }

    const logoutButton = document.createElement("button");
    logoutButton.type = "button";
    logoutButton.id = "adminLogoutButton";
    logoutButton.className = "nav-link nav-action";
    logoutButton.textContent = "Log out";
    adminNav.appendChild(logoutButton);
    return logoutButton;
}

async function handleLogout(logoutButton) {
    if (logoutButton) {
        logoutButton.disabled = true;
        logoutButton.textContent = "Signing out...";
    }

    try {
        await fetch("/api/auth/logout", {
            method: "POST",
            credentials: "same-origin"
        });
    } finally {
        window.location.replace("/login.html");
    }
}

ensureComplaintLink();
const logoutButton = ensureLogoutButton();
syncActiveNavState();
logoutButton?.addEventListener("click", () => {
    handleLogout(logoutButton);
});

(async () => {
    try {
        const response = await fetch("/api/auth/me", {
            credentials: "same-origin"
        });

        if (!response.ok) {
            window.location.replace("/login.html");
            return;
        }

        const user = await response.json();
        if (user.role !== "ADMIN") {
            window.location.replace("/index.html");
        }
    } catch {
        window.location.replace("/login.html");
    }
})();
