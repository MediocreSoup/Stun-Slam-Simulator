# Minecraft Stun Slam Chance Tester
#
# Requirements: pynput, matplotlib, numpy
# Install: pip install -r requirements.txt
# Run: python minecraft_stun_slam_tester.py
#
# Features:
#  - Tick-based probability calculation for Minecraft stun slams
#  - Global keyboard + mouse input capture
#  - Automatic success chance calculation per attempt
#  - Running average success rate (failed attempts included)
#  - Timeline plot with ±2 tick context
#  - Incomplete attempts handled after 3 ticks of inactivity
#  - Interactive hotkey rebinding (keyboard + mouse)
#  - Persistent hotkeys via config.json
#  - Backspace to rebind at any time

import time
import threading
import json
import os
from typing import TypedDict
from pynput import mouse, keyboard
import matplotlib.pyplot as plt
import numpy as np

# ================== CONFIG ==================
CONFIG_FILE = "config.json"
TICK_MS = 50.0  # 20 TPS
# ============================================

# ---- Input state ----
AXE_BIND: Bind | None = None
MACE_BIND: Bind | None = None
ATTACK_BIND: Bind | None = None

config_mode = False
config_step = 0  # 0=axe, 1=mace, 2=attack

# ---- Runtime state ----
events = []
start_time = None
running = True
lock = threading.Lock()

# ---- Stats ----
attempt_count = 0
cumulative_prob = 0.0

# ---- Mouse maps ----
MOUSE_NAME_MAP = {
    "left": mouse.Button.left,
    "right": mouse.Button.right,
    "middle": mouse.Button.middle,
    "x1": mouse.Button.x1,
    "x2": mouse.Button.x2,
}
MOUSE_NAME_MAP_INV = {v: k for k, v in MOUSE_NAME_MAP.items()}

# ---- Types ------
class Bind:
    def __init__(self, type: str, value: str | mouse.Button):
        self.type = type          # "keyboard" | "mouse"
        self.value = value        # str | mouse.Button

    def __str__(self):
        if self.type == "keyboard":
            return f"Key '{self.value}'"
        if self.type == "mouse":
            return f"Mouse '{MOUSE_NAME_MAP_INV[self.value]}'"
        return "<unbound>"

    def matches_key(self, key):
        if self.type != "keyboard":
            return False
        try:
            return key.char and key.char.lower() == self.value
        except AttributeError:
            return key.name == self.value

    def matches_mouse(self, button):
        return self.type == "mouse" and self.value == button

    def to_json(self):
        if self.type == "keyboard":
            return {"type": "keyboard", "value": self.value}
        return {"type": "mouse", "value": MOUSE_NAME_MAP_INV[self.value]}
        

def now_ms():
    return time.perf_counter() * 1000.0


# ---- Config load/save ----

def load_config() -> dict[str, Bind] | None:
    if not os.path.exists(CONFIG_FILE):
        return None
    try:
        with open(CONFIG_FILE, "r") as f:
            data = json.load(f)

        def parse(entry):
            if entry["type"] == "keyboard":
                return Bind("keyboard", entry["value"])
            return Bind("mouse", MOUSE_NAME_MAP[entry["value"]])

        return {
            "axe": parse(data["axe"]),
            "mace": parse(data["mace"]),
            "attack": parse(data["attack"]),
        }
    except Exception as e:
        print(f"Failed to load config.json: {e}")
        return None
    
def capture_bind(prompt: str) -> Bind:
    print(prompt)
    result = {"bind": None} # To get around typing checks ig? What did brochacho GPT even cook up?
    done = threading.Event()

    def on_press(key):
        if key == keyboard.Key.esc:
            print("\nExiting.")
            os._exit(0)
            
        # Do NOT allow backspace or esc to be bound
        if key in (keyboard.Key.backspace, keyboard.Key.esc):
            print("That key cannot be bound. Try again.")
            return
            
        try:
            result["bind"] = Bind("keyboard", key.char.lower())
        except AttributeError:
            result["bind"] = Bind("keyboard", key.name)
        done.set()
        return False

    def on_click(x, y, button, pressed):
        if pressed:
            result["bind"] = Bind("mouse", button)
            done.set()
            return False

    kl = keyboard.Listener(on_press=on_press)
    ml = mouse.Listener(on_click=on_click)
    kl.start()
    ml.start()

    done.wait()

    kl.stop()
    ml.stop()
    
    print(f"Bind set to {result["bind"]}")

    return result["bind"]


def load_binds() -> dict[str, Bind]:
    binds = load_config()

    if binds is None:
        print("No keybinds found. Entering config mode.\n")
        binds = {
            "axe": capture_bind("Press your AXE key or mouse button:"),
            "mace": capture_bind("Press your MACE key or mouse button:"),
            "attack": capture_bind("Press your ATTACK key or mouse button:"),
        }
        save_config(binds)
    else:
        print("Loaded keybinds:")
        for k, v in binds.items():
            print(f" {k.capitalize():6}: {v}")
        print("Press BACKSPACE at startup to rebind.\n")

    return binds



def save_config(binds: dict[str, Bind]):
    data = {
        k: v.to_json()
        for k, v in binds.items()
    }

    with open(CONFIG_FILE, "w") as f:
        json.dump(data, f, indent=2)

    print("=== CONFIG SAVED ===")
    for k, v in binds.items():
        print(f" {k.capitalize():6}: {v}")
    print()


