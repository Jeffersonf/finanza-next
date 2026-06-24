// Graficos
// Extraido de index.html para organizar o frontend sem mudar a arquitetura global.

if(window.Chart){
  Chart.defaults.animation=false;
  Chart.defaults.responsiveAnimationDuration=0;
}

function renderCarCharts(events=[],stats={}){
  const isDark=document.documentElement.dataset.theme==='dark';
  const tick=isDark?'rgba(240,243,255,.45)':'rgba(13,16,32,.42)';
  const grid=isDark?'rgba(255,255,255,.05)':'rgba(0,0,0,.05)';
  const canvasA=document.getElementById('carSpendChart');
  const canvasB=document.getElementById('carMixChart');
  const empty=document.getElementById('carChartsEmpty');
  if(window._carSpendChart)window._carSpendChart.destroy();
  if(window._carMixChart)window._carMixChart.destroy();
  if(!canvasA||!canvasB)return;
  if(!events.length){
    canvasA.style.display='none';
    canvasB.style.display='none';
    if(empty)empty.style.display='block';
    return;
  }
  canvasA.style.display='';
  canvasB.style.display='';
  if(empty)empty.style.display='none';

  const byMonth={};
  [...events].sort((a,b)=>a.date.localeCompare(b.date)).forEach(e=>{
    const key=String(e.date||'').slice(0,7);
    if(!byMonth[key])byMonth[key]={fuel:0,expense:0,total:0,liters:0,km:0};
    byMonth[key].total+=e.amount||0;
    if(e.type==='fuel'){
      byMonth[key].fuel+=e.amount||0;
      byMonth[key].liters+=e.liters||0;
    }else byMonth[key].expense+=e.amount||0;
  });
  const distanceByMonth=new Map();
  const byVehicle=new Map();
  events.forEach(e=>{if(!byVehicle.has(e.vehicleId))byVehicle.set(e.vehicleId,[]);byVehicle.get(e.vehicleId).push(e);});
  byVehicle.forEach(items=>{
    const fuels=items.filter(e=>e.type==='fuel'&&e.odometer>0).sort((a,b)=>a.date.localeCompare(b.date)||a.odometer-b.odometer);
    const monthOdo={};
    fuels.forEach(e=>{
      const key=String(e.date||'').slice(0,7);
      if(!monthOdo[key])monthOdo[key]={min:e.odometer,max:e.odometer};
      monthOdo[key].min=Math.min(monthOdo[key].min,e.odometer);
      monthOdo[key].max=Math.max(monthOdo[key].max,e.odometer);
    });
    Object.entries(monthOdo).forEach(([key,val])=>{
      distanceByMonth.set(key,(distanceByMonth.get(key)||0)+Math.max(0,(val.max||0)-(val.min||0)));
    });
  });
  const months=Object.keys(byMonth).sort();
  const monthLabels=months.map(k=>{
    const [y,m]=k.split('-');
    return `${m}/${String(y).slice(2)}`;
  });
  const fuelSeries=months.map(k=>byMonth[k].fuel);
  const expenseSeries=months.map(k=>byMonth[k].expense);
  const totalSeries=months.map(k=>byMonth[k].total);
  const kmSeries=months.map(k=>distanceByMonth.get(k)||0);
  const ctxA=canvasA.getContext('2d');
  const ctxB=canvasB.getContext('2d');
  const moneyTick=v=>privacyMode?'R$•••':'R$'+(v>=1000?(v/1000).toFixed(0)+'k':Number(v).toLocaleString('pt-BR'));

  window._carSpendChart=new Chart(ctxA,{
    type:carChartMode==='line'?'line':'bar',
    data:{
      labels:monthLabels,
      datasets:carChartMode==='line'
        ?[
          {label:'Total',data:totalSeries,borderColor:'#c8f55a',backgroundColor:'rgba(200,245,90,.12)',fill:true,tension:.35,pointRadius:3,pointBackgroundColor:'#c8f55a'},
          {label:'Combustível',data:fuelSeries,borderColor:'rgba(245,200,90,.95)',backgroundColor:'rgba(245,200,90,.10)',fill:false,tension:.35,pointRadius:2},
          {label:'Despesas',data:expenseSeries,borderColor:'rgba(245,112,90,.95)',backgroundColor:'rgba(245,112,90,.10)',fill:false,tension:.35,pointRadius:2}
        ]
        :[
          {label:'Combustível',data:fuelSeries,backgroundColor:'rgba(245,200,90,.75)',borderRadius:4,borderSkipped:false},
          {label:'Despesas',data:expenseSeries,backgroundColor:'rgba(245,112,90,.68)',borderRadius:4,borderSkipped:false}
        ]
    },
    options:{
      responsive:true,maintainAspectRatio:true,animation:false,animations:false,resizeDelay:120,
      plugins:{legend:{labels:{color:tick,font:{family:'DM Sans',size:10},boxWidth:8,boxHeight:8}}},
      scales:{
        x:{grid:{color:grid},ticks:{color:tick,font:{family:'DM Sans',size:10}}},
        y:{grid:{color:grid},ticks:{color:tick,font:{family:'DM Sans',size:10},callback:v=>moneyTick(v)}}
      }
    }
  });

  window._carMixChart=new Chart(ctxB,{
    type:'line',
    data:{
      labels:monthLabels,
      datasets:[
        {label:'Km medidos',data:kmSeries,borderColor:'rgba(90,245,200,.95)',backgroundColor:'rgba(90,245,200,.14)',fill:true,tension:.35,pointRadius:3,pointBackgroundColor:'rgba(90,245,200,.95)',yAxisID:'y'},
        {label:'Consumo (km/l)',data:months.map(k=>byMonth[k].liters&&distanceByMonth.get(k)?(distanceByMonth.get(k)/byMonth[k].liters):null),borderColor:'rgba(167,139,250,.95)',backgroundColor:'rgba(167,139,250,.10)',fill:false,tension:.35,pointRadius:3,pointBackgroundColor:'rgba(167,139,250,.95)',yAxisID:'y1'}
      ]
    },
    options:{
      responsive:true,maintainAspectRatio:true,animation:false,animations:false,resizeDelay:120,
      plugins:{legend:{labels:{color:tick,font:{family:'DM Sans',size:10},boxWidth:8,boxHeight:8}}},
      scales:{
        x:{grid:{color:grid},ticks:{color:tick,font:{family:'DM Sans',size:10}}},
        y:{position:'left',grid:{color:grid},ticks:{color:tick,font:{family:'DM Sans',size:10}}},
        y1:{position:'right',grid:{drawOnChartArea:false},ticks:{color:tick,font:{family:'DM Sans',size:10}}}
      }
    }
  });
}

