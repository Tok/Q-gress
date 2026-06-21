# RELEASE.md — pushing the 3D rewrite & preserving the 2D version

Status: **draft for review — nothing pushed yet.** Goal: get the 3D rewrite onto GitHub + GitHub
Pages **without losing the original 2D site**, and set up CI (tests + coverage + deploy).

## Where things stand

- Remote: `https://github.com/Tok/Q-gress` (the live site is `https://tok.github.io/Q-gress/`). The
  repo is **fully active / pushable** — the "Arctic Code Vault" badge just means the (2D) code was
  captured in GitHub's **2020 Archive Program** snapshot (a nice bonus: the 2D version is preserved in
  the vault forever); it is **not** a read-only archived state. Untouched for ~7 years otherwise.
- **`master`** = the original **2D** build (~2018). The 2D site is served by **GitHub Pages from the
  `master` branch root** (`index.html` + `published/*.js` + `mochawesome.html`). There is **no
  `gh-pages` branch**. The 2D `index.html` **inlines an old Mapbox `pk.` token** (a *publishable*
  client token, public for years — not a fresh secret leak, but see the decision below).
- **`develop`** = the **3D** rewrite — **131 commits ahead** of master, gate-green, never pushed. It
  uses **keyless** map tiles (OpenFreeMap + Esri) — **no token anywhere** (verified).
- No CI yet. Version is `1.0-SNAPSHOT` (the 3D build now embeds a timestamp + git-sha — see below).

The 2D build depends on dead tooling and is **not rebuildable**, so we don't migrate it — we preserve
its **source** on an archive branch.

## Decision: what to do with the old 2D Mapbox token

The 2D inlines a Mapbox publishable token. It has been exposed in the live site's source for ~7 years,
so archiving the source changes nothing — **but** keeping the 2D *deployed* keeps a live site calling
Mapbox with that token. Recommended:

1. **Revoke / rotate that token in the Mapbox account** (the real fix — then it doesn't matter that
   it's in old code/history).
2. **Decision (chosen): 3D at root, 2D preserved.** The **3D becomes the primary site at the root**.
   The 2D is preserved regardless: its **source** on `archive/q-gress-2D` (+ a tag, + it's already in
   the Arctic vault) — and, **optionally**, its **built site served at `/2D/`** so the old thing keeps
   running. Revoke the old Mapbox token either way; if you do serve `/2D/`, its map will then stop
   loading (fine for a frozen legacy) — or leave the (already-public-for-years) token in place.

## Target end state

- `main` (renamed from / replacing `master`) = the **3D** rewrite. The old 2D source lives on
  **`archive/q-gress-2D`** (tagged `v1.0-2d`).
- GitHub Pages serves from a **CI-managed `gh-pages` branch**:
  - `/` (root) — the **3D** app (auto-deployed by CI on every push to `main`); `/#demo` = the sandbox.
  - `/2D/` — _(optional)_ the preserved 2D site, seeded once and left untouched by CI (`keep_files`).

## Sequence (do in order)

**1. Push the 3D work.** `git push origin develop` (publishes the branch; nothing live changes yet).

**2. Archive the 2D source (always).**
- `git branch archive/q-gress-2D master && git push origin archive/q-gress-2D`
- `git tag v1.0-2d master && git push origin v1.0-2d`

**3. Promote 3D to the main branch.** Rename `develop`→`main` (or merge develop→master, then rename
master→main) and make it the default branch — the 2D source is already safe on the archive branch.

**4. GitHub Pages from `gh-pages` (manual GitHub steps — only you can do these):**
- **Settings → Actions → General → Workflow permissions = Read and write** (so CI can push gh-pages).
- Push to `main` → CI builds + deploys the 3D bundle to the **`gh-pages` root**.
- **Settings → Pages → Source = `gh-pages` branch, `/ (root)`.**
- Verify `tok.github.io/Q-gress/` = the 3D app.

**5. (Optional) Also serve the old 2D at `/2D/`.** On `gh-pages`, add a `2D/` directory with the old
2D files (`master:index.html`, `published/`, `favicon.png`, `mochawesome.html`) and push; CI's
`keep_files: true` leaves it alone on future 3D deploys. _Skip this to fully retire the 2D — the
source archive (+ Arctic vault) is enough._

**6. Revoke the old Mapbox token** in the Mapbox account (the 3D needs none — it's keyless).

## CI (`.github/workflows/ci.yml`)

- **On push / PR (any branch):** JDK 21 → `ktlintCheck detekt jsNodeTest` (the same gate as the
  pre-commit hook) + a best-effort **Codecov** upload (see caveat).
- **On push to `main`:** also `jsBrowserDistribution`, then deploy `build/dist/js/productionExecutable`
  to the **`gh-pages` root** (via `peaceiris/actions-gh-pages` with `keep_files: true`, so an optional
  `/2D/` survives). No manual secrets needed (`GITHUB_TOKEN` + write permission). Codecov is
  token-less for public repos; add a `CODECOV_TOKEN` secret only if uploads get rate-limited.

**Coverage caveat:** Kover has **no Kotlin/JS support**. Real line-coverage arrives with the
functional-core split (extract pure logic to `commonMain` + a `jvm()` test target, run Kover there) —
see `PLAN.md`. Until then the Codecov job runs but reports little; the badge is mostly a build signal.

## Versioning

The app embeds a **build timestamp + git short-sha** (generated at build time into `config/BuildInfo`)
and shows it in-app, so any deployed build is identifiable. Tag releases as `v<major>.<minor>` (the
2D archive is `v1.0-2d`; the first 3D release should be `v3.0.0` or similar).

## Risks / notes

- **Sub-path asset paths:** the 3D bundle must load its assets with **relative** paths so it works
  under `/3D/`. The app already references `models/…`, `fonts/…`, `stylesheet/…`, `Q-Gress.js`
  relatively; confirm webpack's `publicPath` is relative (`""`/`auto`) before the first deploy, or set
  it. CDN deps (MapLibre, uPlot) are absolute and fine.
- **Mobile:** blocked already (`Controls.isUnsupported` — no WebGL or touch-only). Desktop-only.
