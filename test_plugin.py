import re

with open("settings.gradle.kts", "r") as f:
    content = f.read()

# Make sure google() is explicitly there, yes it is. Wait, is it `pluginManagement` or `buildscript`?
# In modern gradle, pluginManagement in settings.gradle.kts should suffice.
# But wait, looking at the CI failure:
# Plugin [id: 'com.android.application', version: '8.5.2', apply: false] was not found in any of the following sources:
# - Gradle Core Plugins (plugin is not in 'org.gradle' namespace)
# - Included Builds (No included builds contain this plugin)
# - Plugin Repositories (could not resolve plugin artifact 'com.android.application:com.android.application.gradle.plugin:8.5.2')
#   Searched in the following repositories:
#     Gradle Central Plugin Repository
# This specifically says it ONLY searched in "Gradle Central Plugin Repository".
# This means `google()` is either missing from `pluginManagement` repositories, or the `settings.gradle.kts` wasn't loaded properly?
# Wait! Let's look at `settings.gradle.kts`!

print(content)
