import re

with open("settings.gradle.kts", "r") as f:
    content = f.read()

# Make sure google() is explicitly there, it is.
