(function(){
  'use strict';
  const core=window.AxialStoreCore,grid=document.querySelector('#store-products');
  if(!core||!grid)return;
  const status=document.querySelector('#store-status'),retry=document.querySelector('#store-retry'),currency=document.querySelector('#store-currency');
  const search=document.querySelector('#store-search'),count=document.querySelector('#product-count'),notice=document.querySelector('#store-notice');
  const dialog=document.querySelector('#store-checkout'),form=document.querySelector('#checkout-form'),username=document.querySelector('#checkout-username');
  const coupon=document.querySelector('#checkout-coupon'),submit=document.querySelector('#checkout-submit'),error=document.querySelector('#checkout-error');
  let category='all';
  let products=[],csrf='',account='',selected=null,loading=0,checkoutRequest=0,busy=false,trigger=null;
  const year=document.querySelector('#year');if(year)year.textContent=String(new Date().getFullYear());
  async function api(input){
    const url=new URL('store-api.php',location.href);url.searchParams.set('currency',currency.value);
    const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),40000);
    try{
      const response=await fetch(url,{method:input?'POST':'GET',headers:input?{'Content-Type':'application/json'}:{Accept:'application/json'},body:input?JSON.stringify(input):undefined,credentials:'same-origin',cache:'no-store',signal:controller.signal});
      let data;try{data=await response.json();}catch{throw new Error('The store could not load. Please try again in a moment.');}
      if(!response.ok){const failure=new Error(data.error||'The store could not complete this request.');failure.status=response.status;throw failure;}
      return data;
    }catch(failure){if(failure.name==='AbortError')throw new Error('The store took too long to respond. Please try again.');throw failure;}
    finally{clearTimeout(timer);}
  }
  function element(tag,className,text){const node=document.createElement(tag);if(className)node.className=className;if(text!==undefined)node.textContent=text;return node;}
  function productImage(product){const img=element('img');img.src=core.image(product);img.alt=product.name;img.loading='lazy';img.addEventListener('error',()=>{img.src='../icon.svg';},{once:true});return img;}
  function render(){
    grid.replaceChildren();const matches=core.search(products,search.value).filter(product=>category==='all'||/cape/i.test(product.name+' '+product.description));count.textContent=String(matches.length);
    status.hidden=matches.length>0;status.textContent=products.length?'No cosmetics match your search.':'The next collection is on its way. Check back soon.';
    for(const product of matches){
      const card=element('article','store-product'),art=element('button','store-product-art');art.type='button';art.setAttribute('aria-label','View '+product.name);
      art.append(productImage(product),element('span','store-product-tag','AXIALCLIENT'));art.addEventListener('click',()=>open(product,art));
      const body=element('div','store-product-body'),bottom=element('div','store-product-bottom');
      const price=element('p','store-price',core.money(product.price,product.currency));
      if(product.original)price.append(element('del','',core.money(product.original,product.currency)));
      const button=element('button','store-button',product.available?'VIEW COSMETIC ↗':product.reason||'UNAVAILABLE');button.type='button';button.addEventListener('click',()=>open(product,button));
      bottom.append(price,button);body.append(element('h3','',product.name),element('p','store-product-description',product.description),bottom);card.append(art,body);grid.append(card);
    }
  }
  async function load(){
    const request=++loading;retry.hidden=true;status.hidden=false;status.textContent='Loading the collection…';grid.setAttribute('aria-busy','true');grid.replaceChildren();count.textContent='—';
    try{
      const data=await api();if(request!==loading)return;
      if(!Array.isArray(data.products)||typeof data.csrf!=='string')throw new Error('The store returned an unreadable catalogue.');
      products=data.products;csrf=data.csrf;account=data.username||'';render();
    }catch(failure){if(request!==loading)return;products=[];status.hidden=false;status.textContent=failure.message;retry.hidden=false;}
    finally{if(request===loading)grid.setAttribute('aria-busy','false');}
  }
  function open(product,button){
    selected=product;trigger=button;document.querySelector('#checkout-title').textContent=product.name;
    document.querySelector('#checkout-price').textContent=core.money(product.price,product.currency);
    document.querySelector('#checkout-description').textContent=product.description;
    const img=document.querySelector('#checkout-image');img.src=core.image(product);img.alt=product.name;
    username.value=account;coupon.value='';error.hidden=true;submit.disabled=!product.available;
    submit.textContent=product.available?'CONTINUE TO PAYNOW ↗':product.reason||'UNAVAILABLE';
    if(!product.available){error.textContent='This cosmetic is currently unavailable for checkout.';error.hidden=false;}
    document.body.classList.add('store-modal-open');dialog.showModal();username.focus();
  }
  function close(){dialog.close();}
  document.querySelector('.store-dialog-close').addEventListener('click',close);
  dialog.addEventListener('click',event=>{if(event.target===dialog){const r=dialog.getBoundingClientRect();if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)close();}});
  dialog.addEventListener('close',()=>{checkoutRequest++;selected=null;busy=false;document.body.classList.remove('store-modal-open');if(trigger)trigger.focus();});
  form.addEventListener('submit',async event=>{
    event.preventDefault();if(busy||!selected||!selected.available||!form.reportValidity())return;
    busy=true;const request=++checkoutRequest;submit.disabled=true;submit.textContent='OPENING SECURE CHECKOUT…';error.hidden=true;
    try{
      const data=await api({csrf,product_id:selected.id,username:username.value.trim(),coupon:coupon.value.trim(),currency:currency.value});
      const url=new URL(data.url);if(url.protocol!=='https:'||!(url.hostname==='paynow.gg'||url.hostname.endsWith('.paynow.gg')))throw new Error('The checkout address was invalid. Please try again.');
      if(request===checkoutRequest&&dialog.open){account=data.username;location.assign(url.href);}
    }catch(failure){
      if(request!==checkoutRequest)return;error.textContent=failure.message;error.hidden=false;
      if(failure.status===401||failure.status===403){try{const fresh=await api();csrf=fresh.csrf;}catch{}}
    }finally{if(request===checkoutRequest){busy=false;submit.disabled=false;submit.textContent='CONTINUE TO PAYNOW ↗';}}
  });
  document.querySelectorAll('[data-category]').forEach(button=>button.addEventListener('click',()=>{
    category=button.dataset.category;
    document.querySelectorAll('[data-category]').forEach(item=>item.setAttribute('aria-pressed',String(item===button)));
    document.querySelector('#catalogue-title').textContent=category==='capes'?'CAPES':'COSMETICS';
    // Keep a failed catalogue request visible until it is retried successfully.
    if(retry.hidden)render();
  }));
  currency.addEventListener('change',load);search.addEventListener('input',render);retry.addEventListener('click',load);
  const returned=new URL(location.href).searchParams.get('checkout');
  if(returned==='returned'||returned==='cancelled'){
    notice.hidden=false;notice.textContent=returned==='cancelled'?'Checkout was cancelled. You can choose another cosmetic or try again.':'Back from PayNow? Once your payment is confirmed, your cosmetic normally appears in your AxialClient wardrobe within one minute.';
  }
  load();
})();
