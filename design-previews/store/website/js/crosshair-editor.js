/* HTML adapter for the client's CrosshairSettingsScreen. Rendering lives in crosshair-core.js. */
(function () {
  'use strict';
  const core=window.AxialCrosshairCore;
  const root=document.querySelector('#crosshair-editor');
  if(!root||!core)return;
  const $=selector=>root.querySelector(selector);
  const storageKey='axial.crosshair.v1';
  let config=core.defaults(),canSave=true;
  try { const saved=localStorage.getItem(storageKey); if(saved)config=core.normalize(JSON.parse(saved)); }
  catch { canSave=false; }
  const history=new core.History();
  let zoom=1,background=0,pulseStart=performance.now(),activeTab=0;
  const inputs=[],toggleButtons=[],templateButtons=[],shapeButtons=[];
  const saveStatus=$('#ce-save-status');
  function save() {
    try {localStorage.setItem(storageKey,JSON.stringify(config));canSave=true;}
    catch {canSave=false;}
    saveStatus.textContent=canSave?'Saves in this browser automatically':'Preview only · browser storage unavailable';
    document.dispatchEvent(new CustomEvent('axial:crosshair-change',{detail:{enabled:config.enabled}}));
  }
  function edit(action) {history.checkpoint(config);action();save();sync();}
  function prettyStyle(style) {return ({LUNAR:'Cross',SMALL_DOT:'Pinpoint',SMALL_CROSS:'Small Cross'})[style]||style.charAt(0)+style.slice(1).toLowerCase();}
  function paintThumbnail(canvas,c) {
    const ctx=canvas.getContext('2d');ctx.clearRect(0,0,canvas.width,canvas.height);
    core.draw(ctx,canvas.width/2,canvas.height/2,{...c,enabled:true});
  }
  core.templates.forEach(template=>{
    const button=document.createElement('button');button.type='button';button.title=template.description;
    button.className='ce-template';button.setAttribute('aria-pressed','false');
    const canvas=document.createElement('canvas');canvas.width=100;canvas.height=40;canvas.setAttribute('aria-hidden','true');
    const label=document.createElement('span');label.textContent=template.name;
    button.append(canvas,label);$('#ce-gallery').append(button);
    button.addEventListener('click',()=>edit(()=>core.applyTemplate(config,template)));
    templateButtons.push({button,canvas,template});
  });
  core.styles.forEach(style=>{
    const button=document.createElement('button');button.type='button';button.title=prettyStyle(style);
    button.textContent=prettyStyle(style);button.setAttribute('aria-pressed','false');
    $('#ce-shapes').append(button);button.addEventListener('click',()=>edit(()=>{config.style=style;}));
    shapeButtons.push({button,style});
  });
  function dimension(parent,label,key,min,max,step=1,multiplier=1) {
    const row=document.createElement('div');row.className='ce-dimension';
    const rangeLabel=document.createElement('label');rangeLabel.textContent=label;
    const range=document.createElement('input');range.type='range';range.min=min;range.max=max;range.step=step;range.id=`ce-${key}-range`;
    rangeLabel.htmlFor=range.id;
    const number=document.createElement('input');number.type='number';number.min=min;number.max=max;number.step=step;number.setAttribute('aria-label',label+' value');
    row.append(rangeLabel,range,number);$(parent).append(row);
    const spec={range,number,key,multiplier,step};inputs.push(spec);
    for(const input of [range,number]) {
      let editing=false;
      input.addEventListener('input',()=>{
        const value=input.valueAsNumber;
        const valid=Number.isFinite(value)&&value>=min&&value<=max;
        input.setAttribute('aria-invalid',String(!valid));
        if(!valid)return;
        const snapped=Math.min(max,Math.max(min,min+Math.round((value-min)/step)*step));
        const next=Math.fround(snapped/multiplier);
        if(config[key]===next)return;
        if(!editing){history.checkpoint(config);editing=true;}
        config[key]=next;
        // Keep the field being typed intact; synchronize the other controls.
        sync(input);save();
      });
      input.addEventListener('change',()=>{editing=false;sync();});
      input.addEventListener('blur',()=>{editing=false;sync();});
    }
  }
  dimension('#ce-geometry','Scale','size',.1,4,.05);
  dimension('#ce-geometry','Arm Length / Radius','length',0,32);
  dimension('#ce-geometry','Thickness','width',1,12);
  dimension('#ce-geometry','Gap','gap',0,20);
  dimension('#ce-opacity','Opacity %','opacity',10,100,1,100);
  dimension('#ce-details','Outline Thickness','outlineThickness',1,4);
  dimension('#ce-details','Center Dot Size','dotSize',1,8);
  for(const [label,key] of [['Outline','outlineEnabled'],['Center Dot','centerDot'],['Dynamic Gap','dynamicEnabled']]) {
    const button=document.createElement('button');button.className='ce-toggle';button.type='button';
    button.setAttribute('role','switch');button.setAttribute('aria-label',label);
    const text=document.createElement('span');text.textContent=label;
    const indicator=document.createElement('i');indicator.setAttribute('aria-hidden','true');
    button.append(text,indicator);$('#ce-toggles').append(button);
    button.addEventListener('click',()=>edit(()=>{config[key]=!config[key];if(key==='dynamicEnabled')pulseStart=performance.now();}));
    toggleButtons.push({button,key});
  }
  const swatches=[];
  for(const color of ['#ffffff','#46e891','#55dfff','#c78df5','#ffd65a','#ff647b']) {
    const button=document.createElement('button');button.type='button';button.style.background=color;button.title=color;
    button.setAttribute('aria-label',`Set crosshair color ${color}`);
    button.addEventListener('click',()=>edit(()=>{config.color=color;}));$('#ce-swatches').append(button);swatches.push({button,color});
  }
  for(const [selector,key] of [['#ce-color','color'],['#ce-outline-color','outlineColor']]) {
    const input=$(selector);let editing=false;
    input.addEventListener('input',()=>{
      if(!editing){history.checkpoint(config);editing=true;}
      config[key]=input.value;save();sync();
    });
    input.addEventListener('change',()=>{editing=false;});
    input.addEventListener('blur',()=>{editing=false;});
  }
  const tabs=[$('#ce-templates-tab'),$('#ce-customize-tab')];
  function selectTab(index) {
    activeTab=index;
    tabs.forEach((tab,i)=>{tab.setAttribute('aria-selected',String(i===index));tab.tabIndex=i===index?0:-1;});
    $('#ce-templates').hidden=index!==0;$('#ce-customize').hidden=index!==1;
  }
  tabs.forEach((tab,index)=>{
    tab.addEventListener('click',()=>selectTab(index));
    tab.addEventListener('keydown',event=>{
      if(!['ArrowLeft','ArrowRight','Home','End'].includes(event.key))return;
      event.preventDefault();selectTab(event.key==='Home'?0:event.key==='End'?1:1-activeTab);tabs[activeTab].focus();
    });
  });
  $('#ce-enabled').addEventListener('click',()=>edit(()=>{config.enabled=!config.enabled;}));
  $('#ce-reset').addEventListener('click',()=>edit(()=>Object.assign(config,core.defaults())));
  $('#ce-undo').addEventListener('click',()=>{history.undo(config);save();sync();});
  $('#ce-zoom').addEventListener('click',()=>{zoom=zoom===1?4:1;sync();});
  $('#ce-pulse').addEventListener('click',()=>{pulseStart=performance.now();render();});
  root.querySelectorAll('[data-ce-background]').forEach(button=>button.addEventListener('click',()=>{
    background=Number(button.dataset.ceBackground);sync();
  }));
  const canvas=$('#ce-canvas'),ctx=canvas.getContext('2d');
  let lastFrameKey='',visible=true;
  function render(now=performance.now()) {
    const elapsed=Math.floor(now-pulseStart)%1500;
    const pulse=config.dynamicEnabled?core.pulseAt(elapsed):1;
    const frameKey=JSON.stringify([core.key(config,pulse),config.color,config.outlineColor,config.opacity,background,zoom,canvas.width,canvas.height]);
    if(lastFrameKey===frameKey)return;
    lastFrameKey=frameKey;
    const w=canvas.width,h=canvas.height;
    ctx.globalAlpha=1;ctx.fillStyle=background===1?'#e5e2d9':background===2?'#7facc2':'#14171d';ctx.fillRect(0,0,w,h);
    if(background===2) {
      const ground=Math.trunc(h/2);ctx.fillStyle='#59794d';ctx.fillRect(0,ground,w,h-ground);
      ctx.fillStyle='#7f7770';ctx.fillRect(Math.trunc(w/3),ground+Math.trunc(h/6),w,h);
    }
    ctx.save();ctx.translate(Math.trunc(w/2),Math.trunc(h/2));ctx.scale(zoom,zoom);
    // The client editor always previews the shape, even when it is disabled in-game.
    core.draw(ctx,0,0,{...config,enabled:true},pulse);ctx.restore();
    canvas.setAttribute('aria-label',`${prettyStyle(config.style)} crosshair, ${config.color}, ${zoom}× preview`);
  }
  function sync(activeInput) {
    templateButtons.forEach(({button,canvas,template})=>{
      button.setAttribute('aria-pressed',String(core.matches(config,template)));
      paintThumbnail(canvas,core.applyTemplate({...config},template));
    });
    shapeButtons.forEach(({button,style})=>button.setAttribute('aria-pressed',String(style===config.style)));
    inputs.forEach(({range,number,key,multiplier,step})=>{
      const value=(config[key]*multiplier).toFixed(step<1?2:0);
      if(range!==activeInput)range.value=value;
      if(number!==activeInput){number.value=value;number.setAttribute('aria-invalid','false');}
    });
    toggleButtons.forEach(({button,key})=>button.setAttribute('aria-checked',String(config[key])));
    swatches.forEach(({button,color})=>button.setAttribute('aria-pressed',String(config.color.toLowerCase()===color)));
    $('#ce-color').value=config.color;$('#ce-outline-color').value=config.outlineColor;
    $('#ce-enabled').textContent=config.enabled?'Enabled':'Disabled';$('#ce-enabled').setAttribute('aria-pressed',String(config.enabled));
    $('#ce-undo').disabled=!history.available;
    $('#ce-zoom').textContent=`${zoom}×`;
    $('#ce-preview-note').textContent=config.enabled?`Actual size · ${zoom}× view`:'Disabled in game';
    $('#ce-pulse').disabled=!config.dynamicEnabled;$('#ce-pulse').textContent=config.dynamicEnabled?'Test Pulse':'Dynamic: Off';
    root.querySelectorAll('[data-ce-background]').forEach(button=>button.setAttribute('aria-pressed',String(Number(button.dataset.ceBackground)===background)));
    render();
  }
  const resize=new ResizeObserver(()=>{
    const rect=canvas.getBoundingClientRect();
    canvas.width=Math.max(1,Math.round(rect.width));canvas.height=Math.max(1,Math.round(rect.height));
    render();
  });resize.observe(canvas);
  // Don't keep animation frames running while the section is offscreen or the tab is hidden.
  let animationFrame;
  function animate(now){animationFrame=undefined;if(!visible||document.hidden)return;if(config.dynamicEnabled)render(now);animationFrame=requestAnimationFrame(animate);}
  function startAnimation(){if(!animationFrame&&visible&&!document.hidden)animationFrame=requestAnimationFrame(animate);}
  new IntersectionObserver(entries=>{visible=entries[0].isIntersecting;if(visible)startAnimation();else{cancelAnimationFrame(animationFrame);animationFrame=undefined;}}).observe(root);
  document.addEventListener('visibilitychange',()=>{if(document.hidden){cancelAnimationFrame(animationFrame);animationFrame=undefined;}else startAnimation();});
  document.addEventListener('axial:crosshair-enable',event=>{
    const enabled=event.detail?.enabled;
    if(typeof enabled==='boolean'&&config.enabled!==enabled){history.checkpoint(config);config.enabled=enabled;save();sync();}
  });
  sync();
  if(!canSave)saveStatus.textContent='Preview only · browser storage unavailable';
})();
