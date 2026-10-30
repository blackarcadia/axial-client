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
    $path=parse_url($_SERVER['REQUEST_URI'],PHP_URL_PATH);
    $method=$_SERVER['REQUEST_METHOD'];
    if($path==='/health') {if($method!=='GET')reply(['error'=>'Method not allowed'],405);$db->query('SELECT value FROM metadata LIMIT 1');reply(['status'=>'ok']);}
    if($path==='/v1/cosmetics') {
        if($method!=='GET')reply(['error'=>'Method not allowed'],405);
        $db->beginTransaction();
        $revision=(int)$db->query("SELECT value FROM metadata WHERE name='revision'")->fetchColumn();
        $players=[];
        foreach($db->query('SELECT uuid FROM players ORDER BY uuid') as $row)$players[$row['uuid']]=[];
        foreach($db->query('SELECT uuid,cape FROM grants ORDER BY uuid,cape') as $row)$players[$row['uuid']][]=$row['cape'];
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
<div class="catalog"><?php foreach($catalog as $id=>$cape):?><label class="cosmetic"><img src="/assets/<?=h($cape['image'])?>" alt="<?=h($cape['name'])?> cape"><span><input type="checkbox" name="capes[]" value="<?=h($id)?>" <?=in_array($id,$owned,true)?'checked':''?>> <?=h($cape['name'])?></span></label><?php endforeach?></div>
<p class="hint">Checked capes appear in this player's wardrobe. Uncheck a cape to revoke access. Unchecking all capes removes wardrobe access. Updated clients refresh within one minute.</p><button class="primary">Save cosmetic access</button></form>
<section><h2>Assigned players</h2><div class="players"><?php foreach($db->query('SELECT p.uuid,COUNT(g.cape) AS total FROM players p LEFT JOIN grants g ON g.uuid=p.uuid GROUP BY p.uuid ORDER BY p.updated_at DESC LIMIT 100') as $row):?><a href="/admin?uuid=<?=h($row['uuid'])?>"><code><?=h($row['uuid'])?></code><span><?=h((string)$row['total'])?> capes →</span></a><?php endforeach?></div></section>
<?php endif?></main></body></html>
