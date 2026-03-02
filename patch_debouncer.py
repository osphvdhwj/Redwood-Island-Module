def patch_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # Define the old block to match exactly
    search_str = """    private fun resolveHighestPriority() {"""

    replacement = """    private val resolveDebounceRunnable = Runnable { resolveHighestPriority() }

    private fun resolveHighestPriorityDebounced() {
        mainHandler.removeCallbacks(resolveDebounceRunnable)
        mainHandler.postDelayed(resolveDebounceRunnable, 300) // Delay slightly to allow closing animations to start
    }

    private fun resolveHighestPriority() {"""

    if search_str in content:
        content = content.replace(search_str, replacement)
        with open(file_path, 'w') as f:
            f.write(content)
        print("Successfully patched resolveHighestPriorityDebounced.")
    else:
        print("Could not find the block to replace.")

patch_file('app/src/main/java/com/example/dynamicisland/IslandController.kt')
