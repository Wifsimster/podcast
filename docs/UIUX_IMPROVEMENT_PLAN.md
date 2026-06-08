# Ondes — UI/UX Improvement Plan

## 1. TL;DR

- **Cut the bottom navigation from 6 tabs to 4** (Home / Library / Search / Queue). Fold **Downloads into Library** as a filter, and **demote Settings** to a top-app-bar action on Home. This is the headline fix and brings the app into Material 3 spec (3–5 destinations).
- **Wire up the refresh and state plumbing that already exists but is dead.** `refresh()`/`refreshing` flows are implemented in Home and Podcast Detail ViewModels but never collected — there is no pull-to-refresh anywhere in the app despite being the primary way users fetch new episodes.
- **Stop swallowing errors in the data layer.** Search network failures render as "No podcasts found," and a *successful* paste-a-URL subscribe renders as a failure-styled "No luck there." The retryable error UI already exists; the repository just needs to stop collapsing exceptions into empty lists.
- **Make destructive actions reversible.** Download-delete, queue-remove (hidden long-press), and "Clear queue" all fire instantly with no confirmation/undo. Add Snackbar+Undo and a confirm dialog.
- **Fix the brand identity bug.** Dynamic color silently discards the chili-red color scheme on every Android 12+ device, so the app ships with two competing accents. Copy the brand primary into the dynamic scheme.
- **Add a proper resume experience on Home.** "Continue listening" sorts by publish date (not last-played), looks identical to "Latest," and never shows time-remaining — undercutting Home's entire job of driving the next play.

---

## 2. Bottom Navigation (headline concern)

### Recommendation: go to **4 tabs**

Material 3 specifies navigation bars for **3–5 destinations**. Ondes currently ships **6**, and two of them don't earn a permanent slot: **Settings** is a textbook overflow destination (reached from a top-bar icon in Pocket Casts, AntennaPod, Spotify), and **Downloads** is just a filtered view of the library. **Queue** is the one secondary surface that genuinely deserves prominence in a player-centric app.

At six tabs, labels truncate on narrow screens and at large font scale, and each target shrinks below comfortable size.

### Before → After

| Slot | Before (6) | After (4) |
|------|-----------|-----------|
| 1 | Home | **Home** |
| 2 | Queue | **Library** *(Downloads folded in as a filter chip/tab)* |
| 3 | Library | **Search** |
| 4 | Search | **Queue** |
| 5 | Downloads | — *(Downloads → Library filter)* |
| 6 | Settings | — *(Settings → top-app-bar action/avatar on Home)* |

**Why this exact set, decisively:**

- **Home** — the launch surface; drives the next play.
- **Library** — subscriptions *and* downloads. Downloads is a filtered library view (both already render `EpisodeRow`), so a filter chip ("Downloaded") inside Library is the right home for it. This also gives Downloads a place to grow storage/management tooling without a top-level slot.
- **Search** — discovery; already a strong standalone surface.
- **Queue** — kept top-level because "what's next" is a primary, frequently-checked surface for a podcast player, and it benefits from being one tap away. (It already has its own `TopAppBar` in `QueueScreen.kt:67`.)
- **Settings** — moved to a gear/avatar action on the Home top app bar (and optionally a gear in the player). Matches platform convention and frees the most valuable nav real estate.

> Alternative considered: a 5-tab set keeping Downloads as its own tab. Rejected — Downloads is a filtered library view, and demoting it keeps us comfortably mid-range in the 3–5 spec rather than at the ceiling.

### Navigation cleanups to ship alongside the IA change

| Change | Severity | Effort |
|---|---|---|
| Add `windowInsetsPadding(navigationBars)` to the nav bar / mini-player so content clears the system bar (`OndesNavigation.kt:76-86`, `MiniPlayer.kt:49-53`) | high | moderate |
| Selected-tab detection uses `==` instead of `destination.hierarchy.any { }` — fix so nested destinations highlight the right tab (`OndesNavigation.kt:159`) | high | moderate |
| Tab re-select is a no-op (`OndesNavigation.kt:162-172`) — wire it to `animateScrollToItem(0)` (scroll-to-top, the standard pattern) | medium | quick-win |
| Adopt **`NavigationSuiteScaffold` / `NavigationRail`** so the same 4 destinations adapt to medium/expanded widths (tablets, foldables, landscape) instead of a bottom bar everywhere (`OndesNavigation.kt:73-88`) | high | large |

