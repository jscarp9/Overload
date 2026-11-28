/* ========================================================================
   Overload: Polish and selfie posts (0.26)

   Line icons that replace UI emojis as the page renders, and selfie photos on crew posts:
   picking, shrinking, uploading, viewing, saving and the Progress photos library.
   Wraps postHTML, postData and sheet15, so it must load after 13-extensions.js.

   Loaded by index.html as a plain script, in file-number order. Everything declared at the
   top level is shared with the other files. Functions can be called from any file once the
   page has loaded, but code that runs while loading can only use earlier files.
   ======================================================================== */

/* ---------- 0.26: polish, cleaner crew, selfie posts ---------- */
/* line icon set: replaces UI emojis so the app reads calm and professional */
const IC27={
 flame:'<path d="M12 3c1 3.5 5 5.4 5 10a5 5 0 0 1-10 0c0-2.2 1-3.6 2.2-4.8.3 1.6 1 2.6 2.1 3.1C11 9 11 6 12 3z"/>',
 lock:'<rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>',
 unlock:'<rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 7.6-1.7"/>',
 crown:'<path d="M3 8l4.5 4L12 5l4.5 7L21 8l-2 11H5z"/>',
 dumbbell:'<path d="M6.5 7v10M3.5 9.5v5M17.5 7v10M20.5 9.5v5M6.5 12h11"/>',
 trophy:'<path d="M8 4h8v5a4 4 0 0 1-8 0zM8 6H5a3 3 0 0 0 3 4.5M16 6h3a3 3 0 0 1-3 4.5M12 13v4M8.5 20.5h7M10 17h4v3.5h-4z"/>',
 camera:'<path d="M4 8h3l2-2.5h6L17 8h3a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z"/><circle cx="12" cy="13" r="3.5"/>',
 trend:'<path d="M3 17l6-6 4 4 8-8M15 7h6v6"/>',
 megaphone:'<path d="M4 10v4a1 1 0 0 0 1 1h2l7 4V5L7 9H5a1 1 0 0 0-1 1zM17.5 9a4 4 0 0 1 0 6M8 15l1 5"/>',
 target:'<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1.2"/>',
 cloud:'<path d="M7 18h10a4 4 0 0 0 .5-7.97A6 6 0 0 0 6 9.5 4.25 4.25 0 0 0 7 18z"/>',
 gift:'<path d="M4 9h16v4H4zM5 13v7h14v-7M12 9v11M12 9H8.5a2.5 2.5 0 1 1 0-5C11 4 12 9 12 9zM12 9h3.5a2.5 2.5 0 1 0 0-5C13 4 12 9 12 9z"/>',
 users:'<circle cx="9" cy="8" r="3.5"/><path d="M3 20a6 6 0 0 1 12 0M16 4.6a3.5 3.5 0 0 1 0 6.8M18 14.2a5.5 5.5 0 0 1 3 5.8"/>',
 userplus:'<circle cx="9" cy="8" r="4"/><path d="M2 20a7 7 0 0 1 14 0M19 8v6M16 11h6"/>',
 user:'<circle cx="12" cy="8" r="4"/><path d="M4.5 20a7.5 7.5 0 0 1 15 0"/>',
 alert:'<path d="M12 4 2.5 20h19zM12 10v4M12 17.2v.1"/>',
 clipboard:'<rect x="5.5" y="4.5" width="13" height="16.5" rx="2"/><path d="M9 3.5h6v3H9zM9 11h6M9 15h4"/>',
 swords:'<path d="M4 4l9.5 9.5M4 4h4.5M4 4v4.5M20 4l-9.5 9.5M20 4h-4.5M20 4v4.5M6.5 14.5l3 3M17.5 14.5l-3 3M5 19l2.5-2.5M19 19l-2.5-2.5"/>',
 palette:'<path d="M12 3a9 9 0 0 0 0 18c1.2 0 1.8-.8 1.8-1.7 0-1.3-1-1.6-1-2.6s.8-1.7 2-1.7H17a4 4 0 0 0 4-4c0-4.4-4-8-9-8z"/><circle cx="7.5" cy="11.5" r="1"/><circle cx="10" cy="7.5" r="1"/><circle cx="15" cy="7.8" r="1"/>',
 pencil:'<path d="M4 20h4L19 9l-4-4L4 16zM14 6l4 4"/>',
 note:'<rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 8h6M9 12h6M9 16h3"/>',
 map:'<path d="M3 6l6-2 6 2 6-2v14l-6 2-6-2-6 2zM9 4v14M15 6v14"/>',
 flag:'<path d="M5 21V4M5 4h12l-2.5 4L17 12H5"/>',
 medal:'<circle cx="12" cy="15" r="5.5"/><path d="M8.5 3h7l-2 7.2M8.5 3l2 7.2M12 12.6l.8 1.6 1.7.2-1.2 1.2.3 1.7-1.6-.8-1.6.8.3-1.7-1.2-1.2 1.7-.2z"/>',
 star:'<path d="M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z"/>',
 spark:'<path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8zM18.5 15.5l.7 1.8 1.8.7-1.8.7-.7 1.8-.7-1.8-1.8-.7 1.8-.7z"/>',
 bolt:'<path d="M13 2.5 4.5 14H11l-1 7.5L18.5 10H12z"/>',
 chat:'<path d="M4 5h16v11H9.5L5 20v-4H4z"/>',
 check:'<circle cx="12" cy="12" r="9"/><path d="M8 12.3l2.8 2.8L16.2 9.4"/>',
 snow:'<path d="M12 2.5v19M3.8 7.2l16.4 9.6M20.2 7.2 3.8 16.8M9.5 4.5 12 7l2.5-2.5M9.5 19.5 12 17l2.5 2.5"/>',
 gear:'<circle cx="12" cy="12" r="3"/><path d="M12 2.8v2.4M12 18.8v2.4M2.8 12h2.4M18.8 12h2.4M5.5 5.5l1.7 1.7M16.8 16.8l1.7 1.7M5.5 18.5l1.7-1.7M16.8 7.2l1.7-1.7"/><circle cx="12" cy="12" r="6.6"/>',
 shield:'<path d="M12 3l8 3v6c0 4.5-3.4 7.7-8 9-4.6-1.3-8-4.5-8-9V6z"/>',
 key:'<circle cx="8" cy="15" r="4"/><path d="M11 12l9-9M16.5 6.5l3 3M14 9l2 2"/>',
 chart:'<path d="M5 20V11M11 20V5M17 20v-6M3 20.5h18"/>',
 landmark:'<path d="M3 21h18M5 18h14M6.5 18v-7M10 18v-7M14 18v-7M17.5 18v-7M3.5 10h17L12 4z"/>',
 save:'<path d="M5 3.5h11l3 3V20.5H5zM8 3.5v5h7v-5M8 20.5V15h8v5.5"/>',
 film:'<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 9h18M3 15h18M8 4v16M16 4v16"/>',
 timer:'<circle cx="12" cy="13.5" r="7.5"/><path d="M12 10v3.5l2.5 2M10 2.5h4"/>',
 moon:'<path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"/>',
 sun:'<circle cx="12" cy="12" r="4"/><path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"/>',
 cal:'<rect x="3.5" y="5" width="17" height="16" rx="2"/><path d="M3.5 10h17M8 3v4M16 3v4"/>',
 repeat:'<path d="M4 12a8 8 0 0 1 13.7-5.6M20 4v5h-5M20 12a8 8 0 0 1-13.7 5.6M4 20v-5h5"/>',
 trash:'<path d="M4 7h16M9 7V4h6v3M6 7l1 14h10l1-14"/>',
 bell:'<path d="M6 16v-5a6 6 0 0 1 12 0v5l2 2H4zM10 21h4"/>',
 pin:'<path d="M12 21s7-6.3 7-12a7 7 0 0 0-14 0c0 5.7 7 12 7 12z"/><circle cx="12" cy="9" r="2.5"/>',
 skull:'<path d="M12 3a8 8 0 0 0-8 8c0 2.6 1.3 4.3 3 5.4V20h10v-3.6c1.7-1.1 3-2.8 3-5.4a8 8 0 0 0-8-8zM10.5 20v-2M13.5 20v-2"/><circle cx="9" cy="11" r="1.6"/><circle cx="15" cy="11" r="1.6"/>',
 dice:'<rect x="4" y="4" width="16" height="16" rx="3"/><circle cx="9" cy="9" r="1"/><circle cx="15" cy="15" r="1"/><circle cx="15" cy="9" r="1"/><circle cx="9" cy="15" r="1"/>',
 id:'<rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="9" cy="11" r="2.2"/><path d="M5.8 16a3.2 3.2 0 0 1 6.4 0M14.5 10h4M14.5 14h3"/>',
 disc:'<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="2.5"/>',
 heart:'<path d="M12 20s-8-4.6-8-10.5A4.5 4.5 0 0 1 12 7a4.5 4.5 0 0 1 8 2.5C20 15.4 12 20 12 20z"/>',
 rocket:'<path d="M12 15l-3-3c1.5-5 4.5-8 10-9-.9 5.5-4 8.5-9 10M9 12l-4 1 2-4h4M12 15l-1 4 4-2v-4M5.5 18.5l2-2"/>',
 help:'<circle cx="12" cy="12" r="9"/><path d="M9.5 9.5a2.6 2.6 0 0 1 5 .9c0 1.7-2.5 2.2-2.5 3.6M12 17v.1"/>',
 buoy:'<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="4"/><path d="M5.6 5.6l3.6 3.6M14.8 14.8l3.6 3.6M18.4 5.6l-3.6 3.6M9.2 14.8l-3.6 3.6"/>',
 eye:'<path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"/><circle cx="12" cy="12" r="3"/>',
 link:'<path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/>',
 music:'<path d="M9 18V5l11-2v13"/><circle cx="6.5" cy="18" r="2.5"/><circle cx="17.5" cy="16" r="2.5"/>',
 image:'<rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="9.5" r="1.8"/><path d="M3 17l5-5 4 4 3-3 6 6"/>',
 phone:'<rect x="6.5" y="2.5" width="11" height="19" rx="2.5"/><path d="M11 18.5h2"/>',
 folder:'<path d="M3 6.5a1.5 1.5 0 0 1 1.5-1.5H9l2 2.5h8.5A1.5 1.5 0 0 1 21 9v9.5a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18.5z"/>',
 sound:'<path d="M4 9.5v5h3.5L13 19V5L7.5 9.5zM16.5 9a4 4 0 0 1 0 6M19 6.5a7.5 7.5 0 0 1 0 11"/>',
 compass:'<circle cx="12" cy="12" r="9"/><path d="M15.5 8.5l-2 5-5 2 2-5z"/>',
 clock:'<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
 x:'<path d="M6 6l12 12M18 6 6 18"/>',
 download:'<path d="M12 4v11M7 10.5l5 5 5-5M5 20h14"/>',
 ticket:'<path d="M3 8a2 2 0 0 0 2-2h14a2 2 0 0 0 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 0-2 2H5a2 2 0 0 0-2-2v-2a2 2 0 0 0 0-4z"/><path d="M14.5 6v12" stroke-dasharray="2 2"/>',
 ladder:'<path d="M7 3v18M17 3v18M7 7.5h10M7 12h10M7 16.5h10"/>',
 grid:'<rect x="3.5" y="3.5" width="17" height="17" rx="2"/><path d="M3.5 9.2h17M3.5 14.8h17M9.2 3.5v17M14.8 3.5v17"/>'};
