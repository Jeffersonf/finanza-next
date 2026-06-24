// Componentes e widgets renderizados
// Extraido de index.html para organizar o frontend sem mudar a arquitetura global.

function widgetCards() {
  const txM = getMonthTx(curDt);
  const inc = txM.filter(t => t.type === 'income' && !isFut(t.date) && !t.paid).reduce((s, t) => s + t.amount, 0);
  const exp = txM.filter(t => t.type === 'expense' && !isFut(t.date) && !t.paid).reduce((s, t) => s + t.amount, 0);
  const fut = S.transactions.filter(t => t.type === 'expense' && isFut(t.date) && !t.paid).reduce((s, t) => s + t.amount, 0);
  const income = monthlyIncomeCents / 100;
  const base = income || inc;
  const remaining = base - exp;
  const now = new Date();
  const sameMonth = curDt.getMonth() === now.getMonth() && curDt.getFullYear() === now.getFullYear();
  const daysInMonth = new Date(curDt.getFullYear(), curDt.getMonth() + 1, 0).getDate();
  const daysLeft = sameMonth ? Math.max(daysInMonth - now.getDate() + 1, 1) : daysInMonth;
  const safeDay = base ? Math.max(remaining / daysLeft, 0) : 0;
  const burnPct = base ? Math.min(Math.round(exp / base * 100), 999) : 0;
  window._dashInc = inc;
  window._dashExp = exp;
  return `<div class="g4 summary-grid dash-section">
    <div class="sc sc-glow-ac2" style="cursor:pointer" onclick="showPage('settings')"><span class="ci">💼</span><div class="cl">Salário base</div><div class="cv ${base ? 'pos' : 'neu'}">${base ? fmt(base) : 'Definir'}</div><div class="cc">${income ? 'renda mensal configurada' : 'toque para configurar'}</div></div>
    <div class="sc sc-glow-dan" style="cursor:pointer" onclick="showPage('transactions')"><span class="ci">⬇️</span><div class="cl">Gasto no mês</div><div class="cv neg">${fmt(exp)}</div><div class="cc dn">${base ? burnPct + '% do salário' : 'sem salário definido'}</div></div>
    <div class="sc sc-glow-ac" style="cursor:pointer" onclick="showPage('transactions')"><span class="ci">🧭</span><div class="cl">Seguro por dia</div><div class="cv ${safeDay > 0 ? 'pos' : 'neg'}">${fmt(safeDay)}</div><div class="cc">${daysLeft} dia${daysLeft === 1 ? '' : 's'} até fechar</div></div>
    <div class="sc sc-glow-ac" style="cursor:pointer" onclick="showPage('transactions')"><span class="ci">🌱</span><div class="cl">Sobra projetada</div><div class="cv ${remaining >= 0 ? 'pos' : 'neg'}">${fmt(remaining)}</div><div class="cc">${fmt(inc)} recebido no mês</div></div>
    <div class="sc sc-glow-fut" style="cursor:pointer" onclick="showPage('future')"><span class="ci">🔮</span><div class="cl">A pagar</div><div class="cv fut">${fmt(fut)}</div><div class="cc" style="color:var(--fut)">ver contas futuras</div></div>
  </div>`;
}

