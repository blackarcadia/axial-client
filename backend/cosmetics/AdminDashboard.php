<?php
declare(strict_types=1);

final class AdminDashboard {
    public const PAGES=[
        'overview'=>['Overview','Your client, at a glance.','grid'],
        'cosmetics'=>['Cosmetics','Manage a player’s wardrobe access.','spark'],
        'codes'=>['Unlock codes','Private codes. New additions to their wardrobe.','spark'],
        'players'=>['Players','Find players and open their cosmetic grants.','users'],
        'feedback'=>['Feedback','Review ideas and bugs from your community.','message'],
        'polls'=>['Polls','Ask your community. See what matters to them.','chart'],
        'payments'=>['Payments','Manage your PayNow connection and delivery status.','card'],
    ];
    public static function page(mixed $page): string {return is_string($page)&&isset(self::PAGES[$page])?$page:'overview';}
    public static function url(string $page,array $query=[]): string {return '/admin?'.http_build_query(['page'=>self::page($page)]+$query);}
    private static function query(PDO $db,string $sql,array $args=[]): PDOStatement {$q=$db->prepare($sql);$q->execute($args);return $q;}
    public static function data(PDO $db,array $catalog,string $page,array $input): array {
        $data=['counts'=>[
            'players'=>(int)$db->query('SELECT COUNT(*) FROM players')->fetchColumn(),
            'feedback'=>(int)$db->query("SELECT COUNT(*) FROM feedback WHERE status='pending'")->fetchColumn(),
            'polls'=>(int)self::query($db,'SELECT COUNT(*) FROM polls WHERE closed=0 AND closes_at>?',[time()])->fetchColumn(),
            'cosmetics'=>count($catalog),
        ]];
        if($page==='codes'){require_once __DIR__.'/RedeemCodes.php';RedeemCodes::migrate($db);$data['codes']=RedeemCodes::listing($db);}
        if($page==='overview'){
            $data['recentFeedback']=$db->query('SELECT id,username,category,kind,status,created_at FROM feedback ORDER BY id DESC LIMIT 5')->fetchAll(PDO::FETCH_ASSOC);
            $data['recentPolls']=self::query($db,'SELECT p.*,COUNT(v.uuid) AS votes FROM polls p LEFT JOIN poll_votes v ON v.poll_id=p.id GROUP BY p.id ORDER BY p.id DESC LIMIT 4')->fetchAll(PDO::FETCH_ASSOC);
        }
        $number=filter_var($input['p']??1,FILTER_VALIDATE_INT);$number=$number===false?1:max(1,min(1000000,$number));
        if($page==='players'){
            $search=is_string($input['q']??null)?substr(trim($input['q']),0,36):'';
            $data['search']=$search;$where=$search!==''?' WHERE p.uuid LIKE ?':'';$args=$search!==''?['%'.$search.'%']:[];
            $total=(int)self::query($db,'SELECT COUNT(*) FROM players p'.$where,$args)->fetchColumn();
            $data+=self::pagination($number,$total,25);
            $data['players']=self::query($db,'SELECT p.uuid,p.updated_at,COUNT(g.cape) AS total FROM players p LEFT JOIN effective_grants g ON g.uuid=p.uuid'.$where.' GROUP BY p.uuid ORDER BY p.updated_at DESC,p.uuid LIMIT 25 OFFSET '.(($data['number']-1)*25),$args)->fetchAll(PDO::FETCH_ASSOC);
        }
        if($page==='feedback'){
            $category=in_array($input['category']??null,['client','prisons'],true)?$input['category']:'';
            $status=in_array($input['status']??null,['pending','approved'],true)?$input['status']:'';
            $clauses=[];$args=[];
            if($category!==''){$clauses[]='category=?';$args[]=$category;}
            if($status!==''){$clauses[]='status=?';$args[]=$status;}
            $where=$clauses?' WHERE '.implode(' AND ',$clauses):'';
            $total=(int)self::query($db,'SELECT COUNT(*) FROM feedback'.$where,$args)->fetchColumn();
            $data+=self::pagination($number,$total,20);$data['category']=$category;$data['status']=$status;
            $data['feedback']=self::query($db,'SELECT * FROM feedback'.$where.' ORDER BY id DESC LIMIT 20 OFFSET '.(($data['number']-1)*20),$args)->fetchAll(PDO::FETCH_ASSOC);
        }
        if($page==='polls'){
            $state=in_array($input['state']??null,['open','ended'],true)?$input['state']:'';
            $where=$state==='open'?' WHERE closed=0 AND closes_at>?':($state==='ended'?' WHERE (closed=1 OR closes_at<=?)':'');$args=$state!==''?[time()]:[];
            $total=(int)self::query($db,'SELECT COUNT(*) FROM polls'.$where,$args)->fetchColumn();
            $data+=self::pagination($number,$total,10);$data['state']=$state;
            $data['polls']=self::query($db,'SELECT * FROM polls'.$where.' ORDER BY id DESC LIMIT 10 OFFSET '.(($data['number']-1)*10),$args)->fetchAll(PDO::FETCH_ASSOC);
            foreach($data['polls'] as &$poll){
                $poll['results']=self::query($db,'SELECT o.option_id,o.label,COUNT(v.uuid) AS votes FROM poll_options o LEFT JOIN poll_votes v ON v.poll_id=o.poll_id AND v.option_id=o.option_id WHERE o.poll_id=? GROUP BY o.option_id ORDER BY o.option_id',[$poll['id']])->fetchAll(PDO::FETCH_ASSOC);
                $poll['total']=array_sum(array_column($poll['results'],'votes'));
            }
        }
        return $data;
    }
    private static function pagination(int $number,int $total,int $size): array {
        $pages=max(1,(int)ceil($total/$size));return ['number'=>min($number,$pages),'pages'=>$pages,'total'=>$total];
    }
}

