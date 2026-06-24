'use strict';

function saveTxFilters(){
  const payload={
    srch:document.getElementById('txSrch')?.value||'',
    type:document.getElementById('fTyp')?.value||'all',
    cat:document.getElementById('fCat')?.value||'all',
    acc:document.getElementById('fAcc')?.value||'all',
    sort:document.getElementById('fSort')?.value||'dd',
    min:document.getElementById('fMin')?.value||'',
    max:document.getElementById('fMax')?.value||'',
    period:curTxP||'1m-p'
  };
  localStorage.setItem(TX_FILTERS_KEY,JSON.stringify(payload));
}

function loadTxFilters(){
  try{return JSON.parse(localStorage.getItem(TX_FILTERS_KEY)||'{}');}
  catch{return{};}
}

function applySavedTxFilters(){
  const saved=loadTxFilters();
  if(saved.srch!==undefined&&document.getElementById('txSrch'))document.getElementById('txSrch').value=saved.srch;
  if(saved.type&&document.getElementById('fTyp'))document.getElementById('fTyp').value=saved.type;
  if(saved.acc&&document.getElementById('fAcc'))document.getElementById('fAcc').value=saved.acc;
  if(saved.sort&&document.getElementById('fSort'))document.getElementById('fSort').value=saved.sort;
  if(saved.min!==undefined&&document.getElementById('fMin'))document.getElementById('fMin').value=saved.min;
  if(saved.max!==undefined&&document.getElementById('fMax'))document.getElementById('fMax').value=saved.max;
  curTxP=saved.period||curTxP;
  document.querySelectorAll('[data-p]').forEach(x=>x.classList.toggle('active',x.dataset.p===curTxP));
}

function getRange(p){
  const t=today();
  const m={'7d-p':{from:offD(new Date(),-7),to:t},'1m-p':{from:offD(new Date(),-30),to:t},'3m-p':{from:offD(new Date(),-90),to:t},'6m-p':{from:offD(new Date(),-180),to:t},'all-p':{from:'2000-01-01',to:t},'1m-f':{from:t,to:offD(new Date(),30)},'3m-f':{from:t,to:offD(new Date(),90)},'all-f':{from:t,to:'2099-12-31'},'all':{from:'2000-01-01',to:'2099-12-31'}};
  return m[p]||{from:'2000-01-01',to:'2099-12-31'};
}

function txHTML(tx){
  const cat=getCat(tx.category);const fut=isFut(tx.date);const dl=dDiff(tx.date);
  const meta=getTxImportMeta(tx.id);
  const splitBadge=txSplitBadge(tx);
  const amtCls=fut&&!tx.paid?'fut-c':tx.type==='income'?'income':'expense';
  const acc=S.accounts.find(a=>a.id===tx.accountId);
  let bdgs='';
  if(tx.paid)bdgs+='<span class="bdg bdg-ok">✓ pago</span>';
  else if(tx.pending)bdgs+='<span class="bdg bdg-p">❓ pendente</span>';
  if(meta.imported)bdgs+='<span class="bdg">📥 importado</span>';
  if(splitBadge)bdgs+=`<span class="bdg">${splitBadge}</span>`;
  if(splitNeedsApproval(tx))bdgs+='<span class="bdg bdg-p">🕒 aprovação</span>';
  else if(splitRejected(tx))bdgs+='<span class="bdg">🚫 recusado</span>';
  if(meta.reconciled)bdgs+='<span class="bdg">🔗 conciliado</span>';
  if(tx.installmentNum)bdgs+=`<span class="bdg bdg-i">💳 ${tx.installmentNum}/${tx.installmentTotal}</span>`;
  if(tx.recurGroup)bdgs+='<span class="bdg bdg-r">🔄</span>';
  if(fut&&!tx.paid)bdgs+=`<span class="bdg bdg-f">🔮 ${dl>0?dl+'d':'hoje'}</span>`;
  const delBtn=tx.installmentGroup?`<button class="ib del" ${writeActionAttrs('remover grupos de transações')} onclick="delGrp('${tx.installmentGroup}','installmentGroup')">🗑️</button>`:tx.recurGroup?`<button class="ib del" ${writeActionAttrs('remover grupos de transações')} onclick="delGrp('${tx.recurGroup}','recurGroup')">🗑️</button>`:`<button class="ib del" ${writeActionAttrs('remover transações')} onclick="delTx('${tx.id}')">🗑️</button>`;
  return`<div style="border-radius:14px;overflow:hidden;margin-bottom:0"><div class="ti" id="tx-${tx.id}"><div class="tico" style="background:${cat.col}20">${cat.ico}</div><div class="tinf"><div class="tnm">${tx.desc}</div><div class="tcat"><span class="bdg" style="background:${cat.col}20;color:${cat.col}">${tx.category}</span>${acc?`<span style="font-size:11px;color:var(--mt)">${acc.icon}</span>`:''}${bdgs}${tx.note?`<span style="color:var(--mt);font-size:9px">${tx.note}</span>`:''}</div></div><div class="tr"><div class="tam ${amtCls}">${tx.type==='income'?'+':'-'}${fmt(tx.amount)}</div><div class="tdt">${fmtD(tx.date)}</div></div><div class="tact">${fut||tx.pending?`<button class="ib ok" ${writeActionAttrs('alterar status de pagamento')} onclick="markPaid('${tx.id}')">${tx.paid?'↩':'✓'}</button>`:''}<button class="ib" ${writeActionAttrs('editar transações')} onclick="openModal('${tx.id}')">✏️</button><button class="ib" ${writeActionAttrs('duplicar transações')} onclick="dupTx('${tx.id}')">⧉</button>${delBtn}</div><button class="ib" ${writeActionAttrs('editar transações')} onclick="openInlineEdit('${tx.id}')" title="Edição rápida" style="font-size:11px;color:var(--ac)">⚡</button></div></div><div class="ti-edit-wrap" id="ie-${tx.id}"></div></div>`;
}

