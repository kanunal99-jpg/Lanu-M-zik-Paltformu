# LANU Music — Full Professional Release Gate

This document is the final product-quality gate for the local-first LANU Music application.

## Product requirements

- No fabricated catalog, popularity, release or social activity data.
- Local MediaStore audio remains the primary safe playback source when available.
- Verified/licensed/permission-compatible audio only; no DRM bypass or piracy.
- User library remains user-scoped and survives app restart.
- Playlist lifecycle covers create, add, remove, reorder, rename and delete.
- History is created by real playback, not simulated activity.
- Offline state is reconciled against files on disk.
- Player state is synchronized with Media3/MediaSession.
- Shuffle and repeat obey standard player semantics.
- Android Auto, widget and deep links are wired to real in-app destinations.
- Sharing must use LANU app/deep-link targets unless a verified public web target exists.

## Quality gate

1. Unit and Robolectric tests pass.
2. APK assemble passes.
3. APK SHA-256 is generated and independently verified.
4. Post-merge `main` CI passes.
5. The resulting artifact is retained as the release candidate.

## Explicit non-goals

- Inventing a cloud catalog.
- Claiming remote accounts or social features that are not backed by a real provider.
- Adding paid APIs/infrastructure without approval.
- Treating a successful compile as proof of runtime correctness.
