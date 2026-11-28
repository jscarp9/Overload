/* ========================================================================
   Overload: Profile showcase and Profile settings (0.27)

   The Profile's progress sections (rank, highlights, then vs now, Big 3, strength levels,
   PRs, month vs last, achievements, heatmap and more), the showcase crewmates see, the
   monthly coach note and the Profile settings sheet. Wraps myProfile, renderProfile16,
   profPage and sheet15, so it must load after 14-polish.js.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

/* ---------- 0.27: a real profile ---------- */
const SECS28=[['rank','Rank progress'],['high','Highlights'],['coach','Coach note (only you)'],['goal','Active goal'],['then','Then vs now'],['big3','Big 3 total'],['lvl','Strength levels'],['prs','Personal records'],['month','This month vs last'],['recent','Recent achievements'],['near','Almost there'],['heat','Consistency'],['mus','Muscle map'],['tro','Trophies'],['photos','Progress photos (only you)']];
const hid28=k=>!!(st.set.hide28||{})[k];
const fd28=d=>{if(!d)return '';const t=toD(d);return MON[t.getMonth()]+' '+t.getDate()};
const lvlName28=t=>t?TIERS[t-1][0]:'Starter';
const kw28=(v,u)=>Math.round(toMine(+v||0,u||U()));
function pf28Data(){const E=engine(),rs=realSessions(),bw=+st.set.bw||0,L={};
 rs.forEach(s=>Object.entries(s.ex||{}).forEach(([id,a])=>{const nm=exName(id);if(/leg curl|hamstring curl/i.test(nm))return;const lf=LIFTS.find(l=>l[2].test(nm));if(!lf)return;let b=null;(a||[]).forEach(x=>{if(full(x)){const v=e1(x);if(!b||v>b.e)b={w:+x.w,r:+x.r,e:v,d:s.date}}});if(!b)return;
  const o=L[lf[0]]=L[lf[0]]||{k:lf[0],name:lf[1],n:0,first:b,best:b,hist:[],lastD:''};if(o.lastD!==s.date){o.n++;o.lastD=s.date}if(b.e>o.best.e)o.best=b;o.hist.push([s.date,b.e])}));
 const all=Object.values(L),r1=v=>Math.round(v);
 const then=all.filter(o=>o.n>=2).sort((a,b)=>b.n-a.n).slice(0,4).map(o=>({n:o.name,f:[o.first.w,o.first.r,o.first.d],b:[o.best.w,o.best.r,o.best.d],de:r1(o.best.e-o.first.e)}));
 const ends=[];{const t=toD(today());for(let i=5;i>=0;i--){const e=new Date(t.getFullYear(),t.getMonth()-i+1,0);ends.push(i?iso(e):today())}}
 const b3k=['bench','squat','dead'],big3={p:b3k.map(k=>L[k]?r1(L[k].best.e):0),h:ends.map(e=>b3k.reduce((a,k)=>a+(L[k]?Math.max(0,...L[k].hist.filter(x=>x[0]<=e).map(x=>x[1])):0),0)).map(r1)};
 const lvl=bw?all.sort((a,b)=>b.best.e-a.best.e).slice(0,5).map(o=>{const lf=LIFTS.find(l=>l[0]===o.k),ratio=o.best.e/bw,T=lf[3],t=T.filter(x=>ratio>=x).length,nx=T[t],lo=t?T[t-1]:0,pc=nx?Math.round((t+Math.max(0,Math.min(1,(ratio-lo)/Math.max(.01,nx-lo))))/T.length*100):100;return{n:o.name,t,go:nx?Math.max(1,Math.ceil(nx*bw-o.best.e)):0,pc}}):[];
 const prs=all.slice().sort((a,b)=>b.best.e-a.best.e).slice(0,5).map(o=>[o.name,o.best.w,o.best.r,o.best.d]);
 const td=today(),dd=+td.slice(8),mo0=td.slice(0,8)+'01',pm=new Date(toD(mo0).getFullYear(),toD(mo0).getMonth()-1,1),pmEnd=new Date(pm.getFullYear(),pm.getMonth()+1,0),pmTo=iso(new Date(pm.getFullYear(),pm.getMonth(),Math.min(dd,pmEnd.getDate()))),pmFrom=iso(pm);
 const agg=(a,b)=>{const ss=rs.filter(s=>s.date>=a&&s.date<=b);return{w:new Set(ss.map(s=>s.date)).size,s:ss.reduce((x,s)=>x+sSets(s),0),v:Math.round(ss.reduce((x,s)=>x+sVol(s),0)),p:ss.reduce((x,s)=>x+sessStats(s).prs.length,0)}};
 const month={a:agg(mo0,td),b:agg(pmFrom,pmTo),lbl:[MON[toD(td).getMonth()],MON[pm.getMonth()],dd]};
 const sets={};rs.forEach(s=>{sets[s.date]=(sets[s.date]||0)+sSets(s)});const start=new Date(toD(weekDates(td)[0]).getTime()-11*7*864e5),heat=[];for(let i=0;i<84;i++){const d=iso(new Date(start.getTime()+i*864e5));heat.push(d>td?-1:(sets[d]||0))}
 const fav=all.slice().sort((a,b)=>b.n-a.n)[0];
 return{u:U(),then,big3,lvl,prs,month,heat,fav:fav?fav.name:'',best:E.best,since:E.since,L}}
