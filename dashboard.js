function dashboardWidgetRenderers(){
  return {
    cards:widgetCards,
    quickactions:widgetQuickActions,
    workbench:widgetWorkbench,
    ministats:widgetMiniStats,
    dailyweek:widgetDailyWeek,
    dailymonth:widgetDailyMonth,
    dailyyear:widgetDailyYear,
    accounts:widgetAccounts,
    commitments:widgetCommitments,
    renewals:widgetRenewals,
    vehicles:widgetVehicles,
    shopping:widgetShoppingDash,
    compare:()=>'<div class="dash-section" id="monthCompare"></div>',
    projection:()=>'<div class="dash-section" id="projCard"></div>',
    weekly:()=>'<div class="dash-section" id="wsum"></div>',
    anomaly:()=>'<div class="dash-section" id="anom"></div>',
    budalerts:()=>'<div class="dash-section" id="budAlerts"></div>',
    saverate:widgetSaveRate,
    goals:widgetGoals,
    budgets:widgetBudgets,
    barcats:widgetBarCats,
    charts:widgetCharts,
    recent:widgetRecent
  };
}
function dashboardVisibleWidgetIds(){
  return widgetOrder.filter(id=>isWidgetOn(id)&&dashboardWidgetRenderers()[id]);
}
function dashboardWidgetMarkup(id,isDark=document.documentElement.dataset.theme==='dark'){
  const renderers=dashboardWidgetRenderers();
  const render=renderers[id];
  if(!render||!isWidgetOn(id))return '';
  const html=render();
  if(!html)return '';
  const menuOpen=activeWidgetMenuId===id;
  const pinned=isPinnedWidget(id);
  const menu=`<div class="widget-menu-shell"><button class="widget-menu-btn" onclick="event.stopPropagation();toggleWidgetMenu('${id}')" aria-label="Abrir menu do widget" title="${pinned?'Ajustar atalho fixo':'Ajustar widget'}">⋯</button>${menuOpen?`<div class="widget-menu-panel" onclick="event.stopPropagation()">${renderWidgetMenuPanel(id)}</div>`:''}</div>`;
  return `<div class="dash-section-wrap ${pinned?'dash-section-wrap-pinned':''}" data-widget-id="${id}" data-theme-snapshot="${isDark?'dark':'light'}">${menu}${html}</div>`;
}
function dashboardWidgetPostRender(id,isDark=document.documentElement.dataset.theme==='dark'){
  if(id==='compare')renderMonthCompare();
  if(id==='projection')renderProjection();
  if(id==='weekly'||id==='anomaly')renderWeekly();
  if(id==='budalerts')renderBudAlerts();
  if(id==='charts')renderCharts(isDark);
}
function replaceDashboardWidget(id){
  const container=document.getElementById('dashWidgets');
  if(!container||!isDashboardActive())return;
  const isDark=document.documentElement.dataset.theme==='dark';
  const markup=dashboardWidgetMarkup(id,isDark);
  const existing=container.querySelector(`.dash-section-wrap[data-widget-id="${id}"]`);
  if(!markup){
    if(existing)existing.remove();
    return;
  }
  const temp=document.createElement('div');
  temp.innerHTML=markup.trim();
  const nextId=dashboardVisibleWidgetIds().slice(dashboardVisibleWidgetIds().indexOf(id)+1).find(candidate=>container.querySelector(`.dash-section-wrap[data-widget-id="${candidate}"]`));
  const nextNode=nextId?container.querySelector(`.dash-section-wrap[data-widget-id="${nextId}"]`):null;
  const replacement=temp.firstElementChild;
  if(existing)existing.replaceWith(replacement);
  else if(nextNode)container.insertBefore(replacement,nextNode);
  else container.appendChild(replacement);
  dashboardWidgetPostRender(id,isDark);
}
function rerenderDashboardWidgets(ids=[]){
  [...new Set(ids.filter(Boolean))].forEach(id=>replaceDashboardWidget(id));
}
function widgetSelectControl(widget,key,label,options,fallback){
  const current=String(getWidgetFilter(widget,key,fallback));
  return `<label class="widget-menu-field"><span>${label}</span><select class="widget-filter" onchange="setWidgetFilter('${widget}','${key}',this.value)">${options.map(opt=>`<option value="${esc(opt.value)}" ${current===String(opt.value)?'selected':''}>${esc(opt.label)}</option>`).join('')}</select></label>`;
}
function widgetMenuAction(label,fn,tone='ghost'){
  const cls=tone==='danger'?'btn btn-d btn-sm':'btn btn-g btn-sm';
  return `<button class="${cls}" onclick="${fn}">${label}</button>`;
}
function renderWidgetMenuPanel(id){
  const pinned=isPinnedWidget(id);
  const sections=[];
  sections.push(`<div class="widget-menu-head"><strong>${esc(widgetById(id)?.name||'Widget')}</strong><small>Ajuste os dados mostrados e a posicao deste bloco.</small></div>`);
  switch(id){
    case 'cards':
      sections.push(widgetSelectControl('cards','future','Cartao futuro',[{value:'on',label:'Mostrar a pagar'},{value:'off',label:'Ocultar a pagar'}],'on'));
      break;
    case 'quickactions':
      sections.push(widgetSelectControl('quickactions','limit','Atalhos',[{value:'4',label:'4 atalhos'},{value:'6',label:'6 atalhos'}],'6'));
      break;
    case 'workbench':
      sections.push(widgetSelectControl('workbench','layout','Visual',[{value:'grid',label:'Grade completa'},{value:'compact',label:'Compacto'}],'grid'));
      break;
    case 'charts':
      sections.push(widgetSelectControl('charts','mode','Grafico principal',[{value:'bars',label:'Barras'},{value:'line',label:'Linha acumulada'}],'bars'));
      break;
    case 'accounts':
      sections.push(widgetSelectControl('accounts','limit','Contas',[{value:'3',label:'3 contas'},{value:'5',label:'5 contas'},{value:'all',label:'Todas'}],'5'));
      sections.push(widgetSelectControl('accounts','sort','Ordem',[{value:'manual',label:'Ordem cadastrada'},{value:'balance_desc',label:'Maior saldo'},{value:'balance_asc',label:'Menor saldo'}],'manual'));
      break;
    case 'commitments':
      sections.push(widgetSelectControl('commitments','scope','Mostrar',[{value:'monthly',label:'Mensal'},{value:'debts',label:'Dívidas'},{value:'all',label:'Tudo'}],'monthly'));
      break;
    case 'renewals':
      sections.push(widgetSelectControl('renewals','limit','Linhas',[{value:'3',label:'3 alertas'},{value:'5',label:'5 alertas'},{value:'8',label:'8 alertas'}],'5'));
      break;
    case 'goals':
      sections.push(widgetSelectControl('goals','limit','Metas',[{value:'3',label:'3 metas'},{value:'4',label:'4 metas'},{value:'6',label:'6 metas'}],'4'));
      sections.push(widgetSelectControl('goals','status','Mostrar',[{value:'active',label:'Em andamento'},{value:'all',label:'Todas'}],'active'));
      break;
    case 'budgets':
      sections.push(widgetSelectControl('budgets','limit','Orçamentos',[{value:'3',label:'3 categorias'},{value:'6',label:'6 categorias'},{value:'8',label:'8 categorias'}],'6'));
      sections.push(widgetSelectControl('budgets','status','Recorte',[{value:'all',label:'Todos'},{value:'risk',label:'So em risco'},{value:'overflow',label:'So estourados'}],'all'));
      break;
    case 'recent':
      sections.push(widgetSelectControl('recent','type','Tipo',[{value:'all',label:'Tudo'},{value:'expense',label:'Só despesas'},{value:'income',label:'Só receitas'}],'all'));
      sections.push(widgetSelectControl('recent','limit','Linhas',[{value:'4',label:'4 linhas'},{value:'6',label:'6 linhas'},{value:'10',label:'10 linhas'}],'6'));
      sections.push(widgetSelectControl('recent','scope','Escopo',[{value:'all',label:'Historico recente'},{value:'month',label:'So mes atual'}],'all'));
      break;
    case 'ministats':
      sections.push(widgetSelectControl('ministats','scope','Base',[{value:'month',label:'Mes atual'},{value:'30d',label:'Ultimos 30 dias'}],'month'));
      break;
    case 'vehicles':
      sections.push(widgetSelectControl('vehicles','period','Período',[{value:'30d',label:'30 dias'},{value:'90d',label:'90 dias'},{value:'year',label:'Ano'},{value:'all',label:'Tudo'}],'90d'));
      sections.push(widgetSelectControl('vehicles','limit','Histórico',[{value:'3',label:'3 eventos'},{value:'5',label:'5 eventos'}],'3'));
      sections.push(widgetSelectControl('vehicles','scope','Veiculo',[{value:'active',label:'Ativo'},{value:'all',label:'Todos'}],'active'));
      break;
    case 'shopping':
      sections.push(widgetSelectControl('shopping','limit','Itens',[{value:'3',label:'3 itens'},{value:'5',label:'5 itens'},{value:'8',label:'8 itens'}],'5'));
      sections.push(widgetSelectControl('shopping','scope','Lista',[{value:'active',label:'Lista ativa'},{value:'all',label:'Todas as listas'}],'active'));
      break;
    case 'saverate':
      sections.push(widgetSelectControl('saverate','months','Meses',[{value:'3',label:'3 meses'},{value:'6',label:'6 meses'},{value:'12',label:'12 meses'}],'3'));
      sections.push(widgetSelectControl('saverate','focus','Indicador',[{value:'current',label:'Mes atual'},{value:'average',label:'Media do periodo'}],'current'));
      break;
    case 'barcats':
      sections.push(widgetSelectControl('barcats','limit','Categorias',[{value:'4',label:'Top 4'},{value:'6',label:'Top 6'},{value:'8',label:'Top 8'}],'6'));
      sections.push(widgetSelectControl('barcats','scope','Base',[{value:'month',label:'Mes atual'},{value:'30d',label:'Ultimos 30 dias'}],'month'));
      break;
    default:
      sections.push('<div class="widget-menu-empty">Esse widget segue a visão principal da dashboard por enquanto.</div>');
      break;
  }
  if(pinned){
    sections.push('<div class="widget-menu-empty">Esse atalho fica fixo no topo para servir como entrada principal das áreas complementares.</div>');
    sections.push(`<div class="widget-inline-actions">${widgetMenuAction('Abrir area',`openWidgetTarget('${id}')`)}</div>`);
  }else{
    sections.push(`<div class="widget-inline-actions">${widgetMenuAction('Abrir area',`openWidgetTarget('${id}')`)}${widgetMenuAction('Topo',`moveWidgetToEdge('${id}','start')`)}${widgetMenuAction('Base',`moveWidgetToEdge('${id}','end')`)}</div>`);
    sections.push(`<div class="widget-inline-actions">${widgetMenuAction('Subir',`moveWidgetOrder('${id}',-1)`)}${widgetMenuAction('Descer',`moveWidgetOrder('${id}',1)`)}${widgetMenuAction('Remover',`toggleWidget('${id}')`,'danger')}</div>`);
  }
  return sections.join('');
}
function renderDashboardManager(){
  const el=document.getElementById('dashManager');
  if(!el)return;
  const activeIds=WIDGET_DEFS.filter(w=>isWidgetOn(w.id)).map(w=>w.id);
  const coreVisible=activeIds.filter(id=>widgetById(id)?.group==='core').length;
  const supportVisible=activeIds.filter(id=>widgetById(id)?.group==='support').length;
  const analysisVisible=activeIds.filter(id=>widgetById(id)?.group==='analysis').length;
  const allActive=activeIds.length===WIDGET_DEFS.length;
  el.innerHTML=`<div class="dash-manager-head"><div class="dash-manager-copy"><div class="dash-manager-title">Página inicial do seu jeito</div><div class="dash-manager-sub">${activeIds.length} widgets ativos • ${coreVisible} essenciais • ${supportVisible} de apoio • ${analysisVisible} analíticos</div></div><button class="btn btn-g btn-sm" onclick="toggleDashboardManager()">${dashboardManagerOpen?'Fechar editor':'Editar widgets'}</button></div>${dashboardManagerOpen?`<div class="dash-manager-panel"><div class="dash-manager-actions"><button class="btn btn-g btn-sm" onclick="applyFocusedDashboardPreset()">Só o essencial</button><button class="btn btn-g btn-sm" ${allActive?'disabled':''} onclick="enableAllDashboardWidgets()">Selecionar todos</button><button class="btn btn-g btn-sm" onclick="resetWidgetOrder()">Restaurar ordem</button><button class="btn btn-g btn-sm" onclick="resetDashboardWidgets()">Voltar ao padrão</button></div><div class="dash-manager-tip">Deixe o topo para abrir o que você usa sempre e ligue abaixo só os blocos que ajudam de verdade.</div><div class="dash-manager-list">${widgetOrder.map((id,idx)=>{const w=widgetById(id);if(!w)return'';const on=isWidgetOn(id);const pinned=isPinnedWidget(id);return `<div class="dash-manager-row ${on?'is-on':'is-off'} ${pinned?'is-pinned':''}" data-widget-id="${id}" draggable="${pinned?'false':'true'}" ondragstart="onDashboardManagerDragStart('${id}')" ondragend="onDashboardManagerDragEnd()" ondragover="onDashboardManagerDragOver('${id}',event)" ondrop="onDashboardManagerDrop('${id}',event)"><div class="dash-manager-info"><span class="dash-manager-grab" aria-hidden="true">${pinned?'★':'⋮⋮'}</span><span class="dash-manager-ico">${w.ico}</span><div><strong>${esc(w.name)} <span class="dash-manager-tag tag-${esc(w.group||'analysis')}">${widgetGroupLabel(w.group)}</span>${pinned?` <span class="dash-manager-tag tag-core">Fixo</span>`:''}</strong><small>${esc(w.desc)}</small></div></div><div class="dash-manager-controls"><button class="btn btn-g btn-sm" ${idx===0||pinned?'disabled':''} onclick="moveWidgetOrder('${id}',-1)">↑</button><button class="btn btn-g btn-sm" ${idx===widgetOrder.length-1||pinned?'disabled':''} onclick="moveWidgetOrder('${id}',1)">↓</button><button class="btn ${on&&!pinned?'btn-d':'btn-g'} btn-sm" ${pinned?'disabled':''} onclick="toggleWidget('${id}')">${on&&!pinned?'Remover':pinned?'Fixo':'Adicionar'}</button></div></div>`;}).join('')}</div></div>`:''}`;
}

