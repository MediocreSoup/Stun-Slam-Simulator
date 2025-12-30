# Stun-Slam-Simulator
Perfect your stun slams for minecraft mace pvp.

Note: This is not a minecraft mod, this is an external program just for checking stun slam inputs

This Tool:
- Records your real mouse and keyboard inputs
- Treats Minecraft ticks as unknown/random
- Calculates the probability that your inputs align correctly
- Shows a timing graph after each attempt
- Tracks your average stun slam success rate

## Requirements

- Windows, macOS, or Linux
- Python 3.9 or newer

To check if Python is installed:
Go to your terminal, paste in and press enter:
python --version

If you do not have Python installed:
https://www.python.org/downloads/

During installation on Windows:
✔ Check "Add Python to PATH"

## Running

Download/Clone the repository

Run the script with:
python minecraft_stun_slam_tester.py
Or pressing run on your code editor

Press Esc to quit the program.

### Keybind Configuration

On first launch, the program will prompt you to set your keybinds.

Binds are saved to `config.json` and loaded automatically on future runs.

To change keybinds later:
- Press **Backspace** at any time

Supported inputs:
- Keyboard keys (letters, numbers)
- Mouse buttons (left, right, middle, side buttons 1 and 2)

Double binding as allowed in 1.21.11 is allowed.


## Important Notes

If something goes HORRIBLY WRONG, to force quit the program:
Click on the terminal and press Ctrl + C
