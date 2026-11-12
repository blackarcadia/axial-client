# Community polls

Create polls in https://api.axialclient.com/admin#polls. Supply a question, 2–6
distinct choices, a duration and a unit (minutes, hours or days). For example,
use `30` minutes or `24` hours. Durations range from 1 minute to 30 days, with
up to 10 polls active together. Questions/options are immutable after publishing.

The panel shows vote totals, counts and percentages per choice, the UTC closing
time and whether voting is open. Close voting early without losing results, or
permanently delete a poll and its votes. Expired polls retain results automatically.
The panel displays the latest 50 polls; reload it to update counts.

## Client

**Mods → HUD → Polls** toggles the overlay. Its configuration button opens voting.
The default **O** key also opens voting while playing; rebind **Open Community
Polls** under Minecraft Controls. The HUD shows the earliest-ending unvoted poll
(or an already-voted poll when all have been answered), remaining time, and the
key hint. When multiple polls are active, use the arrows in the voting screen.
Select a choice and press **Submit Vote**. Votes cannot be changed.

Use **Move HUD / Move Elements** to drag the Polls card. A sample is shown when no
poll is active, and position/toggle are saved in `config/axial-polls.json`.
Disabled polls make no new polling requests and hide the overlay. Enabled clients
refresh every minute while in a world. Ended polls disappear locally; server-side
closing times remain authoritative. The countdown uses server time plus a local
monotonic clock, so changing the computer's clock does not extend a poll.

## Backend

`Polls.php` and `polls.sql` extend the existing PHP/SQLite service; no new service
or client secret is needed. `Feedback.php` dispatches authenticated `polls` and
`vote` actions under `/v1/feedback/` using the same Minecraft account verification
and in-memory bearer session as feedback. Request-supplied UUIDs are ignored.
Only the account's vote receipt is returned; vote totals stay in the admin panel.

SQLite's `(poll_id,uuid)` primary key enforces one vote per account even across
sessions/devices or simultaneous requests. A single insert checks both the
closing time and option membership under the writer lock. Same-choice retries
are idempotent; attempts to change a recorded choice return 409. Closed or
expired polls reject new votes. Admin actions retain session/CSRF protection.

Back up the database and existing panel files, then deploy `Polls.php`,
`polls.sql`, `Feedback.php`, `public/index.php` and `public/admin.css`. Schema
creation is additive and automatic. Reload PHP-FPM after updating the files.
Existing feedback and cosmetic data are preserved.

## Validation

Run `php tests/polls.php`, `php tests/polls-concurrency.php` and
`php tests/feedback.php`. These use isolated databases. Poll tests cover duration
validation, account verification, duplicate votes, retries, option membership,
expiry, early closure, results retention, deletion and active limits. The
concurrency test races two independent PHP processes voting differently for one
account and asserts exactly one stored ballot.

Client tests cover server-based countdowns, receipt validation, saved positions
and existing menu/editor callbacks. A real in-game visual/voting check is still
needed before distributing the updated client.
