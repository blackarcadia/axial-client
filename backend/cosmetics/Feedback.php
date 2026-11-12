<?php
declare(strict_types=1);

final class FeedbackError extends RuntimeException {
    public function __construct(string $message, public readonly int $status) { parent::__construct($message); }
}

/** Private inbox: verified Minecraft sessions, bounded submissions and separate delivery receipts. */
final class Feedback {
    public static function migrate(PDO $db): void {
        $db->exec(file_get_contents(__DIR__.'/feedback.sql'));
    }
    private static function query(PDO $db,string $sql,array $args=[]): PDOStatement {
        $q=$db->prepare($sql);$q->execute($args);return $q;
    }
    private static function limit(PDO $db,string $key,int $maximum,int $window): void {
        self::query($db,'INSERT INTO feedback_limits VALUES(?,?,1) ON CONFLICT(peer) DO UPDATE SET attempts=CASE WHEN started_at<? THEN 1 ELSE attempts+1 END,started_at=CASE WHEN started_at<? THEN excluded.started_at ELSE started_at END',[$key,time(),time()-$window,time()-$window]);
        if((int)self::query($db,'SELECT attempts FROM feedback_limits WHERE peer=?',[$key])->fetchColumn()>$maximum)
            throw new FeedbackError('Too many requests. Please try again later.',429);
    }
    public static function handle(PDO $db,string $action,array $body,string $token,string $peer,?callable $verify=null): array {
        self::query($db,'DELETE FROM feedback_challenges WHERE expires_at<?',[time()]);
        self::query($db,'DELETE FROM feedback_sessions WHERE expires_at<?',[time()]);
        self::query($db,'DELETE FROM feedback_limits WHERE started_at<?',[time()-86400]);
        if($action==='challenge') {
            self::limit($db,'challenge:'.$peer,20,600);
            $challenge=bin2hex(random_bytes(20));
            self::query($db,'INSERT INTO feedback_challenges VALUES(?,?)',[$challenge,time()+120]);
            return ['challenge'=>$challenge];
        }
        if($action==='authenticate') {
            self::limit($db,'auth:'.$peer,30,600);
            $challenge=$body['challenge']??'';$name=$body['username']??'';
            if(!is_string($challenge)||!preg_match('/\A[a-f0-9]{40}\z/',$challenge)||!is_string($name)||!preg_match('/\A[A-Za-z0-9_]{1,16}\z/',$name))throw new FeedbackError('Invalid account verification.',400);
            if(self::query($db,'DELETE FROM feedback_challenges WHERE challenge=? AND expires_at>=?',[$challenge,time()])->rowCount()!==1)throw new FeedbackError('Verification expired. Please retry.',401);
            $profile=$verify?$verify($name,$challenge):self::verify($name,$challenge);
            $uuid=$profile['id']??'';$verifiedName=$profile['name']??'';
            if(!is_string($uuid)||!preg_match('/\A[a-fA-F0-9]{32}\z/',$uuid)||!is_string($verifiedName)||strcasecmp($verifiedName,$name)!==0)throw new FeedbackError('Minecraft account verification failed.',401);
            $uuid=strtolower(preg_replace('/(.{8})(.{4})(.{4})(.{4})(.{12})/','$1-$2-$3-$4-$5',$uuid));
            $token=bin2hex(random_bytes(32));
            self::query($db,'INSERT INTO feedback_sessions VALUES(?,?,?,?,?)',[hash('sha256',$token),$uuid,$verifiedName,time(),time()+3600]);
            return ['token'=>$token];
        }
        $session=self::query($db,'SELECT uuid,username FROM feedback_sessions WHERE token=? AND expires_at>=?',[hash('sha256',$token),time()])->fetch(PDO::FETCH_ASSOC);
        if(!$session)throw new FeedbackError('Please verify your Minecraft account again.',401);
        $uuid=$session['uuid'];
        if($action==='polls'||$action==='vote')return Polls::handle($db,$action,$body,$uuid);
        if($action==='submit') {
            $message=$body['message']??'';$request=$body['requestId']??'';
            if(!is_string($message)||!preg_match('//u',$message)||strlen(trim($message))<10||strlen($message)>8000||!is_string($request)||!preg_match('/\A[a-f0-9-]{36}\z/',$request))throw new FeedbackError('Enter 10–2000 characters of feedback.',400);
            if(preg_match_all('/./us',$message)>2000)throw new FeedbackError('Feedback is limited to 2000 characters.',400);
            $old=self::query($db,'SELECT id FROM feedback WHERE uuid=? AND request_id=?',[$uuid,$request])->fetchColumn();
            if($old)return ['id'=>(int)$old];
            self::limit($db,'submit:'.$uuid,5,86400);
            self::limit($db,'submit-peer:'.$peer,50,86400);
            self::query($db,'INSERT INTO feedback(uuid,username,message,request_id,created_at,status) VALUES(?,?,?,?,?,?)',[$uuid,$session['username'],trim($message),$request,time(),'pending']);
            return ['id'=>(int)$db->lastInsertId()];
        }
        if($action==='notifications')return ['notifications'=>self::query($db,'SELECT id,feedback_id,reply FROM feedback_notifications WHERE uuid=? ORDER BY id LIMIT 20',[$uuid])->fetchAll(PDO::FETCH_ASSOC)];
        if($action==='ack') {
            $ids=$body['ids']??[];
            if(!is_array($ids)||count($ids)>20)throw new FeedbackError('Invalid receipt.',400);
            foreach($ids as $id)if(is_int($id)&&$id>0)self::query($db,'DELETE FROM feedback_notifications WHERE id=? AND uuid=?',[$id,$uuid]);
            return ['ok'=>true];
        }
        throw new FeedbackError('Not found',404);
    }
    private static function verify(string $name,string $challenge): array {
        $context=stream_context_create(['http'=>['timeout'=>6,'ignore_errors'=>true,'follow_location'=>0]]);
        $raw=@file_get_contents('https://sessionserver.mojang.com/session/minecraft/hasJoined?username='.rawurlencode($name).'&serverId='.$challenge,false,$context,0,16384);
        if($raw===false)throw new FeedbackError('Minecraft verification is temporarily unavailable.',503);
        return json_decode($raw,true)??[];
    }
    public static function moderate(PDO $db,string $action,int $id,string $reply): void {
        if(strlen($reply)>2000||!preg_match('//u',$reply))throw new FeedbackError('Reply is too long.',400);
        $db->beginTransaction();
        try {
            if($action==='approve') {
                self::query($db,"UPDATE feedback SET status='approved' WHERE id=? AND status='pending'",[$id]);
                if(self::query($db,'SELECT changes()')->fetchColumn()>0) {
                    $uuid=self::query($db,'SELECT uuid FROM feedback WHERE id=?',[$id])->fetchColumn();
                    self::query($db,'INSERT INTO feedback_notifications(uuid,feedback_id,reply) VALUES(?,?,?)',[$uuid,$id,trim($reply)]);
                }
            } elseif($action==='delete')self::query($db,'DELETE FROM feedback WHERE id=?',[$id]);
            else throw new FeedbackError('Invalid action.',400);
            $db->commit();
        } catch(Throwable $ex) {$db->rollBack();throw $ex;}
    }
}
