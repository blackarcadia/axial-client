<?php
declare(strict_types=1);
if(PHP_SAPI!=='cli'){http_response_code(404);exit;}
require_once __DIR__.'/DeveloperRoles.php';
if(count($argv)!==3||!in_array($argv[1],['grant','revoke'],true)){fwrite(STDERR,"Usage: php developer.php grant|revoke MINECRAFT-UUID\n");exit(1);}
$config=require '/etc/axial-cosmetics/config.php';
$db=new PDO('sqlite:'.$config['database'],null,null,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);
$db->exec('PRAGMA busy_timeout=5000');
DeveloperRoles::migrate($db);
DeveloperRoles::set($db,$argv[2],$argv[1]==='grant');
echo "Developer role updated. Clients refresh within 60 seconds.\n";
