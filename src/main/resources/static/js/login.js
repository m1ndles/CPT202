const form = document.querySelector("#loginForm");
const emailInput = document.querySelector("#email");
const passwordInput = document.querySelector("#password");
const rememberMeInput = document.querySelector("#rememberMe");
const submitButton = document.querySelector("#submitButton");
const guestButton = document.querySelector("#guestButton");
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
        setMessage("注册成功，请使用新账号登录。", "success");
    }
}

async function handleSubmit(event) {
    event.preventDefault();
    setMessage("", null);

    if (submitButton.disabled) {
        setMessage("请填写完整的邮箱和密码。", "error");
        return;
    }

    submitButton.disabled = true;
    submitButton.querySelector("span").textContent = "登录中...";

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
            setMessage(data.message || "登录失败。", "error");
            return;
        }

        setMessage(data.message, "success");
        window.setTimeout(() => {
            window.location.href = data.redirectUrl || "/index.html";
        }, 700);
    } catch (error) {
        setMessage("无法连接服务器，请稍后重试。", "error");
    } finally {
        submitButton.querySelector("span").textContent = "立即登录";
        updateButtonState();
    }
}

async function continueAsGuest() {
    setMessage("", null);
    guestButton.disabled = true;
    guestButton.querySelectorAll("span")[1].textContent = "进入中...";

    try {
        const response = await fetch("/api/auth/guest", {
            method: "POST"
        });
        const data = await response.json();

        if (!response.ok) {
            setMessage(data.message || "游客进入失败。", "error");
            return;
        }

        setMessage(data.message, "success");
        window.setTimeout(() => {
            window.location.href = data.redirectUrl || "/index.html";
        }, 500);
    } catch (error) {
        setMessage("无法连接服务器，请稍后重试。", "error");
    } finally {
        guestButton.disabled = false;
        guestButton.querySelectorAll("span")[1].textContent = "游客模式";
    }
}

emailInput.addEventListener("input", updateButtonState);
passwordInput.addEventListener("input", updateButtonState);
form.addEventListener("submit", handleSubmit);
guestButton.addEventListener("click", continueAsGuest);

updateButtonState();
loadRedirectMessage();
