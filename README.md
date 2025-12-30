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

## Running

# 2 Options: Run the python file (recommended) or the windows exe file (mac app support pending)

# Running the Python file:

Download/Clone the repository

Download dependencies with:
pip install -r requirements.txt
(if pip doesnt work try 'python -m pip install -r requirements.txt')

Run the script with:
python StunSlamSimulator.py
Or pressing run on your code editor

Press Esc to quit the program.

# Running the exe file

Download/Clone the repository

compile it yourself with:
python -m pip install PyInstaller
python -m PyInstaller --onefile StunSlamSimulator.py

And double click on the exe file that comes out on the other end

Or:
Download the pre-compiled exe file in a release tag
put it in a folder (it will auto generate a config file)
double click, click run anyways, and follow instructions in the console


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
