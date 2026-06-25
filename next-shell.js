(() => {
  'use strict';

  const VERSION = '5.0.1-next';
  const HOME_ID = 'nextHome';
  const CHROME_ID = 'nextAppChrome';
  const LAUNCHER_ID = 'nextLauncher';
  const ACCENT_KEY = 'next_accent';
  const DEFAULT_ACCENT = '#35c96f';
  const accents = [
    ['#35c96f', 'Verde'],
    ['#f4cf45', 'Amarelo'],
    ['#5b8cff', 'Azul'],
    ['#b18cff', 'Lilás'],
    ['#ff7f66', 'Coral']
  ];

  const BRL = new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 2
  });

  let renderTimer = null;
  let lastHomeSignature = '';

  const esc = value => String(value ?? '').replace(/[&<>"']/g, char => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[char]));

  const validAccent = value =>
    /^#[0-9a-f]{6}$/i.test(String(value || ''))
      ? String(value).toLowerCase()
      : DEFAULT_ACCENT;

  const currentAccent = () =>
    validAccent(localStorage.getItem(ACCENT_KEY) || DEFAULT_ACCENT);

  const applyAccent = value => {
    const color = validAccent(value);
    localStorage.setItem(ACCENT_KEY, color);
    document.documentElement.style.setProperty('--next-accent', color);
    document.documentElement.style.setProperty('--ac', color);
    const meta = document.getElementById('themeColorMeta');
    if (meta) meta.content = '#f5f5f7';
    return color;
  };

  const setAccent = value => {
    applyAccent(value);
    renderAccentPanel();
    renderHome(true);
    try {
      if (typeof cfg !== 'undefined' && cfg?.mode === 'api' && typeof saveRemoteState === 'function') {
        saveRemoteState().catch(() => {});
      }
    } catch {}
  };

  window.setNextAccent = setAccent;

  const money = value => {
    try {
      if (typeof fmt === 'function') return fmt(Number(value || 0));
    } catch {}
    return BRL.format(Number(value || 0));
  };

  const todayISO = () => {
    const date = new Date();
    date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
    return date.toISOString().slice(0, 10);
  };

  const parseDate = value => {
    const date = new Date(`${value || todayISO()}T12:00:00`);
    return Number.isNaN(date.getTime()) ? new Date() : date;
  };

  const fmtDate = value =>
    parseDate(value).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' });

  const monthShort = () =>
    new Intl.DateTimeFormat('pt-BR', { month: 'short' })
      .format(new Date())
      .replace('.', '')
      .toUpperCase();

  const monthLong = () =>
    new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' })
      .format(new Date());

  const greeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Bom dia';
    if (hour < 18) return 'Boa tarde';
    return 'Boa noite';
  };

  const userName = () => {
    const candidates = [
      document.getElementById('uName')?.textContent,
      document.getElementById('setUsr')?.textContent,
      localStorage.getItem('finanza_user_name'),
      localStorage.getItem('user_name')
    ];
    const value = candidates.find(item =>
      item && !['—', 'Minha Conta', 'null', 'undefined'].includes(String(item).trim())
    );
    return String(value || 'Você').trim().split(/\s+/)[0];
  };

  const inCurrentMonth = tx => {
    const now = new Date();
    const date = parseDate(tx.date);
    return date.getMonth() === now.getMonth() && date.getFullYear() === now.getFullYear();
  };

  const isFuture = tx => String(tx?.date || '') > todayISO();

  const safeState = () => {
    try {
      return {
        transactions: Array.isArray(S?.transactions) ? S.transactions : [],
        budgets: Array.isArray(S?.budgets) ? S.budgets : [],
        accounts: Array.isArray(S?.accounts) ? S.accounts : [],
        due: Array.isArray(dueItems) ? dueItems : [],
        income: Number(monthlyIncomeCents || 0) / 100
      };
    } catch {
      return { transactions: [], budgets: [], accounts: [], due: [], income: 0 };
    }
  };

  const currentPage = () =>
    document.querySelector('.page.active')?.id?.replace('page-', '') || 'dashboard';

  const primaryFor = page => {
    if (page === 'dashboard' || page === 'transactions') return 'dashboard';
    if (page === 'future' || page === 'commitments') return 'future';
    if (page === 'budget' || page === 'goals') return 'budget';
    if (page === 'settings' || page === 'admin') return 'settings';
    return '';
  };

  const primaryNav = [
    ['future', '▤', 'Contas'],
    ['dashboard', '⌂', 'Home'],
    ['budget', '▥', 'Análise'],
    ['settings', '⚙', 'Config']
  ];

  const launcherItems = [
    ['transactions', '↕', 'Transações', 'Histórico completo e filtros'],
    ['accounts', '▣', 'Carteiras', 'Contas, cartões e saldos'],
    ['goals', '◎', 'Metas', 'Objetivos e progresso'],
    ['commitments', '▤', 'Compromissos', 'Assinaturas, dívidas e contratos'],
    ['shared', '⇄', 'Acertos', 'Gastos compartilhados'],
    ['shopping', '✓', 'Compras', 'Listas e itens'],
    ['car', '◇', 'Carro', 'Abastecimentos e manutenção'],
    ['settings', '⚙', 'Ajustes', 'Conta, dados e aparência']
  ];

  const showLauncher = open => {
    const launcher = document.getElementById(LAUNCHER_ID);
    if (!launcher) return;
    launcher.classList.toggle('open', open ?? !launcher.classList.contains('open'));
    document.body.classList.toggle('next-launcher-open', launcher.classList.contains('open'));
  };

  window.toggleNextLauncher = () => showLauncher();
  window.closeNextLauncher = () => showLauncher(false);
  window.nextGo = page => {
    showLauncher(false);
    if (typeof showPage === 'function') showPage(page);
  };

  const renderChrome = force => {
    const setupVisible = document.getElementById('setup')?.classList.contains('visible');
    document.body.classList.toggle('next-setup-visible', !!setupVisible);
    document.body.classList.add('next-app-ready');

    let chrome = document.getElementById(CHROME_ID);
    const page = currentPage();
    const active = primaryFor(page);
    document.body.dataset.nextPage = page;
    const signature = `${page}|${setupVisible}|${VERSION}`;
    if (!force && chrome?.dataset.signature === signature) return;

    if (!chrome) {
      chrome = document.createElement('div');
      chrome.id = CHROME_ID;
      chrome.className = 'next-app-chrome';
      document.body.appendChild(chrome);
    }

    chrome.dataset.signature = signature;
    chrome.innerHTML = `
      <button class="next-more-button" type="button" onclick="toggleNextLauncher()" aria-label="Abrir todos os módulos">
        <span>•••</span>
      </button>
      <nav class="next-tabbar" aria-label="Navegação principal">
        ${primaryNav.map(([id, icon, label]) => `
          <button type="button" class="${active === id ? 'active' : ''}" onclick="nextGo('${id}')">
            <span>${icon}</span>
            <strong>${label}</strong>
          </button>
        `).join('')}
      </nav>
      <button class="next-add-button" type="button" onclick="openModal()" aria-label="Nova transação">+</button>
    `;

    let launcher = document.getElementById(LAUNCHER_ID);
    if (!launcher) {
      launcher = document.createElement('div');
      launcher.id = LAUNCHER_ID;
      launcher.className = 'next-launcher';
      document.body.appendChild(launcher);
    }
    launcher.innerHTML = `
      <button class="next-launcher-backdrop" type="button" onclick="closeNextLauncher()" aria-label="Fechar"></button>
      <section class="next-launcher-sheet" aria-label="Todos os módulos">
        <div class="next-sheet-grabber"></div>
        <header>
          <div><small>Seu espaço</small><h2>Todos os módulos</h2></div>
          <button type="button" onclick="closeNextLauncher()">×</button>
        </header>
        <div class="next-launcher-grid">
          ${launcherItems.map(([id, icon, label, sub]) => `
            <button type="button" class="${page === id ? 'active' : ''}" onclick="nextGo('${id}')">
              <span>${icon}</span>
              <div><strong>${label}</strong><small>${sub}</small></div>
            </button>
          `).join('')}
        </div>
      </section>
    `;
  };

  const categoriesFor = state => {
    const map = new Map();
    state.transactions
      .filter(tx => tx.type === 'expense' && !tx.paid && inCurrentMonth(tx) && !isFuture(tx))
      .forEach(tx => {
        const key = tx.category || 'Sem categoria';
        map.set(key, (map.get(key) || 0) + Number(tx.amount || 0));
      });
    return [...map.entries()]
      .map(([name, amount]) => ({ name, amount }))
      .sort((a, b) => b.amount - a.amount);
  };

  const recentFor = state =>
    [...state.transactions]
      .filter(tx => !isFuture(tx))
      .sort((a, b) => String(b.date || '').localeCompare(String(a.date || '')))
      .slice(0, 6);

  const budgetBaseFor = state => {
    const budgetTotal = state.budgets.reduce((sum, item) => sum + Number(item.limit || 0), 0);
    const monthIncome = state.transactions
      .filter(tx => tx.type === 'income' && inCurrentMonth(tx) && !isFuture(tx))
      .reduce((sum, tx) => sum + Number(tx.amount || 0), 0);
    return state.income || budgetTotal || monthIncome;
  };

  const homeSignature = state => [
    state.transactions.length,
    state.budgets.length,
    state.accounts.length,
    state.due.length,
    budgetBaseFor(state),
    currentAccent(),
    userName(),
    VERSION
  ].join('|');

  const renderRecent = items => items.length
    ? items.map(tx => `
      <button class="box-transaction" type="button" onclick="openModal('${esc(tx.id)}')">
        <span class="box-transaction-icon">${tx.type === 'income' ? '＋' : '−'}</span>
        <span class="box-transaction-copy">
          <strong>${esc(tx.desc || tx.description || 'Transação')}</strong>
          <small>${esc(tx.category || 'Sem categoria')}</small>
        </span>
        <span class="box-transaction-value">
          <b>${tx.type === 'income' ? '+' : '-'} ${money(tx.amount)}</b>
          <small>${esc(fmtDate(tx.date))}</small>
        </span>
      </button>
    `).join('')
    : `<div class="box-empty"><strong>Nenhuma transação ainda</strong><span>Toque em + para registrar o primeiro movimento.</span></div>`;

  const renderHome = force => {
    const page = document.getElementById('page-dashboard');
    if (!page) return;

    const state = safeState();
    const signature = homeSignature(state);
    if (!force && signature === lastHomeSignature && document.getElementById(HOME_ID)) return;
    lastHomeSignature = signature;

    const monthTransactions = state.transactions.filter(tx => inCurrentMonth(tx) && !isFuture(tx));
    const spent = monthTransactions
      .filter(tx => tx.type === 'expense' && !tx.paid)
      .reduce((sum, tx) => sum + Number(tx.amount || 0), 0);
    const base = budgetBaseFor(state);
    const available = base - spent;
    const used = base > 0 ? Math.min(100, Math.max(0, spent / base * 100)) : 0;
    const recent = recentFor(state);
    const categories = categoriesFor(state);
    const daily = new Date().getDate() ? spent / new Date().getDate() : 0;
    const daysLeft = Math.max(0, new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0).getDate() - new Date().getDate());

    page.classList.add('next-home-page');
    page.querySelectorAll(':scope > *:not(#nextHome)').forEach(node => {
      node.style.display = 'none';
    });

    let home = document.getElementById(HOME_ID);
    if (!home) {
      home = document.createElement('section');
      home.id = HOME_ID;
      home.className = 'next-home';
      page.appendChild(home);
    }

    home.innerHTML = `
      <header class="box-home-head">
        <div>
          <small>${greeting()}</small>
          <h1>${esc(userName())}</h1>
        </div>
        <div class="box-home-actions">
          <button type="button" onclick="toggleNextLauncher()" aria-label="Abrir módulos">•••</button>
          <button type="button" onclick="openModal()" aria-label="Nova transação">+</button>
        </div>
      </header>

      <article class="box-budget-hero">
        <div class="box-budget-top">
          <span>Orçamento mensal</span>
          <strong>${monthShort()}</strong>
        </div>
        <h2>${money(available)}</h2>
        <div class="box-progress"><i style="width:${used}%"></i></div>
        <div class="box-budget-foot">
          <span><small>Gasto</small><strong>${money(spent)}</strong></span>
          <span><small>Orçamento mensal</small><strong>${money(base)}</strong></span>
        </div>
        <button class="box-detail-button" type="button" onclick="showPage('budget')">Ver detalhes</button>
      </article>

      <section class="box-section">
        <header>
          <h2>Últimas transações</h2>
          <button type="button" onclick="showPage('transactions')">Ver todas</button>
        </header>
        <div class="box-transaction-list">${renderRecent(recent)}</div>
      </section>

      <section class="box-insight-strip">
        <button type="button" onclick="showPage('budget')">
          <small>Maior categoria</small>
          <strong>${esc(categories[0]?.name || 'Sem gastos')}</strong>
          <span>${categories[0] ? money(categories[0].amount) : 'Tudo sob controle'}</span>
        </button>
        <button type="button" onclick="showPage('future')">
          <small>Média diária</small>
          <strong>${money(daily)}</strong>
          <span>${daysLeft} dias restantes</span>
        </button>
      </section>
    `;
  };

  const pageMeta = {
    accounts: ['Carteiras', 'Contas e cartões'],
    transactions: ['Transações', 'Histórico'],
    shared: ['Acertos', 'Compartilhado'],
    commitments: ['Compromissos', 'Planejamento'],
    future: ['Contas', 'Planejamento'],
    budget: ['Análise', 'Metas'],
    goals: ['Metas', 'Objetivos'],
    settings: ['Configurações', 'Sua conta'],
    admin: ['Administração', 'Sistema'],
    shopping: ['Lista de compras', 'Organização'],
    car: ['Meu carro', 'Mobilidade']
  };

  const decoratePages = () => {
    document.querySelectorAll('.page:not(#page-dashboard)').forEach(page => {
      const id = page.id.replace('page-', '');
      const meta = pageMeta[id];
      if (!meta) return;

      page.classList.add('box-page');
      const header = page.querySelector(':scope > .ph, :scope > .sl-header, :scope > .car-hero');
      if (!header || header.dataset.boxDecorated) return;
      header.dataset.boxDecorated = 'true';

      const title = header.querySelector('.pt');
      const subtitle = header.querySelector('.ps');
      if (title) title.textContent = meta[0];
      if (subtitle) subtitle.textContent = meta[1];

      const copyWrap = title?.parentElement;
      if (copyWrap && !copyWrap.querySelector('.box-page-kicker')) {
        const kicker = document.createElement('small');
        kicker.className = 'box-page-kicker';
        kicker.textContent = meta[1];
        copyWrap.insertBefore(kicker, title);
      }
    });
  };

  const renderAccentPanel = () => {
    const settings = document.getElementById('page-settings');
    if (!settings) return;
    let panel = document.getElementById('nextAccentSettings');
    if (!panel) {
      panel = document.createElement('div');
      panel.id = 'nextAccentSettings';
      panel.className = 'ss box-accent-settings';
      const first = settings.querySelector('.ss');
      settings.insertBefore(panel, first || settings.firstChild);
    }
    panel.innerHTML = `
      <div class="ss-t">Cor de progresso</div>
      <div class="ss-r box-accent-row">
        <div>
          <div class="ss-l">Destaque do app</div>
          <div class="ss-s">A estrutura permanece monocromática. A cor aparece apenas em progresso, status e confirmação.</div>
        </div>
        <div class="box-swatches">
          ${accents.map(([color, label]) => `
            <button type="button" title="${esc(label)}" aria-label="${esc(label)}"
              class="${currentAccent() === color ? 'active' : ''}"
              style="--sw:${color}" onclick="setNextAccent('${color}')"></button>
          `).join('')}
        </div>
      </div>
    `;
  };

  const scheduleRender = () => {
    clearTimeout(renderTimer);
    renderTimer = setTimeout(() => {
      renderChrome();
      decoratePages();
      renderAccentPanel();
      renderHome();
    }, 50);
  };

  const patchShowPage = () => {
    try {
      if (typeof showPage !== 'function' || showPage.__nextBoxPatched) return;
      const original = showPage;
      showPage = function nextBoxShowPage(...args) {
        const result = original.apply(this, args);
        showLauncher(false);
        scheduleRender();
        return result;
      };
      showPage.__nextBoxPatched = true;
    } catch {}
  };

  const init = () => {
    applyAccent(currentAccent());
    document.documentElement.setAttribute('data-next-design', 'box');
    renderChrome(true);
    patchShowPage();
    scheduleRender();

    setInterval(() => {
      patchShowPage();
      scheduleRender();
    }, 1600);

    const observer = new MutationObserver(scheduleRender);
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['class']
    });
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init, { once: true });
  } else {
    init();
  }
})();
