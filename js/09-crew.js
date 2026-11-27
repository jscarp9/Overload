/* ========================================================================
   Overload: Crew

   Crew feed, posts, chat, live crew, arena, raids, lifting together, awards.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

/* ---------- 0.12: crew helpers ---------- */
const toMine = (v, unit) => {
  if (!unit || unit === U()) return v;
  return unit === 'kg' ? v * 2.2046 : v / 2.2046
};
const crewOn = () => !!(st.set.crew && st.set.crew.code);
const myId = () => crewOn() ? crewIdentity().id : null;
const memberOf = id => st.cs && st.cs.members.find(m => m.id === id);
const hue = s => {
  let h = 0;
  for (const c of String(s)) h = (h * 31 + c.charCodeAt(0)) % 360;
  return h
};
const initials = n => String(n || '?').trim().split(/\s+/).map(w => w[0]).join('').slice(0, 2).toUpperCase();
function ago(ts) {
  const s = (Date.now() - ts) / 1000;
  if (s < 60) return 'now';
  if (s < 3600) return Math.floor(s / 60) + 'm';
  if (s < 86400) return Math.floor(s / 3600) + 'h';
  if (s < 604800) return Math.floor(s / 86400) + 'd';
  const d = new Date(ts);
  return MON[d.getMonth()] + ' ' + d.getDate()
}
function avatar(m, sz) {
  const p = m && m.profile || {}, h = hue(m && m.id || 'x'), s = sz || 40;
  return `<span class="av fr fr-${esc(p.frame||'none')} au-${esc(p.aura||'none')}" style="--s:${s}px">${avInner(m)}</span>`
}
function rankChip(p) {
  if (!p) return '';
  return `<span class="rchip">${emblem(Math.max(0,Math.min(7,p.rank||0)))}<span>${esc(p.rname||RANKS[p.rank||0][0])}</span></span>`
}
function crewIdentity() {
  if (!st.set.crewMe) {
    const r = () => Math.random().toString(36).slice(2);
    st.set.crewMe = {id: 'm' + r() + r(), key: r() + r() + r() + r()};
    saveSet()
  }
  return st.set.crewMe
}
let crewBusy = false, crewLast = 0, pubLast = 0, pubHash = '', crewTimer = null;
async function crewCall(p, body) {
  const me = crewIdentity(), c = st.set.crew || {};
  try {
    const r = await fetch(PUSH_URL + '/crew/' + p, {
      method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({code: c.code, id: me.id, key: me.key, member: st.set.name || 'Lifter', ...body})
    });
    let j = {};
    try {j = await r.json()} catch (x) {}
    if (((r.status === 403 && j.error === 'auth') || (r.status === 404 && j.error === 'no crew')) && crewOn() && p !== 'join' && p !== 'create') {
      st.set.crew = null;
      st.cs = null;
      saveSet();
      store('overload-crewcache', null);
      toast('You’re no longer in that crew');
      render();
      return {error: 'gone'}
    }
    if (!r.ok) return {error: j.error || ('HTTP ' + r.status)};
    if (j.crew) {
      st.cs = j;
      store('overload-crewcache', j);
      crewCheck()
    }
    return j
  } catch (e) {return {error: 'offline'}}
}
async function syncCrew(force) {
  if (!crewOn() || crewBusy || !st.plan) return;
  if (!force && Date.now() - crewLast < 15000) return;
  crewBusy = true;
  crewLast = Date.now();
  const prof = myProfile(), h = JSON.stringify(prof);
  const due = h !== pubHash || Date.now() - pubLast > 6e5;
  const r = due ? await crewCall('profile', {
    profile: prof, acct: st.set.acct && st.set.acct.id, atoken: st.set.acct && st.set.acct.token, today: today()
  }) : await crewCall('state', {});
  if (due && !r.error) {
    pubHash = h;
    pubLast = Date.now()
  }
  crewBusy = false;
  if (!r.error && st.tab === 'crew' && !st.settings && !st.profile && !document.querySelector('#main textarea:focus,#main input:focus')) render()
}
function crewLoop() {
  clearInterval(crewTimer);
  crewTimer = setInterval(() => {if (!document.hidden && st.tab === 'crew' && !st.profile && !st.settings) syncCrew()}, 30000)
}
function saveCrewId() {
  IDB.put('crewid', crewOn() ? {url: PUSH_URL, code: st.set.crew.code, id: crewIdentity().id, key: crewIdentity().key} : null)
}
function weekStats(r) {
  const rs = realSessions(), ss = rs.filter(s => s.date >= r[0] && s.date <= r[6]);
  return {
    wk: r[0], days: new Set(ss.map(s => s.date)).size, sets: ss.reduce((a, s) => a + sSets(s), 0), vol: Math.round(ss.reduce((a, s) => a + sVol(s), 0)), prs: ss.reduce((a, s) => a + sessStats(s).prs.length, 0), early: ss.filter(s => hrOf(s) !== null && hrOf(s) < 7).length
  }
}
function myProfile() {
  const E = engine(), wd = weekDates(today()), pw = weekDates(iso(new Date(toD(wd[0]).getTime() - 7 * 864e5))), rs = realSessions();
  const lifts = {};
  rs.forEach(s => Object.entries(s.ex).forEach(([id, a]) => (a || []).forEach(x => {
    if (!full(x)) return;
    const n = exName(id), v = e1(x);
    if (!lifts[n] || v > lifts[n].e) lifts[n] = {e: Math.round(v), w: +x.w, r: +x.r}
  })));
  const top = Object.entries(lifts).sort((a, b) => b[1].e - a[1].e).slice(0, 30);
  return {
    rank: E.rank, div: E.div, pres: E.prestige, rname: E.rname, power: E.power, streak: E.streak, bcount: E.bcount, active: E.active, workouts: E.workouts, lifeVol: Math.round(E.lifeVol), unit: U(), since: E.since, frac: Math.round(E.frac * 100) / 100,
    keys: E.B.filter(b => b.earned).map(b => b.k), show: (st.set.showcase || []).map(k => {
      const b = E.B.find(x => x.k === k) || E.R.find(x => x.k === k);
      return b ? [b.icon, b.name] : null
    }).filter(Boolean),
    week: weekStats(wd), last: weekStats(pw), prev: weekStats(weekDates(iso(new Date(toD(wd[0]).getTime() - 14 * 864e5)))), plan: st.plan ? {
      days: st.plan.days.map(d => ({name: d.name, type: d.type, color: d.color, rest: d.rest, groups: d.groups}))
    } : null, days: [...new Set(rs.map(s => s.date))].slice(-21), lifts: Object.fromEntries(top), bw: +st.set.bw || 0, lvl: E.lvl, frame: st.set.frame || '', aura: st.set.aura || '', tag: st.set.tag || '', trial: E.trial ? 1 : 0
  }
}
const curWk = () => weekDates(today())[0];
const wkOf = p => p && p.week && p.week.wk === curWk() ? p.week : {days: 0, sets: 0, vol: 0, prs: 0, early: 0};
const lastOf = p => {
  if (!p) return null;
  const lw = weekDates(iso(new Date(toD(curWk()).getTime() - 7 * 864e5)))[0];
  return p.week && p.week.wk === lw ? p.week : p.last && p.last.wk === lw ? p.last : null
};
const weekPts = p => {
  const w = wkOf(p);
  return w.days * 40 + w.sets * 3 + w.prs * 20
};
const TITLES = [['machine', 'The Machine', '⚙️', 'Most sets this week', p => wkOf(p).sets], ['hunter', 'PR Hunter', '🏹', 'Most PRs this week', p => wkOf(p).prs], ['king', 'Volume King', '👑', 'Most volume this week', p => toMine(wkOf(p).vol, p.unit)], ['will', 'Iron Will', '🛡️', 'Longest week streak', p => p.streak || 0], ['bird', 'Early Bird', '🐓', 'Most workouts before 7 AM this week', p => wkOf(p).early], ['steady', 'Mr. Consistent', '📅', 'Most training days this week', p => wkOf(p).days]];
function titleHolders() {
  const out = {};
  if (!st.cs) return out;
  const mem = st.cs.members.filter(m => m.profile);
  if (mem.length < 2) return out;
  TITLES.forEach(([k, , , , f]) => {
    let best = null, bv = 0;
    mem.forEach(m => {
      const v = f(m.profile);
      if (v > bv || (v === bv && v > 0 && best && (m.profile.power || 0) > (best.profile.power || 0))) {
        bv = v;
        best = m
      }
    });
    if (best && bv > 0) out[k] = best.id
  });
  return out
}
function h2h(a, b) {
  const A = a, B2 = b;
  if (!A || !B2) return[0, 0];
  let w = 0, l = 0;
  [['days'], ['sets'], ['vol'], ['prs']].forEach(([k]) => {
    const x = k === 'vol' ? toMine(A.vol, A.unit) : A[k], y = k === 'vol' ? toMine(B2.vol, B2.unit) : B2[k];
    if (x > y) w++;
    else if (y > x) l++
  });
  return[w, l]
}
function chalProgress() {
  const c = st.cs && st.cs.crew.challenge;
  if (!c || c.wk !== curWk()) return null;
  const sum = st.cs.members.reduce((a, m) => {
    if (!m.profile) return a;
    const w = wkOf(m.profile);
    return a + (c.type === 'vol' ? toMine(w.vol, m.profile.unit) : w[c.type] || 0)
  }, 0);
  return {...c, cur: Math.round(sum)}
}
function crewCheck() {
  if (!st.cs) return;
  const cb = st.set.crewB = st.set.crewB || {}, d = today(), mem = st.cs.members, me = myId();
  let ch = false;
  const give = k => {if (!cb[k]) {cb[k] = d; ch = true}};
  if (mem.length >= 2) {
    const sets = mem.map(m => new Set(m.profile && m.profile.days || []));
    if (sets.every(s => s.size) && [...sets[0]].some(x => sets.every(s => s.has(x)))) give('squad')
  }
  const lifeV = mem.reduce((a, m) => a + (m.profile ? toMine(m.profile.lifeVol || 0, m.profile.unit) : 0), 0);
  if (lifeV >= (U() === 'kg' ? 450000 : 1e6)) give('cmil');
  if ((st.set.reactN || 0) >= 50) give('hype');
  if (st.cs.crew.owner === me && mem.length >= 2) give('recruit');
  const cp = chalProgress();
  if (cp && cp.cur >= cp.target) give('team');
  const riv = memberOf(st.set.rival), mine = memberOf(me);
  if (riv && mine && mine.profile && riv.profile) {
    const a = lastOf(mine.profile), b = lastOf(riv.profile);
    if (a && b) {
      const[w, l] = h2h({...a, unit: mine.profile.unit}, {...b, unit: riv.profile.unit});
      if (w > l) give('rival')
    }
  }
  const T = titleHolders(), was = st.set.titlesMine || [], now = Object.keys(T).filter(k => T[k] === me);
  was.filter(k => !now.includes(k)).forEach(k => {
    const t = TITLES.find(x => x[0] === k), thief = memberOf(T[k]);
    if (t && thief) {
      setTimeout(() => toast(`${thief.name} stole ${t[1]} from you 👀`), 600);
      addBell('👀', `${esc(thief.name)} stole ${t[1]} from you`)
    }
  });
  now.filter(k => !was.includes(k)).forEach(k => {
    const t = TITLES.find(x => x[0] === k);
    if (t) setTimeout(() => toast(`You took the title ${t[2]} ${t[1]}`), 900)
  });
  if (was.join() !== now.join()) {
    st.set.titlesMine = now;
    ch = true
  }
  const pos = arenaOrder('all').map(m => m.id), snap = st.set.arenaSnap || {};
  if (snap.wk !== curWk()) {
    st.set.arenaSnap = {wk: curWk(), pos: st.set.arenaLast || {}};
    ch = true
  }
  const last = {};
  pos.forEach((id, i) => last[id] = i);
  if (JSON.stringify(last) !== JSON.stringify(st.set.arenaLast || {})) {
    st.set.arenaLast = last;
    ch = true
  }
  crewExtraCheck();
  updateBellDot();
  if (ch) {
    saveSet();
    setTimeout(checkUnlocks, 50)
  }
}
function arenaOrder(mode) {
  if (!st.cs) return[];
  const mem = st.cs.members.slice();
  const f = mode === 'week' ? m => m.profile ? weekPts(m.profile) : -1 : m => m.profile ? m.profile.power || 0 : -1;
  return mem.sort((a, b) => f(b) - f(a))
}
/* ---------- 0.12: post data + rendering ---------- */
function postData(s) {
  const ss = sessStats(s), p = st.plan.days[DAYS.indexOf(s.day)] || {};
  const top = Object.entries(s.ex).map(([id, a]) => {
    const v = (a || []).filter(full);
    if (!v.length) return null;
    const b = v.reduce((m, x) => e1(x) > e1(m) ? x : m, v[0]);
    return {n: exName(id), w: +b.w, r: +b.r, pr: ss.prs.includes(id) ? 1 : 0, e: e1(b)}
  }).filter(Boolean).sort((x, y) => (y.pr - x.pr) || (y.e - x.e)).slice(0, 3).map(o => ({n: o.n, w: o.w, r: o.r, pr: o.pr}));
  return {
    day: p.name || DN[DAYS.indexOf(s.day)], color: colorOf(p), date: s.date, min: s.start && s.end ? Math.round(elapsed(s) / 6e4) : 0, sets: sSets(s), vol: Math.round(sVol(s)), prs: ss.prs.length, unit: U(), top, L: mLevels(muscleSets(s)), cmp: ss.vol ? volCompare(ss.vol)[1].replace(/<[^>]+>/g, '') : '', badges: engine().earnedOn(s.date).map(b => [b.icon, b.name]).slice(0, 4)
  }
}
const RE = {fire: '🔥', muscle: '💪', trophy: '🏆'};
function postHTML(p, preview) {
  if (p.type === 'trophy') return trophyPostHTML(p);
  if (p.type === 'group') return groupPostHTML(p);
  if (p.type === 'raid') return raidPostHTML(p);
  const m = memberOf2(p.by) || {id: p.by, name: 'Former member'}, pr = m.profile, me = myId(), own = p.by === me, owner = st.cs && st.cs.crew.owner === me, d = p.data || {}, dk = isDark();
  const head = `<div class="ph">${avatar(m)}<div class="pn"><div><b class="${nfxClass(m)}">${esc(m.name)}</b>${shamed(m.id)?' <span class="shame" title="Shame plate">🩷</span>':''} ${rankChip(pr)}${pr&&pr.tag?`<span class="ptag">${esc(pr.tag)}</span>`:''}</div><div class="sub">${p.type==='workout'?esc(d.day||'Workout')+' · ':''}${preview?'Preview':ago(p.ts)}</div></div>${!preview&&(own||owner)?`<button class="pm" data-pdel="${p.id}" aria-label="Delete post">⋯</button>`:''}</div>`;
  let body = '';
  if (p.type === 'join') body = `<p class="pcap">Joined the crew 👋</p>`;
  else if (p.type === 'rank') {
    const r = Math.max(0, Math.min(7, d.rank || 0));
    body = `<div class="prank" style="--rc:${RANKS[r][4]}">${emblem(r)}<div><div class="sub">Ranked up to</div><b>${esc(d.rname||RANKS[r][0])}</b></div></div>`
  } else {
    const u = d.unit || U(), vol = toMine(d.vol || 0, u);
    body = `${p.cap?`<p class="pcap">${esc(p.cap)}</p>`:''}${preview?'':roast(p)+hofBtn(p)}<div class="pcard" style="--pc:${(PC[d.color]||PC.red)[0]}"><div class="pmap"><svg viewBox="30 0 140 380" aria-hidden="true">${anatomy('front',d.L||{},dk)}</svg><svg viewBox="30 0 140 380" aria-hidden="true">${anatomy('back',d.L||{},dk)}</svg></div><div class="ptiles"><div><b>${d.min||'–'}</b><span>min</span></div><div><b>${d.sets||0}</b><span>sets</span></div><div><b>${kfmt(vol)}</b><span>${U()}</span></div><div><b>${d.prs||0}</b><span>PRs</span></div></div></div>
 ${(d.top||[]).length?`<div class="ptop">${d.top.map(t=>`<div><span>${esc(t.n)}</span><span>${t.pr?'<i class="prt">PR</i>':''}${Math.round(toMine(t.w,u)*10)/10} × ${t.r}</span></div>`).join('')}</div>`:''}
 ${d.cmp&&!preview?`<div class="sub pcmp">${esc(d.cmp)}</div>`:''}${(d.badges||[]).length?`<div class="pbad">${d.badges.map(b=>`<span>${esc(b[0])} ${esc(b[1])}</span>`).join('')}</div>`:''}`
  }
  if (preview) return `<div class="post">${head}${body}</div>`;
  const reacts = [...new Set([...reactKeys(), ...Object.keys(p.reacts || {}).filter(k => RE[k] && ((p.reacts || {})[k] || []).length)])].map(k => {
    const a = (p.reacts || {})[k] || [], on = a.includes(me);
    return `<button class="rx${on?' on':''}" data-react="${p.id}:${k}" aria-pressed="${on}">${RE[k]}${a.length?` ${a.length}`:''}</button>`
  }).join('');
  const cm = p.comments || [];
  return `<div class="post pc-${esc((pr&&pr.card)||'plain')}">${head}${body}<div class="prx">${reacts}<button class="rx cm" data-cmt="${p.id}">💬${cm.length?` ${cm.length}`:''}</button></div>${cm.slice(-2).map(c=>`<div class="pcm"><b>${esc((memberOf(c.by)||{name:'?'}).name)}</b> ${esc(c.text)}</div>`).join('')}</div>`
}
function crewEmptyHTML() {
  return `<div class="card crewhero"><div class="chi">🏟️</div><h2>Train with your crew</h2><p class="sub">Post your workouts, react to your friends' lifts and climb the Arena together. No account needed.</p>${st.set.name?'':`<label class="glbl" for="crname">YOUR NAME</label><input id="crname" class="field" maxlength="24" placeholder="Jake">`}
 <div class="glbl" style="margin-top:14px">JOIN A CREW</div><div class="row2"><input id="crcode" class="field" maxlength="11" placeholder="IRON-4821" autocapitalize="characters"><button class="btn acc" id="crjoin">Join</button></div>
 <div class="glbl" style="margin-top:14px">OR START ONE</div><div class="row2"><input id="crnew" class="field" maxlength="30" placeholder="Crew name"><button class="btn" id="crcreate">Create</button></div></div>`
}
function seg(tab, opts, cur) {
  return `<div class="seg2" role="tablist">${opts.map(([k,l])=>`<button data-seg="${tab}:${k}" aria-pressed="${cur===k}">${l}</button>`).join('')}</div>`
}
function lastFinished() {
  const rs = realSessions().filter(s => s.end && s.date >= iso(new Date(Date.now() - 2 * 864e5)));
  return rs[rs.length - 1] || null
}
function arenaHTML() {
  const mode = st.sub.arena || 'all', me = myId();
  let h = seg('arena', [['all', 'All-time'], ['week', 'This week'], ['lifts', 'Lifts']], mode);
  if (mode === 'lifts') return h + liftBoard();
  const order = arenaOrder(mode).filter(m => m.profile), T = titleHolders(), snap = (st.set.arenaSnap || {}).pos || {};
  if (!order.length) return h + `<div class="card"><p class="sub">Rankings show up once crew members open the app.</p></div>`;
  const val = m => mode === 'week' ? weekPts(m.profile) + ' pts' : (m.profile.power || 0);
  const pod = [order[1], order[0], order[2]];
  h += `<div class="podium">${pod.map((m,i)=>{if(!m)return '<div></div>';const place=[2,1,3][i],t=Object.keys(T).find(k=>T[k]===m.id),tt=t&&TITLES.find(x=>x[0]===t);return `<button class="pod p${place}" data-mem="${m.id}">${tt?`<span class="ttl">${tt[2]} ${tt[1]}</span>`:''}<span class="pemb2 fr fr-${esc(m.profile.frame||'none')} au-${esc(m.profile.aura||'none')}">${emblem(m.profile.rank||0,(m.profile.rank||0)>=6?'spin':'')}</span><b>${esc(m.name)}</b><span class="sub">${esc(m.profile.rname||'')} · ${val(m)}</span><span class="step">${place}</span></button>`}).join('')}</div>`;
  h += `<div class="card alist">${order.map((m,i)=>{const p=m.profile,prev=snap[m.id],mv=mode==='all'&&prev!==undefined?prev-i:0,tk=Object.keys(T).filter(k=>T[k]===m.id).map(k=>TITLES.find(x=>x[0]===k));
  return `<button class="arow${m.id===me?' me':''}" data-mem="${m.id}"><span class="apos">${i+1}</span>${avatar(m,34)}<span class="ainfo"><span><b>${esc(m.name)}</b>${m.ok?'<i class="vchk" title="Stats checked by the server">✓</i>':''}${mv>0?`<i class="up">▲${mv}</i>`:mv<0?`<i class="dn">▼${-mv}</i>`:''}${tk.slice(0,1).map(t=>`<i class="tchip">${t[2]} ${t[1]}</i>`).join('')}${tk.length>1?`<i class="tchip">+${tk.length-1}</i>`:''}</span><span class="abar"><u style="width:${Math.round((p.rank>=7?1:(p.frac||0))*100)}%;background:${RANKS[p.rank||0][4]}"></u></span><span class="sub">${esc(p.rname||'')} · 🔥${p.streak||0}${p.trial?' · Promotion trial':''}</span></span><b class="aval">${val(m)}</b></button>`}).join('')}</div>`;
  h += `<div class="card"><h3>Titles</h3><p class="sub">Only one person holds each title. Take it from them this week.</p><div class="tgrid">${TITLES.map(t=>{const m=memberOf(T[t[0]]);return `<div class="tcell${T[t[0]]===me?' mine':''}"><span class="ti2">${t[2]}</span><b>${t[1]}</b><span class="sub">${m?esc(m.name):'Up for grabs'}</span><span class="sub tdesc">${t[3]}</span></div>`}).join('')}</div></div>`;
  const riv = memberOf(st.set.rival), mine = memberOf(me);
  h += `<div class="duo"><div class="card mini">${riv&&riv.profile&&mine&&mine.profile?(()=>{const[w,l]=h2h({...wkOf(mine.profile),unit:mine.profile.unit},{...wkOf(riv.profile),unit:riv.profile.unit});return `<div class="sub">⚔️ Your rival</div><b>You ${w} – ${l} ${esc(riv.name)}</b><div class="sub">${w>l?'You lead this week':w<l?'They lead this week':'Dead even'} · days, sets, volume, PRs</div>`})():`<div class="sub">⚔️ Rival</div><b>Pick a rival</b><div class="sub">Tap anyone above to start a weekly head-to-head</div>`}</div>
 <div class="card mini">${(()=>{const cp=chalProgress();return cp?`<div class="sub">🏁 Crew challenge</div><b>${esc(cp.title)}</b><span class="abar"><u style="width:${Math.min(100,Math.round(cp.cur/cp.target*100))}%;background:#16b65b"></u></span><div class="sub">${cp.cur.toLocaleString()} / ${cp.target.toLocaleString()}${cp.cur>=cp.target?' · Done! 🎉':''}</div>`:`<div class="sub">🏁 Crew challenge</div><b>None this week</b><button class="btn sm" id="setchal" style="margin-top:6px">Set one</button>`})()}</div></div>`;
  h += awardsHTML();
  return h
}
function liftBoard() {
  const mem = st.cs.members.filter(m => m.profile && m.profile.lifts), cnt = {};
  mem.forEach(m => Object.keys(m.profile.lifts).forEach(n => cnt[n] = (cnt[n] || 0) + 1));
  const names = Object.keys(cnt).sort((a, b) => cnt[b] - cnt[a] || a.localeCompare(b));
  if (!names.length) return `<div class="card"><p class="sub">No lifts shared yet.</p></div>`;
  const sel = names.includes(st.liftSel) ? st.liftSel : names[0], rel = !!st.liftRel;
  const rows = mem.filter(m => m.profile.lifts[sel]).map(m => {
    const l = m.profile.lifts[sel], e = toMine(l.e, m.profile.unit), bw = m.profile.bw ? toMine(m.profile.bw, m.profile.unit) : 0;
    return {m, l, e, r: bw ? e / bw : 0}
  }).filter(x => !rel || x.r).sort((a, b) => rel ? b.r - a.r : b.e - a.e);
  return `<div class="card"><label class="glbl" for="liftsel">LIFT</label><select id="liftsel" class="field">${names.map(n=>`<option${n===sel?' selected':''}>${esc(n)}</option>`).join('')}</select>
 <div class="tog"><span>Relative to bodyweight</span><button class="sw" data-liftrel="1" aria-pressed="${rel}" aria-label="Relative to bodyweight"></button></div>${rel&&!(+st.set.bw)?`<p class="sub">Add your bodyweight in Crew ⚙︎ to show up here.</p>`:''}</div>
 <div class="card alist">${rows.length?rows.map((x,i)=>`<button class="arow${x.m.id===myId()?' me':''}" data-mem="${x.m.id}"><span class="apos">${['🥇','🥈','🥉'][i]||i+1}</span>${avatar(x.m,34)}<span class="ainfo"><b>${esc(x.m.name)}</b><span class="sub">Best ${Math.round(toMine(x.l.w,x.m.profile.unit))} × ${x.l.r}</span></span><b class="aval">${rel?x.r.toFixed(2)+'×':Math.round(x.e)+' '+U()}</b></button>`).join(''):'<p class="sub">Nobody here yet.</p>'}<p class="sub" style="margin-top:8px">Ranked by estimated 1-rep max${rel?' divided by bodyweight':''}.</p></div>`
}
/* ---------- 0.12: crew + profile click handling ---------- */
async function crewClick(b) {
  const d = b.dataset;
  if (d.seg) {
    const[t, k] = d.seg.split(':');
    if (t === 'arena') st.sub.arena = k;
    else st.sub[t] = k;
    if (t === 'stats' && k === 'lifts') setFlag('prog');
    if (t === 'plan' && k === 'notes') {st.editNote = null}render();
    return true
  }
  if (b.id === 'crjoin' || b.id === 'crcreate') {
    const nm = $('crname');
    if (nm) {
      if (!nm.value.trim()) {
        toast('Add your name first');
        nm.focus();
        return true
      }
      st.set.name = nm.value.trim().slice(0, 24);
      saveSet()
    }
    b.disabled = true;
    let r;
    if (b.id === 'crjoin') {
      const code = ($('crcode').value || '').trim().toUpperCase();
      if (!/^[A-Z]{3,6}-\d{4}$/.test(code)) {
        toast('Codes look like IRON-4821');
        b.disabled = false;
        return true
      }
      st.set.crew = {code};
      r = await crewCall('join', {})
    } else {
      const n = ($('crnew').value || '').trim();
      if (!n) {
        toast('Give your crew a name');
        b.disabled = false;
        return true
      }
      r = await crewCall('create', {name: n})
    }
    b.disabled = false;
    if (r.error) {
      st.set.crew = null;
      saveSet();
      toast(r.error === 'no crew' || r.error === 'bad code' ? 'No crew with that code' : r.error === 'full' ? 'That crew is full' : r.error === 'offline' ? 'You’re offline. Try again' : 'Could not do that. Try again');
      return true
    }
    st.set.crew = {code: r.crew.code};
    saveSet();
    saveCrewId();
    toast(b.id === 'crjoin' ? `Welcome to ${r.crew.name} 🎉` : `Crew created. Code: ${r.crew.code}`);
    syncCrew(true);
    render();
    return true
  }
  if (b.id === 'crinfo') {
    st.sheet = {type: 'crewinfo'};
    renderSheet();
    return true
  }
  if (b.id === 'postcrew' || b.id === 'postlast') {
    const s = b.id === 'postcrew' ? cur() : lastFinished();
    if (!s) return true;
    st.sheet = {type: 'post', key: s.date + '_' + s.day};
    renderSheet();
    return true
  }
  if (d.react) {
    const[pid2, k] = d.react.split(':'), p = (st.cs.posts || []).find(x => x.id === pid2);
    if (p) {
      const a = p.reacts[k] = p.reacts[k] || [], me = myId(), i = a.indexOf(me);
      if (i < 0) {
        a.push(me);
        st.set.reactN = (st.set.reactN || 0) + 1;
        saveSet()
      } else a.splice(i, 1);
      render()
    }
    crewCall('react', {post: pid2, emoji: k});
    return true
  }
  if (d.cmt) {
    st.sheet = {type: 'cmts', post: d.cmt};
    renderSheet();
    return true
  }
  if (d.pdel) {
    if (st.confirm !== 'p' + d.pdel) {
      st.confirm = 'p' + d.pdel;
      toast('Tap ⋯ again to delete this post');
      return true
    }
    st.confirm = null;
    st.cs.posts = st.cs.posts.filter(x => x.id !== d.pdel);
    render();
    crewCall('delete', {post: d.pdel});
    return true
  }
  if (d.mem) {
    st.sheet = {type: 'member', id: d.mem};
    renderSheet();
    return true
  }
  if (b.id === 'setchal') {
    st.sheet = {type: 'chal'};
    renderSheet();
    return true
  }
  if (d.liftrel) {
    st.liftRel = !st.liftRel;
    render();
    return true
  }
  if (b.id === 'openpass') {
    st.sheet = {type: 'pass'};
    renderSheet();
    return true
  }
  if (b.id === 'editlook') {
    st.sheet = {type: 'look'};
    renderSheet();
    return true
  }
  if (b.id === 'openyir') {
    openYear();
    return true
  }
  if (d.bdg) {
    st.sheet = {type: 'badge', k: d.bdg};
    renderSheet();
    return true
  }
  if (d.lad) {
    st.sheet = {type: 'lad', cat: d.lad};
    renderSheet();
    return true
  }
  return false
}
function crewSheet(sh) {
  if (!st.plan) return null;
  const E = engine(), me = myId();
  if (sh.type === 'post') {
    const s = st.sessions[sh.key];
    if (!s) return '';
    const fake = {id: 'x', by: me, ts: Date.now(), type: 'workout', cap: '', data: postData(s)};
    return `<div class="ghd"><h2>Post to crew</h2><button class="btn sm" id="sheetx">Cancel</button></div><textarea id="capin" class="field" maxlength="280" rows="2" placeholder="Say something about it (optional)"></textarea><div style="margin:12px 0">${postHTML(fake,true)}</div><button class="btn acc wide" id="dopost" style="min-height:52px">Post</button>`
  }
  if (sh.type === 'cmts') {
    const p = (st.cs.posts || []).find(x => x.id === sh.post);
    if (!p) return '';
    return `<div class="ghd"><h2>Comments</h2><button class="btn sm" id="sheetx">Done</button></div>${(p.comments||[]).map(c=>`<div class="pcm"><b>${esc((memberOf(c.by)||{name:'?'}).name)}</b> ${esc(c.text)} <span class="sub">${ago(c.ts)}</span></div>`).join('')||'<p class="sub">No comments yet. Say something.</p>'}<div class="row2" style="margin-top:12px"><input id="cmin" class="field" maxlength="200" placeholder="Add a comment"><button class="btn acc" id="cmsend">Send</button></div>`
  }
  if (sh.type === 'crewinfo') {
    const c = st.cs.crew, own = c.owner === me;
    return `<div class="ghd"><h2>${esc(c.name)}</h2><button class="btn sm" id="sheetx">Done</button></div><div class="codebox"><div class="sub">Crew code</div><b>${esc(c.code)}</b><button class="btn sm" id="sharecode">Share invite</button></div>
  <h3>Members</h3>${st.cs.members.map(m=>`<div class="mrow2">${avatar(m,34)}<span><b>${esc(m.name)}</b>${m.id===c.owner?' <span class="sub">· owner</span>':''}${m.id===me?' <span class="sub">· you</span>':''}<div class="sub">${m.profile?esc(m.profile.rname):'Hasn’t synced yet'}</div></span>${own&&m.id!==me?`<button class="btn sm" data-kick="${m.id}">${st.confirm==='k'+m.id?'Tap to confirm':'Remove'}</button>`:''}</div>`).join('')}
  ${crewInfoExtra()}
  <div class="tog"><span>Crew pings<div class="sub" style="font-weight:500">Lock-screen ping when someone posts or ranks up</div></span><button class="sw" id="crpings" aria-pressed="${!!st.set.crewPings}" aria-label="Crew pings"></button></div>
  <div class="tog"><span>Bodyweight (${U()})<div class="sub" style="font-weight:500">Optional. Only used for the relative lift boards</div></span><input id="bwin" class="field" inputmode="decimal" style="width:90px" value="${st.set.bw||''}" placeholder="180"></div>
  <button class="btn wide" id="crleave" style="margin-top:14px;color:var(--red)">${st.confirm==='leave'?'Tap again to leave':'Leave crew'}</button>`
  }
  if (sh.type === 'member') {
    const m = memberOf(sh.id);
    if (!m) return '';
    const p = m.profile || {}, mine = memberOf(me), mp = mine && mine.profile, isMe = m.id === me;
    const lifts = Object.entries(p.lifts || {}).slice(0, 6);
    const common = mp ? lifts.filter(([n]) => mp.lifts && mp.lifts[n]) : [];
    return `<div class="ghd"><h2>${esc(m.name)}</h2><button class="btn sm" id="sheetx">Done</button></div><div class="prof" style="text-align:center"><div class="pemb" style="width:110px;height:110px"><span class="fr fr-${esc(p.frame||'none')} au-${esc(p.aura||'none')}">${emblem(p.rank||0,(p.rank||0)>=6?'spin':'')}</span></div><h2 style="margin:4px 0">${esc(p.rname||'Unranked')}</h2>${p.tag?`<div class="ptag big">${esc(p.tag)}</div>`:''}<div class="sub">Rank Power ${p.power||0} · 🔥 ${p.streak||0} week${p.streak===1?'':'s'} · ${p.bcount||0} badges</div></div>
  ${(p.show||[]).length?`<div class="showc">${p.show.map(x=>`<div class="slot on">${esc(x[0])}<span>${esc(x[1])}</span></div>`).join('')}</div>`:''}
  ${!isMe&&common.length?`<h3>Head-to-head</h3>${common.map(([n,l])=>{const a=toMine(mp.lifts[n].e,mp.unit),b2=toMine(l.e,p.unit);return `<div class="h2r"><b class="${a>=b2?'win':''}">${Math.round(a)}</b><span>${esc(n)}</span><b class="${b2>a?'win':''}">${Math.round(b2)}</b></div>`}).join('')}<p class="sub" style="text-align:center">You vs ${esc(m.name)} · estimated 1RM</p>`:lifts.length?`<h3>Best lifts</h3>${lifts.map(([n,l])=>`<div class="h2r"><span>${esc(n)}</span><b>${Math.round(toMine(l.w,p.unit))} × ${l.r}</b></div>`).join('')}`:''}
  ${!isMe&&st.cs.buddies&&st.cs.buddies[m.id]?`<p class="sub" style="text-align:center">👯 ${st.cs.buddies[m.id]} gym buddy session${st.cs.buddies[m.id]>1?'s':''} together</p>`:''}${!isMe&&p.plan&&p.plan.days?`<h3>Their plan</h3>${p.plan.days.map((d,i)=>d.groups&&d.groups.length?`<div class="h2r"><span style="text-align:left"><b style="color:var(--ink)">${DN[i]} · ${esc(d.name)}</b> · ${d.groups.reduce((a,g)=>a+g.length,0)} exercises</span><button class="btn sm" data-steal="${m.id}:${i}">Copy</button></div>`:'').join('')}<button class="btn wide" data-steal="${m.id}:all" style="margin-top:8px">Copy their whole split</button>`:''}
  ${isMe?'':`<button class="btn acc wide" id="setrival" data-id="${m.id}" style="margin-top:12px;min-height:50px">${st.set.rival===m.id?'Remove rival':'⚔️ Make them your rival'}</button>`}`
  }
  if (sh.type === 'chal') {
    const n = Math.max(2, st.cs.members.length), k = U() === 'kg', sc = v => Math.round(v * n / 4 / 10) * 10 || v;
    const pre = [['sets', sc(300), `${sc(300)} sets as a crew`], ['days', Math.max(4, Math.round(4 * n)), `${Math.max(4,Math.round(4*n))} workouts as a crew`], ['vol', sc(k ? 110000 : 250000), `Move ${kfmt(sc(k?110000:250000))} ${U()} as a crew`], ['prs', Math.max(3, Math.round(2.5 * n)), `${Math.max(3,Math.round(2.5*n))} PRs as a crew`]];
    return `<div class="ghd"><h2>Crew challenge</h2><button class="btn sm" id="sheetx">Cancel</button></div><p class="sub">Everyone's workouts this week count toward it. Resets Monday.</p>${pre.map(([t,v,l])=>`<button class="btn wide" data-chal="${t}:${v}" data-title="${esc(l)}" style="margin-top:8px;min-height:50px">${esc(l)}</button>`).join('')}`
  }
  if (sh.type === 'badge') {
    const b = E.B.find(x => x.k === sh.k) || E.R.find(x => x.k === sh.k);
    if (!b) return '';
    const earned = b.earned || b.count > 0, [rr, rn] = b.count !== undefined ? ['Weekly', ''] : rarity(b), pinned = (st.set.showcase || []).includes(b.k);
    return `<div class="ghd"><h2>${b.hid&&!earned?'Hidden badge':esc(b.name)}</h2><button class="btn sm" id="sheetx">Done</button></div><div style="text-align:center"><div style="font-size:64px;${earned?'':'filter:grayscale(1);opacity:.5'}">${b.hid&&!earned?'❔':b.icon}</div><span class="rar" style="--rc:${RARC[rr]||'#8c939e'}">${rr}</span>${rn?`<div class="sub">${rn}</div>`:''}<p>${b.hid&&!earned?'Keep training to discover it.':esc(b.desc)}</p>${b.count!==undefined?`<p class="sub">Earned ${b.count} time${b.count===1?'':'s'}</p>`:earned?'<p class="sub">Earned ✓</p>':b.tgt>1?`<p class="sub">${b.cur.toLocaleString()} / ${b.tgt.toLocaleString()}</p>`:''}</div>${earned?`<button class="btn ${pinned?'':'acc'} wide" id="pinb" data-k="${b.k}" style="min-height:50px">${pinned?'Unpin from showcase':'📌 Pin to showcase'}</button>`:''}`
  }
  if (sh.type === 'lad') {
    const list = E.B.filter(b => b.cat === sh.cat), [nm, ic] = LADDER[sh.cat];
    return `<div class="ghd"><h2>${ic} ${nm}</h2><button class="btn sm" id="sheetx">Done</button></div>${list.map((b,i)=>`<div class="ladder${b.earned?' past':''}" style="opacity:${b.earned?1:.6}"><span class="tdot" style="background:${TIERS[i][1]}">${ROMAN[i+1]}</span><div><b>${TIERS[i][0]} · ${esc(b.name)}</b><div class="sub">${esc(b.desc)}${b.earned?'':` · ${b.cur.toLocaleString()}/${b.tgt.toLocaleString()}`}</div></div>${b.earned?`<button class="btn sm" data-pinb2="${b.k}">${(st.set.showcase||[]).includes(b.k)?'Pinned':'Pin'}</button>`:'<span class="lk">🔒</span>'}</div>`).join('')}`
  }
  if (sh.type === 'look') {
    const fr = Object.entries(FRAMES), au = Object.entries(AURAS), tags = st.set.cos && st.set.cos.tag || [];
    return `<div class="ghd"><h2>Frames and auras</h2><button class="btn sm" id="sheetx">Done</button></div><h3>Frame</h3><div class="lookg"><button data-frame="" aria-pressed="${!st.set.frame}"><span class="fr fr-none">${emblem(E.rank)}</span><span>None</span></button>${fr.map(([k,v])=>{const ok=frameOK(k,E);return `<button data-frame="${k}" aria-pressed="${st.set.frame===k}" ${ok?'':'class="lockd"'}><span class="fr fr-${k}">${emblem(E.rank)}</span><span>${ok?v[0]:'🔒 '+v[1]}</span></button>`}).join('')}</div>
  <h3>Aura</h3><div class="lookg"><button data-aura="" aria-pressed="${!st.set.aura}"><span class="fr">${emblem(E.rank)}</span><span>None</span></button>${au.map(([k,v])=>{const ok=auraOK(k,E);return `<button data-aura="${k}" aria-pressed="${st.set.aura===k}" ${ok?'':'class="lockd"'}><span class="fr au-${k}">${emblem(E.rank)}</span><span>${ok?v[0]:'🔒 '+v[1]}</span></button>`}).join('')}</div>
  <h3>Nameplate</h3>${tags.length?`<div class="exfoot"><button class="btn sm" data-tag="" aria-pressed="${!st.set.tag}">None</button>${tags.map(t=>`<button class="btn sm" data-tag="${esc(t)}" aria-pressed="${st.set.tag===t}">${esc(t)}</button>`).join('')}</div>`:'<p class="sub">Unlock nameplates in the season pass (levels 10 and 26).</p>'}`
  }
  if (sh.type === 'pass') {
    return `<div class="ghd"><h2>Season ${E.season} pass</h2><button class="btn sm" id="sheetx">Done</button></div><p class="sub">Level ${E.lvl} of 30 · ${E.xp.toLocaleString()} XP. Earn XP from daily challenges (10 each, +20 for a sweep), weekly quests (30), workouts (15), PRs (5) and weekly badges (25). Rewards stay yours forever.</p>${PASS.map(([lv,,,lbl])=>`<div class="ladder${E.lvl>=lv?' past':''}" style="opacity:${E.lvl>=lv?1:.6}"><span class="tdot" style="background:${E.lvl>=lv?'#16b65b':'#8c939e'}">${lv}</span><div><b>${esc(lbl)}</b><div class="sub">Level ${lv}</div></div>${E.lvl>=lv?'<span class="ok">✓</span>':'<span class="lk">🔒</span>'}</div>`).join('')}<p class="sub">Finish the season to keep a permanent Season ${E.season} badge showing your level.</p>`
  }
  return null
}
async function crewSheetClick(b) {
  const sh = st.sheet, d = b.dataset;
  if (!sh) return false;
  if (b.id === 'dopost') {
    const s = st.sessions[sh.key];
    b.disabled = true;
    const r = await crewCall('post', {post: {type: 'workout', cap: ($('capin').value || '').trim(), data: postData(s)}});
    b.disabled = false;
    if (r.error) {
      toast('Could not post. Check your connection');
      return true
    }
    s.posted = true;
    saveS();
    st.sheet = null;
    renderSheet();
    st.tab = 'crew';
    st.sub.crew = 'feed';
    render();
    window.scrollTo({top: 0});
    toast('Posted to your crew 📣');
    return true
  }
  if (b.id === 'cmsend') {
    const t = ($('cmin').value || '').trim();
    if (!t) return true;
    b.disabled = true;
    const r = await crewCall('comment', {post: sh.post, text: t});
    b.disabled = false;
    if (r.error) {
      toast('Could not send');
      return true
    }
    renderSheet();
    render();
    return true
  }
  if (b.id === 'sharecode') {
    const c = st.cs.crew, link = location.origin + location.pathname, txt = `Join my crew "${c.name}" on Overload 💪\n1. Open ${link}\n2. Add it to your home screen\n3. Crew tab → Join → ${c.code}`;
    try {
      if (navigator.share) await navigator.share({text: txt});
      else {
        await navigator.clipboard.writeText(txt);
        toast('Invite copied')
      }
    } catch (e) {}return true
  }
  if (d.kick) {
    if (st.confirm !== 'k' + d.kick) {
      st.confirm = 'k' + d.kick;
      renderSheet();
      return true
    }
    st.confirm = null;
    await crewCall('kick', {target: d.kick});
    renderSheet();
    return true
  }
  if (b.id === 'crpings') {
    if (!st.set.crewPings) {
      if (!st.set.push) {
        const ok = await enablePush();
        if (!ok) return true;
        toast('Rest alerts turned on too')
      }
      st.set.crewPings = true;
      await crewCall('pings', {endpoint: st.set.push})
    } else {
      st.set.crewPings = false;
      await crewCall('pings', {endpoint: null})
    }
    saveSet();
    renderSheet();
    return true
  }
  if (b.id === 'crleave') {
    if (st.confirm !== 'leave') {
      st.confirm = 'leave';
      renderSheet();
      return true
    }
    st.confirm = null;
    await crewCall('leave', {});
    st.set.crew = null;
    st.cs = null;
    store('overload-crewcache', null);
    saveSet();
    saveCrewId();
    st.sheet = null;
    renderSheet();
    render();
    toast('You left the crew');
    return true
  }
  if (b.id === 'setrival') {
    st.set.rival = st.set.rival === d.id ? null : d.id;
    saveSet();
    st.sheet = null;
    renderSheet();
    render();
    toast(st.set.rival ? 'Rival set ⚔️ Weekly head-to-head is on' : 'Rival removed');
    return true
  }
  if (d.chal) {
    const[t, v] = d.chal.split(':');
    await crewCall('challenge', {type: t, target: +v, wk: curWk(), title: d.title});
    st.sheet = null;
    renderSheet();
    render();
    toast('Challenge set 🏁');
    return true
  }
  if (b.id === 'pinb' || d.pinb2) {
    const k = d.k || d.pinb2, a = st.set.showcase = st.set.showcase || [], i = a.indexOf(k);
    if (i >= 0) a.splice(i, 1);
    else {
      if (a.length >= 3) a.shift();
      a.push(k)
    }
    saveSet();
    renderSheet();
    render();
    syncCrew(true);
    return true
  }
  if (d.frame !== undefined) {
    if (b.classList.contains('lockd')) {
      toast('Locked. ' + FRAMES[d.frame][1]);
      return true
    }
    st.set.frame = d.frame;
    saveSet();
    renderSheet();
    render();
    syncCrew(true);
    return true
  }
  if (d.aura !== undefined) {
    if (b.classList.contains('lockd')) {
      toast('Locked. ' + AURAS[d.aura][1]);
      return true
    }
    st.set.aura = d.aura;
    saveSet();
    renderSheet();
    render();
    syncCrew(true);
    return true
  }
  if (d.tag !== undefined) {
    st.set.tag = d.tag;
    saveSet();
    renderSheet();
    render();
    syncCrew(true);
    return true
  }
  return false
}
/* ---------- 0.13: activity bell ---------- */
function addBell(icon, text) {
  const a = st.set.bellLocal = st.set.bellLocal || [];
  a.unshift({ts: Date.now(), icon, text});
  st.set.bellLocal = a.slice(0, 60);
  saveSet();
  updateBellDot()
}
function bellItems() {
  const me = myId(), out = (st.set.bellLocal || []).map(x => ({ts: x.ts, icon: x.icon, text: x.text}));
  if (st.cs)(st.cs.events || []).forEach(e => {
    if (e.from === me) return;
    const who = esc((memberOf(e.from) || {name: 'Someone'}).name);
    const T = {
      react: ['🔥', `${who} reacted ${RE[e.emoji]||'🔥'} to your post`], comment: ['💬', `${who} commented: "${esc(e.text||'')}"`], rank: ['🏅', `${who} ranked up to ${esc(e.text||'')}`], join: ['👋', `${who} joined the crew`], challenge: ['🏁', `${who} set a challenge: ${esc(e.text||'')}`], hype: ['🔥', `${who} hyped you up`], mention: ['💬', `${who} mentioned you: "${esc(e.text||'')}"`], reply: ['↪', `${who} replied: "${esc(e.text||'')}"`], trophy: ['🏆', `Crew trophy unlocked: ${esc(e.text||'')}`], crewrank: ['🛡', `Your crew ranked up to ${esc(e.text||'')}`], rename: ['✏️', `${who} renamed the crew to ${esc(e.text||'')}`], shame: ['🩷', `${esc(e.text||'')} got the shame plate this week`], restore: ['↩', `${who} restored crew posts from a backup`]
    }[e.type];
    if (T) out.push({ts: e.ts, icon: T[0], text: T[1], raw: 1})
  });
  return out.sort((a, b) => b.ts - a.ts).slice(0, 60)
}
const bellUnread = () => bellItems().filter(x => x.ts > (st.set.bellSeen || 0)).length;
function updateBellDot() {
  const t = $('t-crew');
  if (t) t.classList.toggle('dot', bellUnread() > 0 || chatUnread() > 0 || (crewOn() && !!st.cs && [1, 2, 3].includes(new Date().getDay()) && st.set.recapSeen !== curWk()))
}
/* ---------- 0.13: live crew ---------- */
let ws = null, wsTry = 0, wsBeat = null;
function wsConnect() {
  if (!crewOn() || (ws && ws.readyState < 2) || document.hidden) return;
  const me = crewIdentity();
  try {
    ws = new WebSocket(PUSH_URL.replace(/^http/, 'ws') + '/crew/ws?code=' + encodeURIComponent(st.set.crew.code) + '&id=' + encodeURIComponent(me.id) + '&key=' + encodeURIComponent(me.key))
  } catch (e) {
    ws = null;
    return
  }
  ws.onmessage = e => {
    let m;
    try {m = JSON.parse(e.data)} catch (x) {return}
    if (m.t === 'refresh' || m.t === 'kicked') syncCrew(true);
    if (m.t === 'group') syncCrew(true).then(() => render());
    if (m.t === 'live' && GRP && st.tab === 'log' && !st.profile && !st.settings && !document.querySelector('#main input:focus')) {
      st.liveMap = m.live || {};
      render()
    }
    if (m.t === 'live') {
      st.liveMap = m.live || {};
      if (st.tab === 'crew' && !st.profile && !st.settings && !document.querySelector('#main input:focus,#main textarea:focus')) render()
    }
    if (m.t === 'hype') hypeBurst(m.from);
    if (m.t === 'chat') chatOnMsg(m.msg);
    if (m.t === 'chatupd') chatOnUpd(m.msg)
  };
  ws.onopen = () => {
    wsTry = 0;
    sendLive()
  };
  ws.onclose = () => {
    ws = null;
    if (!document.hidden && crewOn()) setTimeout(wsConnect, Math.min(30000, 1000 * 2 ** wsTry++))
  };
  ws.onerror = () => {}
}
function wsSend(o) {if (ws && ws.readyState === 1) {ws.send(JSON.stringify(o)); return true}return false}
function liveSession() {
  const s = st.sessions[today() + '_' + DAYS[dayIdx(today())]];
  return s && s.start && !s.end ? s : null
}
function sendLive() {
  if (!crewOn()) return;
  const s = liveSession();
  const msg = s ? {
    t: 'live', on: true, day: (st.plan.days[dayIdx(today())] || {}).name || 'Workout', start: s.start, ...grpLive()
  } : {t: 'live', on: false};
  if (!wsSend(msg) && s) crewCall('live', msg).then(r => {if (r && r.live) st.liveMap = r.live});
  clearInterval(wsBeat);
  if (s) wsBeat = setInterval(() => {
    if (liveSession()) sendLive();
    else clearInterval(wsBeat)
  }, 240000)
}
function crewLiveEnd() {
  if (!crewOn()) return;
  clearInterval(wsBeat);
  if (!wsSend({t: 'live', on: false})) crewCall('live', {on: false})
}
function liveStrip() {
  const me = myId(), L = Object.entries(st.liveMap || {}).filter(([id]) => id !== me && memberOf(id));
  if (!L.length) return '';
  return `<div class="livestrip">${L.map(([id,l])=>`<button class="livechip" data-hype="${id}"><span class="ldot"></span><b>${esc(memberOf(id).name)}</b><span class="sub">${esc(l.day||'Lifting')} · ${Math.max(1,Math.round((Date.now()-l.start)/6e4))}m</span><span class="lh">🔥</span></button>`).join('')}</div>`
}
function hypeBurst(from) {
  haptic('heavy');
  const d = document.createElement('div');
  d.className = 'hype';
  d.innerHTML = `${Array.from({length:22},(_,i)=>`<i style="left:${Math.round(Math.random()*100)}%;animation-delay:${(Math.random()*.6).toFixed(2)}s;font-size:${22+Math.round(Math.random()*26)}px">🔥</i>`).join('')}<div class="hypemsg"><b>${esc(from||'Your crew')}</b> hyped you up</div>`;
  document.body.appendChild(d);
  setTimeout(() => d.remove(), 2600);
  addBell('🔥', `${esc(from||'Someone')} hyped you up`)
}
const crewXpFor = l => 60 * (l - 1) ** 2;
function recapHTML() {
  const dw = new Date().getDay();
  if (![1, 2, 3].includes(dw) || st.set.recapSeen === curWk() || !st.cs) return '';
  const mem = st.cs.members.filter(m => m.profile);
  if (mem.length < 2) return '';
  const pts = w => w ? w.days * 40 + w.sets * 3 + w.prs * 20 : 0;
  const lwk = iso(new Date(toD(curWk()).getTime() - 7 * 864e5)), pwk = iso(new Date(toD(curWk()).getTime() - 14 * 864e5));
  const L = mem.map(m => {
    const p = m.profile, l = lastOf(p), pv = p.prev && p.prev.wk === pwk ? p.prev : null;
    return {m, l: pts(l), d: pts(l) - pts(pv), vol: l ? toMine(l.vol, p.unit) : 0}
  });
  const mvp = L.slice().sort((a, b) => b.l - a.l)[0], imp = L.slice().sort((a, b) => b.d - a.d)[0], tot = L.reduce((a, x) => a + x.vol, 0);
  const t0 = toD(lwk).getTime(), t1 = t0 + 7 * 864e5, tp = (st.cs.posts || []).filter(p => p.ts >= t0 && p.ts < t1 && p.type === 'workout').map(p => ({p, n: Object.values(p.reacts || {}).reduce((a, x) => a + x.length, 0)})).sort((a, b) => b.n - a.n)[0];
  return `<div class="card recap"><div class="ghd"><b>📊 Last week in ${esc(st.cs.crew.name)}</b><button class="gx" id="recapx" aria-label="Dismiss">✕</button></div><div class="rgrid"><div><span>🏆 MVP</span><b>${esc(mvp.m.name)}</b></div><div><span>📈 Most improved</span><b>${imp.d>0?esc(imp.m.name):'—'}</b></div><div><span>🏗️ Crew volume</span><b>${kfmt(tot)} ${U()}</b></div><div><span>⭐ Top post</span><b>${tp&&tp.n?esc((memberOf(tp.p.by)||{name:'?'}).name)+` · ${tp.n} reacts`:'—'}</b></div></div></div>`
}
const AWARDS = [['grinder', 'Biggest Grinder', '⚙️'], ['hype', 'Best Hype Man', '📣'], ['comeback', 'Comeback of the Month', '🔁'], ['improved', 'Most Improved', '📈'], ['post', 'Post of the Month', '⭐']];
const monthKey = d => {
  const t = d ? new Date(d) : new Date();
  return t.getFullYear() + '-' + String(t.getMonth() + 1).padStart(2, '0')
};
const prevMonthKey = () => {
  const t = new Date();
  t.setDate(1);
  t.setMonth(t.getMonth() - 1);
  return monthKey(t)
};
const monthName = k => MON[+k.slice(5) - 1];
function awardWinners(mo) {
  const V = (st.cs && st.cs.votes || {})[mo] || {}, out = {};
  AWARDS.forEach(([k]) => {
    const t = (V[k] || {}).tally || {};
    const top = Object.entries(t).sort((a, b) => b[1] - a[1]);
    if (top.length && (!top[1] || top[0][1] > top[1][1])) out[k] = top[0][0]
  });
  return out
}
function awardsHTML() {
  if (!st.cs || st.cs.members.length < 2) return '';
  const d = new Date().getDate(), mo = monthKey(), pm = prevMonthKey();
  if (d <= 7) {
    const W = awardWinners(pm);
    if (Object.keys(W).length) return `<div class="card"><h3>🎖️ ${monthName(pm)} crew awards</h3>${AWARDS.filter(a=>W[a[0]]).map(a=>`<div class="h2r"><span style="text-align:left">${a[2]} ${a[1]}</span><b>${esc((memberOf(W[a[0]])||{name:'?'}).name)}</b></div>`).join('')}</div>`
  }
  if (d >= 20) {
    const V = (st.cs.votes || {})[mo] || {}, done = AWARDS.filter(a => (V[a[0]] || {}).mine).length;
    return `<button class="card awardcta" id="openawards"><span>🎖️</span><span class="stx"><b>Vote: ${monthName(mo)} crew awards</b><span class="sub">${done}/${AWARDS.length} votes cast · results on the 1st</span></span><span class="sc">›</span></button>`
  }
  return ''
}
function awardsSheet() {
  const mo = monthKey(), V = (st.cs.votes || {})[mo] || {}, me = myId(), mem = st.cs.members.filter(m => m.id !== me);
  return `<div class="ghd"><h2>${monthName(mo)} awards</h2><button class="btn sm" id="sheetx">Done</button></div><p class="sub">One vote per category. You can change it until the month ends.</p>${AWARDS.map(([k,n,i])=>`<div class="glbl" style="margin-top:14px">${i} ${n.toUpperCase()}</div><div class="exfoot">${mem.map(m=>`<button class="btn sm" data-vote="${k}:${m.id}" aria-pressed="${(V[k]||{}).mine===m.id}">${esc(m.name)}</button>`).join('')}</div>`).join('')}`
}
function stealSheet(sh) {
  const m = memberOf(sh.id), p = m && m.profile;
  if (!p || !p.plan) return '';
  const d = p.plan.days[sh.day];
  if (sh.day === 'all') return `<h2>Copy ${esc(m.name)}'s whole split?</h2><p class="sub">This replaces all 7 days of your plan. Your workout history stays. Your current plan is saved so you can undo it in Settings.</p><button class="btn acc wide" id="stealallgo" style="min-height:52px">Replace my plan</button><button class="btn wide" id="sheetx" style="margin-top:8px">Cancel</button>`;
  return `<div class="ghd"><h2>Copy ${esc(d.name)}</h2><button class="btn sm" id="sheetx">Cancel</button></div><p class="sub">Which of your days should it replace?</p><div class="days">${DN.map((n,i)=>`<button data-stealto="${i}">${n}<br><span class="sub" style="font-size:10px">${esc((st.plan.days[i]||{}).name||'')}</span></button>`).join('')}</div>`
}
function crewExtraBadges() {
  const cs = st.cs, out = [], cb = st.set.crewB || {};
  const n = cs && cs.buddyDates ? cs.buddyDates.length : 0;
  [[1, 'Gym Buddies'], [5, 'Spotter'], [15, 'Partners in Iron']].forEach(([t, nm], i) => out.push({
    k: 'buddy' + t, name: nm, icon: '👯', cat: 'Crew', cur: Math.min(n, t), tgt: t, desc: `Train at the same time as a crew member ${t} time${t>1?'s':''}`, hid: false, earned: n >= t
  }));
  const aw = Object.keys(cb).filter(k => k.startsWith('award_')).length;
  out.push({
    k: 'awards', name: 'Crew Award', icon: '🎖️', cat: 'Crew', cur: Math.min(aw, 1), tgt: 1, desc: 'Win a monthly crew award', hid: false, earned: aw > 0
  });
  const D = st.set.chainsDone || [];
  D.forEach((nm, i) => out.push({
    k: 'qc' + i, name: nm, icon: '🗺️', cat: 'Quests', cur: 1, tgt: 1, desc: 'Finished a quest chain', hid: false, earned: true
  }));
  const gd = (st.set.goals || []).filter(g => g.doneAt).length;
  [[1, 'Goal Getter'], [3, 'Goal Crusher'], [10, 'Target Locked']].forEach(([t, nm]) => out.push({
    k: 'goal' + t, name: nm, icon: '🎯', cat: 'Quests', cur: Math.min(gd, t), tgt: t, desc: `Crush ${t} goal${t>1?'s':''}`, hid: false, earned: gd >= t
  }));
  return out
}
function crewExtraCheck() {
  if (!st.cs) return;
  const cb = st.set.crewB = st.set.crewB || {}, me = myId(), d = new Date().getDate();
  let ch = false;
  if (d <= 7) {
    const pm = prevMonthKey(), W = awardWinners(pm);
    Object.entries(W).forEach(([cat, id]) => {
      const k = 'award_' + pm + '_' + cat;
      if (id === me && !cb[k]) {
        cb[k] = today();
        ch = true;
        const a = AWARDS.find(x => x[0] === cat);
        setTimeout(() => {
          toast(`🎖️ You won ${a[1]}!`);
          confetti(120);
          addBell('🎖️', `You won ${a[1]} for ${monthName(pm)}`)
        }, 800)
      }
    })
  }
  const riv = memberOf(st.set.rival), mine = memberOf(me);
  if (riv && mine && mine.profile && riv.profile) {
    const a = lastOf(mine.profile), b = lastOf(riv.profile), lw = iso(new Date(toD(curWk()).getTime() - 7 * 864e5));
    if (a && b && st.set.rivalSeen !== lw) {
      const[w, l] = h2h({...a, unit: mine.profile.unit}, {...b, unit: riv.profile.unit});
      st.set.rivalSeen = lw;
      ch = true;
      addBell('⚔️', w > l ? `You beat ${esc(riv.name)} last week, ${w}–${l}` : w < l ? `${esc(riv.name)} beat you last week, ${l}–${w}. Get it back` : `You and ${esc(riv.name)} tied last week`)
    }
  }
  if (ch) saveSet()
}
/* ---------- crew helpers ---------- */
const memberOf2 = id => {
  if (!st.cs) return null;
  const m = st.cs.members.find(x => x.id === id);
  if (m) return m;
  const f = (st.cs.former || {})[id];
  return f ? {id, name: f, former: true} : null
};
function isOwner() {return !!(st.cs && st.cs.crew.owner === myId())}
function crewSum() {
  const ms = (st.cs && st.cs.members || []).filter(m => m.profile);
  return {
    ms, vol: ms.reduce((a, m) => a + toMine(m.profile.lifeVol || 0, m.profile.unit), 0), wo: ms.reduce((a, m) => a + (+m.profile.workouts || 0), 0), streak: ms.length ? Math.min(...ms.map(m => +m.profile.streak || 0)) : 0
  }
}
const CREW_RANK_PTS = [0, 400, 1200, 2500, 4500, 7500, 11500, 17000];
function crewRankInfo() {
  const c = st.cs.crew, pts = (c.xp || 0) + Object.keys(c.trophies || {}).length * 400, r = Math.max(c.rank || 0, CREW_RANK_PTS.filter(t => pts >= t).length - 1), nx = CREW_RANK_PTS[r + 1];
  return {r, pts, nx, frac: nx ? Math.min(1, (pts - CREW_RANK_PTS[r]) / (nx - CREW_RANK_PTS[r])) : 1}
}
function seasonWall() {
  const posts = (st.cs.posts || []).filter(p => p.ts >= toD(seasonRange(seasonOf(today()))[0]).getTime());
  let big = null, hy = null;
  posts.forEach(p => {
    if (p.type !== 'workout') return;
    (p.data && p.data.top || []).forEach(t => {
      if (t.pr && (!big || toMine(t.w, p.data.unit) > big.w)) big = {w: toMine(t.w, p.data.unit), r: t.r, n: t.n, by: p.by}
    });
    const n = Object.values(p.reacts || {}).reduce((a, x) => a + x.length, 0);
    if (n && (!hy || n > hy.n)) hy = {n, by: p.by}
  });
  const ms = crewSum().ms, stk = ms.slice().sort((a, b) => (b.profile.streak || 0) - (a.profile.streak || 0))[0];
  const tile = (icon, t, v, who) => `<div class="sw1"><span>${icon}</span><div><div class="sub">${t}</div><b>${v}</b><div class="sub">${who?esc((memberOf2(who)||{name:'?'}).name):'Up for grabs'}</div></div></div>`;
  return `<div class="card"><div class="ghd"><h3>Season ${seasonOf(today())} wall</h3><span class="sub">Best moments</span></div><div class="swall">${tile('🏆','Biggest PR',big?`${esc(big.n)} ${Math.round(big.w)}×${big.r}`:'No PRs posted yet',big&&big.by)}${tile('🔥','Longest streak',stk&&stk.profile.streak?stk.profile.streak+' weeks':'–',stk&&stk.profile.streak&&stk.id)}${tile('📣','Most hyped post',hy?hy.n+' reactions':'–',hy&&hy.by)}</div></div>`
}
const CTROPHY = [['mpc', 'Million Pound Club', '🏋️', ['1M', '5M', '10M'], 'Lift 1,000,000 lb together'], ['unbroken', 'Unbroken', '⛓', '', 'Everyone trains in the same week'], ['prstorm', 'PR Storm', '⚡', '', '10 PRs as a crew in one week'], ['fullhouse', 'Full House', '🏠', '', 'Everyone (3+) trains on the same day'], ['ironweek', 'Iron Week', '🛡', '', 'Everyone trains 3+ days in one week'], ['century', 'Century Club', '💯', ['100', '500', '1000'], '100 crew workouts'], ['comeback', 'Comeback Crew', '↩', '', 'Someone comes back after 2+ weeks off']];
function trophyCase() {
  const T = st.cs.crew.trophies || {};
  return `<div class="card"><div class="ghd"><h3>Crew trophy case</h3><span class="sub">${Object.keys(T).length}/${CTROPHY.length}</span></div><div class="tcase">${CTROPHY.map(([k,n,i,tiers,d])=>{const t=T[k];return `<button class="ct${t?' on':''}" data-ctrophy="${k}"><span class="cti">${t?i:'?'}</span><b>${esc(n)}</b>${t&&tiers?`<span class="sub">${tiers[t.tier-1]}</span>`:`<span class="sub">${t?'Earned':'Locked'}</span>`}</button>`}).join('')}</div></div>`
}
function crewRecords() {
  const ms = crewSum().ms;
  if (!ms.length) return '';
  const best = {};
  ms.forEach(m => Object.entries(m.profile.lifts || {}).forEach(([n, l]) => {
    const v = toMine(l.e || 0, m.profile.unit);
    if (!best[n] || v > best[n].v) best[n] = {v, w: toMine(l.w, m.profile.unit), r: l.r, by: m.id}
  }));
  const rows = Object.entries(best).sort((a, b) => b[1].v - a[1].v).slice(0, 6);
  const wk = ms.map(m => ({
    m, v: Math.max(toMine((m.profile.week || {}).vol || 0, m.profile.unit), toMine((m.profile.last || {}).vol || 0, m.profile.unit))
  })).sort((a, b) => b.v - a.v)[0], wo = ms.slice().sort((a, b) => (b.profile.workouts || 0) - (a.profile.workouts || 0))[0];
  return `<div class="card"><div class="ghd"><h3>Crew records</h3><span class="sub">Best ever</span></div>${rows.map(([n,b])=>`<div class="rrow"><span>${esc(n)}</span><span class="sub">${esc((memberOf2(b.by)||{}).name||'')}</span><b>${Math.round(b.w)}×${b.r}</b></div>`).join('')||'<p class="sub">Records show up once crew members log lifts.</p>'}
 ${wk&&wk.v?`<div class="rrow"><span>Biggest week</span><span class="sub">${esc(wk.m.name)}</span><b>${kfmt(wk.v)} ${U()}</b></div>`:''}${wo?`<div class="rrow"><span>Most workouts</span><span class="sub">${esc(wo.name)}</span><b>${wo.profile.workouts||0}</b></div>`:''}</div>`
}
/* joke weekly awards */
function jokeAwards() {
  const ms = crewSum().ms;
  if (ms.length < 2) return '';
  const wk0 = Date.now() - 7 * DAYMS;
  const grunts = ms.slice().sort((a, b) => ((b.profile.last || {}).sets || 0) - ((a.profile.last || {}).sets || 0))[0];
  const talk = {};
  (st.cs.crew.chatRecent || []).forEach(x => {if (x.ts > wk0) talk[x.by] = (talk[x.by] || 0) + 1});
  (st.cs.posts || []).forEach(p => {if (p.ts > wk0 && p.by) talk[p.by] = (talk[p.by] || 0) + 1});
  const tk = Object.entries(talk).sort((a, b) => b[1] - a[1])[0];
  const legs = {};
  ms.forEach(m => legs[m.id] = 0);
  (st.cs.posts || []).forEach(p => {
    if (p.type === 'workout' && p.ts > Date.now() - 14 * DAYMS && legs[p.by] !== undefined) {
      const L = p.data && p.data.L || {};
      legs[p.by] += Object.keys(L).filter(k => /quad|ham|glute|calf|adduct|leg/i.test(k)).length
    }
  });
  const skip = Object.entries(legs).sort((a, b) => a[1] - b[1])[0];
  const A = (i, t, who, why) => `<div class="ja"><span>${i}</span><div><b>${t}</b><div class="sub">${who?esc((memberOf2(who)||{name:'?'}).name):'Nobody yet'} · ${why}</div></div></div>`;
  return `<div class="card"><div class="ghd"><h3>This week's awards</h3><span class="sub">Totally official</span></div>${A('🗣','Most Grunts',grunts&&grunts.id,`${(grunts.profile.last||{}).sets||0} sets last week`)}${A('📢','Biggest Talker',tk&&tk[0],tk?`${tk[1]} posts and messages`:'quiet week')}${A('🦵','Most Likely to Skip Legs',skip&&skip[0],skip&&skip[1]===0?'no leg day posted in 2 weeks':'barely any legs lately')}</div>`
}
const chatUnread = () => {
  if (!st.cs) return 0;
  const me = myId(), seen = st.set.chatSeen || 0;
  return (st.cs.crew.chatRecent || []).filter(x => x.ts > seen && x.by !== me).length
};
async function chatLoad(older) {
  if (st.chat.loading) return;
  st.chat.loading = true;
  const r = await crewCall('chat/get', older ? {before: Math.max(0, st.chat.page - 1)} : {});
  st.chat.loading = false;
  if (r.error) return;
  if (older) {
    const have = new Set(st.chat.msgs.map(m => m.id));
    st.chat.msgs = r.msgs.filter(m => !have.has(m.id)).concat(st.chat.msgs)
  } else {
    const have = new Map(st.chat.msgs.map(m => [m.id, m]));
    r.msgs.forEach(m => have.set(m.id, m));
    st.chat.msgs = [...have.values()].sort((a, b) => a.ts - b.ts)
  }
  st.chat.page = r.page;
  st.chat.more = r.more;
  st.chat.loaded = true;
  chatSeen();
  if (isChatOpen()) render()
}
function isChatOpen() {return st.tab === 'crew' && !st.profile && !st.settings && (st.sub.crew || 'feed') === 'chat'}
function chatSeen() {
  if (!st.cs) return;
  const t = Math.max(st.cs.crew.chatLast || 0, ...st.chat.msgs.map(m => m.ts), 0);
  if (t > (st.set.chatSeen || 0)) {
    st.set.chatSeen = t;
    saveSet()
  }
  updateBellDot()
}
function chatOnMsg(m) {
  if (!st.cs) return;
  st.cs.crew.chatRecent = [...(st.cs.crew.chatRecent || []), {ts: m.ts, by: m.by}].slice(-40);
  st.cs.crew.chatLast = m.ts;
  if (st.chat.loaded && !st.chat.msgs.some(x => x.id === m.id)) st.chat.msgs.push(m);
  if (isChatOpen()) {
    chatSeen();
    if (!document.querySelector('#chatin:focus')) render();
    else chatPaint()
  } else {
    updateBellDot();
    if (m.by !== myId() && st.tab === 'crew' && !st.profile && !st.settings) render()
  }
}
function chatOnUpd(m) {
  const i = st.chat.msgs.findIndex(x => x.id === m.id);
  if (i >= 0) st.chat.msgs[i] = m;
  if (isChatOpen()) chatPaint()
}
function linkify(t) {
  return esc(t).replace(/@([A-Za-z0-9_'’ ]{1,24}?)(?=[\s,.!?]|$)/g, (a, n) => {
    const m = st.cs && st.cs.members.find(x => x.name.toLowerCase() === n.toLowerCase());
    return m ? `<b class="mention">@${esc(m.name)}</b>` : a
  })
}
function msgHTML(m, prev) {
  const me = myId(), who = memberOf2(m.by) || {id: m.by, name: 'Former member'}, mine = m.by === me, group = prev && prev.by === m.by && m.ts - prev.ts < 5 * 6e4;
  if (m.del) return `<div class="msg ${mine?'me':''} del"><div class="bub"><i>Message deleted</i></div></div>`;
  const rx = Object.entries(m.reacts || {}).filter(([k, a]) => a.length).map(([k, a]) => `<button class="mrx${a.includes(me)?' on':''}" data-mreact="${m.id}:${k}">${RE14[k]||''}${a.length>1?' '+a.length:''}</button>`).join('');
  const card = m.card ? `<div class="mcard" style="--pc:${(PC[m.card.color]||PC.red)[0]}"><b>${esc(m.card.day||'Workout')}</b><div class="mct"><span><b>${m.card.min||'–'}</b> min</span><span><b>${m.card.sets||0}</b> sets</span><span><b>${kfmt(toMine(m.card.vol||0,m.card.unit||U()))}</b> ${U()}</span><span><b>${m.card.prs||0}</b> PRs</span></div>${(m.card.top||[]).map(t=>`<div class="sub">${esc(t.n)} ${Math.round(toMine(t.w,m.card.unit||U())*10)/10}×${t.r}${t.pr?' <i class="prt">PR</i>':''}</div>`).join('')}</div>` : '';
  return `<div class="msg ${mine?'me':''}${group?' grp':''}">${mine||group?'':`<span class="mav">${avatar(who,28)}</span>`}<div class="mbody">${mine||group?'':`<div class="mname ${nfxClass(who)}">${esc(who.name)}${shamed(who.id)?' <span class="shame" title="Shame plate">🩷</span>':''}</div>`}
 ${m.reply?`<div class="mrep">↪ <b>${esc((memberOf2(m.reply.by)||{name:'?'}).name)}</b> ${esc(m.reply.text||'')}</div>`:''}<div class="bub">${m.text?`<span>${linkify(m.text)}</span>`:''}${card}<span class="mt">${new Date(m.ts).toLocaleTimeString([],{hour:'numeric',minute:'2-digit'})}</span><button class="mmore" data-mmore="${m.id}" aria-label="Message options">⋯</button></div>${rx?`<div class="mrxs">${rx}</div>`:''}</div></div>`
}
function chatListHTML() {
  const L = st.chat.msgs;
  if (!st.chat.loaded) return '<p class="sub" style="text-align:center;padding:30px 0">Loading chat…</p>';
  if (!L.length) return `<div class="chatempty"><b>Say what's up</b><p class="sub">Chat with your crew. Tag someone with @, reply to a message, or share your last workout.</p></div>`;
  let h = st.chat.more ? `<button class="btn sm" id="chatolder" style="margin:0 auto 10px;display:block">Load older messages</button>` : '', lastDay = '';
  L.forEach((m, i) => {
    const d = new Date(m.ts).toDateString();
    if (d !== lastDay) {
      lastDay = d;
      h += `<div class="mday">${d===new Date().toDateString()?'Today':new Date(m.ts).toLocaleDateString([],{weekday:'short',month:'short',day:'numeric'})}</div>`
    }
    h += msgHTML(m, i && new Date(L[i - 1].ts).toDateString() === d ? L[i - 1] : null)
  });
  return h
}
function chatHTML() {
  if (!st.chat.loaded && !st.chat.loading) chatLoad();
  const muted = (st.cs.crew.muted || {})[myId()];
  const mu = muted && muted > Date.now();
  const mem = st.cs.members.filter(m => m.id !== myId());
  return pinsHTML() + `<div class="chat" id="chatbox">${chatListHTML()}</div><div class="chatbar">${st.chat.reply?`<div class="crep"><span>Replying to <b>${esc((memberOf2(st.chat.reply.by)||{name:'?'}).name)}</b>: ${esc((st.chat.reply.text||'').slice(0,60))}</span><button id="crepx" aria-label="Cancel reply">✕</button></div>`:''}
 <div class="ment hide" id="mentbox">${mem.map(m=>`<button data-ment="${esc(m.name)}">@${esc(m.name)}</button>`).join('')}</div>
 ${mu?`<p class="sub" style="text-align:center">The owner muted you until ${new Date(muted).toLocaleTimeString([],{hour:'numeric',minute:'2-digit'})}</p>`:`<div class="cin"><button class="cplus" id="chatplus" aria-label="Share">＋</button><input id="chatin" class="field" maxlength="500" placeholder="Message ${esc(st.cs.crew.name)}" autocomplete="off" enterkeyhint="send"><button class="btn acc" id="chatsend" aria-label="Send">↑</button></div>`}</div>`
}
function chatPaint() {
  const b = $('chatbox');
  if (!b) return;
  const atEnd = b.scrollHeight - b.scrollTop - b.clientHeight < 80;
  b.innerHTML = chatListHTML();
  if (atEnd) b.scrollTop = b.scrollHeight
}
async function chatSend(extra) {
  const inp = $('chatin'), text = (inp && inp.value || '').trim();
  if (!text && !(extra && extra.card)) return;
  const body = {text, reply: st.chat.reply && st.chat.reply.id, ...(extra || {})};
  if (inp) inp.value = '';
  st.chat.reply = null;
  haptic('tap');
  const tmp = {
    id: 'tmp' + Date.now(), by: myId(), ts: Date.now(), text, card: body.card || null, reacts: {}, reply: null, pending: 1
  };
  st.chat.msgs.push(tmp);
  render();
  const r = await crewCall('chat/send', body);
  st.chat.msgs = st.chat.msgs.filter(m => m !== tmp);
  if (r.error) {
    toast(r.error === 'muted' ? 'You’re muted for now' : r.error === 'slow down' ? 'Slow down a sec' : 'Message not sent. Check your connection');
    render();
    const i2 = $('chatin');
    if (i2) i2.value = text;
    return
  } else if (!st.chat.msgs.some(m => m.id === r.msg.id)) st.chat.msgs.push(r.msg);
  st.set.chatN = (st.set.chatN || 0) + 1;
  saveSet();
  render();
  setTimeout(() => {
    const b = $('chatbox');
    if (b) b.scrollTop = b.scrollHeight
  }, 30)
}
/* ---------- shame plate + name effects ---------- */
const shamed = id => {
  const s = st.cs && st.cs.crew.shame;
  return !!(s && s.target === id && s.until > Date.now())
};
function nfxClass(m) {
  const p = m && m.profile || {};
  return p.nameFx && p.nameFx !== 'none' ? 'nf-' + String(p.nameFx).replace(/[^a-z]/g, '') : ''
}
/* ---------- reactions ---------- */
const RE14 = {
  fire: '🔥', muscle: '💪', trophy: '🏆', clap: '👏', heart: '❤️', crown: '👑', rocket: '🚀', skull: '💀', goat: '🐐', lol: '😂'
};
/* ---------- roast lines on crew posts ---------- */
function roast(p) {
  if (st.set.roasts === false || p.type !== 'workout') return '';
  const d = p.data || {}, L = d.L || {}, legs = Object.keys(L).some(k => /quad|ham|glute|calf|adduct|leg/i.test(k)), u = d.unit || 'lb', v = Math.round(toMine(d.vol || 0, u)), pool = [];
  if (!legs && Object.keys(L).length) pool.push('Legs filed a missing persons report', 'Upper body day again? The legs are drafting a strongly worded letter');
  if (legs) pool.push('Leg day survived. Stairs are now the enemy', 'Walking funny tomorrow, guaranteed');
  if (d.prs >= 2) pool.push(`${d.prs} PRs? Somebody check this person for performance enhancing vibes`);
  else if (d.prs === 1) pool.push('PR alert. Neighbors felt that one');
  if (v > 20000) pool.push(`${kfmt(v)} ${U()} moved. The floor wants a break`);
  if (d.sets >= 25) pool.push(`${d.sets} sets. Somebody is not going home early`);
  if (d.min && d.min < 30) pool.push(`In and out in ${d.min} min. Speedrun or skipped half? We’ll never know`);
  if (d.min >= 100) pool.push(`${d.min} minutes. Did you lift or move in?`);
  pool.push('Solid work. The mirror is impressed', 'Certified gym rat behavior', 'Another one for the highlight reel');
  return `<p class="roast">🤖 ${esc(pool[hashS(p.id)%pool.length])}</p>`
}
/* ---------- crew tab ---------- */
function renderCrew14() {
  const sub = st.sub.crew || 'feed';
  if (!crewOn()) return `<div class="ghd"><h2>Crew</h2></div>` + crewEmptyHTML();
  if (!st.cs) {
    syncCrew(true);
    return `<div class="ghd"><h2>Crew</h2></div><div class="card"><p class="sub">Loading your crew…</p></div>`
  }
  setTimeout(() => syncCrew(), 50);
  wsConnect();
  const c = st.cs.crew, cu = chatUnread();
  let h = crewHead14() + seg('crew', [['feed', 'Feed'], ['chat', 'Chat' + (cu ? ` (${cu>9?'9+':cu})` : '')], ['arena', 'Arena']], sub);
  if (sub === 'chat') return h + chatHTML();
  if (sub === 'feed') {
    const posts = st.cs.posts || [];
    h += raidCard() + liveStrip() + recapHTML();
    const s = lastFinished();
    h += s && !s.posted ? `<button class="card postnudge" id="postlast"><span>📣</span><div><b>Post your ${esc(postData(s).day)}</b><div class="sub">Share your breakdown with the crew</div></div></button>` : '';
    h += posts.length ? feed17(posts) : `<div class="card"><p class="sub">No posts yet. Finish a workout and tap Post to crew, or share the code <b>${esc(c.code)}</b> so friends can join.</p></div>`
  } else h += arena15();
  return h
}
function trophyPostHTML(p) {
  const d = p.data || {};
  const t = CTROPHY.find(x => x[1] === d.name) || ['', 'Crew trophy', '🏆'];
  return `<div class="post ptrophy"><div class="ptro"><span>${t[2]}</span><div><div class="sub">Crew trophy unlocked</div><b>${esc(d.name||'Trophy')}</b><div class="sub">${ago(p.ts)}</div></div></div></div>`
}
/* crew card image */
async function crewCardImage() {
  const c = st.cs.crew, R = crewRankInfo(), S = crewSum(), W = 1080, H = 1350, cv = document.createElement('canvas');
  cv.width = W;
  cv.height = H;
  const x = cv.getContext('2d');
  const load = src => new Promise(r => {
    const im = new Image();
    im.onload = () => r(im);
    im.onerror = () => r(null);
    im.src = src
  });
  const sv = sceneSVG(c.banner, c.level, 'card' + c.code).replace('<svg class="scene"', '<svg xmlns="http://www.w3.org/2000/svg" width="1080" height="370"');
  const bg = await load('data:image/svg+xml;charset=utf-8,' + encodeURIComponent(sv));
  x.fillStyle = '#0b0c10';
  x.fillRect(0, 0, W, H);
  if (bg) x.drawImage(bg, 0, 0, W, 370);
  const g = x.createLinearGradient(0, 250, 0, 420);
  g.addColorStop(0, 'rgba(11,12,16,0)');
  g.addColorStop(1, '#0b0c10');
  x.fillStyle = g;
  x.fillRect(0, 250, W, 170);
  const cr = await load('data:image/svg+xml;charset=utf-8,' + encodeURIComponent(crestSVG(c.crest, R.r, 220).replace('<svg ', '<svg xmlns="http://www.w3.org/2000/svg" ')));
  if (cr) x.drawImage(cr, W / 2 - 110, 260, 220, 220);
  x.textAlign = 'center';
  x.fillStyle = '#fff';
  x.font = '900 84px Archivo,system-ui,sans-serif';
  x.fillText(c.name, W / 2, 580);
  x.font = '600 34px Inter,system-ui,sans-serif';
  x.fillStyle = '#9aa0ae';
  x.fillText(`${RANKS[R.r][0]} crew · Level ${c.level||1}${c.founders?' · Founders':''}`, W / 2, 632);
  const stats = [[kfmt(S.vol), U() + ' lifted'], [String(S.wo), 'workouts'], [String(Object.keys(c.trophies || {}).length), 'trophies'], [String(st.cs.members.length), 'members']];
  stats.forEach(([v, l], i) => {
    const cx = W / 2 + (i - 1.5) * 250;
    x.fillStyle = '#fff';
    x.font = '900 72px Archivo,system-ui,sans-serif';
    x.fillText(v, cx, 800);
    x.fillStyle = '#9aa0ae';
    x.font = '600 30px Inter,system-ui,sans-serif';
    x.fillText(l, cx, 846)
  });
  x.fillStyle = '#ef2b36';
  x.font = '800 40px Inter,system-ui,sans-serif';
  x.fillText('Join with code ' + c.code, W / 2, 1040);
  x.fillStyle = '#6b7080';
  x.font = '600 30px Inter,system-ui,sans-serif';
  x.fillText('jscarp9.github.io/Overload', W / 2, 1100);
  x.fillText('OVERLOAD', W / 2, 1260);
  const blob = await new Promise(r => cv.toBlob(r, 'image/png'));
  return {
    blob, url: cv.toDataURL('image/png'), file: new File([blob], 'crew-card.png', {type: 'image/png'})
  }
}

/* ---------- bosses ---------- */
function bossSVG(k, sz) {
  const s = sz || 86, g = '#2a1640', l = '#8b5cf6', e = '#ffd23f';
  const B = {
    titan: `<path d="M20 86V44q0-24 30-26 30 2 30 26v42z" fill="${g}"/><path d="M30 40h40v10H30z" fill="${l}" opacity=".6"/><rect x="34" y="42" width="10" height="5" fill="${e}"/><rect x="56" y="42" width="10" height="5" fill="${e}"/><path d="M38 64h24M40 70h20" stroke="${l}" stroke-width="3"/>`,
    golem: `<circle cx="50" cy="56" r="30" fill="${g}"/><circle cx="50" cy="56" r="10" fill="#120a1c"/><circle cx="50" cy="56" r="20" fill="none" stroke="${l}" stroke-width="3"/><circle cx="42" cy="30" r="4" fill="${e}"/><circle cx="58" cy="30" r="4" fill="${e}"/><rect x="34" y="18" width="32" height="20" rx="6" fill="${g}" opacity=".9"/><circle cx="43" cy="28" r="3" fill="${e}"/><circle cx="57" cy="28" r="3" fill="${e}"/>`,
    hydra: `${[28,50,72].map((x,i)=>`<path d="M50 90Q${x} 60 ${x} 30" stroke="${g}" stroke-width="10" fill="none" stroke-linecap="round"/><circle cx="${x}" cy="26" r="9" fill="${g}"/><circle cx="${x+3}" cy="24" r="2" fill="${e}"/>`).join('')}`,
    kraken: `<ellipse cx="50" cy="36" rx="22" ry="20" fill="${g}"/>${[22,36,50,64,78].map((x,i)=>`<path d="M${40+i*5} 52Q${x} 74 ${x+(i%2?6:-6)} 90" stroke="${g}" stroke-width="6" fill="none" stroke-linecap="round"/>`).join('')}<circle cx="42" cy="36" r="4" fill="${e}"/><circle cx="58" cy="36" r="4" fill="${e}"/>`,
    dragon: `<path d="M50 30L12 14l14 30-10 10 34-8 34 8-10-10 14-30z" fill="${g}"/><path d="M40 46q10 30 10 44 0-14 10-44" fill="${l}" opacity=".7"/><circle cx="44" cy="34" r="3" fill="${e}"/><circle cx="56" cy="34" r="3" fill="${e}"/><path d="M42 20l-4-10M58 20l4-10" stroke="${g}" stroke-width="4" stroke-linecap="round"/>`,
    minotaur: `<path d="M30 30Q14 26 12 10q14 8 24 12M70 30q16-4 18-20-14 8-24 12" fill="#d8c9a8"/><path d="M26 30h48v30q0 26-24 30-24-4-24-30z" fill="${g}"/><circle cx="40" cy="46" r="4" fill="${e}"/><circle cx="60" cy="46" r="4" fill="${e}"/><circle cx="50" cy="72" r="6" fill="none" stroke="#d8c9a8" stroke-width="2.4"/>`,
    yeti: `<path d="M18 90q-4-50 32-66 36 16 32 66z" fill="#d9e4f2"/><path d="M34 44h32v20H34z" fill="#8ea4c2" rx="6"/><circle cx="43" cy="52" r="3" fill="#1a2540"/><circle cx="57" cy="52" r="3" fill="#1a2540"/><path d="M42 60l4 3 4-3 4 3 4-3" stroke="#fff" stroke-width="1.6" fill="none"/>`,
    phoenix: `<path d="M50 88q-8-20 0-40 8 20 0 40z" fill="#ff7a1f"/><path d="M48 50Q30 40 12 16q22 6 36 28zM52 50q18-10 36-34-22 6-36 28z" fill="#ffb347"/><circle cx="50" cy="40" r="8" fill="#ff7a1f"/><circle cx="52" cy="38" r="2" fill="#fff"/>`
  };
  return `<svg viewBox="0 0 100 100" width="${s}" height="${s}" class="boss" aria-hidden="true">${B[k]||B.titan}</svg>`
}
function raidCard() {
  const r = st.cs && st.cs.raid;
  if (!r) return '';
  const me = myId(), mine = (r.hits || {})[me] || 0, pct = Math.min(100, Math.round(r.dmg / r.hp * 100)), end = new Date(toD(r.wk).getTime() + 7 * DAYMS);
  const left = Math.max(0, Math.ceil((end - Date.now()) / DAYMS));
  return `<button class="card raid${r.beaten?' won':''}" id="raidbtn"><div class="raidart">${bossSVG(r.boss,70)}</div><div class="raidin"><div class="sub">${r.beaten?'BOSS DEFEATED':'WEEKLY BOSS RAID · '+left+' day'+(left===1?'':'s')+' left'}</div><b>${esc(r.name)}</b><div class="hpbar"><u style="width:${r.beaten?0:100-pct}%"></u></div><div class="sub">${r.beaten?'Loot unlocked for everyone who hit it':`${kfmt(r.hp-r.dmg)} ${'lb'} HP left · you hit ${kfmt(mine)}`}</div></div></button>`
}
function raidSheet() {
  const r = st.cs.raid, rows = Object.entries(r.hits || {}).sort((a, b) => b[1] - a[1]);
  return `<div class="ghd"><h2>${esc(r.name)}</h2><button class="btn sm" id="sheetx">Done</button></div><div style="text-align:center">${bossSVG(r.boss,140)}</div>
 <p class="sub" style="text-align:center">${r.beaten?'Defeated! ':''}Every pound your crew lifts this week does damage. HP grows with crew size.</p>
 <div class="hpbar big"><u style="width:${r.beaten?0:100-Math.min(100,Math.round(r.dmg/r.hp*100))}%"></u></div><p style="text-align:center"><b>${kfmt(r.dmg)}</b> / ${kfmt(r.hp)} lb</p>
 <div class="glbl">DAMAGE</div>${rows.map(([id,v],i)=>`<div class="rrow"><span>${i===0?'🥇 ':''}${esc((memberOf2(id)||{name:'?'}).name)}</span><span></span><b>${kfmt(v)} lb</b></div>`).join('')||'<p class="sub">No hits yet. Go lift.</p>'}
 <div class="glbl" style="margin-top:12px">LOOT</div><p class="sub">Beat it before Monday: +300 crew XP, a mystery box for everyone who hit it, and the Boss Slayer plate. Raids won: <b>${r.wins||0}</b></p>`
}
/* raid wins give mystery boxes */
{
  const _bs = boxSources;
  boxSources = function(E) {
    const out = _bs(E);
    try {
      (st.cs && st.cs.raid && st.cs.raid.won || []).forEach(w => {if ((w.by || []).includes(myId())) out.push('RAID' + w.wk)})
    } catch (e) {}return [...new Set(out)]
  }
}
/* ---------- lift together ---------- */
let GRP = null;
try {GRP = localStorage.getItem('overload-grp') || null} catch (e) {}
const setGrp = g => {
  GRP = g;
  try {g ? localStorage.setItem('overload-grp', g) : localStorage.removeItem('overload-grp')} catch (e) {}
};
function grpLive() {
  const s = liveSession();
  if (!s) return {};
  const sess = cur();
  let sets = 0, vol = 0, ex = '', last = 0;
  Object.entries(sess.ex || {}).forEach(([id, a]) => (a || []).forEach(x => {if (full(x)) {sets++; vol += (+x.w) * (+x.r); if ((x.t || 0) >= last) {last = x.t || 0; ex = exName(id)}}}));
  return {
    g: GRP || '', sets, vol: Math.round(vol), ex, rest: st.rest && st.rest.end > Date.now() ? st.rest.end : 0, prs: Object.keys(sess.prShown || {}).length, unit: U()
  }
}
function activeGroups() {return Object.entries(st.cs && st.cs.groups || {})}
function groupBar() {
  if (!st.cs || !crewOn()) return '';
  const G = st.cs.groups || {};
  if (GRP && !G[GRP]) {setGrp(null)}
  if (!GRP) {
    const act = activeGroups().filter(([g, x]) => !x.members.includes(myId()));
    if (act.length) {
      const [g, x] = act[0];
      const host = memberOf2(x.host) || {name: 'Someone'};
      return `<button class="card grpinv" data-gjoin="${g}"><span>👥</span><div><b>${esc(host.name)} is lifting${x.members.length>1?` with ${x.members.length-1} more`:''}</b><div class="sub">Tap to lift together</div></div><span class="chev">›</span></button>`
    }
    return `<button class="liftto" id="grpbtn">👥 Lift together</button>`
  }
  const g = G[GRP], L = st.liveMap || {};
  return `<div class="card grp"><div class="ghd"><b>👥 Lifting together · ${g.members.length}</b><span class="sub">${Math.max(1,Math.round((Date.now()-g.ts)/60000))} min</span></div><div class="grpm">${g.members.map(id=>{const m=memberOf2(id)||{id,name:'?'},l=id===myId()?grpLive():(L[id]||{});const rest=l.rest&&l.rest>Date.now();return `<div class="gm"><span>${avatar(m,34)}</span><b>${esc(id===myId()?'You':m.name)}</b><span class="sub">${l.sets||0} sets</span><span class="gst${rest?' rest':''}" ${rest?`data-rend="${l.rest}"`:''}>${rest?'Resting '+fmtLeft(l.rest):esc(l.ex||'Warming up')}</span></div>`}).join('')}</div><div class="exfoot"><button class="btn acc" id="grpend">Finish together</button><button class="btn" id="grpleave">Leave</button></div></div>`
}
const fmtLeft = t => {
  const s = Math.max(0, Math.round((t - Date.now()) / 1000));
  return Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0')
};
function groupPostHTML(p) {
  const d = p.data || {}, rows = (d.rows || []).slice().sort((a, b) => b.vol - a.vol), tot = rows.reduce((a, r) => a + r.vol, 0);
  return `<div class="post pgrp"><div class="ptro"><span>👥</span><div><div class="sub">Lifted together · ${d.min||0} min · ${ago(p.ts)}</div><b>${rows.map(r=>esc((memberOf2(r.id)||{name:'?'}).name)).join(' + ')}</b></div></div><div class="grows">${rows.map((r,i)=>`<div class="rrow"><span>${i===0?'🥇 ':''}${esc((memberOf2(r.id)||{name:'?'}).name)}</span><span class="sub">${r.sets} sets${r.prs?` · ${r.prs} PR`:''}</span><b>${kfmt(toMine(r.vol,'lb'))} ${U()}</b></div>`).join('')}</div><div class="sub" style="margin-top:6px">Together: <b>${kfmt(toMine(tot,'lb'))} ${U()}</b></div></div>`
}
function raidPostHTML(p) {
  const d = p.data || {};
  return `<div class="post ptrophy"><div class="ptro">${bossSVG(d.boss,56)}<div><div class="sub">Boss defeated · ${ago(p.ts)}</div><b>${esc(d.name||'Boss')}</b><div class="sub">${Object.keys(d.dmg||{}).length} lifters took it down</div></div></div></div>`
}
try {
  Object.defineProperty(RE, 'crest', {get() {return RE14.crest}, enumerable: true, configurable: true})
} catch (e) {}
LOCK.react.items.crest = ['Crew crest', '', 'Be in a crew', () => !!st.cs];
function pinsHTML() {
  const P = st.cs && st.cs.pins || [];
  if (!P.length) return '';
  return `<details class="pinsb"><summary>📌 Pinned · ${P.length}</summary>${P.map(x=>`<div class="pinm"><b>${esc((memberOf2(x.by)||{name:'?'}).name)}</b> ${esc(x.text||(x.card?'Workout: '+x.card.day:''))}</div>`).join('')}</details>`
}
function hofHTML() {
  const H = st.cs && st.cs.hof || [];
  return `<div class="card"><div class="ghd"><h3>🏛 Hall of Fame</h3><span class="sub">${H.length}</span></div>${H.length?H.map(x=>`<div class="hofr"><span>${x.kind==='member'?'⭐':'🏋️'}</span><div><b>${esc(x.kind==='member'?x.name:(memberOf2(x.who)||{name:'?'}).name)}</b><div class="sub">${esc(x.text||'')}${x.data?` · ${esc(x.data.day||'')} ${x.data.vol?kfmt(toMine(x.data.vol,x.data.unit||'lb'))+' '+U():''}`:''}</div></div>${isOwner()?`<button class="mmore" data-hofdel="${x.id}" aria-label="Remove">✕</button>`:''}</div>`).join(''):`<p class="sub">Legendary sessions and members live here forever. ${isOwner()?'Induct a post from its ⋯ button or a member from their profile.':'The crew owner inducts them.'}</p>`}</div>`
}
function raidWinsHTML() {
  const r = st.cs.raid;
  if (!r || !(r.won || []).length) return '';
  return `<div class="card"><div class="ghd"><h3>Bosses defeated</h3><span class="sub">${r.won.length}</span></div><div class="lkrow">${r.won.slice(-8).reverse().map(w=>`<span class="lkc">${esc(w.name)}<span class="sub">${esc(w.wk)}</span></span>`).join('')}</div></div>`
}
/* Lift together and Need an excuse live in the ⋯ menu */
{
  const _g = groupBar;
  groupBar = function() {
    const h = _g();
    return /id="grpbtn"/.test(h) ? '' : h
  }
}

/* ---------- Crew feed: slim joins, compact reactions ---------- */
function feed17(posts) {
  const J = posts.filter(p => p.type === 'join');
  let out = '', done = false;
  posts.forEach(p => {
    if (p.type !== 'join') {
      out += postHTML(p);
      return
    }
    if (done) return;
    done = true;
    const names = [...new Set(J.map(x => (memberOf2(x.by) || {name: 'Someone'}).name))];
    const nm = names.length > 3 ? names.slice(0, 3).join(', ') + ' and ' + (names.length - 3) + ' more' : names.length > 1 ? names.slice(0, -1).join(', ') + ' and ' + names.slice(-1) : names[0];
    out += `<div class="join17">👋 <b>${esc(nm)}</b> joined the crew<span class="sub"> · ${ago(J[0].ts)}</span></div>`
  });
  return out
}
{
  const _p = postHTML;
  postHTML = function(p, preview) {
    let h = _p(p, preview);
    if (preview || !/class="prx"/.test(h)) return h;
    const open = st.rx17 === p.id;
    if (open) return h.replace('<div class="prx">', `<div class="prx open">`).replace(/(<button class="rx cm")/, `<button class="rx add" data-rxo="${p.id}" aria-label="Close reactions">✕</button>$1`);
    return h.replace(/<div class="prx">([\s\S]*?)(<button class="rx cm")/, (m, rx, cm) => {
      const kept = (rx.match(/<button class="rx[^"]*"[^>]*>[^<]*<\/button>/g) || []).filter(b => /\s\d+<\/button>$/.test(b)).join('');
      return `<div class="prx">${kept}<button class="rx add" data-rxo="${p.id}" aria-label="React">＋</button>${cm}`
    })
  }
}
/* ---------- block and report ---------- */
{
  const _f = feed17;
  feed17 = function(posts) {
    const B = st.set.blocked || [];
    return _f(B.length ? posts.filter(p => !B.includes(p.by)) : posts)
  }
}
{
  const _c = chatListHTML;
  chatListHTML = function() {
    const B = st.set.blocked || [];
    if (!B.length || !st.chat || !st.chat.msgs) return _c();
    const keep = st.chat.msgs;
    st.chat.msgs = keep.filter(m => !B.includes(m.by));
    try {return _c()} finally {st.chat.msgs = keep}
  }
}
const REP_WHY = ['Spam', 'Rude or bullying', 'Inappropriate', 'Fake or cheating stats', 'Something else'];
{
  const _cs = crewSheet;
  crewSheet = function(sh) {
    let h = _cs(sh);
    if (h && sh.type === 'crewinfo') h = h.replace(/(<\/div>)/, '$1<button class="btn acc wide" id="invite18" style="min-height:50px;margin:4px 0 12px">📨 Invite friends</button>');
    if (h && sh.type === 'crewinfo' && isOwner()) {
      const R = (st.cs.crew.reports || []).slice().reverse();
      if (R.length) h += `<div class="card"><h3>🚩 Reports (${R.length})</h3>${R.slice(0,10).map(x=>`<div class="rep18"><b>${esc((memberOf2(x.target)||{name:'Someone'}).name)}</b> · ${esc(x.why||'')}<span class="sub"> · by ${esc((memberOf2(x.by)||{name:'Someone'}).name)} · ${ago(x.ts)}</span></div>`).join('')}<p class="sub">Reports are only visible to you as the crew owner. Nothing gets deleted automatically.</p></div>`
    }
    return h
  }
}
/* ---------- invite ---------- */
function inviteText() {
  const c = st.cs.crew, u = location.origin + location.pathname + '?join=' + encodeURIComponent(c.code);
  return {
    u, t: `Join my crew "${c.name}" on Overload 💪\n1. Open ${u}\n2. Add it to your Home Screen\n3. Crew tab → enter code ${c.code}`
  }
}
async function shareInvite() {
  const {u, t} = inviteText();
  try {
    if (navigator.share) {
      await navigator.share({title: 'Join my crew on Overload', text: t});
      return
    }
  } catch (e) {if (e && e.name === 'AbortError') return}try {
    await navigator.clipboard.writeText(t);
    toast('Invite copied. Paste it to your friends')
  } catch (e) {
    st.sheet = {type: 'invite18'};
    renderSheet()
  }
}/* ---------- crew screen: tidy banner with stats inside, 4 arena tabs, no repeats ---------- */
crewHead14 = function() {
  const c = st.cs.crew, lv = c.level || 1, a = crewXpFor(lv), z = crewXpFor(lv + 1), n = bellUnread(), live = Object.keys(st.liveMap || {}).length, R = crewRankInfo(), S = crewSum(), founded = new Date(c.created || Date.now()), pc = Math.min(100, Math.round(((c.xp || 0) - a) / (z - a) * 100));
  return `<div class="crewban2 cb20"${st.cs.style&&st.cs.style.c1?` style="--sc1:${esc(st.cs.style.c1)};--sc2:${esc(st.cs.style.c2||st.cs.style.c1)}"`:''}>${sceneSVG(c.banner,lv,'crew'+c.code)}<div class="cb20shade"></div>
 <div class="cb20btns"><button class="ic20" id="bellbtn" aria-label="Activity${n?`, ${n} new`:''}">🔔${n?`<i>${n>9?'9+':n}</i>`:''}</button><button class="ic20" id="crinfo" aria-label="Crew settings">⚙︎</button></div>
 <div class="cb2in"><button class="cb2crest" id="crestbtn" aria-label="Crew crest">${crestSVG(c.crest,R.r,58)}</button><div class="cb2t"><h2>${esc(c.name)}</h2><div class="cb2s">${c.founders?'<span class="fbadge">★ Founders</span>':''}<span>Est. ${MON[founded.getMonth()]} ${founded.getFullYear()}</span></div>${st.cs.style&&st.cs.style.motto?`<div class="cb2motto">“${esc(st.cs.style.motto)}”</div>`:''}</div></div>
 <div class="cb20stats"><span><b>${kfmt(S.vol)}</b> ${U()}</span><i>·</i><span><b>${S.streak}</b> wk streak</span><i>·</i><button class="cb20rk" id="crewrankbtn">${emblem(R.r)}<b>${RANKS[R.r][0]}</b></button><i>·</i><span class="cb20lv"><b>Lv ${lv}</b><span class="cbbar2"><u style="width:${pc}%"></u></span></span></div>
 ${live?`<div class="cb2live"><span class="ldot"></span>${live} training now</div>`:''}</div>`
};
arena15 = function() {
  const k = st.sub.arena2 || 'rank';
  let h = `<div class="pchips arena20">${[['rank','Rankings'],['tro','Trophies'],['rec','Records'],['fun','Awards']].map(([a,b])=>`<button class="pchip${a===k?' on':''}" data-arena20="${a}">${b}</button>`).join('')}</div>`;
  if (k === 'rank') {
    let a = arenaHTML().replace('<div class="seg2" role="tablist">', '<div class="seg2 seg20" role="tablist">');
    let n = 0;
    a = a.replace(/<button class="arow[^"]*"[\s\S]*?<\/button>/g, m => (n++<3 ? '' : m));
    a = a.replace('<div class="card alist"></div>', '');
    h += a
  } else if (k === 'tro') h += trophyCase() + raidWinsHTML() + hofHTML() + `<button class="btn wide" id="crewcard" style="margin:4px 0 14px">📸 Share crew card</button>`;
  else if (k === 'rec') h += crewRecords() + seasonWall();
  else h += jokeAwards() + (typeof awardsHTML === 'function' ? awardsHTML() : '');
  return h
};
async function click20(b) {
  const d = b.dataset;
  if (d.arena20) {
    st.sub.arena2 = d.arena20;
    render();
    return true
  }
  if (d.arena17) {
    const a = d.arena17;
    if (a.startsWith('r_')) {
      st.sub.arena2 = 'rank';
      st.sub.arena = a.slice(2)
    } else st.sub.arena2 = a;
    render();
    return true
  }
  return false
}
function boot20() {
  try {
    const L = st.set.look;
    if (L && OLDF20[L.pbanner]) {
      L.pbanner = OLDF20[L.pbanner];
      saveSet()
    }
  } catch (e) {}
}/* ---------- arena: no more Lifts board ---------- */
{
  const _a = arenaHTML;
  arenaHTML = function() {
    if (st.sub.arena === 'lifts') st.sub.arena = 'all';
    return _a().replace(/<button data-seg="arena:lifts"[^>]*>Lifts<\/button>/, '')
  }
}
