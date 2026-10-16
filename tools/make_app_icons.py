"""Derive every platform's launcher icon from icons/at_talibul_waqt_icon_1024.png.

The master artwork lives in icons/ (SVG plus 1024/512 PNG exports with transparent rounded
corners). This script writes the raster variants the three hosts need; the Android adaptive-icon
layers are hand-converted VectorDrawables under androidApp/src/main/res/drawable and are not
touched here.

    python tools/make_app_icons.py

Needs Pillow only. Outputs:
  androidApp/src/main/res/mipmap-*/ic_launcher.png        legacy (API 24/25) icon, rounded PNG as is
  androidApp/src/main/res/mipmap-*/ic_launcher_round.png  circular crop of the squared icon
  iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png  opaque square (iOS masks it)
  desktopApp/icons/talibulwaqt.{ico,icns,png}             installer icons per OS
  desktopApp/src/main/resources/talibulwaqt_icon.png      window / taskbar icon
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "icons" / "at_talibul_waqt_icon_1024.png"

# The SVG background: a top-left to bottom-right linear gradient with these stops.
GRADIENT_STOPS = [(0.0, (0x12, 0x5F, 0x53)), (0.52, (0x09, 0x4A, 0x43)), (1.0, (0x06, 0x2D, 0x31))]

ANDROID_DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def gradient_colour(t):
    for (t0, c0), (t1, c1) in zip(GRADIENT_STOPS, GRADIENT_STOPS[1:]):
        if t <= t1:
            f = 0.0 if t1 == t0 else (t - t0) / (t1 - t0)
            return tuple(round(a + (b - a) * f) for a, b in zip(c0, c1))
    return GRADIENT_STOPS[-1][1]


def gradient_square(size):
    """The SVG's background gradient filled to the corners, without the rounded mask."""
    lookup = [gradient_colour(i / (2 * (size - 1))) for i in range(2 * size - 1)]
    rows = bytearray()
    for y in range(size):
        for x in range(size):
            rows.extend(lookup[x + y])
    return Image.frombytes("RGB", (size, size), bytes(rows))


def squared(source):
    """The icon with its transparent corners filled by the same gradient: seamless, opaque."""
    base = gradient_square(source.width).convert("RGBA")
    return Image.alpha_composite(base, source)


def circular(image, size, supersample=4):
    big = size * supersample
    mask = Image.new("L", (big, big), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, big - 1, big - 1), fill=255)
    out = image.resize((big, big), Image.LANCZOS)
    out.putalpha(mask)
    return out.resize((size, size), Image.LANCZOS)


def fit(image, size):
    return image.resize((size, size), Image.LANCZOS)


def save(image, path, **kwargs):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, **kwargs)
    print(f"wrote {path.relative_to(ROOT)}")


def main():
    source = Image.open(SOURCE).convert("RGBA")
    square = squared(source)

    res = ROOT / "androidApp" / "src" / "main" / "res"
    for density, size in ANDROID_DENSITIES.items():
        save(fit(source, size), res / f"mipmap-{density}" / "ic_launcher.png", optimize=True)
        save(circular(square, size), res / f"mipmap-{density}" / "ic_launcher_round.png", optimize=True)

    save(square.convert("RGB"), ROOT / "iosApp" / "iosApp" / "Assets.xcassets" / "AppIcon.appiconset" / "app-icon-1024.png", optimize=True)

    desktop_icons = ROOT / "desktopApp" / "icons"
    save(fit(source, 512), desktop_icons / "talibulwaqt.png", optimize=True)
    save(fit(source, 256), desktop_icons / "talibulwaqt.ico", sizes=[(s, s) for s in (16, 24, 32, 48, 64, 128, 256)])
    save(source, desktop_icons / "talibulwaqt.icns")
    save(fit(source, 256), ROOT / "desktopApp" / "src" / "main" / "resources" / "talibulwaqt_icon.png", optimize=True)


if __name__ == "__main__":
    main()
