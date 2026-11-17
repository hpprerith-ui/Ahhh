package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MceCampusData
import com.example.model.StepDirection
import com.example.navigation.IndoorPathfinder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BlockWay", appName)
    }

    @Test
    fun `pathfinder routes multi-floor inside Main Block`() {
        val portico = MceCampusData.getRoom("mb_g_portico")!!
        val mb202 = MceCampusData.getRoom("mb_2_202")!!

        val route = IndoorPathfinder.findRoute(portico, mb202, accessibleOnly = false)
        assertNotNull(route)
        assertTrue(route!!.crossesFloors)
        assertFalse(route.crossesBlocks)
        assertTrue(route.totalDistanceMeters > 10)
        assertEquals(StepDirection.ARRIVE, route.steps.last().direction)
    }

    @Test
    fun `accessible routing avoids stairs and uses elevator`() {
        val portico = MceCampusData.getRoom("mb_g_portico")!!
        val mb202 = MceCampusData.getRoom("mb_2_202")!!

        val route = IndoorPathfinder.findRoute(portico, mb202, accessibleOnly = true)
        assertNotNull(route)
        assertTrue(route!!.accessibleOnly)
        assertFalse(
            route.steps.any {
                it.direction == StepDirection.STAIRS_UP || it.direction == StepDirection.STAIRS_DOWN
            }
        )
        assertTrue(
            route.steps.any { it.direction == StepDirection.ELEVATOR_UP }
        )
    }

    @Test
    fun `pathfinder routes across blocks from MB to AB-204`() {
        val chemLab = MceCampusData.getRoom("mb_g_chem_lab")!!
        val ab204 = MceCampusData.getRoom("ab_2_204")!!

        val route = IndoorPathfinder.findRoute(chemLab, ab204, accessibleOnly = true)
        assertNotNull(route)
        assertTrue(route!!.crossesBlocks)
        assertTrue(route.crossesFloors)
        assertTrue(route.steps.any { it.direction == StepDirection.OUTDOOR_WALK })
    }

    @Test
    fun `searchRooms ranks exact code match first and supports wing and keyword queries`() {
        val codeResults = MceCampusData.searchRooms("MB 202")
        assertTrue(codeResults.isNotEmpty())
        assertEquals("mb_2_202", codeResults.first().id)

        val physicsResults = MceCampusData.searchRooms("physics")
        assertTrue(physicsResults.any { it.id == "mb_g_phys_w" || it.id == "mb_g_phys_e" })
    }

    @Test
    fun `map quick search and focusRoomOnMap update ViewModel floor and focus state`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.CampusNavViewModel(app)

        vm.updateMapSearchQuery("MB 202")
        repeat(40) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            if (vm.uiState.value.mapSearchResults.isNotEmpty()) return@repeat
            Thread.sleep(25)
        }
        assertTrue(vm.uiState.value.mapSearchResults.isNotEmpty())
        assertEquals("mb_2_202", vm.uiState.value.mapSearchResults.first().id)

        val ab204 = MceCampusData.getRoom("ab_2_204")!!
        vm.focusRoomOnMap(ab204)
        repeat(20) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            if (vm.uiState.value.focusedRoom?.id == "ab_2_204") return@repeat
            Thread.sleep(15)
        }
        val afterFocus = vm.uiState.value
        assertEquals("AB", afterFocus.viewingBlockId)
        assertEquals(2, afterFocus.viewingFloorNumber)
        assertEquals("ab_2_204", afterFocus.focusedRoom?.id)
        assertTrue(afterFocus.focusRequestNonce > 0)
        assertEquals("", afterFocus.mapSearchQuery)
        assertTrue(afterFocus.mapSearchResults.isEmpty())
    }

    @Test
    fun `AB 2nd floor contains AB-201 through AB-212 as classrooms and routes to AB-207`() {
        val ab2Floor = MceCampusData.getFloor("AB", 2)!!
        val expectedCodes = (201..212).map { "AB-$it" }
        expectedCodes.forEach { code ->
            val room = ab2Floor.rooms.find { it.code == code }
            assertNotNull("Expected $code on AB 2F", room)
            assertEquals("Expected $code to be CLASSROOM", com.example.model.RoomCategory.CLASSROOM, room!!.category)
        }

        val portico = MceCampusData.getRoom("mb_g_portico")!!
        val ab207 = MceCampusData.getRoom("ab_2_207")!!
        val route = IndoorPathfinder.findRoute(portico, ab207, accessibleOnly = false)
        assertNotNull(route)
        assertEquals(StepDirection.ARRIVE, route!!.steps.last().direction)
    }
}