function widgetRangeDate(scope='month'){
  if(scope==='30d')return iso(offD(new Date(),-29));
  return '';
}
function dashLocalIso(date){
  const d=new Date(date);
  d.setHours(12,0,0,0);
  const y=d.getFullYear();
  const m=String(d.getMonth()+1).padStart(2,'0');
  const day=String(d.getDate()).padStart(2,'0');
  return `${y}-${m}-${day}`;
}
function dashboardPeriodBounds(period='week'){
  const now=new Date();
  const inViewedMonth=curDt.getMonth()===now.getMonth()&&curDt.getFullYear()===now.getFullYear();
  const base=inViewedMonth?now:new Date(curDt.getFullYear(),curDt.getMonth(),Math.min(curDt.getDate(),28));
  base.setHours(12,0,0,0);
  if(period==='year'){
    return {
      from:dashLocalIso(new Date(curDt.getFullYear(),0,1)),
      to:dashLocalIso(new Date(curDt.getFullYear(),11,31)),
      label:`${curDt.getFullYear()}`
    };
  }
  if(period==='month'){
    return {
      from:dashLocalIso(new Date(curDt.getFullYear(),curDt.getMonth(),1)),
      to:dashLocalIso(new Date(curDt.getFullYear(),curDt.getMonth()+1,0)),
      label:`${MO[curDt.getMonth()]} ${curDt.getFullYear()}`
    };
  }
  const start=new Date(base);
  const day=(start.getDay()+6)%7;
  start.setDate(start.getDate()-day);
  const end=new Date(start);
  end.setDate(start.getDate()+6);
  return {
    from:dashLocalIso(start),
    to:dashLocalIso(end),
    label:`${fmtD(dashLocalIso(start))} a ${fmtD(dashLocalIso(end))}`
  };
}
function countDaysInclusive(from,to){
  const a=new Date(`${from}T12:00:00`);
  const b=new Date(`${to}T12:00:00`);
  const days=Math.round((b-a)/864e5)+1;
  return Number.isFinite(days)?Math.max(1,days):1;
}
function daysLeftInclusive(to){
  const todayIso=today();
  if(to<todayIso)return 0;
  return countDaysInclusive(todayIso,to);
}
function daysRemainingInPeriod(from,to){
  const todayIso=today();
  if(todayIso<from)return countDaysInclusive(from,to);
  if(todayIso>to)return 0;
  return countDaysInclusive(todayIso,to);
}
function dailyFlowAmount(tx){
  const amount=Number(tx?.amount);
  return Number.isFinite(amount)?amount:0;
}
function dailyFlowMoney(value){
  const amount=Number(value);
  return Number.isFinite(amount)?amount:0;
}
function dailyFlowShiftIso(date,days){
  const d=new Date(`${date}T12:00:00`);
  d.setDate(d.getDate()+days);
  return dashLocalIso(d);
}
function dailyFlowRealizedTotals(from,to,limit=today()){
  const end=limit<to?limit:to;
  const txs=(S.transactions||[]).filter(t=>t.date>=from&&t.date<=end&&!t.paid);
  return {
    spent:txs.filter(t=>t.type==='expense').reduce((sum,tx)=>sum+dailyFlowAmount(tx),0),
    earned:txs.filter(t=>t.type==='income').reduce((sum,tx)=>sum+dailyFlowAmount(tx),0)
  };
}
function dailyFlowFutureExpenseTransactions(from,to){
  return (S.transactions||[]).filter(t=>t.type==='expense'&&t.date>=from&&t.date<=to&&!t.paid).reduce((sum,tx)=>sum+dailyFlowAmount(tx),0);
}
function dailyFlowFutureExpenseItems(from,to){
  return (S.transactions||[]).filter(t=>t.type==='expense'&&t.date>=from&&t.date<=to&&!t.paid).map(t=>({date:t.date,amount:dailyFlowAmount(t)}));
}
function dailyFlowDueDateForMonth(item,ym){
  const [y,m]=ym.split('-').map(Number);
  const last=new Date(y,m,0).getDate();
  const day=Math.min(Number(item?.dueDay)||1,last);
  return `${y}-${String(m).padStart(2,'0')}-${String(day).padStart(2,'0')}`;
}
function dailyFlowScheduledDueExpenses(from,to){
  const out=[];
  (dueItems||[]).filter(item=>item&&item.active).forEach(item=>{
    const paidKeys=Array.isArray(item.paidKeys)?item.paidKeys:[];
    if(item.recurrence==='once'){
      const date=String(item.nextDueDate||'').substring(0,10);
      if(date>=from&&date<=to&&!paidKeys.includes(date.substring(0,7)))out.push({date,amount:dailyFlowAmount(item)});
      return;
    }
    const cursor=new Date(`${from}T12:00:00`);
    cursor.setDate(1);
    for(let i=0;i<18;i++){
      const ym=`${cursor.getFullYear()}-${String(cursor.getMonth()+1).padStart(2,'0')}`;
      const date=dailyFlowDueDateForMonth(item,ym);
      if(date>to)break;
      if(date>=from&&date<=to&&!paidKeys.includes(ym))out.push({date,amount:dailyFlowAmount(item)});
      cursor.setMonth(cursor.getMonth()+1);
    }
  });
  return out;
}
function dailyFlowScheduledExpenses(from,to,txFrom=from){
  if(from>to)return 0;
  const txTotal=txFrom<=to?dailyFlowFutureExpenseTransactions(txFrom,to):0;
  const dueTotal=dailyFlowScheduledDueExpenses(from,to).reduce((sum,item)=>sum+dailyFlowAmount(item),0);
  return txTotal+dueTotal;
}
function txCreatedTime(tx){
  const raw=tx?.createdAt||tx?.created_at;
  if(raw){
    const parsed=new Date(raw).getTime();
    if(Number.isFinite(parsed))return parsed;
    const numeric=Number(raw);
    if(Number.isFinite(numeric))return numeric;
  }
  const id=String(tx?.id||'');
  const fromUid=parseInt(id.slice(0,8),36);
  if(id[8]!=='-'&&Number.isFinite(fromUid)&&fromUid>946684800000&&fromUid<4102444800000)return fromUid;
  const fromDate=new Date(`${tx?.date||today()}T12:00:00`).getTime();
  return Number.isFinite(fromDate)?fromDate:0;
}
function fmtDashDateTime(ms){
  const d=new Date(ms);
  if(!Number.isFinite(d.getTime()))return '';
  const now=new Date();
  const startToday=new Date(now.getFullYear(),now.getMonth(),now.getDate()).getTime();
  const startYesterday=startToday-864e5;
  const dateLabel=d.getTime()>=startToday?'hoje':d.getTime()>=startYesterday?'ontem':d.toLocaleDateString('pt-BR',{day:'2-digit',month:'short',year:'numeric'});
  return `${dateLabel}, ${d.toLocaleTimeString('pt-BR',{hour:'2-digit',minute:'2-digit'})}`;
}
function lastDataNoticeInfo(){
  const txs=[...(S.transactions||[])];
  if(!txs.length)return null;
  const latest=[...txs].sort((a,b)=>txCreatedTime(b)-txCreatedTime(a))[0];
  const latestMs=txCreatedTime(latest);
  const latestDay=new Date(latestMs).toDateString();
  const sameDay=txs.filter(t=>new Date(txCreatedTime(t)).toDateString()===latestDay).length;
  const latestDate=[...txs].map(t=>t.date).filter(Boolean).sort().pop();
  const inferred=!(latest?.createdAt||latest?.created_at);
  return {latest,latestMs,sameDay,latestDate,inferred};
}
function renderLastDataNotice(){
  const el=document.getElementById('lastDataNotice');
  if(!el)return;
  const info=lastDataNoticeInfo();
  if(!info){
    el.innerHTML=`<button class="last-data-card empty" onclick="openModal()"><span class="last-data-ico">⏱</span><span><strong>Nenhum dado inserido ainda</strong><small>Crie ou importe sua primeira transação para acompanhar a última atualização.</small></span><span class="last-data-action">Lançar</span></button>`;
    return;
  }
  const source=info.inferred?'estimado pelas transações salvas':'registrado no histórico de transações';
  el.innerHTML=`<button class="last-data-card" onclick="showPage('transactions')"><span class="last-data-ico">⏱</span><span><strong>Últimos dados inseridos: ${esc(fmtDashDateTime(info.latestMs))}</strong><small>${info.sameDay} lançamento${info.sameDay===1?'':'s'} nesse dia • transação mais recente em ${fmtD(info.latestDate)} • ${source}</small></span><span class="last-data-action">Ver</span></button>`;
}
function widgetCards(){
  const txM=getMonthTx(curDt);
  const showFuture=((widgetFilters?.cards?.future)||'on')!=='off';
  const inc=txM.filter(t=>t.type==='income'&&!isFut(t.date)&&!t.paid).reduce((s,t)=>s+t.amount,0);
  const exp=txM.filter(t=>t.type==='expense'&&!isFut(t.date)&&!t.paid).reduce((s,t)=>s+t.amount,0);
  const fut=S.transactions.filter(t=>t.type==='expense'&&isFut(t.date)&&!t.paid).reduce((s,t)=>s+t.amount,0);
  const income=monthlyIncomeCents/100;
  const base=income||inc;
  const remaining=base-exp;
  const now=new Date();
  const sameMonthView=curDt.getMonth()===now.getMonth()&&curDt.getFullYear()===now.getFullYear();
  const daysInMonth=new Date(curDt.getFullYear(),curDt.getMonth()+1,0).getDate();
  const daysLeft=sameMonthView?Math.max(daysInMonth-now.getDate()+1,1):daysInMonth;
  const safeDay=base?Math.max(remaining/daysLeft,0):0;
  const burnPct=base?Math.min(Math.round(exp/base*100),999):0;
  window._dashInc=inc;
  window._dashExp=exp;
  return `<div class="g4 summary-grid dash-section">
    <div class="sc sc-glow-ac2" style="cursor:pointer" onclick="showPage('settings')"><span class="ci">💼</span><div class="cl">Salario base</div><div class="cv ${base?'pos':'neu'}">${base?fmt(base):'Definir'}</div><div class="cc">${income?'renda mensal configurada':'toque para configurar'}</div></div>
    <div class="sc sc-glow-dan" style="cursor:pointer" onclick="showPage('transactions')"><span class="ci">⬇️</span><div class="cl">Gasto no mes</div><div class="cv neg">${fmt(exp)}</div><div class="cc dn">${base?`${burnPct}% do salario`:'sem salario definido'}</div></div>
    <div class="sc sc-glow-ac" style="cursor:pointer" onclick="showPage('transactions')"><span class="ci">🧭</span><div class="cl">Seguro por dia</div><div class="cv ${safeDay>0?'pos':'neg'}">${fmt(safeDay)}</div><div class="cc">${daysLeft} dia${daysLeft===1?'':'s'} ate fechar</div></div>
    <div class="sc sc-glow-ac" style="cursor:pointer" onclick="showPage('transactions')"><span class="ci">🌱</span><div class="cl">Sobra projetada</div><div class="cv ${remaining>=0?'pos':'neg'}">${fmt(remaining)}</div><div class="cc">${fmt(inc)} recebido no mes</div></div>
    ${showFuture?`<div class="sc sc-glow-fut" style="cursor:pointer" onclick="showPage('future')"><span class="ci">🔮</span><div class="cl">A pagar</div><div class="cv fut">${fmt(fut)}</div><div class="cc" style="color:var(--fut)">ver contas futuras</div></div>`:''}
  </div>`;
}
function dailyFlowMetric(label,value,meta,tone='neutral',icon='•',page=''){
  const valueClass={income:'pos',danger:'neg',safe:'neu',forecast:'fut',muted:''}[tone]||'';
  const detailClass=tone==='danger'?'dn':tone==='safe'?'up':'';
  const tag=page?'button':'div';
  const action=page?` type="button" onclick="showPage('${page}')" aria-label="Abrir ${esc(label)}"`:'';
  return `<${tag} class="daily-flow-metric ${tone} ${page?'is-clickable':''}"${action}><span class="daily-flow-ico">${icon}</span><div class="daily-flow-copy"><div class="cl">${label}</div><div class="cv ${valueClass}">${fmt(value)}</div>${meta?`<div class="cc ${detailClass}">${meta}</div>`:''}</div></${tag}>`;
}
function dailyFlowMonthlyCharts(bounds,todayIso,scheduledFrom,txScheduledFrom,monthlyBase,spendingBase){
  const days=countDaysInclusive(bounds.from,bounds.to);
  const shortMoney=value=>{
    const abs=Math.abs(value);
    if(abs>=1000)return `${(value/1000).toLocaleString('pt-BR',{maximumFractionDigits:1})}k`;
    return Math.round(value).toLocaleString('pt-BR');
  };
  const realByDay=new Map();
  (S.transactions||[]).filter(t=>t.type==='expense'&&t.date>=bounds.from&&t.date<=todayIso&&t.date<=bounds.to&&!t.paid).forEach(tx=>{
    realByDay.set(tx.date,(realByDay.get(tx.date)||0)+dailyFlowAmount(tx));
  });
  const scheduledByDay=new Map();
  dailyFlowFutureExpenseItems(txScheduledFrom,bounds.to).forEach(item=>{
    scheduledByDay.set(item.date,(scheduledByDay.get(item.date)||0)+dailyFlowAmount(item));
  });
  dailyFlowScheduledDueExpenses(scheduledFrom,bounds.to).forEach(item=>{
    scheduledByDay.set(item.date,(scheduledByDay.get(item.date)||0)+dailyFlowAmount(item));
  });
  const chartW=1080;
  const chartH=198;
  const padL=46;
  const padR=46;
  const top=20;
  const lineBottom=154;
  const labelY=178;
  const values=Array.from({length:days},(_,idx)=>{
    const date=dailyFlowShiftIso(bounds.from,idx);
    return {
      date,
      day:idx+1,
      real:realByDay.get(date)||0,
      scheduled:scheduledByDay.get(date)||0
    };
  });
  let realizedSpent=0;
  const realizedSeries=values.map(item=>{
    if(item.date<=todayIso)realizedSpent+=item.real;
    return {...item,spent:realizedSpent,balance:spendingBase-realizedSpent};
  });
  let plannedSpent=realizedSpent;
  const plannedSeries=values.map(item=>{
    if(item.date>todayIso)plannedSpent+=item.scheduled;
    return {...item,spent:plannedSpent,balance:spendingBase-plannedSpent};
  });
  const combined=values.map((item,idx)=>item.date>todayIso?plannedSeries[idx]:realizedSeries[idx]);
  const allValues=[
    0,
    spendingBase,
    ...realizedSeries.map(item=>item.spent),
    ...plannedSeries.map(item=>item.spent),
    ...combined.map(item=>item.balance)
  ];
  const dataMax=Math.max(1,...allValues);
  const minValue=0;
  const tickStep=1000;
  const maxValue=Math.max(tickStep,Math.ceil(dataMax/tickStep)*tickStep);
  const range=Math.max(1,maxValue-minValue);
  const xFor=idx=>padL+(idx*Math.max(1,(chartW-padL-padR)/(days-1||1)));
  const yFor=value=>lineBottom-(((value-minValue)/range)*(lineBottom-top));
  const toPoint=(idx,value)=>({x:xFor(idx),y:yFor(value)});
  const smoothPath=points=>{
    if(!points.length)return '';
    if(points.length===1)return `M ${points[0].x.toFixed(2)} ${points[0].y.toFixed(2)}`;
    let path=`M ${points[0].x.toFixed(2)} ${points[0].y.toFixed(2)}`;
    for(let i=1;i<points.length;i++){
      const p0=points[i-1];
      const p1=points[i];
      const pm=points[i-2]||p0;
      const pp=points[i+1]||p1;
      const cp1={x:p0.x+(p1.x-pm.x)/6,y:p0.y+(p1.y-pm.y)/6};
      const cp2={x:p1.x-(pp.x-p0.x)/6,y:p1.y-(pp.y-p0.y)/6};
      path+=` C ${cp1.x.toFixed(2)} ${cp1.y.toFixed(2)}, ${cp2.x.toFixed(2)} ${cp2.y.toFixed(2)}, ${p1.x.toFixed(2)} ${p1.y.toFixed(2)}`;
    }
    return path;
  };
  const realPoints=realizedSeries.map((item,idx)=>item.date<=todayIso?toPoint(idx,item.spent):null).filter(Boolean);
  const plannedPoints=plannedSeries.map((item,idx)=>item.date>=todayIso?toPoint(idx,item.spent):null).filter(Boolean);
  const balancePoints=combined.map((item,idx)=>toPoint(idx,item.balance));
  const realPath=smoothPath(realPoints);
  const plannedPath=smoothPath(plannedPoints);
  const balancePath=smoothPath(balancePoints);
  const rawTodayIndex=values.findIndex(item=>item.date===todayIso);
  const todayIndex=rawTodayIndex>=0?rawTodayIndex:(todayIso<bounds.from?0:days-1);
  const todayX=xFor(todayIndex);
  const ticks=[];
  for(let value=maxValue;value>=minValue;value-=tickStep)ticks.push(value);
  const yAxis=ticks.map(value=>{
    const y=yFor(value).toFixed(2);
    return `<g class="daily-flow-y"><line x1="${padL}" y1="${y}" x2="${chartW-padR}" y2="${y}"></line><text x="${padL-8}" y="${Number(y)+3}" text-anchor="end">${shortMoney(value)}</text></g>`;
  }).join('');
  const yAxisEnd=ticks.map(value=>{
    const y=yFor(value).toFixed(2);
    return `<g class="daily-flow-y daily-flow-y-end"><text x="${chartW-padR+8}" y="${Number(y)+3}" text-anchor="start">${shortMoney(value)}</text></g>`;
  }).join('');
  const dots=values.map((item,idx)=>{
    const date=item.date;
    const future=date>todayIso;
    const point=future?plannedSeries[idx]:realizedSeries[idx];
    const x=xFor(idx).toFixed(2);
    const y=yFor(point.spent).toFixed(2);
    const cls=date===todayIso?'today':future?'planned':'real';
    return `<g class="daily-flow-dot ${cls}"><circle cx="${x}" cy="${y}" r="${date===todayIso?4:2.8}"></circle><title>${fmtD(date)} • gasto acumulado ${fmt(point.spent)} • saldo ${fmt(point.balance)} • gasto do dia ${fmt(item.real)} • agenda ${fmt(item.scheduled)}</title></g>`;
  }).join('');
  const balanceDots=combined.map((item,idx)=>{
    const date=item.date;
    if(idx%3!==0&&date!==todayIso&&idx!==days-1)return '';
    const x=xFor(idx).toFixed(2);
    const y=yFor(item.balance).toFixed(2);
    return `<g class="daily-flow-dot balance"><circle cx="${x}" cy="${y}" r="${date===todayIso?4.2:2.6}"></circle><title>${fmtD(date)} • saldo disponível ${fmt(item.balance)} • gastos acumulados ${fmt(item.spent)}</title></g>`;
  }).join('');
  const labels=values.map((item,idx)=>{
    const x=xFor(idx).toFixed(2);
    const cls=item.date===todayIso?'today':item.date>todayIso?'planned':'';
    return `<text class="${cls}" x="${x}" y="${labelY}" text-anchor="middle">${item.day}</text>`;
  }).join('');
  const grid=values.map((item,idx)=>{
    const x=xFor(idx).toFixed(2);
    const cls=item.date===todayIso?'today':item.date>todayIso?'planned':'';
    return `<line class="${cls}" x1="${x}" y1="${top}" x2="${x}" y2="${lineBottom}"></line>`;
  }).join('');
  return `<div class="daily-flow-graphs">
    <div class="daily-flow-chart-card">
      <div class="daily-flow-chart-head"><strong>Gasto x saldo</strong></div>
      <svg class="daily-flow-line-chart" viewBox="0 0 ${chartW} ${chartH}" role="img" aria-label="Gastos acumulados e saldo disponível do mês">
        <g class="daily-flow-y-axis">${yAxis}</g>
        <g class="daily-flow-y-axis daily-flow-y-axis-end">${yAxisEnd}</g>
        <g class="daily-flow-grid-lines">${grid}</g>
        <line class="daily-flow-today-line" x1="${todayX.toFixed(2)}" y1="${top}" x2="${todayX.toFixed(2)}" y2="${lineBottom}"></line>
        <path class="daily-flow-line-balance" d="${balancePath}"></path>
        <path class="daily-flow-line-real" d="${realPath}"></path>
        <path class="daily-flow-line-planned" d="${plannedPath}"></path>
        <g class="daily-flow-labels">${labels}</g>
        <g>${dots}</g>
        <g>${balanceDots}</g>
      </svg>
      <div class="daily-flow-legend"><span><i class="real"></i>Gasto realizado</span><span><i class="scheduled"></i>Gasto previsto</span><span><i class="balance"></i>Saldo disponível</span><span><i class="today"></i>Hoje</span></div>
    </div>
  </div>`;
}
function widgetDailyWeek(){
  const bounds=dashboardPeriodBounds('week');
  const todayIso=today();
  const endForElapsed=todayIso<bounds.from?bounds.from:(todayIso>bounds.to?bounds.to:todayIso);
  const elapsedDays=countDaysInclusive(bounds.from,endForElapsed);
  const lastFrom=dailyFlowShiftIso(bounds.from,-7);
  const lastTo=dailyFlowShiftIso(bounds.to,-7);
  const current=dailyFlowRealizedTotals(bounds.from,bounds.to,todayIso);
  const previous=dailyFlowRealizedTotals(lastFrom,lastTo,lastTo);
  const currentAvg=dailyFlowMoney(current.spent/elapsedDays);
  const delta=current.spent-previous.spent;
  const scheduledFrom=todayIso>bounds.from?todayIso:bounds.from;
  const txScheduledFrom=todayIso>=bounds.from?dailyFlowShiftIso(todayIso,1):bounds.from;
  const weekAgenda=dailyFlowScheduledExpenses(scheduledFrom,bounds.to,txScheduledFrom);
  const remainingWeekDays=daysRemainingInPeriod(bounds.from,bounds.to);
  const deltaLabel=delta===0?'mesmo gasto da semana passada':`${delta>0?'+':'-'}${fmt(Math.abs(delta))} vs semana passada`;
  return `<div class="daily-flow-widget daily-flow-widget-week dash-section">
    <div class="bh daily-flow-head"><div><div class="ct">🗓️ Ritmo semanal</div><div class="cs">${bounds.label} • comparado com ${fmtD(lastFrom)} a ${fmtD(lastTo)}</div></div></div>
    <div class="daily-flow-grid">
      ${dailyFlowMetric('Gasto da semana',current.spent,deltaLabel,'danger','⬇️','transactions')}
      ${dailyFlowMetric('Agendado na semana',weekAgenda,`${remainingWeekDays} dia${remainingWeekDays===1?'':'s'} restantes`,'forecast','🔮','future')}
      ${dailyFlowMetric('Média diária',currentAvg,`${elapsedDays} dia${elapsedDays===1?'':'s'} considerados`,'danger','⏱️','transactions')}
      ${dailyFlowMetric('Semana passada',previous.spent,'referência dos 7 dias anteriores','muted','↩️','transactions')}
    </div>
  </div>`;
}
function widgetDailyMonth(){
  const bounds=dashboardPeriodBounds('month');
  const todayIso=today();
  const endForElapsed=todayIso<bounds.from?bounds.from:(todayIso>bounds.to?bounds.to:todayIso);
  const elapsedDays=countDaysInclusive(bounds.from,endForElapsed);
  const periodDays=countDaysInclusive(bounds.from,bounds.to);
  const remainingDays=daysRemainingInPeriod(bounds.from,bounds.to);
  const realized=dailyFlowRealizedTotals(bounds.from,bounds.to,todayIso);
  const scheduledFrom=todayIso>bounds.from?todayIso:bounds.from;
  const txScheduledFrom=todayIso>=bounds.from?dailyFlowShiftIso(todayIso,1):bounds.from;
  const agenda=dailyFlowScheduledExpenses(scheduledFrom,bounds.to,txScheduledFrom);
  const plannedSpent=realized.spent+agenda;
  const monthlyBase=dailyFlowMoney(monthlyIncomeCents)/100;
  const spendingBase=monthlyBase||realized.earned;
  const canSpend=dailyFlowMoney(Math.max(spendingBase-plannedSpent,0));
  const canSpendDaily=dailyFlowMoney(canSpend/Math.max(remainingDays,1));
  const earnedDaily=dailyFlowMoney(realized.earned/elapsedDays);
  const spentDaily=dailyFlowMoney(plannedSpent/periodDays);
  const canSpendMeta=`${fmt(canSpend)} sobrando após ${fmt(realized.spent)} feitos + ${fmt(agenda)} agendados`;
  return `<div class="daily-flow-widget daily-flow-widget-month dash-section">
    <div class="bh daily-flow-head"><div><div class="ct">📆 Ritmo mensal</div><div class="cs">${bounds.label} • ${elapsedDays} dia${elapsedDays===1?'':'s'} até agora • base: ${monthlyBase?'salário configurado':'ganhos do mês'}</div></div></div>
    <div class="daily-flow-grid daily-flow-grid-wide">
      ${dailyFlowMetric('Ganho por dia',earnedDaily,`${fmt(realized.earned)} no mês`,'income','💼','transactions')}
      ${dailyFlowMetric('Gasto diário mensal',spentDaily,`${fmt(plannedSpent)} no mês, incluindo agenda`,'danger','⬇️','transactions')}
      ${dailyFlowMetric('Gasto diário até hoje',dailyFlowMoney(realized.spent/elapsedDays),`${fmt(realized.spent)} já feito no mês`,'danger','🧾','transactions')}
      ${dailyFlowMetric('Agendado até o fim',agenda,`${remainingDays} dia${remainingDays===1?'':'s'} restantes no mês`,'forecast','🔮','future')}
      ${dailyFlowMetric('Pode gastar por dia',canSpendDaily,canSpendMeta,'safe','🌱','transactions')}
    </div>
    ${dailyFlowMonthlyCharts(bounds,todayIso,scheduledFrom,txScheduledFrom,monthlyBase,spendingBase)}
  </div>`;
}
function widgetDailyYear(){
  const bounds=dashboardPeriodBounds('year');
  const todayIso=today();
  const endForElapsed=todayIso<bounds.from?bounds.from:(todayIso>bounds.to?bounds.to:todayIso);
  const elapsedDays=countDaysInclusive(bounds.from,endForElapsed);
  const realized=dailyFlowRealizedTotals(bounds.from,bounds.to,todayIso);
  return `<div class="daily-flow-widget daily-flow-widget-year dash-section">
    <div class="bh daily-flow-head"><div><div class="ct">📅 Ritmo anual</div><div class="cs">${bounds.label} • acumulado até ${fmtD(endForElapsed)} • ${elapsedDays} dia${elapsedDays===1?'':'s'}</div></div></div>
    <div class="daily-flow-grid">
      ${dailyFlowMetric('Ganho até hoje',realized.earned,'receitas realizadas no ano','income','💼','transactions')}
      ${dailyFlowMetric('Ganho diário',dailyFlowMoney(realized.earned/elapsedDays),'média anual até agora','income','📈','transactions')}
      ${dailyFlowMetric('Gasto anual',realized.spent,'despesas realizadas no ano','danger','⬇️','transactions')}
      ${dailyFlowMetric('Gasto diário',dailyFlowMoney(realized.spent/elapsedDays),'média anual até agora','danger','⏱️','transactions')}
    </div>
  </div>`;
}
function widgetMiniStats(){
  const scope=(widgetFilters?.ministats?.scope)||'month';
  const txM=(scope==='30d'?S.transactions.filter(t=>t.date>=widgetRangeDate('30d')):getMonthTx(curDt)).filter(t=>!isFut(t.date)&&!t.paid&&t.type==='expense');
  if(!txM.length)return '';
  const total=txM.reduce((s,t)=>s+t.amount,0);
  const dias=new Set(txM.map(t=>t.date)).size;
  const media=total/Math.max(dias,1);
  const maior=Math.max(...txM.map(t=>t.amount));
  const diasMes=new Date(curDt.getFullYear(),curDt.getMonth()+1,0).getDate();
  const diasRest=diasMes-curDt.getDate();
  return `<div class="mini-stats dash-section">
    <div class="mini-stat"><div class="ms-ico">📊</div><div class="ms-val" style="color:var(--warn)">${fmt(media)}</div><div class="ms-lbl">Media/dia</div></div>
    <div class="mini-stat"><div class="ms-ico">🔺</div><div class="ms-val" style="color:var(--dan)">${fmt(maior)}</div><div class="ms-lbl">Maior gasto</div></div>
    <div class="mini-stat"><div class="ms-ico">🗓️</div><div class="ms-val" style="color:var(--ac2)">${diasRest}</div><div class="ms-lbl">${scope==='30d'?'Dias do mes':'Dias restantes'}</div></div>
  </div>`;
}
function widgetQuickActions(){
  const limit=Number((widgetFilters?.quickactions?.limit)||6);
  const actions=[
    {icon:'+',title:'Nova transação',hint:'Lançar gasto ou receita',className:'primary',fn:'openModal()'},
    {icon:'📥',title:'Importar',hint:'CSV, OFX, Pix, OCR e texto',className:'primary',fn:'openImportCenter()'},
    {icon:'📌',title:'Vencimento',hint:'Conta futura ou fixa',fn:'openDueModal()'},
    {icon:'🎯',title:'Orçamento',hint:'Definir limite mensal',fn:'openBudModal()'},
    {icon:'🏦',title:'Conta',hint:'Saldo ou carteira',fn:'openAccModal()'},
    {icon:'🏆',title:'Meta',hint:'Objetivo financeiro',fn:'openGoalModal()'}
  ].slice(0,Math.max(limit,4));
  return `<div class="quick-actions-widget dash-section">
    <div class="bh"><div><div class="ct">Ações rápidas</div><div class="cs">Capture, importe e organize sem sair da dashboard</div></div></div>
    <div class="quick-actions-grid">${actions.map(a=>`<button class="quick-action-card ${a.className||''}" onclick="${a.fn}"><span class="quick-action-ico">${a.icon}</span><strong>${esc(a.title)}</strong><small>${esc(a.hint)}</small></button>`).join('')}</div>
  </div>`;
}
function widgetWorkbench(){
  const layout=(widgetFilters?.workbench?.layout)||'grid';
  const areas=[
    {icon:'📦',title:'Compromissos',short:'Comprom.',hint:'Assinaturas, dívidas e contratos',page:'commitments'},
    {icon:'🤝',title:'Acertos',short:'Acertos',hint:'Casal, família e divisão',page:'shared'},
    {icon:'🎯',title:'Limites',short:'Limites',hint:'Orçamentos e risco',page:'budget'},
    {icon:'🛒',title:'Compras',short:'Compras',hint:'Listas e reposição',page:'shopping'},
    {icon:'🚗',title:'Carro',short:'Carro',hint:'Combustível e manutenção',page:'car'},
    {icon:'🏆',title:'Metas',short:'Metas',hint:'Objetivos e reserva',page:'goals'},
    {icon:'🏦',title:'Contas',short:'Contas',hint:'Saldo e rendimento',page:'accounts'},
    {icon:'🔔',title:'Renovações',short:'Alertas',hint:'Reajustes e vencimentos próximos',page:'commitments'}
  ];
  const compact=layout==='compact';
  const items=areas;
  return `<div class="quick-actions-widget quick-actions-widget-featured dash-section">
    <div class="bh"><div><div class="ct">🧭 Atalhos do Finanza</div></div></div>
    <div class="quick-actions-grid workbench-grid ${compact?'workbench-grid-compact':'workbench-grid-full'}">${items.map(area=>compact
      ? `<button class="quick-action-card quick-action-card-compact" onclick="showPage('${area.page}')" title="${esc(area.title)}"><span class="quick-action-ico">${area.icon}</span><strong>${esc(area.short||area.title)}</strong></button>`
      : `<button class="quick-action-card quick-action-card-workbench" onclick="showPage('${area.page}')"><span class="quick-action-ico">${area.icon}</span><strong>${esc(area.title)}</strong><small>${esc(area.hint)}</small></button>`
    ).join('')}</div>
  </div>`;
}
function widgetAccounts(){
  if(!S.accounts.length)return '';
  const limit=(widgetFilters?.accounts?.limit)||'5';
  const sort=(widgetFilters?.accounts?.sort)||'manual';
  let accounts=[...S.accounts];
  if(sort==='balance_desc')accounts.sort((a,b)=>getAccBal(b.id)-getAccBal(a.id));
  if(sort==='balance_asc')accounts.sort((a,b)=>getAccBal(a.id)-getAccBal(b.id));
  accounts=limit==='all'?accounts:accounts.slice(0,Number(limit));
  return `<div class="box dash-section">
    <div class="bh"><div class="ct">Contas</div><button class="btn btn-g btn-sm" onclick="showPage('accounts')">Ver →</button></div>
    <div style="display:flex;flex-direction:column;gap:8px;">
      ${accounts.map(a=>{const bal=getAccBal(a.id);const y=getAccYield(a,1);return `<div style="display:flex;align-items:center;justify-content:space-between;padding:8px 4px;border-bottom:1px solid var(--bd2)"><div style="display:flex;align-items:center;gap:8px"><span style="font-size:18px">${a.icon}</span><span style="font-size:13px;font-weight:500">${a.name}</span>${y>0?`<span class="yield-pill">+${fmt(y)}</span>`:''}</div><span style="font-family:var(--font-money);font-weight:700;font-size:14px;color:${bal>=0?'var(--ac)':'var(--dan)'}">${fmt(bal)}</span></div>`;}).join('')}
    </div>
  </div>`;
}
function widgetCommitments(){
  if(typeof moneyCommittedSummary!=='function')return '';
  const summary=moneyCommittedSummary();
  const debts=(typeof debtCenterItems==='function'?debtCenterItems():[]).filter(item=>item.status==='active');
  const subs=(typeof subscriptionCenterItems==='function'?subscriptionCenterItems():[]).filter(item=>item.status==='active');
  const contracts=(typeof contractCenterItems==='function'?contractCenterItems():[]).filter(item=>item.status!=='ended');
  const scope=(widgetFilters?.commitments?.scope)||'monthly';
  const mainValue=scope==='debts'
    ? debts.reduce((sum,item)=>sum+item.outstandingAmount,0)
    : summary.total;
  const chips=[
    {label:'Assinaturas',value:summary.subscriptions},
    {label:'Parcelas',value:summary.debts},
    {label:'Contratos',value:summary.contracts}
  ];
  return `<div class="box dash-section">
    <div class="bh"><div><div class="ct">📦 Compromissos fixos</div><div class="cs">${subs.length} assinatura(s) • ${debts.length} dívida(s) • ${contracts.length} contrato(s)</div></div><button class="btn btn-g btn-sm" onclick="showPage('commitments')">Abrir →</button></div>
    <div class="future-center-row" style="padding:6px 0 12px"><div><div class="future-center-name">${scope==='debts'?'Dívidas em aberto':'Dinheiro comprometido no mês'}</div><div class="future-center-meta">${scope==='debts'?'saldo total ainda pendente':'o que já sai antes do restante do mês'}</div></div><div class="future-center-amt">${fmt(mainValue)}</div></div>
    <div class="proj-scenarios">
      ${chips.map(item=>`<div class="proj-scenario"><span>${item.label}</span><strong>${fmt(item.value)}</strong></div>`).join('')}
    </div>
  </div>`;
}
function widgetRenewals(){
  if(typeof subscriptionCenterItems!=='function'||typeof contractCenterItems!=='function'||typeof daysUntilDate!=='function')return '';
  const limit=Number((widgetFilters?.renewals?.limit)||5);
  const entries=[
    ...subscriptionCenterItems().filter(item=>item.status==='active').map(item=>({name:item.name,date:item.renewalDate,kind:'Assinatura',amount:item.amount})),
    ...contractCenterItems().filter(item=>item.status!=='ended').map(item=>({name:item.name,date:item.adjustmentDate||item.renewalDate,kind:item.adjustmentDate?'Reajuste':'Contrato',amount:item.monthlyAmount||0}))
  ]
    .filter(item=>item.date&&daysUntilDate(item.date)!==null&&daysUntilDate(item.date)<=30)
    .sort((a,b)=>a.date.localeCompare(b.date))
    .slice(0,limit);
  if(!entries.length)return '';
  return `<div class="box dash-section">
    <div class="bh"><div><div class="ct">🔔 Renovações e reajustes</div><div class="cs">O que merece revisão nos próximos 30 dias</div></div><button class="btn btn-g btn-sm" onclick="showPage('commitments')">Ver central →</button></div>
    <div style="display:flex;flex-direction:column;gap:8px">
      ${entries.map(item=>{const days=daysUntilDate(item.date);const tone=days<=3?'var(--dan)':days<=10?'var(--warn)':'var(--ac2)';return `<div class="future-center-row"><div><div class="future-center-name">${esc(item.name)}</div><div class="future-center-meta" style="color:${tone}">${item.kind} • ${days<0?`há ${Math.abs(days)}d`:`em ${days}d`} • ${fmtD(item.date)}</div></div><div class="future-center-amt">${item.amount?fmt(item.amount):'—'}</div></div>`;}).join('')}
    </div>
  </div>`;
}
function widgetGoals(){
  const limit=Number((widgetFilters?.goals?.limit)||4);
  const status=(widgetFilters?.goals?.status)||'active';
  const goals=S.goals.filter(g=>status==='all'||(g.current/g.target)<1).slice(0,limit);
  if(!goals.length)return '';
  return `<div class="goals-widget dash-section">
    <div class="bh"><div class="ct">🏆 Metas</div><button class="btn btn-g btn-sm" onclick="showPage('goals')">Ver todas →</button></div>
    <div class="goals-widget-list">
      ${goals.map(g=>{const pct=Math.min((g.current/g.target)*100,100);const col=pct>=80?'var(--ac)':pct>=50?'var(--ac2)':'var(--warn)';return `<div class="gw-item"><span class="gw-icon">${g.icon}</span><div class="gw-info"><div class="gw-name">${g.name}</div><div class="gw-bar"><div class="gw-fill" style="width:${pct}%;background:${col}"></div></div></div><span class="gw-pct" style="color:${col}">${Math.round(pct)}%</span></div>`;}).join('')}
    </div>
  </div>`;
}
function widgetBudgets(){
  const limit=Number((widgetFilters?.budgets?.limit)||6);
  const status=(widgetFilters?.budgets?.status)||'all';
  const now=new Date();
  const buds=S.budgets.map(b=>{const spent=S.transactions.filter(t=>{if(t.type!=='expense'||isFut(t.date)||t.paid)return false;const d=new Date(t.date+'T12:00:00');return d.getMonth()===now.getMonth()&&d.getFullYear()===now.getFullYear()&&t.category===b.category;}).reduce((s,t)=>s+t.amount,0);return {...b,spent,pct:Math.min((spent/b.limit)*100,100)};}).filter(b=>status==='all'||(status==='risk'?b.pct>=80:b.pct>=100)).sort((a,b)=>b.pct-a.pct).slice(0,limit);
  if(!buds.length)return '';
  return `<div class="budget-widget dash-section">
    <div class="bh"><div class="ct">🎯 Orcamentos</div><button class="btn btn-g btn-sm" onclick="showPage('budget')">Ver todos →</button></div>
    <div class="bw-list">
      ${buds.map(b=>{const cat=getCat(b.category);const col=b.pct>=100?'var(--dan)':b.pct>=80?'var(--warn)':'var(--ac)';return `<div class="bw-item"><span class="bw-cat">${cat.ico}</span><div class="bw-info"><div class="bw-top"><span class="bw-name">${b.category}</span><span class="bw-vals">${fmt(b.spent)} / ${fmt(b.limit)}</span></div><div class="bw-bar"><div class="bw-fill" style="width:${b.pct}%;background:${col}"></div></div></div></div>`;}).join('')}
    </div>
  </div>`;
}
function widgetCharts(){
  const preferredMode=(widgetFilters?.charts?.mode)||'bars';
  if(preferredMode!==chartMode&&typeof setChartMode==='function')setTimeout(()=>setChartMode(preferredMode),0);
  return `<div class="cr dash-section">
    <div class="cc-box">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:2px;">
        <div class="ct">Fluxo de Caixa</div>
        <div style="display:flex;gap:3px;">
          <button class="vbtn ${chartMode==='bars'?'active':''}" id="cBars" onclick="setChartMode('bars')" title="Barras">▦</button>
          <button class="vbtn ${chartMode==='line'?'active':''}" id="cLine" onclick="setChartMode('line')" title="Acumulado">📈</button>
        </div>
      </div>
      <div class="cs" id="chartSub">${chartMode==='bars'?'Receitas vs despesas - ultimos 6 meses':'Saldo acumulado - ultimos 6 meses'}</div>
      <canvas id="flowChart"></canvas>
    </div>
    <div class="cc-box"><div class="ct">Categorias</div><div class="cs">Clique para filtrar</div><canvas id="catChart" style="cursor:pointer"></canvas></div>
  </div>`;
}
function widgetRecent(){
  const type=(widgetFilters?.recent?.type)||'all';
  const limit=Number((widgetFilters?.recent?.limit)||6);
  const scope=(widgetFilters?.recent?.scope)||'all';
  const sorted=[...S.transactions].filter(t=>!isFut(t.date)&&!t.paid&&(type==='all'||t.type===type)).filter(t=>{if(scope!=='month')return true;const d=new Date(t.date+'T12:00:00');return d.getMonth()===curDt.getMonth()&&d.getFullYear()===curDt.getFullYear();}).sort((a,b)=>b.date.localeCompare(a.date)).slice(0,limit);
  return `<div class="box dash-section"><div class="bh"><div class="ct">Ultimas transacoes</div><div class="widget-header-actions"><button class="btn btn-g btn-sm" onclick="openImportCenter()">📥 Importar</button><button class="btn btn-g btn-sm" onclick="showPage('transactions')">Ver todas</button></div></div><div class="recent-list" id="recList">${sorted.length?sorted.map(recentTxHTML).join(''):'<div class="empty"><span class="ei">💸</span><p>Nenhuma transacao ainda.</p></div>'}</div></div>`;
}
function widgetShoppingDash(){
  let data={lists:[],items:[]};
  try{data=JSON.parse(localStorage.getItem('fz_shopping')||'{}');}catch{}
  const lists=data.lists||[];
  const items=data.items||[];
  const limit=Number((widgetFilters?.shopping?.limit)||5);
  const scope=(widgetFilters?.shopping?.scope)||'active';
  const pending=items.filter(i=>!i.bought);
  const list=lists[0];
  const listItems=scope==='all'?items:items.filter(i=>i.listId===list?.id);
  const bought=listItems.filter(i=>i.bought).length;
  const pct=listItems.length?Math.round(bought/listItems.length*100):0;
  const visiblePending=scope==='all'?pending:pending.filter(i=>i.listId===list?.id);
  return `<div class="box dash-section"><div class="bh"><div><div class="ct">🛒 ${list?.ico||'🛒'} ${scope==='all'?'Listas de compras':(list?.name||'Lista de Compras')}</div><div class="cs">${visiblePending.length} pendente${visiblePending.length!==1?'s':''}</div></div><button class="btn btn-g btn-sm" onclick="showPage('shopping')">Abrir →</button></div><div class="sl-prog-bar" style="margin-bottom:12px"><div class="sl-prog-fill" style="width:${pct}%"></div></div>${visiblePending.slice(0,limit).map(i=>`<div style="display:flex;align-items:center;gap:8px;padding:7px 2px;border-bottom:1px solid var(--bd2)"><div style="width:16px;height:16px;border-radius:5px;border:2px solid var(--bd);flex-shrink:0"></div><span style="font-size:13px;flex:1;min-width:0">${i.name}</span>${i.qty?`<span style="font-size:11px;color:var(--mt)">${i.qty}</span>`:''}</div>`).join('')||'<div class="empty" style="padding:18px"><p>Lista sem pendencias.</p></div>'}</div>`;
}
function widgetSaveRate(){
  const monthsCount=Number((widgetFilters?.saverate?.months)||3);
  const focus=(widgetFilters?.saverate?.focus)||'current';
  const months=[];
  for(let i=monthsCount-1;i>=0;i--){
    const d=new Date(curDt.getFullYear(),curDt.getMonth()-i,1);
    const txs=getMonthTx(d).filter(t=>!isFut(t.date)&&!t.paid);
    const inc=txs.filter(t=>t.type==='income').reduce((s,t)=>s+t.amount,0);
    const exp=txs.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amount,0);
    if(inc>0)months.push({label:['Jan','Fev','Mar','Abr','Mai','Jun','Jul','Ago','Set','Out','Nov','Dez'][d.getMonth()],rate:Math.round((1-exp/inc)*100),inc,exp});
  }
  if(!months.length)return '';
  const cur=focus==='average'?{...months[months.length-1],rate:Math.round(months.reduce((s,m)=>s+m.rate,0)/months.length)}:months[months.length-1];
  const col=cur.rate>=20?'var(--grn)':cur.rate>=0?'var(--warn)':'var(--dan)';
  return `<div class="box dash-section"><div class="bh"><div class="ct">💹 Taxa de economia</div></div><div style="display:flex;align-items:center;gap:16px;flex-wrap:wrap"><div style="text-align:center"><div style="font-family:var(--font-money);font-size:42px;font-weight:800;color:${col};letter-spacing:0">${cur.rate}%</div><div style="font-size:11px;color:var(--mt)">${focus==='average'?'media do periodo':'este mes'}</div></div><div style="flex:1;min-width:120px">${months.map(m=>{const c=m.rate>=20?'var(--grn)':m.rate>=0?'var(--warn)':'var(--dan)';return `<div style="display:flex;align-items:center;gap:8px;margin-bottom:6px"><span style="font-size:11px;color:var(--mt);width:28px">${m.label}</span><div style="flex:1;height:8px;background:var(--sf2);border-radius:99px;overflow:hidden"><div style="width:${Math.min(Math.abs(m.rate),100)}%;height:100%;background:${c};border-radius:99px"></div></div><span style="font-size:11px;font-weight:700;color:${c};width:34px;text-align:right">${m.rate}%</span></div>`;}).join('')}</div></div></div>`;
}
function widgetBarCats(){
  const limit=Number((widgetFilters?.barcats?.limit)||6);
  const scope=(widgetFilters?.barcats?.scope)||'month';
  const txM=(scope==='30d'?S.transactions.filter(t=>t.date>=widgetRangeDate('30d')):getMonthTx(curDt)).filter(t=>t.type==='expense'&&!isFut(t.date)&&!t.paid);
  const cats={};
  txM.forEach(t=>cats[t.category]=(cats[t.category]||0)+t.amount);
  const sorted=Object.entries(cats).sort((a,b)=>b[1]-a[1]).slice(0,limit);
  if(!sorted.length)return '';
  const max=sorted[0][1];
  return `<div class="box dash-section"><div class="bh"><div><div class="ct">📉 Top categorias</div><div class="cs">${scope==='30d'?'Maiores gastos dos ultimos 30 dias':'Maiores gastos do mes'}</div></div></div><div style="display:flex;flex-direction:column;gap:10px">${sorted.map(([cat,val],i)=>{const c=getCat(cat);const pct=(val/max*100).toFixed(0);const cols=['var(--dan)','var(--warn)','var(--ac)','var(--ac2)','var(--fut)','var(--grn)'];const col=cols[i]||'var(--mt)';return `<div><div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px"><span style="font-size:13px">${c.ico} ${cat}</span><span style="font-family:var(--font-money);font-size:13px;font-weight:700;color:${col}">${fmt(val)}</span></div><div style="height:6px;background:var(--sf2);border-radius:99px;overflow:hidden"><div style="width:${pct}%;height:100%;background:${col};border-radius:99px"></div></div></div>`;}).join('')}</div></div>`;
}
function widgetVehicles(){
  loadCar();
  const vehicles=carVehicles();
  if(!vehicles.length)return '';
  const period=(widgetFilters?.vehicles?.period)||'90d';
  const limit=Number((widgetFilters?.vehicles?.limit)||3);
  const scope=(widgetFilters?.vehicles?.scope)||'active';
  const previousFilter={...carFilters};
  const activeId=carState.activeVehicleId||vehicles[0]?.id||'all';
  carFilters.vehicle=scope==='all'?'all':(activeId||'all');
  carFilters.period=period;
  carFilters.type='all';
  carFilters.kind='all';
  carFilters.query='';
  carFilters.sort='date_desc';
  const stats=carStats();
  const maint=carMaintenanceInsights(stats.events);
  Object.assign(carFilters,previousFilter);
  const usesFallbackMetrics=stats.metricSource==='fallback_latest_pair';
  const fuelNote=stats.liters?(usesFallbackMetrics?`calculado pelo ultimo intervalo valido (${stats.liters.toLocaleString('pt-BR',{maximumFractionDigits:1})} L)`:`${stats.liters.toLocaleString('pt-BR',{maximumFractionDigits:1})} L medidos`):'precisa de pelo menos 2 abastecimentos';
  const distanceNote=stats.distance?(usesFallbackMetrics?`ultimo intervalo valido: ${Math.round(stats.distance).toLocaleString('pt-BR')} km`:`${Math.round(stats.distance).toLocaleString('pt-BR')} km calculados`):'sem distancia suficiente';
  const activeVehicle=carVehicle(activeId);
  const recent=[...carState.events].filter(e=>scope==='all'||e.vehicleId===activeId).sort((a,b)=>b.date.localeCompare(a.date)||b.createdAt-a.createdAt).slice(0,limit);
  const dueTone=maint.upcomingStatus==='urgent'?'var(--dan)':maint.upcomingStatus==='warn'?'var(--warn)':'var(--ac)';
  const dueText=maint.kmLeft!==null?`${Math.round(maint.kmLeft).toLocaleString('pt-BR')} km restantes`:maint.nextOilDate?`proxima revisao ate ${fmtD(maint.nextOilDate)}`:'cadastre uma troca de oleo ou revisao';
  const headerMeta=[scope==='all'?'todos os veiculos':activeVehicle?.model,activeVehicle?.plate,activeVehicle?.odometer?`${Math.round(activeVehicle.odometer).toLocaleString('pt-BR')} km`:'' ].filter(Boolean).join(' • ');
  return `<div class="vehicle-widget dash-section">
    <div class="bh"><div><div class="ct">Veiculos</div><div class="cs">${vehicles.length} veiculo${vehicles.length!==1?'s':''} • ${scope==='all'?'visao consolidada':'foco no ativo'}</div></div><button class="btn btn-g btn-sm" onclick="showPage('car')">Abrir modulo</button></div>
    <div class="vehicle-widget-hero" onclick="showPage('car')"><div><div class="vehicle-widget-name">${esc(scope==='all'?'Frota pessoal':(activeVehicle?.name||'Meu carro'))}</div><div class="vehicle-widget-meta">${esc(headerMeta||'Consumo, manutencao e historico')}</div></div><div class="vehicle-widget-kpi"><span class="vehicle-widget-kpi-label">Gasto ${period==='30d'?'30 dias':period==='year'?'ano':period==='all'?'total':'90 dias'}</span><strong>${fmt(stats.total)}</strong></div></div>
    <div class="vehicle-widget-grid">
      <div class="vehicle-widget-card"><div class="vehicle-widget-label">Consumo medio</div><div class="vehicle-widget-value" style="color:var(--ac2)">${stats.kmPerLiter?`${stats.kmPerLiter.toFixed(1).replace('.',',')} km/l`:'—'}</div><div class="vehicle-widget-note">${fuelNote}</div></div>
      <div class="vehicle-widget-card"><div class="vehicle-widget-label">Custo por km</div><div class="vehicle-widget-value" style="color:var(--warn)">${stats.costPerKm?fmt(stats.costPerKm):'—'}</div><div class="vehicle-widget-note">${distanceNote}</div></div>
      <div class="vehicle-widget-card"><div class="vehicle-widget-label">Proxima manutencao</div><div class="vehicle-widget-value" style="color:${dueTone}">${maint.nextOilKm?`${Math.round(maint.nextOilKm).toLocaleString('pt-BR')} km`:'Revisar'}</div><div class="vehicle-widget-note">${esc(dueText)}</div></div>
    </div>
    <div class="vehicle-widget-list">${recent.length?recent.map(e=>{const icon=e.type==='fuel'?'⛽':'🔧';const title=e.type==='fuel'?(e.fuelType||'Abastecimento'):(e.title||carExpenseLabel(e.category));const meta=[scope==='all'?vehicleName(e.vehicleId):'',fmtD(e.date),e.odometer?`${Math.round(e.odometer).toLocaleString('pt-BR')} km`:'',e.note].filter(Boolean).join(' • ');return `<button class="vehicle-widget-row" onclick="openSearchTarget('car-event','${e.id}','car')"><span class="vehicle-widget-row-ico">${icon}</span><span class="vehicle-widget-row-main"><strong>${esc(title)}</strong><small>${esc(meta||'Sem detalhes')}</small></span><span class="vehicle-widget-row-amt">${fmt(e.amount)}</span></button>`;}).join(''):`<div class="vehicle-widget-empty">Adicione abastecimentos ou despesas para montar os insights do veiculo.</div>`}</div>
  </div>`;
}

