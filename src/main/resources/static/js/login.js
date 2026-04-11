const form = document.querySelector("#loginForm");
const emailInput = document.querySelector("#email");
const passwordInput = document.querySelector("#password");
const rememberMeInput = document.querySelector("#rememberMe");
const submitButton = document.querySelector("#submitButton");
const guestButton = document.querySelector("#guestButton");
const guestButtonText = document.querySelector(".guest-button-text");
const message = document.querySelector("#message");

function setMessage(text, type) {
    message.textContent = text;
    message.className = "min-h-6 text-sm leading-6";

    if (type === "error") {
        message.classList.add("text-red-700");
    } else if (type === "success") {
        message.classList.add("text-green-700");
    } else {
        message.classList.add("text-text-soft");
    }
}

function updateButtonState() {
    const hasEmail = emailInput.value.trim().length > 0;
    const hasPassword = passwordInput.value.trim().length > 0;
    submitButton.disabled = !(hasEmail && hasPassword);
}

function loadRedirectMessage() {
    const params = new URLSearchParams(window.location.search);
    if (params.get("registered") === "1") {
        setMessage("Account created successfully. Please sign in with your new account.", "success");
    }
}

async function handleSubmit(event) {
    event.preventDefault();
    setMessage("", null);

    if (submitButton.disabled) {
        setMessage("Please enter both email and password.", "error");
        return;
    }

    submitButton.disabled = true;
    submitButton.querySelector("span").textContent = "Signing In...";

    try {
        const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: emailInput.value.trim(),
                password: passwordInput.value,
                rememberMe: rememberMeInput.checked
            })
        });

        const data = await response.json();
        if (!response.ok) {
            setMessage(data.message || "Sign-in failed.", "error");
            return;
        }

        setMessage(data.message, "success");
        window.setTimeout(() => {
            window.location.href = data.redirectUrl || "/index.html";
        }, 700);
    } catch (error) {
        setMessage("Unable to reach the server. Please try again later.", "error");
    } finally {
        submitButton.querySelector("span").textContent = "Sign In";
        updateButtonState();
    }
}

async function continueAsGuest() {
    setMessage("", null);
    guestButton.disabled = true;
    guestButtonText.textContent = "Entering...";

    try {
        const response = await fetch("/api/auth/guest", {
            method: "POST"
        });
        const data = await response.json();

        if (!response.ok) {
            setMessage(data.message || "Guest access failed.", "error");
            return;
        }

        setMessage(data.message, "success");
        window.setTimeout(() => {
            window.location.href = data.redirectUrl || "/index.html";
        }, 500);
    } catch (error) {
        setMessage("Unable to reach the server. Please try again later.", "error");
    } finally {
        guestButton.disabled = false;
        guestButtonText.textContent = "Guest Access";
    }
}

emailInput.addEventListener("input", updateButtonState);
passwordInput.addEventListener("input", updateButtonState);
form.addEventListener("submit", handleSubmit);
guestButton.addEventListener("click", continueAsGuest);

updateButtonState();
loadRedirectMessage();
