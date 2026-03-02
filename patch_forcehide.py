def patch_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    replacement = """
    private fun forceHide() {
        val island = islandViewRef?.get() ?: return
        island.setState(DynamicIslandView.IslandState.HIDDEN)
    }

    private fun resolveHighestPriority() {"""

    search_str = "    private fun resolveHighestPriority() {"

    if search_str in content and "fun forceHide()" not in content:
        content = content.replace(search_str, replacement)
        with open(file_path, 'w') as f:
            f.write(content)
        print("Successfully added forceHide.")
    else:
        print("Could not patch or forceHide already exists.")

patch_file('app/src/main/java/com/example/dynamicisland/IslandController.kt')
