(function(){
  'use strict';
  // Keep section navigation on the homepage without exposing file names or hashes.
  const cleanPath=location.pathname.replace(/index\.html$/i,'');
  function cleanAddress(){history.replaceState(history.state,'',cleanPath+location.search);}
  function findSection(hash){
    if(!hash||hash==='#')return null;
    try{return document.getElementById(decodeURIComponent(hash.slice(1)));}catch{return null;}
  }
  function scrollToSection(target,smooth){
    target.scrollIntoView({behavior:smooth&&!matchMedia('(prefers-reduced-motion: reduce)').matches?'smooth':'instant',block:'start'});
  }
  document.addEventListener('click',event=>{
    if(event.defaultPrevented||event.button!==0||event.metaKey||event.ctrlKey||event.shiftKey||event.altKey)return;
    const link=event.target.closest('a[href]');
    if(!link||link.hasAttribute('download')||(link.target&&link.target!=='_self'))return;
    const destination=new URL(link.href,location.href);
    if(destination.origin!==location.origin||destination.pathname!==location.pathname||destination.search!==location.search)return;
    const target=findSection(destination.hash);
    if(!target)return;
    event.preventDefault();scrollToSection(target,true);cleanAddress();
    if(link.classList.contains('skip-link')){
      if(!target.hasAttribute('tabindex'))target.setAttribute('tabindex','-1');
      target.focus({preventScroll:true});
    }
  });
  const initialTarget=findSection(location.hash);
  function finishInitialNavigation(){
    if(initialTarget)scrollToSection(initialTarget,false);
    if(location.hash||cleanPath!==location.pathname)cleanAddress();
  }
  if(document.readyState==='complete')finishInitialNavigation();
  else window.addEventListener('load',finishInitialNavigation,{once:true});
  const button=document.querySelector('.nav-toggle'),nav=document.querySelector('#site-navigation');
  if(!button||!nav)return;
  function close(returnFocus=false){
    const wasOpen=button.getAttribute('aria-expanded')==='true';
    button.setAttribute('aria-expanded','false');button.setAttribute('aria-label','Open navigation');nav.classList.remove('is-open');
    if(returnFocus&&wasOpen)button.focus();
  }
  button.addEventListener('click',()=>{
    const open=button.getAttribute('aria-expanded')!=='true';
    button.setAttribute('aria-expanded',String(open));button.setAttribute('aria-label',open?'Close navigation':'Open navigation');nav.classList.toggle('is-open',open);
  });
  nav.querySelectorAll('a').forEach(link=>link.addEventListener('click',()=>close()));
  document.addEventListener('click',event=>{if(!event.target.closest('.site-nav'))close();});
  document.addEventListener('keydown',event=>{if(event.key==='Escape')close(true);});
  matchMedia('(max-width: 900px)').addEventListener('change',()=>close());
})();
