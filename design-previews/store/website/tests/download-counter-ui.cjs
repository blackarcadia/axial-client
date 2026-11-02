const {JSDOM}=require('jsdom');
const fs=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..');
const html=fs.readFileSync(path.join(root,'index.html'),'utf8');
const script=fs.readFileSync(path.join(root,'js/app.js'),'utf8');
async function render(response){
 const dom=new JSDOM(html,{url:'https://axialclient.com/',runScripts:'outside-only',pretendToBeVisual:true});
 const calls=[];const w=dom.window;
 w.matchMedia=()=>({matches:true});w.IntersectionObserver=class {observe(){}};w.AbortSignal=AbortSignal;
 w.fetch=async(url,options)=>{calls.push({url:String(url),options});return {ok:response.ok!==false,headers:{get:()=> 'application/json'},json:async()=>response};};
 w.eval(script);await new Promise(resolve=>setImmediate(resolve));return {dom,calls};
}
(async()=>{
 let {dom,calls}=await render({count:921,stale:false});let doc=dom.window.document;
 assert.equal(doc.querySelector('.download-counter').hidden,false);assert.equal(doc.querySelector('#download-count').textContent,'921');
 assert.equal(calls[0].url,'https://axialclient.com/download-counter.php');assert.ok(!calls[0].options.method||calls[0].options.method==='GET');
 doc.querySelectorAll('.download-card').forEach(link=>{link.addEventListener('click',e=>e.preventDefault());link.dispatchEvent(new dom.window.MouseEvent('click',{bubbles:true,cancelable:true}));});
 await new Promise(resolve=>setImmediate(resolve));assert.equal(calls.length,1,'Download clicks must not double-count GitHub statistics.');dom.window.close();
 for(const data of [{count:-1},{count:'921'},{ok:false}]){({dom}=await render(data));assert.equal(dom.window.document.querySelector('.download-counter').hidden,true,'Invalid/unavailable totals remain hidden.');dom.window.close();}
 ({dom}=await render({count:0}));assert.equal(dom.window.document.querySelector('#download-count').textContent,'0');assert.equal(dom.window.document.querySelector('.download-counter').hidden,false);dom.window.close();
 console.log('Combined download display, read-only requests, click behavior, invalid counts, failure, and real zero passed.');
})().catch(error=>{console.error(error);process.exitCode=1;});
