import re

with open("app/src/main/java/com/example/dynamicisland/ConfigActivity.kt", "r") as f:
    text = f.read()

open_braces = text.count('{')
close_braces = text.count('}')

print(f"Current config: {open_braces} open, {close_braces} close")