const ic27=(n,c)=>IC27[n]?`<span class="ic27${c?' '+c:''}" aria-hidden="true"><svg viewBox="0 0 24 24">${IC27[n]}</svg></span>`:'';
const EM27={'🔥':'flame','🔒':'lock','🔓':'unlock','👑':'crown','🏋️':'dumbbell','🏋':'dumbbell','💪':'dumbbell','🦵':'dumbbell','🏆':'trophy','💯':'trophy','📸':'camera','📷':'camera','📈':'trend','📣':'megaphone','📢':'megaphone','🎯':'target','☁️':'cloud','☁':'cloud','🎁':'gift','📦':'gift','👥':'users','🏟️':'users','🏟':'users','👯':'users','🫂':'users','🤝':'users','👋':'userplus','🙂':'user','⚠️':'alert','⚠':'alert','📋':'clipboard','⚔️':'swords','⚔':'swords','🎨':'palette','✎':'pencil','✏️':'pencil','✍️':'pencil','📝':'note','🗺️':'map','🗺':'map','🏁':'flag','🚩':'flag','🎖️':'medal','🎖':'medal','🏅':'medal','🥇':'medal','🥈':'medal','🥉':'medal','⭐':'star','🌟':'star','★':'star','⚡':'bolt','💬':'chat','🎉':'spark','✨':'spark','🤖':'spark','🎆':'spark','🎇':'spark','✅':'check','❄️':'snow','❄':'snow','⚙️':'gear','⚙':'gear','⚙︎':'gear','🛡️':'shield','🛡':'shield','🔑':'key','🗝️':'key','📊':'chart','🏗️':'chart','🏗':'chart','🏛️':'landmark','🏛':'landmark','💾':'save','🎬':'film','⏱️':'timer','⏱':'timer','⏳':'timer','🌙':'moon','😴':'moon','☀️':'sun','🗓️':'cal','🗓':'cal','📆':'cal','📅':'cal','🔄':'repeat','🔁':'repeat','🗑':'trash','🗑️':'trash','🔔':'bell','📌':'pin','📍':'pin','💀':'skull','🎲':'dice','🪪':'id','💿':'disc','❤️':'heart','🩷':'heart','💗':'heart','🚀':'rocket','❔':'help','❓':'help','🛟':'buoy','👀':'eye','🔗':'link','🎵':'music','🖼':'image','🖼️':'image','📱':'phone','📂':'folder','🗂':'folder','🗂️':'folder','🔊':'sound','🧭':'compass','🕛':'clock','🎟':'ticket','🎟️':'ticket','🎫':'ticket','🪜':'ladder','🎬':'film'};
const RX27=new RegExp('('+Object.keys(EM27).sort((a,b)=>b.length-a.length).map(k=>k.replace(/[.*+?^${}()|[\]\\]/g,'\\$&')).join('|')+')\\uFE0F?','gu');
/* places where emojis are the user's own words or are reactions: leave them alone */
const SKIP27='svg,textarea,input,select,script,style,.pcap,.pcm,.bub,.mrx,.mrep,.rx,[data-react],[data-mreact],.ma25,.mu25,.aip25 p,.motto,[data-noic],.rxpick,.emo,.emote';
function polish27(root){if(!root||root.nodeType!==1)return;if(root.closest&&root.closest('svg'))return;
 const tw=document.createTreeWalker(root,NodeFilter.SHOW_TEXT),hit=[];let n;
 while((n=tw.nextNode())){const v=n.nodeValue;if(!v||v.length>600)continue;RX27.lastIndex=0;if(!RX27.test(v))continue;const pe=n.parentElement;if(!pe||pe.closest(SKIP27))continue;hit.push(n)}
 hit.forEach(t=>{const pe=t.parentElement,toastEl=pe&&pe.id==='toast';RX27.lastIndex=0;
  const html=esc(t.nodeValue).replace(RX27,m=>{const k=EM27[m]||EM27[m.replace(/️/g,'')];return toastEl?'':ic27(k)}).replace(/^\s+|\s+$/g,s=>s.replace(/\s+/g,' '));
  if(toastEl){pe.textContent=t.nodeValue.replace(RX27,'').replace(/\s{2,}/g,' ').trim();return}
  const sp=document.createElement('span');sp.className='em27';sp.innerHTML=html;t.replaceWith(sp)})}
