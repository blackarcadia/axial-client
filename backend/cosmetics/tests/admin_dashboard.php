<?php
declare(strict_types=1);
set_error_handler(function(int $severity,string $message,string $file,int $line): never {throw new ErrorException($message,0,$severity,$file,$line);});
function h(string $value): string{return htmlspecialchars($value,ENT_QUOTES|ENT_SUBSTITUTE,'UTF-8');}
require dirname(__DIR__).'/AdminDashboard.php';
require dirname(__DIR__).'/Feedback.php';require dirname(__DIR__).'/Polls.php';
$db=new PDO('sqlite::memory:',null,null,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);
$db->exec(file_get_contents(dirname(__DIR__).'/schema.sql'));Feedback::migrate($db);Polls::migrate($db);
$checks=0;
function check(bool $value,string $message): void{global $checks;$checks++;if(!$value)throw new RuntimeException($message);}
$catalog=json_decode(file_get_contents(dirname(__DIR__).'/catalog.json'),true,32,JSON_THROW_ON_ERROR);
for($i=1;$i<=61;$i++){$q=$db->prepare('INSERT INTO players VALUES(?,?)');$q->execute(['00000000-0000-4000-8000-'.sprintf('%012d',$i),time()-$i*60]);}
$uuid='00000000-0000-4000-8000-000000000001';$db->prepare('INSERT INTO grants VALUES(?,?)')->execute([$uuid,'staff']);
for($i=1;$i<=43;$i++){$q=$db->prepare('INSERT INTO feedback(uuid,username,message,request_id,created_at,status,category,kind) VALUES(?,?,?,?,?,?,?,?)');$q->execute([$uuid,'TestPlayer','<script>alert(1)</script> A test idea with a long paragraph and a link-shaped string.','test-'.$i,time()-$i*60,$i%3?'pending':'approved',$i%2?'prisons':'client',$i%2?'suggestion':'bug']);}
$open=Polls::create($db,'What should we add next?',['More mining features','More cosmetics','Better quality of life'],24,'hours');
$ended=Polls::create($db,'Which event should return?',['Prison Riot','Meteor shower'],30,'minutes');Polls::moderate($db,'close',$ended);
for($i=0;$i<8;$i++)$db->prepare('INSERT INTO poll_votes VALUES(?,?,?,?)')->execute([$open,'fixture-'.$i,$i<5?1:2,time()]);
check(AdminDashboard::page('../../private')==='overview','Unknown pages cannot select files');
check(AdminDashboard::page(['feedback'])==='overview','Malformed page input is rejected');
check(AdminDashboard::url('cosmetics',['uuid'=>$uuid])==='/admin?page=cosmetics&uuid='.$uuid,'URLs retain page and UUID');
$players=AdminDashboard::data($db,$catalog,'players',[]);check(count($players['players'])===25&&$players['pages']===3,'Players paginate');
$players=AdminDashboard::data($db,$catalog,'players',['p'=>'999999']);check(count($players['players'])===11&&$players['number']===3,'Large page numbers clamp to the last page');
$players=AdminDashboard::data($db,$catalog,'players',['q'=>'%\' OR 1=1--']);check($players['total']===0,'Search values stay bound parameters');
$feedback=AdminDashboard::data($db,$catalog,'feedback',['category'=>'prisons','status'=>'pending']);check($feedback['total']===15,'Feedback category and status filters compose');
$polls=AdminDashboard::data($db,$catalog,'polls',['state'=>'open']);check(count($polls['polls'])===1&&$polls['polls'][0]['total']===8,'Open filter and result counts');
$polls=AdminDashboard::data($db,$catalog,'polls',['state'=>'ended']);check(count($polls['polls'])===1,'Ended polls retain their page');
$logged=true;$owned=['staff'];$paid=[];$secretEvents=['ON_ORDER_COMPLETED'];$recentEvents=[];$partialRefunds=0;$error='';$notice='';$_SESSION=['csrf'=>'fixture-not-a-real-session'];
$out=$argv[1]??null;if($out&&!is_dir($out))mkdir($out,0700,true);
foreach(AdminDashboard::PAGES as $page=>$meta){
    $dashboard=AdminDashboard::data($db,$catalog,$page,[]);
    ob_start();require dirname(__DIR__).'/views/admin.php';$html=ob_get_clean();
    check(str_contains($html,'<h1>'.h($meta[0]).'</h1>'),'Page heading: '.$page);
    check(substr_count($html,'aria-current="page"')===1,'Only current nav item is active: '.$page);
    check(str_contains($html,'name="csrf"'),'CSRF tokens remain in forms: '.$page);
    check(!str_contains($html,'<script>'),'No script or unescaped feedback: '.$page);
    if($page==='feedback')check(str_contains($html,'&lt;script&gt;alert(1)&lt;/script&gt;'),'Feedback is escaped');
    if($page==='payments')check(!str_contains($html,'value="test-secret"'),'No signing secret output');
    if($out)file_put_contents($out.'/'.$page.'.html',$html);
}
$logged=false;$page='overview';ob_start();require dirname(__DIR__).'/views/admin.php';$html=ob_get_clean();
check(str_contains($html,'Admin password')&&!str_contains($html,$uuid),'Signed-out view never exposes player data');
if($out)file_put_contents($out.'/login.html',$html);
echo "Admin dashboard: $checks checks passed.\n";
