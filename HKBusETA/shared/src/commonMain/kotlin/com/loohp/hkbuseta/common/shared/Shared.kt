/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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

package com.loohp.hkbuseta.common.shared

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.FavouriteRouteStop
import com.loohp.hkbuseta.common.objects.LastLookupRoute
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.RouteSortMode
import com.loohp.hkbuseta.common.objects.gmbRegion
import com.loohp.hkbuseta.common.utils.Immutable
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


@Immutable
object Shared {

    const val ETA_UPDATE_INTERVAL: Int = 15000

    fun invalidateCache(context: AppContext) {
        try {
            Registry.invalidateCache(context)
        } catch (_: Throwable) {}
    }

    fun getMtrLineSortingIndex(lineName: String): Int {
        return when (lineName) {
            "AEL" -> 0
            "TCL" -> 1
            "TML" -> 4
            "TKL" -> 9
            "EAL" -> 3
            "SIL" -> 5
            "TWL" -> 8
            "ISL" -> 6
            "KTL" -> 7
            "DRL" -> 2
            else -> 10
        }
    }

    fun getMtrLineName(lineName: String): String {
        return getMtrLineName(lineName, lineName)
    }

    fun getMtrLineName(lineName: String, orElse: String): String {
        return if (language == "en") when (lineName) {
            "AEL" -> "Airport Express"
            "TCL" -> "Tung Chung Line"
            "TML" -> "Tuen Ma Line"
            "TKL" -> "Tseung Kwan O Line"
            "EAL" -> "East Rail Line"
            "SIL" -> "South Island Line"
            "TWL" -> "Tsuen Wan Line"
            "ISL" -> "Island Line"
            "KTL" -> "Kwun Tong Line"
            "DRL" -> "Disneyland Resort Line"
            else -> orElse
        } else when (lineName) {
            "AEL" -> "機場快綫"
            "TCL" -> "東涌綫"
            "TML" -> "屯馬綫"
            "TKL" -> "將軍澳綫"
            "EAL" -> "東鐵綫"
            "SIL" -> "南港島綫"
            "TWL" -> "荃灣綫"
            "ISL" -> "港島綫"
            "KTL" -> "觀塘綫"
            "DRL" -> "迪士尼綫"
            else -> orElse
        }
    }

    var language = "zh"

    private val suggestedMaxFavouriteRouteStop = MutableStateFlow(0)
    private val currentMaxFavouriteRouteStop = MutableStateFlow(0)
    private val favouriteRouteStopLock: Lock = Lock()
    val favoriteRouteStops: Map<Int, FavouriteRouteStop> = ConcurrentMutableMap()

    fun updateFavoriteRouteStops(mutation: (MutableMap<Int, FavouriteRouteStop>) -> Unit) {
        favouriteRouteStopLock.withLock {
            mutation.invoke(favoriteRouteStops as MutableMap<Int, FavouriteRouteStop>)
            val max = favoriteRouteStops.maxOfOrNull { it.key }?: 0
            currentMaxFavouriteRouteStop.value = max.coerceAtLeast(8)
            suggestedMaxFavouriteRouteStop.value = (max + 1).coerceIn(8, 30)
        }
    }

    @NativeCoroutinesState
    val suggestedMaxFavouriteRouteStopState: StateFlow<Int> = suggestedMaxFavouriteRouteStop
    @NativeCoroutinesState
    val currentMaxFavouriteRouteStopState: StateFlow<Int> = currentMaxFavouriteRouteStop

    private const val LAST_LOOKUP_ROUTES_MEM_SIZE = 50
    private val lastLookupRouteLock: Lock = Lock()
    private val lastLookupRoutes: ArrayDeque<LastLookupRoute> = ArrayDeque(LAST_LOOKUP_ROUTES_MEM_SIZE)

    fun addLookupRoute(routeNumber: String, co: Operator, meta: String) {
        addLookupRoute(LastLookupRoute(routeNumber, co, meta))
    }

    fun addLookupRoute(data: LastLookupRoute) {
        lastLookupRouteLock.withLock {
            lastLookupRoutes.removeAll { it == data }
            lastLookupRoutes.add(data)
            while (lastLookupRoutes.size > LAST_LOOKUP_ROUTES_MEM_SIZE) {
                lastLookupRoutes.removeFirst()
            }
        }
    }

    fun clearLookupRoute() {
        lastLookupRoutes.clear()
    }

    fun getLookupRoutes(): List<LastLookupRoute> {
        lastLookupRouteLock.withLock {
            return ArrayList(lastLookupRoutes)
        }
    }

    fun getFavoriteAndLookupRouteIndex(routeNumber: String, co: Operator, meta: String): Int {
        for ((index, route) in favoriteRouteStops) {
            val routeData = route.route
            if (routeData.routeNumber == routeNumber && route.co == co && (co != Operator.GMB || routeData.gmbRegion == meta.gmbRegion) && (co != Operator.NLB || routeData.nlbId == meta)) {
                return index
            }
        }
        lastLookupRouteLock.withLock {
            for ((index, data) in lastLookupRoutes.withIndex()) {
                val (lookupRouteNumber, lookupCo, lookupMeta) = data
                if (lookupRouteNumber == routeNumber && lookupCo == co && ((co != Operator.GMB && co != Operator.NLB) || meta == lookupMeta)) {
                    return (lastLookupRoutes.size - index) + 8
                }
            }
        }
        return Int.MAX_VALUE
    }

    fun hasFavoriteAndLookupRoute(): Boolean {
        return favoriteRouteStops.isNotEmpty() || lastLookupRoutes.isNotEmpty()
    }

    val routeSortModePreference: Map<RouteListType, RouteSortMode> = ConcurrentMutableMap()

}