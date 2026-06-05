package com.example.dynamicisland.shared.ipc;

import android.os.Bundle;

/**
 * 🛰️ IIslandCallback
 *
 * Interface for Satellites to receive real-time state updates from the Brain.
 */
interface IIslandCallback {
    /**
     * Called when the Island's visual state or visibility changes.
     */
    void onIslandStateChanged(String stateName, boolean isVisible);

    /**
     * Called when a specific config or setting is updated.
     */
    void onConfigChanged(String key, String value);
}