function setView(v,options={}){
  const {render=true,persist=true,syncRemote=false}=options;
  if(v==='cal')v='n';
  curView=v;
  if(persist)localStorage.setItem(VK,v);
  if(syncRemote&&cfg.mode==='api')saveRemoteState().catch(()=>{});
  ['N','C','Chart'].forEach(n=>document.getElementById('v'+n)?.classList.toggle('active',n.toLowerCase()===v));
  const chartWrap=document.getElementById('txChartWrap');
  if(chartWrap)chartWrap.style.display=v==='chart'?'block':'none';
  if(render)renderTx();
  if(render&&v==='chart')renderTxCharts();
}

function renderTx(){
  const srch=document.getElementById('txSrch')?.value.toLowerCase()||'';
  const fTyp=document.getElementById('fTyp')?.value||'all';
  const fCat=document.getElementById('fCat')?.value||'all';
  const fAcc=document.getElementById('fAcc')?.value||'all';
  const fSort=document.getElementById('fSort')?.value||'dd';
  const fMin=parseFloat(document.getElementById('fMin')?.value)||0;
  const fMax=parseFloat(document.getElementById('fMax')?.value)||Infinity;
  const range=getRange(curTxP);
  saveTxFilters();
  const catSel=document.getElementById('fCat');
  if(catSel){
    const prev=catSel.value;
    catSel.innerHTML='<option value="all">Categoria</option>'+[...new Set(S.transactions.map(t=>t.category))].sort().map(c=>`<option value="${c}"${c===prev?' selected':''}>${getCat(c).ico} ${c}</option>`).join('');
  }
  renderCatFilterChips(fCat);
  let txs=[...S.transactions].filter(t=>t.date>=range.from&&t.date<=range.to);
  if(fTyp!=='all')txs=txs.filter(t=>t.type===fTyp);
  if(fCat!=='all')txs=txs.filter(t=>t.category===fCat);
  if(fAcc!=='all')txs=txs.filter(t=>t.accountId===fAcc);
  if(fMin>0)txs=txs.filter(t=>t.amount>=fMin);
  if(fMax<Infinity)txs=txs.filter(t=>t.amount<=fMax);
  if(srch)txs=txs.filter(t=>t.desc.toLowerCase().includes(srch)||t.category.toLowerCase().includes(srch)||t.note.toLowerCase().includes(srch));
  txs.sort((a,b)=>({dd:()=>b.date.localeCompare(a.date),da:()=>a.date.localeCompare(b.date),'ad':()=>b.amount-a.amount,'aa':()=>a.amount-b.amount}[fSort]||(()=>0))());
  const tI=txs.filter(t=>t.type==='income').reduce((s,t)=>s+t.amount,0);
  const tE=txs.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amount,0);
  const fE=txs.filter(t=>t.type==='expense'&&isFut(t.date)).reduce((s,t)=>s+t.amount,0);
  const pnd=txs.filter(t=>t.pending).length;
  const inboxCt=txs.filter(t=>t.category==='A classificar'&&(getTxImportMeta(t.id).imported||t.pending)).length;
  document.getElementById('txSum').innerHTML=`<span class="tx-sum-pill pos">Entradas ${fmt(tI)}</span><span class="tx-sum-pill neg">Saídas ${fmt(tE)}</span>${fE>0?`<span class="tx-sum-pill fut">Futuro ${fmt(fE)}</span>`:''}${pnd>0?`<span class="tx-sum-pill warn">${pnd} pendente${pnd>1?'s':''}</span>`:''}${inboxCt>0?`<button class="btn btn-g btn-sm" onclick="openImportInbox()">📥 Caixa de entrada ${inboxCt}</button>`:''}<span class="tx-sum-pill muted">${txs.length} lançamento${txs.length!==1?'s':''}</span>`;
  renderTxInsights(txs);
  const el=document.getElementById('txView');
  const renderLimit=curView==='c'?400:250;
  const visible=txs.slice(0,renderLimit);
  const more=txs.length>visible.length?`<div class="empty" style="padding:18px"><p>Mostrando ${visible.length} de ${txs.length}. Use busca ou filtros para refinar.</p></div>`:'';
  el.innerHTML=`<div class="tl${curView==='c'?' compact':''}">${visible.length?visible.map(txHTML).join('')+more:`<div class="empty"><span class="ei">🔍</span><p>Nenhuma transação no período.</p></div>`}</div>`;
}

