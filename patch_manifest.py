import re

with open('./app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

# Add WRITE_SETTINGS permission
content = content.replace('<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />', '<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />\n    <uses-permission android:name="android.permission.WRITE_SETTINGS" tools:ignore="ProtectedPermissions" />')

with open('./app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
