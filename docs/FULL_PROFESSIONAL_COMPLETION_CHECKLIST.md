# LANU Music — Completion Checklist

## Data integrity
- [x] Fabricated popularity counts removed from the product contract.
- [x] Simulated new-release trigger is disabled.
- [ ] All legacy social UI paths are either backed by a verified provider or explicitly show unavailable state.

## Library
- [x] User-scoped favorites.
- [x] User-scoped playlists.
- [x] Playlist creation and deletion.
- [x] Playlist rename contract.
- [x] Playlist reorder contract.
- [x] Playlist reorder tests.
- [x] History clear and user isolation tests.
- [ ] Playlist UI verifies rename/reorder end-to-end.

## Player
- [x] Media3 MediaSession.
- [x] Queue, next and previous.
- [x] Shuffle and repeat state.
- [x] Seek and progress state.
- [ ] Runtime smoke coverage for headset/audio-focus/background/lock-screen.
- [ ] Persist and restore playback state across process recreation.

## Offline
- [x] Local MediaStore scanning.
- [x] Downloaded-file reconciliation with database state.
- [x] SHA-256 for downloaded local audio.
- [ ] Runtime smoke coverage for corrupt/missing/offline files.

## Platform
- [x] MediaLibraryService surface.
- [x] Android manifest service registration.
- [x] Glance widget surface.
- [x] `lanumusic://` deep-link scheme.
- [ ] Deep-link destination smoke coverage.
- [ ] Widget action smoke coverage.
- [ ] Android Auto runtime verification.

## Product quality
- [ ] Accessibility audit.
- [ ] State restoration audit.
- [ ] Final UI smoke test.
- [ ] Final post-merge Main CI.
- [ ] Final APK artifact.
- [ ] Independent APK SHA-256 verification.
