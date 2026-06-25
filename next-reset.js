(() => {
  const configKey = "fz_cfg";
  const localKey = "fz_local";

  try {
    const cfg = JSON.parse(localStorage.getItem(configKey) || "null");
    const isOldNextAutoLocal =
      cfg &&
      cfg.mode === "local" &&
      cfg.userName === "Finanza Next" &&
      !cfg.url &&
      !cfg.key &&
      !cfg.userId;

    if (!isOldNextAutoLocal) return;

    localStorage.removeItem(configKey);

    const localState = JSON.parse(localStorage.getItem(localKey) || "null");
    const hasUserData =
      localState &&
      (
        (Array.isArray(localState.transactions) && localState.transactions.length) ||
        (Array.isArray(localState.accounts) && localState.accounts.length) ||
        (Array.isArray(localState.budgets) && localState.budgets.length) ||
        (Array.isArray(localState.goals) && localState.goals.length)
      );

    if (!hasUserData) {
      localStorage.removeItem(localKey);
    }
  } catch {
    localStorage.removeItem(configKey);
  }
})();
