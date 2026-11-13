# Private cosmetic unlock codes

Open https://api.axialclient.com/admin?page=codes and sign in with the existing
admin account. Choose a cosmetic, private label, total use limit, and optional
expiry. New code text is shown once; copy it to a private location. The database
stores only a SHA-256 hash of a cryptographically random 128-bit code. There is
no code list in the client, public API, store assets, or source control.

The initial code list for all 13 current cosmetics is saved outside the project
in the owner's AxialPrivate directory. Each initial code is single-use, with no
expiry. Generate more in the private panel as needed. Total uses 0 creates a
reusable community code; each account can still unlock a cosmetic only once.
Disabling a code prevents future claims and retains existing unlocks.

Players open Wardrobe -> Redeem code. Empty wardrobes are accessible to the
signed-in account. Equipping still requires an actual grant. Redemption uses
the existing verified Minecraft session challenge (joinServer / hasJoined);
client-supplied UUIDs are ignored. Requests are limited per verified account and
network. Plaintext codes are POSTed over HTTPS, never put in URLs or audit logs.

Code claims serialize usage checks and grant writes in one SQLite transaction.
A request UUID makes a lost-response retry idempotent. Already-owned cosmetics
do not consume a code. Redeemed grants are separate from manual and PayNow
sources, so saving manual grants or refunding a purchase does not erase them.
The public grant revision increments on success. The client applies the verified
owned list immediately and ignores older snapshots that could undo the reveal.
The celebration uses the wardrobe model card, a light-green impact stamp and a
fade; it does not run on invalid, exhausted, disabled or expired codes.

Deploy RedeemCodes.php, the small Feedback.php dispatch addition, admin page
controller/navigation/view and CSS. RedeemCodes::migrate upgrades the effective
grant view while preserving both manual and paid sources. No website dist
update is needed: this feature is in the Minecraft client and VPS admin panel.

Validation: tests/redeem.php and tests/redeem-concurrency.php use synthetic
codes and in-memory/temporary databases; they never consume production codes.
Run existing PayNow, feedback, polls and dashboard tests after backend changes.
