# Adhan recordings bundled with TalibulWaqt

Every recording here carries an explicit redistributable licence from its source page, checked
file by file (not per collection) on 2026-10-16. Recordings by well-known muadhdhins that are
circulated without a licence (Mishary Rashid Alafasy, Abdul Basit Abdus Samad, Ali Ahmed Mulla,
Nasser Al Qatami and others) were deliberately **not** included; a Commons file attributed to Sabah
Fakhri was skipped because its `PD-Old` / `PD-US-expired` tags cannot apply to a 1985 recording.

The ids in the first column are what `UserSettings.adhanSound` stores (see `AdhanSounds.kt`).

| id | file | Muadhdhin / recording | Place, occasion, style | Length | Licence | Source |
|----|------|-----------------------|------------------------|--------|---------|--------|
| `makkah_haram` | `makkah_haram.mp3` | Muadhdhin of the Great Mosque (not named by the source) | Masjid al-Haram, Makkah, 21 Jan 2013 · Hijazi style | 3:17 | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/) — attribution: Seyfula Islam | [Wikimedia Commons: Adhan, Great Mosque of Mecca - Jan 21, 2013.webm](https://commons.wikimedia.org/wiki/File:Adhan,_Great_Mosque_of_Mecca_-_Jan_21,_2013.webm) (YouTube CC-BY import, licence reviewed on Commons 2018-07-19) |
| `madinah_nabawi` | `madinah_nabawi.mp3` | Muadhdhin of the Prophet's Mosque (not named by the source) | Masjid an-Nabawi, Madinah · Hijazi style | 3:06 | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/) — attribution: ejaz215 | [Wikimedia Commons: 33937 ejaz215 call-to-prayer-from-the-prophet-s-mo.ogg](https://commons.wikimedia.org/wiki/File:33937_ejaz215_call-to-prayer-from-the-prophet-s-mo.ogg), originally [Freesound #33937](http://www.freesound.org/people/ejaz215/sounds/33937/) |
| `malmo_besim_azemi` | `malmo_besim_azemi.mp3` | **Besim Azemi** (named in the source title) | Malmö Mosque (Islamic Center Malmö), Eid al-Fitr Fajr adhan, 19 Aug 2012 · Balkan/Albanian style. Note: a Fajr adhan, so it includes *as-salatu khayrun min an-nawm* | 4:05 | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/) — attribution: Islamic Center Malmö | [Wikimedia Commons: Eid al-Fitr Fajr azan at Malmö Mosque - 19 August 2012.webm](https://commons.wikimedia.org/wiki/File:Eid_al-Fitr_Fajr_azan_at_Malm%C3%B6_Mosque_-_19_August_2012.webm) (YouTube CC-BY import, reviewed 2020-02-12) |
| `doha_qatar` | `doha_qatar.mp3` | Muadhdhin not named by the source | Doha, Qatar, Dhuhr adhan, recorded 2013–2014 · Gulf style | 2:59 | [Public Domain Mark 1.0](https://creativecommons.org/publicdomain/mark/1.0/) set by the uploader on their own recordings | [Internet Archive: adhan.recordings.from.doha.qatar](https://archive.org/details/adhan.recordings.from.doha.qatar), file `Adhan_Doha_Qatar_02_Dhuhr_Adhan.mp3` |
| `istanbul` | `istanbul.mp3` | Muadhdhin not named by the source | Istanbul, Türkiye, 5 Sep 2025 · Turkish style; the source is a 50 s opening excerpt | 0:50 | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) — attribution (optional): Viceskeeni2 | [Wikimedia Commons: Adhan in Istanbul.webm](https://commons.wikimedia.org/wiki/File:Adhan_in_Istanbul.webm) |
| `adam_synagda` | `adam_synagda.mp3` | **Adam-synagda** (uploader's own recitation) | Studio recording of the full adhan, 29 Apr 2022 | 2:34 | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) — attribution (optional): Adam-synagda | [Wikimedia Commons: Beautiful adhan.ogg](https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg) |
| `tone` | `tone.wav` | — | Synthesised nine-note sequence reproducing the Web Audio graph of NoorSalat's `playAdhanSample()` | 0:14 | Generated in this project by `tools/make_adhan_clip.py`; no third-party rights | — |
| `beep` | (none) | — | The device's default notification sound; no file of ours is played | — | — | — |

Bundled audio: 6.8 MB of MP3 plus the 0.6 MB tone.

## Processing

Each recording was converted with ffmpeg 7.1 from the source container (WebM/Opus or Vorbis video
audio, Ogg Vorbis, or MP3) to **mono, 22.05 kHz, 56 kbps CBR MP3** so that it plays on Android
(MediaPlayer), iOS (AVAudioPlayer) and desktop (javax.sound with the mp3spi decoder) alike:

    ffmpeg -i SOURCE -vn -ac 1 -ar 22050 \
      -af "loudnorm=I=-16:TP=-1.5:LRA=11,afade=t=in:st=0:d=0.3" \
      -c:a libmp3lame -b:a 56k OUT.mp3

Loudness was normalised to −16 LUFS so the field recordings and the studio one sit at the same
level. Trims: Madinah starts at 0.5 s, Malmö at 3 s (silence before the adhan); Istanbul is cut at
49.5 s with a 2.5 s fade because the source itself ends there. No other edits were made.

## iOS notification clips

iOS limits notification sounds to 30 seconds in the app bundle, so `iosApp/iosApp/Sounds/` holds the
first 30 s of each recording (2.5 s fade-out) as IMA4-compressed CAF, under the names listed in
`AdhanSound.iosNotificationClip`. The same licences and attributions apply to those excerpts.
