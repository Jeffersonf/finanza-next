'use strict';

function getFutEnd(fp){const m={'7d':offD(new Date(),7),'15d':offD(new Date(),15),'1m':offD(new Date(),30),'3m':offD(new Date(),90),'6m':offD(new Date(),180),'all':'2099-12-31'};return m[fp]||offD(new Date(),30);}

function renderFut(){
  const toDate=getFutEnd(curFP);const t=today();
  renderFixedBillsHub();
  renderDueSection(toDate);
  const fE=S.transactions.filter(x=>x.type==='expense'&&x.date>t&&x.date<=toDate&&!x.paid).sort((a,b)=>a.date.localeCompare(b.date));
  const fI=S.transactions.filter(x=>x.type==='income'&&x.date>t&&x.date<=toDate&&!x.paid).sort((a,b)=>a.date.localeCompare(b.date));
  const ovd=S.transactions.filter(x=>x.type==='expense'&&x.installmentGroup&&x.date<t&&!x.paid).sort((a,b)=>a.date.localeCompare(b.date));
  const tE=fE.reduce((s,x)=>s+x.amount,0);const tI=fI.reduce((s,x)=>s+x.amount,0);const tO=ovd.reduce((s,x)=>s+x.amount,0);const sal=tI-tE;
  document.getElementById('futDbt').textContent=fmt(tE);document.getElementById('futInc').textContent=fmt(tI);document.getElementById('futOvd').textContent=fmt(tO);
  const bEl=document.getElementById('futBal');bEl.textContent=fmt(sal);bEl.className='cv '+(sal>=0?'pos':'neg');
  document.getElementById('futBalLbl').textContent=sal>=0?'positivo ✓':`faltam ${fmt(Math.abs(sal))}`;
  if(fE.length){const n=fE[0],dl=dDiff(n.date);document.getElementById('futNxt').textContent=fmtD(n.date);document.getElementById('futNxtN').textContent=`${n.desc}  em ${dl}d`;}
  else{document.getElementById('futNxt').textContent='';document.getElementById('futNxtN').textContent='Nenhum';}
  const ovdS=document.getElementById('ovdSec');
  if(ovd.length){ovdS.style.display='block';document.getElementById('ovdList').innerHTML=ovd.map(x=>{const dl=Math.abs(dDiff(x.date));const c=getCat(x.category);return`<div class="ti ovd-tx"><div class="tico" style="background:rgba(245,112,90,.12)">${c.ico}</div><div class="tinf"><div class="tnm">${x.desc}</div><div class="tcat"><span class="bdg bdg-o">⚠️ ${dl}d atrasado</span></div></div><div class="tr"><div class="tam expense">-${fmt(x.amount)}</div><div class="tdt">${fmtD(x.date)}</div></div><div class="tact"><button class="ib ok" onclick="markPaid('${x.id}')">✓</button><button class="ib" onclick="openModal('${x.id}')">✏️</button><button class="ib del" onclick="delTx('${x.id}')">🗑️</button></div></div>`;}).join('');}
  else ovdS.style.display='none';
  const incS=document.getElementById('futIncSec');
  if(fI.length){incS.style.display='block';document.getElementById('futIncList').innerHTML=fI.map(x=>{const dl=dDiff(x.date);const c=getCat(x.category);return`<div class="ti" style="border-left:3px solid var(--ac);background:rgba(200,245,90,.03)"><div class="tico" style="background:rgba(200,245,90,.1)">${c.ico}</div><div class="tinf"><div class="tnm">${x.desc}</div><div class="tcat"><span class="bdg bdg-f" style="color:var(--ac)">💰 em ${dl}d</span><span class="bdg" style="background:${c.col}20;color:${c.col}">${x.category}</span></div></div><div class="tr"><div class="tam income">+${fmt(x.amount)}</div><div class="tdt">${fmtD(x.date)}</div></div><div class="tact"><button class="ib ok" onclick="markPaid('${x.id}')">✓</button><button class="ib" onclick="openModal('${x.id}')">✏️</button><button class="ib del" onclick="delTx('${x.id}')">🗑️</button></div></div>`;}).join('');}
  else incS.style.display='none';
  document.getElementById('futExpList').innerHTML=fE.length?fE.map(x=>{const dl=dDiff(x.date);const c=getCat(x.category);const urg=dl<=3?'var(--dan)':dl<=7?'var(--warn)':'var(--fut)';return`<div class="ti fut-tx"><div class="tico" style="background:rgba(167,139,250,.1)">${c.ico}</div><div class="tinf"><div class="tnm">${x.desc}</div><div class="tcat"><span class="bdg" style="background:rgba(167,139,250,.12);color:${urg}">🔮 em ${dl}d</span><span class="bdg" style="background:${c.col}20;color:${c.col}">${x.category}</span>${x.installmentNum?`<span class="bdg bdg-i">💳 ${x.installmentNum}/${x.installmentTotal}</span>`:''}</div></div><div class="tr"><div class="tam fut-c">-${fmt(x.amount)}</div><div class="tdt">${fmtD(x.date)}</div></div><div class="tact"><button class="ib ok" onclick="markPaid('${x.id}')">✓</button><button class="ib" onclick="openModal('${x.id}')">✏️</button><button class="ib" onclick="dupTx('${x.id}')">⧉</button><button class="ib del" onclick="${x.installmentGroup?`delGrp('${x.installmentGroup}','installmentGroup')`:x.recurGroup?`delGrp('${x.recurGroup}','recurGroup')`:`delTx('${x.id}')`}">🗑️</button></div></div>`;}).join(''):`<div class="empty"><span class="ei">🔮</span><p>Nenhum gasto futuro no período.</p></div>`;
  const center=document.getElementById('futureCommandCenter');
  if(center){
    const urgent=[...ovd.map(x=>({name:x.desc,date:x.date,amount:x.amount,kind:'Atrasado',color:'var(--dan)'})),...fE.filter(x=>dDiff(x.date)<=7).map(x=>({name:x.desc,date:x.date,amount:x.amount,kind:dDiff(x.date)<=3?'Vence logo':'Próximo',color:dDiff(x.date)<=3?'var(--dan)':'var(--warn)'}))].sort((a,b)=>a.date.localeCompare(b.date)).slice(0,6);
    const horizon=getFutEnd('1m');
    const byCategory={};
    [...fE.filter(x=>x.date<=horizon),...dueOccurrences(horizon).map(o=>({category:o.item.category||'A classificar',amount:o.item.amount}))].forEach(item=>{byCategory[item.category]=(byCategory[item.category]||0)+item.amount;});
    const topCats=Object.entries(byCategory).sort((a,b)=>b[1]-a[1]).slice(0,4);
    const total30=Object.values(byCategory).reduce((s,v)=>s+v,0);
    center.innerHTML=`
      <div class="future-center-grid">
        <div class="future-center-card">
          <div class="ct">Comando rápido</div>
          <div class="cs">Atenção imediata para os próximos dias</div>
          <div class="future-center-list">${urgent.length?urgent.map(item=>`<div class="future-center-row"><div><div class="future-center-name">${esc(item.name)}</div><div class="future-center-meta" style="color:${item.color}">${item.kind} • ${fmtD(item.date)}</div></div><div class="future-center-amt">${fmt(item.amount)}</div></div>`).join(''):'<div class="sync-empty">Sem contas urgentes neste horizonte.</div>'}</div>
        </div>
        <div class="future-center-card">
          <div class="ct">Peso dos próximos 30 dias</div>
          <div class="cs">Categorias que mais apertam seu caixa</div>
          <div class="future-center-list">${topCats.length?topCats.map(([name,total])=>`<div class="future-center-row"><div><div class="future-center-name">${esc(name)}</div><div class="future-center-meta">${Math.round((total/Math.max(total30,1))*100)}% do previsto no mês</div></div><div class="future-center-amt">${fmt(total)}</div></div>`).join(''):'<div class="sync-empty">Sem compromissos fortes no próximo mês.</div>'}</div>
        </div>
      </div>`;
  }
}

