package com.example.dynamicisland.ipc;

import com.example.dynamicisland.ipc.LiveActivityInfo;
import android.os.Bundle;

interface ILiveActivityManager {
    String startActivity(in LiveActivityInfo info);
    void updateActivity(String token, in Bundle state);
    void endActivity(String token);
    List<LiveActivityInfo> getActiveActivities();
}