/* what the crew sees: the same sections, minus the private ones */
function pf28Share(){try{const D=pf28Data(),H=st.set.hide28||{},o={u:D.u,fav:D.fav,best:D.best};['then','big3','lvl','prs','month','heat'].forEach(k=>{if(!H[k])o[k]=D[k]});return o}catch(e){return null}}
{const _mp=myProfile;myProfile=function(){const p=_mp.apply(this,arguments);try{p.pf28=pf28Share();p.hide28=Object.keys(st.set.hide28||{}).filter(k=>st.set.hide28[k])}catch(e){}return p}}

/* ---------- sections ---------- */
const card28=(cls,title,body,see)=>`<div class="card s28 ${cls}">${title?`<div class="s28h"><b>${title}</b>${see||''}</div>`:''}${body}</div>`;
const see28=(attr,l)=>`<button class="see28" ${attr}>${l||'See all'} ›</button>`;
function rank28(){const E=engine(),nx=RANKS[E.rank+1],un=nx&&UNLOCKS.find(u=>u[0]===E.rank+1);
 let pct=1,step='You’re at the top rank. Prestige keeps going.';
 if(E.trial){const t=E.trial;pct=Math.min(1,((t.days/Math.max(1,t.need))+(t.beat/Math.max(1,t.needBeat)))/2);step=`Promotion trial is on: train your planned days and beat last week on ${t.needBeat} lifts to reach ${RANKS[t.tier][0]}.`}
 else if(nx){const a=Math.min(1,E.active/nx[1]),b=Math.min(1,E.bcount/nx[2]);pct=(a+b)/2;const wk=Math.max(0,nx[1]-E.active),bd=Math.max(0,nx[2]-E.bcount);
  step=wk&&bd?`${wk} more active week${wk>1?'s':''} and ${bd} more badge${bd>1?'s':''} to start your ${nx[0]} trial.`:wk?`${wk} more active week${wk>1?'s':''} to start your ${nx[0]} trial. Train 2+ times this week.`:bd?`${bd} more badge${bd>1?'s':''} to start your ${nx[0]} trial.`:`Ready for your ${nx[0]} trial. It starts on your next planned day.`}
 const R=40,C=2*Math.PI*R,col=(nx||RANKS[E.rank])[4];
 const sl=E.lvl,xp=E.xp%200;
 return `<div class="card s28 rk28"><div class="rk28top"><button class="rk28emb" data-rk28="1" aria-label="Open rank up"><svg class="ring28" viewBox="0 0 100 100" aria-hidden="true"><circle cx="50" cy="50" r="${R}" class="rbg28"/><circle cx="50" cy="50" r="${R}" class="rfg28" style="stroke:${col}" stroke-dasharray="${(C*pct).toFixed(1)} ${C.toFixed(1)}" transform="rotate(-90 50 50)"/></svg>${emblem(E.rank)}</button>
  <div class="rk28t"><div class="glbl">YOUR RANK</div><h3>${esc(E.rname)}</h3><div class="sub">${nx?`${Math.round(pct*100)}% of the way to ${nx[0]}`:'Top rank'} · ${E.power} rank power</div></div></div>
  ${!E.trial&&nx?`${bar22(E.active,nx[1],'Active weeks','A week counts when you train 2+ times')}${bar22(E.bcount,nx[2],'Badges')}`:''}
  <p class="rk28next">${ic27('target')}<span>${step}</span></p>
  ${un?`<div class="rk28un">${ic27('gift')}<span>${nx[0]} unlocks <b>${esc(un[1])}</b></span></div>`:''}
  ${feat('season')?`<div class="rk28s"><div class="b22h"><span>Season level ${sl}</span><b>${sl>=30?'Maxed':`${200-xp} XP to ${sl+1}`}</b></div><i><u style="width:${sl>=30?100:Math.round(xp/200*100)}%"></u></i></div>`:''}
  <div class="rk28l"><button data-psec="ladder">${ic27('ladder')} Rank ladder</button>${feat('season')?`<button id="openpass">${ic27('ticket')} Season pass</button>`:''}</div></div>`}
