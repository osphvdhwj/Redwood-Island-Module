import re

with open("build.gradle.kts", "r") as f:
    content = f.read()

new_content = """buildscript {
    repositories {
        google()
        mavenCentral()
    }
}
""" + content

with open("build.gradle.kts", "w") as f:
    f.write(new_content)
