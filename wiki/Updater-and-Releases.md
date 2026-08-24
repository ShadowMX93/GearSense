# Updater and releases

## Server updater

GearSense checks the official `ShadowMX93/GearSense` latest-release endpoint on
an asynchronous task, so GitHub network requests do not block the server tick.

The safe default is notification-only:

```yaml
updater:
  enabled: true
  check-on-startup: true
  check-interval-hours: 12
  notify-admins: true
  auto-download: false
```

Set `auto-download: true` to place a newer JAR in `plugins/update`. Bukkit
installs it on the next full restart. GearSense never restarts the server.

Before accepting a download, GearSense verifies that the asset URL belongs to
the official GitHub repository, the file is a readable JAR, its `plugin.yml`
names GearSense, and its embedded version matches the release tag.

## Publishing releases

1. Push feature changes to `main`.
2. Open **Actions** on GitHub.
3. Select **Release GearSense**.
4. Choose **Run workflow**.

If no release tag exists for the version in the project, the workflow publishes
that version. If its tag already exists, the workflow increments the patch
number: `1.0.1`, `1.0.2`, and so on.

If a release is deleted while its tag remains, the workflow republishes the
current version and moves that tag to the current release commit instead of
advancing the version number.

The workflow updates Maven and Gradle version files, updates documentation and
the manifest, runs the Gradle build and tests, commits the version bump directly
to `main`, pushes an annotated tag, and publishes the built JAR with generated
release notes. No pull request is created.

Advanced users may push a matching `vX.Y.Z` tag; the same workflow verifies,
builds, and publishes it.