function renderFixedBillsHub(){
  const el=document.getElementById('fixedBillsHub');if(!el)return;
  const activeFixed=dueItems.filter(d=>d.active&&d.recurrence==='monthly');
  const monthlyTotal=activeFixed.reduce((s,d)=>s+d.amount,0);
  const byName=new Set(activeFixed.map(d=>normalizeTxText(d.name)));
  const missing=FIXED_BILL_PRESETS.filter(p=>!byName.has(normalizeTxText(p.name))).slice(0,6);
  el.innerHTML=`
    <div class="bh">
      <div>
        <div class="ct">Contas fixas</div>
        <div class="cs">Aluguel, água, luz, internet e recorrentes entram direto na previsão</div>
      </div>
      <button class="btn btn-g btn-sm" onclick="openDueModal()">+ Personalizada</button>
    </div>
    <div class="fixed-bills-summary">
      <div><span>Total mensal previsto</span><strong>${fmt(monthlyTotal)}</strong></div>
      <div><span>Recorrentes ativas</span><strong>${activeFixed.length}</strong></div>
      <div><span>Próximas sugestões</span><strong>${missing.length}</strong></div>
    </div>
    <div class="fixed-bills-grid">
      ${FIXED_BILL_PRESETS.map(p=>{const exists=byName.has(normalizeTxText(p.name));return `<button class="fixed-bill-card ${exists?'done':''}" onclick="openFixedBillPreset('${p.id}')"><span>${p.icon}</span><strong>${p.name}</strong><small>${exists?'já cadastrada':'mensal • dia '+p.day}</small></button>`;}).join('')}
    </div>`;
}