**File:** `app/src/main/java/com/ondes/podcast/ui/navigation/OndesNavigation.kt`, `Routes.kt`, `MiniPlayer.kt`

---

## 3. Top recommendations by screen / feature

### Home

| Change | Sev | Effort |
|---|---|---|
| **Wire pull-to-refresh.** `refresh()`/`refreshing` exist in `HomeViewModel.kt:49-59` but Home never collects them — no manual refresh affordance at all. Wrap the `LazyColumn` in M3 `PullToRefreshBox` bound to `viewModel.refreshing`/`viewModel.refresh()`. | high | moderate |
| **Fix "Continue listening" ordering.** Query is documented "most recently touched first" but does `ORDER BY pubDate DESC` (`Daos.kt:72,77`). Add a `lastPlayedAt: Long` column to `EpisodeEntity`, set it in the position-update path, sort by it. | high | moderate |
| **Differentiate resume from latest.** Both sections render identical `EpisodeRow`s (`HomeScreen.kt:76-91` vs `96-111`). Make the first "Continue listening" item a hero card with a prominent **Resume** button + "X min left", or give in-progress rows a filled primary play button vs outlined for fresh. | high | moderate |
| **Show "time remaining" on resume rows.** Data (`positionMs`/`durationMs`) is already in hand; metadata line only shows date · total duration. When `positionMs > 0 && !isFinished`, append "23 min left" (localized EN/FR). | medium | quick-win |
| **Don't auto-play on row tap.** `open()` calls `connection.play(...)` (`HomeViewModel.kt:68`) so a tap hijacks current audio. Make row tap open an episode-detail / show-notes sheet (or the player without auto-start); reserve playback for the play button. | medium | moderate |
| **Content-shaped skeleton on cold load** instead of a centered blocking spinner (`HomeScreen.kt:48-60`). | medium | moderate |
| **Large-screen adaptation.** Single narrow column at any width (`HomeScreen.kt:70-112`). Cap content width; on expanded, "Continue listening" as a horizontal carousel above a "Latest" grid. | medium | large |
| **"See all" for Latest.** Hard-capped at `LIMIT 50` (`Daos.kt:87`) with no affordance. Add a "See all episodes" row or Paging 3. | low | moderate |
| **Better empty-state icon.** `Whatshot` (flame) reads "trending," not "subscribe" (`HomeScreen.kt:64`). Use Podcasts/Add/Search + a leading icon on the CTA. | low | quick-win |

### Now Playing / Player

| Change | Sev | Effort |
|---|---|---|
| **Swipe-down-to-dismiss.** Player is dismissible only via a small chevron (`PlayerScreen.kt:99-104`). Add a vertical drag-to-dismiss (`anchoredDraggable`) + a grabber handle at the top. | high | moderate |
| **Add `navigationBarsPadding()`.** Show notes clip under the gesture/nav bar; the fixed 32dp spacer is not a substitute (`PlayerScreen.kt:91,254`). | high | quick-win |
| **Remaining-time toggle.** Tap right-hand timestamp to toggle total ↔ "-12:34", adjusted for `state.speed`, persisted, with a `stateDescription` (`PlayerScreen.kt:154-160`). | medium | quick-win |
| **Queue / up-next from the player.** No next/prev and no queue entry point (`PlayerScreen.kt:163-202`). Add a queue affordance (ModalBottomSheet mirroring ChaptersSheet) and/or skip-next, backed by new `PlaybackConnection` calls. | medium | large |
| **Configurable, label-correct skip intervals** (see Accessibility — this is also a correctness bug). Read `skipBackMs`/`skipForwardMs` in `PlayerViewModel`; reconsider the 10/30 asymmetry. | medium | moderate |
| **Stabilize the secondary control row.** Chapters button only renders when present, so Speed/Sleep jump position across episodes (`PlayerScreen.kt:205-228`). Reserve the slot; give Speed more prominence; ensure 48dp targets. | low | moderate |
| **Chapters sheet a11y.** Active chapter is color-only (`PlayerScreen.kt:359-368`) — add `selected`/`stateDescription` + a non-color cue + per-row `contentDescription`. | low | quick-win |
| **Mini-player second line.** Add a dimmer podcast-name line under the title; verify both `IconButton`s meet 48dp (`MiniPlayer.kt:84-92,107-113`). | low | quick-win |
| **Buffering state on the scrubber.** While `duration<=0` the slider is inert at 0:00/0:00 and reads as broken (`PlayerScreen.kt:148`); show a buffering hint and, if available, a buffered-position secondary track. | low | quick-win |
| **Cap show-notes line length** (`widthIn(max=600.dp)`) for tablet readability (`HtmlText.kt`, `PlayerScreen.kt:93`). | low | moderate |

