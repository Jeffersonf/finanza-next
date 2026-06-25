(() => {
  'use strict';

  const HOME_ID = 'nextHome';
  const VERSION = '4.6.0-next';
  let renderTimer = null;
  let lastSignature = '';

  const BRL = new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 2
  });

  const monthName = new Intl.DateTimeFormat('pt-BR', {
    month: 'long',
    year: 'numeric'
  });

  const esc = value => String(value ?? '').replace(/[&<>"']/g, char => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[char]));

  const money = value => {
    try {
      if (typeof fmt === 'function') return fmt(Number(value || 0));
    } catch {}
    return BRL.format(Number(value || 0));
  };

  const rawMoney = value => {
    try {
      if (typeof rawFmt === 'function') return rawFmt(Number(value || 0));
    } catch {}
    return BRL.format(Number(value || 0));
  };

  const todayISO = () => {
    const d = new Date();
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 10);
  };

  const parseDate = value => {
    const date = new Date(`${value || todayISO()}T12:00:00`);
    return Number.isNaN(date.getTime()) ? new Date() : date;
  };

  const fmtDate = value => {
    const date = parseDate(value);
    return date.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' });
  };

  const inCurrentMonth = tx => {
    const now = new Date();
    const d = parseDate(tx.date);
    return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
  };

  const isFuture = tx => String(tx?.date || '') > todayISO();

  const safeState = () => {
    try {
      return {
        transactions: Array.isArray(S?.transactions) ? S.transactions : [],
        budgets: Array.isArray(S?.budgets) ? S.budgets : [],
        goals: Array.isArray(S?.goals) ? S.goals : [],
        accounts: Array.isArray(S?.accounts) ? S.accounts : [],
        due: Array.isArray(dueItems) ? dueItems : [],
        income: Number(monthlyIncomeCents || 0) / 100
      };
    } catch {
      return { transactions: [], budgets: [], goals: [], accounts: [], due: [], income: 0 };
    }
  };

  const catMeta = name => {
    try {
      if (typeof getCat === 'function') return getCat(name);
    } catch {}
    return { ico: '•', name: name || 'Sem categoria', col: '#111111' };
  };

  const signatureOf = state => [
    state.transactions.length,
    state.budgets.length,
    state.goals.length,
    state.accounts.length,
    state.due.length,
    state.income,
    document.querySelector('#page-dashboard.active') ? 'active' : 'hidden',
    VERSION
  ].join('|');

  const categorySummary = state => {
    const map = new Map();
    state.transactions
      .filter(tx => tx.type === 'expense' && !tx.paid && inCurrentMonth(tx) && !isFuture(tx))
      .forEach(tx => {
        const key = tx.category || 'Sem categoria';
        map.set(key, (map.get(key) || 0) + Number(tx.amount || 0));
      });
    return [...map.entries()]
      .map(([name, amount]) => ({ name, amount, meta: catMeta(name) }))
      .sort((a, b) => b.amount - a.amount);
  };

  const upcoming = state => {
    const dues = state.due.map(item => ({
      type: 'due',
      title: item.name || item.title || item.desc || 'Conta fixa',
      amount: Number(item.amount || item.value || 0),
      category: item.category || 'Vencimento',
      date: item.date || nextDayDate(item.day),
      status: item.paid ? 'Pago' : 'Aberto'
    }));

    const futures = state.transactions
      .filter(tx => isFuture(tx) || tx.paid)
      .map(tx => ({
        type: 'tx',
        title: tx.desc || tx.description || 'Movimento futuro',
        amount: Number(tx.amount || 0),
        category: tx.category || (tx.type === 'income' ? 'Receita' : 'Despesa'),
        date: tx.date,
        status: tx.type === 'income' ? 'Entrada' : 'Previsto'
      }));

    return [...dues, ...futures]
      .sort((a, b) => String(a.date || '').localeCompare(String(b.date || '')))
      .slice(0, 5);
  };

  const nextDayDate = day => {
    const d = new Date();
    const safeDay = Math.min(Math.max(Number(day || 1), 1), 28);
    d.setDate(safeDay);
    if (d < new Date()) d.setMonth(d.getMonth() + 1);
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 10);
  };

  const recent = state => state.transactions
    .filter(tx => !isFuture(tx))
    .slice()
    .sort((a, b) => String(b.date || '').localeCompare(String(a.date || '')))
    .slice(0, 6);

  const accountCards = state => state.accounts.slice(0, 4).map(account => {
    const label = account.name || account.title || account.bank || 'Carteira';
    const type = account.type || account.kind || 'conta';
    const balance = Number(account.balance || account.initialBalance || 0);
    const isCredit = /credit|cart/i.test(`${type} ${label}`);
    return { label, type: isCredit ? 'Cartão' : 'Conta', balance, isCredit };
  });

  const donutStyle = categories => {
    const total = categories.reduce((sum, item) => sum + item.amount, 0);
    if (!total) return 'background: conic-gradient(#111 0 100%)';
    let cursor = 0;
    const segments = categories.slice(0, 6).map((item, idx) => {
      const start = cursor;
      const size = (item.amount / total) * 100;
      cursor += size;
      const color = item.meta.col || ['#111111', '#2f2f2f', '#555555', '#f3c63b', '#7c6a00', '#fff1a8'][idx];
      return `${color} ${start}% ${cursor}%`;
    });
    if (cursor < 100) segments.push(`#ece7d9 ${cursor}% 100%`);
    return `background: conic-gradient(${segments.join(', ')})`;
  };

  const budgetRows = (state, categories) => {
    if (state.budgets.length) {
      return state.budgets.slice(0, 4).map(budget => {
        const spent = categories.find(cat => cat.name === budget.category)?.amount || 0;
        const limit = Number(budget.limit || 0);
        return { name: budget.category, spent, limit, pct: limit ? Math.min(100, spent / limit * 100) : 0 };
      });
    }
    return categories.slice(0, 4).map(cat => ({ name: cat.name, spent: cat.amount, limit: 0, pct: 0 }));
  };

  const renderRows = rows => rows.length
    ? rows.map(row => {
      const meta = catMeta(row.name);
      const subtitle = row.limit ? `${money(row.spent)} de ${money(row.limit)}` : `${money(row.spent)} este mês`;
      return `
        <button class="next-budget-row" type="button" onclick="showPage('budget')">
          <span class="next-row-icon" style="--row-color:${esc(meta.col || '#111')}">${esc(meta.ico || '•')}</span>
          <span>
            <strong>${esc(row.name || 'Categoria')}</strong>
            <small>${subtitle}</small>
          </span>
          <i style="--p:${Math.round(row.pct)}%"></i>
        </button>
      `;
    }).join('')
    : `<div class="next-empty">Defina seus orçamentos para ver o consumo por categoria aqui.</div>`;

  const renderUpcoming = items => items.length
    ? items.map(item => `
      <button class="next-list-line" type="button" onclick="showPage('future')">
        <span class="next-date-chip">${esc(fmtDate(item.date))}</span>
        <span class="next-list-copy">
          <strong>${esc(item.title)}</strong>
          <small>${esc(item.category)} · ${esc(item.status)}</small>
        </span>
        <b>${money(item.amount)}</b>
      </button>
    `).join('')
    : `<div class="next-empty">Nenhuma conta futura registrada ainda.</div>`;

  const renderRecent = items => items.length
    ? items.map(tx => `
      <button class="next-list-line ${tx.type === 'income' ? 'is-income' : ''}" type="button" onclick="openModal('${esc(tx.id)}')">
        <span class="next-method-dot">${tx.type === 'income' ? '+' : '−'}</span>
        <span class="next-list-copy">
          <strong>${esc(tx.desc || tx.description || 'Transação')}</strong>
          <small>${esc(fmtDate(tx.date))} · ${esc(tx.category || 'Sem categoria')}</small>
        </span>
        <b>${tx.type === 'income' ? '+' : '-'}${money(tx.amount)}</b>
      </button>
    `).join('')
    : `<div class="next-empty">Registre o primeiro gasto para a home ganhar vida.</div>`;

  const renderAccounts = items => items.length
    ? items.map(item => `
      <button class="next-wallet" type="button" onclick="showPage('accounts')">
        <span>${item.isCredit ? '💳' : '◼'}</span>
        <strong>${esc(item.label)}</strong>
        <small>${esc(item.type)}</small>
        <b>${rawMoney(item.balance)}</b>
      </button>
    `).join('')
    : `<button class="next-wallet next-wallet-empty" type="button" onclick="showPage('accounts')"><span>＋</span><strong>Criar carteira</strong><small>Pix, débito, crédito ou dinheiro</small></button>`;

  const renderHome = () => {
    const page = document.getElementById('page-dashboard');
    if (!page) return;

    const state = safeState();
    const signature = signatureOf(state);
    const active = page.classList.contains('active');
    if (signature === lastSignature && document.getElementById(HOME_ID)) return;
    lastSignature = signature;

    const monthTx = state.transactions.filter(tx => inCurrentMonth(tx) && !isFuture(tx));
    const income = monthTx.filter(tx => tx.type === 'income').reduce((sum, tx) => sum + Number(tx.amount || 0), 0);
    const expense = monthTx.filter(tx => tx.type === 'expense' && !tx.paid).reduce((sum, tx) => sum + Number(tx.amount || 0), 0);
    const monthlyBase = state.income || income || state.budgets.reduce((sum, budget) => sum + Number(budget.limit || 0), 0);
    const remaining = monthlyBase - expense;
    const usedPct = monthlyBase ? Math.min(100, Math.max(0, expense / monthlyBase * 100)) : 0;
    const categories = categorySummary(state);
    const catTotal = categories.reduce((sum, item) => sum + item.amount, 0);
    const topCat = categories[0];
    const budgetItems = budgetRows(state, categories);
    const upcomingItems = upcoming(state);
    const recentItems = recent(state);
    const wallets = accountCards(state);

    document.body.classList.toggle('next-shell-ready', active);

    let home = document.getElementById(HOME_ID);
    if (!home) {
      home = document.createElement('section');
      home.id = HOME_ID;
      home.className = 'next-home';
      page.appendChild(home);
    }

    home.innerHTML = `
      <header class="next-hero">
        <div>
          <span class="next-kicker">Next · ${esc(monthName.format(new Date()))}</span>
          <h1>Orçamento. Contas. Controle.</h1>
          <p>Uma tela de decisão rápida para lançar gastos, acompanhar categorias e enxergar o mês sem ruído.</p>
        </div>
        <div class="next-hero-actions">
          <button class="next-icon-btn" type="button" onclick="openImportCenter()" title="Importar">📥</button>
          <button class="next-primary" type="button" onclick="openModal()">+ Gasto rápido</button>
        </div>
      </header>

      <nav class="next-nav-rail" aria-label="Navegação Next">
        <button type="button" class="active" onclick="showPage('dashboard')"><span>▣</span>Início</button>
        <button type="button" onclick="showPage('transactions')"><span>−</span>Gastos</button>
        <button type="button" onclick="showPage('future')"><span>⏱</span>Contas</button>
        <button type="button" onclick="showPage('budget')"><span>%</span>Orçamento</button>
        <button type="button" onclick="showPage('accounts')"><span>◼</span>Carteiras</button>
        <button type="button" onclick="showPage('settings')"><span>⚙</span>Ajustes</button>
      </nav>

      <div class="next-main-grid">
        <article class="next-budget-card">
          <div class="next-card-top">
            <span>Orçamento mensal</span>
            <button type="button" onclick="showPage('budget')">Editar</button>
          </div>
          <strong class="next-money">${money(remaining)}</strong>
          <p>${remaining >= 0 ? 'disponível para o restante do mês' : 'acima do plano do mês'}</p>
          <div class="next-progress"><i style="width:${usedPct}%"></i></div>
          <div class="next-budget-metrics">
            <span><b>${money(monthlyBase)}</b><small>Base</small></span>
            <span><b>${money(expense)}</b><small>Gastos</small></span>
            <span><b>${Math.round(usedPct)}%</b><small>Usado</small></span>
          </div>
        </article>

        <article class="next-black-card">
          <div class="next-dots"><i></i><i></i><i></i></div>
          <span>Resumo financeiro</span>
          <strong>${money(income - expense)}</strong>
          <div class="next-mini-stack">
            <div><small>Receitas</small><b>${money(income)}</b></div>
            <div><small>Despesas</small><b>${money(expense)}</b></div>
            <div><small>Movimentos</small><b>${monthTx.length}</b></div>
          </div>
        </article>
      </div>

      <nav class="next-action-grid" aria-label="Ações rápidas">
        <button type="button" onclick="openModal()"><span>−</span><strong>Gasto</strong><small>registrar em segundos</small></button>
        <button type="button" onclick="openModal(null,true)"><span>＋</span><strong>Receita</strong><small>entrada ou previsão</small></button>
        <button type="button" onclick="openDueModal()"><span>⏱</span><strong>Conta</strong><small>vencimento fixo</small></button>
        <button type="button" onclick="showPage('transactions')"><span>⌕</span><strong>Histórico</strong><small>filtrar e revisar</small></button>
      </nav>

      <div class="next-content-grid">
        <section class="next-card next-analysis">
          <div class="next-section-head">
            <div><span>Análise</span><h2>Categorias em tempo real</h2></div>
            <button type="button" onclick="showPage('transactions')">Ver tudo</button>
          </div>
          <div class="next-analysis-body">
            <div class="next-donut" style="${donutStyle(categories)}"><span>${catTotal ? Math.round((topCat?.amount || 0) / catTotal * 100) : 0}%</span></div>
            <div class="next-legend">
              ${categories.slice(0, 5).map(item => `
                <button type="button" onclick="showPage('transactions')">
                  <i style="background:${esc(item.meta.col || '#111')}"></i>
                  <span>${esc(item.name)}</span>
                  <b>${money(item.amount)}</b>
                </button>
              `).join('') || '<div class="next-empty">Sem gastos neste mês.</div>'}
            </div>
          </div>
        </section>

        <section class="next-card">
          <div class="next-section-head">
            <div><span>Plano</span><h2>Limites por categoria</h2></div>
            <button type="button" onclick="showPage('budget')">Orçamentos</button>
          </div>
          <div class="next-budget-list">${renderRows(budgetItems)}</div>
        </section>

        <section class="next-card">
          <div class="next-section-head">
            <div><span>Agenda</span><h2>Próximos vencimentos</h2></div>
            <button type="button" onclick="showPage('future')">Vencimentos</button>
          </div>
          <div class="next-list">${renderUpcoming(upcomingItems)}</div>
        </section>

        <section class="next-card">
          <div class="next-section-head">
            <div><span>Histórico</span><h2>Últimos movimentos</h2></div>
            <button type="button" onclick="showPage('transactions')">Transações</button>
          </div>
          <div class="next-list">${renderRecent(recentItems)}</div>
        </section>
      </div>

      <section class="next-wallet-strip">
        <div>
          <span>Carteiras e meios de pagamento</span>
          <h2>Pix, débito, crédito e dinheiro num só painel</h2>
        </div>
        <div class="next-wallets">${renderAccounts(wallets)}</div>
      </section>
    `;
  };

  const scheduleRender = () => {
    clearTimeout(renderTimer);
    renderTimer = setTimeout(renderHome, 60);
  };

  const patchShowPage = () => {
    try {
      if (typeof showPage !== 'function' || showPage.__nextPatched) return;
      const original = showPage;
      showPage = function nextShowPage(...args) {
        const result = original.apply(this, args);
        scheduleRender();
        return result;
      };
      showPage.__nextPatched = true;
    } catch {}
  };

  const init = () => {
    patchShowPage();
    scheduleRender();
    setInterval(() => {
      patchShowPage();
      scheduleRender();
    }, 1800);

    const observer = new MutationObserver(scheduleRender);
    observer.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['class'] });
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init, { once: true });
  } else {
    init();
  }
})();
