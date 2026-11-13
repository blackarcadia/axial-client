<?php
declare(strict_types=1);

/** High-entropy bearer codes. Plaintext is displayed once, never stored in the database. */
final class RedeemCodes {
    private static function query(PDO $db,string $sql,array $args=[]): PDOStatement {$q=$db->prepare($sql);$q->execute($args);return $q;}
    public static function migrate(PDO $db): void {
        $db->exec('CREATE TABLE IF NOT EXISTS redeem_codes(id INTEGER PRIMARY KEY AUTOINCREMENT,code_hash TEXT NOT NULL UNIQUE,cosmetic TEXT NOT NULL,label TEXT NOT NULL,maximum INTEGER NOT NULL,uses INTEGER NOT NULL DEFAULT 0,active INTEGER NOT NULL DEFAULT 1,created_at INTEGER NOT NULL,expires_at INTEGER)');
        $db->exec('CREATE TABLE IF NOT EXISTS redeem_receipts(code_id INTEGER NOT NULL,uuid TEXT NOT NULL,cosmetic TEXT NOT NULL,request_id TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(code_id,uuid),UNIQUE(uuid,request_id))');
        $db->exec('CREATE TABLE IF NOT EXISTS redeem_limits(peer TEXT PRIMARY KEY,started_at INTEGER NOT NULL,attempts INTEGER NOT NULL)');
        if((int)self::query($db,"SELECT value FROM metadata WHERE name='redeem_schema'")->fetchColumn()>=1)return;
        $db->beginTransaction();
        try {
            $db->exec("UPDATE metadata SET value=value WHERE name='revision'");
            if((int)self::query($db,"SELECT value FROM metadata WHERE name='redeem_schema'")->fetchColumn()<1){
                $db->exec('DROP VIEW IF EXISTS effective_grants');
                $db->exec('CREATE VIEW effective_grants AS SELECT uuid,cape FROM grants UNION SELECT uuid,cosmetic AS cape FROM redeem_receipts UNION SELECT p.uuid,p.cape FROM paynow_purchases p JOIN paynow_orders o USING(order_id) WHERE NOT EXISTS (SELECT 1 FROM paynow_chargebacks c WHERE c.order_id=p.order_id) AND NOT EXISTS (SELECT 1 FROM paynow_refunds r WHERE r.order_id=p.order_id GROUP BY r.order_id HAVING SUM(r.amount)>=o.total)');
                $db->exec("INSERT OR REPLACE INTO metadata VALUES('redeem_schema',1)");
            }
            $db->commit();
        }catch(Throwable $e){$db->rollBack();throw $e;}
    }
    public static function normalize(string $code): string {return strtoupper(preg_replace('/[\s-]+/','',$code));}
    public static function create(PDO $db,array $catalog,string $cosmetic,string $label,int $maximum=1,?int $expires=null,?string $code=null): array {
        if(!isset($catalog[$cosmetic])||strlen($label)>80||$maximum<0||$maximum>100000||($expires!==null&&$expires<=time()))throw new FeedbackError('Choose a cosmetic and valid code limits.',400);
        $code??='AXIAL-'.implode('-',str_split(strtoupper(bin2hex(random_bytes(16))),8));
        $normalized=self::normalize($code);
        if(!preg_match('/\AAXIAL[A-F0-9]{32}\z/',$normalized))throw new FeedbackError('Invalid unlock code format.',400);
        self::query($db,'INSERT INTO redeem_codes(code_hash,cosmetic,label,maximum,created_at,expires_at) VALUES(?,?,?,?,?,?)',[hash('sha256',$normalized),$cosmetic,trim($label),$maximum,time(),$expires]);
        return ['id'=>(int)$db->lastInsertId(),'code'=>$code];
    }
    public static function disable(PDO $db,int $id): void {self::query($db,'UPDATE redeem_codes SET active=0 WHERE id=?',[$id]);}
    public static function listing(PDO $db): array {return $db->query('SELECT id,cosmetic,label,maximum,uses,active,created_at,expires_at FROM redeem_codes ORDER BY id DESC LIMIT 200')->fetchAll(PDO::FETCH_ASSOC);}
    private static function limit(PDO $db,string $key,int $maximum): void {
        self::query($db,'INSERT INTO redeem_limits VALUES(?,?,1) ON CONFLICT(peer) DO UPDATE SET attempts=CASE WHEN started_at<? THEN 1 ELSE attempts+1 END,started_at=CASE WHEN started_at<? THEN excluded.started_at ELSE started_at END',[$key,time(),time()-600,time()-600]);
        $r=self::query($db,'SELECT * FROM redeem_limits WHERE peer=?',[$key])->fetch(PDO::FETCH_ASSOC);
        if((int)$r['attempts']>$maximum)throw new FeedbackError('Too many code attempts. Please try again in a few minutes.',429,max(1,(int)$r['started_at']+600-time()));
    }
    public static function redeem(PDO $db,array $body,string $uuid,string $peer): array {
        self::migrate($db);
        self::query($db,'DELETE FROM redeem_limits WHERE started_at<?',[time()-86400]);
        self::limit($db,'account:'.$uuid,10);self::limit($db,'network:'.$peer,50);
        $code=$body['code']??null;$request=$body['requestId']??null;
        if(!is_string($code)||strlen($code)>96||!is_string($request)||!preg_match('/\A[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}\z/',$request))throw new FeedbackError('Enter a valid unlock code.',400);
        $hash=hash('sha256',self::normalize($code));
        $db->beginTransaction();
        try {
            // The first write serializes claims before checking usage/ownership, including across PHP workers.
            $db->exec('UPDATE redeem_codes SET uses=uses WHERE id=-1');
            $old=self::query($db,'SELECT cosmetic FROM redeem_receipts WHERE uuid=? AND request_id=?',[$uuid,$request])->fetchColumn();
            if($old){$result=self::result($db,$uuid,$old);$db->commit();return $result;}
            $row=self::query($db,'SELECT * FROM redeem_codes WHERE code_hash=?',[$hash])->fetch(PDO::FETCH_ASSOC);
            if(!$row||!(int)$row['active']||($row['expires_at']!==null&&(int)$row['expires_at']<=time())||((int)$row['maximum']>0&&(int)$row['uses']>=(int)$row['maximum']))throw new FeedbackError('This code is invalid, expired, or already used.',400);
            if(self::query($db,'SELECT 1 FROM effective_grants WHERE uuid=? AND cape=?',[$uuid,$row['cosmetic']])->fetchColumn())throw new FeedbackError('You already own this cosmetic. The code has not been used.',409);
            self::query($db,'INSERT INTO players(uuid,updated_at) VALUES(?,?) ON CONFLICT(uuid) DO UPDATE SET updated_at=excluded.updated_at',[$uuid,time()]);
            self::query($db,'INSERT INTO redeem_receipts VALUES(?,?,?,?,?)',[(int)$row['id'],$uuid,$row['cosmetic'],$request,time()]);
            self::query($db,'UPDATE redeem_codes SET uses=uses+1 WHERE id=?',[$row['id']]);
            self::query($db,'INSERT INTO audit(created_at,uuid,action) VALUES(?,?,?)',[time(),$uuid,'Redeemed cosmetic '.$row['cosmetic'].' (code #'.$row['id'].')']);
            $db->exec("UPDATE metadata SET value=value+1 WHERE name='revision'");
            $result=self::result($db,$uuid,$row['cosmetic']);$db->commit();return $result;
        }catch(Throwable $e){$db->rollBack();throw $e;}
    }
    private static function result(PDO $db,string $uuid,string $cosmetic): array {return ['uuid'=>$uuid,'revision'=>(int)self::query($db,"SELECT value FROM metadata WHERE name='revision'")->fetchColumn(),'cosmetic'=>$cosmetic,'cosmetics'=>self::query($db,'SELECT cape FROM effective_grants WHERE uuid=? ORDER BY cape',[$uuid])->fetchAll(PDO::FETCH_COLUMN)];}
}
