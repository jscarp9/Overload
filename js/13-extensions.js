/* ========================================================================
   Overload: Extensions and event wiring

   Later versions extend earlier functions here: each block saves the current version of a
   function and wraps it, so these blocks must stay in this order. After that, the click and
   sheet handlers for each feature and every event listener are registered, also in order.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

setInterval(tick, 1000);
document.addEventListener('visibilitychange', () => {if (!document.hidden) {if (st.date !== today() && st.auto) {setDate(today(), false)}render()}});
$('rplus').addEventListener('click', () => {
  if (st.rest) {
    st.rest.end += 15000;
    st.rest.dur += 15;
    st.rest.buzzed = false;
    $('rest').classList.remove('done');
    tick()
  }
});
$('rminus').addEventListener('click', () => {if (st.rest) {st.rest.end -= 15000; tick()}});
$('rreset').addEventListener('click', () => {
  if (st.rest) {
    unlockAudio();
    startRest(st.rest.base, st.rest.next);
    toast('Rest timer reset')
  }
});
$('rstop').addEventListener('click', () => {
  st.rest = null;
  $('rest').classList.remove('on', 'done')
});
/* ---------- events ---------- */
const main = $('main');
main.addEventListener('input', e => {
  const el = e.target;
  if (el.dataset.ex) {
    const s = ensure(), id = el.dataset.ex, i = +el.dataset.i;
    s.ex[id] = s.ex[id] || [];
    while (s.ex[id].length <= i) s.ex[id].push({});
    s.ex[id][i][el.dataset.k] = el.value === '' ? '' : +el.value;
    s.names[id] = exName(id);
    const l = last(id), m = $('m-' + id + '-' + i);
    if (m) m.textContent = better(s.ex[id][i], l ? (l.ex[id] || [])[i] : null) ? '↑' : '';
    const ex = exById(id);
    if (ex && $('tag-' + id)) $('tag-' + id).innerHTML = tagFor(ex);
    saveS();
    liveHero();
    return
  }
  if (el.id === 'snote') {
    ensure().notes = el.value;
    saveS();
    return
  }
  if (el.dataset.exnote) {
    clearTimeout(window._unT);
    window._unT = setTimeout(checkUnlocks, 1500);
    const s = ensure();
    s.exNotes = s.exNotes || {};
    s.exNotes[el.dataset.exnote] = el.value;
    el.style.height = 'auto';
    el.style.height = el.scrollHeight + 'px';
    saveS();
    return
  }
  if (el.id === 'psearch') {
    $('plist').innerHTML = plistHTML(el.value);
    return
  }
  if (el.id === 'obname') {
    st.obName = el.value;
    return
  }
  if (el.id === 'sname') {
    st.set.name = el.value.trim();
    saveSet();
    return
  }
  const d = st.plan && st.plan.days[st.pday];
  if (el.dataset.pf2) {
    const f = el.dataset.pf2;
    d[f] = f === 'rest' ? +el.value : el.value;
    saveP();
    if (f === 'type') {
      delete d.color;
      render()
    }
    return
  }
  if (el.dataset.ef) {
    const ex = d.groups[+el.dataset.g][+el.dataset.e], f = el.dataset.ef;
    if (f === 'name' || f === 'm' || f === 'cue') ex[f] = el.value;
    else ex[f] = el.value === '' ? '' : Math.max(0, +el.value);
    saveP()
  }
});
function saveRename() {
  const r = $('rename');
  if (!r) return;
  const v = r.value.trim();
  if (v) {
    pd().name = v;
    saveP()
  }
  st.renaming = false;
  render()
}
main.addEventListener('focusout', e => {if (e.target.id === 'rename') setTimeout(saveRename, 0)});
main.addEventListener('keydown', e => {
  if (e.target.id === 'rename' && e.key === 'Enter') {
    e.preventDefault();
    e.target.blur();
    return
  }
  if (e.target.id === 'psearch' && e.key === 'Enter') {
    e.preventDefault();
    const q = e.target.value.trim();
    if (!q) return;
    const hit = LIB.find(x => x[0].toLowerCase() === q.toLowerCase());
    if (hit) addFromPicker(...hit);
    else addFromPicker(q, 'Other', 8, 12)
  }
});
main.addEventListener('click', async e => {
  const b = e.target.closest('button');
  if (!b) return;
  const d = st.plan && st.plan.days[st.pday];
  if (await click28(b)) return;
  if (await click27(b)) return;
  if (await click25(b)) return;
  if (await click24(b)) return;
  if (await click23(b)) return;
  if (await click22(b)) return;
  if (await click21(b)) return;
  if (await click20(b)) return;
  if (await click18(b)) return;
  if (await click17(b)) return;
  if (await click16(b)) return;
  if (await click15(b)) return;
  if (await click14(b)) return;
  if (await crewClick(b)) return;
  if (await acctClick(b)) return;
  if (await click13(b)) return;
  if (b.id === 'openprof') {
    st.profile = true;
    render();
    window.scrollTo({top: 0});
    return
  }
  if (b.id === 'pclose2') {
    st.profile = false;
    render();
    return
  }
  if (b.id === 'shareprof') {
    st.sheet = {type: 'share', title: 'Share your rank'};
    renderSheet();
    profileCard().then(r => {
      if (st.sheet && st.sheet.type === 'share') {
        st.sheet.url = r.url;
        st.sheet.file = r.file;
        renderSheet()
      }
    });
    return
  }
  if (b.dataset.manual) {
    setFlag(b.dataset.manual === 'rwalk' ? 'walk' : b.dataset.manual);
    render();
    return
  }
  if (b.id === 'copyday') {
    st.copySel = [];
    st.sheet = {type: 'copy'};
    renderSheet();
    return
  }
  if (b.dataset.snap) {
    const k = 's' + b.dataset.snap;
    if (st.confirm !== k) {
      st.confirm = k;
      render();
      return
    }
    const sn = await IDB.get('snap:' + b.dataset.snap);
    if (sn && sn.data) {
      try {
        restoreReplace(sn.data);
        st.confirm = null;
        toast('Restored your data from ' + b.dataset.snap);
        render()
      } catch (x) {toast('That snapshot could not be read')}
    }
    return
  }
  if (b.dataset.pc && plateLocked(b.dataset.pc)) {
    toast('Unlocks at ' + plateLocked(b.dataset.pc));
    return
  }
  if (b.dataset.more) {
    st.menu = st.menu === b.dataset.more ? null : b.dataset.more;
    st.confirm = null;
    render();
    return
  }
  if (b.dataset.act) {
    const key = b.dataset.key, [gi, ei] = key.split('-').map(Number), e2 = todayGroups()[gi][ei], act = b.dataset.act;
    if (act === 'plates') {
      const s = cur(), x = (s.ex[e2.id] || []).find(full), sg = suggest(e2);
      st.menu = null;
      st.sheet = {type: 'plates', w: x ? +x.w : sg ? sg.w : '', bar: U() === 'kg' ? 20 : 45, name: e2.name};
      render();
      renderSheet();
      return
    }
    if (act === 'nhist') {
      st.menu = null;
      st.sheet = {type: 'nhist', id: e2.id, name: e2.name};
      render();
      renderSheet();
      return
    }
    if (act === 'warm') {
      st.warm[e2.id] = !st.warm[e2.id];
      st.menu = null;
      render();
      return
    }
    if (act === 'swap') {
      st.picker = {ctx: 'swap', gi: -1, key};
      st.menu = null;
      render();
      const pk = $('pick');
      if (pk) pk.scrollIntoView({behavior: 'smooth', block: 'center'});
      return
    }
    if (act === 'unswap') {
      const s = ensure(), orig = pd().groups[gi][ei];
      if (s.swap) delete s.swap[orig.id];
      saveS();
      st.menu = null;
      toast('Back to ' + orig.name);
      render();
      return
    }
    if (act === 'del') {
      const k = 'x' + key;
      if (st.confirm !== k) {
        st.confirm = k;
        render();
        return
      }
      const p = pd(), nm = (p.groups[gi] && p.groups[gi][ei] || {}).name;
      p.groups[gi].splice(ei, 1);
      if (!p.groups[gi].length) p.groups.splice(gi, 1);
      st.confirm = null;
      st.menu = null;
      saveP();
      toast((nm || 'Exercise') + ' removed. Logged sets are kept');
      render();
      return
    }
  }
  if (b.dataset.warm) {
    delete st.warm[b.dataset.warm];
    render();
    return
  }
  if (b.id === 'sharecard') {
    openShare();
    return
  }
  if (b.id === 'hidesafari') {
    st.hideSafari = true;
    try {sessionStorage.setItem('ov-hs', '1')} catch (x) {}render();
    return
  }
  if (b.dataset.themeSet) {
    st.set.theme = b.dataset.themeSet;
    saveSet();
    applyTheme();
    artKey = '';
    render();
    return
  }
  if (b.id === 'shownews') {
    st.sheet = {type: 'news'};
    renderSheet();
    return
  }
  if (b.dataset.exdel) {
    const k = 'x' + b.dataset.exdel;
    if (st.confirm !== k) {
      st.confirm = k;
      render();
      return
    }
    const [gi, ei] = b.dataset.exdel.split('-').map(Number), p = pd(), nm = (p.groups[gi] && p.groups[gi][ei] || {}).name;
    p.groups[gi].splice(ei, 1);
    if (!p.groups[gi].length) p.groups.splice(gi, 1);
    st.confirm = null;
    saveP();
    toast((nm || 'Exercise') + ' removed. Logged sets are kept');
    render();
    return
  }
  if (b.id === 'renbtn') {
    st.renaming = true;
    render();
    const r = $('rename');
    if (r) {
      r.focus();
      r.select()
    }
    return
  }
  if (b.id === 'bklater') {
    st.hideBk = true;
    render();
    return
  }
  if (b.id === 'bkquick' || b.id === 'bkfile') {
    await saveBackupFile();
    render();
    return
  }
  if (b.dataset.start) {
    st.plan = {days: TEMPLATES[b.dataset.start].days()};
    store('overload-plan', st.plan);
    st.set.name = st.obName.trim();
    st.set.seenVer = VERSION;
    saveSet();
    toast("You're set. Let's lift");
    render();
    window.scrollTo({top: 0});
    return
  }
  if (b.dataset.date) {
    setDate(b.dataset.date, true);
    render();
    return
  }
  if (b.dataset.wk) {
    const x = toD(st.date);
    x.setDate(x.getDate() + 7 * (+b.dataset.wk));
    setDate(iso(x), true);
    render();
    return
  }
  if (b.id === 'gotoday') {
    setDate(today(), false);
    render();
    return
  }
  if (b.dataset.ck) {
    unlockAudio();
    const s = ensure(), id = b.dataset.ck, i = +b.dataset.i, gi = +b.dataset.g, ei = +b.dataset.e;
    s.ex[id] = s.ex[id] || [];
    while (s.ex[id].length <= i) s.ex[id].push({});
    const x = s.ex[id][i];
    const row = b.closest('.srow');
    if (x.d) {
      x.d = 0;
      saveS();
      row.classList.remove('done');
      liveHero();
      return
    }
    const [wi, ri] = row.querySelectorAll('input');
    if (x.w === '' || x.w === undefined) {if (+wi.placeholder > 0) {x.w = +wi.placeholder; wi.value = x.w}}
    if (x.r === '' || x.r === undefined) {if (+ri.placeholder > 0) {x.r = +ri.placeholder; ri.value = x.r}}
    if (!full(x)) {
      toast('Enter weight and reps first');
      (+x.w > 0 ? ri : wi).focus();
      return
    }
    x.d = 1;
    haptic('tap');
    b.classList.remove('pop');
    void b.offsetWidth;
    b.classList.add('pop');
    const wasLive = !!s.start;
    startClock(s);
    if (!wasLive) setTimeout(sendLive, 150);
    if (s.pauseAt) {
      s.paused = (s.paused || 0) + (Date.now() - s.pauseAt);
      delete s.pauseAt
    }
    s.names[id] = exName(id);
    row.classList.add('done');
    saveS();
    const ex = exById(id);
    if (ex && $('tag-' + id)) $('tag-' + id).innerHTML = tagFor(ex);
    const pb = bestBefore(id, st.date);
    if (pb && e1(x) > pb && !x.prT) {
      x.prT = 1;
      toast('New PR on ' + exName(id));
      haptic('success')
    }
    const p = pd(), tg = todayGroups(), gr = tg[gi] || [];
    if (ei < gr.length - 1) {
      const nx = gr[ei + 1];
      if (!(pb && e1(x) > pb)) toast('Next: ' + nx.name);
      const t = document.getElementById('row-' + nx.id + '-' + i) || document.getElementById('row-' + nx.id + '-0');
      if (t) t.scrollIntoView({behavior: 'smooth', block: 'center'})
    } else if (st.set.autoRest) {
      let next = '';
      const first = gr[0];
      if (first && i + 1 < Math.max(+first.sets || 2, (s.ex[first.id] || []).length)) next = first.name + ' · set ' + (i + 2);
      else if (tg[gi + 1]) next = tg[gi + 1][0].name;
      else next = 'Last round. Finish strong';
      startRest(p.rest || 90, next)
    }
    liveHero();
    checkUnlocks();
    return
  }
  if (b.dataset.addset) {
    const s = ensure(), id = b.dataset.addset, ex = exById(id);
    s.ex[id] = s.ex[id] || [];
    while (s.ex[id].length < (ex ? +ex.sets || 2 : 2)) s.ex[id].push({});
    s.ex[id].push({});
    saveS();
    render();
    return
  }
  if (b.dataset.rmset) {
    const s = ensure(), id = b.dataset.rmset;
    if (s.ex[id] && s.ex[id].length) s.ex[id].pop();
    saveS();
    render();
    return
  }
  if (b.dataset.rest) {
    unlockAudio();
    startRest(+b.dataset.rest, '');
    return
  }
  if (b.id === 'startw') {
    const s = ensure();
    startClock(s);
    saveS();
    render();
    toast('Clock is running');
    return
  }
  if (b.id === 'pausew') {
    const s = ensure();
    if (s.pauseAt) {
      s.paused = (s.paused || 0) + (Date.now() - s.pauseAt);
      delete s.pauseAt
    } else s.pauseAt = Date.now();
    saveS();
    render();
    return
  }
  if (b.id === 'finish') {
    const s = ensure();
    if (s.pauseAt) {
      s.paused = (s.paused || 0) + (Date.now() - s.pauseAt);
      delete s.pauseAt
    }
    s.end = Date.now();
    saveS();
    crewLiveEnd();
    haptic('success');
    st.rest = null;
    $('rest').classList.remove('on', 'done');
    render();
    window.scrollTo({top: 0, behavior: 'smooth'});
    setTimeout(checkUnlocks, 400);
    return
  }
  if (b.id === 'resetw') {
    if (st.confirm !== 'rw') {
      st.confirm = 'rw';
      render();
      return
    }
    const s = ensure();
    delete s.start;
    delete s.end;
    delete s.pauseAt;
    s.paused = 0;
    st.confirm = null;
    saveS();
    render();
    toast('Workout clock reset');
    return
  }
  if (b.id === 'reopen') {
    const s = ensure();
    if (s.end) {
      s.pauseAt = s.end;
      delete s.end
    }
    saveS();
    render();
    return
  }
  if (b.id === 'todayadd') {
    st.picker = {ctx: 'today', gi: -1};
    render();
    return
  }
  if (b.dataset.padd !== undefined) {
    st.picker = {ctx: 'plan', gi: +b.dataset.padd};
    render();
    return
  }
  if (b.id === 'pclose') {
    st.picker = null;
    render();
    return
  }
  if (b.dataset.pick !== undefined) {
    if (b.dataset.pick === '__custom') {
      const q = $('psearch').value.trim();
      if (q) addFromPicker(q, 'Other', 8, 12)
    } else addFromPicker(...LIB[+b.dataset.pick]);
    return
  }
  if (b.dataset.lift) {
    st.prog = b.dataset.lift;
    render();
    window.scrollTo({top: 0});
    return
  }
  if (b.id === 'pback') {
    st.prog = null;
    render();
    return
  }
  if (b.dataset.pf) {
    st.pfilter = b.dataset.pf;
    render();
    return
  }
  if (b.dataset.pday) {
    st.pday = +b.dataset.pday;
    st.confirm = null;
    st.picker = null;
    render();
    return
  }
  if (b.dataset.pc) {
    d.color = b.dataset.pc;
    saveP();
    render();
    return
  }
  if (b.dataset.gdel !== undefined) {
    const k = 'g' + b.dataset.gdel;
    if (st.confirm !== k) {
      st.confirm = k;
      render();
      return
    }
    d.groups.splice(+b.dataset.gdel, 1);
    st.confirm = null;
    saveP();
    render();
    return
  }
  if (b.dataset.gup !== undefined) {
    const i = +b.dataset.gup;
    if (i > 0) {
      [d.groups[i - 1], d.groups[i]] = [d.groups[i], d.groups[i - 1]];
      saveP();
      render()
    }
    return
  }
  if (b.dataset.edel) {
    const [gi, ei] = b.dataset.edel.split('-').map(Number);
    d.groups[gi].splice(ei, 1);
    if (!d.groups[gi].length) d.groups.splice(gi, 1);
    saveP();
    render();
    return
  }
  if (b.dataset.tpl) {
    const k = 't' + b.dataset.tpl;
    if (st.confirm !== k) {
      st.confirm = k;
      render();
      return
    }
    st.plan = {days: TEMPLATES[b.dataset.tpl].days()};
    st.confirm = null;
    store('overload-plan', st.plan);
    toast('Plan replaced');
    render();
    return
  }
  if (b.id === 'nadd') {
    st.editNote = {id: '', title: '', text: '', color: 'yellow', pin: false};
    st.confirm = null;
    render();
    return
  }
  if (b.dataset.note) {
    const n = st.notes.find(x => x.id === b.dataset.note);
    if (n) {
      st.editNote = clone(n);
      st.confirm = null;
      render();
      window.scrollTo({top: 0, behavior: 'smooth'})
    }
    return
  }
  if (b.dataset.col) {
    st.editNote.title = $('ntitle').value;
    st.editNote.text = $('ntext').value;
    st.editNote.color = b.dataset.col;
    render();
    return
  }
  if (b.id === 'npin') {
    st.editNote.title = $('ntitle').value;
    st.editNote.text = $('ntext').value;
    st.editNote.pin = !st.editNote.pin;
    render();
    return
  }
  if (b.id === 'nsave') {
    const n = st.editNote;
    n.title = $('ntitle').value.trim();
    n.text = $('ntext').value.trim();
    if (!n.title && !n.text) {
      toast('Write something first');
      return
    }
    n.ts = Date.now();
    if (n.id) {
      const i = st.notes.findIndex(x => x.id === n.id);
      st.notes[i] = n
    } else {
      n.id = 'n' + Date.now().toString(36);
      st.notes.push(n)
    }
    saveN();
    st.editNote = null;
    toast('Note saved');
    checkUnlocks();
    render();
    return
  }
  if (b.id === 'ndel') {
    if (st.confirm !== 'n') {
      st.confirm = 'n';
      render();
      return
    }
    st.notes = st.notes.filter(x => x.id !== st.editNote.id);
    saveN();
    st.editNote = null;
    st.confirm = null;
    render();
    return
  }
  if (b.id === 'ncancel') {
    st.editNote = null;
    st.confirm = null;
    render();
    return
  }
  if (b.id === 'sclose') {
    st.settings = false;
    st.confirm = null;
    render();
    return
  }
  if (b.dataset.unit) {
    st.set.unit = b.dataset.unit;
    saveSet();
    render();
    return
  }
  if (b.dataset.tog === 'count') {
    try {
      localStorage.getItem('skipgc') === 't' ? localStorage.removeItem('skipgc') : localStorage.setItem('skipgc', 't')
    } catch (x) {}b.setAttribute('aria-pressed', localStorage.getItem('skipgc') !== 't');
    toast(localStorage.getItem('skipgc') === 't' ? 'Your visits won\u2019t be counted' : 'Your visits will be counted');
    return
  }
  if (b.dataset.tog === 'push') {
    b.disabled = true;
    const was = !!st.set.push;
    const on = was ? (await disablePush(), false) : await enablePush();
    b.disabled = false;
    b.setAttribute('aria-pressed', !!on);
    if (was) toast('Rest alerts off');
    return
  }
  if (b.dataset.tog) {
    const k = b.dataset.tog;
    st.set[k] = k === 'splash' ? !(st.set[k] !== false) : !st.set[k];
    saveSet();
    b.setAttribute('aria-pressed', k === 'splash' ? st.set[k] !== false : !!st.set[k]);
    if (k === 'rir') toast(st.set.rir ? 'RIR box added to each set' : 'RIR box hidden');
    return
  }
  const m = $('bkmsg');
  if (b.id === 'bk') {
    st.set.lastBackup = Date.now();
    saveSet();
    const data = JSON.stringify({app: 'overload', v: 4, sessions: st.sessions, plan: st.plan, notes: st.notes, settings: st.set});
    try {
      await navigator.clipboard.writeText(data);
      m.textContent = 'Backup copied. Paste it somewhere safe.'
    } catch (x) {
      $('rs').value = data;
      m.textContent = 'Select all the text in the box and copy it.'
    }
    return
  }
  if (b.id === 'rsb') {
    try {
      const o = JSON.parse($('rs').value);
      if (!o || typeof o !== 'object') throw 0;
      if (o.sessions) {
        Object.assign(st.sessions, o.sessions);
        if (o.plan && o.plan.days) st.plan = o.plan;
        if (Array.isArray(o.notes)) st.notes = o.notes;
        if (o.settings) Object.assign(st.set, o.settings)
      } else Object.assign(st.sessions, o);
      store('liftlog', st.sessions);
      store('overload-plan', st.plan);
      saveN();
      saveSet();
      m.textContent = 'Restored.';
      setTimeout(render, 500)
    } catch (x) {m.textContent = 'That backup could not be read. Paste the whole thing.'}return
  }
  if (b.id === 'wipe') {
    if (st.confirm !== 'wipe') {
      st.confirm = 'wipe';
      render();
      return
    }['liftlog', 'overload-plan', 'overload-notes', 'overload-settings'].forEach(k => {try {localStorage.removeItem(k)} catch (x) {}});
    st.sessions = {};
    st.notes = [];
    st.set = {
      unit: 'lb', sound: true, vibe: true, autoRest: true, awake: false, rir: false, splash: true, theme: st.set.theme || 'auto', seenVer: VERSION
    };
    st.plan = null;
    st.settings = false;
    st.confirm = null;
    st.flags = {};
    try {localStorage.removeItem('overload-flags')} catch (x) {}IDB.put('main', null);
    IDB.put('syncbase', null);
    missGuard = false;
    render();
    return
  }
});
$('rankbtn').addEventListener('click', () => {
  st.profile = !st.profile;
  st.settings = false;
  st.confirm = null;
  render();
  window.scrollTo({top: 0})
});
$('gear').addEventListener('click', () => {
  st.set17 = null;
  st.profile = false;
  st.snaps = null;
  st.settings = !st.settings;
  st.confirm = null;
  render();
  window.scrollTo({top: 0})
});
['log', 'crew', 'stats', 'plan'].forEach(t => $('t-' + t).addEventListener('click', () => {
  const again = st.tab === t && !st.settings && !st.profile;
  haptic('tap');
  st.tab = t;
  st.settings = false;
  st.profile = false;
  if (t === 'plan') setFlag('plan');
  st.confirm = null;
  st.picker = null;
  st.editNote = null;
  if (t === 'plan') st.pday = DAYS.indexOf(st.day);
  if (t === 'log' && again) setDate(today(), false);
  if (t === 'prog' && again) st.prog = null;
  if (again || !paneCached()) render();
  paneFade();
  window.scrollTo({top: 0})
}));
$('sheet').addEventListener('input', e => {
  if (e.target.id === 'bwin') {
    const v = parseFloat(e.target.value);
    st.set.bw = v > 0 && v < 1000 ? v : '';
    saveSet()
  }
});
$('sheet').addEventListener('click', async e => {
  if (e.target.id === 'sheetbg') {
    if (st.sheet && st.sheet.type === 'news') markNews();
    st.sheet = null;
    renderSheet();
    return
  }
  const b = e.target.closest('button');
  if (!b) return;
  if (b.id === 'sheetx') {
    if (st.sheet && st.sheet.back) {
      st.sheet = st.sheet.back;
      renderSheet();
      return
    }
    st.sheet = null;
    st.confirm = null;
    renderSheet();
    return
  }
  if (await sheetClick28(b)) return;
  if (await sheetClick27(b)) return;
  if (await sheetClick25(b)) return;
  if (await sheetClick24(b)) return;
  if (await sheetClick23(b)) return;
  if (await sheetClick22(b)) return;
  if (await sheetClick21(b)) return;
  if (await sheetClick19(b)) return;
  if (await sheetClick18(b)) return;
  if (await sheetClick17(b)) return;
  if (await sheetClick16(b)) return;
  if (await sheetClick15(b)) return;
  if (await sheetClick14(b)) return;
  if (await crewSheetClick(b)) return;
  if (await acctSheetClick(b)) return;
  if (await sheetClick13(b)) return;
  if (b.id === 'newsok') {
    markNews();
    st.sheet = null;
    renderSheet();
    setTimeout(() => {if (!weekReport()) checkUnlocks()}, 300);
    return
  }
  if (b.id === 'shareprof2') {
    st.sheet = {type: 'share', title: 'Share your rank'};
    renderSheet();
    const r = await profileCard();
    if (st.sheet && st.sheet.type === 'share') {
      st.sheet.url = r.url;
      st.sheet.file = r.file;
      renderSheet()
    }
    return
  }
  if (b.id === 'missno') {
    missGuard = false;
    st.sheet = null;
    renderSheet();
    mirrorNow();
    return
  }
  if (b.id === 'dorestore') {
    try {
      restoreReplace(st.sheet.data);
      toast('Your data is back');
      st.sheet = null;
      renderSheet();
      render()
    } catch (x) {toast('Could not restore')}return
  }
  if (b.dataset.csel !== undefined) {
    const i = +b.dataset.csel, a = st.copySel;
    a.includes(i) ? a.splice(a.indexOf(i), 1) : a.push(i);
    renderSheet();
    return
  }
  if (b.id === 'docopy') {
    const src = st.plan.days[st.pday];
    st.copySel.forEach(i => {st.plan.days[i] = clone(src)});
    const n = st.copySel.length;
    saveP();
    st.sheet = null;
    renderSheet();
    render();
    toast(`Copied to ${n} day${n>1?'s':''}`);
    return
  }
  if (b.dataset.bar !== undefined) {
    st.sheet.bar = +b.dataset.bar;
    renderSheet();
    return
  }
  if (b.id === 'sharego') {
    const sh = st.sheet;
    if (!sh || !sh.file) return;
    try {
      if (navigator.canShare && navigator.canShare({files: [sh.file]})) {
        await navigator.share({files: [sh.file], title: 'My Overload workout'});
        return
      }
    } catch (x) {if (x && x.name === 'AbortError') return}const a = document.createElement('a');
    a.href = sh.url;
    a.download = sh.file.name;
    document.body.appendChild(a);
    a.click();
    a.remove();
    toast('Image saved')
  }
});
$('sheet').addEventListener('input', e => {if (e.target.id === 'pcw') {st.sheet.w = e.target.value; $('pcout').innerHTML = platesBody()}});
main.addEventListener('change', e => {
  if (e.target.id === 'liftsel') {
    st.liftSel = e.target.value;
    render();
    return
  }
  if (e.target.id !== 'rsfile') return;
  const f = e.target.files && e.target.files[0];
  if (!f) return;
  const rd = new FileReader();
  rd.onload = () => {
    try {
      restoreFrom(rd.result);
      toast('Backup restored');
      render()
    } catch (x) {toast('That file could not be read')}
  };
  rd.readAsText(f)
});
setInterval(wakeSync, 2000);
document.addEventListener('click', e => {if (st.menu && !e.target.closest('.menuwrap')) {st.menu = null; st.confirm = null; render()}});
document.addEventListener('visibilitychange', () => {if (document.hidden) flushAll()});
addEventListener('pagehide', flushAll);
document.addEventListener('visibilitychange', () => {
  if (!st.set || !st.set.push) return;
  if (document.hidden) {
    if (st.rest) {
      const left = st.rest.end - Date.now();
      if (left > 3000) {
        restMsg();
        pushPost('/schedule', {endpoint: st.set.push, delay: left});
        pushPending = true
      }
    }
  } else if (pushPending) {
    pushPending = false;
    pushPost('/cancel', {endpoint: st.set.push})
  }
});
st.liveMap = {};
/* ---------- crew chat ---------- */
st.chat = {msgs: [], page: 0, more: false, loaded: false, loading: false, reply: null};
/* ---------- click handling (main) ---------- */
async function click14(b) {
  const d = b.dataset;
  if (d.ck !== undefined) {
    const s = ensure(), id = d.ck, i = +d.i, before = !!((s.ex[id] || [])[i] || {}).d, pb = bestBefore(id, st.date);
    setTimeout(() => {
      const x = (s.ex[id] || [])[i];
      if (!before && x && x.d) {
        setFX(b);
        if (pb && full(x) && e1(x) > pb && !(s.prShown || {})[id]) {
          s.prShown = s.prShown || {};
          s.prShown[id] = 1;
          prCelebrate(exName(id))
        }
      }
    }, 0);
    return false
  }
  if (d.seg === 'crew:chat') {
    st.sub.crew = 'chat';
    render();
    setTimeout(() => {
      const bx = $('chatbox');
      if (bx) bx.scrollTop = bx.scrollHeight
    }, 40);
    chatLoad();
    return true
  }
  if (b.id === 'chatsend') {
    await chatSend();
    return true
  }
  if (b.id === 'chatolder') {
    await chatLoad(true);
    return true
  }
  if (b.id === 'crepx') {
    st.chat.reply = null;
    render();
    return true
  }
  if (d.ment) {
    const inp = $('chatin');
    if (inp) {
      inp.value = inp.value.replace(/@[^@\s]*$/, '') + '@' + d.ment + ' ';
      inp.focus()
    }
    $('mentbox').classList.add('hide');
    return true
  }
  if (b.id === 'chatplus') {
    st.sheet = {type: 'chatplus'};
    renderSheet();
    return true
  }
  if (d.mmore) {
    const m = st.chat.msgs.find(x => x.id === d.mmore);
    if (m) {
      st.sheet = {type: 'msg', id: m.id};
      renderSheet()
    }
    return true
  }
  if (d.mreact) {
    const[mid, k] = d.mreact.split(':');
    await crewCall('chat/react', {mid, emoji: k});
    return true
  }
  if (b.id === 'crestbtn') {
    if (isOwner()) {
      st.sheet = {
        type: 'crest', crest: {...(st.cs.crew.crest || {shape: 'shield', icon: 'barbell', c1: '#ef2b36', c2: '#14161c', txt: ''})}
      };
      renderSheet()
    } else toast('The crew owner designs the crest');
    return true
  }
  if (b.id === 'crewrankbtn') {
    st.sheet = {type: 'crewrank'};
    renderSheet();
    return true
  }
  if (d.ctrophy) {
    const t = CTROPHY.find(x => x[0] === d.ctrophy), got = (st.cs.crew.trophies || {})[d.ctrophy];
    toast(`${t[2]} ${t[1]}: ${got?'earned '+new Date(got.ts).toLocaleDateString():t[4]}`);
    return true
  }
  if (b.id === 'crewcard') {
    st.sheet = {type: 'crewcard'};
    renderSheet();
    try {
      const r = await crewCardImage();
      if (st.sheet && st.sheet.type === 'crewcard') {
        st.sheet.url = r.url;
        st.sheet.file = r.file;
        renderSheet()
      }
    } catch (e) {toast('Could not make the card')}return true
  }
  if (d.tro) {
    const t = trophies().find(x => x.k === d.tro) || (st._viewTro || []).find(x => x.k === d.tro);
    if (t) {
      st.sheet = {type: 'trophy', t};
      renderSheet()
    }
    return true
  }
  if (b.id === 'pinpick') {
    st.sheet = {type: 'pinpick'};
    renderSheet();
    return true
  }
  if (b.id === 'lockerbtn') {
    st.sheet = {type: 'locker'};
    renderSheet();
    return true
  }
  if (b.id === 'excusebtn') {
    st.sheet = {type: 'excuse', i: Math.floor(Math.random() * EX_A.length)};
    renderSheet();
    return true
  }
  if (b.id === 'trashbtn') {
    st.sheet = {type: 'trash'};
    await trashLoad();
    renderSheet();
    if (crewOn()) {
      const r = await crewCall('trash', {});
      if (!r.error && st.sheet && st.sheet.type === 'trash') {
        st.sheet.crew = r.items;
        renderSheet()
      }
    }
    return true
  }
  if (b.id === 'exportall') {
    await exportAll();
    return true
  }
  // deletions go to Trash first (runs before the real delete, only on the confirming tap)
  const exAt = (p, key) => {
    const[gi, ei] = String(key).split('-').map(Number);
    return p && p.groups && p.groups[gi] && p.groups[gi][ei] ? {gi, e: p.groups[gi][ei]} : null
  };
  const trEx = (p, key) => {
    const x = exAt(p, key);
    if (x) trashPutSync('exercise', `${x.e.name} (from ${p.name||'plan'})`, {day: st.plan.days.indexOf(p), gi: x.gi, ex: x.e})
  };
  if (d.act === 'del' && d.key && st.confirm === 'x' + d.key) {
    trEx(pd(), d.key);
    return false
  }
  if (d.exdel && st.confirm === 'x' + d.exdel) {
    trEx(pd(), d.exdel);
    return false
  }
  if (d.edel && st.plan) {
    trEx(st.plan.days[st.pday], d.edel);
    return false
  }
  if (d.gdel !== undefined && st.confirm === 'g' + d.gdel && st.plan) {
    const day = st.plan.days[st.pday], g = day && day.groups[+d.gdel];
    if (g) trashPutSync('group', `${g.map(e=>e.name).join(' + ')} (from ${day.name})`, {day: st.pday, gi: +d.gdel, group: g});
    return false
  }
  if (d.goaldel !== undefined && st.confirm === 'G' + d.goaldel && st.set.goals && st.set.goals[+d.goaldel]) {
    const g = st.set.goals[+d.goaldel];
    trashPutSync('goal', `Goal: ${exName(g.ex)} ${g.w} ${U()}`, g);
    return false
  }
  if (b.id === 'ndel' && st.confirm === 'n' && st.editNote && st.editNote.id) {
    const n = st.notes.find(x => x.id === st.editNote.id);
    if (n) trashPutSync('note', (n.title || n.text || 'Note').slice(0, 40), n);
    return false
  }
  if (d.tpl !== undefined && st.confirm === 't' + d.tpl && st.plan) {
    trashPutSync('plan', 'Your plan before switching templates', st.plan);
    return false
  }
  if (d.rmset) {
    const s = ensure(), a = s.ex[d.rmset] || [], x = a[a.length - 1];
    if (x && full(x)) trashPutSync('set', `${exName(d.rmset)} ${x.w}×${x.r} (${s.date})`, {key: sid(), id: d.rmset, set: x});
    return false
  }
  return false
}
/* ---------- sheets ---------- */
function sheet14(sh) {
  const me = myId();
  if (sh.type === 'trash') {
    const L = TR14 || [], C = sh.crew || [];
    return `<div class="ghd"><h2>Trash</h2><button class="btn sm" id="sheetx">Done</button></div><p class="sub">Anything you delete waits here for 30 days. Tap Restore to bring it back.</p>
  ${L.length?L.map(x=>`<div class="trow"><span>${TRASH_ICON[x.kind]||'•'}</span><div><b>${esc(x.label)}</b><div class="sub">${ago(x.ts)} · ${Math.max(0,30-Math.floor((Date.now()-x.ts)/DAYMS))} days left</div></div><button class="btn sm" data-trest="${x.id}">Restore</button></div>`).join(''):'<p class="sub">Nothing on this phone is in the Trash.</p>'}
  ${crewOn()?`<h3 style="margin-top:16px">Crew trash</h3>${C.length?C.map(x=>`<div class="trow"><span>${x.kind==='msg'?'💬':'📣'}</span><div><b>${esc(String(x.text||'Post').slice(0,60))}</b><div class="sub">${x.kind==='msg'?'Message':'Post'} by ${esc((memberOf2(x.by)||{name:'?'}).name)} · ${x.left} days left</div></div><button class="btn sm" data-ctrest="${x.id}">Restore</button></div>`).join(''):'<p class="sub">No deleted crew posts or messages.</p>'}`:''}`
  }
  if (sh.type === 'locker') {
    const E = engine(), bl = boxesLeft(E).length;
    st.set.look = st.set.look || {};
    return `<div class="ghd"><h2>Customize</h2><button class="btn sm" id="sheetx">Done</button></div>${bl?`<button class="card boxcta" id="openbox"><span class="mbox">🎁</span><span class="stx"><b>${bl} mystery box${bl>1?'es':''} to open</b><span class="sub">Earned from your season pass and rank-ups</span></span></button>`:''}
  <h3>Plate finishes</h3><p class="sub">More plates (ranks, lifts, crew, funny, holiday) are in Plan → Plate.</p><div class="lkgrid">${Object.entries(FIN).filter(([k,f])=>f[5]==='finish').map(([k,f])=>{const o=finOwned(k,E);return `<button class="lki${o?'':' lk'}" data-finfo="${k}"><span class="lkp">${plateSVG(k,'done')}</span><b>${f[0]}</b><span class="sub">${o?'Unlocked':'🔒 '+f[2]}</span></button>`}).join('')}</div>
  ${Object.entries(LOCK).map(([t,v])=>`<h3>${v.name}</h3><div class="lkrow">${Object.entries(v.items).map(([k,it])=>{const o=owns(t,k,E),on=t==='react'?o:look(t)===k;return `<button class="lkc${o?'':' lk'}${on?' on':''}" data-look="${t}:${k}" aria-pressed="${on}">${t==='accent'?`<i style="background:${it[1]}"></i>`:''}${t==='icon'?`<span class="lki2">${plateSVG(it[1],it[1]==='black'?'idle':'done')}</span>`:''}${esc(it[0])}${o?'':`<span class="sub">🔒 ${esc(it[2])}</span>`}</button>`}).join('')}</div>${t==='icon'?'<p class="sub">To use a new icon on iPhone, remove Overload from your Home Screen and add it again from Safari.</p>':''}`).join('')}
  <h3>Fun</h3>${tog('roasts',st.set.roasts!==false,'Roast lines on crew posts','A funny line under each workout post. Turn off if your crew is sensitive')}${tog('restJokes',st.set.restJokes!==false,'Rest timer one-liners')}`
  }
  if (sh.type === 'pinpick') {
    const E = engine(), pins = st.set.showcase || [], L = [...E.B.filter(b => b.earned), ...E.R.filter(r => r.count > 0)];
    return `<div class="ghd"><h2>Pin badges</h2><button class="btn sm" id="sheetx">Done</button></div><p class="sub">Pick up to 3 to show on your profile and to your crew. ${pins.length}/3 pinned.</p>${L.length?`<div class="pinlist">${L.map(b=>`<button class="pinrow${pins.includes(b.k)?' on':''}" data-pinb2="${b.k}"><span class="pi">${b.icon}</span><span><b>${esc(b.name)}</b><span class="sub">${esc(b.desc||'')}</span></span><span class="pst">${pins.includes(b.k)?'Pinned':'Pin'}</span></button>`).join('')}</div>`:'<p class="sub">Earn your first badge and you can pin it here.</p>'}`
  }
  if (sh.type === 'box') {
    return `<div class="boxopen"><div class="bigbox ${sh.got?'opened':''}">🎁</div>${sh.got?(sh.got==='dupe'?`<h2>Nothing new</h2><p class="sub">You already own every mystery box item. Legend.</p>`:`<h2>${esc(LOCK[sh.got[0]].items[sh.got[1]][0])}</h2><p class="sub">New ${esc(LOCK[sh.got[0]].name.toLowerCase())}. Equip it in Customize.</p>`):'<h2>Opening…</h2>'}<button class="btn acc wide" id="sheetx" style="min-height:52px;margin-top:16px">Nice</button></div>`
  }
  if (sh.type === 'trophy') {
    const t = sh.t;
    return `<div class="ghd"><h2>${esc(t.name)}</h2><button class="btn sm" id="sheetx">Done</button></div><div class="tspin${t.earned?'':' dim'}">${trophySVG(t,true)}</div><p style="text-align:center">${t.earned?'Earned':'Not yet'}</p><p class="sub" style="text-align:center">${esc(t.how)}</p>`
  }
  if (sh.type === 'excuse') {
    return `<div class="ghd"><h2>Excuse generator</h2><button class="btn sm" id="sheetx">Done</button></div><div class="excuse">“${esc(EX_A[sh.i%EX_A.length])}”</div><div class="exfoot"><button class="btn" id="excnext">🎲 Another one</button>${crewOn()?'<button class="btn acc" id="excsend">Send to crew chat</button>':''}</div><p class="sub" style="margin-top:10px">Or, hear us out, just go lift.</p>`
  }
  if (sh.type === 'crest') {
    const c = sh.crest;
    return `<div class="ghd"><h2>Crew crest</h2><button class="btn sm" id="sheetx">Cancel</button></div><div style="text-align:center;margin:8px 0 14px">${crestSVG(c,crewRankInfo().r,120)}</div>
  <div class="glbl">SHAPE</div><div class="lkrow">${['shield','plate','circle','hex'].map(k=>`<button class="lkc${c.shape===k?' on':''}" data-crs="shape:${k}">${k[0].toUpperCase()+k.slice(1)}</button>`).join('')}</div>
  <div class="glbl">ICON</div><div class="lkrow">${Object.keys(CREST_ICON).map(k=>`<button class="lkc${c.icon===k?' on':''}" data-crs="icon:${k}">${k[0].toUpperCase()+k.slice(1)}</button>`).join('')}</div>
  <div class="glbl">COLORS</div><div class="lkrow">${[['#ef2b36','#14161c'],['#2563ff','#0b1a3e'],['#16b65b','#0a2e18'],['#7c4dff','#1b0f4a'],['#c99a2e','#2a1e05'],['#00c2b2','#06302c'],['#ff3d8b','#2e0617'],['#e8e8ec','#3a3d46']].map(([a,b2])=>`<button class="lkc${c.c1===a?' on':''}" data-crs="col:${a},${b2}"><i style="background:linear-gradient(${a},${b2})"></i></button>`).join('')}</div>
  <div class="glbl">INITIALS</div><input id="crtxt" class="field" maxlength="3" value="${esc(c.txt||'')}" placeholder="TF" style="text-transform:uppercase"><p class="sub">The crest gets fancier as your crew ranks up: borders at Silver, gold trim at Platinum, a glow at Titan and laurels at Olympian.</p><button class="btn acc wide" id="crsave" style="min-height:52px;margin-top:10px">Save crest</button>`
  }
  if (sh.type === 'crewrank') {
    const R = crewRankInfo();
    return `<div class="ghd"><h2>Crew rank</h2><button class="btn sm" id="sheetx">Done</button></div><div style="text-align:center"><div class="pemb">${emblem(R.r,'spin')}</div><h1 style="font-size:36px">${RANKS[R.r][0]}</h1><p class="sub">${R.pts.toLocaleString()} crew points${R.nx?` · ${(R.nx-R.pts).toLocaleString()} to ${RANKS[R.r+1][0]}`:' · Top rank'}</p></div><div class="pbar"><i><u style="width:${Math.round(R.frac*100)}%"></u></i></div><p class="sub">Crew points come from crew XP (every workout and post) plus 400 for each crew trophy. Higher ranks unlock better banners and a fancier crest.</p>`
  }
  if (sh.type === 'crewcard') return `<div class="ghd"><h2>Crew card</h2><button class="btn sm" id="sheetx">Done</button></div>${sh.url?`<img src="${sh.url}" alt="Crew card" style="width:100%;border-radius:16px"><button class="btn acc wide" id="crewcardshare" style="min-height:52px;margin-top:12px">Share</button><p class="sub">Or press and hold the image to save it.</p>`:'<p class="sub">Making your card…</p>'}`;
  if (sh.type === 'chatplus') {
    const s = lastFinished() || realSessions().slice(-1)[0];
    return `<div class="ghd"><h2>Share to chat</h2><button class="btn sm" id="sheetx">Cancel</button></div>${s?`<button class="card postnudge" id="chatcard"><span>🏋️</span><div><b>Share your ${esc(postData(s).day)} workout</b><div class="sub">${s.date}</div></div></button>`:''}<button class="card postnudge" id="excusebtn2"><span>🎲</span><div><b>Send a gym excuse</b><div class="sub">For when you’re skipping</div></div></button>`
  }
  if (sh.type === 'msg') {
    const m = st.chat.msgs.find(x => x.id === sh.id);
    if (!m) return '<p class="sub">Message gone.</p><button class="btn wide" id="sheetx">Close</button>';
    const mine = m.by === me, own = isOwner();
    return `<div class="ghd"><h2>Message</h2><button class="btn sm" id="sheetx">Done</button></div><p class="sub">${esc((m.text||'Workout card').slice(0,120))}</p><div class="lkrow">${reactKeys().map(k=>`<button class="lkc" data-mreact2="${m.id}:${k}">${RE14[k]}</button>`).join('')}</div>
  <div class="exfoot" style="margin-top:12px"><button class="btn" id="msgreply">↪ Reply</button>${mine||own?`<button class="btn" id="msgdel">${st.confirm==='md'?'Tap to delete':'Delete'}</button>`:''}${own||((myMember()||{}).role==='Co-captain')?`<button class="btn" data-mpin="${m.id}">📌 ${(st.cs.pins||[]).some(x=>x.id===m.id)?'Unpin':'Pin'}</button>`:''}${own&&!mine?`<button class="btn" id="msgmute">${(st.cs.crew.muted||{})[m.by]>Date.now()?'Unmute':'Mute 24h'}</button>`:''}</div>`
  }
  if (sh.type === 'crewsnaps') {
    return `<div class="ghd"><h2>Crew backups</h2><button class="btn sm" id="sheetx">Done</button></div><p class="sub">The server saves your whole crew every day and keeps 30 days. Restoring only adds back what’s missing. Nothing current gets removed.</p>${(sh.days||[]).map(x=>`<button class="btn sm" data-crsnap="${x.day}" style="margin:4px">${st.confirm==='cs'+x.day?'Tap to restore '+x.day:x.day}</button>`).join('')||'<p class="sub">Loading…</p>'}`
  }
  if (sh.type === 'member' && st.cs) {
    const m = memberOf2(sh.id);
    if (!m) return null;
    const base = crewSheet(sh);
    if (!base) return null;
    const p = m.profile || {};
    let extra = '';
    if (Array.isArray(p.troph)) {
      st._viewTro = trophies().map(t => ({...t, earned: p.troph.includes(t.k)}));
      extra += trophyRoomHTML(st._viewTro, m.id === me ? '' : m.name)
    }
    extra += shameHTML(m.id);
    const ban = p.pbanner && p.pbanner !== 'none' ? `<div class="pban" style="margin:0 0 10px;border-radius:16px">${sceneSVG(p.pbanner,8,'mb'+m.id)}</div>` : '';
    return ban + base + extra
  }
  return null
}
async function sheetClick14(b) {
  const sh = st.sheet, d = b.dataset;
  if (!sh) return false;
  if (d.trest) {
    const ok = await trashRestore(d.trest);
    toast(ok ? 'Restored' : 'Could not restore that one');
    renderSheet();
    render();
    return true
  }
  if (d.ctrest) {
    const r = await crewCall('restore', {item: d.ctrest});
    if (!r.error) {
      toast('Restored to the crew');
      sh.crew = (sh.crew || []).filter(x => x.id !== d.ctrest);
      st.chat.loaded = false;
      renderSheet();
      render()
    }
    return true
  }
  if (b.id === 'openbox') {
    const got = openBox();
    st.sheet = {type: 'box'};
    renderSheet();
    haptic('heavy');
    setTimeout(() => {
      st.sheet = {type: 'box', got};
      renderSheet();
      if (got && got !== 'dupe') confetti(80)
    }, 900);
    return true
  }
  if (d.look) {
    const[t, k] = d.look.split(':');
    if (t === 'react') {
      toast(owns(t, k) ? 'You can use this reaction' : '🔒 ' + LOCK[t].items[k][2]);
      return true
    }
    if (!owns(t, k)) {
      toast('🔒 ' + LOCK[t].items[k][2]);
      return true
    }
    st.set.look = st.set.look || {};
    st.set.look[t] = k;
    saveSet();
    applyLook();
    if (t === 'sound') {if (!playPack()) beep()}if (t === 'setfx') setFX(b);
    if (t === 'prfx') prCelebrate('');
    if (t === 'nameFx' || t === 'card' || t === 'pbanner') syncCrew(true);
    renderSheet();
    render();
    return true
  }
  if (d.finfo) {
    const f = FIN[d.finfo];
    toast(finOwned(d.finfo) ? `${f[0]} is unlocked. Pick it in Plan → Plate color` : `🔒 ${f[0]}: ${f[2]}`);
    return true
  }
  if (b.id === 'excnext') {
    sh.i = (sh.i + 1 + Math.floor(Math.random() * (EX_A.length - 1))) % EX_A.length;
    renderSheet();
    return true
  }
  if (b.id === 'excsend') {
    const t = EX_A[sh.i % EX_A.length];
    st.sheet = null;
    renderSheet();
    st.tab = 'crew';
    st.sub.crew = 'chat';
    render();
    const inp = $('chatin');
    if (inp) inp.value = 'Can’t make it today. ' + t;
    await chatSend();
    st.set.excuses = (st.set.excuses || 0) + 1;
    saveSet();
    return true
  }
  if (b.id === 'excusebtn2') {
    st.sheet = {type: 'excuse', i: Math.floor(Math.random() * EX_A.length)};
    renderSheet();
    return true
  }
  if (b.id === 'chatcard') {
    const s = lastFinished() || realSessions().slice(-1)[0];
    st.sheet = null;
    renderSheet();
    if (s) {
      const pd2 = postData(s);
      await chatSend({
        card: {
          day: pd2.day, color: pd2.color, min: pd2.min, sets: pd2.sets, vol: pd2.vol, prs: pd2.prs, unit: pd2.unit, top: pd2.top
        }
      })
    }
    return true
  }
  if (d.mreact2) {
    const[mid, k] = d.mreact2.split(':');
    st.sheet = null;
    renderSheet();
    await crewCall('chat/react', {mid, emoji: k});
    return true
  }
  if (b.id === 'msgreply') {
    const m = st.chat.msgs.find(x => x.id === sh.id);
    st.chat.reply = m ? {id: m.id, by: m.by, text: m.text} : null;
    st.sheet = null;
    renderSheet();
    render();
    setTimeout(() => {
      const i = $('chatin');
      if (i) i.focus()
    }, 50);
    return true
  }
  if (b.id === 'msgdel') {
    if (st.confirm !== 'md') {
      st.confirm = 'md';
      renderSheet();
      return true
    }
    st.confirm = null;
    await crewCall('chat/delete', {mid: sh.id});
    st.sheet = null;
    renderSheet();
    toast('Deleted. It’s in the crew Trash for 30 days');
    return true
  }
  if (b.id === 'msgmute') {
    const m = st.chat.msgs.find(x => x.id === sh.id);
    if (m) {
      const on = (st.cs.crew.muted || {})[m.by] > Date.now();
      await crewCall('chat/mute', {target: m.by, hours: 24, off: on ? 1 : 0});
      toast(on ? 'Unmuted' : 'Muted for 24 hours');
      await syncCrew(true)
    }
    st.sheet = null;
    renderSheet();
    return true
  }
  if (d.crs) {
    const[k, v] = d.crs.split(':');
    if (k === 'col') {
      const[a, c2] = v.split(',');
      sh.crest.c1 = a;
      sh.crest.c2 = c2
    } else sh.crest[k] = v;
    sh.crest.txt = ($('crtxt') || {}).value || sh.crest.txt;
    renderSheet();
    return true
  }
  if (b.id === 'crsave') {
    sh.crest.txt = (($('crtxt') || {}).value || '').toUpperCase();
    const r = await crewCall('crest', {crest: sh.crest});
    if (!r.error) {
      toast('Crest saved');
      st.sheet = null;
      renderSheet();
      render()
    }
    return true
  }
  if (b.id === 'crewcardshare') {
    try {
      if (navigator.canShare && navigator.canShare({files: [sh.file]})) await navigator.share({files: [sh.file], title: st.cs.crew.name});
      else {
        const a = document.createElement('a');
        a.href = sh.url;
        a.download = 'crew-card.png';
        a.click()
      }
    } catch (e) {}return true
  }
  if (b.id === 'crrename') {
    const n = (($('crrenin') || {}).value || '').trim();
    if (!n) {
      toast('Enter a name');
      return true
    }
    const r = await crewCall('rename', {name: n});
    if (!r.error) {
      toast('Crew renamed to ' + n);
      renderSheet();
      render()
    }
    return true
  }
  if (d.scene) {
    if (!sceneOK(d.scene)) {
      toast('🔒 ' + SCENES[d.scene][1]);
      return true
    }
    await crewCall('banner', {banner: d.scene});
    renderSheet();
    render();
    return true
  }
  if (b.id === 'crsnapsbtn') {
    st.sheet = {type: 'crewsnaps', days: null};
    renderSheet();
    const r = await crewCall('snaps', {});
    if (st.sheet && st.sheet.type === 'crewsnaps') {
      st.sheet.days = r.days || [];
      renderSheet()
    }
    return true
  }
  if (d.crsnap) {
    if (st.confirm !== 'cs' + d.crsnap) {
      st.confirm = 'cs' + d.crsnap;
      renderSheet();
      return true
    }
    st.confirm = null;
    const r = await crewCall('snaprestore', {day: d.crsnap});
    toast(r.error ? 'Could not restore' : `Restored. ${r.added||0} item${r.added===1?'':'s'} came back`);
    st.sheet = null;
    renderSheet();
    render();
    return true
  }
  if (d.shame) {
    const r = await crewCall('shame', {target: d.shame});
    if (!r.error) {
      toast(r.crew.shame && r.crew.shame.target === d.shame ? 'Shame plate awarded 🩷' : 'Vote counted');
      renderSheet();
      render()
    }
    return true
  }
  return false
}
/* extra pieces in the crew settings sheet */
function crewInfoExtra() {
  const c = st.cs.crew, own = isOwner();
  if (!own) return '';
  return `<h3>Crew name</h3><div class="row2"><input id="crrenin" class="field" maxlength="30" value="${esc(c.name)}"><button class="btn" id="crrename">Rename</button></div>
 <h3>Crew banner</h3><div class="scgrid">${Object.entries(SCENES).filter(([k])=>k!=='founder'||sceneOK(k)).map(([k,v])=>{const ok=sceneOK(k),on=(LEGACY_SCENE[c.banner]||c.banner)===k;return `<button class="scb${ok?'':' lk'}${on?' on':''}" data-scene="${k}" aria-pressed="${on}">${sceneSVG(k,c.level,'pick'+k)}<span>${esc(v[0])}${ok?'':' · 🔒 '+esc(v[1])}</span></button>`}).join('')}</div>
 <h3>Crew motto and colors</h3><input id="crmotto" class="field" maxlength="60" placeholder="No days off" value="${esc((st.cs.style||{}).motto||'')}"><div class="lkrow" style="margin-top:8px">${['#ef2b36','#2563ff','#16b65b','#7c4dff','#c99a2e','#06b6d4','#ff3d8b','#e8e8ec'].map(x=>`<button class="lkc${(st.cs.style||{}).c1===x?' on':''}" data-crc="${x}"><i style="background:${x}"></i></button>`).join('')}</div><button class="btn" id="crstylesave">Save motto and colors</button>
 <h3>Safety</h3><button class="btn" id="crsnapsbtn">Crew backups (30 days)</button>`
}
/* shame plate voting: inside a member's sheet */
function shameHTML(id) {
  if (!st.cs || id === myId() || st.cs.members.length < 2) return '';
  const V = st.cs.crew.shameVotes || {}, mine = V.mine === id, n = (V.tally || {})[id] || 0, need = Math.max(1, Math.ceil((st.cs.members.length - 1) / 2));
  return shamed(id) ? `<p class="sub" style="margin-top:12px">🩷 Wearing the shame plate this week. The crew has spoken.</p>` : `<button class="btn sm" data-shame="${id}" style="margin-top:12px">${mine?'You voted':'🩷 Vote shame plate'} · ${n}/${need}</button><p class="sub">For skipping leg day. Lasts a week. All in good fun.</p>`
}
/* ---------- boot ---------- */
function boot14() {
  try {
    if (!st.set.look) st.set.look = {};
    applyLook()
  } catch (e) {console.warn('look', e)}try {
    const g = st.set.ghostD && st.set.ghostD.d === today() ? st.set.ghostD : {d: today(), n: 0};
    g.n++;
    st.set.ghostD = g;
    if (g.n >= 5 && !Object.values(st.sessions).some(s => s.date === today() && Object.values(s.ex || {}).some(a => (a || []).some(full)))) {
      st.set.fb = st.set.fb || {};
      st.set.fb.ghost = 1;
      dirtyEng()
    }
    saveSet();
    updateGuard().then(() => {if (st.plan) {try {grant14()} catch (e) {}}});
    document.addEventListener('click', e => {
      const t = e.target.closest('#rstop');
      if (t && st.rest && st.rest.end > Date.now()) {
        const s = cur();
        s.skips = (s.skips || 0) + 1;
        saveS()
      }
    }, true);
    document.addEventListener('click', e => {
      const pl = e.target.closest('.bigplate');
      if (pl && !e.target.closest('button')) plateTalk(pl)
    });
    document.addEventListener('keydown', e => {if (e.key === 'Enter' && e.target && e.target.id === 'chatin') {e.preventDefault(); chatSend()}});
    document.addEventListener('input', e => {
      if (e.target && e.target.id === 'chatin') {
        const mb = $('mentbox');
        if (mb) mb.classList.toggle('hide', !/@[^@\s]*$/.test(e.target.value))
      }
    });
    const _cu = checkUnlocks;
    checkUnlocks = function() {
      const r = _cu.apply(this, arguments);
      try {grant14()} catch (e) {}return r
    };
  } catch (e) {console.warn('boot14', e)}
}
/* small helpers used by patched code */
function welcomeFX() {
  const k = look('welcome');
  if (k === 'lightning') return '<svg class="wfx wfx-lightning" viewBox="0 0 100 100" preserveAspectRatio="none"><path d="M62 0L44 40h12L40 100l30-56H58l14-44z" fill="#fff6c8" opacity="0"/></svg>';
  if (k === 'forge') return `<div class="wfx wfx-forge">${Array.from({length:24},(_,i)=>`<i style="--x:${(i*37)%100}%;--d:${(i%8)*.15}s"></i>`).join('')}</div>`;
  if (k === 'galaxy') return `<div class="wfx wfx-galaxy">${Array.from({length:40},(_,i)=>`<i style="--x:${(i*53)%100}%;--y:${(i*29)%100}%;--d:${(i%7)*.2}s"></i>`).join('')}</div>`;
  return ''
}
function evTier(b) {
  if (!ENG || !LADDER[b.cat]) return 1;
  const L = ENG.B.filter(x => x.cat === b.cat);
  return Math.min(6, L.indexOf(b) + 1)
}
function shareG(col) {
  const k = look('share');
  if (k === 'midnight') return ['#0b0c10', '#1a1c26', '#2a2e3a'];
  if (k === 'gold') return ['#7a5410', '#c99a2e', '#ffe07a'];
  if (k === 'neon') return ['#0d0f14', '#ff2bd6', '#00e5ff'];
  return SHG[col] || SHG.red
}
Object.assign(RE, RE14);
window.__FIN = FIN;
{
  const _mp = myProfile;
  myProfile = function() {
    const p = _mp();
    try {
      p.nameFx = look('nameFx');
      p.card = look('card');
      p.pbanner = look('pbanner');
      p.troph = trophyKeys()
    } catch (e) {}return p
  }
}