function high28(D,E){const t=(v,l)=>`<div><b>${v}</b><span>${l}</span></div>`;const s=D.since||(E&&E.since);
 return card28('hi28','',`<div class="hg28">${t(E.workouts,'Workouts')}${t(E.lifeVol>=1e6?(E.lifeVol/1e6).toFixed(2)+'M':kfmt(E.lifeVol),U()+' moved')}${t(E.lifePR,'PRs')}${t((D.best||E.best||0)+' wk','Best streak')}${t(D.fav?esc(D.fav):'–','Favorite lift')}${t(s?MON[toD(s).getMonth()]+' ’'+String(toD(s).getFullYear()).slice(2):'–','Lifting since')}</div>`)}
function then28(D){if(!D.then||!D.then.length)return '';const u=D.u;
 return card28('then28','Then vs now',D.then.map(x=>`<div class="tv28"><div class="tvn28"><b>${esc(x.n)}</b><span class="sub">since ${fd28(x.f[2])}</span></div><div class="tvv28"><span class="was28">${kw28(x.f[0],u)}×${x.f[1]}</span><span class="arr28">→</span><b>${kw28(x.b[0],u)}×${x.b[1]}</b>${x.de>0?`<i class="up28">+${kw28(x.de,u)} ${U()}</i>`:''}</div></div>`).join('')+`<p class="sub s28f">Change in estimated 1-rep max since your first logged session.</p>`)}
function spark28(h){const mx=Math.max(1,...h),mn=Math.min(...h.filter(x=>x>0),mx),W=120,H=34,pts=h.map((v,i)=>[i*(W/(h.length-1)),v?H-4-(v-mn)/Math.max(1,mx-mn)*(H-8):H-2]);return `<svg class="spk28" viewBox="0 0 ${W} ${H}" aria-hidden="true"><polyline points="${pts.map(p=>p.map(x=>x.toFixed(1)).join(',')).join(' ')}"/><circle cx="${pts[pts.length-1][0]}" cy="${pts[pts.length-1][1]}" r="3"/></svg>`}
function big328(D){const b=D.big3;if(!b)return '';const u=D.u,nm=['Bench','Squat','Deadlift'],tot=b.p.reduce((a,x)=>a+x,0),miss=nm.filter((n,i)=>!b.p[i]);
 if(!tot)return card28('b328','Big 3 total',`<p class="sub">Log bench press, squat and deadlift to see your total and watch it climb.</p>`);
 const h=b.h,prev=h[h.length-2]||0,gain=h[h.length-1]-prev;
 return card28('b328','Big 3 total',`<div class="b3t28"><div><b class="num">${kw28(tot,u).toLocaleString()}</b><span>${U()} estimated</span>${gain>0&&prev?`<i class="up28">+${kw28(gain,u)} this month</i>`:''}</div>${h.filter(x=>x).length>1?spark28(h):''}</div><div class="b3p28">${b.p.map((v,i)=>`<div><span>${nm[i]}</span><b>${v?kw28(v,u):'–'}</b></div>`).join('')}</div>${miss.length?`<p class="sub s28f">Log ${miss.join(' and ').toLowerCase()} to complete your total.</p>`:''}`)}