function setCatFilter(cat){
  const sel=document.getElementById('fCat');
  if(!sel)return;
  sel.value=cat||'all';
  renderTx();
}

function renderCatFilterChips(active='all'){
  const el=document.getElementById('catFilterChips');
  if(!el)return;
  const cats=[...new Set(S.transactions.map(t=>t.category).filter(Boolean))].sort();
  if(!cats.length){el.innerHTML='';return;}
  el.innerHTML=`<button class="cat-filter-chip ${active==='all'?'active':''}" onclick="setCatFilter('all')">Todas</button>`+
    cats.map(name=>{const c=getCat(name);return`<button class="cat-filter-chip ${active===name?'active':''}" style="--cat:${c.col}" onclick="setCatFilter('${name.replace(/'/g,"\\'")}')"><span>${c.ico}</span>${name}</button>`;}).join('');
}

function renderTxInsights(txs){
  const el=document.getElementById('txInsightCards');
  if(!el)return;
  const expenses=txs.filter(t=>t.type==='expense'&&!t.paid);
  const incomes=txs.filter(t=>t.type==='income'&&!t.paid);
  const exp=expenses.reduce((s,t)=>s+t.amount,0);
  const inc=incomes.reduce((s,t)=>s+t.amount,0);
  const avg=expenses.length?exp/expenses.length:0;
  const top=expenses.reduce((m,t)=>!m||t.amount>m.amount?t:m,null);
  const cats={};expenses.forEach(t=>cats[t.category]=(cats[t.category]||0)+t.amount);
  const topCat=Object.entries(cats).sort((a,b)=>b[1]-a[1])[0];
  el.innerHTML=`
    <div class="insight-card"><div class="insight-k">Saldo no filtro</div><div class="insight-v" style="color:${inc-exp>=0?'var(--ac)':'var(--dan)'}">${fmt(inc-exp)}</div></div>
    <div class="insight-card"><div class="insight-k">Ticket médio</div><div class="insight-v" style="color:var(--warn)">${fmt(avg)}</div><div class="cc">${expenses.length} despesas</div></div>
    <div class="insight-card"><div class="insight-k">Maior gasto</div><div class="insight-v" style="color:var(--dan)">${top?fmt(top.amount):''}</div><div class="cc">${top?top.desc:'sem dados'}</div></div>
    <div class="insight-card"><div class="insight-k">Categoria líder</div><div class="insight-v" style="font-size:16px">${topCat?getCat(topCat[0]).ico+' '+topCat[0]:''}</div><div class="cc">${topCat?fmt(topCat[1]):'sem dados'}</div></div>`;
}

let renderTxTimer=null;
function renderTxDebounced(){clearTimeout(renderTxTimer);renderTxTimer=setTimeout(renderTx,140);}
document.getElementById('txSrch')?.addEventListener('input',renderTxDebounced);
document.getElementById('fTyp')?.addEventListener('change',renderTx);
document.getElementById('fCat')?.addEventListener('change',renderTx);
document.getElementById('fAcc')?.addEventListener('change',renderTx);
document.getElementById('fSort')?.addEventListener('change',renderTx);