# ---- Reset ----

def reset():
    global events, start_time
    with lock:
        events = []
        start_time = now_ms()
    print("Reset. Try a stun slam... \n")


# ---- Input listeners ----

def on_click(x, y, button, pressed):
    if not pressed or start_time is None:
        return

    if AXE_BIND.matches_mouse(button): # type: ignore
        events.append((now_ms() - start_time, "AXE"))
    if MACE_BIND.matches_mouse(button): # type: ignore
        events.append((now_ms() - start_time, "MACE"))
    if ATTACK_BIND.matches_mouse(button): # type: ignore
        events.append((now_ms() - start_time, "ATTACK"))


def on_press(key):
    global running
    global AXE_BIND, MACE_BIND, ATTACK_BIND

    if key == keyboard.Key.esc: # Hardcoded quit function
        running = False
        return False
    
    if attempt_count == 0 and key == keyboard.Key.backspace:
        print("\n=== REBINDING HOTKEYS ===")
        binds = {
            "axe": capture_bind("Press your AXE key or mouse button:"),
            "mace": capture_bind("Press your MACE key or mouse button:"),
            "attack": capture_bind("Press your ATTACK key or mouse button:"),
        }
        save_config(binds)

        AXE_BIND = binds["axe"]
        MACE_BIND = binds["mace"]
        ATTACK_BIND = binds["attack"]

        print("Rebinding complete. Starting...\n")

        reset()
        return

    if start_time is None:
        return

    if AXE_BIND.matches_key(key): # type: ignore
        events.append((now_ms() - start_time, "AXE"))
    if MACE_BIND.matches_key(key): # type: ignore
        events.append((now_ms() - start_time, "MACE"))
    if ATTACK_BIND.matches_key(key): # type: ignore
        events.append((now_ms() - start_time, "ATTACK"))



# ---- Probability calculation ----

def success_for_phase(phase_ms, evs):
    ticks = []
    for t, label in evs:
        tick = int(np.floor((t - phase_ms) / TICK_MS))
        ticks.append((tick, label))

    by_tick = {}
    for tick, label in ticks:
        by_tick.setdefault(tick, []).append(label)

    for tick in by_tick:
        if ('ATTACK' in by_tick.get(tick, []) and
            'AXE' in by_tick.get(tick, []) and
            'ATTACK' in by_tick.get(tick + 1, []) and
            'MACE' in by_tick.get(tick + 1, [])):
            return True
    return False


def calculate_probability(evs, resolution=0.05):
    phases = np.arange(0.0, TICK_MS, resolution)
    return sum(success_for_phase(p, evs) for p in phases) / len(phases)


# ---- Plotting ----

def plot_events(evs):
    times = [t for t, _ in evs]
    labels = [l for _, l in evs]
    y_map = {'ATTACK': 2, 'AXE': 1, 'MACE': 0}
    ys = [y_map[l] for l in labels]

    pre_padding = 2 * TICK_MS
    post_padding = 2 * TICK_MS

    start_t = max(0.0, min(times) - pre_padding)
    end_t = max(times) + post_padding

    plt.figure(figsize=(10, 3))
    plt.scatter(times, ys)
    for t, y, l in zip(times, ys, labels):
        plt.text(t, y + 0.05, l, ha='center', fontsize=9)

    for x in np.arange(0, end_t + TICK_MS, TICK_MS):
        plt.axvline(x, linestyle='--', alpha=0.3)

    plt.yticks([0, 1, 2], ['MACE', 'AXE', 'ATTACK'])
    plt.xlabel('Time (ms)')
    plt.title('Input Timing (±2 Ticks Context)')
    plt.xlim(start_t, end_t)
    plt.tight_layout()
    plt.show(block=False)
    plt.pause(1.2)
    plt.close()


# ---- Main ----

def main():
    global AXE_BIND, MACE_BIND, ATTACK_BIND
    print("Stun Slam Tester starting...")
    print("Press Esc at any time to quit the program.")

    binds = load_binds()
        
    AXE_BIND = binds['axe']
    MACE_BIND = binds['mace']
    ATTACK_BIND = binds['attack']

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

            if len(evs) >= 4:
                prob = calculate_probability(evs)
                global attempt_count, cumulative_prob
                attempt_count += 1
                cumulative_prob += prob
                avg = cumulative_prob / attempt_count

                print(f"Attempt {attempt_count}")
                print(f"Current success chance: {prob*100:.2f}%")
                print(f"Average success chance: {avg*100:.2f}%")

                plot_events(evs)
                reset()
                last_event_time = None

            elif last_event_time is not None:
                idle = (now_ms() - start_time) - last_event_time # type: ignore
                if idle >= 3 * TICK_MS:
                    attempt_count += 1
                    avg = cumulative_prob / attempt_count
                    print(f"Attempt {attempt_count}")
                    print("Incomplete attempt (0.00%)")
                    print(f"Average success chance: {avg*100:.2f}%")
                    plot_events(evs)
                    reset()
                    last_event_time = None
                    
        print("\nExiting.")
        os._exit(0)
    finally:
        m_listener.stop()
        k_listener.stop()


if __name__ == '__main__':
    main()
    