let ob27=null;
function watch27(){if(ob27||!window.MutationObserver)return;ob27=new MutationObserver(ms=>{for(const m of ms){if(m.type==='characterData'){const p=m.target.parentElement;if(p)polish27(p)}else m.addedNodes.forEach(x=>{if(x.nodeType===1)polish27(x);else if(x.nodeType===3&&x.parentElement)polish27(x.parentElement)})}ph27Fill()});
 ob27.observe(document.body,{childList:true,subtree:true,characterData:true});polish27(document.body)}

/* ---------- selfie posts ---------- */
const PH27={},PH27L={},PH27K='overload-ph27',PH27_KEEP=183*864e5;
function ph27List(){try{return JSON.parse(localStorage.getItem(PH27K)||'[]')}catch(e){return []}}
function ph27Put(a){try{localStorage.setItem(PH27K,JSON.stringify(a.slice(-500)))}catch(e){}}
async function ph27Get(pid){if(PH27[pid])return PH27[pid];if(PH27L[pid])return null;PH27L[pid]=1;let d=null;
 try{d=await IDB.get('ph27:'+pid);if(!d&&crewOn()){const r=await crewCall('photo/get',{pid});if(r&&r.img)d='data:image/jpeg;base64,'+r.img;else if(r&&r.gone)d='gone'}}catch(e){}
 PH27L[pid]=0;if(d){PH27[pid]=d;ph27Fill()}return d}