Object.entries(T15).forEach(([k, t]) => {
  PC[k] = [t[1][0], t[1][1], t[1][2], `radial-gradient(circle at 35% 30%,${t[1][1]},${t[1][0]} 55%,${t[1][2]})`];
  FIN[k] = [t[0], PC[k], t[2], t[3], null, t[4]];
  if (typeof SHG !== 'undefined' && !SHG[k]) SHG[k] = [t[1][2], t[1][0], t[1][1]]
});
Object.keys(FIN).forEach(k => {if (!FIN[k][5]) FIN[k][5] = 'finish'});
/* what gets published to the crew */
{
  const _mp2 = myProfile;
  myProfile = function() {
    const p = _mp2();
    try {
      const P = st.set.prof || {};
      Object.assign(p, {
        motto: P.motto || '', gym: P.gym || '', phase: P.phase || '', ltype: P.ltype || '', song: P.song || '', av: st.set.av || null, pacc: P.acc || '', rim: st.set.rim || '', tpins: (P.tpins || []).slice(0, 3), flex: myFlex(), prtl: prTimeline().slice(-20), best: bestLift(), plates: Object.keys(FIN).filter(k => FIN[k][5] !== 'finish' && finOwned(k)).slice(0, 40), fav: P.fav || ''
      })
    } catch (e) {}return p
  }
}

