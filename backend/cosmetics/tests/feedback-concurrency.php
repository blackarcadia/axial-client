<?php
declare(strict_types=1);
require dirname(__DIR__).'/Feedback.php';
if(($argv[1]??'')==='worker'){
    $db=new PDO('sqlite:'.$argv[2],null,null,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);$db->exec('PRAGMA busy_timeout=5000');
    for($i=0;!file_exists($argv[2].'.start');$i++){if($i>10000)exit(2);usleep(1000);}
    Feedback::handle($db,'submit',['message'=>'Concurrent feedback retry','requestId'=>'00000000-0000-4000-8000-000000000001'],'fixture-token','fixture-peer');exit(0);
}
$dir=sys_get_temp_dir().'/axial-feedback-race-'.bin2hex(random_bytes(8));mkdir($dir,0700);$file=$dir.'/test.sqlite';
try{
    $db=new PDO('sqlite:'.$file,null,null,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);Feedback::migrate($db);
    $db->prepare('INSERT INTO feedback_sessions VALUES(?,?,?,?,?)')->execute([hash('sha256','fixture-token'),'fixture-player','Fixture',time(),time()+3600]);$db=null;
    $jobs=[];for($i=0;$i<2;$i++){
        $process=proc_open([PHP_BINARY,__FILE__,'worker',$file],[0=>['pipe','r'],1=>['pipe','w'],2=>['pipe','w']],$pipes);
        if(!is_resource($process))throw new RuntimeException('Cannot start worker');fclose($pipes[0]);$jobs[]=[$process,$pipes];
    }
    touch($file.'.start');
    foreach($jobs as [$process,$pipes]){stream_get_contents($pipes[1]);$error=stream_get_contents($pipes[2]);fclose($pipes[1]);fclose($pipes[2]);if(proc_close($process)!==0)throw new RuntimeException('Feedback worker failed: '.$error);}
    $db=new PDO('sqlite:'.$file);
    if((int)$db->query('SELECT COUNT(*) FROM feedback')->fetchColumn()!==1)throw new RuntimeException('Duplicate submission');
    if((int)$db->query("SELECT attempts FROM feedback_limits WHERE peer='submit:fixture-player'")->fetchColumn()!==1)throw new RuntimeException('Duplicate quota charge');
    $db=null;echo "Concurrent feedback retries: one submission and one quota charge.\n";
}finally{foreach(glob($dir.'/*') as $path)unlink($path);rmdir($dir);}
