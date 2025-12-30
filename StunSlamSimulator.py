# Minecraft Stun Slam Chance Tester
# Requirements: pynput, matplotlib, numpy
# Install with: pip install -r requirements.txt
# Run: python minecraft_stun_slam_tester.py
# Controls:
#  - Left Click: attack
#  - Key '2': axe keybind
#  - Key 'r': mace keybind
#  - Key 'q': quit

import time
import threading
from pynput import mouse, keyboard
import matplotlib.pyplot as plt
import numpy as np

TICK_MS = 50.0  # 20 TPS

# Store events as (time_ms, label)
events = []
start_time = None
running = True
lock = threading.Lock()

# ---- Stats ----
attempt_count = 0
cumulative_prob = 0.0


def now_ms():
    return time.perf_counter() * 1000.0


def reset():
    global events, start_time
    with lock:
        events = []
        start_time = now_ms()
    print("Reset. Try a stun slam...")


def on_click(x, y, button, pressed):
    if not pressed:
        return
    if button == mouse.Button.left:
        with lock:
            if start_time is None:
                return
            events.append((now_ms() - start_time, 'LC'))


def on_press(key):
    global running
    try:
        k = key.char
    except AttributeError:
        return

    if k == '2':
        with lock:
            if start_time is None:
                return
            events.append((now_ms() - start_time, 'AXE'))
    elif k == 'r':
        with lock:
            if start_time is None:
                return
            events.append((now_ms() - start_time, 'MACE'))
    elif k.lower() == 'q':
        running = False
        return False


# ---- Probability calculation ----

def success_for_phase(phase_ms, evs):
    # phase_ms: offset of tick start relative to start_time in [0, 50)
    # Determine tick index for each event
    ticks = []
    for t, label in evs:
        tick = int(np.floor((t - phase_ms) / TICK_MS))
        ticks.append((tick, t, label))

    # We need:
    # same tick: LC + AXE
    # next tick: LC + MACE
    by_tick = {}
    for tick, t, label in ticks:
        by_tick.setdefault(tick, []).append(label)

    for tick in by_tick:
        cur = by_tick.get(tick, [])
        nxt = by_tick.get(tick + 1, [])
        if ('LC' in cur and 'AXE' in cur and
            'LC' in nxt and 'MACE' in nxt):
            return True
    return False


def calculate_probability(evs, resolution=0.05):
    # resolution in ms for phase sampling
    phases = np.arange(0.0, TICK_MS, resolution)
    good = 0
    for p in phases:
        if success_for_phase(p, evs):
            good += 1
    return good / len(phases) if phases.size else 0.0


# ---- Plotting ----

def plot_events(evs):
    if not evs:
        return
    times = [t for t, _ in evs]
    labels = [l for _, l in evs]

    y_map = {'LC': 2, 'AXE': 1, 'MACE': 0}
    ys = [y_map[l] for l in labels]

    # Add padding of 1–2 ticks before and after inputs
    pre_padding = 2 * TICK_MS
    post_padding = 2 * TICK_MS

    start_t = max(0.0, min(times) - pre_padding)
    end_t = max(times) + post_padding

    plt.figure(figsize=(10, 3))
    plt.scatter(times, ys)
    for t, y, l in zip(times, ys, labels):
        plt.text(t, y + 0.05, l, fontsize=9, ha='center')

    # Draw tick grid
    for x in np.arange(0, end_t + TICK_MS, TICK_MS):
        plt.axvline(x, linestyle='--', alpha=0.3)

    plt.yticks([0, 1, 2], ['MACE', 'AXE', 'LC'])
    plt.xlabel('Time (ms)')
    plt.title('Input Timing (±2 Ticks Context)')
    plt.xlim(start_t, end_t)
    plt.tight_layout()

    # Non-blocking show so reset is instant
    plt.show(block=False)
    plt.pause(1.2)
    plt.close()


# ---- Main ----

def main():
    global start_time, attempt_count, cumulative_prob
    global start_time
    print("Stun Slam Tester running. Press 'q' to quit.")
    reset()

    m_listener = mouse.Listener(on_click=on_click)
    k_listener = keyboard.Listener(on_press=on_press)
    m_listener.start()
    k_listener.start()

    last_event_time = None

    try:
        while running:
            time.sleep(0.05)
            with lock:
                evs = list(events)

            if evs:
                last_event_time = evs[-1][0]

            # If full attempt is detected
            if len(evs) >= 4:
                prob = calculate_probability(evs)

                # Update running average
                attempt_count += 1
                cumulative_prob += prob
                avg_prob = cumulative_prob / attempt_count

                print(f"Attempt {attempt_count}")
                print(f"Current success chance: {prob*100:.2f}%")
                print(f"Average success chance: {avg_prob*100:.2f}%")

                plot_events(evs)
                reset()
                last_event_time = None

            # If inputs stopped for 3 ticks (incomplete attempt)
            elif last_event_time is not None:
                idle_time = (now_ms() - start_time) - last_event_time
                if idle_time >= 3 * TICK_MS:
                    # Count failed attempt with 0% success chance
                    attempt_count += 1
                    cumulative_prob += 0.0
                    avg_prob = cumulative_prob / attempt_count

                    print(f"Attempt {attempt_count}")
                    print("Incomplete stun slam attempt")
                    print("Current success chance: 0.00%")
                    print(f"Average success chance: {avg_prob*100:.2f}%")

                    plot_events(evs)
                    reset()
                    last_event_time = None

    finally:
        m_listener.stop()
        k_listener.stop()


if __name__ == '__main__':
    main()