function lvl28(D,own){if(!D.lvl||!D.lvl.length){return own?card28('lv28','Strength levels',`<p class="sub">Add your bodyweight to see how strong each lift is for your size.</p><button class="btn sm glass" data-go28="bw">Add bodyweight</button>`):''}
 const u=D.u;return card28('lv28','Strength levels',D.lvl.map(x=>{const nx=TIERS[x.t],col=x.t?TIERS[x.t-1][1]:'#8c939e';return `<div class="lvr28"><div class="lvn28"><b>${esc(x.n)}</b><span style="color:${col}">${lvlName28(x.t)}</span></div><div class="lvb28"><i><u style="width:${Math.max(4,x.pc||0)}%;background:${col}"></u></i><span class="sub">${nx?`${kw28(x.go,u)} ${U()} to ${nx[0]}`:'Top level'}</span></div></div>`}).join('')+`<p class="sub s28f">Estimated 1-rep max compared with bodyweight. A general guide, not a test.</p>`,own?see28('data-psec="lr"'):'')}
function prs28(D){if(!D.prs||!D.prs.length)return '';const u=D.u;return card28('pr28','Personal records',D.prs.map(x=>`<div class="prr28"><span>${esc(x[0])}</span><b>${kw28(x[1],u)} × ${x[2]}</b><span class="sub">${fd28(x[3])}</span></div>`).join(''))}
function month28(D){const m=D.month;if(!m)return '';const u=D.u,row=(l,a,b,f)=>{const d=a-b;return `<div class="mc28"><span>${l}</span><b>${f(a)}</b><i class="${d>0?'up28':d<0?'dn28':'eq28'}">${d>0?'▲':d<0?'▼':'='} ${d?f(Math.abs(d)):''}</i></div>`};
 return card28('mo28',`${m.lbl[0]} vs ${m.lbl[1]}`,`<div class="mcg28">${row('Workouts',m.a.w,m.b.w,x=>x)}${row('Sets',m.a.s,m.b.s,x=>x)}${row(U()+' moved',kw28(m.a.v,u),kw28(m.b.v,u),x=>kfmt(x))}${row('PRs',m.a.p,m.b.p,x=>x)}</div><p class="sub s28f">First ${m.lbl[2]} days of each month, so it’s a fair race.</p>`)}
function goal28(){const G=(st.set.goals||[]).filter(g=>!g.doneAt);if(!G.length||!feat('goals'))return '';const g=G[0],I=goalInfo(g);
 return card28('go28','Active goal',`<div class="b22"><div class="b22h"><span>${esc(exName(g.ex))} ${g.w} ${U()}</span><b>${I.pct}%</b></div><i><u style="width:${I.pct}%"></u></i><div class="b22s">${I.status==='on'?`On pace · projected ${fd28(I.proj)}`:I.status==='behind'?`Behind pace · projected ${fd28(I.proj)}, target ${fd28(g.date)}`:'Keep logging this lift to get a projection'}</div></div>`)}
function recent28(){const E=engine(),ev=[],lim=iso(new Date(Date.now()-75*864e5));
 E.R.forEach(r=>(r.list||[]).forEach(d=>{if(d>=lim)ev.push([d,r.icon,r.name,'Weekly badge'])}));
 realSessions().filter(s=>s.date>=lim).forEach(s=>{const ids=sessStats(s).prs||[];ids.slice(0,2).forEach(id=>{const a=(s.ex[id]||[]).filter(full);const b=a.reduce((m,x)=>!m||e1(x)>e1(m)?x:m,null);if(b)ev.push([s.date,'🏆',exName(id)+' PR',`${Math.round(+b.w)} × ${b.r}`])})});
 (st.set.goals||[]).forEach(g=>{if(g.doneAt&&g.doneAt>=lim)ev.push([g.doneAt,'🎯',`Goal hit: ${exName(g.ex)} ${g.w}`,''])});
 ev.sort((a,b)=>a[0]<b[0]?1:-1);if(!ev.length)return '';
 return card28('re28','Recent achievements',ev.slice(0,6).map(x=>`<div class="rer28"><span class="rei28">${x[1]}</span><div><b>${esc(x[2])}</b>${x[3]?`<span class="sub">${esc(x[3])}</span>`:''}</div><span class="sub">${fd28(x[0])}</span></div>`).join(''),see28('id="badges16"'))}
