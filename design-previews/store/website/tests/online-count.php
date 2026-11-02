<?php
declare(strict_types=1);
define('AXIAL_PRESENCE_LIBRARY_ONLY', true);
require __DIR__ . '/../online-count.php';
$id = '6a9cccc8-8e00-4a73-bd37-1121f81b0674';
if (axial_presence_session($id) !== axial_presence_session(strtoupper($id))) throw new RuntimeException('Session normalization failed.');
foreach ([null, [], '', 'not-a-uuid', "' OR 1=1 --", str_repeat('a', 10000), '6a9cccc8-8e00-1a73-bd37-1121f81b0674'] as $bad) {
    try { axial_presence_session($bad); } catch (InvalidArgumentException $ex) { continue; }
    throw new RuntimeException('Accepted an invalid session ID.');
}
$_SERVER['REQUEST_METHOD'] = 'DELETE';
ob_start(); axial_presence_run(); $body = ob_get_clean();
if (http_response_code() !== 405 || json_decode($body, true)['error'] !== 'Method not allowed.') throw new RuntimeException('Method rejection failed.');
$_SERVER['REQUEST_METHOD'] = 'GET';
putenv('AXIAL_RELAY_CONFIG');
ob_start(); axial_presence_run(); $body = ob_get_clean();
if (http_response_code() !== 503 || strpos($body, 'password') !== false) throw new RuntimeException('Missing-config handling failed.');
echo "Presence validation and safe-failure tests passed.\n";
