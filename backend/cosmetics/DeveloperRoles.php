<?php
declare(strict_types=1);
final class DeveloperRoles {
    public static function migrate(PDO $db): void {
        $db->exec('CREATE TABLE IF NOT EXISTS developer_roles (uuid TEXT PRIMARY KEY)');
    }
    public static function members(PDO $db): array {
        return $db->query('SELECT uuid FROM developer_roles ORDER BY uuid')->fetchAll(PDO::FETCH_COLUMN);
    }
    public static function set(PDO $db,string $uuid,bool $enabled): void {
        $uuid=strtolower($uuid);
        if(!preg_match('/^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/D',$uuid))throw new InvalidArgumentException('Use a full Minecraft UUID');
        $db->beginTransaction();
        try {
            $q=$db->prepare($enabled?'INSERT OR IGNORE INTO developer_roles(uuid) VALUES(?)':'DELETE FROM developer_roles WHERE uuid=?');
            $q->execute([$uuid]);
            $db->exec("UPDATE metadata SET value=CAST(value AS INTEGER)+1 WHERE name='revision'");
            $db->commit();
        } catch(Throwable $ex){$db->rollBack();throw $ex;}
    }
}
