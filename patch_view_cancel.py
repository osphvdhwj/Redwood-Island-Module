import re

with open("app/src/main/java/com/example/dynamicisland/DynamicIslandView.kt", "r") as f:
    content = f.read()

old_code = """        viewScope?.cancel()"""
new_code = """        viewScope?.cancel(null)"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/dynamicisland/DynamicIslandView.kt", "w") as f:
    f.write(content)
