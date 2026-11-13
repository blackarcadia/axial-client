<?php
declare(strict_types=1);
require dirname(__DIR__).'/Feedback.php';require dirname(__DIR__).'/RedeemCodes.php';
if(($argv[1]??'')==='worker'){
    $db=new PDO('sqlite:'.$argv[2],null,null,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);$db->exec('PRAGMA busy_timeout=5000');
    try {RedeemCodes::redeem($db,['code'=>$argv[3],'requestId'=>$argv[4]],$argv[4],$argv[4]);exit(0);}
    catch(FeedbackError $e){exit($e->status===400?2:3);}
}
$file=tempnam(sys_get_temp_dir(),'axial-redeem-race-');
try{
    $db=new PDO('sqlite:'.$file,null,null,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);$db->exec('PRAGMA busy_timeout=5000');$db->exec(file_get_contents(dirname(__DIR__).'/schema.sql'));RedeemCodes::migrate($db);
    $code=RedeemCodes::create($db,['purrr'=>[]],'purrr','Concurrency fixture')['code'];
    $processes=[];
    for($i=1;$i<=4;$i++){
        $pipes=[];$uuid='00000000-0000-4000-8000-'.sprintf('%012d',$i);
        $processes[]=proc_open([PHP_BINARY,__FILE__,'worker',$file,$code,$uuid],[0=>['pipe','r'],1=>['pipe','w'],2=>['pipe','w']],$pipes);
        foreach($pipes as $pipe)fclose($pipe);
    }
    $statuses=array_map(fn($p)=>proc_close($p),$processes);sort($statuses);
    if($statuses!==[0,2,2,2]||(int)$db->query('SELECT COUNT(*) FROM redeem_receipts')->fetchColumn()!==1||(int)$db->query('SELECT uses FROM redeem_codes')->fetchColumn()!==1)throw new RuntimeException('Single-use claim was not atomic: '.json_encode($statuses));
    echo "PASS: four concurrent claims produce exactly one grant and one consumed use\n";
}finally{unset($db);foreach([$file,$file.'-wal',$file.'-shm'] as $f)if(is_file($f))unlink($f);}
