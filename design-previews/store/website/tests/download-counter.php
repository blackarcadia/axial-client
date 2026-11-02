<?php
declare(strict_types=1);
define('AXIAL_DOWNLOAD_TEST', true);
require __DIR__ . '/../download-counter.php';
function check_download(bool $condition, string $message): void { if (!$condition) throw new RuntimeException($message); }
$release = ['draft' => false, 'assets' => [
 ['id' => 1, 'name' => 'AxialLauncher-windows.zip', 'download_count' => 10],
 ['id' => 2, 'name' => 'AxialLauncher.app.zip', 'download_count' => 20],
 ['id' => 3, 'name' => 'AxialLauncher-windows-installer.exe', 'download_count' => 5],
 ['id' => 4, 'name' => 'axial-client.jar', 'download_count' => 999],
]];
$older = ['assets' => [['id' => 5, 'name' => 'AxialLauncher.app.zip', 'download_count' => 3]]];
$pages = [];
$total = axial_download_total(static function (int $page) use ($release, $older, &$pages): array {
 $pages[] = $page; return $page === 1 ? array_fill(0, 100, $release) : [$older];
});
check_download($pages === [1, 2], 'All release pages should be fetched.');
check_download($total['count'] === 38 && $total['platforms'] === ['windows' => 15, 'mac' => 23], 'Combined totals, installer, old releases, asset deduplication, unrelated-file exclusion.');
check_download(axial_download_total(static function () { return []; })['count'] === 0, 'Empty repository has a real zero total.');
$draft = $release; $draft['draft'] = true;
check_download(axial_download_total(static function () use ($draft) { return [$draft]; })['count'] === 0, 'Drafts are not public downloads.');
$invalid = $release; $invalid['assets'][0]['download_count'] = -1;
$failed = false; try { axial_download_total(static function () use ($invalid) { return [$invalid]; }); } catch (Throwable $e) { $failed = true; }
check_download($failed, 'Invalid counts cannot be published.');
function download_request(?callable $fetch = null): array {
 http_response_code(200);ob_start();axial_download_run($fetch);$body=ob_get_clean();return [http_response_code(),json_decode($body,true,512,JSON_THROW_ON_ERROR)];
}
$directory = sys_get_temp_dir() . '/axial-downloads-' . substr(hash('sha256', realpath(__DIR__ . '/..')), 0, 16);
@mkdir($directory,0700,true);$file=$directory.'/github-v1.json';$retry=$directory.'/retry-at';@unlink($file);@unlink($retry);
$_SERVER['REQUEST_METHOD']='POST';[$code,$body]=download_request();check_download($code===405,'Clicks cannot artificially increment the count.');
$_SERVER['REQUEST_METHOD']='GET';
[$code,$body]=download_request(static function () {throw new RuntimeException('Test API outage');});check_download($code===502&&!isset($body['count']),'Outage must not invent zero.');
@unlink($retry);
[$code,$body]=download_request(static function () use ($release) {return [$release];});check_download($code===200&&$body['count']===35,'Fresh count is returned.');
[$code,$body]=download_request(static function () {throw new RuntimeException('Should not fetch fresh cache');});check_download($code===200&&$body['stale']===false,'Fresh cache prevents extra API calls.');
$cached=json_decode(file_get_contents($file),true);$cached['updatedAt']=time()-1000;file_put_contents($file,json_encode($cached));
[$code,$body]=download_request(static function () {throw new RuntimeException('Test API outage');});check_download($code===200&&$body['count']===35&&$body['stale']===true,'Existing total survives API failure.');
[$code,$body]=download_request(static function () {throw new RuntimeException('Retry cooldown should prevent fetching');});check_download($code===200&&$body['stale']===true,'API failures are throttled.');
@unlink($file);@unlink($retry);
echo "Download pagination, platform totals, deduplication, invalid data, cache, rate-limit cooldown, and outage handling passed.\n";
