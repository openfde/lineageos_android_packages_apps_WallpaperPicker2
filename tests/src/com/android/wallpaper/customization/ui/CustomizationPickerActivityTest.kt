/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wallpaper.customization.ui

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.category.ui.view.CategoriesFragment
import com.android.wallpaper.picker.customization.ui.CustomizationPickerActivity
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.util.LaunchSourceUtils.LAUNCH_SOURCE_LAUNCHER
import com.android.wallpaper.util.LaunchSourceUtils.LAUNCH_SOURCE_SETTINGS
import com.android.wallpaper.util.LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [CustomizationPickerActivity] */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CustomizationPickerActivityTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var testInjector: TestInjector

    @Before
    fun setUp() {
        hiltRule.inject()
        InjectorProvider.setInjector(testInjector)
    }

    @After
    fun tearDown() {
        // TODO(b/467381197) Perhaps move this call to ClockRegistryProvider, etc.
        Thread.setUncaughtExceptionPreHandler(null)
    }

    @Test
    fun launchSuccess_showsCategoriesAsRootFragment() {
        ActivityScenario.launch(CustomizationPickerActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertThat(activity.supportFragmentManager.findFragmentById(R.id.fragment_container))
                    .isInstanceOf(CategoriesFragment::class.java)
            }
        }
    }

    @Test
    fun launchFromLauncher_showsHomeDestination() {
        val intent =
            Intent(context, CustomizationPickerActivity::class.java).apply {
                putExtra(WALLPAPER_LAUNCH_SOURCE, LAUNCH_SOURCE_LAUNCHER)
            }
        ActivityScenario.launch<CustomizationPickerActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentById(
                        R.id.fragment_container
                    ) as CategoriesFragment
                assertThat(fragment.arguments?.getSerializable("destination_screen", Screen::class.java))
                    .isEqualTo(Screen.HOME_SCREEN)
            }
        }
    }

    @Test
    fun launchFromSettings_showsLockDestination() {
        val intent =
            Intent(context, CustomizationPickerActivity::class.java).apply {
                putExtra(WALLPAPER_LAUNCH_SOURCE, LAUNCH_SOURCE_SETTINGS)
            }
        ActivityScenario.launch<CustomizationPickerActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentById(
                        R.id.fragment_container
                    ) as CategoriesFragment
                assertThat(fragment.arguments?.getSerializable("destination_screen", Screen::class.java))
                    .isEqualTo(Screen.LOCK_SCREEN)
            }
        }
    }
}
