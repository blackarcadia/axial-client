/* Subtle depth on the supplied hero artwork; no colour or opacity changes. */
(function(){
  'use strict';
  const hero=document.querySelector('.page');if(!hero)return;
  const media=matchMedia('(prefers-reduced-motion: reduce)');
  let targetX=0,targetY=0,scrollY=0,x=0,y=0,frame,visible=true;
  function render(){
    frame=undefined;
    if(media.matches||document.hidden||!visible)return;
    x+=(targetX-x)*.09;y+=(targetY+scrollY-y)*.09;
    hero.style.setProperty('--hero-x',`${x.toFixed(3)}px`);
    hero.style.setProperty('--hero-y',`${y.toFixed(3)}px`);
    if(Math.abs(targetX-x)>.02||Math.abs(targetY+scrollY-y)>.02)frame=requestAnimationFrame(render);
  }
  function start(){if(!frame&&visible&&!media.matches&&!document.hidden)frame=requestAnimationFrame(render);}
  function reset(){targetX=0;targetY=0;start();}
  hero.addEventListener('pointermove',event=>{
    if(event.pointerType!=='mouse'||media.matches)return;
    const rect=hero.getBoundingClientRect();
    targetX=((event.clientX-rect.left)/rect.width-.5)*-14;
    targetY=((event.clientY-rect.top)/rect.height-.5)*-10;
    start();
  });
  hero.addEventListener('pointerleave',reset);
  function onScroll(){
    if(!visible)return;
    const rect=hero.getBoundingClientRect();
    scrollY=Math.min(10,Math.max(0,-rect.top*.025));start();
  }
  window.addEventListener('scroll',onScroll,{passive:true});
  window.addEventListener('blur',reset);
  document.addEventListener('visibilitychange',()=>{
    if(document.hidden){cancelAnimationFrame(frame);frame=undefined;targetX=0;targetY=0;}
    else start();
  });
  new IntersectionObserver(entries=>{
    visible=entries[0].isIntersecting;
    if(visible)onScroll();else{cancelAnimationFrame(frame);frame=undefined;}
  }).observe(hero);
  media.addEventListener('change',()=>{
    cancelAnimationFrame(frame);frame=undefined;
    if(media.matches){x=y=targetX=targetY=scrollY=0;hero.style.removeProperty('--hero-x');hero.style.removeProperty('--hero-y');}
    else onScroll();
  });
})();
