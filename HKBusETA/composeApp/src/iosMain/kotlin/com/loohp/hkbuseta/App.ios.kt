/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkbuseta

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import com.loohp.hkbuseta.appcontext.applicationAppContext
import com.loohp.hkbuseta.appcontext.navColorState
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.utils.IO
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSProcessInfo
import platform.Foundation.isiOSAppOnMac
import kotlin.system.exitProcess

actual fun exitApp() {
    exitProcess(0)
}

actual fun watchDataOverwriteWarningInitialValue(): Boolean = runBlocking(Dispatchers.IO) { Registry.isNewInstall(applicationAppContext) }

@Composable
actual fun SnackbarInterface(instance: AppActiveContext, snackbarHostState: SnackbarHostState) = SnackbarHost(snackbarHostState)

@Composable
actual fun Modifier.consumePlatformWindowInsets(): Modifier {
    return this
        .consumeWindowInsets(WindowInsets.safeContent.exclude(WindowInsets.ime))
        .padding(WindowInsets.safeDrawing.detectKeepSafeAreaSides().asPaddingValues())
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun WindowInsets.detectKeepSafeAreaSides(): WindowInsets {
    return if (NSProcessInfo.processInfo.isiOSAppOnMac()) {
        WindowInsets(0, 0, 0, 0)
    } else {
        val windowSize = calculateWindowSizeClass()
        if (windowSize.heightSizeClass == WindowHeightSizeClass.Compact || windowSize.widthSizeClass == WindowWidthSizeClass.Expanded) {
            only(WindowInsetsSides.Top + WindowInsetsSides.Left + WindowInsetsSides.Right)
        } else {
            this
        }
    }
}