function widgetQuickActions() {
  const dueSoon = dueOccurrences(offD(new Date(), 7)).length;
  const pending = S.transactions.filter(t => t.pending).length;
  const limit = Number((widgetFilters?.quickactions?.limit) || 6);
  const actions = [
    `<button class="quick-action-card primary" onclick="openModal()"><strong>+ Transação</strong><small>Lançamento manual</small></button>`,
    `<button class="quick-action-card" onclick="toggleQA()"><strong>+ Rápido</strong><small>Teclado numérico</small></button>`,
    `<button class="quick-action-card" onclick="openDueModal()"><strong>+ Vencimento</strong><small>${dueSoon} nos próximos 7 dias</small></button>`,
    `<button class="quick-action-card" onclick="openCarEntry('fuel')"><strong>+ Abastecimento</strong><small>Módulo do carro</small></button>`,
    `<button class="quick-action-card" onclick="openSrch()"><strong>Buscar</strong><small>Ctrl+K • tudo no app</small></button>`,
    `<button class="quick-action-card" onclick="showPage('transactions')"><strong>Pendentes</strong><small>${pending} para revisar</small></button>`
  ];
  return `<div class="quick-actions-widget dash-section">
    <div class="bh"><div><div class="ct">Ações rápidas</div><div class="cs">Menos cliques para o que você faz toda hora</div></div></div>
    <div class="quick-actions-grid">
      ${actions.slice(0, limit).join('')}
    </div>
  </div>`;
}

function widgetMiniStats() {
  const txM = getMonthTx(curDt).filter(t => !isFut(t.date) && !t.paid && t.type === 'expense');
  if (!txM.length) return '';
  const total = txM.reduce((s, t) => s + t.amount, 0);
  const dias = new Set(txM.map(t => t.date)).size;
  const media = total / Math.max(dias, 1);
  const maior = Math.max(...txM.map(t => t.amount));
  const diasMes = new Date(curDt.getFullYear(), curDt.getMonth() + 1, 0).getDate();
  const diasRest = diasMes - curDt.getDate();
  return `<div class="mini-stats dash-section">
    <div class="mini-stat"><div class="ms-ico">📊</div><div class="ms-val" style="color:var(--warn)">${fmt(media)}</div><div class="ms-lbl">Média/dia</div></div>
    <div class="mini-stat"><div class="ms-ico">🔺</div><div class="ms-val" style="color:var(--dan)">${fmt(maior)}</div><div class="ms-lbl">Maior gasto</div></div>
    <div class="mini-stat"><div class="ms-ico">📅</div><div class="ms-val" style="color:var(--ac2)">${diasRest}</div><div class="ms-lbl">Dias restantes</div></div>
  </div>`;
}

function widgetAccounts() {
  if (!S.accounts.length) return '';
  const limit = (widgetFilters?.accounts?.limit) || '5';
  const accounts = limit === 'all' ? S.accounts : S.accounts.slice(0, Number(limit));
  return `<div class="box dash-section">
    <div class="bh"><div class="ct">Contas</div><button class="btn btn-g btn-sm" onclick="showPage('accounts')">Ver →</button></div>
    <div style="display:flex;flex-direction:column;gap:8px;">
      ${accounts.map(a => {
        const bal = getAccBal(a.id);
        const y = getAccYield(a, 1);
        return `<div style="display:flex;align-items:center;justify-content:space-between;padding:8px 4px;border-bottom:1px solid var(--bd2)">
          <div style="display:flex;align-items:center;gap:8px"><span style="font-size:18px">${a.icon}</span><span style="font-size:13px;font-weight:500">${a.name}</span>${y > 0 ? `<span class="yield-pill">+${fmt(y)}</span>` : ''}</div>
          <span style="font-family:var(--font-money);font-weight:700;font-size:14px;color:${bal >= 0 ? 'var(--ac)' : 'var(--dan)'}">${fmt(bal)}</span>
        </div>`;
      }).join('')}
    </div>
  </div>`;
}

