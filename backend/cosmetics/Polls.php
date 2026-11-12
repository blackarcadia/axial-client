<?php
declare(strict_types=1);

/** Votes are tied to verified Minecraft UUIDs, with uniqueness enforced by SQLite. */
final class Polls {
    public static function migrate(PDO $db): void {$db->exec(file_get_contents(__DIR__.'/polls.sql'));}
    private static function query(PDO $db,string $sql,array $args=[]): PDOStatement {$q=$db->prepare($sql);$q->execute($args);return $q;}
    private static function text(mixed $value,int $max): string {
        if(!is_string($value)||!preg_match('//u',$value)||trim($value)===''||preg_match_all('/./us',$value)>$max)throw new FeedbackError('Enter a question and 2–6 distinct options within the length limits.',400);
        return trim($value);
    }
    public static function create(PDO $db,mixed $question,mixed $options,mixed $duration,mixed $unit): int {
        $question=self::text($question,160);
        if(!is_array($options)||count($options)<2||count($options)>6)throw new FeedbackError('Provide 2–6 voting options.',400);
        $options=array_map(fn($v)=>self::text($v,80),array_values($options));
        if(count(array_unique(array_map('strtolower',$options)))!==count($options))throw new FeedbackError('Each option must be different.',400);
        $multipliers=['minutes'=>60,'hours'=>3600,'days'=>86400];
        $duration=filter_var($duration,FILTER_VALIDATE_INT);
        if(!is_string($unit)||!isset($multipliers[$unit])||$duration===false||$duration<1||$duration>43200)throw new FeedbackError('Choose a valid poll duration.',400);
        $seconds=$duration*$multipliers[$unit];
        if($seconds<60||$seconds>30*86400)throw new FeedbackError('Poll duration must be between 1 minute and 30 days.',400);
        $db->beginTransaction();
        try {
            // Take the writer lock before checking the active-poll limit.
            $db->exec('UPDATE polls SET closed=closed WHERE id=-1');
            if((int)self::query($db,'SELECT COUNT(*) FROM polls WHERE closed=0 AND closes_at>?',[time()])->fetchColumn()>=10)throw new FeedbackError('Close an active poll before creating another (maximum 10).',400);
            self::query($db,'INSERT INTO polls(question,created_at,closes_at) VALUES(?,?,?)',[$question,time(),time()+$seconds]);
            $id=(int)$db->lastInsertId();
            foreach($options as $index=>$label)self::query($db,'INSERT INTO poll_options VALUES(?,?,?)',[$id,$index+1,$label]);
            $db->commit();return $id;
        }catch(Throwable $ex){$db->rollBack();throw $ex;}
    }
    public static function handle(PDO $db,string $action,array $body,string $uuid): array {
        $now=time();
        if($action==='polls') {
            $polls=self::query($db,'SELECT p.id,p.question,p.closes_at,(SELECT option_id FROM poll_votes v WHERE v.poll_id=p.id AND v.uuid=?) AS voted FROM polls p WHERE p.closed=0 AND p.closes_at>? ORDER BY p.closes_at,p.id LIMIT 10',[$uuid,$now])->fetchAll(PDO::FETCH_ASSOC);
            foreach($polls as &$poll)$poll['options']=self::query($db,'SELECT option_id AS id,label FROM poll_options WHERE poll_id=? ORDER BY option_id',[$poll['id']])->fetchAll(PDO::FETCH_ASSOC);
            return ['serverTime'=>$now,'polls'=>$polls];
        }
        if($action!=='vote')throw new FeedbackError('Not found',404);
        $id=$body['pollId']??null;$option=$body['optionId']??null;
        if(!is_int($id)||$id<=0||!is_int($option)||$option<1||$option>6)throw new FeedbackError('Choose a valid poll option.',400);
        // A single statement checks closure/option membership and inserts under SQLite's writer lock.
        self::query($db,'INSERT INTO poll_votes(poll_id,uuid,option_id,created_at) SELECT p.id,?,o.option_id,? FROM polls p JOIN poll_options o ON o.poll_id=p.id WHERE p.id=? AND o.option_id=? AND p.closed=0 AND p.closes_at>? ON CONFLICT(poll_id,uuid) DO NOTHING',[$uuid,$now,$id,$option,$now]);
        $voted=self::query($db,'SELECT option_id FROM poll_votes WHERE poll_id=? AND uuid=?',[$id,$uuid])->fetchColumn();
        if($voted!==false) {
            if((int)$voted!==$option)throw new FeedbackError('You have already voted in this poll.',409);
            return ['optionId'=>(int)$voted];
        }
        $poll=self::query($db,'SELECT closes_at,closed FROM polls WHERE id=?',[$id])->fetch(PDO::FETCH_ASSOC);
        if(!$poll)throw new FeedbackError('This poll no longer exists.',404);
        if((int)$poll['closed']!==0||(int)$poll['closes_at']<=$now)throw new FeedbackError('This poll has ended.',410);
        throw new FeedbackError('Choose a valid poll option.',400);
    }
    public static function moderate(PDO $db,string $action,int $id): void {
        if($action==='close'){self::query($db,'UPDATE polls SET closed=1 WHERE id=?',[$id]);return;}
        if($action!=='delete')throw new FeedbackError('Invalid poll action.',400);
        $db->beginTransaction();
        try {
            self::query($db,'DELETE FROM polls WHERE id=?',[$id]);
            self::query($db,'DELETE FROM poll_votes WHERE poll_id=?',[$id]);
            self::query($db,'DELETE FROM poll_options WHERE poll_id=?',[$id]);
            $db->commit();
        }catch(Throwable $ex){$db->rollBack();throw $ex;}
    }
}
