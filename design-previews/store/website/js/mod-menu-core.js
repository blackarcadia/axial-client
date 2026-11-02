/* Browser adaptation of AxialClientAlpha ModMenuCatalog, ModMenuEntry,
 * ModFeatureSettings, and ToggleAnimation. Game effects require Minecraft.
 */
(function(root){
  'use strict';
  const categories=[['ALL','All mods','grid'],['HUD','HUD','monitor'],['MINING','Mining','pickaxe'],['VISUALS','Visuals','eye'],['WORLD','World','globe']];
  // Order follows the backend's real entries, then the injected menu entries.
  // Fissure/Rupture Highlights is removed by AxialConfigScreenFissureRemovalMixin.
  const entries=[
    ['hud','HUD','On-screen information','HUD','monitor',true,'hud'],
    ['satchel','Satchel Helper','Ore satchel information','HUD','backpack',true,'satchel'],
    ['full-bright','Full Bright','Brighten dark areas','VISUALS','sun',false,null],
    ['smart-mining','Smart Mining','Hide players when mining','MINING','pickaxe',false,null],
    ['cps','CPS','Clicks per second','HUD','mouse',true,'cps'],
    ['armor','Armor HUD','Armor and durability','HUD','shirt',true,'armor'],
    ['contraband','Contraband','Contraband highlights','MINING','cube',false,null],
    ['scoreboard','Scoreboard','Scoreboard display','HUD','chart',false,null],
    ['world-border','World Border','World boundary display','WORLD','globe',true,null],
    ['title','Title Overlay','Overlay appearance','VISUALS','monitor',true,'title'],
    ['item-scaler','Item Scaler','Held item scale','VISUALS','scale',null,'items'],
    ['crosshair','Crosshair','Aim style and appearance','VISUALS','crosshair',true,'crosshair'],
    ['chunk-borders','Chunk Borders','Show chunk boundaries','WORLD','globe',false,'chunks'],
    ['compass','Compass','Direction at a glance','HUD','compass',true,'compass'],
    ['minimap','Minimap','Your surroundings','WORLD','map',false,'minimap'],
    ['waypoints','Waypoints','Saved world locations','WORLD','map',null,'waypoints'],
    ['potions','Potions HUD','Active potion effects','HUD','potion',false,'potions'],
    ['keystrokes','Keystrokes','Keyboard and mouse inputs','HUD','mouse',false,'keystrokes'],
    ['cooldowns','Cooldowns','Pet and combat timers','HUD','compass',true,'cooldowns'],
    ['gangs','Gangs','Server gangs and rankings','WORLD','account',null,'gangs'],
    ['enchantments','Enchantments','Server enchantment guide','MINING','book',null,'enchantments'],
  ].map(([id,title,description,category,icon,enabled,settings])=>({id,title,description,category,icon,enabled,settings}));
  const toggle=(key,label,value)=>({key,label,type:'toggle',value});
  const color=(key,label,value)=>({key,label,type:'color',value});
  const range=(key,label,value,min,max,step=1)=>({key,label,type:'range',value,min,max,step});
  const choice=(key,label,value,options)=>({key,label,type:'choice',value,options});
  const titleColor=()=>color('titleColor','Title color','#f900df');
  const textColor=()=>color('textColor','Text color','#ffc917');
  // Labels and ranges come from ModFeatureSettings / ClientSettingsScreen.
  const schemas={
    hud:[toggle('ping','Ping',false),toggle('coordinates','Coordinates',false),toggle('charge','Charge per min',true),toggle('xp','XP per min',true),toggle('server','Server',false),toggle('facing','Facing',false),titleColor(),color('pingColor','Ping color','#ffc917'),color('coordinatesColor','Coordinates color','#ffc917'),color('chargeColor','Charge / min color','#ffc917'),color('xpColor','XP / min color','#ffc917'),color('serverColor','Server color','#ffc917'),color('facingColor','Facing color','#ffc917')],
    satchel:[titleColor(),color('countColor','Satchel count color','#ffc917'),color('emptyColor','Satchel empty color','#ffc917'),{key:'title',label:'Display title',type:'text',value:'SATCHEL HUD'}],
    cps:[titleColor(),textColor()],
    armor:[toggle('helmet','Helmet',true),toggle('chest','Chest',true),toggle('legs','Legs',true),toggle('boots','Boots',true),toggle('held','Held item',true),toggle('durability','Durability',true),titleColor(),textColor(),choice('format','Format','NUMBER',['NUMBER','PERCENT'])],
    title:[toggle('centered','Centered',true),range('scale','Scale',1,.5,3,.01)],
    items:[range('mainHand','Main hand',1,.25,3,.01),range('offHand','Off hand',1,.25,3,.01)],
    chunks:[color('color','Border color','#ff4040')],
    compass:[range('scale','Scale',1,.5,3,.01)],
    potions:[titleColor()],
    cooldowns:[toggle('pets','Pet cooldowns',true),toggle('combat','Combat tag',true)],
    keystrokes:[choice('layout','Layout','W / AS / D',['W / ASD','W / AS / D']),range('scale','Scale',1,.5,3,.05),range('opacity','Background opacity',75,20,100),color('pressedColor','Pressed background','#8860b7'),...['W','A','S','D','LMB','RMB'].map(key=>toggle(key,key,true)),...['W','A','S','D','LMB','RMB'].map(key=>color(key+'Color',key+' text color','#f0f2f3'))],
    minimap:[choice('shape','Shape','SQUARE',['SQUARE','CIRCLE']),choice('size','Size','1.0×',['1.0×','1.5×','2.0×','3.0×']),choice('zoom','Map scale','0.75×',['0.25×','0.5×','0.75×','1×','1.5×','2×']),toggle('coordinates','Coordinates',true),toggle('names','Waypoint names',true),toggle('death','Death marker',true)],
    client:[toggle('shadows','Player shadows',true),toggle('nameTags','Third person name tags',false),toggle('displayFps','Display FPS',false),color('fpsColor','FPS color','#ffffff'),toggle('droppedItems','Render dropped items',true),toggle('liquids','Render liquids',true),toggle('spawnerMobs','Disable spawner mobs',false),toggle('particles','Render particles',true),toggle('weather','Render weather',true),range('glint','Enchant glint strength',100,100,400),color('glintColor','Enchant glint color','#a060ff')],
  };
  function defaults(){return {enabled:Object.fromEntries(entries.filter(e=>e.enabled!==null).map(e=>[e.id,e.enabled])),settings:Object.fromEntries(Object.entries(schemas).map(([key,fields])=>[key,Object.fromEntries(fields.map(field=>[field.key,field.value]))]))};}
  function matches(entry,category,query,enabledOnly,state){
    return (category==='ALL'||entry.category===category)&&(!enabledOnly||state.enabled[entry.id]===true)&&
      (entry.title+' '+entry.description).toLowerCase().includes(query.trim().toLowerCase());
  }
  function restore(input){
    const result=defaults();
    for(const id of Object.keys(result.enabled))if(typeof input?.enabled?.[id]==='boolean')result.enabled[id]=input.enabled[id];
    for(const [schema,fields] of Object.entries(schemas))for(const field of fields){
      const value=input?.settings?.[schema]?.[field.key];
      if(field.type==='toggle'&&typeof value==='boolean')result.settings[schema][field.key]=value;
      if(field.type==='color'&&/^#[0-9a-f]{6}$/i.test(value))result.settings[schema][field.key]=value;
      if(field.type==='range'&&Number.isFinite(value))result.settings[schema][field.key]=Math.min(field.max,Math.max(field.min,Math.round((value-field.min)/field.step)*field.step+field.min));
      if(field.type==='choice'&&field.options.includes(value))result.settings[schema][field.key]=value;
      if(field.type==='text'&&typeof value==='string')result.settings[schema][field.key]=value.slice(0,64);
    }
    return result;
  }
  // Exact critically damped spring integration from ToggleAnimation.java, speed 24.
  class ToggleAnimation{
    constructor(enabled,now=0){this.position=this.target=enabled?1:0;this.velocity=0;this.last=now;}
    value(enabled,now){
      const seconds=Math.max(0,(now-this.last)/1000);this.last=now;
      const error=this.position-this.target,coefficient=this.velocity+24*error,decay=Math.exp(-24*seconds);
      this.position=this.target+(error+coefficient*seconds)*decay;this.velocity=(this.velocity-24*coefficient*seconds)*decay;
      this.target=enabled?1:0;return Math.fround(Math.min(1,Math.max(0,this.position)));
    }
    get settled(){return Math.abs(this.position-this.target)<.001&&Math.abs(this.velocity)<.001;}
  }
  const api={entries,categories,schemas,defaults,matches,restore,ToggleAnimation};
  if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.AxialModMenuCore=api;
})(typeof window!=='undefined'?window:globalThis);