function ph27Fill(){document.querySelectorAll('[data-ph27]').forEach(el=>{const pid=el.dataset.ph27,d=PH27[pid];
 if(d==='gone'){el.classList.add('gone27');return}
 if(d){const im=el.querySelector('img');if(!im){const i=document.createElement('img');i.alt='Workout photo';i.decoding='async';i.src=d;el.prepend(i);const s=el.querySelector('.sh27');if(s)s.remove()}return}
 if(!PH27L[pid])ph27Get(pid)})}
async function ph27Shrink(file){const url=URL.createObjectURL(file);try{const im=await new Promise((res,rej)=>{const i=new Image();i.onload=()=>res(i);i.onerror=rej;i.src=url});
 const W0=im.naturalWidth,H0=im.naturalHeight;if(!W0||!H0)throw new Error('bad');
 // crop to 4:5 portrait, the way it shows in the feed
 let sw=W0,sh=H0;if(W0/H0>0.8)sw=Math.round(H0*0.8);else sh=Math.round(W0/0.8);const sx=Math.round((W0-sw)/2),sy=Math.round((H0-sh)/2);
 let w=Math.min(864,sw),q=.8,d='';for(let t=0;t<8;t++){const h=Math.round(w/0.8),cv=document.createElement('canvas');cv.width=w;cv.height=h;const x=cv.getContext('2d');x.drawImage(im,sx,sy,sw,sh,0,0,w,h);d=cv.toDataURL('image/jpeg',q);if(d.length<=200000)break;if(q>.55)q-=.1;else w=Math.round(w*.8)}
 return d.startsWith('data:image/jpeg')?d:''}catch(e){return ''}finally{URL.revokeObjectURL(url)}}
