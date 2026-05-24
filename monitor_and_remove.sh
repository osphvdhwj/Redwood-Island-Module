#!/bin/bash
PACKAGE="com.example.dynamicisland"

uninstall_module() {
    echo "Triggering uninstall for $PACKAGE..."
    # Try pm uninstall first
    su -c "pm uninstall $PACKAGE"
    # Also try to remove the APK if it was installed as a system app or manually
    su -c "rm -rf /data/app/*$PACKAGE*"
    echo "Module removed."
    exit 0
}

# 60 second safety timer
(
    sleep 60
    echo "60 seconds elapsed. Safety uninstall triggered."
    uninstall_module
) &
TIMER_PID=$!

echo "Monitoring for crashes or events (1 minute timeout)... (PID: $$)"

# Monitor logcat for fatal exceptions or system crashes
# We use -c to clear logcat first so we don't catch old crashes
su -c "logcat -c"
su -c "logcat -v brief *:E" | while read line; do
    # Log the crash for debugging if it happens
    if [[ "$line" == *"FATAL EXCEPTION"* ]] || [[ "$line" == *"SystemUI"* ]] || [[ "$line" == *"$PACKAGE"* ]]; then
        echo "CRITICAL EVENT DETECTED: $line"
        kill $TIMER_PID
        uninstall_module
    fi
done
