import subprocess
try:
    print(subprocess.check_output(["git", "diff", "HEAD~1", "app/src/main/java/com/example/dynamicisland/ConfigActivity.kt"], text=True))
except Exception as e:
    print(e)