function near28(){const E=engine(),N=E.B.filter(b=>!b.earned&&!b.hid&&b.tgt>0&&b.cur>0).map(b=>({...b,p:b.cur/b.tgt})).sort((a,b)=>b.p-a.p).slice(0,3);if(!N.length)return '';
 return card28('ne28','Almost there',N.map(b=>`<button class="ner28" data-bdg="${esc(b.k)}"><span class="rei28">${b.icon}</span><div><div class="b22h"><span>${esc(b.name)}</span><b>${Math.round(b.cur).toLocaleString()}/${b.tgt.toLocaleString()}</b></div><i class="nb28"><u style="width:${Math.round(b.p*100)}%"></u></i><span class="sub">${esc(b.desc||'')}</span></div></button>`).join(''))}
function heat28(D){if(!D.heat)return '';const n=D.heat.filter(x=>x>0).length,cell=v=>`<i class="hc28 ${v<0?'fut':v===0?'z':v<10?'l1':v<20?'l2':'l3'}"></i>`;let cols='';for(let w=0;w<12;w++){cols+=`<div class="hw28">${D.heat.slice(w*7,w*7+7).map(cell).join('')}</div>`}
 return card28('ht28','Consistency',`<div class="hm28">${cols}</div><div class="hl28"><span class="sub">${n} training day${n===1?'':'s'} in the last 12 weeks</span><span class="hk28">Less ${cell(0)}${cell(5)}${cell(15)}${cell(25)} More</span></div>`)}
function mus28(){const D=masteryData();if(!D.length)return '';const C={};D.forEach(x=>{if(x.t)(MKEY[x.k]||[]).forEach(m=>C[m]=TIERS[x.t-1][1])});const dk=isDark();
 return card28('mu28','Muscle map',`<div class="mu28b"><div class="bodies"><svg viewBox="30 0 140 380" aria-label="Front mastery">${anatomy('front',{},dk,C)}</svg><svg viewBox="30 0 140 380" aria-label="Back mastery">${anatomy('back',{},dk,C)}</svg></div><div class="mu28l">${D.slice(0,5).map(x=>`<div><span>${esc(x.k)}</span><b style="color:${x.t?TIERS[x.t-1][1]:'var(--mute)'}">${x.t?TIERS[x.t-1][0]:'–'}</b></div>`).join('')}</div></div>`,see28('data-psec="mast"'))}
