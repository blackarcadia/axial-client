<?php
declare(strict_types=1);
define('AXIAL_NEWS_TEST', true);
require __DIR__ . '/../news-feed.php';
function check(bool $condition, string $message): void { if (!$condition) throw new RuntimeException($message); }
$fixture = file_get_contents(__DIR__ . '/fixtures/news.xml');
$posts = axial_news_parse($fixture);
check(count($posts) === 1, 'Live RSS fixture should yield one post.');
check($posts[0]['id'] === '37' && $posts[0]['title'] === 'TEST ARTICLE', 'Stable thread ID and title.');
check($posts[0]['author'] === 'Subxil', 'Author should use creator, not placeholder email.');
check(strpos($posts[0]['html'], 'NEWS TEST') !== false, 'Full article HTML should survive parsing.');
check($posts[0]['publishedAt'] === '2026-10-25T13:28:13+00:00', 'Publication timestamp.');
$foreign = str_replace('https://axialprisons.com/threads/', 'https://other.example/threads/', $fixture);
check(axial_news_parse($foreign) === [], 'Foreign article origins are not mirrored.');
foreach (['<html>not RSS</html>', '<rss>', '<!DOCTYPE rss [<!ENTITY x SYSTEM "file:///etc/passwd">]><rss><channel/></rss>'] as $invalid) {
    $rejected = false;
    try { axial_news_parse($invalid); } catch (Throwable $error) { $rejected = true; }
    check($rejected, 'Malformed and entity-bearing feeds must be rejected.');
}
check(axial_news_parse('<rss><channel/></rss>') === [], 'Empty news is a valid feed.');
function request(?callable $fetch = null): array {
    http_response_code(200); ob_start(); axial_news_run($fetch); $body = ob_get_clean();
    return [http_response_code(), json_decode($body, true, 512, JSON_THROW_ON_ERROR)];
}
$directory = sys_get_temp_dir() . '/axial-news-' . substr(hash('sha256', realpath(__DIR__ . '/..')), 0, 16);
$file = $directory . '/feed-v1.json';
@mkdir($directory, 0700, true);
@unlink($file);
$_SERVER['REQUEST_METHOD'] = 'POST';
[$code, $body] = request(); check($code === 405, 'Only GET is allowed.');
$_SERVER['REQUEST_METHOD'] = 'GET';
[$code, $body] = request(static function () { throw new RuntimeException('Test source outage'); });
check($code === 502 && isset($body['error']), 'Uncached upstream failure is explicit.');
[$code, $body] = request(static function () use ($fixture) { return $fixture; });
check($code === 200 && count($body['posts']) === 1 && $body['stale'] === false, 'Fresh upstream response.');
[$code, $body] = request(static function () { throw new RuntimeException('Fresh cache should prevent fetch.'); });
check($code === 200 && $body['stale'] === false, 'Fresh cache is served without fetching.');
$saved = json_decode(file_get_contents($file), true); $saved['updatedAt'] = time() - 360;
file_put_contents($file, json_encode($saved));
[$code, $body] = request(static function () { throw new RuntimeException('Test source outage'); });
check($code === 200 && $body['stale'] === true && count($body['posts']) === 1, 'Stale posts survive source outages.');
[$code, $body] = request(static function () { return '<rss><channel/></rss>'; });
check($code === 200 && $body['posts'] === [] && $body['stale'] === false, 'Empty valid feed replaces old posts.');
@unlink($file);
echo "News feed parsing, origin checks, XML rejection, caching, empty feed, and outage handling passed.\n";