### Library & Podcast Detail

| Change | Sev | Effort |
|---|---|---|
| **Wire Podcast-detail refresh.** `refreshing` exists (`PodcastViewModel.kt:42-53`) but is never collected; the toolbar Refresh icon does nothing visible. Collect it, wrap list in `PullToRefreshBox`, swap the icon for a spinner while refreshing. | high | moderate |
| **Unplayed/new-episode badges on tiles.** No badge and no data (`LibraryScreen.kt:67-93`, `Daos.kt:16`). Add a DAO count projection (episodes where `isFinished=0`) → `PodcastWithUnplayed`, overlay a `Badge` on artwork. | high | large |
| **Sort/filter controls.** Library is locked to alphabetical (`Daos.kt:16`); detail list locked to `pubDate DESC` (`Daos.kt:50`). Add Library sort (Recently updated / Alphabetical / Unplayed) and a newest/oldest + "Unplayed only" `FilterChip` on detail. Persist the choice. | high | large |
| **Confirm/undo on unsubscribe.** One tap on "Subscribed" silently unsubscribes (`PodcastScreen.kt:132-138`) with no SnackbarHost at all. Add Snackbar+Undo; clarify the destructive affordance. | high | moderate |
| **Loading/error/empty on Podcast detail.** Cold/offline feed shows empty top bar, blank artwork, "0 episodes" (`PodcastScreen.kt:65,109`); `runCatching` swallows failures (`PodcastViewModel.kt:50`). Add Loading/Content/Error states + Retry. | high | moderate |
| **Expandable description.** Truncated to 5 lines with no "Show more" (`PodcastScreen.kt:149-158`). Toggle `maxLines`. | medium | quick-win |
| **Adaptive grid.** `GridCells.Fixed(2)` → `GridCells.Adaptive(minSize=160.dp)` (`LibraryScreen.kt:57`); cap detail content width on large screens. | medium | quick-win |
| **Merge tile semantics.** Each tile reads as 3 separate TalkBack nodes (`LibraryScreen.kt:68-92`). Add `semantics(mergeDescendants=true)` + onClick label "Open {title}". | medium | quick-win |
| **Stable subscribe button width** (filled "Subscribe" ↔ outlined "Subscribed" resize); set `contentDescription` to reflect unsubscribe when subscribed (`PodcastScreen.kt:132-146`). | low | quick-win |
| **Localize the version footer.** Hardcoded `"🌶️ Ondes v…"` literal in the grid (`LibraryScreen.kt:95-103`); version already lives in Settings — remove it or move to a localized string. | low | quick-win |

### Search / Discover

| Change | Sev | Effort |
|---|---|---|
| **Stop swallowing search errors.** `runCatching{...}.getOrDefault(emptyList())` (`PodcastRepository.kt:168-170`) makes offline look like "No podcasts found"; the retryable error UI (`SearchScreen.kt:94-100`) is dead code. Return a `Result`/sealed type so the VM distinguishes a thrown error (retryable) from an empty 200. | high | moderate |
| **Localize theme chip labels.** `PodcastThemes.all` hardcodes English (`PodcastThemes.kt:16-31`), so Browse-by-theme is untranslated in FR and produces mixed-language errors. Replace `label: String` with `@StringRes labelRes: Int`; add EN/FR strings. | high | moderate |
| **Fix paste-URL success state.** A successful subscribe is pushed through the `error` field and renders as "No luck there" with a SearchOff icon (`SearchViewModel.kt:98-103`, `SearchScreen.kt:94-100`). Model it as its own one-shot success event (snackbar / navigate to the new podcast). | high | moderate |
| **Make result rows tappable** to open podcast detail before subscribing (`SearchScreen.kt:229-240`); after subscribe, turn the button into "View" not a dead "Added". | medium | moderate |
| **Surface the dual-purpose field + robust URL detection.** Token-matching (`startsWith('http') && contains 'rss'/'feed'/'.xml'`, `SearchViewModel.kt:55`) misses valid feeds. Detect any http(s) URL; add a persistent helper line / "Add feed" affordance. | medium | moderate |
| **Clear button + debounce.** No trailing clear "x"; search only fires on IME action (`SearchScreen.kt:65-74`). Add a clear icon (returns to theme landing) and ~300ms as-you-type search. Consider M3 `SearchBar`/`DockedSearchBar`. | medium | quick-win |
| **Large-screen layout** — cap field/list width, multi-column grid for theme results on expanded (`SearchScreen.kt:83-92,174-184`). | medium | large |
| **Richer Browse-by-theme.** Only 3 shows/category (`SearchViewModel.kt:79`); empty genre and network failure look identical and both blame the connection. Raise the limit to 10–15; distinguish empty-success from error. | low | quick-win |
| **Skeleton loading** instead of clearing results to a centered spinner on every chip tap (`SearchScreen.kt:77-81,156-163`, `SearchViewModel.kt:73-77`). | low | moderate |