setInterval(() => {
  document.querySelectorAll('[data-rend]').forEach(el => {
    const t = +el.dataset.rend;
    if (t > Date.now()) el.textContent = 'Resting ' + fmtLeft(t);
    else {
      el.textContent = 'Up next';
      el.classList.remove('rest');
      delete el.dataset.rend
    }
  })
}, 1000);
/* ---------- crew extras ---------- */
Object.defineProperty(RE14, 'crest', {
  get() {return st.cs ? `<span class="rcrest">${crestSVG(st.cs.crew.crest,0,18)}</span>` : '🛡'}, enumerable: true, configurable: true
});
/* ---------- clicks ---------- */
async function click15(b) {
  const d = b.dataset;
  if (d.ck !== undefined && GRP) setTimeout(sendLive, 120);
  if (b.id === 'platepick') {
    st.sheet = {type: 'plate'};
    renderSheet();
    return true
  }
  if (b.id === 'myprof') {
    st.sheet = {type: 'member', id: myId()};
    renderSheet();
    return true
  }
  if (b.id === 'troombtn') {
    st.sheet = {type: 'troom'};
    renderSheet();
    return true
  }
  if (b.id === 'raidbtn') {
    st.sheet = {type: 'raid'};
    renderSheet();
    return true
  }
  if (b.id === 'grpbtn') {
    st.sheet = {type: 'group'};
    renderSheet();
    return true
  }
  if (d.gjoin) {
    const r = await crewCall('group/join', {gid: d.gjoin});
    if (!r.error) {
      setGrp(d.gjoin);
      toast('Lifting together 👥');
      sendLive();
      render()
    } else toast('That group already finished');
    return true
  }
  if (b.id === 'grpleave') {
    const g = GRP;
    setGrp(null);
    await crewCall('group/leave', {gid: g});
    render();
    return true
  }
  if (b.id === 'grpend') {
    const g = GRP, me = myId(), l = grpLive();
    const r = await crewCall('group/end', {
      gid: g, rows: {[me]: {sets: l.sets || 0, vol: Math.round((l.vol || 0) * (U() === 'kg' ? 2.20462 : 1)), prs: l.prs || 0}}
    });
    setGrp(null);
    if (r.post) {
      st.sheet = {type: 'grpcard', post: r.post};
      renderSheet()
    }
    render();
    return true
  }
  if (d.arena2) {
    st.sub.arena2 = d.arena2;
    render();
    return true
  }
  if (d.hofp) {
    if (!isOwner()) return true;
    st.sheet = {type: 'hofp', ref: d.hofp};
    renderSheet();
    return true
  }
  if (d.hofdel) {
    await crewCall('hof', {remove: d.hofdel});
    render();
    return true
  }
  if (b.classList.contains('c15h') || b.closest && b.closest('.c15h')) {return false}
  return false
}
document.addEventListener('click', e => {
  const h = e.target.closest('.c15h');
  if (!h) return;
  const c = h.closest('.c15');
  if (!c) return;
  c.classList.toggle('shut');
  st._open15 = st._open15 || {};
  st._open15[h.dataset.c15t] = !c.classList.contains('shut')
});
/* ---------- sheets ---------- */
function sheet15(sh) {
  if (sh.type === 'plate') return plateSheet(sh);
  if (sh.type === 'member' && (st.cs || sh.id === myId())) return profPage(sh.id);
  if (sh.type === 'profedit') return profEditSheet(sh);
  if (sh.type === 'troom') {
    const v = sh.of ? memberData(sh.of) : null;
    return `<div class="ghd"><h2>Trophy room</h2><button class="btn sm" id="sheetx">Done</button></div>${trophyRoomHTML(v?st._viewTro:null,v?v.name:'')}`
  }
  if (sh.type === 'raid' && st.cs && st.cs.raid) return raidSheet();
  if (sh.type === 'group') {
    const act = activeGroups();
    return `<div class="ghd"><h2>Lift together</h2><button class="btn sm" id="sheetx">Cancel</button></div><p class="sub">Start a group when you’re at the gym with crewmates. Everyone sees each other’s sets and rest timers live, and you finish with one shared workout card.</p>${act.map(([g,x])=>`<button class="card grpinv" data-gjoin="${g}"><span>👥</span><div><b>${esc((memberOf2(x.host)||{name:'?'}).name)}’s group</b><div class="sub">${x.members.map(id=>esc((memberOf2(id)||{name:'?'}).name)).join(', ')}</div></div><span class="chev">Join</span></button>`).join('')}<button class="btn acc wide" id="grpstart" style="min-height:52px;margin-top:10px">Start a group</button><p class="sub" style="margin-top:8px">Your crew gets a ping so they can join.</p>`
  }
  if (sh.type === 'grpcard') return `<div class="ghd"><h2>Lifted together</h2><button class="btn sm" id="sheetx">Done</button></div>${groupPostHTML(sh.post)}<p class="sub">Posted to your crew feed.</p>`;
  if (sh.type === 'hofp') {
    const p = (st.cs.posts || []).find(x => x.id === sh.ref);
    return `<div class="ghd"><h2>Hall of Fame</h2><button class="btn sm" id="sheetx">Cancel</button></div>${p?postHTML(p,true):''}<div class="glbl">WHY IT’S LEGENDARY</div><input class="field" id="hoftxt" maxlength="80" placeholder="First 405 deadlift in crew history" value="${esc(p&&p.cap||'')}"><button class="btn acc wide" id="hofsave" style="min-height:52px;margin-top:12px">Induct</button>`
  }
  if (sh.type === 'dash') return dashSheet(sh);
  if (sh.type === 'founder') return `<div class="ghd"><h2>Founder code</h2><button class="btn sm" id="sheetx">Cancel</button></div><p class="sub">For the people who built Overload. A Founder or Co-Founder code unlocks its own plate, title and banners on your account.</p><input class="field" id="fcode" autocapitalize="characters" placeholder="FNDR-XXXXXX-…"><button class="btn acc wide" id="fsave" style="min-height:52px;margin-top:12px">Unlock</button>`;
  return null
}
async function sheetClick15(b) {
  const sh = st.sheet, d = b.dataset;
  if (!sh) return false;
  if (d.crc) {
    sh.crc = d.crc;
    document.querySelectorAll('[data-crc]').forEach(x => x.classList.toggle('on', x === b));
    return true
  }
  if (b.id === 'crstylesave') {
    const c1 = sh.crc || (st.cs.style || {}).c1 || '';
    const r = await crewCall('crewstyle', {motto: (($('crmotto') || {}).value || '').trim(), c1, c2: c1});
    if (!r.error) {
      toast('Crew style saved');
      renderSheet();
      render()
    }
    return true
  }
  if (d.tro) {
    const t = trophies().find(x => x.k === d.tro) || (st._viewTro || []).find(x => x.k === d.tro);
    if (t) {
      st.sheet = {type: 'trophy', t};
      renderSheet()
    }
    return true
  }
  if (sh.type === 'plate') {
    if (d.pcat) {
      sh.cat = d.pcat;
      sh.sel = null;
      renderSheet();
      return true
    }
    if (d.psel) {
      sh.sel = d.psel;
      renderSheet();
      return true
    }
    if (b.id === 'plocked') {
      sh.locked = !sh.locked;
      renderSheet();
      return true
    }
    if (b.id === 'puse') {
      const k = sh.sel || colorOf(st.plan.days[st.pday]);
      if (plateLocked(k) || (FIN[k] && !finOwned(k))) {
        toast('🔒 ' + (plateLocked(k) || FIN[k][2]));
        return true
      }
      st.plan.days[st.pday].color = k;
      saveP();
      st.sheet = null;
      renderSheet();
      render();
      toast('Plate set');
      return true
    }
  }
  if (b.id === 'profedit') {
    const P = JSON.parse(JSON.stringify(st.set.prof || {}));
    st.sheet = {
      type: 'profedit', p: P, av: {...(st.set.av || {})}, flex: (st.set.flex || ['best', 'streak', 'vol']).slice()
    };
    renderSheet();
    return true
  }
  if (sh.type === 'profedit') {
    const grab = () => {
      document.querySelectorAll('[data-pe]').forEach(i => sh.p[i.dataset.pe] = i.value.trim());
      const r = $('rimin');
      if (r) sh.rim = r.value.trim()
    };
    if (d.av) {
      grab();
      const[t, v] = d.av.split(':');
      sh.av = t === 'init' ? {c: sh.av.c} : {t, v, c: sh.av.c};
      renderSheet();
      return true
    }
    if (d.avc) {
      grab();
      sh.av = {...sh.av, c: d.avc};
      renderSheet();
      return true
    }
    if (d.pset) {
      grab();
      const i = d.pset.indexOf(':'), k = d.pset.slice(0, i), v = d.pset.slice(i + 1);
      sh.p[k] = sh.p[k] === v && k !== 'acc' ? '' : v;
      renderSheet();
      return true
    }
    if (d.flex) {
      grab();
      const f = sh.flex, i = f.indexOf(d.flex);
      if (i >= 0) f.splice(i, 1);
      else {
        if (f.length >= 3) f.shift();
        f.push(d.flex)
      }
      renderSheet();
      return true
    }
    if (d.tpin) {
      grab();
      const a = sh.p.tpins = sh.p.tpins || [], i = a.indexOf(d.tpin);
      if (i >= 0) a.splice(i, 1);
      else {
        if (a.length >= 3) a.shift();
        a.push(d.tpin)
      }
      renderSheet();
      return true
    }
    if (b.id === 'jersave') {
      const n = +($('jerin') || {}).value;
      const r = await crewCall('jersey', {num: n});
      toast(r.error ? (r.error === 'taken' ? 'That number is taken' : 'Could not claim it') : 'You’re #' + n);
      return true
    }
    if (b.id === 'profsave') {
      grab();
      st.set.prof = sh.p;
      st.set.av = sh.av && (sh.av.t || sh.av.c) ? sh.av : null;
      st.set.flex = sh.flex;
      st.set.rim = (sh.rim || '').slice(0, 24);
      saveSet();
      st.sheet = {type: 'member', id: myId()};
      renderSheet();
      render();
      syncCrew(true);
      toast('Profile saved');
      return true
    }
  }
  if (d.respect) {
    const r = await crewCall('respect', {target: d.respect});
    if (!r.error) {
      haptic('heavy');
      toast('💪 Respect given');
      renderSheet()
    }
    return true
  }
  if (d.nick) {
    const n = (($('nickin') || {}).value || '').trim();
    const r = await crewCall('nick', {target: d.nick, nick: n});
    if (!r.error) {
      toast(n ? 'Nickname vote counted' : 'Vote removed');
      renderSheet()
    }
    return true
  }
  if (d.role) {
    const i = d.role.indexOf(':');
    await crewCall('role', {target: d.role.slice(0, i), role: d.role.slice(i + 1)});
    renderSheet();
    return true
  }
  if (d.hofm) {
    const m = memberOf2(d.hofm);
    const r = await crewCall('hof', {kind: 'member', ref: d.hofm, text: ''});
    if (!r.error) toast(`${m.name} is in the Hall of Fame`);
    return true
  }
  if (d.viewtro) {
    const m = memberData(d.viewtro);
    st._viewTro = trophies().map(t => ({...t, earned: (m.profile.troph || []).includes(t.k)}));
    st.sheet = {type: 'troom', of: d.viewtro};
    renderSheet();
    return true
  }
  if (b.id === 'grpstart') {
    const r = await crewCall('group/start', {sync: true});
    if (r.gid) {
      setGrp(r.gid);
      st.sheet = null;
      renderSheet();
      st.tab = 'log';
      sendLive();
      render();
      toast('Group started. Your crew got a ping')
    }
    return true
  }
  if (b.id === 'hofsave') {
    const r = await crewCall('hof', {kind: 'post', ref: sh.ref, text: (($('hoftxt') || {}).value || '').trim()});
    if (!r.error) {
      toast('Inducted into the Hall of Fame 🏛');
      st.sheet = null;
      renderSheet();
      render()
    }
    return true
  }
  if (d.mpin) {
    await crewCall('chat/pin', {mid: d.mpin});
    st.sheet = null;
    renderSheet();
    await syncCrew(true);
    render();
    return true
  }
  if (b.id === 'fsave') {
    try {
      await vaultCall('/founder', acctBody({code: (($('fcode') || {}).value || '').trim()}));
      st.set.founder = true;
      saveSet();
      st.sheet = null;
      renderSheet();
      confetti(140);
      toast('👑 Welcome, Founder');
      syncCrew(true);
      render()
    } catch (e) {toast(e.message === 'wrong code' ? 'That code isn’t right' : 'Sign in first, then try again')}return true
  }
  return false
}
async function openDash() {
  st.sheet = {type: 'dash'};
  renderSheet();
  try {
    const r = await fetch(PUSH_URL + '/stats/dash', {
      method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(acctBody({}))
    });
    const j = await r.json();
    if (st.sheet && st.sheet.type === 'dash') {
      if (j.error) st.sheet.err = j.error === 'owner only' ? 'Only the founder account can open this.' : j.error;
      else st.sheet.d = j;
      renderSheet()
    }
  } catch (e) {
    if (st.sheet && st.sheet.type === 'dash') {
      st.sheet.err = 'Could not load. Check your connection.';
      renderSheet()
    }
  }
}
function settings15() {
  return settings16() + `${st.set.founder?`<div class="card"><h3>👑 Founder</h3><button class="btn wide" id="dashbtn">📊 Owner dashboard</button></div>`:''}`
}
/* ---------- boot ---------- */
function boot15() {
  try {addLiftPlates()} catch (e) {console.warn('lifts', e)}
  try {
    statsPing();
    document.addEventListener('visibilitychange', () => {if (!document.hidden) statsPing()})
  } catch (e) {}
  document.addEventListener('click', e => {
    const b = e.target.closest('#dashbtn');
    if (b) openDash();
    const f = e.target.closest('#founderlink');
    if (f) {
      st.sheet = {type: 'founder'};
      renderSheet()
    }
  });
}
const hofBtn = p => isOwner() && p.type === 'workout' ? `<button class="hofb" data-hofp="${p.id}" aria-label="Induct into Hall of Fame">🏛 Hall of Fame</button>` : '';Object.assign(LOCK.pbanner.items, {
  sunrise: ['Sunrise', '', '', E => true], neon: ['Neon Gym', '', '4 week streak', E => E.streak >= 4], summit: ['Summit', '', '50 workouts', E => E.workouts >= 50], chalk: ['Chalk Cloud', '', '10 PRs', E => E.lifePR >= 10], founder: ['Founder', '', 'Founder only', () => !!st.set.founder]
});
/* ---------- sheet routing ---------- */
{
  const _s15 = sheet15;
  sheet15 = function(sh) {
    if (sh.type === 'profedit') return profEdit16(sh);
    if (sh.type === 'badges16') return badgesSheet(sh);
    if (sh.type === 'psec') return psecSheet(sh);
    if (sh.type === 'tour') return tourSheet(sh);
    return _s15(sh)
  }
}
{
  const _s14 = sheet14;
  sheet14 = function(sh) {
    if (sh.type === 'locker') return locker16(sh);
    return _s14(sh)
  }
}
const FWD = [() => click15, () => click14, () => crewClick, () => acctClick, () => click13];
async function sheetClick16(b) {
  const sh = st.sheet, d = b.dataset;
  if (!sh) return false;
  if (b.id === 'sheetx' && sh.back) {
    st.sheet = sh.back;
    renderSheet();
    return true
  }
  if (b.id === 'profedit' || b.id === 'profedit2') {
    const P = JSON.parse(JSON.stringify(st.set.prof || {}));
    st.sheet = {
      type: 'profedit', p: P, av: {...(st.set.av || {})}, flex: (st.set.flex || ['best', 'streak', 'vol']).slice(), rim: st.set.rim || ''
    };
    renderSheet();
    return true
  }
  if (sh.type === 'profedit') {
    if (d.pesec) {
      sh.sec = d.pesec;
      renderSheet();
      return true
    }
    if (b.id === 'pe16back' || b.id === 'pe16back2') {
      grab16(sh);
      sh.sec = null;
      renderSheet();
      return true
    }
    if (b.id === 'profsave') {grab16(sh)}
  }
  if (sh.type === 'locker') {
    if (d.lksec) {
      sh.sec = d.lksec;
      renderSheet();
      return true
    }
    if (b.id === 'lk16back' || b.id === 'lk16back2') {
      sh.sec = null;
      renderSheet();
      return true
    }
    if (b.id === 'editlook') {
      st.sheet = {type: 'look', back: sh};
      renderSheet();
      return true
    }
    if (d.tog === 'roasts' || d.tog === 'restJokes') {
      const k = d.tog;
      st.set[k] = st.set[k] === false;
      saveSet();
      b.setAttribute('aria-pressed', st.set[k] !== false);
      return true
    }
  }
  if (sh.type === 'badges16' && d.bcat) {
    sh.cat = d.bcat;
    renderSheet();
    return true
  }
  if (sh.type === 'tour') {
    if (b.id === 'tournext') {
      if ((sh.i || 0) >= TOUR.length - 1) {
        st.sheet = null;
        renderSheet();
        return true
      }
      sh.i = (sh.i || 0) + 1;
      renderSheet();
      return true
    }
    if (b.id === 'tourback') {
      sh.i = Math.max(0, (sh.i || 0) - 1);
      renderSheet();
      return true
    }
  }
  if (b.closest('.fwd')) {
    const prev = st.sheet;
    for (const f of FWD) {
      let fn;
      try {fn = f()} catch (e) {continue}if (typeof fn === 'function' && await fn(b)) {
        if (st.sheet && st.sheet !== prev && !st.sheet.back && ['badge', 'lad', 'pass'].includes(st.sheet.type)) st.sheet.back = prev;
        if (st.sheet) renderSheet();
        return true
      }
    }
  }
  return false
}
/* main-pane clicks for the profile tab */
async function click16(b) {
  const d = b.dataset;
  if (b.id === 'profedit2') {
    const P = JSON.parse(JSON.stringify(st.set.prof || {}));
    st.sheet = {
      type: 'profedit', p: P, av: {...(st.set.av || {})}, flex: (st.set.flex || ['best', 'streak', 'vol']).slice(), rim: st.set.rim || ''
    };
    renderSheet();
    return true
  }
  if (b.id === 'badges16') {
    st.sheet = {type: 'badges16', cat: 'mile'};
    renderSheet();
    return true
  }
  if (d.psec) {
    st.sheet = {type: 'psec', sec2: d.psec};
    renderSheet();
    return true
  }
  if (b.id === 'tourbtn') {
    st.sheet = {type: 'tour', i: 0};
    renderSheet();
    return true
  }
  return false
}
function settings16() {
  return `<div class="card rows15">${row16('tourbtn','👋','Take the app tour','How Overload works')}</div>`
}
function navSync16() {
  const b = $('t-me');
  if (b) b.setAttribute('aria-pressed', !!st.profile && !st.settings)
}
function boot16() {
  try {
    const me = $('t-me');
    if (me) me.addEventListener('click', () => {
      const again = st.profile && !st.settings;
      haptic('tap');
      st.profile = true;
      st.settings = false;
      st.confirm = null;
      st.picker = null;
      if (again || !paneCached()) render();
      else render();
      paneFade();
      window.scrollTo({top: 0})
    });
    const rb = $('rankbtn');
    if (rb) rb.style.display = 'none';
    setTimeout(tourCheck, 1400);
    // the founder’s crew wears the founder banner the first time it’s unlocked
    setTimeout(async() => {
      try {
        const c = st.cs && st.cs.crew;
        if (c && c.fcrew && isOwner() && c.banner !== 'founder' && !st.set.fban16) {
          st.set.fban16 = 1;
          saveSet();
          await crewCall('banner', {banner: 'founder'});
          render()
        }
      } catch (e) {}
    }, 5000);
  } catch (e) {console.warn('boot16', e)}
}
LOCK.bg.items.minimal[0] = 'Dim art';/* week strip: swipe to change weeks */
document.addEventListener('touchstart', e => {
  const r = e.target.closest && e.target.closest('.rack');
  if (r) {
    r._x = e.touches[0].clientX;
    r._y = e.touches[0].clientY
  }
}, {passive: true});
document.addEventListener('touchend', e => {
  const r = e.target.closest && e.target.closest('.rack');
  if (!r || r._x == null) return;
  const dx = e.changedTouches[0].clientX - r._x, dy = e.changedTouches[0].clientY - r._y;
  r._x = null;
  if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy) * 1.5) {
    const b = r.querySelector(`[data-wk="${dx<0?1:-1}"]`);
    if (b) b.click()
  }
}, {passive: true});/* ---------- clicks ---------- */
async function click17(b) {
  const d = b.dataset;
  if (b.id === 'tmore') {
    st.sheet = {type: 'tmore'};
    renderSheet();
    return true
  }
  if (b.id === 'chal17') {
    st.chal17 = !st.chal17;
    render();
    return true
  }
  if (d.rxo) {
    st.rx17 = st.rx17 === d.rxo ? null : d.rxo;
    render();
    return true
  }
  if (d.react && st.rx17) {setTimeout(() => {st.rx17 = null; render()}, 10)}
  if (d.arena17) {
    const a = d.arena17;
    if (a.startsWith('r_')) {
      st.sub.arena2 = 'rank';
      st.sub.arena = a.slice(2)
    } else st.sub.arena2 = a;
    render();
    return true
  }
  if (d.set17) {
    st.set17 = d.set17;
    render();
    window.scrollTo({top: 0});
    return true
  }
  if (b.id === 'set17back') {
    st.set17 = null;
    render();
    window.scrollTo({top: 0});
    return true
  }
  if (b.id === 'gear' || b.id === 'sclose') st.set17 = null;
  return false
}
async function sheetClick17(b) {
  const sh = st.sheet;
  if (!sh) return false;
  if (sh.type === 'tmore') {
    if (b.id === 't17grp') {
      st.sheet = {type: 'group'};
      renderSheet();
      return true
    }
    if (b.id === 't17exc') {
      st.sheet = {type: 'excuse', i: Math.floor(Math.random() * EX_A.length)};
      renderSheet();
      return true
    }
    if (b.id === 't17note') {
      st.sheet = null;
      renderSheet();
      setTimeout(() => {
        const n = $('snote');
        if (n) {
          n.scrollIntoView({behavior: 'smooth', block: 'center'});
          n.focus()
        }
      }, 80);
      return true
    }
  }
  return false
}
{
  const _s = sheet15;
  sheet15 = function(sh) {
    if (sh.type === 'tmore') return `<div class="ghd"><h2>Today</h2><button class="btn sm" id="sheetx">Done</button></div><div class="card rows15">${crewOn()?row16('t17grp','👥','Lift together','Train with your crew live'):''}${row16('t17exc','🎲','Need an excuse?','The generator, for emergencies')}${row16('t17note','📝','Session notes','Jump to today’s notes')}</div>`;
    return _s(sh)
  }
}
/* crew tab dot when the weekly recap is waiting; gear dot when a backup is due */
function dots17() {
  try {
    const dw = new Date().getDay(), rc = crewOn() && st.cs && [1, 2, 3].includes(dw) && st.set.recapSeen !== curWk();
    const c = $('t-crew');
    if (c && rc && !(st.tab === 'crew' && !st.profile && !st.settings)) c.classList.add('dot');
    const g = $('gear');
    if (g) g.classList.toggle('gdot', bkDue() && !st.set.acct && !st.settings)
  } catch (e) {}
}
{
  const _s14 = sheet14;
  sheet14 = function(sh) {
    if (sh.type === 'crest') return crestSheet18(sh);
    return _s14(sh)
  }
}

