'use strict';

// Accessible feature tabs: inactive panels are also hidden from assistive technology.
const featureTabs=[...document.querySelectorAll('.feature-tab')];
const featurePanels=[...document.querySelectorAll('.feature-panel')];
function selectFeature(tab,focus=false){
  featureTabs.forEach(item=>{
    const selected=item===tab;
    item.classList.toggle('active',selected);item.setAttribute('aria-selected',String(selected));item.tabIndex=selected?0:-1;
  });
  featurePanels.forEach(panel=>{
    const selected=panel.dataset.featurePanel===tab.dataset.panel;
    panel.hidden=!selected;panel.classList.toggle('active',selected);
  });
  if(focus){tab.focus();tab.scrollIntoView({block:'nearest',inline:'nearest',behavior:'instant'});}
}
featureTabs.forEach((tab,index)=>{
  tab.addEventListener('click',()=>selectFeature(tab));
  tab.addEventListener('keydown',event=>{
    let next;
    if(event.key==='ArrowRight')next=(index+1)%featureTabs.length;
    if(event.key==='ArrowLeft')next=(index-1+featureTabs.length)%featureTabs.length;
    if(event.key==='Home')next=0;
    if(event.key==='End')next=featureTabs.length-1;
    if(next!==undefined){event.preventDefault();selectFeature(featureTabs[next],true);}
  });
});

const imageDialog=document.querySelector('#image-dialog');
const lightbox=document.querySelector('#lightbox-image');
function showScreenshot(src,alt){lightbox.src=src;lightbox.alt=alt;imageDialog.showModal();document.body.style.overflow='hidden';}
document.querySelectorAll('.screenshot-button').forEach(button=>button.addEventListener('click',()=>{
  const image=button.querySelector('img');showScreenshot(image.getAttribute('src'),image.alt);
}));

imageDialog.querySelector('.dialog-close').addEventListener('click',()=>imageDialog.close());
imageDialog.addEventListener('close',()=>{document.body.style.overflow='';});
imageDialog.addEventListener('click',event=>{
  if(event.target!==imageDialog)return;
  const r=imageDialog.getBoundingClientRect();
  if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)imageDialog.close();
});

// Compact feature previews: animate from the selected card, then reveal the description.
const detailDialog=document.querySelector('#feature-detail-dialog');
const detailImage=document.querySelector('#detail-image');
const detailTitle=document.querySelector('#detail-title');
const detailDescription=document.querySelector('#detail-description');
const detailCopy=detailDialog.querySelector('.detail-description');
const detailContent={
  map:{title:'Map & waypoints',description:'View the map and your saved locations. Search waypoints, show or hide markers, and check their distance and coordinates.'},
  accounts:{title:'Accounts',description:'View your saved accounts and active profile. Switch accounts, preview your player skin, or add a Microsoft account.'},
  'waypoint-editor':{title:'Waypoint editor',description:'Set a waypoint name and its X, Y, and Z coordinates. Choose a marker color, then save your changes.'},
};
let detailOpener,detailRequest=0,detailClosing=false;
const detailAnimations=[];
function stopDetailAnimations(){detailAnimations.splice(0).forEach(animation=>animation.cancel());}
async function openFeatureDetail(button){
  const request=++detailRequest,content=detailContent[button.dataset.detail];
  detailOpener=button;
  detailTitle.textContent=content.title;detailDescription.textContent=content.description;
  detailImage.src=button.dataset.screenshot;detailImage.alt=button.dataset.alt;
  // The thumbnails already load these files; decode before revealing to avoid a flash.
  try{await detailImage.decode();}catch{ /* Keep the image's descriptive alt text if loading fails. */ }
  if(request!==detailRequest)return;
  stopDetailAnimations();detailClosing=false;detailDialog.classList.remove('is-closing');
  detailDialog.showModal();document.body.style.overflow='hidden';
  if(reducedMotion.matches)return;
  const origin=button.getBoundingClientRect(),target=detailDialog.getBoundingClientRect();
  const dx=Math.max(-70,Math.min(70,(origin.x+origin.width/2-target.x-target.width/2)*.12));
  const dy=Math.max(-45,Math.min(45,(origin.y+origin.height/2-target.y-target.height/2)*.12));
  detailAnimations.push(detailDialog.animate([
    {opacity:0,transform:`translate3d(${dx}px,${dy+16}px,0) scale(.88)`},
    {opacity:1,transform:'translate3d(0,-2px,0) scale(1.012)',offset:.72},
    {opacity:1,transform:'translate3d(0,0,0) scale(1)'},
  ],{duration:440,easing:'cubic-bezier(.2,.8,.2,1)'}));
  detailAnimations.push(detailCopy.animate([
    {opacity:0,transform:'translateY(10px)'},
    {opacity:1,transform:'translateY(0)'},
  ],{duration:320,delay:130,easing:'cubic-bezier(.2,.7,.2,1)',fill:'backwards'}));
}
async function closeFeatureDetail(){
  if(!detailDialog.open||detailClosing)return;
  detailClosing=true;++detailRequest;
  stopDetailAnimations();
  if(!reducedMotion.matches){
    detailDialog.classList.add('is-closing');
    const animation=detailDialog.animate([
      {opacity:1,transform:'translateY(0) scale(1)'},
      {opacity:0,transform:'translateY(9px) scale(.97)'},
    ],{duration:170,easing:'cubic-bezier(.4,0,1,1)',fill:'forwards'});
    detailAnimations.push(animation);
    try{await animation.finished;}catch{}
  }
  detailDialog.close();
}
document.querySelectorAll('[data-detail]').forEach(button=>button.addEventListener('click',()=>{void openFeatureDetail(button);}));
detailDialog.querySelector('.detail-close').addEventListener('click',()=>{void closeFeatureDetail();});
detailDialog.addEventListener('cancel',event=>{event.preventDefault();void closeFeatureDetail();});
let detailBackdropPress=false;
detailDialog.addEventListener('pointerdown',event=>{detailBackdropPress=event.target===detailDialog;});
detailDialog.addEventListener('click',event=>{
  if(!detailBackdropPress||event.target!==detailDialog)return;
  const r=detailDialog.getBoundingClientRect();
  if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)void closeFeatureDetail();
});
detailDialog.addEventListener('close',()=>{
  stopDetailAnimations();detailDialog.classList.remove('is-closing');detailClosing=false;
  document.body.style.overflow='';detailOpener?.focus({preventScroll:true});
});

