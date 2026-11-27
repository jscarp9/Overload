/* ========================================================================
   Overload: Rendering and navigation

   Tab panes, render(), sheets, haptics, collapsible cards, the popup queue.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

/* ---------- render ---------- */
let roc = null;
/* ---------- 0.14: tabs stay built (instant switching) ---------- */
let DREV = 0;
const PANES = {}, CVOK = typeof CSS !== 'undefined' && CSS.supports && CSS.supports('content-visibility', 'hidden');
function paneKey() {return !st.plan ? 'onboard' : st.profile ? 'profile' : st.settings ? 'settings' : st.tab}
function uiKey() {
  return JSON.stringify([st.sub, st.prog, st.pday, st.date, st.day, st.menu || null, st.confirm || null, !!st.picker, !!st.editNote, !!st.snaps, st.set.theme, st.set.unit, new Date().getHours()])
}
function paneEl(k) {
  const m = $('main');
  let el = PANES[k] && PANES[k].el;
  if (!el || el.parentNode !== m) {
    el = document.createElement('div');
    el.className = 'pane';
    el.dataset.k = k;
    m.appendChild(el);
    PANES[k] = {el, drev: -1}
  }
  return el
}
function paneHide(el) {
  el.classList.add('off');
  el.classList.remove('fin');
  if (!CVOK) el.style.display = 'none';
  el.querySelectorAll('[id]').forEach(x => {
    x.dataset.cid = x.id;
    x.removeAttribute('id')
  })
}
function paneShow(el) {
  el.classList.remove('off');
  if (!CVOK) el.style.display = '';
  el.querySelectorAll('[data-cid]').forEach(x => {
    if (!x.id) x.id = x.dataset.cid;
    delete x.dataset.cid
  })
}
function paneHTML(h) {
  const k = paneKey(), el = paneEl(k);
  Object.entries(PANES).forEach(([k2, p]) => {if (k2 !== k && !p.el.classList.contains('off')) paneHide(p.el)});
  paneShow(el);
  el.innerHTML = h;
  if (k === 'settings') try {collapse15(el)} catch (e) {}Object.assign(PANES[k], {drev: DREV, ui: uiKey(), ts: Date.now(), cs: st.cs, live: st.liveMap})
}
function paneCached() {
  const k = paneKey(), p = PANES[k];
  if (!p || !st.plan || p.drev !== DREV || p.ui !== uiKey() || p.cs !== st.cs || p.live !== st.liveMap || Date.now() - p.ts > 180000 || k === 'settings' || k === 'onboard') return false;
  renderChrome();
  Object.entries(PANES).forEach(([k2, q]) => {if (k2 !== k && !q.el.classList.contains('off')) paneHide(q.el)});
  paneShow(p.el);
  return true
}
function paneFade() {
  const k = paneKey(), p = PANES[k];
  if (!p) return;
  p.el.classList.remove('fin');
  requestAnimationFrame(() => p.el.classList.add('fin'))
}
function renderChrome() {
  const E = engine();
  $('rankbtn').innerHTML = emblem(E.rank, E.rank >= 6 ? 'spin' : '');
  $('rankbtn').setAttribute('aria-pressed', st.profile);
  ['log', 'crew', 'stats', 'plan'].forEach(t => $('t-' + t).setAttribute('aria-pressed', !st.settings && !st.profile && st.tab === t));
  navSync16();
  dots17();
  {
    const k = curArt();
    if (artKey !== k + innerWidth + 'x' + innerHeight + (isDark() ? 'd' : 'l')) art(k)
  }
}
function render() {
  pid = 0;
  {
    const k = curArt();
    if (artKey !== k + innerWidth + 'x' + innerHeight + (isDark() ? 'd' : 'l')) art(k)
  }
  const hr = new Date().getHours();
  $('hi').textContent = st.set.name ? `${hr<12?'Good morning':hr<18?'Good afternoon':'Good evening'}, ${st.set.name}` : '';
  $('wm').innerHTML = plateSVG('red', 'done') + 'Overload' + (st.set.acct ? `<span id="syncdot" class="sdot s-${st.syncState.state==='off'?'ok':st.syncState.state}"></span>` : '');
  if (!st.plan) {
    paneHTML(renderOnboard() + `<button class="card obsign" id="acin"><span>☁️</span><span class="stx"><b>Already have an account?</b><span class="sub">Sign in to bring back all your workouts</span></span><span class="sc">›</span></button>`);
    $('dock').classList.add('hide');
    $('gear').classList.add('hide');
    $('rankbtn').classList.add('hide');
    return
  }
  $('dock').classList.remove('hide');
  $('gear').classList.remove('hide');
  {
    const E = engine();
    $('rankbtn').classList.remove('hide');
    $('rankbtn').innerHTML = emblem(E.rank, E.rank >= 6 ? 'spin' : '');
    $('rankbtn').setAttribute('aria-pressed', st.profile)
  }
  ['log', 'crew', 'stats', 'plan'].forEach(t => $('t-' + t).setAttribute('aria-pressed', !st.settings && !st.profile && st.tab === t));
  navSync16();
  dots17();
  paneHTML(st.profile ? renderProfile16() : st.settings ? renderSettings() : st.tab === 'log' ? renderLog() : st.tab === 'crew' ? renderCrew14() : st.tab === 'stats' ? seg('stats', [['ov', 'Overview'], ['lifts', 'Lifts']], st.sub.stats) + (st.sub.stats === 'lifts' ? renderProg() : goalsHTML() + chainsHTML() + renderStats()) : seg('plan', [['plan', 'Plan'], ['notes', 'Notes']], st.sub.plan) + (st.sub.plan === 'notes' ? renderNotes() : renderPlan()));
  if (roc) {
    roc.disconnect();
    roc = null
  }
  const c = $('chart');
  if (c) {
    roc = new ResizeObserver(drawChart);
    roc.observe(c)
  }
  if (st.picker) {
    const ps = $('psearch');
    if (ps) ps.focus({preventScroll: true})
  }
}
function ensure() {
  const k = sid();
  if (!st.sessions[k]) st.sessions[k] = {date: st.date, day: st.day, ex: {}, names: {}};
  const s = st.sessions[k];
  s.ex = s.ex || {};
  s.names = s.names || {};
  return s
}
let svT = null, plT = null;
let pendS = false, pendP = false;
const saveS = () => {
  DREV++;
  dirtyEng();
  pendS = true;
  clearTimeout(svT);
  svT = setTimeout(() => {
    pendS = false;
    if (!store('liftlog', st.sessions)) toast('Could not save. Storage is full or blocked')
  }, 250)
};
const saveP = () => {
  dirtyEng();
  clearTimeout(plT);
  pendP = true;
  plT = setTimeout(() => {
    pendP = false;
    store('overload-plan', st.plan)
  }, 250)
};
const saveN = () => store('overload-notes', st.notes);
const saveSet = () => store('overload-settings', st.set);
function startClock(s) {if (!s.start) {s.start = Date.now(); s.paused = 0; delete s.pauseAt; delete s.end}}
function liveHero() {
  const s = cur(), [dn, tot] = counts(), ss = sessStats(s);
  if ($('hsets')) $('hsets').textContent = `${dn}/${tot}`;
  if ($('hvol')) $('hvol').textContent = kfmt(ss.vol);
  const r = $('bigring');
  if (r) {
    const C = 2 * Math.PI * 49;
    r.setAttribute('stroke-dashoffset', (C * (1 - (tot ? dn / tot : 0))).toFixed(1))
  }
  if ($('startw') && s.start) render()
}
function setDate(ds, manual) {
  st.date = ds;
  st.day = DAYS[dayIdx(ds)];
  st.pday = DAYS.indexOf(st.day);
  st.picker = null;
  st.confirm = null;
  st.auto = !manual || ds === today()
}
/* ---------- 2.0: sheets ---------- */
const PLB = {
  lb: [[45, '#ef2b36', 100], [35, '#ffbe0b', 90], [25, '#16b65b', 78], [10, '#e8e8ec', 58], [5, '#2563ff', 46], [2.5, '#23252b', 38]], kg: [[25, '#ef2b36', 100], [20, '#2563ff', 100], [15, '#ffbe0b', 90], [10, '#16b65b', 80], [5, '#e8e8ec', 58], [2.5, '#23252b', 46], [1.25, '#9aa0ae', 38]]
};
function plateCalc(total, bar) {
  const list = PLB[U()] || PLB.lb;
  let per = (total - bar) / 2;
  const out = [];
  if (per < 0) return {out, left: per};
  for (const [w] of list) {while (per >= w - 1e-9) {out.push(w); per = +(per - w).toFixed(3)}}return {out, left: per}
}
function platesBody() {
  const sh = st.sheet, t = +sh.w || 0, {out, left} = plateCalc(t, +sh.bar), list = PLB[U()] || PLB.lb, info = w => list.find(x => x[0] === w);
  const vis = out.map(w => {
    const [, c, h] = info(w);
    return `<span class="pl" style="height:${h}%;background:${c}"></span>`
  }).join('');
  let msg = '';
  if (!t) msg = 'Enter a total weight.';
  else if (t < +sh.bar) msg = 'That’s less than the bar.';
  else if (!out.length) msg = 'Just the bar.';
  else msg = `Per side: <b>${out.join(' + ')}</b>`;
  if (t && left > 0.01 && t >= +sh.bar) msg += ` <span class="sub">(${(left*2).toFixed(1).replace('.0','')} ${U()} short of exact)</span>`;
  return `<div class="bar"><span class="sleeve"></span>${vis.split('</span>').filter(Boolean).reverse().map(x=>x+'</span>').join('')}<span class="shaft"></span>${vis}<span class="sleeve"></span></div><p style="text-align:center;margin:4px 0 0;font-size:17px">${msg}</p>`
}
function renderSheet() {
  const el = $('sheet'), sh = st.sheet;
  if (!sh) {
    el.innerHTML = '';
    return
  }
  let h = '';
  if (sh.type === 'news') h = `<h2>What's new in ${VERSION}</h2><p class="sub">Here's everything new in this update.</p><ul class="news">${NEWS.map(([t,d])=>`<li>${t}<span>${d}</span></li>`).join('')}</ul><button class="btn acc wide" id="newsok" style="min-height:52px">Let's go 💪</button>`;
  if (sh.type === 'plates') {
    const bars = U() === 'kg' ? [20, 15, 0] : [45, 35, 0];
    h = `<div class="ghd"><h2>Plate calculator</h2><button class="btn sm" id="sheetx">Done</button></div><p class="sub">${esc(sh.name||'')}</p><div class="form2" style="margin-top:10px"><input class="field" id="pcw" type="number" inputmode="decimal" value="${sh.w}" placeholder="Total ${U()}" aria-label="Total weight"><div class="seg" style="margin:0">${bars.map(b=>`<button data-bar="${b}" aria-pressed="${+sh.bar===b}">${b?b+' bar':'No bar'}</button>`).join('')}</div></div><div id="pcout">${platesBody()}</div><p class="sub" style="text-align:center">Colours match standard competition plates.</p>`
  }
  if (sh.type === 'share') h = `<div class="ghd"><h2>${esc(sh.title||'Share your workout')}</h2><button class="btn sm" id="sheetx">Close</button></div>${sh.url?`<img class="shareimg" src="${sh.url}" alt="Workout card"><div class="exfoot" style="margin-top:12px"><button class="btn acc wide" id="sharego" style="min-height:52px">${navigator.canShare?'Share to Instagram, Messages…':'Save image'}</button></div><p class="sub" style="text-align:center">Or press and hold the image to save it.</p>`:'<p class="sub">Making your card…</p>'}`;
  if (sh.type === 'badges') h = `<div class="celebrate"><div class="cbig">${sh.items[0]?sh.items[0].icon:'🏅'}</div><h2>${sh.items.length>1?sh.items.length+' badges unlocked!':'Badge unlocked!'}</h2></div><div class="bcase" style="margin:14px 0">${sh.items.map(b=>badgeTile(b,true)).join('')}</div><p class="sub" style="text-align:center">${engine().bcount} badges toward your rank</p><button class="btn acc wide" id="sheetx" style="min-height:52px">Let's go 💪</button>`;
  if (sh.type === 'rankup') {
    const r = RANKS[sh.rank], u = UNLOCKS.find(x => x[0] === sh.rank);
    h = `<div class="celebrate rk" style="--rc:${r[4]}"><div class="forge">${emblem(sh.rank,'spin1')}</div><div class="sub">You ranked up to</div><h1 style="font-size:52px">${r[0]}</h1>${u?`<p class="sub">Unlocked: <b style="color:var(--ink)">${u[1]}</b></p>`:''}</div><div class="exfoot" style="margin-top:12px"><button class="btn acc" id="shareprof2" style="flex:1;min-height:52px">📸 Share</button><button class="btn" id="sheetx" style="flex:1;min-height:52px">Let's go</button></div>`
  }
  if (sh.type === 'report') {
    const a = sh.a, b = sh.b, pct = b.vol ? Math.round((a.vol - b.vol) / b.vol * 100) : null;
    h = `<div class="ghd"><h2>Your week</h2><button class="btn sm" id="sheetx">Close</button></div><p class="sub">${MON[toD(sh.pw[0]).getMonth()]} ${toD(sh.pw[0]).getDate()} to ${MON[toD(sh.pw[6]).getMonth()]} ${toD(sh.pw[6]).getDate()}</p><div class="grade g${sh.g.replace('+','p')}">${sh.g}</div><div class="tiles four"><div class="tile"><div class="num">${a.done}/${a.planned}</div><div class="sub">Sessions</div></div><div class="tile"><div class="num">${a.sets}</div><div class="sub">Sets</div></div><div class="tile"><div class="num">${kfmt(a.vol)}</div><div class="sub">${U()}${pct!==null?` · ${pct>=0?'+':''}${pct}%`:''}</div></div><div class="tile"><div class="num">${a.prs}</div><div class="sub">PRs</div></div></div>${sh.earned.length?`<p class="sub">Badges: <b style="color:var(--ink)">${sh.earned.join(', ')}</b></p>`:''}<p class="sub">${sh.g==='A+'?'Every session, a PR and more volume. Elite week.':sh.g==='A'?'You hit every planned session. Now chase a PR.':sh.g==='B'?'Solid week. One more session gets you an A.':'Every week is a fresh start. New quests are up.'}</p><button class="btn acc wide" id="sheetx" style="min-height:52px">New week, let's go</button>`
  }
  if (sh.type === 'missing') h = `<h2>Restore your data?</h2><p class="sub">Your workouts look missing on this screen, but a saved copy from ${new Date(sh.ts).toLocaleString()} has ${sh.theirs} workout${sh.theirs===1?'':'s'} (you have ${sh.mine} here).</p><p class="sub">If you deleted them on purpose, tap Not now. Older copies stay in Settings → Restore from a past day.</p><div class="exfoot" style="margin-top:12px"><button class="btn acc" id="dorestore" style="flex:1;min-height:52px">Restore it</button><button class="btn" id="missno" style="flex:1;min-height:52px">Not now</button></div>`;
  if (sh.type === 'nhist') h = `<div class="ghd"><h2>Notes history</h2><button class="btn sm" id="sheetx">Done</button></div><p class="sub">${esc(sh.name)}</p>${notesHistoryHTML(sh.id)}`;
  if (sh.type === 'copy') {
    const cur2 = st.plan.days[st.pday], sel = st.copySel, over = sel.filter(i => (st.plan.days[i].groups || []).length);
    h = `<div class="ghd"><h2>Copy ${esc(cur2.name)}</h2><button class="btn sm" id="sheetx">Cancel</button></div><p class="sub">Pick the days to copy it onto. Exercises, supersets, sets, reps, rest time and color all come with it.</p><div class="days">${DN.map((n,i)=>i===st.pday?`<button disabled style="opacity:.35">${n}</button>`:`<button data-csel="${i}" aria-pressed="${sel.includes(i)}">${n}</button>`).join('')}</div>${over.length?`<p class="sub" style="color:var(--red)">This replaces what's on ${over.map(i=>DN[i]).join(', ')}. Your logged history stays.</p>`:''}<button class="btn acc wide" id="docopy" style="min-height:52px" ${sel.length?'':'disabled'}>${sel.length?`Copy to ${sel.length} day${sel.length>1?'s':''}`:'Pick a day'}</button>`
  }
  {
    const ch = crewSheet(sh);
    if (ch !== null) h = ch
  }
  {
    const ch = acctSheet(sh) || sheet13(sh);
    if (ch) h = ch
  }
  {
    const c14 = sheet14(sh);
    if (c14) h = c14
  }
  {
    const c15 = sheet15(sh);
    if (c15) h = c15
  }
  el.innerHTML = `<div class="sheetbg" id="sheetbg"><div class="sheet" role="dialog" aria-modal="true"><div class="grab"></div>${h}</div></div>`
}
function markNews() {
  st.set.seenVer = VERSION;
  saveSet()
}
/* ---------- 0.13: haptics ---------- */
let hapEl = null;
function haptic(k) {
  try {
    if (st.set.vibe === false) return;
    if (navigator.vibrate) {
      navigator.vibrate(k === 'heavy' ? [30, 40, 70] : k === 'success' ? [14, 40, 22] : 10);
      return
    }
    if (!hapEl) {
      hapEl = document.createElement('label');
      hapEl.setAttribute('aria-hidden', 'true');
      hapEl.innerHTML = '<input type="checkbox" switch>';
      hapEl.style.cssText = 'position:fixed;left:-200px;top:0;opacity:0;pointer-events:none';
      document.body.appendChild(hapEl)
    }
    hapEl.click();
    if (k === 'success' || k === 'heavy') setTimeout(() => hapEl.click(), 110);
    if (k === 'heavy') setTimeout(() => hapEl.click(), 220)
  } catch (e) {}
}
/* ---------- collapsible cards (settings and profile) ---------- */
const OPEN15 = new Set(['Account', '☁️ Account', '☁️ Back up and sync', 'Safety net', '👑 Founder']);
function collapse15(root) {
  if (!root) return;
  root.querySelectorAll(':scope > .card, :scope > div > .card').forEach(c => {
    if (c.dataset.c15) return;
    const h = c.querySelector(':scope > h3');
    if (!h) return;
    const t = h.textContent.trim();
    if (OPEN15.has(t)) return;
    c.dataset.c15 = 1;
    c.classList.add('c15');
    if (!(st._open15 || {})[t]) c.classList.add('shut');
    h.classList.add('c15h');
    h.setAttribute('role', 'button');
    h.dataset.c15t = t
  })
}
/* ---------- smooth sheets: swap content in place, no close and reopen ---------- */
{
  const _rs = renderSheet;
  renderSheet = function() {
    const el = $('sheet'), old = el.querySelector('.sheet'), top = old ? old.scrollTop : 0, pk = el.dataset.k || '';
    _rs();
    const s = el.querySelector('.sheet');
    if (!s || !st.sheet) {
      el.dataset.k = '';
      return
    }
    const k = st.sheet.type + '|' + (st.sheet.sec || '');
    if (old) {
      s.classList.add('na');
      s.parentNode.classList.add('na');
      if (pk === k) s.scrollTop = top
    }
    el.dataset.k = k
  }
}
/* ---------- one popup at a time ---------- */
const AUTO18 = ['news', 'rankup', 'badges', 'crewrank', 'report', 'tour', 'box'];
let shown18 = null;
const queue18 = [];
{
  const _rs = renderSheet;
  renderSheet = function() {
    const el = $('sheet'), open = !!el.querySelector('.sheet');
    if (st.sheet && open && shown18 && st.sheet !== shown18 && AUTO18.includes(st.sheet.type) && AUTO18.includes(shown18.type) && !st.sheet.back) {
      if (queue18.length < 5) queue18.push(st.sheet);
      st.sheet = shown18;
      return
    }
    if (!st.sheet && queue18.length) {
      _rs();
      shown18 = null;
      setTimeout(() => {if (!st.sheet && queue18.length) {st.sheet = queue18.shift(); renderSheet()}}, 380);
      return
    }
    _rs();
    shown18 = st.sheet || null
  }
}
