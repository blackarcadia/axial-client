<?php
declare(strict_types=1);
ini_set('display_errors','0');
header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: no-referrer');
header("Content-Security-Policy: default-src 'self'; style-src 'self'; script-src 'none'; frame-ancestors 'none'; form-action 'self'; base-uri 'none'");
function h(string $value): string {return htmlspecialchars($value,ENT_QUOTES|ENT_SUBSTITUTE,'UTF-8');}
function reply(array $body,int $status=200): never {
    http_response_code($status);header('Content-Type: application/json');echo json_encode($body,JSON_UNESCAPED_SLASHES);exit;
}
try {
    $config=require '/etc/axial-cosmetics/config.php';
    $catalog=json_decode(file_get_contents(dirname(__DIR__).'/catalog.json'),true,32,JSON_THROW_ON_ERROR);
    $db=new PDO('sqlite:'.$config['database'],null,null,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);
    $db->exec('PRAGMA busy_timeout=5000');
    require_once dirname(__DIR__).'/Feedback.php';
    Feedback::migrate($db);
    $path=parse_url($_SERVER['REQUEST_URI'],PHP_URL_PATH);
    $method=$_SERVER['REQUEST_METHOD'];
    if(str_starts_with($path,'/v1/feedback/')) {
        header('Cache-Control: no-store');
        if($method!=='POST')reply(['error'=>'Method not allowed'],405);
        $raw=file_get_contents('php://input',false,null,0,20001);
        if(strlen($raw)>20000)reply(['error'=>'Request too large'],413);
        $body=json_decode($raw,true);
        if(!is_array($body))reply(['error'=>'Invalid request'],400);
        $auth=$_SERVER['HTTP_AUTHORIZATION']??'';
        $token=str_starts_with($auth,'Bearer ')?substr($auth,7):'';
        $peer=hash_hmac('sha256',$_SERVER['REMOTE_ADDR']??'unknown',$config['rate_secret']);
        try {reply(Feedback::handle($db,substr($path,13),$body,$token,$peer));}
        catch(FeedbackError $ex){reply(['error'=>$ex->getMessage()],$ex->status);}
    }
    if($path==='/webhooks/paynow') {
        if($method!=='POST')reply(['error'=>'Method not allowed'],405);
        require dirname(__DIR__).'/PayNowWebhook.php';
        $raw=file_get_contents('php://input',false,null,0,1048577);
        try {
            $settings=json_decode(file_get_contents(dirname(__DIR__).'/paynow.json'),true,16,JSON_THROW_ON_ERROR);
            reply(PayNowWebhook::handle($db,$settings,$raw,$_SERVER['HTTP_PAYNOW_TIMESTAMP']??'',$_SERVER['HTTP_PAYNOW_SIGNATURE']??''));
        } catch(WebhookError $ex){reply(['error'=>$ex->getMessage()],$ex->status);}
    }
    if($path==='/health') {if($method!=='GET')reply(['error'=>'Method not allowed'],405);$db->query('SELECT value FROM metadata LIMIT 1');reply(['status'=>'ok']);}
    if($path==='/v1/cosmetics') {
        if($method!=='GET')reply(['error'=>'Method not allowed'],405);
        $db->beginTransaction();
        $revision=(int)$db->query("SELECT value FROM metadata WHERE name='revision'")->fetchColumn();
        $players=[];
        foreach($db->query('SELECT uuid FROM players ORDER BY uuid') as $row)$players[$row['uuid']]=[];
        foreach($db->query('SELECT uuid,cape FROM effective_grants ORDER BY uuid,cape') as $row)$players[$row['uuid']][]=$row['cape'];
        $db->commit();
        $etag='"cosmetics-'.$revision.'"';header('ETag: '.$etag);header('Cache-Control: public, max-age=15');
        if(($_SERVER['HTTP_IF_NONE_MATCH']??'')===$etag){http_response_code(304);exit;}
        reply(['version'=>1,'revision'=>$revision,'players'=>(object)$players]);
    }
    if($path!=='/admin'&&$path!=='/')reply(['error'=>'Not found'],404);
    header('Cache-Control: no-store');
    session_name('axial_admin');
    session_set_cookie_params(['httponly'=>true,'secure'=>true,'samesite'=>'Strict','path'=>'/']);
    session_start();
    if(isset($_SESSION['last'])&&time()-(int)$_SESSION['last']>3600)$_SESSION=[];
    $_SESSION['csrf']??=bin2hex(random_bytes(32));
    $error='';
    if($method==='POST') {
        if((int)($_SERVER['CONTENT_LENGTH']??0)>16384)reply(['error'=>'Request too large'],413);
        if(!is_string($_POST['csrf']??null)||!hash_equals($_SESSION['csrf'],$_POST['csrf']))reply(['error'=>'Invalid form token; reload the page'],403);
        $action=$_POST['action']??'';
        if($action==='login') {
            $peer=hash_hmac('sha256',$_SERVER['REMOTE_ADDR']??'unknown',$config['rate_secret']);
            $db->beginTransaction();
            $q=$db->prepare('SELECT started_at,attempts FROM login_limits WHERE peer=?');$q->execute([$peer]);$limit=$q->fetch(PDO::FETCH_ASSOC);
            if(!$limit||time()-(int)$limit['started_at']>900) {
                $q=$db->prepare('INSERT INTO login_limits(peer,started_at,attempts) VALUES(?,?,1) ON CONFLICT(peer) DO UPDATE SET started_at=excluded.started_at,attempts=1');$q->execute([$peer,time()]);$attempts=1;
            } else {
                $q=$db->prepare('UPDATE login_limits SET attempts=attempts+1 WHERE peer=?');$q->execute([$peer]);$attempts=(int)$limit['attempts']+1;
            }
            $db->commit();
            if($attempts>10){http_response_code(429);$error='Too many login attempts. Try again in 15 minutes.';}
            elseif(is_string($_POST['password']??null)&&password_verify($_POST['password'],$config['admin_hash'])) {
                session_regenerate_id(true);$_SESSION['admin']=true;$_SESSION['last']=time();$_SESSION['csrf']=bin2hex(random_bytes(32));
                $q=$db->prepare('DELETE FROM login_limits WHERE peer=?');$q->execute([$peer]);
                header('Location: /admin',true,303);exit;
            } else {$error='Incorrect password.';http_response_code(401);}
        } elseif(!empty($_SESSION['admin'])) {
            $_SESSION['last']=time();
            if($action==='logout'){$_SESSION=[];session_destroy();header('Location: /admin',true,303);exit;}
            if($action==='feedback_approve'||$action==='feedback_delete') {
                $id=filter_var($_POST['feedback_id']??null,FILTER_VALIDATE_INT);
                $reply=$_POST['reply']??'';
                if(!$id||!is_string($reply))reply(['error'=>'Invalid feedback action'],400);
                try {Feedback::moderate($db,$action==='feedback_approve'?'approve':'delete',$id,$reply);}
                catch(FeedbackError $ex){reply(['error'=>$ex->getMessage()],$ex->status);}
                $_SESSION['notice']=$action==='feedback_approve'?'Feedback approved. The player will be notified on login.':'Feedback deleted. Any pending approval notice is preserved.';
                header('Location: /admin#feedback',true,303);exit;
            }
            if($action==='paynow_secret') {
                require_once dirname(__DIR__).'/PayNowWebhook.php';
                $event=$_POST['event_type']??'';$secret=$_POST['signing_secret']??'';
                if(!is_string($event)||!in_array($event,PayNowWebhook::EVENTS,true)||!is_string($secret)||strlen(trim($secret))<16||strlen($secret)>512)
                    reply(['error'=>'Choose a supported event and enter its signing secret'],400);
                $q=$db->prepare('INSERT INTO paynow_secrets VALUES(?,?) ON CONFLICT(event_type) DO UPDATE SET secret=excluded.secret');$q->execute([$event,trim($secret)]);
                $_SESSION['notice']='Saved PayNow signing secret for '.$event;
                header('Location: /admin#paynow',true,303);exit;
            }
            if($action==='save') {
                $uuid=strtolower(trim(is_string($_POST['uuid']??null)?$_POST['uuid']:''));
                $capes=$_POST['capes']??[];
                if(!preg_match('/\A[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\z/',$uuid)||!is_array($capes)||count($capes)>count($catalog)) {
                    $error='Enter a valid Minecraft UUID and select cosmetics.';
                } else {
                    foreach($capes as $cape)if(!is_string($cape)||!isset($catalog[$cape]))reply(['error'=>'Unknown cosmetic'],400);
                    $db->beginTransaction();
                    $q=$db->prepare('INSERT INTO players(uuid,updated_at) VALUES(?,?) ON CONFLICT(uuid) DO UPDATE SET updated_at=excluded.updated_at');$q->execute([$uuid,time()]);
                    $q=$db->prepare('DELETE FROM grants WHERE uuid=?');$q->execute([$uuid]);
                    $q=$db->prepare('INSERT OR IGNORE INTO grants(uuid,cape) VALUES(?,?)');foreach($capes as $cape)$q->execute([$uuid,$cape]);
                    $q=$db->prepare('INSERT INTO audit(created_at,uuid,action) VALUES(?,?,?)');$q->execute([time(),$uuid,json_encode(array_values(array_unique($capes)))]);
                    $db->exec("UPDATE metadata SET value=value+1 WHERE name='revision'");$db->commit();
                    $_SESSION['notice']='Saved cosmetic access for '.$uuid;
                    header('Location: /admin?uuid='.rawurlencode($uuid),true,303);exit;
                }
            }
        } else {reply(['error'=>'Login required'],401);}
    } elseif($method!=='GET')reply(['error'=>'Method not allowed'],405);
    $logged=!empty($_SESSION['admin']);if($logged)$_SESSION['last']=time();
    $uuid=strtolower(trim(is_string($_GET['uuid']??null)?$_GET['uuid']:''));
    if($method==='POST'&&isset($_POST['uuid'])&&is_string($_POST['uuid']))$uuid=strtolower(trim($_POST['uuid']));
    $owned=[];
    if($logged&&preg_match('/\A[0-9a-f-]{36}\z/',$uuid)){$q=$db->prepare('SELECT cape FROM grants WHERE uuid=?');$q->execute([$uuid]);$owned=$q->fetchAll(PDO::FETCH_COLUMN);}
    $notice=$_SESSION['notice']??'';unset($_SESSION['notice']);
    $paid=[];
    if($logged) {
        $q=$db->prepare('SELECT DISTINCT e.cape FROM effective_grants e WHERE e.uuid=? AND EXISTS (SELECT 1 FROM paynow_purchases p WHERE p.uuid=e.uuid AND p.cape=e.cape)');$q->execute([$uuid]);$paid=$q->fetchAll(PDO::FETCH_COLUMN);
        $secretEvents=$db->query('SELECT event_type FROM paynow_secrets')->fetchAll(PDO::FETCH_COLUMN);
        $recentEvents=$db->query('SELECT event_type,received_at FROM paynow_events ORDER BY received_at DESC LIMIT 5')->fetchAll(PDO::FETCH_ASSOC);
        $partialRefunds=(int)$db->query('SELECT COUNT(*) FROM paynow_orders o JOIN (SELECT order_id,SUM(amount) AS refunded FROM paynow_refunds GROUP BY order_id) r USING(order_id) WHERE r.refunded<o.total')->fetchColumn();
    }
} catch(Throwable $ex) {
    if(isset($db)&&$db->inTransaction())$db->rollBack();
    error_log('[AxialCosmetics] '.get_class($ex));reply(['error'=>'Service temporarily unavailable'],503);
}
?>
<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Axial Cosmetics Admin</title><link rel="stylesheet" href="/admin.css"></head><body><main>
<header><div><span class="brand">AXIAL CLIENT</span><h1>Cosmetics admin</h1><p>Assign wardrobe access by Minecraft UUID.</p></div><?php if($logged):?><form method="post"><input type="hidden" name="csrf" value="<?=h($_SESSION['csrf'])?>"><input type="hidden" name="action" value="logout"><button>Sign out</button></form><?php endif?></header>
<?php if($error):?><p class="error" role="alert"><?=h($error)?></p><?php endif?>
<?php if($notice):?><p class="notice" role="status"><?=h($notice)?></p><?php endif?>
<?php if(!$logged):?>
<form method="post" class="login"><input type="hidden" name="csrf" value="<?=h($_SESSION['csrf'])?>"><input type="hidden" name="action" value="login"><label>Admin password<input type="password" name="password" required autocomplete="current-password"></label><button class="primary">Sign in</button></form>
<?php else:?>
<form method="get" class="lookup"><label>Minecraft UUID<input name="uuid" value="<?=h($uuid)?>" required maxlength="36" placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"></label><button>Load player</button></form>
<form method="post"><input type="hidden" name="csrf" value="<?=h($_SESSION['csrf'])?>"><input type="hidden" name="action" value="save"><label>Player UUID<input name="uuid" value="<?=h($uuid)?>" required maxlength="36" pattern="[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}" placeholder="Paste the player's Minecraft UUID"></label>
<?php if($paid):?><p class="notice">Cosmetics with purchase records: <?=h(implode(', ',$paid))?>. The checkboxes below control separate manual grants.</p><?php endif?>
<div class="catalog"><?php foreach($catalog as $id=>$cape):?><label class="cosmetic"><img src="/assets/<?=h($cape['image'])?>" alt="<?=h($cape['name'])?> cosmetic"><span><input type="checkbox" name="capes[]" value="<?=h($id)?>" <?=in_array($id,$owned,true)?'checked':''?>> <?=h($cape['name'])?></span><small><?=h($cape['category']??'Capes')?></small></label><?php endforeach?></div>
<p class="hint">Checked cosmetics grant manual access. Unchecking removes that manual grant; active purchases still grant access. Updated clients refresh within one minute.</p><button class="primary">Save cosmetic access</button></form>
<section><h2>Assigned players</h2><div class="players"><?php foreach($db->query('SELECT p.uuid,COUNT(g.cape) AS total FROM players p LEFT JOIN effective_grants g ON g.uuid=p.uuid GROUP BY p.uuid ORDER BY p.updated_at DESC LIMIT 100') as $row):?><a href="/admin?uuid=<?=h($row['uuid'])?>"><code><?=h($row['uuid'])?></code><span><?=h((string)$row['total'])?> cosmetics →</span></a><?php endforeach?></div></section>
<section id="feedback"><h2>Player feedback</h2><p>Latest 100 submissions. Approval queues a private notification for the player's next login. Deleting a submission preserves its undelivered approval notice.</p>
<?php $feedbackRows=$db->query('SELECT * FROM feedback ORDER BY id DESC LIMIT 100')->fetchAll(PDO::FETCH_ASSOC); if(!$feedbackRows):?><p>No feedback yet.</p><?php endif?>
<?php foreach($feedbackRows as $item):?><article class="feedback-note">
<h3>#<?=h((string)$item['id'])?> · <?=h($item['username'])?> <span class="feedback-status"><?=h($item['status'])?></span></h3>
<p><code><?=h($item['uuid'])?></code> · <?=h(gmdate('Y-m-d H:i',(int)$item['created_at']))?> UTC</p>
<div class="feedback-message"><?=nl2br(h($item['message']))?></div>
<?php if($item['status']==='pending'):?><form method="post"><input type="hidden" name="csrf" value="<?=h($_SESSION['csrf'])?>"><input type="hidden" name="action" value="feedback_approve"><input type="hidden" name="feedback_id" value="<?=h((string)$item['id'])?>"><label>Optional reply<textarea name="reply" maxlength="2000" rows="2" placeholder="Thank you for the suggestion!"></textarea></label><button class="primary">Approve &amp; notify player</button></form><?php endif?>
<form method="post" class="feedback-delete"><input type="hidden" name="csrf" value="<?=h($_SESSION['csrf'])?>"><input type="hidden" name="action" value="feedback_delete"><input type="hidden" name="feedback_id" value="<?=h((string)$item['id'])?>"><button>Delete feedback permanently</button></form>
</article><?php endforeach?></section>
<section id="paynow"><h2>PayNow connection</h2><p>Store: <code>559186693647761408</code><br>Product: <code>619762575072043008</code> → STAFF cape</p>
<p>In PayNow → Integrations → Webhooks, create one JSON webhook for each event below, using <code>https://api.axialclient.com/webhooks/paynow</code>. Paste each webhook's signing secret here. Secrets are saved privately and never displayed.</p>
<ul><?php foreach(['ON_ORDER_COMPLETED','ON_REFUND','ON_CHARGEBACK'] as $event):?><li><?=h($event)?>: <?=in_array($event,$secretEvents,true)?'Secret configured':'Awaiting signing secret'?></li><?php endforeach?></ul>
<form method="post"><input type="hidden" name="csrf" value="<?=h($_SESSION['csrf'])?>"><input type="hidden" name="action" value="paynow_secret"><label>Webhook event<select name="event_type"><option value="ON_ORDER_COMPLETED">Order Completed</option><option value="ON_REFUND">Refund</option><option value="ON_CHARGEBACK">Chargeback</option></select></label><label>Signing secret<input type="password" name="signing_secret" required minlength="16" maxlength="512" autocomplete="off"></label><button>Save signing secret</button></form>
<p class="hint">One-time purchases only. Full order refunds and chargebacks remove access from that order. Partial refunds require review so unrelated products in the same order are not removed. Manual grants and other valid purchases remain active. After configuring secrets, run a zero-cost test checkout and verify delivery before selling.</p>
<?php if($partialRefunds):?><p class="error"><?=h((string)$partialRefunds)?> partially refunded cosmetic order(s) need review.</p><?php endif?>
<h3>Recent verified events</h3><?php if(!$recentEvents):?><p>No verified PayNow events received yet.</p><?php endif?>
<ul><?php foreach($recentEvents as $event):?><li><?=h($event['event_type'])?> — <?=h(gmdate('Y-m-d H:i:s',(int)$event['received_at']))?> UTC</li><?php endforeach?></ul></section>
<?php endif?></main></body></html>
