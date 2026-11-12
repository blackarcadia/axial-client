<?php
declare(strict_types=1);
require dirname(__DIR__).'/Feedback.php';
require dirname(__DIR__).'/Polls.php';
if(($argv[1]??'')==='worker') {
    $db=new PDO('sqlite:'.$argv[2],null,null,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);
    $db->exec('PRAGMA busy_timeout=5000');
    for($i=0;!file_exists($argv[2].'.start');$i++){if($i>10000)exit(2);usleep(1000);}
    try{Polls::handle($db,'vote',['pollId'=>(int)$argv[3],'optionId'=>(int)$argv[4]],'same-verified-account');}
    catch(FeedbackError $ex){if($ex->status!==409)throw $ex;}
    exit(0);
}
$dir=sys_get_temp_dir().'/axial-poll-race-'.bin2hex(random_bytes(8));mkdir($dir,0700);$file=$dir.'/test.sqlite';
try {
    $db=new PDO('sqlite:'.$file,null,null,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);Polls::migrate($db);
    $id=Polls::create($db,'Concurrent vote test',['One','Two'],30,'minutes');$db=null;
    $jobs=[];
    foreach([1,2] as $option){$process=proc_open([PHP_BINARY,__FILE__,'worker',$file,(string)$id,(string)$option],[0=>['pipe','r'],1=>['pipe','w'],2=>['pipe','w']],$pipes);if(!is_resource($process))throw new RuntimeException('Cannot start worker');fclose($pipes[0]);$jobs[]=[$process,$pipes];}
    touch($file.'.start');
    foreach($jobs as [$process,$pipes]){stream_get_contents($pipes[1]);$error=stream_get_contents($pipes[2]);fclose($pipes[1]);fclose($pipes[2]);if(proc_close($process)!==0)throw new RuntimeException('Vote worker failed: '.$error);}
    $db=new PDO('sqlite:'.$file);
    if((int)$db->query('SELECT COUNT(*) FROM poll_votes')->fetchColumn()!==1)throw new RuntimeException('Concurrent requests recorded multiple votes');
    $db=null;echo "Concurrent voting: two simultaneous choices, exactly one stored vote.\n";
}finally{foreach(glob($dir.'/*') as $path)unlink($path);rmdir($dir);}