FIN.cofounder = ['Co-Founder 002', ['#0b0b0d', '#2a2a2e', '#000000'], 'Only the co-founder of Overload', () => !!st.set.cofounder, 'special'];
if (typeof PC === 'object' && !PC.cofounder) PC.cofounder = PC.founder;
LOCK.pbanner.items.cofounder = ['Co-Founder', '', 'Co-founder only', () => !!st.set.cofounder];
Object.assign(LOCK.pbanner.items, {
  vault: ['The Vault', '', 'Founder only', () => !!st.set.founder], fblue: ['The Blueprint', '', 'Founder only', () => !!st.set.founder], throne: ['The Throne', '', 'Founder only', () => !!st.set.founder]
});/* ---------- clicks ---------- */
async function click18(b) {
  const d = b.dataset;
  if (b.id === 'undo18b') {
    undoSet();
    return true
  }
  if (b.id === 'invite18') {
    await shareInvite();
    return true
  }
  if (b.id === 'privbtn') {
    st.sheet = {type: 'priv18'};
    renderSheet();
    return true
  }
  if (d.act === 'fill') {
    const [gi, ei] = d.key.split('-').map(Number), e = (todayGroups()[gi] || [])[ei];
    st.menu = null;
    if (e) fillFromLast([e.id]);
    return true
  }
  return false
}
async function sheetClick18(b) {
  const sh = st.sheet, d = b.dataset;
  if (!sh) return false;
  if (b.id === 't17fill') {
    st.sheet = null;
    renderSheet();
    fillFromLast();
    return true
  }
  if (b.id === 'invite18') {
    await shareInvite();
    return true
  }
  if (b.id === 'privbtn') {
    st.sheet = {type: 'priv18', back: sh};
    renderSheet();
    return true
  }
  if (b.id === 'fsave') {
    try {
      const r = await vaultCall('/founder', acctBody({code: (($('fcode') || {}).value || '').trim()}));
      if (r && r.cofounder) {
        st.set.cofounder = true;
        saveSet();
        st.sheet = null;
        renderSheet();
        confetti(120);
        toast('🥈 Welcome, Co-Founder')
      } else {
        st.set.founder = true;
        saveSet();
        st.sheet = null;
        renderSheet();
        confetti(140);
        toast('👑 Welcome, Founder')
      }
      syncCrew(true);
      render()
    } catch (e) {toast(e.message === 'wrong code' ? 'That code isn’t right' : 'Sign in first, then try again')}return true
  }
  if (sh.type === 'crest') {
    const c = sh.crest;
    if (d.crsec) {
      if (d.crsec === 'rib') {}sh.sec = d.crsec;
      renderSheet();
      return true
    }
    if (b.id === 'cr18back' || b.id === 'cr18back2') {
      const r = $('crrib');
      if (r) c.rib = r.value.trim().toUpperCase().slice(0, 26);
      sh.sec = null;
      renderSheet();
      return true
    }
    if (d.cr) {
      if (d.crlock) {
        toast('🔒 ' + d.crlock);
        return true
      }
      const i = d.cr.indexOf(':');
      c[d.cr.slice(0, i)] = d.cr.slice(i + 1);
      haptic('tap');
      renderSheet();
      return true
    }
    if (b.id === 'crsave18') {
      const r = $('crrib');
      if (r) c.rib = r.value.trim().toUpperCase().slice(0, 26);
      const n = crNorm(c);
      const res = await crewCall('crest', {crest: n});
      if (!res.error) {
        toast('Crest saved');
        st.sheet = null;
        renderSheet();
        render()
      }
      return true
    }
  }
  if (b.id === 'rep18') {
    st.sheet = {type: 'rep18', mid: d.mid, back: sh};
    renderSheet();
    return true
  }
  if (d.rep18) {
    const res = await crewCall('report', {target: sh.mid, why: d.rep18});
    if (!res || !res.error) {
      toast('Reported. Thanks for keeping the crew good');
      st.sheet = sh.back || null;
      renderSheet()
    }
    return true
  }
  if (b.id === 'blk18') {
    const B = st.set.blocked = st.set.blocked || [], i = B.indexOf(d.mid);
    if (i >= 0) {
      B.splice(i, 1);
      toast('Unblocked')
    } else {
      B.push(d.mid);
      toast('Blocked. You won’t see their posts or chat')
    }
    saveSet();
    renderSheet();
    render();
    return true
  }
  return false
}
{
  const _s = sheet15;
  sheet15 = function(sh) {
    if (sh.type === 'priv18') return privacyHTML();
    if (sh.type === 'rep18') {
      const m = memberOf2(sh.mid) || {name: 'this person'};
      return `<div class="ghd"><h2>Report ${esc(m.name)}</h2><button class="btn sm" id="sheetx">Cancel</button></div><p class="sub">Only your crew owner sees reports. They won’t know it was you unless the owner checks.</p><div class="card rows15">${REP_WHY.map(w=>row16(`data-rep18="${esc(w)}"`,'🚩',w,'')).join('')}</div>`
    }
    if (sh.type === 'invite18') {
      const {t} = inviteText();
      return `<div class="ghd"><h2>Invite friends</h2><button class="btn sm" id="sheetx">Done</button></div><p class="sub">Copy this and send it to your friends.</p><textarea class="field" readonly style="min-height:130px">${esc(t)}</textarea>`
    }
    let h = _s(sh);
    if (sh.type === 'tmore') h = h.replace('<div class="card rows15">', '<div class="card rows15">' + row16('t17fill', '↺', 'Same as last time', 'Fill every set from your last session'));
    return h
  }
}
{
  const _s16 = settings16;
  settings16 = function() {
    return _s16().replace('</div>', row16('privbtn', '🔒', 'Privacy', 'What’s saved and shared') + '</div>')
  }
}
function joinCheck18() {
  try {
    if (!st.join18) return;
    if (st.cs) {
      st.join18 = null;
      return
    }
    if (!st.plan) {
      setTimeout(joinCheck18, 1000);
      return
    }
    if (!st.set.tour16) {
      st.set.tour16 = 1;
      saveSet()
    }
    if (st.sheet && st.sheet.type === 'tour') {
      st.sheet = null;
      renderSheet()
    }
    st.tab = 'crew';
    st.profile = false;
    st.settings = false;
    render();
    setTimeout(() => {
      const i = $('crcode');
      if (i) {
        i.value = st.join18;
        toast('Invite code filled in. Tap Join')
      }
    }, 300)
  } catch (e) {}
}
{
  const _d = dots17;
  dots17 = function() {
    _d();
    try {
      if (st.join18) {
        if (st.cs) {
          st.join18 = null;
          return
        }
        setTimeout(() => {
          const i = $('crcode');
          if (i && !i.value) i.value = st.join18 || ''
        }, 0)
      }
    } catch (e) {}
  }
}
function boot18() {
  try {
    if (!$('undo18')) {
      const u = document.createElement('div');
      u.id = 'undo18';
      u.className = 'undo18';
      document.body.appendChild(u);
      u.addEventListener('click', e => {if (e.target.id === 'undo18b') undoSet()})
    }
    const q = new URLSearchParams(location.search), j = q.get('join');
    if (j) {
      history.replaceState(null, '', location.pathname);
      st.join18 = j.toUpperCase().replace(/[^A-Z0-9-]/g, '').slice(0, 16);
      joinCheck18()
    }
  } catch (e) {console.warn('boot18', e)}
}
/* ---------- plates: Easter is new; every holiday plate is animated ---------- */
FIN.h_easter = ['Easter', ['#e98cbf', '#fde4f1', '#8f4a74'], 'Lift on Easter', () => holHas('easter'), 'holiday'];
Object.entries(HOL19).forEach(([k, H]) => {if (FIN[H.p]) FIN[H.p][3] = () => holHas(k)});
try {if (PC && !PC.h_easter) PC.h_easter = FIN.h_easter[1]} catch (e) {}
{
  const _p = plateSVG;
  plateSVG = function(c, state, o) {
    let s = _p(c, state, o);
    const k = PLATE2HOL[c];
    if (!k || state === 'idle' || state === 'rest' || typeof s !== 'string') return s;
    const i = s.lastIndexOf('</svg>');
    if (i < 0) return s;
    let yr = '';
    if (o && o.big) {
      const Y = holYears(k);
      if (Y.length) yr = `<text x="50" y="80" text-anchor="middle" style="font:900 6px Archivo,'Inter Display',Inter,sans-serif;letter-spacing:1.5px" fill="#fff" opacity=".85">${Y[Y.length-1]}</text>`
    }
    return s.slice(0, i) + holFx(k, (pid++) + c, o && o.big) + yr + s.slice(i)
  }
}

