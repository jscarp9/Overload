/* ========================================================================
   Overload: Plan

   The Plan page and its editor.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

/* ---------- plan page refresh ---------- */
const fmtRest21 = v => `${Math.floor(v/60)}:${String(v%60).padStart(2,'0')}`;
let undo21 = null;
function showUndo21(msg) {
  const el = $('undo18');
  if (!el) return;
  el.innerHTML = `<span>${esc(msg)}</span><button id="undo21b">Undo</button>`;
  el.classList.add('on');
  clearTimeout(el._t);
  el._t = setTimeout(() => {
    el.classList.remove('on');
    undo21 = null
  }, 6000)
}
function doUndo21() {
  if (!undo21) return;
  const u = undo21, d = st.plan.days[u.day];
  if (d) {
    if (u.group) d.groups.splice(Math.min(u.gi, d.groups.length), 0, [u.ex]);
    else if (d.groups[u.gi]) d.groups[u.gi].splice(Math.min(u.ei, d.groups[u.gi].length), 0, u.ex);
    else d.groups.push([u.ex]);
    saveP();
    render();
    toast(u.ex.name + ' is back')
  }
  undo21 = null;
  const el = $('undo18');
  if (el) el.classList.remove('on')
}
{
  const _af = addFromPicker;
  addFromPicker = function(...a) {
    const chain = st.ss21 && st.picker && st.picker.ctx === 'plan' && st.picker.gi === -1;
    const r = _af.apply(this, a);
    if (chain) {
      st.ss21 = 0;
      const d = st.plan.days[st.pday];
      st.picker = {ctx: 'plan', gi: d.groups.length - 1};
      render();
      toast('Now pick the exercise to pair with it');
      const pk = $('pick');
      if (pk) pk.scrollIntoView({behavior: 'smooth', block: 'center'})
    }
    return r
  }
}
async function click21(b) {
  const d = b.dataset, day = st.plan && st.plan.days[st.pday];
  if (b.id === 'ptip21') {
    st.set.ptip21 = 1;
    saveSet();
    render();
    return true
  }
  if (b.id === 'pd21edit') {
    st.pd21 = !st.pd21;
    st.pd21m = false;
    render();
    return true
  }
  if (b.id === 'pd21done') {
    st.pd21 = false;
    render();
    return true
  }
  if (b.id === 'pd21more') {
    st.pd21m = !st.pd21m;
    render();
    return true
  }
  if (b.id === 'tpl21') {
    st.tpl21 = true;
    st.pd21m = false;
    render();
    const t = $('tplsec');
    if (t) t.scrollIntoView({behavior: 'smooth', block: 'start'});
    return true
  }
  if (b.id === 'copyday') {st.pd21m = false}
  if (d.pday !== undefined) {
    st.pex21 = null;
    st.pd21 = false;
    st.pd21m = false;
    st.note21 = null
  }
  if (d.pex21) {
    st.pex21 = st.pex21 === d.pex21 ? null : d.pex21;
    st.note21 = null;
    st.picker = null;
    render();
    return true
  }
  if (d.note21) {
    st.note21 = d.note21;
    render();
    const i = document.querySelector('.cue21');
    if (i) i.focus();
    return true
  }
  if (d.st21 && day) {
    const [key, f, dl] = d.st21.split(':'), [gi, ei] = key.split('-').map(Number), e = day.groups[gi] && day.groups[gi][ei];
    if (!e) return true;
    let v = Math.max(f === 'sets' ? 1 : 1, (+e[f] || 0) + (+dl));
    if (f !== 'sets') v = Math.min(100, v);
    else v = Math.min(20, v);
    e[f] = v;
    if (f === 'lo' && +e.hi < v) e.hi = v;
    if (f === 'hi' && +e.lo > v) e.lo = v;
    saveP();
    render();
    return true
  }
  if (d.gdn21 !== undefined && day) {
    const i = +d.gdn21;
    if (i < day.groups.length - 1) {
      [day.groups[i + 1], day.groups[i]] = [day.groups[i], day.groups[i + 1]];
      if (st.pex21) {
        const [g, e] = st.pex21.split('-').map(Number);
        if (g === i) st.pex21 = (i + 1) + '-' + e
      }
      saveP();
      render()
    }
    return true
  }
  if (d.gup !== undefined && st.pex21 && day) {
    const i = +d.gup, [g, e] = st.pex21.split('-').map(Number);
    if (i > 0 && g === i) st.pex21 = (i - 1) + '-' + e;
    return false
  }
  if (d.edel21 && day) {
    const [gi, ei] = d.edel21.split('-').map(Number), gr = day.groups[gi], ex = gr && gr[ei];
    if (!ex) return true;
    try {
      trashPutSync('exercise', `${ex.name} (from ${day.name||'plan'})`, {day: st.pday, gi, ex: JSON.parse(JSON.stringify(ex))})
    } catch (x) {}
    gr.splice(ei, 1);
    const group = !gr.length;
    if (group) day.groups.splice(gi, 1);
    undo21 = {day: st.pday, gi, ei, ex: JSON.parse(JSON.stringify(ex)), group};
    st.pex21 = null;
    saveP();
    render();
    showUndo21(ex.name + ' removed');
    return true
  }
  if (b.id === 'ss21new') {
    st.ss21 = 1;
    st.picker = {ctx: 'plan', gi: -1};
    render();
    const pk = $('pick');
    if (pk) pk.scrollIntoView({behavior: 'smooth', block: 'center'});
    return true
  }
  if (b.id === 'undo21b') {
    doUndo21();
    return true
  }
  return false
}
/* ---------- 0.24: clean Plan page ---------- */
const LINK24 = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1"/><path d="M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/></svg>';
const GRIP24 = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 8h14M5 12h14M5 16h14"/></svg>';
const reps24 = e => `${e.sets||0} × ${e.lo||0}${e.hi&&+e.hi!==+e.lo?'–'+e.hi:''}`;
function day24() {return st.plan && st.plan.days[st.pday]}
function row24(e, gi, ei) {
  const key = gi + '-' + ei;
  return `<div class="r24" data-r24="${key}"><div class="r24del">Delete</div><div class="r24in" data-pex24="${key}" role="button" tabindex="0" aria-label="Edit ${esc(e.name||'exercise')}"><span class="r24n"><b>${esc(e.name||'Exercise')}</b><span>${reps24(e)} · ${esc(e.m||'')}${e.cue?' · 📝':''}</span></span><span class="r24h" data-drag24="${key}" aria-label="Drag to reorder">${GRIP24}</span></div></div>`
}
renderPlan = function() {
  const d = st.plan.days[st.pday], col = colorOf(d), off = d.type === 'off', f = FIN[col], ai = typeof aiOnNow === 'function' && aiOnNow();
  let h = `<div class="days">${DN.map((n,i)=>{const dd=st.plan.days[i];return `<button data-pday="${i}" aria-pressed="${i===st.pday}">${n}<i style="background:${dd.type==='off'?'#c9cdd4':PC[colorOf(dd)][0]}"></i></button>`}).join('')}</div>`;
  if (!st.set.ptip21) h += `<div class="tip21"><span>Set each day once. It repeats every week.</span><button id="ptip21" aria-label="Got it">Got it</button></div>`;
  const empty = st.plan.days.every(x => !(x.groups || []).length);
  if (ai && (empty || !st.set.aiSeen23)) h += `<button class="aibar23" id="ai23open2">${ORB23}<span><b>Build my plan with AI</b><i>From a photo of your notes, a list, or your history</i></span><em>›</em></button>`;
  const sub = off ? 'Rest day' : [d.focus || 'Add a focus', `Rest ${fmtRest21(+d.rest||90)}`].join(' · ');
  h += `<div class="pd21"><button class="pd21p" id="platepick" aria-label="Change plate">${plateSVG(col,'done')}</button><button class="pd21t" id="pd21edit" aria-expanded="${!!st.pd21}"><b>${esc(d.name||'Untitled day')}</b><span>${esc(sub)}</span>${f?`<em>${esc(f[0])}</em>`:''}</button>${ai?`<button class="ai23chip" id="ai23open" aria-label="Build my plan with AI"><span>✦</span>AI</button>`:''}<button class="pd21m" id="pd21more" aria-label="More options">⋯</button></div>`;
  if (st.pd21m) h += `<div class="pm21">${ai?'<button id="ai23open" class="ai23mi">✦ Build my plan with AI</button>':''}<button id="pd21edit2">✎ Edit day name, focus and rest</button><button id="copyday">📋 Copy this day to other days</button><button id="tpl21">🗂 Templates</button></div>`;
  if (st.pd21) h += `<div class="pe21"><label><span>Name</span><input class="field" data-pf2="name" value="${esc(d.name)}" placeholder="Day name"></label><label><span>Type</span><select data-pf2="type">${Object.entries(TYPES).map(([k,t])=>`<option value="${k}"${d.type===k?' selected':''}>${t[0]}</option>`).join('')}</select></label>${off?'':`<label class="w"><span>Focus</span><input class="field" data-pf2="focus" value="${esc(d.focus)}" placeholder="e.g. Chest and back"></label><label><span>Rest between sets</span><select data-pf2="rest">${[30,45,60,75,90,105,120,150,180,240].map(v=>`<option value="${v}"${+d.rest===v?' selected':''}>${fmtRest21(v)}</option>`).join('')}</select></label>`}<button class="btn sm acc" id="pd21done">Done</button></div>`;
  if (off) {
    h += `<div class="emp24">😴<b>Rest day</b><span>Tap the day name to change it to a training day.</span></div>`
  } else {
    const G = d.groups;
    h += `<div class="l24" id="l24">`;
    G.forEach((g, gi) => {
      if (gi > 0) {
        const a = G[gi - 1];
        if (a.length + g.length <= 3) h += `<div class="gap24"><button class="lk24" data-pair24="${gi-1}" aria-label="Superset ${esc(a[a.length-1].name)} with ${esc(g[0].name)}">${LINK24}</button></div>`;
        else h += `<div class="gap24 n"></div>`
      }
      h += `<div class="g24${g.length>1?' ss24':''}" data-g24="${gi}">${g.length>1?'<div class="sst24">Superset</div>':''}`;
      g.forEach((e, ei) => {
        if (ei > 0) h += `<div class="gap24 in"><button class="lk24 on" data-unpair24="${gi}-${ei-1}" aria-label="Unpair ${esc(g[ei-1].name)} and ${esc(e.name)}">${LINK24}</button></div>`;
        h += row24(e, gi, ei)
      });
      h += `</div>`
    });
    if (!G.length) h += `<div class="emp24">🏋️<b>No exercises yet</b><span>${ai?'Add one below, or tap ✦ AI to build the whole week.':'Add your first one below.'}</span></div>`;
    h += `</div>`;
    h += st.picker && st.picker.ctx === 'plan' ? pickerHTML() : `<button class="btn go add24" data-padd="-1">＋ Add exercise</button>`;
    if (G.length && !st.set.hint24) h += `<p class="hint24">Tap to edit · drag ≡ to reorder · swipe left to delete · tap 🔗 to pair or unpair <button id="hint24x">Got it</button></p>`
  }
  if (st.tpl21) h += `<div class="sec" id="tplsec"><h3>Templates</h3><p class="sub">Replaces the whole week. Your logged history stays.</p><div class="tpl">${Object.entries(TEMPLATES).map(([k,t])=>`<button data-tpl="${k}">${plateSVG(t.c,'done')}<span><b>${st.confirm==='t'+k?'Tap again to replace your plan':t.label}</b><span class="sub">${t.desc}</span></span></button>`).join('')}</div></div>`;
  return h
};
/* group surgery */
function pair24(gi) {
  const G = day24().groups, a = G[gi], b = G[gi + 1];
  if (!a || !b) return;
  if (a.length + b.length > 3) {
    toast('A superset can have up to 3 exercises');
    return
  }
  G.splice(gi, 2, [...a, ...b]);
  saveP();
  render();
  try {haptic('tap')} catch (e) {}toast('Superset made')
}
function unpair24(gi, ei) {
  const G = day24().groups, g = G[gi];
  if (!g || ei >= g.length - 1) return;
  G.splice(gi, 1, g.slice(0, ei + 1), g.slice(ei + 1));
  saveP();
  render();
  try {haptic('tap')} catch (e) {}toast('Unpaired')
}
function del24(gi, ei) {
  const d = day24(), gr = d.groups[gi], ex = gr && gr[ei];
  if (!ex) return;
  try {
    trashPutSync('exercise', `${ex.name} (from ${d.name||'plan'})`, {day: st.pday, gi, ex: clone(ex)})
  } catch (x) {}
  gr.splice(ei, 1);
  const group = !gr.length;
  if (group) d.groups.splice(gi, 1);
  undo21 = {day: st.pday, gi, ei, ex: clone(ex), group};
  saveP();
  render();
  showUndo21(ex.name + ' removed')
}
function move24(gi, ei, k) {
  const G = day24().groups, ex = G[gi][ei];
  G[gi].splice(ei, 1);
  let t = k;
  if (!G[gi].length) {
    G.splice(gi, 1);
    if (gi < k) t--
  }
  G.splice(Math.max(0, Math.min(G.length, t)), 0, [ex]);
  saveP();
  render()
}
/* editor card */
function stp24(fld, lbl, v) {
  return `<div class="st24"><span>${lbl}</span><div><button data-st24="${fld}:-1" aria-label="Less ${lbl}">−</button><input type="number" inputmode="numeric" data-f24="${fld}" value="${v}" aria-label="${lbl}"><button data-st24="${fld}:1" aria-label="More ${lbl}">+</button></div></div>`
}
function ed24(sh) {
  const d = st.plan.days[sh.day], g = d && d.groups[sh.gi], e = g && g[sh.ei];
  if (!e) return `<div class="ghd"><h2>Exercise</h2><button class="btn sm" id="sheetx">Done</button></div><p class="sub">This exercise was moved or removed.</p>`;
  const next = d.groups[sh.gi + 1], ss = g.length > 1;
  return `<div class="ghd"><span class="sub" style="font-weight:700">${esc(d.name)}</span><button class="btn sm acc" id="sheetx">Done</button></div>
 <input class="field nm24" data-f24="name" value="${esc(e.name)}" aria-label="Exercise name" maxlength="40">
 <div class="mu24"><span>Muscle</span><select data-f24="m" aria-label="Muscle">${MUSCLES.map(x=>`<option${x===e.m?' selected':''}>${x}</option>`).join('')}</select></div>
 <div class="sts24">${stp24('sets','Sets',e.sets)}${stp24('lo','Min reps',e.lo)}${stp24('hi','Max reps',e.hi)}</div>
 <input class="field" data-f24="cue" value="${esc(e.cue||'')}" placeholder="Form cue or note (optional)" aria-label="Note" maxlength="80">
 <div class="act24">${ss?`<button id="ed24unpair">${LINK24}<span>Take out of superset</span></button>`:next&&g.length+next.length<=3?`<button id="ed24pair">${LINK24}<span>Superset with ${esc(next[0].name)}</span></button>`:''}
 <button class="rm" id="ed24del">Remove from this day</button></div>`
}function open24(key) {
  const [gi, ei] = key.split('-').map(Number);
  st.sheet = {type: 'pex24', day: st.pday, gi, ei};
  renderSheet()
}
async function sheetClick24(b) {
  const sh = st.sheet, d = b.dataset;
  if (!sh || sh.type !== 'pex24') return false;
  const dd = st.plan.days[sh.day], g = dd && dd.groups[sh.gi], e = g && g[sh.ei];
  if (b.id === 'pex24done' || b.id === 'sheetx') {
    st.sheet = null;
    renderSheet();
    render();
    return true
  }
  if (!e) return false;
  if (d.st24) {
    const [f, dl] = d.st24.split(':');
    let v = Math.max(1, (+e[f] || 0) + (+dl));
    v = Math.min(f === 'sets' ? 20 : 100, v);
    e[f] = v;
    if (f === 'lo' && +e.hi < v) e.hi = v;
    if (f === 'hi' && +e.lo > v) e.lo = v;
    saveP();
    renderSheet();
    return true
  }
  if (b.id === 'ed24unpair') {
    const G = dd.groups;
    G.splice(sh.gi, 1, ...(sh.ei > 0 ? [g.slice(0, sh.ei)] : []), [e], ...(sh.ei < g.length - 1 ? [g.slice(sh.ei + 1)] : []));
    saveP();
    st.sheet = null;
    renderSheet();
    render();
    toast('Taken out of the superset');
    return true
  }
  if (b.id === 'ed24pair') {
    st.sheet = null;
    renderSheet();
    pair24(sh.gi);
    return true
  }
  if (b.id === 'ed24del') {
    st.sheet = null;
    renderSheet();
    del24(sh.gi, sh.ei);
    return true
  }
  return false
}
async function click24(b) {
  const d = b.dataset;
  if (d.pair24 !== undefined) {
    pair24(+d.pair24);
    return true
  }
  if (d.unpair24) {
    const [gi, ei] = d.unpair24.split('-').map(Number);
    unpair24(gi, ei);
    return true
  }
  if (b.id === 'pd21edit2') {
    st.pd21 = true;
    st.pd21m = false;
    render();
    return true
  }
  if (b.id === 'hint24x') {
    st.set.hint24 = 1;
    saveSet();
    render();
    return true
  }
  return false
}
/* gestures: tap to edit, swipe left to delete, drag the grip to reorder */
function boot24() {
  let g = null;
  document.addEventListener('input', e => {
    const t = e.target, sh = st.sheet;
    if (!t.dataset || !t.dataset.f24 || !sh || sh.type !== 'pex24') return;
    const dd = st.plan.days[sh.day], x = dd && dd.groups[sh.gi] && dd.groups[sh.gi][sh.ei];
    if (!x) return;
    const f = t.dataset.f24;
    if (f === 'name') x.name = t.value.slice(0, 40);
    else if (f === 'cue') x.cue = t.value.slice(0, 80);
    else if (f === 'm') x.m = t.value;
    else {
      const v = Math.round(+t.value);
      if (v > 0) x[f] = Math.min(f === 'sets' ? 20 : 100, v)
    }
    saveP()
  });
  document.addEventListener('change', e => {
    const t = e.target;
    if (t.dataset && t.dataset.f24 === 'm' && st.sheet && st.sheet.type === 'pex24') {
      const dd = st.plan.days[st.sheet.day], x = dd.groups[st.sheet.gi][st.sheet.ei];
      x.m = t.value;
      saveP()
    }
  });
  document.addEventListener('keydown', e => {
    const t = e.target;
    if ((e.key === 'Enter' || e.key === ' ') && t.dataset && t.dataset.pex24) {
      e.preventDefault();
      open24(t.dataset.pex24)
    }
  });
  document.addEventListener('pointerdown', e => {
    const inn = e.target.closest && e.target.closest('.r24in');
    if (!inn || e.button > 0) return;
    const key = inn.dataset.pex24, drag = !!e.target.closest('[data-drag24]');
    g = {
      key, inn, row: inn.parentNode, x: e.clientX, y: e.clientY, dx: 0, dy: 0, mode: drag ? 'drag' : 'wait', id: e.pointerId
    };
    if (drag) {
      e.preventDefault();
      startDrag24(g, e)
    }
  }, {passive: false});
  document.addEventListener('pointermove', e => {
    if (!g || e.pointerId !== g.id) return;
    g.dx = e.clientX - g.x;
    g.dy = e.clientY - g.y;
    if (g.mode === 'wait') {
      if (Math.abs(g.dx) > 10 && Math.abs(g.dx) > Math.abs(g.dy) * 1.3 && g.dx < 0) {
        g.mode = 'swipe';
        g.row.classList.add('sw24')
      } else if (Math.abs(g.dy) > 10) {g.mode = 'scroll'}
    }
    if (g.mode === 'swipe') {
      e.preventDefault();
      const x = Math.max(-160, Math.min(0, g.dx));
      g.inn.style.transform = `translateX(${x}px)`;
      g.row.classList.toggle('arm24', x < -90)
    }
    if (g.mode === 'drag') {
      e.preventDefault();
      moveDrag24(g, e)
    }
  }, {passive: false});
  const end = e => {
    if (!g || e.pointerId !== g.id) return;
    const c = g;
    g = null;
    if (c.mode === 'wait' && e.type === 'pointerup' && Math.abs(c.dx) < 8 && Math.abs(c.dy) < 8) {
      open24(c.key);
      return
    }
    if (c.mode === 'swipe') {
      c.row.classList.remove('sw24', 'arm24');
      if (c.dx < -90 && e.type === 'pointerup') {
        c.inn.style.transform = 'translateX(-110%)';
        const [gi, ei] = c.key.split('-').map(Number);
        setTimeout(() => del24(gi, ei), 160)
      } else c.inn.style.transform = ''
    }
    if (c.mode === 'drag') endDrag24(c, e.type === 'pointerup')
  };
  document.addEventListener('pointerup', end);
  document.addEventListener('pointercancel', end)
}
function startDrag24(c, e) {
  const list = $('l24');
  if (!list) return;
  c.list = list;
  c.inn.setPointerCapture && c.inn.setPointerCapture(e.pointerId);
  c.groups = [...list.querySelectorAll('.g24')].map(el => ({gi: +el.dataset.g24, r: el.getBoundingClientRect()}));
  c.row.classList.add('drag');
  document.body.classList.add('drag24');
  c.line = document.createElement('div');
  c.line.className = 'dl24';
  list.appendChild(c.line);
  c.k = null;
  moveDrag24(c, e);
  try {haptic('tap')} catch (x) {}
}
function moveDrag24(c, e) {
  c.row.style.transform = `translateY(${c.dy}px)`;
  const y = e.clientY, lr = c.list.getBoundingClientRect();
  let k = c.groups.length, top;
  for (let i = 0; i < c.groups.length; i++) {
    const r = c.groups[i].r;
    if (y < r.top + r.height / 2) {
      k = i;
      break
    }
  }
  top = k < c.groups.length ? c.groups[k].r.top - 5 : c.groups[c.groups.length - 1].r.bottom + 3;
  c.k = k;
  c.line.style.top = (top - lr.top) + 'px';
  if (y < 70) window.scrollBy(0, -8);
  else if (y > window.innerHeight - 140) window.scrollBy(0, 8)
}
function endDrag24(c, ok) {
  document.body.classList.remove('drag24');
  if (c.line) c.line.remove();
  c.row.classList.remove('drag');
  c.row.style.transform = '';
  if (!ok || c.k == null) return;
  const [gi, ei] = c.key.split('-').map(Number), G = day24().groups, single = G[gi].length === 1;
  if (single && (c.k === gi || c.k === gi + 1)) return;
  move24(gi, ei, c.k)
}
