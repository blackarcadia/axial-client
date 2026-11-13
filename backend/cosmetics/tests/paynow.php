<?php
declare(strict_types=1);
require dirname(__DIR__).'/PayNowWebhook.php';
$settings=json_decode(file_get_contents(dirname(__DIR__).'/paynow.json'),true,16,JSON_THROW_ON_ERROR);
$db=new PDO('sqlite::memory:',null,null,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);
$db->exec(file_get_contents(dirname(__DIR__).'/schema.sql'));
$secret='test-only-signing-secret-never-used-in-production';
foreach(PayNowWebhook::EVENTS as $type){$q=$db->prepare('INSERT INTO paynow_secrets VALUES(?,?)');$q->execute([$type,$secret.$type]);}
$counter=0;$checks=0;$now=1793330000;
$uuid='00000000-0000-4000-8000-000000000123';$gift='00000000-0000-4000-8000-000000000456';
function check(bool $ok,string $message):void {global $checks;$checks++;if(!$ok)throw new RuntimeException($message);}
function send(string $type,array $body,?string $eventId=null,?string $key=null,?string $timestamp=null):array {
 global $db,$settings,$secret,$counter,$now;
 $raw=json_encode(['event_type'=>$type,'event_id'=>$eventId??(string)++$counter,'body'=>$body],JSON_THROW_ON_ERROR);
 $timestamp??=(string)($now*1000);
 return PayNowWebhook::handle($db,$settings,$raw,$timestamp,base64_encode(hash_hmac('sha256',$timestamp.'.'.$raw,$key??$secret.$type,true)),$now);
}
function rejected(callable $fn,int $status):void {try{$fn();}catch(WebhookError $e){check($e->status===$status,'Wrong rejection status');return;}throw new RuntimeException('Expected rejection');}
function owns(string $uuid):bool {global $db;$q=$db->prepare("SELECT 1 FROM effective_grants WHERE uuid=? AND cape='staff'");$q->execute([$uuid]);return (bool)$q->fetchColumn();}
function order(string $id='100'):array {global $settings,$uuid;return ['id'=>$id,'store_id'=>$settings['store_id'],'status'=>'completed','total_amount'=>1000,'customer'=>['minecraft'=>['id'=>str_replace('-','',$uuid)]],'lines'=>[['id'=>'101','product_id'=>'619762575072043008','quantity'=>1]]];}
function refund(string $order,string $id='200',int $amount=1000):array {global $settings;return ['id'=>$id,'order_id'=>$order,'store_id'=>$settings['store_id'],'status'=>'completed','amount'=>$amount];}
rejected(fn()=>send('ON_ORDER_COMPLETED',order(),key:'bad-signature'),401);
rejected(fn()=>send('ON_ORDER_COMPLETED',order(),timestamp:(string)(($now-301)*1000)),401);
rejected(fn()=>send('ON_ORDER_COMPLETED',order(),timestamp:(string)(($now+301)*1000)),401);
$o=order();$o['store_id']='999';rejected(fn()=>send('ON_ORDER_COMPLETED',$o),400);
$o=order();$o['status']='created';rejected(fn()=>send('ON_ORDER_COMPLETED',$o),400);
$o=order();unset($o['customer']);rejected(fn()=>send('ON_ORDER_COMPLETED',$o),422);
check((int)$db->query('SELECT COUNT(*) FROM paynow_events')->fetchColumn()===0,'Failed events roll back');
$first=send('ON_ORDER_COMPLETED',order(),eventId:'10000');check(owns($uuid),'Purchase grants Staff');
$revision=$db->query("SELECT value FROM metadata WHERE name='revision'")->fetchColumn();
check(send('ON_ORDER_COMPLETED',order(),eventId:'10000')['status']==='duplicate','Event deduplicated');
check($db->query("SELECT value FROM metadata WHERE name='revision'")->fetchColumn()===$revision,'Duplicate has no revision change');
send('ON_ORDER_COMPLETED',order());check((int)$db->query('SELECT COUNT(*) FROM paynow_purchases')->fetchColumn()===1,'Order deduplicated across events');
send('ON_REFUND',refund('100','200',500));check(owns($uuid),'Partial refund retains access for review');
send('ON_REFUND',refund('100','201',500));check(!owns($uuid),'Accumulated full refund revokes');
send('ON_ORDER_COMPLETED',order());check(!owns($uuid),'Replayed order cannot undo refund');
send('ON_ORDER_COMPLETED',order('110'));check(owns($uuid),'Second purchase restores independent access');
$charge=['id'=>'300','order_id'=>'110','store_id'=>$settings['store_id']];
rejected(fn()=>send('ON_CHARGEBACK',$charge,key:$secret.'ON_ORDER_COMPLETED'),401);
send('ON_CHARGEBACK',$charge);check(!owns($uuid),'Chargeback revokes only affected purchases');
$q=$db->prepare('INSERT INTO grants VALUES(?,?)');$q->execute([$uuid,'staff']);check(owns($uuid),'Manual access survives refund and chargeback');
send('ON_REFUND',refund('120','202'));send('ON_ORDER_COMPLETED',order('120'));
check((int)$db->query("SELECT COUNT(*) FROM effective_grants WHERE uuid='$uuid'")->fetchColumn()===1,'Refund before order cannot grant paid access');
$o=order('130');$o['lines'][0]['gift_to_customer']=['minecraft'=>['id'=>$gift]];send('ON_ORDER_COMPLETED',$o);check(owns($gift),'Gifts belong to recipient');
send('ON_REFUND',refund('130','203'));check(!owns($gift),'Gift revoked for recipient');
$o=order('140');$o['lines'][0]['product_id']='999';unset($o['customer']);send('ON_ORDER_COMPLETED',$o);
check((int)$db->query("SELECT COUNT(*) FROM paynow_orders WHERE order_id='140'")->fetchColumn()===0,'Unrelated server products ignored');
$o=order('150');$o['lines'][0]['subscription_id']='999';rejected(fn()=>send('ON_ORDER_COMPLETED',$o),400);
$o=order('160');$o['lines'][]=['id'=>'102','product_id'=>'619762575072043008','quantity'=>1,'gift_to_customer'=>['minecraft'=>['id'=>'not-a-uuid']]];
rejected(fn()=>send('ON_ORDER_COMPLETED',$o),422);check((int)$db->query("SELECT COUNT(*) FROM paynow_orders WHERE order_id='160'")->fetchColumn()===0,'Bad second line cannot partially fulfill order');
$newProducts=['624788824383295488'=>'white_sneakers','624789316765229056'=>'swim_bands','624790151096176640'=>'purrr','624789572366114816'=>'ribcage','624807205576642560'=>'ducking_around','624815138351882240'=>'magenta_city'];
foreach(array_keys($newProducts) as $i=>$productId){
    $cosmetic=$newProducts[$productId];
    check(($settings['products'][$productId]??null)===$cosmetic,'Product maps to '.$cosmetic);
    $orderId=(string)(600+$i);$o=order($orderId);
    $o['lines'][0]=['id'=>(string)(700+$i),'product_id'=>(string)$productId,'quantity'=>1];
    send('ON_ORDER_COMPLETED',$o);
    $q=$db->prepare('SELECT COUNT(*) FROM effective_grants WHERE uuid=? AND cape=?');$q->execute([$uuid,$cosmetic]);
    check((int)$q->fetchColumn()===1,'Purchase grants '.$cosmetic);
    send('ON_REFUND',refund($orderId,(string)(800+$i)));
    $q->execute([$uuid,$cosmetic]);check((int)$q->fetchColumn()===0,'Full refund removes '.$cosmetic);
}
echo "PASS: $checks PayNow security, purchase, gift, refund, replay and manual-grant checks\n";