### Settings, Downloads, Queue

| Change | Sev | Effort |
|---|---|---|
| **Downloads storage management.** Bare `LazyColumn` of rows, no header, no sizes (`DownloadsScreen.kt:40-60`); `EpisodeEntity` has no size field. Add `fileSizeBytes` (from Content-Length / `File.length()`), a TopAppBar with a "12 episodes · 1.4 GB" summary, per-row size, sort, and bulk/"Delete finished". | high | large |
| **Reversible destructive actions.** Download-delete (`EpisodeRow.kt:214-221`), queue long-press remove (`QueueScreen.kt:142`), and "Clear" (`QueueScreen.kt:115`) all fire instantly. Add Snackbar+Undo for deletes; gate "Clear queue" behind an `AlertDialog`; remove the hidden long-press-to-delete. | high | moderate |
| **Drag-to-reorder the queue.** Four trailing icon buttons (play / up / down / remove, `QueueScreen.kt:174-210`). Adopt a reorderable `LazyColumn` with a `DragHandle` + haptics; keep up/down as accessibility-only custom actions; collapse remove into swipe/overflow. | high | large |
| **Wi-Fi-only download feedback.** A deferred download shows the same spinner as active (`EpisodeRow.kt:213-244`). Give "waiting for Wi-Fi" a distinct cloud-off icon + caption and a one-time "Will download when on Wi-Fi" snackbar. | medium | moderate |
| **Settings TopAppBar + grouped containers.** Raw scrolling `Column`, no app bar (`SettingsScreen.kt:70-84`). Wrap in `Scaffold` + (large) TopAppBar; group sections into rounded `surfaceContainer` cards; cap width on tablets. | medium | moderate |
| **Segmented buttons for Theme.** Replace the dropdown for System/Light/Dark with `SingleChoiceSegmentedButtonRow` (`SettingsScreen.kt:291-312`). | low | moderate |
| **Queue inset handling** — standardize the inner/outer Scaffold padding mix (`QueueScreen.kt:81-86`); ensure the empty-state CTA isn't under the mini-player. | low | quick-win |
| **QueueRow resume progress.** Bespoke `QueueRow` drops the inline progress bar `EpisodeRow` has (`QueueScreen.kt:123-212`). Add the thin progress bar + a stronger now-playing cue (tonal background / equalizer indicator) beyond title tint. | low | moderate |

### Material 3 Expressive, Motion & Theming (cross-cutting)

| Change | Sev | Effort |
|---|---|---|
| **Brand color survives dynamic color.** Dynamic scheme discards `LightColors`/`DarkColors` on Android 12+ (`Theme.kt:70-81`), leaving wallpaper-primary on Material components and chili-red only on `OndesColors` — two competing accents. Build the dynamic scheme then `.copy(primary = ChiliRed, onPrimary = …)`. | high | moderate |
| **IME insets.** Zero `imePadding`/`safeDrawing` anywhere; the keyboard occludes the search field/results (`SearchScreen.kt:57-74`). Add `Modifier.imePadding()` and adopt a consistent inset strategy. | high | quick-win |
| **Large-screen layout** — `NavigationRail` at medium/expanded, adaptive Library grid, capped player artwork (`OndesNavigation.kt:157`, `LibraryScreen.kt:57`, `PlayerScreen.kt:88-94`). *(Consolidates the large-screen findings across reviewers — see Bigger Bets.)* | medium | large |
| **Top app bar consistency.** No `scrollBehavior` / Large/Medium app bars anywhere; three different header treatments across six screens. Give Podcast detail a `LargeTopAppBar` with `exitUntilCollapsedScrollBehavior`; standardize the rest. | medium | moderate |
| **Token discipline sweep.** `PodcastScreen`/`SettingsScreen` hardcode 16/12/8/4dp literals the design system exists to replace. Sweep to `OndesTheme.spacing`; add a lint rule forbidding raw `.dp` in `ui/screens`. | medium | quick-win |
| **Unify the progress bar.** Mini-player and `EpisodeRow` progress bars diverge in color/track (`MiniPlayer.kt:58-68`, `EpisodeRow.kt:128-133`). Extract one `OndesProgressBar`; update the design-system note (wavy indicator no longer used). | low | quick-win |
| **Expressive nav motion.** NavHost uses plain `fadeIn/fadeOut` with default specs, so `MotionScheme.expressive()` never reaches transitions (`OndesNavigation.kt:96-99`). Pass the motion-scheme spec or upgrade to a subtle shared-axis; keep the player slide-up. | low | moderate |

