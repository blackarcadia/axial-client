<?php
declare(strict_types=1);
$root=__DIR__;
$settingsPath='/etc/axial-cosmetics/config.php';
if(!is_file($settingsPath)) {
    $bootstrap=json_decode(file_get_contents('/tmp/axial-cosmetics-bootstrap.json'),true,8,JSON_THROW_ON_ERROR);
    $settings=['database'=>'/var/lib/axial-cosmetics/cosmetics.sqlite','admin_hash'=>password_hash($bootstrap['admin_password'],PASSWORD_DEFAULT),'rate_secret'=>bin2hex(random_bytes(32))];
    file_put_contents($settingsPath,"<?php\nreturn ".var_export($settings,true).";\n");chmod($settingsPath,0640);chgrp($settingsPath,'www-data');
}
$config=require $settingsPath;
$fresh=!is_file($config['database']);
$db=new PDO('sqlite:'.$config['database'],null,null,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);
$db->exec(file_get_contents($root.'/schema.sql'));
if($fresh) {
    $seed=json_decode(file_get_contents($root.'/seed.json'),true,32,JSON_THROW_ON_ERROR);
    $db->beginTransaction();
    $player=$db->prepare('INSERT INTO players(uuid,updated_at) VALUES(?,?)');
    $grant=$db->prepare('INSERT INTO grants(uuid,cape) VALUES(?,?)');
    foreach($seed as $uuid=>$capes){$player->execute([$uuid,time()]);foreach($capes as $cape)$grant->execute([$uuid,$cape]);}
    $db->commit();
}
echo "Cosmetics database initialized; existing grants preserved.\n";
