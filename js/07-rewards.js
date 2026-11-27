/* ========================================================================
   Overload: Rewards

   Badges, ranks, challenges, seasons, unlocks, the locker, trophies, medals, holiday drops.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

/* ---------- 0.10: badges, ranks, challenges ---------- */
const RANKS = [['Iron', 0, 0, '#5b6069', '#b3b8c1'], ['Bronze', 2, 3, '#8a4f1f', '#f0b27a'], ['Silver', 6, 8, '#7d838d', '#f4f6f9'], ['Gold', 12, 15, '#a97a05', '#ffe07a'], ['Platinum', 20, 25, '#2f948f', '#c6f5f0'], ['Diamond', 32, 35, '#2468d6', '#cfe6ff'], ['Titan', 45, 45, '#6d2fe0', '#e6c4ff'], ['Olympian', 52, 60, '#b88600', '#fff3b0']];
const UNLOCKS = [[1, 'Pink and teal plate colors'], [2, 'Shiny badges'], [3, 'Gold plate color'], [4, 'Chrome plate color'], [5, 'Neon welcome screen'], [6, 'Animated rank emblem'], [7, 'Gold welcome plate']];
const PLATE_LOCK = {pink: 1, teal: 1, gold: 3, chrome: 4};
const isHoliday = d => {
  const t = toD(d), m = t.getMonth(), day = t.getDate();
  if ((m === 0 && day === 1) || (m === 6 && day === 4) || (m === 11 && (day === 25 || day === 31))) return true;
  if (m === 10 && t.getDay() === 4 && day >= 22 && day <= 28) return true;
  return false
};
const hrOf = s => s.start ? new Date(s.start).getHours() : null;
let ENG = null;
const dirtyEng = () => {
  ENG = null;
  RSC = null
};
function hashS(str) {
  let h = 2166136261;
  for (const c of str) {
    h^=c.charCodeAt(0);
    h = Math.imul(h, 16777619)
  }
  return h >>> 0
}
function pick(pool, n, seed) {
  const a = pool.slice(), out = [];
  let s = seed || 1;
  while (out.length < n && a.length) {
    s = (s * 1103515245 + 12345) >>> 0;
    out.push(a.splice(s % a.length, 1)[0])
  }
  return out
}
function lastBefore(id, date) {
  const rs = realSessions();
  for (let i = rs.length - 1; i >= 0; i--) {
    const s = rs[i];
    if (s.date < date && s.ex && (s.ex[id] || []).some(full)) return s
  }
  return null
}
function beatCount(s) {
  let n = 0;
  Object.entries(s.ex || {}).forEach(([id, a]) => {
    const l = lastBefore(id, s.date);
    if (!l) return;
    const la = l.ex[id] || [];
    if ((a || []).some((x, i) => better(x, la[i] || la[la.length - 1]))) n++
  });
  return n
}
const plannedSetsFor = d => (d && d.groups || []).reduce((t, g) => t + g.reduce((u, e) => u + (+e.sets || 2), 0), 0);
const sessOn = d => st.sessions[d + '_' + DAYS[dayIdx(d)]];
function dayChallenges(d) {
  const pl = st.plan.days[dayIdx(d)] || {}, rest = pl.type === 'off' || !pl.groups || !pl.groups.length, seed = hashS('c' + d), f = (st.flags[d] || {});
  if (rest) {
    const pool = [['rnote', 'Write something on your Notes board', () => st.notes.some(n => n.ts && iso(new Date(n.ts)) === d)], ['rprog', 'Check how a lift is trending in Progress', () => !!f.prog], ['rplan', 'Look over your next workout in Plan', () => !!f.plan], ['rbk', 'Save a backup', () => st.set.lastBackup && iso(new Date(st.set.lastBackup)) === d], ['rwalk', 'Go for a recovery walk (tap to check off)', () => !!f.walk, 1]];
    return pick(pool, 3, seed).map(([k, t, fn, man]) => ({k, t, done: !!fn(), man}))
  }
  const s = sessOn(d) || {ex: {}}, ps = plannedSetsFor(pl), prev = realSessions().filter(x => x.day === DAYS[dayIdx(d)] && x.date < d).pop(), tv = prev ? Math.round(sVol(prev) / 100) * 100 : 0;
  const pool = [['beat', 'Beat last time on at least 1 lift', () => beatCount(s) >= 1], ['sets', `Log ${ps||12} sets`, () => sSets(s) >= (ps || 12)], ['fast', 'Finish in under 60 minutes', () => s.end && elapsed(s) < 36e5], ['pr', 'Hit a new PR', () => sessStats(s).prs.length > 0], ['note', 'Add a note to an exercise', () => Object.values(s.exNotes || {}).some(v => v && v.trim())]];
  if (tv) pool.push(['vol', `Move ${tv.toLocaleString()} ${U()} or more`, () => sVol(s) >= tv]);
  return pick(pool, 3, seed).map(([k, t, fn]) => ({k, t, done: !!fn()}))
}
function weekQuests(ws, wd) {
  const seed = hashS('q' + ws), pl = st.plan.days, planned = pl.filter(x => x.type !== 'off').length, psw = pl.reduce((t, x) => t + (x.type === 'off' ? 0 : plannedSetsFor(x)), 0);
  const inW = realSessions().filter(s => s.date >= wd[0] && s.date <= wd[6]), prevWd = weekDates(iso(new Date(toD(wd[0]).getTime() - 7 * 864e5))), prevV = realSessions().filter(s => s.date >= prevWd[0] && s.date <= prevWd[6]).reduce((a, s) => a + sVol(s), 0), tv = prevV ? Math.round(prevV * 1.05 / 500) * 500 : 20000;
  const prLifts = new Set();
  inW.forEach(s => sessStats(s).prs.forEach(id => prLifts.add(id)));
  const days = new Set(inW.map(s => s.date)).size, sets = inW.reduce((a, s) => a + sSets(s), 0), vol = inW.reduce((a, s) => a + sVol(s), 0), fast = inW.filter(s => s.start && s.end && elapsed(s) < 36e5).length;
  const planM = new Set();
  pl.forEach(x => (x.groups || []).forEach(g => g.forEach(e => planM.add(e.m))));
  const hitM = new Set();
  inW.forEach(s => Object.keys(muscleSets(s)).forEach(k => hitM.add(k)));
  const pool = [['pr2', 'PR on 2 different lifts', prLifts.size, 2], ['every', 'Train every planned day', days, Math.max(1, planned)], ['sets', `Log ${psw||40} sets`, sets, psw || 40], ['vol', `Move ${tv.toLocaleString()} ${U()}`, Math.round(vol), tv], ['fast3', '3 workouts under 60 minutes', fast, 3], ['musc', 'Train every muscle in your plan', [...planM].filter(k => hitM.has(k)).length, Math.max(1, planM.size)]];
  return pick(pool, 3, seed).map(([k, t, c, n]) => ({k, t, c: Math.min(c, n), n, done: c >= n}))
}
function engine() {
  if (ENG) return ENG;
  const rs = realSessions(), pl = st.plan.days, planned = Math.max(1, pl.filter(x => x.type !== 'off').length), kg = U() === 'kg';
  const stats = new Map();
  rs.forEach(s => stats.set(s, sessStats(s)));
  const wk = {};
  rs.forEach(s => {
    const k = weekDates(s.date)[0];
    (wk[k] = wk[k] || []).push(s)
  });
  const tw = weekDates(today())[0];
  let first = rs.length ? weekDates(rs[0].date)[0] : tw;
  const weeks = [];
  for (let d = toD(first); iso(d) <= tw; d.setDate(d.getDate() + 7)) weeks.push(iso(d));
  let streak = 0, best = 0, freeze = 0, since = 0, active = 0;
  weeks.forEach(w => {
    const n = new Set((wk[w] || []).map(s => s.date)).size;
    if (n >= 2) active++;
    if (n >= 1) {
      streak++;
      since++;
      best = Math.max(best, streak);
      if (since >= 4) {
        since = 0;
        freeze = Math.min(2, freeze + 1)
      }
    } else if (w !== tw) {if (freeze > 0) {freeze--} else {streak = 0; since = 0}}
  });
  const lifeSets = rs.reduce((a, s) => a + sSets(s), 0), lifeVol = rs.reduce((a, s) => a + sVol(s), 0), lifePR = rs.reduce((a, s) => a + stats.get(s).prs.length, 0);
  let maxW = 0;
  rs.forEach(s => Object.values(s.ex).forEach(a => (a || []).forEach(x => {if (full(x)) maxW = Math.max(maxW, +x.w)})));
  const exIds = new Set();
  rs.forEach(s => Object.entries(s.ex).forEach(([id, a]) => {if ((a || []).some(full)) exIds.add(id)}));
  const noteN = Object.values(st.sessions).reduce((a, s) => a + Object.values(s.exNotes || {}).filter(v => v && v.trim()).length, 0);
  const planM = new Set();
  pl.forEach(x => (x.groups || []).forEach(g => g.forEach(e => planM.add(e.m))));
  // weekly repeatables
  const qd = {};
  const rep = {fullweek: [], overload: [], prhunt: [], speed: [], dawnwk: [], quest: [], sweep: []};
  const wBest = w => {
    const m = {};
    (wk[w] || []).forEach(s => Object.entries(s.ex).forEach(([id, a]) => (a || []).forEach(x => {if (full(x)) m[id] = Math.max(m[id] || 0, e1(x))})));
    return m
  };
  let fullRun = 0, perfectMonth = 0, balanced = 0, century = 0;
  weeks.forEach((w, i) => {
    const ss = wk[w] || [], days = new Set(ss.map(s => s.date)).size;
    if (days >= planned) {
      rep.fullweek.push(w);
      fullRun++;
      if (fullRun >= 4) perfectMonth = 1
    } else if (w !== tw) fullRun = 0;
    if (i > 0) {
      const a = wBest(w), b = wBest(weeks[i - 1]);
      if (Object.keys(a).filter(id => b[id] && a[id] > b[id]).length >= 5) rep.overload.push(w)
    }
    if (ss.reduce((t, s) => t + stats.get(s).prs.length, 0) >= 3) rep.prhunt.push(w);
    const timed = ss.filter(s => s.start && s.end);
    if (timed.length >= 2 && timed.every(s => elapsed(s) < 36e5)) rep.speed.push(w);
    if (ss.filter(s => hrOf(s) !== null && hrOf(s) < 6).length >= 3) rep.dawnwk.push(w);
    {
      const q = weekQuests(w, weekDates(w));
      qd[w] = q.filter(x => x.done).length;
      if (q.every(x => x.done) && ss.length) rep.quest.push(w)
    }
    if (ss.reduce((t, s) => t + sSets(s), 0) >= 100) century = 1;
    const hit = new Set();
    ss.forEach(s => Object.keys(muscleSets(s)).forEach(k => hit.add(k)));
    if (planM.size && [...planM].every(k => hit.has(k))) balanced = 1
  });
  const fdays = new Set([...rs.map(s => s.date), ...Object.keys(st.flags)]);
  [...fdays].sort().forEach(d => {
    if (d > today()) return;
    const c = dayChallenges(d);
    if (c.length && c.every(x => x.done)) rep.sweep.push(d)
  });
  // hidden facts
  let comeback = 0;
  for (let i = 1; i < rs.length; i++) if ((toD(rs[i].date) - toD(rs[i - 1].date)) / 864e5 >= 14) comeback = 1;
  const weekend = weeks.some(w => {
    const d = weekDates(w);
    const ds = new Set((wk[w] || []).map(s => s.date));
    return ds.has(d[5]) && ds.has(d[6])
  });
  const perfect = rs.some(s => {
    let n = 0, ok = true;
    Object.entries(s.ex).forEach(([id, a]) => {
      const e = exById(id) || {};
      (a || []).filter(full).forEach(x => {
        n++;
        if (!e.hi || +x.r < e.hi) ok = false
      })
    });
    return ok && n >= 4
  });
  const speedrun = rs.some(s => {
    const p = pl[DAYS.indexOf(s.day)];
    return s.start && s.end && elapsed(s) < 18e5 && p && plannedSetsFor(p) > 0 && sSets(s) >= plannedSetsFor(p)
  });
  const B = [];
  const T = (k, name, icon, cat, cur, tgt, desc, hid) => B.push({k, name, icon, cat, cur: Math.min(cur, tgt), tgt, desc, hid: !!hid, earned: cur >= tgt});
  [[1, 'First Rep'], [10, 'Getting Going'], [25, 'Regular'], [50, 'Committed'], [100, 'Centurion'], [250, 'Iron Legend']].forEach(([n, nm]) => T('w' + n, nm, '🏋️', 'Workouts', rs.length, n, `Log ${n} workout${n>1?'s':''}`));
  [2, 4, 8, 12, 26, 52].forEach((n, i) => T('s' + n, ['Spark', 'On Fire', 'Blazing', 'Inferno', 'Unstoppable', 'Eternal Flame'][i], '🔥', 'Streaks', best, n, `${n} week streak`));
  const vt = kg ? [25e3, 5e4, 1e5, 25e4, 5e5, 1e6] : [5e4, 1e5, 25e4, 5e5, 1e6, 25e5];
  vt.forEach((n, i) => T('v' + i, ['Mover', 'Hauler', 'Heavy Duty', 'Freight Train', 'Millionaire', 'Mountain Mover'][i], '🏗️', 'Volume', lifeVol, n, `Move ${n>=1e6?(n/1e6)+'M':(n/1e3)+'k'} ${U()} total`));
  [100, 500, 1000, 2500, 5000].forEach((n, i) => T('t' + n, ['Set Starter', 'Grinder', 'Thousand Club', 'Workhorse', 'Machine'][i], '✅', 'Sets', lifeSets, n, `Log ${n.toLocaleString()} sets`));
  [1, 10, 25, 50, 100].forEach((n, i) => T('p' + n, ['First PR', 'PR Machine', 'Record Breaker', 'Limit Pusher', 'Legend'][i], '🏆', 'PRs', lifePR, n, `Hit ${n} PR${n>1?'s':''}`));
  (kg ? [60, 100, 140, 180] : [135, 225, 315, 405]).forEach((n, i) => T('c' + i, `${n} Club`, '🥇', 'Lift clubs', maxW, n, `Lift ${n} ${U()} on any exercise`));
  T('pm', 'Perfect Month', '🗓️', 'Consistency', perfectMonth, 1, '4 full weeks in a row');
  T('nt', 'Note Taker', '📝', 'Consistency', noteN, 10, 'Write 10 exercise notes');
  T('ex', 'Explorer', '🧭', 'Consistency', exIds.size, 20, 'Log 20 different exercises');
  T('bal', 'Balanced', '⚖️', 'Consistency', balanced, 1, 'Train every muscle in your plan in one week');
  const H = [['dawn', 'Dawn Patrol', '🌅', 'Start a workout before 6 AM', rs.some(s => hrOf(s) !== null && hrOf(s) < 6)], ['owl', 'Night Owl', '🌙', 'Start a workout after 10 PM', rs.some(s => hrOf(s) !== null && hrOf(s) >= 22)], ['back', 'Comeback', '🔁', 'Return after 2+ weeks off', comeback], ['wknd', 'Weekend Warrior', '🛡️', 'Train Saturday and Sunday', weekend], ['hol', 'No Days Off', '🦃', 'Train on a holiday', rs.some(s => isHoliday(s.date))], ['mara', 'Marathon', '⏳', 'A workout over 2 hours', rs.some(s => s.start && s.end && elapsed(s) >= 72e5)], ['spd', 'Speedrun', '⚡', 'Finish a full plan day in under 30 min', speedrun], ['cent', 'Century', '💯', '100 sets in one week', century], ['perf', 'Perfectionist', '🎯', 'Top of the rep range on every set (4+ sets)', perfect]];
  H.forEach(([k, n, i, d, v]) => T(k, n, i, 'Hidden', v ? 1 : 0, 1, d, true));
  try {funnyBadges(rs, weeks, wk).forEach(([k, n, i, d, v]) => T(k, n, i, 'Funny', v ? 1 : 0, 1, d, true))} catch (e) {}
  const R = [['fullweek', 'Full Week', '🔥', 'Train every planned day in a week'], ['overload', 'Overloader', '📈', 'Beat last week on 5+ lifts'], ['prhunt', 'PR Hunter', '🏹', '3+ PRs in a week'], ['speed', 'Speed Demon', '⚡', 'Every timed workout under an hour (2+)'], ['dawnwk', 'Early Bird', '🐓', '3 workouts before 6 AM in a week'], ['quest', 'Quest Master', '🗺️', 'Finish all 3 weekly quests'], ['sweep', 'Daily Sweep', '🧹', 'Finish all 3 daily challenges']].map(([k, name, icon, desc]) => ({k, name, icon, desc, count: rep[k].length, list: rep[k]}));
  B.push(...crewBadges(), ...crewExtraBadges());
  const tierOf = c => B.filter(b => b.cat === c && b.earned).length, hasK = k => B.some(b => b.k === k && b.earned);
  [['col_club', 'Club Collector', '🏛️', 'Earn all 4 lift clubs', ['c0', 'c1', 'c2', 'c3'].every(hasK)], ['col_clock', 'Around the Clock', '🕛', 'Earn Dawn Patrol and Night Owl', hasK('dawn') && hasK('owl')], ['col_secret', 'Secret Keeper', '🗝️', 'Find every hidden badge', B.filter(b => b.hid && b.cat === 'Hidden').every(b => b.earned)], ['col_week', 'Weekly Grinder', '📆', 'Earn every weekly badge once (Early Bird optional)', R.filter(r => r.k !== 'dawnwk').every(r => r.count > 0)], ['col_ms', 'Milestone Master', '👑', 'Reach Gold in every milestone', Object.keys(LADDER).every(c => tierOf(c) >= 3)]].forEach(([k, n, i, d, v]) => T(k, n, i, 'Collections', v ? 1 : 0, 1, d));
  const bcount = B.filter(b => b.earned).length + R.reduce((a, r) => a + r.count, 0);
  let elig = 0;
  RANKS.forEach((r, i) => {if (active >= r[1] && bcount >= r[2]) elig = i});
  const rank = st.set.held === undefined ? elig : Math.max(0, Math.min(7, st.set.held));
  const twd = weekDates(today()), tws = rs.filter(s => s.date >= twd[0] && s.date <= twd[6]), pwd = weekDates(iso(new Date(toD(twd[0]).getTime() - 7 * 864e5)));
  const bestIn = list => {
    const m = {};
    list.forEach(s => Object.entries(s.ex).forEach(([id, a]) => (a || []).forEach(x => {if (full(x)) m[id] = Math.max(m[id] || 0, e1(x))})));
    return m
  };
  const wbm = bestIn(tws), lbm = bestIn(rs.filter(s => s.date >= pwd[0] && s.date <= pwd[6])), beat = Object.keys(wbm).filter(id => lbm[id] && wbm[id] > lbm[id]).length, tdays = new Set(tws.map(s => s.date)).size, need = Math.max(2, planned - 1);
  const trial = elig > rank && rank < 7 ? {tier: rank + 1, days: tdays, need, beat, needBeat: 3, passed: tdays >= need && beat >= 3} : null;
  let div = 0, frac = 1;
  if (rank < 7) {
    const c = RANKS[rank], n = RANKS[rank + 1], f = (v, a, b) => b > a ? (v - a) / (b - a) : 1;
    frac = elig > rank ? 1 : Math.max(0, Math.min(1, Math.min(f(active, c[1], n[1]), f(bcount, c[2], n[2]))));
    div = frac < 1 / 3 ? 3 : frac < 2 / 3 ? 2 : 1
  }
  let prestige = 0;
  if (rank === 7) while (active >= 52 + 13 * (prestige + 1) && bcount >= 60 + 15 * (prestige + 1)) prestige++;
  const rname = RANKS[rank][0] + (rank < 7 ? ' ' + ROMAN[div] : prestige ? ' ' + '★'.repeat(Math.min(prestige, 5)) : '');
  const power = Math.round(rank * 120 + (rank < 7 ? (3 - div) * 40 : 0) + prestige * 100 + active * 6 + bcount * 3 + lifePR * 2 + streak * 4);
  const season = seasonOf(today()), [s0, s1] = seasonRange(season), sw0 = weekDates(s0)[0], inS = d => d >= s0 && d <= s1 && d <= today();
  let xp = 0;
  new Set([...rs.map(s => s.date), ...Object.keys(st.flags)].filter(inS)).forEach(d => {
    const n = dayChallenges(d).filter(x => x.done).length;
    xp += n * 10 + (n === 3 ? 20 : 0)
  });
  rs.forEach(s => {if (inS(s.date)) xp += 15 + stats.get(s).prs.length * 5});
  Object.entries(qd).forEach(([w, n]) => {if (w >= sw0 && w <= s1) xp += n * 30});
  R.forEach(r => r.list.forEach(x => {if (x >= sw0 && x <= s1) xp += 25}));
  xp += ((st.cs && st.cs.buddyDates) || []).filter(inS).length * 50;
  const lvl = Math.min(30, 1 + Math.floor(xp / 200));
  const keys = new Set([...B.filter(b => b.earned).map(b => b.k), ...R.flatMap(r => r.list.map(x => r.k + '@' + x))]);
  ENG = {
    B, R, bcount, active, rank, elig, trial, div, frac, prestige, rname, power, season, xp, lvl, streak, best, freeze, keys, lifeSets, lifeVol, lifePR, workouts: rs.length, since: rs.length ? rs[0].date : null,
    earnedOn: d => {
      const w = weekDates(d)[0];
      const out = [];
      R.forEach(r => {
        if (r.list.includes(d) || (r.list.includes(w) && r.k !== 'sweep' && d === ([...(wk[w] || [])].map(s => s.date).sort().pop()))) out.push({...r, count: 1})
      });
      return out
    }
  };
  return ENG
}
function emblem(i, cls) {
  const r = RANKS[i], id = 'em' + (pid++);
  const pts = Array.from({length: 12}, (_, k) => {
    const a = k * Math.PI / 6;
    return `<circle cx="${(50+44.5*Math.cos(a)).toFixed(1)}" cy="${(50+44.5*Math.sin(a)).toFixed(1)}" r="1.5" fill="${r[4]}"/>`
  }).join('');
  return `<svg class="emb ${cls||''}" viewBox="0 0 100 100" aria-hidden="true"><defs><radialGradient id="${id}a" cx=".35" cy=".3" r=".85"><stop offset="0" stop-color="${r[4]}"/><stop offset=".55" stop-color="${r[3]}"/><stop offset="1" stop-color="#14161b"/></radialGradient><linearGradient id="${id}b" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fff"/><stop offset=".45" stop-color="${r[4]}"/><stop offset="1" stop-color="${r[3]}"/></linearGradient></defs><circle cx="50" cy="50" r="48" fill="url(#${id}a)"/><circle cx="50" cy="50" r="42" fill="none" stroke="url(#${id}b)" stroke-width="3"/>${pts}<circle cx="50" cy="50" r="28" fill="rgba(0,0,0,.18)" stroke="rgba(0,0,0,.3)" stroke-width="1"/><path d="M50 27 L56.5 43 L73 43.5 L60 53.5 L64.5 70 L50 60.5 L35.5 70 L40 53.5 L27 43.5 L43.5 43 Z" fill="url(#${id}b)" stroke="rgba(0,0,0,.35)" stroke-width="1"/>${i>=5?`<circle cx="50" cy="50" r="48" fill="none" stroke="${r[4]}" stroke-width="2"/>`:''}<ellipse cx="34" cy="26" rx="17" ry="6" transform="rotate(-32 34 26)" fill="#fff" opacity=".28"/></svg>`
}
function badgeTile(b, on) {
  const earned = on || b.earned || b.count > 0, shine = earned && ENG && ENG.rank >= 2;
  const lbl = b.hid && !earned ? '???' : b.name;
  const sub = b.count !== undefined ? (b.count ? `×${b.count}` : 'Not yet') : earned ? 'Earned' : b.hid ? 'Hidden' : `${b.cur.toLocaleString()}/${b.tgt.toLocaleString()}`;
  return `<div class="bdg${earned?' on':''}${shine?' shine':''}${earned?' ev'+evTier(b):''}" title="${esc(b.desc)}"><div class="bi">${b.hid&&!earned?'❔':b.icon}</div><b>${esc(lbl)}</b><span>${sub}</span>${!earned&&b.tgt>1&&!b.hid?`<i><u style="width:${Math.round(b.cur/b.tgt*100)}%"></u></i>`:''}</div>`
}
function nudge(E) {
  const c = E.B.filter(b => !b.earned && !b.hid && b.tgt > 1 && b.cur / b.tgt >= .7).sort((a, b) => b.cur / b.tgt - a.cur / a.tgt)[0];
  return c ? `<div class="nudge">${c.icon} Almost there: <b>${esc(c.name)}</b> · ${(c.tgt-c.cur).toLocaleString()} to go</div>` : ''
}
function challengesHTML() {
  const E = engine(), c = dayChallenges(st.date), n = c.filter(x => x.done).length, r = RANKS[E.rank];
  return `<div class="card chal"><div class="ghd"><span class="glbl">${st.date===today()?"TODAY'S":DN[DAYS.indexOf(st.day)].toUpperCase()+"'S"} CHALLENGES · ${n}/3</span><button class="rankchip" id="openprof">${emblem(E.rank)}<span>${E.rname}${E.streak>1?` · 🔥${E.streak}`:''}</span></button></div>${c.map(x=>`<${x.man&&!x.done?'button':'div'} class="crow${x.done?' done':''}" ${x.man&&!x.done?`data-manual="${x.k}"`:''}><span class="cdot">${x.done?'✓':''}</span><span>${esc(x.t)}</span></${x.man&&!x.done?'button':'div'}>`).join('')}${n===3?'<div class="sweep">🧹 Daily Sweep earned</div>':''}${nudge(E)}</div>`
}
function questsHTML() {
  const wd = weekDates(today()), q = weekQuests(wd[0], wd);
  return `<div class="card"><div class="ghd"><h3>Weekly quests</h3><span class="sub">${q.filter(x=>x.done).length}/3 · resets Monday</span></div>${q.map(x=>`<div class="quest${x.done?' done':''}"><div class="qh"><span>${x.done?'✅':'🎯'} ${esc(x.t)}</span><b>${x.c.toLocaleString()}/${x.n.toLocaleString()}</b></div><i><u style="width:${Math.round(x.c/x.n*100)}%"></u></i></div>`).join('')}</div>`
}
/* ---------- 0.10: unlock popups, confetti, weekly report ---------- */
function confetti(n) {
  const box = document.createElement('div');
  box.className = 'confetti';
  const cols = ['#ef2b36', '#ffbe0b', '#2563ff', '#16b65b', '#d14dff', '#ff7a3d'];
  for (let i = 0; i < (n || 70); i++) {
    const s = document.createElement('i');
    s.style.left = Math.random() * 100 + '%';
    s.style.background = cols[i % cols.length];
    s.style.animationDelay = (Math.random() * .4) + 's';
    s.style.animationDuration = (1.6 + Math.random() * 1.2) + 's';
    s.style.transform = `rotate(${Math.random()*360}deg)`;
    box.appendChild(s)
  }
  document.body.appendChild(box);
  setTimeout(() => box.remove(), 3400)
}
function weekReport() {
  const tw = weekDates(today())[0];
  if (st.set.lastReport === tw || !st.plan) return false;
  const pw = weekDates(iso(new Date(toD(tw).getTime() - 7 * 864e5))), a = weekAgg(pw);
  st.set.lastReport = tw;
  saveSet();
  if (!a.sets) return false;
  const ppw = weekDates(iso(new Date(toD(pw[0]).getTime() - 7 * 864e5))), b = weekAgg(ppw), up = a.vol > b.vol, ratio = a.done / Math.max(1, a.planned);
  const g = ratio >= 1 && a.prs >= 1 && up ? 'A+' : ratio >= 1 ? 'A' : ratio >= .6 || a.prs >= 3 ? 'B' : 'C';
  const E = engine();
  const earned = E.R.filter(r => r.list.includes(pw[0])).map(r => r.icon + ' ' + r.name);
  st.sheet = {type: 'report', g, a, b, pw, earned};
  renderSheet();
  return true
}
function setFlag(k) {
  const d = today();
  st.flags[d] = st.flags[d] || {};
  if (st.flags[d][k]) return;
  st.flags[d][k] = 1;
  const keys = Object.keys(st.flags).sort();
  while (keys.length > 90) delete st.flags[keys.shift()];
  store('overload-flags', st.flags);
  checkUnlocks()
}
async function profileCard() {
  const E = engine(), r = RANKS[E.rank], W = 1080, H = 1920, cv = document.createElement('canvas');
  cv.width = W;
  cv.height = H;
  const c = cv.getContext('2d');
  try {
    await Promise.all([document.fonts.load('900 120px Archivo'), document.fonts.load('700 40px Inter')])
  } catch (x) {}
  const bg = c.createLinearGradient(0, 0, W, H);
  bg.addColorStop(0, '#0b0c12');
  bg.addColorStop(1, '#1c1030');
  c.fillStyle = bg;
  c.fillRect(0, 0, W, H);
  const gl = c.createRadialGradient(W / 2, 640, 0, W / 2, 640, 700);
  gl.addColorStop(0, r[4] + '88');
  gl.addColorStop(1, 'rgba(0,0,0,0)');
  c.fillStyle = gl;
  c.fillRect(0, 0, W, H);
  const svg = emblem(E.rank).replace('<svg ', '<svg xmlns="http://www.w3.org/2000/svg" width="640" height="640" ');
  const im = await new Promise(res => {
    const i = new Image();
    i.onload = () => res(i);
    i.onerror = () => res(null);
    i.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg)
  });
  if (im) c.drawImage(im, W / 2 - 280, 360, 560, 560);
  c.textAlign = 'center';
  c.fillStyle = '#fff';
  c.font = 'italic 900 64px Archivo, sans-serif';
  c.fillText('OVERLOAD', W / 2, 170);
  c.font = '900 150px Archivo, sans-serif';
  c.font = (E.rname.length > 9 ? '900 120px' : '900 150px') + ' Archivo, sans-serif';
  c.fillText(E.rname.toUpperCase(), W / 2, 1080);
  c.font = '700 42px Inter, sans-serif';
  c.globalAlpha = .85;
  c.fillText(`${st.set.name||'Lifter'} · Rank Power ${E.power}`, W / 2, 1160);
  c.globalAlpha = 1;
  const t = [[E.workouts, 'Workouts'], [E.bcount, 'Badges'], [E.streak, 'Week streak'], [E.lifePR, 'PRs']], tw = (W - 160 - 72) / 4;
  t.forEach(([v, l], i) => {
    const x = 80 + i * (tw + 24);
    c.fillStyle = 'rgba(255,255,255,.1)';
    c.beginPath();
    c.roundRect ? c.roundRect(x, 1260, tw, 170, 28) : c.rect(x, 1260, tw, 170);
    c.fill();
    c.fillStyle = '#fff';
    c.font = '900 66px Archivo, sans-serif';
    c.fillText(String(v), x + tw / 2, 1352);
    c.font = '700 28px Inter, sans-serif';
    c.globalAlpha = .85;
    c.fillText(l, x + tw / 2, 1400);
    c.globalAlpha = 1
  });
  c.font = '700 34px Inter, sans-serif';
  c.globalAlpha = .8;
  c.fillText('Tracked with Overload', W / 2, H - 110);
  c.globalAlpha = 1;
  const blob = await new Promise(rr => cv.toBlob(rr, 'image/png'));
  return {
    url: cv.toDataURL('image/png'), file: new File([blob], 'overload-rank.png', {type: 'image/png'})
  }
}
/* ---------- 0.12: seasons, cosmetics, tiers ---------- */
const seasonOf = d => {
  const t = toD(d);
  return (t.getFullYear() - 2026) * 4 + Math.floor(t.getMonth() / 3) - 2
};
const seasonRange = n => {
  const q = n + 2, y = 2026 + Math.floor(q / 4), m = ((q % 4) + 4) % 4 * 3;
  return [iso(new Date(y, m, 1)), iso(new Date(y, m + 3, 0))]
};
const TIERS = [['Bronze', '#c07a3e'], ['Silver', '#8fb4dc'], ['Gold', '#e8b923'], ['Platinum', '#4fd1c5'], ['Diamond', '#5aa9ff'], ['Legend', '#d14dff']];
const LADDER = {
  Workouts: ['Iron Regular', '🏋️'], Streaks: ['Streak Keeper', '🔥'], Volume: ['Mover', '🏗️'], Sets: ['Grinder', '✅'], PRs: ['PR Machine', '🏆']
};
const ROMAN = ['', 'I', 'II', 'III', 'IV', 'V', 'VI'];
const PASS = [[2, 'plate', 'ember', 'Ember plate color'], [4, 'aura', 'spark', 'Spark aura'], [6, 'frame', 'neon', 'Neon frame'], [8, 'plate', 'ice', 'Ice plate color'], [10, 'tag', 'Grinder', '"Grinder" nameplate'], [12, 'aura', 'frost', 'Frost aura'], [14, 'frame', 'circuit', 'Circuit frame'], [16, 'plate', 'toxic', 'Toxic plate color'], [18, 'aura', 'ember', 'Ember aura'], [20, 'frame', 'laurel', 'Laurel frame'], [22, 'plate', 'galaxy', 'Galaxy plate color'], [24, 'aura', 'prism', 'Prism aura'], [26, 'tag', 'Unbreakable', '"Unbreakable" nameplate'], [28, 'frame', 'crown', 'Crown frame'], [30, 'aura', 'gold', 'Gold aura + season badge']];
const FRAMES = {
  bronze: ['Bronze ring', 'Reach Bronze', e => e.rank >= 1], silver: ['Silver ring', 'Reach Silver', e => e.rank >= 2], gold: ['Gold ring', 'Reach Gold', e => e.rank >= 3], plat: ['Platinum ring', 'Reach Platinum', e => e.rank >= 4], diamond: ['Diamond ring', 'Reach Diamond', e => e.rank >= 5], titan: ['Titan ring', 'Reach Titan', e => e.rank >= 6], olymp: ['Olympian ring', 'Reach Olympian', e => e.rank >= 7], iron: ['Iron Club', 'Club Collector badge', e => hasB(e, 'col_club')], shadow: ['Shadow', 'Secret Keeper badge', e => hasB(e, 'col_secret')], neon: ['Neon', 'Season pass level 6'], circuit: ['Circuit', 'Season pass level 14'], laurel: ['Laurel', 'Season pass level 20'], crown: ['Crown', 'Season pass level 28']
};
const AURAS = {
  glow: ['Rank glow', 'Reach Diamond', e => e.rank >= 5], spark: ['Spark', 'Season pass level 4'], frost: ['Frost', 'Season pass level 12'], ember: ['Ember', 'Season pass level 18'], prism: ['Prism', 'Season pass level 24'], gold: ['Season gold', 'Season pass level 30']
};
const hasB = (e, k) => e.B.some(b => b.k === k && b.earned);
const cosOwn = (t, k) => ((st.set.cos || {})[t] || []).includes(k);
const frameOK = (k, e) => {
  const f = FRAMES[k];
  return !!f && (f[2] ? f[2](e) : cosOwn('frame', k))
};
const auraOK = (k, e) => {
  const a = AURAS[k];
  return !!a && (a[2] ? a[2](e) : cosOwn('aura', k))
};
const SEASON_PLATES = {ember: 2, ice: 8, toxic: 16, galaxy: 22};
function plateLocked(c) {
  {
    const F = window.__FIN;
    if (F && F[c]) return finOwned(c) ? '' : F[c][2]
  }
  if (SEASON_PLATES[c]) return cosOwn('plate', c) ? '' : `Season pass level ${SEASON_PLATES[c]}`;
  if (PLATE_LOCK[c] && engine().rank < PLATE_LOCK[c]) return `${RANKS[PLATE_LOCK[c]][0]} rank`;
  return ''
}
function crewBadges() {
  const cb = st.set.crewB || {}, cs = st.cs, kg = U() === 'kg', mem = cs ? cs.members : [], me = myId(), out = [];
  const T = (k, name, icon, cur, tgt, desc) => out.push({
    k, name, icon, cat: 'Crew', cur: cb[k] ? tgt : Math.min(cur, tgt), tgt, desc, hid: false, earned: !!cb[k] || cur >= tgt
  });
  const lifeV = mem.reduce((a, m) => a + (m.profile ? toMine(m.profile.lifeVol || 0, m.profile.unit) : 0), 0);
  T('squad', 'Full Squad', '🫂', 0, 1, 'Everyone in your crew trains on the same day');
  T('cmil', 'Crew Million', '🌋', Math.round(lifeV), kg ? 450000 : 1000000, `Move ${kg?'450k kg':'1M lb'} as a crew`);
  T('hype', 'Hype Man', '📣', st.set.reactN || 0, 50, 'React to 50 crew posts');
  T('rival', 'Rival', '⚔️', 0, 1, 'Beat your rival over a full week');
  T('recruit', 'Recruiter', '🤝', cs && cs.crew.owner === me ? mem.length - 1 : 0, 1, 'Start a crew and get someone to join');
  T('team', 'Team Player', '🏁', 0, 1, 'Finish a crew challenge');
  return out
}
function rarity(b) {
  const cs = st.cs;
  if (cs && cs.members.length > 1 && b.cat !== 'Crew') {
    const mem = cs.members.filter(m => m.profile && m.profile.keys);
    if (mem.length > 1) {
      const n = mem.filter(m => m.profile.keys.includes(b.k)).length, f = n / mem.length;
      return [f <= .25 ? 'Legendary' : f <= .5 ? 'Epic' : f <= .75 ? 'Rare' : 'Common', `${n} of ${mem.length} in your crew`]
    }
  }
  if (b.cat === 'Collections') return ['Legendary', ''];
  if (b.hid) return ['Epic', ''];
  if (b.cat === 'Lift clubs') {
    const i = +b.k.slice(1);
    return [['Common', 'Rare', 'Epic', 'Legendary'][i], '']
  }
  if (b.cat === 'Crew') return ['Rare', ''];
  const L = (engine().B.filter(x => x.cat === b.cat).findIndex(x => x.k === b.k));
  return [L <= 1 ? 'Common' : L <= 2 ? 'Rare' : L <= 4 ? 'Epic' : 'Legendary', '']
}
const RARC = {Common: '#8c939e', Rare: '#2f7de1', Epic: '#8b3dff', Legendary: '#e8a10b'};
/* ---------- 0.12: profile ---------- */
function ladderTile(cat) {
  const E = engine(), list = E.B.filter(b => b.cat === cat), n = list.filter(b => b.earned).length, [nm, ic] = LADDER[cat], nx = list[n], t = n ? TIERS[Math.min(n, 6) - 1] : null;
  return `<button class="bdg lad${n?' on':''}${n&&E.rank>=2?' shine':''}" data-lad="${cat}" style="--tc:${t?t[1]:'#8c939e'}"><div class="bi">${ic}</div><b>${nm} ${ROMAN[n]||''}</b><span>${t?t[0]:'Locked'}</span>${nx?`<i><u style="width:${Math.round(nx.cur/nx.tgt*100)}%"></u></i>`:'<span>Maxed</span>'}</button>`
}
function badgeBtn(b) {
  const earned = b.earned || b.count > 0;
  const t = badgeTile(b);
  return t.replace('<div class="bdg', '<button data-bdg="' + b.k + '" class="bdg').replace(/<\/div>$/, '</button>')
}
/* ---------- 0.12: rank-up cinematic ---------- */
function cinematic(rank, label, unlock, sub) {
  document.querySelectorAll('.cine').forEach(x => x.remove());
  const d = document.createElement('div');
  d.className = 'cine';
  d.style.setProperty('--rc', RANKS[rank][4]);
  const sp = Array.from({length: 26}, (_, i) => {
    const a = Math.random() * Math.PI * 2, r = 140 + Math.random() * 180;
    return `<i style="--x:${Math.round(Math.cos(a)*r)}px;--y:${Math.round(Math.sin(a)*r)}px;animation-delay:${(.55+Math.random()*.25).toFixed(2)}s"></i>`
  }).join('');
  d.innerHTML = `<div class="cflash"></div><div class="cstage"><div class="csparks">${sp}</div><div class="cemb">${emblem(rank)}</div></div><div class="csub">${sub||'You ranked up to'}</div><div class="cname">${esc(label.toUpperCase())}</div>${unlock?`<div class="cun">Unlocked: <b>${esc(unlock)}</b></div>`:''}<div class="cbtns"><button class="btn acc" data-c="share">📸 Share</button><button class="btn" data-c="x">Let's go</button></div>`;
  document.body.appendChild(d);
  setTimeout(() => {try {navigator.vibrate && navigator.vibrate([20, 40, 160, 60, 40])} catch (e) {}}, 600);
  d.addEventListener('click', async e => {
    const b = e.target.closest('button');
    if (!b) return;
    d.remove();
    if (b.dataset.c === 'share') {
      st.sheet = {type: 'share', title: 'Share your rank'};
      renderSheet();
      const r = await profileCard();
      if (st.sheet && st.sheet.type === 'share') {
        st.sheet.url = r.url;
        st.sheet.file = r.file;
        renderSheet()
      }
    }
  })
}
/* ---------- 0.12: year in review ---------- */
function yearData(y) {
  const rs = realSessions().filter(s => s.date.slice(0, 4) === String(y));
  if (!rs.length) return null;
  const vol = rs.reduce((a, s) => a + sVol(s), 0), sets = rs.reduce((a, s) => a + sSets(s), 0), prs = rs.reduce((a, s) => a + sessStats(s).prs.length, 0);
  const m = {};
  rs.forEach(s => Object.entries(muscleSets(s)).forEach(([k, n]) => m[k] = (m[k] || 0) + n));
  const mk = Object.keys(m).sort((a, b) => m[b] - m[a]);
  const L = {};
  mk.forEach((k, i) => (MKEY[k] || []).forEach(x => {L[x] = Math.max(L[x] || 0, i < 3 ? 2 : 1)}));
  let best = null;
  rs.forEach(s => Object.entries(s.ex).forEach(([id, a]) => (a || []).forEach(x => {if (full(x) && (!best || e1(x) > best.e)) best = {n: exName(id), w: x.w, r: x.r, e: e1(x)}})));
  const dow = [0, 0, 0, 0, 0, 0, 0];
  rs.forEach(s => dow[dayIdx(s.date)]++);
  const fav = dow.indexOf(Math.max(...dow));
  const am = rs.filter(s => hrOf(s) !== null && hrOf(s) < 12).length, pm = rs.filter(s => hrOf(s) !== null && hrOf(s) >= 12).length;
  const mins = rs.filter(s => s.start && s.end).reduce((a, s) => a + elapsed(s) / 6e4, 0);
  return {
    y, n: rs.length, days: new Set(rs.map(s => s.date)).size, vol, sets, prs, mk, m, L, best, fav, am, pm, hours: Math.round(mins / 60)
  }
}
function openYear() {
  const y = new Date().getFullYear(), D = yearData(y), E = engine();
  if (!D) {
    toast('Log a workout this year to unlock your review');
    return
  }
  const pos = crewOn() && st.cs ? arenaOrder('all').findIndex(m => m.id === myId()) + 1 : 0, vc = volCompare(D.vol);
  const S = [['#ef2b36', '#ff8a3d', `<div class="ybig">${y}</div><h2>Your year in Overload</h2><p>${esc(st.set.name||'Lifter')}, let's look back.</p>`],
  ['#2563ff', '#00b7ff', `<p>You showed up</p><div class="ybig">${D.n}</div><h2>times</h2><p>${D.hours?`That's about ${D.hours} hours under the bar.`:''}</p>`],
  ['#ff7a3d', '#ffbe0b', `<p>Total weight moved</p><div class="ybig">${D.vol>=1e6?(D.vol/1e6).toFixed(2)+'M':kfmt(D.vol)}</div><h2>${U()}</h2><p>${vc[0]} ${vc[1].replace(/<[^>]+>/g,'').replace(/^You moved [\d,.]+ \w+, /,'That\u2019s ')}</p>`],
  ['#16b65b', '#00c2a8', `<p>Sets logged</p><div class="ybig">${D.sets.toLocaleString()}</div><h2>${D.prs} personal records</h2>`],
  D.best ? ['#7c4dff', '#d14dff', `<p>Your strongest lift</p><h2>${esc(D.best.n)}</h2><div class="ybig">${D.best.w}×${D.best.r}</div><p>About a ${Math.round(D.best.e)} ${U()} one-rep max</p>`] : null,
  ['#ef2b36', '#7c4dff', `<p>Your most-trained muscle</p><h2>${esc(D.mk[0]||'All of them')}</h2><div class="ymap"><svg viewBox="30 0 140 380">${anatomy('front',D.L,true)}</svg><svg viewBox="30 0 140 380">${anatomy('back',D.L,true)}</svg></div><p>${D.mk.slice(1,3).map(esc).join(' and ')}${D.mk.length>1?' close behind':''}</p>`],
  ['#00b7ff', '#2563ff', `<p>Your favorite day</p><div class="ybig" style="font-size:72px">${DN[D.fav]}</div><p>${D.am>D.pm?'Morning lifter ☀️':D.pm>D.am?'After-work grinder 🌙':''}</p>`],
  ['#ff7a3d', '#ef2b36', `<p>Longest streak</p><div class="ybig">${E.best}</div><h2>weeks in a row 🔥</h2>`],
  ['#b88600', '#ffd75e', `<p>You finish the year as</p><div class="yemb">${emblem(E.rank)}</div><h2>${esc(E.rname)}</h2><p>${E.bcount} badges earned${pos?` · #${pos} in your crew`:''}</p>`],
  ['#0b0c12', '#3a1d6e', `<div class="ybig" style="font-size:64px">${y+1}</div><h2>See you under the bar.</h2><p>Same plan. Heavier plates.</p>`]].filter(Boolean);
  let i = 0;
  const d = document.createElement('div');
  d.className = 'yir-ov';
  document.body.appendChild(d);
  const draw = () => {
    const s = S[i];
    d.style.background = `linear-gradient(160deg,${s[0]},${s[1]})`;
    d.innerHTML = `<div class="ybars">${S.map((_,k)=>`<i class="${k<i?'d':k===i?'c':''}"></i>`).join('')}</div><button class="yx" aria-label="Close">✕</button><div class="yslide">${s[2]}</div><div class="yhint">${i<S.length-1?'Tap to continue':'Tap to close'}</div>`
  };
  draw();
  d.addEventListener('click', e => {
    if (e.target.closest('.yx')) {
      d.remove();
      return
    }
    const left = e.clientX < innerWidth * .3;
    if (left) {
      i = Math.max(0, i - 1);
      draw();
      return
    }
    if (i >= S.length - 1) {
      d.remove();
      st.set.yirSeen = y;
      saveSet();
      return
    }
    i++;
    draw()
  })
}
/* ---------- 0.12: unlock engine ---------- */
function checkUnlocks() {
  if (!st.plan) return;
  dirtyEng();
  let E = engine();
  if (!Array.isArray(st.set.seen) || !st.set.v12) {
    st.set.seen = [...E.keys];
    if (st.set.held === undefined) st.set.held = E.elig;
    st.set.rankSeen = Math.max(st.set.rankSeen || 0, st.set.held);
    st.set.v12 = 1;
    dirtyEng();
    E = engine();
    st.set.divSeen = E.rank * 10 + (4 - (E.div || 1));
    st.set.presSeen = E.prestige;
    st.set.curSeason = E.season;
    grantPass(E, true);
    checkFeatures(true);
    st.set.v13 = 1;
    saveSet();
    return
  }
  if (!st.set.v13) {
    checkFeatures(true);
    st.set.seen = [...E.keys];
    st.set.v13 = 1;
    saveSet();
    return
  }
  checkFeatures(false);
  goalCheck();
  if (st.set.held === undefined) st.set.held = E.elig;
  if (E.trial && E.trial.passed) {
    st.set.held = E.rank + 1;
    dirtyEng();
    E = engine()
  }
  if (st.set.curSeason !== E.season) {
    if (st.set.curSeason) {
      st.set.seasons = st.set.seasons || {};
      st.set.seasons[st.set.curSeason] = st.set.lastLvl || 1
    }
    st.set.curSeason = E.season
  }
  st.set.lastLvl = E.lvl;
  const newCos = grantPass(E, false);
  const seen = new Set(st.set.seen), fresh = [...E.keys].filter(k => !seen.has(k)), up = E.rank > (st.set.rankSeen || 0), pres = (E.prestige || 0) > (st.set.presSeen || 0), dkey = E.rank * 10 + (4 - (E.div || 1)), divUp = !up && E.rank < 7 && dkey > (st.set.divSeen || 0);
  st.set.seen = [...E.keys];
  st.set.rankSeen = Math.max(E.rank, st.set.rankSeen || 0);
  st.set.presSeen = Math.max(E.prestige, st.set.presSeen || 0);
  st.set.divSeen = Math.max(dkey, st.set.divSeen || 0);
  saveSet();
  newCos.forEach((c, i) => setTimeout(() => toast(`🎁 Season reward: ${c}`), 400 + i * 2000));
  if (up || pres) {
    addBell('🏅', `You reached ${E.rname}`);
    haptic('heavy');
    const u = up ? UNLOCKS.find(x => x[0] === E.rank) : null;
    setTimeout(() => {
      cinematic(E.rank, E.rname, u ? u[1] : '', pres && !up ? 'Prestige unlocked' : 'You ranked up to');
      confetti(120)
    }, 250);
    if (crewOn()) crewCall('post', {post: {type: 'rank', data: {rank: E.rank, rname: E.rname}}});
    syncCrew(true);
    return
  }
  if (divUp) {
    setTimeout(() => toast(`📈 Division up: ${E.rname}`), 300);
    addBell('📈', `Division up: ${E.rname}`)
  }
  if (!fresh.length) {
    if (divUp) syncCrew(true);
    return
  }
  const items = fresh.map(k => {
    const base = k.split('@')[0];
    return E.B.find(b => b.k === base) || E.R.find(r => r.k === base)
  }).filter(Boolean);
  const s = cur(), mid = s.start && !s.end;
  syncCrew(true);
  if (mid) {
    items.forEach((b, i) => setTimeout(() => {
      toast(`${b.icon} Badge unlocked: ${b.name}`);
      confetti(40)
    }, i * 2100));
    return
  }
  if (!items.length) return;
  st.sheet = {type: 'badges', items};
  renderSheet();
  confetti(70)
}
function grantPass(E, silent) {
  const cos = st.set.cos = st.set.cos || {plate: [], aura: [], frame: [], tag: []}, got = [];
  ['plate', 'aura', 'frame', 'tag'].forEach(t => cos[t] = cos[t] || []);
  PASS.forEach(([lv, t, k, label]) => {if (E.lvl >= lv && !cos[t].includes(k)) {cos[t].push(k); got.push(label)}});
  return silent ? [] : got
}
/* ---------- 0.13: progressive unlocks ---------- */
const FEATS = [['mastery', 1, 'Muscle mastery', 'Your body map now levels up as you train'], ['goals', 2, 'Goals', 'Set a lift target with a date in Stats'], ['season', 3, 'Season pass', 'Earn levels and rewards all season'], ['quests', 3, 'Quest chains', 'Step-by-step roads to your next big lift'], ['liftranks', 5, 'Lift ranks', 'See how strong each lift is for your bodyweight']];
const feat = k => (st.set.unl || []).includes(k);
function checkFeatures(silent) {
  const n = realSessions().length, u = st.set.unl = st.set.unl || [];
  let fresh = [];
  FEATS.forEach(([k, need, name, desc]) => {if (n >= need && !u.includes(k)) {u.push(k); fresh.push([name, desc])}});
  if (fresh.length) {
    saveSet();
    if (!silent) fresh.forEach(([n, d], i) => setTimeout(() => {
      toast(`🔓 New: ${n}`);
      addBell('🔓', `${n} unlocked. ${d}`)
    }, 700 + i * 2200))
  }
}
/* ---------- 0.13: quest chains ---------- */
function chainSteps(M) {
  const kg = U() === 'kg', big = kg ? 10 : 20, small = kg ? 5 : 10;
  return [[M - big, 5], [M - small, 3], [M, 1]]
}
const MILES = () => U() === 'kg' ? [40, 60, 80, 100, 120, 140, 160, 180, 200, 220, 240, 260] : [95, 135, 185, 225, 275, 315, 365, 405, 455, 495, 545, 585, 635];
function chains() {
  const rs = realSessions(), cnt = {}, bestW = {};
  rs.forEach(s => Object.entries(s.ex).forEach(([id, a]) => {
    const v = (a || []).filter(full);
    if (!v.length) return;
    cnt[id] = (cnt[id] || 0) + 1;
    v.forEach(x => {bestW[id] = Math.max(bestW[id] || 0, +x.w)})
  }));
  const C = st.set.chains = st.set.chains || {};
  let ch = false;
  Object.keys(cnt).filter(id => {
    const n = exName(id);
    return LIFTS.some(l => l[0] !== 'curl' && l[2].test(n)) && !/leg curl|raise|fly|extension/i.test(n)
  }).sort((a, b) => cnt[b] - cnt[a]).slice(0, 3).forEach(id => {
    if (C[id]) return;
    const M = MILES().find(m => m > bestW[id] && m - bestW[id] <= (U() === 'kg' ? 40 : 90));
    if (M) {
      C[id] = {M, at: today()};
      ch = true
    }
  });
  const out = [];
  Object.entries(C).forEach(([id, c]) => {
    if (!cnt[id]) return;
    const steps = chainSteps(c.M).map(([w, r]) => ({w, r, done: rs.some(s => (s.ex[id] || []).some(x => full(x) && +x.w >= w && +x.r >= r))}));
    out.push({id, name: exName(id), M: c.M, steps, done: steps.every(s => s.done)})
  });
  out.filter(c => c.done).forEach(c => {
    const nm = `Road to ${c.M} · ${c.name}`, D = st.set.chainsDone = st.set.chainsDone || [];
    if (!D.includes(nm)) {
      D.push(nm);
      ch = true;
      setTimeout(() => {
        toast(`🗺️ Quest complete: ${nm}`);
        confetti(80);
        haptic('success');
        addBell('🗺️', `Quest complete: ${esc(nm)}`)
      }, 500)
    }
    delete C[c.id];
    ch = true
  });
  if (ch) saveSet();
  return out.filter(c => !c.done)
}
function chainsHTML() {
  if (!feat('quests')) return '';
  const L = chains();
  if (!L.length) return '';
  return `<div class="card"><div class="ghd"><h3>Quest chains</h3><span class="sub">${(st.set.chainsDone||[]).length} done</span></div>${L.map(c=>`<div class="chain"><b>🗺️ Road to ${c.M} · ${esc(c.name)}</b><div class="csteps">${c.steps.map((s,i)=>`<span class="cstep${s.done?' done':''}"><i>${s.done?'✓':i+1}</i>${s.w}×${s.r}</span>`).join('<span class="carrow">›</span>')}</div></div>`).join('')}</div>`
}
/* ---------- the locker: everything you can unlock ---------- */
const box = 1;
const LOCK = {
  accent: {
    name: 'App theme', items: {
      red: ['Overload Red', '#ef2b36', '', E => true], blue: ['Ocean', '#2563ff', 'Bronze rank', E => E.rank >= 1], green: ['Forest', '#16b65b', 'Silver rank', E => E.rank >= 2], purple: ['Royal', '#7c4dff', 'Gold rank', E => E.rank >= 3], cyan: ['Ice', '#06b6d4', 'Crew level 3', () => crewLv() >= 3], gold: ['Gold', '#c99a2e', 'Platinum rank', E => E.rank >= 4], mono: ['Mono', '#14161c', 'Season pass level 10', E => E.lvl >= 10], pink: ['Hot Pink', '#ff3d8b', 'Mystery box', null, box]
    }
  },
  bg: {
    name: 'Background', items: {
      classic: ['Classic', '', '', E => true], minimal: ['Minimal', '', '', E => true], grid: ['Blueprint', '', 'Silver rank', E => E.rank >= 2], stars: ['Starfield', '', 'Diamond rank', E => E.rank >= 5]
    }
  },
  welcome: {
    name: 'Welcome screen', items: {
      classic: ['Classic', '', '', E => true], lightning: ['Lightning', '', 'Gold rank', E => E.rank >= 3], forge: ['Forge sparks', '', 'Season pass level 12', E => E.lvl >= 12], galaxy: ['Galaxy', '', 'Mystery box', null, box]
    }
  },
  icon: {
    name: 'App icon', items: {
      red: ['Red plate', 'red', '', E => true], black: ['Black plate', 'black', '', E => true], gold: ['Gold plate', 'gold', 'Gold rank', E => E.rank >= 3], ...Object.fromEntries(Object.entries(FIN).map(([k, f]) => [k, [f[0], k, f[2], E => finOwned(k, E)]]))
    }
  },
  setfx: {
    name: 'Set complete effect', items: {
      pulse: ['Pulse', '', '', E => true], sparks: ['Sparks', '', 'Bronze rank', E => E.rank >= 1], shock: ['Shockwave', '', 'Silver rank', E => E.rank >= 2], pop: ['Confetti pop', '', 'Season pass level 7', E => E.lvl >= 7], lightning: ['Lightning', '', 'Mystery box', null, box]
    }
  },
  prfx: {
    name: 'PR celebration', items: {
      confetti: ['Confetti', '', '', E => true], fireworks: ['Fireworks', '', 'Gold rank', E => E.rank >= 3], goldrain: ['Gold rain', '', 'Platinum rank', E => E.rank >= 4], storm: ['Lightning storm', '', 'Mystery box', null, box]
    }
  },
  rest: {
    name: 'Rest timer', items: {
      classic: ['Classic ring', '', '', E => true], bar: ['Minimal bar', '', '', E => true], plate: ['Spinning plate', '', 'Bronze rank', E => E.rank >= 1], neon: ['Neon', '', 'Mystery box', null, box]
    }
  },
  sound: {
    name: 'Sound pack', items: {
      classic: ['Classic beep', '', '', E => true], soft: ['Soft chime', '', '', E => true], gong: ['Gong', '', 'Silver rank', E => E.rank >= 2], airhorn: ['Air horn', '', 'Gold rank', E => E.rank >= 3], arcade: ['Arcade', '', 'Mystery box', null, box]
    }
  },
  nameFx: {
    name: 'Name effect', items: {
      none: ['Plain', '', '', E => true], gold: ['Gold shine', '', 'Gold rank', E => E.rank >= 3], neon: ['Neon', '', 'Crew level 4', () => crewLv() >= 4], fire: ['Fire', '', 'Season pass level 17', E => E.lvl >= 17], rainbow: ['Rainbow', '', 'Mystery box', null, box]
    }
  },
  card: {
    name: 'Post card style', items: {
      plain: ['Plain', '', '', E => true], carbon: ['Carbon', '', 'Silver rank', E => E.rank >= 2], gold: ['Gold', '', 'Gold rank', E => E.rank >= 3], ice: ['Ice', '', 'Season pass level 21', E => E.lvl >= 21], neon: ['Neon', '', 'Mystery box', null, box]
    }
  },
  pbanner: {
    name: 'Profile banner', items: {
      none: ['None', '', '', E => true], forge: ['Forge', '', '', E => true], city: ['Night City', '', 'Bronze rank', E => E.rank >= 1], ocean2: ['Ocean Storm', '', 'Silver rank', E => E.rank >= 2], gym: ['Iron Gym', '', 'Gold rank', E => E.rank >= 3], aurora: ['Northern Lights', '', 'Platinum rank', E => E.rank >= 4], volcano: ['Volcano', '', 'Diamond rank', E => E.rank >= 5], space: ['Deep Space', '', 'Titan rank', E => E.rank >= 6], olympus: ['Olympus', '', 'Olympian rank', E => E.rank >= 7], colosseum: ['Colosseum', '', 'Season pass level 20', E => E.lvl >= 20], thunder: ['Thunder', '', '25 PRs', E => E.lifePR >= 25]
    }
  },
  share: {
    name: 'Share card theme', items: {
      plate: ['Plate color', '', '', E => true], midnight: ['Midnight', '', '', E => true], gold: ['Gold', '', 'Gold rank', E => E.rank >= 3], neon: ['Neon', '', 'Mystery box', null, box]
    }
  },
  react: {
    name: 'Reactions', items: {
      fire: ['🔥', '', '', E => true], muscle: ['💪', '', '', E => true], trophy: ['🏆', '', '', E => true], clap: ['👏', '', 'Bronze rank', E => E.rank >= 1], heart: ['❤️', '', 'Silver rank', E => E.rank >= 2], crown: ['👑', '', 'Gold rank', E => E.rank >= 3], rocket: ['🚀', '', 'Season pass level 9', E => E.lvl >= 9], skull: ['💀', '', 'Earn a funny badge', E => E.B.some(b => b.cat === 'Funny' && b.earned)], goat: ['🐐', '', 'Mystery box', null, box], lol: ['😂', '', 'Mystery box', null, box]
    }
  }
};
const LOOK_DEF = {
  accent: 'red', bg: 'classic', welcome: 'classic', icon: 'red', setfx: 'pulse', prfx: 'confetti', rest: 'classic', sound: 'classic', nameFx: 'none', card: 'plain', pbanner: 'none', share: 'plate'
};
function owns(t, k, E) {
  const it = LOCK[t] && LOCK[t].items[k];
  if (!it) return false;
  if (cos14(t).includes(k)) return true;
  return it[3] ? !!it[3](E || eng14()) : false
}
function look(t) {
  const k = (st.set.look || {})[t] || LOOK_DEF[t];
  return LOCK[t] && LOCK[t].items[k] && owns(t, k) ? k : LOOK_DEF[t]
}
const reactKeys = () => Object.keys(LOCK.react.items).filter(k => owns('react', k));
/* mystery boxes: earned only, never bought */
const BOX_LVLS = [3, 7, 11, 19, 23, 27];
function boxSources(E) {
  const out = [];
  BOX_LVLS.forEach(l => {if (E.lvl >= l) out.push('S' + E.season + '-' + l)});
  for (let r = 1; r <= E.rank; r++) out.push('R' + r);
  Object.entries(st.set.seasons || {}).forEach(([s, l]) => BOX_LVLS.forEach(x => {if (l >= x) out.push('S' + s + '-' + x)}));
  return [...new Set(out)]
}
const boxesLeft = E => boxSources(E || eng14()).filter(k => !(st.set.boxes || {})[k]);
function openBox() {
  const E = engine(), src = boxesLeft(E)[0];
  if (!src) return null;
  const pool = [];
  Object.entries(LOCK).forEach(([t, v]) => Object.entries(v.items).forEach(([k, it]) => {if (it[4] && !cos14(t).includes(k)) pool.push([t, k])}));
  st.set.boxes = st.set.boxes || {};
  let got;
  if (pool.length) {
    got = pool[hashS(src + (st.set.crewMe ? st.set.crewMe.id : 'x')) % pool.length];
    st.set.cos14 = st.set.cos14 || {};
    (st.set.cos14[got[0]] = st.set.cos14[got[0]] || []).push(got[1]);
    st.set.boxes[src] = got.join(':')
  } else {
    st.set.boxes[src] = 'dupe';
    got = null
  }
  saveSet();
  return got || 'dupe'
}
/* season pass finishes are granted and kept forever */
function grant14() {
  const E = engine();
  let any = [];
  st.set.cos14 = st.set.cos14 || {};
  Object.entries(FIN).forEach(([k, f]) => {
    if (f[4] && f[4].season && E.lvl >= f[4].season && !cos14('plate').includes(k)) {
      (st.set.cos14.plate = st.set.cos14.plate || []).push(k);
      any.push(f[0] + ' plate finish')
    }
  });
  if (any.length) {
    saveSet();
    any.forEach((c, i) => setTimeout(() => toast('🎁 Season reward: ' + c), 600 + i * 1800))
  }
}
/* ---------- funny badges ---------- */
function funnyBadges(rs, weeks, wk) {
  const kg = U() === 'kg', legM = /Quads|Hamstrings|Glutes|Calves/;
  const out = [];
  const legSets = s => Object.entries(s.ex || {}).reduce((a, [id, x]) => a + (legM.test(exMuscle(id)) ? (x || []).filter(full).length : 0), 0);
  let run = 0, dodger = 0;
  weeks.forEach((w, i) => {
    if (i === weeks.length - 1) return;
    const ss = wk[w] || [], days = new Set(ss.map(s => s.date)).size;
    if (days >= 2 && ss.every(s => legSets(s) === 0)) {
      run++;
      if (run >= 3) dodger = 1
    } else run = 0
  });
  out.push(['f_legs', 'Leg Day Dodger', '🦵', 'Skip legs 3 weeks in a row (we see you)', dodger]);
  let ego = 0;
  const lastW = {};
  rs.forEach(s => Object.entries(s.ex || {}).forEach(([id, a]) => {
    const m = Math.max(0, ...(a || []).filter(full).map(x => +x.w));
    if (!m) return;
    const p = lastW[id];
    if (p && m - p.w >= (kg ? 9 : 20) && (toD(s.date) - toD(p.d)) / DAYMS <= 10) ego = 1;
    lastW[id] = {w: m, d: s.date}
  }));
  out.push(['f_ego', 'Ego Lifter', '🦚', 'Jump 20+ lb on a lift in a week', ego]);
  out.push(['f_ghost', 'Ghost', '👻', 'Open the app 5 times in a day without logging a set', (st.set.fb || {}).ghost ? 1 : 0]);
  out.push(['f_night', 'Night Shift', '🌃', 'Start a workout after midnight', rs.some(s => {
    const h = hrOf(s);
    return h !== null && h < 4
  }) ? 1 : 0]);
  let cardio = 0;
  weeks.forEach((w, i) => {
    if (!i) return;
    const a = new Set((wk[weeks[i - 1]] || []).map(s => s.date)).size, b = new Set((wk[w] || []).map(s => s.date)).size;
    if (a === 0 && b >= 5) cardio = 1
  });
  out.push(['f_cardio', 'Cardio Is Hard', '🫠', 'Zero workouts one week, then 5+ the next', cardio]);
  let rest = 0;
  for (let i = 1; i < rs.length; i++) if ((toD(rs[i].date) - toD(rs[i - 1].date)) / DAYMS >= 8) rest = 1;
  out.push(['f_rest', 'Rest Day Champion', '🛋', 'Take 7 rest days in a row', rest]);
  out.push(['f_rebel', 'Rest Timer Rebel', '⏭', 'Skip the rest timer 10 times in one workout', rs.some(s => (s.skips || 0) >= 10) ? 1 : 0]);
  return out
}
/* ---------- trophy room ---------- */
function trophies() {
  const E = engine(), out = [];
  const T = (k, type, name, how, earned, color) => out.push({k, type, name, how, earned: !!earned, color});
  for (let r = 1; r < 8; r++) T('rank' + r, 'statue', RANKS[r][0] + ' statue', 'Reach ' + RANKS[r][0], E.rank >= r, RANKS[r][4]);
  E.B.filter(b => b.cat === 'Collections').forEach(b => T('cup_' + b.k, 'cup', b.name + ' cup', b.desc, b.earned, '#e3c25a'));
  const seasons = {...(st.set.seasons || {}), [E.season]: E.lvl};
  Object.entries(seasons).sort().forEach(([s, l]) => {
    const tier = l >= 30 ? 'Gold' : l >= 20 ? 'Silver' : l >= 10 ? 'Bronze' : '';
    if (tier || +s === E.season) T('season' + s, 'medal', `Season ${s} ${tier||''} medal`.replace('  ', ' '), 'Reach season pass level 10, 20 or 30', !!tier, tier === 'Gold' ? '#e3c25a' : tier === 'Silver' ? '#cfd4dc' : '#c98a4a')
  });
  try {
    const me = myId();
    if (me && st.cs) {
      Object.keys(st.cs.votes || {}).forEach(mo => {
        const W = awardWinners(mo);
        AWARDS.forEach(a => {
          if (W[a[0]] === me) T('award_' + mo + a[0], 'plaque', `${a[1]} · ${monthName(mo)}`, 'Win a monthly crew award', 1, '#9b7bff')
        })
      })
    }
    if (!out.some(x => x.type === 'plaque')) T('award_any', 'plaque', 'Crew award plaque', 'Win a monthly crew award', 0, '#9b7bff')
  } catch (e) {}
  const lr = (() => {try {return liftRanks()} catch (e) {return []}})(), legend = lr.filter(r => r.t >= LIFTS[0][3].length);
  T('barbell', 'barbell', 'Golden barbell', 'Reach Legend on a lift rank', legend.length, '#e3c25a');
  return out
}
const trophyKeys = () => trophies().filter(t => t.earned).map(t => t.k);
function trophySVG(t, big) {
  const c = t.earned ? t.color : '#6b7080', id = 'tr' + (pid++), s = big ? 120 : 46;
  const body = {
    statue: `<path d="M0-30a7 7 0 1 1 0 14a7 7 0 1 1 0-14zM-10-14h20l-4 24h-12z"/><rect x="-14" y="10" width="28" height="7" rx="2"/><rect x="-18" y="17" width="36" height="8" rx="2"/>`, cup: `<path d="M-16-26h32v8c0 14-8 22-16 22s-16-8-16-22z"/><path d="M-16-22h-6v4c0 6 4 10 9 10M16-22h6v4c0 6-4 10-9 10" fill="none" stroke="url(#${id})" stroke-width="3"/><rect x="-3" y="4" width="6" height="10"/><rect x="-12" y="14" width="24" height="10" rx="2"/>`, medal: `<path d="M-10-30l10 16 10-16h8l-12 22h-12l-12-22z" opacity=".75"/><circle cy="8" r="16"/><circle cy="8" r="10" fill="none" stroke="rgba(255,255,255,.5)" stroke-width="1.5"/>`, plaque: `<rect x="-20" y="-26" width="40" height="50" rx="4"/><rect x="-14" y="-18" width="28" height="20" rx="2" fill="rgba(255,255,255,.35)"/><rect x="-12" y="8" width="24" height="3" rx="1" fill="rgba(255,255,255,.5)"/><rect x="-9" y="14" width="18" height="3" rx="1" fill="rgba(255,255,255,.4)"/>`, barbell: `<rect x="-28" y="-3" width="56" height="5" rx="2"/><rect x="-24" y="-15" width="7" height="30" rx="2"/><rect x="17" y="-15" width="7" height="30" rx="2"/><rect x="-30" y="-10" width="5" height="20" rx="2"/><rect x="25" y="-10" width="5" height="20" rx="2"/><rect x="-14" y="18" width="28" height="8" rx="2"/>`
  }[t.type] || '';
  return `<svg viewBox="-34 -34 68 68" width="${s}" height="${s}" aria-hidden="true"><defs><linearGradient id="${id}" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fff" stop-opacity="${t.earned?.9:.15}"/><stop offset=".35" stop-color="${c}"/><stop offset="1" stop-color="${t.earned?'#3a2a05':'#2a2c31'}"/></linearGradient></defs><g fill="url(#${id})" ${t.earned?'':'opacity=".35"'}>${body}</g></svg>`
}
function trophyRoomHTML(list, owner) {
  const L = list || trophies(), got = L.filter(t => t.earned).length, rows = [];
  for (let i = 0; i < L.length; i += 4) rows.push(L.slice(i, i + 4));
  return `<div class="card troom"><div class="ghd"><h3>${owner?esc(owner)+'’s trophy room':'Trophy room'}</h3><span class="sub">${got}/${L.length}</span></div>${rows.map(r=>`<div class="shelf">${r.map(t=>`<button class="tro${t.earned?' on':''}" data-tro="${esc(t.k)}" aria-label="${esc(t.name)}"><span class="t3d">${trophySVG(t)}</span></button>`).join('')}</div>`).join('')}<p class="sub" style="margin-top:8px">Tap a trophy to spin it.</p></div>`
}/* challenges: one slim row, tap to open */
{
  const _c = challengesHTML;
  challengesHTML = function() {
    const c = dayChallenges(st.date), n = c.filter(x => x.done).length;
    return `<button class="c17" id="chal17" aria-expanded="${!!st.chal17}"><span class="c17i">🎯</span><b>Challenges</b><span class="c17d">${c.map(x=>`<i class="${x.done?'on':''}"></i>`).join('')}</span><span class="sub">${n}/3</span><span class="chev">${st.chal17?'⌄':'›'}</span></button>${st.chal17?_c():''}`
  }
}

/* ---------- holiday drops (0.19) ---------- */
function easterLocal(y) {
  const a = y % 19, b = Math.floor(y / 100), c = y % 100, d = Math.floor(b / 4), e = b % 4, f = Math.floor((b + 8) / 25), g = Math.floor((b - f + 1) / 3), h = (19 * a + b - d - g + 15) % 30, i = Math.floor(c / 4), k = c % 4, l = (32 + 2 * e + 2 * i - h - k) % 7, m = Math.floor((a + 11 * h + 22 * l) / 451);
  return new Date(y, Math.floor((h + l - 7 * m + 114) / 31) - 1, ((h + l - 7 * m + 114) % 31) + 1)
}
const HOL19 = {
  thanks: {
    n: 'Thanksgiving', p: 'h_thanks', e: '🦃', fx: ['🍂', '🍁', '🦃'], b: 'Turkey Day Grind', c: '#e0903f', t: t => isThanks(t), w: t => t.getMonth() === 10 && t.getDate() >= 15
  },
  xmas: {
    n: 'Christmas', p: 'h_xmas', e: '🎄', fx: ['❄️', '🎄', '⭐'], b: 'Christmas Grind', c: '#e8424e', t: t => t.getMonth() === 11 && t.getDate() === 25, w: t => t.getMonth() === 11 && t.getDate() < 31
  },
  newyear: {
    n: 'New Year’s', p: 'h_newyear', e: '🎆', fx: ['🎉', '✨', '🎆'], b: 'New Year Grind', c: '#e3c25a', t: t => t.getMonth() === 0 && t.getDate() === 1, w: t => (t.getMonth() === 11 && t.getDate() === 31) || (t.getMonth() === 0 && t.getDate() <= 7)
  },
  valentine: {
    n: 'Valentine’s', p: 'h_valentine', e: '💘', fx: ['💘', '💗', '💕'], b: 'Valentine Grind', c: '#ff6fa8', t: t => t.getMonth() === 1 && t.getDate() === 14, w: t => t.getMonth() === 1 && t.getDate() >= 7 && t.getDate() <= 14
  },
  stpat: {
    n: 'St. Patrick’s', p: 'h_stpat', e: '☘️', fx: ['☘️', '🍀', '✨'], b: 'Lucky Grind', c: '#3fbf6a', t: t => t.getMonth() === 2 && t.getDate() === 17, w: t => t.getMonth() === 2 && t.getDate() >= 10 && t.getDate() <= 17
  },
  easter: {
    n: 'Easter', p: 'h_easter', e: '🐣', fx: ['🐣', '🥚', '🌸'], b: 'Easter Grind', c: '#f2a8d0', t: t => {
      const e = easterLocal(t.getFullYear());
      return t.getMonth() === e.getMonth() && t.getDate() === e.getDate()
    }, w: t => {
      const e = easterLocal(t.getFullYear()), d = (e - new Date(t.getFullYear(), t.getMonth(), t.getDate())) / 864e5;
      return d >= 0 && d <= 7
    }
  },
  july4: {
    n: 'Fourth of July', p: 'h_july4', e: '🎇', fx: ['🎆', '⭐', '🎇'], b: 'Independence Grind', c: '#4a6fd6', t: t => t.getMonth() === 6 && t.getDate() === 4, w: t => (t.getMonth() === 5 && t.getDate() >= 28) || (t.getMonth() === 6 && t.getDate() <= 4)
  },
  halloween: {
    n: 'Halloween', p: 'h_halloween', e: '🎃', fx: ['🎃', '🦇', '👻'], b: 'Halloween Grind', c: '#ff8a2a', t: t => t.getMonth() === 9 && t.getDate() === 31, w: t => t.getMonth() === 9 && t.getDate() >= 15
  }
};
function holYears(k) {
  const H = HOL19[k], Y = new Set();
  Object.values(st.sessions || {}).forEach(s => {
    if (!s || !s.date || s._del) return;
    const t = toD(s.date);
    if (H.t(t) && Object.values(s.ex || {}).some(a => Array.isArray(a) && a.some(full))) Y.add(t.getFullYear())
  });
  return [...Y].sort()
}
const holHas = k => holYears(k).length > 0;
const PLATE2HOL = Object.fromEntries(Object.entries(HOL19).map(([k, H]) => [H.p, k]));
function holFx(k, uid, big) {
  const u = 'hx' + uid, R = (n, a, b) => a + (hashS(u + n) % 1000) / 1000 * (b - a), fall = (n, draw, dur) => Array.from({length: n}, (_, i) => {
    const x = R('x' + i, 14, 86).toFixed(1), d = (dur || R('d' + i, 3, 6)).toFixed(1), b = (-R('b' + i, 0, 6)).toFixed(1);
    return `<g><animateTransform attributeName="transform" type="translate" values="${x} 6;${(+x+R('w'+i,-8,8)).toFixed(1)} 96" dur="${d}s" begin="${b}s" repeatCount="indefinite"/>${draw(i)}<animate attributeName="opacity" values="0;1;1;0" keyTimes="0;.15;.8;1" dur="${d}s" begin="${b}s" repeatCount="indefinite"/></g>`
  }).join('');
  const burst = (x, y, c, d, b) => `<g transform="translate(${x} ${y})">${Array.from({length:10},(_,i)=>`<path d="M0 0 L0 -9" stroke="${c}" stroke-width="1.2" stroke-linecap="round" transform="rotate(${i*36})"/>`).join('')}<animateTransform attributeName="transform" type="scale" additive="sum" values="0;1.2;1.3" dur="${d}s" begin="${b}s" repeatCount="indefinite"/><animate attributeName="opacity" values="0;1;0" dur="${d}s" begin="${b}s" repeatCount="indefinite"/></g>`;
  const rimLights = (cols) => Array.from({length: 16}, (_, i) => {
    const a = i * Math.PI / 8, x = (50 + 45.5 * Math.cos(a)).toFixed(1), y = (50 + 45.5 * Math.sin(a)).toFixed(1);
    return `<circle cx="${x}" cy="${y}" r="1.7" fill="${cols[i%cols.length]}"><animate attributeName="opacity" values="1;.25;1" dur="1.4s" begin="${(i%3)*.45}s" repeatCount="indefinite"/></circle>`
  }).join('');
  let o = '';
  if (k === 'thanks') o = `<circle cx="50" cy="50" r="46" fill="#ff9a3c" opacity=".12"><animate attributeName="opacity" values=".06;.2;.06" dur="4s" repeatCount="indefinite"/></circle>` + fall(big ? 7 : 4, i => `<path d="M0 -3 C3 -3 4 0 0 4 C-4 0 -3 -3 0 -3Z" fill="${['#e8650c','#c2410c','#f2b13c'][i%3]}"><animateTransform attributeName="transform" type="rotate" values="0;360" dur="3s" repeatCount="indefinite"/></path>`);
  else if (k === 'xmas') o = rimLights(['#ff3b47', '#34d27a', '#ffd24a', '#ffffff']) + fall(big ? 10 : 6, i => `<circle r="${i%3?1:1.5}" fill="#fff"/>`);
  else if (k === 'newyear') o = burst(28, 30, '#ffd24a', 2.2, 0) + burst(72, 34, '#ffffff', 2.6, .8) + burst(66, 72, '#e3c25a', 2.4, 1.5) + fall(big ? 8 : 4, i => `<rect x="-1" y="-1.5" width="2" height="3" fill="${['#ffd24a','#fff','#e3c25a'][i%3]}"><animateTransform attributeName="transform" type="rotate" values="0;360" dur="1.2s" repeatCount="indefinite"/></rect>`);
  else if (k === 'valentine') o = `<circle cx="50" cy="50" r="46" fill="#ff6fa8" opacity=".12"/>` + Array.from({length: big ? 6 : 4}, (_, i) => {
    const x = R('h' + i, 16, 84).toFixed(1), d = R('hd' + i, 3, 5).toFixed(1), b = (-R('hb' + i, 0, 5)).toFixed(1);
    return `<path d="M0 2 C-5 -2 -3 -6 0 -3.5 C3 -6 5 -2 0 2Z" fill="${i%2?'#ff4f8b':'#ffb3cf'}"><animateTransform attributeName="transform" type="translate" values="${x} 92;${x} 8" dur="${d}s" begin="${b}s" repeatCount="indefinite"/><animate attributeName="opacity" values="0;1;0" dur="${d}s" begin="${b}s" repeatCount="indefinite"/></path>`
  }).join('');
  else if (k === 'stpat') o = `<g><animateTransform attributeName="transform" type="rotate" values="0 50 50;360 50 50" dur="14s" repeatCount="indefinite"/>${Array.from({length:6},(_,i)=>`<g transform="rotate(${i*60} 50 50) translate(50 6)">${[0,120,240].map(r=>`<circle cx="0" cy="-1.6" r="1.6" fill="#5ed47f" transform="rotate(${r})"/>`).join('')}</g>`).join('')}</g>` + Array.from({length: 6}, (_, i) => `<circle cx="${R('cx'+i,20,80).toFixed(1)}" cy="${R('cy'+i,20,80).toFixed(1)}" r="1.3" fill="#ffd24a"><animate attributeName="opacity" values="0;1;0" dur="1.8s" begin="${(i*.3).toFixed(1)}s" repeatCount="indefinite"/></circle>`).join('');
  else if (k === 'easter') o = Array.from({length: 5}, (_, i) => {
    const a = i * Math.PI * 2 / 5 - Math.PI / 2, x = (50 + 36 * Math.cos(a)).toFixed(1), y = (50 + 36 * Math.sin(a)).toFixed(1);
    return `<ellipse cx="${x}" cy="${y}" rx="3.4" ry="4.4" fill="${['#ffd1e8','#c7f0ff','#fff3b0','#d8ffd1','#e8d1ff'][i]}" stroke="#fff" stroke-width=".6"><animateTransform attributeName="transform" type="rotate" values="-12 ${x} ${y};12 ${x} ${y};-12 ${x} ${y}" dur="1.6s" begin="${i*.2}s" repeatCount="indefinite"/></ellipse>`
  }).join('');
  else if (k === 'july4') o = burst(28, 30, '#ff3b47', 2.2, 0) + burst(72, 30, '#ffffff', 2.5, .7) + burst(50, 76, '#4a8cff', 2.3, 1.4) + Array.from({length: 8}, (_, i) => `<path d="M${R('sx'+i,16,84).toFixed(0)} ${R('sy'+i,16,84).toFixed(0)}m0 -2l.6 1.4 1.5 .1-1.1 1 .3 1.5-1.3-.8-1.3 .8 .3-1.5-1.1-1 1.5-.1z" fill="#fff"><animate attributeName="opacity" values=".1;1;.1" dur="1.6s" begin="${(i*.2).toFixed(1)}s" repeatCount="indefinite"/></path>`).join('');
  else if (k === 'halloween') o = `<circle cx="50" cy="50" r="25" fill="none" stroke="#ff8a2a" stroke-width="2.5" opacity=".7"><animate attributeName="opacity" values=".2;.9;.4;.8;.2" dur="1.3s" repeatCount="indefinite"/></circle>` + `<g><animateTransform attributeName="transform" type="rotate" values="0 50 50;-360 50 50" dur="7s" repeatCount="indefinite"/>${[0,1,2].map(i=>`<path transform="rotate(${i*120} 50 50) translate(50 10)" d="M-5 0 Q-3 -3 -1 -.5 Q0 -2 1 -.5 Q3 -3 5 0 Q3 -.6 1 1 Q0 .3 -1 1 Q-3 -.6 -5 0Z" fill="#1b0f24"/>`).join('')}</g>` + `<ellipse cx="50" cy="84" rx="30" ry="6" fill="#9b6bd6" opacity=".25"><animateTransform attributeName="transform" type="translate" values="-6 0;6 0;-6 0" dur="5s" repeatCount="indefinite"/></ellipse>`;
  return `<g class="holfx" pointer-events="none">${o}</g>`
}/* ---------- banners ---------- */
function holScene(k, uid) {
  const u = ('hs' + (uid || pid++)).replace(/[^\w-]/g, ''), R = (n, a, b) => a + (hashS(u + n) % 1000) / 1000 * (b - a);
  let o = '';
  const fall = (n, draw, y0, y1) => Array.from({length: n}, (_, i) => {
    const x = R('x' + i, 0, 380).toFixed(0), d = R('d' + i, 4, 9).toFixed(1), b = (-R('b' + i, 0, 9)).toFixed(1);
    return `<g><animateTransform attributeName="transform" type="translate" values="${x} ${y0||-10};${(+x+R('w'+i,-30,30)).toFixed(0)} ${y1||140}" dur="${d}s" begin="${b}s" repeatCount="indefinite"/>${draw(i)}</g>`
  }).join('');
  const fw = (x, y, c, d, b, r) => `<g transform="translate(${x} ${y})">${Array.from({length:14},(_,i)=>`<circle cx="0" cy="-${r||22}" r="1.6" fill="${c}" transform="rotate(${i*360/14})"/>`).join('')}<animateTransform attributeName="transform" type="scale" additive="sum" values="0;1;1.15" dur="${d}s" begin="${b}s" repeatCount="indefinite"/><animate attributeName="opacity" values="0;1;0" dur="${d}s" begin="${b}s" repeatCount="indefinite"/></g>`;
  const sky = (a, b) => `<defs><linearGradient id="${u}s" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${a}"/><stop offset="1" stop-color="${b}"/></linearGradient></defs><rect width="380" height="130" fill="url(#${u}s)"/>`;
  const city = c => `<path d="M0 130 V96 H24 V80 H44 V100 H62 V70 H80 V92 H104 V76 H122 V100 H146 V64 H160 V58 H170 V64 H184 V96 H206 V82 H230 V100 H250 V72 H268 V90 H292 V78 H312 V98 H334 V84 H352 V100 H380 V130 Z" fill="${c}"/>`;
  if (k === 'thanks') o = sky('#3a1d0e', '#c2601c') + `<circle cx="300" cy="96" r="34" fill="#ffb347" opacity=".55"/><path d="M0 130 V104 Q80 86 170 102 T380 96 V130 Z" fill="#4a2410"/>` + fall(18, i => `<path d="M0 -5 C5 -5 7 0 0 7 C-7 0 -5 -5 0 -5Z" fill="${['#e8650c','#c2410c','#f2b13c','#8a2b0c'][i%4]}"><animateTransform attributeName="transform" type="rotate" values="0;360" dur="${(2+i%3)}s" repeatCount="indefinite"/></path>`) + `<text x="18" y="22" style="font:900 9px Archivo,Inter,sans-serif;letter-spacing:5px" fill="#ffd9a8" opacity=".8">GIVING THANKS · LIFTING ANYWAY</text>`;
  else if (k === 'xmas') o = sky('#071226', '#1b2f5a') + `<path d="M300 112 L272 112 L300 38 L328 112 Z" fill="#0f3d24"/><path d="M300 30 l3 7 7 .5-5.5 4.5 2 7-6.5-4-6.5 4 2-7-5.5-4.5 7-.5z" fill="#ffd24a"><animate attributeName="opacity" values=".6;1;.6" dur="1.5s" repeatCount="indefinite"/></path><path d="M0 130 V112 Q190 100 380 112 V130 Z" fill="#e9f1ff"/>` + `<path d="M0 8 Q95 26 190 8 T380 8" stroke="#2a2a2a" stroke-width="1" fill="none"/>` + Array.from({length: 20}, (_, i) => {
    const x = i * 19 + 8, y = 8 + Math.sin(i * 19 / 380 * Math.PI * 2) * -9 + 9;
    return `<circle cx="${x}" cy="${(y).toFixed(1)}" r="3" fill="${['#ff3b47','#34d27a','#ffd24a','#4a8cff'][i%4]}"><animate attributeName="opacity" values="1;.3;1" dur="1.2s" begin="${(i%4)*.3}s" repeatCount="indefinite"/></circle>`
  }).join('') + fall(30, i => `<circle r="${i%3?1.2:2}" fill="#fff" opacity=".9"/>`);
  else if (k === 'newyear') o = sky('#05050c', '#141433') + fw(90, 40, '#ffd24a', 2.4, 0) + fw(200, 34, '#ffffff', 2.8, .9) + fw(310, 44, '#e3c25a', 2.5, 1.7) + fw(250, 60, '#ff6fa8', 2.6, .4, 16) + city('#0b0b16') + `<text x="190" y="118" text-anchor="middle" style="font:900 22px Archivo,Inter,sans-serif;letter-spacing:8px" fill="#e3c25a">${new Date().getMonth()===0?new Date().getFullYear():new Date().getFullYear()+1}<animate attributeName="opacity" values=".5;1;.5" dur="2s" repeatCount="indefinite"/></text>`;
  else if (k === 'valentine') o = sky('#3d0a24', '#d6457f') + Array.from({length: 16}, (_, i) => {
    const x = R('vx' + i, 10, 370).toFixed(0), d = R('vd' + i, 5, 9).toFixed(1), b = (-R('vb' + i, 0, 9)).toFixed(1), s = R('vs' + i, .6, 1.6).toFixed(2);
    return `<g><animateTransform attributeName="transform" type="translate" values="${x} 140;${x} -14" dur="${d}s" begin="${b}s" repeatCount="indefinite"/><path transform="scale(${s})" d="M0 6 C-12 -4 -7 -14 0 -8 C7 -14 12 -4 0 6Z" fill="${i%2?'#ff4f8b':'#ffc2d8'}"/></g>`
  }).join('');
  else if (k === 'stpat') o = sky('#0b3d1f', '#2a8a4a') + `<g fill="none" stroke-width="6" opacity=".5">${['#ff3b47','#ff9a3c','#ffd24a','#34d27a','#4a8cff','#8a5cff'].map((c,i)=>`<path d="M60 140 A140 120 0 0 1 340 140" stroke="${c}" transform="translate(0 ${i*7})"/>`).join('')}</g><path d="M0 130 V108 Q190 92 380 108 V130 Z" fill="#0f5a2a"/>` + Array.from({length: 10}, (_, i) => `<circle cx="${R('gx'+i,150,250).toFixed(0)}" cy="${R('gy'+i,96,118).toFixed(0)}" r="3" fill="#ffd24a"><animate attributeName="opacity" values=".3;1;.3" dur="1.4s" begin="${(i*.15).toFixed(2)}s" repeatCount="indefinite"/></circle>`).join('');
  else if (k === 'easter') o = sky('#bfe6ff', '#ffe1f0') + `<path d="M0 130 V100 Q100 88 190 100 T380 96 V130 Z" fill="#8fd48a"/>` + Array.from({length: 7}, (_, i) => {
    const x = 40 + i * 48, y = 104 + (i % 2) * 6;
    return `<ellipse cx="${x}" cy="${y}" rx="9" ry="12" fill="${['#ffd1e8','#c7f0ff','#fff3b0','#d8ffd1','#e8d1ff'][i%5]}" stroke="#fff" stroke-width="2"><animateTransform attributeName="transform" type="rotate" values="-8 ${x} ${y+10};8 ${x} ${y+10};-8 ${x} ${y+10}" dur="2s" begin="${i*.25}s" repeatCount="indefinite"/></ellipse>`
  }).join('') + fall(10, i => `<circle r="2" fill="${['#ffb3cf','#fff'][i%2]}"/>`, -10, 90);
  else if (k === 'july4') o = sky('#050a1f', '#16224d') + fw(80, 40, '#ff3b47', 2.4, 0) + fw(190, 30, '#ffffff', 2.7, .8) + fw(300, 42, '#4a8cff', 2.5, 1.5) + city('#070b1a') + `<g opacity=".5">${Array.from({length:7},(_,i)=>`<rect y="${118+(i%2)}" x="${i*55}" width="55" height="12" fill="${i%2?'#fff':'#c8202b'}"/>`).join('')}</g>`;
  else if (k === 'halloween') o = sky('#12051f', '#3d1a5c') + `<circle cx="300" cy="40" r="26" fill="#ffe9a8"/><circle cx="292" cy="34" r="26" fill="#3d1a5c" opacity=".12"/>` + Array.from({length: 5}, (_, i) => `<g><animateTransform attributeName="transform" type="translate" values="${-40+i*30} ${30+i*12};${420} ${10+i*14}" dur="${8+i*2}s" begin="${-i*2}s" repeatCount="indefinite"/><path d="M-10 0 Q-6 -6 -2 -1 Q0 -4 2 -1 Q6 -6 10 0 Q6 -1 2 2 Q0 .5 -2 2 Q-6 -1 -10 0Z" fill="#0b0410"/></g>`).join('') + `<path d="M0 130 V112 Q190 100 380 112 V130 Z" fill="#1a0b26"/>` + [60, 120, 330].map((x, i) => `<g transform="translate(${x} 110)"><ellipse rx="16" ry="12" fill="#ff7a1a"/><path d="M-6 -3 l3 -4 3 4z M3 -3 l3 -4 3 4z M-8 4 Q0 10 8 4" fill="#ffe14a"><animate attributeName="opacity" values="1;.4;1;.7;1" dur="${1+i*.3}s" repeatCount="indefinite"/></path><rect x="-1.5" y="-15" width="3" height="5" fill="#2f5a1a"/></g>`).join('') + `<ellipse cx="190" cy="120" rx="200" ry="10" fill="#b48cff" opacity=".15"><animateTransform attributeName="transform" type="translate" values="-20 0;20 0;-20 0" dur="7s" repeatCount="indefinite"/></ellipse>`;
  return `<svg class="scene" viewBox="0 0 380 130" preserveAspectRatio="xMidYMid slice" aria-hidden="true">${o}</svg>`
}/* dated medals in the trophy room */
{
  const _t = trophies;
  trophies = function() {
    const out = _t();
    Object.entries(HOL19).forEach(([k, H]) => holYears(k).forEach(y => out.push({
      k: 'hol_' + k + '_' + y, type: 'medal', name: H.b + ' ' + y, how: 'Lifted on ' + H.n + ' ' + y, earned: true, color: H.c
    })));
    return out
  }
}
/* tell the crew when you lift on a holiday (3 lifters unlock it for everyone) */
async function holCheck19() {
  try {
    if (!crewOn() || !st.cs) return;
    const t = new Date(), y = t.getFullYear(), k = Object.keys(HOL19).find(k => HOL19[k].t(t));
    if (!k) return;
    const S = st.set.holSent = st.set.holSent || {};
    if (S[k + y]) return;
    const today = Object.values(st.sessions).some(s => s && s.date === iso(t) && !s._del && Object.values(s.ex || {}).some(a => Array.isArray(a) && a.some(full)));
    if (!today) return;
    const r = await crewCall('holiday', {hk: k, year: y});
    if (r && !r.error) {
      S[k + y] = 1;
      saveSet()
    }
  } catch (e) {}
}
/* the drop reveal */
function holReveal19() {
  try {
    const seen = st.set.holSeen = st.set.holSeen || {};
    for (const [k, H] of Object.entries(HOL19)) {
      const Y = holYears(k), y = Y[Y.length - 1];
      if (y && !seen[k + y]) {
        if (st.sheet && !AUTO18.includes(st.sheet.type)) {
          clearTimeout(window.__hr19b);
          window.__hr19b = setTimeout(holReveal19, 2500);
          return
        }
        seen[k + y] = 1;
        saveSet();
        st.sheet = {type: 'hol19', k, y};
        renderSheet();
        return
      }
    }
  } catch (e) {}
}
function holSheet(sh) {
  const H = HOL19[sh.k], k = sh.k;
  return `<div class="hol19"><div class="hol19ban">${holScene(k,'rv'+k)}</div><div class="hol19pl">${plateSVG(H.p,'done',{big:true,label:H.n.toUpperCase()})}</div><h2>${H.e} ${esc(H.n)} Drop unlocked</h2><p class="sub">You lifted on ${esc(H.n)} ${sh.y}. Here’s your set:</p>
 <div class="card rows15">${row16('data-holgo="plate"','💿','Animated plate','Plan → Plate → Holiday')}${row16('data-holgo="banner"','🖼','Profile banner','Edit profile → Banner')}${row16('data-holgo="name"','✨','Name effect','Customize → Name effect')}${row16('data-holgo="react"',H.e,'Crew reaction','Shows on posts')}${row16('data-holgo="fx"','💥','Set complete burst','Customize → Set complete effect')}${row16('data-holgo="medal"','🏅',esc(H.b)+' '+sh.y,'In your trophy room')}</div>
 <p class="sub" style="text-align:center">If 3 people in your crew lift on ${esc(H.n)}, the whole crew unlocks a holiday crew banner and a seasonal crest.</p><button class="btn acc wide" id="hol19wear" style="min-height:52px">Wear the whole set</button><button class="btn wide" id="sheetx" style="margin-top:8px">Later</button></div>`
}async function sheetClick19(b) {
  const sh = st.sheet;
  if (!sh || sh.type !== 'hol19') return false;
  if (b.id === 'hol19wear') {
    const k = sh.k, L = st.set.look = st.set.look || {};
    L.pbanner = 'hb_' + k;
    L.nameFx = 'h' + k;
    L.setfx = 'h' + k;
    saveSet();
    try {applyLook()} catch (e) {}confetti(120);
    toast(HOL19[k].e + ' Holiday set on');
    st.sheet = null;
    renderSheet();
    render();
    try {syncCrew(true)} catch (e) {}return true
  }
  if (b.dataset.holgo) {
    const g = b.dataset.holgo;
    st.sheet = null;
    renderSheet();
    if (g === 'banner' || g === 'name' || g === 'fx' || g === 'react') {
      st.profile = true;
      st.settings = false;
      render();
      setTimeout(() => {
        const x = $(g === 'banner' ? 'profedit2' : 'lockerbtn');
        if (x) x.click()
      }, 150)
    } else if (g === 'medal') {
      st.profile = true;
      render();
      setTimeout(() => {
        const x = $('troombtn');
        if (x) x.click()
      }, 150)
    } else {
      st.tab = 'plan';
      st.profile = false;
      render()
    }
    return true
  }
  return false
}function boot19() {
  try {
    AUTO18.push('hol19');
    setTimeout(holReveal19, 2500);
    setTimeout(holCheck19, 6000);
    setInterval(holCheck19, 60000);
    document.addEventListener('visibilitychange', () => {if (!document.hidden) {setTimeout(holReveal19, 800); setTimeout(holCheck19, 2000)}})
  } catch (e) {console.warn('boot19', e)}
}
/* ---------- 0.21: medal awards ---------- */
const MTL21 = {
  bronze: ['#ffe3c8', '#c98a4f', '#6b3a17', '#2a1406', 'Bronze', '#7a2a1c', '#e8a46a'], silver: ['#ffffff', '#c6cfdb', '#5f6a79', '#1d232c', 'Silver', '#1f3f73', '#d9e1ec'], gold: ['#fff5c9', '#e8b923', '#8a6209', '#2e1f02', 'Gold', '#7a1020', '#f3cf5a'], plat: ['#f0fffc', '#93e2d8', '#2f7b74', '#0d302d', 'Platinum', '#0f4f4a', '#c4f5ee'], diam: ['#eef6ff', '#8cc4ff', '#2457a8', '#0b1e47', 'Diamond', '#13255a', '#a8d0ff'], legend: ['#fbe8ff', '#d083ff', '#5a1a80', '#200631', 'Legend', '#3a0f57', '#eab0ff'], obs: ['#d7dbe3', '#4a505c', '#15181d', '#050608', 'Secret', '#121418', '#d4a93c']
};
const TIER21 = ['bronze', 'silver', 'gold', 'plat', 'diam', 'legend'];
const IC21 = {
  dumbbell: 'M3 10v4M6 7.5v9M18 7.5v9M21 10v4M6 12h12', flame: 'M12 3c1 3 5 5 5 10a5 5 0 0 1-10 0c0-3 2-4.5 2-6.5 1.2 1 2 2.2 2 3.5 1-2 1-5 1-7z', stack: 'M4 19h16M6 15h12M8 11h8M10 7h4', check: 'M5 12.5l4.5 4.5L19 7.5', trophy: 'M8 4h8v5a4 4 0 0 1-8 0zM8 6H5a3 3 0 0 0 3 4M16 6h3a3 3 0 0 1-3 4M12 13v4M8.5 20h7M10 17h4', calendar: 'M5 6h14v13H5zM5 10h14M9 4v4M15 4v4M9 14h2M13 14h2', pencil: 'M5 19l1-4L16 5l3 3L9 18zM14 7l3 3', compass: 'M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM15.5 8.5l-2 5-5 2 2-5z', scale: 'M12 4v16M8 20h8M5 7h14M5 7l-2.5 6h5zM19 7l-2.5 6h5z', sunrise: 'M3 17h18M7 17a5 5 0 0 1 10 0M12 6v3M5.6 10.6l2 1.6M18.4 10.6l-2 1.6M8 20.5h8', moon: 'M16 4a8 8 0 1 0 4 12A7 7 0 0 1 16 4z', loop: 'M5 12a7 7 0 0 1 12-5l2 2M19 4v5h-5M19 12a7 7 0 0 1-12 5l-2-2M5 20v-5h5', shield: 'M12 3l7 3v5c0 5-3 8-7 10-4-2-7-5-7-10V6zM12 7v10', leaf: 'M5 19c0-9 5-14 14-14 0 9-5 14-14 14zM5 19l8-8', hourglass: 'M7 3h10M7 21h10M8 3c0 5 8 6 8 9s-8 4-8 9M16 3c0 5-8 6-8 9s8 4 8 9', bolt: 'M13 3L5 13h6l-1 8 8-10h-6z', target: 'M12 4a8 8 0 1 0 0 16a8 8 0 1 0 0-16zM12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8zM12 11.2a.8.8 0 1 0 0 1.6a.8.8 0 1 0 0-1.6z', people: 'M8 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM16 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM3 19c0-3 2-5 5-5s5 2 5 5M11 19c0-3 2-5 5-5s5 2 5 5', mountain: 'M2.5 19l6.5-11 4 6.5 3-4 5.5 8.5zM7.5 10.5l1.5 1.5 1.5-1', megaphone: 'M4 10v4h3l8 4.5V5.5L7 10zM18 9.5a3.5 3.5 0 0 1 0 5M7 14l1.2 5', swords: 'M4 4l11 11M20 4L9 15M12.5 17.5l5-5M6.5 12.5l5 5M4 20l3-3M20 20l-3-3', hands: 'M2.5 11l4-4 4 2 2.5-1.5L16 9l5.5 2M6.5 12l4 4a1.4 1.4 0 0 0 2-2M10 15.5l1.6 1.6a1.4 1.4 0 0 0 2-2M12.4 17.4l.8.8a1.4 1.4 0 0 0 2-2l-4-4M16 9l-3.5 3', flag: 'M6 21V4M6 4h11l-2.5 4L17 12H6', columns: 'M4 20h16M5 9h14M12 4l8 5H4zM7 9v11M12 9v11M17 9v11', clock: 'M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM12 7v5l3.5 2', key: 'M15.5 3.5a5 5 0 1 0 0 10 5 5 0 0 0 0-10zM12 12l-8.5 8.5M5.5 18.5l2 2M8.5 15.5l2 2', crown: 'M4 17l-1-9 5 4 4-7 4 7 5-4-1 9zM4 20h16', chart: 'M4 20h16M6 16l4-5 3 3 6-7M15 7h4v4', arrow: 'M4 20L19 5M13 5h6v6M4 20l3.5-.5M4 20l.5-3.5', map: 'M3 6l6-2 6 2 6-2v14l-6 2-6-2-6 2zM9 4v14M15 6v14', broom: 'M19 3l-7.5 7.5M8 10l6 6-3 5H4v-7z', star: 'M12 3l2.6 5.6 6 .7-4.5 4.1 1.2 6-5.3-3-5.3 3 1.2-6-4.5-4.1 6-.7z'
};
const MK21 = {
  w: [[1, 10, 25, 50, 100, 250], 'dumbbell'], s: [[2, 4, 8, 12, 26, 52], 'flame'], t: [[100, 500, 1000, 2500, 5000], 'check'], p: [[1, 10, 25, 50, 100], 'trophy']
};
const MFIX21 = {
  pm: ['gold', 'calendar'], nt: ['silver', 'pencil'], ex: ['silver', 'compass'], bal: ['silver', 'scale'], dawn: ['obs', 'sunrise'], owl: ['obs', 'moon'], back: ['obs', 'loop'], wknd: ['obs', 'shield'], hol: ['obs', 'leaf'], mara: ['obs', 'hourglass'], spd: ['obs', 'bolt'], cent: ['obs', '#100'], perf: ['obs', 'target'], squad: ['plat', 'people'], cmil: ['plat', 'mountain'], hype: ['plat', 'megaphone'], rival: ['plat', 'swords'], recruit: ['plat', 'hands'], team: ['plat', 'flag'], col_club: ['legend', 'columns'], col_clock: ['legend', 'clock'], col_secret: ['legend', 'key'], col_week: ['legend', 'calendar'], col_ms: ['legend', 'crown']
};
const MWK21 = {
  fullweek: 'flame', overload: 'chart', prhunt: 'arrow', speed: 'bolt', dawnwk: 'sunrise', quest: 'map', sweep: 'broom'
};
function medalOf21(k, name, count) {
  k = String(k || '');
  let m = 'silver', ic = null;
  const a = k.match(/^([wstp])(\d+)$/);
  if (a && MK21[a[1]]) {
    const i = MK21[a[1]][0].indexOf(+a[2]);
    if (i >= 0) {
      m = TIER21[Math.min(5, i)];
      ic = MK21[a[1]][1]
    }
  }
  if (!ic) {
    const v = k.match(/^v(\d)$/);
    if (v) {
      m = TIER21[Math.min(5, +v[1])];
      ic = 'stack'
    }
  }
  if (!ic) {
    const c = k.match(/^c(\d)$/);
    if (c) {
      m = TIER21[Math.min(5, +c[1])];
      const n = String(name || '').match(/^\d+/);
      ic = '#' + (n ? n[0] : '')
    }
  }
  if (!ic && MFIX21[k]) {[m, ic] = MFIX21[k]}
  if (!ic && MWK21[k]) {
    ic = MWK21[k];
    const n = +count || 0;
    m = n >= 25 ? 'plat' : n >= 10 ? 'gold' : n >= 3 ? 'silver' : 'bronze'
  }
  return {m, ic}
}
let mid21 = 0;
const MT21 = {
  bronze: ['#ffe6cc', '#e8ab72', '#b5703a', '#6e3c17', '#2e1606'], silver: ['#ffffff', '#e6ebf1', '#adb7c3', '#677382', '#262d36'], gold: ['#fffbe2', '#f7d97c', '#d6a63a', '#8a6212', '#3c2905'], plat: ['#f5fffd', '#cdf3ee', '#88d1c8', '#3d8880', '#123532'], diam: ['#f3f9ff', '#c8e2ff', '#82b6f2', '#3965b5', '#0f2252'], legend: ['#fdf2ff', '#ebc6ff', '#ba7cea', '#682a98', '#24083a'], obs: ['#8d95a3', '#545b68', '#2c3139', '#15181d', '#050607']
};
const GEO21 = (() => {
  const cx = 60, cy = 60;
  let reed = '', gl = '', rings = '', leaves = '';
  for (let i = 0; i < 90; i++) {
    const t = i * Math.PI / 45, c = Math.cos(t), s = Math.sin(t);
    reed += `M${(cx+51.2*c).toFixed(2)} ${(cy+51.2*s).toFixed(2)}L${(cx+54*c).toFixed(2)} ${(cy+54*s).toFixed(2)}`
  }
  for (let i = 0; i < 72; i++) {
    const t = i * Math.PI / 36, c = Math.cos(t), s = Math.sin(t);
    gl += `M${(cx+9*c).toFixed(2)} ${(cy+9*s).toFixed(2)}L${(cx+35*c).toFixed(2)} ${(cy+35*s).toFixed(2)}`
  }
  for (let r = 12; r < 35; r += 2.6) rings += `<circle cx="${cx}" cy="${cy}" r="${r.toFixed(1)}"/>`;
  const L = (a, side) => {
    const t = a * Math.PI / 180, x = cx + 29.5 * Math.cos(t), y = cy + 29.5 * Math.sin(t), rot = a + (side > 0 ? 60 : -60);
    return `<ellipse cx="${x.toFixed(2)}" cy="${y.toFixed(2)}" rx="3.6" ry="1.5" transform="rotate(${rot.toFixed(1)} ${x.toFixed(2)} ${y.toFixed(2)})"/>`
  };
  for (let a = 100; a <= 215; a += 13) leaves += L(a, 1);
  for (let a = 80; a >= -35; a -= 13) leaves += L(a, -1);
  return {reed, gl, rings, leaves}
})();
function medal21(k, o) {
  o = o || {};
  const {m, ic} = medalOf21(k, o.name, o.count), C = MT21[m], id = 'md' + (mid21++), sz = o.size || 64, cx = 60, cy = 60, G = GEO21;
  const ink = m === 'obs' ? ['#fff1b8', '#e8b923', '#8a6209'] : [C[0], C[2], C[4]], band = sz >= 56 && o.text !== false;
  const shine = !o.dim && ['gold', 'plat', 'diam', 'legend'].includes(m);
  const nm = esc(String(o.name || '').toUpperCase()), fs = nm.length > 16 ? 5.4 : nm.length > 13 ? 6.2 : 7.2, tier = MTL21[m][4].toUpperCase();
  let face;
  if (ic && ic[0] === '#') {
    const t = ic.slice(1), f = t.length >= 3 ? 19 : 23;
    face = `<g font-family="Inter,system-ui,sans-serif" font-weight="900" font-size="${f}" text-anchor="middle"><text x="${cx+1}" y="${cy+8}" fill="${C[4]}" opacity=".7">${t}</text><text x="${cx-.5}" y="${cy+6.5}" fill="${ink[0]}">${t}</text><text x="${cx}" y="${cy+7}" fill="url(#${id}i)">${t}</text></g>`
  } else if (ic && IC21[ic]) {
    const p = IC21[ic];
    face = `<g transform="translate(${cx} ${cy}) scale(1.32) translate(-12 -12)" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="${p}" stroke="${C[4]}" stroke-opacity=".7" stroke-width="3.2" transform="translate(.8 1)"/><path d="${p}" stroke="${ink[0]}" stroke-width="2.6" transform="translate(-.45 -.45)"/><path d="${p}" stroke="url(#${id}i)" stroke-width="2.2"/></g>`
  } else face = `<text x="${cx}" y="${cy+9}" text-anchor="middle" font-size="26">${esc(o.emoji||'⭐')}</text>`;
  return `<svg class="md21svg${o.dim?' dim':''}${shine?' shn':''}" viewBox="0 0 120 120" width="${sz}" height="${sz}" aria-hidden="true"><defs>
 <linearGradient id="${id}r" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${C[0]}"/><stop offset=".18" stop-color="${C[1]}"/><stop offset=".38" stop-color="${C[3]}"/><stop offset=".52" stop-color="${C[1]}"/><stop offset=".7" stop-color="${C[2]}"/><stop offset=".86" stop-color="${C[3]}"/><stop offset="1" stop-color="${C[4]}"/></linearGradient>
 <linearGradient id="${id}v" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C[0]}"/><stop offset=".5" stop-color="${C[2]}"/><stop offset="1" stop-color="${C[4]}"/></linearGradient>
 <linearGradient id="${id}w" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C[4]}"/><stop offset=".5" stop-color="${C[3]}"/><stop offset="1" stop-color="${C[0]}"/></linearGradient>
 <radialGradient id="${id}b" cx=".4" cy=".35" r=".75"><stop offset="0" stop-color="${C[2]}"/><stop offset=".7" stop-color="${C[3]}"/><stop offset="1" stop-color="${C[4]}"/></radialGradient>
 <radialGradient id="${id}f" cx=".36" cy=".3" r=".85"><stop offset="0" stop-color="${C[0]}"/><stop offset=".35" stop-color="${C[1]}"/><stop offset=".75" stop-color="${C[2]}"/><stop offset="1" stop-color="${C[3]}"/></radialGradient>
 <linearGradient id="${id}i" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${ink[0]}"/><stop offset=".55" stop-color="${ink[1]}"/><stop offset="1" stop-color="${ink[2]}"/></linearGradient>
 <radialGradient id="${id}s" cx=".3" cy=".22" r=".5"><stop offset="0" stop-color="#fff" stop-opacity=".55"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></radialGradient>
 <path id="${id}t" d="M18 60 A42 42 0 0 1 102 60"/><path id="${id}u" d="M13.5 60 A46.5 46.5 0 0 0 106.5 60"/>
 <clipPath id="${id}k"><circle cx="60" cy="60" r="54"/></clipPath></defs>
 <circle cx="60" cy="63" r="54" fill="${C[4]}"/><circle cx="60" cy="60" r="54" fill="url(#${id}r)"/>
 <path d="${G.reed}" stroke="${C[4]}" stroke-opacity=".45" stroke-width=".9"/>
 <circle cx="60" cy="60" r="49.5" fill="none" stroke="url(#${id}v)" stroke-width="2"/>
 ${band?`<circle cx="60" cy="60" r="48.5" fill="url(#${id}b)"/><g font-family="Inter,system-ui,sans-serif" font-weight="800" letter-spacing="1.6" text-anchor="middle">
 <text font-size="${fs}" fill="${C[0]}" opacity=".55" dy=".7"><textPath href="#${id}t" startOffset="50%">${nm}</textPath></text>
 <text font-size="${fs}" fill="${C[4]}"><textPath href="#${id}t" startOffset="50%">${nm}</textPath></text>
 <text font-size="6" fill="${C[0]}" opacity=".55" dy="-.4"><textPath href="#${id}u" startOffset="50%">★ ${tier} ★</textPath></text>
 <text font-size="6" fill="${C[4]}" dy="-1"><textPath href="#${id}u" startOffset="50%">★ ${tier} ★</textPath></text></g>`:''}
 <circle cx="60" cy="60" r="${band?37.5:48}" fill="none" stroke="url(#${id}w)" stroke-width="2.4"/>
 <circle cx="60" cy="60" r="${band?36.3:46.8}" fill="url(#${id}f)"/>
 <g transform="translate(60 60) scale(${band?1:1.28}) translate(-60 -60)">
 <path d="${G.gl}" stroke="${C[0]}" stroke-opacity=".16" stroke-width=".5"/>
 <g fill="none" stroke="${C[3]}" stroke-opacity=".14" stroke-width=".45">${G.rings}</g>
 <g fill="${C[1]}" stroke="${C[3]}" stroke-width=".5" opacity=".9">${G.leaves}</g>${face}</g>
 <g clip-path="url(#${id}k)"><ellipse cx="46" cy="38" rx="44" ry="30" fill="url(#${id}s)"/>${shine?`<rect class="md21sw" x="-40" y="-10" width="18" height="140" fill="#fff" opacity=".35" transform="rotate(20 60 60)"/>`:''}</g>
 ${o.count>1?`<g><rect x="80" y="94" width="34" height="18" rx="9" fill="#0b0c10" stroke="${C[1]}" stroke-width="1.2"/><text x="97" y="107" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-weight="800" font-size="11" fill="#fff">×${o.count>99?'99+':o.count}</text></g>`:''}
 </svg>`
}
function tierName21(k, name, count) {
  const {m} = medalOf21(k, name, count);
  return MTL21[m][4]
}
function badgeMedal21(b, size, dim) {return medal21(b.k, {name: b.name, count: b.count, emoji: b.icon, size, dim})}
/* profile shelf */
function shelf21(show) {
  const slots = [0, 1, 2].map(i => {
    const b = show[i];
    if (b) return `<button class="md21" data-bdg="${esc(b.k)}" aria-label="${esc(b.name)}"><span class="md21i">${badgeMedal21(b,84)}</span><b>${esc(b.name)}</b><i>${tierName21(b.k,b.name,b.count)}${b.count?` · ×${b.count}`:''}</i></button>`;
    return `<button class="md21 add" id="pinpick" aria-label="Pin a badge"><span class="md21e">＋</span><b>Pin</b><i>Badge</i></button>`
  });
  return `<div class="sh21"><div class="sh21row">${slots.join('')}</div><div class="sh21ledge"></div></div>`
}function openEdit21(sec) {
  const P = JSON.parse(JSON.stringify(st.set.prof || {}));
  st.sheet = {
    type: 'profedit', p: P, av: {...(st.set.av || {})}, flex: (st.set.flex || ['best', 'streak', 'vol']).slice(), rim: st.set.rim || '', sec, direct: 1
  };
  renderSheet()
}
function tap21(k) {
  st.set.tap21 = st.set.tap21 || {};
  if (!st.set.tap21[k]) {
    st.set.tap21[k] = 1;
    saveSet()
  }
  openEdit21(k === 'ban' ? 'banner' : 'plate21');
  render()
}
function commit21(sh) {
  grab16(sh);
  st.set.prof = sh.p;
  st.set.av = sh.av && (sh.av.t || sh.av.c) ? sh.av : null;
  st.set.flex = sh.flex;
  st.set.rim = (sh.rim || '').slice(0, 24);
  saveSet();
  st.sheet = null;
  renderSheet();
  render();
  try {syncCrew(true)} catch (e) {}toast('Saved')
}
const PEHIDE21 = ['banner', 'avatar', 'tpins', 'fav', 'rim', 'plate21'];
/* ---------- 0.22: one place for ranking up ---------- */
function lbOf22(v) {return U() === 'kg' ? v * 2.20462 : v}
function bar22(cur, tgt, label, sub, fmt) {
  const F = fmt || (v => (+v).toLocaleString());
  const p = Math.max(0, Math.min(100, Math.round(cur / Math.max(1, tgt) * 100))), done = cur >= tgt;
  return `<div class="b22${done?' done':''}"><div class="b22h"><span>${label}</span><b>${done?'✓':`${F(cur)}/${F(tgt)}`}</b></div><i><u style="width:${p}%"></u></i>${sub?`<div class="b22s">${sub}</div>`:''}</div>`
}
function meNear22() {
  const E = engine(), nx = RANKS[E.rank + 1];
  if (E.trial) return true;
  if (!nx) return false;
  const a = nx[1] - E.active, b = nx[2] - E.bcount;
  return (a <= 0 && b <= 1) || (b <= 0 && a <= 1)
}
function crewNear22() {
  if (!st.cs || !st.cs.crew) return false;
  const R = crewRankInfo();
  return !!R.nx && R.nx - R.pts <= 100
}
function crewItems22() {
  const c = st.cs.crew, T = c.trophies || {}, ms = (st.cs.members || []).filter(m => m.profile), me = myId(), S = crewSum();
  const nm = m => m.id === me ? 'You' : esc(String(m.name || '').split(' ')[0]);
  const wk = m => wkOf(m.profile), out = [];
  const per = (f, t) => ms.map(m => `<span class="${f(m)>=t?'ok':''}">${nm(m)} ${Math.min(f(m),t)}/${t}</span>`).join('');
  const add = (k, body) => {
    const d = CTROPHY.find(x => x[0] === k);
    out.push({k, ic: d[2], n: d[1], desc: d[4], won: !!T[k], body})
  };
  add('unbroken', ms.length < 2 ? '<span>Needs 2+ members in the crew</span>' : per(m => wk(m).days, 1));
  add('ironweek', ms.length < 2 ? '<span>Needs 2+ members in the crew</span>' : per(m => wk(m).days, 3));
  add('prstorm', bar22(ms.reduce((a, m) => a + (+wk(m).prs || 0), 0), 10, 'Crew PRs this week'));
  add('fullhouse', ms.length < 3 ? `<span>Needs 3+ members · you have ${ms.length}</span>` : '<span>Pick a day you all train</span>');
  add('century', bar22(S.wo, 100, 'Crew workouts'));
  add('mpc', bar22(Math.round(lbOf22(S.vol)), 1e6, 'Lifted together (lb)', '', v => v >= 1e6 ? (v / 1e6) + 'M' : kfmt(v)));
  add('comeback', '');
  return out
}
function rk22(sh) {
  const tab = sh.tab || 'you', E = engine();
  let h = `<div class="ghd"><h2>Rank up</h2><button class="btn sm" id="sheetx">Done</button></div><div class="seg2 rk22seg">${[['you','You'],['crew','Crew'],['plates','Plates']].map(([k,l])=>`<button data-rk22="${k}" aria-pressed="${tab===k}">${l}${k==='you'&&meNear22()||k==='crew'&&crewNear22()?'<i class="nd22"></i>':''}</button>`).join('')}</div>`;
  if (tab === 'you') {
    const r = RANKS[E.rank], nx = RANKS[E.rank + 1];
    h += `<div class="rk22hero"><div class="rk22em">${emblem(E.rank,'spin')}</div><div><div class="sub">YOUR RANK</div><h2>${esc(E.rname||r[0])}</h2><div class="sub">${E.power||0} rank power · 🔥 ${E.streak} wk streak</div></div></div>`;
    if (E.trial) {
      const t = E.trial;
      h += `<div class="rk22card hot"><div class="glbl">🔥 PROMOTION TRIAL · ${RANKS[t.tier][0].toUpperCase()}</div><p class="sub">You’ve earned a shot at ${RANKS[t.tier][0]}. Pass it this week to rank up. Miss it and you try again next week.</p>${bar22(t.days,t.need,'Train on your planned days')}${bar22(t.beat,t.needBeat,'Beat last week on lifts')}</div>`
    } else if (nx) {
      h += `<div class="rk22card"><div class="glbl">NEXT: ${nx[0].toUpperCase()}</div><p class="sub">Fill both bars to start a one-week promotion trial.</p>${bar22(E.active,nx[1],'Active weeks','A week counts when you train 2+ times')}${bar22(E.bcount,nx[2],'Badges earned','Every badge counts, weekly ones too')}</div>`
    } else h += `<div class="rk22card"><div class="glbl">TOP RANK</div><p class="sub">You’re ${r[0]}. Nothing above this. Keep the streak alive.</p></div>`;
    const nxU = UNLOCKS.find(u => u[0] === E.rank + 1);
    h += `<div class="rk22card"><div class="glbl">HOW IT WORKS</div><ol class="rk22how"><li><b>Train</b> 2+ times a week to bank active weeks.</li><li><b>Earn badges</b> by hitting milestones, PRs and weekly goals.</li><li><b>Pass the trial:</b> one week on your plan, beating last week on 3 lifts.</li></ol>${nxU?`<p class="sub" style="margin:8px 0 0">${nx[0]} unlocks: <b>${esc(nxU[1])}</b> and the ${esc(nx[0])} plate. See Plates.</p>`:''}</div>`
  } else if (tab === 'crew') {
    if (!st.cs || !st.cs.crew) {
      h += `<div class="rk22card"><p class="sub">Join or start a crew to rank up together. Crew ranks unlock better crew banners and a fancier crest.</p><button class="btn acc wide" id="rk22join">Go to Crew</button></div>`;
      return h
    }
    const R = crewRankInfo(), c = st.cs.crew, items = crewItems22(), r = st.cs.raid;
    h += `<div class="rk22hero"><div class="rk22em">${crestSVG(c.crest,R.r,64)}</div><div><div class="sub">${esc(c.name).toUpperCase()}</div><h2>${RANKS[R.r][0]}</h2><div class="sub">${R.pts.toLocaleString()} crew points</div></div></div>`;
    h += `<div class="rk22card">${R.nx?bar22(R.pts,R.nx,`Next: ${RANKS[R.r+1][0]}`,`${(R.nx-R.pts).toLocaleString()} points to go`):'<p class="sub">Top crew rank. Legends.</p>'}</div>`;
    h += `<div class="glbl rk22t">CREW TROPHIES · +400 EACH</div><div class="rk22list">${items.map(x=>`<div class="rk22i${x.won?' won':''}"><span class="rk22ic">${x.won?x.ic:x.ic}</span><div class="rk22b"><div class="rk22n"><b>${esc(x.n)}</b>${x.won?'<em>Won ✓</em>':'<em>+400</em>'}</div><div class="sub">${esc(x.desc)}</div>${x.won||!x.body?'':`<div class="rk22p">${x.body}</div>`}</div></div>`).join('')}</div>`;
    h += `<div class="glbl rk22t">EVERY WEEK</div><div class="rk22list">
  <div class="rk22i"><span class="rk22ic">👹</span><div class="rk22b"><div class="rk22n"><b>Beat the boss raid</b><em>+300</em></div><div class="sub">${r?(r.beaten?'Beaten this week ✓':`${esc(r.name)} · ${kfmt(Math.max(0,r.hp-r.dmg))} HP left. Every pound you lift hits it.`):'A new boss shows up every Monday'}</div></div></div>
  <div class="rk22i"><span class="rk22ic">👯</span><div class="rk22b"><div class="rk22n"><b>Lift together</b><em>+25 each</em></div><div class="sub">Start a group workout with 2+ of you</div></div></div>
  <div class="rk22i"><span class="rk22ic">🏋️</span><div class="rk22b"><div class="rk22n"><b>Every workout</b><em>+10</em></div><div class="sub">Anyone in the crew, signed in and synced</div></div></div>
  <div class="rk22i"><span class="rk22ic">📣</span><div class="rk22b"><div class="rk22n"><b>Post a workout</b><em>+5</em></div><div class="sub">Share it to the crew feed</div></div></div></div>
  <p class="sub" style="text-align:center;margin-top:10px">Higher crew ranks unlock crew banners and a fancier crest.</p>`
  } else {
    const owned = x => {
      const f = FIN[x];
      return f ? finOwned(x, E) : !plateLocked(x)
    };
    h += `<p class="sub" style="margin:0 2px 10px">Plates you unlock by ranking up. Lit ones are yours.</p><div class="rk22lad">`;
    for (let i = 1; i < RANKS.length; i++) {
      const pl = [...Object.keys(PLATE_LOCK).filter(k => PLATE_LOCK[k] === i), ...Object.keys(FIN).filter(k => /^r_/.test(k) && FIN[k][2] === RANKS[i][0] + ' rank')];
      const u = UNLOCKS.find(x => x[0] === i), got = E.rank >= i;
      h += `<div class="rk22r${got?' got':''}${E.rank+1===i?' next':''}"><div class="rk22re">${emblem(i)}</div><div class="rk22rb"><div class="rk22n"><b>${RANKS[i][0]}</b>${got?'<em>Unlocked ✓</em>':E.rank+1===i?'<em>Next</em>':''}</div>${u?`<div class="sub">${esc(u[1])}</div>`:''}<div class="rk22pl">${pl.map(x=>`<button class="rk22p2${owned(x)?'':' lk'}" data-finfo="${x}" aria-label="${esc(FIN[x]?FIN[x][0]:x)}">${plateSVG(x,'done')}</button>`).join('')}</div></div></div>`
    }
    h += `</div>`
  }
  return h
}function openRk22(tab) {
  st.sheet = {type: 'rk22', tab};
  renderSheet()
}
async function sheetClick22(b) {
  const sh = st.sheet, d = b.dataset;
  if (!sh || sh.type !== 'rk22') return false;
  if (d.rk22) {
    sh.tab = d.rk22;
    renderSheet();
    return true
  }
  if (b.id === 'rk22join') {
    st.sheet = null;
    renderSheet();
    st.profile = false;
    st.settings = false;
    st.tab = 'crew';
    render();
    return true
  }
  if (d.finfo) {
    const f = FIN[d.finfo];
    if (f) {
      toast(`${f[0]} · ${finOwned(d.finfo,engine())?'Unlocked':f[2]}`);
      return true
    }
    const l = plateLocked(d.finfo);
    toast(l ? 'Unlocks at ' + l : 'Unlocked');
    return true
  }
  return false
}
async function click22(b) {if (b.id === 'crewrankbtn' || b.id === 'nr22') {openRk22('crew'); return true}return false}/* the little emblem in the top right */
{
  const _rc = renderChrome;
  renderChrome = function() {
    _rc.apply(this, arguments);
    try {
      const rb = $('rankbtn');
      if (rb) {
        rb.classList.toggle('near22', meNear22() || crewNear22());
        rb.setAttribute('aria-label', 'Rank up: your rank, crew rank and plate unlocks')
      }
    } catch (e) {}
  }
}
function boot22() {
  const rb = $('rankbtn');
  if (!rb) return;
  const nb = rb.cloneNode(true);
  nb.style.display = '';
  rb.replaceWith(nb);
  nb.addEventListener('click', () => {
    haptic && haptic('tap');
    openRk22(meNear22() || !crewNear22() ? 'you' : 'crew')
  });
  try {renderChrome()} catch (e) {}
}