### Accessibility & Ergonomics (cross-cutting)

| Change | Sev | Effort |
|---|---|---|
| **Skip-button label/icon correctness bug.** Skip amounts are user-configurable and honored by playback, but the UI always draws `Replay10`/`Forward30` and announces "Back 10 seconds" (`PlayerScreen.kt:168-201`, `MiniPlayer.kt:107-112`). A user who set 30s sees "10" and TalkBack lies. Drive label/glyph from `skipBackMs`/`skipForwardMs` via a parameterized plural string. | high | moderate |
| **Queue row touch-target crowding** — 4 icon buttons after a weighted title overflow at large font / narrow widths (`QueueScreen.kt:139-211`). Solved by the drag-to-reorder change above; keep arrows as TalkBack-only custom actions. | high | moderate |
| **Expose hidden row actions to TalkBack.** Mark-played / Play-next / Add-to-queue live only behind long-press (`EpisodeRow.kt:79-82,156-203`) — undiscoverable and unreachable for TalkBack. Add `customActions` semantics and/or a visible overflow (MoreVert) button. *(Merges the Home "kebab" finding.)* | medium | moderate |
| **Settings ChoiceRow semantics.** Only the small value `TextButton` is the target; title + control are separate nodes (`SettingsScreen.kt:283-303`). Make the whole row clickable, merge descendants, `stateDescription = current value`, mark active menu item `selected`. | medium | quick-win |
| **Progress bar isn't color-only.** Append "X min left" to metadata (also covers the Home time-remaining win) so the 3dp bar isn't the sole cue (`EpisodeRow.kt:126-134`). | medium | quick-win |
| **Localize duration formatting.** `formatTime`/`formatDurationLabel` use `Locale.US` + literal "h"/"m"/"min" (`Format.kt:15-25`) — won't translate to FR. Move units to string resources. | low | quick-win |
| **Verify brand contrast.** ChiliRed-as-text for current titles/chapters and the amber downloaded tint on warm-paper surfaces sit near AA limits (`Theme.kt`, `EpisodeRow.kt:98`). Audit against actual surfaces in both themes. | low | moderate |
| **Initial player focus.** TalkBack lands on the small Close chevron, not the now-playing title. Add a `focusRequester` to the title heading; add predictive-back/swipe-dismiss (covered above). | low | quick-win |

---

## 4. Quick wins (great first PR)

A flat checklist of low-effort / high-value items pulled from across the report:

- [ ] Add `Modifier.navigationBarsPadding()` to the Player Column so show notes clear the system bar (`PlayerScreen.kt`).
- [ ] Add `Modifier.imePadding()` to the Search column so the keyboard stops covering the field/results (`SearchScreen.kt`).
- [ ] Add the nav-bar window inset to the bottom bar / mini-player (`OndesNavigation.kt`, `MiniPlayer.kt`).
- [ ] Show **"X min left"** on in-progress rows (Home + EpisodeRow); add EN/FR string. Doubles as the color-only-progress a11y fix.
- [ ] Right-timestamp **remaining-time toggle** on the player scrubber.
- [ ] **Library grid** `Fixed(2)` → `Adaptive(minSize = 160.dp)` (one line).
- [ ] **Expandable podcast description** ("Show more / Show less").
- [ ] Merge **Library tile semantics** + "Open {title}" onClick label.
- [ ] Add a **clear "x"** trailing icon to the search field.
- [ ] **Localize / remove** the hardcoded `"🌶️ Ondes v…"` footer; move duration unit labels to string resources.
- [ ] Fix tab **re-select** → scroll-to-top (`animateScrollToItem(0)`).
- [ ] Fix selected-tab check to use `destination.hierarchy.any { }`.
- [ ] **Mini-player second line** with podcast name; verify 48dp targets.
- [ ] Chapters-sheet **active-chapter semantics** + non-color cue.
- [ ] Settings **ChoiceRow** full-row clickable + merged semantics + `stateDescription`.
- [ ] Better Home **empty-state icon** (Podcasts/Add) + leading icon on the CTA.
- [ ] Token sweep: `PodcastScreen`/`SettingsScreen` raw `.dp` → `OndesTheme.spacing`.

