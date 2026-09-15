# LANU Music — Senior Project Coordination Audit

Date: 2026-09-16
Base: main after PR #10 (`90328f872d6504d49c28ef65d693eeb7966a0fff`)

## Findings

### P0 — Truthfulness / product integrity
- Home UI contains claims that are not backed by a verified provider: "%100 Ücretsiz Platform", "PREMIUM ÖZELLİKLER BEDAVA", "Reklamsız", FLAC/320kbps marketing, "Milyonların...", "Dünya listelerinin zirvesindeki...", and a hard-coded 2025-2026 release heading.
- Social UI exposes "CANLI DİNLİYOR" and the repository still contains a simulation path for social/new-release behavior.
- Sharing emits `https://lanumusic.app/...` URLs although ownership/verification of that domain is not established in the repository.

### P1 — Architecture correctness
- `MainViewModel.artists` snapshots `repository.artists` once; local catalog changes can therefore fail to reach Home's artist list.
- Player service is a `MediaSessionService`, while the completion specification calls for `MediaLibraryService` compatibility for Android Auto.
- Deep-link handling is absent while the UI already emits content URLs.
- Typo-tolerant search is promised by the constitution but current matching is mostly normalized substring matching.

### P1 — Platform quality
- Glance widget surface is absent.
- Accessibility/state restoration coverage is thin compared with the product specification.
- Instrumentation coverage is minimal; most quality gates are unit/Robolectric only.

## Applied direction

1. Remove unsupported product claims and simulation entry points from user-facing flows.
2. Use an explicit custom LANU deep-link scheme until a real owned web domain is verified; never imply web ownership.
3. Make artist/catalog state reactive.
4. Introduce a provider-agnostic search matcher with bounded edit-distance tolerance.
5. Prepare Media3 library browsing through a real `MediaLibraryService` surface rather than claiming Android Auto support without the API.
6. Add focused tests around truthfulness, search tolerance, reactive catalog state, and deep-link routing.

The licensing/rights acquisition boundary remains unchanged: no unlicensed catalog is fabricated or implied.
