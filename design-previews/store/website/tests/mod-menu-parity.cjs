// Compare filtering and spring motion to the actual standalone Java client classes.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),os=require('node:os');
const {execFileSync}=require('node:child_process');
const core=require('../js/mod-menu-core.js');
const source=path.resolve(process.argv[2]||path.join(__dirname,'../../AxialClientAlpha'));
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'axial-mod-menu-'));
const queries=['',' HUD ','ore','bound','keyboard'];
const timeline=[[0,false],[0,true],[16,true],[33,true],[65,true],[80,false],[96,false],[112,true],[160,true],[300,true],[800,true]];
try{
 const dest=path.join(tmp,'com/axial/cosmetics/client');fs.mkdirSync(dest,{recursive:true});
 for(const name of ['ModMenuEntry','ToggleAnimation'])fs.copyFileSync(path.join(source,'src/main/java/com/axial/cosmetics/client',name+'.java'),path.join(dest,name+'.java'));
 const entries=core.entries.map(e=>`entries.add(new ModMenuEntry(${JSON.stringify(e.title)},${JSON.stringify(e.description)},ModMenuEntry.Category.${e.category},${JSON.stringify(e.icon)},${e.enabled===null?'null':`() -> ${e.enabled}`},null,null));`).join('\n');
 const program=`package com.axial.cosmetics.client;
 import java.util.*;
 public class BrowserMenuParity {
  public static void main(String[] args){
   var entries=new ArrayList<ModMenuEntry>();${entries}
   for(var entry:entries)for(var category:ModMenuEntry.Category.values())for(String query:new String[]{${queries.map(JSON.stringify).join(',')}})for(boolean only:new boolean[]{false,true})System.out.println(entry.matches(category,query,only)?1:0);
   long[] clock={0};var animation=new ToggleAnimation(false,()->clock[0]);
   ${timeline.map(([now,on])=>`clock[0]=${now}000000L;System.out.println(animation.value(${on}));`).join('\n')}
  }
 }`;
 fs.writeFileSync(path.join(dest,'BrowserMenuParity.java'),program);
 let javaHome=process.env.JAVA_HOME;
 if(!javaHome&&process.platform==='darwin')javaHome=execFileSync('/usr/libexec/java_home',[],{encoding:'utf8'}).trim();
 const bin=name=>javaHome?path.join(javaHome,'bin',name):name;
 execFileSync(bin('javac'),['-d',tmp,...fs.readdirSync(dest).filter(n=>n.endsWith('.java')).map(n=>path.join(dest,n))],{stdio:'pipe'});
 const lines=execFileSync(bin('java'),['-cp',tmp,'com.axial.cosmetics.client.BrowserMenuParity'],{encoding:'utf8'}).trim().split('\n');
 let i=0;const state=core.defaults();
 for(const entry of core.entries)for(const [category] of core.categories)for(const query of queries)for(const only of [false,true])assert.equal(core.matches(entry,category,query,only,state),lines[i++]==='1');
 const motion=new core.ToggleAnimation(false,0);
 for(const [now,on] of timeline)assert.ok(Math.abs(motion.value(on,now)-Number(lines[i++]))<1e-6,'Spring mismatch');
 const restored=core.restore({enabled:{'full-bright':true,'waypoints':true,hud:'bad'},settings:{compass:{scale:99},satchel:{title:'A'.repeat(100),titleColor:'broken'},client:{glint:-1},minimap:{shape:'REMOVED'}}});
 assert.equal(restored.enabled['full-bright'],true);assert.equal(restored.enabled.hud,true);assert.equal(restored.enabled.waypoints,undefined);
 assert.equal(restored.settings.compass.scale,3);assert.equal(restored.settings.client.glint,100);assert.equal(restored.settings.satchel.title.length,64);assert.equal(restored.settings.satchel.titleColor,'#f900df');assert.equal(restored.settings.minimap.shape,'SQUARE');
 assert.equal(new Set(core.entries.map(e=>e.id)).size,core.entries.length);
 for(const e of core.entries){assert.ok(core.categories.some(([id])=>id===e.category));if(e.settings&&!['crosshair','waypoints','gangs','enchantments'].includes(e.settings))assert.ok(core.schemas[e.settings]);}
 const a=core.defaults(),b=core.defaults();a.settings.cps.titleColor='#000000';assert.notEqual(a.settings.cps.titleColor,b.settings.cps.titleColor);
 console.log(`PASS: ${core.entries.length*core.categories.length*queries.length*2} filter cases and ${timeline.length} spring samples match Java; state validation and settings routes passed.`);
}finally{fs.rmSync(tmp,{recursive:true,force:true});}