function renderTxCharts(){
  const isDark=document.documentElement.dataset.theme==='dark';
  const moneyTick=()=>privacyMode?'R$•••':null;
  const tick=isDark?'rgba(240,243,255,.4)':'rgba(13,16,32,.35)';
  const grid=isDark?'rgba(255,255,255,.04)':'rgba(0,0,0,.04)';
  const range=getRange(curTxP);
  let txs=[...S.transactions].filter(t=>t.date>=range.from&&t.date<=range.to&&t.type==='expense'&&!t.paid);
  // Category donut
  const catTot={};txs.forEach(t=>{catTot[t.category]=(catTot[t.category]||0)+t.amount;});
  const cats=Object.keys(catTot).sort((a,b)=>catTot[b]-catTot[a]).slice(0,8);
  const ctx1=document.getElementById('txCatChart')?.getContext('2d');
  if(ctx1){
    if(window._txCatChart)window._txCatChart.destroy();
    window._txCatChart=new Chart(ctx1,{type:'doughnut',data:{labels:cats,datasets:[{data:cats.map(c=>catTot[c]),backgroundColor:cats.map(c=>getCat(c).col+'cc'),borderColor:cats.map(c=>getCat(c).col),borderWidth:1.5,hoverOffset:3}]},options:{responsive:true,maintainAspectRatio:true,animation:false,animations:false,resizeDelay:120,cutout:'62%',plugins:{legend:{display:true,position:'right',labels:{color:tick,font:{family:'DM Sans',size:10},boxWidth:8,padding:6}},tooltip:{callbacks:{label:c=>` ${fmt(c.raw)}`}}}}});
  }
  // Daily line chart
  const allTxs=[...S.transactions].filter(t=>t.date>=range.from&&t.date<=range.to&&!t.paid);
  const dayMap={};
  allTxs.forEach(t=>{
    if(!dayMap[t.date])dayMap[t.date]={inc:0,exp:0};
    if(t.type==='income')dayMap[t.date].inc+=t.amount;
    else dayMap[t.date].exp+=t.amount;
  });
  const days=Object.keys(dayMap).sort();
  const ctx2=document.getElementById('txLineChart')?.getContext('2d');
  if(ctx2){
    if(window._txLineChart)window._txLineChart.destroy();
    window._txLineChart=new Chart(ctx2,{type:'bar',data:{labels:days.map(d=>{const[,m,day]=d.split('-');return`${day}/${m}`;}),datasets:[{label:'Receitas',data:days.map(d=>dayMap[d].inc),backgroundColor:'rgba(200,245,90,.7)',borderRadius:3,borderSkipped:false},{label:'Despesas',data:days.map(d=>dayMap[d].exp),backgroundColor:'rgba(245,112,90,.6)',borderRadius:3,borderSkipped:false}]},options:{responsive:true,maintainAspectRatio:true,animation:false,animations:false,resizeDelay:120,plugins:{legend:{labels:{color:tick,font:{family:'DM Sans',size:10},boxWidth:8,boxHeight:8}}},scales:{x:{grid:{color:grid},ticks:{color:tick,font:{family:'DM Sans',size:9},maxTicksLimit:8}},y:{grid:{color:grid},ticks:{color:tick,font:{family:'DM Sans',size:9},callback:v=>moneyTick()||'R$'+(v>=1000?(v/1000).toFixed(0)+'k':v)}}}}});
  }
}

