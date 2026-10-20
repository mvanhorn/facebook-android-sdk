/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.internal.WhiteboxImpl
import org.robolectric.shadows.ShadowLog

@PrepareForTest(FacebookSdk::class)
class FacebookInitProviderTest : FacebookPowerMockTestCase() {
  private lateinit var provider: FacebookInitProvider
  private lateinit var mockContext: Context
  private lateinit var mockPackageManager: PackageManager
  private lateinit var mockApplicationInfo: ApplicationInfo

  @Before
  fun init() {
    mockContext = PowerMockito.mock(Context::class.java)
    mockPackageManager = mock()
    mockApplicationInfo = mock()
    whenever(mockContext.packageManager).thenReturn(mockPackageManager)
    whenever(mockContext.packageName).thenReturn("com.facebook.test")
    whenever(mockPackageManager.getApplicationInfo(any<String>(), any<Int>()))
        .thenReturn(mockApplicationInfo)
    provider = FacebookInitProvider()
    WhiteboxImpl.setInternalState(provider, "mContext", mockContext as Any)
    PowerMockito.mockStatic(FacebookSdk::class.java)
  }

  @Test
  fun `test onCreate call FacebookSdk initialization when metadata is missing`() {
    mockApplicationInfo.metaData = null
    var capturedContext: Context? = null
    var initializeCalls = 0
    whenever(FacebookSdk.sdkInitialize(any())).thenAnswer {
      initializeCalls++
      capturedContext = it.arguments[0] as Context
      Unit
    }
    Assert.assertFalse(provider.onCreate())
    Assert.assertEquals(1, initializeCalls)
    Assert.assertNotNull(capturedContext)
    Assert.assertEquals(mockContext, capturedContext)
  }

  @Test
  fun `test onCreate initializes once when auto init is enabled`() {
    val metaData = Bundle()
    metaData.putBoolean(FacebookSdk.AUTO_INIT_ENABLED_PROPERTY, true)
    mockApplicationInfo.metaData = metaData
    var capturedContext: Context? = null
    var initializeCalls = 0
    whenever(FacebookSdk.sdkInitialize(any())).thenAnswer {
      initializeCalls++
      capturedContext = it.arguments[0] as Context
      Unit
    }
    Assert.assertFalse(provider.onCreate())
    Assert.assertEquals(1, initializeCalls)
    Assert.assertEquals(mockContext, capturedContext)
  }

  @Test
  fun `test onCreate initializes once when auto init flag is absent`() {
    mockApplicationInfo.metaData = Bundle()
    var capturedContext: Context? = null
    var initializeCalls = 0
    whenever(FacebookSdk.sdkInitialize(any())).thenAnswer {
      initializeCalls++
      capturedContext = it.arguments[0] as Context
      Unit
    }
    Assert.assertFalse(provider.onCreate())
    Assert.assertEquals(1, initializeCalls)
    Assert.assertEquals(mockContext, capturedContext)
  }

  @Test
  fun `test onCreate skips sdkInitialize when auto init is disabled`() {
    val metaData = Bundle()
    metaData.putBoolean(FacebookSdk.AUTO_INIT_ENABLED_PROPERTY, false)
    mockApplicationInfo.metaData = metaData
    var initializeCalls = 0
    whenever(FacebookSdk.sdkInitialize(any())).thenAnswer {
      initializeCalls++
      Unit
    }
    Assert.assertFalse(provider.onCreate())
    Assert.assertEquals(0, initializeCalls)
  }

  @Test
  fun `test onCreate initializes when metadata lookup is not found`() {
    whenever(mockPackageManager.getApplicationInfo(any<String>(), any<Int>()))
        .thenThrow(PackageManager.NameNotFoundException())
    var capturedContext: Context? = null
    var initializeCalls = 0
    whenever(FacebookSdk.sdkInitialize(any())).thenAnswer {
      initializeCalls++
      capturedContext = it.arguments[0] as Context
      Unit
    }
    Assert.assertFalse(provider.onCreate())
    Assert.assertEquals(1, initializeCalls)
    Assert.assertEquals(mockContext, capturedContext)
  }

  @Test
  fun `test onCreate returns false when sdkInitialize throws`() {
    val failure = RuntimeException("missing app id")
    var initializeCalls = 0
    whenever(FacebookSdk.sdkInitialize(any())).thenAnswer {
      initializeCalls++
      throw failure
    }
    Assert.assertFalse(provider.onCreate())
    Assert.assertEquals(1, initializeCalls)
    val logged =
        ShadowLog.getLogs().lastOrNull { it.tag == FacebookInitProvider::class.java.simpleName }
    Assert.assertNotNull(logged)
    Assert.assertSame(failure, logged?.throwable)
  }
}