Object.entries(HOL19).forEach(([k, H]) => {
  XSC['hb_' + k] = [H.n, 'Holiday'];
  LOCK.pbanner.items['hb_' + k] = [H.n, '', 'Lift on ' + H.n, () => holHas(k)];
  SCENES['hb_' + k] = [H.n, '3 crew lifts on ' + H.n, {hol: k}];
  LOCK.nameFx.items['h' + k] = [H.n, '', 'Lift on ' + H.n, () => holHas(k)];
  LOCK.react.items['h' + k] = [H.e, '', 'Lift on ' + H.n, () => holHas(k)];
  RE['h' + k] = H.e;
  LOCK.setfx.items['h' + k] = [H.n + ' burst', '', 'Lift on ' + H.n, () => holHas(k)]
});
{
  const _x = xScene;
  xScene = function(k, uid) {
    if (String(k).startsWith('hb_') && HOL19[k.slice(3)]) return holScene(k.slice(3), uid);
    return _x(k, uid)
  }
}
{
  const _ok = sceneOK;
  sceneOK = function(k) {
    if (String(k).startsWith('hb_')) return !!(st.cs && st.cs.crew && (st.cs.crew.holU || {})[k.slice(3)]);
    return _ok(k)
  }
}
/* the crew's seasonal crest touch */
{
  const _c = crestSVG;
  crestSVG = function(cr, rank, size) {
    let s = _c(cr, rank, size);
    try {
      const c = st.cs && st.cs.crew;
      if (!c || cr !== c.crest) return s;
      const U = c.holU || {}, t = new Date(), k = Object.keys(HOL19).find(k => U[k] && HOL19[k].w(t));
      if (!k) return s;
      const small = (size || 52) < 48, E = k === 'xmas' ? '🎅' : HOL19[k].e, i = s.lastIndexOf('</svg>');
      return s.slice(0, i) + `<text x="${small?30:34}" y="${small?-36:-38}" text-anchor="middle" font-size="${small?24:26}">${E}</text>` + s.slice(i)
    } catch (e) {return s}
  }
}