function renderCharts(isDark) {
  const moneyTick=()=>privacyMode?'R$•••':null;
  const tick = isDark ? 'rgba(240,243,255,.4)' : 'rgba(13,16,32,.35)';
  const grid = isDark ? 'rgba(255,255,255,.05)' : 'rgba(0,0,0,.04)';
  const months=[], incomes=[], expenses=[], balances=[];
  let runBal = 0;
  for (let i = 5; i >= 0; i--) {
    const d = new Date(curDt.getFullYear(), curDt.getMonth()-i, 1);
    months.push(['Jan','Fev','Mar','Abr','Mai','Jun','Jul','Ago','Set','Out','Nov','Dez'][d.getMonth()]);
    const txs = getMonthTx(d).filter(t=>!isFut(t.date)&&!t.paid);
    const mi = txs.filter(t=>t.type==='income').reduce((s,t)=>s+t.amount,0);
    const me = txs.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amount,0);
    incomes.push(mi); expenses.push(me); runBal+=mi-me; balances.push(runBal);
  }
  const scaleOpts = {
    x:{grid:{color:grid},ticks:{color:tick,font:{family:'DM Sans',size:10}}},
    y:{grid:{color:grid},ticks:{color:tick,font:{family:'DM Sans',size:10},callback:v=>moneyTick()||'R$'+(v/1000).toFixed(0)+'k'}}
  };
  const legOpts = { labels:{color:tick,font:{family:'DM Sans',size:10},boxWidth:8,boxHeight:8} };
  if (window._flowChart) window._flowChart.destroy();
  const ctx1 = document.getElementById('flowChart')?.getContext('2d');
  if (ctx1) {
    if (chartMode==='bars') {
      window._flowChart = new Chart(ctx1,{type:'bar',data:{labels:months,datasets:[
        {label:'Receitas',data:incomes,backgroundColor:'rgba(200,245,90,.8)',borderRadius:4,borderSkipped:false},
        {label:'Despesas',data:expenses,backgroundColor:'rgba(245,112,90,.7)',borderRadius:4,borderSkipped:false}
      ]},options:{responsive:true,maintainAspectRatio:true,animation:false,animations:false,resizeDelay:120,plugins:{legend:{labels:legOpts.labels}},scales:scaleOpts}});
    } else {
      window._flowChart = new Chart(ctx1,{type:'line',data:{labels:months,datasets:[
        {label:'Saldo acumulado',data:balances,borderColor:'#c8f55a',backgroundColor:'rgba(200,245,90,.1)',fill:true,tension:.4,pointBackgroundColor:'#c8f55a',pointRadius:4}
      ]},options:{responsive:true,maintainAspectRatio:true,animation:false,animations:false,resizeDelay:120,plugins:{legend:{labels:legOpts.labels}},scales:scaleOpts}});
    }
  }
  // Donut de categorias
  const catTot = {};
  const txMF = catFilter
    ? getMonthTx(curDt).filter(t=>t.type==='expense'&&!isFut(t.date)&&t.category===catFilter)
    : getMonthTx(curDt).filter(t=>t.type==='expense'&&!isFut(t.date)&&!t.paid);
  txMF.forEach(t=>{ catTot[t.category]=(catTot[t.category]||0)+t.amount; });
  const cats = Object.keys(catTot);
  if (window._catChart) window._catChart.destroy();
  const ctx2 = document.getElementById('catChart')?.getContext('2d');
  if (ctx2) {
    window._catChart = new Chart(ctx2,{type:'doughnut',
      data: cats.length
        ? {labels:cats,datasets:[{data:cats.map(c=>catTot[c]),backgroundColor:cats.map(c=>getCat(c).col+'cc'),borderColor:cats.map(c=>getCat(c).col),borderWidth:1.5,hoverOffset:3}]}
        : {labels:['Sem dados'],datasets:[{data:[1],backgroundColor:[isDark?'#1e2330':'#dde1ef'],borderWidth:0}]},
      options:{responsive:true,maintainAspectRatio:true,animation:false,animations:false,resizeDelay:120,cutout:'65%',
        onClick:(_,els)=>{ if(els.length){const lbl=cats[els[0].index];catFilter=catFilter===lbl?null:lbl;renderDash();} },
        plugins:{legend:{display:cats.length>0,position:'right',labels:{color:tick,font:{family:'DM Sans',size:10},boxWidth:8,padding:6}},
          tooltip:{callbacks:{label:c=>` ${fmt(c.raw)}`}}}
      }
    });
  }
  if (catFilter) {
    const rl = document.getElementById('recList');
    if (rl) {
      const filtered = S.transactions.filter(t=>t.category===catFilter&&getMonthTx(curDt).includes(t));
      rl.innerHTML = `<div style="font-size:11px;color:var(--ac);margin-bottom:7px">🔍 ${catFilter} <a onclick="catFilter=null;renderDash()" style="cursor:pointer;text-decoration:underline">limpar</a></div>`
        + filtered.map(txHTML).join('');
    }
  }
}


// ════════════════════════════════════════════════════════════
// LISTA DE COMPRAS
// ════════════════════════════════════════════════════════════
