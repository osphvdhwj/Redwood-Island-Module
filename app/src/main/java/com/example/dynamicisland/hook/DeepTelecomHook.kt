package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.UserHandle
import android.provider.ContactsContract
import de.robv.android.xposed.XC_MethodHook
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * BATCH 4: Deep Telecom Hook
 *
 * Supersedes FrameworkTelecomHook.kt with two enhancements:
 *
 *   1. ContactsContract lookup — the moment a call state changes, we query
 *      the system Contacts provider for the display name and photo URI
 *      associated with the caller's E.164 number. This runs synchronously
 *      on the Telecom thread (which is a background thread, so it's safe).
 *
 *   2. Photo URI delivery — we include the content:// photo URI in the
 *      broadcast. IslandCallManager in SystemUI then loads it on an IO
 *      coroutine and sets it on LiveActivityModel.Call. No bitmap is passed
 *      over IPC — only the URI string, which is lightweight and safe.
 *
 *   3. Relationship label — if the contact has a custom label (e.g., "Dad",
 *      "Work"), we include it alongside the display name so the island can
 *      show "Incoming • Dad" instead of just the name.
 *
 * Broadcast extras published:
 *   "state"        String  RINGING | ONGOING | DISCONNECTED
 *   "caller"       String  display name (or number if not in contacts)
 *   "number"       String  raw E.164 number (redacted as "Private" if unknown)
 *   "photoUri"     String? content:// photo URI (null if no contact photo)
 *   "relationLabel" String? custom relationship label (null if none)
 *   "isSpam"       Boolean true if number appears in call log as REJECTED ≥3 times
 */
object DeepTelecomHook {

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {

        IslandHookEngine.hookMethodSafe(
            "com.android.server.telecom.Call",
            lpparam.classLoader,
            "setState",
            Int::class.javaPrimitiveType ?: Int::class.java,
            String::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val stateInt = param.args[0] as Int
                        val stateStr = mapState(stateInt) ?: return

                        val context = XposedHelpers.callMethod(
                            param.thisObject, "getContext"
                        ) as? Context ?: return

                        val handle = runCatching {
                            XposedHelpers.callMethod(param.thisObject, "getHandle")
                        }.getOrNull()

                        val rawNumber = handle?.toString()
                            ?.replace("tel:", "")
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() } ?: "Private"

                        // ── Contact lookup ─────────────────────────────────────
                        val contactInfo = if (rawNumber != "Private") {
                            lookupContact(context, rawNumber)
                        } else {
                            ContactInfo(rawNumber, null, null)
                        }

                        // ── Spam heuristic ─────────────────────────────────────
                        val isSpam = if (rawNumber != "Private") {
                            isLikelySpam(context, rawNumber)
                        } else false

                        val intent = Intent("com.example.dynamicisland.BRAIN_EVENT").apply {
                            setPackage("com.example.dynamicisland.core")
                            putExtra("action",       "CALL_STATE_CHANGED")
                            putExtra("state",        stateStr)
                            putExtra("caller",       contactInfo.displayName)
                            putExtra("number",       rawNumber)
                            putExtra("photoUri",     contactInfo.photoUri)
                            putExtra("relationLabel", contactInfo.relationLabel)
                            putExtra("isSpam",       isSpam)
                        }
                        context.sendBroadcastAsUser(intent, userAll, "com.redwood.permission.SECURE_IPC")

                    } catch (_: Throwable) {}
                }
            }
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun mapState(stateInt: Int): String? = when (stateInt) {
        2 -> "RINGING"        // CallState.RINGING (dialing incoming)
        4 -> "RINGING"        // CallState.RINGING (confirmed)
        5 -> "ONGOING"        // CallState.ACTIVE
        7 -> "DISCONNECTED"   // CallState.DISCONNECTED
        else -> null
    }

    data class ContactInfo(
        val displayName:   String,
        val photoUri:      String?,
        val relationLabel: String?
    )

    /**
     * Queries [ContactsContract.PhoneLookup] for the given phone number.
     * Returns display name + photo URI if a match is found; the raw number otherwise.
     *
     * Also checks [ContactsContract.CommonDataKinds.Relation] for a custom label.
     */
    private fun lookupContact(context: Context, number: String): ContactInfo {
        var displayName: String   = number
        var photoUri:    String?  = null
        var relationLabel: String? = null
        var contactId:   Long     = -1L

        // ── Phase 1: Phone number → contact ID + display name ─────────────────
        val phoneLookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val phoneCursor: Cursor? = runCatching {
            context.contentResolver.query(
                phoneLookupUri,
                arrayOf(
                    ContactsContract.PhoneLookup._ID,
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.PHOTO_URI
                ),
                null, null, null
            )
        }.getOrNull()

        phoneCursor?.use { cur ->
            if (cur.moveToFirst()) {
                contactId   = cur.getLong(cur.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
                displayName = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                    ?: number
                photoUri    = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI))
            }
        }

        if (contactId == -1L) return ContactInfo(displayName, photoUri, null)

        // ── Phase 2: Contact ID → relationship label ──────────────────────────
        val relationCursor: Cursor? = runCatching {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Relation.NAME,
                    ContactsContract.CommonDataKinds.Relation.LABEL
                ),
                "${ContactsContract.Data.CONTACT_ID} = ? AND " +
                "${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(
                    contactId.toString(),
                    ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE
                ),
                null
            )
        }.getOrNull()

        relationCursor?.use { cur ->
            if (cur.moveToFirst()) {
                val relName  = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Relation.NAME))
                val relLabel = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Relation.LABEL))
                relationLabel = when {
                    !relName.isNullOrBlank()  -> relName
                    !relLabel.isNullOrBlank() -> relLabel
                    else                      -> null
                }
            }
        }

        return ContactInfo(displayName, photoUri, relationLabel)
    }

    /**
     * Returns true if the number appears in the call log as REJECTED ≥ 3 times.
     * A crude but zero-network spam signal using only local data.
     */
    private fun isLikelySpam(context: Context, number: String): Boolean {
        return try {
            val cursor = context.contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                arrayOf(android.provider.CallLog.Calls.NUMBER, android.provider.CallLog.Calls.TYPE),
                "${android.provider.CallLog.Calls.NUMBER} = ? AND " +
                "${android.provider.CallLog.Calls.TYPE} = ?",
                arrayOf(number, android.provider.CallLog.Calls.REJECTED_TYPE.toString()),
                null
            )
            val count = cursor?.count ?: 0
            cursor?.close()
            count >= 3
        } catch (_: Exception) { false }
    }
}