{
  const _s = sheet15;
  sheet15 = function(sh) {
    if (sh.type === 'hol19') return holSheet(sh);
    return _s(sh)
  }
}

{
  const _d = dots17;
  dots17 = function() {
    _d();
    try {
      const s = cur && cur();
      if (s && s.start && !s.end) return;
      clearTimeout(window.__hr19);
      window.__hr19 = setTimeout(holReveal19, 900);
      if (st.cs && !window.__hc19) {
        window.__hc19 = 1;
        setTimeout(() => {
          window.__hc19 = 0;
          holCheck19()
        }, 1500)
      }
    } catch (e) {}
  }
}

['vault', 'fblue', 'throne'].forEach(k => {
  delete LOCK.pbanner.items[k];
  delete XSC[k]
});
Object.assign(XSC, {
  feclipse: ['Gold Eclipse', 'Founder only'], fconst: ['The Constellation', 'Founder only'], fgalaxy: ['Golden Galaxy', 'Founder only']
});
Object.assign(LOCK.pbanner.items, {
  feclipse: ['Gold Eclipse', '', 'Founder only', () => !!st.set.founder], fconst: ['The Constellation', '', 'Founder only', () => !!st.set.founder], fgalaxy: ['Golden Galaxy', '', 'Founder only', () => !!st.set.founder]
});
{
  const _x = xScene;
  xScene = function(k, uid) {
    if (OLDF20[k]) k = OLDF20[k];
    if (k === 'feclipse' || k === 'fconst' || k === 'fgalaxy') return spaceScene(k, uid);
    let s = _x(k, uid);
    if (k === 'founder' || k === 'cofounder') s = s.replace(/<text x="190" y="123"[\s\S]*?<\/text>/, '');
    return s
  }
}

