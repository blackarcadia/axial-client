<?php
declare(strict_types=1);
require_once dirname(__DIR__).'/DeveloperRoles.php';
$db=new PDO('sqlite::memory:',null,null,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);
$db->exec("CREATE TABLE metadata(name TEXT PRIMARY KEY,value INTEGER); INSERT INTO metadata VALUES('revision',1)");
DeveloperRoles::migrate($db);
$uuid='12345678-1234-1234-1234-123456789abc';
function check(bool $ok): void {if(!$ok)throw new RuntimeException('Role test failed');}
check(DeveloperRoles::members($db)===[]);
DeveloperRoles::set($db,strtoupper($uuid),true);
check(DeveloperRoles::members($db)===[$uuid]);
DeveloperRoles::set($db,$uuid,true);
check(count(DeveloperRoles::members($db))===1);
DeveloperRoles::set($db,$uuid,false);
check(DeveloperRoles::members($db)===[]);
check((int)$db->query("SELECT value FROM metadata WHERE name='revision'")->fetchColumn()===4);
try {DeveloperRoles::set($db,'invalid',true);throw new RuntimeException('Accepted invalid UUID');}catch(InvalidArgumentException $expected){}
echo "Developer role tests passed\n";