function widgetGoals() {
  const limit = Number((widgetFilters?.goals?.limit) || 4);
  const goals = S.goals.filter(g => (g.current / g.target) < 1).slice(0, limit);
  if (!goals.length) return '';
  return `<div class="goals-widget dash-section">
    <div class="bh"><div class="ct">🏆 Metas</div><button class="btn btn-g btn-sm" onclick="showPage('goals')">Ver todas →</button></div>
    <div class="goals-widget-list">
      ${goals.map(g => {
        const pct = Math.min((g.current / g.target) * 100, 100);
        const col = pct >= 80 ? 'var(--ac)' : pct >= 50 ? 'var(--ac2)' : 'var(--warn)';
        return `<div class="gw-item">
          <span class="gw-icon">${g.icon}</span>
          <div class="gw-info">
            <div class="gw-name">${g.name}</div>
            <div class="gw-bar"><div class="gw-fill" style="width:${pct}%;background:${col}"></div></div>
          </div>
          <span class="gw-pct" style="color:${col}">${Math.round(pct)}%</span>
        </div>`;
      }).join('')}
    </div>
  </div>`;
}

function widgetBudgets() {
  const limit = Number((widgetFilters?.budgets?.limit) || 6);
  const now = new Date();
  const buds = S.budgets.map(b => {
    const spent = S.transactions.filter(t => {
      if (t.type !== 'expense' || isFut(t.date) || t.paid) return false;
      const d = new Date(t.date + 'T12:00:00');
      return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear() && t.category === b.category;
    }).reduce((s, t) => s + t.amount, 0);
    return { ...b, spent, pct: Math.min((spent / b.limit) * 100, 100) };
  }).sort((a, b) => b.pct - a.pct).slice(0, limit);
  if (!buds.length) return '';
  return `<div class="budget-widget dash-section">
    <div class="bh"><div class="ct">🎯 Orçamentos</div><button class="btn btn-g btn-sm" onclick="showPage('budget')">Ver todos →</button></div>
    <div class="bw-list">
      ${buds.map(b => {
        const cat = getCat(b.category);
        const col = b.pct >= 100 ? 'var(--dan)' : b.pct >= 80 ? 'var(--warn)' : 'var(--ac)';
        return `<div class="bw-item">
          <span class="bw-cat">${cat.ico}</span>
          <div class="bw-info">
            <div class="bw-top">
              <span class="bw-name">${b.category}</span>
              <span class="bw-vals">${fmt(b.spent)} / ${fmt(b.limit)}</span>
            </div>
            <div class="bw-bar"><div class="bw-fill" style="width:${b.pct}%;background:${col}"></div></div>
          </div>
        </div>`;
      }).join('')}
    </div>
  </div>`;
}

function widgetCharts() {
  return `<div class="cr dash-section">
    <div class="cc-box">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:2px;">
        <div class="ct">Fluxo de Caixa</div>
        <div style="display:flex;gap:3px;">
          <button class="vbtn ${chartMode === 'bars' ? 'active' : ''}" id="cBars" onclick="setChartMode('bars')" title="Barras">▦</button>
          <button class="vbtn ${chartMode === 'line' ? 'active' : ''}" id="cLine" onclick="setChartMode('line')" title="Acumulado">📈</button>
        </div>
      </div>
      <div class="cs" id="chartSub">${chartMode === 'bars' ? 'Receitas vs despesas — últimos 6 meses' : 'Saldo acumulado — últimos 6 meses'}</div>
      <canvas id="flowChart"></canvas>
    </div>
    <div class="cc-box"><div class="ct">Categorias</div><div class="cs">Clique para filtrar</div><canvas id="catChart" style="cursor:pointer"></canvas></div>
  </div>`;
}

function widgetRecent() {
  const type = (widgetFilters?.recent?.type) || 'all';
  const limit = Number((widgetFilters?.recent?.limit) || 6);
  const sorted = [...S.transactions]
    .filter(t => !isFut(t.date) && !t.paid && (type === 'all' || t.type === type))
    .sort((a, b) => b.date.localeCompare(a.date))
    .slice(0, limit);
  return `<div class="box dash-section">
    <div class="bh"><div class="ct">Últimas transações</div><button class="btn btn-g btn-sm" onclick="showPage('transactions')">Ver todas</button></div>
    <div class="recent-list" id="recList">${sorted.length ? sorted.map(recentTxHTML).join('') : '<div class="empty"><span class="ei">💸</span><p>Nenhuma transação ainda.</p></div>'}</div>
  </div>`;
}

