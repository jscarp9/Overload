/* ========================================================================
   Overload: Settings and onboarding

   Settings pages, onboarding, the welcome splash and tour, privacy.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

/* ---------- settings + onboarding ---------- */
function tog(id, on, label, sub) {
  return `<div class="tog"><span>${label}${sub?`<div class="sub" style="font-weight:500">${sub}</div>`:''}</span><button class="sw" data-tog="${id}" aria-pressed="${!!on}" aria-label="${label}"></button></div>`
}
function renderSettings() {
  const th = st.set.theme || 'auto';
  if (st.set.acct && !st.csnaps && !st._csl) {
    st._csl = 1;
    vaultCall('/snaps', acctBody({})).then(r => {
      st.csnaps = r.days || [];
      st._csl = 0;
      if (st.settings) render()
    }).catch (() => {
      st._csl = 0;
      st.csnaps = []
    })
  }
  if (st.snaps === null) IDB.get('snaps').then(x => {
    st.snaps = x || [];
    if (st.settings) render()
  });
  return `<div class="ghd"><h2>Settings</h2><button class="btn sm go" id="sclose">Done</button></div>
<div class="secl">LOOK</div><div class="card"><label class="glbl" for="sname">YOUR NAME</label><input class="field" id="sname" value="${esc(st.set.name||'')}" placeholder="Name" style="margin:8px 0 14px"><span class="glbl">THEME</span><div class="seg">${[['light','☀️ Light'],['dark','🌙 Dark'],['auto','Auto']].map(([k,l])=>`<button data-theme-set="${k}" aria-pressed="${th===k}">${l}</button>`).join('')}</div><span class="glbl">UNITS</span><div class="seg">${['lb','kg'].map(u=>`<button data-unit="${u}" aria-pressed="${U()===u}">${u}</button>`).join('')}</div>${tog('splash',st.set.splash!==false,'Welcome screen','Quick intro when you open the app')}</div>
<div class="secl">WORKOUT</div><div class="card">${tog('autoRest',st.set.autoRest,'Auto rest timer','Starts when you finish the last exercise in a round')}${tog('sound',st.set.sound,'Sound when rest ends')}${tog('vibe',st.set.vibe,'Vibrate when rest ends','Android only. iPhone doesn’t allow it')}${tog('rir',st.set.rir,'Track RIR','Adds a reps in reserve box to each set')}${tog('awake',st.set.awake,'Keep screen on during workouts','So the rest timer stays on screen')+(pushOK()||/iPhone|iPad/i.test(navigator.userAgent)?tog('push',!!st.set.push,'Rest alerts on lock screen','Get a ping when rest is over, even with your phone locked'):'')}</div>
<div class="secl">DATA</div><div class="card"><h3>Backup</h3><p class="sub">Your log, plan and notes live on this phone. Save a backup file every week or two to Files, iCloud or a text to yourself.${st.set.lastBackup?` Last backup ${Math.floor((Date.now()-st.set.lastBackup)/864e5)} days ago.`:''}</p><div class="exfoot"><button class="btn go" id="bkfile">💾 Save backup file</button><label class="btn" for="rsfile" style="cursor:pointer">📂 Restore from file</label><input type="file" id="rsfile" accept=".json,application/json,text/plain" class="hide"></div><details style="margin-top:8px"><summary>Copy and paste instead</summary><button class="btn" id="bk">Copy backup</button><textarea id="rs" style="margin-top:10px" placeholder="Paste a backup here to restore"></textarea><button class="btn" id="rsb" style="margin-top:8px">Restore</button></details><div class="sub" id="bkmsg" style="margin-top:6px"></div></div>${settings15()}${settings14()}${acctCard()}<div class="card"><h3>Restore from a past day</h3><p class="sub">The app saves a snapshot of everything once a day and keeps the last 7. Tap one twice to roll back to it.</p>${st.snaps===null?'<p class="sub">Loading…</p>':st.snaps.length?`<div class="exfoot">${st.snaps.slice().reverse().map(d=>`<button class="btn sm" data-snap="${d}">${st.confirm==='s'+d?'Tap again to restore':DN[dayIdx(d)]+' '+md(d)}</button>`).join('')}</div>`:'<p class="sub">No snapshots yet. The first one saves today.</p>'}${st.set.acct?(st.csnaps?`<div class="glbl" style="margin-top:12px">CLOUD · LAST 30 DAYS</div><div class="exfoot">${st.csnaps.map(d=>`<button class="btn sm" data-csnap="${d}">${st.confirm==='c'+d?'Tap again to restore':DN[dayIdx(d)]+' '+md(d)}</button>`).join('')||'<span class="sub">None yet</span>'}</div>`:'<p class="sub">Loading cloud copies…</p>'):''}</div><div class="card">${tog('count',localStorage.getItem('skipgc')!=='t','Count my visits','Turn off so your own opens don\u2019t show up in the visitor stats')}</div><div class="card"><h3>Start over</h3><p class="sub">Erases every workout, note and your plan from this phone.</p><button class="btn" id="wipe" style="color:var(--red)">${st.confirm==='wipe'?'Tap again to erase everything':'Erase all data'}</button></div>
<div class="foot">Overload ${VERSION} · <button class="btn sm" id="shownews" style="min-height:30px">What's new</button><br>© 2026 Jake Scarpa. All rights reserved.</div>`
}
function renderOnboard() {
  return `<section class="obhero">${plateSVG('black','idle',{big:true,label:'LET’S GO'}).replace('<svg','<svg class="deco"')}<div style="font-weight:700;opacity:.9">Welcome to</div><h1 style="margin:2px 0 8px">Overload</h1><p style="max-width:24em;margin:0 0 14px;font-weight:500">Log every set, beat last week, watch the numbers climb. Pick a plan to start. Every day and exercise can be changed later.</p><input class="field" id="obname" placeholder="Your name (optional)" value="${esc(st.obName)}" style="background:rgba(255,255,255,.92);max-width:260px"></section><div class="tpl">${Object.entries(TEMPLATES).map(([k,t])=>`<button data-start="${k}">${plateSVG(t.c,'done')}<span><b>${t.label}</b><span class="sub">${t.desc}</span></span></button>`).join('')}</div>`
}
/* ---------- 2.0: welcome splash ---------- */
function splash() {
  if (!st.plan || st.set.splash === false) return Promise.resolve();
  const el = document.createElement('div');
  el.id = 'splash';
  const p = pd(), hr = new Date().getHours(), greet = hr < 12 ? 'Good morning' : hr < 18 ? 'Good afternoon' : 'Good evening';
  const w = weekAgg(weekDates(today()));
  let streak = 0;
  for (let i = 0; i < 52; i++) {
    const d = toD(weekDates(today())[0]);
    d.setDate(d.getDate() - i * 7);
    const ws = weekDates(iso(d));
    if (Object.values(st.sessions).some(s => real(s) && s.date >= ws[0] && s.date <= ws[6] && Object.values(s.ex || {}).some(has))) streak++;
    else if (i > 0) break
  }
  const extra = w.prs ? ` · ${w.prs} PR${w.prs>1?'s':''} this week` : streak > 1 ? ` · ${streak} week streak` : '';
  const line = p.type === 'off' ? `Rest day. You earned it.${extra}` : `<b>${esc(p.name)}</b> today${extra}`;
  const ER = engine().rank, neon = ER >= 5;
  const lines = [[.22, .06], [.78, .08]].map(([y, a], i) => {
    let d = '';
    for (let k = 0; k <= 60; k++) {
      const t = k / 60, x = -5 + 110 * t, yy = y * 100 + a * 100 * Math.sin(t * 6.28 + i * 2);
      d += (k ? 'L' : 'M') + x.toFixed(1) + ',' + yy.toFixed(1)
    }
    return `<path d="${d}" fill="none" stroke="${i?'#2563ff':'#ff3d4a'}" stroke-width="${neon?1.1:.5}" opacity="${neon?.95:.6}" ${neon?'style="filter:drop-shadow(0 0 3px '+(i?'#2563ff':'#ff3d4a')+')"':''}/>`
  }).join('');
  el.innerHTML = `<div class="sbg"></div>${welcomeFX()}<svg class="lines" viewBox="0 0 100 100" preserveAspectRatio="none">${lines}</svg><div class="sp">${plateSVG(ER>=7?'gold':'black',ER>=7?'done':'idle',{big:true,label:'OVERLOAD'})}<span class="sring"></span></div><div class="swm">OVER<i>LOAD</i></div><div class="sl">${st.set.name?esc(greet+', '+st.set.name)+'<br>':''}${line}</div>`;
  document.body.appendChild(el);
  return new Promise(res => {
    let done = false;
    const end = () => {
      if (done) return;
      done = true;
      el.classList.add('out');
      setTimeout(() => {
        el.remove();
        res()
      }, 350)
    };
    el.addEventListener('click', end);
    setTimeout(end, matchMedia('(prefers-reduced-motion: reduce)').matches ? 600 : 1250)
  })
}
// Anatomy figure generator. viewBox 0 0 200 400, draws left half and mirrors it.
function anatomy(side, L, dk, C) {
  const lv = k => L[k] || 0;
  const g = `<defs>
 <radialGradient id="am0" cx=".35" cy=".3" r=".85"><stop offset="0" stop-color="${dk?'#4a4f5b':'#f3f4f7'}"/><stop offset=".6" stop-color="${dk?'#30343d':'#d9dce2'}"/><stop offset="1" stop-color="${dk?'#1c1f26':'#b9bec8'}"/></radialGradient>
 <radialGradient id="am1" cx=".35" cy=".3" r=".85"><stop offset="0" stop-color="#ffd0a8"/><stop offset=".5" stop-color="#ff8a3d"/><stop offset="1" stop-color="#e0531c"/></radialGradient>
 <radialGradient id="am2" cx=".35" cy=".3" r=".85"><stop offset="0" stop-color="#ff9a9e"/><stop offset=".45" stop-color="#f0303a"/><stop offset="1" stop-color="#a50d17"/></radialGradient>
 <linearGradient id="ask" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="${dk?'#1d2027':'#c3c7cf'}"/><stop offset=".5" stop-color="${dk?'#2c3039':'#e4e6ea'}"/><stop offset="1" stop-color="${dk?'#1d2027':'#c3c7cf'}"/></linearGradient>
 <filter id="agw" x="-40%" y="-40%" width="180%" height="180%"><feGaussianBlur stdDeviation="2.4" result="b"/><feFlood flood-color="#ff2a35" flood-opacity=".55"/><feComposite in2="b" operator="in" result="c"/><feMerge><feMergeNode in="c"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
 </defs>`;
  const M = (d, k, fib) => {
    const v = lv(k);
    return `<g ${v>=2?'filter="url(#agw)"':''}><path d="${d}" fill="${C&&C[k]?C[k]:'url(#am'+v+')'}" stroke="${v?'rgba(120,10,10,.55)':(dk?'#4b505c':'#9aa0ab')}" stroke-width=".7" stroke-linejoin="round"/>${(fib||[]).map(f=>`<path d="${f}" fill="none" stroke="${v?'rgba(255,255,255,.38)':(dk?'rgba(255,255,255,.1)':'rgba(255,255,255,.7)')}" stroke-width=".55" stroke-linecap="round"/>`).join('')}</g>`
  };
  const sil = 'M100 58 L91 58 C84 64 68 67 56 71 C44 75 39 86 39 99 C37 114 37 127 39 138 C35 155 33 170 35 184 C34 194 37 202 42 203 C48 203 50 195 50 186 C54 168 58 152 62 139 C64 128 66 118 68 109 C70 130 70 150 68 168 C65 180 63 190 64 202 C61 230 62 255 67 272 C63 292 61 322 65 352 C64 362 60 372 70 375 L89 375 C91 365 90 354 90 342 C92 318 94 298 92 282 C96 262 98 232 98 204 L100 200 Z';
  let half = '', center = '';
  center += `<ellipse cx="100" cy="30" rx="16" ry="20" fill="url(#ask)" stroke="${dk?'#4b505c':'#9aa0ab'}" stroke-width=".7"/><path d="M90 46 L89 60 L111 60 L110 46 Z" fill="url(#ask)" stroke="${dk?'#4b505c':'#9aa0ab'}" stroke-width=".7"/>`;
  half += `<path d="${sil}" fill="url(#ask)" stroke="${dk?'#4b505c':'#8d939e'}" stroke-width=".9"/>`;
  if (side === 'front') {
    half += M('M91 59 C84 63 74 67 66 70 L80 71 L92 66 Z', 'trap');
    half += M('M64 70 C52 70 44 78 42 92 C44 97 47 98 49 96 C50 86 54 78 60 74 Z', 'sdelt', ['M60 73 C52 78 47 85 46 93']);
    half += M('M66 71 C58 74 52 81 50 95 C54 99 59 98 62 94 C63 86 66 80 71 75 Z', 'fdelt', ['M64 75 C58 81 55 88 54 95']);
    half += M('M99 72 L99 107 C90 112 76 111 66 103 C61 97 62 87 70 77 C80 73 90 72 99 72 Z', 'chest', ['M98 76 C86 78 74 84 66 96', 'M98 84 C86 86 75 92 68 100', 'M98 93 C88 96 78 100 72 104', 'M98 101 C90 104 82 106 76 106']);
    half += M('M49 98 C44 109 44 122 47 133 C52 138 58 136 60 130 C62 118 61 107 58 99 C55 96 51 96 49 98 Z', 'bic', ['M53 101 C51 112 51 124 53 133']);
    half += M('M45 101 C41 112 41 124 43 134 L47 134 C44 123 44 110 48 99 Z', 'tri');
    half += M('M45 138 C39 152 37 168 39 182 C42 186 46 186 48 183 C51 170 55 155 58 141 C55 136 49 135 45 138 Z', 'fore', ['M50 141 C46 154 43 168 42 181']);
    for (let r = 0; r < 4; r++) {
      const y = 113 + r * 15, h = r === 3 ? 20 : 13;
      half += M(`M99 ${y} L99 ${y+h} L${r===3?92:89} ${y+h} C${88} ${y+h-4} 88 ${y+4} 89 ${y} Z`, 'abs')
    }
    half += M('M87 110 C79 116 74 130 75 148 C77 162 81 172 89 180 L89 112 Z', 'abs', ['M84 118 C80 128 79 140 80 152', 'M86 140 C83 150 83 160 86 170']);
    [[76, 104], [77, 113], [78, 122]].forEach(([x, y]) => {half += M(`M${x} ${y} L${x+8} ${y+2} L${x+7} ${y+7} L${x} ${y+5} Z`, 'abs')});
    half += M('M73 186 C65 210 65 238 69 262 C73 268 79 266 81 258 C82 236 82 211 80 191 Z', 'quad', ['M75 195 C70 215 70 240 73 258']);
    half += M('M82 188 C79 214 81 240 85 262 L91 262 C94 236 94 210 92 190 Z', 'quad', ['M87 194 C86 216 87 240 88 258']);
    half += M('M89 234 C96 242 98 255 94 268 C88 268 85 260 85 251 Z', 'quad');
    half += M('M92 189 L98 192 L98 228 C96 236 92 236 89 230 C90 216 91 202 92 189 Z', 'quad');
    half += `<ellipse cx="83" cy="273" rx="7" ry="5" fill="url(#ask)" stroke="${dk?'#4b505c':'#9aa0ab'}" stroke-width=".6"/>`;
    half += M('M71 281 C65 301 65 325 69 350 L77 352 C79 327 81 302 81 283 Z', 'calf', ['M73 290 C70 310 70 330 72 348']);
    half += M('M83 283 C85 307 85 331 83 352 L89 352 C91 327 91 301 89 284 Z', 'calf');
  } else {
    half += M('M100 60 L100 120 C94 112 86 96 80 84 C74 78 68 74 62 72 C74 68 86 64 91 59 Z', 'trap', ['M99 66 C90 70 80 74 68 74', 'M99 80 C93 82 87 84 81 86', 'M99 96 C95 98 91 100 88 102']);
    half += M('M62 71 C52 71 44 78 42 92 C45 97 49 97 51 94 C53 84 58 78 66 75 Z', 'rdelt', ['M60 74 C53 79 49 86 47 93']);
    half += M('M67 76 C76 80 82 86 86 96 C80 100 72 100 66 96 C64 89 64 82 67 76 Z', 'mid', ['M70 82 C76 86 80 91 83 96']);
    half += M('M86 97 C82 108 80 120 80 128 C74 128 68 122 64 110 C63 104 64 100 66 97 C72 101 80 101 86 97 Z', 'lat');
    half += M('M64 102 C58 118 60 140 70 160 C78 166 86 166 92 162 C90 150 84 140 80 129 C72 128 66 120 64 102 Z', 'lat', ['M66 112 C68 130 74 146 84 158', 'M64 124 C68 138 74 150 80 158']);
    half += M('M99 124 L99 176 C95 176 92 172 91 166 C92 152 93 138 93 126 Z', 'lowb', ['M96 130 L96 170']);
    half += M('M48 98 C43 109 43 122 46 134 C51 138 57 137 59 131 C61 118 60 107 57 99 C54 96 50 96 48 98 Z', 'tri', ['M52 101 C50 112 50 124 52 133', 'M56 103 C55 113 55 123 57 130']);
    half += M('M45 138 C39 152 37 168 39 182 C42 186 46 186 48 183 C51 170 55 155 58 141 C55 136 49 135 45 138 Z', 'fore');
    half += M('M99 182 L99 218 C88 222 74 216 68 204 C66 194 68 186 74 180 C82 178 92 178 99 182 Z', 'glute', ['M96 186 C86 190 78 196 72 204']);
    half += M('M70 220 C65 240 66 258 70 270 L82 270 C82 252 81 236 80 220 Z', 'ham', ['M73 226 C70 242 70 256 72 266']);
    half += M('M81 220 C83 240 84 256 85 270 L95 270 C96 252 96 236 96 220 Z', 'ham', ['M88 226 C89 242 90 256 90 266']);
    half += M('M69 280 C62 296 63 314 70 326 C76 328 81 322 82 312 C83 300 81 288 78 280 Z', 'calf', ['M72 288 C68 300 68 312 71 322']);
    half += M('M80 280 C85 290 88 302 88 314 C87 322 84 326 81 324 C82 310 82 294 80 280 Z', 'calf');
    half += `<path d="M78 328 L80 352 L86 352 L86 328 Z" fill="url(#ask)" stroke="${dk?'#4b505c':'#9aa0ab'}" stroke-width=".6"/>`;
  }
  return g + `<g>${half}</g><g transform="translate(200 0) scale(-1 1)">${half}</g>` + center;
}
/* ---------- settings additions ---------- */
function settings14() {
  return `<div class="card"><h3>Safety net</h3><p class="sub">Deleted things wait in the Trash for 30 days. Before every update the app saves a full copy and puts back anything that goes missing.</p><div class="exfoot"><button class="btn" id="trashbtn">🗑 Trash</button><button class="btn" id="exportall">📦 Export everything</button></div></div>`
}
/* ---------- welcome tour ---------- */
const TOUR = [['🏋️', 'Today', 'Your workout for today. Tap a set to log it and the big plate fills up as you go. Rest timer starts on its own.'], ['👥', 'Crew', 'Lift with friends. Post workouts, chat, and take down a boss together every week.'], ['🪪', 'Profile', 'Your rank, badges and trophies. Make it yours in Edit profile and Customize.'], ['📊', 'Stats', 'Your progress, PRs and how every lift is trending.'], ['📋', 'Plan', 'Set your weekly split once. Each day gets its own plate and it repeats every week.']];
function tourSheet(sh) {
  const i = sh.i || 0, t = TOUR[i], last = i === TOUR.length - 1;
  return `<div class="tour16"><div class="t16i">${t[0]}</div><h2>${t[1]}</h2><p class="sub">${t[2]}</p><div class="t16d">${TOUR.map((_,j)=>`<i class="${j===i?'on':''}"></i>`).join('')}</div>
 <div class="exfoot" style="margin-top:14px"><button class="btn" id="${i?'tourback':'sheetx'}" style="flex:1;min-height:50px">${i?'Back':'Skip'}</button><button class="btn acc" id="tournext" style="flex:1;min-height:50px">${last?'Let’s lift':'Next'}</button></div></div>`
}
function tourDone() {
  st.set.tour16 = 1;
  saveSet()
}
function tourCheck() {
  try {
    if (st.set.tour16) return;
    if (!st.plan) {
      setTimeout(tourCheck, 1500);
      return
    }
    if (realSessions().length >= 3) {
      tourDone();
      return
    }
    if (st.sheet) {
      setTimeout(tourCheck, 1500);
      return
    }
    tourDone();
    st.sheet = {type: 'tour', i: 0};
    renderSheet()
  } catch (e) {}
}
/* ---------- Settings: a short list, one page per section ---------- */
const SET17 = [['you', '🙂', 'Name, theme and units', (c, t) => /YOUR NAME/.test(t)], ['workout', '🏋️', 'Workout', (c, t) => /Auto rest timer|Rest alerts|Keep screen on|RIR/.test(t) && !/YOUR NAME/.test(t)], ['sync', '☁️', 'Account and sync', (c, t) => /Back up and sync|Already have an account|Signed in|passkey|Recovery code/i.test(t)], ['data', '🛟', 'Backup and restore', (c, t) => /^(Backup|Safety net|Restore from a past day)/.test(t)], ['founder', '👑', 'Founder', (c, t) => /Owner dashboard/.test(t)], ['help', '👋', 'Help', (c, t) => /app tour/.test(t)], ['danger', '⚠️', 'Start over', (c, t) => /^Start over/.test(t)]];
{
  const _rs = renderSettings;
  renderSettings = function() {
    const html = _rs();
    try {
      const T = document.createElement('div');
      T.innerHTML = html;
      const kids = [...T.children], head = kids.shift(), groups = {}, other = [];
      kids.forEach(c => {
        const t = (c.textContent || '').trim();
        if (/Count my visits/.test(t)) return;
        if (c.classList.contains('glbl') || /^(LOOK|WORKOUT|DATA|ACCOUNT|HELP)$/.test(t)) return;
        const g = SET17.find(x => x[3](c, t));
        (g ? (groups[g[0]] = groups[g[0]] || []) : other).push(c.outerHTML)
      });
      const bk = bkDue() && !st.set.acct;
      if (st.set17 && groups[st.set17]) {
        const g = SET17.find(x => x[0] === st.set17);
        return `<div class="ghd"><button class="btn sm" id="set17back">‹ Settings</button><h2 style="margin:0">${g[2]}</h2><span></span></div>` + groups[st.set17].join('')
      }
      return head.outerHTML + (bk ? `<button class="card bk17" data-set17="data"><span>💾</span><div><b>Back up your log</b><div class="sub">${st.set.lastBackup?'Last backup '+Math.floor((Date.now()-st.set.lastBackup)/864e5)+' days ago':'You haven’t backed up yet'}. Takes 2 seconds</div></div><span class="chev">›</span></button>` : '') +
      `<div class="card rows15">${SET17.filter(g=>groups[g[0]]).map(g=>row16(`data-set17="${g[0]}"`,g[1],g[2],'')).join('')}</div>` + other.join('')
    } catch (e) {
      console.warn('set17', e);
      return html
    }
  }
}

