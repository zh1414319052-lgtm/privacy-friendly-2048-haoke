"""Render original 2048 sound effects. Python standard library only."""
import math
import random
import struct
import wave
from pathlib import Path

RATE = 44_100
TAU = 2 * math.pi
DESTINATION = Path(__file__).resolve().parent
rng = random.Random(2048)

def tone(frequency, duration, decay, amplitude, partials=(), bend=0.0):
    samples = []
    count = round(duration * RATE)
    for i in range(count):
        t = i / RATE
        phase = TAU * frequency * (t + bend * t * t / (2 * duration))
        onset = min(1.0, t / 0.003)
        tail = min(1.0, (count - 1 - i) / (RATE * 0.012))
        value = math.sin(phase)
        value += sum(weight * math.sin(phase * ratio) * math.exp(-t * 22)
                     for ratio, weight in partials)
        samples.append(value * amplitude * onset * tail * math.exp(-t * decay))
    return samples

def brush(duration, amplitude, smoothing):
    out = []
    prev = 0.0
    low = 0.0
    n = round(duration * RATE)
    for i in range(n):
        progress = i / max(1, n - 1)
        prev += (rng.uniform(-1.0, 1.0) - prev) * smoothing
        low += (prev - low) * 0.017
        out.append((prev - low) * math.sin(math.pi * progress) ** 1.45
                   * (1.0 - 0.5 * progress) * amplitude)
    return out

def combine(duration, tracks):
    out = [0.0] * round(duration * RATE)
    for offset, waveform in tracks:
        start = round(offset * RATE)
        for i, sample in enumerate(waveform[:len(out) - start]):
            out[start + i] += sample
    return out

def ceramic(freq, duration, decay, amp, bend=0.0):
    return tone(freq, duration, decay, amp, [(2.60, .26), (3.92, .12)], bend)

def render():
    return {
        "ui-select": combine(.073, [
            (0, ceramic(880, .071, 52, .145)),
            (0, brush(.024, .075, .20))]),
        "tile-slide": combine(.108, [
            (0, brush(.092, .285, .073)),
            (.045, tone(330, .062, 34, .087, [(2.13, .13)], -.17))]),
        "tile-merge": combine(.178, [
            (0, ceramic(587.33, .166, 20, .187)),
            (.014, tone(880, .148, 30, .062, [(1.99, .13)])),
            (0, brush(.035, .087, .25))]),
        "stone-contact": combine(.110, [
            (0, tone(222, .101, 35, .182, [(2.77, .25), (4.45, .15)])),
            (0, brush(.032, .15, .22))]),
        "undo": combine(.196, [
            (0, tone(700, .129, 22, .094, [(2.01, .16)], -.22)),
            (.038, ceramic(392, .152, 21, .155, -.14))]),
        "puzzle-clear": combine(.548, [
            (0, ceramic(587.33, .273, 11, .162)),
            (.090, ceramic(739.99, .281, 10, .149)),
            (.182, ceramic(880.00, .354, 8, .156)),
            (.201, tone(1174.66, .322, 11, .055))]),
        "out-of-moves": combine(.366, [
            (0, tone(349.23, .167, 20, .111, [(1.98, .10)])),
            (.092, tone(261.63, .268, 14, .124, [(1.99, .15)], -.035))]),
    }

def write(path, samples):
    with wave.open(str(path), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(b"".join(struct.pack("<h", round(max(-1., min(1., x)) * 32767))
                                  for x in samples))

if __name__ == "__main__":
    cues = render()
    for name, samples in cues.items():
        write(DESTINATION / (name + ".wav"), samples)
    timeline = [(0.20, "ui-select"), (.63, "tile-slide"), (1.02, "tile-merge"),
                (1.53, "stone-contact"), (1.99, "undo"), (2.62, "puzzle-clear"),
                (3.67, "out-of-moves")]
    write(DESTINATION / "ceramic-preview.wav", combine(4.27, [(at, cues[name]) for at, name in timeline]))