function tro28(){const T=trophies(),got=T.filter(t=>t.earned);return card28('tr28',`Trophies <span class="sub">${got.length}/${T.length}</span>`,got.length?`<div class="trr28">${got.slice(0,12).map(t=>`<span class="tri28" title="${esc(t.name)}">${trophySVG(t)}</span>`).join('')}</div>`:`<p class="sub">Your first trophy comes at Bronze.</p>`,see28('id="troombtn"'))}
function photos28(){const L=ph27List().slice(-4).reverse();return card28('ph28','Progress photos',L.length?`<div class="phr28">${L.map(x=>`<button class="phg27" data-ph28="${esc(x.pid)}"><span class="ph27" data-ph27="${esc(x.pid)}">${PH27[x.pid]&&PH27[x.pid]!=='gone'?`<img src="${PH27[x.pid]}" alt="">`:'<i class="sh27"></i>'}</span><small>${fd28(x.date)}</small></button>`).join('')}</div>`:`<p class="sub">Add a selfie when you post a workout to your crew. It’s kept here on your phone.</p>`,L.length?see28('id="ph27libbtn"'):'')}
/* monthly coach note: one cheap AI call a month, cached */
let cm28=0;
function coach28(){if(typeof aiReady!=='function'||!aiReady())return '';const mo=today().slice(0,7);AI23.monthly=AI23.monthly||{};const t=AI23.monthly[mo];
 if(t)return `<div class="card s28 cn28"><div class="cn27">${ic27('spark')}<div><b>Coach · ${MON[toD(today()).getMonth()]}</b><p>${esc(t)}</p></div></div></div>`;
 const rs=realSessions();if(rs.length<3||cm28===mo||cm28==='x')return '';cm28=mo;
 const now=today(),d30=iso(new Date(toD(now).getTime()-30*864e5)),d60=iso(new Date(toD(now).getTime()-60*864e5)),A=rs.filter(s=>s.date>d30),B=rs.filter(s=>s.date>d60&&s.date<=d30),D=pf28Data(),mus={};
 A.forEach(s=>Object.entries(muscleSets(s)).forEach(([k,n])=>mus[k]=(mus[k]||0)+n));
 const lifts=Object.values(D.L).sort((a,b)=>b.n-a.n).slice(0,8).map(o=>({n:o.name,e:Math.round(o.best.e),p:Math.round(Math.max(0,...o.hist.filter(x=>x[0]<=d30).map(x=>x[1])))}));
 vaultCall('/ai/monthly',acctBody({mo,n:new Set(A.map(s=>s.date)).size,sets:A.reduce((x,s)=>x+sSets(s),0),vol:Math.round(A.reduce((x,s)=>x+sVol(s),0)),prs:A.reduce((x,s)=>x+sessStats(s).prs.length,0),pn:new Set(B.map(s=>s.date)).size,psets:B.reduce((x,s)=>x+sSets(s),0),pvol:Math.round(B.reduce((x,s)=>x+sVol(s),0)),unit:U(),lifts,mus}))
  .then(r=>{if(r&&r.text){AI23.monthly[mo]=r.text;const ks=Object.keys(AI23.monthly).sort();while(ks.length>6)delete AI23.monthly[ks.shift()];aiSave(AI23);render()}}).catch(()=>{cm28='x'});
 return ''}
function yir28(){return new Date().getMonth()===11?`<button class="card s28 yi28" id="openyir">${ic27('film')}<div><b>Your ${new Date().getFullYear()} Year in review is ready</b><span class="sub">Your year in lifting, start to finish</span></div><span class="chev">›</span></button>`:''}

{const _rp=renderProfile16;renderProfile16=function(){const h=_rp.apply(this,arguments);try{const cut=h.indexOf('<div class="tiles four life16">');if(cut<0)return h;
 const E=engine(),D=pf28Data(),S=k=>!hid28(k);let o=h.slice(0,cut);
 if(S('rank'))o+=rank28();if(S('high'))o+=high28(D,E);if(S('coach'))o+=coach28();if(S('goal'))o+=goal28();if(S('then'))o+=then28(D);if(S('big3'))o+=big328(D);if(S('lvl'))o+=lvl28(D,true);if(S('prs'))o+=prs28(D);if(S('month'))o+=month28(D);if(S('recent'))o+=recent28();if(S('near'))o+=near28();if(S('heat'))o+=heat28(D);if(S('mus'))o+=mus28();if(S('tro'))o+=tro28();if(S('photos'))o+=photos28();o+=yir28();
 o+=`<button class="pset28" id="pset28open">${ic27('gear')} Profile settings</button>`;return o}catch(e){console.error(e);return h}}}

/* crewmates see the same showcase sections */
{const _pp=profPage;profPage=function(id){let h=_pp.apply(this,arguments);try{if(!h)return h;const me=id===myId();let D=null;
 if(me){const s=pf28Share();D=s}else{const m=memberOf2(id);D=m&&m.profile&&m.profile.pf28}
 if(!D)return h;let x='';x+=then28(D)+big328(D)+lvl28(D,false)+prs28(D)+month28(D)+heat28(D);if(!x)return h;
 return h+`<div class="glbl" style="margin:16px 2px 8px">${me?'WHAT YOUR CREW SEES':'SHOWCASE'}</div><div class="cv28">${x}</div>`}catch(e){return h}}}

