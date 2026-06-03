#!/system/bin/sh
LOGCAT_FILE="/data/data/com.termux/files/home/Redwood-Island-Module-Working/island_logcat.log"
STRACE_FILE="/data/data/com.termux/files/home/Redwood-Island-Module-Working/island_strace.log"

echo "🧹 Clearing old logs..."
rm -f $LOGCAT_FILE
rm -f $STRACE_FILE

logcat -c
sleep 1
logcat -c

echo "🚀 Starting 60-second capture..."
logcat -v threadtime > $LOGCAT_FILE &
LOG_PID=$!

SYSUI_PID=$(pidof com.android.systemui)
if [ -n "$SYSUI_PID" ]; then
    strace -p $SYSUI_PID -f -e trace=openat,read,write,mmap -o $STRACE_FILE &
    STRACE_PID=$!
fi

# 🚨 FAILSAFE LOOP (Checks every 2 seconds for 1 minute)
for i in $(seq 1 30); do
    sleep 2
    if grep -qE "AndroidRuntime|FATAL EXCEPTION|SystemUI has stopped" $LOGCAT_FILE; then
        echo "🚨 CRITICAL SYSTEMUI CRASH DETECTED! INITIATING FAILSAFE..."
        kill -9 $LOG_PID
        [ -n "$STRACE_PID" ] && kill -9 $STRACE_PID
        
        # Output the crash reason before nuking
        echo "--- CRASH LOG SNIPPET ---"
        grep -A 10 -B 2 -E "AndroidRuntime|FATAL EXCEPTION" $LOGCAT_FILE | head -n 15
        echo "-------------------------"

        pm disable com.example.dynamicisland
        pkill -9 -f com.android.systemui
        
        # Delete logs instantly as requested
        rm -f $LOGCAT_FILE
        rm -f $STRACE_FILE
        echo "💥 Failsafe triggered. Module disabled. Logs nuked."
        exit 1
    fi
done

kill -9 $LOG_PID
[ -n "$STRACE_PID" ] && kill -9 $STRACE_PID
echo "✅ 60 seconds completed safely. No fatal crashes."
