/* ========================================================================
   Overload: Core

   Constants, workout templates, app state, storage and small helpers.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

const BETA = /\/overload-beta\//i.test(location.pathname);
if (BETA) {
  document.title = 'Overload Beta';
  const mt = document.querySelector('meta[name="apple-mobile-web-app-title"]') || document.head.appendChild(Object.assign(document.createElement('meta'), {name: 'apple-mobile-web-app-title'}));
  mt.content = 'Overload β'
}
if (BETA) {
  try {
    const P = 'ovb:', S = Storage.prototype, g = S.getItem, sv = S.setItem, rm = S.removeItem;
    if (!g.call(localStorage, P + 'init')) {
      ['liftlog', 'overload-plan', 'overload-notes', 'overload-settings', 'overload-flags'].forEach(k => {
        const v = g.call(localStorage, k);
        if (v !== null) sv.call(localStorage, P + k, v)
      });
      sv.call(localStorage, P + 'init', '1')
    }
    S.getItem = function(k) {return g.call(this, P + k)};
    S.setItem = function(k, v) {return sv.call(this, P + k, v)};
    S.removeItem = function(k) {return rm.call(this, P + k)}
  } catch (e) {}
}
const RAW0 = {};
try {
  ['liftlog', 'overload-plan', 'overload-notes', 'overload-settings', 'overload-flags'].forEach(k => {RAW0[k] = localStorage.getItem(k)})
} catch (e) {}
const DAYS = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];
const DN = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const MUSCLES = ['Chest', 'Back', 'Shoulders', 'Side delts', 'Rear delts', 'Biceps', 'Triceps', 'Forearms', 'Traps', 'Quads', 'Hamstrings', 'Glutes', 'Calves', 'Abs', 'Lower back', 'Other'];
const PC = {
  ember: ['#ff5a1f', '#ffb36b', '#8a2100', 'linear-gradient(135deg,#ff2d2d 0%,#ff6a00 50%,#ffc14d 100%)'], ice: ['#5ec8ff', '#d4f1ff', '#1a6c99', 'linear-gradient(135deg,#8be0ff 0%,#5ec8ff 45%,#e9f8ff 100%)'], toxic: ['#7ddc1f', '#d2ff8a', '#336600', 'linear-gradient(135deg,#3ddc84 0%,#a8ff1a 60%,#f0ff6b 100%)'], galaxy: ['#5b2bd6', '#b892ff', '#21105c', 'linear-gradient(135deg,#1b0f4a 0%,#5b2bd6 50%,#ff4fd8 100%)'], red: ['#ef2b36', '#ff6b73', '#9c0f18', 'linear-gradient(135deg,#ff3d4a 0%,#ef2b36 45%,#ff7a3d 100%)'], blue: ['#2563ff', '#6b95ff', '#0f2f9e', 'linear-gradient(135deg,#2563ff 0%,#3b82f6 50%,#00b7ff 100%)'], yellow: ['#ffbe0b', '#ffd75e', '#b07a00', 'linear-gradient(135deg,#ff9f1c 0%,#ffbe0b 55%,#ffd23f 100%)'], green: ['#16b65b', '#4fdc8c', '#0a6e35', 'linear-gradient(135deg,#0fa958 0%,#16b65b 50%,#00c2a8 100%)'], purple: ['#7c4dff', '#a98bff', '#4520b8', 'linear-gradient(135deg,#6a3dff 0%,#7c4dff 50%,#d14dff 100%)'], black: ['#23252b', '#4a4d56', '#0c0d10', 'linear-gradient(135deg,#23252b 0%,#3a3d46 60%,#5a5e69 100%)'], pink: ['#ff3d8b', '#ff86b8', '#a3104f', 'linear-gradient(135deg,#ff3d8b 0%,#ff5fa2 50%,#ff9f6b 100%)'], teal: ['#00b3a4', '#4de0d3', '#00665d', 'linear-gradient(135deg,#00a896 0%,#00c2b2 50%,#3ad0ff 100%)'], gold: ['#d4a20b', '#ffe27a', '#8a6400', 'linear-gradient(135deg,#b8860b 0%,#e8b923 45%,#ffe27a 100%)'], chrome: ['#8c939e', '#f4f6f9', '#454a52', 'linear-gradient(135deg,#5d636d 0%,#aeb5bf 45%,#eef1f5 100%)']
};
const TYPES = {
  up: ['Upper', 'red'], leg: ['Lower', 'blue'], full: ['Full body', 'green'], arm: ['Arms', 'yellow'], other: ['Other', 'purple'], off: ['Rest', 'black']
};
const colorOf = d => d.color || (TYPES[d.type] || TYPES.other)[1];
const slug = s => String(s).toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'ex';
const E = (name, m, lo, hi, sets, id) => ({id: id || slug(name), name, m, lo, hi, sets: sets || 2, cue: ''});
const D = (name, type, focus, groups, rest) => ({name, type, focus, groups: groups || [], rest: rest || 90});
const LIB = [['Bench press', 'Chest', 6, 10], ['Incline bench press', 'Chest', 6, 10], ['Incline DB press', 'Chest', 8, 12], ['DB bench press', 'Chest', 8, 12], ['Machine chest press', 'Chest', 8, 12], ['Pec deck', 'Chest', 10, 15], ['Cable fly', 'Chest', 10, 15], ['Dips', 'Chest', 8, 12], ['Push ups', 'Chest', 10, 20], ['Lat pulldown', 'Back', 8, 12], ['Pull ups', 'Back', 6, 10], ['Chin ups', 'Back', 6, 10], ['Barbell row', 'Back', 6, 10], ['Seated cable row', 'Back', 8, 12], ['Chest supported row', 'Back', 8, 12], ['T bar row', 'Back', 8, 12], ['One arm DB row', 'Back', 8, 12], ['MTS row', 'Back', 8, 12], ['Deadlift', 'Back', 3, 6], ['Overhead press', 'Shoulders', 5, 8], ['DB shoulder press', 'Shoulders', 8, 12], ['Machine shoulder press', 'Shoulders', 8, 12], ['Arnold press', 'Shoulders', 8, 12], ['Lateral raise', 'Side delts', 12, 20], ['Cable lateral raise', 'Side delts', 12, 20], ['Upright row', 'Side delts', 10, 15], ['Reverse pec deck', 'Rear delts', 12, 20], ['Face pull', 'Rear delts', 12, 20], ['Rear delt fly', 'Rear delts', 12, 20], ['Barbell curl', 'Biceps', 8, 12], ['DB curl', 'Biceps', 10, 15], ['Hammer curl', 'Biceps', 10, 15], ['Preacher curl', 'Biceps', 10, 15], ['Incline DB curl', 'Biceps', 10, 15], ['Cable curl', 'Biceps', 10, 15], ['Tricep pushdown', 'Triceps', 10, 15], ['Overhead tricep extension', 'Triceps', 10, 15], ['Skull crushers', 'Triceps', 8, 12], ['JM press', 'Triceps', 8, 12], ['Close grip bench', 'Triceps', 6, 10], ['Wrist curl', 'Forearms', 12, 15], ['Reverse curl', 'Forearms', 10, 15], ['Shrugs', 'Traps', 10, 15], ['Squat', 'Quads', 5, 8], ['Front squat', 'Quads', 6, 10], ['Hack squat', 'Quads', 8, 12], ['Leg press', 'Quads', 10, 15], ['Leg extension', 'Quads', 12, 15], ['Bulgarian split squat', 'Quads', 8, 12], ['Walking lunge', 'Glutes', 10, 12], ['Romanian deadlift', 'Hamstrings', 8, 12], ['Stiff leg deadlift', 'Hamstrings', 8, 12], ['Lying leg curl', 'Hamstrings', 10, 15], ['Seated leg curl', 'Hamstrings', 10, 15], ['Hip thrust', 'Glutes', 8, 12], ['Cable kickback', 'Glutes', 12, 15], ['Standing calf raise', 'Calves', 10, 15], ['Seated calf raise', 'Calves', 12, 20], ['Cable crunch', 'Abs', 12, 20], ['Hanging leg raise', 'Abs', 10, 15], ['Ab wheel', 'Abs', 8, 15], ['Back extensions', 'Lower back', 10, 15]];
const J = {
  incline: E('Incline press', 'Chest', 6, 10, 2, 'incline'), mts: E('MTS row', 'Back', 8, 12, 2, 'mts'), pecdeck: E('Pec deck', 'Chest', 10, 15, 2, 'pecdeck'), pulldown: E('Lat pulldown', 'Back', 8, 12, 2, 'pulldown'), lateral: E('Lateral raise', 'Side delts', 12, 20, 2, 'lateral'), revpec: E('Reverse pec deck', 'Rear delts', 12, 20, 2, 'revpec'), preacher: E('Preacher curl', 'Biceps', 10, 15, 2, 'preacher'), pushdown: E('Tricep pushdown', 'Triceps', 10, 15, 2, 'pushdown'), shoulder: E('Shoulder press', 'Shoulders', 6, 10, 2, 'shoulder'), closerow: E('Close grip row', 'Back', 8, 12, 2, 'closerow'), widerow: E('Wide grip row', 'Back', 10, 12, 2, 'widerow'), backext: E('Back extensions', 'Lower back', 10, 15, 2, 'backext'), jm: E('JM press', 'Triceps', 8, 12, 2, 'jm'), dbcurl: E('DB curl', 'Biceps', 10, 15, 2, 'dbcurl'), revcurl: E('Reverse curl', 'Forearms', 10, 15, 2, 'revcurl'), fcurl: E('Forearm curl', 'Forearms', 12, 15, 2, 'fcurl'), shrugs: E('Shrugs', 'Traps', 10, 15, 2, 'shrugs'), fcurl2: E('Forearm curl 2', 'Forearms', 12, 15, 2, 'fcurl2')
};
const g = (...ids) => ids.map(i => ({...J[i]}));
const S1 = (...a) => a.map(x => [x]);
const push = () => D('Push', 'up', 'Chest, shoulders, triceps', S1(E('Bench press', 'Chest', 6, 10, 3), E('Incline DB press', 'Chest', 8, 12), E('DB shoulder press', 'Shoulders', 8, 12), E('Lateral raise', 'Side delts', 12, 20, 3), E('Tricep pushdown', 'Triceps', 10, 15), E('Overhead tricep extension', 'Triceps', 10, 15)), 120);
const pull = () => Object.assign(D('Pull', 'up', 'Back, rear delts, biceps', S1(E('Lat pulldown', 'Back', 8, 12, 3), E('Seated cable row', 'Back', 8, 12, 3), E('Face pull', 'Rear delts', 12, 20), E('Barbell curl', 'Biceps', 8, 12), E('Hammer curl', 'Biceps', 10, 15)), 120), {color: 'yellow'});
const legs = () => D('Legs', 'leg', 'Quads, hamstrings, glutes, calves', S1(E('Squat', 'Quads', 5, 8, 3), E('Romanian deadlift', 'Hamstrings', 8, 12, 3), E('Leg press', 'Quads', 10, 15), E('Lying leg curl', 'Hamstrings', 10, 15), E('Standing calf raise', 'Calves', 10, 15, 3)), 150);
const up = () => D('Upper', 'up', 'Chest, back, shoulders, arms', [[E('Bench press', 'Chest', 6, 10, 3), E('Barbell row', 'Back', 6, 10, 3)], [E('DB shoulder press', 'Shoulders', 8, 12), E('Lat pulldown', 'Back', 8, 12)], [E('Barbell curl', 'Biceps', 10, 15), E('Tricep pushdown', 'Triceps', 10, 15)]], 90);
const lo = () => D('Lower', 'leg', 'Quads, hamstrings, glutes, abs', S1(E('Squat', 'Quads', 5, 8, 3), E('Romanian deadlift', 'Hamstrings', 8, 12, 3), E('Walking lunge', 'Glutes', 10, 12), E('Lying leg curl', 'Hamstrings', 10, 15), E('Cable crunch', 'Abs', 12, 20)), 150);
const fb = () => D('Full body', 'full', 'Everything, 3 times a week', [[E('Squat', 'Quads', 5, 8, 3)], [E('Bench press', 'Chest', 6, 10, 3), E('Barbell row', 'Back', 6, 10, 3)], [E('Romanian deadlift', 'Hamstrings', 8, 12)], [E('Lateral raise', 'Side delts', 12, 20), E('Barbell curl', 'Biceps', 10, 15)]], 120);
const R = () => D('Rest', 'off', 'Recovery day');
const TEMPLATES = {
  jake: {
    label: "Jake's split", desc: 'Upper body 3x a week with supersets, 2 leg days, an arm day. 6 days.', c: 'red', days: () => [D('Upper A', 'up', 'Chest, back, delts, arms', [g('incline', 'mts'), g('pecdeck', 'pulldown'), g('lateral', 'revpec'), g('preacher', 'pushdown')]), D('Legs 1', 'leg', 'Hack squat focus', [], 150), D('Upper B', 'up', 'Chest, back, delts, lower back', [g('shoulder', 'closerow'), g('pecdeck', 'pulldown'), g('lateral', 'widerow'), g('backext')]), D('Arms', 'arm', 'Biceps, triceps, forearms, traps', [g('jm', 'dbcurl'), g('revcurl', 'fcurl'), g('shrugs', 'fcurl2')], 75), D('Legs 2', 'leg', 'SLDL focus', [], 150), D('Upper C', 'up', 'Chest, back, delts, arms', [g('incline', 'widerow'), g('pecdeck', 'mts'), g('lateral', 'revpec'), g('preacher', 'pushdown')]), R()]
  },
  ppl: {
    label: 'Push pull legs', desc: 'The classic 6 day split. Every muscle twice a week.', c: 'yellow', days: () => [push(), pull(), legs(), push(), pull(), legs(), R()]
  },
  ul: {
    label: 'Upper lower', desc: '4 days a week. Simple, strong, easy to recover from.', c: 'blue', days: () => [up(), lo(), R(), up(), lo(), R(), R()]
  },
  fb: {
    label: 'Full body', desc: '3 days a week. Big lifts, in and out.', c: 'green', days: () => [fb(), R(), fb(), R(), fb(), R(), R()]
  },
  blank: {
    label: 'Build my own', desc: 'Seven empty days. Pick from the library or type your own.', c: 'purple', days: () => DAYS.map((_, i) => D(['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'][i], 'other', ''))
  }
};
const clone = o => JSON.parse(JSON.stringify(o));
const iso = d => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
const toD = s => new Date(s + 'T12:00:00');
const dayIdx = s => (toD(s).getDay() + 6) % 7;
const md = s => `${toD(s).getMonth()+1}/${toD(s).getDate()}`;
const MON = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
const load = k => {try {const r = localStorage.getItem(k); return r ? JSON.parse(r) : null} catch (e) {return null}};
const store = (k, v) => {
  DREV++;
  let ok = true;
  try {localStorage.setItem(k, JSON.stringify(v))} catch (e) {ok = false}if (/^(liftlog|overload-)/.test(k) && typeof queueMirror === 'function') queueMirror();
  if (/^(liftlog|overload-(plan|notes|settings|flags))$/.test(k) && typeof queueSync === 'function') queueSync();
  return ok
};
const today = () => iso(new Date());
const st = {
  sessions: load('liftlog') || {}, plan: load('overload-plan'), notes: load('overload-notes') || [], set: Object.assign({
    unit: 'lb', sound: true, vibe: true, autoRest: true, awake: false, rir: false, splash: true, theme: 'auto'
  }, load('overload-settings') || {}), date: today(), auto: true, tab: 'log', prog: null, pfilter: 'All', pday: 0, rest: null, confirm: null, picker: null, editNote: null, settings: false, obName: '', obPick: null, menu: null, warm: {}, sheet: null, hideSafari: false, profile: false, sub: {crew: 'feed', arena: 'all', stats: 'ov', plan: 'plan'}, cs: load('overload-crewcache') || null, liftSel: null, liftRel: false, flags: load('overload-flags') || {}, snaps: null, copySel: []
};
st.day = DAYS[dayIdx(st.date)];
st.pday = DAYS.indexOf(st.day);
if (st.plan) st.plan.days.forEach(d => {
  d.groups = d.groups || [];
  if (!d.rest) d.rest = d.type === 'leg' ? 150 : 90;
  d.groups.forEach(gr => gr.forEach(e => {if (e.cue === undefined) e.cue = ''}))
});
const U = () => st.set.unit || 'lb';
const $ = id => document.getElementById(id);
let toastT = null;
const toast = t => {
  const el = $('toast');
  el.textContent = t;
  el.classList.add('on');
  clearTimeout(toastT);
  toastT = setTimeout(() => el.classList.remove('on'), 1900)
};
const sid = () => st.date + '_' + st.day;
const pd = () => st.plan.days[DAYS.indexOf(st.day)];
const cur = () => st.sessions[sid()] || {date: st.date, day: st.day, ex: {}, names: {}};
const has = a => Array.isArray(a) && a.some(x => x && (+x.w || +x.r));
const full = x => x && +x.w > 0 && +x.r > 0;
const e1 = x => (+x.w || 0) * (1 + (+x.r || 0) / 30);
const fmt = a => (a || []).filter(full).map(x => `${+x.w}×${+x.r}`).join(', ');
const kfmt = n => n >= 10000 ? (n / 1000).toFixed(1) + 'k' : Math.round(n).toLocaleString();
const real = s => s.day !== 'base';
function exMap() {
  const m = {};
  if (st.plan) st.plan.days.forEach(d => d.groups.forEach(gr => gr.forEach(e => {if (!m[e.id]) m[e.id] = e})));
  return m
}
function exName(id) {
  const m = exMap();
  if (m[id]) return m[id].name;
  if (J[id]) return J[id].name;
  for (const s of Object.values(st.sessions)) if (s.names && s.names[id]) return s.names[id];
  return id
}
function exMuscle(id) {
  const m = exMap();
  if (m[id]) return m[id].m;
  if (J[id]) return J[id].m;
  const l = LIB.find(x => slug(x[0]) === id);
  return l ? l[1] : 'Other'
}
const skey = s => s.date + (s.day === 'base' ? '0' : '1');
const sorted = () => Object.entries(st.sessions).sort((a, b) => skey(a[1]) < skey(b[1]) ? 1 : skey(a[1]) > skey(b[1]) ? -1 : 0);
function last(id) {
  for (const [k, s] of sorted()) {
    if (k === sid() || s.date > st.date) continue;
    if (s.ex && has(s.ex[id])) return s
  }
  return null
}
const better = (a, b) => full(a) && b && ((+a.w > +b.w) || (+a.w === +b.w && +a.r > +b.r));
function bestBefore(id, date) {
  let b = 0;
  Object.values(st.sessions).forEach(s => {
    if ((s.date < date || (s.date === date && s.day === 'base')) && s.ex && s.ex[id]) s.ex[id].forEach(x => {if (full(x)) b = Math.max(b, e1(x))})
  });
  return b
}
function weekDates(base) {
  const d = toD(base || st.date);
  d.setDate(d.getDate() - dayIdx(iso(d)));
  return DAYS.map((_, i) => {
    const x = new Date(d);
    x.setDate(d.getDate() + i);
    return iso(x)
  })
}
const doneOn = (date, k) => {
  const s = st.sessions[date + '_' + k];
  return !!(s && Object.values(s.ex || {}).some(has))
};
const mins = ms => {
  ms = Math.max(0, ms);
  const m = Math.floor(ms / 60000), s = Math.floor(ms % 60000 / 1000);
  return m >= 60 ? `${Math.floor(m/60)}h ${m%60}m` : `${m}:${String(s).padStart(2,'0')}`
};
function elapsed(s) {
  if (!s.start) return 0;
  const end = s.end || (s.pauseAt || Date.now());
  return end - s.start - (s.paused || 0)
}
function sessStats(s) {
  let sets = 0, vol = 0, prs = [];
  Object.entries(s.ex || {}).forEach(([id, a]) => {
    const pb = bestBefore(id, s.date);
    let hit = false;
    a.forEach(x => {if (full(x)) {sets++; vol += x.w * x.r; if (pb && e1(x) > pb) hit = true}});
    if (hit) prs.push(id)
  });
  return {sets, vol, prs}
}

const DAYMS = 864e5;
