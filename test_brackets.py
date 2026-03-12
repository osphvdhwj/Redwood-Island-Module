with open("app/src/main/java/com/example/dynamicisland/IslandController.kt", "r") as f:
    text = f.read()

count = 0
for i, char in enumerate(text):
    if char == '{': count += 1
    elif char == '}': count -= 1

    if count < 0:
        print(f"Excess closing bracket at index {i}")

print(f"Final count: {count}")
