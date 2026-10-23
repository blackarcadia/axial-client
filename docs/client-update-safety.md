# Client update safety validation

Validated locally on macOS with JDK 25. The release workflow uses JDK 21 on macOS and Windows; those hosted jobs have not been run for these additional changes yet.

## Enforced behavior

- Both downloaded-client installation and bundled-client installation use the same JVM and operating-system file lock. Installation eligibility is checked again while holding it.
- Each game directory records its highest confirmed client build in `.client-minimum-build`. The record is written using a flushed temporary file and atomic replacement where supported. It never decreases.
- Missing, corrupted, or externally restored old client files cannot cause the fixed launcher to install or launch a build below that record. An unavailable repair causes launch to stop.
- Release tags, outer launcher metadata, and embedded client metadata must agree. Unsupported version strings cannot gain priority through incidental digits.
- Stale legacy client JARs are removed even when a newer downloaded client is preserved, preventing Fabric from loading a mixed installation. Locked legacy JARs cause launch to stop.
- Separate downloads use separate staging directories. Client extraction completes before installed files are replaced; an ordinary installation I/O failure restores the previous client and dependencies.
- Release packaging verifies the actual nested client metadata. Release version inputs invalidate expanded resources. Both platform release jobs run the launcher regression suite before packaging.

## Checks performed

`./gradlew test --tests 'org.example.launcher.*' verifyReleasePayload --offline`

Result: **41 launcher tests passed**, including:

- 1,000 deterministic randomized update/restart events, checking that installed builds never decrease.
- 32 concurrent update attempts interleaved with bundled-client installation.
- Lock contention between separate JVMs.
- Killing an installer JVM while the working client is backed up, followed by refusal of an older fallback and successful same-build repair.
- Mid-commit I/O failure and byte-for-byte restoration of the original client.
- Deleted, corrupted, and restored old clients; damaged version records; independent game directories.
- HTTP failures, malformed release responses, stale embedded payloads, shuffled release publication order, and separate staging for repeated downloads.
- Validation of the actual protected release JAR and its nested client metadata.

Changing `appVersion` to `1.0.987654` and `appChannel` to `release` regenerated `version.properties` with those values. Normal development metadata was restored and the final tests and release verification passed again.

A full-suite run also found four failures in `PotionsHudConfigTest`, `PumpkinBombModelTest`, and `ColorRowsRegressionTest`. The same four failures were reproduced from an untouched archive of commit `61fbe1ad`; they predate this hardening.

## Scope of the evidence

These checks validate the fixed launcher's version selection and installation paths. They do not prove behavior on every filesystem or operating system, and the synthetic update fixtures do not run an authenticated Minecraft session. The operating-system tests ran on macOS, not Windows.

Older launcher executables do not participate in these locks or version checks. Users need to install the fixed launcher once because the existing updater replaces mods rather than the launcher executable. Deleting/restoring the entire game directory also deletes/restores its local version history; protection against arbitrary external replacement of both the client and its history is outside this design.
