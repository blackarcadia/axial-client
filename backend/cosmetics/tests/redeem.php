<?php
declare(strict_types=1);
require dirname(__DIR__).'/Feedback.php';require dirname(__DIR__).'/RedeemCodes.php';
$db=new PDO('sqlite::memory:',null,null,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);
$db->exec(file_get_contents(dirname(__DIR__).'/schema.sql'));Feedback::migrate($db);RedeemCodes::migrate($db);
$catalog=json_decode(file_get_contents(dirname(__DIR__).'/catalog.json'),true,32,JSON_THROW_ON_ERROR);
$db->exec("INSERT INTO paynow_orders VALUES('view-test',100,1)");
$db->exec("INSERT INTO paynow_purchases VALUES('view-test','1','00000000-0000-4000-8000-000000999999','staff')");
$checks=0;
function check(bool $ok,string $why): void {global $checks;$checks++;if(!$ok)throw new RuntimeException($why);}
function rejected(callable $fn,int $status): void {try{$fn();}catch(FeedbackError $e){check($e->status===$status,'Expected rejection '.$status.'; got '.$e->status);return;}throw new RuntimeException('Expected rejection');}
function uuid(int $i): string {return '00000000-0000-4000-8000-'.sprintf('%012d',$i);}
function body(string $code,int $request): array {return ['code'=>$code,'requestId'=>uuid($request)];}
function owns(string $account,string $cosmetic): bool {global $db;$q=$db->prepare('SELECT 1 FROM effective_grants WHERE uuid=? AND cape=?');$q->execute([$account,$cosmetic]);return (bool)$q->fetchColumn();}
check(owns(uuid(999999),'staff'),'Migrated view preserves paid grants');$db->exec("INSERT INTO paynow_refunds VALUES('view-refund','view-test',100)");check(!owns(uuid(999999),'staff'),'Migrated view preserves full refunds');
$one=RedeemCodes::create($db,$catalog,'magenta_city','Single use');
check(!str_contains(json_encode(RedeemCodes::listing($db)),$one['code']),'Code text is never returned in listings');
check(!str_contains(json_encode($db->query('SELECT * FROM redeem_codes')->fetchAll()),str_replace('-','',$one['code'])),'Only code hashes are persisted');
rejected(fn()=>Feedback::handle($db,'redeem',body($one['code'],100),'not-a-session','unauthenticated'),401);
$token=bin2hex(random_bytes(32));$db->prepare('INSERT INTO feedback_sessions VALUES(?,?,?,?,?)')->execute([hash('sha256',$token),uuid(1),'VerifiedPlayer',time(),time()+600]);
$b=body(strtolower(str_replace('-',' ',$one['code'])),101);$b['uuid']=uuid(999);
$r=Feedback::handle($db,'redeem',$b,$token,'network-a');
check($r['uuid']===uuid(1)&&owns(uuid(1),'magenta_city')&&!owns(uuid(999),'magenta_city'),'Grant belongs only to the verified session, ignoring claimed UUIDs');
check($r['cosmetic']==='magenta_city'&&in_array('magenta_city',$r['cosmetics'],true),'Response provides verified ownership for instant client refresh');
$retry=Feedback::handle($db,'redeem',$b,$token,'network-a');check($retry===$r,'Lost-response retry is idempotent');
check((int)$db->query('SELECT uses FROM redeem_codes WHERE id='.$one['id'])->fetchColumn()===1,'Retry never consumes another use');
rejected(fn()=>RedeemCodes::redeem($db,body($one['code'],102),uuid(2),'network-b'),400);
$other=RedeemCodes::create($db,$catalog,'magenta_city','Already owned');
rejected(fn()=>RedeemCodes::redeem($db,body($other['code'],103),uuid(1),'network-a'),409);
check((int)$db->query('SELECT uses FROM redeem_codes WHERE id='.$other['id'])->fetchColumn()===0,'Already-owned claim does not consume code');
$off=RedeemCodes::create($db,$catalog,'purrr','Disabled');RedeemCodes::disable($db,$off['id']);rejected(fn()=>RedeemCodes::redeem($db,body($off['code'],104),uuid(2),'network-b'),400);
$expired=RedeemCodes::create($db,$catalog,'purrr','Expired');$db->exec('UPDATE redeem_codes SET expires_at=1 WHERE id='.$expired['id']);rejected(fn()=>RedeemCodes::redeem($db,body($expired['code'],105),uuid(2),'network-b'),400);
$shared=RedeemCodes::create($db,$catalog,'ribcage','Two players',2);
RedeemCodes::redeem($db,body($shared['code'],106),uuid(2),'network-b');RedeemCodes::redeem($db,body($shared['code'],107),uuid(3),'network-c');
rejected(fn()=>RedeemCodes::redeem($db,body($shared['code'],108),uuid(4),'network-d'),400);
check(owns(uuid(2),'ribcage')&&owns(uuid(3),'ribcage'),'Limited multi-use code grants independent accounts');
RedeemCodes::disable($db,$shared['id']);$db->exec('DELETE FROM grants');check(owns(uuid(2),'ribcage'),'Disabling codes and editing manual grants retains redeemed unlocks');
$unlimited=RedeemCodes::create($db,$catalog,'swim_bands','Community',0);foreach([5,6,7] as $i)RedeemCodes::redeem($db,body($unlimited['code'],200+$i),uuid($i),'shared');
check((int)$db->query('SELECT uses FROM redeem_codes WHERE id='.$unlimited['id'])->fetchColumn()===3,'Unlimited mode records uses');
foreach(array_keys($catalog) as $i=>$id){$issued=RedeemCodes::create($db,$catalog,$id,'Catalogue check');$result=RedeemCodes::redeem($db,body($issued['code'],500+$i),uuid(100+$i),'catalogue');check($result['cosmetic']===$id,'Every current cosmetic is redeemable: '.$id);}
for($i=0;$i<10;$i++)rejected(fn()=>RedeemCodes::redeem($db,body('INVALID',700+$i),uuid(800),'bad-network'),400);
rejected(fn()=>RedeemCodes::redeem($db,body('INVALID',720),uuid(800),'bad-network'),429);
check(!str_contains(json_encode($db->query('SELECT action FROM audit')->fetchAll()),$one['code']),'Audit entries contain no code text');
echo "PASS: $checks redeem authentication, privacy, grant, retry, expiry, limits and catalogue checks\n";
