/* ========================================================================
   Overload: Plates and art

   Bumper plate drawing, share card, plate finishes, themed and founder plates, background art.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

/* ---------- realistic bumper plate ---------- */
let pid = 0;
function plateSVG(color, state, opts = {}) {
  const id = 'p' + (pid++), c = PC[color] || PC.red, size = opts.big ? 240 : 100;
  const ring = opts.ring;
  const black = state === 'idle', steel = state === 'rest';
  const base = steel ? '#c9cdd4' : black ? '#25272d' : c[0], light = steel ? '#ffffff' : black ? '#4a4d55' : c[1], dark = steel ? '#7d828c' : black ? '#0d0e11' : c[2];
  let o = `<svg viewBox="0 0 100 100" aria-hidden="true"><defs>
 <radialGradient id="${id}b" cx="38%" cy="32%" r="75%"><stop offset="0" stop-color="${light}"/><stop offset=".55" stop-color="${base}"/><stop offset="1" stop-color="${dark}"/></radialGradient>
 <linearGradient id="${id}h" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fff"/><stop offset=".25" stop-color="#e4e7ec"/><stop offset=".5" stop-color="#9aa0aa"/><stop offset=".75" stop-color="#f4f6f9"/><stop offset="1" stop-color="#6f7580"/></linearGradient>
 <linearGradient id="${id}g" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff" stop-opacity=".55"/><stop offset=".5" stop-color="#fff" stop-opacity="0"/></linearGradient>
 <radialGradient id="${id}c" cx="50%" cy="40%" r="60%"><stop offset="0" stop-color="#3a3d44"/><stop offset="1" stop-color="#050506"/></radialGradient>
 </defs>`;
  if (steel) {
    o += `<circle cx="50" cy="50" r="34" fill="url(#${id}b)"/><circle cx="50" cy="50" r="33" fill="none" stroke="#fff" stroke-opacity=".7" stroke-width="1"/><circle cx="50" cy="50" r="24" fill="none" stroke="rgba(0,0,0,.18)" stroke-width="1"/><circle cx="50" cy="50" r="12" fill="url(#${id}h)"/><circle cx="50" cy="50" r="6.5" fill="url(#${id}c)"/><path d="M26 34 A30 30 0 0 1 50 20" stroke="#fff" stroke-width="2.5" stroke-linecap="round" fill="none" opacity=".8"/></svg>`;
    return o
  }
  o += `<circle cx="50" cy="50" r="48" fill="url(#${id}b)"/>`;
  {
    const F = window.__FIN;
    if (F && F[color] && !black && !steel) o += `<g>${finishOverlay(color,id)}</g>`
  }
  o += `<circle cx="50" cy="50" r="47.2" fill="none" stroke="${black?'rgba(255,255,255,.12)':'rgba(255,255,255,.35)'}" stroke-width=".8"/>`;
  o += `<circle cx="50" cy="50" r="44.5" fill="none" stroke="rgba(0,0,0,.22)" stroke-width="1.2"/>`;
  if (black && !opts.big) o += `<circle cx="50" cy="50" r="41.5" fill="none" stroke="${c[0]}" stroke-width="2.4"/>`;
  o += `<circle cx="50" cy="50" r="36" fill="none" stroke="rgba(0,0,0,.16)" stroke-width=".9"/><circle cx="50" cy="50" r="33" fill="none" stroke="rgba(255,255,255,.10)" stroke-width=".9"/>`;
  if (opts.big && color !== 'og') {
    o += `<path id="${id}t" d="M50 50 m-39 0 a39 39 0 1 1 78 0 a39 39 0 1 1 -78 0" fill="none"/><text style="font:800 6.4px Archivo,sans-serif;letter-spacing:1.6px" fill="${black?'rgba(255,255,255,.55)':'rgba(255,255,255,.85)'}"><textPath href="#${id}t" startOffset="2%">OVERLOAD · ${esc(opts.label||'')}</textPath></text><text style="font:800 6.4px Archivo,sans-serif;letter-spacing:1.6px" fill="${black?'rgba(255,255,255,.55)':'rgba(255,255,255,.85)'}"><textPath href="#${id}t" startOffset="52%">${rimLabel(opts.label)}</textPath></text>`
  }
  o += `<circle cx="50" cy="50" r="24" fill="rgba(0,0,0,.28)"/><circle cx="50" cy="50" r="22.5" fill="url(#${id}h)"/><circle cx="50" cy="50" r="22.5" fill="none" stroke="rgba(0,0,0,.35)" stroke-width=".8"/>`;
  o += `<circle cx="50" cy="50" r="16" fill="none" stroke="rgba(0,0,0,.22)" stroke-width=".8"/>`;
  for (let i = 0; i < 6; i++) {
    const a = i * Math.PI / 3 + Math.PI / 6;
    o += `<circle cx="${(50+19.3*Math.cos(a)).toFixed(2)}" cy="${(50+19.3*Math.sin(a)).toFixed(2)}" r="1.3" fill="#6f7580"/><circle cx="${(50+19.3*Math.cos(a)-.35).toFixed(2)}" cy="${(50+19.3*Math.sin(a)-.35).toFixed(2)}" r=".6" fill="#fff" opacity=".9"/>`
  }
  o += `<circle cx="50" cy="50" r="9.5" fill="url(#${id}c)"/><circle cx="50" cy="50" r="9.5" fill="none" stroke="#c9cdd4" stroke-width="1.4"/>`;
  {
    const F = window.__FIN;
    if (F && F[color] && F[color][5] && F[color][5] !== 'finish' && !black && !steel) o += `<g>${themedOverlay(color,id)}</g><circle cx="50" cy="50" r="5.5" fill="#0d0e11" stroke="#c9cdd4" stroke-width="1"/>`
  }
  o += `<path d="M14 44 A37 37 0 0 1 44 13" stroke="url(#${id}g)" stroke-width="7" stroke-linecap="round" fill="none"/>`;
  o += `<ellipse cx="41" cy="41" rx="9" ry="4" transform="rotate(-45 41 41)" fill="#fff" opacity=".45"/>`;
  if (ring !== undefined) {
    const C = 2 * Math.PI * 49;
    o += `<circle cx="50" cy="50" r="49" fill="none" stroke="rgba(255,255,255,.25)" stroke-width="2"/><circle id="bigring" cx="50" cy="50" r="49" fill="none" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-dasharray="${C.toFixed(1)}" stroke-dashoffset="${(C*(1-ring)).toFixed(1)}" transform="rotate(-90 50 50)"/>`
  }
  return o + '</svg>'
}
/* ---------- 2.0: share card ---------- */
const SHG = {
  red: ['#ff3d4a', '#ef2b36', '#ff7a3d'], blue: ['#2563ff', '#3b82f6', '#00b7ff'], yellow: ['#ff9f1c', '#ffbe0b', '#ffd23f'], green: ['#0fa958', '#16b65b', '#00c2a8'], purple: ['#6a3dff', '#7c4dff', '#d14dff'], black: ['#23252b', '#3a3d46', '#5a5e69'], pink: ['#ff3d8b', '#ff5fa2', '#ff9f6b'], teal: ['#00a896', '#00c2b2', '#3ad0ff'], gold: ['#b8860b', '#e8b923', '#ffd75e'], chrome: ['#5d636d', '#8c939e', '#c9ced6'], ember: ['#ff2d2d', '#ff6a00', '#ffc14d'], ice: ['#8be0ff', '#5ec8ff', '#e9f8ff'], toxic: ['#3ddc84', '#a8ff1a', '#f0ff6b'], galaxy: ['#1b0f4a', '#5b2bd6', '#ff4fd8']
};
function drawPlate(c, x, y, r) {
  const g = c.createRadialGradient(x - r * .25, y - r * .35, r * .1, x, y, r);
  g.addColorStop(0, '#4a4d55');
  g.addColorStop(.55, '#25272d');
  g.addColorStop(1, '#0b0c0f');
  c.fillStyle = g;
  c.beginPath();
  c.arc(x, y, r, 0, 7);
  c.fill();
  c.shadowColor = 'transparent';
  c.shadowBlur = 0;
  c.shadowOffsetY = 0;
  c.lineWidth = r * .045;
  const rg = c.createLinearGradient(x - r, y - r, x + r, y + r);
  rg.addColorStop(0, '#ffffff');
  rg.addColorStop(.45, '#7fd3ff');
  rg.addColorStop(1, '#2563ff');
  c.strokeStyle = rg;
  c.shadowColor = '#3b9bff';
  c.shadowBlur = r * .12;
  c.beginPath();
  c.arc(x, y, r * .95, 0, 7);
  c.stroke();
  c.shadowBlur = 0;
  c.lineWidth = r * .012;
  c.strokeStyle = 'rgba(0,0,0,.3)';
  [.75, .68].forEach(k => {
    c.beginPath();
    c.arc(x, y, r * k, 0, 7);
    c.stroke()
  });
  const hg = c.createLinearGradient(x - r * .47, y - r * .47, x + r * .47, y + r * .47);
  [['0', '#fff'], ['.25', '#e4e7ec'], ['.5', '#9aa0aa'], ['.75', '#f4f6f9'], ['1', '#6f7580']].forEach(([o, col]) => hg.addColorStop(+o, col));
  c.fillStyle = 'rgba(0,0,0,.35)';
  c.beginPath();
  c.arc(x, y, r * .5, 0, 7);
  c.fill();
  c.fillStyle = hg;
  c.beginPath();
  c.arc(x, y, r * .47, 0, 7);
  c.fill();
  c.fillStyle = '#6f7580';
  for (let i = 0; i < 6; i++) {
    const a = i * Math.PI / 3 + Math.PI / 6;
    c.beginPath();
    c.arc(x + r * .4 * Math.cos(a), y + r * .4 * Math.sin(a), r * .028, 0, 7);
    c.fill()
  }
  const hole = c.createRadialGradient(x, y - r * .05, 1, x, y, r * .2);
  hole.addColorStop(0, '#3a3d44');
  hole.addColorStop(1, '#050506');
  c.fillStyle = hole;
  c.beginPath();
  c.arc(x, y, r * .2, 0, 7);
  c.fill();
  c.lineWidth = r * .03;
  c.strokeStyle = '#c9cdd4';
  c.stroke();
  c.save();
  c.globalAlpha = .35;
  c.strokeStyle = '#fff';
  c.lineWidth = r * .12;
  c.lineCap = 'round';
  c.beginPath();
  c.arc(x, y, r * .8, Math.PI * 1.08, Math.PI * 1.42);
  c.stroke();
  c.restore()
}
async function makeCard() {
  const s = cur(), p = pd(), col = colorOf(p), ss = sessStats(s), G = shareG(col), W = 1080, H = 1920;
  try {
    await Promise.all([document.fonts.load('900 120px Archivo'), document.fonts.load('italic 900 120px Archivo'), document.fonts.load('700 40px Inter')])
  } catch (x) {}
  const cv = document.createElement('canvas');
  cv.width = W;
  cv.height = H;
  const c = cv.getContext('2d');
  const bg = c.createLinearGradient(0, 0, W, H);
  bg.addColorStop(0, G[0]);
  bg.addColorStop(.5, G[1]);
  bg.addColorStop(1, G[2]);
  c.fillStyle = bg;
  c.fillRect(0, 0, W, H);
  [[W * .9, H * .08, 700, 'rgba(255,255,255,.28)'], [0, H * .95, 800, 'rgba(0,0,0,.22)']].forEach(([x, y, r, cl]) => {
    const g = c.createRadialGradient(x, y, 0, x, y, r);
    g.addColorStop(0, cl);
    g.addColorStop(1, 'rgba(0,0,0,0)');
    c.fillStyle = g;
    c.fillRect(0, 0, W, H)
  });
  c.lineCap = 'round';
  [[H * .2, 90, 1, 0], [H * .55, 120, .8, 2.2], [H * .86, 100, 1.1, 4]].forEach(([y0, a, f, ph]) => {
    for (const [lw, al, bl] of [[26, .18, 30], [4, .7, 0]]) {
      c.strokeStyle = `rgba(255,255,255,${al})`;
      c.lineWidth = lw;
      c.shadowColor = '#fff';
      c.shadowBlur = bl;
      c.beginPath();
      for (let k = 0; k <= 120; k++) {
        const t = k / 120, x = -50 + (W + 100) * t, y = y0 + a * Math.sin(t * Math.PI * 2 * f + ph) - (t - .5) * 120;
        k ? c.lineTo(x, y) : c.moveTo(x, y)
      }
      c.stroke()
    }
  });
  c.shadowBlur = 0;
  c.fillStyle = '#fff';
  c.font = 'italic 900 64px Archivo, sans-serif';
  c.textBaseline = 'alphabetic';
  c.fillText('OVERLOAD', 80, 150);
  c.font = '700 40px Inter, sans-serif';
  c.globalAlpha = .9;
  const dt = toD(s.date || st.date);
  c.fillText(`${DN[dayIdx(s.date||st.date)]} ${MON[dt.getMonth()]} ${dt.getDate()}`, 80, 215);
  c.globalAlpha = 1;
  c.shadowColor = 'rgba(0,0,0,.45)';
  c.shadowBlur = 50;
  c.shadowOffsetY = 24;
  drawPlate(c, W - 250, 330, 190);
  c.shadowColor = 'transparent';
  c.shadowBlur = 0;
  c.shadowOffsetY = 0;
  c.fillStyle = '#fff';
  let fs = 150;
  c.font = `900 ${fs}px Archivo, sans-serif`;
  while (c.measureText(p.name).width > W - 160 && fs > 70) {
    fs -= 8;
    c.font = `900 ${fs}px Archivo, sans-serif`
  }
  c.fillText(p.name, 80, 640);
  const tiles = [[mins(elapsed(s)), 'Time'], [String(ss.sets), 'Sets'], [kfmt(ss.vol), U() + ' moved'], [String(ss.prs.length), 'PRs']];
  const tw = (W - 160 - 3 * 24) / 4;
  tiles.forEach(([v, l], i) => {
    const x = 80 + i * (tw + 24), y = 700;
    c.fillStyle = 'rgba(255,255,255,.18)';
    c.beginPath();
    c.roundRect ? c.roundRect(x, y, tw, 170, 28) : c.rect(x, y, tw, 170);
    c.fill();
    c.fillStyle = '#fff';
    let f = 64;
    c.font = `900 ${f}px Archivo, sans-serif`;
    while (c.measureText(v).width > tw - 36 && f > 36) {
      f -= 4;
      c.font = `900 ${f}px Archivo, sans-serif`
    }
    c.fillText(v, x + 22, y + 92);
    c.font = '700 30px Inter, sans-serif';
    c.globalAlpha = .9;
    c.fillText(l, x + 22, y + 140);
    c.globalAlpha = 1
  });
  const lifts = Object.entries(s.ex || {}).map(([id, a]) => {
    const v = a.filter(full);
    if (!v.length) return null;
    const b = v.reduce((m, x) => e1(x) > e1(m) ? x : m, v[0]);
    return {id, b, pr: ss.prs.includes(id)}
  }).filter(Boolean).sort((x, y) => (y.pr - x.pr) || (e1(y.b) - e1(x.b))).slice(0, 3);
  c.fillStyle = 'rgba(0,0,0,.2)';
  c.beginPath();
  const ly = 930, lh = 110 + lifts.length * 104;
  c.roundRect ? c.roundRect(80, ly, W - 160, lh, 36) : c.rect(80, ly, W - 160, lh);
  c.fill();
  c.fillStyle = '#fff';
  c.font = '800 34px Inter, sans-serif';
  c.globalAlpha = .85;
  c.fillText('TOP SETS', 124, ly + 72);
  c.globalAlpha = 1;
  lifts.forEach((L, i) => {
    const y = ly + 150 + i * 104;
    let nm = exName(L.id);
    c.font = '700 44px Inter, sans-serif';
    while (c.measureText(nm).width > W - 560 && nm.length > 4) nm = nm.slice(0, -2) + '…';
    c.fillText(nm, 124, y);
    c.font = '900 54px Archivo, sans-serif';
    const t = `${L.b.w}×${L.b.r}`;
    const twid = c.measureText(t).width;
    c.fillText(t, W - 124 - twid, y);
    if (L.pr) {
      c.font = '900 28px Inter, sans-serif';
      const pw = c.measureText('PR').width + 28;
      c.fillStyle = '#fff';
      c.beginPath();
      c.roundRect ? c.roundRect(W - 124 - twid - pw - 20, y - 38, pw, 44, 22) : c.rect(W - 124 - twid - pw - 20, y - 38, pw, 44);
      c.fill();
      c.fillStyle = G[1];
      c.fillText('PR', W - 124 - twid - pw - 6, y - 6);
      c.fillStyle = '#fff'
    }
  });
  if (!lifts.length) {
    c.font = '700 40px Inter, sans-serif';
    c.fillText('Log some sets to fill this in', 124, ly + 150)
  }
  const mi = await mapImage(mLevels(muscleSets(s)));
  if (mi) {
    c.save();
    c.shadowColor = 'rgba(0,0,0,.35)';
    c.shadowBlur = 30;
    c.drawImage(mi, W / 2 - 150, 1440, 300, 300 * 760 / 560);
    c.restore();
    c.fillStyle = '#fff';
    c.font = '800 30px Inter, sans-serif';
    c.globalAlpha = .85;
    c.textAlign = 'center';
    c.fillText('MUSCLES HIT', W / 2, 1420);
    c.textAlign = 'left';
    c.globalAlpha = 1
  }
  c.font = '700 34px Inter, sans-serif';
  c.globalAlpha = .85;
  c.textAlign = 'center';
  c.fillText('Tracked with Overload', W / 2, H - 36);
  c.globalAlpha = 1;
  c.textAlign = 'left';
  const blob = await new Promise(r => cv.toBlob(r, 'image/png'));
  const name = `overload-${s.date||st.date}.png`;
  return {blob, url: cv.toDataURL('image/png'), file: new File([blob], name, {type: 'image/png'})}
}
async function openShare() {
  st.sheet = {type: 'share'};
  renderSheet();
  try {
    const r = await makeCard();
    if (st.sheet && st.sheet.type === 'share') {
      st.sheet.url = r.url;
      st.sheet.file = r.file;
      renderSheet()
    }
  } catch (x) {
    toast('Could not make the card');
    st.sheet = null;
    renderSheet()
  }
}
/* ---------- special plate finishes ---------- */
const FIN = {
  mirror: ['Mirror Chrome', ['#c9ced6', '#ffffff', '#6b717c', 'linear-gradient(135deg,#7b818b 0%,#ffffff 42%,#8b9099 58%,#eef1f5 100%)'], 'Platinum rank', E => E.rank >= 4],
  goldleaf: ['Gold Leaf', ['#c99a2e', '#ffe58a', '#7a5410', 'linear-gradient(135deg,#8a6114 0%,#f6d36b 45%,#b88a2a 70%,#ffe9a0 100%)'], 'Gold rank', E => E.rank >= 3],
  carbon: ['Carbon Fiber', ['#2a2c31', '#4a4d55', '#101114', 'repeating-linear-gradient(45deg,#1a1b1f 0 6px,#2e3036 6px 12px)'], 'Intermediate lift rank on any lift', () => liftTierMax() >= 2],
  lava: ['Lava', ['#b3260b', '#ff8a3d', '#3d0b03', 'linear-gradient(135deg,#2a0602 0%,#b3260b 40%,#ff7a1f 60%,#3d0b03 100%)'], 'Season pass level 5', null, {season: 5}],
  holo: ['Holographic', ['#b9c6ff', '#ffffff', '#7a88c9', 'linear-gradient(135deg,#ffb3e6 0%,#b3d4ff 30%,#b3ffd9 55%,#fff4b3 80%,#e0b3ff 100%)'], 'Season pass level 15', null, {season: 15}],
  nebula: ['Galaxy', ['#2a1260', '#8f6bff', '#0c0526', 'radial-gradient(circle at 35% 35%,#ff4fd8 0%,#5b2bd6 35%,#0c0526 75%)'], 'Season pass level 25', null, {season: 25}],
  neonring: ['Neon Ring', ['#0d0f14', '#2a2e38', '#000000', 'radial-gradient(circle,#0d0f14 55%,#00e5ff 58%,#0d0f14 64%,#ff2bd6 70%,#0d0f14 74%)'], 'Crew level 5', () => crewLv() >= 5],
  diamondp: ['Diamond Plate', ['#9aa1ab', '#e7eaee', '#555b65', 'repeating-linear-gradient(45deg,#8b929c 0 4px,#c9ced6 4px 6px,#8b929c 6px 10px)'], 'Diamond rank', E => E.rank >= 5],
  frost: ['Frost', ['#8fd3ff', '#e9f8ff', '#2b7fb0', 'linear-gradient(135deg,#e9f8ff 0%,#8fd3ff 50%,#c9ecff 100%)'], 'Weekly Grinder collection', E => hasB(E, 'col_week')],
  radioactive: ['Toxic', ['#3cbf2f', '#c8ff5a', '#0f4d0a', 'radial-gradient(circle,#c8ff5a 0%,#3cbf2f 45%,#0f4d0a 100%)'], 'Around the Clock collection', E => hasB(E, 'col_clock')],
  camo: ['Camo', ['#5d6b3a', '#8a9a5b', '#2f3a1c', 'radial-gradient(circle at 30% 30%,#8a9a5b 0 18%,transparent 19%),radial-gradient(circle at 70% 60%,#3d4a22 0 22%,transparent 23%),#5d6b3a'], 'Earn 3 funny badges', E => E.B.filter(b => b.cat === 'Funny' && b.earned).length >= 3],
  marble: ['Marble', ['#ece9e4', '#ffffff', '#a9a49c', 'linear-gradient(120deg,#ffffff 0%,#ece9e4 40%,#c9c4bb 45%,#ece9e4 50%,#fff 100%)'], 'Crew rank Gold', () => crewRk() >= 3],
  bloodmoon: ['Blood Moon', ['#7a0a12', '#d4252f', '#2a0306', 'radial-gradient(circle at 40% 40%,#d4252f 0%,#7a0a12 55%,#2a0306 100%)'], 'Titan rank', E => E.rank >= 6],
  laurel: ['Olympian Laurel', ['#f4e3b0', '#fffaf0', '#b8862b', 'radial-gradient(circle,#fffaf0 40%,#f4e3b0 60%,#c9a640 100%)'], 'Olympian rank', E => E.rank >= 7]
};
Object.entries(FIN).forEach(([k, f]) => {
  PC[k] = f[1];
  if (typeof SHG !== 'undefined' && !SHG[k]) SHG[k] = [f[1][2], f[1][0], f[1][1]]
});
const eng14 = () => st.plan ? engine() : {rank: 0, lvl: 0, B: [], R: [], lifePR: 0, season: seasonOf(today()), keys: new Set()};
const crewLv = () => st.cs && st.cs.crew ? st.cs.crew.level || 1 : 0, crewRk = () => st.cs && st.cs.crew ? st.cs.crew.rank || 0 : 0;
function liftTierMax() {try {return Math.max(0, ...liftRanks().map(r => r.t))} catch (e) {return 0}}
const cos14 = t => ((st.set.cos14 || {})[t] || []);
function finOwned(k, E) {
  const f = FIN[k];
  if (!f) return true;
  if (cos14('plate').includes(k)) return true;
  return f[3] ? !!f[3](E || eng14()) : false
}
function finishOverlay(k, id) {
  const g = (a) => a;
  if (T15[k] || String(k).startsWith('l_')) return '';
  switch (k) {
    case 'mirror': return `<linearGradient id="${id}fx" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fff" stop-opacity=".0"/><stop offset=".38" stop-color="#fff" stop-opacity=".9"/><stop offset=".46" stop-color="#5c626c" stop-opacity=".7"/><stop offset=".62" stop-color="#fff" stop-opacity=".6"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient><circle cx="50" cy="50" r="47" fill="url(#${id}fx)"/>`;
    case 'goldleaf': return Array.from({length: 16}, (_, i) => {
      const a = hashS(id + i) % 360, r = 12 + hashS(id + 'r' + i) % 32, x = 50 + r * Math.cos(a), y = 50 + r * Math.sin(a);
      return `<path d="M${x.toFixed(1)} ${y.toFixed(1)}l${3+i%3} 1l-1 ${3+i%2}l-${2+i%3}-1z" fill="#fff3c2" opacity=".75"/>`
    }).join('');
    case 'carbon': return `<pattern id="${id}cb" width="5" height="5" patternUnits="userSpaceOnUse"><rect width="5" height="5" fill="#1d1e22"/><rect width="2.5" height="2.5" fill="#34363c"/><rect x="2.5" y="2.5" width="2.5" height="2.5" fill="#34363c"/></pattern><circle cx="50" cy="50" r="47" fill="url(#${id}cb)" opacity=".9"/>`;
    case 'lava': return `<g fill="none" stroke="#ffb347" stroke-width="1.6" stroke-linecap="round"><path d="M10 40 Q22 44 30 36 T48 30"><animate attributeName="stroke-opacity" values=".3;1;.3" dur="2.4s" repeatCount="indefinite"/></path><path d="M60 88 Q66 74 80 72 T92 56"><animate attributeName="stroke-opacity" values="1;.3;1" dur="2s" repeatCount="indefinite"/></path><path d="M20 76 Q30 70 34 80"><animate attributeName="stroke-opacity" values=".4;1;.4" dur="1.7s" repeatCount="indefinite"/></path></g>`;
    case 'holo': return `<linearGradient id="${id}hg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ff7ad9"/><stop offset=".25" stop-color="#7ab8ff"/><stop offset=".5" stop-color="#7affc4"/><stop offset=".75" stop-color="#ffe27a"/><stop offset="1" stop-color="#c47aff"/><animateTransform attributeName="gradientTransform" type="rotate" from="0 .5 .5" to="360 .5 .5" dur="6s" repeatCount="indefinite"/></linearGradient><circle cx="50" cy="50" r="47" fill="url(#${id}hg)" opacity=".55"/>`;
    case 'nebula': return `<g><ellipse cx="44" cy="40" rx="30" ry="12" fill="#ff4fd8" opacity=".35" transform="rotate(-20 50 50)"/><ellipse cx="58" cy="60" rx="26" ry="10" fill="#4fb3ff" opacity=".3" transform="rotate(25 50 50)"/><animateTransform attributeName="transform" type="rotate" from="0 50 50" to="360 50 50" dur="40s" repeatCount="indefinite"/></g>${Array.from({length:14},(_,i)=>`<circle cx="${10+hashS(id+'x'+i)%80}" cy="${10+hashS(id+'y'+i)%80}" r=".7" fill="#fff"><animate attributeName="opacity" values=".2;1;.2" dur="${1.5+i%4*.5}s" repeatCount="indefinite"/></circle>`).join('')}`;
    case 'neonring': return `<circle cx="50" cy="50" r="42" fill="none" stroke="#00e5ff" stroke-width="2"><animate attributeName="stroke-opacity" values=".5;1;.5" dur="1.8s" repeatCount="indefinite"/></circle><circle cx="50" cy="50" r="42" fill="none" stroke="#00e5ff" stroke-width="5" opacity=".18"/><circle cx="50" cy="50" r="31" fill="none" stroke="#ff2bd6" stroke-width="1.6"><animate attributeName="stroke-opacity" values="1;.5;1" dur="1.8s" repeatCount="indefinite"/></circle><circle cx="50" cy="50" r="31" fill="none" stroke="#ff2bd6" stroke-width="4.5" opacity=".18"/>`;
    case 'diamondp': return `<pattern id="${id}dp" width="7" height="7" patternUnits="userSpaceOnUse"><rect width="7" height="7" fill="none"/><rect x="1" y="2.6" width="4" height="1.4" rx=".7" fill="#eef1f4" opacity=".8" transform="rotate(45 3 3.3)"/></pattern><circle cx="50" cy="50" r="47" fill="url(#${id}dp)"/>`;
    case 'frost': return `<g stroke="#ffffff" stroke-width=".9" stroke-linecap="round" opacity=".85">${Array.from({length:6},(_,i)=>`<g transform="rotate(${i*60} 50 50)"><path d="M50 50V8M50 20l-5-5M50 20l5-5M50 32l-6-6M50 32l6-6"/></g>`).join('')}</g>`;
    case 'radioactive': return `<circle cx="50" cy="50" r="40" fill="none" stroke="#e7ff7a" stroke-width="1.2" stroke-dasharray="3 5"><animateTransform attributeName="transform" type="rotate" from="0 50 50" to="360 50 50" dur="12s" repeatCount="indefinite"/></circle><g fill="#d8ff5a" opacity=".8"><path d="M22 20c2 6 4 10 2 14-2-3-4-8-2-14z"/><path d="M76 24c2 7 3 11 1 15-2-4-3-9-1-15z"/></g><circle cx="50" cy="50" r="46" fill="#c8ff5a" opacity=".12"><animate attributeName="opacity" values=".05;.22;.05" dur="2.2s" repeatCount="indefinite"/></circle>`;
    case 'camo': return `<g opacity=".9"><path d="M14 30c8-10 22-6 20 4s-16 8-20-4z" fill="#2f3a1c"/><path d="M58 18c10-4 20 4 14 12s-18 2-14-12z" fill="#8a9a5b"/><path d="M60 64c12-4 22 6 14 14s-20 2-14-14z" fill="#3d2f1c"/><path d="M20 66c6-8 18-4 16 6s-14 6-16-6z" fill="#8a9a5b"/><path d="M40 44c6-4 12 0 10 6s-10 2-10-6z" fill="#3d2f1c"/></g>`;
    case 'marble': return `<g fill="none" stroke="#8f8a82" stroke-linecap="round" opacity=".55"><path d="M8 34c14 4 18 18 34 16s20 10 34 4" stroke-width="1"/><path d="M20 78c10-8 22-4 30-14s18-8 30-20" stroke-width=".7"/><path d="M30 14c4 8 12 10 14 20" stroke-width=".5"/></g>`;
    case 'bloodmoon': return `<g fill="#3a0408" opacity=".55"><circle cx="34" cy="34" r="6"/><circle cx="66" cy="70" r="8"/><circle cx="72" cy="30" r="3.5"/><circle cx="28" cy="70" r="4"/></g><circle cx="50" cy="50" r="46.5" fill="none" stroke="#ff3d4a" stroke-width="1.4"><animate attributeName="stroke-opacity" values=".4;1;.4" dur="3s" repeatCount="indefinite"/></circle>`;
    case 'laurel': return `<g fill="#c9a640">${Array.from({length:18},(_,i)=>{const a=(i*20)*Math.PI/180;const x=50+41*Math.cos(a),y=50+41*Math.sin(a);return `<ellipse cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" rx="2.2" ry="4.6" transform="rotate(${i*20} ${x.toFixed(1)} ${y.toFixed(1)})"/>`}).join('')}</g>`;
  }
  return ''
}/* ---------- themed, holiday, founder and OG plates (0.15) ---------- */
const sessOnHoliday = test => Object.values(st.sessions).some(s => s && s.date && !s._del && test(toD(s.date)) && Object.values(s.ex || {}).some(a => Array.isArray(a) && a.some(full)));
const isThanks = t => t.getMonth() === 10 && t.getDay() === 4 && t.getDate() >= 22 && t.getDate() <= 28;
const liftLegend = k => {
  try {
    const r = liftRanks().find(x => x.k === k);
    return !!(r && r.t >= LIFTS[0][3].length)
  } catch (e) {return false}
};
const myMember = () => st.cs && st.cs.members.find(m => m.id === myId());
const myOG = () => {
  const m = myMember();
  return m && m.profile && m.profile.og || st.set.og || 0
};
const badgeOn = (E, k) => !!(E.B || []).find(b => b.k === k && b.earned);
const crewTro = k => !!(st.cs && st.cs.crew.trophies && st.cs.crew.trophies[k]);
const raidWon = () => !!(st.cs && st.cs.raid && (st.cs.raid.won || []).some(w => (w.by || []).includes(myId())));
const T15 = {
  r_bronze: ['Bronze Forged', ['#a0612b', '#e0a066', '#4a2a0e'], 'Bronze rank', E => E.rank >= 1, 'rank'],
  r_silver: ['Silver Engraved', ['#a9b1bc', '#f2f5f8', '#5b6370'], 'Silver rank', E => E.rank >= 2, 'rank'],
  r_gold: ['Gold Crest', ['#c99a2e', '#ffe58a', '#6b4a0c'], 'Gold rank', E => E.rank >= 3, 'rank'],
  r_plat: ['Platinum Cut', ['#cfd8e3', '#ffffff', '#7d8a9b'], 'Platinum rank', E => E.rank >= 4, 'rank'],
  r_diamond: ['Diamond Crystal', ['#7fd3ff', '#e8f8ff', '#1f6fa8'], 'Diamond rank', E => E.rank >= 5, 'rank'],
  r_titan: ['Titan Stone', ['#3a3a40', '#6b6b74', '#121216'], 'Titan rank', E => E.rank >= 6, 'rank'],
  r_olympian: ['Olympian Marble', ['#f1ece2', '#ffffff', '#b9ad92'], 'Olympian rank', E => E.rank >= 7, 'rank'],
  c_mpc: ['Million Pound', ['#4a4d55', '#8b9099', '#1a1b1f'], 'Crew trophy: Million Pound Club', () => crewTro('mpc'), 'crew'],
  c_storm: ['PR Storm', ['#2a2f4a', '#5b6390', '#0d0f1c'], 'Crew trophy: PR Storm', () => crewTro('prstorm'), 'crew'],
  c_phoenix: ['Phoenix', ['#c2410c', '#ffb347', '#3d0b03'], 'Crew trophy: Comeback Crew', () => crewTro('comeback'), 'crew'],
  c_raid: ['Boss Slayer', ['#3b1d4a', '#8b5cf6', '#12061a'], 'Help your crew beat a boss raid', () => raidWon(), 'crew'],
  f_chicken: ['Chicken Legs', ['#f5d77a', '#fff3c4', '#b8862b'], 'Funny badge: Leg Day Dodger', E => badgeOn(E, 'f_legs'), 'funny'],
  f_ghost: ['Ghost', ['#e8ecf5', '#ffffff', '#9aa3b8'], 'Funny badge: Ghost', E => badgeOn(E, 'f_ghost'), 'funny'],
  f_moon: ['Night Shift', ['#141b3d', '#2f3b7a', '#05081a'], 'Funny badge: Night Shift', E => badgeOn(E, 'f_night'), 'funny'],
  f_couch: ['Couch Potato', ['#b07a3f', '#e0b072', '#5a3a17'], 'Funny badge: Rest Day Champion', E => badgeOn(E, 'f_rest'), 'funny'],
  f_peacock: ['Peacock', ['#0f6b6b', '#3cc9b5', '#06302f'], 'Funny badge: Ego Lifter', E => badgeOn(E, 'f_ego'), 'funny'],
  h_xmas: ['Christmas', ['#b3121f', '#e8424e', '#4d060b'], 'Lift on Christmas (Dec 25)', () => sessOnHoliday(t => t.getMonth() === 11 && t.getDate() === 25), 'holiday'],
  h_newyear: ['New Year', ['#16130b', '#4a3f1c', '#000000'], 'Lift on New Year’s Day', () => sessOnHoliday(t => t.getMonth() === 0 && t.getDate() === 1), 'holiday'],
  h_valentine: ['Valentine', ['#e04a82', '#ff9fc4', '#7a1236'], 'Lift on Valentine’s Day (Feb 14)', () => sessOnHoliday(t => t.getMonth() === 1 && t.getDate() === 14), 'holiday'],
  h_stpat: ['Lucky Clover', ['#178a3c', '#5ed47f', '#063a17'], 'Lift on St. Patrick’s Day (Mar 17)', () => sessOnHoliday(t => t.getMonth() === 2 && t.getDate() === 17), 'holiday'],
  h_july4: ['Fourth of July', ['#1d3a8a', '#4a6fd6', '#0a1640'], 'Lift on the Fourth of July', () => sessOnHoliday(t => t.getMonth() === 6 && t.getDate() === 4), 'holiday'],
  h_halloween: ['Halloween', ['#e8650c', '#ffa44a', '#5a2302'], 'Lift on Halloween (Oct 31)', () => sessOnHoliday(t => t.getMonth() === 9 && t.getDate() === 31), 'holiday'],
  h_thanks: ['Turkey Day', ['#9a4a17', '#e0903f', '#3d1a05'], 'Lift on Thanksgiving', () => sessOnHoliday(isThanks), 'holiday'],
  founder: ['Founder 001', ['#0b0b0d', '#2a2a2e', '#000000'], 'Only the founder of Overload', () => !!st.set.founder, 'special'],
  og: ['OG', ['#efe6cf', '#fffaf0', '#a8946a'], 'Join Overload before the App Store launch', () => !!myOG(), 'special']
};
const PCAT = {
  colors: 'Colors', finish: 'Finishes', rank: 'Ranks', lift: 'Lift legends', crew: 'Crew', funny: 'Funny', holiday: 'Holiday', special: 'Special'
};
function addPlate15(k, t) {
  PC[k] = [t[1][0], t[1][1], t[1][2], `radial-gradient(circle at 35% 30%,${t[1][1]},${t[1][0]} 55%,${t[1][2]})`];
  FIN[k] = [t[0], PC[k], t[2], t[3], null, t[4]];
  if (typeof SHG !== 'undefined' && !SHG[k]) SHG[k] = [t[1][2], t[1][0], t[1][1]]
}
/* lift legend plates are added at startup (the lift list is defined later in the file) */
function addLiftPlates() {
  LIFTS.forEach(l => {
    const k = 'l_' + l[0];
    if (!T15[k]) {
      T15[k] = [l[1] + ' Legend', ['#2b2e35', '#5a606b', '#0d0e12'], 'Legend rank on ' + l[1], () => liftLegend(l[0]), 'lift'];
      addPlate15(k, T15[k])
    }
  })
}
const plateCat = k => FIN[k] ? FIN[k][5] : 'colors';
/* themed artwork (drawn over the plate face, under the hub) */
const SIL = {
  press: '<path d="M28 58h44M34 58v8M66 58v8M40 50c4-3 16-3 20 0M50 47v-8M44 39h12M22 52v12M78 52v12" />', squat: '<path d="M30 34h40M24 28v12M76 28v12M50 36a5 5 0 1 1 0-1M50 41v12l-8 6 4 10M50 53l8 6-4 10"/>', hinge: '<path d="M26 70h48M20 63v14M80 63v14M50 34a5 5 0 1 1 0-1M50 40l-2 14 4 14M48 54l-8 8"/>', over: '<path d="M28 26h44M22 20v12M78 20v12M50 44a5 5 0 1 1 0-1M44 30l6 9 6-9M50 49v14l-6 9M50 63l6 9"/>'
};
const liftSil = k => /bench|incline/.test(k) ? SIL.press : /squat|hack|legpress/.test(k) ? SIL.squat : /dead|rdl/.test(k) ? SIL.hinge : /ohp/.test(k) ? SIL.over : SIL.press;
function rimTxt(id, txt, off) {
  return `<path id="${id}rt" d="M50 50 m-40 0 a40 40 0 1 1 80 0 a40 40 0 1 1 -80 0" fill="none"/><text style="font:800 5.6px Archivo,sans-serif;letter-spacing:1.4px" fill="rgba(255,255,255,.9)"><textPath href="#${id}rt" startOffset="${off||'4%'}">${esc(txt)}</textPath></text>`
}
function themedOverlay(k, id) {
  const R = (n, a, b) => a + (hashS(id + k + n) % 1000) / 1000 * (b - a);
  if (k.startsWith('l_')) return `<circle cx="50" cy="50" r="42" fill="none" stroke="#e3c25a" stroke-width="1.2" stroke-dasharray="2 2"/><g fill="none" stroke="#e3c25a" stroke-width="4.4" stroke-linecap="round" stroke-linejoin="round" transform="translate(35 4) scale(.3)">${liftSil(k.slice(2))}</g><text x="50" y="86" text-anchor="middle" style="font:900 6px Archivo,sans-serif;letter-spacing:1.6px" fill="#e3c25a">LEGEND</text>`;
  switch (k) {
    case 'r_bronze': return `${Array.from({length:22},(_,i)=>`<circle cx="${R('x'+i,18,82).toFixed(1)}" cy="${R('y'+i,18,82).toFixed(1)}" r="${R('r'+i,2,5).toFixed(1)}" fill="${i%2?'#e0a066':'#4a2a0e'}" opacity=".35"/>`).join('')}${Array.from({length:10},(_,i)=>{const a=i*36*Math.PI/180;return `<circle cx="${(50+42*Math.cos(a)).toFixed(1)}" cy="${(50+42*Math.sin(a)).toFixed(1)}" r="1.9" fill="#f0c088" stroke="#5a3210" stroke-width=".6"/>`}).join('')}`;
    case 'r_silver': return `${[44,40,36,32,28].map(r=>`<circle cx="50" cy="50" r="${r}" fill="none" stroke="#ffffff" stroke-width=".35" opacity=".6"/>`).join('')}<circle cx="50" cy="50" r="43" fill="none" stroke="#5b6370" stroke-width="1.4" stroke-dasharray="1 2.2"/>${Array.from({length:12},(_,i)=>`<path d="M50 9l2 4-2 2-2-2z" fill="#5b6370" transform="rotate(${i*30} 50 50)"/>`).join('')}`;
    case 'r_gold': return `${Array.from({length:24},(_,i)=>`<path d="M50 50L49 8h2z" fill="#fff3b0" opacity=".25" transform="rotate(${i*15} 50 50)"/>`).join('')}<path d="M50 22l4 9 9 1-7 6 2 9-8-5-8 5 2-9-7-6 9-1z" fill="#fff3b0" stroke="#6b4a0c" stroke-width=".8"/><circle cx="50" cy="50" r="43" fill="none" stroke="#fff3b0" stroke-width="1"/>`;
    case 'r_plat': return Array.from({length: 12}, (_, i) => `<path d="M50 50L${(50+47*Math.cos(i*30*Math.PI/180)).toFixed(1)} ${(50+47*Math.sin(i*30*Math.PI/180)).toFixed(1)}L${(50+47*Math.cos((i+1)*30*Math.PI/180)).toFixed(1)} ${(50+47*Math.sin((i+1)*30*Math.PI/180)).toFixed(1)}z" fill="${i%2?'#ffffff':'#7d8a9b'}" opacity="${i%2?.35:.25}"/>`).join('');
    case 'r_diamond': return `${Array.from({length:8},(_,i)=>`<path d="M50 50L${(50+47*Math.cos(i*45*Math.PI/180)).toFixed(1)} ${(50+47*Math.sin(i*45*Math.PI/180)).toFixed(1)}L${(50+30*Math.cos((i+.5)*45*Math.PI/180)).toFixed(1)} ${(50+30*Math.sin((i+.5)*45*Math.PI/180)).toFixed(1)}z" fill="${i%2?'#ffffff':'#1f6fa8'}" opacity=".35"/>`).join('')}${[[30,28],[70,34],[64,72]].map(([x,y],i)=>`<path d="M${x} ${y-5}l1.2 3.8 3.8 1.2-3.8 1.2-1.2 3.8-1.2-3.8-3.8-1.2 3.8-1.2z" fill="#fff"><animate attributeName="opacity" values="0;1;0" dur="${2+i*.6}s" repeatCount="indefinite"/></path>`).join('')}`;
    case 'r_titan': return `<g fill="none" stroke="#ff6a1f" stroke-width="1.3" stroke-linecap="round"><path d="M14 40l12 4 6 10 10 2 6 8M70 14l-4 14 8 10 10 2M60 86l-4-12 6-8 12-2"><animate attributeName="stroke-opacity" values=".35;1;.35" dur="2.6s" repeatCount="indefinite"/></path></g><g fill="#000" opacity=".25"><circle cx="30" cy="70" r="5"/><circle cx="74" cy="56" r="4"/></g>`;
    case 'r_olympian': return `${finishOverlay('marble',id)}${finishOverlay('laurel',id)}<linearGradient id="${id}om" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset=".5" stop-color="#fff6c8" stop-opacity=".6"/><stop offset="1" stop-color="#fff" stop-opacity="0"/><animateTransform attributeName="gradientTransform" type="translate" from="-1 0" to="1 0" dur="3.5s" repeatCount="indefinite"/></linearGradient><circle cx="50" cy="50" r="47" fill="url(#${id}om)"/>`;
    case 'c_mpc': return `${Array.from({length:12},(_,i)=>{const a=i*30*Math.PI/180;return `<circle cx="${(50+43*Math.cos(a)).toFixed(1)}" cy="${(50+43*Math.sin(a)).toFixed(1)}" r="1.6" fill="#bfc3ca"/>`}).join('')}<text x="50" y="22" text-anchor="middle" style="font:900 7px Archivo,sans-serif" fill="#d6d9de">1,000,000</text><text x="50" y="84" text-anchor="middle" style="font:900 7px Archivo,sans-serif;letter-spacing:2px" fill="#d6d9de">LB CLUB</text>`;
    case 'c_storm': return `<g fill="#11152a" opacity=".85"><ellipse cx="32" cy="20" rx="18" ry="7"/><ellipse cx="62" cy="17" rx="20" ry="8"/></g><path d="M76 30l-8 14h6l-5 14 13-19h-7l5-9z" fill="#e8ecff"><animate attributeName="opacity" values="0;0;1;.2;1;0" dur="3s" repeatCount="indefinite"/></path>`;
    case 'c_phoenix': return `<g fill="#ffcf6b" opacity=".9"><path d="M50 72c-4-10-4-20 0-28 4 8 4 18 0 28z"/><path d="M48 50c-10-4-20-14-28-30 12 4 22 12 28 22zM52 50c10-4 20-14 28-30-12 4-22 12-28 22z"><animate attributeName="opacity" values=".7;1;.7" dur="1.6s" repeatCount="indefinite"/></path><path d="M50 72c-6 6-8 12-6 18 2-4 4-6 6-8 2 2 4 4 6 8 2-6 0-12-6-18z" fill="#ff7a1f"/></g>`;
    case 'c_raid': return `<g stroke="#e6d6ff" stroke-width="2.6" stroke-linecap="round"><path d="M30 30l40 40M70 30l-40 40"/></g><g stroke="#8b5cf6" stroke-width="5" stroke-linecap="round"><path d="M64 70l6 6M36 70l-6 6"/></g><circle cx="50" cy="50" r="7" fill="#12061a" stroke="#e6d6ff"/>`;
    case 'f_chicken': return `<g transform="translate(-20 -4) rotate(-20 50 50)"><ellipse cx="40" cy="44" rx="9" ry="12" fill="#c8742b"/><rect x="38" y="54" width="4" height="16" rx="2" fill="#fff6e0"/><circle cx="37" cy="72" r="3" fill="#fff6e0"/><circle cx="43" cy="72" r="3" fill="#fff6e0"/></g><g transform="translate(20 -4) rotate(20 50 50)"><ellipse cx="60" cy="44" rx="9" ry="12" fill="#c8742b"/><rect x="58" y="54" width="4" height="16" rx="2" fill="#fff6e0"/><circle cx="57" cy="72" r="3" fill="#fff6e0"/><circle cx="63" cy="72" r="3" fill="#fff6e0"/></g>`;
    case 'f_ghost': return `<animate attributeName="opacity" values="1;.55;1" dur="3s" repeatCount="indefinite"/><path d="M34 70V44a16 16 0 0 1 32 0v26l-5-4-5 4-6-4-6 4-5-4z" fill="#fff" opacity=".9"/><circle cx="44" cy="46" r="2.6" fill="#2a2f45"/><circle cx="56" cy="46" r="2.6" fill="#2a2f45"/><ellipse cx="50" cy="55" rx="3" ry="4" fill="#2a2f45"/>`;
    case 'f_moon': return `<circle cx="56" cy="44" r="16" fill="#ffe9a8"/><circle cx="63" cy="38" r="15" fill="#141b3d"/>${Array.from({length:10},(_,i)=>`<circle cx="${R('sx'+i,16,84).toFixed(0)}" cy="${R('sy'+i,16,84).toFixed(0)}" r=".9" fill="#fff"><animate attributeName="opacity" values=".2;1;.2" dur="${1.4+i%3*.5}s" repeatCount="indefinite"/></circle>`).join('')}<text x="50" y="82" text-anchor="middle" style="font:900 5.4px Archivo,sans-serif;letter-spacing:1.4px" fill="#ffe9a8">NIGHT SHIFT</text>`;
    case 'f_couch': return `<g transform="translate(0 -28) scale(1)"><rect x="24" y="50" width="52" height="16" rx="4" fill="#7a3a2a"/><rect x="20" y="44" width="10" height="22" rx="3" fill="#8a4a3a"/><rect x="70" y="44" width="10" height="22" rx="3" fill="#8a4a3a"/><ellipse cx="50" cy="46" rx="12" ry="8" fill="#c9954f"/><circle cx="46" cy="44" r="1.2" fill="#3a2410"/><circle cx="54" cy="44" r="1.2" fill="#3a2410"/><path d="M47 49q3 2 6 0" stroke="#3a2410" stroke-width=".9" fill="none"/></g>`;
    case 'f_peacock': return Array.from({length: 9}, (_, i) => `<g transform="rotate(${-80+i*20} 50 78)"><path d="M50 78L50 24" stroke="#3cc9b5" stroke-width="1"/><ellipse cx="50" cy="26" rx="5" ry="7" fill="#1a8f7a"/><ellipse cx="50" cy="27" rx="2.6" ry="3.6" fill="#1d3a8a"/><circle cx="50" cy="27.5" r="1.2" fill="#ffd23f"/></g>`).join('');
    case 'h_xmas': return `${Array.from({length:7},(_,i)=>`<g transform="translate(${R('x'+i,18,82).toFixed(0)} ${R('y'+i,18,82).toFixed(0)}) scale(.5)" stroke="#fff" stroke-width="1.4" opacity=".75"><path d="M0-6V6M-5-3l10 6M-5 3l10-6"/></g>`).join('')}<path d="M50 26l-14 26h8l-10 18h32l-10-18h8z" fill="#0f6b2f"/><rect x="47" y="70" width="6" height="6" fill="#5a3210"/><circle cx="50" cy="26" r="2.4" fill="#ffd23f"/><circle cx="44" cy="52" r="1.6" fill="#ffd23f"/><circle cx="56" cy="60" r="1.6" fill="#fff"/>`;
    case 'h_newyear': return `${Array.from({length:3},(_,j)=>{const cx=[34,64,50][j],cy=[34,40,64][j],c=['#ffd23f','#ff4fd8','#4fb3ff'][j];return `<g>${Array.from({length:12},(_,i)=>`<path d="M${cx} ${cy}l0 -12" stroke="${c}" stroke-width="1.4" stroke-linecap="round" transform="rotate(${i*30} ${cx} ${cy})"/>`).join('')}<animate attributeName="opacity" values="0;1;0" dur="2.4s" begin="${j*.8}s" repeatCount="indefinite"/></g>`}).join('')}<text x="50" y="84" text-anchor="middle" style="font:900 6px Archivo,sans-serif;letter-spacing:1.8px" fill="#e3c25a">NEW YEAR</text>`;
    case 'h_valentine': return Array.from({length: 7}, (_, i) => {
      const x = R('x' + i, 22, 78), y = R('y' + i, 22, 78), s2 = R('s' + i, .5, 1.1);
      return `<path transform="translate(${x.toFixed(0)} ${y.toFixed(0)}) scale(${s2.toFixed(2)})" d="M0 6C-8 0-8-6-4-7c2 0 3 1 4 3 1-2 2-3 4-3 4 1 4 7-4 13z" fill="#fff" opacity=".85"/>`
    }).join('');
    case 'h_stpat': return Array.from({length: 5}, (_, i) => {
      const x = R('x' + i, 24, 76), y = R('y' + i, 24, 76);
      return `<g transform="translate(${x.toFixed(0)} ${y.toFixed(0)}) scale(${R('s'+i,.6,1).toFixed(2)})" fill="#c9f5d6"><circle cx="0" cy="-4" r="4"/><circle cx="-4" cy="1" r="4"/><circle cx="4" cy="1" r="4"/><path d="M0 2q1 6 3 9" stroke="#c9f5d6" stroke-width="1.4" fill="none"/></g>`
    }).join('');
    case 'h_july4': return `<clipPath id="${id}fc"><circle cx="50" cy="50" r="47"/></clipPath><g clip-path="url(#${id}fc)">${Array.from({length:7},(_,i)=>`<rect x="0" y="${50+i*7}" width="100" height="3.5" fill="#d62839"/><rect x="0" y="${53.5+i*7}" width="100" height="3.5" fill="#fff"/>`).join('')}</g>${Array.from({length:9},(_,i)=>`<path d="M${28+(i%3)*22} ${20+Math.floor(i/3)*10}l1.4 3 3.2.4-2.4 2.2.6 3.2-2.8-1.6-2.8 1.6.6-3.2-2.4-2.2 3.2-.4z" fill="#fff"/>`).join('')}`;
    case 'h_halloween': return `<path d="M38 38l6 6-6 2zM62 38l-6 6 6 2z" fill="#2a1200"/><path d="M34 58q16 12 32 0l-4 4-4-3-4 4-4-4-4 4-4-4-4 3z" fill="#2a1200"/><path d="M50 18q4-6 8-6" stroke="#2f5d1a" stroke-width="2.4" fill="none"/>${[[30,40],[70,40]].map(([x,y])=>`<circle cx="${x}" cy="${y}" r="0" fill="#ffd23f"><animate attributeName="r" values="0;1.4;0" dur="1.2s" repeatCount="indefinite"/></circle>`).join('')}`;
    case 'h_thanks': return Array.from({length: 7}, (_, i) => `<path d="M50 70 L50 30" stroke="${['#c0392b','#e67e22','#f1c40f','#8e5b2b','#e67e22','#c0392b','#f1c40f'][i]}" stroke-width="9" stroke-linecap="round" transform="rotate(${-60+i*20} 50 70)" opacity=".85"/>`).join('') + `<circle cx="50" cy="66" r="9" fill="#7a4a24"/><circle cx="50" cy="58" r="5" fill="#8e5b2b"/><circle cx="48" cy="57" r=".9" fill="#000"/><path d="M50 60l3 1-3 1z" fill="#f1c40f"/><path d="M50 61q1 3-1 4" stroke="#c0392b" stroke-width="1.4" fill="none"/>`;
    case 'founder': return `<g fill="none" stroke="#e3c25a" stroke-width=".9" stroke-linecap="round" opacity=".9"><path d="M14 50h14l6-8h12M86 50H72l-6 8H54M50 14v12l8 6M50 86V74l-8-6M24 26l10 10M76 74L66 64"><animate attributeName="stroke-dasharray" values="0 200;120 0" dur="3s" repeatCount="indefinite"/></path></g><circle cx="50" cy="50" r="43" fill="none" stroke="#e3c25a" stroke-width="1.4"/><text x="50" y="36" text-anchor="middle" style="font:900 9px Archivo,sans-serif;letter-spacing:1px" fill="#e3c25a">001</text>${rimTxt(id,'FOUNDER · OVERLOAD · FOUNDER · OVERLOAD','2%')}`;
    case 'og': {
      const n = myOG();
      return `<circle cx="50" cy="50" r="43" fill="none" stroke="#2a2a2e" stroke-width="1.2"/><text x="50" y="38" text-anchor="middle" style="font:900 12px Archivo,sans-serif" fill="#2a2a2e">OG</text>${n?rimTxt(id,`OG #${String(n).padStart(3,'0')} · EARLY LIFTER · OG #${String(n).padStart(3,'0')}`,'2%').replace('rgba(255,255,255,.9)','#2a2a2e'):''}`
    }
  }
  return ''
}
/* the rim: your name or motto replaces the second OVERLOAD on big plates */
function rimLabel(lbl) {
  const r = (st.set.rim || '').trim();
  return r ? esc(r.toUpperCase().slice(0, 24)) : 'OVERLOAD · ' + esc(lbl || '')
}
function plateSheet(sh) {
  const E = eng14(), col = colorOf(st.plan.days[st.pday]), cat = sh.cat || plateCat(col), all = Object.keys(PC).filter(k => plateCat(k) === cat), own = k => !plateLocked(k) && (!FIN[k] || finOwned(k, E));
  const vis = sh.locked ? all : all.filter(own), nl = all.filter(k => !own(k)).length, sel = sh.sel || col;
  const counts = Object.keys(PCAT).map(c => [c, Object.keys(PC).filter(k => plateCat(k) === c && own(k)).length]);
  return `<div class="ghd"><h2>Plate</h2><button class="btn sm" id="sheetx">Done</button></div><div class="pbig">${plateSVG(sel,'done',{big:true,label:(st.plan.days[st.pday].name||'').toUpperCase()})}<b>${esc(FIN[sel]?FIN[sel][0]:sel)}</b>${own(sel)?'':`<span class="sub">🔒 ${esc(plateLocked(sel)||(FIN[sel]&&FIN[sel][2])||'')}</span>`}</div>
 <div class="pchips">${counts.map(([c,n])=>`<button class="pchip${c===cat?' on':''}" data-pcat="${c}">${PCAT[c]}${n?` <i>${n}</i>`:''}</button>`).join('')}</div>
 <div class="pgrid">${vis.map(k=>`<button class="pg${k===sel?' on':''}${own(k)?'':' lk'}" data-psel="${k}" aria-label="${esc(FIN[k]?FIN[k][0]:k)}">${plateSVG(k,'done')}${own(k)?'':'<i>🔒</i>'}</button>`).join('')||`<p class="sub">None unlocked here yet.</p>`}</div>
 ${nl?`<button class="btn sm" id="plocked" style="margin-top:8px">${sh.locked?'Hide locked':'Show locked ('+nl+')'}</button>`:''}
 <button class="btn acc wide" id="puse" style="min-height:52px;margin-top:14px"${own(sel)?'':' disabled'}>${own(sel)?'Use this plate':'Locked'}</button>`
}