function recentTxHTML(tx) {
  const cat = getCat(tx.category);
  const acc = S.accounts.find(a => a.id === tx.accountId);
  const sign = tx.type === 'income' ? '+' : '-';
  const tone = tx.type === 'income' ? 'income' : 'expense';
  return `<div class="recent-tx" onclick="showPage('transactions');setTimeout(()=>hlTx('${tx.id}'),250)">
    <div class="recent-ico" style="background:${cat.col}20;color:${cat.col}">${cat.ico}</div>
    <div class="recent-main">
      <div class="recent-name">${tx.desc}</div>
      <div class="recent-meta"><span>${tx.category}</span><span>${fmtD(tx.date)}</span>${acc ? `<span>${acc.icon}</span>` : ''}</div>
    </div>
    <div class="recent-amt ${tone}">${sign}${fmt(tx.amount)}</div>
  </div>`;
}

function widgetShoppingDash() {
  let data = { lists: [], items: [] };
  try { data = JSON.parse(localStorage.getItem('fz_shopping') || '{}'); } catch {}
  const lists = data.lists || [];
  const items = data.items || [];
  const limit = Number((widgetFilters?.shopping?.limit) || 5);
  const pending = items.filter(i => !i.bought);
  const list = lists[0];
  const listItems = items.filter(i => i.listId === list?.id);
  const bought = listItems.filter(i => i.bought).length;
  const pct = listItems.length ? Math.round(bought / listItems.length * 100) : 0;
  return `<div class="box dash-section">
    <div class="bh"><div><div class="ct">🛒 ${list?.ico || '🛒'} ${list?.name || 'Lista de Compras'}</div><div class="cs">${pending.length} pendente${pending.length !== 1 ? 's' : ''}</div></div><button class="btn btn-g btn-sm" onclick="showPage('shopping')">Abrir →</button></div>
    <div class="sl-prog-bar" style="margin-bottom:12px"><div class="sl-prog-fill" style="width:${pct}%"></div></div>
    ${pending.slice(0, limit).map(i => `<div style="display:flex;align-items:center;gap:8px;padding:7px 2px;border-bottom:1px solid var(--bd2)"><div style="width:16px;height:16px;border-radius:5px;border:2px solid var(--bd);flex-shrink:0"></div><span style="font-size:13px;flex:1;min-width:0">${i.name}</span>${i.qty ? `<span style="font-size:11px;color:var(--mt)">${i.qty}</span>` : ''}</div>`).join('') || '<div class="empty" style="padding:18px"><p>Lista sem pendências.</p></div>'}
  </div>`;
}

