/* ========================================================================
   Overload: Sync, backup and data safety

   Backups, trash, cloud sync, export, the update safety net, push alerts, owner stats.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

/* ---------- 2.0: backup files ---------- */
function backupData() {
  return JSON.stringify({
    app: 'overload', v: VERSION, sessions: st.sessions, plan: st.plan, notes: st.notes, settings: st.set
  })
}
async function saveBackupFile() {
  const name = `overload-backup-${today()}.json`, file = new File([backupData()], name, {type: 'application/json'});
  st.set.lastBackup = Date.now();
  saveSet();
  try {
    if (navigator.canShare && navigator.canShare({files: [file]})) {
      await navigator.share({files: [file], title: 'Overload backup'});
      toast('Backup ready. Save it to Files or iCloud');
      return
    }
  } catch (x) {if (x && x.name === 'AbortError') {toast('Backup not saved'); return}}
  try {
    const url = URL.createObjectURL(file), a = document.createElement('a');
    a.href = url;
    a.download = name;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 4000);
    toast('Backup file saved to Downloads')
  } catch (x) {
    try {
      await navigator.clipboard.writeText(backupData());
      toast('Backup copied. Paste it into Notes')
    } catch (y) {toast('Use Copy and paste in Settings')}
  }
}
function restoreFrom(text) {
  const o = JSON.parse(text);
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
  applyTheme()
}
/* ---------- 0.10: data safety ---------- */
const IDB = {
  db: null, open() {
    return new Promise(r => {
      if (this.db) return r(this.db);
      try {
        const q = indexedDB.open(BETA ? 'overload-beta' : 'overload', 1);
        q.onupgradeneeded = () => {
          const d = q.result;
          if (!d.objectStoreNames.contains('kv')) d.createObjectStore('kv')
        };
        q.onsuccess = () => {
          this.db = q.result;
          r(this.db)
        };
        q.onerror = () => r(null)
      } catch (e) {r(null)}
    })
  },
  async put(k, v) {
    const d = await this.open();
    if (!d) return;
    try {
      const t = d.transaction('kv', 'readwrite');
      v === null ? t.objectStore('kv').delete(k) : t.objectStore('kv').put(v, k)
    } catch (e) {}
  },
  async get(k) {
    const d = await this.open();
    if (!d) return null;
    return new Promise(r => {
      try {
        const q = d.transaction('kv').objectStore('kv').get(k);
        q.onsuccess = () => r(q.result || null);
        q.onerror = () => r(null)
      } catch (e) {r(null)}
    })
  }
};
let mirT = null;
function queueMirror() {
  clearTimeout(mirT);
  mirT = setTimeout(() => {
    mirT = null;
    mirrorNow()
  }, 900)
}
let missGuard = true;
async function mirrorNow() {
  if (missGuard) return;
  if (!st.plan && !Object.keys(st.sessions).length) return;
  const data = backupData();
  await IDB.put('main', {ts: Date.now(), data});
  const d = today();
  await IDB.put('snap:' + d, {ts: Date.now(), data});
  let idx = (await IDB.get('snaps')) || [];
  if (!idx.includes(d)) idx.push(d);
  idx.sort();
  while (idx.length > 7) {await IDB.put('snap:' + idx.shift(), null)}await IDB.put('snaps', idx)
}
function flushAll() {
  if (pendS) {
    clearTimeout(svT);
    pendS = false;
    store('liftlog', st.sessions)
  }
  if (pendP && st.plan) {
    clearTimeout(plT);
    pendP = false;
    store('overload-plan', st.plan)
  }
  if (mirT) {
    clearTimeout(mirT);
    mirT = null;
    mirrorNow()
  }
}
async function checkMissing() {
  const m = await IDB.get('main');
  if (!m || !m.data) {
    missGuard = false;
    return
  }
  let o;
  try {o = JSON.parse(m.data)} catch (e) {
    missGuard = false;
    return
  }
  const theirs = Object.keys(o.sessions || {}).length, mine = Object.keys(st.sessions).length;
  if ((!st.plan && o.plan) || theirs - mine >= 2) {
    st.sheet = {type: 'missing', ts: m.ts, data: m.data, theirs, mine};
    renderSheet()
  } else missGuard = false
}
function restoreReplace(text) {
  missGuard = false;
  const o = JSON.parse(text);
  if (!o || !o.sessions) throw 0;
  st.sessions = o.sessions;
  if (o.plan && o.plan.days) st.plan = o.plan;
  if (Array.isArray(o.notes)) st.notes = o.notes;
  if (o.settings) Object.assign(st.set, o.settings);
  store('liftlog', st.sessions);
  store('overload-plan', st.plan);
  saveN();
  saveSet();
  applyTheme();
  dirtyEng()
}
function notesHistoryHTML(id) {
  const list = Object.values(st.sessions).filter(s => s.exNotes && s.exNotes[id] && s.exNotes[id].trim()).sort((a, b) => a.date < b.date ? 1 : -1);
  return list.length ? list.map(s => `<div class="tl"><span class="sub">${DN[dayIdx(s.date)]} ${MON[toD(s.date).getMonth()]} ${toD(s.date).getDate()}</span><p>${esc(s.exNotes[id])}</p></div>`).join('') : '<p class="sub">No notes for this exercise yet. Type one in the note box under your sets and it shows up here.</p>'
}
function browserKind() {
  const u = navigator.userAgent;
  if (/FxiOS|Firefox/i.test(u)) return 'firefox';
  if (/SamsungBrowser/i.test(u)) return 'samsung';
  if (/CriOS|EdgiOS|OPiOS|GSA\//i.test(u)) return 'iosother';
  if (/OPR|Opera|Edg\//i.test(u)) return 'other';
  return 'ok'
}
/* ---------- 0.10: visitor counter ---------- */
function loadCounter() {/* 0.15: GoatCounter removed. Usage stats now come from Overload's own server (owner dashboard). */
}
/* ---------- 0.11: lock-screen rest alerts ---------- */
const PUSH_URL = 'https://overload-push.jscarp961.workers.dev', VAPID = 'BOsm6FMRlijSrefkibBYMTatg6uZXCWxs58ncZD5EeCuhX84uhb3J_AAyXl_IBKjAMR6636JjZjbnHv1zE2tfzM';
const pushOK = () => 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;
const b64uBytes = x => Uint8Array.from(atob(x.replace(/-/g, '+').replace(/_/g, '/') + '='.repeat((4 - x.length % 4) % 4)), c => c.charCodeAt(0));
const pushPost = (path, body) => fetch(PUSH_URL + path, {
  method: 'POST', keepalive: true, headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)
}).then(r => r.json()).catch (() => null);
let pushPending = false;
async function enablePush() {
  if (/iPhone|iPad/i.test(navigator.userAgent) && !isStandalone()) {
    toast('Open Overload from your home screen icon to turn this on');
    return false
  }
  if (!pushOK()) {
    toast('This browser can\u2019t do lock-screen alerts');
    return false
  }
  const perm = await Notification.requestPermission();
  if (perm !== 'granted') {
    toast('Notifications are blocked. Allow them in your phone settings');
    return false
  }
  try {
    const reg = await navigator.serviceWorker.ready;
    const sub = await reg.pushManager.getSubscription() || await reg.pushManager.subscribe({userVisibleOnly: true, applicationServerKey: b64uBytes(VAPID)});
    st.set.push = sub.endpoint;
    saveSet();
    await IDB.put('pushmsg', {
      at: Date.now() + 1500, title: 'Rest alerts are on', body: 'You\u2019ll get a ping here when rest is over 💪'
    });
    const r = await pushPost('/test', {endpoint: sub.endpoint});
    toast(r && r.sent >= 200 && r.sent < 300 ? 'Rest alerts on. Test ping sent' : 'Turned on, but the test ping didn\u2019t go through');
    return true
  } catch (e) {
    toast('Could not turn on alerts');
    return false
  }
}
async function disablePush() {
  try {
    const reg = await navigator.serviceWorker.ready;
    const sub = await reg.pushManager.getSubscription();
    if (sub) {
      pushPost('/cancel', {endpoint: sub.endpoint});
      await sub.unsubscribe()
    }
  } catch (e) {}st.set.push = null;
  saveSet()
}
function restMsg() {
  if (!st.rest) return;
  IDB.put('pushmsg', {
    at: st.rest.end, title: 'Rest is over ⏱️', body: st.rest.next ? `Next up: ${st.rest.next}` : 'Time for your next set 💪'
  })
}
/* ---------- 0.13: cloud sync ---------- */
const SYNC_SKIP = ['push', 'crewPings', 'acct', 'hideSafari', 'seenVer', 'schema', 'lastBackup', 'bellSeen'];
const stripS = o => {
  if (!o) return o;
  const c = {...o};
  delete c._t;
  delete c._xt;
  delete c._v;
  return c
};
const syncSet = () => {
  const o = {};
  Object.keys(st.set).forEach(k => {if (!SYNC_SKIP.includes(k)) o[k] = st.set[k]});
  return o
};
let syncT = null, syncing = false, syncAgain = false, syncPoll = null;
st.syncState = {state: 'off'};
function queueSync(ms) {
  if (!st.set || !st.set.acct) return;
  clearTimeout(syncT);
  syncT = setTimeout(() => syncNow(), ms == null ? 3500 : ms)
}
async function vaultCall(p, body) {
  const r = await fetch(PUSH_URL + '/vault' + p, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body || {})});
  let j = {};
  try {j = await r.json()} catch (e) {}if (!r.ok) throw Object.assign(new Error(j.error || ('HTTP ' + r.status)), {status: r.status});
  return j
}
const acctBody = x => ({acct: st.set.acct.id, token: st.set.acct.token, ...x});
function setSync(state, extra) {
  st.syncState = {...st.syncState, state, ...extra};
  const d = $('syncdot');
  if (d) {
    d.className = 'sdot s-' + state;
    d.title = {ok: 'Synced', pending: 'Syncing', err: 'Sync failed, will retry', held: 'Sync paused', off: ''}[state] || ''
  }
}
async function syncNow(force) {
  const a = st.set.acct;
  if (!a || !st.plan && !Object.keys(st.sessions).length && !force) return;
  if (syncing) {
    syncAgain = true;
    return
  }
  syncing = true;
  setSync('pending');
  try {
    const base = (await IDB.get('syncbase')) || {seq: 0, sessions: {}, first: true};
    const now = Date.now();
    if (base.first) {
      const r = await vaultCall('/sync', acctBody({since: 0, today: today(), changes: {}}));
      applyRemote(r.changes, base, true);
      base.seq = r.seq;
      base.first = false;
      await IDB.put('syncbase', base)
    }
    const ch = {sessions: {}};
    let dels = 0, live = 0;
    Object.entries(st.sessions).forEach(([k, s]) => {
      live++;
      const bj = base.sessions[k], cur = JSON.stringify(stripS(s));
      let bo = null;
      try {bo = bj ? JSON.parse(bj) : null} catch (e) {}
      if (bo && !bo._del && JSON.stringify(stripS(bo)) === cur) return;
      s._t = now;
      s._xt = s._xt || {};
      Object.keys(s.ex || {}).forEach(id => {
        if (!bo || bo._del || JSON.stringify((bo.ex || {})[id]) !== JSON.stringify(s.ex[id])) s._xt[id] = now
      });
      ch.sessions[k] = s
    });
    Object.keys(base.sessions).forEach(k => {
      if (!st.sessions[k]) {
        let bo = null;
        try {bo = JSON.parse(base.sessions[k])} catch (e) {}if (bo && !bo._del) {
          ch.sessions[k] = {_del: 1, _t: now};
          dels++
        }
      }
    });
    {
      const ks = Object.keys(ch.sessions);
      if (ks.length > 300) {
        ks.slice(300).forEach(k => delete ch.sessions[k]);
        syncAgain = true
      }
    }
    if (dels > 5 && dels > (live + dels) * .2 && !st.allowDel) {
      setSync('held');
      syncing = false;
      if (!st.sheet) {
        st.sheet = {type: 'syncguard', n: dels};
        renderSheet()
      }
      return
    }
    st.allowDel = false;
    const cmp = (kind, val, key) => {
      const j = JSON.stringify(val);
      if (j !== base[key]) {
        ch[kind] = {...JSON.parse(j), _t: now};
        return j
      }
      return null
    };
    const pj = st.plan ? cmp('plan', st.plan, 'plan') : null, nj = cmp('notes', {list: st.notes}, 'notes'), sj = cmp('settings', syncSet(), 'settings'), fj = cmp('flags', {d: st.flags}, 'flags');
    const sentS = {};
    Object.entries(ch.sessions).forEach(([k, v]) => sentS[k] = JSON.stringify(v));
    const r = await vaultCall('/sync', acctBody({since: base.seq || 0, today: today(), changes: ch}));
    Object.entries(sentS).forEach(([k, j]) => {base.sessions[k] = j});
    if (pj !== null) base.plan = pj;
    if (nj !== null) base.notes = nj;
    if (sj !== null) base.settings = sj;
    if (fj !== null) base.flags = fj;
    const sentNow = {};
    Object.keys(sentS).forEach(k => sentNow[k] = st.sessions[k] ? JSON.stringify(st.sessions[k]) : null);
    applyRemote(r.changes, base, false, sentS, sentNow);
    base.seq = r.seq;
    await IDB.put('syncbase', base);
    setSync('ok', {at: Date.now()});
  } catch (e) {
    setSync(e.status === 403 ? 'err' : 'err', {err: e.message});
    if (e.status === 403) {
      toast('Signed out of sync on this device');
      st.set.acct = null;
      saveSet()
    }
  }
  syncing = false;
  if (syncAgain) {
    syncAgain = false;
    queueSync(800)
  }
}
function applyRemote(c, base, first, sentS, sentNow) {
  if (!c) return;
  let changed = false;
  Object.entries(c.sessions || {}).forEach(([k, v]) => {
    const local = st.sessions[k];
    if (sentS && sentS[k] && sentNow[k] !== null && JSON.stringify(local) !== sentNow[k]) return; // edited again since sending, push next round
    if (first && local && local._t && !v._del) return;
    if (v._del) {
      if (local) {
        if (Object.values(local.ex || {}).some(a => (a || []).some(full))) trashPutSync('workout', `Workout on ${local.date} (deleted on another device)`, {key: k, sess: local});
        delete st.sessions[k];
        changed = true
      }
      base.sessions[k] = JSON.stringify(v);
      return
    }
    if (JSON.stringify(local) !== JSON.stringify(v)) {
      st.sessions[k] = v;
      changed = true
    }
    base.sessions[k] = JSON.stringify(v)
  });
  const take = (kind, key, apply) => {
    const v = c[kind];
    if (!v) return;
    const plain = stripS(v);
    const j = JSON.stringify(plain);
    if (first || (v._t || 0) >= (base[key + 'T'] || 0)) {
      apply(plain);
      base[key] = j;
      base[key + 'T'] = v._t || 0;
      changed = true
    }
  };
  take('plan', 'plan', v => {if (v.days) st.plan = v});
  take('notes', 'notes', v => {if (Array.isArray(v.list)) st.notes = v.list});
  take('settings', 'settings', v => {
    const keep = {};
    SYNC_SKIP.forEach(k => {if (k in st.set) keep[k] = st.set[k]});
    st.set = {...st.set, ...v, ...keep}
  });
  take('flags', 'flags', v => {if (v.d) st.flags = v.d});
  if (changed) {
    try {
      localStorage.setItem('liftlog', JSON.stringify(st.sessions));
      if (st.plan) localStorage.setItem('overload-plan', JSON.stringify(st.plan));
      localStorage.setItem('overload-notes', JSON.stringify(st.notes));
      localStorage.setItem('overload-settings', JSON.stringify(st.set));
      localStorage.setItem('overload-flags', JSON.stringify(st.flags))
    } catch (e) {}
    dirtyEng();
    applyTheme();
    if (!document.querySelector('#main input:focus,#main textarea:focus')) render()
  }
}
async function signedIn(acct, token, isNew) {
  st.set.acct = {id: acct, token};
  saveSet();
  await IDB.put('syncbase', {seq: 0, sessions: {}, first: !isNew});
  setSync('pending');
  await syncNow(true);
  startSyncPoll();
  if (crewOn()) syncCrew(true)
}
function startSyncPoll() {
  clearInterval(syncPoll);
  if (!st.set.acct) return;
  syncPoll = setInterval(() => {if (!document.hidden) syncNow()}, 90000)
}
async function passkeyCreate() {
  if (!window.PublicKeyCredential || !navigator.credentials) {
    toast('Passkeys aren’t supported in this browser');
    return false
  }
  try {
    const {challenge} = await vaultCall('/challenge', {});
    const cred = await navigator.credentials.create({
      publicKey: {
        rp: {id: location.hostname, name: 'Overload'}, user: {
          id: new TextEncoder().encode(st.set.acct.id), name: st.set.name || 'Overload lifter', displayName: st.set.name || 'Overload lifter'
        }, challenge: b64uBytes(challenge), pubKeyCredParams: [{type: 'public-key', alg: -7}, {type: 'public-key', alg: -257}], authenticatorSelection: {residentKey: 'required', userVerification: 'preferred'}, timeout: 60000
      }
    });
    const r = cred.response;
    const pk = r.getPublicKey ? r.getPublicKey() : null;
    if (!pk) {
      toast('This device can’t make a passkey for Overload');
      return false
    }
    await vaultCall('/passkey/register', acctBody({
      id: cred.id, publicKey: bytesB64u(pk), alg: r.getPublicKeyAlgorithm ? r.getPublicKeyAlgorithm() : -7, label: navigator.platform || ''
    }));
    toast('Passkey added 🔑');
    return true
  } catch (e) {
    if (e && e.name !== 'NotAllowedError') toast('Passkey not added');
    return false
  }
}
async function passkeyLogin() {
  if (!window.PublicKeyCredential) {
    toast('Passkeys aren’t supported here. Use your recovery code');
    return
  }
  try {
    const {challenge} = await vaultCall('/challenge', {});
    const a = await navigator.credentials.get({
      publicKey: {
        challenge: b64uBytes(challenge), rpId: location.hostname, userVerification: 'preferred', timeout: 60000
      }
    });
    const r = a.response;
    const uh = bytesB64u(r.userHandle);
    const j = await vaultCall('/passkey/login', {
      id: a.id, userHandle: uh, clientDataJSON: bytesB64u(r.clientDataJSON), authenticatorData: bytesB64u(r.authenticatorData), signature: bytesB64u(r.signature)
    });
    const acct = new TextDecoder().decode(new Uint8Array(r.userHandle));
    st.sheet = null;
    renderSheet();
    toast('Signed in. Syncing your data…');
    await signedIn(acct, j.token, false);
    toast('All synced ✓')
  } catch (e) {
    if (e && e.name === 'NotAllowedError') return;
    toast(e.status === 404 ? 'No account found for that passkey' : 'Sign-in failed. Try your recovery code')
  }
}
const bytesB64u = buf => btoa(String.fromCharCode(...new Uint8Array(buf))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
function acctCard() {
  const a = st.set.acct;
  if (!a) return `<div class="card acct"><h3>☁️ Back up and sync</h3><p class="sub">Keep every workout safe in the cloud, use Overload on more than one device, and keep your crew spot if you change phones. Optional and free.</p><div class="exfoot"><button class="btn acc" id="acnew">Create account</button><button class="btn" id="acin">I have an account</button></div></div>`;
  const s = st.syncState || {};
  return `<div class="card acct"><h3>☁️ Account</h3><p class="sub">${s.state==='err'?'⚠️ Last sync failed. It will retry on its own':s.state==='pending'?'Syncing…':s.state==='held'?'Sync paused. Check the message about missing workouts':'Synced ✓'}${s.at?` · ${ago(s.at)} ago`:''}</p><div class="exfoot"><button class="btn sm" id="acsync">Sync now</button><button class="btn sm" id="acpk">🔑 Add passkey</button><button class="btn sm" id="acout">Sign out</button></div>${st.set.founder?'':'<button class="excl" id="founderlink" style="margin-top:8px">Founder code</button>'}</div>`
}
function acctSheet(sh) {
  if (sh.type === 'acin') return `<div class="ghd"><h2>Sign in</h2><button class="btn sm" id="sheetx">Cancel</button></div><button class="btn acc wide" id="pklogin" style="min-height:52px">🔑 Sign in with passkey</button><div class="glbl" style="margin:16px 0 6px">OR USE YOUR RECOVERY CODE</div><div class="row2"><input id="rcin" class="field" placeholder="ABCD-EFGH-JKLM-NPQR" autocapitalize="characters" autocomplete="one-time-code"><button class="btn" id="rcgo">Go</button></div><p class="sub" style="margin-top:12px">Workouts already on this phone get merged with your account. Nothing is deleted.</p>`;
  if (sh.type === 'accode') return `<div class="ghd"><h2>Save your recovery code</h2></div><p class="sub">This code is the master key to your account. Save it somewhere safe, like your notes app or a screenshot. We can't show it again.</p><div class="codebox"><b style="font-size:24px">${esc(sh.code)}</b><div class="exfoot" style="justify-content:center"><button class="btn sm" id="rccopy">Copy</button><button class="btn sm" id="rcshare">Share</button></div></div>${window.PublicKeyCredential?'<button class="btn acc wide" id="rcpk" style="min-height:50px">🔑 Add a passkey too (Face ID)</button>':''}<button class="btn wide" id="rcdone" style="min-height:50px;margin-top:8px">I saved it</button>`;
  if (sh.type === 'syncguard') return `<h2>Workouts missing</h2><p class="sub">${sh.n} workouts are gone from this phone but still safe in your account. Did you delete them on purpose?</p><div class="exfoot" style="margin-top:12px"><button class="btn acc" id="sgback" style="flex:1;min-height:52px">Bring them back</button><button class="btn" id="sgdel" style="flex:1;min-height:52px">Delete them</button></div>`;
  return null
}
async function acctClick(b) {
  const id = b.id;
  if (id === 'acnew') {
    b.disabled = true;
    try {
      const j = await vaultCall('/new', {name: st.set.name || ''});
      st.set.acct = {id: j.acct, token: j.token};
      saveSet();
      await IDB.put('syncbase', {seq: 0, sessions: {}, first: false});
      st.sheet = {type: 'accode', code: j.code};
      renderSheet();
      syncNow(true);
      startSyncPoll()
    } catch (e) {
      toast(e.message === 'Failed to fetch' ? 'You’re offline. Try again' : 'Could not create account')
    }
    b.disabled = false;
    return true
  }
  if (id === 'acin') {
    st.sheet = {type: 'acin'};
    renderSheet();
    return true
  }
  if (id === 'acsync') {
    await syncNow(true);
    toast(st.syncState.state === 'ok' ? 'Synced ✓' : 'Sync failed. Will retry');
    render();
    return true
  }
  if (id === 'acpk') {
    await passkeyCreate();
    return true
  }
  if (id === 'acout') {
    if (st.confirm !== 'acout') {
      st.confirm = 'acout';
      toast('Tap Sign out again. Your data stays on this phone and in the cloud');
      return true
    }
    st.confirm = null;
    try {await vaultCall('/signout', acctBody({}))} catch (e) {}st.set.acct = null;
    saveSet();
    await IDB.put('syncbase', null);
    clearInterval(syncPoll);
    setSync('off');
    render();
    toast('Signed out of sync');
    return true
  }
  if (id === 'acdel') {
    if (st.confirm !== 'acdel') {
      st.confirm = 'acdel';
      render();
      return true
    }
    st.confirm = null;
    try {
      await vaultCall('/delete', acctBody({}));
      st.set.acct = null;
      saveSet();
      await IDB.put('syncbase', null);
      clearInterval(syncPoll);
      setSync('off');
      toast('Cloud account deleted. Your data is still on this phone');
      render()
    } catch (e) {toast('Could not delete. Check your connection')}return true
  }
  return false
}
async function acctSheetClick(b) {
  const id = b.id;
  if (id === 'pklogin') {
    await passkeyLogin();
    return true
  }
  if (id === 'rcgo') {
    const code = ($('rcin').value || '').trim();
    if (code.replace(/[^A-Za-z0-9]/g, '').length < 16) {
      toast('Codes have 16 letters and numbers');
      return true
    }
    b.disabled = true;
    try {
      const j = await vaultCall('/recover', {code});
      st.sheet = null;
      renderSheet();
      toast('Signed in. Syncing your data…');
      await signedIn(j.acct, j.token, false);
      toast('All synced ✓')
    } catch (e) {
      toast(e.status === 404 ? 'That code doesn’t match an account' : 'Could not sign in. Check your connection')
    }
    b.disabled = false;
    return true
  }
  if (id === 'rccopy') {
    try {
      await navigator.clipboard.writeText(st.sheet.code);
      toast('Copied')
    } catch (e) {}return true
  }
  if (id === 'rcshare') {
    try {await navigator.share({text: `My Overload recovery code: ${st.sheet.code}`})} catch (e) {}return true
  }
  if (id === 'rcpk') {
    const ok = await passkeyCreate();
    if (ok) {
      st.sheet = null;
      renderSheet();
      render()
    }
    return true
  }
  if (id === 'rcdone') {
    st.sheet = null;
    renderSheet();
    render();
    toast('Backup and sync is on ☁️');
    return true
  }
  if (id === 'sgdel') {
    st.allowDel = true;
    st.sheet = null;
    renderSheet();
    syncNow(true);
    return true
  }
  if (id === 'sgback') {
    st.sheet = null;
    renderSheet();
    const base = (await IDB.get('syncbase')) || {};
    base.seq = 0;
    base.first = true;
    Object.keys(base.sessions || {}).forEach(k => {if (!st.sessions[k]) delete base.sessions[k]});
    try {
      const r = await vaultCall('/sync', acctBody({since: 0, today: today(), changes: {}}));
      Object.entries(r.changes.sessions || {}).forEach(([k, v]) => {if (!v._del && !st.sessions[k]) {st.sessions[k] = v; base.sessions[k] = JSON.stringify(v)}});
      base.seq = r.seq;
      base.first = false;
      await IDB.put('syncbase', base);
      store('liftlog', st.sessions);
      dirtyEng();
      render();
      toast('Your workouts are back ✓');
      setSync('ok', {at: Date.now()})
    } catch (e) {toast('Could not reach the cloud. Try again')}return true
  }
  return false
}
/* ---------- Trash: anything deleted on this phone waits here 30 days ---------- */
let TR14 = null;
async function trashLoad() {
  if (TR14) return TR14;
  TR14 = (await IDB.get('trash')) || [];
  try {
    const ls = JSON.parse(localStorage.getItem('overload-trash') || '[]');
    ls.forEach(x => {if (!TR14.some(y => y.id === x.id)) TR14.push(x)})
  } catch (e) {}return TR14
}
async function trashSave() {
  const now = Date.now();
  TR14 = (TR14 || []).filter(x => now - x.ts < 30 * DAYMS).slice(0, 400);
  await IDB.put('trash', TR14);
  try {localStorage.setItem('overload-trash', JSON.stringify(TR14.slice(0, 60)))} catch (e) {}
}
async function trashPut(kind, label, data) {
  await trashLoad();
  TR14.unshift({
    id: Date.now().toString(36) + Math.random().toString(36).slice(2, 6), kind, label, ts: Date.now(), data: JSON.parse(JSON.stringify(data))
  });
  await trashSave()
}
function trashPutSync(kind, label, data) {trashPut(kind, label, data).catch (() => {})}
async function trashRestore(id) {
  await trashLoad();
  const i = TR14.findIndex(x => x.id === id);
  if (i < 0) return false;
  const x = TR14[i], d = x.data;
  if (x.kind === 'exercise' || x.kind === 'group') {
    const day = st.plan.days[d.day];
    if (!day) return false;
    day.groups = day.groups || [];
    const g = x.kind === 'group' ? d.group : [d.ex];
    const have = new Set(day.groups.flat().map(e => e.id));
    const add = g.filter(e => !have.has(e.id));
    if (add.length) day.groups.splice(Math.min(d.gi || 0, day.groups.length), 0, add);
    saveP()
  } else if (x.kind === 'note') {
    if (!st.notes.some(n => n.id === d.id)) st.notes.push(d);
    saveN()
  } else if (x.kind === 'goal') {
    st.set.goals = st.set.goals || [];
    st.set.goals.push(d);
    saveSet()
  } else if (x.kind === 'plan') {
    await trashPut('plan', 'Plan before restoring', st.plan);
    st.plan = d;
    saveP()
  } else if (x.kind === 'set') {
    const s = st.sessions[d.key];
    if (!s) return false;
    s.ex[d.id] = s.ex[d.id] || [];
    const e = s.ex[d.id];
    const empty = e.findIndex(z => !full(z));
    if (empty >= 0) e[empty] = d.set;
    else e.push(d.set);
    saveS()
  } else if (x.kind === 'workout') {
    const cur0 = st.sessions[d.key];
    if (cur0 && Object.values(cur0.ex || {}).some(a => (a || []).some(full))) return false;
    d.sess._t = Date.now();
    st.sessions[d.key] = d.sess;
    saveS()
  } else if (x.kind === 'undo-restore') {restoreReplace(d.backup)}
  TR14.splice(i, 1);
  await trashSave();
  dirtyEng();
  return true
}
const TRASH_ICON = {
  exercise: '🏋️', group: '🏋️', note: '📝', goal: '🎯', plan: '📋', set: '✅', workout: '🗓', ['undo-restore']: '↩'
};
/* restoring an old copy never silently throws away what you have now */
const _restoreReplace = restoreReplace;
restoreReplace = function(text) {
  try {trashPutSync('undo-restore', 'Your data from before the restore', {backup: backupData()})} catch (e) {}return _restoreReplace(text)
};
/* ---------- full export ---------- */
async function exportAll() {
  const ls = {};
  try {
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i);
      if (/^(ovb:)?(liftlog|overload-)/.test(k)) ls[k] = localStorage.getItem(k)
    }
  } catch (e) {}
  const o = {
    app: 'overload', kind: 'full-export', v: VERSION, at: new Date().toISOString(), sessions: st.sessions, plan: st.plan, notes: st.notes, settings: st.set, flags: st.flags, trash: await trashLoad(), crew: st.cs ? {
      crew: st.cs.crew, members: st.cs.members.map(m => ({id: m.id, name: m.name, profile: m.profile})), posts: st.cs.posts
    } : null, chat: st.chat && st.chat.msgs || null, raw: ls
  };
  const txt = JSON.stringify(o), name = `overload-everything-${today()}.json`, file = new File([txt], name, {type: 'application/json'});
  try {
    if (navigator.canShare && navigator.canShare({files: [file]})) {
      await navigator.share({files: [file], title: 'Overload export'});
      st.set.lastBackup = Date.now();
      saveSet();
      toast('Exported everything');
      return
    }
  } catch (e) {if (e && e.name === 'AbortError') return}
  const a = document.createElement('a');
  a.href = URL.createObjectURL(file);
  a.download = name;
  document.body.appendChild(a);
  a.click();
  setTimeout(() => {
    URL.revokeObjectURL(a.href);
    a.remove()
  }, 4000);
  st.set.lastBackup = Date.now();
  saveSet();
  toast('Exported everything')
}
async function updateGuard() {
  try {
    const prevVer = st.set.seenVer || '', raw = RAW0;
    if (!raw.liftlog) return;
    if (prevVer !== VERSION && !(await IDB.get('preupd:' + VERSION))) {
      await IDB.put('preupd:' + VERSION, {at: Date.now(), from: prevVer, raw});
      const idx = (await IDB.get('preupdIdx')) || [];
      idx.push(VERSION);
      while (idx.length > 4) await IDB.put('preupd:' + idx.shift(), null);
      await IDB.put('preupdIdx', idx)
    }
    const snap = await IDB.get('preupd:' + VERSION);
    if (!snap) return;
    snap.boots = (snap.boots || 0) + 1;
    await IDB.put('preupd:' + VERSION, snap);
    if (snap.boots > 2 || Date.now() - snap.at > 30 * 60000) return;
    let old = {};
    try {old = JSON.parse(snap.raw.liftlog || '{}') || {}} catch (e) {return}
    let back = 0;
    Object.entries(old).forEach(([k, v]) => {
      if (!v || v._del) return;
      const has0 = Object.values(v.ex || {}).some(a => Array.isArray(a) && a.some(full));
      if (!has0) return;
      const now = st.sessions[k];
      const hasN = now && Object.values(now.ex || {}).some(a => Array.isArray(a) && a.some(full));
      if (!hasN) {
        st.sessions[k] = v;
        back++
      }
    });
    if (!st.plan && snap.raw['overload-plan']) {
      try {
        const p = JSON.parse(snap.raw['overload-plan']);
        if (p && p.days) {
          st.plan = p;
          saveP();
          back++
        }
      } catch (e) {}
    }
    if ((!st.notes || !st.notes.length) && snap.raw['overload-notes']) {
      try {
        const n = JSON.parse(snap.raw['overload-notes']);
        if (Array.isArray(n) && n.length) {
          st.notes = n;
          saveN();
          back++
        }
      } catch (e) {}
    }
    if (back) {
      saveS();
      dirtyEng();
      toast(`Update safety net put back ${back} item${back>1?'s':''}`);
      render()
    }
  } catch (e) {}
}
/* ---------- owner dashboard + usage ping ---------- */
function devId() {
  let d = '';
  try {d = localStorage.getItem('overload-dev') || ''} catch (e) {}if (!/^[\w-]{8,40}$/.test(d)) {
    d = 'd-' + Date.now().toString(36) + Math.random().toString(36).slice(2, 10);
    try {localStorage.setItem('overload-dev', d)} catch (e) {}
  }
  return d
}
let pingAt = 0;
function statsPing() {
  if (Date.now() - pingAt < 6 * 3600e3 || document.hidden) return;
  pingAt = Date.now();
  try {
    const ua = navigator.userAgent, plat = /iPhone|iPad/.test(ua) ? 'iPhone' : /Android/.test(ua) ? 'Android' : /Mac/.test(ua) ? 'Mac' : /Windows/.test(ua) ? 'Windows' : 'Other', wk = realSessions().filter(s => s.date >= curWk()).length;
    fetch(PUSH_URL + '/stats/ping', {
      method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({
        dev: devId(), name: st.set.name || '', plat, sa: !!(navigator.standalone || matchMedia('(display-mode: standalone)').matches), ver: VERSION, crew: st.cs ? st.cs.crew.name : '', sync: !!st.set.acct, wk, tot: realSessions().length
      })
    }).catch (() => {})
  } catch (e) {}
}
function dashSheet(sh) {
  const d = sh.d;
  if (!d) return `<div class="ghd"><h2>Owner dashboard</h2><button class="btn sm" id="sheetx">Done</button></div><p class="sub">${sh.err?esc(sh.err):'Loading…'}</p>`;
  const mx = Math.max(1, ...d.days.map(x => x.n));
  const tile = (v, l) => `<div><b>${v}</b><span>${l}</span></div>`;
  return `<div class="ghd"><h2>Owner dashboard</h2><button class="btn sm" id="sheetx">Done</button></div><div class="dtiles">${tile(d.dau,'today')}${tile(d.wau,'this week')}${tile(d.mau,'this month')}${tile(d.total,'devices ever')}${tile(d.home,'on home screen')}${tile(d.synced,'signed in')}${tile(d.crews,'crews')}${tile(d.wkWorkouts,'workouts this week')}</div>
 <div class="glbl">DAILY ACTIVE · LAST 30 DAYS</div><div class="dbars">${d.days.map(x=>`<i style="height:${Math.round(x.n/mx*100)}%" title="${x.d}: ${x.n} active, ${x.nw} new"></i>`).join('')}</div>
 <div class="glbl">VERSIONS</div><div class="lkrow">${Object.entries(d.ver).sort((a,b)=>b[1]-a[1]).map(([v,n])=>`<span class="lkc">${esc(v)}<span class="sub">${n}</span></span>`).join('')}</div>
 <div class="glbl">DEVICES · ${d.devices.length}</div>${d.devices.map(x=>`<div class="drow"><div><b>${esc(x.name||'Device '+x.id.slice(-4))}</b><div class="sub">${esc(x.plat||'?')} · ${x.sa?'Home screen':'Browser'} · ${esc(x.ver||'?')}${x.crew?' · '+esc(x.crew):''}${x.sync?' · ☁️':''}</div></div><div style="text-align:right"><b>${x.wk||0}</b><div class="sub">${ago(x.last)} ago</div></div></div>`).join('')}
 <p class="sub" style="margin-top:12px">Counts devices, not people. Nothing here is shared with anyone else.</p>`
}
