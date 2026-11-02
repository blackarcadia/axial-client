(function(){
  'use strict';
  const root=document.querySelector('[data-news]');
  if(!root)return;
  const status=document.querySelector('#news-status');
  const retry=document.querySelector('#news-retry');
  const articleMode=root.dataset.news==='article';
  const base=articleMode?'../../':'../';
  const params=new URLSearchParams(location.search);
  const articleId=params.get('id');
  const source='https://axialprisons.com/forums/news/';
  let busy=false;
  function element(tag,className,text){const node=document.createElement(tag);if(className)node.className=className;if(text!==undefined)node.textContent=text;return node;}
  function safeUrl(value,baseUrl){
    if(typeof value!=='string'||!value.trim())return null;
    try{const url=new URL(value,baseUrl);return ['https:','http:'].includes(url.protocol)?url.href:null;}catch{return null;}
  }
  function contentFor(post){
    const fragment=DOMPurify.sanitize(post.html||'',{
      RETURN_DOM_FRAGMENT:true,
      ALLOWED_TAGS:['p','div','span','br','hr','h1','h2','h3','h4','h5','h6','strong','b','em','i','u','s','del','sup','sub','ul','ol','li','blockquote','pre','code','a','img','figure','figcaption','table','thead','tbody','tr','td','th','details','summary'],
      ALLOWED_ATTR:['href','src','alt','title','colspan','rowspan','start','open'],
      ALLOW_DATA_ATTR:false,ALLOW_ARIA_ATTR:false
    });
    fragment.querySelectorAll('a').forEach(link=>{
      const url=safeUrl(link.getAttribute('href'),post.sourceUrl);
      if(!url){link.removeAttribute('href');return;}
      link.href=url;link.target='_blank';link.rel='noopener noreferrer';
    });
    fragment.querySelectorAll('img').forEach(img=>{
      const url=safeUrl(img.getAttribute('src'),post.sourceUrl);
      if(!url){img.remove();return;}
      img.src=url;img.loading='lazy';img.decoding='async';img.referrerPolicy='no-referrer';
    });
    return fragment;
  }
  function metadata(post){
    const row=element('div','news-meta');
    if(post.publishedAt){const date=new Date(post.publishedAt);if(!Number.isNaN(date.getTime())){const time=element('time','',new Intl.DateTimeFormat('en-GB',{day:'numeric',month:'short',year:'numeric'}).format(date));time.dateTime=post.publishedAt;row.append(time);}}
    row.append(element('span','',post.author||'AXIAL'));return row;
  }
  function card(post,index){
    const link=element('a','news-card');link.href='article/?id='+encodeURIComponent(post.id);
    const fragment=contentFor(post);const image=fragment.querySelector('img');
    const media=element('div','news-card-media');
    if(image){media.append(image.cloneNode(true));media.classList.add('has-image');}
    else{media.append(element('span','news-card-brand','AXIAL'),element('span','news-card-type','CLIENT NEWS'));}
    const body=element('div','news-card-body');
    const title=element('h2','',post.title||'UNTITLED');
    const text=(fragment.textContent||'').replace(/\s+/g,' ').trim();
    const excerpt=text.length>180?text.slice(0,177).trimEnd()+'…':text;
    body.append(metadata(post),title);
    if(excerpt)body.append(element('p','news-excerpt',excerpt));
    const action=element('span','news-read','READ ARTICLE');action.append(element('span','','↗'));body.append(action);
    link.append(media,body);link.style.setProperty('--card-order',String(Math.min(index,5)));return link;
  }
  function showArticle(post,posts){
    document.title=(post.title||'Article')+' — AxialClient News';
    const article=element('article','news-article');
    const header=element('header','news-article-header');
    header.append(element('p','news-eyebrow','AXIAL NEWS'),element('h1','',post.title||'UNTITLED'),metadata(post));
    const content=element('div','news-prose');content.append(contentFor(post));
    if(!content.textContent.trim()&&!content.querySelector('img'))content.append(element('p','','Read this article on the forum using the link below.'));
    const foot=element('footer','news-article-footer');
    const original=element('a','news-outline-link','VIEW ON FORUM ↗');original.href=post.sourceUrl;original.target='_blank';original.rel='noopener noreferrer';
    foot.append(original,element('p','','COMMENTS AND DISCUSSION ON AXIALPRISONS.'));
    article.append(header,content,foot);root.append(article);
    const more=posts.filter(item=>item.id!==post.id).slice(0,3);
    if(more.length){const related=element('section','news-related');related.append(element('h2','','MORE NEWS'));const grid=element('div','news-grid');more.forEach((post,i)=>{const c=card(post,i);c.href='../article/?id='+encodeURIComponent(post.id);grid.append(c);});related.append(grid);root.append(related);}
  }
  async function load(){
    if(busy)return;busy=true;retry.hidden=true;status.textContent='LOADING NEWS…';root.setAttribute('aria-busy','true');root.replaceChildren();
    try{
      const response=await fetch(base+'news-feed.php',{headers:{Accept:'application/json'},signal:AbortSignal.timeout(18000)});
      const type=response.headers.get('content-type')||'';
      if(!type.includes('application/json'))throw new Error('News is temporarily unavailable. Please try again shortly.');
      const data=await response.json();if(!response.ok)throw new Error(data.error||'News is temporarily unavailable.');
      if(!Array.isArray(data.posts))throw new Error('News is temporarily unavailable.');
      // Only the expected forum's public thread URLs can become source links.
      const posts=data.posts.filter(post=>post&&/^\d+$/.test(post.id)&&typeof post.html==='string'&&/^https:\/\/axialprisons\.com\/threads\/[^/?#]+\.\d+\/$/.test(post.sourceUrl));
      if(articleMode){
        const post=posts.find(post=>post.id===articleId);
        if(!post){status.textContent='THIS ARTICLE IS NO LONGER IN THE LATEST NEWS FEED. VIEW ALL POSTS ON THE FORUM.';const link=element('a','news-outline-link','OPEN FORUM ↗');link.href=source;link.target='_blank';link.rel='noopener noreferrer';root.append(element('h1','news-unavailable-title','ARTICLE UNAVAILABLE'),link);return;}
        showArticle(post,posts);
      }else if(posts.length){const grid=element('div','news-grid');posts.forEach((post,i)=>grid.append(card(post,i)));root.append(grid);}
      status.textContent=data.stale?'SHOWING SAVED NEWS. THE FORUM IS TEMPORARILY UNAVAILABLE.':posts.length?'':'NO NEWS POSTS YET.';
    }catch(error){status.textContent=error.name==='TimeoutError'?'NEWS TOOK TOO LONG TO LOAD. PLEASE TRY AGAIN.':error.message||'NEWS IS TEMPORARILY UNAVAILABLE.';retry.hidden=false;}
    finally{busy=false;root.setAttribute('aria-busy','false');}
  }
  retry.addEventListener('click',load);
  document.querySelector('#year').textContent=new Date().getFullYear();
  load();
})();
