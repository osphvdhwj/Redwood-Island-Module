import sys

def patch_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    search_str = "    private var isScreenOn = true"
    replace_str = "    private var isScreenOn = true\n    private var isUserExpanded = false // Tracks if the user manually opened the pill"

    if search_str in content and "isUserExpanded =" not in content:
        content = content.replace(search_str, replace_str)
        with open(file_path, 'w') as f:
            f.write(content)
        print("Successfully patched isUserExpanded")
    elif "isUserExpanded =" in content:
        print("Already patched")
    else:
        print("Could not find insertion point")

patch_file('app/src/main/java/com/example/dynamicisland/IslandController.kt')
