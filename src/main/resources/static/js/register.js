const registerForm = document.querySelector("#registerForm");
const registerEmailInput = document.querySelector("#registerEmail");
const registerUsernameInput = document.querySelector("#registerUsername");
const registerPasswordInput = document.querySelector("#registerPassword");
const confirmPasswordInput = document.querySelector("#confirmPassword");
const termsInput = document.querySelector("#terms");
const registerSubmitButton = document.querySelector("#registerSubmitButton");
const registerMessage = document.querySelector("#registerMessage");

function setRegisterMessage(text, type) {
    registerMessage.textContent = text;
    registerMessage.className = "min-h-6 text-sm leading-6";

    if (type === "error") {
        registerMessage.classList.add("text-red-700");
    } else if (type === "success") {
        registerMessage.classList.add("text-green-700");
    } else {
        registerMessage.classList.add("text-text-soft");
    }
}

function isValidEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function updateRegisterButtonState() {
    const hasEmail = registerEmailInput.value.trim().length > 0;
    const hasUsername = registerUsernameInput.value.trim().length > 0;
    const hasPassword = registerPasswordInput.value.length > 0;
    const hasConfirmPassword = confirmPasswordInput.value.length > 0;
    const acceptedTerms = termsInput.checked;
    registerSubmitButton.disabled = !(hasEmail && hasUsername && hasPassword && hasConfirmPassword && acceptedTerms);
}

function validateRegisterForm() {
    const email = registerEmailInput.value.trim();
    const username = registerUsernameInput.value.trim();
    const password = registerPasswordInput.value;
    const confirmPassword = confirmPasswordInput.value;

    if (!email || !username || !password || !confirmPassword) {
        return "请完整填写注册信息。";
    }

    if (!isValidEmail(email)) {
        return "请输入有效的邮箱地址。";
    }

    if (password.length < 8) {
        return "密码长度至少为 8 位。";
    }

    if (password !== confirmPassword) {
        return "两次输入的密码不一致。";
    }

    if (!termsInput.checked) {
        return "请先阅读并同意服务条款。";
    }

    return null;
}

async function handleRegisterSubmit(event) {
    event.preventDefault();
    setRegisterMessage("", null);

    const validationMessage = validateRegisterForm();
    if (validationMessage) {
        setRegisterMessage(validationMessage, "error");
        return;
    }

    registerSubmitButton.disabled = true;
    registerSubmitButton.querySelector("span").textContent = "注册中...";

    try {
        const response = await fetch("/api/auth/register", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: registerEmailInput.value.trim(),
                username: registerUsernameInput.value.trim(),
                password: registerPasswordInput.value
            })
        });

        const data = await response.json();
        if (!response.ok) {
            setRegisterMessage(data.message || "注册失败。", "error");
            return;
        }

        setRegisterMessage(data.message, "success");
        window.setTimeout(() => {
            window.location.href = data.redirectUrl || "/login.html?registered=1";
        }, 700);
    } catch (error) {
        setRegisterMessage("无法连接服务器，请稍后重试。", "error");
    } finally {
        registerSubmitButton.querySelector("span").textContent = "立即注册";
        updateRegisterButtonState();
    }
}

registerEmailInput.addEventListener("input", updateRegisterButtonState);
registerUsernameInput.addEventListener("input", updateRegisterButtonState);
registerPasswordInput.addEventListener("input", updateRegisterButtonState);
confirmPasswordInput.addEventListener("input", updateRegisterButtonState);
termsInput.addEventListener("change", updateRegisterButtonState);
registerForm.addEventListener("submit", handleRegisterSubmit);

updateRegisterButtonState();