function ph27Stats(d){const u=d.unit||U(),vol=toMine(d.vol||0,u);return `<div class="phst27"><span><b>${d.min||'–'}</b> min</span><span><b>${d.sets||0}</b> sets</span><span><b>${kfmt(vol)}</b> ${U()}</span>${d.prs?`<span><b>${d.prs}</b> PR${d.prs>1?'s':''}</span>`:''}</div>`}
function ph27Block(p){const ph=p.photo;if(!ph||!ph.pid)return '';const now=Date.now();if(ph.exp&&now>ph.exp)return '';
 const d=PH27[ph.pid],own=p.by===myId()||p.id==='x',left=ph.exp?Math.ceil((ph.exp-now)/864e5):99;
 return `<div class="ph27${d==='gone'?' gone27':''}" data-ph27="${esc(ph.pid)}">${d&&d!=='gone'?`<img src="${d}" alt="Workout photo" decoding="async">`:'<i class="sh27"></i>'}${ph27Stats(p.data||{})}${p.id!=='x'?`<button class="phopen27" data-ph27open="${esc(p.id)}" aria-label="Open photo"></button>`:''}</div>${own&&p.id!=='x'&&left<=14?`<div class="phw27">${ic27('clock')}<span>Photo leaves the feed in ${left} day${left===1?'':'s'}. It stays in your Progress photos.</span><button class="btn sm glass" data-ph27save="${esc(ph.pid)}">Save</button></div>`:''}`}
{const _ph=postHTML;postHTML=function(p,preview){let h=_ph.apply(this,arguments);try{if(!h)return h;
 h=h.replace('<div class="post','<div class="post p27'+(p.photo&&p.photo.pid&&!(p.photo.exp&&Date.now()>p.photo.exp)?' hasph27':''));
 if(p.type==='workout'){const d=p.data||{},blk=ph27Block(p),cn=d.coach&&st.set.coachPost27!==false?`<div class="cn27">${ic27('spark')}<div><b>Coach</b><p>${esc(d.coach)}</p></div></div>`:'';
  if(blk){const i=h.indexOf('<div class="pcard"');if(i>0)h=h.slice(0,i)+blk+h.slice(i)}
  if(cn){const i=h.indexOf('<div class="prx">');if(i>0)h=h.slice(0,i)+cn+h.slice(i);else h=h.replace(/<\/div>$/,cn+'</div>')}}
 if(p.type==='join')h=h.replace('Joined the crew 👋','Joined the crew');}catch(e){}return h}}
