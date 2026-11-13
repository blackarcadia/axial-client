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
    require_once dirname(__DIR__).'/RedeemCodes.php';
    require_once dirname(__DIR__).'/Polls.php';
    Polls::migrate($db);
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
        catch(FeedbackError $ex){
            if($ex->retryAfter>0)header('Retry-After: '.$ex->retryAfter);
            reply(['error'=>$ex->getMessage(),'retryAfterSeconds'=>$ex->retryAfter],$ex->status);
        }
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
    require_once dirname(__DIR__).'/AdminDashboard.php';
    $page=AdminDashboard::page($_GET['page']??(isset($_GET['uuid'])?'cosmetics':'overview'));
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
            if($action==='code_create'||$action==='code_disable') {
                RedeemCodes::migrate($db);
                try {
                    if($action==='code_create') {
                        $maximum=filter_var($_POST['maximum']??1,FILTER_VALIDATE_INT);
                        $days=filter_var($_POST['days']??0,FILTER_VALIDATE_INT);
                        if($maximum===false||$days===false||$days<0||$days>3650)throw new FeedbackError('Enter valid code limits.',400);
                        $issued=RedeemCodes::create($db,$catalog,is_string($_POST['cosmetic']??null)?$_POST['cosmetic']:'',is_string($_POST['label']??null)?$_POST['label']:'',$maximum,$days>0?time()+$days*86400:null);
                        $_SESSION['issued_code']=$issued['code'];
                        $_SESSION['notice']='Code created. Copy it now; it will not be shown again.';
                    } else {
                        $id=filter_var($_POST['code_id']??null,FILTER_VALIDATE_INT);
                        if(!$id||$id<1)throw new FeedbackError('Choose a valid code.',400);
                        RedeemCodes::disable($db,$id);$_SESSION['notice']='Code disabled. Existing unlocks are retained.';
                    }
                }catch(FeedbackError $ex){reply(['error'=>$ex->getMessage()],$ex->status);}
                header('Location: /admin?page=codes',true,303);exit;
            }
            if($action==='poll_create') {
                $options=$_POST['options']??[];
                if(is_array($options))$options=array_values(array_filter($options,fn($v)=>!is_string($v)||trim($v)!==''));
                try {Polls::create($db,$_POST['question']??null,$options,$_POST['duration']??null,$_POST['unit']??null);}
                catch(FeedbackError $ex){reply(['error'=>$ex->getMessage()],$ex->status);}
                $_SESSION['notice']='Poll published. Updated clients refresh within one minute.';
                header('Location: /admin?page=polls',true,303);exit;
            }
            if($action==='poll_close'||$action==='poll_delete') {
                $id=filter_var($_POST['poll_id']??null,FILTER_VALIDATE_INT);
                if(!$id||$id<1)reply(['error'=>'Invalid poll'],400);
                Polls::moderate($db,$action==='poll_close'?'close':'delete',$id);
                $_SESSION['notice']=$action==='poll_close'?'Poll closed. Results retained.':'Poll and votes permanently deleted.';
                header('Location: /admin?page=polls',true,303);exit;
            }
            if($action==='feedback_approve'||$action==='feedback_delete') {
                $id=filter_var($_POST['feedback_id']??null,FILTER_VALIDATE_INT);
                $reply=$_POST['reply']??'';
                if(!$id||!is_string($reply))reply(['error'=>'Invalid feedback action'],400);
                try {Feedback::moderate($db,$action==='feedback_approve'?'approve':'delete',$id,$reply);}
                catch(FeedbackError $ex){reply(['error'=>$ex->getMessage()],$ex->status);}
                $_SESSION['notice']=$action==='feedback_approve'?'Feedback approved. The player will be notified on login.':'Feedback deleted. Any pending approval notice is preserved.';
                header('Location: /admin?page=feedback',true,303);exit;
            }
            if($action==='paynow_secret') {
                require_once dirname(__DIR__).'/PayNowWebhook.php';
                $event=$_POST['event_type']??'';$secret=$_POST['signing_secret']??'';
                if(!is_string($event)||!in_array($event,PayNowWebhook::EVENTS,true)||!is_string($secret)||strlen(trim($secret))<16||strlen($secret)>512)
                    reply(['error'=>'Choose a supported event and enter its signing secret'],400);
                $q=$db->prepare('INSERT INTO paynow_secrets VALUES(?,?) ON CONFLICT(event_type) DO UPDATE SET secret=excluded.secret');$q->execute([$event,trim($secret)]);
                $_SESSION['notice']='Saved PayNow signing secret for '.$event;
                header('Location: /admin?page=payments',true,303);exit;
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
                    header('Location: /admin?page=cosmetics&uuid='.rawurlencode($uuid),true,303);exit;
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
    $dashboard=$logged?AdminDashboard::data($db,$catalog,$page,$_GET):[];
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
require dirname(__DIR__).'/views/admin.php';