{
  const _r = renderProfile16;
  renderProfile16 = function() {
    let h = _r();
    try {
      const E = engine(), show = (st.set.showcase || []).map(k => E.B.find(x => x.k === k) || E.R.find(x => x.k === k)).filter(Boolean);
      h = h.replace(/<div class="ph20pins">[\s\S]*?<\/div><\/div>/, shelf21(show) + '</div>');
      const T = st.set.tap21 || {};
      h = h.replace('<div class="ph16ban">', `<div class="ph16ban tap21" data-tap21="ban" role="button" tabindex="0" aria-label="Change banner">${T.ban?'':'<span class="dot21">✎</span>'}`);
      h = h.replace('<div class="ph16ban ph16plain">', `<div class="ph16ban ph16plain tap21" data-tap21="ban" role="button" tabindex="0" aria-label="Add a banner">${T.ban?'':'<span class="dot21">✎</span>'}`);
      h = h.replace('<span class="ph16av">', `<span class="ph16av tap21" data-tap21="av" role="button" tabindex="0" aria-label="Change plate and avatar">${T.av?'':'<span class="dot21 av">✎</span>'}`);
    } catch (e) {}return h
  }
}

/* ---------- sheets: medals in badge, pin picker, crew member ---------- */
async function sheetClick21(b) {
  const sh = st.sheet, d = b.dataset;
  if (!sh) return false;
  if (sh.type === 'profedit' && sh.direct && (b.id === 'pe16back2' || b.id === 'pe16back')) {
    commit21(sh);
    return true
  }
  if (sh.type === 'profedit' && sh.direct && b.id === 'sheetx') {
    st.sheet = null;
    renderSheet();
    return true
  }
  if (sh.type === 'pinpick' && d.tpin21) {
    const P = st.set.prof = st.set.prof || {}, a = P.tpins = P.tpins || [], i = a.indexOf(d.tpin21);
    if (i >= 0) a.splice(i, 1);
    else {
      if (a.length >= 3) a.shift();
      a.push(d.tpin21)
    }
    saveSet();
    renderSheet();
    try {syncCrew(true)} catch (e) {}return true
  }
  if (sh.type === 'badge' && b.id === 'pinswap21') {
    st.sheet = {type: 'pinpick', back: sh};
    renderSheet();
    return true
  }
  return false
}
function patch21() {
  const sh = st.sheet, el = $('sheet');
  if (!sh || !el) return;
  const box = el.querySelector('.sheet');
  if (!box) return;
  const E = engine();
  if (sh.type === 'badge') {
    const b = E.B.find(x => x.k === sh.k) || E.R.find(x => x.k === sh.k);
    const big = box.querySelector('div[style*="font-size:64px"]');
    if (b && big) {
      const earned = b.earned || b.count > 0;
      big.removeAttribute('style');
      big.className = 'md21big';
      big.innerHTML = b.hid && !earned ? medal21('zz', {emoji: '❔', size: 130, dim: true}) : badgeMedal21(b, 130, !earned);
      const rar = box.querySelector('.rar');
      if (rar && earned) {
        const t = document.createElement('div');
        t.className = 'md21tier';
        t.textContent = tierName21(b.k, b.name, b.count) + ' medal';
        rar.parentNode.insertBefore(t, rar)
      }
      const pin = box.querySelector('#pinb');
      if (pin && (st.set.showcase || []).length >= 3 && !(st.set.showcase || []).includes(b.k)) {
        const s = document.createElement('p');
        s.className = 'sub';
        s.style.textAlign = 'center';
        s.textContent = 'Your shelf holds 3. Pinning this replaces the oldest one.';
        pin.parentNode.insertBefore(s, pin)
      }
    }
  }
  if (sh.type === 'pinpick') {
    box.querySelectorAll('.pinrow[data-pinb2]').forEach(r => {
      const k = r.dataset.pinb2, b = E.B.find(x => x.k === k) || E.R.find(x => x.k === k), pi = r.querySelector('.pi');
      if (b && pi) {
        pi.innerHTML = badgeMedal21(b, 38);
        pi.classList.add('pi21')
      }
    });
    if (!box.querySelector('.tp21')) {
      const earned = trophies().filter(t => t.earned), tp = (st.set.prof && st.set.prof.tpins) || [];
      const w = document.createElement('div');
      w.className = 'tp21';
      w.innerHTML = `<h3 style="margin:18px 0 4px">Trophies</h3><p class="sub">Up to 3 show on your crew card. ${tp.length}/3 pinned.</p>${earned.length?`<div class="lkrow lk16">${earned.map(t=>`<button class="lkc${tp.includes(t.k)?' on':''}" data-tpin21="${esc(t.k)}"><span class="lki2">${trophySVG(t)}</span>${esc(t.name.slice(0,18))}</button>`).join('')}</div>`:'<p class="sub">Win trophies with your crew and pin them here.</p>'}`;
      box.appendChild(w)
    }
    const h2 = box.querySelector('.ghd h2');
    if (h2) h2.textContent = 'Your shelf'
  }
  if (sh.type === 'member') {
    const m = (typeof memberOf2 === 'function' ? memberOf2 : memberOf)(sh.id), p = m && m.profile;
    const sc = box.querySelector('.showc');
    if (sc && p && p.show) {
      sc.className = 'sh21 mem';
      sc.innerHTML = `<div class="sh21row">${p.show.map(x=>`<div class="md21"><span class="md21i">${medal21(x[2]||'',{name:x[1],count:x[3],emoji:x[0],size:72})}</span><b>${esc(x[1])}</b>${x[2]?`<i>${tierName21(x[2],x[1],x[3])}</i>`:''}</div>`).join('')}</div><div class="sh21ledge"></div>`
    }
  }
  if (sh.type === 'crinfo' || box.querySelector('#bwin')) {
    const bw = box.querySelector('#bwin');
    if (bw) {
      const t = bw.closest('.tog');
      if (t) t.style.display = 'none'
    }
  }
}
{
  const _rs = renderSheet;
  renderSheet = function() {
    const r = _rs.apply(this, arguments);
    try {patch21()} catch (e) {}return r
  }
}
{
  const _pp = profPage;
  profPage = function(id) {
    const h = _pp(id);
    try {
      if (!h) return h;
      const m = memberData(id), sh = (m && m.profile && m.profile.show) || [];
      if (!sh.length) return h;
      const shelf = `<div class="sh21 mem"><div class="sh21row">${sh.slice(0,3).map(x=>`<div class="md21"><span class="md21i">${medal21(x[2]||'',{name:x[1],count:x[3],emoji:x[0],size:72})}</span><b>${esc(x[1])}</b>${x[2]?`<i>${tierName21(x[2],x[1],x[3])}</i>`:''}</div>`).join('')}</div><div class="sh21ledge"></div></div>`;
      return h.replace('<div class="ppresp">', shelf + '<div class="ppresp">')
    } catch (e) {return h}
  }
}
{
  const _lt = ladderTile;
  ladderTile = function(cat) {
    const h = _lt(cat);
    try {
      const E = engine(), list = E.B.filter(b => b.cat === cat), n = list.filter(b => b.earned).length, b = n ? list[n - 1] : list[0];
      if (!b) return h;
      return h.replace(/<div class="bi">[\s\S]*?<\/div>/, `<div class="bi bi21">${badgeMedal21(b,54,!n)}</div>`)
    } catch (e) {return h}
  }
}
{
  const _bt = badgeTile;
  badgeTile = function(b, on) {
    const h = _bt(b, on);
    try {
      const earned = on || b.earned || b.count > 0;
      return h.replace(/<div class="bi">[\s\S]*?<\/div>/, `<div class="bi bi21">${b.hid&&!earned?medal21('zz',{emoji:'❔',size:46,dim:true}):badgeMedal21(b,54,!earned)}</div>`)
    } catch (e) {return h}
  }
}
{
  const _mp = myProfile;
  myProfile = function() {
    const p = _mp();
    try {
      const E = engine();
      p.show = (st.set.showcase || []).map(k => {
        const b = E.B.find(x => x.k === k) || E.R.find(x => x.k === k);
        return b ? [b.icon, b.name, b.k, b.count || 0] : null
      }).filter(Boolean)
    } catch (e) {}return p
  }
}
function boot21() {
  document.addEventListener('click', e => {
    const t = e.target.closest('[data-tap21]');
    if (t && !e.target.closest('button')) {tap21(t.dataset.tap21)}
  });
  document.addEventListener('keydown', e => {
    if ((e.key === 'Enter' || e.key === ' ') && e.target.dataset && e.target.dataset.tap21) {
      e.preventDefault();
      tap21(e.target.dataset.tap21)
    }
  });
  const u = $('undo18');
  if (u) u.addEventListener('click', e => {if (e.target.id === 'undo21b') doUndo21()})
}
{
  const _s = sheet15;
  sheet15 = function(sh) {
    if (sh.type === 'rk22') return rk22(sh);
    return _s(sh)
  }
}

