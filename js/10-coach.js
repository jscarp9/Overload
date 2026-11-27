/* ========================================================================
   Overload: Coach (Overload AI)

   Plan builder, coach chat, form tips, recaps.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

/* ---------- 0.23: Overload AI ---------- */
const AIK = 'overload-ai';
function aiStore() {try {return JSON.parse(localStorage.getItem(AIK)) || {}} catch (e) {return {}}}
function aiSave(o) {try {localStorage.setItem(AIK, JSON.stringify(o))} catch (e) {}}
let AI23 = aiStore();
AI23.recaps = AI23.recaps || {};
AI23.weekly = AI23.weekly || {};
const aiOnNow = () => !!AI23.on;
const aiReady = () => aiOnNow() && !!st.set.acct && !!st.set.aiOk && st.set.aiCoach !== false;
async function aiStatus() {
  try {
    const r = await fetch(PUSH_URL + '/aistatus', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: '{}'});
    const j = await r.json();
    const was = AI23.on;
    AI23.on = !!j.on;
    aiSave(AI23);
    if (was !== AI23.on) render()
  } catch (e) {}
}
const ORB23 = '<span class="orb23"><i>✦</i></span>';
function stars23(n, seed) {
  let s = seed || 7, h = '';
  const r = () => (s = (s * 16807) % 2147483647) / 2147483647;
  for (let i = 0; i < n; i++) {
    const x = (r() * 100).toFixed(1), y = (r() * 100).toFixed(1), z = r() < .1 ? 1.6 : r() * .9 + .3;
    h += `<circle cx="${x}%" cy="${y}%" r="${z.toFixed(2)}" fill="${r()<.3?'#f7d97c':'#fff'}" opacity="${(r()*.6+.25).toFixed(2)}"/>`
  }
  return `<svg class="stars23" aria-hidden="true">${h}</svg>`
}
/* photo shrink: keeps every build cheap */
function shrink23(file) {
  return new Promise((res, rej) => {
    const url = URL.createObjectURL(file), im = new Image();
    im.onload = () => {
      const M = 1400, k = Math.min(1, M / Math.max(im.width, im.height)), c = document.createElement('canvas');
      c.width = Math.round(im.width * k);
      c.height = Math.round(im.height * k);
      c.getContext('2d').drawImage(im, 0, 0, c.width, c.height);
      let q = .75, d = c.toDataURL('image/jpeg', q);
      while (d.length > 420000 && q > .35) {
        q -= .1;
        d = c.toDataURL('image/jpeg', q)
      }
      URL.revokeObjectURL(url);
      res({t: 'image/jpeg', d: d.split(',')[1], url: d})
    };
    im.onerror = () => {
      URL.revokeObjectURL(url);
      rej(new Error('That photo could not be read'))
    };
    im.src = url
  })
}
function histFor23() {
  const cut = iso(new Date(toD(today()).getTime() - 70 * 864e5)), m = {};
  for (const [, s] of sorted()) {
    if (!s || s.date < cut || !s.ex) continue;
    for (const [id, a] of Object.entries(s.ex)) {
      const v = (a || []).filter(full);
      if (!v.length) continue;
      const b = v.reduce((x, y) => (+y.w > +x.w || (+y.w === +x.w && +y.r > +x.r)) ? y : x, v[0]);
      const o = m[id] || (m[id] = {n: exName(id), id, m: exMuscle(id), w: 0, r: 0, c: 0});
      o.c++;
      if (+b.w > o.w || (+b.w === o.w && +b.r > o.r)) {
        o.w = +b.w;
        o.r = +b.r
      }
    }
  }
  return Object.values(m).sort((a, b) => b.c - a.c).slice(0, 80)
}
function known23() {
  const m = {};
  (st.plan ? st.plan.days : []).forEach(d => (d.groups || []).flat().forEach(e => {if (e && e.id) m[e.id] = {id: e.id, name: e.name, m: e.m}}));
  histFor23().forEach(h => {if (!m[h.id]) m[h.id] = {id: h.id, name: h.n, m: h.m}});
  return Object.values(m).slice(0, 150)
}
function toPlan23(p) {
  const old = st.plan ? st.plan.days : [], taken = {};
  return p.days.map((d, i) => {
    const seen = {}, o = old[i];
    const day = {
      name: d.name, type: d.type, focus: d.focus || '', rest: d.rest || 90, groups: d.groups.map(g => g.map(e => {
        let id = e.id || slug(e.name);
        if (seen[id]) {
          let n = 2;
          while (seen[id + '-' + n]) n++;
          id = id + '-' + n
        }
        seen[id] = 1;
        taken[id] = 1;
        const x = {id, name: e.name, m: e.m, lo: e.lo, hi: e.hi, sets: e.sets, cue: e.cue || ''};
        if (e.w > 0 && !last(id)) x.start = {w: e.w, r: e.r || e.lo};
        if (e.isNew) x.ai = 'new';
        return x
      }))
    };
    if (o && o.color && o.type !== 'off' && day.type !== 'off') day.color = o.color;
    return day
  })
}
{
  const _sg = suggest;
  suggest = function(e) {
    const r = _sg(e);
    if (r || !e || !e.start) return r;
    return {w: +e.start.w, r: +e.lo || +e.start.r}
  }
}
/* the builder sheet */
function openAi23() {
  if (!aiOnNow()) {
    toast('Overload AI is off right now');
    return
  }
  st.sheet = {
    type: 'ai23', stage: 'form', src: 'photo', imgs: [], text: '', ask: '', days: st.plan ? Math.max(2, Math.min(6, st.plan.days.filter(d => d.type !== 'off').length)) || 4 : 4, goal: 'both', mins: 60
  };
  renderSheet()
}
function seg23(key, opts, val) {
  return `<div class="seg23">${opts.map(([v,l])=>`<button data-ai23set="${key}:${v}" aria-pressed="${String(val)===String(v)}">${l}</button>`).join('')}</div>`
}
function aiSheet23(sh) {
  const left = AI23.left != null ? AI23.left : 3;
  let h = '';
  if (sh.stage === 'busy') {
    const steps = sh.src === 'hist' ? ['Reading your history', 'Matching your lifts', 'Balancing muscles across the week', 'Setting sets, reps and rest'] : sh.src === 'text' ? ['Reading your list', 'Matching your lifts', 'Balancing muscles across the week', 'Setting sets, reps and rest'] : [`Reading your ${sh.imgs.length>1?sh.imgs.length+' photos':'photo'}`, 'Matching your lifts', 'Balancing muscles across the week', 'Setting sets, reps and rest'], k = sh.step || 0;
    return `<div class="ai23 busy">${stars23(90,11)}<div class="ai23c"><h1 class="gt23">Forging your week</h1><div class="sub">About 10 to 20 seconds</div>
  <div class="core23"><i class="o1"></i><i class="o2"></i><svg class="arc23" viewBox="0 0 100 100"><circle cx="50" cy="50" r="46" fill="none" stroke="rgba(255,255,255,.08)" stroke-width="3"/><circle class="arcp" cx="50" cy="50" r="46" fill="none" stroke="url(#ga23)" stroke-width="3.5" stroke-linecap="round" stroke-dasharray="289" stroke-dashoffset="${289-Math.min(.92,.18+k*.24)*289}"/><defs><linearGradient id="ga23"><stop offset="0" stop-color="#8a6212"/><stop offset="1" stop-color="#fff1b8"/></linearGradient></defs></svg><div class="pl23">${plateSVG('gold','idle')}</div>
  <div class="orbit23">${['Push','Pull','Legs','Arms'].map((n,i)=>`<span style="--i:${i}">${n}</span>`).join('')}</div></div>
  <div class="steps23">${steps.map((t,i)=>`<div class="${i<k?'ok':i===k?'w':'p'}"><i>${i<k?'✓':''}</i>${t}</div>`).join('')}</div></div></div>`
  }
  if (sh.stage === 'preview') {
    const p = sh.res, days = toPlan23(p);
    const ex = days.reduce((a, d) => a + d.groups.flat().length, 0), on = days.filter(d => d.type !== 'off').length;
    return `<div class="ai23">${stars23(90,23)}<div class="ai23h">${ORB23}<div><h1 class="gt23">Your week is ready</h1><div class="sub">${on} training days · ${ex} exercises</div></div><button class="x23" id="sheetx" aria-label="Close">✕</button></div>
  <div class="wk23">${days.map((d,i)=>`<div class="${d.type==='off'?'off':''}">${plateSVG(colorOf(d),'done')}<span>${DN[i].toUpperCase()}</span></div>`).join('')}</div>
  ${p.summary?`<div class="ins23">✦ ${esc(p.summary)}</div>`:''}${(p.warnings||[]).map(w=>`<div class="warn23">⚠️ ${esc(w)}</div>`).join('')}
  ${days.map((d,i)=>d.type==='off'?`<div class="dc23 off"><b>${DN[i]} · Rest</b></div>`:`<div class="dc23" style="--c:${PC[colorOf(d)][0]}"><div class="dch"><b>${esc(d.name)}</b><em>${DN[i].toUpperCase()} · ${d.groups.flat().length} lifts</em></div>${d.focus?`<div class="sub dcf">${esc(d.focus)}</div>`:''}<div class="pills23">${d.groups.map((g,gi)=>g.length>1?`<span class="ss">${g.map((e,ei)=>`${String.fromCharCode(65+gi)}${ei+1} ${esc(e.name)}`).join(' / ')} · ${g[0].sets}×${g[0].lo}${g[0].hi>g[0].lo?'–'+g[0].hi:''}</span>`:`<span class="${g[0].ai?'new':''}">${g[0].ai?'+ ':''}${esc(g[0].name)} ${g[0].sets}×${g[0].lo}${g[0].hi>g[0].lo?'–'+g[0].hi:''}${g[0].start?` · ${g[0].start.w}`:''}</span>`).join('')}</div></div>`).join('')}
  <div class="bt23"><button class="cta23" id="ai23use">Use this plan</button><button class="gh23" id="ai23again"${left?'':' disabled'}>↻ Try again</button></div>
  <div class="foot23">Your current plan goes to Trash first · <b>${left} build${left===1?'':'s'} left today</b></div></div>`
  }
  if (sh.stage === 'consent') return `<div class="ai23">${stars23(70,5)}<div class="ai23h">${ORB23}<div><h1 class="gt23">Before we start</h1><div class="sub">One time only</div></div><button class="x23" id="sheetx" aria-label="Close">✕</button></div>
  <div class="cons23"><p><b>Overload AI uses Anthropic’s Claude</b> to build plans and write short coach notes.</p><ul><li><b>Plan builder:</b> the photos or text you add, your lift names and recent best sets.</li><li><b>Coach notes:</b> the sets, weights and reps from a finished workout or week.</li></ul><p>Your name, crew, chats and account are never sent. You can turn AI off any time in Settings → Overload AI.</p></div>
  <button class="cta23" id="ai23allow">Allow and continue</button><button class="gh23 w" id="sheetx">Not now</button></div>`;
  if (!st.set.acct) return `<div class="ai23">${stars23(70,5)}<div class="ai23h">${ORB23}<div><h1 class="gt23">Overload AI</h1><div class="sub">Turn anything into your perfect week</div></div><button class="x23" id="sheetx" aria-label="Close">✕</button></div><div class="cons23"><p>Sign in first so your builds are saved and limits stay fair. It takes 10 seconds with Face ID, and it also backs up your log.</p></div><button class="cta23" id="ai23signin">Sign in</button></div>`;
  const src = sh.src;
  h = `<div class="ai23">${stars23(90,7)}<div class="ai23h">${ORB23}<div><h1 class="gt23">Overload AI</h1><div class="sub">Turn anything into your perfect week</div></div><button class="x23" id="sheetx" aria-label="Close">✕</button></div>
 <div class="gl23">START FROM</div><div class="src23">${[['photo','<rect x="3" y="6" width="18" height="14" rx="3"/><circle cx="12" cy="13" r="3.5"/><path d="M8 6l1.5-2h5L16 6"/>','Photo'],['text','<path d="M4 6h16M4 12h16M4 18h10"/>','Type it'],['hist','<path d="M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5M12 7v5l3 2"/>','My history']].map(([k,p,l])=>`<button data-ai23src="${k}" aria-pressed="${src===k}"><svg viewBox="0 0 24 24">${p}</svg>${l}</button>`).join('')}</div>`;
  if (src === 'photo') h += `<div class="ph23">${sh.imgs.map((m,i)=>`<div class="pi23"><img src="${m.url}" alt="Photo ${i+1}"><i class="beam23"></i><button data-ai23rm="${i}" aria-label="Remove photo">✕</button></div>`).join('')}${sh.imgs.length<3?`<label class="pa23"><input type="file" id="ai23file" accept="image/*" multiple hidden><span>＋</span><b>${sh.imgs.length?'Add another':'Add a photo'}</b><i>Notes, a screenshot or a whiteboard</i></label>`:''}</div>`;
  if (src === 'text') h += `<textarea class="ta23" id="ai23text" rows="6" maxlength="4000" placeholder="Push day&#10;Incline press 230x6&#10;Shoulder press 105x5&#10;…">${esc(sh.text)}</textarea>`;
  if (src === 'hist') {
    const n = histFor23().length;
    h += `<div class="hi23">${n?`<b>${n} lifts</b> from your last 10 weeks, with your best sets.`:'No workouts logged yet. Add a photo or type your lifts instead.'}</div>`
  }
  h += `<div class="gl23">WHAT DO YOU WANT? <span>optional</span></div><input class="in23" id="ai23ask" maxlength="300" value="${esc(sh.ask)}" placeholder="e.g. make it a PPL, or upper lower with an arm day">
 <div class="gl23">DAYS A WEEK</div>${seg23('days',[[3,'3'],[4,'4'],[5,'5'],[6,'6']],sh.days)}
 <div class="r23"><div><div class="gl23">GOAL</div>${seg23('goal',[['strength','Strength'],['size','Size'],['both','Both']],sh.goal)}</div></div>
 <div class="gl23">TIME PER WORKOUT</div>${seg23('mins',[[45,'45 min'],[60,'60 min'],[75,'75 min'],[90,'90+']],sh.mins)}
 ${sh.err?`<div class="err23">${esc(sh.err)}</div>`:''}
 <div class="lk23">🔒 <span>Only ${src==='photo'?'your photos':src==='text'?'your list':'your lift history'} and lift names go to <b>Anthropic’s Claude</b>. Nothing else leaves your phone.</span></div>
 <button class="cta23" id="ai23go"${left?'':' disabled'}>✦ Forge my week</button><div class="left23">${[0,1,2].map(i=>`<i class="${i<left?'':'o'}"></i>`).join('')} ${left} build${left===1?'':'s'} left today</div></div>`;
  return h
}
async function aiGo23(sh) {
  if (!st.set.aiOk) {
    sh.stage = 'consent';
    sh.after = 'go';
    renderSheet();
    return
  }
  sh.err = '';
  const body = {src: sh.src, days: sh.days, goal: sh.goal, mins: sh.mins, unit: U(), ask: sh.ask, known: known23()};
  if (sh.src === 'photo') {
    if (!sh.imgs.length) {
      sh.err = 'Add a photo of your workouts first.';
      renderSheet();
      return
    }
    body.images = sh.imgs.map(m => ({t: m.t, d: m.d}))
  }
  if (sh.src === 'text') {
    if (!sh.text.trim()) {
      sh.err = 'Type a few lifts first.';
      renderSheet();
      return
    }
    body.text = sh.text
  }
  if (sh.src === 'hist') {
    body.history = histFor23();
    if (!body.history.length) {
      sh.err = 'No history yet. Try a photo or type your lifts.';
      renderSheet();
      return
    }
  }
  sh.stage = 'busy';
  sh.step = 0;
  renderSheet();
  const tk = setInterval(() => {
    if (st.sheet !== sh || sh.stage !== 'busy') {
      clearInterval(tk);
      return
    }
    if (sh.step < 3) {
      sh.step++;
      renderSheet()
    }
  }, 2600);
  try {
    const r = await vaultCall('/ai/plan', acctBody(body));
    clearInterval(tk);
    AI23.left = r.left;
    aiSave(AI23);
    if (st.sheet !== sh) return;
    sh.res = r.plan;
    sh.stage = 'preview';
    try {haptic('success')} catch (x) {};
    renderSheet()
  } catch (e) {
    clearInterval(tk);
    if (e.status === 429) AI23.left = 0, aiSave(AI23);
    if (e.status === 503) {
      AI23.on = false;
      aiSave(AI23)
    }
    if (st.sheet !== sh) return;
    sh.stage = 'form';
    sh.err = e.message || 'Something went wrong. Try again.';
    renderSheet()
  }
}
function useAi23(sh) {
  const days = toPlan23(sh.res);
  if (st.plan) trashPutSync('plan', 'Your plan before the AI build', clone(st.plan));
  st.plan = {days};
  store('overload-plan', st.plan);
  try {saveP()} catch (e) {}st.sheet = null;
  renderSheet();
  st.tab = 'plan';
  st.profile = false;
  st.settings = false;
  st.pday = dayIdx(today());
  render();
  toast('New plan saved. Your old one is in Trash.');
  try {confetti()} catch (e) {}
}
async function sheetClick23(b) {
  const sh = st.sheet, d = b.dataset;
  if (!sh || sh.type !== 'ai23') return false;
  if (d.ai23src) {
    sh.src = d.ai23src;
    sh.err = '';
    renderSheet();
    return true
  }
  if (d.ai23set) {
    const [k, v] = d.ai23set.split(':');
    sh[k] = k === 'goal' ? v : +v;
    renderSheet();
    return true
  }
  if (d.ai23rm !== undefined) {
    sh.imgs.splice(+d.ai23rm, 1);
    renderSheet();
    return true
  }
  if (b.id === 'ai23go') {
    aiGo23(sh);
    return true
  }
  if (b.id === 'ai23again') {
    sh.stage = 'form';
    aiGo23(sh);
    return true
  }
  if (b.id === 'ai23use') {
    useAi23(sh);
    return true
  }
  if (b.id === 'ai23allow') {
    st.set.aiOk = 1;
    st.set.aiCoach = true;
    saveSet();
    if (sh.after === 'go') {
      sh.stage = 'form';
      aiGo23(sh)
    } else {
      st.sheet = null;
      renderSheet();
      render()
    }
    return true
  }
  if (b.id === 'ai23signin') {
    st.sheet = null;
    renderSheet();
    st.settings = true;
    st.set17 = 'sync';
    st.profile = false;
    render();
    return true
  }
  return false
}async function click23(b) {
  if (b.dataset.tog === 'aiCoach23') {
    const on = !(st.set.aiOk && st.set.aiCoach !== false);
    if (on) {
      st.set.aiOk = 1;
      st.set.aiCoach = true
    } else st.set.aiCoach = false;
    saveSet();
    render();
    return true
  }
  if (b.id === 'ai23open' || b.id === 'ai23open2') {
    st.pd21m = false;
    if (!st.set.aiSeen23) {
      st.set.aiSeen23 = 1;
      saveSet()
    }
    openAi23();
    return true
  }
  if (b.id === 'ai23coachoff') {
    st.set.aiCoach = false;
    saveSet();
    render();
    toast('Coach notes off');
    return true
  }
  return false
}
/* coach recap after a workout: once per workout, never on demand */
function holo23(title, small, body, extra) {
  return `<div class="holo23">${ORB23}<div class="hb23"><div class="hh23"><b class="gt23">${title}</b><small>${small}</small></div><p>${body}</p>${extra||''}</div></div>`
}
function recapBody23(s, key) {
  const ex = Object.entries(s.ex || {}).map(([id, a]) => {
    const v = (a || []).filter(full);
    if (!v.length) return null;
    const l = last(id), p = l ? (l.ex[id] || []).filter(full) : [];
    return {n: exName(id), s: v.map(x => [+x.w, +x.r]), p: p.map(x => [+x.w, +x.r])}
  }).filter(Boolean), ss = sessStats(s);
  return acctBody({
    sid: key, name: (pd() && pd().name) || 'Workout', mins: Math.round(elapsed(s) / 60000), sets: ss.sets, vol: Math.round(ss.vol), unit: U(), prs: (ss.prs || []).map(x => exName(x)).slice(0, 8), ex
  })
}
let rc23 = {};
{
  const _sm = summaryHTML;
  summaryHTML = function() {
    const h = _sm();
    try {
      if (!h || !aiReady()) return h;
      const s = cur(), key = sid();
      if (!s || !s.end) return h;
      const txt = AI23.recaps[key];
      if (txt) return h + holo23('COACH RECAP', 'AI · once per workout', esc(txt));
      if (Date.now() - s.end > 6 * 3600e3 || rc23[key] === 'x') return h;
      if (!rc23[key]) {
        rc23[key] = 1;
        vaultCall('/ai/recap', recapBody23(s, key)).then(r => {
          AI23.recaps[key] = r.text;
          const ks = Object.keys(AI23.recaps);
          while (ks.length > 40) delete AI23.recaps[ks.shift()];
          aiSave(AI23);
          render()
        }).catch (() => {
          rc23[key] = 'x';
          render()
        })
      }
      return h + `<div class="holo23 ld">${ORB23}<div class="hb23"><div class="hh23"><b class="gt23">COACH RECAP</b><small>writing…</small></div><p><i class="sh23"></i><i class="sh23 s"></i></p></div></div>`
    } catch (e) {return h}
  }
}
/* weekly coach note in the Monday report */
function weekBody23(sh) {
  const wd = sh.pw, inWk = ([, s]) => s && wd.includes(s.date) && real(s), ses = sorted().filter(inWk), mus = {}, ids = {};
  ses.forEach(([, s]) => {
    Object.entries(muscleSets(s)).forEach(([k, v]) => mus[k] = (mus[k] || 0) + v);
    Object.keys(s.ex || {}).forEach(id => ids[id] = (ids[id] || 0) + 1)
  });
  const wkOf = d => weekDates(d)[0], lifts = Object.keys(ids).sort((a, b) => ids[b] - ids[a]).slice(0, 10).map(id => {
    const pts = points(id), wk = {};
    pts.forEach(p => {
      const w = wkOf(p.s.date);
      if (w <= wd[0]) wk[w] = Math.max(wk[w] || 0, p.e)
    });
    const ks = Object.keys(wk).sort().slice(-6);
    return {n: exName(id), e: Math.round(wk[wd[0]] || 0), h: ks.map(k => Math.round(wk[k]))}
  });
  return acctBody({
    wk: wd[0], n: sh.a.done, planned: sh.a.planned, sets: sh.a.sets, vol: Math.round(sh.a.vol), unit: U(), prs: sh.a.prs ? [sh.a.prs + ' PRs'] : [], lifts, mus
  })
}
let wk23 = {};
function patch23() {
  const sh = st.sheet;
  if (!sh || sh.type !== 'report' || !aiReady()) return;
  const box = document.querySelector('#sheet .sheet');
  if (!box || box.querySelector('.holo23')) return;
  const key = sh.pw && sh.pw[0];
  if (!key) return;
  const txt = AI23.weekly[key], el = document.createElement('div');
  el.className = 'coachwrap23';
  if (txt) {el.innerHTML = holo23('COACH NOTE', 'AI · once a week', esc(txt))}
  else if (wk23[key] !== 'x' && sh.a && sh.a.done) {
    el.innerHTML = `<div class="holo23 ld">${ORB23}<div class="hb23"><div class="hh23"><b class="gt23">COACH NOTE</b><small>writing…</small></div><p><i class="sh23"></i><i class="sh23 s"></i></p></div></div>`;
    if (!wk23[key]) {
      wk23[key] = 1;
      vaultCall('/ai/weekly', weekBody23(sh)).then(r => {
        AI23.weekly[key] = r.text;
        aiSave(AI23);
        if (st.sheet === sh) renderSheet()
      }).catch (() => {
        wk23[key] = 'x';
        if (st.sheet === sh) renderSheet()
      })
    }
  } else return;
  const btn = box.querySelector('.btn.acc.wide,.btn.acc.wi');
  if (btn) btn.parentNode.insertBefore(el, btn);
  else box.appendChild(el)
}function boot23() {
  aiStatus();
  setInterval(aiStatus, 30 * 60e3);
  document.addEventListener('change', async e => {
    const t = e.target;
    if (t.id !== 'ai23file' || !st.sheet || st.sheet.type !== 'ai23') return;
    const sh = st.sheet, fs = [...t.files].slice(0, 3 - sh.imgs.length);
    sh.err = '';
    for (const f of fs) {try {sh.imgs.push(await shrink23(f))} catch (x) {sh.err = x.message}}renderSheet()
  });
  document.addEventListener('input', e => {
    const t = e.target, sh = st.sheet;
    if (!sh || sh.type !== 'ai23') return;
    if (t.id === 'ai23text') sh.text = t.value;
    if (t.id === 'ai23ask') sh.ask = t.value
  })
}/* ---------- 0.25: Coach chat, form tips, crew AI recap, budget aware ---------- */
AI23.chat = AI23.chat || [];
AI23.cues = AI23.cues || {};
{
  const _st = aiStatus;
  aiStatus = async function() {
    try {
      const r = await fetch(PUSH_URL + '/aistatus', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: '{}'});
      const j = await r.json();
      const was = AI23.on + '|' + AI23.rest;
      AI23.on = !!j.on;
      AI23.rest = !!j.resting;
      aiSave(AI23);
      if (was !== AI23.on + '|' + AI23.rest) render()
    } catch (e) {}
  }
}
const CHIPS25 = [['🔄', 'Swap an exercise', 'Swap an exercise on today’s workout for something else'], ['⏱', 'I only have 30 min', 'I only have 30 minutes today. Shorten today’s workout to fit.'], ['😮‍💨', 'Deload week', 'I feel beat up. Make my plan a deload week.'], ['📈', 'Why is my bench stuck?', 'Why is my bench press stuck and what should I change?'], ['🗓', 'Change my split', 'I want to change my split. Ask me what I want.']];
function planForAI25() {
  return (st.plan ? st.plan.days : []).map(d => ({
    name: d.name, type: d.type, focus: d.focus, rest: d.rest, groups: (d.groups || []).map(g => g.map(e => ({id: e.id, name: e.name, m: e.m, sets: e.sets, lo: e.lo, hi: e.hi})))
  }))
}
function recentForAI25() {
  return sorted().filter(([, s]) => s && s.ex && real(s)).slice(0, 6).map(([, s]) => ({
    date: s.date, day: (s.name || (st.plan && st.plan.days[dayIdx(s.date)] || {}).name || ''), ex: Object.entries(s.ex).map(([id, a]) => ({n: exName(id), s: (a || []).filter(full).map(x => [+x.w, +x.r])})).filter(e => e.s.length).slice(0, 12)
  }))
}
function dayFromAI25(d, old) {
  const seen = {};
  const day = {
    name: d.name, type: d.type, focus: d.focus || '', rest: d.rest || 90, groups: (d.groups || []).map(g => g.map(e => {
      let id = e.id || slug(e.name);
      if (seen[id]) {
        let n = 2;
        while (seen[id + '-' + n]) n++;
        id = id + '-' + n
      }
      seen[id] = 1;
      const x = {id, name: e.name, m: e.m, lo: e.lo, hi: e.hi, sets: e.sets, cue: e.cue || ''};
      const prev = old && (old.groups || []).flat().find(p => p.id === id);
      if (prev && prev.cue && !x.cue) x.cue = prev.cue;
      if (prev && prev.start) x.start = prev.start;
      if (e.w > 0 && !last(id) && !x.start) x.start = {w: e.w, r: e.r || e.lo};
      return x
    }))
  };
  if (old && old.color && old.type !== 'off' && day.type !== 'off') day.color = old.color;
  return day
}
function diff25(d) {
  const old = st.plan && st.plan.days[d.day], a = (old && old.groups || []).flat().map(e => e.name), b = (d.groups || []).flat().map(e => e.name), add = b.filter(x => !a.includes(x)), rem = a.filter(x => !b.includes(x));
  const bits = [];
  if (old && old.name !== d.name) bits.push(`renamed to <b>${esc(d.name)}</b>`);
  add.forEach(x => bits.push(`<i class="p">+ ${esc(x)}</i>`));
  rem.forEach(x => bits.push(`<i class="m">− ${esc(x)}</i>`));
  if (!bits.length) bits.push('sets, reps or order changed');
  return bits.join(' ')
}
function coach25(sh) {
  const left = AI23.chatLeft != null ? AI23.chatLeft : 25;
  let h = `<div class="ai23 co25">${stars23(80,31)}<div class="ai23h">${ORB23}<div><h1 class="gt23">Coach</h1><div class="sub">Ask anything, or tell me what to change</div></div><button class="x23" id="sheetx" aria-label="Close">✕</button></div>`;
  if (!st.set.acct) return h + `<div class="cons23"><p>Sign in first so Coach knows it’s you and limits stay fair. It takes 10 seconds with Face ID.</p></div><button class="cta23" id="ai23signin">Sign in</button></div>`;
  if (!st.set.aiOk) return h + `<div class="cons23"><p><b>Coach uses Anthropic’s Claude.</b> When you send a message it gets your message, your plan and your last few workouts (exercises, weights and reps).</p><p>Your name, crew, chats and account are never sent. Nothing in your plan changes unless you tap Apply.</p></div><button class="cta23" id="co25allow">Allow and start</button><button class="gh23 w" id="sheetx">Not now</button></div>`;
  if (AI23.rest) h += `<div class="warn23">Overload AI used up this month’s budget. Coach is back on the 1st.</div>`;
  const msgs = AI23.chat.slice(-40);
  h += `<div class="msgs25" id="msgs25">`;
  if (!msgs.length) h += `<div class="hello25"><p>Hey${st.set.name?' '+esc(String(st.set.name).split(' ')[0]):''}. I can see your plan and your recent workouts. Try one of these:</p><div class="chips25">${CHIPS25.map(([i,l,q])=>`<button data-co25q="${esc(q)}">${i} ${l}</button>`).join('')}<button id="ai23build">✦ Build a whole new week</button></div></div>`;
  msgs.forEach((m, i) => {
    if (m.r === 'u') h += `<div class="mu25">${esc(m.t)}</div>`;
    else {
      h += `<div class="ma25">${ORB23}<div><p>${esc(m.t)}</p>${m.ch?`<div class="ch25${m.st?' '+m.st:''}"><div class="ch25h">✦ ${esc(m.ch.summary||'Plan change')}</div>${m.ch.days.map(d=>`<div class="ch25d"><b>${DN[d.day]} · ${esc(d.name)}</b><span>${d._diff||diff25(d)}</span></div>`).join('')}${m.st==='applied'?'<div class="ch25s">✓ Applied. Your old plan is in Trash.</div>':m.st==='no'?'<div class="ch25s">Not applied</div>':`<div class="ch25b"><button class="cta23" data-co25apply="${i}">Apply</button><button class="gh23" data-co25no="${i}">No thanks</button></div>`}</div>`:''}</div></div>`
    }
  });
  if (sh.busy) h += `<div class="ma25 typing">${ORB23}<div><p><i></i><i></i><i></i></p></div></div>`;
  if (sh.err) h += `<div class="err23">${esc(sh.err)}</div>`;
  h += `</div><div class="in25"><textarea id="co25in" rows="1" maxlength="500" placeholder="${AI23.rest?'Coach is resting until the 1st':'Message Coach'}"${AI23.rest||!left?' disabled':''}>${esc(sh.draft||'')}</textarea><button id="co25send" aria-label="Send"${sh.busy||AI23.rest||!left?' disabled':''}>↑</button></div>
 <div class="left23">${left} message${left===1?'':'s'} left today${msgs.length?` · <button class="lnk25" id="co25clear">Clear chat</button>`:''}</div></div>`;
  return h
}
async function send25(sh, text) {
  text = String(text || '').trim().slice(0, 500);
  if (!text || sh.busy) return;
  AI23.chat.push({r: 'u', t: text});
  sh.draft = '';
  sh.busy = true;
  sh.err = '';
  aiSave(AI23);
  renderSheet();
  scroll25();
  const hist = AI23.chat.slice(-10).map(m => ({r: m.r, t: m.t + (m.ch && m.st ? ` [plan change ${m.st==='applied'?'applied':'not applied'}]` : '')}));
  try {
    const r = await vaultCall('/ai/chat', acctBody({
      msgs: hist, plan: planForAI25(), recent: recentForAI25(), today: dayIdx(today()), unit: U(), known: known23()
    }));
    AI23.chatLeft = r.left;
    if (r.changes) r.changes.days.forEach(d => d._diff = diff25(d));
    AI23.chat.push({r: 'a', t: r.text, ch: r.changes || null});
    try {haptic('tap')} catch (e) {}
  } catch (e) {
    if (e.status === 429) AI23.chatLeft = 0;
    if (e.status === 503 && /budget/i.test(e.message)) AI23.rest = true;
    sh.err = e.message || 'Coach could not answer. Try again.';
    AI23.chat.pop();
    sh.draft = text
  }
  while (AI23.chat.length > 60) AI23.chat.shift();
  sh.busy = false;
  aiSave(AI23);
  if (st.sheet === sh) {
    renderSheet();
    scroll25()
  }
}
function scroll25() {
  setTimeout(() => {
    const m = $('msgs25');
    if (m) m.scrollIntoView({block: 'end'});
    const s = document.querySelector('#sheet .sheet');
    if (s) s.scrollTop = s.scrollHeight
  }, 30)
}
function apply25(i) {
  const m = AI23.chat[i];
  if (!m || !m.ch || m.st) return;
  if (st.plan) trashPutSync('plan', 'Your plan before a Coach change', clone(st.plan));
  m.ch.days.forEach(d => {st.plan.days[d.day] = dayFromAI25(d, st.plan.days[d.day])});
  store('overload-plan', st.plan);
  try {saveP()} catch (e) {}m.st = 'applied';
  aiSave(AI23);
  renderSheet();
  render();
  toast('Plan updated. Old one is in Trash.')
}
function openCoach25() {
  if (!aiOnNow()) {
    toast('Overload AI is off right now');
    return
  }
  st.sheet = {type: 'coach25', draft: ''};
  renderSheet();
  scroll25()
}
/* form tips */
function cueKey25(n) {return String(n || '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim()}
function cueSheet25(sh) {
  const t = AI23.cues[cueKey25(sh.name)];
  let h = `<div class="ai23">${stars23(50,41)}<div class="ai23h">${ORB23}<div><h1 class="gt23">${esc(sh.name)}</h1><div class="sub">Form tips</div></div><button class="x23" id="sheetx" aria-label="Close">✕</button></div>`;
  if (!st.set.acct) return h + `<div class="cons23"><p>Sign in to get AI form tips. It takes 10 seconds with Face ID.</p></div><button class="cta23" id="ai23signin">Sign in</button></div>`;
  if (!st.set.aiOk) return h + `<div class="cons23"><p>Form tips are written by Anthropic’s Claude. Only the exercise name is sent.</p></div><button class="cta23" id="cue25allow">Allow</button></div>`;
  if (t) return h + `<div class="tips25"><ol>${t.cues.map(c=>`<li>${esc(c)}</li>`).join('')}</ol>${t.feel?`<div class="tp25"><b>Feel it in</b>${esc(t.feel)}</div>`:''}${t.mistake?`<div class="tp25 x"><b>Avoid</b>${esc(t.mistake)}</div>`:''}<p class="sub" style="margin-top:12px;font-size:11.5px">AI tips, not a substitute for a coach watching you. Stop if anything hurts.</p></div></div>`;
  if (sh.err) return h + `<div class="err23">${esc(sh.err)}</div></div>`;
  return h + `<div class="tips25 ld"><i class="sh23"></i><i class="sh23"></i><i class="sh23 s"></i></div></div>`
}
async function openCue25(name) {
  st.sheet = {type: 'cue25', name, back: st.sheet && st.sheet.type === 'pex24' ? st.sheet : null};
  renderSheet();
  fetchCue25(st.sheet)
}
async function fetchCue25(sh) {
  const k = cueKey25(sh.name);
  if (AI23.cues[k] || !st.set.acct || !st.set.aiOk) return;
  try {
    const r = await vaultCall('/ai/cue', acctBody({name: sh.name}));
    AI23.cues[k] = r.tips;
    const ks = Object.keys(AI23.cues);
    while (ks.length > 150) delete AI23.cues[ks.shift()];
    aiSave(AI23)
  } catch (e) {sh.err = e.message || 'Could not load tips'}if (st.sheet === sh) renderSheet()
}async function sheetClick25(b) {
  const sh = st.sheet, d = b.dataset;
  if (!sh) return false;
  if (sh.type === 'pex24' && b.id === 'ed24tips') {
    const dd = st.plan.days[sh.day], e = dd.groups[sh.gi][sh.ei];
    openCue25(e.name);
    return true
  }
  if (sh.type === 'tmore' && b.id === 't25coach') {
    openCoach25();
    return true
  }
  if (sh.type === 'cue25') {
    if (b.id === 'cue25allow') {
      st.set.aiOk = 1;
      if (st.set.aiCoach === undefined) st.set.aiCoach = true;
      saveSet();
      renderSheet();
      fetchCue25(sh);
      return true
    }
    if (b.id === 'ai23signin') {
      st.sheet = null;
      renderSheet();
      st.settings = true;
      st.set17 = 'sync';
      render();
      return true
    }
    return false
  }
  if (sh.type !== 'coach25') return false;
  if (b.id === 'co25allow') {
    st.set.aiOk = 1;
    if (st.set.aiCoach === undefined) st.set.aiCoach = true;
    saveSet();
    renderSheet();
    return true
  }
  if (b.id === 'ai23signin') {
    st.sheet = null;
    renderSheet();
    st.settings = true;
    st.set17 = 'sync';
    st.profile = false;
    render();
    return true
  }
  if (b.id === 'ai23build') {
    openAi23();
    return true
  }
  if (d.co25q) {
    send25(sh, d.co25q);
    return true
  }
  if (b.id === 'co25send') {
    const i = $('co25in');
    send25(sh, i ? i.value : sh.draft);
    return true
  }
  if (d.co25apply !== undefined) {
    apply25(+d.co25apply);
    return true
  }
  if (d.co25no !== undefined) {
    const m = AI23.chat[+d.co25no];
    if (m) {
      m.st = 'no';
      aiSave(AI23);
      renderSheet()
    }
    return true
  }
  if (b.id === 'co25clear') {
    AI23.chat = [];
    aiSave(AI23);
    renderSheet();
    return true
  }
  return false
}
async function click25(b) {
  const d = b.dataset;
  if (b.id === 'co25open') {
    openCoach25();
    return true
  }
  if (d.cue25) {
    st.menu = null;
    render();
    openCue25(d.cue25);
    return true
  }
  return false
}
/* entry points */
{
  const _rp = renderPlan;
  renderPlan = function() {
    let h = _rp();
    if (!aiOnNow()) return h;
    return h.replace('<button id="ai23open" class="ai23mi">✦ Build my plan with AI</button>', '<button id="co25open" class="ai23mi">✦ Ask Coach</button><button id="ai23open" class="ai23mi">✦ Build a whole new week with AI</button>').replace('<button class="ai23chip" id="ai23open" aria-label="Build my plan with AI"><span>✦</span>AI</button>', '<button class="ai23chip" id="co25open" aria-label="Ask Coach"><span>✦</span>Coach</button>')
  }
}
{
  const _eb = exBlock;
  exBlock = function(e) {
    const h = _eb.apply(this, arguments);
    if (!aiOnNow()) return h;
    return h.replace('<button data-act="nhist"', `<button data-cue25="${esc(e.name)}">✦ Form tips</button><button data-act="nhist"`)
  }
}
/* crew: weekly AI recap post */
{
  const _p = postHTML;
  postHTML = function(p, preview) {
    if (p && p.type === 'ai') return `<div class="post holo23 aip25">${ORB23}<div class="hb23"><div class="hh23"><b class="gt23">CREW RECAP</b><small>Overload AI · ${ago(p.ts)}</small></div><p>${esc(p.cap||'')}</p></div></div>`;
    return _p(p, preview)
  }
}
let hype25 = 0;
function hypeCheck25() {
  try {
    if (!aiOnNow() || AI23.rest || !st.cs || !st.cs.crew || hype25) return;
    const tw = weekDates(today())[0], k = 'overload-hype';
    if (localStorage.getItem(k) === st.cs.crew.code + tw) return;
    hype25 = 1;
    crewCall('aihype', {today: today()}).then(r => {if (r && (r.ok || r.done)) localStorage.setItem(k, st.cs.crew.code + tw)}).catch (() => {}).finally(() => {setTimeout(() => hype25 = 0, 60000)})
  } catch (e) {}
}function boot25() {
  aiStatus();
  setInterval(hypeCheck25, 60000);
  setTimeout(hypeCheck25, 6000);
  document.addEventListener('input', e => {
    const t = e.target;
    if (t.id === 'co25in' && st.sheet && st.sheet.type === 'coach25') {
      st.sheet.draft = t.value;
      t.style.height = 'auto';
      t.style.height = Math.min(120, t.scrollHeight) + 'px'
    }
  });
  document.addEventListener('keydown', e => {
    if (e.target.id === 'co25in' && e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (st.sheet && st.sheet.type === 'coach25') send25(st.sheet, e.target.value)
    }
  })
}
/* ---------- 0.26: Coach lives in the top bar ---------- */
{
  const _rp = renderPlan;
  renderPlan = function() {
    return _rp().replace(/<button class="ai23chip" id="co25open"[^>]*>[\s\S]*?<\/button>/, '')
  }
}
function upd26() {
  try {
    const b = $('coach26');
    if (b) {
      b.classList.toggle('hide', !aiOnNow());
      b.classList.toggle('rest26', !!AI23.rest)
    }
  } catch (e) {}
}
{
  const _rc = renderChrome;
  renderChrome = function() {
    _rc.apply(this, arguments);
    upd26()
  }
}
function boot26() {
  const b = $('coach26');
  if (b) b.addEventListener('click', () => {try {haptic('tap')} catch (e) {}openCoach25()});
  upd26();
  const _st = aiStatus;
  aiStatus = async function() {
    await _st.apply(this, arguments);
    upd26()
  };
  setInterval(upd26, 5000)
}
