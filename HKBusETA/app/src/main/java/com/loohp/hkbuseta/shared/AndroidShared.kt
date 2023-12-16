/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.hkbuseta.shared

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.loohp.hkbuseta.FatalErrorActivity
import com.loohp.hkbuseta.MainActivity
import com.loohp.hkbuseta.appcontext.AppActiveContext
import com.loohp.hkbuseta.appcontext.AppActiveContextAndroid
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.utils.isEqualTo
import java.util.concurrent.atomic.AtomicReference

data class CurrentActivityData(val cls: Class<Activity>, val extras: Bundle?, val shouldRelaunch: Boolean = extras?.getBoolean("shouldRelaunch", true)?: true) {

    fun isEqualTo(other: Any?): Boolean {
        return if (other is CurrentActivityData) {
            this.cls == other.cls && this.shouldRelaunch == other.shouldRelaunch && ((this.extras == null && other.extras == null) || (this.extras != null && this.extras.isEqualTo(other.extras)))
        } else {
            false
        }
    }

}

object AndroidShared {

    @SuppressLint("WearRecents")
    fun setDefaultExceptionHandler(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Shared.invalidateCache(context.appContext)
                if (context is Activity) {
                    var stacktrace = throwable.stackTraceToString()
                    if (stacktrace.length > 459000) {
                        stacktrace = stacktrace.substring(0, 459000).plus("...")
                    }
                    val intent = Intent(context, FatalErrorActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.putExtra("exception", stacktrace)
                    context.startActivity(intent)
                }
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
                throw throwable
            }
        }
    }

    private var currentActivity: AtomicReference<CurrentActivityData?> = AtomicReference(null)

    fun getCurrentActivity(): CurrentActivityData? {
        return currentActivity.get()
    }

    fun setSelfAsCurrentActivity(activity: Activity) {
        currentActivity.set(CurrentActivityData(activity.javaClass, activity.intent.extras))
    }

    fun removeSelfFromCurrentActivity(activity: Activity) {
        val data = CurrentActivityData(activity.javaClass, activity.intent.extras)
        currentActivity.updateAndGet { if (it != null && it.isEqualTo(data)) null else it }
    }

    @SuppressLint("WearRecents")
    fun ensureRegistryDataAvailable(activity: Activity): Boolean {
        return if (!Registry.hasInstanceCreated() || Registry.getInstanceNoUpdateCheck(activity.appContext).state.value.isProcessing) {
            val intent = Intent(activity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            activity.startActivity(intent)
            activity.finishAffinity()
            false
        } else {
            true
        }
    }

    fun restoreCurrentScreenOrRun(context: AppActiveContext, orElse: () -> Unit) {
        val currentActivity = getCurrentActivity()
        if (currentActivity == null || !currentActivity.shouldRelaunch) {
            orElse.invoke()
        } else {
            (context as AppActiveContextAndroid).context.apply {
                val intent2 = Intent(this, currentActivity.cls)
                if (currentActivity.extras != null) {
                    intent2.putExtras(currentActivity.extras)
                }
                startActivity(intent2)
                finishAffinity()
            }
        }
    }

}