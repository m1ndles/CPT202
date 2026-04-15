(function () {
  const icons = {
    account_balance: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 3 7v2h18V7l-9-4Zm7 8H5v6H3v2h18v-2h-2v-6Zm-2 0v6h-2v-6h2Zm-4 0v6h-2v-6h2Zm-4 0v6H7v-6h2Z"/></svg>',
    manage_accounts: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0-6.2a2.2 2.2 0 1 1 0 4.4 2.2 2.2 0 0 1 0-4.4ZM4 19.5c0-3.2 3.66-4.9 6-4.9s6 1.7 6 4.9V21H4v-1.5Zm1.97-.3h8.06c-.3-1.43-2.26-2.8-4.03-2.8s-3.73 1.37-4.03 2.8ZM18.35 11.95l-1.3-.75.35-1.47-1.55-.9-1.08 1.06-1.44-.38v1.8l1.44-.38 1.08 1.06 1.55-.9-.35-1.47 1.3-.77Z"/></svg>',
    send: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3.4 20.4 21 12 3.4 3.6 3 10l12 2-12 2 .4 6.4Z"/></svg>',
    progress_activity: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 4a8 8 0 1 0 8 8" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/><path d="M20 4v5h-5" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    account_circle: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2Zm0 4a3.2 3.2 0 1 1-3.2 3.2A3.2 3.2 0 0 1 12 6Zm0 14a8 8 0 0 1-5.54-2.22c.78-1.58 3.3-2.58 5.54-2.58s4.76 1 5.54 2.58A8 8 0 0 1 12 20Z"/></svg>',
    upload: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19h14v2H5v-2Zm7-16 5 5h-3v6h-4V8H7l5-5Z"/></svg>',
    save: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M17 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V7l-4-4Zm-5 16a3 3 0 1 1 3-3 3 3 0 0 1-3 3Zm3-10H5V5h10v4Z"/></svg>',
    mail: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 5h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Zm8 6 8-4H4l8 4Zm0 2L4 9v8h16V9l-8 4Z"/></svg>',
    key: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M14 3a7 7 0 0 0-6.9 8.2L2 16.3V21h4.7l1.5-1.5V18h1.8l1.5-1.5V15h1.5A7 7 0 1 0 14 3Zm3 7a1.5 1.5 0 1 1 1.5-1.5A1.5 1.5 0 0 1 17 10Z"/></svg>',
    arrow_forward: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M13 5.5 19.5 12 13 18.5l-1.4-1.4 4.1-4.1H4v-2h11.7l-4.1-4.1L13 5.5Z"/></svg>',
    search: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 4a6 6 0 1 0 3.87 10.58l4.27 4.28 1.42-1.42-4.28-4.27A6 6 0 0 0 10 4Zm0 2a4 4 0 1 1 0 8 4 4 0 0 1 0-8Z"/></svg>',
    refresh: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M17.65 6.35A7.95 7.95 0 0 0 12 4V1L7 6l5 5V7a5 5 0 1 1-4.9 6H5.02A7 7 0 1 0 17.65 6.35Z"/></svg>',
    museum: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 2 8v2h20V8L12 3Zm-7 9h2v6H5v-6Zm4 0h2v6H9v-6Zm4 0h2v6h-2v-6Zm4 0h2v6h-2v-6ZM2 20h20v2H2v-2Z"/></svg>',
    location_on: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2a7 7 0 0 0-7 7c0 5.25 7 13 7 13s7-7.75 7-13a7 7 0 0 0-7-7Zm0 9.5A2.5 2.5 0 1 1 14.5 9 2.5 2.5 0 0 1 12 11.5Z"/></svg>',
    visibility: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5c-5 0-9.27 3.11-11 7 1.73 3.89 6 7 11 7s9.27-3.11 11-7c-1.73-3.89-6-7-11-7Zm0 11a4 4 0 1 1 4-4 4 4 0 0 1-4 4Zm0-6.4A2.4 2.4 0 1 0 14.4 12 2.41 2.41 0 0 0 12 9.6Z"/></svg>',
    chevron_left: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m14.7 6.3-1.4-1.4L6.2 12l7.1 7.1 1.4-1.4L9 12l5.7-5.7Z"/></svg>',
    chevron_right: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9.3 17.7 1.4 1.4 7.1-7.1-7.1-7.1-1.4 1.4L15 12l-5.7 5.7Z"/></svg>',
    arrow_back: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m11 5-7 7 7 7 1.4-1.4L7.8 13H20v-2H7.8l4.6-4.6L11 5Z"/></svg>',
    forum: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 4h16a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H8l-4 4V6a2 2 0 0 1 2-2Zm2 4v2h12V8H6Zm0 4v2h8v-2H6Z"/></svg>',
    chat_bubble: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 4h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H7l-5 4V6a2 2 0 0 1 2-2Z"/></svg>',
    folder: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 4 12 6h8a2 2 0 0 1 2 2v8a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3h5Z"/></svg>',
    description: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 2h9l5 5v15a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Zm8 1.5V8h4.5L14 3.5ZM8 12h8v2H8v-2Zm0 4h8v2H8v-2Zm0-8h5v2H8V8Z"/></svg>',
    link: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10.59 13.41a1.996 1.996 0 0 1 0-2.82l3.18-3.18a2 2 0 1 1 2.83 2.83l-1.06 1.06 1.41 1.41 1.06-1.06a4 4 0 1 0-5.65-5.65l-3.18 3.18a4 4 0 0 0 5.66 5.66l.7-.7-1.41-1.41-.7.68a2 2 0 0 1-2.84 0Zm2.82-2.82a2 2 0 0 1 0 2.82l-3.18 3.18a2 2 0 1 1-2.83-2.83l1.06-1.06-1.41-1.41-1.06 1.06a4 4 0 1 0 5.65 5.65l3.18-3.18a4 4 0 0 0-5.66-5.66l-.7.7 1.41 1.41.7-.68a2 2 0 0 1 2.84 0Z"/></svg>',
    open_in_new: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M14 3h7v7h-2V6.41l-9.29 9.3-1.42-1.42 9.3-9.29H14V3ZM5 5h6v2H7v10h10v-4h2v6H5V5Z"/></svg>',
    calendar_today: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 2h2v2h6V2h2v2h3a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h3V2Zm13 8H4v10h16V10Zm0-4H4v2h16V6Z"/></svg>',
    sell: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M21.41 11.58 12.42 2.59A2 2 0 0 0 11 2H4a2 2 0 0 0-2 2v7a2 2 0 0 0 .59 1.41l8.99 8.99a2 2 0 0 0 2.83 0l7-7a2 2 0 0 0 0-2.82ZM6.5 8A1.5 1.5 0 1 1 8 6.5 1.5 1.5 0 0 1 6.5 8Z"/></svg>',
    copyright: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2Zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8Zm3-5.5A4.5 4.5 0 1 1 15 9h-2.1a2.5 2.5 0 1 0 0 5H15Z"/></svg>',
    favorite: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 21.35-1.45-1.32C5.4 15.36 2 12.28 2 8.5A4.5 4.5 0 0 1 6.5 4C8.24 4 9.91 4.81 11 6.09 12.09 4.81 13.76 4 15.5 4A4.5 4.5 0 0 1 20 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35Z"/></svg>',
    error: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M1 21h22L12 2 1 21Zm12-3h-2v-2h2v2Zm0-4h-2v-4h2v4Z"/></svg>'
  };

  function replaceIcon(node) {
    const iconName = (node.textContent || "").trim();
    const svg = icons[iconName];
    if (!svg) {
      return;
    }
    if (iconName === "progress_activity") {
      node.setAttribute("data-stroke", "true");
    }
    node.innerHTML = svg;
    node.setAttribute("aria-hidden", "true");
  }

  function applyIcons(root) {
    root.querySelectorAll(".material-symbols-outlined").forEach(replaceIcon);
  }

  window.applyLocalIcons = applyIcons;

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", function () {
      applyIcons(document);
    });
  } else {
    applyIcons(document);
  }

  const observer = new MutationObserver(function (mutations) {
    mutations.forEach(function (mutation) {
      mutation.addedNodes.forEach(function (node) {
        if (!(node instanceof Element)) {
          return;
        }
        if (node.classList.contains("material-symbols-outlined")) {
          replaceIcon(node);
        }
        applyIcons(node);
      });
    });
  });

  observer.observe(document.documentElement, {
    childList: true,
    subtree: true
  });
})();