/* ---------- founder plate ---------- */
function founderPlate(o) {
  o = o || {};
  const id = 'fp' + (pid++), lbl = String(o.label || '').toUpperCase().replace(/[^A-Z0-9 ]/g, '').trim().slice(0, 12);
  const rim = `FOUNDER · 001 · OVERLOAD${lbl?' · '+esc(lbl):''} · EST 2026 ·`;
  const leaf = (a, r, side) => {
    const t = a * Math.PI / 180, x = 50 + r * Math.cos(t), y = 50 + r * Math.sin(t), rot = a + (side < 0 ? 90 : -90) + (side < 0 ? -28 : 28);
    return `<ellipse cx="${x.toFixed(2)}" cy="${y.toFixed(2)}" rx="1.35" ry="3.1" transform="rotate(${rot.toFixed(1)} ${x.toFixed(2)} ${y.toFixed(2)})"/>`
  };
  let L = '';
  for (let i = 0; i < 7; i++) {
    const a = 124 + i * 13.5;
    L += leaf(a, i % 2 ? 34.6 : 31.6, -1) + leaf(180 - a, i % 2 ? 34.6 : 31.6, 1)
  }
  const spark = (x, y, d) => `<path d="M${x} ${y-2.6}L${x+.6} ${y-.6}L${x+2.6} ${y}L${x+.6} ${y+.6}L${x} ${y+2.6}L${x-.6} ${y+.6}L${x-2.6} ${y}L${x-.6} ${y-.6}Z" fill="#fff6cf" opacity="0"><animate attributeName="opacity" values="0;1;0" dur="2.6s" begin="${d}s" repeatCount="indefinite"/></path>`;
  let s = `<svg viewBox="0 0 100 100" aria-hidden="true" class="fplate"><defs>
 <radialGradient id="${id}b" cx="40%" cy="35%" r="75%"><stop offset="0" stop-color="#2c2a2f"/><stop offset=".6" stop-color="#0f0f13"/><stop offset="1" stop-color="#030304"/></radialGradient>
 <linearGradient id="${id}g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fff6cf"/><stop offset=".35" stop-color="#e8c55c"/><stop offset=".7" stop-color="#9c7424"/><stop offset="1" stop-color="#f3d77a"/></linearGradient>
 <radialGradient id="${id}h" cx="38%" cy="32%" r="72%"><stop offset="0" stop-color="#fff3c0"/><stop offset=".5" stop-color="#d9b04a"/><stop offset="1" stop-color="#6e5014"/></radialGradient>
 <linearGradient id="${id}s" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#fff6cf" stop-opacity="0"/><stop offset=".5" stop-color="#fff6cf" stop-opacity=".5"/><stop offset="1" stop-color="#fff6cf" stop-opacity="0"/></linearGradient>
 <linearGradient id="${id}w" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff" stop-opacity=".35"/><stop offset=".5" stop-color="#fff" stop-opacity="0"/></linearGradient>
 <path id="${id}t" d="M50 50 m-42.6 0 a42.6 42.6 0 1 1 85.2 0 a42.6 42.6 0 1 1 -85.2 0" fill="none"/>
 <clipPath id="${id}c"><circle cx="50" cy="50" r="48"/></clipPath></defs>
 <circle cx="50" cy="50" r="48" fill="url(#${id}b)"/>
 <circle cx="50" cy="50" r="47.4" fill="none" stroke="url(#${id}g)" stroke-width="1.8"/>
 <circle cx="50" cy="50" r="40.4" fill="none" stroke="url(#${id}g)" stroke-width=".9"/>
 <text style="font:800 4.5px Archivo,sans-serif;letter-spacing:1.15px" fill="url(#${id}g)"><textPath href="#${id}t" startOffset="0">${rim}</textPath></text>
 <circle cx="50" cy="50" r="37.6" fill="none" stroke="#e8c55c" stroke-width=".7" stroke-dasharray=".5 2.4" opacity=".7"><animateTransform attributeName="transform" type="rotate" from="0 50 50" to="360 50 50" dur="40s" repeatCount="indefinite"/></circle>
 <g fill="url(#${id}g)">${L}</g>
 <path d="M41.5 25 L40 16.5 L44.8 20.2 L50 13.6 L55.2 20.2 L60 16.5 L58.5 25 Z" fill="url(#${id}g)" stroke="#6e5014" stroke-width=".4"/><circle cx="50" cy="13.4" r="1.1" fill="#fff6cf"/><circle cx="40" cy="16.3" r=".8" fill="#fff6cf"/><circle cx="60" cy="16.3" r=".8" fill="#fff6cf"/><rect x="41.6" y="25.2" width="16.8" height="1.6" rx=".8" fill="url(#${id}g)"/>
 <text x="50" y="87.4" text-anchor="middle" style="font:900 8.2px Archivo,sans-serif;letter-spacing:1.4px" fill="url(#${id}g)">001</text>
 <circle cx="50" cy="50" r="23.2" fill="rgba(0,0,0,.45)"/><circle cx="50" cy="50" r="22" fill="url(#${id}h)"/><circle cx="50" cy="50" r="22" fill="none" stroke="#5a410f" stroke-width=".7"/><circle cx="50" cy="50" r="15.6" fill="none" stroke="#7a5a18" stroke-width=".7"/>`;
  for (let i = 0; i < 6; i++) {
    const a = i * Math.PI / 3 + Math.PI / 6;
    s += `<circle cx="${(50+18.8*Math.cos(a)).toFixed(2)}" cy="${(50+18.8*Math.sin(a)).toFixed(2)}" r="1.3" fill="#5a410f"/><circle cx="${(50+18.8*Math.cos(a)-.35).toFixed(2)}" cy="${(50+18.8*Math.sin(a)-.35).toFixed(2)}" r=".55" fill="#fff6cf"/>`
  }
  s += `<circle cx="50" cy="50" r="9.5" fill="#050506"/><circle cx="50" cy="50" r="9.5" fill="none" stroke="url(#${id}g)" stroke-width="1.5"/>
 <g clip-path="url(#${id}c)"><rect x="-40" y="-20" width="26" height="140" fill="url(#${id}s)" transform="rotate(22 50 50)"><animate attributeName="x" values="-60;-60;130" keyTimes="0;.55;1" dur="6s" repeatCount="indefinite"/></rect></g>
 ${spark(20,24,0)}${spark(82,28,.9)}${spark(80,78,1.7)}${spark(19,72,2.2)}
 <path d="M14 44 A37 37 0 0 1 44 13" stroke="url(#${id}w)" stroke-width="6" stroke-linecap="round" fill="none"/>`;
  if (o.ring !== undefined) {
    const C = 2 * Math.PI * 49;
    s += `<circle cx="50" cy="50" r="49" fill="none" stroke="rgba(232,197,92,.3)" stroke-width="2"/><circle id="bigring" cx="50" cy="50" r="49" fill="none" stroke="#f3d77a" stroke-width="2.4" stroke-linecap="round" stroke-dasharray="${C.toFixed(1)}" stroke-dashoffset="${(C*(1-o.ring)).toFixed(1)}" transform="rotate(-90 50 50)"/>`
  }
  return s + '</svg>'
}
{
  const _p = plateSVG;
  plateSVG = function(c, state, o) {
    if (c === 'founder' && state !== 'idle' && state !== 'rest') return founderPlate(o);
    return _p(c, state, o)
  }
}

