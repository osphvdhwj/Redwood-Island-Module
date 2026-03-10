import re

with open('./app/src/main/java/com/example/dynamicisland/LiveActivityModel.kt', 'r') as f:
    content = f.read()

# Add isCritical to root
content = content.replace("    abstract val isSensitive: Boolean // 🚀 SECURITY: New Privacy Flag", "    abstract val isSensitive: Boolean // 🚀 SECURITY: New Privacy Flag\n    abstract val isCritical: Boolean // 🚀 NEW: Landscape Override Flag")

# SystemAlert and AppTimerWarning
content = content.replace("override val isSensitive: Boolean = true // 🚀 Masks from screen recorders\n    ) : LiveActivityModel()", "override val isSensitive: Boolean = true, // 🚀 Masks from screen recorders\n        override val isCritical: Boolean = true // 🚀 Forces itself on screen\n    ) : LiveActivityModel()")

# All others get isCritical = false
for model in ['RealityPill', 'Music', 'General', 'Dashboard', 'Charging', 'HardwareMonitor']:
    content = re.sub(r'(data class ' + model + r'\(.*?override val isSensitive: Boolean = false)(\n\s*\) : LiveActivityModel\(\))', r'\1,\n        override val isCritical: Boolean = false\2', content, flags=re.DOTALL)

with open('./app/src/main/java/com/example/dynamicisland/LiveActivityModel.kt', 'w') as f:
    f.write(content)
