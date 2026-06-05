package com.example.dynamicisland.shared.ipc;

import android.os.Bundle;

/**
 * 🧠 IIslandBrain
 *
 * The primary IPC interface for the Redwood Island Core App.
 * Allows Xposed Satellites and system services to interact with the Neural Core.
 */
interface IIslandBrain {
    /**
     * Dispatch a raw intent payload to the Neural Core.
     * 
     * @param action The intent action name.
     * @param extras Optional parameters.
     */
    void dispatch(String action, in Bundle extras);

    /**
     * Synchronize settings from a satellite.
     */
    void updateSettings(in Bundle settingsBundle);
    
    /**
     * Notify the brain of a new live activity.
     */
    void postActivity(String modelJson);

    /**
     * Remove an activity by ID.
     */
    void removeActivity(String activityId);
}