/* coach's workout recap rides along on the post (already written, no extra AI call) */
{const _pd=postData;postData=function(s){const d=_pd.apply(this,arguments);try{const k=Object.keys(st.sessions).find(k=>st.sessions[k]===s);const t=k&&AI23&&AI23.recaps&&AI23.recaps[k];if(t&&st.set.coachPost27!==false)d.coach=String(t).slice(0,400)}catch(e){}return d}}

function post27Sheet(sh){const s=st.sessions[sh.key];if(!s)return '';const data=postData(s),fake={id:'x',by:myId(),ts:Date.now(),type:'workout',cap:sh.cap||'',data};
 if(sh.img){PH27._prev=sh.img;fake.photo={pid:'_prev',exp:Date.now()+PH27_KEEP}}
 return `<div class="ghd"><h2>Post to crew</h2><button class="btn sm" id="sheetx">Cancel</button></div>
 ${sh.img?`<div class="phpick27 has"><div class="ph27 big" data-ph27="_prev"><img src="${sh.img}" alt="Your photo">${ph27Stats(data)}</div><div class="phrow27"><button class="btn sm glass" id="ph27cam">${ic27('camera')} Retake</button><button class="btn sm glass" id="ph27lib">${ic27('image')} Library</button><button class="btn sm glass" id="ph27x">${ic27('x')} Remove</button></div></div>`
 :`<div class="phpick27"><button class="phadd27" id="ph27cam">${ic27('camera')}<b>Add a selfie</b><span>Your workout stats go on the photo</span></button><button class="phlib27" id="ph27lib">${ic27('image')} Choose from library</button></div>`}
 <p class="phnote27">${ic27('lock')} Only your crew sees photos. They stay up for 6 months and stay in your Progress photos on this phone.</p>
 ${sh.busy?'<div class="phbusy27"><i class="spin27"></i> Getting your photo ready…</div>':''}
 <textarea id="capin" class="field" maxlength="280" rows="2" placeholder="Say something about it (optional)">${esc(sh.cap||'')}</textarea>
 <div class="glbl" style="margin:14px 0 6px">PREVIEW</div><div class="prev27">${postHTML(fake,true)}</div>
 <button class="btn acc wide gold27" id="dopost" style="min-height:52px">${sh.img?'Post with photo':'Post'}</button>`}
function ph27Pick(cam){const i=$(cam?'ph27f1':'ph27f2');if(!i)return;i.value='';i.click()}
async function ph27Picked(file){const sh=st.sheet;if(!sh||sh.type!=='post'||!file)return;const c=$('capin');if(c)sh.cap=c.value;sh.busy=1;renderSheet();
 const d=await ph27Shrink(file);sh.busy=0;if(!d){toast('Could not read that photo');renderSheet();return}sh.img=d;try{haptic('tap')}catch(e){}renderSheet()}