/* ---------- privacy ---------- */
function privacyHTML() {
  return `<div class="ghd"><h2>Privacy</h2><button class="btn sm" id="sheetx">Done</button></div><div class="priv18">
<p class="sub">Last updated November 2026. Plain version, no legal speak.</p>
<h3>What stays on your phone</h3><p>Your workouts, plan, notes and settings are saved on your device. If you never sign in or join a crew, nothing leaves your phone except the basic usage counts below.</p>
<h3>What goes to the Overload server</h3><p>If you sign in, your log is backed up so you can restore it on another phone. If you join a crew, your name, profile, rank, workout summaries, posts, reactions and chat messages are shared with that crew. The server runs on Cloudflare.</p>
<h3>Workout photos</h3><p>A photo you add to a crew post is made smaller and stored on the Overload server, where only members of that crew can see it. It is removed from the server 6 months after you post it, or when it is taken off the feed (it waits in the crew Trash for 30 days first). Your own photos also stay on your phone in Progress photos until you delete them there. Photos are never sent to AI.</p><h3>Usage stats</h3><p>The app counts how it is used so it can get better: a random device ID, phone type, app version, whether it runs from the Home Screen, and counts like workouts and sets. If you are signed in or in a crew, your name can show next to your device in the owner’s private dashboard.</p>
<h3>Overload AI</h3><p>AI is off until you allow it. When you message Coach, your message, your plan and your last few workouts are sent. When you ask for form tips, only the exercise name is sent. Once a week your crew’s weekly totals (first names, days trained, sets, volume, PRs) are sent to write the crew recap. When you use the plan builder, the photos or text you add, your lift names and recent best sets are sent through the Overload server to Anthropic’s Claude to build your plan. If coach notes are on, the sets, weights and reps from a finished workout or week, and once a month your last 30 days of totals and best lifts, are sent the same way to write a short note. Your name, crew, chats and account details are never sent. Anthropic processes this only to answer the request. Turn it off any time in Settings → Overload AI.</p><h3>What we never do</h3><p>No ads, no selling or renting your data, no tracking across other apps or sites.</p>
<h3>Your choices</h3><p>You can stay signed out, leave a crew, block people and export everything in Settings → Backup and restore. To have your account data removed from the server, contact Jake Scarpa, the app owner.</p>
<p class="sub">© 2026 Jake Scarpa. All rights reserved.</p></div>`
}