function openFixedBillPreset(id){
  const p=FIXED_BILL_PRESETS.find(x=>x.id===id);if(!p)return;
  openDueModal();
  const next=new Date();
  const lastDay=new Date(next.getFullYear(),next.getMonth()+1,0).getDate();
  next.setDate(Math.min(p.day,lastDay));
  if(next.toISOString().split('T')[0]<today())next.setMonth(next.getMonth()+1);
  const date=next.toISOString().split('T')[0];
  document.getElementById('dueName').value=p.name;
  document.getElementById('dueDate').value=date;
  document.getElementById('dueRec').value='monthly';
  document.getElementById('dueMethod').value=p.method;
  document.getElementById('duePlace').value=p.place;
  document.getElementById('dueCat').value=p.category;
  document.getElementById('dueNotes').value='Conta fixa mensal';
  setTimeout(()=>document.getElementById('dueAmount')?.focus(),80);
}

function dueKey(date){return date.substring(0,7);}
function dueDateForMonth(item,ym){
  const [y,m]=ym.split('-').map(Number);
  const last=new Date(y,m,0).getDate();
  return `${y}-${String(m).padStart(2,'0')}-${String(Math.min(item.dueDay,last)).padStart(2,'0')}`;
}
function dueOccurrences(toDate){
  const from=today();
  const out=[];
  dueItems.filter(d=>d.active).forEach(item=>{
    if(item.recurrence==='once'){
      const date=item.nextDueDate;
      if(date>=from&&date<=toDate&&!item.paidKeys.includes(dueKey(date)))out.push({item,date,key:dueKey(date)});
      return;
    }
    let d=new Date(from+'T12:00:00');
    d.setDate(1);
    for(let i=0;i<18;i++){
      const ym=`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`;
      const date=dueDateForMonth(item,ym);
      if(date>=from&&date<=toDate&&!item.paidKeys.includes(ym))out.push({item,date,key:ym});
      d.setMonth(d.getMonth()+1);
    }
  });
  return out.sort((a,b)=>a.date.localeCompare(b.date));
}
function methodLabel(m){
  return ({pix:'Pix',boleto:'Boleto',credit:'Cartão principal',store_card:'Cartão próprio/loja',debit:'Débito automático',financing:'Crediário',cash:'Dinheiro'}[m]||m||'Pagar');
}
function renderDueSection(toDate){
  const list=document.getElementById('dueList');if(!list)return;
  const occ=dueOccurrences(toDate);
  const overdue=dueItems.filter(d=>d.active).flatMap(item=>{
    const date=item.recurrence==='once'?item.nextDueDate:dueDateForMonth(item,dueKey(today()));
    return date<today()&&!item.paidKeys.includes(dueKey(date))?[{item,date,key:dueKey(date)}]:[];
  }).sort((a,b)=>a.date.localeCompare(b.date));
  const all=[...overdue,...occ].filter((x,i,a)=>a.findIndex(y=>y.item.id===x.item.id&&y.date===x.date)===i);
  const total=occ.reduce((s,x)=>s+x.item.amount,0);
  const next=all[0];
  document.getElementById('dueTotal').textContent=fmt(total);
  document.getElementById('dueCount').textContent=String(occ.length);
  document.getElementById('dueNext').textContent=next?fmtD(next.date):'—';
  document.getElementById('dueNextName').textContent=next?next.item.name:'sem vencimentos';
  list.innerHTML=all.length?all.map(dueHTML).join(''):`<div class="empty"><span class="ei">📌</span><p>Nenhum vencimento cadastrado nesse período.</p></div>`;
}
function dueHTML(o){
  const item=o.item,c=getCat(item.category),dl=dDiff(o.date),late=o.date<today();
  const acc=S.accounts.find(a=>a.id===item.accountId);
  const tone=late?'var(--dan)':dl<=3?'var(--warn)':'var(--fut)';
  return `<div style="border-radius:14px;overflow:hidden"><div class="ti fut-tx" style="border-left-color:${tone}"><div class="tico" style="background:${c.col}20">${c.ico}</div><div class="tinf"><div class="tnm">${item.name}</div><div class="tcat"><span class="bdg" style="background:rgba(167,139,250,.12);color:${tone}">${late?'atrasado':dl===0?'vence hoje':'em '+dl+'d'}</span><span class="bdg">${methodLabel(item.paymentMethod)}</span>${item.paymentPlace?`<span class="bdg">${item.paymentPlace}</span>`:''}${acc?`<span class="bdg">${acc.icon} ${acc.name}</span>`:''}${item.notes?`<span style="color:var(--mt);font-size:9px">${item.notes}</span>`:''}</div></div><div class="tr"><div class="tam fut-c">-${fmt(item.amount)}</div><div class="tdt">${fmtD(o.date)}</div></div><div class="tact"><button class="ib ok" onclick="payDue('${item.id}','${o.date}')" title="Pagar">✓</button><button class="ib" onclick="postponeDue('${item.id}','${o.date}','week')" title="Adiar 7 dias">⏭️</button><button class="ib" onclick="openDueInline('${item.id}')" title="Editar inline">✏️</button><button class="ib" onclick="openDueModal('${item.id}')" title="Editar completo">⋯</button><button class="ib del" onclick="delDue('${item.id}')" title="Remover">🗑️</button></div></div><div class="due-inline-wrap" id="due-inline-${item.id}"></div></div>`;
}
function openDueInline(id){
  const item=dueItems.find(x=>x.id===id);if(!item)return;
  document.querySelectorAll('.due-inline-wrap').forEach(el=>{if(el.id!==`due-inline-${id}`)el.innerHTML='';});
  const el=document.getElementById(`due-inline-${id}`);if(!el)return;
  const catOpts=allCats().map(c=>`<option value="${esc(c.name)}" ${c.name===item.category?'selected':''}>${c.ico} ${esc(c.name)}</option>`).join('');
  el.innerHTML=`<div class="due-inline-edit">
    <input class="ti-edit-inp" id="dueInName-${id}" value="${esc(item.name)}" placeholder="Nome">
    <input class="ti-edit-inp amount" id="dueInAmount-${id}" type="number" min="0" step="0.01" value="${item.amount||''}" placeholder="Valor">
    <input class="ti-edit-inp" id="dueInDate-${id}" type="date" value="${item.nextDueDate||today()}">
    <select class="ti-edit-inp" id="dueInCat-${id}">${catOpts}</select>
    <button class="btn btn-p btn-sm" onclick="saveDueInline('${id}')">Salvar</button>
    <button class="btn btn-g btn-sm" onclick="document.getElementById('due-inline-${id}').innerHTML=''">Cancelar</button>
  </div>`;
  document.getElementById(`dueInName-${id}`)?.focus();
}
function saveDueInline(id){
  if(!requireWriteAccess('editar vencimentos'))return;
  const item=dueItems.find(x=>x.id===id);if(!item)return;
  const name=document.getElementById(`dueInName-${id}`)?.value.trim();
  const amount=parseFloat(document.getElementById(`dueInAmount-${id}`)?.value)||0;
  const date=document.getElementById(`dueInDate-${id}`)?.value||today();
  if(!name||!amount){toast('Informe nome e valor','error');return;}
  item.name=name;item.amount=amount;item.nextDueDate=date;item.dueDay=new Date(date+'T12:00:00').getDate();
  item.category=document.getElementById(`dueInCat-${id}`)?.value||item.category;
  saveDueItems();renderFut();toast('Vencimento atualizado inline','success');
}
function openDueModal(id=null){
  if(!requireWriteAccess(id?'editar vencimentos':'criar vencimentos'))return;
  const d=id?dueItems.find(x=>x.id===id):null;
  document.getElementById('dueId').value=d?.id||'';
  document.getElementById('dueName').value=d?.name||'';
  document.getElementById('dueAmount').value=d?.amount||'';
  document.getElementById('dueDate').value=d?.nextDueDate||today();
  document.getElementById('dueRec').value=d?.recurrence||'monthly';
  document.getElementById('dueMethod').value=d?.paymentMethod||'pix';
  document.getElementById('duePlace').value=d?.paymentPlace||'';
  document.getElementById('dueNotes').value=d?.notes||'';
  popCatSels();
  document.getElementById('dueCat').innerHTML=allCats().map(c=>`<option value="${c.name}">${c.ico} ${c.name}</option>`).join('');
  document.getElementById('dueCat').value=d?.category||'A classificar';
  document.getElementById('dueAcc').innerHTML='<option value="">Sem conta/cartão</option>'+S.accounts.map(a=>`<option value="${a.id}">${a.icon} ${a.name}</option>`).join('');
  document.getElementById('dueAcc').value=d?.accountId||'';
  document.getElementById('dueModal').classList.add('open');
}
function saveDue(){
  if(!requireWriteAccess(document.getElementById('dueId').value?'editar vencimentos':'criar vencimentos'))return;
  const date=document.getElementById('dueDate').value||today();
  const item=nDue({
    id:document.getElementById('dueId').value||uid(),
    name:document.getElementById('dueName').value.trim(),
    amount:parseFloat(document.getElementById('dueAmount').value)||0,
    category:document.getElementById('dueCat').value,
    recurrence:document.getElementById('dueRec').value,
    nextDueDate:date,
    dueDay:new Date(date+'T12:00:00').getDate(),
    paymentMethod:document.getElementById('dueMethod').value,
    paymentPlace:document.getElementById('duePlace').value.trim(),
    accountId:document.getElementById('dueAcc').value,
    notes:document.getElementById('dueNotes').value.trim(),
    paidKeys:dueItems.find(x=>x.id===document.getElementById('dueId').value)?.paidKeys||[]
  });
  if(!item.name||!item.amount){toast('Informe nome e valor','error');return;}
  const i=dueItems.findIndex(x=>x.id===item.id);
  if(i>=0)dueItems[i]=item;else dueItems.push(item);
  saveDueItems();
  logAuditEvent('Vencimento salvo',item.recurrence==='monthly'?'Conta fixa':'Previsão',`${item.name} • ${fmt(item.amount)}`);
  closeM('dueModal');renderFut();toast('Vencimento salvo','success');setTimeout(scheduleVencimentoNotifications,500);
}
async function payDue(id,date){
  if(!requireWriteAccess('marcar vencimentos como pagos'))return;
  const item=dueItems.find(x=>x.id===id);if(!item)return;
  const key=dueKey(date);
  if(!item.paidKeys.includes(key))item.paidKeys.push(key);
  const tx={id:uid(),type:'expense',desc:item.name,amount:item.amount,category:item.category,date,note:`${methodLabel(item.paymentMethod)}${item.paymentPlace?' • '+item.paymentPlace:''}`,accountId:item.accountId||S.accounts[0]?.id||null,installmentGroup:null,installmentNum:null,installmentTotal:null,recurGroup:null,paid:true,pending:false};
  try{
    if(cfg.mode==='api'){const r=await api('POST','/api/transactions',{type:'expense',description:tx.desc,amount:tx.amount,category:tx.category,date,note:tx.note,account_id:tx.accountId,paid:true,pending:false});S.transactions.unshift(nTx({...r,accountId:tx.accountId}));}
    else{S.transactions.unshift(tx);saveLocal();}
    saveDueItems();refreshAll();toast('Pago e lançado nas transações','success');
  }catch(e){toast('Erro: '+e.message,'error');}
}
function delDue(id){
  if(!requireWriteAccess('remover vencimentos'))return;
  if(!confirm('Remover este vencimento?'))return;
  const removed=dueItems.find(x=>x.id===id);if(!removed)return;
  dueItems=dueItems.filter(x=>x.id!==id);
  removeCommitmentLinkFromDeletedDue(id);
  saveDueItems();renderFut();
  queueUndo(`Vencimento removido: ${removed.name}`,async()=>{dueItems.push(removed);saveDueItems();renderFut();});
  toast('Vencimento removido','info');
}
function postponeDue(id,date,mode='week'){
  if(!requireWriteAccess('adiar vencimentos'))return;
  const item=dueItems.find(x=>x.id===id);if(!item)return;
  const prev={...item,paidKeys:[...(item.paidKeys||[])]};
  const nextDate=mode==='month'?addM(date,1):offD(new Date(date+'T12:00:00'),7);
  item.nextDueDate=nextDate;
  item.dueDay=new Date(nextDate+'T12:00:00').getDate();
  if(item.recurrence!=='once'){
    const currentKey=dueKey(date);
    if(!item.paidKeys.includes(currentKey))item.paidKeys.push(currentKey);
  }
  saveDueItems();
  renderFut();
  queueUndo(`Vencimento adiado: ${item.name}`,async()=>{const idx=dueItems.findIndex(x=>x.id===id);if(idx>=0)dueItems[idx]=prev;saveDueItems();renderFut();});
  toast(`Vencimento adiado para ${fmtD(nextDate)}`,'info');
}
