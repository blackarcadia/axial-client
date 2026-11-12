# Player feedback

Updated clients expose **Feedback** in the quick menu and Features → World. The
editor uses the shared client panels, buttons, fonts and accent colours. Players
can send 10–2000 characters, up to five submissions per account per day. Requests
run off the game thread, and failed sends retain the draft for retry.

In https://api.axialclient.com/admin#feedback, the team can read the latest 100
submissions, approve with an optional reply, or delete permanently. Approval
queues a private notification; repeating approval does not queue another.
Deletion removes the submission text but preserves an undelivered approval.
SQLite securely clears deleted cells and reuses freed pages; the file does not
need to shrink after every deletion. Existing backups retain their snapshots.

Eight seconds after joining a world, the client checks for approvals and shows
them in chat, then acknowledges them. Only acknowledged notices are removed.
Network failure can cause a notice to appear again on the next login rather than
lose it. Notifications are checked on world/server join, not launcher startup.

## Identity and API

All routes use POST JSON under `/v1/feedback/`, with no caching:

- `challenge`: issues a random, single-use 120-second server ID.
- `authenticate`: checks the username/server ID with Mojang `hasJoined` after the
  client calls the Minecraft session service `joinServer`. Returns a one-hour
  bearer token; only its SHA-256 hash is stored. The client holds it in memory.
- `submit`: requires a bearer token, message and per-draft UUID `requestId`.
  Retries with the same account/request ID return the existing submission.
- `notifications`: returns at most 20 outstanding approvals for that account.
- `ack`: accepts at most 20 notice IDs and removes only that account's notices.

Minecraft access tokens and admin credentials are never sent to the feedback
API or bundled in the client. Challenge/authentication attempts and submissions
are rate limited. Expired auth/rate records are pruned on API requests. Admin
actions use the existing authenticated session and CSRF protection; text is
escaped before rendering.

## Deployment and checks

Deploy `Feedback.php`, `feedback.sql`, `public/index.php` and `public/admin.css`
alongside the existing service. Back up the SQLite database and panel files
first. The service applies the additive feedback schema automatically and keeps
cosmetics, purchases and grants unchanged. Reload PHP-FPM after deploying.

Run `php tests/feedback.php` for isolated tests using an in-memory database and
an injected account verifier. They cover authentication/replay, validation,
idempotency, account isolation, approval/deletion, acknowledgements and limits.
Live checks cover the authenticated panel and unauthenticated API boundaries.
A real Minecraft account is still needed to check submission and notification
delivery end to end in game.
