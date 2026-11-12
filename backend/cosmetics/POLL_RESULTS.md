# Compact polls and aggregate results

The client poll HUD is 228 × 90 GUI pixels, reduced from 238 × 108. It shows the
question, countdown, first two choices, percentages, animated bars, total votes
and voting shortcut. Polls with more choices show a `+N choices` hint; all 2–6
options remain available in the voting menu. The HUD editor uses the same bounds.
The voting menu sizes itself to the question and choices, with a 300-pixel height
cap and scrolling for longer content.

Authenticated `polls` responses now include `votes` on every option, including
explicit zero counts. Successful `vote` responses include the same option results
alongside `optionId`. Aggregate results are visible before and after voting;
voter UUIDs and names are never returned. Older clients ignore the extra fields.
New clients show unavailable results when connected to a backend without counts.

The client refreshes results every 30 seconds and after a vote. Receipts use the
server totals directly, so retries cannot inflate the displayed counts. Empty
polls show 0% on every bar. Nonempty percentages use largest-remainder rounding
to total 100%, with stable option-ID ordering for tied remainders.

Deploy only `Polls.php` and `tests/polls.php` for this API update, preserving all
database files and configuration. No schema or grant changes are required.
The SQLite uniqueness constraint and existing vote confirmation remain intact.

Validation: `php tests/polls.php` uses an in-memory database and covers zero
counts, aggregates visible to unvoted users, idempotent receipts, one vote per
account, closure and deletion. Client tests cover count parsing, percentage
rounding, receipt handling, countdowns and compact HUD bounds. An in-game visual
check remains pending because the development client currently fails at an
unrelated Emotecraft/Bendable Cuboids mixin during startup.