async function post27Go(b){const sh=st.sheet,s=st.sessions[sh.key];if(!s)return true;const cap=(($('capin')||{}).value||'').trim();sh.cap=cap;b.disabled=true;b.textContent=sh.img?'Uploading photo…':'Posting…';
 let photo=null;
 if(sh.img){const r=await crewCall('photo/up',{img:sh.img.split(',')[1]});if(!r||r.error||!r.pid){b.disabled=false;b.textContent='Post with photo';toast(r&&r.error&&r.error.length<80&&!/^HTTP/.test(r.error)?r.error:'Could not upload the photo. Check your connection');return true}
  photo=r.pid;PH27[r.pid]=sh.img;try{await IDB.put('ph27:'+r.pid,sh.img)}catch(e){}const L=ph27List();L.push({pid:r.pid,date:s.date,day:postData(s).day,ts:Date.now(),exp:r.exp});ph27Put(L)}
 const post={type:'workout',cap,data:postData(s)};if(photo)post.photo=photo;
 const r=await crewCall('post',{post});b.disabled=false;if(r.error){b.textContent=sh.img?'Post with photo':'Post';toast('Could not post. Check your connection');return true}
 s.posted=true;saveS();st.sheet=null;renderSheet();st.tab='crew';st.sub.crew='feed';render();window.scrollTo({top:0});toast(photo?'Posted with your photo':'Posted to your crew');return true}
async function ph27SaveFile(pid){const d=PH27[pid]||await IDB.get('ph27:'+pid)||await ph27Get(pid);if(!d||d==='gone'){toast('That photo is no longer available');return}
 try{const blob=await (await fetch(d)).blob(),f=new File([blob],'overload-'+pid.slice(0,6)+'.jpg',{type:'image/jpeg'});if(navigator.canShare&&navigator.canShare({files:[f]})){await navigator.share({files:[f]});return}}catch(e){if(e&&e.name==='AbortError')return}
 const a=document.createElement('a');a.href=d;a.download='overload-'+pid.slice(0,6)+'.jpg';document.body.appendChild(a);a.click();a.remove()}
function ph27View(sh){const p=(st.cs&&st.cs.posts||[]).find(x=>x.id===sh.post),pid=sh.pid||(p&&p.photo&&p.photo.pid);if(!pid)return '';
 const mine=p?p.by===myId():true,owner=st.cs&&st.cs.crew.owner===myId(),d=PH27[pid],m=p?(memberOf2(p.by)||{name:'Former member'}):null,left=p&&p.photo&&p.photo.exp?Math.max(0,Math.ceil((p.photo.exp-Date.now())/864e5)):null,loc=sh.loc;
 return `<div class="ghd"><h2>${m?esc(m.name):'Progress photo'}</h2><button class="btn sm" id="sheetx">Done</button></div>
 <div class="ph27 big" data-ph27="${esc(pid)}">${d&&d!=='gone'?`<img src="${d}" alt="Workout photo">`:'<i class="sh27"></i>'}${p?ph27Stats(p.data||{}):''}</div>
 <p class="sub" style="margin:10px 2px">${p?esc((p.data&&p.data.day)||'Workout')+' · '+ago(p.ts):loc?esc(loc.day||'Workout')+' · '+esc(loc.date||''):''}${left!==null?` · in the feed ${left} more day${left===1?'':'s'}`:''}</p>
 <div class="phrow27"><button class="btn glass" data-ph27save="${esc(pid)}">${ic27('download')} Save to phone</button>${p&&(mine||owner)?`<button class="btn glass" data-ph27del="${esc(pid)}">${ic27('trash')} Remove from feed</button>`:''}${!p&&loc?`<button class="btn glass" data-ph27forget="${esc(pid)}">${ic27('trash')} Delete from this phone</button>`:''}</div>
 ${sh.confirm27?`<div class="conf27"><p>${p?'Take this photo off the crew feed? It goes to the crew Trash for 30 days, so you can bring it back.':'Delete this photo from this phone? If it is still in the crew feed it stays there.'}</p><div class="phrow27"><button class="btn" data-ph27no="1">Keep it</button><button class="btn danger27" data-ph27yes="${esc(pid)}">${p?'Remove':'Delete'}</button></div></div>`:''}`}
