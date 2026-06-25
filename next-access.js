(() => {
  const configKey = "fz_cfg";
  const pageKey = "fz_page";

  try {
    const existing = JSON.parse(localStorage.getItem(configKey) || "null");
    if (existing && existing.mode) return;
  } catch {
    // If the config is corrupted, replace it with a clean local profile.
  }

  localStorage.setItem(configKey, JSON.stringify({
    url: "",
    key: "",
    mode: "local",
    userName: "Finanza Next",
    userId: "",
    role: "",
    twoFactorEnabled: false
  }));

  if (!sessionStorage.getItem(pageKey)) {
    sessionStorage.setItem(pageKey, "dashboard");
  }
})();
