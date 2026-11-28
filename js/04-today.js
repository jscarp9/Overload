/* ========================================================================
   Overload: Today: logging a workout

   Today screen, exercise picker, rest timer, faster logging, the fun one-liners.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

/* ---------- today ---------- */
function counts() {
  const s = cur();
  let tot = 0, dn = 0;
  todayGroups().forEach(gr => gr.forEach(e => {
    tot += Math.max(+e.sets || 2, (s.ex[e.id] || []).length);
    dn += (s.ex[e.id] || []).filter(x => x && x.d).length
  }));
  return[dn, tot]
}
const VERSION = BETA ? '0.27 beta' : '0.27';
const NEWS = [['A real profile','Your Profile now shows your progress: rank and what it unlocks next, then vs now, your Big 3 total, strength levels, PRs, this month vs last, recent achievements and the badges you’re closest to.'],['Profile settings','Customizing moved off the Profile. Tap the pencil for plates, themes, pinned medals, bodyweight and which sections show.'],['Coach on your profile','If Coach is on, you get a short note about your last 30 days once a month.'],['Your crew sees it too','Tap a crewmate to see their showcase. Use Preview as crew in Profile settings to see yours.']];
const isDark = () => document.documentElement.dataset.mode === 'dark';
function applyTheme() {
  const t = st.set.theme || 'auto', mq = window.matchMedia && matchMedia('(prefers-color-scheme: dark)');
  const m = t === 'auto' ? (mq && mq.matches ? 'dark' : 'light') : t;
  document.documentElement.dataset.mode = m;
  const mt = document.querySelector('meta[name=theme-color]');
  if (mt) mt.setAttribute('content', m === 'dark' ? '#07080c' : '#f7f5f1')
}
function todayGroups() {
  const p = pd(), sw = (cur().swap) || {};
  return p.groups.map(gr => gr.map(e => sw[e.id] || e))
}
function exById(id) {
  const m = exMap();
  if (m[id]) return m[id];
  const sw = cur().swap || {};
  return Object.values(sw).find(x => x.id === id)
}
const inc = w => U() === 'kg' ? (w >= 40 ? 2.5 : 1) : (w >= 60 ? 5 : 2.5);
function suggest(e) {
  const l = last(e.id);
  if (!l) return null;
  const a = (l.ex[e.id] || []).filter(full);
  if (!a.length) return null;
  const top = a.reduce((m, x) => (+x.w > +m.w || (+x.w === +m.w && +x.r > +m.r)) ? x : m, a[0]), atTop = a.filter(x => +x.w === +top.w);
  if (e.hi && a.length >= Math.min(+e.sets || 2, a.length) && atTop.every(x => +x.r >= e.hi)) return {w: +top.w + inc(+top.w), r: e.lo || +top.r};
  const minR = Math.min(...atTop.map(x => +x.r));
  return {w: +top.w, r: e.hi ? Math.min(e.hi, minR + 1) : minR + 1}
}
function stalled(id) {
  const pts = points(id).filter(p => p.s.date < st.date && real(p.s));
  if (pts.length < 4) return false;
  const base = pts[pts.length - 4].e;
  return pts.slice(-3).every(p => p.e <= base)
}
const r5 = w => U() === 'kg' ? Math.round(w / 2.5) * 2.5 : Math.round(w / 5) * 5;
function warmHTML(e) {
  const s = cur(), sg = suggest(e), x = (s.ex[e.id] || []).find(full), t = x ? +x.w : sg ? sg.w : 0;
  if (!t) return `<div class="warm"><div class="ghd"><span class="glbl">WARM-UPS</span><button class="btn sm" data-warm="${esc(e.id)}">Hide</button></div><div class="sub">Log your first set or do this lift once, then warm-ups are worked out for you.</div></div>`;
  const rows = [[.4, 10], [.6, 6], [.75, 3], [.85, 1]].map(([p, r]) => [Math.max(r5(t * p), U() === 'kg' ? 20 : 45), r]).filter((v, i, a) => v[0] < t && (!i || v[0] !== a[i - 1][0]));
  return `<div class="warm"><div class="ghd"><span class="glbl">WARM-UPS FOR ${t} ${U().toUpperCase()}</span><button class="btn sm" data-warm="${esc(e.id)}">Hide</button></div>${rows.map(([w,r])=>`<div class="wr"><span>${w} ${U()}</span><b>× ${r}</b></div>`).join('')}<div class="sub" style="margin-top:4px">Easy and fast. Warm-ups don't count as working sets.</div></div>`
}
function tagFor(e) {
  const s = cur(), sets = (s.ex[e.id] || []).filter(full), l = last(e.id);
  const pb = bestBefore(e.id, st.date);
  if (pb && sets.some(x => e1(x) > pb)) return '<span class="tag pr">New PR</span>';
  if (e.hi && sets.length >= (+e.sets || 2) && sets.every(x => +x.r >= e.hi)) return '<span class="tag up">Top of range. Add weight next time</span>';
  if (l && sets.some((x, i) => better(x, (l.ex[e.id] || [])[i]))) return '<span class="tag pr">Beat last time</span>';
  if (!sets.length && stalled(e.id)) return '<span class="tag st">Stalled 3 sessions · try a 10% deload or swap it</span>';
  return ''
}
function lastNote(id) {
  for (const [k, s] of sorted()) {
    if (k === sid() || s.date > st.date) continue;
    if (s.exNotes && s.exNotes[id] && s.exNotes[id].trim()) return s.exNotes[id].trim()
  }
  return ''
}
const CK = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>';
function rackHTML() {
  const wd = weekDates(), t = today();
  const first = toD(wd[0]), lastd = toD(wd[6]);
  const label = first.getMonth() === lastd.getMonth() ? `${MON[first.getMonth()]} ${first.getDate()} – ${lastd.getDate()}` : `${MON[first.getMonth()]} ${first.getDate()} – ${MON[lastd.getMonth()]} ${lastd.getDate()}`;
  const inWeek = wd.includes(t);
  return `<section class="card rack"><div class="rackh"><b>${label}</b><div class="nav"><button data-wk="-1" aria-label="Previous week">‹</button>${inWeek&&st.date===t?'':'<button class="today" id="gotoday">Today</button>'}<button data-wk="1" aria-label="Next week">›</button></div></div><div class="plates">${wd.map((ds,i)=>{const d=st.plan.days[i],k=DAYS[i],done=doneOn(ds,k),state=d.type==='off'?'rest':done?'done':'idle';
  return `<button class="pday${ds===st.date?' sel':''}${ds===t?' now':''}" data-date="${ds}" aria-label="${DN[i]} ${md(ds)} ${esc(d.name)}${done?', logged':''}">${plateSVG(colorOf(d),state)}<span class="d">${ds===t?'Today':DN[i]}</span><span class="dn">${toD(ds).getDate()}</span></button>`}).join('')}</div></section>`
}
function heroHTML() {
  const p = pd(), s = cur(), [dn, tot] = counts(), ss = sessStats(s), col = colorOf(p), t = today();
  const when = st.date === t ? 'Today' : st.date < t ? 'Past' : 'Upcoming';
  let act = '';
  if (p.type !== 'off') {
    if (!s.start) act = `<button class="btn white" id="startw">▶ Start workout</button><button class="excl" id="excusebtn">Need an excuse?</button>`;
    else if (!s.end) {
      act = `<button class="btn white" id="pausew">${s.pauseAt?'▶ Resume':'❚❚ Pause'}</button><button class="btn glass" id="finish">Finish</button>`
    }
    if (s.start) act += `<button class="btn glass" id="resetw">${st.confirm==='rw'?'Tap to reset':'↺ Reset'}</button>`
  }
  return `<section class="hero" style="--grad:${PC[col][3]};--accent:${PC[col][0]}"><div class="hgrid"><div><div class="kick">${when} · ${DN[DAYS.indexOf(st.day)]} ${MON[toD(st.date).getMonth()]} ${toD(st.date).getDate()}</div>${st.renaming?`<input class="field rn" id="rename" value="${esc(p.name)}" aria-label="Day name" maxlength="28">`:`<h1 class="hname">${esc(p.name)}<button class="ren" id="renbtn" aria-label="Rename this day">✎</button></h1>`}<div class="kick">${esc(p.focus||'')}</div></div><div class="bigplate">${plateSVG(col,p.type==='off'?'rest':'idle',{big:true,label:p.name.toUpperCase(),ring:p.type==='off'?undefined:(tot?dn/tot:0)})}</div></div>
 ${p.type==='off'?'':`<div class="hstats"><div class="hs"><div class="num" id="elapsed">${mins(elapsed(s))}</div><span>${s.pauseAt?'Paused':'Time'}</span></div><div class="hs"><div class="num" id="hsets">${dn}/${tot}</div><span>Sets</span></div><div class="hs"><div class="num" id="hvol">${kfmt(ss.vol)}</div><span>${U()} moved</span></div></div><div class="hact">${act}</div>`}</section>`
}
function summaryHTML() {
  const s = cur();
  if (!s.end) return '';
  const ss = sessStats(s);
  return `<div class="card sum"><div class="ghd"><h3>Workout complete</h3><span style="display:flex;gap:6px"><button class="btn sm white" id="sharecard">📸 Share</button><button class="btn sm glass" id="reopen">Keep logging</button></span></div><div class="tiles four"><div class="tile"><div class="num">${mins(elapsed(s))}</div><div class="sub">Time</div></div><div class="tile"><div class="num">${ss.sets}</div><div class="sub">Sets</div></div><div class="tile"><div class="num">${kfmt(ss.vol)}</div><div class="sub">${U()} moved</div></div><div class="tile"><div class="num">${ss.prs.length}</div><div class="sub">PRs</div></div></div>${ss.prs.length?`<div class="sub">New PRs: <b style="color:#fff">${ss.prs.map(exName).map(esc).join(', ')}</b></div>`:''}</div>`
}
function exBlock(e, gi, ei, glen, col) {
  const s = cur(), sets = s.ex[e.id] || [], l = last(e.id), n = Math.max(+e.sets || 2, sets.length), letter = String.fromCharCode(65 + gi) + (glen > 1 ? (ei + 1) : ''), be = Math.round(bestBefore(e.id, st.date)), sg = suggest(e), rir = !!st.set.rir, key = gi + '-' + ei, orig = pd().groups[gi] && pd().groups[gi][ei], isSw = orig && orig.id !== e.id;
  let rows = '';
  for (let i = 0; i < n; i++) {
    const x = sets[i] || {}, la = l ? (l.ex[e.id] || []) : [], lx = la[i] || la[la.length - 1] || null, pw = sg ? sg.w : (lx && lx.w ? lx.w : ''), pr = sg ? sg.r : (lx && lx.r ? lx.r : '');
    rows += `<div class="srow${x.d?' done':''}" id="row-${e.id}-${i}"><span class="n">${i+1}<em id="m-${e.id}-${i}">${better(x,la[i])?'↑':''}</em></span><input type="number" inputmode="decimal" step="any" placeholder="${pw!==''?esc(pw):'0'}" value="${x.w??''}" data-ex="${esc(e.id)}" data-i="${i}" data-k="w" aria-label="${esc(e.name)} set ${i+1} weight"><input type="number" inputmode="numeric" placeholder="${pr!==''?esc(pr):'0'}" value="${x.r??''}" data-ex="${esc(e.id)}" data-i="${i}" data-k="r" aria-label="${esc(e.name)} set ${i+1} reps">${rir?`<input type="number" inputmode="numeric" placeholder="RIR" value="${x.q??''}" data-ex="${esc(e.id)}" data-i="${i}" data-k="q" aria-label="${esc(e.name)} set ${i+1} reps in reserve">`:''}<button class="ck" data-ck="${esc(e.id)}" data-i="${i}" data-g="${gi}" data-e="${ei}" aria-label="Complete set ${i+1}">${CK}</button></div>`
  }
  const open = st.menu === key, cf = st.confirm === 'x' + key;
  const menu = `<div class="menuwrap"><button class="more" data-more="${key}" aria-label="More options for ${esc(e.name)}" aria-expanded="${open}">⋯</button>${open?`<div class="menu" role="menu"><button data-act="plates" data-key="${key}">🏋️ Plate calculator</button><button data-act="fill" data-key="${key}">↺ Same as last time</button><button data-act="warm" data-key="${key}">🔥 Warm-ups</button><button data-act="nhist" data-key="${key}">📝 Notes history</button>${isSw?`<button data-act="unswap" data-key="${key}">↩︎ Undo swap (back to ${esc(orig.name)})</button>`:`<button data-act="swap" data-key="${key}">🔄 Swap for today</button>`}<button class="dz" data-act="del" data-key="${key}">${cf?'Tap again to remove':'🗑 Remove from plan'}</button></div>`:''}</div>`;
  const lastLine = l ? `Last ${md(l.date)}: <b>${fmt(l.ex[e.id])}</b>${sg?` · <span class="try">Try ${sg.w}×${sg.r}</span>`:''}` : 'First time. Set your baseline';
  const swapPick = st.picker && st.picker.ctx === 'swap' && st.picker.key === key ? pickerHTML() : '';
  return `<div class="ex${rir?' rir':''}"><div class="exh"><span class="badge" style="background:${PC[col][0]}">${letter}</span><h3>${esc(e.name)}${isSw?'<span class="swapped">SWAPPED</span>':''}</h3><span class="tgt">${e.sets||2} × ${e.lo||'?'}–${e.hi||'?'}</span>${menu}</div><div class="meta">${esc(e.m)} · ${lastLine}${be?` · Best ${be}`:''}</div>${swapPick}${st.warm[e.id]?warmHTML(e):''}${e.cue?`<div class="cue">${esc(e.cue)}</div>`:''}${lastNote(e.id)?`<div class="lnote">Last note: ${esc(lastNote(e.id))}</div>`:''}<div id="tag-${e.id}">${tagFor(e)}</div><div class="shead"><span>Set</span><span>${U()}</span><span>Reps</span>${rir?'<span>RIR</span>':''}<span></span></div>${rows}<textarea class="exnote" rows="1" data-exnote="${esc(e.id)}" placeholder="Note for today (seat, grip, how it felt)" aria-label="Note for ${esc(e.name)}">${esc((s.exNotes||{})[e.id]||'')}</textarea><div class="exfoot"><button class="btn sm" data-addset="${esc(e.id)}">+ Set</button>${sets.length>(+e.sets||2)?`<button class="btn sm" data-rmset="${esc(e.id)}">− Set</button>`:''}</div></div>`
}
function bkDue() {
  const n = Object.values(st.sessions).filter(x => real(x) && Object.values(x.ex || {}).some(has)).length;
  return n >= 3 && !st.hideBk && (!st.set.lastBackup || Date.now() - st.set.lastBackup > 7 * 864e5)
}
function safariBanner() {
  if (isStandalone() || !isMobile() || st.hideSafari) return '';
  const k = browserKind(), and = /Android/i.test(navigator.userAgent);
  const msg = k === 'firefox' || k === 'samsung' || k === 'other' ? (and ? `⚠️ ${k==='firefox'?'Firefox':k==='samsung'?'Samsung Internet':'This browser'} can clear your workouts. To keep them safe, open this link in <b>Chrome</b> → ⋮ → <b>Add to home screen</b> → Install, then use that icon.` : `⚠️ This browser can clear your workouts. Open this link in <b>Safari</b> → Share → <b>Add to Home Screen</b>, then use that icon.`) : k === 'iosother' ? `⚠️ On iPhone, install Overload from <b>Safari</b>: open this link in Safari → Share → <b>Add to Home Screen</b>.` : `⚠️ You're in the browser. Workouts logged here are saved separately from your home screen app. Open Overload from your home screen icon to log.`;
  return `<div class="sbanner"><span>${msg}</span><button class="btn sm" id="hidesafari">Got it</button></div>`
}
const isStandalone = () => !!(navigator.standalone || (window.matchMedia && matchMedia('(display-mode: standalone)').matches));
const isMobile = () => /iPhone|iPad|iPod|Android/i.test(navigator.userAgent);
function renderLog() {
  const p = pd(), s = cur(), col = colorOf(p);
  let h = safariBanner() + (false && bkDue() && !st.set.acct ? `<div class="card bkb"><div><b>Back up your log</b><div class="sub">${st.set.lastBackup?'Last backup '+Math.floor((Date.now()-st.set.lastBackup)/864e5)+' days ago':'You haven\u2019t backed up yet'}. Takes 2 seconds.</div></div><div class="exfoot"><button class="btn sm go" id="bkquick">💾 Save backup</button><button class="btn sm" id="bklater">Later</button></div></div>` : '') + smartCard() + (new Date().getMonth() === 11 && st.set.yirSeen !== new Date().getFullYear() && realSessions().length ? `<button class="card yir" id="openyir"><span>🎬</span><div><b>Your ${new Date().getFullYear()} Year in review is here</b><div class="sub">Tap to watch it</div></div></button>` : '') + rackHTML() + heroHTML() + groupBar() + summaryHTML() + (cur().end ? analysisHTML(cur()) : '') + challengesHTML();
  if (p.type === 'off') return h + `<div class="card empty"><h3>Rest day</h3><p>Walk, eat, sleep. Growth happens today. Want to train anyway? Tap another day above or add an exercise.</p><button class="btn" id="todayadd">+ Add exercise</button></div>` + (st.picker && st.picker.ctx === 'today' ? pickerHTML() : '');
  todayGroups().forEach((gr, gi) => {
    h += `<div class="card"><div class="ghd"><span class="glbl">${gr.length>1?'SUPERSET '+String.fromCharCode(65+gi):'EXERCISE '+String.fromCharCode(65+gi)}</span><button class="btn sm" data-rest="${p.rest||90}">⏱ Rest ${Math.floor((p.rest||90)/60)}:${String((p.rest||90)%60).padStart(2,'0')}</button></div>${gr.map((e,ei)=>exBlock(e,gi,ei,gr.length,col)).join('')}</div>`
  });
  if (!p.groups.length) h += `<div class="card empty"><h3>No exercises yet</h3><p>Add them now. They save to your plan for every ${DN[DAYS.indexOf(st.day)]}.</p></div>`;
  h += st.picker && st.picker.ctx === 'today' ? pickerHTML() : `<button class="btn wide go" id="todayadd" style="margin-bottom:12px;min-height:52px">+ Add exercise</button>`;
  h += `<div class="card"><label class="glbl" for="snote">SESSION NOTES</label><textarea id="snote" style="margin-top:8px" placeholder="Energy, sleep, what felt strong, what hurt…">${esc(s.notes||'')}</textarea></div>`;
  return h
}
/* ---------- picker ---------- */
function pickerHTML() {
  return `<div class="pick" id="pick"><div class="ghd"><span class="glbl">${st.picker.ctx==='swap'?'SWAP FOR TODAY':st.picker.gi>=0?'ADD TO SUPERSET '+String.fromCharCode(65+st.picker.gi):'ADD EXERCISE'}</span><button class="btn sm white" id="pclose">Close</button></div><input class="field" id="psearch" style="background:#fff;margin-top:6px" placeholder="Search 60+ exercises or type your own" autocomplete="off" aria-label="Search exercises"><div class="plist" id="plist">${plistHTML('')}</div></div>`
}
function plistHTML(q) {
  const ql = q.trim().toLowerCase();
  const r = LIB.filter(x => !ql || x[0].toLowerCase().includes(ql) || x[1].toLowerCase().includes(ql)).slice(0, 40);
  let h = '';
  if (ql && !LIB.some(x => x[0].toLowerCase() === ql)) h += `<button data-pick="__custom">Add “${esc(q.trim())}”<span>Custom</span></button>`;
  return h + r.map(x => `<button data-pick="${LIB.indexOf(x)}">${esc(x[0])}<span>${x[1]} · ${x[2]}–${x[3]}</span></button>`).join('')
}
function uniqueId(name) {
  const base = slug(name), m = exMap();
  let id = base, n = 2;
  while (m[id] && m[id].name.toLowerCase() !== name.toLowerCase()) id = base + '-' + (n++);
  return id
}
function addFromPicker(name, m, lo, hi) {
  if (st.picker && st.picker.ctx === 'swap') {
    const [gi, ei] = st.picker.key.split('-').map(Number), orig = pd().groups[gi][ei], s = ensure();
    s.swap = s.swap || {};
    const ex = E(name, m, lo, hi, +orig.sets || 2, slug(name));
    s.swap[orig.id] = ex;
    s.names[ex.id] = name;
    saveS();
    st.picker = null;
    st.menu = null;
    toast('Swapped to ' + name + ' for today');
    render();
    return
  }
  const dayI = st.picker.ctx === 'today' ? DAYS.indexOf(st.day) : st.pday, d = st.plan.days[dayI];
  const ex = E(name, m, lo, hi, 2, uniqueId(name));
  if (st.picker.gi >= 0 && d.groups[st.picker.gi]) d.groups[st.picker.gi].push(ex);
  else d.groups.push([ex]);
  if (d.type === 'off') d.type = 'other';
  saveP();
  toast(name + ' added');
  st.picker = null;
  render()
}
/* ---------- audio + rest ---------- */
let actx = null;
function unlockAudio() {
  try {
    actx = actx || new (window.AudioContext || window.webkitAudioContext)();
    if (actx.state === 'suspended') actx.resume()
  } catch (e) {}
}
function beep() {
  if (!st.set.sound) return;
  if (playPack()) return;
  try {
    unlockAudio();
    [0, .22, .44].forEach((t, i) => {
      const o = actx.createOscillator(), gn = actx.createGain();
      o.type = 'sine';
      o.frequency.value = i === 2 ? 1320 : 880;
      gn.gain.setValueAtTime(.0001, actx.currentTime + t);
      gn.gain.exponentialRampToValueAtTime(.35, actx.currentTime + t + .02);
      gn.gain.exponentialRampToValueAtTime(.0001, actx.currentTime + t + .18);
      o.connect(gn).connect(actx.destination);
      o.start(actx.currentTime + t);
      o.stop(actx.currentTime + t + .2)
    })
  } catch (e) {}
}
function startRest(sec, next) {
  if (typeof GRP !== 'undefined' && GRP) setTimeout(sendLive, 120);
  st.rest = {
    end: Date.now() + sec * 1000, dur: sec, base: sec, buzzed: false, next: next || '', joke: REST_LINES[Math.floor(Math.random() * REST_LINES.length)]
  };
  $('rest').classList.add('on');
  $('rest').classList.remove('done');
  tick()
}
function tick() {
  if (st.rest) {
    const left = st.rest.end - Date.now(), lbl = $('rlbl');
    if (left <= 0) {
      $('rnum').textContent = 'Go';
      $('ringp').setAttribute('stroke-dashoffset', '0');
      $('rest').classList.add('done');
      lbl.innerHTML = `<b>Time's up</b>${esc(st.rest.next||'Next set')}`;
      if (!st.rest.buzzed) {
        st.rest.buzzed = true;
        beep();
        if (st.set.vibe) {try {navigator.vibrate && navigator.vibrate([250, 120, 250])} catch (e) {}}
      }
    } else {
      const s = Math.ceil(left / 1000);
      $('rnum').textContent = `${Math.floor(s/60)}:${String(s%60).padStart(2,'0')}`;
      $('ringp').setAttribute('stroke-dashoffset', String(138.2 * (1 - left / (st.rest.dur * 1000))));
      lbl.innerHTML = `<b>Rest</b>${st.rest.next?'Next: '+esc(st.rest.next):'Breathe'}${st.set.restJokes!==false&&st.rest.joke?'<span class="rjoke">'+esc(st.rest.joke)+'</span>':''}`
    }
  }
  const el = $('elapsed');
  if (el) {
    const s = cur();
    if (s.start && !s.end && !s.pauseAt) el.textContent = mins(elapsed(s))
  }
  if (st.auto && st.date !== today() && !document.querySelector('input:focus,textarea:focus')) {
    setDate(today(), false);
    if (st.tab === 'log' && !st.settings) render()
  }
}
/* ---------- 2.0: keep screen on ---------- */
let wl = null, wlBusy = false;
function wakeSync() {
  const s = st.plan ? cur() : {}, run = !!(st.set.awake && st.plan && s.start && !s.end && !s.pauseAt && !document.hidden);
  if (run && !wl && !wlBusy && navigator.wakeLock) {
    wlBusy = true;
    navigator.wakeLock.request('screen').then(l => {
      wl = l;
      wlBusy = false;
      l.addEventListener('release', () => {wl = null})
    }).catch (() => {wlBusy = false})
  } else if (!run && wl) {try {wl.release()} catch (x) {}wl = null}
}
/* ---------- 0.13: smart card ---------- */
function smartCard() {
  if (st.date !== today() || liveSession()) return '';
  const E = engine(), c = [];
  const me = myId();
  const L = Object.entries(st.liveMap || {}).filter(([id]) => id !== me && memberOf(id));
  if (L.length) {
    const m = memberOf(L[0][0]);
    c.push(['crew', '🔥', `${esc(m.name)} is lifting right now`, `${esc(L[0][1].day||'Workout')} · tap to send some hype`])
  }
  const lf = crewOn() ? lastFinished() : null;
  if (lf && !lf.posted) c.push(['post', '📣', 'Post your workout to the crew', 'Share your muscle map and stats']);
  if (E.trial) {
    const t = E.trial;
    c.push(['rank', '⚔️', `Promotion trial: ${RANKS[t.tier][0]}`, `${Math.max(0,t.need-t.days)} more training day${t.need-t.days===1?'':'s'} and ${Math.max(0,t.needBeat-t.beat)} more lift${t.needBeat-t.beat===1?'':'s'} to beat this week`])
  }
  (st.set.goals || []).filter(g => !g.doneAt).forEach(g => {
    const I = goalInfo(g);
    if (I.status === 'behind') c.push(['stats', '🎯', `${esc(exName(g.ex))} ${g.w} is behind pace`, 'Open Goals to see your projection'])
  });
  if (feat('quests')) {
    const q = (st.set.chains && Object.keys(st.set.chains).length) ? chains()[0] : null;
    if (q) {
      const n = q.steps.find(s => !s.done);
      if (n) c.push(['stats', '🗺️', `Road to ${q.M}: next step`, `${esc(q.name)} ${n.w}×${n.r}`])
    }
  }
  const cp = crewOn() ? chalProgress() : null;
  if (cp && cp.cur < cp.target) c.push(['crew', '🏁', esc(cp.title), `${cp.cur.toLocaleString()} of ${cp.target.toLocaleString()} as a crew`]);
  if (feat('season') && E.lvl < 30) c.push(['rank', '🎖️', `Season level ${E.lvl}`, `${200-E.xp%200} XP to level ${E.lvl+1}`]);
  const x = c[0];
  if (!x) return '';
  return `<button class="card smart" data-smart="${x[0]}"><span class="si">${x[1]}</span><span class="stx"><b>${x[2]}</b><span class="sub">${x[3]}</span></span><span class="sc">›</span></button>`
}
/* ---------- rest timer one-liners ---------- */
const REST_LINES = ['Your phone is not a set', 'Scroll less, lift more', 'Breathe. Then go be dangerous', 'Hydrate or diedrate', 'The weights miss you already', 'Rest is part of the program. Allegedly', 'Don’t you dare check Instagram', 'Shake it out. Look tough', 'Count your blessings. And your reps', 'Chalk up. Mentally', 'The bar isn’t going to lift itself', 'Silence. Champion resting', 'Fix your hair, next set is the money set', 'Rest like a lion. Lift like a bigger lion'];
/* ---------- tappable plate ---------- */
const PLATE_LINES = ['Put me down, I’m tired', 'Hey! That tickles', 'I’m a 45, not a frisbee', 'Lift me, don’t poke me', 'I’ve seen things. Heavy things', 'Rack me when you’re done', 'Spin me right round', 'I’m load bearing, you know', 'Squat day? Please say squat day', 'You’re doing great, sweetie'];
function plateTalk(el) {
  const b = document.createElement('div');
  b.className = 'pbub';
  b.textContent = PLATE_LINES[Math.floor(Math.random() * PLATE_LINES.length)];
  el.appendChild(b);
  el.classList.remove('wob');
  void el.offsetWidth;
  el.classList.add('wob');
  haptic('tap');
  setTimeout(() => b.remove(), 2200)
}
/* ---------- excuse generator ---------- */
const EX_A = ['My pre-workout was decaf', 'My foam roller filed for divorce', 'Mercury is in retrograde and so are my gains', 'My protein shaker is in the wash', 'I pulled a muscle reaching for the remote', 'The gym playlist hurt my feelings', 'My dog said I looked too strong already', 'I’m saving my energy for leg day next year', 'My spotter is on vacation. In my mind', 'It’s chest day somewhere, just not here', 'My lifting shoes are in a mood', 'The squat rack and I need space', 'I’m in a committed relationship with my couch', 'My muscles requested a personal day', 'I’m bulking. Emotionally'];
/* ---------- Today: compact workout card ---------- */
function estMin(p, tot) {
  const r = (+p.rest || 90) + 40;
  return Math.max(10, Math.round(tot * r / 60 / 5) * 5)
}
{
  const _h = heroHTML;
  heroHTML = function() {try {return hero17()} catch (e) {console.warn('hero17', e); return _h()}}
}
function hero17() {
  const p = pd(), s = cur(), [dn, tot] = counts(), ss = sessStats(s), col = colorOf(p), t = today(), off = p.type === 'off';
  const when = st.date === t ? 'Today' : st.date < t ? 'Past' : 'Upcoming', dt = `${when} · ${DN[DAYS.indexOf(st.day)]} ${MON[toD(st.date).getMonth()]} ${toD(st.date).getDate()}`;
  const name = st.renaming ? `<input class="field rn" id="rename" value="${esc(p.name)}" aria-label="Day name" maxlength="28">` : `<h1 class="h17n">${esc(p.name)}</h1>`;
  let sub, act = '';
  if (off) sub = 'Rest day. Recover and come back stronger';
  else if (!s.start) sub = tot ? `${tot} set${tot===1?'':'s'} · about ${estMin(p,tot)} min` : 'No exercises yet. Add some below';
  else sub = `<span id="elapsed">${mins(elapsed(s))}</span> · <span id="hsets">${dn}/${tot}</span> sets · <span id="hvol">${kfmt(ss.vol)}</span> ${U()}${s.pauseAt?' · Paused':''}`;
  if (!off) {
    if (!s.start) act = `<button class="btn white h17go" id="startw">▶ Start workout</button>`;
    else if (!s.end) act = `<button class="btn white h17go" id="pausew">${s.pauseAt?'▶ Resume':'❚❚ Pause'}</button><button class="btn h17b" id="finish">Finish</button>`;
    if (s.start) act += `<button class="btn h17b" id="resetw" aria-label="Reset">${st.confirm==='rw'?'Tap to reset':'↺'}</button>`
  }
  return `<section class="h17" style="--accent:${PC[col][0]}"><div class="h17top"><div class="bigplate h17pl">${plateSVG(col,off?'rest':'idle',{big:true,label:p.name.toUpperCase(),ring:off?undefined:(tot?dn/tot:0)})}</div>
 <div class="h17t"><div class="h17k">${dt}${st.renaming?'':`<button class="ren h17r" id="renbtn" aria-label="Rename this day">✎</button>`}</div>${name}<div class="h17s">${sub}</div></div>
 <button class="h17m" id="tmore" aria-label="More">⋯</button></div>${act?`<div class="h17a">${act}</div>`:''}</section>`
}
/* ---------- faster logging ---------- */
function fillFromLast(ids) {
  const s = ensure();
  let n = 0;
  (ids || exList().map(e => e.id)).forEach(id => {
    const l = last(id);
    if (!l) return;
    const la = (l.ex[id] || []).filter(x => x && (x.w || x.r));
    if (!la.length) return;
    const cur = s.ex[id] = s.ex[id] || [];
    la.forEach((x, i) => {
      const c = cur[i] = cur[i] || {};
      if (c.d || c.w || c.r) return;
      if (x.w != null && x.w !== '') c.w = x.w;
      if (x.r != null && x.r !== '') c.r = x.r;
      n++
    })
  });
  if (n) {
    saveS();
    render();
    toast(`Filled ${n} set${n>1?'s':''} from last time. Check each one off as you go`)
  } else toast('Nothing to fill. No earlier sets for these');
  return n
}
function exList() {
  const L = [];
  try {todayGroups().forEach(g => g.forEach(e => L.push(e)))} catch (e) {}return L
}
function copySet(id, i) {
  const s = ensure(), a = s.ex[id] = s.ex[id] || [], x = a[i];
  if (!x || (x.w == null && x.r == null)) {
    toast('Fill this set first');
    return
  }
  const nx = a[i + 1] = a[i + 1] || {};
  if (nx.d) {
    toast('Next set is already done');
    return
  }
  nx.w = x.w;
  nx.r = x.r;
  if (x.q != null) nx.q = x.q;
  saveS();
  haptic('tap');
  render();
  toast(`Copied to set ${i+2}`)
}
let undo18 = null;
function delSet(id, i) {
  const s = ensure(), a = s.ex[id];
  if (!a || !a[i]) return;
  const was = JSON.parse(JSON.stringify(a));
  a.splice(i, 1);
  saveS();
  render();
  undo18 = {id, was, date: st.date, day: st.day};
  const el = $('undo18');
  if (el) {
    el.innerHTML = `<span>Set ${i+1} removed</span><button id="undo18b">Undo</button>`;
    el.classList.add('on');
    clearTimeout(el._t);
    el._t = setTimeout(() => {
      el.classList.remove('on');
      undo18 = null
    }, 6000)
  }
}
function undoSet() {
  if (!undo18) return;
  const s = ensure();
  if (st.date === undo18.date) {
    s.ex[undo18.id] = undo18.was;
    saveS();
    render();
    toast('Set restored')
  }
  undo18 = null;
  const el = $('undo18');
  if (el) el.classList.remove('on')
}
{
  let lp = null;
  const start = (e) => {
    const r = e.target.closest && e.target.closest('.srow');
    if (!r || e.target.closest('input,button')) return;
    const m = /^row-(.+)-(\d+)$/.exec(r.id);
    if (!m) return;
    const pt = e.touches ? e.touches[0] : e;
    r._sx = pt.clientX;
    r._sy = pt.clientY;
    r._m = m;
    clearTimeout(lp);
    lp = setTimeout(() => {
      r._held = 1;
      copySet(m[1], +m[2])
    }, 550)
  };
  const move = (e) => {
    const r = e.target.closest && e.target.closest('.srow');
    if (!r || r._sx == null) return;
    const pt = e.touches ? e.touches[0] : e, dx = pt.clientX - r._sx, dy = pt.clientY - r._sy;
    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) clearTimeout(lp);
    if (dx < 0 && Math.abs(dx) > Math.abs(dy)) {
      r.style.transform = `translateX(${Math.max(dx,-110)}px)`;
      r.classList.toggle('del18', dx < -70)
    }
  };
  const end = (e) => {
    clearTimeout(lp);
    const r = e.target.closest && e.target.closest('.srow');
    if (!r || r._sx == null) return;
    const pt = e.changedTouches ? e.changedTouches[0] : e, dx = pt.clientX - r._sx, m = r._m;
    r._sx = null;
    r.style.transform = '';
    r.classList.remove('del18');
    if (!r._held && dx < -70 && m) delSet(m[1], +m[2]);
    r._held = 0
  };
  document.addEventListener('touchstart', start, {passive: true});
  document.addEventListener('touchmove', move, {passive: true});
  document.addEventListener('touchend', end, {passive: true})
}
