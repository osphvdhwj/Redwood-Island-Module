with open("old_controller.kt", "r") as f:
    text = f.read()

count = 0
for i, char in enumerate(text):
    if char == '{': count += 1
    elif char == '}': count -= 1

print(f"Final count: {count}")
