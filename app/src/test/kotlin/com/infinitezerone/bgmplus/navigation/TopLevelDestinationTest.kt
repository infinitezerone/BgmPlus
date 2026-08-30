package com.infinitezerone.bgmplus.navigation

import com.infinitezerone.bgmplus.feature.user.navigation.UserRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopLevelDestinationTest {
    @Test
    fun verifyTopLevelDestinations() {
        val destinations = TopLevelDestination.entries
        assertEquals(4, destinations.size)

        val schedule = TopLevelDestination.SCHEDULE
        assertEquals("放送", schedule.labelText)
        assertEquals(ScheduleRoute, schedule.route)

        val explore = TopLevelDestination.EXPLORE
        assertEquals("探索", explore.labelText)
        assertEquals(ExploreRoute, explore.route)

        val rakuen = TopLevelDestination.RAKUEN
        assertEquals("超展开", rakuen.labelText)
        assertEquals(RakuenRoute, rakuen.route)

        val user = TopLevelDestination.USER
        assertEquals("我的", user.labelText)
        assertEquals(UserRoute, user.route)
    }

    @Test
    fun verifySubjectDetailRoute() {
        val route = SubjectDetailRoute(subjectId = 1001L)
        assertEquals(1001L, route.subjectId)
        assertNotNull(route.toString())
        assertTrue(route.toString().contains("1001"))
    }
}