const reducedMotion=window.matchMedia('(prefers-reduced-motion: reduce)');
const particleContainer=document.querySelector('.hero-particles');
if(!reducedMotion.matches){
  const count=window.innerWidth<700?16:30;
  for(let i=0;i<count;i++){
    const particle=document.createElement('span');particle.className='hero-particle';
    particle.style.left=`${Math.random()*100}%`;particle.style.top=`${Math.random()*120}%`;
    particle.style.setProperty('--duration',`${18+Math.random()*20}s`);
    particle.style.setProperty('--fade-duration',`${6+Math.random()*8}s`);
    particle.style.setProperty('--delay',`${Math.random()*-25}s`);
    particle.style.setProperty('--drift',`${-60+Math.random()*120}px`);
    particle.style.setProperty('--opacity',`${.15+Math.random()*.35}`);particleContainer.append(particle);
  }
}
new IntersectionObserver(entries=>{
  particleContainer.style.visibility=entries[0].isIntersecting?'visible':'hidden';
  document.body.classList.toggle('page-hidden',!entries[0].isIntersecting);
}).observe(document.querySelector('.page'));
document.addEventListener('visibilitychange',()=>document.body.classList.toggle('page-hidden',document.hidden));
document.querySelector('#year').textContent=new Date().getFullYear();

// GitHub counts actual release-asset downloads across Mac and Windows, including past releases.
const counterEndpoint=window.AXIAL_COUNTER_ENDPOINT||'download-counter.php';
const downloadCount=document.querySelector('#download-count'),downloadCounter=document.querySelector('.download-counter');
let countRequestPending=false;
async function refreshDownloadCount(){
  if(countRequestPending||!downloadCount||!downloadCounter)return;
  countRequestPending=true;
  try{
    const response=await fetch(new URL(counterEndpoint,location.href),{cache:'no-store',credentials:'omit',signal:AbortSignal.timeout(35000)});
    if(!response.ok||!response.headers.get('content-type')?.includes('application/json'))return;
    const data=await response.json();
    if(!Number.isSafeInteger(data.count)||data.count<0)return;
    downloadCount.textContent=data.count.toLocaleString();downloadCounter.hidden=false;
    downloadCounter.title=data.stale?'Last available download total':'Combined Windows and Mac release downloads';
  }catch{/* Keep the last known total visible; never invent one on failure. */}
  finally{countRequestPending=false;}
}
void refreshDownloadCount();
document.addEventListener('visibilitychange',()=>{if(!document.hidden)void refreshDownloadCount();});
