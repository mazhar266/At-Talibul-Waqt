"""Generates shared/src/commonMain/composeResources/files/adhan_test.wav.

The clip is a synthesised tone sequence, not a recording. It reproduces the Web Audio graph in
NoorSalat's ``playAdhanSample()`` (salat.mazhar.fi) as closely as a fixed PCM file can: the same
nine notes, each a band-limited sawtooth plus a sine one octave up, through an 850 Hz low-pass,
shaped by a 0.15 s linear attack and an exponential decay. So the mobile app's "Adhan test"
sounds like the web app's, and the file carries no third-party licence: it is generated here and
belongs to this project like the rest of the source.

Run from the repository root with any Python 3 (no third-party packages):

    python tools/make_adhan_clip.py
"""

import math
import pathlib
import struct
import sys

SAMPLE_RATE = 22_050
OUTPUT = pathlib.Path(__file__).resolve().parents[1] / "shared/src/commonMain/composeResources/files/adhan_test.wav"

# (frequency in Hz, sounding duration in s, silence after it in s): copied verbatim from the web app.
NOTES = [
    (293.66, 0.6, 0.1),
    (329.63, 0.7, 0.1),
    (349.23, 1.2, 0.3),
    (440.00, 1.4, 0.4),
    (392.00, 1.8, 0.8),
    (293.66, 0.6, 0.1),
    (349.23, 0.8, 0.1),
    (440.00, 1.5, 0.4),
    (392.00, 2.2, 0.5),
]
FLOOR_GAIN = 0.001     # gain.setValueAtTime(.001, start)
PEAK_GAIN = 0.25       # gain.linearRampToValueAtTime(.25, start + .15)
ATTACK = 0.15
OSCILLATOR_TAIL = 0.1  # oscillators stop 0.1 s after the envelope has decayed, like the web app
LOWPASS_HZ = 850.0
LOWPASS_Q = 10 ** (1 / 20)  # Web Audio's default Q of 1 dB for a low-pass BiquadFilter
TARGET_PEAK = 0.79     # about -2 dBFS; the Web Audio graph peaks near -6 dBFS, which is quiet on a phone


def gain_at(t, duration):
    """Envelope value at `t` seconds into a note (Web Audio linear then exponential ramps)."""
    if t < ATTACK:
        return FLOOR_GAIN + (PEAK_GAIN - FLOOR_GAIN) * (t / ATTACK)
    if t >= duration:
        return FLOOR_GAIN
    return PEAK_GAIN * (FLOOR_GAIN / PEAK_GAIN) ** ((t - ATTACK) / (duration - ATTACK))


def lowpass_coefficients():
    """RBJ cookbook low-pass, the same form Web Audio's BiquadFilterNode uses."""
    w0 = 2 * math.pi * LOWPASS_HZ / SAMPLE_RATE
    alpha = math.sin(w0) / (2 * LOWPASS_Q)
    cos_w0 = math.cos(w0)
    a0 = 1 + alpha
    return (
        (1 - cos_w0) / 2 / a0,
        (1 - cos_w0) / a0,
        (1 - cos_w0) / 2 / a0,
        -2 * cos_w0 / a0,
        (1 - alpha) / a0,
    )


def render_note(frequency, duration):
    """Samples for one note: saw(f) + sin(2f) -> low-pass -> envelope."""
    b0, b1, b2, a1, a2 = lowpass_coefficients()
    harmonics = int((SAMPLE_RATE / 2) // frequency)  # band-limit the sawtooth at Nyquist
    total = int((duration + OSCILLATOR_TAIL) * SAMPLE_RATE)
    out = [0.0] * total
    x1 = x2 = y1 = y2 = 0.0
    two_pi_f = 2 * math.pi * frequency
    for n in range(total):
        t = n / SAMPLE_RATE
        phase = two_pi_f * t
        saw = (2 / math.pi) * sum(((-1) ** (k + 1)) * math.sin(k * phase) / k for k in range(1, harmonics + 1))
        x = saw + math.sin(2 * phase)
        y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2, x1, y2, y1 = x1, x, y1, y
        out[n] = y * gain_at(t, duration)
    return out


def render_clip():
    length = sum(d + p for _, d, p in NOTES) + OSCILLATOR_TAIL
    mix = [0.0] * int(math.ceil(length * SAMPLE_RATE))
    start = 0.0
    for frequency, duration, pause in NOTES:
        offset = int(start * SAMPLE_RATE)
        for i, sample in enumerate(render_note(frequency, duration)):
            mix[offset + i] += sample
        start += duration + pause
    peak = max(abs(s) for s in mix)
    scale = TARGET_PEAK / peak if peak > 0 else 1.0
    return [s * scale for s in mix], peak


def write_wav(path, samples):
    data = b"".join(struct.pack("<h", int(max(-1.0, min(1.0, s)) * 32767)) for s in samples)
    byte_rate = SAMPLE_RATE * 2
    header = struct.pack(
        "<4sI4s4sIHHIIHH4sI",
        b"RIFF", 36 + len(data), b"WAVE",
        b"fmt ", 16, 1, 1, SAMPLE_RATE, byte_rate, 2, 16,
        b"data", len(data),
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(header + data)


def main():
    target = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else OUTPUT
    samples, raw_peak = render_clip()
    write_wav(target, samples)
    print(f"wrote {target} ({len(samples) / SAMPLE_RATE:.2f} s, {target.stat().st_size} bytes, "
          f"raw peak {raw_peak:.3f} scaled to {TARGET_PEAK})")


if __name__ == "__main__":
    main()
