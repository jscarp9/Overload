/* ========================================================================
   Overload: Stats and progress

   Progress charts, stats, notes, muscle map, mastery, lift ranks, goals.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

/* ---------- progress ---------- */
function allIds() {
  const ids = [];
  Object.keys(exMap()).forEach(id => ids.push(id));
  Object.values(st.sessions).forEach(s => Object.keys(s.ex || {}).forEach(id => {if (!ids.includes(id)) ids.push(id)}));
  return ids
}
function points(id) {
  return sorted().reverse().filter(([, s]) => s.ex && s.ex[id] && s.ex[id].some(full)).map(([k, s]) => {
    const v = s.ex[id].filter(full);
    const b = v.reduce((m, x) => e1(x) > e1(m) ? x : m, v[0]);
    return {s, e: Math.round(e1(b)), b, vol: v.reduce((a, x) => a + x.w * x.r, 0)}
  })
}
function spark(pts, c) {
  if (pts.length < 2) return '<svg viewBox="0 0 80 30"><line x1="0" y1="15" x2="80" y2="15" stroke="#d7d9e0" stroke-dasharray="3 3"/></svg>';
  const v = pts.map(p => p.e), mn = Math.min(...v), mx = Math.max(...v), r = mx - mn || 1;
  const d = pts.map((p, i) => `${i?'L':'M'}${(i/(pts.length-1)*76+2).toFixed(1)},${(26-(p.e-mn)/r*22).toFixed(1)}`).join('');
  return `<svg viewBox="0 0 80 30" preserveAspectRatio="none"><path d="${d}" fill="none" stroke="${c}" stroke-width="2.4" stroke-linejoin="round" stroke-linecap="round" vector-effect="non-scaling-stroke"/></svg>`
}
const mcol = m => ({
  Chest: '#ef2b36', Back: '#2563ff', Shoulders: '#ff7a3d', 'Side delts': '#ff9f1c', 'Rear delts': '#ffbe0b', Biceps: '#7c4dff', Triceps: '#d14dff', Forearms: '#a98bff', Traps: '#00b7ff', Quads: '#16b65b', Hamstrings: '#0fa958', Glutes: '#00c2a8', Calves: '#4fdc8c', Abs: '#23252b', 'Lower back': '#6b7080'
}[m] || '#6b7080');
function renderProg() {
  const ids = allIds();
  const muscles = ['All', ...MUSCLES.filter(m => ids.some(id => exMuscle(id) === m))];
  let h = `<h2>Progress</h2><p class="sub">Every lift you've logged. Tap one for the full story.</p>`;
  if (st.prog) {
    const id = st.prog, pts = points(id), c = mcol(exMuscle(id));
    h += `<div class="card" style="margin-top:12px"><div class="ghd"><h3>${esc(exName(id))}</h3><button class="btn sm" id="pback">All lifts</button></div><div class="sub">${esc(exMuscle(id))}</div>`;
    if (pts.length) {
      const a = pts[0].e, b = pts[pts.length - 1].e, best = pts.reduce((m, p) => p.e > m.e ? p : m, pts[0]), tv = pts.reduce((x, p) => x + p.vol, 0);
      h += `<div class="tiles"><div class="tile"><div class="num">${b}</div><div class="sub">Est. max ${U()}</div></div><div class="tile"><div class="num ${b-a>0?'grn':b-a<0?'redt':''}">${b-a>=0?'+':''}${b-a}</div><div class="sub">Since first log</div></div><div class="tile"><div class="num">${best.b.w}×${best.b.r}</div><div class="sub">Best set · ${md(best.s.date)}</div></div><div class="tile"><div class="num">${kfmt(tv)}</div><div class="sub">Total ${U()} · ${pts.length} sessions</div></div></div>`;
      if (pts.length > 1) h += `<div class="fig" data-c="${c}"><svg id="chart" role="img" aria-label="Estimated max over time"></svg></div>`;
      h += `<table class="hist">${pts.slice().reverse().map(({s,e})=>`<tr><td>${DN[dayIdx(s.date)]} ${md(s.date)}<div class="sub">${real(s)?esc((st.plan.days[DAYS.indexOf(s.day)]||{}).name||''):'Old split'}</div></td><td>${fmt(s.ex[id])}</td><td>${e}</td></tr>`).join('')}</table>`
    } else h += `<p class="sub">No sets logged yet.</p>`;
    return h + `</div><p class="sub">Est. max uses your best set each session: weight × (1 + reps ÷ 30).</p>`
  }
  h += `<div class="seg">${muscles.map(m=>`<button data-pf="${m}" aria-pressed="${st.pfilter===m}">${m}</button>`).join('')}</div><div class="card">`;
  const list = ids.filter(id => st.pfilter === 'All' || exMuscle(id) === st.pfilter).map(id => ({id, pts: points(id)})).sort((a, b) => (b.pts.length ? b.pts[b.pts.length - 1].s.date : '') > (a.pts.length ? a.pts[a.pts.length - 1].s.date : '') ? 1 : -1);
  h += list.length ? list.map(({id, pts}) => {
    const b = pts.length ? pts[pts.length - 1].e : 0, a = pts.length ? pts[0].e : 0;
    return `<button class="lift" data-lift="${esc(id)}"><span><b>${esc(exName(id))}</b><span class="sub">${esc(exMuscle(id))}${pts.length>1?` · <span class="${b-a>=0?'grn':'redt'}">${b-a>=0?'+':''}${b-a}</span>`:''}</span></span>${spark(pts,mcol(exMuscle(id)))}<span class="num">${b||'–'}</span></button>`
  }).join('') : '<p class="sub">Nothing logged yet.</p>';
  return h + `</div>`
}
function drawChart() {
  const s = $('chart');
  if (!s || !st.prog) return;
  const W = s.clientWidth, H = s.clientHeight, pts = points(st.prog);
  if (pts.length < 2 || !W) return;
  const c = s.parentNode.dataset.c || '#ef2b36';
  const l = 40, r = 16, t = 14, b = 26, v = pts.map(p => p.e);
  let mn = Math.min(...v), mx = Math.max(...v);
  const pad = Math.max(5, (mx - mn) * .2);
  mn = Math.floor((mn - pad) / 5) * 5;
  mx = Math.ceil((mx + pad) / 5) * 5;
  const X = i => l + (W - l - r) * (i / (pts.length - 1)), Y = x => t + (H - t - b) * (1 - (x - mn) / (mx - mn));
  let o = `<defs><linearGradient id="ar" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${c}" stop-opacity=".3"/><stop offset="1" stop-color="${c}" stop-opacity="0"/></linearGradient></defs>`;
  [mn, Math.round((mn + mx) / 10) * 5, mx].forEach(x => {
    o += `<line x1="${l}" x2="${W-r}" y1="${Y(x)}" y2="${Y(x)}" stroke="#ececf1"/><text x="${l-8}" y="${Y(x)+4}" text-anchor="end">${x}</text>`
  });
  const d = pts.map((p, i) => `${i?'L':'M'}${X(i)},${Y(p.e)}`).join('');
  o += `<path d="${d}L${X(pts.length-1)},${H-b}L${X(0)},${H-b}Z" fill="url(#ar)"/><path d="${d}" fill="none" stroke="${c}" stroke-width="3.2" stroke-linejoin="round" stroke-linecap="round"/>`;
  o += pts.map((p, i) => `<circle cx="${X(i)}" cy="${Y(p.e)}" r="${i===pts.length-1?6:4}" fill="${i===pts.length-1?c:'#fff'}" stroke="${c}" stroke-width="2.4"/>`).join('');
  o += `<text x="${l}" y="${H-5}">${md(pts[0].s.date)}</text><text x="${W-r}" y="${H-5}" text-anchor="end">${md(pts[pts.length-1].s.date)}</text>`;
  s.innerHTML = o
}
/* ---------- stats ---------- */
function weekAgg(wd) {
  let done = 0, planned = 0, sets = 0, vol = 0, prs = 0, dur = 0, dn = 0;
  st.plan.days.forEach((d, i) => {if (d.type !== 'off') {planned++; if (doneOn(wd[i], DAYS[i])) done++}});
  Object.values(st.sessions).forEach(s => {
    if (real(s) && s.date >= wd[0] && s.date <= wd[6]) {
      const ss = sessStats(s);
      sets += ss.sets;
      vol += ss.vol;
      prs += ss.prs.length;
      const el = elapsed(s);
      if (s.start && s.end && el > 0) {
        dur += el;
        dn++
      }
    }
  });
  return {done, planned, sets, vol, prs, avg: dn ? dur / dn : 0}
}
function renderStats() {
  const wd = weekDates(today()), w = weekAgg(wd);
  let h = `<h2>Stats</h2><p class="sub">This week · ${MON[toD(wd[0]).getMonth()]} ${toD(wd[0]).getDate()} to ${MON[toD(wd[6]).getMonth()]} ${toD(wd[6]).getDate()}</p><div class="tiles four"><div class="tile"><div class="num">${w.done}<span class="sub">/${w.planned}</span></div><div class="sub">Sessions</div></div><div class="tile"><div class="num">${w.sets}</div><div class="sub">Hard sets</div></div><div class="tile"><div class="num">${kfmt(w.vol)}</div><div class="sub">${U()} moved</div></div><div class="tile"><div class="num ${w.prs?'redt':''}">${w.prs}</div><div class="sub">PRs</div></div></div>`;
  h += questsHTML();
  if (w.avg) h += `<p class="sub">Average workout this week: <b style="color:var(--ink)">${mins(w.avg)}</b></p>`;
  const m = {}, planned = {};
  Object.values(st.sessions).forEach(s => {
    if (real(s) && s.date >= wd[0] && s.date <= wd[6]) Object.entries(s.ex || {}).forEach(([id, a]) => {
      const k = exMuscle(id);
      m[k] = (m[k] || 0) + a.filter(full).length
    })
  });
  st.plan.days.forEach(d => d.groups.forEach(gr => gr.forEach(e => {planned[e.m] = (planned[e.m] || 0) + (+e.sets || 2)})));
  const keys = MUSCLES.filter(k => planned[k] || m[k]);
  const max = Math.max(1, ...keys.map(k => Math.max(planned[k] || 0, m[k] || 0)));
  h += `<div class="card sec"><h3>Sets by muscle</h3><p class="sub">Logged this week against what your plan calls for.</p>${keys.length?keys.map(k=>`<div class="mrow"><span>${k}</span><span class="t"><i style="width:${Math.round((m[k]||0)/max*100)}%;background:${mcol(k)}"></i></span><b>${m[k]||0}<span class="sub">/${planned[k]||0}</span></b></div>`).join(''):'<p class="sub">Add exercises to your plan to see this.</p>'}</div>`;
  const wk = [];
  for (let i = 7; i >= 0; i--) {
    const d = toD(wd[0]);
    d.setDate(d.getDate() - i * 7);
    const ws = weekDates(iso(d));
    wk.push({lbl: md(ws[0]), a: weekAgg(ws)})
  }
  const vmax = Math.max(1, ...wk.map(x => x.a.vol));
  h += `<div class="card"><h3>Weekly volume</h3><p class="sub">Total ${U()} moved, last 8 weeks. This week in blue.</p><div class="vbars">${wk.map((x,i)=>`<div class="${i===7?'cur':''}"><span>${x.a.vol?kfmt(x.a.vol):''}</span><i style="height:${Math.round(x.a.vol/vmax*100)}%"></i><span>${x.lbl}</span></div>`).join('')}</div></div>`;
  let streak = 0;
  for (let i = 0; i < 104; i++) {
    const d = toD(wd[0]);
    d.setDate(d.getDate() - i * 7);
    const ws = weekDates(iso(d));
    const any = Object.values(st.sessions).some(s => real(s) && s.date >= ws[0] && s.date <= ws[6] && Object.values(s.ex || {}).some(has));
    if (any) streak++;
    else if (i > 0) break
  }
  h += `<div class="card"><div class="ghd"><h3>Consistency</h3><span class="sub"><b style="color:var(--ink);font:900 22px Archivo">${streak}</b> week streak</span></div><div class="heat" style="margin-top:10px"><span></span>${DN.map(d=>`<span style="text-align:center">${d[0]}</span>`).join('')}`;
  const start = toD(wd[0]);
  start.setDate(start.getDate() - 77);
  const td = today();
  for (let w2 = 0; w2 < 12; w2++) {
    const ws = new Date(start);
    ws.setDate(start.getDate() + w2 * 7);
    h += `<span>${ws.getMonth()+1}/${ws.getDate()}</span>`;
    for (let i = 0; i < 7; i++) {
      const x = new Date(ws);
      x.setDate(ws.getDate() + i);
      const ds = iso(x);
      let bg = '';
      Object.values(st.sessions).forEach(s => {
        if (real(s) && s.date === ds && Object.values(s.ex || {}).some(has)) {
          const dd = st.plan.days[DAYS.indexOf(s.day)];
          bg = dd ? PC[colorOf(dd)][0] : '#ef2b36'
        }
      });
      h += `<i class="${ds===td?'td':''}" style="${bg?'background:'+bg:''}"></i>`
    }
  }
  h += `</div></div>`;
  const prs = allIds().map(id => {
    let best = null, bd = '', bb = false;
    Object.values(st.sessions).forEach(s => (s.ex && s.ex[id] || []).forEach(x => {if (full(x) && (!best || e1(x) > e1(best))) {best = x; bd = s.date; bb = !real(s)}}));
    return {id, best, bd, bb}
  }).filter(p => p.best).sort((a, b) => e1(b.best) - e1(a.best));
  const wk7 = iso(new Date(Date.now() - 7 * 864e5));
  h += `<div class="card"><h3>PR board</h3><p class="sub">Best estimated max for every lift.</p>${prs.length?prs.map(p=>`<div class="prrow"><div><b>${esc(exName(p.id))}</b><div class="sub">${p.best.w}×${p.best.r} · ${md(p.bd)}${p.bb?' · old split':p.bd>=wk7?' · <span class="redt" style="font-weight:800">new</span>':''}</div></div><span class="num">${Math.round(e1(p.best))}</span></div>`).join(''):'<p class="sub">Log a session to start your board.</p>'}</div>`;
  return h
}
/* ---------- notes ---------- */
const NC = ['red', 'yellow', 'blue', 'green', 'white'];
function renderNotes() {
  let h = `<div class="ghd"><h2>Notes</h2><button class="btn sm acc" id="nadd">+ New note</button></div><p class="sub">Goals, form cues, PR targets. Pinned notes stay on top.</p>`;
  if (st.editNote) {
    const n = st.editNote;
    h += `<div class="card" style="margin-top:12px"><input class="field" id="ntitle" placeholder="Title" value="${esc(n.title)}" aria-label="Note title"><textarea id="ntext" style="margin-top:8px;min-height:120px" placeholder="Write it down…" aria-label="Note text">${esc(n.text)}</textarea><div class="swatches">${NC.map(c=>`<button class="c-${c}" data-col="${c}" aria-pressed="${n.color===c}" aria-label="${c}"></button>`).join('')}</div><div class="exfoot"><button class="btn go" id="nsave">Save</button><button class="btn" id="npin">${n.pin?'Unpin':'Pin'}</button>${n.id?`<button class="btn" id="ndel">${st.confirm==='n'?'Tap to delete':'Delete'}</button>`:''}<button class="btn" id="ncancel">Cancel</button></div></div>`
  }
  const list = st.notes.slice().sort((a, b) => ((b.pin ? 1 : 0) - (a.pin ? 1 : 0)) || (b.ts - a.ts));
  h += list.length ? `<div class="notes">${list.map(n=>`<button class="note c-${NC.includes(n.color)?n.color:'yellow'}" data-note="${n.id}">${n.pin?'<span class="pin">PINNED</span>':''}<b>${esc(n.title||'Untitled')}</b><p>${esc(n.text)}</p></button>`).join('')}</div>` : `<div class="card empty" style="margin-top:12px"><h3>Your board is empty</h3><p>Try a goal like “Incline 245×6 by January” or a cue like “Elbows tucked on JM press”.</p></div>`;
  const sn = sorted().filter(([, s]) => (s.notes && s.notes.trim()) || Object.values(s.exNotes || {}).some(v => v && v.trim())).slice(0, 20);
  if (sn.length) h += `<div class="card sec"><h3>Session notes</h3>${sn.map(([,s])=>`<div class="tl"><span class="sub">${DN[dayIdx(s.date)]} ${md(s.date)} · ${esc((st.plan.days[DAYS.indexOf(s.day)]||{}).name||'')}</span>${s.notes&&s.notes.trim()?`<p>${esc(s.notes)}</p>`:''}${Object.entries(s.exNotes||{}).filter(([,v])=>v&&v.trim()).map(([id,v])=>`<p><b>${esc(exName(id))}:</b> ${esc(v)}</p>`).join('')}</div>`).join('')}</div>`;
  return h
}
/* ---------- 0.10: muscle map + post-workout analysis ---------- */
const MKEY = {
  Chest: ['chest'], Back: ['lat', 'mid'], Shoulders: ['fdelt'], 'Side delts': ['sdelt'], 'Rear delts': ['rdelt'], Biceps: ['bic'], Triceps: ['tri'], Forearms: ['fore'], Traps: ['trap'], Quads: ['quad'], Hamstrings: ['ham'], Glutes: ['glute'], Calves: ['calf'], Abs: ['abs'], 'Lower back': ['lowb']
};
function muscleSets(s) {
  const m = {};
  Object.entries(s.ex || {}).forEach(([id, a]) => {
    const n = (a || []).filter(full).length;
    if (!n) return;
    const k = exMuscle(id);
    m[k] = (m[k] || 0) + n
  });
  return m
}
function mLevels(m) {
  const L = {};
  Object.entries(m).forEach(([k, n]) => (MKEY[k] || []).forEach(x => {L[x] = Math.max(L[x] || 0, n >= 4 ? 2 : 1)}));
  return L
}
function mapHTML(L) {
  const dk = isDark();
  return `<div class="bodies"><svg viewBox="30 0 140 380" role="img" aria-label="Front muscles">${anatomy('front',L,dk)}</svg><svg viewBox="30 0 140 380" role="img" aria-label="Back muscles">${anatomy('back',L,dk)}</svg></div><div class="bodies"><div class="cap">Front</div><div class="cap">Back</div></div><div class="leg"><span><i style="background:#ff8a3d"></i>1–3 sets</span><span><i style="background:#f0303a"></i>4+ sets</span><span><i style="background:${dk?'#30343d':'#d9dce2'}"></i>Rested</span></div>`
}
const OBJS = [[250, 'a fridge', '🧊'], [1500, 'a grand piano', '🎹'], [4000, 'a pickup truck', '🛻'], [9000, 'an elephant', '🐘'], [12000, 'a school bus', '🚌'], [18000, 'a T-rex', '🦖'], [33000, 'a loaded semi truck', '🚛'], [60000, 'a fighter jet', '✈️'], [150000, 'a blue whale', '🐋']];
function volCompare(v) {
  const lb = U() === 'kg' ? v * 2.2046 : v;
  let o = OBJS[0];
  for (const x of OBJS) if (lb >= x[0]) o = x;
  const n = lb / o[0];
  return [o[2], `You moved <b>${Math.round(v).toLocaleString()} ${U()}</b>, ${n>=1.8?`about ${Math.round(n)}× ${o[1].replace(/^an? /,'')}${Math.round(n)>1?'s':''}`:`about the weight of ${o[1]}`}`]
}
let RSC = null;
function realSessions() {
  if (RSC) return RSC;
  return RSC = realSessions0()
}
function realSessions0() {
  return Object.values(st.sessions).filter(s => real(s) && Object.values(s.ex || {}).some(a => Array.isArray(a) && a.some(full))).sort((a, b) => a.date < b.date ? -1 : a.date > b.date ? 1 : 0)
}
function sVol(s) {
  let v = 0;
  Object.values(s.ex || {}).forEach(a => (a || []).forEach(x => {if (full(x)) v += x.w * x.r}));
  return v
}
function sSets(s) {
  let n = 0;
  Object.values(s.ex || {}).forEach(a => (a || []).forEach(x => {if (full(x)) n++}));
  return n
}
function funStats(s) {
  const out = [], ss = sessStats(s), rs = realSessions(), p = st.plan.days[DAYS.indexOf(s.day)] || {};
  if (ss.vol) out.push(volCompare(ss.vol));
  if (ss.prs.length) {
    const id = ss.prs[0], b = (s.ex[id] || []).filter(full).reduce((m, x) => e1(x) > e1(m) ? x : m, (s.ex[id] || []).filter(full)[0]);
    out.push(['🏆', `<b>Heaviest set ever</b> on ${esc(exName(id))}: ${b.w} × ${b.r}`])
  }
  const prev = rs.filter(x => x.day === s.day && x.date < s.date).pop();
  if (prev) {
    const d = ss.vol - sVol(prev);
    if (d > 0) out.push(['📈', `You beat last ${DN[DAYS.indexOf(s.day)]} by <b>+${Math.round(d).toLocaleString()} ${U()}</b>`]);
    const ds = sSets(s) - sSets(prev);
    if (d <= 0 && ds > 0) out.push(['➕', `<b>${ds} more set${ds>1?'s':''}</b> than last ${DN[DAYS.indexOf(s.day)]}`])
  }
  if (s.start && s.end) {
    const mine = elapsed(s), others = rs.filter(x => x.day === s.day && x !== s && x.start && x.end).map(elapsed);
    if (others.length && mine < Math.min(...others)) out.push(['⚡', `<b>Fastest</b> ${esc(p.name||'workout')} yet, ${Math.max(1,Math.round((Math.min(...others)-mine)/6e4))} min quicker`])
  }
  const n = rs.filter(x => x.date <= s.date).length, next = [10, 25, 50, 100, 250, 500].find(m => m > n);
  out.push(['🗓️', `Workout <b>#${n}</b> logged${next&&next-n<=3?` · ${next-n} more to hit ${next}`:''}`]);
  const life = rs.reduce((a, x) => a + sVol(x), 0);
  if (life > ss.vol) out.push(['🌍', `Lifetime total: <b>${life>=1e6?(life/1e6).toFixed(2)+'M':kfmt(life)} ${U()}</b>`]);
  return out.slice(0, 4)
}
function analysisHTML(s) {
  const m = muscleSets(s), keys = Object.keys(m).sort((a, b) => m[b] - m[a]), mx = Math.max(1, ...keys.map(k => m[k]));
  const fs = funStats(s), today = ENG ? ENG.earnedOn(s.date) : [];
  return `${crewOn()&&!s.posted&&st.date===s.date?'<button class="btn acc wide" id="postcrew" style="min-height:52px;margin-bottom:12px">📣 Post to crew</button>':''}<div class="card"><div class="glbl">MUSCLES HIT</div>${mapHTML(mLevels(m))}<div style="margin-top:10px">${keys.map(k=>`<div class="mrow"><span>${k}</span><span class="t"><i style="width:${Math.round(m[k]/mx*100)}%;background:linear-gradient(90deg,#ff8a3d,#ef2b36)"></i></span><b>${m[k]}</b></div>`).join('')||'<p class="sub">Log some sets to light up the map.</p>'}</div></div>${fs.length?`<div class="card"><div class="glbl">FUN STATS</div><div class="facts">${fs.map(([e,t])=>`<div class="fact"><span class="fe">${e}</span><p>${t}</p></div>`).join('')}</div></div>`:''}${today.length?`<div class="card"><div class="glbl">BADGES EARNED TODAY</div><div class="bcase" style="margin-top:10px">${today.map(b=>badgeTile(b,true)).join('')}</div></div>`:''}`
}
async function mapImage(L) {
  const f = anatomy('front', L, false), b = anatomy('back', L, false);
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="560" height="760" viewBox="0 0 280 380"><g transform="translate(-30 0)">${f}</g><g transform="translate(110 0)">${b.replace(/id="a(m0|m1|m2|sk|gw)"/g,'id="b$1"').replace(/url\(#a(m0|m1|m2|sk|gw)\)/g,'url(#b$1)')}</g></svg>`;
  return new Promise(r => {
    const im = new Image();
    im.onload = () => r(im);
    im.onerror = () => r(null);
    im.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg)
  })
}
/* ---------- 0.13: muscle mastery ---------- */
const MASTER = [25, 75, 150, 300, 600, 1000];
function masteryData() {
  const tot = {};
  realSessions().forEach(s => Object.entries(muscleSets(s)).forEach(([k, n]) => tot[k] = (tot[k] || 0) + n));
  return Object.entries(tot).map(([k, n]) => {
    const t = MASTER.filter(x => n >= x).length;
    return {k, n, t, next: MASTER[t]}
  }).sort((a, b) => b.n - a.n)
}
function masteryHTML() {
  const D = masteryData(), C = {};
  D.forEach(x => {if (x.t)(MKEY[x.k] || []).forEach(m => C[m] = TIERS[x.t - 1][1])});
  const dk = isDark();
  return `<div class="card"><h3>Muscle mastery</h3><p class="sub">Every set levels up the muscle it hits, from Bronze to Legend.</p><div class="bodies"><svg viewBox="30 0 140 380" aria-label="Front mastery">${anatomy('front',{},dk,C)}</svg><svg viewBox="30 0 140 380" aria-label="Back mastery">${anatomy('back',{},dk,C)}</svg></div>
 <div style="margin-top:8px">${D.map(x=>`<div class="mrow"><span>${esc(x.k)}</span><span class="t"><i style="width:${x.next?Math.round(x.n/x.next*100):100}%;background:${x.t?TIERS[x.t-1][1]:'#8c939e'}"></i></span><b style="color:${x.t?TIERS[x.t-1][1]:'var(--sub)'};min-width:70px;text-align:right">${x.t?TIERS[x.t-1][0]:'—'}</b></div>`).join('')||'<p class="sub">Log a workout to start.</p>'}</div></div>`
}
/* ---------- 0.13: lift ranks ---------- */
const LIFTS = [['incline', 'Incline press', /incline/i, [.4, .6, .85, 1.05, 1.25, 1.45]], ['bench', 'Bench press', /bench|chest press/i, [.5, .75, 1, 1.25, 1.5, 1.75]], ['hack', 'Hack squat', /hack/i, [.75, 1.1, 1.5, 1.85, 2.2, 2.6]], ['legpress', 'Leg press', /leg press/i, [1.25, 1.75, 2.25, 2.75, 3.25, 3.75]], ['squat', 'Squat', /squat/i, [.75, 1, 1.25, 1.5, 1.75, 2]], ['rdl', 'RDL', /rdl|romanian/i, [.6, .85, 1.1, 1.35, 1.6, 1.85]], ['dead', 'Deadlift', /deadlift/i, [1, 1.25, 1.5, 1.85, 2.2, 2.5]], ['ohp', 'Overhead press', /overhead|shoulder press|ohp|military/i, [.35, .5, .65, .8, .95, 1.1]], ['pull', 'Pulldown', /pulldown|pull-down|pull down|lat pull/i, [.5, .65, .8, .95, 1.1, 1.25]], ['row', 'Row', /row/i, [.5, .7, .9, 1.1, 1.3, 1.5]], ['curl', 'Curl', /curl/i, [.2, .3, .4, .5, .6, .7]]];
function liftRanks() {
  const best = {};
  realSessions().forEach(s => Object.entries(s.ex).forEach(([id, a]) => {
    const n = exName(id);
    if (/leg curl|hamstring curl/i.test(n)) return;
    const L = LIFTS.find(l => l[2].test(n));
    if (!L) return;
    (a || []).forEach(x => {
      if (full(x)) {
        const v = e1(x);
        if (!best[L[0]] || v > best[L[0]].e) best[L[0]] = {e: v, w: +x.w, r: +x.r, n}
      }
    })
  }));
  const bw = +st.set.bw || 0;
  return LIFTS.filter(l => best[l[0]]).map(l => {
    const b = best[l[0]], ratio = bw ? b.e / bw : 0, t = bw ? l[3].filter(x => ratio >= x).length : 0, nx = l[3][t];
    return {k: l[0], name: l[1], ...b, ratio, t, nextW: nx && bw ? Math.ceil(nx * bw / (1 + 1 / 30) / 5) * 5 : 0}
  })
}
function liftRanksHTML() {
  const R = liftRanks(), bw = +st.set.bw;
  if (!R.length) return '';
  return `<div class="card"><h3>Lift ranks</h3><p class="sub">Each lift is ranked by your estimated 1-rep max compared with your bodyweight. These are general standards, so treat them as a guide.</p>${bw?'':`<div class="row2" style="margin:8px 0"><input id="bwin2" class="field" inputmode="decimal" placeholder="Bodyweight (${U()})"><button class="btn acc" id="bwsave">Save</button></div>`}
 <div class="lrgrid">${R.map(x=>{const t=x.t?TIERS[x.t-1]:null;return `<div class="lr" style="--tc:${t?t[1]:'#8c939e'}"><span class="lring">${x.t?ROMAN[x.t]:'–'}</span><b>${esc(x.name)}</b><span class="sub">${t?t[0]:bw?'Unranked':'Add bodyweight'}${bw?` · ${x.ratio.toFixed(2)}×`:''}</span><span class="sub lnext">${x.nextW?`Next: ${x.nextW}×1`:x.t>=6?'Legend':''}</span></div>`}).join('')}</div></div>`
}
/* ---------- 0.13: goals ---------- */
function goalInfo(g) {
  const rs = realSessions(), T = g.w * (1 + 1 / 30);
  let cur = 0, done = null;
  const wkBest = {};
  rs.forEach(s => (s.ex[g.ex] || []).forEach(x => {
    if (!full(x)) return;
    const v = e1(x);
    cur = Math.max(cur, v);
    const w = weekDates(s.date)[0];
    wkBest[w] = Math.max(wkBest[w] || 0, v);
    if (!done && s.date >= g.created && +x.w >= g.w) done = s.date
  }));
  const pts = Object.entries(wkBest).sort().slice(-8).map(([w, v]) => [(toD(w) - toD(today())) / (7 * 864e5), v]);
  let slope = 0;
  if (pts.length >= 3) {
    const n = pts.length, mx = pts.reduce((a, p) => a + p[0], 0) / n, my = pts.reduce((a, p) => a + p[1], 0) / n;
    const num = pts.reduce((a, p) => a + (p[0] - mx) * (p[1] - my), 0), den = pts.reduce((a, p) => a + (p[0] - mx) ** 2, 0);
    slope = den ? num / den : 0
  }
  let proj = null;
  if (!done && cur < T && slope > 0) {proj = iso(new Date(Date.now() + Math.ceil((T - cur) / slope) * 7 * 864e5))} else if (cur >= T && !done) proj = today();
  return {
    cur, T, done, proj, pct: Math.min(100, Math.round(cur / T * 100)), status: done ? 'done' : proj ? (proj <= g.date ? 'on' : 'behind') : 'flat'
  }
}
function goalsHTML() {
  if (!feat('goals')) return '';
  const G = st.set.goals || [];
  const fmt = d => `${MON[toD(d).getMonth()]} ${toD(d).getDate()}`;
  return `<div class="card"><div class="ghd"><h3>Goals</h3><button class="btn sm" id="goaladd">+ Goal</button></div>${G.length?G.map((g,i)=>{const I=goalInfo(g);return `<div class="goal ${I.status}"><div class="qh"><span><b>${esc(exName(g.ex))} ${g.w} ${U()}</b> <span class="sub">by ${fmt(g.date)}</span></span><button class="gx" data-goaldel="${i}" aria-label="Remove goal">✕</button></div><i><u style="width:${I.done?100:I.pct}%"></u></i><div class="sub">${I.done?`🎉 Crushed on ${fmt(I.done)}`:I.status==='on'?`On pace · projected ${fmt(I.proj)}`:I.status==='behind'?`Behind pace · projected ${fmt(I.proj)}`:'Not enough recent data to project yet. Keep logging'}</div></div>`}).join(''):'<p class="sub">Pick a lift, a target and a date. Overload projects when you’ll hit it.</p>'}</div>`
}
function goalCheck() {
  const G = st.set.goals || [];
  let ch = false;
  G.forEach(g => {
    if (g.doneAt) return;
    const I = goalInfo(g);
    if (I.done) {
      g.doneAt = I.done;
      ch = true;
      setTimeout(() => {
        toast(`🎯 Goal crushed: ${exName(g.ex)} ${g.w}`);
        confetti(110);
        haptic('success');
        addBell('🎯', `Goal crushed: ${esc(exName(g.ex))} ${g.w} ${U()}`)
      }, 600)
    }
  });
  if (ch) saveSet()
}
function goalSheet() {
  const rs = realSessions(), cnt = {};
  rs.forEach(s => Object.keys(s.ex).forEach(id => {if ((s.ex[id] || []).some(full)) cnt[id] = (cnt[id] || 0) + 1}));
  const ids = Object.keys(cnt).sort((a, b) => cnt[b] - cnt[a]);
  const d = new Date();
  d.setMonth(d.getMonth() + 3);
  return `<div class="ghd"><h2>New goal</h2><button class="btn sm" id="sheetx">Cancel</button></div><label class="glbl" for="gex">LIFT</label><select id="gex" class="field">${ids.map(id=>`<option value="${esc(id)}">${esc(exName(id))}</option>`).join('')}</select><label class="glbl" for="gw" style="margin-top:10px;display:block">TARGET (${U()} for 1 rep)</label><input id="gw" class="field" inputmode="decimal" placeholder="225"><label class="glbl" for="gd" style="margin-top:10px;display:block">BY</label><input id="gd" class="field" type="date" value="${iso(d)}"><button class="btn acc wide" id="goalsave" style="margin-top:14px;min-height:52px">Set goal</button>`
}
