/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.facebook.FacebookSdk
import java.lang.Exception

/**
 * Initializes the Facebook SDK when the process starts.
 *
 * The startup decision reads [FacebookSdk.AUTO_INIT_ENABLED_PROPERTY] from application manifest
 * metadata and skips [FacebookSdk.sdkInitialize] only when that flag is explicitly false. Runtime
 * settings continue to govern full initialization after an explicit sdkInitialize call.
 */
class FacebookInitProvider : ContentProvider() {
  companion object {
    private val TAG = FacebookInitProvider::class.java.simpleName
  }

  @Suppress("DEPRECATION")
  override fun onCreate(): Boolean {
    try {
      val context = this.context
      requireNotNull(context)
      // Startup uses the manifest flag. Runtime settings govern full initialization inside
      // sdkInitialize, and are loaded only after the SDK is initialized.
      if (isAutoInitEnabledInManifest(context)) {
        FacebookSdk.sdkInitialize(context)
      } else {
        FacebookSdk.deferInitialization(context)
      }
    } catch (ex: Exception) {
      Log.i(TAG, "Failed to auto initialize the Facebook SDK", ex)
    }
    return false
  }

  private fun isAutoInitEnabledInManifest(context: Context): Boolean {
    try {
      val applicationInfo =
          context.packageManager.getApplicationInfo(
              context.packageName, PackageManager.GET_META_DATA)
      val metaData = applicationInfo.metaData
      if (metaData != null && metaData.containsKey(FacebookSdk.AUTO_INIT_ENABLED_PROPERTY)) {
        return metaData.getBoolean(FacebookSdk.AUTO_INIT_ENABLED_PROPERTY, true)
      }
    } catch (e: PackageManager.NameNotFoundException) {
      // Package metadata is unavailable; attempt initialization as before.
    }
    return true
  }

  override fun query(
      uri: Uri,
      projection: Array<String>?,
      selection: String?,
      selectionArgs: Array<String>?,
      sortOrder: String?
  ): Cursor? {
    return null
  }

  override fun getType(uri: Uri): String? = null

  override fun insert(uri: Uri, values: ContentValues?): Uri? = null

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

  override fun update(
      uri: Uri,
      values: ContentValues?,
      selection: String?,
      selectionArgs: Array<String>?
  ): Int {
    return 0
  }
}
