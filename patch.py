with open("app/src/main/java/com/example/dynamicisland/ConfigActivity.kt", "r") as f:
    lines = f.readlines()

for i in range(165, 175):
    print(f"{i+1}: {lines[i].rstrip()}")