/* ---------- background art safety: check it really shows, fall back to inline ---------- */
{
  const _art = art;
  art = function(k) {
    _art(k);
    try {
      const im = $('artimg');
      if (!im) return;
      const m = /url\("(.*)"\)/.exec(im.style.backgroundImage || '');
      if (!m) {
        artInline(k);
        return
      }
      const t = new Image();
      t.onload = () => {if (!t.naturalWidth) artInline(k)};
      t.onerror = () => artInline(k);
      t.src = m[1]
    } catch (e) {}
  }
}
function artInline(k) {
  try {
    const s = $('art'), im = $('artimg');
    const u = (ARTC.get(artKey) || '');
    const svg = decodeURIComponent(u.replace(/^data:image\/svg\+xml;charset=utf-8,/, ''));
    if (!svg.startsWith('<svg')) return;
    s.setAttribute('viewBox', `0 0 ${innerWidth} ${innerHeight}`);
    s.innerHTML = svg.replace(/^<svg[^>]*>/, '').replace(/<\/svg>$/, '');
    s.style.display = 'block';
    if (im) im.style.backgroundImage = 'none'
  } catch (e) {}
}
/* ---------- background art ---------- */
const ICONS = {
  dumbbell: '<rect x="4" y="34" width="9" height="32" rx="3"/><rect x="13" y="24" width="12" height="52" rx="4"/><rect x="25" y="45" width="50" height="10" rx="3"/><path d="M33 45v10M38 45v10M62 45v10M67 45v10"/><rect x="75" y="24" width="12" height="52" rx="4"/><rect x="87" y="34" width="9" height="32" rx="3"/>',
  kettlebell: '<path d="M34 46 C30 14 70 14 66 46"/><path d="M40 44 C38 24 62 24 60 44"/><path d="M27 66 C27 46 42 40 50 40 C58 40 73 46 73 66 C73 80 64 88 50 88 C36 88 27 80 27 66Z"/><path d="M34 88h32"/><path d="M38 62 a12 12 0 0 1 10 -10"/>',
  plate: '<circle cx="50" cy="50" r="44"/><circle cx="50" cy="50" r="38"/><circle cx="50" cy="50" r="27"/><circle cx="50" cy="50" r="16"/><circle cx="50" cy="50" r="7"/><circle cx="50" cy="31" r="1.6"/><circle cx="66.5" cy="40.5" r="1.6"/><circle cx="66.5" cy="59.5" r="1.6"/><circle cx="50" cy="69" r="1.6"/><circle cx="33.5" cy="59.5" r="1.6"/><circle cx="33.5" cy="40.5" r="1.6"/>',
  stopwatch: '<circle cx="50" cy="56" r="34"/><circle cx="50" cy="56" r="28"/><rect x="43" y="10" width="14" height="8" rx="2"/><path d="M50 18v4M76 28l5-5M50 56l12-14M50 32v4M74 56h-4M50 80v-4M26 56h4"/><circle cx="50" cy="56" r="3"/>',
  rack: '<path d="M18 92V8M82 92V8M10 92h16M74 92h16"/><path d="M18 36h6v6M82 36h-6v6"/><path d="M2 40h96"/><rect x="6" y="22" width="7" height="36" rx="2"/><rect x="13" y="28" width="5" height="24" rx="2"/><rect x="87" y="22" width="7" height="36" rx="2"/><rect x="82" y="28" width="5" height="24" rx="2"/><path d="M30 40v0M40 37v6M60 37v6"/>',
  rope: '<path d="M20 20 C20 70 80 30 80 80"/><rect x="14" y="6" width="12" height="20" rx="5"/><rect x="74" y="78" width="12" height="20" rx="5"/>'
};
const ART = {
  log: ['dumbbell', 'plate', '#ff3d4a', '#ff9f1c'], prog: ['kettlebell', 'dumbbell', '#2563ff', '#00b7ff'], stats: ['plate', 'kettlebell', '#ff9f1c', '#ffbe0b'], notes: ['stopwatch', 'rope', '#16b65b', '#00c2a8'], plan: ['rack', 'plate', '#7c4dff', '#d14dff'], set: ['plate', 'stopwatch', '#23252b', '#6b7080']
};
let artKey = '';
function art(key) {
  key = key || 'log';
  const s = $('art'), W = innerWidth, H = innerHeight, dk = isDark(), K = dk ? 1.7 : 1;
  artKey = key + W + 'x' + H + (dk ? 'd' : 'l');
  const a = ART[key] || ART.log, c1 = dk && key === 'set' ? '#9aa0ae' : a[2], c2 = dk && key === 'set' ? '#ffffff' : a[3];
  let o = `<defs><filter id="bl" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="8"/></filter><linearGradient id="ag" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${c1}"/><stop offset="1" stop-color="${c2}"/></linearGradient><linearGradient id="lg" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="${c1}" stop-opacity="0"/><stop offset=".3" stop-color="${c1}"/><stop offset=".7" stop-color="${c2}"/><stop offset="1" stop-color="${c2}" stop-opacity="0"/></linearGradient></defs>`;
  const wave = (y0, amp, f, ph, shift) => {
    let d = '';
    for (let k = 0; k <= 160; k++) {
      const t = k / 160, x = -W * .1 + W * 1.2 * t, y = H * y0 + H * amp * Math.sin(t * Math.PI * 2 * f + ph) + shift - (t - .5) * H * .1;
      d += (k ? 'L' : 'M') + x.toFixed(1) + ',' + y.toFixed(1)
    }
    return d
  };
  [[.3, .1, 1, 0], [.62, .13, .8, 2.2]].forEach(w => {
    const d = wave(...w, 0);
    o += `<path d="${d}" fill="none" stroke="url(#lg)" stroke-width="16" opacity="${(.18*K).toFixed(2)}" filter="url(#bl)"/><path d="${d}" fill="none" stroke="url(#lg)" stroke-width="1.5" opacity="${Math.min(.9,.38*K).toFixed(2)}"/><path d="${wave(...w,13)}" fill="none" stroke="url(#lg)" stroke-width="1" opacity=".16"/>`
  });
  const icon = (name, cx, cy, size, rot, op) => {
    const sc = size / 100, tr = `translate(${(cx-size/2).toFixed(1)} ${(cy-size/2).toFixed(1)}) rotate(${rot} ${size/2} ${size/2}) scale(${sc.toFixed(3)})`;
    return `<g transform="${tr}" fill="none" stroke-linecap="round" stroke-linejoin="round"><g stroke="url(#ag)" stroke-width="10" opacity="${(op*.5).toFixed(2)}" filter="url(#bl)" vector-effect="non-scaling-stroke">${ICONS[name].replace(/\/>/g,' vector-effect="non-scaling-stroke"/>')}</g><g stroke="url(#ag)" stroke-width="2" opacity="${op}">${ICONS[name].replace(/\/>/g,' vector-effect="non-scaling-stroke"/>')}</g><g stroke="#fff" stroke-width=".8" opacity="${(op*.9).toFixed(2)}">${ICONS[name].replace(/\/>/g,' vector-effect="non-scaling-stroke"/>')}</g></g>`
  };
  const M = Math.min(W, H), big = Math.max(260, Math.min(560, M * .95));
  o += icon(a[0], W * .86, H * .17, big * .82, -20, Math.min(.9, .55 * K));
  o += icon(a[1], W * .1, H * .74, big * .6, 16, Math.min(.8, .42 * K));
  o += icon(a[0], W * .92, H * .92, big * .38, 24, Math.min(.6, .3 * K));
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">${o}</svg>`;
  let u = ARTC.get(artKey);
  if (!u) {
    u = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
    ARTC.set(artKey, u);
    if (ARTC.size > 14) {
      const k0 = ARTC.keys().next().value;
      ARTC.delete(k0)
    }
  }
  const im = $('artimg');
  if (im) {
    im.style.backgroundImage = `url("${u}")`;
    s.innerHTML = ''
  } else {
    s.setAttribute('viewBox', `0 0 ${W} ${H}`);
    s.innerHTML = o
  }
}
const ARTC = new Map();
{
  const re = () => {try {artKey = ''; art(curArt())} catch (e) {}};
  document.addEventListener('visibilitychange', () => {if (!document.hidden) re()});
  addEventListener('pageshow', re);
  addEventListener('focus', re)
}
let artT = null;
const curArt = () => !st.plan ? 'log' : st.profile ? 'stats' : st.settings ? 'set' : st.tab === 'crew' ? (st.sub.crew === 'arena' ? 'stats' : 'prog') : st.tab === 'stats' && st.sub.stats === 'lifts' ? 'prog' : st.tab === 'plan' && st.sub.plan === 'notes' ? 'notes' : st.tab;
