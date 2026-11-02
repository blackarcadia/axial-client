<?php
declare(strict_types=1);
require dirname(__DIR__).'/store/store-api.php';
use AxialStore\Storefront;
use AxialStore\StoreError;
$checks=0;$requests=[];$mode='normal';
function check(bool $value,string $label):void{global $checks;$checks++;if(!$value)throw new RuntimeException($label);}
function rejected(callable $callback,int $status):void{try{$callback();}catch(StoreError $e){check($e->status===$status,'Expected status '.$status.', got '.$e->status);return;}throw new RuntimeException('Expected rejection');}
function product(string $id='619762575072043008',string $tag='axial-client-cosmetics'):array{return ['id'=>$id,'name'=>'Staff cape','description'=>'<p>Staff &amp; friends</p><script>no html</script>','hidden'=>false,'image_url'=>'javascript:alert(1)','sort_order'=>0,'price'=>9999,'pricing'=>['price_final'=>660,'price_original'=>1200],'currency'=>'gbp','tags'=>[['slug'=>$tag]],'allow_one_time_purchase'=>true,'stock'=>['available_to_purchase'=>true],'custom_variables'=>[],'gameservers'=>[]];}
$transport=function($method,$url,$headers,$body)use(&$requests,&$mode):array {
 $requests[]=compact('method','url','headers','body');
 if($mode==='expired')return [401,'{}'];
 if($mode==='busy')return [429,'{}'];
 if($mode==='broken')return [200,'<html>bad gateway</html>'];
 if(str_contains($url,'/customer/auth'))return [200,json_encode(['customer_token'=>'private-test-token'])];
 if(str_contains($url,'/store/products')) {
  $p=product();
  if($mode==='stock')$p['stock']['available_to_purchase']=false;
  if($mode==='hidden')$p['hidden']=true;
  if($mode==='subscription')$p['allow_one_time_purchase']=false;
  if($mode==='variables')$p['custom_variables']=[['name'=>'test']];
  if($mode==='servers')$p['gameservers']=[['id'=>'1']];
  if($mode==='price')unset($p['pricing']);
  return [200,json_encode([$p,product('123','prison-coins'),product('124','bundles')])];
 }
 if(str_contains($url,'/checkouts'))return [200,json_encode(['url'=>$mode==='evil'?'https://evil.example/pay':'https://checkout.paynow.gg/test-only'])];
 throw new RuntimeException('Unexpected API route');
};
$store=new Storefront($transport);$session=[];$ip='192.0.2.1';
$products=$store->catalogue('gbp',$session,$ip);
check(count($products)===1,'Unrelated products excluded even when upstream returns all');
check($products[0]['price']===660&&$products[0]['original']===1200,'Uses final and original PayNow prices');
check($products[0]['image']===null,'Unsafe image URL rejected');
check(!str_contains($products[0]['description'],'<'),'Description is plain text');
check(str_contains($requests[0]['url'],'tag=axial-client-cosmetics'),'API also filters by tag');
check(in_array('x-paynow-customer-ip: '.$ip,$requests[0]['headers'],true),'Visitor IP forwarded for pricing');
rejected(fn()=>$store->catalogue('xxx',$session,$ip),400);
$input=['username'=>'Test_Player','product_id'=>'619762575072043008','currency'=>'gbp','coupon'=>'TEST100','price'=>1];
$result=$store->checkout($input,$session,$ip);check($result['url']==='https://checkout.paynow.gg/test-only','Uses hosted checkout');
check(!str_contains(json_encode($result),'private-test-token'),'Customer token stays server-side');
check($session['username']==='Test_Player'&&$session['token']==='private-test-token','Customer token held in session');
$last=end($requests);check($last['body']['promo_codes']===['TEST100'],'Coupon forwarded');
check($last['body']['lines']===[['product_id'=>'619762575072043008','quantity'=>1,'subscription'=>false,'trial'=>false]],'Exact one-time product only; no client price accepted');
check(!str_contains($last['url'],'cart'),'Does not touch existing Prisons cart');
check($last['body']['return_url']==='https://axialclient.com/store/?checkout=returned','Return URL belongs to Axial Client');
check(in_array('Authorization: Customer private-test-token',$last['headers'],true),'Token sent only upstream');
$bad=$input;$bad['username']='invalid<>name';rejected(fn()=>$store->checkout($bad,$session,$ip),400);
$bad=$input;$bad['product_id']='123';rejected(fn()=>$store->checkout($bad,$session,$ip),400);
foreach(['stock','subscription','variables','servers'] as $mode)rejected(fn()=>$store->checkout($input,$session,$ip),409);
$mode='hidden';check($store->catalogue('gbp',$session,$ip)===[],'Hidden cosmetics excluded');
$mode='price';check($store->catalogue('gbp',$session,$ip)===[],'No invented fallback price');
$mode='evil';rejected(fn()=>$store->checkout($input,$session,$ip),502);
$mode='broken';rejected(fn()=>$store->catalogue('gbp',$session,$ip),502);
$mode='busy';rejected(fn()=>$store->catalogue('gbp',$session,$ip),429);
$mode='expired';rejected(function()use($store,&$session,$ip){$store->catalogue('gbp',$session,$ip);},401);check(!isset($session['token'])&&!isset($session['username']),'Expired token removed');
echo "PASS: $checks storefront catalogue, tag, checkout, pricing and session checks\n";
