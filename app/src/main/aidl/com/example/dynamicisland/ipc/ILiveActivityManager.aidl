package com.example.dynamicisland.shared.ipc;

import com.example.dynamicisland.shared.ipc.LiveActivityInfo;
import android.os.Bundle;

interface ILiveActivityManager {
    String startActivity(in LiveActivityInfo info);
    void updateActivity(String token, in Bundle state);
    void endActivity(String token);
    List<LiveActivityInfo> getActiveActivities();
}
