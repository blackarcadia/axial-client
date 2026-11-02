const assert=require('node:assert/strict');
const core=require('../js/hud-core.js');
// The client's center snap includes an 8 GUI-pixel threshold, then clamps to bounds.
assert.equal(core.snapAxis(444,52,957),452);
assert.equal(core.snapAxis(460,52,957),452);
assert.equal(core.snapAxis(443,52,957),443);
assert.equal(core.snapAxis(461,52,957),461);
assert.equal(core.snapAxis(452,52,957),452);
assert.equal(core.snapAxis(450,52,957,false),450);
assert.equal(core.snapAxis(391,131,957),391);
assert.equal(core.snapAxis(413,131,957),413);
assert.equal(core.snapAxis(414,131,957),413); // Odd-width rectangle rounds half-pixels as Java does.
for(const [id,e] of Object.entries(core.elements)){
  assert.deepEqual(core.position(id,-100,-100),{x:0,y:0});
  assert.deepEqual(core.position(id,5000,5000),{x:957-e.width,y:514-e.height});
  const atCenter=core.position(id,478-e.width/2,257-e.height/2);
  assert.ok(Math.abs(atCenter.x+e.width/2-478)<=.5);
  assert.ok(Math.abs(atCenter.y+e.height/2-257)<=.5);
  const layout=core.defaults();layout[id]={x:-400,y:10000};
  assert.deepEqual(core.restore(layout)[id],{x:0,y:514-e.height});
}
assert.deepEqual(core.restore({cps:{x:NaN,y:Infinity}}),core.defaults());
assert.deepEqual(core.restore(null),core.defaults());
const a=core.defaults(),b=core.defaults();a.cps.x=0;assert.notEqual(b.cps.x,0);
console.log('PASS: inclusive center snapping, odd-size rounding, all HUD bounds, saved-layout validation, and independent resets.');
