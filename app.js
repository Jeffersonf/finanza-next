'use strict';
const APP_VERSION='5.0.1-next';
const DEFAULT_API_URL='https://finanza-api.onrender.com';
const CK='fz_cfg',LK='fz_local',CCK='fz_cats',VK='fz_view',AVK='fz_avatar',PRIVK='fz_privacy',CAR_KEY='fz_car',PAGE_KEY='fz_page';
const RATES_KEY='fz_rates', WIDGET_ORDER_KEY='fz_widget_order', WIDGET_FILTER_KEY='fz_widget_filters', DUE_KEY='fz_due_items', TX_FILTERS_KEY='fz_tx_filters', COMMITMENTS_KEY='fz_commitments', SIDEBAR_SHORTCUTS_KEY='fz_sidebar_shortcuts';
const SAVE_STATE_KEY='fz_save_state', SYNC_HISTORY_KEY='fz_sync_history', AUDIT_HISTORY_KEY='fz_audit_history';
let monthlyIncomeCents=0;
let dueItems=[];
let cfg={url:'',key:'',mode:'',userName:'',userId:'',role:'',twoFactorEnabled:false};
let S={transactions:[],budgets:[],goals:[],accounts:[]};
let custCats=[];
let sharedSpace={mode:'couple',name:'',ownerPersonId:'',people:[]};
let carState={vehicles:[],events:[],activeVehicleId:''};
let commitmentsState={subscriptions:[],debts:[],contracts:[]};
let carFilters={vehicle:'active',period:'month',type:'all',kind:'all',query:'',sort:'date_desc',from:'',to:''};
let curTxP='1m-p',curFP='7d',curDt=new Date();
let chartMode='bars',curView='n',catFilter=null,carChartMode='bars';
let editId=null,accEditId=null,qaTyp='expense',qaVal='',qaSelCat='';
let privacyMode=false;
let syncHistory=[];
let auditHistory=[];
let adminUsers=[];
let adminOverviewState={loading:false,loaded:false,data:null,error:''};
let undoState=null;
let importPreviewState=null;
let pendingTwoFactorSecret='';
let subscriptionEditId=null,debtEditId=null,contractEditId=null;
const IMPORT_CENTER_KEY='fz_import_center';
const IMPORT_BATCH_PREFIX='imp_';
let importCenterState={profiles:{csv:{}},rules:{categories:[],subscriptions:[]},snapshots:[],txMeta:{}};
let importDraft={source:'csv',files:[],rows:[],headers:[],mapping:{},dedupe:'exact',profileName:'',text:'',batchId:'',balanceDivergence:null};
let srchScope='all';
const uid=()=>Date.now().toString(36)+Math.random().toString(36).slice(2);
function persistCfg(){localStorage.setItem(CK,JSON.stringify(cfg));}
function applyPerformanceMode(){
  document.documentElement.dataset.perf=localStorage.getItem('fz_perf_mode')||'lite';
}
const rawFmt=n=>'R$ '+Number(n).toLocaleString('pt-BR',{minimumFractionDigits:2,maximumFractionDigits:2});
const fmt=n=>privacyMode?'R$ •••':rawFmt(n);
const fmtD=d=>{if(!d)return'';const[y,m,day]=d.substring(0,10).split('-');return`${day}/${m}/${y}`;};
const today=()=>new Date().toISOString().split('T')[0];
const isFut=d=>d>today();
const dDiff=d=>Math.ceil((new Date(d+'T12:00:00')-new Date())/864e5);
const offD=(b,n)=>{const d=new Date(b);d.setDate(d.getDate()+n);return d.toISOString().split('T')[0];};
const addM=(s,n)=>{const d=new Date(s+'T12:00:00');d.setMonth(d.getMonth()+n);return d.toISOString().split('T')[0];};
const addW=(s,n)=>offD(new Date(s),n*7);
const addY=(s,n)=>{const d=new Date(s+'T12:00:00');d.setFullYear(d.getFullYear()+n);return d.toISOString().split('T')[0];};
const MO=['Janeiro','Fevereiro','Março','Abril','Maio','Junho','Julho','Agosto','Setembro','Outubro','Novembro','Dezembro'];
const RATES={cdi:10.40,selic:10.50,tr:0};
const BCATS=[
  {id:'sal',ico:'\u{1F4BC}',name:'Salário',col:'#c8f55a'},
  {id:'frl',ico:'\u{1F5A5}\uFE0F',name:'Freelance',col:'#f5f55a'},
  {id:'inv',ico:'\u{1F4C8}',name:'Investimentos',col:'#5af55a'},
  {id:'oth',ico:'\u{1F516}',name:'Outros',col:'#a78bfa'},
  {id:'mor',ico:'\u{1F3E0}',name:'Moradia',col:'#5af5c8'},
  {id:'ali',ico:'\u{1F37D}\uFE0F',name:'Alimentação',col:'#f5c85a'},
  {id:'tra',ico:'\u{1F697}',name:'Transporte',col:'#5a9ef5'},
  {id:'car',ico:'\u{1F699}',name:'Carro',col:'#5af5c8'},
  {id:'sau',ico:'\u{1F48A}',name:'Saúde',col:'#f55a9e'},
  {id:'laz',ico:'\u{1F3AC}',name:'Lazer',col:'#c85af5'},
  {id:'edu',ico:'\u{1F4DA}',name:'Educação',col:'#5af55a'},
  {id:'rou',ico:'\u{1F455}',name:'Roupas',col:'#f57c5a'},
  {id:'tec',ico:'\u{1F4BB}',name:'Tecnologia',col:'#5acff5'},
  {id:'ass',ico:'\u{1F4E6}',name:'Assinaturas',col:'#f5a05a'},
  {id:'cls',ico:'\u2753',name:'A classificar',col:'#6b7494'},
];
const CAT_ALIASES={
  Salario:'Salário',
  Alimentacao:'Alimentação',
  Saude:'Saúde',
  Educacao:'Educação',
  Poupanca:'Poupança',
};
const FIXED_BILL_PRESETS=[
  {id:'rent',icon:'🏠',name:'Aluguel',category:'Moradia',method:'pix',day:5,place:'Imobiliária'},
  {id:'water',icon:'💧',name:'Água',category:'Moradia',method:'boleto',day:10,place:'Saneamento'},
  {id:'energy',icon:'💡',name:'Luz',category:'Moradia',method:'boleto',day:12,place:'Energia'},
  {id:'internet',icon:'🌐',name:'Internet',category:'Assinaturas',method:'debit',day:15,place:'Operadora'},
  {id:'condo',icon:'🏢',name:'Condomínio',category:'Moradia',method:'boleto',day:8,place:'Condomínio'},
  {id:'phone',icon:'📱',name:'Celular',category:'Assinaturas',method:'credit',day:20,place:'Operadora'}
];
const normCatName=n=>CAT_ALIASES[n]||n||'';
const CAT_COLORS=['#5af5c8','#f5705a','#a78bfa','#5a9ef5','#f5c85a','#f55a9e','#4ade80','#f5a05a','#5acff5','#c8f55a','#9e8cff','#ff8c6b'];
function hashStr(s){let h=0;for(let i=0;i<(s||'').length;i++)h=((h<<5)-h+s.charCodeAt(i))|0;return Math.abs(h);}
function cleanColor(c){
  if(!c)return'';
  c=String(c).trim().toLowerCase();
  if(/^#[0-9a-f]{3}$/.test(c))c='#'+c[1]+c[1]+c[2]+c[2]+c[3]+c[3];
  return /^#[0-9a-f]{6}$/.test(c)?c:'';
}
function catColor(name,col){
  const c=cleanColor(col);
  if(!c||['#000000','#111111','#222222','#333333','#666666','#777777','#888888','#999999'].includes(c))return CAT_COLORS[hashStr(name)%CAT_COLORS.length];
  return c;
}
function normalizeCat(c){const name=normCatName(c?.name);return {...c,name,col:catColor(name,c?.col||c?.color)};}
const allCats=()=>[...BCATS,...custCats];
const getCat=n=>normalizeCat(allCats().find(c=>c.name===n)||{ico:'\u{1F516}',name:n,col:catColor(n),custom:true});
function getInitials(name){
  return (name||'Eu').trim().split(/\s+/).slice(0,2).map(p=>p[0]||'').join('').toUpperCase()||'EU';
}
function applyAvatar(){
  const img=localStorage.getItem(AVK);
  document.querySelectorAll('#uAvatar,#uAvatarMobile').forEach(av=>{
    av.classList.toggle('has-photo',!!img);
    av.style.backgroundImage=img?`url("${img}")`:'';
    av.textContent=img?'':getInitials(cfg.userName||'Eu');
    if(img)av.setAttribute('aria-label','Foto do perfil');else av.removeAttribute('aria-label');
  });
}
function changeAvatar(inp){
  const file=inp?.files?.[0];if(!file)return;
  if(!file.type.startsWith('image/')){toast('Escolha uma imagem','error');inp.value='';return;}
  if(file.size>2*1024*1024){toast('Use uma imagem até 2 MB','error');inp.value='';return;}
  const reader=new FileReader();
  reader.onload=e=>{
    localStorage.setItem(AVK,e.target.result);
    applyAvatar();
    noteLocalSave('Foto do perfil atualizada');
    if(cfg.mode==='api')saveRemoteState().catch(err=>toast('Erro ao salvar foto: '+err.message,'error'));
    toast('Foto atualizada','success');
    inp.value='';
  };
  reader.onerror=()=>toast('Não foi possível carregar a foto','error');
  reader.readAsDataURL(file);
}
function removeAvatar(){
  localStorage.removeItem(AVK);
  applyAvatar();
  noteLocalSave('Foto do perfil removida');
  if(cfg.mode==='api')saveRemoteState().catch(err=>toast('Erro ao remover foto: '+err.message,'error'));
  toast('Foto removida','info');
}
function applyTheme(t){
  document.documentElement.dataset.theme=t;
  document.getElementById('thmBtn').textContent=t==='dark'?'\u{1F319}':'\u2600\uFE0F';
  const tog=document.getElementById('thmTog');if(tog)tog.checked=t==='dark';
  const m=document.getElementById('themeColorMeta');if(m)m.content=t==='dark'?'#0a0c10':'#f2f4fb';
  localStorage.setItem('fz_t',t);
  if(cfg?.mode==='api')saveRemoteState().catch(()=>{});
}
function toggleTheme(){applyTheme(document.documentElement.dataset.theme==='dark'?'light':'dark');renderDash();}
function thmFromSet(v){applyTheme(v?'dark':'light');renderDash();}
function initTheme(){applyTheme(localStorage.getItem('fz_t')||(window.matchMedia('(prefers-color-scheme:light)').matches?'light':'dark'));}
function applyPrivacy(v){
  privacyMode=!!v;
  document.documentElement.dataset.privacy=privacyMode?'hidden':'visible';
  const icon=privacyMode?'\u{1F648}':'\u{1F441}\uFE0F';
  const btn=document.getElementById('privBtn');
  if(btn){btn.textContent=icon;btn.title=privacyMode?'Mostrar valores':'Ocultar valores';btn.setAttribute('aria-label',btn.title);}
  const tog=document.getElementById('privTog');if(tog)tog.checked=privacyMode;
  localStorage.setItem(PRIVK,privacyMode?'1':'0');
}
function togglePrivacy(){
  applyPrivacy(!privacyMode);
  refreshAll();
  toast(privacyMode?'Valores ocultos':'Valores visíveis','info');
}
function privacyFromSet(v){applyPrivacy(v);refreshAll();}
function initPrivacy(){applyPrivacy(localStorage.getItem(PRIVK)==='1');}
function loadCC(){try{custCats=JSON.parse(localStorage.getItem(CCK)||'[]');}catch{custCats=[];}}
function saveCC(){localStorage.setItem(CCK,JSON.stringify(custCats));noteLocalSave('Categorias salvas localmente');if(cfg.mode==='api')saveRemoteState().catch(()=>{});}
function addCustCat(){
  if(!requireWriteAccess('criar categorias personalizadas'))return;
  const ico=document.getElementById('nCatIco').value.trim()||'\u{1F3F7}\uFE0F';
  const name=normCatName(document.getElementById('nCatNm').value.trim());
  if(!name){toast('Informe o nome','error');return;}
  if(allCats().find(c=>c.name===normCatName(name))){toast('Categoria já existe','error');return;}
  custCats.push({id:uid(),ico,name,col:CAT_COLORS[custCats.length%CAT_COLORS.length],custom:true});
  saveCC();renderCatChips();popCatSels();toast(`"${name}" criada! OK`,'success');
}
function delCustCat(id){if(!requireWriteAccess('remover categorias personalizadas'))return;custCats=custCats.filter(c=>c.id!==id);saveCC();renderCatChips();popCatSels();}
function renderCatChips(){
  const el=document.getElementById('custCatChips');if(!el)return;
  el.innerHTML=custCats.length?custCats.map(c=>`<div class="cat-chip">${c.ico} ${c.name}<span class="chip-x" onclick="delCustCat('${c.id}')">&times;</span></div>`).join(''):'<span style="font-size:11px;color:var(--mt)">Nenhuma ainda.</span>';
}
function popCatSels(){
  const cats=allCats();
  const opts=cats.map(c=>`<option value="${c.name}">${c.ico} ${c.name}</option>`).join('');
  ['txCat','budCat'].forEach(id=>{const e=document.getElementById(id);if(e)e.innerHTML=opts;});
  renderQACats();
}
function renderQACats(){
  const el=document.getElementById('qaCats');if(!el)return;
  el.innerHTML=allCats().filter(c=>!['Salário','Freelance','Investimentos','Outros'].includes(c.name)).slice(0,10).map(c=>`<div class="qa-cat${qaSelCat===c.name?' sel':''}" onclick="qaSelC('${c.name}')">${c.ico} ${c.name}</div>`).join('');
}
function showSetup(){document.getElementById('setup').classList.add('visible');showS('online');}
function hideSetup(){document.getElementById('setup').classList.remove('visible');}
function showS(s){['Main','Online','NewUser','Reset','Local'].forEach(n=>{const e=document.getElementById('s'+n);if(e)e.style.display=n.toLowerCase()===s?'block':'none';});}
async function doSetup(){
  const url=(document.getElementById('sUrl')?.value.trim()||cfg.url||DEFAULT_API_URL).replace(/\/$/,'');
  const username=document.getElementById('sUser').value.trim();
  const password=document.getElementById('sPass').value;
  const otp=document.getElementById('sOtp')?.value.trim()||'';
  const err=document.getElementById('sErr');err.classList.remove('show');
  if(!url||!username||!password){err.textContent='Preencha URL, usuário e senha.';err.classList.add('show');return;}
  const btn=document.getElementById('sBtn'),txt=document.getElementById('sBtnTxt');
  btn.disabled=true;txt.textContent='Conectando...';
  try{
    await fetch(url+'/health');
    const login=await fetch(url+'/api/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username,password,otp})});
    if(!login.ok){const er=await login.json().catch(()=>({}));throw new Error(er.error||'Login inválido');}
    const u=await login.json();
    cfg={url,key:u.api_key,mode:'api',userName:u.name,userId:u.id,loginName:username,role:u.role||'',twoFactorEnabled:!!u.two_factor_enabled};
    adminOverviewState={loading:false,loaded:false,data:null,error:''};
    persistCfg();
    sessionStorage.setItem(PAGE_KEY,'dashboard');
    hideSetup();await initApp();toast(`Bem-vindo, ${u.name}! OK`,'success');
  }catch(e){err.textContent='Falha: '+e.message;err.classList.add('show');btn.disabled=false;txt.textContent='Entrar →';}
}
['sUser','sPass'].forEach(id=>{
  document.getElementById(id)?.addEventListener('keydown',e=>{
    if(e.key!=='Enter')return;
    e.preventDefault();
    doSetup();
  });
});
async function createUser(){
  const url=(document.getElementById('nuUrl')?.value.trim()||cfg.url||DEFAULT_API_URL).replace(/\/$/,'');
  const admin=document.getElementById('nuAdmin').value.trim();
  const name=document.getElementById('nuName').value.trim()||'Usuário';
  const username=document.getElementById('nuUser').value.trim();
  const password=document.getElementById('nuPass').value;
  const err=document.getElementById('nuErr');err.classList.remove('show');
  if(!username||!password){err.textContent='Preencha usuário e senha.';err.classList.add('show');return;}
  try{
    const endpoint=admin?'/api/users':'/api/register';
    const headers={'Content-Type':'application/json'};
    if(admin)headers['x-api-key']=admin;
    const r=await fetch(url+endpoint,{method:'POST',headers,body:JSON.stringify({name,username,password})});
    if(!r.ok){const er=await r.json().catch(()=>({}));throw new Error(er.error||'Erro');}
    await r.json();
    document.getElementById('nuRes').style.display='block';
  }catch(e){err.textContent='Erro: '+e.message;err.classList.add('show');}
}
function useNewUserKey(){
  const sUrl=document.getElementById('sUrl');if(sUrl)sUrl.value=(document.getElementById('nuUrl')?.value.trim()||DEFAULT_API_URL);
  document.getElementById('sUser').value=document.getElementById('nuUser').value.trim();
  document.getElementById('sPass').value=document.getElementById('nuPass').value;
  showS('online');
  doSetup();
}
async function resetPassword(){
  const url=(document.getElementById('rpUrl')?.value.trim()||cfg.url||DEFAULT_API_URL).replace(/\/$/,'');
  const username=document.getElementById('rpUser').value.trim();
  const password=document.getElementById('rpPass').value;
  const admin=document.getElementById('rpAdmin').value.trim();
  const recoveryCode=(document.getElementById('rpRecovery')?.value.trim()||'').toUpperCase();
  const err=document.getElementById('rpErr');err.classList.remove('show');
  if(!username||!password||(!admin&&!recoveryCode)){err.textContent='Preencha usuário, nova senha e chave admin ou código de recuperação.';err.classList.add('show');return;}
  try{
    const endpoint=recoveryCode?'/api/password-reset/recovery':'/api/password-reset';
    const headers={'Content-Type':'application/json'};
    if(!recoveryCode)headers['x-api-key']=admin;
    const body=recoveryCode?{username,password,recovery_code:recoveryCode}:{username,password};
    const r=await fetch(url+endpoint,{method:'POST',headers,body:JSON.stringify(body)});
    if(!r.ok){const e=await r.json().catch(()=>({}));throw new Error(e.error||'Erro');}
    document.getElementById('sUser').value=username;
    document.getElementById('sPass').value=password;
    const otp=document.getElementById('sOtp');if(otp)otp.value='';
    showS('online');
    toast('Senha redefinida. Entrando...','success');
    doSetup();
  }catch(e){err.textContent='Erro: '+e.message;err.classList.add('show');}
}
async function generateRecoveryCode(){
  if(cfg.mode!=='api'){toast('Código de recuperação só faz sentido no modo online','info');return;}
  try{
    const data=await api('POST','/api/me/recovery-code');
    const box=document.getElementById('recoveryCodeBox');
    if(box){box.style.display='block';box.textContent=data.recovery_code;}
    toast('Código de recuperação gerado','success');
  }catch(e){toast('Erro ao gerar código: '+e.message,'error');}
}
async function beginTwoFactorSetup(){
  if(cfg.mode!=='api'){toast('2FA opcional só está disponível no modo online','info');return;}
  try{
    const data=await api('POST','/api/me/2fa/setup');
    pendingTwoFactorSecret=data.secret;
    const panel=document.getElementById('twoFactorSetupPanel');
    const secret=document.getElementById('twoFactorSecret');
    if(secret)secret.textContent=data.secret;
    if(panel)panel.style.display='block';
    toast('Segredo 2FA gerado. Cadastre no autenticador e confirme o código.','success');
  }catch(e){toast('Erro ao iniciar 2FA: '+e.message,'error');}
}
async function confirmTwoFactorSetup(){
  const code=document.getElementById('twoFactorCode')?.value.trim()||'';
  if(!pendingTwoFactorSecret||!code){toast('Informe o código do autenticador para ativar','error');return;}
  try{
    const data=await api('POST','/api/me/2fa/confirm',{code});
    cfg={...cfg,twoFactorEnabled:!!data.user?.two_factor_enabled};
    persistCfg();
    pendingTwoFactorSecret='';
    const panel=document.getElementById('twoFactorSetupPanel');if(panel)panel.style.display='none';
    const input=document.getElementById('twoFactorCode');if(input)input.value='';
    renderSet();
    toast('2FA ativado','success');
  }catch(e){toast('Erro ao confirmar 2FA: '+e.message,'error');}
}
async function disableTwoFactor(){
  const code=prompt('Digite o código atual do autenticador para desativar o 2FA');
  if(!code)return;
  try{
    const data=await api('DELETE','/api/me/2fa',{code});
    cfg={...cfg,twoFactorEnabled:!!data.user?.two_factor_enabled};
    persistCfg();
    pendingTwoFactorSecret='';
    const panel=document.getElementById('twoFactorSetupPanel');if(panel)panel.style.display='none';
    renderSet();
    toast('2FA desativado','success');
  }catch(e){toast('Erro ao desativar 2FA: '+e.message,'error');}
}
function canManageUsers(){return cfg.mode==='api'&&cfg.role==='admin';}
function canSeeAdminPanel(){return cfg.mode==='api'&&cfg.role==='admin';}
function currentUserRole(){
  return String(cfg.role||'').trim().toLowerCase();
}
function canWriteAppData(){
  if(cfg.mode!=='api')return true;
  return ['admin','editor'].includes(currentUserRole());
}
function isReadOnlyApp(){
  return cfg.mode==='api'&&!canWriteAppData();
}
function roleCapabilitySummary(){
  if(cfg.mode!=='api')return 'Modo local com edição liberada neste dispositivo';
  if(canWriteAppData())return 'Pode criar, editar e remover dados financeiros';
  return 'Somente leitura: pode consultar, buscar, exportar e auditar sem editar';
}
function rolePillLabel(){
  if(cfg.mode!=='api')return 'Local';
  return roleLabel(currentUserRole()||'editor');
}
function requireWriteAccess(action='alterar dados'){
  if(canWriteAppData())return true;
  toast(`Conta em modo leitura: você não pode ${action}`,'error');
  return false;
}
function writeActionAttrs(action='alterar dados'){
  return canWriteAppData()?'':`disabled title="Conta em modo leitura: você não pode ${action}"`;
}
function applyWriteAccessUI(){
  const locked=isReadOnlyApp();
  document.body.classList.toggle('read-only-mode',locked);
  document.querySelectorAll('[data-write-only]').forEach(el=>{
    const action=el.dataset.writeOnly||'alterar dados';
    if('disabled' in el)el.disabled=locked;
    if(locked){
      el.setAttribute('aria-disabled','true');
      el.setAttribute('title',`Conta em modo leitura: você não pode ${action}`);
    }else{
      el.removeAttribute('aria-disabled');
      if(el.getAttribute('title')?.startsWith('Conta em modo leitura:'))el.removeAttribute('title');
    }
  });
}
function fmtCompactDateTime(ts){
  if(!ts)return'—';
  return new Date(ts).toLocaleString('pt-BR',{dateStyle:'short',timeStyle:'short'});
}
function fmtFileSize(bytes){
  const value=Number(bytes)||0;
  if(value<=0)return'0 B';
  const units=['B','KB','MB','GB','TB'];
  const idx=Math.min(units.length-1,Math.floor(Math.log(value)/Math.log(1024)));
  const sized=value/1024**idx;
  return `${sized>=10||idx===0?sized.toFixed(0):sized.toFixed(1)} ${units[idx]}`;
}
function adminRoleBreakdownText(byRole={}){
  const labels={admin:'admin',editor:'editor',read:'leitura',guest:'convidado'};
  const parts=Object.entries(labels)
    .map(([role,label])=>({label,total:Number(byRole?.[role])||0}))
    .filter(item=>item.total>0)
    .map(item=>`${item.total} ${item.label}${item.total===1?'':'s'}`);
  return parts.length?parts.join(' • '):'Nenhum papel carregado';
}
function applyAdminPanelVisibility(){
  document.querySelectorAll('[data-admin-only]').forEach(el=>{
    el.style.display=canSeeAdminPanel()?(el.dataset.adminDisplay||''):'none';
  });
}
function updateAdminOverview(){
  const account=document.getElementById('adminAccountName');if(account)account.textContent=cfg.userName||'—';
  const mode=document.getElementById('adminServerMode');if(mode)mode.textContent=cfg.mode==='api'?'Online (API + PostgreSQL)':'Local';
  const server=document.getElementById('adminServerUrl');if(server)server.textContent=cfg.url||'Modo local';
  const users=document.getElementById('adminUsersMeta');
  const roles=document.getElementById('adminRolesMeta');
  const health=document.getElementById('adminHealthMeta');
  const audit=document.getElementById('adminAuditMeta');
  const backup=document.getElementById('adminBackupMeta');
  const transactions=document.getElementById('adminTransactionsMeta');
  if(users){
    if(!canManageUsers())users.textContent='Disponível apenas para administradores online';
    else if(adminOverviewState.loading&&!adminOverviewState.data)users.textContent='Carregando usuários...';
    else if(adminOverviewState.data?.users?.total>=0)users.textContent=`${adminOverviewState.data.users.total} conta${adminOverviewState.data.users.total===1?'':'s'} cadastrada${adminOverviewState.data.users.total===1?'':'s'}`;
    else users.textContent=adminUsers.length?`${adminUsers.length} conta${adminUsers.length===1?'':'s'} carregada${adminUsers.length===1?'':'s'}`:'Carregando usuários...';
  }
  if(roles)roles.textContent=!canManageUsers()?'Disponível apenas para administradores online':(adminOverviewState.data?.users?adminRoleBreakdownText(adminOverviewState.data.users.byRole):adminOverviewState.loading?'Carregando distribuição...':'Sem dados disponíveis');
  if(health)health.textContent=!canManageUsers()?'Disponível apenas para administradores online':(adminOverviewState.data?.server?`${adminOverviewState.data.server.status==='ok'?'Operacional':'Com alerta'} • verificado em ${fmtCompactDateTime(adminOverviewState.data.server.checkedAt)}`:adminOverviewState.loading?'Verificando...':(adminOverviewState.error?`Falha: ${adminOverviewState.error}`:'Sem diagnóstico disponível'));
  if(audit)audit.textContent=!canManageUsers()?'Disponível apenas para administradores online':(adminOverviewState.data?.activity?`${adminOverviewState.data.activity.auditCount} evento${adminOverviewState.data.activity.auditCount===1?'':'s'} • última atividade em ${fmtCompactDateTime(adminOverviewState.data.activity.lastAuditAt)}`:adminOverviewState.loading?'Carregando auditoria...':'Sem dados de auditoria');
  if(backup)backup.textContent=!canManageUsers()?'Disponível apenas para administradores online':(adminOverviewState.data?.backup?.available?`${adminOverviewState.data.backup.last.filename} • ${fmtFileSize(adminOverviewState.data.backup.last.sizeBytes)} • ${fmtCompactDateTime(adminOverviewState.data.backup.last.createdAt)}`:adminOverviewState.loading?'Buscando backups...':'Nenhum backup SQL registrado');
  if(transactions)transactions.textContent=!canManageUsers()?'Disponível apenas para administradores online':(adminOverviewState.data?.transactions?`${adminOverviewState.data.transactions.total} transaç${adminOverviewState.data.transactions.total===1?'ão':'ões'} no ambiente online`:adminOverviewState.loading?'Contando transações...':'Sem contagem disponível');
}
async function loadAdminOverview(force=false){
  if(!canSeeAdminPanel()){
    adminOverviewState={loading:false,loaded:false,data:null,error:''};
    updateAdminOverview();
    return;
  }
  if(adminOverviewState.loading)return;
  if(adminOverviewState.loaded&&!force){
    updateAdminOverview();
    return;
  }
  adminOverviewState={...adminOverviewState,loading:true,error:''};
  updateAdminOverview();
  try{
    const data=await api('GET','/api/admin/overview');
    adminOverviewState={loading:false,loaded:true,data,error:''};
  }catch(err){
    adminOverviewState={loading:false,loaded:false,data:null,error:err.message};
  }
  updateAdminOverview();
}
function roleLabel(role){
  return {admin:'Admin',editor:'Editor',read:'Leitura',guest:'Convidado'}[role]||role||'Editor';
}
async function loadAdminUsers(){
  const box=document.getElementById('adminUsersBox');
  if(!canManageUsers()){if(box)box.style.display='none';updateAdminOverview();return;}
  if(box)box.style.display='block';
  const list=document.getElementById('adminUsersList');
  if(list)list.innerHTML='<div class="sync-empty">Carregando usuários...</div>';
  updateAdminOverview();
  try{
    adminUsers=await api('GET','/api/users');
    renderAdminUsers();
  }catch(err){
    if(list)list.innerHTML=`<div class="sync-empty">Não foi possível carregar usuários: ${esc(err.message)}</div>`;
    updateAdminOverview();
  }
}
function renderAdminUsers(){
  const box=document.getElementById('adminUsersBox');
  if(box)box.style.display=canManageUsers()?'block':'none';
  const list=document.getElementById('adminUsersList');if(!list)return;
  if(!canManageUsers()){list.innerHTML='';updateAdminOverview();return;}
  const roleOptions=['admin','editor','read','guest'];
  list.innerHTML=adminUsers.length?adminUsers.map(u=>`
    <div class="admin-user-row">
      <div class="admin-user-main">
        <div class="sync-title">${esc(u.name||u.username||'Usuário')}</div>
        <div class="sync-meta">${esc(u.username||'sem login')} • ${roleLabel(u.role)}${u.is_admin?' • admin':''}</div>
      </div>
      <div class="admin-user-actions">
        <select class="fi admin-role-select" onchange="updateAdminUserRole('${u.id}',this.value)" ${u.id===cfg.userId?'disabled title="Use outra conta admin para alterar seu próprio papel"':''}>
          ${roleOptions.map(role=>`<option value="${role}" ${role===u.role?'selected':''}>${roleLabel(role)}</option>`).join('')}
        </select>
        <button class="btn btn-d btn-sm" onclick="deleteAdminUser('${u.id}')" ${u.id===cfg.userId?'disabled title="Você não pode remover sua própria conta aqui"':''}>Remover</button>
      </div>
    </div>
  `).join(''):'<div class="sync-empty">Nenhum usuário encontrado.</div>';
  updateAdminOverview();
}
async function createAdminUser(){
  if(!canManageUsers()){toast('Apenas admin online pode criar usuários','error');return;}
  const name=document.getElementById('adminNewName')?.value.trim()||'Usuário';
  const username=document.getElementById('adminNewUser')?.value.trim();
  const password=document.getElementById('adminNewPass')?.value||'';
  const role=document.getElementById('adminNewRole')?.value||'editor';
  if(!username||!password){toast('Preencha usuário e senha','error');return;}
  try{
    await api('POST','/api/users',{name,username,password,role});
    ['adminNewName','adminNewUser','adminNewPass'].forEach(id=>{const el=document.getElementById(id);if(el)el.value='';});
    toast('Usuário criado','success');
    await loadAdminOverview(true);
    await loadAdminUsers();
    await loadPersistentAuditHistory();
    updateTrustPanel();
  }catch(err){toast('Erro: '+err.message,'error');}
}
async function updateAdminUserRole(id,role){
  if(!canManageUsers())return;
  if(id===cfg.userId){toast('Use outra conta admin para alterar seu próprio papel','error');renderAdminUsers();return;}
  const prev=adminUsers.find(u=>u.id===id)?.role;
  try{
    const updated=await api('PATCH',`/api/users/${id}/role`,{role});
    const i=adminUsers.findIndex(u=>u.id===id);
    if(i>=0)adminUsers[i]=updated;
    renderAdminUsers();
    toast('Papel atualizado','success');
    await loadAdminOverview(true);
    await loadPersistentAuditHistory();
    updateTrustPanel();
  }catch(err){
    if(prev){
      const i=adminUsers.findIndex(u=>u.id===id);
      if(i>=0)adminUsers[i]={...adminUsers[i],role:prev};
      renderAdminUsers();
    }
    toast('Erro: '+err.message,'error');
  }
}
async function deleteAdminUser(id){
  if(!canManageUsers())return;
  if(id===cfg.userId){toast('Use outra conta admin para remover esta conta','error');return;}
  if(!confirm('Remover este usuário?'))return;
  try{
    await api('DELETE',`/api/users/${id}`);
    toast('Usuário removido','success');
    await loadAdminOverview(true);
    await loadAdminUsers();
    await loadPersistentAuditHistory();
    updateTrustPanel();
  }catch(err){toast('Erro: '+err.message,'error');}
}
function startLocal(){cfg={url:'',key:'',mode:'local',userName:'Eu',userId:'',role:'',twoFactorEnabled:false};adminOverviewState={loading:false,loaded:false,data:null,error:''};persistCfg();sessionStorage.setItem(PAGE_KEY,'dashboard');hideSetup();initApp();}
function normalizeBackupData(d){
  const data=d?.app==='Finanza'||d?.version?d:{...d};
  if(!Array.isArray(data.transactions))throw new Error('Arquivo inválido: não parece um backup do Finanza');
  data.transactions=(data.transactions||[]).map(t=>nTx({
    ...t,
    description:t.description||t.desc||'Lançamento',
    account_id:t.account_id||t.accountId||null,
    installment_group:t.installment_group||t.installmentGroup||null,
    installment_num:t.installment_num||t.installmentNum||null,
    installment_total:t.installment_total||t.installmentTotal||null,
    recur_group:t.recur_group||t.recurGroup||null
  })).filter(t=>t.amount>0&&t.date);
  data.budgets=(data.budgets||[]).map(nBud).filter(b=>b.category&&b.limit>0);
  data.goals=(data.goals||[]).map(nGoal).filter(g=>g.name&&g.target>0&&g.deadline);
  data.accounts=(data.accounts||[]).map(normalizeAccount);
  data.categories=(data.categories||data.customCategories||[]).map(nCat);
  const shopping=data.shopping||{lists:data.shoppingLists||[],items:data.shoppingItems||[]};
  data.shopping={lists:(shopping.lists||[]).map(nShopList),items:(shopping.items||[]).map(nShopItem)};
  data.settings=data.settings||{};
  data.settings.importCenter=normalizeImportCenterState(data.settings.import_center||data.settings.importCenter||{});
  data.settings.commitments=normalizeCommitmentsState(data.settings.commitments||data.settings.commitmentCenter||{});
  data.car=normalizeCarState(data.car||data.vehicle||data.vehicles||data.settings?.rates?.car||{});
  data.dueItems=(data.dueItems||data.settings?.rates?.dueItems||data.settings?.rates?.due_items||[]).map(nDue).filter(Boolean);
  data.avatarData=(data.settings?.rates?.avatarData??data.settings?.rates?.avatar_data)||'';
  return data;
}
function applyBackupData(data){
  S={transactions:data.transactions,budgets:data.budgets,goals:data.goals,accounts:data.accounts.length?data.accounts:defAccs()};
  custCats=data.categories||[];
  sl=data.shopping?.lists?.length?data.shopping:{lists:[{id:uid(),name:'Mercado',ico:'\u{1F6D2}'}],items:[]};
  carState=normalizeCarState(data.car||{});
  dueItems=data.dueItems||[];
  slActiveList=data.settings?.activeList||data.settings?.active_list||sl.lists[0]?.id||null;
  importCenterState=normalizeImportCenterState(data.settings?.import_center||data.settings?.importCenter||importCenterState);
  commitmentsState=normalizeCommitmentsState(data.settings?.commitments||data.settings?.commitmentCenter||commitmentsState);
  localStorage.setItem(LK,JSON.stringify(S));
  localStorage.setItem(CCK,JSON.stringify(custCats));
  localStorage.setItem(SL_KEY,JSON.stringify(sl));
  localStorage.setItem(CAR_KEY,JSON.stringify(carState));
  localStorage.setItem(DUE_KEY,JSON.stringify(dueItems));
  localStorage.setItem(IMPORT_CENTER_KEY,JSON.stringify(importCenterState));
  localStorage.setItem(COMMITMENTS_KEY,JSON.stringify(commitmentsState));
  if(typeof data.avatarData==='string'){
    if(data.avatarData)localStorage.setItem(AVK,data.avatarData);
    else localStorage.removeItem(AVK);
    applyAvatar();
  }
}
function fmtDateTime(ts){
  if(!ts)return'—';
  return new Date(ts).toLocaleString('pt-BR',{dateStyle:'short',timeStyle:'short'});
}
function loadSaveState(){
  try{return JSON.parse(localStorage.getItem(SAVE_STATE_KEY)||'{}');}
  catch{return{};}
}
function setSaveState(status,message,extra={}){
  const payload={status,message,at:Date.now(),mode:cfg.mode||'local',...extra};
  localStorage.setItem(SAVE_STATE_KEY,JSON.stringify(payload));
  updateTrustPanel();
}
function noteLocalSave(message='Dados salvos neste dispositivo'){
  setSaveState('local',message);
}
function loadSyncHistory(){
  try{syncHistory=JSON.parse(localStorage.getItem(SYNC_HISTORY_KEY)||'[]');}
  catch{syncHistory=[];}
}
function saveSyncHistory(){localStorage.setItem(SYNC_HISTORY_KEY,JSON.stringify(syncHistory.slice(0,20)));}
function loadAuditHistory(){
  try{auditHistory=JSON.parse(localStorage.getItem(AUDIT_HISTORY_KEY)||'[]');}
  catch{auditHistory=[];}
}
function saveAuditHistory(){localStorage.setItem(AUDIT_HISTORY_KEY,JSON.stringify(auditHistory.slice(0,30)));}
function auditLabel(v){
  return String(v||'')
    .replace(/_/g,' ')
    .replace(/\b\w/g,m=>m.toUpperCase());
}
function normalizeAuditEvent(item={}){
  return {
    id:item.id||uid(),
    action:auditLabel(item.action),
    target:auditLabel(item.entity||item.target||'Registro'),
    detail:item.detail||item.entity_id||'',
    source:item.source||'online',
    actor:item.actor_name||item.actor||cfg.userName||cfg.loginName||'online',
    role:item.actor_role||'',
    at:item.created_at?new Date(item.created_at).getTime():(Number(item.at)||Date.now())
  };
}
async function loadPersistentAuditHistory(){
  if(cfg.mode!=='api')return;
  try{
    const events=await api('GET','/api/audit-log?limit=30');
    auditHistory=(events||[]).map(normalizeAuditEvent);
    saveAuditHistory();
  }catch(err){
    logSyncEvent('error','Auditoria online indisponível',err.message);
  }
}
function logAuditEvent(action,target,detail='',source=cfg.mode==='api'?'online':'local'){
  const actor=cfg.userName||cfg.loginName||'local';
  auditHistory.unshift({id:uid(),action,target,detail,source,actor,at:Date.now()});
  auditHistory=auditHistory.slice(0,30);
  saveAuditHistory();
  updateTrustPanel();
}
function logSyncEvent(kind,message,meta=''){
  syncHistory.unshift({id:uid(),kind,message,meta,at:Date.now()});
  syncHistory=syncHistory.slice(0,20);
  saveSyncHistory();
  updateTrustPanel();
}
function updateTrustPanel(){
  const state=loadSaveState();
  const pill=document.getElementById('dataStatePill');
  const txt=document.getElementById('dataStateTxt');
  const meta=document.getElementById('dataStateMeta');
  const queue=document.getElementById('syncQueueTxt');
  const hist=document.getElementById('syncHistoryList');
  const audit=document.getElementById('activityHistoryList');
  const map={
    local:{label:'Local',cls:'local'},
    syncing:{label:'Sincronizando',cls:'syncing'},
    synced:{label:'Sincronizado',cls:'synced'},
    error:{label:'Erro',cls:'error'}
  };
  const cur=map[state.status]||{label:'Sem status',cls:'local'};
  if(pill){pill.textContent=cur.label;pill.className=`state-pill ${cur.cls}`;}
  if(txt)txt.textContent=state.message||'Sem atividade recente';
  if(meta)meta.textContent=`${cfg.mode==='api'?'Modo online':'Modo local'} • ${fmtDateTime(state.at)}`;
  if(queue)queue.textContent=syncQ.length?`${syncQ.length} operação(ões) aguardando envio`:'Sem pendências na fila';
  if(hist)hist.innerHTML=syncHistory.length?syncHistory.slice(0,6).map(item=>`<div class="sync-item ${item.kind}"><div><div class="sync-title">${esc(item.message)}</div><div class="sync-meta">${esc(item.meta||fmtDateTime(item.at))}</div></div><span class="sync-time">${fmtDateTime(item.at)}</span></div>`).join(''):'<div class="sync-empty">Sem eventos recentes de sincronização.</div>';
  if(audit)audit.innerHTML=auditHistory.length?auditHistory.slice(0,8).map(item=>`<div class="sync-item"><div><div class="sync-title">${esc(item.action)} • ${esc(item.target)}</div><div class="sync-meta">${esc(item.actor)}${item.role?` • ${esc(item.role)}`:''} • ${esc(item.source)}${item.detail?` • ${esc(item.detail)}`:''}</div></div><span class="sync-time">${fmtDateTime(item.at)}</span></div>`).join(''):'<div class="sync-empty">Nenhuma ação importante registrada ainda.</div>';
}
function txFingerprint(t={}){
  return [
    String(t.type||''),
    String(t.date||'').substring(0,10),
    roundCarImportNum(t.amount,2),
    normalizeTxText(t.description||t.desc||''),
    normalizeTxText(t.category||''),
    normalizeTxText(t.note||''),
    String(t.accountId||t.account_id||'')
  ].join('|');
}
function goalFingerprint(g={}){
  return [normalizeTxText(g.name||''),String(g.deadline||'').substring(0,10),roundCarImportNum(g.target,2)].join('|');
}
function dedupeBy(items,fingerprint,preferLatest=true){
  const map=new Map();
  items.forEach(item=>{
    const key=fingerprint(item);
    if(!map.has(key)||preferLatest)map.set(key,item);
  });
  return [...map.values()];
}
function buildMergedBackupData(imported){
  const current=buildBackupData();
  const mergedTx=dedupeBy([...current.transactions,...imported.transactions],txFingerprint);
  const mergedBudgets=dedupeBy([...current.budgets,...imported.budgets],b=>normalizeTxText(b.category||'')); 
  const mergedGoals=dedupeBy([...current.goals,...imported.goals],goalFingerprint);
  const mergedAccounts=dedupeBy([...current.accounts,...imported.accounts],a=>String(a.id||''));
  const mergedCategories=dedupeBy([...current.categories,...imported.categories],c=>normalizeTxText(c.name||''));
  const curLists=current.shopping?.lists||[],impLists=imported.shopping?.lists||[];
  const curItems=current.shopping?.items||[],impItems=imported.shopping?.items||[];
  const mergedLists=dedupeBy([...curLists,...impLists],l=>String(l.id||''));
  const mergedItems=dedupeBy([...curItems,...impItems],i=>String(i.id||''));
  const curCar=normalizeCarState(current.car||{}),impCar=normalizeCarState(imported.car||{});
  const mergedVehicles=dedupeBy([...curCar.vehicles,...impCar.vehicles],v=>String(v.id||''));
  const mergedEvents=dedupeBy([...curCar.events,...impCar.events],carEventFingerprint);
  const mergedDue=dedupeBy([...(current.dueItems||[]),...(imported.dueItems||[])],d=>String(d.id||''));
  return {
    ...current,
    version:'4.0-preview',
    transactions:mergedTx,
    budgets:mergedBudgets,
    goals:mergedGoals,
    accounts:mergedAccounts,
    categories:mergedCategories,
    shopping:{lists:mergedLists,items:mergedItems},
    car:{vehicles:mergedVehicles,events:mergedEvents,activeVehicleId:impCar.activeVehicleId||curCar.activeVehicleId||mergedVehicles[0]?.id||''},
    dueItems:mergedDue,
    settings:{...current.settings,rates:{...(current.settings?.rates||{}),dueItems:mergedDue,car:{vehicles:mergedVehicles,events:mergedEvents,activeVehicleId:impCar.activeVehicleId||curCar.activeVehicleId||mergedVehicles[0]?.id||''}}}
  };
}
function summarizeBackupImport(data){
  const current=buildBackupData();
  const curTx=new Set((current.transactions||[]).map(txFingerprint));
  const impTx=(data.transactions||[]).map(txFingerprint);
  const dupTx=impTx.filter(k=>curTx.has(k)).length;
  const curCar=new Set((normalizeCarState(current.car||{}).events||[]).map(carEventFingerprint));
  const impCar=(normalizeCarState(data.car||{}).events||[]).map(carEventFingerprint);
  const dupCar=impCar.filter(k=>curCar.has(k)).length;
  return {
    tx:data.transactions.length,
    txDup:dupTx,
    accounts:data.accounts.length,
    budgets:data.budgets.length,
    goals:data.goals.length,
    categories:data.categories.length,
    carEvents:normalizeCarState(data.car||{}).events.length,
    carDup:dupCar,
    due:(data.dueItems||[]).length
  };
}
function openImportPreview(data,fileName){
  importPreviewState={data,fileName,summary:summarizeBackupImport(data)};
  const s=importPreviewState.summary;
  document.getElementById('importPreviewName').textContent=fileName||'backup.json';
  document.getElementById('importPreviewVersion').textContent=data.version||'sem versão';
  document.getElementById('importPreviewReplace').innerHTML=`<strong>${s.tx}</strong> transações • <strong>${s.accounts}</strong> contas • <strong>${s.carEvents}</strong> registros do carro`;
  document.getElementById('importPreviewMerge').innerHTML=`Ignora aprox. <strong>${s.txDup}</strong> transações e <strong>${s.carDup}</strong> registros do carro já presentes`;
  document.getElementById('importPreviewNotes').innerHTML=`<div class="import-note-row">Orçamentos: <strong>${s.budgets}</strong></div><div class="import-note-row">Metas: <strong>${s.goals}</strong></div><div class="import-note-row">Categorias: <strong>${s.categories}</strong></div><div class="import-note-row">Vencimentos: <strong>${s.due}</strong></div>`;
  document.getElementById('importPreviewModal').classList.add('open');
}
async function confirmImportBackup(mode='replace'){
  if(!importPreviewState)return;
  const payload=mode==='merge'?buildMergedBackupData(importPreviewState.data):importPreviewState.data;
  try{
    if(cfg.mode==='api'){
      const result=await api('PUT','/api/import',payload);
      await loadAll();
      logSyncEvent('success',mode==='merge'?'Backup mesclado no online':'Backup restaurado no online',`${result.imported?.transactions||payload.transactions.length} transações processadas`);
      setSaveState('synced',mode==='merge'?'Backup mesclado e sincronizado':'Backup restaurado e sincronizado');
    }else{
      applyBackupData(payload);
      noteLocalSave(mode==='merge'?'Backup mesclado localmente':'Backup restaurado localmente');
    }
    logAuditEvent(mode==='merge'?'Importacao mesclada':'Importacao substituiu dados','Backup',`${payload.transactions.length} transacoes`,cfg.mode==='api'?'online':'local');
    refreshAll();
    closeM('importPreviewModal');
    importPreviewState=null;
    toast(mode==='merge'?'Backup mesclado sem duplicar':'Backup importado com substituição','success');
  }catch(err){
    setSaveState('error','Falha ao importar backup');
    logSyncEvent('error','Erro ao importar backup',err.message);
    toast('Erro ao importar: '+err.message,'error');
  }
}
function queueUndo(label,restore){
  if(undoState?.timer)clearTimeout(undoState.timer);
  undoState={label,restore,timer:setTimeout(()=>dismissUndo(),9000)};
  const bar=document.getElementById('undoBar');
  const lbl=document.getElementById('undoLabel');
  if(lbl)lbl.textContent=label;
  if(bar)bar.classList.add('show');
}
function dismissUndo(){
  if(undoState?.timer)clearTimeout(undoState.timer);
  undoState=null;
  document.getElementById('undoBar')?.classList.remove('show');
}
async function undoLastAction(){
  if(!undoState)return;
  const restore=undoState.restore;
  dismissUndo();
  try{
    await restore();
    refreshAll();
    toast('Ação desfeita','success');
  }catch(err){toast('Não foi possível desfazer: '+err.message,'error');}
}
function importLocal(inp){
  const f=inp.files[0];if(!f)return;
  const r=new FileReader();
  r.onload=e=>{
    try{
      const data=normalizeBackupData(JSON.parse(e.target.result));
      applyBackupData(data);
      cfg={url:'',key:'',mode:'local',userName:data.user||'Eu',userId:'',role:'',twoFactorEnabled:false};
      persistCfg();
      hideSetup();
      initApp();
      toast('Importado: '+data.transactions.length+' transações OK','success');
    }catch(err){
      alert('Erro ao importar: '+err.message+'\n\nVerifique se o arquivo é um backup válido do Finanza.');
    }
  };
  r.readAsText(f,'UTF-8');
}
function importBackupFile(inp){
  const f=inp.files[0];if(!f)return;
  if(!requireWriteAccess('importar backups')){inp.value='';return;}
  const r=new FileReader();
  r.onload=async e=>{
    try{
      const data=normalizeBackupData(JSON.parse(e.target.result));
      openImportPreview(data,f.name);
    }catch(err){toast('Erro ao importar: '+err.message,'error');}
    finally{inp.value='';}
  };
  r.readAsText(f,'UTF-8');
}
function openImportCenter(){
  if(!importDraft.batchId)clearImportDraft();
  renderImportCenter();
  document.getElementById('importCenterModal')?.classList.add('open');
}
function importSourceLabel(src){return({csv:'CSV',ofx:'OFX',text:'Texto',pdf:'PDF/Extrato',ocr:'OCR/Recibo',pix:'Comprovante Pix',qr:'QR/NFC-e',folder:'Pasta'})[src]||src;}
function isTextImportSource(src=''){
  return ['text','pdf','ocr','pix','qr'].includes(src);
}
function importSourcePlaceholder(src='text'){
  return ({
    text:'Cole aqui texto de extrato, linhas copiadas do banco ou anotações para importar.',
    pdf:'Cole aqui o texto copiado de um PDF simples de fatura ou extrato. Se o PDF estiver em imagem, use OCR antes.',
    ocr:'Cole aqui o texto extraído por OCR de recibo, comprovante ou nota.',
    pix:'Cole aqui o texto do comprovante Pix copiado do banco ou OCR do comprovante.',
    qr:'Cole aqui o texto lido de QR code, NFC-e ou cupom fiscal convertido para texto.'
  })[src]||'Cole aqui o texto para importar.';
}
function importSourceHint(src='text'){
  return ({
    text:'Aceita várias linhas. Cada linha pode virar um lançamento.',
    pdf:'Bom para PDF simples com texto selecionável ou OCR já extraído.',
    ocr:'Use quando você já tiver o texto extraído de imagem ou recibo.',
    pix:'O parser tenta reconhecer valor, data, descrição e sentido de entrada/saída.',
    qr:'Use para textos copiados de QR/NFC-e ou dados fiscais simplificados.'
  })[src]||'Aceita várias linhas. Cada linha pode virar um lançamento.';
}
function renderImportCenter(){
  renderImportToolbar();
  renderImportSourceTabs();
  renderImportSourceBody();
  renderImportSnapshots();
  renderImportMapping();
  renderImportReview();
}
function renderImportToolbar(){
  const rows=importDraft.rows||[];
  const pendingInbox=S.transactions.filter(t=>t.category==='A classificar'&&(getTxImportMeta(t.id).imported||t.pending)).length;
  const duplicates=rows.filter(r=>r.status==='duplicate').length;
  const recon=rows.filter(r=>r.status==='match').length;
  const el=document.getElementById('importCenterToolbar');if(!el)return;
  el.innerHTML=`<div class="import-status-strip">
    <div><span>Fonte</span><strong>${importSourceLabel(importDraft.source)}</strong></div>
    <div><span>Preparadas</span><strong>${rows.length}</strong></div>
    <div><span>Duplicadas</span><strong style="color:var(--warn)">${duplicates}</strong></div>
    <div><span>Conciliar</span><strong style="color:var(--ac2)">${recon}</strong></div>
    <div><span>Entrada</span><strong style="color:var(--dan)">${pendingInbox}</strong></div>
  </div>`;
}
function renderImportSourceTabs(){
  const el=document.getElementById('importSourceTabs');if(!el)return;
  const tabs=[['csv','▦ CSV'],['ofx','◇ OFX'],['pdf','▤ PDF'],['ocr','⌕ OCR'],['pix','Pix'],['qr','QR/NFC-e'],['text','Texto'],['folder','Pasta']];
  el.innerHTML=tabs.map(([id,label])=>`<button class="cat-filter-chip ${importDraft.source===id?'active':''}" onclick="setImportSource('${id}')">${label}</button>`).join('');
}
function setImportSource(src){
  importDraft.source=src;
  importDraft.files=[];
  importDraft.headers=[];
  importDraft.rows=[];
  importDraft.text='';
  importDraft.balanceDivergence=null;
  renderImportCenter();
}
function renderImportSourceBody(){
  const el=document.getElementById('importSourceBody');if(!el)return;
  const profileOptions=['<option value="">Perfil salvo</option>',...Object.keys(importCenterState.profiles.csv).sort().map(name=>`<option value="${esc(name)}" ${importDraft.profileName===name?'selected':''}>${esc(name)}</option>`)].join('');
  const ruleSummary=`${importCenterState.rules.categories.length} regra(s) de categoria • ${importCenterState.rules.subscriptions.length} regra(s) de assinatura`;
  const textBody=src=>`<div class="import-source-panel"><label class="fl">${importSourceLabel(src)}</label><textarea class="ta import-textarea" id="importTextArea" placeholder="${esc(importSourcePlaceholder(src))}" oninput="importDraft.text=this.value">${esc(importDraft.text||'')}</textarea><div class="import-helper">${importSourceHint(src)}</div></div>`;
  const sourceBody={
    csv:`<div class="import-source-panel"><label class="import-file-drop"><span>Selecionar CSV/XLSX</span><small>Escolha .csv, .txt, .xlsx ou .xls exportado do banco/cartão.</small><input type="file" accept=".csv,text/csv,.txt,.xlsx,.xls,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel" onchange="handleImportFiles(this.files)"></label><div class="import-inline-grid"><label><span>Perfil</span><select class="fi sel" onchange="applyImportProfile(this.value)">${profileOptions}</select></label><label><span>Deduplicação</span><select class="fi sel" id="importDedupe" onchange="importDraft.dedupe=this.value"><option value="exact" ${importDraft.dedupe==='exact'?'selected':''}>Exata</option><option value="soft" ${importDraft.dedupe==='soft'?'selected':''}>Descrição + valor</option><option value="off" ${importDraft.dedupe==='off'?'selected':''}>Não ignorar</option></select></label></div><div class="import-helper">${ruleSummary}</div></div>`,
    ofx:`<div class="import-source-panel"><label class="import-file-drop"><span>Selecionar OFX</span><small>Use .ofx ou .qfx exportado pelo banco/cartão.</small><input type="file" accept=".ofx,.qfx,.txt" onchange="handleImportFiles(this.files)"></label><div class="import-helper">${ruleSummary}</div></div>`,
    pdf:`<div class="import-source-panel"><label class="import-file-drop"><span>Selecionar PDF</span><small>Use PDF com texto selecionável; PDF escaneado precisa de OCR.</small><input type="file" accept=".pdf,application/pdf" onchange="handleImportFiles(this.files)"></label></div>${textBody('pdf')}`,
    ocr:textBody('ocr'),
    pix:textBody('pix'),
    qr:textBody('qr'),
    text:textBody('text'),
    folder:`<div class="import-source-panel"><label class="import-file-drop"><span>Selecionar pasta</span><small>Importa lotes de CSV, XLSX, PDF, OFX e textos extraídos de uma pasta local.</small><input type="file" multiple webkitdirectory directory accept=".csv,.txt,.xlsx,.xls,.pdf,.ofx,.qfx" onchange="handleImportFiles(this.files)"></label></div>`
  };
  el.innerHTML=sourceBody[importDraft.source]||'';
}
async function handleImportFiles(fileList){
  importDraft.files=asArr([...fileList]);
  if(!importDraft.files.length)return;
  importDraft.rows=[];
  importDraft.balanceDivergence=null;
  const first=importDraft.files[0];
  if(importDraft.source==='csv'){
    importDraft.headers=[];
    importDraft.mapping={};
    const parsed=await parseTabularImportFile(first);
    importDraft.headers=parsed.headers;
    importDraft.mapping=detectImportMapping(parsed.headers);
  }else if(importDraft.source==='pdf'){
    importDraft.text='';
    try{
      importDraft.text=await extractPdfText(first);
      toast('Texto do PDF extraído. Agora prepare a revisão.','success');
    }catch(err){toast(err.message,'error');}
  }
  renderImportCenter();
}
function isXlsxImportFile(file){
  return /\.(xlsx|xls)$/i.test(file?.name||'');
}
function isPdfImportFile(file){
  return /\.pdf$/i.test(file?.name||'')||file?.type==='application/pdf';
}
const vendorScriptPromises={};
function loadVendorScript(src,globalName){
  if(globalName&&window[globalName])return Promise.resolve(window[globalName]);
  if(vendorScriptPromises[src])return vendorScriptPromises[src];
  vendorScriptPromises[src]=new Promise((resolve,reject)=>{
    const s=document.createElement('script');
    s.src=src;
    s.async=true;
    s.onload=()=>resolve(globalName?window[globalName]:true);
    s.onerror=()=>reject(new Error(`Não consegui carregar ${src}`));
    document.head.appendChild(s);
  });
  return vendorScriptPromises[src];
}
function parseCsvMatrix(text=''){
  const lines=String(text).replace(/\r/g,'').split('\n').filter(Boolean);
  if(!lines.length)return{headers:[],rows:[]};
  const delimiter=(lines[0].match(/;/g)||[]).length>=(lines[0].match(/,/g)||[]).length?';':',';
  const matrix=lines.map(line=>{
    const out=[];let cur='';let quoted=false;
    for(let i=0;i<line.length;i++){
      const ch=line[i];
      if(ch==='"'){quoted=!quoted;continue;}
      if(ch===delimiter&&!quoted){out.push(cur.trim());cur='';continue;}
      cur+=ch;
    }
    out.push(cur.trim());
    return out;
  });
  const [headers,...rawRows]=matrix;
  const rows=rawRows.map(row=>{
    if(headers.length&&row.length>headers.length){
      return [...row.slice(0,headers.length-1),row.slice(headers.length-1).join(delimiter)];
    }
    return row;
  });
  return {headers,rows};
}
async function parseXlsxMatrix(file){
  if(!window.XLSX)await loadVendorScript('vendor/xlsx.full.min.js?v=4.3.2','XLSX');
  if(!window.XLSX)throw new Error('Leitor XLSX indisponível. Verifique a conexão e tente novamente.');
  const workbook=window.XLSX.read(await file.arrayBuffer(),{type:'array',cellDates:false});
  const sheet=workbook.Sheets[workbook.SheetNames[0]];
  const matrix=window.XLSX.utils.sheet_to_json(sheet,{header:1,raw:false,defval:''})
    .map(row=>row.map(cell=>String(cell||'').trim()))
    .filter(row=>row.some(Boolean));
  if(!matrix.length)return{headers:[],rows:[]};
  const [headers,...rows]=matrix;
  return{headers,rows};
}
async function parseTabularImportFile(file){
  return isXlsxImportFile(file)?parseXlsxMatrix(file):parseCsvMatrix(await file.text());
}
async function extractPdfText(file){
  if(!isPdfImportFile(file))throw new Error('Escolha um arquivo PDF.');
  if(!window.pdfjsLib)await loadVendorScript('vendor/pdf.min.js?v=4.3.2','pdfjsLib');
  if(!window.pdfjsLib)throw new Error('Leitor PDF indisponível. Selecione e copie o texto do PDF no campo abaixo.');
  const pdfjs=window.pdfjsLib;
  if(pdfjs.GlobalWorkerOptions&&!pdfjs.GlobalWorkerOptions.workerSrc){
    pdfjs.GlobalWorkerOptions.workerSrc='vendor/pdf.worker.min.js';
  }
  const pdf=await pdfjs.getDocument({data:new Uint8Array(await file.arrayBuffer())}).promise;
  const pages=[];
  for(let i=1;i<=pdf.numPages;i++){
    const page=await pdf.getPage(i);
    const content=await page.getTextContent();
    pages.push(content.items.map(item=>item.str).join(' '));
  }
  const text=pages.join('\n').trim();
  if(!text)throw new Error('Não consegui extrair texto desse PDF. Se for imagem/escaneado, use OCR e cole o texto.');
  return text;
}
function importFieldOptions(){
  return [
    ['ignore','Ignorar'],['date','Data'],['description','Descrição'],['amount','Valor'],['type','Tipo'],['category','Categoria'],['account','Conta'],['note','Observação'],['balance','Saldo']
  ];
}
function keyImportHeader(v=''){return normalizeTxText(String(v).replace(/[^\p{L}\p{N}]+/gu,' '));}
function detectImportMapping(headers=[]){
  const aliases={
    date:['data','date','posted','lancamento','lançamento'],
    description:['descricao','descrição','historico','histórico','memo','description','titulo','title','compra','compras','estabelecimento','merchant','local'],
    amount:['valor','amount','valor rs','amount rs','total','saida','saída','entrada','preco','preço'],
    type:['tipo','type','natureza','dc','debito credito','d/c'],
    category:['categoria','category'],
    account:['conta','account','cartao','cartão','bank'],
    note:['obs','observacao','observação','note','notes'],
    balance:['saldo','balance']
  };
  const used={};
  const mapping={};
  headers.forEach((head,idx)=>{
    const key=keyImportHeader(head);
    let found='ignore';
    Object.entries(aliases).some(([field,list])=>{
      if(list.some(alias=>key.includes(keyImportHeader(alias)))&&!used[field]){
        found=field;used[field]=true;return true;
      }
      return false;
    });
    mapping[idx]=found;
  });
  return mapping;
}
function renderImportMapping(){
  const el=document.getElementById('importMapping');if(!el)return;
  if(importDraft.source!=='csv'||!importDraft.headers?.length){el.innerHTML='';return;}
  el.innerHTML=`<div class="ss-l" style="margin-bottom:8px">Mapeamento assistido</div><div class="import-inline-grid">${importDraft.headers.map((head,idx)=>`<label><span>${esc(head||`Coluna ${idx+1}`)}</span><select class="fi sel" onchange="setImportMapping(${idx},this.value)">${importFieldOptions().map(([value,label])=>`<option value="${value}" ${importDraft.mapping[idx]===value?'selected':''}>${label}</option>`).join('')}</select></label>`).join('')}</div>`;
}
function setImportMapping(idx,value){
  importDraft.mapping[idx]=value;
}
function parseImportDate(raw=''){
  const v=String(raw||'').trim();
  if(!v)return today();
  if(/^\d{4}-\d{2}-\d{2}$/.test(v))return v;
  const m=v.match(/^(\d{1,2})[\/.-](\d{1,2})[\/.-](\d{2,4})$/);
  if(m){
    const year=String(m[3]).length===2?`20${m[3]}`:m[3];
    return `${year}-${String(m[2]).padStart(2,'0')}-${String(m[1]).padStart(2,'0')}`;
  }
  return today();
}
function parseImportAmount(raw=''){
  const clean=String(raw||'').replace(/[^\d,.-]/g,'').trim();
  if(!clean)return 0;
  const pt=clean.includes(',')?clean.replace(/\./g,'').replace(',','.'):clean;
  return Math.abs(Number(pt)||0);
}
function inferImportType(typeRaw, amountRaw, description=''){
  const t=normalizeTxText(typeRaw||'');
  const desc=normalizeTxText(description);
  if(/credit|credito|crédito|income|entrada|receb/i.test(t))return'income';
  if(/debit|debito|débito|expense|saida|saída/i.test(t))return'expense';
  if(/^[-]/.test(String(amountRaw||'')))return'expense';
  if(desc.includes('pix recebido')||desc.includes('salario')||desc.includes('salário')||desc.includes('receb'))return'income';
  return'expense';
}
function applyImportRules(row){
  const lower=normalizeTxText(`${row.description} ${row.note}`);
  const catRule=importCenterState.rules.categories.find(rule=>lower.includes(normalizeTxText(rule.match)));
  if(catRule)row.category=catRule.category;
  const day=parseInt(String(row.date).slice(8,10),10);
  const subRule=importCenterState.rules.subscriptions.find(rule=>rule.day===day&&Math.abs(rule.amount-row.amount)<0.01);
  if(subRule){
    row.category=subRule.category;
    if(!row.description)row.description=subRule.description||row.category;
    row.subscriptionHint=true;
  }
  return row;
}
function buildImportRowsFromMatrix(parsed,fileName=''){
  if(!importDraft.headers.length){importDraft.headers=parsed.headers;importDraft.mapping=detectImportMapping(parsed.headers);}
  return parsed.rows.map((cols,idx)=>{
    const mapped={source:fileName,rowNumber:idx+2};
    cols.forEach((val,colIdx)=>mapped[importDraft.mapping[colIdx]||'ignore']=val);
    const description=(mapped.description||mapped.note||'Importado').trim();
    const type=inferImportType(mapped.type,mapped.amount,description);
    const amount=parseImportAmount(mapped.amount);
    const category=mapped.category?normCatName(mapped.category):inferTxCategory(description,type);
    return applyImportRules({
      id:uid(),
      date:parseImportDate(mapped.date),
      description,
      amount,
      type,
      category:category||'A classificar',
      accountId:findImportAccount(mapped.account,description),
      note:String(mapped.note||'').trim(),
      balance:mapped.balance?parseImportAmount(mapped.balance):0,
      raw:mapped
    });
  }).filter(row=>row.amount>0&&row.description);
}
async function buildImportRowsFromTabularFile(file){
  return buildImportRowsFromMatrix(await parseTabularImportFile(file),file.name);
}
function buildImportRowsFromCsv(text,fileName=''){
  return buildImportRowsFromMatrix(parseCsvMatrix(text),fileName);
}
function findImportAccount(raw='',description=''){
  const source=normalizeTxText(`${raw} ${description}`);
  return S.accounts.find(acc=>source.includes(normalizeTxText(acc.name)))?.id||S.accounts[0]?.id||null;
}
function parseOfxRows(text,fileName=''){
  return String(text).split(/<STMTTRN>/i).slice(1).map(block=>{
    const take=tag=>block.match(new RegExp(`<${tag}>([^<\\r\\n]+)`,'i'))?.[1]?.trim()||'';
    const amountRaw=take('TRNAMT');
    const amount=Math.abs(Number(amountRaw.replace(',','.'))||0);
    const description=take('MEMO')||take('NAME')||'OFX';
    const type=(amountRaw||'').trim().startsWith('-')?'expense':'income';
    return applyImportRules({
      id:uid(),
      source:fileName,
      date:parseImportDate(take('DTPOSTED').slice(0,8).replace(/^(\d{4})(\d{2})(\d{2}).*$/,'$1-$2-$3')),
      description,
      amount,
      type,
      category:inferTxCategory(description,type)||'A classificar',
      accountId:findImportAccount('',description),
      note:take('FITID'),
      balance:0,
      raw:{}
    });
  }).filter(row=>row.amount>0&&row.description);
}
function parseTextImportRows(text=''){
  return String(text).split(/\n+/).map(line=>line.trim()).filter(Boolean).map(line=>{
    const parsed=parseTxText(line);
    if(parsed){
      return applyImportRules({
        id:uid(),
        date:parsed.date,
        description:parsed.desc,
        amount:parsed.amount,
        type:parsed.type,
        category:parsed.category||'A classificar',
        accountId:parsed.accountId||S.accounts[0]?.id||null,
        note:'Importado de texto',
        balance:0,
        raw:{text:line}
      });
    }
    const amountMatch=line.match(/(?:r\$)?\s*(-?\d[\d.,]*)/i);
    const amount=parseImportAmount(amountMatch?.[1]||'');
    if(!amount)return null;
    const type=inferImportType('',amountMatch?.[1]||'',line);
    return applyImportRules({
      id:uid(),
      date:parseImportDate(line.match(/\d{1,2}[\/.-]\d{1,2}[\/.-]\d{2,4}/)?.[0]||today()),
      description:line.replace(amountMatch?.[0]||'','').trim()||'Importado de texto',
      amount,
      type,
      category:inferTxCategory(line,type)||'A classificar',
      accountId:S.accounts[0]?.id||null,
      note:'Importado de texto',
      balance:0,
      raw:{text:line}
    });
  }).filter(Boolean);
}
function findExactDuplicate(row){
  if(importDraft.dedupe==='off')return null;
  const fp=txFingerprint({type:row.type,date:row.date,amount:row.amount,description:row.description,category:row.category,note:row.note,accountId:row.accountId});
  return S.transactions.find(tx=>txFingerprint(tx)===fp)||null;
}
function findSoftMatch(row){
  return S.transactions.find(tx=>
    Math.abs(Number(tx.amount)-Number(row.amount))<0.01 &&
    normalizeTxText(tx.desc)===normalizeTxText(row.description) &&
    Math.abs(new Date(tx.date+'T12:00:00')-new Date(row.date+'T12:00:00'))<=86400000*3
  )||null;
}
function prepareImportRows(rows){
  const batchId=`${IMPORT_BATCH_PREFIX}${Date.now()}`;
  importDraft.batchId=batchId;
  importDraft.rows=rows.map(row=>{
    const duplicate=findExactDuplicate(row);
    const matched=!duplicate&&importDraft.dedupe!=='exact'?findSoftMatch(row):null;
    const status=duplicate?'duplicate':matched?'match':'new';
    return {
      ...row,
      duplicateId:duplicate?.id||'',
      matchId:(duplicate||matched)?.id||'',
      status,
      decision:status==='duplicate'?'skip':status==='match'?'reconcile':'create'
    };
  });
  const balanceRows=importDraft.rows.filter(r=>r.balance>0);
  if(balanceRows.length){
    const last=balanceRows[balanceRows.length-1];
    const current=last.accountId?getAccBal(last.accountId):0;
    importDraft.balanceDivergence=Math.round((last.balance-current)*100)/100;
  }else importDraft.balanceDivergence=null;
}
async function parseImportDraft(){
  try{
    let rows=[];
    if(importDraft.source==='csv'){
      if(!importDraft.files.length)throw new Error('Escolha um CSV ou XLSX primeiro');
      for(const file of importDraft.files){
        const tabularRows=await buildImportRowsFromTabularFile(file);
        rows.push(...tabularRows);
      }
    }else if(importDraft.source==='ofx'){
      if(!importDraft.files.length)throw new Error('Escolha um OFX primeiro');
      for(const file of importDraft.files)rows.push(...parseOfxRows(await file.text(),file.name));
    }else if(isTextImportSource(importDraft.source)){
      if(!String(importDraft.text||'').trim())throw new Error('Cole um texto para importar');
      rows=parseTextImportRows(importDraft.text);
    }else if(importDraft.source==='folder'){
      if(!importDraft.files.length)throw new Error('Escolha uma pasta ou arquivos');
      for(const file of importDraft.files){
        if(/\.(ofx|qfx)$/i.test(file.name))rows.push(...parseOfxRows(await file.text(),file.name));
        else if(isPdfImportFile(file))rows.push(...parseTextImportRows(await extractPdfText(file)));
        else{
          const tabularRows=await buildImportRowsFromTabularFile(file);
          rows.push(...tabularRows);
        }
      }
    }
    if(!rows.length)throw new Error('Nada reconhecido para importar');
    prepareImportRows(rows);
    renderImportCenter();
    toast(`${rows.length} linha(s) preparadas para revisão`,'success');
  }catch(err){toast(err.message,'error');}
}
function renderImportReview(){
  const head=document.getElementById('importReviewHead');
  const list=document.getElementById('importReviewList');
  if(!head||!list)return;
  const rows=importDraft.rows||[];
  if(!rows.length){
    head.innerHTML='<div class="ss-l">Nenhuma revisão preparada</div><div class="ss-s">Escolha uma fonte à esquerda e toque em Preparar revisão.</div>';
    list.innerHTML='<div class="import-empty"><span>📥</span><strong>Pronto para receber dados</strong><p>Depois da preparação, cada lançamento aparece aqui com decisão, categoria e conta antes de entrar no histórico.</p></div>';
    return;
  }
  const counts={
    create:rows.filter(r=>r.decision==='create').length,
    reconcile:rows.filter(r=>r.decision==='reconcile').length,
    replace:rows.filter(r=>r.decision==='replace').length,
    merge:rows.filter(r=>r.decision==='merge').length,
    skip:rows.filter(r=>r.decision==='skip').length
  };
  head.innerHTML=`<div class="ss-l">Revisão em lote</div><div class="ss-s">${rows.length} itens • criar ${counts.create} • reconciliar ${counts.reconcile} • substituir ${counts.replace} • mesclar ${counts.merge} • ignorar ${counts.skip}${importDraft.balanceDivergence!==null?` • divergência ${fmt(importDraft.balanceDivergence)}`:''}</div>`;
  list.innerHTML=rows.map((row,idx)=>{
    const match=row.matchId?S.transactions.find(t=>t.id===row.matchId):null;
    return `<div class="import-review-row ${row.status}">
      <div class="import-review-main">
        <div><strong>${esc(row.description)}</strong><small>${fmtD(row.date)} • ${row.type==='income'?'Receita':'Despesa'} • ${fmt(row.amount)} • ${esc(row.category)}</small></div>
        <span class="state-pill ${row.status==='duplicate'?'error':row.status==='match'?'syncing':'synced'}">${row.status==='duplicate'?'Duplicada':row.status==='match'?'Conciliar':'Nova'}</span>
      </div>
      ${match?`<div class="import-review-match">Atual: ${esc(match.desc)} • ${fmt(match.amount)} • ${fmtD(match.date)}</div>`:''}
      ${match?`<div class="import-conflict-grid">
        <div class="import-conflict-card">
          <div class="import-conflict-kicker">Atual</div>
          <strong>${esc(match.desc)}</strong>
          <small>${fmt(match.amount)} • ${fmtD(match.date)} • ${esc(match.category||'Sem categoria')}</small>
          <small>${esc(match.note||'Sem observação')}</small>
        </div>
        <div class="import-conflict-card imported">
          <div class="import-conflict-kicker">Importado</div>
          <strong>${esc(row.description)}</strong>
          <small>${fmt(row.amount)} • ${fmtD(row.date)} • ${esc(row.category||'Sem categoria')}</small>
          <small>${esc(row.note||'Sem observação')}</small>
        </div>
      </div>
      <div class="import-conflict-actions">
        <button class="btn btn-g btn-sm" onclick="setImportRowField(${idx},'decision','reconcile')">Manter atual</button>
        <button class="btn btn-g btn-sm" onclick="setImportRowField(${idx},'decision','replace')">Usar importado</button>
        <button class="btn btn-g btn-sm" onclick="setImportRowField(${idx},'decision','merge')">Mesclar manualmente</button>
      </div>`:''}
      <div class="import-inline-grid">
        <label><span>Decisão</span><select class="fi sel" onchange="setImportRowField(${idx},'decision',this.value)"><option value="create" ${row.decision==='create'?'selected':''}>Criar novo</option><option value="reconcile" ${row.decision==='reconcile'?'selected':''}>Conciliar</option><option value="replace" ${row.decision==='replace'?'selected':''}>Usar importado</option><option value="merge" ${row.decision==='merge'?'selected':''}>Mesclar manual</option><option value="skip" ${row.decision==='skip'?'selected':''}>Ignorar</option></select></label>
        <label><span>Categoria</span><select class="fi sel" onchange="setImportRowField(${idx},'category',this.value)">${allCats().map(cat=>`<option value="${esc(cat.name)}" ${cat.name===row.category?'selected':''}>${cat.ico} ${esc(cat.name)}</option>`).join('')}</select></label>
        <label><span>Conta</span><select class="fi sel" onchange="setImportRowField(${idx},'accountId',this.value)">${S.accounts.map(acc=>`<option value="${esc(acc.id)}" ${acc.id===row.accountId?'selected':''}>${acc.icon} ${esc(acc.name)}</option>`).join('')}</select></label>
      </div>
      <div class="import-inline-actions"><button class="btn btn-g btn-sm" onclick="saveImportCategoryRule(${idx})">Salvar regra</button><button class="btn btn-g btn-sm" onclick="saveImportSubscriptionRule(${idx})">Salvar assinatura</button></div>
    </div>`;
  }).join('');
}
function setImportRowField(idx,key,value){
  if(!importDraft.rows[idx])return;
  importDraft.rows[idx][key]=value;
  renderImportReview();
}
function saveImportCategoryRule(idx){
  const row=importDraft.rows[idx];if(!row)return;
  importCenterState.rules.categories.unshift({id:uid(),match:row.description,category:row.category});
  importCenterState.rules.categories=dedupeBy(importCenterState.rules.categories,r=>`${normalizeTxText(r.match)}|${r.category}`);
  saveImportCenterState(false);
  renderImportCenter();
  toast('Regra de categoria salva','success');
}
function saveImportSubscriptionRule(idx){
  const row=importDraft.rows[idx];if(!row)return;
  importCenterState.rules.subscriptions.unshift({id:uid(),day:Number(String(row.date).slice(8,10)),amount:row.amount,description:row.description,category:row.category});
  importCenterState.rules.subscriptions=dedupeBy(importCenterState.rules.subscriptions,r=>`${r.day}|${roundCarImportNum(r.amount,2)}|${normalizeTxText(r.description)}`);
  saveImportCenterState(false);
  renderImportCenter();
  toast('Regra de assinatura salva','success');
}
function applyImportProfile(name){
  importDraft.profileName=name;
  if(!name)return;
  const profile=asObj(importCenterState.profiles.csv[name]);
  importDraft.mapping=asObj(profile.mapping);
  importDraft.dedupe=profile.dedupe||'exact';
  renderImportCenter();
}
function saveCurrentImportProfile(){
  if(importDraft.source!=='csv'||!importDraft.headers.length){toast('Abra um CSV primeiro para salvar perfil','info');return;}
  const name=prompt('Nome do perfil de importação:',importDraft.profileName||'Banco principal');
  if(!name)return;
  importCenterState.profiles.csv[name]={mapping:importDraft.mapping,dedupe:importDraft.dedupe};
  importDraft.profileName=name;
  saveImportCenterState(false);
  renderImportCenter();
  toast('Perfil salvo','success');
}
function buildCompactImportSnapshot(){
  const backup=buildBackupData();
  return {
    id:uid(),
    at:Date.now(),
    label:`${backup.stats.transactions} tx • ${fmtDateTime(Date.now())}`,
    data:{transactions:backup.transactions,budgets:backup.budgets,goals:backup.goals,accounts:backup.accounts,categories:backup.categories,shopping:backup.shopping,car:backup.car,dueItems:backup.dueItems,settings:{...backup.settings,importCenter:importCenterState},avatarData:localStorage.getItem(AVK)||''}
  };
}
function createImportSnapshot(){
  importCenterState.snapshots.unshift(buildCompactImportSnapshot());
  importCenterState.snapshots=importCenterState.snapshots.slice(0,5);
  saveImportCenterState(false);
}
function renderImportSnapshots(){
  const el=document.getElementById('importSnapshots');if(!el)return;
  const items=importCenterState.snapshots||[];
  el.innerHTML=`<div class="ss-l" style="margin:12px 0 8px">Snapshots rápidos</div>${items.length?items.map(item=>`<div class="sync-item"><div><div class="sync-title">${esc(item.label)}</div><div class="sync-meta">${fmtDateTime(item.at)}</div></div><button class="btn btn-g btn-sm" onclick="restoreImportSnapshot('${item.id}')">Restaurar</button></div>`).join(''):'<div class="sync-empty">O próximo import salva um snapshot de restauração rápida.</div>'}`;
}
function restoreImportSnapshot(id){
  const item=(importCenterState.snapshots||[]).find(s=>s.id===id);
  if(!item)return;
  applyBackupData(normalizeBackupData({app:'Finanza',version:APP_VERSION,...item.data}));
  refreshAll();
  toast('Snapshot restaurado','success');
}
async function confirmImportTransactions(){
  const rows=importDraft.rows||[];
  if(!rows.length){toast('Nada preparado para importar','info');return;}
  if(!requireWriteAccess('importar transações'))return;
  createImportSnapshot();
  let created=0,reconciled=0,replaced=0,merged=0,skipped=0;
  try{
    for(const row of rows){
      const match=row.matchId?S.transactions.find(t=>t.id===row.matchId):null;
      if(row.decision==='skip'){skipped++;continue;}
      if(row.decision==='reconcile'&&match){
        const nextCategory=match.category==='A classificar'&&row.category!=='A classificar'?row.category:match.category;
        if(cfg.mode==='api')await api('PUT',`/api/transactions/${match.id}`,{type:match.type,description:match.desc,amount:match.amount,category:nextCategory,date:match.date,note:match.note||'',account_id:match.accountId,paid:match.paid,pending:match.pending});
        match.category=nextCategory;
        if(cfg.mode==='local')saveLocal();
        setTxImportMeta(match.id,{reconciled:true,reconciledAt:Date.now(),imported:true,source:importSourceLabel(importDraft.source),batchId:importDraft.batchId});
        reconciled++;
        continue;
      }
      if((row.decision==='replace'||row.decision==='merge')&&match){
        const payload={
          type:row.type||match.type,
          description:row.description||match.desc,
          amount:row.amount||match.amount,
          category:row.category||match.category,
          date:row.date||match.date,
          note:row.decision==='merge'?[match.note,row.note].filter(Boolean).join(' • '):(row.note||match.note||''),
          account_id:row.accountId||match.accountId,
          paid:match.paid,
          pending:match.pending
        };
        if(cfg.mode==='api')await api('PUT',`/api/transactions/${match.id}`,payload);
        Object.assign(match,{type:payload.type,desc:payload.description,amount:payload.amount,category:payload.category,date:payload.date,note:payload.note,accountId:payload.account_id||null});
        if(cfg.mode==='local')saveLocal();
        setTxImportMeta(match.id,{reconciled:true,reconciledAt:Date.now(),imported:true,source:importSourceLabel(importDraft.source),batchId:importDraft.batchId});
        row.decision==='merge'?merged++:replaced++;
        continue;
      }
      const tx={id:uid(),type:row.type,desc:row.description,amount:row.amount,category:row.category||'A classificar',date:row.date,note:row.note||'',accountId:row.accountId||null,installmentGroup:null,installmentNum:null,installmentTotal:null,recurGroup:null,paid:false,pending:row.category==='A classificar'};
      if(cfg.mode==='api'){
        const saved=await api('POST','/api/transactions',{type:tx.type,description:tx.desc,amount:tx.amount,category:tx.category,date:tx.date,note:tx.note,account_id:tx.accountId,paid:false,pending:tx.pending});
        S.transactions.unshift(nTx({...saved,accountId:tx.accountId,pending:tx.pending}));
        setTxImportMeta(saved.id,{imported:true,importedAt:Date.now(),source:importSourceLabel(importDraft.source),batchId:importDraft.batchId});
      }else{
        S.transactions.unshift(tx);
        saveLocal();
        setTxImportMeta(tx.id,{imported:true,importedAt:Date.now(),source:importSourceLabel(importDraft.source),batchId:importDraft.batchId});
      }
      created++;
    }
  }catch(err){
    refreshAll();
    renderImportCenter();
    toast(`Importação interrompida: ${created} novos • ${reconciled} conciliados • ${replaced} substituídos • ${merged} mesclados • ${skipped} ignorados. Erro: ${err.message}`,'error');
    return;
  }
  refreshAll();
  renderImportCenter();
  toast(`Importação concluída: ${created} novos • ${reconciled} conciliados • ${replaced} substituídos • ${merged} mesclados • ${skipped} ignorados`,'success');
}
function openImportInbox(){
  showPage('transactions');
  setTimeout(()=>{
    document.getElementById('fCat').value='A classificar';
    document.getElementById('fTyp').value='all';
    document.getElementById('txSrch').value='';
    renderTx();
  },120);
}
function logout(){if(!confirm('Sair da conta?'))return;adminOverviewState={loading:false,loaded:false,data:null,error:''};localStorage.removeItem(CK);localStorage.removeItem(LK);location.reload();}
function setConn(s){
  const dot=document.getElementById('connDot');if(dot)dot.className='conn-dot '+s;
  const L={online:'Dados sincronizados',offline:'Dados locais',error:'Sem conexao'};
  const lbl=document.getElementById('connLbl');if(lbl)lbl.textContent=L[s]||s;
  document.getElementById('uRole').textContent=cfg.mode==='api'?`Conta online • ${roleLabel(currentUserRole()||'editor')}${isReadOnlyApp()?' • somente leitura':''}`:'Modo local';
}
function openConnModal(){document.getElementById('connUrl').value=cfg.url;document.getElementById('connUser').value=cfg.loginName||'';document.getElementById('connPass').value='';const otp=document.getElementById('connOtp');if(otp)otp.value='';document.getElementById('connModal').classList.add('open');}
async function saveConn(){
  const url=document.getElementById('connUrl').value.trim().replace(/\/$/,'');
  const username=document.getElementById('connUser').value.trim();
  const password=document.getElementById('connPass').value;
  const otp=document.getElementById('connOtp')?.value.trim()||'';
  if(!url||!username||!password){toast('Preencha URL, usuário e senha','error');return;}
  try{
    const r=await fetch(url+'/api/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username,password,otp})});
    if(!r.ok){const e=await r.json().catch(()=>({}));throw new Error(e.error||'Login inválido');}
    const u=await r.json();
    cfg={...cfg,url,key:u.api_key,mode:'api',userName:u.name,userId:u.id,loginName:username,role:u.role||'',twoFactorEnabled:!!u.two_factor_enabled};persistCfg();
    closeM('connModal');await initApp();toast('Salvo!','success');
  }catch(e){toast('Erro: '+e.message,'error');}
}
function nTx(t){return{id:t.id,type:t.type,desc:t.description||t.desc||'',amount:parseFloat(t.amount),category:normCatName(t.category||'A classificar'),date:(t.date||'').substring(0,10),purchaseDate:(t.purchaseDate||t.purchase_date||t.date||'').substring(0,10),note:t.note||'',accountId:t.accountId||t.account_id||null,installmentGroup:t.installmentGroup||t.installment_group||null,installmentNum:t.installmentNum||t.installment_num||null,installmentTotal:t.installmentTotal||t.installment_total||null,recurGroup:t.recurGroup||t.recur_group||null,splitMeta:splitMetaOf(t),paid:t.paid||false,pending:t.pending||false,createdAt:t.createdAt||t.created_at||null,updatedAt:t.updatedAt||t.updated_at||null};}
function nBud(b){return{id:b.id,category:normCatName(b.category),limit:parseFloat(b.limit)};}
function nGoal(g){return{id:g.id,name:g.name,icon:g.icon||'\u{1F3AF}',target:parseFloat(g.target),current:parseFloat(g.current||0),deadline:(g.deadline||'').substring(0,10),desc:g.description||g.desc||'',monthly:parseFloat(g.monthly||0)};}
function nAcc(a){return normalizeAccount({id:a.id,name:a.name,icon:a.icon,type:a.type,balance:a.balance,yieldRate:a.yield_rate,yieldType:a.yield_type,yieldVal:a.yield_val,calcBase:a.calc_base,startDate:(a.start_date||'').substring(0,10),cardClosingDay:a.card_closing_day,cardDueDay:a.card_due_day,cardLast4:a.card_last4,cardExpiry:a.card_expiry,note:a.note});}
function nCat(c){const name=normCatName(c.name);return{id:c.id,ico:c.icon||c.ico||'\u{1F3F7}\uFE0F',name,col:catColor(name,c.color||c.col),custom:true};}
function nShopList(l){return{id:l.id,name:l.name,ico:l.icon||l.ico||'\u{1F6D2}',position:l.position||0};}
function nShopItem(i){return{id:i.id,listId:i.list_id||i.listId,name:i.name,qty:i.qty||'',cat:i.category||i.cat||'\u{1F6D2} Geral',bought:!!i.bought,createdAt:Number(i.created_ms||i.createdAt||Date.now())};}
function defAccs(){return[{id:uid(),name:'Principal',icon:'\u{1F3E6}',type:'checking',balance:0,yieldRate:0,note:''}];}
function clampCardDay(v){return Math.min(31,Math.max(1,parseInt(v,10)||1));}
function monthLastDay(year,monthIndex){return new Date(year,monthIndex+1,0).getDate();}
function buildMonthDate(year,monthIndex,day){
  const safeDay=Math.min(clampCardDay(day),monthLastDay(year,monthIndex));
  return new Date(year,monthIndex,safeDay,12,0,0,0);
}
function toIsoDate(dateObj){return dateObj.toISOString().slice(0,10);}
function formatCardExpiry(v){
  const digits=String(v||'').replace(/\D/g,'').slice(0,4);
  if(!digits)return'';
  if(digits.length<=2)return digits;
  return `${digits.slice(0,2)}/${digits.slice(2)}`;
}
function getCreditCardMeta(account){
  if(!account||account.type!=='credit')return null;
  const closingDay=clampCardDay(account.cardClosingDay||1);
  const dueDay=clampCardDay(account.cardDueDay||closingDay);
  const bestDay=closingDay===31?1:closingDay+1;
  return {closingDay,dueDay,bestDay,expiry:formatCardExpiry(account.cardExpiry||'')};
}
function getCreditCardChargeInfo(account,purchaseDate){
  const meta=getCreditCardMeta(account);
  const purchaseIso=(purchaseDate||today()).substring(0,10);
  if(!meta)return{purchaseDate:purchaseIso,effectiveDate:purchaseIso,label:'',statementDate:purchaseIso};
  const base=new Date(`${purchaseIso}T12:00:00`);
  const purchaseDay=base.getDate();
  const statementMonthOffset=purchaseDay>meta.closingDay?1:0;
  const statementMonth=new Date(base.getFullYear(),base.getMonth()+statementMonthOffset,1,12,0,0,0);
  const statementDate=buildMonthDate(statementMonth.getFullYear(),statementMonth.getMonth(),meta.closingDay);
  const dueMonthOffset=meta.dueDay<=meta.closingDay?1:0;
  const dueBase=new Date(statementMonth.getFullYear(),statementMonth.getMonth()+dueMonthOffset,1,12,0,0,0);
  const effectiveDate=buildMonthDate(dueBase.getFullYear(),dueBase.getMonth(),meta.dueDay);
  return {
    purchaseDate:purchaseIso,
    effectiveDate:toIsoDate(effectiveDate),
    statementDate:toIsoDate(statementDate),
    label:`Compra em ${fmtD(purchaseIso)} • fecha ${fmtD(toIsoDate(statementDate))} • paga ${fmtD(toIsoDate(effectiveDate))}`
  };
}
function getTxEntryDates(accountId,date,type){
  const purchaseDate=(date||today()).substring(0,10);
  const account=S.accounts.find(a=>a.id===accountId);
  if(type!=='expense'||!account||account.type!=='credit'){
    return {purchaseDate,effectiveDate:purchaseDate,label:''};
  }
  return getCreditCardChargeInfo(account,purchaseDate);
}
function asObj(v){return v&&typeof v==='object'&&!Array.isArray(v)?v:{};}
function asArr(v){return Array.isArray(v)?v:[];}
function esc(v){return String(v??'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));}
function defaultSharedOwnerName(){
  return String(cfg.userName||'Eu').trim()||'Eu';
}
function normalizeSharedPerson(person, fallbackName='Pessoa'){
  const name=String(person?.name||fallbackName).trim()||fallbackName;
  return {
    id:String(person?.id||uid()),
    name,
    color:cleanColor(person?.color)||CAT_COLORS[hashStr(name)%CAT_COLORS.length]
  };
}
function normalizeSharedSpace(data={}){
  const input=asObj(data);
  const ownerId=String(input.ownerPersonId||input.owner_person_id||'');
  let people=asArr(input.people).map(p=>normalizeSharedPerson(p)).filter(p=>p.name);
  if(!people.length){
    const ownerName=defaultSharedOwnerName();
    people=[normalizeSharedPerson({id:ownerId||'self',name:ownerName},ownerName)];
  }
  let resolvedOwnerId=ownerId||people[0].id;
  let owner=people.find(p=>p.id===resolvedOwnerId);
  if(!owner){
    owner=normalizeSharedPerson({id:resolvedOwnerId,name:defaultSharedOwnerName()},defaultSharedOwnerName());
    people.unshift(owner);
  }
  owner.name=owner.name||defaultSharedOwnerName();
  return {
    mode:['couple','family','house'].includes(input.mode)?input.mode:'couple',
    name:String(input.name||'').trim(),
    ownerPersonId:owner.id,
    people
  };
}
function sharedModeLabel(mode){
  return ({couple:'Modo casal',family:'Modo família',house:'Modo república/casa'})[mode]||'Modo compartilhado';
}
function getSharedOwner(){
  sharedSpace=normalizeSharedSpace(sharedSpace);
  return sharedSpace.people.find(p=>p.id===sharedSpace.ownerPersonId)||sharedSpace.people[0];
}
function isSharedOwner(id){
  return String(id||'')===String(getSharedOwner()?.id||'');
}
function sharedPeople(){
  sharedSpace=normalizeSharedSpace(sharedSpace);
  return sharedSpace.people;
}
function sharedPersonName(id){
  return sharedPeople().find(p=>p.id===id)?.name||'Pessoa';
}
function updateRatesCache(){
  localStorage.setItem(RATES_KEY,JSON.stringify({
    cdi:RATES.cdi,
    selic:RATES.selic,
    monthlyIncomeCents,
    monthly_income_cents:monthlyIncomeCents,
    dueItems,
    sharedSpace,
    shared_space:sharedSpace,
    avatarData:localStorage.getItem(AVK)||''
  }));
}
function saveSharedSpace(){
  sharedSpace=normalizeSharedSpace(sharedSpace);
  updateRatesCache();
  if(cfg.mode==='api')saveRemoteState().catch(e=>toast('Erro ao salvar espaço compartilhado: '+e.message,'error'));
  else noteLocalSave('Espaço compartilhado salvo localmente');
}
function splitMetaOf(tx){
  return asObj(tx?.splitMeta||tx?.split_meta);
}
function approvalMetaOf(tx){
  return asObj(splitMetaOf(tx).approval);
}
function splitNeedsApproval(tx){
  return approvalMetaOf(tx).status==='pending';
}
function splitRejected(tx){
  return approvalMetaOf(tx).status==='rejected';
}
function splitApproved(tx){
  const approval=approvalMetaOf(tx);
  return !approval.status||approval.status==='approved';
}
function txSplitBadge(tx){
  const meta=splitMetaOf(tx);
  if(meta.kind==='equal'&&Array.isArray(meta.participants)&&meta.participants.length>1)return '🤝 dividido';
  if(meta.kind==='settlement')return '🔁 acerto';
  return '';
}
function settlementDescription(fromId,toId){
  return `Acerto: ${sharedPersonName(fromId)} → ${sharedPersonName(toId)}`;
}
function summarizeEqualSplit(meta, amount){
  const payer=sharedPersonName(meta.payerId);
  const participants=asArr(meta.participants).map(sharedPersonName);
  const share=participants.length?amount/participants.length:0;
  return `${payer} pagou ${rawFmt(amount)} • ${participants.length} pessoa(s) • ${rawFmt(share)} por pessoa`;
}
function computeSharedBalances(){
  const owner=getSharedOwner();
  const balances={};
  for(const person of sharedPeople()){
    if(person.id!==owner.id)balances[person.id]=0;
  }
  for(const tx of S.transactions){
    const meta=splitMetaOf(tx);
    if(meta.kind==='equal'&&tx.type==='expense'){
      if(!splitApproved(tx))continue;
      const participants=[...new Set(asArr(meta.participants).filter(Boolean))];
      if(participants.length<2)continue;
      const payerId=String(meta.payerId||owner.id);
      const share=Number(tx.amount||0)/participants.length;
      if(!share)continue;
      if(payerId===owner.id){
        participants.filter(id=>id!==owner.id).forEach(id=>{
          balances[id]=(balances[id]||0)+share;
        });
      }else if(participants.includes(owner.id)){
        balances[payerId]=(balances[payerId]||0)-share;
      }
    }else if(meta.kind==='settlement'){
      const fromId=String(meta.fromPersonId||'');
      const toId=String(meta.toPersonId||'');
      const amount=Number(tx.amount||0);
      if(!amount)continue;
      if(fromId===owner.id&&balances[toId]!==undefined)balances[toId]+=amount;
      if(toId===owner.id&&balances[fromId]!==undefined)balances[fromId]-=amount;
    }
  }
  return balances;
}
function mergeSharedInvitePayload(payload={}){
  const base=normalizeSharedSpace(sharedSpace);
  const incoming=normalizeSharedSpace({
    mode:payload.mode,
    name:payload.name,
    ownerPersonId:payload.ownerPersonId,
    people:payload.people
  });
  const owner=getSharedOwner();
  const people=[owner,...incoming.people.filter(p=>String(p.id)!==String(owner.id))];
  const mergedByName=[];
  const seen=new Set();
  people.forEach(person=>{
    const key=person.name.trim().toLowerCase();
    if(seen.has(key))return;
    seen.add(key);
    mergedByName.push(normalizeSharedPerson(person,person.name));
  });
  sharedSpace=normalizeSharedSpace({
    mode:incoming.mode||base.mode,
    name:incoming.name||base.name,
    ownerPersonId:owner.id,
    people:mergedByName
  });
  saveSharedSpace();
}
function buildSharedInviteLink(){
  const owner=getSharedOwner();
  const payload={
    v:1,
    mode:sharedSpace.mode,
    name:sharedSpace.name,
    ownerPersonId:owner.id,
    people:sharedPeople().map(p=>({id:p.id,name:p.name,color:p.color}))
  };
  const encoded=btoa(unescape(encodeURIComponent(JSON.stringify(payload))));
  const url=new URL(window.location.href);
  url.searchParams.set('invite',encoded);
  return url.toString();
}
async function copySharedInviteLink(){
  const people=sharedPeople();
  if(people.length<2){toast('Adicione pelo menos uma pessoa ao espaço antes de convidar','info');return;}
  const link=buildSharedInviteLink();
  try{
    await navigator.clipboard.writeText(link);
    toast('Link de convite copiado','success');
  }catch{
    prompt('Copie o link do convite',link);
  }
}
function consumeSharedInviteFromUrl(){
  const params=new URLSearchParams(window.location.search);
  const raw=params.get('invite');
  if(!raw)return;
  try{
    const payload=JSON.parse(decodeURIComponent(escape(atob(raw))));
    const accepted=confirm(`Entrar no espaço compartilhado "${payload.name||'Finanza compartilhado'}"?`);
    if(accepted){
      mergeSharedInvitePayload(payload);
      toast('Convite aplicado ao espaço compartilhado','success');
    }
  }catch{
    toast('Não foi possível ler o convite compartilhado','error');
  }
  params.delete('invite');
  const next=`${window.location.pathname}${params.toString()?`?${params}`:''}${window.location.hash||''}`;
  window.history.replaceState({},'',next);
}
function populateSharedPersonSelect(selectId, selectedId=''){
  const el=document.getElementById(selectId);if(!el)return;
  const people=sharedPeople();
  el.innerHTML=people.map(p=>`<option value="${p.id}">${esc(p.name)}</option>`).join('');
  if(selectedId&&people.some(p=>p.id===selectedId))el.value=selectedId;
}
function renderTxSplitParticipants(selectedIds=[]){
  const wrap=document.getElementById('txSplitParticipants');if(!wrap)return;
  const selected=new Set(selectedIds.length?selectedIds:[getSharedOwner().id]);
  wrap.innerHTML=sharedPeople().map(person=>`<label style="display:flex;align-items:center;gap:8px;padding:10px 12px;border:1px solid var(--bd);border-radius:12px;background:var(--sf2)"><input type="checkbox" value="${person.id}" ${selected.has(person.id)?'checked':''} onchange="syncTxSplitHint()"><span style="display:inline-flex;align-items:center;gap:8px"><span style="width:10px;height:10px;border-radius:999px;background:${person.color}"></span>${esc(person.name)}</span></label>`).join('');
}
function selectedTxSplitParticipants(){
  return [...document.querySelectorAll('#txSplitParticipants input[type="checkbox"]:checked')].map(el=>el.value);
}
function syncTxSplitHint(){
  const hint=document.getElementById('txSplitHint');if(!hint)return;
  const mode=document.getElementById('txSplitMode')?.value||'none';
  const payerId=document.getElementById('txSplitPayer')?.value||getSharedOwner().id;
  const participants=selectedTxSplitParticipants();
  if(mode==='none'){hint.textContent='Essa despesa não entra na divisão.';return;}
  if(participants.length<2){hint.textContent='Escolha pelo menos duas pessoas para dividir.';return;}
  const amount=parseFloat(document.getElementById('txAmt')?.value)||0;
  const share=amount&&participants.length?amount/participants.length:0;
  hint.textContent=`${sharedPersonName(payerId)} pagou. Cada pessoa fica com ${share?rawFmt(share):'R$ 0,00'}.`;
}
function syncTxSplitUI(){
  const wrap=document.getElementById('txSplitWrap');
  const participantsWrap=document.getElementById('txSplitParticipantsWrap');
  const approvalWrap=document.getElementById('txApprovalWrap');
  const mode=document.getElementById('txSplitMode')?.value||'none';
  const isExpense=getTyp()==='expense';
  if(wrap)wrap.style.display=isExpense?'block':'none';
  if(participantsWrap)participantsWrap.style.display=mode==='equal'&&isExpense?'block':'none';
  if(approvalWrap)approvalWrap.style.display=mode==='equal'&&isExpense?'block':'none';
  syncTxSplitHint();
}
function splitMetaFromForm(){
  if(getTyp()!=='expense')return {};
  const mode=document.getElementById('txSplitMode')?.value||'none';
  if(mode!=='equal')return {};
  const participants=[...new Set(selectedTxSplitParticipants())];
  if(participants.length<2)return {};
  const approvalRequested=!!document.getElementById('txNeedApproval')?.checked;
  return {
    kind:'equal',
    payerId:document.getElementById('txSplitPayer')?.value||getSharedOwner().id,
    participants,
    approval:approvalRequested?{
      status:'pending',
      requestedAt:Date.now(),
      requestedBy:getSharedOwner().id
    }:{
      status:'approved',
      approvedAt:Date.now(),
      approvedBy:getSharedOwner().id
    }
  };
}
function fillTxSplitForm(meta={}){
  populateSharedPersonSelect('txSplitPayer',meta.payerId||getSharedOwner().id);
  const mode=meta.kind==='equal'?'equal':'none';
  const modeEl=document.getElementById('txSplitMode');
  if(modeEl)modeEl.value=mode;
  const approvalEl=document.getElementById('txNeedApproval');
  if(approvalEl)approvalEl.checked=asObj(meta.approval).status==='pending';
  renderTxSplitParticipants(asArr(meta.participants).length?asArr(meta.participants):[getSharedOwner().id]);
  syncTxSplitUI();
}
function saveSharedSpaceFromInputs(){
  if(!requireWriteAccess('gerenciar espaço compartilhado'))return;
  sharedSpace={
    ...sharedSpace,
    mode:document.getElementById('sharedMode')?.value||sharedSpace.mode,
    name:document.getElementById('sharedName')?.value.trim()||sharedSpace.name
  };
  saveSharedSpace();
  renderShared();
}
function renameSharedOwner(name){
  if(!requireWriteAccess('gerenciar espaço compartilhado'))return;
  const owner=getSharedOwner();
  owner.name=String(name||'').trim()||defaultSharedOwnerName();
  sharedSpace=normalizeSharedSpace(sharedSpace);
  saveSharedSpace();
  renderShared();
}
function addSharedPerson(){
  if(!requireWriteAccess('gerenciar pessoas compartilhadas'))return;
  const inp=document.getElementById('sharedPersonName');
  const name=String(inp?.value||'').trim();
  if(!name){toast('Informe o nome da pessoa','error');return;}
  if(sharedPeople().some(p=>p.name.toLowerCase()===name.toLowerCase())){toast('Essa pessoa já existe','error');return;}
  sharedSpace.people.push(normalizeSharedPerson({name},name));
  if(inp)inp.value='';
  saveSharedSpace();
  renderShared();
  fillTxSplitForm(splitMetaFromForm());
}
function removeSharedPerson(id){
  if(!requireWriteAccess('gerenciar pessoas compartilhadas'))return;
  if(isSharedOwner(id)){toast('Seu perfil principal não pode ser removido','error');return;}
  sharedSpace.people=sharedPeople().filter(p=>p.id!==id);
  sharedSpace=normalizeSharedSpace(sharedSpace);
  saveSharedSpace();
  renderShared();
  fillTxSplitForm(splitMetaFromForm());
}
function openSettlementModal(personId=''){
  if(!requireWriteAccess('registrar acertos'))return;
  populateSharedPersonSelect('settlementFrom',getSharedOwner().id);
  populateSharedPersonSelect('settlementTo',personId||sharedPeople().find(p=>p.id!==getSharedOwner().id)?.id||getSharedOwner().id);
  const acc=document.getElementById('settlementAccount');
  if(acc)acc.innerHTML=S.accounts.map(a=>`<option value="${a.id}">${esc(a.icon||'🏦')} ${esc(a.name)}</option>`).join('');
  if(acc&&S.accounts[0]?.id)acc.value=S.accounts[0].id;
  document.getElementById('settlementAmount').value='';
  document.getElementById('settlementDate').value=today();
  document.getElementById('settlementNote').value='';
  document.getElementById('settlementModal').classList.add('open');
}
function openSettlementFromBalance(personId){
  const owner=getSharedOwner();
  const amount=computeSharedBalances()[personId]||0;
  openSettlementModal(personId);
  if(amount>0){
    document.getElementById('settlementFrom').value=personId;
    document.getElementById('settlementTo').value=owner.id;
  }else{
    document.getElementById('settlementFrom').value=owner.id;
    document.getElementById('settlementTo').value=personId;
  }
  document.getElementById('settlementAmount').value=Math.abs(amount).toFixed(2);
}
async function saveSettlement(){
  if(!requireWriteAccess('registrar acertos'))return;
  const fromId=document.getElementById('settlementFrom').value;
  const toId=document.getElementById('settlementTo').value;
  const amount=parseFloat(document.getElementById('settlementAmount').value)||0;
  const date=document.getElementById('settlementDate').value||today();
  const note=document.getElementById('settlementNote').value.trim();
  const accountId=document.getElementById('settlementAccount').value||S.accounts[0]?.id||null;
  const owner=getSharedOwner();
  if(!fromId||!toId||fromId===toId){toast('Escolha quem pagou e quem recebeu','error');return;}
  if(!amount){toast('Informe o valor do acerto','error');return;}
  if(fromId!==owner.id&&toId!==owner.id){toast('O acerto precisa envolver você','error');return;}
  const type=fromId===owner.id?'expense':'income';
  const desc=settlementDescription(fromId,toId);
  const splitMeta={kind:'settlement',fromPersonId:fromId,toPersonId:toId};
  try{
    if(cfg.mode==='api'){
      const saved=await api('POST','/api/transactions',{type,description:desc,amount,category:'Outros',date,note,account_id:accountId,paid:false,pending:false,split_meta:splitMeta});
      S.transactions.unshift(nTx({...saved,accountId,split_meta:splitMeta}));
    }else{
      S.transactions.unshift({id:uid(),type,desc,amount,category:'Outros',date,note,accountId,installmentGroup:null,installmentNum:null,installmentTotal:null,recurGroup:null,splitMeta,paid:false,pending:false});
      saveLocal();
    }
    closeM('settlementModal');
    toast('Acerto registrado','success');
    refreshAll();
  }catch(e){toast('Erro: '+e.message,'error');}
}
function normalizeImportCenterState(raw={}){
  const base=asObj(raw);
  return {
    profiles:{csv:asObj(base.profiles?.csv)},
    rules:{
      categories:asArr(base.rules?.categories).map(r=>({id:String(r.id||uid()),match:String(r.match||''),category:normCatName(r.category||'A classificar')})).filter(r=>r.match),
      subscriptions:asArr(base.rules?.subscriptions).map(r=>({id:String(r.id||uid()),day:Number(r.day)||0,amount:Number(r.amount)||0,description:String(r.description||''),category:normCatName(r.category||'Assinaturas')})).filter(r=>r.day&&r.amount>0)
    },
    snapshots:asArr(base.snapshots).slice(0,5),
    txMeta:asObj(base.txMeta)
  };
}
function nSubscription(item={}){
  const rawStatus=String(item.status||'active');
  return {
    id:String(item.id||uid()),
    name:String(item.name||'Assinatura').trim()||'Assinatura',
    amount:Number(item.amount)||0,
    category:normCatName(item.category||'Assinaturas'),
    billingDay:Math.min(31,Math.max(1,Number(item.billingDay||item.billing_day||new Date((item.renewalDate||item.renewal_date||today())+'T12:00:00').getDate())||1)),
    renewalDate:String(item.renewalDate||item.renewal_date||today()).substring(0,10),
    paymentMethod:String(item.paymentMethod||item.payment_method||'credit'),
    paymentPlace:String(item.paymentPlace||item.payment_place||''),
    accountId:item.accountId||item.account_id||'',
    usage:['high','medium','low'].includes(item.usage)?item.usage:'medium',
    status:['active','paused','cancelled'].includes(rawStatus)?rawStatus:'active',
    notes:String(item.notes||item.note||''),
    linkedDueId:String(item.linkedDueId||item.linked_due_id||'')
  };
}
function nDebt(item={}){
  const totalAmount=Math.max(0,Number(item.totalAmount||item.total_amount)||0);
  const outstandingAmount=Math.max(0,Number(item.outstandingAmount||item.outstanding_amount)||totalAmount);
  const totalInstallments=Math.max(1,Number(item.totalInstallments||item.total_installments)||1);
  const remainingInstallments=Math.max(0,Math.min(totalInstallments,Number(item.remainingInstallments||item.remaining_installments)||totalInstallments));
  const rawStatus=String(item.status||'active');
  return {
    id:String(item.id||uid()),
    name:String(item.name||'Divida').trim()||'Divida',
    totalAmount,
    outstandingAmount,
    installmentAmount:Math.max(0,Number(item.installmentAmount||item.installment_amount)||0),
    totalInstallments,
    remainingInstallments,
    interestRate:Math.max(0,Number(item.interestRate||item.interest_rate)||0),
    nextDueDate:String(item.nextDueDate||item.next_due_date||today()).substring(0,10),
    accountId:item.accountId||item.account_id||'',
    strategy:['snowball','avalanche','custom'].includes(item.strategy)?item.strategy:'custom',
    status:['active','watch','closed'].includes(rawStatus)?rawStatus:(remainingInstallments<=0||outstandingAmount<=0?'closed':'active'),
    notes:String(item.notes||item.note||''),
    linkedDueId:String(item.linkedDueId||item.linked_due_id||'')
  };
}
function nContract(item={}){
  const rawStatus=String(item.status||'active');
  return {
    id:String(item.id||uid()),
    name:String(item.name||'Contrato').trim()||'Contrato',
    kind:String(item.kind||'service'),
    monthlyAmount:Math.max(0,Number(item.monthlyAmount||item.monthly_amount)||0),
    provider:String(item.provider||''),
    renewalDate:String(item.renewalDate||item.renewal_date||today()).substring(0,10),
    adjustmentDate:String(item.adjustmentDate||item.adjustment_date||'').substring(0,10),
    accountId:item.accountId||item.account_id||'',
    status:['active','watch','ended'].includes(rawStatus)?rawStatus:'active',
    notes:String(item.notes||item.note||''),
    linkedDueId:String(item.linkedDueId||item.linked_due_id||'')
  };
}
function normalizeCommitmentsState(raw={}){
  const base=asObj(raw);
  return {
    subscriptions:asArr(base.subscriptions).map(nSubscription).filter(item=>item.name&&item.amount>=0),
    debts:asArr(base.debts).map(nDebt).filter(item=>item.name&&item.totalAmount>=0),
    contracts:asArr(base.contracts).map(nContract).filter(item=>item.name)
  };
}
function loadCommitments(){
  try{commitmentsState=normalizeCommitmentsState(JSON.parse(localStorage.getItem(COMMITMENTS_KEY)||'{}'));}catch{commitmentsState=normalizeCommitmentsState();}
}
function saveCommitments(persistRemote=true){
  commitmentsState=normalizeCommitmentsState(commitmentsState);
  localStorage.setItem(COMMITMENTS_KEY,JSON.stringify(commitmentsState));
  noteLocalSave('Compromissos salvos neste dispositivo');
  if(persistRemote&&cfg.mode==='api')saveRemoteState().catch(e=>toast('Erro ao salvar compromissos: '+e.message,'error'));
}
function loadImportCenterState(){
  try{importCenterState=normalizeImportCenterState(JSON.parse(localStorage.getItem(IMPORT_CENTER_KEY)||'{}'));}catch{importCenterState=normalizeImportCenterState();}
}
function saveImportCenterState(persistRemote=true){
  importCenterState=normalizeImportCenterState(importCenterState);
  localStorage.setItem(IMPORT_CENTER_KEY,JSON.stringify(importCenterState));
  if(persistRemote&&cfg.mode==='api')saveRemoteState().catch(()=>{});
}
function getTxImportMeta(id){return asObj(importCenterState.txMeta[id]);}
function setTxImportMeta(id,patch){
  importCenterState.txMeta[id]={...getTxImportMeta(id),...patch};
  saveImportCenterState();
}
function clearImportDraft(){
  importDraft={source:'csv',files:[],rows:[],headers:[],mapping:{},dedupe:'exact',profileName:'',text:'',batchId:'',balanceDivergence:null};
}
function nCarVehicle(v={}){
  return {
    id:String(v.id||uid()),
    name:String(v.name||'Meu carro'),
    plate:String(v.plate||''),
    model:String(v.model||''),
    odometer:Number(v.odometer)||0
  };
}
function nCarEvent(e={}){
  const type=e.type==='service'||e.type==='expense'?'expense':'fuel';
  const liters=Number(e.liters)||0;
  const pricePerLiter=Number(e.pricePerLiter??e.price_per_liter)||0;
  const amount=Number(e.amount)||Number((liters*pricePerLiter).toFixed(2))||0;
  return {
    id:String(e.id||uid()),
    vehicleId:String(e.vehicleId||e.vehicle_id||''),
    type,
    date:String(e.date||today()).substring(0,10),
    odometer:Number(e.odometer)||0,
    fuelType:String(e.fuelType||e.fuel_type||'Gasolina'),
    liters,
    pricePerLiter,
    amount,
    title:String(e.title||''),
    category:String(e.category||'Combustivel'),
    note:String(e.note||''),
    accountId:e.accountId||e.account_id||null,
    txId:e.txId||e.tx_id||null,
    createdAt:Number(e.createdAt||e.created_at||Date.now())
  };
}
function normalizeCarState(data={}){
  const vehicles=asArr(data.vehicles).map(nCarVehicle);
  const fallback=vehicles[0]?.id||uid();
  if(!vehicles.length)vehicles.push({id:fallback,name:'Meu carro',plate:'',model:'',odometer:0});
  const events=asArr(data.events).map(e=>nCarEvent({...e,vehicleId:e.vehicleId||e.vehicle_id||fallback})).filter(e=>e.amount>0||e.liters>0);
  let activeVehicleId=String(data.activeVehicleId||data.active_vehicle_id||'');
  if(!vehicles.some(v=>v.id===activeVehicleId))activeVehicleId=vehicles[0]?.id||fallback;
  return {vehicles,events,activeVehicleId};
}
function loadCar(){
  if(carState?.vehicles?.length)return;
  try{carState=normalizeCarState(JSON.parse(localStorage.getItem(CAR_KEY)||'{}'));}catch{carState=normalizeCarState();}
}
function saveCar(){
  localStorage.setItem(CAR_KEY,JSON.stringify(carState));
  noteLocalSave('Carro atualizado localmente');
  if(cfg.mode==='api')saveRemoteState().catch(e=>toast('Erro ao salvar carro: '+e.message,'error'));
}
function compactImportCenterForSync(raw=importCenterState){
  const state=normalizeImportCenterState(raw);
  const txIds=new Set((S.transactions||[]).slice(0,600).map(t=>String(t.id)));
  const txMeta={};
  Object.entries(state.txMeta||{}).forEach(([id,meta])=>{
    if(txIds.has(String(id)))txMeta[id]=meta;
  });
  return {...state,snapshots:[],txMeta};
}
function avatarDataForSync(){
  const data=localStorage.getItem(AVK)||'';
  return data.length<=300000?data:'';
}
function getAppSettings(){
  const avatarData=avatarDataForSync();
  const nextAccent=localStorage.getItem('next_accent')||'#35c96f';
  return {theme:document.documentElement.dataset.theme||localStorage.getItem('fz_t')||'dark',nextAccent,next_accent:nextAccent,rates:{cdi:RATES.cdi,selic:RATES.selic,monthlyIncomeCents,monthly_income_cents:monthlyIncomeCents,dueItems,car:carState?.vehicles?.length?carState:normalizeCarState(),sharedSpace,shared_space:sharedSpace,avatarData,avatar_data:avatarData},widgetPrefs,widgetOrder,widgetFilters,sidebarShortcuts:sidebarShortcutPrefs,txView:curView,activeList:slActiveList,importCenter:compactImportCenterForSync(),commitments:commitmentsState};
}
function applyRemoteSettings(settings={}){
  if(settings.theme)applyTheme(settings.theme);
  if(settings.nextAccent||settings.next_accent)localStorage.setItem('next_accent',settings.nextAccent||settings.next_accent);
  const rates=settings.rates||{};
  if(rates.cdi)RATES.cdi=parseFloat(rates.cdi);
  if(rates.selic)RATES.selic=parseFloat(rates.selic);
  const income=rates.monthlyIncomeCents??rates.monthly_income_cents;
  if(income!==undefined&&income!==null&&!Number.isNaN(Number(income)))monthlyIncomeCents=Math.max(0,Math.round(Number(income)));
  if(Array.isArray(rates.dueItems)||Array.isArray(rates.due_items)){
    dueItems=(rates.dueItems||rates.due_items).map(nDue).filter(Boolean);
    localStorage.setItem(DUE_KEY,JSON.stringify(dueItems));
  }
  if(rates.car){
    carState=normalizeCarState(rates.car);
    localStorage.setItem(CAR_KEY,JSON.stringify(carState));
  }
  sharedSpace=normalizeSharedSpace(rates.sharedSpace||rates.shared_space||sharedSpace);
  const avatarData=rates.avatarData??rates.avatar_data;
  if(typeof avatarData==='string'){
    if(avatarData)localStorage.setItem(AVK,avatarData);
    else localStorage.removeItem(AVK);
    applyAvatar();
  }
  updateRatesCache();
  widgetPrefs=asObj(settings.widget_prefs||settings.widgetPrefs||widgetPrefs);
  widgetOrder=asArr(settings.widget_order||settings.widgetOrder||widgetOrder);
  widgetFilters=asObj(settings.widget_filters||settings.widgetFilters||widgetFilters);
  sidebarShortcutPrefs=normalizeSidebarShortcuts(settings.sidebar_shortcuts||settings.sidebarShortcuts||sidebarShortcutPrefs);
  importCenterState=normalizeImportCenterState(settings.import_center||settings.importCenter||importCenterState);
  commitmentsState=normalizeCommitmentsState(settings.commitments||settings.commitmentCenter||commitmentsState);
  localStorage.setItem(IMPORT_CENTER_KEY,JSON.stringify(importCenterState));
  localStorage.setItem(COMMITMENTS_KEY,JSON.stringify(commitmentsState));
  localStorage.setItem(SIDEBAR_SHORTCUTS_KEY,JSON.stringify(sidebarShortcutPrefs));
  renderSidebarShortcuts();
  if(settings.tx_view||settings.txView)localStorage.setItem(VK,settings.tx_view||settings.txView);
  if(settings.active_list||settings.activeList)slActiveList=settings.active_list||settings.activeList;
}
async function saveRemoteState(){
  if(cfg.mode!=='api')return;
  const shopping=sl?.lists?.length?sl:(()=>{try{return JSON.parse(localStorage.getItem(SL_KEY)||'{}');}catch{return{lists:[],items:[]};}})();
  const car=carState?.vehicles?.length?carState:(()=>{try{return normalizeCarState(JSON.parse(localStorage.getItem(CAR_KEY)||'{}'));}catch{return normalizeCarState();}})();
  setSaveState('syncing','Sincronizando alterações com a nuvem');
  try{
    await api('PUT','/api/state',{accounts:S.accounts,categories:custCats,shopping,car,settings:getAppSettings()});
    setSaveState('synced','Tudo sincronizado com a conta online');
    logSyncEvent('success','Sincronização concluída',`${S.transactions.length} transações • ${car.events?.length||0} registros do carro`);
  }catch(err){
    setSaveState('error','Falha ao sincronizar com o servidor',{error:err.message});
    logSyncEvent('error','Falha na sincronização',err.message);
    showConnBar('error','Falha ao sincronizar',3200);
    throw err;
  }
}
function persistLocalOrRemote(){
  if(cfg.mode==='api')saveRemoteState().catch(e=>toast('Erro ao salvar estado: '+e.message,'error'));
  else saveLocal();
}
const ATYPES={checking:'Corrente',savings:'Poupança',credit:'Cartão',cash:'Dinheiro',investment:'Investimento'};
function getAccBal(id){
  const a=S.accounts.find(x=>x.id===id);if(!a)return 0;
  return a.balance+S.transactions.filter(t=>t.accountId===id&&!isFut(t.date)&&!t.paid).reduce((s,t)=>s+(t.type==='income'?t.amount:-t.amount),0);
}
function loadRates(){
  try{
    const r=JSON.parse(localStorage.getItem(RATES_KEY)||'{}');
    if(r.cdi)RATES.cdi=parseFloat(r.cdi);
    if(r.selic)RATES.selic=parseFloat(r.selic);
    const income=r.monthlyIncomeCents??r.monthly_income_cents;
    if(income!==undefined&&income!==null&&!Number.isNaN(Number(income)))monthlyIncomeCents=Math.max(0,Math.round(Number(income)));
    if(Array.isArray(r.dueItems)||Array.isArray(r.due_items))dueItems=(r.dueItems||r.due_items).map(nDue).filter(Boolean);
    sharedSpace=normalizeSharedSpace(r.sharedSpace||r.shared_space||sharedSpace);
  }catch{}
}
function nDue(d){
  if(!d)return null;
  const amount=Number(d.amount)||0;
  const day=Math.min(31,Math.max(1,parseInt(d.dueDay||d.due_day||new Date((d.nextDueDate||d.next_due_date||today())+'T12:00:00').getDate())||1));
  return {
    id:String(d.id||uid()),
    name:String(d.name||'Vencimento'),
    amount,
    category:normCatName(d.category||'A classificar'),
    recurrence:d.recurrence||'monthly',
    nextDueDate:(d.nextDueDate||d.next_due_date||today()).substring(0,10),
    dueDay:day,
    paymentMethod:d.paymentMethod||d.payment_method||'pix',
    paymentPlace:d.paymentPlace||d.payment_place||'',
    accountId:d.accountId||d.account_id||'',
    notifyDays:Array.isArray(d.notifyDays||d.notify_days)?(d.notifyDays||d.notify_days).map(Number):[3,1,0],
    notes:d.notes||d.note||'',
    active:d.active!==false,
    paidKeys:Array.isArray(d.paidKeys||d.paid_keys)?(d.paidKeys||d.paid_keys):[]
  };
}
function loadDueItems(){
  try{
    const raw=localStorage.getItem(DUE_KEY);
    if(raw)dueItems=JSON.parse(raw).map(nDue).filter(Boolean);
  }catch{dueItems=[];}
}
function saveDueItems(){
  dueItems=dueItems.map(nDue).filter(Boolean);
  localStorage.setItem(DUE_KEY,JSON.stringify(dueItems));
  noteLocalSave('Vencimentos salvos localmente');
  if(cfg.mode==='api')saveRemoteState().catch(e=>toast('Erro ao salvar vencimentos: '+e.message,'error'));
}
function parseMoneyToCents(v){
  const raw=String(v||'').replace(/\s/g,'').replace(/^R\$/i,'');
  if(!raw)return 0;
  const normalized=raw.includes(',')?raw.replace(/\./g,'').replace(',','.'):raw;
  const n=parseFloat(normalized);
  return Number.isFinite(n)?Math.max(0,Math.round(n*100)):0;
}
function formatCentsInput(cents){
  return cents?String((cents/100).toFixed(2)).replace('.',','):'';
}
function updateRates(){
  if(!requireWriteAccess('alterar referências globais'))return;
  const cdi=parseFloat(document.getElementById('setCDI')?.value);
  const selic=parseFloat(document.getElementById('setSelic')?.value);
  const incomeEl=document.getElementById('setIncome');
  if(cdi>0)RATES.cdi=cdi;
  if(selic>0)RATES.selic=selic;
  if(incomeEl)monthlyIncomeCents=parseMoneyToCents(incomeEl.value);
  updateRatesCache();
  if(cfg.mode==='api')saveRemoteState().catch(e=>toast('Erro ao salvar taxas: '+e.message,'error'));
  toast(`Configurações atualizadas`,'success');
  renderAccs();renderDash();
}
function calcEffectiveRate(type,val){
  switch(type){
    case 'cdi_pct':return RATES.cdi*val/100;
    case 'cdi_plus':return RATES.cdi+val;
    case 'fixed':return val;
    case 'selic':return RATES.selic*val/100;
    case 'poupanca':{
      const mensal=RATES.selic>8.5?0.5:(RATES.selic/100)*0.70/12*100;
      return (Math.pow(1+mensal/100,12)-1)*100+RATES.tr;
    }
    default:return val;
  }
}
function getAccRateInfo(a){
  if(a.type==='investment'&&a.yieldType&&a.yieldType!=='manual'){
    const aa=calcEffectiveRate(a.yieldType,parseFloat(a.yieldVal)||0);
    const base=a.calcBase||'du';
    const days=base==='du'?21:30;
    const daysYear=base==='du'?252:360;
    const monthly=(Math.pow(1+aa/100,days/daysYear)-1)*100;
    return {monthly,annual:aa,label:`${aa.toFixed(2)}% a.a.`};
  }
  const monthly=parseFloat(a.yieldRate)||0;
  return {monthly,annual:(Math.pow(1+monthly/100,12)-1)*100,label:`${monthly}% a.m.`};
}
function getAccYield(a,months=1){
  const bal=Math.max(getAccBal(a.id),0);
  const rate=(getAccRateInfo(a).monthly||0)/100;
  if(!bal||!rate)return 0;
  return bal*(Math.pow(1+rate,months)-1);
}
function getYieldSummary(){
  const accounts=S.accounts.map(a=>({account:a,month:getAccYield(a,1)})).filter(x=>x.month>0);
  const month=accounts.reduce((s,x)=>s+x.month,0);
  return {accounts,month,day:month/30,year:S.accounts.reduce((s,a)=>s+getAccYield(a,12),0)};
}
function openAccModal(id=null){
  if(!requireWriteAccess(id?'editar contas':'criar contas'))return;
  accEditId=id;
  const a=id?normalizeAccount(S.accounts.find(x=>x.id===id)||{}):null;
  document.getElementById('accModalTitle').textContent=a?'Editar Conta':'Nova Conta';
  document.getElementById('accModalSub').textContent=a?'Ajuste saldo, tipo e rendimento':'Conta bancária, carteira ou investimento';
  document.getElementById('accNm').value=a?.name||'';
  document.getElementById('accIco').value=a?.icon||'';
  document.getElementById('accTyp').value=a?.type||'checking';
  document.getElementById('accBal').value=a?.balance??'';
  document.getElementById('accYield').value=a?.yieldRate??'';
  document.getElementById('accYieldType').value=a?.yieldType||'cdi_pct';
  document.getElementById('accYieldVal').value=a?.yieldVal||100;
  document.getElementById('accCalcBase').value=a?.calcBase||'du';
  document.getElementById('accStartDate').value=a?.startDate||'';
  document.getElementById('accCardClosingDay').value=a?.cardClosingDay||'';
  document.getElementById('accCardDueDay').value=a?.cardDueDay||'';
  document.getElementById('accCardLast4').value=a?.cardLast4||'';
  document.getElementById('accCardExpiry').value=formatCardExpiry(a?.cardExpiry||'');
  document.getElementById('accCardBestDay').value=a?.type==='credit'&&a?.cardClosingDay?String((Number(a.cardClosingDay)%31)+1).padStart(2,'0'):'';
  document.getElementById('accNote').value=a?.note||'';
  toggleAccInvFields();
  updAccYieldPreview();
  document.getElementById('accModal').classList.add('open');
}
function openCreditCardModal(){
  openAccModal();
  document.getElementById('accTyp').value='credit';
  document.getElementById('accIco').value=document.getElementById('accIco').value||'💳';
  toggleAccInvFields();
}
function saveAcc(){
  if(!requireWriteAccess(accEditId?'editar contas':'criar contas'))return;
  const name=document.getElementById('accNm').value.trim();
  const icon=document.getElementById('accIco').value.trim()||'🏦';
  const type=document.getElementById('accTyp').value;
  const balance=parseFloat(document.getElementById('accBal').value)||0;
  const yieldRate=parseFloat(document.getElementById('accYield').value)||0;
  const note=document.getElementById('accNote').value.trim();
  const yieldType=document.getElementById('accYieldType')?.value||'manual';
  const yieldVal=parseFloat(document.getElementById('accYieldVal')?.value)||100;
  const calcBase=document.getElementById('accCalcBase')?.value||'du';
  const startDate=document.getElementById('accStartDate')?.value||'';
  const cardClosingDay=Math.min(31,Math.max(0,parseInt(document.getElementById('accCardClosingDay')?.value,10)||0));
  const cardDueDay=Math.min(31,Math.max(0,parseInt(document.getElementById('accCardDueDay')?.value,10)||0));
  const cardLast4=String(document.getElementById('accCardLast4')?.value||'').replace(/\D/g,'').slice(-4);
  const cardExpiry=formatCardExpiry(document.getElementById('accCardExpiry')?.value||'');
  if(!name){toast('Informe o nome','error');return;}
  if(type==='credit'&&!cardClosingDay){toast('Informe o dia de fechamento do cartão','error');return;}
  if(type==='credit'&&!cardDueDay){toast('Informe o dia de vencimento do cartão','error');return;}
  const rateInfo=type==='investment'?getAccRateInfo({type,yieldType,yieldVal,calcBase,yieldRate}):{monthly:yieldRate};
  const account={id:accEditId||uid(),name,icon,type,balance,yieldRate:type==='investment'?Number(rateInfo.monthly.toFixed(4)):yieldRate,yieldType,yieldVal,calcBase,startDate,cardClosingDay:type==='credit'?cardClosingDay:0,cardDueDay:type==='credit'?cardDueDay:0,cardLast4:type==='credit'?cardLast4:'',cardExpiry:type==='credit'?cardExpiry:'',note};
  if(accEditId){
    const i=S.accounts.findIndex(a=>a.id===accEditId);
    if(i>=0)S.accounts[i]=account;
  }else S.accounts.push(account);
  persistLocalOrRemote();closeM('accModal');renderAccs();renderDash();popAccSels();toast(accEditId?'Conta atualizada! ✓':'Conta criada! ✓','success');
  accEditId=null;
}
function toggleAccInvFields(){
  const type=document.getElementById('accTyp')?.value;
  const fields=document.getElementById('accInvFields');
  const cardFields=document.getElementById('accCardFields');
  if(fields)fields.style.display=type==='investment'?'block':'none';
  if(cardFields)cardFields.style.display=type==='credit'?'block':'none';
  const bestDay=document.getElementById('accCardBestDay');
  const closingDay=parseInt(document.getElementById('accCardClosingDay')?.value,10)||0;
  if(bestDay)bestDay.value=type==='credit'&&closingDay?String((closingDay%31)+1).padStart(2,'0'):'';
  updAccYieldPreview();
}
function updAccYieldPreview(){
  const el=document.getElementById('accYieldPreview');if(!el)return;
  const type=document.getElementById('accYieldType')?.value||'cdi_pct';
  const val=parseFloat(document.getElementById('accYieldVal')?.value)||0;
  const base=document.getElementById('accCalcBase')?.value||'du';
  const bal=parseFloat(document.getElementById('accBal')?.value)||0;
  const start=document.getElementById('accStartDate')?.value;
  const lbl=document.getElementById('accYieldLbl');
  const labels={cdi_pct:'% do CDI',cdi_plus:'% a.a. (spread)',fixed:'% a.a.',selic:'% da Selic',poupanca:''};
  if(lbl)lbl.textContent=labels[type]||'%';
  if(!val&&type!=='poupanca'){el.textContent='Digite o percentual para ver a projeo.';return;}
  if(!bal){el.textContent='Digite o saldo para ver a projeo.';return;}
  const aa=calcEffectiveRate(type,val);
  const diasAno=base==='du'?252:360;
  const daily=Math.pow(1+aa/100,1/diasAno)-1;
  const r1m=Math.pow(1+daily,base==='du'?21:30)-1;
  const r3m=Math.pow(1+daily,base==='du'?63:90)-1;
  const r12m=Math.pow(1+daily,diasAno)-1;
  let acum='';
  if(start){
    const diasPassados=Math.floor((Date.now()-new Date(start+'T12:00:00'))/86400000);
    const diasCalc=base==='du'?Math.round(diasPassados*252/365):diasPassados;
    const rAcum=Math.pow(1+daily,Math.max(0,diasCalc))-1;
    if(diasPassados>0)acum=`<br><strong>Rendimento acumulado:</strong> <span style="color:var(--ac)">${fmt(bal*rAcum)}</span> em ${diasPassados} dias`;
  }
  el.innerHTML=`<strong>Taxa efetiva:</strong> ${aa.toFixed(2)}% a.a.  ${base==='du'?'dias teis':'dias corridos'}<br><strong>Rendimento estimado:</strong><br>1 ms: <span style="color:var(--ac)">${fmt(bal*r1m)}</span>  3 meses: <span style="color:var(--ac)">${fmt(bal*r3m)}</span>  12 meses: <span style="color:var(--ac)">${fmt(bal*r12m)}</span>${acum}<br><span style="font-size:10px;opacity:.65">CDI ${RATES.cdi}% a.a.  Selic ${RATES.selic}% a.a.</span>`;
}
function delAcc(id){
  if(!requireWriteAccess('remover contas'))return;
  if(S.accounts.length<=1){toast('Mnimo 1 conta','error');return;}
  if(!confirm('Remover esta conta?'))return;
  S.accounts=S.accounts.filter(a=>a.id!==id);persistLocalOrRemote();renderAccs();popAccSels();
}
function popAccSels(){
  const opts=S.accounts.map(a=>`<option value="${a.id}">${a.icon} ${a.name}</option>`).join('');
  const aOpts='<option value="all">Todas</option>'+opts;
  ['txAcc'].forEach(id=>{const e=document.getElementById(id);if(e)e.innerHTML=opts;});
  ['trFrom','trTo'].forEach(id=>{const e=document.getElementById(id);if(e)e.innerHTML=opts;});
  const fa=document.getElementById('fAcc');if(fa)fa.innerHTML=aOpts;
}
async function doTransfer(){
  if(!requireWriteAccess('transferir entre contas'))return;
  const from=document.getElementById('trFrom').value,to=document.getElementById('trTo').value;
  const amount=parseFloat(document.getElementById('trAmt').value);
  if(from===to){toast('Contas iguais','error');return;}
  if(!amount||amount<=0){toast('Valor inválido','error');return;}
  const fA=S.accounts.find(a=>a.id===from),tA=S.accounts.find(a=>a.id===to);
  const now=today();
  const mk=(type,desc,accId)=>({id:uid(),type,desc,amount,category:'Outros',date:now,note:'',accountId:accId,installmentGroup:null,installmentNum:null,installmentTotal:null,recurGroup:null,paid:false,pending:false});
  const out=mk('expense',`Transferência → ${tA.name}`,from);
  const inc=mk('income',`Transferência ← ${fA.name}`,to);
  try{
    if(cfg.mode==='api'){
      const saved=await Promise.all([out,inc].map(t=>api('POST','/api/transactions',{type:t.type,description:t.desc,amount:t.amount,category:t.category,date:t.date,note:t.note,account_id:t.accountId,paid:false,pending:false})));
      S.transactions.unshift(...saved.map(nTx));
    }else{
      S.transactions.unshift(out,inc);
      saveLocal();
    }
    document.getElementById('trAmt').value='';renderAccs();renderDash();toast(`${fmt(amount)} transferido! OK`,'success');
  }catch(e){toast('Erro: '+e.message,'error');}
}
function renderAccs(){
  const el=document.getElementById('accGrid');if(!el)return;
  const total=S.accounts.reduce((s,a)=>s+getAccBal(a.id),0);
  const invs=S.accounts.filter(a=>a.type==='investment');
  const cards=S.accounts.filter(a=>a.type==='credit');
  const invTotal=invs.reduce((s,a)=>s+Math.max(getAccBal(a.id),0),0);
  const y=getYieldSummary();
  const best=[...S.accounts].sort((a,b)=>getAccYield(b,1)-getAccYield(a,1))[0];
  const stats=document.getElementById('accStats');
  if(stats)stats.innerHTML=`
    <div class="insight-card"><div class="insight-k">Patrimônio</div><div class="insight-v money ${total>=0?'neu':'neg'}">${fmt(total)}</div></div>
    <div class="insight-card"><div class="insight-k">Cartões</div><div class="insight-v" style="color:var(--warn)">${cards.length}</div><div class="cc">${cards.filter(a=>a.cardClosingDay&&a.cardDueDay).length} com ciclo completo</div></div>
    <div class="insight-card"><div class="insight-k">Investido</div><div class="insight-v" style="color:var(--ac2)">${fmt(invTotal)}</div><div class="cc">${invs.length} conta${invs.length!==1?'s':''}</div></div>
    <div class="insight-card"><div class="insight-k">Rendimento mensal</div><div class="insight-v" style="color:var(--ac)">${fmt(y.month)}</div><div class="cc">${fmt(y.year)}/ano estimado</div></div>
    <div class="insight-card"><div class="insight-k">Melhor conta</div><div class="insight-v" style="font-size:16px">${best?best.icon+' '+best.name:''}</div><div class="cc">${best?fmt(getAccYield(best,1))+'/mês':'sem dados'}</div></div>`;
  const cardCenter=document.getElementById('accCardCenter');
  if(cardCenter)cardCenter.innerHTML=cards.length?cards.map(a=>{
    const meta=getCreditCardMeta(a);
    const last4=a.cardLast4?`•••• ${a.cardLast4}`:'sem final';
    const expiry=a.cardExpiry||'validade não informada';
    return `<div class="card-center-item"><div class="card-center-top"><div><div class="card-center-name">${esc(a.name)}</div><div class="card-center-sub">${esc(last4)} • ${esc(expiry)}</div></div><div class="card-center-icon">${esc(a.icon||'💳')}</div></div><div class="card-center-meta"><span>Fecha dia ${meta?.closingDay||'—'}</span><span>Vence dia ${meta?.dueDay||'—'}</span><span>Melhor dia ${meta?.bestDay||'—'}</span></div><div class="card-center-actions"><button class="btn btn-g btn-sm" ${writeActionAttrs('editar contas')} onclick="openAccModal('${a.id}')">Editar</button><button class="btn btn-d btn-sm" ${writeActionAttrs('remover contas')} onclick="delAcc('${a.id}')">Remover</button></div></div>`;
  }).join(''):`<div class="card-center-empty"><strong>Nenhum cartão salvo.</strong><span>Cadastre seus cartões aqui para organizar fechamento, vencimento e compras futuras.</span></div>`;
  el.innerHTML=S.accounts.map(a=>{
    const bal=getAccBal(a.id);
    const y=getAccYield(a,1);
    const rate=getAccRateInfo(a);
    const cardMeta=a.type==='credit'?getCreditCardMeta(a):null;
    const yLbl=a.type==='credit'
      ? `<div class="yield-detail"><span class="yield-pill">Fecha ${cardMeta?.closingDay||'—'}</span><span>Vence ${cardMeta?.dueDay||'—'}</span></div>`
      : y>0?`<div class="yield-detail"><span class="yield-pill">+${fmt(y)}/mês</span><span>${rate.label}</span></div>`:`<div class="yield-detail">Sem rendimento configurado</div>`;
    const noteExtra=a.type==='credit'&&a.cardLast4?`<div style="font-size:11px;color:var(--mt);margin-top:6px">Final ${esc(a.cardLast4)}${a.cardExpiry?` • validade ${esc(a.cardExpiry)}`:''}</div>`:'';
    return`<div class="bg-card"><div style="display:flex;justify-content:space-between;margin-bottom:10px"><div style="font-size:24px">${a.icon}</div><div style="display:flex;align-items:center;gap:4px"><span style="font-size:11px;color:var(--mt);background:var(--sf2);border:1px solid var(--bd);border-radius:5px;padding:2px 6px">${ATYPES[a.type]||a.type}</span><button class="ib" ${writeActionAttrs('editar contas')} onclick="openAccModal('${a.id}')" title="Editar">✏️</button><button class="ib del" ${writeActionAttrs('remover contas')} onclick="delAcc('${a.id}')" title="Remover">🗑️</button></div></div><div style="font-size:13px;font-weight:600;margin-bottom:3px">${a.name}</div><div class="money" style="font-size:20px;font-weight:700;color:${bal>=0?'var(--ac)':'var(--dan)'}">${fmt(bal)}</div>${yLbl}${noteExtra}${a.note?`<div style="font-size:11px;color:var(--mt);margin-top:6px">${a.note}</div>`:''}</div>`;
  }).join('')+`<div class="bg-card" style="border-style:dashed;display:flex;align-items:center;justify-content:center;gap:7px;color:var(--mt);font-size:12px;cursor:pointer;" ${canWriteAppData()?'':'title="Conta em modo leitura"'} onclick="openAccModal()"><span style="font-size:20px">+</span> Nova Conta</div>`;
}
let qaOpen=false;
function toggleQA(){
  qaOpen=!qaOpen;
  document.getElementById('qaPanel').classList.toggle('open',qaOpen);
  const fab=document.getElementById('fabBtn');fab.classList.toggle('open',qaOpen);fab.textContent=qaOpen?'✕':'+';
  if(qaOpen){qaVal='';qaSelCat='';renderQACats();updQADisp();}
}
function qaType(t){
  qaTyp=t;
  document.getElementById('qaE').className='qa-tb'+(t==='expense'?' active expense':'');
  document.getElementById('qaI').className='qa-tb'+(t==='income'?' active income':'');
  updQADisp();
}
function qaSelC(n){qaSelCat=qaSelCat===n?'':n;renderQACats();}
function qaK(k){
  if(k==='del')qaVal=qaVal.slice(0,-1);
  else if(k==='.')qaVal.includes('.')||(qaVal+=k);
  else qaVal.length<8&&(qaVal+=k);
  updQADisp();
}
function updQADisp(){
  const v=parseFloat(qaVal)||0;
  const el=document.getElementById('qaAmt');
  el.textContent=v?fmt(v):'R$ 0';
  el.className='qa-amt '+(qaTyp==='expense'?'expense':'income');
}
async function qaSave(){
  if(!requireWriteAccess('lançar transações'))return;
  const amount=parseFloat(qaVal)||0;
  if(!amount){toast('Digite um valor','error');return;}
  const cat=qaSelCat||'A classificar';
  const note=document.getElementById('qaNote').value.trim();
  const pending=!qaSelCat;
  const tx={id:uid(),type:qaTyp,desc:note||cat,amount,category:cat,date:today(),note:'',accountId:S.accounts[0]?.id||null,installmentGroup:null,installmentNum:null,installmentTotal:null,recurGroup:null,paid:false,pending};
  if(cfg.mode==='api'){try{const r=await api('POST','/api/transactions',{type:qaTyp,description:tx.desc,amount,category:cat,date:tx.date,note:'',account_id:tx.accountId,paid:false,pending});S.transactions.unshift(nTx({...r,accountId:tx.accountId,pending}));}catch(e){toast('Erro: '+e.message,'error');return;}}
  else{S.transactions.unshift(tx);saveLocal();}
  toast(`${fmt(amount)} salvo${pending?'  classifique depois':''} ✓`,'success');
  qaVal='';qaSelCat='';document.getElementById('qaNote').value='';updQADisp();renderQACats();toggleQA();renderDash();
}
function renderSrchScopes(){
  const el=document.getElementById('srchScopes');
  if(!el)return;
  const defs=[['all','Tudo'],['transactions','Transações'],['future','Vencimentos'],['car','Carro'],['lists','Compras']];
  el.innerHTML=defs.map(([id,label])=>`<button class="srch-chip ${srchScope===id?'active':''}" onclick="setSrchScope('${id}')">${label}</button>`).join('');
}
function setSrchScope(scope='all'){
  srchScope=scope||'all';
  renderSrchScopes();
  doSrch(document.getElementById('srchInp')?.value||'');
}
function getSearchQuickActions(){
  return `<div class="srch-actions">
    <button class="srch-action-btn" ${writeActionAttrs('criar transações')} onclick="closeSrch();openModal()">+ Transação</button>
    <button class="srch-action-btn" ${writeActionAttrs('criar vencimentos')} onclick="closeSrch();openDueModal()">+ Vencimento</button>
    <button class="srch-action-btn" ${writeActionAttrs('criar registros do carro')} onclick="closeSrch();openCarEntry('fuel')">+ Abastecimento</button>
    <button class="srch-action-btn" onclick="closeSrch();showPage('dashboard')">Dashboard</button>
  </div>`;
}
function openSrch(q=''){document.getElementById('srchOv').classList.add('open');renderSrchScopes();const i=document.getElementById('srchInp');i.value=q;i.focus();doSrch(q);}
function closeSrch(){document.getElementById('srchOv').classList.remove('open');}
function searchScore(texts=[],query=''){
  const hay=texts.map(normalizeTxText).join(' ');
  const q=normalizeTxText(query);
  if(!hay||!q)return 0;
  if(hay===q)return 120;
  if(hay.startsWith(q))return 90;
  if(hay.includes(q))return 70;
  const parts=q.split(/\s+/).filter(Boolean);
  const matched=parts.filter(p=>hay.includes(p)).length;
  return matched?matched*18:0;
}
function searchItem(meta){
  return {...meta,score:searchScore(meta.texts,meta.query)};
}
function stopSearchClick(ev){
  if(ev)ev.stopPropagation();
}
function searchActions(item){
  const id=esc(item.id);
  if(item.kind==='transaction'){
    const payAction=item.type==='expense'?`<button onclick="stopSearchClick(event);closeSrch();markPaid('${id}')">${item.paid?'Desmarcar':'Pagar'}</button>`:'';
    return `<div class="srch-inline-actions">
      <button onclick="stopSearchClick(event);closeSrch();openModal('${id}')">Editar</button>
      <button onclick="stopSearchClick(event);closeSrch();dupTx('${id}')">Duplicar</button>
      ${payAction}
    </div>`;
  }
  if(item.kind==='due'){
    return `<div class="srch-inline-actions">
      <button onclick="stopSearchClick(event);closeSrch();openDueModal('${id}')">Editar</button>
      <button onclick="stopSearchClick(event);closeSrch();payDue('${id}', '${esc(item.date||today())}')">Pagar</button>
      <button onclick="stopSearchClick(event);closeSrch();postponeDue('${id}', '${esc(item.date||today())}', 'week')">Adiar</button>
    </div>`;
  }
  if(item.kind==='account')return `<div class="srch-inline-actions"><button onclick="stopSearchClick(event);closeSrch();openAccModal('${id}')">Abrir conta</button></div>`;
  if(item.kind==='goal')return `<div class="srch-inline-actions"><button onclick="stopSearchClick(event);closeSrch();showPage('goals')">Abrir meta</button></div>`;
  return '';
}
function openSearchTarget(kind,id,aux=''){
  closeSrch();
  if(kind==='transaction'){
    showPage('transactions');
    setTimeout(()=>hlTx(id),300);
    return;
  }
  if(kind==='goal'){showPage('goals');return;}
  if(kind==='budget'){showPage('budget');return;}
  if(kind==='due'){
    showPage('future');
    setTimeout(()=>openDueModal(id),260);
    return;
  }
  if(kind==='shopping'){
    loadSL();
    if(id&&sl.lists.some(l=>l.id===id))slActiveList=id;
    showPage('shopping');
    return;
  }
  if(kind==='account'){showPage('accounts');return;}
  if(kind==='vehicle'){
    loadCar();
    if(id&&carState.vehicles.some(v=>v.id===id)){
      carFilters.vehicle=id;
      carState.activeVehicleId=id;
      saveCar();
    }
    showPage('car');
    return;
  }
  if(kind==='car-event'){
    loadCar();
    const ev=carState.events.find(e=>e.id===id);
    if(ev){
      carFilters.vehicle=ev.vehicleId||'all';
      carState.activeVehicleId=ev.vehicleId||carState.activeVehicleId;
      carFilters.query=ev.title||ev.note||ev.fuelType||'';
      saveCar();
    }
    showPage('car');
    return;
  }
  showPage(aux||'dashboard');
}
function doSrch(q){
  const el=document.getElementById('srchRes');
  if(!q||q.length<2){el.innerHTML=getSearchQuickActions()+'<div class="srch-empty">Digite para buscar...</div>';return;}
  loadSL();loadCar();
  const txs=S.transactions.map(t=>searchItem({
    query:q,
    kind:'transaction',
    id:t.id,
    type:t.type,
    paid:t.paid,
    texts:[t.desc,t.category,t.note,S.accounts.find(a=>a.id===t.accountId)?.name],
    title:t.desc,
    meta:[fmtD(t.date),t.category,t.note].filter(Boolean).join(' • '),
    amount:`${t.type==='income'?'+':'-'}${fmt(t.amount)}`,
    tone:t.type==='income'?'var(--ac)':'var(--dan)',
    icon:getCat(t.category).ico,
    action:"openSearchTarget('transaction','"+t.id+"')"
  })).filter(x=>x.score>0).sort((a,b)=>b.score-a.score||String(b.meta).localeCompare(String(a.meta))).slice(0,8);
  const goals=S.goals.map(g=>searchItem({
    query:q,
    kind:'goal',
    id:g.id,
    texts:[g.name,g.desc,g.deadline],
    title:g.name,
    meta:[fmt(g.current)+' de '+fmt(g.target),g.deadline?`vence ${fmtD(g.deadline)}`:''].filter(Boolean).join(' • '),
    icon:g.icon||'🎯',
    action:"openSearchTarget('goal','"+g.id+"')"
  })).filter(x=>x.score>0).sort((a,b)=>b.score-a.score).slice(0,4);
  const buds=S.budgets.map(b=>searchItem({
    query:q,
    kind:'budget',
    id:b.id,
    texts:[b.category],
    title:b.category,
    meta:`Limite ${fmt(b.limit)}`,
    icon:getCat(b.category).ico,
    action:"openSearchTarget('budget','"+b.id+"')"
  })).filter(x=>x.score>0).sort((a,b)=>b.score-a.score).slice(0,4);
  const dues=dueItems.map(d=>searchItem({
    query:q,
    kind:'due',
    id:d.id,
    date:d.nextDueDate||today(),
    texts:[d.name,d.category,d.paymentPlace,d.notes,methodLabel(d.paymentMethod)],
    title:d.name,
    meta:[fmt(d.amount),d.category,d.nextDueDate?fmtD(d.nextDueDate):'',d.paymentPlace].filter(Boolean).join(' • '),
    icon:getCat(d.category).ico,
    action:"openSearchTarget('due','"+d.id+"')"
  })).filter(x=>x.score>0).sort((a,b)=>b.score-a.score).slice(0,5);
  const shopItems=sl.items.map(item=>{
    const list=sl.lists.find(l=>l.id===item.listId);
    return searchItem({
      query:q,
      kind:'shopping',
      id:item.listId,
      texts:[item.name,item.qty,item.cat,list?.name],
      title:item.name,
      meta:[list?.name,item.cat,item.qty,item.bought?'comprado':'pendente'].filter(Boolean).join(' • '),
      icon:list?.ico||'🛒',
      action:"openSearchTarget('shopping','"+item.listId+"')"
    });
  }).filter(x=>x.score>0).sort((a,b)=>b.score-a.score).slice(0,5);
  const vehicles=carState.vehicles.map(v=>searchItem({
    query:q,
    kind:'vehicle',
    id:v.id,
    texts:[v.name,v.model,v.plate,String(v.odometer||'')],
    title:v.name,
    meta:[v.model,v.plate,v.odometer?`${Math.round(v.odometer).toLocaleString('pt-BR')} km`:'' ].filter(Boolean).join(' • '),
    icon:'🚗',
    action:"openSearchTarget('vehicle','"+v.id+"')"
  })).filter(x=>x.score>0).sort((a,b)=>b.score-a.score).slice(0,4);
  const carEvents=carState.events.map(e=>searchItem({
    query:q,
    kind:'car-event',
    id:e.id,
    texts:[e.title,e.note,e.fuelType,e.category,vehicleName(e.vehicleId)],
    title:e.type==='fuel'?(e.fuelType||'Combustível'):(e.title||carExpenseLabel(e.category)),
    meta:[vehicleName(e.vehicleId),fmtD(e.date),fmt(e.amount),e.note].filter(Boolean).join(' • '),
    icon:e.type==='fuel'?'⛽':'🔧',
    action:"openSearchTarget('car-event','"+e.id+"')"
  })).filter(x=>x.score>0).sort((a,b)=>b.score-a.score).slice(0,5);
  const accounts=S.accounts.map(a=>searchItem({
    query:q,
    kind:'account',
    id:a.id,
    texts:[a.name,a.bank,a.type],
    title:a.name,
    meta:[a.bank,a.type,a.icon].filter(Boolean).join(' • '),
    icon:a.icon||'💳',
    action:"openSearchTarget('account','"+a.id+"')"
  })).filter(x=>x.score>0).sort((a,b)=>b.score-a.score).slice(0,4);
  const groups=[
    {label:'Transações',items:txs},
    {label:'Vencimentos',items:dues},
    {label:'Carro',items:[...vehicles,...carEvents].sort((a,b)=>b.score-a.score).slice(0,6)},
    {label:'Lista de compras',items:shopItems},
    {label:'Metas',items:goals},
    {label:'Orçamentos',items:buds},
    {label:'Contas',items:accounts}
  ].filter(g=>g.items.length).filter(g=>{
    if(srchScope==='all')return true;
    if(srchScope==='transactions')return g.label==='Transações';
    if(srchScope==='future')return g.label==='Vencimentos';
    if(srchScope==='car')return g.label==='Carro';
    if(srchScope==='lists')return g.label==='Lista de compras';
    return true;
  });
  if(!groups.length){el.innerHTML=getSearchQuickActions()+'<div class="srch-empty">Nenhum resultado.</div>';return;}
  const total=groups.reduce((s,g)=>s+g.items.length,0);
  let html=getSearchQuickActions();
  html+=`<div class="srch-summary">${total} resultado${total!==1?'s':''} em ${groups.length} área${groups.length!==1?'s':''}</div>`;
  html+=groups.map(group=>`<div class="srch-group"><div class="srch-lbl">${group.label}</div>${group.items.map(item=>`<div class="srch-row" role="button" tabindex="0" onclick="${item.action}" onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();${item.action}}"><div class="srch-ico">${item.icon||'•'}</div><div style="flex:1;min-width:0"><div class="srch-title">${esc(item.title)}</div><div class="srch-meta">${esc(item.meta||'')}</div>${searchActions(item)}</div>${item.amount?`<div class="srch-amt" style="${item.tone?`color:${item.tone}`:''}">${esc(item.amount)}</div>`:''}</div>`).join('')}</div>`).join('');
  el.innerHTML=html;
}
function handleModalEnter(e){
  if(e.defaultPrevented)return false;
  if(e.key!=='Enter'||e.shiftKey||e.ctrlKey||e.altKey||e.metaKey)return false;
  const tag=(e.target?.tagName||'').toLowerCase();
  if(['button','textarea','select'].includes(tag))return false;
  if(document.getElementById('txModal')?.classList.contains('open')){e.preventDefault();saveTx();return true;}
  if(document.getElementById('dueModal')?.classList.contains('open')){e.preventDefault();saveDue();return true;}
  if(document.getElementById('budModal')?.classList.contains('open')){e.preventDefault();saveBud();return true;}
  if(document.getElementById('goalModal')?.classList.contains('open')){e.preventDefault();saveGoal();return true;}
  return false;
}
document.addEventListener('keydown',e=>{if((e.ctrlKey||e.metaKey)&&e.key==='k'){e.preventDefault();openSrch();return;}if(handleModalEnter(e))return;if(e.key==='Escape'){closeSrch();document.querySelectorAll('.ov.open').forEach(o=>o.classList.remove('open'));}});
function refreshTxDateHint(){
  const label=document.getElementById('txDateLabel');
  const hint=document.getElementById('txCardHint');
  if(!label||!hint)return;
  const type=getTyp();
  const accountId=document.getElementById('txAcc')?.value||S.accounts[0]?.id||null;
  const date=document.getElementById('txDt')?.value||today();
  const info=getTxEntryDates(accountId,date,type);
  if(info.label){
    label.textContent='Data da compra';
    hint.textContent=info.label;
    hint.style.display='block';
  }else{
    label.textContent='Data';
    hint.textContent='';
    hint.style.display='none';
  }
}
function openModal(id=null,futDate=false){
  if(!requireWriteAccess(id?'editar transações':'criar transações'))return;
  editId=id;const tx=id?S.transactions.find(t=>t.id===id):null;
  document.getElementById('mTit').textContent=tx?'Editar Transação':'Nova Transação';
  document.getElementById('mSub').textContent=tx?'Edite os dados':'Registre uma receita ou despesa';
  const quick=document.getElementById('txQuickText');if(quick)quick.value='';
  const quickPrev=document.getElementById('txQuickPreview');if(quickPrev)quickPrev.textContent='Digite uma frase para ver a prévia antes de salvar.';
  document.getElementById('txDesc').value=tx?.desc||'';
  document.getElementById('txAmt').value=tx?.amount||'';
  document.getElementById('txDt').value=tx?.purchaseDate||tx?.date||(futDate?addM(today(),1):today());
  document.getElementById('txNote').value=tx?.note||'';
  popCatSels();popAccSels();
  setTyp(tx?.type||'expense');
  fillTxSplitForm(splitMetaOf(tx));
  if(tx?.category)document.getElementById('txCat').value=tx.category;
  if(tx?.accountId)document.getElementById('txAcc').value=tx.accountId;
  document.getElementById('instChk').checked=false;document.getElementById('instSec').style.display='none';
  document.getElementById('recChk').checked=false;document.getElementById('recSec').style.display='none';
  refreshTxDateHint();
  document.getElementById('txModal').classList.add('open');
}
function dupTx(id){
  if(!requireWriteAccess('duplicar transações'))return;
  const tx=S.transactions.find(t=>t.id===id);if(!tx)return;
  editId=null;
  document.getElementById('mTit').textContent='Duplicar Transação';
  document.getElementById('txDesc').value=tx.desc;document.getElementById('txAmt').value=tx.amount;
  document.getElementById('txDt').value=tx.purchaseDate||today();document.getElementById('txNote').value=tx.note;
  popCatSels();popAccSels();document.getElementById('txCat').value=tx.category;
  setTyp(tx.type);
  fillTxSplitForm(splitMetaOf(tx));
  if(tx.accountId)document.getElementById('txAcc').value=tx.accountId;
  document.getElementById('instChk').checked=false;document.getElementById('instSec').style.display='none';
  document.getElementById('recChk').checked=false;document.getElementById('recSec').style.display='none';
  refreshTxDateHint();
  document.getElementById('txModal').classList.add('open');
}
function normalizeTxText(s){
  return String(s||'').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'');
}
function parseTextAmount(text){
  const m=String(text||'').match(/(?:r\$\s*)?(\d{1,3}(?:\.\d{3})*,\d{1,2}|\d+(?:[.,]\d{1,2})?)/i);
  if(!m)return 0;
  const raw=m[1].replace(/\./g,'').replace(',','.');
  const n=parseFloat(raw);
  return Number.isFinite(n)?n:0;
}
function titleCleanText(text){
  return String(text||'')
    .replace(/\b(hoje|ontem|amanh[aã]|paguei|gastei|comprei|recebi|receita|despesa|fixo|fixa|mensal|todo mes|todo mês|vence|vencimento|dia|no|na|em|pelo|pela|com|cartao|cartão)\b/gi,' ')
    .replace(/\s+/g,' ')
    .trim();
}
function inferTxCategory(text,type){
  const n=normalizeTxText(text);
  const rules=[
    ['Salário',['salario','pagamento','holerite','renda']],
    ['Freelance',['freela','freelance','job','cliente']],
    ['Alimentação',['mercado','supermercado','ifood','restaurante','lanche','padaria','pizza','comida']],
    ['Transporte',['uber','99','gasolina','combustivel','onibus','metro','estacionamento']],
    ['Moradia',['aluguel','condominio','luz','agua','internet','energia']],
    ['Saúde',['farmacia','remedio','medico','consulta','exame']],
    ['Lazer',['cinema','bar','show','jogo','lazer']],
    ['Tecnologia',['notebook','celular','iphone','software','app']],
    ['Assinaturas',['netflix','spotify','prime','assinatura']]
  ];
  for(const [cat,keys] of rules)if(keys.some(k=>n.includes(k)))return cat;
  if(type==='income')return allCats().find(c=>normalizeTxText(c.name).includes('salario'))?.name||'Salário';
  return 'A classificar';
}
function inferTxAccount(text,desc='',category=''){
  const source=normalizeTxText([text,desc,category].join(' '));
  const explicit=S.accounts.find(a=>source.includes(normalizeTxText(a.name)));
  if(explicit)return explicit.id;
  const recent=[...S.transactions]
    .filter(t=>t.accountId&&(normalizeTxText(t.desc).includes(normalizeTxText(desc))||normalizeTxText(t.category)===normalizeTxText(category)))
    .sort((a,b)=>b.date.localeCompare(a.date));
  return recent[0]?.accountId||S.accounts[0]?.id||'';
}
function parseTxDate(text){
  const n=normalizeTxText(text);
  if(n.includes('ontem'))return offD(new Date(),-1);
  if(n.includes('amanha'))return offD(new Date(),1);
  const inDays=n.match(/\b(?:em|daqui)\s+(\d{1,2})\s+dias?\b/);
  if(inDays)return offD(new Date(),parseInt(inDays[1],10));
  const day=n.match(/\b(?:dia|vence(?:\s+dia)?|vencimento(?:\s+dia)?)\s+(\d{1,2})\b/);
  if(day){
    const now=new Date();
    const d=new Date(now.getFullYear(),now.getMonth(),Math.min(parseInt(day[1],10),31),12);
    if(d.toISOString().slice(0,10)<today())d.setMonth(d.getMonth()+1);
    return d.toISOString().slice(0,10);
  }
  return today();
}
function inferTxRecurrence(text,desc,amount,type){
  const n=normalizeTxText(text);
  if(/\b(mensal|todo mes|todo mês|recorrente|fixo|fixa|assinatura)\b/.test(n))return true;
  if(type==='income')return false;
  const similar=S.transactions.filter(t=>t.type===type&&Math.abs(t.amount-amount)<0.01&&normalizeTxText(t.desc).includes(normalizeTxText(desc).slice(0,8))).length;
  return similar>=2;
}
function parseTxText(text){
  const amount=parseTextAmount(text);
  if(!amount)return null;
  const n=normalizeTxText(text);
  const type=/(recebi|receita|salario|pix recebido|entrada|ganhei|freela|freelance)/.test(n)?'income':'expense';
  const date=parseTxDate(text);
  const pending=/(vence|vencimento|a pagar|boleto|conta fixa|fixo|fixa)/.test(n)||date>today();
  const category=inferTxCategory(text,type);
  const accountNames=S.accounts.map(a=>String(a.name||'').replace(/[.*+?^${}()|[\]\\]/g,'\\$&')).filter(Boolean);
  let desc=String(text||'')
    .replace(/(?:r\$\s*)?\d{1,3}(?:\.\d{3})*,\d{1,2}|(?:r\$\s*)?\d+(?:[.,]\d{1,2})?/i,'')
    .replace(/\b(?:em|daqui)\s+\d{1,2}\s+dias?\b/gi,'')
    .replace(/\b(?:dia|vence(?:\s+dia)?|vencimento(?:\s+dia)?)\s+\d{1,2}\b/gi,'')
    .trim();
  if(accountNames.length)desc=desc.replace(new RegExp(accountNames.join('|'),'gi'),'').trim();
  const clean=titleCleanText(desc);
  const finalDesc=clean||category;
  const accountId=inferTxAccount(text,finalDesc,category);
  return {type,amount,date,desc:finalDesc,category,accountId,pending,recurring:inferTxRecurrence(text,finalDesc,amount,type)};
}
function previewTxFromText(){
  const el=document.getElementById('txQuickPreview');if(!el)return;
  const text=document.getElementById('txQuickText')?.value||'';
  const parsed=parseTxText(text);
  if(!text.trim()){el.className='quick-preview';el.textContent='Digite uma frase para ver a prévia antes de salvar.';return;}
  if(!parsed){el.className='quick-preview warn';el.textContent='Ainda falta um valor. Ex: mercado 82,40 hoje nubank';return;}
  const acc=S.accounts.find(a=>a.id===parsed.accountId);
  const bits=[
    parsed.type==='income'?'Receita':'Despesa',
    rawFmt(parsed.amount),
    parsed.category,
    fmtD(parsed.date),
    acc?`${acc.icon} ${acc.name}`:'',
    parsed.pending?'futuro/pendente':'',
    parsed.recurring?'recorrente sugerido':''
  ].filter(Boolean);
  el.className='quick-preview ok';
  el.innerHTML=`<strong>Prévia:</strong> ${bits.map(esc).join(' • ')}`;
}
function fillTxFromText(){
  const input=document.getElementById('txQuickText');
  const parsed=parseTxText(input?.value||'');
  if(!parsed){toast('Escreva algo como: mercado 82,40 hoje','error');return;}
  setTyp(parsed.type);
  document.getElementById('txDesc').value=parsed.desc;
  document.getElementById('txAmt').value=parsed.amount.toFixed(2);
  document.getElementById('txDt').value=parsed.date;
  popCatSels();
  const cat=document.getElementById('txCat');
  if([...cat.options].some(o=>o.value===parsed.category))cat.value=parsed.category;
  popAccSels();
  const acc=document.getElementById('txAcc');
  if(parsed.accountId&&[...acc.options].some(o=>o.value===parsed.accountId))acc.value=parsed.accountId;
  document.getElementById('recChk').checked=!!parsed.recurring;
  togRec();
  if(parsed.recurring)document.getElementById('recN').value=document.getElementById('recN').value||12;
  if(parsed.pending)document.getElementById('txNote').value='Lançamento sugerido pelo texto rápido';
  updIPrev();
  syncTxSplitUI();
  previewTxFromText();
  toast('Campos preenchidos','success');
}
function fillAndSaveTxFromText(){
  fillTxFromText();
  const parsed=parseTxText(document.getElementById('txQuickText')?.value||'');
  if(parsed)setTimeout(()=>saveTx(),40);
}
function togInst(){
  const on=document.getElementById('instChk').checked;
  document.getElementById('instSec').style.display=on?'block':'none';
  if(on){document.getElementById('recChk').checked=false;document.getElementById('recSec').style.display='none';document.getElementById('instSt').value=document.getElementById('txDt').value||today();}
}
function togRec(){
  const on=document.getElementById('recChk').checked;
  document.getElementById('recSec').style.display=on?'block':'none';
  if(on){document.getElementById('instChk').checked=false;document.getElementById('instSec').style.display='none';}
}
function updIPrev(){
  if(!document.getElementById('instChk').checked)return;
  const total=parseFloat(document.getElementById('txAmt').value)||0;
  const n=parseInt(document.getElementById('instN').value)||0;
  const start=document.getElementById('instSt').value;
  if(!total||!n||!start){document.getElementById('iPrev').textContent='Preencha valor e parcelas.';return;}
  const per=Math.round(total/n*100)/100;
  const dates=Array.from({length:n},(_,i)=>fmtD(addM(start,i)));
  document.getElementById('iPrev').innerHTML=`<strong>${fmt(per)}</strong>/mês  ${n} = ${fmt(total)}<br>${dates.slice(0,3).join(', ')}${n>3?` ... ${dates[n-1]}`:''}`;
}
async function saveTx(){
  if(!requireWriteAccess(editId?'editar transações':'criar transações'))return;
  let desc=document.getElementById('txDesc').value.trim();
  const amount=parseFloat(document.getElementById('txAmt').value);
  const category=document.getElementById('txCat').value||'A classificar';
  const entryDate=document.getElementById('txDt').value||today();
  const note=document.getElementById('txNote').value.trim();
  const type=getTyp();const accountId=document.getElementById('txAcc').value||S.accounts[0]?.id||null;
  const splitMeta=splitMetaFromForm();
  const entryDates=getTxEntryDates(accountId,entryDate,type);
  const date=entryDates.effectiveDate;
  const purchaseDate=entryDates.purchaseDate;
  const isInst=document.getElementById('instChk').checked;
  const isRec=document.getElementById('recChk').checked;
  if(!amount){toast('Informe o valor para salvar','error');return;}
  if(amount<=0){toast('Valor deve ser positivo','error');return;}
  if(!desc)desc=category||'Lançamento';
  const btn=document.getElementById('saveTxBtn');btn.disabled=true;btn.textContent='Salvando...';
  try{
    if(isInst&&!editId){
      const n=parseInt(document.getElementById('instN').value)||1;
      const st=document.getElementById('instSt').value||entryDate;
      if(n<2){toast('Mínimo 2 parcelas','error');return;}
      const per=Math.round(amount/n*100)/100,gid=uid();
      for(let i=0;i<n;i++){
        const d=addM(st,i);
        const instDates=getTxEntryDates(accountId,d,type);
        const tx={id:uid(),type,desc:`${desc} (${i+1}/${n})`,amount:per,category,date:instDates.effectiveDate,purchaseDate:instDates.purchaseDate,note,accountId,installmentGroup:gid,installmentNum:i+1,installmentTotal:n,recurGroup:null,splitMeta,paid:false,pending:false};
        if(cfg.mode==='api'){const r=await api('POST','/api/transactions',{type,description:tx.desc,amount:per,category,date:tx.date,purchase_date:tx.purchaseDate,note,account_id:accountId,paid:false,pending:false,installment_group:gid,installment_num:i+1,installment_total:n,split_meta:splitMeta});S.transactions.unshift(nTx({...r,accountId,split_meta:splitMeta,purchase_date:tx.purchaseDate}));}
        else S.transactions.unshift(tx);
      }
      if(cfg.mode==='local')saveLocal();toast(`${n} parcelas criadas! ✓`,'success');
    } else if(isRec&&!editId){
      const freq=document.getElementById('recFreq').value;
      const cnt=parseInt(document.getElementById('recN').value)||12;
      const gid=uid();const nD=(d,i)=>freq==='weekly'?addW(d,i):freq==='yearly'?addY(d,i):addM(d,i);
      for(let i=0;i<cnt;i++){
        const d=nD(entryDate,i);
        const recDates=getTxEntryDates(accountId,d,type);
        const tx={id:uid(),type,desc,amount,category,date:recDates.effectiveDate,purchaseDate:recDates.purchaseDate,note,accountId,installmentGroup:null,installmentNum:null,installmentTotal:null,recurGroup:gid,splitMeta,paid:false,pending:false};
        if(cfg.mode==='api'){const r=await api('POST','/api/transactions',{type,description:desc,amount,category,date:tx.date,purchase_date:tx.purchaseDate,note,account_id:accountId,paid:false,pending:false,recur_group:gid,split_meta:splitMeta});S.transactions.unshift(nTx({...r,accountId,split_meta:splitMeta,purchase_date:tx.purchaseDate}));}
        else S.transactions.unshift(tx);
      }
      if(cfg.mode==='local')saveLocal();toast(`${cnt} lançamentos criados! ✓`,'success');
    } else {
      const oldTx=editId?S.transactions.find(t=>t.id===editId):null;
      const pl={type,description:desc,amount,category,date,purchase_date:purchaseDate,note,account_id:accountId,paid:oldTx?.paid||false,pending:oldTx?.pending||false,split_meta:splitMeta};
      if(cfg.mode==='api'){
        if(editId){const u=await api('PUT',`/api/transactions/${editId}`,pl);const i=S.transactions.findIndex(t=>t.id===editId);S.transactions[i]=nTx({...u,accountId,split_meta:splitMeta,purchase_date:purchaseDate});}
        else{const c=await api('POST','/api/transactions',pl);S.transactions.unshift(nTx({...c,accountId,split_meta:splitMeta,purchase_date:purchaseDate}));}
      } else {
        const tx={id:editId||uid(),type,desc,amount,category,date,purchaseDate,note,accountId,installmentGroup:oldTx?.installmentGroup||null,installmentNum:oldTx?.installmentNum||null,installmentTotal:oldTx?.installmentTotal||null,recurGroup:oldTx?.recurGroup||null,splitMeta,paid:oldTx?.paid||false,pending:oldTx?.pending||false};
        if(editId){const i=S.transactions.findIndex(t=>t.id===editId);S.transactions[i]=tx;}else S.transactions.unshift(tx);
        saveLocal();
      }
      logAuditEvent(editId?'Lancamento atualizado':'Lancamento criado','Transacao',`${desc} • ${fmt(amount)}`,cfg.mode==='api'?'online':'local');
      toast(editId?'Atualizado! ✓':'Salvo! ✓','success');
    }
    closeM('txModal');refreshAll();setTimeout(scheduleVencimentoNotifications,500);
  }catch(e){toast('Erro: '+e.message,'error');}
  finally{btn.disabled=false;btn.textContent='Salvar';}
}
async function delTx(id){
  if(!requireWriteAccess('remover transações'))return;
  if(!confirm('Remover?'))return;
  const tx=S.transactions.find(t=>t.id===id);if(!tx)return;
  try{
    if(cfg.mode==='api')await api('DELETE',`/api/transactions/${id}`);
    S.transactions=S.transactions.filter(t=>t.id!==id);
    if(cfg.mode==='local')saveLocal();
    queueUndo(`Transação removida: ${tx.desc}`,async()=>{
      if(cfg.mode==='api'){
        const restored=await api('POST','/api/transactions',{type:tx.type,description:tx.desc,amount:tx.amount,category:tx.category,date:tx.date,purchase_date:tx.purchaseDate||tx.date,note:tx.note||'',account_id:tx.accountId,paid:tx.paid,pending:tx.pending,split_meta:splitMetaOf(tx)});
        S.transactions.unshift(nTx({...restored,accountId:tx.accountId,split_meta:splitMetaOf(tx),purchase_date:tx.purchaseDate||tx.date}));
      }else{S.transactions.unshift(tx);saveLocal();}
    });
    toast('Removida','error');refreshAll();
  }
  catch(e){toast('Erro: '+e.message,'error');}
}
async function delGrp(gid,field){
  if(!requireWriteAccess('remover grupos de transações'))return;
  if(!confirm('Remover TODOS do grupo?'))return;
  try{
    const toD=S.transactions.filter(t=>t[field]===gid);
    if(cfg.mode==='api')await Promise.all(toD.map(t=>api('DELETE',`/api/transactions/${t.id}`)));
    S.transactions=S.transactions.filter(t=>t[field]!==gid);
    if(cfg.mode==='local')saveLocal();
    queueUndo(`${toD.length} lançamentos removidos`,async()=>{
      if(cfg.mode==='api'){
        for(const tx of toD){
          const restored=await api('POST','/api/transactions',{type:tx.type,description:tx.desc,amount:tx.amount,category:tx.category,date:tx.date,purchase_date:tx.purchaseDate||tx.date,note:tx.note||'',account_id:tx.accountId,paid:tx.paid,pending:tx.pending,split_meta:splitMetaOf(tx)});
          S.transactions.unshift(nTx({...restored,accountId:tx.accountId,installmentGroup:tx.installmentGroup,installmentNum:tx.installmentNum,installmentTotal:tx.installmentTotal,recurGroup:tx.recurGroup,split_meta:splitMetaOf(tx),purchase_date:tx.purchaseDate||tx.date}));
        }
      }else{S.transactions.unshift(...toD);saveLocal();}
    });
    toast(`${toD.length} removidos`,'error');refreshAll();
  }catch(e){toast('Erro: '+e.message,'error');}
}
async function markPaid(id){
  if(!requireWriteAccess('alterar status de pagamento'))return;
  const tx=S.transactions.find(t=>t.id===id);if(!tx)return;
  const prev=tx.paid;tx.paid=!tx.paid;
  try{
    if(cfg.mode==='api')await api('PUT',`/api/transactions/${id}`,{type:tx.type,description:tx.desc,amount:tx.amount,category:tx.category,date:tx.date,purchase_date:tx.purchaseDate||tx.date,note:tx.note||'',account_id:tx.accountId,paid:tx.paid,pending:tx.pending,split_meta:splitMetaOf(tx)});
    else saveLocal();
  }catch(e){tx.paid=prev;toast('Erro: '+e.message,'error');return;}
  refreshAll();toast(tx.paid?'Marcado como pago OK':'Desmarcado','info');setTimeout(scheduleVencimentoNotifications,500);setTimeout(setupPersistentNotification,1000);
}
function hlTx(id){const el=document.getElementById('tx-'+id);if(el){el.scrollIntoView({behavior:'smooth',block:'center'});el.style.outline='2px solid var(--ac)';setTimeout(()=>el.style.outline='',2000);}}
function exportCSV(){
  // Exporta CSV com UTF-8 completo, emojis e caracteres especiais preservados
  const esc=v=>{
    const s=String(v==null?'':v);
    // Fora string limpa preservando emojis via normalize
    return '"'+s.normalize('NFC').replace(/"/g,'""')+'"';
  };
  const rows=[
    ['Data','Tipo','Descrição','Valor (R$)','Categoria','Conta','Parcela','Recorrente','Pago','Pendente','Observação']
  ];
  [...S.transactions]
    .sort((a,b)=>b.date.localeCompare(a.date))
    .forEach(t=>{
      const acc=S.accounts.find(x=>x.id===t.accountId);
      rows.push([
        t.date,
        t.type==='income'?'Receita':'Despesa',
        t.desc,
        Number(t.amount).toFixed(2).replace('.',','),
        t.category,
        acc?acc.name:'',
        t.installmentNum?`${t.installmentNum}/${t.installmentTotal}`:'',
        t.recurGroup?'Sim':'',
        t.paid?'Sim':'',
        t.pending?'Sim':'',
        t.note||''
      ]);
    });
  // Usa ; como separador (padro pt-BR Excel) e BOM UTF-8
  const sep=';';
  const csv=rows.map(r=>r.map(esc).join(sep)).join('\r\n');
  // TextEncoder garante UTF-8 correto com emojis
  const enc=new TextEncoder();
  const bom=new Uint8Array([0xEF,0xBB,0xBF]);
  const body=enc.encode(csv);
  const blob=new Blob([bom,body],{type:'text/csv;charset=utf-8'});
  const a=document.createElement('a');
  a.href=URL.createObjectURL(blob);
  a.download='finanza_'+today()+'.csv';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(()=>URL.revokeObjectURL(a.href),1000);
  toast('CSV exportado: '+(rows.length-1)+' lançamentos ✓','success');
}
function buildBackupData(){
  return {
    version:APP_VERSION,
    exported_at:new Date().toISOString(),
    app:'Finanza',
    user:cfg.userName||'local',
    stats:{
      transactions:S.transactions.length,
      budgets:S.budgets.length,
      goals:S.goals.length,
      accounts:S.accounts.length
    },
    transactions:S.transactions,
    budgets:S.budgets,
    goals:S.goals,
    accounts:S.accounts,
    categories:custCats,
    shopping:sl?.lists?.length?sl:(()=>{try{return JSON.parse(localStorage.getItem(SL_KEY)||'{}');}catch{return{lists:[],items:[]};}})(),
    car:carState?.vehicles?.length?carState:(()=>{try{return normalizeCarState(JSON.parse(localStorage.getItem(CAR_KEY)||'{}'));}catch{return normalizeCarState();}})(),
    dueItems,
    settings:getAppSettings()
  };
}
function exportJson(){
  const backup=buildBackupData();
  const json=JSON.stringify(backup,null,2);
  const enc=new TextEncoder();
  const bytes=enc.encode(json);
  const blob=new Blob([bytes],{type:'application/json;charset=utf-8'});
  const a=document.createElement('a');
  a.href=URL.createObjectURL(blob);
  a.download='finanza_backup_'+today()+'.json';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(()=>URL.revokeObjectURL(a.href),1000);
  logSyncEvent('success','Backup JSON exportado',`${backup.stats?.transactions||S.transactions.length} transações`);
  toast('Backup exportado: '+S.transactions.length+' transações OK','success');
}

function exportBackupFull(){
  exportJson();
  setTimeout(()=>exportCSV(),800);
}

async function migrateToOnline(){
  if(!requireWriteAccess('migrar dados para o modo online'))return;
  const url=prompt('URL do servidor:');if(!url)return;
  const key=prompt('Chave de acesso:');if(!key)return;
  try{
    const base=url.replace(/\/$/,'');
    const me=await fetch(base+'/api/me',{headers:{'x-api-key':key}});
    if(!me.ok)throw new Error('Chave inválida');const u=await me.json();
    toast('Migrando tudo...','info');
    const r=await fetch(base+'/api/import',{method:'PUT',headers:{'Content-Type':'application/json','x-api-key':key},body:JSON.stringify(buildBackupData())});
    if(!r.ok){const e=await r.json().catch(()=>({}));throw new Error(e.error||'Erro ao importar');}
    cfg={url:base,key,mode:'api',userName:u.name,userId:u.id,role:u.role||'',twoFactorEnabled:!!u.two_factor_enabled};persistCfg();
    logAuditEvent('Migracao concluida','Conta online',base,'online');
    toast('Dados migrados para online OK','success');await initApp();
  }catch(e){toast('Erro: '+e.message,'error');}
}
function getRange(p){
  const t=today();
  const m={'7d-p':{from:offD(new Date(),-7),to:t},'1m-p':{from:offD(new Date(),-30),to:t},'3m-p':{from:offD(new Date(),-90),to:t},'6m-p':{from:offD(new Date(),-180),to:t},'all-p':{from:'2000-01-01',to:t},'1m-f':{from:t,to:offD(new Date(),30)},'3m-f':{from:t,to:offD(new Date(),90)},'all-f':{from:t,to:'2099-12-31'},'all':{from:'2000-01-01',to:'2099-12-31'}};
  return m[p]||{from:'2000-01-01',to:'2099-12-31'};
}
document.querySelectorAll('[data-p]').forEach(b=>{b.addEventListener('click',()=>{document.querySelectorAll('[data-p]').forEach(x=>x.classList.remove('active'));b.classList.add('active');curTxP=b.dataset.p;renderTx();});});
document.querySelectorAll('[data-fp]').forEach(b=>{b.addEventListener('click',()=>{document.querySelectorAll('[data-fp]').forEach(x=>x.classList.remove('active'));b.classList.add('active');curFP=b.dataset.fp;renderFut();});});
function updM(){document.getElementById('curM').textContent=MO[curDt.getMonth()]+' '+curDt.getFullYear();}
function getMonthTx(d){return S.transactions.filter(t=>{const x=new Date(t.date+'T12:00:00');return x.getMonth()===d.getMonth()&&x.getFullYear()===d.getFullYear();});}
document.getElementById('prevM').onclick=()=>{curDt=new Date(curDt.getFullYear(),curDt.getMonth()-1,1);updM();renderDash();};
document.getElementById('nextM').onclick=()=>{curDt=new Date(curDt.getFullYear(),curDt.getMonth()+1,1);updM();renderDash();};
let pgHist=[];
function showPage(id){
  if(id==='admin'&&!canSeeAdminPanel()){
    toast('Painel admin disponível apenas para contas administradoras','error');
    id='settings';
  }
  const prev=document.querySelector('.page.active')?.id?.replace('page-','');
  document.querySelectorAll('.page').forEach(p=>p.classList.remove('active'));
  document.getElementById('page-'+id)?.classList.add('active');
  document.querySelectorAll('.nav-item,.fn-item,[data-page]').forEach(n=>n.classList.toggle('active',n.dataset.page===id));
  if(prev&&prev!==id)pgHist.push(prev);
  sessionStorage.setItem(PAGE_KEY,id);
  window.scrollTo({top:0,behavior:'auto'});
  if(id==='dashboard')renderDash(true);
  else if(id==='accounts'){renderAccs();popAccSels();}
  else if(id==='transactions')renderTx();
  else if(id==='shared')renderShared();
  else if(id==='commitments')renderCommitments();
  else if(id==='future')renderFut();
  else if(id==='budget')renderBuds();
  else if(id==='goals')renderGoals();
  else if(id==='shopping'){loadSL();renderShopping();}
  else if(id==='car'){loadCar();renderCar();}
  else if(id==='settings'||id==='admin')renderSet();
  applyWriteAccessUI();
}
document.querySelectorAll('.nav-item,.fn-item,[data-page]').forEach(n=>{n.onclick=()=>showPage(n.dataset.page);});
function refreshAll(){renderDash();const id=currentPageId();if(id&&id!=='dashboard')showPage(id);}
function renderWeekly(){
  const el=document.getElementById('wsum'),an=document.getElementById('anom');
  const ws=offD(new Date(),-7);
  const wTx=S.transactions.filter(t=>t.date>=ws&&t.date<=today()&&!isFut(t.date));
  const wI=wTx.filter(t=>t.type==='income').reduce((s,t)=>s+t.amount,0);
  const wE=wTx.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amount,0);
  if(wTx.length){
    const sv=wI-wE;
    el.innerHTML=`<div class="wcard"><div style="font-size:26px">📅</div><div><div style="font-family:var(--font-money);font-size:15px;font-weight:700;margin-bottom:3px">Esta semana</div><div style="font-size:11px;color:var(--mt)">Gastou <strong>${fmt(wE)}</strong>  Recebeu <strong>${fmt(wI)}</strong>${sv>0?`  <span style="color:var(--ac)">Economizou ${fmt(sv)}</span>`:sv<0?`  <span style="color:var(--dan)">Dficit de ${fmt(Math.abs(sv))}</span>`:''}</div></div></div>`;
  } else el.innerHTML=`<div class="wcard"><div style="font-size:26px">📅</div><div><div style="font-family:var(--font-money);font-size:15px;font-weight:700;margin-bottom:3px">Esta semana</div><div style="font-size:11px;color:var(--mt)">Sem lançamentos nesta semana. Quando você registrar gastos, este balão vira um resumo rápido.</div></div></div>`;
  // Anomaly
  const cats={};getMonthTx(curDt).filter(t=>t.type==='expense'&&!isFut(t.date)).forEach(t=>{cats[t.category]=(cats[t.category]||0)+t.amount;});
  const anoms=[];
  for(const[cat,spent] of Object.entries(cats)){
    let tot=0,cnt=0;
    for(let i=1;i<=3;i++){const d=new Date(curDt.getFullYear(),curDt.getMonth()-i,1);const mt=getMonthTx(d).filter(t=>t.type==='expense'&&t.category===cat&&!isFut(t.date)).reduce((s,t)=>s+t.amount,0);if(mt>0){tot+=mt;cnt++;}}
    if(cnt>0){const avg=tot/cnt;const pct=Math.round(((spent-avg)/avg)*100);if(pct>=40)anoms.push({cat,spent,avg,pct});}
  }
  if(anoms.length){const a=anoms.sort((a,b)=>b.pct-a.pct)[0];const c=getCat(a.cat);an.innerHTML=`<div class="anom">${c.ico} <div><strong>${a.cat}</strong> está <strong>${a.pct}% acima</strong> da média (${fmt(a.avg)}/mês). Este mês: ${fmt(a.spent)}</div></div>`;}
  else an.innerHTML=`<div class="anom">💡 <div><strong>Sem alerta fora da curva.</strong> Seus gastos por categoria estão dentro do padrão recente.</div></div>`;
}
// DASHBOARD
let flowChart,catChartObj;
function setChartMode(m){
  chartMode=m;
  renderDash();
}

function pendingSharedApprovalTxs(){
  return S.transactions
    .filter(tx=>splitMetaOf(tx).kind==='equal'&&splitNeedsApproval(tx))
    .sort((a,b)=>b.date.localeCompare(a.date));
}
async function setSharedApprovalStatus(txId,status){
  if(!requireWriteAccess('aprovar gastos compartilhados'))return;
  const tx=S.transactions.find(item=>item.id===txId);
  if(!tx)return;
  const splitMeta=splitMetaOf(tx);
  splitMeta.approval={
    ...asObj(splitMeta.approval),
    status,
    reviewedAt:Date.now(),
    reviewedBy:getSharedOwner().id
  };
  tx.splitMeta=splitMeta;
  if(cfg.mode==='api'){
    await api('PUT',`/api/transactions/${tx.id}`,{type:tx.type,description:tx.desc,amount:tx.amount,category:tx.category,date:tx.date,purchase_date:tx.purchaseDate||tx.date,note:tx.note||'',account_id:tx.accountId,paid:tx.paid,pending:tx.pending,split_meta:splitMeta});
  }else{
    saveLocal();
  }
  renderShared();
  renderTx();
  renderDash();
  toast(status==='approved'?'Gasto compartilhado aprovado':'Gasto compartilhado recusado',status==='approved'?'success':'info');
}
function renderShared(){
  sharedSpace=normalizeSharedSpace(sharedSpace);
  const people=sharedPeople();
  const balances=computeSharedBalances();
  const approvals=pendingSharedApprovalTxs();
  const values=Object.values(balances);
  const receivable=values.filter(v=>v>0).reduce((sum,v)=>sum+v,0);
  const payable=Math.abs(values.filter(v=>v<0).reduce((sum,v)=>sum+v,0));
  const owner=getSharedOwner();
  const modeEl=document.getElementById('sharedMode');if(modeEl)modeEl.value=sharedSpace.mode;
  const nameEl=document.getElementById('sharedName');if(nameEl)nameEl.value=sharedSpace.name||'';
  const ownerNameEl=document.getElementById('sharedOwnerName');if(ownerNameEl)ownerNameEl.value=owner.name;
  const peopleCount=document.getElementById('sharedPeopleCount');if(peopleCount)peopleCount.textContent=people.length;
  const modeLabel=document.getElementById('sharedModeLabel');if(modeLabel)modeLabel.textContent=sharedModeLabel(sharedSpace.mode);
  const recv=document.getElementById('sharedReceivable');if(recv)recv.textContent=fmt(receivable);
  const pay=document.getElementById('sharedPayable');if(pay)pay.textContent=fmt(payable);
  const invite=document.getElementById('sharedInviteMeta');if(invite)invite.textContent=sharedSpace.name?`Convite pronto para ${sharedSpace.name}`:'Crie um nome para o espaço e compartilhe o link';
  const approvalMeta=document.getElementById('sharedApprovalMeta');if(approvalMeta)approvalMeta.textContent=approvals.length?`${approvals.length} gasto(s) aguardando revisão`:'Nenhum gasto aguardando aprovação';
  const list=document.getElementById('sharedPeopleList');
  if(list)list.innerHTML=people.map(person=>`<div class="ss-r"><div><div class="ss-l"><span style="display:inline-flex;align-items:center;gap:8px"><span style="width:10px;height:10px;border-radius:999px;background:${person.color}"></span>${esc(person.name)}</span></div><div class="ss-s">${person.id===owner.id?'Você / dono do espaço':'Participa da divisão e dos acertos'}</div></div>${person.id===owner.id?'<span class="diag-pill">Você</span>':`<button class="btn btn-d btn-sm" data-write-only="gerenciar pessoas compartilhadas" onclick="removeSharedPerson('${person.id}')">Remover</button>`}</div>`).join('');
  const approvalList=document.getElementById('sharedApprovalList');
  if(approvalList)approvalList.innerHTML=approvals.length?approvals.map(tx=>{
    const meta=splitMetaOf(tx);
    const participants=asArr(meta.participants).map(sharedPersonName).join(', ');
    return `<div class="ss-r"><div><div class="ss-l">${esc(tx.desc)}</div><div class="ss-s">${sharedPersonName(meta.payerId)} pagou ${fmt(tx.amount)} • ${esc(participants)}${tx.note?` • ${esc(tx.note)}`:''}</div></div><div style="display:flex;align-items:center;gap:8px"><button class="btn btn-p btn-sm" data-write-only="aprovar gastos compartilhados" onclick="setSharedApprovalStatus('${tx.id}','approved')">Aprovar</button><button class="btn btn-d btn-sm" data-write-only="aprovar gastos compartilhados" onclick="setSharedApprovalStatus('${tx.id}','rejected')">Recusar</button></div></div>`;
  }).join(''):'<div class="sync-empty">Quando uma despesa compartilhada pedir revisão, ela aparece aqui.</div>';
  const balList=document.getElementById('sharedBalancesList');
  if(!balList)return;
  const others=people.filter(p=>p.id!==owner.id);
  if(!others.length){
    balList.innerHTML='<div class="empty"><span class="ei">🤝</span><p>Adicione pelo menos mais uma pessoa para começar a dividir despesas.</p></div>';
    applyWriteAccessUI();
    return;
  }
  balList.innerHTML=others.map(person=>{
    const balance=balances[person.id]||0;
    const tone=balance>0?'var(--ac)':balance<0?'var(--dan)':'var(--mt)';
    const label=balance>0?`${person.name} te deve`:balance<0?`Você deve para ${person.name}`:'Tudo acertado';
    const action=balance===0?'':`<button class="btn btn-g btn-sm" data-write-only="registrar acertos" onclick="openSettlementFromBalance('${person.id}')">${balance>0?'Registrar recebimento':'Registrar pagamento'}</button>`;
    return `<div class="ss-r"><div><div class="ss-l">${esc(person.name)}</div><div class="ss-s">${label}</div></div><div style="display:flex;align-items:center;gap:10px"><strong style="font-family:var(--font-money);color:${tone}">${fmt(Math.abs(balance))}</strong>${action}</div></div>`;
  }).join('');
  applyWriteAccessUI();
}
function commitmentStatusLabel(status){
  return ({active:'Ativo',paused:'Pausado',cancelled:'Cancelado',watch:'Acompanhar',closed:'Quitada',ended:'Encerrado'}[status]||status||'');
}
function commitmentUsageLabel(usage){
  return ({high:'muito usada',medium:'uso normal',low:'quase esquecida'}[usage]||'uso normal');
}
function monthlySubscriptionDueEntries(){
  const linkedIds=new Set(commitmentsState.subscriptions.map(item=>item.linkedDueId).filter(Boolean));
  return dueItems
    .filter(item=>item.active&&item.recurrence==='monthly'&&normalizeTxText(item.category).includes('assin')&&!linkedIds.has(item.id))
    .map(item=>({
      ...nSubscription({
        id:`due-${item.id}`,
        name:item.name,
        amount:item.amount,
        category:item.category,
        billingDay:item.dueDay,
        renewalDate:item.nextDueDate,
        paymentMethod:item.paymentMethod,
        paymentPlace:item.paymentPlace,
        accountId:item.accountId,
        notes:item.notes,
        linkedDueId:item.id
      }),
      source:'due'
    }));
}
function subscriptionCenterItems(){
  return [
    ...commitmentsState.subscriptions.map(item=>({...nSubscription(item),source:'manual'})),
    ...monthlySubscriptionDueEntries()
  ].sort((a,b)=>{
    if(a.status!==b.status)return a.status==='active'?-1:1;
    return (a.renewalDate||'').localeCompare(b.renewalDate||'');
  });
}
function contractCenterItems(){
  return commitmentsState.contracts.map(item=>nContract(item)).sort((a,b)=>(a.renewalDate||'').localeCompare(b.renewalDate||''));
}
function debtCenterItems(){
  return commitmentsState.debts.map(item=>nDebt(item)).sort((a,b)=>b.outstandingAmount-a.outstandingAmount);
}
function commitmentLinkedDue(id){
  return dueItems.find(item=>item.id===id)||null;
}
function daysUntilDate(date){
  if(!date)return null;
  return Math.ceil((new Date(date+'T12:00:00')-new Date(today()+'T12:00:00'))/864e5);
}
function ensureSubscriptionDue(subscriptionId){
  const item=commitmentsState.subscriptions.find(entry=>entry.id===subscriptionId);
  if(!item)return;
  const linked=commitmentLinkedDue(item.linkedDueId);
  if(linked){openDueModal(linked.id);return;}
  const date=item.renewalDate||today();
  const due=nDue({
    id:uid(),
    name:item.name,
    amount:item.amount,
    category:item.category,
    recurrence:'monthly',
    nextDueDate:date,
    dueDay:item.billingDay||new Date(date+'T12:00:00').getDate(),
    paymentMethod:item.paymentMethod,
    paymentPlace:item.paymentPlace,
    accountId:item.accountId,
    notes:item.notes||'Assinatura mensal'
  });
  dueItems.push(due);
  item.linkedDueId=due.id;
  saveDueItems();
  saveCommitments();
  renderCommitments();
  toast('Assinatura vinculada aos vencimentos','success');
}
function ensureDebtDue(debtId){
  const item=commitmentsState.debts.find(entry=>entry.id===debtId);
  if(!item)return;
  const linked=commitmentLinkedDue(item.linkedDueId);
  if(linked){openDueModal(linked.id);return;}
  const due=nDue({
    id:uid(),
    name:item.name,
    amount:item.installmentAmount||item.outstandingAmount,
    category:'Outros',
    recurrence:item.remainingInstallments>1?'monthly':'once',
    nextDueDate:item.nextDueDate||today(),
    dueDay:new Date((item.nextDueDate||today())+'T12:00:00').getDate(),
    paymentMethod:'financing',
    paymentPlace:'Contrato / financiamento',
    accountId:item.accountId,
    notes:item.notes||'Parcela de divida'
  });
  dueItems.push(due);
  item.linkedDueId=due.id;
  saveDueItems();
  saveCommitments();
  renderCommitments();
  toast('Divida vinculada aos vencimentos','success');
}
function ensureContractDue(contractId){
  const item=commitmentsState.contracts.find(entry=>entry.id===contractId);
  if(!item)return;
  const linked=commitmentLinkedDue(item.linkedDueId);
  if(linked){openDueModal(linked.id);return;}
  const due=nDue({
    id:uid(),
    name:item.name,
    amount:item.monthlyAmount,
    category:'Moradia',
    recurrence:'monthly',
    nextDueDate:item.renewalDate||today(),
    dueDay:new Date((item.renewalDate||today())+'T12:00:00').getDate(),
    paymentMethod:'boleto',
    paymentPlace:item.provider,
    accountId:item.accountId,
    notes:item.notes||`Contrato • ${item.kind}`
  });
  dueItems.push(due);
  item.linkedDueId=due.id;
  saveDueItems();
  saveCommitments();
  renderCommitments();
  toast('Contrato vinculado aos vencimentos','success');
}
function removeCommitmentLinkFromDeletedDue(id){
  let changed=false;
  commitmentsState.subscriptions.forEach(item=>{if(item.linkedDueId===id){item.linkedDueId='';changed=true;}});
  commitmentsState.debts.forEach(item=>{if(item.linkedDueId===id){item.linkedDueId='';changed=true;}});
  commitmentsState.contracts.forEach(item=>{if(item.linkedDueId===id){item.linkedDueId='';changed=true;}});
  if(changed)saveCommitments(false);
}
function detectSubscriptionSuggestions(){
  const manualNames=new Set(subscriptionCenterItems().map(item=>normalizeTxText(item.name)));
  return detectSubscriptions(recentMonthStats(6))
    .filter(item=>!manualNames.has(normalizeTxText(item.desc)))
    .map(item=>({
      name:item.desc,
      amount:Number(item.avg.toFixed(2)),
      category:item.category||'Assinaturas',
      billingDay:Number(String(item.last||today()).slice(8,10))||1,
      renewalDate:addM(item.last||today(),1),
      paymentMethod:'credit',
      paymentPlace:'',
      accountId:inferTxAccount(item.desc,item.desc,item.category),
      usage:'medium',
      notes:`Sugestao criada a partir de ${item.count} meses semelhantes`
    }))
    .slice(0,5);
}
function openSubscriptionModal(id=null,preset=null){
  if(!requireWriteAccess(id?'editar assinaturas':'criar assinaturas'))return;
  subscriptionEditId=id;
  const item=id?commitmentsState.subscriptions.find(entry=>entry.id===id):null;
  const sub=nSubscription(item||preset||{renewalDate:addM(today(),1),billingDay:new Date(addM(today(),1)+'T12:00:00').getDate(),paymentMethod:'credit'});
  document.getElementById('subId').value=id||'';
  document.getElementById('subName').value=sub.name==='Assinatura'&&!id&&preset===null?'':sub.name;
  document.getElementById('subAmount').value=sub.amount||'';
  document.getElementById('subCategory').value=sub.category||'Assinaturas';
  document.getElementById('subDay').value=sub.billingDay||'';
  document.getElementById('subRenewal').value=sub.renewalDate||addM(today(),1);
  document.getElementById('subMethod').value=sub.paymentMethod||'credit';
  document.getElementById('subPlace').value=sub.paymentPlace||'';
  document.getElementById('subUsage').value=sub.usage||'medium';
  document.getElementById('subStatus').value=sub.status||'active';
  document.getElementById('subNotes').value=sub.notes||'';
  document.getElementById('subAcc').innerHTML='<option value="">Sem conta/cartão</option>'+S.accounts.map(a=>`<option value="${a.id}">${a.icon} ${a.name}</option>`).join('');
  document.getElementById('subAcc').value=sub.accountId||'';
  document.getElementById('subscriptionModal').classList.add('open');
}
function saveSubscription(){
  if(!requireWriteAccess(document.getElementById('subId').value?'editar assinaturas':'criar assinaturas'))return;
  const existing=commitmentsState.subscriptions.find(entry=>entry.id===document.getElementById('subId').value);
  const item=nSubscription({
    id:document.getElementById('subId').value||uid(),
    name:document.getElementById('subName').value.trim(),
    amount:parseFloat(document.getElementById('subAmount').value)||0,
    category:document.getElementById('subCategory').value||'Assinaturas',
    billingDay:parseInt(document.getElementById('subDay').value,10)||new Date((document.getElementById('subRenewal').value||today())+'T12:00:00').getDate(),
    renewalDate:document.getElementById('subRenewal').value||today(),
    paymentMethod:document.getElementById('subMethod').value,
    paymentPlace:document.getElementById('subPlace').value.trim(),
    accountId:document.getElementById('subAcc').value,
    usage:document.getElementById('subUsage').value,
    status:document.getElementById('subStatus').value,
    notes:document.getElementById('subNotes').value.trim(),
    linkedDueId:existing?.linkedDueId||''
  });
  if(!item.name||!item.amount){toast('Informe nome e valor da assinatura','error');return;}
  const idx=commitmentsState.subscriptions.findIndex(entry=>entry.id===item.id);
  if(idx>=0)commitmentsState.subscriptions[idx]=item;else commitmentsState.subscriptions.unshift(item);
  saveCommitments();
  closeM('subscriptionModal');
  renderCommitments();
  toast('Assinatura salva','success');
}
function deleteSubscription(id){
  if(!requireWriteAccess('remover assinaturas'))return;
  if(!confirm('Remover esta assinatura da central?'))return;
  commitmentsState.subscriptions=commitmentsState.subscriptions.filter(entry=>entry.id!==id);
  saveCommitments();
  renderCommitments();
  toast('Assinatura removida','info');
}
function openDebtModal(id=null){
  if(!requireWriteAccess(id?'editar dividas':'criar dividas'))return;
  debtEditId=id;
  const item=nDebt(id?commitmentsState.debts.find(entry=>entry.id===id):{nextDueDate:addM(today(),1)});
  document.getElementById('debtId').value=id||'';
  document.getElementById('debtName').value=id?item.name:'';
  document.getElementById('debtTotal').value=item.totalAmount||'';
  document.getElementById('debtOutstanding').value=item.outstandingAmount||'';
  document.getElementById('debtInstallment').value=item.installmentAmount||'';
  document.getElementById('debtTotalInstallments').value=item.totalInstallments||1;
  document.getElementById('debtRemainingInstallments').value=item.remainingInstallments||1;
  document.getElementById('debtInterest').value=item.interestRate||'';
  document.getElementById('debtNextDate').value=item.nextDueDate||addM(today(),1);
  document.getElementById('debtStrategy').value=item.strategy||'custom';
  document.getElementById('debtStatus').value=item.status||'active';
  document.getElementById('debtNotes').value=item.notes||'';
  document.getElementById('debtAcc').innerHTML='<option value="">Sem conta/cartão</option>'+S.accounts.map(a=>`<option value="${a.id}">${a.icon} ${a.name}</option>`).join('');
  document.getElementById('debtAcc').value=item.accountId||'';
  document.getElementById('debtModal').classList.add('open');
}
function saveDebt(){
  if(!requireWriteAccess(document.getElementById('debtId').value?'editar dividas':'criar dividas'))return;
  const existing=commitmentsState.debts.find(entry=>entry.id===document.getElementById('debtId').value);
  const item=nDebt({
    id:document.getElementById('debtId').value||uid(),
    name:document.getElementById('debtName').value.trim(),
    totalAmount:parseFloat(document.getElementById('debtTotal').value)||0,
    outstandingAmount:parseFloat(document.getElementById('debtOutstanding').value)||0,
    installmentAmount:parseFloat(document.getElementById('debtInstallment').value)||0,
    totalInstallments:parseInt(document.getElementById('debtTotalInstallments').value,10)||1,
    remainingInstallments:parseInt(document.getElementById('debtRemainingInstallments').value,10)||1,
    interestRate:parseFloat(document.getElementById('debtInterest').value)||0,
    nextDueDate:document.getElementById('debtNextDate').value||today(),
    accountId:document.getElementById('debtAcc').value,
    strategy:document.getElementById('debtStrategy').value,
    status:document.getElementById('debtStatus').value,
    notes:document.getElementById('debtNotes').value.trim(),
    linkedDueId:existing?.linkedDueId||''
  });
  if(!item.name||!item.totalAmount){toast('Informe nome e valor total da divida','error');return;}
  const idx=commitmentsState.debts.findIndex(entry=>entry.id===item.id);
  if(idx>=0)commitmentsState.debts[idx]=item;else commitmentsState.debts.unshift(item);
  saveCommitments();
  closeM('debtModal');
  renderCommitments();
  toast('Divida salva','success');
}
async function registerDebtPayment(id){
  if(!requireWriteAccess('registrar parcelas de dividas'))return;
  const item=commitmentsState.debts.find(entry=>entry.id===id);
  if(!item)return;
  const amount=item.installmentAmount||Math.min(item.outstandingAmount,item.totalAmount);
  const date=item.nextDueDate||today();
  const note=`Parcela de divida • ${item.remainingInstallments}/${item.totalInstallments}`;
  const tx={id:uid(),type:'expense',desc:item.name,amount,category:'Outros',date,note,accountId:item.accountId||S.accounts[0]?.id||null,installmentGroup:null,installmentNum:null,installmentTotal:null,recurGroup:null,paid:true,pending:false};
  try{
    if(cfg.mode==='api'){
      const saved=await api('POST','/api/transactions',{type:'expense',description:tx.desc,amount:tx.amount,category:tx.category,date,note,account_id:tx.accountId,paid:true,pending:false});
      S.transactions.unshift(nTx({...saved,accountId:tx.accountId}));
    }else{
      S.transactions.unshift(tx);
      saveLocal();
    }
    item.outstandingAmount=Math.max(0,Number((item.outstandingAmount-amount).toFixed(2)));
    item.remainingInstallments=Math.max(0,item.remainingInstallments-1);
    item.nextDueDate=item.remainingInstallments>0?addM(date,1):date;
    if(item.outstandingAmount<=0||item.remainingInstallments===0)item.status='closed';
    if(item.linkedDueId){
      const linked=commitmentLinkedDue(item.linkedDueId);
      if(linked){
        if(item.status==='closed')linked.active=false;
        else{
          linked.amount=item.installmentAmount||linked.amount;
          linked.nextDueDate=item.nextDueDate;
          linked.dueDay=new Date(item.nextDueDate+'T12:00:00').getDate();
        }
        saveDueItems();
      }
    }
    saveCommitments();
    refreshAll();
    toast('Parcela registrada','success');
  }catch(e){toast('Erro ao registrar parcela: '+e.message,'error');}
}
function deleteDebt(id){
  if(!requireWriteAccess('remover dividas'))return;
  if(!confirm('Remover esta divida da central?'))return;
  commitmentsState.debts=commitmentsState.debts.filter(entry=>entry.id!==id);
  saveCommitments();
  renderCommitments();
  toast('Divida removida','info');
}
function openContractModal(id=null){
  if(!requireWriteAccess(id?'editar contratos':'criar contratos'))return;
  contractEditId=id;
  const item=nContract(id?commitmentsState.contracts.find(entry=>entry.id===id):{renewalDate:addM(today(),1)});
  document.getElementById('contractId').value=id||'';
  document.getElementById('contractName').value=id?item.name:'';
  document.getElementById('contractKind').value=item.kind||'service';
  document.getElementById('contractAmount').value=item.monthlyAmount||'';
  document.getElementById('contractProvider').value=item.provider||'';
  document.getElementById('contractRenewal').value=item.renewalDate||addM(today(),1);
  document.getElementById('contractAdjustment').value=item.adjustmentDate||'';
  document.getElementById('contractStatus').value=item.status||'active';
  document.getElementById('contractNotes').value=item.notes||'';
  document.getElementById('contractAcc').innerHTML='<option value="">Sem conta/cartão</option>'+S.accounts.map(a=>`<option value="${a.id}">${a.icon} ${a.name}</option>`).join('');
  document.getElementById('contractAcc').value=item.accountId||'';
  document.getElementById('contractModal').classList.add('open');
}
function saveContract(){
  if(!requireWriteAccess(document.getElementById('contractId').value?'editar contratos':'criar contratos'))return;
  const existing=commitmentsState.contracts.find(entry=>entry.id===document.getElementById('contractId').value);
  const item=nContract({
    id:document.getElementById('contractId').value||uid(),
    name:document.getElementById('contractName').value.trim(),
    kind:document.getElementById('contractKind').value,
    monthlyAmount:parseFloat(document.getElementById('contractAmount').value)||0,
    provider:document.getElementById('contractProvider').value.trim(),
    renewalDate:document.getElementById('contractRenewal').value||today(),
    adjustmentDate:document.getElementById('contractAdjustment').value||'',
    accountId:document.getElementById('contractAcc').value,
    status:document.getElementById('contractStatus').value,
    notes:document.getElementById('contractNotes').value.trim(),
    linkedDueId:existing?.linkedDueId||''
  });
  if(!item.name){toast('Informe o nome do contrato','error');return;}
  const idx=commitmentsState.contracts.findIndex(entry=>entry.id===item.id);
  if(idx>=0)commitmentsState.contracts[idx]=item;else commitmentsState.contracts.unshift(item);
  saveCommitments();
  closeM('contractModal');
  renderCommitments();
  toast('Contrato salvo','success');
}
function deleteContract(id){
  if(!requireWriteAccess('remover contratos'))return;
  if(!confirm('Remover este contrato da central?'))return;
  commitmentsState.contracts=commitmentsState.contracts.filter(entry=>entry.id!==id);
  saveCommitments();
  renderCommitments();
  toast('Contrato removido','info');
}
function moneyCommittedSummary(){
  const subscriptions=subscriptionCenterItems().filter(item=>item.status==='active').reduce((sum,item)=>sum+item.amount,0);
  const debts=debtCenterItems().filter(item=>item.status==='active').reduce((sum,item)=>sum+(item.installmentAmount||0),0);
  const contracts=contractCenterItems().filter(item=>item.status!=='ended').reduce((sum,item)=>sum+(item.monthlyAmount||0),0);
  return {subscriptions,debts,contracts,total:subscriptions+debts+contracts};
}
function debtPayoffPlans(){
  const active=debtCenterItems().filter(item=>item.status==='active'&&item.outstandingAmount>0);
  return {
    snowball:[...active].sort((a,b)=>a.outstandingAmount-b.outstandingAmount),
    avalanche:[...active].sort((a,b)=>b.interestRate-a.interestRate)
  };
}
function renderCommitments(){
  const subs=subscriptionCenterItems();
  const debts=debtCenterItems();
  const contracts=contractCenterItems();
  const summary=moneyCommittedSummary();
  const renewals=[
    ...subs.filter(item=>item.status==='active').map(item=>({name:item.name,date:item.renewalDate,type:'Assinatura'})),
    ...contracts.filter(item=>item.status!=='ended').map(item=>({name:item.name,date:item.adjustmentDate||item.renewalDate,type:item.adjustmentDate?'Reajuste':'Contrato'}))
  ].filter(item=>item.date&&daysUntilDate(item.date)!==null&&daysUntilDate(item.date)<=30).sort((a,b)=>a.date.localeCompare(b.date));
  const overdueDebts=debts.filter(item=>item.status==='active'&&item.nextDueDate<today());
  const subTotal=document.getElementById('commitSubTotal');if(subTotal)subTotal.textContent=fmt(summary.subscriptions);
  const debtTotal=document.getElementById('commitDebtTotal');if(debtTotal)debtTotal.textContent=fmt(debts.reduce((sum,item)=>sum+item.outstandingAmount,0));
  const renewCount=document.getElementById('commitRenewCount');if(renewCount)renewCount.textContent=String(renewals.length);
  const committed=document.getElementById('commitCommittedMonthly');if(committed)committed.textContent=fmt(summary.total);
  const debtAlert=document.getElementById('commitDebtAlert');if(debtAlert)debtAlert.textContent=overdueDebts.length?`${overdueDebts.length} divida(s) atrasadas`:'parcelas em dia';
  const renewMeta=document.getElementById('commitRenewMeta');if(renewMeta)renewMeta.textContent=renewals[0]?`${renewals[0].type} • ${fmtD(renewals[0].date)}`:'nada vence em 30 dias';
  const center=document.getElementById('commitmentCenter');
  if(center){
    const plans=debtPayoffPlans();
    center.innerHTML=`
      <div class="future-center-grid">
        <div class="future-center-card">
          <div class="ct">Dinheiro comprometido</div>
          <div class="cs">O que ja sai do mes antes das escolhas do dia a dia</div>
          <div class="future-center-list">
            <div class="future-center-row"><div><div class="future-center-name">Assinaturas</div><div class="future-center-meta">${subs.filter(item=>item.status==='active').length} ativas</div></div><div class="future-center-amt">${fmt(summary.subscriptions)}</div></div>
            <div class="future-center-row"><div><div class="future-center-name">Parcelas de dividas</div><div class="future-center-meta">${debts.filter(item=>item.status==='active').length} acompanhadas</div></div><div class="future-center-amt">${fmt(summary.debts)}</div></div>
            <div class="future-center-row"><div><div class="future-center-name">Contratos</div><div class="future-center-meta">${contracts.filter(item=>item.status!=='ended').length} em vigor</div></div><div class="future-center-amt">${fmt(summary.contracts)}</div></div>
          </div>
        </div>
        <div class="future-center-card">
          <div class="ct">Plano de quitacao</div>
          <div class="cs">Bola de neve e avalanche para atacar o saldo com menos improviso</div>
          <div class="future-center-list">
            ${plans.snowball.length?`<div class="future-center-row"><div><div class="future-center-name">Bola de neve</div><div class="future-center-meta">Comece por ${esc(plans.snowball[0].name)}</div></div><div class="future-center-amt">${fmt(plans.snowball[0].outstandingAmount)}</div></div>`:'<div class="sync-empty">Sem dividas ativas para ordenar.</div>'}
            ${plans.avalanche.length?`<div class="future-center-row"><div><div class="future-center-name">Avalanche</div><div class="future-center-meta">Maior juros em ${esc(plans.avalanche[0].name)}</div></div><div class="future-center-amt">${plans.avalanche[0].interestRate.toFixed(2)}% a.m.</div></div>`:''}
          </div>
        </div>
      </div>`;
  }
  const suggestions=detectSubscriptionSuggestions();
  const subList=document.getElementById('subscriptionList');
  if(subList)subList.innerHTML=subs.length?subs.map(item=>{
    const renewDays=daysUntilDate(item.renewalDate);
    const tone=item.status==='cancelled'?'var(--mt)':renewDays!==null&&renewDays<=3?'var(--dan)':renewDays!==null&&renewDays<=10?'var(--warn)':'var(--ac2)';
    const linked=commitmentLinkedDue(item.linkedDueId);
    const actions=item.source==='due'
      ? `<button class="btn btn-g btn-sm" onclick="openDueModal('${item.linkedDueId}')">Abrir vencimento</button>`
      : `<button class="btn btn-g btn-sm" onclick="${linked?`openDueModal('${linked.id}')`:`ensureSubscriptionDue('${item.id}')`}">${linked?'Editar vencimento':'Gerar vencimento'}</button><button class="btn btn-g btn-sm" onclick="openSubscriptionModal('${item.id}')">Editar</button><button class="btn btn-d btn-sm" onclick="deleteSubscription('${item.id}')">Remover</button>`;
    return `<div class="commit-row"><div><div class="commit-title">${esc(item.name)}</div><div class="commit-meta"><span class="bdg">${esc(item.category)}</span><span class="bdg">${commitmentStatusLabel(item.status)}</span><span class="bdg">${commitmentUsageLabel(item.usage)}</span>${item.paymentPlace?`<span class="bdg">${esc(item.paymentPlace)}</span>`:''}${item.notes?`<span class="commit-note">${esc(item.notes)}</span>`:''}</div></div><div class="commit-side"><strong style="color:${tone}">${fmt(item.amount)}</strong><small>${renewDays===null?'sem renovacao':renewDays<0?`renovou ha ${Math.abs(renewDays)}d`:`renova em ${renewDays}d`}</small><div class="commit-actions">${actions}</div></div></div>`;
  }).join(''):'<div class="sync-empty">Nenhuma assinatura salva ainda.</div>';
  const suggestionList=document.getElementById('subscriptionSuggestionList');
  if(suggestionList)suggestionList.innerHTML=suggestions.length?suggestions.map((item,idx)=>`<div class="ss-r"><div><div class="ss-l">${esc(item.name)}</div><div class="ss-s">${fmt(item.amount)} • categoria ${esc(item.category)} • dia ${item.billingDay}</div></div><button class="btn btn-g btn-sm" data-write-only="criar assinaturas" onclick="openSubscriptionModal(null,window.__subscriptionSuggestions[${idx}])">Salvar na central</button></div>`).join(''):'<div class="sync-empty">O detector automatico ainda nao encontrou recorrencias novas relevantes.</div>';
  window.__subscriptionSuggestions=suggestions;
  const debtList=document.getElementById('debtList');
  if(debtList)debtList.innerHTML=debts.length?debts.map(item=>{
    const nextDays=daysUntilDate(item.nextDueDate);
    const linked=commitmentLinkedDue(item.linkedDueId);
    return `<div class="commit-row"><div><div class="commit-title">${esc(item.name)}</div><div class="commit-meta"><span class="bdg">${commitmentStatusLabel(item.status)}</span><span class="bdg">${item.remainingInstallments}/${item.totalInstallments} parcelas</span><span class="bdg">${item.interestRate.toFixed(2)}% a.m.</span>${item.notes?`<span class="commit-note">${esc(item.notes)}</span>`:''}</div></div><div class="commit-side"><strong>${fmt(item.outstandingAmount)}</strong><small>${nextDays<0?`atrasada ha ${Math.abs(nextDays)}d`:`proxima em ${nextDays}d`}</small><div class="commit-actions"><button class="btn btn-p btn-sm" data-write-only="registrar parcelas de dividas" onclick="registerDebtPayment('${item.id}')">Registrar parcela</button><button class="btn btn-g btn-sm" onclick="${linked?`openDueModal('${linked.id}')`:`ensureDebtDue('${item.id}')`}">${linked?'Abrir vencimento':'Gerar vencimento'}</button><button class="btn btn-g btn-sm" onclick="openDebtModal('${item.id}')">Editar</button><button class="btn btn-d btn-sm" onclick="deleteDebt('${item.id}')">Remover</button></div></div></div>`;
  }).join(''):'<div class="sync-empty">Nenhuma divida cadastrada ainda.</div>';
  const planList=document.getElementById('debtPlanList');
  if(planList){
    const snowball=debtPayoffPlans().snowball;
    planList.innerHTML=snowball.length?snowball.map((item,idx)=>`<div class="ss-r"><div><div class="ss-l">${idx+1}. ${esc(item.name)}</div><div class="ss-s">${fmt(item.outstandingAmount)} restantes • parcela ${fmt(item.installmentAmount||0)}</div></div><span class="diag-pill">${item.strategy==='avalanche'?'Avalanche':item.strategy==='snowball'?'Neve':'Livre'}</span></div>`).join(''):'<div class="sync-empty">Quando voce cadastrar dividas, a ordem sugerida aparece aqui.</div>';
  }
  const contractList=document.getElementById('contractList');
  if(contractList)contractList.innerHTML=contracts.length?contracts.map(item=>{
    const alertDate=item.adjustmentDate||item.renewalDate;
    const days=daysUntilDate(alertDate);
    const linked=commitmentLinkedDue(item.linkedDueId);
    return `<div class="commit-row"><div><div class="commit-title">${esc(item.name)}</div><div class="commit-meta"><span class="bdg">${esc(item.kind)}</span><span class="bdg">${commitmentStatusLabel(item.status)}</span>${item.provider?`<span class="bdg">${esc(item.provider)}</span>`:''}${item.notes?`<span class="commit-note">${esc(item.notes)}</span>`:''}</div></div><div class="commit-side"><strong>${item.monthlyAmount?fmt(item.monthlyAmount):'—'}</strong><small>${days===null?'sem alerta':days<0?`venceu ha ${Math.abs(days)}d`:`alerta em ${days}d`}</small><div class="commit-actions"><button class="btn btn-g btn-sm" onclick="${linked?`openDueModal('${linked.id}')`:`ensureContractDue('${item.id}')`}">${linked?'Abrir vencimento':'Gerar vencimento'}</button><button class="btn btn-g btn-sm" onclick="openContractModal('${item.id}')">Editar</button><button class="btn btn-d btn-sm" onclick="deleteContract('${item.id}')">Remover</button></div></div></div>`;
  }).join(''):'<div class="sync-empty">Nenhum contrato cadastrado ainda.</div>';
  applyWriteAccessUI();
}
function debounce(fn,wait=160){let t;return(...args)=>{clearTimeout(t);t=setTimeout(()=>fn(...args),wait);};}
// BUDGETS
function openBudModal(){if(!requireWriteAccess('criar orçamentos'))return;popCatSels();document.getElementById('budLim').value='';document.getElementById('budModal').classList.add('open');}
async function saveBud(){
  if(!requireWriteAccess('salvar orçamentos'))return;
  const cat=document.getElementById('budCat').value;const lim=parseFloat(document.getElementById('budLim').value);
  if(!lim||lim<=0){toast('Valor invlido','error');return;}
  try{
    if(cfg.mode==='api'){const r=await api('POST','/api/budgets',{category:cat,limit:lim});const i=S.budgets.findIndex(b=>b.category===cat);if(i>=0)S.budgets[i]=nBud(r);else S.budgets.push(nBud(r));}
    else{const i=S.budgets.findIndex(b=>b.category===cat);if(i>=0)S.budgets[i].limit=lim;else S.budgets.push({id:uid(),category:cat,limit:lim});saveLocal();}
    logAuditEvent('Orcamento salvo','Orcamento',`${cat} • ${fmt(lim)}`,cfg.mode==='api'?'online':'local');
    closeM('budModal');renderBuds();toast('Oramento salvo! ✓','success');
  }catch(e){toast('Erro: '+e.message,'error');}
}
async function delBud(id){
  if(!requireWriteAccess('remover orçamentos'))return;
  try{if(cfg.mode==='api')await api('DELETE',`/api/budgets/${id}`);S.budgets=S.budgets.filter(b=>b.id!==id);if(cfg.mode==='local')saveLocal();renderBuds();}
  catch(e){toast('Erro: '+e.message,'error');}
}
function renderBuds(){
  const now=new Date();let tL=0,tS=0;
  const cards=S.budgets.map(b=>{
    const spent=S.budgets&&S.transactions.filter(t=>t.type==='expense'&&t.category===b.category&&!isFut(t.date)&&!t.paid&&(()=>{const d=new Date(t.date+'T12:00:00');return d.getMonth()===now.getMonth()&&d.getFullYear()===now.getFullYear();})()).reduce((s,t)=>s+t.amount,0);
    const pct=Math.min((spent/b.limit)*100,100);const col=pct>=100?'var(--dan)':pct>=80?'var(--warn)':'var(--ac)';const rem=b.limit-spent;tL+=b.limit;tS+=spent;const cat=getCat(b.category);
    return`<div class="bg-card"><div style="display:flex;justify-content:space-between;margin-bottom:9px"><div><div style="font-size:18px">${cat.ico}</div><div style="font-size:11px;font-weight:600;margin-top:2px">${b.category}</div></div><button class="ib del" ${writeActionAttrs('remover orçamentos')} onclick="delBud('${b.id}')">🗑️</button></div><div style="display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:3px"><div style="font-family:var(--font-money);font-size:18px;font-weight:700;color:${col}">${fmt(spent)}</div><div style="font-size:10px;color:var(--mt)">de ${fmt(b.limit)}</div></div><div class="prg"><div class="pf" style="width:${pct}%;background:${col}"></div></div><div style="font-size:12px;color:${rem<0?'var(--dan)':'var(--mt)'};margin-top:5px">${rem>=0?`Restam ${fmt(rem)}`:`Excedido em ${fmt(Math.abs(rem))}`}</div></div>`;
  });
  document.getElementById('budTot').textContent=fmt(tL);document.getElementById('budSpent').textContent=fmt(tS);document.getElementById('budAvail').textContent=fmt(tL-tS);
  document.getElementById('budGrid').innerHTML=cards.length?cards.join(''):`<div class="empty" style="grid-column:1/-1"><span class="ei">🎯</span><p>Nenhum orçamento. Crie um!</p></div>`;
}
// GOALS
function openGoalModal(){if(!requireWriteAccess('criar metas'))return;['gNm','gIco','gDesc','gMon','gTgt','gCur'].forEach(id=>document.getElementById(id).value='');document.getElementById('goalModal').classList.add('open');}
async function saveGoal(){
  if(!requireWriteAccess('salvar metas'))return;
  const name=document.getElementById('gNm').value.trim();const icon=document.getElementById('gIco').value.trim()||'🎯';
  const target=parseFloat(document.getElementById('gTgt').value);const current=parseFloat(document.getElementById('gCur').value)||0;
  const deadline=document.getElementById('gDl').value;const desc=document.getElementById('gDesc').value.trim();const monthly=parseFloat(document.getElementById('gMon').value)||0;
  if(!name||!target||!deadline){toast('Preencha os campos obrigatrios','error');return;}
  try{
    if(cfg.mode==='api'){const r=await api('POST','/api/goals',{name,icon,target,current,deadline,description:desc,monthly});S.goals.push(nGoal(r));}
    else{S.goals.push({id:uid(),name,icon,target,current,deadline,desc,monthly});saveLocal();}
    logAuditEvent('Meta salva','Meta',`${name} • alvo ${fmt(target)}`,cfg.mode==='api'?'online':'local');
    closeM('goalModal');renderGoals();toast('Meta criada! ✓','success');
  }catch(e){toast('Erro: '+e.message,'error');}
}
async function delGoal(id){
  if(!requireWriteAccess('remover metas'))return;
  try{if(cfg.mode==='api')await api('DELETE',`/api/goals/${id}`);S.goals=S.goals.filter(g=>g.id!==id);if(cfg.mode==='local')saveLocal();renderGoals();}
  catch(e){toast('Erro: '+e.message,'error');}
}
async function addGoalAmt(id){
  if(!requireWriteAccess('atualizar metas'))return;
  const v=prompt('Quanto adicionar? (R$)');if(v===null)return;
  const n=parseFloat(v.replace(',','.'));if(isNaN(n)||n<=0){toast('Valor invlido','error');return;}
  try{
    if(cfg.mode==='api'){const u=await api('PATCH',`/api/goals/${id}/add`,{amount:n});const i=S.goals.findIndex(g=>g.id===id);S.goals[i]=nGoal(u);}
    else{const g=S.goals.find(g=>g.id===id);g.current=Math.min(g.current+n,g.target);saveLocal();}
    renderGoals();toast(`${fmt(n)} adicionado! ✓`,'success');
  }catch(e){toast('Erro: '+e.message,'error');}
}
function renderGoals(){
  const el=document.getElementById('goalGrid');if(!el)return;
  const stats=document.getElementById('goalStats');
  if(stats){
    const target=S.goals.reduce((s,g)=>s+g.target,0);
    const current=S.goals.reduce((s,g)=>s+g.current,0);
    const done=S.goals.filter(g=>g.current>=g.target).length;
    const monthly=S.goals.reduce((s,g)=>s+(g.monthly||0),0);
    stats.innerHTML=`
      <div class="insight-card"><div class="insight-k">Guardado</div><div class="insight-v" style="color:var(--ac)">${fmt(current)}</div><div class="cc">de ${fmt(target)}</div></div>
      <div class="insight-card"><div class="insight-k">Progresso geral</div><div class="insight-v" style="color:var(--ac2)">${target?Math.round(current/target*100):0}%</div></div>
      <div class="insight-card"><div class="insight-k">Metas concludas</div><div class="insight-v">${done}/${S.goals.length}</div></div>
      <div class="insight-card"><div class="insight-k">Aporte planejado</div><div class="insight-v" style="color:var(--warn)">${fmt(monthly)}</div><div class="cc">por mês</div></div>`;
  }
  if(!S.goals.length){el.innerHTML=`<div class="empty" style="grid-column:1/-1"><span class="ei">🏆</span><p>Nenhuma meta. Crie uma!</p></div>`;return;}
  el.innerHTML=S.goals.map(g=>{
    const pct=Math.min((g.current/g.target)*100,100);const col=pct>=100?'var(--ac)':pct>=60?'var(--ac2)':'var(--warn)';
    const dl=Math.ceil((new Date(g.deadline+'T12:00:00')-new Date())/864e5);const rem=g.target-g.current;
    const proj=g.monthly>0&&pct<100?`<div style="font-size:12px;color:var(--mt);margin-top:5px">📅 ~${Math.ceil(rem/g.monthly)} meses com ${fmt(g.monthly)}/mês</div>`:'';
    return`<div class="bg-card"><span style="font-size:24px;margin-bottom:9px;display:block">${g.icon}</span><div style="font-family:var(--font-money);font-size:13px;font-weight:800;margin-bottom:2px">${g.name}</div><div style="font-size:10px;color:var(--mt);margin-bottom:10px">${g.desc}</div><div style="display:flex;justify-content:space-between;margin-bottom:5px"><div style="font-family:var(--font-money);font-size:20px;font-weight:700;color:var(--ac)">${fmt(g.current)}</div><div style="font-size:10px;color:var(--mt);align-self:flex-end">de ${fmt(g.target)}</div></div><div class="prg"><div class="pf" style="width:${pct}%;background:${col}"></div></div><div style="display:flex;justify-content:space-between;margin-top:5px"><div style="font-size:11px;color:var(--mt)">📅 ${dl>0?dl+'d':'Encerrado'}</div><span style="font-size:11px;font-weight:600;color:${col}">${Math.round(pct)}%</span></div>${proj}<div style="display:flex;gap:4px;margin-top:9px"><button class="btn btn-g btn-sm" ${writeActionAttrs('atualizar metas')} style="flex:1" onclick="addGoalAmt('${g.id}')">+ Adicionar</button><button class="ib del" ${writeActionAttrs('remover metas')} onclick="delGoal('${g.id}')">🗑️</button></div></div>`;
  }).join('');
}
// CARRO
function carVehicles(){loadCar();return carState.vehicles;}
function carVehicle(id){
  loadCar();
  return carState.vehicles.find(v=>v.id===id)||carState.vehicles.find(v=>v.id===carState.activeVehicleId)||carState.vehicles[0]||nCarVehicle();
}
function activeCarVehicle(){return carVehicle(carState.activeVehicleId);}
function vehicleName(id){return carVehicle(id)?.name||'Veículo';}
function currentCarVehicleFilter(){
  loadCar();
  if(carFilters.vehicle==='active'||!carFilters.vehicle)return carState.activeVehicleId||carState.vehicles[0]?.id||'all';
  return carFilters.vehicle;
}
function carPeriodRange(){
  const now=new Date(today()+'T12:00:00');
  const y=now.getFullYear(),m=now.getMonth();
  if(carFilters.period==='all')return {};
  if(carFilters.period==='custom')return {from:carFilters.from,to:carFilters.to};
  if(carFilters.period==='year')return {from:`${y}-01-01`,to:`${y}-12-31`};
  if(carFilters.period==='90d')return {from:offD(now,-89),to:today()};
  if(carFilters.period==='30d')return {from:offD(now,-29),to:today()};
  return {from:`${y}-${String(m+1).padStart(2,'0')}-01`,to:today()};
}
function carKindValue(e){return e.type==='fuel'?`fuel:${keyCsvHeader(e.fuelType)}`:`cat:${e.category}`;}
function carSortEvents(events){
  const arr=[...events],sort=carFilters.sort||'date_desc',dir=sort.endsWith('_asc')?1:-1;
  if(sort.startsWith('odo'))return arr.sort((a,b)=>((a.odometer||0)-(b.odometer||0))*dir||b.date.localeCompare(a.date));
  if(sort.startsWith('amount'))return arr.sort((a,b)=>((a.amount||0)-(b.amount||0))*dir||b.date.localeCompare(a.date));
  return arr.sort((a,b)=>a.date.localeCompare(b.date)*dir+(a.date===b.date?(a.createdAt-b.createdAt)*dir:0));
}
function daysSinceDate(date){
  if(!date)return null;
  const diff=Math.floor((new Date(today()+'T12:00:00')-new Date(String(date).substring(0,10)+'T12:00:00'))/864e5);
  return Number.isFinite(diff)?Math.max(0,diff):null;
}
function carFilteredEvents(){
  loadCar();
  const vehicle=currentCarVehicleFilter(),range=carPeriodRange(),q=normalizeTxText(carFilters.query||'');
  let events=[...carState.events];
  if(vehicle!=='all')events=events.filter(e=>e.vehicleId===vehicle);
  if(range.from)events=events.filter(e=>e.date>=range.from);
  if(range.to)events=events.filter(e=>e.date<=range.to);
  if(carFilters.type&&carFilters.type!=='all')events=events.filter(e=>e.type===carFilters.type);
  if(carFilters.kind&&carFilters.kind!=='all')events=events.filter(e=>carKindValue(e)===carFilters.kind);
  if(q)events=events.filter(e=>normalizeTxText([vehicleName(e.vehicleId),e.fuelType,e.title,carExpenseLabel(e.category),e.note,e.odometer].join(' ')).includes(q));
  return carSortEvents(events);
}
function carFuelWindowMetrics(allEvents=[],filteredEvents=[]){
  const sortFuels=list=>[...list].filter(e=>e.type==='fuel'&&e.liters>0&&e.odometer>0).sort((a,b)=>a.odometer-b.odometer||a.date.localeCompare(b.date)||((a.createdAt||0)-(b.createdAt||0)));
  const calcFromPair=(fuels,allForVehicle,source)=>{
    if(fuels.length<2)return null;
    const start=fuels[fuels.length-2];
    const end=fuels[fuels.length-1];
    const distance=Math.max(0,(end.odometer||0)-(start.odometer||0));
    if(!distance||!(end.liters>0))return null;
    const windowEvents=allForVehicle.filter(e=>e.date>=start.date&&e.date<=end.date);
    const total=windowEvents.reduce((s,e)=>s+e.amount,0);
    const fuelTotal=windowEvents.filter(e=>e.type==='fuel').reduce((s,e)=>s+e.amount,0);
    return {
      distance,
      liters:end.liters,
      total,
      fuelTotal,
      costPerKm:total/distance,
      kmPerLiter:distance/end.liters,
      source,
      fromDate:start.date,
      toDate:end.date,
      fromOdo:start.odometer||0,
      toOdo:end.odometer||0
    };
  };
  const filteredFuels=sortFuels(filteredEvents);
  const allFuels=sortFuels(allEvents);
  const periodMetrics=calcFromPair(filteredFuels,allEvents,'period');
  if(periodMetrics)return periodMetrics;
  if(allFuels.length>=2){
    const latestFiltered=filteredFuels[filteredFuels.length-1];
    if(latestFiltered){
      const idx=allFuels.findIndex(f=>f.id===latestFiltered.id);
      if(idx>=1){
        const pair=allFuels.slice(idx-1,idx+1);
        const metrics=calcFromPair(pair,allEvents,'fallback_latest_pair');
        if(metrics)return metrics;
      }
    }
    const latestMetrics=calcFromPair(allFuels.slice(-2),allEvents,'fallback_latest_pair');
    if(latestMetrics)return latestMetrics;
  }
  return null;
}
function carStats(events=carFilteredEvents()){
  const byVehicle=new Map();
  events.forEach(e=>{if(!byVehicle.has(e.vehicleId))byVehicle.set(e.vehicleId,[]);byVehicle.get(e.vehicleId).push(e);});
  let distance=0,liters=0,metricTotal=0,metricFuelTotal=0;
  const metricSources=[];
  byVehicle.forEach((items,vehicleId)=>{
    const allVehicleEvents=carState.events.filter(e=>e.vehicleId===vehicleId);
    const metrics=carFuelWindowMetrics(allVehicleEvents,items);
    if(metrics){
      distance+=metrics.distance;
      liters+=metrics.liters;
      metricTotal+=metrics.total;
      metricFuelTotal+=metrics.fuelTotal;
      metricSources.push(metrics);
    }
  });
  const total=events.reduce((s,e)=>s+e.amount,0);
  const fuelTotal=events.filter(e=>e.type==='fuel').reduce((s,e)=>s+e.amount,0);
  const expenseTotal=events.filter(e=>e.type==='expense').reduce((s,e)=>s+e.amount,0);
  const fuelCount=events.filter(e=>e.type==='fuel').length;
  const expenseCount=events.filter(e=>e.type==='expense').length;
  const kmPerLiter=distance&&liters?distance/liters:0;
  const costPerKm=distance?(metricTotal||total)/distance:0;
  const avgTicket=events.length?total/events.length:0;
  const avgFuelPrice=liters?(metricFuelTotal||fuelTotal)/liters:0;
  const vehicle=currentCarVehicleFilter();
  const maxOdo=vehicle==='all'?0:Math.max(carVehicle(vehicle).odometer||0,...events.map(e=>e.odometer||0));
  const lastEvent=[...events].sort((a,b)=>b.date.localeCompare(a.date)||b.createdAt-a.createdAt)[0]||null;
  const byKind=new Map();
  events.forEach(e=>{
    const key=e.type==='fuel'?(e.fuelType||'Combustível'):(e.title||carExpenseLabel(e.category));
    if(!byKind.has(key))byKind.set(key,{label:key,total:0,count:0});
    const item=byKind.get(key);item.total+=e.amount;item.count++;
  });
  const topKind=[...byKind.values()].sort((a,b)=>b.total-a.total)[0]||null;
  const primaryMetric=metricSources[0]||null;
  return {events,distance,liters,kmPerLiter,total,fuelTotal,expenseTotal,fuelCount,expenseCount,costPerKm,maxOdo,vehicle,avgTicket,avgFuelPrice,lastEvent,topKind,metricSource:primaryMetric?.source||'',metricFromDate:primaryMetric?.fromDate||'',metricToDate:primaryMetric?.toDate||''};
}
function monthBounds(date=new Date()){
  const y=date.getFullYear(),m=date.getMonth();
  const first=`${y}-${String(m+1).padStart(2,'0')}-01`;
  const last=new Date(y,m+1,0).getDate();
  return {from:first,to:`${y}-${String(m+1).padStart(2,'0')}-${String(last).padStart(2,'0')}`,days:last};
}
function extractCarVendor(note=''){
  const cleaned=String(note||'').split('•')[0].trim();
  if(!cleaned||/^importado do drivvo$/i.test(cleaned))return '';
  return cleaned;
}
function carMaintenanceInsights(events=carFilteredEvents()){
  const singleVehicle=currentCarVehicleFilter()==='all'?null:currentCarVehicleFilter();
  const baseEvents=singleVehicle?events.filter(e=>e.vehicleId===singleVehicle):events;
  const maintenance=baseEvents.filter(e=>e.type==='expense');
  const fuelEvents=baseEvents.filter(e=>e.type==='fuel');
  const oilLike=maintenance.filter(e=>/oleo|filtro|revis|troca/.test(normalizeTxText(`${e.title} ${e.note}`)));
  const latestOil=[...oilLike].sort((a,b)=>b.date.localeCompare(a.date)||b.odometer-a.odometer)[0]||null;
  const highestOdo=Math.max(0,...baseEvents.map(e=>e.odometer||0));
  const nextOilKm=latestOil?.odometer?latestOil.odometer+5000:null;
  const kmLeft=nextOilKm?Math.max(0,nextOilKm-highestOdo):null;
  const nextOilDate=latestOil?.date?offD(new Date(latestOil.date+'T12:00:00'),180):null;
  const kmSinceService=latestOil?.odometer&&highestOdo?Math.max(0,highestOdo-latestOil.odometer):null;
  const daysSinceService=latestOil?.date?Math.max(0,Math.floor((Date.now()-new Date(latestOil.date+'T12:00:00').getTime())/86400000)):null;
  const maintenanceSpend=maintenance.reduce((s,e)=>s+e.amount,0);
  const vendors=new Map();
  baseEvents.forEach(e=>{
    const vendor=extractCarVendor(e.note);
    if(!vendor)return;
    if(!vendors.has(vendor))vendors.set(vendor,{name:vendor,total:0,count:0,fuel:0,expense:0});
    const item=vendors.get(vendor);
    item.total+=e.amount||0;
    item.count++;
    if(e.type==='fuel')item.fuel+=e.amount||0;else item.expense+=e.amount||0;
  });
  const topVendors=[...vendors.values()].sort((a,b)=>b.total-a.total).slice(0,4);
  const recurringCats={};
  maintenance.forEach(e=>{const label=carExpenseLabel(e.category);recurringCats[label]=(recurringCats[label]||0)+e.amount;});
  const topMaintenance=Object.entries(recurringCats).sort((a,b)=>b[1]-a[1])[0]||null;
  const upcomingStatus=kmLeft!==null&&kmLeft<=500?'urgent':daysSinceService!==null&&daysSinceService>=170?'warn':'ok';
  const latestMaintenance=[...maintenance].sort((a,b)=>b.date.localeCompare(a.date)||b.amount-a.amount).slice(0,4);
  return {latestOil,nextOilKm,kmLeft,nextOilDate,kmSinceService,daysSinceService,maintenanceSpend,topVendors,topMaintenance,highestOdo,upcomingStatus,latestMaintenance};
}
function openCarMaintenanceEntry(type='oil'){
  openCarEntry('expense');
  const category=document.getElementById('carCategory');
  const title=document.getElementById('carTitle');
  const note=document.getElementById('carNote');
  if(category)category.value='Maintenance';
  const presets={
    oil:{title:'Troca de óleo',note:'Revisão preventiva'},
    review:{title:'Revisão geral',note:'Checklist oficina'},
    tire:{title:'Alinhamento / pneus',note:'Manutenção preventiva'}
  };
  const preset=presets[type]||presets.review;
  if(title)title.value=preset.title;
  if(note&&!note.value)note.value=preset.note;
}
function popCarAccSel(){
  const el=document.getElementById('carAcc');if(!el)return;
  el.innerHTML=S.accounts.map(a=>`<option value="${a.id}">${esc(a.icon)} ${esc(a.name)}</option>`).join('');
}
function popCarVehicleSels(){
  loadCar();
  const opts=carState.vehicles.map(v=>`<option value="${v.id}">${esc(v.name)}${v.plate?` - ${esc(v.plate)}`:''}</option>`).join('');
  const entry=document.getElementById('carEntryVehicle');
  if(entry){entry.innerHTML=opts;entry.value=carState.activeVehicleId||carState.vehicles[0]?.id||'';}
  const filter=document.getElementById('carVehicleFilter');
  if(filter){
    const selected=currentCarVehicleFilter();
    filter.innerHTML=`<option value="all">Todos os veículos</option>${opts}`;
    filter.value=carFilters.vehicle==='all'?'all':selected;
  }
}
function renderCarKindFilter(events=carState.events){
  const el=document.getElementById('carKindFilter');if(!el)return;
  const fuel=new Map(),cats=new Map();
  events.forEach(e=>{
    if(e.type==='fuel')fuel.set(`fuel:${keyCsvHeader(e.fuelType)}`,e.fuelType||'Combustível');
    else cats.set(`cat:${e.category}`,carExpenseLabel(e.category));
  });
  let html='<option value="all">Combustível / tipo</option>';
  if(fuel.size)html+='<optgroup label="Combustíveis">'+[...fuel].sort((a,b)=>a[1].localeCompare(b[1])).map(([v,l])=>`<option value="${esc(v)}">${esc(l)}</option>`).join('')+'</optgroup>';
  if(cats.size)html+='<optgroup label="Despesas">'+[...cats].sort((a,b)=>a[1].localeCompare(b[1])).map(([v,l])=>`<option value="${esc(v)}">${esc(l)}</option>`).join('')+'</optgroup>';
  el.innerHTML=html;
  if([...fuel.keys(),...cats.keys(),'all'].includes(carFilters.kind))el.value=carFilters.kind;
  else{carFilters.kind='all';el.value='all';}
}
function syncCarFilterControls(){
  popCarVehicleSels();
  const period=document.getElementById('carPeriodFilter');if(period)period.value=carFilters.period;
  const type=document.getElementById('carTypeFilter');if(type)type.value=carFilters.type;
  const sort=document.getElementById('carSortFilter');if(sort)sort.value=carFilters.sort;
  const q=document.getElementById('carSearchFilter');if(q&&q.value!==carFilters.query)q.value=carFilters.query;
  const range=document.getElementById('carCustomRange');if(range)range.style.display=carFilters.period==='custom'?'grid':'none';
  const from=document.getElementById('carFromFilter');if(from&&from.value!==carFilters.from)from.value=carFilters.from;
  const to=document.getElementById('carToFilter');if(to&&to.value!==carFilters.to)to.value=carFilters.to;
  renderCarKindFilter(carState.events);
}
function setCarVehicleFilter(value){
  loadCar();
  carFilters.vehicle=value||'all';
  if(value&&value!=='all')carState.activeVehicleId=value;
  saveCar();
  renderCar();
}
function setCarPeriodFilter(value){
  carFilters.period=value||'month';
  if(carFilters.period==='custom'&&!carFilters.from&&!carFilters.to){carFilters.from=offD(new Date(today()+'T12:00:00'),-29);carFilters.to=today();}
  renderCar();
}
function setCarFilter(key,value){
  carFilters[key]=value||'';
  renderCar();
}
function setCarChartMode(mode){
  carChartMode=mode==='line'?'line':'bars';
  document.getElementById('carChartBars')?.classList.toggle('active',carChartMode==='bars');
  document.getElementById('carChartLine')?.classList.toggle('active',carChartMode==='line');
  renderCar();
}
function updateCarAmount(){
  const liters=Number(document.getElementById('carLiters')?.value)||0;
  const price=Number(document.getElementById('carPrice')?.value)||0;
  const amount=document.getElementById('carAmount');
  if(amount&&liters&&price&&!amount.dataset.manual)amount.value=(liters*price).toFixed(2);
}
function setCarEntryType(type){
  const isFuel=type==='fuel';
  document.getElementById('carTypeFuel')?.classList.toggle('active',isFuel);
  document.getElementById('carTypeExpense')?.classList.toggle('active',!isFuel);
  const fuelFields=document.getElementById('carFuelFields');
  const expenseFields=document.getElementById('carExpenseFields');
  if(fuelFields)fuelFields.style.display=isFuel?'block':'none';
  if(expenseFields)expenseFields.style.display=isFuel?'none':'block';
}
function openCarEntry(type='fuel'){
  if(!requireWriteAccess('criar registros do carro'))return;
  const v=activeCarVehicle();
  document.getElementById('carEntryId').value='';
  popCarVehicleSels();
  document.getElementById('carEntryVehicle').value=v.id;
  document.getElementById('carDate').value=today();
  document.getElementById('carOdo').value=v.odometer||'';
  document.getElementById('carFuelType').value='Gasolina';
  document.getElementById('carLiters').value='';
  document.getElementById('carPrice').value='';
  const amount=document.getElementById('carAmount');amount.value='';delete amount.dataset.manual;
  const expenseAmount=document.getElementById('carAmountExpense');if(expenseAmount)expenseAmount.value='';
  document.getElementById('carCategory').value='Maintenance';
  document.getElementById('carTitle').value='';
  document.getElementById('carNote').value='';
  popCarAccSel();
  setCarEntryType(type);
  document.getElementById('carModal').classList.add('open');
}
async function createCarTransaction(event){
  const accountId=event.accountId||S.accounts[0]?.id||null;
  const desc=event.type==='fuel'
    ? `Abastecimento - ${event.fuelType}`
    : (event.title||carExpenseLabel(event.category));
  const note=[vehicleName(event.vehicleId),event.odometer?`${event.odometer} km`:'',event.note].filter(Boolean).join(' • ');
  const payload={type:'expense',description:desc,amount:event.amount,category:'Carro',date:event.date,note,account_id:accountId,paid:false,pending:false};
  if(cfg.mode==='api'){
    const saved=await api('POST','/api/transactions',payload);
    const tx=nTx({...saved,accountId});
    S.transactions.unshift(tx);
    return tx.id;
  }
  const tx={id:uid(),type:'expense',desc,amount:event.amount,category:'Carro',date:event.date,note,accountId,installmentGroup:null,installmentNum:null,installmentTotal:null,recurGroup:null,paid:false,pending:false};
  S.transactions.unshift(tx);
  saveLocal();
  return tx.id;
}
async function saveCarEntry(){
  if(!requireWriteAccess('salvar registros do carro'))return;
  loadCar();
  const vehicleId=document.getElementById('carEntryVehicle').value||carState.activeVehicleId;
  const v=carVehicle(vehicleId);
  if(!v?.id){toast('Cadastre um veículo primeiro','error');return;}
  carState.activeVehicleId=v.id;
  const isFuel=document.getElementById('carTypeFuel').classList.contains('active');
  const date=document.getElementById('carDate').value||today();
  const odometer=Number(document.getElementById('carOdo').value)||0;
  const liters=Number(document.getElementById('carLiters').value)||0;
  const pricePerLiter=Number(document.getElementById('carPrice').value)||0;
  const amount=isFuel
    ? (Number(document.getElementById('carAmount').value)||Number((liters*pricePerLiter).toFixed(2))||0)
    : (Number(document.getElementById('carAmountExpense').value)||0);
  if(!amount||amount<=0){toast('Informe o valor','error');return;}
  if(isFuel&&(!liters||!pricePerLiter)){toast('Informe litros e preço por litro','error');return;}
  const event=nCarEvent({
    vehicleId:v.id,
    type:isFuel?'fuel':'expense',
    date,
    odometer,
    fuelType:document.getElementById('carFuelType').value,
    liters,
    pricePerLiter,
    amount,
    title:document.getElementById('carTitle').value.trim(),
    category:document.getElementById('carCategory').value,
    note:document.getElementById('carNote').value.trim(),
    accountId:document.getElementById('carAcc').value||S.accounts[0]?.id||null
  });
  v.odometer=Math.max(v.odometer||0,odometer||0);
  try{
    event.txId=await createCarTransaction(event);
    carState.events.unshift(event);
    saveCar();
    logAuditEvent('Registro do carro salvo','Veiculo',`${vehicleName(event.vehicleId)} • ${fmt(event.amount)}`,cfg.mode==='api'?'online':'local');
    closeM('carModal');
    renderCar();
    renderDash();
    toast('Registro do carro salvo e lançado nas transações','success');
  }catch(e){toast('Erro: '+e.message,'error');}
}
function openCarVehicleModal(id=''){
  if(!requireWriteAccess(id?'editar veículos':'criar veículos'))return;
  loadCar();
  const v=id?carVehicle(id):null;
  document.getElementById('carVehicleModalTitle').textContent=v?.id?'Editar veículo':'Novo veículo';
  document.getElementById('carVehicleId').value=v?.id||'';
  document.getElementById('carVehicleName').value=v?.name||'';
  document.getElementById('carPlate').value=v?.plate||'';
  document.getElementById('carModel').value=v?.model||'';
  document.getElementById('carVehicleOdo').value=v?.odometer||'';
  const del=document.getElementById('carVehicleDeleteBtn');if(del)del.style.display=v?.id?'':'none';
  document.getElementById('carVehicleModal').classList.add('open');
}
function openSelectedCarVehicleModal(){
  loadCar();
  const id=currentCarVehicleFilter()==='all'?carState.activeVehicleId:currentCarVehicleFilter();
  openCarVehicleModal(id||carState.vehicles[0]?.id||'');
}
function saveCarVehicle(){
  if(!requireWriteAccess('salvar veículos'))return;
  loadCar();
  const id=document.getElementById('carVehicleId').value;
  const name=document.getElementById('carVehicleName').value.trim();
  if(!name){toast('Informe o nome do veículo','error');return;}
  const isNew=!id;
  const data={
    id:id||uid(),
    name,
    plate:document.getElementById('carPlate').value.trim().toUpperCase(),
    model:document.getElementById('carModel').value.trim(),
    odometer:Number(document.getElementById('carVehicleOdo').value)||0
  };
  const idx=carState.vehicles.findIndex(v=>v.id===id);
  if(idx>=0)carState.vehicles[idx]={...carState.vehicles[idx],...data};
  else carState.vehicles.push(data);
  carState.activeVehicleId=data.id;
  if(carFilters.vehicle!=='all'){
    carFilters.vehicle=isNew&&carState.events.length?'all':data.id;
  }
  saveCar();
  closeM('carVehicleModal');
  renderCar();
  toast('Veículo salvo','success');
}
function deleteCarVehicle(){
  if(!requireWriteAccess('remover veículos'))return;
  loadCar();
  const id=document.getElementById('carVehicleId').value;
  if(!id)return;
  if(carState.vehicles.length<=1){toast('Mantenha pelo menos um veículo','error');return;}
  const count=carState.events.filter(e=>e.vehicleId===id).length;
  if(count){toast('Esse veículo tem registros. Exclua ou mova os registros antes.','error');return;}
  if(!confirm('Excluir este veículo?'))return;
  carState.vehicles=carState.vehicles.filter(v=>v.id!==id);
  if(carState.activeVehicleId===id)carState.activeVehicleId=carState.vehicles[0]?.id||'';
  if(carFilters.vehicle===id)carFilters.vehicle=carState.activeVehicleId||'all';
  saveCar();
  closeM('carVehicleModal');
  renderCar();
  toast('Veículo excluído','info');
}
function carExpenseLabel(cat){
  return ({Maintenance:'Manutenção',Insurance:'Seguro',Tax:'Imposto',Parking:'Estacionamento',Wash:'Lavagem',Fine:'Multa',Other:'Outro'})[cat]||cat||'Despesa do carro';
}
function parseCsvRows(text){
  const raw=String(text||'').replace(/^\uFEFF/,'');
  const sample=raw.split(/\r?\n/).slice(0,5).join('\n');
  const candidates=[';',',','\t'];
  const delimiter=candidates.map(d=>({d,n:(sample.match(new RegExp(d==='\t'?'\\t':`\\${d}`,'g'))||[]).length})).sort((a,b)=>b.n-a.n)[0]?.d||';';
  const rows=[];let row=[],cell='',q=false;
  for(let i=0;i<raw.length;i++){
    const ch=raw[i],nx=raw[i+1];
    if(ch==='"'){
      if(q&&nx==='"'){cell+='"';i++;}
      else q=!q;
    }else if(ch===delimiter&&!q){row.push(cell);cell='';}
    else if((ch==='\n'||ch==='\r')&&!q){
      if(ch==='\r'&&nx==='\n')i++;
      row.push(cell);cell='';
      if(row.some(v=>String(v).trim()))rows.push(row);
      row=[];
    }else cell+=ch;
  }
  row.push(cell);
  if(row.some(v=>String(v).trim()))rows.push(row);
  return rows;
}
async function readCsvFileText(file){
  const buffer=await file.arrayBuffer();
  const utf8=new TextDecoder('utf-8').decode(buffer);
  if(!utf8.includes('\uFFFD'))return utf8;
  try{return new TextDecoder('windows-1252').decode(buffer);}
  catch{return utf8;}
}
function keyCsvHeader(h){
  return String(h||'').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-z0-9]+/g,' ').trim();
}
function pickCsv(row, headers, aliases){
  for(const alias of aliases){
    const key=keyCsvHeader(alias);
    const i=headers.findIndex(h=>h&&(h===key||h.includes(key)||key.includes(h)));
    if(i>=0&&row[i]!=null&&String(row[i]).trim()!=='')return String(row[i]).trim();
  }
  return '';
}
function parseCsvNumber(value){
  let raw=String(value||'').trim().replace(/[^\d,.-]/g,'');
  if(!raw)return 0;
  const lastComma=raw.lastIndexOf(','),lastDot=raw.lastIndexOf('.');
  if(lastComma>=0&&lastDot>=0)raw=lastComma>lastDot?raw.replace(/\./g,'').replace(',','.'):raw.replace(/,/g,'');
  else if(lastComma>=0)raw=raw.replace(/\./g,'').replace(',','.');
  const n=parseFloat(raw);
  return Number.isFinite(n)?n:0;
}
function parseCsvDate(value){
  const raw=String(value||'').trim();
  if(!raw)return today();
  const iso=raw.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/);
  if(iso)return `${iso[1]}-${iso[2].padStart(2,'0')}-${iso[3].padStart(2,'0')}`;
  const br=raw.match(/^(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})/);
  if(br){
    let y=br[3];if(y.length===2)y='20'+y;
    const a=parseInt(br[1],10),b=parseInt(br[2],10);
    const day=a>12?a:(b>12?b:a);
    const month=a>12?b:(b>12?a:b);
    return `${y}-${String(month).padStart(2,'0')}-${String(day).padStart(2,'0')}`;
  }
  const d=new Date(raw);
  return Number.isNaN(d.getTime())?today():d.toISOString().split('T')[0];
}
function carCategoryFromText(text){
  const n=normalizeTxText(text);
  if(/seguro|insurance/.test(n))return 'Insurance';
  if(/ipva|licenciamento|taxa|imposto|document|tax/.test(n))return 'Tax';
  if(/estacion|parking|pedagio|toll/.test(n))return 'Parking';
  if(/lavagem|lava|wash/.test(n))return 'Wash';
  if(/multa|fine/.test(n))return 'Fine';
  if(/manut|revis|oleo|pneu|oficina|service|repair|maintenance/.test(n))return 'Maintenance';
  return 'Other';
}
function eventLooksFuel(rowText, liters, fuelType){
  const n=normalizeTxText(`${rowText} ${fuelType}`);
  return liters>0||/abastec|combust|gasolina|etanol|alcool|diesel|gnv|fuel|refuel|gas station/.test(n);
}
function csvHeaderScore(row){
  const keys=row.map(keyCsvHeader);
  const joined=keys.join(' ');
  let score=0;
  ['data','date','odometro','odometer','valor total','amount','combustivel','fuel','volume','litros','tipo de despesa','expense'].forEach(k=>{
    if(joined.includes(keyCsvHeader(k)))score++;
  });
  return score;
}
function csvSectionName(row){
  const first=String(row?.[0]||'').trim();
  return first.startsWith('##')?keyCsvHeader(first.replace(/^##/,'')):'';
}
function carImportStatus(message,type='info'){
  const el=document.getElementById('carImportStatus');
  if(!el)return;
  el.textContent=message||'';
  el.className=`car-import-status ${type}`;
}
function roundCarImportNum(v,dec=3){
  const n=Number(v)||0,base=10**dec;
  return Math.round(n*base)/base;
}
function carEventFingerprint(e={}){
  return [
    String(e.vehicleId||''),
    String(e.type||''),
    String(e.date||'').substring(0,10),
    roundCarImportNum(e.odometer,0),
    roundCarImportNum(e.amount,2),
    roundCarImportNum(e.liters,3),
    roundCarImportNum(e.pricePerLiter,3),
    keyCsvHeader(e.fuelType||''),
    keyCsvHeader(e.title||''),
    keyCsvHeader(e.category||'')
  ].join('|');
}
function mapDrivvoCsvEvent(row, headers, vehicleId, section=''){
  const rowText=row.join(' ');
  const date=parseCsvDate(pickCsv(row,headers,['data','date','dia']));
  const odometer=parseCsvNumber(pickCsv(row,headers,['odometro','odômetro','odometer','hodometro','quilometragem','quilometragem atual','km','mileage']));
  const liters=parseCsvNumber(pickCsv(row,headers,['litros','liters','litres','volume','quantidade','qtd','quantity']));
  const fuelType=pickCsv(row,headers,['combustivel','combustível','fuel','fuel type','tipo combustivel'])||'Gasolina';
  const pricePerLiter=parseCsvNumber(pickCsv(row,headers,['preco litro','preço litro','valor litro','price liter','price per liter','unit price','preco por litro','preço por litro','preco','preço']));
  const amount=parseCsvNumber(pickCsv(row,headers,['valor total','preco total','preço total','total','amount','valor','custo','cost','valor pago']))||Number((liters*pricePerLiter).toFixed(2));
  const title=pickCsv(row,headers,['descricao','descrição','description','servico','serviço','service','categoria','category','tipo','type']);
  if(!amount&&!liters)return null;
  const isFuel=section.includes('refuelling')||section.includes('abastec')||eventLooksFuel(rowText,liters,fuelType);
  const isExpense=section.includes('expense')||section.includes('despesa');
  return nCarEvent({
    vehicleId,
    type:isExpense?'expense':isFuel?'fuel':'expense',
    date,
    odometer,
    fuelType,
    liters,
    pricePerLiter:pricePerLiter||(liters&&amount?amount/liters:0),
    amount,
    title:isFuel?'':title,
    category:isFuel?'Combustivel':carCategoryFromText(`${title} ${rowText}`),
    note:pickCsv(row,headers,['observacao','observação','notes','note','posto','gas station','local','place'])||'Importado do Drivvo',
    accountId:S.accounts[0]?.id||null
  });
}
async function importCarCsv(inp){
  const file=inp.files?.[0];if(!file)return;
  if(!requireWriteAccess('importar registros do carro')){inp.value='';return;}
  try{
    carImportStatus(`Lendo ${file.name}...`,'info');
    const text=await readCsvFileText(file);
    const rows=parseCsvRows(text);
    if(rows.length<2)throw new Error('CSV sem linhas suficientes.');
    loadCar();
    const v=activeCarVehicle();
    const createTransactions=!!document.getElementById('carImportTxChk')?.checked;
    const existing=new Set(carState.events.filter(e=>e.vehicleId===v.id).map(carEventFingerprint));
    let imported=0,skipped=0,fuels=0,expenses=0,section='',headers=null,vehicleName=false;
    for(let idx=0;idx<rows.length;idx++){
      const row=rows[idx];
      const nextSection=csvSectionName(row);
      if(nextSection){section=nextSection;headers=null;continue;}
      if(!headers){
        if(csvHeaderScore(row)>=2){
          headers=row.map(keyCsvHeader);
          vehicleName=headers.some(h=>h.includes('veiculo')||h.includes('vehicle'));
        }
        continue;
      }
      if(csvHeaderScore(row)>=3){headers=row.map(keyCsvHeader);continue;}
      const event=mapDrivvoCsvEvent(row,headers,v.id,section);
      if(event){
        const csvVehicle=vehicleName?pickCsv(row,headers,['veiculo','vehicle','carro','car']):'';
        if(csvVehicle&&!v.name)v.name=csvVehicle;
        const fingerprint=carEventFingerprint(event);
        if(existing.has(fingerprint)){skipped++;continue;}
        existing.add(fingerprint);
        if(createTransactions)event.txId=await createCarTransaction(event);
        carState.events.unshift(event);
        v.odometer=Math.max(v.odometer||0,event.odometer||0);
        imported++;
        if(event.type==='fuel')fuels++;else expenses++;
        if(imported%25===0)carImportStatus(`Importando... ${imported} registros lidos`,'info');
      }
    }
    if(!imported){
      if(skipped)throw new Error('Esse CSV parece ja ter sido importado para este veiculo.');
      throw new Error('Nao encontrei abastecimentos ou despesas reconheciveis nesse CSV.');
    }
    saveCar();
    renderCar();
    refreshAll();
    const msg=`Importado: ${imported} registros (${fuels} abastecimentos, ${expenses} despesas)${createTransactions?' + transações':' sem transações'}${skipped?` • ${skipped} duplicados ignorados`:''}`;
    carImportStatus(msg,'success');
    toast(msg,'success');
  }catch(e){
    carImportStatus('Erro ao importar CSV: '+e.message,'error');
    toast('Erro ao importar CSV: '+e.message,'error');
  }finally{
    inp.value='';
  }
}
async function deleteCarEvent(id){
  if(!requireWriteAccess('remover registros do carro'))return;
  const event=carState.events.find(e=>e.id===id);if(!event)return;
  if(!confirm('Remover este registro do carro?'))return;
  const txSnapshot=event.txId?S.transactions.find(t=>t.id===event.txId):null;
  try{
    if(event.txId){
      if(cfg.mode==='api')await api('DELETE',`/api/transactions/${event.txId}`).catch(()=>{});
      S.transactions=S.transactions.filter(t=>t.id!==event.txId);
      if(cfg.mode==='local')saveLocal();
    }
    carState.events=carState.events.filter(e=>e.id!==id);
    saveCar();
    renderCar();
    refreshAll();
    queueUndo(`Registro removido: ${event.type==='fuel'?'Abastecimento':'Despesa do carro'}`,async()=>{
      let restoredTxId=event.txId||null;
      if(txSnapshot){
        if(cfg.mode==='api'){
          const restored=await api('POST','/api/transactions',{type:txSnapshot.type,description:txSnapshot.desc,amount:txSnapshot.amount,category:txSnapshot.category,date:txSnapshot.date,note:txSnapshot.note||'',account_id:txSnapshot.accountId,paid:txSnapshot.paid,pending:txSnapshot.pending});
          const normalized=nTx({...restored,accountId:txSnapshot.accountId});
          S.transactions.unshift(normalized);
          restoredTxId=normalized.id;
        }else{S.transactions.unshift(txSnapshot);saveLocal();restoredTxId=txSnapshot.id;}
      }
      carState.events.unshift({...event,txId:restoredTxId});
      saveCar();
      renderCar();
    });
    toast('Registro removido','info');
  }catch(e){toast('Erro: '+e.message,'error');}
}
function renderCar(){
  loadCar();
  syncCarFilterControls();
  const events=carFilteredEvents();
  const st=carStats(events);
  const maint=carMaintenanceInsights(events);
  const single=st.vehicle!=='all';
  const v=single?carVehicle(st.vehicle):null;
  const title=document.getElementById('carTitleView');if(title)title.textContent=single?(v.name||'Meu veículo'):'Todos os veículos';
  const sub=document.getElementById('carSubView');if(sub)sub.textContent=single?[v.model,v.plate,st.maxOdo?`${Math.round(st.maxOdo).toLocaleString('pt-BR')} km`:'' ].filter(Boolean).join(' • ')||'Consumo, abastecimentos e despesas':`${carState.vehicles.length} veículo${carState.vehicles.length!==1?'s':''} cadastrados`;
  const stats=document.getElementById('carStats');
  const activeVehicles=new Set(st.events.map(e=>e.vehicleId)).size;
  const lastDays=daysSinceDate(st.lastEvent?.date);
  const lastLabel=st.lastEvent?`${fmtD(st.lastEvent.date)}${lastDays===0?' • hoje':lastDays!==null?` • há ${lastDays}d`:''}`:'sem registros';
  const usesFallbackMetrics=st.metricSource==='fallback_latest_pair';
  const distanceCaption=st.distance?(usesFallbackMetrics?'ultimo intervalo valido':'calculado por veiculo'):'precisa de 2 abastecimentos';
  const litersCaption=st.liters?(usesFallbackMetrics?`${st.liters.toLocaleString('pt-BR',{maximumFractionDigits:1})} L no ultimo intervalo valido`:`${st.liters.toLocaleString('pt-BR',{maximumFractionDigits:1})} L medidos`):'sem litros suficientes';
  if(stats)stats.innerHTML=`
    <div class="insight-card"><div class="insight-k">Gastos totais</div><div class="insight-v" style="color:var(--dan)">${fmt(st.total)}</div><div class="cc">${st.events.length} registro${st.events.length!==1?'s':''} no filtro</div></div>
    <div class="insight-card"><div class="insight-k">Km rodados</div><div class="insight-v" style="color:var(--ac2)">${st.distance?Math.round(st.distance).toLocaleString('pt-BR'):'—'}</div><div class="cc">${distanceCaption}</div></div>
    <div class="insight-card"><div class="insight-k">Consumo médio</div><div class="insight-v" style="color:var(--ac)">${st.kmPerLiter?st.kmPerLiter.toFixed(1).replace('.',','):'—'} km/l</div><div class="cc">${litersCaption}</div></div>
    <div class="insight-card"><div class="insight-k">Custo por km</div><div class="insight-v" style="color:var(--warn)">${st.costPerKm?fmt(st.costPerKm):'—'}</div><div class="cc">combustível + despesas</div></div>
    <div class="insight-card"><div class="insight-k">Combustível</div><div class="insight-v" style="color:var(--warn)">${fmt(st.fuelTotal)}</div><div class="cc">${st.fuelCount} abastecimento${st.fuelCount!==1?'s':''} • ${st.avgFuelPrice?fmt(st.avgFuelPrice)+'/L':'sem média'}</div></div>
    <div class="insight-card"><div class="insight-k">Despesas</div><div class="insight-v" style="color:var(--dan)">${fmt(st.expenseTotal)}</div><div class="cc">${st.expenseCount} despesa${st.expenseCount!==1?'s':''} fora do tanque</div></div>
    <div class="insight-card"><div class="insight-k">Ticket médio</div><div class="insight-v">${st.avgTicket?fmt(st.avgTicket):'—'}</div><div class="cc">por registro no período</div></div>
    <div class="insight-card"><div class="insight-k">Último lançamento</div><div class="insight-v">${st.lastEvent?fmt(st.lastEvent.amount):'—'}</div><div class="cc">${lastLabel}</div></div>
    <div class="insight-card"><div class="insight-k">Hodômetro</div><div class="insight-v">${single&&st.maxOdo?Math.round(st.maxOdo).toLocaleString('pt-BR'):'—'}</div><div class="cc">${single?'veículo selecionado':'selecione um veículo'}</div></div>
    <div class="insight-card"><div class="insight-k">Maior peso</div><div class="insight-v">${st.topKind?esc(st.topKind.label):'—'}</div><div class="cc">${st.topKind?`${fmt(st.topKind.total)} em ${st.topKind.count} registro${st.topKind.count!==1?'s':''}`:'sem dados'}</div></div>
    <div class="insight-card"><div class="insight-k">Veículos</div><div class="insight-v">${single?'1':activeVehicles}</div><div class="cc">${single?esc(v.name):'com registros no filtro'}</div></div>`;
  document.getElementById('carChartBars')?.classList.toggle('active',carChartMode==='bars');
  document.getElementById('carChartLine')?.classList.toggle('active',carChartMode==='line');
  if(typeof renderCarCharts==='function')renderCarCharts(events,st);
  const maintEl=document.getElementById('carMaintenancePanel');
  if(maintEl)maintEl.innerHTML=`
    <div class="car-maint-grid">
      <div class="car-maint-card ${maint.upcomingStatus==='urgent'?'urgent':maint.upcomingStatus==='warn'?'warn':''}">
        <div class="car-maint-k">Próxima revisão</div>
        <div class="car-maint-v">${maint.nextOilKm?`${Math.round(maint.nextOilKm).toLocaleString('pt-BR')} km`:'—'}</div>
        <div class="cc">${maint.kmLeft!==null?`${Math.round(maint.kmLeft).toLocaleString('pt-BR')} km restantes`:'cadastre troca de óleo ou revisão'}</div>
      </div>
      <div class="car-maint-card">
        <div class="car-maint-k">Prazo por data</div>
        <div class="car-maint-v">${maint.nextOilDate?fmtD(maint.nextOilDate):'—'}</div>
        <div class="cc">${maint.latestOil?`última revisão em ${fmtD(maint.latestOil.date)}`:'sem referência ainda'}</div>
      </div>
      <div class="car-maint-card">
        <div class="car-maint-k">Maior gasto de manutenção</div>
        <div class="car-maint-v">${maint.topMaintenance?esc(maint.topMaintenance[0]):'—'}</div>
        <div class="cc">${maint.topMaintenance?fmt(maint.topMaintenance[1]):'sem despesas suficientes'}</div>
      </div>
      <div class="car-maint-card">
        <div class="car-maint-k">Rodado desde a revisão</div>
        <div class="car-maint-v">${maint.kmSinceService!==null?`${Math.round(maint.kmSinceService).toLocaleString('pt-BR')} km`:'—'}</div>
        <div class="cc">${maint.daysSinceService!==null?`${maint.daysSinceService} dias desde o último serviço`:'sem histórico suficiente'}</div>
      </div>
      <div class="car-maint-card">
        <div class="car-maint-k">Gasto de manutenção</div>
        <div class="car-maint-v">${fmt(maint.maintenanceSpend||0)}</div>
        <div class="cc">${maint.latestMaintenance.length} lançamento${maint.latestMaintenance.length!==1?'s':''} no filtro</div>
      </div>
      <div class="car-maint-card car-maint-actions">
        <div class="car-maint-k">Ações rápidas</div>
        <div class="car-maint-btns">
          <button class="btn btn-p" onclick="openCarMaintenanceEntry('oil')">Troca de óleo</button>
          <button class="btn btn-g" onclick="openCarMaintenanceEntry('review')">Revisão</button>
        </div>
        <div class="cc">Abre o lançamento já preenchido para você só confirmar.</div>
      </div>
    </div>
    <div class="car-maint-lower">
      <div class="car-vendor-box">
        <div class="bh"><div><div class="ct">Postos e oficinas</div><div class="cs">Quem mais pesa no período filtrado</div></div></div>
        <div class="car-vendor-list">${maint.topVendors.length?maint.topVendors.map(vendor=>`<div class="car-vendor-row"><div><div class="car-vendor-name">${esc(vendor.name)}</div><div class="car-vendor-meta">${vendor.count} registro${vendor.count!==1?'s':''} • combustível ${fmt(vendor.fuel)} • despesas ${fmt(vendor.expense)}</div></div><div class="car-vendor-amt">${fmt(vendor.total)}</div></div>`).join(''):'<div class="sync-empty">Adicione observações com posto ou oficina para ver o ranking.</div>'}</div>
      </div>
      <div class="car-vendor-box">
        <div class="bh"><div><div class="ct">Histórico recente de manutenção</div><div class="cs">Últimos serviços dentro do filtro atual</div></div></div>
        <div class="car-vendor-list">${maint.latestMaintenance.length?maint.latestMaintenance.map(item=>`<div class="car-vendor-row"><div><div class="car-vendor-name">${esc(item.title||carExpenseLabel(item.category))}</div><div class="car-vendor-meta">${[fmtD(item.date),vehicleName(item.vehicleId),item.odometer?`${Math.round(item.odometer).toLocaleString('pt-BR')} km`:'',item.note].filter(Boolean).join(' • ')}</div></div><div class="car-vendor-amt">${fmt(item.amount)}</div></div>`).join(''):'<div class="sync-empty">As próximas manutenções salvas aparecem aqui.</div>'}</div>
      </div>
    </div>`;
  const list=document.getElementById('carList');
  if(list)list.innerHTML=st.events.length?st.events.map(e=>{
    const isFuel=e.type==='fuel';
    const icon=isFuel?'⛽':'🔧';
    const title=isFuel?`${e.fuelType} • ${e.liters.toLocaleString('pt-BR')} L`:(e.title||carExpenseLabel(e.category));
    const meta=[!single?vehicleName(e.vehicleId):'',fmtD(e.date),e.odometer?`${Math.round(e.odometer).toLocaleString('pt-BR')} km`:'',isFuel&&e.pricePerLiter?`${fmt(e.pricePerLiter)}/L`:'',e.note].filter(Boolean).join(' • ');
    return `<div class="car-row">
      <div class="car-ico">${icon}</div>
      <div class="car-info"><div class="car-name">${esc(title)}</div><div class="car-meta">${esc(meta)}</div></div>
      <div class="car-amt">${fmt(e.amount)}</div>
      ${single?`<button class="ib" ${writeActionAttrs('editar veículos')} onclick="openCarVehicleModal('${e.vehicleId}')" title="Editar veículo">🚗</button>`:''}
      <button class="ib del" ${writeActionAttrs('remover registros do carro')} onclick="deleteCarEvent('${e.id}')">🗑️</button>
    </div>`;
  }).join(''):`<div class="empty"><span class="ei">🚗</span><p>Nenhum registro neste filtro.</p></div>`;
}
// SETTINGS
const CHANGELOG_ITEMS=[
  {tag:'4.3 - Dados e importação',items:['Importador CSV com mapeamento assistido, perfis e deduplicação','Importação OFX, texto colado, PDF simples, OCR, Pix e QR/NFC-e em texto','Revisão em lote com estados de nova, duplicada e conciliável','Tela visual de conflito para manter atual, usar importado ou mesclar','Snapshots compactos antes de importar e metadados de conciliação']},
  {tag:'4.1/4.2 - Usabilidade e foco',items:['Cadastro principal de transação exigindo só o valor','Defaults inteligentes para descrição, categoria, data e conta','Busca global com ações diretas e edição inline de vencimentos','Experimentos de inteligência financeira ficaram fora do núcleo estável desta linha']},
  {tag:'Produto',items:['Linha 4.3.1 consolidada como base web + Capacitor + API','Modo local e modo online com login por usuário, senha e 2FA opcional','Navegação desktop/mobile, tema escuro e privacidade de valores']},
  {tag:'Finanças',items:['Transações com filtros, busca, visual compacta, gráficos e edição inline','Entrada rápida por texto com prévia, categoria, conta e recorrência sugeridas','Recorrências, parcelamentos, pendências e lançamentos futuros','Orçamentos por categoria, metas, contas, transferências e rendimento']},
  {tag:'Planejamento',items:['Dashboard com widgets configuráveis, reordenáveis e restauráveis','Comparativo mensal, projeção até o fim do mês e central de pendências','Vencimentos com contas fixas, atrasados, a pagar, a receber e notificações locais']},
  {tag:'Módulos',items:['Lista de compras com múltiplas listas, categorias e progresso','Carro com abastecimentos, manutenções, custo por km, gráficos e importação CSV deduplicada','Busca global cobrindo transações, vencimentos, carro e compras']},
  {tag:'Dados',items:['Backup completo, JSON, CSV, importação com prévia e deduplicação','Fila offline visível, status de conexão e sincronização manual','Backend PostgreSQL multiusuário com estado remoto, resumo financeiro e backup SQL']}
];
function renderChangelogSettings(){
  const el=document.getElementById('settingsChangelog');if(!el)return;
  el.innerHTML=CHANGELOG_ITEMS.map(section=>`
    <div class="changelog-group">
      <div class="changelog-tag">${esc(section.tag)}</div>
      <div class="changelog-lines">${section.items.map(item=>`<div class="changelog-line"><span>✓</span><p>${esc(item)}</p></div>`).join('')}</div>
    </div>
  `).join('');
}
function getChangelogText(){
  return [`Finanza ${APP_VERSION} - Changelog`,...CHANGELOG_ITEMS.flatMap(section=>['',section.tag,...section.items.map(item=>`- ${item}`)])].join('\n');
}
async function copyChangelog(){
  const txt=getChangelogText();
  try{await navigator.clipboard.writeText(txt);toast('Changelog copiado','success');}
  catch{toast('Changelog pronto para copiar no arquivo CHANGELOG-COMPLETO.md','info');}
}
function renderSet(){
  const isDark=document.documentElement.dataset.theme==='dark';
  applyAdminPanelVisibility();
  updateAdminOverview();
  const cdi=document.getElementById('setCDI');if(cdi)cdi.value=RATES.cdi;
  const selic=document.getElementById('setSelic');if(selic)selic.value=RATES.selic;
  const income=document.getElementById('setIncome');if(income)income.value=formatCentsInput(monthlyIncomeCents);
  const tog=document.getElementById('thmTog');if(tog)tog.checked=isDark;
  document.getElementById('setUsr').textContent=cfg.userName||'';
  document.getElementById('setMod').textContent=cfg.mode==='api'?'Online (API + PostgreSQL)':'Local (neste dispositivo)';
  document.getElementById('setUrl').textContent=cfg.url||'Modo local';
  const roleSummary=document.getElementById('setRoleSummary');if(roleSummary)roleSummary.textContent=roleCapabilitySummary();
  const rolePill=document.getElementById('setRolePill');if(rolePill)rolePill.textContent=rolePillLabel();
  const twoFactorStatus=document.getElementById('set2faStatus');if(twoFactorStatus)twoFactorStatus.textContent=cfg.mode!=='api'?'Disponível no modo online':(cfg.twoFactorEnabled?'Ativo para o login desta conta':'Desligado');
  const twoFactorActions=document.getElementById('twoFactorActions');
  if(twoFactorActions)twoFactorActions.innerHTML=cfg.mode!=='api'?'':(cfg.twoFactorEnabled?'<button class="btn btn-d btn-sm" onclick="disableTwoFactor()">Desativar 2FA</button>':'<button class="btn btn-g btn-sm" onclick="beginTwoFactorSetup()">Ativar 2FA</button>');
  const recoveryBox=document.getElementById('recoveryCodeBox');if(recoveryBox&&!recoveryBox.textContent.trim())recoveryBox.style.display='none';
  document.getElementById('setTxCt').textContent=S.transactions.length;
  const loc=cfg.mode==='local';
  const jRow=document.getElementById('setJsonRow');if(jRow)jRow.style.display=loc?'flex':'none';
  const mRow=document.getElementById('setMigRow');if(mRow)mRow.style.display=loc?'flex':'none';
  renderAdminUsers();
  if(canManageUsers()){
    loadAdminOverview().catch(()=>{});
    loadAdminUsers().catch(()=>{});
  }
  renderCatChips();
  renderSidebarShortcutEditor();
  const pJ=document.getElementById('popJsonExp');if(pJ)pJ.style.display=loc?'':'none';
  const pM=document.getElementById('popMig');if(pM)pM.style.display=loc?'':'none';
  updateTrustPanel();
  renderDiag();
  renderChangelogSettings();
  applyWriteAccessUI();
}
function getDiagText(){
  const syncPending=Array.isArray(syncQ)?syncQ.length:0;
  return [
    `Finanza ${APP_VERSION}`,
    `Modo: ${cfg.mode==='api'?'Online':'Local'}`,
    `Usuário: ${cfg.userName||'local'}`,
    `Servidor: ${cfg.url||'modo local'}`,
    `Transações: ${S.transactions.length}`,
    `Contas: ${S.accounts.length}`,
    `Metas: ${S.goals.length}`,
    `Orçamentos: ${S.budgets.length}`,
    `Sync pendente: ${syncPending}`,
    `Tema: ${document.documentElement.dataset.theme||'dark'}`
  ].join('\n');
}
function renderDiag(){
  const syncPending=Array.isArray(syncQ)?syncQ.length:0;
  const v=document.getElementById('diagVersion');if(v)v.textContent=APP_VERSION;
  const m=document.getElementById('diagMode');if(m)m.textContent=cfg.mode==='api'?`Online - ${cfg.url||'sem URL'}`:'Local neste dispositivo';
  const s=document.getElementById('diagSync');if(s)s.textContent=syncPending?`${syncPending} item(ns) pendente(s)`:'Sem pendencias';
  const d=document.getElementById('diagData');if(d)d.textContent=`${S.transactions.length} transações, ${S.accounts.length} contas, ${S.goals.length} metas`;
}
async function copyDiag(){
  const txt=getDiagText();
  try{await navigator.clipboard.writeText(txt);toast('Diagnostico copiado','success');}
  catch{toast(txt.replace(/\n/g,' | '),'info');}
}
// POPOVER
function togglePop(){const p=document.getElementById('acctPop');const c=document.getElementById('popChev');p.classList.toggle('open');c.textContent=p.classList.contains('open')?'▼':'▲';}
function closePop(){document.getElementById('acctPop').classList.remove('open');document.getElementById('popChev').textContent='▲';}
document.addEventListener('click',e=>{
  const btn=document.getElementById('acctBtn');
  const pop=document.getElementById('acctPop');
  if(pop&&!pop.contains(e.target)&&btn&&!btn.contains(e.target))closePop();
  const shortcutMenu=document.getElementById('sidebarShortcutMenu');
  const shortcutCard=e.target.closest('.sidebar-card');
  if(sidebarShortcutMenuOpen&&shortcutMenu&&!shortcutMenu.contains(e.target)&&!shortcutCard)closeSidebarShortcutMenu();
});
// HELPERS
function setTyp(t){document.getElementById('tExp').className='ttb'+(t==='expense'?' active expense':'');document.getElementById('tInc').className='ttb'+(t==='income'?' active income':'');document.getElementById('tExp').dataset.t=t==='expense'?'1':'';document.getElementById('tInc').dataset.t=t==='income'?'1':'';syncTxSplitUI();refreshTxDateHint();}
function getTyp(){return document.getElementById('tExp').dataset.t?'expense':'income';}
function closeM(id){document.getElementById(id).classList.remove('open');}
document.querySelectorAll('.ov').forEach(o=>{let md=null;o.addEventListener('mousedown',e=>{md=e.target;});o.addEventListener('mouseup',e=>{if(e.target===o&&md===o)o.classList.remove('open');md=null;});let td=null;o.addEventListener('touchstart',e=>{td=e.target;},{passive:true});o.addEventListener('touchend',e=>{if(td===o&&e.target===o)o.classList.remove('open');td=null;});});
function toast(msg,type='success'){const el=document.createElement('div');el.className=`toast ${type}`;el.innerHTML=({success:'✅',error:'❌',info:'ℹ️'}[type]||'')+' '+msg;document.getElementById('twrap').appendChild(el);setTimeout(()=>el.remove(),3500);}
function clearCache(){if(!confirm('Limpar cache local?'))return;localStorage.removeItem(LK);toast('Cache limpo. Recarregando...','info');setTimeout(()=>location.reload(),1000);}
// RELOAD
async function doReload(){
  ['rlBtn','rlBtnS'].forEach(id=>{const b=document.getElementById(id);if(b){b.classList.add('spin');b.disabled=true;}});
  setSaveState('syncing','Atualizando dados do servidor');
  let ok=false;
  try{await loadAll();popCatSels();popAccSels();refreshAll();ok=true;toast('Dados atualizados! ✓','success');}
  catch(e){setSaveState('error','Falha ao atualizar dados');logSyncEvent('error','Atualização manual falhou',e.message);toast('Erro: '+e.message,'error');}
  finally{
    if(cfg.mode==='api'&&ok){setSaveState('synced','Atualização manual concluída');logSyncEvent('success','Atualização manual concluída',cfg.url||'servidor');}
    ['rlBtn','rlBtnS'].forEach(id=>{const b=document.getElementById(id);if(b){b.classList.remove('spin');b.disabled=false;}});
  }
}
// PTR
(()=>{
  let sy=0,pulling=false,triggered=false;const th=80;
  const ind=document.getElementById('ptr'),arr=document.getElementById('ptrAr'),txt=document.getElementById('ptrTxt');
  document.addEventListener('touchstart',e=>{if(window.scrollY===0)sy=e.touches[0].clientY;},{passive:true});
  document.addEventListener('touchmove',e=>{if(!sy)return;const dy=e.touches[0].clientY-sy;if(dy>10&&window.scrollY===0){pulling=true;ind.classList.add('show');if(dy>th){triggered=true;arr.classList.add('ready');txt.textContent='Solte para atualizar';}else{triggered=false;arr.classList.remove('ready');txt.textContent='Puxe para atualizar';}}},{passive:true});
  document.addEventListener('touchend',()=>{if(triggered){ind.classList.add('loading');txt.textContent='Atualizando...';doReload().then(()=>setTimeout(()=>{ind.classList.remove('show','loading');arr.classList.remove('ready');txt.textContent='Puxe para atualizar';},400));}else if(pulling)ind.classList.remove('show');sy=0;pulling=false;triggered=false;});
})();
// BACK
(()=>{
  let once=false,timer=null;const bt=document.getElementById('btst');
  history.pushState({fz:true},'','');
  window.addEventListener('popstate',()=>{
    if(pgHist.length>0){showPage(pgHist.pop());history.pushState({fz:true},'','');return;}
    const active=document.querySelector('.page.active')?.id?.replace('page-','');
    if(active&&active!=='dashboard'){showPage('dashboard');history.pushState({fz:true},'','');return;}
    if(once){if(window.Capacitor?.Plugins?.App)window.Capacitor.Plugins.App.exitApp();return;}
    once=true;bt.classList.add('show');history.pushState({fz:true},'','');
    if(timer)clearTimeout(timer);timer=setTimeout(()=>{once=false;bt.classList.remove('show');},2500);
  });
})();
// DEMO
function seedDemo(){
  const y=new Date().getFullYear(),m=String(new Date().getMonth()+1).padStart(2,'0');
  const pm=String(new Date().getMonth()).padStart(2,'0'),py=new Date().getMonth()===0?y-1:y;
  const a1=uid(),a2=uid(),gid=uid(),rid=uid();
  S.accounts=[{id:a1,name:'Principal',icon:'\u{1F3E6}',type:'checking',balance:1500,yieldRate:0,note:''},{id:a2,name:'Poupança',icon:'\u{1F416}',type:'savings',balance:8000,yieldRate:0.55,note:'Rendimento estimado'}];
  const mk=(type,desc,amount,category,date,aId=a1,extra={})=>({id:uid(),type,desc,amount,category,date,note:'',accountId:aId,...{installmentGroup:null,installmentNum:null,installmentTotal:null,recurGroup:null,paid:false,pending:false},...extra});
  S.transactions=[
    mk('income','Salário',5500,'Salário',`${y}-${m}-05`),
    mk('expense','Aluguel',1400,'Moradia',`${y}-${m}-07`),
    mk('expense','Supermercado',480,'Alimentação',`${y}-${m}-10`),
    mk('expense','Uber',95,'Transporte',`${y}-${m}-12`),
    mk('income','Freelance',1200,'Freelance',`${y}-${m}-15`),
    mk('expense','Netflix',55,'Assinaturas',`${y}-${m}-16`),
    mk('expense','Compra rápida',37.5,'A classificar',`${y}-${m}-17`,a1,{pending:true}),
    mk('expense','iPhone (1/4)',750,'Tecnologia',`${y}-${m}-20`,a1,{installmentGroup:gid,installmentNum:1,installmentTotal:4}),
    mk('expense','iPhone (2/4)',750,'Tecnologia',addM(`${y}-${m}-20`,1),a1,{installmentGroup:gid,installmentNum:2,installmentTotal:4}),
    mk('expense','iPhone (3/4)',750,'Tecnologia',addM(`${y}-${m}-20`,2),a1,{installmentGroup:gid,installmentNum:3,installmentTotal:4}),
    mk('expense','iPhone (4/4)',750,'Tecnologia',addM(`${y}-${m}-20`,3),a1,{installmentGroup:gid,installmentNum:4,installmentTotal:4}),
    mk('expense','Aluguel',1400,'Moradia',addM(`${y}-${m}-07`,1),a1,{recurGroup:rid}),
    mk('expense','Aluguel',1400,'Moradia',addM(`${y}-${m}-07`,2),a1,{recurGroup:rid}),
    mk('income','Salário',5500,'Salário',`${py}-${pm}-05`),
    mk('expense','Aluguel',1400,'Moradia',`${py}-${pm}-07`),
    mk('expense','Supermercado',520,'Alimentação',`${py}-${pm}-12`),
    mk('expense','Lazer',180,'Lazer',`${py}-${pm}-20`),
  ];
  S.budgets=[{id:uid(),category:'Alimentação',limit:600},{id:uid(),category:'Moradia',limit:1500},{id:uid(),category:'Transporte',limit:200},{id:uid(),category:'Lazer',limit:300},{id:uid(),category:'Assinaturas',limit:150},{id:uid(),category:'Tecnologia',limit:400}];
  S.goals=[{id:uid(),name:'Viagem Europa',icon:'\u2708\uFE0F',target:12000,current:4500,deadline:`${y+1}-06-01`,desc:'Lisboa e Berlim',monthly:800},{id:uid(),name:'Reserva Emergência',icon:'\u{1F6E1}\uFE0F',target:15000,current:8200,deadline:`${y}-12-31`,desc:'6 meses de despesas',monthly:500},{id:uid(),name:'Notebook Novo',icon:'\u{1F4BB}',target:6000,current:2100,deadline:`${y+1}-03-01`,desc:'Trabalho e estudos',monthly:300}];
  commitmentsState=normalizeCommitmentsState({
    subscriptions:[{name:'Netflix',amount:55,category:'Assinaturas',billingDay:16,renewalDate:addM(`${y}-${m}-16`,1),paymentMethod:'credit',paymentPlace:'Cartão principal',usage:'medium',status:'active',notes:'Streaming principal'}],
    debts:[{name:'Notebook parcelado',totalAmount:3600,outstandingAmount:1800,installmentAmount:300,totalInstallments:12,remainingInstallments:6,interestRate:1.79,nextDueDate:`${y}-${m}-28`,strategy:'snowball',status:'active',notes:'Compra de trabalho'}],
    contracts:[{name:'Internet fibra',kind:'internet',monthlyAmount:120,provider:'Operadora',renewalDate:`${y}-${m}-15`,adjustmentDate:`${y}-${m}-15`,status:'watch',notes:'Checar reajuste anual'}]
  });
  saveLocal();
  saveCommitments(false);
}
// Detect Capacitor and tune the mobile shell.
(function detectCapacitor() {
  const isCapacitor = !!window.Capacitor;
  const isAndroid = /Android/i.test(navigator.userAgent || '');
  document.body.classList.toggle('is-capacitor', isCapacitor);
  document.body.classList.toggle('is-android', isAndroid);
  if (isCapacitor || isAndroid) {
    document.body.classList.add('no-blur', 'mobile-shell');
    const statusH = window.Capacitor?.Plugins?.StatusBar ? 24 : 0;
    document.documentElement.style.setProperty('--status-bar', statusH + 'px');
  }
  const syncViewport = () => {
    document.documentElement.style.setProperty('--app-vh', `${window.innerHeight * 0.01}px`);
  };
  syncViewport();
  window.addEventListener('resize', syncViewport, { passive: true });
  window.addEventListener('orientationchange', () => setTimeout(syncViewport, 250), { passive: true });
})();


// ════════════════════════════════════════════════════════════
// SYNC QUEUE  operaes offline aguardando
// ════════════════════════════════════════════════════════════
let syncQ=[];
function loadSyncQ(){try{syncQ=JSON.parse(localStorage.getItem('fz_syncq')||'[]');}catch{syncQ=[];}}
function saveSyncQ(){localStorage.setItem('fz_syncq',JSON.stringify(syncQ));}
function addToQueue(op){
  syncQ.push({...op,ts:Date.now()});
  saveSyncQ();
  updSyncBadge();
}
function updSyncBadge(){
  const b=document.getElementById('syncBadge');
  const ct=document.getElementById('syncCount');
  if(!b)return;
  if(syncQ.length>0){b.classList.add('visible');if(ct)ct.textContent=syncQ.length;}
  else b.classList.remove('visible');
}
async function syncQueue(){
  if(!syncQ.length){toast('Nada para sincronizar','info');return;}
  if(cfg.mode!=='api'){toast('Conecte ao servidor primeiro','error');return;}
  setSaveState('syncing','Enviando fila offline');
  let ok=0,fail=0;
  for(const op of [...syncQ]){
    try{
      if(op.method==='POST')await api('POST',op.path,op.body);
      else if(op.method==='DELETE')await api('DELETE',op.path);
      else if(op.method==='PUT')await api('PUT',op.path,op.body);
      syncQ=syncQ.filter(q=>q.ts!==op.ts);
      ok++;
    }catch{fail++;}
  }
  saveSyncQ();updSyncBadge();
  if(fail){
    setSaveState('error','Parte da fila offline ainda falhou');
    logSyncEvent('error','Fila offline com falhas',`${ok} enviada(s), ${fail} falha(s)`);
  }else{
    setSaveState('synced','Fila offline enviada com sucesso');
    logSyncEvent('success','Fila offline sincronizada',`${ok} operação(ões) enviadas`);
  }
  updateTrustPanel();
  toast(`Sincronizado: ${ok} OK${fail?', '+fail+' falha(s)':''}`,fail?'error':'success');
}

// ════════════════════════════════════════════════════════════
// CONNECTION BAR  indicador visual offline/online
// ════════════════════════════════════════════════════════════
let _lastConnState='';
function showConnBar(state,msg,duration=3000){
  if(state===_lastConnState&&state==='online')return;
  _lastConnState=state;
  const b=document.getElementById('connBar');
  if(!b)return;
  const icons={online:'✓',offline:'📴',error:'!',syncing:''};
  b.className='conn-toast '+state+' show';
  b.innerHTML=`<span class="conn-ico">${state==='syncing'?'<span class="conn-spin"></span>':icons[state]||''}</span><span>${msg}</span>`;
  if(duration>0)setTimeout(()=>b.classList.remove('show'),duration);
}


// ════════════════════════════════════════════════════════════
// AUTO BACKUP
// ════════════════════════════════════════════════════════════
const BK_KEY='fz_last_backup';
const BK_INTERVAL_DAYS=3;
function checkAutoBackup(){
  updBackupStatus();
}
function autoBackupNow(){
  exportJson();
  localStorage.setItem(BK_KEY,Date.now().toString());
  updBackupStatus();
  noteLocalSave('Backup local atualizado');
  if(parseInt(localStorage.getItem(BK_KEY)||'0')>0)
    toast('Backup automtico salvo ✓','success');
}
function updBackupStatus(){
  const el=document.getElementById('backupStatusTxt');
  const dot=document.getElementById('backupDot');
  const last=parseInt(localStorage.getItem(BK_KEY)||'0');
  if(!el)return;
  if(!last){el.textContent='Nunca feito';if(dot)dot.className='backup-dot warn';return;}
  const days=Math.floor((Date.now()-last)/(1000*60*60*24));
  el.textContent=days===0?'Hoje':days===1?'Ontem':`H ${days} dias`;
  if(dot)dot.className='backup-dot'+(days>=BK_INTERVAL_DAYS?' warn':'');
}

// ════════════════════════════════════════════════════════════
// COMPARATIVO MENSAL
// ════════════════════════════════════════════════════════════
function renderMonthCompare(){
  const el=document.getElementById('monthCompare');
  if(!el)return;
  const cur=curDt;
  const prev=new Date(cur.getFullYear(),cur.getMonth()-1,1);
  const txCur=getMonthTx(cur).filter(t=>!isFut(t.date)&&!t.paid);
  const txPrev=getMonthTx(prev).filter(t=>!isFut(t.date)&&!t.paid);
  const sum=(txs,type)=>txs.filter(t=>t.type===type).reduce((s,t)=>s+t.amount,0);
  const cInc=sum(txCur,'income'),cExp=sum(txCur,'expense');
  const pInc=sum(txPrev,'income'),pExp=sum(txPrev,'expense');
  const cSav=cInc-cExp, pSav=pInc-pExp;
  const cCount=txCur.filter(t=>t.type==='expense').length;
  const pCount=txPrev.filter(t=>t.type==='expense').length;
  const cAvg=cCount?cExp/cCount:0;
  const pAvg=pCount?pExp/pCount:0;
  const cDaily=cExp/(monthBounds(cur).days||1);
  const pDaily=pExp/(monthBounds(prev).days||1);
  const curCatMap=new Map();
  txCur.filter(t=>t.type==='expense').forEach(t=>curCatMap.set(t.category,(curCatMap.get(t.category)||0)+t.amount));
  const prevCatMap=new Map();
  txPrev.filter(t=>t.type==='expense').forEach(t=>prevCatMap.set(t.category,(prevCatMap.get(t.category)||0)+t.amount));
  const topCur=[...curCatMap.entries()].sort((a,b)=>b[1]-a[1])[0]||null;
  const topPrev=[...prevCatMap.entries()].sort((a,b)=>b[1]-a[1])[0]||null;
  const delta=(cur,prev)=>{
    if(!prev)return{pct:null,cls:'neu'};
    const pct=Math.round(((cur-prev)/prev)*100);
    return{pct,cls:pct>0?'up':'dn',str:(pct>0?'▲':'▼')+Math.abs(pct)+'%'};
  };
  const dExp=delta(cExp,pExp);
  const dInc=delta(cInc,pInc);
  const dSav=delta(cSav,pSav);
  const dAvg=delta(cAvg,pAvg);
  const dDaily=delta(cDaily,pDaily);
  el.innerHTML=`
    <div class="month-compare month-compare-dense">
      <div class="mc-card">
        <div class="mc-label">Receitas</div>
        <div class="mc-val" style="color:var(--ac)">${fmt(cInc)}</div>
        <div class="mc-delta ${dInc.cls}">${dInc.pct!==null?dInc.str+' vs mês ant.':'primeiro mês'}</div>
      </div>
      <div class="mc-card">
        <div class="mc-label">Despesas</div>
        <div class="mc-val" style="color:var(--dan)">${fmt(cExp)}</div>
        <div class="mc-delta ${dExp.cls==='up'?'up':'dn'}">${dExp.pct!==null?dExp.str+' vs mês ant.':'primeiro mês'}</div>
      </div>
      <div class="mc-card">
        <div class="mc-label">Economizado</div>
        <div class="mc-val" style="color:${cSav>=0?'var(--ac2)':'var(--dan)'}">${fmt(cSav)}</div>
        <div class="mc-delta ${dSav.cls}">${dSav.pct!==null?dSav.str+' vs mês ant.':''}</div>
      </div>
      <div class="mc-card">
        <div class="mc-label">Ticket médio</div>
        <div class="mc-val">${cAvg?fmt(cAvg):'—'}</div>
        <div class="mc-delta ${dAvg.cls}">${dAvg.pct!==null?dAvg.str+' por despesa':'sem base anterior'}</div>
      </div>
      <div class="mc-card">
        <div class="mc-label">Ritmo diário</div>
        <div class="mc-val">${fmt(cDaily)}</div>
        <div class="mc-delta ${dDaily.cls==='up'?'up':'dn'}">${dDaily.pct!==null?dDaily.str+' por dia':'sem base anterior'}</div>
      </div>
      <div class="mc-card">
        <div class="mc-label">Categoria dominante</div>
        <div class="mc-val mc-text">${topCur?esc(topCur[0]):'—'}</div>
        <div class="mc-delta neu">${topCur?`${fmt(topCur[1])} no mês atual`:'sem despesas'}</div>
      </div>
      <div class="mc-card">
        <div class="mc-label">Mês anterior</div>
        <div class="mc-val">${fmt(pExp)}</div>
        <div class="mc-delta neu">${topPrev?`${esc(topPrev[0])} liderou com ${fmt(topPrev[1])}`:'sem histórico'}</div>
      </div>
    </div>`;
}

// ════════════════════════════════════════════════════════════
// PROJEO DE SALDO
// ════════════════════════════════════════════════════════════
function renderProjection(){
  const el=document.getElementById('projCard');
  if(!el)return;
  const now=new Date();
  const {from,to,days}=monthBounds(curDt);
  const isCurrentMonth=curDt.getMonth()===now.getMonth()&&curDt.getFullYear()===now.getFullYear();
  const dayOfMonth=isCurrentMonth?now.getDate():days;
  const currentMonthTx=S.transactions.filter(t=>t.date>=from&&t.date<=to&&!isFut(t.date)&&!t.paid);
  const curInc=currentMonthTx.filter(t=>t.type==='income').reduce((s,t)=>s+t.amount,0);
  const curExp=currentMonthTx.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amount,0);
  const paceInc=dayOfMonth?curInc/dayOfMonth:0;
  const paceExp=dayOfMonth?curExp/dayOfMonth:0;
  const projectedInc=paceInc*days;
  const projectedExp=paceExp*days;
  const projectedBalance=projectedInc-projectedExp;
  const dueThisMonth=dueOccurrences(to).filter(o=>o.date>=today()).reduce((s,o)=>s+o.item.amount,0);
  // Média dos últimos 3 meses
  let totalInc=0,totalExp=0,cnt=0;
  const monthlyHistory=[];
  for(let i=1;i<=3;i++){
    const d=new Date(curDt.getFullYear(),curDt.getMonth()-i,1);
    const txs=getMonthTx(d).filter(t=>!isFut(t.date)&&!t.paid);
    const inc=txs.filter(t=>t.type==='income').reduce((s,t)=>s+t.amount,0);
    const exp=txs.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amount,0);
    if(inc>0||exp>0){totalInc+=inc;totalExp+=exp;cnt++;monthlyHistory.push({inc,exp,sav:inc-exp});}
  }
  if(!cnt&&!currentMonthTx.length){el.innerHTML=`<div class="proj-card"><div class="proj-icon">🔭</div><div class="proj-info"><div class="proj-title">Projeção em preparação</div><div class="proj-detail">Depois de alguns lançamentos, este balão mostra tendência mensal e quanto sobra ou falta nos próximos meses.</div></div></div>`;return;}
  const avgInc=totalInc/cnt,avgExp=totalExp/cnt,avgSav=avgInc-avgExp;
  const curBal=S.accounts.reduce((s,a)=>s+getAccBal(a.id),0);
  const proj3=curBal+(avgSav*3);
  const proj6=curBal+(avgSav*6);
  const trend=avgSav>=0?'positiva':'negativa';
  const trendColor=avgSav>=0?'var(--ac2)':'var(--dan)';
  const daysLeft=Math.max(days-dayOfMonth,0);
  const remainingThisMonth=projectedBalance-dueThisMonth;
  const avgMonthlyDue=cnt?monthlyHistory.reduce((s,m)=>s+m.exp,0)/cnt:curExp;
  const conservativeSav=Math.min(avgSav,projectedBalance-(dueThisMonth*0.7));
  const optimisticSav=Math.max(avgSav,projectedBalance+(Math.max(0,projectedInc-curInc)*0.15));
  const projected90Base=curBal+(avgSav*3);
  const projected90Conservative=curBal+(conservativeSav*3);
  const projected90Optimistic=curBal+(optimisticSav*3);
  const emergencyDays=paceExp>0?Math.floor(curBal/paceExp):null;
  const nextThreeMonths=Array.from({length:3},(_,i)=>{
    const d=new Date(curDt.getFullYear(),curDt.getMonth()+i+1,1);
    const bounds=monthBounds(d);
    const expectedDue=dueOccurrences(bounds.to).filter(o=>o.date>=bounds.from&&o.date<=bounds.to).reduce((s,o)=>s+o.item.amount,0);
    const expectedIncome=avgInc||projectedInc||curInc;
    const expectedExpense=Math.max(avgExp||projectedExp||curExp,expectedDue);
    return {label:`${String(d.getMonth()+1).padStart(2,'0')}/${String(d.getFullYear()).slice(-2)}`,income:expectedIncome,expense:expectedExpense,balance:expectedIncome-expectedExpense};
  });
  el.innerHTML=`
    <div class="proj-card">
      <div class="proj-icon">🔭</div>
      <div class="proj-info">
        <div class="proj-title">Projeção de saldo</div>
        <div class="proj-detail">
          Tendência <strong style="color:${trendColor}">${trend}</strong>
          média mensal de <strong style="color:${avgSav>=0?'var(--ac)':'var(--dan)'}">${fmt(Math.abs(avgSav))}</strong>
          ${avgSav>=0?'economizados':'de dficit'}<br>
          Em 3 meses: <strong>${fmt(proj3)}</strong> &nbsp;&nbsp; Em 6 meses: <strong>${fmt(proj6)}</strong><br>
          Fechamento estimado do mês: <strong style="color:${projectedBalance>=0?'var(--ac)':'var(--dan)'}">${fmt(projectedBalance)}</strong> • contas/vencimentos restantes: <strong>${fmt(dueThisMonth)}</strong>
        </div>
        <div class="proj-grid">
          <div class="proj-chip">
            <span class="proj-chip-k">Ritmo diário</span>
            <strong class="${paceExp<=paceInc?'pos':'neg'}">${fmt(paceExp)}</strong>
            <small>${daysLeft} dia${daysLeft===1?'':'s'} restantes no mês</small>
          </div>
          <div class="proj-chip">
            <span class="proj-chip-k">Folga após vencimentos</span>
            <strong class="${remainingThisMonth>=0?'pos':'neg'}">${fmt(remainingThisMonth)}</strong>
            <small>fechamento do mês descontando compromissos</small>
          </div>
          <div class="proj-chip">
            <span class="proj-chip-k">Reserva em dias</span>
            <strong>${emergencyDays!==null?`${emergencyDays}d`:'—'}</strong>
            <small>quanto o saldo atual sustenta no ritmo atual</small>
          </div>
        </div>
        <div class="proj-scenarios">
          <div class="proj-scenario">
            <span>Conservador</span>
            <strong class="${projected90Conservative>=curBal?'pos':'neg'}">${fmt(projected90Conservative)}</strong>
          </div>
          <div class="proj-scenario">
            <span>Base 90 dias</span>
            <strong class="${projected90Base>=curBal?'pos':'neg'}">${fmt(projected90Base)}</strong>
          </div>
          <div class="proj-scenario">
            <span>Otimista</span>
            <strong class="${projected90Optimistic>=curBal?'pos':'neg'}">${fmt(projected90Optimistic)}</strong>
          </div>
        </div>
        <div class="proj-horizon">
          ${nextThreeMonths.map(item=>`<div class="proj-horizon-row"><span>${item.label}</span><span>${fmt(item.income)}</span><span>${fmt(item.expense)}</span><strong class="${item.balance>=0?'pos':'neg'}">${fmt(item.balance)}</strong></div>`).join('')}
        </div>
      </div>
    </div>`;
}

// ════════════════════════════════════════════════════════════
// ALERTAS DE ORAMENTO
// ════════════════════════════════════════════════════════════
function renderBudAlerts(){
  const el=document.getElementById('budAlerts');
  if(!el||!S.budgets.length){if(el)el.innerHTML='';return;}
  const now=new Date();
  const alerts=[];
  S.budgets.forEach(b=>{
    const spent=S.transactions.filter(t=>{
      if(t.type!=='expense'||isFut(t.date)||t.paid)return false;
      const d=new Date(t.date+'T12:00:00');
      return d.getMonth()===now.getMonth()&&d.getFullYear()===now.getFullYear()&&t.category===b.category;
    }).reduce((s,t)=>s+t.amount,0);
    const pct=(spent/b.limit)*100;
    if(pct>=80)alerts.push({cat:b.category,spent,limit:b.limit,pct,over:pct>=100});
  });
  if(!alerts.length){el.innerHTML='';return;}
  el.innerHTML=alerts.map(a=>{
    const cat=getCat(a.cat);
    const cls=a.over?'bud-alert bud-alert-danger':'bud-alert';
    const msg=a.over
      ?`${cat.ico} <strong>${a.cat}</strong> excedeu o limite  gastou <strong>${fmt(a.spent)}</strong> de ${fmt(a.limit)}`
      :`${cat.ico} <strong>${a.cat}</strong> em <strong>${Math.round(a.pct)}%</strong> do orçamento (${fmt(a.spent)} de ${fmt(a.limit)})`;
    return`<div class="${cls}">⚠️ <span>${msg}</span></div>`;
  }).join('');
}

// ════════════════════════════════════════════════════════════
// CENTRO DE INTELIGÊNCIA 4.2
// ════════════════════════════════════════════════════════════
function monthStatsFor(date){
  const txs=getMonthTx(date).filter(t=>!isFut(t.date)&&!t.paid);
  const income=txs.filter(t=>t.type==='income').reduce((s,t)=>s+t.amount,0);
  const expense=txs.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amount,0);
  const catMap=new Map();
  txs.filter(t=>t.type==='expense').forEach(t=>catMap.set(t.category,(catMap.get(t.category)||0)+t.amount));
  return {date,txs,income,expense,balance:income-expense,cats:[...catMap.entries()].sort((a,b)=>b[1]-a[1])};
}
function recentMonthStats(count=12,offset=0){
  return Array.from({length:count},(_,i)=>monthStatsFor(new Date(curDt.getFullYear(),curDt.getMonth()-i-offset,1))).filter(m=>m.income||m.expense);
}
function average(values){
  return values.length?values.reduce((s,v)=>s+v,0)/values.length:0;
}
function detectSubscriptions(months){
  const map=new Map();
  months.flatMap(m=>m.txs).filter(t=>t.type==='expense').forEach(t=>{
    const key=normalizeTxText(t.desc).replace(/\s+\(\d+\/\d+\)$/,'').slice(0,28)+'|'+t.category;
    const cur=map.get(key)||{desc:t.desc.replace(/\s+\(\d+\/\d+\)$/,''),category:t.category,amounts:[],months:new Set(),last:t.date,recur:!!t.recurGroup};
    cur.amounts.push(t.amount);
    cur.months.add(t.date.slice(0,7));
    if(t.date>cur.last)cur.last=t.date;
    cur.recur=cur.recur||!!t.recurGroup||normalizeTxText(t.category).includes('assin');
    map.set(key,cur);
  });
  return [...map.values()]
    .map(x=>({...x,avg:average(x.amounts),count:x.months.size}))
    .filter(x=>x.count>=2||x.recur)
    .sort((a,b)=>b.avg-a.avg)
    .slice(0,6);
}
// ════════════════════════════════════════════════════════════
// EDIO INLINE
// ════════════════════════════════════════════════════════════
let _editingInline=null;
function openInlineEdit(id){
  if(!requireWriteAccess('editar transações'))return;
  // Fecha se j aberto
  if(_editingInline===id){closeInlineEdit();return;}
  closeInlineEdit();
  _editingInline=id;
  const tx=S.transactions.find(t=>t.id===id);
  if(!tx)return;
  const wrap=document.getElementById('ie-'+id);
  if(!wrap)return;
  wrap.classList.add('open');
  wrap.innerHTML=`
    <div class="ti-edit-row">
      <input class="ti-edit-inp desc" id="ie-desc-${id}" value="${tx.desc.replace(/"/g,'&quot;')}" placeholder="Descrição">
      <input class="ti-edit-inp amount" id="ie-amt-${id}" type="number" value="${tx.amount}" step="0.01" min="0.01">
      <input class="ti-edit-inp" id="ie-dt-${id}" type="date" value="${tx.date}" style="width:130px">
      <button class="btn btn-p btn-sm" onclick="saveInlineEdit('${id}')">✓</button>
      <button class="btn btn-g btn-sm" onclick="closeInlineEdit()">✕</button>
    </div>`;
  setTimeout(()=>document.getElementById('ie-amt-'+id)?.focus(),50);
}
function closeInlineEdit(){
  if(!_editingInline)return;
  const wrap=document.getElementById('ie-'+_editingInline);
  if(wrap){wrap.classList.remove('open');wrap.innerHTML='';}
  _editingInline=null;
}
async function saveInlineEdit(id){
  if(!requireWriteAccess('editar transações'))return;
  const tx=S.transactions.find(t=>t.id===id);
  if(!tx)return;
  const desc=document.getElementById('ie-desc-'+id)?.value.trim()||tx.desc;
  const amount=parseFloat(document.getElementById('ie-amt-'+id)?.value)||tx.amount;
  const date=document.getElementById('ie-dt-'+id)?.value||tx.date;
  if(amount<=0){toast('Valor invlido','error');return;}
  tx.desc=desc;tx.amount=amount;tx.date=date;
  if(cfg.mode==='api'){
    try{await api('PUT',`/api/transactions/${id}`,{type:tx.type,description:desc,amount,category:tx.category,date,purchase_date:tx.purchaseDate||date,note:tx.note||'',account_id:tx.accountId,paid:tx.paid,pending:tx.pending});}
    catch(e){toast('Erro: '+e.message,'error');return;}
  } else saveLocal();
  closeInlineEdit();
  refreshAll();
  toast('Atualizado ✓','success');
}


// ════════════════════════════════════════════════════════════
// NOTIFICAES LOCAIS (Capacitor)
// ════════════════════════════════════════════════════════════
async function initNotifications() {
  if (!window.Capacitor) return;
  try {
    const { LocalNotifications } = Capacitor.Plugins;
    if (!LocalNotifications) return;

    const perm = await LocalNotifications.requestPermissions();
    if (perm.display !== 'granted') return;

    // Cancela notificaes antigas antes de reagendar
    await LocalNotifications.cancel({ notifications: [{ id: 1 }, { id: 2 }, { id: 3 }] }).catch(() => {});

    scheduleVencimentoNotifications();
  } catch (e) {
    console.warn('Notificações não disponíveis:', e.message);
  }
}

async function scheduleVencimentoNotifications() {
  if (!window.Capacitor?.Plugins?.LocalNotifications) return;
  const { LocalNotifications } = Capacitor.Plugins;

  const hoje = today();
  const em3d = offD(new Date(), 3);
  const em7d = offD(new Date(), 7);

  const txProximas = S.transactions.filter(t =>
    t.type === 'expense' && !t.paid && t.date > hoje && t.date <= em7d
  ).map(t=>({date:t.date,amount:t.amount,name:t.desc}));
  const dueProximas = dueOccurrences(em7d).map(o=>({date:o.date,amount:o.item.amount,name:o.item.name}));
  const proximas = [...txProximas,...dueProximas].sort((a, b) => a.date.localeCompare(b.date));

  if (!proximas.length) return;

  const notifs = [];
  const agora = new Date();

  // Notificação diária às 9h se tiver contas a vencer hoje ou amanhã
  const urgentes = proximas.filter(t => t.date <= offD(new Date(), 1));
  if (urgentes.length) {
    const total = urgentes.reduce((s, t) => s + t.amount, 0);
    const schedDate = new Date();
    schedDate.setHours(9, 0, 0, 0);
    if (schedDate <= agora) schedDate.setDate(schedDate.getDate() + 1);
    notifs.push({
      id: 1,
      title: '⚠️ Contas vencendo',
      body: `${urgentes.length} conta${urgentes.length > 1 ? 's' : ''} a vencer: ${fmt(total)}`,
      schedule: { at: schedDate, repeats: false },
      sound: 'default',
      smallIcon: 'ic_stat_icon_config_sample',
    });
  }

  // Resumo semanal: domingo s 10h
  const domingo = new Date();
  domingo.setDate(domingo.getDate() + (7 - domingo.getDay()) % 7 || 7);
  domingo.setHours(10, 0, 0, 0);
  const totalSem = proximas.reduce((s, t) => s + t.amount, 0);
  notifs.push({
    id: 2,
    title: '📅 Finanza  semana',
    body: `${proximas.length} conta${proximas.length > 1 ? 's' : ''} nos próximos 7 dias: ${fmt(totalSem)}`,
    schedule: { at: domingo, repeats: false },
    sound: 'default',
    smallIcon: 'ic_stat_icon_config_sample',
  });

  try {
    await LocalNotifications.schedule({ notifications: notifs });
    console.log('✅ Notificações agendadas:', notifs.length);
  } catch (e) {
    console.warn('Erro ao agendar notificaes:', e.message);
  }
}

// ════════════════════════════════════════════════════════════
// DEEP LINK  shortcut "quick-add" abre o FAB direto
// ════════════════════════════════════════════════════════════
function initDeepLink() {
  if (!window.Capacitor) return;
  try {
    const { App } = Capacitor.Plugins;
    if (!App) return;

    // Ao abrir o app via shortcut
    App.addListener('appUrlOpen', (data) => {
      if (data.url?.includes('quick-add')) {
        // Navega para transações e abre o FAB
        setTimeout(() => {
          showPage('transactions');
          setTimeout(() => toggleQA(), 300);
        }, 500);
      }
    });

    // Verifica se foi aberto via shortcut na inicializao
    App.getLaunchUrl().then((data) => {
      if (data?.url?.includes('quick-add')) {
        setTimeout(() => {
          showPage('transactions');
          setTimeout(() => toggleQA(), 600);
        }, 1000);
      }
    }).catch(() => {});
  } catch (e) {
    console.warn('Deep link não disponível:', e.message);
  }
}


// ════════════════════════════════════════════════════════════
// SISTEMA DE WIDGETS DO DASHBOARD
// ════════════════════════════════════════════════════════════
const WIDGETS_KEY = 'fz_widgets';
const MIN_DASH_WIDGETS = 3;
const PINNED_WIDGET_IDS = ['workbench'];
const FOCUSED_WIDGET_IDS = ['workbench','cards','commitments','quickactions','budalerts','renewals'];
const DEFAULT_DASH_WIDGET_ORDER = ['workbench','cards','commitments','budalerts','quickactions','dailyweek','dailymonth','dailyyear','renewals','recent','accounts','budgets','goals','shopping','vehicles','compare','charts','barcats','ministats','saverate','projection','weekly','anomaly'];

// Definição de todos os widgets disponíveis
const WIDGET_DEFS = [
  { id:'cards',     ico:'💳', name:'Resumo do dia a dia',  desc:'Salário, gastos, sobra e a pagar',   default:true,  group:'core' },
  { id:'quickactions', ico:'⚡', name:'Ações rápidas',      desc:'Atalhos úteis para o dia a dia',     default:true,  group:'core' },
  { id:'workbench', ico:'🧭', name:'Atalhos do Finanza',    desc:'Porta de entrada para áreas complementares', default:true, group:'support' },
  { id:'recent',    ico:'💸', name:'Últimas transações',    desc:'Lançamentos recentes',               default:false, group:'core' },
  { id:'budalerts', ico:'⚠️', name:'Alertas de orçamento',  desc:'Riscos que pedem decisão hoje',      default:true,  group:'core' },
  { id:'commitments', ico:'📦', name:'Compromissos fixos',  desc:'Assinaturas, dívidas e contratos',   default:true,  group:'support' },
  { id:'dailyweek', ico:'🗓️', name:'Ritmo semanal',        desc:'Semana: totais, média e livre',       default:true,  group:'core' },
  { id:'dailymonth',ico:'📆', name:'Ritmo mensal',         desc:'Mês: totais, média e livre',          default:true,  group:'core' },
  { id:'dailyyear', ico:'📅', name:'Ritmo anual',          desc:'Ano: total ganho, gasto e média',     default:true,  group:'core' },
  { id:'renewals',  ico:'🔔', name:'Renovações próximas',   desc:'Alertas de reajuste e vencimento',   default:true,  group:'support' },
  { id:'accounts',  ico:'🏦', name:'Saldos das contas',     desc:'Saldo de cada conta bancária',       default:false, group:'support' },
  { id:'budgets',   ico:'🎯', name:'Orçamentos rápidos',    desc:'Uso mensal por categoria',            default:false, group:'support' },
  { id:'goals',     ico:'🏆', name:'Metas rápidas',         desc:'Progresso das suas metas',            default:false, group:'support' },
  { id:'shopping',  ico:'🛒', name:'Lista de compras',      desc:'Itens pendentes da lista ativa',     default:false, group:'support' },
  { id:'vehicles',  ico:'🚗', name:'Veículos',              desc:'Resumo do carro ativo e manutenção', default:false, group:'support' },
  { id:'compare',   ico:'📅', name:'Comparativo mensal',    desc:'Este mês vs mês anterior',           default:false, group:'analysis' },
  { id:'ministats', ico:'📈', name:'Mini estatísticas',     desc:'Média diária, maior gasto, dias',    default:false, group:'analysis' },
  { id:'barcats',   ico:'📉', name:'Ranking de gastos',     desc:'Top categorias em barras',           default:false, group:'analysis' },
  { id:'saverate',  ico:'💹', name:'Taxa de economia',      desc:'Quanto sobra das receitas',          default:false, group:'analysis' },
  { id:'charts',    ico:'📊', name:'Gráficos',              desc:'Fluxo de caixa e categorias',         default:false, group:'analysis' },
  { id:'projection',ico:'🔭', name:'Dica de projeção',      desc:'Tendência dos próximos meses',        default:false, group:'analysis' },
  { id:'weekly',    ico:'📆', name:'Dica da semana',        desc:'Gastos e economia da semana',         default:false, group:'analysis' },
  { id:'anomaly',   ico:'💡', name:'Dica fora da curva',    desc:'Categorias acima da média',           default:false, group:'analysis' },
];

let widgetPrefs = {};
let widgetOrder = [];
let widgetFilters = {};
let sidebarShortcutPrefs = [];
let sidebarShortcutMenuOpen = false;
let dashboardManagerOpen = false;
let activeWidgetMenuId = '';
let dashboardDragId = '';
let dashDirty = true;
const SIDEBAR_SHORTCUT_DEFS = [
  {id:'commitments',label:'Compromissos',icon:'📦',page:'commitments'},
  {id:'budget',label:'Limites',icon:'🎯',page:'budget'},
  {id:'shared',label:'Acertos',icon:'🤝',page:'shared'},
  {id:'shopping',label:'Compras',icon:'🛒',page:'shopping'},
  {id:'goals',label:'Metas',icon:'🏆',page:'goals'},
  {id:'accounts',label:'Contas',icon:'🏦',page:'accounts'},
  {id:'car',label:'Carro',icon:'🚗',page:'car'},
  {id:'future',label:'Vencimentos',icon:'📌',page:'future'}
];
const DEFAULT_SIDEBAR_SHORTCUTS = ['commitments','budget','shared','shopping','goals','accounts'];

function defaultDashboardWidgetOrder(){
  const ids=WIDGET_DEFS.map(w=>w.id);
  return [...DEFAULT_DASH_WIDGET_ORDER.filter(id=>ids.includes(id)),...ids.filter(id=>!DEFAULT_DASH_WIDGET_ORDER.includes(id))];
}
function sidebarShortcutById(id){
  return SIDEBAR_SHORTCUT_DEFS.find(item=>item.id===id)||SIDEBAR_SHORTCUT_DEFS[0];
}
function normalizeSidebarShortcuts(raw=[]){
  const ids=SIDEBAR_SHORTCUT_DEFS.map(item=>item.id);
  const picked=asArr(raw).map(String).filter(id=>ids.includes(id));
  const result=[];
  for(let i=0;i<DEFAULT_SIDEBAR_SHORTCUTS.length;i++){
    const preferred=picked[i];
    if(preferred&&!result.includes(preferred))result.push(preferred);
    else{
      const fallback=DEFAULT_SIDEBAR_SHORTCUTS.find(id=>!result.includes(id))||ids.find(id=>!result.includes(id))||DEFAULT_SIDEBAR_SHORTCUTS[i];
      result.push(fallback);
    }
  }
  return result;
}
function loadSidebarShortcutPrefs(){
  try{sidebarShortcutPrefs=normalizeSidebarShortcuts(JSON.parse(localStorage.getItem(SIDEBAR_SHORTCUTS_KEY)||'[]'));}catch{sidebarShortcutPrefs=normalizeSidebarShortcuts();}
}
function saveSidebarShortcutPrefs(persistRemote=true){
  sidebarShortcutPrefs=normalizeSidebarShortcuts(sidebarShortcutPrefs);
  localStorage.setItem(SIDEBAR_SHORTCUTS_KEY,JSON.stringify(sidebarShortcutPrefs));
  renderSidebarShortcuts();
  if(persistRemote&&cfg.mode==='api')saveRemoteState().catch(()=>{});
}
function renderSidebarShortcuts(){
  const el=document.getElementById('sidebarShortcuts');
  if(!el)return;
  el.innerHTML=sidebarShortcutPrefs.map(id=>{
    const item=sidebarShortcutById(id);
    return `<button class="sidebar-icon-btn" onclick="showPage('${item.page}')" title="${esc(item.label)}" aria-label="${esc(item.label)}">${item.icon}</button>`;
  }).join('');
}
function renderSidebarShortcutEditor(){
  const el=document.getElementById('sidebarShortcutMenu');
  if(!el)return;
  el.classList.toggle('open',sidebarShortcutMenuOpen);
  el.innerHTML=`<div class="sidebar-shortcut-menu-head"><div><strong>Editar atalhos</strong><span>Escolha o que fica sempre visível.</span></div><button class="btn btn-g btn-sm" onclick="closeSidebarShortcutMenu()">Fechar</button></div><div class="sidebar-shortcut-editor">${sidebarShortcutPrefs.map((currentId,idx)=>`<label class="sidebar-shortcut-slot"><span>Atalho ${idx+1}</span><select class="fi sel" onchange="setSidebarShortcut(${idx},this.value)">${SIDEBAR_SHORTCUT_DEFS.map(item=>`<option value="${item.id}" ${item.id===currentId?'selected':''}>${item.icon} ${esc(item.label)}</option>`).join('')}</select></label>`).join('')}</div>`;
}
function setSidebarShortcut(index,id){
  const nextId=String(id||'');
  const existingIndex=sidebarShortcutPrefs.findIndex((item,idx)=>item===nextId&&idx!==index);
  if(existingIndex>=0)[sidebarShortcutPrefs[existingIndex],sidebarShortcutPrefs[index]]=[sidebarShortcutPrefs[index],sidebarShortcutPrefs[existingIndex]];
  else sidebarShortcutPrefs[index]=nextId;
  saveSidebarShortcutPrefs();
  renderSidebarShortcutEditor();
  toast('Atalhos atualizados','success');
}
function toggleSidebarShortcutMenu(){
  sidebarShortcutMenuOpen=!sidebarShortcutMenuOpen;
  renderSidebarShortcutEditor();
}
function closeSidebarShortcutMenu(){
  if(!sidebarShortcutMenuOpen)return;
  sidebarShortcutMenuOpen=false;
  renderSidebarShortcutEditor();
}
function isPinnedWidget(id){
  return PINNED_WIDGET_IDS.includes(id);
}
function enforcePinnedWidgets(){
  const ids=WIDGET_DEFS.map(w=>w.id);
  PINNED_WIDGET_IDS.forEach(id=>{
    if(ids.includes(id))widgetPrefs[id]=true;
  });
  widgetOrder=[...PINNED_WIDGET_IDS.filter(id=>ids.includes(id)),...widgetOrder.filter(id=>ids.includes(id)&&!PINNED_WIDGET_IDS.includes(id))];
}

function loadWidgetPrefs() {
  try {
    const s = localStorage.getItem(WIDGETS_KEY);
    widgetPrefs = asObj(s ? JSON.parse(s) : {});
  } catch { widgetPrefs = {}; }
  // Aplica defaults para widgets sem preferncia salva
  WIDGET_DEFS.forEach(w => {
    if (widgetPrefs[w.id] === undefined) widgetPrefs[w.id] = w.default;
  });
  try{widgetOrder=asArr(JSON.parse(localStorage.getItem(WIDGET_ORDER_KEY)||'[]'));}catch{widgetOrder=[];}
  try{widgetFilters=asObj(JSON.parse(localStorage.getItem(WIDGET_FILTER_KEY)||'{}'));}catch{widgetFilters={};}
  const ids=WIDGET_DEFS.map(w=>w.id);
  const fallbackOrder=defaultDashboardWidgetOrder();
  widgetOrder=[...widgetOrder.filter(id=>ids.includes(id)),...fallbackOrder.filter(id=>!widgetOrder.includes(id))];
  enforcePinnedWidgets();
  ensureAtLeastOneWidget();
}

function saveWidgetPrefs() {
  localStorage.setItem(WIDGETS_KEY, JSON.stringify(widgetPrefs));
  if(cfg.mode==='api')saveRemoteState().catch(()=>{});
}
function saveWidgetOrder(){localStorage.setItem(WIDGET_ORDER_KEY,JSON.stringify(widgetOrder));if(cfg.mode==='api')saveRemoteState().catch(()=>{});}
function saveWidgetFilters(){localStorage.setItem(WIDGET_FILTER_KEY,JSON.stringify(widgetFilters));if(cfg.mode==='api')saveRemoteState().catch(()=>{});}
function isWidgetOn(id) {
  return widgetPrefs[id] !== false;
}
function activeWidgetCount(){
  return WIDGET_DEFS.filter(w=>isWidgetOn(w.id)).length;
}
function widgetById(id){
  return WIDGET_DEFS.find(w=>w.id===id)||null;
}
function widgetGroupLabel(group){
  if(group==='core')return 'Essencial';
  if(group==='support')return 'Apoio';
  return 'Análise';
}
function getWidgetFilter(widget,key,fallback){
  const current=asObj(widgetFilters[widget]);
  return current[key] ?? fallback;
}
function currentPageId(){
  return document.querySelector('.page.active')?.id?.replace('page-','')||'dashboard';
}
function isDashboardActive(){
  return currentPageId()==='dashboard';
}
function ensureAtLeastOneWidget(){
  if(activeWidgetCount()>=MIN_DASH_WIDGETS)return;
  WIDGET_DEFS.filter(w=>w.default||w.id==='cards').some(w=>{
    widgetPrefs[w.id]=true;
    return activeWidgetCount()>=MIN_DASH_WIDGETS;
  });
  WIDGET_DEFS.some(w=>{
    if(activeWidgetCount()>=MIN_DASH_WIDGETS)return true;
    widgetPrefs[w.id]=true;
    return false;
  });
}

function toggleWidget(id) {
  if(isPinnedWidget(id)){
    toast('Esse atalho fica fixo no topo da dashboard','info');
    return;
  }
  const willDisable=isWidgetOn(id);
  if(willDisable&&activeWidgetCount()<=MIN_DASH_WIDGETS){
    toast(`Mantenha pelo menos ${MIN_DASH_WIDGETS} widgets na dashboard`,'info');
    return;
  }
  widgetPrefs[id] = !willDisable;
  if(willDisable&&activeWidgetMenuId===id)activeWidgetMenuId='';
  saveWidgetPrefs();
  renderDash();
}
function applyFocusedDashboardPreset(){
  widgetPrefs={};
  WIDGET_DEFS.forEach(w=>widgetPrefs[w.id]=FOCUSED_WIDGET_IDS.includes(w.id));
  enforcePinnedWidgets();
  ensureAtLeastOneWidget();
  saveWidgetPrefs();
  activeWidgetMenuId='';
  renderDash();
  toast('Dashboard focada em controle de gastos','success');
}
function enableAllDashboardWidgets(){
  WIDGET_DEFS.forEach(w=>widgetPrefs[w.id]=true);
  enforcePinnedWidgets();
  ensureAtLeastOneWidget();
  saveWidgetPrefs();
  renderDash();
  toast('Todos os widgets foram reativados','success');
}
function toggleDashboardManager(){
  dashboardManagerOpen=!dashboardManagerOpen;
  renderDashboardManager();
}
function moveWidgetOrder(id,dir){
  if(isPinnedWidget(id))return;
  const idx=widgetOrder.indexOf(id);
  const next=idx+dir;
  if(idx<0||next<0||next>=widgetOrder.length||isPinnedWidget(widgetOrder[next]))return;
  [widgetOrder[idx],widgetOrder[next]]=[widgetOrder[next],widgetOrder[idx]];
  enforcePinnedWidgets();
  saveWidgetOrder();
  renderDash();
}
function resetWidgetOrder(){
  widgetOrder=defaultDashboardWidgetOrder();
  enforcePinnedWidgets();
  saveWidgetOrder();
  renderDash();
  toast('Ordem do dashboard restaurada','info');
}
function moveWidgetToEdge(id,edge){
  if(isPinnedWidget(id))return;
  const idx=widgetOrder.indexOf(id);
  if(idx<0)return;
  const [item]=widgetOrder.splice(idx,1);
  if(edge==='start')widgetOrder.unshift(item);
  else widgetOrder.push(item);
  enforcePinnedWidgets();
  saveWidgetOrder();
  renderDash();
}
function resetDashboardWidgets(){
  widgetPrefs={};
  WIDGET_DEFS.forEach(w=>widgetPrefs[w.id]=w.default);
  widgetOrder=defaultDashboardWidgetOrder();
  widgetFilters={};
  activeWidgetMenuId='';
  enforcePinnedWidgets();
  ensureAtLeastOneWidget();
  localStorage.setItem(WIDGETS_KEY,JSON.stringify(widgetPrefs));
  localStorage.setItem(WIDGET_ORDER_KEY,JSON.stringify(widgetOrder));
  localStorage.setItem(WIDGET_FILTER_KEY,JSON.stringify(widgetFilters));
  if(cfg.mode==='api')saveRemoteState().catch(()=>{});
  renderDash();
  toast('Dashboard voltou aos padrões','success');
}
function setWidgetFilter(widget,key,value){
  widgetFilters[widget]=asObj(widgetFilters[widget]);
  widgetFilters[widget][key]=value;
  saveWidgetFilters();
  if(isDashboardActive()){
    replaceDashboardWidget(widget);
    renderDashboardManager();
    return;
  }
  renderDash();
}
function toggleWidgetMenu(id){
  const previous=activeWidgetMenuId;
  activeWidgetMenuId=activeWidgetMenuId===id?'':id;
  if(isDashboardActive()){
    rerenderDashboardWidgets([previous,id].filter(Boolean));
    return;
  }
  renderDash();
}
function closeWidgetMenu(){
  if(!activeWidgetMenuId)return;
  const previous=activeWidgetMenuId;
  activeWidgetMenuId='';
  if(isDashboardActive()){
    rerenderDashboardWidgets([previous]);
    return;
  }
  renderDash();
}
function openWidgetTarget(id){
  const targets={
    cards:'transactions',
    quickactions:'transactions',
    workbench:'dashboard',
    commitments:'commitments',
    renewals:'commitments',
    charts:'transactions',
    compare:'transactions',
    projection:'future',
    weekly:'transactions',
    anomaly:'transactions',
    budalerts:'budget',
    goals:'goals',
    budgets:'budget',
    recent:'transactions',
    ministats:'transactions',
    accounts:'accounts',
    vehicles:'car',
    shopping:'shopping',
    barcats:'transactions',
    saverate:'transactions'
  };
  const page=targets[id]||'dashboard';
  activeWidgetMenuId='';
  showPage(page);
}
function onDashboardManagerDragStart(id){
  dashboardDragId=id;
}
function onDashboardManagerDragEnd(){
  dashboardDragId='';
}
function onDashboardManagerDragOver(id,event){
  event.preventDefault();
  if(!dashboardDragId||dashboardDragId===id)return;
  const rows=document.querySelectorAll('.dash-manager-row');
  rows.forEach(row=>row.classList.toggle('drag-over',row.dataset.widgetId===id));
}
function onDashboardManagerDrop(id,event){
  event.preventDefault();
  const rows=document.querySelectorAll('.dash-manager-row');
  rows.forEach(row=>row.classList.remove('drag-over'));
  if(!dashboardDragId||dashboardDragId===id)return;
  if(isPinnedWidget(dashboardDragId)||isPinnedWidget(id))return;
  const from=widgetOrder.indexOf(dashboardDragId);
  const to=widgetOrder.indexOf(id);
  if(from<0||to<0)return;
  const [item]=widgetOrder.splice(from,1);
  widgetOrder.splice(to,0,item);
  enforcePinnedWidgets();
  dashboardDragId='';
  saveWidgetOrder();
  renderDash();
}

const SL_KEY = 'fz_shopping';

// Estrutura: { lists: [{id, name, ico}], items: [{id, listId, name, qty, cat, bought, createdAt}] }
let sl = { lists: [], items: [] };
let slActiveList = null;
let slNewListIco = '\u{1F6D2}';

function loadSL() {
  if(cfg.mode==='api'&&sl?.lists?.length)return;
  try { sl = JSON.parse(localStorage.getItem(SL_KEY) || 'null') || { lists:[], items:[] }; }
  catch { sl = { lists:[], items:[] }; }
  if (!sl.lists) sl.lists = [];
  if (!sl.items) sl.items = [];
  // Cria lista padro se no existir
  if (!sl.lists.length) {
    sl.lists.push({ id: uid(), name: 'Mercado', ico: '\u{1F6D2}' });
    saveSL();
  }
  slActiveList = sl.lists[0]?.id || null;
}

function saveSL() {
  localStorage.setItem(SL_KEY, JSON.stringify(sl));
  if(cfg.mode==='api')saveRemoteState().catch(e=>toast('Erro ao salvar lista: '+e.message,'error'));
}

function renderShopping() {
  loadSL();
  renderSLStats();
  renderSLTabs();
  renderSLContent();
}
function renderSLStats(){
  const el=document.getElementById('slStats');if(!el)return;
  const total=sl.items.length;
  const bought=sl.items.filter(i=>i.bought).length;
  const pending=total-bought;
  const active=sl.items.filter(i=>i.listId===slActiveList);
  const cats=new Set(sl.items.map(i=>i.cat)).size;
  el.innerHTML=`
    <div class="insight-card"><div class="insight-k">Pendentes</div><div class="insight-v" style="color:var(--warn)">${pending}</div><div class="cc">${total} itens totais</div></div>
    <div class="insight-card"><div class="insight-k">Comprados</div><div class="insight-v" style="color:var(--ac)">${bought}</div><div class="cc">${total?Math.round(bought/total*100):0}% concluído</div></div>
    <div class="insight-card"><div class="insight-k">Lista atual</div><div class="insight-v">${active.length}</div><div class="cc">itens nesta lista</div></div>
    <div class="insight-card"><div class="insight-k">Categorias</div><div class="insight-v" style="color:var(--ac2)">${cats}</div></div>`;
}

function renderSLTabs() {
  const el = document.getElementById('slTabs');
  if (!el) return;
  el.innerHTML = sl.lists.map(l => {
    const items = sl.items.filter(i => i.listId === l.id);
    const pending = items.filter(i => !i.bought).length;
    const isActive = l.id === slActiveList;
    return `<div class="sl-tab ${isActive ? 'active' : ''}" onclick="switchSLList('${l.id}')">
      <span>${l.ico}</span>
      <span>${l.name}</span>
      ${pending > 0 ? `<span class="sl-count">${pending}</span>` : ''}
      ${sl.lists.length > 1 ? `<span class="sl-tab-del" onclick="event.stopPropagation();deleteList('${l.id}')">✕</span>` : ''}
    </div>`;
  }).join('') + `<button class="sl-tab" onclick="openNewListModal()" style="color:var(--mt)">+ Lista</button>`;
}

function renderSLContent() {
  const el = document.getElementById('slContent');
  if (!el) return;

  const items = sl.items.filter(i => i.listId === slActiveList);
  const total = items.length;
  const bought = items.filter(i => i.bought).length;

  // Progress
  const fill = document.getElementById('slProgFill');
  const txt = document.getElementById('slProgTxt');
  if (fill) fill.style.width = total ? (bought/total*100)+'%' : '0%';
  if (txt) txt.textContent = `${bought}/${total}`;

  if (!total) {
    el.innerHTML = `<div class="sl-empty"><span class="sei">🛒</span><p>Lista vazia  adicione itens acima.</p></div>`;
    return;
  }

  // Agrupar por categoria
  const cats = {};
  items.forEach(i => {
    if (!cats[i.cat]) cats[i.cat] = [];
    cats[i.cat].push(i);
  });

  // Pendentes primeiro, depois comprados
  const sortedCats = Object.entries(cats).sort(([,a],[,b]) => {
    const aPend = a.some(x=>!x.bought);
    const bPend = b.some(x=>!x.bought);
    if (aPend && !bPend) return -1;
    if (!aPend && bPend) return 1;
    return 0;
  });

  el.innerHTML = sortedCats.map(([cat, catItems]) => {
    const pending = catItems.filter(i=>!i.bought).length;
    return `<div class="sl-cat-section">
      <div class="sl-cat-label">
        <span>${cat}</span>
        ${pending > 0 ? `<span style="color:var(--ac);font-size:9px">${pending} pendente${pending>1?'s':''}</span>` : `<span style="color:var(--grn);font-size:9px">✓ todos</span>`}
      </div>
      <div class="sl-items">
        ${catItems.sort((a,b)=>a.bought-b.bought).map(i => `
          <div class="sl-item ${i.bought?'bought':''}" onclick="toggleSLItem('${i.id}')">
            <div class="sl-check"><span class="sl-check-ico">✓</span></div>
            <span class="sl-name">${i.name}</span>
            ${i.qty ? `<span class="sl-qty">${i.qty}</span>` : ''}
            <button class="sl-item-del" onclick="event.stopPropagation();deleteSLItem('${i.id}')">🗑️</button>
          </div>`).join('')}
      </div>
    </div>`;
  }).join('');
}

function switchSLList(id) {
  slActiveList = id;
  if(cfg.mode==='api')saveRemoteState().catch(()=>{});
  renderSLStats();
  renderSLTabs();
  renderSLContent();
}

function addShoppingItem() {
  if(!requireWriteAccess('adicionar itens às listas'))return;
  const nameEl = document.getElementById('slItemName');
  const qtyEl  = document.getElementById('slItemQty');
  const catEl  = document.getElementById('slItemCat');
  const name = nameEl?.value.trim();
  if (!name) { nameEl?.focus(); return; }
  if (!slActiveList) return;

  sl.items.push({
    id: uid(),
    listId: slActiveList,
    name,
    qty: qtyEl?.value.trim() || '',
    cat: catEl?.value || '🛒 Geral',
    bought: false,
    createdAt: Date.now()
  });
  saveSL();

  if (nameEl) nameEl.value = '';
  if (qtyEl)  qtyEl.value  = '';
  nameEl?.focus();

  renderSLTabs();
  renderSLStats();
  renderSLContent();
}

function toggleSLItem(id) {
  if(!requireWriteAccess('marcar itens da lista'))return;
  const item = sl.items.find(i => i.id === id);
  if (!item) return;
  item.bought = !item.bought;
  saveSL();
  renderSLStats();
  renderSLTabs();
  renderSLContent();
}

function deleteSLItem(id) {
  if(!requireWriteAccess('remover itens da lista'))return;
  sl.items = sl.items.filter(i => i.id !== id);
  saveSL();
  renderSLStats();
  renderSLTabs();
  renderSLContent();
}

function clearBought() {
  if(!requireWriteAccess('limpar itens comprados'))return;
  if (!confirm('Remover todos os itens j comprados?')) return;
  sl.items = sl.items.filter(i => i.listId !== slActiveList || !i.bought);
  saveSL();
  renderSLStats();
  renderSLContent();
  toast('Comprados removidos ✓', 'success');
}

function openNewListModal() {
  if(!requireWriteAccess('criar listas de compras'))return;
  document.getElementById('nlName').value = '';
  slNewListIco = '🛒';
  document.querySelectorAll('.list-ico-btn').forEach(b => b.classList.toggle('sel', b.textContent === '🛒'));
  document.getElementById('newListModal').classList.add('open');
  setTimeout(() => document.getElementById('nlName').focus(), 100);
}

function selListIco(btn, ico) {
  slNewListIco = ico;
  document.querySelectorAll('.list-ico-btn').forEach(b => b.classList.remove('sel'));
  btn.classList.add('sel');
}

function createNewList() {
  if(!requireWriteAccess('criar listas de compras'))return;
  const name = document.getElementById('nlName').value.trim();
  if (!name) { toast('Informe o nome', 'error'); return; }
  const id = uid();
  sl.lists.push({ id, name, ico: slNewListIco });
  slActiveList = id;
  saveSL();
  closeM('newListModal');
  renderSLStats();
  renderSLTabs();
  renderSLContent();
  toast(`Lista "${name}" criada! ✓`, 'success');
}

function deleteList(id) {
  if(!requireWriteAccess('remover listas de compras'))return;
  if (!confirm('Remover esta lista e todos os itens?')) return;
  sl.lists = sl.lists.filter(l => l.id !== id);
  sl.items = sl.items.filter(i => i.listId !== id);
  slActiveList = sl.lists[0]?.id || null;
  saveSL();
  renderSLStats();
  renderSLTabs();
  renderSLContent();
}

// ════════════════════════════════════════════════════════════
// NOTIFICAO PERSISTENTE (Android Capacitor)
// ════════════════════════════════════════════════════════════
async function setupPersistentNotification() {
  if (!window.Capacitor) return;
  const { LocalNotifications } = Capacitor.Plugins || {};
  if (!LocalNotifications) return;

  try {
    const perm = await LocalNotifications.checkPermissions();
    if (perm.display !== 'granted') return;

    // Cancela notificao persistente anterior
    await LocalNotifications.cancel({ notifications: [{ id: 99 }] }).catch(() => {});

    const saldo = S.accounts.reduce((s,a) => s + getAccBal(a.id), 0);
    const pendentes = S.transactions.filter(t => isFut(t.date) && !t.paid && t.type === 'expense').length + dueOccurrences(offD(new Date(), 7)).length;
    const slPending = (() => {
      try {
        const data = JSON.parse(localStorage.getItem(SL_KEY) || '{}');
        return (data.items || []).filter(i => !i.bought).length;
      } catch { return 0; }
    })();

  // Notificação persistente, ongoing + sem autoCancel + priority max
    await LocalNotifications.schedule({
      notifications: [{
        id: 99,
        title: `💰 Finanza  ${fmt(saldo)}`,
        body: `${pendentes > 0 ? pendentes + ' conta(s) a pagar' : 'Sem contas pendentes'}${slPending > 0 ? '  🛒 ' + slPending + ' itens na lista' : ''}`,
        schedule: { at: new Date(Date.now() + 500), repeats: false },
        ongoing: true,
        sticky: true,
        autoCancel: false,
        silent: true,
        foreground: true,
        sound: null,
        channelId: 'finanza_persistent',
        smallIcon: 'ic_stat_icon_config_sample',
        actionTypeId: 'FINANZA_ACTIONS',
        extra: { action: 'open' }
      }]
    });
  } catch (e) {
    console.warn('Persistent notification:', e.message);
  }
}

// Registrar action types para a notificao persistente
async function registerNotifActions() {
  if (!window.Capacitor) return;
  const { LocalNotifications } = Capacitor.Plugins || {};
  if (!LocalNotifications) return;
  try {
    // Criar canal Android com importncia mnima (sem som, sem pop-up, no pode fechar)
    await LocalNotifications.createChannel({
      id: 'finanza_persistent',
      name: 'Finanza Status',
      description: 'Saldo e atalhos do Finanza',
      importance: 2,       // IMPORTANCE_LOW  aparece mas no interrompe
      visibility: 1,       // VISIBILITY_PUBLIC
      sound: null,
      vibration: false,
      lights: false,
    }).catch(() => {});    // ignora se j existe

    await LocalNotifications.registerActionTypes({
      types: [{
        id: 'FINANZA_ACTIONS',
        actions: [
          { id: 'add_expense', title: '+ Gasto', foreground: true },
          { id: 'shopping',    title: '🛒 Lista', foreground: true },
        ]
      }]
    });
    // Listener para aes da notificao
    LocalNotifications.addListener('localNotificationActionPerformed', (notif) => {
      if (notif.actionId === 'add_expense') {
        showPage('transactions');
        setTimeout(() => toggleQA(), 400);
      } else if (notif.actionId === 'shopping') {
        showPage('shopping');
      } else {
        // Toque simples na notificao
        showPage('dashboard');
      }
    });
  } catch (e) {
    console.warn('Notif actions:', e.message);
  }
}




