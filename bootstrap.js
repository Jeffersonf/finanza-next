'use strict';

const INITIAL_BOOT_TX_LIMIT=1000;
let fullTxHydrationPromise=null;

function applyRemotePayload(txR,buds,goals,state){
  S.transactions=(txR.data||[]).map(nTx);S.budgets=(buds||[]).map(nBud);S.goals=(goals||[]).map(nGoal);
  S.accounts=(state.accounts||[]).map(nAcc);
  if(!S.accounts.length)S.accounts=defAccs();
  custCats=(state.categories||[]).map(nCat);
  sl={lists:(state.shopping?.lists||[]).map(nShopList),items:(state.shopping?.items||[]).map(nShopItem)};
  if(!sl.lists.length)sl={lists:[{id:uid(),name:'Mercado',ico:'\u{1F6D2}'}],items:[]};
  carState=normalizeCarState(state.car||state.vehicle||state.vehicles||state.settings?.rates?.car||{});
  localStorage.setItem(CAR_KEY,JSON.stringify(carState));
  slActiveList=state.settings?.active_list||sl.lists[0]?.id||null;
  applyRemoteSettings(state.settings||{});
  saveLocal();
  setConn('online');
  if(!(state.accounts||[]).length)saveRemoteState().catch(()=>{});
}

async function hydrateFullTransactionsInBackground(){
  if(cfg.mode!=='api'||fullTxHydrationPromise)return fullTxHydrationPromise;
  fullTxHydrationPromise=(async()=>{
    try{
      const txR=await api('GET','/api/transactions?limit=1000');
      S.transactions=(txR.data||[]).map(nTx);
      saveLocal();
      refreshAll();
      showConnBar('online','Sincronizado',1800);
    }catch(err){
      console.warn('full hydration:',err.message);
    }finally{
      fullTxHydrationPromise=null;
    }
  })();
  return fullTxHydrationPromise;
}

async function loadAll(options={}){
  if(cfg.mode==='local'){
    S=loadLocal();
    if(!S.transactions?.length)seedDemo();
    if(!S.accounts?.length)S.accounts=defAccs();
    setConn('offline');
    return;
  }
  const limit=Math.min(Math.max(parseInt(options.limit,10)||1000,1),1000);
  const backgroundFull=!!options.backgroundFull;
  try{
    const [txR,buds,goals,state]=await Promise.all([
      api('GET',`/api/transactions?limit=${limit}`),
      api('GET','/api/budgets'),
      api('GET','/api/goals'),
      api('GET','/api/state')
    ]);
    applyRemotePayload(txR,buds,goals,state);
    if(backgroundFull&&Number(txR.total||0)>limit)hydrateFullTransactionsInBackground();
  }catch(e){
    console.warn('offline:',e.message);
    const c=loadLocal();
    S=c.transactions?.length?c:{transactions:[],budgets:[],goals:[],accounts:defAccs()};
    if(c.transactions?.length)toast('Offline - cache local','info');
    setConn('error');
  }
}

function primeAppFromCache(){
  if(cfg.mode!=='api')return false;
  let cached=null;
  try{cached=loadLocal();}catch{}
  if(!cached)return false;
  const hasCachedData=!!((cached.transactions||[]).length||(cached.accounts||[]).length||(cached.budgets||[]).length||(cached.goals||[]).length);
  if(!hasCachedData)return false;
  S=cached;
  if(!S.accounts?.length)S.accounts=defAccs();
  loadCC();
  loadCar();
  popCatSels();
  popAccSels();
  updM();
  renderDash();
  return true;
}

async function initApp(){
  loadImportCenterState();
  loadCommitments();
  loadSidebarShortcutPrefs();
  loadWidgetPrefs();
  loadRates();
  loadDueItems();
  const name=cfg.userName||'Eu';
  document.getElementById('uName').textContent=name;
  applyAvatar();
  const bootedFromCache=primeAppFromCache();
  if(bootedFromCache)showConnBar('syncing','Sincronizando...',2200);
  await loadAll({limit:INITIAL_BOOT_TX_LIMIT,backgroundFull:true});
  if(bootedFromCache&&cfg.mode==='api')showConnBar('online','Sincronizado',2200);
  if(cfg.mode==='local')loadCC();
  loadCar();
  popCatSels();
  popAccSels();
  updM();
  renderDash();
  applyAdminPanelVisibility();
  applyWriteAccessUI();
  renderSidebarShortcuts();
  const sv=localStorage.getItem(VK)||'n';
  setView(sv,{render:false,persist:false,syncRemote:false});
  const savedPage=sessionStorage.getItem(PAGE_KEY)||'dashboard';
  if(savedPage!=='dashboard')showPage(savedPage);
  applySavedTxFilters();
  loadSyncQ();
  loadSyncHistory();
  loadAuditHistory();
  await loadPersistentAuditHistory();
  updSyncBadge();
  if(!loadSaveState().status)noteLocalSave(cfg.mode==='api'?'Conta pronta para sincronizar':'Dados prontos neste dispositivo');
  consumeSharedInviteFromUrl();
  updateTrustPanel();
  checkAutoBackup();
  initDeepLink();
  setTimeout(initNotifications, 3000);
  setTimeout(registerNotifActions, 1000);
  setTimeout(setupPersistentNotification, 5000);
}
