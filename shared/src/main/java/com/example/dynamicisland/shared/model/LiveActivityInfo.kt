package com.example.dynamicisland.shared.model

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

data class LiveActivityInfo(
    val activityId: String,
    val appPackage: String,
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
    val layoutType: String,
    val initialState: Bundle
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readBundle(LiveActivityInfo::class.java.classLoader) ?: Bundle()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(activityId)
        parcel.writeString(appPackage)
        parcel.writeString(layoutType)
        parcel.writeBundle(initialState)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<LiveActivityInfo> {
        override fun createFromParcel(parcel: Parcel): LiveActivityInfo {
            return LiveActivityInfo(parcel)
        }

        override fun newArray(size: Int): Array<LiveActivityInfo?> {
            return arrayOfNulls(size)
        }
    }
}
