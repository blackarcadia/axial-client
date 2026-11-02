/* DOM adapter for the native mod menu. No Minecraft or server state is changed. */
(function(){
  'use strict';
  const core=window.AxialModMenuCore,root=document.querySelector('#mod-menu');
  if(!core||!root)return;
  const $=selector=>root.querySelector(selector);
  const grid=$('#mm-grid'),search=$('#mm-search'),filter=$('#mm-enabled-filter');
  const controls=$('#mm-setting-controls'),sample=$('#mm-sample');
  const storageKey='axial.mod-menu.v1',motion=matchMedia('(prefers-reduced-motion: reduce)');
  let state=core.defaults(),category='ALL',query='',enabledOnly=false,view=null,canSave=true,animationFrame;
  try{const saved=localStorage.getItem(storageKey);if(saved)state=core.restore(JSON.parse(saved));}catch{canSave=false;}
  const toggleMotions=new Map(),toggleNodes=new Map();
  const make=(tag,className,text)=>{const node=document.createElement(tag);if(className)node.className=className;if(text!==undefined)node.textContent=text;return node;};
  function icon(name){
    if(name==='shield'){
      const svg=document.createElementNS('http://www.w3.org/2000/svg','svg');svg.setAttribute('viewBox','0 0 24 24');svg.setAttribute('aria-hidden','true');
      const path=document.createElementNS(svg.namespaceURI,'path');path.setAttribute('d','M4 3h16v10c0 5-8 9-8 9s-8-4-8-9Z');path.setAttribute('fill','#6e8293');path.setAttribute('stroke','#d6dce1');path.setAttribute('stroke-width','1.5');svg.append(path);return svg;
    }
    if(name==='book'){
      const svg=document.createElementNS('http://www.w3.org/2000/svg','svg');svg.setAttribute('viewBox','0 0 24 24');svg.setAttribute('aria-hidden','true');
      const path=document.createElementNS(svg.namespaceURI,'path');path.setAttribute('d','M12 5C9 2 5 3 3 4v15c3-1 6-1 9 2 3-3 6-3 9-2V4c-2-1-6-2-9 1Zm0 0v16');svg.append(path);return svg;
    }
    const image=make('img');image.src=`img/mod-menu/${name}.png`;image.alt='';image.draggable=false;return image;
  }
  function notify(text){$('#mm-status').textContent=text;}
  function save(){
    try{localStorage.setItem(storageKey,JSON.stringify(state));canSave=true;}catch{canSave=false;}
    $('#mm-local-note').textContent=canSave?'BROWSER PREVIEW · SAVED LOCALLY':'BROWSER PREVIEW · STORAGE UNAVAILABLE';
  }
  function syncCrosshair(){
    try{const saved=JSON.parse(localStorage.getItem('axial.crosshair.v1'));state.enabled.crosshair=typeof saved?.enabled==='boolean'?saved.enabled:true;}catch{state.enabled.crosshair=true;}
  }
  syncCrosshair();
  function paintToggle(node,value){
    node.style.setProperty('--toggle-progress',value);
    const rgb=[240,100,108].map((off,i)=>Math.round(off+([70,213,125][i]-off)*value));
    node.style.setProperty('--toggle-color',`rgb(${rgb.join(',')})`);
  }
  function animateToggles(now){
    animationFrame=undefined;let moving=false;
    toggleNodes.forEach((node,id)=>{
      const anim=toggleMotions.get(id),on=state.enabled[id];
      if(motion.matches){paintToggle(node,on?1:0);return;}
      paintToggle(node,anim.value(on,now));if(!anim.settled)moving=true;
    });
    if(moving&&!document.hidden)animationFrame=requestAnimationFrame(animateToggles);
  }
  function startToggles(){if(!animationFrame)animationFrame=requestAnimationFrame(animateToggles);}
  function setEnabled(id,value){
    const old=state.enabled[id];state.enabled[id]=value;
    if(id==='crosshair')document.dispatchEvent(new CustomEvent('axial:crosshair-enable',{detail:{enabled:value}}));
    save();notify(`${core.entries.find(e=>e.id===id).title}: ${value?'ON':'OFF'}`);
    const node=toggleNodes.get(id);
    if(node){node.setAttribute('aria-checked',String(value));node.querySelector('.mm-onoff').textContent=value?'ON':'OFF';}
    if(!toggleMotions.has(id))toggleMotions.set(id,new core.ToggleAnimation(old,performance.now()));
    toggleMotions.get(id).value(value,performance.now());startToggles();
    if(enabledOnly&&!view){renderLibrary();search.focus({preventScroll:true});}
  }
  core.categories.forEach(([id,label,name])=>{
    const button=make('button','mm-category');button.type='button';button.dataset.category=id;button.setAttribute('aria-pressed',String(id===category));
    button.append(icon(name),make('span','',label));button.addEventListener('click',()=>{category=id;renderLibrary();grid.scrollTop=0;});$('#mm-categories').append(button);
  });
  function renderLibrary(){
    const matches=core.entries.filter(entry=>core.matches(entry,category,query,enabledOnly,state));
    toggleNodes.clear();grid.replaceChildren();
    root.querySelectorAll('[data-category]').forEach(button=>button.setAttribute('aria-pressed',String(button.dataset.category===category)));
    matches.forEach(entry=>{
      const card=make('article','mm-card');card.dataset.mod=entry.id;
      const info=make('div','mm-card-info'),text=make('div');
      text.append(make('h4','',entry.title),make('p','',entry.description));info.append(icon(entry.icon),text);card.append(info);
      if(entry.enabled!==null){
        const button=make('button','mm-card-toggle');button.type='button';button.setAttribute('role','switch');button.setAttribute('aria-label',`Toggle ${entry.title}`);button.setAttribute('aria-checked',String(state.enabled[entry.id]));
        const track=make('span','mm-switch');track.setAttribute('aria-hidden','true');track.append(make('i'));
        button.append(track,make('span','mm-onoff',state.enabled[entry.id]?'ON':'OFF'));
        if(!toggleMotions.has(entry.id))toggleMotions.set(entry.id,new core.ToggleAnimation(state.enabled[entry.id],performance.now()));
        paintToggle(button,toggleMotions.get(entry.id).value(state.enabled[entry.id],performance.now()));toggleNodes.set(entry.id,button);
        button.addEventListener('click',()=>setEnabled(entry.id,!state.enabled[entry.id]));card.append(button);
      }else{
        const button=make('button','mm-card-open');button.type='button';button.setAttribute('aria-label',`${['gangs','enchantments'].includes(entry.id)?'Open':'Configure'} ${entry.title}`);
        button.append(make('span','', ['gangs','enchantments'].includes(entry.id)?'BROWSE':'CONFIGURE'));button.addEventListener('click',()=>openEntry(entry));card.append(button);
      }
      if(entry.settings&&entry.enabled!==null){
        const gear=make('button','mm-gear');gear.type='button';gear.setAttribute('aria-label',`Configure ${entry.title}`);gear.title=`Configure ${entry.title}`;gear.append(icon('gear'));
        gear.addEventListener('click',()=>openEntry(entry));card.append(gear);
      }
      grid.append(card);
    });
    if(!matches.length){const empty=make('div','mm-empty');empty.append(make('p','','NO MATCHING MODS'));const clear=make('button','','CLEAR FILTERS');clear.type='button';clear.addEventListener('click',()=>{category='ALL';query='';enabledOnly=false;search.value='';filter.checked=false;renderLibrary();search.focus();});empty.append(clear);grid.append(empty);}
    $('#mm-count').textContent=`${matches.length} OF ${core.entries.length} MODS`;
    startToggles();
  }
  search.addEventListener('input',()=>{query=search.value;renderLibrary();grid.scrollTop=0;});
  filter.addEventListener('change',()=>{enabledOnly=filter.checked;renderLibrary();grid.scrollTop=0;});
  function enterLibrary(){
    view=null;$('#mm-library').hidden=false;$('#mm-settings').hidden=true;$('#mm-back').hidden=true;$('#mm-reset').hidden=true;
    $('#mm-heading-title').textContent='MOD MENU';$('#mm-nav-mods').setAttribute('aria-pressed','true');$('#mm-nav-settings').setAttribute('aria-pressed','false');
    syncCrosshair();renderLibrary();
  }
  $('#mm-nav-mods').addEventListener('click',enterLibrary);$('#mm-back').addEventListener('click',enterLibrary);
  $('#mm-nav-settings').addEventListener('click',()=>openSettings('client','SETTINGS'));
  function openHud(){document.querySelector('[data-panel="move"]').click();}
  $('#mm-nav-hud').addEventListener('click',openHud);
  function openEntry(entry){
    if(entry.settings==='crosshair'){document.querySelector('[data-panel="crosshair"]').click();return;}
    if(entry.settings==='waypoints'){document.querySelector('[data-detail="map"]').click();return;}
    openSettings(entry.settings,entry.title,entry);
  }
  let settingNodes=[];
  function openSettings(schema,title,entry){
    view={schema,entry};$('#mm-library').hidden=true;$('#mm-settings').hidden=false;$('#mm-back').hidden=false;$('#mm-reset').hidden=!core.schemas[schema];
    $('#mm-heading-title').textContent=title;$('#mm-nav-mods').setAttribute('aria-pressed','false');$('#mm-nav-settings').setAttribute('aria-pressed',String(schema==='client'));
    controls.replaceChildren();settingNodes=[];sample.parentElement.hidden=false;$('#mm-settings').classList.remove('mm-settings-message');
    if(!core.schemas[schema]){
      sample.parentElement.hidden=true;$('#mm-settings').classList.add('mm-settings-message');
      const message=make('div','mm-message');message.append(icon(entry.icon),make('h4','',title),make('p','',schema==='gangs'?'GANGS AND RANKINGS REQUIRE A SERVER CONNECTION IN AXIALCLIENT.':'THE ENCHANTMENT GUIDE REQUIRES A SERVER CONNECTION IN AXIALCLIENT.'));
      controls.append(message);$('#mm-count').textContent='IN-GAME FEATURE';return;
    }
    if(entry?.enabled!==null&&entry?.enabled!==undefined){
      const button=make('button','mm-setting-toggle');button.type='button';button.setAttribute('role','switch');button.setAttribute('aria-label',`Enable ${entry.title}`);button.setAttribute('aria-checked',String(state.enabled[entry.id]));button.append(make('span','','ENABLED'),make('span','mm-setting-state',state.enabled[entry.id]?'ON':'OFF'));
      button.addEventListener('click',()=>{setEnabled(entry.id,!state.enabled[entry.id]);button.setAttribute('aria-checked',String(state.enabled[entry.id]));button.lastElementChild.textContent=state.enabled[entry.id]?'ON':'OFF';});controls.append(button);
    }
    core.schemas[schema].forEach(field=>{
      const value=state.settings[schema][field.key];
      if(field.type==='toggle'){
        const button=make('button','mm-setting-toggle');button.type='button';button.setAttribute('role','switch');button.setAttribute('aria-label',field.label);button.setAttribute('aria-checked',String(value));
        button.append(make('span','',field.label),make('span','mm-setting-state',value?'ON':'OFF'));
        button.addEventListener('click',()=>{const next=!state.settings[schema][field.key];state.settings[schema][field.key]=next;button.setAttribute('aria-checked',String(next));button.lastElementChild.textContent=next?'ON':'OFF';save();renderSample();});controls.append(button);return;
      }
      const row=make('label','mm-setting-field'),label=make('span','',field.label);row.append(label);
      let input;
      if(field.type==='choice'){
        input=make('select');field.options.forEach(value=>{const option=make('option','',value);option.value=value;input.append(option);});input.value=value;
      }else{
        input=make('input');input.type=field.type==='range'?'range':field.type==='color'?'color':'text';input.value=value;
        if(field.type==='range'){input.min=field.min;input.max=field.max;input.step=field.step;row.classList.add('mm-range');}
        if(field.type==='text')input.maxLength=64;
      }
      input.setAttribute('aria-label',field.label);
      const output=field.type==='range'?make('output','',Number(value).toFixed(field.step<1?2:0)):null;
      if(output)row.append(output);row.append(input);controls.append(row);settingNodes.push({field,input});
      input.addEventListener('input',()=>{
        state.settings[schema][field.key]=field.type==='range'?Number(input.value):input.value;
        if(output)output.textContent=Number(input.value).toFixed(field.step<1?2:0);save();renderSample();
      });
      input.addEventListener('change',()=>{state.settings[schema][field.key]=field.type==='range'?Number(input.value):input.value;save();renderSample();});
    });
    if(['hud','armor','cps'].includes(schema)){const button=make('button','mm-extra-action','EDIT HUD ↗');button.type='button';button.addEventListener('click',openHud);controls.append(button);}
    if(schema==='minimap'){const button=make('button','mm-extra-action','MAP & WAYPOINTS ↗');button.type='button';button.addEventListener('click',()=>document.querySelector('[data-detail="map"]').click());controls.append(button);}
    $('#mm-count').textContent='SETTINGS';renderSample();
    controls.scrollTop=0;
    if(!motion.matches)$('#mm-settings').animate([{opacity:.25,transform:'translateY(7px)'},{opacity:1,transform:'translateY(0)'}],{duration:180,easing:'cubic-bezier(.2,.7,.2,1)'});
  }
  $('#mm-reset').addEventListener('click',()=>{
    if(!view)return;const {schema,entry}=view;
    state.settings[schema]={...core.defaults().settings[schema]};if(entry&&entry.enabled!==null)state.enabled[entry.id]=entry.enabled;
    save();openSettings(schema,entry?.title||'SETTINGS',entry);notify('DEFAULTS RESTORED');
  });
  function line(text,color){const node=make('div','mm-sample-line',text);if(color)node.style.color=color;sample.append(node);return node;}
  function renderSample(){
    if(!view||!core.schemas[view.schema])return;
    const {schema}=view,s=state.settings[schema];sample.replaceChildren();sample.className='mm-sample';sample.style.cssText='';
    switch(schema){
      case 'hud':
        line('INFORMATION HUD',s.titleColor);
        [['ping','PING: 24 MS'],['coordinates','XYZ: 120 64 −32'],['charge','CHARGE / MIN: 420'],['xp','XP / MIN: 1,250'],['server','SERVER: EXAMPLE'],['facing','FACING: NORTH']].forEach(([key,text])=>{if(s[key])line(text,s[key+'Color']);});break;
      case 'satchel':line(s.title,s.titleColor);line('SATCHELS: 3',s.countColor);line('EMPTY: 1',s.emptyColor);break;
      case 'cps':line('CPS HUD',s.titleColor);line('8 CPS',s.textColor);break;
      case 'armor':
        ['helmet','chest','legs','boots','held'].forEach((key,i)=>{if(s[key]){line(['HELMET','CHESTPLATE','LEGGINGS','BOOTS','HELD ITEM'][i],s.titleColor);if(s.durability)line(s.format==='PERCENT'?`${100-i*3}%`:`${400-i*20} / 400`,s.textColor);}});break;
      case 'title':{const title=line('AXIAL CLIENT');sample.style.textAlign=s.centered?'center':'left';title.style.transform=`scale(${s.scale})`;break;}
      case 'compass':{const compass=make('div','mm-compass');compass.style.transform=`scale(${s.scale})`;compass.append(make('div','','NW     N     NE'),make('div','','|   |   |   |   |'),make('strong','','N'));sample.append(compass);break;}
      case 'potions':line('POTIONS HUD',s.titleColor);line('SPEED II   1:42');line('HASTE II   0:58');break;
      case 'chunks':{const grid=make('div','mm-chunk-grid');grid.style.setProperty('--chunk-color',s.color);sample.append(grid);break;}
      case 'items':{const items=make('div','mm-item-scales');for(const [key,label] of [['mainHand','MAIN HAND'],['offHand','OFF HAND']]){const item=make('div'),art=icon(key==='mainHand'?'pickaxe':'shield');art.style.transform=`scale(${s[key]})`;item.append(art,make('small','',label));items.append(item);}sample.append(items);break;}
      case 'cooldowns':if(s.combat)line('COMBAT: 0:12');if(s.pets)line('PET COOLDOWN: 0:30');if(!s.combat&&!s.pets)line('TIMERS HIDDEN');break;
      case 'keystrokes':{
        const keys=make('div','mm-keys');keys.dataset.layout=s.layout;keys.style.transform=`scale(${s.scale})`;keys.style.setProperty('--key-opacity',s.opacity/100);keys.style.setProperty('--pressed-color',s.pressedColor);
        ['W','A','S','D','LMB','RMB'].forEach(key=>{if(s[key]){const button=make('button','mm-key',key);button.type='button';button.dataset.key=key;button.style.color=s[key+'Color'];button.addEventListener('pointerdown',()=>button.classList.add('pressed'));['pointerup','pointercancel','pointerleave','blur'].forEach(event=>button.addEventListener(event,()=>button.classList.remove('pressed')));keys.append(button);}});
        sample.append(keys);if(!keys.childElementCount)line('NO KEYS SELECTED');break;
      }
      case 'minimap':{
        const map=make('div','mm-minimap');map.style.borderRadius=s.shape==='CIRCLE'?'50%':'4px';const sizes=['1.0×','1.5×','2.0×','3.0×'];map.style.width=`${110+sizes.indexOf(s.size)*20}px`;
        const image=make('img');image.src='img/client/map.png';image.alt='Map preview';image.style.transform=`scale(${parseFloat(s.zoom)/.75})`;map.append(image);sample.append(map);if(s.coordinates)line('XYZ: 120 64 −32');if(s.names)line('SPAWN');if(s.death)line('DEATH MARKER');break;
      }
      case 'client':
        line('CLIENT PREFERENCES');line(s.displayFps?'FPS DISPLAY: ON':'FPS DISPLAY: OFF',s.fpsColor);line(s.particles?'PARTICLES: ON':'PARTICLES: OFF');line(s.weather?'WEATHER: ON':'WEATHER: OFF');line(`GLINT: ${s.glint}%`,s.glintColor);break;
    }
  }
  document.addEventListener('keydown',event=>{
    if(view?.schema!=='keystrokes'||root.closest('[hidden]')||/INPUT|SELECT|TEXTAREA/.test(event.target.tagName))return;
    const key=event.code.replace('Key',''),button=sample.querySelector(`[data-key="${key}"]`);if(button)button.classList.add('pressed');
  });
  document.addEventListener('keyup',event=>{const key=event.code.replace('Key','');sample.querySelector(`[data-key="${key}"]`)?.classList.remove('pressed');});
  window.addEventListener('blur',()=>sample.querySelectorAll('.pressed').forEach(key=>key.classList.remove('pressed')));
  document.addEventListener('axial:crosshair-change',event=>{state.enabled.crosshair=event.detail.enabled;save();if(!view)renderLibrary();});
  // Synchronize the menu after editing the crosshair in its dedicated tab.
  document.querySelector('[data-panel="features"]').addEventListener('click',()=>{syncCrosshair();if(!view)renderLibrary();});
  document.addEventListener('visibilitychange',()=>{if(!document.hidden)startToggles();});
  grid.addEventListener('keydown',event=>{
    if(event.target!==grid)return;
    if(event.key==='Home'){event.preventDefault();grid.scrollTop=0;}
    if(event.key==='End'){event.preventDefault();grid.scrollTop=grid.scrollHeight;}
  });
  renderLibrary();
  if(!canSave)$('#mm-local-note').textContent='BROWSER PREVIEW · STORAGE UNAVAILABLE';
})();
