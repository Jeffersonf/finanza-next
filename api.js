// API
// Extraido de index.html para organizar o frontend sem mudar a arquitetura global.

async function api(m,p,b=null){
  const r=await fetch(cfg.url+p,{method:m,headers:{'Content-Type':'application/json','x-api-key':cfg.key},body:b?JSON.stringify(b):undefined});
  if(!r.ok){const e=await r.json().catch(()=>({}));throw new Error(e.error||`HTTP ${r.status}`);}
  return r.json();
}
