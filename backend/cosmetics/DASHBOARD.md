# Admin dashboard

The dashboard remains at https://api.axialclient.com/admin, using the existing
admin account, secure session, CSRF tokens and login limits. Pages use the `page`
query parameter, so no Caddy routing changes or new services are required.

- **Overview:** live counts, workspace shortcuts, recent feedback and polls.
- **Cosmetics:** UUID lookup, capes/auras/outfits grouped into selectable cards,
  and the existing manual grant workflow. Active purchases retain their access.
- **Players:** UUID search, 25 players per page, links to their wardrobe editor.
- **Feedback:** category/status filters, 20 submissions per page, approval/reply,
  and a separate expandable permanent-deletion control.
- **Polls:** expandable creation form, open/ended filters, 10 polls per page,
  results, early closure and expandable deletion. Older results remain accessible.
- **Payments:** signing-secret configuration/status, verified events, existing
  refund warnings and setup notes. Stored secrets are never rendered.

Legacy `/admin?uuid=...` links open Cosmetics. Old overview fragments point to
shortcut cards; new direct links use `/admin?page=feedback`, `/admin?page=polls`
and `/admin?page=payments`.

`AdminDashboard.php` owns read-only dashboard queries, filter validation and
pagination. `views/admin.php` is the shared navigation/login shell; the other
six view files are page-specific templates. Page names are allowlisted before
selecting a view. `public/admin.css` provides the responsive charcoal/purple
appearance. No JavaScript, third-party fonts, external dependencies or CSP
relaxation are introduced.

## Deployment

Back up `public/index.php`, `public/admin.css` and the SQLite database. Deploy
`AdminDashboard.php`, all seven `views/*.php` files, `public/admin.css` and
`public/index.php`; install helpers/views before replacing the entry point.
Reload PHP-FPM to clear opcode caches. Do not replace the live config/database.
No schema changes are needed for this dashboard update.

## Validation

`php tests/admin_dashboard.php` uses an in-memory database to check navigation,
filters, search, pagination, poll totals, CSRF fields, escaping and all page
renders, including the signed-out view. An optional directory argument writes
fake-data HTML pages for visual review. Existing feedback, poll/concurrency and
PayNow regression tests remain applicable. Desktop and 390-pixel mobile previews
are checked in Chrome before deployment. Live checks exercise login, all pages,
legacy player links, unauthenticated boundaries and the existing API health.