/* next rank bar on the crew banner */
{
  const _h = crewHead14;
  crewHead14 = function() {
    let h = _h();
    try {
      const R = crewRankInfo();
      if (!R.nx) return h;
      const bar = `<button class="nr22${crewNear22()?' near':''}" id="nr22"><span class="nr22t">Next: <b>${RANKS[R.r+1][0]}</b></span><span class="nr22b"><u style="width:${Math.round(R.frac*100)}%"></u></span><span class="nr22n">${(R.nx-R.pts).toLocaleString()} pts · How?</span></button>`;
      const i = h.lastIndexOf('</div>');
      return h.slice(0, i) + bar + h.slice(i)
    } catch (e) {return h}
  }
}

{
  const _s = sheet15;
  sheet15 = function(sh) {
    if (sh.type === 'ai23') return aiSheet23(sh);
    return _s(sh)
  }
}

{
  const _rs = renderSheet;
  renderSheet = function() {
    const r = _rs.apply(this, arguments);
    try {patch23()} catch (e) {}return r
  }
}
/* settings: one row, its own page */
{
  const _set = renderSettings;
  renderSettings = function() {
    if (st.set17 === 'ai') {
      const on = st.set.aiOk && st.set.aiCoach !== false;
      return `<div class="ghd"><button class="btn sm" id="set17back">‹ Settings</button><h2 style="margin:0">Overload AI</h2><span></span></div>
 <div class="card"><div class="ai23set">${ORB23}<div><b>${aiOnNow()?'Overload AI is on':'Overload AI is off right now'}</b><div class="sub">Builds plans from your notes and writes short coach notes. Powered by Anthropic’s Claude.</div></div></div>
 ${tog('aiCoach23',on,'Coach notes','A short recap after each workout and a note in your Monday report.')}
 <p class="sub" style="margin-top:10px">${st.set.aiOk?'You allowed AI on this phone. Turning coach notes off stops all automatic AI. The plan builder always asks you first.':'You haven’t allowed AI yet. The plan builder will ask the first time you use it.'}</p>
 <p class="sub">Sent only when you use it: your photos or typed lifts, lift names and sets. Never your name, crew, chat or account. Limits: 3 plan builds a day, 1 recap per workout, 1 note a week.</p>
 ${aiOnNow()?'<button class="cta23" id="ai23open" style="margin-top:8px">✦ Build my plan with AI</button>':''}</div>`
    }
    const h = _set.apply(this, arguments);
    if (st.set17) return h;
    return h.replace(/(<button[^>]*data-set17="data")/, `${row16('data-set17="ai"','✦','Overload AI',st.set.aiOk&&st.set.aiCoach!==false?'On':'Off')}$1`)
  }
}

{
  const _s = sheet15;
  sheet15 = function(sh) {
    if (sh.type === 'pex24') return ed24(sh);
    return _s(sh)
  }
}

{
  let last24 = null;
  const _rs = renderSheet;
  renderSheet = function() {
    const was = last24;
    last24 = st.sheet && st.sheet.type;
    const r = _rs.apply(this, arguments);
    if (was === 'pex24' && !st.sheet) try {render()} catch (e) {}return r
  }
}

{
  const _s = sheet15;
  sheet15 = function(sh) {
    if (sh.type === 'coach25') return coach25(sh);
    if (sh.type === 'cue25') return cueSheet25(sh);
    return _s(sh)
  }
}
{
  const _s = sheet15;
  sheet15 = function(sh) {
    let h = _s(sh);
    if (sh.type === 'tmore' && aiOnNow()) h = h.replace('<div class="card rows15">', '<div class="card rows15">' + row16('t25coach', '✦', 'Ask Coach', 'Swaps, short on time, questions'));
    if (sh.type === 'pex24' && aiOnNow()) h = h.replace('<div class="act24">', '<div class="act24"><button id="ed24tips" class="tip24">✦ <span>Form tips</span></button>');
    return h
  }
}

/* settings page wording */
{
  const _set = renderSettings;
  renderSettings = function() {
    let h = _set.apply(this, arguments);
    if (st.set17 !== 'ai') return h;
    return h.replace('Limits: 3 plan builds a day, 1 recap per workout, 1 note a week.', 'Daily limits per person: 25 Coach messages, 4 plan builds, 15 new form tips, 1 recap per workout, 1 note a week. There is also a hard monthly budget for the whole app.').replace('Builds plans from your notes and writes short coach notes.', 'Coach chat, plan builder, form tips, workout recaps and a weekly crew post.') + (AI23.rest ? '<div class="warn23" style="margin-top:10px">This month’s AI budget is used up. Everything AI is back on the 1st.</div>' : '')
  }
}

/* ---------- 0.13: click handling ---------- */
async function click13(b) {
  const d = b.dataset;
  if (d.smart) {
    const k = d.smart;
    if (k === 'post') {
      const s = lastFinished();
      if (s) {
        st.sheet = {type: 'post', key: s.date + '_' + s.day};
        renderSheet()
      }
      return true
    }
    if (k === 'rank') {
      st.profile = true;
      st.settings = false;
      render();
      window.scrollTo({top: 0});
      return true
    }
    if (k === 'crew') {
      st.tab = 'crew';
      st.sub.crew = 'feed';
      render();
      window.scrollTo({top: 0});
      return true
    }
    if (k === 'stats') {
      st.tab = 'stats';
      st.sub.stats = 'ov';
      render();
      window.scrollTo({top: 0});
      return true
    }
    return true
  }
  if (d.hype) {
    haptic('success');
    const ok = wsSend({t: 'hype', to: d.hype});
    b.classList.remove('sent');
    void b.offsetWidth;
    b.classList.add('sent');
    toast(ok ? `Hype sent to ${(memberOf(d.hype)||{}).name||'them'} 🔥` : 'Hype will send when you’re back online');
    return true
  }
  if (b.id === 'bellbtn') {
    st.sheet = {type: 'bell'};
    renderSheet();
    st.set.bellSeen = Date.now();
    saveSet();
    updateBellDot();
    setTimeout(() => {if (st.tab === 'crew') render()}, 50);
    return true
  }
  if (b.id === 'recapx') {
    st.set.recapSeen = curWk();
    saveSet();
    render();
    return true
  }
  if (b.id === 'openawards') {
    st.sheet = {type: 'awards'};
    renderSheet();
    return true
  }
  if (b.id === 'goaladd') {
    st.sheet = {type: 'goal'};
    renderSheet();
    return true
  }
  if (d.goaldel !== undefined) {
    const i = +d.goaldel;
    if (st.confirm !== 'G' + i) {
      st.confirm = 'G' + i;
      toast('Tap ✕ again to remove this goal');
      return true
    }
    st.confirm = null;
    st.set.goals.splice(i, 1);
    saveSet();
    render();
    return true
  }
  if (b.id === 'bwsave') {
    const v = parseFloat(($('bwin2') || {}).value);
    if (!(v > 40 && v < 1000)) {
      toast('Enter your bodyweight');
      return true
    }
    st.set.bw = v;
    saveSet();
    render();
    syncCrew(true);
    return true
  }
  if (d.csnap) {
    const k = 'c' + d.csnap;
    if (st.confirm !== k) {
      st.confirm = k;
      render();
      return true
    }
    st.confirm = null;
    try {
      const r = await vaultCall('/snap', acctBody({day: d.csnap}));
      const o = JSON.parse(r.data);
      const sess = {};
      Object.entries(o.sessions || {}).forEach(([k2, v]) => {if (!v._del) sess[k2] = v});
      restoreReplace(JSON.stringify({
        sessions: sess, plan: o.plan ? stripS(o.plan) : null, notes: o.notes && o.notes.list || st.notes, settings: o.settings ? (() => {
          const x = stripS(o.settings);
          delete x._v;
          return x
        })() : null
      }));
      toast('Restored your data from ' + d.csnap);
      render()
    } catch (e) {toast('Could not load that cloud copy')}return true
  }
  return false
}
function sheet13(sh) {
  const me = myId();
  if (sh.type === 'bell') {
    const L = bellItems();
    return `<div class="ghd"><h2>Activity</h2><button class="btn sm" id="sheetx">Done</button></div>${L.length?L.map(x=>`<div class="bellrow${x.ts>(sh.seen||0)?'':''}"><span class="bi2">${x.icon}</span><span class="btx">${x.text}<span class="sub">${ago(x.ts)}</span></span></div>`).join(''):'<p class="sub">Nothing yet. Reactions, comments, titles and rank-ups show up here.</p>'}`
  }
  if (sh.type === 'awards') return awardsSheet();
  if (sh.type === 'steal') return stealSheet(sh);
  if (sh.type === 'goal') return goalSheet();
  return null
}
async function sheetClick13(b) {
  const d = b.dataset, sh = st.sheet;
  if (!sh) return false;
  if (d.vote) {
    const[cat, t] = d.vote.split(':');
    await crewCall('vote', {month: monthKey(), cat, target: t});
    haptic('tap');
    renderSheet();
    return true
  }
  if (d.steal) {
    const[mid, day] = d.steal.split(':');
    st.sheet = {type: 'steal', id: mid, day: day === 'all' ? 'all' : +day};
    renderSheet();
    return true
  }
  if (d.stealto !== undefined) {
    const m = memberOf(sh.id), src = m && m.profile && m.profile.plan && m.profile.plan.days[sh.day];
    if (src) {
      await IDB.put('planbackup', {at: Date.now(), plan: st.plan});
      st.plan.days[+d.stealto] = clone(src);
      saveP();
      st.sheet = null;
      renderSheet();
      render();
      toast(`Copied ${src.name} to ${DN[+d.stealto]} 📋`);
      haptic('success')
    }
    return true
  }
  if (b.id === 'stealallgo') {
    const m = memberOf(sh.id), pl = m && m.profile && m.profile.plan;
    if (pl && pl.days && pl.days.length === 7) {
      await IDB.put('planbackup', {at: Date.now(), plan: st.plan});
      st.plan.days = clone(pl.days).map(x => ({...x, groups: x.groups || []}));
      saveP();
      st.sheet = null;
      renderSheet();
      st.tab = 'plan';
      st.sub.plan = 'plan';
      render();
      toast(`You’re running ${m.name}’s split now 📋`);
      haptic('success')
    }
    return true
  }
  if (b.id === 'goalsave') {
    const ex = $('gex').value, w = parseFloat($('gw').value), dt = $('gd').value;
    if (!ex || !(w > 0) || !dt) {
      toast('Pick a lift, a weight and a date');
      return true
    }
    if (dt <= today()) {
      toast('Pick a date in the future');
      return true
    }(st.set.goals = st.set.goals || []).push({ex, w, date: dt, created: today()});
    saveSet();
    st.sheet = null;
    renderSheet();
    render();
    toast('Goal set 🎯');
    return true
  }
  if (d.banner) {
    if (b.classList.contains('lockd')) {
      toast('Your crew unlocks that banner at a higher level');
      return true
    }
    await crewCall('banner', {banner: d.banner});
    renderSheet();
    render();
    return true
  }
  return false
}
