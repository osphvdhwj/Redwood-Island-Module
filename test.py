import re

with open("old_controller.kt", "r") as f:
    text = f.read()

# Let's count properly for the original file in origin.
print(f"Total old: {text.count('{')} open, {text.count('}')} close")

# The problem in old_controller.kt is that there are 167 '{' and 166 '}'.
# Let's find where the mismatch happens.
