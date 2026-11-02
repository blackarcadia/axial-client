(function(){
  'use strict';
  const core=window.AxialHudCore,editor=document.querySelector('#hud-editor');
  if(!core||!editor)return;
  const viewport=document.querySelector('#hud-viewport'),scene=document.querySelector('#hud-scene');
  const widgets=[...editor.querySelectorAll('[data-hud]')];
  const dialog=document.querySelector('#hud-preview-dialog'),home=document.querySelector('#hud-editor-home');
  const expand=document.querySelector('#hud-expand'),done=document.querySelector('#hud-done');
  const status=document.querySelector('#hud-status'),saveState=document.querySelector('#hud-save-state');
  const storageKey='axial.hud-layout.v1';
  let layout=core.defaults(),selected=null,drag=null,scale=1,canSave=true;
  try{const saved=localStorage.getItem(storageKey);if(saved)layout=core.restore(JSON.parse(saved));}catch{canSave=false;}
  function updateStatus(){
    saveState.textContent=canSave?'LAYOUT SAVED IN THIS BROWSER':'PREVIEW ONLY · BROWSER STORAGE UNAVAILABLE';
    if(selected){const p=layout[selected];saveState.textContent=`${core.elements[selected].name} · X ${p.x} / Y ${p.y}`;}
  }
  function save(){try{localStorage.setItem(storageKey,JSON.stringify(layout));canSave=true;}catch{canSave=false;}updateStatus();}
  function render(){
    widgets.forEach(widget=>{
      const id=widget.dataset.hud,e=core.elements[id],p=layout[id];
      widget.style.width=`${e.width}px`;widget.style.height=`${e.height}px`;
      widget.style.left=`${p.x}px`;widget.style.top=`${p.y}px`;
      widget.classList.toggle('is-selected',id===selected);
    });
    updateStatus();
  }
  function setSelection(id){selected=id;render();}
  function clearGuides(){scene.classList.remove('is-snapped-x','is-snapped-y');}
  function showGuides(id){
    const p=layout[id],e=core.elements[id];
    scene.classList.toggle('is-snapped-x',Math.abs(p.x+e.width/2-Math.trunc(core.WIDTH/2))<=.5);
    scene.classList.toggle('is-snapped-y',Math.abs(p.y+e.height/2-Math.trunc(core.HEIGHT/2))<=.5);
  }
  function pointerPosition(event){
    const rect=scene.getBoundingClientRect();
    return {x:Math.trunc((event.clientX-rect.left)/scale),y:Math.trunc((event.clientY-rect.top)/scale)};
  }
  function resize(){
    const width=viewport.clientWidth;if(!width)return;
    scale=width/core.WIDTH;scene.style.transform=`scale(${scale})`;
    scene.style.setProperty('--hud-hit-size',`${44/scale}px`);
    // Scene coordinates stay fixed, so layouts survive responsive resizing and expansion.
  }
  new ResizeObserver(resize).observe(viewport);
  function finishDrag(saveChanges=true){
    if(!drag)return;
    const {id,widget,pointerId}=drag;drag=null;
    widget.classList.remove('is-dragging');editor.classList.remove('is-dragging');
    if(widget.hasPointerCapture(pointerId))widget.releasePointerCapture(pointerId);
    clearGuides();if(saveChanges)save();
    status.textContent=`${core.elements[id].name} MOVED TO X ${layout[id].x}, Y ${layout[id].y}.`;
  }
  widgets.forEach(widget=>{
    const id=widget.dataset.hud;
    widget.addEventListener('pointerdown',event=>{
      if(event.button!==0||drag)return;
      event.preventDefault();widget.focus({preventScroll:true});setSelection(id);
      const point=pointerPosition(event);
      drag={id,widget,pointerId:event.pointerId,offsetX:point.x-layout[id].x,offsetY:point.y-layout[id].y};
      widget.setPointerCapture(event.pointerId);widget.classList.add('is-dragging');editor.classList.add('is-dragging');
    });
    widget.addEventListener('pointermove',event=>{
      if(!drag||drag.id!==id||drag.pointerId!==event.pointerId)return;
      const point=pointerPosition(event);
      layout[id]=core.position(id,point.x-drag.offsetX,point.y-drag.offsetY);
      render();showGuides(id);
    });
    widget.addEventListener('pointerup',()=>finishDrag());
    widget.addEventListener('pointercancel',()=>finishDrag());
    widget.addEventListener('lostpointercapture',()=>finishDrag());
    widget.addEventListener('focus',()=>setSelection(id));
    widget.addEventListener('keydown',event=>{
      const directions={ArrowLeft:[-1,0],ArrowRight:[1,0],ArrowUp:[0,-1],ArrowDown:[0,1]};
      if(!directions[event.key])return;
      event.preventDefault();const [dx,dy]=directions[event.key],step=event.shiftKey?10:1;
      // Browser keyboard extension: permit precise nudging away from a snapped center.
      layout[id]=core.position(id,layout[id].x+dx*step,layout[id].y+dy*step,false);
      render();showGuides(id);save();
      status.textContent=`${core.elements[id].name} AT X ${layout[id].x}, Y ${layout[id].y}.`;
    });
  });
  viewport.addEventListener('pointerdown',event=>{if(!event.target.closest('[data-hud]')){selected=null;clearGuides();render();}});
  document.querySelector('#hud-reset').addEventListener('click',()=>{
    finishDrag(false);layout=core.defaults();selected=null;clearGuides();render();save();status.textContent='HUD LAYOUT RESET.';
  });
  expand.addEventListener('click',()=>{
    finishDrag();dialog.append(editor);expand.hidden=true;done.hidden=false;
    dialog.showModal();document.body.style.overflow='hidden';resize();
    (widgets.find(w=>w.dataset.hud===selected)||widgets[0]).focus({preventScroll:true});
  });
  done.addEventListener('click',()=>dialog.close());
  dialog.addEventListener('close',()=>{
    finishDrag();home.before(editor);expand.hidden=false;done.hidden=true;
    document.body.style.overflow='';resize();expand.focus({preventScroll:true});save();
  });
  dialog.addEventListener('click',event=>{
    if(event.target!==dialog)return;const r=dialog.getBoundingClientRect();
    if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)dialog.close();
  });
  window.addEventListener('blur',()=>finishDrag());
  document.addEventListener('visibilitychange',()=>{if(document.hidden)finishDrag();});
  render();resize();
})();
