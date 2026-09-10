package com.monstera.harbor.spoof

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle

class SpoofConfigProvider : ContentProvider() {
    override fun onCreate() = true
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "profile") return null
        val context = context ?: return null
        val callerPackages = context.packageManager.getPackagesForUid(Binder.getCallingUid()).orEmpty().toSet()
        val repository = SpoofRepository(context)
        if (!repository.isEnabled() || callerPackages.intersect(repository.enabledPackages()).isEmpty()) return null
        val profile = repository.load()
        if (profile.validate().isNotEmpty()) return null
        return Bundle().apply {
            putString("json", profile.toJson().toString())
            putStringArrayList("enabledFields", ArrayList(profile.enabledFields))
        }
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
