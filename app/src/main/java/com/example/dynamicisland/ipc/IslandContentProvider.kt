package com.example.dynamicisland.ipc

import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * BATCH 1: Secure ContentProvider-based IPC
 *
 * Replaces the world-readable SharedPreferences hack entirely.
 * SystemUI reads config through this provider using standard Android
 * content resolution — no file permission manipulation, no race conditions.
 *
 * URI scheme:
 *   content://com.example.dynamicisland.provider/prefs         → all preferences
 *   content://com.example.dynamicisland.provider/prefs/{key}   → single preference
 *   content://com.example.dynamicisland.provider/state         → live island state
 *   content://com.example.dynamicisland.provider/gesture       → gesture matrix
 *   content://com.example.dynamicisland.provider/dashboard     → pinned apps + QS tiles
 */
class IslandContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.example.dynamicisland.provider"
        const val TAG = "IslandProvider"

        // Table paths
        const val PATH_PREFS     = "prefs"
        const val PATH_STATE     = "state"
        const val PATH_GESTURE   = "gesture"
        const val PATH_DASHBOARD = "dashboard"

        // Column names
        const val COL_KEY   = "key"
        const val COL_VALUE = "value"
        const val COL_TYPE  = "type"   // "string" | "float" | "int" | "boolean"

        // Type constants stored in COL_TYPE
        const val TYPE_STRING  = "string"
        const val TYPE_FLOAT   = "float"
        const val TYPE_INT     = "int"
        const val TYPE_BOOLEAN = "boolean"
        const val TYPE_JSON    = "json"

        // URI builder helpers — call these from any process
        val BASE_URI: Uri = Uri.parse("content://$AUTHORITY")

        fun prefsUri(): Uri = Uri.withAppendedPath(BASE_URI, PATH_PREFS)
        fun prefUri(key: String): Uri = Uri.withAppendedPath(prefsUri(), key)
        fun stateUri(): Uri = Uri.withAppendedPath(BASE_URI, PATH_STATE)
        fun gestureUri(): Uri = Uri.withAppendedPath(BASE_URI, PATH_GESTURE)
        fun dashboardUri(): Uri = Uri.withAppendedPath(BASE_URI, PATH_DASHBOARD)

        // The in-process store — both the module app and SystemUI keep a local
        // copy so reads never cross a process boundary twice.
        private val store = ConcurrentHashMap<String, Pair<String, String>>() // key → (value, type)
        private val lock  = ReentrantReadWriteLock()

        // Observers registered by SystemUI's IslandBinderClient
        private val observers = ConcurrentHashMap<String, MutableList<() -> Unit>>()

        fun registerObserver(path: String, callback: () -> Unit) {
            observers.getOrPut(path) { mutableListOf() }.add(callback)
        }

        fun unregisterAll(path: String) {
            observers.remove(path)
        }

        // Convenience typed write — used by ConfigManager replacement
        fun putFloat(key: String, value: Float) {
            lock.write { store[key] = Pair(value.toString(), TYPE_FLOAT) }
        }

        fun putInt(key: String, value: Int) {
            lock.write { store[key] = Pair(value.toString(), TYPE_INT) }
        }

        fun putBoolean(key: String, value: Boolean) {
            lock.write { store[key] = Pair(value.toString(), TYPE_BOOLEAN) }
        }

        fun putString(key: String, value: String) {
            lock.write { store[key] = Pair(value, TYPE_STRING) }
        }

        fun putJson(key: String, json: JSONObject) {
            lock.write { store[key] = Pair(json.toString(), TYPE_JSON) }
        }

        fun getFloat(key: String, default: Float = 0f): Float =
            lock.read { store[key]?.first?.toFloatOrNull() ?: default }

        fun getInt(key: String, default: Int = 0): Int =
            lock.read { store[key]?.first?.toIntOrNull() ?: default }

        fun getBoolean(key: String, default: Boolean = false): Boolean =
            lock.read { store[key]?.first?.toBooleanStrictOrNull() ?: default }

        fun getString(key: String, default: String = ""): String =
            lock.read { store[key]?.first ?: default }

        fun getJson(key: String): JSONObject? =
            lock.read {
                val raw = store[key]?.first ?: return@read null
                try { JSONObject(raw) } catch (e: Exception) { null }
            }

        // Snapshot the entire store as a flat JSON blob for bulk sync
        fun snapshotJson(): JSONObject {
            val obj = JSONObject()
            lock.read {
                store.forEach { (k, pair) ->
                    obj.put(k, JSONObject().apply {
                        put("v", pair.first)
                        put("t", pair.second)
                    })
                }
            }
            return obj
        }

        // Load from SharedPreferences on first boot so we don't lose data
        fun bootstrapFromSharedPrefs(prefs: android.content.SharedPreferences) {
            lock.write {
                prefs.all.forEach { (k, v) ->
                    when (v) {
                        is Float   -> store[k] = Pair(v.toString(), TYPE_FLOAT)
                        is Int     -> store[k] = Pair(v.toString(), TYPE_INT)
                        is Boolean -> store[k] = Pair(v.toString(), TYPE_BOOLEAN)
                        is String  -> store[k] = Pair(v, TYPE_STRING)
                        else       -> { /* skip */ }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // ContentProvider lifecycle
    // -------------------------------------------------------------------------

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, PATH_PREFS,           1)
        addURI(AUTHORITY, "$PATH_PREFS/*",      2)
        addURI(AUTHORITY, PATH_STATE,           3)
        addURI(AUTHORITY, PATH_GESTURE,         4)
        addURI(AUTHORITY, PATH_DASHBOARD,       5)
    }

    override fun onCreate(): Boolean {
        // Bootstrap from legacy SharedPreferences if they still exist
        context?.let { ctx ->
            try {
                val legacy = ctx.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
                bootstrapFromSharedPrefs(legacy)
                Log.i(TAG, "Bootstrapped ${store.size} keys from legacy prefs")
            } catch (e: Exception) {
                Log.w(TAG, "No legacy prefs to bootstrap: ${e.message}")
            }
        }
        return true
    }

    // -------------------------------------------------------------------------
    // QUERY — reads
    // -------------------------------------------------------------------------

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(arrayOf(COL_KEY, COL_VALUE, COL_TYPE))
        when (uriMatcher.match(uri)) {

            1 -> { // All prefs
                lock.read {
                    store.forEach { (k, pair) ->
                        cursor.addRow(arrayOf(k, pair.first, pair.second))
                    }
                }
            }

            2 -> { // Single pref by key
                val key = uri.lastPathSegment ?: return cursor
                lock.read {
                    store[key]?.let { (v, t) -> cursor.addRow(arrayOf(key, v, t)) }
                }
            }

            3 -> { // State snapshot (full JSON)
                cursor.addRow(arrayOf("snapshot", snapshotJson().toString(), TYPE_JSON))
            }

            4 -> { // Gesture matrix only
                lock.read {
                    store.filter { it.key.startsWith("TYPE_") }.forEach { (k, pair) ->
                        cursor.addRow(arrayOf(k, pair.first, pair.second))
                    }
                }
            }

            5 -> { // Dashboard — pinned apps + QS tiles
                lock.read {
                    store.filter { it.key.startsWith("pinned_app_") ||
                                   it.key.startsWith("qs_tile_") }.forEach { (k, pair) ->
                        cursor.addRow(arrayOf(k, pair.first, pair.second))
                    }
                }
            }
        }
        return cursor
    }

    // -------------------------------------------------------------------------
    // INSERT / UPDATE — writes from the module app process
    // -------------------------------------------------------------------------

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        values ?: return null
        val key   = values.getAsString(COL_KEY)   ?: return null
        val value = values.getAsString(COL_VALUE) ?: return null
        val type  = values.getAsString(COL_TYPE)  ?: TYPE_STRING

        lock.write { store[key] = Pair(value, type) }

        // Notify all registered observers and content observers
        val notifyUri = prefUri(key)
        context?.contentResolver?.notifyChange(notifyUri, null)
        notifyObserversFor(uri)

        return notifyUri
    }

    override fun bulkInsert(uri: Uri, valuesArray: Array<out ContentValues>): Int {
        var count = 0
        lock.write {
            valuesArray.forEach { values ->
                val key   = values.getAsString(COL_KEY)   ?: return@forEach
                val value = values.getAsString(COL_VALUE) ?: return@forEach
                val type  = values.getAsString(COL_TYPE)  ?: TYPE_STRING
                store[key] = Pair(value, type)
                count++
            }
        }
        context?.contentResolver?.notifyChange(prefsUri(), null)
        notifyObserversFor(prefsUri())
        return count
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        if (values == null) return 0
        // Reuse insert logic
        insert(uri, values)
        return 1
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        if (uriMatcher.match(uri) == 2) {
            val key = uri.lastPathSegment ?: return 0
            lock.write { store.remove(key) }
            context?.contentResolver?.notifyChange(uri, null)
            return 1
        }
        return 0
    }

    // -------------------------------------------------------------------------
    // call() — used for typed atomic operations without full cursor overhead
    // -------------------------------------------------------------------------

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val result = Bundle()
        when (method) {
            "GET_FLOAT"   -> result.putFloat("v",   getFloat(arg   ?: "", extras?.getFloat("d")   ?: 0f))
            "GET_INT"     -> result.putInt("v",     getInt(arg     ?: "", extras?.getInt("d")     ?: 0))
            "GET_BOOLEAN" -> result.putBoolean("v", getBoolean(arg ?: "", extras?.getBoolean("d") ?: false))
            "GET_STRING"  -> result.putString("v",  getString(arg  ?: "", extras?.getString("d")  ?: ""))
            "GET_SNAPSHOT"-> result.putString("v",  snapshotJson().toString())

            "PUT_FLOAT"   -> { putFloat(arg   ?: "", extras?.getFloat("v")   ?: 0f);   notifyAll() }
            "PUT_INT"     -> { putInt(arg     ?: "", extras?.getInt("v")     ?: 0);    notifyAll() }
            "PUT_BOOLEAN" -> { putBoolean(arg ?: "", extras?.getBoolean("v") ?: false); notifyAll() }
            "PUT_STRING"  -> { putString(arg  ?: "", extras?.getString("v")  ?: "");   notifyAll() }
            "PUT_JSON"    -> {
                val json = try { JSONObject(extras?.getString("v") ?: "{}") } catch (e: Exception) { JSONObject() }
                putJson(arg ?: "", json)
                notifyAll()
            }

            "BULK_PUT" -> {
                val payload = extras?.getString("payload") ?: return result
                try {
                    val obj = JSONObject(payload)
                    lock.write {
                        obj.keys().forEach { key ->
                            val entry = obj.getJSONObject(key)
                            store[key] = Pair(entry.getString("v"), entry.getString("t"))
                        }
                    }
                    notifyAll()
                    result.putBoolean("ok", true)
                } catch (e: Exception) {
                    result.putBoolean("ok", false)
                }
            }
        }
        return result
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.$AUTHORITY.prefs"

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun notifyObserversFor(uri: Uri) {
        val path = uri.path ?: return
        observers[path]?.forEach { try { it() } catch (e: Exception) { /* isolated */ } }
    }

    private fun notifyAll() {
        context?.contentResolver?.notifyChange(prefsUri(), null)
        observers.values.flatten().forEach { try { it() } catch (e: Exception) { } }
    }
}