/* ---------- profile settings ---------- */
function pset28(){const bl=st.plan?boxesLeft().length:0,H=st.set.hide28||{};
 return `<div class="ghd"><h2>Profile settings</h2><button class="btn sm" id="sheetx">Done</button></div>
 <div class="card rows15">${row16('data-go28="edit"','✎','Name, motto and details','Edit')}${row16('data-go28="pins"','📌','Pinned medals','Pick 3')}${row16('data-go28="locker"',bl?'🎁':'🎨','Customize',bl?bl+' mystery box'+(bl>1?'es':''):'Plates, themes, effects, sounds')}${st.cs?row16('data-go28="crew"','👀','Preview as crew','What your crew sees'):''}</div>
 <div class="glbl" style="margin:16px 2px 6px">BODYWEIGHT</div><div class="card bw28"><div class="row2"><input id="bwin" class="field" type="number" inputmode="decimal" min="0" max="999" value="${st.set.bw||''}" placeholder="Your bodyweight"><span class="bwu28">${U()}</span></div><p class="sub" style="margin:8px 2px 0">Used for strength levels and lift ranks.</p></div>
 <div class="glbl" style="margin:16px 2px 6px">SHOW ON YOUR PROFILE</div><div class="card tgl28">${SECS28.map(([k,l])=>`<button class="tg28" data-hide28="${k}" aria-pressed="${!H[k]}"><span>${l}</span><i class="sw28"><b></b></i></button>`).join('')}</div>
 <div class="glbl" style="margin:16px 2px 6px">MORE</div><div class="card rows15">${row16('data-go28="badges"','🏅','Badges',engine().B.filter(b=>b.earned).length+' earned')}${row16('data-go28="troom"','🏆','Trophy room','')}${feat('season')?row16('data-go28="pass"','🎟','Season pass','Level '+engine().lvl+' of 30'):''}${row16('data-go28="ladder"','🪜','Rank ladder','')}${row16('data-go28="life"','📈','Lifetime stats','')}${row16('data-go28="yir"','🎬',new Date().getFullYear()+' Year in review',new Date().getMonth()===11?'Ready':'Preview')}</div>`}
{const _s=sheet15;sheet15=function(sh){if(sh&&sh.type==='pset28')return pset28();return _s.apply(this,arguments)}}
function edit28(){const P=JSON.parse(JSON.stringify(st.set.prof||{}));return {type:'profedit',p:P,av:{...(st.set.av||{})},flex:(st.set.flex||['best','streak','vol']).slice(),rim:st.set.rim||''}}
async function sheetClick28(b){const sh=st.sheet,d=b.dataset;if(!sh||sh.type!=='pset28')return false;
 if(d.hide28){const H=st.set.hide28=st.set.hide28||{};H[d.hide28]=!H[d.hide28];if(!H[d.hide28])delete H[d.hide28];saveSet();renderSheet();render();if(crewOn())setTimeout(()=>{try{syncCrew(true)}catch(e){}},300);return true}
 if(d.go28){const back={type:'pset28'},g=d.go28;
  if(g==='yir'){st.sheet=null;renderSheet();openYear();return true}
  const M={edit:edit28(),pins:{type:'pinpick'},locker:{type:'locker'},crew:{type:'member',id:myId()},badges:{type:'badges16',cat:'mile'},troom:{type:'troom'},pass:{type:'pass'},ladder:{type:'psec',sec2:'ladder'},life:{type:'psec',sec2:'life'}};
  if(M[g]){st.sheet={...M[g],back};renderSheet();return true}}
 return false}
async function click28(b){const d=b.dataset;
 if(b.id==='profedit2'||b.id==='pset28open'){st.sheet={type:'pset28'};renderSheet();return true}
 if(d.rk28){const r=$('rankbtn');if(r)r.click();return true}
 if(d.go28==='bw'){st.sheet={type:'pset28'};renderSheet();setTimeout(()=>{const i=$('bwin');if(i)i.focus()},300);return true}
 if(d.ph28){const x=ph27List().find(q=>q.pid===d.ph28);st.sheet={type:'ph27view',pid:d.ph28,loc:x||{}};renderSheet();return true}
 return false}
/* the bodyweight box re-renders the profile when it changes */
function boot28(){try{$('sheet').addEventListener('change',e=>{if(e.target.id==='bwin'&&st.sheet&&st.sheet.type==='pset28'){render();if(crewOn())setTimeout(()=>{try{syncCrew(true)}catch(x){}},300)}})}catch(e){}}
