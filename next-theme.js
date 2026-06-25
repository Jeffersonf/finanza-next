(() => {
  const STORAGE_KEY = "finanza.next.accent";
  const DEFAULT_ACCENT = "amber";
  const ACCENTS = [
    { id: "amber", label: "Amarelo", swatch: "#f5e95f" },
    { id: "lime", label: "Lima", swatch: "#d7f26a" },
    { id: "mint", label: "Menta", swatch: "#78e8c5" },
    { id: "blue", label: "Azul", swatch: "#80b8ff" },
    { id: "violet", label: "Violeta", swatch: "#b6a8ff" },
    { id: "coral", label: "Coral", swatch: "#ff9b79" }
  ];

  function getAccent() {
    const saved = localStorage.getItem(STORAGE_KEY);
    return ACCENTS.some(accent => accent.id === saved) ? saved : DEFAULT_ACCENT;
  }

  function applyAccent(accentId) {
    document.documentElement.dataset.nextAccent = accentId;
    localStorage.setItem(STORAGE_KEY, accentId);
    document.querySelectorAll("[data-next-accent-choice]").forEach(button => {
      const isActive = button.dataset.nextAccentChoice === accentId;
      button.classList.toggle("is-active", isActive);
      button.setAttribute("aria-pressed", String(isActive));
    });
  }

  function createStyleCard() {
    if (document.getElementById("nextStyleCard")) return;

    const settingsPage = document.getElementById("page-settings");
    if (!settingsPage) return;

    const card = document.createElement("section");
    card.className = "next-style-card";
    card.id = "nextStyleCard";
    card.innerHTML = `
      <h3>Visual Next</h3>
      <p>O amarelo continua como base do padrão inspirado no The Box, mas você pode trocar a cor de ação sem perder o visual escuro, direto e modular.</p>
      <div class="next-accent-grid" role="group" aria-label="Cor base do Visual Next">
        ${ACCENTS.map(accent => `
          <button
            type="button"
            class="next-accent-btn"
            data-next-accent-choice="${accent.id}"
            style="--swatch:${accent.swatch}"
            aria-label="${accent.label}"
            title="${accent.label}">
          </button>
        `).join("")}
      </div>
    `;

    const firstBox = settingsPage.querySelector(".box");
    if (firstBox && firstBox.parentNode) {
      firstBox.parentNode.insertBefore(card, firstBox.nextSibling);
    } else {
      settingsPage.appendChild(card);
    }

    card.querySelectorAll("[data-next-accent-choice]").forEach(button => {
      button.addEventListener("click", () => applyAccent(button.dataset.nextAccentChoice));
    });
  }

  applyAccent(getAccent());

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
      createStyleCard();
      applyAccent(getAccent());
    });
  } else {
    createStyleCard();
    applyAccent(getAccent());
  }
})();
