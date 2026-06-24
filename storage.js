// Storage e estado local
// Extraido de index.html para organizar o frontend sem mudar a arquitetura global.

function defaultYieldRate(type){return type==='savings'?0.55:type==='investment'?0.8:0;}
function normalizeAccount(a){
  const type=a.type||'checking';
  const raw=a.yieldRate ?? a.yield_rate;
  const yieldRate=raw===undefined||raw===null||raw===''?defaultYieldRate(type):parseFloat(raw)||0;
  const closingRaw=a.cardClosingDay ?? a.card_closing_day ?? a.closingDay ?? a.closing_day;
  const dueRaw=a.cardDueDay ?? a.card_due_day ?? a.dueDay ?? a.due_day;
  return {
    ...a,
    type,
    balance:parseFloat(a.balance)||0,
    yieldRate,
    yieldType:a.yieldType||a.yield_type||(type==='investment'?'cdi_pct':'manual'),
    yieldVal:parseFloat(a.yieldVal ?? a.yield_val ?? (type==='investment'?100:yieldRate))||0,
    calcBase:a.calcBase||a.calc_base||'du',
    startDate:a.startDate||a.start_date||'',
    cardClosingDay:Math.min(31,Math.max(0,parseInt(closingRaw,10)||0)),
    cardDueDay:Math.min(31,Math.max(0,parseInt(dueRaw,10)||0)),
    cardLast4:String(a.cardLast4||a.card_last4||a.last4||'').replace(/\D/g,'').slice(-4),
    cardExpiry:String(a.cardExpiry||a.card_expiry||a.expiry||'').trim(),
    note:a.note||''
  };
}
function normalizeState(st){
  const state=st||{};
  return {
    transactions:state.transactions||[],
    budgets:state.budgets||[],
    goals:state.goals||[],
    accounts:(state.accounts||[]).map(normalizeAccount)
  };
}
function loadLocal(){const r=localStorage.getItem(LK);return normalizeState(r?JSON.parse(r):{transactions:[],budgets:[],goals:[],accounts:[]});}
function saveLocal(){localStorage.setItem(LK,JSON.stringify(S));if(typeof noteLocalSave==='function')noteLocalSave();}
