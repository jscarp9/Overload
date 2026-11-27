/* ========================================================================
   Overload: Startup

   Runs last, once every other file has loaded. The order of these lines matters.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

addEventListener('resize', () => {
  clearTimeout(artT);
  artT = setTimeout(() => art(curArt()), 150)
});
applyTheme();
try {
  const mq = matchMedia('(prefers-color-scheme: dark)');
  (mq.addEventListener ? mq.addEventListener.bind(mq, 'change') : mq.addListener.bind(mq))(() => {if ((st.set.theme || 'auto') === 'auto') {applyTheme(); render()}})
} catch (x) {}
try {if (sessionStorage.getItem('ov-hs')) st.hideSafari = true} catch (x) {}
try {
  if (navigator.storage && navigator.storage.persist) navigator.storage.persisted().then(p => {if (!p) return navigator.storage.persist()}).catch (() => {})
} catch (x) {}
try {
  if ('serviceWorker' in navigator && location.protocol === 'https:') navigator.serviceWorker.register('sw.js').catch (() => {})
} catch (x) {}
render();
/* ---------- 0.13: startup ---------- */
(function boot13() {
  try {
    if (st.plan && (st.set.schema || 0) < 13) {
      IDB.put('pre13', {at: Date.now(), data: backupData()});
      st.set.schema = 13;
      saveSet()
    }
  } catch (e) {}
  if (st.set.acct) {
    setSync('ok');
    setTimeout(() => syncNow(true), 1200);
    startSyncPoll()
  }
  setTimeout(() => {
    if (crewOn()) wsConnect();
    updateBellDot()
  }, 1800);
  addEventListener('online', () => {
    if (st.set.acct) syncNow(true);
    if (crewOn()) wsConnect()
  });
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) {if (ws) {try {ws.close()} catch (e) {}}return}if (st.set.acct) syncNow();
    if (crewOn()) {
      wsConnect();
      setTimeout(sendLive, 500)
    }
  })
})();
boot20();
boot15();
boot14();
boot16();
boot18();
boot19();
boot21();
boot22();
boot23();
boot24();
boot25();
boot26();
loadCounter();
crewLoop();
if (crewOn()) {
  saveCrewId();
  setTimeout(() => syncCrew(true), 2500)
}
document.addEventListener('visibilitychange', () => {if (!document.hidden && crewOn()) syncCrew()});
if (st.plan && !Array.isArray(st.set.seen)) checkUnlocks();
splash().then(() => {
  if (st.plan && st.set.seenVer !== VERSION) {
    st.sheet = {type: 'news'};
    renderSheet()
  } else if (!weekReport()) {checkUnlocks()}checkMissing().then(() => mirrorNow())
});