function ph27Lib(){const L=ph27List().slice().reverse();
 return `<div class="ghd"><h2>Progress photos</h2><button class="btn sm" id="sheetx">Done</button></div><p class="sub" style="margin:0 2px 12px">Every selfie you post is kept here on this phone, even after it leaves the crew feed.</p>
 ${L.length?`<div class="phgrid27">${L.map(x=>`<button class="phg27" data-ph27lib="${esc(x.pid)}"><span class="ph27" data-ph27="${esc(x.pid)}">${PH27[x.pid]&&PH27[x.pid]!=='gone'?`<img src="${PH27[x.pid]}" alt="">`:'<i class="sh27"></i>'}</span><small>${esc(x.date?x.date.slice(5).replace('-','/'):'')}</small></button>`).join('')}</div>`
 :`<div class="empty27">${ic27('camera')}<b>No photos yet</b><p class="sub">Finish a workout, tap Post to crew and add a selfie. It shows up here.</p></div>`}`}
{const _s=sheet15;sheet15=function(sh){if(sh&&sh.type==='post'&&crewOn())return post27Sheet(sh);if(sh&&sh.type==='ph27view')return ph27View(sh);if(sh&&sh.type==='ph27lib')return ph27Lib(sh);return _s.apply(this,arguments)}}
async function sheetClick27(b){const sh=st.sheet,d=b.dataset;if(!sh)return false;
 if(sh.type==='post'){if(b.id==='ph27cam'){ph27Pick(true);return true}if(b.id==='ph27lib'){ph27Pick(false);return true}
  if(b.id==='ph27x'){const c=$('capin');if(c)sh.cap=c.value;sh.img=null;renderSheet();return true}
  if(b.id==='dopost')return post27Go(b)}
 if(d.ph27save){ph27SaveFile(d.ph27save);return true}
 if(d.ph27del||d.ph27forget){sh.confirm27=1;renderSheet();return true}
 if(d.ph27no){sh.confirm27=0;renderSheet();return true}
 if(d.ph27yes){const pid=d.ph27yes;if(sh.type==='ph27view'&&sh.post){b.disabled=true;const r=await crewCall('photo/del',{pid});if(r.error){b.disabled=false;toast('Could not remove it. Check your connection');return true}PH27[pid]='gone';st.sheet=null;renderSheet();render();toast('Photo moved to crew Trash for 30 days');return true}
  await IDB.put('ph27:'+pid,null);delete PH27[pid];ph27Put(ph27List().filter(x=>x.pid!==pid));st.sheet={type:'ph27lib'};renderSheet();toast('Deleted from this phone');return true}
 if(d.ph27lib){const x=ph27List().find(q=>q.pid===d.ph27lib);st.sheet={type:'ph27view',pid:d.ph27lib,loc:x||{},back:{type:'ph27lib'}};renderSheet();return true}
 return false}
async function click27(b){const d=b.dataset;
 if(d.ph27open){const p=(st.cs&&st.cs.posts||[]).find(x=>x.id===d.ph27open);if(!p||!p.photo)return true;st.sheet={type:'ph27view',post:p.id};renderSheet();return true}
 if(d.ph27save){ph27SaveFile(d.ph27save);return true}
 if(b.id==='ph27libbtn'){st.sheet={type:'ph27lib'};renderSheet();return true}
 return false}
function boot27(){try{['ph27f1','ph27f2'].forEach((id,i)=>{if($(id))return;const f=document.createElement('input');f.type='file';f.accept='image/*';f.id=id;f.hidden=true;if(!i)f.setAttribute('capture','user');f.addEventListener('change',()=>{const x=f.files&&f.files[0];if(x)ph27Picked(x)});document.body.appendChild(f)})}catch(e){}
 // keep own photo list in step with local copies
 watch27()}
