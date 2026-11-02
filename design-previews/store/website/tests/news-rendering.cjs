/* Run with jsdom available (e.g. NODE_PATH=/path/to/test/node_modules node tests/news-rendering.cjs). */
const {JSDOM}=require('jsdom');
const fs=require('node:fs');
const assert=require('node:assert/strict');
const path=require('node:path');
const root=path.resolve(__dirname,'..');
const script=fs.readFileSync(path.join(root,'js/news.js'),'utf8');
const purifier=fs.readFileSync(path.join(root,'js/vendor/purify.min.js'),'utf8');
const post={id:'37',title:'<img src=x onerror=alert(1)> TEST',author:'Author',publishedAt:'2026-10-25T13:28:13+00:00',sourceUrl:'https://axialprisons.com/threads/test-article.37/',html:`<h2>Release notes</h2><p>Text <strong>bold</strong> and <em>italic</em>.</p><img src="/attachments/card.png" onerror="alert(1)"><a href="/wiki/">Wiki</a><ul><li>Item</li></ul><pre><code>const x = 1;</code></pre><table><tr><td>Cell</td></tr></table><script>alert(1)</script><iframe src="https://evil.example"></iframe><a href="javascript:alert(1)">Bad URL</a><svg onload="alert(1)"></svg><span style="position:fixed">Plain</span>`};
async function render({article=false,posts=[post],responseError=false,stale=false,id='37'}={}){
 const dom=new JSDOM(`<p id="news-status"></p><button id="news-retry" hidden>RETRY</button><div data-news="${article?'article':'index'}"></div><span id="year"></span>`,{url:`https://axialclient.com/news/${article?'article/?id='+id:''}`,runScripts:'outside-only'});
 const {window}=dom;window.AbortSignal=AbortSignal;
 window.fetch=async()=>({ok:!responseError,headers:{get:()=> 'application/json'},json:async()=>responseError?{error:'News is temporarily unavailable.'}:{posts,stale}});
 window.eval(purifier);window.eval(script);
 await new Promise(resolve=>setImmediate(resolve));
 return dom;
}
(async()=>{
 let dom=await render({article:true});let doc=dom.window.document;
 assert.equal(doc.querySelector('h1').textContent,post.title,'Title is literal text.');
 assert.ok(doc.querySelector('.news-prose strong'));assert.ok(doc.querySelector('.news-prose table'));assert.ok(doc.querySelector('.news-prose pre code'));
 assert.equal(doc.querySelector('.news-prose img').src,'https://axialprisons.com/attachments/card.png');
 assert.equal(doc.querySelector('.news-prose a').href,'https://axialprisons.com/wiki/');
 assert.equal(doc.querySelector('.news-prose a').rel,'noopener noreferrer');
 assert.equal(doc.querySelectorAll('.news-prose script,.news-prose iframe,.news-prose svg,[onerror],[onload],[style]').length,0,'Unsafe markup and inline styles removed.');
 assert.ok(!Array.from(doc.querySelectorAll('.news-prose a')).some(a=>a.href.startsWith('javascript:')));
 dom.window.close();
 dom=await render();doc=dom.window.document;
 assert.equal(doc.querySelectorAll('.news-card').length,1);assert.ok(doc.querySelector('.news-card').href.endsWith('/news/article/?id=37'));assert.equal(doc.querySelector('h2').textContent,post.title);dom.window.close();
 dom=await render({posts:[]});assert.match(dom.window.document.querySelector('#news-status').textContent,/NO NEWS/);dom.window.close();
 dom=await render({article:true,id:'999'});assert.match(dom.window.document.querySelector('h1').textContent,/UNAVAILABLE/);dom.window.close();
 dom=await render({responseError:true});assert.equal(dom.window.document.querySelector('#news-retry').hidden,false);dom.window.close();
 dom=await render({stale:true});assert.match(dom.window.document.querySelector('#news-status').textContent,/SAVED NEWS/);dom.window.close();
 dom=await render({posts:[{...post,sourceUrl:'https://foreign.example/threads/test.37/'}]});assert.equal(dom.window.document.querySelectorAll('.news-card').length,0);dom.window.close();
 console.log('News article formatting, URL resolution, unsafe markup removal, listing, missing, empty, stale, and failure states passed.');
})().catch(error=>{console.error(error);process.exitCode=1;});
