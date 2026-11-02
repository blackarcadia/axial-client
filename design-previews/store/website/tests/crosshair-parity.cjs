/* Compare the browser port to the unmodified Java source in the neighbouring client project.
 * Usage: node tests/crosshair-parity.cjs [path/to/AxialClientAlpha]
 * Requires JDK 25 (the client toolchain). Compiles copies in a temporary directory.
 */
const assert=require('node:assert/strict');
const fs=require('node:fs');
const os=require('node:os');
const path=require('node:path');
const {execFileSync}=require('node:child_process');
const core=require('../js/crosshair-core.js');
const source=path.resolve(process.argv[2]||path.join(__dirname,'../../AxialClientAlpha'));
const packagePath='src/main/java/com/axial/cosmetics/client';
const temp=fs.mkdtempSync(path.join(os.tmpdir(),'axial-crosshair-parity-'));
const fixtures=core.templates.map(p=>({config:core.applyTemplate(core.defaults(),p),pulse:1}));
for(const style of core.styles) for(let i=0;i<9;i++)fixtures.push({config:{...core.defaults(),style,
  size:[.1,.35,.65,1,1.45,2.25,4,NaN,1][i],length:[0,1,2,4,7,15,32,4,Infinity][i],
  width:1+i%4,gap:i%5,centerDot:i%2===0,dotSize:1+i%8,outlineEnabled:i%3!==0,outlineThickness:1+i%4},pulse:i%2?1.42:1});
const float=value=>Number.isNaN(value)?'Float.NaN':value===Infinity?'Float.POSITIVE_INFINITY':`${value}f`;
function emitFixture({config:c,pulse}){
  return `c=new CrosshairConfig(); c.style=CrosshairConfig.CrosshairStyle.${c.style}; c.size=${float(c.size)}; c.length=${float(c.length)}; c.width=${float(c.width)}; c.gap=${float(c.gap)}; c.centerDot=${c.centerDot}; c.dotSize=${c.dotSize}; c.outlineEnabled=${c.outlineEnabled}; c.outlineThickness=${c.outlineThickness}; print(c,${float(pulse)});`;
}
const elapsed=[-1,0,1,30,55,80,110,165,219,220,1000];
try {
  const dest=path.join(temp,'com/axial/cosmetics/client');fs.mkdirSync(dest,{recursive:true});
  for(const name of ['CrosshairConfig','CrosshairGeometry','CrosshairDynamicState','CrosshairPresets'])fs.copyFileSync(path.join(source,packagePath,name+'.java'),path.join(dest,name+'.java'));
  const runner=`package com.axial.cosmetics.client;
import java.util.*;
public class BrowserParity {
  static String rects(List<CrosshairGeometry.Rect> rs){var out=new StringJoiner(",","[","]");for(var r:rs)out.add("["+r.x()+","+r.y()+","+r.width()+","+r.height()+"]");return out.toString();}
  static void print(CrosshairConfig c,float pulse){var k=CrosshairGeometry.Key.of(c,pulse);var s=CrosshairGeometry.build(k);System.out.println("{"+(char)34+"ink"+(char)34+":"+rects(s.ink())+","+(char)34+"outline"+(char)34+":"+rects(s.outline())+"}");}
  public static void main(String[] args){CrosshairConfig c;${fixtures.map(emitFixture).join('\n')}
    for(long t:new long[]{${elapsed.join(',')}})System.out.println(CrosshairDynamicState.atElapsedMillis(t));
    for(var p:CrosshairPresets.templates())print(p.preview(new CrosshairConfig()),1);
  }
}`.replaceAll('\\"','\"');
  fs.writeFileSync(path.join(dest,'BrowserParity.java'),runner);
  let javaHome=process.env.JAVA_HOME;
  if(!javaHome&&process.platform==='darwin')javaHome=execFileSync('/usr/libexec/java_home',[],{encoding:'utf8'}).trim();
  const bin=name=>javaHome?path.join(javaHome,'bin',name):name;
  execFileSync(bin('javac'),['-d',temp,...fs.readdirSync(dest).filter(n=>n.endsWith('.java')).map(n=>path.join(dest,n))],{stdio:'pipe'});
  const results=execFileSync(bin('java'),['-cp',temp,'com.axial.cosmetics.client.BrowserParity'],{encoding:'utf8',maxBuffer:20*1024*1024}).trim().split('\n');
  const shapeOf=fixture=>{
    const shape=core.geometry(fixture.config,fixture.pulse);
    const rects=rs=>rs.map(r=>[r.x,r.y,r.width,r.height]);
    return {ink:rects(shape.ink),outline:rects(shape.outline)};
  };
  fixtures.forEach((fixture,i)=>assert.deepEqual(shapeOf(fixture),JSON.parse(results[i]),`Java geometry mismatch at fixture ${i}: ${fixture.config.style}`));
  elapsed.forEach((t,i)=>assert.ok(Math.abs(core.pulseAt(t)-Number(results[fixtures.length+i]))<1e-7,`Pulse mismatch at ${t}ms`));
  fixtures.slice(0,20).forEach((fixture,i)=>assert.deepEqual(shapeOf(fixture),JSON.parse(results[fixtures.length+elapsed.length+i]),`Java preset mismatch: ${core.templates[i].name}`));
  const signature=c=>JSON.stringify(core.geometry(c).ink);
  assert.equal(new Set(fixtures.slice(0,20).map(f=>signature(f.config))).size,20,'All templates must be distinct');
  const history=new core.History(),c=core.defaults();
  for(let i=0;i<40;i++){c.length=i;history.checkpoint(c);}c.length=99;
  for(let i=39;i>=8;i--){history.undo(c);assert.equal(c.length,i);}assert.equal(history.available,false);
  Object.assign(c,{enabled:false,color:'#123456',outlineColor:'#abcdef',opacity:.7,dynamicEnabled:true});
  core.applyTemplate(c,core.templates[9]);assert.equal(c.color,'#123456');assert.equal(c.enabled,false);assert.equal(c.dynamicEnabled,true);assert.equal(c.opacity,.7);
  assert.equal(core.normalize({style:'REMOVED',width:-2,size:999,opacity:0}).width,1);
  const calls=[],ctx={save(){},restore(){},fillRect(...args){calls.push(args);}};
  core.draw(ctx,0,0,{...core.defaults(),enabled:false});assert.equal(calls.length,0);
  core.draw(ctx,0,0,core.defaults());assert.ok(calls.length>0);
  console.log(`PASS: ${fixtures.length} geometry fixtures, all 20 Java presets, ${elapsed.length} pulse timings, history, normalization, template preservation, and rendering guards.`);
} finally {fs.rmSync(temp,{recursive:true,force:true});}
