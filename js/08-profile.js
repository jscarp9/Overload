/* ========================================================================
   Overload: Profile and looks

   Profile page, avatars, banners and scenes, crests, cosmetic looks and effects.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

/* ---------- scenes (animated crew + profile banners) ---------- */
const SCENES = {
  forge: ['Forge', 'Crew level 1', {lv: 1}], city: ['Night City', 'Crew level 2', {lv: 2}], ocean2: ['Ocean Storm', 'Crew level 3', {lv: 3}], gym: ['Iron Gym', 'Crew level 4', {lv: 4}], aurora: ['Northern Lights', 'Crew level 6', {lv: 6}], volcano: ['Volcano', 'Crew level 8', {lv: 8}], colosseum: ['Colosseum', 'Crew rank Gold', {rank: 3}], space: ['Deep Space', 'Crew rank Diamond', {rank: 5}], thunder: ['Thunder', 'Million Pound Club trophy', {trophy: 'mpc'}], olympus: ['Olympus', 'Crew rank Olympian', {rank: 7}]
};
const LEGACY_SCENE = {steel: 'gym', ember: 'forge', ocean: 'ocean2', toxic: 'volcano', royal: 'aurora', gold: 'olympus'};
function sceneOK(k) {
  const c = st.cs && st.cs.crew;
  if (!c) return false;
  const r = SCENES[k][2];
  return !!((r.lv && (c.level || 1) >= r.lv) || (r.rank && (c.rank || 0) >= r.rank) || (r.trophy && (c.trophies || {})[r.trophy]))
}
function sceneSVG(k, lvl, uid) {
  k = LEGACY_SCENE[k] || k;
  if (!SCENES[k]) k = 'forge';
  const L = Math.max(1, lvl || 1), u = uid || ('sc' + (pid++)), R = (n, a, b) => {
    let x = hashS(u + n) % 1000 / 1000;
    return a + (b - a) * x
  };
  const dots = (n, cls, y0, y1, col, r0, r1) => Array.from({length: n}, (_, i) => `<circle class="${cls}" cx="${R('x'+cls+i,10,370).toFixed(0)}" cy="${R('y'+cls+i,y0,y1).toFixed(0)}" r="${R('r'+cls+i,r0,r1).toFixed(1)}" fill="${col}" style="animation-delay:${R('d'+cls+i,0,3).toFixed(2)}s"/>`).join('');
  let o = '';
  if (k === 'forge') o = `<rect width="380" height="130" fill="#140b07"/><ellipse class="s-gl" cx="290" cy="122" rx="130" ry="42" fill="#7a2a0c"/><ellipse class="s-gl" cx="290" cy="122" rx="64" ry="20" fill="#e0571b"/><path d="M232 92h108l-14 12h-28v14h20v8h-64v-8h20v-14h-30z" fill="#0b0605"/><rect x="250" y="66" width="72" height="7" rx="3" fill="#1c1210"/><rect x="238" y="52" width="12" height="34" rx="3" fill="#0b0605"/><rect x="322" y="52" width="12" height="34" rx="3" fill="#0b0605"/>${dots(6+Math.min(10,L),'s-em',104,118,'#ffb347',1.2,2.4)}`;
  else if (k === 'city') o = `<rect width="380" height="130" fill="#0a0f24"/>${dots(8+Math.min(12,L),'s-tw',4,40,'#fff',.6,1.3)}<circle cx="330" cy="30" r="12" fill="#e8e6d9"/><circle cx="335" cy="26" r="11" fill="#0a0f24"/><path d="M0 130V92h22V70h18v22h14V58h26v34h12V78h20v14h16V48h24v44h14V66h22v26h12V74h18v18h20V54h28v38h14V70h20v22h16V60h24v70z" fill="#141a38"/>${Array.from({length:14},(_,i)=>`<rect class="s-tw" x="${R('wx'+i,20,360).toFixed(0)}" y="${R('wy'+i,62,100).toFixed(0)}" width="3" height="3" fill="#f5c46b" style="animation-delay:${R('wd'+i,0,3).toFixed(1)}s"/>`).join('')}<rect y="118" width="380" height="12" fill="#070a18"/><g class="s-car"><rect x="380" y="122" width="10" height="2" rx="1" fill="#ff4d4d"/><rect x="470" y="122" width="10" height="2" rx="1" fill="#ffd27a"/><rect x="540" y="122" width="10" height="2" rx="1" fill="#ff4d4d"/></g>`;
  else if (k === 'ocean2') o = `<rect width="380" height="130" fill="#0b1a2e"/><g fill="#22344d"><ellipse cx="90" cy="16" rx="110" ry="22"/><ellipse cx="290" cy="12" rx="120" ry="22"/></g><path class="s-bolt" d="M300 20l-12 26h10l-8 24 22-32h-11l8-18z" fill="#eaf4ff" opacity="0"/><g class="s-wave"><path d="M-40 96 Q-10 78 20 96 T80 96 T140 96 T200 96 T260 96 T320 96 T380 96 T440 96 V130 H-40Z" fill="#12406b"/></g><g class="s-wave2"><path d="M-40 108 Q-10 92 20 108 T80 108 T140 108 T200 108 T260 108 T320 108 T380 108 T440 108 V130 H-40Z" fill="#0c2c4c"/></g>${dots(Math.min(10,2+L),'s-spray',84,100,'#cfe8ff',.7,1.4)}`;
  else if (k === 'gym') o = `<rect width="380" height="130" fill="#121316"/><rect class="s-flick" x="40" y="6" width="120" height="5" rx="2" fill="#f4f1e6"/><rect x="220" y="6" width="120" height="5" rx="2" fill="#f4f1e6"/><path d="M40 11 L10 130 H190 L160 11Z" fill="#f4f1e6" fill-opacity=".06" class="s-flick"/><path d="M220 11 L190 130 H370 L340 11Z" fill="#f4f1e6" fill-opacity=".06"/><rect x="0" y="112" width="380" height="18" fill="#0a0a0c"/><g fill="#2a2c31"><rect x="250" y="92" width="100" height="5" rx="2"/><rect x="244" y="74" width="10" height="40" rx="3"/><rect x="346" y="74" width="10" height="40" rx="3"/><rect x="232" y="80" width="10" height="28" rx="3"/><rect x="358" y="80" width="10" height="28" rx="3"/></g>${dots(6+Math.min(12,L),'s-dust',40,110,'#e8e3d6',.6,1.2)}`;
  else if (k === 'aurora') o = `<rect width="380" height="130" fill="#06121c"/>${dots(10,'s-tw',4,50,'#fff',.5,1.1)}<path class="s-aur" d="M-20 60 C60 20 120 70 200 36 S330 30 400 50 L400 70 C330 52 270 84 200 62 S60 54 -20 84Z" fill="#2ee6a6" opacity=".45"/><path class="s-aur2" d="M-20 44 C80 10 140 54 220 26 S340 20 400 36 L400 48 C330 34 280 64 210 44 S70 36 -20 60Z" fill="#7a5cff" opacity=".35"/><path d="M0 130 L70 92 L120 108 L190 80 L260 104 L320 86 L380 100 V130Z" fill="#e8f1f6"/><path d="M0 130 L70 104 L120 116 L190 96 L260 114 L320 100 L380 110 V130Z" fill="#c4d4de"/>`;
  else if (k === 'volcano') o = `<rect width="380" height="130" fill="#1a0c0a"/>${dots(6+Math.min(10,L),'s-ash',10,70,'#6b5b57',.8,1.8)}<path d="M150 130 L240 40 L262 40 L360 130Z" fill="#2a1512"/><path class="s-lava" d="M246 40 C244 60 236 70 238 86 C240 100 230 112 226 130 L236 130 C240 110 250 100 248 84 C246 68 254 58 256 40Z" fill="#ff5a1f"/><ellipse class="s-gl" cx="251" cy="40" rx="16" ry="6" fill="#ffb347"/>${dots(4+Math.min(8,L),'s-em',30,44,'#ffcf6b',.8,1.6)}`;
  else if (k === 'colosseum') o = `<rect width="380" height="130" fill="#2a1408"/><circle cx="300" cy="96" r="44" fill="#ff8a3d" opacity=".55"/><circle cx="300" cy="96" r="28" fill="#ffc16b" opacity=".8"/><path d="M120 130V62h220v68z" fill="#3b2213"/>${Array.from({length:7},(_,i)=>`<path d="M${132+i*30} 130v-30a10 10 0 0 1 20 0v30z" fill="#1a0e07"/><path d="M${132+i*30} 92v-14a10 10 0 0 1 20 0v14z" fill="#1a0e07"/>`).join('')}<rect x="120" y="58" width="220" height="6" fill="#4a2c18"/><g><rect x="104" y="80" width="4" height="40" fill="#2a1508"/><ellipse class="s-torch" cx="106" cy="76" rx="5" ry="8" fill="#ffb347"/><rect x="352" y="80" width="4" height="40" fill="#2a1508"/><ellipse class="s-torch" cx="354" cy="76" rx="5" ry="8" fill="#ffb347" style="animation-delay:.4s"/></g>`;
  else if (k === 'space') o = `<rect width="380" height="130" fill="#05040f"/>${dots(18,'s-tw',4,126,'#fff',.4,1.2)}<g class="s-gal" style="transform-origin:290px 64px"><ellipse cx="290" cy="64" rx="70" ry="18" fill="#5b2bd6" opacity=".45"/><ellipse cx="290" cy="64" rx="44" ry="11" fill="#ff4fd8" opacity=".35"/><ellipse cx="290" cy="64" rx="14" ry="6" fill="#fff" opacity=".8"/></g><path class="s-shoot" d="M40 20 l40 14" stroke="#fff" stroke-width="1.4" stroke-linecap="round"/>`;
  else if (k === 'thunder') o = `<rect width="380" height="130" fill="#12141c"/><rect class="s-sky" width="380" height="130" fill="#5b6390" opacity="0"/><g fill="#262a3a"><ellipse cx="80" cy="18" rx="90" ry="22"/><ellipse cx="220" cy="12" rx="110" ry="24"/><ellipse cx="340" cy="20" rx="80" ry="22"/></g><path class="s-bolt" d="M262 30l-16 34h14l-12 32 30-42h-15l12-24z" fill="#f4f0ff" opacity="0"/><path d="M0 130l60-52 34 26 52-48 46 40 40-30 64 50 38-22 46 36z" fill="#0b0c12"/><g opacity=".25"><circle cx="330" cy="104" r="16" fill="none" stroke="#9ba3c9" stroke-width="5"/><circle cx="330" cy="104" r="5" fill="#9ba3c9"/></g>`;
  else if (k === 'olympus') o = `<rect width="380" height="130" fill="#f3ead6"/><ellipse cx="300" cy="30" rx="120" ry="70" fill="#ffe7a8" opacity=".7"/>${dots(10,'s-tw',6,60,'#fff7d6',.8,1.6)}<g fill="#fffaf0"><ellipse class="s-cloud" cx="60" cy="112" rx="80" ry="20"/><ellipse class="s-cloud" cx="330" cy="116" rx="90" ry="20" style="animation-delay:2s"/></g><path d="M200 38 L300 18 L400 38Z" fill="#e8dcc0"/><rect x="210" y="38" width="180" height="6" fill="#d8c79f"/>${Array.from({length:6},(_,i)=>`<rect x="${218+i*28}" y="46" width="10" height="56" fill="#efe6d0"/><rect x="${216+i*28}" y="44" width="14" height="3" fill="#cdb98c"/>`).join('')}<rect x="206" y="102" width="188" height="8" fill="#d8c79f"/><path d="M30 40 c6-10 14-12 20-8 M30 40 c2 10 8 14 16 12" stroke="#c9a640" stroke-width="2" fill="none"/>`;
  return `<svg class="scene" viewBox="0 0 380 130" preserveAspectRatio="xMidYMid slice" aria-hidden="true">${o}</svg>`
}
/* ---------- crest ---------- */
const CREST_ICON = {
  barbell: '<path d="M-14 0h28M-14-7v14M14-7v14M-18-4v8M18-4v8" stroke-width="3.2"/>', skull: '<path d="M-9 3a10 10 0 1 1 18 0v5h-18z" stroke-width="2.6"/><circle cx="-4" cy="-1" r="2.4" fill="currentColor"/><circle cx="4" cy="-1" r="2.4" fill="currentColor"/><path d="M-4 8v3M0 8v3M4 8v3" stroke-width="2"/>', crown: '<path d="M-13 8l-3-14 9 6 7-10 7 10 9-6-3 14z" stroke-width="2.6"/>', bolt: '<path d="M3-14l-12 16h9l-3 12 12-16h-9z" stroke-width="2.6"/>', wolf: '<path d="M-12-12l5 8h14l5-8 1 14-7 10-6 2-6-2-7-10z" stroke-width="2.4"/><circle cx="-4" cy="0" r="1.8" fill="currentColor"/><circle cx="4" cy="0" r="1.8" fill="currentColor"/>', flame: '<path d="M0 14c-7 0-10-5-10-9 0-6 5-8 6-13 2 4 4 5 5 7 1-4 3-7 6-9 0 6 3 9 3 14 0 6-4 10-10 10z" stroke-width="2.4"/>', star: '<path d="M0-13l4 8 9 1-7 6 2 9-8-5-8 5 2-9-7-6 9-1z" stroke-width="2.4"/>', none: ''
};
/* ---------- apply looks ---------- */
function applyLook() {
  const R = document.documentElement, a = LOCK.accent.items[look('accent')];
  R.style.setProperty('--accent', a[1]);
  document.body.dataset.bg = look('bg');
  $('rest') && ($('rest').dataset.skin = look('rest'));
  iconApply()
}
let iconKey = '';
function iconApply() {
  const k = look('icon');
  if (k === iconKey) return;
  iconKey = k;
  if (k === 'red') {
    const l = document.querySelector('link[rel=apple-touch-icon]');
    if (l && l.dataset.orig) l.href = l.dataset.orig;
    return
  }
  try {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="180" height="180"><rect width="100" height="100" fill="#0b0c10"/>${plateSVG(k,k==='black'?'idle':'done').replace(/^<svg[^>]*>/,'<g transform="translate(6 6) scale(.88)">').replace(/<\/svg>$/,'</g>')}</svg>`;
    const im = new Image();
    im.onload = () => {
      const c = document.createElement('canvas');
      c.width = c.height = 180;
      c.getContext('2d').drawImage(im, 0, 0, 180, 180);
      let l = document.querySelector('link[rel=apple-touch-icon]');
      if (!l) {
        l = document.createElement('link');
        l.rel = 'apple-touch-icon';
        document.head.appendChild(l)
      }
      if (!l.dataset.orig) l.dataset.orig = l.getAttribute('href') || 'icon.png';
      l.href = c.toDataURL('image/png')
    };
    im.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg)
  } catch (e) {}
}
/* ---------- effects ---------- */
function setFX(el) {
  const k = look('setfx');
  if (k === 'pulse' || !el) return;
  const r = el.getBoundingClientRect(), fx = document.createElement('div');
  fx.className = 'sfx sfx-' + k;
  fx.style.left = (r.left + r.width / 2) + 'px';
  fx.style.top = (r.top + r.height / 2) + 'px';
  if (k === 'sparks' || k === 'pop') fx.innerHTML = Array.from({length: k === 'pop' ? 14 : 10}, (_, i) => `<i style="--a:${i*(360/(k==='pop'?14:10))}deg;--c:${['#ffbe0b','#ef2b36','#2563ff','#16b65b','#d14dff'][i%5]}"></i>`).join('');
  if (k === 'lightning') fx.innerHTML = '<svg viewBox="0 0 24 40" width="26" height="44"><path d="M14 0L2 22h9l-3 18 14-24h-9z" fill="#fff6a8" stroke="#ffd23f"/></svg>';
  document.body.appendChild(fx);
  setTimeout(() => fx.remove(), 900)
}
const PR_LINES = ['Neighbors felt that one', 'New PR. Somebody call the news', 'The bar just asked for your number', 'That plate is never going to be the same', 'PR unlocked. Ego slightly inflated', 'Gym mirror wants an autograph', 'Gravity has filed a complaint', 'Your future self says thanks'];
function prCelebrate(name) {
  const k = look('prfx'), line = PR_LINES[Math.floor(Math.random() * PR_LINES.length)];
  if (k === 'confetti') confetti(90);
  else {
    const fx = document.createElement('div');
    fx.className = 'prfx prfx-' + k;
    fx.innerHTML = Array.from({length: k === 'goldrain' ? 40 : k === 'storm' ? 6 : 28}, (_, i) => `<i style="--x:${Math.random()*100}%;--d:${(Math.random()*.9).toFixed(2)}s;--y:${(10+Math.random()*50).toFixed(0)}%"></i>`).join('');
    document.body.appendChild(fx);
    setTimeout(() => fx.remove(), 2600)
  }
  haptic('heavy');
  toast(`🏆 ${name?name+' PR. ':''}${line}`)
}
/* sound packs */
function playPack() {
  const k = look('sound');
  if (k === 'classic') return false;
  try {
    unlockAudio();
    const T = actx.currentTime, note = (f, t, d, type, v) => {
      const o = actx.createOscillator(), g = actx.createGain();
      o.type = type || 'sine';
      o.frequency.value = f;
      g.gain.setValueAtTime(.0001, T + t);
      g.gain.exponentialRampToValueAtTime(v || .3, T + t + .02);
      g.gain.exponentialRampToValueAtTime(.0001, T + t + d);
      o.connect(g).connect(actx.destination);
      o.start(T + t);
      o.stop(T + t + d + .05)
    };
    if (k === 'soft') {
      note(660, 0, .5, 'sine', .2);
      note(990, .18, .7, 'sine', .15)
    }
    if (k === 'gong') {
      note(110, 0, 2.4, 'sine', .5);
      note(220, 0, 1.8, 'triangle', .18);
      note(331, 0, 1.2, 'sine', .08)
    }
    if (k === 'airhorn') {
      [0, .45].forEach(t => {
        note(415, t, .35, 'sawtooth', .22);
        note(523, t, .35, 'sawtooth', .18);
        note(622, t, .35, 'sawtooth', .14)
      })
    }
    if (k === 'arcade') {[523, 659, 784, 1047].forEach((f, i) => note(f, i * .09, .12, 'square', .14))}
  } catch (e) {}return true
}
/* ---------- crew profiles, avatars, flex stats, PR timeline (0.15) ---------- */
const AV_ICONS = ['🦍', '🐺', '🦁', '🐻', '🦅', '🐉', '💀', '👑', '⚡', '🔥', '🗿', '🦈', '🐂', '🥷', '🤖', '👽'];
const AV_COLS = ['#ef2b36', '#2563ff', '#16b65b', '#7c4dff', '#c99a2e', '#06b6d4', '#ff3d8b', '#14161c', '#e8650c', '#64748b'];
const PHASES = ['Bulking', 'Cutting', 'Maintaining', 'Recomp', 'Peaking'];
const LTYPES = ['Powerlifter', 'Bodybuilder', 'Strength', 'Hybrid', 'Aesthetic', 'Athlete', 'Beginner'];
/* PRs in order: every time a lift's best estimated 1RM went up */
function prTimeline() {
  const rs = realSessions().slice().sort((a, b) => a.date < b.date ? -1 : 1), best = {}, out = [];
  rs.forEach(s => Object.entries(s.ex || {}).forEach(([id, a]) => {
    const top = (a || []).filter(full).sort((x, y) => e1(y) - e1(x))[0];
    if (!top) return;
    const v = e1(top);
    if (best[id] !== undefined && v > best[id] + .01) out.push({n: (s.names && s.names[id]) || exName(id), w: +top.w, r: +top.r, d: s.date});
    if (best[id] === undefined || v > best[id]) best[id] = v
  }));
  return out
}
function bestLift() {
  let b = null;
  realSessions().forEach(s => Object.entries(s.ex || {}).forEach(([id, a]) => (a || []).filter(full).forEach(x => {if (!b || e1(x) > b.v) b = {v: e1(x), n: (s.names && s.names[id]) || exName(id), w: +x.w, r: +x.r, d: s.date}})));
  return b
}
const FLEX = {
  best: ['Best lift', () => {
    const b = bestLift();
    return b ? `${b.n.split(' ')[0]} ${Math.round(b.w)}×${b.r}` : '–'
  }], streak: ['Week streak', E => E.streak + ' wk'], vol: ['Total lifted', () => kfmt(realSessions().reduce((a, s) => a + sVol(s), 0)) + ' ' + U()], wo: ['Workouts', () => String(realSessions().length)], prs: ['PRs', () => String(prTimeline().length)], badges: ['Badges', E => String(E.B.filter(b => b.earned).length)], season: ['Season level', E => 'Lv ' + E.lvl], rank: ['Rank', E => RANKS[E.rank][0]]
};
function myFlex() {
  const E = eng14(), pick = (st.set.flex || ['best', 'streak', 'vol']).slice(0, 3);
  return pick.map(k => FLEX[k] ? {k, l: FLEX[k][0], v: FLEX[k][1](E)} : null).filter(Boolean)
}/* avatars */
function avInner(m) {
  const p = m && m.profile || {}, a = p.av || (m && m.id === myId() ? st.set.av : null);
  if (a && a.t === 'plate' && PC[a.v]) return `<span class="avp">${plateSVG(a.v,'done')}</span>`;
  if (a && a.t === 'icon') return `<span style="background:${esc(a.c||'#14161c')};color:#fff;font-size:calc(var(--s) * .5)">${esc(a.v)}</span>`;
  const h = hue(m && m.id || 'x');
  return a && a.c ? `<span style="background:${esc(a.c)};color:#fff">${esc(initials(m&&m.name))}</span>` : `<span style="background:hsl(${h} 70% 88%);color:hsl(${h} 60% 26%)">${esc(initials(m&&m.name))}</span>`
}
/* profile page */
function memberData(id) {
  const me = myId();
  if (id === me || !st.cs) {
    const p = myProfile(), mm = myMember() || {};
    return {
      id: me, name: st.set.name || 'You', profile: {...(mm.profile || {}), ...p}, respect: mm.respect || 0, nick: mm.nick || '', role: mm.role || '', jersey: mm.jersey || null, self: true
    }
  }
  const m = st.cs.members.find(x => x.id === id);
  return m ? {...m, self: false} : null
}
function profPage(id) {
  const m = memberData(id);
  if (!m) return null;
  const p = m.profile || {}, me = myId(), own = isOwner(), E = eng14(), acc = p.pacc || '';
  const trs = trophies(), tp = (p.tpins || []).map(k => trs.find(t => t.k === k)).filter(Boolean).map(t => ({...t, earned: true}));
  const posts = st.cs ? (st.cs.posts || []).filter(x => x.by === m.id && x.type === 'workout').slice(0, 2) : [];
  const given = (st.cs && st.cs.respectGiven || []).includes(m.id), myNick = st.cs && st.cs.nickVotes && st.cs.nickVotes[m.id];
  const pb = p.pbanner && p.pbanner !== 'none' ? p.pbanner : (p.founder ? 'founder' : 'forge');
  return `<div class="pp"${acc?` style="--pacc:${esc(acc)}"`:''}><div class="ppban">${pb==='blueprint'?blueprintSVG():sceneSVG(pb,8,'pp'+m.id)}<button class="btn sm ppx" id="sheetx">Done</button></div>
 <div class="pphead"><span class="ppav">${avatar(m,84)}</span><div class="ppid"><h2 class="${nfxClass(m)}${p.founder?' nf-founder':''}">${p.founder?'<span class="crown">👑</span> ':''}${esc(m.name)}</h2>${m.nick?`<div class="ppnick">“${esc(m.nick)}”</div>`:''}
 <div class="pptags">${p.founder?'<span class="tg tg-f">Founder #001</span>':''}${p.og&&!p.founder?`<span class="tg tg-og">OG #${String(p.og).padStart(3,'0')}</span>`:''}${m.role?`<span class="tg">${esc(m.role)}</span>`:''}${m.jersey!=null?`<span class="tg">#${m.jersey}</span>`:''}${rankChip(p)}</div></div></div>
 ${p.motto?`<p class="ppmotto">“${esc(p.motto)}”</p>`:''}
 <div class="ppchips">${p.ltype?`<span>🏋️ ${esc(p.ltype)}</span>`:''}${p.phase?`<span>📈 ${esc(p.phase)}</span>`:''}${p.gym?`<span>📍 ${esc(p.gym)}</span>`:''}${p.song?`<span>🎵 ${esc(p.song)}</span>`:''}</div>
 <div class="ppresp">${m.self?`<span class="sub">💪 ${m.respect||0} respect from your crew</span><button class="btn sm" id="profedit">Edit profile</button>`:`<button class="btn ${given?'':'acc'}" data-respect="${m.id}"${given?' disabled':''}>${given?'Respect given today':'💪 Give respect'}</button><span class="sub">${m.respect||0} respect</span>`}</div>
 ${(p.flex||[]).length?`<div class="ppflex">${p.flex.map(f=>`<div><b>${esc(f.v)}</b><span>${esc(f.l)}</span></div>`).join('')}</div>`:''}
 ${p.best?`<div class="pppr"><span class="sub">Best lift</span><b>${esc(p.best.n)}</b><span class="pprv">${Math.round(toMine(p.best.w,p.unit||U()))} ${U()} × ${p.best.r}</span><span class="sub">${esc(p.best.d||'')}</span></div>`:''}
 ${tp.length?`<div class="glbl">PINNED TROPHIES</div><div class="pptro">${tp.map(t=>`<button class="tro on" data-tro="${esc(t.k)}"><span class="t3d">${trophySVG(t)}</span><span class="sub">${esc(t.name)}</span></button>`).join('')}</div>`:''}
 ${(p.plates||[]).length?`<div class="glbl">PLATE COLLECTION · ${p.plates.length}</div><div class="ppplates">${(p.fav&&p.plates.includes(p.fav)?[p.fav,...p.plates.filter(k=>k!==p.fav)]:p.plates).filter(k=>PC[k]).map((k,i)=>`<span class="ppl${i===0&&p.fav?' fav':''}" title="${esc(FIN[k]?FIN[k][0]:k)}">${plateSVG(k,'done')}</span>`).join('')}</div>`:''}
 ${(p.prtl||[]).length?`<details class="ppdet"><summary>PR timeline · ${p.prtl.length}</summary>${p.prtl.slice().reverse().map(x=>`<div class="tlrow"><i></i><span>${esc(x.n)}</span><b>${Math.round(toMine(x.w,p.unit||U()))}×${x.r}</b><span class="sub">${esc(x.d)}</span></div>`).join('')}</details>`:''}
 ${posts.length?`<div class="glbl">RECENT WORKOUTS</div>${posts.map(x=>postHTML(x,true)).join('')}`:''}
 ${!m.self&&st.cs?`<details class="ppdet"><summary>Crew stuff</summary>
  <div class="glbl">SUGGEST A NICKNAME</div><div class="row2"><input id="nickin" class="field" maxlength="20" placeholder="The Machine" value="${esc(myNick||'')}"><button class="btn" data-nick="${m.id}">Vote</button></div><p class="sub">Majority vote wins. It shows under their name.</p>
  ${own?`<div class="glbl">ROLE</div><div class="lkrow">${['','Co-captain','Hype Man','Spotter','Coach','Rookie','Legend','Enforcer','Comedian'].map(r=>`<button class="lkc${(m.role||'')===r?' on':''}" data-role="${m.id}:${r}">${r||'None'}</button>`).join('')}</div><button class="btn sm" data-hofm="${m.id}">🏛 Induct into Hall of Fame</button>`:''}
  ${shameHTML(m.id)}</details>`:''}
 ${Array.isArray(p.troph)&&!m.self?`<button class="btn wide" data-viewtro="${m.id}" style="margin-top:10px">🏆 View ${esc(m.name)}’s trophy room</button>`:''}</div>`
}
function blueprintSVG() {
  return `<svg class="scene" viewBox="0 0 380 130" preserveAspectRatio="xMidYMid slice" aria-hidden="true"><rect width="380" height="130" fill="#0b2a4a"/>${Array.from({length:20},(_,i)=>`<path d="M${i*20} 0V130" stroke="#3a6ea5" stroke-width=".4" opacity=".5"/>`).join('')}${Array.from({length:7},(_,i)=>`<path d="M0 ${i*20}H380" stroke="#3a6ea5" stroke-width=".4" opacity=".5"/>`).join('')}<g fill="none" stroke="#cfe3ff" stroke-width="1.2" opacity=".9"><circle cx="300" cy="64" r="40" stroke-dasharray="4 3"><animateTransform attributeName="transform" type="rotate" from="0 300 64" to="360 300 64" dur="40s" repeatCount="indefinite"/></circle><circle cx="300" cy="64" r="12"/><path d="M260 64h-60M200 54v20M120 30h60v68h-60z"/><path d="M130 44h40M130 56h30M130 68h36" opacity=".6"/></g><text x="22" y="24" style="font:800 10px Archivo,sans-serif;letter-spacing:3px" fill="#cfe3ff" opacity=".8">OVERLOAD · BLUEPRINT · 001</text></svg>`
}
/* edit profile */
function profEditSheet(sh) {
  const P = sh.p, E = eng14(), earned = trophies().filter(t => t.earned), av = sh.av || {}, ownPl = Object.keys(PC).filter(k => !plateLocked(k) && finOwned(k, E));
  const inp = (k, ph, max) => `<input class="field" data-pe="${k}" maxlength="${max}" placeholder="${ph}" value="${esc(P[k]||'')}">`;
  return `<div class="ghd"><h2>Edit profile</h2><button class="btn sm" id="sheetx">Cancel</button></div>
 <div style="text-align:center;margin:6px 0 10px">${avatar({id:myId(),name:st.set.name,profile:{av}},72)}</div>
 <div class="glbl">AVATAR</div><div class="lkrow"><button class="lkc${!av.t?' on':''}" data-av="init:">Initials</button>${AV_ICONS.map(i=>`<button class="lkc${av.t==='icon'&&av.v===i?' on':''}" data-av="icon:${i}">${i}</button>`).join('')}</div>
 <div class="lkrow">${AV_COLS.map(c=>`<button class="lkc${av.c===c?' on':''}" data-avc="${c}"><i style="background:${c}"></i></button>`).join('')}</div>
 <details class="ppdet"><summary>Use a plate as your avatar</summary><div class="lkrow">${ownPl.map(k=>`<button class="lkc${av.t==='plate'&&av.v===k?' on':''}" data-av="plate:${k}"><span class="lki2">${plateSVG(k,'done')}</span></button>`).join('')}</div></details>
 <div class="glbl">MOTTO</div>${inp('motto','Leg day is a lifestyle',60)}
 <div class="row2" style="margin-top:8px"><div><div class="glbl">GYM</div>${inp('gym','Home gym',30)}</div><div><div class="glbl">WALKOUT SONG</div>${inp('song','Lose Yourself',30)}</div></div>
 <div class="glbl">LIFTER TYPE</div><div class="lkrow">${LTYPES.map(t=>`<button class="lkc${P.ltype===t?' on':''}" data-pset="ltype:${t}">${t}</button>`).join('')}</div>
 <div class="glbl">RIGHT NOW I’M</div><div class="lkrow">${PHASES.map(t=>`<button class="lkc${P.phase===t?' on':''}" data-pset="phase:${t}">${t}</button>`).join('')}</div>
 <div class="glbl">FLEX STATS (PICK 3)</div><div class="lkrow">${Object.entries(FLEX).map(([k,f])=>`<button class="lkc${(sh.flex||[]).includes(k)?' on':''}" data-flex="${k}">${f[0]}</button>`).join('')}</div>
 <div class="glbl">PINNED TROPHIES (UP TO 3)</div><div class="lkrow">${earned.map(t=>`<button class="lkc${(P.tpins||[]).includes(t.k)?' on':''}" data-tpin="${esc(t.k)}"><span class="lki2">${trophySVG(t)}</span>${esc(t.name.slice(0,18))}</button>`).join('')||'<span class="sub">Earn trophies to pin them.</span>'}</div>
 <div class="glbl">FAVORITE PLATE</div><div class="lkrow">${ownPl.filter(k=>FIN[k]).map(k=>`<button class="lkc${P.fav===k?' on':''}" data-pset="fav:${k}"><span class="lki2">${plateSVG(k,'done')}</span></button>`).join('')||'<span class="sub">Unlock special plates to show one off.</span>'}</div>
 <div class="glbl">NAME ON YOUR PLATE RIM</div><input class="field" id="rimin" maxlength="24" placeholder="${esc((st.set.name||'Your name').toUpperCase())}" value="${esc(st.set.rim||'')}"><p class="sub">Engraved around the big plate on Today.</p>
 <div class="glbl">PROFILE COLOR</div><div class="lkrow"><button class="lkc${!P.acc?' on':''}" data-pset="acc:">Default</button>${AV_COLS.map(c=>`<button class="lkc${P.acc===c?' on':''}" data-pset="acc:${c}"><i style="background:${c}"></i></button>`).join('')}</div>
 ${st.cs?`<div class="glbl">JERSEY NUMBER</div><div class="row2"><input class="field" id="jerin" type="number" min="0" max="99" inputmode="numeric" placeholder="23" value="${esc(String((myMember()||{}).jersey??''))}"><button class="btn" id="jersave">Claim</button></div><p class="sub">Numbers are unique in your crew. #1 belongs to whoever started the crew.</p>`:''}
 <button class="btn acc wide" id="profsave" style="min-height:52px;margin-top:14px">Save profile</button>`
}/* ---------- new banner scenes (profile + founder crew) ---------- */
const XSC = {
  founder: ['Founder', 'Founder only'], sunrise: ['Sunrise', 'Free'], neon: ['Neon Gym', '4 week streak'], summit: ['Summit', '50 workouts'], chalk: ['Chalk Cloud', '10 PRs']
};
function xScene(k, uid) {
  const u = (uid || 'x' + (pid++)).replace(/[^\w-]/g, ''), R = (n, a, b) => a + (hashS(u + n) % 1000) / 1000 * (b - a);
  let o = '';
  if (k === 'founder') {
    const dust = Array.from({length: 26}, (_, i) => {
      const x = R('x' + i, 8, 372).toFixed(0), d = R('d' + i, 5, 11).toFixed(1), b = (-R('b' + i, 0, 11)).toFixed(1), r = R('r' + i, .6, 1.9).toFixed(1);
      return `<circle cx="${x}" cy="140" r="${r}" fill="#f3d77a"><animate attributeName="cy" values="140;-10" dur="${d}s" begin="${b}s" repeatCount="indefinite"/><animate attributeName="opacity" values="0;1;1;0" keyTimes="0;.15;.7;1" dur="${d}s" begin="${b}s" repeatCount="indefinite"/></circle>`
    }).join('');
    const rays = Array.from({length: 6}, (_, i) => `<path d="M360 -20 L${120+i*46} 140 L${142+i*46} 140 Z" fill="#f3d77a" opacity=".05"><animate attributeName="opacity" values=".02;.09;.02" dur="${(5+i*.8).toFixed(1)}s" repeatCount="indefinite"/></path>`).join('');
    const spokes = Array.from({length: 12}, (_, i) => {
      const a = i * Math.PI / 6;
      return `<path d="M${(Math.cos(a)*30).toFixed(1)} ${(Math.sin(a)*30).toFixed(1)}L${(Math.cos(a)*56).toFixed(1)} ${(Math.sin(a)*56).toFixed(1)}"/>`
    }).join('');
    o = `<defs><radialGradient id="${u}a" cx="80%" cy="10%" r="90%"><stop offset="0" stop-color="#4a3510"/><stop offset=".45" stop-color="#16110a"/><stop offset="1" stop-color="#050505"/></radialGradient>
  <linearGradient id="${u}g" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#9c7424"/><stop offset=".5" stop-color="#fff2bd"/><stop offset="1" stop-color="#9c7424"/></linearGradient>
  <linearGradient id="${u}s" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#fff6cf" stop-opacity="0"/><stop offset=".5" stop-color="#fff6cf" stop-opacity=".22"/><stop offset="1" stop-color="#fff6cf" stop-opacity="0"/></linearGradient>
  <path id="${u}p" d="M0 0 m-64 0 a64 64 0 1 1 128 0 a64 64 0 1 1 -128 0" fill="none"/></defs>
  <rect width="380" height="130" fill="url(#${u}a)"/>${rays}
  <g opacity=".14" stroke="#f3d77a" stroke-width=".5">${Array.from({length:22},(_,i)=>`<path d="M${i*20-60} 130 L${i*20+10} 0"/>`).join('')}</g>
  <g transform="translate(312 66)" opacity=".55"><g><animateTransform attributeName="transform" type="rotate" from="0" to="360" dur="70s" repeatCount="indefinite"/>
   <circle r="70" fill="none" stroke="url(#${u}g)" stroke-width="1.4"/><circle r="58" fill="none" stroke="#e8c55c" stroke-width=".8" stroke-dasharray="1 4"/>
   <g stroke="#e8c55c" stroke-width=".6" opacity=".6">${spokes}</g>
   <text style="font:800 7px Archivo,sans-serif;letter-spacing:3px" fill="#e8c55c"><textPath href="#${u}p">FOUNDING CREW · EST 2026 · OVERLOAD · FOUNDER 001 ·</textPath></text></g>
   <circle r="26" fill="#0b0906" stroke="url(#${u}g)" stroke-width="1.4"/>
   <path d="M-12 8 L-14 -6 L-6 0 L0 -11 L6 0 L14 -6 L12 8 Z" fill="url(#${u}g)"/></g>
  ${dust}
  <rect x="-80" y="-20" width="60" height="170" fill="url(#${u}s)" transform="rotate(18 190 65)"><animate attributeName="x" values="-120;-120;460" keyTimes="0;.5;1" dur="8s" repeatCount="indefinite"/></rect>
  <rect y="0" width="380" height="2" fill="url(#${u}g)"/><rect y="128" width="380" height="2" fill="url(#${u}g)"/>
  <text x="190" y="123" text-anchor="middle" style="font:800 5.5px Archivo,sans-serif;letter-spacing:4px" fill="#e8c55c" opacity=".35">FOUNDER · 001 · THE ORIGINAL · OVERLOAD · FOUNDER · 001</text>`
  } else if (k === 'sunrise') {
    o = `<defs><linearGradient id="${u}a" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2b1d4f"/><stop offset=".55" stop-color="#e2557a"/><stop offset="1" stop-color="#ffb35c"/></linearGradient></defs><rect width="380" height="130" fill="url(#${u}a)"/>
  <circle cx="280" cy="100" r="30" fill="#ffe08a"><animate attributeName="cy" values="104;92;104" dur="12s" repeatCount="indefinite"/></circle>
  <path d="M0 130 L0 96 Q60 70 120 92 T250 88 T380 84 V130Z" fill="#5a2846"/><path d="M0 130 L0 108 Q80 90 170 110 T380 104 V130Z" fill="#2c1530"/>
  ${[0,1,2].map(i=>`<path d="M${60+i*24} ${34+i*8} q4 -4 8 0 q4 -4 8 0" stroke="#2c1530" stroke-width="1.4" fill="none"><animateTransform attributeName="transform" type="translate" values="0 0;40 -6;0 0" dur="${10+i*2}s" repeatCount="indefinite"/></path>`).join('')}`
  } else if (k === 'neon') {
    o = `<rect width="380" height="130" fill="#08060f"/><g stroke="#7a2cff" stroke-width=".8" opacity=".55">${Array.from({length:13},(_,i)=>`<path d="M190 70 L${-120+i*52} 130"/>`).join('')}${[78,88,102,122].map(y=>`<path d="M0 ${y}H380"/>`).join('')}</g>
  <text x="240" y="52" text-anchor="middle" style="font:900 34px Archivo,sans-serif;letter-spacing:6px" fill="none" stroke="#ff3d8b" stroke-width="1.6">LIFT<animate attributeName="opacity" values="1;1;.2;1;.4;1;1" keyTimes="0;.6;.62;.64;.66;.68;1" dur="4s" repeatCount="indefinite"/></text>
  <text x="240" y="52" text-anchor="middle" style="font:900 34px Archivo,sans-serif;letter-spacing:6px" fill="#ff3d8b" opacity=".18">LIFT</text>
  <rect x="200" y="62" width="80" height="2" rx="1" fill="#22e3ff"><animate attributeName="opacity" values=".4;1;.4" dur="2s" repeatCount="indefinite"/></rect>`
  } else if (k === 'summit') {
    o = `<rect width="380" height="130" fill="#0c1426"/>${Array.from({length:16},(_,i)=>`<circle cx="${R('sx'+i,5,375).toFixed(0)}" cy="${R('sy'+i,4,60).toFixed(0)}" r="${R('sr'+i,.4,1.1).toFixed(1)}" fill="#fff"><animate attributeName="opacity" values=".2;1;.2" dur="${R('sd'+i,2,5).toFixed(1)}s" repeatCount="indefinite"/></circle>`).join('')}
  <path d="M0 130 L90 70 L140 96 L230 30 L300 88 L380 60 V130Z" fill="#26324d"/><path d="M230 30 L214 46 L224 44 L232 52 L240 44 L248 48Z" fill="#eef3fb"/><path d="M0 130 L70 100 L160 118 L260 96 L380 112 V130Z" fill="#141d33"/>
  <path d="M230 30 V12" stroke="#c9cfdb" stroke-width="1.2"/><path d="M230 12 l14 4 -14 4z" fill="#ef2b36"><animateTransform attributeName="transform" type="skewY" values="0;6;0" dur="1.6s" repeatCount="indefinite"/></path>`
  } else if (k === 'chalk') {
    o = `<rect width="380" height="130" fill="#15161b"/>${Array.from({length:14},(_,i)=>`<circle cx="${R('cx'+i,20,360).toFixed(0)}" cy="${R('cy'+i,20,110).toFixed(0)}" r="${R('cr'+i,8,26).toFixed(0)}" fill="#e9e9ef" opacity=".05"><animateTransform attributeName="transform" type="translate" values="0 0;${R('tx'+i,-14,14).toFixed(0)} ${R('ty'+i,-10,4).toFixed(0)};0 0" dur="${R('td'+i,7,14).toFixed(1)}s" repeatCount="indefinite"/></circle>`).join('')}
  <g fill="#2a2c33"><rect x="200" y="74" width="150" height="5" rx="2"/><rect x="214" y="54" width="12" height="46" rx="3"/><rect x="324" y="54" width="12" height="46" rx="3"/><rect x="200" y="60" width="10" height="34" rx="3"/><rect x="340" y="60" width="10" height="34" rx="3"/></g>
  ${Array.from({length:18},(_,i)=>`<circle cx="${R('dx'+i,180,370).toFixed(0)}" cy="${R('dy'+i,40,100).toFixed(0)}" r=".9" fill="#fff" opacity=".5"><animate attributeName="opacity" values="0;.7;0" dur="${R('dd'+i,2,4).toFixed(1)}s" begin="${R('db'+i,0,3).toFixed(1)}s" repeatCount="indefinite"/></circle>`).join('')}`
  }
  return `<svg class="scene" viewBox="0 0 380 130" preserveAspectRatio="xMidYMid slice" aria-hidden="true">${o}</svg>`
}
{
  const _s = sceneSVG;
  sceneSVG = function(k, lvl, uid) {
    if (XSC[k]) return xScene(k, uid);
    return _s(k, lvl, uid)
  }
}
SCENES.founder = ['Founder', 'Founder only', {founder: 1}];
{
  const _ok = sceneOK;
  sceneOK = function(k) {
    if (k === 'founder') return !!(st.cs && st.cs.crew && st.cs.crew.fcrew);
    return _ok(k)
  }
}
/* ---------- profile tab ---------- */
const BCATS = () => {
  const E = engine();
  return [['mile', 'Milestones'], ['week', 'Weekly'], ...['Lift clubs', 'Consistency', 'Collections', 'Crew', 'Hidden', 'Funny'].filter(c => E.B.some(b => b.cat === c)).map(c => [c, c])]
};
function row16(id, icon, label, val, extra) {
  return `<button class="r15" ${id.includes('=')?id:`id="${id}"`}${extra||''}><span>${icon}</span><b>${label}</b><span class="sub">${val||''}</span><span class="chev">›</span></button>`
}
function renderProfile16() {
  const E = engine(), r = RANKS[E.rank], nx = RANKS[E.rank + 1], ban = myBanner(), show = (st.set.showcase || []).map(k => E.B.find(x => x.k === k) || E.R.find(x => x.k === k)).filter(Boolean);
  const bar = (a, b, l) => `<div class="pbar"><div class="qh"><span>${l}</span><b>${Math.min(a,b)}/${b}</b></div><i><u style="width:${Math.min(100,Math.round(a/b*100))}%"></u></i></div>`;
  const t = trophies(), got = t.filter(x => x.earned).length, bl = st.plan ? boxesLeft().length : 0, bE = E.B.filter(b => b.earned).length, lr = feat('liftranks') && liftRanks().length;
  const nm = st.set.name || 'Lifter', P = st.set.prof || {};
  let h = `<div class="card ph16">${ban?`<div class="ph16ban">${sceneSVG(ban,8,'me'+ban)}</div>`:'<div class="ph16ban ph16plain"></div>'}
 <div class="ph16row"><span class="ph16av">${avatar({id:myId(),name:nm,profile:{av:st.set.av||{}}},76)}</span><div class="ph16emb">${emblem(E.rank,E.rank>=6?'spin':'')}</div></div>
 <h2 class="ph16n ${nfxClass({profile:{nameFx:look('nameFx')}})}${st.set.founder?' nf-founder':''}">${st.set.founder?'<span class="crown">👑</span> ':''}${esc(nm)}</h2>
 <div class="ph16tags">${st.set.founder?'<span class="tg tg-f">Founder #001</span>':''}<span class="tg">${esc(E.rname)}</span><span class="tg">🔥 ${E.streak} wk</span>${E.since?`<span class="tg">Since ${MON[toD(E.since).getMonth()]} ${toD(E.since).getFullYear()}</span>`:''}</div>
 ${P.motto?`<p class="ph16m">“${esc(P.motto)}”</p>`:''}
 <div class="ph16pins">${show.map(b=>`<button class="pin16" data-bdg="${b.k}">${b.icon}<span>${esc(b.name)}</span></button>`).join('')}${show.length<3?`<button class="pin16 add" id="pinpick">＋ Pin a badge</button>`:''}</div>
 <div class="ph16btns"><button class="btn" id="profedit2">Edit profile</button><button class="btn" id="shareprof">📸 Share</button></div></div>`;
  h += `<div class="tiles four life16"><div class="tile"><div class="num">${E.workouts}</div><div class="sub">Workouts</div></div><div class="tile"><div class="num">${E.lifeVol>=1e6?(E.lifeVol/1e6).toFixed(2)+'M':kfmt(E.lifeVol)}</div><div class="sub">${U()} moved</div></div><div class="tile"><div class="num">${E.lifePR}</div><div class="sub">PRs</div></div><div class="tile"><div class="num">${E.power}</div><div class="sub">Rank power</div></div></div>`;
  if (E.trial) {
    const tr = E.trial;
    h += `<div class="card trial"><div class="glbl">🔥 PROMOTION TRIAL · ${RANKS[tr.tier][0].toUpperCase()}</div>${bar(tr.days,tr.need,'Train on your planned days')}${bar(tr.beat,tr.needBeat,'Beat last week on lifts')}</div>`
  } else if (nx) h += `<div class="card nx16"><div class="glbl">NEXT RANK: ${nx[0].toUpperCase()}</div>${bar(E.active,nx[1],'Active weeks')}${bar(E.bcount,nx[2],'Badges')}</div>`;
  h += `<div class="card rows15">${row16('ph27libbtn','📸','Progress photos',ph27List().length?ph27List().length+' saved':'From your crew posts')}${st.cs?row16('myprof','🪪','Your crew profile','What your crew sees'):''}${row16('lockerbtn',bl?'🎁':'🎨','Customize',bl?bl+' mystery box'+(bl>1?'es':''):'Themes, effects, sounds')}${row16('troombtn','🏆','Trophy room',got+'/'+t.length)}${row16('badges16','🏅','Badges',bE+' earned')}${feat('season')?row16('openpass','🎟','Season pass','Level '+E.lvl+' of 30'):''}</div>`;
  h += `<div class="card rows15">${lr?row16('data-psec="lr"','🏋️','Lift ranks',lr+' lifts ranked'):''}${feat('mastery')?row16('data-psec="mast"','💪','Muscle mastery','Bronze to Legend'):''}${row16('data-psec="ladder"','🪜','Rank ladder','Rank '+(E.rank+1)+' of 8')}${row16('data-psec="life"','📈','Lifetime stats',E.lifeSets.toLocaleString()+' sets')}${row16('openyir','🎬',new Date().getFullYear()+' Year in review',new Date().getMonth()===11?'Ready':'Preview')}</div>`;
  return h
}
function psecSheet(sh) {
  const E = engine(), k = sh.sec2;
  let title = '', body = '';
  if (k === 'ladder') {
    title = 'Rank ladder';
    body = `<p class="sub">Each rank has 3 divisions: III, II, then I.</p>${RANKS.map((x,i)=>`<div class="ladder${i===E.rank?' cur':''}${i<E.rank?' past':''}">${emblem(i)}<div><b>${x[0]}</b><div class="sub">${i?`${x[1]} active weeks · ${x[2]} badges`:'Start here'}${i?` · ${UNLOCKS.find(u=>u[0]===i)[1]}`:''}</div></div>${i<=E.rank?'<span class="ok">✓</span>':'<span class="lk">🔒</span>'}</div>`).join('')}`
  } else if (k === 'life') {
    title = 'Lifetime stats';
    body = `<div class="tiles four"><div class="tile"><div class="num">${E.workouts}</div><div class="sub">Workouts</div></div><div class="tile"><div class="num">${E.lifeSets.toLocaleString()}</div><div class="sub">Sets</div></div><div class="tile"><div class="num">${E.lifeVol>=1e6?(E.lifeVol/1e6).toFixed(2)+'M':kfmt(E.lifeVol)}</div><div class="sub">${U()} moved</div></div><div class="tile"><div class="num">${E.lifePR}</div><div class="sub">PRs</div></div></div><p class="sub">Rank power ${E.power} · ${E.active} active weeks · ❄️ ${E.freeze} streak freeze${E.freeze===1?'':'s'}</p>`
  } else if (k === 'lr') {
    title = 'Lift ranks';
    body = `<div class="fwd">${liftRanksHTML().replace(/^<div class="card"><h3>Lift ranks<\/h3>/,'<div>')}</div>`
  } else if (k === 'mast') {
    title = 'Muscle mastery';
    body = masteryHTML().replace(/^<div class="card"><h3>Muscle mastery<\/h3>/, '<div>')
  }
  return `<div class="ghd"><h2>${title}</h2><button class="btn sm" id="sheetx">Done</button></div>${body}`
}
function badgesSheet(sh) {
  const E = engine(), cats = BCATS(), c = sh.cat || 'mile';
  let body = '';
  if (c === 'mile') body = `<p class="sub">Each one levels up: Bronze, Silver, Gold, Platinum, Diamond, Legend.</p><div class="bcase">${Object.keys(LADDER).map(ladderTile).join('')}</div>`;
  else if (c === 'week') body = `<p class="sub">Earn these again every week.</p><div class="bcase">${E.R.map(badgeBtn).join('')}</div>`;
  else body = `<div class="bcase">${E.B.filter(b=>b.cat===c).map(badgeBtn).join('')}</div>`;
  return `<div class="ghd"><h2>Badges</h2><button class="btn sm" id="sheetx">Done</button></div><p class="sub">${E.B.filter(b=>b.earned).length} earned. Tap one to see it or pin it.</p><div class="pchips">${cats.map(([k,n])=>`<button class="pchip${k===c?' on':''}" data-bcat="${esc(k)}">${esc(n)}</button>`).join('')}</div><div class="fwd">${body}</div>`
}
/* ---------- edit profile as a clean list ---------- */
const PE16 = [['banner', '🖼', 'Banner'], ['avatar', '🙂', 'Avatar'], ['motto', '💬', 'Motto'], ['gym', '📍', 'Gym'], ['song', '🎵', 'Walkout song'], ['ltype', '🏋️', 'Lifter type'], ['phase', '📈', 'Right now I’m'], ['flex', '📊', 'Flex stats'], ['tpins', '🏆', 'Pinned trophies'], ['fav', '💿', 'Favorite plate'], ['rim', '✍️', 'Name on plate'], ['acc', '🎨', 'Profile color'], ['jersey', '🔢', 'Jersey number']];
function pe16Val(sh, k) {
  const P = sh.p;
  if (k === 'banner') {
    const b = myBanner();
    return b ? (LOCK.pbanner.items[b] || [b])[0] : 'None'
  }
  if (k === 'avatar') {
    const a = sh.av || {};
    return a.t === 'icon' ? a.v : a.t === 'plate' ? 'Plate' : 'Initials'
  }
  if (['motto', 'gym', 'song', 'ltype', 'phase'].includes(k)) return P[k] || 'Not set';
  if (k === 'flex') return (sh.flex || []).map(f => (FLEX[f] || [f])[0]).join(', ') || 'None';
  if (k === 'tpins') return (P.tpins || []).length + ' of 3';
  if (k === 'fav') return P.fav ? (FIN[P.fav] || [P.fav])[0] : 'None';
  if (k === 'rim') return sh.rim || 'Default';
  if (k === 'acc') return P.acc ? `<i class="sw16" style="background:${esc(P.acc)}"></i>` : 'Default';
  if (k === 'jersey') {
    const m = myMember && myMember();
    return m && m.jersey != null ? '#' + m.jersey : 'None'
  }
  return ''
}
function profEdit16(sh) {
  const k = sh.sec;
  if (!k) {
    const rows = PE16.filter(([k]) => k !== 'jersey' || st.cs).map(([k, i, l]) => row16(`data-pesec="${k}"`, i, l, pe16Val(sh, k))).join('');
    return `<div class="ghd"><h2>Edit profile</h2><button class="btn sm" id="sheetx">Cancel</button></div><div style="text-align:center;margin:4px 0 12px">${avatar({id:myId(),name:st.set.name,profile:{av:sh.av||{}}},64)}</div><div class="card rows15 pe16">${rows}</div><button class="btn acc wide" id="profsave" style="min-height:52px;margin-top:6px">Save profile</button>`
  }
  const lbl = (PE16.find(x => x[0] === k) || [, , ''])[2], head = `<div class="ghd"><button class="btn sm" id="pe16back">‹ Back</button><h2 style="margin:0">${lbl}</h2><button class="btn sm acc" id="pe16back2">Done</button></div>`;
  if (k === 'banner') {
    const E = engine();
    return head + `<div class="scgrid">${Object.entries(LOCK.pbanner.items).filter(([b,it])=>!FONLY18.includes(b)||it[3](E)).sort((a,c)=>FO18(a[0])-FO18(c[0])).map(([b,it])=>{const ok=owns('pbanner',b,E),on=myBanner()===b||(b==='none'&&!myBanner());return `<button class="scb${ok?'':' lk'}${on?' on':''}" data-look="pbanner:${b}" aria-pressed="${on}">${b==='none'?'<div class="scnone">None</div>':sceneSVG(b,8,'pk'+b)}<span>${esc(it[0])}${ok?'':' 🔒'}</span></button>`}).join('')}</div><p class="sub">Locked ones show how to unlock them when you tap.</p>`
  }
  const P = sh.p, E = eng14(), av = sh.av || {}, ownPl = Object.keys(PC).filter(x => !plateLocked(x) && finOwned(x, E)), inp = (f, ph, max) => `<input class="field" data-pe="${f}" maxlength="${max}" placeholder="${ph}" value="${esc(P[f]||'')}">`;
  const chips = (f, L) => `<div class="lkrow lk16">${L.map(t=>`<button class="lkc${P[f]===t?' on':''}" data-pset="${f}:${t}">${t}</button>`).join('')}</div>`;
  let part = '';
  if (k === 'avatar') part = `<div style="text-align:center;margin:6px 0 10px">${avatar({id:myId(),name:st.set.name,profile:{av}},84)}</div><div class="glbl">ICON</div><div class="lkrow lk16"><button class="lkc${!av.t?' on':''}" data-av="init:">Initials</button>${AV_ICONS.map(i=>`<button class="lkc${av.t==='icon'&&av.v===i?' on':''}" data-av="icon:${i}">${i}</button>`).join('')}</div><div class="glbl">COLOR</div><div class="lkrow lk16">${AV_COLS.map(c=>`<button class="lkc${av.c===c?' on':''}" data-avc="${c}"><i style="background:${c}"></i></button>`).join('')}</div>${ownPl.length?`<div class="glbl">OR USE A PLATE</div><div class="lkrow lk16">${ownPl.map(x=>`<button class="lkc${av.t==='plate'&&av.v===x?' on':''}" data-av="plate:${x}"><span class="lki2">${plateSVG(x,'done')}</span></button>`).join('')}</div>`:''}`;
  else if (k === 'motto') part = inp('motto', 'Leg day is a lifestyle', 60);
  else if (k === 'gym') part = inp('gym', 'Home gym', 30);
  else if (k === 'song') part = inp('song', 'Lose Yourself', 30);
  else if (k === 'ltype') part = chips('ltype', LTYPES);
  else if (k === 'phase') part = chips('phase', PHASES);
  else if (k === 'flex') part = `<p class="sub">Pick 3 to show on your crew profile.</p><div class="lkrow lk16">${Object.entries(FLEX).map(([f,x])=>`<button class="lkc${(sh.flex||[]).includes(f)?' on':''}" data-flex="${f}">${x[0]}</button>`).join('')}</div>`;
  else if (k === 'tpins') {
    const earned = trophies().filter(t => t.earned);
    part = `<p class="sub">Up to 3.</p><div class="lkrow lk16">${earned.map(t=>`<button class="lkc${(P.tpins||[]).includes(t.k)?' on':''}" data-tpin="${esc(t.k)}"><span class="lki2">${trophySVG(t)}</span>${esc(t.name.slice(0,18))}</button>`).join('')||'<span class="sub">Earn trophies to pin them.</span>'}</div>`
  } else if (k === 'fav') part = `<div class="lkrow lk16">${ownPl.filter(x=>FIN[x]).map(x=>`<button class="lkc${P.fav===x?' on':''}" data-pset="fav:${x}"><span class="lki2">${plateSVG(x,'done')}</span></button>`).join('')||'<span class="sub">Unlock special plates to show one off.</span>'}</div>`;
  else if (k === 'rim') part = `<input class="field" id="rimin" maxlength="24" placeholder="${esc((st.set.name||'Your name').toUpperCase())}" value="${esc(sh.rim||'')}"><p class="sub">Engraved around the big plate on Today.</p>`;
  else if (k === 'acc') part = `<div class="lkrow lk16"><button class="lkc${!P.acc?' on':''}" data-pset="acc:">Default</button>${AV_COLS.map(c=>`<button class="lkc${P.acc===c?' on':''}" data-pset="acc:${c}"><i style="background:${c}"></i></button>`).join('')}</div>`;
  else if (k === 'jersey') part = `<div class="row2"><input class="field" id="jerin" type="number" min="0" max="99" inputmode="numeric" placeholder="23" value="${esc(String((myMember()||{}).jersey??''))}"><button class="btn" id="jersave">Claim</button></div><p class="sub">Numbers are unique in your crew. #1 belongs to whoever started the crew.</p>`;
  return head + `<div class="pe16sec">${part}</div>`
}
function grab16(sh) {
  document.querySelectorAll('#sheet [data-pe]').forEach(i => sh.p[i.dataset.pe] = i.value.trim());
  const r = $('rimin');
  if (r) sh.rim = r.value.trim()
}
/* ---------- customize as a clean list ---------- */
const LK_ICON = {
  accent: '🎨', bg: '🌌', welcome: '👋', icon: '📱', setfx: '✨', prfx: '🎉', rest: '⏱', sound: '🔊', nameFx: '🔤', card: '🃏', share: '📸', react: '😂'
};
function locker16(sh) {
  const E = engine(), bl = boxesLeft(E).length, t = sh.sec;
  if (!t) {
    return `<div class="ghd"><h2>Customize</h2><button class="btn sm" id="sheetx">Done</button></div>${bl?`<button class="card boxcta" id="openbox"><span class="mbox">🎁</span><span class="stx"><b>${bl} mystery box${bl>1?'es':''} to open</b><span class="sub">Earned from your season pass and rank-ups</span></span></button>`:''}
  <div class="card rows15">${Object.entries(LOCK).filter(([k])=>k!=='pbanner').map(([k,v])=>{const own=Object.keys(v.items).filter(i=>owns(k,i,E)).length,n=Object.keys(v.items).length;return row16(`data-lksec="${k}"`,LK_ICON[k]||'•',v.name,k==='react'?own+' of '+n:esc(v.items[look(k)][0])+(own<n?` · ${own}/${n}`:''))}).join('')}${row16('editlook','🪞','Frames and auras','')}${row16('data-lksec="fun"','😜','Fun extras','')}</div>
  <p class="sub" style="text-align:center">Plate designs live in Plan → Plate. Profile banners are in Edit profile.</p>`
  }
  const head = `<div class="ghd"><button class="btn sm" id="lk16back">‹ Back</button><h2 style="margin:0">${t==='fun'?'Fun extras':esc(LOCK[t].name)}</h2><button class="btn sm acc" id="sheetx">Done</button></div>`;
  if (t === 'fun') return head + tog('roasts', st.set.roasts !== false, 'Roast lines on crew posts', 'A funny line under each workout post') + tog('restJokes', st.set.restJokes !== false, 'Rest timer one-liners');
  const v = LOCK[t];
  return head + `<div class="lkrow lk16">${Object.entries(v.items).map(([k,it])=>{const o=owns(t,k,E),on=t==='react'?o:look(t)===k;return `<button class="lkc${o?'':' lk'}${on?' on':''}" data-look="${t}:${k}" aria-pressed="${on}">${t==='accent'?`<i style="background:${it[1]}"></i>`:''}${t==='icon'?`<span class="lki2">${plateSVG(it[1],it[1]==='black'?'idle':'done')}</span>`:''}${esc(it[0])}${o?'':' <span class="lk16i">🔒</span>'}</button>`}).join('')}</div>
 <p class="sub">${t==='react'?'Reactions you own show up on crew posts.':'Tap a locked one to see how to unlock it.'}${t==='icon'?' To use a new icon on iPhone, remove Overload from your Home Screen and add it again from Safari.':''}</p>`
}

/* ---------- crests ---------- */
const FONLY18 = ['vault', 'fblue', 'throne', 'cofounder', 'founder'];
const FO18 = k => {
  const i = FONLY18.indexOf(k);
  return i < 0 ? 99 : i
};
const CR_METAL = {
  steel: ['Steel', ['#f4f6f9', '#b8bec8', '#6b717c', '#e3e6eb'], 0], bronze: ['Bronze', ['#ffd9b0', '#c8834a', '#6e3c16', '#f0b783'], 1], silver: ['Silver', ['#ffffff', '#cfd4dc', '#7d838e', '#eef1f5'], 2], gold: ['Gold', ['#fff4c4', '#e3b84a', '#7a5410', '#ffe08a'], 3], black: ['Black chrome', ['#7a7d86', '#2a2c31', '#050506', '#4a4d55'], 5], founders: ['Founders gold', ['#fff4c4', '#e3b84a', '#7a5410', '#ffe08a'], 'f']
};
const CR_SHAPE = {
  shield: ['Shield', 'M0 -58 L46 -44 L46 4 C46 34 24 52 0 62 C-24 52 -46 34 -46 4 L-46 -44 Z'], kite: ['Kite', 'M0 -60 C24 -60 46 -50 46 -30 C46 10 26 40 0 64 C-26 40 -46 10 -46 -30 C-46 -50 -24 -60 0 -60 Z'], seal: ['Seal', 'M0 -56 A56 56 0 1 1 0 56 A56 56 0 1 1 0 -56 Z'], hex: ['Hex', 'M0 -60 L52 -30 L52 30 L0 60 L-52 30 L-52 -30 Z']
};
const CR_PAT = {plain: 'Plain', chevron: 'Chevron', stripes: 'Stripes', split: 'Split', quarter: 'Quartered'};
const CR_BORDER = {plain: 'Clean', rope: 'Rope', studs: 'Studded'};
const DB = `<rect x="-40" y="-3" width="80" height="6" rx="2"/><rect x="-36" y="-14" width="7" height="28" rx="2"/><rect x="-28" y="-10" width="5" height="20" rx="2"/><rect x="29" y="-14" width="7" height="28" rx="2"/><rect x="23" y="-10" width="5" height="20" rx="2"/>`;
const CR_EMB = {
  barbells: ['Crossed dumbbells', `<g transform="rotate(-35)">${DB}</g><g transform="rotate(35)">${DB}</g>`],
  bull: ['Bull', `<path d="M-34 -30 C-30 -16 -22 -12 -14 -14 L-10 -18 L10 -18 L14 -14 C22 -12 30 -16 34 -30 C36 -14 30 -4 18 -4 L16 6 L12 22 C8 30 4 34 0 34 C-4 34 -8 30 -12 22 L-16 6 L-18 -4 C-30 -4 -36 -14 -34 -30 Z"/><path d="M-10 0 L-5 4 L-10 7 Z M10 0 L5 4 L10 7 Z" fill="var(--eye)" stroke="none"/><circle cx="0" cy="29" r="5" fill="none" stroke="var(--eye)" stroke-width="1.6"/>`],
  wolf: ['Wolf', `<path d="M-26 -34 L-14 -14 L-6 -16 L0 -12 L6 -16 L14 -14 L26 -34 L28 -6 L22 8 L12 18 L6 30 L0 34 L-6 30 L-12 18 L-22 8 L-28 -6 Z"/><path d="M-14 -2 L-5 2 L-12 5 Z M14 -2 L5 2 L12 5 Z M-4 22 L0 26 L4 22 Z" fill="var(--eye)" stroke="none"/>`],
  eagle: ['Eagle', `<path d="M0 -26 C5 -26 8 -22 8 -17 L14 -12 L8 -10 C8 -4 6 2 4 6 C18 0 34 -10 46 -24 C44 -8 36 4 24 10 C32 10 38 8 42 6 C36 16 24 22 12 22 L8 34 L0 30 L-8 34 L-12 22 C-24 22 -36 16 -42 6 C-38 8 -32 10 -24 10 C-36 4 -44 -8 -46 -24 C-34 -10 -18 0 -4 6 C-6 2 -8 -4 -8 -10 C-8 -20 -5 -26 0 -26 Z"/><circle cx="3" cy="-19" r="1.6" fill="var(--eye)" stroke="none"/>`],
  skull: ['Skull', `<path d="M0 -30 C18 -30 28 -18 28 -2 C28 8 22 14 18 16 L18 26 L-18 26 L-18 16 C-22 14 -28 8 -28 -2 C-28 -18 -18 -30 0 -30 Z"/><path d="M-16 -4 C-16 -12 -4 -12 -4 -2 C-4 4 -16 6 -16 -4 Z M16 -4 C16 -12 4 -12 4 -2 C4 4 16 6 16 -4 Z M0 6 L-4 13 L4 13 Z" fill="var(--eye)" stroke="none"/><path d="M-10 18 V26 M-3 18 V26 M4 18 V26 M11 18 V26" stroke="var(--eye)" stroke-width="2"/>`],
  kettle: ['Kettlebell', `<path d="M-16 -8 L-16 -18 C-16 -34 16 -34 16 -18 L16 -8 L9 -8 L9 -18 C9 -26 -9 -26 -9 -18 L-9 -8 Z"/><path d="M-22 8 C-22 -8 -10 -12 0 -12 C10 -12 22 -8 22 8 C22 22 12 30 0 30 C-12 30 -22 22 -22 8 Z"/><rect x="-14" y="28" width="28" height="5" rx="2"/>`],
  crown: ['Crown', `<g transform="scale(1.3)"><path d="M-20 4 L-24 -18 L-11 -8 L0 -26 L11 -8 L24 -18 L20 4 Z"/><rect x="-20" y="6" width="40" height="6" rx="2"/></g>`],
  bolt: ['Lightning', `<path d="M8 -34 L-20 4 L-2 4 L-10 34 L20 -6 L2 -6 Z"/>`],
  flame: ['Flame', `<path d="M2 -34 C10 -20 26 -10 24 10 C22 26 10 34 0 34 C-12 34 -24 24 -22 8 C-20 -4 -10 -8 -8 -20 C-2 -14 0 -8 0 -2 C6 -8 8 -20 2 -34 Z"/>`],
  star: ['Star', `<path d="M0 -32 L9 -10 L32 -10 L14 4 L20 28 L0 14 L-20 28 L-14 4 L-32 -10 L-9 -10 Z"/>`]
};
const CR_ICONMAP = {
  barbell: 'barbells', skull: 'skull', crown: 'crown', bolt: 'bolt', wolf: 'wolf', flame: 'flame', star: 'star', none: 'barbells'
};
function crNorm(cr) {
  cr = {...(cr || {})};
  if (!CR_SHAPE[cr.shape]) cr.shape = cr.shape === 'plate' || cr.shape === 'circle' ? 'seal' : 'shield';
  if (!CR_EMB[cr.emb]) cr.emb = CR_ICONMAP[cr.icon] || 'barbells';
  if (!CR_PAT[cr.pat]) cr.pat = 'plain';
  if (!CR_METAL[cr.metal]) cr.metal = 'steel';
  if (!CR_BORDER[cr.border]) cr.border = 'plain';
  cr.c1 = /^#[0-9a-f]{6}$/i.test(cr.c1 || '') ? cr.c1 : '#b8222c';
  cr.c2 = /^#[0-9a-f]{6}$/i.test(cr.c2 || '') ? cr.c2 : '#14161c';
  if (cr.rib == null) cr.rib = cr.txt || '';
  return cr
}
function crPat(k, id, c1, c2) {
  if (k === 'chevron') return `<pattern id="${id}" width="24" height="24" patternUnits="userSpaceOnUse"><rect width="24" height="24" fill="${c1}"/><path d="M0 18 L12 6 L24 18 L24 24 L12 12 L0 24 Z" fill="${c2}" opacity=".45"/></pattern>`;
  if (k === 'stripes') return `<pattern id="${id}" width="14" height="14" patternUnits="userSpaceOnUse" patternTransform="rotate(30)"><rect width="14" height="14" fill="${c1}"/><rect width="6" height="14" fill="${c2}" opacity=".45"/></pattern>`;
  if (k === 'quarter') return `<pattern id="${id}" x="-60" y="-60" width="120" height="120" patternUnits="userSpaceOnUse"><rect width="120" height="120" fill="${c1}"/><rect width="60" height="60" fill="${c2}"/><rect x="60" y="60" width="60" height="60" fill="${c2}"/></pattern>`;
  if (k === 'split') return `<linearGradient id="${id}" x1="0" y1="0" x2="1" y2="0"><stop offset=".5" stop-color="${c1}"/><stop offset=".5" stop-color="${c2}"/></linearGradient>`;
  return `<radialGradient id="${id}" cx="50%" cy="35%" r="70%"><stop offset="0" stop-color="${c1}"/><stop offset="1" stop-color="${c2}"/></radialGradient>`
}
crestSVG = function(cr0, rank, size) {
  const cr = crNorm(cr0), fc = cr.metal === 'founders', r = fc ? 7 : (rank || 0), s = size || 52, small = s < 48, id = 'cq' + (pid++), M = CR_METAL[cr.metal][1], shp = CR_SHAPE[cr.shape][1];
  const c1 = fc ? '#08080a' : cr.c1, c2 = fc ? '#2a2418' : cr.c2, dark = metalDark(cr.metal), embF = fc ? `url(#${id}m)` : dark ? '#15161a' : `url(#${id}m)`;
  let deco = '', top = '', rib = '';
  if (!small) {
    if (r >= 5) deco += `<g fill="url(#${id}m)">${[-1,1].map(q=>`<path transform="scale(${q},1)" d="M48 -30 C78 -40 96 -24 100 -4 C88 -14 76 -14 66 -8 C80 -6 88 4 90 16 C78 6 66 4 56 8 C64 12 68 20 68 28 C58 18 50 14 46 14 Z"/>`).join('')}</g>`;
    if (r >= 7) deco += `<g fill="url(#${id}m)" stroke="${M[2]}" stroke-width=".5">${[-1,1].map(q=>Array.from({length:7},(_,i)=>{const a=(118+i*15)*Math.PI/180,x=Math.cos(a)*66*q,y=Math.sin(a)*66+8,rot=q*(i*15+28);return `<path d="M${x.toFixed(1)} ${(y-9).toFixed(1)} Q${(x+4).toFixed(1)} ${y.toFixed(1)} ${x.toFixed(1)} ${(y+9).toFixed(1)} Q${(x-4).toFixed(1)} ${y.toFixed(1)} ${x.toFixed(1)} ${(y-9).toFixed(1)} Z" transform="rotate(${rot} ${x.toFixed(1)} ${y.toFixed(1)})"/>`}).join('')).join('')}</g>`;
    if (r >= 6) top = `<g transform="translate(0 -70)" fill="url(#${id}m)" stroke="${M[2]}" stroke-width=".8"><path d="M-22 6 L-26 -14 L-12 -4 L0 -22 L12 -4 L26 -14 L22 6 Z"/><circle cx="0" cy="-22" r="3"/><circle cx="-26" cy="-14" r="2.4"/><circle cx="26" cy="-14" r="2.4"/></g>`;
    else if (r >= 2) {
      const n = r >= 4 ? 3 : r >= 3 ? 2 : 1;
      top = `<g fill="url(#${id}m)">${Array.from({length:n},(_,i)=>`<path transform="translate(${(i-(n-1)/2)*16} -70)" d="M0 -7 L2 -2 L7 -2 L3 1.5 L4.5 7 L0 3.6 L-4.5 7 L-3 1.5 L-7 -2 L-2 -2 Z"/>`).join('')}</g>`
    }
    const t = String(cr.rib || '').toUpperCase().slice(0, 26);
    if (t) {
      const fs = Math.min(9.5, 150 / t.length).toFixed(1);
      rib = `<g transform="translate(0 ${cr.shape==='seal'?50:56})"><path d="M-70 -2 L-56 -10 L-56 12 L-70 4 L-62 1 Z M70 -2 L56 -10 L56 12 L70 4 L62 1 Z" fill="${fc?'#000':shade(c1,-.45)}"/><path d="M-58 -12 Q0 -2 58 -12 L58 10 Q0 20 -58 10 Z" fill="${fc?'#121216':c1}" stroke="url(#${id}m)" stroke-width="1.6"/><path id="${id}rp" d="M-54 3 Q0 13 54 3" fill="none"/><text style="font:900 ${fs}px Archivo,'Inter Display',Inter,sans-serif;letter-spacing:${t.length>14?1:1.6}px" fill="${fc?'#e3b84a':'#fff'}"><textPath href="#${id}rp" startOffset="50%" text-anchor="middle">${esc(t)}</textPath></text></g>`
    }
  }
  const vb = small ? '-62 -66 124 132' : '-110 -105 220 200', h = small ? s * 132 / 124 : s * 200 / 220;
  return `<svg class="crest crest2${r>=6&&!small?' crest-glow':''}" viewBox="${vb}" width="${s}" height="${h.toFixed(0)}" aria-hidden="true" style="--eye:${fc?'#08080a':cr.c1}"><defs><linearGradient id="${id}m" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${M[0]}"/><stop offset=".35" stop-color="${M[1]}"/><stop offset=".65" stop-color="${M[2]}"/><stop offset="1" stop-color="${M[3]}"/></linearGradient>${crPat(cr.pat,id+'p',c1,c2)}<linearGradient id="${id}s" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff" stop-opacity=".28"/><stop offset=".45" stop-color="#fff" stop-opacity="0"/></linearGradient><clipPath id="${id}c"><path d="${shp}"/></clipPath></defs>
 ${deco}<path d="${shp}" fill="rgba(0,0,0,.45)" transform="translate(0 4) scale(1.1)"/><path d="${shp}" fill="url(#${id}m)" transform="scale(1.1)"/><path d="${shp}" fill="${M[2]}" transform="scale(1.04)"/><path d="${shp}" fill="url(#${id}p)"/>
 ${cr.border==='rope'?`<path d="${shp}" fill="none" stroke="url(#${id}m)" stroke-width="3" stroke-dasharray="4 2" transform="scale(.9)"/>`:cr.border==='studs'?`<path d="${shp}" fill="none" stroke="url(#${id}m)" stroke-width="3.6" stroke-dasharray=".1 9" stroke-linecap="round" transform="scale(.9)"/>`:`<path d="${shp}" fill="none" stroke="url(#${id}m)" stroke-width="1.2" transform="scale(.88)"/>`}
 <g clip-path="url(#${id}c)"><path d="M-60 -70 L60 -70 L60 -10 Q0 10 -60 -10 Z" fill="url(#${id}s)"/></g>
 <g transform="translate(0 -2)" fill="${embF}" stroke="${fc?'#7a5410':M[2]}" stroke-width=".8" style="color:${M[1]}"><g transform="translate(0 2.5)" opacity=".45" fill="#000" stroke="none">${CR_EMB[cr.emb][1]}</g>${CR_EMB[cr.emb][1]}</g>${top}${rib}</svg>`
}
function metalDark(m) {return m === 'black'}
function shade(hex, amt) {
  const n = parseInt(hex.slice(1), 16);
  let r = n >> 16, g = n >> 8 & 255, b = n & 255;
  const f = v => Math.max(0, Math.min(255, Math.round(v + (amt < 0 ? v * amt : (255 - v) * amt))));
  return '#' + [f(r), f(g), f(b)].map(v => v.toString(16).padStart(2, '0')).join('')
}
function crestSheet18(sh) {
  const c = crNorm(sh.crest), R = crewRankInfo().r, fc = !!(st.cs && st.cs.crew.fcrew), k = sh.sec;
  const prev = `<div class="cr18prev">${crestSVG(c,R,190)}</div>`;
  if (!k) {
    const rows = [['shape', '🛡', 'Shape', CR_SHAPE[c.shape][0]], ['pat', '▦', 'Pattern', CR_PAT[c.pat]], ['col', '🎨', 'Colors', `<i class="sw16" style="background:${c.c1}"></i> <i class="sw16" style="background:${c.c2}"></i>`], ['emb', '🐂', 'Emblem', CR_EMB[c.emb][0]], ['metal', '⚙️', 'Metal', CR_METAL[c.metal][0]], ['border', '◎', 'Border', CR_BORDER[c.border]], ['rib', '🎗', 'Ribbon', c.rib ? esc(c.rib) : 'None']];
    return `<div class="ghd"><h2>Crew crest</h2><button class="btn sm" id="sheetx">Cancel</button></div>${prev}<p class="sub" style="text-align:center">Stars, wings, a crown and laurels get added as your crew ranks up.</p><div class="card rows15">${rows.map(([a,i,l,v])=>row16(`data-crsec="${a}"`,i,l,v)).join('')}</div><button class="btn acc wide" id="crsave18" style="min-height:52px">Save crest</button>`
  }
  const L = {
    shape: 'Shape', pat: 'Pattern', col: 'Colors', emb: 'Emblem', metal: 'Metal', border: 'Border', rib: 'Ribbon'
  }[k];
  const head = `<div class="ghd"><button class="btn sm" id="cr18back">‹ Back</button><h2 style="margin:0">${L}</h2><button class="btn sm acc" id="cr18back2">Done</button></div><div class="cr18prev sm">${crestSVG(c,R,130)}</div>`;
  const tile = (key, val, label, lock) => `<button class="cr18t${c[key]===val?' on':''}${lock?' lk':''}" data-cr="${key}:${val}"${lock?` data-crlock="${esc(lock)}"`:''}>${crestSVG({...c,[key]:val,rib:''},0,54)}<span>${label}${lock?' 🔒':''}</span></button>`;
  let body = '';
  if (k === 'shape') body = Object.entries(CR_SHAPE).map(([v, x]) => tile('shape', v, x[0])).join('');
  else if (k === 'pat') body = Object.entries(CR_PAT).map(([v, x]) => tile('pat', v, x)).join('');
  else if (k === 'emb') body = Object.entries(CR_EMB).map(([v, x]) => tile('emb', v, x[0])).join('');
  else if (k === 'border') body = Object.entries(CR_BORDER).map(([v, x]) => tile('border', v, x)).join('');
  else if (k === 'metal') body = Object.entries(CR_METAL).filter(([v]) => v !== 'founders' || fc).map(([v, x]) => {
    const req = x[2], lock = req === 'f' ? (fc ? '' : 'Founder crew only') : (R >= req ? '' : RANKS[req][0] + ' crew rank');
    return tile('metal', v, x[0], lock)
  }).join('');
  else if (k === 'col') return head + `<div class="glbl">MAIN COLOR</div><div class="lkrow lk16">${AV_COLS.concat(['#14161c','#f2f3f6']).map(x=>`<button class="lkc${c.c1===x?' on':''}" data-cr="c1:${x}"><i style="background:${x}"></i></button>`).join('')}</div><div class="glbl">SECOND COLOR</div><div class="lkrow lk16">${AV_COLS.concat(['#14161c','#f2f3f6']).map(x=>`<button class="lkc${c.c2===x?' on':''}" data-cr="c2:${x}"><i style="background:${x}"></i></button>`).join('')}</div>`;
  else if (k === 'rib') return head + `<input class="field" id="crrib" maxlength="26" placeholder="${esc((st.cs.crew.name||'').toUpperCase().slice(0,26))}" value="${esc(c.rib||'')}"><p class="sub">Your crew name or motto, up to 26 letters. Leave empty for no ribbon.</p>`;
  return head + `<div class="cr18g">${body}</div>`
}/* ---------- co-founder ---------- */
const SILVER = [['#fff6cf', '#ffffff'], ['#e8c55c', '#d5dae2'], ['#9c7424', '#7d838e'], ['#f3d77a', '#eef1f5'], ['#fff3c0', '#ffffff'], ['#d9b04a', '#b9c0cb'], ['#6e5014', '#5d636d'], ['#5a410f', '#4a4f57'], ['#7a5a18', '#6b717b'], ['#fff2bd', '#ffffff'], ['#4a3510', '#2c3038'], ['#16110a', '#101216'], ['#e3c25a', '#cfd4dc']];
function silverize(svg, pairs) {
  let o = svg;
  (pairs || SILVER).forEach(([a, b]) => {o = o.split(a).join(b)});
  return o
}
function cofounderPlate(o) {
  return silverize(founderPlate(o)).replace(/FOUNDER · 001/g, 'CO-FOUNDER · 002').replace(/>001</g, '>002<')
}
{
  const _p = plateSVG;
  plateSVG = function(c, state, o) {
    if (c === 'cofounder' && state !== 'idle' && state !== 'rest') return cofounderPlate(o);
    return _p(c, state, o)
  }
}
XSC.cofounder = ['Co-Founder', 'Co-founder only'];
/* ---------- the founder's own banners (different from the crew banner) ---------- */
Object.assign(XSC, {
  vault: ['The Vault', 'Founder only'], fblue: ['The Blueprint', 'Founder only'], throne: ['The Throne', 'Founder only']
});
{
  const _x = xScene;
  xScene = function(k, uid) {
    if (k === 'cofounder') return silverize(_x('founder', uid)).replace(/FOUNDING CREW · EST 2026 · OVERLOAD · FOUNDER 001 ·/, 'CO-FOUNDER · OVERLOAD · EST 2026 · 002 ·').replace(/FOUNDER · 001 · THE ORIGINAL · OVERLOAD · FOUNDER · 001/, 'CO-FOUNDER · 002 · OVERLOAD · CO-FOUNDER · 002');
    if (k === 'vault' || k === 'fblue' || k === 'throne') return founderScene(k, uid);
    return _x(k, uid)
  }
}
function founderScene(k, uid) {
  const u = (uid || 'f' + (pid++)).replace(/[^\w-]/g, ''), R = (n, a, b) => a + (hashS(u + n) % 1000) / 1000 * (b - a);
  let o = `<defs><linearGradient id="${u}g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fff2bd"/><stop offset=".4" stop-color="#e8c55c"/><stop offset=".75" stop-color="#8a6a1f"/><stop offset="1" stop-color="#f3d77a"/></linearGradient><radialGradient id="${u}r" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#f3d77a" stop-opacity=".55"/><stop offset="1" stop-color="#f3d77a" stop-opacity="0"/></radialGradient></defs><rect width="380" height="130" fill="#060606"/>`;
  if (k === 'vault') {
    const bolts = Array.from({length: 10}, (_, i) => {
      const a = i * Math.PI / 5;
      return `<rect x="-5" y="-71" width="10" height="16" rx="3" transform="rotate(${i*36})" fill="url(#${u}g)" stroke="#5a410f" stroke-width=".6"/>`
    }).join('');
    o += `<g opacity=".18" stroke="#e8c55c" stroke-width=".5">${Array.from({length:16},(_,i)=>`<path d="M0 ${i*9} H190"/>`).join('')}</g>
  <circle cx="290" cy="65" r="110" fill="url(#${u}r)"><animate attributeName="opacity" values=".55;1;.55" dur="4s" repeatCount="indefinite"/></circle>
  <g transform="translate(290 65)"><circle r="78" fill="#0c0b09" stroke="url(#${u}g)" stroke-width="3"/><circle r="70" fill="none" stroke="#e8c55c" stroke-width=".7" stroke-dasharray="2 3"/>${bolts}<circle r="54" fill="#121009" stroke="url(#${u}g)" stroke-width="2"/>${Array.from({length:24},(_,i)=>`<circle cx="${(48*Math.cos(i*Math.PI/12)).toFixed(1)}" cy="${(48*Math.sin(i*Math.PI/12)).toFixed(1)}" r="1.3" fill="#e8c55c"/>`).join('')}
   <g><animateTransform attributeName="transform" type="rotate" values="0;40;40;-20;-20;0" keyTimes="0;.2;.45;.65;.85;1" dur="9s" repeatCount="indefinite"/><circle r="36" fill="none" stroke="url(#${u}g)" stroke-width="4"/>${Array.from({length:6},(_,i)=>`<g transform="rotate(${i*60})"><rect x="-2.5" y="-46" width="5" height="22" rx="2" fill="url(#${u}g)"/><circle cy="-47" r="4.5" fill="url(#${u}g)"/></g>`).join('')}</g>
   <circle r="20" fill="#0a0907" stroke="url(#${u}g)" stroke-width="2"/><text y="6" text-anchor="middle" style="font:900 15px Archivo,'Inter Display',Inter,sans-serif;letter-spacing:1px" fill="url(#${u}g)">001</text></g>
  <path d="M212 65 A78 78 0 0 1 290 -13" stroke="#fff2bd" stroke-width="2" fill="none" opacity=".5"><animate attributeName="opacity" values=".1;.7;.1" dur="3s" repeatCount="indefinite"/></path>
  <text x="18" y="20" style="font:800 7px Archivo,Inter,sans-serif;letter-spacing:4px" fill="#e8c55c" opacity=".6">THE VAULT · FOUNDER 001</text>`
  } else if (k === 'fblue') {
    const L = (d, len, dl) => `<path d="${d}" stroke="#e8c55c" stroke-width="1.2" fill="none" stroke-dasharray="${len}" stroke-dashoffset="${len}"><animate attributeName="stroke-dashoffset" values="${len};0;0;${len}" keyTimes="0;.45;.9;1" dur="10s" begin="${dl}s" repeatCount="indefinite"/></path>`;
    o += `<g stroke="#e8c55c" stroke-width=".4" opacity=".16">${Array.from({length:39},(_,i)=>`<path d="M${i*10} 0 V130"/>`).join('')}${Array.from({length:14},(_,i)=>`<path d="M0 ${i*10} H380"/>`).join('')}</g>
  ${L('M290 65 m-52 0 a52 52 0 1 1 104 0 a52 52 0 1 1 -104 0',330,0)}${L('M290 65 m-40 0 a40 40 0 1 1 80 0 a40 40 0 1 1 -80 0',252,.4)}${L('M290 65 m-14 0 a14 14 0 1 1 28 0 a14 14 0 1 1 -28 0',90,.8)}
  ${L('M120 65 H238',118,1)}${L('M342 65 H372',30,1.2)}${L('M238 6 V124',118,1.4)}${L('M342 6 V124',118,1.4)}${L('M238 10 H342',104,1.8)}${L('M120 50 V80',30,1.1)}
  <g opacity="0"><animate attributeName="opacity" values="0;0;1;1;0" keyTimes="0;.35;.45;.9;1" dur="10s" repeatCount="indefinite"/><text x="290" y="8" text-anchor="middle" style="font:700 6px Inter,sans-serif;letter-spacing:1px" fill="#e8c55c">⌀ 450 MM · 45 LB</text>
  <g transform="translate(70 92) rotate(-8)"><rect x="-52" y="-14" width="104" height="28" rx="3" fill="none" stroke="url(#${u}g)" stroke-width="2"/><text y="5" text-anchor="middle" style="font:900 12px Archivo,'Inter Display',Inter,sans-serif;letter-spacing:2px" fill="url(#${u}g)">FOUNDER 001</text></g></g>
  <text x="14" y="18" style="font:700 7px Inter,sans-serif;letter-spacing:3px" fill="#e8c55c" opacity=".6">ORIGINAL SPEC · OVERLOAD · 2026</text>`
  } else if (k === 'throne') {
    const dust = Array.from({length: 22}, (_, i) => {
      const x = R('x' + i, 200, 330).toFixed(0), d = R('d' + i, 4, 9).toFixed(1), b = (-R('b' + i, 0, 9)).toFixed(1);
      return `<circle cx="${x}" cy="-5" r="${R('r'+i,.5,1.6).toFixed(1)}" fill="#f3d77a"><animate attributeName="cy" values="-5;135" dur="${d}s" begin="${b}s" repeatCount="indefinite"/><animate attributeName="opacity" values="0;1;0" dur="${d}s" begin="${b}s" repeatCount="indefinite"/></circle>`
    }).join('');
    o += `<defs><linearGradient id="${u}c" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff2bd" stop-opacity=".45"/><stop offset="1" stop-color="#fff2bd" stop-opacity="0"/></linearGradient></defs>
  <path d="M248 -10 L282 -10 L340 130 L190 130 Z" fill="url(#${u}c)"><animate attributeName="opacity" values=".7;1;.7" dur="5s" repeatCount="indefinite"/></path>${dust}
  <g transform="translate(265 112)">${[0,1,2].map(i=>`<ellipse cy="${-i*9}" rx="${46-i*6}" ry="7" fill="#15130e" stroke="url(#${u}g)" stroke-width="1.4"/>`).join('')}</g>
  <g transform="translate(265 58)"><animateTransform attributeName="transform" type="translate" values="265 60;265 54;265 60" dur="4s" repeatCount="indefinite"/><path d="M-30 14 L-36 -16 L-17 -2 L0 -28 L17 -2 L36 -16 L30 14 Z" fill="url(#${u}g)" stroke="#6e5014" stroke-width="1"/><rect x="-30" y="15" width="60" height="7" rx="3" fill="url(#${u}g)"/><circle cy="-28" r="4" fill="#fff6cf"/><circle cx="-36" cy="-16" r="3" fill="#fff6cf"/><circle cx="36" cy="-16" r="3" fill="#fff6cf"/><circle cy="4" r="3.5" fill="#c8202b"/></g>
  <rect y="126" width="380" height="4" fill="url(#${u}g)" opacity=".7"/><text x="18" y="22" style="font:900 9px Archivo,'Inter Display',Inter,sans-serif;letter-spacing:6px" fill="url(#${u}g)">THE ORIGINAL</text>`
  }
  return `<svg class="scene" viewBox="0 0 380 130" preserveAspectRatio="xMidYMid slice" aria-hidden="true">${o}</svg>`
}
{
  const _r = renderProfile16;
  renderProfile16 = function() {
    let h = _r();
    if (st.set.cofounder && !st.set.founder) h = h.replace('<div class="ph16tags">', '<div class="ph16tags"><span class="tg tg-cf">Co-Founder #002</span>').replace('<h2 class="ph16n ', '<h2 class="ph16n nf-cofounder ');
    return h
  }
}
{
  const _pp = profPage;
  profPage = function(id) {
    let h = _pp(id);
    if (h == null) return h;
    const m = memberData(id) || {}, p = m.profile || {};
    if (p.cofounder && !p.founder) h = h.replace('<div class="pptags">', '<div class="pptags"><span class="tg tg-cf">Co-Founder #002</span>').replace(/<div class="ppid"><h2 class="/, '<div class="ppid"><h2 class="nf-cofounder ');
    if (id !== myId()) h += `<div class="pp18act"><button class="btn sm" id="rep18" data-mid="${esc(id)}">Report</button><button class="btn sm" id="blk18" data-mid="${esc(id)}">${(st.set.blocked||[]).includes(id)?'Unblock':'Block'}</button></div>`;
    return h
  }
}
/* set-complete burst */
{
  const _f = setFX;
  setFX = function(el) {
    const k = look('setfx');
    if (!/^h[a-z0-9]+$/.test(k) || !HOL19[k.slice(1)] || !el) return _f(el);
    const H = HOL19[k.slice(1)], r = el.getBoundingClientRect(), fx = document.createElement('div');
    fx.className = 'sfx sfx-hol';
    fx.style.left = (r.left + r.width / 2) + 'px';
    fx.style.top = (r.top + r.height / 2) + 'px';
    fx.innerHTML = Array.from({length: 9}, (_, i) => `<i style="--a:${i*40}deg">${H.fx[i%H.fx.length]}</i>`).join('');
    document.body.appendChild(fx);
    setTimeout(() => fx.remove(), 1000)
  }
}

/* ---------- space founder banners ---------- */
function spaceKit(u) {
  let sd = hashS(u) || 7;
  const rnd = () => {
    sd = (sd * 16807) % 2147483647;
    return (sd - 1) / 2147483646
  }, R = (a, b) => a + rnd() * (b - a);
  const layer = (n, rmin, rmax, drift, cols) => {
    let o = '';
    for (let i = 0; i < n; i++) {
      const x = R(-20, 400).toFixed(1), y = R(0, 130).toFixed(1), r = R(rmin, rmax).toFixed(2), d = R(1.6, 5.5).toFixed(1), b = R(0, 5).toFixed(1);
      o += `<circle cx="${x}" cy="${y}" r="${r}" fill="${cols[i%cols.length]}"><animate attributeName="opacity" values="${R(.25,.55).toFixed(2)};1;${R(.25,.55).toFixed(2)}" dur="${d}s" begin="-${b}s" repeatCount="indefinite"/></circle>`
    }
    return `<g><animateTransform attributeName="transform" type="translate" values="0 0;${-drift} 0;0 0" dur="${(60+drift*4)}s" repeatCount="indefinite"/>${o}</g>`
  };
  const stars = () => layer(90, .2, .6, 4, ['#ffffff', '#d6dcff', '#ffe9b8']) + layer(45, .5, 1, 9, ['#ffffff', '#ffe7a8']) + layer(14, .9, 1.5, 16, ['#fff8e6', '#ffd77a']);
  const flare = (x, y, s, c, d) => `<g transform="translate(${x} ${y})"><circle r="${3*s}" fill="${c}" opacity=".25"/><circle r="${1.1*s}" fill="#fffdf2"/><path d="M0 -${7*s} L${.45*s} 0 L0 ${7*s} L-${.45*s} 0Z M-${7*s} 0 L0 ${.45*s} L${7*s} 0 L0 -${.45*s}Z" fill="#fffdf2" opacity=".85"><animate attributeName="opacity" values=".4;1;.4" dur="${d}s" repeatCount="indefinite"/></path></g>`;
  const neb = (n, cols, cx, cy, spread, op) => {
    let o = '';
    for (let i = 0; i < n; i++) o += `<ellipse cx="${(cx+R(-spread,spread)).toFixed(0)}" cy="${(cy+R(-40,40)).toFixed(0)}" rx="${R(40,120).toFixed(0)}" ry="${R(14,40).toFixed(0)}" fill="${cols[i%cols.length]}" opacity="${R(op*.6,op).toFixed(2)}" transform="rotate(${R(-30,30).toFixed(0)} 190 65)"/>`;
    return o
  };
  const plate = (r, id) => {
    let bolts = '';
    for (let i = 0; i < 8; i++) {
      const a = i * Math.PI / 4 + Math.PI / 8;
      bolts += `<circle cx="${(r*.62*Math.cos(a)).toFixed(2)}" cy="${(r*.62*Math.sin(a)).toFixed(2)}" r="${(r*.045).toFixed(2)}" fill="#6e5014"/><circle cx="${(r*.62*Math.cos(a)-r*.012).toFixed(2)}" cy="${(r*.62*Math.sin(a)-r*.012).toFixed(2)}" r="${(r*.018).toFixed(2)}" fill="#fff6cf"/>`
    }
    return `<circle r="${r}" fill="url(#${id}pl)"/><circle r="${r}" fill="none" stroke="url(#${id}g)" stroke-width="${(r*.06).toFixed(2)}"/><circle r="${(r*.86).toFixed(2)}" fill="none" stroke="url(#${id}g)" stroke-width="${(r*.015).toFixed(2)}" opacity=".7"/><circle r="${(r*.76).toFixed(2)}" fill="none" stroke="#e8c55c" stroke-width="${(r*.012).toFixed(2)}" stroke-dasharray="${(r*.02).toFixed(2)} ${(r*.06).toFixed(2)}" opacity=".6"/>${bolts}<circle r="${(r*.42).toFixed(2)}" fill="url(#${id}hub)"/><circle r="${(r*.42).toFixed(2)}" fill="none" stroke="#5a410f" stroke-width="${(r*.015).toFixed(2)}"/><circle r="${(r*.2).toFixed(2)}" fill="#030303" stroke="url(#${id}g)" stroke-width="${(r*.035).toFixed(2)}"/><text y="${(-r*.5).toFixed(2)}" text-anchor="middle" style="font:900 ${(r*.17).toFixed(2)}px Archivo,'Inter Display',Inter,sans-serif;letter-spacing:${(r*.03).toFixed(2)}px" fill="url(#${id}g)">001</text><path d="M${(-r*.7).toFixed(1)} ${(-r*.35).toFixed(1)} A${(r*.78).toFixed(1)} ${(r*.78).toFixed(1)} 0 0 1 ${(-r*.1).toFixed(1)} ${(-r*.77).toFixed(1)}" stroke="#fff" stroke-width="${(r*.05).toFixed(2)}" stroke-linecap="round" fill="none" opacity=".18"/>`
  };
  const defs = id => `<linearGradient id="${id}g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fff4c4"/><stop offset=".35" stop-color="#e8c55c"/><stop offset=".7" stop-color="#8a6a1f"/><stop offset="1" stop-color="#ffe08a"/></linearGradient><radialGradient id="${id}pl" cx="40%" cy="35%" r="75%"><stop offset="0" stop-color="#26241f"/><stop offset=".7" stop-color="#0b0a08"/><stop offset="1" stop-color="#020202"/></radialGradient><radialGradient id="${id}hub" cx="38%" cy="32%" r="72%"><stop offset="0" stop-color="#fff3c0"/><stop offset=".5" stop-color="#d9b04a"/><stop offset="1" stop-color="#6e5014"/></radialGradient><filter id="${id}b1"><feGaussianBlur stdDeviation="2.2"/></filter><filter id="${id}b2"><feGaussianBlur stdDeviation="9"/></filter><filter id="${id}b3"><feGaussianBlur stdDeviation="16"/></filter>`;
  return {R, stars, flare, neb, plate, defs}
}
function spaceScene(k, uid) {
  const u = ('sp' + k + (uid || pid++)).replace(/[^\w-]/g, ''), K = spaceKit(u), {R} = K;
  let o = `<defs>${K.defs(u)}`;
  if (k === 'feclipse') {
    const cx = 272, cy = 64;
    let rays = '';
    for (let i = 0; i < 48; i++) {
      const len = R(60, 150);
      rays += `<path d="M0 -40 L${R(.8,2.6).toFixed(1)} -${len.toFixed(0)} L-${R(.8,2.6).toFixed(1)} -${len.toFixed(0)} Z" fill="#ffd77a" opacity="${R(.04,.2).toFixed(2)}" transform="rotate(${(i*7.5+R(-2,2)).toFixed(1)})"/>`
    }
    let rays2 = '';
    for (let i = 0; i < 24; i++) {
      const len = R(50, 95);
      rays2 += `<path d="M0 -40 Q${R(-6,6).toFixed(0)} -${(len*.6).toFixed(0)} 0 -${len.toFixed(0)}" stroke="#fff0c0" stroke-width="${R(.4,1.2).toFixed(1)}" fill="none" opacity="${R(.08,.25).toFixed(2)}" transform="rotate(${(i*15).toFixed(0)})"/>`
    }
    let prom = '';
    for (let i = 0; i < 7; i++) {
      const a = R(0, 360), h = R(8, 18);
      prom += `<path d="M-4 -40 C-6 -${(40+h).toFixed(0)} 6 -${(40+h).toFixed(0)} 4 -40" stroke="#ff9a2a" stroke-width="${R(1.2,2.6).toFixed(1)}" fill="none" transform="rotate(${a.toFixed(0)})"><animate attributeName="opacity" values="0;.9;0" dur="${R(3,7).toFixed(1)}s" begin="-${R(0,7).toFixed(1)}s" repeatCount="indefinite"/></path>`
    }
    let dust = '';
    for (let i = 0; i < 34; i++) {
      const x = R(120, 390), y = R(0, 130), d = R(7, 16).toFixed(1);
      dust += `<circle cx="${x.toFixed(0)}" cy="${y.toFixed(0)}" r="${R(.4,1.3).toFixed(1)}" fill="#ffe7a0"><animateTransform attributeName="transform" type="translate" values="0 0;${R(-40,-12).toFixed(0)} ${R(-12,12).toFixed(0)}" dur="${d}s" begin="-${R(0,10).toFixed(1)}s" repeatCount="indefinite"/><animate attributeName="opacity" values="0;.9;0" dur="${d}s" begin="-${R(0,10).toFixed(1)}s" repeatCount="indefinite"/></circle>`
    }
    o += `<radialGradient id="${u}bg" cx="72%" cy="50%" r="80%"><stop offset="0" stop-color="#3d2708"/><stop offset=".32" stop-color="#140c04"/><stop offset="1" stop-color="#010102"/></radialGradient><radialGradient id="${u}c" cx="50%" cy="50%" r="50%"><stop offset=".52" stop-color="#fff6d0"/><stop offset=".6" stop-color="#ffc94a" stop-opacity=".95"/><stop offset=".74" stop-color="#e08a12" stop-opacity=".35"/><stop offset="1" stop-color="#e08a12" stop-opacity="0"/></radialGradient></defs>
  <rect width="380" height="130" fill="url(#${u}bg)"/>${K.neb(5,['#5a3608','#2a1a40','#7a4e10'],260,65,140,.35).replace(/<ellipse/g,`<ellipse filter="url(#${u}b3)"`)}${K.stars()}${K.flare(48,26,1,'#ffd77a',3.4)}${K.flare(150,104,.7,'#cfd8ff',4.6)}
  <g transform="translate(${cx} ${cy})"><g><animateTransform attributeName="transform" type="rotate" values="0;360" dur="140s" repeatCount="indefinite"/>${rays}</g><g><animateTransform attributeName="transform" type="rotate" values="360;0" dur="90s" repeatCount="indefinite"/>${rays2}</g>
   <circle r="78" fill="url(#${u}c)" filter="url(#${u}b3)"><animate attributeName="r" values="72;82;72" dur="6s" repeatCount="indefinite"/></circle><circle r="50" fill="url(#${u}c)" filter="url(#${u}b1)"/><g filter="url(#${u}b1)">${prom}</g>
   <circle r="41.4" fill="none" stroke="#fffbe6" stroke-width="2.4"/><g><animateTransform attributeName="transform" type="translate" values="-2.5 1;0 0;-2.5 1" dur="12s" repeatCount="indefinite"/>${K.plate(41,u)}</g>
   <circle cx="-29" cy="-29" r="3.4" fill="#fffdf0" filter="url(#${u}b1)" opacity="0"><animate attributeName="opacity" values="0;0;1;0" keyTimes="0;.86;.9;1" dur="8s" repeatCount="indefinite"/></circle><path d="M-29 -41 V-17 M-41 -29 H-17" stroke="#fffdf0" stroke-width="1.2" opacity="0"><animate attributeName="opacity" values="0;0;1;0" keyTimes="0;.86;.9;1" dur="8s" repeatCount="indefinite"/></path></g>${dust}
  <text x="16" y="20" style="font:800 7px Archivo,Inter,sans-serif;letter-spacing:4.5px" fill="url(#${u}g)" opacity=".8">TOTALITY · FOUNDER 001</text>`
  } else if (k === 'fconst') {
    const crown = [[70, 72], [62, 48], [80, 60], [94, 36], [108, 60], [126, 48], [118, 72]], pc = [288, 62], ring = Array.from({length: 12}, (_, i) => [pc[0] + 36 * Math.cos(i * Math.PI / 6 - Math.PI / 2), pc[1] + 36 * Math.sin(i * Math.PI / 6 - Math.PI / 2)]), inner = Array.from({length: 6}, (_, i) => [pc[0] + 16 * Math.cos(i * Math.PI / 3), pc[1] + 16 * Math.sin(i * Math.PI / 3)]);
    const T = 16, line = (a, b, t0) => {
      const len = Math.hypot(b[0] - a[0], b[1] - a[1]).toFixed(1), k1 = (t0 / T).toFixed(3), k2 = ((t0 + .5) / T).toFixed(3);
      return `<path d="M${a[0].toFixed(1)} ${a[1].toFixed(1)} L${b[0].toFixed(1)} ${b[1].toFixed(1)}" stroke="#ffd77a" stroke-width=".9" stroke-dasharray="${len}" stroke-dashoffset="${len}" stroke-linecap="round"><animate attributeName="stroke-dashoffset" values="${len};${len};0;0;${len}" keyTimes="0;${k1};${k2};.93;1" dur="${T}s" repeatCount="indefinite"/><animate attributeName="opacity" values=".55;1;.55" dur="2.4s" repeatCount="indefinite"/></path>`
    };
    let L = '';
    [0, 1, 2, 3, 4, 5, 6, 0].forEach((v, i, a) => {if (i < a.length - 1) L += line(crown[v], crown[a[i + 1]], .6 + i * .35)});
    ring.forEach((p, i) => L += line(p, ring[(i + 1) % 12], 3.6 + i * .28));
    inner.forEach((p, i) => L += line(p, inner[(i + 1) % 6], 7.2 + i * .25));
    for (let i = 0; i < 6; i++) L += line(inner[i], ring[i * 2], 9 + i * .22);
    const node = (p, s, t0) => `<g transform="translate(${p[0].toFixed(1)} ${p[1].toFixed(1)})" opacity=".35"><animate attributeName="opacity" values=".35;.35;1;1;.35" keyTimes="0;${(t0/T).toFixed(3)};${((t0+.3)/T).toFixed(3)};.93;1" dur="${T}s" repeatCount="indefinite"/><circle r="${5*s}" fill="#ffd77a" opacity=".2"/><circle r="${1.6*s}" fill="#fff8e0"/><path d="M0 -${5.5*s} L.45 0 L0 ${5.5*s} L-.45 0Z M-${5.5*s} 0 L0 .45 L${5.5*s} 0 L0 -.45Z" fill="#fff8e0" opacity=".8"/></g>`;
    const nodes = crown.map((p, i) => node(p, i === 3 ? 1.3 : 1, .6 + i * .35)).join('') + ring.map((p, i) => node(p, .85, 3.6 + i * .28)).join('') + inner.map((p, i) => node(p, .7, 7.2 + i * .25)).join('') + node(pc, 1.5, 10.4);
    const shoot = (t0, x0, y0, x1, y1, len) => `<g opacity="0"><animate attributeName="opacity" values="0;0;1;0;0" keyTimes="0;${(t0/T).toFixed(3)};${((t0+.2)/T).toFixed(3)};${((t0+1.1)/T).toFixed(3)};1" dur="${T}s" repeatCount="indefinite"/><g><animateTransform attributeName="transform" type="translate" values="${x0} ${y0};${x0} ${y0};${x1} ${y1};${x1} ${y1}" keyTimes="0;${(t0/T).toFixed(3)};${((t0+1.1)/T).toFixed(3)};1" dur="${T}s" repeatCount="indefinite"/><path d="M0 0 L-${len} -${(len*.28).toFixed(0)}" stroke="url(#${u}ss)" stroke-width="1.5" stroke-linecap="round"/><circle r="1.4" fill="#fffdf0"/></g></g>`;
    o += `<linearGradient id="${u}ss" x1="1" y1="1" x2="0" y2="0"><stop offset="0" stop-color="#fff6d6"/><stop offset="1" stop-color="#fff6d6" stop-opacity="0"/></linearGradient></defs><rect width="380" height="130" fill="#02030a"/>
  <g filter="url(#${u}b3)"><g><animateTransform attributeName="transform" type="rotate" values="0 200 65;8 200 65;0 200 65" dur="40s" repeatCount="indefinite"/>${K.neb(8,['#8a5a12','#4a2f7a','#b8862b','#22164a','#6a3a10'],200,65,190,.42)}</g></g>
  <path d="M-10 100 Q120 60 240 70 T400 30" stroke="#ffe7b0" stroke-width="22" fill="none" opacity=".05" filter="url(#${u}b2)"/>${K.stars()}${K.flare(210,22,1,'#cfd8ff',3.8)}${K.flare(168,110,.8,'#ffd77a',4.4)}
  <g filter="url(#${u}b1)" opacity=".6">${L}</g>${L}${nodes}
  <text x="${pc[0]}" y="${pc[1]+3}" text-anchor="middle" style="font:900 8.5px Archivo,'Inter Display',Inter,sans-serif;letter-spacing:1.5px" fill="url(#${u}g)" opacity="0">001<animate attributeName="opacity" values="0;0;1;1;0" keyTimes="0;.68;.72;.93;1" dur="${T}s" repeatCount="indefinite"/></text>
  ${shoot(2.4,-40,8,420,120,70)}${shoot(12.2,60,-10,330,110,55)}
  <text x="16" y="20" style="font:800 7px Archivo,Inter,sans-serif;letter-spacing:4.5px" fill="url(#${u}g)" opacity=".8">CONSTELLATION · FOUNDER 001</text>`
  } else {
    const cx = 270, cy = 64;
    let arms = '', glow = '';
    for (let arm = 0; arm < 4; arm++) {
      for (let i = 0; i < 120; i++) {
        const t = i / 120, a = arm * Math.PI / 2 + t * 5.4, r = 10 + t * 150, x = r * Math.cos(a) + R(-7, 7) * t * 2, y = r * Math.sin(a) * .46 + R(-4, 4) * t;
        arms += `<circle cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="${(R(.35,1.7)*(1.15-t*.6)).toFixed(2)}" fill="${t<.3?'#fffaf0':t<.6?'#f3d77a':t<.85?'#c99a35':'#8aa0d6'}" opacity="${(.98-t*.5).toFixed(2)}"/>`
      }
      for (let i = 0; i < 10; i++) {
        const t = i / 10, a = arm * Math.PI / 2 + t * 5.4, r = 14 + t * 140;
        glow += `<circle cx="${(r*Math.cos(a)).toFixed(1)}" cy="${(r*Math.sin(a)*.46).toFixed(1)}" r="${(24-t*12).toFixed(1)}" fill="${i%3?'#c99a35':'#5a3a8a'}" opacity=".14"/>`
      }
    }
    const planet = (rx, ry, dur, r, c1, c2, ring, begin) => `<g><animateMotion dur="${dur}s" begin="-${begin}s" repeatCount="indefinite" path="M${rx} 0 A${rx} ${ry} 0 1 1 -${rx} 0 A${rx} ${ry} 0 1 1 ${rx} 0"/>${ring?`<ellipse rx="${r*2.2}" ry="${r*.55}" fill="none" stroke="#e8c55c" stroke-width="1" opacity=".8" transform="rotate(-14)"/>`:''}<circle r="${r}" fill="${c1}"/><circle r="${r}" fill="url(#${u}ps)"/><path d="M-${r} 0 A${r} ${r*.3} 0 0 0 ${r} 0" stroke="${c2}" stroke-width="${r*.25}" fill="none" opacity=".5"/>${ring?`<path d="M-${r*2.2} 0 A${r*2.2} ${r*.55} 0 0 0 ${r*2.2} 0" stroke="#ffe9a8" stroke-width="1" fill="none" transform="rotate(-14)"/>`:''}</g>`;
    o += `<radialGradient id="${u}core" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#fffbe8"/><stop offset=".3" stop-color="#ffd77a" stop-opacity=".9"/><stop offset="1" stop-color="#ffd77a" stop-opacity="0"/></radialGradient><radialGradient id="${u}ps" cx="32%" cy="30%" r="75%"><stop offset="0" stop-color="#fff" stop-opacity=".4"/><stop offset=".6" stop-color="#000" stop-opacity=".1"/><stop offset="1" stop-color="#000" stop-opacity=".65"/></radialGradient></defs>
  <rect width="380" height="130" fill="#020207"/>${K.neb(4,['#2a1a4a','#3d2a08'],120,60,140,.4).replace(/<ellipse/g,`<ellipse filter="url(#${u}b3)"`)}${K.stars()}${K.flare(40,22,1,'#cfd8ff',4)}${K.flare(120,108,.8,'#ffd77a',3.2)}
  <g transform="translate(${cx} ${cy}) rotate(-16)"><g filter="url(#${u}b2)">${glow}</g><g><animateTransform attributeName="transform" type="rotate" values="0;360" dur="160s" repeatCount="indefinite"/><g filter="url(#${u}b1)" opacity=".55">${arms}</g>${arms}</g>
   <ellipse rx="62" ry="26" fill="url(#${u}core)" filter="url(#${u}b2)"/><ellipse rx="26" ry="12" fill="url(#${u}core)"/>
   <g transform="rotate(16)">${K.plate(17,u)}<circle r="17" fill="none" stroke="#fff6d6" stroke-width="1"><animate attributeName="r" values="17;34" dur="4.5s" repeatCount="indefinite"/><animate attributeName="opacity" values=".6;0" dur="4.5s" repeatCount="indefinite"/></circle></g>
   ${planet(84,36,26,5,'#c9873a','#7a4a1a',1,4)}${planet(124,52,44,3.2,'#8c95b8','#4a5270',0,22)}${planet(58,24,16,2.3,'#e8c55c','#8a6a1f',0,8)}</g>
  <g opacity="0"><animate attributeName="opacity" values="0;0;1;1;0" keyTimes="0;.55;.58;.72;.76" dur="13s" repeatCount="indefinite"/><g><animateTransform attributeName="transform" type="translate" values="-40 120;420 -10" dur="13s" repeatCount="indefinite"/><circle r="2.6" fill="#fff8e0"/><path d="M0 0 L-44 13" stroke="#ffe7a0" stroke-width="1.8" stroke-linecap="round" opacity=".7"/><path d="M0 0 L-30 4" stroke="#cfe0ff" stroke-width="1" stroke-linecap="round" opacity=".5"/></g></g>
  <text x="16" y="20" style="font:800 7px Archivo,Inter,sans-serif;letter-spacing:4.5px" fill="url(#${u}g)" opacity=".8">GALAXY 001 · THE ORIGIN</text>`
  }
  return `<svg class="scene" viewBox="0 0 380 130" preserveAspectRatio="xMidYMid slice" aria-hidden="true">${o}</svg>`
}
const OLDF20 = {vault: 'feclipse', fblue: 'fconst', throne: 'fgalaxy'};
FONLY18.length = 0;
FONLY18.push('feclipse', 'fconst', 'fgalaxy', 'cofounder', 'founder');myBanner = function() {
  let k = look('pbanner');
  if (OLDF20[k]) k = OLDF20[k];
  if (k !== 'none') return k;
  return st.set.founder ? 'feclipse' : st.set.cofounder ? 'cofounder' : ''
};
/* ---------- profile header: centered name, one info line, small pins, icon buttons ---------- */
{
  const _r = renderProfile16;
  renderProfile16 = function() {
    const full = _r(), cut = full.indexOf('<div class="tiles four life16">');
    if (cut < 0) return full;
    const E = engine(), ban = myBanner(), nm = st.set.name || 'Lifter', P = st.set.prof || {}, show = (st.set.showcase || []).map(k => E.B.find(x => x.k === k) || E.R.find(x => x.k === k)).filter(Boolean), F = !!st.set.founder, CF = !F && !!st.set.cofounder;
    const nfx = [nfxClass({profile: {nameFx: look('nameFx')}}), F && look('nameFx') === 'none' ? 'nf-founder' : '', CF && look('nameFx') === 'none' ? 'nf-cofounder' : ''].filter(Boolean).join(' ');
    const meta = [F ? '<b class="m20f">Founder #001</b>' : CF ? '<b class="m20cf">Co-Founder #002</b>' : '', esc(E.rname), '🔥 ' + E.streak + ' wk'].filter(Boolean).join('<i>·</i>');
    const head = `<div class="card ph16 ph20">${ban?`<div class="ph16ban">${sceneSVG(ban,8,'me'+ban)}</div>`:'<div class="ph16ban ph16plain"></div>'}
 <div class="ph20btns"><button class="ic20" id="profedit2" aria-label="Profile settings"><svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><circle cx="12" cy="12" r="6.6"/><path d="M12 2.8v2.4M12 18.8v2.4M2.8 12h2.4M18.8 12h2.4M5.5 5.5l1.7 1.7M16.8 16.8l1.7 1.7M5.5 18.5l1.7-1.7M16.8 7.2l1.7-1.7"/></svg></button><button class="ic20" id="shareprof" aria-label="Share"><svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 15V3M7 8l5-5 5 5"/><path d="M5 13v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6"/></svg></button></div>
 <div class="ph16row"><span class="ph16av">${avatar({id:myId(),name:nm,profile:{av:st.set.av||{}}},76)}</span><div class="ph16emb">${emblem(E.rank,E.rank>=6?'spin':'')}</div></div>
 <div class="ph20name">${F?'<span class="ph20cr">👑</span>':''}<h2 class="ph16n ${nfx}">${esc(nm)}</h2>${F?'<span class="ph20cr ph20bal">👑</span>':''}</div>
 <div class="ph20meta">${meta}</div>${P.motto?`<p class="ph16m">“${esc(P.motto)}”</p>`:''}
 <div class="ph20pins">${show.map(b=>`<button class="pin20" data-bdg="${b.k}" aria-label="${esc(b.name)}" title="${esc(b.name)}">${b.icon}</button>`).join('')}${show.length<3?`<button class="pin20 add" id="pinpick" aria-label="Pin a badge">＋</button>`:''}</div></div>`;
    return head + full.slice(cut)
  }
}

/* edit profile: personal stuff only, plate + banner live on the profile itself */
PE16.push(['plate21', '💿', 'Plate and avatar']);
{
  const _p = profEdit16;
  profEdit16 = function(sh) {
    if (!sh.sec) {
      const rows = PE16.filter(([k]) => !PEHIDE21.includes(k) && (k !== 'jersey' || st.cs)).map(([k, i, l]) => row16(`data-pesec="${k}"`, i, l, pe16Val(sh, k))).join('');
      return `<div class="ghd"><h2>Edit profile</h2><button class="btn sm" id="sheetx">Cancel</button></div><button class="pe21av" data-pesec="plate21" aria-label="Change plate and avatar">${avatar({id:myId(),name:st.set.name,profile:{av:sh.av||{}}},64)}<span>Change plate</span></button><div class="card rows15 pe16">${rows}</div><button class="btn acc wide" id="profsave" style="min-height:52px;margin-top:6px">Save profile</button><p class="sub" style="text-align:center;margin-top:10px">Tip: tap your banner or plate on your profile to change them.</p>`
    }
    if (sh.sec === 'plate21') {
      const av = sh.av || {}, P = sh.p, E = eng14(), ownPl = Object.keys(PC).filter(x => !plateLocked(x) && finOwned(x, E)), fav = ownPl.filter(x => FIN[x]);
      const head = `<div class="ghd">${sh.direct?'<span></span>':'<button class="btn sm" id="pe16back">‹ Back</button>'}<h2 style="margin:0">Plate and avatar</h2><button class="btn sm acc" id="pe16back2">Done</button></div>`;
      return head + `<div class="pe16sec"><div style="text-align:center;margin:6px 0 10px">${avatar({id:myId(),name:st.set.name,profile:{av}},84)}</div>
  ${ownPl.length?`<div class="glbl">USE A PLATE</div><div class="lkrow lk16">${ownPl.map(x=>`<button class="lkc${av.t==='plate'&&av.v===x?' on':''}" data-av="plate:${x}"><span class="lki2">${plateSVG(x,'done')}</span></button>`).join('')}</div>`:''}
  <div class="glbl">OR AN ICON</div><div class="lkrow lk16"><button class="lkc${!av.t?' on':''}" data-av="init:">Initials</button>${AV_ICONS.map(i=>`<button class="lkc${av.t==='icon'&&av.v===i?' on':''}" data-av="icon:${i}">${i}</button>`).join('')}</div>
  <div class="glbl">ICON COLOR</div><div class="lkrow lk16">${AV_COLS.map(c=>`<button class="lkc${av.c===c?' on':''}" data-avc="${c}"><i style="background:${c}"></i></button>`).join('')}</div>
  <div class="glbl">FAVORITE PLATE</div>${fav.length?`<div class="lkrow lk16"><button class="lkc${!P.fav?' on':''}" data-pset="fav:">None</button>${fav.map(x=>`<button class="lkc${P.fav===x?' on':''}" data-pset="fav:${x}"><span class="lki2">${plateSVG(x,'done')}</span></button>`).join('')}</div>`:'<p class="sub">Unlock special plates to show one off.</p>'}
  <div class="glbl">NAME ON YOUR PLATE</div><input class="field" id="rimin" maxlength="24" placeholder="${esc((st.set.name||'Your name').toUpperCase())}" value="${esc(sh.rim||'')}"><p class="sub">Engraved around the big plate on Today.</p></div>`
    }
    let h = _p(sh);
    if (sh.direct && sh.sec === 'banner') h = h.replace('<button class="btn sm" id="pe16back">‹ Back</button>', '<span></span>');
    return h
  }
}
