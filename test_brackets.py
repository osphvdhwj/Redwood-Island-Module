with open("head1_config.kt", "r") as f:
    text = f.read()

count = 0
for i, char in enumerate(text):
    if char == '{': count += 1
    elif char == '}': count -= 1
print(f"Final Count head1_config: {count}")