function renderDash(force=false) {
  dashDirty = true;
  if(!force && !isDashboardActive()){
    renderDashboardManager();
    return;
  }
  const isDark = document.documentElement.dataset.theme === 'dark';
  const ids=WIDGET_DEFS.map(w=>w.id);
  widgetPrefs=asObj(widgetPrefs);
  widgetOrder=asArr(widgetOrder);
  widgetFilters=asObj(widgetFilters);
  const fallbackOrder=defaultDashboardWidgetOrder();
  widgetOrder=[...widgetOrder.filter(id=>ids.includes(id)),...fallbackOrder.filter(id=>!widgetOrder.includes(id))];
  ensureAtLeastOneWidget();
  const sections=dashboardVisibleWidgetIds().map(id=>dashboardWidgetMarkup(id,isDark)).filter(Boolean);

  const container = document.getElementById('dashWidgets');
  if (container) container.innerHTML = sections.join('');
  dashDirty = false;
  renderLastDataNotice();
  renderDashboardManager();

  dashboardVisibleWidgetIds().forEach(id=>dashboardWidgetPostRender(id,isDark));
}
document.addEventListener('click',e=>{
  if(!activeWidgetMenuId)return;
  if(e.target.closest('.widget-menu-shell'))return;
  closeWidgetMenu();
});
