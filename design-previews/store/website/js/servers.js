(function(){
  'use strict';
  const input=document.querySelector('#prisons-address'),button=document.querySelector('#copy-server-ip'),status=document.querySelector('#server-copy-status');
  const label=button.querySelector('span');let resetTimer;
  button.addEventListener('click',async()=>{
    clearTimeout(resetTimer);
    try{
      await navigator.clipboard.writeText(input.value);
      label.textContent='COPIED';button.classList.add('is-copied');status.textContent='SERVER ADDRESS COPIED.';
      resetTimer=setTimeout(()=>{label.textContent='COPY IP';button.classList.remove('is-copied');status.textContent='';},2600);
    }catch{
      input.focus();input.select();status.textContent='ADDRESS SELECTED. PRESS CTRL+C OR CMD+C TO COPY.';
    }
  });
  document.querySelector('#year').textContent=new Date().getFullYear();
})();
