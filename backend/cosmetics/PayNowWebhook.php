<?php
declare(strict_types=1);

final class WebhookError extends RuntimeException {
    public function __construct(public readonly int $status,string $message){parent::__construct($message);}
}

/** Signed PayNow JSON notifications; no API key is exposed to a client/browser. */
final class PayNowWebhook {
    public const EVENTS=['ON_ORDER_COMPLETED','ON_REFUND','ON_CHARGEBACK'];
    public static function handle(PDO $db,array $settings,string $raw,string $timestamp,string $signature,?int $now=null): array {
        $now??=time();
        if(strlen($raw)>1048576)throw new WebhookError(413,'Payload too large');
        try{$event=json_decode($raw,true,64,JSON_THROW_ON_ERROR|JSON_BIGINT_AS_STRING);}
        catch(JsonException){throw new WebhookError(400,'Invalid JSON');}
        if(!is_array($event))throw new WebhookError(400,'Invalid event');
        $type=$event['event_type']??null;
        if(!is_string($type)||!in_array($type,self::EVENTS,true))throw new WebhookError(400,'Unsupported event');
        $q=$db->prepare('SELECT secret FROM paynow_secrets WHERE event_type=?');$q->execute([$type]);$secret=$q->fetchColumn();
        if(!is_string($secret)||$secret==='')throw new WebhookError(503,'Webhook signing secret is not configured');
        if(!preg_match('/\A[0-9]{13}\z/',$timestamp)||abs($now*1000-(int)$timestamp)>300000)
            throw new WebhookError(401,'Invalid or expired timestamp');
        $decoded=base64_decode($signature,true);
        if($decoded===false||!hash_equals(hash_hmac('sha256',$timestamp.'.'.$raw,$secret,true),$decoded))
            throw new WebhookError(401,'Invalid signature');
        $id=self::id($event['event_id']??null);
        $body=$event['body']??null;
        if(!is_array($body)||($body['store_id']??null)!==$settings['store_id'])throw new WebhookError(400,'Wrong store');
        $db->beginTransaction();
        try {
            // First write serializes concurrent deliveries. Everything commits together or is retried.
            $q=$db->prepare('INSERT OR IGNORE INTO paynow_events VALUES(?,?,?)');$q->execute([$id,$type,$now]);
            if($q->rowCount()===0){$db->commit();return ['status'=>'duplicate'];}
            if($type==='ON_ORDER_COMPLETED')self::order($db,$settings,$body,$now);
            elseif($type==='ON_REFUND') {
                if(($body['status']??null)!=='completed')throw new WebhookError(400,'Refund is not completed');
                $q=$db->prepare('INSERT OR IGNORE INTO paynow_refunds VALUES(?,?,?)');
                $q->execute([self::id($body['id']??null),self::id($body['order_id']??null),self::amount($body['amount']??null)]);
            } else {
                $q=$db->prepare('INSERT OR IGNORE INTO paynow_chargebacks VALUES(?,?)');
                $q->execute([self::id($body['id']??null),self::id($body['order_id']??null)]);
            }
            $db->exec("UPDATE metadata SET value=value+1 WHERE name='revision'");
            $db->commit();return ['status'=>'processed'];
        } catch(Throwable $ex){if($db->inTransaction())$db->rollBack();throw $ex;}
    }
    private static function order(PDO $db,array $settings,array $body,int $now): void {
        if(($body['status']??null)!=='completed')throw new WebhookError(400,'Order is not completed');
        $order=self::id($body['id']??null);
        $lines=$body['lines']??null;
        if(!is_array($lines)||!array_is_list($lines)||count($lines)>1000)throw new WebhookError(400,'Invalid order lines');
        $q=$db->prepare('SELECT 1 FROM paynow_orders WHERE order_id=?');$q->execute([$order]);
        if($q->fetchColumn())return; // A different event ID must not recreate/reassign the same purchase.
        $purchases=[];
        foreach($lines as $line) {
            if(!is_array($line))throw new WebhookError(400,'Invalid order line');
            $product=$line['product_id']??'';
            if(!is_string($product)||!isset($settings['products'][$product]))continue;
            if(!is_int($line['quantity']??null)||$line['quantity']<1)throw new WebhookError(400,'Invalid quantity');
            if(!empty($line['subscription_id'])||!empty($line['trial_id']))throw new WebhookError(400,'Only one-time cosmetics are supported');
            $recipient=$line['gift_to_customer']??($body['customer']??null);
            $uuid=self::uuid(is_array($recipient)?($recipient['minecraft']['id']??null):null);
            $purchases[]=[self::id($line['id']??null),$uuid,$settings['products'][$product]];
        }
        if(!$purchases)return; // Ignore unrelated Prisons products, including buyers without Java UUIDs.
        $q=$db->prepare('INSERT INTO paynow_orders VALUES(?,?,?)');$q->execute([$order,self::amount($body['total_amount']??null),$now]);
        foreach($purchases as [$line,$uuid,$cape]) {
            $q=$db->prepare('INSERT INTO players(uuid,updated_at) VALUES(?,?) ON CONFLICT(uuid) DO UPDATE SET updated_at=excluded.updated_at');$q->execute([$uuid,$now]);
            $q=$db->prepare('INSERT INTO paynow_purchases VALUES(?,?,?,?)');$q->execute([$order,$line,$uuid,$cape]);
            $q=$db->prepare('INSERT INTO audit(created_at,uuid,action) VALUES(?,?,?)');$q->execute([$now,$uuid,'PayNow purchase '.$order.': '.$cape]);
        }
    }
    private static function id(mixed $value): string {
        if(!is_string($value)||!preg_match('/\A[0-9]{1,32}\z/',$value))throw new WebhookError(400,'Invalid PayNow identifier');
        return $value;
    }
    private static function amount(mixed $value): int {
        if(!is_int($value)||$value<0||$value>1000000000)throw new WebhookError(400,'Invalid amount');
        return $value;
    }
    private static function uuid(mixed $value): string {
        if(!is_string($value))throw new WebhookError(422,'Mapped cosmetic has no Minecraft UUID');
        $hex=strtolower(str_replace('-','',$value));
        if(!preg_match('/\A[0-9a-f]{32}\z/',$hex))throw new WebhookError(422,'Mapped cosmetic has no Minecraft UUID');
        return substr($hex,0,8).'-'.substr($hex,8,4).'-'.substr($hex,12,4).'-'.substr($hex,16,4).'-'.substr($hex,20);
    }
}
