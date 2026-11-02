(function(root){
  'use strict';
  const currencies=['gbp','usd','eur','cad','aud'];
  function currency(value){return currencies.includes(value)?value:'gbp';}
  function money(value,code){
    if(!Number.isSafeInteger(value)||value<0)throw new Error('Invalid price');
    const formatter=new Intl.NumberFormat('en-GB',{style:'currency',currency:currency(code).toUpperCase()});
    return formatter.format(value/(10**formatter.resolvedOptions().maximumFractionDigits));
  }
  function image(product){
    try{const url=new URL(product.image);if(url.protocol==='https:')return url.href;}catch{}
    return product.id==='619762575072043008'?'../img/store/staff.png':'../icon.svg';
  }
  function search(products,query){const text=String(query).trim().toLowerCase();return products.filter(p=>(p.name+' '+p.description).toLowerCase().includes(text));}
  const api={currency,money,image,search};
  if(typeof module==='object'&&module.exports)module.exports=api;else root.AxialStoreCore=api;
})(typeof globalThis!=='undefined'?globalThis:this);
