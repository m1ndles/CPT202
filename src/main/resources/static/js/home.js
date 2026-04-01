const welcomeTitle = document.querySelector("#welcomeTitle");
const sessionText = document.querySelector("#sessionText");
const logoutButton = document.querySelector("#logoutButton");
const homeMessage = document.querySelector("#homeMessage");

function setHomeMessage(text, type) {
    homeMessage.textContent = text;
    homeMessage.className = "message";

    if (type) {
        homeMessage.classList.add(type === "error" ? "is-error" : "is-success");
    }
}

async function loadSession() {
    try {
        const response = await fetch("/api/auth/me");
        const data = await response.json();

        if (!response.ok) {
            window.location.href = "/login.html";
            return;
        }

        welcomeTitle.textContent = `Welcome back, ${data.username}.`;
        sessionText.textContent = `Your session is active for ${data.email}. You can continue into the platform from here.`;
    } catch (error) {
        setHomeMessage("Unable to load session details.", "error");
    }
}

async function logout() {
    logoutButton.disabled = true;
    logoutButton.textContent = "Signing out...";

    try {
        const response = await fetch("/api/auth/logout", {
            method: "POST"
        });
        const data = await response.json();

        if (!response.ok) {
            setHomeMessage(data.message || "Logout failed.", "error");
            return;
        }

        setHomeMessage(data.message, "success");
        window.setTimeout(() => {
            window.location.href = data.redirectUrl || "/login.html";
        }, 500);
    } catch (error) {
        setHomeMessage("Unable to reach the server.", "error");
    } finally {
        logoutButton.disabled = false;
        logoutButton.textContent = "Log Out";
    }
}

logoutButton.addEventListener("click", logout);
loadSession();
