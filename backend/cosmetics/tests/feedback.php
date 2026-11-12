<?php
declare(strict_types=1);
require dirname(__DIR__).'/Feedback.php';
$db=new PDO('sqlite::memory:',null,null,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);
Feedback::migrate($db);Feedback::migrate($db);
$checks=0;
function check(bool $ok,string $why): void {global $checks;$checks++;if(!$ok)throw new RuntimeException($why);}
function rejected(callable $fn,int $status): void {try{$fn();}catch(FeedbackError $e){check($e->status===$status,'Correct rejection status');return;}throw new RuntimeException('Expected rejection');}
function login(string $name,string $uuid,string $peer='test'): string {
    global $db;
    $challenge=Feedback::handle($db,'challenge',[],'',$peer)['challenge'];
    $result=Feedback::handle($db,'authenticate',['username'=>$name,'challenge'=>$challenge],'',$peer,fn()=>['id'=>$uuid,'name'=>$name]);
    rejected(fn()=>Feedback::handle($db,'authenticate',['username'=>$name,'challenge'=>$challenge],'',$peer,fn()=>['id'=>$uuid,'name'=>$name]),401);
    return $result['token'];
}
$token=login('Tester','00000000000040008000000000000123');
$other=login('Other','00000000000040008000000000000456','other');
rejected(fn()=>Feedback::handle($db,'submit',['message'=>'Valid feedback message'],'','test'),401);
rejected(fn()=>Feedback::handle($db,'submit',['message'=>'short','requestId'=>str_repeat('a',36)],$token,'test'),400);
rejected(fn()=>Feedback::handle($db,'submit',['message'=>str_repeat('a',2001),'requestId'=>str_repeat('a',36)],$token,'test'),400);
$body=['message'=>"A useful suggestion\n<script>alert(1)</script>",'requestId'=>'00000000-0000-4000-8000-000000000001'];
$id=Feedback::handle($db,'submit',$body,$token,'test')['id'];
check(Feedback::handle($db,'submit',$body,$token,'test')['id']===$id,'Retries do not create duplicates');
check($db->query('SELECT username FROM feedback')->fetchColumn()==='Tester','Identity comes from verified session');
Feedback::moderate($db,'approve',$id,'We are adding this.');
Feedback::moderate($db,'approve',$id,'Repeated approval');
$notes=Feedback::handle($db,'notifications',[],$token,'test')['notifications'];
check(count($notes)===1&&$notes[0]['reply']==='We are adding this.','Approval is idempotent and preserves reply');
check(Feedback::handle($db,'notifications',[],$other,'other')['notifications']===[],'Other players cannot read notices');
Feedback::handle($db,'ack',['ids'=>[(int)$notes[0]['id']]],$other,'other');
check(count(Feedback::handle($db,'notifications',[],$token,'test')['notifications'])===1,'Other players cannot acknowledge notices');
Feedback::moderate($db,'delete',$id,'');
check((int)$db->query('SELECT COUNT(*) FROM feedback')->fetchColumn()===0,'Deletion removes feedback text');
check(count(Feedback::handle($db,'notifications',[],$token,'test')['notifications'])===1,'Deletion preserves pending approval');
Feedback::handle($db,'ack',['ids'=>[(int)$notes[0]['id']]],$token,'test');
check(Feedback::handle($db,'notifications',[],$token,'test')['notifications']===[],'Acknowledged notices are removed');
for($i=2;$i<=5;$i++)Feedback::handle($db,'submit',['message'=>'Another useful suggestion','requestId'=>sprintf('00000000-0000-4000-8000-%012d',$i)],$token,'test');
rejected(fn()=>Feedback::handle($db,'submit',['message'=>'One too many suggestions','requestId'=>'00000000-0000-4000-8000-000000000006'],$token,'test'),429);
$db->exec('UPDATE feedback_sessions SET expires_at=0');
rejected(fn()=>Feedback::handle($db,'notifications',[],$token,'test'),401);
$challenge=Feedback::handle($db,'challenge',[],'','bad-profile')['challenge'];
rejected(fn()=>Feedback::handle($db,'authenticate',['username'=>'Tester','challenge'=>$challenge],'','bad-profile',fn()=>['id'=>'00000000000040008000000000000123','name'=>'SomeoneElse']),401);
for($i=0;$i<20;$i++)Feedback::handle($db,'challenge',[],'','flood');
rejected(fn()=>Feedback::handle($db,'challenge',[],'','flood'),429);
echo "Feedback: $checks checks passed.\n";