function widgetSaveRate() {
  const monthsCount = Number((widgetFilters?.saverate?.months) || 3);
  const months = [];
  for (let i = monthsCount - 1; i >= 0; i--) {
    const d = new Date(curDt.getFullYear(), curDt.getMonth() - i, 1);
    const txs = getMonthTx(d).filter(t => !isFut(t.date) && !t.paid);
    const inc = txs.filter(t => t.type === 'income').reduce((s, t) => s + t.amount, 0);
    const exp = txs.filter(t => t.type === 'expense').reduce((s, t) => s + t.amount, 0);
    if (inc > 0) months.push({ label: ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'][d.getMonth()], rate: Math.round((1 - exp / inc) * 100), inc, exp });
  }
  if (!months.length) return '';
  const cur = months[months.length - 1];
  const col = cur.rate >= 20 ? 'var(--grn)' : cur.rate >= 0 ? 'var(--warn)' : 'var(--dan)';
  return `<div class="box dash-section"><div class="bh"><div class="ct">💹 Taxa de economia</div></div><div style="display:flex;align-items:center;gap:16px;flex-wrap:wrap"><div style="text-align:center"><div style="font-family:var(--font-money);font-size:42px;font-weight:800;color:${col};letter-spacing:0">${cur.rate}%</div><div style="font-size:11px;color:var(--mt)">este mês</div></div><div style="flex:1;min-width:120px">${months.map(m => { const c = m.rate >= 20 ? 'var(--grn)' : m.rate >= 0 ? 'var(--warn)' : 'var(--dan)'; return `<div style="display:flex;align-items:center;gap:8px;margin-bottom:6px"><span style="font-size:11px;color:var(--mt);width:28px">${m.label}</span><div style="flex:1;height:8px;background:var(--sf2);border-radius:99px;overflow:hidden"><div style="width:${Math.min(Math.abs(m.rate), 100)}%;height:100%;background:${c};border-radius:99px"></div></div><span style="font-size:11px;font-weight:700;color:${c};width:34px;text-align:right">${m.rate}%</span></div>`; }).join('')}</div></div></div>`;
}

function widgetBarCats() {
  const limit = Number((widgetFilters?.barcats?.limit) || 6);
  const txM = getMonthTx(curDt).filter(t => t.type === 'expense' && !isFut(t.date) && !t.paid);
  const cats = {};
  txM.forEach(t => cats[t.category] = (cats[t.category] || 0) + t.amount);
  const sorted = Object.entries(cats).sort((a, b) => b[1] - a[1]).slice(0, limit);
  if (!sorted.length) return '';
  const max = sorted[0][1];
  return `<div class="box dash-section"><div class="bh"><div><div class="ct">📉 Top categorias</div><div class="cs">Maiores gastos do mês</div></div></div><div style="display:flex;flex-direction:column;gap:10px">${sorted.map(([cat, val], i) => { const c = getCat(cat); const pct = (val / max * 100).toFixed(0); const cols = ['var(--dan)', 'var(--warn)', 'var(--ac)', 'var(--ac2)', 'var(--fut)', 'var(--grn)']; const col = cols[i] || 'var(--mt)'; return `<div><div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px"><span style="font-size:13px">${c.ico} ${cat}</span><span style="font-family:var(--font-money);font-size:13px;font-weight:700;color:${col}">${fmt(val)}</span></div><div style="height:6px;background:var(--sf2);border-radius:99px;overflow:hidden"><div style="width:${pct}%;height:100%;background:${col};border-radius:99px"></div></div></div>`; }).join('')}</div></div>`;
}

function widgetVehicles() {
  loadCar();
  const vehicles = carVehicles();
  if (!vehicles.length) return '';
  const period = (widgetFilters?.vehicles?.period) || '90d';
  const limit = Number((widgetFilters?.vehicles?.limit) || 3);
  const previousFilter = { ...carFilters };
  const activeId = carState.activeVehicleId || vehicles[0]?.id || 'all';
  carFilters.vehicle = activeId || 'all';
  carFilters.period = period;
  carFilters.type = 'all';
  carFilters.kind = 'all';
  carFilters.query = '';
  carFilters.sort = 'date_desc';
  const stats = carStats();
  const maint = carMaintenanceInsights(stats.events);
  Object.assign(carFilters, previousFilter);

  const activeVehicle = carVehicle(activeId);
  const recent = [...carState.events]
    .filter(e => e.vehicleId === activeId)
    .sort((a, b) => b.date.localeCompare(a.date) || b.createdAt - a.createdAt)
    .slice(0, limit);
  const dueTone = maint.upcomingStatus === 'urgent'
    ? 'var(--dan)'
    : maint.upcomingStatus === 'warn'
      ? 'var(--warn)'
      : 'var(--ac)';
  const dueText = maint.kmLeft !== null
    ? `${Math.round(maint.kmLeft).toLocaleString('pt-BR')} km restantes`
    : maint.nextOilDate
      ? `proxima revisao ate ${fmtD(maint.nextOilDate)}`
      : 'cadastre uma troca de oleo ou revisao';
  const headerMeta = [
    activeVehicle?.model,
    activeVehicle?.plate,
    activeVehicle?.odometer ? `${Math.round(activeVehicle.odometer).toLocaleString('pt-BR')} km` : ''
  ].filter(Boolean).join(' • ');

  return `<div class="vehicle-widget dash-section">
    <div class="bh">
      <div>
        <div class="ct">Veiculos</div>
        <div class="cs">${vehicles.length} veiculo${vehicles.length !== 1 ? 's' : ''} • foco no ativo</div>
      </div>
      <button class="btn btn-g btn-sm" onclick="showPage('car')">Abrir modulo</button>
    </div>
    <div class="vehicle-widget-hero" onclick="showPage('car')">
      <div>
        <div class="vehicle-widget-name">${esc(activeVehicle?.name || 'Meu carro')}</div>
        <div class="vehicle-widget-meta">${esc(headerMeta || 'Consumo, manutencao e historico')}</div>
      </div>
      <div class="vehicle-widget-kpi">
        <span class="vehicle-widget-kpi-label">Gasto ${period === '30d' ? '30 dias' : period === 'year' ? 'ano' : period === 'all' ? 'total' : '90 dias'}</span>
        <strong>${fmt(stats.total)}</strong>
      </div>
    </div>
    <div class="vehicle-widget-grid">
      <div class="vehicle-widget-card">
        <div class="vehicle-widget-label">Consumo medio</div>
        <div class="vehicle-widget-value" style="color:var(--ac2)">${stats.kmPerLiter ? `${stats.kmPerLiter.toFixed(1).replace('.', ',')} km/l` : '—'}</div>
        <div class="vehicle-widget-note">${stats.liters ? `${stats.liters.toLocaleString('pt-BR', { maximumFractionDigits: 1 })} L medidos` : 'precisa de pelo menos 2 abastecimentos'}</div>
      </div>
      <div class="vehicle-widget-card">
        <div class="vehicle-widget-label">Custo por km</div>
        <div class="vehicle-widget-value" style="color:var(--warn)">${stats.costPerKm ? fmt(stats.costPerKm) : '—'}</div>
        <div class="vehicle-widget-note">${stats.distance ? `${Math.round(stats.distance).toLocaleString('pt-BR')} km calculados` : 'sem distancia suficiente no periodo'}</div>
      </div>
      <div class="vehicle-widget-card">
        <div class="vehicle-widget-label">Proxima manutencao</div>
        <div class="vehicle-widget-value" style="color:${dueTone}">${maint.nextOilKm ? `${Math.round(maint.nextOilKm).toLocaleString('pt-BR')} km` : 'Revisar'}</div>
        <div class="vehicle-widget-note">${esc(dueText)}</div>
      </div>
    </div>
    <div class="vehicle-widget-list">
      ${recent.length ? recent.map(e => {
        const icon = e.type === 'fuel' ? '⛽' : '🧰';
        const title = e.type === 'fuel' ? (e.fuelType || 'Abastecimento') : (e.title || carExpenseLabel(e.category));
        const meta = [fmtD(e.date), e.odometer ? `${Math.round(e.odometer).toLocaleString('pt-BR')} km` : '', e.note].filter(Boolean).join(' • ');
        return `<button class="vehicle-widget-row" onclick="openSearchTarget('car-event','${e.id}','car')">
          <span class="vehicle-widget-row-ico">${icon}</span>
          <span class="vehicle-widget-row-main">
            <strong>${esc(title)}</strong>
            <small>${esc(meta || 'Sem detalhes')}</small>
          </span>
          <span class="vehicle-widget-row-amt">${fmt(e.amount)}</span>
        </button>`;
      }).join('') : `<div class="vehicle-widget-empty">Adicione abastecimentos ou despesas para montar os insights do veiculo.</div>`}
    </div>
  </div>`;
}
