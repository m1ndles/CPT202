const pageName = document.body.dataset.page;

document.querySelectorAll("[data-nav]").forEach((link) => {
    if (link.dataset.nav === pageName) {
        link.classList.add("active");
    }
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