---

## 5. Bigger bets (roadmap)

1. **Information architecture + adaptive navigation.** Land the 4-tab restructure (Downloads→Library filter, Settings→top bar), then adopt `NavigationSuiteScaffold`/`NavigationRail` so the same set adapts to medium/expanded widths. This is the single biggest gap versus mature podcast apps and unblocks every other large-screen item.
2. **Full large-screen / foldable story.** WindowSizeClass at the root: adaptive Library grid, capped player artwork + two-pane player (list-detail), Home as carousel-over-grid, capped search/show-notes line length, multi-column theme results. Consolidates the large-screen findings raised by five separate reviewers.
3. **Downloads as a real storage manager.** Add `fileSizeBytes` to the schema, aggregate/per-item sizes, device free space, sort, and multi-select bulk delete. Currently the screen cannot answer "how much is downloaded?"
4. **Library/Detail data model for new-episode awareness.** Unplayed-count DAO projection → badges on tiles + "Unplayed first" sort + per-feed sort/filter persistence. Requires schema and DAO work; high payoff (the at-a-glance "what's waiting" signal).
5. **Player ⇄ Queue integration.** Up-next/queue surface in the player, skip-next/prev transport, configurable skip intervals — backed by new `PlaybackConnection`/`PlayerViewModel` APIs. Closes most of the remaining distance to Pocket Casts/Spotify.
6. **Resume-first Home.** `lastPlayedAt` column + ordering, a distinct hero/resume card, episode-detail sheet on tap (instead of auto-play hijack), skeletons, and a "see all" path for Latest.
7. **Search robustness + discovery depth.** Sealed-result error model end-to-end, debounced as-you-type (or migrate to M3 `SearchBar`), tappable previews, robust URL detection, and a richer Browse-by-theme.

---

## 6. What's already good

Honest credit — Ondes is well above the bar for a sideloaded podcast app:

- **Accessibility hygiene is genuinely strong.** Icon-only buttons carry `contentDescription`; the play/pause control is a single stateful semantics node with `stateDescription` and a correctly null-described inner icon (no double announcement); row artwork is intentionally undescribed while header/player artwork gets `artwork_for`; the decorative mini-player progress is hidden via `clearAndSetSemantics`; loading states use polite live regions; headings are marked; EN/FR strings are at full 122/122 parity.
- **The theming foundation is clean and disciplined** — a brand-token layer (`OndesColors`/`Spacing`/`Shapes`) over `MaterialExpressiveTheme` with `MotionScheme.expressive()`, thoughtful Expressive component adoption (`LoadingIndicator`, `ContainedLoadingIndicator`, the spring-backed AnimatedContent play/pause), and good token discipline in newer screens.
- **The player is tasteful and above-average craft** — large labeled artwork, brand-tinted 80dp play/pause with expressive cross-fade, a scrubber with local scrub-state so the thumb doesn't fight position ticks, and an `HtmlText` that turns mm:ss timestamps in show notes into seek links while keeping feed hyperlinks tappable. The mini→full slide-up with a shared root-scoped `PlayerViewModel` is a nice touch.
- **Home correctly distinguishes loading / empty / content** with an explicit `loading` flag (avoiding the classic "spinner forever on an empty library" trap), and uses `animateItem()` for list mutations.
- **Solid data-management fundamentals** — SAF-based export/import, a shared on-brand `OndesEmptyState` with good first-run copy, properly localized episode-count headers, and accessibility semantics on subscribe/reorder controls.

The recurring theme across this plan: the *intent* is consistently right (refresh flows, error states, expressive motion, brand color are all already built) — much of the highest-impact work is **wiring up plumbing that exists but was dropped**, not net-new construction.