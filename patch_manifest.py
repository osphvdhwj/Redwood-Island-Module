import re

with open('./app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

# Add QUERY_ALL_PACKAGES
content = content.replace('<uses-permission android:name="android.permission.WRITE_SETTINGS" tools:ignore="ProtectedPermissions" />', '<uses-permission android:name="android.permission.WRITE_SETTINGS" tools:ignore="ProtectedPermissions" />\n    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" tools:ignore="QueryAllPackagesPermission" />')

with open('./app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
