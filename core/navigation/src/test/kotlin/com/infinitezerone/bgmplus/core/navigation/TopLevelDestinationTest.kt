package com.infinitezerone.bgmplus.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopLevelDestinationTest {
    @Test
    fun verifyTopLevelDestinations() {
        val destinations = TopLevelDestination.entries
        assertEquals(3, destinations.size)

        val schedule = TopLevelDestination.SCHEDULE
        assertEquals("放送", schedule.labelText)
        assertEquals(ScheduleRoute, schedule.route)

        val explore = TopLevelDestination.EXPLORE
        assertEquals("探索", explore.labelText)
        assertEquals(ExploreRoute, explore.route)

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
