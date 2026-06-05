package com.example.dynamicisland.core.ipc

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

data class LiveActivityInfo(
    val activityId: String,
    val appPackage: String,
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
