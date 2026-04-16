const pageName = document.body.dataset.page;

document.querySelectorAll("[data-nav]").forEach((link) => {
    if (link.dataset.nav === pageName) {
        link.classList.add("active");
    }
});

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

const logoutButton = ensureLogoutButton();
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