function admin_icon(string $name): string {
    $paths=[
        'grid'=>'<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/>',
        'spark'=>'<path d="m12 3 2.7 6.3L21 12l-6.3 2.7L12 21l-2.7-6.3L3 12l6.3-2.7Z"/>',
        'users'=>'<circle cx="9" cy="8" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3M17 5a3 3 0 0 1 0 6M18 15a5 5 0 0 1 3 4v2"/>',
        'message'=>'<path d="M21 11a8 8 0 0 1-8 8H7l-4 3V6a3 3 0 0 1 3-3h7a8 8 0 0 1 8 8Z"/><path d="M7 8h9M7 12h6"/>',
        'chart'=>'<path d="M4 20h17M7 16V9M12 16V4M17 16v-5"/>',
        'card'=>'<rect x="3" y="5" width="18" height="14" rx="3"/><path d="M3 10h18M7 15h3"/>',
        'arrow'=>'<path d="M5 12h14m-6-6 6 6-6 6"/>',
        'logout'=>'<path d="M9 4H4v16h5M12 12h9m-4-4 4 4-4 4"/>',
        'check'=>'<path d="m5 12 4 4L19 6"/>',
    ];
    return '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'.($paths[$name]??$paths['grid']).'</svg>';
}
function admin_csrf(): void {echo '<input type="hidden" name="csrf" value="'.h($_SESSION['csrf']).'">';}
function admin_pagination(array $data,string $page,array $query=[]): void {
    if($data['pages']<=1)return;
    echo '<nav class="pagination" aria-label="Pagination"><span>'.h((string)$data['total']).' items · Page '.h((string)$data['number']).' of '.h((string)$data['pages']).'</span><div>';
    if($data['number']>1)echo '<a class="button" href="'.h(AdminDashboard::url($page,$query+['p'=>$data['number']-1])).'">Previous</a>';
    if($data['number']<$data['pages'])echo '<a class="button" href="'.h(AdminDashboard::url($page,$query+['p'=>$data['number']+1])).'">Next</a>';
    echo '</div></nav>';
}
