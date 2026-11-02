/* Move-mode geometry ported from AxialConfigScreen.snapPositionX/Y and the
 * HUD move mixins in AxialClientAlpha. The backend is in axialutils-1.0-SNAPSHOT.jar.
 * One coordinate unit is one Minecraft GUI pixel; the screenshot is GUI scale 2.
 */
(function(root){
  'use strict';
  const WIDTH=957,HEIGHT=514,SNAP_DISTANCE=8;
  const elements={
    information:{width:131,height:79,x:28,y:28,name:'INFORMATION HUD'},
    cps:{width:52,height:28,x:805,y:55,name:'CPS HUD'},
    armor:{width:129,height:50,x:775,y:410,name:'ARMOR HUD'},
  };
  const clamp=(value,min,max)=>Math.max(min,Math.min(max,value));
  function snapAxis(desired,elementSize,screenSize,snap=true){
    let position=Math.trunc(desired);
    const center=Math.trunc(screenSize/2);
    if(snap&&Math.abs(position+elementSize/2-center)<=SNAP_DISTANCE)position=Math.round(center-elementSize/2);
    return clamp(position,0,Math.max(0,screenSize-elementSize));
  }
  function position(id,x,y,snap=true){
    const element=elements[id];
    return {x:snapAxis(x,element.width,WIDTH,snap),y:snapAxis(y,element.height,HEIGHT,snap)};
  }
  function defaults(){return Object.fromEntries(Object.entries(elements).map(([id,e])=>[id,{x:e.x,y:e.y}]));}
  function restore(input){
    const layout=defaults();
    for(const id of Object.keys(elements)){
      const saved=input?.[id];
      if(saved&&Number.isFinite(saved.x)&&Number.isFinite(saved.y))layout[id]=position(id,saved.x,saved.y,false);
    }
    return layout;
  }
  const api={WIDTH,HEIGHT,SNAP_DISTANCE,elements,snapAxis,position,defaults,restore};
  if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.AxialHudCore=api;
})(typeof window!=='undefined'?window:globalThis);
