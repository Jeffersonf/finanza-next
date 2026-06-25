const FinanzaNext = (() => {
  const storeKey = "finanza.next.v5";
  const accentKey = "finanza.next.accent";
  const money = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
  const today = new Date().toISOString().slice(0, 10);

  const nav = [
    ["overview", "Início", "●"],
    ["transactions", "Transações", "◆"],
    ["bills", "Contas", "!"],
    ["analysis", "Análise", "%"],
    ["wallets", "Carteiras", "□"],
    ["goals", "Metas", "▲"],
    ["plans", "Planos", "◌"],
    ["settings", "Ajustes", "⚙"]
  ];

  const accents = [
    ["amber", "Amarelo", "#f5e95f"],
    ["lime", "Lima", "#d7f26a"],
    ["mint", "Menta", "#78e8c5"],
    ["blue", "Azul", "#80b8ff"],
    ["violet", "Violeta", "#b6a8ff"],
    ["coral", "Coral", "#ff9b79"]
  ];

  const categories = ["Moradia", "Mercado", "Transporte", "Lazer", "Saúde", "Educação", "Carro", "Compras"];

  const demo = {
    budget: 6200,
    wallets: [
      { id: "pix", name: "Pix", kind: "Conta", balance: 2450 },
      { id: "debit", name: "Débito", kind: "Banco", balance: 1320 },
      { id: "credit", name: "Cartão", kind: "Crédito", balance: -860 },
      { id: "cash", name: "Dinheiro", kind: "Carteira", balance: 180 }
    ],
    entries: [
      { id: 1, title: "Salário", category: "Receita", wallet: "pix", amount: 5500, type: "income", date: today },
      { id: 2, title: "Freela", category: "Receita", wallet: "pix", amount: 900, type: "income", date: today },
      { id: 3, title: "Mercado", category: "Mercado", wallet: "debit", amount: 286.75, type: "expense", date: today },
      { id: 4, title: "Combustível", category: "Carro", wallet: "credit", amount: 190, type: "expense", date: today },
      { id: 5, title: "Cinema", category: "Lazer", wallet: "credit", amount: 78, type: "expense", date: offsetDate(-1) },
      { id: 6, title: "Farmácia", category: "Saúde", wallet: "debit", amount: 64.9, type: "expense", date: offsetDate(-2) },
      { id: 7, title: "Curso", category: "Educação", wallet: "credit", amount: 129.9, type: "expense", date: offsetDate(-3) }
    ],
    bills: [
      { id: 1, title: "Aluguel", category: "Moradia", amount: 1280, due: offsetDate(3), paid: false },
      { id: 2, title: "Internet", category: "Moradia", amount: 119.9, due: offsetDate(5), paid: false },
      { id: 3, title: "Fatura cartão", category: "Cartão", amount: 860, due: offsetDate(8), paid: false },
      { id: 4, title: "Seguro carro", category: "Carro", amount: 210, due: offsetDate(14), paid: false }
    ],
    goals: [
      { title: "Viagem", current: 4200, target: 10000 },
      { title: "Reserva", current: 6800, target: 12000 },
      { title: "Quitar cartão", current: 540, target: 860 }
    ],
    shopping: ["Mercado da semana", "Tênis novo", "Presente aniversário"],
    car: ["IPVA reservado", "Troca de óleo em 20 dias", "Seguro vence em 14 dias"]
  };

  let state = load();
  let route = "overview";
  let entryMode = "expense";

  function offsetDate(days) {
    const d = new Date();
    d.setDate(d.getDate() + days);
    return d.toISOString().slice(0, 10);
  }

  function load() {
    const saved = localStorage.getItem(storeKey);
    return saved ? JSON.parse(saved) : structuredClone(demo);
  }

  function save() {
    localStorage.setItem(storeKey, JSON.stringify(state));
  }

  function byId(id) { return document.getElementById(id); }
  function fmtDate(value) { return new Date(`${value}T12:00:00`).toLocaleDateString("pt-BR", { day: "2-digit", month: "short" }); }
  function parseAmount(value) { return Number(String(value).replace(/\./g, "").replace(",", ".")) || 0; }
  function visibleMoney(value) { return money.format(value); }

  function init() {
    document.documentElement.dataset.accent = localStorage.getItem(accentKey) || "amber";
    renderNav();
    bindActions();
    fillForms();
    routeTo(location.hash.replace("#", "") || "overview", false);
    renderAll();
  }

  function renderNav() {
    const side = byId("sideNav");
    const mobile = byId("mobileNav");
    side.innerHTML = "";
    mobile.innerHTML = "";
    nav.forEach(([id, label, icon], index) => {
      const item = `<button class="nav-item" data-route="${id}"><span>${icon}</span>${label}</button>`;
      side.insertAdjacentHTML("beforeend", item);
      if (index < 5) mobile.insertAdjacentHTML("beforeend", item);
    });
  }

  function bindActions() {
    document.addEventListener("click", event => {
      const routeTarget = event.target.closest("[data-route]");
      if (routeTarget) {
        event.preventDefault();
        routeTo(routeTarget.dataset.route);
      }
      const actionTarget = event.target.closest("[data-action]");
      if (!actionTarget) return;
      const action = actionTarget.dataset.action;
      if (action === "open-entry") openEntry("expense");
      if (action === "open-income") openEntry("income");
      if (action === "open-bill") byId("billDialog").showModal();
      if (action === "open-budget") editBudget();
      if (action === "open-wallet") addWallet();
      if (action === "toggle-privacy") document.body.classList.toggle("is-private");
      if (action === "mark-paid") markNextPaid();
      if (action === "reset-demo") resetDemo();
    });

    byId("entryForm").addEventListener("submit", submitEntry);
    byId("billForm").addEventListener("submit", submitBill);
    byId("searchInput").addEventListener("input", renderTransactions);
    byId("walletFilter").addEventListener("change", renderTransactions);
  }

  function fillForms() {
    const categoryOptions = categories.map(cat => `<option>${cat}</option>`).join("");
    const walletOptions = state.wallets.map(w => `<option value="${w.id}">${w.name}</option>`).join("");
    document.querySelector('#entryForm [name="category"]').innerHTML = categoryOptions;
    document.querySelector('#billForm [name="category"]').innerHTML = categoryOptions;
    document.querySelector('#entryForm [name="wallet"]').innerHTML = walletOptions;
    document.querySelector('#entryForm [name="date"]').value = today;
    document.querySelector('#billForm [name="due"]').value = offsetDate(7);
    byId("walletFilter").innerHTML = `<option value="">Todas as carteiras</option>${walletOptions}`;
  }

  function routeTo(nextRoute, push = true) {
    route = nav.some(([id]) => id === nextRoute) ? nextRoute : "overview";
    document.querySelectorAll(".screen").forEach(screen => screen.classList.toggle("is-active", screen.id === `screen-${route}`));
    document.querySelectorAll(".nav-item").forEach(item => item.classList.toggle("is-active", item.dataset.route === route));
    const screen = byId(`screen-${route}`);
    byId("screenTitle").textContent = screen.dataset.title;
    byId("screenSubtitle").textContent = screen.dataset.subtitle;
    if (push) history.replaceState(null, "", `#${route}`);
    renderAll();
  }

  function renderAll() {
    renderOverview();
    renderTransactions();
    renderBills();
    renderAnalysis();
    renderWallets();
    renderGoals();
    renderPlans();
    renderAccents();
  }

  function totals() {
    const income = state.entries.filter(e => e.type === "income").reduce((sum, e) => sum + e.amount, 0);
    const expense = state.entries.filter(e => e.type === "expense").reduce((sum, e) => sum + e.amount, 0);
    return { income, expense, remaining: state.budget + income - expense, used: Math.min(100, Math.round((expense / state.budget) * 100)) };
  }

  function renderOverview() {
    const t = totals();
    byId("budgetRemaining").textContent = visibleMoney(t.remaining);
    byId("monthIncome").textContent = visibleMoney(t.income);
    byId("monthExpense").textContent = visibleMoney(t.expense);
    byId("budgetPercent").textContent = `${t.used}%`;
    byId("budgetMeter").style.width = `${t.used}%`;
    byId("budgetSentence").textContent = t.remaining >= 0 ? `Você ainda tem ${visibleMoney(t.remaining)} de margem prevista.` : `Você passou ${visibleMoney(Math.abs(t.remaining))} do limite previsto.`;
    byId("todayCount").textContent = state.entries.filter(e => e.date === today).length;
    byId("dueCount").textContent = state.bills.filter(b => !b.paid).length;
    byId("topCategory").textContent = topCategory().name;
    byId("donutCenter").textContent = `${t.used}%`;
    renderLegend();
    renderList(byId("recentList"), state.entries.slice().reverse().slice(0, 5), entryRow);
    renderList(byId("upcomingList"), state.bills.filter(b => !b.paid).slice(0, 4), billRow);
  }

  function categoryTotals() {
    const map = {};
    state.entries.filter(e => e.type === "expense").forEach(e => map[e.category] = (map[e.category] || 0) + e.amount);
    return Object.entries(map).sort((a, b) => b[1] - a[1]).map(([name, amount]) => ({ name, amount }));
  }

  function topCategory() { return categoryTotals()[0] || { name: "—", amount: 0 }; }

  function renderLegend() {
    const colors = ["var(--accent)", "var(--violet)", "var(--accent2)", "#666"];
    byId("categoryLegend").innerHTML = categoryTotals().slice(0, 4).map((item, index) => `
      <div class="legend-row"><i class="dot" style="--dot:${colors[index]}"></i><span>${item.name}</span><strong>${visibleMoney(item.amount)}</strong></div>
    `).join("") || `<p class="muted">Sem gastos ainda.</p>`;
  }

  function renderTransactions() {
    const q = byId("searchInput")?.value?.toLowerCase() || "";
    const wallet = byId("walletFilter")?.value || "";
    const items = state.entries.filter(e => (!q || `${e.title} ${e.category}`.toLowerCase().includes(q)) && (!wallet || e.wallet === wallet)).slice().reverse();
    renderList(byId("transactionsList"), items, entryRow);
  }

  function renderBills() {
    renderList(byId("billsTimeline"), state.bills.slice().sort((a, b) => a.due.localeCompare(b.due)), billRow);
  }

  function renderAnalysis() {
    const t = totals();
    byId("analysisGrid").innerHTML = [
      stat("Orçamento usado", `${t.used}%`, "Percentual gasto do limite mensal."),
      stat("Categoria líder", topCategory().name, `${visibleMoney(topCategory().amount)} concentrados aqui.`),
      stat("Saldo previsto", visibleMoney(t.remaining), "Margem antes do mês fechar.")
    ].join("") + categoryTotals().map(item => stat(item.name, visibleMoney(item.amount), "Gasto acumulado na categoria.")).join("");
  }

  function renderWallets() {
    byId("walletsGrid").innerHTML = state.wallets.map(w => stat(w.name, visibleMoney(w.balance), w.kind)).join("");
    fillForms();
  }

  function renderGoals() {
    byId("goalsGrid").innerHTML = state.goals.map(g => stat(g.title, `${Math.round((g.current / g.target) * 100)}%`, `${visibleMoney(g.current)} de ${visibleMoney(g.target)}`)).join("");
  }

  function renderPlans() {
    byId("shoppingList").innerHTML = state.shopping.map((item, i) => planRow(item, i + 1)).join("");
    byId("carList").innerHTML = state.car.map((item, i) => planRow(item, i + 1)).join("");
  }

  function renderAccents() {
    byId("accentGrid").innerHTML = accents.map(([id, label, swatch]) => `
      <button class="accent-button ${document.documentElement.dataset.accent === id ? "is-active" : ""}" data-accent-choice="${id}" title="${label}"><span style="--swatch:${swatch}"></span></button>
    `).join("");
    document.querySelectorAll("[data-accent-choice]").forEach(button => button.onclick = () => {
      document.documentElement.dataset.accent = button.dataset.accentChoice;
      localStorage.setItem(accentKey, button.dataset.accentChoice);
      renderAccents();
    });
  }

  function renderList(node, items, renderer) {
    node.innerHTML = items.length ? items.map(renderer).join("") : `<p class="muted">Nada por aqui ainda.</p>`;
  }

  function entryRow(e) {
    const isIncome = e.type === "income";
    const wallet = state.wallets.find(w => w.id === e.wallet)?.name || e.wallet;
    return `<article class="row" style="--row-color:${isIncome ? "var(--accent2)" : "var(--danger)"}">
      <span class="row-icon">${isIncome ? "+" : "−"}</span>
      <div><strong>${e.title}</strong><small>${e.category} • ${wallet} • ${fmtDate(e.date)}</small></div>
      <div class="row-value" data-money>${isIncome ? "+" : "−"}${visibleMoney(e.amount)}<small>${e.type === "income" ? "entrada" : "saída"}</small></div>
    </article>`;
  }

  function billRow(b) {
    return `<article class="row" style="--row-color:${b.paid ? "var(--accent2)" : "var(--accent)"}">
      <span class="row-icon">${b.paid ? "✓" : "!"}</span>
      <div><strong>${b.title}</strong><small>${b.category} • vence ${fmtDate(b.due)}</small></div>
      <div class="row-value" data-money>${visibleMoney(b.amount)}<small>${b.paid ? "paga" : "pendente"}</small></div>
    </article>`;
  }

  function planRow(item, index) {
    return `<article class="row"><span class="row-icon">${index}</span><div><strong>${item}</strong><small>planejado</small></div></article>`;
  }

  function stat(label, value, copy) {
    return `<article class="stat-card"><span class="eyebrow">${label}</span><strong data-money>${value}</strong><p>${copy}</p></article>`;
  }

  function openEntry(mode) {
    entryMode = mode;
    byId("entryDialogTitle").textContent = mode === "income" ? "Nova receita" : "Novo gasto";
    byId("entryDialog").showModal();
  }

  function submitEntry(event) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    state.entries.push({
      id: Date.now(),
      title: data.get("title"),
      amount: parseAmount(data.get("amount")),
      category: data.get("category"),
      wallet: data.get("wallet"),
      date: data.get("date") || today,
      type: entryMode
    });
    save();
    event.currentTarget.reset();
    document.querySelector('#entryForm [name="date"]').value = today;
    byId("entryDialog").close();
    renderAll();
  }

  function submitBill(event) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    state.bills.push({ id: Date.now(), title: data.get("title"), amount: parseAmount(data.get("amount")), category: data.get("category"), due: data.get("due") || today, paid: false });
    save();
    event.currentTarget.reset();
    document.querySelector('#billForm [name="due"]').value = offsetDate(7);
    byId("billDialog").close();
    renderAll();
  }

  function editBudget() {
    const value = prompt("Orçamento mensal", state.budget);
    if (value === null) return;
    state.budget = parseAmount(value);
    save();
    renderAll();
  }

  function addWallet() {
    const name = prompt("Nome da carteira");
    if (!name) return;
    state.wallets.push({ id: name.toLowerCase().replace(/\W+/g, "-"), name, kind: "Carteira", balance: 0 });
    save();
    renderAll();
  }

  function markNextPaid() {
    const next = state.bills.find(b => !b.paid);
    if (next) next.paid = true;
    save();
    renderAll();
  }

  function resetDemo() {
    if (!confirm("Restaurar dados de demonstração?")) return;
    state = structuredClone(demo);
    save();
    renderAll();
  }

  return { init };
})();

document.addEventListener("DOMContentLoaded", FinanzaNext.init);
