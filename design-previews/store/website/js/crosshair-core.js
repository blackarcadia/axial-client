/* Browser port of AxialClientAlpha's CrosshairConfig, CrosshairGeometry,
 * CrosshairPresets, CrosshairHistory and CrosshairDynamicState.
 * Source: com/axial/cosmetics/client, inspected 2026-10-25.
 * Keep integer pixel placement and Java float rounding: no CSS shape approximations.
 */
(function (root) {
  'use strict';
  const clamp = (value, min, max) => Math.min(max, Math.max(min, value));
  const f = Math.fround;
  const round = value => Number.isNaN(value) ? 0 : clamp(Math.floor(value + 0.5), -2147483648, 2147483647);
  const styles = ['CLASSIC','LUNAR','DOT','SMALL_DOT','PLUS','T','X','CIRCLE','SMALL_CROSS','DIAMOND','SQUARE','CHEVRON','BRACKETS'];
  function defaults() {
    return { enabled:true, style:'LUNAR', color:'#ffffff', outlineColor:'#000000', size:1, length:4, width:2, gap:2,
      outlineEnabled:true, dynamicEnabled:false, centerDot:false, dotSize:2, outlineThickness:1, opacity:1 };
  }
  const templates = [
  {
    "name": "Classic",
    "description": "Balanced four-arm crosshair",
    "style": "LUNAR",
    "length": 4,
    "width": 2,
    "gap": 2,
    "outline": true,
    "dot": false
  },
  {
    "name": "Precision",
    "description": "Thin arms and a clear aiming gap",
    "style": "LUNAR",
    "length": 4,
    "width": 1,
    "gap": 2,
    "outline": true,
    "dot": false
  },
  {
    "name": "Compact",
    "description": "Small, unobtrusive cross",
    "style": "LUNAR",
    "length": 2,
    "width": 1,
    "gap": 1,
    "outline": true,
    "dot": false
  },
  {
    "name": "Plus",
    "description": "Connected one-pixel cross",
    "style": "PLUS",
    "length": 4,
    "width": 1,
    "gap": 0,
    "outline": true,
    "dot": false
  },
  {
    "name": "Dot",
    "description": "Simple two-pixel aiming point",
    "style": "DOT",
    "length": 0,
    "width": 2,
    "gap": 0,
    "outline": false,
    "dot": false
  },
  {
    "name": "Pinpoint",
    "description": "A single-pixel aiming point",
    "style": "SMALL_DOT",
    "length": 0,
    "width": 1,
    "gap": 0,
    "outline": false,
    "dot": false
  },
  {
    "name": "Bold Dot",
    "description": "Larger, easy-to-see aiming point",
    "style": "DOT",
    "length": 0,
    "width": 4,
    "gap": 0,
    "outline": true,
    "dot": false
  },
  {
    "name": "Tactical T",
    "description": "Open top for a clearer target view",
    "style": "T",
    "length": 4,
    "width": 1,
    "gap": 2,
    "outline": true,
    "dot": false
  },
  {
    "name": "Diagonal",
    "description": "Four separated diagonal arms",
    "style": "X",
    "length": 3,
    "width": 1,
    "gap": 2,
    "outline": true,
    "dot": false
  },
  {
    "name": "Ring",
    "description": "Round, hollow aiming ring",
    "style": "CIRCLE",
    "length": 5,
    "width": 1,
    "gap": 0,
    "outline": true,
    "dot": false
  },
  {
    "name": "Ring + Dot",
    "description": "Circular target with a center point",
    "style": "CIRCLE",
    "length": 6,
    "width": 1,
    "gap": 0,
    "outline": true,
    "dot": true
  },
  {
    "name": "Diamond",
    "description": "Angular hollow diamond",
    "style": "DIAMOND",
    "length": 6,
    "width": 1,
    "gap": 0,
    "outline": true,
    "dot": false
  },
  {
    "name": "Diamond Dot",
    "description": "Diamond with a precise center point",
    "style": "DIAMOND",
    "length": 7,
    "width": 1,
    "gap": 0,
    "outline": true,
    "dot": true
  },
  {
    "name": "Square",
    "description": "Thin, hollow square reticle",
    "style": "SQUARE",
    "length": 5,
    "width": 1,
    "gap": 0,
    "outline": true,
    "dot": false
  },
  {
    "name": "Chevron",
    "description": "Upward-pointing aiming chevron",
    "style": "CHEVRON",
    "length": 4,
    "width": 1,
    "gap": 0,
    "outline": true,
    "dot": false
  },
  {
    "name": "Brackets",
    "description": "Open center framed by two brackets",
    "style": "BRACKETS",
    "length": 4,
    "width": 1,
    "gap": 3,
    "outline": true,
    "dot": false
  },
  {
    "name": "Cross + Dot",
    "description": "Separated arms around a center point",
    "style": "LUNAR",
    "length": 4,
    "width": 1,
    "gap": 3,
    "outline": true,
    "dot": true
  },
  {
    "name": "Wide Cross",
    "description": "Longer arms and a wider center gap",
    "style": "LUNAR",
    "length": 7,
    "width": 2,
    "gap": 4,
    "outline": true,
    "dot": false
  },
  {
    "name": "Tiny Cross",
    "description": "Compact cross with short arms",
    "style": "SMALL_CROSS",
    "length": 2,
    "width": 1,
    "gap": 0,
    "outline": true,
    "dot": false
  },
  {
    "name": "Bold Plus",
    "description": "Thicker, connected cross",
    "style": "PLUS",
    "length": 5,
    "width": 3,
    "gap": 0,
    "outline": true,
    "dot": false
  }
];
  function applyTemplate(config, template) {
    Object.assign(config, {style:template.style,size:1,length:template.length,width:template.width,gap:template.gap,
      outlineEnabled:template.outline,outlineThickness:1,centerDot:template.dot,dotSize:2});
    return config;
  }
  function matches(c,p) {
    return c.style===p.style && c.size===1 && c.length===p.length && c.width===p.width && c.gap===p.gap &&
      c.outlineEnabled===p.outline && c.outlineThickness===1 && c.centerDot===p.dot && (!p.dot || c.dotSize===2);
  }
  function normalize(input) {
    const c={...defaults(),...input};
    const bounded=(v,min,max,fallback)=>Number.isFinite(v)?clamp(v,min,max):fallback;
    c.style=styles.includes(c.style)?c.style:'LUNAR';
    c.size=f(bounded(c.size,.1,4,1)); c.length=round(bounded(c.length,0,32,4));
    c.width=round(bounded(c.width,1,12,2)); c.gap=round(bounded(c.gap,0,20,2));
    c.opacity=f(bounded(c.opacity,.1,1,1));
    c.dotSize=round(bounded(c.dotSize,1,8,2)); c.outlineThickness=round(bounded(c.outlineThickness,1,4,1));
    for (const name of ['enabled','outlineEnabled','dynamicEnabled','centerDot']) c[name]=typeof c[name]==='boolean'?c[name]:defaults()[name];
    for (const name of ['color','outlineColor']) c[name]=/^#[0-9a-f]{6}$/i.test(c[name])?c[name]:defaults()[name];
    return c;
  }
  function key(c,pulse=1) {
    const scale=Number.isFinite(c.size)?clamp(f(c.size),f(.1),4):1;
    const boost=Number.isFinite(pulse)?round(f(clamp(f(f(pulse)-1),0,1)*8)):0;
    return {style:c.style??'LUNAR', length:clamp(round(f(f(c.length)*scale)),0,128),width:clamp(round(f(f(c.width)*scale)),1,48),
      gap:clamp(round(f(f(c.gap)*scale))+boost,0,88),dot:c.centerDot,dotSize:clamp(round(f(c.dotSize*scale)),1,32),
      outline:c.outlineEnabled?clamp(c.outlineThickness,1,4):0};
  }
  function square(x,y,size) { const start=Math.trunc(-size/2); return x>=start&&x<start+size&&y>=start&&y<start+size; }
  function ring(distance,radius,stroke) { return distance<=radius+.25&&distance>Math.max(-.5,radius-stroke+.25); }
  function contains(k,x,y) {
    if(k.dot&&square(x,y,k.dotSize)) return true;
    const center=k.width%2===0?-.5:0;
    const dx=x-center,dy=y-center,ax=Math.abs(dx),ay=Math.abs(dy),half=(k.width-1)/2;
    const length=k.length,gap=k.gap;
    switch(k.style) {
      case 'DOT': return square(x,y,k.width);
      case 'SMALL_DOT': return square(x,y,Math.max(1,Math.trunc(k.width/2)));
      case 'CIRCLE': return ring(Math.hypot(dx,dy),Math.max(1,length)+gap,k.width);
      case 'DIAMOND': return ring(ax+ay,Math.max(1,length)+gap,k.width);
      case 'SQUARE': return ring(Math.max(ax,ay),Math.max(1,length)+gap,k.width);
      case 'CHEVRON': return dy>=-half&&dy<=length+half&&ax<=length+half&&Math.abs(dy-ax)<=half+.25;
      case 'BRACKETS': return (Math.abs(ax-(gap+length))<=half&&ay<=length)||(ay>Math.max(0,length-k.width)&&ay<=length&&ax>=gap&&ax<=gap+length);
      case 'X': return ax>gap&&ax<=gap+length&&Math.abs(ax-ay)<=half+.25;
      default: {
        const l=k.style==='SMALL_CROSS'?Math.max(1,Math.trunc(length/2)):length;
        const g=k.style==='PLUS'?0:k.style==='SMALL_CROSS'?Math.trunc(gap/2):gap;
        if(l===0) return square(x,y,k.width);
        const horizontal=ay<=half&&ax>g&&ax<=g+l;
        const vertical=ax<=half&&ay>g&&ay<=g+l&&(k.style!=='T'||dy>0);
        return horizontal||vertical||(g===0&&ax<=half&&ay<=half);
      }
    }
  }
  function runs(pixels,side,offset) {
    const rectangles=[]; let previous=new Map();
    for(let y=0;y<side;y++) {
      const row=new Map(); let x=0;
      while(x<side) {
        if(!pixels[y*side+x]) { x++; continue; }
        const start=x; while(x<side&&pixels[y*side+x]) x++;
        const id=`${start},${x-start}`; let index=previous.get(id);
        if(index!==undefined) rectangles[index].height++;
        else { index=rectangles.length; rectangles.push({x:start-offset,y:y-offset,width:x-start,height:1}); }
        row.set(id,index);
      }
      previous=row;
    }
    return rectangles;
  }
  function build(k) {
    const extent=k.length+k.gap+k.width+k.dotSize+k.outline+3,side=extent*2+1;
    const ink=new Uint8Array(side*side),outline=new Uint8Array(side*side);
    for(let y=-extent;y<=extent;y++) for(let x=-extent;x<=extent;x++) if(contains(k,x,y)) ink[(y+extent)*side+x+extent]=1;
    if(k.outline>0) for(let y=0;y<side;y++) for(let x=0;x<side;x++) if(ink[y*side+x]) {
      for(let dy=-k.outline;dy<=k.outline;dy++) for(let dx=-k.outline;dx<=k.outline;dx++) {
        const nx=x+dx,ny=y+dy;
        if(nx>=0&&ny>=0&&nx<side&&ny<side) outline[ny*side+nx]=1;
      }
    }
    for(let i=0;i<ink.length;i++) if(ink[i]) outline[i]=0;
    return {ink:runs(ink,side,extent),outline:runs(outline,side,extent)};
  }
  const cache=new Map();
  function geometry(c,pulse=1) {
    const k=key(c,pulse),id=JSON.stringify(k);
    if(cache.has(id)) { const shape=cache.get(id); cache.delete(id); cache.set(id,shape); return shape; }
    const shape=build(k); cache.set(id,shape);
    if(cache.size>64) cache.delete(cache.keys().next().value);
    return shape;
  }
  function pulseAt(elapsed) {
    if(elapsed<0||elapsed>=220) return 1;
    const progress=clamp(f(elapsed/220),0,1);
    const pulse=f(Math.sin(progress*Math.PI));
    const wobble=f(f(f(Math.sin(progress*Math.PI*3))*f(.10))*f(1-progress));
    return f(f(1+f(f(.42)*pulse))+wobble);
  }
  function draw(ctx,x,y,c,pulse=1) {
    if(!c||!c.enabled) return;
    const shape=geometry(c,pulse);
    ctx.save(); ctx.globalAlpha=round(f(255*clamp(c.opacity,f(.1),1)))/255;
    ctx.fillStyle=c.outlineColor;
    shape.outline.forEach(r=>ctx.fillRect(x+r.x,y+r.y,r.width,r.height));
    ctx.fillStyle=c.color;
    shape.ink.forEach(r=>ctx.fillRect(x+r.x,y+r.y,r.width,r.height));
    ctx.restore();
  }
  class History {
    constructor(){this.items=[];}
    checkpoint(c){if(this.items.length===32)this.items.shift();this.items.push({...c});}
    undo(c){if(this.items.length)Object.assign(c,this.items.pop());}
    get available(){return this.items.length>0;}
  }
  const api={defaults,templates,styles,applyTemplate,matches,normalize,key,geometry,pulseAt,draw,History};
  if(typeof module!=='undefined'&&module.exports)module.exports=api;
  else root.AxialCrosshairCore=api;
})(typeof window!=='undefined'?window:globalThis);
