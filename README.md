<div align="center">

  <img src="https://github.com/rukamori/ArchiveTune/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="160" height="160" alt="ArchiveTune S" style="border-radius: 22%">

  <h1>ArchiveTune S</h1>

  <p align="center">
    <strong>A personal fork of ArchiveTune.</strong>
    <br />
    <em>Not an official release. Maintained privately. No support offered.</em>
  </p>

</div>

<hr />

> [!WARNING]
> ## This is an unofficial fork
>
> **ArchiveTune S** is a personal fork of **[ArchiveTune](https://github.com/rukamori/ArchiveTune)** by Rukamori.
>
> It is **not** an official ArchiveTune release, **not** affiliated with the upstream project, and **not** supported by Rukamori or the ArchiveTune contributors. All modifications, regressions, and risks encountered with this fork are entirely the responsibility of the fork's maintainer.
>
> If you want the maintained, supported, official app:
>
> ### → [**github.com/rukamori/ArchiveTune**](https://github.com/rukamori/ArchiveTune)
>
> This fork exists for the personal use of its maintainer and is published openly for transparency and for anyone who finds the changes useful.

---

## What this fork changes

Compared to upstream ArchiveTune `15.1.1`, this fork adds:

### Playback
- Reworked manual crossfade handoff: the primary player stays audible until the secondary is fully buffered, eliminating the silent gap during track switches.
- Equal-power handoff ramp extended to 200 ms (up from 96 ms) so the AudioTrack has time to warm on slower devices.
- Seek on downloaded tracks no longer rebuilds audio effects on every seek; four AudioEffect instances were being torn down and recreated per seek, causing buffering even on cached songs.
- `SeekParameters.CLOSEST_SYNC` for tap-to-word navigation, dramatically reducing the post-seek buffering that EXACT mode produced.
- Normalization factor and cast MIME lookups moved off the data-source open path so Room queries no longer block the loader thread.
- Primary player buffer raised to 300 s so a completed seek into an already-playing downloaded song hits the existing buffer instead of triggering a refill from disk.

### Lyrics performance
- V2 and Enhanced lyrics stop polling while the app is backgrounded.
- Word-sync position updates are frame-aligned (`withFrameNanos`) instead of a 16 ms timer.
- Only the active lyric line receives a live position feed; past and future lines render with sentinel values so they don't recompose at 60 Hz.
- Manual-scroll timeout no longer recomposes the list per gesture pixel.
- Dynamic theme extraction is keyed on `mediaId` only, so liked / in-library mutations don't retrigger palette extraction.

### Lyrics customization
- Letter-by-letter animation mode for both V2 and Enhanced renderers.
- Contrast guard with tunable WCAG threshold and blend strength.
- Custom text colour mode with hex input.
- Translation size, line-height, opacity, and italic toggles.
- Accompaniment / phonetic scale sliders.
- Viewport offset, keep-alive zone, and selection limit sliders.

### Display
- Force high refresh rate preference now actually gates the 120 Hz request instead of applying it unconditionally.

---

## What this fork does not provide

- No support, no issue triage, no community channels.
- No automatic merges from upstream.
- The updater checks this fork's releases, not upstream's.

---

## Getting the app

Build from source, or download the latest release from this repository's [Releases page](https://github.com/7assrnrns-cmd/ArchiveTune_S/releases).

The application id is `moe.rukamori.archivetune.s`, so it installs alongside the official ArchiveTune without conflict.

---

## Credits

The entire foundation of this app belongs to the ArchiveTune project. This fork would not exist without:

- **ArchiveTune** by [Rukamori](https://github.com/rukamori) — [github.com/rukamori/ArchiveTune](https://github.com/rukamori/ArchiveTune)
- **Metrolist** by [Mostafa Alagamy](https://github.com/mostafaalagamy/Metrolist) — base framework
- **SimpMusic** by [maxrave-dev](https://github.com/maxrave-dev/SimpMusic) — lyrics API provider
- [BetterLyrics](https://better-lyrics.boidu.dev/) — word-by-word lyrics, unison, and artwork provider support
- [Material Color Utilities](https://github.com/material-foundation/material-color-utilities)
- [Read You](https://github.com/Ashinch/ReadYou) and [Seal](https://github.com/JunkFood02/Seal) — UI component inspiration
- All translators, beta testers, and contributors to the original project

---

## License

This project is distributed under the **GNU General Public License v3.0 (GPLv3)**, in compliance with the upstream license. See the [`LICENSE`](LICENSE) file for the full terms.

In accordance with GPLv3:

- This is a modified version of ArchiveTune and is clearly identified as such.
- The complete corresponding source code of this fork is available in this repository.
- All copyright notices from the original work are preserved.

### Trademark notice

The **ArchiveTune™** name, logo, application icon, and official branding are **not** covered by the GPLv3 and remain the property of Rukamori and the ArchiveTune project. This fork does not claim any of these marks, does not represent itself as an official release, and does not imply endorsement by the upstream maintainers. Any use of the original branding in this repository is for identification purposes only.

---

## Legal

- Not affiliated with Google LLC, YouTube, or any of their subsidiaries.
- Does not bypass YouTube's technical protections.
- Users are encouraged to support artists through official channels.

---

<div align="center">
  <p><b>This is a personal fork. If you want the real thing, use <a href="https://github.com/rukamori/ArchiveTune">the upstream ArchiveTune</a>.</b></p>
</div>
